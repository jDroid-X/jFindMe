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
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * TrackingService: Foreground service for persistent GPS tracking.
 * Main -> Background Ops -> Service Layer -> TrackingService
 */
class TrackingService : Service() {

    private lateinit var locationEngine: LocationEngine
    private lateinit var repository: LocationRepository
    private var batteryReceiver: BroadcastReceiver? = null
    private var cpuWakeLock: android.os.PowerManager.WakeLock? = null
    private val TAG = "TrackingService_DozeBypass"

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

        val prefs = getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker"

        locationEngine = LocationEngine(this) { location ->
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
            val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(safeBaseId)
            val resolvedAvatarId = if (dynamicIdx == -1) "custom_photo.png" else "avatar_custom_$dynamicIdx"

            val userId = getQACompliantDeviceId()
            val userLocation = UserLocation(
                userId = userId,
                userName = userName,
                latitude = location.latitude,
                longitude = location.longitude,
                geohash = com.jdroidx.jfindme.engine.location.GeohashHelper.encode(location.latitude, location.longitude),
                bearing = location.bearing,
                speed = location.speed,
                timestamp = System.currentTimeMillis(),
                avatar_id = resolvedAvatarId, 
                allowDependents = prefs.getBoolean("allow_dependents", true),
                active_group = prefs.getString("active_group", "") ?: "",
                role_family = prefs.getString("role_family", "None") ?: "None",
                role_friend = prefs.getString("role_friend", "None") ?: "None"
            )
            repository.updateMyLocation(userLocation)
        }
        locationEngine.startTracking()
        registerBatteryMonitor()
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
        
        // QA Memory Leak Resolution: Safely unregister native OS Receiver to prevent RAM explosion
        // QA Memory Leak Resolution: Safely unregister native OS Receiver to prevent RAM explosion
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { SystemLogger.e(TAG, "Receiver Cleanup Error", e) }
        }
        
        // 🚀 QA Architecture: De-allocate Hardware Wakelock cleanly preventing Phantom battery draining
        if (cpuWakeLock != null && cpuWakeLock!!.isHeld) {
            cpuWakeLock?.release()
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
