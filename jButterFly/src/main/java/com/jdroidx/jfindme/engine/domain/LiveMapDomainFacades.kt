package com.jdroidx.jfindme.engine.domain

import com.jdroidx.jfindme.data.model.LiveDeviceModel
import kotlin.math.*

/**
 * 🛰️ LiveMapDomainFacades: OOPS Domain Handlers for Screen 06 (LiveMapDashboardActivity).
 * [Variables]: devices, centerLat, centerLng, radiusMeters
 * [Dependencies]: LiveDeviceModel
 * [Integration Points]: LiveMapDashboardActivity.java, MapActivity.java
 */
object FleetMetricsAggregator {
    fun aggregate(devices: List<LiveDeviceModel>): Map<String, Any> {
        val total = devices.size
        val active = devices.count { System.currentTimeMillis() - it.timestamp < 300000L }
        val sos = devices.count { it.isSosActive }
        return mapOf("total" to total, "active" to active, "sos" to sos)
    }
}

object PolarCoordinateTransformer {
    fun toPolar(lat: Double, lng: Double, centerLat: Double, centerLng: Double): Pair<Double, Double> {
        val dLat = Math.toRadians(lat - centerLat)
        val dLng = Math.toRadians(lng - centerLng)
        val distance = sqrt(dLat * dLat + dLng * dLng) * 6371000.0
        val bearing = (Math.toDegrees(atan2(dLng, dLat)) + 360.0) % 360.0
        return Pair(distance, bearing)
    }
}

object AzimuthBearingCalculator {
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }
}

object HaversineSpatialFilter {
    fun filterWithinRadius(devices: List<LiveDeviceModel>, centerLat: Double, centerLng: Double, radiusMeters: Double): List<LiveDeviceModel> {
        return devices.filter { d ->
            val dist = PolarCoordinateTransformer.toPolar(d.latitude, d.longitude, centerLat, centerLng).first
            dist <= radiusMeters
        }
    }
}

object FleetCollectionSorter {
    fun sortByName(devices: List<LiveDeviceModel>): List<LiveDeviceModel> = devices.sortedBy { it.userName }
    fun sortByDistance(devices: List<LiveDeviceModel>, centerLat: Double, centerLng: Double): List<LiveDeviceModel> {
        return devices.sortedBy { d ->
            PolarCoordinateTransformer.toPolar(d.latitude, d.longitude, centerLat, centerLng).first
        }
    }
}

