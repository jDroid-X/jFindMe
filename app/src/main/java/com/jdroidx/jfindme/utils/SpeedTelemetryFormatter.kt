package com.jdroidx.jfindme.utils

import java.util.Locale
import kotlin.math.roundToInt

/**
 * 🚀 SSOT for Speed and Motion Telemetry Normalization across jFindMe.
 * Guarantees consistent conversion from Android/GNSS canonical standard (meters/second)
 * to user-facing formats (km/h) and motion states.
 */
object SpeedTelemetryFormatter {

    const val MPS_TO_KMH_FACTOR: Float = 3.6f
    const val THRESHOLD_DRIVING_KMH: Float = 25f
    const val THRESHOLD_RUNNING_KMH: Float = 7f
    const val THRESHOLD_WALKING_KMH: Float = 1.5f

    @JvmStatic
    fun toKmh(speedMps: Float): Float = speedMps * MPS_TO_KMH_FACTOR

    @JvmStatic
    fun formatSpeed(speedMps: Float): String {
        val speedKmh = toKmh(speedMps)
        return String.format(Locale.US, "%.1f km/h", speedKmh)
    }

    @JvmStatic
    fun getMotionBadge(speedMps: Float): String {
        val speedKmh = toKmh(speedMps)
        return when {
            speedKmh > THRESHOLD_DRIVING_KMH -> "🚗 ${speedKmh.roundToInt()} km/h"
            speedKmh > THRESHOLD_RUNNING_KMH -> "🏃 ${speedKmh.roundToInt()} km/h"
            speedKmh > THRESHOLD_WALKING_KMH -> "🚶 Walking"
            else -> "⏱️ Resting"
        }
    }

    @JvmStatic
    fun getRadarMotionTag(speedMps: Float): String {
        val speedKmh = toKmh(speedMps)
        return when {
            speedKmh > THRESHOLD_DRIVING_KMH -> " • 🚗 ${speedKmh.roundToInt()} km/h"
            speedKmh > THRESHOLD_RUNNING_KMH -> " • 🏃 ${speedKmh.roundToInt()} km/h"
            speedKmh > THRESHOLD_WALKING_KMH -> " • 🚶 Walk"
            else -> ""
        }
    }

    @JvmStatic
    fun getMarqueeSpeedTag(speedMps: Float): String {
        val speedKmh = toKmh(speedMps)
        return if (speedKmh > THRESHOLD_WALKING_KMH) {
            String.format(Locale.US, " • 🚗 %.1f km/h", speedKmh)
        } else {
            " • 🛑 Static (0.0 km/h)"
        }
    }
}
