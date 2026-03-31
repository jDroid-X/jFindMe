package com.jdroidx.jfindme.engine.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.*
import androidx.core.app.NotificationCompat
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * AlertManager: Handles emergency SOS signaling and notifications.
 * Integration Fix: Ensure singleton object accessibility.
 */
object AlertManager {
    private const val TAG = "AlertManager"
    private const val CHANNEL_ID = "sos_alerts"
    private const val NOTIFICATION_ID = 911

    @JvmStatic
    fun triggerLocalSOSFeedback(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    @JvmStatic
    fun showSosNotification(context: Context, userName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Emergency SOS", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "High priority emergency alerts"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("EMERGENCY SOS: $userName")
            .setContentText("User has triggered an emergency signal! Check map.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        SystemLogger.w(TAG, "SOS_NOTIFICATION_POSTED for $userName")
    }

    @JvmStatic
    fun showGeofenceNotification(context: Context, payload: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val geoChannelId = "geofence_sentinel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 🚀 QA Fix: Elevated to IMPORTANCE_HIGH to mathematically guarantee Heads-Up physical drop-down UI!
            val channel = NotificationChannel(geoChannelId, "Geofence Sentinel", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Dependent perimeter transitions"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true) // Required for Heads-Up trigger!
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, geoChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Dependent Perimeter Alert")
            .setContentText(payload)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 🚀 QA Fix: Forces Legacy OS to execute Heads-Up drop-down!
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // 🚀 QA Fix: Force Sound and Vibrate synchronously!
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        SystemLogger.i(TAG, "GEOFENCE_UI_ALERT_POSTED: $payload")
    }
}
