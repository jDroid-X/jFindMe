package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: groupId, groupName, createdBy, groupType, isActive, groupAvatar, inviteCode, now
 * Constants: TABLE_NAME = "groups", COL_GROUP_ID, COL_GROUP_NAME, COL_CREATED_BY, COL_GROUP_TYPE, COL_IS_ACTIVE, COL_GROUP_AVATAR, COL_INVITE_CODE, COL_CREATED_ON, COL_UPDATED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, GroupViewModel.kt, FamilyFriendsViewModel.kt, NetworkGatewayActivity.java
 */
object GroupTable {
    const val TABLE_NAME = "groups" // Renamed from groups_table for consistency

    const val COL_GROUP_ID = "group_id" // PK
    const val COL_GROUP_NAME = "group_name"
    const val COL_CREATED_BY = "created_by" // 🧬 V66.0: Internal Identity Handle (FK)
    const val COL_GROUP_TYPE = "group_type" // FAMILY, FRIENDS, WORKPLACE, SENTINEL
    const val COL_IS_ACTIVE = "is_active"
    const val COL_GROUP_AVATAR = "group_avatar"
    const val COL_INVITE_CODE = "invite_code"
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_GROUP_ID TEXT PRIMARY KEY,
            $COL_GROUP_NAME TEXT,
            $COL_CREATED_BY INTEGER,
            $COL_GROUP_TYPE TEXT DEFAULT 'FAMILY',
            $COL_IS_ACTIVE INTEGER DEFAULT 1,
            $COL_GROUP_AVATAR TEXT DEFAULT '',
            $COL_INVITE_CODE TEXT DEFAULT '',
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_CREATED_BY) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE SET NULL
        )
    """.trimIndent()

    @JvmOverloads
    fun upsert(
        db: SQLiteDatabase,
        groupId: String,
        name: String?,
        createdBy: Long?,
        groupType: String = "FAMILY",
        isActive: Boolean = true,
        groupAvatar: String = "",
        inviteCode: String = ""
    ) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_GROUP_ID, groupId)
            put(COL_GROUP_NAME, name)
            put(COL_CREATED_BY, createdBy)
            put(COL_GROUP_TYPE, groupType)
            put(COL_IS_ACTIVE, if (isActive) 1 else 0)
            put(COL_GROUP_AVATAR, groupAvatar)
            put(COL_INVITE_CODE, inviteCode)
            put(COL_UPDATED_AT, now)
        }
        val rows = db.update(TABLE_NAME, v, "$COL_GROUP_ID = ?", arrayOf(groupId))
        if (rows == 0) {
            v.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
