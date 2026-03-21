package com.jdroidx.jfindme.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * DeviceRegistry: Tracks individual device identity and session state.
 * Required for multi-device sync and conflict resolution.
 * Role-Based Access Control (RBAC): ADMIN vs VIEWER.
 */
@IgnoreExtraProperties
data class DeviceRegistry(
    val deviceId: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val platform: String = "android",
    val isPrimary: Boolean = false,
    val lastActive: Long = System.currentTimeMillis(),
    val status: String = "active",
    val deviceToken: String = "",
    val userRole: String = "VIEWER" // Default to limited role
)
