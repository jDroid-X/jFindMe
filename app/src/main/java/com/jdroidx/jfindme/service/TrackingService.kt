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
    private val TAG = "TrackingService"

    override fun onCreate() {
        super.onCreate()
        SystemLogger.i(TAG, "Service Created")
        repository = LocationRepository(this)
        createNotificationChannel()
        startForeground(SERVICE_NOTIFICATION_ID, getNotification("Initializing tracking..."))
        
        val prefs = getSharedPreferences("jFindMe_Prefs", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker"

        locationEngine = LocationEngine(this) { location ->
            // Rule 5: Security Agent - Background Mock Check
            if (com.jdroidx.jfindme.engine.security.SecurityEngine.isMockLocation(location)) {
                SystemLogger.e(TAG, "MOCK_LOCATION_BLOCKED_IN_BACKGROUND")
                return@LocationEngine
            }

            SystemLogger.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
            
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return@LocationEngine
            val userLocation = UserLocation(
                userId = userId,
                userName = userName,
                latitude = location.latitude,
                longitude = location.longitude,
                geohash = com.jdroidx.jfindme.engine.location.GeohashHelper.encode(location.latitude, location.longitude),
                bearing = location.bearing,
                speed = location.speed,
                timestamp = System.currentTimeMillis()
            )
            repository.updateMyLocation(userLocation)
        }
        locationEngine.startTracking()
        registerBatteryMonitor()
    }

    private fun registerBatteryMonitor() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryReceiver = object : BroadcastReceiver() {
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
        registerReceiver(batteryReceiver, filter)
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
    }

    companion object {
        private const val CHANNEL_ID = "tracking_channel"
        private const val SERVICE_NOTIFICATION_ID = 101
    }
}
