package com.jdroidx.jfindme.ui.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 📡 STRICT MVVM ORCHESTRATOR FOR RELATIONAL ROSTER
 * Pulls 'members' Array from Active Group Hub, deciphers specific Roles directly from target 
 * Identity Arrays, and pipes mapped Telemetry directly back to the physical UI layer natively.
 */
data class RosterNode(
    val uid: String,
    val displayName: String,
    val roleTitle: String,
    val statusText: String,
    val isAdministrator: Boolean,
    val isFriend: Boolean,
    val avatarId: String? = null,
    val category: String = "EXTENDED_FAMILY", // ROOT_GUARDIAN, SPOUSE, DEPENDENT_CHILD, EXTENDED_FAMILY, FRIEND
    val batteryPercent: Int = 100,
    val isOnline: Boolean = true,
    val lastSeenTime: String = "Online",
    val lastSeenTimestamp: Long = 0L,
    val safeZoneName: String? = null,
    val safeZoneAddress: String? = null,
    val safeZoneId: String? = null,
    val permissionsMask: Int = 0xFF
)

class FamilyFriendsViewModel : ViewModel() {
    private val TAG = "FamilyFriendsViewModel"
    private val db = FirebaseDatabase.getInstance().reference

    private val _rosterLiveData = MutableLiveData<List<RosterNode>>()
    val rosterLiveData: LiveData<List<RosterNode>> = _rosterLiveData

    // GAP-07: Unified roster cache preventing split-brain clobbering between /members and /devices
    private val rosterUnifiedMap = java.util.concurrent.ConcurrentHashMap<String, RosterNode>()

    fun getRosterNodes(): LiveData<List<RosterNode>> = rosterLiveData

    private var rosterListener: ValueEventListener? = null
    private var activeGroupId: String? = null
    private var locationObserver: androidx.lifecycle.Observer<List<com.jdroidx.jfindme.data.model.UserLocation>>? = null

    fun initializeCloudSync(context: android.content.Context) {
        rosterUnifiedMap.clear()
        viewModelScope.launch(Dispatchers.IO) {
            val localDb = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
            val offlineNodes = localDb.getOfflineRoster()
            val prefs = context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
            val selfAuthUid = FirebaseAuth.getInstance().uid ?: ""
            val selfAgentId = prefs.getString("agent_id", "") ?: ""
            val selfDevUid = prefs.getString("device_uid", "") ?: ""
            val resolvedLocalAgentId = if (selfAuthUid.isNotEmpty()) localDb.resolveAgentIdLocally(selfAuthUid) else null
            val focalId = if (selfAgentId.isNotBlank()) selfAgentId else (resolvedLocalAgentId ?: selfAuthUid)
            val scopedIds = if (focalId.isNotBlank()) localDb.getConnectedDeviceIds(focalId) else emptySet()

            if (offlineNodes.isNotEmpty()) {
                val activeGrp = activeGroupId ?: prefs.getString("active_group", null)
                val cached = offlineNodes.filterNot { item ->
                    val u1 = item["uid_1"] as? String ?: ""
                    val u2 = item["uid_2"] as? String ?: ""
                    val role = item["role_type"] as? String ?: ""
                    val name = item["fullName"] as? String ?: ""
                    val isPrime = isChildOrPrimeTarget(u1, role, name) || isChildOrPrimeTarget(u2, role, name)
                    val isSelf = (u1.isNotEmpty() && (u1 == selfAuthUid || u1 == selfAgentId || u1 == selfDevUid || (resolvedLocalAgentId != null && u1 == resolvedLocalAgentId))) ||
                        (u2.isNotEmpty() && (u2 == selfDevUid || u2 == selfAgentId || (resolvedLocalAgentId != null && u2 == resolvedLocalAgentId)))
                    val isDecomOrDel = localDb.isDeviceDeleted(u1, activeGrp) || (u2.isNotEmpty() && localDb.isDeviceDeleted(u2, activeGrp)) ||
                        com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, u1) ||
                        (u2.isNotEmpty() && com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, u2))
                    val inScope = isPrime || (scopedIds.contains(u1) || (u2.isNotEmpty() && scopedIds.contains(u2)))
                    isSelf || isDecomOrDel || (!isPrime && !inScope)
                }.map { item ->
                    val role = item["role_type"] as? String ?: "Unknown"
                    val isFriend = when (val fr = item["is_friend"]) {
                        is Boolean -> fr
                        is Number -> fr.toInt() == 1
                        is String -> fr == "1" || fr.equals("true", ignoreCase = true)
                        else -> false
                    }
                    val isAdmin = when (val adm = item["is_admin"]) {
                        is Boolean -> adm
                        is Number -> adm.toInt() == 1
                        is String -> adm == "1" || adm.equals("true", ignoreCase = true)
                        else -> false
                    }
                    val pMaskStr = item["permissions_mask"]?.toString() ?: "0xFF"
                    val pMask = try {
                        if (pMaskStr.startsWith("0x", ignoreCase = true)) pMaskStr.substring(2).toInt(16)
                        else pMaskStr.toInt()
                    } catch (e: Exception) { 0xFF }
                    val rawBatteryPct = (item["battery_pct"] as? Number)?.toInt() ?: 100
                    val rawLastSeenMs = (item["last_seen_ts"] as? Number)?.toLong() ?: 0L
                    val u1 = item["uid_1"] as? String ?: ""
                    val u2 = item["uid_2"] as? String ?: ""

                    // 🚀 Cross-Reference Live Location SSOT
                    val locRepo = com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context)
                    val cachedLoc = locRepo.getLatestLocationFor(u1) ?: (if (u2.isNotEmpty()) locRepo.getLatestLocationFor(u2) else null)
                    val lastSeenMs = if (cachedLoc != null && cachedLoc.getLongTimestamp() > rawLastSeenMs) cachedLoc.getLongTimestamp() else rawLastSeenMs
                    val batteryPct = if (cachedLoc != null && cachedLoc.batteryLevel > 0) cachedLoc.batteryLevel else rawBatteryPct
                    val isSos = cachedLoc?.isSosActive ?: false

                    val rag = com.jdroidx.jfindme.utils.DeviceRagEngine.evaluate(lastSeenMs, batteryPct, isSos)
                    val isOnline = (rag.status == com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.GREEN)
                    RosterNode(
                        uid = u1,
                        displayName = item["fullName"] as? String ?: "Unknown",
                        roleTitle = role,
                        statusText = rag.statusText,
                        isAdministrator = isAdmin,
                        isFriend = isFriend,
                        avatarId = item["avatar_id"] as? String,
                        category = classifyCategory(role, isAdmin, isFriend),
                        batteryPercent = batteryPct,
                        isOnline = isOnline,
                        lastSeenTimestamp = lastSeenMs,
                        permissionsMask = pMask
                    ) 
                }
                val sortedCached = cached.sortedWith(
                    compareByDescending<RosterNode> { isChildOrPrimeTarget(it.uid, it.roleTitle, it.displayName) }
                        .thenByDescending { it.batteryPercent <= 15 && it.isOnline }
                        .thenByDescending { it.isOnline }
                        .thenByDescending { it.lastSeenTimestamp }
                )
                _rosterLiveData.postValue(sortedCached) // Zero-Latency Boot with REAL RAG Vitals (Latest/Prime first)
            }
        }

        // 🚀 Unified Live Location Observer: Keeps Family & Friends Roster 100% in sync with Live Map Telemetry
        if (locationObserver == null) {
            val obs = androidx.lifecycle.Observer<List<com.jdroidx.jfindme.data.model.UserLocation>> { locations ->
                val current = _rosterLiveData.value?.toMutableList() ?: return@Observer
                var changed = false
                for (loc in locations) {
                    val uId = loc.getResolvedUserId()
                    val cleanId = uId.removePrefix("HW_")
                    val idx = current.indexOfFirst { it.uid == uId || it.uid == cleanId || it.uid == "HW_$cleanId" }
                    if (idx != -1) {
                        val old = current[idx]
                        if (loc.getLongTimestamp() > old.lastSeenTimestamp) {
                            val ragEval = com.jdroidx.jfindme.utils.DeviceRagEngine.evaluate(loc.getLongTimestamp(), loc.batteryLevel, loc.isSosActive)
                            current[idx] = old.copy(
                                batteryPercent = if (loc.batteryLevel > 0) loc.batteryLevel else old.batteryPercent,
                                lastSeenTimestamp = loc.getLongTimestamp(),
                                isOnline = ragEval.status == com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.GREEN,
                                statusText = ragEval.statusText
                            )
                            changed = true
                        }
                    }
                }
                if (changed) {
                    _rosterLiveData.postValue(current)
                }
            }
            locationObserver = obs
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context.applicationContext).userLocations.observeForever(obs)
                } catch (_: Exception) {}
            }
        }

        // 🛡️ Zero-Trust Offline Gate: If unauthenticated, remain strictly offline using local SQLite cache
        if (FirebaseAuth.getInstance().currentUser == null) {
            SystemLogger.d(TAG, "initializeCloudSync: Offline mode active (unauthenticated), relying on SQLite cache.")
            return
        }

        val finalAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val defaultGroup = "GRP_" + finalAgentId
        
        // [Variables] | [Constants] | [Dependencies] | [Integration Points]
        // Dual-Path Active Group Resolver: Reads /profile/active_group with fallback to /Identity/active_group & SharedPreferences
        com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(finalAgentId).get().addOnSuccessListener { devSnap ->
            val pSnap = devSnap.child("profile")
            val idSnap = devSnap.child("Identity")
            val groupId = pSnap.child("active_group").getValue(String::class.java)
                ?: idSnap.child("active_group").getValue(String::class.java)
                ?: com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(context)
            val effectiveGroup = if (!groupId.isNullOrBlank() && groupId != "DEFAULT_GROUP") groupId else com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(context)
            activeGroupId = effectiveGroup
            bindLiveRosterSocket(effectiveGroup, context)
        }.addOnFailureListener {
            val cachedGroup = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(context)
            activeGroupId = cachedGroup
            bindLiveRosterSocket(cachedGroup, context)
        }
    }

    /**
     * 🚀 AGGRESSIVE RECOVERY: Reconnects to private Circle relationships node.
     */
    fun forceAggressiveRecovery(context: android.content.Context, callback: (Boolean) -> Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            callback(false)
            return
        }
        val finalAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val defaultGroup = "GRP_" + finalAgentId

        com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(finalAgentId).child("profile").child("active_group").get().addOnCompleteListener { grpTask ->
            val groupId = if (grpTask.isSuccessful && grpTask.result != null) grpTask.result!!.getValue(String::class.java) else null
            val effectiveGroup = if (!groupId.isNullOrBlank() && groupId != "DEFAULT_GROUP") groupId else com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(context)

            // Stage 1: Query current active group (unified devices or legacy members)
            val groupRelRef = db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(effectiveGroup)
            groupRelRef.child("devices").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(devSnap: DataSnapshot) {
                    if (devSnap.exists() && devSnap.childrenCount > 1) {
                        resolveRosterIdentities(devSnap, context)
                        callback(true)
                    } else {
                        groupRelRef.child("members").addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists() && snapshot.childrenCount > 1) {
                                    resolveRosterIdentities(snapshot, context)
                                    callback(true)
                                } else {
                                    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
                                    // Stage 2: Targeted Inverse Index Scan (/users/{id}/joined_circles) avoiding global /relationships scan
                                    db.child("users").child(finalAgentId).child("joined_circles")
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(circlesSnap: DataSnapshot) {
                                                var candidateGroup: String? = null
                                                for (cSnap in circlesSnap.children) {
                                                    val gId = cSnap.key
                                                    if (!gId.isNullOrBlank() && gId != "DEFAULT_GROUP") {
                                                        candidateGroup = gId
                                                        break
                                                    }
                                                }

                                                if (candidateGroup != null) {
                                                    com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(finalAgentId).child("profile").child("active_group").setValue(candidateGroup)
                                                    com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(finalAgentId).child("Identity").child("active_group").setValue(candidateGroup)
                                                    context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE).edit().putString("active_group", candidateGroup).apply()
                                                    activeGroupId = candidateGroup
                                                    bindLiveRosterSocket(candidateGroup, context)
                                                    callback(true)
                                                } else {
                                                    // Stage 3: Scan /devices to discover paired fleet nodes
                                                    com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(finalAgentId).child("relationships")
                                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                                            override fun onDataChange(relSnap: DataSnapshot) {
                                                                if (relSnap.exists() && relSnap.childrenCount > 0) {
                                                                    val localDb = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
                                                                    for (child in relSnap.children) {
                                                                        val peerId = child.key ?: continue
                                                                        val peerName = child.child("name").getValue(String::class.java) ?: "Node $peerId"
                                                                        val peerRole = child.child("role").getValue(String::class.java) ?: "Member"
                                                                        localDb.saveRosterNode(peerId, peerName, peerRole, "Active", false, false, "Recovered Peer", "1", effectiveGroup)
                                                                    }
                                                                    initializeCloudSync(context)
                                                                    callback(true)
                                                                } else {
                                                                    callback(false)
                                                                }
                                                            }
                                                            override fun onCancelled(error: DatabaseError) { callback(false) }
                                                        })
                                                }
                                            }
                                            override fun onCancelled(error: DatabaseError) { callback(false) }
                                        })
                                }
                            }
                            override fun onCancelled(error: DatabaseError) { callback(false) }
                        })
                    }
                }
                override fun onCancelled(error: DatabaseError) { callback(false) }
            })
        }
    }

    private var signalListener: ValueEventListener? = null
    private var devicesSocketListener: ValueEventListener? = null
    private val peerLiveListeners = mutableMapOf<String, ValueEventListener>()

    private fun bindLiveRosterSocket(groupId: String, context: android.content.Context) {
        if (rosterListener != null) return // Already socketed

        rosterListener = object : ValueEventListener {
            override fun onDataChange(rosterSnap: DataSnapshot) {
                resolveRosterIdentities(rosterSnap, context)
            }

            override fun onCancelled(error: DatabaseError) {
                SystemLogger.e(TAG, "Roster Socket Collapsed", error.toException())
            }
        }
        db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(groupId).child("members").addValueEventListener(rosterListener!!)

        devicesSocketListener = object : ValueEventListener {
            override fun onDataChange(devSnap: DataSnapshot) {
                if (devSnap.exists()) {
                    resolveRosterIdentities(devSnap, context)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(groupId).child("devices").addValueEventListener(devicesSocketListener!!)

        // 🚀 Real-time closed-loop signal listener
        signalListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val groupNode = db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(groupId)
                    groupNode.child("devices").get().addOnSuccessListener { freshDevSnap ->
                        if (freshDevSnap.exists() && freshDevSnap.childrenCount > 0) {
                            resolveRosterIdentities(freshDevSnap, context)
                        } else {
                            groupNode.child("members").get().addOnSuccessListener { freshMemSnap ->
                                resolveRosterIdentities(freshMemSnap, context)
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(groupId).child("signals").child("refresh_roster").addValueEventListener(signalListener!!)
    }

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Helper to safely extract Long from DataSnapshot (handles String, Number, or ISO/formatted timestamps)
    private fun safeLong(snap: DataSnapshot?): Long? {
        val v = snap?.value ?: return null
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: try {
                com.jdroidx.jfindme.data.model.UserLocation(timestamp = v).getLongTimestamp()
            } catch (_: Exception) { null }
            else -> null
        }
    }

    // Helper to safely extract Int from DataSnapshot (handles String or Number)
    private fun safeInt(snap: DataSnapshot?): Int? {
        val v = snap?.value ?: return null
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }
    }

    private fun resolveRosterIdentities(rosterSnap: DataSnapshot, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedNodes = mutableListOf<RosterNode>()
            val seenAgentIds = mutableSetOf<String>()
            val localDb = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
            val now = System.currentTimeMillis()

            for (nodeSnap in rosterSnap.children) {
            val rawKey = nodeSnap.key ?: continue
            val profileSnap = if (nodeSnap.hasChild("profile")) nodeSnap.child("profile") else nodeSnap
            val telemetrySnap = if (nodeSnap.hasChild("telemetry")) nodeSnap.child("telemetry") else null
            
            // 🛡️ DEDUPLICATION & IDENTIFIER RESOLUTION
            val canonicalAgentId = if (com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(rawKey)) {
                rawKey
            } else {
                val candidate = nodeSnap.child("Identity").child("uid_1").getValue(String::class.java)
                val localResolved = localDb.resolveAgentIdLocally(rawKey)
                when {
                    candidate != null && com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(candidate) -> candidate
                    !localResolved.isNullOrEmpty() && com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(localResolved) -> localResolved
                    else -> rawKey // Fallback to preserve device visibility
                }
            }

            val ctxPrefs = context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
            val selfAuthUid = FirebaseAuth.getInstance().uid ?: ""
            val selfAgentId = ctxPrefs.getString("agent_id", "") ?: ""
            val selfDevUid = ctxPrefs.getString("device_uid", "") ?: ""
            val resolvedLocalAgentId = if (selfAuthUid.isNotEmpty()) localDb.resolveAgentIdLocally(selfAuthUid) else null

            // 🛡️ SELF-NODE EXCLUSION: The host device is displayed exclusively in the top Root Anchor
            if (canonicalAgentId == selfAgentId || canonicalAgentId == selfDevUid || canonicalAgentId == selfAuthUid ||
                (resolvedLocalAgentId != null && canonicalAgentId == resolvedLocalAgentId) ||
                rawKey == selfAgentId || rawKey == selfDevUid || rawKey == selfAuthUid) {
                continue
            }

            // 🛡️ Scoped connected device visibility: strictly isolate to respective connected peers
            val focalId = if (selfAgentId.isNotBlank()) selfAgentId else (resolvedLocalAgentId ?: selfAuthUid)
            val scopedIds = if (focalId.isNotBlank()) localDb.getConnectedDeviceIds(focalId) else emptySet()
            val candidateRole = nodeSnap.child("membership").child("role").getValue(String::class.java)
                ?: nodeSnap.child("role").getValue(String::class.java)
                ?: profileSnap.child("role_title").getValue(String::class.java)
            val isPrime = isChildOrPrimeTarget(canonicalAgentId, candidateRole, localDb.getDeviceCentricName(canonicalAgentId))
            val inScope = isPrime || scopedIds.contains(canonicalAgentId) || scopedIds.contains(rawKey) || (scopedIds.isEmpty() && isPrime)
            if (!inScope) {
                continue
            }

            if (seenAgentIds.contains(canonicalAgentId)) continue
            seenAgentIds.add(canonicalAgentId)

            // 🛡️ SOFT-DELETE & DECOMMISSION FILTER: Exclude any nodes marked as DELETED/UNLINKED in cloud, SQLite or registry
            val isDeletedCloud = nodeSnap.child("is_deleted").getValue(Boolean::class.java) == true 
                || profileSnap.child("status").getValue(String::class.java)?.equals("DELETED", ignoreCase = true) == true
                || profileSnap.child("status").getValue(String::class.java)?.equals("UNLINKED", ignoreCase = true) == true
            val isDecomOrDel = isDeletedCloud || localDb.isDeviceDeleted(canonicalAgentId, activeGroupId) ||
                com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, canonicalAgentId) ||
                com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, rawKey)
            if (isDecomOrDel) {
                continue
            }

            val ssotName = localDb.getDeviceCentricName(canonicalAgentId)
            val ssotAvatar = localDb.getDeviceCentricAvatar(canonicalAgentId)

            val cloudName = profileSnap.child("full_name").getValue(String::class.java)
                ?: profileSnap.child("display_name").getValue(String::class.java)
                ?: profileSnap.child("name").getValue(String::class.java)
                ?: nodeSnap.child("identity").child("name").getValue(String::class.java)
                ?: nodeSnap.child("identity").child("full_name").getValue(String::class.java)
                ?: nodeSnap.child("name").getValue(String::class.java)
                ?: nodeSnap.child("device").child("live").child("userName").getValue(String::class.java)
                
            val name = if (ssotName.isNotEmpty() && !ssotName.startsWith("Agent_", true)) {
                ssotName
            } else if (!cloudName.isNullOrEmpty() && !cloudName.startsWith("Agent_", true)) {
                cloudName
            } else {
                "Agent_$canonicalAgentId"
            }
                
            val roleTitle = nodeSnap.child("membership").child("role").getValue(String::class.java)
                ?: nodeSnap.child("role").getValue(String::class.java)
                ?: profileSnap.child("role_title").getValue(String::class.java)
                ?: "Member"

            val rawAvatar = profileSnap.child("avatar_id").getValue(String::class.java)
                ?: profileSnap.child("equipped_avatar_id").getValue(String::class.java)
                ?: nodeSnap.child("identity").child("avatar_id").getValue(String::class.java)
                ?: nodeSnap.child("avatar_id").getValue(String::class.java)
                ?: nodeSnap.child("device").child("live").child("avatar_id").getValue(String::class.java)

            // 🛡️ ANTI-CONTAMINATION SSOT: Prioritize verified local SSOT avatar over stale group snapshot ("1", "butterfly")
            val avatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly" && ssotAvatar != "1") {
                ssotAvatar
            } else if (!rawAvatar.isNullOrBlank() && rawAvatar != "butterfly" && rawAvatar != "1") {
                rawAvatar
            } else if (ssotAvatar.isNotEmpty()) {
                ssotAvatar
            } else {
                "butterfly"
            }

            // Only update local DB if we have a verified new avatar from live cloud, not an obsolete group snapshot!
            if (!cloudName.isNullOrBlank() && !rawAvatar.isNullOrBlank() && rawAvatar != "1" && rawAvatar != "butterfly") {
                localDb.updateDeviceIdentityAndAvatar(canonicalAgentId, cloudName, avatarId)
            }

            // [Variables] directHb, rawDirectBattery, lastHeartbeat, batteryPct, isSos, rag, isDeviceOnline, statusLabel | [Constants] None | [Dependencies] safeLong, safeInt, DeviceRagEngine, DatabaseHelper | [Integration Points] FamilyFriendsActivity, DeviceVitalsTable
            // Direct Multi-Path Heartbeat Resolution (Unified Cloud Fan-Out) with Type-Safe Deserialization
            val directHb = safeLong(nodeSnap.child("device").child("live").child("last_heartbeat"))
                ?: safeLong(nodeSnap.child("device").child("live").child("timestamp"))
                ?: safeLong(nodeSnap.child("device").child("live").child("updated_at"))
                ?: safeLong(nodeSnap.child("telemetry").child("last_heartbeat"))
                ?: safeLong(nodeSnap.child("telemetry").child("last_seen"))
                ?: safeLong(nodeSnap.child("last_heartbeat"))
                ?: safeLong(nodeSnap.child("last_seen"))
                ?: safeLong(nodeSnap.child("timestamp"))
                ?: safeLong(nodeSnap.child("updated_at"))
            val rawDirectBattery = safeInt(nodeSnap.child("device").child("live").child("battery"))
                ?: safeInt(nodeSnap.child("device").child("live").child("battery_pct"))
                ?: safeInt(nodeSnap.child("battery"))
                ?: safeInt(nodeSnap.child("battery_pct"))
                ?: safeInt(telemetrySnap?.child("battery_pct"))

            val lastHeartbeat = directHb
                ?: safeLong(telemetrySnap?.child("last_heartbeat"))
                ?: safeLong(telemetrySnap?.child("last_seen"))
                ?: safeLong(nodeSnap.child("ts_updated"))
                ?: run {
                    val dbHist = localDb.getLatestHistoricalLocation(canonicalAgentId)
                    (dbHist?.get("timestamp") as? Number)?.toLong()
                }
                ?: 0L

            // GAP-01: Avoid defaulting missing battery to 100%; check local SQLite device_vitals before fallback
            val batteryPct = when {
                rawDirectBattery != null && rawDirectBattery in 0..100 -> rawDirectBattery
                else -> {
                    val dbBatt = localDb.getLatestBatteryLevel(canonicalAgentId)
                    if (dbBatt in 0..100) dbBatt else (rawDirectBattery ?: -1)
                }
            }
            val isSos = telemetrySnap?.child("is_sos")?.getValue(Boolean::class.java)
                ?: nodeSnap.child("is_sos").getValue(Boolean::class.java)
                ?: false
            val rag = com.jdroidx.jfindme.utils.DeviceRagEngine.evaluate(lastHeartbeat, batteryPct, isSos)
            val isDeviceOnline = rag.status == com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.GREEN
            val statusLabel = rag.statusText

            val upper = roleTitle.uppercase()
            val isAdmin = upper.contains("MOTHER") || upper.contains("FATHER") || upper.contains("SPOUSE") || upper.contains("GUARDIAN") || upper.contains("ADMIN")
            val isFriend = upper.contains("FRIEND") || upper.contains("COLLEAGUE") || upper.contains("PEER") || upper.contains("ACQUAINTANCE") || upper.contains("NEIGHBOR")
            val category = classifyCategory(roleTitle, isAdmin, isFriend)

            val assignedZone = localDb.getAssignedSafeZoneForDevice(canonicalAgentId)
            val szName = assignedZone?.get("name")
                ?: nodeSnap.child("device").child("live").child("current_safe_zone_name").getValue(String::class.java)
                ?: nodeSnap.child("safe_zone_name").getValue(String::class.java)
            val szAddr = assignedZone?.get("address")
                ?: nodeSnap.child("device").child("live").child("current_safe_zone_address").getValue(String::class.java)
            val szId = assignedZone?.get("zone_id")

            // [Variables] | [Constants] | [Dependencies] | [Integration Points]
            // Role-Aware ACL Default: Family gets 0xFF (Full Protection); Friends get 0x01 (Mutual GPS View Only)
            val cloudMaskStr = nodeSnap.child("membership").child("permissions_mask").getValue(String::class.java)
                ?: nodeSnap.child("permissions_mask").getValue(String::class.java)
            val pMask = try {
                if (cloudMaskStr != null) {
                    if (cloudMaskStr.startsWith("0x", true)) cloudMaskStr.substring(2).toInt(16) else cloudMaskStr.toInt()
                } else {
                    if (category == "FRIEND") 0x01 else 0xFF
                }
            } catch (e: Exception) {
                if (category == "FRIEND") 0x01 else 0xFF
            }

            resolvedNodes.add(RosterNode(
                uid = canonicalAgentId,
                displayName = name,
                roleTitle = roleTitle,
                statusText = statusLabel,
                isAdministrator = isAdmin,
                isFriend = isFriend,
                avatarId = avatarId,
                category = category,
                isOnline = isDeviceOnline,
                batteryPercent = batteryPct,
                lastSeenTimestamp = lastHeartbeat,
                safeZoneName = szName,
                safeZoneAddress = szAddr,
                safeZoneId = szId,
                permissionsMask = pMask
            ))

            // [Variables] | [Constants] | [Dependencies] | [Integration Points]
            // Real-Time Live Cursor Sentinel: Binds O(1) live listener directly to peer device live node
            if (!peerLiveListeners.containsKey(canonicalAgentId)) {
                val liveQuery = com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(canonicalAgentId).child("device").child("live")
                val liveListener = object : ValueEventListener {
                    override fun onDataChange(liveSnap: DataSnapshot) {
                        if (!liveSnap.exists()) return
                        var liveTs = safeLong(liveSnap.child("last_heartbeat"))
                            ?: safeLong(liveSnap.child("timestamp"))
                        if (liveTs == null || liveTs == 0L) {
                            val upAtObj = liveSnap.child("updated_at").getValue()
                            if (upAtObj is Number) liveTs = upAtObj.toLong()
                            else if (upAtObj is String) {
                                val ul = com.jdroidx.jfindme.data.model.UserLocation(timestamp = upAtObj).apply { this.updated_at = upAtObj }
                                liveTs = ul.getLongTimestamp()
                            }
                        }
                        val rawLiveBattery = safeInt(liveSnap.child("battery"))
                            ?: safeInt(liveSnap.child("battery_pct"))
                            ?: safeInt(liveSnap.child("telemetry").child("battery_pct"))
                        val liveBattery = when {
                            rawLiveBattery != null && rawLiveBattery in 0..100 -> rawLiveBattery
                            batteryPct in 0..100 -> batteryPct
                            else -> {
                                val dbBatt = localDb.getLatestBatteryLevel(canonicalAgentId)
                                if (dbBatt in 0..100) dbBatt else (rawLiveBattery ?: batteryPct)
                            }
                        }
                        val liveSos = liveSnap.child("isSosActive").getValue(Boolean::class.java)
                            ?: liveSnap.child("sos").getValue(Boolean::class.java)
                            ?: false
                        val liveAvatar = liveSnap.child("avatar_id").getValue(String::class.java)
                        val liveName = liveSnap.child("userName").getValue(String::class.java)
                            ?: liveSnap.child("name").getValue(String::class.java)

                        val effectiveTs = liveTs ?: 0L
                        val ragEval = com.jdroidx.jfindme.utils.DeviceRagEngine.evaluate(effectiveTs, liveBattery, liveSos)

                        val currentList = _rosterLiveData.value?.toMutableList() ?: return
                        val idx = currentList.indexOfFirst { it.uid == canonicalAgentId }
                        if (idx != -1) {
                            val old = currentList[idx]
                            val updated = old.copy(
                                displayName = liveName?.takeIf { it.isNotEmpty() && !it.startsWith("Agent_") } ?: old.displayName,
                                avatarId = liveAvatar?.takeIf { it.isNotEmpty() } ?: old.avatarId,
                                batteryPercent = liveBattery,
                                lastSeenTimestamp = effectiveTs,
                                isOnline = ragEval.status == com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.GREEN,
                                statusText = ragEval.statusText
                            )
                            currentList[idx] = updated
                            _rosterLiveData.postValue(currentList)

                            // 🚀 Dual-Engine Closed-Loop: Persist latest peer vitals to local SQLite DeviceVitalsTable
                            viewModelScope.launch(Dispatchers.IO) {
                                localDb.upsertDeviceVitals(
                                    canonicalAgentId,
                                    liveBattery,
                                    isCharging = false,
                                    networkType = "UNKNOWN",
                                    signalDbm = 0,
                                    isMockGps = false,
                                    isRooted = false,
                                    timestamp = effectiveTs
                                )
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                liveQuery.addValueEventListener(liveListener)
                peerLiveListeners[canonicalAgentId] = liveListener
            }

            // 🚀 REAL-TIME PROFILE SYNC: Authoritative Device Root SSOT Resolution
            // [Variables] devSnap, pSnap, lSnap, liveAv, liveNm, liveSz, liveSzAddr, currentList, idx, old, finalNm, finalAv, updated | [Constants] None | [Dependencies] FirebaseComm, DatabaseHelper | [Integration Points] FamilyFriendsActivity, Roster RecyclerView
            com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(canonicalAgentId).get().addOnSuccessListener { devSnap ->
                if (devSnap.exists()) {
                    val pSnap = devSnap.child("profile")
                    val lSnap = devSnap.child("device").child("live")
                    val avatarData = pSnap.child("avatar_data").getValue(String::class.java)
                        ?: lSnap.child("avatar_data").getValue(String::class.java)
                    if (!avatarData.isNullOrEmpty()) {
                        try {
                            val cleanB64 = if (avatarData.contains(",")) avatarData.substringAfter(",") else avatarData
                            val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                            val dir = java.io.File(context.filesDir, "avatars")
                            if (!dir.exists()) dir.mkdirs()
                            java.io.File(dir, "${canonicalAgentId}_custom.png").writeBytes(bytes)
                        } catch (_: Exception) {}
                    }
                    val liveAv = pSnap.child("avatar_id").getValue(String::class.java)
                        ?: pSnap.child("equipped_avatar_id").getValue(String::class.java)
                        ?: lSnap.child("avatar_id").getValue(String::class.java)
                    val liveNm = pSnap.child("fullName").getValue(String::class.java)
                        ?: pSnap.child("full_name").getValue(String::class.java)
                        ?: pSnap.child("name").getValue(String::class.java)
                        ?: lSnap.child("userName").getValue(String::class.java)
                    val liveSz = lSnap.child("current_safe_zone_name").getValue(String::class.java)
                        ?: devSnap.child("safe_zone_name").getValue(String::class.java)
                    val liveSzAddr = lSnap.child("current_safe_zone_address").getValue(String::class.java)

                    val currentList = _rosterLiveData.value?.toMutableList() ?: return@addOnSuccessListener
                    val idx = currentList.indexOfFirst { it.uid == canonicalAgentId }
                    if (idx != -1) {
                        val old = currentList[idx]
                        val finalNm = liveNm?.takeIf { it.isNotEmpty() && !it.startsWith("Agent_") } ?: old.displayName
                        val finalAv = liveAv?.takeIf { it.isNotEmpty() && it != "butterfly" && it != "1" } ?: old.avatarId
                        val updated = old.copy(
                            displayName = finalNm,
                            avatarId = finalAv,
                            safeZoneName = liveSz?.takeIf { it.isNotEmpty() } ?: old.safeZoneName,
                            safeZoneAddress = liveSzAddr?.takeIf { it.isNotEmpty() } ?: old.safeZoneAddress
                        )
                        currentList[idx] = updated
                        _rosterLiveData.postValue(currentList)

                        viewModelScope.launch(Dispatchers.IO) {
                            localDb.updateDeviceIdentityAndAvatar(canonicalAgentId, finalNm, finalAv)
                            localDb.saveRosterNode(
                                canonicalAgentId, updated.displayName, updated.roleTitle, updated.statusText,
                                updated.isAdministrator, updated.isFriend,
                                avatarId = updated.avatarId, activeGroupId = activeGroupId,
                                permissionsMask = cloudMaskStr
                            )
                        }
                    }
                }
            }

            // 🚀 PRIME DIRECTIVE: Backup explicitly verified Firebase Target locally with active group!
            localDb.saveRosterNode(
                canonicalAgentId, name, roleTitle, statusLabel, isAdmin, isFriend, 
                ownerName = "Cloud Roster Mirror", uid2 = null, connectedDeviceId = canonicalAgentId, qrCode = avatarId,
                avatarId = avatarId, activeGroupId = activeGroupId, permissionsMask = cloudMaskStr
            )
            localDb.upsertDeviceVitals(
                canonicalAgentId,
                batteryPct,
                isCharging = false,
                networkType = "UNKNOWN",
                signalDbm = 0,
                isMockGps = false,
                isRooted = false,
                timestamp = lastHeartbeat
            )
        }

        // 🛡️ PERSISTENCE GUARANTEE: Only merge local SQLite nodes if cloud snapshot is empty/offline
        if (!rosterSnap.exists() || rosterSnap.childrenCount == 0L) {
            val offlineRoster = localDb.getOfflineRoster()
            val prefs = context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
            val selfAgentId = prefs.getString("agent_id", "") ?: ""
            val selfAuthUid = FirebaseAuth.getInstance().uid ?: ""
            val resolvedLocalAgentId = if (selfAuthUid.isNotEmpty()) localDb.resolveAgentIdLocally(selfAuthUid) else null

            for (item in offlineRoster) {
                val u1 = item["uid_1"] as? String ?: ""
                if (u1.isEmpty() || u1 == selfAgentId || u1 == selfAuthUid || (resolvedLocalAgentId != null && u1 == resolvedLocalAgentId) || seenAgentIds.contains(u1)) continue
                seenAgentIds.add(u1)

                val role = item["role_type"] as? String ?: "Member"
                val isFriend = when (val fr = item["is_friend"]) {
                    is Boolean -> fr
                    is Number -> fr.toInt() == 1
                    is String -> fr == "1" || fr.equals("true", ignoreCase = true)
                    else -> false
                }
                val isAdmin = when (val adm = item["is_admin"]) {
                    is Boolean -> adm
                    is Number -> adm.toInt() == 1
                    is String -> adm == "1" || adm.equals("true", ignoreCase = true)
                    else -> false
                }
                val pMaskStr2 = item["permissions_mask"]?.toString() ?: "0xFF"
                val pMask2 = try {
                    if (pMaskStr2.startsWith("0x", ignoreCase = true)) pMaskStr2.substring(2).toInt(16)
                    else pMaskStr2.toInt()
                } catch (e: Exception) { 0xFF }
                val rawBatteryPct2 = (item["battery_pct"] as? Number)?.toInt() ?: 100
                val rawLastSeenMs2 = (item["last_seen_ts"] as? Number)?.toLong() ?: 0L
                val locRepo2 = com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context)
                val cachedLoc2 = locRepo2.getLatestLocationFor(u1)
                val lastSeenMs2 = if (cachedLoc2 != null && cachedLoc2.getLongTimestamp() > rawLastSeenMs2) cachedLoc2.getLongTimestamp() else rawLastSeenMs2
                val batteryPct2 = if (cachedLoc2 != null && cachedLoc2.batteryLevel > 0) cachedLoc2.batteryLevel else rawBatteryPct2
                val isSos2 = cachedLoc2?.isSosActive ?: false
                val rag2 = com.jdroidx.jfindme.utils.DeviceRagEngine.evaluate(lastSeenMs2, batteryPct2, isSos2)

                resolvedNodes.add(RosterNode(
                    uid = u1,
                    displayName = item["fullName"] as? String ?: "Cached Device",
                    roleTitle = role,
                    statusText = rag2.statusText,
                    isAdministrator = isAdmin,
                    isFriend = isFriend,
                    avatarId = item["avatar_id"] as? String,
                    category = classifyCategory(role, isAdmin, isFriend),
                    isOnline = (rag2.status == com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.GREEN),
                    batteryPercent = batteryPct2,
                    lastSeenTimestamp = lastSeenMs2,
                    permissionsMask = pMask2
                ))
            }
        }

        // [Variables] sortedNodes, existing | [Constants] None | [Dependencies] rosterUnifiedMap, isChildOrPrimeTarget | [Integration Points] FamilyFriendsActivity, _rosterLiveData
        // GAP-07: Unify roster nodes into concurrent map to prevent clobbering between /members and /devices
        for (node in resolvedNodes) {
            if (localDb.isDeviceDeleted(node.uid)) {
                rosterUnifiedMap.remove(node.uid)
            } else {
                val existing = rosterUnifiedMap[node.uid]
                if (existing != null) {
                    val candAv = node.avatarId
                    val candName = node.displayName
                    val candRole = node.roleTitle
                    val candStatus = node.statusText
                    rosterUnifiedMap[node.uid] = existing.copy(
                        displayName = if (candName.isNotBlank() && !candName.startsWith("Agent_")) candName else existing.displayName,
                        roleTitle = if (candRole.isNotBlank()) candRole else existing.roleTitle,
                        statusText = if (candStatus.isNotBlank()) candStatus else existing.statusText,
                        avatarId = if (!candAv.isNullOrBlank() && candAv != "butterfly" && candAv != "1") candAv else existing.avatarId,
                        batteryPercent = if (node.batteryPercent >= 0) node.batteryPercent else existing.batteryPercent,
                        isOnline = node.isOnline || existing.isOnline,
                        lastSeenTimestamp = maxOf(node.lastSeenTimestamp, existing.lastSeenTimestamp),
                        safeZoneName = node.safeZoneName ?: existing.safeZoneName,
                        safeZoneAddress = node.safeZoneAddress ?: existing.safeZoneAddress,
                        safeZoneId = node.safeZoneId ?: existing.safeZoneId,
                        permissionsMask = if (node.permissionsMask != 0) node.permissionsMask else existing.permissionsMask
                    )
                } else {
                    rosterUnifiedMap[node.uid] = node
                }
            }
        }

        val sortedNodes = rosterUnifiedMap.values.toList().sortedWith(
            compareByDescending<RosterNode> { isChildOrPrimeTarget(it.uid, it.roleTitle, it.displayName) }
                .thenByDescending { it.batteryPercent in 0..15 && it.isOnline }
                .thenByDescending { it.isOnline }
                .thenByDescending { it.lastSeenTimestamp }
        )
        _rosterLiveData.postValue(sortedNodes)
        }
    }

    /**
     * 🚀 Closed-Loop Device Unlink: Removes target from local SQLite ledger and Firebase Group Hub.
     */
    fun unlinkDevice(context: android.content.Context, targetUid: String, callback: ((Boolean) -> Unit)? = null) {
        val grpId = activeGroupId ?: context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE).getString("active_group", null)
        val localDb = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
        // [Variables] | [Constants] | [Dependencies] | [Integration Points]
        // Group-Scoped Unlink: Purges group_members row for active group while preserving global user_identities
        localDb.deleteRosterNode(targetUid, grpId)
        if (!grpId.isNullOrEmpty()) {
            val relRef = db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(grpId)
            relRef.child("devices").child(targetUid).removeValue()
            relRef.child("members").child(targetUid).removeValue()
            relRef.child("citizen_dossiers").child(targetUid).removeValue()
            relRef.child("direct_pairs").child(targetUid).removeValue()
            db.child("users").child(targetUid).child("joined_circles").child(grpId).removeValue()
            relRef.child("signals").child("refresh_roster").setValue(System.currentTimeMillis())
        }

        // 🧬 V68.0 Clean up direct device relationships sub-nodes
        val finalMyId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(finalMyId).child(targetUid).removeValue()
        com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(targetUid).child(finalMyId).removeValue()

        // Evict target listener, decommission from registry, and clear active cache from LocationRepository
        val cleanTid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(targetUid)
        try {
            com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context).removeTarget(targetUid)
            com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context).removeTarget(cleanTid)
            com.jdroidx.jfindme.utils.RegistryManager.decommissionDevice(context, targetUid)
            com.jdroidx.jfindme.utils.RegistryManager.decommissionDevice(context, cleanTid)
        } catch (_: Exception) {}

        // Update in-memory LiveData immediately for zero perceived latency
        rosterUnifiedMap.remove(targetUid)
        rosterUnifiedMap.remove(cleanTid)
        val updated = _rosterLiveData.value?.filter { it.uid != targetUid && it.uid != cleanTid } ?: emptyList()
        _rosterLiveData.postValue(updated)
        callback?.invoke(true)
    }

    /**
     * 🛡️ Master Administrative Purge: Atomically purges target device from all 22 tables and Firebase.
     */
    fun purgeDevice(context: android.content.Context, targetUid: String, callback: ((Boolean) -> Unit)? = null) {
        val grpId = activeGroupId ?: context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE).getString("active_group", null)
        val localDb = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
        localDb.deleteRosterNode(targetUid, grpId)
        localDb.purgeDeviceRecordsMaster(targetUid)
        if (!grpId.isNullOrEmpty()) {
            val relRef = db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(grpId)
            relRef.child("devices").child(targetUid).removeValue()
            relRef.child("members").child(targetUid).removeValue()
            relRef.child("citizen_dossiers").child(targetUid).removeValue()
            relRef.child("direct_pairs").child(targetUid).removeValue()
            db.child("users").child(targetUid).child("joined_circles").child(grpId).removeValue()
            relRef.child("signals").child("refresh_roster").setValue(System.currentTimeMillis())
        }

        val finalMyId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(finalMyId).child(targetUid).removeValue()
        com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(targetUid).child(finalMyId).removeValue()

        try {
            com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context).removeTarget(targetUid)
        } catch (_: Exception) {}

        rosterUnifiedMap.remove(targetUid)
        val updated = _rosterLiveData.value?.filter { it.uid != targetUid } ?: emptyList()
        _rosterLiveData.postValue(updated)
        callback?.invoke(true)
    }

    companion object {
        fun classifyCategory(role: String, isAdmin: Boolean, isFriend: Boolean): String {
            val upper = role.uppercase()
            return when {
                isFriend || upper.contains("FRIEND") || upper.contains("COLLEAGUE") || upper.contains("PEER") || upper.contains("ACQUAINTANCE") || upper.contains("NEIGHBOR") || upper.contains("INNOVANT") -> "FRIEND"
                upper.contains("SPOUSE") || upper.contains("WIFE") || upper.contains("HUSBAND") || upper.contains("PARTNER") -> "SPOUSE"
                isAdmin || upper.contains("GUARDIAN") || upper.contains("FATHER") || upper.contains("MOTHER") || upper.contains("PARENT") || upper.contains("ADMIN") -> "GUARDIAN"
                upper.contains("CHILD") || upper.contains("WARD") || upper.contains("SON") || upper.contains("DAUGHTER") || upper.contains("DEPENDENT") || upper.contains("KID") -> "DEPENDENT_CHILD"
                else -> "FRIEND"
            }
        }
    }

    /**
     * 🧸 PRIME OBJECTIVE: Evaluates whether a roster node is a Child / Protected Ward target (dynamic SSOT via RegistryManager)
     */
    private fun isChildOrPrimeTarget(uid: String, roleTitle: String?, displayName: String?): Boolean {
        val appCtx = com.jdroidx.jfindme.JFindMeApplication.getContext()
        val primeTarget = if (appCtx != null) {
            com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(appCtx))
        } else {
            com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_PRIME_TARGET_ID
        }
        val cleanUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(uid)
        if (cleanUid.equals(primeTarget, ignoreCase = true)) return true
        val primeName = if (appCtx != null) com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetName(appCtx) else com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_PRIME_TARGET_NAME
        if (displayName != null && primeName.isNotBlank() && displayName.contains(primeName, ignoreCase = true)) return true
        val role = roleTitle ?: ""
        return role.contains("Child", ignoreCase = true) ||
               role.contains("Daughter", ignoreCase = true) ||
               role.contains("Son", ignoreCase = true) ||
               role.contains("Ward", ignoreCase = true) ||
               role.contains("Dependent", ignoreCase = true) ||
               role.contains("DEPENDENT_CHILD", ignoreCase = true)
    }

    override fun onCleared() {
        super.onCleared()
        // Kill the Background Socket cleanly on Activity destruction!
        activeGroupId?.let {
            // 🎯 V59.7: Master Cleanup (Unified Path)
            rosterListener?.let { listener ->
                db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(it).child("members").removeEventListener(listener)
            }
            devicesSocketListener?.let { listener ->
                db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(it).child("devices").removeEventListener(listener)
            }
            signalListener?.let { listener ->
                db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(it).child("signals").child("refresh_roster").removeEventListener(listener)
            }
        }
        peerLiveListeners.forEach { (agentId, listener) ->
            com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(agentId).child("device").child("live").removeEventListener(listener)
        }
        peerLiveListeners.clear()
        locationObserver?.let { obs ->
            try {
                com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(com.jdroidx.jfindme.JFindMeApplication.getContext()).userLocations.removeObserver(obs)
            } catch (_: Exception) {}
            locationObserver = null
        }
    }
}


