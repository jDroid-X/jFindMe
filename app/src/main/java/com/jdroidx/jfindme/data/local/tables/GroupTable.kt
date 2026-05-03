package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * Tier 3: Relationship Graph
 * 👥 group: Master Group Registry
 */
object GroupTable {
    const val TABLE_NAME = "groups" // Renamed from groups_table for consistency

    const val COL_GROUP_ID = "group_id" // PK
    const val COL_GROUP_NAME = "group_name"
    const val COL_CREATED_BY = "created_by" // 🧬 V66.0: Internal Identity Handle (FK)
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_GROUP_ID TEXT PRIMARY KEY,
            $COL_GROUP_NAME TEXT,
            $COL_CREATED_BY INTEGER,
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_CREATED_BY) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE SET NULL
        )
    """.trimIndent()

    fun upsert(db: SQLiteDatabase, groupId: String, name: String?, createdBy: Long?) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_GROUP_ID, groupId)
            put(COL_GROUP_NAME, name)
            put(COL_CREATED_BY, createdBy)
            put(COL_UPDATED_AT, now)
        }
        val rows = db.update(TABLE_NAME, v, "$COL_GROUP_ID = ?", arrayOf(groupId))
        if (rows == 0) {
            v.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
