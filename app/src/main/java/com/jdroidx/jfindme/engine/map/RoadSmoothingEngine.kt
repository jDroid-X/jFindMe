package com.jdroidx.jfindme.engine.map

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger
import kotlin.math.*

/**
 * 🛣️ RoadSmoothingEngine: Real-time Road Alignment, Kalman Filter & Heading Rotation Sentinel.
 *
 * Implements Google Maps, Zenly, and Life360 movement benchmarks:
 * 1. 2D Coordinate Kalman Filtering (Reduces GPS multi-path jitter & signal jumps).
 * 2. Spherical Road Heading / Azimuth Calculation (Smooth marker rotation along road curves).
 * 3. Heading EMA Low-Pass Filter (Prevents erratic marker spinning at intersections/stops).
 * 4. Micro-Step Geodesic Interpolation (60 FPS smooth marker road transitions).
 * 5. Dynamic Parameter Loading via RegistryManager (User-configurable via Settings).
 */
object RoadSmoothingEngine {

    private const val TAG = "RoadSmoothingEngine"

    data class SmoothedResult(
        val smoothedLatLng: LatLng,
        val filteredHeading: Float,
        val smoothedSpeedMps: Float,
        val isMoving: Boolean
    )

    private class Kalman1D {
        private var x = 0.0 // Value
        private var p = 100.0 // Estimation error covariance
        private var k = 0.0 // Kalman gain
        private var isInitialized = false

        fun filter(measurement: Double, accuracy: Double, timeStepSec: Double, qParam: Double, rParam: Double): Double {
            if (!isInitialized) {
                x = measurement
                p = accuracy * accuracy
                isInitialized = true
                return x
            }

            // Prediction update
            p += qParam * timeStepSec

            // Measurement update
            val effectiveR = max(rParam, accuracy * accuracy)
            k = p / (p + effectiveR)
            x += k * (measurement - x)
            p *= (1.0 - k)

            return x
        }

        fun reset() {
            isInitialized = false
            p = 100.0
        }
    }

    private data class DeviceMovementState(
        val deviceId: String,
        var lastSmoothedLatLng: LatLng? = null,
        var smoothedHeading: Float = 0f,
        var smoothedSpeedMps: Float = 0f,
        var lastTimestamp: Long = 0L,
        val latFilter: Kalman1D = Kalman1D(),
        val lngFilter: Kalman1D = Kalman1D()
    )

    private val deviceStateMap = mutableMapOf<String, DeviceMovementState>()

    /**
     * Filters raw GPS readings through the Kalman filter and computes road-aligned heading.
     */
    @Synchronized
    @JvmOverloads
    fun processLocation(
        deviceId: String,
        rawLat: Double,
        rawLng: Double,
        accuracyMeters: Float = 10f,
        rawSpeedMps: Float = 0f,
        rawBearing: Float = 0f,
        timestamp: Long = System.currentTimeMillis(),
        context: Context? = null
    ): SmoothedResult {
        val isSmoothingEnabled = if (context != null) RegistryManager.isRoadSmoothingEnabled(context) else true
        if (!isSmoothingEnabled) {
            val rawLatLng = LatLng(rawLat, rawLng)
            return SmoothedResult(
                smoothedLatLng = rawLatLng,
                filteredHeading = rawBearing,
                smoothedSpeedMps = rawSpeedMps,
                isMoving = rawSpeedMps > 0.8f
            )
        }

        var state = deviceStateMap[deviceId]
        if (state == null) {
            state = DeviceMovementState(deviceId = deviceId)
            deviceStateMap[deviceId] = state
        }

        val timeStepSec = if (state.lastTimestamp > 0L) {
            max(0.5, (timestamp - state.lastTimestamp) / 1000.0)
        } else {
            1.0
        }
        state.lastTimestamp = timestamp

        // Dynamic parameters from RegistryManager
        val qNoise = if (context != null) RegistryManager.getKalmanProcessNoise(context).toDouble() else RegistryManager.DEFAULT_KALMAN_PROCESS_NOISE.toDouble()
        val rNoise = if (context != null) RegistryManager.getKalmanMeasurementNoise(context).toDouble() else RegistryManager.DEFAULT_KALMAN_MEASUREMENT_NOISE.toDouble()
        val headingEmaFactor = if (context != null) RegistryManager.getHeadingEmaFactor(context) else RegistryManager.DEFAULT_HEADING_EMA_FACTOR
        val minMotionSpeed = if (context != null) RegistryManager.getMinMotionSpeedMps(context) else RegistryManager.DEFAULT_MIN_MOTION_SPEED_MPS

        // 1. Run 2D Kalman Filter on Lat/Lng
        val acc = if (accuracyMeters <= 0f) 10.0 else accuracyMeters.toDouble()
        val filteredLat = state.latFilter.filter(rawLat, acc, timeStepSec, qNoise, rNoise)
        val filteredLng = state.lngFilter.filter(rawLng, acc, timeStepSec, qNoise, rNoise)
        val filteredLatLng = LatLng(filteredLat, filteredLng)

        // 2. Determine Speed and Movement
        val previousLatLng = state.lastSmoothedLatLng
        val distanceDelta = if (previousLatLng != null) {
            SphericalUtil.computeDistanceBetween(previousLatLng, filteredLatLng).toFloat()
        } else {
            0f
        }

        val calculatedSpeedMps = if (timeStepSec > 0) distanceDelta / timeStepSec.toFloat() else 0f
        val effectiveSpeedMps = if (rawSpeedMps > 0.5f) rawSpeedMps else calculatedSpeedMps
        state.smoothedSpeedMps = (state.smoothedSpeedMps * 0.4f) + (effectiveSpeedMps * 0.6f)

        val isMoving = state.smoothedSpeedMps >= minMotionSpeed && distanceDelta >= 1.5f

        // 3. Compute Road-Aligned Bearing / Azimuth
        if (isMoving && previousLatLng != null) {
            // Compute spherical azimuth between successive coordinate vectors
            val roadAzimuth = SphericalUtil.computeHeading(previousLatLng, filteredLatLng).toFloat()
            // Normalize to [0, 360)
            val normalizedAzimuth = (roadAzimuth + 360f) % 360f

            val candidateHeading = if (rawBearing > 0.1f) {
                // Blend hardware compass with road trajectory
                blendHeadings(normalizedAzimuth, rawBearing, 0.7f)
            } else {
                normalizedAzimuth
            }

            // Apply EMA Low-Pass Filter to eliminate jitter
            state.smoothedHeading = smoothHeadingEMA(state.smoothedHeading, candidateHeading, headingEmaFactor)
        } else if (rawBearing > 0.1f && isMoving) {
            state.smoothedHeading = smoothHeadingEMA(state.smoothedHeading, rawBearing, headingEmaFactor)
        }

        state.lastSmoothedLatLng = filteredLatLng

        return SmoothedResult(
            smoothedLatLng = filteredLatLng,
            filteredHeading = state.smoothedHeading,
            smoothedSpeedMps = state.smoothedSpeedMps,
            isMoving = isMoving
        )
    }

    /**
     * Compute intermediate points between two LatLngs for ultra-smooth 60 FPS road animation.
     */
    fun interpolateRoadPoint(start: LatLng, end: LatLng, fraction: Float): LatLng {
        return SphericalUtil.interpolate(start, end, fraction.toDouble())
    }

    /**
     * Computes spherical road heading between two arbitrary points.
     */
    fun computeBearingHeading(from: LatLng, to: LatLng): Float {
        val heading = SphericalUtil.computeHeading(from, to).toFloat()
        return (heading + 360f) % 360f
    }

    /**
     * Smooth heading interpolation accounting for 360-degree modular wrap-around.
     */
    private fun smoothHeadingEMA(current: Float, target: Float, factor: Float): Float {
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        val smoothed = current + (diff * factor)
        return (smoothed + 360f) % 360f
    }

    private fun blendHeadings(h1: Float, h2: Float, weight1: Float): Float {
        var diff = (h2 - h1) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        val blended = h1 + (diff * (1f - weight1))
        return (blended + 360f) % 360f
    }

    /**
     * Clears cached filter state for a disconnected or removed device.
     */
    @Synchronized
    fun resetDevice(deviceId: String) {
        deviceStateMap.remove(deviceId)
    }

    /**
     * Clears all filter states.
     */
    @Synchronized
    fun clearAll() {
        deviceStateMap.clear()
    }
}
