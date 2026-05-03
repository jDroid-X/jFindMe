package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * Tier 2: Device & Trust Layer
 * 📶 sim: SIM Verification Hub
 */
object SimTable {
    const val TABLE_NAME = "sim"

    const val COL_SIM_ID = "sim_id"
    const val COL_DEVICE_ID = "device_id" // FK
    const val COL_SIM_SERIAL = "sim_serial"
    const val COL_OPERATOR = "operator"
    const val COL_MCC = "mcc"
    const val COL_MNC = "mnc"
    const val COL_IS_ACTIVE = "is_active"
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_SIM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_DEVICE_ID TEXT NOT NULL,
            $COL_SIM_SERIAL TEXT UNIQUE,
            $COL_OPERATOR TEXT,
            $COL_MCC TEXT,
            $COL_MNC TEXT,
            $COL_IS_ACTIVE INTEGER DEFAULT 1,
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_DEVICE_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    fun logSim(db: SQLiteDatabase, deviceId: String, serial: String?, operator: String?, mcc: String?, mnc: String?, isActive: Boolean) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_DEVICE_ID, deviceId)
            put(COL_SIM_SERIAL, serial)
            put(COL_OPERATOR, operator)
            put(COL_MCC, mcc)
            put(COL_MNC, mnc)
            put(COL_IS_ACTIVE, if(isActive) 1 else 0)
            put(COL_UPDATED_AT, now)
        }
        val rows = db.update(TABLE_NAME, v, "$COL_SIM_SERIAL = ?", arrayOf(serial))
        if (rows == 0) {
            v.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
