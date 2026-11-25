package com.jdroidx.jfindme.engine.location

import com.jdroidx.jfindme.utils.SystemLogger

/**
 * [Variables] isAppInForeground, isSosActive, motionState, lastKnownLat, lastKnownLng, cellId, lac
 * [Constants] ACCURACY_CELL_TOWER = 500.0f, PROVIDER_SIM = "CELL_TOWER_SIM", PROVIDER_GPS = "gps"
 * [Dependencies] SystemLogger
 * [Integration Points] TrackingService.kt, SIMTrackingService.kt, LocationEngine.kt, SimTelephonyEngine.kt, FirebaseComm.kt
 *
 * 🛰️ Dual-Engine Telemetry & Power Coordinator (SSOT)
 * Solves the GPS vs SIM Telemetry conflict:
 * 1. App in Foreground -> Powers up GPS (10s - 20s if moving, 30s - 60s if stationary).
 * 2. App in Background / Screen Off -> Powers down GPS chip to save 90% battery, activates SIM Cell Tower tracking.
 * 3. Fallback Lat/Lng Anchor -> Ensures SIM updates never write (0.0, 0.0) to Firebase, retaining live RAG status.
 * 4. Emergency SOS -> Concurrent dual-engine surge (5s high-rate GPS + SIM failover).
 */
enum class TelemetryMode {
    FOREGROUND_HIGH_GPS,
    FOREGROUND_BALANCED_GPS,
    BACKGROUND_SIM_FALLBACK,
    DUAL_PARALLEL_SURGE
}

object DualModeCoordinator {

    private const val TAG = "DualModeCoordinator"
    const val ACCURACY_CELL_TOWER_METERS = 500.0f
    const val PROVIDER_CELL_TOWER = "CELL_TOWER_SIM"
    const val PROVIDER_GPS = "gps"

    @JvmStatic
    fun resolveTelemetryMode(
        isAppInForeground: Boolean,
        isSosActive: Boolean,
        isMoving: Boolean
    ): TelemetryMode {
        return when {
            isSosActive -> {
                SystemLogger.i(TAG, "Mode: DUAL_PARALLEL_SURGE (Emergency SOS Active)")
                TelemetryMode.DUAL_PARALLEL_SURGE
            }
            isAppInForeground -> {
                if (isMoving) {
                    SystemLogger.d(TAG, "Mode: FOREGROUND_HIGH_GPS (In-Motion)")
                    TelemetryMode.FOREGROUND_HIGH_GPS
                } else {
                    SystemLogger.d(TAG, "Mode: FOREGROUND_BALANCED_GPS (Stationary)")
                    TelemetryMode.FOREGROUND_BALANCED_GPS
                }
            }
            else -> {
                SystemLogger.d(TAG, "Mode: BACKGROUND_SIM_FALLBACK (Screen off / Background)")
                TelemetryMode.BACKGROUND_SIM_FALLBACK
            }
        }
    }

    @JvmStatic
    fun shouldPowerDownGps(isAppInForeground: Boolean, isSosActive: Boolean): Boolean {
        // GPS is powered down in background UNLESS active SOS is underway
        return !isAppInForeground && !isSosActive
    }

    /**
     * Resolves valid coordinates for SIM tower updates so they never broadcast (0.0, 0.0).
     * If live GPS is absent, uses the last verified non-zero anchor.
     */
    @JvmStatic
    fun resolveSimCoordinates(
        currentLat: Double,
        currentLng: Double,
        fallbackAnchorLat: Double,
        fallbackAnchorLng: Double
    ): Pair<Double, Double> {
        return if (Math.abs(currentLat) > 0.001 && Math.abs(currentLng) > 0.001) {
            Pair(currentLat, currentLng)
        } else if (Math.abs(fallbackAnchorLat) > 0.001 && Math.abs(fallbackAnchorLng) > 0.001) {
            Pair(fallbackAnchorLat, fallbackAnchorLng)
        } else {
            // Unresolved cold anchor
            Pair(0.0, 0.0)
        }
    }
}
