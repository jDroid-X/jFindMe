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
                SystemLogger.w(TAG, "DEVICE_REBOOTED_ALERT: Restarting Tracking Service & Re-registering Geofences")
                startTrackingService(context)
                restoreHardwareGeofences(context)
                com.jdroidx.jfindme.utils.SystemUpdateScheduler.scheduleMaintenanceWindow(context)
                com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.scheduleNextStealthPulse(context)
            }
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val isOn = SecurityEngine.isAirplaneModeOn(context)
                if (isOn) {
                    SystemLogger.e(TAG, "OFFLINE_ALERT: Airplane Mode Enabled. Tracking impacted.")
                    com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.dispatchAlert(
                        context,
                        com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.TYPE_AIRPLANE_MODE,
                        "Airplane Mode was enabled. Network connection lost.",
                        "HIGH"
                    )
                } else {
                    SystemLogger.i(TAG, "ONLINE_RESTORED: Airplane Mode Disabled.")
                }
            }
            Intent.ACTION_SHUTDOWN -> {
                SystemLogger.w(TAG, "DEVICE_SHUTDOWN: Last-gasp alert dispatching.")
                com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.dispatchAlert(
                    context,
                    com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.TYPE_DEVICE_SHUTDOWN,
                    "Device is powering off / shutting down.",
                    "HIGH"
                )
            }
            android.location.LocationManager.PROVIDERS_CHANGED_ACTION -> {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                val isGpsEnabled = lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ?: false
                if (!isGpsEnabled) {
                    SystemLogger.e(TAG, "GPS_DISABLED_ALERT: Location service was disabled.")
                    com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.dispatchAlert(
                        context,
                        com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.TYPE_GPS_DISABLED,
                        "Location / GPS was disabled on this device. Switched to SIM Tower Tracking.",
                        "CRITICAL"
                    )
                    // 🛡️ Trigger fast 1-min Stealth SIM failover pulse
                    try {
                        com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.scheduleNextStealthPulse(context, 1L)
                    } catch (_: Exception) {}
                }
            }
            Intent.ACTION_BATTERY_LOW -> {
                SystemLogger.w(TAG, "BATTERY_LOW_ALERT: Battery level dropped below threshold.")
                com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.dispatchAlert(
                    context,
                    com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.TYPE_BATTERY_CRITICAL,
                    "Device battery level is critically low (<15%).",
                    "MEDIUM"
                )
            }
        }
    }

    private fun startTrackingService(context: Context) {
        try {
            val serviceIntent = Intent(context, TrackingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "FAILED_TO_START_TRACKING_SERVICE_ON_BOOT", e)
        }
    }

    private fun restoreHardwareGeofences(context: Context) {
        try {
            val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val zones = db.getOfflineSafeZones()
            val helper = com.jdroidx.jfindme.engine.location.GeofenceHelper(context)
            var count = 0
            for (zone in zones) {
                val isActive = (zone["flag_b"] as? Int ?: 1) == 1
                if (isActive) {
                    val zoneId = zone["zone_id"] as? String ?: continue
                    val lat = zone["lat"] as? Double ?: continue
                    val lng = zone["lng"] as? Double ?: continue
                    val radius = zone["radius"] as? Double ?: 100.0
                    helper.addGeofence(zoneId, lat, lng, radius.toFloat())
                    count++
                }
            }
            SystemLogger.i(TAG, "BOOT_GEOFENCES_RESTORED: Successfully re-registered $count active safe zones.")
        } catch (e: Exception) {
            SystemLogger.e(TAG, "BOOT_GEOFENCES_RESTORE_ERROR", e)
        }
    }

    companion object {
        private const val TAG = "SecurityReceiver"
    }
}
