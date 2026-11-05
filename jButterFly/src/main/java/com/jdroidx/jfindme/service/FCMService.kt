package com.jdroidx.jfindme.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.engine.alert.AlertManager
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * FCMService: Background Push Sentinel for Geofence Breaches & Cloud Commands.
 * Wakes up dormant / backgrounded devices on perimeter violations.
 */
class FCMService : FirebaseMessagingService() {

    private val TAG = "FCMService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            SystemLogger.i(TAG, "FCM_MESSAGE_RECEIVED from: ${remoteMessage.from}")

            // 1. Process Data Payload
            if (remoteMessage.data.isNotEmpty()) {
                val data = remoteMessage.data
                val type = data["type"] ?: "GENERAL"
                val title = data["title"] ?: "Security Sentinel Alert"
                val body = data["body"] ?: data["msg"] ?: "Perimeter transition detected."
                val zoneName = data["zone_name"] ?: ""
                val avatarName = data["avatar_name"] ?: ""

                SystemLogger.w(TAG, "FCM_DATA_PAYLOAD: Type=$type | Title=$title | Body=$body")

                when (type) {
                    "GEOFENCE_EXIT", "GEOFENCE_ENTER", "GEOFENCE_ALERT" -> {
                        // 🛑 FCM DORMANCY CHECK: Mute repeated pushes for the same state
                        val prefs = getSharedPreferences("system_registry", MODE_PRIVATE)
                        val fcmKey = "FCM_ZONE_STATE_${avatarName}_${zoneName}"
                        val lastFcmState = prefs.getString(fcmKey, null)
                        val currentFcmState = if (type == "GEOFENCE_ENTER") "ENTERED" else if (type == "GEOFENCE_EXIT") "EXITED" else type
                        
                        if (lastFcmState != null && lastFcmState.equals(currentFcmState, ignoreCase = true)) {
                            SystemLogger.i(TAG, "FCM_GEOFENCE_DORMANT: Zone '$zoneName' already in state '$currentFcmState'. Muting notification.")
                        } else {
                            prefs.edit().putString(fcmKey, currentFcmState).apply()
                            AlertManager.showGeofenceNotification(applicationContext, body)
                        }
                    }
                    "GEOFENCE_ASSIGNMENT" -> {
                        AlertManager.showGeofenceNotification(
                            applicationContext,
                            "New Safe Zone Assigned: $zoneName. Protection active."
                        )
                    }
                    "SOS_ALERT" -> {
                        AlertManager.showSosNotification(applicationContext, avatarName.ifEmpty { "Connected Node" })
                        AlertManager.playRingBeaconAlarm(applicationContext, 20)
                    }
                    "RING_DEVICE" -> {
                        val duration = data["duration_sec"]?.toIntOrNull() ?: 30
                        AlertManager.playRingBeaconAlarm(applicationContext, duration)
                    }
                    "STOP_RING" -> {
                        AlertManager.stopRingBeaconAlarm()
                    }
                    "BATTERY_LOW" -> {
                        val pct = data["battery_pct"]?.toIntOrNull() ?: 15
                        AlertManager.showLowBatteryNotification(applicationContext, avatarName.ifEmpty { "Monitored Device" }, pct)
                    }
                    "PING_LOCATION", "POKE_DEVICE", "REQUEST_LOCATION", "REMOTE_WAKEUP" -> {
                        SystemLogger.w(TAG, "REMOTE_LOCATION_WAKEUP: High-priority background location ping received.")
                        requestImmediateLocationAndBroadcast()
                    }
                    "UNLINK_REQUEST" -> {
                        AlertManager.showGeofenceNotification(
                            applicationContext,
                            body.ifEmpty { "Connection unlink request received. Tap to review in Network Gateway." }
                        )
                    }
                    "UNLINK_CANCELLED" -> {
                        AlertManager.showGeofenceNotification(
                            applicationContext,
                            body.ifEmpty { "Unlink request was cancelled. Connection restored." }
                        )
                    }
                    else -> {
                        AlertManager.showGeofenceNotification(applicationContext, body)
                    }
                }
            }

            // 2. Process Standard Notification Payload
            remoteMessage.notification?.let {
                val title = it.title ?: "Mission Sentinel"
                val body = it.body ?: "Perimeter alert received."
                AlertManager.showGeofenceNotification(applicationContext, "$title: $body")
            }
        } catch (fatal: Exception) {
            SystemLogger.e(TAG, "FCM_PROCESS_FATAL_ERROR", fatal)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SystemLogger.i(TAG, "FCM_TOKEN_REFRESHED: $token")

        // Persist token in system registry
        val prefs = getSharedPreferences("system_registry", MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()

        // Sync token to Cloud Matrix for targeted messaging
        val myUid = FirebaseAuth.getInstance().uid
        if (myUid != null) {
            FirebaseComm.resolveHardwareIdentity(myUid) { agentId ->
                val finalId = agentId ?: myUid
                FirebaseComm.devicesRef.child(finalId).child("Identity").child("fcm_token").setValue(token)
            }
        }
    }

    /**
     * 🛰️ Silent High-Priority Location Wakeup ("Find My Phone" style)
     * Obtains immediate high-accuracy GPS fix even when the app is backgrounded or screen is locked.
     */
    @android.annotation.SuppressLint("MissingPermission")
    private fun requestImmediateLocationAndBroadcast() {
        try {
            val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "jFindMe::FcmLocationWakeLock")
            wakeLock.acquire(15000L) // 15-second burst

            val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
            val cts = com.google.android.gms.tasks.CancellationTokenSource()
            
            fusedClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        SystemLogger.i(TAG, "IMMEDIATE_GPS_FIX: Lat=${loc.latitude}, Lng=${loc.longitude}")
                        val agentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
                        val prefs = getSharedPreferences("system_registry", MODE_PRIVATE)
                        val userName = prefs.getString("user_nickname", "Agent") ?: "Agent"
                        val avatarId = prefs.getString("user_avatar_id", "butterfly") ?: "butterfly"

                        val liveMap = mapOf(
                            "latitude" to loc.latitude,
                            "longitude" to loc.longitude,
                            "speed" to loc.speed,
                            "bearing" to loc.bearing,
                            "accuracy" to loc.accuracy,
                            "altitude" to loc.altitude,
                            "updated_at" to FirebaseComm.getForensicTimestamp(),
                            "userName" to userName,
                            "avatar_id" to avatarId
                        )

                        FirebaseComm.devicesRef.child(agentId).child("device").child("live").updateChildren(liveMap)
                    }
                    if (wakeLock.isHeld) wakeLock.release()
                }
                .addOnFailureListener {
                    if (wakeLock.isHeld) wakeLock.release()
                }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Immediate location fetch failed", e)
        }
    }
}
