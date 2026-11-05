package com.jdroidx.jfindme.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.LocationResult
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.repository.LocationRepository
import com.jdroidx.jfindme.engine.location.GeofenceHelper
import com.jdroidx.jfindme.engine.location.GeohashHelper
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * LocationReceiver: OS-Level PendingIntent GPS Handler.
 */
class LocationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PROCESS_LOCATION = "com.jdroidx.jfindme.ACTION_PROCESS_LOCATION"
        private const val TAG = "LocationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        try {
            if (LocationResult.hasResult(intent)) {
                val locationResult = LocationResult.extractResult(intent) ?: return
                val appContext = context.applicationContext ?: context
                val repository = LocationRepository(appContext)
                val targetAgentId = DeviceIdentityHelper.getOrGenerateAgentId(appContext)
                val deviceId = DeviceIdentityHelper.getHardwareUid(appContext)
                val prefs = appContext.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(appContext)
                val ssotAvatar = db.getDeviceCentricAvatar(targetAgentId)
                val resolvedAvatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly") ssotAvatar else (prefs.getString("user_avatar_id", "butterfly") ?: "butterfly")
                val resolvedUserName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker"
                val forensicTs = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(System.currentTimeMillis())

                for (location in locationResult.locations) {
                    if (location == null) continue
                    SystemLogger.d(TAG, "OS_PENDING_INTENT_LOCATION_FIX: Lat=${location.latitude}, Lng=${location.longitude}")

                    try {
                        val userLoc = UserLocation(
                            userId = targetAgentId,
                            deviceId = deviceId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            geohash = GeohashHelper.encode(location.latitude, location.longitude),
                            timestamp = forensicTs,
                            bearing = location.bearing,
                            speed = location.speed,
                            accuracy = location.accuracy,
                            altitude = location.altitude,
                            allowDependents = prefs.getBoolean("allow_dependents", true),
                            active_group = prefs.getString("active_group", "") ?: "",
                            isForensicAudit = true,
                            userName = resolvedUserName,
                            avatar_id = resolvedAvatarId
                        )
                        com.jdroidx.jfindme.engine.sync.TelemetryFailoverRouter.dispatchLocationWithFailover(appContext, userLoc)
                    } catch (repoEx: Exception) {
                        SystemLogger.e(TAG, "REPO_UPDATE_PENDING_INTENT_ERROR: ${repoEx.message}", repoEx)
                    }
                }
                
                // Flush offline sync queue
                try {
                    com.jdroidx.jfindme.data.remote.OfflineSyncManager.getInstance(appContext).triggerBackgroundSync()
                } catch (e: Exception) {}
            }
        } catch (fatal: Throwable) {
            SystemLogger.e(TAG, "LOCATION_RECEIVER_FATAL_ERROR", fatal)
        }
    }
}
