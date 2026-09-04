package com.jdroidx.jfindme.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RegistryManager — CENTRALIZED SharedPreferences access for "system_registry".
 *
 * AUDIT FIX [AUD-015]: `getSharedPreferences("system_registry", MODE_PRIVATE)` was
 * called at 20+ locations. This singleton provides typed, documented access to every
 * registry key used across the application.
 *
 * Architecture: utils layer — no Firebase, no UI dependencies.
 * Thread Safety: SharedPreferences reads are thread-safe. Writes use .apply() (async).
 */
object RegistryManager {

    private const val PREFS_NAME = "system_registry"

    // ─── KEY CONSTANTS ────────────────────────────────────────────────────────

    // Identity & Session
    const val KEY_AGENT_ID           = "agent_id"
    const val KEY_USER_NICKNAME      = "user_nickname"
    const val KEY_USER_GENDER        = "user_gender"
    const val KEY_AUTO_LOGIN         = "auto_login_enabled"
    const val KEY_PROFILE_BYPASS     = "profile_bypass_nudge"

    // UI Configuration
    const val KEY_THEME_ID           = "theme_id"
    const val KEY_UI_CONFIG_HASH     = "ui_config_hash"
    const val KEY_STAGE_OVERRIDE     = "stage_override"
    const val KEY_CUSTOM_WIDGET_TINT = "custom_widget_tint"
    const val KEY_DEVELOPER_LOGGING  = "developer_logging_enabled"

    // Tracking & Service
    const val KEY_TRACKING_INTERVAL  = "pref_tracking_interval"
    const val KEY_SERVICE_HALTED     = "pref_service_halted"
    const val KEY_ALLOW_DEPENDENTS   = "allow_dependents"
    const val KEY_ACTIVE_GROUP       = "active_group"
    const val KEY_EQUIPPED_AVATAR_ID = "equipped_avatar_id"

    // Privacy & ACL
    const val KEY_ACL_LIVE_TRACKING  = "acl_live_tracking"
    const val KEY_BIOMETRIC_LOCK     = "biometric_lock_enabled"
    const val KEY_CHILD_WARD_MODE    = "child_ward_mode_active"
    const val KEY_GUARDIAN_PIN       = "guardian_pin_hash"

    // QA / Debug
    const val KEY_MOCK_DEVICE_UUID   = "mock_device_uuid"

    // ─── DYNAMIC CONFIGURATION & DEFAULTS ─────────────────────────────────────
    const val KEY_DEFAULT_LAT        = "pref_default_latitude"
    const val KEY_DEFAULT_LNG        = "pref_default_longitude"
    const val KEY_DEFAULT_ZOOM       = "pref_default_zoom"
    const val KEY_LAST_KNOWN_LAT     = "last_known_latitude"
    const val KEY_LAST_KNOWN_LNG     = "last_known_longitude"
    const val KEY_ENTERPRISE_DOMAIN  = "pref_enterprise_domain"
    const val KEY_SUPPORT_EMAIL      = "pref_support_email"
    const val KEY_SUPPORT_PHONE      = "pref_support_phone"
    const val KEY_PAIRING_TIMEOUT_MS = "pref_pairing_timeout_ms"
    const val KEY_GEOCODE_DEBOUNCE_MS= "pref_geocode_debounce_ms"
    const val KEY_HW_PREFIX          = "pref_hw_prefix"
    const val KEY_AGENT_PREFIX       = "pref_agent_prefix"
    const val KEY_DEFAULT_GEOFENCE_RADIUS = "pref_geofence_radius"
    const val KEY_MIN_GEOFENCE_RADIUS     = "pref_min_geofence_radius"
    const val KEY_MAX_GEOFENCE_RADIUS     = "pref_max_geofence_radius"
    const val KEY_HISTORY_REPLAY_SPEED    = "pref_history_replay_speed"
    const val KEY_EMAIL_REGEX             = "pref_email_regex"
    const val KEY_SYSTEM_UPDATE_LINK      = "pref_system_update_link"
    const val KEY_GITHUB_UPDATE_LINK      = "pref_github_update_link"
    const val KEY_SILENT_UPDATE_ENABLED   = "silent_update_enabled"
    const val KEY_SILENT_UPDATE_LAST_STATUS = "silent_update_last_status"
    const val KEY_SILENT_UPDATE_LAST_TIME   = "silent_update_last_time"
    const val KEY_SILENT_UPDATE_LAST_ERROR  = "silent_update_last_error"
    const val KEY_SILENT_UPDATE_UNACKNOWLEDGED = "silent_update_unacknowledged"
    const val KEY_LAST_UPDATE_TS          = "last_update_ts"
    const val KEY_LAST_DOWNLOAD_TS        = "last_download_ts"
    const val KEY_LAST_CHECK_TS           = "last_check_ts"
    const val KEY_UPDATE_AVAILABLE_TS     = "update_available_ts"
    const val KEY_UPDATE_STATUS_TS        = "update_status_ts"
    const val KEY_IS_UPDATED              = "is_updated"
    const val KEY_PURGE_SAFETY_PHRASE     = "pref_purge_safety_phrase"
    const val KEY_LAST_FAILED_INSTALL_TS    = "last_failed_install_ts"
    const val KEY_LAST_FAILED_INSTALL_EPOCH = "last_failed_install_epoch"
    const val KEY_LAST_FAILED_INSTALL_REASON= "last_failed_install_reason"
    const val KEY_LAST_FAILED_INSTALL_VER   = "last_failed_install_ver"
    const val KEY_CACHED_APK_PATH           = "cached_apk_path"
    const val KEY_CACHED_APK_SIZE           = "cached_apk_size"

    // 🌐 Geofence Sentinel Configuration Keys
    const val KEY_GEOFENCE_JITTER_DEBOUNCE_MS = "pref_geofence_jitter_debounce_ms"
    const val KEY_GEOFENCE_LATCH_ENABLED      = "pref_geofence_latch_enabled"
    const val KEY_GEOFENCE_LOCAL_NOTIF_ENABLED = "pref_geofence_local_notif_enabled"
    const val KEY_GEOFENCE_FCM_RELAY_ENABLED   = "pref_geofence_fcm_relay_enabled"
    const val KEY_GEOFENCE_SNAP_CENTER_ENABLED = "pref_geofence_snap_center_enabled"

    // 📍 Adaptive GPS & Displacement
    const val KEY_MIN_DISPLACEMENT_METERS    = "pref_min_displacement_meters"
    const val KEY_STRICT_ACCURACY_WAIT       = "pref_strict_accuracy_wait"

    // 🚦 RAG Telemetry Thresholds
    const val KEY_LOW_BATTERY_THRESHOLD      = "pref_low_battery_threshold"
    const val KEY_STALE_INACTIVE_MINUTES     = "pref_stale_inactive_minutes"
    const val KEY_CRITICAL_OFFLINE_HOURS     = "pref_critical_offline_hours"

    // 🔊 Emergency Siren Settings
    const val KEY_SIREN_DURATION_SEC         = "pref_siren_duration_sec"
    const val KEY_SIREN_VIBRATION_ENABLED    = "pref_siren_vibration_enabled"
    const val KEY_SIREN_DND_BYPASS_ENABLED   = "pref_siren_dnd_bypass_enabled"

    // 📦 Offline Vault & Sync
    const val KEY_OFFLINE_QUEUE_MAX_POINTS   = "pref_offline_queue_max_points"
    const val KEY_SYNC_WIFI_ONLY             = "pref_sync_wifi_only"

    // Default Fallbacks
    const val DEFAULT_FALLBACK_LAT   = 18.5204
    const val DEFAULT_FALLBACK_LNG   = 73.8567
    const val DEFAULT_FALLBACK_ZOOM  = 15.0f
    const val DEFAULT_ENTERPRISE_DOMAIN = "jfindme.net"
    const val DEFAULT_SUPPORT_EMAIL  = "enterprise-support@jfindme.net"
    const val DEFAULT_SUPPORT_PHONE  = "1-800-FINDME-CORP"
    const val DEFAULT_PAIRING_TIMEOUT= 300000L // 5 mins
    const val DEFAULT_GEOCODE_DEBOUNCE = 5000L
    const val DEFAULT_HW_PREFIX      = "HW_"
    const val DEFAULT_AGENT_PREFIX   = "Agent_"
    const val DEFAULT_GEOFENCE_RADIUS= 200.0f
    const val DEFAULT_MIN_GEOFENCE_RADIUS = 50.0f
    const val DEFAULT_MAX_GEOFENCE_RADIUS = 5000.0f
    const val DEFAULT_HISTORY_REPLAY_SPEED = 1
    const val DEFAULT_EMAIL_REGEX    = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    const val DEFAULT_FALLBACK_OWNER_ID = "OWNER_001"

    // 🌐 Geofence Sentinel Default Values (Min, Mid, Max)
    const val DEFAULT_GEOFENCE_JITTER_DEBOUNCE_MS = 15000L // 15 seconds (Balanced Mid)
    const val MIN_GEOFENCE_JITTER_DEBOUNCE_MS     = 5000L  // 5 seconds (Min)
    const val MID_GEOFENCE_JITTER_DEBOUNCE_MS     = 15000L // 15 seconds (Mid)
    const val MAX_GEOFENCE_JITTER_DEBOUNCE_MS     = 60000L // 60 seconds (Max)

    // Extended Telemetry Defaults
    const val DEFAULT_MIN_DISPLACEMENT_METERS = 25.0f // 🔋 Stationary battery defense: 25m threshold
    const val DEFAULT_LOW_BATTERY_THRESHOLD   = 20
    const val DEFAULT_STALE_INACTIVE_MINUTES  = 15
    const val DEFAULT_CRITICAL_OFFLINE_HOURS  = 6
    const val DEFAULT_SIREN_DURATION_SEC      = 30
    const val DEFAULT_OFFLINE_QUEUE_MAX_POINTS= 500
    const val DEFAULT_SYSTEM_UPDATE_LINK = "https://drive.google.com/drive/folders/1qs1JRgku7SjfU59EMhZHCc7Kq2yFBGdC"
    const val DEFAULT_GITHUB_UPDATE_LINK = "https://github.com/jDroid-X/jButterfly/tree/main/jbutterfly"
    const val DEFAULT_PURGE_SAFETY_PHRASE = "jbutterfly@123"

    // ─── ACCESS METHODS ───────────────────────────────────────────────────────

    @JvmStatic fun get(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @JvmStatic fun getString(context: Context, key: String, default: String = ""): String =
        get(context).getString(key, default) ?: default

    @JvmStatic fun getInt(context: Context, key: String, default: Int = 0): Int =
        get(context).getInt(key, default)

    @JvmStatic fun getLong(context: Context, key: String, default: Long = 0L): Long =
        get(context).getLong(key, default)

    @JvmStatic fun getFloat(context: Context, key: String, default: Float = 0f): Float =
        get(context).getFloat(key, default)

    @JvmStatic fun getDouble(context: Context, key: String, default: Double = 0.0): Double {
        val strVal = getString(context, key, "")
        return if (strVal.isNotEmpty()) strVal.toDoubleOrNull() ?: default else default
    }

    @JvmStatic fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean =
        get(context).getBoolean(key, default)

    @JvmStatic fun putString(context: Context, key: String, value: String) =
        get(context).edit().putString(key, value).apply()

    @JvmStatic fun putInt(context: Context, key: String, value: Int) =
        get(context).edit().putInt(key, value).apply()

    @JvmStatic fun putLong(context: Context, key: String, value: Long) =
        get(context).edit().putLong(key, value).apply()

    @JvmStatic fun putFloat(context: Context, key: String, value: Float) =
        get(context).edit().putFloat(key, value).apply()

    @JvmStatic fun putDouble(context: Context, key: String, value: Double) =
        get(context).edit().putString(key, value.toString()).apply()

    @JvmStatic fun putBoolean(context: Context, key: String, value: Boolean) =
        get(context).edit().putBoolean(key, value).apply()

    @JvmStatic fun getAgentId(context: Context): String {
        val cached = getString(context, KEY_AGENT_ID, "")
        if (DeviceIdentityHelper.isValidAgentId(cached)) {
            return cached
        }
        return DeviceIdentityHelper.getOrGenerateAgentId(context)
    }
    @JvmStatic fun getUserNickname(context: Context): String = getString(context, KEY_USER_NICKNAME, "Active Tracker")
    @JvmStatic fun getThemeId(context: Context): Int = getInt(context, KEY_THEME_ID, 0)
    @JvmStatic fun getTrackingInterval(context: Context): Long = getLong(context, KEY_TRACKING_INTERVAL, 300000L)
    @JvmStatic fun isServiceHalted(context: Context): Boolean = getBoolean(context, KEY_SERVICE_HALTED, false)
    @JvmStatic fun isAutoLoginEnabled(context: Context): Boolean = getBoolean(context, KEY_AUTO_LOGIN, false)
    @JvmStatic fun getEquippedAvatarId(context: Context): Int = getInt(context, KEY_EQUIPPED_AVATAR_ID, 1)
    @JvmStatic fun getActiveGroup(context: Context): String = getString(context, KEY_ACTIVE_GROUP, "")
    @JvmStatic fun isLiveTrackingGranted(context: Context): Boolean = getString(context, KEY_ACL_LIVE_TRACKING) != "DENIED"
    @JvmStatic fun isDeveloperLoggingEnabled(context: Context): Boolean = getBoolean(context, KEY_DEVELOPER_LOGGING, false)

    // Dynamic Spatial & Config Getters
    @JvmStatic fun getDefaultLatitude(context: Context): Double {
        val lastLat = getDouble(context, KEY_LAST_KNOWN_LAT, 0.0)
        if (lastLat != 0.0) return lastLat
        return getDouble(context, KEY_DEFAULT_LAT, DEFAULT_FALLBACK_LAT)
    }

    @JvmStatic fun getDefaultLongitude(context: Context): Double {
        val lastLng = getDouble(context, KEY_LAST_KNOWN_LNG, 0.0)
        if (lastLng != 0.0) return lastLng
        return getDouble(context, KEY_DEFAULT_LNG, DEFAULT_FALLBACK_LNG)
    }

    @JvmStatic fun getDefaultZoom(context: Context): Float =
        getFloat(context, KEY_DEFAULT_ZOOM, DEFAULT_FALLBACK_ZOOM)

    @JvmStatic fun saveLastKnownPosition(context: Context, lat: Double, lng: Double) {
        if (lat != 0.0 && lng != 0.0) {
            get(context).edit().apply {
                putString(KEY_LAST_KNOWN_LAT, lat.toString())
                putString(KEY_LAST_KNOWN_LNG, lng.toString())
            }.apply()
        }
    }

    @JvmStatic fun getEnterpriseDomain(context: Context): String =
        getString(context, KEY_ENTERPRISE_DOMAIN, DEFAULT_ENTERPRISE_DOMAIN)

    @JvmStatic fun getSupportEmail(context: Context): String =
        getString(context, KEY_SUPPORT_EMAIL, DEFAULT_SUPPORT_EMAIL)

    @JvmStatic fun getSupportPhone(context: Context): String =
        getString(context, KEY_SUPPORT_PHONE, DEFAULT_SUPPORT_PHONE)

    @JvmStatic fun getPairingTimeoutMs(context: Context): Long =
        getLong(context, KEY_PAIRING_TIMEOUT_MS, DEFAULT_PAIRING_TIMEOUT)

    @JvmStatic fun getGeocodeDebounceMs(context: Context): Long =
        getLong(context, KEY_GEOCODE_DEBOUNCE_MS, DEFAULT_GEOCODE_DEBOUNCE)

    @JvmStatic fun getHardwarePrefix(context: Context): String =
        getString(context, KEY_HW_PREFIX, DEFAULT_HW_PREFIX)

    @JvmStatic fun getAgentPrefix(context: Context): String =
        getString(context, KEY_AGENT_PREFIX, DEFAULT_AGENT_PREFIX)

    @JvmStatic fun getGeofenceRadius(context: Context): Float =
        getFloat(context, KEY_DEFAULT_GEOFENCE_RADIUS, DEFAULT_GEOFENCE_RADIUS)

    @JvmStatic fun getMinGeofenceRadius(context: Context): Float =
        getFloat(context, KEY_MIN_GEOFENCE_RADIUS, DEFAULT_MIN_GEOFENCE_RADIUS)

    @JvmStatic fun getMaxGeofenceRadius(context: Context): Float =
        getFloat(context, KEY_MAX_GEOFENCE_RADIUS, DEFAULT_MAX_GEOFENCE_RADIUS)

    @JvmStatic fun getHistoryReplaySpeed(context: Context): Int =
        getInt(context, KEY_HISTORY_REPLAY_SPEED, DEFAULT_HISTORY_REPLAY_SPEED)

    @JvmStatic fun getEmailRegex(context: Context): String =
        getString(context, KEY_EMAIL_REGEX, DEFAULT_EMAIL_REGEX)

    @JvmStatic fun getSystemUpdateLink(context: Context): String =
        getString(context, KEY_SYSTEM_UPDATE_LINK, DEFAULT_SYSTEM_UPDATE_LINK)

    @JvmStatic fun setSystemUpdateLink(context: Context, link: String) {
        putString(context, KEY_SYSTEM_UPDATE_LINK, link)
    }

    @JvmStatic fun getGithubUpdateLink(context: Context): String =
        getString(context, KEY_GITHUB_UPDATE_LINK, DEFAULT_GITHUB_UPDATE_LINK)

    @JvmStatic fun setGithubUpdateLink(context: Context, link: String) {
        putString(context, KEY_GITHUB_UPDATE_LINK, link)
    }

    @JvmStatic fun isSilentUpdateEnabled(context: Context): Boolean =
        getBoolean(context, KEY_SILENT_UPDATE_ENABLED, true)

    @JvmStatic fun setSilentUpdateEnabled(context: Context, enabled: Boolean) {
        putBoolean(context, KEY_SILENT_UPDATE_ENABLED, enabled)
    }

    @JvmStatic fun isChildWardMode(context: Context): Boolean =
        getBoolean(context, KEY_CHILD_WARD_MODE, false)

    @JvmStatic fun setChildWardMode(context: Context, enabled: Boolean) {
        putBoolean(context, KEY_CHILD_WARD_MODE, enabled)
    }

    @JvmStatic fun getGuardianPin(context: Context): String =
        getString(context, KEY_GUARDIAN_PIN, "")

    @JvmStatic fun setGuardianPin(context: Context, pin: String) {
        putString(context, KEY_GUARDIAN_PIN, pin)
    }

    @JvmStatic fun getOwnerAgentId(context: Context): String = getAgentId(context)

    // 🌐 Geofence Sentinel Telemetry Accessors
    @JvmStatic fun getGeofenceJitterDebounceMs(context: Context): Long =
        getLong(context, KEY_GEOFENCE_JITTER_DEBOUNCE_MS, DEFAULT_GEOFENCE_JITTER_DEBOUNCE_MS)

    @JvmStatic fun setGeofenceJitterDebounceMs(context: Context, ms: Long) {
        putLong(context, KEY_GEOFENCE_JITTER_DEBOUNCE_MS, ms)
    }

    @JvmStatic fun isGeofenceLatchEnabled(context: Context): Boolean =
        getBoolean(context, KEY_GEOFENCE_LATCH_ENABLED, true)

    @JvmStatic fun setGeofenceLatchEnabled(context: Context, enabled: Boolean) {
        putBoolean(context, KEY_GEOFENCE_LATCH_ENABLED, enabled)
    }

    @JvmStatic fun isGeofenceLocalNotifEnabled(context: Context): Boolean =
        getBoolean(context, KEY_GEOFENCE_LOCAL_NOTIF_ENABLED, true)

    @JvmStatic fun setGeofenceLocalNotifEnabled(context: Context, enabled: Boolean) {
        putBoolean(context, KEY_GEOFENCE_LOCAL_NOTIF_ENABLED, enabled)
    }

    @JvmStatic fun isGeofenceFcmRelayEnabled(context: Context): Boolean =
        getBoolean(context, KEY_GEOFENCE_FCM_RELAY_ENABLED, true)

    @JvmStatic fun setGeofenceFcmRelayEnabled(context: Context, enabled: Boolean) {
        putBoolean(context, KEY_GEOFENCE_FCM_RELAY_ENABLED, enabled)
    }

    @JvmStatic fun isGeofenceSnapCenterEnabled(context: Context): Boolean =
        getBoolean(context, KEY_GEOFENCE_SNAP_CENTER_ENABLED, true)

    @JvmStatic fun setGeofenceSnapCenterEnabled(context: Context, enabled: Boolean) {
        putBoolean(context, KEY_GEOFENCE_SNAP_CENTER_ENABLED, enabled)
    }

    // 📍 Adaptive GPS & Displacement
    @JvmStatic fun getMinDisplacementMeters(context: Context): Float =
        getFloat(context, KEY_MIN_DISPLACEMENT_METERS, DEFAULT_MIN_DISPLACEMENT_METERS)

    @JvmStatic fun setMinDisplacementMeters(context: Context, meters: Float) {
        putFloat(context, KEY_MIN_DISPLACEMENT_METERS, meters)
    }

    @JvmStatic fun isStrictAccuracyWait(context: Context): Boolean =
        getBoolean(context, KEY_STRICT_ACCURACY_WAIT, false)

    @JvmStatic fun setStrictAccuracyWait(context: Context, wait: Boolean) {
        putBoolean(context, KEY_STRICT_ACCURACY_WAIT, wait)
    }

    // 🚦 RAG Telemetry Thresholds
    @JvmStatic fun getLowBatteryThreshold(context: Context): Int =
        getInt(context, KEY_LOW_BATTERY_THRESHOLD, DEFAULT_LOW_BATTERY_THRESHOLD)

    @JvmStatic fun setLowBatteryThreshold(context: Context, percent: Int) {
        putInt(context, KEY_LOW_BATTERY_THRESHOLD, percent)
    }

    @JvmStatic fun getStaleInactiveMinutes(context: Context): Int =
        getInt(context, KEY_STALE_INACTIVE_MINUTES, DEFAULT_STALE_INACTIVE_MINUTES)

    @JvmStatic fun setStaleInactiveMinutes(context: Context, mins: Int) {
        putInt(context, KEY_STALE_INACTIVE_MINUTES, mins)
    }

    @JvmStatic fun getCriticalOfflineHours(context: Context): Int =
        getInt(context, KEY_CRITICAL_OFFLINE_HOURS, DEFAULT_CRITICAL_OFFLINE_HOURS)

    @JvmStatic fun setCriticalOfflineHours(context: Context, hours: Int) {
        putInt(context, KEY_CRITICAL_OFFLINE_HOURS, hours)
    }

    // 🔊 Emergency Siren Settings
    @JvmStatic fun getSirenDurationSec(context: Context): Int =
        getInt(context, KEY_SIREN_DURATION_SEC, DEFAULT_SIREN_DURATION_SEC)

    @JvmStatic fun setSirenDurationSec(context: Context, sec: Int) {
        putInt(context, KEY_SIREN_DURATION_SEC, sec)
    }

    @JvmStatic fun isSirenVibrationEnabled(context: Context): Boolean =
        getBoolean(context, KEY_SIREN_VIBRATION_ENABLED, true)

    @JvmStatic fun setSirenVibrationEnabled(context: Context, enabled: Boolean) {
        putBoolean(context, KEY_SIREN_VIBRATION_ENABLED, enabled)
    }

    @JvmStatic fun isSirenDndBypassEnabled(context: Context): Boolean =
        getBoolean(context, KEY_SIREN_DND_BYPASS_ENABLED, true)

    @JvmStatic fun setSirenDndBypassEnabled(context: Context, enabled: Boolean) {
        putBoolean(context, KEY_SIREN_DND_BYPASS_ENABLED, enabled)
    }

    // 📦 Offline Vault & Sync
    @JvmStatic fun getOfflineQueueMaxPoints(context: Context): Int =
        getInt(context, KEY_OFFLINE_QUEUE_MAX_POINTS, DEFAULT_OFFLINE_QUEUE_MAX_POINTS)

    @JvmStatic fun setOfflineQueueMaxPoints(context: Context, points: Int) {
        putInt(context, KEY_OFFLINE_QUEUE_MAX_POINTS, points)
    }

    @JvmStatic fun isSyncWifiOnly(context: Context): Boolean =
        getBoolean(context, KEY_SYNC_WIFI_ONLY, false)

    @JvmStatic fun setSyncWifiOnly(context: Context, wifiOnly: Boolean) {
        putBoolean(context, KEY_SYNC_WIFI_ONLY, wifiOnly)
    }

    // 🛡️ Master Purge Safety Phrase
    @JvmStatic fun getPurgeSafetyPhrase(context: Context): String =
        getString(context, KEY_PURGE_SAFETY_PHRASE, DEFAULT_PURGE_SAFETY_PHRASE)

    @JvmStatic fun setPurgeSafetyPhrase(context: Context, phrase: String) {
        putString(context, KEY_PURGE_SAFETY_PHRASE, phrase)
    }

    @JvmStatic
    fun recordInstallFailure(context: Context, version: String, reason: String) {
        val nowStr = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
        val nowEpoch = System.currentTimeMillis()
        putString(context, KEY_LAST_FAILED_INSTALL_TS, nowStr)
        putLong(context, KEY_LAST_FAILED_INSTALL_EPOCH, nowEpoch)
        putString(context, KEY_LAST_FAILED_INSTALL_REASON, reason)
        putString(context, KEY_LAST_FAILED_INSTALL_VER, version)
        SystemLogger.w("RegistryManager", "Recorded installation failure: $version at $nowStr ($reason)")
    }

    @JvmStatic
    fun clearInstallFailure(context: Context) {
        get(context).edit().apply {
            remove(KEY_LAST_FAILED_INSTALL_TS)
            remove(KEY_LAST_FAILED_INSTALL_EPOCH)
            remove(KEY_LAST_FAILED_INSTALL_REASON)
            remove(KEY_LAST_FAILED_INSTALL_VER)
        }.apply()
    }

    @JvmStatic
    fun isFailureBufferActive(context: Context): Boolean {
        val lastEpoch = getLong(context, KEY_LAST_FAILED_INSTALL_EPOCH, 0L)
        if (lastEpoch <= 0) return false
        val bufferTimeMs = 24 * 60 * 60 * 1000L // 24 Hours
        val elapsed = System.currentTimeMillis() - lastEpoch
        return elapsed < bufferTimeMs
    }

    @JvmStatic
    fun getRemainingBufferHours(context: Context): Int {
        val lastEpoch = getLong(context, KEY_LAST_FAILED_INSTALL_EPOCH, 0L)
        if (lastEpoch <= 0) return 0
        val bufferTimeMs = 24 * 60 * 60 * 1000L
        val elapsed = System.currentTimeMillis() - lastEpoch
        val remMs = bufferTimeMs - elapsed
        return if (remMs > 0) ((remMs / (1000 * 60 * 60)).toInt() + 1) else 0
    }

    // ─── TYPED CONVENIENCE SETTERS ────────────────────────────────────────────

    fun saveSessionAfterLogin(
        context: Context,
        agentId: String,
        nickname: String,
        autoLogin: Boolean
    ) {
        get(context).edit().apply {
            putString(KEY_AGENT_ID, agentId)
            putString(KEY_USER_NICKNAME, nickname)
            putBoolean(KEY_AUTO_LOGIN, autoLogin)
            putBoolean(KEY_PROFILE_BYPASS, false)
        }.apply()
        SystemLogger.i("RegistryManager", "Session saved for agentId: $agentId")
    }

    fun registerOnChangeListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        get(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnChangeListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        get(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}
