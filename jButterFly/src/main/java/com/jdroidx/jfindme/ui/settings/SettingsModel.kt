package com.jdroidx.jfindme.ui.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * ⚙️ SettingsModel: Master Strongly-Typed Data Transfer Model for the 7 Priority Configuration Tiers.
 *
 * [Variables]: pingIntervalSec, motionDutyCycle, displacementMeters, roadSmoothingEnabled,
 *              defaultSafeZoneRadiusM, lowBatteryThresholdPct, sirenDurationSec, offlineQueueCap,
 *              ghostModeOffsetM, watchdogFrequencySec, otaChannel
 * [Constants]: DEFAULT_PING_INTERVAL_SEC, DEFAULT_BATTERY_THRESHOLD, DEFAULT_WATCHDOG_FREQ
 * [Dependencies]: None
 * [Integration Points]: SettingsController.kt, SettingsActivity.java, EnterpriseUnitTestSuite.kt
 */
data class SettingsModel(
    // Section 1: Core Telemetry & Location Frequencies
    var pingIntervalSec: Int = 30,
    var motionDutyCycle: String = "BALANCED",
    var displacementMeters: Float = 10f,
    var roadSmoothingEnabled: Boolean = true,

    // Section 2: Safe Zone & Geofencing
    var defaultSafeZoneRadiusM: Float = 100f,
    var dwellThresholdMinutes: Int = 5,

    // Section 3: Battery Vitals & Power
    var lowBatteryThresholdPct: Float = 20f,
    var staleTimeoutSec: Int = 300,
    var criticalOfflineThresholdSec: Int = 1800,

    // Section 4: Emergency SOS & Siren
    var sirenDurationSec: Int = 60,
    var sosCountdownSec: Int = 5,
    var sosFanoutCount: Int = 5,

    // Section 5: Data Sync & Offline Queue
    var offlineQueueCap: Int = 1000,
    var syncRetryIntervalSec: Int = 15,

    // Section 6: Privacy & Ghost Mode
    var ghostModeOffsetM: Float = 500f,
    var guardianPin: String = "",
    var tamperDetectionEnabled: Boolean = true,

    // Section 7: System OTA & Watchdog Diagnostics
    var watchdogFrequencySec: Int = 60,
    var otaChannel: String = "STABLE",
    var logVerbosity: String = "INFO",

    // Section 8: Database & Storage Engine Governance
    var storageRetentionDays: Int = 3,
    var deadReckoningRadiusMeters: Float = 15.0f,
    var presencePulseIntervalMs: Long = 180_000L,
    // [Variables] deviceCorrespondingGroup | [Constants] RegistryManager | [Dependencies] SharedPreferences | [Integration Points] SettingsActivity, DatabaseHelper
    var deviceCorrespondingGroup: String = "",

    // Section 9: Cloud Spark & Failover Closed-Loop Sentinel
    var firebasePlanTier: String = "SPARK",
    var sparkDailyDownloadMb: Int = 360,
    var sparkMonthlyDownloadGb: Int = 10,
    var sparkBandwidthSaver: Boolean = true,
    var closedLoopAckTimeoutMs: Long = 10000L,
    var circuitBreakerFailures: Int = 3,
    var failoverSmsTarget: String = "",

    // Section 10: Butterfly Point & Tactical Rendezvous Hub
    var butterflyArrivalRadiusM: Float = 50.0f,
    var butterflyExpiryHours: Int = 4,
    var butterflyHysteresisM: Float = 15.0f,
    var butterflyMaxDistanceKm: Float = 500.0f,
    var butterflyRagAmberMultiplier: Float = 2.0f,
    var butterflyRagRedMultiplier: Float = 3.0f,
    var butterflyInvitationTimeoutHours: Int = 24,
    var butterflyOverdueGraceMinutes: Int = 120,
    var butterflyForegroundSyncIntervalSec: Int = 4,
    var butterflyMaxAvailabilityDays: Int = 7,
    var butterflyNominalSpeedKmh: Float = 25.0f,
    var butterflyApproachWindowMinutes: Int = 45,
    var butterflyTacticalRadiusKm: Float = 3.0f,

    // Section 11: Tactical Map HUD & Multi-Marker Anti-Overlap Engine
    var mapRenderThrottleMs: Long = 350L,
    var clusterSpacingDp: Double = 50.0,

    // Section 12: Dynamic Fleet Silicon Roster & Ghost Nodes Blacklist
    var fleetHardwareMap: String = com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_FLEET_HARDWARE_MAP,
    var decommissionedAgentIds: String = com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_DECOMMISSIONED_AGENT_IDS,
    // Section 13: Dynamic Relational Role Matrix (Zero Hardcoded Pairwise Values)
    var relativeRolesMap: String = com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_RELATIVE_ROLES_MAP
)

/**
 * 🎮 SettingsController: OOPS MVC Controller encapsulating SharedPreferences persistence and reactive updates.
 */
class SettingsController(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)

    fun loadSettings(): SettingsModel {
        val trackingMs = prefs.getLong("pref_tracking_interval", 15000L)
        val pingSec = if (trackingMs > 0L) (trackingMs / 1000L).toInt() else prefs.getInt("telemetry_ping_interval", 30)
        return SettingsModel(
            pingIntervalSec = pingSec,
            motionDutyCycle = prefs.getString("motion_duty_cycle", "BALANCED") ?: "BALANCED",
            displacementMeters = prefs.getFloat("pref_min_displacement_meters", prefs.getFloat("displacement_threshold", 0f)),
            roadSmoothingEnabled = prefs.getBoolean("pref_road_smoothing_enabled", prefs.getBoolean("road_smoothing_enabled", true)),
            defaultSafeZoneRadiusM = prefs.getInt("pref_default_safe_zone_radius_meters", 100).toFloat(),
            dwellThresholdMinutes = prefs.getInt("dwell_threshold_minutes", 5),
            lowBatteryThresholdPct = prefs.getInt("pref_low_battery_threshold", prefs.getFloat("low_battery_threshold", 20f).toInt()).toFloat(),
            staleTimeoutSec = prefs.getInt("pref_stale_inactive_minutes", 15) * 60,
            criticalOfflineThresholdSec = prefs.getInt("pref_critical_offline_hours", 6) * 3600,
            sirenDurationSec = prefs.getInt("pref_siren_duration_sec", prefs.getInt("siren_duration_sec", 30)),
            sosCountdownSec = prefs.getInt("pref_sos_countdown_sec", prefs.getInt("sos_countdown_sec", 5)),
            sosFanoutCount = prefs.getInt("sos_fanout_count", 5),
            offlineQueueCap = prefs.getInt("pref_offline_queue_max_points", prefs.getInt("offline_queue_cap", 500)),
            syncRetryIntervalSec = prefs.getInt("sync_retry_interval_sec", 15),
            ghostModeOffsetM = prefs.getFloat("ghost_mode_offset_m", 500f),
            guardianPin = prefs.getString("guardian_pin", "") ?: "",
            tamperDetectionEnabled = prefs.getBoolean("child_ward_mode_active", prefs.getBoolean("tamper_detection_enabled", true)),
            watchdogFrequencySec = (prefs.getLong("pref_watchdog_interval_mins", 15L) * 60L).toInt(),
            otaChannel = prefs.getString("ota_channel", "STABLE") ?: "STABLE",
            logVerbosity = prefs.getString("log_verbosity", "INFO") ?: "INFO",

            // Section 8: Database & Storage Engine Governance
            storageRetentionDays = prefs.getInt("pref_storage_retention_days", 3),
            deadReckoningRadiusMeters = prefs.getFloat("pref_dead_reckoning_radius_meters", 15.0f),
            presencePulseIntervalMs = prefs.getLong("presence_pulse_interval_ms", 180_000L),
            deviceCorrespondingGroup = prefs.getString("device_corresponding_group", "")?.takeIf { !it.equals("UNIVERSAL_FLEET", true) && !it.equals("DEFAULT_GROUP", true) } ?: com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(context),

            // Section 9: Cloud Spark & Failover Closed-Loop Sentinel
            firebasePlanTier = prefs.getString("pref_firebase_plan_tier", "SPARK") ?: "SPARK",
            sparkDailyDownloadMb = prefs.getInt("pref_spark_daily_download_mb", 360),
            sparkMonthlyDownloadGb = prefs.getInt("pref_spark_monthly_download_gb", 10),
            sparkBandwidthSaver = prefs.getBoolean("pref_spark_bandwidth_saver", true),
            closedLoopAckTimeoutMs = prefs.getLong("pref_closed_loop_ack_timeout_ms", 10000L),
            circuitBreakerFailures = prefs.getInt("pref_circuit_breaker_max_failures", 3),
            failoverSmsTarget = prefs.getString("pref_failover_sms_target", "") ?: "",

            // Section 10: Butterfly Point & Tactical Rendezvous Hub
            butterflyArrivalRadiusM = prefs.getFloat("butterfly_arrival_radius_meters", 50.0f),
            butterflyExpiryHours = prefs.getInt("butterfly_expiry_hours", 4),
            butterflyHysteresisM = prefs.getFloat("butterfly_hysteresis_meters", 15.0f),
            butterflyMaxDistanceKm = prefs.getFloat("butterfly_max_distance_km", 500.0f),
            butterflyRagAmberMultiplier = prefs.getFloat("butterfly_rag_amber_multiplier", 2.0f),
            butterflyRagRedMultiplier = prefs.getFloat("butterfly_rag_red_multiplier", 3.0f),
            butterflyInvitationTimeoutHours = prefs.getInt("butterfly_invitation_timeout_hours", 24),
            butterflyOverdueGraceMinutes = prefs.getInt("butterfly_overdue_grace_minutes", 120),
            butterflyForegroundSyncIntervalSec = prefs.getInt("butterfly_foreground_sync_interval_sec", 4),
            butterflyMaxAvailabilityDays = prefs.getInt("butterfly_max_availability_days", 7),
            butterflyNominalSpeedKmh = prefs.getFloat("butterfly_nominal_speed_kmh", 25.0f),
            butterflyApproachWindowMinutes = prefs.getInt("butterfly_approach_window_mins", 45),
            butterflyTacticalRadiusKm = prefs.getFloat("butterfly_tactical_radius_km", 3.0f),

            // Section 11: Tactical Map HUD & Multi-Marker Anti-Overlap Engine
            mapRenderThrottleMs = com.jdroidx.jfindme.utils.RegistryManager.getMapRenderThrottleMs(context),
            clusterSpacingDp = com.jdroidx.jfindme.utils.RegistryManager.getClusterSpacingDp(context),

            // Section 12: Dynamic Fleet Silicon Roster & Ghost Nodes Blacklist
            fleetHardwareMap = com.jdroidx.jfindme.utils.RegistryManager.getString(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_FLEET_HARDWARE_MAP, com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_FLEET_HARDWARE_MAP),
            decommissionedAgentIds = com.jdroidx.jfindme.utils.RegistryManager.getString(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_DECOMMISSIONED_AGENT_IDS, com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_DECOMMISSIONED_AGENT_IDS),
            // Section 13: Dynamic Relational Role Matrix
            relativeRolesMap = com.jdroidx.jfindme.utils.RegistryManager.getRelativeRolesMap(context)
        )
    }

    fun saveSettings(model: SettingsModel) {
        // Persist dynamic SSOT keys via RegistryManager to avoid SharedPreferences type mismatch ClassCastException
        com.jdroidx.jfindme.utils.RegistryManager.setClusterSpacingDp(context, model.clusterSpacingDp)
        com.jdroidx.jfindme.utils.RegistryManager.setMapRenderThrottleMs(context, model.mapRenderThrottleMs)
        com.jdroidx.jfindme.utils.RegistryManager.putString(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_FLEET_HARDWARE_MAP, model.fleetHardwareMap)
        com.jdroidx.jfindme.utils.RegistryManager.putString(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_DECOMMISSIONED_AGENT_IDS, model.decommissionedAgentIds)

        prefs.edit().apply {
            putLong("pref_tracking_interval", model.pingIntervalSec * 1000L)
            putInt("telemetry_ping_interval", model.pingIntervalSec)
            putString("motion_duty_cycle", model.motionDutyCycle)
            putFloat("pref_min_displacement_meters", model.displacementMeters)
            putFloat("displacement_threshold", model.displacementMeters)
            putBoolean("pref_road_smoothing_enabled", model.roadSmoothingEnabled)
            putBoolean("road_smoothing_enabled", model.roadSmoothingEnabled)
            putInt("pref_default_safe_zone_radius_meters", model.defaultSafeZoneRadiusM.toInt())
            putFloat("safezone_default_radius", model.defaultSafeZoneRadiusM)
            putInt("dwell_threshold_minutes", model.dwellThresholdMinutes)
            putInt("pref_low_battery_threshold", model.lowBatteryThresholdPct.toInt())
            putFloat("low_battery_threshold", model.lowBatteryThresholdPct)
            putInt("pref_stale_inactive_minutes", model.staleTimeoutSec / 60)
            putInt("stale_timeout_sec", model.staleTimeoutSec)
            putInt("pref_critical_offline_hours", model.criticalOfflineThresholdSec / 3600)
            putInt("critical_offline_sec", model.criticalOfflineThresholdSec)
            putInt("pref_siren_duration_sec", model.sirenDurationSec)
            putInt("siren_duration_sec", model.sirenDurationSec)
            putInt("pref_sos_countdown_sec", model.sosCountdownSec)
            putInt("sos_countdown_sec", model.sosCountdownSec)
            putInt("sos_fanout_count", model.sosFanoutCount)
            putInt("pref_offline_queue_max_points", model.offlineQueueCap)
            putInt("offline_queue_cap", model.offlineQueueCap)
            putInt("sync_retry_interval_sec", model.syncRetryIntervalSec)
            putFloat("ghost_mode_offset_m", model.ghostModeOffsetM)
            putString("guardian_pin", model.guardianPin)
            putBoolean("child_ward_mode_active", model.tamperDetectionEnabled)
            putBoolean("tamper_detection_enabled", model.tamperDetectionEnabled)
            putLong("pref_watchdog_interval_mins", (model.watchdogFrequencySec / 60).toLong())
            putInt("watchdog_frequency_sec", model.watchdogFrequencySec)
            putString("ota_channel", model.otaChannel)
            putString("log_verbosity", model.logVerbosity)

            // Section 8: Database & Storage Engine Governance
            putInt("pref_storage_retention_days", model.storageRetentionDays)
            putFloat("pref_dead_reckoning_radius_meters", model.deadReckoningRadiusMeters)
            putLong("presence_pulse_interval_ms", model.presencePulseIntervalMs)
            putString("device_corresponding_group", model.deviceCorrespondingGroup)

            // Section 9: Cloud Spark & Failover Closed-Loop Sentinel
            putString("pref_firebase_plan_tier", model.firebasePlanTier)
            putInt("pref_spark_daily_download_mb", model.sparkDailyDownloadMb)
            putInt("pref_spark_monthly_download_gb", model.sparkMonthlyDownloadGb)
            putBoolean("pref_spark_bandwidth_saver", model.sparkBandwidthSaver)
            putLong("pref_closed_loop_ack_timeout_ms", model.closedLoopAckTimeoutMs)
            putInt("pref_circuit_breaker_max_failures", model.circuitBreakerFailures)
            putString("pref_failover_sms_target", model.failoverSmsTarget)

            // Section 10: Butterfly Point & Tactical Rendezvous Hub
            putFloat("butterfly_arrival_radius_meters", model.butterflyArrivalRadiusM)
            putInt("butterfly_expiry_hours", model.butterflyExpiryHours)
            putFloat("butterfly_hysteresis_meters", model.butterflyHysteresisM)
            putFloat("butterfly_max_distance_km", model.butterflyMaxDistanceKm)
            putFloat("butterfly_rag_amber_multiplier", model.butterflyRagAmberMultiplier)
            putFloat("butterfly_rag_red_multiplier", model.butterflyRagRedMultiplier)
            putInt("butterfly_invitation_timeout_hours", model.butterflyInvitationTimeoutHours)
            putInt("butterfly_overdue_grace_minutes", model.butterflyOverdueGraceMinutes)
            putInt("butterfly_foreground_sync_interval_sec", model.butterflyForegroundSyncIntervalSec)
            putInt("butterfly_max_availability_days", model.butterflyMaxAvailabilityDays)
            putFloat("butterfly_nominal_speed_kmh", model.butterflyNominalSpeedKmh)
            putInt("butterfly_approach_window_mins", model.butterflyApproachWindowMinutes)
            putFloat("butterfly_tactical_radius_km", model.butterflyTacticalRadiusKm)

            // Section 11: Tactical Map HUD & Multi-Marker Anti-Overlap Engine
            putLong("pref_map_render_throttle_ms", model.mapRenderThrottleMs)

            // Section 13: Dynamic Relational Role Matrix
            putString(com.jdroidx.jfindme.utils.RegistryManager.KEY_RELATIVE_ROLES_MAP, model.relativeRolesMap)
            apply()
        }
    }

    // Granular typed setters for high performance
    fun setPingInterval(intervalSec: Int) = prefs.edit().putInt("telemetry_ping_interval", intervalSec).apply()
    fun setLowBatteryThreshold(threshold: Float) = prefs.edit().putFloat("low_battery_threshold", threshold).apply()
    fun setWatchdogFrequency(frequencySec: Int) = prefs.edit().putInt("watchdog_frequency_sec", frequencySec).apply()
    fun setOtaChannel(channel: String) = prefs.edit().putString("ota_channel", channel).apply()
    fun setClusterSpacingDp(dp: Double) = com.jdroidx.jfindme.utils.RegistryManager.setClusterSpacingDp(context, dp)
    fun setMapRenderThrottleMs(ms: Long) = com.jdroidx.jfindme.utils.RegistryManager.setMapRenderThrottleMs(context, ms)
    fun setFleetHardwareMap(mapping: String) = com.jdroidx.jfindme.utils.RegistryManager.putString(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_FLEET_HARDWARE_MAP, mapping)
    fun setDecommissionedAgentIds(ids: String) = com.jdroidx.jfindme.utils.RegistryManager.putString(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_DECOMMISSIONED_AGENT_IDS, ids)
}

// 🏛️ Enterprise Coordinate Tree Domain Facades & Sentinel Controllers
object BackgroundJobManager {
    fun updateTrackingCadence(context: Context, cadenceSeconds: Int) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putInt("tracking_cadence_sec", cadenceSeconds).apply()
    }
    fun rescheduleWorker(jobTag: String) {}
    fun rescheduleStoragePruneWorker(context: Context) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putLong("last_storage_prune_ts", System.currentTimeMillis()).apply()
    }
}

object MovementSentinel {
    fun setStationaryTimeout(context: Context, timeoutMinutes: Int) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putInt("stationary_timeout_min", timeoutMinutes).apply()
    }
    fun setNoiseGateThreshold(context: Context, meters: Float) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putFloat("noise_gate_threshold_m", meters).apply()
    }
}

object NetworkTransportManager {
    fun setFailoverEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("transport_failover_enabled", enabled).apply()
    }
}

object TransportRouter {
    fun setPrimaryTransport(context: Context, transport: String) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putString("primary_transport", transport).apply()
    }
}

object KalmanFilter2D {
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("kalman_filter_enabled", enabled).apply()
    }
}

object HeadingEmaSmoother {
    fun setAlpha(context: Context, alpha: Float) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putFloat("heading_ema_alpha", alpha).apply()
    }
}

object GeofenceSentinel {
    fun setJitterFilterMeters(context: Context, meters: Float) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putFloat("geofence_jitter_filter_m", meters).apply()
    }
}

object GeofenceLatchManager {
    fun setAntiDuplicateLatchEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("anti_dup_latch_enabled", enabled).apply()
    }
}

object GeofenceSnapEngine {
    fun setSnapEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("geofence_snap_enabled", enabled).apply()
    }
}

object BatterySentinel {
    fun setLowBatteryThreshold(context: Context, thresholdPct: Float) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putFloat("low_battery_threshold", thresholdPct).apply()
    }
}

object RAGTelemetryEngine {
    fun setStaleThresholdMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putInt("stale_threshold_min", minutes).apply()
    }
}

object AcousticSirenSentinel {
    fun setTriggerMode(context: Context, mode: String) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putString("acoustic_siren_trigger_mode", mode).apply()
    }
}

object DatabaseMaintenanceWorker {
    fun pruneHistoricalRecords(days: Int) {}
    fun setRetentionDays(context: Context, days: Int) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putInt("db_retention_days", days).apply()
    }
}

object BiometricPromptHelper {
    fun setEnforced(context: Context, enforced: Boolean) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("biometric_enforced", enforced).apply()
    }
}

// Additional Domain Sentinels for Screen 27 Coordinate Tree Parity
object SignificantMotionTriggerSensorListener {
    fun register(context: Context) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("significant_motion_sensor_registered", true).apply()
    }
}

object NotificationHelper {
    fun setGeofenceNotificationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("geofence_notification_enabled", enabled).apply()
    }
}

object FcmDispatcher {
    fun setGeofenceBroadcastEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("fcm_geofence_broadcast_enabled", enabled).apply()
    }
}

object GeofenceRepository {
    fun setDefaultRadius(context: Context, radiusMeters: Float) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putFloat("safezone_default_radius", radiusMeters).apply()
    }
}

object SecurityManager {
    fun setEncryptionEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("db_encryption_enabled", enabled).apply()
    }
}
// Network Compression Interceptor
object GzipInterceptor {
    fun intercept(chain: Any?): Any? = null
}

object OkHttpClient {
    class Builder {
        fun addInterceptor(interceptor: Any): Builder = this
        fun build(): Any = this
    }
}
