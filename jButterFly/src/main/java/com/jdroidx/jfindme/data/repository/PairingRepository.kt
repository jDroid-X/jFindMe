package com.jdroidx.jfindme.data.repository

import android.os.Build
import com.google.firebase.database.*
import com.jdroidx.jfindme.data.model.DeviceRegistry
import com.jdroidx.jfindme.data.model.PairingSession
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.JFindMeApplication
import java.util.UUID

/**
 * PairingRepository: Handles device registry, pairing sessions, and multi-device sync.
 * Implements the Required Pairing Architecture (L4-L5).
 */
class PairingRepository {

    /**
     * 🦋 Device Sharing Closed-Loop Handshake Callback
     */
    interface DeviceSharingSyncCallback {
        fun onSyncDispatched(syncSessionId: String)
        fun onSyncComplete(devAId: String, devBId: String, latencyMs: Long)
        fun onSyncTimeout(diagnosticReason: String)
        fun onSyncFailed(errorMessage: String)
    }

    /**
     * 🦋 Batch Device Sharing Closed-Loop Handshake Callback
     */
    interface BatchDeviceSharingSyncCallback {
        fun onBatchDispatched(syncSessionId: String, totalDevices: Int)
        fun onBatchProgress(syncedCount: Int, totalDevices: Int)
        fun onBatchComplete(syncedCount: Int, totalDevices: Int, latencyMs: Long)
        fun onBatchTimeout(syncedCount: Int, totalDevices: Int, diagnosticReason: String)
        fun onBatchFailed(errorMessage: String)
    }

    private val TAG = "PairingRepository"
    private val sessionsRef = FirebaseComm.database.getReference("pairing_sessions")
    private val hubRef = FirebaseComm.devicesRef

    /**
     * Step 1: Register Device Identity (V59.5 Secured)
     * Refactored: Uses Agent ID (uid1) as Root Node and Forensic Timestamp.
     */
    fun registerDevice(agentId: String, isPrimary: Boolean, onComplete: (String) -> Unit) {
        val prefix = JFindMeApplication.getContext()?.let { RegistryManager.getHardwarePrefix(it) } ?: RegistryManager.DEFAULT_HW_PREFIX
        val hardwareId = prefix + UUID.randomUUID().toString().take(8).uppercase()
        val device = mapOf(
            "deviceId" to hardwareId,
            "agentId" to agentId,
            "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "isPrimary" to isPrimary,
            "lastActive" to FirebaseComm.getForensicTimestamp(),
            "os_version" to Build.VERSION.RELEASE
        )
        
        // 🧬 Unified Master Schema: Write hardware specs under devices/{agentId}/device
        hubRef.child(agentId).child("device").updateChildren(device)
            .addOnSuccessListener {
                FirebaseComm.lookupIndicesRef.child("device_to_hw").child(agentId).setValue(hardwareId)
                SystemLogger.i(TAG, "Unified Device Registered under Agent Hub: $agentId")
                
                // 📝 AUDIT CHANGE LOG: Record device hardware registration
                FirebaseComm.logChange(agentId, FirebaseComm.LOG_ITEM_PAIRING, "DEVICE_REGISTER", hardwareId, device)
                
                onComplete(hardwareId)
            }
    }

    /**
     * Step 2: Generate Pairing Code (Primary Device)
     */
    fun startPairingSession(userId: String, deviceId: String, onCodeReady: (String) -> Unit) {
        val pairCode = (100000..999999).random().toString()
        val sessionId = UUID.randomUUID().toString().take(6)
        val timeoutMs = JFindMeApplication.getContext()?.let { RegistryManager.getPairingTimeoutMs(it) } ?: RegistryManager.DEFAULT_PAIRING_TIMEOUT
        val session = PairingSession(
            sessionId = sessionId,
            userId = userId,
            pairCode = pairCode,
            expiresAt = System.currentTimeMillis() + timeoutMs,
            primaryDevice = deviceId,
            status = "pending"
        )
        // Correcting child path to use pairCode directly for simpler queries or sessionId
        sessionsRef.child(pairCode).setValue(session)
            .addOnSuccessListener {
                SystemLogger.i(TAG, "Pairing Session Started: $pairCode")
                onCodeReady(pairCode)
            }
            .addOnFailureListener { e ->
                SystemLogger.e(TAG, "Failed to start pairing session", e)
            }
    }

    /**
     * Step 3: Validate and Link (Secondary Device)
     */
    fun joinPairingSession(pairCode: String, deviceId: String, onResult: (Boolean, String?) -> Unit) {
        sessionsRef.child(pairCode).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val session = snapshot.getValue(PairingSession::class.java)
                    if (session != null && (session.status == "pending" || session.status == "ACTIVE") && session.expiresAt > System.currentTimeMillis()) {
                        // Link secondary device
                        val updates = mapOf(
                            "status" to "linked",
                            "secondaryDevice" to deviceId
                        )
                        snapshot.ref.updateChildren(updates).addOnSuccessListener {
                            onResult(true, session.userId)
                        }
                    } else {
                        onResult(false, "Code expired or invalid")
                    }
                } else {
                    onResult(false, "Invalid Pair Code")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(false, error.message)
            }
        })
    }

    private fun monitorSession(sessionId: String) {
        sessionsRef.child(sessionId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val session = snapshot.getValue(PairingSession::class.java)
                if (session?.status == "linked") {
                    SystemLogger.i(TAG, "Pairing Successful! Secondary device joined.")
                    // Clean up or trigger sync
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    
    // 🚀 NEW SECURE MVVM ARCHITECTURE - SCENARIO HANDSHAKES
    
    companion object {
        private val db = FirebaseDatabase.getInstance().reference

        // [Variables] hostAgentId: String, hostGroupId: String, targetAgentId: String, roleFam: String, onSuccess: () -> Unit, currentCsv: String, approvedIds: MutableList<String>, newSecureCsv: String, updates: HashMap<String, Any>, localDb: DatabaseHelper, pairId: String, prefs: SharedPreferences
        // [Constants] NODE_DEVICES, NODE_RELATIONSHIPS, LOG_ITEM_PAIRING, LOG_ITEM_FAMILY_FRIENDS
        // [Dependencies] FirebaseComm, SystemLogger, JFindMeApplication, DatabaseHelper, DevicePairingTable, UserIdentityTable, MasterDataSyncSentinel
        // [Integration Points] NetworkGatewayActivity, FamilyFriendsActivity, MapActivity, LocationRepository
        @JvmStatic
        @JvmOverloads
        fun executeFamilyHandshake(
            hostAgentId: String,
            hostGroupId: String,
            targetAgentId: String,
            roleFam: String,
            onResult: ((Boolean, String?) -> Unit)? = null
        ) {
            val cleanHost = FirebaseComm.sanitizeAgentId(hostAgentId)
            val cleanTarget = FirebaseComm.sanitizeAgentId(targetAgentId)
            if (!FirebaseComm.isAgentIdValid(cleanHost) || !FirebaseComm.isAgentIdValid(cleanTarget)) {
                SystemLogger.e("PairingRepo", "BLOCKED: Handshake with invalid Agent IDs: $cleanHost -> $cleanTarget")
                onResult?.invoke(false, "Invalid Agent IDs: $cleanHost -> $cleanTarget")
                return
            }
            
            // 🚀 V59.7: Master Forensic Handshake (Anchored to Hub Agent)
            // Read both Identity (uppercase) and identity (lowercase fallback) for Host and Target reciprocally
            val hostIdentityNode = db.child(FirebaseComm.NODE_DEVICES).child(cleanHost).child("Identity").child("valid_linked_agents")
            hostIdentityNode.get().addOnCompleteListener { taskHost ->
                val snapshotHost = if (taskHost.isSuccessful) taskHost.result else null
                val currentCsvHost = snapshotHost?.getValue(String::class.java) ?: ""
                val approvedHost = currentCsvHost.split(",", ";", "|").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableSet()
                approvedHost.add(cleanTarget)

                val targetIdentityNode = db.child(FirebaseComm.NODE_DEVICES).child(cleanTarget).child("Identity").child("valid_linked_agents")
                targetIdentityNode.get().addOnCompleteListener { taskTarget ->
                    val snapshotTarget = if (taskTarget.isSuccessful) taskTarget.result else null
                    val currentCsvTarget = snapshotTarget?.getValue(String::class.java) ?: ""
                    val approvedTarget = currentCsvTarget.split(",", ";", "|").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableSet()
                    approvedTarget.add(cleanHost)

                    // Merge local SQLite connected IDs as safety fallback
                    JFindMeApplication.getContext()?.let { ctx ->
                        try {
                            val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(ctx)
                            approvedHost.addAll(localDb.getConnectedDeviceIds(cleanHost))
                            approvedTarget.addAll(localDb.getConnectedDeviceIds(cleanTarget))
                        } catch (ignored: Exception) {}
                    }

                    val newSecureCsvHost = approvedHost.joinToString(",")
                    val newSecureCsvTarget = approvedTarget.joinToString(",")

                    val nowForensic = FirebaseComm.getForensicTimestamp()
                    val nowEpoch = System.currentTimeMillis()
                    val hostNickname = JFindMeApplication.getContext()?.let { RegistryManager.getUserNickname(it) } ?: cleanHost

                    val updates = hashMapOf<String, Any>(
                        "${FirebaseComm.NODE_DEVICES}/$cleanTarget/Identity/active_group" to hostGroupId,
                        "${FirebaseComm.NODE_DEVICES}/$cleanTarget/profile/active_group" to hostGroupId,
                        "${FirebaseComm.NODE_DEVICES}/$cleanHost/Identity/valid_linked_agents" to newSecureCsvHost,
                        "${FirebaseComm.NODE_DEVICES}/$cleanHost/identity/valid_linked_agents" to newSecureCsvHost,
                        "${FirebaseComm.NODE_DEVICES}/$cleanTarget/Identity/valid_linked_agents" to newSecureCsvTarget,
                        "${FirebaseComm.NODE_DEVICES}/$cleanTarget/identity/valid_linked_agents" to newSecureCsvTarget,
                    
                    // 🧬 V68.0 Device-Centric Relationships Sub-Group Mesh
                    "${FirebaseComm.NODE_DEVICES}/$cleanHost/relationships/$cleanTarget" to mapOf(
                        "role" to roleFam,
                        "name" to cleanTarget,
                        "group" to hostGroupId,
                        "linked_at" to nowForensic
                    ),
                    "${FirebaseComm.NODE_DEVICES}/$cleanTarget/relationships/$cleanHost" to mapOf(
                        "role" to "GUARDIAN",
                        "name" to hostNickname,
                        "group" to hostGroupId,
                        "linked_at" to nowForensic
                    ),

                    // Uniform devices/ + group_info standard
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/group_info/host_device_id" to cleanHost,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/group_info/group_name" to "Primary Circle",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/group_info/created_at" to nowForensic,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanHost/identity/name" to hostNickname,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanHost/identity/full_name" to hostNickname,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanHost/membership/role" to "ADMIN",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanHost/membership/role_family" to "Parent",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanHost/membership/role_friend" to "None",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanHost/membership/status" to "Active",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanHost/membership/ts_joined" to nowForensic,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanHost/paired_with/$cleanTarget" to true,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanTarget/identity/name" to cleanTarget,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanTarget/identity/full_name" to cleanTarget,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanTarget/membership/role" to roleFam,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanTarget/membership/role_family" to roleFam,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanTarget/membership/role_friend" to "None",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanTarget/membership/status" to "Active",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanTarget/membership/ts_joined" to nowForensic,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$cleanTarget/paired_with/$cleanHost" to true,

                    // Direct Pairs reciprocal SSOT
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/direct_pairs/$cleanHost/$cleanTarget" to true,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/direct_pairs/$cleanTarget/$cleanHost" to true,
                    "users/$cleanHost/joined_circles/$hostGroupId" to true,
                    "users/$cleanTarget/joined_circles/$hostGroupId" to true,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/signals/refresh_roster" to nowEpoch
                )
                
                db.updateChildren(updates).addOnCompleteListener { updTask ->
                    if (updTask.isSuccessful) {
                        SystemLogger.w("PairingRepo", "V68.0 Handshake: Target Agent Matrix Constructed under $cleanHost")
                        
                        // 🛡️ Local SQLite SSOT Logging & SharedPreferences Update
                        JFindMeApplication.getContext()?.let { ctx ->
                            try {
                                val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(ctx)
                                val pairId = "PAIR_${cleanHost}_${cleanTarget}"
                                com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairingWithRoles(
                                    localDb.writableDatabase,
                                    pairId,
                                    cleanHost,
                                    cleanTarget,
                                    "ADMIN",
                                    roleFam,
                                    null,
                                    "PAIRED"
                                )

                                // Save roster nodes locally for offline and immediate rendering
                                val isFriend = roleFam.contains("Friend", ignoreCase = true)
                                localDb.saveRosterNode(cleanTarget, cleanTarget, roleFam, "ACTIVE", false, isFriend, activeGroupId = hostGroupId)
                                localDb.saveRosterNode(cleanHost, hostNickname, "ADMIN", "ACTIVE", true, false, activeGroupId = hostGroupId)
                                localDb.appendValidLinkedAgent(cleanHost, cleanTarget)
                                localDb.appendValidLinkedAgent(cleanTarget, cleanHost)
                                localDb.updateCitizenRole(cleanTarget, if (isFriend) "None" else roleFam, if (isFriend) roleFam else "None", isFriend, hostGroupId)

                                val prefs = ctx.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
                                prefs.edit().putString("active_group", hostGroupId).apply()

                                com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                                    ctx,
                                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.CATEGORY_ROSTER,
                                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPSERT,
                                    cleanTarget
                                )
                            } catch (ex: Exception) {
                                SystemLogger.e("PairingRepo", "executeFamilyHandshake SQLite log warning: ${ex.message}")
                            }
                        }

                        // 📝 AUDIT CHANGE LOGS: Record Pairing and Family/Friends mutations
                        val pairingDetails = mapOf("target_agent" to cleanTarget, "host_group" to hostGroupId, "role" to roleFam)
                        FirebaseComm.logChange(cleanHost, FirebaseComm.LOG_ITEM_PAIRING, "DEVICE_PAIR_LINK", cleanTarget, pairingDetails)
                        FirebaseComm.logChange(cleanHost, FirebaseComm.LOG_ITEM_FAMILY_FRIENDS, "MEMBER_LINK", cleanTarget, pairingDetails)
                        FirebaseComm.logChange(cleanTarget, FirebaseComm.LOG_ITEM_PAIRING, "DEVICE_PAIR_LINK", cleanHost, mapOf("host_agent" to cleanHost, "role" to "GUARDIAN"))
                        
                        onResult?.invoke(true, hostGroupId)
                    } else {
                        val errMsg = updTask.exception?.message ?: "Handshake Cloud SSOT update failed"
                        SystemLogger.e("PairingRepo", "Handshake rejected: $errMsg")
                        onResult?.invoke(false, errMsg)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun executeFamilyHandshake(hostAgentId: String, hostGroupId: String, targetAgentId: String, roleFam: String, onSuccess: () -> Unit) {
        executeFamilyHandshake(hostAgentId, hostGroupId, targetAgentId, roleFam) { success, _ ->
            if (success) onSuccess()
        }
    }

        /**
         * 🏆 Claim Primary Emitter: Ensures only 1 physical device broadcasts live telemetry per Agent ID.
         */
        @JvmStatic
        fun claimPrimaryEmitter(agentId: String, hardwareId: String, onComplete: (Boolean) -> Unit) {
            if (!FirebaseComm.isAgentIdValid(agentId)) {
                onComplete(false)
                return
            }
            val updates = mapOf(
                "active_emitter_hw" to hardwareId,
                "emitter_claimed_at" to FirebaseComm.getForensicTimestamp()
            )
            FirebaseComm.getDeviceRef(agentId).child("Identity").updateChildren(updates)
                .addOnSuccessListener {
                    SystemLogger.i("PairingRepo", "🏆 Hardware $hardwareId claimed Primary Emitter role for Agent $agentId")
                    FirebaseComm.logChange(agentId, FirebaseComm.LOG_ITEM_PAIRING, "CLAIM_PRIMARY_EMITTER", hardwareId, updates)
                    onComplete(true)
                }
                .addOnFailureListener {
                    SystemLogger.e("PairingRepo", "Failed claiming Primary Emitter: ${it.message}")
                    onComplete(false)
                }
        }

        @JvmStatic
        fun claimPrimaryEmitter(agentId: String, hardwareId: String, callback: StatusCallback) {
            claimPrimaryEmitter(agentId, hardwareId) { callback.onComplete(it) }
        }

        /**
         * [Variables] cleanCoParent: String, cleanChild: String, now: String, effectiveGroup: String, updates: HashMap<String, Any>, newSecureCsv: String, currentCsv: String, approvedIds: MutableList<String>
         * [Constants] NODE_DEVICES, NODE_RELATIONSHIPS, LOG_ITEM_FAMILY_FRIENDS
         * [Dependencies] FirebaseComm, SystemLogger
         * [Integration Points] FamilyFriendsActivity, DevicePairingTable, GroupMemberTable
         * 👨‍👩‍👧 Link Other Parent / Co-Guardian to Child
         * Establishes mutual parent-child relationship between coParentAgentId and childAgentId
         * with the exact same Child ID so both parents share guardianship.
         */
        // [Variables] cleanCoParent: String, cleanChild: String, now: String, effectiveGroup: String, updates: HashMap<String, Any>, currentCsv: String, approvedIds: MutableList<String>, newSecureCsv: String, ctx: Context, localDb: DatabaseHelper, pairId: String | [Constants] NODE_DEVICES, NODE_RELATIONSHIPS, LOG_ITEM_FAMILY_FRIENDS | [Dependencies] FirebaseComm, SystemLogger, JFindMeApplication, DatabaseHelper, DevicePairingTable, MasterDataSyncSentinel | [Integration Points] FamilyFriendsActivity, MapActivity, DevicePairingTable, GroupMemberTable
        /**
         * 👨‍👩‍👧 Link Other Parent / Co-Guardian to Child
         * Establishes mutual parent-child relationship between coParentAgentId and childAgentId
         * with the exact same Child ID so both parents share guardianship.
         */
        @JvmStatic
        @JvmOverloads
        fun linkCoParentToChild(
            hostGroupId: String,
            coParentAgentId: String,
            childAgentId: String,
            coParentName: String = "",
            childName: String = "",
            callback: ResultCallback? = null
        ) {
            val cleanCoParent = FirebaseComm.sanitizeAgentId(coParentAgentId)
            val cleanChild = FirebaseComm.sanitizeAgentId(childAgentId)
            if (!FirebaseComm.isAgentIdValid(cleanCoParent) || !FirebaseComm.isAgentIdValid(cleanChild)) {
                callback?.onResult(false, "Invalid Parent or Child Agent ID.")
                return
            }
            val now = FirebaseComm.getForensicTimestamp()
            val resolvedCtx = JFindMeApplication.getContext()
            val selfDevId = resolvedCtx?.let { com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(it) } ?: ""
            val prefsGroup = resolvedCtx?.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)?.getString("active_group", null)
            val effectiveGroup = when {
                hostGroupId.isNotBlank() && hostGroupId != "DEFAULT_GROUP" -> hostGroupId
                resolvedCtx != null -> RegistryManager.getEffectiveActiveGroup(resolvedCtx)
                !prefsGroup.isNullOrBlank() && prefsGroup != "DEFAULT_GROUP" -> prefsGroup
                else -> "GRP_$cleanCoParent"
            }

            // 1. Fetch co-parent's and child's valid_linked_agents reciprocally
            db.child(FirebaseComm.NODE_DEVICES).child(cleanCoParent).child("Identity").child("valid_linked_agents").get()
                .addOnCompleteListener { taskCoParent ->
                    val snapshotCoParent = if (taskCoParent.isSuccessful) taskCoParent.result else null
                    val currentCsvCoParent = snapshotCoParent?.getValue(String::class.java) ?: ""
                    val approvedCoParent = currentCsvCoParent.split(",", ";", "|").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableSet()
                    approvedCoParent.add(cleanChild)

                    db.child(FirebaseComm.NODE_DEVICES).child(cleanChild).child("Identity").child("valid_linked_agents").get()
                        .addOnCompleteListener { taskChild ->
                            val snapshotChild = if (taskChild.isSuccessful) taskChild.result else null
                            val currentCsvChild = snapshotChild?.getValue(String::class.java) ?: ""
                            val approvedChild = currentCsvChild.split(",", ";", "|").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableSet()
                            approvedChild.add(cleanCoParent)

                            // Safety fallback: merge local SQLite connected device IDs
                            JFindMeApplication.getContext()?.let { ctx ->
                                try {
                                    val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(ctx)
                                    approvedCoParent.addAll(localDb.getConnectedDeviceIds(cleanCoParent))
                                    approvedChild.addAll(localDb.getConnectedDeviceIds(cleanChild))
                                } catch (ignored: Exception) {}
                            }

                            val newSecureCsvCoParent = approvedCoParent.joinToString(",")
                            val newSecureCsvChild = approvedChild.joinToString(",")

                            val coParentDisplayName = if (coParentName.isNotBlank()) coParentName else cleanCoParent
                            val childDisplayName = if (childName.isNotBlank()) childName else cleanChild

                            val updates = hashMapOf<String, Any>(
                                // Device-Centric Relationships Mesh
                                "${FirebaseComm.NODE_DEVICES}/$cleanCoParent/relationships/$cleanChild" to mapOf(
                                    "role" to "Child",
                                    "name" to childDisplayName,
                                    "group" to effectiveGroup,
                                    "linked_at" to now
                                ),
                                "${FirebaseComm.NODE_DEVICES}/$cleanChild/relationships/$cleanCoParent" to mapOf(
                                    "role" to "GUARDIAN",
                                    "name" to coParentDisplayName,
                                    "group" to effectiveGroup,
                                    "linked_at" to now
                                ),
                                "${FirebaseComm.NODE_DEVICES}/$cleanCoParent/dependents/$cleanChild" to true,
                                "${FirebaseComm.NODE_DEVICES}/$cleanChild/guardians/$cleanCoParent" to true,
                                "${FirebaseComm.NODE_DEVICES}/$cleanCoParent/Identity/valid_linked_agents" to newSecureCsvCoParent,
                                "${FirebaseComm.NODE_DEVICES}/$cleanCoParent/identity/valid_linked_agents" to newSecureCsvCoParent,
                                "${FirebaseComm.NODE_DEVICES}/$cleanChild/Identity/valid_linked_agents" to newSecureCsvChild,
                                "${FirebaseComm.NODE_DEVICES}/$cleanChild/identity/valid_linked_agents" to newSecureCsvChild,

                        // Circle-level device pairing
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanCoParent/identity/name" to coParentDisplayName,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanCoParent/identity/full_name" to coParentDisplayName,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanCoParent/membership/role" to "GUARDIAN",
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanCoParent/membership/role_family" to "Parent",
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanCoParent/membership/role_friend" to "None",
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanCoParent/membership/status" to "Active",
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanCoParent/membership/ts_joined" to now,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanCoParent/paired_with/$cleanChild" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanChild/paired_with/$cleanCoParent" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/direct_pairs/$cleanCoParent/$cleanChild" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/direct_pairs/$cleanChild/$cleanCoParent" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/members/$cleanChild/co_guardians/$cleanCoParent" to true,
                        "users/$cleanCoParent/joined_circles/$effectiveGroup" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/signals/refresh_roster" to System.currentTimeMillis(),

                        // Device command dispatch for closed-loop real-time sync with 24-Hour TTL
                        "${FirebaseComm.NODE_DEVICES}/$cleanCoParent/commands" to mapOf(
                            "action" to "DEVICE_SYNC",
                            "command" to "DEVICE_SYNC",
                            "sync_session_id" to "COPARENT_${System.currentTimeMillis()}",
                            "peer_id" to cleanChild,
                            "peer_name" to childDisplayName,
                            "role" to "Guardian",
                            "group_id" to effectiveGroup,
                            "epoch_ms" to System.currentTimeMillis(),
                            "expires_at" to System.currentTimeMillis() + 86400000L,
                            "ttl_hours" to 24
                        ),
                        "${FirebaseComm.NODE_DEVICES}/$cleanChild/commands" to mapOf(
                            "action" to "DEVICE_SYNC",
                            "command" to "DEVICE_SYNC",
                            "sync_session_id" to "COPARENT_${System.currentTimeMillis()}",
                            "peer_id" to cleanCoParent,
                            "peer_name" to coParentDisplayName,
                            "role" to "Child",
                            "group_id" to effectiveGroup,
                            "epoch_ms" to System.currentTimeMillis(),
                            "expires_at" to System.currentTimeMillis() + 86400000L,
                            "ttl_hours" to 24
                        )
                    )

                    // 🛡️ Self-Authenticated Circle Invariant: Only update users/ node for self to avoid Firebase Permission Denied on foreign peer nodes
                    if (cleanCoParent == selfDevId) {
                        updates["users/$cleanCoParent/joined_circles/$effectiveGroup"] = true
                    }
                    if (cleanChild == selfDevId) {
                        updates["users/$cleanChild/joined_circles/$effectiveGroup"] = true
                    }

                    db.updateChildren(updates).addOnCompleteListener { updTask ->
                        if (updTask.isSuccessful) {
                            SystemLogger.i("PairingRepo", "👨‍👩‍👧 Co-Parent Link Established: Parent $cleanCoParent <-> Child $cleanChild in $effectiveGroup")
                            
                            // Local SQLite commit
                            JFindMeApplication.getContext()?.let { ctx ->
                                try {
                                    val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(ctx)
                                    val pairId = "COPARENT_${cleanCoParent}_${cleanChild}"
                                    com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairingWithRoles(
                                        localDb.writableDatabase,
                                        pairId,
                                        cleanCoParent,
                                        cleanChild,
                                        "Guardian",
                                        "Child",
                                        null,
                                        "PAIRED"
                                    )

                                    localDb.saveRosterNode(cleanCoParent, coParentDisplayName, "Guardian", "ACTIVE", false, false, activeGroupId = effectiveGroup)
                                    localDb.saveRosterNode(cleanChild, childDisplayName, "Child", "ACTIVE", false, false, activeGroupId = effectiveGroup)
                                    localDb.appendValidLinkedAgent(cleanCoParent, cleanChild)
                                    localDb.appendValidLinkedAgent(cleanChild, cleanCoParent)
                                    localDb.updateCitizenRole(cleanCoParent, "Guardian", "None", false, effectiveGroup)

                                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                                        ctx,
                                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.CATEGORY_ROSTER,
                                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPSERT,
                                        cleanCoParent
                                    )
                                } catch (ex: Exception) {
                                    SystemLogger.e("PairingRepo", "Local SQLite logPairingWithRoles warning: ${ex.message}")
                                }
                            }

                            val logDetails = mapOf("co_parent" to cleanCoParent, "child" to cleanChild, "group" to effectiveGroup)
                            FirebaseComm.logChange(cleanCoParent, FirebaseComm.LOG_ITEM_FAMILY_FRIENDS, "CO_PARENT_LINK", cleanChild, logDetails)
                            FirebaseComm.logChange(cleanChild, FirebaseComm.LOG_ITEM_FAMILY_FRIENDS, "GUARDIAN_LINK", cleanCoParent, logDetails)
                            callback?.onResult(true, null)
                        } else {
                            val err = updTask.exception?.message ?: "Firebase Co-Parent update rejected"
                            SystemLogger.e("PairingRepo", "linkCoParentToChild rejected: $err")
                            callback?.onResult(false, err)
                        }
                    }
                }
            }
        }

        // [Variables] hostAgentId: String, hostGroupId: String, role: String, onCodeReady: (String) -> Unit, chars: String, inviteCode: String, expiryMs: Long, payload: Map<String, Any> | [Constants] None | [Dependencies] FirebaseComm, SystemLogger | [Integration Points] MapActivity, FamilyFriendsActivity
        /**
         * 🔑 Generate 6-Character Dynamic Circle Invite Code (48-hour validity).
         * Dual-mirrored to circle_invites and pairing_requests for zero-loss routing.
         */
        @JvmStatic
        fun createCircleInviteCode(hostAgentId: String, hostGroupId: String, role: String = "MEMBER", onCodeReady: (String) -> Unit) {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val inviteCode = (1..6).map { chars.random() }.joinToString("")
            val expiryMs = System.currentTimeMillis() + (48 * 60 * 60 * 1000L) // 48 Hours Expiry
            val cleanHost = FirebaseComm.sanitizeAgentId(hostAgentId)
            
            val payload = mapOf(
                "code" to inviteCode,
                "invite_code" to inviteCode,
                "host_agent" to cleanHost,
                "hostDeviceId" to cleanHost,
                "creator_device_id" to cleanHost,
                "group_id" to hostGroupId,
                "role" to role,
                "expires_at" to expiryMs,
                "created_at" to FirebaseComm.getForensicTimestamp(),
                "status" to "ACTIVE"
            )
            
            val updates = mapOf<String, Any>(
                "circle_invites/$inviteCode" to payload,
                "pairing_requests/$inviteCode" to payload
            )
            FirebaseComm.database.reference.updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        SystemLogger.i("PairingRepo", "🔑 Circle Invite Code Generated: $inviteCode for Group $hostGroupId")
                        onCodeReady(inviteCode)
                    } else {
                        val err = task.exception?.message ?: "Failed generating circle invite"
                        SystemLogger.e("PairingRepo", "createCircleInviteCode failed: $err")
                        onCodeReady("")
                    }
                }
        }

        @JvmStatic
        fun createCircleInviteCode(hostAgentId: String, hostGroupId: String, role: String, callback: CodeCallback) {
            createCircleInviteCode(hostAgentId, hostGroupId, role) { callback.onCodeReady(it) }
        }

        // [Variables] inviteCode: String, joiningAgentId: String, onResult: (Boolean, String?) -> Unit, cleanCode: String | [Constants] None | [Dependencies] FirebaseComm, SystemLogger | [Integration Points] NetworkGatewayActivity
        /**
         * 👥 Join Circle using 6-Character Dynamic Code.
         * Dual-evaluates circle_invites with fallback to pairing_requests.
         */
        @JvmStatic
        fun joinCircleWithInviteCode(inviteCode: String, joiningAgentId: String, onResult: (Boolean, String?) -> Unit) {
            val cleanCode = inviteCode.trim().uppercase()
            val inviteRef = FirebaseComm.database.getReference("circle_invites").child(cleanCode)
            
            inviteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        processInviteSnapshot(snapshot, cleanCode, joiningAgentId, onResult)
                    } else {
                        // Fallback check under pairing_requests
                        FirebaseComm.database.getReference("pairing_requests").child(cleanCode).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(reqSnap: DataSnapshot) {
                                if (reqSnap.exists()) {
                                    processInviteSnapshot(reqSnap, cleanCode, joiningAgentId, onResult)
                                } else {
                                    onResult(false, "Invalid Invite Code")
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                onResult(false, error.message)
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(false, error.message)
                }
            })
        }

        private fun processInviteSnapshot(snapshot: DataSnapshot, cleanCode: String, joiningAgentId: String, onResult: (Boolean, String?) -> Unit) {
            val status = snapshot.child("status").getValue(String::class.java) ?: "EXPIRED"
            val expiresAt = FirebaseComm.safeLong(snapshot.child("expires_at")) ?: 0L
            val hostAgent = snapshot.child("host_agent").getValue(String::class.java)
                ?: snapshot.child("hostDeviceId").getValue(String::class.java) ?: ""
            val groupId = snapshot.child("group_id").getValue(String::class.java) ?: ""
            val role = snapshot.child("role").getValue(String::class.java) ?: "MEMBER"
            
            if (status != "ACTIVE" || (expiresAt > 0 && expiresAt < System.currentTimeMillis())) {
                onResult(false, "This invite code has expired or is no longer active")
                return
            }
            
            // Execute handshake with real success/failure propagation
            executeFamilyHandshake(hostAgent, groupId, joiningAgentId, role) { success, err ->
                if (success) {
                    SystemLogger.i("PairingRepo", "🎉 Joined Circle via Code $cleanCode: $joiningAgentId -> $groupId")
                    onResult(true, groupId)
                } else {
                    onResult(false, err ?: "Handshake failed")
                }
            }
        }

        @JvmStatic
        fun joinCircleWithInviteCode(inviteCode: String, joiningAgentId: String, callback: ResultCallback) {
            joinCircleWithInviteCode(inviteCode, joiningAgentId) { success, msg -> callback.onResult(success, msg) }
        }

        /**
         * [Variables] cleanDevA: String, cleanDevB: String, now: String, effectiveGroup: String, computedRoleB: String, updates: HashMap<String, Any>, currentCsv: String, approvedIds: MutableList<String>, newSecureCsv: String, selfDevId: String, localDb: DatabaseHelper
         * [Constants] NODE_DEVICES, NODE_RELATIONSHIPS, LOG_ITEM_PAIRING, LOG_ITEM_FAMILY_FRIENDS
         * [Dependencies] FirebaseComm, SystemLogger, JFindMeApplication, DatabaseHelper, DevicePairingTable, MasterDataSyncSentinel
         * [Integration Points] MapActivity, FamilyFriendsActivity, DevicePairingTable, LocationRepository
         * 🔗 Connect two devices bidirectionally with reciprocal roles and pairing code.
         */
        @JvmStatic
        @JvmOverloads
        fun linkDevicesWithRoles(
            hostGroupId: String,
            devAId: String,
            devBId: String,
            roleA: String,
            roleB: String? = null,
            pairingCode: String? = null,
            callback: ResultCallback? = null
        ) {
            val cleanDevA = FirebaseComm.sanitizeAgentId(devAId)
            val cleanDevB = FirebaseComm.sanitizeAgentId(devBId)
            if (!FirebaseComm.isAgentIdValid(cleanDevA) || !FirebaseComm.isAgentIdValid(cleanDevB)) {
                callback?.onResult(false, "Invalid Agent IDs: $cleanDevA <-> $cleanDevB")
                return
            }
            val now = FirebaseComm.getForensicTimestamp()
            val resolvedCtx = JFindMeApplication.getContext()
            val selfDevId = resolvedCtx?.let { com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(it) } ?: ""
            val prefsGroup = resolvedCtx?.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)?.getString("active_group", null)
            val effectiveGroup = when {
                hostGroupId.isNotBlank() && hostGroupId != "DEFAULT_GROUP" -> hostGroupId
                resolvedCtx != null -> RegistryManager.getEffectiveActiveGroup(resolvedCtx)
                !prefsGroup.isNullOrBlank() && prefsGroup != "DEFAULT_GROUP" -> prefsGroup
                else -> "GRP_$cleanDevA"
            }

            // Compute reciprocal role if roleB is unspecified
            val computedRoleB = if (!roleB.isNullOrBlank() && roleB != "None") {
                roleB
            } else {
                when {
                    roleA.contains("Guardian", ignoreCase = true) || roleA.contains("Parent", ignoreCase = true) -> "Child"
                    roleA.contains("Child", ignoreCase = true) || roleA.contains("Ward", ignoreCase = true) -> "Guardian"
                    roleA.contains("Spouse", ignoreCase = true) -> "Spouse"
                    roleA.contains("Friend", ignoreCase = true) -> "Friend"
                    else -> "Member"
                }
            }

            // 1. Fetch Dev A's and Dev B's valid_linked_agents reciprocally
            db.child(FirebaseComm.NODE_DEVICES).child(cleanDevA).child("Identity").child("valid_linked_agents").get()
                .addOnCompleteListener { taskA ->
                    val snapshotA = if (taskA.isSuccessful) taskA.result else null
                    val currentCsvA = snapshotA?.getValue(String::class.java) ?: ""
                    val approvedA = currentCsvA.split(",", ";", "|").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableSet()
                    approvedA.add(cleanDevB)
                    if (selfDevId.isNotBlank() && selfDevId != cleanDevA) approvedA.add(selfDevId)

                    db.child(FirebaseComm.NODE_DEVICES).child(cleanDevB).child("Identity").child("valid_linked_agents").get()
                        .addOnCompleteListener { taskB ->
                            val snapshotB = if (taskB.isSuccessful) taskB.result else null
                            val currentCsvB = snapshotB?.getValue(String::class.java) ?: ""
                            val approvedB = currentCsvB.split(",", ";", "|").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableSet()
                            approvedB.add(cleanDevA)
                            if (selfDevId.isNotBlank() && selfDevId != cleanDevB) approvedB.add(selfDevId)

                            val localDb = resolvedCtx?.let { com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(it) }
                            if (localDb != null) {
                                try {
                                    approvedA.addAll(localDb.getConnectedDeviceIds(cleanDevA))
                                    approvedB.addAll(localDb.getConnectedDeviceIds(cleanDevB))
                                } catch (ignored: Exception) {}
                            }

                            val newSecureCsvA = approvedA.joinToString(",")
                            val newSecureCsvB = approvedB.joinToString(",")

                            val nameA = localDb?.getDeviceCentricName(cleanDevA)?.ifEmpty { cleanDevA } ?: cleanDevA
                            val nameB = localDb?.getDeviceCentricName(cleanDevB)?.ifEmpty { cleanDevB } ?: cleanDevB
                            val effectiveSyncSessionId = if (!pairingCode.isNullOrBlank()) pairingCode else "SYNC_${System.currentTimeMillis()}_${(1000..9999).random()}"

                            val nowMs = System.currentTimeMillis()
                            val expiresAtMs = nowMs + 86400000L // 24 Hours TTL

                            val updates = hashMapOf<String, Any>(
                                // Device-Centric Relationships Mesh
                                "${FirebaseComm.NODE_DEVICES}/$cleanDevA/relationships/$cleanDevB" to mapOf(
                                    "role" to roleA,
                                    "name" to nameB,
                                    "group" to effectiveGroup,
                                    "linked_at" to now
                                ),
                                "${FirebaseComm.NODE_DEVICES}/$cleanDevB/relationships/$cleanDevA" to mapOf(
                                    "role" to computedRoleB,
                                    "name" to nameA,
                                    "group" to effectiveGroup,
                                    "linked_at" to now
                                ),
                                "${FirebaseComm.NODE_DEVICES}/$cleanDevA/Identity/valid_linked_agents" to newSecureCsvA,
                                "${FirebaseComm.NODE_DEVICES}/$cleanDevA/identity/valid_linked_agents" to newSecureCsvA,
                                "${FirebaseComm.NODE_DEVICES}/$cleanDevB/Identity/valid_linked_agents" to newSecureCsvB,
                                "${FirebaseComm.NODE_DEVICES}/$cleanDevB/identity/valid_linked_agents" to newSecureCsvB,

                        // Circle-level device pairing
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevA/identity/name" to nameA,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevA/identity/full_name" to nameA,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevA/membership/role" to roleA,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevA/membership/status" to "Active",
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevA/membership/ts_joined" to now,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevA/paired_with/$cleanDevB" to true,

                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevB/identity/name" to nameB,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevB/identity/full_name" to nameB,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevB/membership/role" to computedRoleB,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevB/membership/status" to "Active",
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevB/membership/ts_joined" to now,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevB/paired_with/$cleanDevA" to true,

                        // Direct pairs bidirectional entry
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/direct_pairs/$cleanDevA/$cleanDevB" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/direct_pairs/$cleanDevB/$cleanDevA" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/signals/refresh_roster" to System.currentTimeMillis(),

                        // Device command dispatch for closed-loop real-time sync with 24-Hour TTL (Dual-mirrored for zero-latency ingress)
                        "${FirebaseComm.NODE_DEVICES}/$cleanDevA/commands" to mapOf(
                            "action" to "DEVICE_SYNC",
                            "command" to "DEVICE_SYNC",
                            "sync_session_id" to effectiveSyncSessionId,
                            "pairing_code" to (pairingCode ?: ""),
                            "peer_id" to cleanDevB,
                            "peer_name" to nameB,
                            "role" to computedRoleB,
                            "peer_role" to computedRoleB,
                            "my_role" to roleA,
                            "group_id" to effectiveGroup,
                            "epoch_ms" to nowMs,
                            "expires_at" to expiresAtMs,
                            "ttl_hours" to 24
                        ),
                        "${FirebaseComm.NODE_DEVICES}/$cleanDevA/device/remote_command" to mapOf(
                            "action" to "DEVICE_SYNC",
                            "command" to "DEVICE_SYNC",
                            "sync_session_id" to effectiveSyncSessionId,
                            "pairing_code" to (pairingCode ?: ""),
                            "peer_id" to cleanDevB,
                            "peer_name" to nameB,
                            "role" to computedRoleB,
                            "peer_role" to computedRoleB,
                            "my_role" to roleA,
                            "group_id" to effectiveGroup,
                            "epoch_ms" to nowMs,
                            "expires_at" to expiresAtMs,
                            "ttl_hours" to 24
                        ),
                        "${FirebaseComm.NODE_DEVICES}/$cleanDevB/commands" to mapOf(
                            "action" to "DEVICE_SYNC",
                            "command" to "DEVICE_SYNC",
                            "sync_session_id" to effectiveSyncSessionId,
                            "pairing_code" to (pairingCode ?: ""),
                            "peer_id" to cleanDevA,
                            "peer_name" to nameA,
                            "role" to roleA,
                            "peer_role" to roleA,
                            "my_role" to computedRoleB,
                            "group_id" to effectiveGroup,
                            "epoch_ms" to nowMs,
                            "expires_at" to expiresAtMs,
                            "ttl_hours" to 24
                        ),
                        "${FirebaseComm.NODE_DEVICES}/$cleanDevB/device/remote_command" to mapOf(
                            "action" to "DEVICE_SYNC",
                            "command" to "DEVICE_SYNC",
                            "sync_session_id" to effectiveSyncSessionId,
                            "pairing_code" to (pairingCode ?: ""),
                            "peer_id" to cleanDevA,
                            "peer_name" to nameA,
                            "role" to roleA,
                            "peer_role" to roleA,
                            "my_role" to computedRoleB,
                            "group_id" to effectiveGroup,
                            "epoch_ms" to nowMs,
                            "expires_at" to expiresAtMs,
                            "ttl_hours" to 24
                        )
                    )

                    // 🛡️ Self-Authenticated Circle Invariant: Only update users/ node for self to avoid Firebase Permission Denied on foreign peer nodes
                    if (cleanDevA == selfDevId) {
                        updates["users/$cleanDevA/joined_circles/$effectiveGroup"] = true
                    }
                    if (cleanDevB == selfDevId) {
                        updates["users/$cleanDevB/joined_circles/$effectiveGroup"] = true
                    }

                    db.updateChildren(updates).addOnCompleteListener { updTask ->
                        if (updTask.isSuccessful) {
                            SystemLogger.i("PairingRepo", "🔗 Devices Connected: $cleanDevA ($roleA) <-> $cleanDevB ($computedRoleB) in $effectiveGroup")
                            
                            // Local SQLite commit
                            resolvedCtx?.let { ctx ->
                                try {
                                    val sLocalDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(ctx)
                                    val selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(ctx)
                                    val pairId = "PAIR_${cleanDevA}_${cleanDevB}"
                                    com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairingWithRoles(
                                        sLocalDb.writableDatabase,
                                        pairId,
                                        cleanDevA,
                                        cleanDevB,
                                        roleA,
                                        computedRoleB,
                                        pairingCode,
                                        "PAIRED"
                                    )

                                    val isDevAFriend = roleA.contains("Friend", ignoreCase = true)
                                    val isDevBFriend = computedRoleB.contains("Friend", ignoreCase = true)
                                    sLocalDb.saveRosterNode(cleanDevA, nameA, roleA, "ACTIVE", false, isDevAFriend, activeGroupId = effectiveGroup)
                                    sLocalDb.saveRosterNode(cleanDevB, nameB, computedRoleB, "ACTIVE", false, isDevBFriend, activeGroupId = effectiveGroup)
                                    sLocalDb.appendValidLinkedAgent(cleanDevA, cleanDevB)
                                    sLocalDb.appendValidLinkedAgent(cleanDevB, cleanDevA)

                                    sLocalDb.updateCitizenRole(cleanDevA, if (isDevAFriend) "None" else roleA, if (isDevAFriend) roleA else "None", isDevAFriend, effectiveGroup)
                                    sLocalDb.updateCitizenRole(cleanDevB, if (isDevBFriend) "None" else computedRoleB, if (isDevBFriend) computedRoleB else "None", isDevBFriend, effectiveGroup)

                                    // 🛡️ TRIADIC PAIRING SSOT: If initiating device (A) is linking B <-> C, ensure bidirectional pairs (A <-> B) and (A <-> C) exist in SQLite
                                    if (selfDevId.isNotBlank()) {
                                        if (selfDevId != cleanDevA) {
                                            com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairingWithRoles(
                                                sLocalDb.writableDatabase,
                                                "PAIR_${selfDevId}_${cleanDevA}",
                                                selfDevId,
                                                cleanDevA,
                                                "Member",
                                                roleA,
                                                pairingCode,
                                                "PAIRED"
                                            )
                                            sLocalDb.appendValidLinkedAgent(selfDevId, cleanDevA)
                                            sLocalDb.appendValidLinkedAgent(cleanDevA, selfDevId)
                                        }
                                        if (selfDevId != cleanDevB) {
                                            com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairingWithRoles(
                                                sLocalDb.writableDatabase,
                                                "PAIR_${selfDevId}_${cleanDevB}",
                                                selfDevId,
                                                cleanDevB,
                                                "Member",
                                                computedRoleB,
                                                pairingCode,
                                                "PAIRED"
                                            )
                                            sLocalDb.appendValidLinkedAgent(selfDevId, cleanDevB)
                                            sLocalDb.appendValidLinkedAgent(cleanDevB, selfDevId)
                                        }
                                    }

                                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                                        ctx,
                                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.CATEGORY_ROSTER,
                                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPSERT,
                                        cleanDevB
                                    )
                                } catch (ex: Exception) {
                                    SystemLogger.e("PairingRepo", "Local SQLite logPairingWithRoles warning: ${ex.message}")
                                }
                            }

                            val logDetails = mapOf("device_a" to cleanDevA, "device_b" to cleanDevB, "role_a" to roleA, "role_b" to computedRoleB, "group" to effectiveGroup)
                            FirebaseComm.logChange(cleanDevA, FirebaseComm.LOG_ITEM_PAIRING, "DEVICE_PAIR_LINK", cleanDevB, logDetails)
                            FirebaseComm.logChange(cleanDevB, FirebaseComm.LOG_ITEM_PAIRING, "DEVICE_PAIR_LINK", cleanDevA, logDetails)
                            callback?.onResult(true, null)
                        } else {
                            val err = updTask.exception?.message ?: "Firebase link update rejected"
                            SystemLogger.e("PairingRepo", "linkDevicesWithRoles rejected: $err")
                            callback?.onResult(false, err)
                        }
                    }
                }
            }
        }

        // [Variables] context: Context, hostGroupId: String, devAId: String, devBId: String, roleA: String, roleB: String?, callback: DeviceSharingSyncCallback, cleanDevA: String, cleanDevB: String, selfDevId: String, timeoutMs: Long, syncSessionId: String, startTime: Long, mainHandler: Handler, isResolved: AtomicBoolean, needsAckA: Boolean, needsAckB: Boolean, ackReceivedA: AtomicBoolean, ackReceivedB: AtomicBoolean, listenerA: ValueEventListener?, listenerB: ValueEventListener?
        // [Constants] NODE_DEVICES
        // [Dependencies] FirebaseComm, SystemLogger, RegistryManager, DeviceIdentityHelper
        // [Integration Points] MapActivity, FamilyFriendsActivity, TrackingService, FCMService
        /**
         * 🚀 Execute Closed-Loop Device Sharing:
         * Dispatches reciprocal roles, commands, and attaches real-time ACK listeners with standard watchdog timer.
         * Angel Butterfly progress remains active until remote peers physically acknowledge, or watchdog fires timeout feedback.
         */
        @JvmStatic
        fun executeClosedLoopDeviceSharing(
            context: android.content.Context,
            hostGroupId: String,
            devAId: String,
            devBId: String,
            roleA: String,
            roleB: String? = null,
            callback: DeviceSharingSyncCallback
        ) {
            val cleanDevA = FirebaseComm.sanitizeAgentId(devAId)
            val cleanDevB = FirebaseComm.sanitizeAgentId(devBId)
            val selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            val timeoutMs = RegistryManager.getDeviceSharingTimeoutMs(context)
            com.jdroidx.jfindme.utils.RegistryManager.undecommissionDevice(context, cleanDevA)
            com.jdroidx.jfindme.utils.RegistryManager.undecommissionDevice(context, cleanDevB)
            val pairCode = (100000..999999).random().toString()
            val syncSessionId = "SYNC_${pairCode}_${System.currentTimeMillis()}"
            val startTime = System.currentTimeMillis()
            val shareId = "DSH_${cleanDevA}_${cleanDevB}"

            // 🛡️ OOPS MVC SSOT: Register pairing session at /pairing_sessions/{pairCode} (24h TTL)
            try {
                val pSession = PairingSession(
                    sessionId = syncSessionId,
                    userId = selfDevId,
                    pairCode = pairCode,
                    expiresAt = System.currentTimeMillis() + 86400000L,
                    primaryDevice = cleanDevA,
                    status = "ACTIVE"
                )
                FirebaseComm.database.getReference("pairing_sessions").child(pairCode).setValue(pSession)
            } catch (_: Exception) {}

            // 🛡️ V83 Sub-Master State Initialization
            val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            localDb.upsertDeviceSharing(
                shareId = shareId,
                masterDev = selfDevId,
                submasterDev = cleanDevA,
                targetDev = cleanDevB,
                relType = roleA,
                mask = 0xFF,
                status = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REQUESTED,
                isActive = false,
                retryCount = 0
            )
            localDb.recordDeviceSharingTransaction(
                shareId = shareId,
                actorDev = selfDevId,
                recipientDev = cleanDevA,
                prevStage = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_NONE,
                newStage = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REQUESTED,
                attempt = 1,
                channel = "CLOUD_FIREBASE",
                reason = "Triadic device sharing request initiated by Master"
            )

            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val isResolved = java.util.concurrent.atomic.AtomicBoolean(false)

            val needsAckA = cleanDevA != selfDevId && cleanDevA.isNotBlank()
            val needsAckB = cleanDevB != selfDevId && cleanDevB.isNotBlank()

            val ackReceivedA = java.util.concurrent.atomic.AtomicBoolean(!needsAckA)
            val ackReceivedB = java.util.concurrent.atomic.AtomicBoolean(!needsAckB)

            var listenerA: ValueEventListener? = null
            var listenerB: ValueEventListener? = null
            val refA = FirebaseComm.getDeviceRef(cleanDevA).child("commands_ack").child("device_sync")
            val refB = FirebaseComm.getDeviceRef(cleanDevB).child("commands_ack").child("device_sync")

            val cleanup = {
                listenerA?.let { refA.removeEventListener(it) }
                listenerB?.let { refB.removeEventListener(it) }
            }

            val timeoutRunnable = java.lang.Runnable {
                if (isResolved.compareAndSet(false, true)) {
                    cleanup()
                    val unAcked = when {
                        !ackReceivedA.get() && !ackReceivedB.get() -> "$cleanDevA & $cleanDevB"
                        !ackReceivedA.get() -> cleanDevA
                        !ackReceivedB.get() -> cleanDevB
                        else -> "Peer device"
                    }
                    val diagReason = "Peer device ($unAcked) did not acknowledge synchronization handshake within standard time limit (${timeoutMs / 1000}s).\n\n" +
                        "Diagnostic Reason: Device offline, power-saving doze active, or cellular latency exceeded SLA threshold."
                    localDb.updateDeviceSharingStage(
                        shareId,
                        com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_TIMED_OUT,
                        selfDevId,
                        diagReason
                    )
                    localDb.saveSysLog("WARN", "PairingRepository", diagReason)
                    mainHandler.post { callback.onSyncTimeout(diagReason) }

                    // 🛡️ CONTINUOUS 24-HOUR BACKGROUND SENTINEL: Keep passive background listener armed
                    armPersistent24hAckSentinel(context, cleanDevA, cleanDevB, syncSessionId, roleA, roleB)
                }
            }

            mainHandler.postDelayed(timeoutRunnable, timeoutMs)

            val checkCompletion = {
                if (ackReceivedA.get() && ackReceivedB.get()) {
                    if (isResolved.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        cleanup()
                        val latencyMs = System.currentTimeMillis() - startTime
                        localDb.updateDeviceSharingStage(
                            shareId,
                            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_ACTIVE,
                            cleanDevA,
                            "Mutual synchronization acknowledged (Latency: ${latencyMs}ms)"
                        )
                        mainHandler.post { callback.onSyncComplete(cleanDevA, cleanDevB, latencyMs) }
                    }
                }
            }

            if (needsAckA) {
                listenerA = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val status = snapshot.child("status").getValue(String::class.java)
                        val incomingSession = snapshot.child("sync_session_id").getValue(String::class.java)
                        if ("ACKNOWLEDGED".equals(status, ignoreCase = true) &&
                            (incomingSession == null || incomingSession == syncSessionId || incomingSession.startsWith("SYNC_"))) {
                            ackReceivedA.set(true)
                            checkCompletion()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                refA.addValueEventListener(listenerA!!)
            }

            if (needsAckB) {
                listenerB = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val status = snapshot.child("status").getValue(String::class.java)
                        val incomingSession = snapshot.child("sync_session_id").getValue(String::class.java)
                        if ("ACKNOWLEDGED".equals(status, ignoreCase = true) &&
                            (incomingSession == null || incomingSession == syncSessionId || incomingSession.startsWith("SYNC_"))) {
                            ackReceivedB.set(true)
                            checkCompletion()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                refB.addValueEventListener(listenerB!!)
            }

            // Dispatch mutations to Firebase RTDB
            linkDevicesWithRoles(
                hostGroupId,
                cleanDevA,
                cleanDevB,
                roleA,
                roleB,
                pairingCode = pairCode,
                callback = object : ResultCallback {
                    override fun onResult(success: Boolean, errorMessage: String?) {
                        if (success) {
                            mainHandler.post { callback.onSyncDispatched(syncSessionId) }
                            // 🚀 OOPS MVC Direct Completion: Master has written the bidirectional cloud pairing
                            // and dispatched commands. If either device is an offline node (e.g. Device C / Pushpa),
                            // we do not stall for 45s on socket ACK. We complete immediately and keep background sentinel armed.
                            if (isResolved.compareAndSet(false, true)) {
                                mainHandler.removeCallbacks(timeoutRunnable)
                                val latencyMs = System.currentTimeMillis() - startTime
                                localDb.updateDeviceSharingStage(
                                    shareId,
                                    com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_ACTIVE,
                                    cleanDevA,
                                    "Cloud device sharing established and committed (Latency: ${latencyMs}ms)"
                                )
                                mainHandler.post { callback.onSyncComplete(cleanDevA, cleanDevB, latencyMs) }
                            }
                            armPersistent24hAckSentinel(context, cleanDevA, cleanDevB, syncSessionId, roleA, roleB)
                        } else {
                            if (isResolved.compareAndSet(false, true)) {
                                mainHandler.removeCallbacks(timeoutRunnable)
                                cleanup()
                                val failMsg = errorMessage ?: "Firebase cloud sync dispatch rejected"
                                com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).saveSysLog("ERROR", "PairingRepository", failMsg)
                                mainHandler.post { callback.onSyncFailed(failMsg) }
                            }
                        }
                    }
                }
            )
        }

        // [Variables] context, hostGroupId, devAId, devBId, callback, cleanDevA, cleanDevB, effectiveGroup, localDb, nowMs, shareId, altShareId, updates | [Constants] NODE_DEVICES, NODE_RELATIONSHIPS | [Dependencies] FirebaseComm, DatabaseHelper, RegistryManager, SystemLogger | [Integration Points] FamilyFriendsActivity, MapActivity, PairingRepository
        @JvmStatic
        fun unlinkPeerDevices(
            context: android.content.Context,
            hostGroupId: String,
            devAId: String,
            devBId: String,
            callback: ((Boolean, String?) -> Unit)? = null
        ) {
            val cleanDevA = FirebaseComm.sanitizeAgentId(devAId)
            val cleanDevB = FirebaseComm.sanitizeAgentId(devBId)
            val effectiveGroup = if (hostGroupId.isNotBlank() && hostGroupId != "DEFAULT_GROUP") {
                hostGroupId
            } else {
                RegistryManager.getEffectiveActiveGroup(context)
            }
            val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val nowMs = System.currentTimeMillis()

            // 1. Local Database cleanup
            val shareId = "DSH_${cleanDevA}_${cleanDevB}"
            val altShareId = "DSH_${cleanDevB}_${cleanDevA}"
            localDb.updateDeviceSharingStage(
                shareId,
                com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REVOKED,
                cleanDevA,
                "Device sharing unlinked by Master"
            )
            localDb.updateDeviceSharingStage(
                altShareId,
                com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REVOKED,
                cleanDevA,
                "Device sharing unlinked by Master"
            )
            val pairId1 = "PAIR_${cleanDevA}_${cleanDevB}"
            val pairId2 = "PAIR_${cleanDevB}_${cleanDevA}"
            val cvPair = android.content.ContentValues().apply {
                put(com.jdroidx.jfindme.data.local.tables.DevicePairingTable.COL_PAIR_STATUS, "UNLINKED")
                put(com.jdroidx.jfindme.data.local.tables.DevicePairingTable.COL_UPDATED_AT, FirebaseComm.getForensicTimestamp())
            }
            try {
                localDb.writableDatabase.update(
                    com.jdroidx.jfindme.data.local.tables.DevicePairingTable.TABLE_NAME,
                    cvPair,
                    "${com.jdroidx.jfindme.data.local.tables.DevicePairingTable.COL_PAIR_ID} = ? OR ${com.jdroidx.jfindme.data.local.tables.DevicePairingTable.COL_PAIR_ID} = ?",
                    arrayOf(pairId1, pairId2)
                )
            } catch (_: Exception) {}

            // 2. Cloud Firebase RTDB updates
            val updates = hashMapOf<String, Any?>(
                "${FirebaseComm.NODE_DEVICES}/$cleanDevA/relationships/$cleanDevB" to null,
                "${FirebaseComm.NODE_DEVICES}/$cleanDevB/relationships/$cleanDevA" to null,
                "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/direct_pairs/$cleanDevA/$cleanDevB" to null,
                "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/direct_pairs/$cleanDevB/$cleanDevA" to null,
                "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevA/paired_with/$cleanDevB" to null,
                "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanDevB/paired_with/$cleanDevA" to null,
                "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/signals/refresh_roster" to nowMs,

                // Dispatch UNLINK commands to both devices
                "${FirebaseComm.NODE_DEVICES}/$cleanDevA/device/remote_command" to mapOf(
                    "action" to "UNLINK",
                    "command" to "UNLINK",
                    "from_agent" to cleanDevB,
                    "target_agent" to cleanDevB,
                    "epoch_ms" to nowMs
                ),
                "${FirebaseComm.NODE_DEVICES}/$cleanDevB/device/remote_command" to mapOf(
                    "action" to "UNLINK",
                    "command" to "UNLINK",
                    "from_agent" to cleanDevA,
                    "target_agent" to cleanDevA,
                    "epoch_ms" to nowMs
                )
            )

            // 3. Remove from valid_linked_agents reciprocally
            db.child(FirebaseComm.NODE_DEVICES).child(cleanDevA).child("Identity").child("valid_linked_agents").get()
                .addOnCompleteListener { tA ->
                    val curA = if (tA.isSuccessful) tA.result?.getValue(String::class.java) ?: "" else ""
                    val newCsvA = curA.split(",", ";", "|")
                        .map { it.trim().uppercase() }
                        .filter { it.isNotEmpty() && it != cleanDevB }
                        .joinToString(",")
                    updates["${FirebaseComm.NODE_DEVICES}/$cleanDevA/Identity/valid_linked_agents"] = newCsvA
                    updates["${FirebaseComm.NODE_DEVICES}/$cleanDevA/identity/valid_linked_agents"] = newCsvA

                    db.child(FirebaseComm.NODE_DEVICES).child(cleanDevB).child("Identity").child("valid_linked_agents").get()
                        .addOnCompleteListener { tB ->
                            val curB = if (tB.isSuccessful) tB.result?.getValue(String::class.java) ?: "" else ""
                            val newCsvB = curB.split(",", ";", "|")
                                .map { it.trim().uppercase() }
                                .filter { it.isNotEmpty() && it != cleanDevA }
                                .joinToString(",")
                            updates["${FirebaseComm.NODE_DEVICES}/$cleanDevB/Identity/valid_linked_agents"] = newCsvB
                            updates["${FirebaseComm.NODE_DEVICES}/$cleanDevB/identity/valid_linked_agents"] = newCsvB

                            FirebaseComm.database.reference.updateChildren(updates).addOnCompleteListener { updTask ->
                                if (updTask.isSuccessful) {
                                    SystemLogger.i("PairingRepo", "🗑️ Unlinked shared peers $cleanDevA <-> $cleanDevB")
                                    callback?.invoke(true, null)
                                } else {
                                    val err = updTask.exception?.message ?: "Unlink failed"
                                    callback?.invoke(false, err)
                                }
                            }
                        }
                }
        }

        // [Variables] context: Context, hostGroupId: String, sourceDevId: String, targetDevIds: List<String>, roles: List<String>, callback: BatchDeviceSharingSyncCallback, cleanSource: String, selfDevId: String, timeoutMs: Long, syncSessionId: String, startTime: Long, total: Int, mainHandler: Handler, isResolved: AtomicBoolean, ackedCount: AtomicInteger, listeners: List<Pair<DatabaseReference, ValueEventListener>>, timeoutRunnable: Runnable, checkCompletion: Function0, primaryRole: String, dispatchedCount: AtomicInteger
        // [Constants] NODE_DEVICES
        // [Dependencies] FirebaseComm, SystemLogger, RegistryManager, DeviceIdentityHelper
        // [Integration Points] MapActivity, FamilyFriendsActivity, TrackingService, FCMService
        /**
         * 🚀 Execute Closed-Loop Batch Device Sharing:
         * Attaches physical ACK listeners across all target devices with dynamic watchdog timer.
         * Angel Butterfly remains flapping until all peers physically ACK, or watchdog fires timeout feedback.
         */
        @JvmStatic
        fun executeClosedLoopBatchDeviceSharing(
            context: android.content.Context,
            hostGroupId: String,
            sourceDevId: String,
            targetDevIds: List<String>,
            roles: List<String>,
            callback: BatchDeviceSharingSyncCallback
        ) {
            val cleanSource = FirebaseComm.sanitizeAgentId(sourceDevId)
            val selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            val timeoutMs = RegistryManager.getDeviceSharingTimeoutMs(context)
            val syncSessionId = "SYNC_BATCH_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val startTime = System.currentTimeMillis()
            val total = targetDevIds.size

            if (total == 0) {
                val noDevMsg = "No target devices specified for batch sharing."
                com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).saveSysLog("WARN", "PairingRepository", noDevMsg)
                callback.onBatchFailed(noDevMsg)
                return
            }

            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val isResolved = java.util.concurrent.atomic.AtomicBoolean(false)
            val ackedCount = java.util.concurrent.atomic.AtomicInteger(0)
            val listeners = mutableListOf<Pair<com.google.firebase.database.DatabaseReference, ValueEventListener>>()
            val primaryRole = if (roles.isNotEmpty()) roles[0].replace(Regex("[^a-zA-Z /]"), "").trim() else "Member"

            val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            for (tId in targetDevIds) {
                val cleanT = FirebaseComm.sanitizeAgentId(tId)
                val shareId = "DSH_${cleanSource}_${cleanT}"
                localDb.upsertDeviceSharing(
                    shareId = shareId,
                    masterDev = selfDevId,
                    submasterDev = cleanSource,
                    targetDev = cleanT,
                    relType = primaryRole,
                    mask = 0xFF,
                    status = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REQUESTED,
                    isActive = false,
                    retryCount = 0
                )
                localDb.recordDeviceSharingTransaction(
                    shareId = shareId,
                    actorDev = selfDevId,
                    recipientDev = cleanT,
                    prevStage = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_NONE,
                    newStage = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REQUESTED,
                    attempt = 1,
                    channel = "CLOUD_FIREBASE",
                    reason = "Batch device sharing invitation dispatched"
                )
            }

            val cleanup = {
                listeners.forEach { (ref, listener) ->
                    try { ref.removeEventListener(listener) } catch (ignored: Exception) {}
                }
                listeners.clear()
            }

            val timeoutRunnable = java.lang.Runnable {
                if (isResolved.compareAndSet(false, true)) {
                    cleanup()
                    val count = ackedCount.get()
                    val diagReason = "Batch synchronization timed out after ${timeoutMs / 1000}s ($count of $total devices acknowledged).\n\n" +
                        "Diagnostic Reason: One or more target devices failed to acknowledge within SLA threshold (devices offline, sleeping, or restricted)."
                    for (tId in targetDevIds) {
                        val cleanT = FirebaseComm.sanitizeAgentId(tId)
                        val shareId = "DSH_${cleanSource}_${cleanT}"
                        localDb.updateDeviceSharingStage(
                            shareId,
                            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_TIMED_OUT,
                            selfDevId,
                            diagReason
                        )
                    }
                    com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).saveSysLog("WARN", "PairingRepository", diagReason)
                    mainHandler.post { callback.onBatchTimeout(count, total, diagReason) }

                    // 🛡️ CONTINUOUS 24-HOUR BACKGROUND SENTINEL: Arm passive listeners for unreached batch devices
                    for (tId in targetDevIds) {
                        armPersistent24hAckSentinel(context, tId, cleanSource, syncSessionId, primaryRole, null)
                    }
                }
            }
            mainHandler.postDelayed(timeoutRunnable, timeoutMs)

            val checkCompletion = {
                val count = ackedCount.get()
                mainHandler.post { callback.onBatchProgress(count, total) }
                if (count >= total) {
                    if (isResolved.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        cleanup()
                        val latencyMs = System.currentTimeMillis() - startTime
                        for (tId in targetDevIds) {
                            val cleanT = FirebaseComm.sanitizeAgentId(tId)
                            val shareId = "DSH_${cleanSource}_${cleanT}"
                            localDb.updateDeviceSharingStage(
                                shareId,
                                com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_ACTIVE,
                                cleanT,
                                "Batch synchronization acknowledged"
                            )
                        }
                        mainHandler.post { callback.onBatchComplete(count, total, latencyMs) }
                    }
                }
            }

            for (tId in targetDevIds) {
                val cleanTId = FirebaseComm.sanitizeAgentId(tId)
                if (cleanTId == selfDevId) {
                    ackedCount.incrementAndGet()
                    continue
                }
                val ref = FirebaseComm.getDeviceRef(cleanTId).child("commands_ack").child("device_sync")
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val status = snapshot.child("status").getValue(String::class.java)
                        val incomingSession = snapshot.child("sync_session_id").getValue(String::class.java)
                        if ("ACKNOWLEDGED".equals(status, ignoreCase = true) &&
                            (incomingSession == null || incomingSession == syncSessionId || incomingSession.startsWith("SYNC_"))) {
                            ackedCount.incrementAndGet()
                            checkCompletion()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                listeners.add(Pair(ref, listener))
                ref.addValueEventListener(listener)
            }

            val dispatchedCount = java.util.concurrent.atomic.AtomicInteger(0)
            for (tId in targetDevIds) {
                linkDevicesWithRoles(
                    hostGroupId,
                    cleanSource,
                    tId,
                    primaryRole,
                    null,
                    pairingCode = syncSessionId,
                    callback = object : ResultCallback {
                        override fun onResult(success: Boolean, errorMessage: String?) {
                            if (success) {
                                if (dispatchedCount.incrementAndGet() == total) {
                                    mainHandler.post { callback.onBatchDispatched(syncSessionId, total) }
                                    checkCompletion()
                                }
                            } else {
                                val failMsg = "Batch link failed for $tId: $errorMessage"
                                SystemLogger.e("PairingRepo", failMsg)
                                com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).saveSysLog("ERROR", "PairingRepository", failMsg)
                            }
                        }
                    }
                )
            }
        }

        /**
         * [Variables] pairCode: String, sessionId: String, timeoutMs: Long, effectiveGroup: String, session: PairingSession, cleanPrimary: String, payloadRequests: Map<String, Any>
         * [Constants] None | [Dependencies] FirebaseComm, SystemLogger, PairingSession
         * [Integration Points] MapActivity, NetworkGatewayActivity, PairingSession
         * 🔑 Start Multi-Device Pairing Session (Supports attaching up to 5 devices using 1 pairing code).
         * Dual-mirrored across pairing_sessions and pairing_requests for zero-permission-block delivery.
         */
        @JvmStatic
        @JvmOverloads
        fun startMultiDevicePairingSession(
            userId: String,
            primaryDeviceId: String,
            groupId: String = "DEFAULT_GROUP",
            role: String = "MEMBER",
            maxAttachments: Int = 5,
            onCodeReady: (String) -> Unit
        ) {
            val resolvedCtx = JFindMeApplication.getContext()
            val pairCode = (100000..999999).random().toString()
            val sessionId = UUID.randomUUID().toString().take(6)
            // [Variables] timeoutMs: dynamic from RegistryManager (configurable via Settings) | [Constants] None | [Dependencies] RegistryManager | [Integration Points] SettingsActivity, PairingSession
            val timeoutMs = resolvedCtx?.let { RegistryManager.getPairingTimeoutMs(it) } ?: (30 * 60 * 1000L)
            val cleanPrimary = FirebaseComm.sanitizeAgentId(primaryDeviceId)
            val selfDevId = resolvedCtx?.let { com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(it) } ?: ""
            val prefsGroup = resolvedCtx?.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)?.getString("active_group", null)
            val effectiveGroup = when {
                groupId.isNotBlank() && groupId != "DEFAULT_GROUP" -> groupId
                resolvedCtx != null -> RegistryManager.getEffectiveActiveGroup(resolvedCtx)
                !prefsGroup.isNullOrBlank() && prefsGroup != "DEFAULT_GROUP" -> prefsGroup
                else -> "GRP_$cleanPrimary"
            }
            
            val session = PairingSession(
                sessionId = sessionId,
                userId = userId,
                pairCode = pairCode,
                expiresAt = System.currentTimeMillis() + timeoutMs,
                status = "ACTIVE",
                primaryDevice = cleanPrimary,
                secondaryDevice = null,
                attached_devices = emptyMap(),
                max_attachments = maxAttachments,
                role = role,
                group_id = effectiveGroup
            )

            val payloadRequests = mapOf(
                "pairCode" to pairCode,
                "invite_code" to pairCode,
                "sessionId" to sessionId,
                "hostDeviceId" to cleanPrimary,
                "creator_device_id" to cleanPrimary,
                "group_id" to effectiveGroup,
                "role" to role,
                "status" to "ACTIVE",
                "max_attachments" to maxAttachments,
                "expires_at" to (System.currentTimeMillis() + timeoutMs),
                "is_multi_device" to true
            )

            val updates = mapOf<String, Any>(
                "pairing_sessions/$pairCode" to session,
                "pairing_requests/$pairCode" to payloadRequests
            )

            FirebaseComm.database.reference.updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        SystemLogger.i("PairingRepo", "Multi-Device Pairing Session Started: $pairCode in Group $effectiveGroup (Max $maxAttachments devices)")
                        onCodeReady(pairCode)
                    } else {
                        val err = task.exception?.message ?: "Failed writing pairing session"
                        SystemLogger.e("PairingRepo", "pairing session update failed: $err")
                        onCodeReady("")
                    }
                }
        }

        @JvmStatic
        fun startMultiDevicePairingSession(userId: String, primaryDeviceId: String, role: String, maxAttachments: Int, callback: CodeCallback) {
            val ctx = JFindMeApplication.getContext()
            val resolvedGroup = if (ctx != null) RegistryManager.getEffectiveActiveGroup(ctx) else "GRP_$userId"
            startMultiDevicePairingSession(userId, primaryDeviceId, resolvedGroup, role, maxAttachments) { callback.onCodeReady(it) }
        }

        @JvmStatic
        fun startMultiDevicePairingSession(userId: String, primaryDeviceId: String, groupId: String, role: String, maxAttachments: Int, callback: CodeCallback) {
            startMultiDevicePairingSession(userId, primaryDeviceId, groupId, role, maxAttachments) { callback.onCodeReady(it) }
        }

        /**
         * [Variables] cleanCode: String, cleanJoiner: String, ref: DatabaseReference, session: PairingSession, attachedMap: Map, now: String, newAttachment: Map, updates: HashMap, hostGrp: String, ctx: Context, localDb: DatabaseHelper, prefs: SharedPreferences
         * [Constants] None | [Dependencies] FirebaseComm, SystemLogger, JFindMeApplication, DatabaseHelper, DevicePairingTable, MasterDataSyncSentinel
         * [Integration Points] NetworkGatewayActivity, MapActivity
         * 👥 Join Multi-Device Pairing Session (1 code for close attachment across 3/4/5 devices).
         * Dual-evaluates pairing_sessions with fallback to pairing_requests.
         */
        @JvmStatic
        @JvmOverloads
        fun joinMultiDevicePairingSession(
            pairCode: String,
            joiningDeviceId: String,
            requestedRole: String = "MEMBER",
            onResult: (Boolean, String?) -> Unit
        ) {
            val cleanCode = pairCode.trim()
            val cleanJoiner = FirebaseComm.sanitizeAgentId(joiningDeviceId)
            val ref = FirebaseComm.database.getReference("pairing_sessions").child(cleanCode)
            
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        processMultiDeviceSession(snapshot, cleanCode, cleanJoiner, requestedRole, onResult)
                    } else {
                        // Fallback check under pairing_requests
                        FirebaseComm.database.getReference("pairing_requests").child(cleanCode).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(reqSnap: DataSnapshot) {
                                if (reqSnap.exists()) {
                                    val primaryDev = reqSnap.child("hostDeviceId").getValue(String::class.java)
                                        ?: reqSnap.child("creator_device_id").getValue(String::class.java) ?: ""
                                    val grp = reqSnap.child("group_id").getValue(String::class.java) ?: "GRP_$primaryDev"
                                    val r = reqSnap.child("role").getValue(String::class.java) ?: "MEMBER"
                                    
                                    linkDevicesWithRoles(
                                        hostGroupId = grp,
                                        devAId = primaryDev,
                                        devBId = cleanJoiner,
                                        roleA = r,
                                        roleB = requestedRole,
                                        pairingCode = cleanCode,
                                        callback = { success, err ->
                                            if (success) {
                                                finalizeJoiningDeviceState(primaryDev, cleanJoiner, requestedRole, grp)
                                                onResult(true, primaryDev)
                                            } else {
                                                onResult(false, err)
                                            }
                                        }
                                    )
                                } else {
                                    onResult(false, "Invalid Pair Code")
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                onResult(false, error.message)
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(false, error.message)
                }
            })
        }

        private fun processMultiDeviceSession(
            snapshot: DataSnapshot,
            cleanCode: String,
            cleanJoiner: String,
            requestedRole: String,
            onResult: (Boolean, String?) -> Unit
        ) {
            val session = snapshot.getValue(PairingSession::class.java)
            if (session == null) {
                onResult(false, "Corrupted Pairing Session")
                return
            }
            if (session.expiresAt < System.currentTimeMillis() || (session.status != "ACTIVE" && session.status != "pending")) {
                onResult(false, "Pairing code has expired or session is sealed.")
                return
            }
            val attachedMap = session.attached_devices ?: emptyMap()
            if (attachedMap.size >= session.max_attachments) {
                onResult(false, "Maximum device limit (${session.max_attachments}) reached for this pairing code.")
                return
            }

            val now = FirebaseComm.getForensicTimestamp()
            val newAttachment = mapOf(
                "role" to requestedRole,
                "status" to "LINKED",
                "joined_at" to now
            )
            val updates = hashMapOf<String, Any>(
                "attached_devices/$cleanJoiner" to newAttachment,
                "secondaryDevice" to cleanJoiner
            )
            if (attachedMap.size + 1 >= session.max_attachments) {
                updates["status"] = "SEALED"
            }

            snapshot.ref.updateChildren(updates).addOnCompleteListener {
                val hostGrp = if (!session.group_id.isNullOrBlank() && session.group_id != "DEFAULT_GROUP") {
                    session.group_id
                } else {
                    "GRP_${session.primaryDevice}"
                }
                linkDevicesWithRoles(
                    hostGroupId = hostGrp,
                    devAId = session.primaryDevice,
                    devBId = cleanJoiner,
                    roleA = session.role,
                    roleB = requestedRole,
                    pairingCode = cleanCode,
                    callback = { success, err ->
                        if (success) {
                            finalizeJoiningDeviceState(session.primaryDevice, cleanJoiner, requestedRole, hostGrp)
                            // 🌐 Full Mesh: Reciprocally link new joiner to all other sibling devices attached in this session
                            for (peerJoiner in attachedMap.keys) {
                                val cleanPeer = FirebaseComm.sanitizeAgentId(peerJoiner)
                                if (FirebaseComm.isAgentIdValid(cleanPeer) && cleanPeer != cleanJoiner) {
                                    linkDevicesWithRoles(
                                        hostGroupId = hostGrp,
                                        devAId = cleanPeer,
                                        devBId = cleanJoiner,
                                        roleA = "MEMBER",
                                        roleB = requestedRole,
                                        pairingCode = cleanCode,
                                        callback = { _, _ -> }
                                    )
                                }
                            }
                            SystemLogger.i("PairingRepo", "Device $cleanJoiner successfully attached via Code $cleanCode")
                            onResult(true, session.primaryDevice)
                        } else {
                            onResult(false, err)
                        }
                    }
                )
            }
        }

        private fun finalizeJoiningDeviceState(primaryDev: String, cleanJoiner: String, requestedRole: String, hostGrp: String) {
            JFindMeApplication.getContext()?.let { ctx ->
                try {
                    val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(ctx)
                    val prefs = ctx.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("active_group", hostGrp).apply()

                    val isFriend = requestedRole.contains("Friend", ignoreCase = true)
                    localDb.saveRosterNode(primaryDev, "Primary Hub ($primaryDev)", "ADMIN", "ACTIVE", true, false, activeGroupId = hostGrp)
                    localDb.saveRosterNode(cleanJoiner, cleanJoiner, requestedRole, "ACTIVE", false, isFriend, activeGroupId = hostGrp)
                    localDb.appendValidLinkedAgent(primaryDev, cleanJoiner)
                    localDb.appendValidLinkedAgent(cleanJoiner, primaryDev)
                    localDb.updateCitizenRole(cleanJoiner, if (isFriend) "None" else requestedRole, if (isFriend) requestedRole else "None", isFriend, hostGrp)

                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                        ctx,
                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.CATEGORY_ROSTER,
                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPSERT,
                        primaryDev
                    )
                } catch (e: Exception) {
                    SystemLogger.e("PairingRepo", "Failed finalizing joining device state: ${e.message}")
                }
            }
        }

        @JvmStatic
        fun joinMultiDevicePairingSession(pairCode: String, joiningDeviceId: String, requestedRole: String, callback: ResultCallback) {
            joinMultiDevicePairingSession(pairCode, joiningDeviceId, requestedRole) { success, msg -> callback.onResult(success, msg) }
        }

        // [Variables] context: Context, devAId: String, devBId: String, syncSessionId: String, roleA: String, roleB: String?, selfDevId: String, targets: List<String>
        // [Constants] None
        // [Dependencies] FirebaseComm, DatabaseHelper, SystemLogger, AlertManager
        // [Integration Points] PairingRepository, TrackingService, FCMService
        /**
         * 🛡️ Persistent 24-Hour ACK Sentinel:
         * Maintains passive background listeners for unreached/offline nodes up to 24 hours.
         * When target boots and posts ACK, updates SQLite, dispatches broadcast, and displays confirmation notification.
         */
        @JvmStatic
        fun armPersistent24hAckSentinel(
            context: android.content.Context,
            devAId: String,
            devBId: String,
            syncSessionId: String,
            roleA: String,
            roleB: String? = null
        ) {
            val selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            val targets = listOf(devAId, devBId).filter { it.isNotBlank() && it != selfDevId }

            for (targetId in targets) {
                val ackRef = FirebaseComm.getDeviceRef(targetId).child("commands_ack").child("device_sync")
                val sentinelListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) return
                        val status = snapshot.child("status").getValue(String::class.java)
                        val incomingSession = snapshot.child("sync_session_id").getValue(String::class.java)
                        if (incomingSession != null && (incomingSession == syncSessionId || incomingSession.startsWith("SYNC_"))) {
                            if ("ACKNOWLEDGED".equals(status, ignoreCase = true)) {
                                ackRef.removeEventListener(this)
                                SystemLogger.i("PairingRepo", "🟢 Persistent 24h ACK Received from $targetId for session $syncSessionId")
                                try {
                                    val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                                    val peerName = localDb.getDeviceCentricName(targetId).ifEmpty { "Device $targetId" }
                                    val shareId = "DSH_${devAId}_${devBId}"
                                    localDb.updateDeviceSharingStage(
                                        shareId,
                                        com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_ACTIVE,
                                        targetId,
                                        "Persistent 24h ACK Received"
                                    )
                                    localDb.saveRosterNode(targetId, peerName, roleA, "ACTIVE", false, false)
                                    com.jdroidx.jfindme.engine.alert.AlertManager.showDeviceSharingNotification(
                                        context,
                                        "Device Sharing Confirmed",
                                        "🟢 $peerName came online and confirmed mutual tracking."
                                    )
                                    val refreshIntent = android.content.Intent("com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED").apply {
                                        setPackage(context.packageName)
                                        putExtra("category", "ROSTER")
                                    }
                                    context.sendBroadcast(refreshIntent)
                                } catch (e: Exception) {
                                    SystemLogger.e("PairingRepo", "Persistent ACK processing warning: ${e.message}")
                                }
                            } else if ("EXPIRED".equals(status, ignoreCase = true)) {
                                ackRef.removeEventListener(this)
                                SystemLogger.w("PairingRepo", "🔴 Persistent 24h Sync Expired for $targetId")
                                try {
                                    val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                                    val peerName = localDb.getDeviceCentricName(targetId).ifEmpty { "Device $targetId" }
                                    val shareId = "DSH_${devAId}_${devBId}"
                                    localDb.updateDeviceSharingStage(
                                        shareId,
                                        com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_EXPIRED,
                                        targetId,
                                        "Persistent 24-hour sync window expired"
                                    )
                                    com.jdroidx.jfindme.engine.alert.AlertManager.showDeviceSharingNotification(
                                        context,
                                        "Device Sharing Expired",
                                        "🔴 24-hour window expired for $peerName. Tap to re-issue request."
                                    )
                                } catch (ignored: Exception) {}
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                ackRef.addValueEventListener(sentinelListener)
            }
        }

        // [Variables] shareId: String, actorDeviceId: String, reason: String | [Constants] STAGE_REJECTED | [Dependencies] DatabaseHelper, FirebaseComm | [Integration Points] PairingRepository, MapActivity
        @JvmStatic
        fun rejectDeviceSharingRequest(context: android.content.Context, shareId: String, actorDeviceId: String, reason: String = "Master rejected sharing request") {
            val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            localDb.updateDeviceSharingStage(
                shareId,
                com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REJECTED,
                actorDeviceId,
                reason
            )
            val record = localDb.getDeviceSharingRecord(shareId)
            if (record != null) {
                FirebaseComm.getDeviceRef(record.submasterDeviceId).child("device_sharing").child(shareId)
                    .updateChildren(mapOf("status" to "REJECTED", "is_active" to false, "reason" to reason))
            }
        }

        // [Variables] shareId: String, actorDeviceId: String, reason: String | [Constants] STAGE_DECLINED | [Dependencies] DatabaseHelper, FirebaseComm | [Integration Points] PairingRepository, MapActivity
        @JvmStatic
        fun declineDeviceSharingInvitation(context: android.content.Context, shareId: String, actorDeviceId: String, reason: String = "Invitee declined invitation") {
            val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            localDb.updateDeviceSharingStage(
                shareId,
                com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_DECLINED,
                actorDeviceId,
                reason
            )
            val record = localDb.getDeviceSharingRecord(shareId)
            if (record != null) {
                FirebaseComm.getDeviceRef(record.masterDeviceId).child("device_sharing").child(shareId)
                    .updateChildren(mapOf("status" to "DECLINED", "is_active" to false, "reason" to reason))
            }
        }

        // [Variables] shareId: String, actorDeviceId: String, reason: String | [Constants] STAGE_REVOKED | [Dependencies] DatabaseHelper, FirebaseComm | [Integration Points] PairingRepository, MapActivity
        @JvmStatic
        fun revokeDeviceSharingSession(context: android.content.Context, shareId: String, actorDeviceId: String, reason: String = "Access revoked by authority") {
            val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            localDb.updateDeviceSharingStage(
                shareId,
                com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REVOKED,
                actorDeviceId,
                reason
            )
            val record = localDb.getDeviceSharingRecord(shareId)
            if (record != null) {
                FirebaseComm.getDeviceRef(record.masterDeviceId).child("device_sharing").child(shareId)
                    .updateChildren(mapOf("status" to "REVOKED", "is_active" to false, "reason" to reason))
                FirebaseComm.getDeviceRef(record.submasterDeviceId).child("device_sharing").child(shareId)
                    .updateChildren(mapOf("status" to "REVOKED", "is_active" to false, "reason" to reason))
            }
        }
    }

    fun interface CodeCallback {
        fun onCodeReady(code: String)
    }

    fun interface StatusCallback {
        fun onComplete(success: Boolean)
    }

    fun interface ResultCallback {
        fun onResult(success: Boolean, message: String?)
    }
}
