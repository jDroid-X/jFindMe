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
        cpuWakeLock?.acquire() // Forces CPU to physically stay awake executing Coroutines safely even when Screen is Locked!

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
            
            // 🚀 QA Fix: Force Background Avatar Sync (V8.7)
            val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
            val genderStr = prefs.getString("user_gender", "Male") ?: "Male"
            val safeBaseId = if (rawBaseId == -1) {
                 if (genderStr.contains("Female", true)) 4 else 1 // Fallback to 'Student' (F) or 'Walker' (M)
            } else {
                 rawBaseId 
            }
            // 🧬 V59.6: Forensic Identity Resolution (Scope Fix)
            val authUid = FirebaseAuth.getInstance().uid ?: return@LocationEngine
            val rawHw = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "NULL_HW"
            val deviceId = "HW_" + rawHw.uppercase()
            
            val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(safeBaseId)
            val resolvedAvatarId = if (dynamicIdx == -1) "custom_photo.png" else "avatar_custom_$dynamicIdx"

            // 🛡️ V59.3 Forensic Pulse Throttle
            val frequencyMs = prefs.getLong("pref_tracking_interval", 90000L)
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastPulseTime < frequencyMs) {
                SystemLogger.d(TAG, "THROTTLE: Ignoring high-frequency pulse (Delta: ${(currentTime - lastPulseTime)/1000}s)")
                return@LocationEngine
            }
            
            lastPulseTime = currentTime
            val timeStampSign = FirebaseComm.getForensicTimestamp(currentTime)

            // 🧬 V59.6: Standardized Forensic Pulse Update
            FirebaseComm.resolveHardwareIdentity(authUid) { agentId ->
                val finalAgentId = agentId ?: return@resolveHardwareIdentity
                
                val userLocation = UserLocation(
                    userId = finalAgentId,
                    deviceId = deviceId, 
                    latitude = location.latitude,
                    longitude = location.longitude,
                    geohash = com.jdroidx.jfindme.engine.location.GeohashHelper.encode(location.latitude, location.longitude),
                    bearing = location.bearing,
                    speed = location.speed,
                    timestamp = timeStampSign, // 🧬 V59.3 Forensic Format Sync
                    allowDependents = prefs.getBoolean("allow_dependents", true),
                    active_group = prefs.getString("active_group", "") ?: "",
                    isForensicAudit = true,
                    userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker",
                    avatar_id = resolvedAvatarId
                )
                repository.updateMyLocation(userLocation)
            }
        }
        locationEngine.startTracking()
        registerBatteryMonitor()
        
        // 🚀 MISSION CONNECTIVITY: Autonomous Geofence Handshake Listener (V47)
        setupBackgroundSyncListener()

        // 🔋 BATTERY TELEMETRY: Launch Silent Validation Ping
        startSilentBatteryValidation()

        // 🛡️ OPTIMIZATION BYPASS: Verify permanent background status
        requestBatteryOptimizationBypass()
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
                
                // Keep CPU Active during ping
                cpuWakeLock?.let { if (!it.isHeld) it.acquire(1000) }
                
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
            val finalId = agentId ?: myUid
            val ref = FirebaseDatabase.getInstance().getReference(FirebaseComm.NODE_DEVICES)
                .child(finalId).child("identity").child("geofence_sync_requests")
            
            syncListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { snap ->
                    val status = snap.child("status").getValue(String::class.java)
                    if (status == "PENDING_ACCEPTANCE") {
                        val zoneName = snap.child("zone_name").getValue(String::class.java) ?: return@forEach
                        val groupId = snap.child("group_id").getValue(String::class.java) ?: return@forEach
                        val requestId = snap.key ?: return@forEach
                        
                        // 🛰️ AUTO-SYNC: Fetch mission data from the group registry
                        processBackgroundSync(requestId, zoneName, groupId)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(syncListener!!)
    }
}

    private fun processBackgroundSync(reqId: String, name: String, group: String) {
        // 🚀 V67.2: Geofences are now part of the Forensic Hub node structure
        val ref = FirebaseDatabase.getInstance().getReference(FirebaseComm.NODE_DEVICES)
            .child(group).child("geofences").child(name)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (snap.exists()) {
                    val lat = snap.child("lat").getValue(Double::class.java) ?: return
                    val lng = snap.child("lng").getValue(Double::class.java) ?: return
                    val rad = snap.child("radius").getValue(Double::class.java) ?: 100.0
                    val addr = snap.child("address").getValue(String::class.java) ?: "Unknown Target"
                    
                    // 💾 LOCAL FORENSIC ARCHIVAL
                    val db = DatabaseHelper.getInstance(this@TrackingService)
                    val v = android.content.ContentValues()
                    v.put("zone_id", name); v.put("name", name); v.put("lat", lat); v.put("lng", lng)
                    v.put("radius", rad); v.put("sync_status", "AUTO_ACCEPTED")
                    db.saveSafeZone(v)
                    
                    // 🚀 HARDWARE REGISTRATION: Geofence Active on Device B Hardware
                    val helper = GeofenceHelper(this@TrackingService)
                    val myUid = FirebaseAuth.getInstance().uid
                    if (myUid != null) {
                        helper.addGeofence(name + "___" + myUid, lat, lng, rad.toFloat())
                        
                        // 🛰️ CONFIRMATION: Update Cloud request status in the Unified Hub
                        FirebaseComm.resolveHardwareIdentity(myUid) { agentId ->
                            val finalId = agentId ?: myUid
                            FirebaseDatabase.getInstance().getReference(FirebaseComm.NODE_DEVICES)
                                .child(finalId).child("identity").child("geofence_sync_requests")
                                .child(reqId).child("status").setValue("SYNC_CONFIRMED")
                        }
                    }
                    
                    // 🔔 UX NOTIFICATION: Notify Pushpa of new protection
                    AlertManager.showGeofenceNotification(this@TrackingService, "Safe Zone Assigned: $name. You are now protected.")
                    SystemLogger.w(TAG, "GEOFENCE_AUTO_SYNC: Mission Node [$name] hardware-registered for device.")
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
                    val finalId = agentId ?: myUid
                    FirebaseDatabase.getInstance().getReference(FirebaseComm.NODE_DEVICES)
                        .child(finalId).child("identity").child("geofence_sync_requests")
                        .removeEventListener(listener)
                }
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
