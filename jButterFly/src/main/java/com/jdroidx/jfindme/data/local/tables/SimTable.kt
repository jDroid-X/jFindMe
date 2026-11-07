package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: simId, deviceId, serial, operator, mcc, mnc, isActive, servingCid, carrierName, slotIndex, phoneNumber, lac, signalStrengthDbm, now
 * Constants: TABLE_NAME = "sim", COL_SIM_ID, COL_DEVICE_ID, COL_SIM_SERIAL, COL_OPERATOR, COL_MCC, COL_MNC, COL_IS_ACTIVE, COL_SERVING_CID, COL_CARRIER_NAME, COL_SLOT_INDEX, COL_PHONE_NUMBER, COL_LAC, COL_SIGNAL_STRENGTH_DBM, COL_CREATED_ON, COL_UPDATED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, HardwareIdentitySentinel.kt, NetworkGatewayActivity.java
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

    // 🛡️ V79 REA Refabrication Columns: Serving Cell Tower & Carrier Identity
    const val COL_SERVING_CID = "serving_cid"
    const val COL_CARRIER_NAME = "carrier_name"

    // 🚀 V81 Dual-SIM Slot & Baseband Radio Parity
    const val COL_SLOT_INDEX = "slot_index"
    const val COL_PHONE_NUMBER = "phone_number"
    const val COL_LAC = "lac"
    const val COL_SIGNAL_STRENGTH_DBM = "signal_strength_dbm"

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
            $COL_SERVING_CID TEXT DEFAULT '',
            $COL_CARRIER_NAME TEXT DEFAULT '',
            $COL_SLOT_INDEX INTEGER DEFAULT 0,
            $COL_PHONE_NUMBER TEXT DEFAULT '',
            $COL_LAC TEXT DEFAULT '',
            $COL_SIGNAL_STRENGTH_DBM INTEGER DEFAULT 0,
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_DEVICE_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    @JvmOverloads
    fun logSim(
        db: SQLiteDatabase,
        deviceId: String,
        serial: String?,
        operator: String?,
        mcc: String?,
        mnc: String?,
        isActive: Boolean,
        servingCid: String? = null,
        carrierName: String? = null
    ) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_DEVICE_ID, deviceId)
            put(COL_SIM_SERIAL, serial)
            put(COL_OPERATOR, operator)
            put(COL_MCC, mcc)
            put(COL_MNC, mnc)
            put(COL_IS_ACTIVE, if(isActive) 1 else 0)
            if (!servingCid.isNullOrEmpty()) put(COL_SERVING_CID, servingCid)
            if (!carrierName.isNullOrEmpty()) put(COL_CARRIER_NAME, carrierName)
            put(COL_UPDATED_AT, now)
        }
        val rows = db.update(TABLE_NAME, v, "$COL_SIM_SERIAL = ?", arrayOf(serial))
        if (rows == 0) {
            v.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
