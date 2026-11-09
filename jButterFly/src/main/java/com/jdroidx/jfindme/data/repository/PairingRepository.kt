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
                    if (session != null && session.status == "pending" && session.expiresAt > System.currentTimeMillis()) {
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

        @JvmStatic
        fun executeFamilyHandshake(hostAgentId: String, hostGroupId: String, targetAgentId: String, roleFam: String, onSuccess: () -> Unit) {
            if (!FirebaseComm.isAgentIdValid(hostAgentId) || !FirebaseComm.isAgentIdValid(targetAgentId)) {
                SystemLogger.e("PairingRepo", "BLOCKED: Handshake with invalid Agent IDs: $hostAgentId -> $targetAgentId")
                return
            }
            
            // 🚀 V59.7: Master Forensic Handshake (Anchored to Hub Agent)
            db.child(FirebaseComm.NODE_DEVICES).child(hostAgentId).child("identity").child("valid_linked_agents").get().addOnSuccessListener { snapshot ->
                val currentCsv = snapshot.getValue(String::class.java) ?: ""
                val approvedIds = currentCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                
                if (!approvedIds.contains(targetAgentId)) {
                    approvedIds.add(targetAgentId)
                }
                val newSecureCsv = approvedIds.joinToString(",")

                val updates = hashMapOf<String, Any>(
                    "${FirebaseComm.NODE_DEVICES}/$targetAgentId/Identity/active_group" to hostGroupId,
                    "${FirebaseComm.NODE_DEVICES}/$hostAgentId/Identity/valid_linked_agents" to newSecureCsv,
                    
                    // 🧬 V68.0 Device-Centric Relationships Sub-Group Mesh
                    "${FirebaseComm.NODE_DEVICES}/$hostAgentId/relationships/$targetAgentId" to mapOf(
                        "role" to roleFam,
                        "group" to hostGroupId,
                        "linked_at" to FirebaseComm.getForensicTimestamp()
                    ),
                    "${FirebaseComm.NODE_DEVICES}/$targetAgentId/relationships/$hostAgentId" to mapOf(
                        "role" to "GUARDIAN",
                        "group" to hostGroupId,
                        "linked_at" to FirebaseComm.getForensicTimestamp()
                    ),

                    // Uniform devices/ + group_info standard
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/group_info/host_device_id" to hostAgentId,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/group_info/group_name" to "Primary Circle",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/group_info/created_at" to FirebaseComm.getForensicTimestamp(),
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$hostAgentId/identity/name" to (JFindMeApplication.getContext()?.let { RegistryManager.getUserNickname(it) } ?: hostAgentId),
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$hostAgentId/identity/full_name" to (JFindMeApplication.getContext()?.let { RegistryManager.getUserNickname(it) } ?: hostAgentId),
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$hostAgentId/membership/role" to "ADMIN",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$hostAgentId/membership/role_family" to "Parent",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$hostAgentId/membership/role_friend" to "None",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$hostAgentId/membership/status" to "Active",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$hostAgentId/membership/ts_joined" to FirebaseComm.getForensicTimestamp(),
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$hostAgentId/paired_with/$targetAgentId" to true,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$targetAgentId/identity/name" to targetAgentId,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$targetAgentId/identity/full_name" to targetAgentId,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$targetAgentId/membership/role" to roleFam,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$targetAgentId/membership/role_family" to roleFam,
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$targetAgentId/membership/role_friend" to "None",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$targetAgentId/membership/status" to "Active",
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$targetAgentId/membership/ts_joined" to FirebaseComm.getForensicTimestamp(),
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/devices/$targetAgentId/paired_with/$hostAgentId" to true
                )
                
                db.updateChildren(updates).addOnSuccessListener {
                    SystemLogger.w("PairingRepo", "V68.0 Handshake: Target Agent Matrix Constructed under $hostAgentId")
                    
                    // 📝 AUDIT CHANGE LOGS: Record Pairing and Family/Friends mutations
                    val pairingDetails = mapOf("target_agent" to targetAgentId, "host_group" to hostGroupId, "role" to roleFam)
                    FirebaseComm.logChange(hostAgentId, FirebaseComm.LOG_ITEM_PAIRING, "DEVICE_PAIR_LINK", targetAgentId, pairingDetails)
                    FirebaseComm.logChange(hostAgentId, FirebaseComm.LOG_ITEM_FAMILY_FRIENDS, "MEMBER_LINK", targetAgentId, pairingDetails)
                    FirebaseComm.logChange(targetAgentId, FirebaseComm.LOG_ITEM_PAIRING, "DEVICE_PAIR_LINK", hostAgentId, mapOf("host_agent" to hostAgentId, "role" to "GUARDIAN"))
                    
                    onSuccess()
                }
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
        @JvmStatic
        @JvmOverloads
        fun linkCoParentToChild(
            hostGroupId: String,
            coParentAgentId: String,
            childAgentId: String,
            coParentName: String = "",
            childName: String = "",
            onComplete: ((Boolean, String?) -> Unit)? = null
        ) {
            val cleanCoParent = FirebaseComm.sanitizeAgentId(coParentAgentId)
            val cleanChild = FirebaseComm.sanitizeAgentId(childAgentId)
            if (!FirebaseComm.isAgentIdValid(cleanCoParent) || !FirebaseComm.isAgentIdValid(cleanChild)) {
                onComplete?.invoke(false, "Invalid Parent or Child Agent ID.")
                return
            }
            val now = FirebaseComm.getForensicTimestamp()
            val effectiveGroup = if (hostGroupId.isNotBlank() && hostGroupId != "DEFAULT_GROUP") hostGroupId else "GRP_$cleanCoParent"

            // 1. Fetch co-parent's valid_linked_agents to append childAgentId
            db.child(FirebaseComm.NODE_DEVICES).child(cleanCoParent).child("identity").child("valid_linked_agents").get()
                .addOnSuccessListener { snapshot ->
                    val currentCsv = snapshot.getValue(String::class.java) ?: ""
                    val approvedIds = currentCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                    if (!approvedIds.contains(cleanChild)) {
                        approvedIds.add(cleanChild)
                    }
                    val newSecureCsv = approvedIds.joinToString(",")

                    val updates = hashMapOf<String, Any>(
                        // Device-Centric Relationships Mesh
                        "${FirebaseComm.NODE_DEVICES}/$cleanCoParent/relationships/$cleanChild" to mapOf(
                            "role" to "Child",
                            "group" to effectiveGroup,
                            "linked_at" to now
                        ),
                        "${FirebaseComm.NODE_DEVICES}/$cleanChild/relationships/$cleanCoParent" to mapOf(
                            "role" to "GUARDIAN",
                            "group" to effectiveGroup,
                            "linked_at" to now
                        ),
                        "${FirebaseComm.NODE_DEVICES}/$cleanCoParent/dependents/$cleanChild" to true,
                        "${FirebaseComm.NODE_DEVICES}/$cleanChild/guardians/$cleanCoParent" to true,
                        "${FirebaseComm.NODE_DEVICES}/$cleanCoParent/identity/valid_linked_agents" to newSecureCsv,

                        // Circle-level device pairing
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanCoParent/paired_with/$cleanChild" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/devices/$cleanChild/paired_with/$cleanCoParent" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/members/$cleanChild/co_guardians/$cleanCoParent" to true,
                        "${FirebaseComm.NODE_RELATIONSHIPS}/$effectiveGroup/signals/refresh_roster" to System.currentTimeMillis()
                    )

                    db.updateChildren(updates).addOnSuccessListener {
                        SystemLogger.i("PairingRepo", "👨‍👩‍👧 Co-Parent Link Established: Parent $cleanCoParent <-> Child $cleanChild in $effectiveGroup")
                        val logDetails = mapOf("co_parent" to cleanCoParent, "child" to cleanChild, "group" to effectiveGroup)
                        FirebaseComm.logChange(cleanCoParent, FirebaseComm.LOG_ITEM_FAMILY_FRIENDS, "CO_PARENT_LINK", cleanChild, logDetails)
                        FirebaseComm.logChange(cleanChild, FirebaseComm.LOG_ITEM_FAMILY_FRIENDS, "GUARDIAN_LINK", cleanCoParent, logDetails)
                        onComplete?.invoke(true, null)
                    }.addOnFailureListener { e ->
                        SystemLogger.e("PairingRepo", "Failed to update co-parent relationship: ${e.message}", e)
                        onComplete?.invoke(false, e.message)
                    }
                }.addOnFailureListener { e ->
                    SystemLogger.e("PairingRepo", "Failed reading co-parent linked agents: ${e.message}", e)
                    onComplete?.invoke(false, e.message)
                }
        }

        @JvmStatic
        fun linkCoParentToChild(
            hostGroupId: String,
            coParentAgentId: String,
            childAgentId: String,
            coParentName: String,
            childName: String,
            callback: ResultCallback
        ) {
            linkCoParentToChild(hostGroupId, coParentAgentId, childAgentId, coParentName, childName) { success, msg ->
                callback.onResult(success, msg)
            }
        }

        /**
         * 🔑 Generate 6-Character Dynamic Circle Invite Code (48-hour validity).
         */
        @JvmStatic
        fun createCircleInviteCode(hostAgentId: String, hostGroupId: String, role: String = "MEMBER", onCodeReady: (String) -> Unit) {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val inviteCode = (1..6).map { chars.random() }.joinToString("")
            val expiryMs = System.currentTimeMillis() + (48 * 60 * 60 * 1000L) // 48 Hours Expiry
            
            val payload = mapOf(
                "code" to inviteCode,
                "host_agent" to hostAgentId,
                "group_id" to hostGroupId,
                "role" to role,
                "expires_at" to expiryMs,
                "created_at" to FirebaseComm.getForensicTimestamp(),
                "status" to "ACTIVE"
            )
            
            FirebaseComm.database.getReference("circle_invites").child(inviteCode).setValue(payload)
                .addOnSuccessListener {
                    SystemLogger.i("PairingRepo", "🔑 Circle Invite Code Generated: $inviteCode for Group $hostGroupId")
                    onCodeReady(inviteCode)
                }
                .addOnFailureListener { e ->
                    SystemLogger.e("PairingRepo", "Failed to generate Circle Invite Code", e)
                }
        }

        @JvmStatic
        fun createCircleInviteCode(hostAgentId: String, hostGroupId: String, role: String, callback: CodeCallback) {
            createCircleInviteCode(hostAgentId, hostGroupId, role) { callback.onCodeReady(it) }
        }

        /**
         * 👥 Join Circle using 6-Character Dynamic Code.
         */
        @JvmStatic
        fun joinCircleWithInviteCode(inviteCode: String, joiningAgentId: String, onResult: (Boolean, String?) -> Unit) {
            val cleanCode = inviteCode.trim().uppercase()
            val inviteRef = FirebaseComm.database.getReference("circle_invites").child(cleanCode)
            
            inviteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        onResult(false, "Invalid Invite Code")
                        return
                    }
                    val status = snapshot.child("status").getValue(String::class.java) ?: "EXPIRED"
                    val expiresAt = snapshot.child("expires_at").getValue(Long::class.java) ?: 0L
                    val hostAgent = snapshot.child("host_agent").getValue(String::class.java) ?: ""
                    val groupId = snapshot.child("group_id").getValue(String::class.java) ?: ""
                    val role = snapshot.child("role").getValue(String::class.java) ?: "MEMBER"
                    
                    if (status != "ACTIVE" || expiresAt < System.currentTimeMillis()) {
                        onResult(false, "This invite code has expired or is no longer active")
                        return
                    }
                    
                    // Execute handshake
                    executeFamilyHandshake(hostAgent, groupId, joiningAgentId, role) {
                        SystemLogger.i("PairingRepo", "🎉 Joined Circle via Code $cleanCode: $joiningAgentId -> $groupId")
                        onResult(true, groupId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(false, error.message)
                }
            })
        }

        @JvmStatic
        fun joinCircleWithInviteCode(inviteCode: String, joiningAgentId: String, callback: ResultCallback) {
            joinCircleWithInviteCode(inviteCode, joiningAgentId) { success, msg -> callback.onResult(success, msg) }
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
