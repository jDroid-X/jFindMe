package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, payloadType, targetUid, lat, lng, bearing, speed, battery, isSos, timestamp, status, retryCount, extraData, accuracy, altitude, motionState, createdOn, lastAttemptTs, errorMessage
 * Constants: TABLE_NAME = "pending_sync_queue", COL_ID, COL_PAYLOAD_TYPE, COL_TARGET_UID, COL_LAT, COL_LNG, COL_BEARING, COL_SPEED, COL_BATTERY, COL_IS_SOS, COL_TIMESTAMP, COL_STATUS, COL_RETRY_COUNT, COL_EXTRA_DATA, COL_ACCURACY, COL_ALTITUDE, COL_MOTION_STATE, COL_CREATED_ON, COL_LAST_ATTEMPT_TS, COL_ERROR_MESSAGE
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, OfflineSyncManager.kt, TrackingService.kt
 */
object PendingSyncQueueTable {
    const val TABLE_NAME = "pending_sync_queue"

    const val COL_ID = "id"
    const val COL_PAYLOAD_TYPE = "payload_type" // LOCATION, VITALS, GEOFENCE_EVENT, SOS
    const val COL_TARGET_UID = "target_uid"
    const val COL_LAT = "lat"
    const val COL_LNG = "lng"
    const val COL_BEARING = "bearing"
    const val COL_SPEED = "speed"
    const val COL_BATTERY = "battery"
    const val COL_IS_SOS = "is_sos"
    const val COL_TIMESTAMP = "timestamp"
    const val COL_STATUS = "sync_status" // PENDING, SYNCING, FAILED
    const val COL_RETRY_COUNT = "retry_count"
    const val COL_EXTRA_DATA = "extra_data" // JSON metadata
    const val COL_ACCURACY = "accuracy"
    const val COL_ALTITUDE = "altitude"
    const val COL_MOTION_STATE = "motion_state"

    // 🚀 V81 Offline Diagnostic & Backoff Sentinel
    const val COL_CREATED_ON = "created_on"
    const val COL_LAST_ATTEMPT_TS = "last_attempt_ts"
    const val COL_ERROR_MESSAGE = "error_message"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_PAYLOAD_TYPE TEXT NOT NULL,
            $COL_TARGET_UID TEXT NOT NULL,
            $COL_LAT REAL,
            $COL_LNG REAL,
            $COL_BEARING REAL,
            $COL_SPEED REAL,
            $COL_BATTERY INTEGER,
            $COL_IS_SOS INTEGER DEFAULT 0,
            $COL_TIMESTAMP INTEGER NOT NULL,
            $COL_STATUS TEXT DEFAULT 'PENDING',
            $COL_RETRY_COUNT INTEGER DEFAULT 0,
            $COL_EXTRA_DATA TEXT,
            $COL_ACCURACY REAL DEFAULT 0.0,
            $COL_ALTITUDE REAL DEFAULT 0.0,
            $COL_MOTION_STATE TEXT DEFAULT 'UNKNOWN',
            $COL_CREATED_ON TEXT,
            $COL_LAST_ATTEMPT_TS INTEGER DEFAULT 0,
            $COL_ERROR_MESSAGE TEXT DEFAULT ''
        )
    """.trimIndent()

    fun enqueue(
        db: SQLiteDatabase,
        payloadType: String,
        targetUid: String,
        lat: Double,
        lng: Double,
        bearing: Float,
        speed: Float,
        battery: Int,
        isSos: Boolean,
        timestamp: Long = System.currentTimeMillis(),
        extraData: String? = null,
        accuracy: Float = 0f,
        altitude: Double = 0.0,
        motionState: String = "UNKNOWN"
    ): Long {
        val cv = ContentValues().apply {
            put(COL_PAYLOAD_TYPE, payloadType)
            put(COL_TARGET_UID, targetUid)
            put(COL_LAT, lat)
            put(COL_LNG, lng)
            put(COL_BEARING, bearing)
            put(COL_SPEED, speed)
            put(COL_BATTERY, battery)
            put(COL_IS_SOS, if (isSos) 1 else 0)
            put(COL_TIMESTAMP, timestamp)
            put(COL_STATUS, "PENDING")
            put(COL_RETRY_COUNT, 0)
            put(COL_EXTRA_DATA, extraData)
            put(COL_ACCURACY, accuracy)
            put(COL_ALTITUDE, altitude)
            put(COL_MOTION_STATE, motionState)
        }
        return try {
            db.insert(TABLE_NAME, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }
}
