package com.jdroidx.jfindme.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.data.local.DatabaseHelper

class SimForensicWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        const val TAG = "SIMForensicWorker"
    }

    override fun doWork(): Result {
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        
        if (prefs.getBoolean("pref_service_halted", false)) {
            SystemLogger.w(TAG, "Health Engine Suspended.")
            return Result.success()
        }

        SystemLogger.d(TAG, "STATIONARY_PULSE: Recording stationary device status.")
        
        return try {
            val dbHelper = DatabaseHelper.getInstance(context)
            val hwId = prefs.getString("hardware_device_id", "LOCAL_NODE") ?: "LOCAL_NODE"
            dbHelper.logSimCard(0, hwId, "DEVICE_HEALTH", "000", "00", "0000", "0000", true)
            dbHelper.logSimTransaction(0, "000", "00", "0000", "0000", 0.0, 0.0, "HEALTH_HEARTBEAT")
            Result.success()
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Health logging skipped: ${e.message}")
            Result.retry()
        }
    }
}
