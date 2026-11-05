package com.jdroidx.jfindme.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.jdroidx.jfindme.engine.sync.DatabaseMaintenanceWorker as EngineDatabaseMaintenanceWorker

/**
 * Legacy compatibility bridge for DatabaseMaintenanceWorker in worker package.
 */
class DatabaseMaintenanceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : EngineDatabaseMaintenanceWorker(appContext, workerParams)
