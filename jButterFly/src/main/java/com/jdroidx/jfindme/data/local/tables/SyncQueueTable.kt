package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * Tier 5: System Reliability
 * 🔁 sync_queue: Tracking cloud synchronization state
 */
object SyncQueueTable {
    const val TABLE_NAME = "sync_queue"

    const val COL_SYNC_ID = "sync_id"
    const val COL_ENTITY_TYPE = "entity_type" // e.g., LOCATION, ALIVE, PROFILE
    const val COL_ENTITY_ID = "entity_id"
    const val COL_STATUS = "status" // PENDING/SENT/ACKED/FAILED
    const val COL_RETRY_COUNT = "retry_count"
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_SYNC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_ENTITY_TYPE TEXT NOT NULL,
            $COL_ENTITY_ID TEXT NOT NULL,
            $COL_STATUS TEXT DEFAULT 'PENDING',
            $COL_RETRY_COUNT INTEGER DEFAULT 0,
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT
        )
    """.trimIndent()

    fun queueItem(db: SQLiteDatabase, type: String, id: String) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues().apply {
            put(COL_ENTITY_TYPE, type)
            put(COL_ENTITY_ID, id)
            put(COL_CREATED_ON, now)
            put(COL_UPDATED_AT, now)
        }
        db.insert(TABLE_NAME, null, v)
    }

    fun markSuccess(db: SQLiteDatabase, syncId: Int) {
        val v = ContentValues().apply {
            put(COL_STATUS, "ACKED")
            put(COL_UPDATED_AT, FirebaseComm.getForensicTimestamp())
        }
        db.update(TABLE_NAME, v, "$COL_SYNC_ID=?", arrayOf(syncId.toString()))
    }
}
