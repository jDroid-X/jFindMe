package com.jdroidx.jbutterfly.service

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jdroidx.jbutterfly.ui.alert.AcousticSirenActivity
import com.jdroidx.jbutterfly.utils.SystemLogger

/**
 * FCMService — Firebase Cloud Messaging background sentinel.
 */
class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SystemLogger.i("FCMService", "FCM Device Push Token updated: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        SystemLogger.i("FCMService", "Push payload received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val command = data["command"]
            val senderId = data["senderId"] ?: "REMOTE_GUARDIAN"

            if (command == "BUZZ_SIREN") {
                SystemLogger.i("FCMService", "FCM Remote BUZZ command triggered by $senderId")
                val sirenIntent = Intent(this, AcousticSirenActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("SENDER_ID", senderId)
                }
                startActivity(sirenIntent)
            }
        }
    }
}
