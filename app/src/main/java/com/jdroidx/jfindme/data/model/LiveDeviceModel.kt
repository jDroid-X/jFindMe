package com.jdroidx.jfindme.data.model

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: userId, deviceId, userName, nickname, latitude, longitude, speed, bearing, accuracy, altitude, motionState, batteryLevel, timestamp, isSosActive, address, avatarId
 * Constants: Default values
 * Dependencies: None (Pure Data POJO)
 * Integration Points: MapV2ViewModel.kt, LiveMapDashboardActivity.java, PersonDetailsViewModel.kt, LiveMapDomainFacades.kt
 */
data class LiveDeviceModel(
    val userId: String = "",
    val deviceId: String = "",
    val userName: String = "",
    val nickname: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val motionState: String = "STATIONARY",
    val batteryLevel: Float = 0f,
    val timestamp: Long = 0L,
    val isSosActive: Boolean = false,
    val address: String? = null,
    val avatarId: String = "default"
)
