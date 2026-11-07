package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: orgId, name, address, clearanceLevel, now
 * Constants: TABLE_NAME = "organization", COL_ORG_ID, COL_ORG_NAME, COL_GEO_ADDRESS, COL_CLEARANCE_LEVEL, COL_CREATED_ON, COL_UPDATED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, UserOrgMapTable.kt, ProfileViewModel.kt
 */
object OrganizationTable {
    const val TABLE_NAME = "organization"

    const val COL_ORG_ID = "org_id" // PK
    const val COL_ORG_NAME = "org_name"
    const val COL_GEO_ADDRESS = "geo_address"
    const val COL_CLEARANCE_LEVEL = "clearance_level"
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ORG_ID TEXT PRIMARY KEY,
            $COL_ORG_NAME TEXT,
            $COL_GEO_ADDRESS TEXT,
            $COL_CLEARANCE_LEVEL TEXT DEFAULT 'STANDARD',
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT
        )
    """.trimIndent()

    @JvmOverloads
    fun upsert(db: SQLiteDatabase, orgId: String, name: String?, address: String?, clearanceLevel: String? = "STANDARD") {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_ORG_ID, orgId)
            put(COL_ORG_NAME, name)
            put(COL_GEO_ADDRESS, address)
            put(COL_CLEARANCE_LEVEL, clearanceLevel ?: "STANDARD")
            put(COL_UPDATED_AT, now)
        }
        val rows = db.update(TABLE_NAME, v, "$COL_ORG_ID = ?", arrayOf(orgId))
        if (rows == 0) {
            v.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
