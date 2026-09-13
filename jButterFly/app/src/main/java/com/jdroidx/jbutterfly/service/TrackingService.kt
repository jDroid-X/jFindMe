package com.jdroidx.jbutterfly.service

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jbutterfly.R
import com.jdroidx.jbutterfly.data.local.DatabaseHelper
import com.jdroidx.jbutterfly.data.model.UserLocation
import com.jdroidx.jbutterfly.data.remote.FirebaseComm
import com.jdroidx.jbutterfly.ui.alert.AcousticSirenActivity
import com.jdroidx.jbutterfly.ui.map.MapActivity
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper
import com.jdroidx.jbutterfly.utils.GeofenceEngine
import com.jdroidx.jbutterfly.utils.RegistryManager
import com.jdroidx.jbutterfly.utils.SystemLogger

/**
 * TrackingService — Unkillable Foreground Service providing WhatsApp-style continuous background tracking.
 * Features:
 * - Dynamic adaptive location polling (moving vs stationary intervals from RegistryManager)
 * - Silicon Hardware Agent ID anchoring
 * - Local SQLite SSOT storage + Cloud Realtime Database synchronization
 * - Geofence boundary verification
 * - WakeLock and AlarmManager watchdog integration
 * - Remote Buzz / Siren listener
 */
class TrackingService : Service() {

    companion object {
        private const val TAG = "TrackingService"
        const val CHANNEL_ID = "jbutterfly_tracking_channel"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START = "com.jdroidx.jbutterfly.action.START_TRACKING"
        const val ACTION_STOP = "com.jdroidx.jbutterfly.action.STOP_TRACKING"

        @JvmStatic
        fun startTracking(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        @JvmStatic
        fun stopTracking(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var buzzListener: ValueEventListener? = null
    private var incomingConnListener: ValueEventListener? = null
    private var currentBatteryPct: Int = 100
    private var batteryAlertFired = false

    private val insideZoneIds = mutableSetOf<String>()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                currentBatteryPct = (level * 100) / scale
                val threshold = RegistryManager.getInt(context, "battery_alert_threshold", 20)
                if (currentBatteryPct <= threshold && !batteryAlertFired) {
                    batteryAlertFired = true
                    DatabaseHelper.getInstance(context).insertNotification(
                        "🔋 Low Battery Warning",
                        "Battery dropped to $currentBatteryPct% (Threshold: $threshold%)",
                        "BATTERY"
                    )
                } else if (currentBatteryPct > threshold + 5) {
                    batteryAlertFired = false
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        SystemLogger.i(TAG, "TrackingService onCreate - Initializing telemetry pipeline")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jButterFly:TrackingWakeLock")
        wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24h safety timeout

        createNotificationChannel()

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val agentId = DeviceIdentityHelper.getOrGenerateAgentId(this)

        // Start listening for remote buzz commands
        buzzListener = FirebaseComm.listenForBuzzCommands(agentId) { senderId ->
            SystemLogger.i(TAG, "REMOTE BUZZ COMMAND RECEIVED from $senderId! Launching Acoustic Siren.")
            val db = DatabaseHelper.getInstance(this)
            db.insertNotification("🚨 Acoustic Siren Triggered", "Remote finding buzz initiated by $senderId", "SOS")
            val sirenIntent = Intent(this, AcousticSirenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("SENDER_ID", senderId)
            }
            startActivity(sirenIntent)
        }

        // Start listening for bidirectional pairing connections
        incomingConnListener = FirebaseComm.listenForIncomingConnections(agentId) { dev ->
            SystemLogger.i(TAG, "INCOMING CONNECTION DETECTED from ${dev.agentId} (${dev.deviceName})")
            val db = DatabaseHelper.getInstance(this)
            val existing = db.getDeviceByAgentId(dev.agentId)
            if (existing == null) {
                db.insertOrUpdateDevice(dev)
                db.insertNotification("🔗 New Device Linked", "Connected with ${dev.deviceName} (${dev.category})", "PAIRING")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification("Live Sentinel Active", "Device protected by jButterFly mesh")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startLocationUpdates()
        scheduleWatchdog()

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        stopLocationUpdates()

        val pollMoving = RegistryManager.getLong(this, RegistryManager.KEY_POLL_INTERVAL_MOVING, RegistryManager.DEFAULT_POLL_MOVING_MS)
        val fastestInterval = RegistryManager.getLong(this, RegistryManager.KEY_FASTEST_INTERVAL, RegistryManager.DEFAULT_FASTEST_MS)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, pollMoving)
            .setMinUpdateIntervalMillis(fastestInterval)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                handleNewLocation(loc)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
            SystemLogger.i(TAG, "Fused location tracking active with $pollMoving ms interval")
        } catch (e: SecurityException) {
            SystemLogger.e(TAG, "Location permission missing", e)
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun handleNewLocation(location: Location) {
        val agentId = DeviceIdentityHelper.getOrGenerateAgentId(this)
        val hardwareId = DeviceIdentityHelper.getHardwareUid(this)

        val userLocation = UserLocation(
            agentId = agentId,
            hardwareId = hardwareId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            speed = location.speed,
            heading = location.bearing,
            altitude = location.altitude,
            batteryPct = currentBatteryPct,
            timestamp = System.currentTimeMillis()
        )

        // 1. Update SQLite Local Database (SSOT)
        val db = DatabaseHelper.getInstance(this)
        db.updateDeviceLocation(agentId, location.latitude, location.longitude, location.speed, location.bearing, currentBatteryPct)
        db.recordHistory(userLocation)

        // 2. Publish to Firebase Cloud Realtime Mesh (Respect Ghost Cloaking Mode)
        val isGhost = RegistryManager.getBoolean(this, "ghost_mode_active", false)
        if (!isGhost) {
            FirebaseComm.publishLocation(userLocation)
        } else {
            SystemLogger.i(TAG, "Ghost Cloaking Mode ACTIVE: Remote coordinates suppressed for $agentId")
        }

        // 3. Geofence In/Out Verification
        checkGeofenceTransitions(location.latitude, location.longitude)

        // 4. Update notification text
        val summary = "Lat: ${"%.4f".format(location.latitude)}, Lng: ${"%.4f".format(location.longitude)} | Bat: $currentBatteryPct%"
        updateNotification(summary)
    }

    private fun checkGeofenceTransitions(lat: Double, lng: Double) {
        val db = DatabaseHelper.getInstance(this)
        val safeZones = db.getAllSafeZones()

        for (zone in safeZones) {
            val isInside = GeofenceEngine.isInsideZone(lat, lng, zone)
            val wasInside = insideZoneIds.contains(zone.zoneId)

            if (isInside && !wasInside) {
                insideZoneIds.add(zone.zoneId)
                db.insertNotification("🛡️ Entered SafeZone", "Device entered ${zone.name} perimeter", "GEOFENCE")
                if (zone.notifyEntry) {
                    showGeofenceNotification("SafeZone Alert: Entered", "Device entered ${zone.name}")
                }
            } else if (!isInside && wasInside) {
                insideZoneIds.remove(zone.zoneId)
                db.insertNotification("📍 Exited SafeZone", "Device left ${zone.name} perimeter", "GEOFENCE")
                if (zone.notifyExit) {
                    showGeofenceNotification("SafeZone Alert: Exited", "Device left ${zone.name}")
                }
            }
        }
    }

    private fun showGeofenceNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.butterfly)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notif)
    }

    private fun scheduleWatchdog() {
        val intent = Intent(this, com.jdroidx.jbutterfly.receiver.WatchdogAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMs = 15 * 60 * 1000L // 15 minutes
        val triggerAt = SystemClock.elapsedRealtime() + intervalMs

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "jButterFly Background Sentinel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous enterprise location and safety tracking"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(title: String, message: String): Notification {
        val tapIntent = Intent(this, MapActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.butterfly)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = buildForegroundNotification("jButterFly Live Radar", message)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        SystemLogger.w(TAG, "TrackingService onDestroy - Cleaning up")
        stopLocationUpdates()
        buzzListener?.let {
            val agentId = DeviceIdentityHelper.getOrGenerateAgentId(this)
            FirebaseComm.removeLocationListener(agentId, it)
        }
        try {
            unregisterReceiver(batteryReceiver)
        } catch (ignored: Exception) {}

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
