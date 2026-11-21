package com.jdroidx.jfindme.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import java.util.concurrent.ConcurrentHashMap

/**
 * [Variables] pointId: String, creatorDeviceId: String, creatorName: String, pointName: String, address: String, lat: Double, lng: Double, geohash: String, rendezvousTimestamp: Long, expiryTimestamp: Long, arrivalRadiusMeters: Double, notes: String, iconType: String, isActive: Boolean, isArchived: Boolean, createdAt: String, updatedAt: String, devices: MutableMap<String, ButterflyDeviceEntry>
 * [Constants] DEFAULT_RADIUS_METERS = 50.0
 * [Dependencies] com.google.firebase.database.IgnoreExtraProperties, com.google.firebase.database.PropertyName, java.util.concurrent.ConcurrentHashMap
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
    var devices: MutableMap<String, ButterflyDeviceEntry> = ConcurrentHashMap()
) {
    /**
     * [Variables] None | [Constants] None | [Dependencies] ConcurrentHashMap, HashMap | [Integration Points] MapActivity.java, ButterflyPointDialog.java, ButterflyPointEngine.kt
     * Directive 1 & GAP-03: Thread-safe CME protection snapshot getter wrapping synchronized(devices) { return HashMap(devices) }
     */
    fun getDevicesSnapshot(): Map<String, ButterflyDeviceEntry> {
        synchronized(devices) {
            return HashMap(devices)
        }
    }

    fun ensureConcurrentDevices() {
        if (devices !is ConcurrentHashMap) {
            synchronized(devices) {
                devices = ConcurrentHashMap(devices)
            }
        }
    }
}

/**
 * [Variables] deviceId, deviceName, status, rejectionReason, distanceMeters, speedKmh, etaMinutes, lastLat, lastLng, lastUpdated, arrivalTimestamp, batteryPct, isSosActive, ragStatus, ragReason, invitedBy
 * [Constants] STATUS_PENDING, STATUS_ACCEPTED, STATUS_REJECTED, STATUS_ON_MOVE, STATUS_REACHED
 * [Dependencies] com.google.firebase.database.IgnoreExtraProperties, com.google.firebase.database.PropertyName
 * [Integration Points] ButterflyPointDeviceMapTable.kt, ButterflyPointEngine.kt
 */
@IgnoreExtraProperties
data class ButterflyDeviceEntry(
    var deviceId: String = "",
    var deviceName: String = "",
    var status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, ON_MOVE, REACHED
    var rejectionReason: String = "",

    @get:com.google.firebase.database.PropertyName("distance_meters")
    @set:com.google.firebase.database.PropertyName("distance_meters")
    var distanceMeters: Double = 0.0,

    var speedKmh: Float = 0.0f,
    var etaMinutes: Int = 0,
    var lastLat: Double = 0.0,
    var lastLng: Double = 0.0,
    var lastUpdated: Long = 0L,
    var arrivalTimestamp: Long = 0L,
    var batteryPct: Int = -1,

    @get:com.google.firebase.database.PropertyName("is_sos")
    @set:com.google.firebase.database.PropertyName("is_sos")
    var isSosActive: Boolean = false,

    var ragStatus: String = "GREY", // GREEN, AMBER, RED, GREY
    var ragReason: String = "",
    var invitedBy: String = ""
)
