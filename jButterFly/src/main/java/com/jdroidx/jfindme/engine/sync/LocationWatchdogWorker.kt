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

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            SystemLogger.w(TAG, "No active user session. Watchdog aborting cloud push.")
            return Result.success()
        }

        return try {
            val timestamp = System.currentTimeMillis()
            val localAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(applicationContext)
            val finalId = if (localAgentId.isNotBlank()) localAgentId else user.uid
            com.jdroidx.jfindme.data.remote.FirebaseComm.getTelemetryRef(finalId).child("last_heartbeat").setValue(timestamp)
            com.jdroidx.jfindme.data.remote.FirebaseComm.lookupIndicesRef.child("active_monitors").child(finalId).setValue(timestamp)

            // Asynchronously resolve cloud mapping as secondary fallback
            com.jdroidx.jfindme.data.remote.FirebaseComm.resolveHardwareIdentity(user.uid) { resolvedAgentId ->
                if (!resolvedAgentId.isNullOrBlank() && resolvedAgentId != finalId) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getTelemetryRef(resolvedAgentId).child("last_heartbeat").setValue(timestamp)
                    com.jdroidx.jfindme.data.remote.FirebaseComm.lookupIndicesRef.child("active_monitors").child(resolvedAgentId).setValue(timestamp)
                }
            }

            // 2. Fetch fresh GPS fix synchronously on worker thread via FusedLocationProviderClient (up to 5s timeout)
            try {
                val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(applicationContext)
                val loc = com.google.android.gms.tasks.Tasks.await(fusedClient.lastLocation, 5, java.util.concurrent.TimeUnit.SECONDS)
                if (loc != null) {
                    val repository = com.jdroidx.jfindme.data.repository.LocationRepository(applicationContext)
                    val prefs = applicationContext.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                    val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
                    val dynamicIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
                    val photoSeq = prefs.getInt("custom_avatar_seq", 1)
                    val customAvatarTag = String.format(java.util.Locale.US, "AV_%s_%02d", finalId, photoSeq)
                    val resolvedAvatarId = if (dynamicIdx == -1) customAvatarTag else "avatar_custom_$dynamicIdx"
                    val rawHw = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getRawAndroidId(applicationContext)
                    val deviceId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(applicationContext)

                    val userLoc = com.jdroidx.jfindme.data.model.UserLocation(
                        userId = finalId,
                        deviceId = deviceId,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        geohash = com.jdroidx.jfindme.engine.location.GeohashHelper.encode(loc.latitude, loc.longitude),
                        bearing = loc.bearing,
                        speed = loc.speed,
                        timestamp = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(System.currentTimeMillis()),
                        userName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker",
                        avatar_id = resolvedAvatarId
                    )
                    repository.updateMyLocation(userLoc)
                    SystemLogger.i(TAG, "Watchdog successfully pushed background location: ${loc.latitude}, ${loc.longitude}")
                } else {
                    SystemLogger.d(TAG, "Watchdog lastLocation returned null (cached GPS fix not yet available).")
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
