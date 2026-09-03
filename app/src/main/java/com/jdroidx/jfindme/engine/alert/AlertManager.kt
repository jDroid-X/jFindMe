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
    @JvmOverloads
    fun vibrate(context: Context, durationMs: Long = 300) {
        try {
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val hapticEnabled = prefs.getBoolean("pref_haptic_feedback", true)
            if (!hapticEnabled) return

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) { }
    }

    @JvmStatic
    fun triggerLocalSOSFeedback(context: Context) {
        try {
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val hapticEnabled = prefs.getBoolean("pref_haptic_feedback", true)
            if (!hapticEnabled) return

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
        } catch (e: Exception) { }
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

    private const val CHANNEL_BATTERY_ID = "battery_alerts"
    private const val NOTIFICATION_BATTERY_ID = 912
    private var mediaPlayer: android.media.MediaPlayer? = null

    @JvmStatic
    @Synchronized
    fun playRingBeaconAlarm(context: Context, durationSeconds: Int = 30) {
        try {
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val audioEnabled = prefs.getBoolean("pref_audio_alerts", true)
            if (!audioEnabled) {
                // If audio disabled, only trigger haptic if enabled
                triggerLocalSOSFeedback(context)
                return
            }

            stopRingBeaconAlarm()
            val alertUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            // Vibrate synchronously with pattern
            triggerLocalSOSFeedback(context)

            // Auto stop after duration
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                stopRingBeaconAlarm()
            }, durationSeconds * 1000L)

            SystemLogger.w(TAG, "ACOUSTIC_RING_BEACON_ACTIVE: Playing alarm for ${durationSeconds}s")
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to play acoustic beacon", e)
        }
    }

    @JvmStatic
    @Synchronized
    fun stopRingBeaconAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            SystemLogger.i(TAG, "ACOUSTIC_RING_BEACON_STOPPED")
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error stopping ring beacon", e)
        }
    }

    @JvmStatic
    fun showLowBatteryNotification(context: Context, deviceName: String, batteryPct: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_BATTERY_ID, "Battery Sentinel", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Monitored device battery level alerts"
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BATTERY_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentTitle("⚠️ Low Battery Alert: $deviceName")
            .setContentText("Device battery has dropped to $batteryPct%. Remind them to charge soon.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_BATTERY_ID, notification)
        SystemLogger.w(TAG, "LOW_BATTERY_ALERT_POSTED for $deviceName ($batteryPct%)")
    }

    @JvmStatic
    fun triggerGeofenceHapticAlert(context: Context) {
        try {
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val hapticMaster = prefs.getBoolean("pref_haptic_feedback", true)
            if (!hapticMaster) return

            val is10Sec = prefs.getBoolean("pref_geofence_persistent_haptic", false)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (is10Sec) {
                // 🚀 Continuous 10-Second Alert Pattern (10 pulses: 800ms buzz + 200ms pause)
                val pattern = LongArray(20)
                for (i in 0 until 10) {
                    pattern[i * 2] = 200L     // pause
                    pattern[i * 2 + 1] = 800L // vibration buzz
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
                SystemLogger.w(TAG, "GEOFENCE_10SEC_HAPTIC: 10-second continuous vibration alert triggered.")
            } else {
                // One-time only quick alert (500ms)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(500L)
                }
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Geofence haptic alert error", e)
        }
    }

    @JvmStatic
    fun showGeofenceNotification(context: Context, zoneName: String, transitionName: String, userName: String) {
        val nowTime = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
        val nowDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val notificationMessage = "$userName has $transitionName the '$zoneName' Safe Zone.\nDate: $nowDate • Time: $nowTime"
        showGeofenceNotification(context, notificationMessage)
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

        val now = System.currentTimeMillis()
        val timeFormat = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        val timeStr = timeFormat.format(java.util.Date(now))
        val dateStr = dateFormat.format(java.util.Date(now))

        val notification = NotificationCompat.Builder(context, geoChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Perimeter Alert • $timeStr")
            .setSubText(dateStr)
            .setContentText(payload)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$payload\n\n🕒 Date: $dateStr | Time: $timeStr"))
            .setShowWhen(true)
            .setWhen(now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((now % 10000).toInt(), notification)
        triggerGeofenceHapticAlert(context)
        SystemLogger.i(TAG, "GEOFENCE_UI_ALERT_POSTED: $payload [$dateStr $timeStr]")
    }
}
