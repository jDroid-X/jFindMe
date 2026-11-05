package com.jdroidx.jfindme.engine.location

/**
 * SafeZoneConstants: Single Source of Truth (SSOT) constants for Geofence and SafeZone operations.
 * OOPS / MVVM Architecture Token Module.
 */
object SafeZoneConstants {
    // Intent Extras
    const val EXTRA_ESTABLISH_DOMAIN_ADDRESS = "ESTABLISH_DOMAIN_ADDRESS"
    const val EXTRA_ESTABLISH_DOMAIN_NAME = "ESTABLISH_DOMAIN_NAME"
    const val EXTRA_TARGET_UID = "TARGET_UID"
    const val EXTRA_TARGET_ZONE_ID = "TARGET_ZONE_ID"
    const val EXTRA_TARGET_ZONE_NAME = "TARGET_ZONE_NAME"
    const val EXTRA_LATITUDE = "LATITUDE"
    const val EXTRA_LONGITUDE = "LONGITUDE"
    const val EXTRA_RADIUS = "RADIUS"

    // Default Configuration
    const val DEFAULT_RADIUS_METERS = 100.0
    const val MIN_RADIUS_METERS = 25.0
    const val MAX_RADIUS_METERS = 5000.0
    const val DEFAULT_START_TIME = 0      // 00:00
    const val DEFAULT_END_TIME = 2359     // 23:59
    const val ALL_DAYS_STRING = "0,1,2,3,4,5,6" // Mon(0) - Sun(6)
    const val WEEKDAYS_STRING = "0,1,2,3,4"
    const val WEEKENDS_STRING = "5,6"
    const val ASSIGN_ALL_DEVICES = "ALL CONNECTED DEVICES"

    // Transition Actions
    const val TRANSITION_ENTER = "ENTERED"
    const val TRANSITION_EXIT = "EXITED"
    const val TRANSITION_IN = "IN"
    const val TRANSITION_OUT = "OUT"
    const val TRANSITION_DEPLOYED = "DEPLOYED"

    // Cloud Sync Keys
    const val TYPE_GEOFENCE_ASSIGNMENT = "GEOFENCE_ASSIGNMENT"
    const val STATUS_PENDING_ACCEPTANCE = "PENDING_ACCEPTANCE"
    const val STATUS_SYNC_CONFIRMED = "SYNC_CONFIRMED"
    const val STATUS_REJECTED = "REJECTED"
    const val STATUS_AUTO_ACCEPTED = "AUTO_ACCEPTED"

    // Request Codes
    const val RC_GEOFENCE_PERMISSIONS = 200
    const val RC_LOCATION_PERMISSIONS = 101

    // Quick Radius Presets
    val RADIUS_PRESETS = intArrayOf(50, 100, 250, 500, 1000)
}
