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
                
                // 🚀 QA Edge-Computing Split: Extract the Targeted Device Name physically from the Android Hardware Object
                val parts = geofence.requestId.split("___")
                val zoneName = parts[0]
                val assignedTo = if (parts.size > 1) parts[1] else "ALL CONNECTED DEVICES"

                val prefs = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
                val userName = prefs.getString("user_nickname", "Agent") ?: "Agent"
                
                // 🛑 CRITICAL SECURITY FILTER: If this geofence wasn't assigned to me, completely ignore the physical transition!
                if (assignedTo != "ALL CONNECTED DEVICES" && assignedTo != userName) {
                    SystemLogger.i(TAG, "GEOFENCE_SUPPRESSED: Device '$userName' intersected zone but is NOT the targeted Subject ($assignedTo)!")
                    continue
                }

                SystemLogger.i(TAG, "GEOFENCE_EVENT: $transitionName $zoneName for TARGET $userName")
                
                // 🚀 QA Kill-Switch: Does the Parent want background notifications for this specific Safe Zone?
                var alertsEnabled = true
                try {
                    val jsonStr = prefs.getString("geofence_backup_vault", "[]")
                    val array = org.json.JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        if (obj.getString("name") == zoneName) {
                            alertsEnabled = obj.optBoolean("alertsEnabled", true)
                            break
                        }
                    }
                } catch (e: Exception) {}

                if (!alertsEnabled) {
                    SystemLogger.i(TAG, "GEOFENCE_EVENT_MUTED: Bell Indicator is GREY for Zone [$zoneName] - Muting payload!")
                    continue
                }

                // 🚀 QA User Exact Formatting: "Kid moved in/out School Safe Zone"
                val inOut = if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) "in" else "out"
                val exactPayload = "$userName moved $inOut $zoneName Safe Zone"
                
                val groupId = prefs.getString("active_group", null)
                val isMom = prefs.getBoolean("role_mother", false)
                val isDad = prefs.getBoolean("role_father", false)
                val isSpouse = prefs.getBoolean("role_spouse", false)
                val isGuardian = isMom || isDad || isSpouse
                
                // 🛑 PHASE 2 QA INTELLIGENCE: 
                // Guardians crossing perimeters DO NOT spam the children's phones!
                if (!isGuardian) {
                    if (groupId != null && !groupId.startsWith("LOCAL_SANDBOX")) {
                        com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("groups").child(groupId).child("alerts").push().setValue(exactPayload)
                        SystemLogger.w(TAG, "GEOFENCE_SENTINEL: Dependent Perimeter Breach Recorded to Cloud!")
                    }
                } else {
                    SystemLogger.i(TAG, "GEOFENCE_SENTINEL: Guardian crossed border. Firebase Sync Suppressed.")
                }
                
                // 🚀 QA Fix: Linked Notification Table Sync (Forensic Identity Bridge)
                val db = com.jdroidx.jfindme.data.local.CitizenDbHelper(context)
                val activeUid = prefs.getString("user_uid", "OWNER_001") ?: "OWNER_001"
                db.saveAlert(activeUid, "GEOFENCE: $transitionName", exactPayload)
                
                // Natively pop local OS alert
                AlertManager.showGeofenceNotification(context, exactPayload)
            }
        } else {
            SystemLogger.e(TAG, "Invalid Geofence Transition: $geofenceTransition")
        }
    }
}
