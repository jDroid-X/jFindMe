package com.jdroidx.jfindme.receiver

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.jdroidx.jfindme.service.TrackingService
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 🐕 WatchdogAlarmReceiver: Self-Healing Sentinel for TrackingService
 * Prevents silent terminations from OEM battery killers and Doze mode restrictions.
 */
class WatchdogAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WatchdogAlarmReceiver"
        const val ACTION_WATCHDOG_HEARTBEAT = "com.jdroidx.jfindme.action.WATCHDOG_HEARTBEAT"

        @JvmStatic
        fun isTrackingServiceRunning(context: Context): Boolean {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            @Suppress("DEPRECATION")
            val runningServices = am.getRunningServices(Int.MAX_VALUE)
            for (service in runningServices) {
                if (TrackingService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        SystemLogger.i(TAG, "Watchdog heartbeat check triggered.")
        
        val isRunning = isTrackingServiceRunning(context)
        if (!isRunning) {
            SystemLogger.w(TAG, "TrackingService was found DEAD. Reviving service immediately...")
            val serviceIntent = Intent(context, TrackingService::class.java).apply {
                action = TrackingService.ACTION_START_TRACKING
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                SystemLogger.i(TAG, "TrackingService successfully revived by Watchdog.")
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Watchdog failed to revive service: ${e.message}")
            }
        } else {
            SystemLogger.i(TAG, "TrackingService is healthy and active.")
        }
    }
}
