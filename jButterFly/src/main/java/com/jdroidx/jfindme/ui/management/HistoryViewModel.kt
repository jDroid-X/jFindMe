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
        val dbHelper = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
        val cutoff = System.currentTimeMillis() - timeWindow

        viewModelScope.launch(Dispatchers.IO) {
            var sessions = dbHelper.getHistoryLogsFiltered(uid, cutoff)
            if (sessions.isEmpty() && fallbackPoints.isNotEmpty()) {
                sessions = buildLocationClusters(fallbackPoints)
            }
            _historySessions.postValue(sessions)
            
            // OOPS MVC: 2-Tier Caching via ReverseGeocodeEngine SSOT (Late-Binding Resolution)
            // [Variables] humanAddress, latch, result, copy | [Dependencies] ReverseGeocodeEngine | [Integration Points] HistoryViewModel ⟷ ReverseGeocodeEngine
            val enriched = sessions.map { row ->
                val addr = row["address"] as? String ?: "Recorded Location Fix"
                if (addr == "Recorded Location Fix" || addr == "Locating..." || addr.isBlank() || addr == "Tracing...") {
                    val lat = (row["lat"] as? Number)?.toDouble() ?: 0.0
                    val lng = (row["lng"] as? Number)?.toDouble() ?: 0.0
                    val logId = row["log_id"] as? String ?: ""
                    
                    if (lat != 0.0 || lng != 0.0) {
                        try {
                            var humanAddress = "Recorded Location Fix"
                            val latch = java.util.concurrent.CountDownLatch(1)
                            com.jdroidx.jfindme.engine.map.ReverseGeocodeEngine.resolveAddress(
                                context, lat, lng, object : com.jdroidx.jfindme.engine.map.ReverseGeocodeEngine.AddressCallback {
                                    override fun onAddressResolved(result: com.jdroidx.jfindme.engine.map.ReverseGeocodeEngine.AddressResult) {
                                        if (result.isSuccess && result.formattedAddress.isNotBlank()) {
                                            humanAddress = result.formattedAddress
                                        }
                                        latch.countDown()
                                    }
                                }
                            )
                            latch.await(1200, java.util.concurrent.TimeUnit.MILLISECONDS)
                            if (humanAddress != "Recorded Location Fix") {
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
     * 🛡️ SSOT: OOPS-Based MVC Target Resolver
     * Extracts only authorized, role-scoped connected devices for the host device.
     * Prevents global cloud pollution, ghost HW_ aliases, and unlinked nodes.
     */
    fun fetchTrackingTargets(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            // [Variables] dbHelper, selfAgentId, myUid, myNick, selfLabel, selfAvatarId, primeTarget, connectedIds, targets, cleanId, ssotName, name, rawRole, role, avId, sortedTargets | [Constants] None | [Dependencies] DeviceRepository, DeviceIdentityHelper, RegistryManager, AvatarLoader, FirebaseComm | [Integration Points] HistoryActivity, SecurityDialogManager
            val dbHelper = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
            val selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            val myUid = FirebaseAuth.getInstance().uid ?: ""
            val primeTarget = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(
                com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(context)
            )
            
            _isLoading.postValue(true)
            
            val targets = mutableListOf<TrackingTarget>()
            
            // 1. Host Device (Self-Target)
            val myNick = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(context)
            val selfLabel = if (myNick.isNotBlank()) myNick else "My Device (Host)"
            val selfAvatarId = "avatar_custom_" + com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(
                context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getInt("equipped_avatar_id", 1)
            )
            targets.add(TrackingTarget(selfLabel, selfAgentId, "Host", selfAvatarId))
            
            // [Variables] candidateIds, primeTarget, cleanId, ssotName, name, rawRole, role, avId | [Constants] None | [Dependencies] DeviceRepository, RegistryManager, DeviceIdentityHelper | [Integration Points] HistoryActivity, TrackingTarget
            // 2. Verified Connected Peers (SSOT from Scoped Connected Devices, strictly excluding fleet wide catch-all pollution)
            val activeGroup = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(context)
            val candidateIds = mutableSetOf<String>()
            candidateIds.addAll(dbHelper.getConnectedDeviceIds(selfAgentId))
            if (primeTarget.isNotBlank()) {
                candidateIds.add(primeTarget)
            }
            
            for (rawId in candidateIds) {
                if (rawId.isBlank() || rawId.startsWith("HW_")) continue
                val cleanId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(rawId)
                if (cleanId.isBlank() || cleanId == selfAgentId || cleanId == myUid) continue
                if (cleanId.contains("OWNER") || cleanId.startsWith("UNKNOWN", ignoreCase = true)) continue
                if (com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanId)) continue
                if (dbHelper.isDeviceDeleted(cleanId, activeGroup)) continue
                if (!com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(cleanId)) continue
                
                val ssotName = dbHelper.getDeviceCentricName(cleanId)
                val name = if (ssotName.isNotBlank() && !ssotName.startsWith("Agent_", true)) {
                    ssotName
                } else {
                    val dossier = dbHelper.getLocalDossier(cleanId)
                    dossier["name"]?.takeIf { it.isNotBlank() && !it.startsWith("Agent_", true) } ?: "Device ($cleanId)"
                }
                
                val rawRole = dbHelper.getRoleForDevice(cleanId)
                val role = if (rawRole.isNotBlank() && !rawRole.equals("None", ignoreCase = true)) {
                    rawRole
                } else {
                    "Peer"
                }
                val avId = dbHelper.getDeviceCentricAvatar(cleanId)
                
                targets.add(TrackingTarget(name, cleanId, role, avId))
            }
            
            // 3. Hierarchical Role-Based Sorting: Host (0) -> Child/Ward (1) -> Guardian/Parent (2) -> Family (3) -> Peer (4)
            val sortedTargets = targets.sortedWith { a, b ->
                fun rank(t: TrackingTarget): Int {
                    if (t.role.equals("Host", ignoreCase = true) || t.uid == selfAgentId) return 0
                    val r = t.role.uppercase()
                    if (t.uid == primeTarget || r.contains("CHILD") || r.contains("WARD") || r.contains("SON") || r.contains("DAUGHTER")) return 1
                    if (r.contains("PARENT") || r.contains("GUARDIAN") || r.contains("MOTHER") || r.contains("FATHER")) return 2
                    if (r.contains("FAMILY") || r.contains("SIBLING") || r.contains("SISTER") || r.contains("BROTHER") || r.contains("SPOUSE")) return 3
                    return 4
                }
                val rankCompare = rank(a).compareTo(rank(b))
                if (rankCompare != 0) rankCompare else a.name.compareTo(b.name, ignoreCase = true)
            }
            
            _trackingTargets.postValue(sortedTargets)
            _isLoading.postValue(false)
        }
    }

    /**
     * 🔄 Cloud Archive Recovery: Re-hydrates historical traces from Firebase Partitioned Archives
     */
    fun rehydrateHistoryFromCloud(context: Context, targetUid: String, onComplete: ((Int) -> Unit)? = null) {
        val myAuthUid = FirebaseAuth.getInstance().uid
        if (myAuthUid.isNullOrEmpty() || !com.jdroidx.jfindme.data.remote.FirebaseComm.isAuthenticated()) {
            _isLoading.postValue(false)
            onComplete?.invoke(0)
            return
        }
        val dbHelper = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
        _isLoading.value = true

        com.jdroidx.jfindme.data.remote.FirebaseComm.rehydrateCloudLocationHistory(myAuthUid, targetUid) { points ->
            if (points.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (pt in points) {
                        val lat = (pt["lat"] as? Number)?.toDouble() ?: 0.0
                        val lng = (pt["lng"] as? Number)?.toDouble() ?: 0.0
                        val spd = (pt["speed"] as? Number)?.toFloat() ?: 0f
                        val bat = (pt["battery"] as? Number)?.toInt() ?: -1
                        val acc = (pt["accuracy"] as? Number)?.toFloat() ?: 0f
                        val brg = (pt["bearing"] as? Number)?.toFloat() ?: 0f
                        val alt = (pt["altitude"] as? Number)?.toDouble() ?: 0.0
                        val motion = (pt["motion_state"] as? String) ?: "UNKNOWN"
                        val ts = (pt["timestamp"] as? String) ?: com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
                        val addr = pt["address"] as? String

                        // [Variables] cleanTargetId: String, canonicalHwUid: String | [Constants] None | [Dependencies] FirebaseComm.sanitizeAgentId | [Integration Points] SQLite HistoryPointTable SSOT
                        val cleanTargetId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(targetUid)
                        val canonicalHwUid = "HW_$cleanTargetId"
                        dbHelper.saveHistoryPoint(
                            cleanTargetId, canonicalHwUid,
                            lat, lng,
                            spd, bat,
                            false, addr, ts,
                            acc, brg, alt, motion
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

