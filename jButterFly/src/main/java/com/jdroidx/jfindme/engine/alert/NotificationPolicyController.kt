package com.jdroidx.jfindme.engine.alert

import android.content.Context
import android.content.SharedPreferences
import com.jdroidx.jfindme.data.model.NotificationPolicyModel

/**
 * 🎮 NotificationPolicyController: OOPS MVC Controller managing Notification Policies.
 *
 * Encapsulates SharedPreferences read/write operations and provides clean Boolean gates:
 * - isAuthSecurityAllowed()
 * - isDeviceSharingAllowed()
 * - isGeofenceAllowed()
 * - isButterflyAllowed()
 * - isBatteryAllowed()
 * - isSystemUpdateAllowed()
 * - isSilentForegroundService()
 */
class NotificationPolicyController(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)

    companion object {
        const val PREF_NOTIF_AUTH_SECURITY = "pref_notif_auth_security"
        const val PREF_NOTIF_DEVICE_SHARING = "pref_notif_device_sharing"
        const val PREF_NOTIF_GEOFENCE = "pref_notif_geofence"
        const val PREF_NOTIF_BUTTERFLY = "pref_notif_butterfly"
        const val PREF_NOTIF_BATTERY = "pref_notif_battery"
        const val PREF_NOTIF_SYSTEM_UPDATES = "pref_notif_system_updates"
        const val PREF_NOTIF_FORENSIC = "pref_notif_forensic"
        const val PREF_NOTIF_SILENT_FOREGROUND = "pref_notif_silent_foreground"
        const val ACTION_NOTIFICATION_POLICY_CHANGED = "com.jdroidx.jfindme.ACTION_NOTIFICATION_POLICY_CHANGED"

        @Volatile
        private var instance: NotificationPolicyController? = null

        @JvmStatic
        fun getInstance(context: Context): NotificationPolicyController {
            return instance ?: synchronized(this) {
                instance ?: NotificationPolicyController(context.applicationContext).also { instance = it }
            }
        }
    }

    fun loadPolicy(): NotificationPolicyModel {
        return NotificationPolicyModel(
            authSecurityEnabled = prefs.getBoolean(PREF_NOTIF_AUTH_SECURITY, true), // Mandatory
            deviceSharingEnabled = prefs.getBoolean(PREF_NOTIF_DEVICE_SHARING, false), // Silent by default
            geofenceAlertsEnabled = prefs.getBoolean(PREF_NOTIF_GEOFENCE, false), // Silent by default
            butterflyPointEnabled = prefs.getBoolean(PREF_NOTIF_BUTTERFLY, false), // Silent by default
            batteryAlertsEnabled = prefs.getBoolean(PREF_NOTIF_BATTERY, false), // Silent by default
            systemUpdatesEnabled = prefs.getBoolean(PREF_NOTIF_SYSTEM_UPDATES, false), // Silent by default
            forensicDiagnosticsEnabled = prefs.getBoolean(PREF_NOTIF_FORENSIC, false), // Silent by default
            silentForegroundService = prefs.getBoolean(PREF_NOTIF_SILENT_FOREGROUND, true)
        )
    }

    fun savePolicy(policy: NotificationPolicyModel) {
        prefs.edit().apply {
            putBoolean(PREF_NOTIF_AUTH_SECURITY, policy.authSecurityEnabled)
            putBoolean(PREF_NOTIF_DEVICE_SHARING, policy.deviceSharingEnabled)
            putBoolean(PREF_NOTIF_GEOFENCE, policy.geofenceAlertsEnabled)
            putBoolean(PREF_NOTIF_BUTTERFLY, policy.butterflyPointEnabled)
            putBoolean(PREF_NOTIF_BATTERY, policy.batteryAlertsEnabled)
            putBoolean(PREF_NOTIF_SYSTEM_UPDATES, policy.systemUpdatesEnabled)
            putBoolean(PREF_NOTIF_FORENSIC, policy.forensicDiagnosticsEnabled)
            putBoolean(PREF_NOTIF_SILENT_FOREGROUND, policy.silentForegroundService)
            apply()
        }
    }

    fun setSetting(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
        dispatchPolicyChangedBroadcast(key, enabled)
    }

    private fun dispatchPolicyChangedBroadcast(key: String, enabled: Boolean) {
        try {
            val intent = android.content.Intent(ACTION_NOTIFICATION_POLICY_CHANGED).apply {
                setPackage(context.packageName)
                putExtra("policy_key", key)
                putExtra("policy_value", enabled)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }

    fun isAuthSecurityAllowed(): Boolean = prefs.getBoolean(PREF_NOTIF_AUTH_SECURITY, true)
    fun isDeviceSharingAllowed(): Boolean = prefs.getBoolean(PREF_NOTIF_DEVICE_SHARING, false)
    fun isGeofenceAllowed(): Boolean = prefs.getBoolean(PREF_NOTIF_GEOFENCE, false)
    fun isButterflyAllowed(): Boolean = prefs.getBoolean(PREF_NOTIF_BUTTERFLY, false)
    fun isBatteryAllowed(): Boolean = prefs.getBoolean(PREF_NOTIF_BATTERY, false)
    fun isSystemUpdateAllowed(): Boolean = prefs.getBoolean(PREF_NOTIF_SYSTEM_UPDATES, false)
    fun isForensicAllowed(): Boolean = prefs.getBoolean(PREF_NOTIF_FORENSIC, false)
    fun isSilentForeground(): Boolean = prefs.getBoolean(PREF_NOTIF_SILENT_FOREGROUND, true)
}
