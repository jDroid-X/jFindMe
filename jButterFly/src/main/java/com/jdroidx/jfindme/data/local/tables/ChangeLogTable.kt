package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm
import org.json.JSONObject

/**
 * Tier 5: Audit & Change Ledger
 * 📝 change_log: Forensic Change Audit Ledger for all Device state mutations.
 * Categories: Profile, Pairing_Devices, Family_and_Friends, Safehouse
 */
object ChangeLogTable {
    const val TABLE_NAME = "change_log"

    const val COL_ID = "id"
    const val COL_LOG_ID = "log_id"
    const val COL_IDENTITY_ID = "identity_id" // FK to UserIdentityTable.id
    const val COL_CATEGORY = "category"       // Profile, Pairing_Devices, Family_and_Friends, Safehouse
    const val COL_ACTION = "action"           // CREATE, UPDATE, DELETE, SYNC, LINK, ASSIGN
    const val COL_ENTITY_ID = "entity_id"     // ID of the target modified entity
    const val COL_CHANGED_BY = "changed_by"   // Agent ID or UID who initiated change
    const val COL_DETAILS_JSON = "details_json" // Serialized key-value delta / metadata
    const val COL_TIMESTAMP = "timestamp"     // Forensic timestamp
    const val COL_CREATED_ON = "created_on"

    // Standard Category Constants
    const val CATEGORY_PROFILE = "Profile"
    const val CATEGORY_PAIRING = "Pairing_Devices"
    const val CATEGORY_FAMILY_FRIENDS = "Family_and_Friends"
    const val CATEGORY_SAFEHOUSE = "Safehouse"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_LOG_ID TEXT UNIQUE NOT NULL,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_CATEGORY TEXT NOT NULL,
            $COL_ACTION TEXT NOT NULL,
            $COL_ENTITY_ID TEXT,
            $COL_CHANGED_BY TEXT,
            $COL_DETAILS_JSON TEXT,
            $COL_TIMESTAMP TEXT NOT NULL,
            $COL_CREATED_ON TEXT,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    fun logChange(
        db: SQLiteDatabase,
        identityId: Long,
        logId: String,
        category: String,
        action: String,
        entityId: String?,
        changedBy: String?,
        details: Map<String, Any?>
    ) {
        val now = FirebaseComm.getForensicTimestamp()
        val jsonStr = try {
            JSONObject(details).toString()
        } catch (e: Exception) {
            "{}"
        }
        val v = ContentValues().apply {
            put(COL_LOG_ID, logId)
            put(COL_IDENTITY_ID, identityId)
            put(COL_CATEGORY, category)
            put(COL_ACTION, action)
            put(COL_ENTITY_ID, entityId ?: "")
            put(COL_CHANGED_BY, changedBy ?: "")
            put(COL_DETAILS_JSON, jsonStr)
            put(COL_TIMESTAMP, now)
            put(COL_CREATED_ON, now)
        }
        db.insertWithOnConflict(TABLE_NAME, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
