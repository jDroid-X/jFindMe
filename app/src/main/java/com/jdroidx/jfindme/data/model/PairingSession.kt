package com.jdroidx.jfindme.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * PairingSession: State machine for secure device linking.
 * States: PENDING -> VALIDATED -> LINKED -> ACTIVE -> SYNCED
 * 🛡️ Multi-Device Architecture: Supports up to 5 simultaneous devices using 1 pairing code.
 */
@IgnoreExtraProperties
data class PairingSession(
    val sessionId: String = "",
    val userId: String = "",
    val pairCode: String = "",
    val expiresAt: Long = 0,
    val status: String = "pending",
    val primaryDevice: String = "",
    val secondaryDevice: String? = null,
    val attached_devices: Map<String, Any>? = null,
    val max_attachments: Int = 5,
    val role: String = "MEMBER",
    val timestamp: Long = System.currentTimeMillis()
)
