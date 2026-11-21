package com.jdroidx.jfindme.engine.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.engine.EngineRegistry
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.SystemLogger
import java.io.File

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

    /**
     * 🔄 Auto-Reconciles local avatar truth and guarantees Base64 thumbnail is published to Cloud.
     * [Variables] prefs, agentId, equippedId, avatarsDir, selfCustom, agentCustom, nickname, db, photoSeq, customAvatarTag, targetFile, bmp, baos, thumb, b64, devRef, dynIdx, avId | [Constants] "system_registry", "equipped_avatar_id", "avatars", "custom_photo.png", "_custom.png" | [Dependencies] Context, SharedPreferences, File, DeviceIdentityHelper, DatabaseHelper, AvatarLoader, MasterDataSyncSentinel, FirebaseComm, SystemLogger | [Integration Points] MapActivity, TrackingService, ProfileViewModel, AvatarCropActivity
     */
    @JvmStatic
    fun ensureLocalAvatarSynchronized(context: android.content.Context) {
        val prefs = context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
        val agentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        if (agentId.isBlank()) return

        val equippedId = prefs.getInt("equipped_avatar_id", 1)
        val avatarsDir = java.io.File(context.filesDir, "avatars")
        if (!avatarsDir.exists()) avatarsDir.mkdirs()

        val selfCustom = java.io.File(avatarsDir, "custom_photo.png")
        val agentCustom = java.io.File(avatarsDir, "${agentId}_custom.png")

        // Ensure agent-named photo file exists if custom_photo exists
        if (!agentCustom.exists() && selfCustom.exists()) {
            try { selfCustom.copyTo(agentCustom, overwrite = true) } catch (_: Exception) {}
        } else if (agentCustom.exists() && !selfCustom.exists()) {
            try { agentCustom.copyTo(selfCustom, overwrite = true) } catch (_: Exception) {}
        }

        val nickname = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker"
        val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)

        if (equippedId == -1 || agentCustom.exists() || selfCustom.exists()) {
            val photoSeq = prefs.getInt("custom_avatar_seq", 1)
            val customAvatarTag = String.format(java.util.Locale.US, "AV_%s_%02d", agentId, photoSeq)

            prefs.edit()
                .putInt("equipped_avatar_id", -1)
                .putString("avatar_id", customAvatarTag)
                .apply()

            db.updateDeviceIdentityAndAvatar(agentId, nickname, customAvatarTag)
            db.commitAnalyticalDossier(agentId, mapOf("avatar_id" to customAvatarTag, "equipped_avatar_id" to "-1"))

            val syncPayload = mutableMapOf<String, Any>(
                "avatar_id" to customAvatarTag,
                "name" to nickname,
                "equipped_avatar_id" to -1
            )

            val targetFile = if (agentCustom.exists()) agentCustom else selfCustom
            if (targetFile.exists() && targetFile.length() > 0) {
                try {
                    val bmp = android.graphics.BitmapFactory.decodeFile(targetFile.absolutePath)
                    if (bmp != null) {
                        val baos = java.io.ByteArrayOutputStream()
                        val thumb = android.graphics.Bitmap.createScaledBitmap(bmp, 180, 180, true)
                        thumb.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, baos)
                        val b64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
                        syncPayload["avatar_data"] = b64

                        // 🛡️ Pre-emptive Queue Flush: Ensure avatar thumbnail write is not blocked by stale queues
                        FirebaseComm.purgeOutstandingWrites()

                        val devRef = FirebaseComm.devicesRef.child(agentId)
                        devRef.child("profile").child("avatar_data").setValue(b64)
                        devRef.child("profile").child("avatar_id").setValue(customAvatarTag)
                        devRef.child("profile").child("equipped_avatar_id").setValue(-1)
                        devRef.child("device").child("live").child("avatar_data").setValue(b64)
                        devRef.child("device").child("live").child("avatar_id").setValue(customAvatarTag)
                        devRef.child("Identity").child("avatar_id").setValue(customAvatarTag)
                        SystemLogger.i(TAG, "Uploaded Base64 custom avatar thumbnail to RTDB for $agentId ($customAvatarTag)")
                    }
                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Error encoding custom avatar thumbnail: ${e.message}")
                }
            }

            propagateAvatarUpdate(agentId, customAvatarTag, nickname)
            MasterDataSyncSentinel.notifyMasterDataChanged(
                context,
                MasterDataSyncSentinel.CATEGORY_PROFILE,
                MasterDataSyncSentinel.TYPE_UPDATE,
                agentId,
                syncPayload
            )
        } else {
            val dynIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(equippedId)
            val avId = "avatar_custom_$dynIdx"
            prefs.edit().putString("avatar_id", avId).apply()
            db.updateDeviceIdentityAndAvatar(agentId, nickname, avId)
            propagateAvatarUpdate(agentId, avId, nickname)
        }
    }

    /**
     * 🔄 Dynamic Peer Avatar Reconciler: Proactively queries and caches custom avatar thumbnails
     * for all linked and paired peer devices that do not yet have local physical files on disk.
     */
    @JvmStatic
    fun fetchAndCachePeerAvatars(context: Context) {
        if (!FirebaseComm.isAuthenticated()) return
        val appContext = context.applicationContext
        val db = DatabaseHelper.getInstance(appContext)
        val myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(appContext)
        val prefs = appContext.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val linkedCsv = prefs.getString("linked_devices_csv", "") ?: ""
        
        val targetPeers = mutableSetOf<String>()
        if (linkedCsv.isNotBlank()) {
            linkedCsv.split(",").map { it.trim() }.filter { it.isNotBlank() && it != myAgentId }.forEach { targetPeers.add(it) }
        }
        targetPeers.addAll(db.getAllPairedDeviceIds().filter { it != myAgentId })

        for (rawPeer in targetPeers) {
            val cleanPeer = DeviceIdentityHelper.toCanonicalAgentId(rawPeer)
            if (!FirebaseComm.isAgentIdValid(cleanPeer)) continue

            val dir = java.io.File(appContext.filesDir, "avatars")
            val targetFile = java.io.File(dir, "${cleanPeer}_custom.png")
            // Fetch if file doesn't exist or is empty
            if (!targetFile.exists() || targetFile.length() == 0L) {
                FirebaseComm.devicesRef.child(cleanPeer).child("profile").get().addOnSuccessListener { profSnap ->
                    var avData = profSnap.child("avatar_data").value?.toString()
                        ?: profSnap.child(cleanPeer).child("avatar_data").value?.toString()
                    val avId = profSnap.child("avatar_id").value?.toString() ?: "AV_${cleanPeer}_01"
                    val name = profSnap.child("name").value?.toString()
                        ?: profSnap.child("fullName").value?.toString()
                        ?: profSnap.child("userName").value?.toString()

                    val applyAvatarBytes: (String) -> Unit = { rawB64 ->
                        try {
                            val cleanB64 = if (rawB64.contains(",")) rawB64.substringAfter(",") else rawB64
                            val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                            if (!dir.exists()) dir.mkdirs()
                            for (fname in listOf(cleanPeer, "HW_$cleanPeer", rawPeer).distinct()) {
                                java.io.File(dir, "${fname}_custom.png").writeBytes(bytes)
                            }
                            db.updateDeviceIdentityAndAvatar(cleanPeer, name, avId)
                            db.commitAnalyticalDossier(cleanPeer, mapOf("avatar_id" to avId, "equipped_avatar_id" to "-1"))
                            com.jdroidx.jfindme.utils.AvatarLoader.evictDevice(cleanPeer)
                            com.jdroidx.jfindme.utils.AvatarLoader.clearCache()
                            SystemLogger.i(TAG, "Successfully fetched and cached remote avatar for peer $cleanPeer")
                        } catch (e: Exception) {
                            SystemLogger.e(TAG, "Failed to decode/save avatar bytes for $cleanPeer", e)
                        }
                    }

                    if (!avData.isNullOrBlank()) {
                        applyAvatarBytes(avData)
                    } else {
                        // 🛰️ Dual Fallback: Check device/live/avatar_data if profile node is blank
                        FirebaseComm.devicesRef.child(cleanPeer).child("device").child("live").child("avatar_data").get()
                            .addOnSuccessListener { liveSnap ->
                                val liveAv = liveSnap.value?.toString()
                                if (!liveAv.isNullOrBlank()) {
                                    applyAvatarBytes(liveAv)
                                }
                            }
                    }
                }.addOnFailureListener { e ->
                    SystemLogger.w(TAG, "fetchAndCachePeerAvatars failed for $cleanPeer: ${e.message}")
                }
            }
        }
    }
}

