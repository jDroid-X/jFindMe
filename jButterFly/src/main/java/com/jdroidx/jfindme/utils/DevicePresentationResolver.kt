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
// [Variables] context, deviceId, fallbackName, allowPronoun, maxLength, userLocation, roleStr, roleColor, isSelf, selfUid, selfAgent, selfCanonical, canonicalDevId, localDb, ssot, ssotCanon, primeTarget, primeName, dossier, dName | [Constants] DEFAULT_DEVICE_NAME, DEFAULT_AVATAR | [Dependencies] DatabaseHelper, FirebaseAuth, DeviceIdentityHelper, AvatarCalloutGenerator, RegistryManager | [Integration Points] MapActivity, FamilyFriendsActivity, NetworkGatewayActivity, PersonDetailsActivity, HistoryActivity, LocationRepository, DeviceRepository
object DevicePresentationResolver {

    const val DEFAULT_DEVICE_NAME = "Device"
    const val DEFAULT_AVATAR = "butterfly"
    
    // Dynamic fallbacks linked to RegistryManager SSOT
    @Deprecated("Use RegistryManager.getCircleHostDeviceId(context) for dynamic SSOT", ReplaceWith("RegistryManager.getCircleHostDeviceId(context)"))
    const val KNOWN_HOST_ID = "2T929153"
    @Deprecated("Use RegistryManager.getCircleHostName(context) for dynamic SSOT", ReplaceWith("RegistryManager.getCircleHostName(context)"))
    const val KNOWN_HOST_NAME = "JEET"

    /**
     * 🛡️ Centralized validator that identifies artificial or placeholder names across the ecosystem.
     * When isSelf is false, relative pronouns like "You", "Owner", etc. are treated as placeholders.
     */
    @JvmStatic
    @JvmOverloads
    fun isPlaceholderName(name: String?, isSelf: Boolean = false): Boolean {
        if (name.isNullOrBlank()) return true
        val trimmed = name.trim()
        if (trimmed.equals("null", ignoreCase = true) ||
            trimmed.equals("Device", ignoreCase = true) ||
            trimmed.equals("Unknown", ignoreCase = true) ||
            trimmed.equals("Unknown Device", ignoreCase = true) ||
            trimmed.equals("Secure Node", ignoreCase = true) ||
            trimmed.equals("Target Node", ignoreCase = true) ||
            trimmed.equals("Peer Node", ignoreCase = true) ||
            trimmed.equals("Active Tracker", ignoreCase = true) ||
            trimmed.equals("Member", ignoreCase = true) ||
            trimmed.equals("None", ignoreCase = true) ||
            trimmed.equals("Master Guardian", ignoreCase = true) ||
            trimmed.equals("Primary Guardian", ignoreCase = true) ||
            trimmed.equals("Co-Guardian", ignoreCase = true) ||
            trimmed.equals("Guardian", ignoreCase = true) ||
            trimmed.equals("Ward", ignoreCase = true)
        ) {
            return true
        }
        if (!isSelf && (trimmed.equals("You", ignoreCase = true) ||
            trimmed.startsWith("You (", ignoreCase = true) ||
            trimmed.equals("Owner", ignoreCase = true) ||
            trimmed.equals("Self", ignoreCase = true)
        )) {
            return true
        }
        val lower = trimmed.lowercase()
        return lower.startsWith("node ") ||
                lower.startsWith("node_") ||
                lower.startsWith("agent_") ||
                lower.startsWith("target_node") ||
                lower.startsWith("hw_") ||
                lower.matches(Regex("^node[ _-][0-9a-f]+$", RegexOption.IGNORE_CASE))
    }

    /**
     * 👑 SSOT Circle Host Resolver: Resolves the authoritative Master Guardian (Host device ID) of a circle.
     */
    @JvmStatic
    @JvmOverloads
    fun getCircleHostDeviceId(context: Context, groupId: String? = null): String {
        val effGroup = if (!groupId.isNullOrBlank() && groupId != "DEFAULT_GROUP") groupId else RegistryManager.getEffectiveActiveGroup(context)
        if (effGroup.startsWith("GRP_") && !effGroup.equals("GRP_LOCAL_MESH", ignoreCase = true) && !effGroup.equals("GRP_UNIVERSAL_FLEET", ignoreCase = true)) {
            val candidate = effGroup.substring(4)
            val canon = DeviceIdentityHelper.toCanonicalAgentId(candidate)
            if (canon.length in 6..12) return canon
        }
        try {
            val db = DatabaseHelper.getInstance(context)
            val host = db.getGroupHostDeviceId(effGroup)
            if (!host.isNullOrBlank()) return host
        } catch (_: Exception) {}
        return RegistryManager.getCircleHostDeviceId(context)
    }

    /**
     * 👑 Determines whether target deviceId represents the Master Guardian (Creator/Host) of the active circle.
     */
    @JvmStatic
    @JvmOverloads
    fun isCircleHost(context: Context, deviceId: String?, groupId: String? = null): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val hostId = getCircleHostDeviceId(context, groupId)
        if (hostId.isBlank()) return false
        val canonical = DeviceIdentityHelper.toCanonicalAgentId(deviceId)
        return canonical.equals(hostId, ignoreCase = true)
    }

    /**
     * Resolves human-readable SSOT display name for a physical device node.
     * Priority:
     * 1. Check if Self / Host device -> return Operator Nickname or "You"
     * 2. Check Gate 1 Minor / Prime Target Child -> return "Riddhi" / Prime name
     * 3. Check Circle Host Device -> return Master Guardian name ("JEET")
     * 4. Check SQLite DeviceCentricName (SSOT master roster)
     * 5. Check SQLite Dossier directly for raw full name
     * 6. Fallback to fallbackName (if not a placeholder)
     * 7. Safe fallback to "Device" (NEVER "Node <id>")
     */
    /**
     * Resolves human-readable SSOT display name for a physical device node.
     * Priority:
     * 1. Check if Self device -> return SQLite Model name, Operator Nickname, or "You" (if allowPronoun)
     * 2. Check Gate 1 Minor / Prime Target Child -> return "Riddhi" / Prime name
     * 3. Check Circle Host Device -> return Master Guardian name (dynamic from RegistryManager)
     * 4. Check SQLite DeviceCentricName (SSOT master roster)
     * 5. Check SQLite Dossier directly for raw full name
     * 6. Check SQLite Device table for device friendly model/name
     * 7. Fallback to fallbackName (if not a placeholder)
     * 8. Safe fallback to "Device" (NEVER "Node <id>")
     */
    @JvmStatic
    @JvmOverloads
    fun resolveDisplayName(
        context: Context,
        deviceId: String?,
        fallbackName: String? = null,
        allowPronoun: Boolean = true
    ): String {
        if (deviceId.isNullOrBlank()) {
            return if (!isPlaceholderName(fallbackName, false)) fallbackName!!.trim() else DEFAULT_DEVICE_NAME
        }

        // 1. Self Device Evaluation (OOPS MVC Model-First)
        val selfUid = FirebaseAuth.getInstance().uid ?: ""
        val selfAgent = DeviceIdentityHelper.getOrGenerateAgentId(context)
        val selfCanonical = DeviceIdentityHelper.toCanonicalAgentId(selfAgent)
        val canonicalDevId = DeviceIdentityHelper.toCanonicalAgentId(deviceId)

        val isSelf = deviceId == selfUid || deviceId == selfAgent || deviceId == "SELF_USER" ||
                "You".equals(deviceId, ignoreCase = true) || canonicalDevId.equals(selfCanonical, ignoreCase = true)

        if (isSelf) {
            // First consult SQLite Model SSOT before defaulting to pronoun
            try {
                val localDb = DatabaseHelper.getInstance(context)
                val ssot = localDb.getDeviceCentricName(selfCanonical)
                if (!isPlaceholderName(ssot, false)) {
                    return if (allowPronoun) ssot.trim() else ssot.trim()
                }
                val dossier = localDb.getLocalDossier(selfCanonical)
                val dName = dossier["fullName"] ?: dossier["name"] ?: dossier["owner_name"] ?: dossier["id_full_name"]
                if (!isPlaceholderName(dName, false)) {
                    return dName!!.trim()
                }
            } catch (_: Exception) {}

            val nick = RegistryManager.getUserNickname(context)
            if (!nick.isNullOrBlank() && !isPlaceholderName(nick, true)) {
                return nick.trim()
            }
            return if (allowPronoun) "You" else (RegistryManager.getCircleHostName(context).takeIf { !isPlaceholderName(it, false) } ?: "Host Device")
        }

        // 2. Gate 1 Minor / Prime Target Priority Check (Riddhi / 4379A761)
        try {
            val primeTarget = RegistryManager.getPrimeTargetDeviceId(context)
            if (!primeTarget.isNullOrBlank() && (primeTarget.equals(deviceId, ignoreCase = true) || primeTarget.equals(canonicalDevId, ignoreCase = true))) {
                val primeName = RegistryManager.getPrimeTargetName(context)
                if (!primeName.isNullOrBlank() && !isPlaceholderName(primeName, false)) {
                    val localDb = DatabaseHelper.getInstance(context)
                    val ssot = localDb.getDeviceCentricName(deviceId)
                    if (!isPlaceholderName(ssot, false)) return ssot.trim()
                    return primeName.trim()
                }
            }
        } catch (ignored: Exception) {}

        // 3. Circle Host Device Check (Authority Master Anchor - Dynamic SSOT)
        val hostDeviceId = getCircleHostDeviceId(context)
        if (canonicalDevId.equals(hostDeviceId, ignoreCase = true) || deviceId.equals(hostDeviceId, ignoreCase = true)) {
            val localDb = DatabaseHelper.getInstance(context)
            val ssot = localDb.getDeviceCentricName(hostDeviceId)
            if (!isPlaceholderName(ssot, false)) return ssot.trim()
            val hostName = RegistryManager.getCircleHostName(context)
            if (!isPlaceholderName(hostName, false)) return hostName.trim()
            return RegistryManager.DEFAULT_CIRCLE_HOST_NAME
        }

        // 4. Local Database SSOT Master Roster Lookup
        try {
            val localDb = DatabaseHelper.getInstance(context)
            val ssot = localDb.getDeviceCentricName(deviceId)
            if (!isPlaceholderName(ssot, false)) {
                return ssot.trim()
            }
            if (!canonicalDevId.equals(deviceId, ignoreCase = true)) {
                val ssotCanon = localDb.getDeviceCentricName(canonicalDevId)
                if (!isPlaceholderName(ssotCanon, false)) {
                    return ssotCanon.trim()
                }
            }
            // Check SQLite Dossier across all candidate keys
            for (idCandidate in listOf(deviceId, canonicalDevId, canonicalDevId.removePrefix("HW_"), "HW_${canonicalDevId.removePrefix("HW_")}").distinct()) {
                val dossier = localDb.getLocalDossier(idCandidate)
                val dName = dossier["fullName"] ?: dossier["name"] ?: dossier["owner_name"] ?: dossier["id_full_name"] ?: dossier["displayName"] ?: dossier["nickname"]
                if (!isPlaceholderName(dName, false)) {
                    return dName!!.trim()
                }
                val devModel = dossier["model"] ?: dossier["device_name"] ?: dossier["device_model"]
                if (!isPlaceholderName(devModel, false)) {
                    return devModel!!.trim()
                }
            }
            // Check Device Hardware specifications for friendly device name
            val devModel = localDb.getDeviceModelOrName(deviceId)
            if (!isPlaceholderName(devModel, false)) {
                return devModel!!.trim()
            }
        } catch (ignored: Exception) {}

        // 5. Fallback Name Evaluation
        if (!isPlaceholderName(fallbackName, false)) {
            return fallbackName!!.trim()
        }

        // 6. Gate 1 Minor Safety Net
        try {
            val primeTarget = RegistryManager.getPrimeTargetDeviceId(context)
            if (!primeTarget.isNullOrBlank() && (primeTarget.equals(deviceId, ignoreCase = true) || primeTarget.equals(canonicalDevId, ignoreCase = true))) {
                val primeName = RegistryManager.getPrimeTargetName(context)
                if (!primeName.isNullOrBlank()) return primeName.trim()
            }
        } catch (ignored: Exception) {}

        // Disambiguated Fallback: Never display identical ambiguous "Device" for multiple peers!
        val shortId = canonicalDevId.removePrefix("HW_").takeLast(4).uppercase()
        return if (shortId.isNotEmpty() && shortId != "VICE") "Device $shortId" else DEFAULT_DEVICE_NAME
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

    // [Variables] context, deviceId, userLocation, observerDeviceId, db, cleanDev, cleanObs, dbRole, dossier, fRole, directRole, isHost, isSelf, famRole, friendRole | [Constants] "Master Guardian", "Co-Guardian", "Host", "Member", "None", "DEFAULT" | [Dependencies] DatabaseHelper, DeviceIdentityHelper, AvatarCalloutGenerator, RegistryManager | [Integration Points] MapActivity Floating Rail, FamilyFriendsActivity, NetworkGatewayActivity
    /**
     * Resolves social role string (e.g., "Master Guardian", "Co-Guardian", "Child", "Spouse", "Friend") prioritizing SQLite SSOT ground truth.
     * Supports Directed Relative Perspective:
     * - To Note 7 (Host/Jeet): Note 5 is "Friend", Pushpa is "Spouse", Riddhi is "Child", Innovant is "Friend".
     * - To Riddhi (Child): Note 5 is "Co-Guardian", Note 7 is "Master Guardian", Pushpa is "Parent/Spouse".
     * - To Note 5: Note 7 is "Circle Host / Friend", Pushpa is "Friend", Riddhi is "Ward / Child", Innovant is "Friend".
     * - To Pushpa: Note 7 is "Spouse", Note 5 is "Friend", Riddhi is "Child", Innovant is "Friend".
     */
    @JvmStatic
    @JvmOverloads
    fun resolveRoleString(
        context: Context,
        deviceId: String?,
        userLocation: UserLocation? = null,
        observerDeviceId: String? = null
    ): String {
        if (!deviceId.isNullOrBlank()) {
            val cleanDev = DeviceIdentityHelper.toCanonicalAgentId(deviceId)
            val selfAgent = DeviceIdentityHelper.getOrGenerateAgentId(context)
            val cleanObs = if (!observerDeviceId.isNullOrBlank()) {
                DeviceIdentityHelper.toCanonicalAgentId(observerDeviceId)
            } else {
                DeviceIdentityHelper.toCanonicalAgentId(selfAgent)
            }

            val isHost = isCircleHost(context, deviceId) || isCircleHost(context, cleanDev)
            val isObsHost = isCircleHost(context, cleanObs)
            val isSelf = isSelfDevice(context, deviceId)

            if (isHost && !isSelf) {
                // To spouse, Host is Spouse; to child, Host is Master Guardian / Father; to friend, Host is Circle Host / Friend
                if (cleanObs == "F8CAFC67") return "Spouse"
                return "Master Guardian"
            }

            // 🧬 DYNAMIC RELATIVE PAIRWISE RESOLUTION (SSOT Variable Valuebase)
            // Editable and configurable via Settings without hardcoding device IDs or roles
            val dynamicRole = RegistryManager.resolveConfiguredRelativeRole(context, cleanObs, cleanDev)
            if (!dynamicRole.isNullOrBlank()) {
                return dynamicRole
            }

            try {
                val db = DatabaseHelper.getInstance(context)
                val dbRole = db.getRoleForDevice(deviceId) ?: db.getRoleForDevice(cleanDev)
                if (!dbRole.isNullOrBlank() && !dbRole.equals("None", ignoreCase = true) && !dbRole.equals("DEFAULT", ignoreCase = true) && !dbRole.equals("Active Tracker", ignoreCase = true) && !dbRole.equals("Member", ignoreCase = true) && !dbRole.equals("Host", ignoreCase = true)) {
                    return if ((dbRole.equals("ADMIN", true) || dbRole.equals("CO_GUARDIAN", true) || dbRole.equals("CO-GUARDIAN", true)) && !isHost) "Co-Guardian" else dbRole
                }
                val dossier = db.getLocalDossier(deviceId)
                val fRole = dossier["role_family"] ?: dossier["role"]
                if (!fRole.isNullOrBlank() && !fRole.equals("None", ignoreCase = true) && !fRole.equals("DEFAULT", ignoreCase = true) && !fRole.equals("Active Tracker", ignoreCase = true) && !fRole.equals("Member", ignoreCase = true) && !fRole.equals("Host", ignoreCase = true)) {
                    return if ((fRole.equals("ADMIN", true) || fRole.equals("CO_GUARDIAN", true) || fRole.equals("CO-GUARDIAN", true)) && !isHost) "Co-Guardian" else fRole
                }
                val canonDossier = db.getLocalDossier(cleanDev)
                val cRole = canonDossier["role_family"] ?: canonDossier["role"]
                if (!cRole.isNullOrBlank() && !cRole.equals("None", ignoreCase = true) && !cRole.equals("DEFAULT", ignoreCase = true) && !cRole.equals("Active Tracker", ignoreCase = true) && !cRole.equals("Member", ignoreCase = true) && !cRole.equals("Host", ignoreCase = true)) {
                    return if ((cRole.equals("ADMIN", true) || cRole.equals("CO_GUARDIAN", true) || cRole.equals("CO-GUARDIAN", true)) && !isHost) "Co-Guardian" else cRole
                }
            } catch (ignored: Exception) {}

            if (isSelf) {
                val famRole = RegistryManager.getRoleFamily(context, "")
                if (famRole.isNotBlank() && !famRole.equals("None", ignoreCase = true) && !famRole.equals("Primary Guardian", ignoreCase = true) && !famRole.equals("Guardian", ignoreCase = true)) {
                    return famRole
                }
                val friendRole = RegistryManager.getRoleFriend(context, "")
                if (friendRole.isNotBlank() && !friendRole.equals("None", ignoreCase = true)) {
                    return friendRole
                }
                return "Member"
            }
        }
        @Suppress("DEPRECATION")
        val directRole = resolveRoleString(userLocation)
        if (directRole.isNotBlank() && !directRole.equals("None", ignoreCase = true) && !directRole.equals("DEFAULT", ignoreCase = true) && !directRole.equals("Active Tracker", ignoreCase = true) && !directRole.equals("Host", ignoreCase = true)) {
            return directRole
        }
        return "Member"
    }

    /**
     * Helper to determine if target deviceId represents the local Host operator.
     */
    @JvmStatic
    fun isSelfDevice(context: Context, deviceId: String?): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val selfUid = FirebaseAuth.getInstance().uid ?: ""
        val selfAgent = DeviceIdentityHelper.getOrGenerateAgentId(context)
        val selfCanonical = DeviceIdentityHelper.toCanonicalAgentId(selfAgent)
        val canonicalDevId = DeviceIdentityHelper.toCanonicalAgentId(deviceId)
        return deviceId == selfUid || deviceId == selfAgent || deviceId == "SELF_USER" ||
                "You".equals(deviceId, ignoreCase = true) || canonicalDevId.equals(selfCanonical, ignoreCase = true)
    }

    /**
     * Resolves standard tactical accent color with SQLite SSOT ground truth fallback.
     */
    @JvmStatic
    @JvmOverloads
    @ColorInt
    fun resolveRoleColor(context: Context, deviceId: String?, userLocation: UserLocation? = null): Int {
        val role = resolveRoleString(context, deviceId, userLocation)
        val isSelf = isSelfDevice(context, deviceId)
        return AvatarCalloutGenerator.getRoleColor(role, isSelf)
    }

    /**
     * Resolves social role string (e.g., "Child", "Guardian", "Friend") from raw UserLocation.
     * Deprecated: Use resolveRoleString(context, deviceId, userLocation) for SQLite SSOT ground truth.
     */
    @Deprecated("Use context-aware resolveRoleString(context, deviceId, userLocation) for SQLite SSOT ground truth.")
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
     * Resolves role bracket tag (e.g. " [Child]") with SQLite SSOT ground truth.
     */
    @JvmStatic
    @JvmOverloads
    fun resolveRoleTag(context: Context, deviceId: String?, userLocation: UserLocation? = null): String {
        val role = resolveRoleString(context, deviceId, userLocation)
        return if (role.isNotBlank() && !role.equals("Member", ignoreCase = true) && !role.equals("None", ignoreCase = true)) " [$role]" else ""
    }

    /**
     * Resolves role bracket tag (e.g. " [Child]").
     * Deprecated: Use context-aware resolveRoleTag(context, deviceId, userLocation) for SQLite SSOT ground truth.
     */
    @Deprecated("Use context-aware resolveRoleTag(context, deviceId, userLocation) for SQLite SSOT ground truth.")
    @JvmStatic
    fun resolveRoleTag(userLocation: UserLocation?): String {
        @Suppress("DEPRECATION")
        val role = resolveRoleString(userLocation)
        return if (role.isNotEmpty()) " [$role]" else ""
    }

    /**
     * Resolves standard tactical accent color for the assigned role.
     * Deprecated: Use resolveRoleColor(context, deviceId, userLocation).
     */
    @Deprecated("Use context-aware resolveRoleColor(context, deviceId, userLocation).")
    @JvmStatic
    @ColorInt
    fun resolveRoleColor(userLocation: UserLocation?): Int {
        @Suppress("DEPRECATION")
        val role = resolveRoleString(userLocation)
        return AvatarCalloutGenerator.getRoleColor(role, false)
    }
}
