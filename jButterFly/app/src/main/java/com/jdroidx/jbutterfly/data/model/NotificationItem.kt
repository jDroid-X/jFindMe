package com.jdroidx.jbutterfly.data.model

/**
 * NotificationItem — Entity representing an incident or system alert.
 * Categories: GEOFENCE, SOS, BATTERY, PAIRING, SYSTEM
 */
data class NotificationItem(
    @JvmField val id: Long = 0,
    @JvmField val title: String = "",
    @JvmField val message: String = "",
    @JvmField val category: String = "SYSTEM",
    @JvmField val timestamp: Long = System.currentTimeMillis(),
    @JvmField val isRead: Boolean = false
)
