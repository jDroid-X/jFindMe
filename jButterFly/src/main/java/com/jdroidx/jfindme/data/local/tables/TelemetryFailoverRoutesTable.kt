package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, identityId, hardwareId, routeName, priorityTier, state, rttLatencyMs, packetLossRate, consecutiveFailures, lastTrippedTs, lastRecoveryTs, createdOn
 * Constants: TABLE_NAME = "telemetry_failover_routes", COL_ID, COL_IDENTITY_ID, COL_HARDWARE_ID, COL_ROUTE_NAME, COL_PRIORITY_TIER, COL_STATE, COL_RTT_LATENCY_MS, COL_PACKET_LOSS_RATE, COL_CONSECUTIVE_FAILURES, COL_LAST_TRIPPED_TS, COL_LAST_RECOVERY_TS, COL_CREATED_ON
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, TelemetryFailoverRouter.kt, TelemetryClosedLoopSentinel.kt, HealthActivity.kt
 */
object TelemetryFailoverRoutesTable {
    const val TABLE_NAME = "telemetry_failover_routes"

    const val COL_ID = "id"
    const val COL_IDENTITY_ID = "identity_id" // FK to UserIdentityTable.id
    const val COL_HARDWARE_ID = "hardware_id"
    const val COL_ROUTE_NAME = "route_name" // N1_CELLULAR_FAST, N2_WIFI_BROADBAND, N3_UDP_BURST, N4_P2P_MESH, N5_SMS_FALLBACK
    const val COL_PRIORITY_TIER = "priority_tier" // 1 to 5
    const val COL_STATE = "state" // ACTIVE, TRIPPED, COOLING_DOWN, DISABLED
    const val COL_RTT_LATENCY_MS = "rtt_latency_ms"
    const val COL_PACKET_LOSS_RATE = "packet_loss_rate"
    const val COL_CONSECUTIVE_FAILURES = "consecutive_failures"
    const val COL_LAST_TRIPPED_TS = "last_tripped_ts"
    const val COL_LAST_RECOVERY_TS = "last_recovery_ts"
    const val COL_CREATED_ON = "created_on"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_HARDWARE_ID TEXT NOT NULL,
            $COL_ROUTE_NAME TEXT NOT NULL,
            $COL_PRIORITY_TIER INTEGER NOT NULL,
            $COL_STATE TEXT DEFAULT 'ACTIVE',
            $COL_RTT_LATENCY_MS INTEGER DEFAULT 0,
            $COL_PACKET_LOSS_RATE REAL DEFAULT 0.0,
            $COL_CONSECUTIVE_FAILURES INTEGER DEFAULT 0,
            $COL_LAST_TRIPPED_TS INTEGER DEFAULT 0,
            $COL_LAST_RECOVERY_TS INTEGER DEFAULT 0,
            $COL_CREATED_ON TEXT,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    fun logRouteHealth(
        db: SQLiteDatabase,
        identityId: Long,
        hardwareId: String,
        routeName: String,
        priorityTier: Int,
        state: String,
        rttLatencyMs: Long,
        packetLossRate: Float,
        consecutiveFailures: Int
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()
        val cv = ContentValues().apply {
            put(COL_IDENTITY_ID, identityId)
            put(COL_HARDWARE_ID, hardwareId)
            put(COL_ROUTE_NAME, routeName)
            put(COL_PRIORITY_TIER, priorityTier)
            put(COL_STATE, state)
            put(COL_RTT_LATENCY_MS, rttLatencyMs)
            put(COL_PACKET_LOSS_RATE, packetLossRate.toDouble())
            put(COL_CONSECUTIVE_FAILURES, consecutiveFailures)
            if (state == "TRIPPED") {
                put(COL_LAST_TRIPPED_TS, System.currentTimeMillis())
            } else if (state == "ACTIVE") {
                put(COL_LAST_RECOVERY_TS, System.currentTimeMillis())
            }
            put(COL_CREATED_ON, now)
        }
        return db.insert(TABLE_NAME, null, cv)
    }
}

