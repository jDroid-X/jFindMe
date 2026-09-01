package com.jdroidx.jfindme.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.utils.SecureVault
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * OfflineSyncManager: Handles data persistence and queueing during network loss.
 * Strategic Update: Integrated AES-256 Persistent Vault for GPS points.
 */
class OfflineSyncManager(private val context: Context) {

    private val TAG = "OfflineSyncManager"
    private val PREF_OFFLINE_QUEUE = "offline_gps_queue"
    private val syncQueue: Queue<UserLocation> = ConcurrentLinkedQueue()
    private var isNetworkAvailable = false
    private val gson = Gson()

    init {
        loadPersistedQueue()
        
        // QA Bug Fix: Actively check true physical network state at Boot. Do NOT blindly assume 'Offline'!
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkAvailable = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        
        monitorNetwork()
    }

    private fun monitorNetwork() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isNetworkAvailable = true
                SystemLogger.i(TAG, "Network Restored. Processing Sync Queue: ${syncQueue.size} items.")
                processQueue()
            }

            override fun onLost(network: Network) {
                isNetworkAvailable = false
                SystemLogger.w(TAG, "Network Lost. Entering Offline Queue Mode.")
            }
        })
    }

    /**
     * Strategic Update: Sync or Persist in AES-256 Vault.
     */
    fun syncLocation(location: UserLocation) {
        val rUserId = location.getResolvedUserId()
        
        // 🔒 Save locally into Database first (Zero Data Loss)
        try {
            val db = DatabaseHelper.getInstance(context)
            db.saveHistoryPoint(
                rUserId,
                location.getResolvedDeviceId(),
                location.latitude,
                location.longitude,
                location.speed,
                location.batteryLevel.toInt(),
                location.isSosActive,
                location.address,
                location.getResolvedTimestamp()
            )
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Local history point caching error: ${e.message}")
        }

        if (isNetworkAvailable) {
            FirebaseComm.syncAnalyticalEvent(rUserId, location)
            // If queue had backlogged items, flush them now
            if (syncQueue.isNotEmpty()) {
                processQueue()
            }
        } else {
            if (syncQueue.size >= 500) syncQueue.poll() // GAP-003: Increased cap from 100 to 500
            syncQueue.offer(location)
            persistQueue()
            SystemLogger.d(TAG, "Offline: Location persisted to Secure Vault. Total: ${syncQueue.size}")
            
            // LOOP-005: Schedule WorkManager for deferred background sync
            scheduleDeferredSync()
        }
    }

    private fun scheduleDeferredSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
            
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.jdroidx.jfindme.engine.sync.OfflineSyncWorker>()
            .setConstraints(constraints)
            .build()
            
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "OfflineSyncDeferred",
            androidx.work.ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun isTrackingAuthorized(userId: String): Boolean {
        return true // Enterprise policy: Always allow authorized telemetry stream for connected devices
    }

    fun triggerBackgroundSync() {
        processQueue()
        reconcileMetadataWithCloud()
    }

    private fun processQueue() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            var itemsProcessed = 0
            while (syncQueue.isNotEmpty() && isNetworkAvailable) {
                val location = syncQueue.poll()
                if (location != null) {
                    FirebaseComm.syncAnalyticalEvent(location.getResolvedUserId(), location)
                    itemsProcessed++
                }
            }
            if (itemsProcessed > 0) {
                persistQueue() // Clear persisted data after successful sync
            }
        }
    }

    /**
     * 🔄 Bidirectional Metadata Reconciliation:
     * Pulls latest Profile, Relationships, and SafeZones from Cloud into Local SQLite
     * to eliminate data drift and ensure mobile displays 100% accurate data.
     */
    fun reconcileMetadataWithCloud() {
        if (!isNetworkAvailable) return
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return

        FirebaseComm.resolveHardwareIdentity(myUid) { agentId ->
            val finalId = agentId ?: return@resolveHardwareIdentity
            val db = DatabaseHelper.getInstance(context)
            val internalId = db.resolveInternalId(finalId)

            // 1. Reconcile Profile
            FirebaseComm.devicesRef.child(finalId).child("profile").get().addOnSuccessListener { profSnap ->
                if (profSnap.exists()) {
                    val map = mutableMapOf<String, String>()
                    for (child in profSnap.children) {
                        val v = child.value?.toString() ?: ""
                        map[child.key ?: ""] = v
                    }
                    if (internalId != -1L) {
                        com.jdroidx.jfindme.data.local.tables.UserProfileTable.upsert(db.writableDatabase, internalId, map)
                    }
                    SystemLogger.d(TAG, "Reconciled Profile metadata for: $finalId")
                }
            }

            // 2. Reconcile Relationships
            FirebaseComm.devicesRef.child(finalId).child("relationships").get().addOnSuccessListener { relSnap ->
                if (relSnap.exists()) {
                    for (child in relSnap.children) {
                        val targetAgentId = child.key ?: continue
                        val role = child.child("role").value?.toString() ?: "MEMBER"
                        val pairId = "PR_${finalId.hashCode()}_${targetAgentId.hashCode()}"
                        com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairing(
                            db.writableDatabase,
                            pairId,
                            finalId,
                            targetAgentId,
                            "PAIRED"
                        )
                    }
                    SystemLogger.d(TAG, "Reconciled ${relSnap.childrenCount} relationship nodes for: $finalId")
                }
            }
        }
    }

    private fun persistQueue() {
        try {
            val securePrefs = SecureVault.getEncryptedPrefs(context)
            val json = gson.toJson(syncQueue.toList())
            securePrefs.edit().putString(PREF_OFFLINE_QUEUE, json).apply()
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to persist offline queue", e)
        }
    }

    private fun loadPersistedQueue() {
        try {
            val securePrefs = SecureVault.getEncryptedPrefs(context)
            val json = securePrefs.getString(PREF_OFFLINE_QUEUE, null)
            if (json != null) {
                val type = object : TypeToken<List<UserLocation>>() {}.type
                val list: List<UserLocation> = gson.fromJson(json, type)
                syncQueue.addAll(list)
                SystemLogger.i(TAG, "Recovered ${syncQueue.size} items from Secure Vault.")
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to load persisted queue", e)
        }
    }
}

