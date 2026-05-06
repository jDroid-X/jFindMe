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
    val avatarId: String? = null // 🚀 V52: Identity Link
)

class FamilyFriendsViewModel : ViewModel() {
    private val TAG = "FamilyFriendsViewModel"
    private val db = FirebaseDatabase.getInstance().reference

    private val _rosterLiveData = MutableLiveData<List<RosterNode>>()
    val rosterLiveData: LiveData<List<RosterNode>> = _rosterLiveData

    private var rosterListener: ValueEventListener? = null
    private var activeGroupId: String? = null

    fun initializeCloudSync(context: android.content.Context) {
        // 🚀 PRIME DIRECTIVE: Yield the Local SQLite Database completely offline first!
        val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val offlineNodes = localDb.getOfflineRoster()
        if (offlineNodes.isNotEmpty()) {
            val cached = offlineNodes.map { 
                RosterNode(
                    uid = it["uid_1"] as? String ?: "",
                    displayName = it["fullName"] as? String ?: "Unknown",
                    roleTitle = it["role_type"] as? String ?: "Unknown",
                    statusText = it["st_session_state"] as? String ?: "Loading...", // V55 unified
                    isAdministrator = it["is_admin"]?.toString() == "1",
                    isFriend = (it["is_friend"] as? Long ?: 1L) == 1L,
                    avatarId = it["avatar_id"] as? String
                ) 
            }
            _rosterLiveData.postValue(cached) // Zero-Latency Boot
        }

        val myUid = FirebaseAuth.getInstance().uid ?: return

        // 🎯 V58 PROTOCOL: Resolve Agent ID from Multi-Node Query Hub
        com.jdroidx.jfindme.data.remote.FirebaseComm.resolveHardwareIdentity(myUid) { agentId ->
            val finalAgentId = agentId ?: "LEGACY_NODE"
            
            // Check profiles for active_group (Standard Hub path)
            com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(finalAgentId).child("profile").child("active_group").get().addOnSuccessListener { grpSnap ->
                val groupId = grpSnap.getValue(String::class.java)
                if (groupId.isNullOrEmpty()) {
                    if(offlineNodes.isEmpty()) _rosterLiveData.postValue(emptyList())
                    return@addOnSuccessListener
                }
                activeGroupId = groupId
                bindLiveRosterSocket(groupId, context)
            }.addOnFailureListener {
                SystemLogger.e(TAG, "Failed resolving Active Group from Hub", it)
            }
        }
    }

    /**
     * 🚀 AGGRESSIVE RECOVERY: Deep-scans the cloud matrix for target IDs sharing the same 
     * Group Hub. Essential for full roster restoration after a factory reset.
     */
    fun forceAggressiveRecovery(context: android.content.Context, callback: (Boolean) -> Unit) {
        val myUid = FirebaseAuth.getInstance().uid ?: return callback(false)
        
        com.jdroidx.jfindme.data.remote.FirebaseComm.resolveHardwareIdentity(myUid) { agentId ->
            val finalAgentId = agentId ?: return@resolveHardwareIdentity callback(false)

            com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(finalAgentId).child("profile").child("active_group").get().addOnSuccessListener { grpSnap ->
                val groupId = grpSnap.getValue(String::class.java)
                if (groupId.isNullOrEmpty()) return@addOnSuccessListener callback(false)

                // 🛰️ V61 Query Hub: Search across all Master Agent Nodes for matching group keys
                com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.orderByChild("profile/active_group").equalTo(groupId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                // Extract nested profile blobs for recovery
                                resolveRosterIdentities(snapshot, context)
                                callback(true)
                            } else { callback(false) }
                        }
                        override fun onCancelled(error: DatabaseError) { callback(false) }
                    })
            }
        }
    }

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
        // 🎯 V59.7: Use Unified 'members' path to eliminate fragmentation
        db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(groupId).child("members").addValueEventListener(rosterListener!!)
    }

    private fun resolveRosterIdentities(rosterSnap: DataSnapshot, context: android.content.Context) {
        val resolvedNodes = mutableListOf<RosterNode>()
        val myUid = FirebaseAuth.getInstance().uid ?: "U_001"

        for (nodeSnap in rosterSnap.children) {
            val uid = nodeSnap.key ?: continue
            // 🎯 V61 Hub Resolve: Check for nested profile if this is a direct Agent scan
            val profileSnap = if (nodeSnap.hasChild("profile")) nodeSnap.child("profile") else nodeSnap
            
            // ✅ DEFECT 4 FIX: relationships/{groupId}/members only stores role + ts_joined — no name fields.
            // Firebase profile names are under devices/{agentId}/profile/, not under members/.
            // Fall back to local SQLite dossier which has the correct name from registration.
            val cloudName = profileSnap.child("full_name").getValue(String::class.java)
                ?: profileSnap.child("display_name").getValue(String::class.java)
                ?: profileSnap.child("name").getValue(String::class.java)
            val localName = if (cloudName.isNullOrEmpty()) {
                com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                    .getLocalDossier(uid)["fullName"]
                    ?.takeIf { it.isNotEmpty() }
            } else null
            val name = cloudName?.takeIf { it.isNotEmpty() }
                ?: localName
                ?: "Target ($uid)"
                
            val roleTitle = nodeSnap.child("role").getValue(String::class.java)
                ?: profileSnap.child("role_title").getValue(String::class.java)
                ?: "P2P GUEST"
            val avatarId = profileSnap.child("avatar_id").getValue(String::class.java)

            // Determine admin status from role title (Forensic Rule)
            val upper = roleTitle.uppercase()
            val isAdmin = upper.contains("MOTHER") || upper.contains("FATHER") || upper.contains("SPOUSE") || upper.contains("GUARDIAN") || upper == "ADMIN"
            val isFriend = !isAdmin && !upper.contains("CHILD")

            resolvedNodes.add(RosterNode(
                uid = uid,
                displayName = name,
                roleTitle = roleTitle,
                statusText = "🟢 LIVE TARGET TRACKED",
                isAdministrator = isAdmin,
                isFriend = isFriend,
                avatarId = avatarId
            ))

            // 🚀 PRIME DIRECTIVE: Backup explicitly verified Firebase Target locally!
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).saveRosterNode(
                uid, name, roleTitle, "🟢 LIVE TARGET TRACKED", isAdmin, isFriend, 
                ownerName = "Cloud Roster Mirror", uid2 = myUid, connectedDeviceId = uid, qrCode = avatarId
            )
        }
        _rosterLiveData.postValue(resolvedNodes)
    }

    override fun onCleared() {
        super.onCleared()
        // Kill the Background Socket cleanly on Activity destruction!
        activeGroupId?.let {
            // 🎯 V59.7: Master Cleanup (Unified Path)
            rosterListener?.let { listener ->
                db.child(com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS).child(it).child("members").removeEventListener(listener)
            }
        }
    }
}

