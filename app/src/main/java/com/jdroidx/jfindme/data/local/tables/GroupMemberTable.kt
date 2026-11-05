package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, groupId, identityId, role, permissionsMask, agentId, phoneNumber, circleType, isMuted, ts, now
 * Constants: TABLE_NAME = "group_members", COL_ID, COL_GROUP_ID, COL_IDENTITY_ID, COL_ROLE, COL_PERMISSIONS_MASK, COL_AGENT_ID, COL_PHONE_NUMBER, COL_CIRCLE_TYPE, COL_IS_MUTED, MASTER_ROSTER, COL_CREATED_ON, COL_JOINED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, FamilyFriendsViewModel.kt, PersonDetailsViewModel.kt
 */
object GroupMemberTable {
    const val TABLE_NAME = "group_members"

    const val COL_ID = "id"
    const val COL_GROUP_ID = "group_id" // FK
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V65.0: Secure Internal Link (FK)
    const val COL_ROLE = "role"
    const val COL_PERMISSIONS_MASK = "permissions_mask"
    const val COL_AGENT_ID = "agent_id"
    const val COL_PHONE_NUMBER = "phone_number"
    const val MASTER_ROSTER = "master_roster" // admin/moderator/member

    // 🛡️ V79 REA Refabrication Columns: Circle Archetype & Notification Suppression
    const val COL_CIRCLE_TYPE = "circle_type" // FAMILY, FRIENDS, WORK, SENTINEL
    const val COL_IS_MUTED = "is_muted" // 0 = normal, 1 = muted

    const val COL_CREATED_ON = "created_on"
    const val COL_JOINED_AT = "joined_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_GROUP_ID TEXT NOT NULL,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_ROLE TEXT DEFAULT 'member',
            $COL_PERMISSIONS_MASK TEXT DEFAULT '0xFF',
            $COL_AGENT_ID TEXT,
            $COL_PHONE_NUMBER TEXT,
            $COL_CIRCLE_TYPE TEXT DEFAULT 'FAMILY',
            $COL_IS_MUTED INTEGER DEFAULT 0,
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

    @JvmOverloads
    fun upsert(
        db: SQLiteDatabase,
        groupId: String,
        identityId: Long,
        role: String,
        ts: Long = System.currentTimeMillis(),
        permissionsMask: String? = null,
        agentId: String? = null,
        phoneNumber: String? = null,
        circleType: String? = null,
        isMuted: Boolean? = null
    ) {
        val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_GROUP_ID, groupId)
            put(COL_IDENTITY_ID, identityId)
            put(COL_ROLE, role)
            put(COL_JOINED_AT, ts)
            if (!permissionsMask.isNullOrEmpty()) put(COL_PERMISSIONS_MASK, permissionsMask)
            if (!agentId.isNullOrEmpty()) put(COL_AGENT_ID, agentId)
            if (!phoneNumber.isNullOrEmpty()) put(COL_PHONE_NUMBER, phoneNumber)
            if (!circleType.isNullOrEmpty()) put(COL_CIRCLE_TYPE, circleType)
            if (isMuted != null) put(COL_IS_MUTED, if (isMuted) 1 else 0)
        }
        val rows = db.update(TABLE_NAME, v, "$COL_GROUP_ID = ? AND $COL_IDENTITY_ID = ?", arrayOf(groupId, identityId.toString()))
        if (rows == 0) {
            v.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
