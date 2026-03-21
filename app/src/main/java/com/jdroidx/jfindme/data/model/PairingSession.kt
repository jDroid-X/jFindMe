package com.jdroidx.jfindme.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * PairingSession: State machine for secure device linking.
 * States: PENDING -> VALIDATED -> LINKED -> SYNCED
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
    val timestamp: Long = System.currentTimeMillis()
)
