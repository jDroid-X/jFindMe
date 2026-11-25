package com.jdroidx.jfindme.data.model

/**
 * 🔔 NotificationPolicyModel: Single Source of Truth (SSOT) Model for Notification Categories & Preferences.
 *
 * Requirements Matrix:
 * - Mandatory: Authentication & Security notifications (Enabled by default, strictly enforced or active).
 * - Optional / Silent by default:
 *   - Device Sharing & Mesh Sync (default: false / silent)
 *   - Foreground Tracking Sentinel ongoing alert (default: silent IMPORTANCE_MIN/LOW)
 *   - Geofence Perimeter Breaches (default: false / silent)
 *   - Butterfly Point Rendezvous (default: false / silent)
 *   - Battery Alerts (default: false / silent)
 *   - Performance / Diagnostic Warnings (default: false / silent)
 */
data class NotificationPolicyModel(
    // 🛡️ Mandatory Category (Default: TRUE)
    var authSecurityEnabled: Boolean = true,

    // 🔕 Optional Categories (Default: FALSE / SILENT per user mandate)
    var deviceSharingEnabled: Boolean = false,
    var geofenceAlertsEnabled: Boolean = false,
    var butterflyPointEnabled: Boolean = false,
    var batteryAlertsEnabled: Boolean = false,
    var systemUpdatesEnabled: Boolean = false,
    var forensicDiagnosticsEnabled: Boolean = false,

    // Notification presentation mode
    var silentForegroundService: Boolean = true
)
