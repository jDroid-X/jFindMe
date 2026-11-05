package com.jdroidx.jfindme.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jdroidx.jfindme.service.StealthAlarmSentinelEngine
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * ⏰ StealthAlarmReceiver: System-Level BroadcastReceiver for Scheduled Alarm Clock Pulses
 *
 * Catches ACTION_STEALTH_PULSE fired by AlarmManager.setAlarmClock() and executes
 * the autonomous 90-second background telemetry pulse.
 */
class StealthAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STEALTH_PULSE = "com.jdroidx.jfindme.ACTION_STEALTH_PULSE"
        private const val TAG = "StealthAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            ACTION_STEALTH_PULSE -> {
                SystemLogger.i(TAG, "⏰ ACTION_STEALTH_PULSE received! Initiating stealth telemetry cycle.")
                val pendingResult = goAsync()
                StealthAlarmSentinelEngine.executeStealthPulseWithWakeLock(context) {
                    try {
                        pendingResult.finish()
                    } catch (e: Exception) {
                        SystemLogger.w(TAG, "Error finishing async broadcast: ${e.message}")
                    }
                }
            }
            "com.jdroidx.jfindme.action.MAINTENANCE_WAKEUP" -> {
                SystemLogger.i(TAG, "⏰ MAINTENANCE_WAKEUP received! Punching Doze for 02:39 AM maintenance window.")
                com.jdroidx.jfindme.utils.SystemUpdateScheduler.triggerImmediateMaintenance(context)
            }
        }
    }
}

