package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, identityId, configKey, configValue, description, isEnabled, updatedAt
 * Constants: TABLE_NAME = "stealth_schedule_config", COL_ID, COL_IDENTITY_ID, COL_CONFIG_KEY, COL_CONFIG_VALUE, COL_DESCRIPTION, COL_IS_ENABLED, COL_UPDATED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase
 * Integration Points: DatabaseHelper.kt, SettingsActivity.kt, TrackingService.kt, PrivacyActivity.java
 */
object StealthScheduleConfigTable {
    const val TABLE_NAME = "stealth_schedule_config"

    const val COL_ID = "id"
    const val COL_IDENTITY_ID = "identity_id" // FK to UserIdentityTable.id
    const val COL_CONFIG_KEY = "config_key" // PK / Unique
    const val COL_CONFIG_VALUE = "config_value"
    const val COL_DESCRIPTION = "description"
    const val COL_IS_ENABLED = "is_enabled"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_IDENTITY_ID INTEGER DEFAULT 0,
            $COL_CONFIG_KEY TEXT UNIQUE NOT NULL,
            $COL_CONFIG_VALUE TEXT NOT NULL,
            $COL_DESCRIPTION TEXT DEFAULT '',
            $COL_IS_ENABLED INTEGER DEFAULT 1,
            $COL_UPDATED_AT INTEGER NOT NULL
        )
    """.trimIndent()

    fun setConfig(db: SQLiteDatabase, key: String, value: String, description: String = "", identityId: Long = 0L): Long {
        val cv = ContentValues().apply {
            put(COL_IDENTITY_ID, identityId)
            put(COL_CONFIG_KEY, key)
            put(COL_CONFIG_VALUE, value)
            put(COL_DESCRIPTION, description)
            put(COL_IS_ENABLED, 1)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        return db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getConfig(db: SQLiteDatabase, key: String, defaultValue: String = ""): String {
        return try {
            db.query(
                TABLE_NAME,
                arrayOf(COL_CONFIG_VALUE),
                "$COL_CONFIG_KEY = ? AND $COL_IS_ENABLED = 1",
                arrayOf(key),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }
}

