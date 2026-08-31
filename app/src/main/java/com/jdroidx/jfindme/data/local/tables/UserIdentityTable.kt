/**
 * Auto-Login Architecture: Forensic Identity Ledger
 * Waterfall Method: Secured Hardware Lock & Auth Mapping
 * V60.2 RESTORED FORENSIC ANCHOR
 */
package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * Tier 1: Identity Core (Single Source of Truth)
 *🧬 user_identity: The absolute forensic anchor for all nodes.
 */
object UserIdentityTable {
    const val TABLE_NAME = "user_identity"

    const val COL_ID = "id"                          // PK (Internal)
    const val COL_UID_1 = "uid_1"                    // Forensic Agent ID (Locked)
    const val COL_UID_2 = "uid_2"                    // Hardware ID (ANDROID_ID)
    const val COL_AUTH_UID = "auth_uid"              // Cloud Identity (Firebase)
    const val COL_NODE_INDEX = "node_index"          // 🧬 L7: 1, 2, 3... per UID_1
    
    // --- V60.2 RESTORED Forensic Identity Block ---
    const val COL_FULL_NAME = "id_full_name"         // Display Identity (Blank by default)
    const val COL_SESSION_STATE = "st_session_state" // Connectivity Socket
    const val COL_IS_ADMIN = "is_admin"              // Authority Level (Blank by default)
    const val COL_OWNER_NAME = "owner_name"          // Provenance Ledger (Blank by default)
    
    const val COL_CONNECTED_ID = "connected_id"      // Active Pairing Bridge
    const val COL_QR_REF = "qr_code_ref"             // Handshake Trace
    
    const val COL_STATUS = "status"
    const val COL_INSTALL_EPOCH = "install_epoch_id"         // 🧬 Installation/Session Epoch UUID
    const val COL_IS_CURRENT_SESSION = "is_current_session" // 🧬 1 = Active Session, 0 = Archived Trace
    const val COL_CREATED_ON = "created_on"          // 🧬 Forensic String: YYYYMMDDHHMMSSSS
    const val COL_CREATED_AT = "created_at"          // 🧬 Forensic String: YYYYMMDDHHMMSSSS
    const val COL_UPDATED_AT = "updated_at"          // 🧬 Forensic String: YYYYMMDDHHMMSSSS

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_UID_1 TEXT NOT NULL UNIQUE,
            $COL_UID_2 TEXT UNIQUE,
            $COL_AUTH_UID TEXT DEFAULT '',
            $COL_NODE_INDEX INTEGER DEFAULT 1,
            $COL_FULL_NAME TEXT DEFAULT '',
            $COL_SESSION_STATE TEXT DEFAULT 'OFFLINE',
            $COL_IS_ADMIN TEXT DEFAULT '',
            $COL_OWNER_NAME TEXT DEFAULT '',
            $COL_CONNECTED_ID TEXT DEFAULT '',
            $COL_QR_REF TEXT DEFAULT '',
            $COL_STATUS TEXT DEFAULT 'Active',
            $COL_INSTALL_EPOCH TEXT DEFAULT '',
            $COL_IS_CURRENT_SESSION INTEGER DEFAULT 1,
            $COL_CREATED_ON TEXT,
            $COL_CREATED_AT TEXT,
            $COL_UPDATED_AT TEXT
        )
    """.trimIndent()

    fun insertOrUpdate(db: SQLiteDatabase, uid1: String, uid2: String?, authUid: String? = "", 
                       fullName: String? = "", isAdmin: String? = "", owner: String? = "",
                       connectedId: String? = "", qrRef: String? = "", nodeIndex: Int = 1): Long {
        val now = FirebaseComm.getForensicTimestamp() // 🧬 V60.2: Use 16-char precise string
        val v = ContentValues().apply {
            put(COL_UID_1, uid1)
            put(COL_UID_2, uid2)
            put(COL_AUTH_UID, authUid ?: "")
            put(COL_FULL_NAME, fullName ?: "")
            put(COL_IS_ADMIN, isAdmin ?: "")
            put(COL_OWNER_NAME, if (owner.isNullOrEmpty()) (fullName ?: "") else owner)
            put(COL_CONNECTED_ID, connectedId ?: "")
            put(COL_QR_REF, qrRef ?: "")
            put(COL_NODE_INDEX, nodeIndex)
            put(COL_UPDATED_AT, now)
        }
        
        // 🧬 Forensic Conflict Anchor: Check for existing Agent ID (uid_1) first (Single Source of Truth)
        var existingId = -1L
        db.rawQuery("SELECT $COL_ID FROM $TABLE_NAME WHERE $COL_UID_1 = ?", arrayOf(uid1)).use {
            if (it.moveToFirst()) existingId = it.getLong(0)
        }

        // If not found by uid_1, but uid_2 is provided, check for existing hardware ID
        if (existingId == -1L && !uid2.isNullOrEmpty()) {
            db.rawQuery("SELECT $COL_ID FROM $TABLE_NAME WHERE $COL_UID_2 = ?", arrayOf(uid2)).use {
                if (it.moveToFirst()) existingId = it.getLong(0)
            }
        }

        // 🛡️ UNIQUE CONSTRAINT RESOLVER: If uid_2 is provided, prevent unique constraint crashes by unsetting it on any other row
        if (!uid2.isNullOrEmpty()) {
            db.execSQL("UPDATE $TABLE_NAME SET $COL_UID_2 = NULL WHERE $COL_UID_2 = ? AND $COL_UID_1 != ?", arrayOf(uid2, uid1))
        }

        return if (existingId != -1L) {
            // Update existing record by internal ID to maintain referential integrity
            db.update(TABLE_NAME, v, "$COL_ID = ?", arrayOf(existingId.toString()))
            existingId
        } else {
            // New Node: Securely insert with Forensic Creation Marker
            v.put(COL_CREATED_ON, now)
            v.put(COL_CREATED_AT, now)
            db.insert(TABLE_NAME, null, v)
        }
    }

    /**
     * 🧬 V70.0: Epoch-Aware Session Isolation (No Merge, No Overwrite, No Trace Deletion)
     * When a new login or fresh install occurs, older sessions on this device are archived
     * (marked is_current_session = 0), preserving all location history under their historical identity_id.
     */
    fun insertOrArchiveSession(
        db: SQLiteDatabase,
        uid1: String,
        uid2: String?,
        authUid: String? = "",
        epochId: String = "",
        fullName: String? = "",
        isAdmin: String? = "",
        owner: String? = "",
        connectedId: String? = "",
        qrRef: String? = "",
        nodeIndex: Int = 1
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()

        // 1. Mark previous active sessions for this hardware/agent as ARCHIVED (Never Delete)
        try {
            val archiveValues = ContentValues().apply {
                put(COL_IS_CURRENT_SESSION, 0)
                put(COL_STATUS, "ARCHIVED_TRACE")
                put(COL_UPDATED_AT, now)
            }
            if (!uid2.isNullOrEmpty()) {
                db.update(TABLE_NAME, archiveValues, "($COL_UID_2 = ? OR $COL_UID_1 = ?) AND $COL_AUTH_UID != ?", arrayOf(uid2, uid1, authUid ?: ""))
            }
        } catch (e: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.e("UserIdentityTable", "Session archive update note: ${e.message}")
        }

        // 2. Check if a record already exists for THIS specific (uid_1 + auth_uid)
        var targetId = -1L
        try {
            val cursor = db.query(
                TABLE_NAME,
                arrayOf(COL_ID),
                "$COL_UID_1 = ? AND $COL_AUTH_UID = ?",
                arrayOf(uid1, authUid ?: ""),
                null, null, null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    targetId = it.getLong(0)
                }
            }
        } catch (e: Exception) { }

        val v = ContentValues().apply {
            put(COL_UID_1, uid1)
            put(COL_UID_2, uid2)
            put(COL_AUTH_UID, authUid ?: "")
            put(COL_FULL_NAME, fullName ?: "")
            put(COL_IS_ADMIN, isAdmin ?: "")
            put(COL_OWNER_NAME, if (owner.isNullOrEmpty()) (fullName ?: "") else owner)
            put(COL_CONNECTED_ID, connectedId ?: "")
            put(COL_QR_REF, qrRef ?: "")
            put(COL_NODE_INDEX, nodeIndex)
            put(COL_INSTALL_EPOCH, epochId)
            put(COL_IS_CURRENT_SESSION, 1)
            put(COL_STATUS, "Active")
            put(COL_UPDATED_AT, now)
        }

        // Prevent hardware ID collision on older rows
        if (!uid2.isNullOrEmpty()) {
            try {
                db.execSQL("UPDATE $TABLE_NAME SET $COL_UID_2 = NULL WHERE $COL_UID_2 = ? AND $COL_UID_1 != ?", arrayOf(uid2, uid1))
            } catch (e: Exception) { }
        }

        return if (targetId != -1L) {
            db.update(TABLE_NAME, v, "$COL_ID = ?", arrayOf(targetId.toString()))
            targetId
        } else {
            v.put(COL_CREATED_ON, now)
            v.put(COL_CREATED_AT, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
