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
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * OfflineSyncManager: Handles data persistence and queueing during network loss.
 * Strategic Update: Integrated AES-256 Persistent Vault for GPS points.
 * 🚀 V83: Latest Data First LIFO Prioritization Engine.
 *
 * [Variables]: syncQueue (ConcurrentLinkedDeque), isNetworkAvailable, gson
 * [Constants]: TAG, PREF_OFFLINE_QUEUE
 * [Dependencies]: UserLocation, DatabaseHelper, FirebaseComm, SecureVault
 * [Integration Points]: LocationRepository, TrackingService, MasterDataSyncSentinel
 */
class OfflineSyncManager private constructor(context: Context) {

    private val context = context.applicationContext
    private val TAG = "OfflineSyncManager"
    private val PREF_OFFLINE_QUEUE = "offline_gps_queue"
    private val syncQueue: java.util.concurrent.ConcurrentLinkedDeque<UserLocation> = ConcurrentLinkedDeque()
    @Volatile private var isNetworkAvailable = false
    private val gson = Gson()
    private var isNetworkMonitoringActive = false

    // 🔋 SPARK QUOTA SENTINEL: Intelligent displacement & heartbeat throttling state
    @Volatile private var lastDispatchedLat: Double = 0.0
    @Volatile private var lastDispatchedLng: Double = 0.0
    @Volatile private var lastDispatchedTs: Long = 0L
    @Volatile private var lastDispatchedBattery: Int = -1

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
        return syncQueue.size + com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context).getPendingSyncCount()
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

    // [Variables] networkDebounceJob | [Constants] NETWORK_DEBOUNCE_MS = 3000L | [Dependencies] CoroutineScope, Dispatchers, delay | [Integration Points] NetworkCallback
    private var networkDebounceJob: kotlinx.coroutines.Job? = null

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
                    listeners.forEach { it.onNetworkStatusChanged(true) }

                    // 🛡️ AIRPLANE MODE RAPID-CYCLE DEBOUNCE (3000ms Baseband Settling Delay)
                    networkDebounceJob?.cancel()
                    networkDebounceJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        kotlinx.coroutines.delay(3000L)
                        if (isNetworkAvailable) {
                            SystemLogger.i(TAG, "Network Restored & Debounced (3000ms). Processing Sync Queue: ${syncQueue.size} items.")
                            processQueue()
                            reconcileMetadataWithCloud()
                            com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.flushPendingMasterSyncQueue(context)
                        }
                    }
                }

                override fun onLost(network: Network) {
                    networkDebounceJob?.cancel()
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
     * [Variables]: rUserId, db, dispatchCloud | [Constants]: TAG | [Dependencies]: UserLocation, DatabaseHelper, FirebaseComm
     * [Integration Points]: LocationRepository.finalizeLocationPush, Zero-Duplicate-Write Single-Push Guard
     */
    @JvmOverloads
    fun syncLocation(location: UserLocation, dispatchCloud: Boolean = true) {
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

        val isCloudReady = isNetworkAvailable && FirebaseComm.isAuthenticated()
        if (isCloudReady) {
            // 🚀 SPARK QUOTA OPTIMIZER: Throttle redundant stationary GPS jitter to protect free tier limit
            val now = System.currentTimeMillis()
            val minDisplacement = RegistryManager.getMinDisplacementMeters(context)
            val distMoved = if (lastDispatchedLat != 0.0 && lastDispatchedLng != 0.0) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(lastDispatchedLat, lastDispatchedLng, location.latitude, location.longitude, results)
                results[0]
            } else {
                Float.MAX_VALUE
            }
            val timeSinceLast = now - lastDispatchedTs
            // 🔋 BATTERY & QUOTA SENTINEL: Enforce dynamic presence pulse interval (default 180s) when stationary
            val presenceInterval = RegistryManager.getPresencePulseIntervalMs(context)
            val isHeartbeatDue = timeSinceLast >= presenceInterval
            val isSignificantMove = distMoved >= minDisplacement
            val isBatteryChanged = kotlin.math.abs(location.batteryLevel.toInt() - lastDispatchedBattery) >= 3
            val shouldDispatchCloud = location.isSosActive || isSignificantMove || isHeartbeatDue || isBatteryChanged

            if (dispatchCloud && shouldDispatchCloud) {
                lastDispatchedLat = location.latitude
                lastDispatchedLng = location.longitude
                lastDispatchedTs = now
                lastDispatchedBattery = location.batteryLevel.toInt()
                FirebaseComm.syncAnalyticalEvent(rUserId, location)
            }
            // If queue had backlogged items, flush them now in priority order
            if (syncQueue.isNotEmpty()) {
                processQueue()
            }
        } else {
            val maxQueue = RegistryManager.getOfflineQueueMaxPoints(context)
            if (syncQueue.size >= maxQueue) syncQueue.pollLast() // Drop oldest item from back
            syncQueue.offerFirst(location) // 🚀 Latest data placed at FRONT of sync queue (LIFO priority)

            // Also persist into SQLite PendingSyncQueueTable for Zero Data Loss
            try {
                val repo = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
                repo.enqueuePendingSync(
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

            val totalPending = syncQueue.size + com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context).getPendingSyncCount()
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
     * ⚖️ Reconciles all pending balance data accumulated across the previous 26 hours (T - 26h) to today (T_0).
     * Atomically extracts pending records from DatabaseHelper, pushes in a single atomic batch to Firebase,
     * and marks as synced.
     */
    // [Variables] now, hoursWindow, windowStartTime, itemsProcessed, db, deltaList, batchUpdates, candidates, countLatch, batchSucceeded, totalRemaining
    // [Constants] limit = 100
    // [Dependencies] DatabaseHelper, FirebaseAtomicGateway, FirebaseComm, RegistryManager
    // [Integration Points] MaintenanceSyncScheduler, MasterDataSyncSentinel
    fun reconcileYesterdayTodayBalance(): Int {
        val now = System.currentTimeMillis()
        val hoursWindow = RegistryManager.getInt(context, "maintenance_sync_window_hours", 26)
        val windowStartTime = now - (hoursWindow * 60 * 60 * 1000L)
        var itemsProcessed = 0

        try {
            val db = DatabaseHelper.getInstance(context)
            // 🛡️ RECONNECTION RATE-LIMITER: Clamp to 100 records per cycle to prevent socket congestion
            val deltaList = db.getBalanceDeltaQueue(windowStartTime, now, limit = 100)
            if (deltaList.isEmpty()) {
                SystemLogger.i(TAG, "Balance delta reconciliation: Zero pending records in previous ${hoursWindow}h window.")
                return 0
            }

            SystemLogger.i(TAG, "Balance delta reconciliation starting for ${deltaList.size} records (window: ${hoursWindow}h).")
            val batchUpdates = mutableMapOf<String, Any>()
            val candidates = mutableListOf<Long>()

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
                    val updates = FirebaseComm.buildLocationUpdates(targetUid, ul)
                    batchUpdates.putAll(updates)
                    candidates.add(id)
                }
            }

            if (batchUpdates.isNotEmpty()) {
                val countLatch = java.util.concurrent.CountDownLatch(1)
                var batchSucceeded = false
                FirebaseAtomicGateway.updateChildrenAtomic(FirebaseComm.database.reference, batchUpdates) { success, _ ->
                    batchSucceeded = success
                    countLatch.countDown()
                }
                countLatch.await(10, java.util.concurrent.TimeUnit.SECONDS)

                if (batchSucceeded) {
                    db.markBalanceDeltaSynced(candidates)
                    itemsProcessed = candidates.size
                }
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
        // [Variables] itemsProcessed, batchUpdates, syncedItemIds, expiredIds, db, sqlitePending, totalRemaining, poppedMemItems
        // [Constants] None
        // [Dependencies] DatabaseHelper, FirebaseAtomicGateway, FirebaseComm, UserLocation
        // [Integration Points] TrackingService, LocationRepository, OfflineSyncWorker
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            var itemsProcessed = 0
            val batchUpdates = mutableMapOf<String, Any>()
            val syncedItemIds = mutableListOf<Long>()
            val expiredIds = mutableListOf<Long>()
            val poppedMemItems = mutableListOf<UserLocation>()

            // 🚀 Latest Data First Priority: Drain newest memory queued pings first into atomic batch
            while (syncQueue.isNotEmpty() && isNetworkAvailable) {
                val location = syncQueue.pollFirst()
                if (location != null) {
                    val memUpdates = FirebaseComm.buildLocationUpdates(location.getResolvedUserId(), location)
                    batchUpdates.putAll(memUpdates)
                    poppedMemItems.add(location)
                    itemsProcessed++
                }
            }

            // 🚀 Drain persistent SQLite pending_sync_queue with LATEST TIMESTAMP FIRST (DESC)
            try {
                val db = DatabaseHelper.getInstance(context)
                val sqlitePending = db.getLatestPendingSyncQueue(50)
                val retentionDays = db.getDynamicConfigInt("retention_days", 7)
                val maxAgeMs = retentionDays * 24 * 3600 * 1000L
                val now = System.currentTimeMillis()

                for (item in sqlitePending) {
                    val id = (item["id"] as? Long) ?: continue
                    val targetUid = (item["target_uid"] as? String) ?: ""
                    val lat = (item["lat"] as? Double) ?: 0.0
                    val lng = (item["lng"] as? Double) ?: 0.0
                    val bearing = (item["bearing"] as? Double)?.toFloat() ?: 0f
                    val speed = (item["speed"] as? Double)?.toFloat() ?: 0f
                    val battery = (item["battery"] as? Long)?.toInt() ?: 100
                    val isSos = (item["is_sos"] as? Long) == 1L
                    val ts = (item["timestamp"] as? Long) ?: now
                    val accuracy = (item["accuracy"] as? Double)?.toFloat() ?: ((item["accuracy"] as? Long)?.toFloat() ?: 0f)
                    val altitude = (item["altitude"] as? Double) ?: ((item["altitude"] as? Long)?.toDouble() ?: 0.0)
                    val motionState = (item["motion_state"] as? String) ?: "UNKNOWN"

                    // 🛡️ Age Boundary Guard (TS-06): Suppress queue items older than dynamic retention ceiling
                    val itemAge = now - ts
                    if (itemAge > maxAgeMs) {
                        expiredIds.add(id)
                        SystemLogger.w(TAG, "Suppressed expired outbox telemetry item $id (age: ${itemAge / 86400000}d > ${retentionDays}d ceiling)")
                        continue
                    }

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
                        val updates = FirebaseComm.buildLocationUpdates(targetUid, ul)
                        batchUpdates.putAll(updates)
                        syncedItemIds.add(id)
                    }
                }

                // Batch delete expired items in single SQLite transaction
                if (expiredIds.isNotEmpty()) {
                    db.deletePendingSyncItems(expiredIds)
                }

                // Dispatch single atomic batch multi-path update across all queued items
                if (batchUpdates.isNotEmpty() && isNetworkAvailable) {
                    val syncLatch = java.util.concurrent.CountDownLatch(1)
                    var atomicSuccess = false
                    FirebaseAtomicGateway.updateChildrenAtomic(FirebaseComm.database.reference, batchUpdates) { success, _ ->
                        atomicSuccess = success
                        syncLatch.countDown()
                    }
                    syncLatch.await(15, java.util.concurrent.TimeUnit.SECONDS)

                    if (atomicSuccess && syncedItemIds.isNotEmpty()) {
                        db.deletePendingSyncItems(syncedItemIds)
                        itemsProcessed += syncedItemIds.size
                    } else if (!atomicSuccess && poppedMemItems.isNotEmpty()) {
                        // 🛡️ Zero Data Loss Rollback: If atomic write fails/times out, re-enqueue in-memory pings
                        poppedMemItems.asReversed().forEach { syncQueue.offerFirst(it) }
                    }
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error draining sqlite pending sync queue: ${e.message}")
                if (poppedMemItems.isNotEmpty()) {
                    poppedMemItems.asReversed().forEach { syncQueue.offerFirst(it) }
                }
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

