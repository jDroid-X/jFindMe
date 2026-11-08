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
import com.jdroidx.jfindme.data.local.DatabaseHelper
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
    private val TAG = "TrackingService_DozeBypass"
    
    // 🔋 SILENT BATTERY VALIDATION (V9.2)
    private var batteryPingJob: kotlinx.coroutines.Job? = null
    private var lastPulseTime = 0L
    @Volatile private var lastUploadedLocation: android.location.Location? = null
    @Volatile private var lastPresencePulseEpoch: Long = 0L
    private val PING_INTERVAL_MS = 60000L // 60 Second Forensic Cycle
    private var serviceStartEpoch: Long = System.currentTimeMillis()

    private fun hasLocationPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
               androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
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

        ensureLocationEngineInitialized()
        registerBatteryMonitor()
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
            val p = getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
            if (p.getBoolean("pref_service_halted", false)) {
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

            // 🛡️ SOFTWARE GEOFENCING: Ray-Casting Polygon Perimeter Evaluation
            evaluateSoftwareGeofences(location.latitude, location.longitude)

            // 🚀 Authoritative SSOT Background Avatar Sync & Vitals Logging
            val db = DatabaseHelper.getInstance(applicationContext)
            val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
            val ssotAvatar = db.getDeviceCentricAvatar(myAgentId)
            val rawBaseId = p.getInt("equipped_avatar_id", 1)
            val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
            val resolvedAvatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly") {
                ssotAvatar
            } else if (dynamicIdx > 0) {
                "avatar_custom_$dynamicIdx"
            } else {
                "butterfly"
            }

            // Cache Device Vitals in local SQLite for offline health inspection
            try {
                val servingCell = com.jdroidx.jfindme.engine.location.CellTowerSentinel.extractServingCellInfo(applicationContext)
                val netType = if (servingCell.hasValidTowerData) servingCell.networkType else "CELLULAR"
                val sigDbm = if (servingCell.signalDbm != -999) servingCell.signalDbm else -75
                db.upsertDeviceVitals(
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
            val isSos = p.getBoolean("is_sos_active", false)
            val distMoved = lastUploadedLocation?.distanceTo(location) ?: Float.MAX_VALUE
            // [Variables]: deadReckoningThreshold, presencePulseInterval, stationarySpeedThreshold | [Constants]: KEY_DEAD_RECKONING_RADIUS_METERS, KEY_PRESENCE_PULSE_INTERVAL_MS, KEY_STATIONARY_SPEED_THRESHOLD_MPS | [Dependencies]: RegistryManager | [Integration Points]: Settings & Dynamic Fleet OOPS Config
            val deadReckoningThreshold = com.jdroidx.jfindme.utils.RegistryManager.getDeadReckoningRadiusMeters(applicationContext)
            val presencePulseInterval = com.jdroidx.jfindme.utils.RegistryManager.getPresencePulseIntervalMs(applicationContext)
            val stationarySpeedThreshold = com.jdroidx.jfindme.utils.RegistryManager.getStationarySpeedThresholdMps(applicationContext)
            val isPhysicallyStationary = (distMoved < deadReckoningThreshold && location.speed < stationarySpeedThreshold) && !isSos

            // 🛡️ Spatial Dead-Reckoning: Suppress redundant full GPS uploads when stationary (< deadReckoningThreshold m)
            if (isPhysicallyStationary) {
                // Dispatch lean 28-byte presence heartbeat every presencePulseInterval ms (default 180s) to keep server.js and family map ONLINE
                if (currentTime - lastPresencePulseEpoch >= presencePulseInterval) {
                    lastPresencePulseEpoch = currentTime
                    val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
                    val authUid = FirebaseAuth.getInstance().uid ?: ""
                    FirebaseComm.syncPresencePulse(authUid, localAgentId, p.getInt("last_known_battery", 100))
                    SystemLogger.d(TAG, "DEAD_RECKONING: Stationary device ($distMoved m < $deadReckoningThreshold m). Dispatched 28-byte presence heartbeat.")
                }
                return@LocationEngine
            }

            // Kinematic movement (> 15m) or Emergency SOS: Update anchor and continue through adaptive throttle
            lastUploadedLocation = android.location.Location(location)
            lastPresencePulseEpoch = currentTime

            // 🛡️ Adaptive Telemetry Polling Throttle (GisSpatialEngine)
            val adaptiveInterval = com.jdroidx.jfindme.engine.location.GisSpatialEngine.computeAdaptivePollingInterval(
                isSosActive = isSos,
                speedMps = location.speed,
                batteryPct = p.getInt("last_known_battery", 100),
                isCharging = p.getBoolean("last_known_charging", false),
                isStationary = isPhysicallyStationary
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
                        timestamp = timeStampSign,
                        allowDependents = p.getBoolean("allow_dependents", true),
                        active_group = p.getString("active_group", "") ?: "",
                        isForensicAudit = true,
                        userName = p.getString("user_nickname", "Active Tracker") ?: "Active Tracker",
                        avatar_id = resolvedAvatarId
                    )
                    com.jdroidx.jfindme.engine.sync.TelemetryFailoverRouter.dispatchLocationWithFailover(applicationContext, userLocation)
                }
            }

            // 🧬 Hardware Root Anchor: Publish strictly to local hardware silicon agent ID
            publishLocation(localAgentId)
        }
        if (isLowPowerMode) {
            locationEngine?.setLowPowerMode(true)
        }
        locationEngine?.startTracking()
    }

    private fun evaluateSoftwareGeofences(lat: Double, lng: Double) {
        try {
            val db = DatabaseHelper.getInstance(applicationContext)
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

    private var remoteCommandListener: ValueEventListener? = null
    private var pokeCommandListener: ValueEventListener? = null

    private fun setupRemoteCommandListener() {
        if (!FirebaseComm.isAuthenticated()) return
        val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
        if (!FirebaseComm.isAgentIdValid(localAgentId)) return

        // 🛰️ MASTER DATA SYNC SENTINEL: Real-time peer inbox listener
        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.startRealtimePeerSyncListener(applicationContext, localAgentId)

        val cmdRef = FirebaseComm.getDeviceRef(localAgentId).child("device").child("remote_command")
        remoteCommandListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val action = snapshot.child("action").getValue(String::class.java)
                    val durationSec = snapshot.child("duration_sec").getValue(Int::class.java) ?: 30
                    val epochMs = snapshot.child("epoch_ms").getValue(Long::class.java) ?: 0L
                    val now = System.currentTimeMillis()

                    if (action == "RING_DEVICE") {
                        if ((now - epochMs) < 90000L) {
                            SystemLogger.w(TAG, "🔊 INCOMING REMOTE RING COMMAND RECEIVED for $localAgentId (${durationSec}s)")
                            com.jdroidx.jfindme.engine.alert.AcousticBeaconEngine.startSiren(
                                this@TrackingService,
                                durationSec,
                                "Remote Guardian"
                            )
                        } else {
                            SystemLogger.w(TAG, "🔊 Expired RING_DEVICE command ($epochMs) purged from cloud")
                        }
                        snapshot.ref.removeValue()
                    } else if (action == "LOCATE_DEVICE" || action == "POKE_DEVICE" || action == "REQUEST_LOCATION" || action == "PING_LOCATION") {
                        if ((now - epochMs) < 90000L) {
                            SystemLogger.w(TAG, "🛰️ INCOMING REMOTE LOCATE COMMAND RECEIVED for $localAgentId -> triggering immediate GPS fix")
                            requestImmediateLocationAndPublish()
                        } else {
                            SystemLogger.w(TAG, "🛰️ Expired LOCATE_DEVICE command ($epochMs) purged from cloud")
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

        val pokeRef = FirebaseComm.getDeviceRef(localAgentId).child("commands").child("poke")
        pokeCommandListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val cmd = snapshot.child("command").getValue(String::class.java)
                    val ts = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    val now = System.currentTimeMillis()
                    if (cmd == "POKE_LOCATION_UPDATE") {
                        if ((now - ts) < 90000L) {
                            SystemLogger.w(TAG, "🛰️ INCOMING POKE LOCATION UPDATE RECEIVED for $localAgentId")
                            requestImmediateLocationAndPublish()
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
                        val db = DatabaseHelper.getInstance(applicationContext)
                        val ssotAvatar = db.getDeviceCentricAvatar(myAgentId)
                        val rawBaseId = RegistryManager.getEquippedAvatarId(applicationContext)
                        val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
                        val resolvedAvatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly") {
                            ssotAvatar
                        } else if (dynamicIdx > 0) {
                            "avatar_custom_$dynamicIdx"
                        } else {
                            "butterfly"
                        }

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
                            timestamp = FirebaseComm.getForensicTimestamp(),
                            allowDependents = RegistryManager.getAllowDependents(applicationContext),
                            active_group = RegistryManager.getActiveGroup(applicationContext),
                            isForensicAudit = true,
                            userName = userName,
                            avatar_id = resolvedAvatarId
                        )
                        RegistryManager.setLastKnownLatitude(applicationContext, loc.latitude)
                        RegistryManager.setLastKnownLongitude(applicationContext, loc.longitude)
                        com.jdroidx.jfindme.engine.sync.TelemetryFailoverRouter.dispatchLocationWithFailover(applicationContext, userLocation)
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

            while (isActive) {
                // 📡 WHATSAPP ALGORITHM: Dynamic live cadence respecting user tracking frequency setting
                val userInterval = RegistryManager.getTrackingInterval(applicationContext)
                val cycleMs = userInterval.coerceIn(15000L, 300000L) // Respect configured interval (min 15s)
                
                // Acquire brief partial WakeLock (max 3s) to bypass deep Android Doze while screen is off / silent
                try {
                    if (cpuWakeLock == null) {
                        val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                        cpuWakeLock = pm?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "jFindMe:WhatsAppStyleHeartbeat")
                    }
                    cpuWakeLock?.acquire(3000L)
                } catch (e: Exception) {
                    SystemLogger.w(TAG, "WakeLock acquisition warning: ${e.message}")
                }
                
                // Forensic Battery Sweep
                val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val plugged = intent?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: -1
                
                SystemLogger.i(TAG, "LIVE_HEARTBEAT_ACTIVE: Level $level% | Cadence: ${cycleMs/1000}s | Mode: ${if(plugged > 0) "Charging" else "Battery"}")
                
                val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
                val now = System.currentTimeMillis()

                // 🛰️ WhatsApp Continuous Background Position Sharing with SIM-First Multi-Tier Fallback:
                // When app is in background or silent mode, proactively fetch fresh fused/network location
                if (hasLocationPermission()) {
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
                            val timeStampSign = FirebaseComm.getForensicTimestamp(now)
                            val db = DatabaseHelper.getInstance(applicationContext)
                            val ssotAvatar = db.getDeviceCentricAvatar(myAgentId)
                            val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
                            val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
                            val resolvedAvatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly") {
                                ssotAvatar
                            } else if (dynamicIdx > 0) {
                                "avatar_custom_$dynamicIdx"
                            } else {
                                "butterfly"
                            }

                            val userLoc = UserLocation(
                                userId = myAgentId,
                                deviceId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(applicationContext),
                                latitude = resolvedLoc.latitude,
                                longitude = resolvedLoc.longitude,
                                bearing = resolvedLoc.bearing,
                                speed = resolvedLoc.speed,
                                accuracy = resolvedLoc.accuracy,
                                altitude = resolvedLoc.altitude,
                                timestamp = timeStampSign,
                                allowDependents = prefs.getBoolean("allow_dependents", true),
                                active_group = prefs.getString("active_group", "") ?: "",
                                isForensicAudit = true,
                                userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker",
                                avatar_id = resolvedAvatarId
                            )
                            RegistryManager.setLastKnownLatitude(applicationContext, resolvedLoc.latitude)
                            RegistryManager.setLastKnownLongitude(applicationContext, resolvedLoc.longitude)
                            repository.updateMyLocation(userLoc)
                        }
                    } catch (e: Exception) {
                        SystemLogger.d(TAG, "Background fused location tick completed with fallback: ${e.message}")
                    }
                }

                // 📡 Push Vitals to Cloud Telemetry Sub-Group
                if (FirebaseComm.isAuthenticated() && FirebaseComm.isAgentIdValid(myAgentId) && level != -1) {
                    val durationSec = (now - serviceStartEpoch).coerceAtLeast(0L) / 1000L
                    val telemetry = mapOf(
                        "battery_pct" to level,
                        "is_charging" to (plugged > 0),
                        "status" to FirebaseComm.STATUS_ONLINE,
                        "session_start_epoch" to serviceStartEpoch,
                        "duration_sec" to durationSec,
                        "duration_formatted" to FirebaseComm.formatDuration(durationSec),
                        "last_seen" to now,
                        "last_heartbeat" to now
                    )
                    FirebaseComm.updateTelemetry(myAgentId, telemetry)
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
            val db = DatabaseHelper.getInstance(this@TrackingService)
            val v = android.content.ContentValues()
            v.put("zone_id", zoneId); v.put("name", name); v.put("lat", lat); v.put("lng", lng)
            v.put("radius", rad); v.put("sync_status", "AUTO_ACCEPTED")
            v.put("flag_a", 1); v.put("flag_b", 1)
            db.saveSafeZone(v)
            
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

    private fun registerBatteryMonitor() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                if (level != -1 && level < 15) {
                    SystemLogger.w(TAG, "LOW_BATTERY_DETECTED: Level $level%. Enabling Low Power Tracking.")
                    isLowPowerMode = true
                    locationEngine?.setLowPowerMode(true)
                } else if (level > 20) {
                    isLowPowerMode = false
                    locationEngine?.setLowPowerMode(false)
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

    private var closedLoopReceiver: BroadcastReceiver? = null

    private fun registerClosedLoopFeedbackReceiver() {
        val filter = IntentFilter(com.jdroidx.jfindme.engine.sync.TelemetryClosedLoopSentinel.ACTION_CLOSED_LOOP_ACK)
        closedLoopReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("jFindMe Tracking")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SystemLogger.i(TAG, "onStartCommand received (flags: $flags, startId: $startId)")
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
        
        ensureLocationEngineInitialized()
        
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        SystemLogger.w(TAG, "Task removed from recent apps. Scheduling auto-resurrect alarm...")
        try {
            val restartIntent = Intent(applicationContext, TrackingService::class.java).apply {
                setPackage(packageName)
            }
            val pFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_ONE_SHOT
            }
            val pendingIntent = PendingIntent.getService(applicationContext, 1001, restartIntent, pFlags)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000L,
                pendingIntent
            )
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed scheduling service resurrect on task removal", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        SystemLogger.w(TAG, "Service Destroyed")
        locationEngine?.stopTracking()
        locationEngine = null
        batteryPingJob?.cancel()
        
        // QA Memory Leak Resolution: Safely unregister native OS Receiver to prevent RAM explosion
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { SystemLogger.e(TAG, "Receiver Cleanup Error", e) }
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
            com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.stopRealtimePeerSyncListener(localAgentId)
        }
    }

    companion object {
        const val ACTION_START_TRACKING = "com.jdroidx.jfindme.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.jdroidx.jfindme.ACTION_STOP_TRACKING"
        private const val CHANNEL_ID = "tracking_channel"
        private const val SERVICE_NOTIFICATION_ID = 101

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
