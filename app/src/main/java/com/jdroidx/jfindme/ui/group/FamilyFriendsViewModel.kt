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
    val safeZoneName: String? = null,
    val safeZoneAddress: String? = null,
    val safeZoneId: String? = null
)

class FamilyFriendsViewModel : ViewModel() {
    private val TAG = "FamilyFriendsViewModel"
    private val db = FirebaseDatabase.getInstance().reference

    private val _rosterLiveData = MutableLiveData<List<RosterNode>>()
    val rosterLiveData: LiveData<List<RosterNode>> = _rosterLiveData

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
                RosterNode(
                    uid = item["uid_1"] as? String ?: "",
                    displayName = item["fullName"] as? String ?: "Unknown",
                    roleTitle = role,
                    statusText = item["st_session_state"] as? String ?: "Cached",
                    isAdministrator = isAdmin,
                    isFriend = isFriend,
                    avatarId = item["avatar_id"] as? String,
                    category = classifyCategory(role, isAdmin, isFriend)
                ) 
            }
            _rosterLiveData.postValue(cached) // Zero-Latency Boot
        }

        val finalAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val defaultGroup = "GRP_" + finalAgentId
        
        // Check profiles for active_group (Standard Hub path)
        com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(finalAgentId).child("profile").child("active_group").get().addOnSuccessListener { grpSnap ->
            val groupId = grpSnap.getValue(String::class.java)
            val effectiveGroup = if (!groupId.isNullOrBlank() && groupId != "DEFAULT_GROUP") groupId else defaultGroup
            activeGroupId = effectiveGroup
            bindLiveRosterSocket(effectiveGroup, context)
        }.addOnFailureListener {
            activeGroupId = defaultGroup
            bindLiveRosterSocket(defaultGroup, context)
        }
    }

    /**
     * 🚀 AGGRESSIVE RECOVERY: Reconnects to private Circle relationships node.
     */
    fun forceAggressiveRecovery(context: android.content.Context, callback: (Boolean) -> Unit) {
        val finalAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val defaultGroup = "GRP_" + finalAgentId

        com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(finalAgentId).child("profile").child("active_group").get().addOnSuccessListener { grpSnap ->
            val groupId = grpSnap.getValue(String::class.java)
            val effectiveGroup = if (!groupId.isNullOrBlank() && groupId != "DEFAULT_GROUP") groupId else defaultGroup

            // 🛰️ Query private relationships members node for this circle only
            db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(effectiveGroup).child("members")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            resolveRosterIdentities(snapshot, context)
                            callback(true)
                        } else { callback(false) }
                    }
                    override fun onCancelled(error: DatabaseError) { callback(false) }
                })
        }
    }

    private var signalListener: ValueEventListener? = null

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

        // 🚀 Real-time closed-loop signal listener
        signalListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(groupId).child("members").get().addOnSuccessListener { freshSnap ->
                        resolveRosterIdentities(freshSnap, context)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(groupId).child("signals").child("refresh_roster").addValueEventListener(signalListener!!)
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
                ?: nodeSnap.child("name").getValue(String::class.java)
                ?: nodeSnap.child("device").child("live").child("userName").getValue(String::class.java)
                
            val name = cloudName?.takeIf { it.isNotEmpty() && !it.startsWith("Agent_", true) }
                ?: ssotName.takeIf { it.isNotEmpty() && !it.startsWith("Agent_", true) }
                ?: cloudName?.takeIf { it.isNotEmpty() }
                ?: "Agent_$canonicalAgentId"
                
            val roleTitle = nodeSnap.child("role").getValue(String::class.java)
                ?: profileSnap.child("role_title").getValue(String::class.java)
                ?: "Member"

            val avatarId = profileSnap.child("avatar_id").getValue(String::class.java)
                ?: profileSnap.child("equipped_avatar_id").getValue(String::class.java)
                ?: nodeSnap.child("avatar_id").getValue(String::class.java)
                ?: nodeSnap.child("device").child("live").child("avatar_id").getValue(String::class.java)
                ?: ssotAvatar.takeIf { it.isNotEmpty() }
                ?: "butterfly"

            if (!cloudName.isNullOrBlank() || !avatarId.isNullOrBlank()) {
                localDb.updateDeviceIdentityAndAvatar(canonicalAgentId, cloudName ?: name, avatarId)
            }

            val lastHeartbeat = telemetrySnap?.child("last_heartbeat")?.getValue(Long::class.java)
                ?: telemetrySnap?.child("last_seen")?.getValue(Long::class.java)
                ?: nodeSnap.child("ts_updated")?.getValue(Long::class.java)
                ?: 0L

            val isDeviceOnline = (now - lastHeartbeat) < (180 * 1000L) || telemetrySnap?.child("status")?.getValue(String::class.java) == "ONLINE"
            val batteryPct = telemetrySnap?.child("battery_pct")?.getValue(Long::class.java)?.toInt() ?: 100
            val statusLabel = if (isDeviceOnline) "🟢 ONLINE" else "🔴 OFFLINE"

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
                safeZoneName = szName,
                safeZoneAddress = szAddr,
                safeZoneId = szId
            ))

            // 🚀 REAL-TIME PROFILE SYNC: Query master device profile node to ensure 100% Avatar parity with Live Map
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

            // 🚀 PRIME DIRECTIVE: Backup explicitly verified Firebase Target locally with active group!
            localDb.saveRosterNode(
                canonicalAgentId, name, roleTitle, statusLabel, isAdmin, isFriend, 
                ownerName = "Cloud Roster Mirror", uid2 = null, connectedDeviceId = canonicalAgentId, qrCode = avatarId,
                avatarId = avatarId, activeGroupId = activeGroupId
            )
        }

        // 🛡️ PERSISTENCE GUARANTEE: Merge any local SQLite paired nodes not in current snapshot (Do NOT disappear!)
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

            resolvedNodes.add(RosterNode(
                uid = u1,
                displayName = item["fullName"] as? String ?: "Cached Device",
                roleTitle = role,
                statusText = "🔴 OFFLINE",
                isAdministrator = isAdmin,
                isFriend = isFriend,
                avatarId = item["avatar_id"] as? String,
                category = classifyCategory(role, isAdmin, isFriend),
                isOnline = false,
                batteryPercent = 100
            ))
        }

        _rosterLiveData.postValue(resolvedNodes)
    }

    /**
     * 🚀 Closed-Loop Device Unlink: Removes target from local SQLite ledger and Firebase Group Hub.
     */
    fun unlinkDevice(context: android.content.Context, targetUid: String, callback: ((Boolean) -> Unit)? = null) {
        val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        localDb.deleteRosterNode(targetUid)

        val grpId = activeGroupId ?: context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE).getString("active_group", null)
        if (!grpId.isNullOrEmpty()) {
            db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(grpId).child("members").child(targetUid).removeValue()
            db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(grpId).child("citizen_dossiers").child(targetUid).removeValue()
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
            signalListener?.let { listener ->
                db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(it).child("signals").child("refresh_roster").removeEventListener(listener)
            }
        }
    }
}


