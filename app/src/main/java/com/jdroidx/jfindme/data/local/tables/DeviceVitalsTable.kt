package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, hardwareId, battery, isCharging, networkType, signalDbm, isMockGps, isRooted, timestamp, activeRoute, ackTimestamp, ackLatencyMs, motionState, storageFreeMb, ramUsageMb, temperatureC, cpuUsagePct, isAirplaneMode, wifiSsid
 * Constants: TABLE_NAME = "device_vitals", COL_ID, COL_HARDWARE_ID, COL_BATTERY_LEVEL, COL_IS_CHARGING, COL_NETWORK_TYPE, COL_SIGNAL_DBM, COL_IS_MOCK_GPS, COL_IS_ROOTED, COL_LAST_SEEN, COL_ACTIVE_ROUTE, COL_IS_STEALTH, COL_ACK_TIMESTAMP, COL_ACK_LATENCY_MS, COL_MOTION_STATE, COL_STORAGE_FREE_MB, COL_RAM_USAGE_MB, COL_TEMPERATURE_C, COL_CPU_USAGE_PCT, COL_IS_AIRPLANE_MODE, COL_WIFI_SSID
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase
 * Integration Points: DatabaseHelper.kt, TrackingService.kt, HardwareIdentitySentinel.kt
 */
object DeviceVitalsTable {
    const val TABLE_NAME = "device_vitals"

    const val COL_ID = "id"
    const val COL_HARDWARE_ID = "hardware_id"
    const val COL_BATTERY_LEVEL = "battery_level"
    const val COL_IS_CHARGING = "is_charging"
    const val COL_NETWORK_TYPE = "network_type" // WIFI, 4G, 5G, NONE
    const val COL_SIGNAL_DBM = "signal_dbm"
    const val COL_IS_MOCK_GPS = "is_mock_gps"
    const val COL_IS_ROOTED = "is_rooted"
    const val COL_LAST_SEEN = "last_seen"
    const val COL_ACTIVE_ROUTE = "active_route"
    const val COL_IS_STEALTH = "is_stealth"
    const val COL_ACK_TIMESTAMP = "ack_timestamp"
    const val COL_ACK_LATENCY_MS = "ack_latency_ms"

    // 🛡️ V79 REA Refabrication Columns: Spatial Motion State & Memory Footprint
    const val COL_MOTION_STATE = "motion_state" // STATIONARY, WALKING, VEHICLE, UNKNOWN
    const val COL_STORAGE_FREE_MB = "storage_free_mb"
    const val COL_RAM_USAGE_MB = "ram_usage_mb"

    // 🚀 V81 Environmental & Connectivity Diagnostics Parity
    const val COL_TEMPERATURE_C = "temperature_c"
    const val COL_CPU_USAGE_PCT = "cpu_usage_pct"
    const val COL_IS_AIRPLANE_MODE = "is_airplane_mode"
    const val COL_WIFI_SSID = "wifi_ssid"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_HARDWARE_ID TEXT UNIQUE NOT NULL,
            $COL_BATTERY_LEVEL INTEGER NOT NULL,
            $COL_IS_CHARGING INTEGER DEFAULT 0,
            $COL_NETWORK_TYPE TEXT DEFAULT 'UNKNOWN',
            $COL_SIGNAL_DBM INTEGER DEFAULT 0,
            $COL_IS_MOCK_GPS INTEGER DEFAULT 0,
            $COL_IS_ROOTED INTEGER DEFAULT 0,
            $COL_LAST_SEEN INTEGER NOT NULL,
            $COL_ACTIVE_ROUTE TEXT DEFAULT 'N1_CELLULAR_FAST',
            $COL_IS_STEALTH INTEGER DEFAULT 0,
            $COL_ACK_TIMESTAMP INTEGER DEFAULT 0,
            $COL_ACK_LATENCY_MS INTEGER DEFAULT 0,
            $COL_MOTION_STATE TEXT DEFAULT 'STATIONARY',
            $COL_STORAGE_FREE_MB INTEGER DEFAULT 0,
            $COL_RAM_USAGE_MB INTEGER DEFAULT 0,
            $COL_TEMPERATURE_C REAL DEFAULT 0.0,
            $COL_CPU_USAGE_PCT REAL DEFAULT 0.0,
            $COL_IS_AIRPLANE_MODE INTEGER DEFAULT 0,
            $COL_WIFI_SSID TEXT DEFAULT ''
        )
    """.trimIndent()

    /**
     * Preserves closed-loop ACK, motion state, and active route values during periodic telemetry updates.
     * Uses atomic UPDATE with fallback INSERT to avoid overwriting existing ACK state.
     */
    @JvmOverloads
    fun upsertVitals(
        db: SQLiteDatabase,
        hardwareId: String,
        battery: Int,
        isCharging: Boolean,
        networkType: String,
        signalDbm: Int,
        isMockGps: Boolean,
        isRooted: Boolean,
        timestamp: Long = System.currentTimeMillis(),
        activeRoute: String? = null,
        ackTimestamp: Long? = null,
        ackLatencyMs: Long? = null,
        motionState: String? = null,
        storageFreeMb: Int? = null,
        ramUsageMb: Int? = null
    ): Long {
        val cleanId = hardwareId.removePrefix("HW_")
        val hwPrefixedId = "HW_$cleanId"
        val whereClause = "$COL_HARDWARE_ID = ? OR $COL_HARDWARE_ID = ? OR $COL_HARDWARE_ID = ?"
        val whereArgs = arrayOf(hardwareId, cleanId, hwPrefixedId)

        val cv = ContentValues().apply {
            put(COL_BATTERY_LEVEL, battery)
            put(COL_IS_CHARGING, if (isCharging) 1 else 0)
            put(COL_NETWORK_TYPE, networkType)
            put(COL_SIGNAL_DBM, signalDbm)
            put(COL_IS_MOCK_GPS, if (isMockGps) 1 else 0)
            put(COL_IS_ROOTED, if (isRooted) 1 else 0)
            put(COL_LAST_SEEN, timestamp)
            if (activeRoute != null && activeRoute.isNotBlank()) {
                put(COL_ACTIVE_ROUTE, activeRoute)
            }
            if (ackTimestamp != null && ackTimestamp > 0L) {
                put(COL_ACK_TIMESTAMP, ackTimestamp)
            }
            if (ackLatencyMs != null && ackLatencyMs >= 0L) {
                put(COL_ACK_LATENCY_MS, ackLatencyMs)
            }
            if (motionState != null && motionState.isNotBlank()) {
                put(COL_MOTION_STATE, motionState)
            }
            if (storageFreeMb != null && storageFreeMb >= 0) {
                put(COL_STORAGE_FREE_MB, storageFreeMb)
            }
            if (ramUsageMb != null && ramUsageMb >= 0) {
                put(COL_RAM_USAGE_MB, ramUsageMb)
            }
        }

        return try {
            val rowsUpdated = db.update(TABLE_NAME, cv, whereClause, whereArgs)
            if (rowsUpdated > 0) {
                rowsUpdated.toLong()
            } else {
                cv.put(COL_HARDWARE_ID, hardwareId)
                if (!cv.containsKey(COL_ACTIVE_ROUTE)) cv.put(COL_ACTIVE_ROUTE, "N1_CELLULAR_FAST")
                if (!cv.containsKey(COL_ACK_TIMESTAMP)) cv.put(COL_ACK_TIMESTAMP, 0L)
                if (!cv.containsKey(COL_ACK_LATENCY_MS)) cv.put(COL_ACK_LATENCY_MS, 0L)
                if (!cv.containsKey(COL_MOTION_STATE)) cv.put(COL_MOTION_STATE, "STATIONARY")
                db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            }
        } catch (e: Exception) {
            -1L
        }
    }
}
