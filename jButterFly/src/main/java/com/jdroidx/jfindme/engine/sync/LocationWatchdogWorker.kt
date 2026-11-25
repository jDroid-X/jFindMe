package com.jdroidx.jfindme.engine.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * PENDING Architecture Fix: Background Service Watchdog (Implemented)
 * Throttles Global WebSocket Location Pings while the app is backgrounded to preserve battery life.
 * Periodically reports Last Known Location natively.
 */
class LocationWatchdogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG = "WatchdogWorker"

    override suspend fun doWork(): Result {
        SystemLogger.i(TAG, "Watchdog executing background location sync & service resurrection check.")

        // 🛡️ DECOMMISSION GATE: If device was decommissioned by admin, suppress resurrection
        try {
            val regPrefs = applicationContext.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            if (regPrefs.getBoolean("is_decommissioned_by_admin", false)) {
                SystemLogger.w(TAG, "Device is marked as DECOMMISSIONED by Admin. Watchdog worker aborting.")
                return Result.success()
            }
        } catch (_: Exception) { }

        // 1. Resurrect TrackingService if not running
        try {
            val serviceIntent = android.content.Intent(applicationContext, com.jdroidx.jfindme.service.TrackingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Watchdog service resurrection error", e)
        }

        // [Variables] user, localAgentId, finalId, timestamp, cts, locTask, loc, repository, prefs, rawBaseId, dynamicIdx, photoSeq, customAvatarTag, resolvedAvatarId, deviceId, userLoc | [Constants] TAG | [Dependencies] FirebaseAuth, FirebaseComm, DeviceIdentityHelper, LocationServices, LocationRepository, GeohashHelper | [Integration Points] Background location sync, Firebase RTDB, SQLite
        val user = FirebaseAuth.getInstance().currentUser
        val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
        val finalId = if (localAgentId.isNotBlank()) localAgentId else (user?.uid ?: "")
        if (finalId.isBlank()) {
            SystemLogger.w(TAG, "No valid device identity. Watchdog aborting cloud push.")
            return Result.success()
        }

        return try {
            val timestamp = System.currentTimeMillis()
            com.jdroidx.jfindme.data.remote.FirebaseComm.getTelemetryRef(finalId).child("last_heartbeat").setValue(timestamp)
            com.jdroidx.jfindme.data.remote.FirebaseComm.lookupIndicesRef.child("active_monitors").child(finalId).setValue(timestamp)

            // Asynchronously resolve cloud mapping as secondary fallback
            if (user != null) {
                com.jdroidx.jfindme.data.remote.FirebaseComm.resolveHardwareIdentity(user.uid) { resolvedAgentId ->
                    if (!resolvedAgentId.isNullOrBlank() && resolvedAgentId != finalId) {
                        com.jdroidx.jfindme.data.remote.FirebaseComm.getTelemetryRef(resolvedAgentId).child("last_heartbeat").setValue(timestamp)
                        com.jdroidx.jfindme.data.remote.FirebaseComm.lookupIndicesRef.child("active_monitors").child(resolvedAgentId).setValue(timestamp)
                    }
                }
            }

            // 2. Fetch fresh GPS fix synchronously on worker thread via active getCurrentLocation with fallback to lastLocation
            try {
                val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(applicationContext)
                val cts = com.google.android.gms.tasks.CancellationTokenSource()
                val locTask = fusedClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                var loc: android.location.Location? = try {
                    com.google.android.gms.tasks.Tasks.await(locTask, 10, java.util.concurrent.TimeUnit.SECONDS)
                } catch (e: Exception) { null }
                if (loc == null) {
                    loc = try {
                        com.google.android.gms.tasks.Tasks.await(fusedClient.lastLocation, 3, java.util.concurrent.TimeUnit.SECONDS)
                    } catch (e: Exception) { null }
                }
                if (loc != null) {
                    val repository = com.jdroidx.jfindme.data.repository.LocationRepository(applicationContext)
                    val prefs = applicationContext.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                    val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
                    val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
                    val photoSeq = prefs.getInt("custom_avatar_seq", 1)
                    val customAvatarTag = String.format(java.util.Locale.US, "AV_%s_%02d", finalId, photoSeq)
                    val resolvedAvatarId = if (dynamicIdx == -1) customAvatarTag else "avatar_custom_$dynamicIdx"
                    val deviceId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(applicationContext)

                    val userLoc = com.jdroidx.jfindme.data.model.UserLocation(
                        userId = finalId,
                        deviceId = deviceId,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        geohash = com.jdroidx.jfindme.engine.location.GeohashHelper.encode(loc.latitude, loc.longitude),
                        bearing = loc.bearing,
                        speed = loc.speed,
                        accuracy = loc.accuracy,
                        altitude = loc.altitude,
                        timestamp = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(System.currentTimeMillis()),
                        userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker",
                        avatar_id = resolvedAvatarId
                    )
                    repository.updateMyLocation(userLoc)
                    SystemLogger.i(TAG, "Watchdog successfully pushed fresh background location: ${loc.latitude}, ${loc.longitude}")
                } else {
                    SystemLogger.d(TAG, "Watchdog location fix returned null (GNSS hardware unavailable).")
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Watchdog GPS fetch error", e)
            }

            SystemLogger.i(TAG, "Watchdog successful. Background heartbeat recorded.")
            Result.success()
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Watchdog failed to ping Firebase cluster.", e)
            Result.retry()
        }
    }
}
