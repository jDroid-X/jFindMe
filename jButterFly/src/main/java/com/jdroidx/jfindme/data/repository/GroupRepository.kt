package com.jdroidx.jfindme.data.repository

import android.content.Context
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.local.tables.GroupMemberTable
import com.jdroidx.jfindme.data.local.tables.InvitationsTable
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [Variables]: context, db, ioScope
 * [Constants]: TAG
 * [Dependencies]: DatabaseHelper, FirebaseComm, GroupMemberTable, InvitationsTable
 * [Integration Points]: FamilyFriendsActivity, NetworkGatewayActivity, GroupActivity, PersonDetailsActivity
 *
 * 🚀 GroupRepository (OOPS-based MVC SSOT)
 * Centralizes all Group membership, roles (Parent, Child, Co-Guardian), pairing invitations,
 * and roster synchronization signals. Eliminates duplicate SQLite queries in UI activities.
 */
class GroupRepository private constructor(private val context: Context) {

    private val db = DatabaseHelper.getInstance(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "GroupRepository"

    companion object {
        @Volatile
        private var instance: GroupRepository? = null

        @JvmStatic
        fun getInstance(context: Context): GroupRepository {
            return instance ?: synchronized(this) {
                instance ?: GroupRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Retrieves offline roster members from local SQLite SSOT.
     */
    fun getOfflineRoster(groupId: String? = null): List<Map<String, Any>> {
        return db.getOfflineRoster(groupId)
    }

    /**
     * Updates the role of a group member (e.g., GUARDIAN, CHILD, SPOUSE, FRIEND)
     * and broadcasts the synchronization signal to the circle.
     */
    fun updateMemberRole(
        groupId: String,
        memberAgentId: String,
        newRole: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (memberAgentId.isBlank()) {
            onComplete(false)
            return
        }

        ioScope.launch {
            try {
                // 1. Update in local SQLite
                val writableDb = db.writableDatabase
                val values = android.content.ContentValues().apply {
                    put(GroupMemberTable.COL_ROLE, newRole)
                }
                writableDb.update(
                    GroupMemberTable.TABLE_NAME,
                    values,
                    "${GroupMemberTable.COL_IDENTITY_ID} = ? OR ${GroupMemberTable.COL_AGENT_ID} = ?",
                    arrayOf(memberAgentId, memberAgentId)
                )

                // 2. Broadcast refresh signal via Firebase
                if (groupId.isNotBlank()) {
                    FirebaseComm.relationshipsRef
                        .child(groupId)
                        .child("members")
                        .child(memberAgentId)
                        .child("role")
                        .setValue(newRole)

                    FirebaseComm.relationshipsRef
                        .child(groupId)
                        .child("signals")
                        .child("refresh_roster")
                        .setValue(System.currentTimeMillis())
                }

                SystemLogger.i(TAG, "Updated role for $memberAgentId to $newRole in group $groupId")
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to update member role: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    /**
     * Signals all devices in the circle to refresh their local roster.
     */
    fun broadcastRosterRefresh(groupId: String) {
        if (groupId.isBlank()) return
        ioScope.launch {
            try {
                FirebaseComm.relationshipsRef
                    .child(groupId)
                    .child("signals")
                    .child("refresh_roster")
                    .setValue(System.currentTimeMillis())
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to broadcast roster refresh: ${e.message}")
            }
        }
    }

    /**
     * Records a newly generated pairing invitation code with its NIST-compliant TTL.
     */
    fun recordInvitation(
        inviteCode: String,
        groupId: String,
        ttlMinutes: Int,
        targetRole: String,
        onComplete: (Boolean) -> Unit
    ) {
        ioScope.launch {
            try {
                val writableDb = db.writableDatabase
                val values = android.content.ContentValues().apply {
                    put(InvitationsTable.COL_CODE, inviteCode)
                    put(InvitationsTable.COL_GROUP_ID, groupId)
                    put(InvitationsTable.COL_ROLE, targetRole)
                    put(InvitationsTable.COL_STATUS, "ACTIVE")
                    put(InvitationsTable.COL_EXPIRES_AT, System.currentTimeMillis() + (ttlMinutes * 60 * 1000L))
                }
                writableDb.insertWithOnConflict(
                    InvitationsTable.TABLE_NAME,
                    null,
                    values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error recording invitation: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }
}
