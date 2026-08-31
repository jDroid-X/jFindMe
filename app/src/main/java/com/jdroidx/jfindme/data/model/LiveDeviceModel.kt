package com.jdroidx.jfindme.data.model

data class LiveDeviceModel(
    val userId: String = "",
    val deviceId: String = "",
    val userName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float = 0f,
    val batteryLevel: Float = 0f,
    val timestamp: Long = 0L,
    val isSosActive: Boolean = false,
    val address: String? = null,
    val avatarId: String = "default"
)
