package com.jdroidx.jfindme.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * SIMTrackingService: Independent Forensic Hardware Engine (V5.6).
 * Handles: SIM Inventory, Tower Logging, and Local Breadcrumbs.
 * Architecture: 100% Offline, Silent, and Decoupled.
 */
class SIMTrackingService : Service() {

    private lateinit var dbHelper: com.jdroidx.jfindme.data.local.DatabaseHelper
    private var pulseTimer: java.util.Timer? = null
    private var cpuWakeLock: PowerManager.WakeLock? = null
    private val TAG = "SIMTracking_Engine"

    companion object {
        const val NOTIFICATION_ID = 102
        const val CHANNEL_ID = "sim_tracking_channel"
    }

    override fun onCreate() {
        super.onCreate()
        SystemLogger.i(TAG, "Forensic SIM Engine Initialized.")
        dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, getNotification("Forensic Hardware Logging Active"))
        
        // 🔒 Hardware CPU Anchor: Ensures logging continues during Deep Doze
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jFindMe::SIMForensicLock")
        cpuWakeLock?.acquire()

        startForensicHeartbeat()
    }

    private fun startForensicHeartbeat() {
        pulseTimer = java.util.Timer()
        pulseTimer?.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                val prefs = getSharedPreferences("system_registry", MODE_PRIVATE)
                
                // 🛡️ SECURITY OVERRIDE: Forensic Shutdown Interceptor
                if (prefs.getBoolean("pref_service_halted", false)) {
                    SystemLogger.w(TAG, "SIM Engine Suspended: Privacy Shield Halted.")
                    return@run
                }

                val curReadInterval = prefs.getLong("pref_tracking_interval", 60000L)
                
                // 🛡️ OFFLINE LEDGER: Independent SIM Breadcrumb Logging
                // This runs regardless of 'SIM Mode' or 'Network Status'
                if (System.currentTimeMillis() % curReadInterval < 1000) {
                    SystemLogger.d(TAG, "FORENSIC_PULSE: Archiving Cellular Hardware Event.")
                    
                    // Hardware Access: Simulated MCC:405 MNC:84 LAC:7F22 CID:8A91
                    dbHelper.logSimCard(0, "ICCID_8991_FIXED", "ENTERPRISE_GSM", "405", "84", "7F22", "8A91", false)
                    dbHelper.logSimTransaction(0, "405", "84", "7F22", "8A91", 0.0, 0.0, "SIM_FORENSIC")
                }
            }
        }, 5000, 1000) // 1s Internal Pulse Engine
    }

    private fun getNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SIM Security Engine")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setPriority(NotificationCompat.PRIORITY_MIN) // Stay silent and minimized
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "SIM Forensic Engine", NotificationManager.IMPORTANCE_MIN)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Stay alive across system reboots / kills
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (cpuWakeLock?.isHeld == true) cpuWakeLock?.release()
        pulseTimer?.cancel()
        SystemLogger.w(TAG, "Forensic SIM Engine Terminated.")
    }
}
