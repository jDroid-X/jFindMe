/**
 * Auto-Login Architecture: Siren Execution Audit Ledger
 * Waterfall Method: Secured Acoustic Beacon Ledger & Forensic Tamper Defense
 */
package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: siren_id, triggered_by_device_id, trigger_source, duration_seconds, decibel_peak, battery_impact_pct, terminated_by_pin, resolved_at, timestamp, created_on
 * Constants: TABLE_NAME = "siren_execution_audit", COL_SIREN_ID, COL_TRIGGERED_BY_DEVICE_ID, COL_TRIGGER_SOURCE, COL_DURATION_SECONDS, COL_DECIBEL_PEAK, COL_BATTERY_IMPACT_PCT, COL_TERMINATED_BY_PIN, COL_RESOLVED_AT, COL_TIMESTAMP, COL_CREATED_ON, SOURCE_MANUAL, SOURCE_REMOTE_FCM, SOURCE_BLE_MESH, SOURCE_HARDWARE_KEY
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase
 * Integration Points: DatabaseHelper.kt (onCreate, onUpgrade), AudioOverrideService, AcousticSirenActivity, EmergencyAlertHelper
 */
object SirenExecutionAuditTable {
    const val TABLE_NAME = "siren_execution_audit"

    const val COL_SIREN_ID = "siren_id"
    const val COL_TRIGGERED_BY_DEVICE_ID = "triggered_by_device_id"
    const val COL_TRIGGER_SOURCE = "trigger_source"
    const val COL_DURATION_SECONDS = "duration_seconds"
    const val COL_DECIBEL_PEAK = "decibel_peak"
    const val COL_BATTERY_IMPACT_PCT = "battery_impact_pct"
    const val COL_TERMINATED_BY_PIN = "terminated_by_pin"
    const val COL_RESOLVED_AT = "resolved_at"
    const val COL_TIMESTAMP = "timestamp"
    const val COL_CREATED_ON = "created_on"

    const val SOURCE_MANUAL = "MANUAL"
    const val SOURCE_REMOTE_FCM = "REMOTE_FCM"
    const val SOURCE_BLE_MESH = "BLE_MESH"
    const val SOURCE_HARDWARE_KEY = "HARDWARE_KEY"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_SIREN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_TRIGGERED_BY_DEVICE_ID TEXT DEFAULT '',
            $COL_TRIGGER_SOURCE TEXT NOT NULL DEFAULT '$SOURCE_MANUAL',
            $COL_DURATION_SECONDS INTEGER DEFAULT 30,
            $COL_DECIBEL_PEAK REAL DEFAULT 0.0,
            $COL_BATTERY_IMPACT_PCT INTEGER DEFAULT 0,
            $COL_TERMINATED_BY_PIN INTEGER DEFAULT 0,
            $COL_RESOLVED_AT INTEGER DEFAULT 0,
            $COL_TIMESTAMP INTEGER NOT NULL,
            $COL_CREATED_ON TEXT DEFAULT ''
        )
    """.trimIndent()

    val CREATE_INDEX_TRIGGERED = "CREATE INDEX IF NOT EXISTS idx_siren_audit_ts ON $TABLE_NAME ($COL_TIMESTAMP DESC)"

    @JvmStatic
    fun recordSirenStart(
        db: SQLiteDatabase,
        triggeredByDeviceId: String,
        triggerSource: String = SOURCE_MANUAL,
        durationSeconds: Int = 30
    ): Long {
        val cv = ContentValues().apply {
            put(COL_TRIGGERED_BY_DEVICE_ID, triggeredByDeviceId)
            put(COL_TRIGGER_SOURCE, triggerSource)
            put(COL_DURATION_SECONDS, durationSeconds)
            put(COL_TERMINATED_BY_PIN, 0)
            put(COL_RESOLVED_AT, 0)
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_CREATED_ON, java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
        }
        return try {
            db.insert(TABLE_NAME, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }

    @JvmStatic
    fun resolveSiren(
        db: SQLiteDatabase,
        sirenId: Long,
        terminatedByPin: Boolean,
        decibelPeak: Double = 0.0,
        batteryImpactPct: Int = 0
    ): Boolean {
        val cv = ContentValues().apply {
            put(COL_TERMINATED_BY_PIN, if (terminatedByPin) 1 else 0)
            put(COL_RESOLVED_AT, System.currentTimeMillis())
            if (decibelPeak > 0.0) put(COL_DECIBEL_PEAK, decibelPeak)
            if (batteryImpactPct > 0) put(COL_BATTERY_IMPACT_PCT, batteryImpactPct)
        }
        return try {
            db.update(TABLE_NAME, cv, "$COL_SIREN_ID = ?", arrayOf(sirenId.toString())) > 0
        } catch (e: Exception) {
            false
        }
    }
}
