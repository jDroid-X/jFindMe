package com.jdroidx.jfindme.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.jdroidx.jfindme.engine.alert.AlertManager
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.utils.RegistryManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * GeofenceBroadcastReceiver: Handles background geofencing events.
 * Part of Production Hardening (L5).
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        try {
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
                    val rawZoneId = parts[0]
                    val assignedTo = if (parts.size > 1) parts[1] else "ALL CONNECTED DEVICES"

                    val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                    val zoneName = db.getSafeZoneDisplayName(rawZoneId)

                    // 🚀 mission-critical identity retrieval: sync with global L7 registry
                    val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                    val userName = prefs.getString("user_nickname", "Agent") ?: "Agent"
                    val myUid = FirebaseAuth.getInstance().uid ?: "NULL_IDENTITY"
                    
                    // 🛑 CRITICAL SECURITY FILTER: Multi-Point Identity Handshake (V50.0)
                    val matchesIdentity = assignedTo == "ALL CONNECTED DEVICES" || 
                                          assignedTo.equals(myUid, ignoreCase = true) || 
                                          assignedTo.equals(userName, ignoreCase = true)
                    if (!matchesIdentity) {
                        SystemLogger.i(TAG, "GEOFENCE_SUPPRESSED: Device '$userName' ($myUid) intersected zone but is NOT the targeted Subject ($assignedTo)!")
                        continue
                    }

                    // 🕒 TEMPORAL SENTINEL: Active Schedule Window & Day-Fencing Verification
                    val inSchedule = db.isSafeZoneInSchedule(rawZoneId)
                    if (!inSchedule) {
                        SystemLogger.i(TAG, "GEOFENCE_OUT_OF_SCHEDULE: Event suppressed because Zone '$zoneName' is outside configured active hours/days.")
                        continue
                    }

                    // 🛑 ZERO-DUPLICATE DORMANCY STATE LATCH: 
                    // Once an 'IN' or 'OUT' notification is emitted for a given zone, it enters a dormant state.
                    // No further notifications will be emitted until the device transitions to the opposite state.
                    val stateKey = "GEOFENCE_LATCH_STATE_${rawZoneId}_${myUid}"
                    val stateTimeKey = "GEOFENCE_LATCH_TIME_${rawZoneId}_${myUid}"
                    val lastState = prefs.getString(stateKey, null)
                    val lastTime = prefs.getLong(stateTimeKey, 0L)
                    val now = System.currentTimeMillis()

                    if (lastState != null && lastState.equals(transitionName, ignoreCase = true)) {
                        SystemLogger.i(TAG, "GEOFENCE_DORMANT_STATE: Zone '$zoneName' is already latched as '$transitionName'. Alert dormant until opposite perimeter transition occurs.")
                        continue
                    }

                    // Debounce rapid hardware boundary jitter (15s threshold)
                    if (now - lastTime < 15000L) {
                        SystemLogger.i(TAG, "GEOFENCE_JITTER_SUPPRESSED: Rapid perimeter flutter suppressed (${now - lastTime}ms).")
                        continue
                    }

                    // State transition verified: update dormancy latch
                    prefs.edit()
                        .putString(stateKey, transitionName)
                        .putLong(stateTimeKey, now)
                        .apply()

                    SystemLogger.i(TAG, "GEOFENCE_TRANSITION_EMITTED: $transitionName $zoneName for TARGET $userName (New State: $transitionName)")
                
                // 🚀 QA Kill-Switch: Does the Parent want background notifications for this specific Safe Zone?
                val alertsEnabled = db.isGeofenceAlertEnabled(rawZoneId)

                if (!alertsEnabled) {
                    SystemLogger.i(TAG, "GEOFENCE_EVENT_MUTED: Bell Indicator is GREY for Zone [$zoneName] - Muting payload!")
                    continue
                }

                val groupId = prefs.getString("active_group", null)

                // 🚀 REMOTE MISSION MONITORING: Push to Cloud Matrix (V50.0)
                if (groupId != null && !groupId.startsWith("LOCAL_SANDBOX") && myUid != "NULL_IDENTITY") {
                    try {
                        val cleanGroupId = FirebaseComm.sanitizePathKey(groupId)
                        val now = System.currentTimeMillis()
                        val dateFormatted = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(now))
                        val timeFormatted = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(now))
                        val dateTimeFormatted = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(now))
                        val shortDateTime = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(now))
                        val cloudPacket = mapOf(
                            "avatar_name" to userName,
                            "zone_name" to zoneName,
                            "status" to transitionName,
                            "timestamp" to now,
                            "date" to dateFormatted,
                            "time" to timeFormatted,
                            "datetime" to dateTimeFormatted,
                            "readable_time" to shortDateTime,
                            "uid" to myUid
                        )

                        // 🌳 GEOFENCING TREE DISPATCH: Creator -> SafeZone -> Target Device -> Notifications (Group by Date)
                        val hostDeviceCode = prefs.getString("agent_id", null)
                            ?: com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
                            ?: db.resolveAgentIdLocally(myUid)
                            ?: FirebaseComm.sanitizePathKey(groupId)
                        val cleanHostCode = FirebaseComm.sanitizePathKey(hostDeviceCode)
                        val targetDeviceCode = prefs.getString("agent_id", null)
                            ?: com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
                            ?: db.resolveAgentIdLocally(myUid) 
                            ?: myUid
                        val cleanTargetCode = FirebaseComm.sanitizePathKey(targetDeviceCode)

                        val eventId = "EVT_${now}_${cleanTargetCode.takeLast(4)}"
                        val transitionTag = if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) "IN" else "OUT"
                        val rawCleanZone = if (geofence.requestId.contains("___")) geofence.requestId.split("___")[0] else geofence.requestId
                        val cleanZoneId = FirebaseComm.sanitizePathKey(rawCleanZone)

                        val avatarId = prefs.getString("user_avatar_id", "butterfly") ?: "butterfly"

                        val treeNotificationPacket = mapOf(
                            "event_id" to eventId,
                            "transition" to transitionTag,
                            "event_type" to if (transitionTag == "IN") "ENTER" else "EXIT",
                            "timestamp" to now,
                            "date" to dateFormatted,
                            "time" to timeFormatted,
                            "datetime" to dateTimeFormatted,
                            "readable_time" to shortDateTime,
                            "state_latch" to transitionTag,
                            "target_uid" to cleanTargetCode,
                            "target_name" to userName,
                            "avatar_id" to avatarId,
                            "zone_id" to cleanZoneId,
                            "zone_name" to zoneName,
                            "creator_uid" to cleanHostCode
                        )
                        
                        // 📜 GEOFENCING NODE (REGULAR HISTORY & NOTIFICATIONS)
                        val targetDeviceRef = FirebaseComm.getGeofencingTargetDeviceRef(cleanHostCode, cleanZoneId, cleanTargetCode)
                        targetDeviceRef.child("notifications").child(dateFormatted).child(eventId).setValue(treeNotificationPacket)
                        targetDeviceRef.updateChildren(mapOf(
                            "device_id" to cleanTargetCode,
                            "device_name" to userName,
                            "avatar_id" to avatarId,
                            "current_state" to transitionTag,
                            "last_transition_ts" to now,
                            "last_transition_readable" to shortDateTime
                        ))

                        // 📱 DEVICE NODE (LATEST FINAL DATA & STATE SNAPSHOT)
                        val liveUpdateMap = mutableMapOf<String, Any>(
                            "current_safe_zone_id" to cleanZoneId,
                            "current_safe_zone_name" to zoneName,
                            "current_safe_zone_state" to transitionTag,
                            "last_geofence_transition_ts" to now,
                            "last_geofence_transition_readable" to shortDateTime,
                            "userName" to userName,
                            "avatar_id" to avatarId
                        )

                        // If ENTERING safe zone, query zone coordinate from SQLite and update live GPS
                        if (transitionTag == "IN") {
                            try {
                                val zoneModel = db.getSafeZoneById(cleanZoneId)
                                if (zoneModel != null && zoneModel.lat != 0.0 && zoneModel.lng != 0.0) {
                                    liveUpdateMap["latitude"] = zoneModel.lat
                                    liveUpdateMap["longitude"] = zoneModel.lng
                                    liveUpdateMap["address"] = "Inside $zoneName Safe Perimeter"
                                }
                            } catch (e: Exception) {}
                        }

                        val targetDeviceLiveRef = FirebaseComm.getDeviceRef(cleanTargetCode)
                        targetDeviceLiveRef.child("device").child("live").updateChildren(liveUpdateMap)
                        targetDeviceLiveRef.child("safe_zones").child(cleanZoneId).updateChildren(mapOf(
                            "current_state" to transitionTag,
                            "last_transition_ts" to now,
                            "last_transition_readable" to shortDateTime,
                            "updated_at" to now
                        ))

                        // 🛰️ DISPATCH 1: Instantaneous handshake to Admin's Notification Screen & Map HUD
                        FirebaseComm.relationshipsRef.child(cleanGroupId)
                            .child("notifications").push().setValue(cloudPacket)

                        // 🛰️ DISPATCH 2: Persistent history audit log on the Geofence node
                        val historyEntry = "$userName $transitionName on $dateFormatted at $timeFormatted"
                        FirebaseComm.relationshipsRef.child(cleanGroupId)
                            .child("geofences").child(cleanZoneId).child("history").push().setValue(historyEntry)
                            
                        // GAP-007, LOOP-007: Fire FCM Trigger for Transitions
                        val fcmPayload = mapOf(
                            "targetGroupId" to cleanGroupId,
                            "title" to "Safe Zone $transitionName • $timeFormatted",
                            "body" to "$userName has $transitionName the $zoneName Safe Zone on $dateFormatted at $timeFormatted.",
                            "type" to if (transitionName == "EXITED") "GEOFENCE_EXIT" else "GEOFENCE_ENTER",
                            "zone_name" to zoneName,
                            "avatar_name" to userName,
                            "timestamp" to now,
                            "date" to dateFormatted,
                            "time" to timeFormatted,
                            "datetime" to dateTimeFormatted
                        )
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("fcm_queue").add(fcmPayload)
                            .addOnSuccessListener {
                                SystemLogger.i(TAG, "FCM Push Event Queued for: $zoneName ($transitionName) at $dateTimeFormatted")
                            }
                    } catch (e: Exception) {
                        SystemLogger.e(TAG, "GEOFENCE_CLOUD_DISPATCH_ERR", e)
                    }
                }

                // 🔔 UX ALERT: Natively pop local OS notification for the Subject with explicit date & time
                val nowTime = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
                val nowDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                val notificationMessage = "$userName has $transitionName the '$zoneName' Safe Zone.\nDate: $nowDate • Time: $nowTime"
                AlertManager.showGeofenceNotification(context, notificationMessage)

                // 🚀 MASTER-CHILD CLUSTER SYNC: Local Analytic Archival (V50.0)
                val fallbackOwner = RegistryManager.DEFAULT_FALLBACK_OWNER_ID
                val activeUid = prefs.getString("user_uid", fallbackOwner) ?: fallbackOwner
                
                db.saveGeofenceLog(activeUid, userName, geofence.requestId, zoneName, transitionName)
            }
        } else {
            SystemLogger.e(TAG, "Invalid Geofence Transition: $geofenceTransition")
        }
    } catch (fatal: Exception) {
        SystemLogger.e(TAG, "GEOFENCE_RECEIVE_FATAL_ERROR", fatal)
    }
}
}

