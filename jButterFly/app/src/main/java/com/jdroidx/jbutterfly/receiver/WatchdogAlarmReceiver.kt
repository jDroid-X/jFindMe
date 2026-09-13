package com.jdroidx.jbutterfly.receiver

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jdroidx.jbutterfly.service.TrackingService
import com.jdroidx.jbutterfly.utils.SystemLogger

/**
 * WatchdogAlarmReceiver — Wakes up periodically to ensure TrackingService is running.
 */
class WatchdogAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        SystemLogger.i("WatchdogAlarmReceiver", "Watchdog heartbeat triggered. Checking service health.")

        if (!isServiceRunning(context, TrackingService::class.java)) {
            SystemLogger.w("WatchdogAlarmReceiver", "TrackingService was found dead! Resurrecting...")
            TrackingService.startTracking(context)
        } else {
            SystemLogger.d("WatchdogAlarmReceiver", "TrackingService is running normally.")
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
