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
    fun startHostPairingSession(assignedRoles: String, onCodeGenerated: (Bitmap?, String) -> Unit, onChildRequestReceived: (String) -> Unit) {
        val hostUid = getDeviceId()
        val inviteCode = generateSecureToken()
        activePairingCode = inviteCode

        val payload = mapOf(
            "parentDeviceId" to hostUid,
            "childDeviceId" to "",
            "status" to "waiting",
            "assignedRoles" to assignedRoles // 🚀 Burns the Checkboxes directly into the Math Payload!
        )

        pairingRequestsEndpoint.child(inviteCode).setValue(payload).addOnSuccessListener {
            val qr = constructQrGraphic(inviteCode)
            onCodeGenerated(qr, inviteCode)

            // Step 2: Parent natively listens for a child attempting handshake
            activeHostListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    val childId = snapshot.child("childDeviceId").getValue(String::class.java)

                    if (status == "pending" && !childId.isNullOrEmpty()) {
                        SystemLogger.i(TAG, "Child Handshake Intercepted: $childId")
                        onChildRequestReceived(childId)
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
     * Child Device Phase 1: Injects UUID into the Host's waiting Node.
     */
    fun requestJoinGroup(inviteCode: String, onStatusUpdate: (Boolean, String) -> Unit) {
        val childUid = getDeviceId()
        
        pairingRequestsEndpoint.child(inviteCode).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val status = snapshot.child("status").getValue(String::class.java)
                if (status == "waiting") {
                    // Inject Identity Handshake!
                    pairingRequestsEndpoint.child(inviteCode).updateChildren(
                        mapOf(
                            "childDeviceId" to childUid,
                            "status" to "pending"
                        )
                    )
                    
                    // Wait for Parent to physically hit 'Accept'
                    listenForAcceptance(inviteCode, onStatusUpdate)
                } else {
                    onStatusUpdate(false, "Code is already actively pairing or full.")
                }
            } else {
                onStatusUpdate(false, "Invalid Code.")
            }
        }.addOnFailureListener {
            onStatusUpdate(false, "Network Frame Error.")
        }
    }

    /**
     * Parent Device Phase 2: Host clicked 'Accept' physically.
     * Triggers the final DB construction locking both devices into the shared Matrix.
     */
    fun approveHandshake(inviteCode: String, childId: String) {
        val hostUid = getDeviceId()
        
        // 🚀 QA ARCHITECTURE FIX: "Remote Multi-User Disconnect Flaw"
        // We MUST verify if the parent already owns an established group before generating a new one!
        // Approving a remote child blindly abandoned all previously paired children.
        usersEndpoint.child(hostUid).child("active_group").get().addOnSuccessListener { snapshot ->
            val existingGroupId = snapshot.getValue(String::class.java)
            if (!existingGroupId.isNullOrEmpty()) {
                // Parent already has a physical group! Simply inject the new child into it.
                groupsEndpoint.child(existingGroupId).child("members").child(childId).setValue(true).addOnSuccessListener {
                    usersEndpoint.child(childId).child("active_group").setValue(existingGroupId)
                    
                    // 🚀 Phase 3 Handshake: Extract Checkbox Array and force it onto the target device securely!
                    pairingRequestsEndpoint.child(inviteCode).child("assignedRoles").get().addOnSuccessListener { roleSnap ->
                        val roleStr = roleSnap.getValue(String::class.java) ?: ""
                        val cRef = usersEndpoint.child(childId)
                        cRef.child("role_mother").setValue(roleStr.contains("MOTHER", true))
                        cRef.child("role_father").setValue(roleStr.contains("FATHER", true))
                        cRef.child("role_spouse").setValue(roleStr.contains("SPOUSE", true))
                        cRef.child("role_friends").setValue(roleStr.contains("FRIENDS", true))
                    }
                    
                    // Release the Child from 'waiting' state!
                    pairingRequestsEndpoint.child(inviteCode).child("status").setValue("approved")
                    SystemLogger.i(TAG, "Remote Handshake Approved: Child appended to existing Master Group ($existingGroupId).")
                    clearSession(inviteCode)
                }
            } else {
                // User has absolutely no group. Build a fresh one securely.
                forgeRemotePersistentGroup(hostUid, childId, inviteCode)
            }
        }.addOnFailureListener {
            SystemLogger.e(TAG, "Failed resolving existing group legacy.", it)
            forgeRemotePersistentGroup(hostUid, childId, inviteCode) // Fallback constraint
        }
    }
    
    private fun forgeRemotePersistentGroup(hostUid: String, childId: String, inviteCode: String) {
        pairingRequestsEndpoint.child(inviteCode).child("assignedRoles").get().addOnSuccessListener { roleSnap ->
            val roleStr = roleSnap.getValue(String::class.java) ?: ""
            val cRef = usersEndpoint.child(childId)
            cRef.child("role_mother").setValue(roleStr.contains("MOTHER", true))
            cRef.child("role_father").setValue(roleStr.contains("FATHER", true))
            cRef.child("role_spouse").setValue(roleStr.contains("SPOUSE", true))
            cRef.child("role_friends").setValue(roleStr.contains("FRIENDS", true))
        }
        val groupId = "GRP_${System.currentTimeMillis()}"

        // Build the Final Unified Architecture schema block
        val groupPayload = mapOf(
            "host" to hostUid,
            "members/$hostUid" to true,
            "members/$childId" to true,
            "invite_code" to "REMOTE_LINK" // Safe fallback identifier
        )

        groupsEndpoint.child(groupId).updateChildren(groupPayload).addOnSuccessListener {
            // Bind routing directly to Profile references
            usersEndpoint.child(hostUid).child("active_group").setValue(groupId)
            usersEndpoint.child(childId).child("active_group").setValue(groupId)

            // Release the Child from 'waiting' state!
            pairingRequestsEndpoint.child(inviteCode).child("status").setValue("approved")
            SystemLogger.i(TAG, "Remote Handshake Approved: Fresh Master Group Built ($groupId).")
            
            // Clean up memory
            clearSession(inviteCode)
        }
    }

    /**
     * Parent Device: Rejects safely and wipes Node preventing loop exploits.
     */
    fun rejectHandshake(inviteCode: String) {
        pairingRequestsEndpoint.child(inviteCode).child("status").setValue("rejected")
        clearSession(inviteCode)
    }

    /**
     * Child Device Phase 2: Listener waiting on Parent's physical GUI click
     */
    private fun listenForAcceptance(inviteCode: String, onStatusUpdate: (Boolean, String) -> Unit) {
        pairingRequestsEndpoint.child(inviteCode).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java)
                if (status == "approved") {
                    onStatusUpdate(true, "Handshake Approved! Syncing Pipeline...")
                    pairingRequestsEndpoint.child(inviteCode).removeEventListener(this) // Close listener loop securely
                } else if (status == "rejected") {
                    onStatusUpdate(false, "Parent Rejected Link.")
                    pairingRequestsEndpoint.child(inviteCode).removeEventListener(this)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
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
    private fun getDeviceId(): String {
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
