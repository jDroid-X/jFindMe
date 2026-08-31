package com.jdroidx.jfindme.engine.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jdroidx.jfindme.data.remote.OfflineSyncManager
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * OfflineSyncWorker: Background WorkManager task for deferred network queue flushing.
 * Replaces manual thread polling to preserve battery in Doze mode.
 */
class OfflineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        SystemLogger.i("OfflineSyncWorker", "Executing deferred offline queue flush.")
        val syncManager = OfflineSyncManager(applicationContext)
        syncManager.triggerBackgroundSync()
        return Result.success()
    }
}
