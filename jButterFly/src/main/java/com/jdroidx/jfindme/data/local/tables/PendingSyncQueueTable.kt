package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * 🛰️ PendingSyncQueueTable: Offline Telemetry & Event Queue
 * Caches location pings, battery updates, and safezone events when offline.
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
            $COL_EXTRA_DATA TEXT
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
        extraData: String? = null
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
        }
        return try {
            db.insert(TABLE_NAME, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }
}
