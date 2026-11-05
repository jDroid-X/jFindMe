package com.jdroidx.jfindme.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.jdroidx.jfindme.utils.SystemLogger

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * SIMTrackingService: Independent Forensic Hardware Engine (V5.6).
 * Handles: SIM Inventory, Tower Logging, and Local Breadcrumbs.
 * Architecture: 100% Offline, Silent, and Decoupled.
 */
class SIMTrackingService : Service() {

    private val TAG = "SIMTracking_Engine"

    companion object {
        const val NOTIFICATION_ID = 102
        const val CHANNEL_ID = "sim_tracking_channel"
    }

    override fun onCreate() {
        super.onCreate()
        SystemLogger.i(TAG, "Forensic SIM Engine Initialized.")
        
        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, getNotification("Forensic Hardware Logging Active"), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, getNotification("Forensic Hardware Logging Active"))
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "SIMTrackingService startForeground error: ${e.message}")
        }
        
        startForensicHeartbeat()
    }

    private fun startForensicHeartbeat() {
        val workRequest = PeriodicWorkRequest.Builder(SimForensicWorker::class.java, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SIMForensicWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun getNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stationary Device Monitor")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Stationary Health Service", NotificationManager.IMPORTANCE_LOW)
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
        SystemLogger.w(TAG, "Forensic SIM Engine Terminated.")
    }
}
