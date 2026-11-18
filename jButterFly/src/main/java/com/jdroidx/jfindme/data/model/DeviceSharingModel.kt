package com.jdroidx.jfindme.data.model

/**
 * [Variables] shareId: String, masterDeviceId: String, submasterDeviceId: String, targetDeviceId: String, relationshipType: String, permissionsMask: Int, currentStatus: String, isActive: Boolean, retryCount: Int, validUntil: Long, createdOn: String, updatedAt: String
 * [Constants] None
 * [Dependencies] None
 * [Integration Points] DatabaseHelper.kt, PairingRepository.kt, MapActivity.java, PersonDetailsViewModel.kt
 *
 * 🛡️ Data Model for Triadic Device Sharing (Master ➔ Submaster ➔ Target)
 */
data class DeviceSharingRecord(
    val shareId: String,
    val masterDeviceId: String,
    val submasterDeviceId: String,
    val targetDeviceId: String,
    val relationshipType: String = "CO_GUARDIAN",
    val permissionsMask: Int = 0xFF,
    val currentStatus: String = "REQUESTED",
    val isActive: Boolean = true,
    val retryCount: Int = 0,
    val validUntil: Long = 0L,
    val createdOn: String = "",
    val updatedAt: String = ""
) {
    fun isExpired(): Boolean {
        return validUntil > 0L && System.currentTimeMillis() > validUntil
    }

    fun hasPermission(permissionBit: Int): Boolean {
        return (permissionsMask and permissionBit) == permissionBit
    }
}

/**
 * [Variables] transactionId: String, shareId: String, actorDeviceId: String, recipientDeviceId: String, previousStage: String, newStage: String, attemptNumber: Int, channelMode: String, notesOrReason: String, epochMs: Long, forensicTimestamp: String
 * [Constants] None
 * [Dependencies] None
 * [Integration Points] DatabaseHelper.kt, PairingRepository.kt
 *
 * 🛡️ Data Model for Device Sharing Lifecycle Transaction
 */
data class DeviceSharingTransactionRecord(
    val transactionId: String,
    val shareId: String,
    val actorDeviceId: String,
    val recipientDeviceId: String,
    val previousStage: String,
    val newStage: String,
    val attemptNumber: Int = 1,
    val channelMode: String = "CLOUD_FIREBASE",
    val notesOrReason: String = "",
    val epochMs: Long = System.currentTimeMillis(),
    val forensicTimestamp: String = ""
)
