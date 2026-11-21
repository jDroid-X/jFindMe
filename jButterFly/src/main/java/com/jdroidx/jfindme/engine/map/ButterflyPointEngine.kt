package com.jdroidx.jfindme.engine.map

import android.content.Context
import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable
import com.jdroidx.jfindme.data.local.tables.ButterflyPointTable
import com.jdroidx.jfindme.data.model.ButterflyDeviceEntry
import com.jdroidx.jfindme.data.model.ButterflyPoint
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger
import kotlin.math.*

/**
 * [Variables] butterflyMarker, arrivalPerimeterCircle, rendezvousVectorLines, activePointId, arrivalHysteresisMap
 * [Constants] TAG = "ButterflyPointEngine", DEFAULT_ARRIVAL_RADIUS_METERS = 50.0, HYSTERESIS_DEADBAND_METERS = 15.0, COLOR_PERIMETER_FILL, COLOR_PERIMETER_STROKE, COLOR_VECTOR_LINE
 * [Dependencies] com.google.android.gms.maps.GoogleMap, com.jdroidx.jfindme.data.local.DatabaseHelper, com.jdroidx.jfindme.data.model.ButterflyPoint, com.google.firebase.database.FirebaseDatabase
 * [Integration Points] MapActivity.java, MapViewModel.kt, TrackingService.kt, MultiMarkerEngine.kt, Firebase RTDB /butterfly_points/{pointId}
 *
 * Enterprise Origin-Shifted Rendezvous Tracking & Telemetry Engine (OOPS MVC Architecture)
 */
object ButterflyPointEngine {

    private const val TAG = "ButterflyPointEngine"

    const val DEFAULT_ARRIVAL_RADIUS_METERS = 50.0
    const val HYSTERESIS_DEADBAND_METERS = 15.0

    private val COLOR_PERIMETER_FILL = Color.argb(40, 0, 230, 118)   // Emerald translucent
    private val COLOR_PERIMETER_STROKE = Color.argb(220, 0, 230, 118) // Emerald solid
    private val COLOR_VECTOR_LINE = Color.argb(160, 0, 229, 255)      // Cyan tactical dashed

    private var currentButterflyMarker: Marker? = null
    private var currentPerimeterCircle: Circle? = null
    private val currentVectorLines = mutableListOf<Polyline>()
    private var activeRendezvousPointId: String? = null

    // Tracks devices that have already latched arrival state to apply hysteresis
    private val arrivalLatchMap = mutableMapOf<String, Boolean>()

    // Currently focused Butterfly Point in memory
    @get:JvmStatic
    var activeButterflyPoint: ButterflyPoint? = null
        private set

    /**
     * Mathematical Haversine Distance Calculation (Great Circle Distance in meters)
     * Delegated to SpatialMathUtil for enterprise OOPS deduplication.
     */
    @JvmStatic
    fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return com.jdroidx.jfindme.utils.SpatialMathUtil.computeDistanceBetween(lat1, lon1, lat2, lon2)
    }

    /**
     * Alias for calculateHaversineDistance providing clean call syntax
     */
    @JvmStatic
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
        calculateHaversineDistance(lat1, lon1, lat2, lon2)

    /**
     * Formats distance into human-readable metric string.
     */
    @JvmStatic
    fun formatDistance(meters: Double): String {
        return if (meters < 1000.0) {
            "${meters.roundToInt()} m"
        } else {
            String.format(java.util.Locale.US, "%.1f km", meters / 1000.0)
        }
    }

    /**
     * Calculates estimated travel duration in minutes based on real-time speed.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateEtaMinutes(distanceMeters: Double, speedKmh: Float, context: Context? = null): Int {
        if (distanceMeters <= 0.0) return 0
        val minSpeed = if (context != null) com.jdroidx.jfindme.utils.RegistryManager.getButterflyMinSpeedKmh(context) else 3.0f
        val fallbackNominal = if (context != null) com.jdroidx.jfindme.utils.RegistryManager.getButterflyNominalSpeedKmh(context).toDouble() else 25.0
        val safeSpeed = if (speedKmh > minSpeed) speedKmh.toDouble() else fallbackNominal
        val hours = (distanceMeters / 1000.0) / safeSpeed
        return (hours * 60.0).roundToInt().coerceAtLeast(1)
    }

    /**
     * Computes the rendezvous status with dynamic arrival perimeter and deadband hysteresis from RegistryManager.
     */
    @JvmStatic
    fun evaluateDeviceStatus(
        context: Context?,
        deviceId: String,
        currentStatus: String,
        distanceMeters: Double
    ): String {
        val radius = if (context != null) com.jdroidx.jfindme.utils.RegistryManager.getButterflyArrivalRadius(context).toDouble() else DEFAULT_ARRIVAL_RADIUS_METERS
        val hysteresis = if (context != null) com.jdroidx.jfindme.utils.RegistryManager.getButterflyHysteresisMeters(context).toDouble() else HYSTERESIS_DEADBAND_METERS
        return evaluateDeviceStatus(deviceId, currentStatus, distanceMeters, radius, hysteresis)
    }

    /**
     * Computes the rendezvous status with 15-meter deadband hysteresis.
     */
    @JvmStatic
    @JvmOverloads
    fun evaluateDeviceStatus(
        deviceId: String,
        currentStatus: String,
        distanceMeters: Double,
        arrivalRadiusMeters: Double = DEFAULT_ARRIVAL_RADIUS_METERS,
        hysteresisDeadbandMeters: Double = HYSTERESIS_DEADBAND_METERS
    ): String {
        if (currentStatus.equals(ButterflyPointDeviceMapTable.STATUS_REJECTED, ignoreCase = true)) {
            return ButterflyPointDeviceMapTable.STATUS_REJECTED
        }

        val wasArrived = (arrivalLatchMap[deviceId] == true) || currentStatus.equals(ButterflyPointDeviceMapTable.STATUS_REACHED, ignoreCase = true)
        return if (wasArrived) {
            // Hysteresis release: Only revert to ON_MOVE if moved beyond arrival radius + deadband
            if (distanceMeters > (arrivalRadiusMeters + hysteresisDeadbandMeters)) {
                arrivalLatchMap[deviceId] = false
                ButterflyPointDeviceMapTable.STATUS_ON_MOVE
            } else {
                arrivalLatchMap[deviceId] = true
                ButterflyPointDeviceMapTable.STATUS_REACHED
            }
        } else {
            if (distanceMeters <= arrivalRadiusMeters) {
                arrivalLatchMap[deviceId] = true
                ButterflyPointDeviceMapTable.STATUS_REACHED
            } else if (currentStatus.equals(ButterflyPointDeviceMapTable.STATUS_ACCEPTED, ignoreCase = true) ||
                currentStatus.equals(ButterflyPointDeviceMapTable.STATUS_ON_MOVE, ignoreCase = true)
            ) {
                ButterflyPointDeviceMapTable.STATUS_ON_MOVE
            } else {
                ButterflyPointDeviceMapTable.STATUS_PENDING
            }
        }
    }

    /**
     * Builds LatLngBounds dynamically encompassing the Butterfly Point + all participating devices
     */
    @JvmStatic
    fun calculateMaxRadiusBounds(
        butterflyLat: Double,
        butterflyLng: Double,
        deviceCoordinates: Collection<LatLng>
    ): LatLngBounds {
        val builder = LatLngBounds.Builder()
        builder.include(LatLng(butterflyLat, butterflyLng))

        var validCount = 0
        for (coord in deviceCoordinates) {
            if (coord.latitude != 0.0 && coord.longitude != 0.0 &&
                (Math.abs(coord.latitude - butterflyLat) > 0.00001 || Math.abs(coord.longitude - butterflyLng) > 0.00001)) {
                builder.include(coord)
                validCount++
            }
        }

        if (validCount == 0) {
            // Expand by safe delta if only 1 point exists to avoid bounds collapse
            val delta = 0.005
            builder.include(LatLng(butterflyLat + delta, butterflyLng + delta))
            builder.include(LatLng(butterflyLat - delta, butterflyLng - delta))
        }

        return builder.build()
    }

    /**
     * Renders Butterfly Point marker, arrival perimeter circle, and tactical vector lines on map
     */
    @JvmStatic
    fun renderButterflyOverlay(
        map: GoogleMap,
        point: ButterflyPoint,
        devicePositions: Map<String, LatLng>
    ) {
        clearButterflyOverlay()
        activeButterflyPoint = point
        activeRendezvousPointId = point.pointId

        val center = LatLng(point.lat, point.lng)

        // 1. Butterfly Center Marker
        val markerOptions = MarkerOptions()
            .position(center)
            .title("🦋 ${point.pointName}")
            .snippet("${point.address} • Arrival Radius: ${point.arrivalRadiusMeters.toInt()}m")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .anchor(0.5f, 0.5f)
        currentButterflyMarker = map.addMarker(markerOptions)
        currentButterflyMarker?.tag = "BUTTERFLY_POINT"

        // 2. Arrival Perimeter Circle
        val effectiveRadius = if (point.arrivalRadiusMeters > 0) point.arrivalRadiusMeters else DEFAULT_ARRIVAL_RADIUS_METERS
        val circleOptions = CircleOptions()
            .center(center)
            .radius(effectiveRadius)
            .fillColor(COLOR_PERIMETER_FILL)
            .strokeColor(COLOR_PERIMETER_STROKE)
            .strokeWidth(3.0f)
        currentPerimeterCircle = map.addCircle(circleOptions)

        // 3. Tactical Vector Lines (Spiderweb anchor lines from devices to Butterfly Point with RAG dynamic coloring)
        for ((devId, pos) in devicePositions) {
            if (pos.latitude != 0.0 && pos.longitude != 0.0) {
                val devEntry = point.devices[devId]
                // 🛡️ Filter out inactive, declined, or revoked nodes from the active vector mesh
                if (devEntry != null && (
                    devEntry.status.equals(ButterflyPointDeviceMapTable.STATUS_REJECTED, ignoreCase = true) ||
                    devEntry.status.equals(ButterflyPointDeviceMapTable.STATUS_REVOKED, ignoreCase = true) ||
                    devEntry.status.equals(ButterflyPointDeviceMapTable.STATUS_TIMED_OUT, ignoreCase = true)
                )) {
                    continue
                }
                val lineColor = when (devEntry?.ragStatus) {
                    "RED" -> Color.argb(220, 239, 68, 68)     // Solid Tactical Red (Distress / Overdue)
                    "AMBER" -> Color.argb(220, 245, 158, 11)  // Solid Tactical Amber (Delayed / Low Battery / Stale)
                    else -> COLOR_VECTOR_LINE                 // Tactical Cyan (On-Track / Nominal)
                }
                val polyline = map.addPolyline(
                    PolylineOptions()
                        .add(pos, center)
                        .color(lineColor)
                        .width(3.5f)
                        .geodesic(true)
                )
                currentVectorLines.add(polyline)
            }
        }
    }

    /**
     * Removes active Butterfly Point map markers, circles, and polylines cleanly.
     */
    @JvmStatic
    fun clearButterflyOverlay() {
        try {
            currentButterflyMarker?.remove()
        } catch (_: Exception) {}
        currentButterflyMarker = null

        try {
            currentPerimeterCircle?.remove()
        } catch (_: Exception) {}
        currentPerimeterCircle = null

        for (line in currentVectorLines) {
            try {
                line.remove()
            } catch (_: Exception) {}
        }
        currentVectorLines.clear()
        activeRendezvousPointId?.let { stopActivePointPeerSync(it) }
        activeButterflyPoint = null
        activeRendezvousPointId = null
        quorumAlertedPoints.clear()
    }

    /**
     * Dual-Engine Dispatch: Writes point and device invitations to SQLite & Firebase RTDB.
     */
    @JvmStatic
    fun dispatchButterflyPoint(
        context: Context,
        point: ButterflyPoint,
        targetDeviceIds: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val dbHelper = DatabaseHelper.getInstance(context)
                val db = dbHelper.writableDatabase

                // 1. Master DB [Offline: SQLite butterfly_point]
                ButterflyPointTable.save(
                    db = db,
                    pointId = point.pointId,
                    creatorDeviceId = point.creatorDeviceId,
                    creatorIdentityId = null,
                    pointName = point.pointName,
                    address = point.address,
                    lat = point.lat,
                    lng = point.lng,
                    geohash = point.geohash,
                    rendezvousTimestamp = point.rendezvousTimestamp,
                    expiryTimestamp = point.expiryTimestamp,
                    arrivalRadiusMeters = point.arrivalRadiusMeters,
                    notes = point.notes,
                    iconType = point.iconType,
                    isActive = true,
                    isArchived = false
                )

                // 2. Sub Master / Fact DB: Connected Devices (butterfly_point_device_map)
                // 2a. Assign Creator (Device A) as confirmed participant (STATUS_ACCEPTED)
                val creatorDevId = point.creatorDeviceId
                if (creatorDevId.isNotBlank()) {
                    ButterflyPointDeviceMapTable.assignDevice(
                        db = db,
                        pointId = point.pointId,
                        deviceId = creatorDevId,
                        initialStatus = ButterflyPointDeviceMapTable.STATUS_ACCEPTED
                    )
                    point.devices[creatorDevId] = ButterflyDeviceEntry(
                        deviceId = creatorDevId,
                        deviceName = point.creatorName.ifBlank { "Creator" },
                        status = ButterflyPointDeviceMapTable.STATUS_ACCEPTED,
                        lastLat = point.lat,
                        lastLng = point.lng,
                        lastUpdated = System.currentTimeMillis()
                    )
                }

                for (devId in targetDeviceIds) {
                    if (devId.equals(creatorDevId, ignoreCase = true)) continue
                    ButterflyPointDeviceMapTable.assignDevice(
                        db = db,
                        pointId = point.pointId,
                        deviceId = devId,
                        initialStatus = ButterflyPointDeviceMapTable.STATUS_PENDING
                    )
                    point.devices[devId] = ButterflyDeviceEntry(
                        deviceId = devId,
                        status = ButterflyPointDeviceMapTable.STATUS_PENDING,
                        lastUpdated = System.currentTimeMillis()
                    )
                }

                // 3. Sub Master / Fact DB: change_log
                com.jdroidx.jfindme.data.local.tables.ChangeLogTable.logChange(
                    db = db,
                    identityId = 0L,
                    logId = "CHG_" + System.currentTimeMillis(),
                    category = "Safehouse",
                    action = "CREATE",
                    entityId = point.pointId,
                    changedBy = point.creatorDeviceId,
                    details = mapOf(
                        "point_name" to point.pointName,
                        "lat" to point.lat,
                        "lng" to point.lng,
                        "devices_count" to targetDeviceIds.size
                    )
                )

                // 4. Transaction DB: alert_history & sys_log
                com.jdroidx.jfindme.data.local.tables.AlertHistoryTable.logAlert(
                    db = db,
                    identityId = 0L,
                    agentId = point.creatorDeviceId,
                    alertType = "BUTTERFLY_RENDEZVOUS",
                    timestamp = System.currentTimeMillis(),
                    details = "Created Butterfly Point '${point.pointName}' with ${targetDeviceIds.size} devices.",
                    lat = point.lat,
                    lng = point.lng,
                    zoneId = point.pointId,
                    severity = "INFO"
                )

                dbHelper.saveSysLog(
                    "I",
                    "BUTTERFLY_DISPATCH",
                    "Dispatched rendezvous '${point.pointId}' to ${targetDeviceIds.size} nodes.",
                    null
                )

                // 5. Master DB [Online: Firebase RTDB /butterfly_points/{pointId}]
                val rtdb = FirebaseDatabase.getInstance().reference
                val cloudMap = hashMapOf<String, Any>(
                    "point_id" to point.pointId,
                    "creator_device_id" to point.creatorDeviceId,
                    "creator_name" to point.creatorName,
                    "point_name" to point.pointName,
                    "address" to point.address,
                    "lat" to point.lat,
                    "lng" to point.lng,
                    "geohash" to point.geohash,
                    "rendezvous_timestamp" to point.rendezvousTimestamp,
                    "expiry_timestamp" to point.expiryTimestamp,
                    "arrival_radius_meters" to point.arrivalRadiusMeters,
                    "notes" to point.notes,
                    "icon_type" to point.iconType,
                    "is_active" to true,
                    "is_archived" to false,
                    "created_at" to FirebaseComm.getForensicTimestamp(),
                    "updated_at" to FirebaseComm.getForensicTimestamp()
                )

                val devicesMap = hashMapOf<String, Any>()
                if (creatorDevId.isNotBlank()) {
                    val cleanCreatorId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(creatorDevId)
                    devicesMap[cleanCreatorId] = hashMapOf<String, Any>(
                        "device_id" to creatorDevId,
                        "device_name" to (point.creatorName.ifBlank { "Creator" }),
                        "status" to ButterflyPointDeviceMapTable.STATUS_ACCEPTED,
                        "distance_meters" to 0.0,
                        "last_lat" to point.lat,
                        "last_lng" to point.lng,
                        "last_updated" to System.currentTimeMillis()
                    )
                }
                for (devId in targetDeviceIds) {
                    if (devId.equals(creatorDevId, ignoreCase = true)) continue
                    val cleanDevId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(devId)
                    devicesMap[cleanDevId] = hashMapOf<String, Any>(
                        "device_id" to devId,
                        "status" to ButterflyPointDeviceMapTable.STATUS_PENDING,
                        "distance_meters" to 0.0,
                        "last_updated" to System.currentTimeMillis()
                    )
                }
                cloudMap["devices"] = devicesMap

                // [Variables] pointUpdates: HashMap<String, Any>, rtdb: DatabaseReference | [Constants] ButterflyPointDeviceMapTable.STATUS_ACCEPTED, STATUS_PENDING | [Dependencies] FirebaseComm, ButterflyPointDeviceMapTable | [Integration Points] ButterflyPointEngine, Firebase RTDB /butterfly_points
                // Directive 2 & GAP-04: Multi-path atomic updateChildren() rather than whole-object setValue() clobbering peer status
                val pointUpdates = hashMapOf<String, Any>(
                    "point_id" to point.pointId,
                    "creator_device_id" to point.creatorDeviceId,
                    "creator_name" to (point.creatorName.ifBlank { "Creator" }),
                    "point_name" to point.pointName,
                    "address" to point.address,
                    "lat" to point.lat,
                    "lng" to point.lng,
                    "geohash" to point.geohash,
                    "rendezvous_timestamp" to point.rendezvousTimestamp,
                    "expiry_timestamp" to point.expiryTimestamp,
                    "arrival_radius_meters" to point.arrivalRadiusMeters,
                    "notes" to point.notes,
                    "icon_type" to point.iconType,
                    "is_active" to true,
                    "is_archived" to false,
                    "created_at" to (cloudMap["created_at"] ?: FirebaseComm.getForensicTimestamp()),
                    "updated_at" to FirebaseComm.getForensicTimestamp()
                )

                // Directive 2: Ensure initial participant devices (pointUpdates["devices/$cleanDevId/..."]) are included so invited devices are not dropped
                if (creatorDevId.isNotBlank()) {
                    val cleanCreatorId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(creatorDevId)
                    pointUpdates["devices/$cleanCreatorId/device_id"] = creatorDevId
                    pointUpdates["devices/$cleanCreatorId/device_name"] = (point.creatorName.ifBlank { "Creator" })
                    pointUpdates["devices/$cleanCreatorId/status"] = ButterflyPointDeviceMapTable.STATUS_ACCEPTED
                    pointUpdates["devices/$cleanCreatorId/distance_meters"] = 0.0
                    pointUpdates["devices/$cleanCreatorId/last_lat"] = point.lat
                    pointUpdates["devices/$cleanCreatorId/last_lng"] = point.lng
                    pointUpdates["devices/$cleanCreatorId/last_updated"] = System.currentTimeMillis()
                }
                for (devId in targetDeviceIds) {
                    if (devId.equals(creatorDevId, ignoreCase = true)) continue
                    val cleanDevId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(devId)
                    pointUpdates["devices/$cleanDevId/device_id"] = devId
                    pointUpdates["devices/$cleanDevId/status"] = ButterflyPointDeviceMapTable.STATUS_PENDING
                    pointUpdates["devices/$cleanDevId/distance_meters"] = 0.0
                    pointUpdates["devices/$cleanDevId/last_updated"] = System.currentTimeMillis()
                }

                // Start local real-time sync and notify success immediately for high-speed UI responsiveness
                startActivePointPeerSync(context, point.pointId)
                onSuccess()

                // Asynchronous atomic cloud dispatch (Directive 2 & GAP-04)
                rtdb.child("butterfly_points").child(point.pointId).updateChildren(pointUpdates)
                    .addOnSuccessListener {
                        // Also mirror to each device's invitation inbox: /devices/{canonicalId}/butterfly_invitations/{pointId}
                        for (devId in targetDeviceIds) {
                            val canonicalTargetId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(devId)
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(canonicalTargetId)
                                .child("butterfly_invitations").child(point.pointId).setValue(cloudMap)
                            // 🛰️ Cross-Identity Mirroring: dispatch to all associated alias IDs (e.g. auth_uid, hardware_id)
                            try {
                                val altIds = dbHelper.getAssociatedIds(devId)
                                for (alt in altIds) {
                                    val canonicalAltId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(alt)
                                    if (canonicalAltId.isNotBlank() && canonicalAltId != canonicalTargetId) {
                                        com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(canonicalAltId)
                                            .child("butterfly_invitations").child(point.pointId).setValue(cloudMap)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    .addOnFailureListener { ex ->
                        SystemLogger.w(TAG, "Offline fallback: RTDB write queued (${ex.message})")
                    }

            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to dispatch butterfly point: ${e.message}")
                onError(e.message ?: "Failed to save butterfly point")
            }
        }.start()
    }

    /**
     * Updates peer response (ACCEPT or REJECT) across local Sub-Master DB and Firebase RTDB.
     */
    @JvmStatic
    @JvmOverloads
    fun respondToInvitation(
        context: Context,
        pointId: String,
        deviceId: String,
        accept: Boolean,
        rejectionReason: String? = null
    ) {
        submitPeerResponse(context, pointId, deviceId, accept, rejectionReason)
    }

    @JvmStatic
    fun submitPeerResponse(
        context: Context,
        pointId: String,
        deviceId: String,
        isAccepted: Boolean,
        rejectionReason: String? = null
    ) {
        Thread {
            try {
                val dbHelper = DatabaseHelper.getInstance(context)
                val db = dbHelper.writableDatabase
                val newStatus = if (isAccepted) ButterflyPointDeviceMapTable.STATUS_ACCEPTED else ButterflyPointDeviceMapTable.STATUS_REJECTED

                // Update local Sub-Master DB
                ButterflyPointDeviceMapTable.updateStatus(db, pointId, deviceId, newStatus, rejectionReason)

                // Audit log in Sub-Master / Fact change_log & Transaction DB alert_history
                com.jdroidx.jfindme.data.local.tables.ChangeLogTable.logChange(
                    db = db,
                    identityId = 0L,
                    logId = "CHG_" + System.currentTimeMillis(),
                    category = "Pairing_Devices",
                    action = "UPDATE",
                    entityId = "$pointId:$deviceId",
                    changedBy = deviceId,
                    details = mapOf("status" to newStatus, "reason" to (rejectionReason ?: ""))
                )
                com.jdroidx.jfindme.data.local.tables.AlertHistoryTable.logAlert(
                    db = db,
                    identityId = 0L,
                    agentId = deviceId,
                    alertType = "BUTTERFLY_RESPONSE",
                    timestamp = System.currentTimeMillis(),
                    details = "Device $deviceId $newStatus rendezvous invitation.",
                    lat = 0.0,
                    lng = 0.0,
                    zoneId = pointId,
                    severity = if (isAccepted) "INFO" else "WARNING"
                )

                // Sync to Firebase Cloud SSOT
                val rtdb = FirebaseDatabase.getInstance().reference
                val updates = hashMapOf<String, Any>(
                    "status" to newStatus,
                    "last_updated" to System.currentTimeMillis()
                )
                if (rejectionReason != null) {
                    updates["rejection_reason"] = rejectionReason
                }

                rtdb.child("butterfly_points").child(pointId).child("devices").child(deviceId).updateChildren(updates)
                val cleanDevId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(deviceId)
                com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(cleanDevId)
                    .child("butterfly_invitations").child(pointId).child("status").setValue(newStatus)

                // 🔔 Notify all UI observers in MapActivity & ViewModels
                com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                    context,
                    "BUTTERFLY_POINT",
                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPSERT,
                    pointId
                )
                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                    setPackage(context.packageName)
                    putExtra("category", "BUTTERFLY_POINT")
                    putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                    putExtra("EXTRA_ENTITY_ID", pointId)
                    putExtra("entity_id", pointId)
                    putExtra("EXTRA_CHANGE_TYPE", "UPDATE")
                    putExtra("change_type", "UPDATE")
                    putExtra("point_id", pointId)
                    putExtra("status", newStatus)
                }
                context.sendBroadcast(refreshIntent)

                // 🔄 Feedback Loop Closure: If accepted, immediately activate real-time peer sync
                if (isAccepted) {
                    startActivePointPeerSync(context, pointId)
                }

            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error submitting peer response: ${e.message}")
            }
        }.start()
    }

    // [Variables] activePointListeners: MutableMap<String, ValueEventListener>, quorumAlertedPoints: MutableSet<String>
    // [Constants] None
    // [Dependencies] FirebaseDatabase, ValueEventListener, DatabaseHelper, ButterflyPointDeviceMapTable, SystemLogger
    // [Integration Points] TrackingService, ButterflyPointDialog, MapActivity
    private val activePointListeners = mutableMapOf<String, com.google.firebase.database.ValueEventListener>()
    private val quorumAlertedPoints = mutableSetOf<String>()

    /**
     * Attaches a real-time listener to Firebase RTDB /butterfly_points/{pointId}/devices
     * to sync peer responses (ACCEPTED, REJECTED, ON_MOVE, REACHED) and distance metrics
     * into local SQLite Sub-Master DB in real time.
     */
    @JvmStatic
    fun startActivePointPeerSync(context: Context, pointId: String) {
        if (pointId.isBlank() || activePointListeners.containsKey(pointId)) return
        val devicesRef = FirebaseDatabase.getInstance().reference
            .child("butterfly_points").child(pointId).child("devices")

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (!snapshot.exists()) return
                Thread {
                    try {
                        val dbHelper = DatabaseHelper.getInstance(context)
                        val db = dbHelper.writableDatabase
                        var hasChanges = false
                        val locRepo = com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context)
                        val curPt = activeButterflyPoint ?: dbHelper.getButterflyPoint(pointId)
                        for (child in snapshot.children) {
                            val devId = child.child("device_id").getValue(String::class.java) ?: child.key ?: continue
                            val status = child.child("status").getValue(String::class.java) ?: ButterflyPointDeviceMapTable.STATUS_PENDING
                            val rejReason = child.child("rejection_reason").getValue(String::class.java)
                            val lastTs = FirebaseComm.safeLong(child.child("last_updated")) ?: System.currentTimeMillis()
                            val batPct = child.child("battery_pct").getValue(Int::class.java) ?: -1
                            val isSosActive = child.child("is_sos").getValue(Boolean::class.java) ?: false

                            // 🌟 SSOT Dynamic Origin-Shift: Resolve position from canonical location history table / repo!
                            val canonicalLoc = locRepo.getLatestLocationFor(devId)
                            val histLoc = if (canonicalLoc == null || (canonicalLoc.latitude == 0.0 && canonicalLoc.longitude == 0.0)) {
                                dbHelper.getLatestHistoricalLocation(devId)
                            } else null

                            val resolvedLat = canonicalLoc?.latitude ?: ((histLoc?.get("lat") as? Number)?.toDouble() ?: child.child("last_lat").getValue(Double::class.java) ?: 0.0)
                            val resolvedLng = canonicalLoc?.longitude ?: ((histLoc?.get("lng") as? Number)?.toDouble() ?: child.child("last_lng").getValue(Double::class.java) ?: 0.0)
                            val resolvedSpeedKmh = if (canonicalLoc != null) com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.toKmh(canonicalLoc.speed)
                                                   else ((histLoc?.get("speed") as? Number)?.toFloat() ?: 0.0f)
                            val resolvedBat = if (canonicalLoc != null && canonicalLoc.batteryLevel > 0) canonicalLoc.batteryLevel
                                              else if (histLoc != null && histLoc.containsKey("battery")) ((histLoc["battery"] as? Number)?.toInt() ?: batPct)
                                              else batPct

                            // Calculate Origin-Shifted Distance & ETA dynamically locally
                            val dynamicDist = if (resolvedLat != 0.0 && resolvedLng != 0.0 && curPt != null) {
                                calculateHaversineDistance(resolvedLat, resolvedLng, curPt.lat, curPt.lng)
                            } else {
                                child.child("distance_meters").getValue(Double::class.java) ?: 0.0
                            }
                            val dynamicEtaSec = if (dynamicDist > 0.0) {
                                calculateEtaMinutes(dynamicDist, resolvedSpeedKmh, context) * 60
                            } else {
                                child.child("eta_seconds").getValue(Int::class.java) ?: 0
                            }

                            // Dynamically evaluate RAG status locally
                            val dynamicEntry = ButterflyDeviceEntry(
                                deviceId = devId,
                                status = status,
                                distanceMeters = dynamicDist,
                                speedKmh = resolvedSpeedKmh,
                                etaMinutes = dynamicEtaSec / 60,
                                lastLat = resolvedLat,
                                lastLng = resolvedLng,
                                lastUpdated = lastTs,
                                batteryPct = resolvedBat,
                                isSosActive = isSosActive
                            )
                            val dynamicRag = if (curPt != null) evaluateButterflyRag(context, dynamicEntry, curPt.rendezvousTimestamp) else null
                            val dynamicRagStatus = dynamicRag?.name ?: child.child("rag_status").getValue(String::class.java) ?: "GREY"
                            val dynamicRagReason = dynamicEntry.ragReason.ifBlank { child.child("rag_reason").getValue(String::class.java) ?: "" }

                            // Update local Sub-Master SQLite with locally computed origin-shifted metadata
                            ButterflyPointDeviceMapTable.assignDevice(db, pointId, devId, null, status)
                            if (dynamicDist > 0.0 || status == ButterflyPointDeviceMapTable.STATUS_REACHED || resolvedBat != -1 || isSosActive || resolvedLat != 0.0) {
                                ButterflyPointDeviceMapTable.updateTelemetry(
                                    db = db,
                                    pointId = pointId,
                                    deviceId = devId,
                                    distanceMeters = dynamicDist,
                                    etaSeconds = dynamicEtaSec,
                                    telemetryTs = lastTs,
                                    isArrived = (status == ButterflyPointDeviceMapTable.STATUS_REACHED),
                                    batteryPct = resolvedBat,
                                    isSos = isSosActive,
                                    ragStatus = dynamicRagStatus,
                                    ragReason = dynamicRagReason,
                                    lastLat = resolvedLat,
                                    lastLng = resolvedLng
                                )
                            }
                            if (rejReason != null) {
                                ButterflyPointDeviceMapTable.updateStatus(db, pointId, devId, status, rejReason)
                            }

                            // Keep in-memory cache synchronized for immediate UI frame & rendering
                            val prevStatus = activeButterflyPoint?.devices?.get(devId)?.status
                            activeButterflyPoint?.devices?.get(devId)?.let { dEntry ->
                                dEntry.status = status
                                dEntry.distanceMeters = dynamicDist
                                dEntry.etaMinutes = dynamicEtaSec / 60
                                if (resolvedLat != 0.0) dEntry.lastLat = resolvedLat
                                if (resolvedLng != 0.0) dEntry.lastLng = resolvedLng
                                dEntry.lastUpdated = lastTs
                                dEntry.batteryPct = resolvedBat
                                dEntry.isSosActive = isSosActive
                                dEntry.ragStatus = dynamicRagStatus
                                dEntry.ragReason = dynamicRagReason
                            }

                            // 🎯 Peer Arrival Alert Cascade: When remote peer transitions to REACHED, notify operator
                            val mySelfId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
                            val isSelfNode = devId.equals(mySelfId, ignoreCase = true)
                            if (status == ButterflyPointDeviceMapTable.STATUS_REACHED &&
                                prevStatus != null &&
                                prevStatus != ButterflyPointDeviceMapTable.STATUS_REACHED &&
                                !isSelfNode) {
                                val devName = dbHelper.getDeviceCentricName(devId).ifBlank { "Device $devId" }
                                val ptName = activeButterflyPoint?.pointName ?: "Butterfly Point"
                                com.jdroidx.jfindme.engine.alert.AlertManager.showButterflyArrivalNotification(context, ptName, devName)
                            }

                            hasChanges = true
                        }

                        // 🎯 Full Quorum Assembly Cascade: When all active members arrive
                        val allDevices = dbHelper.getButterflyDevicesForPoint(pointId)
                        val eligibleDevices = allDevices.filter {
                            !it.status.equals(ButterflyPointDeviceMapTable.STATUS_REJECTED, ignoreCase = true) &&
                            !it.status.equals(ButterflyPointDeviceMapTable.STATUS_REVOKED, ignoreCase = true) &&
                            !it.status.equals(ButterflyPointDeviceMapTable.STATUS_TIMED_OUT, ignoreCase = true)
                        }
                        if (eligibleDevices.size >= 2 && eligibleDevices.all { it.status == ButterflyPointDeviceMapTable.STATUS_REACHED }) {
                            if (!quorumAlertedPoints.contains(pointId)) {
                                quorumAlertedPoints.add(pointId)
                                val ptName = activeButterflyPoint?.pointName ?: "Butterfly Point"
                                com.jdroidx.jfindme.engine.alert.AlertManager.showButterflyQuorumNotification(context, ptName)
                            }
                        }

                        if (hasChanges) {
                            val intent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                                setPackage(context.packageName)
                                putExtra("category", "BUTTERFLY_POINT")
                                putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                                putExtra("EXTRA_ENTITY_ID", pointId)
                                putExtra("entity_id", pointId)
                                putExtra("EXTRA_CHANGE_TYPE", "UPDATE")
                                putExtra("change_type", "UPDATE")
                            }
                            context.sendBroadcast(intent)
                        }
                    } catch (e: Exception) {
                        SystemLogger.e(TAG, "Error in activePointPeerSync for $pointId: ${e.message}")
                    }
                }.start()
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                SystemLogger.w(TAG, "activePointPeerSync cancelled for $pointId: ${error.message}")
            }
        }

        devicesRef.addValueEventListener(listener)
        activePointListeners[pointId] = listener
        SystemLogger.i(TAG, "Started real-time peer sync for Butterfly Point: $pointId")
    }

    @JvmStatic
    fun stopActivePointPeerSync(pointId: String) {
        quorumAlertedPoints.remove(pointId)
        val listener = activePointListeners.remove(pointId) ?: return
        try {
            FirebaseDatabase.getInstance().reference
                .child("butterfly_points").child(pointId).child("devices")
                .removeEventListener(listener)
            SystemLogger.i(TAG, "Stopped real-time peer sync for Butterfly Point: $pointId")
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error stopping activePointPeerSync: ${e.message}")
        }
    }

    @JvmStatic
    fun stopAllActivePointPeerSyncs() {
        for ((pId, listener) in activePointListeners) {
            try {
                FirebaseDatabase.getInstance().reference
                    .child("butterfly_points").child(pId).child("devices")
                    .removeEventListener(listener)
            } catch (_: Exception) {}
        }
        activePointListeners.clear()
    }

    @JvmStatic
    fun isPeerSyncActive(pointId: String): Boolean {
        return activePointListeners.containsKey(pointId)
    }

    // [Variables] batIntent: Intent, batLevel: Int, prefs: SharedPreferences, isSos: Boolean, myEntry: ButterflyDeviceEntry, isParticipant: Boolean, currentStatus: String, dist: Double, newStatus: String, etaMinutes: Int, etaSec: Int, db: SQLiteDatabase, updateMap: HashMap
    // [Constants] None
    // [Dependencies] DatabaseHelper, ButterflyPointDeviceMapTable, FirebaseDatabase, AlertManager, SystemLogger, DeviceRagEngine
    // [Integration Points] TrackingService, LocationRepository, MapActivity
    /**
     * Evaluates tactical Red-Amber-Green (RAG) telemetry status for a participating device.
     * Incorporates scheduled rendezvous time, ETA, battery percentage, telemetry staleness,
     * SOS distress, and explicit acceptance/rejection status.
     */
    @JvmStatic
    fun evaluateButterflyRag(
        context: Context,
        deviceEntry: ButterflyDeviceEntry,
        rendezvousTimestamp: Long
    ): com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus {
        // 1. Explicit Rejection or Cancellation -> RED
        if (deviceEntry.status.equals(ButterflyPointDeviceMapTable.STATUS_REJECTED, ignoreCase = true)) {
            deviceEntry.ragStatus = "RED"
            deviceEntry.ragReason = "Declined Rendezvous"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.RED
        }

        // 1b. Revocation by Organizer -> GREY
        if (deviceEntry.status.equals(ButterflyPointDeviceMapTable.STATUS_REVOKED, ignoreCase = true)) {
            deviceEntry.ragStatus = "GREY"
            deviceEntry.ragReason = "Invitation Revoked"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.GREY
        }

        // 1c. Timed Out (> 24h or past rendezvous without answer) -> RED
        if (deviceEntry.status.equals(ButterflyPointDeviceMapTable.STATUS_TIMED_OUT, ignoreCase = true)) {
            deviceEntry.ragStatus = "RED"
            deviceEntry.ragReason = "Invitation Timed Out"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.RED
        }

        // 2. Active Emergency SOS Distress -> RED
        if (deviceEntry.isSosActive) {
            deviceEntry.ragStatus = "RED"
            deviceEntry.ragReason = "🚨 SOS Distress Active"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.RED
        }

        val now = System.currentTimeMillis()
        val staleMins = com.jdroidx.jfindme.utils.RegistryManager.getStaleInactiveMinutes(context)
        val criticalHours = com.jdroidx.jfindme.utils.RegistryManager.getCriticalOfflineHours(context)
        val lowBatThresh = com.jdroidx.jfindme.utils.RegistryManager.getLowBatteryThreshold(context)

        val deltaTelemetryMs = if (deviceEntry.lastUpdated > 0L) (now - deviceEntry.lastUpdated).coerceAtLeast(0L) else 0L

        // 3. Critical Offline (> criticalHours or battery <= 5%) -> RED
        if (deviceEntry.lastUpdated > 0L && deltaTelemetryMs > (criticalHours * 3600 * 1000L)) {
            deviceEntry.ragStatus = "RED"
            deviceEntry.ragReason = "Offline (> ${criticalHours}h)"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.RED
        }
        if (deviceEntry.batteryPct in 1..5) {
            deviceEntry.ragStatus = "RED"
            deviceEntry.ragReason = "Critical Battery (${deviceEntry.batteryPct}%)"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.RED
        }

        // 4. Overdue Check: Meeting time passed by > 15m and device has NOT reached -> RED
        if (deviceEntry.status != ButterflyPointDeviceMapTable.STATUS_REACHED && rendezvousTimestamp > 0L && (now - rendezvousTimestamp) > (15 * 60 * 1000L)) {
            val overdueMins = (now - rendezvousTimestamp) / 60000L
            deviceEntry.ragStatus = "RED"
            deviceEntry.ragReason = "Overdue by ${overdueMins}m"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.RED
        }

        // 5. Arrived -> GREEN
        if (deviceEntry.status.equals(ButterflyPointDeviceMapTable.STATUS_REACHED, ignoreCase = true)) {
            deviceEntry.ragStatus = "GREEN"
            deviceEntry.ragReason = "Reached Destination"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.GREEN
        }

        // 6. Pending Invitation Warning -> AMBER if within approach window to rendezvous, otherwise GREY; Timed Out if exceeded
        if (deviceEntry.status.equals(ButterflyPointDeviceMapTable.STATUS_PENDING, ignoreCase = true)) {
            val timeoutHours = com.jdroidx.jfindme.utils.RegistryManager.getButterflyInvitationTimeoutHours(context)
            val timeoutMs = timeoutHours * 3600 * 1000L
            val approachWindowMins = com.jdroidx.jfindme.utils.RegistryManager.getButterflyApproachWindowMinutes(context)
            val approachWindowMs = approachWindowMins * 60 * 1000L
            val isTimedOut = (rendezvousTimestamp > 0L && now > rendezvousTimestamp) ||
                             (deviceEntry.lastUpdated > 0L && (now - deviceEntry.lastUpdated) > timeoutMs)
            if (isTimedOut) {
                deviceEntry.status = ButterflyPointDeviceMapTable.STATUS_TIMED_OUT
                deviceEntry.ragStatus = "RED"
                deviceEntry.ragReason = "Invitation Timed Out (> ${timeoutHours}h)"
                return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.RED
            } else if (rendezvousTimestamp > 0L && (rendezvousTimestamp - now) in 1..approachWindowMs) {
                deviceEntry.ragStatus = "AMBER"
                deviceEntry.ragReason = "Pending Confirmation (< ${approachWindowMins}m)"
                return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.AMBER
            } else {
                deviceEntry.ragStatus = "GREY"
                deviceEntry.ragReason = "Awaiting Response"
                return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.GREY
            }
        }

        // 7. Low Battery Warning -> AMBER
        if (deviceEntry.batteryPct in 6..lowBatThresh) {
            deviceEntry.ragStatus = "AMBER"
            deviceEntry.ragReason = "Low Battery (${deviceEntry.batteryPct}%)"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.AMBER
        }

        // 8. Stale Telemetry Warning -> AMBER (Between staleMins and criticalHours)
        if (deviceEntry.lastUpdated > 0L && deltaTelemetryMs > (staleMins * 60 * 1000L)) {
            val minsAgo = deltaTelemetryMs / 60000L
            deviceEntry.ragStatus = "AMBER"
            deviceEntry.ragReason = "Stale Telemetry (${minsAgo}m ago)"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.AMBER
        }

        // 9. Schedule ETA Delay Check: If ETA exceeds scheduled time by > 5m -> AMBER
        if (deviceEntry.etaMinutes > 0 && rendezvousTimestamp > 0L) {
            val estimatedArrivalEpoch = now + (deviceEntry.etaMinutes * 60 * 1000L)
            if (estimatedArrivalEpoch > rendezvousTimestamp + (5 * 60 * 1000L)) {
                val delayMins = (estimatedArrivalEpoch - rendezvousTimestamp) / 60000L
                deviceEntry.ragStatus = "AMBER"
                deviceEntry.ragReason = "Delayed by ~${delayMins}m"
                return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.AMBER
            }
        }

        // 9b. Dynamic Distance Boundary Safeguard: If device is beyond configured max distance threshold -> AMBER
        val maxDistKm = com.jdroidx.jfindme.utils.RegistryManager.getButterflyMaxDistanceKm(context)
        if (deviceEntry.distanceMeters > (maxDistKm * 1000.0)) {
            deviceEntry.ragStatus = "AMBER"
            deviceEntry.ragReason = "Far Out (> ${(deviceEntry.distanceMeters / 1000.0).roundToInt()} km)"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.AMBER
        }

        // 9c. Missing Location Telemetry Check -> AMBER (Device has accepted or pending, but zero GPS locked)
        if (deviceEntry.lastLat == 0.0 && deviceEntry.lastLng == 0.0 && deviceEntry.distanceMeters == 0.0) {
            deviceEntry.ragStatus = "AMBER"
            deviceEntry.ragReason = "Awaiting GPS Fix"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.AMBER
        }

        // 9d. Stationary Warning: Meeting in < 30m, distance > 1km, speed < minSpeedKmh -> AMBER
        val minSpeedKmh = com.jdroidx.jfindme.utils.RegistryManager.getButterflyMinSpeedKmh(context)
        if (rendezvousTimestamp > 0L && (rendezvousTimestamp - now) in 1..(30 * 60 * 1000L) &&
            deviceEntry.distanceMeters > 1000.0 && deviceEntry.speedKmh < minSpeedKmh) {
            deviceEntry.ragStatus = "AMBER"
            deviceEntry.ragReason = "Stationary (${formatDistance(deviceEntry.distanceMeters)} away)"
            return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.AMBER
        }

        // 10. Default: On-track and healthy -> GREEN
        deviceEntry.ragStatus = "GREEN"
        deviceEntry.ragReason = if (deviceEntry.distanceMeters > 0) "On Track • ${formatDistance(deviceEntry.distanceMeters)}" else "On Track"
        return com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus.GREEN
    }

    /**
     * Human-in-the-Loop SOP: Sends an immediate high-priority nudge/poke command
     * to a delayed or pending rendezvous participant.
     */
    @JvmStatic
    fun nudgeParticipant(context: Context, targetDeviceId: String, pointName: String) {
        if (targetDeviceId.isBlank()) return
        Thread {
            try {
                val cleanTargetId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(targetDeviceId)
                val pokePayload = mapOf(
                    "command" to "POKE_LOCATION_UPDATE",
                    "type" to "RENDEZVOUS_NUDGE",
                    "title" to "🦋 Rendezvous Nudge: $pointName",
                    "point_name" to pointName,
                    "message" to "Organizer requested your current progress and arrival update.",
                    "timestamp" to System.currentTimeMillis()
                )
                com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(cleanTargetId)
                    .child("commands").child("poke")
                    .setValue(pokePayload)
                SystemLogger.i(TAG, "Sent rendezvous nudge to $cleanTargetId for '$pointName'")
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to send nudge: ${e.message}")
            }
        }.start()
    }

    /**
     * Human-in-the-Loop SOP: Extends rendezvous time (+15m, +30m, +1h) for active point
     * when traffic or delays occur, updating SQLite and propagating to all participants.
     */
    @JvmStatic
    fun extendRendezvousTime(context: Context, pointId: String, additionalMinutes: Long) {
        if (pointId.isBlank() || additionalMinutes <= 0) return
        Thread {
            try {
                DatabaseHelper.getInstance(context).extendButterflyPointTime(pointId, additionalMinutes)
                val addMs = additionalMinutes * 60 * 1000L
                if (activeButterflyPoint?.pointId == pointId) {
                    activeButterflyPoint?.rendezvousTimestamp = (activeButterflyPoint?.rendezvousTimestamp ?: 0L) + addMs
                    activeButterflyPoint?.expiryTimestamp = (activeButterflyPoint?.expiryTimestamp ?: 0L) + addMs
                }
                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                    setPackage(context.packageName)
                    putExtra("category", "BUTTERFLY_POINT")
                    putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                    putExtra("EXTRA_ENTITY_ID", pointId)
                    putExtra("EXTRA_CHANGE_TYPE", "UPDATE")
                }
                context.sendBroadcast(refreshIntent)
                SystemLogger.i(TAG, "Extended rendezvous $pointId by $additionalMinutes minutes")
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to extend rendezvous time: ${e.message}")
            }
        }.start()
    }

    /**
     * [Variables] dbHelper: DatabaseHelper, refreshIntent: Intent
     * [Constants] None
     * [Dependencies] DatabaseHelper, SystemLogger, Context
     * [Integration Points] ButterflyPointDialog.java, MapActivity.java
     *
     * Dual-Engine Archival: Cleans up local SQLite and cloud Firebase RTDB state
     * Stops peer real-time synchronization listeners to prevent leaks (GAP-04 & GAP-07).
     */
    @JvmStatic
    fun archiveButterflyPoint(context: Context, pointId: String, onComplete: (() -> Unit)? = null) {
        if (pointId.isBlank()) return
        stopActivePointPeerSync(pointId)
        if (activeButterflyPoint?.pointId == pointId) {
            clearButterflyOverlay()
        }
        Thread {
            try {
                val dbHelper = DatabaseHelper.getInstance(context)
                dbHelper.archiveButterflyPoint(pointId)
                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                    setPackage(context.packageName)
                    putExtra("category", "BUTTERFLY_POINT")
                    putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                    putExtra("EXTRA_ENTITY_ID", pointId)
                    putExtra("EXTRA_CHANGE_TYPE", "DELETE")
                }
                context.sendBroadcast(refreshIntent)
                SystemLogger.i(TAG, "Archived Butterfly Point: $pointId cleanly across SQLite and Cloud.")
                onComplete?.invoke()
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error archiving Butterfly Point: ${e.message}")
            }
        }.start()
    }

    /**
     * [Variables] dbHelper: DatabaseHelper, activeCount: Int, batteryInfo: Pair<Int, Boolean>, batteryPct: Int, isCharging: Boolean
     * [Constants] MAX_ACTIVE_POINTS = 5, MIN_BATTERY_PCT = 10
     * [Dependencies] DatabaseHelper, DeviceIdentityHelper, Context
     * [Integration Points] ButterflyPointDialog.java, MapActivity.java
     *
     * Precondition Enforcer (GAP-10 Remediation):
     * Validates quota availability (max 5 active points) and hardware battery health (> 10%).
     */
    @JvmStatic
    fun canCreateButterflyPoint(context: Context): Pair<Boolean, String> {
        val dbHelper = DatabaseHelper.getInstance(context)
        val activeCount = dbHelper.getActiveButterflyPoints().size
        if (activeCount >= 5) {
            return Pair(false, "Maximum active rendezvous limit (5) reached. Please archive existing points before creating new ones.")
        }
        val batteryInfo = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getResolvedBatteryAndCharging(context)
        val batteryPct = batteryInfo.first
        val isCharging = batteryInfo.second
        if (batteryPct in 1..10 && !isCharging) {
            return Pair(false, "Battery critical ($batteryPct%). Connect charger before starting high-precision rendezvous tracking.")
        }
        return Pair(true, "OK")
    }

    /**
     * Delegated Mesh Ingress: Adds additional connected devices to an existing Butterfly Point.
     * Can be invoked by Creator or an Accepted Participant sharing with their connected peers.
     */
    @JvmStatic
    fun inviteAdditionalDevices(
        context: Context,
        pointId: String,
        newDeviceIds: List<String>,
        invitedByDeviceId: String,
        onComplete: () -> Unit
    ) {
        if (pointId.isBlank() || newDeviceIds.isEmpty()) return
        Thread {
            try {
                val dbHelper = DatabaseHelper.getInstance(context)
                val db = dbHelper.writableDatabase
                val pt = dbHelper.getButterflyPoint(pointId) ?: return@Thread
                val rtdb = FirebaseDatabase.getInstance().reference

                for (devId in newDeviceIds) {
                    ButterflyPointDeviceMapTable.assignDevice(
                        db = db,
                        pointId = pointId,
                        deviceId = devId,
                        initialStatus = ButterflyPointDeviceMapTable.STATUS_PENDING,
                        invitedBy = invitedByDeviceId
                    )
                    val devEntry = hashMapOf<String, Any>(
                        "device_id" to devId,
                        "status" to ButterflyPointDeviceMapTable.STATUS_PENDING,
                        "distance_meters" to 0.0,
                        "invited_by" to invitedByDeviceId,
                        "last_updated" to System.currentTimeMillis()
                    )
                    val cleanDevId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(devId)
                    rtdb.child("butterfly_points").child(pointId).child("devices").child(cleanDevId).setValue(devEntry)

                    // Mirror invitation to device's inbox
                    val cloudMap = hashMapOf<String, Any>(
                        "point_id" to pt.pointId,
                        "creator_device_id" to pt.creatorDeviceId,
                        "creator_name" to pt.creatorName,
                        "point_name" to pt.pointName,
                        "address" to pt.address,
                        "lat" to pt.lat,
                        "lng" to pt.lng,
                        "rendezvous_timestamp" to pt.rendezvousTimestamp,
                        "expiry_timestamp" to pt.expiryTimestamp,
                        "arrival_radius_meters" to pt.arrivalRadiusMeters,
                        "invited_by" to invitedByDeviceId,
                        "is_active" to true,
                        "is_archived" to false
                    )
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(cleanDevId)
                        .child("butterfly_invitations").child(pointId).setValue(cloudMap)
                }

                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                    setPackage(context.packageName)
                    putExtra("category", "BUTTERFLY_POINT")
                    putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                    putExtra("EXTRA_ENTITY_ID", pointId)
                    putExtra("EXTRA_CHANGE_TYPE", "UPDATE")
                }
                context.sendBroadcast(refreshIntent)
                onComplete()
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error inviting additional devices: ${e.message}")
            }
        }.start()
    }

    /**
     * Revokes a pending or timed-out invitation.
     */
    @JvmStatic
    fun revokeDeviceInvitation(context: Context, pointId: String, targetDeviceId: String) {
        if (pointId.isBlank() || targetDeviceId.isBlank()) return
        Thread {
            try {
                val db = DatabaseHelper.getInstance(context).writableDatabase
                ButterflyPointDeviceMapTable.updateStatus(db, pointId, targetDeviceId, ButterflyPointDeviceMapTable.STATUS_REVOKED, "Revoked by organizer")
                val rtdb = FirebaseDatabase.getInstance().reference
                val cleanDevId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(targetDeviceId)
                rtdb.child("butterfly_points").child(pointId).child("devices").child(cleanDevId).child("status")
                    .setValue(ButterflyPointDeviceMapTable.STATUS_REVOKED)
                com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(cleanDevId)
                    .child("butterfly_invitations").child(pointId).removeValue()

                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                    setPackage(context.packageName)
                    putExtra("category", "BUTTERFLY_POINT")
                    putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                    putExtra("EXTRA_ENTITY_ID", pointId)
                    putExtra("EXTRA_CHANGE_TYPE", "UPDATE")
                }
                context.sendBroadcast(refreshIntent)
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error revoking invitation: ${e.message}")
            }
        }.start()
    }

    /**
     * Resends/re-invites a pending or timed-out node.
     */
    @JvmStatic
    fun reInviteDevice(context: Context, pointId: String, targetDeviceId: String) {
        if (pointId.isBlank() || targetDeviceId.isBlank()) return
        Thread {
            try {
                val db = DatabaseHelper.getInstance(context).writableDatabase
                ButterflyPointDeviceMapTable.updateStatus(db, pointId, targetDeviceId, ButterflyPointDeviceMapTable.STATUS_PENDING, null)
                val rtdb = FirebaseDatabase.getInstance().reference
                val now = System.currentTimeMillis()
                val cleanDevId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(targetDeviceId)
                rtdb.child("butterfly_points").child(pointId).child("devices").child(cleanDevId)
                    .updateChildren(mapOf("status" to ButterflyPointDeviceMapTable.STATUS_PENDING, "last_updated" to now))
                rtdb.child("butterfly_points").child(pointId).get().addOnSuccessListener { snap ->
                    if (snap.exists()) {
                        com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(cleanDevId)
                            .child("butterfly_invitations").child(pointId).setValue(snap.value)
                    }
                }
                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                    setPackage(context.packageName)
                    putExtra("category", "BUTTERFLY_POINT")
                    putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                    putExtra("EXTRA_ENTITY_ID", pointId)
                    putExtra("EXTRA_CHANGE_TYPE", "UPDATE")
                }
                context.sendBroadcast(refreshIntent)
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error re-inviting device: ${e.message}")
            }
        }.start()
    }

    /**
     * Evaluates local device's distance and status relative to all active Butterfly Points.
     * Called whenever a new GPS fix is acquired by TrackingService.
     */
    @JvmStatic
    fun processLocalLocationUpdate(context: Context, latitude: Double, longitude: Double, speedKmh: Float) {
        if (latitude == 0.0 && longitude == 0.0) return
        Thread {
            try {
                val dbHelper = DatabaseHelper.getInstance(context)
                val activePoints = dbHelper.getActiveButterflyPoints()
                if (activePoints.isEmpty()) return@Thread

                val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
                val rtdb = FirebaseDatabase.getInstance().reference
                val now = System.currentTimeMillis()
                val hysteresisDeadband = com.jdroidx.jfindme.utils.RegistryManager.getButterflyHysteresisMeters(context).toDouble()

                val batIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                val batLevel = batIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                val isSos = prefs.getBoolean("is_sos_active", false)

                for (pt in activePoints) {
                    val myEntry = pt.devices[myAgentId]
                    val isParticipant = myEntry != null || pt.creatorDeviceId == myAgentId
                    if (!isParticipant) continue

                    val currentStatus = myEntry?.status ?: ButterflyPointDeviceMapTable.STATUS_ACCEPTED
                    if (currentStatus == ButterflyPointDeviceMapTable.STATUS_REJECTED) continue

                    // ⏰ Closed-Loop Dynamic Approach-Window Lifecycle:
                    // Auto-engage peer sync if inside approach window, auto-disengage if expired
                    val approachWindowMs = com.jdroidx.jfindme.utils.RegistryManager.getButterflyApproachWindowMinutes(context) * 60 * 1000L
                    val timeToRendezvous = pt.rendezvousTimestamp - now
                    val isWithinApproach = timeToRendezvous in (-2 * 3600 * 1000L)..approachWindowMs
                    if (isWithinApproach && !isPeerSyncActive(pt.pointId)) {
                        startActivePointPeerSync(context, pt.pointId)
                    } else if (!isWithinApproach && isPeerSyncActive(pt.pointId)) {
                        stopActivePointPeerSync(pt.pointId)
                    }

                    val dist = calculateHaversineDistance(latitude, longitude, pt.lat, pt.lng)
                    val newStatus = evaluateDeviceStatus(
                        deviceId = myAgentId,
                        currentStatus = currentStatus,
                        distanceMeters = dist,
                        arrivalRadiusMeters = pt.arrivalRadiusMeters,
                        hysteresisDeadbandMeters = hysteresisDeadband
                    )

                    val etaMinutes = calculateEtaMinutes(dist, speedKmh, context)
                    val etaSec = etaMinutes * 60

                    // Tactical RAG Status Evaluation
                    val entryForRag = ButterflyDeviceEntry(
                        deviceId = myAgentId,
                        status = newStatus,
                        distanceMeters = dist,
                        speedKmh = speedKmh,
                        etaMinutes = etaMinutes,
                        lastLat = latitude,
                        lastLng = longitude,
                        lastUpdated = now,
                        batteryPct = batLevel,
                        isSosActive = isSos
                    )
                    evaluateButterflyRag(context, entryForRag, pt.rendezvousTimestamp)

                    // 🔋 Anti-Thrashing Stationary Jitter Suppression (Low-Cost Model):
                    // When device is stationary (speed < minSpeed and displacement < deadband),
                    // suppress redundant SQLite writes and breadcrumb logging to once per 15 mins.
                    val prevLat = myEntry?.lastLat ?: 0.0
                    val prevLng = myEntry?.lastLng ?: 0.0
                    val displacementMeters = if (prevLat != 0.0 && prevLng != 0.0) {
                        calculateHaversineDistance(latitude, longitude, prevLat, prevLng)
                    } else {
                        Double.MAX_VALUE
                    }
                    val minSpeedKmh = com.jdroidx.jfindme.utils.RegistryManager.getButterflyMinSpeedKmh(context)
                    val isMoving = speedKmh >= minSpeedKmh || displacementMeters >= hysteresisDeadband
                    val statusChanged = newStatus != currentStatus
                    val previousSos = myEntry?.isSosActive ?: false
                    val sosChanged = isSos != previousSos
                    val timeSinceLastUpdateMs = now - (myEntry?.lastUpdated ?: 0L)
                    val isStationaryHeartbeatDue = timeSinceLastUpdateMs >= 900000L // 15 min heartbeat

                    val shouldPersistTelemetry = isMoving || statusChanged || sosChanged || isStationaryHeartbeatDue || (myEntry == null)

                    if (shouldPersistTelemetry) {
                        // 1. Update SQLite Sub-Master DB with RAG Telemetry
                        val db = dbHelper.writableDatabase
                        ButterflyPointDeviceMapTable.updateTelemetry(
                            db = db,
                            pointId = pt.pointId,
                            deviceId = myAgentId,
                            distanceMeters = dist,
                            etaSeconds = etaSec,
                            telemetryTs = now,
                            isArrived = (newStatus == ButterflyPointDeviceMapTable.STATUS_REACHED),
                            batteryPct = batLevel,
                            isSos = isSos,
                            ragStatus = entryForRag.ragStatus,
                            ragReason = entryForRag.ragReason,
                            lastLat = latitude,
                            lastLng = longitude
                        )

                        // 1b. Tier 3: Transactional Database Breadcrumb
                        com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.logTelemetry(
                            db = db,
                            pointId = pt.pointId,
                            deviceId = myAgentId,
                            lat = latitude,
                            lng = longitude,
                            speedKmh = speedKmh,
                            distanceMeters = dist,
                            etaSeconds = etaSec,
                            batteryPct = batLevel,
                            isSos = isSos,
                            ragStatus = entryForRag.ragStatus,
                            ragReason = entryForRag.ragReason,
                            timestamp = now
                        )
                    }

                    // 2. 🛡️ Zero-Duplication Cloud SSOT Protocol:
                    // Raw GPS coordinates, speed, and battery are ALREADY dispatched to /devices/{myAgentId}/location 
                    // by the canonical TrackingService / LocationRepository pipeline.
                    // We strictly avoid duplicating raw telemetry into /butterfly_points/{pointId}/telemetry.
                    // We ONLY dispatch a cloud update to /butterfly_points/ if a critical state transition occurs:
                    // (a) Member arrived within arrival perimeter (Terminal Arrival Latch)
                    // (b) SOS Emergency activation
                    val cleanMyAgentId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(myAgentId)
                    if (newStatus == ButterflyPointDeviceMapTable.STATUS_REACHED && currentStatus != ButterflyPointDeviceMapTable.STATUS_REACHED) {
                        val arrivalMap = hashMapOf<String, Any>(
                            "status" to ButterflyPointDeviceMapTable.STATUS_REACHED,
                            "arrival_timestamp" to now,
                            "distance_meters" to dist,
                            "last_updated" to now
                        )
                        rtdb.child("butterfly_points").child(pt.pointId).child("devices").child(cleanMyAgentId)
                            .updateChildren(arrivalMap)
                        // Also update personal invitation status
                        com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(cleanMyAgentId)
                            .child("butterfly_invitations").child(pt.pointId).child("status")
                            .setValue(ButterflyPointDeviceMapTable.STATUS_REACHED)

                        // 3. Fire arrival notification locally
                        com.jdroidx.jfindme.engine.alert.AlertManager.showButterflyArrivalNotification(context, pt.pointName, "You")
                    } else if (sosChanged && isSos) {
                        val sosMap = hashMapOf<String, Any>(
                            "is_sos" to true,
                            "last_updated" to now
                        )
                        rtdb.child("butterfly_points").child(pt.pointId).child("devices").child(cleanMyAgentId)
                            .updateChildren(sosMap)
                    }
                }

            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error in processLocalLocationUpdate: ${e.message}")
            }
        }.start()
    }
}
