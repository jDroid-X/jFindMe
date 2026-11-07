package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, zoneId, identityId, scheduleStart, scheduleEnd, activeDays, isActive, alertOnEntry, alertOnExit, alertOnDwell, currentState, lastTransitionTs, status, createdOn, now
 * Constants: TABLE_NAME = "safe_zone_user_map", COL_ID, COL_ZONE_ID, COL_IDENTITY_ID, COL_SCHEDULE_START, COL_SCHEDULE_END, COL_ACTIVE_DAYS, COL_IS_ACTIVE, COL_ALERT_ON_ENTRY, COL_ALERT_ON_EXIT, COL_ALERT_ON_DWELL, COL_CURRENT_STATE, COL_LAST_TRANSITION_TS, COL_STATUS, COL_CREATED_ON
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, GeofenceHelper.kt, GeofenceViewModel.kt, SafeZoneActivity.java
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

    // 🚀 V81 Transition State-Latch & Granular Breach Controls
    const val COL_ALERT_ON_ENTRY = "alert_on_entry"
    const val COL_ALERT_ON_EXIT = "alert_on_exit"
    const val COL_ALERT_ON_DWELL = "alert_on_dwell"
    const val COL_CURRENT_STATE = "current_state" // IN or OUT (prevents alert oscillation)
    const val COL_LAST_TRANSITION_TS = "last_transition_ts"
    const val COL_STATUS = "status" // ACTIVE, PENDING_ACCEPTANCE

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
            $COL_ALERT_ON_ENTRY INTEGER DEFAULT 1,
            $COL_ALERT_ON_EXIT INTEGER DEFAULT 1,
            $COL_ALERT_ON_DWELL INTEGER DEFAULT 1,
            $COL_CURRENT_STATE TEXT DEFAULT 'OUT',
            $COL_LAST_TRANSITION_TS INTEGER DEFAULT 0,
            $COL_STATUS TEXT DEFAULT 'ACTIVE',
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
