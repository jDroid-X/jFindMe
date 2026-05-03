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
    private val hubRef = FirebaseComm.devicesRef

    /**
     * Step 1: Register Device Identity (V59.5 Secured)
     * Refactored: Uses Agent ID (uid1) as Root Node and Forensic Timestamp.
     */
    fun registerDevice(agentId: String, isPrimary: Boolean, onComplete: (String) -> Unit) {
        val hardwareId = "HW_" + UUID.randomUUID().toString().take(8).uppercase()
        val device = mapOf(
            "deviceId" to hardwareId,
            "agentId" to agentId,
            "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "isPrimary" to isPrimary,
            "lastActive" to FirebaseComm.getForensicTimestamp(),
            "os_version" to Build.VERSION.RELEASE
        )
        
        // 🧬 V59.5: Anchor to Agent Hub, NOT Auth UID
        hubRef.child(agentId).child(hardwareId).setValue(device)
            .addOnSuccessListener {
                SystemLogger.i(TAG, "Forensic Device Registered under Agent Hub: $agentId")
                onComplete(hardwareId)
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
                    "${FirebaseComm.NODE_DEVICES}/$targetAgentId/identity/active_group" to hostGroupId,
                    "${FirebaseComm.NODE_DEVICES}/$hostAgentId/identity/valid_linked_agents" to newSecureCsv,
                    
                    // V61.0: Canonical Relational Bridge (Legacy Peer-to-Peer)
                    "${FirebaseComm.NODE_RELATIONSHIPS}/$hostGroupId/members/$targetAgentId" to mapOf(
                        "role" to roleFam,
                        "ts_linked" to FirebaseComm.getForensicTimestamp()
                    ),
                    
                    // 🚀 V61.0: MASTER GROUPS NODE (Hierarchical Hub)
                    "${FirebaseComm.NODE_GROUPS}/$hostAgentId/Family/$targetAgentId/identity" to mapOf(
                        "name" to "Target Agent ($targetAgentId)", // Placeholder, will be overriden by Target's sync
                        "ts_authorized" to FirebaseComm.getForensicTimestamp()
                    ),
                    "${FirebaseComm.NODE_GROUPS}/$hostAgentId/Family/$targetAgentId/pairing" to mapOf(
                        "relationship_type" to roleFam,
                        "group_id" to hostGroupId
                    )
                )
                
                db.updateChildren(updates).addOnSuccessListener {
                    SystemLogger.w("PairingRepo", "V61.0 Handshake: Target Agent Matrix Constructed under $hostAgentId")
                    onSuccess()
                }
            }
        }
    }
}
