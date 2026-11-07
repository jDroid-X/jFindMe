package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: eventId, identityId, zoneId, eventType, timestamp, createdOn, lat, lng, dwellDurationSeconds, syncStatus, now
 * Constants: TABLE_NAME = "geofence_event", COL_EVENT_ID, COL_IDENTITY_ID, COL_ZONE_ID, COL_EVENT_TYPE, COL_TIMESTAMP, COL_CREATED_ON, COL_LAT, COL_LNG, COL_DWELL_DURATION_SECONDS, COL_SYNC_STATUS
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, GeofenceBroadcastReceiver.kt, GeofenceViewModel.kt, GeofenceNotificationActivity.java
 */
object GeofenceEventTable {
    const val TABLE_NAME = "geofence_event"

    const val COL_EVENT_ID = "event_id"
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V63.7: Secure Internal Link (FK)
    const val COL_ZONE_ID = "zone_id" // FK
    const val COL_EVENT_TYPE = "event_type" // ENTRY, EXIT, DWELL
    const val COL_TIMESTAMP = "timestamp"
    const val COL_CREATED_ON = "created_on"

    // 🚀 V81 Spatial Coordinates & Transition Telemetry
    const val COL_LAT = "lat"
    const val COL_LNG = "lng"
    const val COL_DWELL_DURATION_SECONDS = "dwell_duration_seconds"
    const val COL_SYNC_STATUS = "sync_status" // PENDING, SYNCED

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_ZONE_ID TEXT NOT NULL,
            $COL_EVENT_TYPE TEXT NOT NULL,
            $COL_TIMESTAMP INTEGER NOT NULL,
            $COL_CREATED_ON TEXT,
            $COL_LAT REAL DEFAULT 0.0,
            $COL_LNG REAL DEFAULT 0.0,
            $COL_DWELL_DURATION_SECONDS INTEGER DEFAULT 0,
            $COL_SYNC_STATUS TEXT DEFAULT 'SYNCED',
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE,
            FOREIGN KEY ($COL_ZONE_ID) REFERENCES ${SafeZoneTable.TABLE_NAME}(${SafeZoneTable.COL_ZONE_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    fun logEvent(db: SQLiteDatabase, identityId: Long, zoneId: String, type: String) {
        val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_IDENTITY_ID, identityId)
            put(COL_ZONE_ID, zoneId)
            put(COL_EVENT_TYPE, type)
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_CREATED_ON, now)
        }
        db.insert(TABLE_NAME, null, v)
    }

    fun save(db: SQLiteDatabase, identityId: Long, zoneId: String, type: String) {
        logEvent(db, identityId, zoneId, type)
    }
}
