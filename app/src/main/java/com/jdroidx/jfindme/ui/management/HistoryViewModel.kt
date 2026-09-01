package com.jdroidx.jfindme.ui.management

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.repository.LocationRepository
import com.jdroidx.jfindme.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.location.Geocoder
import java.util.Locale

/**
 * ≡ƒÜÇ Enterprise History Replay ViewModel
 * Adheres to Waterfall MVVM standards by isolating Repository and Network
 * interactions from the HistoryActivity UI Controller.
 */
class HistoryViewModel(application: android.app.Application) : BaseViewModel(application) {

    private var repository: LocationRepository? = null

    private val _historyPoints = MutableLiveData<List<UserLocation>>()
    val historyPoints: LiveData<List<UserLocation>> get() = _historyPoints

    private val _trackingTargets = MutableLiveData<List<TrackingTarget>>()
    val trackingTargets: LiveData<List<TrackingTarget>> get() = _trackingTargets

    private val _historySessions = MutableLiveData<List<Map<String, Any>>>()
    val historySessions: LiveData<List<Map<String, Any>>> get() = _historySessions

    data class TrackingTarget(
        val name: String,
        val uid: String,
        val role: String = "PEER",
        val avatarId: String = "butterfly"
    )

    fun init(context: Context) {
        if (repository == null) {
            repository = LocationRepository(context)
        }
    }

    /**
     * ≡ƒÜÇ QA Logic: Secure History Extraction Matrix
     */
    fun loadHistoryData(uid: String, timeWindow: Long) {
        val repo = repository ?: return
        
        _isLoading.value = true
        val now = System.currentTimeMillis()
        val targetPast = now - timeWindow

        viewModelScope.launch {
            repo.fetchHistory(uid, targetPast, now) { points ->
                _historyPoints.postValue(points)
                // ≡ƒÜÇ QA Fix: After Sync (Step 3), trigger a UI list refresh to show the new pings!
                loadHistorySessions(repo.context, uid, timeWindow, points)
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * 🛰️ Location-Wise Spatial Clustering: Groups GPS points into distinct geographic stays / locations.
     */
    fun buildLocationClusters(points: List<UserLocation>): List<Map<String, Any>> {
        if (points.isEmpty()) return emptyList()
        
        // Group points by spatial grid (approx 100m - rounding to 3 decimal places)
        val clusters = mutableListOf<Map<String, Any>>()
        val grouped = points.groupBy { p ->
            val latRound = (p.latitude * 1000.0).toLong()
            val lngRound = (p.longitude * 1000.0).toLong()
            "$latRound,$lngRound"
        }

        for ((_, group) in grouped) {
            val count = group.size.toLong()
            val sortedGroup = group.sortedBy { it.getLongTimestamp() }
            val first = sortedGroup.first()
            val last = sortedGroup.last()
            
            val avgLat = group.map { it.latitude }.average()
            val avgLng = group.map { it.longitude }.average()
            val avgSpeed = group.map { it.speed.toDouble() }.average().toFloat()
            val maxBattery = group.map { it.batteryLevel }.maxOrNull() ?: -1
            
            var tsStart = first.getLongTimestamp()
            var tsEnd = last.getLongTimestamp()
            if (tsEnd < tsStart) tsEnd = tsStart

            val map = mutableMapOf<String, Any>(
                "lat" to avgLat,
                "lng" to avgLng,
                "ts_start" to tsStart,
                "ts_end" to tsEnd,
                "from_datetime" to tsStart,
                "to_datetime" to tsEnd,
                "ping_count" to count,
                "speed" to avgSpeed,
                "battery" to maxBattery,
                "address" to "Recorded Location Fix"
            )
            clusters.add(map)
        }
        
        // Sort clusters by latest departure time descending
        return clusters.sortedByDescending { (it["ts_end"] as? Long) ?: 0L }
    }

    /**
     * ≡ƒÜÇ QA Logic: Synchronized Data Scavenge
     * Fetches grouped location session logs from the local Forensic DB with a Time Window filter.
     */
    @JvmOverloads
    fun loadHistorySessions(context: Context, uid: String, timeWindow: Long, fallbackPoints: List<UserLocation> = emptyList()) {
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val geocoder = Geocoder(context, Locale.getDefault())
        val cutoff = System.currentTimeMillis() - timeWindow

        viewModelScope.launch(Dispatchers.IO) {
            var sessions = dbHelper.getHistoryLogsFiltered(uid, cutoff)
            if (sessions.isEmpty() && fallbackPoints.isNotEmpty()) {
                sessions = buildLocationClusters(fallbackPoints)
            }
            _historySessions.postValue(sessions)
            
            // ≡ƒÜÇ QA Fix: Forensic Geocoding Array (Late-Binding Resolution)
            val enriched = sessions.map { row ->
                val addr = row["address"] as? String ?: "Recorded Location Fix"
                if (addr == "Recorded Location Fix" || addr == "Locating..." || addr.isBlank() || addr == "Tracing...") {
                    val lat = (row["lat"] as? Number)?.toDouble() ?: 0.0
                    val lng = (row["lng"] as? Number)?.toDouble() ?: 0.0
                    val logId = row["log_id"] as? String ?: ""
                    
                    if (lat != 0.0 || lng != 0.0) {
                        try {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(lat, lng, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val humanAddress = addresses[0].getAddressLine(0) ?: "Recorded Location Fix"
                                if (logId.isNotEmpty()) {
                                    dbHelper.updateHistoryAddress(logId, humanAddress)
                                }
                                val copy = row.toMutableMap()
                                copy["address"] = humanAddress
                                copy["geo_address"] = humanAddress
                                return@map copy
                            }
                        } catch (e: Exception) {
                            // Geocoding service may be temporarily unavailable
                        }
                    }
                }
                row
            }
            _historySessions.postValue(enriched)
        }
    }

    /**
     * ≡ƒÜÇ QA Logic: Network Asset Scanner (SSOT Mirror)
     * Extracts all approved target nodes from the validated Roster Table.
     * Rule: Roster Table is the absolute Forensic SSOT (V36).
     */
    fun fetchTrackingTargets(context: Context) {
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val myUid = FirebaseAuth.getInstance().uid ?: ""
        
        _isLoading.value = true
        
        // 1. Resolve Local Mirror (SSOT)
        val offlineNodes = dbHelper.getOfflineRoster()
        val targets = mutableListOf<TrackingTarget>()
        
        // Include Self-Target for personal travel history inspection
        val myNick = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(context)
        val selfLabel = if (myNick.isNotBlank()) "👑 $myNick (My Device)" else "👑 My Device (Host)"
        val selfAvatarId = "avatar_custom_" + com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(
            context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getInt("equipped_avatar_id", 1)
        )
        targets.add(TrackingTarget(selfLabel, selfAgentId, "Host", selfAvatarId))
        
        // ≡ƒÜÇ PILLAR 2: Validated P2P Matrix
        offlineNodes.forEach { row ->
            val uid = row["uid_1"] as? String ?: row["uid"] as? String ?: ""
            val ssotName = dbHelper.getDeviceCentricName(uid)
            val name = if (ssotName.isNotBlank() && !ssotName.startsWith("Agent_", true)) ssotName else ((row["fullName"] as? String) ?: "Target ($uid)")
            val role = row["role_type"] as? String ?: "PEER"
            val avId = dbHelper.getDeviceCentricAvatar(uid)
            
            if (uid.isNotEmpty() && uid != selfAgentId && uid != myUid && !uid.contains("OWNER")) {
                targets.add(TrackingTarget("$name ($role)", uid, role, avId))
            }
        }
        
        if (targets.isNotEmpty()) {
            _trackingTargets.postValue(targets)
            _isLoading.postValue(false)
        } else {
            // 🛡️ EMERGENCY FALLBACK: Local table is unpopulated, trigger forensic network scavenge
            com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.children.forEach { child ->
                            val childUid = child.key ?: return@forEach
                            if (childUid != myUid) {
                                val liveSnap = child.child("device").child("live")
                                val profSnap = child.child("profile")
                                val targetName = profSnap.child("name").getValue(String::class.java)
                                    ?: liveSnap.child("userName").getValue(String::class.java)
                                    ?: "Target ($childUid)"
                                val avatarId = liveSnap.child("avatar_id").getValue(String::class.java) ?: "butterfly"
                                
                                // If found remotely, save back to SSOT (Persistence Loop)
                                dbHelper.saveRosterNode(childUid, targetName, "MIRROR_T", "Handshake Restored", isAdmin = false, isFriend = true,
                                    ownerName = "P2P Mirror Recovery", connectedDeviceId = childUid, qrCode = avatarId)
                                targets.add(TrackingTarget("$targetName (P2P MIRROR)", childUid, "Peer", avatarId))
                            }
                        }
                        _trackingTargets.postValue(targets)
                        _isLoading.postValue(false)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        _trackingTargets.postValue(targets)
                        _isLoading.postValue(false)
                    }
                })
        }
    }

    /**
     * 🔄 Cloud Archive Recovery: Re-hydrates historical traces from Firebase Partitioned Archives
     */
    fun rehydrateHistoryFromCloud(context: Context, targetUid: String, onComplete: ((Int) -> Unit)? = null) {
        val myAuthUid = FirebaseAuth.getInstance().uid ?: return
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        _isLoading.value = true

        com.jdroidx.jfindme.data.remote.FirebaseComm.rehydrateCloudLocationHistory(myAuthUid, targetUid) { points ->
            if (points.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (pt in points) {
                        dbHelper.saveHistoryPoint(
                            targetUid, "HW_$targetUid",
                            pt["lat"] as Double, pt["lng"] as Double,
                            pt["speed"] as Float, pt["battery"] as Int,
                            false, pt["address"] as? String, (pt["timestamp"] as? String) ?: com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
                        )
                    }
                    loadHistorySessions(context, targetUid, 86400000L * 7) // 7 days window
                    _isLoading.postValue(false)
                    onComplete?.invoke(points.size)
                }
            } else {
                _isLoading.postValue(false)
                onComplete?.invoke(0)
            }
        }
    }
}

