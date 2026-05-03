package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * Tier 4: Event & Tracking Layer
 * 🚪 geofence_event: Entry/Exit event log
 */
object GeofenceEventTable {
    const val TABLE_NAME = "geofence_event"

    const val COL_EVENT_ID = "event_id"
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V63.7: Secure Internal Link (FK)
    const val COL_ZONE_ID = "zone_id" // FK
    const val COL_EVENT_TYPE = "event_type" // ENTRY/EXIT
    const val COL_TIMESTAMP = "timestamp"
    const val COL_CREATED_ON = "created_on"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_ZONE_ID TEXT NOT NULL,
            $COL_EVENT_TYPE TEXT NOT NULL,
            $COL_TIMESTAMP INTEGER NOT NULL,
            $COL_CREATED_ON TEXT,
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
