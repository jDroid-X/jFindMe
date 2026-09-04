package com.jdroidx.jfindme.engine.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.jdroidx.jfindme.engine.EngineRegistry
import com.jdroidx.jfindme.receiver.LocationReceiver
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * LocationEngine - Adaptive GPS tracking with Dual Foreground Callback + OS PendingIntent Sentinel.
 */
class LocationEngine(private val context: Context, private val onLocationChanged: (Location) -> Unit) {

    private val TAG = "LocationEngine"

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private var trackingPriority = PRIORITY_HIGH_ACCURACY
    private var updateInterval = 60000L

    private val HEARTBEAT_INTERVAL_MS = EngineRegistry.getHeartbeatInterval(EngineRegistry.ENGINE_LOCATION)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, LocationReceiver::class.java).apply {
            action = LocationReceiver.ACTION_PROCESS_LOCATION
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 1001, intent, flags)
    }

    init {
        updateInterval = RegistryManager.getTrackingInterval(context)

        EngineRegistry.observeMode(EngineRegistry.ENGINE_LOCATION) { mode ->
            when (mode) {
                EngineRegistry.EngineMode.ACTIVE -> {
                    updateInterval = RegistryManager.getTrackingInterval(context)
                    trackingPriority = PRIORITY_HIGH_ACCURACY
                    updateTrackingParams()
                    SystemLogger.d(TAG, "Mode -> ACTIVE  (interval: " + updateInterval + "ms)")
                }
                EngineRegistry.EngineMode.HEARTBEAT -> {
                    updateInterval = HEARTBEAT_INTERVAL_MS
                    trackingPriority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    updateTrackingParams()
                    SystemLogger.d(TAG, "Mode -> HEARTBEAT  (interval: " + HEARTBEAT_INTERVAL_MS + "ms)")
                }
                EngineRegistry.EngineMode.DORMANT -> {
                    stopTracking()
                    SystemLogger.d(TAG, "Mode -> DORMANT (tracking stopped)")
                }
            }
        }
    }


    private fun buildLocationRequest(): LocationRequest {
        val configuredDisplacement = RegistryManager.getMinDisplacementMeters(context)
        val isHeartbeat = trackingPriority == Priority.PRIORITY_BALANCED_POWER_ACCURACY
        // 🔋 In heartbeat/background mode, enforce at least 25m displacement to avoid waking CPU while device is stationary
        val minDisplacement = if (isHeartbeat) maxOf(25.0f, configuredDisplacement) else configuredDisplacement
        val waitAccurate = if (isHeartbeat) false else RegistryManager.isStrictAccuracyWait(context)

        return LocationRequest.Builder(trackingPriority, updateInterval).apply {
            setMinUpdateIntervalMillis(maxOf(5000L, updateInterval / 2))
            setMinUpdateDistanceMeters(minDisplacement)
            setMaxUpdateDelayMillis(if (isHeartbeat) updateInterval * 2 else updateInterval)
            setWaitForAccurateLocation(waitAccurate)
        }.build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val loc = locationResult.lastLocation ?: locationResult.locations.lastOrNull()
            loc?.let {
                onLocationChanged(it)
            }
        }
    }


    private val motionListener: (ActivityRecognitionSentinel.MotionState) -> Unit = { motionState ->
        SystemLogger.i(TAG, "Hardware Activity Recognition update in LocationEngine: $motionState")
        when (motionState) {
            ActivityRecognitionSentinel.MotionState.STILL -> {
                // 🔋 ZERO GPS SATELLITE DRAIN: Device is stationary, enter balanced power heartbeat (5 min / 25m)
                trackingPriority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
                updateInterval = HEARTBEAT_INTERVAL_MS
                updateTrackingParams()
                SystemLogger.i(TAG, "🔋 STILL detected: GPS Chip sleeping, heartbeat mode enabled (300s / 25m).")
            }
            ActivityRecognitionSentinel.MotionState.IN_VEHICLE -> {
                // 🚗 HIGH VELOCITY: Fast road tracking (10s / 10m)
                trackingPriority = PRIORITY_HIGH_ACCURACY
                updateInterval = 10000L
                updateTrackingParams()
                SystemLogger.i(TAG, "🚗 IN_VEHICLE detected: High-accuracy road tracking enabled (10s / 10m).")
            }
            ActivityRecognitionSentinel.MotionState.WALKING,
            ActivityRecognitionSentinel.MotionState.RUNNING,
            ActivityRecognitionSentinel.MotionState.ON_BICYCLE -> {
                // 🚶 PEDESTRIAN MOTION: Moderate tracking (20s / 15m)
                trackingPriority = PRIORITY_HIGH_ACCURACY
                updateInterval = 20000L
                updateTrackingParams()
                SystemLogger.i(TAG, "🚶 MOTION detected ($motionState): High-accuracy pedestrian tracking enabled (20s / 15m).")
            }
            ActivityRecognitionSentinel.MotionState.UNKNOWN -> {
                // Default fallback to configured user preference
                val userChoice = RegistryManager.getTrackingInterval(context)
                trackingPriority = PRIORITY_HIGH_ACCURACY
                updateInterval = userChoice
                updateTrackingParams()
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
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                buildLocationRequest(),
                pendingIntent
            )
        } catch (e: Exception) {
            SystemLogger.w(TAG, "PendingIntent GPS registration fallback: " + e.message)
        }

        // 🏃 Initialize Hardware Motion Sentinel
        try {
            ActivityRecognitionSentinel.addListener(motionListener)
            ActivityRecognitionSentinel.startMonitoring(context)
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Could not start ActivityRecognition: ${e.message}")
        }

        val currentMode = EngineRegistry.getMode(EngineRegistry.ENGINE_LOCATION)
        if (currentMode == EngineRegistry.EngineMode.DORMANT) {
            EngineRegistry.heartbeat(EngineRegistry.ENGINE_LOCATION)
        }
        SystemLogger.i(TAG, "Tracking started (interval: " + updateInterval + "ms, priority: " + trackingPriority + ")")
    }

    fun enterHeartbeatMode() {
        EngineRegistry.heartbeat(EngineRegistry.ENGINE_LOCATION)
    }

    fun setLowPowerMode(enabled: Boolean) {
        val userChoice = RegistryManager.getTrackingInterval(context)
        trackingPriority = if (enabled) Priority.PRIORITY_BALANCED_POWER_ACCURACY else PRIORITY_HIGH_ACCURACY
        updateInterval = if (enabled) maxOf(userChoice, 300000L) else userChoice
        updateTrackingParams()
        SystemLogger.i(TAG, "LowPowerMode=" + enabled + " -> interval=" + updateInterval + "ms")
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
                try {
                    fusedLocationProviderClient.requestLocationUpdates(
                        buildLocationRequest(),
                        pendingIntent
                    )
                } catch (e: Exception) {
                    SystemLogger.w(TAG, "Failed updating PendingIntent location request: " + e.message)
                }
            }
        }
    }

    fun stopTracking() {
        ActivityRecognitionSentinel.removeListener(motionListener)
        ActivityRecognitionSentinel.stopMonitoring(context)
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        try {
            fusedLocationProviderClient.removeLocationUpdates(pendingIntent)
        } catch (e: Exception) {}
        trackingThread?.quitSafely()
        trackingThread = null
        EngineRegistry.deactivate(EngineRegistry.ENGINE_LOCATION)
        SystemLogger.i(TAG, "Tracking stopped.")
    }
}
