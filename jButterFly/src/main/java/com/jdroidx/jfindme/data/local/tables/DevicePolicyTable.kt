/**
 * Auto-Login Architecture: Device Policy State Sentinel Ledger
 * Waterfall Method: Secured Policy Storage & REA Specification Parity
 */
package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, device_id, policy_key, policy_value, is_active, created_on, updated_at
 * Constants: TABLE_NAME = "device_policy_state", COL_ID, COL_DEVICE_ID, COL_POLICY_KEY, COL_POLICY_VALUE, COL_IS_ACTIVE, COL_CREATED_ON, COL_UPDATED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase
 * Integration Points: DatabaseHelper.kt (onCreate, onUpgrade, getDevicePolicy, setDevicePolicy)
 */
object DevicePolicyTable {
    const val TABLE_NAME = "device_policy_state"

    const val COL_ID = "id"
    const val COL_DEVICE_ID = "device_id"
    const val COL_POLICY_KEY = "policy_key"
    const val COL_POLICY_VALUE = "policy_value"
    const val COL_IS_ACTIVE = "is_active"
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_DEVICE_ID TEXT DEFAULT '',
            $COL_POLICY_KEY TEXT UNIQUE NOT NULL,
            $COL_POLICY_VALUE TEXT NOT NULL,
            $COL_IS_ACTIVE INTEGER DEFAULT 1,
            $COL_CREATED_ON TEXT DEFAULT '',
            $COL_UPDATED_AT INTEGER NOT NULL
        )
    """.trimIndent()

    // [Variables]: db, key, value, isActive, deviceId, cv, selection, args, cursor
    // [Constants]: TABLE_NAME, COL_POLICY_KEY, COL_POLICY_VALUE, COL_IS_ACTIVE, COL_DEVICE_ID, COL_UPDATED_AT
    // [Dependencies]: SQLiteDatabase, ContentValues
    // [Integration Points]: DatabaseHelper (getDevicePolicy, setDevicePolicy) & Multi-Node Group-By Hierarchy
    fun setPolicy(db: SQLiteDatabase, key: String, value: String, isActive: Boolean = true, deviceId: String = ""): Long {
        val cv = ContentValues().apply {
            put(COL_POLICY_KEY, key)
            put(COL_POLICY_VALUE, value)
            put(COL_IS_ACTIVE, if (isActive) 1 else 0)
            if (deviceId.isNotEmpty()) {
                put(COL_DEVICE_ID, deviceId)
            }
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        return db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getPolicy(db: SQLiteDatabase, key: String): String? {
        return getPolicyForDevice(db, key, "")
    }

    fun getPolicyForDevice(db: SQLiteDatabase, key: String, deviceId: String = ""): String? {
        return try {
            val selection = if (deviceId.isNotEmpty()) {
                "$COL_POLICY_KEY = ? AND ($COL_DEVICE_ID = ? OR $COL_DEVICE_ID = '') AND $COL_IS_ACTIVE = 1"
            } else {
                "$COL_POLICY_KEY = ? AND $COL_IS_ACTIVE = 1"
            }
            val args = if (deviceId.isNotEmpty()) arrayOf(key, deviceId) else arrayOf(key)
            db.query(
                TABLE_NAME,
                arrayOf(COL_POLICY_VALUE),
                selection,
                args,
                null,
                null,
                "$COL_DEVICE_ID DESC"
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

