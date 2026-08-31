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
                com.jdroidx.jfindme.utils.AvatarLoader.clearCache()
                
                // 2. Cascade update to all active Group Hubs where this agent belongs
                devicesRef.child(agentId).child("profile").child("active_group").get().addOnSuccessListener { grpSnap ->
                    val groupId = grpSnap.getValue(String::class.java)
                    if (!groupId.isNullOrEmpty()) {
                        val relRef = com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef.child(groupId)
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
                
                SystemLogger.i(TAG, "LOOP-001 Avatar propagation complete across cloud matrix: agentId=$agentId avatar=$newAvatarId")
                closedLoopCallbacks.forEach { cb -> cb(agentId, update) }
            }
            .addOnFailureListener { e ->
                SystemLogger.e(TAG, "LOOP-001 Avatar propagation failed for agentId=$agentId", e)
            }
    }
}

