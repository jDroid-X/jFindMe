package com.jdroidx.jfindme.ui.group

import android.app.Application
import android.content.Intent
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.repository.DeviceRepository
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.DevicePresentationResolver
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 📦 Legacy Node Candidate DTO for Circle Reclamation
 */
data class LegacyNodeCandidate(
    val uid: String,
    val name: String,
    val role: String,
    val avatarId: String,
    val isAdmin: Boolean,
    val isFriend: Boolean
)

/**
 * 🚀 Enterprise Identity & Grouping ViewModel (MVVM Architecture)
 * Handles reactive LiveData bindings, local SQLite roster SSOT,
 * cloud synchronization, countdown timer state, and lifecycle-safe Firebase signals.
 */
// [Variables] TAG, _syncStatus, _isLoading, _rosterData, _signalReceived, _timerDisplay, _isTimerExpired, _isTimerActive, activeCountDownTimer
// [Constants] TAG = "GroupViewModel"
// [Dependencies] AndroidViewModel, Application, DeviceRepository, DeviceRepository, FirebaseComm
// [Integration Points] NetworkGatewayActivity, FamilyFriendsActivity
class GroupViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "GroupViewModel"

    private val _syncStatus = MutableLiveData<String>("READY")
    val syncStatus: LiveData<String> = _syncStatus

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _rosterData = MutableLiveData<List<Map<String, Any>>>(emptyList())
    val rosterData: LiveData<List<Map<String, Any>>> = _rosterData

    private val _signalReceived = MutableLiveData<Boolean>(false)
    val signalReceived: LiveData<Boolean> = _signalReceived

    // ⏱️ Reactive PIN Expiry Countdown Timer
    private val _timerDisplay = MutableLiveData<String>("")
    val timerDisplay: LiveData<String> = _timerDisplay

    private val _isTimerExpired = MutableLiveData<Boolean>(false)
    val isTimerExpired: LiveData<Boolean> = _isTimerExpired

    private val _isTimerActive = MutableLiveData<Boolean>(false)
    val isTimerActive: LiveData<Boolean> = _isTimerActive

    private var activeCountDownTimer: CountDownTimer? = null
    private var activeSignalQuery: Query? = null
    private var activeSignalListener: ValueEventListener? = null
    private var activeHandshakeQuery: Query? = null
    private var activeHandshakeListener: ValueEventListener? = null

    // 🛡️ Handshake Grace Period Sentinel
    private var isHandshakePending = false

    fun setHandshakePending(pending: Boolean) {
        this.isHandshakePending = pending
    }

    fun setSyncStatus(status: String) {
        _syncStatus.postValue(status)
    }

    fun setIsLoading(loading: Boolean) {
        _isLoading.postValue(loading)
    }

    /**
     * ⏱️ Starts lifecycle-safe countdown timer for 6/8-digit Pairing PIN/Token
     */
    fun startCountdown(ttlMinutes: Int, isLocal: Boolean, onExpire: (() -> Unit)? = null) {
        cancelCountdown()
        val totalMs = ttlMinutes.coerceAtLeast(1) * 60 * 1000L
        _isTimerExpired.postValue(false)
        _isTimerActive.postValue(true)

        activeCountDownTimer = object : CountDownTimer(totalMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val mins = (millisUntilFinished / 1000) / 60
                val secs = (millisUntilFinished / 1000) % 60
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                _timerDisplay.postValue(formatted)
                _syncStatus.postValue("BROADCASTING (EXPIRES IN $formatted)")
            }

            override fun onFinish() {
                if (isHandshakePending) {
                    _syncStatus.postValue("⏳ NEGOTIATING HANDSHAKE (GRACE PERIOD: 30s)")
                    activeCountDownTimer = object : CountDownTimer(30000L, 1000L) {
                        override fun onTick(ms: Long) {
                            val secs = ms / 1000
                            _timerDisplay.postValue("00:${String.format(Locale.getDefault(), "%02d", secs)}")
                        }
                        override fun onFinish() {
                            _timerDisplay.postValue("00:00")
                            _isTimerExpired.postValue(true)
                            _isTimerActive.postValue(false)
                            isHandshakePending = false
                            _syncStatus.postValue(if (isLocal) "🔴 P2P TOKEN EXPIRED - REGENERATE" else "🔴 CLOUD PIN EXPIRED - REGENERATE")
                            onExpire?.invoke()
                        }
                    }.start()
                    return
                }
                _timerDisplay.postValue("00:00")
                _isTimerExpired.postValue(true)
                _isTimerActive.postValue(false)
                _syncStatus.postValue(if (isLocal) "🔴 P2P TOKEN EXPIRED - REGENERATE" else "🔴 CLOUD PIN EXPIRED - REGENERATE")
                onExpire?.invoke()
            }
        }.start()
    }

    fun cancelCountdown() {
        activeCountDownTimer?.cancel()
        activeCountDownTimer = null
        _isTimerActive.postValue(false)
        isHandshakePending = false // Reset pending handshake sentinel
    }

    /**
     * 🚀 Asynchronous Local Database Roster Query (SSOT)
     */
    fun refreshLocalRoster() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val roster = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(getApplication()).getOfflineRoster()
                SystemLogger.i(TAG, "refreshLocalRoster: queried " + (roster?.size ?: 0) + " nodes from SSOT")
                _rosterData.postValue(roster ?: emptyList())
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to query local offline roster via DeviceRepository", e)
            }
        }
    }

    fun refreshLocalRoster(db: DeviceRepository?) {
        if (db == null) {
            refreshLocalRoster()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val roster = db.getOfflineRoster()
                SystemLogger.i(TAG, "refreshLocalRoster(db): queried " + (roster?.size ?: 0) + " nodes from SSOT")
                _rosterData.postValue(roster ?: emptyList())
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to query local offline roster", e)
            }
        }
    }

    /**
     * 🔘 State 1: Park Member for Deletion (Grey Shade & FCM Notification)
     */
    @JvmOverloads
    fun parkMemberForDeletion(uid: String?, activeGroup: String?, db: DeviceRepository? = null, onComplete: Runnable? = null) {
        if (uid.isNullOrEmpty()) return
        _isLoading.postValue(true)
        _syncStatus.postValue("PARKING NODE FOR UNLINK...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val effectiveDb = db ?: DeviceRepository.getInstance(getApplication())
                if (!activeGroup.isNullOrEmpty()) {
                    // 1. Mark status as PENDING_UNLINK in cloud group hub
                    FirebaseComm.relationshipsRef.child(activeGroup).child("members").child(uid).child("status").setValue("PENDING_UNLINK")
                    FirebaseComm.relationshipsRef.child(activeGroup).child("signals").child("refresh_roster").setValue(System.currentTimeMillis())

                    // 2. Dispatch FCM Push Notification payload to target device
                    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
                    val myName = if (myUid.isNotEmpty()) effectiveDb.getDeviceCentricName(myUid) ?: "Connected Guardian" else "Connected Guardian"
                    val cmd = mapOf(
                        "action" to "UNLINK_REQUEST",
                        "initiator_uid" to myUid,
                        "initiator_name" to myName,
                        "msg" to "$myName requested to unlink your device. Tap to review in Network Gateway.",
                        "epoch_ms" to System.currentTimeMillis()
                    )
                    FirebaseComm.devicesRef.child(uid).child("device").child("remote_command").setValue(cmd)
                }

                // 3. Mark local SQLite status as PENDING_UNLINK
                effectiveDb.parkRosterNode(uid)

                val updated = effectiveDb.getOfflineRoster()
                _rosterData.postValue(updated ?: emptyList())
                _syncStatus.postValue("NODE PARKED (GREY)")
                _isLoading.postValue(false)
                onComplete?.let {
                    android.os.Handler(android.os.Looper.getMainLooper()).post(it)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to park node $uid", e)
                _isLoading.postValue(false)
                _syncStatus.postValue("PARK FAILED")
            }
        }
    }

    /**
     * ↩️ State 2A: Cancel Parked Deletion (Restore to Active)
     */
    @JvmOverloads
    fun cancelParkedDeletion(uid: String?, activeGroup: String?, db: DeviceRepository? = null, onComplete: Runnable? = null) {
        if (uid.isNullOrEmpty()) return
        _isLoading.postValue(true)
        _syncStatus.postValue("RESTORING NODE...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val effectiveDb = db ?: DeviceRepository.getInstance(getApplication())
                if (!activeGroup.isNullOrEmpty()) {
                    // 1. Restore status to Active in cloud group hub
                    FirebaseComm.relationshipsRef.child(activeGroup).child("members").child(uid).child("status").setValue("Active")
                    FirebaseComm.relationshipsRef.child(activeGroup).child("signals").child("refresh_roster").setValue(System.currentTimeMillis())

                    // 2. Dispatch cancellation notice
                    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
                    val myName = if (myUid.isNotEmpty()) effectiveDb.getDeviceCentricName(myUid) ?: "Connected Guardian" else "Connected Guardian"
                    val cmd = mapOf(
                        "action" to "UNLINK_CANCELLED",
                        "initiator_uid" to myUid,
                        "initiator_name" to myName,
                        "msg" to "Unlink request was cancelled by $myName. Connection restored.",
                        "epoch_ms" to System.currentTimeMillis()
                    )
                    FirebaseComm.devicesRef.child(uid).child("device").child("remote_command").setValue(cmd)
                }

                // 3. Restore local SQLite status to Active
                effectiveDb.restoreRosterNode(uid)

                val updated = effectiveDb.getOfflineRoster()
                _rosterData.postValue(updated ?: emptyList())
                _syncStatus.postValue("NODE RESTORED")
                _isLoading.postValue(false)
                onComplete?.let {
                    android.os.Handler(android.os.Looper.getMainLooper()).post(it)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to restore node $uid", e)
                _isLoading.postValue(false)
                _syncStatus.postValue("RESTORE FAILED")
            }
        }
    }

    /**
     * 🛡️ Safe Node Deletion (Closed Loop SQLite + Firebase Member Purge)
     * 🚀 Complete 2-Way Closed-Loop Node Unlinking and Deletion
     */
    @JvmOverloads
    fun deleteMember(uid: String?, activeGroup: String?, db: DeviceRepository? = null, onComplete: Runnable? = null) {
        if (uid.isNullOrEmpty()) return
        _isLoading.postValue(true)
        _syncStatus.postValue("PURGING NODE...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val effectiveDb = db ?: DeviceRepository.getInstance(getApplication())
                val myAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
                val myAgentId = if (myAuthUid != null) effectiveDb.resolveAgentIdLocally(myAuthUid) ?: "" else ""

                if (!activeGroup.isNullOrEmpty()) {
                    // 1. Remove from group hub (Unified + Legacy)
                    FirebaseComm.relationshipsRef.child(activeGroup).child("devices").child(uid).removeValue()
                    FirebaseComm.relationshipsRef.child(activeGroup).child("members").child(uid).removeValue()
                    FirebaseComm.relationshipsRef.child(activeGroup).child("citizen_dossiers").child(uid).removeValue()
                    FirebaseComm.relationshipsRef.child(activeGroup).child("direct_pairs").child(uid).removeValue()
                    FirebaseComm.relationshipsRef.child(activeGroup).child("signals").child("refresh_roster").setValue(System.currentTimeMillis())

                    // 2. Remove 2-way device relationships
                    if (myAgentId.isNotEmpty()) {
                        FirebaseComm.devicesRef.child(myAgentId).child("relationships").child(uid).removeValue()
                        FirebaseComm.devicesRef.child(uid).child("relationships").child(myAgentId).removeValue()
                    }

                    // 3. Isolate unlinked member to their own circle
                    FirebaseComm.devicesRef.child(uid).child("profile").child("active_group").setValue("GRP_$uid")
                    FirebaseComm.devicesRef.child(uid).child("Identity").child("active_group").setValue("GRP_$uid")
                    FirebaseComm.relationshipsRef.child("GRP_$uid").child("signals").child("refresh_roster").setValue(System.currentTimeMillis())

                    FirebaseComm.logChange(activeGroup, FirebaseComm.LOG_ITEM_PAIRING, "MEMBER_UNLINK", uid, mapOf("unlinked_member" to uid))
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(uid).child("joined_circles").child(activeGroup).removeValue()
                }

                // [Variables] | [Constants] | [Dependencies] | [Integration Points]
                // Group-Scoped Unlink: Purges group_members entry for active group while keeping user_identities intact
                effectiveDb.deleteRosterNode(uid, activeGroup)

                val updated = effectiveDb.getOfflineRoster()
                _rosterData.postValue(updated ?: emptyList())
                _syncStatus.postValue("NODE PURGED")
                _isLoading.postValue(false)
                onComplete?.let {
                    android.os.Handler(android.os.Looper.getMainLooper()).post(it)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to purge node $uid", e)
                _isLoading.postValue(false)
                _syncStatus.postValue("PURGE FAILED")
            }
        }
    }

    /**
     * 🚀 Forensic Role Change Waterfall: Orchestrates the SSOT sync.
     */
    fun updateForensicIdentity(context: android.content.Context, uid: String, myGrp: String, newRole: String, isFriend: Boolean, onComplete: Runnable?) {
        _isLoading.value = true
        _syncStatus.value = "FINALIZING IDENTITY MATRIX..."

        val db = DeviceRepository.getInstance(context)
        db.updateRosterGrouping(uid, newRole, isFriend, myGrp)

        FirebaseComm.relationshipsRef.child(myGrp).child("members").child(uid).child("role")
            .setValue(newRole)
            .addOnCompleteListener {
                val userRef = FirebaseComm.devicesRef.child(uid).child("profile")
                val map = mapOf(
                    "role_family" to (if (isFriend) "None" else newRole),
                    "role_friend" to (if (isFriend) newRole else "None")
                )
                userRef.updateChildren(map)

                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
                if (currentUid != null) {
                    FirebaseComm.resolveHardwareIdentity(currentUid) { hostAgentId ->
                        val finalHostId = hostAgentId ?: currentUid
                        if (FirebaseComm.isAgentIdValid(finalHostId)) {
                            FirebaseComm.getRelationshipsRef(finalHostId).child(uid).child("role").setValue(newRole)
                        }
                    }
                }

                FirebaseComm.logChange(myGrp, FirebaseComm.LOG_ITEM_FAMILY_FRIENDS, "ROLE_UPDATE", uid, mapOf("member" to uid, "role" to newRole, "is_friend" to isFriend))

                FirebaseComm.relationshipsRef.child(myGrp).child("signals").child("refresh_roster")
                    .setValue(System.currentTimeMillis())
                    .addOnCompleteListener {
                        _isLoading.postValue(false)
                        _syncStatus.postValue("NETWORK PARITY SECURED: $newRole")
                        refreshLocalRoster(db)
                        onComplete?.let {
                            android.os.Handler(android.os.Looper.getMainLooper()).post(it)
                        }
                    }
            }
    }

    /**
     * 🌐 Closed-Loop Cloud to Local SSOT Synchronization
     * Pulls active group members from Firebase Realtime Database and mirrors them
     * into local SQLite via DeviceRepository, ensuring 100% SSOT parity across screens.
     */
    // [Variables] context: Context, groupId: String?, db: DeviceRepository, membersSnap: DataSnapshot, devicesSnap: DataSnapshot, allNodeSnaps: MutableList<DataSnapshot>, uid: String, profSnap: DataSnapshot, role: String, name: String, avatar: String, upper: String, isAdmin: Boolean, isFriend: Boolean | [Constants] NODE_RELATIONSHIPS | [Dependencies] DeviceRepository, FirebaseComm, SystemLogger, Dispatchers.IO, viewModelScope | [Integration Points] NetworkGatewayActivity, FamilyFriendsViewModel, MapViewModel
    fun syncCloudGroupToLocalDb(context: android.content.Context, groupId: String?) {
        if (groupId.isNullOrEmpty()) {
            refreshLocalRoster(DeviceRepository.getInstance(context))
            return
        }

        val db = DeviceRepository.getInstance(context)
        _isLoading.postValue(true)
        _syncStatus.postValue("SYNCING CLOUD ROSTER...")

        FirebaseComm.relationshipsRef.child(groupId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val membersSnap = snapshot.child("members")
                        val devicesSnap = snapshot.child("devices")
                        val allNodeSnaps = mutableListOf<DataSnapshot>()
                        if (devicesSnap.exists()) {
                            for (c in devicesSnap.children) allNodeSnaps.add(c)
                        }
                        if (membersSnap.exists()) {
                            for (c in membersSnap.children) {
                                if (!devicesSnap.hasChild(c.key ?: "")) {
                                    allNodeSnaps.add(c)
                                }
                            }
                        }
                        if (allNodeSnaps.isEmpty() && snapshot.childrenCount > 0) {
                            for (c in snapshot.children) {
                                if (c.key != "signals" && c.key != "created_at" && c.key != "owner_id") {
                                    allNodeSnaps.add(c)
                                }
                            }
                        }

                        for (nodeSnap in allNodeSnaps) {
                            val uid = nodeSnap.key ?: continue
                            // [Variables] memSnap, idSnap, profSnap, role, name, avatar, upper, isAdmin, isFriend | [Constants] "Member", "1" | [Dependencies] DataSnapshot, DeviceRepository | [Integration Points] syncCloudGroupToLocalDb multi-schema cloud sync
                            val memSnap = if (nodeSnap.hasChild("membership")) nodeSnap.child("membership") else null
                            val idSnap = if (nodeSnap.hasChild("identity")) nodeSnap.child("identity") else null
                            val profSnap = if (nodeSnap.hasChild("profile")) nodeSnap.child("profile") else nodeSnap

                            val role = memSnap?.child("role")?.getValue(String::class.java)
                                ?: memSnap?.child("role_family")?.getValue(String::class.java)
                                ?: nodeSnap.child("role").getValue(String::class.java) 
                                ?: profSnap.child("role_title").getValue(String::class.java)
                                ?: "Member"

                            val name = idSnap?.child("name")?.getValue(String::class.java)
                                ?: idSnap?.child("full_name")?.getValue(String::class.java)
                                ?: nodeSnap.child("name").getValue(String::class.java) 
                                ?: profSnap.child("full_name").getValue(String::class.java)
                                ?: profSnap.child("display_name").getValue(String::class.java)
                                ?: nodeSnap.child("fullName").getValue(String::class.java)
                                ?: "Device_$uid"

                            val avatar = idSnap?.child("avatar_id")?.getValue(String::class.java)
                                ?: nodeSnap.child("avatar_id").getValue(String::class.java)
                                ?: profSnap.child("avatar_id").getValue(String::class.java)
                                ?: "1"

                            // [Variables] cleanUid, ssotRole, finalRole, finalName, finalAvatar, upper, circleTypeSnap, primeChildId, isChild, isSpouse, isAdmin, isFriend | [Constants] "Member", "1", "Active", "Cloud Group" | [Dependencies] DevicePresentationResolver, RegistryManager, DeviceIdentityHelper | [Integration Points] syncCloudGroupToLocalDb SSOT Protection
                            val cleanUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(uid)
                            val rawRole = role
                            val ssotRole = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveRoleString(context, uid).let {
                                if (it.isNullOrBlank() || it == "Member" || it == "Device" || it == "Host") {
                                    com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveRoleString(context, cleanUid)
                                } else it
                            }
                            val finalRole = if (!ssotRole.isNullOrBlank() && ssotRole != "Member" && ssotRole != "Device" && ssotRole != "Host") ssotRole else rawRole
                            val finalName = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveDisplayName(context, uid, name)
                            val finalAvatar = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveAvatarId(context, uid, avatar)

                            val upper = finalRole.uppercase()
                            val circleTypeSnap = memSnap?.child("circle_type")?.getValue(String::class.java)
                                ?: nodeSnap.child("circle_type").getValue(String::class.java)
                                ?: ""
                            val primeChildId = com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(context)
                            val isChild = upper.contains("CHILD") || upper.contains("WARD") || upper.contains("DEPENDENT") ||
                                (!primeChildId.isNullOrEmpty() && (primeChildId.equals(uid, ignoreCase = true) || primeChildId.equals(cleanUid, ignoreCase = true)))
                            val isSpouse = !isChild && (upper.contains("SPOUSE") || upper.contains("WIFE") || upper.contains("HUSBAND") || upper.contains("PARTNER"))
                            val isAdmin = !isChild && !isSpouse && (upper.contains("MOTHER") || upper.contains("FATHER") || upper.contains("GUARDIAN") || upper.contains("ADMIN") || upper.contains("OWNER"))
                            val isFriend = !isChild && !isSpouse && !isAdmin && (upper.contains("FRIEND") || upper.contains("COLLEAGUE") || upper.contains("PEER") || upper.contains("ACQUAINTANCE") || upper.contains("NEIGHBOR") || "FRIEND".equals(circleTypeSnap, ignoreCase = true))

                            db.saveRosterNode(
                                deviceId = uid,
                                name = finalName,
                                role = finalRole,
                                status = "Active",
                                isAdmin = isAdmin,
                                isFriend = isFriend,
                                ownerName = "Cloud Group",
                                avatarId = finalAvatar,
                                activeGroupId = groupId
                            )
                        }
                    } catch (e: Exception) {
                        SystemLogger.e(TAG, "Error saving cloud roster to SQLite", e)
                    } finally {
                        _isLoading.postValue(false)
                        _syncStatus.postValue("ROSTER IN SYNC")
                        refreshLocalRoster(db)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.postValue(false)
                _syncStatus.postValue("SYNC TIMED OUT")
                refreshLocalRoster(db)
            }
        })
    }

    /**
     * 📡 Setup reactive signal listener for live roster invalidation with clean registration
     */
    fun setupSignalListener(myUid: String, activeGroup: String?, onSignal: (() -> Unit)? = null) {
        if (activeGroup.isNullOrEmpty()) return
        clearSignalListener()

        val query = FirebaseComm.relationshipsRef.child(activeGroup).child("signals").child("refresh_roster")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (snap.exists()) {
                    _signalReceived.postValue(true)
                    onSignal?.invoke()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        query.addValueEventListener(listener)
        activeSignalQuery = query
        activeSignalListener = listener
    }

    fun clearSignalListener() {
        if (activeSignalQuery != null && activeSignalListener != null) {
            activeSignalQuery?.removeEventListener(activeSignalListener!!)
            activeSignalQuery = null
            activeSignalListener = null
        }
    }

    /**
     * 🤝 Register and monitor Handshake Approval listener safely
     */
    fun registerHandshakeApprovalListener(activeCode: String, onApproved: () -> Unit, onRejected: () -> Unit) {
        clearHandshakeListener()
        val query = FirebaseComm.pairingRef.child(activeCode).child("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(statusSnap: DataSnapshot) {
                val status = statusSnap.getValue(String::class.java)
                if ("approved" == status || "FULLY_SYNCED" == status) {
                    clearHandshakeListener()
                    onApproved.invoke()
                } else if ("rejected" == status) {
                    clearHandshakeListener()
                    onRejected.invoke()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        query.addValueEventListener(listener)
        activeHandshakeQuery = query
        activeHandshakeListener = listener
    }

    fun clearHandshakeListener() {
        if (activeHandshakeQuery != null && activeHandshakeListener != null) {
            activeHandshakeQuery?.removeEventListener(activeHandshakeListener!!)
            activeHandshakeQuery = null
            activeHandshakeListener = null
        }
    }

    /**
     * 🛰️ Scans cloud history for legacy assets associated with safeAgentId.
     * Decouples the 4-phase Firebase asynchronous cascade from the View layer.
     */
    // [Variables] app, finalGrp, authorizedLinkedUids, assetRoles, assetIsAdmin, assetIsFriend, candidates
    // [Constants] "GRP_", "DEFAULT_GROUP", "SCAN COMPLETE", "SCANNIG CLOUD HISTORY..."
    // [Dependencies] Application, FirebaseComm, RegistryManager, DevicePresentationResolver
    // [Integration Points] NetworkGatewayActivity legacy circle reclamation
    fun scanForLegacyCircleNodes(safeAgentId: String, onComplete: (String, List<LegacyNodeCandidate>) -> Unit) {
        val app = getApplication<Application>()
        _isLoading.postValue(true)
        _syncStatus.postValue("SCANNING CLOUD HISTORY...")

        val fallbackGrp = "GRP_$safeAgentId"

        val handleScanFailure = { errorMsg: String ->
            SystemLogger.e(TAG, "Legacy Circle Scan Failure: $errorMsg")
            _isLoading.postValue(false)
            _syncStatus.postValue("SCAN_FAILED")
            onComplete(fallbackGrp, emptyList())
        }

        try {
            // Phase 1: Dual-Path Active Group Resolver
            FirebaseComm.getDeviceRef(safeAgentId).get().addOnCompleteListener { devTask ->
                var myGrp: String? = null
                if (devTask.isSuccessful && devTask.result != null) {
                    val dSnap = devTask.result
                    myGrp = dSnap.child("profile").child("active_group").getValue(String::class.java)
                    if (myGrp.isNullOrEmpty()) {
                        myGrp = dSnap.child("Identity").child("active_group").getValue(String::class.java)
                    }
                }
                if (myGrp.isNullOrEmpty() || myGrp == "DEFAULT_GROUP") {
                    myGrp = RegistryManager.getEffectiveActiveGroup(app)
                }
                val finalGrp = if (!myGrp.isNullOrEmpty()) myGrp else fallbackGrp

                // Phase 2: Precise Circle Discovery (Direct relationships)
                FirebaseComm.getDeviceRef(safeAgentId).child("relationships").get().addOnCompleteListener { relTask ->
                    val authorizedLinkedUids = mutableSetOf<String>()
                    val assetRoles = mutableMapOf<String, String>()
                    val assetIsAdmin = mutableMapOf<String, Boolean>()
                    val assetIsFriend = mutableMapOf<String, Boolean>()

                    if (relTask.isSuccessful && relTask.result != null) {
                        for (child in relTask.result.children) {
                            val key = child.key
                            if (key != null && key != safeAgentId) {
                                authorizedLinkedUids.add(key)
                                var r: String? = null
                                if (child.hasChild("role_title")) r = child.child("role_title").getValue(String::class.java)
                                else if (child.hasChild("role")) r = child.child("role").getValue(String::class.java)

                                val branch = child.child("branch_type").getValue(String::class.java)
                                val isFr = "friends".equals(branch, ignoreCase = true) || (r != null && r.lowercase(Locale.US).contains("friend"))
                                val isAdm = (r != null && (r.uppercase(Locale.US).contains("GUARDIAN") || r.uppercase(Locale.US).contains("ADMIN") || r.uppercase(Locale.US).contains("MOTHER") || r.uppercase(Locale.US).contains("FATHER") || r.uppercase(Locale.US).contains("PARENT")))

                                if (r.isNullOrEmpty()) {
                                    r = if (isFr) "Friend" else "Child"
                                }
                                assetRoles[key] = r
                                assetIsFriend[key] = isFr
                                assetIsAdmin[key] = isAdm
                            }
                        }
                    }

                    // Phase 3: Shared groups membership query
                    FirebaseComm.relationshipsRef.get().addOnCompleteListener { groupsTask ->
                        if (groupsTask.isSuccessful && groupsTask.result != null) {
                            for (grpChild in groupsTask.result.children) {
                                if (grpChild.hasChild("members") && grpChild.child("members").hasChild(safeAgentId)) {
                                    for (m in grpChild.child("members").children) {
                                        val mKey = m.key
                                        if (mKey != null && mKey != safeAgentId) {
                                            authorizedLinkedUids.add(mKey)
                                            if (!assetRoles.containsKey(mKey)) {
                                                var mRole = m.child("role").getValue(String::class.java)
                                                if (mRole.isNullOrEmpty() || mRole.equals("Member", ignoreCase = true)) {
                                                    mRole = "Child"
                                                }
                                                val isFr = mRole.lowercase(Locale.US).contains("friend")
                                                val isAdm = mRole.uppercase(Locale.US).contains("GUARDIAN") || mRole.uppercase(Locale.US).contains("ADMIN") || mRole.uppercase(Locale.US).contains("PARENT")
                                                assetRoles[mKey] = mRole
                                                assetIsFriend[mKey] = isFr
                                                assetIsAdmin[mKey] = isAdm
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Phase 4: Device profile resolution for verified linked nodes
                        // [Variables] authorizedLinkedUids, finalGrp | [Constants] "SCAN COMPLETE" | [Dependencies] LegacyNodeCandidate, FirebaseComm | [Integration Points] NetworkGatewayActivity
                        if (authorizedLinkedUids.isEmpty()) {
                            _isLoading.postValue(false)
                            _syncStatus.postValue("SCAN COMPLETE")
                            onComplete(finalGrp, emptyList())
                            return@addOnCompleteListener
                        }

                        FirebaseComm.devicesRef.get().addOnCompleteListener { devicesTask ->
                            val candidates = mutableListOf<LegacyNodeCandidate>()

                            if (devicesTask.isSuccessful && devicesTask.result != null) {
                                val devSnap = devicesTask.result
                                for (linkedId in authorizedLinkedUids) {
                                    if (devSnap.hasChild(linkedId)) {
                                        val child = devSnap.child(linkedId)
                                        var name: String? = null
                                        if (child.hasChild("profile") && child.child("profile").hasChild("name")) {
                                            name = child.child("profile").child("name").getValue(String::class.java)
                                        } else if (child.hasChild("Identity") && child.child("Identity").hasChild("name")) {
                                            name = child.child("Identity").child("name").getValue(String::class.java)
                                        } else if (child.hasChild("name")) {
                                            name = child.child("name").getValue(String::class.java)
                                        }
                                        name = DevicePresentationResolver.resolveDisplayName(app, linkedId, name)

                                        var avatarId = "1"
                                        if (child.hasChild("profile") && child.child("profile").hasChild("avatar_id")) {
                                            avatarId = child.child("profile").child("avatar_id").value?.toString() ?: "1"
                                        }

                                        val rTitle = assetRoles[linkedId] ?: "Family Member"
                                        val isAdmin = assetIsAdmin[linkedId] ?: false
                                        val isFriend = assetIsFriend[linkedId] ?: false

                                        candidates.add(
                                            LegacyNodeCandidate(
                                                uid = linkedId,
                                                name = name,
                                                role = rTitle,
                                                avatarId = avatarId,
                                                isAdmin = isAdmin,
                                                isFriend = isFriend
                                            )
                                        )
                                    }
                                }
                            }

                            _isLoading.postValue(false)
                            _syncStatus.postValue("SCAN COMPLETE")
                            onComplete(finalGrp, candidates)
                        }.addOnFailureListener { e -> handleScanFailure(e.message ?: "Devices query failed") }
                    }.addOnFailureListener { e -> handleScanFailure(e.message ?: "Relationships query failed") }
                }.addOnFailureListener { e -> handleScanFailure(e.message ?: "Device relationships query failed") }
            }.addOnFailureListener { e -> handleScanFailure(e.message ?: "Device ref query failed") }
        } catch (e: Exception) {
            handleScanFailure(e.message ?: "Unexpected exception during legacy scan")
        }
    }

    /**
     * 🔄 Restores selected legacy nodes to targetGroup in Cloud and Local SQLite SSOT.
     */
    // [Variables] app, repoLocal, targetGrp, groupInfo, selfName, selfAvatar, selfIdentity, curRole, isAuthorizedAdmin, selfMembership, count, nodeIdentity, nodeMembership
    // [Constants] "Primary Circle", "HOST_DEV", "CLIENT_DEV", "Active", "Cloud Group", "ACTION_ROSTER_REFRESH"
    // [Dependencies] Application, DeviceRepository, DatabaseHelper, FirebaseComm, RegistryManager, LocalBroadcastManager
    // [Integration Points] NetworkGatewayActivity, FamilyFriendsActivity
    fun restoreLegacyCircleNodes(
        safeAgentId: String,
        targetGroup: String,
        selectedNodes: List<LegacyNodeCandidate>,
        onComplete: (Int) -> Unit
    ) {
        val app = getApplication<Application>()
        val repoLocal = DeviceRepository.getInstance(app)
        val targetGrp = if (targetGroup.isEmpty()) ("GRP_$safeAgentId") else targetGroup

        // 1. Ensure Self is registered in Cloud Group with uniform structure
        val groupInfo = hashMapOf<String, Any>(
            "group_name" to "Primary Circle",
            "host_device_id" to safeAgentId,
            "created_at" to System.currentTimeMillis()
        )
        FirebaseComm.relationshipsRef.child(targetGrp).child("group_info").setValue(groupInfo)

        val selfName = RegistryManager.getUserNickname(app)
        val selfAvatar = RegistryManager.getEquippedAvatarId(app).toString()
        val selfIdentity = hashMapOf<String, Any>(
            "name" to selfName,
            "full_name" to selfName,
            "avatar_id" to selfAvatar
        )

        val curRole = RegistryManager.getRoleFamily(app, "Guardian")
        val isAuthorizedAdmin = curRole.uppercase(Locale.US).contains("GUARDIAN") ||
                curRole.uppercase(Locale.US).contains("ADMIN") ||
                curRole.uppercase(Locale.US).contains("MOTHER") ||
                curRole.uppercase(Locale.US).contains("FATHER") ||
                curRole.uppercase(Locale.US).contains("PARENT")

        val selfMembership = hashMapOf<String, Any>(
            "role" to if (isAuthorizedAdmin) "ADMIN" else "MEMBER",
            "role_family" to curRole,
            "role_friend" to "None",
            "status" to "Active",
            "ts_joined" to System.currentTimeMillis()
        )

        FirebaseComm.relationshipsRef.child(targetGrp).child("devices").child(safeAgentId).child("identity").setValue(selfIdentity)
        FirebaseComm.relationshipsRef.child(targetGrp).child("devices").child(safeAgentId).child("membership").setValue(selfMembership)

        FirebaseComm.getIdentityRef(safeAgentId).child("active_group").setValue(targetGrp)
        FirebaseComm.getProfileRef(safeAgentId).child("active_group").setValue(targetGrp)
        FirebaseComm.getDatabaseRef().child("users").child(safeAgentId).child("joined_circles").child(targetGrp).setValue(true)
        RegistryManager.setActiveGroup(app, targetGrp)

        var count = 0
        for (node in selectedNodes) {
            val nodeIdentity = hashMapOf<String, Any>(
                "name" to node.name,
                "full_name" to node.name,
                "avatar_id" to node.avatarId
            )
            val nodeMembership = hashMapOf<String, Any>(
                "role" to node.role,
                "role_family" to node.role,
                "role_friend" to if (node.isFriend) "Friend" else "None",
                "status" to "Active",
                "ts_joined" to System.currentTimeMillis()
            )

            FirebaseComm.relationshipsRef.child(targetGrp).child("devices").child(node.uid).child("identity").setValue(nodeIdentity)
            FirebaseComm.relationshipsRef.child(targetGrp).child("devices").child(node.uid).child("membership").setValue(nodeMembership)
            FirebaseComm.relationshipsRef.child(targetGrp).child("devices").child(safeAgentId).child("paired_with").child(node.uid).setValue(true)
            FirebaseComm.relationshipsRef.child(targetGrp).child("devices").child(node.uid).child("paired_with").child(safeAgentId).setValue(true)

            FirebaseComm.getIdentityRef(node.uid).child("active_group").setValue(targetGrp)
            FirebaseComm.getProfileRef(node.uid).child("active_group").setValue(targetGrp)
            FirebaseComm.getDatabaseRef().child("users").child(node.uid).child("joined_circles").child(targetGrp).setValue(true)

            // Local SQLite Mirror with explicit role, isAdmin, and isFriend allocation
            repoLocal.saveRosterNode(
                node.uid, node.name, node.role, "Active",
                node.isAdmin, node.isFriend, "Cloud Group",
                null, null, null, node.avatarId, targetGrp
            )

            // 🗄️ SSOT Bilateral Pair Link
            DatabaseHelper.getInstance(app).logDevicePairing(
                safeAgentId,
                selfName,
                "HOST_DEV",
                node.uid,
                node.name,
                "CLIENT_DEV",
                node.role,
                targetGrp
            )
            count++
        }

        LocalBroadcastManager.getInstance(app).sendBroadcast(Intent("ACTION_ROSTER_REFRESH"))
        syncCloudGroupToLocalDb(app, targetGrp)
        onComplete(count)
    }

    override fun onCleared() {
        super.onCleared()
        cancelCountdown()
        clearSignalListener()
        clearHandshakeListener()
    }

    fun loadFullRoster() {
        // Load group members
    }
}
