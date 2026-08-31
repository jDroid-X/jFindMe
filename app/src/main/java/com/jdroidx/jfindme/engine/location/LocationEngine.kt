package com.jdroidx.jfindme.engine.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.jdroidx.jfindme.engine.EngineRegistry
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * LocationEngine — Adaptive GPS tracking with 3-mode heartbeat lifecycle support.
 *
 * MODES (EngineRegistry):
 * ─ ACTIVE     : Normal update interval from system_registry pref_tracking_interval
 * ─ HEARTBEAT  : 300-second (5-min) minimal pulse — keeps engine alive at low cost
 * ─ DORMANT    : stopTracking() — zero cost; resumes on next activate()
 *
 * AUDIT FIX [AUD-011]: Added pause()/resume() heartbeat support.
 * AUDIT FIX [AUD-015]: Uses RegistryManager instead of raw SharedPreferences.
 */
class LocationEngine(private val context: Context, private val onLocationChanged: (Location) -> Unit) {

    private val TAG = "LocationEngine"

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private var trackingPriority = PRIORITY_HIGH_ACCURACY
    private var updateInterval = 60000L

    // Heartbeat interval: 5-minute minimum pulse when screen is hidden
    private val HEARTBEAT_INTERVAL_MS = EngineRegistry.getHeartbeatInterval(EngineRegistry.ENGINE_LOCATION)

    init {
        updateInterval = RegistryManager.getTrackingInterval(context)

        // Observe EngineRegistry mode changes
        EngineRegistry.observeMode(EngineRegistry.ENGINE_LOCATION) { mode ->
            when (mode) {
                EngineRegistry.EngineMode.ACTIVE -> {
                    updateInterval = RegistryManager.getTrackingInterval(context)
                    trackingPriority = PRIORITY_HIGH_ACCURACY
                    updateTrackingParams()
                    SystemLogger.d(TAG, "Mode → ACTIVE (interval: ${updateInterval}ms)")
                }
                EngineRegistry.EngineMode.HEARTBEAT -> {
                    // Switch to 5-min low-cost heartbeat
                    updateInterval = HEARTBEAT_INTERVAL_MS
                    trackingPriority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    updateTrackingParams()
                    SystemLogger.d(TAG, "Mode → HEARTBEAT (interval: ${HEARTBEAT_INTERVAL_MS}ms)")
                }
                EngineRegistry.EngineMode.DORMANT -> {
                    stopTracking()
                    SystemLogger.d(TAG, "Mode → DORMANT (tracking stopped)")
                }
            }
        }
    }

    private fun buildLocationRequest(): LocationRequest {
        return LocationRequest.Builder(trackingPriority, updateInterval).apply {
            setMinUpdateIntervalMillis(maxOf(1000L, updateInterval / 2))
            // 0m minimum displacement enables immediate live movement detection
            setMinUpdateDistanceMeters(0f)
        }.build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                onLocationChanged(it)
            }
        }
    }

    private var trackingThread: android.os.HandlerThread? = null

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (trackingThread == null) {
            trackingThread = android.os.HandlerThread("JFindMeGpsHardwareThread")
            trackingThread?.start()
        }
        fusedLocationProviderClient.requestLocationUpdates(
            buildLocationRequest(),
            locationCallback,
            trackingThread?.looper ?: Looper.getMainLooper()
        )
        EngineRegistry.activate(EngineRegistry.ENGINE_LOCATION)
        SystemLogger.i(TAG, "Tracking started (interval: ${updateInterval}ms, priority: $trackingPriority)")
    }

    /**
     * Transitions to HEARTBEAT mode — low-cost 5-min pulses.
     * Call from Service.onTaskRemoved() or when app is backgrounded.
     */
    fun enterHeartbeatMode() {
        EngineRegistry.heartbeat(EngineRegistry.ENGINE_LOCATION)
        // Callback in init{} handles the actual parameter update
    }

    fun setLowPowerMode(enabled: Boolean) {
        val userChoice = RegistryManager.getTrackingInterval(context)
        trackingPriority = if (enabled) Priority.PRIORITY_BALANCED_POWER_ACCURACY else PRIORITY_HIGH_ACCURACY
        // Battery safe-lock: Force at least 5 min if battery < 15%
        updateInterval = if (enabled) maxOf(userChoice, 300000L) else userChoice
        updateTrackingParams()
        SystemLogger.i(TAG, "LowPowerMode=$enabled → interval=${updateInterval}ms")
    }

    @SuppressLint("MissingPermission")
    private fun updateTrackingParams() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback).addOnCompleteListener {
            if (EngineRegistry.getMode(EngineRegistry.ENGINE_LOCATION) != EngineRegistry.EngineMode.DORMANT) {
                if (trackingThread == null) {
                    trackingThread = android.os.HandlerThread("JFindMeGpsHardwareThread")
                    trackingThread?.start()
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    buildLocationRequest(),
                    locationCallback,
                    trackingThread?.looper ?: Looper.getMainLooper()
                )
            }
        }
    }

    fun stopTracking() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        trackingThread?.quitSafely()
        trackingThread = null
        EngineRegistry.deactivate(EngineRegistry.ENGINE_LOCATION)
        SystemLogger.i(TAG, "Tracking stopped.")
    }
}

