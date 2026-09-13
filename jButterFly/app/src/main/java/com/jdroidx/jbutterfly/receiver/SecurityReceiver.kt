package com.jdroidx.jbutterfly.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jdroidx.jbutterfly.service.TrackingService
import com.jdroidx.jbutterfly.utils.SystemLogger

/**
 * SecurityReceiver — Listens to OS hardware state transitions (Reboot, Battery, Airplane Mode).
 */
class SecurityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        SystemLogger.i("SecurityReceiver", "Captured hardware/OS transition: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                SystemLogger.i("SecurityReceiver", "Device boot or app updated: Auto-starting jButterFly Sentinel.")
                TrackingService.startTracking(context)
            }
            Intent.ACTION_BATTERY_LOW -> {
                SystemLogger.w("SecurityReceiver", "BATTERY_LOW warning received.")
            }
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val state = intent.getBooleanExtra("state", false)
                SystemLogger.w("SecurityReceiver", "Airplane mode changed: active=$state")
            }
        }
    }
}
