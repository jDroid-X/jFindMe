package com.jdroidx.jfindme.data.model

import android.content.Context
import java.io.Serializable

/**
 * [Variables] liveTracking, historyAudit, geofenceBreach, adminOverride, ghostOverride, biometricLock, cloudSyncEnabled, hasFineLocation, hasBackgroundLocation, hasNotifications, hasPhoneState, hasActivityRecognition, hasDndOverride, isBatteryOptimizationIgnored
 * [Constants] ACL_GRANTED, ACL_DENIED, ACL_RESTRICTED, PREF_REGISTRY = "system_registry"
 * [Dependencies] java.io.Serializable, android.content.Context
 * [Integration Points] ProfileViewModel.kt, ProfileActivity.java, PersonDetailsActivity.java, SettingsActivity.java, PrivacyEngine.kt, TrackingService.kt, DeviceRepository.kt
 *
 * 🛡️ Enterprise Profile Access Control & Privacy Dossier (SSOT)
 * Represents the strongly-typed unified model for application-level ACLs and OS hardware permission state.
 */
enum class AclPermissionState {
    GRANTED,
    DENIED,
    RESTRICTED
}

data class ProfileAclDossier(
    // 🛡️ Enterprise In-App ACLs
    var liveTracking: AclPermissionState = AclPermissionState.GRANTED,
    var historyAudit: AclPermissionState = AclPermissionState.GRANTED,
    var geofenceBreach: AclPermissionState = AclPermissionState.GRANTED,
    var adminOverride: AclPermissionState = AclPermissionState.DENIED,
    var ghostOverride: AclPermissionState = AclPermissionState.GRANTED,
    var biometricLock: AclPermissionState = AclPermissionState.DENIED,
    var cloudSyncEnabled: Boolean = true,

    // 🛰️ System OS Permissions Snapshot
    var hasFineLocation: Boolean = false,
    var hasCoarseLocation: Boolean = false,
    var hasBackgroundLocation: Boolean = false,
    var hasNotifications: Boolean = false,
    var hasPhoneState: Boolean = false,
    var hasActivityRecognition: Boolean = false,
    var hasCamera: Boolean = false,
    var hasMediaStorage: Boolean = false,
    var hasBluetoothMesh: Boolean = false,
    var hasDndOverride: Boolean = false,
    var isBatteryOptimizationIgnored: Boolean = false,
    var canScheduleExactAlarms: Boolean = false,
    var canRequestInstallPackages: Boolean = false
) : Serializable {

    /**
     * Default zero-arg constructor for Java callers: new ProfileAclDossier()
     */
    constructor() : this(
        liveTracking = AclPermissionState.GRANTED,
        historyAudit = AclPermissionState.GRANTED,
        geofenceBreach = AclPermissionState.GRANTED,
        adminOverride = AclPermissionState.DENIED,
        ghostOverride = AclPermissionState.GRANTED,
        biometricLock = AclPermissionState.DENIED,
        cloudSyncEnabled = true
    )

    /**
     * Backward-compatible 14-parameter constructor for legacy Java callers
     */
    constructor(
        liveTracking: AclPermissionState,
        historyAudit: AclPermissionState,
        geofenceBreach: AclPermissionState,
        adminOverride: AclPermissionState,
        ghostOverride: AclPermissionState,
        biometricLock: AclPermissionState,
        cloudSyncEnabled: Boolean,
        hasFineLocation: Boolean,
        hasBackgroundLocation: Boolean,
        hasNotifications: Boolean,
        hasPhoneState: Boolean,
        hasActivityRecognition: Boolean,
        hasDndOverride: Boolean,
        isBatteryOptimizationIgnored: Boolean
    ) : this(
        liveTracking = liveTracking,
        historyAudit = historyAudit,
        geofenceBreach = geofenceBreach,
        adminOverride = adminOverride,
        ghostOverride = ghostOverride,
        biometricLock = biometricLock,
        cloudSyncEnabled = cloudSyncEnabled,
        hasFineLocation = hasFineLocation,
        hasCoarseLocation = false,
        hasBackgroundLocation = hasBackgroundLocation,
        hasNotifications = hasNotifications,
        hasPhoneState = hasPhoneState,
        hasActivityRecognition = hasActivityRecognition,
        hasCamera = false,
        hasMediaStorage = false,
        hasBluetoothMesh = false,
        hasDndOverride = hasDndOverride,
        isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
        canScheduleExactAlarms = false,
        canRequestInstallPackages = false
    )

    // Computed Boolean SSOT properties (for runtime telemetry & controller gates)
    val liveTrackingEnabled: Boolean get() = liveTracking == AclPermissionState.GRANTED
    val historyAuditEnabled: Boolean get() = historyAudit == AclPermissionState.GRANTED
    val geofenceAlertsEnabled: Boolean get() = geofenceBreach == AclPermissionState.GRANTED
    val isAdminOverride: Boolean get() = adminOverride == AclPermissionState.GRANTED
    val ghostModeAllowed: Boolean get() = ghostOverride == AclPermissionState.GRANTED
    val biometricAuthRequired: Boolean get() = biometricLock == AclPermissionState.GRANTED

    fun toPayloadMap(): Map<String, String> = mapOf(
        "acl_live_tracking" to liveTracking.name,
        "acl_history_audit" to historyAudit.name,
        "acl_geofence" to geofenceBreach.name,
        "acl_admin" to adminOverride.name,
        "acl_ghost_override" to ghostOverride.name,
        "acl_biometrics" to biometricLock.name,
        "cloud_sync_enabled" to cloudSyncEnabled.toString().uppercase()
    )

    companion object {
        private const val serialVersionUID = 2L
        private const val PREF_REGISTRY = "system_registry"

        @Volatile
        private var cachedLocalDossier: ProfileAclDossier? = null

        @JvmStatic
        fun invalidateCache() {
            cachedLocalDossier = null
        }

        @JvmStatic
        fun fromMap(map: Map<String, Any?>?): ProfileAclDossier {
            if (map == null) return ProfileAclDossier()
            return ProfileAclDossier(
                liveTracking = parseState(map["acl_live_tracking"], AclPermissionState.GRANTED),
                historyAudit = parseState(map["acl_history_audit"], AclPermissionState.GRANTED),
                geofenceBreach = parseState(map["acl_geofence"], AclPermissionState.GRANTED),
                adminOverride = parseState(map["acl_admin"], AclPermissionState.DENIED),
                ghostOverride = parseState(map["acl_ghost_override"], AclPermissionState.GRANTED),
                biometricLock = parseState(map["acl_biometrics"], AclPermissionState.DENIED),
                cloudSyncEnabled = !"FALSE".equals(map["cloud_sync_enabled"]?.toString(), ignoreCase = true)
            )
        }

        @JvmStatic
        fun loadFromLocal(context: Context): ProfileAclDossier {
            cachedLocalDossier?.let { return it }
            val prefs = context.getSharedPreferences(PREF_REGISTRY, Context.MODE_PRIVATE)
            val parsed = ProfileAclDossier(
                liveTracking = parseState(prefs.getString("acl_live_tracking", "GRANTED"), AclPermissionState.GRANTED),
                historyAudit = parseState(prefs.getString("acl_history_audit", "GRANTED"), AclPermissionState.GRANTED),
                geofenceBreach = parseState(prefs.getString("acl_geofence", "GRANTED"), AclPermissionState.GRANTED),
                adminOverride = parseState(prefs.getString("acl_admin", "DENIED"), AclPermissionState.DENIED),
                ghostOverride = parseState(prefs.getString("acl_ghost_override", "GRANTED"), AclPermissionState.GRANTED),
                biometricLock = parseState(prefs.getString("acl_biometrics", "DENIED"), AclPermissionState.DENIED),
                cloudSyncEnabled = prefs.getBoolean("cloud_sync_enabled", true)
            )
            cachedLocalDossier = parsed
            return parsed
        }

        @JvmStatic
        fun saveToLocal(context: Context, dossier: ProfileAclDossier) {
            cachedLocalDossier = dossier.copy()
            context.getSharedPreferences(PREF_REGISTRY, Context.MODE_PRIVATE).edit()
                .putString("acl_live_tracking", dossier.liveTracking.name)
                .putString("acl_history_audit", dossier.historyAudit.name)
                .putString("acl_geofence", dossier.geofenceBreach.name)
                .putString("acl_admin", dossier.adminOverride.name)
                .putString("acl_ghost_override", dossier.ghostOverride.name)
                .putString("acl_biometrics", dossier.biometricLock.name)
                .putBoolean("cloud_sync_enabled", dossier.cloudSyncEnabled)
                .apply()
        }

        private fun parseState(raw: Any?, defaultState: AclPermissionState): AclPermissionState {
            if (raw == null) return defaultState
            val str = raw.toString().trim().uppercase()
            return when {
                str == "GRANTED" || str == "TRUE" || str == "1" -> AclPermissionState.GRANTED
                str == "DENIED" || str == "FALSE" || str == "0" -> AclPermissionState.DENIED
                str == "RESTRICTED" -> AclPermissionState.RESTRICTED
                else -> defaultState
            }
        }
    }
}
