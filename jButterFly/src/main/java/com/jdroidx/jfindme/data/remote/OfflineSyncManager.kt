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
import com.jdroidx.jfindme.utils.RegistryManager
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * OfflineSyncManager: Handles data persistence and queueing during network loss.
 * Strategic Update: Integrated AES-256 Persistent Vault for GPS points.
 */
class OfflineSyncManager private constructor(context: Context) {

    private val context = context.applicationContext
    private val TAG = "OfflineSyncManager"
    private val PREF_OFFLINE_QUEUE = "offline_gps_queue"
    private val syncQueue: Queue<UserLocation> = ConcurrentLinkedQueue()
    @Volatile private var isNetworkAvailable = false
    private val gson = Gson()
    private var isNetworkMonitoringActive = false

    // 📡 HUMAN-IN-THE-LOOP STATE BROADCASTERS
    val isOnlineLiveData = androidx.lifecycle.MutableLiveData<Boolean>(false)
    val pendingQueueCountLiveData = androidx.lifecycle.MutableLiveData<Int>(0)
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<OfflineSyncListener>()

    interface OfflineSyncListener {
        fun onNetworkStatusChanged(isOnline: Boolean)
        fun onSyncFlushed(itemsFlushed: Int)
    }

    fun addListener(listener: OfflineSyncListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: OfflineSyncListener) {
        listeners.remove(listener)
    }

    fun getPendingQueueCount(): Int {
        return syncQueue.size + DatabaseHelper.getInstance(context).getPendingSyncCount()
    }

    companion object {
        @Volatile private var instance: OfflineSyncManager? = null

        @JvmStatic
        fun getInstance(context: Context): OfflineSyncManager {
            return instance ?: synchronized(this) {
                instance ?: OfflineSyncManager(context).also { instance = it }
            }
        }
    }

    init {
        loadPersistedQueue()
        
        // QA Bug Fix: Actively check true physical network state at Boot.
        try {
            val connectivityManager = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
            isNetworkAvailable = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            isOnlineLiveData.postValue(isNetworkAvailable)
        } catch (e: Exception) {
            isNetworkAvailable = true
            isOnlineLiveData.postValue(true)
        }
        
        monitorNetwork()
    }

    @Synchronized
    private fun monitorNetwork() {
        if (isNetworkMonitoringActive) return
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isNetworkAvailable = true
                    isOnlineLiveData.postValue(true)
                    SystemLogger.i(TAG, "Network Restored. Processing Sync Queue: ${syncQueue.size} items.")
                    listeners.forEach { it.onNetworkStatusChanged(true) }
                    processQueue()
                    reconcileMetadataWithCloud()
                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.flushPendingMasterSyncQueue(context)
                }

                override fun onLost(network: Network) {
                    isNetworkAvailable = false
                    isOnlineLiveData.postValue(false)
                    SystemLogger.w(TAG, "Network Lost. Entering Offline Queue Mode.")
                    listeners.forEach { it.onNetworkStatusChanged(false) }
                }
            })
            isNetworkMonitoringActive = true
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Could not register network callback (handled gracefully): ${e.message}")
        }
    }

    /**
     * Strategic Update: Sync or Persist in AES-256 Vault.
     */
    fun syncLocation(location: UserLocation) {
        // 🛡️ Anti-GIGO Guard: Do not sync corrupt, NaN, or null-island coordinates
        if (!location.isValidLocation()) {
            SystemLogger.w(TAG, "Rejected invalid GPS ping (lat: ${location.latitude}, lng: ${location.longitude})")
            return
        }
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
                location.getResolvedTimestamp(),
                location.accuracy,
                location.bearing,
                location.altitude,
                location.motion_state
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
            val maxQueue = RegistryManager.getOfflineQueueMaxPoints(context)
            if (syncQueue.size >= maxQueue) syncQueue.poll()
            syncQueue.offer(location)

            // Also persist into SQLite PendingSyncQueueTable for Zero Data Loss
            try {
                val db = DatabaseHelper.getInstance(context)
                db.enqueuePendingSync(
                    payloadType = if (location.isSosActive) "SOS" else "LOCATION",
                    targetUid = rUserId,
                    lat = location.latitude,
                    lng = location.longitude,
                    bearing = location.bearing,
                    speed = location.speed,
                    battery = location.batteryLevel.toInt(),
                    isSos = location.isSosActive,
                    accuracy = location.accuracy,
                    altitude = location.altitude,
                    motionState = location.motion_state
                )
            } catch (e: Exception) {
                SystemLogger.e(TAG, "SQLite pending queue enqueue error: ${e.message}")
            }

            val totalPending = syncQueue.size + DatabaseHelper.getInstance(context).getPendingSyncCount()
            pendingQueueCountLiveData.postValue(totalPending)
            persistQueue()
            SystemLogger.d(TAG, "Offline: Location persisted to Secure Vault. Total: $totalPending (Limit: $maxQueue)")
            
            // LOOP-005: Schedule WorkManager for deferred background sync
            scheduleDeferredSync()
        }
    }

    private fun scheduleDeferredSync() {
        val networkType = if (RegistryManager.isSyncWifiOnly(context)) 
            androidx.work.NetworkType.UNMETERED 
        else 
            androidx.work.NetworkType.CONNECTED

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(networkType)
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
        return true
    }

    
    /**
     * ⚖️ Reconciles all pending balance data accumulated between yesterday (T - 24h) and today (T_0).
     * Synchronously extracts pending records from DatabaseHelper, pushes to Firebase, and marks as synced.
     */
    fun reconcileYesterdayTodayBalance(): Int {
        val now = System.currentTimeMillis()
        val yesterday = now - (24 * 60 * 60 * 1000L)
        var itemsProcessed = 0

        try {
            val db = DatabaseHelper.getInstance(context)
            val deltaList = db.getBalanceDeltaQueue(yesterday, now, limit = 200)
            if (deltaList.isEmpty()) {
                SystemLogger.i(TAG, "Balance delta reconciliation: Zero pending records between yesterday and today.")
                return 0
            }

            SystemLogger.i(TAG, "Balance delta reconciliation starting for ${deltaList.size} records.")
            val syncedIds = mutableListOf<Long>()

            for (item in deltaList) {
                val id = (item["id"] as? Long) ?: continue
                val targetUid = (item["target_uid"] as? String) ?: ""
                val lat = (item["lat"] as? Double) ?: 0.0
                val lng = (item["lng"] as? Double) ?: 0.0
                val bearing = (item["bearing"] as? Double)?.toFloat() ?: 0f
                val speed = (item["speed"] as? Double)?.toFloat() ?: 0f
                val battery = (item["battery"] as? Long)?.toInt() ?: 100
                val isSos = (item["is_sos"] as? Long) == 1L
                val ts = (item["timestamp"] as? Long) ?: System.currentTimeMillis()
                val accuracy = (item["accuracy"] as? Double)?.toFloat() ?: ((item["accuracy"] as? Long)?.toFloat() ?: 0f)
                val altitude = (item["altitude"] as? Double) ?: ((item["altitude"] as? Long)?.toDouble() ?: 0.0)
                val motionState = (item["motion_state"] as? String) ?: "UNKNOWN"

                if (targetUid.isNotBlank() && (lat != 0.0 || lng != 0.0)) {
                    val ul = UserLocation(
                        userId = targetUid,
                        latitude = lat,
                        longitude = lng,
                        bearing = bearing,
                        speed = speed,
                        accuracy = accuracy,
                        altitude = altitude,
                        motion_state = motionState,
                        batteryLevel = battery,
                        isSosActive = isSos,
                        timestamp = FirebaseComm.getForensicTimestamp(ts)
                    )
                    FirebaseComm.syncAnalyticalEvent(targetUid, ul) { success ->
                        if (success) {
                            syncedIds.add(id)
                            itemsProcessed++
                        }
                    }
                }
            }

            if (syncedIds.isNotEmpty()) {
                db.markBalanceDeltaSynced(syncedIds)
            }

            val totalRemaining = syncQueue.size + db.getPendingSyncCount()
            pendingQueueCountLiveData.postValue(totalRemaining)
            RegistryManager.putString(context, "balance_delta_last_synced_ts", FirebaseComm.getForensicTimestamp(now))
            RegistryManager.putInt(context, "balance_delta_last_synced_count", itemsProcessed)
            SystemLogger.i(TAG, "Balance delta reconciliation finished. Synced $itemsProcessed records.")
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Error in reconcileYesterdayTodayBalance: ${e.message}")
        }
        return itemsProcessed
    }

    fun triggerBackgroundSync() {
        processQueue()
        reconcileMetadataWithCloud()
        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.flushPendingMasterSyncQueue(context)
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

            // Also drain persistent SQLite pending_sync_queue
            try {
                val db = DatabaseHelper.getInstance(context)
                val sqlitePending = db.getPendingSyncQueue(50)
                for (item in sqlitePending) {
                    val id = (item["id"] as? Long) ?: continue
                    val targetUid = (item["target_uid"] as? String) ?: ""
                    val lat = (item["lat"] as? Double) ?: 0.0
                    val lng = (item["lng"] as? Double) ?: 0.0
                    val bearing = (item["bearing"] as? Double)?.toFloat() ?: 0f
                    val speed = (item["speed"] as? Double)?.toFloat() ?: 0f
                    val battery = (item["battery"] as? Long)?.toInt() ?: 100
                    val isSos = (item["is_sos"] as? Long) == 1L
                    val ts = (item["timestamp"] as? Long) ?: System.currentTimeMillis()
                    val accuracy = (item["accuracy"] as? Double)?.toFloat() ?: ((item["accuracy"] as? Long)?.toFloat() ?: 0f)
                    val altitude = (item["altitude"] as? Double) ?: ((item["altitude"] as? Long)?.toDouble() ?: 0.0)
                    val motionState = (item["motion_state"] as? String) ?: "UNKNOWN"

                    if (targetUid.isNotBlank() && (lat != 0.0 || lng != 0.0)) {
                        val ul = UserLocation(
                            userId = targetUid,
                            latitude = lat,
                            longitude = lng,
                            bearing = bearing,
                            speed = speed,
                            accuracy = accuracy,
                            altitude = altitude,
                            motion_state = motionState,
                            batteryLevel = battery,
                            isSosActive = isSos,
                            timestamp = FirebaseComm.getForensicTimestamp(ts)
                        )
                        FirebaseComm.syncAnalyticalEvent(targetUid, ul) { success -> if (success) { db.deletePendingSyncItem(id); itemsProcessed++ } }
                        // db.deletePendingSyncItem handled above upon verified ACK
                        // itemsProcessed incremented upon verified ACK
                    }
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error draining sqlite pending sync queue: ${e.message}")
            }

            val totalRemaining = syncQueue.size + DatabaseHelper.getInstance(context).getPendingSyncCount()
            pendingQueueCountLiveData.postValue(totalRemaining)
            persistQueue()
            if (itemsProcessed > 0) {
                listeners.forEach { it.onSyncFlushed(itemsProcessed) }
                SystemLogger.i(TAG, "Successfully flushed $itemsProcessed offline trace points to Cloud Matrix.")
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
                    try {
                        context.sendBroadcast(android.content.Intent("com.jdroidx.jfindme.ACTION_PROFILE_SYNCED").putExtra("agent_id", finalId))
                    } catch (e: Exception) { }
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

            // 3. Reconcile SafeZones (Primary: /Geofencing/{creator}/safe_zones, Fallback: /devices/{creator}/safe_zones)
            val primarySafeZonesRef = FirebaseComm.getGeofencingSafeZonesRef(finalId)
            primarySafeZonesRef.get().addOnSuccessListener { zoneSnap ->
                val activeSnap = if (zoneSnap.exists()) zoneSnap else null
                val processZones = { snap: com.google.firebase.database.DataSnapshot ->
                    for (child in snap.children) {
                        val zoneId = child.key ?: continue
                        val name = child.child("zone_name").value?.toString() ?: child.child("name").value?.toString() ?: "Safe Zone"
                        val lat = child.child("lat").value?.toString()?.toDoubleOrNull() ?: 0.0
                        val lng = child.child("lng").value?.toString()?.toDoubleOrNull() ?: 0.0
                        val radius = child.child("radius").value?.toString()?.toDoubleOrNull() ?: child.child("radius_meters").value?.toString()?.toDoubleOrNull() ?: 100.0
                        val isActive = child.child("is_active").getValue(Boolean::class.java) ?: true
                        val alertsEnabled = child.child("alerts_enabled").getValue(Boolean::class.java) ?: true
                        val polygonPoints = child.child("polygon_points").value?.toString() 
                            ?: child.child("polygonPointsJson").value?.toString() 
                            ?: ""
                        val dwellMinutes = child.child("dwell_minutes").value?.toString()?.toIntOrNull() 
                            ?: child.child("dwellThresholdMinutes").value?.toString()?.toIntOrNull() 
                            ?: 0
                        
                        if (lat != 0.0 && lng != 0.0) {
                            com.jdroidx.jfindme.data.local.tables.SafeZoneTable.save(
                                db.writableDatabase,
                                zoneId,
                                name,
                                lat,
                                lng,
                                radius,
                                if (internalId != -1L) internalId else null,
                                finalId,
                                isActive,
                                alertsEnabled,
                                false,
                                polygonPoints,
                                dwellMinutes
                            )
                        }
                    }
                    SystemLogger.d(TAG, "Reconciled ${snap.childrenCount} SafeZone nodes into SQLite for: $finalId")
                }

                if (activeSnap != null) {
                    processZones(activeSnap)
                } else {
                    FirebaseComm.devicesRef.child(finalId).child("safe_zones").get().addOnSuccessListener { fallbackSnap ->
                        if (fallbackSnap.exists()) processZones(fallbackSnap)
                    }
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

