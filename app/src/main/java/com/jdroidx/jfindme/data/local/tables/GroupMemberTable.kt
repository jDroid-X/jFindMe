package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * Tier 3: Relationship Graph
 * 🔗 group_member: Many-to-many bridge
 */
object GroupMemberTable {
    const val TABLE_NAME = "group_members"

    const val COL_ID = "id"
    const val COL_GROUP_ID = "group_id" // FK
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V65.0: Secure Internal Link (FK)
    const val COL_ROLE = "role" // admin/moderator/member
    const val COL_CREATED_ON = "created_on"
    const val COL_JOINED_AT = "joined_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_GROUP_ID TEXT NOT NULL,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_ROLE TEXT DEFAULT 'member',
            $COL_CREATED_ON TEXT,
            $COL_JOINED_AT INTEGER,
            FOREIGN KEY ($COL_GROUP_ID) REFERENCES ${GroupTable.TABLE_NAME}(${GroupTable.COL_GROUP_ID}) ON DELETE CASCADE,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE,
            UNIQUE($COL_GROUP_ID, $COL_IDENTITY_ID)
        )
    """.trimIndent()

    fun addMember(db: SQLiteDatabase, groupId: String, identityId: Long, role: String?) {
        upsert(db, groupId, identityId, role ?: "member")
    }

    fun upsert(db: SQLiteDatabase, groupId: String, identityId: Long, role: String, ts: Long = System.currentTimeMillis()) {
        val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_GROUP_ID, groupId)
            put(COL_IDENTITY_ID, identityId)
            put(COL_ROLE, role)
            put(COL_JOINED_AT, ts)
        }
        val rows = db.update(TABLE_NAME, v, "$COL_GROUP_ID = ? AND $COL_IDENTITY_ID = ?", arrayOf(groupId, identityId.toString()))
        if (rows == 0) {
            v.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
