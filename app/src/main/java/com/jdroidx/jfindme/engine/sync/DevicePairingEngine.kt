package com.jdroidx.jfindme.engine.sync

import android.graphics.Bitmap
import android.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 🔗 PILLAR 2: PAIRING/GROUP SYSTEM
 * Generates and validates explicit QR Code matrices and 6-Digit Alphanumeric tokens.
 * Binds discrete mobile hardware UUIDs directly into a shared 'Family/Group' security node.
 */
object DevicePairingEngine {
    private const val TAG = "DevicePairingEngine"
    
    // Abstract Firebase Nodes explicitly mapping hardware
    private val groupsEndpoint = FirebaseDatabase.getInstance().reference.child("groups")
    private val usersEndpoint = FirebaseDatabase.getInstance().reference.child("users")

    /**
     * Initializes a brand-new master group tracking instance and generates a QR Bitmap.
     * The Parent creates the group, the Children scan the QR Code to join.
     */
    fun bootstrapNewGroup(groupName: String, onQrGenerated: (Bitmap?, String) -> Unit) {
        val hostUid = FirebaseAuth.getInstance().currentUser?.uid ?: "U123"
        
        // 🚀 QA ARCHITECTURE FIX: "Multi-User Disconnect Flaw"
        // We MUST verify if the parent already owns an established group before generating a new one!
        // Generating a new group blindly abandons all previously paired children.
        usersEndpoint.child(hostUid).child("active_group").get().addOnSuccessListener { snapshot ->
            val existingGroupId = snapshot.getValue(String::class.java)
            if (!existingGroupId.isNullOrEmpty()) {
                // Parent already has a group! Fetch its persistent invite code.
                groupsEndpoint.child(existingGroupId).child("invite_code").get().addOnSuccessListener { codeSnap ->
                    val recycledCode = codeSnap.getValue(String::class.java)
                    if (!recycledCode.isNullOrEmpty()) {
                        SystemLogger.i(TAG, "Reactivating existing Master Group ($existingGroupId). Code: $recycledCode")
                        val qrBitmap = constructQrGraphic(recycledCode)
                        onQrGenerated(qrBitmap, recycledCode)
                        return@addOnSuccessListener
                    } else {
                        // The group exists but is corrupted (missing invite code). Overwrite it.
                        forgeNewPersistentGroup(hostUid, groupName, onQrGenerated)
                    }
                }
            } else {
                // User has absolutely no group. Build a fresh one securely.
                forgeNewPersistentGroup(hostUid, groupName, onQrGenerated)
            }
        }.addOnFailureListener {
            SystemLogger.e(TAG, "Failed to verify host group legacy status.", it)
            forgeNewPersistentGroup(hostUid, groupName, onQrGenerated) // Failsafe fallback
        }
    }

    private fun forgeNewPersistentGroup(hostUid: String, groupName: String, onQrGenerated: (Bitmap?, String) -> Unit) {
        val inviteCode = generateSecureToken()
        
        val payload = mutableMapOf<String, Any>()
        payload["group_name"] = groupName
        payload["host"] = hostUid
        payload["invite_code"] = inviteCode
        payload["members/$hostUid"] = true

        val groupId = "GRP_${System.currentTimeMillis()}"

        groupsEndpoint.child(groupId).updateChildren(payload)
            .addOnSuccessListener {
                usersEndpoint.child(hostUid).child("active_group").setValue(groupId)
                val qrBitmap = constructQrGraphic(inviteCode)
                onQrGenerated(qrBitmap, inviteCode)
                SystemLogger.i(TAG, "Fresh Master Group Forged! Invite Code: $inviteCode")
            }
            .addOnFailureListener {
                SystemLogger.e(TAG, "Failed forging master group logic", it)
                onQrGenerated(null, "")
            }
    }

    /**
     * The Child Device scans the QR Code or types the 6-Digit Text.
     * Authenticates the token against Firebase and officially physical binds the devices together.
     */
    fun joinGroupViaToken(inviteToken: String, onJoinStatus: (Boolean) -> Unit) {
        val joinerUid = FirebaseAuth.getInstance().currentUser?.uid ?: "U456"

        // Search the groups endpoint for this exact token
        groupsEndpoint.orderByChild("invite_code").equalTo(inviteToken).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists() && snapshot.children.iterator().hasNext()) {
                    val targetGroup = snapshot.children.iterator().next()
                    val groupId = targetGroup.key ?: return@addOnSuccessListener
                    
                    // Bind User to Group
                    groupsEndpoint.child(groupId).child("members").child(joinerUid).setValue(true)
                    usersEndpoint.child(joinerUid).child("active_group").setValue(groupId)
                    
                    SystemLogger.d(TAG, "Identity bound natively into Group: $groupId")
                    onJoinStatus(true)
                } else {
                    onJoinStatus(false)
                }
            }
            .addOnFailureListener { onJoinStatus(false) }
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
        } catch (e: Exception) {
            SystemLogger.e(TAG, "QR Generation crashed locally.", e)
            null
        }
    }
}
