package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * 🔋 DeviceVitalsTable: Hardware & Device Telemetry Health Cache
 * Records battery levels, network states, and security posture flags.
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
            $COL_LAST_SEEN INTEGER NOT NULL
        )
    """.trimIndent()

    fun upsertVitals(
        db: SQLiteDatabase,
        hardwareId: String,
        battery: Int,
        isCharging: Boolean,
        networkType: String,
        signalDbm: Int,
        isMockGps: Boolean,
        isRooted: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val cv = ContentValues().apply {
            put(COL_HARDWARE_ID, hardwareId)
            put(COL_BATTERY_LEVEL, battery)
            put(COL_IS_CHARGING, if (isCharging) 1 else 0)
            put(COL_NETWORK_TYPE, networkType)
            put(COL_SIGNAL_DBM, signalDbm)
            put(COL_IS_MOCK_GPS, if (isMockGps) 1 else 0)
            put(COL_IS_ROOTED, if (isRooted) 1 else 0)
            put(COL_LAST_SEEN, timestamp)
        }
        return try {
            db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            -1L
        }
    }
}
