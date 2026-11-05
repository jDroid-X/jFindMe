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
    @Volatile
    private var agentIdCache: String? = null // Thread-safe cache for generated IDs

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
    @Synchronized
    fun getOrGenerateAgentId(context: Context): String {
        // Check cache first (thread-safe due to @Synchronized)
        agentIdCache?.let { cached ->
            if (isValidAgentId(cached)) {
                return cached
            }
        }

        val cached = RegistryManager.getString(context, RegistryManager.KEY_AGENT_ID, "")
        if (isValidAgentId(cached)) {
            agentIdCache = cached
            return cached
        }

        val rawHw = getRawAndroidId(context).uppercase().filter { it.isLetterOrDigit() }
        val model = (Build.MODEL ?: "").uppercase()
        val brand = (Build.BRAND ?: "").uppercase()
        val device = (Build.DEVICE ?: "").uppercase()

        // Generic Silicon Hardware Derivation
        val derivedId = if (rawHw.length >= 8 && rawHw != FALLBACK_RAW) {
            rawHw.take(4) + rawHw.takeLast(4)
        } else {
            val hwFingerprint = "${brand}_${model}_${Build.BOARD}_${Build.HARDWARE}_${device}"
            try {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                val digest = md.digest(hwFingerprint.toByteArray())
                digest.take(4).joinToString("") { "%02X".format(it) }
            } catch (e: Exception) {
                val fallbackSeed = (Build.FINGERPRINT + model).filter { it.isLetterOrDigit() }.uppercase()
                if (fallbackSeed.length >= 8) fallbackSeed.take(4) + fallbackSeed.takeLast(4) else "NODE0001"
            }
        }

        // Validate before saving
        if (isValidAgentId(derivedId)) {
            RegistryManager.putString(context, RegistryManager.KEY_AGENT_ID, derivedId)
            agentIdCache = derivedId
            SystemLogger.i(TAG, "Deterministic Hardware Agent ID resolved: $derivedId (from raw: $rawHw)")
            return derivedId
        }

        // Fallback: ensure we always return a valid 8-char alphanumeric ID
        val safeId = derivedId.replace(Regex("[^A-Z0-9]"), "").take(8).padEnd(8, '0').uppercase()
        RegistryManager.putString(context, RegistryManager.KEY_AGENT_ID, safeId)
        agentIdCache = safeId
        SystemLogger.w(TAG, "Generated fallback agent ID: $safeId")
        return safeId
    }

    private fun getPersistentAnchorFile(context: Context): java.io.File? {
        return try {
            val mediaDirs = context.externalMediaDirs
            val targetDir = mediaDirs.firstOrNull { it != null } 
                ?: java.io.File(android.os.Environment.getExternalStorageDirectory(), "Android/media/" + context.packageName)
            if (!targetDir.exists()) targetDir.mkdirs()
            java.io.File(targetDir, ".silicon_anchor")
        } catch (e: Exception) {
            null
        }
    }

    private fun savePersistentAnchor(context: Context, agentId: String) {
        try {
            val file = getPersistentAnchorFile(context) ?: return
            file.writeText(agentId.trim().uppercase())
            SystemLogger.d(TAG, "Persisted silicon anchor to external storage: $agentId")
        } catch (e: Exception) { }
    }

    private fun readPersistentAnchor(context: Context): String? {
        return try {
            val file = getPersistentAnchorFile(context)
            if (file != null && file.exists()) {
                val text = file.readText().trim().uppercase()
                if (isValidAgentId(text)) text else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Updates and saves a custom hardware agent ID across RegistryManager, SQLite user_identity, persistent storage, and runtime cache.
     * Returns true if valid and successfully committed.
     */
    @JvmStatic
    @Synchronized
    fun setCustomAgentId(context: Context, newAgentId: String?): Boolean {
        if (!isValidAgentId(newAgentId)) {
            SystemLogger.w(TAG, "setCustomAgentId rejected: Invalid Agent ID format '$newAgentId'")
            return false
        }
        val cleanId = newAgentId!!.trim().uppercase()
        RegistryManager.putString(context, RegistryManager.KEY_AGENT_ID, cleanId)
        agentIdCache = cleanId
        savePersistentAnchor(context, cleanId)

        try {
            val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
            val hwId = getHardwareUid(context)
            val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            db.syncIdentityCore(authUid, cleanId, hwId)
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Failed to cascade updated Agent ID into SQLite: ${e.message}")
        }

        SystemLogger.i(TAG, "Custom Hardware Agent ID committed: $cleanId")
        return true
    }

    /**
     * Complete Hardware Specifications & Dual-SIM Identifier Snapshot
     */
    data class DeviceHardwareInfo(
        val brand: String,
        val model: String,
        val board: String,
        val hardware: String,
        val deviceName: String,
        val imei1: String,
        val imei2: String,
        val isDualSim: Boolean
    )

    @JvmStatic
    fun getHardwareSpecs(context: Context): DeviceHardwareInfo {
        val brand = Build.BRAND ?: "UNKNOWN"
        val model = Build.MODEL ?: "UNKNOWN"
        val board = Build.BOARD ?: "UNKNOWN"
        val hardware = Build.HARDWARE ?: "UNKNOWN"

        // 1. Resolve Friendly Device Name
        var devName: String? = null
        try {
            devName = Settings.Global.getString(context.contentResolver, "device_name")
        } catch (e: Exception) { }

        if (devName.isNullOrBlank()) {
            try {
                devName = Settings.Secure.getString(context.contentResolver, "bluetooth_name")
            } catch (e: Exception) { }
        }

        if (devName.isNullOrBlank()) {
            devName = Build.DEVICE ?: "$brand $model"
        }

        // 2. Dual-SIM Telemetry & IMEI Probe
        var imei1 = ""
        var imei2 = ""
        var isDualSim = false

        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            if (tm != null) {
                val simCount = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> tm.activeModemCount
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> tm.phoneCount
                    else -> 1
                }
                isDualSim = simCount >= 2

                val hasPhoneState = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_PHONE_STATE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasPhoneState) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            imei1 = tm.getImei(0) ?: tm.imei ?: ""
                        } catch (e: SecurityException) {
                            imei1 = "RESTRICTED_API29+"
                        } catch (e: Exception) {
                            imei1 = ""
                        }

                        if (isDualSim) {
                            try {
                                imei2 = tm.getImei(1) ?: ""
                            } catch (e: SecurityException) {
                                imei2 = "RESTRICTED_API29+"
                            } catch (e: Exception) {
                                imei2 = ""
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        try {
                            imei1 = tm.getDeviceId(0) ?: tm.deviceId ?: ""
                            if (isDualSim) {
                                imei2 = tm.getDeviceId(1) ?: ""
                            }
                        } catch (e: Exception) {
                            imei1 = ""
                            imei2 = ""
                        }
                    }
                } else {
                    imei1 = "PERMISSION_REQUIRED"
                    if (isDualSim) imei2 = "PERMISSION_REQUIRED"
                }
            }
        } catch (e: Exception) {
            SystemLogger.w(TAG, "IMEI telemetry probe encountered: ${e.message}")
        }

        return DeviceHardwareInfo(
            brand = brand,
            model = model,
            board = board,
            hardware = hardware,
            deviceName = devName ?: "$brand $model",
            imei1 = imei1,
            imei2 = imei2,
            isDualSim = isDualSim
        )
    }

    @JvmStatic
    fun getHardwareSerial(context: Context): String = getHardwareUid(context)
}
