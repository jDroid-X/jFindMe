package com.jdroidx.jfindme.engine.security

import android.content.Context
import android.location.Location
import android.os.Build
import android.provider.Settings
import com.jdroidx.jfindme.utils.SystemLogger
import java.io.File

/**
 * SecurityEngine: Detects device manipulation and security risks.
 * Rule 5: Security Agent Implementation.
 */
object SecurityEngine {
    private const val TAG = "SecurityEngine"

    @JvmStatic
    fun isRooted(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    @JvmStatic
    fun isMockLocation(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    @JvmStatic
    fun isDeveloperModeEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic
    fun isAirplaneModeOn(context: Context): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
    }

    /**
     * Audit Check: Returns a security risk score (0-100).
     */
    @JvmStatic
    fun performSecurityAudit(context: Context, currentLocation: Location?): Int {
        var riskScore = 0
        if (isRooted()) riskScore += 50
        if (isDeveloperModeEnabled(context)) riskScore += 20
        if (currentLocation != null && isMockLocation(currentLocation)) {
            riskScore += 30
            com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.dispatchAlert(
                context,
                com.jdroidx.jfindme.engine.alert.ForensicAlertDispatcher.TYPE_MOCK_GPS,
                "Mock location provider detected in live tracking stream.",
                "HIGH"
            )
        }
        
        SystemLogger.i(TAG, "Security Audit Completed. Risk Score: $riskScore")
        return riskScore
    }
}
