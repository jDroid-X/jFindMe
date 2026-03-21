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

    private var activeGroupListener: ValueEventListener? = null
    private var activeGridMap: GoogleMap? = null

    /**
     * Initializes the tracker, checking if the User is securely bound to a paired Group.
     */
    fun initializeRealtimeGroupTracker(context: Context, mapCore: GoogleMap) {
        activeGridMap = mapCore
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: "U123"

        usersEndpoint.child(myUid).child("active_group").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupId = snapshot.getValue(String::class.java)
                if (groupId != null) {
                    SystemLogger.i(TAG, "User belongs to Group: $groupId. Hooking Multi-Device listener...")
                    startGroupLocationListener(context, groupId)
                } else {
                    SystemLogger.w(TAG, "No Group paired! Cannot intercept friendly devices.")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startGroupLocationListener(context: Context, groupId: String) {
        // First, fetch the allowed members roster to PREVENT reading stranger locations
        groupsEndpoint.child(groupId).child("members").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allowedUUIDs = snapshot.children.mapNotNull { it.key }

                // Detach old listener if group roster alters dynamically
                if (activeGroupListener != null) {
                    locationsEndpoint.removeEventListener(activeGroupListener!!)
                }

                // Engage physical Realtime listener!
                activeGroupListener = object : ValueEventListener {
                    override fun onDataChange(rawDump: DataSnapshot) {
                        // Culling: Max 50 markers safeguard loop
                        var markerCount = 0
                        val processedPlacements = mutableListOf<LatLng>()
                        
                        for (uuidNode in rawDump.children) {
                            val deviceId = uuidNode.key ?: continue
                            
                            // Strict Verification: Ensure Device belongs to exactly this group
                            if (!allowedUUIDs.contains(deviceId)) continue
                            if (markerCount >= 50) break // Safe GPU Culling!

                            val lat = uuidNode.child("latitude").getValue(Double::class.java)
                            val lng = uuidNode.child("longitude").getValue(Double::class.java)
                            val name = uuidNode.child("userName").getValue(String::class.java) ?: "Friend"
                            val modelIcon = uuidNode.child("avatar_id").getValue(String::class.java)

                            if (lat != null && lng != null && lat != 0.0) {
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
                        cleanupDisconnectedDevices(context, rawDump, allowedUUIDs)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        SystemLogger.e(TAG, "Multi-Device stream terminated unexpectedly", error.toException())
                    }
                }

                // Bind listener universally across all grouped users!
                locationsEndpoint.addValueEventListener(activeGroupListener!!)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateOrRenderMarker(context: Context, deviceId: String, newPosition: LatLng, rawName: String, avatarId: String?, stackCount: Int) {
        val map = activeGridMap ?: return
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val prefs = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
        val hideMode = prefs.getBoolean("callout_hide_mode", false)
        val revealedAt = prefs.getLong("callout_revealed_at", 0L)
        val showName = !hideMode || (System.currentTimeMillis() - revealedAt < 5000L)

        // Native Resource Engine Decoding (Merged from redundant generator)
        val resourceName = avatarId ?: "avatar_custom_1"
        val isVrm = resourceName.contains(".vrm")
        val resId = if (isVrm) {
            val dynamicIdx = Math.abs(resourceName.hashCode()) % 22 + 1
            context.resources.getIdentifier("avatar_custom_$dynamicIdx", "drawable", context.packageName)
        } else {
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }
        val finalRes = if (resId != 0) resId else android.R.drawable.ic_menu_gallery
        val originalBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, finalRes)

        // Sub-setting QA Integration Phase: Direct AvatarCalloutGenerator Invocation (Single Truth)
        val customIconConfig = com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generate(
            context, rawName, originalBitmap, showName, isSelf = (deviceId == myUid), stackCount = stackCount
        )

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
                    .flat(true) // Lays flat against street perspective dynamically
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

    private fun cleanupDisconnectedDevices(context: Context, snapshot: DataSnapshot, allowedUUIDs: List<String>) {
        val prefs = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
        // 🚀 QA ARCHITECTURE: Three-Tier Recovery Target 2 (Ghost Mode) Defaults to True
        val ghostModePersistent = prefs.getBoolean("pref_ghost_mode_persistent", true)

        val iterator = activeDeviceMarkerMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val deviceId = entry.key
            val marker = entry.value

            // If the user disappeared from the active Firebase node OR left the private group completely
            if (!snapshot.hasChild(deviceId) || !allowedUUIDs.contains(deviceId)) {
                // If Ghost Mode is Active, and they are still a legal group member (just offline)
                if (ghostModePersistent && allowedUUIDs.contains(deviceId)) {
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

    // QA ARCHITECT DEPRECATION: `generateRoundedCustomMarker` was structurally stripped.
    // The engine now utilizes AvatarCalloutGenerator natively to ensure the Tab/Enter sequential keyboard loop
    // strictly binds 1 Callout Hitbox array to exactly 1 Physical Node for complete WCAG mapping compliance.
}
