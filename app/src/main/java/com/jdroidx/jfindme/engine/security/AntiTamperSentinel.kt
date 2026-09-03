package com.jdroidx.jfindme.engine.security

import android.content.Context
import android.location.Location
import android.os.Build
import android.provider.Settings
import com.jdroidx.jfindme.utils.SystemLogger
import java.io.File

/**
 * 🛡️ AntiTamperSentinel: Realtime Threat, Mock Location & Root Detection Engine
 */
object AntiTamperSentinel {
    private const val TAG = "AntiTamperSentinel"

    private val ROOT_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    @JvmStatic
    fun isDeviceRooted(): Boolean {
        // 1. Check build tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // 2. Check for su binary
        for (path in ROOT_PATHS) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun isLocationMocked(location: Location?): Boolean {
        if (location == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    @JvmStatic
    fun isDeveloperOptionsActive(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Conducts a full security audit and returns a comprehensive status map.
     */
    @JvmStatic
    fun auditDeviceSecurity(context: Context, location: Location?): SecurityAuditReport {
        val isRooted = isDeviceRooted()
        val isMock = isLocationMocked(location)
        val isDevMode = isDeveloperOptionsActive(context)
        
        var threatScore = 0
        if (isRooted) threatScore += 50
        if (isMock) threatScore += 40
        if (isDevMode) threatScore += 10

        return SecurityAuditReport(
            isRooted = isRooted,
            isMockGps = isMock,
            isDeveloperMode = isDevMode,
            threatScore = threatScore,
            isCompromised = threatScore >= 40
        )
    }

    data class SecurityAuditReport(
        val isRooted: Boolean,
        val isMockGps: Boolean,
        val isDeveloperMode: Boolean,
        val threatScore: Int,
        val isCompromised: Boolean
    )
}
