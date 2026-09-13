package com.jdroidx.jbutterfly.data.model

data class PairingSession(
    val pairingCode: String = "",
    val hostAgentId: String = "",
    val hostHardwareId: String = "",
    val hostDeviceName: String = "",
    val category: String = "FAMILY", // FAMILY or FRIENDS
    val role: String = "FRIEND",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val guestAgentId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
