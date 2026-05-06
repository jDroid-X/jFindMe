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

    /**
     * 🔗 FIX #1: Removed 'assignedRoles' parameter — the role is negotiated AFTER the child knocks,
     * not at token-generation time. The old "AWAITING_ROLE" dead-write has been eliminated.
     * 🔗 FIX #4: Host listener now performs a fresh .get() server fetch when child knocks,
     * eliminating the Firebase local-cache race condition that caused child_role_preliminary to
     * arrive as null (defaulting to "Friends") before the write fully propagated.
     */
    fun startHostPairingSession(context: android.content.Context, onCodeGenerated: (Bitmap?, String) -> Unit, onChildRequestReceived: (String, String, String) -> Unit) {
        val hostHardwareId = getDeviceId(context)
        
        // 🧬 V59.7: Resolve the Forensic Agent ID before starting the session
        FirebaseComm.resolveHardwareIdentityByHardwareId(hostHardwareId) { hostAgentId ->
            val finalHostId = hostAgentId ?: hostHardwareId
            val inviteCode = generateSecureToken()
            activePairingCode = inviteCode
            
            val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val myDos = dbHelper.getLocalDossier(finalHostId)
            val hostName = myDos?.get("fullName") ?: "Host Admin"

            // 🔗 FIX #1: No 'assignedRoles' field — role is selected by host AFTER child knocks.
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
                            // 🔗 FIX #4: Force a fresh server read to guarantee child_role_preliminary
                            // is fully propagated before the host approval dialog opens.
                            // The ValueEventListener can fire on a cached snapshot that does not yet
                            // contain the child's role write — .get() bypasses the local cache.
                            pairingRequestsEndpoint.child(inviteCode).get().addOnSuccessListener { freshSnap ->
                                val freshChildId   = freshSnap.child("childDeviceId").getValue(String::class.java) ?: ""
                                val childName      = freshSnap.child("child_name").getValue(String::class.java) ?: "Target"
                                val childRole      = freshSnap.child("child_role_preliminary").getValue(String::class.java) ?: ""

                                if (freshChildId.isNotEmpty() && childRole.isNotEmpty()) {
                                    SystemLogger.i(TAG, "Child Knock (Server-Fresh): $freshChildId ($childName) as $childRole")
                                    onChildRequestReceived(freshChildId, childName, childRole)
                                } else {
                                    SystemLogger.e(TAG, "Child role not yet propagated for $freshChildId — skipping")
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
    }

    fun requestJoinGroup(context: android.content.Context, inviteCode: String, selectedRole: String, onStatusUpdate: (Boolean, String) -> Unit) {
        val childHardwareId = getDeviceId(context)
        
        // 🧬 V59.7: Resolve the Forensic Agent ID before requesting the join
        FirebaseComm.resolveHardwareIdentityByHardwareId(childHardwareId) { childAgentId ->
            val finalChildId = childAgentId ?: childHardwareId
            
            val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val myDos = dbHelper.getLocalDossier(finalChildId)
            val childName = myDos?.get("fullName") ?: "Guest"

            pairingRequestsEndpoint.child(inviteCode).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
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

                    listenForAcceptance(context, inviteCode, onStatusUpdate)
                } else {
                    onStatusUpdate(false, "Invalid Code.")
                }
            }.addOnFailureListener {
                onStatusUpdate(false, "Network Frame Error.")
            }
        }
    }

    fun approveHandshake(context: android.content.Context, inviteCode: String, childId: String, hostRole: String, childRole: String, onComplete: (Boolean) -> Unit) {
        val hostHardwareId = getDeviceId(context)
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        
        com.jdroidx.jfindme.utils.AngelLoader.show(context as android.app.Activity)
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(context, "Finalizing Analytical Handshake...")

        pairingRequestsEndpoint.child(inviteCode).get().addOnSuccessListener { handshakeSnap ->
            val childName = handshakeSnap.child("child_name").getValue(String::class.java) ?: "Target"
            val hostName = handshakeSnap.child("host_name").getValue(String::class.java) ?: "Admin"
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                // 🧬 V59.7: Resolve identities before final handshake
                FirebaseComm.resolveHardwareIdentityByHardwareId(hostHardwareId) { hostAgentId ->
                    val finalHostId = hostAgentId ?: hostHardwareId
                    FirebaseComm.resolveHardwareIdentityByHardwareId(childId) { childAgentId ->
                        val finalChildId = childAgentId ?: childId

                        identitiesEndpoint.child(finalHostId).child("Identity").get().addOnSuccessListener { hostIdentitySnap ->
                            val hostGroup = hostIdentitySnap.child("active_group").getValue(String::class.java) ?: "GRP_${System.currentTimeMillis()}"
                            val now = System.currentTimeMillis()
                            val updates = mutableMapOf<String, Any?>()

                            // 1. TIER 3: Relationship Bridge
                            updates["${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroup/members/$finalHostId/role"] = hostRole
                            updates["${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroup/members/$finalHostId/ts_updated"] = now
                            updates["${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroup/members/$finalChildId/role"] = childRole
                            updates["${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroup/members/$finalChildId/ts_joined"] = now

                            // Redundancy: Update Profile Nodes for V60 legacy support
                            updates["${FirebaseComm.NODE_DEVICES}/$finalHostId/profile/role_title"] = hostRole
                            updates["${FirebaseComm.NODE_DEVICES}/$finalChildId/profile/role_title"] = childRole

                            // 2. TIER 2: Device Registry Anchor
                            updates["${FirebaseComm.NODE_DEVICES}/$finalHostId/$hostHardwareId/last_sync"] = FirebaseComm.getForensicTimestamp(now)
                            updates["${FirebaseComm.NODE_DEVICES}/$finalChildId/$childId/last_sync"] = FirebaseComm.getForensicTimestamp(now)

                            // 3. TIER 1: Identity Alignment
                            updates["${FirebaseComm.NODE_DEVICES}/$finalHostId/Identity/active_group"] = hostGroup
                            updates["${FirebaseComm.NODE_DEVICES}/$finalChildId/Identity/active_group"] = hostGroup
                            updates["${FirebaseComm.NODE_DEVICES}/$finalHostId/profile/active_group"] = hostGroup
                            updates["${FirebaseComm.NODE_DEVICES}/$finalChildId/profile/active_group"] = hostGroup

                            // 4. Release Handshake Lock
                            updates["${FirebaseComm.NODE_PAIRING}/$inviteCode/status"] = "approved"
                            updates["${FirebaseComm.NODE_PAIRING}/$inviteCode/host_id"] = finalHostId
                            updates["${FirebaseComm.NODE_PAIRING}/$inviteCode/final_host_role"] = hostRole
                            updates["${FirebaseComm.NODE_PAIRING}/$inviteCode/final_child_role"] = childRole

                            FirebaseDatabase.getInstance().reference.updateChildren(updates).addOnSuccessListener {
                                // HOST side: childRole is the CHILD's assigned role (e.g. "Child", "Spouse").
                                // hostRole is the HOST's own role (e.g. "Father", "Mother").
                                // isFriend is derived from the child's role — symmetric for "Friends".
                                val isChildFriend = (childRole == "Friends")
                                val vRoster = android.content.ContentValues().apply {
                                    put(com.jdroidx.jfindme.data.local.DatabaseHelper.COL_UID_1, finalChildId)
                                    put("fullName", childName)
                                    put("role_type", childRole)          // ✅ Child's own role saved correctly
                                    put("st_session_state", "Analytical Sync Success")
                                    put("is_admin", 0)
                                    put("is_friend", if (isChildFriend) 1 else 0)
                                    put("owner_name", hostName)
                                    put("qr_code_ref", inviteCode)
                                }
                                dbHelper.saveRosterNode(vRoster)
                                dbHelper.logDevicePairing(finalHostId, hostName, null, finalChildId, childName, null, childRole, hostGroup)

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
                    }
                }
            }, 5000)
        }.addOnFailureListener {
            com.jdroidx.jfindme.utils.AngelLoader.hide(context)
            onComplete(false)
        }
    }

    fun rejectHandshake(inviteCode: String) {
        pairingRequestsEndpoint.child(inviteCode).child("status").setValue("rejected")
        clearSession(inviteCode)
    }

    private fun listenForAcceptance(context: android.content.Context, inviteCode: String, onStatusUpdate: (Boolean, String) -> Unit) {
        val childId = getDeviceId(context)
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

                if (assignedChild.isNotEmpty() && assignedChild != childId) return 
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

                        // 🔗 FIX #3: Device B evaluates isFriend from its OWN role (finalChildRole),
                        // NOT from the host's role. Previously used finalHostRole which caused Device B
                        // to incorrectly flag itself as a friend whenever the host happened to be "Friends".
                        val isFriend = finalChildRole.equals("Friends", ignoreCase = true)

                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (hostId.isNotEmpty()) {
                                // 🔗 FIX #3: Device B saves the HOST's entry using finalHostRole as role_type.
                                // This is correct — Device B is recording who the HOST is in its own roster.
                                // The HOST's is_friend flag is also derived from the HOST's role (symmetric).
                                val isHostFriend = finalHostRole.equals("Friends", ignoreCase = true)
                                val vRoster = android.content.ContentValues().apply {
                                    put(com.jdroidx.jfindme.data.local.DatabaseHelper.COL_UID_1, hostId)
                                    put("fullName", hostName)
                                    put("role_type", finalHostRole)     // ✅ Host's role saved in Device B's roster
                                    put("st_session_state", "P2P SYNC COMPLETED")
                                    put("is_admin", 1)
                                    put("is_friend", if (isHostFriend) 1 else 0)
                                    put("owner_name", hostName)
                                    put("qr_code_ref", qrCode)
                                }
                                com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).saveRosterNode(vRoster)

                                // 🔗 FIX #3 + DEFECT 1: Update Device B's own role in group_members.
                                // "OWNER_001" is a display fallback key — it is NOT written to user_identity.uid_1.
                                // resolveInternalId("OWNER_001") always returns -1L → updateCitizenRole silently fails.
                                // Must resolve the actual self-uid from SharedPreferences (set during registration).
                                val prefs = context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
                                val selfUid = prefs.getString("agent_id", null)
                                    ?: com.google.firebase.auth.FirebaseAuth.getInstance().uid

                                if (!selfUid.isNullOrEmpty()) {
                                    val isFriendSelf = finalChildRole.equals("Friends", ignoreCase = true)
                                    com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).updateCitizenRole(
                                        selfUid,
                                        if (isFriendSelf) "None" else finalChildRole,  // famRole
                                        if (isFriendSelf) "Friends" else "None",        // frRole
                                        isFriendSelf
                                    )
                                }
                            }
                            com.jdroidx.jfindme.utils.AngelLoader.hide(context)
                            onStatusUpdate(true, "FULLY_SYNCED")
                            pairingRequestsEndpoint.child(inviteCode).removeEventListener(this)
                        }, 5000)
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
        val hwId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "FALLBACK_HW"
        return "HW_" + hwId.uppercase()
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
