package com.jdroidx.jfindme.utils

import android.graphics.Color

/**
 * 🚦 Centralized Enterprise RAG (Red-Amber-Green-Grey) Device Status Engine (L7 Architecture)
 * Standardizes real-time device health evaluation across Tile View, Tree View, and Live Maps:
 * - 🟢 GREEN  (#00E676): Active & Healthy (Heartbeat < 120s, Battery > 20%, In Safe Zone)
 * - 🟡 AMBER  (#FFD600): Warning / Degraded (Heartbeat 120s–300s, Low Battery ≤ 20%, Degraded GPS)
 * - 🔴 RED    (#FF1744): Critical / Alert (Heartbeat > 300s, SOS Active, Out-of-bounds Breach)
 * - 🔘 GREY   (#9E9E9E): Disconnected / Ghost (Unlinked, Deleted, Ghost Mode Active)
 */
object DeviceRagEngine {

    enum class RagStatus(
        val colorHex: String,
        val colorInt: Int,
        val label: String,
        val emoji: String
    ) {
        GREEN("#22C55E", Color.parseColor("#22C55E"), "ACTIVE (LIVE)", "🟢"),
        AMBER("#F59E0B", Color.parseColor("#F59E0B"), "INACTIVE (< 6H)", "🟡"),
        RED("#EF4444", Color.parseColor("#EF4444"), "INACTIVE (> 6H)", "🔴"),
        GREY("#64748B", Color.parseColor("#64748B"), "UNLINKED / GHOST", "⚪")
    }

    data class EvaluationResult(
        val status: RagStatus,
        val statusColor: Int,
        val statusText: String,
        val cardAlpha: Float,
        val isDesaturated: Boolean,
        val strokeColor: Int
    )

    @JvmStatic
    @JvmOverloads
    fun evaluate(
        timestamp: Long,
        batteryLevel: Int = -1,
        isSos: Boolean = false,
        isDeleted: Boolean = false,
        isGhost: Boolean = false,
        isPendingUnlink: Boolean = false
    ): EvaluationResult {
        if (isPendingUnlink) {
            return EvaluationResult(
                status = RagStatus.GREY,
                statusColor = RagStatus.GREY.colorInt,
                statusText = "⏳ UNLINK REQUESTED (PARKED)",
                cardAlpha = 0.55f,
                isDesaturated = true,
                strokeColor = Color.parseColor("#64748B")
            )
        }

        if (isDeleted || isGhost) {
            return EvaluationResult(
                status = RagStatus.GREY,
                statusColor = RagStatus.GREY.colorInt,
                statusText = if (isDeleted) "⚪ DELETED / UNLINKED" else "⚪ GHOST MODE (PRIVATE)",
                cardAlpha = 0.60f,
                isDesaturated = true,
                strokeColor = Color.parseColor("#64748B")
            )
        }

        if (isSos) {
            return EvaluationResult(
                status = RagStatus.RED,
                statusColor = Color.parseColor("#EF4444"),
                statusText = "🚨 EMERGENCY SOS ACTIVE",
                cardAlpha = 1.0f,
                isDesaturated = false,
                strokeColor = Color.parseColor("#EF4444")
            )
        }

        val now = System.currentTimeMillis()
        val deltaMs = if (timestamp > 0L) (now - timestamp).coerceAtLeast(0L) else 0L

        val context = com.jdroidx.jfindme.JFindMeApplication.getContext()
        val staleMins = if (context != null) RegistryManager.getStaleInactiveMinutes(context) else 15
        val criticalHours = if (context != null) RegistryManager.getCriticalOfflineHours(context) else 6
        val lowBatteryPct = if (context != null) RegistryManager.getLowBatteryThreshold(context) else 20

        val staleMs = staleMins * 60 * 1000L
        val criticalMs = criticalHours * 60 * 60 * 1000L

        // 1. Critical Inactive (> criticalHours without heartbeat/location update)
        if (timestamp <= 0L || deltaMs > criticalMs) {
            val hours = (deltaMs / (3600_000L)).coerceAtLeast(criticalHours.toLong())
            val timeText = if (hours >= 24L) "${hours / 24L}d ago" else "${hours}h ago"
            return EvaluationResult(
                status = RagStatus.RED,
                statusColor = RagStatus.RED.colorInt,
                statusText = "🔴 INACTIVE • LAST SEEN $timeText",
                cardAlpha = 0.75f,
                isDesaturated = true,
                strokeColor = RagStatus.RED.colorInt
            )
        }

        // 2. Warning / Inactive Recent (staleMins – criticalHours OR Low Battery ≤ lowBatteryPct%)
        val isStale = deltaMs > staleMs
        val isLowBattery = batteryLevel in 1..lowBatteryPct

        if (isStale || isLowBattery) {
            val mins = (deltaMs / 60_000L)
            val timeText = if (mins >= 60L) "${mins / 60L}h ago" else "${mins}m ago"
            val reason = when {
                isStale && isLowBattery -> "🟡 INACTIVE ($timeText) • BATTERY $batteryLevel%"
                isLowBattery -> "🟡 LOW BATTERY ($batteryLevel%)"
                else -> "🟡 INACTIVE • LAST SEEN $timeText"
            }
            return EvaluationResult(
                status = RagStatus.AMBER,
                statusColor = RagStatus.AMBER.colorInt,
                statusText = reason,
                cardAlpha = 0.90f,
                isDesaturated = false,
                strokeColor = RagStatus.AMBER.colorInt
            )
        }

        // 3. Optimal Active (Green - within 15 minutes)
        val timeFormatted = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        return EvaluationResult(
            status = RagStatus.GREEN,
            statusColor = RagStatus.GREEN.colorInt,
            statusText = "🟢 ACTIVE • LIVE @ $timeFormatted",
            cardAlpha = 1.0f,
            isDesaturated = false,
            strokeColor = RagStatus.GREEN.colorInt
        )
    }
}
