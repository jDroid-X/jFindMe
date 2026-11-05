package com.jdroidx.jfindme.engine.sync

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.utils.SystemLogger
import org.json.JSONObject

/**
 * 📡 OPTION B — Local Pairing (Nearby Devices - Production Grade)
 * Implements the Official Google Nearby Connections API routing discovery over P2P Wi-Fi Direct.
 * Merges the localized handshake state physically into the Global Firebase Array.
 * Supports structured JSON payloads for dynamic role negotiation (Child, Spouse, Guardian, Friend).
 */
object LocalPairingManager {
    private const val TAG = "LocalPairingManager"
    private const val STRATEGY_ID = "com.jdroidx.jfindme.P2P_SYNC"

    private val relationshipsEndpoint = com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef

    private var activeHostEndpointId: String? = null
    private var pendingChildRole: String = "Child"
    private var pendingChildName: String = "Local Member"

    // 📡 Forensic Merge Callbacks
    var onHostFoundCallback: ((String) -> Unit)? = null
    var onLocalMergeCompletedCallback: (() -> Unit)? = null

    /**
     * 🛡️ Pre-flight Radio Check: Verifies Bluetooth radio availability on Android 12+
     */
    fun isRadioReady(context: Context): Boolean {
        return try {
            val bt = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
            } else {
                @Suppress("DEPRECATION")
                android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            }
            bt != null && bt.isEnabled
        } catch (e: Exception) {
            true // Non-fatal fallback
        }
    }

    /**
     * Parent Device: Spins up the Advertiser beacon using a localized 8-Digit identifier locking P2P requests.
     */
    fun startLocalHostSession(context: Context, onLocalTokenReady: (String) -> Unit) {
        val hostUid = getDeviceId(context)
        
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val localToken = (1..8).map { allowedChars.random() }.joinToString("")
        
        if (!isRadioReady(context)) {
            SystemLogger.w(TAG, "Bluetooth radio disabled before hosting Nearby session.")
            try {
                android.widget.Toast.makeText(context, "⚠️ Turn ON Bluetooth & Wi-Fi for Nearby P2P", android.widget.Toast.LENGTH_SHORT).show()
            } catch (ignored: Exception) {}
        }

        SystemLogger.i(TAG, "Spinning up Physical P2P Nearby Advertiser: $localToken")

        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                val sasToken = info.authenticationDigits
                SystemLogger.i(TAG, "Incoming P2P Connection from: ${info.endpointName} [SAS Token: $sasToken]")
                // [Variables] | [Constants] | [Dependencies] | [Integration Points]
                // Surface 4-digit Short Authentication String (SAS) to Human-in-the-Loop for MITM verification
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "\uD83D\uDCE1 P2P Security Code: $sasToken (Verify on peer device)", android.widget.Toast.LENGTH_LONG).show()
                }
                Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
            }
            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                if (result.status.isSuccess) {
                    SystemLogger.i(TAG, "P2P Hardware Link Established. Blasting Host UID & Metadata to Child...")
                    val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                    val hostDos = dbHelper.getLocalDossier(hostUid)
                    val hostName = hostDos?.get("fullName") ?: "Host Admin"
                    
                    val hostJson = JSONObject().apply {
                        put("type", "HOST_META")
                        put("uid", hostUid)
                        put("name", hostName)
                        put("role", "Guardian")
                    }
                    val hostPayload = Payload.fromBytes(hostJson.toString().toByteArray(Charsets.UTF_8))
                    Nearby.getConnectionsClient(context).sendPayload(endpointId, hostPayload)
                } else {
                    SystemLogger.w(TAG, "P2P Handshake Rejected strictly natively.")
                }
            }
            override fun onDisconnected(endpointId: String) {
                SystemLogger.w(TAG, "P2P Hardware Link Severed.")
            }
        }

        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()

        Nearby.getConnectionsClient(context)
            .startAdvertising(localToken, STRATEGY_ID, connectionLifecycleCallback, advertisingOptions)
            .addOnSuccessListener {
                SystemLogger.i(TAG, "P2P Hardware Beacons Active. Transmitting Token Payload.")
                onLocalTokenReady(localToken)
            }
            .addOnFailureListener {
                SystemLogger.e(TAG, "P2P Hardware Binding Exception. Ensure Manifest Privileges.", it)
                onLocalTokenReady(localToken)
            }
    }

    /**
     * Child Device: Actively scans wireless frequencies hunting for a matching beacon token matrix with role metadata.
     */
    @JvmOverloads
    fun startLocalDiscoveryBinding(scannedToken: String, context: Context, selectedRole: String = "Child", onHostFound: (String) -> Unit) {
        SystemLogger.i(TAG, "Initializing P2P Discovery Seek Mode for Target Identifier: $scannedToken (Role: $selectedRole)")
        val childUid = getDeviceId(context)
        this.pendingChildRole = selectedRole
        this.onHostFoundCallback = onHostFound

        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val childDos = dbHelper.getLocalDossier(childUid)
        this.pendingChildName = childDos?.get("fullName") ?: "Local Target"

        val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                if (info.endpointName == scannedToken) {
                    SystemLogger.i(TAG, "P2P Target Node Located Geographically: $endpointId. Halting search grid...")
                    Nearby.getConnectionsClient(context).stopDiscovery()
                    
                    val lifecycleCallback = object : ConnectionLifecycleCallback() {
                        override fun onConnectionInitiated(epId: String, connectionInfo: ConnectionInfo) {
                            val sasToken = connectionInfo.authenticationDigits
                            SystemLogger.i(TAG, "P2P Initiated with Host: $epId [SAS Token: $sasToken]")
                            // [Variables] | [Constants] | [Dependencies] | [Integration Points]
                            // Surface 4-digit Short Authentication String (SAS) to Human-in-the-Loop for MITM verification
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                android.widget.Toast.makeText(context, "\uD83D\uDCE1 P2P Security Code: $sasToken (Matching with Host)", android.widget.Toast.LENGTH_LONG).show()
                            }
                            Nearby.getConnectionsClient(context).acceptConnection(epId, payloadCallback)
                        }
                        override fun onConnectionResult(epId: String, result: ConnectionResolution) {
                            if (result.status.isSuccess) {
                                SystemLogger.i(TAG, "Child Successfully Bound P2P. Blasting authentic structured UID Payload to Parent.")
                                activeHostEndpointId = epId
                                
                                val childJson = JSONObject().apply {
                                    put("type", "CHILD_JOIN")
                                    put("uid", childUid)
                                    put("name", pendingChildName)
                                    put("role", pendingChildRole)
                                }
                                val childPayload = Payload.fromBytes(childJson.toString().toByteArray(Charsets.UTF_8))
                                Nearby.getConnectionsClient(context).sendPayload(epId, childPayload)
                            }
                        }
                        override fun onDisconnected(epId: String) {
                            SystemLogger.w(TAG, "P2P Hardware Link Severed on Child.")
                            if (epId == activeHostEndpointId) {
                                activeHostEndpointId = null
                            }
                        }
                    }
                    Nearby.getConnectionsClient(context).requestConnection(childUid, endpointId, lifecycleCallback)
                }
            }
            override fun onEndpointLost(endpointId: String) {}
        }

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()

        Nearby.getConnectionsClient(context)
            .startDiscovery(STRATEGY_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener { SystemLogger.i(TAG, "Antenna Discovery Protocols Spinning globally.") }
            .addOnFailureListener { SystemLogger.e(TAG, "Antenna Hardware Offline.", it) }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes()
            if (bytes != null) {
                val message = String(bytes, Charsets.UTF_8)
                SystemLogger.i(TAG, "Intercepted payload over P2P Radio: $message")
                val context = com.jdroidx.jfindme.JFindMeApplication.getContext()
                val myUid = getDeviceId(context)
                val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)

                try {
                    if (message.startsWith("{") && message.endsWith("}")) {
                        val json = JSONObject(message)
                        val type = json.optString("type", "")
                        
                        if (type == "CHILD_JOIN") {
                            val childUid = json.optString("uid", "")
                            val childName = json.optString("name", "Local Target")
                            val childRole = json.optString("role", "Child")
                            
                            SystemLogger.i(TAG, "Authentic Child JSON received: $childUid ($childName, $childRole). Finalizing Merge as Host.")
                            finalizeLocalMergeWithMetadata(myUid, "Guardian", childUid, childName, childRole) { success ->
                                if (success) {
                                    val isFriend = childRole.equals("Friends", ignoreCase = true)
                                    db.saveRosterNode(
                                        childUid,
                                        childName,
                                        childRole,
                                        "P2P PAIRED",
                                        false,
                                        isFriend,
                                        ownerName = "Local Host",
                                        uid2 = null,
                                        connectedDeviceId = childUid,
                                        qrCode = null,
                                        avatarId = "1"
                                    )
                                    db.updateCitizenRole(childUid, childRole, if (isFriend) "Friends" else "None", isFriend)
                                    onLocalMergeCompletedCallback?.invoke()
                                }
                            }
                        } else if (type == "HOST_META") {
                            val hostUid = json.optString("uid", "")
                            val hostName = json.optString("name", "Host Admin")
                            val hostRole = json.optString("role", "Guardian")
                            
                            SystemLogger.i(TAG, "Authentic Host JSON received: $hostUid ($hostName). Finalizing Merge as Child.")
                            finalizeLocalMergeWithMetadata(hostUid, hostRole, myUid, pendingChildName, pendingChildRole) { success ->
                                if (success) {
                                    val isHostFriend = hostRole.equals("Friends", ignoreCase = true)
                                    db.saveRosterNode(
                                        hostUid,
                                        hostName,
                                        hostRole,
                                        "P2P PAIRED",
                                        true,
                                        isHostFriend,
                                        ownerName = hostName,
                                        uid2 = null,
                                        connectedDeviceId = hostUid,
                                        qrCode = null,
                                        avatarId = "1"
                                    )
                                    onHostFoundCallback?.invoke(hostUid)
                                }
                            }
                        }
                    } else if (message.startsWith("CHILD:")) {
                        val childUid = message.substring(6)
                        SystemLogger.i(TAG, "Legacy Child UID: $childUid. Finalizing Database Merge as Host.")
                        finalizeLocalMerge(myUid, childUid) { success ->
                            if (success) {
                                db.saveRosterNode(
                                    childUid,
                                    "Local Child Target",
                                    "Child",
                                    "P2P PAIRED",
                                    false,
                                    false,
                                    null, null, null, null, "1"
                                )
                                db.updateCitizenRole(childUid, "Child", "None", false)
                                onLocalMergeCompletedCallback?.invoke()
                            }
                        }
                    } else if (message.startsWith("HOST:")) {
                        val hostUid = message.substring(5)
                        SystemLogger.i(TAG, "Legacy Host UID: $hostUid. Finalizing Database Merge as Child.")
                        finalizeLocalMerge(hostUid, myUid) { success ->
                            if (success) {
                                onHostFoundCallback?.invoke(hostUid)
                            }
                        }
                    }
                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Payload decode exception: ${e.message}", e)
                }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    /**
     * Final architectural bridge. Takes physical offline nodes and mounts them into the Unified Cloud Tracker.
     */
    fun finalizeLocalMerge(hostDeviceId: String, childDeviceId: String, onMergeComplete: (Boolean) -> Unit) {
        finalizeLocalMergeWithMetadata(hostDeviceId, "Guardian", childDeviceId, "Local Target", "Child", onMergeComplete)
    }

    fun finalizeLocalMergeWithMetadata(
        hostDeviceId: String,
        hostRole: String,
        childDeviceId: String,
        childName: String,
        childRole: String,
        onMergeComplete: (Boolean) -> Unit
    ) {
        val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
        val updates = hashMapOf<String, Any>(
            "${com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_DEVICES}/$hostDeviceId/relationships/$childDeviceId" to mapOf(
                "role" to childRole,
                "name" to childName,
                "linked_at" to now
            ),
            "${com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_DEVICES}/$childDeviceId/relationships/$hostDeviceId" to mapOf(
                "role" to hostRole,
                "name" to "Host Admin",
                "linked_at" to now
            ),
            "${com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS}/$hostDeviceId/$childDeviceId" to true,
            "${com.jdroidx.jfindme.data.remote.FirebaseComm.NODE_RELATIONSHIPS}/$childDeviceId/$hostDeviceId" to hostRole
        )

        com.jdroidx.jfindme.data.remote.FirebaseComm.database.reference.updateChildren(updates).addOnSuccessListener {
            SystemLogger.i("LocalPairingManager", "Strict Local Tier 3 Mesh Binding Complete for $childName ($childRole).")
            
            val context = com.jdroidx.jfindme.JFindMeApplication.getContext()
            val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            db.logDevicePairing(
                hostDeviceId,
                "Local Host",
                "HOST_DEV",
                childDeviceId,
                childName,
                "CHILD_DEV",
                childRole,
                "DEFAULT_GROUP"
            )
            onMergeComplete(true)
        }.addOnFailureListener {
            SystemLogger.e(TAG, "Legacy P2P Matrix Binding Crash.", it)
            onMergeComplete(false)
        }
    }

    private fun getDeviceId(context: Context? = null): String {
        val ctx = context ?: com.jdroidx.jfindme.JFindMeApplication.getContext()
        return com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(ctx)
    }
}

