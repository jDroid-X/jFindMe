package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * Tier 4: Event & Tracking Layer
 * 🔗 safe_zone_user_map: Links Users to Zones with schedules
 */
object SafeZoneUserMapTable {
    const val TABLE_NAME = "safe_zone_user_map"

    const val COL_ID = "id"
    const val COL_ZONE_ID = "zone_id" // FK
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V65.0: Secure Internal Link (FK)
    const val COL_SCHEDULE_START = "schedule_start" // ms from start of day
    const val COL_SCHEDULE_END = "schedule_end"
    const val COL_ACTIVE_DAYS = "active_days" // CSV (e.g., "1,2,3,4,5")
    const val COL_IS_ACTIVE = "is_active"
    const val COL_CREATED_ON = "created_on"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_ZONE_ID TEXT NOT NULL,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_SCHEDULE_START INTEGER,
            $COL_SCHEDULE_END INTEGER,
            $COL_ACTIVE_DAYS TEXT,
            $COL_IS_ACTIVE INTEGER DEFAULT 1,
            $COL_CREATED_ON TEXT,
            FOREIGN KEY ($COL_ZONE_ID) REFERENCES ${SafeZoneTable.TABLE_NAME}(${SafeZoneTable.COL_ZONE_ID}) ON DELETE CASCADE,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE,
            UNIQUE($COL_ZONE_ID, $COL_IDENTITY_ID)
        )
    """.trimIndent()

    fun assignUserToZone(db: SQLiteDatabase, zoneId: String, identityId: Long, start: Long, end: Long, days: String, active: Boolean) {
        val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_ZONE_ID, zoneId)
            put(COL_IDENTITY_ID, identityId)
            put(COL_SCHEDULE_START, start)
            put(COL_SCHEDULE_END, end)
            put(COL_ACTIVE_DAYS, days)
            put(COL_IS_ACTIVE, if(active) 1 else 0)
        }
        val rows = db.update(TABLE_NAME, v, "$COL_ZONE_ID = ? AND $COL_IDENTITY_ID = ?", arrayOf(zoneId, identityId.toString()))
        if (rows == 0) {
            v.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
