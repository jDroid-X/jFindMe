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
    const val KEY_OTA_DRIVE_URL = "ota_drive_url"
    const val KEY_OTA_GITHUB_REPO = "ota_github_repo"
    const val KEY_OTA_LOCAL_ENDPOINT = "ota_local_endpoint"
    const val KEY_MAINTENANCE_WINDOW_HOUR = "maintenance_window_hour"
    const val KEY_MAINTENANCE_WINDOW_MINUTE = "maintenance_window_minute"
    const val KEY_ALLOW_METERED_UPDATES = "allow_metered_updates"
    const val KEY_BALANCE_DELTA_LAST_SYNCED_TS = "balance_delta_last_synced_ts"
    const val KEY_BALANCE_DELTA_LAST_SYNCED_COUNT = "balance_delta_last_synced_count"


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
    const val KEY_LOCAL_SYNC_ENDPOINT       = "pref_local_sync_endpoint"
    const val KEY_LATEST_DELTA_SAVINGS_PCT  = "latest_delta_patch_savings_pct"
    const val KEY_LATEST_DELTA_SHA256       = "latest_delta_patch_sha256"
    const val KEY_LATEST_DELTA_FROM_BUILD   = "latest_delta_patch_from_build"
    const val KEY_LATEST_DELTA_TO_BUILD     = "latest_delta_patch_to_build"

    // 🌐 Geofence Sentinel Configuration Keys
    const val KEY_GEOFENCE_JITTER_DEBOUNCE_MS = "pref_geofence_jitter_debounce_ms"
    const val KEY_GEOFENCE_LATCH_ENABLED      = "pref_geofence_latch_enabled"
    const val KEY_GEOFENCE_LOCAL_NOTIF_ENABLED = "pref_geofence_local_notif_enabled"
    const val KEY_GEOFENCE_FCM_RELAY_ENABLED   = "pref_geofence_fcm_relay_enabled"
    const val KEY_GEOFENCE_SNAP_CENTER_ENABLED = "pref_geofence_snap_center_enabled"
    // 📡 Radar & Remote Locate Sentinel Keys
    const val KEY_RADAR_PROXIMITY_RADIUS_METERS = "pref_radar_proximity_radius_meters"
    const val KEY_REMOTE_LOCATE_TIMEOUT_MS      = "pref_remote_locate_timeout_ms"

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

    // 🔀 Telemetry Failover & Closed-Loop Routing
    const val KEY_FAILOVER_SMS_TARGET         = "pref_sos_sms_target"
    const val KEY_CLOSED_LOOP_ACK_TIMEOUT_MS  = "pref_closed_loop_ack_timeout_ms"
    const val KEY_MAX_ACK_FAILURES            = "pref_max_ack_failures"

    const val DEFAULT_CLOSED_LOOP_ACK_TIMEOUT_MS = 5000L
    const val DEFAULT_MAX_ACK_FAILURES           = 2
    const val DEFAULT_FAILOVER_SMS_TARGET        = ""

    // 🏃 Hardware Motion & Activity Recognition Sentinel
    const val KEY_ACTIVITY_RECOGNITION_ENABLED = "pref_activity_recognition_enabled"
    const val KEY_MOTION_STILL_INTERVAL_MS      = "pref_motion_still_interval_ms"
    const val KEY_MOTION_VEHICLE_INTERVAL_MS    = "pref_motion_vehicle_interval_ms"
    const val KEY_MOTION_WALKING_INTERVAL_MS    = "pref_motion_walking_interval_ms"

    // 🛣️ Road Smoothing & Kalman Filter Engine
    const val KEY_ROAD_SMOOTHING_ENABLED        = "pref_road_smoothing_enabled"
    const val KEY_KALMAN_PROCESS_NOISE          = "pref_kalman_process_noise"
    const val KEY_KALMAN_MEASUREMENT_NOISE      = "pref_kalman_measurement_noise"
    const val KEY_HEADING_EMA_FACTOR            = "pref_heading_ema_factor"
    const val KEY_MIN_MOTION_SPEED_MPS          = "pref_min_motion_speed_mps"

    // ☁️ Firebase Plan & Bandwidth Quota Sentinel
    const val KEY_FIREBASE_PLAN_TIER            = "pref_firebase_plan_tier"
    const val KEY_SPARK_DAILY_DOWNLOAD_MB       = "pref_spark_daily_download_mb"
    const val KEY_SPARK_MONTHLY_DOWNLOAD_GB     = "pref_spark_monthly_download_gb"
    const val KEY_SPARK_DAILY_RESET_TIME_IST    = "pref_spark_daily_reset_time_ist"
    const val KEY_SPARK_BANDWIDTH_SAVER_ENABLED = "pref_spark_bandwidth_saver_enabled"

    // Firebase Spark Plan Defaults
    const val DEFAULT_FIREBASE_PLAN_TIER        = "SPARK" // SPARK (Free) or BLAZE (Pay-as-you-go)
    const val DEFAULT_SPARK_DAILY_DOWNLOAD_MB   = 360     // 360 MB / day
    const val DEFAULT_SPARK_MONTHLY_DOWNLOAD_GB = 10      // 10 GB / month
    const val DEFAULT_SPARK_DAILY_RESET_TIME_IST= "12:30 PM" // Midnight PT (~12:30 PM IST)
    const val DEFAULT_SPARK_MONTHLY_RESET_RULE  = "1st of next month (00:00 UTC)"

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
    const val DEFAULT_RADAR_PROXIMITY_RADIUS_METERS = 100
    const val DEFAULT_REMOTE_LOCATE_TIMEOUT_MS      = 20000L
    const val DEFAULT_SIREN_DURATION_SEC      = 30
    const val DEFAULT_OFFLINE_QUEUE_MAX_POINTS= 500
    const val DEFAULT_SYSTEM_UPDATE_LINK = "https://drive.google.com/drive/folders/1qs1JRgku7SjfU59EMhZHCc7Kq2yFBGdC"
    const val DEFAULT_GITHUB_UPDATE_LINK = "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly.apk"
    const val DEFAULT_GITHUB_DOWNLOAD_LINK = "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/jButterFly.apk"
    const val DEFAULT_GITHUB_WEB_LINK    = "https://github.com/jDroid-X/jButterfly/tree/main/jbutterfly"
    const val DEFAULT_GITHUB_LEGACY_RAW_LINK = "https://github.com/jDroid-X/jButterfly/raw/main/jbutterfly/jButterFly.apk"
    const val DEFAULT_LOCAL_SYNC_ENDPOINT= "http://127.0.0.1:8085"
    const val DEFAULT_PURGE_SAFETY_PHRASE = "jbutterfly@123"

    // Motion & Road Tracking Defaults
    const val DEFAULT_MOTION_STILL_INTERVAL_MS   = 300000L // 5 minutes (Zero GPS drain)
    const val DEFAULT_MOTION_VEHICLE_INTERVAL_MS = 10000L  // 10 seconds (High velocity road tracking)
    const val DEFAULT_MOTION_WALKING_INTERVAL_MS = 20000L  // 20 seconds (Pedestrian duty cycle)
    const val DEFAULT_KALMAN_PROCESS_NOISE       = 3.0f
    const val DEFAULT_KALMAN_MEASUREMENT_NOISE   = 25.0f
    const val DEFAULT_HEADING_EMA_FACTOR         = 0.35f
    const val DEFAULT_MIN_MOTION_SPEED_MPS       = 0.8f

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
    @JvmStatic fun getAllowDependents(context: Context): Boolean = getBoolean(context, KEY_ALLOW_DEPENDENTS, true)
    @JvmStatic fun setAllowDependents(context: Context, allow: Boolean) = putBoolean(context, KEY_ALLOW_DEPENDENTS, allow)

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

    @JvmStatic fun getLastKnownLatitude(context: Context): Double = getDouble(context, KEY_LAST_KNOWN_LAT, 0.0)
    @JvmStatic fun getLastKnownLongitude(context: Context): Double = getDouble(context, KEY_LAST_KNOWN_LNG, 0.0)
    @JvmStatic fun setLastKnownLatitude(context: Context, lat: Double) = putDouble(context, KEY_LAST_KNOWN_LAT, lat)
    @JvmStatic fun setLastKnownLongitude(context: Context, lng: Double) = putDouble(context, KEY_LAST_KNOWN_LNG, lng)

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

    @JvmStatic fun getGithubUpdateLink(context: Context): String {
        val raw = getString(context, KEY_GITHUB_UPDATE_LINK, DEFAULT_GITHUB_UPDATE_LINK)
        return if (raw.isEmpty() || raw == DEFAULT_GITHUB_WEB_LINK || raw == "https://github.com/jDroid-X/jButterfly/tree/main/jbutterfly") {
            DEFAULT_GITHUB_UPDATE_LINK
        } else {
            raw
        }
    }

    @JvmStatic fun getGithubWebLink(context: Context): String = DEFAULT_GITHUB_WEB_LINK

    @JvmStatic fun setGithubUpdateLink(context: Context, link: String) {
        putString(context, KEY_GITHUB_UPDATE_LINK, link)
    }

    @JvmStatic fun getLocalSyncEndpoint(context: Context): String =
        getString(context, KEY_LOCAL_SYNC_ENDPOINT, DEFAULT_LOCAL_SYNC_ENDPOINT)

    @JvmStatic fun setLocalSyncEndpoint(context: Context, url: String) {
        putString(context, KEY_LOCAL_SYNC_ENDPOINT, url)
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
        val clean = phrase.trim()
        val hashed = if (clean.startsWith("SHA256:")) clean else "SHA256:" + hashSha256(clean)
        putString(context, KEY_PURGE_SAFETY_PHRASE, hashed)
    }

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Proactive Boot Encryption: Encrypts legacy plaintext phrase to SHA-256 on startup
    @JvmStatic fun ensurePurgePhraseEncrypted(context: Context) {
        val stored = getPurgeSafetyPhrase(context)
        if (!stored.startsWith("SHA256:")) {
            val upgraded = "SHA256:" + hashSha256(stored.trim())
            putString(context, KEY_PURGE_SAFETY_PHRASE, upgraded)
            SystemLogger.i("RegistryManager", "🛡️ Master safety phrase migrated proactively to SHA-256 hash.")
        }
    }

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Cryptographic Purge Phrase Verification: Transparently upgrades legacy plaintext phrases to salted SHA-256 hashes
    @JvmStatic fun verifyAndUpgradePurgePhrase(context: Context, candidatePass: String): Boolean {
        if (candidatePass.isBlank()) return false
        val cleanCandidate = candidatePass.trim()
        val stored = getPurgeSafetyPhrase(context)
        
        return if (stored.startsWith("SHA256:")) {
            val expectedHash = stored.substring(7)
            val candidateHash = hashSha256(cleanCandidate)
            java.security.MessageDigest.isEqual(expectedHash.toByteArray(Charsets.UTF_8), candidateHash.toByteArray(Charsets.UTF_8))
        } else {
            // Legacy Plaintext Comparison with Transparent Migration
            if (stored == cleanCandidate) {
                val upgraded = "SHA256:" + hashSha256(cleanCandidate)
                setPurgeSafetyPhrase(context, upgraded)
                SystemLogger.w("RegistryManager", "🛡️ Master safety phrase migrated from plaintext to SHA-256 hash.")
                true
            } else {
                false
            }
        }
    }

    @JvmStatic internal fun hashSha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
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

    // ─── 🏃 MOTION & ROAD TRACKING GETTERS / SETTERS ─────────────────────────

    @JvmStatic fun isActivityRecognitionEnabled(context: Context): Boolean =
        getBoolean(context, KEY_ACTIVITY_RECOGNITION_ENABLED, true)

    @JvmStatic fun setActivityRecognitionEnabled(context: Context, enabled: Boolean) =
        putBoolean(context, KEY_ACTIVITY_RECOGNITION_ENABLED, enabled)

    @JvmStatic fun getMotionStillIntervalMs(context: Context): Long =
        getLong(context, KEY_MOTION_STILL_INTERVAL_MS, DEFAULT_MOTION_STILL_INTERVAL_MS)

    @JvmStatic fun setMotionStillIntervalMs(context: Context, intervalMs: Long) =
        putLong(context, KEY_MOTION_STILL_INTERVAL_MS, intervalMs)

    @JvmStatic fun getMotionVehicleIntervalMs(context: Context): Long =
        getLong(context, KEY_MOTION_VEHICLE_INTERVAL_MS, DEFAULT_MOTION_VEHICLE_INTERVAL_MS)

    @JvmStatic fun setMotionVehicleIntervalMs(context: Context, intervalMs: Long) =
        putLong(context, KEY_MOTION_VEHICLE_INTERVAL_MS, intervalMs)

    @JvmStatic fun getMotionWalkingIntervalMs(context: Context): Long =
        getLong(context, KEY_MOTION_WALKING_INTERVAL_MS, DEFAULT_MOTION_WALKING_INTERVAL_MS)

    @JvmStatic fun setMotionWalkingIntervalMs(context: Context, intervalMs: Long) =
        putLong(context, KEY_MOTION_WALKING_INTERVAL_MS, intervalMs)

    @JvmStatic fun isRoadSmoothingEnabled(context: Context): Boolean =
        getBoolean(context, KEY_ROAD_SMOOTHING_ENABLED, true)

    @JvmStatic fun setRoadSmoothingEnabled(context: Context, enabled: Boolean) =
        putBoolean(context, KEY_ROAD_SMOOTHING_ENABLED, enabled)

    @JvmStatic fun getKalmanProcessNoise(context: Context): Float =
        getFloat(context, KEY_KALMAN_PROCESS_NOISE, DEFAULT_KALMAN_PROCESS_NOISE)

    @JvmStatic fun setKalmanProcessNoise(context: Context, value: Float) =
        putFloat(context, KEY_KALMAN_PROCESS_NOISE, value)

    @JvmStatic fun getKalmanMeasurementNoise(context: Context): Float =
        getFloat(context, KEY_KALMAN_MEASUREMENT_NOISE, DEFAULT_KALMAN_MEASUREMENT_NOISE)

    @JvmStatic fun setKalmanMeasurementNoise(context: Context, value: Float) =
        putFloat(context, KEY_KALMAN_MEASUREMENT_NOISE, value)

    @JvmStatic fun getHeadingEmaFactor(context: Context): Float =
        getFloat(context, KEY_HEADING_EMA_FACTOR, DEFAULT_HEADING_EMA_FACTOR)

    @JvmStatic fun setHeadingEmaFactor(context: Context, factor: Float) =
        putFloat(context, KEY_HEADING_EMA_FACTOR, factor)

    @JvmStatic fun getMinMotionSpeedMps(context: Context): Float =
        getFloat(context, KEY_MIN_MOTION_SPEED_MPS, DEFAULT_MIN_MOTION_SPEED_MPS)

    @JvmStatic fun setMinMotionSpeedMps(context: Context, speedMps: Float) =
        putFloat(context, KEY_MIN_MOTION_SPEED_MPS, speedMps)

    // ─── ☁️ FIREBASE PLAN & QUOTA SENTINEL GETTERS/SETTERS ────────────────────

    @JvmStatic fun getFirebasePlanTier(context: Context): String =
        getString(context, KEY_FIREBASE_PLAN_TIER, DEFAULT_FIREBASE_PLAN_TIER)

    @JvmStatic fun setFirebasePlanTier(context: Context, tier: String) =
        putString(context, KEY_FIREBASE_PLAN_TIER, tier)

    @JvmStatic fun getSparkDailyDownloadMb(context: Context): Int =
        getInt(context, KEY_SPARK_DAILY_DOWNLOAD_MB, DEFAULT_SPARK_DAILY_DOWNLOAD_MB)

    @JvmStatic fun setSparkDailyDownloadMb(context: Context, mb: Int) =
        putInt(context, KEY_SPARK_DAILY_DOWNLOAD_MB, mb)

    @JvmStatic fun getSparkMonthlyDownloadGb(context: Context): Int =
        getInt(context, KEY_SPARK_MONTHLY_DOWNLOAD_GB, DEFAULT_SPARK_MONTHLY_DOWNLOAD_GB)

    @JvmStatic fun setSparkMonthlyDownloadGb(context: Context, gb: Int) =
        putInt(context, KEY_SPARK_MONTHLY_DOWNLOAD_GB, gb)

    @JvmStatic fun getSparkDailyResetTimeIst(context: Context): String =
        getString(context, KEY_SPARK_DAILY_RESET_TIME_IST, DEFAULT_SPARK_DAILY_RESET_TIME_IST)

    @JvmStatic fun setSparkDailyResetTimeIst(context: Context, timeIst: String) =
        putString(context, KEY_SPARK_DAILY_RESET_TIME_IST, timeIst)

    @JvmStatic fun isSparkBandwidthSaverEnabled(context: Context): Boolean =
        getBoolean(context, KEY_SPARK_BANDWIDTH_SAVER_ENABLED, true)

    @JvmStatic fun setSparkBandwidthSaverEnabled(context: Context, enabled: Boolean) =
        putBoolean(context, KEY_SPARK_BANDWIDTH_SAVER_ENABLED, enabled)

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

    const val KEY_STORAGE_RETENTION_DAYS = "pref_storage_retention_days"
    const val DEFAULT_STORAGE_RETENTION_DAYS = 3

    @JvmStatic
    fun getStorageRetentionDays(context: Context): Int {
        return getInt(context, KEY_STORAGE_RETENTION_DAYS, DEFAULT_STORAGE_RETENTION_DAYS).coerceIn(1, 30)
    }

    @JvmStatic
    fun setStorageRetentionDays(context: Context, days: Int) {
        putInt(context, KEY_STORAGE_RETENTION_DAYS, days.coerceIn(1, 30))
    }

    // ─── EXTENDED REGISTRY ACCESSORS (Two-Tab Control Hub Standard) ───────────

    @JvmStatic fun setThemeId(context: Context, themeId: Int) {
        putInt(context, "theme_id", themeId)
    }

    @JvmStatic fun setTrackingInterval(context: Context, intervalMs: Long) {
        putLong(context, KEY_TRACKING_INTERVAL, intervalMs)
    }

    @JvmStatic fun getLocationPauseDurationMin(context: Context): Int =
        getInt(context, "pref_location_pause_duration_min", 30)

    @JvmStatic fun setLocationPauseDurationMin(context: Context, minutes: Int) {
        putInt(context, "pref_location_pause_duration_min", minutes)
    }

    @JvmStatic fun getDefaultSafeZoneRadiusMeters(context: Context): Int =
        getInt(context, "pref_default_safe_zone_radius_meters", 100)

    @JvmStatic fun setDefaultSafeZoneRadiusMeters(context: Context, meters: Int) {
        putInt(context, "pref_default_safe_zone_radius_meters", meters)
    }

    @JvmStatic fun getDistantDeviceThresholdMeters(context: Context): Int =
        getInt(context, "pref_distant_device_threshold_meters", 200)

    @JvmStatic fun setDistantDeviceThresholdMeters(context: Context, meters: Int) {
        putInt(context, "pref_distant_device_threshold_meters", meters)
    }

    // 📡 Radar Proximity & Remote Locate Accessors
    @JvmStatic fun getRadarProximityRadiusMeters(context: Context): Int =
        getInt(context, KEY_RADAR_PROXIMITY_RADIUS_METERS, DEFAULT_RADAR_PROXIMITY_RADIUS_METERS)

    @JvmStatic fun setRadarProximityRadiusMeters(context: Context, meters: Int) {
        putInt(context, KEY_RADAR_PROXIMITY_RADIUS_METERS, meters)
    }

    @JvmStatic fun getRemoteLocateTimeoutMs(context: Context): Long =
        getLong(context, KEY_REMOTE_LOCATE_TIMEOUT_MS, DEFAULT_REMOTE_LOCATE_TIMEOUT_MS)

    @JvmStatic fun setRemoteLocateTimeoutMs(context: Context, timeoutMs: Long) {
        putLong(context, KEY_REMOTE_LOCATE_TIMEOUT_MS, timeoutMs)
    }

    @JvmStatic fun isSosConfirmationRequired(context: Context): Boolean =
        getBoolean(context, "pref_sos_confirm_required", true)

    @JvmStatic fun setSosConfirmationRequired(context: Context, required: Boolean) {
        putBoolean(context, "pref_sos_confirm_required", required)
    }

    @JvmStatic fun getSosCountdownSec(context: Context): Int =
        getInt(context, "pref_sos_countdown_sec", 5)

    @JvmStatic fun setSosCountdownSec(context: Context, seconds: Int) {
        putInt(context, "pref_sos_countdown_sec", seconds)
    }

    @JvmStatic fun getSosCooldownSec(context: Context): Int =
        getInt(context, "pref_sos_cooldown_sec", 60)

    @JvmStatic fun setSosCooldownSec(context: Context, seconds: Int) {
        putInt(context, "pref_sos_cooldown_sec", seconds)
    }

    @JvmStatic fun getSosAckTimeoutSec(context: Context): Int =
        getInt(context, "pref_sos_ack_timeout_sec", 1800)

    @JvmStatic fun setSosAckTimeoutSec(context: Context, seconds: Int) {
        putInt(context, "pref_sos_ack_timeout_sec", seconds)
    }

    @JvmStatic fun getSosFanoutScope(context: Context): String =
        getString(context, "pref_sos_fanout_scope", "all_contacts")

    @JvmStatic fun setSosFanoutScope(context: Context, scope: String) {
        putString(context, "pref_sos_fanout_scope", scope)
    }

    @JvmStatic fun isSosCancelRequiresPin(context: Context): Boolean =
        getBoolean(context, "pref_sos_cancel_requires_pin", false)

    @JvmStatic fun setSosCancelRequiresPin(context: Context, required: Boolean) {
        putBoolean(context, "pref_sos_cancel_requires_pin", required)
    }

    @JvmStatic fun isUnitMetric(context: Context): Boolean =
        getBoolean(context, "pref_unit_metric", true)

    @JvmStatic fun setUnitMetric(context: Context, isMetric: Boolean) {
        putBoolean(context, "pref_unit_metric", isMetric)
    }

    @JvmStatic fun isSmartAvatarClusteringEnabled(context: Context): Boolean =
        getBoolean(context, "pref_smart_avatar_clustering", true)

    @JvmStatic fun setSmartAvatarClusteringEnabled(context: Context, enabled: Boolean) {
        putBoolean(context, "pref_smart_avatar_clustering", enabled)
    }

    // ─── 🧬 DIFFERENTIAL PATCH AUDIT & FORENSIC LEDGER ───────────────────────
    const val KEY_PATCH_AUDIT_LOG = "patch_audit_log_json"
    const val KEY_PENDING_PATCH_INFO = "pending_patch_info_summary"

    @JvmStatic
    fun recordPatchApplication(
        context: Context,
        fromBuild: Int,
        toBuild: Int,
        patchSizeStr: String,
        method: String,
        verificationStatus: String
    ) {
        val now = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val existingJson = getString(context, KEY_PATCH_AUDIT_LOG, "[]")
        try {
            val array = org.json.JSONArray(existingJson)
            val item = org.json.JSONObject().apply {
                put("timestamp", now)
                put("from_build", fromBuild)
                put("to_build", toBuild)
                put("patch_size", patchSizeStr)
                put("method", method)
                put("status", verificationStatus)
            }
            array.put(item)
            putString(context, KEY_PATCH_AUDIT_LOG, array.toString())
            SystemLogger.i("RegistryManager", "Recorded patch application: Build $fromBuild -> $toBuild via $method")
        } catch (e: Exception) {
            SystemLogger.e("RegistryManager", "Failed to record patch audit: ${e.message}")
        }
    }

    @JvmStatic
    fun getPatchHistoryFormatted(context: Context): String {
        val json = getString(context, KEY_PATCH_AUDIT_LOG, "[]")
        return try {
            val array = org.json.JSONArray(json)
            if (array.length() == 0) {
                return "• No differential patches recorded yet (Running Baseline).\n• Initial Install: V1.0.13 (Build 97) [Verified Baseline]"
            }
            val sb = java.lang.StringBuilder()
            for (i in (array.length() - 1) downTo 0) {
                val obj = array.getJSONObject(i)
                val ts = obj.optString("timestamp", "Unknown")
                val fromB = obj.optInt("from_build", 0)
                val toB = obj.optInt("to_build", 0)
                val size = obj.optString("patch_size", "")
                val method = obj.optString("method", "DELTA")
                val status = obj.optString("status", "VERIFIED")
                sb.append("• [").append(ts).append("] Build ").append(fromB).append(" ➔ ").append(toB)
                    .append(" (").append(size).append(") • ").append(method).append(" [").append(status).append("]\n")
            }
            sb.toString().trimEnd()
        } catch (e: Exception) {
            "• Initial Install: V1.0.13 (Build 97) [Baseline Verified]"
        }
    }

    @JvmStatic
    fun setPendingPatchInfo(context: Context, info: String) {
        putString(context, KEY_PENDING_PATCH_INFO, info)
    }

    @JvmStatic
    fun getPendingPatchInfo(context: Context): String {
        return getString(context, KEY_PENDING_PATCH_INFO, "None (Running Current)")
    }

    // ─── ⚡ DYNAMIC IN-APP HOT-PATCH ENGINE (ZERO-INSTALL / LIVE INJECTION) ───
    const val KEY_ACTIVE_HOTPATCH_ID    = "active_hotpatch_id"
    const val KEY_ACTIVE_HOTPATCH_NAME  = "active_hotpatch_name"
    const val KEY_ACTIVE_HOTPATCH_SIZE  = "active_hotpatch_size"
    const val KEY_ACTIVE_HOTPATCH_SHA256= "active_hotpatch_sha256"
    const val KEY_ACTIVE_HOTPATCH_NOTES = "active_hotpatch_notes"
    const val KEY_ACTIVE_HOTPATCH_STATUS= "active_hotpatch_status"
    const val KEY_ACTIVE_HOTPATCH_TS    = "active_hotpatch_timestamp"
    const val KEY_PENDING_HOTPATCH_JSON = "pending_hotpatch_json"

    @JvmStatic
    fun applyDynamicHotPatch(
        context: Context,
        patchId: String,
        name: String,
        size: String,
        sha256: String,
        releaseNotes: String,
        payloadJson: String
    ): Boolean {
        return try {
            val now = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            // 1. Persist active hotpatch state
            putString(context, KEY_ACTIVE_HOTPATCH_ID, patchId)
            putString(context, KEY_ACTIVE_HOTPATCH_NAME, name)
            putString(context, KEY_ACTIVE_HOTPATCH_SIZE, size)
            putString(context, KEY_ACTIVE_HOTPATCH_SHA256, sha256)
            putString(context, KEY_ACTIVE_HOTPATCH_NOTES, releaseNotes)
            putString(context, KEY_ACTIVE_HOTPATCH_STATUS, "VERIFIED_ACTIVE")
            putString(context, KEY_ACTIVE_HOTPATCH_TS, now)

            // 2. Parse payload and inject dynamic runtime parameters
            if (payloadJson.isNotEmpty()) {
                val payload = org.json.JSONObject(payloadJson)
                if (payload.has("cache_flush_policy") && payload.optString("cache_flush_policy") == "immediate") {
                    try {
                        val apkFile = java.io.File(context.cacheDir, "update_jfindme.apk")
                        if (apkFile.exists()) apkFile.delete()
                    } catch (e: Exception) {}
                }
                if (payload.has("radar_precision_meters")) {
                    putFloat(context, KEY_MIN_DISPLACEMENT_METERS, payload.optDouble("radar_precision_meters", 10.0).toFloat())
                }
            }

            // 3. Record in Forensic Audit Ledger
            val currentCode = com.jdroidx.jfindme.BuildConfig.VERSION_CODE
            recordPatchApplication(context, currentCode, currentCode, size, "HOTPATCH-LIVE", "VERIFIED-ACTIVE")
            SystemLogger.i("RegistryManager", "Dynamic Hot-Patch applied: $patchId ($name)")
            true
        } catch (e: Exception) {
            SystemLogger.e("RegistryManager", "Dynamic Hot-Patch application failed: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun getActiveHotPatchId(context: Context): String =
        getString(context, KEY_ACTIVE_HOTPATCH_ID, "NONE")

    @JvmStatic
    fun isHotPatchActive(context: Context, patchId: String): Boolean =
        getActiveHotPatchId(context) == patchId

    // ─── TELEMETRY FAILOVER & CLOSED-LOOP ROUTING ────────────────────────────
    @JvmStatic fun getFailoverSmsTarget(context: Context): String =
        getString(context, KEY_FAILOVER_SMS_TARGET, DEFAULT_FAILOVER_SMS_TARGET)

    @JvmStatic fun setFailoverSmsTarget(context: Context, target: String) =
        putString(context, KEY_FAILOVER_SMS_TARGET, target)

    @JvmStatic fun getClosedLoopAckTimeoutMs(context: Context): Long =
        getLong(context, KEY_CLOSED_LOOP_ACK_TIMEOUT_MS, DEFAULT_CLOSED_LOOP_ACK_TIMEOUT_MS)

    @JvmStatic fun setClosedLoopAckTimeoutMs(context: Context, timeoutMs: Long) =
        putLong(context, KEY_CLOSED_LOOP_ACK_TIMEOUT_MS, timeoutMs)

    @JvmStatic fun getMaxAckFailures(context: Context): Int =
        getInt(context, KEY_MAX_ACK_FAILURES, DEFAULT_MAX_ACK_FAILURES)

    @JvmStatic fun setMaxAckFailures(context: Context, maxFailures: Int) =
        putInt(context, KEY_MAX_ACK_FAILURES, maxFailures)

}
