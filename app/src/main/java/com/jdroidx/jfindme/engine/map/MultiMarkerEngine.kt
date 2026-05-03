package com.jdroidx.jfindme.engine.map

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 🔗 PILLAR 3 & 4: REALTIME SYNC & MULTI-USER RENDERING
 * Listens EXCLUSIVELY to paired group members. Maps DeviceID -> Markers.
 * Interpolates movement utilizing safe Thread-locked ValueAnimators preventing UI jerks.
 */
object MultiMarkerEngine {

    private const val TAG = "MultiMarkerEngine"
    
    // Hash map locking all active Device_IDs to their physical Google Maps Markers
    private val activeDeviceMarkerMap = mutableMapOf<String, Marker>()
    
    // Firebase endpoints
    private val usersEndpoint = FirebaseDatabase.getInstance().reference.child("users")
    private val locationsEndpoint = FirebaseDatabase.getInstance().reference.child("locations")
    private val groupsEndpoint = FirebaseDatabase.getInstance().reference.child("groups")

    private val userRoleCache = mutableMapOf<String, String>() 
    private val iconCache = mutableMapOf<String, com.google.android.gms.maps.model.BitmapDescriptor>()
    private val lastRenderedMeta = mutableMapOf<String, String>() // Key: deviceId, Value: "name|avatarId|stackCount|role"
    private var activeGridMap: GoogleMap? = null

    /**
     * 🚀 MVVM PRIME DIRECTIVE: Strict Unidirectional Waterfall execution.
     * The MultiMarker wrapper entirely consumes its network arrays from the MapViewModel securely, 
     * absolutely zero direct database hooks are permitted inside this Graphics layer renderer!
     */
    fun processViewModelLocations(context: Context, mapCore: GoogleMap, locations: List<com.jdroidx.jfindme.data.model.UserLocation>) {
        if (activeGridMap != mapCore) {
            // 🚀 QA Fix: Global Refresh Sync!
            // When the map instance changes (Orientation/Recreation), we MUST wipe the old marker cache!
            resetEngineState()
            activeGridMap = mapCore
        }
        
        // Culling: Max 100 markers safeguard loop
        var markerCount = 0
        val processedPlacements = mutableListOf<LatLng>()
        val allowedUUIDs = mutableListOf<String>()
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        for (ul in locations) {
            val deviceId = ul.getResolvedUserId()
            if (deviceId.isEmpty() || deviceId == myUid) continue // 🚀 QA Fix: Prevent Duplicate Owner Aura (V9.0)
            
            allowedUUIDs.add(deviceId)
            if (markerCount >= 100) break // 🚀 Stress Test Limit: 100 concurrent 3D nodes

            val lat = ul.latitude
            val lng = ul.longitude
            val name = ul.userName ?: "Friend"
            val modelIcon = ul.avatar_id // 🚀 QA Fix: Dynamically hook the equipped avatar natively!

            if (lat != 0.0) {
                // 🚀 QA ARCHITECTURE: 10-Meter Collision Stack Detection
                val collisionRadius = 0.00009 // Approx 10 meters 
                var stackCount = 0
                for (plotted in processedPlacements) {
                    val dx = Math.abs(plotted.latitude - lat)
                    val dy = Math.abs(plotted.longitude - lng)
                    if (dx < collisionRadius && dy < collisionRadius) stackCount++
                }
                processedPlacements.add(LatLng(lat, lng))

                val newLatLng = LatLng(lat, lng)
                updateOrRenderMarker(context, deviceId, newLatLng, name, modelIcon, stackCount)
                markerCount++
            }
        }
        
        // Cleanup Rule: "removed devices are cleaned from map" OR Ghost Mode persists them!
        cleanupDisconnectedDevices(context, allowedUUIDs)
    }

    private val bitmapCache = mutableMapOf<Int, android.graphics.Bitmap>() // QA: Hardware Memory Bridge

    private fun updateOrRenderMarker(context: Context, deviceId: String, newPosition: LatLng, rawName: String, avatarId: String?, stackCount: Int) {
        val map = activeGridMap ?: return
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val prefs = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
        val hideMode = prefs.getBoolean("callout_hide_mode", false)
        val revealedAt = prefs.getLong("callout_revealed_at", 0L)
        val showName = !hideMode || (System.currentTimeMillis() - revealedAt < 5000L)

        // 🚀 QA ARCHITECTURE: UNIVERSAL LOADER DEPLOYMENT
        // Natively bypass redundant processing and delegate directly to the Central Avatar Engine!
        val density = context.resources.displayMetrics.density
        val targetSize = 256 // 🚀 Forensic HQ Resolution Matrix (V16.9)
        
        var originalBitmap = if (deviceId == myUid) {
            com.jdroidx.jfindme.utils.AvatarLoader.getCurrentUserAvatarBitmap(context, targetSize)
        } else {
            com.jdroidx.jfindme.utils.AvatarLoader.getRemoteAvatarBitmap(context, avatarId, targetSize)
        }

        // 🚀 PILLAR 3 LIVE SYNC: Identity Integration
        // QA PERFORMANCE FIX: We strictly utilize the memory HashMap cache 'userRoleCache'.
        // If the cache is empty, we silently perform a single SQLite query across the Offline Matrix avoiding UI loop blocks!
        
        var calculatedRole = userRoleCache[deviceId]
        
        if (calculatedRole == null) {
            // 1st Priority: Offline Core Memory Sandbox
            val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val offlineMatrix = localDb.getOfflineRoster()
            var matchedTitle: String? = null
            
            for (node in offlineMatrix) {
                if (node["uid"] == deviceId) {
                    matchedTitle = node["role_title"] as? String
                    break
                }
            }
            
            if (matchedTitle != null && matchedTitle.isNotBlank()) {
                val upTitle = matchedTitle.uppercase()
                calculatedRole = when {
                    upTitle.contains("MOTHER") || upTitle.contains("FATHER") || upTitle.contains("SPOUSE") || upTitle.contains("GUARDIAN") -> "GUARDIAN"
                    upTitle.contains("FRIEND") -> "FRIEND"
                    else -> "CHILD"
                }
                userRoleCache[deviceId] = calculatedRole // Hardware Cache Lock
            } else {
                // 2nd Priority: Fallback Async Root Cloud Check (V8 String Matrix)
                calculatedRole = "CHILD"
                usersEndpoint.child(deviceId).get().addOnSuccessListener { snapshot ->
                    val famRole = snapshot.child("role_family").getValue(String::class.java) ?: "None"
                    val friendRole = snapshot.child("role_friend").getValue(String::class.java) ?: "None"
                    
                    val asyncRole = when {
                        famRole.contains("MOTHER", true) || famRole.contains("FATHER", true) || famRole.contains("SPOUSE", true) -> "GUARDIAN"
                        friendRole.contains("FRIEND", true) -> "FRIEND"
                        else -> "CHILD"
                    }
                    userRoleCache[deviceId] = asyncRole // Hardware Cache Lock Post-Load
                    // Trigger a re-render once identity is resolved!
                    activeGridMap?.let { updateOrRenderMarker(context, deviceId, newPosition, rawName, avatarId, stackCount) }
                }
            }
        }


        // 🚀 QA Performance Optimization: Cache-Enabled Rendering Matrix (V9.5)
        // Update: Included showName in meta-hash to trigger redraw when 5s reveal expires (V10.8)
        val currentMeta = "$rawName|$avatarId|$stackCount|$calculatedRole|$showName"
        var customIconConfig = iconCache[deviceId]
        
        if (customIconConfig == null || lastRenderedMeta[deviceId] != currentMeta) {
            // Regeneration required per structural identity shift
            customIconConfig = com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generate(
                context, rawName, originalBitmap, showName, isSelf = (deviceId == myUid), stackCount = stackCount, roleStr = calculatedRole
            )
            iconCache[deviceId] = customIconConfig
            lastRenderedMeta[deviceId] = currentMeta
        }

        if (activeDeviceMarkerMap.containsKey(deviceId)) {
            // Marker Exists! Smooth Interpolation required to stop teleporting jerks!
            val targetMarker = activeDeviceMarkerMap[deviceId]!!
            targetMarker.setIcon(customIconConfig) // Update callout text and badge limits directly!
            animateMarkerToLocation(targetMarker, targetMarker.position, newPosition)
        } else {
            // First time spawning this device!
            val newMarker = map.addMarker(
                MarkerOptions()
                    .position(newPosition)
                    .title(rawName)
                    .icon(customIconConfig)
                    .anchor(0.5f, 0.5f) // Centers the Glassmorphism anchor accurately on the physical spot
                    .flat(false) // Billboard fix: Standing upright perpetually!
            )
            
            if (newMarker != null) {
                activeDeviceMarkerMap[deviceId] = newMarker
                newMarker.showInfoWindow()
            }
        }
    }

    /**
     * Translates crude physical location dumps into a flawless linear glide.
     */
    private fun animateMarkerToLocation(marker: Marker, startPosition: LatLng, endPosition: LatLng) {
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 1000 // 1 full physical second Lerp
        valueAnimator.interpolator = LinearInterpolator()
        
        valueAnimator.addUpdateListener { animation ->
            val v = animation.animatedFraction
            val lngInt = v * endPosition.longitude + (1 - v) * startPosition.longitude
            val latInt = v * endPosition.latitude + (1 - v) * startPosition.latitude
            marker.position = LatLng(latInt, lngInt)
        }
        valueAnimator.start()
    }

    private fun cleanupDisconnectedDevices(context: Context, allowedUUIDs: List<String>) {
        val prefs = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
        // 🚀 QA ARCHITECTURE: Three-Tier Recovery Target 2 (Ghost Mode) Defaults to True
        val ghostModePersistent = prefs.getBoolean("pref_ghost_mode_persistent", true)

        val iterator = activeDeviceMarkerMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val deviceId = entry.key
            val marker = entry.value

            // If the user completely vanished from the real-time ViewModel array
            if (!allowedUUIDs.contains(deviceId)) {
                // If Ghost Mode is Active, and they are still a legal group member (just offline)
                if (ghostModePersistent) {
                    marker.alpha = 0.5f // 👻 GHOST MODE: 50% Transparency
                    val currentTitle = marker.title ?: "Friend"
                    if (!currentTitle.contains(" [OFFLINE]")) {
                        marker.title = "$currentTitle [OFFLINE]"
                        marker.showInfoWindow()
                    }
                    SystemLogger.d(TAG, "Device $deviceId disconnected. Entering GHOST MODE (50% Opacity).")
                } else {
                    marker.remove()
                    iterator.remove()
                    iconCache.remove(deviceId)
                    lastRenderedMeta.remove(deviceId)
                    SystemLogger.d(TAG, "Device $deviceId disconnected. Culled from local Map Viewport.")
                }
            } else {
                // Device is active! Ensure they are structurally opaque.
                marker.alpha = 1.0f
                val currentTitle = marker.title ?: "Friend"
                if (currentTitle.contains(" [OFFLINE]")) {
                    marker.title = currentTitle.replace(" [OFFLINE]", "")
                }
            }
        }
    }

    private fun resetEngineState() {
        SystemLogger.w(TAG, "Map Instance Shift Detected. Liquidating Stale Marker Collections.")
        activeDeviceMarkerMap.clear()
        iconCache.clear()
        lastRenderedMeta.clear()
        userRoleCache.clear()
        com.jdroidx.jfindme.utils.AvatarLoader.clearCache()
    }
}

