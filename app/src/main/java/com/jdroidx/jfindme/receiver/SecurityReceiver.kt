package com.jdroidx.jfindme.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jdroidx.jfindme.engine.security.SecurityEngine
import com.jdroidx.jfindme.service.TrackingService
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * SecurityReceiver: Monitors system-level security events.
 * Rule 5: Heartbeat and Reboot detection.
 */
class SecurityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        SystemLogger.i(TAG, "Security Event Received: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                SystemLogger.w(TAG, "DEVICE_REBOOTED_ALERT: Restarting Tracking Service")
                startTrackingService(context)
            }
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val isOn = SecurityEngine.isAirplaneModeOn(context)
                if (isOn) {
                    SystemLogger.e(TAG, "OFFLINE_ALERT: Airplane Mode Enabled. Tracking impacted.")
                } else {
                    SystemLogger.i(TAG, "ONLINE_RESTORED: Airplane Mode Disabled.")
                }
            }
        }
    }

    private fun startTrackingService(context: Context) {
        val serviceIntent = Intent(context, TrackingService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        private const val TAG = "SecurityReceiver"
    }
}
