package com.jdroidx.jfindme.engine.sync

import android.graphics.Bitmap
import android.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.jdroidx.jfindme.utils.SystemLogger
import com.google.firebase.database.DatabaseReference
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * 📡 OPTION A — Firebase Pairing (Remote)
 * Strict Distributed Systems implementation bridging 'pairing_requests' -> 'groups' -> 'locations'
 * Integrates Mandatory Handshake Approval stopping rogue devices from secretly tracking groups.
 */
object FirebasePairingManager {
    private const val TAG = "FirebasePairingManager"

    private val identitiesEndpoint = FirebaseComm.devicesRef
    private val profilesEndpoint = FirebaseComm.devicesRef
    private val devicesEndpoint = FirebaseComm.devicesRef
    private val groupsEndpoint = FirebaseComm.groupsRef           // Tier 3: Communities
    private val relationshipsEndpoint = FirebaseComm.relationshipsRef // Tier 3: P2P Links
    private val pairingRequestsEndpoint = FirebaseComm.pairingRef // Handshake bridge

    // The Active Host Listener Reference (Kept in memory for garbage collection during Activity pause)
    private var activeHostListener: ValueEventListener? = null
    var activePairingCode: String? = null

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Persistent SharedPreferences Brute-Force Lockout (survives process kills and app restarts)
    private const val PREFS_PIN_LOCKOUT = "pin_lockout_registry"
    private const val MAX_PIN_FAILURES = 5
    private const val PIN_LOCKOUT_MS = 15 * 60 * 1000L // 15 minutes lockout

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Dual Monotonic + Wall Clock Protection against Local Date/Time Tampering
    private fun getLockoutRecord(context: android.content.Context, code: String): Pair<Int, Long> {
        val prefs = context.getSharedPreferences(PREFS_PIN_LOCKOUT, android.content.Context.MODE_PRIVATE)
        val count = prefs.getInt("fail_count_$code", 0)
        val ts = prefs.getLong("fail_ts_$code", 0L)
        val mono = prefs.getLong("fail_mono_$code", 0L)
        val now = System.currentTimeMillis()
        val nowMono = android.os.SystemClock.elapsedRealtime()

        if (count >= MAX_PIN_FAILURES) {
            if (mono > 0 && nowMono >= mono && (nowMono - mono < PIN_LOCKOUT_MS)) {
                return Pair(count, ts)
            }
        }

        if (count > 0 && ts > 0 && (now - ts >= PIN_LOCKOUT_MS)) {
            clearPinFailure(context, code)
            return Pair(0, 0L)
        }
        return Pair(count, ts)
    }

    private fun recordPinFailure(context: android.content.Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS_PIN_LOCKOUT, android.content.Context.MODE_PRIVATE)
        val current = getLockoutRecord(context, code)
        prefs.edit()
            .putInt("fail_count_$code", current.first + 1)
            .putLong("fail_ts_$code", System.currentTimeMillis())
            .putLong("fail_mono_$code", android.os.SystemClock.elapsedRealtime())
            .apply()
    }

    private fun clearPinFailure(context: android.content.Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS_PIN_LOCKOUT, android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("fail_count_$code").remove("fail_ts_$code").remove("fail_mono_$code").apply()
    }



    /**
     * 🔗 FIX #1: Removed 'assignedRoles' parameter — the role is negotiated AFTER the child knocks,
     * not at token-generation time. The old "AWAITING_ROLE" dead-write has been eliminated.
     * 🔗 FIX #4: Host listener now performs a fresh .get() server fetch when child knocks,
     * eliminating the Firebase local-cache race condition that caused child_role_preliminary to
     * arrive as null (defaulting to "Friends") before the write fully propagated.
     */
    fun startHostPairingSession(context: android.content.Context, onCodeGenerated: (Bitmap?, String) -> Unit, onChildRequestReceived: (String, String, String) -> Unit) {
        val finalHostId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val inviteCode = generateSecureToken()
        activePairingCode = inviteCode
        
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val myDos = dbHelper.getLocalDossier(finalHostId)
        val hostName = myDos?.get("fullName") ?: com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(context)

        val payload = mapOf(
            "parentDeviceId" to finalHostId,
            "host_name"      to hostName,
            "childDeviceId"  to "",
            "status"         to "waiting"
        )

        pairingRequestsEndpoint.child(inviteCode).setValue(payload).addOnSuccessListener {
            val qr = constructQrGraphic(inviteCode)
            onCodeGenerated(qr, inviteCode)

            activeHostListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status  = snapshot.child("status").getValue(String::class.java)
                    val childId = snapshot.child("childDeviceId").getValue(String::class.java)

                    if (status == "pending" && !childId.isNullOrEmpty()) {
                        pairingRequestsEndpoint.child(inviteCode).get().addOnSuccessListener { freshSnap ->
                            val freshChildId   = freshSnap.child("childDeviceId").getValue(String::class.java) ?: ""
                            val childName      = freshSnap.child("child_name").getValue(String::class.java) ?: "Target"
                            val childRoleRaw   = freshSnap.child("child_role_preliminary").getValue(String::class.java)
                            val childRole      = if (!childRoleRaw.isNullOrBlank()) childRoleRaw else "Member"

                            if (freshChildId.isNotEmpty()) {
                                SystemLogger.i(TAG, "Child Knock (Server-Fresh): $freshChildId ($childName) as $childRole")
                                onChildRequestReceived(freshChildId, childName, childRole)
                            }
                        }.addOnFailureListener { ex ->
                            SystemLogger.e(TAG, "Fresh fetch failed for knock: ${ex.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    SystemLogger.e(TAG, "Host listener collapsed", error.toException())
                }
            }
            pairingRequestsEndpoint.child(inviteCode).addValueEventListener(activeHostListener!!)
        }.addOnFailureListener {
            onCodeGenerated(null, "")
        }
    }

    fun requestJoinGroup(context: android.content.Context, inviteCode: String, selectedRole: String, onStatusUpdate: (Boolean, String) -> Unit) {
        val now = System.currentTimeMillis()
        val failureRecord = getLockoutRecord(context, inviteCode)
        if (failureRecord.first >= MAX_PIN_FAILURES && (now - failureRecord.second) < PIN_LOCKOUT_MS) {
            val remainingMins = ((PIN_LOCKOUT_MS - (now - failureRecord.second)) / 60000L).coerceAtLeast(1)
            onStatusUpdate(false, "PIN temporarily locked due to failed attempts. Retry in $remainingMins mins.")
            return
        }

        val finalChildId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val myDos = dbHelper.getLocalDossier(finalChildId)
        val childName = myDos?.get("fullName") ?: com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(context)

        pairingRequestsEndpoint.child(inviteCode).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                clearPinFailure(context, inviteCode) // 🛡️ Reset attempt counter on authentic PIN entry
                val status = snapshot.child("status").getValue(String::class.java)
                val assignedChild = snapshot.child("childDeviceId").getValue(String::class.java) ?: ""

                if (status == "waiting") {
                    pairingRequestsEndpoint.child(inviteCode).updateChildren(
                        mapOf(
                            "childDeviceId" to finalChildId,
                            "child_name" to childName,
                            "child_role_preliminary" to selectedRole,
                            "status" to "pending",
                            "qr_code" to inviteCode
                        )
                    )
                } else if (assignedChild != finalChildId && status != "waiting") {
                     onStatusUpdate(false, "Code is already actively pairing with another device.")
                     return@addOnSuccessListener
                }

                listenForAcceptance(context, inviteCode, finalChildId, onStatusUpdate)
            } else {
                recordPinFailure(context, inviteCode)
                val (curCount, _) = getLockoutRecord(context, inviteCode)
                if (curCount >= MAX_PIN_FAILURES) {
                    onStatusUpdate(false, "Too many failed attempts ($curCount/$MAX_PIN_FAILURES). PIN locked for 15 mins.")
                } else {
                    onStatusUpdate(false, "Invalid Code ($curCount/$MAX_PIN_FAILURES attempts).")
                }
            }
        }.addOnFailureListener {
            onStatusUpdate(false, "Network Frame Error.")
        }
    }

    fun approveHandshake(context: android.content.Context, inviteCode: String, childId: String, hostRole: String, childRole: String, onComplete: (Boolean) -> Unit) {
        val finalHostId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val finalChildId = childId
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        
        com.jdroidx.jfindme.utils.AngelLoader.show(context as android.app.Activity)
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(context, "Finalizing Analytical Handshake...")

        pairingRequestsEndpoint.child(inviteCode).get().addOnSuccessListener { handshakeSnap ->
            val childName = handshakeSnap.child("child_name").getValue(String::class.java) ?: "Target"
            val hostName = handshakeSnap.child("host_name").getValue(String::class.java) ?: "Admin"
            
            identitiesEndpoint.child(finalHostId).child("Identity").get().addOnSuccessListener { hostIdentitySnap ->
                val hostGroup = hostIdentitySnap.child("active_group").getValue(String::class.java) ?: "GRP_${System.currentTimeMillis()}"
                val now = System.currentTimeMillis()
                val cleanHostGroup = FirebaseComm.sanitizePathKey(hostGroup)
                val cleanHostId = FirebaseComm.sanitizePathKey(finalHostId)
                val cleanChildId = FirebaseComm.sanitizePathKey(finalChildId)
                val cleanInviteCode = FirebaseComm.sanitizePathKey(inviteCode)

                val updates = mutableMapOf<String, Any?>()

                // 1. TIER 3: Relationship Bridge - Unified `devices/` Structure + Host Pointer
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/group_info/host_device_id"] = cleanHostId
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/group_info/group_name"] = "Primary Circle"
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/group_info/created_at"] = now

                // Host Unified Device Node
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanHostId/identity/name"] = hostName
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanHostId/identity/full_name"] = hostName
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanHostId/membership/role"] = hostRole
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanHostId/membership/status"] = "Active"
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanHostId/membership/ts_updated"] = now
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanHostId/paired_with/$cleanChildId"] = true

                // Child Unified Device Node
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanChildId/identity/name"] = childName
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanChildId/identity/full_name"] = childName
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanChildId/membership/role"] = childRole
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanChildId/membership/status"] = "Active"
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanChildId/membership/ts_joined"] = now
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/devices/$cleanChildId/paired_with/$cleanHostId"] = true

                // Legacy Redundancy for backwards compatibility
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/members/$cleanHostId/role"] = hostRole
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/members/$cleanHostId/ts_updated"] = now
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/members/$cleanChildId/role"] = childRole
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/members/$cleanChildId/ts_joined"] = now

                // Reciprocal P2P Direct Pairing authorization
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/direct_pairs/$cleanHostId/$cleanChildId"] = true
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/direct_pairs/$cleanChildId/$cleanHostId"] = true

                // 🧬 V68.0 Device-Centric Relationships Sub-Group Mesh
                updates["${FirebaseComm.NODE_DEVICES}/$cleanHostId/relationships/$cleanChildId"] = mapOf(
                    "role" to childRole,
                    "name" to childName,
                    "group" to cleanHostGroup,
                    "linked_at" to FirebaseComm.getForensicTimestamp(now)
                )
                updates["${FirebaseComm.NODE_DEVICES}/$cleanChildId/relationships/$cleanHostId"] = mapOf(
                    "role" to hostRole,
                    "name" to hostName,
                    "group" to cleanHostGroup,
                    "linked_at" to FirebaseComm.getForensicTimestamp(now)
                )

                // Redundancy: Update Profile Nodes for V60 legacy support
                updates["${FirebaseComm.NODE_DEVICES}/$cleanHostId/profile/role_title"] = hostRole
                // 2. TIER 2: Device Registry Anchor
                updates["${FirebaseComm.NODE_DEVICES}/$cleanHostId/last_sync"] = FirebaseComm.getForensicTimestamp(now)
                updates["${FirebaseComm.NODE_DEVICES}/$cleanChildId/last_sync"] = FirebaseComm.getForensicTimestamp(now)

                // 3. TIER 1: Identity Alignment
                updates["${FirebaseComm.NODE_DEVICES}/$cleanHostId/Identity/active_group"] = cleanHostGroup
                updates["${FirebaseComm.NODE_DEVICES}/$cleanChildId/Identity/active_group"] = cleanHostGroup
                updates["${FirebaseComm.NODE_DEVICES}/$cleanHostId/profile/active_group"] = cleanHostGroup
                updates["${FirebaseComm.NODE_DEVICES}/$cleanChildId/profile/active_group"] = cleanHostGroup
                // Rapid Inverse Index Alignment for multi-circle recovery
                updates["users/$cleanHostId/joined_circles/$cleanHostGroup"] = true
                updates["users/$cleanChildId/joined_circles/$cleanHostGroup"] = true

                // 4. Release Handshake Lock
                updates["${FirebaseComm.NODE_PAIRING}/$cleanInviteCode/status"] = "approved"
                updates["${FirebaseComm.NODE_PAIRING}/$cleanInviteCode/host_id"] = cleanHostId
                updates["${FirebaseComm.NODE_PAIRING}/$cleanInviteCode/final_host_role"] = hostRole
                updates["${FirebaseComm.NODE_PAIRING}/$cleanInviteCode/final_child_role"] = childRole
                updates["${FirebaseComm.NODE_PAIRING}/$cleanInviteCode/active_group"] = cleanHostGroup
                // 5. Trigger Real-time Roster Signal
                updates["${FirebaseComm.NODE_RELATIONSHIPS}/$cleanHostGroup/signals/refresh_roster"] = now

                FirebaseDatabase.getInstance().reference.updateChildren(updates).addOnSuccessListener {
                    val isChildFriend = (childRole.equals("Friends", ignoreCase = true) || childRole.equals("Friend", ignoreCase = true))
                    val vRoster = android.content.ContentValues().apply {
                        put(com.jdroidx.jfindme.data.local.DatabaseHelper.COL_UID_1, finalChildId)
                        put("fullName", childName)
                        put("role_type", childRole)
                        put("st_session_state", "Analytical Sync Success")
                        put("is_admin", 0)
                        put("is_friend", if (isChildFriend) 1 else 0)
                        put("owner_name", hostName)
                        put("qr_code_ref", inviteCode)
                        put("group_id", cleanHostGroup)
                    }
                    dbHelper.saveRosterNode(vRoster)
                    dbHelper.logDevicePairing(finalHostId, hostName, null, finalChildId, childName, null, childRole, cleanHostGroup)

                    com.jdroidx.jfindme.utils.AngelLoader.hide(context)
                    onComplete(true)
                    clearSession(inviteCode)
                }.addOnFailureListener { 
                    com.jdroidx.jfindme.utils.AngelLoader.hide(context)
                    onComplete(false) 
                }
            }.addOnFailureListener {
                com.jdroidx.jfindme.utils.AngelLoader.hide(context)
                onComplete(false)
            }
        }.addOnFailureListener {
            com.jdroidx.jfindme.utils.AngelLoader.hide(context)
            onComplete(false)
        }
    }

    fun rejectHandshake(inviteCode: String) {
        pairingRequestsEndpoint.child(inviteCode).child("status").setValue("rejected")
        clearSession(inviteCode)
    }

    private fun listenForAcceptance(context: android.content.Context, inviteCode: String, finalChildId: String, onStatusUpdate: (Boolean, String) -> Unit) {
        var lastNotifiedStatus: String? = null

        pairingRequestsEndpoint.child(inviteCode).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onStatusUpdate(false, "Handshake Session Expired.")
                    pairingRequestsEndpoint.child(inviteCode).removeEventListener(this)
                    return
                }

                val status = snapshot.child("status").getValue(String::class.java)
                val assignedChild = snapshot.child("childDeviceId").getValue(String::class.java) ?: ""

                if (assignedChild.isNotEmpty() && assignedChild != finalChildId) return 
                if (status == lastNotifiedStatus) return
                lastNotifiedStatus = status

                when (status) {
                    "pending" -> onStatusUpdate(true, "PENDING_HANDSHAKE")
                    "approved" -> {
                        com.jdroidx.jfindme.utils.AngelLoader.show(context as android.app.Activity)
                        com.jdroidx.jfindme.utils.AngelLoader.setStatus(context, "Receiving Forensic Metadata...")

                        val hostId        = snapshot.child("host_id").getValue(String::class.java) ?: ""
                        val hostName      = snapshot.child("host_name").getValue(String::class.java) ?: "Admin"
                        val qrCode        = snapshot.child("qr_code").getValue(String::class.java) ?: inviteCode
                        val finalChildRole = snapshot.child("final_child_role").getValue(String::class.java) ?: "Member"
                        val finalHostRole  = snapshot.child("final_host_role").getValue(String::class.java) ?: "Guardian"
                        val activeGroup    = snapshot.child("active_group").getValue(String::class.java) ?: ""

                        // 🔗 FIX #3: Device B evaluates isFriend from its OWN role (finalChildRole),
                        // NOT from the host's role. Previously used finalHostRole which caused Device B
                        // to incorrectly flag itself as a friend whenever the host happened to be "Friends".
                        val isFriend = finalChildRole.equals("Friends", ignoreCase = true)

                        if (hostId.isNotEmpty()) {
                            val isHostFriend = finalHostRole.equals("Friends", ignoreCase = true) || finalHostRole.equals("Friend", ignoreCase = true)
                            val effectiveGroup = activeGroup.ifEmpty { "DEFAULT_GROUP" }
                            val vRoster = android.content.ContentValues().apply {
                                put(com.jdroidx.jfindme.data.local.DatabaseHelper.COL_UID_1, hostId)
                                put("fullName", hostName)
                                put("role_type", finalHostRole)
                                put("st_session_state", "P2P SYNC COMPLETED")
                                put("is_admin", 1)
                                put("is_friend", if (isHostFriend) 1 else 0)
                                put("owner_name", hostName)
                                put("qr_code_ref", qrCode)
                                put("group_id", effectiveGroup)
                            }
                            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).saveRosterNode(vRoster)
                            
                            val myChildName = snapshot.child("child_name").getValue(String::class.java) ?: "Target"
                            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).logDevicePairing(
                                hostId, hostName, null, finalChildId, myChildName, null, finalHostRole, effectiveGroup
                            )

                            val prefs = context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
                            val selfUid = prefs.getString("agent_id", null)
                                ?: com.google.firebase.auth.FirebaseAuth.getInstance().uid

                            if (!selfUid.isNullOrEmpty()) {
                                val isFriendSelf = finalChildRole.equals("Friends", ignoreCase = true)
                                com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).updateCitizenRole(
                                    selfUid,
                                    if (isFriendSelf) "None" else finalChildRole,
                                    if (isFriendSelf) "Friends" else "None",
                                    isFriendSelf,
                                    activeGroup.ifEmpty { "DEFAULT_GROUP" }
                                )
                            }

                            if (activeGroup.isNotEmpty()) {
                                prefs.edit().putString("active_group", activeGroup).apply()
                            }
                        }
                        com.jdroidx.jfindme.utils.AngelLoader.hide(context)
                        onStatusUpdate(true, "FULLY_SYNCED")
                        pairingRequestsEndpoint.child(inviteCode).removeEventListener(this)
                    }
                    "rejected" -> {
                        onStatusUpdate(false, "Parent Rejected Link.")
                        pairingRequestsEndpoint.child(inviteCode).removeEventListener(this)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onStatusUpdate(false, "Cloud Hub collapsed: ${error.message}")
            }
        })
    }

    fun cancelActiveSession() {
        val code = activePairingCode
        if (code != null) {
            clearSession(code)
        }
    }

    private fun clearSession(code: String) {
        if (activeHostListener != null) {
            pairingRequestsEndpoint.child(code).removeEventListener(activeHostListener!!)
            activeHostListener = null
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            pairingRequestsEndpoint.child(code).removeValue()
        }, 3500)
        activePairingCode = null
    }

    fun getDeviceId(context: android.content.Context): String {
        return com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(context)
    }

    private fun generateSecureToken(): String {
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { allowedChars.random() }.joinToString("")
    }

    private fun constructQrGraphic(payloadText: String): Bitmap? {
        return try {
            val size = 512
            val bitMatrix = QRCodeWriter().encode(payloadText, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) { null }
    }
}
