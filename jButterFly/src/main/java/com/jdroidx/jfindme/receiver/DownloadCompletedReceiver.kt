package com.jdroidx.jfindme.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.ApkInstallerHelper
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger
import java.io.File

/**
 * 📥 DownloadCompletedReceiver: Captures DownloadManager ACTION_DOWNLOAD_COMPLETE.
 * Automatically dismisses progress alerts, verifies package integrity, prompts to install,
 * and reports completed status to Firebase RTDB.
 */
class DownloadCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId <= 0L) return

            SystemLogger.i(TAG, "📥 Android DownloadManager completed: ID=$downloadId")

            // 1. Cleanly dismiss in-progress notification
            ApkInstallerHelper.dismissUpdateProgressNotification(context)

            // 2. Query DownloadManager for downloaded file details
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = dm.query(query) ?: return

            try {
                if (cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else -1

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                        val title = if (titleIdx >= 0) cursor.getString(titleIdx) else "jButterFly Update"
                        val localUriStr = if (localUriIdx >= 0) cursor.getString(localUriIdx) else null

                        var downloadedFile: File? = null
                        if (localUriStr != null) {
                            try {
                                val uri = android.net.Uri.parse(localUriStr)
                                if (uri.path != null) downloadedFile = File(uri.path!!)
                            } catch (_: Exception) {}
                        }

                        // Fallback: search in public Downloads folder for newest jButterFly*.apk
                        if (downloadedFile == null || !downloadedFile.exists()) {
                            val pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            if (pubDir != null && pubDir.exists()) {
                                val apks = pubDir.listFiles { f -> f.isFile && f.name.startsWith("jButterFly") && f.name.endsWith(".apk") }
                                if (apks != null && apks.isNotEmpty()) {
                                    downloadedFile = apks.maxByOrNull { it.lastModified() }
                                }
                            }
                        }

                        if (downloadedFile != null && downloadedFile.exists() && ApkInstallerHelper.isValidApkFile(context, downloadedFile)) {
                            SystemLogger.i(TAG, "✅ Downloaded APK verified: ${downloadedFile.absolutePath} (${downloadedFile.length()} bytes)")

                            // Post single authoritative Ready-to-Install notification
                            ApkInstallerHelper.notifyUpdateReadyToInstall(context, downloadedFile, title)

                            // Close feedback loop to Firebase RTDB
                            val agentId = RegistryManager.getAgentId(context)
                            if (agentId.isNotEmpty()) {
                                FirebaseComm.getDeviceRef(agentId).child("Version_Update").child("pending_push").updateChildren(
                                    mapOf<String, Any>(
                                        "status" to "DOWNLOAD_COMPLETED",
                                        "stage" to "DOWNLOAD_COMPLETED",
                                        "download_file" to downloadedFile.name,
                                        "downloaded_bytes" to downloadedFile.length(),
                                        "completed_epoch_ms" to System.currentTimeMillis()
                                    )
                                )
                            }
                        } else {
                            SystemLogger.w(TAG, "Download completed but file is missing or invalid.")
                        }
                    }
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error processing download complete: ${e.message}", e)
            } finally {
                cursor.close()
            }
        }
    }

    companion object {
        private const val TAG = "DownloadCompletedReceiver"
    }
}
