package com.jdroidx.jfindme.engine.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jdroidx.jfindme.data.remote.OfflineSyncManager
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.utils.SystemUpdateScheduler

/**
 * OfflineSyncWorker: Background WorkManager task for deferred network queue flushing.
 * Replaces manual thread polling to preserve battery in Doze mode.
 * Integrates:
 * 1. Queue flush on network reconnection.
 * 2. Yesterday ↔ Today balance data delta reconciliation.
 * 3. Reactive execution of deferred silent updates.
 */
class OfflineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        SystemLogger.i("OfflineSyncWorker", "Executing deferred offline queue flush and balance delta reconciliation.")
        try {
            val syncManager = OfflineSyncManager.getInstance(applicationContext)
            syncManager.triggerBackgroundSync()
            
            // Reconcile Yesterday ↔ Today Balance Data
            val balancedCount = syncManager.reconcileYesterdayTodayBalance()
            SystemLogger.i("OfflineSyncWorker", "Balance delta sync flushed: $balancedCount records.")

            // Check if 02:39 AM system update was deferred due to offline state
            SystemUpdateScheduler.checkDeferredUpdateOnReconnect(applicationContext)

            return Result.success()
        } catch (e: Exception) {
            SystemLogger.e("OfflineSyncWorker", "Deferred sync failure: ${e.message}")
            return Result.retry()
        }
    }
}
