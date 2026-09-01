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
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

/**
 * TrackingService: Foreground service for persistent GPS tracking.
 * Main -> Background Ops -> Service Layer -> TrackingService
 */
class TrackingService : Service() {

    private lateinit var locationEngine: LocationEngine
    private lateinit var repository: LocationRepository
    private var batteryReceiver: BroadcastReceiver? = null
    private var cpuWakeLock: android.os.PowerManager.WakeLock? = null
    private var syncListener: ValueEventListener? = null
    private val TAG = "TrackingService_DozeBypass"
    
    // 🔋 SILENT BATTERY VALIDATION (V9.2)
    private var batteryPingJob: kotlinx.coroutines.Job? = null
    private var lastPulseTime = 0L
    private val PING_INTERVAL_MS = 60000L // 60 Second Forensic Cycle
    private var serviceStartEpoch: Long = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        SystemLogger.i(TAG, "Service Created")
        repository = LocationRepository(this)
        createNotificationChannel()
        
        // QA OS14 Compliance Patch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(SERVICE_NOTIFICATION_ID, getNotification("Initializing tracking..."), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, getNotification("Initializing tracking..."))
        }
        
        // 🚀 QA Fix: Force Doze Mode Bypass via Hardware CPU Wakelock
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        cpuWakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "jFindMe::TelemetryWakeLock")
        // AUDIT FIX [AUD-005]: Added 10-minute timeout to prevent permanent CPU lock if service dies
        cpuWakeLock?.acquire(10 * 60 * 1000L) // 10-minute maximum hold; refreshed by battery ping job

        // 🧠 IDENTITY SYNC: Connect to the Global System Registry (L7 Authoritative Source)
        val prefs = getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker"

        locationEngine = LocationEngine(this) { location ->
            val p = getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
            if (p.getBoolean("pref_service_halted", false)) {
                SystemLogger.w(TAG, "Forensic Isolation Active: Background Telemetry Blocked.")
                return@LocationEngine
            }

            // Rule 5: Security Agent - Background Mock Check
            if (com.jdroidx.jfindme.engine.security.SecurityEngine.isMockLocation(location)) {
                SystemLogger.e(TAG, "MOCK_LOCATION_BLOCKED_IN_BACKGROUND")
                return@LocationEngine
            }

            SystemLogger.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
            
            // 🚀 Force Hardware CPU Wakelock Renewal on pulse
            try {
                if (cpuWakeLock?.isHeld != true) {
                    cpuWakeLock?.acquire(15 * 60 * 1000L)
                }
            } catch (e: Exception) {}

            // 🚀 Authoritative SSOT Background Avatar Sync
            val db = DatabaseHelper.getInstance(applicationContext)
            val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
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

            // 🛡️ V59.3 Forensic Pulse Throttle (Responsive 15s Default)
            val frequencyMs = prefs.getLong("pref_tracking_interval", 15000L)
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastPulseTime < frequencyMs) {
                SystemLogger.d(TAG, "THROTTLE: Ignoring high-frequency pulse (Delta: ${(currentTime - lastPulseTime)/1000}s)")
                return@LocationEngine
            }
            
            lastPulseTime = currentTime
            val timeStampSign = FirebaseComm.getForensicTimestamp(currentTime)

            // 🧬 V59.6: Forensic Identity Resolution (Reliable Non-Blocking Fallback)
            val authUid = FirebaseAuth.getInstance().uid
            val rawHw = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "NULL_HW"
            val deviceId = "HW_" + rawHw.uppercase()
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
                        timestamp = timeStampSign,
                        allowDependents = prefs.getBoolean("allow_dependents", true),
                        active_group = prefs.getString("active_group", "") ?: "",
                        isForensicAudit = true,
                        userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker",
                        avatar_id = resolvedAvatarId
                    )
                    repository.updateMyLocation(userLocation)
                }
            }

            if (authUid != null) {
                FirebaseComm.resolveHardwareIdentity(authUid) { agentId ->
                    val finalAgentId = if (!agentId.isNullOrEmpty() && FirebaseComm.isAgentIdValid(agentId)) agentId else localAgentId
                    publishLocation(finalAgentId)
                }
            } else {
                publishLocation(localAgentId)
            }
        }
        locationEngine.startTracking()
        registerBatteryMonitor()
        
        // 🚀 MISSION CONNECTIVITY: Autonomous Geofence Handshake Listener (V47)
        setupBackgroundSyncListener()

        // 🔊 ACOUSTIC BEACON: Listen for remote sirens & ring commands
        setupRemoteCommandListener()

        // 🔋 BATTERY TELEMETRY: Launch Silent Validation Ping
        startSilentBatteryValidation()

        // 🛡️ OPTIMIZATION BYPASS: Verify permanent background status
        requestBatteryOptimizationBypass()
    }

    private var remoteCommandListener: ValueEventListener? = null

    private fun setupRemoteCommandListener() {
        val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
        if (!FirebaseComm.isAgentIdValid(localAgentId)) return

        val cmdRef = FirebaseComm.getDeviceRef(localAgentId).child("device").child("remote_command")
        remoteCommandListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val action = snapshot.child("action").getValue(String::class.java)
                    val durationSec = snapshot.child("duration_sec").getValue(Int::class.java) ?: 30
                    val epochMs = snapshot.child("epoch_ms").getValue(Long::class.java) ?: 0L
                    
                    val now = System.currentTimeMillis()
                    if (action == "RING_DEVICE" && (now - epochMs) < 90000L) {
                        SystemLogger.w(TAG, "🔊 INCOMING REMOTE RING COMMAND RECEIVED for $localAgentId (${durationSec}s)")
                        com.jdroidx.jfindme.engine.alert.AcousticBeaconEngine.startSiren(
                            this@TrackingService,
                            durationSec,
                            "Remote Guardian"
                        )
                        snapshot.ref.removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                SystemLogger.e(TAG, "Remote command listener cancelled: ${error.message}")
            }
        }
        cmdRef.addValueEventListener(remoteCommandListener!!)
    }

    private fun startSilentBatteryValidation() {
        batteryPingJob?.cancel()
        batteryPingJob = CoroutineScope(Dispatchers.IO).launch {
            val prefs = getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            while (isActive) {
                val cycleMs = prefs.getLong("pref_tracking_interval", 60000L)
                
                // Forensic Battery Sweep
                val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val plugged = intent?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: -1
                
                SystemLogger.i(TAG, "BATTERY_PING_ACTIVE: Level $level% | Cycle: ${cycleMs/1000}s | Mode: ${if(plugged > 0) "Charging" else "Battery"}")
                
                // 📡 V68.0: Push Vitals to Cloud Telemetry Sub-Group
                val authUid = FirebaseAuth.getInstance().uid
                if (authUid != null && level != -1) {
                    FirebaseComm.resolveHardwareIdentity(authUid) { agentId ->
                        if (agentId != null) {
                            val now = System.currentTimeMillis()
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
                            FirebaseComm.updateTelemetry(agentId, telemetry)
                        }
                    }
                }

                // ⚠️ Low Battery Warning Threshold (< 15%)
                if (level in 1..15 && plugged <= 0) {
                    val lastAlertTime = prefs.getLong("last_low_battery_alert_epoch", 0L)
                    if (System.currentTimeMillis() - lastAlertTime > 1800000L) { // Max once per 30 minutes
                        prefs.edit().putLong("last_low_battery_alert_epoch", System.currentTimeMillis()).apply()
                        val uName = prefs.getString("user_nickname", "My Device") ?: "My Device"
                        AlertManager.showLowBatteryNotification(applicationContext, uName, level)
                        if (authUid != null) {
                            FirebaseComm.resolveHardwareIdentity(authUid) { agentId ->
                                if (agentId != null) {
                                    val alertData = mapOf(
                                        "type" to "BATTERY_LOW",
                                        "battery_pct" to level,
                                        "avatar_name" to uName,
                                        "title" to "Low Battery Alert",
                                        "message" to "Battery dropped to $level%"
                                    )
                                    FirebaseComm.publishAlert(agentId, "battery_low", alertData)
                                }
                            }
                        }
                    }
                }

                // AUDIT FIX [AUD-005]: Re-acquire with fresh 10-min timeout instead of indefinite hold
                cpuWakeLock?.let { if (!it.isHeld) it.acquire(10 * 60 * 1000L) }
                
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
        val myUid = FirebaseAuth.getInstance().uid ?: return
        FirebaseComm.resolveHardwareIdentity(myUid) { agentId ->
            val finalId = agentId ?: return@resolveHardwareIdentity
            val ref = FirebaseComm.devicesRef
                .child(finalId).child("Identity").child("geofence_sync_requests")
            
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
            
            if (myUid != null) {
                FirebaseComm.resolveHardwareIdentity(myUid) { agentId ->
                    val finalId = agentId ?: return@resolveHardwareIdentity
                    FirebaseComm.devicesRef
                        .child(finalId).child("Identity").child("geofence_sync_requests")
                        .child(reqId).child("status").setValue("SYNC_CONFIRMED")
                }
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
                    locationEngine.setLowPowerMode(true)
                } else if (level > 20) {
                    locationEngine.setLowPowerMode(false)
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
        val notification = getNotification("Live Location Tracking Active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(SERVICE_NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification)
        }
        
        if (::locationEngine.isInitialized) {
            locationEngine.startTracking()
        }
        
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
        locationEngine.stopTracking()
        batteryPingJob?.cancel()
        
        // QA Memory Leak Resolution: Safely unregister native OS Receiver to prevent RAM explosion
        // QA Memory Leak Resolution: Safely unregister native OS Receiver to prevent RAM explosion
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { SystemLogger.e(TAG, "Receiver Cleanup Error", e) }
        }
        
        // 🚀 QA Architecture: De-allocate Hardware Wakelock cleanly preventing Phantom battery draining
        if (cpuWakeLock != null && cpuWakeLock!!.isHeld) {
            cpuWakeLock?.release()
        }
        
        // 🛰️ MISSION CLEANUP: Stop background sync listener
        syncListener?.let { listener ->
            val myUid = FirebaseAuth.getInstance().uid
            if (myUid != null) {
                FirebaseComm.resolveHardwareIdentity(myUid) { agentId ->
                    val finalId = agentId ?: return@resolveHardwareIdentity
                    FirebaseDatabase.getInstance().getReference(FirebaseComm.NODE_DEVICES)
                        .child(finalId).child("Identity").child("geofence_sync_requests")
                        .removeEventListener(listener)
                }
            }
        }

        // 🔊 ACOUSTIC BEACON CLEANUP: Remove remote command listener
        remoteCommandListener?.let { listener ->
            val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
            if (FirebaseComm.isAgentIdValid(localAgentId)) {
                FirebaseComm.getDeviceRef(localAgentId).child("device").child("remote_command")
                    .removeEventListener(listener)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "tracking_channel"
        private const val SERVICE_NOTIFICATION_ID = 101
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
