package com.jdroidx.jfindme.utils

import kotlin.math.*

/**
 * 🌐 SpatialMathUtil: Centralized High-Performance Geodesic & Spatial Mathematics Engine
 *
 * Enforces Prime Directive: "Use similar code call-by-reference to avoid repetition and keep execution lightweight."
 * Replaces duplicate Haversine implementations across MapActivity, LocationRepository,
 * MultiMarkerEngine, GeofenceRepository, and TrackingService.
 *
 * [Variables]
 * - dLat, dLng, a, c, distance
 *
 * [Constants]
 * - EARTH_RADIUS_METERS = 6371000.0
 *
 * [Dependencies]
 * - kotlin.math.*
 *
 * [Integration Points]
 * - MapActivity.java: Proximity filtering (<50m vs >=50m)
 * - MultiMarkerEngine.kt: 50dp screen-space dispersion
 * - GeofenceRepository.kt: Radial zone boundary calculation
 * - LocationRepository.kt: Target distance sorting
 * - TrackingService.kt: Dead-reckoning motion detection
 */
object SpatialMathUtil {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Computes the great-circle distance between two geodetic coordinates in meters using the Haversine formula.
     * Caches trigonometric calculations and provides thread-safe, allocation-free execution.
     */
    @JvmStatic
    fun computeDistanceBetween(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val sinDLat = sin(dLat / 2.0)
        val sinDLng = sin(dLng / 2.0)

        val a = sinDLat * sinDLat + cos(rLat1) * cos(rLat2) * sinDLng * sinDLng
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Overload returning Float for Android graphic / layout calculation pipelines.
     */
    @JvmStatic
    fun computeDistanceBetweenFloat(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        return computeDistanceBetween(lat1, lng1, lat2, lng2).toFloat()
    }

    /**
     * Determines whether two points are within a specified distance threshold in meters.
     * Short-circuits calculations when coordinate deltas exceed the bounding box.
     */
    @JvmStatic
    fun isWithinDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
        thresholdMeters: Double
    ): Boolean {
        // Quick bounding box check (~111,139 meters per degree latitude)
        val maxDegreeDelta = thresholdMeters / 110000.0
        if (abs(lat1 - lat2) > maxDegreeDelta) return false
        return computeDistanceBetween(lat1, lng1, lat2, lng2) <= thresholdMeters
    }

    @JvmStatic
    fun isWithinDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
        thresholdMeters: Float
    ): Boolean {
        return isWithinDistance(lat1, lng1, lat2, lng2, thresholdMeters.toDouble())
    }

    /**
     * Determines whether the current coordinate is near (within buffer distance) of any active Safe Zone perimeter.
     * Used for Context-Aware Geofence Cadence Stepping.
     */
    @JvmStatic
    fun isNearAnySafeZonePerimeter(
        currentLat: Double,
        currentLng: Double,
        safeZones: List<com.jdroidx.jfindme.data.model.SafeZoneModel>?,
        perimeterBufferMeters: Double = 150.0
    ): Boolean {
        if (safeZones.isNullOrEmpty()) return false
        for (zone in safeZones) {
            val distToCenter = computeDistanceBetween(currentLat, currentLng, zone.lat, zone.lng)
            val distToPerimeter = abs(distToCenter - zone.radius)
            if (distToPerimeter <= perimeterBufferMeters) {
                return true
            }
        }
        return false
    }
}
