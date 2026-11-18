package com.jdroidx.jfindme.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * [Variables] pointId, creatorDeviceId, creatorName, pointName, address, lat, lng, geohash, rendezvousTimestamp, expiryTimestamp, arrivalRadiusMeters, notes, iconType, isActive, isArchived, createdAt, updatedAt, devices
 * [Constants] DEFAULT_RADIUS_METERS = 50.0
 * [Dependencies] com.google.firebase.database.IgnoreExtraProperties
 * [Integration Points] DatabaseHelper.kt, ButterflyPointEngine.kt, MapActivity.java, Firebase RTDB /butterfly_points/{pointId}
 */
@IgnoreExtraProperties
data class ButterflyPoint(
    var pointId: String = "",
    var creatorDeviceId: String = "",
    var creatorName: String = "",
    var pointName: String = "",
    var address: String = "",
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var geohash: String = "",
    var rendezvousTimestamp: Long = 0L,
    var expiryTimestamp: Long = 0L,
    var arrivalRadiusMeters: Double = 50.0,
    var notes: String = "",
    var iconType: String = "BUTTERFLY_STANDARD",
    var isActive: Boolean = true,
    var isArchived: Boolean = false,
    var createdAt: String = "",
    var updatedAt: String = "",
    var devices: MutableMap<String, ButterflyDeviceEntry> = mutableMapOf()
)

/**
 * [Variables] deviceId, deviceName, status, rejectionReason, distanceMeters, speedKmh, etaMinutes, lastLat, lastLng, lastUpdated, arrivalTimestamp
 * [Constants] STATUS_PENDING, STATUS_ACCEPTED, STATUS_REJECTED, STATUS_ON_MOVE, STATUS_REACHED
 * [Dependencies] com.google.firebase.database.IgnoreExtraProperties
 * [Integration Points] ButterflyPointDeviceMapTable.kt, ButterflyPointEngine.kt
 */
@IgnoreExtraProperties
data class ButterflyDeviceEntry(
    var deviceId: String = "",
    var deviceName: String = "",
    var status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, ON_MOVE, REACHED
    var rejectionReason: String = "",
    var distanceMeters: Double = 0.0,
    var speedKmh: Float = 0.0f,
    var etaMinutes: Int = 0,
    var lastLat: Double = 0.0,
    var lastLng: Double = 0.0,
    var lastUpdated: Long = 0L,
    var arrivalTimestamp: Long = 0L,
    var batteryPct: Int = -1,
    var isSosActive: Boolean = false,
    var ragStatus: String = "GREY", // GREEN, AMBER, RED, GREY
    var ragReason: String = "",
    var invitedBy: String = ""
)
