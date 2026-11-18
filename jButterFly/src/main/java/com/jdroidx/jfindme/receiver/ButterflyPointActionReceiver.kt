package com.jdroidx.jfindme.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable
import com.jdroidx.jfindme.engine.map.ButterflyPointEngine
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * ============================================================================
 * [BR]₅․₁₅: ButterflyPointActionReceiver
 * ----------------------------------------------------------------------------
 * Handles direct notification action buttons (ACCEPT / REJECT) without forcing
 * the operator to launch the full activity.
 * ============================================================================
 */
class ButterflyPointActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ACCEPT = "com.jdroidx.jfindme.ACTION_BUTTERFLY_ACCEPT"
        const val ACTION_REJECT = "com.jdroidx.jfindme.ACTION_BUTTERFLY_REJECT"
        const val EXTRA_POINT_ID = "point_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val TAG = "ButterflyPointActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val pointId = intent.getStringExtra(EXTRA_POINT_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        // Dismiss notification with safe-cast to avoid ClassCastException
        if (notificationId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(notificationId)
        }

        val localDeviceId = DeviceIdentityHelper.getOrGenerateAgentId(context)

        when (action) {
            ACTION_ACCEPT -> {
                SystemLogger.i(TAG, "Operator ACCEPTED Butterfly Point: $pointId")
                ButterflyPointEngine.respondToInvitation(
                    context = context,
                    pointId = pointId,
                    deviceId = localDeviceId,
                    accept = true,
                    rejectionReason = null
                )
                // 🔄 Feedback Loop Closure: Immediately process local location update so Device A & peers see immediate progress
                try {
                    val lat = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLatitude(context)
                    val lng = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLongitude(context)
                    if (lat != 0.0 && lng != 0.0) {
                        ButterflyPointEngine.processLocalLocationUpdate(context, lat, lng, 0f)
                    }
                } catch (_: Exception) {}
                Toast.makeText(context, "Butterfly Point Accepted", Toast.LENGTH_SHORT).show()
            }
            ACTION_REJECT -> {
                SystemLogger.w(TAG, "Operator REJECTED Butterfly Point: $pointId")
                ButterflyPointEngine.respondToInvitation(
                    context = context,
                    pointId = pointId,
                    deviceId = localDeviceId,
                    accept = false,
                    rejectionReason = "Declined via quick notification"
                )
                Toast.makeText(context, "Butterfly Point Declined", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
