package com.jdroidx.jfindme.engine.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY

/**
 * LocationEngine handles adaptive GPS tracking and FusedLocationProviderClient integration.
 */
class LocationEngine(private val context: Context, private val onLocationChanged: (Location) -> Unit) {

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private var trackingPriority = PRIORITY_HIGH_ACCURACY
    private var updateInterval = 60000L // QA Best-Practice Default (60 seconds)

    init {
        val prefs = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
        updateInterval = prefs.getLong("pref_tracking_interval", 60000L)
    }

    private fun buildLocationRequest(): LocationRequest {
        return LocationRequest.Builder(trackingPriority, updateInterval).apply {
            setMinUpdateIntervalMillis(updateInterval / 2)
            // 🚀 QA Fix: Global Adaptive Geometry. Setting to 60 meters to prevent stationary jitter while maintaining tactical breadcrumbs!
            setMinUpdateDistanceMeters(60f) 
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
        // 🚀 QA Fix: Completely decouple GPS from the UI Main Thread!
        // If we use MainLooper, Android will freeze the GPS the second the screen is turned off!
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

    fun setLowPowerMode(enabled: Boolean) {
        val prefs = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
        val userChoice = prefs.getLong("pref_tracking_interval", 60000L)

        trackingPriority = if (enabled) Priority.PRIORITY_BALANCED_POWER_ACCURACY else PRIORITY_HIGH_ACCURACY
        
        // Critical Battery Safe-Lock: Force at least 5 Minutes (300000L) if OS says battery < 15%
        updateInterval = if (enabled) Math.max(userChoice, 300000L) else userChoice 
        
        updateTrackingParams()
    }

    @SuppressLint("MissingPermission")
    private fun updateTrackingParams() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback).addOnCompleteListener {
            startTracking()
        }
    }

    fun stopTracking() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        trackingThread?.quitSafely()
        trackingThread = null
    }
}
