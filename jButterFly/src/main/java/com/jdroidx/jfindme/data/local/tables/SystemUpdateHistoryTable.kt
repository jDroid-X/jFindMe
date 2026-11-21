/**
 * Auto-Login Architecture: System Update History & Bootloop Defense Ledger
 * Waterfall Method: Secured Over-The-Air Update Ledger & REA Specification Parity
 */
package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: update_id, version_name, version_code, source_url, sha256_hash, download_size_bytes, install_status, failure_reason, timestamp, created_on
 * Constants: TABLE_NAME = "system_update_history", COL_UPDATE_ID, COL_VERSION_NAME, COL_VERSION_CODE, COL_SOURCE_URL, COL_SHA256_HASH, COL_DOWNLOAD_SIZE_BYTES, COL_INSTALL_STATUS, COL_FAILURE_REASON, COL_TIMESTAMP, COL_CREATED_ON, STATUS_DOWNLOADED, STATUS_VERIFIED, STATUS_APPLIED, STATUS_FAILED
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase
 * Integration Points: DatabaseHelper.kt (onCreate, onUpgrade), SilentUpdateWorker, SystemUpdateActivity
 */
object SystemUpdateHistoryTable {
    const val TABLE_NAME = "system_update_history"

    const val COL_UPDATE_ID = "update_id"
    const val COL_VERSION_NAME = "version_name"
    const val COL_VERSION_CODE = "version_code"
    const val COL_SOURCE_URL = "source_url"
    const val COL_SHA256_HASH = "sha256_hash"
    const val COL_DOWNLOAD_SIZE_BYTES = "download_size_bytes"
    const val COL_INSTALL_STATUS = "install_status"
    const val COL_FAILURE_REASON = "failure_reason"
    const val COL_TIMESTAMP = "timestamp"
    const val COL_CREATED_ON = "created_on"

    const val STATUS_DOWNLOADED = "DOWNLOADED"
    const val STATUS_VERIFIED = "VERIFIED"
    const val STATUS_APPLIED = "APPLIED"
    const val STATUS_FAILED = "FAILED"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_UPDATE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_VERSION_NAME TEXT NOT NULL,
            $COL_VERSION_CODE INTEGER NOT NULL,
            $COL_SOURCE_URL TEXT DEFAULT '',
            $COL_SHA256_HASH TEXT DEFAULT '',
            $COL_DOWNLOAD_SIZE_BYTES INTEGER DEFAULT 0,
            $COL_INSTALL_STATUS TEXT NOT NULL DEFAULT '$STATUS_DOWNLOADED',
            $COL_FAILURE_REASON TEXT DEFAULT '',
            $COL_TIMESTAMP INTEGER NOT NULL,
            $COL_CREATED_ON TEXT DEFAULT ''
        )
    """.trimIndent()

    val CREATE_INDEX_STATUS = "CREATE INDEX IF NOT EXISTS idx_sys_update_status ON $TABLE_NAME ($COL_INSTALL_STATUS, $COL_TIMESTAMP DESC)"

    @JvmStatic
    fun recordUpdateAttempt(
        db: SQLiteDatabase,
        versionName: String,
        versionCode: Int,
        sourceUrl: String,
        sha256Hash: String,
        sizeBytes: Long,
        status: String = STATUS_DOWNLOADED,
        failureReason: String = ""
    ): Long {
        val cv = ContentValues().apply {
            put(COL_VERSION_NAME, versionName)
            put(COL_VERSION_CODE, versionCode)
            put(COL_SOURCE_URL, sourceUrl)
            put(COL_SHA256_HASH, sha256Hash)
            put(COL_DOWNLOAD_SIZE_BYTES, sizeBytes)
            put(COL_INSTALL_STATUS, status)
            put(COL_FAILURE_REASON, failureReason)
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_CREATED_ON, java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
        }
        return try {
            db.insert(TABLE_NAME, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }

    @JvmStatic
    fun markUpdateStatus(
        db: SQLiteDatabase,
        updateId: Long,
        status: String,
        failureReason: String = ""
    ): Boolean {
        val cv = ContentValues().apply {
            put(COL_INSTALL_STATUS, status)
            if (failureReason.isNotBlank()) {
                put(COL_FAILURE_REASON, failureReason)
            }
        }
        return try {
            db.update(TABLE_NAME, cv, "$COL_UPDATE_ID = ?", arrayOf(updateId.toString())) > 0
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic
    fun hasConsecutiveFailures(db: SQLiteDatabase, versionCode: Int, threshold: Int = 3): Boolean {
        val query = "SELECT COUNT(*) FROM $TABLE_NAME WHERE $COL_VERSION_CODE = ? AND $COL_INSTALL_STATUS = ?"
        return try {
            db.rawQuery(query, arrayOf(versionCode.toString(), STATUS_FAILED)).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getInt(0) >= threshold
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }
}
