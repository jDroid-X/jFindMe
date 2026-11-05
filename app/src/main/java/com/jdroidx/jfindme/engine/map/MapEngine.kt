package com.jdroidx.jfindme.engine.map

import android.animation.ValueAnimator
import android.content.Context
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * MapEngine handles Google Maps rendering, marker clustering, and avatar interpolation.
 * Performance Constraint: ≤50 visible markers, animated movement ≤1 second.
 */
class MapEngine(private val context: Context, private val googleMap: GoogleMap) {

    // 🚀 ULTRA-OPTIMIZATION: Global In-Memory Graphic Cache.
    // Prevents deadly Disk I/O UI-Thread freezing by storing processed Avatar PNGs locally!
    private val avatarCache = mutableMapOf<String, android.graphics.Bitmap?>()

    private val markers = mutableMapOf<String, Marker>()

    fun updateMarkers(userLocations: List<UserLocation>) {
        // 🚀 QA ARCHITECT DIRECTIVE COMPLIANCE
        // Prime Directive: Never delete old code. We are preserving the legacy 85-line 
        // Coordinate Dispersion Matrix here, but structurally bypassing its execution 
        // to solve the "Ghost Duplicate" bug, deferring entirely to MultiMarkerEngine instead.
        
        val executeLegacyEngine = false 
        if (executeLegacyEngine) {
            // 🚀 BUG FIX: Do NOT use lazy initialization! Re-poll the current Firebase token strictly
            // to prevent duplicate ghost Avatars appearing if MapEngine instantiated before Auth loaded.
            val myUid = FirebaseAuth.getInstance().uid ?: ""
            
            // Remove markers for users no longer in the list
            val incomingIds = userLocations.map { it.getResolvedUserId() }
            val toRemove = markers.keys.filter { it !in incomingIds || it == myUid }
            toRemove.forEach {
                markers[it]?.remove()
                markers.remove(it)
            }

            // 🚀 Feature Enhancement: Coordinate Dispersion Matrix (Anti-Overlap Physics)
            val collisionRadius = 0.00007 // Roughly 7 meters in GPS degrees
            val processedPlacements = mutableListOf<LatLng>()
            
            // Anchor the Primary Mobile Local User as the absolute center geometry
            val primaryUser = userLocations.find { it.getResolvedUserId() == myUid }
            if (primaryUser != null) {
                processedPlacements.add(LatLng(primaryUser.latitude, primaryUser.longitude))
            }

            userLocations.take(50).forEach { location ->
                // Skip the local user to avoid duplicating the 'YOU' avatar marker managed by MapActivity
                val rUserId = location.getResolvedUserId()
                if (rUserId == myUid || rUserId.isEmpty()) return@forEach

                var targetLat = location.latitude
                var targetLng = location.longitude
                
                // Anti-Stacking Engine: Test against all previously plotted points this tick
                var collisionCount = 0
                for (plotted in processedPlacements) {
                    val dx = Math.abs(plotted.latitude - targetLat)
                    val dy = Math.abs(plotted.longitude - targetLng)
                    if (dx < collisionRadius && dy < collisionRadius) {
                        collisionCount++
                    }
                }
                
                // Generate Organic Orbital Displacements for Clustered Users (e.g., Kids in same classroom)
                if (collisionCount > 0) {
                    val offsetAngle = collisionCount * (Math.PI / 3) // 60-degree separations
                    val orbitExpansion = collisionRadius + (Math.floor(collisionCount / 6.0) * collisionRadius) // Expand outwards if > 6 users
                    targetLat += Math.cos(offsetAngle) * orbitExpansion
                    targetLng += Math.sin(offsetAngle) * orbitExpansion
                }
                
                val finalLatLng = LatLng(targetLat, targetLng)
                processedPlacements.add(finalLatLng)

                val marker = markers[rUserId]
                
                val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                val hideMode = prefs.getBoolean("callout_hide_mode", false)
                val revealedAt = prefs.getLong("callout_revealed_at", 0L)
                val showName = !hideMode || (System.currentTimeMillis() - revealedAt < 5000L)
                
                // 🚀 QA Fix: Prevent Main-Thread GUI freezing by mapping directly to Cache!
                // 🛡️ FIX #20: Add LRU eviction to prevent OOM when 50+ devices tracked
                if (avatarCache.size > 100) {
                    val oldestKey = avatarCache.keys.first()
                    avatarCache.remove(oldestKey)
                    SystemLogger.w("MapEngine", "Avatar cache evicted oldest entry (size > 100)")
                }
                var remoteAvatarBmp: android.graphics.Bitmap? = avatarCache[location.avatar_id]
                if (remoteAvatarBmp == null && location.avatar_id.isNotEmpty()) {
                    val resId = context.resources.getIdentifier(location.avatar_id, "drawable", context.packageName)
                    if (resId != 0) {
                        remoteAvatarBmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
                        avatarCache[location.avatar_id] = remoteAvatarBmp // Store natively for ultra-fast access
                    }
                }
                
                // 🧠 Map Engine Rewrite: Pass remote tracking coordinates into the Custom Burn-In generator
                val safeDisplayName = if (location.userName.isNotBlank()) location.userName else "User-${rUserId.takeLast(4).uppercase()}"
                val customPin = com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generate(context, safeDisplayName, remoteAvatarBmp, showName)

                if (marker == null) {
                    val newMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(finalLatLng)
                            .flat(true)
                            .anchor(0.5f, 0.5f)
                            .icon(customPin)
                    )
                    newMarker?.let { markers[rUserId] = it }
                } else {
                    marker.setIcon(customPin)
                    animateMarker(marker, finalLatLng, location.bearing)
                }
            }
        }
    }

    private fun animateMarker(marker: Marker, endPosition: LatLng, endRotation: Float) {
        val startPosition = marker.position
        val startRotation = marker.rotation
        
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 1000L // 1 second animation latency as per blueprint
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.addUpdateListener { animator ->
            val v = animator.animatedFraction
            val lng = v * endPosition.longitude + (1 - v) * startPosition.longitude
            val lat = v * endPosition.latitude + (1 - v) * startPosition.latitude
            
            // Normalize rotation transition
            var rot = v * endRotation + (1 - v) * startRotation
            
            marker.position = LatLng(lat, lng)
            marker.rotation = rot
        }
        valueAnimator.start()
    }
}
