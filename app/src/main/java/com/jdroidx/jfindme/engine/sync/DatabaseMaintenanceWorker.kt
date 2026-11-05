package com.jdroidx.jfindme.engine.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 🧹 DatabaseMaintenanceWorker: Background WorkManager task for rolling storage hygiene.
 * Purges expired time-series records, deletes stale queues, and runs SQLite VACUUM to reclaim disk space.
 */
open class DatabaseMaintenanceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "DatabaseMaintenanceWorker"
        const val WORK_NAME = "DatabaseMaintenanceWork"
    }

    override suspend fun doWork(): Result {
        return try {
            SystemLogger.i(TAG, "Starting periodic SQLite storage hygiene & prune sweep...")
            val prefs = applicationContext.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val retentionDays = RegistryManager.getStorageRetentionDays(applicationContext)

            val db = DatabaseHelper.getInstance(applicationContext)
            val rowsDeleted = db.pruneExpiredData(retentionDays)
            val cacheReclaimed = db.pruneCacheAndTempFiles()

            // Record execution timestamp
            prefs.edit()
                .putLong("pref_last_run_JOB_STORAGE_PRUNE", System.currentTimeMillis())
                .putLong("pref_last_storage_purged_rows", rowsDeleted)
                .putLong("pref_last_cache_reclaimed_bytes", cacheReclaimed)
                .apply()

            val cacheMb = String.format(java.util.Locale.US, "%.1f", cacheReclaimed / (1024.0 * 1024.0))
            SystemLogger.i(TAG, "Storage sweep completed successfully. Rows pruned: $rowsDeleted, Cache reclaimed: $cacheMb MB. Current DB size: ${db.getDatabaseStorageSizeMb()} MB")
            Result.success()
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Database maintenance sweep failed", e)
            Result.retry()
        }
    }
}
