package com.jdroidx.jfindme.data.model

/**
 * [Variables] id, title, icon, description, isFamily, defaultPermissionsMask | [Constants] None | [Dependencies] None | [Integration Points] MapActivity, FamilyFriendsActivity, PairingRepository, NetworkGatewayActivity
 * Master Single Source of Truth (SSOT) catalog for all relationship and device sharing roles.
 */
data class DeviceRoleEntry(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val isFamily: Boolean,
    val defaultPermissionsMask: Int
)

object DeviceRoleCatalog {
    val FAMILY_ROLES = listOf(
        DeviceRoleEntry("CO_PARENT", "Co-Parent / Guardian", "👑", "Full oversight, geofencing, siren trigger & bidirectional GPS tracking", true, 0xFF),
        DeviceRoleEntry("DEPENDENT_CHILD", "Dependent Child", "🧸", "Location tracked, geofence alerts active, emergency SOS beacon enabled", true, 0x07),
        DeviceRoleEntry("SPOUSE", "Spouse / Partner", "💍", "Mutual live tracking, safe-zone entry/exit alerts, battery status sharing", true, 0x1F),
        DeviceRoleEntry("ELDER_CARE", "Elder / Senior Care", "🩺", "Passive telemetry tracking, health vitals, stationary motion alerts", true, 0x0F)
    )

    val FRIEND_ROLES = listOf(
        DeviceRoleEntry("CLOSE_FRIEND", "Close Friend", "🤝", "Mutual live GPS location sharing and distance telemetry", false, 0x03),
        DeviceRoleEntry("COLLEAGUE", "Colleague / Peer", "💼", "Temporary work/commute coordinate sharing with geofence visibility", false, 0x01),
        DeviceRoleEntry("NEIGHBOR", "Neighbor / Community", "🏡", "Proximity radar check and mutual safe zone status check", false, 0x01)
    )

    fun getAllRoles(): List<DeviceRoleEntry> = FAMILY_ROLES + FRIEND_ROLES

    fun getRoleById(id: String): DeviceRoleEntry? {
        return getAllRoles().firstOrNull { it.id.equals(id, ignoreCase = true) || it.title.equals(id, ignoreCase = true) }
    }

    fun getIconForRole(roleTitle: String?): String {
        if (roleTitle == null) return "📱"
        val upper = roleTitle.uppercase()
        return when {
            upper.contains("PARENT") || upper.contains("GUARDIAN") || upper.contains("ADMIN") || upper.contains("FATHER") || upper.contains("MOTHER") -> "👑"
            upper.contains("CHILD") || upper.contains("SON") || upper.contains("DAUGHTER") || upper.contains("KID") -> "🧸"
            upper.contains("SPOUSE") || upper.contains("WIFE") || upper.contains("HUSBAND") || upper.contains("PARTNER") -> "💍"
            upper.contains("ELDER") || upper.contains("SENIOR") -> "🩺"
            upper.contains("FRIEND") || upper.contains("PEER") -> "🤝"
            upper.contains("COLLEAGUE") -> "💼"
            upper.contains("NEIGHBOR") -> "🏡"
            else -> "📱"
        }
    }
}
