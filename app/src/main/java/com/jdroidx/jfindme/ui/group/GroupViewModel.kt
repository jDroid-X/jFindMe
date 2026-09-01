package com.jdroidx.jfindme.ui.group

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 🚀 Enterprise Identity & Grouping ViewModel (MVVM Architecture)
 * Handles reactive LiveData bindings, local SQLite roster SSOT,
 * cloud synchronization, countdown timer state, and lifecycle-safe Firebase signals.
 */
class GroupViewModel : ViewModel() {

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
    }

    /**
     * 🚀 Asynchronous Local Database Roster Query (SSOT)
     */
    fun refreshLocalRoster(db: DatabaseHelper?) {
        if (db == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val roster = db.getOfflineRoster()
                _rosterData.postValue(roster ?: emptyList())
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to query local offline roster", e)
            }
        }
    }

    /**
     * 🔘 State 1: Park Member for Deletion (Grey Shade & FCM Notification)
     */
    fun parkMemberForDeletion(uid: String?, activeGroup: String?, db: DatabaseHelper?, onComplete: Runnable?) {
        if (uid.isNullOrEmpty()) return
        _isLoading.postValue(true)
        _syncStatus.postValue("PARKING NODE FOR UNLINK...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!activeGroup.isNullOrEmpty()) {
                    // 1. Mark status as PENDING_UNLINK in cloud group hub
                    FirebaseComm.relationshipsRef.child(activeGroup).child("members").child(uid).child("status").setValue("PENDING_UNLINK")
                    FirebaseComm.relationshipsRef.child(activeGroup).child("signals").child("refresh_roster").setValue(System.currentTimeMillis())

                    // 2. Dispatch FCM Push Notification payload to target device
                    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
                    val myName = if (myUid.isNotEmpty()) db?.getDeviceCentricName(myUid) ?: "Connected Guardian" else "Connected Guardian"
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
                db?.parkRosterNode(uid)

                val updated = db?.getOfflineRoster()
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
    fun cancelParkedDeletion(uid: String?, activeGroup: String?, db: DatabaseHelper?, onComplete: Runnable?) {
        if (uid.isNullOrEmpty()) return
        _isLoading.postValue(true)
        _syncStatus.postValue("RESTORING NODE...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!activeGroup.isNullOrEmpty()) {
                    // 1. Restore status to Active in cloud group hub
                    FirebaseComm.relationshipsRef.child(activeGroup).child("members").child(uid).child("status").setValue("Active")
                    FirebaseComm.relationshipsRef.child(activeGroup).child("signals").child("refresh_roster").setValue(System.currentTimeMillis())

                    // 2. Dispatch cancellation notice
                    val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
                    val myName = if (myUid.isNotEmpty()) db?.getDeviceCentricName(myUid) ?: "Connected Guardian" else "Connected Guardian"
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
                db?.restoreRosterNode(uid)

                val updated = db?.getOfflineRoster()
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
    fun deleteMember(uid: String?, activeGroup: String?, db: DatabaseHelper?, onComplete: Runnable?) {
        if (uid.isNullOrEmpty()) return
        _isLoading.postValue(true)
        _syncStatus.postValue("PURGING NODE...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val myAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
                val myAgentId = if (myAuthUid != null) db?.resolveAgentIdLocally(myAuthUid) ?: "" else ""

                if (!activeGroup.isNullOrEmpty()) {
                    // 1. Remove from group hub
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
                }

                db?.deleteRosterNode(uid)

                val updated = db?.getOfflineRoster()
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

        val db = DatabaseHelper.getInstance(context)
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
     * into local SQLite via DatabaseHelper, ensuring 100% SSOT parity across screens.
     */
    fun syncCloudGroupToLocalDb(context: android.content.Context, groupId: String?) {
        if (groupId.isNullOrEmpty()) {
            refreshLocalRoster(DatabaseHelper.getInstance(context))
            return
        }

        val db = DatabaseHelper.getInstance(context)
        _isLoading.postValue(true)
        _syncStatus.postValue("SYNCING CLOUD ROSTER...")

        FirebaseComm.relationshipsRef.child(groupId).child("members").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        for (nodeSnap in snapshot.children) {
                            val uid = nodeSnap.key ?: continue
                            val role = nodeSnap.child("role").getValue(String::class.java) ?: "Member"
                            val name = nodeSnap.child("name").getValue(String::class.java) 
                                ?: nodeSnap.child("fullName").getValue(String::class.java)
                                ?: "Device_$uid"
                            val avatar = nodeSnap.child("avatar_id").getValue(String::class.java) ?: "1"

                            val upper = role.uppercase()
                            val isAdmin = upper.contains("MOTHER") || upper.contains("FATHER") || upper.contains("SPOUSE") || upper.contains("GUARDIAN") || upper.contains("ADMIN")
                            val isFriend = upper.contains("FRIEND") || upper.contains("COLLEAGUE") || upper.contains("PEER") || upper.contains("ACQUAINTANCE") || upper.contains("NEIGHBOR")

                            db.saveRosterNode(
                                uid1 = uid,
                                name = name,
                                role = role,
                                status = "Active",
                                isAdmin = isAdmin,
                                isFriend = isFriend,
                                ownerName = "Cloud Group",
                                avatarId = avatar,
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

    override fun onCleared() {
        super.onCleared()
        cancelCountdown()
        clearSignalListener()
        clearHandshakeListener()
    }
}
