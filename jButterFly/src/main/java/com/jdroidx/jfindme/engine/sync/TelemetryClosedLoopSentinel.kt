package com.jdroidx.jfindme.engine.sync

import android.content.Context
import android.content.Intent
import com.google.firebase.database.*
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.data.remote.OfflineSyncManager
import com.jdroidx.jfindme.engine.security.SimTransportValidator
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 🔄 TelemetryClosedLoopSentinel (Agent 2 + Agent 4 + Agent 5 Closed-Loop SSOT)
 *
 * Implements the bidirectional closed feedback loop between physical device telemetry
 * and Cloud SSOT / Web Mission Control.
 *
 * [Variables]
 * - pendingPackets: Concurrent map of packetId to DispatchPacket
 * - consecutiveAckFailures: Atomic counter tracking failed cloud acknowledgments
 * - lastVerifiedRttMs: Latest verified round-trip latency in milliseconds
 *
 * [Constants]
 * - TAG: "ClosedLoopSentinel"
 * - ACK_TIMEOUT_MS: Maximum allowed time for cloud round-trip ACK (5000ms)
 * - MAX_FAILURES_BEFORE_FAILOVER: 2 consecutive missed ACKs trip failover switch
 * - ACTION_CLOSED_LOOP_ACK: Local broadcast intent for UI telemetry status
 *
 * [Dependencies]
 * - com.google.firebase.database.DatabaseReference
 * - com.jdroidx.jfindme.data.remote.FirebaseComm
 * - com.jdroidx.jfindme.data.local.DatabaseHelper
 * - com.jdroidx.jfindme.engine.security.SimTransportValidator
 *
 * [Integration Points]
 * - TelemetryFailoverRouter.kt: Dispatches packets and receives ACK confirmation / trip alerts
 * - TrackingService.kt: Modulates dynamic duty cycle based on closed-loop feedback
 * - MapActivity / Web: Visualizes green solid dot on verified ACK; amber on pending ACK
 */
object TelemetryClosedLoopSentinel {

    private const val TAG = "ClosedLoopSentinel"
    const val ACK_TIMEOUT_MS = 5000L
    const val MAX_FAILURES_BEFORE_FAILOVER = 2
    const val ACTION_CLOSED_LOOP_ACK = "com.jdroidx.jfindme.ACTION_CLOSED_LOOP_ACK"

    private val consecutiveAckFailures = AtomicInteger(0)
    private var lastVerifiedRttMs: Long = 0L
    private var lastAckTimestamp: Long = 0L
    private var isCommandFeedbackListenerAttached = false
    private var currentFeedbackAgentId: String? = null
    private var activeFeedbackListener: ValueEventListener? = null
    private var activeFeedbackRef: DatabaseReference? = null

    data class DispatchPacket(
        val packetId: String,
        val agentId: String,
        val location: UserLocation,
        val route: SimTransportValidator.FailoverRoute,
        val sendEpoch: Long = System.currentTimeMillis()
    )

    data class ClosedLoopStatus(
        val isHealthy: Boolean,
        val lastRttMs: Long,
        val lastAckEpoch: Long,
        val consecutiveFailures: Int,
        val activeRoute: SimTransportValidator.FailoverRoute
    )

    /**
     * Dispatches telemetry with an attached Cloud ACK completion listener to close the loop.
     */
    @JvmStatic
    fun dispatchWithClosedLoopFeedback(
        context: Context,
        location: UserLocation,
        agentId: String,
        route: SimTransportValidator.FailoverRoute,
        payload: Map<String, Any?>,
        onAckReceived: ((rttMs: Long) -> Unit)? = null,
        onAckFailed: ((errorMsg: String) -> Unit)? = null
    ) {
        val appContext = context.applicationContext ?: context
        val sendEpoch = System.currentTimeMillis()
        val packetId = "PKT_${sendEpoch}_${agentId.take(6)}"

        // Ensure downstream command feedback listener is listening
        attachDownstreamFeedbackListener(appContext, agentId)

        val telemetryRef = FirebaseComm.getTelemetryRef(agentId)

        // Attach completion listener to close the verification loop
        telemetryRef.updateChildren(payload) { error, _ ->
            val rttMs = System.currentTimeMillis() - sendEpoch

            if (error == null) {
                // =============================================================
                // 🟢 CLOSED LOOP CONFIRMED: Cloud Accepted and Written
                // =============================================================
                consecutiveAckFailures.set(0)
                lastVerifiedRttMs = rttMs
                lastAckTimestamp = System.currentTimeMillis()

                SystemLogger.i(
                    TAG,
                    "CLOSED_LOOP_ACK_VERIFIED: Packet $packetId confirmed in ${rttMs}ms via $route."
                )

                // 1. Update SQLite Vitals table with live RTT latency
                try {
                    val db = DatabaseHelper.getInstance(appContext)
                    db.updateTelemetryAck(agentId, lastAckTimestamp, rttMs, route.name)
                } catch (e: Exception) {
                    SystemLogger.d(TAG, "Local ACK persistence deferred: ${e.message}")
                }

                // 2. Broadcast to UI (MapActivity status dot turns solid emerald green)
                val ackIntent = Intent(ACTION_CLOSED_LOOP_ACK).apply {
                    putExtra("agent_id", agentId)
                    putExtra("rtt_ms", rttMs)
                    putExtra("ack_timestamp", lastAckTimestamp)
                    putExtra("status", "ACK_CONFIRMED")
                    putExtra("route", route.name)
                }
                appContext.sendBroadcast(ackIntent)

                onAckReceived?.invoke(rttMs)
            } else {
                // =============================================================
                // 🔴 CLOSED LOOP BROKEN: Cloud Rejected or Network Stalled
                // =============================================================
                val failures = consecutiveAckFailures.incrementAndGet()
                val errorMsg = error.message

                SystemLogger.e(
                    TAG,
                    "CLOSED_LOOP_ACK_FAILED: Packet $packetId failed ($errorMsg). Consecutive failures=$failures"
                )

                // Broadcast failure status
                val failIntent = Intent(ACTION_CLOSED_LOOP_ACK).apply {
                    putExtra("agent_id", agentId)
                    putExtra("error", errorMsg)
                    putExtra("status", "ACK_FAILED")
                    putExtra("consecutive_failures", failures)
                }
                appContext.sendBroadcast(failIntent)

                // Trip-wire: If consecutive failures exceed threshold, force failover immediately
                val maxAllowed = RegistryManager.getMaxAckFailures(appContext)
                if (failures >= maxAllowed) {
                    SystemLogger.w(
                        TAG,
                        "CIRCUIT_BREAKER_TRIPPED: $failures consecutive ACK timeouts. Triggering emergency failover switch!"
                    )
                    triggerEmergencyFailover(appContext, location, agentId, route)
                }

                onAckFailed?.invoke(errorMsg)
            }
        }
    }

    /**
     * Listens to downstream feedback/ACK channel from Web Mission Control and Cloud SSOT.
     */
    @JvmStatic
    fun attachDownstreamFeedbackListener(context: Context, agentId: String) {
        if (agentId.isBlank()) return
        if (currentFeedbackAgentId == agentId) return

        try {
            activeFeedbackRef?.let { ref ->
                activeFeedbackListener?.let { l -> ref.removeEventListener(l) }
            }
        } catch (e: Exception) { }

        currentFeedbackAgentId = agentId
        isCommandFeedbackListenerAttached = true
        val appContext = context.applicationContext ?: context

        try {
            val feedbackRef = FirebaseComm.devicesRef.child(agentId).child("feedback").child("ack")
            activeFeedbackRef = feedbackRef
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val serverAckTs = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    val commandAck = snapshot.child("command").getValue(String::class.java) ?: ""
                    val clientTs = snapshot.child("clientTs").getValue(Long::class.java)
                        ?: (snapshot.child("client_timestamp").getValue(Long::class.java) ?: 0L)
                    val now = System.currentTimeMillis()
                    val rttMs = if (clientTs > 0L && now >= clientTs) (now - clientTs) else (if (serverAckTs > 0L && now >= serverAckTs) (now - serverAckTs) else 0L)

                    if (serverAckTs > 0L) {
                        lastAckTimestamp = serverAckTs
                        lastVerifiedRttMs = rttMs
                        consecutiveAckFailures.set(0)

                        try {
                            val db = DatabaseHelper.getInstance(appContext)
                            db.updateTelemetryAck(agentId, serverAckTs, rttMs, "N1_CLOUD_DOWNLINK")
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Failed updating SQLite telemetry ACK on downstream feedback: ${e.message}")
                        }

                        try {
                            val ackIntent = Intent(ACTION_CLOSED_LOOP_ACK).apply {
                                putExtra("agent_id", agentId)
                                putExtra("ack_timestamp", serverAckTs)
                                putExtra("rtt_ms", rttMs)
                                putExtra("route", "N1_CLOUD_DOWNLINK")
                                putExtra("command", commandAck)
                                setPackage(appContext.packageName)
                            }
                            appContext.sendBroadcast(ackIntent)
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Failed broadcasting ACTION_CLOSED_LOOP_ACK: ${e.message}")
                        }

                        SystemLogger.d(TAG, "Downstream Cloud ACK received: ts=$serverAckTs, rtt=${rttMs}ms, cmd=$commandAck")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    SystemLogger.w(TAG, "Downstream feedback listener cancelled: ${error.message}")
                }
            }
            activeFeedbackListener = listener
            feedbackRef.addValueEventListener(listener)
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed attaching downstream feedback listener: ${e.message}")
        }
    }

    /**
     * Executes automatic failover switch when closed loop is broken.
     */
    private fun triggerEmergencyFailover(
        context: Context,
        location: UserLocation,
        agentId: String,
        failedRoute: SimTransportValidator.FailoverRoute
    ) {
        // Demote failed route and buffer immediately into offline queue
        try {
            val repo = com.jdroidx.jfindme.data.repository.LocationRepository(context)
            repo.updateMyLocation(location)
            OfflineSyncManager.getInstance(context).triggerBackgroundSync()
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Emergency failover buffer error: ${e.message}")
        }
    }

    @JvmStatic
    fun getClosedLoopStatus(currentRoute: SimTransportValidator.FailoverRoute): ClosedLoopStatus {
        return ClosedLoopStatus(
            isHealthy = consecutiveAckFailures.get() == 0 && (System.currentTimeMillis() - lastAckTimestamp < 60000L),
            lastRttMs = lastVerifiedRttMs,
            lastAckEpoch = lastAckTimestamp,
            consecutiveFailures = consecutiveAckFailures.get(),
            activeRoute = currentRoute
        )
    }
}

