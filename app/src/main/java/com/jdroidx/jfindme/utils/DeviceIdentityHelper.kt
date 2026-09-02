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

        val rawHw = getRawAndroidId(context).uppercase()
        val model = (android.os.Build.MODEL ?: "").uppercase()
        val brand = (android.os.Build.BRAND ?: "").uppercase()
        val device = (android.os.Build.DEVICE ?: "").uppercase()

        // 1. Canonical Device Roster Resolution (Strict Hardware & Model Matching)
        val canonicalId = when {
            // REDMI 5 (rosy): Must match "REDMI 5" or "ROSY" or rawHw "2B75", EXCLUDING "NOTE"
            (rawHw.contains("2B75") || (model.contains("REDMI 5") && !model.contains("NOTE")) || device.contains("ROSY")) -> "2B75E1E9"

            // REDMI N7 PRO / REDMI NOTE (violet / whyred / note 7 pro / note 5 pro):
            (rawHw.contains("BE5A") || model.contains("NOTE 7 PRO") || model.contains("NOTE 5 PRO") || device.contains("VIOLET") || device.contains("WHYRED")) -> "BE5A234A"

            rawHw.contains("497F") || model.contains("RIDDHI") -> "497FA75E"
            rawHw.contains("96F0") || model.contains("PUSHPA") -> "96F0E18F"
            rawHw.contains("B110") || model.contains("INNOVANT") -> "B1108B1A"
            else -> null
        }

        if (canonicalId != null) {
            RegistryManager.putString(context, RegistryManager.KEY_AGENT_ID, canonicalId)
            SystemLogger.i(TAG, "Canonical Agent ID resolved for device ($model): $canonicalId")
            return canonicalId
        }

        // 2. Key-Independent Hardware Fingerprint Hash (SHA-256 of Build Properties)
        val hwFingerprint = "${android.os.Build.BRAND}_${android.os.Build.MODEL}_${android.os.Build.HARDWARE}_${android.os.Build.BOARD}"
        val derived = try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(hwFingerprint.toByteArray())
            digest.take(4).joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            if (rawHw.length >= 8 && rawHw != FALLBACK_RAW) {
                rawHw.take(8).filter { it.isLetterOrDigit() }.padEnd(8, 'X')
            } else {
                "NODE" + (1000..9999).random()
            }
        }

        RegistryManager.putString(context, RegistryManager.KEY_AGENT_ID, derived)
        SystemLogger.i(TAG, "Deterministic Hardware Agent ID resolved ($hwFingerprint): $derived")
        return derived
    }
}
