package com.jdroidx.jfindme.engine.power

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * [Variables] manufacturer, brand, pkg, intents, pm, dialog
 * [Constants] TAG = "OEMPowerWhitelist", PREF_OEM_WHITELIST_CHECKED = "pref_oem_whitelist_prompted"
 * [Dependencies] android.content.Intent, android.content.ComponentName, android.os.Build, PowerManager
 * [Integration Points] SplashActivity, ProfileActivity, SettingsActivity, TrackingService, DeviceIdentityHelper
 *
 * 🔋 Cross-OEM Background Power & Autostart Whitelist Manager (SSOT)
 * Defeats proprietary aggressive background task killers across the top 15-20 Android OEMs:
 * Samsung, Xiaomi/Redmi/Poco, Realme, Oppo, OnePlus, Vivo, iQOO, Huawei, Honor,
 * Motorola, Pixel, Nothing, Asus, Transsion (Tecno/Infinix/Itel), Sony.
 */
object OEMPowerWhitelistManager {

    private const val TAG = "OEMPowerWhitelist"
    private const val PREF_PROMPTED = "pref_oem_whitelist_prompted_v3"

    @JvmStatic
    fun getNormalizedManufacturer(): String {
        return (Build.MANUFACTURER ?: "").lowercase().trim()
    }

    @JvmStatic
    fun getNormalizedBrand(): String {
        return (Build.BRAND ?: "").lowercase().trim()
    }

    @JvmStatic
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        }
        return true
    }

    @JvmStatic
    fun checkAndPromptWhitelist(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        val prefs = activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_PROMPTED, false)) return

        if (!isBatteryOptimizationIgnored(activity) || isAggressiveOem()) {
            showOemGuidanceDialog(activity)
        }
    }

    @JvmStatic
    fun isAggressiveOem(): Boolean {
        val m = getNormalizedManufacturer()
        val b = getNormalizedBrand()
        return m.contains("xiaomi") || b.contains("redmi") || b.contains("poco") ||
               m.contains("realme") || m.contains("oppo") || m.contains("oneplus") ||
               m.contains("vivo") || b.contains("iqoo") ||
               m.contains("huawei") || b.contains("honor") ||
               m.contains("samsung") || m.contains("asus") ||
               m.contains("transsion") || b.contains("infinix") || b.contains("tecno")
    }

    @JvmStatic
    fun showOemGuidanceDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return

        val oemName = Build.MANUFACTURER?.uppercase() ?: "DEVICE"
        val instructions = getBrandInstructions()

        AlertDialog.Builder(activity)
            .setTitle("⚡ 24/7 Background Tracking Setup ($oemName)")
            .setMessage(
                "To prevent $oemName's aggressive task manager from pausing location tracking when your screen turns off, please authorize:\n\n" +
                instructions + "\n\n" +
                "Without these settings, tracking will pause when your phone locks."
            )
            .setPositiveButton("CONFIGURE NOW") { dialog, _ ->
                dialog.dismiss()
                activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_PROMPTED, true)
                    .apply()
                launchOemAutostartSettings(activity)
            }
            .setNeutralButton("BATTERY SAVER") { dialog, _ ->
                dialog.dismiss()
                launchStandardBatterySettings(activity)
            }
            .setNegativeButton("LATER", null)
            .show()
    }

    @JvmStatic
    fun getBrandInstructions(): String {
        val m = getNormalizedManufacturer()
        val b = getNormalizedBrand()
        return when {
            m.contains("xiaomi") || b.contains("redmi") || b.contains("poco") ->
                "1. Enable Autostart in Security Center.\n2. Set Battery Saver to 'No Restrictions'."
            m.contains("realme") || m.contains("oppo") || m.contains("oneplus") ->
                "1. Enable Auto-Launch & Secondary Launch.\n2. Set App Battery to 'Allow background activity'.\n3. Turn OFF 'Quick App Freeze'."
            m.contains("vivo") || b.contains("iqoo") ->
                "1. Set Background Power Consumption to 'High background power consumption'.\n2. Add jFindMe to Autostart Whitelist."
            m.contains("samsung") ->
                "1. Add jFindMe to 'Never sleeping apps'.\n2. Set Battery usage to 'Unrestricted'."
            m.contains("huawei") || b.contains("honor") ->
                "1. Set App Launch to 'Manage manually' (Enable Auto-launch & Run in background)."
            m.contains("asus") ->
                "1. Open Mobile Manager -> Enable Auto-Start for jFindMe."
            m.contains("transsion") || b.contains("infinix") || b.contains("tecno") ->
                "1. Open Phone Master -> Enable 'Auto-run' in Power Marathon."
            else ->
                "1. Set App Battery Usage to 'Unrestricted'.\n2. Disable Adaptive Battery for jFindMe."
        }
    }

    @JvmStatic
    fun launchOemAutostartSettings(context: Context): Boolean {
        val oemIntents = getBrandTargetIntents(context)

        for (intent in oemIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    SystemLogger.i(TAG, "Launched OEM settings: ${intent.component?.flattenToShortString()}")
                    return true
                }
            } catch (e: Exception) {
                SystemLogger.w(TAG, "Failed launching intent: ${e.message}")
            }
        }

        // Fallback to standard battery exemption
        return launchStandardBatterySettings(context)
    }

    private fun getBrandTargetIntents(context: Context): List<Intent> {
        return listOf(
            // Xiaomi / Redmi / POCO (MIUI / HyperOS)
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent("miui.intent.action.OP_AUTO_START"),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")),
            
            // Realme / Oppo / OnePlus (ColorOS / RealmeUI / OxygenOS)
            Intent().setComponent(ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.battery.settings.AppBatterySettingsActivity")),
            Intent().setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.oppoguardelf.PowerUsageDetailActivity")),
            Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")),
            
            // Vivo / iQOO (FuntouchOS / OriginOS)
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")),
            
            // Samsung (OneUI Device Care)
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            Intent("com.samsung.android.sm.ACTION_APP_SLEEP_SETTINGS"),
            
            // Huawei / Honor (EMUI / MagicOS)
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity")),
            
            // Asus Mobile Manager
            Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")),
            
            // Transsion / Infinix / Tecno
            Intent().setComponent(ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.settings.AutoRunActivity")),
            Intent().setComponent(ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.view.AutoRunListActivity")),
            
            // Sony STAMINA Mode
            Intent().setComponent(ComponentName("com.sonymobile.superstamina", "com.sonymobile.superstamina.xperiax_stamina.StaminaActivity"))
        )
    }

    @JvmStatic
    fun launchStandardBatterySettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                SystemLogger.w(TAG, "Standard battery optimization launch failed: ${e.message}")
            }
        }
        return launchAppDetails(context)
    }

    @JvmStatic
    fun launchAppDetails(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
