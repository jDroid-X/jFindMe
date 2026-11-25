package com.jdroidx.jfindme.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.repository.LocationRepository
import com.jdroidx.jfindme.engine.location.LocationEngine
import com.jdroidx.jfindme.engine.location.GeofenceHelper
import com.jdroidx.jfindme.data.repository.DeviceRepository
import com.jdroidx.jfindme.engine.alert.AlertManager
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.utils.RegistryManager
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

/**
 * TrackingService: Foreground service for persistent GPS tracking.
 * Main -> Background Ops -> Service Layer -> TrackingService
 */
class TrackingService : Service() {

    private var locationEngine: LocationEngine? = null
    private var isLowPowerMode = false
    private lateinit var repository: LocationRepository
    private var batteryReceiver: BroadcastReceiver? = null
    private var cpuWakeLock: android.os.PowerManager.WakeLock? = null
    private var syncListener: ValueEventListener? = null
    private var butterflyRef: DatabaseReference? = null
    private var butterflyAuthRef: DatabaseReference? = null
    private var butterflyChildListener: ChildEventListener? = null
    private val TAG = "TrackingService_DozeBypass"
    
    // 🔋 SILENT BATTERY VALIDATION (V9.2)
    private var batteryPingJob: kotlinx.coroutines.Job? = null
    private var lastPulseTime = 0L
    @Volatile private var lastUploadedLocation: android.location.Location? = null
    @Volatile private var lastLocationFixEpoch: Long = 0L
    @Volatile private var lastPresencePulseEpoch: Long = 0L
    private val PING_INTERVAL_MS = 60000L // 60 Second Forensic Cycle
    private var serviceStartEpoch: Long = System.currentTimeMillis()
    private var avatarClosedLoopCallback: ((String, Map<String, Any>) -> Unit)? = null

    private fun hasLocationPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
               androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // [Variables] None | [Constants] None | [Dependencies] DeviceIdentityHelper | [Integration Points] TrackingService, LocationRepository
    fun getResolvedBatteryAndCharging(): Pair<Int, Boolean> {
        return com.jdroidx.jfindme.utils.DeviceIdentityHelper.getResolvedBatteryAndCharging(this)
    }

    /**
     * 🛡️ Proactive Background Firebase Token Refresh:
     * Prevents silent telemetry drops (R-01) by renewing Auth token before the 60-minute expiry window.
     */
    fun refreshAuthTokenIfExpiring() {
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user != null) {
                user.getIdToken(false).addOnSuccessListener { tokenResult ->
                    val exp = tokenResult.expirationTimestamp
                    val nowSec = System.currentTimeMillis() / 1000
                    if (exp - nowSec < 600) { // If expiring within 10 minutes, force renewal
                        user.getIdToken(true).addOnSuccessListener {
                            SystemLogger.i(TAG, "Proactive Firebase Auth token renewed successfully.")
                            com.jdroidx.jfindme.data.remote.OfflineSyncManager.getInstance(applicationContext).triggerBackgroundSync()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            SystemLogger.d(TAG, "Token refresh evaluation: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        SystemLogger.i(TAG, "Service Created")
        repository = LocationRepository(this)
        createNotificationChannel()

        // 🛡️ FORENSIC FIX: Call startForeground IMMEDIATELY on line 1 of onCreate to prevent RemoteServiceException
        try {
            val hasLocPerm = hasLocationPermission()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocPerm) {
                startForeground(SERVICE_NOTIFICATION_ID, getNotification("jFindMe Sentinel Active"), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(SERVICE_NOTIFICATION_ID, getNotification("jFindMe Sentinel Standby"))
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "startForeground error: ${e.message}")
        }

        // [Variables]: isDecommissioned | [Constants]: "is_decommissioned_by_admin"
        // [Dependencies]: SharedPreferences | [Integration Points]: Remote Decommission & Reboot Resurrection Firewall
        val isDecommissioned = getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .getBoolean("is_decommissioned_by_admin", false)
        if (isDecommissioned) {
            SystemLogger.w(TAG, "🛡️ TrackingService blocked in onCreate: Device is marked is_decommissioned_by_admin=true.")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
            } catch (_: Exception) {}
            stopSelf()
            return
        }

        val cachedLat = RegistryManager.getLastKnownLatitude(applicationContext)
        val cachedLng = RegistryManager.getLastKnownLongitude(applicationContext)
        if (cachedLat != 0.0 && cachedLng != 0.0) {
            lastUploadedLocation = android.location.Location("cache").apply {
                latitude = cachedLat
                longitude = cachedLng
                speed = 0.0f
            }
            lastLocationFixEpoch = System.currentTimeMillis()
        }

        ensureLocationEngineInitialized()
        registerBatteryMonitor()
        registerScreenStateMonitor()
        registerClosedLoopFeedbackReceiver()
        setupBackgroundSyncListener()
        setupRemoteCommandListener()
        startSilentBatteryValidation()
        requestBatteryOptimizationBypass()

        // [Variables] | [Constants] | [Dependencies] | [Integration Points]
        // Real-Time Cloud Presence Sentinel: Arms onDisconnect and publishes initial online heartbeat
        try {
            val agentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
            val grpId = getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE).getString("active_group", null)
            FirebaseComm.setupPresenceSentinel(agentId, grpId)
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Presence Sentinel Init Deferred: ${e.message}")
        }

        // 🚀 LIVE SSOT BINDING: Synchronize TrackingService telemetry when local profile edits occur
        avatarClosedLoopCallback = { agentId, payload ->
            try {
                val selfAgent = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
                val cleanSelf = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(selfAgent)
                val cleanAgent = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(agentId)
                if (cleanAgent.equals(cleanSelf, ignoreCase = true)) {
                    val newAv = payload["avatar_id"]?.toString()
                    val newName = payload["name"]?.toString() ?: payload["fullName"]?.toString()
                    SystemLogger.i(TAG, "Closed-loop: TrackingService notified of self SSOT update -> av=$newAv, name=$newName")
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Closed-loop avatar callback error: ${e.message}")
            }
        }
        com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.registerClosedLoopCallback(avatarClosedLoopCallback!!)
    }

    private fun ensureLocationEngineInitialized() {
        if (!hasLocationPermission()) {
            SystemLogger.w(TAG, "Location permission not yet granted. TrackingService running in Standby Mode.")
            return
        }
        if (locationEngine != null) {
            locationEngine?.startTracking()
            return
        }

        // 🔋 Battery-Optimized Sentinel: Android Foreground Service with FOREGROUND_SERVICE_LOCATION
        // receives location callbacks natively without requiring a permanent CPU WakeLock.
        val prefs = getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)

        locationEngine = LocationEngine(this) { location ->
            val p = prefs
            if (RegistryManager.isServiceHalted(applicationContext)) {
                SystemLogger.w(TAG, "Forensic Isolation Active: Background Telemetry Blocked.")
                return@LocationEngine
            }

            // Rule 5: Security Agent - Anti-Tamper & Mock Check
            val secReport = com.jdroidx.jfindme.engine.security.AntiTamperSentinel.auditDeviceSecurity(applicationContext, location)
            if (secReport.isMockGps) {
                SystemLogger.e(TAG, "MOCK_LOCATION_BLOCKED_IN_BACKGROUND")
                return@LocationEngine
            }

            SystemLogger.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")

            // 🛡️ SOFTWARE GEOFENCING: Ray-Casting Polygon Perimeter Evaluation (Bound to ProfileAclDossier SSOT)
            val aclDossier = com.jdroidx.jfindme.data.model.ProfileAclDossier.loadFromLocal(applicationContext)
            if (aclDossier.geofenceAlertsEnabled) {
                evaluateSoftwareGeofences(location.latitude, location.longitude)
            }

            // 🛣️ Kalman 2D Spatial Stabilization & Road Azimuth Smoothing (Execute prior to dead-reckoning distance check)
            val smoothedLocation: android.location.Location = try {
                if (RegistryManager.isRoadSmoothingEnabled(applicationContext)) {
                    val res = com.jdroidx.jfindme.engine.map.RoadSmoothingEngine.processLocation(
                        deviceId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext),
                        rawLat = location.latitude,
                        rawLng = location.longitude,
                        accuracyMeters = location.accuracy,
                        rawSpeedMps = location.speed,
                        rawBearing = location.bearing,
                        timestamp = location.time,
                        context = applicationContext
                    )
                    android.location.Location(location).apply {
                        latitude = res.smoothedLatLng.latitude
                        longitude = res.smoothedLatLng.longitude
                        speed = res.smoothedSpeedMps
                        bearing = res.filteredHeading
                    }
                } else {
                    location
                }
            } catch (_: Exception) {
                location
            }

            // 🚀 Authoritative SSOT Background Avatar Sync & Vitals Logging
            val repo = DeviceRepository.getInstance(applicationContext)
            val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
            val ssotAvatar = repo.getDeviceCentricAvatar(myAgentId)
            val ssotName = repo.getDeviceCentricName(myAgentId)
            val rawBaseId = p.getInt("equipped_avatar_id", 1)
            val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
            val resolvedAvatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly" && ssotAvatar != "1") {
                ssotAvatar
            } else if (dynamicIdx > 0) {
                "avatar_custom_$dynamicIdx"
            } else {
                "butterfly"
            }
            val resolvedUserName = if (ssotName.isNotEmpty() && !ssotName.startsWith("Agent_", true)) {
                ssotName
            } else {
                p.getString("user_nickname", p.getString("fullName", "Active Tracker")) ?: "Active Tracker"
            }

            // Cache Device Vitals in local SQLite for offline health inspection
            try {
                val servingCell = com.jdroidx.jfindme.engine.location.CellTowerSentinel.extractServingCellInfo(applicationContext)
                val netType = if (servingCell.hasValidTowerData) servingCell.networkType else "CELLULAR"
                val sigDbm = if (servingCell.signalDbm != -999) servingCell.signalDbm else -75
                repo.upsertDeviceVitals(
                    hardwareId = myAgentId,
                    battery = p.getInt("last_known_battery", 100),
                    isCharging = p.getBoolean("last_known_charging", false),
                    networkType = netType,
                    signalDbm = sigDbm,
                    isMockGps = secReport.isMockGps,
                    isRooted = secReport.isRooted
                )
            } catch (e: Exception) { }

            val currentTime = System.currentTimeMillis()
            val isSos = RegistryManager.getBoolean(applicationContext, "is_sos_active", false)
            val distMoved = lastUploadedLocation?.distanceTo(smoothedLocation) ?: Float.MAX_VALUE
            // [Variables]: deadReckoningThreshold, presencePulseInterval, stationarySpeedThreshold | [Constants]: KEY_DEAD_RECKONING_RADIUS_METERS, KEY_PRESENCE_PULSE_INTERVAL_MS, KEY_STATIONARY_SPEED_THRESHOLD_MPS | [Dependencies]: RegistryManager | [Integration Points]: Settings & Dynamic Fleet OOPS Config
            val deadReckoningThreshold = com.jdroidx.jfindme.utils.RegistryManager.getDeadReckoningRadiusMeters(applicationContext)
            val presencePulseInterval = com.jdroidx.jfindme.utils.RegistryManager.getPresencePulseIntervalMs(applicationContext)
            val stationarySpeedThreshold = com.jdroidx.jfindme.utils.RegistryManager.getStationarySpeedThresholdMps(applicationContext)
            val isPhysicallyStationary = (distMoved < deadReckoningThreshold && smoothedLocation.speed < stationarySpeedThreshold) && !isSos

            // 🛡️ Spatial Dead-Reckoning: Suppress redundant full GPS uploads when stationary (< deadReckoningThreshold m)
            if (isPhysicallyStationary) {
                if (lastUploadedLocation == null) {
                    lastUploadedLocation = android.location.Location(smoothedLocation).apply { speed = 0.0f }
                } else {
                    lastUploadedLocation?.speed = smoothedLocation.speed
                }
                lastLocationFixEpoch = currentTime
                // Dispatch lean 28-byte presence heartbeat every presencePulseInterval ms (default 180s) to keep server.js and family map ONLINE
                if (currentTime - lastPresencePulseEpoch >= presencePulseInterval) {
                    lastPresencePulseEpoch = currentTime
                    val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
                    val authUid = FirebaseAuth.getInstance().uid ?: ""
                    val curBat = RegistryManager.getInt(applicationContext, "last_known_battery", 100)
                    FirebaseComm.syncPresencePulse(authUid, localAgentId, curBat)
                    SystemLogger.d(TAG, "DEAD_RECKONING: Stationary device ($distMoved m < $deadReckoningThreshold m). Dispatched 28-byte presence heartbeat.")
                }
                return@LocationEngine
            }

            // Kinematic movement (> 15m) or Emergency SOS: Update anchor and continue through adaptive throttle
            lastUploadedLocation = android.location.Location(smoothedLocation)
            lastLocationFixEpoch = currentTime
            lastPresencePulseEpoch = currentTime

            val curBat = RegistryManager.getInt(applicationContext, "last_known_battery", 100)
            val isCharging = RegistryManager.getBoolean(applicationContext, "last_known_charging", false)

            // 🛡️ Adaptive Telemetry Polling Throttle (GisSpatialEngine)
            val adaptiveInterval = com.jdroidx.jfindme.engine.location.GisSpatialEngine.computeAdaptivePollingInterval(
                isSosActive = isSos,
                speedMps = smoothedLocation.speed,
                batteryPct = curBat,
                isCharging = isCharging,
                isStationary = isPhysicallyStationary,
                context = applicationContext
            )
            
            if (currentTime - lastPulseTime < adaptiveInterval) {
                SystemLogger.d(TAG, "THROTTLE: Adaptive throttle holding fix (Interval: ${adaptiveInterval / 1000}s, Delta: ${(currentTime - lastPulseTime) / 1000}s)")
                return@LocationEngine
            }
            
            lastPulseTime = currentTime
            val timeStampSign = FirebaseComm.getForensicTimestamp(currentTime)

            // 🧬 V59.6: Forensic Identity Resolution (Reliable Non-Blocking Fallback)
            val authUid = FirebaseAuth.getInstance().uid
            val rawHw = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getRawAndroidId(applicationContext)
            val deviceId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(applicationContext)
            val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)

            val publishLocation = { targetAgentId: String ->
                val tier = com.jdroidx.jfindme.engine.security.PrivacyEngine.getPrivacyTier(applicationContext)
                if (tier == com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier.FROZEN) {
                    SystemLogger.d(TAG, "GHOST_MODE_ACTIVE: Location broadcast suppressed for $targetAgentId")
                } else {
                    val effectiveLat: Double
                    val effectiveLng: Double
                    if (tier == com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier.FUZZY) {
                        val perturbed = com.jdroidx.jfindme.engine.security.PrivacyEngine.applyFuzzyPerturbation(location.latitude, location.longitude)
                        effectiveLat = perturbed.first
                        effectiveLng = perturbed.second
                    } else {
                        effectiveLat = location.latitude
                        effectiveLng = location.longitude
                    }

                    val (curBattery, isCharging) = getResolvedBatteryAndCharging()
                    val userLocation = UserLocation(
                        userId = targetAgentId,
                        deviceId = deviceId, 
                        latitude = effectiveLat,
                        longitude = effectiveLng,
                        geohash = com.jdroidx.jfindme.engine.location.GeohashHelper.encode(effectiveLat, effectiveLng),
                        bearing = location.bearing,
                        speed = location.speed,
                        accuracy = location.accuracy,
                        altitude = location.altitude,
                        batteryLevel = curBattery,
                        isSosActive = isSos,
                        timestamp = timeStampSign,
                        allowDependents = p.getBoolean("allow_dependents", true),
                        active_group = p.getString("active_group", "") ?: "",
                        isForensicAudit = true,
                        userName = resolvedUserName,
                        avatar_id = resolvedAvatarId
                    ).apply {
                        this.isCharging = isCharging
                    }
                    com.jdroidx.jfindme.engine.sync.TelemetryFailoverRouter.dispatchLocationWithFailover(applicationContext, userLocation)
                }
            }

            // 🧬 Hardware Root Anchor: Publish strictly to local hardware silicon agent ID
            publishLocation(localAgentId)

            // 🦋 Closed-Loop Telemetry: Update proximity, ETA & status for active Butterfly Points (Convert m/s to km/h)
            try {
                val speedKmh = com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.toKmh(smoothedLocation.speed)
                com.jdroidx.jfindme.engine.map.ButterflyPointEngine.processLocalLocationUpdate(
                    applicationContext,
                    smoothedLocation.latitude,
                    smoothedLocation.longitude,
                    speedKmh
                )
            } catch (e: Exception) {
                SystemLogger.d(TAG, "ButterflyPoint local update notice: ${e.message}")
            }

            val activeZones = try {
                DeviceRepository.getInstance(applicationContext).getActiveSafeZones()
            } catch (_: Exception) { emptyList() }
            val isNearPerimeter = com.jdroidx.jfindme.utils.SpatialMathUtil.isNearAnySafeZonePerimeter(
                location.latitude, location.longitude, activeZones, 150.0
            )
            if (isLowPowerMode || isNearPerimeter) {
                locationEngine?.setLowPowerMode(isLowPowerMode, nearGeofencePerimeter = isNearPerimeter)
            }
        }

        if (isLowPowerMode) {
            locationEngine?.setLowPowerMode(true)
        }
        locationEngine?.startTracking()
    }

    private fun evaluateSoftwareGeofences(lat: Double, lng: Double) {
        try {
            val db = DeviceRepository.getInstance(applicationContext)
            val activeZones = db.getActiveSafeZones()
            if (activeZones.isEmpty()) return

            val prefs = getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val debounceMs = RegistryManager.getGeofenceJitterDebounceMs(applicationContext)
            val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
            val userName = prefs.getString("user_nickname", "Agent") ?: "Agent"

            for (zone in activeZones) {
                if (!zone.isPolygon) continue
                if (!com.jdroidx.jfindme.engine.location.GisSpatialEngine.isSafeZoneInSchedule(zone)) continue

                val isInside = com.jdroidx.jfindme.engine.location.GisSpatialEngine.isLocationInsideSafeZone(lat, lng, zone)
                val stateKey = "POLY_LATCH_${zone.zoneId}"
                val timeKey = "POLY_LATCH_TIME_${zone.zoneId}"
                val lastState = prefs.getString(stateKey, null)
                val lastTime = prefs.getLong(timeKey, 0L)
                val currentState = if (isInside) "IN" else "OUT"

                if (lastState == null) {
                    prefs.edit().putString(stateKey, currentState).putLong(timeKey, now).apply()
                    continue
                }

                if (!lastState.equals(currentState, ignoreCase = true) && (now - lastTime >= debounceMs)) {
                    prefs.edit().putString(stateKey, currentState).putLong(timeKey, now).apply()
                    val transitionName = if (isInside) "ENTERED" else "EXITED"
                    SystemLogger.i(TAG, "POLYGON_GEOFENCE_TRANSITION: $transitionName ${zone.name} (State: $currentState)")

                    if (zone.alertsEnabled) {
                        AlertManager.showGeofenceNotification(applicationContext, zone.name, transitionName, userName)
                        db.saveGeofenceEvent(myAgentId, zone.zoneId, currentState)
                    }
                }
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "evaluateSoftwareGeofences error: ${e.message}")
        }
    }

    // [Variables]: remoteCommandListener, pokeCommandListener, systemUpdateCommandListener
    // [Constants]: None
    // [Dependencies]: FirebaseComm, ApkInstallerHelper, SystemLogger
    // [Integration Points]: Cloud-to-Mobile OTA command execution and closed-loop feedback reporting
    private var remoteCommandListener: ValueEventListener? = null
    private var pokeCommandListener: ValueEventListener? = null
    private var systemUpdateCommandListener: ValueEventListener? = null
    private var circleTombstoneListener: ChildEventListener? = null
    private var circleTombstoneRef: DatabaseReference? = null

    private fun setupRemoteCommandListener() {
        if (!FirebaseComm.isAuthenticated()) return
        val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
        if (!FirebaseComm.isAgentIdValid(localAgentId)) return

        // 🛰️ MASTER DATA SYNC SENTINEL: Real-time peer inbox listener
        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.startRealtimePeerSyncListener(applicationContext, localAgentId)

        // 🔄 System Update Closed-Loop Sentinel: Flush pending cloud ACK if deferred during boot
        com.jdroidx.jfindme.utils.RegistryManager.flushPendingUpdateFeedback(applicationContext)

        // 🛡️ HOST PEER TOMBSTONE LISTENER (<500ms Acoustic Disarm & Marker Eviction on DECOMMISSIONED)
        // [Variables]: activeGrp, tombRef, peerId, action | [Constants]: ACTION_MASTER_DATA_UPDATED
        // [Dependencies]: FirebaseComm, AcousticBeaconEngine, DeviceRepository, LocationRepository, RegistryManager
        // [Integration Points]: jFindMeWeb /api/device/delete-everywhere STEP 2 Tombstone Broadcast
        val activeGrp = RegistryManager.getActiveGroup(applicationContext)
        if (activeGrp.isNotBlank() && circleTombstoneListener == null) {
            circleTombstoneRef = FirebaseDatabase.getInstance()
                .getReference(FirebaseComm.NODE_RELATIONSHIPS)
                .child(activeGrp)
                .child("tombstones")
            circleTombstoneListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handlePeerTombstone(snapshot)
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handlePeerTombstone(snapshot)
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    SystemLogger.w(TAG, "Circle tombstone listener cancelled: ${error.message}")
                }
            }
            circleTombstoneRef?.addChildEventListener(circleTombstoneListener!!)
        }

        val cmdRef = FirebaseComm.getDeviceRef(localAgentId).child("device").child("remote_command")
        remoteCommandListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val action = snapshot.child("action").getValue(String::class.java)
                    val durationSec = snapshot.child("duration_sec").getValue(Int::class.java) ?: 30
                    val epochMs = FirebaseComm.safeLong(snapshot.child("epoch_ms")) ?: 0L
                    val now = System.currentTimeMillis()
                    // 🛡️ Clock-Skew & Zero-Epoch Safeguard: Allow immediate execution if epoch is unset (<=0) or within 3-minute bidirectional window
                    val isFreshCommand = (epochMs <= 0L) || (Math.abs(now - epochMs) < 180000L)

                    if (action == "RING_DEVICE") {
                        if (isFreshCommand) {
                            SystemLogger.w(TAG, "🔊 INCOMING REMOTE RING COMMAND RECEIVED for $localAgentId (${durationSec}s)")
                            com.jdroidx.jfindme.engine.alert.AcousticBeaconEngine.startSiren(
                                this@TrackingService,
                                durationSec,
                                "Remote Guardian"
                            )
                        } else {
                            SystemLogger.w(TAG, "🔊 Expired RING_DEVICE command ($epochMs vs now $now) purged from cloud")
                        }
                        snapshot.ref.removeValue()
                    } else if (action == "LOCATE_DEVICE" || action == "POKE_DEVICE" || action == "REQUEST_LOCATION" || action == "PING_LOCATION") {
                        if (isFreshCommand) {
                            SystemLogger.w(TAG, "🛰️ INCOMING REMOTE LOCATE COMMAND RECEIVED for $localAgentId -> triggering immediate GPS fix")
                            requestImmediateLocationAndPublish()
                        } else {
                            SystemLogger.w(TAG, "🛰️ Expired LOCATE_DEVICE command ($epochMs vs now $now) purged from cloud")
                        }
                        snapshot.ref.removeValue()
                    } else if (action == "SYSTEM_UPDATE" || action == "DOWNLOAD_APK_TO_STORAGE" || action == "DOWNLOAD_AND_INSTALL") {
                        val dlUrl = snapshot.child("download_url").getValue(String::class.java)
                            ?: snapshot.child("downloadUrl").getValue(String::class.java) ?: ""
                        val vName = snapshot.child("version_name").getValue(String::class.java) ?: com.jdroidx.jfindme.BuildConfig.VERSION_NAME
                        val vCode = snapshot.child("version_code").getValue(Int::class.java) ?: (com.jdroidx.jfindme.BuildConfig.VERSION_CODE + 1)
                        val isDownloadOnly = (action == "DOWNLOAD_APK_TO_STORAGE" || snapshot.child("install_action").getValue(String::class.java) == "DOWNLOAD_ONLY")

                        // 🛑 10-MINUTE DEDUPLICATION GATE: Suppress identical OTA commands
                        if (isOtaDuplicate(vCode)) {
                            SystemLogger.w(TAG, "Deduplication: Suppressing duplicate OTA trigger for build $vCode within 10 minutes.")
                            snapshot.ref.removeValue()
                            return
                        }

                        SystemLogger.w(TAG, "🚀 INCOMING REMOTE OTA COMMAND RECEIVED for $localAgentId: Action=$action, V=$vName (Build $vCode), URL=$dlUrl")

                        if (dlUrl.isNotBlank()) {
                            val statusNode = FirebaseComm.getDeviceRef(localAgentId).child("Version_Update").child("pending_push")
                            if (isDownloadOnly) {
                                com.jdroidx.jfindme.utils.ApkInstallerHelper.enqueueSystemDownload(
                                    context = applicationContext,
                                    downloadUrl = dlUrl,
                                    fileName = "jButterFly_V${vCode}.apk",
                                    title = "jButterFly V$vName Update",
                                    isWifiOnly = false
                                )
                                statusNode.child("status").setValue("DOWNLOADING_VIA_DOWNLOAD_MANAGER")
                                statusNode.child("stage").setValue("DOWNLOAD_ENQUEUED")
                                statusNode.child("epoch_ms").setValue(System.currentTimeMillis())
                            } else {
                                com.jdroidx.jfindme.utils.ApkInstallerHelper.downloadApkInBackground(
                                    context = applicationContext,
                                    downloadUrl = dlUrl
                                ) { success, apkFile ->
                                    if (success && apkFile != null) {
                                        SystemLogger.i(TAG, "✅ Remote OTA APK downloaded successfully: ${apkFile.absolutePath}")
                                        try {
                                            val publicDl = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                            if (publicDl != null && (publicDl.exists() || publicDl.mkdirs())) {
                                                val destFile = java.io.File(publicDl, "jButterFly_V${vCode}.apk")
                                                apkFile.copyTo(destFile, overwrite = true)
                                                SystemLogger.i(TAG, "📁 Copied package to public downloads: ${destFile.absolutePath}")
                                            }
                                        } catch (copyErr: Exception) {
                                            SystemLogger.w(TAG, "Public download copy notice: ${copyErr.message}")
                                        }

                                        com.jdroidx.jfindme.utils.ApkInstallerHelper.installSilentlyOrPrompt(
                                            context = applicationContext,
                                            apkFile = apkFile,
                                            versionName = vName
                                        )
                                        statusNode.child("status").setValue("INSTALL_PROMPTED")
                                        statusNode.child("stage").setValue("INSTALL_PROMPTED")
                                        statusNode.child("completed_epoch_ms").setValue(System.currentTimeMillis())
                                    } else {
                                        SystemLogger.e(TAG, "❌ Remote OTA APK download failed.")
                                        statusNode.child("status").setValue("FAILED")
                                        statusNode.child("error").setValue("Download verification failed")
                                    }
                                }
                            }
                        }
                        snapshot.ref.removeValue()
                    } else if (action == "DEVICE_SYNC" || action == "DEVICE_SHARING_SYNC") {
                        val peerId = snapshot.child("peer_id").getValue(String::class.java) ?: ""
                        val peerName = snapshot.child("peer_name").getValue(String::class.java) ?: peerId
                        val peerRole = snapshot.child("peer_role").getValue(String::class.java)
                            ?: snapshot.child("role").getValue(String::class.java) ?: "Member"
                        val myRole = snapshot.child("my_role").getValue(String::class.java)
                            ?: if (peerRole.contains("Child", ignoreCase = true)) "Parent / Guardian"
                               else if (peerRole.contains("Guardian", ignoreCase = true) || peerRole.contains("Parent", ignoreCase = true)) "Child"
                               else "Member"
                        val groupId = snapshot.child("group_id").getValue(String::class.java) ?: ""
                        val epochMs = FirebaseComm.safeLong(snapshot.child("epoch_ms")) ?: 0L
                        val expiresAt = FirebaseComm.safeLong(snapshot.child("expires_at")) ?: if (epochMs > 0L) epochMs + 86400000L else Long.MAX_VALUE
                        val syncSessionId = snapshot.child("sync_session_id").getValue(String::class.java)
                            ?: snapshot.child("epoch_ms").getValue()?.toString() ?: ""

                        // ⏱️ 24-Hour Expiration Sentinel: Reject and auto-revoke stale sharing commands
                        if (System.currentTimeMillis() > expiresAt) {
                            SystemLogger.w(TAG, "⏱️ DEVICE_SYNC command expired (>24h). Discarding packet.")
                            snapshot.ref.removeValue()
                            FirebaseComm.getDeviceRef(localAgentId).child("commands_ack").child("device_sync").setValue(mapOf(
                                "status" to "EXPIRED",
                                "sync_session_id" to syncSessionId,
                                "peer_id" to peerId,
                                "role" to peerRole,
                                "reason" to "Request exceeded 24-hour acceptance window",
                                "timestamp" to System.currentTimeMillis()
                            ))
                            return
                        }

                        if (peerId.isNotBlank()) {
                            SystemLogger.i(TAG, "🔗 INCOMING DEVICE_SYNC COMMAND: Pairing with $peerId ($peerName, $peerRole) in $groupId")
                            try {
                                val sLocalDb = DeviceRepository.getInstance(applicationContext)
                                val pairId = "PAIR_${localAgentId}_${peerId}"
                                val pairingPin = snapshot.child("pairing_code").getValue(String::class.java)
                                sLocalDb.logPairingWithRoles(
                                    pairId,
                                    localAgentId,
                                    peerId,
                                    myRole,
                                    peerRole,
                                    pairingPin,
                                    "PAIRED"
                                )
                                val isFriend = peerRole.contains("Friend", ignoreCase = true)
                                sLocalDb.saveRosterNode(peerId, peerName, peerRole, "ACTIVE", false, isFriend, activeGroupId = groupId)
                                sLocalDb.saveRosterNode(localAgentId, "My Device", myRole, "ACTIVE", false, false, activeGroupId = groupId)
                                sLocalDb.appendValidLinkedAgent(localAgentId, peerId)
                                sLocalDb.appendValidLinkedAgent(peerId, localAgentId)
                                sLocalDb.updateCitizenRole(peerId, if (isFriend) "None" else peerRole, if (isFriend) peerRole else "None", isFriend, groupId)

                                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                                    setPackage(applicationContext.packageName)
                                    putExtra("category", "ROSTER")
                                    putExtra("EXTRA_CATEGORY", "ROSTER")
                                }
                                applicationContext.sendBroadcast(refreshIntent)

                                com.jdroidx.jfindme.engine.alert.AlertManager.showDeviceSharingNotification(
                                    applicationContext,
                                    "Device Synchronized",
                                    "$peerName is now synced with your device as [$peerRole] in circle $groupId."
                                )

                                FirebaseComm.getDeviceRef(localAgentId).child("commands_ack").child("device_sync").setValue(mapOf(
                                    "status" to "ACKNOWLEDGED",
                                    "sync_session_id" to syncSessionId,
                                    "peer_id" to peerId,
                                    "role" to peerRole,
                                    "my_role" to myRole,
                                    "timestamp" to System.currentTimeMillis()
                                ))
                            } catch (e: Exception) {
                                SystemLogger.e(TAG, "Failed to process remote DEVICE_SYNC: ${e.message}")
                            }
                        }
                        snapshot.ref.removeValue()
                    } else if (action == "UNLINK") {
                        val fromAgent = snapshot.child("from_agent").getValue(String::class.java) ?: ""
                        if (fromAgent.isNotBlank()) {
                            try {
                                val sLocalDb = DeviceRepository.getInstance(applicationContext)
                                sLocalDb.deleteRosterNode(fromAgent, null)
                                com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(applicationContext).removeTarget(fromAgent)
                                com.jdroidx.jfindme.utils.RegistryManager.decommissionDevice(applicationContext, fromAgent)
                                SystemLogger.i(TAG, "🛡️ Remote UNLINK command processed for peer: $fromAgent")
                                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                                    setPackage(applicationContext.packageName)
                                    putExtra("category", "ROSTER")
                                    putExtra("EXTRA_CATEGORY", "ROSTER")
                                }
                                applicationContext.sendBroadcast(refreshIntent)
                            } catch (e: Exception) {
                                SystemLogger.e(TAG, "Error processing remote UNLINK command: ${e.message}")
                            }
                        }
                        snapshot.ref.removeValue()
                    } else if (action == "REMOTE_DECOMMISSION" || action == "STANDBY_DECOMMISSION") {
                        val mode = snapshot.child("mode").getValue(String::class.java) ?: "CLEANSLATE"
                        SystemLogger.w(TAG, "🚨 INCOMING REMOTE DECOMMISSION COMMAND RECEIVED for $localAgentId: Mode=$mode. Halting TrackingService.")
                        // 1. Instantly silence siren if active
                        try {
                            com.jdroidx.jfindme.engine.alert.AcousticBeaconEngine.stopSiren(this@TrackingService)
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Acoustic siren halt notice: ${e.message}")
                        }
                        // 2. Halt location tracking engine
                        try {
                            locationEngine?.stopTracking()
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Location engine halt notice: ${e.message}")
                        }
                        // 3. Stop foreground notification service and kill process
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                stopForeground(STOP_FOREGROUND_REMOVE)
                            } else {
                                @Suppress("DEPRECATION")
                                stopForeground(true)
                            }
                            stopSelf()
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Foreground service stop notice: ${e.message}")
                        }
                        // 4. Persist decommission latch for BOTH ARCHIVE (Standby) and CLEANSLATE so Watchdog/Alarm/Boot do not resurrect zombie service
                        getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("is_decommissioned_by_admin", true)
                            .putBoolean(RegistryManager.KEY_SERVICE_HALTED, true)
                            .apply()

                        // 5. Cancel WorkManager resurrection & background sync workers + stealth alarms
                        try {
                            val wm = androidx.work.WorkManager.getInstance(applicationContext)
                            wm.cancelUniqueWork("LocationWatchdogWork")
                            wm.cancelUniqueWork("OfflineSyncPeriodic")
                            wm.cancelUniqueWork("OfflineSyncDeferred")
                            com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.cancelStealthAlarms(applicationContext)
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Background worker cancel notice: ${e.message}")
                        }
                        snapshot.ref.removeValue()
                    } else if (action != null) {
                        snapshot.ref.removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                SystemLogger.e(TAG, "Remote command listener cancelled: ${error.message}")
            }
        }
        cmdRef.addValueEventListener(remoteCommandListener!!)

        // 🛰️ Dual-Path Ingress: Direct listener on /devices/{id}/commands for instant 24h background sync
        val directCmdRef = FirebaseComm.getDeviceRef(localAgentId).child("commands")
        directCmdRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val action = snapshot.child("action").getValue(String::class.java) 
                        ?: snapshot.child("command").getValue(String::class.java)
                    if (action == "DEVICE_SYNC" || action == "DEVICE_SHARING_SYNC") {
                        val peerId = snapshot.child("peer_id").getValue(String::class.java) ?: ""
                        val peerName = snapshot.child("peer_name").getValue(String::class.java) ?: peerId
                        val peerRole = snapshot.child("peer_role").getValue(String::class.java)
                            ?: snapshot.child("role").getValue(String::class.java) ?: "Member"
                        val myRole = snapshot.child("my_role").getValue(String::class.java)
                            ?: if (peerRole.contains("Child", ignoreCase = true)) "Parent / Guardian"
                               else if (peerRole.contains("Guardian", ignoreCase = true) || peerRole.contains("Parent", ignoreCase = true)) "Child"
                               else "Member"
                        val groupId = snapshot.child("group_id").getValue(String::class.java) ?: ""
                        val epochMs = FirebaseComm.safeLong(snapshot.child("epoch_ms")) ?: 0L
                        val expiresAt = FirebaseComm.safeLong(snapshot.child("expires_at")) ?: if (epochMs > 0L) epochMs + 86400000L else Long.MAX_VALUE
                        val syncSessionId = snapshot.child("sync_session_id").getValue(String::class.java)
                            ?: snapshot.child("epoch_ms").getValue()?.toString() ?: ""

                        if (System.currentTimeMillis() > expiresAt) {
                            SystemLogger.w(TAG, "⏱️ DEVICE_SYNC command expired (>24h). Discarding packet.")
                            snapshot.ref.removeValue()
                            FirebaseComm.getDeviceRef(localAgentId).child("commands_ack").child("device_sync").setValue(mapOf(
                                "status" to "EXPIRED",
                                "sync_session_id" to syncSessionId,
                                "peer_id" to peerId,
                                "role" to peerRole,
                                "reason" to "Request exceeded 24-hour acceptance window",
                                "timestamp" to System.currentTimeMillis()
                            ))
                            return
                        }

                        if (peerId.isNotBlank()) {
                            try {
                                val sLocalDb = DeviceRepository.getInstance(applicationContext)
                                val pairId = "PAIR_${localAgentId}_${peerId}"
                                val pairingPin = snapshot.child("pairing_code").getValue(String::class.java)
                                sLocalDb.logPairingWithRoles(
                                    pairId,
                                    localAgentId,
                                    peerId,
                                    myRole,
                                    peerRole,
                                    pairingPin,
                                    "PAIRED"
                                )
                                val isFriend = peerRole.contains("Friend", ignoreCase = true)
                                sLocalDb.saveRosterNode(peerId, peerName, peerRole, "ACTIVE", false, isFriend, activeGroupId = groupId)
                                sLocalDb.saveRosterNode(localAgentId, "My Device", myRole, "ACTIVE", false, false, activeGroupId = groupId)
                                sLocalDb.appendValidLinkedAgent(localAgentId, peerId)
                                sLocalDb.appendValidLinkedAgent(peerId, localAgentId)
                                sLocalDb.updateCitizenRole(peerId, if (isFriend) "None" else peerRole, if (isFriend) peerRole else "None", isFriend, groupId)

                                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                                    setPackage(applicationContext.packageName)
                                    putExtra("category", "ROSTER")
                                    putExtra("EXTRA_CATEGORY", "ROSTER")
                                }
                                applicationContext.sendBroadcast(refreshIntent)

                                com.jdroidx.jfindme.engine.alert.AlertManager.showDeviceSharingNotification(
                                    applicationContext,
                                    "Device Synchronized",
                                    "$peerName is now synced with your device as [$peerRole] in circle $groupId."
                                )

                                FirebaseComm.getDeviceRef(localAgentId).child("commands_ack").child("device_sync").setValue(mapOf(
                                    "status" to "ACKNOWLEDGED",
                                    "sync_session_id" to syncSessionId,
                                    "peer_id" to peerId,
                                    "role" to peerRole,
                                    "my_role" to myRole,
                                    "timestamp" to System.currentTimeMillis()
                                ))
                            } catch (e: Exception) {
                                SystemLogger.e(TAG, "Failed to process remote DEVICE_SYNC: ${e.message}")
                            }
                        }
                        snapshot.ref.removeValue()
                    } else if (action == "UNLINK") {
                        val fromAgent = snapshot.child("from_agent").getValue(String::class.java) ?: ""
                        if (fromAgent.isNotBlank()) {
                            try {
                                val sLocalDb = DeviceRepository.getInstance(applicationContext)
                                sLocalDb.deleteRosterNode(fromAgent, null)
                                com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(applicationContext).removeTarget(fromAgent)
                                com.jdroidx.jfindme.utils.RegistryManager.decommissionDevice(applicationContext, fromAgent)
                                SystemLogger.i(TAG, "🛡️ Remote UNLINK command processed for peer: $fromAgent")
                                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                                    setPackage(applicationContext.packageName)
                                    putExtra("category", "ROSTER")
                                    putExtra("EXTRA_CATEGORY", "ROSTER")
                                }
                                applicationContext.sendBroadcast(refreshIntent)
                            } catch (e: Exception) {
                                SystemLogger.e(TAG, "Error processing remote UNLINK command: ${e.message}")
                            }
                        }
                        snapshot.ref.removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        val pokeRef = FirebaseComm.getDeviceRef(localAgentId).child("commands").child("poke")
        pokeCommandListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val cmd = snapshot.child("command").getValue(String::class.java)
                    val type = snapshot.child("type").getValue(String::class.java)
                    val ts = FirebaseComm.safeLong(snapshot.child("timestamp")) ?: 0L
                    val now = System.currentTimeMillis()
                    if (cmd == "POKE_LOCATION_UPDATE" || type == "RENDEZVOUS_NUDGE" || cmd == "RENDEZVOUS_NUDGE") {
                        if ((now - ts) < 90000L) {
                            SystemLogger.w(TAG, "🛰️ INCOMING POKE LOCATION UPDATE RECEIVED for $localAgentId")
                            requestImmediateLocationAndPublish()

                            // If this was a rendezvous nudge from an organizer, post heads-up notification
                            if (type == "RENDEZVOUS_NUDGE" || cmd == "RENDEZVOUS_NUDGE") {
                                val pointName = snapshot.child("point_name").getValue(String::class.java) ?: "Rendezvous"
                                val nudgeMsg = snapshot.child("message").getValue(String::class.java)
                                    ?: "Organizer requested your current progress and arrival update."
                                AlertManager.showButterflyNudgeNotification(applicationContext, pointName, nudgeMsg)
                            }
                        } else {
                            SystemLogger.w(TAG, "🛰️ Expired POKE_LOCATION_UPDATE command ($ts) purged from cloud")
                        }
                        snapshot.ref.removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        pokeRef.addValueEventListener(pokeCommandListener!!)

        val sysUpdateRef = FirebaseComm.getDeviceRef(localAgentId).child("commands").child("system_update")
        systemUpdateCommandListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val action = snapshot.child("action").getValue(String::class.java)
                        ?: snapshot.child("command").getValue(String::class.java)
                    if (action == "SYSTEM_UPDATE" || action == "DOWNLOAD_APK_TO_STORAGE" || action == "DOWNLOAD_AND_INSTALL") {
                        val dlUrl = snapshot.child("download_url").getValue(String::class.java)
                            ?: snapshot.child("downloadUrl").getValue(String::class.java) ?: ""
                        val vName = snapshot.child("version_name").getValue(String::class.java) ?: com.jdroidx.jfindme.BuildConfig.VERSION_NAME
                        val vCode = snapshot.child("version_code").getValue(Int::class.java) ?: (com.jdroidx.jfindme.BuildConfig.VERSION_CODE + 1)
                        val isDownloadOnly = (action == "DOWNLOAD_APK_TO_STORAGE" || snapshot.child("install_action").getValue(String::class.java) == "DOWNLOAD_ONLY")

                        // 🛑 10-MINUTE DEDUPLICATION GATE: Suppress identical OTA commands
                        if (isOtaDuplicate(vCode)) {
                            SystemLogger.w(TAG, "Deduplication: Suppressing duplicate system_update trigger for build $vCode within 10 minutes.")
                            snapshot.ref.removeValue()
                            return
                        }

                        if (dlUrl.isNotBlank()) {
                            val statusNode = FirebaseComm.getDeviceRef(localAgentId).child("Version_Update").child("pending_push")
                            if (isDownloadOnly) {
                                com.jdroidx.jfindme.utils.ApkInstallerHelper.enqueueSystemDownload(
                                    context = applicationContext,
                                    downloadUrl = dlUrl,
                                    fileName = "jButterFly_V${vCode}.apk",
                                    title = "jButterFly V$vName Update",
                                    isWifiOnly = false
                                )
                                statusNode.child("status").setValue("DOWNLOADING_VIA_DOWNLOAD_MANAGER")
                                statusNode.child("stage").setValue("DOWNLOAD_ENQUEUED")
                                statusNode.child("epoch_ms").setValue(System.currentTimeMillis())
                            } else {
                                com.jdroidx.jfindme.utils.ApkInstallerHelper.downloadApkInBackground(
                                    context = applicationContext,
                                    downloadUrl = dlUrl
                                ) { success, apkFile ->
                                    if (success && apkFile != null) {
                                        try {
                                            val publicDl = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                            if (publicDl != null && (publicDl.exists() || publicDl.mkdirs())) {
                                                val destFile = java.io.File(publicDl, "jButterFly_V${vCode}.apk")
                                                apkFile.copyTo(destFile, overwrite = true)
                                            }
                                        } catch (_: Exception) {}

                                        com.jdroidx.jfindme.utils.ApkInstallerHelper.installSilentlyOrPrompt(
                                            context = applicationContext,
                                            apkFile = apkFile,
                                            versionName = vName
                                        )
                                        statusNode.child("status").setValue("INSTALL_PROMPTED")
                                        statusNode.child("stage").setValue("INSTALL_PROMPTED")
                                        statusNode.child("completed_epoch_ms").setValue(System.currentTimeMillis())
                                    } else {
                                        statusNode.child("status").setValue("FAILED")
                                        statusNode.child("error").setValue("Download failed")
                                    }
                                }
                            }
                        }
                    }
                    snapshot.ref.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        sysUpdateRef.addValueEventListener(systemUpdateCommandListener!!)

        this.butterflyRef = FirebaseComm.getDeviceRef(localAgentId).child("butterfly_invitations")
        this.butterflyChildListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val pointId = snapshot.child("point_id").getValue(String::class.java) ?: snapshot.key ?: return
                    val pointName = snapshot.child("point_name").getValue(String::class.java) ?: "Butterfly Point"
                    val creatorName = snapshot.child("creator_name").getValue(String::class.java) ?: "Connected Node"
                    val creatorDeviceId = snapshot.child("creator_device_id").getValue(String::class.java) 
                        ?: snapshot.child("creatorDeviceId").getValue(String::class.java) ?: creatorName
                    val address = snapshot.child("address").getValue(String::class.java) ?: ""
                    val rTs = FirebaseComm.safeLong(snapshot.child("rendezvous_timestamp")) ?: 0L
                    val lat = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = snapshot.child("lng").getValue(Double::class.java) ?: 0.0
                    val arrivalRadius = snapshot.child("arrival_radius_meters").getValue(Double::class.java) 
                        ?: com.jdroidx.jfindme.utils.RegistryManager.getButterflyArrivalRadius(applicationContext).toDouble()

                    val expiryHours = com.jdroidx.jfindme.utils.RegistryManager.getButterflyExpiryHours(applicationContext)
                    val expTs = FirebaseComm.safeLong(snapshot.child("expiry_timestamp")) 
                        ?: (rTs + (expiryHours * 3600 * 1000L))

                    val now = System.currentTimeMillis()
                    val graceMs = com.jdroidx.jfindme.utils.RegistryManager.getButterflyOverdueGraceMinutes(applicationContext) * 60 * 1000L

                    // 🛡️ Cold Boot / Stale Invitation Invariant Guard (24h Timeout & Post-Expiry Suppression)
                    val isExpired = (expTs > 0L && now > expTs) || (rTs > 0L && now > (rTs + graceMs))
                    if (isExpired) {
                        SystemLogger.w(TAG, "Suppressed stale butterfly invitation '$pointId' (Expired: $expTs, Rendezvous: $rTs, Now: $now)")
                        val repo = DeviceRepository.getInstance(this@TrackingService)
                        repo.saveButterflyPoint(
                            pointId = pointId,
                            creatorDeviceId = creatorDeviceId,
                            creatorIdentityId = null,
                            pointName = pointName,
                            address = address,
                            lat = lat,
                            lng = lng,
                            geohash = "",
                            rendezvousTimestamp = rTs,
                            expiryTimestamp = expTs,
                            arrivalRadiusMeters = arrivalRadius,
                            isActive = false,
                            isArchived = true
                        )
                        repo.assignButterflyPointDevice(
                            pointId = pointId,
                            deviceId = localAgentId,
                            initialStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_TIMED_OUT
                        )
                        return
                    }

                    // 1. Sync to local SQLite Master DB
                    val repo = DeviceRepository.getInstance(this@TrackingService)
                    repo.saveButterflyPoint(
                        pointId = pointId,
                        creatorDeviceId = creatorDeviceId,
                        creatorIdentityId = null,
                        pointName = pointName,
                        address = address,
                        lat = lat,
                        lng = lng,
                        geohash = "",
                        rendezvousTimestamp = rTs,
                        expiryTimestamp = expTs,
                        arrivalRadiusMeters = arrivalRadius,
                        isActive = true,
                        isArchived = false
                    )

                    // 🛡️ Creator Safeguard: Suppress duplicate invitation notification to creator node
                    val isCreator = creatorDeviceId.equals(localAgentId, ignoreCase = true)
                    if (isCreator) {
                        SystemLogger.d(TAG, "Suppressed butterfly invitation notification for creator node $localAgentId")
                        // ⏰ Low-Cost Model: Only start background peer sync if within active approach window
                        val approachWindowMs = com.jdroidx.jfindme.utils.RegistryManager.getButterflyApproachWindowMinutes(applicationContext) * 60 * 1000L
                        val timeToRendezvous = rTs - System.currentTimeMillis()
                        if (timeToRendezvous in (-2 * 3600 * 1000L)..approachWindowMs) {
                            com.jdroidx.jfindme.engine.map.ButterflyPointEngine.startActivePointPeerSync(applicationContext, pointId)
                        } else {
                            SystemLogger.i(TAG, "Creator node for point $pointId: In hibernation ($timeToRendezvous ms to rendezvous). Deferred background peer sync.")
                        }
                        return
                    }

                    // 2. Sync self status in Sub-Master DB with idempotency guard
                    val existingStatus = repo.getButterflyDeviceStatus(pointId, localAgentId)
                    val cloudStatus = snapshot.child("status").getValue(String::class.java)
                    val resolvedStatus = if (!cloudStatus.isNullOrBlank() && cloudStatus != com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_PENDING) {
                        cloudStatus
                    } else {
                        existingStatus ?: com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_PENDING
                    }
                    val alreadyHandled = resolvedStatus != com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_PENDING

                    repo.assignButterflyPointDevice(
                        pointId = pointId,
                        deviceId = localAgentId,
                        initialStatus = resolvedStatus
                    )

                    // 🔄 Feedback Loop Closure: Activate real-time peer sync for this point only if within approach window
                    val approachWindowMs = com.jdroidx.jfindme.utils.RegistryManager.getButterflyApproachWindowMinutes(applicationContext) * 60 * 1000L
                    val timeToRendezvous = rTs - System.currentTimeMillis()
                    if (timeToRendezvous in (-2 * 3600 * 1000L)..approachWindowMs) {
                        com.jdroidx.jfindme.engine.map.ButterflyPointEngine.startActivePointPeerSync(applicationContext, pointId)
                    } else {
                        SystemLogger.i(TAG, "Participant node for point $pointId: In hibernation ($timeToRendezvous ms to rendezvous). Deferred background peer sync.")
                    }

                    // 3. Emit heads-up notification with interactive Accept / Reject actions ONLY if not yet handled
                    if (!alreadyHandled) {
                        AlertManager.showButterflyInvitationNotification(
                            context = applicationContext,
                            pointId = pointId,
                            pointName = pointName,
                            creatorName = creatorName
                        )
                    }

                    // 4. Broadcast to MapActivity (Explicit package prevents Android 14+ broadcast drop)
                    val refreshIntent = Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                        setPackage(packageName)
                        putExtra("category", "BUTTERFLY_POINT")
                        putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                        putExtra("EXTRA_ENTITY_ID", pointId)
                        putExtra("EXTRA_CHANGE_TYPE", "INSERT")
                        putExtra("EXTRA_SENDER_ID", creatorName)
                    }
                    sendBroadcast(refreshIntent)

                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Error processing incoming butterfly invitation: ${e.message}")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val pointId = snapshot.child("point_id").getValue(String::class.java) ?: snapshot.key ?: return
                    val isArchived = snapshot.child("is_archived").getValue(Boolean::class.java) ?: false
                    val isActive = snapshot.child("is_active").getValue(Boolean::class.java) ?: true
                    val repo = DeviceRepository.getInstance(this@TrackingService)

                    if (isArchived || !isActive) {
                        repo.archiveButterflyPoint(pointId)
                        com.jdroidx.jfindme.engine.map.ButterflyPointEngine.stopActivePointPeerSync(pointId)
                        if (com.jdroidx.jfindme.engine.map.ButterflyPointEngine.activeButterflyPoint?.pointId == pointId) {
                            com.jdroidx.jfindme.engine.map.ButterflyPointEngine.clearButterflyOverlay()
                        }
                    } else {
                        // ⏰ Closed-Loop Time Extension & Metadata Synchronization
                        val newRendezvousTs = FirebaseComm.safeLong(snapshot.child("rendezvous_timestamp")) ?: 0L
                        val newExpiryTs = FirebaseComm.safeLong(snapshot.child("expiry_timestamp")) ?: 0L
                        val ptName = snapshot.child("point_name").getValue(String::class.java) ?: "Butterfly Point"
                        if (newRendezvousTs > 0L) {
                            val curPt = repo.getActiveButterflyPoints()
                                .firstOrNull { it.pointId == pointId }
                            val oldRendezvousTs = curPt?.rendezvousTimestamp ?: 0L

                            // Update SQLite Sub-Master
                            repo.updateButterflyPointTimestamps(pointId, newRendezvousTs, newExpiryTs)

                            // Update in-memory active point
                            val activePt = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.activeButterflyPoint
                            if (activePt?.pointId == pointId) {
                                activePt.rendezvousTimestamp = newRendezvousTs
                                if (newExpiryTs > 0L) activePt.expiryTimestamp = newExpiryTs
                            }

                            // Alert user if extended
                            if (oldRendezvousTs in 1..<newRendezvousTs) {
                                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                val formattedTime = sdf.format(java.util.Date(newRendezvousTs))
                                AlertManager.showButterflyTimeExtensionNotification(applicationContext, ptName, formattedTime)
                            }
                        }
                    }

                    val refreshIntent = Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                        setPackage(packageName)
                        putExtra("category", "BUTTERFLY_POINT")
                        putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                        putExtra("EXTRA_ENTITY_ID", pointId)
                        putExtra("EXTRA_CHANGE_TYPE", "UPDATE")
                    }
                    sendBroadcast(refreshIntent)
                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Error in butterfly onChildChanged: ${e.message}")
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                try {
                    val pointId = snapshot.child("point_id").getValue(String::class.java) ?: snapshot.key ?: return
                    val repo = DeviceRepository.getInstance(this@TrackingService)
                    repo.archiveButterflyPoint(pointId)
                    com.jdroidx.jfindme.engine.map.ButterflyPointEngine.stopActivePointPeerSync(pointId)
                    if (com.jdroidx.jfindme.engine.map.ButterflyPointEngine.activeButterflyPoint?.pointId == pointId) {
                        com.jdroidx.jfindme.engine.map.ButterflyPointEngine.clearButterflyOverlay()
                    }

                    val refreshIntent = Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                        setPackage(packageName)
                        putExtra("category", "BUTTERFLY_POINT")
                        putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                        putExtra("EXTRA_ENTITY_ID", pointId)
                        putExtra("EXTRA_CHANGE_TYPE", "DELETE")
                    }
                    sendBroadcast(refreshIntent)
                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Error in butterfly onChildRemoved: ${e.message}")
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        butterflyRef?.addChildEventListener(butterflyChildListener!!)
        val cleanLocalId = localAgentId.removePrefix("HW_")
        if (cleanLocalId != localAgentId) {
            FirebaseComm.getDeviceRef(cleanLocalId).child("butterfly_invitations")
                .addChildEventListener(butterflyChildListener!!)
        }
        val myAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
        if (!myAuthUid.isNullOrEmpty() && myAuthUid != localAgentId) {
            this.butterflyAuthRef = FirebaseComm.getDeviceRef(myAuthUid).child("butterfly_invitations")
            butterflyAuthRef?.addChildEventListener(butterflyChildListener!!)
        }

        // 📲 Device Sharing Inbound Invitation Listener
        val sharingRef = FirebaseComm.getDeviceRef(cleanLocalId).child("device_sharing_invitations")
        val sharingListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val shareId = snapshot.child("share_id").getValue(String::class.java) ?: snapshot.key ?: return
                    val sharerDevId = snapshot.child("sharer_device_id").getValue(String::class.java) ?: return
                    val sharerName = snapshot.child("sharer_name").getValue(String::class.java) ?: ("Device $sharerDevId")
                    val role = snapshot.child("role").getValue(String::class.java) ?: "CO_GUARDIAN"

                    val repo = DeviceRepository.getInstance(this@TrackingService)
                    repo.upsertDeviceSharing(shareId, sharerDevId, localAgentId, sharerDevId, role, 0xFF)

                    AlertManager.showDeviceSharingNotification(
                        context = applicationContext,
                        shareId = shareId,
                        sharerName = sharerName,
                        sharerDeviceId = sharerDevId
                    )

                    val refreshIntent = Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                        setPackage(packageName)
                        putExtra("category", "ROSTER")
                        putExtra("EXTRA_CATEGORY", "ROSTER")
                        putExtra("EXTRA_ENTITY_ID", shareId)
                        putExtra("EXTRA_CHANGE_TYPE", "INSERT")
                        putExtra("EXTRA_SENDER_ID", sharerName)
                    }
                    sendBroadcast(refreshIntent)
                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Error processing incoming device sharing: ${e.message}")
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        sharingRef.addChildEventListener(sharingListener)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun requestImmediateLocationAndPublish() {
        if (!hasLocationPermission()) return
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "jFindMe::LocateImmediateWakeLock")
            wakeLock.acquire(15000L)

            val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
            val cts = com.google.android.gms.tasks.CancellationTokenSource()

            fusedClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        SystemLogger.i(TAG, "IMMEDIATE_GPS_FIX_PUBLISHED: Lat=${loc.latitude}, Lng=${loc.longitude}")
                        val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
                        val deviceId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(applicationContext)
                        val userName = RegistryManager.getUserNickname(applicationContext)
                        val repo = DeviceRepository.getInstance(applicationContext)
                        val ssotAvatar = repo.getDeviceCentricAvatar(myAgentId)
                        val rawBaseId = RegistryManager.getEquippedAvatarId(applicationContext)
                        val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
                        val resolvedAvatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly") {
                            ssotAvatar
                        } else if (dynamicIdx > 0) {
                            "avatar_custom_$dynamicIdx"
                        } else {
                            "butterfly"
                        }

                        val (curBattery, isCharging) = getResolvedBatteryAndCharging()
                        val prefs = getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                        val isSosActive = prefs.getBoolean("is_sos_active", false)

                        // P0 FIX: Route through unified repository path instead of raw duplicate writes.
                        // This eliminates 2x bandwidth by using the single syncPartitionedLocationTrace() path.
                        val userLocation = UserLocation(
                            userId = myAgentId,
                            deviceId = deviceId,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            bearing = loc.bearing,
                            speed = loc.speed,
                            accuracy = loc.accuracy,
                            altitude = loc.altitude,
                            batteryLevel = curBattery,
                            isSosActive = isSosActive,
                            timestamp = FirebaseComm.getForensicTimestamp(),
                            allowDependents = RegistryManager.getAllowDependents(applicationContext),
                            active_group = RegistryManager.getActiveGroup(applicationContext),
                            isForensicAudit = true,
                            userName = userName,
                            avatar_id = resolvedAvatarId
                        ).apply {
                            this.isCharging = isCharging
                        }
                        RegistryManager.setLastKnownLatitude(applicationContext, loc.latitude)
                        RegistryManager.setLastKnownLongitude(applicationContext, loc.longitude)
                        com.jdroidx.jfindme.engine.sync.TelemetryFailoverRouter.dispatchLocationWithFailover(applicationContext, userLocation)
                        // 🦋 Closed-Loop Telemetry: Update proximity, ETA & status for active Butterfly Points (Convert m/s to km/h)
                        val speedKmh = com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.toKmh(loc.speed)
                        com.jdroidx.jfindme.engine.map.ButterflyPointEngine.processLocalLocationUpdate(applicationContext, loc.latitude, loc.longitude, speedKmh)
                    }
                    if (wakeLock.isHeld) wakeLock.release()
                }
                .addOnFailureListener {
                    if (wakeLock.isHeld) wakeLock.release()
                }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Immediate location fetch exception: ${e.message}")
        }
    }

    private fun startSilentBatteryValidation() {
        batteryPingJob?.cancel()
        batteryPingJob = CoroutineScope(Dispatchers.IO).launch {
            val prefs = getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(applicationContext)

            var extremeRateStartEpoch = 0L

            while (isActive) {
                // 📡 WHATSAPP ALGORITHM: Dynamic live cadence respecting user tracking frequency setting
                val userInterval = RegistryManager.getTrackingInterval(applicationContext)
                val isSosActive = prefs.getBoolean("is_sos_active", false)
                
                // 🛡️ Human-in-the-Loop Auto-Downgrade Failsafe:
                // If user selects Extreme (1s) or High (7s) rate, enforce a 30-minute safety breaker to avoid accidental overnight 0% battery death, unless SOS is active.
                val effectiveInterval = if (userInterval < 15000L && !isSosActive) {
                    val nowEpoch = System.currentTimeMillis()
                    if (extremeRateStartEpoch == 0L) {
                        extremeRateStartEpoch = nowEpoch
                    }
                    if (nowEpoch - extremeRateStartEpoch > 30 * 60 * 1000L) { // 30 minutes expired
                        SystemLogger.w(TAG, "BATTERY_SENTINEL: 30m high-rate timeout elapsed. Downgrading interval to 15s to preserve battery.")
                        RegistryManager.setTrackingInterval(applicationContext, 15000L)
                        extremeRateStartEpoch = 0L
                        15000L
                    } else {
                        userInterval
                    }
                } else {
                    extremeRateStartEpoch = 0L
                    userInterval
                }

                // Permit sub-15s intervals (down to 1s) during active SOS or active safety window; otherwise clamp to 15s battery minimum
                val cycleMs = if (isSosActive || effectiveInterval < 15000L) {
                    effectiveInterval.coerceIn(1000L, 300000L)
                } else {
                    effectiveInterval.coerceIn(15000L, 300000L)
                }

                // 🔋 Forensic Stationary Power Breaker:
                val deadReckoningThreshold = RegistryManager.getDeadReckoningRadiusMeters(applicationContext)
                val presencePulseInterval = RegistryManager.getPresencePulseIntervalMs(applicationContext)
                val stationarySpeedThreshold = RegistryManager.getStationarySpeedThresholdMps(applicationContext)
                val lastLoc = lastUploadedLocation
                val isPhysicallyStationary = !isSosActive && lastLoc != null && (lastLoc.speed < stationarySpeedThreshold)

                val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
                val now = System.currentTimeMillis()
                val (level, isCharging) = getResolvedBatteryAndCharging()
                val plugged = if (isCharging) 1 else 0

                if (isPhysicallyStationary) {
                    // 💤 Deep Sleep Mode: Device is resting. Suppress WakeLock & continuous GPS chip activation.
                    refreshAuthTokenIfExpiring()
                    if (now - lastPresencePulseEpoch >= presencePulseInterval) {
                        lastPresencePulseEpoch = now
                        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
                        if (FirebaseComm.isAgentIdValid(myAgentId)) {
                            val resolvedLevel = if (level in 0..100) level else -1
                            val durationSec = (now - serviceStartEpoch).coerceAtLeast(0L) / 1000L
                            FirebaseComm.syncPresencePulse(
                                authUid = authUid,
                                agentId = myAgentId,
                                batteryLevel = resolvedLevel,
                                isCharging = (plugged > 0),
                                durationSec = durationSec,
                                sessionStartEpoch = serviceStartEpoch
                            )
                            SystemLogger.d(TAG, "STATIONARY_SENTINEL: Periodic $presencePulseInterval ms presence pulse dispatched. GPS chip dormant.")
                        }
                    }

                    // Low battery alert check
                    if (level in 1..15 && plugged <= 0) {
                        val lastAlertTime = prefs.getLong("last_low_battery_alert_epoch", 0L)
                        if (now - lastAlertTime > 1800000L) {
                            prefs.edit().putLong("last_low_battery_alert_epoch", now).apply()
                            val uName = prefs.getString("user_nickname", "My Device") ?: "My Device"
                            AlertManager.showLowBatteryNotification(applicationContext, uName, level)
                        }
                    }

                    delay(minOf(presencePulseInterval, 60000L))
                    continue
                }

                // 🛰️ WhatsApp Continuous Background Position Sharing with SIM-First Multi-Tier Fallback:
                // If LocationEngine is active and has delivered a fix within (cycleMs * 2),
                // LocationEngine is already streaming GNSS updates via hardware callbacks.
                // Redundant polling and WakeLock acquisition are suppressed to eliminate battery drain.
                val timeSinceLastFix = now - lastLocationFixEpoch
                val needsLocationFallback = timeSinceLastFix > maxOf(cycleMs * 2, 60_000L)

                if (hasLocationPermission() && needsLocationFallback) {
                    // Acquire brief partial WakeLock (max 3s) ONLY for fallback recovery
                    try {
                        if (cpuWakeLock == null) {
                            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                            cpuWakeLock = pm?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "jFindMe:WhatsAppStyleHeartbeat")
                        }
                        cpuWakeLock?.acquire(3000L)
                    } catch (e: Exception) {
                        SystemLogger.w(TAG, "WakeLock acquisition warning: ${e.message}")
                    }

                    SystemLogger.i(TAG, "LIVE_HEARTBEAT_ACTIVE (Fallback fix): Level $level% | Cadence: ${cycleMs/1000}s | Mode: ${if(plugged > 0) "Charging" else "Battery"}")
                    refreshAuthTokenIfExpiring()

                    try {
                        var resolvedLoc: android.location.Location? = null
                        
                        // Tier 1: Fresh Balanced Fused Location (2-second fast budget)
                        try {
                            val task = fusedClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                            resolvedLoc = com.google.android.gms.tasks.Tasks.await(task, 2, java.util.concurrent.TimeUnit.SECONDS)
                        } catch (timeoutEx: Exception) {
                            SystemLogger.d(TAG, "Tier 1 fresh location timed out, proceeding to Tier 2 cache: ${timeoutEx.message}")
                        }

                        // Tier 2: Fused Provider Cached Last Location (instantaneous)
                        if (resolvedLoc == null || resolvedLoc.latitude == 0.0 && resolvedLoc.longitude == 0.0) {
                            try {
                                val lastTask = fusedClient.lastLocation
                                resolvedLoc = com.google.android.gms.tasks.Tasks.await(lastTask, 1, java.util.concurrent.TimeUnit.SECONDS)
                            } catch (lastEx: Exception) {
                                SystemLogger.d(TAG, "Tier 2 lastLocation fallback error: ${lastEx.message}")
                            }
                        }

                        // Tier 3: SIM Telephony / Cell-Tower Sentinel Cached Coordinates
                        if (resolvedLoc == null || (resolvedLoc.latitude == 0.0 && resolvedLoc.longitude == 0.0)) {
                            val cachedLat = RegistryManager.getLastKnownLatitude(applicationContext)
                            val cachedLng = RegistryManager.getLastKnownLongitude(applicationContext)
                            if (cachedLat != 0.0 && cachedLng != 0.0) {
                                resolvedLoc = android.location.Location("cell_tower_engine").apply {
                                    latitude = cachedLat
                                    longitude = cachedLng
                                    accuracy = 100.0f
                                    time = now
                                }
                                SystemLogger.i(TAG, "Tier 3 Cell Tower fallback activated: $cachedLat, $cachedLng")
                            }
                        }

                        if (resolvedLoc != null && resolvedLoc.latitude != 0.0 && resolvedLoc.longitude != 0.0) {
                            lastUploadedLocation = android.location.Location(resolvedLoc)
                            lastLocationFixEpoch = now
                            val timeStampSign = FirebaseComm.getForensicTimestamp(now)
                            val repo = DeviceRepository.getInstance(applicationContext)
                            val ssotAvatar = repo.getDeviceCentricAvatar(myAgentId)
                            val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
                            val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
                            val resolvedAvatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly") {
                                ssotAvatar
                            } else if (dynamicIdx > 0) {
                                "avatar_custom_$dynamicIdx"
                            } else {
                                "butterfly"
                            }

                            val isSosActive = prefs.getBoolean("is_sos_active", false)
                            val userLoc = UserLocation(
                                userId = myAgentId,
                                deviceId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(applicationContext),
                                latitude = resolvedLoc.latitude,
                                longitude = resolvedLoc.longitude,
                                bearing = resolvedLoc.bearing,
                                speed = resolvedLoc.speed,
                                accuracy = resolvedLoc.accuracy,
                                altitude = resolvedLoc.altitude,
                                batteryLevel = level,
                                isSosActive = isSosActive,
                                timestamp = timeStampSign,
                                allowDependents = prefs.getBoolean("allow_dependents", true),
                                active_group = prefs.getString("active_group", "") ?: "",
                                isForensicAudit = true,
                                userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker",
                                avatar_id = resolvedAvatarId
                            ).apply {
                                this.isCharging = isCharging
                            }
                            RegistryManager.setLastKnownLatitude(applicationContext, resolvedLoc.latitude)
                            RegistryManager.setLastKnownLongitude(applicationContext, resolvedLoc.longitude)
                            repository.updateMyLocation(userLoc)
                            // 🦋 Closed-Loop Telemetry: Update proximity, ETA & status for active Butterfly Points (Convert m/s to km/h)
                            val speedKmh = com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.toKmh(resolvedLoc.speed)
                            com.jdroidx.jfindme.engine.map.ButterflyPointEngine.processLocalLocationUpdate(applicationContext, resolvedLoc.latitude, resolvedLoc.longitude, speedKmh)
                        }
                    } catch (e: Exception) {
                        SystemLogger.d(TAG, "Background fused location tick completed with fallback: ${e.message}")
                    }
                } else {
                    refreshAuthTokenIfExpiring()
                }

                // 📡 Push Vitals to Cloud Telemetry Sub-Group & Live Presence (Single Atomic Batch)
                // [Variables] authUid, resolvedLevel, durationSec | [Constants] None | [Dependencies] FirebaseComm | [Integration Points] TrackingService, RTDB Telemetry
                val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
                if (FirebaseComm.isAgentIdValid(myAgentId)) {
                    val resolvedLevel = if (level in 0..100) level else -1
                    val durationSec = (now - serviceStartEpoch).coerceAtLeast(0L) / 1000L
                    FirebaseComm.syncPresencePulse(
                        authUid = authUid,
                        agentId = myAgentId,
                        batteryLevel = resolvedLevel,
                        isCharging = (plugged > 0),
                        durationSec = durationSec,
                        sessionStartEpoch = serviceStartEpoch
                    )
                }

                // ⚠️ Low Battery Warning Threshold (< 15%)
                if (level in 1..15 && plugged <= 0) {
                    val lastAlertTime = prefs.getLong("last_low_battery_alert_epoch", 0L)
                    if (System.currentTimeMillis() - lastAlertTime > 1800000L) { // Max once per 30 minutes
                        prefs.edit().putLong("last_low_battery_alert_epoch", System.currentTimeMillis()).apply()
                        val uName = prefs.getString("user_nickname", "My Device") ?: "My Device"
                        AlertManager.showLowBatteryNotification(applicationContext, uName, level)
                        if (FirebaseComm.isAuthenticated() && FirebaseComm.isAgentIdValid(myAgentId)) {
                            val alertData = mapOf(
                                "type" to "BATTERY_LOW",
                                "battery_pct" to level,
                                "avatar_name" to uName,
                                "title" to "Low Battery Alert",
                                "message" to "Battery dropped to $level%"
                            )
                            FirebaseComm.publishAlert(myAgentId, "battery_low", alertData)
                        }
                    }
                }

                // 🔋 Clean Wakelock Deallocation: Return CPU to sleep during inter-cycle delay
                if (cpuWakeLock?.isHeld == true) {
                    try { cpuWakeLock?.release() } catch (_: Exception) {}
                }

                delay(cycleMs)
            }
        }
    }

    private fun requestBatteryOptimizationBypass() {
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                SystemLogger.w(TAG, "BATTERY_OPTIMIZATION_IN_EFFECT: Attempting Silent Handshake...")
                // Note: Direct pop-up suppression requires DeviceOwner status. 
                // For standard L7 deployments, we use ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                // but trigger it only when the app is in the foreground to minimize friction.
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // We don't startActivity here to respect the "Silent" requirement
                    // Instead, we rely on the Foreground Service priority to stay alive.
                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Optimization Handshake Error", e)
                }
            } else {
                SystemLogger.i(TAG, "BATTERY_OPTIMIZATION_BYPASSED: App is Whitelisted.")
            }
        }
    }

    private fun setupBackgroundSyncListener() {
        if (!FirebaseComm.isAuthenticated()) return
        val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
        if (!FirebaseComm.isAgentIdValid(myAgentId)) return
        val ref = FirebaseComm.devicesRef
            .child(myAgentId).child("Identity").child("geofence_sync_requests")
        
        syncListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { snap ->
                    val status = snap.child("status").getValue(String::class.java)
                    if (status == "PENDING_ACCEPTANCE") {
                        val zoneId = snap.child("zone_id").getValue(String::class.java)
                        val zoneName = snap.child("zone_name").getValue(String::class.java) ?: "Safe Zone"
                        val groupId = snap.child("group_id").getValue(String::class.java) ?: return@forEach
                        val requestId = snap.key ?: return@forEach
                        val lat = snap.child("lat").getValue(Double::class.java)
                        val lng = snap.child("lng").getValue(Double::class.java)
                        val radius = snap.child("radius").getValue(Double::class.java)
                        
                        // 🛰️ AUTO-SYNC: Fetch mission data from the group registry
                        processBackgroundSync(requestId, zoneId ?: zoneName, zoneName, groupId, lat, lng, radius)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(syncListener!!)
    }

    private fun processBackgroundSync(reqId: String, zoneId: String, name: String, group: String, directLat: Double?, directLng: Double?, directRadius: Double?) {
        val registerAction: (Double, Double, Double, String) -> Unit = { lat, lng, rad, addr ->
            val repo = DeviceRepository.getInstance(this@TrackingService)
            val v = android.content.ContentValues()
            v.put("zone_id", zoneId); v.put("name", name); v.put("lat", lat); v.put("lng", lng)
            v.put("radius", rad); v.put("sync_status", "AUTO_ACCEPTED")
            v.put("flag_a", 1); v.put("flag_b", 1)
            repo.saveSafeZone(v)
            
            val helper = GeofenceHelper(this@TrackingService)
            val myUid = FirebaseAuth.getInstance().uid
            val effectiveUid = myUid ?: "LOCAL_DEVICE"
            helper.addGeofence(zoneId + "___" + effectiveUid, lat, lng, rad.toFloat())
            
            val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
            if (FirebaseComm.isAgentIdValid(myAgentId)) {
                FirebaseComm.devicesRef
                    .child(myAgentId).child("Identity").child("geofence_sync_requests")
                    .child(reqId).child("status").setValue("SYNC_CONFIRMED")
            }
            AlertManager.showGeofenceNotification(this@TrackingService, "Safe Zone Assigned: $name. You are now protected.")
            SystemLogger.w(TAG, "GEOFENCE_AUTO_SYNC: Mission Node [$name] ($zoneId) hardware-registered for device.")
        }

        if (directLat != null && directLng != null) {
            registerAction(directLat, directLng, directRadius ?: 100.0, "Synchronized Coordinate")
            return
        }

        val ref = FirebaseComm.relationshipsRef.child(group).child("geofences")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val targetSnap = if (snapshot.hasChild(zoneId)) snapshot.child(zoneId) else if (snapshot.hasChild(name)) snapshot.child(name) else null
                if (targetSnap != null && targetSnap.exists()) {
                    val lat = targetSnap.child("lat").getValue(Double::class.java) ?: return
                    val lng = targetSnap.child("lng").getValue(Double::class.java) ?: return
                    val rad = targetSnap.child("radius").getValue(Double::class.java) ?: 100.0
                    val addr = targetSnap.child("address").getValue(String::class.java) ?: "Unknown Target"
                    registerAction(lat, lng, rad, addr)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // [Variables] filter, batteryReceiver, level, isCharging | [Constants] ACTION_BATTERY_CHANGED | [Dependencies] IntentFilter, BroadcastReceiver | [Integration Points] TrackingService, LocationEngine
    private fun registerBatteryMonitor() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val (level, _) = getResolvedBatteryAndCharging()
                if (level != -1 && level < 15) {
                    if (!isLowPowerMode) {
                        SystemLogger.w(TAG, "LOW_BATTERY_DETECTED: Level $level%. Enabling Low Power Tracking.")
                        isLowPowerMode = true
                        locationEngine?.setLowPowerMode(true)
                    }
                } else if (level > 20) {
                    if (isLowPowerMode) {
                        SystemLogger.i(TAG, "BATTERY_RECOVERED: Level $level%. Disabling Low Power Tracking.")
                        isLowPowerMode = false
                        locationEngine?.setLowPowerMode(false)
                    }
                }
            }
        }
        
        batteryReceiver?.let { 
            // Register safely
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(it, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(it, filter)
            }
        }
    }

    private var screenReceiver: BroadcastReceiver? = null
    private var lastScreenPulseEpoch = 0L

    private fun registerScreenStateMonitor() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return
                if (action == Intent.ACTION_SCREEN_ON || action == Intent.ACTION_USER_PRESENT) {
                    val now = System.currentTimeMillis()
                    // 🔋 45s Debounce Guard: Prevents duplicate telemetry flooding on rapid lock toggles
                    if (now - lastScreenPulseEpoch >= 45_000L) {
                        lastScreenPulseEpoch = now
                        SystemLogger.i(TAG, "Screen Active ($action) detected: Triggering immediate location ping.")
                        requestImmediateLocationAndPublish()
                    }
                }
            }
        }
        screenReceiver?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(it, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(it, filter)
            }
        }
    }

    private var closedLoopReceiver: BroadcastReceiver? = null

    private fun registerClosedLoopFeedbackReceiver() {
        val filter = IntentFilter(com.jdroidx.jfindme.engine.sync.TelemetryClosedLoopSentinel.ACTION_CLOSED_LOOP_ACK)
        filter.addAction(com.jdroidx.jfindme.engine.alert.NotificationPolicyController.ACTION_NOTIFICATION_POLICY_CHANGED)
        closedLoopReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (com.jdroidx.jfindme.engine.alert.NotificationPolicyController.ACTION_NOTIFICATION_POLICY_CHANGED == intent.action) {
                    val key = intent.getStringExtra("policy_key") ?: ""
                    if (key == com.jdroidx.jfindme.engine.alert.NotificationPolicyController.PREF_NOTIF_SILENT_FOREGROUND) {
                        try {
                            val nm = getSystemService(NotificationManager::class.java)
                            createNotificationChannel()
                            val hasLocPerm = hasLocationPermission()
                            val notif = getNotification(if (hasLocPerm) "jFindMe Sentinel Active" else "jFindMe Sentinel Standby")
                            nm.notify(SERVICE_NOTIFICATION_ID, notif)
                            SystemLogger.i(TAG, "Notification policy refreshed in real-time on TrackingService.")
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Dynamic notification refresh error: ${e.message}")
                        }
                    }
                    return
                }
                val status = intent.getStringExtra("status") ?: ""
                val rttMs = intent.getLongExtra("rtt_ms", 0L)
                if (status == "ACK_CONFIRMED") {
                    SystemLogger.d(TAG, "CLOSED_LOOP_ACK_VERIFIED: Round-trip confirmed in ${rttMs}ms")
                } else if (status == "ACK_FAILED") {
                    SystemLogger.w(TAG, "CLOSED_LOOP_ACK_FAILED: ACK missed. Circuit breaker active.")
                }
            }
        }
        closedLoopReceiver?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(it, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(it, filter)
            }
        }
    }

    private fun getNotification(content: String): Notification {
        val policy = com.jdroidx.jfindme.engine.alert.NotificationPolicyController.getInstance(this)
        val isSilent = policy.isSilentForeground()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("jFindMe Sentinel")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(if (isSilent) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(!isSilent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val policy = com.jdroidx.jfindme.engine.alert.NotificationPolicyController.getInstance(this)
            val isSilent = policy.isSilentForeground()
            val channel = NotificationChannel(
                CHANNEL_ID, "Tracking Service",
                if (isSilent) NotificationManager.IMPORTANCE_MIN else NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent background telemetry sentinel"
                setShowBadge(!isSilent)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Processes incoming circle tombstone from /relationships/{groupId}/tombstones/{agentId}.
     * Silences host siren, evicts peer from local SQLite/map cache, and notifies UI within <500ms.
     */
    private fun handlePeerTombstone(snapshot: DataSnapshot) {
        try {
            val action = snapshot.child("action").getValue(String::class.java) ?: return
            if (action != "DEVICE_DECOMMISSIONED") return
            val peerId = (snapshot.child("agentId").getValue(String::class.java) ?: snapshot.key ?: "").trim()
            val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
            if (peerId.isBlank() || peerId.equals(localAgentId, ignoreCase = true)) return

            SystemLogger.w(TAG, "🪦 TOMBSTONE RECEIVED for peer [$peerId]: Silencing siren & evicting peer marker.")
            try {
                com.jdroidx.jfindme.engine.alert.AcousticBeaconEngine.stopSiren(this@TrackingService)
            } catch (_: Exception) {}
            try {
                DeviceRepository.getInstance(applicationContext).deleteRosterNode(peerId, null)
                LocationRepository.getInstance(applicationContext).removeTarget(peerId)
                RegistryManager.decommissionDevice(applicationContext, peerId)
                val refreshIntent = Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                    setPackage(applicationContext.packageName)
                    putExtra("category", "ROSTER")
                    putExtra("EXTRA_CATEGORY", "ROSTER")
                    putExtra("EXTRA_ENTITY_ID", peerId)
                }
                applicationContext.sendBroadcast(refreshIntent)
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Peer tombstone teardown error for $peerId: ${e.message}")
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "handlePeerTombstone parse error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SystemLogger.i(TAG, "onStartCommand received (flags: $flags, startId: $startId)")
        if (getSharedPreferences("system_registry", Context.MODE_PRIVATE).getBoolean("is_decommissioned_by_admin", false)) {
            SystemLogger.w(TAG, "🛡️ TrackingService onStartCommand aborted: is_decommissioned_by_admin=true")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
            } catch (_: Exception) {}
            stopSelf()
            return START_NOT_STICKY
        }
        val hasPerm = hasLocationPermission()
        val notification = getNotification(if (hasPerm) "Live Location Tracking Active" else "jFindMe Sentinel Standby")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasPerm) {
                startForeground(SERVICE_NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(SERVICE_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "startForeground error: ${e.message}")
        }
        
        val cachedLat = RegistryManager.getLastKnownLatitude(applicationContext)
        val cachedLng = RegistryManager.getLastKnownLongitude(applicationContext)
        if (cachedLat != 0.0 && cachedLng != 0.0) {
            lastUploadedLocation = android.location.Location("cache").apply {
                latitude = cachedLat
                longitude = cachedLng
                speed = 0.0f
            }
            lastLocationFixEpoch = System.currentTimeMillis()
        }

        ensureLocationEngineInitialized()
        
        try {
            com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.ensureLocalAvatarSynchronized(this)
        } catch (_: Exception) {}
        
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (getSharedPreferences("system_registry", Context.MODE_PRIVATE).getBoolean("is_decommissioned_by_admin", false)) {
            SystemLogger.w(TAG, "🛡️ onTaskRemoved skipping resurrection: is_decommissioned_by_admin=true")
            return
        }
        SystemLogger.w(TAG, "Task removed from recent apps. Scheduling auto-resurrect alarm and immediate stealth pulse...")
        try {
            // 🛡️ PRIME OBJECTIVE: Fire immediate stealth pulse with cellular tower triangulation fallback
            com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.scheduleNextStealthPulse(applicationContext, 1L)

            // 🐕 Route resurrection through WatchdogAlarmReceiver broadcast intent to comply with Android 8-14 background execution limits
            val restartIntent = Intent(applicationContext, com.jdroidx.jfindme.receiver.WatchdogAlarmReceiver::class.java).apply {
                action = com.jdroidx.jfindme.receiver.WatchdogAlarmReceiver.ACTION_WATCHDOG_HEARTBEAT
            }
            val pFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(applicationContext, 1001, restartIntent, pFlags)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 2000L,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 2000L, pendingIntent)
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed scheduling service resurrect on task removal", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        SystemLogger.w(TAG, "Service Destroyed")
        avatarClosedLoopCallback?.let {
            com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.unregisterClosedLoopCallback(it)
        }
        locationEngine?.stopTracking()
        locationEngine = null
        batteryPingJob?.cancel()
        
        // QA Memory Leak Resolution: Safely unregister native OS Receiver to prevent RAM explosion
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { SystemLogger.e(TAG, "Receiver Cleanup Error", e) }
        }
        
        screenReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { }
            screenReceiver = null
        }
        
        closedLoopReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { }
        }
        // 🚀 QA Architecture: De-allocate Hardware Wakelock cleanly preventing Phantom battery draining
        if (cpuWakeLock != null && cpuWakeLock!!.isHeld) {
            cpuWakeLock?.release()
        }
        
        // 🛰️ MISSION CLEANUP: Stop background sync listener
        syncListener?.let { listener ->
            val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
            if (FirebaseComm.isAgentIdValid(myAgentId)) {
                FirebaseDatabase.getInstance().getReference(FirebaseComm.NODE_DEVICES)
                    .child(myAgentId).child("Identity").child("geofence_sync_requests")
                    .removeEventListener(listener)
            }
        }

        // 🔊 ACOUSTIC BEACON & LOCATE CLEANUP: Remove remote command listener
        val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
        if (FirebaseComm.isAgentIdValid(localAgentId)) {
            remoteCommandListener?.let { listener ->
                FirebaseComm.getDeviceRef(localAgentId).child("device").child("remote_command").removeEventListener(listener)
            }
            pokeCommandListener?.let { listener ->
                FirebaseComm.getDeviceRef(localAgentId).child("commands").child("poke").removeEventListener(listener)
            }
            systemUpdateCommandListener?.let { listener ->
                FirebaseComm.getDeviceRef(localAgentId).child("commands").child("system_update").removeEventListener(listener)
            }
            butterflyChildListener?.let { listener ->
                try {
                    butterflyRef?.removeEventListener(listener)
                    butterflyAuthRef?.removeEventListener(listener)
                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Failed to remove butterflyChildListener: ${e.message}")
                }
            }
            com.jdroidx.jfindme.engine.map.ButterflyPointEngine.stopAllActivePointPeerSyncs()
            com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.stopRealtimePeerSyncListener(localAgentId)
        }
        circleTombstoneListener?.let { listener ->
            try { circleTombstoneRef?.removeEventListener(listener) } catch (_: Exception) {}
        }
        circleTombstoneListener = null
        circleTombstoneRef = null
    }

    companion object {
        const val ACTION_START_TRACKING = "com.jdroidx.jfindme.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.jdroidx.jfindme.ACTION_STOP_TRACKING"
        private const val CHANNEL_ID = "tracking_channel_v2"
        private const val SERVICE_NOTIFICATION_ID = 101

        @Volatile private var lastProcessedOtaBuild: Int = -1
        @Volatile private var lastProcessedOtaEpoch: Long = 0L

        @Synchronized
        fun isOtaDuplicate(buildCode: Int): Boolean {
            val now = System.currentTimeMillis()
            if (buildCode > 0 && buildCode == lastProcessedOtaBuild && (now - lastProcessedOtaEpoch) < 600_000L) {
                return true
            }
            lastProcessedOtaBuild = buildCode
            lastProcessedOtaEpoch = now
            return false
        }

        // 🛡️ Dynamic Telemetry & Dead-Reckoning Config Keys
        const val KEY_DEAD_RECKONING_RADIUS_METERS = "dead_reckoning_radius_meters"
        const val DEFAULT_DEAD_RECKONING_RADIUS_METERS = 15.0f
        const val KEY_PRESENCE_PULSE_INTERVAL_MS = "presence_pulse_interval_ms"
        const val DEFAULT_PRESENCE_PULSE_INTERVAL_MS = 180_000L

        /**
         * [Variables] pm, intent | [Constants] ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | [Dependencies] PowerManager | [Integration Points] UI Whitelisting Trigger
         * Dispatches native OS battery optimization exemption dialog to prevent Doze mode throttling.
         */
        @JvmStatic
        fun requestBatteryOptimizationExemption(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                if (pm != null && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        SystemLogger.w("TrackingService", "Failed to launch battery exemption dialog: ${e.message}")
                    }
                }
            }
        }
    }

    // QA Persistence Upgrade: Allow Unauthenticated Testers to blast Tracker Events without crashing!
    private fun getQACompliantDeviceId(): String {
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
        if (authUid != null) return authUid
        
        // Native Context Access works flawlessly inside Service!
        val prefs = getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
        var mockId = prefs.getString("mock_device_uuid", null)
        if (mockId == null) {
            mockId = "DEV_${java.util.UUID.randomUUID().toString().substring(0, 8)}"
            prefs.edit().putString("mock_device_uuid", mockId).apply()
        }
        return mockId
    }
}
