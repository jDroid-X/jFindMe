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
        GREEN("#00E676", Color.parseColor("#00E676"), "ONLINE", "🟢"),
        AMBER("#FFD600", Color.parseColor("#FFD600"), "STALE / LOW BATTERY", "🟡"),
        RED("#FF1744", Color.parseColor("#FF1744"), "OFFLINE / SOS", "🔴"),
        GREY("#9E9E9E", Color.parseColor("#9E9E9E"), "UNLINKED / GHOST", "⚪")
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
                strokeColor = Color.parseColor("#9E9E9E")
            )
        }

        if (isDeleted || isGhost) {
            return EvaluationResult(
                status = RagStatus.GREY,
                statusColor = RagStatus.GREY.colorInt,
                statusText = if (isDeleted) "⚪ DELETED / UNLINKED" else "⚪ GHOST MODE (PRIVATE)",
                cardAlpha = 0.60f,
                isDesaturated = true,
                strokeColor = Color.parseColor("#757575")
            )
        }

        if (isSos) {
            return EvaluationResult(
                status = RagStatus.RED,
                statusColor = Color.parseColor("#FF1744"),
                statusText = "🚨 EMERGENCY SOS ACTIVE",
                cardAlpha = 1.0f,
                isDesaturated = false,
                strokeColor = Color.parseColor("#FF1744")
            )
        }

        val now = System.currentTimeMillis()
        val deltaMs = if (timestamp > 0L) (now - timestamp).coerceAtLeast(0L) else 0L

        // 1. Critical Inactive (> 5 minutes / 300s without heartbeat)
        if (timestamp <= 0L || deltaMs > 300_000L) {
            val mins = (deltaMs / 60_000L).coerceAtLeast(5L)
            val timeText = if (mins >= 60L) "${mins / 60L}h ago" else "${mins}m ago"
            return EvaluationResult(
                status = RagStatus.RED,
                statusColor = Color.parseColor("#FF5252"),
                statusText = "🔴 OFFLINE • LAST SEEN $timeText",
                cardAlpha = 0.75f,
                isDesaturated = true,
                strokeColor = Color.parseColor("#EF5350")
            )
        }

        // 2. Warning / Degraded State (120s – 300s Stale OR Low Battery ≤ 20%)
        val isStale = deltaMs > 120_000L
        val isLowBattery = batteryLevel in 1..20

        if (isStale || isLowBattery) {
            val reason = when {
                isStale && isLowBattery -> "🟡 STALE SIGNAL (${deltaMs / 1000L}s) • BATTERY $batteryLevel%"
                isLowBattery -> "🟡 LOW BATTERY ($batteryLevel%)"
                else -> "🟡 STALE SIGNAL (${deltaMs / 1000L}s ago)"
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

        // 3. Optimal Active (Green)
        val timeFormatted = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        return EvaluationResult(
            status = RagStatus.GREEN,
            statusColor = RagStatus.GREEN.colorInt,
            statusText = "🟢 ONLINE NOW @ $timeFormatted",
            cardAlpha = 1.0f,
            isDesaturated = false,
            strokeColor = RagStatus.GREEN.colorInt
        )
    }
}
