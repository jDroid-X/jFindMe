package com.jdroidx.jfindme.engine.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
    // 🛡️ FIX-2: Use application context to avoid Activity leak; hold as instance field for lifecycle sync
    private var applicationContext: android.content.Context? = null

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

            // 🛡️ FIX-2: Store app context reference for safe MediaPlayer ownership transfer
            val appCtx = applicationContext ?: context.applicationContext
            applicationContext = appCtx

            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(appCtx, alertUri)
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                // 🛡️ FIX-2: Use onPrepared listener instead of blocking prepare() — prevents
                // IllegalStateException when called off the main thread
                setOnPreparedListener { start() }
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
            applicationContext = null
            SystemLogger.i(TAG, "ACOUSTIC_RING_BEACON_STOPPED")
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error stopping ring beacon", e)
        }
    }

    /** 🛡️ FIX-2: Release MediaPlayer if alarm is playing during app shutdown */
    @JvmStatic
    @Synchronized
    fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
            applicationContext = null
            SystemLogger.i(TAG, "ACOUSTIC_MEDIA_PLAYER_RELEASED_SHUTDOWN")
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error releasing media player", e)
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

    // [Variables] notificationManager, channelId, notificationId, now, timeStr, dateStr, notification
    // [Constants] None | [Dependencies] NotificationCompat, NotificationManager, SystemLogger
    // [Integration Points] TrackingService, FCMService, DeviceSharingHub
    @JvmStatic
    fun showDeviceSharingNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "device_sharing_sync"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Device Sharing & Mesh Sync", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Mutual device pairing and mesh roster synchronization alerts"
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val now = System.currentTimeMillis()
        val timeFormat = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        val timeStr = timeFormat.format(java.util.Date(now))
        val dateStr = dateFormat.format(java.util.Date(now))

        val intent = android.content.Intent().apply {
            setClassName(context, "com.jdroidx.jfindme.ui.group.FamilyFriendsActivity")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            (now % 10000).toInt(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            else
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📱 $title • $timeStr")
            .setSubText(dateStr)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\n🕒 Date: $dateStr | Time: $timeStr"))
            .setContentIntent(pendingIntent)
            .setShowWhen(true)
            .setWhen(now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((now % 10000).toInt(), notification)
        triggerGeofenceHapticAlert(context)
        try {
            val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).writableDatabase
            com.jdroidx.jfindme.data.local.tables.AlertHistoryTable.logAlert(
                db,
                agentId = "DEVICE_SHARING",
                alertType = "DEVICE_PAIR_LINK",
                details = "$title: $message",
                timestamp = now,
                severity = "INFO"
            )
        } catch (ignored: Exception) {}
        SystemLogger.i(TAG, "DEVICE_SHARING_NOTIFICATION_POSTED: $title - $message")
    }

    @JvmStatic
    fun showButterflyInvitationNotification(
        context: Context,
        pointId: String,
        pointName: String,
        creatorName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "butterfly_point_invitations"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Butterfly Point Rendezvous", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tactical Butterfly Point Rendezvous Invitations"
                enableLights(true)
                lightColor = android.graphics.Color.MAGENTA
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = (System.currentTimeMillis() % 10000).toInt()

        // [Variables] contentIntent, pContentIntent | [Constants] EXTRA_FOCUS_BUTTERFLY_POINT_ID | [Dependencies] Intent, PendingIntent | [Integration Points] MapActivity decoupling
        // Content intent: Opens MapActivity focused on the Butterfly Point (Decoupled component binding)
        val contentIntent = Intent().apply {
            setClassName(context, "com.jdroidx.jfindme.ui.map.MapActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_FOCUS_BUTTERFLY_POINT_ID", pointId)
        }
        val pContentIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        // [Variables] acceptIntent, rejectIntent, pAcceptIntent, pRejectIntent
        // [Constants] ACTION_BUTTERFLY_ACCEPT, ACTION_BUTTERFLY_REJECT
        // [Dependencies] Intent, PendingIntent, ButterflyPointActionReceiver
        // [Integration Points] Butterfly Point rendezvous push notification interactive response buttons
        // Action Intent: ACCEPT (Explicit component target prevents OS implicit broadcast drop on exported=false)
        val acceptIntent = Intent("com.jdroidx.jfindme.ACTION_BUTTERFLY_ACCEPT").apply {
            setClass(context, com.jdroidx.jfindme.receiver.ButterflyPointActionReceiver::class.java)
            setPackage(context.packageName)
            putExtra("point_id", pointId)
            putExtra("notification_id", notificationId)
        }
        val pAcceptIntent = android.app.PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            acceptIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action Intent: REJECT (Explicit component target prevents OS implicit broadcast drop on exported=false)
        val rejectIntent = Intent("com.jdroidx.jfindme.ACTION_BUTTERFLY_REJECT").apply {
            setClass(context, com.jdroidx.jfindme.receiver.ButterflyPointActionReceiver::class.java)
            setPackage(context.packageName)
            putExtra("point_id", pointId)
            putExtra("notification_id", notificationId)
        }
        val pRejectIntent = android.app.PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            rejectIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("🦋 Butterfly Rendezvous: $pointName")
            .setContentText("Invited by $creatorName. Tap to open tactical map.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Rendezvous invitation for '$pointName' initiated by $creatorName. Select Accept to participate or Reject to decline."))
            .setContentIntent(pContentIntent)
            .addAction(android.R.drawable.checkbox_on_background, "ACCEPT", pAcceptIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "REJECT", pRejectIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        triggerGeofenceHapticAlert(context)
        SystemLogger.i(TAG, "BUTTERFLY_INVITATION_NOTIFICATION_POSTED: id=$pointId by=$creatorName")
    }

    // [Variables] notificationManager: NotificationManager, channelId: String, notificationId: Int, contentIntent: Intent, pContentIntent: PendingIntent, notification: Notification
    // [Constants] None
    // [Dependencies] NotificationCompat, NotificationManager, Intent, PendingIntent, SystemLogger
    // [Integration Points] TrackingService, ButterflyPointEngine, MapActivity
    @JvmStatic
    fun showButterflyArrivalNotification(
        context: Context,
        pointName: String,
        deviceName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "butterfly_point_invitations"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Butterfly Point Rendezvous", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tactical Butterfly Point Rendezvous Notifications"
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 88000 + (System.currentTimeMillis() % 1000).toInt()
        // [Variables] contentIntent, pContentIntent | [Constants] FLAGS | [Dependencies] Intent, PendingIntent | [Integration Points] MapActivity decoupling
        val contentIntent = Intent().apply {
            setClassName(context, "com.jdroidx.jfindme.ui.map.MapActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pContentIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("🎯 Rendezvous Reached: $pointName")
            .setContentText("$deviceName arrived at Butterfly Point '$pointName'.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("🎯 Arrival Confirmed: $deviceName has reached the rendezvous perimeter for '$pointName'."))
            .setContentIntent(pContentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        triggerGeofenceHapticAlert(context)
        SystemLogger.i(TAG, "BUTTERFLY_ARRIVAL_NOTIFICATION_POSTED: $pointName by $deviceName")
    }

    @JvmStatic
    fun showButterflyNudgeNotification(
        context: Context,
        pointName: String,
        message: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "butterfly_point_invitations"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Butterfly Point Rendezvous", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tactical Butterfly Point Rendezvous Notifications"
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 89000 + (System.currentTimeMillis() % 1000).toInt()
        val contentIntent = Intent().apply {
            setClassName(context, "com.jdroidx.jfindme.ui.map.MapActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pContentIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("🦋 Rendezvous Nudge: $pointName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pContentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        triggerGeofenceHapticAlert(context)
        SystemLogger.i(TAG, "BUTTERFLY_NUDGE_NOTIFICATION_POSTED: $pointName")
    }

    // [Variables] notificationManager, channelId, notificationId, contentIntent, pContentIntent, message, notification
    // [Constants] TAG
    // [Dependencies] NotificationCompat, NotificationManager, PendingIntent, SystemLogger
    // [Integration Points] TrackingService.kt (onChildChanged), MapActivity
    @JvmStatic
    fun showButterflyTimeExtensionNotification(
        context: Context,
        pointName: String,
        newTimeStr: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "butterfly_point_invitations"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Butterfly Point Rendezvous", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tactical Butterfly Point Rendezvous Notifications"
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 89200 + (System.currentTimeMillis() % 1000).toInt()
        val contentIntent = Intent().apply {
            setClassName(context, "com.jdroidx.jfindme.ui.map.MapActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pContentIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val message = "Meeting time extended to $newTimeStr"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("⏰ Rendezvous Extended: $pointName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pContentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        triggerGeofenceHapticAlert(context)
        SystemLogger.i(TAG, "BUTTERFLY_EXTENSION_NOTIFICATION_POSTED: $pointName -> $newTimeStr")
    }

    // [Variables] notificationManager, channelId, notificationId, contentIntent, pContentIntent, message, notification
    // [Constants] TAG
    // [Dependencies] NotificationCompat, NotificationManager, PendingIntent, SystemLogger
    // [Integration Points] ButterflyPointEngine.kt (startActivePointPeerSync), MapActivity
    @JvmStatic
    fun showButterflyQuorumNotification(
        context: Context,
        pointName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "butterfly_point_invitations"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Butterfly Point Rendezvous", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tactical Butterfly Point Rendezvous Notifications"
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 89400 + (System.currentTimeMillis() % 1000).toInt()
        val contentIntent = Intent().apply {
            setClassName(context, "com.jdroidx.jfindme.ui.map.MapActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pContentIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val message = "All group members have successfully arrived at $pointName!"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("🎉 Full Quorum Assembled: $pointName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pContentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        triggerGeofenceHapticAlert(context)
        SystemLogger.i(TAG, "BUTTERFLY_QUORUM_NOTIFICATION_POSTED: $pointName")
    }

    @JvmStatic
    fun cancelButterflyNotification(context: Context, notificationId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(notificationId)
    }
}
