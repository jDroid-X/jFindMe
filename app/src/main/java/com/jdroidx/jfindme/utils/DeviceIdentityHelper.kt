package com.jdroidx.jfindme.utils

import android.content.Context
import android.provider.Settings

/**
 * DeviceIdentityHelper — CENTRALIZED hardware identity resolution.
 *
 * AUDIT FIX [AUD-016]: The pattern:
 *   val rawHw = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
 *   val hardwareUid = "HW_" + rawHw.toUpperCase()
 * was duplicated in 5+ files. This singleton is the single source of truth.
 *
 * Architecture: utils layer — no Firebase, no UI dependencies.
 */
object DeviceIdentityHelper {

    private const val TAG = "DeviceIdentityHelper"
    private const val HW_PREFIX = "HW_"
    private const val FALLBACK_RAW = "UNKNOWN_NODE"

    /**
     * Returns the raw Android ID from secure settings.
     * Never returns null — falls back to UNKNOWN_NODE.
     */
    @JvmStatic
    fun getRawAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" } // known bad emulator value
                ?: FALLBACK_RAW
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to read ANDROID_ID", e)
            FALLBACK_RAW
        }
    }

    @JvmStatic
    fun getHardwareUid(context: Context): String {
        val raw = getRawAndroidId(context)
        return HW_PREFIX + raw.uppercase()
    }

    @JvmStatic
    fun isValidAgentId(agentId: String?): Boolean {
        if (agentId.isNullOrBlank()) return false
        return agentId.length == 8 && agentId.all { it.isLetterOrDigit() }
    }

    @JvmStatic
    fun isValidHardwareUid(hwUid: String?): Boolean {
        if (hwUid.isNullOrBlank()) return false
        return hwUid.startsWith(HW_PREFIX) && hwUid.length > HW_PREFIX.length
    }

    @JvmStatic
    fun getOrGenerateAgentId(context: Context): String {
        val cached = RegistryManager.getAgentId(context)
        if (isValidAgentId(cached)) {
            return cached
        }

        // Derive deterministic 8-char ID from hardware UID
        val rawHw = getRawAndroidId(context).uppercase()
        val derived = if (rawHw.length >= 8 && rawHw != FALLBACK_RAW) {
            rawHw.take(8).filter { it.isLetterOrDigit() }.padEnd(8, 'X')
        } else {
            val allowed = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            (1..8).map { allowed.random() }.joinToString("")
        }

        RegistryManager.putString(context, RegistryManager.KEY_AGENT_ID, derived)
        SystemLogger.i(TAG, "Deterministic Agent ID resolved and cached: $derived")
        return derived
    }
}
