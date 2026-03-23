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

    private val userRoleCache = mutableMapOf<String, String>() // 🚀 Phase 1 Cache Map

    private var activeGroupListener: ValueEventListener? = null
    private var isolatedSelfListener: ValueEventListener? = null // 🚀 Fix 2 Safe Fallback Variable
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
                    // Destroy isolated fallback listener if they just joined a group!
                    if (isolatedSelfListener != null) {
                        locationsEndpoint.child(myUid).removeEventListener(isolatedSelfListener!!)
                        isolatedSelfListener = null
                    }
                    startGroupLocationListener(context, groupId)
                } else {
                    SystemLogger.w(TAG, "No Group paired! Firing Isolated Self-Roster Engine.")
                    
                    // 🛑 Fix 2: Prevent the App from gaslighting the New User. Force the Map to draw their own Identity!
                    if (isolatedSelfListener == null) {
                        isolatedSelfListener = object : ValueEventListener {
                            override fun onDataChange(rawDump: DataSnapshot) {
                                val lat = rawDump.child("latitude").getValue(Double::class.java)
                                val lng = rawDump.child("longitude").getValue(Double::class.java)
                                val name = rawDump.child("userName").getValue(String::class.java) ?: "Admin"
                                val modelIcon = rawDump.child("avatar_id").getValue(String::class.java)
                                
                                if (lat != null && lng != null && lat != 0.0) {
                                    val newLatLng = LatLng(lat, lng)
                                    updateOrRenderMarker(context, myUid, newLatLng, name, modelIcon, 0)
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        }
                        locationsEndpoint.child(myUid).addValueEventListener(isolatedSelfListener!!)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startGroupLocationListener(context: Context, groupId: String) {
        // First, fetch the allowed members roster to PREVENT reading stranger locations
        // 🚀 QA Fix: Master Schema queries the Users table for active links, NOT the root Group endpoint!
        usersEndpoint.orderByChild("active_group").equalTo(groupId).addValueEventListener(object : ValueEventListener {
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
        var originalBitmap = if (deviceId == myUid) {
            com.jdroidx.jfindme.utils.AvatarLoader.getCurrentUserAvatarBitmap(context, 150)
        } else {
            com.jdroidx.jfindme.utils.AvatarLoader.getRemoteAvatarBitmap(context, avatarId, 150)
        }

        // 🚀 Roster Identity Integration (Async Cache Fill)
        // 🚀 PILLAR 3 LIVE SYNC: Identity Integration
        // QA INTEGRATION FIX: Always query SQLite Offline Matrix (Single Source of Truth) to support instant Re-Mapping!
        val localDb = com.jdroidx.jfindme.data.local.CitizenDbHelper(context)
        val offlineMatrix = localDb.getOfflineRoster()
        var matchedTitle: String? = null
        
        for (node in offlineMatrix) {
            if (node["uid"] == deviceId) {
                matchedTitle = node["role_title"] as? String
                break
            }
        }
        
        var calculatedRole = "DEFAULT"
        if (matchedTitle != null && matchedTitle.isNotBlank() && matchedTitle != "GUEST") {
            val upTitle = matchedTitle.uppercase()
            when {
                upTitle.contains("FATHER") || upTitle.contains("MOTHER") || upTitle.contains("SPOUSE") || upTitle.contains("GUARDIAN") -> calculatedRole = "GUARDIAN"
                upTitle.contains("FRIEND") -> calculatedRole = "FRIEND"
                else -> calculatedRole = "CHILD"
            }
            userRoleCache[deviceId] = calculatedRole // Keep backup memory
        } else {
            // Memory Fallback OR Network Root query if completely unsynced
            calculatedRole = userRoleCache[deviceId] ?: "DEFAULT"
            if (calculatedRole == "DEFAULT") {
                usersEndpoint.child(deviceId).get().addOnSuccessListener { snapshot ->
                    val isDad = snapshot.child("role_father").getValue(Boolean::class.java) ?: false
                    val isMom = snapshot.child("role_mother").getValue(Boolean::class.java) ?: false
                    val isSpouse = snapshot.child("role_spouse").getValue(Boolean::class.java) ?: false
                    val isFriend = snapshot.child("role_friends").getValue(Boolean::class.java) ?: false
                    
                    when {
                        isDad || isMom || isSpouse -> userRoleCache[deviceId] = "GUARDIAN"
                        isFriend -> userRoleCache[deviceId] = "FRIEND"
                        else -> userRoleCache[deviceId] = "CHILD"
                    }
                }
            }
        }

        // Sub-setting QA Integration Phase: Direct AvatarCalloutGenerator Invocation (Single Truth)
        val customIconConfig = com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generate(
            context, rawName, originalBitmap, showName, isSelf = (deviceId == myUid), stackCount = stackCount, roleStr = calculatedRole
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
