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
        
        // 🛡️ DECOMMISSION GATE: If device was decommissioned by admin, suppress resurrection
        try {
            val regPrefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            if (regPrefs.getBoolean("is_decommissioned_by_admin", false)) {
                SystemLogger.w(TAG, "Device is marked as DECOMMISSIONED by Admin. Watchdog skipping resurrection.")
                return
            }
        } catch (_: Exception) { }

        val isRunning = isTrackingServiceRunning(context)
        if (!isRunning) {
            SystemLogger.w(TAG, "TrackingService was found DEAD. Triggering autonomous stealth pulse & reviving service...")
            // 🛡️ PRIME OBJECTIVE: Even when app is off or killed, execute autonomous cellular & GPS pulse immediately
            try {
                com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.executeStealthPulseWithWakeLock(context) {}
            } catch (stealthEx: Exception) {
                SystemLogger.e(TAG, "Watchdog stealth pulse fallback error: ${stealthEx.message}")
            }

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
                // Fallback: ensure stealth pulse schedule remains intact even if background FGS start is restricted
                com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.scheduleNextStealthPulse(context)
            }
        } else {
            SystemLogger.i(TAG, "TrackingService is healthy and active.")
        }

        // [Variables] intervalMins: Long, isWatchdogActive: Boolean | [Constants] PREF_WATCHDOG_INTERVAL, PREF_WATCHDOG_ACTIVE | [Dependencies] SharedPreferences, BackgroundJobManager | [Integration Points] Re-arms autonomous watchdog alarm
        try {
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val isWatchdogActive = prefs.getBoolean(com.jdroidx.jfindme.service.BackgroundJobManager.PREF_WATCHDOG_ACTIVE, true)
            val intervalMins = prefs.getLong(com.jdroidx.jfindme.service.BackgroundJobManager.PREF_WATCHDOG_INTERVAL, com.jdroidx.jfindme.service.BackgroundJobManager.DEFAULT_WATCHDOG_MINS)
            if (isWatchdogActive && intervalMins > 0L) {
                // 🔋 ZERO-CHURN ALARM RE-ARM: Re-arm AlarmManager alarm without churning WorkManager periodic queue
                com.jdroidx.jfindme.service.BackgroundJobManager.scheduleWatchdogAlarm(context, intervalMins)
            }
        } catch (_: Exception) { }
    }
}
