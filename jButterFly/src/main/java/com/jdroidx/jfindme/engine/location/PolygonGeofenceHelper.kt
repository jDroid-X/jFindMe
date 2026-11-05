package com.jdroidx.jfindme.engine.location

import org.json.JSONArray
import org.json.JSONObject

/**
 * 📐 PolygonGeofenceHelper: Multi-Polygon Safe-Zone Architect & Ray-Casting Sentinel.
 * Determines deterministic Point-in-Polygon (PIP) containment using Jordan Curve Theorem ray-casting.
 *
 * [Variables]: pointLat, pointLng, vertices, i, j, intersect
 * [Constants]: MIN_POLYGON_VERTICES = 3
 * [Dependencies]: org.json.JSONArray, org.json.JSONObject
 * [Integration Points]: GeofenceActivity.java, GeofenceHelper.kt, SafeZoneModel.kt, EnterpriseUnitTestSuite.kt
 */
object PolygonGeofenceHelper {

    const val MIN_POLYGON_VERTICES = 3

    data class LatLngPoint(val latitude: Double, val longitude: Double)

    /**
     * Parse polygon points from JSON format: `[{"lat": 18.52, "lng": 73.85}, ...]`
     */
    fun parseVertices(polygonJson: String?): List<LatLngPoint> {
        if (polygonJson.isNullOrBlank()) return emptyList()
        val list = mutableListOf<LatLngPoint>()
        try {
            val array = JSONArray(polygonJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val lat = obj.optDouble("lat", obj.optDouble("latitude", 0.0))
                val lng = obj.optDouble("lng", obj.optDouble("longitude", 0.0))
                list.add(LatLngPoint(lat, lng))
            }
        } catch (e: Exception) {
            // fallback for malformed JSON
        }
        return list
    }

    /**
     * Ray-Casting Algorithm: Determines if (lat, lng) is inside the polygon.
     */
    fun isPointInPolygon(pointLat: Double, pointLng: Double, vertices: List<LatLngPoint>): Boolean {
        if (vertices.size < MIN_POLYGON_VERTICES) return false

        var inside = false
        var j = vertices.size - 1

        for (i in vertices.indices) {
            val xi = vertices[i].longitude
            val yi = vertices[i].latitude
            val xj = vertices[j].longitude
            val yj = vertices[j].latitude

            val intersect = ((yi > pointLat) != (yj > pointLat)) &&
                    (pointLng < (xj - xi) * (pointLat - yi) / (yj - yi) + xi)

            if (intersect) {
                inside = !inside
            }
            j = i
        }

        return inside
    }

    /**
     * Serialize vertices to compact JSON.
     */
    fun serializeVertices(points: List<LatLngPoint>): String {
        val array = JSONArray()
        for (p in points) {
            val obj = JSONObject()
            obj.put("lat", p.latitude)
            obj.put("lng", p.longitude)
            array.put(obj)
        }
        return array.toString()
    }
}

