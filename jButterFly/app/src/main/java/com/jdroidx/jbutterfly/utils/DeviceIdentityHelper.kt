package com.jdroidx.jbutterfly.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

/**
 * DeviceIdentityHelper — Physical Silicon Hardware Identity Resolution Sentinel.
 * Hardware Root Anchor Mandate:
 * - Direct probe from Physical Silicon Hardware Properties (ro.ril.oem.sno, ro.boot.serialno, ro.serialno, ro.ril.oem.psno)
 * - Secondary probe from Build Hardware Serial
 * - Tertiary probe from Settings.Secure.ANDROID_ID
 * - Deterministic Formula: First 4 Digits + Last 4 Digits of Hardware Key (8 uppercase alphanumeric characters)
 */
object DeviceIdentityHelper {

    private const val TAG = "DeviceIdentityHelper"
    private const val HW_PREFIX = "HW_"
    private const val FALLBACK_RAW = "UNKNOWN_NODE"

    private fun getSystemProperty(propName: String): String? {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            (get.invoke(c, propName) as? String)?.takeIf { it.isNotBlank() && it != "unknown" }
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun getRawHardwareKey(context: Context): String {
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
                try { 
                    Build.getSerial() 
                } catch (e: SecurityException) { 
                    @Suppress("DEPRECATION")
                    Build.SERIAL 
                }
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
    fun getOrGenerateAgentId(context: Context): String {
        val cached = RegistryManager.getString(context, RegistryManager.KEY_AGENT_ID, "")
        if (isValidAgentId(cached)) {
            return cached
        }

        val rawHw = getRawHardwareKey(context).uppercase().filter { it.isLetterOrDigit() }
        val model = (Build.MODEL ?: "").uppercase()
        val brand = (Build.BRAND ?: "").uppercase()
        val device = (Build.DEVICE ?: "").uppercase()

        // Deterministic Silicon Hardware Derivation
        val derivedId = if (rawHw.length >= 8 && rawHw != FALLBACK_RAW) {
            rawHw.take(4) + rawHw.takeLast(4)
        } else {
            val hwFingerprint = "${brand}_${model}_${Build.BOARD}_${Build.HARDWARE}_${device}"
            try {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(hwFingerprint.toByteArray())
                digest.take(4).joinToString("") { "%02X".format(it) }
            } catch (e: Exception) {
                val fallbackSeed = (Build.FINGERPRINT + model).filter { it.isLetterOrDigit() }.uppercase()
                if (fallbackSeed.length >= 8) fallbackSeed.take(4) + fallbackSeed.takeLast(4) else "NODE0001"
            }
        }

        RegistryManager.putString(context, RegistryManager.KEY_AGENT_ID, derivedId)
        RegistryManager.putString(context, RegistryManager.KEY_HARDWARE_KEY, rawHw)
        SystemLogger.i(TAG, "Deterministic Hardware Agent ID resolved: $derivedId (from raw silicon: $rawHw)")
        return derivedId
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

    data class DeviceHardwareInfo(
        val brand: String,
        val model: String,
        val board: String,
        val hardware: String,
        val deviceName: String,
        val agentId: String,
        val hardwareUid: String,
        val hardwareKey: String = ""
    )

    @JvmStatic
    fun getHardwareSpecs(context: Context): DeviceHardwareInfo {
        val brand = Build.BRAND ?: "Generic"
        val model = Build.MODEL ?: "Android"
        val board = Build.BOARD ?: "Board"
        val hardware = Build.HARDWARE ?: "Hardware"

        var devName: String? = null
        try {
            devName = Settings.Global.getString(context.contentResolver, "device_name")
        } catch (ignored: Exception) {}

        if (devName.isNullOrBlank()) {
            devName = "$brand $model"
        }

        return DeviceHardwareInfo(
            brand = brand,
            model = model,
            board = board,
            hardware = hardware,
            deviceName = devName,
            agentId = getOrGenerateAgentId(context),
            hardwareUid = getHardwareUid(context),
            hardwareKey = getRawHardwareKey(context)
        )
    }
}
