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
    private var updateInterval = 3000L

    private fun buildLocationRequest(): LocationRequest {
        return LocationRequest.Builder(trackingPriority, updateInterval).apply {
            setMinUpdateIntervalMillis(1000L)
            setMinUpdateDistanceMeters(5f)
        }.build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                onLocationChanged(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        fusedLocationProviderClient.requestLocationUpdates(
            buildLocationRequest(),
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun setLowPowerMode(enabled: Boolean) {
        trackingPriority = if (enabled) Priority.PRIORITY_BALANCED_POWER_ACCURACY else PRIORITY_HIGH_ACCURACY
        updateInterval = if (enabled) 10000L else 3000L
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
    }
}
