package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, slot, mcc, mnc, lac, cid, lat, lng, source, timestamp, createdOn, deviceId, signalDbm, networkType
 * Constants: TABLE_NAME = "sim_transactions", COL_ID, COL_SLOT, COL_MCC, COL_MNC, COL_LAC, COL_CID, COL_LAT, COL_LNG, COL_SOURCE, COL_TIMESTAMP, COL_CREATED_ON, COL_DEVICE_ID, COL_SIGNAL_DBM, COL_NETWORK_TYPE
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, HealthActivity.kt, SimForensicWorker.kt, CellTowerSentinel.kt
 */
object SimTransactionsTable {
    const val TABLE_NAME = "sim_transactions"

    const val COL_ID = "id"
    const val COL_SLOT = "slot"
    const val COL_MCC = "mcc"
    const val COL_MNC = "mnc"
    const val COL_LAC = "lac"
    const val COL_CID = "cid"
    const val COL_LAT = "lat"
    const val COL_LNG = "lng"
    const val COL_SOURCE = "source"
    const val COL_TIMESTAMP = "timestamp"
    const val COL_CREATED_ON = "created_on"
    const val COL_DEVICE_ID = "device_id"
    const val COL_SIGNAL_DBM = "signal_dbm"
    const val COL_NETWORK_TYPE = "network_type"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_SLOT INTEGER,
            $COL_MCC TEXT,
            $COL_MNC TEXT,
            $COL_LAC TEXT,
            $COL_CID TEXT,
            $COL_LAT REAL,
            $COL_LNG REAL,
            $COL_SOURCE TEXT,
            $COL_TIMESTAMP INTEGER,
            $COL_CREATED_ON TEXT,
            $COL_DEVICE_ID TEXT DEFAULT '',
            $COL_SIGNAL_DBM INTEGER DEFAULT 0,
            $COL_NETWORK_TYPE TEXT DEFAULT 'LTE'
        )
    """.trimIndent()

    fun logTransaction(
        db: SQLiteDatabase,
        slot: Int,
        mcc: String?,
        mnc: String?,
        lac: String?,
        cid: String?,
        lat: Double = 0.0,
        lng: Double = 0.0,
        source: String = "CELLULAR",
        deviceId: String = "",
        signalDbm: Int = 0,
        networkType: String = "LTE"
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()
        val cv = ContentValues().apply {
            put(COL_SLOT, slot)
            put(COL_MCC, mcc)
            put(COL_MNC, mnc)
            put(COL_LAC, lac)
            put(COL_CID, cid)
            put(COL_LAT, lat)
            put(COL_LNG, lng)
            put(COL_SOURCE, source)
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_CREATED_ON, now)
            put(COL_DEVICE_ID, deviceId)
            put(COL_SIGNAL_DBM, signalDbm)
            put(COL_NETWORK_TYPE, networkType)
        }
        return db.insert(TABLE_NAME, null, cv)
    }
}

