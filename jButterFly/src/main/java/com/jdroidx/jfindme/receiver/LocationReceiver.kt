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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * LocationReceiver: OS-Level PendingIntent GPS Handler.
 * [Variables] pendingResult, appContext, repository, targetAgentId, deviceId, prefs, db, ssotAvatar, resolvedAvatarId, resolvedUserName, forensicTs, userLoc
 * [Constants] ACTION_PROCESS_LOCATION, TAG
 * [Dependencies] LocationResult, LocationRepository, GeohashHelper, DeviceIdentityHelper, TelemetryFailoverRouter, OfflineSyncManager, SystemLogger, CoroutineScope, Dispatchers
 * [Integration Points] LocationEngine, TrackingService, TelemetryFailoverRouter, WatchdogAlarmReceiver
 */
class LocationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PROCESS_LOCATION = "com.jdroidx.jfindme.ACTION_PROCESS_LOCATION"
        private const val TAG = "LocationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // 🔋 BATTERY & PERFORMANCE DEDUP: If TrackingService is actively running, its in-process callback
        // processes location updates with full dead-reckoning and smoothing. Suppress duplicate execution.
        if (WatchdogAlarmReceiver.isTrackingServiceRunning(context)) {
            SystemLogger.d(TAG, "Suppressed redundant LocationReceiver callback (TrackingService foreground active).")
            return
        }

        try {
            if (LocationResult.hasResult(intent)) {
                // 🚀 ZERO-ANR ASYNC UNBLOCK: Unblock Android Main Looper immediately via goAsync()
                val pendingResult = goAsync()
                val appContext = context.applicationContext ?: context

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val locationResult = LocationResult.extractResult(intent) ?: return@launch
                        val repository = LocationRepository(appContext)
                        val targetAgentId = DeviceIdentityHelper.getOrGenerateAgentId(appContext)
                        val deviceId = DeviceIdentityHelper.getHardwareUid(appContext)
                        val prefs = appContext.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                        val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(appContext)
                        val ssotAvatar = db.getDeviceCentricAvatar(targetAgentId)
                        val resolvedAvatarId = if (ssotAvatar.isNotEmpty() && ssotAvatar != "butterfly") ssotAvatar else (prefs.getString("user_avatar_id", "butterfly") ?: "butterfly")
                        val resolvedUserName = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker"
                        val forensicTs = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(System.currentTimeMillis())

                        // 🔒 PRIVACY GATE: If Ghost Mode (FROZEN) is active, suppress OS broadcast completely
                        val tier = com.jdroidx.jfindme.engine.security.PrivacyEngine.getPrivacyTier(appContext)
                        if (tier == com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier.FROZEN) {
                            SystemLogger.d(TAG, "GHOST_MODE_ACTIVE: LocationReceiver broadcast suppressed for $targetAgentId")
                            return@launch
                        }

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
                    } catch (fatal: Throwable) {
                        SystemLogger.e(TAG, "LOCATION_RECEIVER_ASYNC_ERROR", fatal)
                    } finally {
                        try {
                            pendingResult.finish()
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (fatal: Throwable) {
            SystemLogger.e(TAG, "LOCATION_RECEIVER_FATAL_ERROR", fatal)
        }
    }
}
