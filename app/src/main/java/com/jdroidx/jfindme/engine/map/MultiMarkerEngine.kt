package com.jdroidx.jfindme.engine.map

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.AvatarCalloutGenerator
import com.jdroidx.jfindme.utils.AvatarLoader
import com.jdroidx.jfindme.utils.SystemLogger
import kotlin.math.*

/**
 * 🗺️ ENTERPRISE ANTI-OVERLAP MULTI-MARKER ENGINE (50dp Center-to-Center Standard)
 * Implements Google Maps & Apple Find My benchmarks:
 * 1. 50dp Center-to-Center Strict Spacing: Exact 50dp separation across co-located avatars.
 * 2. Apple Find My Spiderweb Anchor Lines: Subtle connecting polylines from dispersed avatars to the true centroid.
 * 3. Directional Heading & Motion Badges: Heading chevrons, driving speed (km/h), walking & battery status.
 * 4. Zoom-Adaptive Dynamic Re-Projection: Continuous collision recalculation on camera movement.
 * 5. Ghost Mode Persistence: 50% opacity preservation for offline family members.
 */
object MultiMarkerEngine {

    private const val TAG = "MultiMarkerEngine"

    const val DEFAULT_SPACING_DP = 50.0
    const val MIN_SPACING_DP = 50.0
    const val MAX_SPACING_DP = 90.0

    var configuredSpacingDp: Double = DEFAULT_SPACING_DP

    @JvmStatic
    fun calculateDispersionRadius(targetDistanceDp: Double, clusterSize: Int): Double {
        val count = clusterSize.coerceAtLeast(2)
        val sinVal = kotlin.math.sin(Math.PI / count)
        val halfD = targetDistanceDp / 2.0
        return halfD / (if (sinVal > 0.01) sinVal else 1.0)
    }

    @JvmStatic
    fun calculateAuraBoundingRadius(targetDistanceDp: Double, clusterSize: Int, avatarIconRadiusDp: Double = 24.0): Double {
        return calculateDispersionRadius(targetDistanceDp, clusterSize) + avatarIconRadiusDp
    }

    @JvmStatic
    fun calculateCircumference(targetDistanceDp: Double, clusterSize: Int): Double {
        return 2.0 * Math.PI * calculateDispersionRadius(targetDistanceDp, clusterSize)
    }

    private val activeDeviceMarkerMap = mutableMapOf<String, Marker>()
    private val activeSpiderLines = mutableListOf<Polyline>()
    private val activeCentroidMarkers = mutableListOf<Marker>()
    
    private val userRoleCache = mutableMapOf<String, String>()
    private val iconCache = mutableMapOf<String, BitmapDescriptor>()
    private val lastRenderedMeta = mutableMapOf<String, String>()
    private var activeGridMap: GoogleMap? = null

    private data class SpatialCandidate(
        val deviceId: String,
        val rawLatLng: LatLng,
        val name: String,
        val avatarId: String?,
        val speedMps: Float = 0f,
        val bearing: Float = 0f,
        val batteryLevel: Int = -1,
        val isSos: Boolean = false,
        val timestamp: Long = 0L,
        var screenPoint: Point = Point(0, 0),
        var dispersedLatLng: LatLng = rawLatLng,
        var centroidLatLng: LatLng = rawLatLng,
        var stackIndex: Int = 0,
        var stackTotal: Int = 1
    )

    /**
     * 🚀 MVVM PRIME DIRECTIVE: Strict Unidirectional Waterfall execution.
     */
    fun processViewModelLocations(context: Context, mapCore: GoogleMap, locations: List<UserLocation>) {
        if (activeGridMap != mapCore) {
            resetEngineState()
            activeGridMap = mapCore
        }

        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val selfHwUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(context)
        val selfRawAndroidId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getRawAndroidId(context)
        val canonicalSelfId = if (myUid.isNotEmpty()) myUid else (if (selfAgentId.isNotEmpty()) selfAgentId else "SELF_USER")

        val candidates = mutableListOf<SpatialCandidate>()
        val allowedUUIDs = mutableListOf<String>()
        val seenDeviceIds = mutableSetOf<String>()
        var selfIncluded = false

        // 🚀 WATERFALL PRIORITY SORTING: SOS emergency first, then Newest Timestamp to Oldest
        val db = DatabaseHelper.getInstance(context)
        val cachedOfflineRoster = db.getOfflineRoster()
        val sortedLocations = locations.filter { loc ->
            val dId = loc.getResolvedUserId()
            !db.isDeviceDeleted(dId)
        }.sortedWith(
            compareByDescending<UserLocation> { it.isSosActive }
                .thenByDescending { it.getLongTimestamp() }
        )

        // 1. Gather all active candidate targets (Strictly deduplicated by canonical identity)
        for (ul in sortedLocations) {
            val rawDeviceId = ul.getResolvedUserId()
            if (rawDeviceId.isEmpty()) continue
            if (ul.latitude == 0.0 && ul.longitude == 0.0) continue

            val isMe = (rawDeviceId == selfAgentId || 
                        rawDeviceId == selfHwUid || 
                        rawDeviceId == selfRawAndroidId || 
                        rawDeviceId == myUid || 
                        rawDeviceId == "SELF_USER" || 
                        rawDeviceId.equals("You", ignoreCase = true) || 
                        (myUid.isNotEmpty() && rawDeviceId.equals(myUid, ignoreCase = true)) ||
                        (selfAgentId.isNotEmpty() && rawDeviceId.equals(selfAgentId, ignoreCase = true)))
            val deviceId = if (isMe) canonicalSelfId else rawDeviceId

            if (isMe) {
                if (selfIncluded) continue // Reject duplicate Self node!
                selfIncluded = true
            } else {
                if (seenDeviceIds.contains(deviceId)) continue // Reject duplicate Peer node!
                seenDeviceIds.add(deviceId)
            }

            // 🚀 Spatial Viewport Culling: Exclude distant off-screen nodes (>16km buffer) when managing dense networks
            val visibleBounds = try { mapCore.projection?.visibleRegion?.latLngBounds } catch (e: Exception) { null }
            val isFarOffScreen = if (visibleBounds != null && sortedLocations.size > 20 && !isMe && !ul.isSosActive) {
                val paddedSouth = visibleBounds.southwest.latitude - 0.15
                val paddedNorth = visibleBounds.northeast.latitude + 0.15
                val paddedWest = visibleBounds.southwest.longitude - 0.15
                val paddedEast = visibleBounds.northeast.longitude + 0.15
                ul.latitude < paddedSouth || ul.latitude > paddedNorth || ul.longitude < paddedWest || ul.longitude > paddedEast
            } else {
                false
            }
            if (isFarOffScreen) continue

            allowedUUIDs.add(deviceId)
            if (candidates.size >= 100) break // Stress limit: 100 concurrent nodes

            // 🛣️ Filter GPS coordinates through 2D Kalman Filter & calculate road-aligned heading azimuth
            val smoothed = RoadSmoothingEngine.processLocation(
                deviceId = deviceId,
                rawLat = ul.latitude,
                rawLng = ul.longitude,
                accuracyMeters = 10f,
                rawSpeedMps = ul.speed,
                rawBearing = ul.bearing,
                timestamp = ul.getLongTimestamp(),
                context = context
            )

            val rawLatLng = smoothed.smoothedLatLng

            // 🚀 Human-Readable Name Resolution: Never show raw ID numbers!
            val isGenericCloudName = ul.userName.isBlank() || 
                                     ul.userName.equals("null", ignoreCase = true) || 
                                     ul.userName.startsWith("Agent_", ignoreCase = true) ||
                                     ul.userName.equals("Active Tracker", ignoreCase = true) ||
                                     ul.userName.equals("Target Node", ignoreCase = true) ||
                                     ul.userName.equals("Target", ignoreCase = true) ||
                                     ul.userName.equals("User Identity", ignoreCase = true)

            val ssotName = db.getDeviceCentricName(deviceId)
            val hasValidSsotName = ssotName.isNotBlank() && 
                                   !ssotName.startsWith("Agent_", true) && 
                                   !ssotName.equals("Target Node", true) &&
                                   !ssotName.equals("Active Tracker", true)

            // [Variables] | [Constants] | [Dependencies] | [Integration Points]
            // Roster Match Resolution: Scoped at device-level for name resolution and ACL permission evaluation
            val rosterMatch = cachedOfflineRoster.find { 
                (it["uid_1"]?.toString() == deviceId) || (it["uid"]?.toString() == deviceId) || 
                (it["uid_2"]?.toString() == deviceId) || (it["connected_id"]?.toString() == deviceId)
            }

            val cleanName = if (isMe) {
                "You"
            } else if (hasValidSsotName) {
                ssotName
            } else if (!isGenericCloudName) {
                ul.userName.trim()
            } else {
                val dossier = db.getLocalDossier(deviceId)
                val dossierName = dossier["fullName"] ?: dossier["name"] ?: dossier["id_full_name"] ?: dossier["full_name"] ?: dossier["owner_name"]
                if (!dossierName.isNullOrBlank() && !dossierName.startsWith("Agent_", ignoreCase = true) && !dossierName.equals("Target Node", true)) {
                    dossierName.trim()
                } else {
                    val rName = rosterMatch?.get("fullName")?.toString() ?: rosterMatch?.get("display_name")?.toString()
                    if (!rName.isNullOrBlank() && !rName.startsWith("Agent_", true) && !rName.equals("Target Node", true)) {
                        rName.trim()
                    } else {
                        val role = rosterMatch?.get("role_type")?.toString()
                        if (!role.isNullOrBlank() && !role.equals("None", ignoreCase = true) && !role.equals("Member", ignoreCase = true)) {
                            role.trim()
                        } else {
                            "Target Node"
                        }
                    }
                }
            }

            // [Variables] | [Constants] | [Dependencies] | [Integration Points]
            // Enterprise ACL Gate: Verify Live Tracking permission (0x01). SOS emergencies override privacy locks.
            val permMaskStr = rosterMatch?.get("permissions_mask")?.toString() ?: "0xFF"
            val permMask = try {
                if (permMaskStr.startsWith("0x", ignoreCase = true)) {
                    permMaskStr.substring(2).toInt(16)
                } else {
                    permMaskStr.toInt()
                }
            } catch (e: Exception) { 0xFF }

            val isLiveTrackingAllowed = (permMask and 0x01) != 0
            if (!isMe && !ul.isSosActive && !isLiveTrackingAllowed) {
                // Tracking is explicitly paused via Person Details ACL for this node
                continue
            }

            val modelIcon = if (!ul.avatar_id.isNullOrBlank() && ul.avatar_id != "butterfly" && ul.avatar_id != "1") {
                ul.avatar_id
            } else {
                val rAv = rosterMatch?.get("avatar_id")?.toString()?.takeIf { it.isNotBlank() && it != "butterfly" && it != "1" }
                rAv ?: db.getDeviceCentricAvatar(deviceId)
            }

            candidates.add(
                SpatialCandidate(
                    deviceId = deviceId,
                    rawLatLng = rawLatLng,
                    name = cleanName,
                    avatarId = modelIcon,
                    speedMps = smoothed.smoothedSpeedMps,
                    bearing = smoothed.filteredHeading,
                    batteryLevel = ul.batteryLevel,
                    isSos = ul.isSosActive,
                    timestamp = ul.getLongTimestamp()
                )
            )
        }

        // 2. Perform Screen-Space Collision Detection & 50dp Spiderfication Dispersion
        resolveCollisionsAndDisperse(context, mapCore, candidates)

        // 3. Render or smoothly animate markers to their dispersed coordinates
        for (candidate in candidates) {
            updateOrRenderMarker(
                context = context,
                candidate = candidate
            )
        }

        // 4. Render Apple Find My Style Spiderweb Anchor Lines (Only when explicitly toggled ON)
        renderSpiderwebAnchors(context, mapCore, candidates)

        // 5. Ghost Mode / Clean up removed devices & purge duplicate legacy self markers
        cleanupDisconnectedDevices(context, allowedUUIDs)
    }

    /**
     * 🛡️ Anti-Collision Screen Projection Algorithm (50dp Center-to-Center Spacing)
     */
    private fun resolveCollisionsAndDisperse(context: Context, mapCore: GoogleMap, candidates: MutableList<SpatialCandidate>) {
        if (candidates.isEmpty()) return

        val projection = try {
            mapCore.projection
        } catch (e: Exception) {
            null
        }

        val density = context.resources.displayMetrics.density
        // 🚀 Target: Configurable Center-to-Center Collision Trigger (50dp - 90dp Standard with 30% Proximity Buffer)
        val targetSpacingDp = configuredSpacingDp.coerceIn(MIN_SPACING_DP, MAX_SPACING_DP)
        val collisionThresholdPx = targetSpacingDp.toFloat() * density * 1.30f

        for (c in candidates) {
            c.screenPoint = if (projection != null) {
                try {
                    val pt = projection.toScreenLocation(c.rawLatLng)
                    if (pt.x == 0 && pt.y == 0) {
                        Point((c.rawLatLng.longitude * 10000).toInt(), (c.rawLatLng.latitude * 10000).toInt())
                    } else {
                        pt
                    }
                } catch (e: Exception) {
                    Point((c.rawLatLng.longitude * 10000).toInt(), (c.rawLatLng.latitude * 10000).toInt())
                }
            } else {
                Point((c.rawLatLng.longitude * 10000).toInt(), (c.rawLatLng.latitude * 10000).toInt())
            }
        }

        // 🛡️ Transitive Connected Component Clustering (BFS - guarantees 3-node Triangle and 4-node Square)
        val clusters = mutableListOf<MutableList<SpatialCandidate>>()
        val visited = mutableSetOf<String>()

        for (i in candidates.indices) {
            val root = candidates[i]
            if (visited.contains(root.deviceId)) continue

            val currentCluster = mutableListOf<SpatialCandidate>()
            val queue = java.util.ArrayDeque<SpatialCandidate>()
            queue.add(root)
            visited.add(root.deviceId)

            while (!queue.isEmpty()) {
                val curr = queue.poll() ?: break
                currentCluster.add(curr)

                for (neighbor in candidates) {
                    if (visited.contains(neighbor.deviceId)) continue

                    val isColliding = if (projection != null && curr.screenPoint.x != 0 && neighbor.screenPoint.x != 0) {
                        val dx = (curr.screenPoint.x - neighbor.screenPoint.x).toDouble()
                        val dy = (curr.screenPoint.y - neighbor.screenPoint.y).toDouble()
                        val distPx = sqrt(dx * dx + dy * dy)
                        distPx < collisionThresholdPx
                    } else {
                        val geoDeltaLat = abs(curr.rawLatLng.latitude - neighbor.rawLatLng.latitude)
                        val geoDeltaLng = abs(curr.rawLatLng.longitude - neighbor.rawLatLng.longitude)
                        geoDeltaLat < 0.00080 && geoDeltaLng < 0.00080
                    }

                    if (isColliding) {
                        visited.add(neighbor.deviceId)
                        queue.add(neighbor)
                    }
                }
            }
            clusters.add(currentCluster)
        }

        // Apply radial equidistant dispersion: Center-to-Center distance = exactly configured targetSpacingDp
        for (cluster in clusters) {
            val count = cluster.size
            if (count == 1) {
                cluster[0].dispersedLatLng = cluster[0].rawLatLng
                cluster[0].centroidLatLng = cluster[0].rawLatLng
                cluster[0].stackIndex = 0
                cluster[0].stackTotal = 1
                continue
            }

            val centroidX = cluster.map { it.screenPoint.x }.average()
            val centroidY = cluster.map { it.screenPoint.y }.average()
            val avgLat = cluster.map { it.rawLatLng.latitude }.average()
            val avgLng = cluster.map { it.rawLatLng.longitude }.average()
            val trueCentroid = LatLng(avgLat, avgLng)

            // 🎯 Precise Geometric Progression:
            // 2 Devices -> Symmetrical Horizontal Line
            // 3 Devices -> Equilateral Triangle with top apex (Δ)
            // 4 Devices -> Square with horizontal/vertical bounds (□)
            // 5 Devices -> Regular Pentagon (⬟)
            // 6 Devices -> Regular Hexagon (⬡)
            // 7+ Devices -> Regular N-gon smoothly converging into a continuous Circle (◯)
            val dispersionRadiusPx = (calculateDispersionRadius(targetSpacingDp, count) * density).toFloat()

            val baseAngle = when (count) {
                2 -> 0.0 // Symmetrical Line along horizontal axis
                3 -> -PI / 2.0 // Equilateral Triangle with top apex (Δ)
                4 -> -PI / 2.0 // 💎 Diamond Formation (Rhombus: Top 12 o'clock, Right 3 o'clock, Bottom 6 o'clock, Left 9 o'clock) (◇)
                else -> -PI / 2.0 // Regular N-gon starting at 12 o'clock converging into Circle
            }

            for (idx in cluster.indices) {
                val node = cluster[idx]
                node.stackIndex = idx
                node.stackTotal = count
                node.centroidLatLng = trueCentroid

                val angle = baseAngle + (2.0 * PI * idx / count)
                val rawOffsetX = (centroidX + dispersionRadiusPx * cos(angle)).toInt()
                val rawOffsetY = (centroidY + dispersionRadiusPx * sin(angle)).toInt()

                // 🛡️ Edge Bezel Clamping: Prevent dispersion off-screen when centroid is near bezel
                val displayMetrics = context.resources.displayMetrics
                val marginInsetPx = (28f * density).toInt()
                val clampedX = rawOffsetX.coerceIn(marginInsetPx, displayMetrics.widthPixels - marginInsetPx)
                val clampedY = rawOffsetY.coerceIn(marginInsetPx, displayMetrics.heightPixels - marginInsetPx)

                if (projection != null) {
                    try {
                        val dispersedLatLng = projection.fromScreenLocation(Point(clampedX, clampedY))
                        node.dispersedLatLng = dispersedLatLng ?: node.rawLatLng
                    } catch (e: Exception) {
                        node.dispersedLatLng = applyGeoOffsetFallback(trueCentroid, angle, dispersionRadiusPx, density)
                    }
                } else {
                    node.dispersedLatLng = applyGeoOffsetFallback(trueCentroid, angle, dispersionRadiusPx, density)
                }
            }
        }
    }

    private fun applyGeoOffsetFallback(center: LatLng, angle: Double, radiusPx: Float, density: Float): LatLng {
        val meters = radiusPx / density * 0.7
        val deltaLat = (meters / 111111.0) * sin(angle)
        val deltaLng = (meters / (111111.0 * cos(Math.toRadians(center.latitude)))) * cos(angle)
        return LatLng(center.latitude + deltaLat, center.longitude + deltaLng)
    }

    /**
     * 🕸️ Geometric Polygon & Spiderweb Anchor Lines:
     * - 2 Devices: Straight Line
     * - 3 Devices: Equilateral Triangle
     * - 4 Devices: Square
     * - 5 Devices: Pentagon
     * - 6 Devices: Hexagon
     * - 8+ Devices: Regular Octagon -> Circle
     */
    private fun renderSpiderwebAnchors(context: Context, map: GoogleMap, candidates: List<SpatialCandidate>) {
        // Clear previous spider lines and centroids
        for (line in activeSpiderLines) {
            try { line.remove() } catch (ignored: Exception) {}
        }
        activeSpiderLines.clear()

        for (marker in activeCentroidMarkers) {
            try { marker.remove() } catch (ignored: Exception) {}
        }
        activeCentroidMarkers.clear()

        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val showSpiderLines = prefs.getBoolean("show_cluster_spiderweb", false) // 🚀 Clean & Minimal default: OFF (Uncluttered)
        if (!showSpiderLines) return

        val clustersByCentroid = mutableMapOf<String, MutableList<SpatialCandidate>>()

        for (c in candidates) {
            if (c.stackTotal > 1) {
                val centerKey = "${c.centroidLatLng.latitude}_${c.centroidLatLng.longitude}"
                clustersByCentroid.getOrPut(centerKey) { mutableListOf() }.add(c)
            }
        }

        for ((_, cluster) in clustersByCentroid) {
            if (cluster.isEmpty()) continue
            val trueCentroid = cluster[0].centroidLatLng

            // 1. Draw glowing anchor dot at true centroid
            val centerMarker = map.addMarker(
                MarkerOptions()
                    .position(trueCentroid)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .alpha(0.6f)
            )
            if (centerMarker != null) activeCentroidMarkers.add(centerMarker)

            // 2. Draw radial dashed anchor lines to centroid
            for (node in cluster) {
                val spiderLine = map.addPolyline(
                    PolylineOptions()
                        .add(node.dispersedLatLng, trueCentroid)
                        .color(Color.argb(140, 0, 229, 255))
                        .width(2.5f)
                        .geodesic(true)
                        .pattern(listOf(Dash(10f), Gap(6f)))
                )
                activeSpiderLines.add(spiderLine)
            }

            // 3. Draw Geometric Perimeter Outline (Line for 2, Triangle for 3, Square for 4, Polygon -> Circle)
            val sortedNodes = cluster.sortedBy { it.stackIndex }
            if (sortedNodes.size == 2) {
                val linePoly = map.addPolyline(
                    PolylineOptions()
                        .add(sortedNodes[0].dispersedLatLng, sortedNodes[1].dispersedLatLng)
                        .color(Color.argb(200, 0, 230, 118))
                        .width(3f)
                        .geodesic(true)
                        .pattern(listOf(Dash(14f), Gap(8f)))
                )
                activeSpiderLines.add(linePoly)
            } else if (sortedNodes.size >= 3) {
                val polyPoints = mutableListOf<LatLng>()
                for (n in sortedNodes) polyPoints.add(n.dispersedLatLng)
                polyPoints.add(sortedNodes[0].dispersedLatLng) // Close loop

                val geomPoly = map.addPolyline(
                    PolylineOptions()
                        .addAll(polyPoints)
                        .color(Color.argb(200, 0, 230, 118))
                        .width(3f)
                        .geodesic(true)
                        .pattern(listOf(Dash(14f), Gap(8f)))
                )
                activeSpiderLines.add(geomPoly)
            }
        }
    }

    private fun updateOrRenderMarker(
        context: Context,
        candidate: SpatialCandidate
    ) {
        val map = activeGridMap ?: return
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val selfAgentId = prefs.getString("agent_id", "") ?: ""
        val deviceId = candidate.deviceId
        val isSelf = (deviceId == myUid || deviceId == selfAgentId || deviceId == "SELF_USER")
        val showName = true // Always show user names prominently on the map

        val targetSize = 256
        val originalBitmap = if (isSelf) {
            AvatarLoader.getCurrentUserAvatarBitmap(context, targetSize)
        } else {
            AvatarLoader.getRemoteAvatarBitmap(context, deviceId, candidate.avatarId, targetSize)
        }

        var calculatedRole = userRoleCache[deviceId]
        if (calculatedRole == null) {
            val localDb = DatabaseHelper.getInstance(context)
            var matchedTitle: String? = null

            // 1. Direct role query from SSOT helper
            val directRole = localDb.getRoleForDevice(deviceId)
            if (!directRole.isNullOrBlank() && !directRole.equals("None", true) && !directRole.equals("Member", true)) {
                matchedTitle = directRole
            }

            // 2. Offline roster check across all UID permutations
            if (matchedTitle == null) {
                val offlineMatrix = localDb.getOfflineRoster()
                for (node in offlineMatrix) {
                    if (node["uid_1"]?.toString() == deviceId || 
                        node["uid"]?.toString() == deviceId || 
                        node["uid_2"]?.toString() == deviceId || 
                        node["connected_id"]?.toString() == deviceId) {
                        val rType = node["role_type"]?.toString()
                        if (!rType.isNullOrBlank() && !rType.equals("None", true)) {
                            matchedTitle = rType
                            break
                        }
                    }
                }
            }

            // 3. Check local dossier if still unmatched
            if (matchedTitle == null) {
                val dossier = localDb.getLocalDossier(deviceId)
                val dosRole = dossier["role_type"] ?: dossier["designation"]
                if (!dosRole.isNullOrBlank() && !dosRole.equals("None", true) && !dosRole.equals("Member", true)) {
                    matchedTitle = dosRole
                }
            }

            if (matchedTitle != null && matchedTitle.isNotBlank()) {
                val upTitle = matchedTitle.uppercase()
                calculatedRole = when {
                    upTitle.contains("SPOUSE") || upTitle.contains("PARTNER") || upTitle.contains("WIFE") || upTitle.contains("HUSBAND") -> "SPOUSE"
                    upTitle.contains("MOTHER") || upTitle.contains("FATHER") || upTitle.contains("GUARDIAN") || upTitle.contains("PARENT") || upTitle.contains("ADMIN") || upTitle.contains("COMMANDER") -> "GUARDIAN"
                    upTitle.contains("FRIEND") || upTitle.contains("PEER") || upTitle.contains("COLLEAGUE") -> "FRIEND"
                    upTitle.contains("CHILD") || upTitle.contains("SON") || upTitle.contains("DAUGHTER") || upTitle.contains("DEPENDENT") || upTitle.contains("KID") -> "CHILD"
                    upTitle.contains("FAMILY") || upTitle.contains("BROTHER") || upTitle.contains("SISTER") || upTitle.contains("RELATIVE") || upTitle.contains("SIBLING") -> "FAMILY"
                    else -> "MEMBER"
                }
                userRoleCache[deviceId] = calculatedRole
            } else {
                calculatedRole = "MEMBER"
                FirebaseComm.getProfileRef(deviceId).get().addOnSuccessListener { snapshot ->
                    val famRole = snapshot.child("role_family").value?.toString() ?: "None"
                    val friendRole = snapshot.child("role_friend").value?.toString() ?: "None"
                    val roleTitle = snapshot.child("role_title").value?.toString() ?: "None"
                    val combined = "$famRole $friendRole $roleTitle".uppercase()

                    val asyncRole = when {
                        combined.contains("SPOUSE") || combined.contains("PARTNER") || combined.contains("WIFE") || combined.contains("HUSBAND") -> "SPOUSE"
                        combined.contains("MOTHER") || combined.contains("FATHER") || combined.contains("GUARDIAN") || combined.contains("PARENT") || combined.contains("ADMIN") -> "GUARDIAN"
                        combined.contains("FRIEND") || combined.contains("PEER") || combined.contains("COLLEAGUE") -> "FRIEND"
                        combined.contains("CHILD") || combined.contains("SON") || combined.contains("DAUGHTER") || combined.contains("DEPENDENT") || combined.contains("KID") -> "CHILD"
                        combined.contains("FAMILY") || combined.contains("BROTHER") || combined.contains("SISTER") || combined.contains("RELATIVE") -> "FAMILY"
                        else -> "MEMBER"
                    }
                    userRoleCache[deviceId] = asyncRole
                    activeGridMap?.let { updateOrRenderMarker(context, candidate) }
                }
            }
        }

        val speedKmh = (candidate.speedMps * 3.6f).toInt()
        val batBucket = if (candidate.batteryLevel >= 0) (candidate.batteryLevel / 10) * 10 else -1
        val spdBucket = if (candidate.speedMps > 1.5f) (candidate.speedMps * 3.6f / 5).toInt() * 5 else 0
        val currentMeta = "${candidate.name}|${candidate.avatarId}|${candidate.stackTotal}|$calculatedRole|$showName|${candidate.isSos}|$batBucket|$spdBucket"
        var customIconConfig = iconCache[deviceId]
        var iconChanged = false

        if (customIconConfig == null || lastRenderedMeta[deviceId] != currentMeta) {
            customIconConfig = AvatarCalloutGenerator.generate(
                context = context,
                name = candidate.name,
                avatarBmp = originalBitmap,
                showName = showName,
                isSelf = isSelf,
                stackCount = if (candidate.stackTotal > 1) candidate.stackTotal else 0,
                roleStr = calculatedRole ?: "DEFAULT",
                speedMps = candidate.speedMps,
                bearing = candidate.bearing,
                batteryLevel = candidate.batteryLevel,
                isSos = candidate.isSos,
                timestamp = candidate.timestamp
            )
            iconCache[deviceId] = customIconConfig
            lastRenderedMeta[deviceId] = currentMeta
            iconChanged = true
        }

        if (activeDeviceMarkerMap.containsKey(deviceId)) {
            val targetMarker = activeDeviceMarkerMap[deviceId]!!
            targetMarker.tag = deviceId
            targetMarker.title = candidate.name
            if (iconChanged) {
                targetMarker.setIcon(customIconConfig)
            }
            animateMarkerToLocation(targetMarker, targetMarker.position, candidate.dispersedLatLng)
        } else {
            val newMarker = map.addMarker(
                MarkerOptions()
                    .position(candidate.dispersedLatLng)
                    .title(candidate.name)
                    .icon(customIconConfig)
                    .anchor(0.5f, 0.5f)
                    .flat(false)
            )

            if (newMarker != null) {
                newMarker.tag = deviceId
                activeDeviceMarkerMap[deviceId] = newMarker
            }
        }
    }

    private fun animateMarkerToLocation(marker: Marker, startPosition: LatLng, endPosition: LatLng) {
        val dx = abs(startPosition.latitude - endPosition.latitude)
        val dy = abs(startPosition.longitude - endPosition.longitude)
        if (dx < 0.000001 && dy < 0.000001) return

        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 600
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
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val ghostModePersistent = prefs.getBoolean("pref_ghost_mode_persistent", false)
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val selfAgentId = prefs.getString("agent_id", "") ?: ""

        val iterator = activeDeviceMarkerMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val deviceId = entry.key
            val marker = entry.value

            val isSelfMarker = (deviceId == myUid || deviceId == selfAgentId || deviceId == "SELF_USER")

            if (!allowedUUIDs.contains(deviceId)) {
                if (isSelfMarker || !ghostModePersistent) {
                    try { marker.remove() } catch (ignored: Exception) {}
                    iterator.remove()
                    iconCache.remove(deviceId)
                    lastRenderedMeta.remove(deviceId)
                    RoadSmoothingEngine.resetDevice(deviceId)
                } else {
                    marker.alpha = 0.5f
                    val currentTitle = marker.title ?: "Friend"
                    if (!currentTitle.contains(" [OFFLINE]")) {
                        marker.title = "$currentTitle [OFFLINE]"
                        marker.showInfoWindow()
                    }
                }
            } else {
                marker.alpha = 1.0f
                val currentTitle = marker.title ?: "Friend"
                if (currentTitle.contains(" [OFFLINE]")) {
                    marker.title = currentTitle.replace(" [OFFLINE]", "")
                }
            }
        }
    }

    fun resetEngineState() {
        SystemLogger.w(TAG, "Map Instance Shift Detected. Liquidating Stale Marker Collections.")
        activeDeviceMarkerMap.clear()
        for (line in activeSpiderLines) line.remove()
        activeSpiderLines.clear()
        for (m in activeCentroidMarkers) m.remove()
        activeCentroidMarkers.clear()
        iconCache.clear()
        lastRenderedMeta.clear()
        userRoleCache.clear()
        RoadSmoothingEngine.clearAll()
        AvatarLoader.clearCache()
    }

    fun invalidateRoleAndNameCaches() {
        userRoleCache.clear()
        iconCache.clear()
        lastRenderedMeta.clear()
    }
}
