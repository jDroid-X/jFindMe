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
import com.jdroidx.jfindme.utils.SystemLogger
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth

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

        val prefs = getSharedPreferences("jFindMe_Prefs", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker"

        locationEngine = LocationEngine(this) { location ->
            val p = getSharedPreferences("jFindMe_Prefs", android.content.Context.MODE_PRIVATE)
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
        
        // 🚀 MISSION CONNECTIVITY: Autonomous Geofence Handshake Listener (V47)
        setupBackgroundSyncListener()
    }

    private fun setupBackgroundSyncListener() {
        val myUid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("users").child(myUid).child("geofence_sync_requests")
        
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

    private fun processBackgroundSync(reqId: String, name: String, group: String) {
        val ref = FirebaseDatabase.getInstance().getReference("groups").child(group).child("geofences").child(name)
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
                    val userName = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getString("user_nickname", "Pushpa")
                    helper.addGeofence(name + "___" + userName, lat, lng, rad.toFloat())
                    
                    // 🛰️ CONFIRMATION: Update Cloud request status
                    val myUid = FirebaseAuth.getInstance().uid
                    if (myUid != null) {
                        FirebaseDatabase.getInstance().getReference("users").child(myUid)
                            .child("geofence_sync_requests").child(reqId).child("status").setValue("SYNC_CONFIRMED")
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
        syncListener?.let {
            val myUid = FirebaseAuth.getInstance().uid
            if (myUid != null) {
                FirebaseDatabase.getInstance().getReference("users").child(myUid)
                    .child("geofence_sync_requests").removeEventListener(it)
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
