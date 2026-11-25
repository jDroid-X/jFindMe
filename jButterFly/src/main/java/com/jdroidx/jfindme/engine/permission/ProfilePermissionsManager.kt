package com.jdroidx.jfindme.engine.permission

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jdroidx.jfindme.data.model.AclPermissionState
import com.jdroidx.jfindme.data.model.ProfileAclDossier
import com.jdroidx.jfindme.engine.power.OEMPowerWhitelistManager

/**
 * [Variables] context, activity, missingPerms, powerManager, notifManager, alarmManager, dossier, spec
 * [Constants] RC_MANDATORY_BATCH = 999, RC_CAMERA = 1003, RC_BACKGROUND_LOC = 1004, RC_SINGLE_GATE = 1201, PREF_REGISTRY = "system_registry"
 * [Dependencies] ContextCompat, ActivityCompat, PowerManager, NotificationManager, AlarmManager, ProfileAclDossier, OEMPowerWhitelistManager
 * [Integration Points] SettingsActivity.java, ProfileActivity.java, PersonDetailsActivity.java, MapActivity.java, PrivacyActivity.java, ProfileViewModel.kt, TrackingService.kt
 *
 * 🛡️ Enterprise Profile Permissions & Access Control Manager (SSOT)
 * Coordinates ALL Android OS runtime permissions, special system gates (Doze bypass, Exact Alarms, DND override, Unknown Sources),
 * OEM background shields across 15+ brands, and 7 in-app Profile ACLs across Android 8 through Android 15+.
 */
object ProfilePermissionsManager {

    const val RC_MANDATORY_BATCH = 999
    const val RC_CAMERA = 1003
    const val RC_BACKGROUND_LOC = 1004
    const val RC_SINGLE_GATE = 1201
    private const val PREF_REGISTRY = "system_registry"
    private const val PREF_SOFT_DISABLE_PREFIX = "perm_gate_soft_disabled_"
    private const val PREF_OPTIONAL_FEATURE_PREFIX = "perm_opt_enabled_"

    // Canonical Gate Identifiers
    const val GATE_FINE_LOCATION = "gate_fine_location"
    const val GATE_COARSE_LOCATION = "gate_coarse_location"
    const val GATE_PHONE_STATE = "gate_phone_state"
    const val GATE_NOTIFICATIONS = "gate_notifications"
    const val GATE_BACKGROUND_LOCATION = "gate_background_location"
    const val GATE_BATTERY_DOZE = "gate_battery_doze"
    const val GATE_EXACT_ALARM = "gate_exact_alarm"
    const val GATE_OEM_AUTOSTART = "gate_oem_autostart"
    const val GATE_ACTIVITY_RECOGNITION = "gate_activity_recognition"
    const val GATE_CAMERA = "gate_camera"
    const val GATE_MEDIA_STORAGE = "gate_media_storage"
    const val GATE_BLUETOOTH_MESH = "gate_bluetooth_mesh"
    const val GATE_DND_OVERRIDE = "gate_dnd_override"
    const val GATE_INSTALL_PACKAGES = "gate_install_packages"
    const val GATE_BIOMETRIC_HW = "gate_biometric_hw"

    // ACL Gate Identifiers (Synced 1:1 with ProfileAclDossier SSOT)
    const val ACL_LIVE_TRACKING = "acl_live_tracking"
    const val ACL_HISTORY_AUDIT = "acl_history_audit"
    const val ACL_GEOFENCE = "acl_geofence"
    const val ACL_ADMIN = "acl_admin"
    const val ACL_GHOST_OVERRIDE = "acl_ghost_override"
    const val ACL_BIOMETRICS = "acl_biometrics"
    const val ACL_CLOUD_SYNC = "cloud_sync_enabled"

    enum class PermissionTier {
        MANDATORY,
        BACKGROUND_OEM,
        OPTIONAL,
        IN_APP_ACL
    }

    data class PermissionGateSpec(
        val key: String,
        val title: String,
        val badge: String,
        val description: String,
        val tier: PermissionTier,
        val isMandatory: Boolean
    )

    private val ALL_SPECS: List<PermissionGateSpec> = listOf(
        // Section A: Core Mandatory
        PermissionGateSpec(GATE_FINE_LOCATION, "Precise GPS Location", "MANDATORY", "Hardware GNSS <3m tracking & geofence precision (ACCESS_FINE_LOCATION)", PermissionTier.MANDATORY, true),
        PermissionGateSpec(GATE_COARSE_LOCATION, "Approximate Network Location", "MANDATORY", "Wi-Fi & cell triangulation fallback (ACCESS_COARSE_LOCATION)", PermissionTier.MANDATORY, true),
        PermissionGateSpec(GATE_PHONE_STATE, "SIM & Cell Tower Telephony", "MANDATORY", "Extracts Cell ID / LAC / SIM state when GPS is off (READ_PHONE_STATE)", PermissionTier.MANDATORY, true),
        PermissionGateSpec(GATE_NOTIFICATIONS, "Persistent Service Notifications", "MANDATORY", "Pins TrackingService alive on Android 13+ (POST_NOTIFICATIONS)", PermissionTier.MANDATORY, true),

        // Section B: 24/7 Background & OEM Survivability
        PermissionGateSpec(GATE_BACKGROUND_LOCATION, "24/7 Background Location", "MANDATORY", "Select \"Allow all the time\" for screen-locked tracking (ACCESS_BACKGROUND_LOCATION)", PermissionTier.BACKGROUND_OEM, true),
        PermissionGateSpec(GATE_BATTERY_DOZE, "Battery Doze Exemption", "MANDATORY", "Unrestricted background sync during Android Deep Doze (IGNORE_BATTERY_OPTIMIZATIONS)", PermissionTier.BACKGROUND_OEM, true),
        PermissionGateSpec(GATE_EXACT_ALARM, "Exact Stealth Alarm Sentinel", "MANDATORY", "Wakes dormant telemetry pulses precisely on time (SCHEDULE_EXACT_ALARM)", PermissionTier.BACKGROUND_OEM, true),
        PermissionGateSpec(GATE_OEM_AUTOSTART, "OEM Autostart & Background Shield", "MANDATORY", "Stops Xiaomi, Samsung, Oppo, Vivo, Realme, Huawei task killers (15+ Brands)", PermissionTier.BACKGROUND_OEM, true),

        // Section C: Optional Feature Hardware
        PermissionGateSpec(GATE_ACTIVITY_RECOGNITION, "Motion Co-Processor", "OPTIONAL", "Detects stationary vs walking/driving to save battery (ACTIVITY_RECOGNITION)", PermissionTier.OPTIONAL, false),
        PermissionGateSpec(GATE_CAMERA, "Camera Capture & QR Pairing", "OPTIONAL", "Custom avatar capture and peer QR scanner (CAMERA)", PermissionTier.OPTIONAL, false),
        PermissionGateSpec(GATE_MEDIA_STORAGE, "Gallery Photos & Media Access", "OPTIONAL", "Import custom avatar photo from gallery (READ_MEDIA_IMAGES / STORAGE)", PermissionTier.OPTIONAL, false),
        PermissionGateSpec(GATE_BLUETOOTH_MESH, "Bluetooth & Nearby P2P Mesh", "OPTIONAL", "Offline nearby peer rendezvous & Wi-Fi Direct discovery", PermissionTier.OPTIONAL, false),
        PermissionGateSpec(GATE_DND_OVERRIDE, "Do Not Disturb Override (SOS Siren)", "OPTIONAL", "Lets emergency acoustic siren ring through silent/DND mode (${Manifest.permission.ACCESS_NOTIFICATION_POLICY})", PermissionTier.OPTIONAL, false),
        PermissionGateSpec(GATE_INSTALL_PACKAGES, "In-App Silent OTA Updates", "OPTIONAL", "Allows direct APK installation for verified app updates", PermissionTier.OPTIONAL, false),
        PermissionGateSpec(GATE_BIOMETRIC_HW, "Biometric Fingerprint / Face Gate", "OPTIONAL", "Hardware biometric authentication support (USE_BIOMETRIC)", PermissionTier.OPTIONAL, false),

        // Section D: Enterprise In-App Profile ACLs
        PermissionGateSpec(ACL_LIVE_TRACKING, "Live Tracking Broadcast Stream", "ACL", "Shares real-time coordinates with circle (OFF = Frozen / Ghost Mode)", PermissionTier.IN_APP_ACL, false),
        PermissionGateSpec(ACL_HISTORY_AUDIT, "Location History & Breadcrumbs", "ACL", "Stores historical route trail & trajectory playback points", PermissionTier.IN_APP_ACL, false),
        PermissionGateSpec(ACL_GEOFENCE, "Geofence Perimeter Alerts", "ACL", "Triggers safe-zone entry and exit breach alerts", PermissionTier.IN_APP_ACL, false),
        PermissionGateSpec(ACL_ADMIN, "Circle Administrative Override", "ACL", "Grants admin permissions to modify peer configurations & sirens", PermissionTier.IN_APP_ACL, false),
        PermissionGateSpec(ACL_GHOST_OVERRIDE, "SOS Emergency Ghost Override", "ACL", "Permits active SOS beacon to pierce Ghost Mode concealment", PermissionTier.IN_APP_ACL, false),
        PermissionGateSpec(ACL_BIOMETRICS, "Biometric Profile Edit Lock", "ACL", "Requires biometric unlock before changing profile & security rules", PermissionTier.IN_APP_ACL, false),
        PermissionGateSpec(ACL_CLOUD_SYNC, "Global Cloud Sync (Firebase RTDB)", "ACL", "Syncs profile & telemetry to cloud (OFF = local silicon storage only)", PermissionTier.IN_APP_ACL, false)
    )

    @JvmStatic
    fun getAllPermissionSpecs(): List<PermissionGateSpec> = ALL_SPECS

    @JvmStatic
    fun getSpecsByTier(tier: PermissionTier): List<PermissionGateSpec> = ALL_SPECS.filter { it.tier == tier }

    @JvmStatic
    fun getMandatoryBatchPermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    @JvmStatic
    fun getOnboardingBatchPermissions(): Array<String> {
        val list = getMandatoryBatchPermissions().toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            list.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        return list.toTypedArray()
    }

    @JvmStatic
    fun hasMandatoryPermissions(context: Context): Boolean {
        return getMandatoryBatchPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @JvmStatic
    fun requestMandatoryBatch(activity: Activity, requestCode: Int = RC_MANDATORY_BATCH) {
        val missing = getMandatoryBatchPermissions().filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, missing.toTypedArray(), requestCode)
        }
    }

    @JvmStatic
    fun isBackgroundLocationGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @JvmStatic
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }

    @JvmStatic
    fun isExactAlarmGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    @JvmStatic
    fun isDndOverrideGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    @JvmStatic
    fun isInstallPackagesGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    @JvmStatic
    fun getRuntimePermissionsForGate(gateKey: String): List<String> {
        return when (gateKey) {
            GATE_FINE_LOCATION -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            GATE_COARSE_LOCATION -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            GATE_PHONE_STATE -> listOf(Manifest.permission.READ_PHONE_STATE)
            GATE_NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
            GATE_BACKGROUND_LOCATION -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION) else emptyList()
            GATE_ACTIVITY_RECOGNITION -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyList()
            GATE_CAMERA -> listOf(Manifest.permission.CAMERA)
            GATE_MEDIA_STORAGE -> when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(Manifest.permission.READ_MEDIA_IMAGES)
                else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            GATE_BLUETOOTH_MESH -> {
                val list = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    list.add(Manifest.permission.BLUETOOTH_SCAN)
                    list.add(Manifest.permission.BLUETOOTH_CONNECT)
                    list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                }
                if (list.isEmpty()) list.add(Manifest.permission.ACCESS_FINE_LOCATION)
                list
            }
            GATE_BIOMETRIC_HW -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) listOf(Manifest.permission.USE_BIOMETRIC) else emptyList()
            else -> emptyList()
        }
    }

    @JvmStatic
    fun isOsGateGranted(context: Context, gateKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_REGISTRY, Context.MODE_PRIVATE)
        return when (gateKey) {
            GATE_FINE_LOCATION -> ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            GATE_COARSE_LOCATION -> ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            GATE_PHONE_STATE -> ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            GATE_NOTIFICATIONS -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            GATE_BACKGROUND_LOCATION -> isBackgroundLocationGranted(context)
            GATE_BATTERY_DOZE -> isBatteryOptimizationIgnored(context)
            GATE_EXACT_ALARM -> isExactAlarmGranted(context)
            GATE_OEM_AUTOSTART -> isBatteryOptimizationIgnored(context) &&
                (!OEMPowerWhitelistManager.isAggressiveOem() || prefs.getBoolean("pref_oem_whitelist_prompted_v3", false))
            GATE_ACTIVITY_RECOGNITION -> Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            GATE_CAMERA -> ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            GATE_MEDIA_STORAGE -> getRuntimePermissionsForGate(GATE_MEDIA_STORAGE).any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            GATE_BLUETOOTH_MESH -> getRuntimePermissionsForGate(GATE_BLUETOOTH_MESH).all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            GATE_DND_OVERRIDE -> isDndOverrideGranted(context)
            GATE_INSTALL_PACKAGES -> isInstallPackagesGranted(context)
            GATE_BIOMETRIC_HW -> Build.VERSION.SDK_INT < Build.VERSION_CODES.P || ContextCompat.checkSelfPermission(context, Manifest.permission.USE_BIOMETRIC) == PackageManager.PERMISSION_GRANTED
            ACL_LIVE_TRACKING -> ProfileAclDossier.loadFromLocal(context).liveTrackingEnabled
            ACL_HISTORY_AUDIT -> ProfileAclDossier.loadFromLocal(context).historyAuditEnabled
            ACL_GEOFENCE -> ProfileAclDossier.loadFromLocal(context).geofenceAlertsEnabled
            ACL_ADMIN -> ProfileAclDossier.loadFromLocal(context).isAdminOverride
            ACL_GHOST_OVERRIDE -> ProfileAclDossier.loadFromLocal(context).ghostModeAllowed
            ACL_BIOMETRICS -> ProfileAclDossier.loadFromLocal(context).biometricAuthRequired
            ACL_CLOUD_SYNC -> ProfileAclDossier.loadFromLocal(context).cloudSyncEnabled
            else -> false
        }
    }

    @JvmStatic
    fun isEffectiveGateActive(context: Context, gateKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_REGISTRY, Context.MODE_PRIVATE)
        if (gateKey.startsWith("acl_") || gateKey == ACL_CLOUD_SYNC) {
            return isOsGateGranted(context, gateKey)
        }
        val osGranted = isOsGateGranted(context, gateKey)
        val softDisabled = prefs.getBoolean(PREF_SOFT_DISABLE_PREFIX + gateKey, false)
        val serviceHalted = (gateKey == GATE_FINE_LOCATION && prefs.getBoolean("pref_service_halted", false))
        return osGranted && !softDisabled && !serviceHalted
    }

    @JvmStatic
    fun setSoftGateDisabled(context: Context, gateKey: String, disabled: Boolean) {
        val editor = context.getSharedPreferences(PREF_REGISTRY, Context.MODE_PRIVATE).edit()
        editor.putBoolean(PREF_SOFT_DISABLE_PREFIX + gateKey, disabled)
        if (gateKey == GATE_FINE_LOCATION) {
            editor.putBoolean("pref_service_halted", disabled)
        }
        editor.apply()

        try {
            val dossier = ProfileAclDossier.loadFromLocal(context)
            val agentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            if (dossier.cloudSyncEnabled && agentId.isNotBlank() && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                com.jdroidx.jfindme.data.remote.FirebaseComm.syncDeviceAccessPermissions(context, agentId)
            }
        } catch (_: Exception) {}
    }

    @JvmStatic
    fun setAclGateState(context: Context, aclKey: String, enabled: Boolean) {
        val dossier = ProfileAclDossier.loadFromLocal(context).copy()
        val targetState = if (enabled) AclPermissionState.GRANTED else AclPermissionState.DENIED
        when (aclKey) {
            ACL_LIVE_TRACKING -> dossier.liveTracking = targetState
            ACL_HISTORY_AUDIT -> dossier.historyAudit = targetState
            ACL_GEOFENCE -> dossier.geofenceBreach = targetState
            ACL_ADMIN -> dossier.adminOverride = targetState
            ACL_GHOST_OVERRIDE -> dossier.ghostOverride = targetState
            ACL_BIOMETRICS -> dossier.biometricLock = targetState
            ACL_CLOUD_SYNC -> dossier.cloudSyncEnabled = enabled
        }
        ProfileAclDossier.saveToLocal(context, dossier)

        // Sync ACL delta to Firebase RTDB only if Cloud Sync is allowed (or explicitly propagating revocation flag when toggling ACL_CLOUD_SYNC)
        try {
            val agentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            if (agentId.isNotBlank() && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                if (dossier.cloudSyncEnabled) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(agentId)
                        .child("profile")
                        .updateChildren(dossier.toPayloadMap())
                    com.jdroidx.jfindme.data.remote.FirebaseComm.syncDeviceAccessPermissions(context, agentId)
                } else if (aclKey == ACL_CLOUD_SYNC) {
                    // Record final offline-only revocation state on profile node without pushing telemetry
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(agentId)
                        .child("profile")
                        .child("cloud_sync_enabled")
                        .setValue("FALSE")
                }
            }
        } catch (_: Exception) {}
    }

    @JvmStatic
    fun requestGate(activity: Activity, gateKey: String, requestCode: Int = RC_SINGLE_GATE) {
        setSoftGateDisabled(activity, gateKey, false)
        when (gateKey) {
            GATE_BATTERY_DOZE -> requestBatteryOptimizationExemption(activity)
            GATE_OEM_AUTOSTART -> OEMPowerWhitelistManager.showOemGuidanceDialog(activity)
            GATE_EXACT_ALARM -> requestExactAlarmPermission(activity)
            GATE_DND_OVERRIDE -> requestDndOverride(activity)
            GATE_INSTALL_PACKAGES -> requestInstallPackagesPermission(activity)
            GATE_BACKGROUND_LOCATION -> {
                if (!isOsGateGranted(activity, GATE_FINE_LOCATION)) {
                    requestMandatoryBatch(activity, requestCode)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bgCode = if (requestCode == RC_SINGLE_GATE) RC_BACKGROUND_LOC else requestCode
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        bgCode
                    )
                }
            }
            else -> {
                val perms = getRuntimePermissionsForGate(gateKey).filter {
                    ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
                }
                if (perms.isNotEmpty()) {
                    ActivityCompat.requestPermissions(activity, perms.toTypedArray(), requestCode)
                } else if (!isOsGateGranted(activity, gateKey)) {
                    openAppDetailsSettings(activity)
                }
            }
        }
    }

    @JvmStatic
    fun requestBatteryOptimizationExemption(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                openAppDetailsSettings(context)
            }
        }
    }

    @JvmStatic
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                openAppDetailsSettings(context)
            }
        }
    }

    @JvmStatic
    fun requestDndOverride(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                openAppDetailsSettings(context)
            }
        }
    }

    @JvmStatic
    fun requestInstallPackagesPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                openAppDetailsSettings(context)
            }
        }
    }

    @JvmStatic
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    @JvmStatic
    fun snapshotDossierPermissions(context: Context, existingDossier: ProfileAclDossier? = null): ProfileAclDossier {
        val dossier = existingDossier ?: ProfileAclDossier.loadFromLocal(context)
        dossier.hasFineLocation = isOsGateGranted(context, GATE_FINE_LOCATION)
        dossier.hasCoarseLocation = isOsGateGranted(context, GATE_COARSE_LOCATION)
        dossier.hasBackgroundLocation = isOsGateGranted(context, GATE_BACKGROUND_LOCATION)
        dossier.hasNotifications = isOsGateGranted(context, GATE_NOTIFICATIONS)
        dossier.hasPhoneState = isOsGateGranted(context, GATE_PHONE_STATE)
        dossier.hasActivityRecognition = isOsGateGranted(context, GATE_ACTIVITY_RECOGNITION)
        dossier.hasCamera = isOsGateGranted(context, GATE_CAMERA)
        dossier.hasMediaStorage = isOsGateGranted(context, GATE_MEDIA_STORAGE)
        dossier.hasBluetoothMesh = isOsGateGranted(context, GATE_BLUETOOTH_MESH)
        dossier.hasDndOverride = isOsGateGranted(context, GATE_DND_OVERRIDE)
        dossier.isBatteryOptimizationIgnored = isOsGateGranted(context, GATE_BATTERY_DOZE)
        dossier.canScheduleExactAlarms = isOsGateGranted(context, GATE_EXACT_ALARM)
        dossier.canRequestInstallPackages = isOsGateGranted(context, GATE_INSTALL_PACKAGES)
        return dossier
    }
}
