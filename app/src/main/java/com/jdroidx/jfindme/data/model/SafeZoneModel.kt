package com.jdroidx.jfindme.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class SafeZoneModel(
    @get:PropertyName("zone_id") @set:PropertyName("zone_id")
    var zoneId: String = "",

    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("lat") @set:PropertyName("lat")
    var lat: Double = 0.0,

    @get:PropertyName("lng") @set:PropertyName("lng")
    var lng: Double = 0.0,

    @get:PropertyName("radius") @set:PropertyName("radius")
    var radius: Float = 100f,

    @get:PropertyName("assignedName") @set:PropertyName("assignedName")
    var assignedName: String = "ALL CONNECTED DEVICES",

    @get:PropertyName("address") @set:PropertyName("address")
    var address: String = "",

    @get:PropertyName("alertsEnabled") @set:PropertyName("alertsEnabled")
    var alertsEnabled: Boolean = true,

    @get:PropertyName("isZoneActive") @set:PropertyName("isZoneActive")
    var isZoneActive: Boolean = true,

    @get:PropertyName("startTime") @set:PropertyName("startTime")
    var startTime: Int = 0,

    @get:PropertyName("endTime") @set:PropertyName("endTime")
    var endTime: Int = 2359,

    @get:PropertyName("activeDays") @set:PropertyName("activeDays")
    var activeDays: String = "1,2,3,4,5,6,0",

    @get:PropertyName("assignedUserIds") @set:PropertyName("assignedUserIds")
    var assignedUserIds: List<String> = emptyList()
) {
    // Backward compatibility aliases
    val latitude: Double get() = lat
    val longitude: Double get() = lng
    val radiusMeters: Float get() = radius

    // Secondary constructor for legacy callers
    constructor(
        zoneId: String,
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        assignedUserIds: List<String> = emptyList()
    ) : this(
        zoneId = zoneId,
        name = name,
        lat = latitude,
        lng = longitude,
        radius = radiusMeters,
        assignedUserIds = assignedUserIds
    )
}
