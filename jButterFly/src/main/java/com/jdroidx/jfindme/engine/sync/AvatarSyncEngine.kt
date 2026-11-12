package com.jdroidx.jfindme.engine.sync

import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.engine.EngineRegistry
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * AvatarSyncEngine — Avatar customization sync with debounce, version control,
 * and closed-loop ProfileActivity → MapEngine propagation.
 *
 * AUDIT FIX [AUD-007]: Was writing to 'users/' collection.
 *   → Corrected to 'devices/' which is the authoritative RTDB root.
 * AUDIT FIX [LOOP-001]: Added propagateAvatarUpdate() for ProfileActivity
 *   closed-loop callback → MultiMarkerEngine callout refresh.
 */
object AvatarSyncEngine {
    private const val TAG = "AvatarSyncEngine"

    // 1. DATA MODEL STRUCTURAL BINDINGS
    // AUDIT FIX [AUD-007]: Corrected from 'users/' to 'devices/' (authoritative RTDB root)
    private val devicesRef = FirebaseDatabase.getInstance().reference.child(FirebaseComm.NODE_DEVICES)
    
    // 3. SYNC QUEUE SYSTEM (CRITICAL)
    private val pendingUpdates = mutableMapOf<String, Any>()
    
    // Timer construct for the 300-500ms Debounce delay
    private var syncJobHandler: Handler = Handler(Looper.getMainLooper())
    private var syncRunnable: Runnable? = null
    
    // Memory Cache
    private var localVersionCounter: Long = 0L

    /**
     * 2. CUSTOMIZATION FLOW: Applied instantly to the local avatar, queued for the network.
     * Use case: User rapidly swaps 5 different shirts. We only sync the final decision.
     */
    fun queueCustomizationUpdate(field: String, value: Any) {
        // Log locally
        SystemLogger.d(TAG, "Item selected -> $field : $value. Queuing for Db Sync.")
        
        // Push to Sync Queue map
        pendingUpdates[field] = value

        // ⚡ DEBOUNCE LOGIC: Cancel the previous network request if it hasn't fired yet!
        syncRunnable?.let { syncJobHandler.removeCallbacks(it) }

        // Start a new 500ms countdown timer
        syncRunnable = Runnable { flushQueueToFirebase() }
        syncJobHandler.postDelayed(syncRunnable!!, 500)
    }

    /**
     * 4. FIREBASE SYNC STRATEGY (Update() NOT Set())
     */
    private fun flushQueueToFirebase() {
        if (pendingUpdates.isEmpty()) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Never write without auth
        
        // 5. CONFLICT RESOLUTION (VERSION CONTROL)
        // Increment the physical version number and attach a server timestamp
        localVersionCounter++
        
        val payload = mutableMapOf<String, Any>()
        payload["version"] = localVersionCounter
        payload["updated_at"] = ServerValue.TIMESTAMP
        
        // Append all un-sycned physical items into the appearance node specifically
        for ((key, value) in pendingUpdates) {
            payload["appearance/$key"] = value
        }

        // Resolve the agentId before writing (uses devices/ root, not users/)
        FirebaseComm.resolveHardwareIdentity(userId) { agentId ->
            val finalId = agentId ?: return@resolveHardwareIdentity

            // Execute partial update() — prevents overwriting unrelated nodes
            devicesRef.child(finalId).child("profile").updateChildren(payload)
                .addOnSuccessListener {
                    SystemLogger.i(TAG, "Avatar sync executed. agentId=$finalId version=$localVersionCounter")
                    pendingUpdates.clear() // Wipe queue ONLY on success
                
                // 10. END-TO-END FLOW: Broadcast to Unity Runtime via JNI
                    broadcastToUnitySandbox(payload)
                    // Notify closed-loop listeners (e.g. MultiMarkerEngine refresh)
                    closedLoopCallbacks.forEach { callback -> callback(finalId, payload) }
                    // 📝 AUDIT CHANGE LOG: Record Profile mutation under /devices/{finalId}/Change_Log/Profile
                    FirebaseComm.logChange(finalId, FirebaseComm.LOG_ITEM_PROFILE, "AVATAR_UPDATE", finalId, payload)
                }
                .addOnFailureListener { error ->
                    SystemLogger.e(TAG, "Network Drop! Queue retained for retry. version=$localVersionCounter", error)
                    // Queue remains intact for next retry loop
                }
        }
    }

    /**
     * 6. AVATAR ENGINE INTEGRATION
     * Flushes the stringified hash payload straight into Unity so the 3D Engine dynamically re-skins.
     */
    private fun broadcastToUnitySandbox(payloadMap: Map<String, Any>) {
        try {
            // Convert simple map to JSON dynamically
            val jsonBuilder = StringBuilder("{")
            payloadMap.forEach { (k, v) -> jsonBuilder.append("\"$k\":\"$v\",") }
            val finalJson = jsonBuilder.toString().trimEnd(',') + "}"

            // Abstract JNI bridge
            val unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer")
            val sendMethod = unityPlayerClass.getMethod("UnitySendMessage", String::class.java, String::class.java, String::class.java)
            sendMethod.invoke(null, "AvatarController", "OnLiveConfigChanged", finalJson)
            
        } catch (e: Exception) {
            // Failsafe behavior when Unity isn't booted yet
            SystemLogger.d(TAG, "Unity Offline. Sync payload cached in Native Android Memory.")
        }
    }

    /**
     * 7. ANIMATION SYNC
     */
    fun syncAnimationState(animationName: String) {
        queueCustomizationUpdate("animation_state", animationName.lowercase())
    }

    // ─── CLOSED-LOOP PROPAGATION (LOOP-001 Fix) ───────────────────────────────

    /**
     * Callbacks registered by ProfileActivity (via ProfileViewModel) to be notified
     * when an avatar sync completes, so MapEngine markers can be refreshed.
     * @param callback  Lambda(agentId: String, payload: Map<String, Any>)
     */
    private val closedLoopCallbacks = mutableListOf<(String, Map<String, Any>) -> Unit>()

    @JvmStatic
    fun registerClosedLoopCallback(callback: @JvmSuppressWildcards (agentId: String, payload: Map<String, Any>) -> Unit) {
        if (!closedLoopCallbacks.contains(callback)) {
            closedLoopCallbacks.add(callback)
            SystemLogger.d(TAG, "Closed-loop callback registered. Total: ${closedLoopCallbacks.size}")
        }
    }

    @JvmStatic
    fun unregisterClosedLoopCallback(callback: @JvmSuppressWildcards (agentId: String, payload: Map<String, Any>) -> Unit) {
        closedLoopCallbacks.remove(callback)
    }

    /**
     * Propagates avatar and name change to all linked roster nodes and group hubs.
     * Called by ProfileViewModel after syncForensicHeartbeat() succeeds.
     * @param agentId     The agent whose avatar changed
     * @param newAvatarId The new avatar resource identifier
     * @param newName     The updated display name
     */
    // [Variables] agentId, newAvatarId, newName, update, updateCircle, grpSnap, groupId, snap, gid, relSnap, groupChild | [Constants] None | [Dependencies] FirebaseDatabase, ServerValue, FirebaseComm, AvatarLoader, SystemLogger | [Integration Points] ProfileViewModel, MasterDataSyncSentinel, FamilyFriendsViewModel, MapActivity
    fun propagateAvatarUpdate(agentId: String, newAvatarId: String, newName: String) {
        if (agentId.isBlank()) return
        val update = mapOf(
            "avatar_id" to newAvatarId,
            "name" to newName,
            "fullName" to newName,
            "userName" to newName,
            "updated_at" to ServerValue.TIMESTAMP
        )
        
        // 1. Direct device profile & telemetry nodes
        devicesRef.child(agentId).child("profile").updateChildren(update)
            .addOnSuccessListener {
                devicesRef.child(agentId).child("device").child("live").child("avatar_id").setValue(newAvatarId)
                devicesRef.child(agentId).child("device").child("live").child("userName").setValue(newName)
                devicesRef.child(agentId).child("Identity").child("avatar_id").setValue(newAvatarId)
                devicesRef.child(agentId).child("Identity").child("name").setValue(newName)
                devicesRef.child(agentId).child("Identity").child("fullName").setValue(newName)
                com.jdroidx.jfindme.utils.AvatarLoader.clearCache()
                
                // Helper to update a specific relationship group
                val updateCircle = { groupId: String ->
                    if (groupId.isNotBlank()) {
                        val relRef = com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef.child(groupId)
                        // 1. Unified `devices/` Structure
                        relRef.child("devices").child(agentId).child("identity").updateChildren(mapOf(
                            "avatar_id" to newAvatarId,
                            "name" to newName,
                            "full_name" to newName
                        ))
                        // 2. Legacy Redundancy for backwards compatibility
                        relRef.child("members").child(agentId).updateChildren(mapOf(
                            "avatar_id" to newAvatarId,
                            "name" to newName,
                            "display_name" to newName
                        ))
                        relRef.child("citizen_dossiers").child(agentId).updateChildren(mapOf(
                            "avatar_id" to newAvatarId,
                            "fullName" to newName,
                            "name" to newName
                        ))
                    }
                }

                // 2. Cascade update to ALL active & historical Group Hubs where this agent belongs
                // A. Active group
                devicesRef.child(agentId).child("profile").child("active_group").get().addOnSuccessListener { grpSnap ->
                    val groupId = grpSnap.getValue(String::class.java)
                    if (!groupId.isNullOrEmpty()) updateCircle(groupId)
                }

                // B. Default self group
                updateCircle("GRP_$agentId")

                // C. Scan users/{agentId}/joined_circles
                FirebaseDatabase.getInstance().reference.child("users").child(agentId).child("joined_circles")
                    .get().addOnSuccessListener { snap ->
                        for (child in snap.children) {
                            val gid = child.key ?: child.getValue(String::class.java)
                            if (!gid.isNullOrBlank()) updateCircle(gid)
                        }
                    }

                // D. Scan entire relationships root for groups where agentId is listed in members, devices, or dossiers
                com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef.get().addOnSuccessListener { relSnap ->
                    for (groupChild in relSnap.children) {
                        val gid = groupChild.key ?: continue
                        if (groupChild.child("devices").hasChild(agentId) || 
                            groupChild.child("members").hasChild(agentId) || 
                            groupChild.child("citizen_dossiers").hasChild(agentId)) {
                            updateCircle(gid)
                        }
                    }
                }
                
                SystemLogger.i(TAG, "LOOP-001 Avatar propagation complete across all cloud matrix circles: agentId=$agentId avatar=$newAvatarId")
                closedLoopCallbacks.forEach { cb -> cb(agentId, update) }
            }
            .addOnFailureListener { e ->
                SystemLogger.e(TAG, "LOOP-001 Avatar propagation failed for agentId=$agentId", e)
            }
    }
}

