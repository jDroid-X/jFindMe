package com.jdroidx.jfindme.engine.location

import com.google.android.gms.maps.model.LatLng
import com.jdroidx.jfindme.data.model.SafeZoneModel
import org.json.JSONArray
import java.util.Calendar
import kotlin.math.*

/**
 * 🗺️ GisSpatialEngine: High-Precision Spatial Math, Geofencing & Telemetry Throttler
 * Governs Agent 6 & 7 spatial and performance operations.
 */
object GisSpatialEngine {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates great-circle distance between two coordinates in meters (Haversine formula).
     */
    @JvmStatic
    fun computeHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates initial compass bearing in degrees (0° - 360°) from point A to point B.
     */
    @JvmStatic
    fun computeBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val lambdaDiff = Math.toRadians(lon2 - lon1)

        val y = sin(lambdaDiff) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(lambdaDiff)
        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360) % 360
        return bearing.toFloat()
    }

    /**
     * Ray-Casting algorithm to determine if a point is inside a polygon boundary.
     */
    @JvmStatic
    fun isPointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        val n = polygon.size
        var j = n - 1

        for (i in 0 until n) {
            val pi = polygon[i]
            val pj = polygon[j]

            if ((pi.longitude > point.longitude) != (pj.longitude > point.longitude) &&
                (point.latitude < (pj.latitude - pi.latitude) * (point.longitude - pi.longitude) / (pj.longitude - pi.longitude) + pi.latitude)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Parses JSON string into a list of LatLng vertices.
     */
    @JvmStatic
    fun parsePolygonJson(jsonString: String?): List<LatLng> {
        val list = mutableListOf<LatLng>()
        if (jsonString.isNullOrBlank()) return list
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val lat = obj.optDouble("lat", obj.optDouble("latitude", 0.0))
                val lng = obj.optDouble("lng", obj.optDouble("longitude", 0.0))
                if (lat != 0.0 || lng != 0.0) {
                    list.add(LatLng(lat, lng))
                }
            }
        } catch (e: Exception) { }
        return list
    }

    /**
     * Checks if a SafeZone is currently active based on its time-window schedule.
     */
    @JvmStatic
    fun isSafeZoneInSchedule(safeZone: SafeZoneModel, calendar: Calendar = Calendar.getInstance()): Boolean {
        if (!safeZone.isZoneActive) return false

        // Check Active Days (supports 0-indexed Monday: 0=Mon..6=Sun, 0-indexed Sunday: 0=Sun..6=Sat, and 1-indexed Calendar: 1=Sun..7=Sat)
        val daysStr = safeZone.activeDays.trim()
        if (daysStr.isNotEmpty() && daysStr != "ALL" && !daysStr.contains("1,2,3,4,5,6,0") && !daysStr.contains("0,1,2,3,4,5,6")) {
            val calDay = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon... 7=Sat
            val dayIdx0 = (calDay + 5) % 7 // 0=Mon, 1=Tue... 6=Sun (GeofenceActivity standard)
            val dayIdxSunday0 = calDay - 1 // 0=Sun, 1=Mon... 6=Sat
            val activeDaysList = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }
            val isDayActive = activeDaysList.contains(calDay) ||
                              activeDaysList.contains(dayIdx0) ||
                              activeDaysList.contains(dayIdxSunday0)
            if (!isDayActive) {
                return false
            }
        }

        // Check Time Window (startTime / endTime format e.g. 800 for 08:00 AM, 1500 for 03:00 PM)
        val currentHourMinute = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE)
        if (safeZone.startTime != 0 || safeZone.endTime != 2359) {
            if (safeZone.startTime <= safeZone.endTime) {
                // Standard daytime window (e.g. 0800 to 1800)
                if (currentHourMinute < safeZone.startTime || currentHourMinute > safeZone.endTime) {
                    return false
                }
            } else {
                // 🌙 Overnight midnight crossover window (e.g. 2200 to 0600)
                if (currentHourMinute < safeZone.startTime && currentHourMinute > safeZone.endTime) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Determines whether the node is currently inside the SafeZone (circular or polygon).
     */
    @JvmStatic
    fun isLocationInsideSafeZone(lat: Double, lng: Double, safeZone: SafeZoneModel): Boolean {
        if (safeZone.isPolygon) {
            val polygon = parsePolygonJson(safeZone.polygonPointsJson)
            if (polygon.size >= 3) {
                return isPointInPolygon(LatLng(lat, lng), polygon)
            }
        }
        // Fallback to circular radius
        val dist = computeHaversineDistance(lat, lng, safeZone.lat, safeZone.lng)
        return dist <= safeZone.radius
    }

    /**
     * Computes the adaptive telemetry polling interval in milliseconds.
     */
    @JvmStatic
    fun computeAdaptivePollingInterval(
        isSosActive: Boolean,
        speedMps: Float,
        batteryPct: Int,
        isCharging: Boolean,
        isStationary: Boolean
    ): Long {
        if (isSosActive) return 1000L // 1 sec emergency stream
        if (batteryPct <= 15 && !isCharging) return 600000L // 10 min battery saver
        if (isStationary && speedMps < 0.5f) return 300000L // 5 min stationary heartbeat
        if (speedMps > 1.5f) return 5000L // 5 sec active motion tracking
        return 15000L // 15 sec standard tracking
    }
}
