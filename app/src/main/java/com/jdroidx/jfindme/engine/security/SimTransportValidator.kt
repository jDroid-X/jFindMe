package com.jdroidx.jfindme.engine.security

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.engine.location.CellTowerSentinel
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 🛡️ SimTransportValidator (Agent 1 + Agent 4 + Agent 5 Standard)
 *
 * Validates all 14 physical, OS, carrier, and background conditions that can block
 * mobile location telemetry from being shared over cellular data / SIM networks.
 *
 * Implements the Interchangeable Failover Switch Matrix to seamlessly reroute
 * telemetry across Wi-Fi IP, Offline SQLite Bursting, and SMS Cellular Signatures.
 *
 * [Variables]
 * - conditions: Mutable list of evaluated BlockingCondition records
 * - canShareViaSim: Boolean aggregate flag determining cellular telemetry readiness
 *
 * [Constants]
 * - TAG: "SimTransportValidator"
 * - FailoverRoute: TIER_1_CELLULAR, TIER_2_WIFI, TIER_3_SQLITE_BURST, TIER_4_SMS_BEACON, TIER_5_PEER_MESH
 *
 * [Dependencies]
 * - android.net.ConnectivityManager
 * - android.telephony.TelephonyManager
 * - android.os.PowerManager
 * - android.location.LocationManager
 * - com.google.firebase.auth.FirebaseAuth
 *
 * [Integration Points]
 * - TelemetryFailoverRouter.kt: Uses FailoverRoute decision to execute transmission
 * - SimForensicWorker.kt: Runs pre-flight audit before background location dispatch
 * - TrackingService.kt: Real-time diagnostics logger for connection health
 */
object SimTransportValidator {

    private const val TAG = "SimTransportValidator"

    enum class Severity {
        CRITICAL, // Telemetry completely halted on this channel
        WARNING,  // Telemetry degraded or delayed
        INFO      // Informational / optimal
    }

    enum class FailoverRoute {
        TIER_1_CELLULAR_DATA,   // Primary: Direct Firebase RTDB over SIM mobile data
        TIER_2_WIFI_IP,         // Secondary: Direct Firebase RTDB over Wi-Fi
        TIER_3_SQLITE_BURST,    // Tertiary: Encrypted SQLite Queue + Auto-burst on reconnect
        TIER_4_SMS_BEACON,      // Quaternary: Direct cellular SMS PDU payload (Zero-Data)
        TIER_5_PEER_MESH        // Quinary: BLE / Nearby local mesh relay
    }

    data class BlockingCondition(
        val code: String,
        val title: String,
        val isBlocked: Boolean,
        val severity: Severity,
        val technicalReason: String,
        val remediation: String,
        val suggestedFailover: FailoverRoute
    )

    data class TransportAuditReport(
        val canShareViaSim: Boolean,
        val isCellularActive: Boolean,
        val isWifiActive: Boolean,
        val recommendedRoute: FailoverRoute,
        val overallStatus: String,
        val criticalCount: Int,
        val warningCount: Int,
        val evaluatedConditions: List<BlockingCondition>,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Executes the comprehensive 14-condition pre-flight audit with Failover Routing.
     */
    @JvmStatic
    fun auditTransportHealth(context: Context): TransportAuditReport {
        val conditions = mutableListOf<BlockingCondition>()
        val appContext = context.applicationContext ?: context

        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isCellularActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isWifiActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        // -------------------------------------------------------------
        // Condition 1: Airplane / Flight Mode
        // -------------------------------------------------------------
        val isAirplaneMode = try {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        } catch (e: Exception) {
            false
        }
        conditions.add(
            BlockingCondition(
                code = "AIRPLANE_MODE_ACTIVE",
                title = "Airplane Mode Active",
                isBlocked = isAirplaneMode,
                severity = Severity.CRITICAL,
                technicalReason = "Cellular transceiver and baseband radio disabled by OS Airplane mode.",
                remediation = "Disable Airplane mode in Quick Settings to restore SIM data and tower connections.",
                suggestedFailover = if (isWifiActive) FailoverRoute.TIER_2_WIFI_IP else FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 2: SIM Card Registration State
        // -------------------------------------------------------------
        val simState = tm?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN
        val isSimAbsentOrLocked = simState != TelephonyManager.SIM_STATE_READY
        conditions.add(
            BlockingCondition(
                code = "SIM_NOT_READY",
                title = "SIM Card Absent or Locked",
                isBlocked = isSimAbsentOrLocked,
                severity = Severity.CRITICAL,
                technicalReason = "SIM state code is $simState (expected SIM_STATE_READY=5).",
                remediation = "Ensure a valid registered SIM card is inserted and unlocked.",
                suggestedFailover = if (isWifiActive) FailoverRoute.TIER_2_WIFI_IP else FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 3: Mobile Data Switch
        // -------------------------------------------------------------
        val isMobileDataEnabled = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && tm != null) {
                tm.isDataEnabled
            } else {
                true
            }
        } catch (secEx: SecurityException) {
            true
        } catch (e: Exception) {
            true
        }
        conditions.add(
            BlockingCondition(
                code = "MOBILE_DATA_OFF",
                title = "Mobile Data Toggle Disabled",
                isBlocked = !isMobileDataEnabled,
                severity = Severity.CRITICAL,
                technicalReason = "Mobile Data switch is switched OFF in system settings.",
                remediation = "Enable Mobile Data under SIM & Network settings.",
                suggestedFailover = if (isWifiActive) FailoverRoute.TIER_2_WIFI_IP else FailoverRoute.TIER_4_SMS_BEACON
            )
        )

        // -------------------------------------------------------------
        // Condition 4: Android Data Saver Mode
        // -------------------------------------------------------------
        val isDataSaverBlocking = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && cm != null) {
                cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
        conditions.add(
            BlockingCondition(
                code = "DATA_SAVER_RESTRICTED",
                title = "Android Data Saver Active",
                isBlocked = isDataSaverBlocking,
                severity = Severity.CRITICAL,
                technicalReason = "OS restricts background data over cellular networks when Data Saver is active.",
                remediation = "Add jFindMe to Unrestricted Data Access whitelist in Settings > Data Saver.",
                suggestedFailover = if (isWifiActive) FailoverRoute.TIER_2_WIFI_IP else FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 5: Doze Mode & Battery Optimization
        // -------------------------------------------------------------
        val isIgnoringBatteryOptimizations = try {
            pm?.isIgnoringBatteryOptimizations(appContext.packageName) == true
        } catch (e: Exception) {
            false
        }
        conditions.add(
            BlockingCondition(
                code = "DOZE_BATTERY_RESTRICTED",
                title = "Battery Optimization Active (Doze Risk)",
                isBlocked = !isIgnoringBatteryOptimizations,
                severity = Severity.WARNING,
                technicalReason = "App is not whitelisted from Android battery optimizations. Network interface will sleep.",
                remediation = "Grant 'Don't Optimize' / 'Unrestricted' battery setting to allow 24/7 background telemetry.",
                suggestedFailover = FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 6: Background Location Permission
        // -------------------------------------------------------------
        val hasBgLoc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        conditions.add(
            BlockingCondition(
                code = "BACKGROUND_LOCATION_RESTRICTED",
                title = "Background Location Not Allowed All The Time",
                isBlocked = !hasBgLoc,
                severity = Severity.CRITICAL,
                technicalReason = "Permission ACCESS_BACKGROUND_LOCATION is missing. OS throttles location while app is closed.",
                remediation = "Set Location permission to 'Allow all the time' in App Permissions.",
                suggestedFailover = FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 7: Fine Location Permission
        // -------------------------------------------------------------
        val hasFineLoc = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        conditions.add(
            BlockingCondition(
                code = "FINE_LOCATION_PERMISSION_MISSING",
                title = "Precise Location Permission Denied",
                isBlocked = !hasFineLoc,
                severity = Severity.CRITICAL,
                technicalReason = "ACCESS_FINE_LOCATION not granted. GPS & Cell tower hardware queries blocked by OS.",
                remediation = "Grant Precise Location permission to jFindMe.",
                suggestedFailover = FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 8: Master Location Services Switch
        // -------------------------------------------------------------
        val isLocationGloballyEnabled = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && lm != null) {
                lm.isLocationEnabled
            } else if (lm != null) {
                @Suppress("DEPRECATION")
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
        conditions.add(
            BlockingCondition(
                code = "LOCATION_SERVICES_DISABLED",
                title = "System Location Switch OFF",
                isBlocked = !isLocationGloballyEnabled,
                severity = Severity.CRITICAL,
                technicalReason = "Android master Location toggle is turned OFF in Quick Settings.",
                remediation = "Enable Location in Android device settings.",
                suggestedFailover = FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 9: Active Network & Internet Capability
        // -------------------------------------------------------------
        conditions.add(
            BlockingCondition(
                code = "NO_NETWORK_CONNECTIVITY",
                title = "No Internet Connection",
                isBlocked = !hasInternet,
                severity = Severity.CRITICAL,
                technicalReason = "No active network route with NET_CAPABILITY_INTERNET. Packets cannot reach cloud.",
                remediation = "Check mobile data connectivity, Wi-Fi, or cellular balance.",
                suggestedFailover = if (!isSimAbsentOrLocked && !isAirplaneMode) FailoverRoute.TIER_4_SMS_BEACON else FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 10: Captive Portal or Unvalidated Gateway
        // -------------------------------------------------------------
        val isNetworkValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val isCaptivePortalBlocked = hasInternet && !isNetworkValidated
        conditions.add(
            BlockingCondition(
                code = "CAPTIVE_PORTAL_OR_UNVALIDATED",
                title = "Network Route Not Validated",
                isBlocked = isCaptivePortalBlocked,
                severity = Severity.WARNING,
                technicalReason = "Connected to cellular network but connection validation failed (no HTTP 204 from carrier gateway).",
                remediation = "Verify carrier APN configuration, data plan validity, or complete captive portal sign-in.",
                suggestedFailover = if (isWifiActive && isNetworkValidated) FailoverRoute.TIER_2_WIFI_IP else FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 11: Data Roaming Restriction
        // -------------------------------------------------------------
        val isRoaming = tm?.isNetworkRoaming == true
        val isRoamingDataBlocked = try {
            if (isRoaming && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.getInt(appContext.contentResolver, Settings.Global.DATA_ROAMING, 0) == 0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
        conditions.add(
            BlockingCondition(
                code = "DATA_ROAMING_RESTRICTED",
                title = "Data Roaming Disabled in Roaming Area",
                isBlocked = isRoamingDataBlocked,
                severity = Severity.WARNING,
                technicalReason = "Device is currently roaming, but Data Roaming is turned OFF in SIM settings.",
                remediation = "Turn ON Data Roaming in SIM settings if traveling.",
                suggestedFailover = if (isWifiActive) FailoverRoute.TIER_2_WIFI_IP else FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 12: Weak Cellular RF Signal (Dead-Zone)
        // -------------------------------------------------------------
        val cellData = CellTowerSentinel.extractServingCellInfo(appContext)
        val isWeakRf = cellData.signalDbm != -999 && cellData.signalDbm < -120
        conditions.add(
            BlockingCondition(
                code = "WEAK_CELLULAR_SIGNAL",
                title = "Cellular Signal Degraded (Dead-Zone)",
                isBlocked = isWeakRf,
                severity = Severity.WARNING,
                technicalReason = "Cellular signal is extremely weak (${cellData.signalDbm} dBm). Packet loss expected.",
                remediation = "Device is in an RF dead zone; location buffered in SQLite queue.",
                suggestedFailover = if (isWifiActive) FailoverRoute.TIER_2_WIFI_IP else FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 13: Firebase Realtime Database Auth State
        // -------------------------------------------------------------
        val isFirebaseAuthReady = FirebaseAuth.getInstance().currentUser != null
        conditions.add(
            BlockingCondition(
                code = "FIREBASE_AUTH_ABSENT",
                title = "Cloud Identity Standby",
                isBlocked = !isFirebaseAuthReady,
                severity = Severity.INFO,
                technicalReason = "Anonymous or registered Firebase Auth token is being provisioned.",
                remediation = "System will use cached Hardware UID / Agent ID for local SQLite & background persistence.",
                suggestedFailover = FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Condition 14: App Standby Bucket (Android 9+)
        // -------------------------------------------------------------
        val isStandbyRestricted = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val usm = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                val bucket = usm?.appStandbyBucket ?: UsageStatsManager.STANDBY_BUCKET_ACTIVE
                bucket == UsageStatsManager.STANDBY_BUCKET_RESTRICTED
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
        conditions.add(
            BlockingCondition(
                code = "APP_STANDBY_BUCKET_RESTRICTED",
                title = "App Placed in Restricted Standby Bucket",
                isBlocked = isStandbyRestricted,
                severity = Severity.WARNING,
                technicalReason = "Android OS placed jFindMe in RESTRICTED standby bucket, limiting background alarms/jobs.",
                remediation = "Open the app or disable adaptive battery restrictions for jFindMe in device settings.",
                suggestedFailover = FailoverRoute.TIER_3_SQLITE_BURST
            )
        )

        // -------------------------------------------------------------
        // Interchangeable Failover Route Determination
        // -------------------------------------------------------------
        val criticalBlocks = conditions.filter { it.isBlocked && it.severity == Severity.CRITICAL }
        val warningBlocks = conditions.filter { it.isBlocked && it.severity == Severity.WARNING }

        val canShareViaSim = isCellularActive && hasInternet && !isAirplaneMode && !isSimAbsentOrLocked && !isDataSaverBlocking

        val recommendedRoute = when {
            canShareViaSim -> FailoverRoute.TIER_1_CELLULAR_DATA
            isWifiActive && hasInternet -> FailoverRoute.TIER_2_WIFI_IP
            !isAirplaneMode && !isSimAbsentOrLocked -> FailoverRoute.TIER_4_SMS_BEACON
            else -> FailoverRoute.TIER_3_SQLITE_BURST
        }

        val overallStatus = when {
            canShareViaSim -> "OPTIMAL_CELLULAR"
            isWifiActive && hasInternet -> "FAILOVER_WIFI_ACTIVE"
            recommendedRoute == FailoverRoute.TIER_4_SMS_BEACON -> "FAILOVER_SMS_ACTIVE"
            else -> "OFFLINE_BUFFER_SQLITE"
        }

        SystemLogger.i(
            TAG,
            "Transport Audit: status=$overallStatus, route=$recommendedRoute, canShareViaSim=$canShareViaSim, critical=${criticalBlocks.size}, warnings=${warningBlocks.size}"
        )

        return TransportAuditReport(
            canShareViaSim = canShareViaSim,
            isCellularActive = isCellularActive,
            isWifiActive = isWifiActive,
            recommendedRoute = recommendedRoute,
            overallStatus = overallStatus,
            criticalCount = criticalBlocks.size,
            warningCount = warningBlocks.size,
            evaluatedConditions = conditions
        )
    }
}
