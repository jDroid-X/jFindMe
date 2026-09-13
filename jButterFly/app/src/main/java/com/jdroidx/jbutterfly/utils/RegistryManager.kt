package com.jdroidx.jbutterfly.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * RegistryManager — Centralized Dynamic Configuration and Preferences Hub.
 * Ensures zero hardcoded operational values. All parameters are dynamically configurable.
 */
object RegistryManager {

    private const val PREFS_NAME = "jbutterfly_registry"

    // Keys - Identity & Profile
    const val KEY_AGENT_ID = "agent_id"
    const val KEY_HARDWARE_KEY = "hardware_key"
    const val KEY_USER_NICKNAME = "user_nickname"
    const val KEY_USER_ROLE = "user_role" // GUARDIAN / CHILD / FRIEND
    const val KEY_EQUIPPED_AVATAR = "equipped_avatar"
    const val KEY_IS_LOGGED_IN = "is_logged_in"

    // Keys - Dynamic Tracking & Telemetry Parameters
    const val KEY_POLL_INTERVAL_MOVING = "poll_interval_moving_ms"
    const val KEY_POLL_INTERVAL_STATIONARY = "poll_interval_stationary_ms"
    const val KEY_FASTEST_INTERVAL = "fastest_interval_ms"
    const val KEY_ACCURACY_THRESHOLD = "accuracy_threshold_meters"
    const val KEY_GEOFENCE_DEFAULT_RADIUS = "geofence_default_radius_meters"
    const val KEY_BATTERY_ALERT_THRESHOLD = "battery_alert_threshold_pct"
    const val KEY_BUZZER_DURATION_SEC = "buzzer_duration_sec"
    const val KEY_MAP_DEFAULT_ZOOM = "map_default_zoom"
    const val KEY_FOLLOW_ME_ENABLED = "follow_me_enabled"

    // Keys - Appearance & Settings
    const val KEY_THEME_MODE = "theme_mode" // SYSTEM, LIGHT, DARK
    const val KEY_COLOR_SCHEME = "color_scheme" // 1..7

    // Default Dynamic Values
    const val DEFAULT_POLL_MOVING_MS = 10000L // 10 seconds
    const val DEFAULT_POLL_STATIONARY_MS = 60000L // 60 seconds
    const val DEFAULT_FASTEST_MS = 5000L // 5 seconds
    const val DEFAULT_ACCURACY_METERS = 25.0f
    const val DEFAULT_GEOFENCE_RADIUS = 200.0f // 200 meters
    const val DEFAULT_BATTERY_ALERT = 20 // 20%
    const val DEFAULT_BUZZER_DURATION = 30 // 30 seconds
    const val DEFAULT_MAP_ZOOM = 16.0f

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getString(context: Context, key: String, defValue: String = ""): String {
        return getPrefs(context).getString(key, defValue) ?: defValue
    }

    @JvmStatic
    fun putString(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    @JvmStatic
    fun getInt(context: Context, key: String, defValue: Int): Int {
        return getPrefs(context).getInt(key, defValue)
    }

    @JvmStatic
    fun putInt(context: Context, key: String, value: Int) {
        getPrefs(context).edit().putInt(key, value).apply()
    }

    @JvmStatic
    fun getLong(context: Context, key: String, defValue: Long): Long {
        return getPrefs(context).getLong(key, defValue)
    }

    @JvmStatic
    fun putLong(context: Context, key: String, value: Long) {
        getPrefs(context).edit().putLong(key, value).apply()
    }

    @JvmStatic
    fun getFloat(context: Context, key: String, defValue: Float): Float {
        return getPrefs(context).getFloat(key, defValue)
    }

    @JvmStatic
    fun putFloat(context: Context, key: String, value: Float) {
        getPrefs(context).edit().putFloat(key, value).apply()
    }

    @JvmStatic
    fun getBoolean(context: Context, key: String, defValue: Boolean): Boolean {
        return getPrefs(context).getBoolean(key, defValue)
    }

    @JvmStatic
    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }
}
