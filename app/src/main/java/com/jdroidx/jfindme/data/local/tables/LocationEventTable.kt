package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tier 4: Event & Tracking Layer (Time-Series)
 * 📍 location_event: Append-only stream of pings
 * V57: Hardened for Day-wise forensic grouping.
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
            $COL_CREATED_ON TEXT,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    fun logLocation(db: SQLiteDatabase, identityId: Long, lat: Double, lng: Double, speed: Float, battery: Int, sos: Boolean, ts: String? = null, addr: String? = null) {
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
            put(COL_CREATED_ON, now)
        }
        try {
            db.insert(TABLE_NAME, null, v)
        } catch (e: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.e("LocationEventTable", "Forensic Archive Blocked: Relational Link Missing for node $identityId", e)
        }
    }
}
