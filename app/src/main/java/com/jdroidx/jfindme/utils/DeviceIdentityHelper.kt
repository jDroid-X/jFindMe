package com.jdroidx.jfindme.utils

import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * DeviceIdentityHelper — PURE GENERIC HARDWARE IDENTITY RESOLUTION SENTINEL (STRATEGY A).
 *
 * 100% Generic Silicon Resolution:
 * - Direct probe from Physical Silicon Hardware Properties (ro.ril.oem.sno, ro.boot.serialno, ro.serialno, ro.ril.oem.psno)
 * - Secondary probe from Build Hardware Serial
 * - Tertiary probe from Settings.Secure.ANDROID_ID
 * - Deterministic Formula: First 4 Digits + Last 4 Digits of Hardware Key
 *
 * Zero hardcoded device dictionaries, zero static model matching.
 */
object DeviceIdentityHelper {

    private const val TAG = "DeviceIdentityHelper"
    private const val HW_PREFIX = "HW_"
    private const val FALLBACK_RAW = "UNKNOWN_NODE"

    /**
     * Reads a system property via reflection (SELinux allowed properties).
     */
    private fun getSystemProperty(propName: String): String? {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            (get.invoke(c, propName) as? String)?.takeIf { it.isNotBlank() && it != "unknown" }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns the raw hardware silicon serial or Android ID from secure settings.
     */
    @JvmStatic
    fun getRawAndroidId(context: Context): String {
        val oemProps = listOf("ro.ril.oem.sno", "ro.boot.serialno", "ro.serialno", "ro.ril.oem.psno")
        for (prop in oemProps) {
            val valStr = getSystemProperty(prop)
            if (!valStr.isNullOrBlank()) {
                val clean = valStr.uppercase().filter { it.isLetterOrDigit() }
                if (clean.length >= 8 && clean != "UNKNOWN") {
                    return clean
                }
            }
        }

        try {
            val serial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { Build.getSerial() } catch (e: SecurityException) { Build.SERIAL }
            } else {
                @Suppress("DEPRECATION")
                Build.SERIAL
            }
            if (!serial.isNullOrBlank() && serial != "UNKNOWN") {
                val cleanSerial = serial.uppercase().filter { it.isLetterOrDigit() }
                if (cleanSerial.length >= 8) {
                    return cleanSerial
                }
            }
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Serial probe fallback: ${e.message}")
        }

        val raw = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
                ?: FALLBACK_RAW
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to read ANDROID_ID", e)
            FALLBACK_RAW
        }

        return raw.uppercase().filter { it.isLetterOrDigit() }
    }

    @JvmStatic
    fun getHardwareUid(context: Context): String {
        val agentId = getOrGenerateAgentId(context)
        return HW_PREFIX + agentId.uppercase()
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

        // Pure Generic Silicon Derivation (Strategy A: First 4 + Last 4)
        val rawHw = getRawAndroidId(context).uppercase().filter { it.isLetterOrDigit() }
        val derivedId = if (rawHw.length >= 8 && rawHw != FALLBACK_RAW) {
            rawHw.take(4) + rawHw.takeLast(4)
        } else {
            val fallbackSeed = (Build.FINGERPRINT + Build.MODEL).filter { it.isLetterOrDigit() }.uppercase()
            if (fallbackSeed.length >= 8) fallbackSeed.take(4) + fallbackSeed.takeLast(4) else "NODE0001"
        }

        RegistryManager.putString(context, RegistryManager.KEY_AGENT_ID, derivedId)
        SystemLogger.i(TAG, "Generic Hardware Agent ID resolved: $derivedId (from raw: $rawHw)")
        return derivedId
    }
}
