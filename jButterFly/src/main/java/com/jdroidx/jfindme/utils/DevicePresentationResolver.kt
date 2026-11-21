package com.jdroidx.jfindme.utils

import android.content.Context
import androidx.annotation.ColorInt
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.model.UserLocation

/**
 * 👤 DEVICE PRESENTATION RESOLVER (OOPS-based MVC SSOT Identity Engine)
 * =====================================================================
 * Centralizes all device identity resolution, display name fallbacks, avatar icons,
 * role string parsing, and role callout color resolution across the enterprise project.
 *
 * Implements Prime Directive 4: Dynamic OOPS-based MVC standard, call-by-reference,
 * eliminating 24+ duplicate, fragmented fallback implementations across Activities.
 *
 * [Variables] context, deviceId, fallbackName, maxLength, userLocation, roleStr, roleColor
 * [Constants] DEFAULT_DEVICE_NAME = "Device", HOST_NAME = "You (Host)", MAX_TRUNCATE_LEN = 10
 * [Dependencies] DatabaseHelper, FirebaseAuth, DeviceIdentityHelper, AvatarCalloutGenerator
 * [Integration Points] MapActivity, FamilyFriendsActivity, NetworkGatewayActivity, PersonDetailsActivity, HistoryActivity
 */
object DevicePresentationResolver {

    const val DEFAULT_DEVICE_NAME = "Device"
    const val DEFAULT_AVATAR = "butterfly"

    /**
     * Resolves human-readable SSOT display name for a physical device node.
     * Priority:
     * 1. Check if Self / Host device -> return Operator Nickname or "You (Host)"
     * 2. Check SQLite DeviceCentricName (SSOT master roster)
     * 3. Fallback to UserLocation.userName (if valid and not "Agent_*" or "Target Node")
     * 4. Safe fallback to "Device"
     */
    @JvmStatic
    fun resolveDisplayName(context: Context, deviceId: String?, fallbackName: String? = null): String {
        if (deviceId.isNullOrBlank()) {
            return if (!fallbackName.isNullOrBlank() && !fallbackName.startsWith("Agent_") && !"Target Node".equals(fallbackName, ignoreCase = true)) {
                fallbackName
            } else {
                DEFAULT_DEVICE_NAME
            }
        }

        // 1. Self Device Evaluation
        val selfUid = FirebaseAuth.getInstance().uid ?: ""
        val selfAgent = DeviceIdentityHelper.getOrGenerateAgentId(context)
        val selfCanonical = DeviceIdentityHelper.toCanonicalAgentId(selfAgent)
        val canonicalDevId = DeviceIdentityHelper.toCanonicalAgentId(deviceId)

        val isSelf = deviceId == selfUid || deviceId == selfAgent || deviceId == "SELF_USER" ||
                "You".equals(deviceId, ignoreCase = true) || canonicalDevId.equals(selfCanonical, ignoreCase = true)

        if (isSelf) {
            val nick = RegistryManager.getUserNickname(context)
            return if (!nick.isNullOrBlank()) nick else "You (Host)"
        }

        // 2. Local Database SSOT Master Roster Lookup
        try {
            val localDb = DatabaseHelper.getInstance(context)
            val ssot = localDb.getDeviceCentricName(deviceId)
            if (!ssot.isNullOrBlank() && !ssot.startsWith("Agent_") && !"Target Node".equals(ssot, ignoreCase = true)) {
                return ssot
            }
            if (!canonicalDevId.equals(deviceId, ignoreCase = true)) {
                val ssotCanon = localDb.getDeviceCentricName(canonicalDevId)
                if (!ssotCanon.isNullOrBlank() && !ssotCanon.startsWith("Agent_") && !"Target Node".equals(ssotCanon, ignoreCase = true)) {
                    return ssotCanon
                }
            }
        } catch (ignored: Exception) {}

        // 3. Fallback Name Evaluation
        if (!fallbackName.isNullOrBlank() && !fallbackName.startsWith("Agent_") && !"Target Node".equals(fallbackName, ignoreCase = true)) {
            return fallbackName
        }

        return DEFAULT_DEVICE_NAME
    }

    /**
     * Returns a truncated display name guaranteed to fit inside tactical pills / cards.
     */
    @JvmStatic
    fun resolveTruncatedName(context: Context, deviceId: String?, fallbackName: String? = null, maxLength: Int = 10): String {
        val full = resolveDisplayName(context, deviceId, fallbackName)
        return if (full.length > maxLength) {
            full.substring(0, maxLength - 1) + "…"
        } else {
            full
        }
    }

    /**
     * Resolves avatar ID from incoming payload with SQLite fallback.
     */
    // [Variables] context, deviceId, incomingAvatarId, dbAvatar | [Constants] DEFAULT_AVATAR | [Dependencies] DatabaseHelper, Context | [Integration Points] GeofenceActivity, MapActivity
    @JvmStatic
    @JvmOverloads
    fun resolveAvatarId(context: Context, deviceId: String?, incomingAvatarId: String? = null): String {
        if (!incomingAvatarId.isNullOrBlank() && incomingAvatarId != DEFAULT_AVATAR && incomingAvatarId != "1") {
            return incomingAvatarId
        }
        if (!deviceId.isNullOrBlank()) {
            try {
                val dbAvatar = DatabaseHelper.getInstance(context).getDeviceCentricAvatar(deviceId)
                if (!dbAvatar.isNullOrBlank()) return dbAvatar
            } catch (ignored: Exception) {}
        }
        return DEFAULT_AVATAR
    }

    /**
     * Resolves social role string (e.g., "Child", "Guardian", "Friend").
     */
    @JvmStatic
    fun resolveRoleString(userLocation: UserLocation?): String {
        if (userLocation == null) return ""
        val rf = userLocation.role_family
        val rfr = userLocation.role_friend
        return when {
            !rf.isNullOrBlank() && rf != "None" -> rf
            !rfr.isNullOrBlank() && rfr != "None" -> rfr
            else -> ""
        }
    }

    /**
     * Resolves role bracket tag (e.g. " [Child]").
     */
    @JvmStatic
    fun resolveRoleTag(userLocation: UserLocation?): String {
        val role = resolveRoleString(userLocation)
        return if (role.isNotEmpty()) " [$role]" else ""
    }

    /**
     * Resolves standard tactical accent color for the assigned role.
     */
    @JvmStatic
    @ColorInt
    fun resolveRoleColor(userLocation: UserLocation?): Int {
        val role = resolveRoleString(userLocation)
        return AvatarCalloutGenerator.getRoleColor(role, false)
    }
}
