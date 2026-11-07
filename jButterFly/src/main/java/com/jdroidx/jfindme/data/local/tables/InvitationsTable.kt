package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, token, groupId, initiatorIdentityId, initiatorAgentId, targetPhoneOrEmail, inviteType, targetRole, targetCircleType, status, expiresAt, acceptedByAgentId, acceptedAt, createdOn, updatedAt
 * Constants: TABLE_NAME = "invitations", COL_ID, COL_TOKEN, COL_GROUP_ID, COL_INITIATOR_IDENTITY_ID, COL_INITIATOR_AGENT_ID, COL_TARGET_PHONE_OR_EMAIL, COL_INVITE_TYPE, COL_TARGET_ROLE, COL_TARGET_CIRCLE_TYPE, COL_STATUS, COL_EXPIRES_AT, COL_ACCEPTED_BY_AGENT_ID, COL_ACCEPTED_AT, COL_CREATED_ON, COL_UPDATED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, NetworkGatewayActivity.java, FamilyFriendsViewModel.kt, PairingRepository.kt
 */
object InvitationsTable {
    const val TABLE_NAME = "invitations"

    const val COL_ID = "id"
    const val COL_TOKEN = "token" // 6-digit PIN or UUID QR token
    const val COL_GROUP_ID = "group_id" // FK to groups.group_id
    const val COL_INITIATOR_IDENTITY_ID = "initiator_identity_id" // FK to UserIdentityTable.id
    const val COL_INITIATOR_AGENT_ID = "initiator_agent_id"
    const val COL_TARGET_PHONE_OR_EMAIL = "target_phone_or_email"
    const val COL_INVITE_TYPE = "invite_type" // QR_CODE, PAIR_CODE_6DIGIT, DIRECT_INVITE
    const val COL_TARGET_ROLE = "target_role" // member, guardian, child
    const val COL_TARGET_CIRCLE_TYPE = "target_circle_type" // FAMILY, FRIENDS, WORK, SENTINEL
    const val COL_STATUS = "status" // PENDING, ACCEPTED, EXPIRED, REVOKED
    const val COL_EXPIRES_AT = "expires_at"
    const val COL_ACCEPTED_IDENTITY_ID = "accepted_identity_id"
    const val COL_ACCEPTED_BY_AGENT_ID = "accepted_by_agent_id"
    const val COL_ACCEPTED_AT = "accepted_at"
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_TOKEN TEXT UNIQUE NOT NULL,
            $COL_GROUP_ID TEXT NOT NULL,
            $COL_INITIATOR_IDENTITY_ID INTEGER NOT NULL,
            $COL_INITIATOR_AGENT_ID TEXT NOT NULL,
            $COL_TARGET_PHONE_OR_EMAIL TEXT DEFAULT '',
            $COL_INVITE_TYPE TEXT DEFAULT 'QR_CODE',
            $COL_TARGET_ROLE TEXT DEFAULT 'member',
            $COL_TARGET_CIRCLE_TYPE TEXT DEFAULT 'FAMILY',
            $COL_STATUS TEXT DEFAULT 'PENDING',
            $COL_EXPIRES_AT INTEGER NOT NULL,
            $COL_ACCEPTED_IDENTITY_ID INTEGER DEFAULT 0,
            $COL_ACCEPTED_BY_AGENT_ID TEXT DEFAULT '',
            $COL_ACCEPTED_AT INTEGER DEFAULT 0,
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_INITIATOR_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    fun createInvitation(
        db: SQLiteDatabase,
        token: String,
        groupId: String,
        initiatorId: Long,
        initiatorAgentId: String,
        targetContact: String = "",
        inviteType: String = "QR_CODE",
        targetRole: String = "member",
        targetCircleType: String = "FAMILY",
        validityMinutes: Long = 60
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()
        val expiresAt = System.currentTimeMillis() + (validityMinutes * 60 * 1000L)
        val cv = ContentValues().apply {
            put(COL_TOKEN, token)
            put(COL_GROUP_ID, groupId)
            put(COL_INITIATOR_IDENTITY_ID, initiatorId)
            put(COL_INITIATOR_AGENT_ID, initiatorAgentId)
            put(COL_TARGET_PHONE_OR_EMAIL, targetContact)
            put(COL_INVITE_TYPE, inviteType)
            put(COL_TARGET_ROLE, targetRole)
            put(COL_TARGET_CIRCLE_TYPE, targetCircleType)
            put(COL_STATUS, "PENDING")
            put(COL_EXPIRES_AT, expiresAt)
            put(COL_CREATED_ON, now)
            put(COL_UPDATED_AT, now)
        }
        return db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun acceptInvitation(db: SQLiteDatabase, token: String, accepterAgentId: String): Boolean {
        val now = FirebaseComm.getForensicTimestamp()
        val cv = ContentValues().apply {
            put(COL_STATUS, "ACCEPTED")
            put(COL_ACCEPTED_BY_AGENT_ID, accepterAgentId)
            put(COL_ACCEPTED_AT, System.currentTimeMillis())
            put(COL_UPDATED_AT, now)
        }
        val rows = db.update(TABLE_NAME, cv, "$COL_TOKEN = ? AND $COL_STATUS = 'PENDING'", arrayOf(token))
        return rows > 0
    }
}

