package com.jdroidx.jfindme.data.repository

import android.os.Build
import com.google.firebase.database.*
import com.jdroidx.jfindme.data.model.DeviceRegistry
import com.jdroidx.jfindme.data.model.PairingSession
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger
import java.util.UUID

/**
 * PairingRepository: Handles device registry, pairing sessions, and multi-device sync.
 * Implements the Required Pairing Architecture (L4-L5).
 */
class PairingRepository {

    private val TAG = "PairingRepository"
    private val sessionsRef = FirebaseComm.database.getReference("pairing_sessions")
    private val devicesRef = FirebaseComm.database.getReference("devices")

    /**
     * Step 1: Register Device Identity
     */
    fun registerDevice(userId: String, isPrimary: Boolean, onComplete: (String) -> Unit) {
        val deviceId = UUID.randomUUID().toString().take(8)
        val device = DeviceRegistry(
            deviceId = deviceId,
            userId = userId,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            isPrimary = isPrimary,
            lastActive = System.currentTimeMillis()
        )
        devicesRef.child(userId).child(deviceId).setValue(device)
            .addOnSuccessListener {
                SystemLogger.i(TAG, "Device registered: $deviceId")
                onComplete(deviceId)
            }
    }

    /**
     * Step 2: Generate Pairing Code (Primary Device)
     */
    fun startPairingSession(userId: String, deviceId: String, onCodeReady: (String) -> Unit) {
        val pairCode = (100000..999999).random().toString()
        val sessionId = UUID.randomUUID().toString().take(6)
        val session = PairingSession(
            sessionId = sessionId,
            userId = userId,
            pairCode = pairCode,
            expiresAt = System.currentTimeMillis() + 300000, // 5 min
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

    fun linkDeviceToGroup(userId: String, groupId: String) {
        FirebaseComm.usersRef.child(userId).child("groupId").setValue(groupId)
            .addOnSuccessListener {
                SystemLogger.i(TAG, "Device linked to group: $groupId")
            }
    }
    
    // 🚀 NEW SECURE MVVM ARCHITECTURE - SCENARIO HANDSHAKES
    
    companion object {
        private val db = FirebaseDatabase.getInstance().reference

        @JvmStatic
        fun executeGenesisNode(uid: String, onSuccess: (String) -> Unit) {
            val newGroupId = db.child("groups").push().key ?: return
            
            val updates = hashMapOf<String, Any>(
                "users/$uid/active_group" to newGroupId,
                "groups/$newGroupId/members/$uid" to true
            )
            
            db.updateChildren(updates).addOnSuccessListener {
                SystemLogger.i("PairingRepo", "Genesis Node mathematically constructed: $newGroupId")
                onSuccess(newGroupId)
            }
        }

        @JvmStatic
        fun executeFamilyHandshake(hostUid: String, hostGroupId: String, targetUid: String, roleFam: String, onSuccess: () -> Unit) {
            // 🚀 QA Fix: Master "One Scan One Device" enforcement!
            // First, physically extract the Host's existing authorized hardware CSV map.
            db.child("users").child(hostUid).child("valid_linked_uids").get().addOnSuccessListener { snapshot ->
                val currentCsv = snapshot.getValue(String::class.java) ?: ""
                val approvedIds = currentCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                
                // Add the newly scanned hardware node explicitly to the Host's UI whitelist!
                if (!approvedIds.contains(targetUid)) {
                    approvedIds.add(targetUid)
                }
                val newSecureCsv = approvedIds.joinToString(",")

                val updates = hashMapOf<String, Any>(
                    "users/$targetUid/active_group" to hostGroupId,
                    "users/$hostUid/valid_linked_uids" to newSecureCsv, // 🔒 Injects scanner whitelist natively into the payload
                    "groups/$hostGroupId/members/$targetUid" to true,
                    "groups/$hostGroupId/citizen_dossiers/$targetUid/role_family" to roleFam,
                    "groups/$hostGroupId/citizen_dossiers/$targetUid/role_friend" to "None"
                )
                
                db.updateChildren(updates).addOnSuccessListener {
                    SystemLogger.w("PairingRepo", "Target Absorbed into Core Family Array. Scanner Whitelist Updated: $targetUid -> $roleFam")
                    onSuccess()
                }
            }
        }

        @JvmStatic
        fun executeFriendHandshake(hostUid: String, targetUid: String, onSuccess: () -> Unit) {
            // Peer-to-peer insulation. Friends do NOT share the explicit active_group.
            // Instead, they are pushed into a dedicated "friend_matrix" node.
            val updates = hashMapOf<String, Any>(
                "users/$hostUid/friend_matrix/$targetUid" to true,
                "users/$targetUid/friend_matrix/$hostUid" to true
            )
            
            db.updateChildren(updates).addOnSuccessListener {
                SystemLogger.w("PairingRepo", "Peer-to-Peer Insulated Link Completed.")
                onSuccess()
            }
        }
    }
}
