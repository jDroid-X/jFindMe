package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, identityId, agentId, alertType, timestamp, details, isRead, resolvedAt, lat, lng, zoneId, severity, createdOn
 * Constants: TABLE_NAME = "alert_history", COL_ID, COL_IDENTITY_ID, COL_AGENT_ID, COL_ALERT_TYPE, COL_TIMESTAMP, COL_DETAILS, COL_IS_READ, COL_RESOLVED_AT, COL_LAT, COL_LNG, COL_ZONE_ID, COL_SEVERITY, COL_CREATED_ON
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, NotifActivity.java, MapActivity.java, AcousticSirenActivity.java
 */
object AlertHistoryTable {
    const val TABLE_NAME = "alert_history"

    const val COL_ID = "id"
    const val COL_IDENTITY_ID = "identity_id" // FK to UserIdentityTable.id
    const val COL_AGENT_ID = "agent_id"
    const val COL_ALERT_TYPE = "alert_type" // SOS_PANIC, GEOFENCE_BREACH, TAMPER_ALARM, BATTERY_LOW
    const val COL_TIMESTAMP = "timestamp"
    const val COL_DETAILS = "details"
    const val COL_IS_READ = "is_read" // 0 = unread, 1 = read
    const val COL_RESOLVED_AT = "resolved_at" // 0 = active, timestamp = resolved
    const val COL_LAT = "lat"
    const val COL_LNG = "lng"
    const val COL_ZONE_ID = "zone_id"
    const val COL_SEVERITY = "severity" // INFO, WARNING, CRITICAL, EMERGENCY
    const val COL_CREATED_ON = "created_on"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_IDENTITY_ID INTEGER DEFAULT 0,
            $COL_AGENT_ID TEXT,
            $COL_ALERT_TYPE TEXT DEFAULT 'SOS_PANIC',
            $COL_TIMESTAMP INTEGER NOT NULL,
            $COL_DETAILS TEXT,
            $COL_IS_READ INTEGER DEFAULT 0,
            $COL_RESOLVED_AT INTEGER DEFAULT 0,
            $COL_LAT REAL DEFAULT 0.0,
            $COL_LNG REAL DEFAULT 0.0,
            $COL_ZONE_ID TEXT DEFAULT '',
            $COL_SEVERITY TEXT DEFAULT 'CRITICAL',
            $COL_CREATED_ON TEXT
        )
    """.trimIndent()

    fun logAlert(
        db: SQLiteDatabase,
        agentId: String?,
        alertType: String = "SOS_PANIC",
        details: String?,
        timestamp: Long = System.currentTimeMillis(),
        identityId: Long = 0L,
        lat: Double = 0.0,
        lng: Double = 0.0,
        zoneId: String = "",
        severity: String = "CRITICAL"
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()
        val cv = ContentValues().apply {
            put(COL_IDENTITY_ID, identityId)
            put(COL_AGENT_ID, agentId)
            put(COL_ALERT_TYPE, alertType)
            put(COL_TIMESTAMP, timestamp)
            put(COL_DETAILS, details)
            put(COL_IS_READ, 0)
            put(COL_RESOLVED_AT, 0)
            put(COL_LAT, lat)
            put(COL_LNG, lng)
            put(COL_ZONE_ID, zoneId)
            put(COL_SEVERITY, severity)
            put(COL_CREATED_ON, now)
        }
        return db.insert(TABLE_NAME, null, cv)
    }
}

