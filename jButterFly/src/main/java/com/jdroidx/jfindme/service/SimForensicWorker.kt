package com.jdroidx.jfindme.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.data.repository.LocationRepository
import com.jdroidx.jfindme.engine.location.CellTowerSentinel
import com.jdroidx.jfindme.engine.security.SimTransportValidator
import com.jdroidx.jfindme.engine.sync.TelemetryFailoverRouter
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger
import java.util.concurrent.TimeUnit

/**
 * 📱 SimForensicWorker: Autonomous Background Location & Hardware Sentinel (Enterprise v5.0)
 *
 * Runs via WorkManager even when the app is completely closed/killed.
 * Acquires cell tower / GNSS fused coordinates, audits all conditions blocking SIM/data sharing,
 * and routes location through the multi-tier Interchangeable TelemetryFailoverRouter.
 *
 * [Variables]
 * - cellData: Active serving cell info (MCC, MNC, LAC/TAC, CID, Signal dBm)
 * - transportReport: 14-condition transport validation diagnostic report
 * - resolvedLat, resolvedLng, resolvedAcc, resolvedSpeed, resolvedBearing
 *
 * [Constants]
 * - TAG = "SIMForensicWorker"
 *
 * [Dependencies]
 * - com.jdroidx.jfindme.engine.location.CellTowerSentinel
 * - com.jdroidx.jfindme.engine.security.SimTransportValidator
 * - com.jdroidx.jfindme.engine.sync.TelemetryFailoverRouter
 * - com.jdroidx.jfindme.data.local.DatabaseHelper
 * - com.jdroidx.jfindme.data.remote.FirebaseComm
 *
 * [Integration Points]
 * - BackgroundJobManager.kt: Orchestrates periodic execution (default: 15 mins)
 * - SQLite SimTable & sim_transactions: Real-time cellular breadcrumb persistence
 * - Firebase RTDB /devices/{id}/telemetry: Live cloud heartbeat & transport health
 */
class SimForensicWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "SIMForensicWorker"
    }

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        
        if (prefs.getBoolean("pref_service_halted", false)) {
            SystemLogger.w(TAG, "Health Engine Suspended.")
            return Result.success()
        }

        SystemLogger.i(TAG, "Autonomous SIM & Telemetry pulse executing in background.")
        
        return try {
            val dbHelper = DatabaseHelper.getInstance(context)
            val hwId = DeviceIdentityHelper.getHardwareUid(context)
            val agentId = DeviceIdentityHelper.getOrGenerateAgentId(context)
            val now = System.currentTimeMillis()
            val timeStampSign = FirebaseComm.getForensicTimestamp(now)

            // =================================================================
            // 1. Audit All 14 Conditions Blocking SIM Location Sharing
            // =================================================================
            val transportReport = SimTransportValidator.auditTransportHealth(context)
            SystemLogger.d(TAG, "Transport Audit Status: ${transportReport.overallStatus} (Route: ${transportReport.recommendedRoute})")

            // =================================================================
            // 2. Extract Real Serving Cell Tower & SIM Hardware Identifiers
            // =================================================================
            val cellData = CellTowerSentinel.extractServingCellInfo(context)
            SystemLogger.d(TAG, "Cell Tower Telemetry: operator=${cellData.operator}, mcc=${cellData.mcc}, mnc=${cellData.mnc}, lac=${cellData.lac}, cid=${cellData.cid}, dbm=${cellData.signalDbm}")

            // Log real SIM Health status locally into SQLite SimTable
            dbHelper.logSimCard(
                cellData.slot,
                hwId,
                cellData.operator,
                cellData.mcc,
                cellData.mnc,
                cellData.lac,
                cellData.cid,
                cellData.isRegistered
            )

            // =================================================================
            // 3. Proactively Acquire Location Fix (Fused GNSS + Network Provider)
            // =================================================================
            var resolvedLat = 0.0
            var resolvedLng = 0.0
            var resolvedAcc = 100f
            var resolvedAlt = 0.0
            var resolvedSpeed = 0f
            var resolvedBearing = 0f

            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val lastLocTask = fusedClient.lastLocation
                val loc = Tasks.await(lastLocTask, 5, TimeUnit.SECONDS)
                if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                    resolvedLat = loc.latitude
                    resolvedLng = loc.longitude
                    resolvedAcc = loc.accuracy
                    resolvedAlt = loc.altitude
                    resolvedSpeed = loc.speed
                    resolvedBearing = loc.bearing
                }
            } catch (locEx: Exception) {
                SystemLogger.w(TAG, "Fused location fetch error in worker: ${locEx.message}")
            }

            // Fallback to SQLite last saved position if GPS hardware is completely cold
            if (resolvedLat == 0.0 && resolvedLng == 0.0) {
                resolvedLat = RegistryManager.getLastKnownLatitude(context)
                resolvedLng = RegistryManager.getLastKnownLongitude(context)
            }

            // =================================================================
            // 4. Commit Cellular Transaction to Local Database
            // =================================================================
            val transactionSource = if (cellData.hasValidTowerData) "CELL_TOWER_${cellData.networkType}" else "CELL_SENTINEL_FIX"
            dbHelper.logSimTransaction(
                cellData.slot,
                cellData.mcc,
                cellData.mnc,
                cellData.lac,
                cellData.cid,
                resolvedLat,
                resolvedLng,
                transactionSource
            )

            // =================================================================
            // 5. Interchangeable Failover Telemetry Dispatch
            // =================================================================
            if (resolvedLat != 0.0 && resolvedLng != 0.0) {
                val ssotAvatar = dbHelper.getDeviceCentricAvatar(agentId)
                val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
                val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
                val photoSeq = prefs.getInt("custom_avatar_seq", 1)
                val customAvatarTag = String.format(java.util.Locale.US, "AV_%s_%02d", agentId, photoSeq)
                val resolvedAvatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly") {
                    ssotAvatar
                } else if (dynamicIdx == -1) {
                    customAvatarTag
                } else if (dynamicIdx > 0) {
                    "avatar_custom_$dynamicIdx"
                } else {
                    "butterfly"
                }

                val userLoc = UserLocation(
                    userId = agentId,
                    deviceId = hwId,
                    latitude = resolvedLat,
                    longitude = resolvedLng,
                    geohash = com.jdroidx.jfindme.engine.location.GeohashHelper.encode(resolvedLat, resolvedLng),
                    bearing = resolvedBearing,
                    speed = resolvedSpeed,
                    accuracy = resolvedAcc,
                    altitude = resolvedAlt,
                    timestamp = timeStampSign,
                    allowDependents = prefs.getBoolean("allow_dependents", true),
                    active_group = prefs.getString("active_group", "") ?: "",
                    isForensicAudit = true,
                    userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker",
                    avatar_id = resolvedAvatarId
                )

                // Dispatch via TelemetryFailoverRouter across interchangeable tiers (Cellular -> Wi-Fi -> SMS -> SQLite)
                val dispatchResult = TelemetryFailoverRouter.dispatchLocationWithFailover(context, userLoc)
                SystemLogger.i(TAG, "Failover Dispatch Result: [${dispatchResult.routeLabel}] - ${dispatchResult.detailMessage}")

                RegistryManager.setLastKnownLatitude(context, resolvedLat)
                RegistryManager.setLastKnownLongitude(context, resolvedLng)
            }

            // =================================================================
            // 6. Resurrect TrackingService if System Terminated It
            // =================================================================
            try {
                val serviceIntent = android.content.Intent(context, TrackingService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (srvEx: Exception) {
                SystemLogger.d(TAG, "Service restart deferred: ${srvEx.message}")
            }

            Result.success()
        } catch (e: Exception) {
            SystemLogger.e(TAG, "SIM Forensic worker execution failed: ${e.message}")
            Result.retry()
        }
    }
}
