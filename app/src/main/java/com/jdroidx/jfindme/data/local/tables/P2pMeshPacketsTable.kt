package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, packetId, identityId, senderId, recipientId, payloadType, encryptedPayload, hopCount, maxHops, rssi, transportMedium, status, timestamp, createdOn
 * Constants: TABLE_NAME = "p2p_mesh_packets", COL_ID, COL_PACKET_ID, COL_IDENTITY_ID, COL_SENDER_ID, COL_RECIPIENT_ID, COL_PAYLOAD_TYPE, COL_ENCRYPTED_PAYLOAD, COL_HOP_COUNT, COL_MAX_HOPS, COL_RSSI, COL_TRANSPORT_MEDIUM, COL_STATUS, COL_TIMESTAMP, COL_CREATED_ON
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, NetworkGatewayActivity.java, TelemetryFailoverRouter.kt
 */
object P2pMeshPacketsTable {
    const val TABLE_NAME = "p2p_mesh_packets"

    const val COL_ID = "id"
    const val COL_PACKET_ID = "packet_id"
    const val COL_IDENTITY_ID = "identity_id" // FK to UserIdentityTable.id
    const val COL_SENDER_ID = "sender_id"
    const val COL_RECIPIENT_ID = "recipient_id"
    const val COL_PAYLOAD_TYPE = "payload_type" // LOCATION_HOP, SOS_BEACON, TEXT_MEMO, ACK
    const val COL_ENCRYPTED_PAYLOAD = "encrypted_payload"
    const val COL_HOP_COUNT = "hop_count"
    const val COL_MAX_HOPS = "max_hops"
    const val COL_RSSI = "rssi"
    const val COL_TRANSPORT_MEDIUM = "transport_medium" // BLE, WIFI_DIRECT, AUDIO_FSK
    const val COL_STATUS = "status" // RECEIVED, FORWARDED, DELIVERED, DROPPED
    const val COL_TIMESTAMP = "timestamp"
    const val COL_CREATED_ON = "created_on"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_PACKET_ID TEXT UNIQUE NOT NULL,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_SENDER_ID TEXT NOT NULL,
            $COL_RECIPIENT_ID TEXT NOT NULL,
            $COL_PAYLOAD_TYPE TEXT NOT NULL,
            $COL_ENCRYPTED_PAYLOAD TEXT NOT NULL,
            $COL_HOP_COUNT INTEGER DEFAULT 0,
            $COL_MAX_HOPS INTEGER DEFAULT 5,
            $COL_RSSI INTEGER DEFAULT 0,
            $COL_TRANSPORT_MEDIUM TEXT DEFAULT 'BLE',
            $COL_STATUS TEXT DEFAULT 'RECEIVED',
            $COL_TIMESTAMP INTEGER NOT NULL,
            $COL_CREATED_ON TEXT,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    fun recordPacket(
        db: SQLiteDatabase,
        packetId: String,
        identityId: Long,
        senderId: String,
        recipientId: String,
        payloadType: String,
        encryptedPayload: String,
        hopCount: Int = 0,
        maxHops: Int = 5,
        rssi: Int = 0,
        transport: String = "BLE",
        status: String = "RECEIVED",
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()
        val cv = ContentValues().apply {
            put(COL_PACKET_ID, packetId)
            put(COL_IDENTITY_ID, identityId)
            put(COL_SENDER_ID, senderId)
            put(COL_RECIPIENT_ID, recipientId)
            put(COL_PAYLOAD_TYPE, payloadType)
            put(COL_ENCRYPTED_PAYLOAD, encryptedPayload)
            put(COL_HOP_COUNT, hopCount)
            put(COL_MAX_HOPS, maxHops)
            put(COL_RSSI, rssi)
            put(COL_TRANSPORT_MEDIUM, transport)
            put(COL_STATUS, status)
            put(COL_TIMESTAMP, timestamp)
            put(COL_CREATED_ON, now)
        }
        return db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }
}

