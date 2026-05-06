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
        // 🔒 ACL FIREWALL ENFORCEMENT: (V11.2)
        // Before uploading, verify if the current user has granted 'Live Sync' permission in their Local Dossier.
        val rUserId = location.getResolvedUserId()
        if (!isTrackingAuthorized(rUserId)) {
            SystemLogger.w(TAG, "Sync Blocked: ACL Live Tracking permission is currently DENIED for Node: $rUserId")
            return
        }

        if (isNetworkAvailable) {
            FirebaseComm.syncAnalyticalEvent(location.getResolvedUserId(), location)
        } else {
            if (syncQueue.size > 100) syncQueue.poll()
            syncQueue.offer(location)
            persistQueue()
            SystemLogger.d(TAG, "Offline: Location persisted to Secure Vault. Total: ${syncQueue.size}")
        }
    }

    private fun isTrackingAuthorized(userId: String): Boolean {
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return false
        if (userId != myUid) return true // Only the owner can restrict their own data flow here
        
        val db = DatabaseHelper.getInstance(context)
        val dossier = db.getLocalDossier(userId)
        return dossier?.get("acl_live_tracking") != "DENIED"
    }

    private fun processQueue() {
        while (syncQueue.isNotEmpty() && isNetworkAvailable) {
            val location = syncQueue.poll()
            if (location != null) {
                FirebaseComm.syncAnalyticalEvent(location.getResolvedUserId(), location)
            }
        }
        persistQueue() // Clear persisted data after successful sync
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

