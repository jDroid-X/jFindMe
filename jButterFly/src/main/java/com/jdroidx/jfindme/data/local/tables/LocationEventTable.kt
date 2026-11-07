package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, identityId, timestamp, dayKey, lat, lng, speed, battery, sosFlag, geoAddress, accuracy, bearing, altitude, motionState, syncStatus, isMock, provider, createdOn
 * Constants: TABLE_NAME = "location_event", COL_EVENT_ID, COL_ID, COL_IDENTITY_ID, COL_TIMESTAMP, COL_DAY_KEY, COL_LAT, COL_LNG, COL_SPEED, COL_BATTERY, COL_SOS_FLAG, COL_ADDRESS, COL_ACCURACY, COL_BEARING, COL_ALTITUDE, COL_MOTION_STATE, COL_SYNC_STATUS, COL_IS_MOCK, COL_PROVIDER, COL_CREATED_ON
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, TrackingService.kt, LocationRepository.kt, HistoryActivity.java
 */
object LocationEventTable {
    const val TABLE_NAME = "location_event"

    const val COL_EVENT_ID = "id"
    const val COL_ID = COL_EVENT_ID // 🧬 Legacy Parity
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V64.0: Secure Internal Anchor (FK)
    const val COL_TIMESTAMP = "timestamp"
    const val COL_DAY_KEY = "day_key" // yyyyMMdd for efficient grouping
    const val COL_LAT = "lat"
    const val COL_LNG = "lng"
    const val COL_SPEED = "speed"
    const val COL_BATTERY = "battery"
    const val COL_SOS_FLAG = "sos_flag"
    const val COL_ADDRESS = "geo_address"

    // 🛡️ V79 REA Refabrication Columns: Enhanced GIS Spatial Geometry & Motion
    const val COL_ACCURACY = "accuracy"
    const val COL_BEARING = "bearing"
    const val COL_ALTITUDE = "altitude"
    const val COL_MOTION_STATE = "motion_state"

    // 🚀 V81 Offline Sync Tracking & Provider Sentinel
    const val COL_SYNC_STATUS = "sync_status"
    const val COL_IS_MOCK = "is_mock"
    const val COL_PROVIDER = "provider"

    const val COL_CREATED_ON = "created_on"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_TIMESTAMP TEXT NOT NULL,
            $COL_DAY_KEY INTEGER,
            $COL_LAT REAL,
            $COL_LNG REAL,
            $COL_SPEED REAL,
            $COL_BATTERY INTEGER,
            $COL_SOS_FLAG INTEGER DEFAULT 0,
            $COL_ADDRESS TEXT,
            $COL_ACCURACY REAL DEFAULT 0.0,
            $COL_BEARING REAL DEFAULT 0.0,
            $COL_ALTITUDE REAL DEFAULT 0.0,
            $COL_MOTION_STATE TEXT DEFAULT 'UNKNOWN',
            $COL_SYNC_STATUS TEXT DEFAULT 'SYNCED',
            $COL_IS_MOCK INTEGER DEFAULT 0,
            $COL_PROVIDER TEXT DEFAULT 'fused',
            $COL_CREATED_ON TEXT,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    @JvmOverloads
    fun logLocation(
        db: SQLiteDatabase,
        identityId: Long,
        lat: Double,
        lng: Double,
        speed: Float,
        battery: Int,
        sos: Boolean,
        ts: String? = null,
        addr: String? = null,
        accuracy: Float = 0f,
        bearing: Float = 0f,
        altitude: Double = 0.0,
        motionState: String? = null
    ) {
        val now = ts ?: com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
        
        // Extract yyyyMMdd from the forensic string for grouping
        val dayKey = try {
            if (now.length >= 8) now.substring(0, 8).toInt() else 0
        } catch (e: Exception) { 0 }

        // 🛡️ Deduplication Guard: Ignore identical duplicate pings for this identity within the same forensic timestamp
        try {
            val dupCheck = db.rawQuery(
                "SELECT 1 FROM $TABLE_NAME WHERE $COL_IDENTITY_ID = ? AND $COL_TIMESTAMP = ? LIMIT 1",
                arrayOf(identityId.toString(), now)
            )
            val exists = dupCheck.use { it.moveToFirst() }
            if (exists) return
        } catch (e: Exception) { }
        
        val v = ContentValues().apply {
            put(COL_IDENTITY_ID, identityId)
            put(COL_TIMESTAMP, now)
            put(COL_DAY_KEY, dayKey)
            put(COL_LAT, lat)
            put(COL_LNG, lng)
            put(COL_SPEED, speed)
            put(COL_BATTERY, battery)
            put(COL_SOS_FLAG, if(sos) 1 else 0)
            put(COL_ADDRESS, addr)
            put(COL_ACCURACY, accuracy)
            put(COL_BEARING, bearing)
            put(COL_ALTITUDE, altitude)
            if (!motionState.isNullOrEmpty()) put(COL_MOTION_STATE, motionState)
            put(COL_CREATED_ON, now)
        }
        try {
            db.insert(TABLE_NAME, null, v)
        } catch (e: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.e("LocationEventTable", "Forensic Archive Blocked: Relational Link Missing for node $identityId", e)
        }
    }
}
