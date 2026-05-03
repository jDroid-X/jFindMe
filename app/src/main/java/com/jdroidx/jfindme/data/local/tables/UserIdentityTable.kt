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
        
        // 🧬 Forensic Conflict Anchor: Check for existing Hardware ID or Agent ID
        var existingId = -1L
        val query = if (!uid2.isNullOrEmpty()) {
            db.rawQuery("SELECT $COL_ID FROM $TABLE_NAME WHERE $COL_UID_2 = ? OR $COL_UID_1 = ?", arrayOf(uid2, uid1))
        } else {
            db.rawQuery("SELECT $COL_ID FROM $TABLE_NAME WHERE $COL_UID_1 = ?", arrayOf(uid1))
        }
        
        query.use {
            if (it.moveToFirst()) existingId = it.getLong(0)
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
}
