package com.jdroidx.jfindme.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import com.jdroidx.jfindme.engine.power.OEMPowerWhitelistManager

/**
 * ⚡ Backward-Compatible Call-by-Reference Wrapper for Xiaomi / Redmi / MIUI Optimization
 * Delegates all logic to canonical SSOT [OEMPowerWhitelistManager] (zero code duplication).
 */
object MiuiOptimizationHelper {

    @JvmStatic
    fun isXiaomiOrRedmi(): Boolean {
        val m = OEMPowerWhitelistManager.getNormalizedManufacturer()
        val b = OEMPowerWhitelistManager.getNormalizedBrand()
        val fp = Build.FINGERPRINT?.lowercase() ?: ""
        return m.contains("xiaomi") || b.contains("redmi") || b.contains("poco") || fp.contains("miui")
    }

    @JvmStatic
    fun checkAndPromptMiuiOptimization(activity: Activity) {
        OEMPowerWhitelistManager.checkAndPromptWhitelist(activity)
    }

    @JvmStatic
    fun showMiuiOptimizationDialog(activity: Activity) {
        OEMPowerWhitelistManager.showOemGuidanceDialog(activity)
    }

    @JvmStatic
    fun launchMiuiAutostartSettings(context: Context) {
        OEMPowerWhitelistManager.launchOemAutostartSettings(context)
    }

    @JvmStatic
    fun launchBatteryOptimizationSettings(context: Context) {
        OEMPowerWhitelistManager.launchStandardBatterySettings(context)
    }

    @JvmStatic
    fun launchAppDetailsSettings(context: Context) {
        OEMPowerWhitelistManager.launchAppDetails(context)
    }
}
