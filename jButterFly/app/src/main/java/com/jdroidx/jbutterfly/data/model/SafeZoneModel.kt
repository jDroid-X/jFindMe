package com.jdroidx.jbutterfly.data.model

data class SafeZoneModel(
    @JvmField val zoneId: String = "",
    @JvmField val name: String = "",
    @JvmField val latitude: Double = 0.0,
    @JvmField val longitude: Double = 0.0,
    @JvmField val radiusMeters: Double = 200.0,
    @JvmField val notifyEntry: Boolean = true,
    @JvmField val notifyExit: Boolean = true,
    @JvmField val createdAt: Long = System.currentTimeMillis()
)
