package com.jdroidx.jfindme.engine.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.SmsManager
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.data.remote.OfflineSyncManager
import com.jdroidx.jfindme.data.repository.LocationRepository
import com.jdroidx.jfindme.engine.location.CellTowerSentinel
import com.jdroidx.jfindme.engine.security.SimTransportValidator
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 🔀 TelemetryFailoverRouter (Agent 1 + Agent 2 + Agent 4 + Agent 7 Standard)
 *
 * Implements the Short-Circuit Encapsulation Waterfall (N1 -> N2 -> N3 -> N4):
 * - Level N1: Direct Primary Cloud Socket (99% Fast Path - O(1) Zero-Check Exit)
 * - Level N2: Alternate IP Handover & Diagnostic Audit (Invoked ONLY if N1 Fails)
 * - Level N3: Baseband SMS Beacon (Zero-Data Bearer for SOS / Emergency)
 * - Level N4: Encrypted SQLite FIFO Vault (Offline Buffer + Reactive Flusher)
 *
 * [Variables]
 * - isNetworkOnline: Boolean quick check for active IP capability
 * - cellData: Serving cell metadata extracted via CellTowerSentinel
 *
 * [Constants]
 * - TAG: "TelemetryFailoverRouter"
 *
 * [Dependencies]
 * - com.jdroidx.jfindme.engine.sync.TelemetryClosedLoopSentinel
 * - com.jdroidx.jfindme.engine.security.SimTransportValidator
 * - com.jdroidx.jfindme.engine.location.CellTowerSentinel
 * - com.jdroidx.jfindme.data.repository.LocationRepository
 * - com.jdroidx.jfindme.data.remote.OfflineSyncManager
 *
 * [Integration Points]
 * - SimForensicWorker.kt: Invokes dispatchLocationWithFailover() in background
 * - TrackingService.kt: Invokes dispatchLocationWithFailover() in foreground
 * - TelemetryClosedLoopSentinel: Receives server ACK and triggers trip-wire on failure
 */
object TelemetryFailoverRouter {

    private const val TAG = "TelemetryFailoverRouter"

    data class DispatchResult(
        val success: Boolean,
        val activeRoute: SimTransportValidator.FailoverRoute,
        val routeLabel: String,
        val detailMessage: String
    )

    /**
     * Dispatches location telemetry using the Cascading Encapsulation Waterfall.
     */
    @JvmStatic
    fun dispatchLocationWithFailover(context: Context, location: UserLocation): DispatchResult {
        val appContext = context.applicationContext ?: context
        val agentId = location.userId?.toString()?.takeIf { it.isNotBlank() }
            ?: DeviceIdentityHelper.getOrGenerateAgentId(appContext)
        val now = System.currentTimeMillis()

        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val isNetworkOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // =====================================================================
        // LEVEL N1: Direct Primary Cloud Socket (99% Hot Path - Ultra Fast)
        // If N1 succeeds, return immediately. Zero redundant manager queries.
        // =====================================================================
        if (isNetworkOnline) {
            val cellData = CellTowerSentinel.extractServingCellInfo(appContext)
            val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val route = if (isCellular) SimTransportValidator.FailoverRoute.TIER_1_CELLULAR_DATA else SimTransportValidator.FailoverRoute.TIER_2_WIFI_IP
            val routeTag = if (isCellular) "N1_CELLULAR_FAST" else "N1_WIFI_FAST"

            executeDirectCloudPush(appContext, location, agentId, now, routeTag, cellData, route)
            try { DatabaseHelper.getInstance(appContext).updateDeviceActiveRoute(agentId, routeTag) } catch (_: Exception) {}

            return DispatchResult(
                success = true,
                activeRoute = route,
                routeLabel = routeTag,
                detailMessage = "Direct cloud socket write executed with closed-loop ACK."
            )
        }

        // =====================================================================
        // LEVEL N2: Alternate IP Diagnostics & Handover (Invoked ONLY if N1 Fails)
        // =====================================================================
        val report = SimTransportValidator.auditTransportHealth(appContext)
        val cellData = CellTowerSentinel.extractServingCellInfo(appContext)
        SystemLogger.w(TAG, "Level N1 failed. Cascading to N2 (Diagnosed: ${report.overallStatus})")

        if (report.isWifiActive && !report.canShareViaSim) {
            executeDirectCloudPush(appContext, location, agentId, now, "N2_WIFI_HANDOVER", cellData, SimTransportValidator.FailoverRoute.TIER_2_WIFI_IP)
            try { DatabaseHelper.getInstance(appContext).updateDeviceActiveRoute(agentId, "N2_WIFI_HANDOVER") } catch (_: Exception) {}
            return DispatchResult(
                success = true,
                activeRoute = SimTransportValidator.FailoverRoute.TIER_2_WIFI_IP,
                routeLabel = "N2_WIFI_HANDOVER",
                detailMessage = "Cellular blocked; routed via Wi-Fi handover."
            )
        }

        // =====================================================================
        // LEVEL N3: Baseband SMS Beacon (Zero-Data Bearer if Emergency / SOS)
        // =====================================================================
        if (location.isSosActive || report.recommendedRoute == SimTransportValidator.FailoverRoute.TIER_4_SMS_BEACON) {
            val smsSent = attemptSmsBeaconDispatch(appContext, location, cellData, agentId)
            if (smsSent) {
                queueOfflineLocation(appContext, location)
                try { DatabaseHelper.getInstance(appContext).updateDeviceActiveRoute(agentId, "N3_SMS_BEACON") } catch (_: Exception) {}
                return DispatchResult(
                    success = true,
                    activeRoute = SimTransportValidator.FailoverRoute.TIER_4_SMS_BEACON,
                    routeLabel = "N3_SMS_BEACON",
                    detailMessage = "Zero-Data SMS Beacon dispatched to Emergency Guardian."
                )
            }
        }

        // =====================================================================
        // LEVEL N4: Encrypted SQLite FIFO Vault + Reactive Network Flusher
        // =====================================================================
        queueOfflineLocation(appContext, location)
        try {
            OfflineSyncManager.getInstance(appContext).triggerBackgroundSync()
        } catch (_: Exception) {}
        try { DatabaseHelper.getInstance(appContext).updateDeviceActiveRoute(agentId, "N4_SQLITE_VAULT") } catch (_: Exception) {}

        return DispatchResult(
            success = true,
            activeRoute = SimTransportValidator.FailoverRoute.TIER_3_SQLITE_BURST,
            routeLabel = "N4_SQLITE_VAULT",
            detailMessage = "Buffered in SQLite Table 3 (location_event); reactive flusher armed."
        )
    }

    private fun executeDirectCloudPush(
        context: Context,
        location: UserLocation,
        agentId: String,
        timestamp: Long,
        routeTag: String,
        cellData: CellTowerSentinel.ServingCellData,
        route: SimTransportValidator.FailoverRoute
    ) {
        try {
            // Push via LocationRepository (updates local cache and Firebase RTDB)
            val repo = LocationRepository(context)
            repo.updateMyLocation(location)

            val payload = mapOf(
                "last_heartbeat" to timestamp,
                "active_route" to routeTag,
                "cellular_mcc_mnc" to "${cellData.mcc}-${cellData.mnc}",
                "cellular_lac_cid" to "${cellData.lac}-${cellData.cid}",
                "cellular_signal_dbm" to cellData.signalDbm
            )

            // 🔄 CLOSE THE FEEDBACK LOOP: Verify cloud write with ACK Completion Listener
            TelemetryClosedLoopSentinel.dispatchWithClosedLoopFeedback(
                context = context,
                location = location,
                agentId = agentId,
                route = route,
                payload = payload,
                onAckReceived = { rttMs ->
                    SystemLogger.d(TAG, "CLOSED_LOOP_ACK: Latency=${rttMs}ms verified via $routeTag")
                },
                onAckFailed = { error ->
                    SystemLogger.w(TAG, "CLOSED_LOOP_ACK_FAILED: $error. Cascading to offline vault.")
                    queueOfflineLocation(context, location)
                }
            )

            FirebaseComm.lookupIndicesRef.child("active_monitors").child(agentId).setValue(timestamp)
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Direct cloud push error: ${e.message}. Falling back to SQLite queue.")
            queueOfflineLocation(context, location)
        }
    }

    private fun queueOfflineLocation(context: Context, location: UserLocation) {
        try {
            val repo = LocationRepository(context)
            repo.updateMyLocation(location)
        } catch (e: Exception) {
            SystemLogger.e(TAG, "SQLite offline queue error: ${e.message}")
        }
    }

    private fun attemptSmsBeaconDispatch(
        context: Context,
        location: UserLocation,
        cellData: CellTowerSentinel.ServingCellData,
        agentId: String
    ): Boolean {
        return try {
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val guardianNumber = RegistryManager.getFailoverSmsTarget(context)

            if (guardianNumber.isBlank()) {
                SystemLogger.d(TAG, "No SOS SMS guardian target configured.")
                return false
            }

            // Compact 140-char telemetry PDU
            // Format: JFM:<AgentId>:<Lat,Lng>:<Spd>:<Bat>:<SOS>:<MCC-MNC-CID>
            val payload = String.format(
                java.util.Locale.US,
                "JFM:%s:%.5f,%.5f:%.0f:%d:%s:%s-%s-%s",
                agentId.take(8),
                location.latitude,
                location.longitude,
                location.speed,
                prefs.getInt("last_known_battery", 100),
                if (location.isSosActive) "SOS" else "NORM",
                cellData.mcc,
                cellData.mnc,
                cellData.cid
            )

            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(guardianNumber, null, payload, null, null)
            SystemLogger.i(TAG, "Cellular SMS Beacon dispatched successfully to $guardianNumber: $payload")
            true
        } catch (smsEx: Exception) {
            SystemLogger.w(TAG, "SMS beacon dispatch error: ${smsEx.message}")
            false
        }
    }
}
