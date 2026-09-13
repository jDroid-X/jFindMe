package com.jdroidx.jbutterfly.utils

import com.jdroidx.jbutterfly.data.model.SafeZoneModel

object GeofenceEngine {

    /**
     * Calculates distance in meters between two lat/lng coordinates using the Haversine formula.
     */
    @JvmStatic
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    /**
     * Alias for calculateDistanceMeters using Haversine calculation.
     */
    @JvmStatic
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return calculateDistanceMeters(lat1, lon1, lat2, lon2)
    }

    /**
     * Returns true if point (lat, lon) is within the SafeZone's perimeter.
     */
    @JvmStatic
    fun isInsideZone(lat: Double, lon: Double, zone: SafeZoneModel): Boolean {
        val dist = calculateDistanceMeters(lat, lon, zone.latitude, zone.longitude)
        return dist <= zone.radiusMeters
    }
}
