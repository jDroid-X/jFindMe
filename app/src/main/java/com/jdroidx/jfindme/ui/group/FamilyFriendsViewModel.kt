package com.jdroidx.jfindme.ui.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.utils.SystemLogger

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

    fun getRosterNodes(): LiveData<List<RosterNode>> = rosterLiveData

    private var rosterListener: ValueEventListener? = null
    private var activeGroupId: String? = null

    fun initializeCloudSync(context: android.content.Context) {
        val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val offlineNodes = localDb.getOfflineRoster()
        val prefs = context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
        val selfAuthUid = FirebaseAuth.getInstance().uid ?: ""
        val selfAgentId = prefs.getString("agent_id", "") ?: ""
        val selfDevUid = prefs.getString("device_uid", "") ?: ""
        val resolvedLocalAgentId = if (selfAuthUid.isNotEmpty()) localDb.resolveAgentIdLocally(selfAuthUid) else null

        if (offlineNodes.isNotEmpty()) {
            val cached = offlineNodes.filterNot { item ->
                val u1 = item["uid_1"] as? String ?: ""
                val u2 = item["uid_2"] as? String ?: ""
                (u1.isNotEmpty() && (u1 == selfAuthUid || u1 == selfAgentId || u1 == selfDevUid || (resolvedLocalAgentId != null && u1 == resolvedLocalAgentId))) ||
                (u2.isNotEmpty() && (u2 == selfDevUid || u2 == selfAgentId || (resolvedLocalAgentId != null && u2 == resolvedLocalAgentId)))
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
                RosterNode(
                    uid = item["uid_1"] as? String ?: "",
                    displayName = item["fullName"] as? String ?: "Unknown",
                    roleTitle = role,
                    statusText = item["st_session_state"] as? String ?: "Cached",
                    isAdministrator = isAdmin,
                    isFriend = isFriend,
                    avatarId = item["avatar_id"] as? String,
                    category = classifyCategory(role, isAdmin, isFriend),
                    permissionsMask = pMask
                ) 
            }
            _rosterLiveData.postValue(cached) // Zero-Latency Boot
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
                ?: context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE).getString("active_group", null)
            val effectiveGroup = if (!groupId.isNullOrBlank() && groupId != "DEFAULT_GROUP") groupId else defaultGroup
            activeGroupId = effectiveGroup
            bindLiveRosterSocket(effectiveGroup, context)
        }.addOnFailureListener {
            val cachedGroup = context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE).getString("active_group", defaultGroup) ?: defaultGroup
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
            val effectiveGroup = if (!groupId.isNullOrBlank() && groupId != "DEFAULT_GROUP") groupId else defaultGroup

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
                                                                    val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
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
        val resolvedNodes = mutableListOf<RosterNode>()
        val seenAgentIds = mutableSetOf<String>()
        val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
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

            if (seenAgentIds.contains(canonicalAgentId)) continue
            seenAgentIds.add(canonicalAgentId)

            // 🛡️ SOFT-DELETE FILTER: Exclude any nodes marked as DELETED in cloud or SQLite
            val isDeletedCloud = nodeSnap.child("is_deleted").getValue(Boolean::class.java) == true 
                || profileSnap.child("status").getValue(String::class.java)?.equals("DELETED", ignoreCase = true) == true
            if (isDeletedCloud || localDb.isDeviceDeleted(canonicalAgentId)) {
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
                
            val name = cloudName?.takeIf { it.isNotEmpty() && !it.startsWith("Agent_", true) }
                ?: ssotName.takeIf { it.isNotEmpty() && !it.startsWith("Agent_", true) }
                ?: cloudName?.takeIf { it.isNotEmpty() }
                ?: "Agent_$canonicalAgentId"
                
            val roleTitle = nodeSnap.child("membership").child("role").getValue(String::class.java)
                ?: nodeSnap.child("role").getValue(String::class.java)
                ?: profileSnap.child("role_title").getValue(String::class.java)
                ?: "Member"

            val avatarId = profileSnap.child("avatar_id").getValue(String::class.java)
                ?: profileSnap.child("equipped_avatar_id").getValue(String::class.java)
                ?: nodeSnap.child("identity").child("avatar_id").getValue(String::class.java)
                ?: nodeSnap.child("avatar_id").getValue(String::class.java)
                ?: nodeSnap.child("device").child("live").child("avatar_id").getValue(String::class.java)
                ?: ssotAvatar.takeIf { it.isNotEmpty() }
                ?: "butterfly"

            if (!cloudName.isNullOrBlank() || !avatarId.isNullOrBlank()) {
                localDb.updateDeviceIdentityAndAvatar(canonicalAgentId, cloudName ?: name, avatarId)
            }

            // [Variables] | [Constants] | [Dependencies] | [Integration Points]
            // Direct Multi-Path Heartbeat Resolution (Unified Cloud Fan-Out) with Type-Safe Deserialization
            val directHb = safeLong(nodeSnap.child("last_heartbeat"))
                ?: safeLong(nodeSnap.child("last_seen"))
                ?: safeLong(nodeSnap.child("timestamp"))
            val directBattery = safeInt(nodeSnap.child("battery"))
                ?: safeInt(nodeSnap.child("battery_pct"))
                ?: safeInt(telemetrySnap?.child("battery_pct"))
                ?: 100

            val lastHeartbeat = directHb
                ?: safeLong(telemetrySnap?.child("last_heartbeat"))
                ?: safeLong(telemetrySnap?.child("last_seen"))
                ?: safeLong(nodeSnap.child("ts_updated"))
                ?: 0L

            val batteryPct = directBattery
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
                        val liveBattery = safeInt(liveSnap.child("battery")) ?: batteryPct
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
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                liveQuery.addValueEventListener(liveListener)
                peerLiveListeners[canonicalAgentId] = liveListener
            }

            // 🚀 REAL-TIME PROFILE SYNC: High-Performance Cache Check
            // [Variables] | [Constants] | [Dependencies] | [Integration Points]
            // O(1) Scalability Gate: Only execute individual cloud .get() if profile or avatar is missing from the parent group snapshot!
            val hasCompleteIdentity = !cloudName.isNullOrBlank() && !avatarId.isNullOrBlank() && avatarId != "butterfly" && avatarId != "1"
            if (!hasCompleteIdentity) {
            com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(canonicalAgentId).get().addOnSuccessListener { devSnap ->
                if (devSnap.exists()) {
                    val pSnap = devSnap.child("profile")
                    val lSnap = devSnap.child("device").child("live")
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
                        val updated = old.copy(
                            displayName = liveNm?.takeIf { it.isNotEmpty() } ?: old.displayName,
                            avatarId = liveAv?.takeIf { it.isNotEmpty() } ?: old.avatarId,
                            safeZoneName = liveSz?.takeIf { it.isNotEmpty() } ?: old.safeZoneName,
                            safeZoneAddress = liveSzAddr?.takeIf { it.isNotEmpty() } ?: old.safeZoneAddress
                        )
                        currentList[idx] = updated
                        _rosterLiveData.postValue(currentList)

                        localDb.saveRosterNode(
                            canonicalAgentId, updated.displayName, updated.roleTitle, updated.statusText,
                            updated.isAdministrator, updated.isFriend,
                            avatarId = updated.avatarId, activeGroupId = activeGroupId
                        )
                    }
                }
            }
            }

            // 🚀 PRIME DIRECTIVE: Backup explicitly verified Firebase Target locally with active group!
            localDb.saveRosterNode(
                canonicalAgentId, name, roleTitle, statusLabel, isAdmin, isFriend, 
                ownerName = "Cloud Roster Mirror", uid2 = null, connectedDeviceId = canonicalAgentId, qrCode = avatarId,
                avatarId = avatarId, activeGroupId = activeGroupId
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

                resolvedNodes.add(RosterNode(
                    uid = u1,
                    displayName = item["fullName"] as? String ?: "Cached Device",
                    roleTitle = role,
                    statusText = item["st_session_state"] as? String ?: "Offline",
                    isAdministrator = isAdmin,
                    isFriend = isFriend,
                    avatarId = item["avatar_id"] as? String,
                    category = classifyCategory(role, isAdmin, isFriend),
                    isOnline = false,
                    batteryPercent = 100,
                    permissionsMask = pMask2
                ))
            }
        }

        _rosterLiveData.postValue(resolvedNodes)
    }

    /**
     * 🚀 Closed-Loop Device Unlink: Removes target from local SQLite ledger and Firebase Group Hub.
     */
    fun unlinkDevice(context: android.content.Context, targetUid: String, callback: ((Boolean) -> Unit)? = null) {
        val grpId = activeGroupId ?: context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE).getString("active_group", null)
        val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
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

        // Update in-memory LiveData immediately for zero perceived latency
        val updated = _rosterLiveData.value?.filter { it.uid != targetUid } ?: emptyList()
        _rosterLiveData.postValue(updated)
        callback?.invoke(true)
    }

    companion object {
        fun classifyCategory(role: String, isAdmin: Boolean, isFriend: Boolean): String {
            val upper = role.uppercase()
            return when {
                isFriend || upper.contains("FRIEND") || upper.contains("COLLEAGUE") || upper.contains("PEER") || upper.contains("ACQUAINTANCE") || upper.contains("NEIGHBOR") -> "FRIEND"
                upper.contains("SPOUSE") || upper.contains("WIFE") || upper.contains("HUSBAND") || upper.contains("PARTNER") -> "SPOUSE"
                isAdmin || upper.contains("GUARDIAN") || upper.contains("FATHER") || upper.contains("MOTHER") || upper.contains("PARENT") || upper.contains("ADMIN") -> "GUARDIAN"
                else -> "DEPENDENT_CHILD"
            }
        }
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
    }
}


