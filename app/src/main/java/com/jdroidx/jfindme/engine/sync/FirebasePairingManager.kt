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

    fun startHostPairingSession(context: android.content.Context, assignedRoles: String, onCodeGenerated: (Bitmap?, String) -> Unit, onChildRequestReceived: (String, String) -> Unit) {
        val hostHardwareId = getDeviceId(context)
        
        // 🧬 V59.7: Resolve the Forensic Agent ID before starting the session
        FirebaseComm.resolveHardwareIdentityByHardwareId(hostHardwareId) { hostAgentId ->
            val finalHostId = hostAgentId ?: hostHardwareId
            val inviteCode = generateSecureToken()
            activePairingCode = inviteCode
            
            val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val myDos = dbHelper.getLocalDossier(finalHostId)
            val hostName = myDos?.get("id_full_name") ?: "Host Admin"

            val payload = mapOf(
                "parentDeviceId" to finalHostId,
                "host_name" to hostName,
                "childDeviceId" to "",
                "status" to "waiting",
                "assignedRoles" to assignedRoles 
            )

            pairingRequestsEndpoint.child(inviteCode).setValue(payload).addOnSuccessListener {
                val qr = constructQrGraphic(inviteCode)
                onCodeGenerated(qr, inviteCode)

                activeHostListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val status = snapshot.child("status").getValue(String::class.java)
                        val childId = snapshot.child("childDeviceId").getValue(String::class.java)
                        val childName = snapshot.child("child_name").getValue(String::class.java) ?: "Target"

                        if (status == "pending" && !childId.isNullOrEmpty()) {
                            SystemLogger.i(TAG, "Child Handshake Intercepted: $childId ($childName)")
                            onChildRequestReceived(childId, childName)
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
            val childName = myDos?.get("id_full_name") ?: "Guest"

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

                        identitiesEndpoint.child(finalHostId).child("identity").get().addOnSuccessListener { hostIdentitySnap ->
                            val hostGroup = hostIdentitySnap.child("active_group").getValue(String::class.java) ?: "GRP_${System.currentTimeMillis()}"
                            val now = System.currentTimeMillis()
                            val updates = mutableMapOf<String, Any?>()

                            // 1. TIER 3: Relationship Bridge
                            updates["${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroup/members/$finalHostId/role"] = "ADMIN"
                            updates["${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroup/members/$finalHostId/ts_updated"] = now
                            updates["${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroup/members/$finalChildId/role"] = childRole
                            updates["${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroup/members/$finalChildId/ts_joined"] = now

                            // 2. TIER 2: Device Registry Anchor
                            updates["${FirebaseComm.NODE_DEVICES}/$finalHostId/$hostHardwareId/last_sync"] = FirebaseComm.getForensicTimestamp(now)
                            updates["${FirebaseComm.NODE_DEVICES}/$finalChildId/$childId/last_sync"] = FirebaseComm.getForensicTimestamp(now)

                            // 3. TIER 1: Identity Alignment
                            updates["${FirebaseComm.NODE_DEVICES}/$finalHostId/identity/active_group"] = hostGroup
                            updates["${FirebaseComm.NODE_DEVICES}/$finalChildId/identity/active_group"] = hostGroup
                            updates["${FirebaseComm.NODE_DEVICES}/$finalHostId/profile/active_group"] = hostGroup
                            updates["${FirebaseComm.NODE_DEVICES}/$finalChildId/profile/active_group"] = hostGroup

                            // 4. Release Handshake Lock
                            updates["${FirebaseComm.NODE_PAIRING}/$inviteCode/status"] = "approved"
                            updates["${FirebaseComm.NODE_PAIRING}/$inviteCode/host_id"] = finalHostId
                            updates["${FirebaseComm.NODE_PAIRING}/$inviteCode/final_host_role"] = hostRole
                            updates["${FirebaseComm.NODE_PAIRING}/$inviteCode/final_child_role"] = childRole

                            FirebaseDatabase.getInstance().reference.updateChildren(updates).addOnSuccessListener {
                                val vRoster = android.content.ContentValues().apply {
                                    put(com.jdroidx.jfindme.data.local.DatabaseHelper.COL_UID_1, finalChildId)
                                    put("id_full_name", childName)
                                    put("role_type", childRole)
                                    put("st_session_state", "Analytical Sync Success")
                                    put("is_admin", 0)
                                    put("is_friend", if(childRole == "Friends") 1 else 0)
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

                        val hostId = snapshot.child("host_id").getValue(String::class.java) ?: ""
                        val hostName = snapshot.child("host_name").getValue(String::class.java) ?: "Admin"
                        val qrCode = snapshot.child("qr_code").getValue(String::class.java) ?: inviteCode
                        val finalChildRole = snapshot.child("final_child_role").getValue(String::class.java) ?: "Member"
                        val isFriend = finalChildRole.equals("Friends", ignoreCase = true)

                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (hostId.isNotEmpty()) {
                                val vRoster = android.content.ContentValues().apply {
                                    put(com.jdroidx.jfindme.data.local.DatabaseHelper.COL_UID_1, hostId)
                                    put("id_full_name", hostName)
                                    put("role_type", "Guardian")
                                    put("st_session_state", "P2P SYNC COMPLETED")
                                    put("is_admin", 1)
                                    put("is_friend", if(isFriend) 1 else 0)
                                    put("owner_name", hostName)
                                    put("qr_code_ref", qrCode)
                                }
                                com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).saveRosterNode(vRoster)
                                
                                val myDosMap = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).getLocalDossier("OWNER_001")
                                if (myDosMap != null) {
                                    com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).commitAnalyticalDossier(
                                        "OWNER_001", myDosMap, finalChildRole
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
