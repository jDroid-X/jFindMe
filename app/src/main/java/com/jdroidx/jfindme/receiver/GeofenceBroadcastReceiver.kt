package com.jdroidx.jfindme.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.jdroidx.jfindme.engine.alert.AlertManager
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * GeofenceBroadcastReceiver: Handles background geofencing events.
 * Part of Production Hardening (L5).
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            SystemLogger.e(TAG, "Geofencing Error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()

            for (geofence in triggeringGeofences) {
                val transitionName = if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) "ENTERED" else "EXITED"
                val zoneName = geofence.requestId

                SystemLogger.i(TAG, "GEOFENCE_EVENT: $transitionName $zoneName")
                
                // Alerting Layer (Sync across users)
                AlertManager.showSosNotification(context, "Zone Alert: Child $transitionName $zoneName")
            }
        } else {
            SystemLogger.e(TAG, "Invalid Geofence Transition: $geofenceTransition")
        }
    }
}
