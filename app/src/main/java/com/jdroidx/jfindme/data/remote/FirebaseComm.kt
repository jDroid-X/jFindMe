/**
 * Auto-Login Architecture: Multi-Cloud Forensic Bridge
 * Waterfall Method: Secured Real-time Sync & Identity Parity
 */
package com.jdroidx.jfindme.data.remote

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.data.model.UserLocation

/**
 * Singleton to manage Firebase Database communication.
 * Standardizes access to tracking, user, and history nodes.
 */
object FirebaseComm {
    // --- L7 FORENSIC HUB NODES (CLEAN HIERARCHY V67.2) ---
    const val NODE_DEVICES = "devices"            // Master Agent Registry (Identity + profile + device)
    
    // REDUNDANT NODES (DECOMMISSIONED AS PER USER REQUEST)
    const val NODE_ANALYTICS_EVENTS = "events"
    const val NODE_LOCATIONS = "locations"
    const val NODE_GROUPS = "groups"
    const val NODE_RELATIONSHIPS = "relationships"
    const val NODE_PAIRING = "pairing_requests"
    const val NODE_DIAGNOSTICS = "diagnostics"

    val database: FirebaseDatabase by lazy {
        val db = FirebaseDatabase.getInstance()
        try { db.setPersistenceEnabled(true) } catch (e: Exception) {}
        db
    }

    // 🧬 V67.2 CLEAN HUB ENDPOINTS
    @JvmStatic val devicesRef: DatabaseReference get() = database.reference.child(NODE_DEVICES)
    
    // 🛡️ V67.2 SAFETY REDIRECTS: Prevents build failures while blocking writes to restricted nodes
    @JvmStatic val groupsRef: DatabaseReference get() = database.reference.child("decommissioned").child("groups")
    @JvmStatic val eventsRef: DatabaseReference get() = database.reference.child("decommissioned").child("events")
    @JvmStatic val locationsRef: DatabaseReference get() = database.reference.child("decommissioned").child("locations")
    @JvmStatic val relationshipsRef: DatabaseReference get() = database.reference.child("decommissioned").child("relationships")
    @JvmStatic val pairingRef: DatabaseReference get() = database.reference.child("decommissioned").child("pairing")
    @JvmStatic val diagRef: DatabaseReference get() = database.reference.child("decommissioned").child("diagnostics")

    // 🧬 V67.2: Group logic decommissioned. Redirecting to safe stub for backwards compatibility.
    @JvmStatic fun getGroupRef(agentId: String, groupType: String): DatabaseReference = groupsRef.child(agentId).child(groupType)

    // 🧬 L7 HUB REDIRECTORS (Bridges legacy Java code to the new hierarchy)
    @Deprecated("Use getProfileRef(agentId)")
    @JvmStatic fun getProfilesRef(): DatabaseReference = devicesRef

    @Deprecated("Use getIdentityRef(agentId)")
    @JvmStatic fun getIdentitiesRef(): DatabaseReference = devicesRef

    @JvmStatic fun getProfileRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child("profile")
    @JvmStatic fun getIdentityRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child("Identity")

    /**
     * TACTICAL SPEED AUDIT: Simulates the diag.py handshake to measure cloud latency.
     */
    fun performLatencyAudit(callback: (latencyMs: Long) -> Unit) {
        val startTime = System.currentTimeMillis()
        val diagRef = devicesRef.child("system_audit").child("latency")
        
        val pingData = mapOf(
            "timestamp" to startTime,
            "deviceId" to "DIAG_NODE"
        )

        diagRef.setValue(pingData).addOnCompleteListener {
            val latency = System.currentTimeMillis() - startTime
            com.jdroidx.jfindme.utils.SystemLogger.i("FirebaseAudit", "📡 Tactical Cloud Handshake: ${latency}ms")
            callback(latency)
        }
    }

    /**
     * 🛡️ IDENTITY GUARDIAN: Strictly validates Agent IDs to prevent Auth UID leakage.
     * All forensic Agent IDs must be 8-character alphanumeric signatures.
     */
    @JvmStatic
    fun isAgentIdValid(id: String): Boolean {
        if (id.isEmpty()) return false
        // Firebase UIDs are typically 28 characters. Agent IDs are 8.
        return id.length == 8 && id.matches(Regex("^[A-Z0-9]+$"))
    }

    @JvmStatic
    fun resolveHardwareIdentity(authUid: String, callback: (String?) -> Unit) {
        // 🧬 V69.8: Universal Local-First Cache (Identity Guardian)
        // Check local SQLite ledger before triggering expensive/un-indexed cloud queries.
        val context = com.jdroidx.jfindme.JFindMeApplication.getContext()
        if (context != null) {
            val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val localId = db.resolveAgentIdLocally(authUid)
            if (!localId.isNullOrEmpty()) {
                // 🧬 V69.8: Local Cache is King. Even if it's a transient Auth UID, 
                // return it to stop un-indexed cloud queries.
                callback(localId)
                return
            }
        }

        // 🔍 CLOUD FALLBACK: Query-based lookup (Requires Index: .indexOn: "Identity/auth_uid")
        devicesRef.orderByChild("Identity/auth_uid").equalTo(authUid).limitToFirst(1)
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val key = snapshot.children.firstOrNull()?.key
                    if (key != null && isAgentIdValid(key)) {
                        callback(key)
                    } else {
                        // 🧬 Forensic Recovery: If the key is invalid (e.g. Auth UID leak), search for a valid child
                        var foundValid = false
                        for (child in snapshot.children) {
                            val childKey = child.key
                            if (childKey != null && isAgentIdValid(childKey)) {
                                callback(childKey)
                                foundValid = true
                                break
                            }
                        }
                        if (!foundValid) callback(null)
                    }
                } else {
                    callback(null)
                }
            }.addOnFailureListener {
                com.jdroidx.jfindme.utils.SystemLogger.e("FirebaseAudit", "Identity Resolution Failed: ${it.message}")
                callback(null)
            }
    }

    @JvmStatic
    fun resolveHardwareIdentityByHardwareId(hwId: String, callback: (String?) -> Unit) {
        database.reference.child("hw_mapping").child(hwId).get().addOnSuccessListener { snapshot ->
            val agentId = snapshot.getValue(String::class.java)
            if (agentId != null && isAgentIdValid(agentId)) {
                callback(agentId)
            } else {
                callback(null)
            }
        }.addOnFailureListener {
            callback(null)
        }
    }

    /**
     * 🧬 V67.0 UNIFIED FORENSIC HEARTBEAT (The Master Program)
     * Consolidates Identity, Profile, Hardware, and Location into ONE atomic sync.
     * Follows strict 1:1 field mapping as per Forensic Identity Matrix.
     */
    @JvmStatic
    fun syncForensicHeartbeat(
        authUid: String,
        agentId: String,
        hardwareId: String,
        payload: Map<String, Any>,
        activeGroupId: String? = null
    ) {
        if (!isAgentIdValid(agentId)) {
            com.jdroidx.jfindme.utils.SystemLogger.e("FirebaseComm", "BLOCKED: Invalid Agent ID $agentId")
            return
        }

        val now = getForensicTimestamp()
        val atomicPayload = mutableMapOf<String, Any>()

        // 🧬 CLEAN HIERARCHY MIRROR (V67.8): All table data even blank
        
        // 1. IDENTITY NODE (Tier 1 Mirror)
        val idPath = "$NODE_DEVICES/$agentId/Identity"
        atomicPayload["$idPath/auth_uid"] = authUid
        atomicPayload["$idPath/uid_1"] = agentId
        atomicPayload["$idPath/uid_2"] = hardwareId
        val fullName = payload["name"] ?: payload["userName"] ?: payload["id_full_name"] ?: ""
        atomicPayload["$idPath/id_full_name"] = fullName
        atomicPayload["$idPath/node_index"] = 1
        atomicPayload["$idPath/is_admin"] = "1"
        atomicPayload["$idPath/owner_name"] = payload["owner_name"] ?: fullName
        atomicPayload["$idPath/connected_id"] = payload["connected_id"] ?: hardwareId
        atomicPayload["$idPath/qr_code_ref"] = payload["qr_code_ref"] ?: ""
        atomicPayload["$idPath/status"] = "Active"
        atomicPayload["$idPath/updated_at"] = now

        // 2. PROFILE NODE (Tier 1 Metadata)
        val profPath = "$NODE_DEVICES/$agentId/profile"
        atomicPayload["$profPath/name"] = payload["name"] ?: payload["userName"] ?: ""
        atomicPayload["$profPath/email"] = payload["email"] ?: payload["userEmail"] ?: ""
        atomicPayload["$profPath/phone"] = payload["phone"] ?: ""
        atomicPayload["$profPath/dob"] = payload["dob"] ?: ""
        atomicPayload["$profPath/gender"] = payload["gender"] ?: ""
        atomicPayload["$profPath/country"] = payload["country"] ?: ""
        atomicPayload["$profPath/state"] = payload["state"] ?: ""
        atomicPayload["$profPath/city"] = payload["city"] ?: ""
        atomicPayload["$profPath/pincode"] = payload["pincode"] ?: ""
        atomicPayload["$profPath/address"] = payload["address"] ?: ""
        atomicPayload["$profPath/org_name"] = payload["org_name"] ?: ""
        atomicPayload["$profPath/dept_name"] = payload["dept_name"] ?: ""
        atomicPayload["$profPath/avatar_id"] = payload["avatar_id"] ?: "1"
        atomicPayload["$profPath/note"] = payload["emergency_note"] ?: payload["note"] ?: ""
        atomicPayload["$profPath/updated_at"] = now

        // 3. DEVICE NODE (Tier 2 Hardware Hub)
        val devPath = "$NODE_DEVICES/$agentId/device"
        atomicPayload["$devPath/device_id"] = hardwareId
        atomicPayload["$devPath/model"] = payload["model"] ?: android.os.Build.MODEL
        atomicPayload["$devPath/imei"] = payload["imei"] ?: ""
        atomicPayload["$devPath/is_rooted"] = if (payload["is_rooted"] == true) "1" else "0"
        atomicPayload["$devPath/fingerprint"] = payload["fingerprint"] ?: ""
        atomicPayload["$devPath/latitude"] = payload["latitude"] ?: 0.0
        atomicPayload["$devPath/longitude"] = payload["longitude"] ?: 0.0
        atomicPayload["$devPath/Address"] = payload["Address"] ?: payload["address"] ?: ""
        atomicPayload["$devPath/updated_at"] = now

        // 🛡️ V67.8: ALL OTHER NODES DECOMMISSIONED
        
        database.reference.updateChildren(atomicPayload).addOnSuccessListener {
            com.jdroidx.jfindme.utils.SystemLogger.i("FirebaseComm", "CLEAN HEARTBEAT SUCCESS: $agentId")
        }.addOnFailureListener {
            com.jdroidx.jfindme.utils.SystemLogger.e("FirebaseComm", "HEARTBEAT FAILURE: ${it.message}")
        }
    }

    /**
     * 🧬 V67.2: DECOMMISSIONED - All legacy registration and safezone logic suspended.
     */
    @JvmStatic fun syncRegistrationBaseline(uid1: String, baseline: Map<String, Any>) {}
    @JvmStatic fun syncSafeZone(uid1: String, zoneName: String, zoneId: String, zoneData: Map<String, Any>) {}

    @JvmStatic
    fun getInvertedForensicKey(input: Any? = System.currentTimeMillis()): String {
        val baseline = 9999123123595999L
        val current = when (input) {
            is Long -> getForensicTimestamp(input).toLongOrNull() ?: input
            is String -> input.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
        return (baseline - current).toString()
    }

    /**
     * 🧬 V60 High-Precision Forensic Pulse
     * Returns: YYYYMMDDHHMMSSSS (16-character precise timeline)
     */
    @JvmStatic
    fun getForensicTimestamp(input: Any? = System.currentTimeMillis()): String {
        if (input is String && input.length == 16) return input
        
        val timeMs = when (input) {
            is Long -> input
            is String -> input.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
        
        val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmssSS", java.util.Locale.US)
        val formatted = sdf.format(java.util.Date(timeMs))
        return if (formatted.length > 16) formatted.take(16) else formatted.padEnd(16, '0')
    }

    @JvmStatic
    fun syncAnalyticalEvent(uid1: String, eventData: UserLocation) {
        // 🛡️ V67.2: Event tracking decommissioned as per clean hierarchy mandate.
    }

    @Deprecated("Use syncAnalyticalEvent for high-fidelity tiered tracking")
    fun updateMyLocation(userLocation: UserLocation) {
        val rId = userLocation.getResolvedUserId()
        if (rId.isNotEmpty()) {
            syncAnalyticalEvent(rId, userLocation)
        }
    }
}
