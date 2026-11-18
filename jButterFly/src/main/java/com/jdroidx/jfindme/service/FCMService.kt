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
                    "SYSTEM_UPDATE_AVAILABLE", "DOWNLOAD_APK" -> {
                        // [Variables] ver: String, bld: String, dlUrl: String, isDownloadOnly: Boolean
                        // [Constants] None
                        // [Dependencies] BuildConfig, ApkInstallerHelper, AlertManager
                        // [Integration Points] Cloud FCM OTA Push Update Pipeline
                        val ver = data["version_name"] ?: (com.jdroidx.jfindme.BuildConfig.VERSION_NAME)
                        val bld = data["version_code"] ?: (com.jdroidx.jfindme.BuildConfig.VERSION_CODE + 1).toString()
                        val dlUrl = data["download_url"] ?: ""
                        val isDownloadOnly = (data["type"] == "DOWNLOAD_APK" || data["action"] == "DOWNLOAD_ONLY" || data["auto_install"] == "false")

                        val prompt = if (isDownloadOnly) {
                            "jButterFly V$ver (Build $bld) downloading to /sdcard/Download/..."
                        } else {
                            "New jFindMe update available: V$ver (Build $bld). Tap to review."
                        }
                        AlertManager.showGeofenceNotification(applicationContext, prompt)

                        if (dlUrl.isNotBlank()) {
                            if (isDownloadOnly) {
                                com.jdroidx.jfindme.utils.ApkInstallerHelper.enqueueSystemDownload(
                                    context = applicationContext,
                                    downloadUrl = dlUrl,
                                    fileName = "jButterFly_V${bld}.apk",
                                    title = "jButterFly V$ver Update"
                                )
                            } else {
                                com.jdroidx.jfindme.utils.ApkInstallerHelper.downloadApkInBackground(applicationContext, dlUrl) { success, apkFile ->
                                    if (success && apkFile != null) {
                                        com.jdroidx.jfindme.utils.ApkInstallerHelper.installSilentlyOrPrompt(
                                            context = applicationContext,
                                            apkFile = apkFile,
                                            versionName = ver
                                        )
                                    }
                                }
                            }
                        } else {
                            com.jdroidx.jfindme.utils.SystemUpdateScheduler.triggerImmediateMaintenance(applicationContext)
                        }
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
                    "DEVICE_SYNC", "DEVICE_SHARING_SYNC" -> {
                        val epochMs = data["epoch_ms"]?.toLongOrNull() ?: 0L
                        val expiresAt = data["expires_at"]?.toLongOrNull() ?: if (epochMs > 0L) epochMs + 86400000L else Long.MAX_VALUE
                        val syncSessionId = data["sync_session_id"] ?: ""
                        val selfId = com.jdroidx.jfindme.utils.RegistryManager.getAgentId(applicationContext)
                        val peerId = data["peer_id"] ?: ""
                        val peerName = data["peer_name"] ?: peerId
                        val peerRole = data["peer_role"] ?: data["role"] ?: "Member"
                        val myRole = data["my_role"] ?: if (peerRole.contains("Child", ignoreCase = true)) "Parent / Guardian"
                                       else if (peerRole.contains("Guardian", ignoreCase = true) || peerRole.contains("Parent", ignoreCase = true)) "Child"
                                       else "Member"
                        val groupId = data["group_id"] ?: ""

                        // ⏱️ 24-Hour Expiration Sentinel: Reject and auto-revoke stale sharing commands via Push
                        if (System.currentTimeMillis() > expiresAt) {
                            SystemLogger.w(TAG, "⏱️ FCM DEVICE_SYNC command expired (>24h). Discarding.")
                            if (selfId.isNotBlank()) {
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(selfId).child("commands_ack").child("device_sync").setValue(mapOf(
                                    "status" to "EXPIRED",
                                    "sync_session_id" to syncSessionId,
                                    "peer_id" to peerId,
                                    "role" to peerRole,
                                    "reason" to "Push sync request exceeded 24-hour acceptance window",
                                    "timestamp" to System.currentTimeMillis()
                                ))
                            }
                            return
                        }

                        if (peerId.isNotBlank()) {
                            val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
                            val syncWakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "jFindMe::FcmDeviceSyncWakeLock")
                            try {
                                syncWakeLock?.acquire(10000L) // 10-second Doze bypass window
                                val sLocalDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(applicationContext)
                                val pairId = "PAIR_${selfId}_${peerId}"
                                com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairingWithRoles(
                                    sLocalDb.writableDatabase,
                                    pairId,
                                    selfId,
                                    peerId,
                                    myRole,
                                    peerRole,
                                    null,
                                    "PAIRED"
                                )
                                val isFriend = peerRole.contains("Friend", ignoreCase = true)
                                sLocalDb.saveRosterNode(peerId, peerName, peerRole, "ACTIVE", false, isFriend, activeGroupId = groupId)
                                sLocalDb.saveRosterNode(selfId, "My Device", myRole, "ACTIVE", false, false, activeGroupId = groupId)
                                sLocalDb.appendValidLinkedAgent(selfId, peerId)
                                sLocalDb.appendValidLinkedAgent(peerId, selfId)
                                sLocalDb.updateCitizenRole(peerId, if (isFriend) "None" else peerRole, if (isFriend) peerRole else "None", isFriend, groupId)

                                val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                                    setPackage(applicationContext.packageName)
                                    putExtra("category", "ROSTER")
                                    putExtra("EXTRA_CATEGORY", "ROSTER")
                                }
                                applicationContext.sendBroadcast(refreshIntent)

                                AlertManager.showDeviceSharingNotification(
                                    applicationContext,
                                    "Device Synchronized",
                                    "$peerName is now synced with your device as [$peerRole] in circle $groupId."
                                )

                                com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(selfId).child("commands_ack").child("device_sync").setValue(mapOf(
                                    "status" to "ACKNOWLEDGED",
                                    "sync_session_id" to syncSessionId,
                                    "peer_id" to peerId,
                                    "role" to peerRole,
                                    "my_role" to myRole,
                                    "timestamp" to System.currentTimeMillis()
                                ))
                            } catch (e: Exception) {
                                SystemLogger.e(TAG, "FCM DEVICE_SYNC error: ${e.message}")
                            } finally {
                                if (syncWakeLock?.isHeld == true) syncWakeLock.release()
                            }
                        }
                    }
                    "BUTTERFLY_INVITATION" -> {
                        AlertManager.showGeofenceNotification(
                            applicationContext,
                            "🦋 Butterfly Point: $body"
                        )
                        val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                            setPackage(applicationContext.packageName)
                            putExtra("category", "BUTTERFLY_POINT")
                            putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                        }
                        applicationContext.sendBroadcast(refreshIntent)
                    }
                    "BUTTERFLY_RESPONSE" -> {
                        AlertManager.showGeofenceNotification(
                            applicationContext,
                            "🦋 Rendezvous Update: $body"
                        )
                        val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                            setPackage(applicationContext.packageName)
                            putExtra("category", "BUTTERFLY_POINT")
                            putExtra("EXTRA_CATEGORY", "BUTTERFLY_POINT")
                        }
                        applicationContext.sendBroadcast(refreshIntent)
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
