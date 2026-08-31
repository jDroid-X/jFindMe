package com.jdroidx.jfindme.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jdroidx.jfindme.BuildConfig
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.service.TrackingService
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.utils.SystemUpdateScheduler
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 👑 AppUpgradeReceiver: Handles post-upgrade lifecycle state restoration.
 * Triggered automatically by Android OS when MY_PACKAGE_REPLACED occurs.
 * 
 * Responsibilities:
 * 1. Mark system registry as updated.
 * 2. Purge cached APK binary downloads to free device storage.
 * 3. Synchronize new version build metadata to Firebase Cloud RTDB.
 * 4. Restart background tracking sentinel and reschedule periodic workers.
 */
class AppUpgradeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val currentVer = "V${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
            SystemLogger.i(TAG, "🚀 SYSTEM UPGRADE DETECTED: App successfully updated to $currentVer")

            // 1. Update Registry State
            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date())
            RegistryManager.putString(context, RegistryManager.KEY_LAST_UPDATE_TS, "$currentVer on $dateStr")
            RegistryManager.putBoolean(context, RegistryManager.KEY_IS_UPDATED, true)

            // 2. Clean temporary APK cache
            try {
                val apkFile = File(context.cacheDir, "update_jfindme.apk")
                if (apkFile.exists()) {
                    val deleted = apkFile.delete()
                    SystemLogger.i(TAG, "Temporary update APK cache purged: $deleted")
                }
            } catch (e: Exception) {
                SystemLogger.w(TAG, "Failed to clean update APK cache: ${e.message}")
            }

            // 3. Sync Cloud Metadata to Firebase
            val agentId = RegistryManager.getAgentId(context)
            if (agentId.isNotEmpty()) {
                try {
                    FirebaseComm.syncVersionUpdateMetadata(
                        context,
                        agentId,
                        "INSTALLED",
                        0L,
                        "~101 MB"
                    )
                    FirebaseComm.logChange(
                        agentId,
                        "System_Update",
                        "APP_UPGRADE_SUCCESS",
                        "BUILD_${BuildConfig.VERSION_CODE}",
                        mapOf("version" to BuildConfig.VERSION_NAME, "version_code" to BuildConfig.VERSION_CODE.toString()),
                        agentId
                    )
                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Failed to sync post-upgrade metadata to Firebase: ${e.message}")
                }
            }

            // 4. Restart Tracking Engine
            restartTrackingService(context)

            // 5. Reschedule Periodic Silent Update Sentinel
            SystemUpdateScheduler.scheduleCheck(context)
        }
    }

    private fun restartTrackingService(context: Context) {
        try {
            val serviceIntent = Intent(context, TrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            SystemLogger.i(TAG, "TrackingService restarted after upgrade.")
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to restart TrackingService after upgrade", e)
        }
    }

    companion object {
        private const val TAG = "AppUpgradeReceiver"
    }
}
