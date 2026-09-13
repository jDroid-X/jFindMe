package com.jdroidx.jbutterfly.data.model

data class AppSettingsModel(
    val pollIntervalMovingMs: Long = 10000L,
    val pollIntervalStationaryMs: Long = 60000L,
    val fastestIntervalMs: Long = 5000L,
    val accuracyThresholdMeters: Float = 25.0f,
    val defaultGeofenceRadiusMeters: Float = 200.0f,
    val batteryAlertThresholdPct: Int = 20,
    val buzzerDurationSeconds: Int = 30,
    val mapDefaultZoom: Float = 16.0f,
    val followMeEnabled: Boolean = true,
    val themeMode: String = "SYSTEM" // SYSTEM, LIGHT, DARK
)
