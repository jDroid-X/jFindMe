package com.jdroidx.jbutterfly.data.model

data class UserLocation(
    @JvmField val agentId: String = "",
    @JvmField val hardwareId: String = "",
    @JvmField val latitude: Double = 0.0,
    @JvmField val longitude: Double = 0.0,
    @JvmField val accuracy: Float = 0.0f,
    @JvmField val speed: Float = 0.0f,
    @JvmField val heading: Float = 0.0f,
    @JvmField val altitude: Double = 0.0,
    @JvmField val batteryPct: Int = 100,
    @JvmField val timestamp: Long = System.currentTimeMillis()
)
