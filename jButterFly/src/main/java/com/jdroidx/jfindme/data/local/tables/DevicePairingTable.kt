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
    const val COL_ROLE_A = "role_a"
    const val COL_ROLE_B = "role_b"
    const val COL_PAIRING_CODE = "pairing_code"
    const val COL_CREATED_ON = "created_on"
    const val COL_CREATED_AT = "created_at"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_PAIR_ID TEXT PRIMARY KEY,
            $COL_DEVICE_A_ID TEXT NOT NULL,
            $COL_DEVICE_B_ID TEXT NOT NULL,
            $COL_PAIR_STATUS TEXT DEFAULT 'PENDING',
            $COL_ROLE_A TEXT DEFAULT 'MEMBER',
            $COL_ROLE_B TEXT DEFAULT 'MEMBER',
            $COL_PAIRING_CODE TEXT,
            $COL_CREATED_ON TEXT,
            $COL_CREATED_AT TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_DEVICE_A_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE,
            FOREIGN KEY ($COL_DEVICE_B_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    // [Variables] now: String, v: ContentValues, id: Long | [Constants] TABLE_NAME, COL_* | [Dependencies] FirebaseComm | [Integration Points] DatabaseHelper, PairingRepository, MapActivity
    fun logPairing(db: SQLiteDatabase, pairId: String, devA: String, devB: String, status: String) {
        logPairingWithRoles(db, pairId, devA, devB, "MEMBER", "MEMBER", null, status)
    }

    // [Variables] now: String, v: ContentValues, id: Long | [Constants] TABLE_NAME, COL_* | [Dependencies] FirebaseComm | [Integration Points] DatabaseHelper, PairingRepository, MapActivity
    fun logPairingWithRoles(
        db: SQLiteDatabase,
        pairId: String,
        devA: String,
        devB: String,
        roleA: String = "MEMBER",
        roleB: String = "MEMBER",
        pairingCode: String? = null,
        status: String = "PAIRED"
    ) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_PAIR_ID, pairId)
            put(COL_DEVICE_A_ID, devA)
            put(COL_DEVICE_B_ID, devB)
            put(COL_PAIR_STATUS, status)
            put(COL_ROLE_A, roleA)
            put(COL_ROLE_B, roleB)
            if (pairingCode != null) put(COL_PAIRING_CODE, pairingCode)
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
