package com.jdroidx.jfindme.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.jdroidx.jfindme.engine.alert.AlertManager
import com.jdroidx.jfindme.utils.SystemLogger
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

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
                
                // 🚀 QA Edge-Computing Split: Extract the Targeted Device Name physically from the Android Hardware Object
                val parts = geofence.requestId.split("___")
                val zoneName = parts[0]
                val assignedTo = if (parts.size > 1) parts[1] else "ALL CONNECTED DEVICES"

                // 🚀 mission-critical identity retrieval: sync with global L7 registry
                val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                val userName = prefs.getString("user_nickname", "Agent") ?: "Agent"
                
                // 🛑 CRITICAL SECURITY FILTER: Liberal Identity Handshake (V49.2) - Ensure node name matches even with role suffixes
                if (assignedTo != "ALL CONNECTED DEVICES" && !userName.contains(assignedTo) && !assignedTo.contains(userName)) {
                    SystemLogger.i(TAG, "GEOFENCE_SUPPRESSED: Device '$userName' intersected zone but is NOT the targeted Subject ($assignedTo)!")
                    continue
                }

                SystemLogger.i(TAG, "GEOFENCE_EVENT: $transitionName $zoneName for TARGET $userName")
                
                // 🚀 QA Kill-Switch: Does the Parent want background notifications for this specific Safe Zone?
                val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                val alertsEnabled = db.isGeofenceAlertEnabled(zoneName)

                if (!alertsEnabled) {
                    SystemLogger.i(TAG, "GEOFENCE_EVENT_MUTED: Bell Indicator is GREY for Zone [$zoneName] - Muting payload!")
                    continue
                }

                val groupId = prefs.getString("active_group", null)
                val isGuardian = prefs.getBoolean("role_mother", false) || 
                                 prefs.getBoolean("role_father", false) || 
                                 prefs.getBoolean("role_spouse", false)
                
                // 🛑 NO NEED TO TRACK DEVICE A: Suppress self-tracking if the user is a Guardian (Admin)
                if (isGuardian) {
                    SystemLogger.i(TAG, "GEOFENCE_SUPPRESSED: Admin Node (Device A) self-tracking is disabled per Mission Protocol.")
                    continue
                }

                // 🚀 REMOTE MISSION MONITORING: Push to Cloud Matrix (V49.5)
                val myUid = FirebaseAuth.getInstance().uid
                if (groupId != null && !groupId.startsWith("LOCAL_SANDBOX") && myUid != null) {
                    
                    val cloudPacket = mapOf(
                        "avatar_name" to userName,
                        "zone_name" to zoneName,
                        "status" to transitionName,
                        "timestamp" to System.currentTimeMillis(),
                        "uid" to myUid
                    )
                    
                    // 🛰️ DISPATCH: Instantaneous handshake to Admin's Notification Screen
                    FirebaseDatabase.getInstance().getReference("groups").child(groupId)
                        .child("notifications").push().setValue(cloudPacket)
                }

                // 🔔 UX ALERT: Natively pop local OS notification for the Subject
                val notificationMessage = "Mission Alert: You have $transitionName the $zoneName Safe Zone."
                AlertManager.showGeofenceNotification(context, notificationMessage)

                // 🚀 MASTER-CHILD CLUSTER SYNC: Local Analytic Archival (V42)
                val activeUid = prefs.getString("user_uid", "OWNER_001") ?: "OWNER_001"
                db.saveGeofenceLog(activeUid, userName, zoneName, zoneName, transitionName)
            }
        } else {
            SystemLogger.e(TAG, "Invalid Geofence Transition: $geofenceTransition")
        }
    }
}

