package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm
import java.util.UUID

/**
 * [Variables] transactionId: String, shareId: String, actorDeviceId: String, recipientDeviceId: String, previousStage: String, newStage: String, attemptNumber: Int, channelMode: String, notesOrReason: String, epochMs: Long, forensicTimestamp: String
 * [Constants] TABLE_NAME = "device_sharing_transactions", COL_*, STAGE_*
 * [Dependencies] android.database.sqlite.SQLiteDatabase, android.content.ContentValues, FirebaseComm, java.util.UUID
 * [Integration Points] DatabaseHelper.kt, PairingRepository.kt, TrackingService.kt, MapActivity.java
 *
 * 🛡️ Tier 2: Transactional Audit Ledger Table for Device Sharing Lifecycle
 * Chronologically records every stage transition (REQUESTED, APPROVED, ACCEPTED, DECLINED, REJECTED, TIMED_OUT, EXPIRED, RE_REQUESTED, REVOKED).
 */
object DeviceSharingTransactionTable {
    const val TABLE_NAME = "device_sharing_transactions"

    const val COL_TRANSACTION_ID = "transaction_id"       // PK: UUID / "TX_xxxx"
    const val COL_SHARE_ID = "share_id"                   // FK -> device_sharing.share_id
    const val COL_ACTOR_DEVICE_ID = "actor_device_id"     // Device triggering this state change
    const val COL_RECIPIENT_DEVICE_ID = "recipient_device_id" // Target recipient node
    const val COL_PREVIOUS_STAGE = "previous_stage"       // Prior stage
    const val COL_NEW_STAGE = "new_stage"                 // Target stage
    const val COL_ATTEMPT_NUMBER = "attempt_number"       // 1, 2, 3...
    const val COL_CHANNEL_MODE = "channel_mode"           // CLOUD_FIREBASE, OFFLINE_P2P, SMS_FAILOVER
    const val COL_NOTES_OR_REASON = "notes_or_reason"     // Reason string / rejection / timeout note
    const val COL_EPOCH_MS = "epoch_ms"                   // System millisecond timestamp
    const val COL_FORENSIC_TIMESTAMP = "forensic_timestamp"

    // Recognized State Taxonomy Constants
    const val STAGE_NONE = "NONE"
    const val STAGE_REQUESTED = "REQUESTED"
    const val STAGE_APPROVED = "APPROVED"
    const val STAGE_ACCEPTED = "ACCEPTED"
    const val STAGE_ACTIVE = "ACTIVE"
    const val STAGE_DECLINED = "DECLINED"
    const val STAGE_REJECTED = "REJECTED"
    const val STAGE_TIMED_OUT = "TIMED_OUT"
    const val STAGE_EXPIRED = "EXPIRED"
    const val STAGE_RE_REQUESTED = "RE_REQUESTED"
    const val STAGE_REVOKED = "REVOKED"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_TRANSACTION_ID TEXT PRIMARY KEY,
            $COL_SHARE_ID TEXT NOT NULL,
            $COL_ACTOR_DEVICE_ID TEXT NOT NULL,
            $COL_RECIPIENT_DEVICE_ID TEXT NOT NULL,
            $COL_PREVIOUS_STAGE TEXT DEFAULT 'NONE',
            $COL_NEW_STAGE TEXT NOT NULL,
            $COL_ATTEMPT_NUMBER INTEGER DEFAULT 1,
            $COL_CHANNEL_MODE TEXT DEFAULT 'CLOUD_FIREBASE',
            $COL_NOTES_OR_REASON TEXT DEFAULT '',
            $COL_EPOCH_MS INTEGER NOT NULL,
            $COL_FORENSIC_TIMESTAMP TEXT NOT NULL,
            FOREIGN KEY ($COL_SHARE_ID) REFERENCES ${DeviceSharingTable.TABLE_NAME}(${DeviceSharingTable.COL_SHARE_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    val CREATE_INDEX_SHARE_TIME = """
        CREATE INDEX IF NOT EXISTS idx_sharing_tx_share_time 
        ON $TABLE_NAME ($COL_SHARE_ID, $COL_EPOCH_MS DESC)
    """.trimIndent()

    // [Variables] db: SQLiteDatabase, txId: String, shareId: String, actorDev: String, recipientDev: String, prevStage: String, newStage: String, attempt: Int, channel: String, reason: String
    // [Constants] TABLE_NAME, COL_*
    // [Dependencies] SQLiteDatabase, ContentValues, FirebaseComm
    // [Integration Points] DatabaseHelper.kt, PairingRepository.kt
    fun record(
        db: SQLiteDatabase,
        shareId: String,
        actorDev: String,
        recipientDev: String,
        prevStage: String,
        newStage: String,
        attempt: Int = 1,
        channel: String = "CLOUD_FIREBASE",
        reason: String = "",
        txId: String = "TX_" + UUID.randomUUID().toString().replace("-", "").take(12)
    ): Long {
        val nowMs = System.currentTimeMillis()
        val forensic = FirebaseComm.getForensicTimestamp()
        val cv = ContentValues().apply {
            put(COL_TRANSACTION_ID, txId)
            put(COL_SHARE_ID, shareId)
            put(COL_ACTOR_DEVICE_ID, actorDev)
            put(COL_RECIPIENT_DEVICE_ID, recipientDev)
            put(COL_PREVIOUS_STAGE, prevStage)
            put(COL_NEW_STAGE, newStage)
            put(COL_ATTEMPT_NUMBER, attempt)
            put(COL_CHANNEL_MODE, channel)
            put(COL_NOTES_OR_REASON, reason)
            put(COL_EPOCH_MS, nowMs)
            put(COL_FORENSIC_TIMESTAMP, forensic)
        }
        return db.insert(TABLE_NAME, null, cv)
    }
}
