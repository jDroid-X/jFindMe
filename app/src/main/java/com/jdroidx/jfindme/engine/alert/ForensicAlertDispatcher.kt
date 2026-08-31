package com.jdroidx.jfindme.engine.alert

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.SystemLogger
import java.util.UUID

/**
 * ForensicAlertDispatcher: Real-time security and anti-tamper alert dispatcher.
 * Dispatches critical device anomalies (GPS disable, Mock GPS, Shutdown, Low Battery) to Guardians via Firebase and local DB.
 */
object ForensicAlertDispatcher {

    private const val TAG = "ForensicAlertDispatcher"

    const val TYPE_GPS_DISABLED = "GPS_DISABLED"
    const val TYPE_MOCK_GPS = "MOCK_GPS_DETECTED"
    const val TYPE_AIRPLANE_MODE = "AIRPLANE_MODE_ENABLED"
    const val TYPE_BATTERY_CRITICAL = "BATTERY_CRITICAL"
    const val TYPE_DEVICE_SHUTDOWN = "DEVICE_SHUTDOWN"
    const val TYPE_EMITTER_REVOKED = "PRIMARY_EMITTER_REVOKED"

    /**
     * Dispatch an emergency forensic alert to Cloud and Local Ledger.
     */
    @JvmStatic
    fun dispatchAlert(
        context: Context,
        alertType: String,
        description: String,
        severity: String = "HIGH",
        extraData: Map<String, Any> = emptyMap()
    ) {
        try {
            val agentId = DeviceIdentityHelper.getOrGenerateAgentId(context)
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val userName = prefs.getString("user_nickname", "Unknown Device") ?: "Unknown Device"
            val activeGroup = prefs.getString("active_group", "") ?: ""

            val alertId = "ALT_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4).uppercase()}"
            val epoch = System.currentTimeMillis()
            val timestampReadable = FirebaseComm.getForensicTimestamp(epoch)

            val alertPayload = mutableMapOf<String, Any>(
                "alert_id" to alertId,
                "alert_type" to alertType,
                "agent_id" to agentId,
                "user_name" to userName,
                "description" to description,
                "severity" to severity,
                "epoch_ms" to epoch,
                "timestamp" to timestampReadable
            )
            if (extraData.isNotEmpty()) {
                alertPayload.putAll(extraData)
            }

            SystemLogger.w(TAG, "🚨 DISPATCHING FORENSIC ALERT: [$alertType] - $description (Severity: $severity)")

            // 1. Write directly to Firebase Device Alert Tree: devices/{agentId}/alerts/{alertId}
            if (FirebaseComm.isAgentIdValid(agentId)) {
                FirebaseComm.getDeviceRef(agentId).child("alerts").child(alertId).setValue(alertPayload)
                    .addOnSuccessListener {
                        SystemLogger.i(TAG, "Alert successfully synced to Firebase device tree: $alertId")
                    }
                    .addOnFailureListener {
                        SystemLogger.e(TAG, "Failed syncing alert to Firebase: ${it.message}")
                    }

                // Log into Master Forensic Change Log
                FirebaseComm.logChange(agentId, "Security_Alert", alertType, alertId, alertPayload)
            }

            // 2. Local Database Forensic Insertion
            try {
                val dbHelper = DatabaseHelper.getInstance(context)
                dbHelper.insertAlert(alertType, description, severity, epoch)
            } catch (dbEx: Exception) {
                SystemLogger.w(TAG, "Local DB alert insertion skipped: ${dbEx.message}")
            }

            // 3. Local Vibration & Notification if high severity
            if (severity == "CRITICAL" || severity == "HIGH") {
                AlertManager.vibrate(context, 400)
            }

        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error dispatching forensic alert", e)
        }
    }
}
