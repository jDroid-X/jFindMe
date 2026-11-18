package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] id, pointId, deviceId, latitude, longitude, speedKmh, distanceMeters, etaSeconds, batteryPct, isSos, ragStatus, ragReason, timestamp
 * [Constants] TABLE_NAME = "butterfly_point_telemetry", COL_*
 * [Dependencies] android.content.ContentValues, android.database.sqlite.SQLiteDatabase
 * [Integration Points] DatabaseHelper.kt, ButterflyPointEngine.kt, TrackingService.kt, Firebase RTDB /butterfly_points/{pointId}/telemetry/{deviceId}
 *
 * Tier 3: Transactional Database Table for Real-Time Rendezvous Telemetry Streams & Breadcrumbs
 */
object ButterflyPointTelemetryTable {
    const val TABLE_NAME = "butterfly_point_telemetry"

    const val COL_ID = "id"
    const val COL_POINT_ID = "point_id"
    const val COL_DEVICE_ID = "device_id"
    const val COL_LAT = "latitude"
    const val COL_LNG = "longitude"
    const val COL_SPEED_KMH = "speed_kmh"
    const val COL_DISTANCE_METERS = "distance_meters"
    const val COL_ETA_SECONDS = "eta_seconds"
    const val COL_BATTERY_PCT = "battery_pct"
    const val COL_IS_SOS = "is_sos"
    const val COL_RAG_STATUS = "rag_status"
    const val COL_RAG_REASON = "rag_reason"
    const val COL_TIMESTAMP = "timestamp"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_POINT_ID TEXT NOT NULL,
            $COL_DEVICE_ID TEXT NOT NULL,
            $COL_LAT REAL DEFAULT 0.0,
            $COL_LNG REAL DEFAULT 0.0,
            $COL_SPEED_KMH REAL DEFAULT 0.0,
            $COL_DISTANCE_METERS REAL DEFAULT 0.0,
            $COL_ETA_SECONDS INTEGER DEFAULT 0,
            $COL_BATTERY_PCT INTEGER DEFAULT -1,
            $COL_IS_SOS INTEGER DEFAULT 0,
            $COL_RAG_STATUS TEXT DEFAULT 'GREEN',
            $COL_RAG_REASON TEXT,
            $COL_TIMESTAMP INTEGER NOT NULL
        )
    """.trimIndent()

    val CREATE_INDEX_POINT_DEVICE = """
        CREATE INDEX IF NOT EXISTS idx_bpt_point_dev ON $TABLE_NAME ($COL_POINT_ID, $COL_DEVICE_ID, $COL_TIMESTAMP)
    """.trimIndent()

    @JvmStatic
    fun logTelemetry(
        db: SQLiteDatabase,
        pointId: String,
        deviceId: String,
        lat: Double,
        lng: Double,
        speedKmh: Float,
        distanceMeters: Double,
        etaSeconds: Int,
        batteryPct: Int,
        isSos: Boolean,
        ragStatus: String,
        ragReason: String?,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        if (pointId.isBlank() || deviceId.isBlank()) return -1L
        val values = ContentValues().apply {
            put(COL_POINT_ID, pointId)
            put(COL_DEVICE_ID, deviceId)
            put(COL_LAT, lat)
            put(COL_LNG, lng)
            put(COL_SPEED_KMH, speedKmh)
            put(COL_DISTANCE_METERS, distanceMeters)
            put(COL_ETA_SECONDS, etaSeconds)
            put(COL_BATTERY_PCT, batteryPct)
            put(COL_IS_SOS, if (isSos) 1 else 0)
            put(COL_RAG_STATUS, ragStatus)
            put(COL_RAG_REASON, ragReason)
            put(COL_TIMESTAMP, timestamp)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    @JvmStatic
    fun purgeExpiredTelemetry(db: SQLiteDatabase, retentionMs: Long): Int {
        val cutoff = System.currentTimeMillis() - retentionMs
        return db.delete(TABLE_NAME, "$COL_TIMESTAMP < ?", arrayOf(cutoff.toString()))
    }
}
