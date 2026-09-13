package com.jdroidx.jbutterfly.data.remote

import com.google.firebase.database.*
import com.jdroidx.jbutterfly.data.model.LiveDeviceModel
import com.jdroidx.jbutterfly.data.model.UserLocation
import com.jdroidx.jbutterfly.utils.SystemLogger

object FirebaseComm {

    private const val TAG = "FirebaseComm"
    private val dbRef: DatabaseReference by lazy {
        try {
            FirebaseDatabase.getInstance().reference
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error obtaining FirebaseDatabase instance", e)
            FirebaseDatabase.getInstance("https://com-jdroidx-jbutterfly-35190.firebaseio.com").reference
        }
    }

    // Java-Friendly Callback Interfaces
    fun interface LocationCallback {
        fun onLocation(location: UserLocation)
    }

    fun interface BuzzCallback {
        fun onBuzz(senderId: String)
    }

    fun interface ClaimCallback {
        fun onClaimed(hostAgentId: String, hostHardwareId: String, category: String)
    }

    fun interface ErrorCallback {
        fun onError(error: String)
    }

    // ─── LOCATION PUBLISHING ──────────────────────────────────────────

    @JvmStatic
    fun publishLocation(location: UserLocation) {
        if (location.agentId.isBlank()) return
        val map = mapOf(
            "agentId" to location.agentId,
            "hardwareId" to location.hardwareId,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "speed" to location.speed,
            "heading" to location.heading,
            "batteryPct" to location.batteryPct,
            "timestamp" to location.timestamp
        )
        dbRef.child("locations").child(location.agentId).updateChildren(map)
            .addOnFailureListener { e -> SystemLogger.e(TAG, "Failed to publish location", e) }
    }

    // ─── PROFILE SYNCHRONIZATION ──────────────────────────────────────

    @JvmStatic
    fun publishProfile(device: LiveDeviceModel) {
        if (device.agentId.isBlank()) return
        val map = mapOf(
            "agentId" to device.agentId,
            "hardwareId" to device.hardwareId,
            "deviceName" to device.deviceName,
            "role" to device.role,
            "category" to device.category,
            "avatarName" to device.avatarName,
            "lastSeenTs" to System.currentTimeMillis()
        )
        dbRef.child("devices").child(device.agentId).child("profile").updateChildren(map)
            .addOnFailureListener { e -> SystemLogger.e(TAG, "Failed to publish profile", e) }
    }

    // ─── LIVE LOCATION LISTENER ───────────────────────────────────────

    @JvmStatic
    fun listenToDeviceLocation(agentId: String, callback: LocationCallback): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                try {
                    val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lng = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val speed = (snapshot.child("speed").getValue(Double::class.java) ?: 0.0).toFloat()
                    val heading = (snapshot.child("heading").getValue(Double::class.java) ?: 0.0).toFloat()
                    val accuracy = (snapshot.child("accuracy").getValue(Double::class.java) ?: 0.0).toFloat()
                    val battery = snapshot.child("batteryPct").getValue(Int::class.java) ?: 100
                    val ts = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                    val hw = snapshot.child("hardwareId").getValue(String::class.java) ?: ""

                    callback.onLocation(
                        UserLocation(
                            agentId = agentId,
                            hardwareId = hw,
                            latitude = lat,
                            longitude = lng,
                            accuracy = accuracy,
                            speed = speed,
                            heading = heading,
                            batteryPct = battery,
                            timestamp = ts
                        )
                    )
                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Error parsing remote location for $agentId", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                SystemLogger.w(TAG, "Location listener cancelled: ${error.message}")
            }
        }
        dbRef.child("locations").child(agentId).addValueEventListener(listener)
        return listener
    }

    @JvmStatic
    fun removeLocationListener(agentId: String, listener: ValueEventListener) {
        dbRef.child("locations").child(agentId).removeEventListener(listener)
    }

    // ─── REMOTE BUZZ / SIREN COMMAND ──────────────────────────────────

    @JvmStatic
    fun sendBuzzCommand(targetAgentId: String, senderAgentId: String, onSuccess: Runnable?, onError: ErrorCallback?) {
        val buzzPayload = mapOf(
            "senderAgentId" to senderAgentId,
            "command" to "BUZZ_SIREN",
            "timestamp" to System.currentTimeMillis()
        )
        dbRef.child("commands").child(targetAgentId).child("buzz").setValue(buzzPayload)
            .addOnSuccessListener { onSuccess?.run() }
            .addOnFailureListener { e -> onError?.onError(e.message ?: "Buzz failed") }
    }

    @JvmStatic
    fun listenForBuzzCommands(myAgentId: String, onBuzz: BuzzCallback): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val cmd = snapshot.child("command").getValue(String::class.java)
                    val sender = snapshot.child("senderAgentId").getValue(String::class.java) ?: "GUARDIAN"
                    val ts = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    if (cmd == "BUZZ_SIREN" && System.currentTimeMillis() - ts < 60000) {
                        onBuzz.onBuzz(sender)
                        snapshot.ref.removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.child("commands").child(myAgentId).child("buzz").addValueEventListener(listener)
        return listener
    }

    // ─── PAIRING PROTOCOL ─────────────────────────────────────────────

    @JvmStatic
    fun createPairingSession(
        pairingCode: String,
        hostAgentId: String,
        hostHardwareId: String,
        deviceName: String,
        category: String,
        onSuccess: Runnable?,
        onError: ErrorCallback?
    ) {
        val sessionData = mapOf(
            "pairingCode" to pairingCode,
            "hostAgentId" to hostAgentId,
            "hostHardwareId" to hostHardwareId,
            "deviceName" to deviceName,
            "category" to category,
            "status" to "PENDING",
            "timestamp" to System.currentTimeMillis()
        )
        dbRef.child("pairings").child(pairingCode).setValue(sessionData)
            .addOnSuccessListener { onSuccess?.run() }
            .addOnFailureListener { e -> onError?.onError(e.message ?: "Failed to create pairing") }
    }

    fun interface ConnectionCallback {
        fun onConnection(device: LiveDeviceModel)
    }

    @JvmStatic
    fun syncPairingConnection(
        myAgentId: String,
        myHardwareId: String,
        myNickname: String,
        myAvatar: String,
        targetAgentId: String,
        targetDeviceName: String,
        category: String,
        role: String,
        onSuccess: Runnable?,
        onError: ErrorCallback?
    ) {
        val targetRole = if (role.equals("GUARDIAN", ignoreCase = true)) "CHILD" 
                         else if (role.equals("CHILD", ignoreCase = true)) "GUARDIAN" 
                         else "FRIEND"

        val targetData = mapOf(
            "agentId" to myAgentId,
            "hardwareId" to myHardwareId,
            "deviceName" to myNickname,
            "role" to targetRole,
            "category" to category,
            "avatarName" to myAvatar,
            "status" to "PAIRED",
            "timestamp" to System.currentTimeMillis()
        )

        val myData = mapOf(
            "agentId" to targetAgentId,
            "hardwareId" to "HW_$targetAgentId",
            "deviceName" to targetDeviceName,
            "role" to role,
            "category" to category,
            "avatarName" to "avatar_custom_1",
            "status" to "PAIRED",
            "timestamp" to System.currentTimeMillis()
        )

        val updates = hashMapOf<String, Any>(
            "/devices/$targetAgentId/connections/$myAgentId" to targetData,
            "/devices/$myAgentId/connections/$targetAgentId" to myData
        )

        dbRef.updateChildren(updates)
            .addOnSuccessListener { onSuccess?.run() }
            .addOnFailureListener { e -> onError?.onError(e.message ?: "Failed to sync connection") }
    }

    @JvmStatic
    fun listenForIncomingConnections(myAgentId: String, callback: ConnectionCallback): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                for (child in snapshot.children) {
                    try {
                        val agentId = child.child("agentId").getValue(String::class.java) ?: child.key ?: continue
                        if (agentId == myAgentId) continue
                        val hwId = child.child("hardwareId").getValue(String::class.java) ?: "HW_$agentId"
                        val devName = child.child("deviceName").getValue(String::class.java) ?: "Paired Node $agentId"
                        val role = child.child("role").getValue(String::class.java) ?: "GUARDIAN"
                        val cat = child.child("category").getValue(String::class.java) ?: "FAMILY"
                        val avatar = child.child("avatarName").getValue(String::class.java) ?: "avatar_custom_1"

                        val model = LiveDeviceModel(
                            agentId = agentId,
                            hardwareId = hwId,
                            deviceName = devName,
                            role = role,
                            category = cat,
                            avatarName = avatar,
                            latitude = 0.0,
                            longitude = 0.0,
                            speed = 0f,
                            heading = 0f,
                            batteryPct = 100,
                            lastSeenTs = System.currentTimeMillis(),
                            isSos = false,
                            isGhost = false
                        )
                        callback.onConnection(model)
                    } catch (e: Exception) {
                        SystemLogger.e(TAG, "Error parsing incoming connection", e)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                SystemLogger.w(TAG, "Connections listener cancelled: ${error.message}")
            }
        }
        dbRef.child("devices").child(myAgentId).child("connections").addValueEventListener(listener)
        return listener
    }
}
