package com.jdroidx.jfindme.engine.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.jdroidx.jfindme.receiver.GeofenceBroadcastReceiver
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * GeofenceHelper: Orchestrates Google Geofencing API registrations.
 * Level 4: Spatial Interaction Engine.
 */
class GeofenceHelper(context: Context) {
    private val TAG = "GeofenceHelper"
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    fun addGeofence(id: String, lat: Double, lng: Double, radius: Float) {
        // Permission check before registration — show feedback to user
        val hasFineLocation = android.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation) {
            SystemLogger.w(TAG, "addGeofence: ACCESS_FINE_LOCATION not granted, skipping $id")
            android.widget.Toast.makeText(context, 
                "Location permission required to set geofence", 
                android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val effectiveRadius = if (radius >= 50f) radius else 100f
        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(lat, lng, effectiveRadius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent).run {
                addOnSuccessListener {
                    SystemLogger.i(TAG, "Geofence Added: $id at $lat, $lng")
                }
                addOnFailureListener {
                    SystemLogger.e(TAG, "Failed to add geofence: $id", it)
                }
            }
        } catch (e: SecurityException) {
            SystemLogger.e(TAG, "Permission missing for Geofencing", e)
        }
    }

    fun removeGeofence(id: String) {
        try {
            geofencingClient.removeGeofences(listOf(id)).run {
                addOnSuccessListener { SystemLogger.i(TAG, "Geofence Removed: $id") }
                addOnFailureListener { SystemLogger.w(TAG, "Failed to remove geofence: $id") }
            }
        } catch (e: Exception) {
            SystemLogger.w(TAG, "removeGeofence exception: ${e.message}")
        }
    }
}
