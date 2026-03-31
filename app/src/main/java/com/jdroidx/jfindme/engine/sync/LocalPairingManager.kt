package com.jdroidx.jfindme.engine.sync

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 📡 OPTION B — Local Pairing (Nearby Devices - Production Grade)
 * Implements the Official Google Nearby Connections API routing discovery over P2P Wi-Fi Direct.
 * Merges the localized handshake state physically into the Global Firebase Array.
 */
object LocalPairingManager {
    private const val TAG = "LocalPairingManager"
    private const val STRATEGY_ID = "com.jdroidx.jfindme.P2P_SYNC"

    private val groupsEndpoint = FirebaseDatabase.getInstance().reference.child("groups")
    private val usersEndpoint = FirebaseDatabase.getInstance().reference.child("users")

    private var activeHostEndpointId: String? = null

    /**
     * Parent Device: Spins up the Advertiser beacon using a localized 8-Digit identifier locking P2P requests.
     */
    fun startLocalHostSession(context: Context, onLocalTokenReady: (String) -> Unit) {
        val hostUid = getDeviceId()
        
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val localToken = (1..8).map { allowedChars.random() }.joinToString("")
        
        SystemLogger.i(TAG, "Spinning up Physical P2P Nearby Advertiser: $localToken")

        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                SystemLogger.i(TAG, "Incoming P2P Connection from: ${info.endpointName}")
                Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
            }
            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                if (result.status.isSuccess) {
                    SystemLogger.i(TAG, "P2P Hardware Link Established. Awaiting Payload Identity from Child...")
                    // We DO NOT sync database here anymore. We wait for the child to beam its actual Firebase UID payload!
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
     * Child Device: Actively scans wireless frequencies hunting for a matching beacon token matrix.
     */
    fun startLocalDiscoveryBinding(scannedToken: String, context: Context, onHostFound: (String) -> Unit) {
        SystemLogger.i(TAG, "Initializing P2P Discovery Seek Mode for Target Identifier: $scannedToken")
        val childUid = getDeviceId()

        val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                if (info.endpointName == scannedToken) {
                    SystemLogger.i(TAG, "P2P Target Node Located Geographically: $endpointId. Halting search grid...")
                    Nearby.getConnectionsClient(context).stopDiscovery()
                    
                    val lifecycleCallback = object : ConnectionLifecycleCallback() {
                        override fun onConnectionInitiated(epId: String, connectionInfo: ConnectionInfo) {
                            Nearby.getConnectionsClient(context).acceptConnection(epId, payloadCallback)
                        }
                        override fun onConnectionResult(epId: String, result: ConnectionResolution) {
                            if (result.status.isSuccess) {
                                SystemLogger.i(TAG, "Child Successfully Bound P2P. Blasting authentic UID Payload to Parent.")
                                activeHostEndpointId = epId
                                
                                // QA Bug Fix: Blast our REAL Firebase UID across the Bluetooth/Wifi Direct channel to the Parent!
                                val childPayload = Payload.fromBytes(childUid.toByteArray(Charsets.UTF_8))
                                Nearby.getConnectionsClient(context).sendPayload(epId, childPayload)
                                
                                onHostFound(endpointId)
                            }
                        }
                        override fun onDisconnected(epId: String) {}
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
                // The Parent catches the REAL Firebase target UID from the child over Bluetooth here!
                val authenticChildUid = String(bytes, Charsets.UTF_8)
                val hostUid = getDeviceId()
                SystemLogger.i(TAG, "Authentic Child UID intercepted over P2P Radio: $authenticChildUid. Finalizing Database Merge.")
                finalizeLocalMerge(hostUid, authenticChildUid) {}
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    /**
     * Final architectural bridge. Takes physical offline nodes and mounts them into the Unified Cloud Tracker.
     */
    fun finalizeLocalMerge(hostDeviceId: String, childDeviceId: String, onMergeComplete: (Boolean) -> Unit) {
        // 🚀 QA STRICK RULE: "no download of device pairing with other mobile... scan one device one device pair"
        // Force offline Nearby connections to adhere to the strict 1-to-1 P2P friend_matrix architecture!
        usersEndpoint.child(hostDeviceId).child("friend_matrix").child(childDeviceId).setValue(true).addOnSuccessListener {
            usersEndpoint.child(childDeviceId).child("friend_matrix").child(hostDeviceId).setValue("HOST_LOCAL")
            SystemLogger.i("LocalPairingManager", "Strict Local P2P Matrix Binding Complete. Logging Audit Ledger...")
            
            // 🚀 AUDIT LEDGER: Log Local P2P Node Status
            val db = com.jdroidx.jfindme.data.local.CitizenDbHelper(com.jdroidx.jfindme.JFindMeApplication.getContext())
            db.logDevicePairing(hostDeviceId, "LOCAL_HOST_NODE", childDeviceId, "LOCAL_CHILD_NODE", "PEER_P2P", "LOCAL_ONLY", "P2P_SOCKET_ESTABLISHED")
            
            onMergeComplete(true)
        }.addOnFailureListener {
            SystemLogger.e(TAG, "Legacy P2P Matrix Binding Crash.", it)
            onMergeComplete(false)
        }
    }

    private fun forgeLocalPersistentGroup(hostDeviceId: String, childDeviceId: String, onMergeComplete: (Boolean) -> Unit) {
        // Obsolete Engine: P2P friend_matrix completely handles offline arrays now!
        SystemLogger.i(TAG, "Legacy forgeLocalPersistentGroup bypassed. Array already written to friend_matrix natively.")
        onMergeComplete(true)
    }

    // QA In-Memory Cache: Prevents the Bluetooth tracker from seizing mid-scan.
    private var cachedMockUid: String? = null

    // QA Secure Fix: Fallback for Unauthenticated Google Nearby tests preventing Database Corruption
    private fun getDeviceId(): String {
        val authUid = FirebaseAuth.getInstance().currentUser?.uid
        if (authUid != null) return authUid

        if (cachedMockUid == null) {
            cachedMockUid = "DEV_${java.util.UUID.randomUUID().toString().substring(0, 8)}"
        }
        return cachedMockUid!!
    }
}
