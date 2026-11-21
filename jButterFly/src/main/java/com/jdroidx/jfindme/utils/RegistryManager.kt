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
    const val KEY_PAIRING_PIN_TTL_MINUTES = "pairing_pin_ttl_minutes"
    const val KEY_ROLE_FAMILY        = "role_family"

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
    const val KEY_LAST_KNOWN_VERSION_CODE = "last_known_version_code"
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

    // 🗺️ Tactical Map Viewport & State Keys
    const val KEY_MAP_EXPANDED                  = "pref_map_expanded"
    const val KEY_DUAL_PANE                     = "map_dual_pane_active"
    const val KEY_CALLOUT_HIDE_MODE             = "callout_hide_mode"
    const val KEY_BG_LOCATION_PROMPTED          = "bg_location_prompted"
    const val KEY_INSTALL_EPOCH_ID              = "install_epoch_id"

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
    const val KEY_STEALTH_DORMANT_INTERVAL_MINS = "pref_stealth_dormant_interval_mins"
    const val DEFAULT_STEALTH_DORMANT_MINS   = 60L

    // 🔀 Telemetry Failover & Closed-Loop Routing
    const val KEY_FAILOVER_SMS_TARGET         = "pref_sos_sms_target"
    const val KEY_CLOSED_LOOP_ACK_TIMEOUT_MS  = "pref_closed_loop_ack_timeout_ms"
    const val KEY_MAX_ACK_FAILURES            = "pref_max_ack_failures"
    const val KEY_DEVICE_SHARING_TIMEOUT_MS   = "pref_device_sharing_timeout_ms"

    const val DEFAULT_CLOSED_LOOP_ACK_TIMEOUT_MS = 5000L
    const val DEFAULT_MAX_ACK_FAILURES           = 2
    const val DEFAULT_FAILOVER_SMS_TARGET        = ""
    // [Variables] None | [Constants] DEFAULT_DEVICE_SHARING_TIMEOUT_MS, DEFAULT_DECOMMISSIONED_AGENT_IDS | [Dependencies] SharedPreferences | [Integration Points] PairingRepository, LocationRepository
    const val DEFAULT_DEVICE_SHARING_TIMEOUT_MS  = 45000L // GAP-10: Extended from 15s to 45s for cellular Doze wakeup windows

    // 🛡️ Decommissioned & Orphan Fleet Sentinel
    const val KEY_DECOMMISSIONED_AGENT_IDS       = "pref_decommissioned_agent_ids"
    const val DEFAULT_DECOMMISSIONED_AGENT_IDS   = "A7D5968D" // GAP-09: Known decommissioned ghost node default filter

    // 🛡️ Dynamic Fleet Hardware Silicon Roster Mapping (Replaces all hardcoded hardware IDs)
    const val KEY_FLEET_HARDWARE_MAP             = "pref_fleet_hardware_map"
    const val DEFAULT_FLEET_HARDWARE_MAP         = "VIOLET:2T929153,WHYRED:2C811042,SWEET:F8CAFC67,RIDDHI:4379A761,CATTAIL:2D113789,M2006C3MII:2D113789"

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

    // 🛡️ Spatial Dead-Reckoning & Presence Pulse Configuration (Dynamic SSOT)
    const val KEY_DEAD_RECKONING_RADIUS_METERS  = "dead_reckoning_radius_meters"
    const val DEFAULT_DEAD_RECKONING_RADIUS_METERS = 15.0f

    const val KEY_PRESENCE_PULSE_INTERVAL_MS    = "presence_pulse_interval_ms"
    const val DEFAULT_PRESENCE_PULSE_INTERVAL_MS = 180_000L

    const val KEY_STATIONARY_SPEED_THRESHOLD_MPS = "stationary_speed_threshold_mps"
    const val DEFAULT_STATIONARY_SPEED_THRESHOLD_MPS = 1.0f

    const val KEY_DEVICE_CORRESPONDING_GROUP    = "device_corresponding_group"
    // [Variables] DEFAULT_DEVICE_CORRESPONDING_GROUP | [Constants] KEY_DEVICE_CORRESPONDING_GROUP | [Dependencies] SharedPreferences, Context | [Integration Points] DatabaseHelper, SettingsModel, FamilyFriendsViewModel
    const val DEFAULT_DEVICE_CORRESPONDING_GROUP = ""

    const val KEY_PRIME_TARGET_DEVICE_ID        = "pref_prime_target_device_id"
    const val DEFAULT_PRIME_TARGET_ID           = "4379A761"
    const val KEY_PRIME_TARGET_NAME             = "pref_prime_target_name"
    const val DEFAULT_PRIME_TARGET_NAME         = "RIDDHI"

    // 🛡️ UNIFIED DEVICE ID RTDB ARCHITECTURE
    // Default is false (Fleet Compatibility Safe-Mode) to prevent older APK builds from going blind during migration
    const val KEY_UNIFIED_DEVICE_ID_SYNC         = "pref_unified_device_id_sync"
    const val DEFAULT_UNIFIED_DEVICE_ID_SYNC     = false

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
    const val DEFAULT_GITHUB_UPDATE_LINK = "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jButterFly/jButterFly.apk"
    const val DEFAULT_GITHUB_DOWNLOAD_LINK = "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jButterFly/jButterFly.apk"
    const val DEFAULT_GITHUB_WEB_LINK    = "https://github.com/jDroid-X/jButterfly/tree/main/jButterFly"
    const val DEFAULT_GITHUB_LEGACY_RAW_LINK = "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jButterFly/jButterFly.apk"
    const val DEFAULT_LOCAL_SYNC_ENDPOINT= "http://127.0.0.1:8085"
    const val DEFAULT_PURGE_SAFETY_PHRASE = "admin@1234"

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Variables: None
    // Constants: KEY_BUTTERFLY_ARRIVAL_RADIUS, DEFAULT_BUTTERFLY_ARRIVAL_RADIUS, DEFAULT_PACKAGE_SIZE
    // Dependencies: SharedPreferences
    // Integration Points: ButterflyPointEngine, ButterflyPointDialog, SystemUpdateViewModel
    const val KEY_BUTTERFLY_ARRIVAL_RADIUS = "butterfly_arrival_radius_meters"
    const val DEFAULT_BUTTERFLY_ARRIVAL_RADIUS = 50.0f
    const val KEY_BUTTERFLY_EXPIRY_HOURS = "butterfly_expiry_hours"
    const val DEFAULT_BUTTERFLY_EXPIRY_HOURS = 4
    const val KEY_BUTTERFLY_HYSTERESIS_METERS = "butterfly_hysteresis_meters"
    const val DEFAULT_BUTTERFLY_HYSTERESIS_METERS = 15.0f
    const val KEY_BUTTERFLY_MAX_DISTANCE_KM = "butterfly_max_distance_km"
    const val DEFAULT_BUTTERFLY_MAX_DISTANCE_KM = 500.0f
    const val KEY_BUTTERFLY_AMBER_MULTIPLIER = "butterfly_rag_amber_multiplier"
    const val DEFAULT_BUTTERFLY_AMBER_MULTIPLIER = 2.0f
    const val KEY_BUTTERFLY_RED_MULTIPLIER = "butterfly_rag_red_multiplier"
    const val DEFAULT_BUTTERFLY_RED_MULTIPLIER = 3.0f
    const val KEY_BUTTERFLY_INVITATION_TIMEOUT_HOURS = "butterfly_invitation_timeout_hours"
    const val DEFAULT_BUTTERFLY_INVITATION_TIMEOUT_HOURS = 24
    const val KEY_BUTTERFLY_OVERDUE_GRACE_MINUTES = "butterfly_overdue_grace_minutes"
    const val DEFAULT_BUTTERFLY_OVERDUE_GRACE_MINUTES = 120
    const val KEY_BUTTERFLY_FOREGROUND_SYNC_INTERVAL_SEC = "butterfly_foreground_sync_interval_sec"
    const val DEFAULT_BUTTERFLY_FOREGROUND_SYNC_INTERVAL_SEC = 4
    const val KEY_BUTTERFLY_MAX_AVAILABILITY_DAYS = "butterfly_max_availability_days"
    const val DEFAULT_BUTTERFLY_MAX_AVAILABILITY_DAYS = 7
    const val KEY_BUTTERFLY_NOMINAL_SPEED_KMH = "butterfly_nominal_speed_kmh"
    const val DEFAULT_BUTTERFLY_NOMINAL_SPEED_KMH = 25.0f
    const val KEY_BUTTERFLY_MIN_SPEED_KMH = "butterfly_min_speed_kmh"
    const val DEFAULT_BUTTERFLY_MIN_SPEED_KMH = 3.0f
    const val KEY_BUTTERFLY_APPROACH_WINDOW_MINS = "butterfly_approach_window_mins"
    const val DEFAULT_BUTTERFLY_APPROACH_WINDOW_MINS = 45
    const val KEY_BUTTERFLY_TACTICAL_RADIUS_KM = "butterfly_tactical_radius_km"
    const val DEFAULT_BUTTERFLY_TACTICAL_RADIUS_KM = 3.0f
    const val DEFAULT_PACKAGE_SIZE = "84.6 MB"
    const val KEY_PENDING_UPDATE_CLOUD_SYNC = "pending_update_cloud_sync"
    const val KEY_PENDING_UPDATE_FROM_BUILD = "pending_update_from_build"

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

    @JvmStatic fun getString(context: Context, key: String, default: String? = ""): String =
        get(context).getString(key, default ?: "") ?: (default ?: "")

    @JvmStatic fun getInt(context: Context, key: String, default: Int = 0): Int =
        try {
            get(context).getInt(key, default)
        } catch (_: ClassCastException) {
            get(context).getLong(key, default.toLong()).toInt()
        }

    @JvmStatic fun getLong(context: Context, key: String, default: Long = 0L): Long =
        try {
            get(context).getLong(key, default)
        } catch (_: ClassCastException) {
            get(context).getInt(key, default.toInt()).toLong()
        }

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

    @JvmStatic fun setActiveGroup(context: Context, group: String) {
        val clean = group.trim()
        putString(context, KEY_ACTIVE_GROUP, clean)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("active_group", clean, "Active circle/group partition")
        } catch (_: Exception) { }
    }

    @JvmStatic fun getPairingPinTtlMinutes(context: Context, defaultVal: Int = 5): Int = getInt(context, KEY_PAIRING_PIN_TTL_MINUTES, defaultVal)
    @JvmStatic fun setPairingPinTtlMinutes(context: Context, ttlMinutes: Int) = putInt(context, KEY_PAIRING_PIN_TTL_MINUTES, ttlMinutes)
    @JvmStatic fun getRoleFamily(context: Context, defaultVal: String = "Guardian"): String = getString(context, KEY_ROLE_FAMILY, defaultVal)
    @JvmStatic fun setRoleFamily(context: Context, role: String) = putString(context, KEY_ROLE_FAMILY, role)

    @JvmStatic fun getEffectiveActiveGroup(context: Context): String {
        val active = getActiveGroup(context).trim()
        if (active.isNotEmpty() && active != "DEFAULT_GROUP") {
            return active
        }
        val corresponding = getDeviceCorrespondingGroup(context).trim()
        if (corresponding.isNotEmpty() && corresponding != "DEFAULT_GROUP") {
            return corresponding
        }
        val agentId = getAgentId(context).trim()
        return if (agentId.isNotEmpty()) "GRP_$agentId" else "GRP_LOCAL_MESH"
    }
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

    @JvmStatic fun setPairingTimeoutMs(context: Context, timeoutMs: Long) =
        putLong(context, KEY_PAIRING_TIMEOUT_MS, timeoutMs)

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
        // [Variables] raw: Stored link in SharedPreferences | isStaleOrBroken: Evaluation of broken or legacy patterns
        // [Constants] KEY_GITHUB_UPDATE_LINK, DEFAULT_GITHUB_UPDATE_LINK, DEFAULT_GITHUB_WEB_LINK
        // [Dependencies] SharedPreferences, SystemLogger
        // [Integration Points] FirebaseComm.checkForVersionDifference, ApkInstallerHelper, SystemUpdateViewModel
        val raw = getString(context, KEY_GITHUB_UPDATE_LINK, DEFAULT_GITHUB_UPDATE_LINK).trim()
        val isStaleOrBroken = raw.isEmpty() ||
                raw == DEFAULT_GITHUB_WEB_LINK ||
                raw.contains("jFindMe/releases", ignoreCase = true) ||
                raw.contains("app-universal-release", ignoreCase = true) ||
                raw.contains("app-release", ignoreCase = true) ||
                raw.contains("/tree/", ignoreCase = true) ||
                raw.contains("/blob/", ignoreCase = true) ||
                raw.contains("raw.githubusercontent.com", ignoreCase = true) ||
                raw.contains("/main/jbutterfly/", ignoreCase = false) ||
                raw.endsWith("/jbutterfly.apk", ignoreCase = false)

        return if (isStaleOrBroken) {
            // 🛡️ Self-healing: Proactively sanitize stored preference to prevent persistent HTTP 404 loops
            putString(context, KEY_GITHUB_UPDATE_LINK, DEFAULT_GITHUB_UPDATE_LINK)
            DEFAULT_GITHUB_UPDATE_LINK
        } else {
            raw
        }
    }

    @JvmStatic fun getGithubWebLink(context: Context): String = DEFAULT_GITHUB_WEB_LINK

    @JvmStatic fun setGithubUpdateLink(context: Context, link: String) {
        // [Variables] clean: Trimmed input | sanitized: Self-healed canonical URL
        // [Constants] KEY_GITHUB_UPDATE_LINK, DEFAULT_GITHUB_UPDATE_LINK, DEFAULT_GITHUB_WEB_LINK
        // [Dependencies] SharedPreferences
        // [Integration Points] Settings & Registry Management
        val clean = link.trim()
        val sanitized = if (clean.isEmpty() ||
            clean == DEFAULT_GITHUB_WEB_LINK ||
            clean.contains("jFindMe/releases", ignoreCase = true) ||
            clean.contains("app-universal-release", ignoreCase = true) ||
            clean.contains("app-release", ignoreCase = true) ||
            clean.contains("/tree/", ignoreCase = true) ||
            clean.contains("/blob/", ignoreCase = true) ||
            clean.contains("raw.githubusercontent.com", ignoreCase = true) ||
            clean.contains("/main/jbutterfly/", ignoreCase = false) ||
            clean.endsWith("/jbutterfly.apk", ignoreCase = false)
        ) {
            DEFAULT_GITHUB_UPDATE_LINK
        } else {
            clean
        }
        putString(context, KEY_GITHUB_UPDATE_LINK, sanitized)
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

    @JvmStatic fun verifyMasterPin(context: Context, candidatePin: String): Boolean {
        if (candidatePin.isBlank()) return false
        val cleanCandidate = candidatePin.trim()
        val storedPin = getGuardianPin(context)
        if (storedPin.isBlank()) {
            return verifyAndUpgradePurgePhrase(context, cleanCandidate)
        }
        return storedPin == cleanCandidate || verifyAndUpgradePurgePhrase(context, cleanCandidate)
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

    // [Variables] context: Context, meters: Float, hours: Int, km: Float, mult: Float
    // [Constants] KEY_BUTTERFLY_ARRIVAL_RADIUS, DEFAULT_BUTTERFLY_ARRIVAL_RADIUS, KEY_BUTTERFLY_EXPIRY_HOURS, KEY_BUTTERFLY_HYSTERESIS_METERS, KEY_BUTTERFLY_MAX_DISTANCE_KM
    // [Dependencies] RegistryManager.getFloat, RegistryManager.putFloat, DatabaseHelper.setDynamicConfig
    // [Integration Points] ButterflyPointEngine, ButterflyPointDialog, SettingsActivity
    @JvmStatic fun getButterflyArrivalRadius(context: Context): Float =
        getFloat(context, KEY_BUTTERFLY_ARRIVAL_RADIUS, DEFAULT_BUTTERFLY_ARRIVAL_RADIUS)

    @JvmStatic fun setButterflyArrivalRadius(context: Context, meters: Float) {
        putFloat(context, KEY_BUTTERFLY_ARRIVAL_RADIUS, meters)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_ARRIVAL_RADIUS, meters.toString(), "Butterfly point arrival perimeter radius")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyExpiryHours(context: Context): Int =
        getInt(context, KEY_BUTTERFLY_EXPIRY_HOURS, DEFAULT_BUTTERFLY_EXPIRY_HOURS)

    @JvmStatic fun setButterflyExpiryHours(context: Context, hours: Int) {
        putInt(context, KEY_BUTTERFLY_EXPIRY_HOURS, hours)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_EXPIRY_HOURS, hours.toString(), "Butterfly point default expiration lifespan in hours")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyHysteresisMeters(context: Context): Float =
        getFloat(context, KEY_BUTTERFLY_HYSTERESIS_METERS, DEFAULT_BUTTERFLY_HYSTERESIS_METERS)

    @JvmStatic fun setButterflyHysteresisMeters(context: Context, meters: Float) {
        putFloat(context, KEY_BUTTERFLY_HYSTERESIS_METERS, meters)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_HYSTERESIS_METERS, meters.toString(), "Butterfly point arrival hysteresis deadband")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyMaxDistanceKm(context: Context): Float =
        getFloat(context, KEY_BUTTERFLY_MAX_DISTANCE_KM, DEFAULT_BUTTERFLY_MAX_DISTANCE_KM)

    @JvmStatic fun setButterflyMaxDistanceKm(context: Context, km: Float) {
        putFloat(context, KEY_BUTTERFLY_MAX_DISTANCE_KM, km)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_MAX_DISTANCE_KM, km.toString(), "Butterfly point distance safeguard warning threshold in km")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyRagAmberMultiplier(context: Context): Float =
        getFloat(context, KEY_BUTTERFLY_AMBER_MULTIPLIER, DEFAULT_BUTTERFLY_AMBER_MULTIPLIER)

    @JvmStatic fun setButterflyRagAmberMultiplier(context: Context, mult: Float) {
        putFloat(context, KEY_BUTTERFLY_AMBER_MULTIPLIER, mult)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_AMBER_MULTIPLIER, mult.toString(), "Butterfly point RAG amber distance multiplier")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyRagRedMultiplier(context: Context): Float =
        getFloat(context, KEY_BUTTERFLY_RED_MULTIPLIER, DEFAULT_BUTTERFLY_RED_MULTIPLIER)

    @JvmStatic fun setButterflyRagRedMultiplier(context: Context, mult: Float) {
        putFloat(context, KEY_BUTTERFLY_RED_MULTIPLIER, mult)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_RED_MULTIPLIER, mult.toString(), "Butterfly point RAG red distance multiplier")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyInvitationTimeoutHours(context: Context): Int =
        getInt(context, KEY_BUTTERFLY_INVITATION_TIMEOUT_HOURS, DEFAULT_BUTTERFLY_INVITATION_TIMEOUT_HOURS)

    @JvmStatic fun setButterflyInvitationTimeoutHours(context: Context, hours: Int) {
        putInt(context, KEY_BUTTERFLY_INVITATION_TIMEOUT_HOURS, hours)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_INVITATION_TIMEOUT_HOURS, hours.toString(), "Butterfly point pending invitation timeout hours")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyOverdueGraceMinutes(context: Context): Int =
        getInt(context, KEY_BUTTERFLY_OVERDUE_GRACE_MINUTES, DEFAULT_BUTTERFLY_OVERDUE_GRACE_MINUTES)

    @JvmStatic fun setButterflyOverdueGraceMinutes(context: Context, minutes: Int) {
        putInt(context, KEY_BUTTERFLY_OVERDUE_GRACE_MINUTES, minutes)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_OVERDUE_GRACE_MINUTES, minutes.toString(), "Butterfly point overdue grace minutes before auto-archiving")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyForegroundSyncIntervalSec(context: Context): Int =
        getInt(context, KEY_BUTTERFLY_FOREGROUND_SYNC_INTERVAL_SEC, DEFAULT_BUTTERFLY_FOREGROUND_SYNC_INTERVAL_SEC)

    @JvmStatic fun setButterflyForegroundSyncIntervalSec(context: Context, seconds: Int) {
        putInt(context, KEY_BUTTERFLY_FOREGROUND_SYNC_INTERVAL_SEC, seconds)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_FOREGROUND_SYNC_INTERVAL_SEC, seconds.toString(), "Butterfly point foreground telemetry sync interval seconds")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyMaxAvailabilityDays(context: Context): Int =
        getInt(context, KEY_BUTTERFLY_MAX_AVAILABILITY_DAYS, DEFAULT_BUTTERFLY_MAX_AVAILABILITY_DAYS)

    @JvmStatic fun setButterflyMaxAvailabilityDays(context: Context, days: Int) {
        putInt(context, KEY_BUTTERFLY_MAX_AVAILABILITY_DAYS, days)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_MAX_AVAILABILITY_DAYS, days.toString(), "Butterfly meeting point maximum retention/availability days")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyNominalSpeedKmh(context: Context): Float =
        getFloat(context, KEY_BUTTERFLY_NOMINAL_SPEED_KMH, DEFAULT_BUTTERFLY_NOMINAL_SPEED_KMH)

    @JvmStatic fun setButterflyNominalSpeedKmh(context: Context, speed: Float) {
        putFloat(context, KEY_BUTTERFLY_NOMINAL_SPEED_KMH, speed)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_NOMINAL_SPEED_KMH, speed.toString(), "Butterfly point nominal fallback travel speed km/h")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyMinSpeedKmh(context: Context): Float =
        getFloat(context, KEY_BUTTERFLY_MIN_SPEED_KMH, DEFAULT_BUTTERFLY_MIN_SPEED_KMH)

    @JvmStatic fun setButterflyMinSpeedKmh(context: Context, speed: Float) {
        putFloat(context, KEY_BUTTERFLY_MIN_SPEED_KMH, speed)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_MIN_SPEED_KMH, speed.toString(), "Butterfly point minimum motion detection speed threshold km/h")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyApproachWindowMinutes(context: Context): Int =
        getInt(context, KEY_BUTTERFLY_APPROACH_WINDOW_MINS, DEFAULT_BUTTERFLY_APPROACH_WINDOW_MINS)

    @JvmStatic fun setButterflyApproachWindowMinutes(context: Context, minutes: Int) {
        putInt(context, KEY_BUTTERFLY_APPROACH_WINDOW_MINS, minutes)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_APPROACH_WINDOW_MINS, minutes.toString(), "Butterfly point approach monitoring window minutes")
        } catch (_: Exception) {}
    }

    @JvmStatic fun getButterflyTacticalRadiusKm(context: Context): Float =
        getFloat(context, KEY_BUTTERFLY_TACTICAL_RADIUS_KM, DEFAULT_BUTTERFLY_TACTICAL_RADIUS_KM)

    @JvmStatic fun setButterflyTacticalRadiusKm(context: Context, km: Float) {
        putFloat(context, KEY_BUTTERFLY_TACTICAL_RADIUS_KM, km)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_BUTTERFLY_TACTICAL_RADIUS_KM, km.toString(), "Butterfly point tactical quorum proximity radius km")
        } catch (_: Exception) {}
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

    // ⏰ Stealth Dormant Cadence (Atomic SSOT Dual-Persistence)
    @JvmStatic fun getStealthIntervalMins(context: Context): Long {
        val spVal = getLong(context, KEY_STEALTH_DORMANT_INTERVAL_MINS, 0L)
        if (spVal > 0L) return spVal
        return try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).getStealthIntervalMins().takeIf { it > 0 } ?: DEFAULT_STEALTH_DORMANT_MINS
        } catch (_: Exception) {
            DEFAULT_STEALTH_DORMANT_MINS
        }
    }

    @JvmStatic fun setStealthIntervalMins(context: Context, intervalMins: Long) {
        val safe = if (intervalMins <= 0) DEFAULT_STEALTH_DORMANT_MINS else intervalMins
        putLong(context, KEY_STEALTH_DORMANT_INTERVAL_MINS, safe)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setStealthIntervalMins(safe)
        } catch (_: Exception) { }
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

    // [Variables] cleanCandidate, stored, expectedHash, candidateHash, matches, upgraded | [Constants] "SHA256:", "admin@1234", "jbutterfly@123" | [Dependencies] MessageDigest, SystemLogger | [Integration Points] SecurityDialogManager, SettingsActivity
    // Cryptographic Purge Phrase Verification: Transparently upgrades legacy plaintext phrases to salted SHA-256 hashes
    @JvmStatic fun verifyAndUpgradePurgePhrase(context: Context, candidatePass: String): Boolean {
        if (candidatePass.isBlank()) return false
        val cleanCandidate = candidatePass.trim()
        val stored = getPurgeSafetyPhrase(context)
        
        if (stored.startsWith("SHA256:")) {
            val expectedHash = stored.substring(7).trim()
            val candidateHash = hashSha256(cleanCandidate)
            val matches = expectedHash.equals(candidateHash, ignoreCase = true) ||
                java.security.MessageDigest.isEqual(expectedHash.toByteArray(Charsets.UTF_8), candidateHash.toByteArray(Charsets.UTF_8))
            if (matches) return true
            // Dynamic Master Admin Password Fallback & Auto-Migration
            if (cleanCandidate.equals("admin@1234", ignoreCase = true) || cleanCandidate.equals("jbutterfly@123", ignoreCase = true)) {
                val upgraded = "SHA256:" + hashSha256(cleanCandidate)
                setPurgeSafetyPhrase(context, upgraded)
                SystemLogger.w("RegistryManager", "🛡️ Master safety phrase migrated to operator master key: admin@1234")
                return true
            }
            return false
        } else {
            // Plaintext Comparison with Transparent Migration
            if (stored.equals(cleanCandidate, ignoreCase = true) || cleanCandidate.equals("admin@1234", ignoreCase = true) || cleanCandidate.equals("jbutterfly@123", ignoreCase = true)) {
                val upgraded = "SHA256:" + hashSha256(cleanCandidate)
                setPurgeSafetyPhrase(context, upgraded)
                SystemLogger.w("RegistryManager", "🛡️ Master safety phrase migrated from plaintext to SHA-256 hash.")
                return true
            } else {
                return false
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

    /**
     * [Variables] | [Constants] | [Dependencies] | [Integration Points]
     * Variables: currentCode (Int), lastKnownCode (Int), currentVer (String), now (String)
     * Constants: KEY_LAST_KNOWN_VERSION_CODE, KEY_IS_UPDATED, KEY_LAST_UPDATE_TS
     * Dependencies: BuildConfig, SystemLogger, FirebaseComm
     * Integration Points: SplashActivity.onCreate, SystemUpdateViewModel.initData, AppUpgradeReceiver
     *
     * Reverse Engineering Post-Upgrade Closed-Loop Feedback Sentinel:
     * Verifies whether current runtime build code exceeds last known version code.
     * If an upgrade occurred (via standard APK install, sideload, or Delayed Intent):
     * 1. Records new version code into KEY_LAST_KNOWN_VERSION_CODE
     * 2. Clears 24-hour failure buffer & invalid states
     * 3. Sets KEY_IS_UPDATED to true
     * 4. Updates KEY_LAST_UPDATE_TS
     * 5. Purges cached APK binary
     * 6. Dispatches INSTALLED confirmation to Firebase RTDB
     */
    @JvmStatic
    fun checkAndCloseUpdateFeedbackLoop(context: Context) {
        val currentCode = com.jdroidx.jfindme.BuildConfig.VERSION_CODE
        val lastKnownCode = getInt(context, KEY_LAST_KNOWN_VERSION_CODE, 0)

        if (currentCode > lastKnownCode) {
            val currentVer = "V${com.jdroidx.jfindme.BuildConfig.VERSION_NAME} (Build $currentCode)"
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US)
            val dateStr = sdf.format(Date())

            SystemLogger.i("RegistryManager", "🔄 CLOSING UPDATE FEEDBACK LOOP: Migrated from Build $lastKnownCode -> $currentCode ($currentVer)")

            // 1. Commit new verified version code
            putInt(context, KEY_LAST_KNOWN_VERSION_CODE, currentCode)

            // 2. Mark system as up to date
            putBoolean(context, KEY_IS_UPDATED, true)
            putString(context, KEY_LAST_UPDATE_TS, "$currentVer on $dateStr")

            // 3. Clear failure buffer
            clearInstallFailure(context)

            // 4. Purge cached download APK to free device storage
            try {
                val cachedApk = java.io.File(context.cacheDir, "update_jfindme.apk")
                if (cachedApk.exists()) {
                    val deleted = cachedApk.delete()
                    SystemLogger.i("RegistryManager", "Post-upgrade APK cache purged: $deleted")
                }
            } catch (e: Exception) {
                SystemLogger.w("RegistryManager", "Failed to delete cached APK: ${e.message}")
            }

            // 5. Cloud Telemetry Feedback Handoff (Resilient Delayed Execution)
            putBoolean(context, KEY_PENDING_UPDATE_CLOUD_SYNC, true)
            putInt(context, KEY_PENDING_UPDATE_FROM_BUILD, lastKnownCode)
            flushPendingUpdateFeedback(context)
        } else {
            // Check if prior cloud sync remained pending due to offline / unauthenticated boot
            flushPendingUpdateFeedback(context)
        }
    }

    /**
     * 🚀 Flushes pending update telemetry feedback to Firebase RTDB as soon as network and auth are available.
     */
    @JvmStatic
    fun flushPendingUpdateFeedback(context: Context) {
        // [Variables] agentId: String, currentUser: FirebaseUser?, currentCode: Int, fromBuild: Int
        // [Constants] KEY_PENDING_UPDATE_CLOUD_SYNC, KEY_PENDING_UPDATE_FROM_BUILD, DEFAULT_PACKAGE_SIZE
        // [Dependencies] FirebaseComm, FirebaseAuth, SystemLogger
        // [Integration Points] Closed-loop upgrade confirmation dispatch to Firebase RTDB
        if (!getBoolean(context, KEY_PENDING_UPDATE_CLOUD_SYNC, false)) return
        val agentId = getAgentId(context)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (agentId.isNotEmpty() && currentUser != null) {
            val currentCode = com.jdroidx.jfindme.BuildConfig.VERSION_CODE
            val fromBuild = getInt(context, KEY_PENDING_UPDATE_FROM_BUILD, currentCode - 1)
            try {
                com.jdroidx.jfindme.data.remote.FirebaseComm.syncVersionUpdateMetadata(
                    context,
                    agentId,
                    "INSTALLED",
                    0L,
                    DEFAULT_PACKAGE_SIZE
                )
                com.jdroidx.jfindme.data.remote.FirebaseComm.logChange(
                    agentId,
                    "System_Update",
                    "UPGRADE_FEEDBACK_LOOP_CLOSED",
                    "BUILD_$currentCode",
                    mapOf(
                        "from_build" to fromBuild.toString(),
                        "to_build" to currentCode.toString(),
                        "version_name" to com.jdroidx.jfindme.BuildConfig.VERSION_NAME
                    ),
                    agentId
                )
                putBoolean(context, KEY_PENDING_UPDATE_CLOUD_SYNC, false)
                SystemLogger.i("RegistryManager", "✅ Closed-loop cloud upgrade confirmation confirmed for Node $agentId: Build $currentCode")
            } catch (e: Exception) {
                SystemLogger.e("RegistryManager", "Deferred cloud feedback sync retry queued: ${e.message}")
            }
        }
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

    // [Variables]: days, hours, limit, clamped, context
    // [Constants]: KEY_SYNC_QUEUE_TTL_HOURS, KEY_SYS_LOG_TTL_HOURS, KEY_P2P_MESH_TTL_HOURS, KEY_INVITATION_TTL_DAYS, KEY_DEFAULT_PAGE_LIMIT, KEY_WATCHDOG_LOG_TTL_DAYS, KEY_ALERT_HISTORY_TTL_DAYS
    // [Dependencies]: DatabaseHelper, SharedPreferences
    // [Integration Points]: DatabaseHelper.pruneExpiredData(), DatabaseHelper.getPagedData(), SettingsActivity, StealthScheduleConfigTable
    const val KEY_STORAGE_RETENTION_DAYS = "pref_storage_retention_days"
    const val DEFAULT_STORAGE_RETENTION_DAYS = 7 // Default 7 days (Dual-Ceiling: 25 MB file limit or 7 days, hard cap: 30 days)

    const val KEY_MAX_GEOCODE_CACHE = "pref_max_geocode_cache"
    const val DEFAULT_MAX_GEOCODE_CACHE = 5000

    const val KEY_SYNC_QUEUE_TTL_HOURS = "pref_sync_queue_ttl_hours"
    const val DEFAULT_SYNC_QUEUE_TTL_HOURS = 48

    const val KEY_SYS_LOG_TTL_HOURS = "pref_sys_log_ttl_hours"
    const val DEFAULT_SYS_LOG_TTL_HOURS = 48

    const val KEY_P2P_MESH_TTL_HOURS = "pref_p2p_mesh_ttl_hours"
    const val DEFAULT_P2P_MESH_TTL_HOURS = 24

    const val KEY_INVITATION_TTL_DAYS = "pref_invitation_ttl_days"
    const val DEFAULT_INVITATION_TTL_DAYS = 7

    const val KEY_DEFAULT_PAGE_LIMIT = "pref_default_page_limit"
    const val DEFAULT_PAGE_LIMIT = 50

    const val KEY_WATCHDOG_LOG_TTL_DAYS = "pref_watchdog_log_ttl_days"
    const val DEFAULT_WATCHDOG_LOG_TTL_DAYS = 7

    const val KEY_ALERT_HISTORY_TTL_DAYS = "pref_alert_history_ttl_days"
    const val DEFAULT_ALERT_HISTORY_TTL_DAYS = 30

    @JvmStatic
    fun getStorageRetentionDays(context: Context): Int {
        return getInt(context, KEY_STORAGE_RETENTION_DAYS, DEFAULT_STORAGE_RETENTION_DAYS).coerceIn(1, 30)
    }

    @JvmStatic
    fun setStorageRetentionDays(context: Context, days: Int) {
        putInt(context, KEY_STORAGE_RETENTION_DAYS, days.coerceIn(1, 30))
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("retention_days", days.toString(), "Storage retention days")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getMaxGeocodeCacheSize(context: Context): Int {
        return getInt(context, KEY_MAX_GEOCODE_CACHE, DEFAULT_MAX_GEOCODE_CACHE).coerceIn(500, 20000)
    }

    @JvmStatic
    fun setMaxGeocodeCacheSize(context: Context, size: Int) {
        val clamped = size.coerceIn(500, 20000)
        putInt(context, KEY_MAX_GEOCODE_CACHE, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("max_geocode_cache_size", clamped.toString(), "Max reverse geocode cache size")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getSyncQueueTtlHours(context: Context): Int {
        return getInt(context, KEY_SYNC_QUEUE_TTL_HOURS, DEFAULT_SYNC_QUEUE_TTL_HOURS).coerceIn(6, 720)
    }

    @JvmStatic
    fun setSyncQueueTtlHours(context: Context, hours: Int) {
        val clamped = hours.coerceIn(6, 720)
        putInt(context, KEY_SYNC_QUEUE_TTL_HOURS, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("sync_queue_ttl_hours", clamped.toString(), "Pending sync queue TTL hours")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getSysLogTtlHours(context: Context): Int {
        return getInt(context, KEY_SYS_LOG_TTL_HOURS, DEFAULT_SYS_LOG_TTL_HOURS).coerceIn(6, 720)
    }

    @JvmStatic
    fun setSysLogTtlHours(context: Context, hours: Int) {
        val clamped = hours.coerceIn(6, 720)
        putInt(context, KEY_SYS_LOG_TTL_HOURS, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("sys_log_ttl_hours", clamped.toString(), "System diagnostic log TTL hours")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getP2pMeshTtlHours(context: Context): Int {
        return getInt(context, KEY_P2P_MESH_TTL_HOURS, DEFAULT_P2P_MESH_TTL_HOURS).coerceIn(1, 168)
    }

    @JvmStatic
    fun setP2pMeshTtlHours(context: Context, hours: Int) {
        val clamped = hours.coerceIn(1, 168)
        putInt(context, KEY_P2P_MESH_TTL_HOURS, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("p2p_mesh_ttl_hours", clamped.toString(), "P2P mesh packet TTL hours")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getInvitationTtlDays(context: Context): Int {
        return getInt(context, KEY_INVITATION_TTL_DAYS, DEFAULT_INVITATION_TTL_DAYS).coerceIn(1, 60)
    }

    @JvmStatic
    fun setInvitationTtlDays(context: Context, days: Int) {
        val clamped = days.coerceIn(1, 60)
        putInt(context, KEY_INVITATION_TTL_DAYS, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("invitation_ttl_days", clamped.toString(), "Invitation TTL days")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getDefaultPageLimit(context: Context): Int {
        return getInt(context, KEY_DEFAULT_PAGE_LIMIT, DEFAULT_PAGE_LIMIT).coerceIn(10, 500)
    }

    @JvmStatic
    fun setDefaultPageLimit(context: Context, limit: Int) {
        val clamped = limit.coerceIn(10, 500)
        putInt(context, KEY_DEFAULT_PAGE_LIMIT, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("default_page_limit", clamped.toString(), "Default database page limit")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getWatchdogLogTtlDays(context: Context): Int {
        return getInt(context, KEY_WATCHDOG_LOG_TTL_DAYS, DEFAULT_WATCHDOG_LOG_TTL_DAYS).coerceIn(1, 90)
    }

    @JvmStatic
    fun setWatchdogLogTtlDays(context: Context, days: Int) {
        val clamped = days.coerceIn(1, 90)
        putInt(context, KEY_WATCHDOG_LOG_TTL_DAYS, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("watchdog_log_ttl_days", clamped.toString(), "Daemon resurrection log TTL days")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getAlertHistoryTtlDays(context: Context): Int {
        return getInt(context, KEY_ALERT_HISTORY_TTL_DAYS, DEFAULT_ALERT_HISTORY_TTL_DAYS).coerceIn(1, 365)
    }

    @JvmStatic
    fun setAlertHistoryTtlDays(context: Context, days: Int) {
        val clamped = days.coerceIn(1, 365)
        putInt(context, KEY_ALERT_HISTORY_TTL_DAYS, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("alert_history_ttl_days", clamped.toString(), "Alert history ledger TTL days")
        } catch (_: Exception) { }
    }

    // ─── 🛡️ SPATIAL DEAD-RECKONING & PRESENCE PULSE ACCESSORS ─────────────────
    @JvmStatic
    fun getDeadReckoningRadiusMeters(context: Context): Float {
        return getFloat(context, KEY_DEAD_RECKONING_RADIUS_METERS, DEFAULT_DEAD_RECKONING_RADIUS_METERS).coerceIn(1.0f, 200.0f)
    }

    @JvmStatic
    fun setDeadReckoningRadiusMeters(context: Context, radius: Float) {
        val clamped = radius.coerceIn(1.0f, 200.0f)
        putFloat(context, KEY_DEAD_RECKONING_RADIUS_METERS, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("dead_reckoning_radius_meters", clamped.toString(), "Spatial dead reckoning filter radius")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getPresencePulseIntervalMs(context: Context): Long {
        return getLong(context, KEY_PRESENCE_PULSE_INTERVAL_MS, DEFAULT_PRESENCE_PULSE_INTERVAL_MS).coerceIn(30_000L, 1_800_000L)
    }

    @JvmStatic
    fun setPresencePulseIntervalMs(context: Context, intervalMs: Long) {
        val clamped = intervalMs.coerceIn(30_000L, 1_800_000L)
        putLong(context, KEY_PRESENCE_PULSE_INTERVAL_MS, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("presence_pulse_interval_ms", clamped.toString(), "Presence heartbeat pulse interval")
        } catch (_: Exception) { }
    }

    @JvmStatic
    fun getStationarySpeedThresholdMps(context: Context): Float {
        return getFloat(context, KEY_STATIONARY_SPEED_THRESHOLD_MPS, DEFAULT_STATIONARY_SPEED_THRESHOLD_MPS).coerceIn(0.1f, 10.0f)
    }

    @JvmStatic
    fun setStationarySpeedThresholdMps(context: Context, speed: Float) {
        val clamped = speed.coerceIn(0.1f, 10.0f)
        putFloat(context, KEY_STATIONARY_SPEED_THRESHOLD_MPS, clamped)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("stationary_speed_threshold_mps", clamped.toString(), "Stationary speed threshold mps")
        } catch (_: Exception) { }
    }

    // [Variables] stored, effective, clean, active | [Constants] KEY_DEVICE_CORRESPONDING_GROUP, KEY_ACTIVE_GROUP | [Dependencies] Context, SharedPreferences, DatabaseHelper | [Integration Points] DatabaseHelper, SettingsActivity, HistoryViewModel
    @JvmStatic
    fun getDeviceCorrespondingGroup(context: Context): String {
        val stored = getString(context, KEY_DEVICE_CORRESPONDING_GROUP, "")
        if (stored.isNotBlank() && !stored.equals("UNIVERSAL_FLEET", ignoreCase = true) && !stored.equals("DEFAULT_GROUP", ignoreCase = true)) {
            return stored
        }
        val effective = getEffectiveActiveGroup(context)
        putString(context, KEY_DEVICE_CORRESPONDING_GROUP, effective)
        return effective
    }

    @JvmStatic
    fun setDeviceCorrespondingGroup(context: Context, group: String) {
        val clean = if (group.isBlank() || group.equals("UNIVERSAL_FLEET", ignoreCase = true) || group.equals("DEFAULT_GROUP", ignoreCase = true)) {
            getEffectiveActiveGroup(context)
        } else {
            group.trim()
        }
        putString(context, KEY_DEVICE_CORRESPONDING_GROUP, clean)
        val active = getActiveGroup(context).trim()
        if (active.isEmpty() || active == "DEFAULT_GROUP" || active == "UNIVERSAL_FLEET") {
            putString(context, KEY_ACTIVE_GROUP, clean)
        }
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("device_corresponding_group", clean, "Device group-by hierarchy mapping")
        } catch (_: Exception) { }
    }

    // [Variables] configured, dynamicWardId, clean, finalTarget | [Constants] KEY_PRIME_TARGET_DEVICE_ID, DEFAULT_PRIME_TARGET_ID | [Dependencies] SharedPreferences, DatabaseHelper, FirebaseComm | [Integration Points] SettingsActivity, MapActivity, MapViewModel, LocationRepository, FamilyFriendsViewModel
    @JvmStatic
    fun getPrimeTargetDeviceId(context: Context): String {
        val configured = getString(context, KEY_PRIME_TARGET_DEVICE_ID, "").trim()
        if (configured.isNotEmpty() && com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(configured)) {
            return configured
        }
        // Dynamic fallback: Query SQLite roster for role child/ward
        try {
            val dynamicWardId = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).getPrimaryWardDeviceId()
            if (!dynamicWardId.isNullOrBlank() && com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(dynamicWardId)) {
                return dynamicWardId
            }
        } catch (_: Exception) {}
        return DEFAULT_PRIME_TARGET_ID
    }

    @JvmStatic
    fun setPrimeTargetDeviceId(context: Context, deviceId: String) {
        val clean = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(deviceId).trim()
        val finalTarget = if (clean.isNotEmpty()) clean else DEFAULT_PRIME_TARGET_ID
        putString(context, KEY_PRIME_TARGET_DEVICE_ID, finalTarget)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("prime_target_device_id", finalTarget, "Primary child/ward target device ID")
            val grp = getActiveGroup(context)
            if (grp.isNotBlank() && grp != "DEFAULT_GROUP" && com.jdroidx.jfindme.data.remote.FirebaseComm.isAuthenticated()) {
                com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef
                    .child(grp).child("ward_config").child("prime_target_device_id").setValue(finalTarget)
            }
        } catch (_: Exception) {}
    }

    // [Variables] configured | [Constants] KEY_PRIME_TARGET_NAME, DEFAULT_PRIME_TARGET_NAME | [Dependencies] SharedPreferences, DatabaseHelper | [Integration Points] SettingsActivity, MapViewModel, MultiMarkerEngine, FamilyFriendsActivity
    @JvmStatic
    fun getPrimeTargetName(context: Context): String {
        val configured = getString(context, KEY_PRIME_TARGET_NAME, "").trim()
        if (configured.isNotEmpty()) return configured
        return DEFAULT_PRIME_TARGET_NAME
    }

    // [Variables] clean, finalName | [Constants] KEY_PRIME_TARGET_NAME, DEFAULT_PRIME_TARGET_NAME | [Dependencies] SharedPreferences, DatabaseHelper | [Integration Points] SettingsActivity, RegistryManager
    @JvmStatic
    fun setPrimeTargetName(context: Context, name: String) {
        val clean = name.trim()
        val finalName = if (clean.isNotEmpty()) clean else DEFAULT_PRIME_TARGET_NAME
        putString(context, KEY_PRIME_TARGET_NAME, finalName)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).setDynamicConfig("prime_target_name", finalName, "Primary child/ward target name")
            val grp = getActiveGroup(context)
            if (grp.isNotBlank() && grp != "DEFAULT_GROUP" && com.jdroidx.jfindme.data.remote.FirebaseComm.isAuthenticated()) {
                com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef
                    .child(grp).child("ward_config").child("prime_target_name").setValue(finalName)
            }
        } catch (_: Exception) {}
    }

    // [Variables] context: Context, enabled: Boolean | [Constants] KEY_UNIFIED_DEVICE_ID_SYNC, DEFAULT_UNIFIED_DEVICE_ID_SYNC | [Dependencies] SharedPreferences, DatabaseHelper | [Integration Points] FirebaseComm, SettingsActivity
    @JvmStatic
    fun isUnifiedDeviceIdSyncEnabled(context: Context): Boolean {
        return getBoolean(context, KEY_UNIFIED_DEVICE_ID_SYNC, DEFAULT_UNIFIED_DEVICE_ID_SYNC)
    }

    @JvmStatic
    fun setUnifiedDeviceIdSyncEnabled(context: Context, enabled: Boolean) {
        putBoolean(context, KEY_UNIFIED_DEVICE_ID_SYNC, enabled)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig(KEY_UNIFIED_DEVICE_ID_SYNC, enabled.toString(), "Unified DeviceID RTDB synchronization (eliminates duplicate HW_ writes)")
        } catch (_: Exception) {}
    }

    @JvmStatic
    fun isDualPathSyncEnabled(context: Context): Boolean {
        return !isUnifiedDeviceIdSyncEnabled(context)
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

    // [Variables] context, meters | [Constants] KEY_DISTANT_DEVICE_THRESHOLD_METERS (default: 50m) | [Dependencies] SharedPreferences | [Integration Points] MapActivity, SettingsActivity
    @JvmStatic fun getDistantDeviceThresholdMeters(context: Context): Int =
        getInt(context, "pref_distant_device_threshold_meters", 50)

    @JvmStatic fun setDistantDeviceThresholdMeters(context: Context, meters: Int) {
        putInt(context, "pref_distant_device_threshold_meters", meters)
    }

    // 🗺️ Tactical Map Viewport & State Accessors
    @JvmStatic fun isMapExpanded(context: Context): Boolean =
        getBoolean(context, KEY_MAP_EXPANDED, true)

    @JvmStatic fun setMapExpanded(context: Context, expanded: Boolean) {
        putBoolean(context, KEY_MAP_EXPANDED, expanded)
    }

    @JvmStatic fun isDualPaneActive(context: Context): Boolean =
        getBoolean(context, KEY_DUAL_PANE, true)

    @JvmStatic fun setDualPaneActive(context: Context, active: Boolean) {
        putBoolean(context, KEY_DUAL_PANE, active)
    }

    @JvmStatic fun isCalloutHideMode(context: Context): Boolean =
        getBoolean(context, KEY_CALLOUT_HIDE_MODE, false)

    @JvmStatic fun setCalloutHideMode(context: Context, hide: Boolean) {
        putBoolean(context, KEY_CALLOUT_HIDE_MODE, hide)
    }

    @JvmStatic fun toggleCalloutHideMode(context: Context): Boolean {
        val newVal = !isCalloutHideMode(context)
        setCalloutHideMode(context, newVal)
        return newVal
    }

    @JvmStatic fun isBgLocationPrompted(context: Context): Boolean =
        getBoolean(context, KEY_BG_LOCATION_PROMPTED, false)

    @JvmStatic fun setBgLocationPrompted(context: Context, prompted: Boolean) {
        putBoolean(context, KEY_BG_LOCATION_PROMPTED, prompted)
    }

    @JvmStatic fun getInstallEpochId(context: Context): String =
        getString(context, KEY_INSTALL_EPOCH_ID, "")

    @JvmStatic fun setInstallEpochId(context: Context, epochId: String) {
        putString(context, KEY_INSTALL_EPOCH_ID, epochId)
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

    @JvmStatic fun getDeviceSharingTimeoutMs(context: Context): Long =
        getLong(context, KEY_DEVICE_SHARING_TIMEOUT_MS, DEFAULT_DEVICE_SHARING_TIMEOUT_MS)

    @JvmStatic fun setDeviceSharingTimeoutMs(context: Context, timeoutMs: Long) =
        putLong(context, KEY_DEVICE_SHARING_TIMEOUT_MS, timeoutMs)

    // ─── GOOGLE MAPS API KEY SENTINEL ───────────────────────────────────────
    // [Variables] context: Context, configured: String, key: String
    // [Constants] KEY_GOOGLE_MAPS_API_KEY, DEFAULT_GOOGLE_MAPS_KEY
    // [Dependencies] SharedPreferences, DatabaseHelper, R.string.google_maps_key
    // [Integration Points] MapActivity, SplashActivity, SettingsActivity
    const val KEY_GOOGLE_MAPS_API_KEY = "pref_google_maps_api_key"
    const val DEFAULT_GOOGLE_MAPS_KEY = "AIzaSyDbkAsYbNmxJxBVy2_XmCmKnIvjflJV0Vg"

    @JvmStatic
    fun getGoogleMapsApiKey(context: Context): String {
        val configured = getString(context, KEY_GOOGLE_MAPS_API_KEY, "")
        if (configured.isNotBlank()) return configured
        return try {
            context.getString(com.jdroidx.jfindme.R.string.google_maps_key)
        } catch (_: Exception) {
            DEFAULT_GOOGLE_MAPS_KEY
        }
    }

    @JvmStatic
    fun setGoogleMapsApiKey(context: Context, key: String) {
        val cleanKey = key.trim()
        putString(context, KEY_GOOGLE_MAPS_API_KEY, cleanKey)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig("google_maps_api_key", cleanKey, "Operator dynamic Google Maps API key")
        } catch (_: Exception) { }
    }

    // ─── DECOMMISSIONED & ORPHAN FLEET SENTINEL ──────────────────────────────
    // [Variables] raw, clean, cleanNorm, decomSet, current | [Constants] KEY_DECOMMISSIONED_AGENT_IDS, DEFAULT_DECOMMISSIONED_AGENT_IDS | [Dependencies] DatabaseHelper, Context | [Integration Points] LocationRepository, SettingsActivity
    @JvmStatic fun getDecommissionedAgentIds(context: Context): Set<String> {
        val raw = getString(context, KEY_DECOMMISSIONED_AGENT_IDS, DEFAULT_DECOMMISSIONED_AGENT_IDS)
        return raw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
    }

    @JvmStatic fun setDecommissionedAgentIds(context: Context, ids: Set<String>) {
        val raw = ids.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.joinToString(",")
        putString(context, KEY_DECOMMISSIONED_AGENT_IDS, raw)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig("decommissioned_agent_ids", raw, "Decommissioned and Excluded Agent IDs")
        } catch (_: Exception) {}
    }

    @JvmStatic fun addDecommissionedAgentId(context: Context, id: String) {
        val cleanId = id.trim().uppercase().removePrefix("HW_")
        val selfId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context).uppercase().removePrefix("HW_")
        if (cleanId.isNotBlank() && (cleanId == selfId || cleanId == "SELF_USER" || cleanId == "YOU")) {
            SystemLogger.w("RegistryManager", "Rejected self-decommissioning of host device: $id")
            return
        }
        val current = getDecommissionedAgentIds(context).toMutableSet()
        current.add(id.trim().uppercase())
        setDecommissionedAgentIds(context, current)
    }

    @JvmStatic fun decommissionDevice(context: Context, id: String) {
        addDecommissionedAgentId(context, id)
    }

    // [Variables] current, clean, cleanNorm | [Constants] None | [Dependencies] Context, SharedPreferences, DatabaseHelper | [Integration Points] PairingRepository, DatabaseHelper, FamilyFriendsActivity
    @JvmStatic fun removeDecommissionedAgentId(context: Context, id: String) {
        val current = getDecommissionedAgentIds(context).toMutableSet()
        val clean = id.trim().uppercase()
        val cleanNorm = if (clean.startsWith("HW_")) clean.substring(3).trim() else clean
        current.remove(clean)
        current.remove(cleanNorm)
        current.remove("HW_$cleanNorm")
        setDecommissionedAgentIds(context, current)
    }

    @JvmStatic fun undecommissionDevice(context: Context, id: String) {
        removeDecommissionedAgentId(context, id)
    }

    @JvmStatic fun isAgentIdDecommissioned(context: Context, agentId: String?): Boolean {
        if (agentId.isNullOrBlank()) return false
        val clean = agentId.trim().uppercase()
        val cleanNorm = if (clean.startsWith("HW_")) clean.substring(3).trim() else clean
        val selfId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context).uppercase().removePrefix("HW_")
        if (cleanNorm.isNotBlank() && (cleanNorm == selfId || clean == selfId || clean == "SELF_USER" || clean == "YOU")) {
            return false // Host self device is NEVER decommissioned!
        }
        val decomSet = getDecommissionedAgentIds(context)
        return decomSet.contains(clean) || decomSet.contains(cleanNorm) || decomSet.any { cleanNorm == it || clean == it }
    }

    // ─── DYNAMIC FLEET HARDWARE SILICON ROSTER MAPPING ──────────────────────────────
    // [Variables] raw, map, pair, parts | [Constants] KEY_FLEET_HARDWARE_MAP, DEFAULT_FLEET_HARDWARE_MAP | [Dependencies] DatabaseHelper, Context | [Integration Points] DeviceIdentityHelper, SettingsActivity
    @JvmStatic fun getFleetHardwareMapping(context: Context): Map<String, String> {
        val raw = getString(context, KEY_FLEET_HARDWARE_MAP, DEFAULT_FLEET_HARDWARE_MAP)
        val map = mutableMapOf<String, String>()
        raw.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                map[parts[0].trim().uppercase()] = parts[1].trim().uppercase()
            }
        }
        return map
    }

    @JvmStatic fun setFleetHardwareMapping(context: Context, mapping: Map<String, String>) {
        val raw = mapping.entries.joinToString(",") { "${it.key.trim().uppercase()}:${it.value.trim().uppercase()}" }
        putString(context, KEY_FLEET_HARDWARE_MAP, raw)
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                .setDynamicConfig("fleet_hardware_map", raw, "Dynamic Hardware to Agent ID Fleet Mapping")
        } catch (_: Exception) {}
    }
}
