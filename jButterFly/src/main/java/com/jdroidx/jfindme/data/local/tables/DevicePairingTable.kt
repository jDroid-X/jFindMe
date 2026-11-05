package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * Tier 2: Device & Trust Layer
 * 🔗 device_pairing: Relational handshake between nodes
 */
object DevicePairingTable {
    const val TABLE_NAME = "device_pairing"

    const val COL_PAIR_ID = "pair_id"
    const val COL_DEVICE_A_ID = "device_a_id" // FK to DeviceTable.device_id
    const val COL_DEVICE_B_ID = "device_b_id" // FK to DeviceTable.device_id
    const val COL_PAIR_STATUS = "pair_status"
    const val COL_CREATED_ON = "created_on"
    const val COL_CREATED_AT = "created_at"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_PAIR_ID TEXT PRIMARY KEY,
            $COL_DEVICE_A_ID TEXT NOT NULL,
            $COL_DEVICE_B_ID TEXT NOT NULL,
            $COL_PAIR_STATUS TEXT DEFAULT 'PENDING',
            $COL_CREATED_ON TEXT,
            $COL_CREATED_AT TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_DEVICE_A_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE,
            FOREIGN KEY ($COL_DEVICE_B_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    fun logPairing(db: SQLiteDatabase, pairId: String, devA: String, devB: String, status: String) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_PAIR_ID, pairId)
            put(COL_DEVICE_A_ID, devA)
            put(COL_DEVICE_B_ID, devB)
            put(COL_PAIR_STATUS, status)
            put(COL_UPDATED_AT, now)
        }
        val id = db.insertWithOnConflict(TABLE_NAME, null, v, SQLiteDatabase.CONFLICT_IGNORE)
        if (id == -1L) {
            db.update(TABLE_NAME, v, "$COL_PAIR_ID=?", arrayOf(pairId))
        } else {
            db.execSQL("UPDATE $TABLE_NAME SET $COL_CREATED_ON = '$now', $COL_CREATED_AT = '$now' WHERE $COL_PAIR_ID = '$pairId'")
        }
    }
}
