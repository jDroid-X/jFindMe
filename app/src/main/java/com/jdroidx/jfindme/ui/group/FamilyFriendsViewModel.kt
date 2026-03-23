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
    val isFriend: Boolean
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
        val localDb = com.jdroidx.jfindme.data.local.CitizenDbHelper(context)
        val offlineNodes = localDb.getOfflineRoster()
        if (offlineNodes.isNotEmpty()) {
            val cached = offlineNodes.map { 
                RosterNode(
                    uid = it["uid"] as? String ?: "",
                    displayName = it["display_name"] as? String ?: "Unknown",
                    roleTitle = it["role_title"] as? String ?: "Unknown",
                    statusText = it["status_text"] as? String ?: "Loading...",
                    isAdministrator = it["is_admin"] as? Boolean ?: false,
                    isFriend = it["is_friend"] as? Boolean ?: false
                ) 
            }
            _rosterLiveData.postValue(cached) // Zero-Latency Boot
        }

        val myUid = FirebaseAuth.getInstance().uid ?: return

        db.child("users").child(myUid).child("active_group").get().addOnSuccessListener { grpSnap ->
            val groupId = grpSnap.getValue(String::class.java)
            if (groupId.isNullOrEmpty()) {
                if(offlineNodes.isEmpty()) _rosterLiveData.postValue(emptyList()) // No Group = Blank Array ONLY if no offline cache
                return@addOnSuccessListener
            }
            activeGroupId = groupId
            bindLiveRosterSocket(groupId, context)
        }.addOnFailureListener {
            SystemLogger.e(TAG, "Failed resolving Active Group Hub", it)
        }
    }

    private fun bindLiveRosterSocket(groupId: String, context: android.content.Context) {
        if (rosterListener != null) return // Already socketed

        rosterListener = object : ValueEventListener {
            override fun onDataChange(membersSnap: DataSnapshot) {
                val uidList = membersSnap.children.mapNotNull { it.key }
                resolveNodeIdentities(uidList, context)
            }

            override fun onCancelled(error: DatabaseError) {
                SystemLogger.e(TAG, "Roster Socket Collapsed", error.toException())
            }
        }
        db.child("groups").child(groupId).child("members").addValueEventListener(rosterListener!!)
    }

    private fun resolveNodeIdentities(uids: List<String>, context: android.content.Context) {
        val resolvedNodes = mutableListOf<RosterNode>()
        var resolutionCount = 0

        if (uids.isEmpty()) {
            _rosterLiveData.postValue(emptyList())
            return
        }

        for (uid in uids) {
            // First Pull: Core Identity Flags
            db.child("users").child(uid).get().addOnSuccessListener { userSnap ->
                val isMother = userSnap.child("role_mother").getValue(Boolean::class.java) ?: false
                val isFather = userSnap.child("role_father").getValue(Boolean::class.java) ?: false
                val isSpouse = userSnap.child("role_spouse").getValue(Boolean::class.java) ?: false
                val isFriend = userSnap.child("role_friends").getValue(Boolean::class.java) ?: false

                // Second Pull: Physical Render Metrics (Locations Database)
                db.child("locations").child(uid).child("userName").get().addOnSuccessListener { nameSnap ->
                    val name = nameSnap.getValue(String::class.java) ?: "Target ($uid)"

                    var roleTitle = "CHILD SLOT"
                    var isAdmin = false

                    if (isMother) { roleTitle = "MOTHER SLOT"; isAdmin = true }
                    else if (isFather) { roleTitle = "FATHER SLOT"; isAdmin = true }
                    else if (isSpouse) { roleTitle = "SPOUSE SLOT"; isAdmin = true }
                    else if (isFriend) { roleTitle = "FRIEND SLOT"; isAdmin = false }

                    resolvedNodes.add(RosterNode(
                        uid = uid,
                        displayName = name,
                        roleTitle = roleTitle,
                        statusText = "🟢 LIVE TARGET TRACKED",
                        isAdministrator = isAdmin,
                        isFriend = isFriend
                    ))
                    
                    // 🚀 PRIME DIRECTIVE: Backup explicitly verified Firebase Target locally!
                    com.jdroidx.jfindme.data.local.CitizenDbHelper(context).saveRosterNode(uid, name, roleTitle, "🟢 LIVE TARGET TRACKED", isAdmin, isFriend)

                    // Synchronization Loop Restraint
                    resolutionCount++
                    if (resolutionCount == uids.size) {
                        _rosterLiveData.postValue(resolvedNodes)
                    }
                }.addOnFailureListener {
                    resolutionCount++
                    if (resolutionCount == uids.size) _rosterLiveData.postValue(resolvedNodes)
                }
            }.addOnFailureListener {
                resolutionCount++
                if (resolutionCount == uids.size) _rosterLiveData.postValue(resolvedNodes)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Kill the Background Socket cleanly on Activity destruction!
        activeGroupId?.let {
            rosterListener?.let { listener ->
                db.child("groups").child(it).child("members").removeEventListener(listener)
            }
        }
    }
}
