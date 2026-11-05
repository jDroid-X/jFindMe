package com.jdroidx.jfindme.engine.sync

import android.graphics.Bitmap
import android.graphics.Color
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

    /**
     * Initializes a brand-new master group tracking instance and generates a QR Bitmap.
     * The Parent creates the group, the Children scan the QR Code to join.
     */
    fun bootstrapNewGroup(groupName: String, onQrGenerated: (Bitmap?, String) -> Unit) {
        val finalHostId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(com.jdroidx.jfindme.JFindMeApplication.getContext())
        com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(finalHostId).child("active_group").get().addOnSuccessListener { snapshot ->
            val existingGroupId = snapshot.getValue(String::class.java)
            if (!existingGroupId.isNullOrEmpty()) {
                // Parent already has a group! Fetch its persistent invite code.
                groupsEndpoint.child(existingGroupId).child("invite_code").get().addOnSuccessListener { codeSnap ->
                    val recycledCode = codeSnap.getValue(String::class.java)
                    if (!recycledCode.isNullOrEmpty()) {
                        SystemLogger.i(TAG, "Reactivating existing Master Group ($existingGroupId). Code: $recycledCode")
                        val qrBitmap = constructQrGraphic(recycledCode)
                        onQrGenerated(qrBitmap, recycledCode)
                    } else {
                        // The group exists but is corrupted (missing invite code). Overwrite it.
                        forgeNewPersistentGroup(finalHostId, groupName, onQrGenerated)
                    }
                }
            } else {
                // User has absolutely no group. Build a fresh one securely.
                forgeNewPersistentGroup(finalHostId, groupName, onQrGenerated)
            }
        }.addOnFailureListener {
            SystemLogger.e(TAG, "Failed to verify host group legacy status.", it)
            forgeNewPersistentGroup(finalHostId, groupName, onQrGenerated) // Failsafe fallback
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
                com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(hostUid).child("active_group").setValue(groupId)
                com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(hostUid).child("active_group").setValue(groupId)
                
                // 🚀 Cross-Screen Roster Bridge: Register Host in relationships roster
                val hostRosterMap = mapOf(
                    "role" to "GUARDIAN",
                    "role_family" to "GUARDIAN",
                    "role_friend" to "None",
                    "joined_at" to System.currentTimeMillis()
                )
                com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef.child(groupId).child("roster").child(hostUid).updateChildren(hostRosterMap)
                com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef.child(groupId).child("signals").child("refresh_roster").setValue(System.currentTimeMillis())

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
        val finalChildId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(com.jdroidx.jfindme.JFindMeApplication.getContext())

        // Search the groups endpoint for this exact token
        groupsEndpoint.orderByChild("invite_code").equalTo(inviteToken).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists() && snapshot.children.iterator().hasNext()) {
                    val targetGroup = snapshot.children.iterator().next()
                    val groupId = targetGroup.key ?: return@addOnSuccessListener
                    
                    // Bind User to Group
                    groupsEndpoint.child(groupId).child("members").child(finalChildId).setValue(true)
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(finalChildId).child("active_group").setValue(groupId)
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(finalChildId).child("active_group").setValue(groupId)

                    val joinerRosterMap = mapOf(
                        "role" to "CHILD",
                        "role_family" to "DEPENDENT_CHILD",
                        "role_friend" to "None",
                        "joined_at" to System.currentTimeMillis()
                    )
                    com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef.child(groupId).child("roster").child(finalChildId).updateChildren(joinerRosterMap)
                    com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef.child(groupId).child("signals").child("refresh_roster").setValue(System.currentTimeMillis())

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
