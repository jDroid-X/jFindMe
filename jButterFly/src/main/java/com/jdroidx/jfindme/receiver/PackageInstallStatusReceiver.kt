package com.jdroidx.jfindme.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 📦 PackageInstallStatusReceiver: Captures PackageInstaller Session Callbacks.
 * Handles unattended upgrade progress, user-action requests, and error diagnostics.
 */
class PackageInstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "No status message"

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                SystemLogger.i(TAG, "Unattended install requires user confirmation prompt.")
                val confirmationIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmationIntent != null) {
                    confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmationIntent)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                SystemLogger.i(TAG, "✅ PackageInstaller session completed successfully!")
            }
            else -> {
                SystemLogger.e(TAG, "⚠️ PackageInstaller session failed with status ($status): $message")
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.jdroidx.jfindme.ACTION_INSTALL_STATUS"
        private const val TAG = "PackageInstallStatusReceiver"
    }
}
