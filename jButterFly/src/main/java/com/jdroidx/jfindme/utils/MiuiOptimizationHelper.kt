package com.jdroidx.jfindme.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

/**
 * ⚡ Xiaomi / Redmi / MIUI Battery Saver & Autostart Optimization Helper
 * Solves Redmi Note 7 Pro background GPS service termination and ensures continuous tracking:
 * 1. Guides user to enable Autostart in MIUI Security Center.
 * 2. Guides user to set MIUI App Battery Saver to "No Restrictions".
 * 3. Requests standard Android Battery Optimization Whitelist.
 */
object MiuiOptimizationHelper {

    private const val TAG = "MiuiOptimizationHelper"
    private const val PREF_MIUI_CHECKED = "pref_miui_optimization_prompted_v2"

    @JvmStatic
    fun isXiaomiOrRedmi(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val brand = Build.BRAND?.lowercase() ?: ""
        val fingerprint = Build.FINGERPRINT?.lowercase() ?: ""
        return manufacturer.contains("xiaomi") || brand.contains("redmi") || 
               brand.contains("poco") || fingerprint.contains("miui")
    }

    @JvmStatic
    fun checkAndPromptMiuiOptimization(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!isXiaomiOrRedmi()) return

        val prefs = activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val alreadyPrompted = prefs.getBoolean(PREF_MIUI_CHECKED, false)
        if (alreadyPrompted) return

        val pm = activity.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isWhitelisted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(activity.packageName) == true
        } else true

        if (!isWhitelisted) {
            showMiuiOptimizationDialog(activity)
        }
    }

    @JvmStatic
    fun showMiuiOptimizationDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return

        AlertDialog.Builder(activity)
            .setTitle("⚡ Xiaomi / Redmi Background Setup")
            .setMessage(
                "To ensure 24/7 background location tracking on ${Build.MODEL} (MIUI):\n\n" +
                "1. Autostart: Set to ON\n" +
                "2. Battery Saver: Set to 'No Restrictions'\n\n" +
                "Without these settings, MIUI will pause GPS when your screen turns off."
            )
            .setPositiveButton("CONFIGURE NOW") { dialog, _ ->
                dialog.dismiss()
                activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_MIUI_CHECKED, true)
                    .apply()
                launchMiuiAutostartSettings(activity)
            }
            .setNeutralButton("BATTERY SAVER") { dialog, _ ->
                dialog.dismiss()
                launchBatteryOptimizationSettings(activity)
            }
            .setNegativeButton("LATER", null)
            .show()
    }

    @JvmStatic
    fun launchMiuiAutostartSettings(context: Context) {
        val intents = arrayOf(
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.securityadd.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
        )

        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Toast.makeText(context, "Please enable Autostart for jFindMe", Toast.LENGTH_LONG).show()
                return
            } catch (e: Exception) {
                // Try next intent
            }
        }

        launchAppDetailsSettings(context)
    }

    @JvmStatic
    fun launchBatteryOptimizationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fallback to standard optimization list
            }
        }
        launchAppDetailsSettings(context)
    }

    @JvmStatic
    fun launchAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }
}
