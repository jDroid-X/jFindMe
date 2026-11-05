package com.jdroidx.jfindme.engine.group

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 🚀 NEXT EVOLUTION: 👥 Group Sessions / Parties Endpoint
 * Isolates your multiplayer visibility pool exclusively to a private channel of Firebase IDs.
 * Bypasses the public global S2 grid completely for high-performance 'Instanced' tracking.
 */
object GroupSessionEngine {

    private const val TAG = "AvatarGroupEngine"
    private val sessionsEndpoint = FirebaseDatabase.getInstance().reference.child("live_parties")
    
    // The Active Roster of UID strings currently rendering on the device
    var activePartyMembers: MutableList<String> = mutableListOf()
        private set

    private var activeSessionId: String? = null
    var isPartyModeEngaged: Boolean = false
        private set

    /**
     * Instigates a private Multiplayer Sandbox channel. Perfect for parent/child direct links
     * or closed friend groups bypassing completely random users scanning the city map.
     */
    fun launchPrivateSession(partyName: String, authorizedUserIds: List<String>) {
        val masterId = FirebaseAuth.getInstance().currentUser?.uid ?: "U123"
        val newSessionId = "PARTY_${System.currentTimeMillis()}"
        
        SystemLogger.i(TAG, "👥 Initiating $partyName... Bootstrapping private Unity sandbox channel: $newSessionId")

        val rosterPayload = mutableMapOf<String, Any>()
        rosterPayload["host_id"] = masterId
        rosterPayload["party_name"] = partyName
        rosterPayload["members"] = authorizedUserIds.associateWith { true }
        
        // Append host to their own roster explicitly
        rosterPayload["members/$masterId"] = true

        sessionsEndpoint.child(newSessionId).updateChildren(rosterPayload)
            .addOnSuccessListener {
                activeSessionId = newSessionId
                isPartyModeEngaged = true
                activePartyMembers.clear()
                activePartyMembers.addAll(authorizedUserIds)
                activePartyMembers.add(masterId)
                
                // Instruct Unity Engine to immediately DELETE all public Avatars currently tracked on the grid!
                broadcastIsolationModeToUnity()
                
                SystemLogger.d(TAG, "Private Party Grid Locked. $partyName online!")
            }
    }

    /**
     * Explicit Security Filter: This strictly intercepts the `MapManager` loops grabbing active public users.
     * Use Case: "If Party Mode == True, ONLY render avatars whose UID exists in our active list"
     */
    fun isUserAllowedInSandbox(uuid: String): Boolean {
        if (!isPartyModeEngaged) return true // Public Mode permits everyone within 300m
        return activePartyMembers.contains(uuid)
    }

    /**
     * Terminate session manually destroying the memory queue preventing orphan ghosts.
     */
    fun dissolveSession() {
        if (activeSessionId == null) return
        
        sessionsEndpoint.child(activeSessionId!!).removeValue()
        activeSessionId = null
        isPartyModeEngaged = false
        activePartyMembers.clear()
        
        SystemLogger.d(TAG, "Private Party Sandbox Suspended. Returning user to Open World GPS array.")
    }
    
    // Polyfill method passing specific security locks straight into the 3D Engine Subsystem
    private fun broadcastIsolationModeToUnity() {
        try {
            val unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer")
            val sendMethod = unityPlayerClass.getMethod("UnitySendMessage", String::class.java, String::class.java, String::class.java)
            sendMethod.invoke(null, "MultiplayerVisibilityManager", "EnableIsolationMode", "true")
        } catch (e: Exception) {
            SystemLogger.d(TAG, "Unity Offline. Syncing isolation mode locally.")
        }
    }
}
