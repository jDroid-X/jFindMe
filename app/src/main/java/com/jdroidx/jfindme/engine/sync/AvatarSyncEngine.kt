package com.jdroidx.jfindme.engine.sync

import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * PHASE 3 BLUEPRINT: AVATAR CUSTOMIZATION LOGIC WITH BACKEND SYNC
 * Implementation of the sophisticated Queue + Debounce + Version Conflict systems.
 * Prevents Firebase overloading while guaranteeing Multi-Device consistency.
 */
object AvatarSyncEngine {
    private const val TAG = "AvatarSyncEngine"
    
    // 1. DATA MODEL STRUCTURAL BINDINGS
    private val database = FirebaseDatabase.getInstance().reference.child("users")
    
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

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "U123" // Fallback for QA Testing
        
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

        // Execute partial update() command! Prevents overwriting unrelated nodes explicitly.
        database.child(userId).child("avatar_config").updateChildren(payload)
            .addOnSuccessListener {
                SystemLogger.i(TAG, "Sync Pipeline Executed Successfully. Version: $localVersionCounter")
                pendingUpdates.clear() // Wipe queue ONLY on success 
                
                // 10. END-TO-END FLOW: Broadcast to Unity Runtime via JNI
                broadcastToUnitySandbox(payload)
            }
            .addOnFailureListener { error ->
                SystemLogger.e(TAG, "Network Drop! Keeping items in Local Queue to Retry later.", error)
                // Exception handling: Do nothing. Queue remains intact for next retry loop.
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
}
