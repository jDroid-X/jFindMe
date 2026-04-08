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

/**
 * 📡 OPTION A — Firebase Pairing (Remote)
 * Strict Distributed Systems implementation bridging 'pairing_requests' -> 'groups' -> 'locations'
 * Integrates Mandatory Handshake Approval stopping rogue devices from secretly tracking groups.
 */
object FirebasePairingManager {
    private const val TAG = "FirebasePairingManager"

    private val db = FirebaseDatabase.getInstance().reference
    private val pairingRequestsEndpoint = db.child("pairing_requests")
    private val groupsEndpoint = db.child("groups")
    private val usersEndpoint = db.child("users")

    // The Active Host Listener Reference (Kept in memory for garbage collection during Activity pause)
    private var activeHostListener: ValueEventListener? = null
    var activePairingCode: String? = null

    /**
     * Parent Device Phase 1: Establish Secure Pending Request Node with Checkbox Logic Integration!
     */
    fun startHostPairingSession(context: android.content.Context, assignedRoles: String, onCodeGenerated: (Bitmap?, String) -> Unit, onChildRequestReceived: (String, String) -> Unit) {
        val hostUid = getDeviceId()
        val inviteCode = generateSecureToken()
        activePairingCode = inviteCode

        // 🚀 Forensic Metadata retrieval for Handshake Exchange
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val myDos = dbHelper.getLocalDossier("OWNER_001")
        val hostName = myDos?.get("full_name") ?: "Host Admin"

        val payload = mapOf(
            "parentDeviceId" to hostUid,
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

    /**
     * Child Device Phase 1: Injects UUID and Preliminary Role into the Host's waiting Node.
     */
    fun requestJoinGroup(context: android.content.Context, inviteCode: String, selectedRole: String, onStatusUpdate: (Boolean, String) -> Unit) {
        val childUid = getDeviceId()
        
        // 🚀 Forensic Metadata retrieval for Handshake Exchange
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val myDos = dbHelper.getLocalDossier("OWNER_001")
        val childName = myDos?.get("full_name") ?: "Guest"

        pairingRequestsEndpoint.child(inviteCode).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val status = snapshot.child("status").getValue(String::class.java)
                val assignedChild = snapshot.child("childDeviceId").getValue(String::class.java) ?: ""

                if (status == "waiting") {
                    pairingRequestsEndpoint.child(inviteCode).updateChildren(
                        mapOf(
                            "childDeviceId" to childUid,
                            "child_name" to childName,
                            "child_role_preliminary" to selectedRole,
                            "status" to "pending",
                            "qr_code" to inviteCode
                        )
                    )
                } else if (assignedChild != childUid && status != "waiting") {
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

    /**
     * Parent Device Phase 2: Host clicked 'Accept' physically.
     * Settles the grouping immediately per the new handshake protocol.
     */
    fun approveHandshake(context: android.content.Context, inviteCode: String, childId: String, hostRole: String, childRole: String, onComplete: (Boolean) -> Unit) {
        val hostId = getDeviceId()
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        
        // 🦋 Premium Cinematic Transition (masking forensic data swap)
        com.jdroidx.jfindme.utils.AngelLoader.show(context as android.app.Activity)
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(context, "Finalizing Forensic Handshake...")

        pairingRequestsEndpoint.child(inviteCode).get().addOnSuccessListener { handshakeSnap ->
            val childName = handshakeSnap.child("child_name").getValue(String::class.java) ?: "Target"
            val hostName = handshakeSnap.child("host_name").getValue(String::class.java) ?: "Admin"
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                usersEndpoint.child(hostId).get().addOnSuccessListener { hostSnap ->
                    val hostGroup = hostSnap.child("active_group").getValue(String::class.java) ?: "GRP_${System.currentTimeMillis()}"
                    val currentCsv = hostSnap.child("valid_linked_uids").getValue(String::class.java) ?: ""

                    // 1. Update Host's Group and Whitelist
                    if (hostSnap.child("active_group").value == null) usersEndpoint.child(hostId).child("active_group").setValue(hostGroup)
                    val approvedIds = currentCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                    if (!approvedIds.contains(childId)) approvedIds.add(childId)
                    usersEndpoint.child(hostId).child("valid_linked_uids").setValue(approvedIds.joinToString(","))
                    usersEndpoint.child(hostId).child("friend_matrix").child(childId).setValue(true)

                    // 2. Update Child's Roles and Group
                    val cRef = usersEndpoint.child(childId)
                    val isFriend = childRole.equals("Friends", ignoreCase = true)
                    cRef.child("role_family").setValue(if (isFriend) "None" else childRole)
                    cRef.child("role_friend").setValue(if (isFriend) "Friends" else "None")
                    cRef.child("active_group").setValue(hostGroup)
                    cRef.child("friend_matrix").child(hostId).setValue("HOST")

                    // 🚀 P2P FORRENSIC PERSISTENCE: Mirror to Local Roster on Host
                    // Using "device_profile_name" logic as requested for display name
                    dbHelper.saveRosterNode(childId, childName, if(isFriend) "Friends" else childRole, "Handshake Established", 
                        isAdmin = false, isFriend = isFriend, ownerName = hostName, ownerDeviceId = hostId, connectedDeviceId = childId, qrCode = inviteCode)
                    
                    // 🚀 AUDIT LEDGER: Log the Paring Relationship History
                    dbHelper.logDevicePairing(hostId, hostName, childId, childName, if(isFriend) "Friends" else childRole)

                    // 3. Update Global Group Node
                    groupsEndpoint.child(hostGroup).child("members").child(hostId).setValue(true)
                    groupsEndpoint.child(hostGroup).child("members").child(childId).setValue(true)
                    groupsEndpoint.child(hostGroup).child("citizen_dossiers").child(childId).updateChildren(
                        mapOf("role_family" to (if(isFriend) "None" else childRole), "role_friend" to (if(isFriend) "Friends" else "None"))
                    )

                    // 4. Release Handshake Lock
                    pairingRequestsEndpoint.child(inviteCode).updateChildren(mapOf(
                        "status" to "approved", "host_id" to hostId, "final_host_role" to hostRole, "final_child_role" to childRole
                    )).addOnSuccessListener {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(context)
                        onComplete(true)
                        clearSession(inviteCode)
                    }
                }.addOnFailureListener { com.jdroidx.jfindme.utils.AngelLoader.hide(context); onComplete(false) }
            }, 5000)
        }
    }

    // 🚀 ONE-TAP FINALIZATION: The confirmHandshake phase has been removed to streamline the P2P handshake.
    // The Host's approval now immediately settles the sync for both nodes.
    
    private fun forgeRemotePersistentGroup(hostUid: String, childId: String, inviteCode: String) {
        // Obsolete Engine: Kept purely for architectural backwards compatibility if called elsewhere.
        // P2P friend_matrix has legally overridden active_group topology for new pairs!
        SystemLogger.i(TAG, "Legacy forgeRemotePersistentGroup called but bypassed mapped to friend_matrix safely.")
    }

    /**
     * Parent Device: Rejects safely and wipes Node preventing loop exploits.
     */
    fun rejectHandshake(inviteCode: String) {
        pairingRequestsEndpoint.child(inviteCode).child("status").setValue("rejected")
        clearSession(inviteCode)
    }

    /**
     * Child Device Phase 2: Listener waiting on Parent's physical GUI click.
     * Continuously monitors the Handshake Node for role-proposals or rejection.
     */
    private fun listenForAcceptance(context: android.content.Context, inviteCode: String, onStatusUpdate: (Boolean, String) -> Unit) {
        val childId = getDeviceId()
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

                // Security: Ensure this listener only reacts to the child identity assigned to this node
                if (assignedChild.isNotEmpty() && assignedChild != childId) return 

                // 🚀 STATE HYSTERESIS: Prevent redundant UI triggers on B (Target)
                if (status == lastNotifiedStatus) return
                lastNotifiedStatus = status

                when (status) {
                    "pending" -> {
                        onStatusUpdate(true, "PENDING_HANDSHAKE") // Host hasn't clicked 'Approve' yet
                    }
                    "approved" -> {
                        // 🦋 Child-Side Cinematic Transition (masking data receipt)
                        com.jdroidx.jfindme.utils.AngelLoader.show(context as android.app.Activity)
                        com.jdroidx.jfindme.utils.AngelLoader.setStatus(context, "Receiving Forensic Metadata...")

                        val hostId = snapshot.child("host_id").getValue(String::class.java) ?: ""
                        val hostName = snapshot.child("host_name").getValue(String::class.java) ?: "Admin"
                        val qrCode = snapshot.child("qr_code").getValue(String::class.java) ?: inviteCode
                        val finalChildRole = snapshot.child("final_child_role").getValue(String::class.java) ?: "Member"
                        val isFriend = finalChildRole.equals("Friends", ignoreCase = true)

                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (hostId.isNotEmpty()) {
                                // Mirror Host node to Child's local Roster SSOT with full forensic metadata
                                com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).saveRosterNode(
                                    hostId, hostName, "Guardian", "P2P SYNC COMPLETED", isAdmin = true, isFriend = isFriend,
                                    ownerName = hostName, ownerDeviceId = hostId, connectedDeviceId = childId, qrCode = qrCode
                                )
                                
                                // 🚀 Update local dossier with roles
                                val myDosMap = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).getLocalDossier("OWNER_001")
                                if (myDosMap != null) {
                                    com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context).commitForensicDossier(
                                        "OWNER_001", myDosMap, finalChildRole, hostName, hostId, childId, qrCode
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
        // 🧠 QA Fix: A delay is strictly required here before wiping the node!
        // Otherwise, the Firebase cache deletes the Handshake Node before the target Child's ValueEventListener 
        // has enough time to read the 'status' = 'approved' feedback loop!
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            pairingRequestsEndpoint.child(code).removeValue()
        }, 3500)
        
        activePairingCode = null
    }

    // QA In-Memory Cache: Locks the randomly generated test ID for the duration of the App process!
    private var cachedMockUid: String? = null

    // QA Bug Fix: Persistent Unique Identifiers required to stop "U_MOCK" Collisions on unauthenticated target devices.
    fun getDeviceId(): String {
        val authUid = FirebaseAuth.getInstance().currentUser?.uid
        if (authUid != null) return authUid

        if (cachedMockUid == null) {
            cachedMockUid = "DEV_${java.util.UUID.randomUUID().toString().substring(0, 8)}"
        }
        return cachedMockUid!!
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

