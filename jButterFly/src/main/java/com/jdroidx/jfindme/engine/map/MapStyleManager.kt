package com.jdroidx.jfindme.engine.map

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import com.jdroidx.jfindme.R

/**
 * 🗺️ MapStyleManager: Enterprise Map Style Selector & Vector Styler.
 * Dynamically toggles Dark, Night, Satellite, Hybrid and Standard styles.
 *
 * [Variables]: googleMap, context, styleType
 * [Constants]: STYLE_DARK, STYLE_NIGHT, STYLE_STANDARD, STYLE_SATELLITE, STYLE_HYBRID
 * [Dependencies]: com.google.android.gms.maps.GoogleMap, com.google.android.gms.maps.model.MapStyleOptions
 * [Integration Points]: MapActivity.java, LiveMapDashboardActivity.java, EnterpriseUnitTestSuite.kt
 */
object MapStyleManager {

    const val STYLE_STANDARD = "STANDARD"
    const val STYLE_DARK = "DARK"
    const val STYLE_NIGHT = "NIGHT"
    const val STYLE_SATELLITE = "SATELLITE"
    const val STYLE_HYBRID = "HYBRID"

    fun applyStyle(context: Context, map: GoogleMap, styleType: String) {
        when (styleType.uppercase()) {
            STYLE_DARK -> {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                val rawId = context.resources.getIdentifier("map_style_dark", "raw", context.packageName)
                if (rawId != 0) {
                    map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, rawId))
                }
            }
            STYLE_NIGHT -> {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                val rawId = context.resources.getIdentifier("map_style_night", "raw", context.packageName)
                if (rawId != 0) {
                    map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, rawId))
                }
            }
            STYLE_SATELLITE -> {
                map.setMapStyle(null)
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
            STYLE_HYBRID -> {
                map.setMapStyle(null)
                map.mapType = GoogleMap.MAP_TYPE_HYBRID
            }
            else -> {
                map.setMapStyle(null)
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
        }
    }
}

