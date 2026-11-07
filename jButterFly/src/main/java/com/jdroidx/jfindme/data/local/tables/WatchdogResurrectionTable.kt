/**
 * Auto-Login Architecture: Watchdog Resurrection Ledger
 * Waterfall Method: Secured Service Health Ledger & REA Specification Parity
 */
package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, hardware_id, trigger_action, timestamp, success, resurrection_latency_ms, details, created_on
 * Constants: TABLE_NAME = "watchdog_resurrection_log", COL_ID, COL_HARDWARE_ID, COL_TRIGGER_ACTION, COL_TIMESTAMP, COL_SUCCESS, COL_RESURRECTION_LATENCY_MS, COL_DETAILS, COL_CREATED_ON
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase
 * Integration Points: DatabaseHelper.kt (onCreate, onUpgrade, logWatchdogResurrection)
 */
object WatchdogResurrectionTable {
    const val TABLE_NAME = "watchdog_resurrection_log"

    const val COL_ID = "id"
    const val COL_HARDWARE_ID = "hardware_id"
    const val COL_TRIGGER_ACTION = "trigger_action"
    const val COL_TIMESTAMP = "timestamp"
    const val COL_SUCCESS = "success"
    const val COL_RESURRECTION_LATENCY_MS = "resurrection_latency_ms"
    const val COL_DETAILS = "details"
    const val COL_CREATED_ON = "created_on"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_HARDWARE_ID TEXT DEFAULT '',
            $COL_TRIGGER_ACTION TEXT NOT NULL,
            $COL_TIMESTAMP INTEGER NOT NULL,
            $COL_SUCCESS INTEGER DEFAULT 1,
            $COL_RESURRECTION_LATENCY_MS INTEGER DEFAULT 0,
            $COL_DETAILS TEXT DEFAULT '',
            $COL_CREATED_ON TEXT DEFAULT ''
        )
    """.trimIndent()

    fun logResurrection(db: SQLiteDatabase, triggerAction: String, success: Boolean = true, timestamp: Long = System.currentTimeMillis()): Long {
        val cv = ContentValues().apply {
            put(COL_TRIGGER_ACTION, triggerAction)
            put(COL_TIMESTAMP, timestamp)
            put(COL_SUCCESS, if (success) 1 else 0)
        }
        return try {
            db.insert(TABLE_NAME, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }
}

