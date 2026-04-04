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
 * 🚀 Enterprise History Replay ViewModel
 * Adheres to Waterfall MVVM standards by isolating Repository and Network
 * interactions from the HistoryActivity UI Controller.
 */
class HistoryViewModel : BaseViewModel() {

    private var repository: LocationRepository? = null

    private val _historyPoints = MutableLiveData<List<UserLocation>>()
    val historyPoints: LiveData<List<UserLocation>> get() = _historyPoints

    private val _trackingTargets = MutableLiveData<List<TrackingTarget>>()
    val trackingTargets: LiveData<List<TrackingTarget>> get() = _trackingTargets

    private val _historySessions = MutableLiveData<List<Map<String, Any>>>()
    val historySessions: LiveData<List<Map<String, Any>>> get() = _historySessions

    data class TrackingTarget(val name: String, val uid: String)

    fun init(context: Context) {
        if (repository == null) {
            repository = LocationRepository(context)
        }
    }

    /**
     * 🚀 QA Logic: Secure History Extraction Matrix
     */
    fun loadHistoryData(uid: String, timeWindow: Long) {
        val repo = repository ?: return
        
        _isLoading.value = true
        val now = System.currentTimeMillis()
        val targetPast = now - timeWindow

        viewModelScope.launch {
            repo.fetchHistory(uid, targetPast, now) { points ->
                _historyPoints.postValue(points)
                // 🚀 QA Fix: After Sync (Step 3), trigger a UI list refresh to show the new pings!
                loadHistorySessions(repo.context, uid, timeWindow)
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * 🚀 QA Logic: Synchronized Data Scavenge
     * Fetches grouped session logs from the local Forensic DB with a Time Window filter.
     */
    fun loadHistorySessions(context: Context, uid: String, timeWindow: Long) {
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper(context)
        val geocoder = Geocoder(context, Locale.getDefault())
        val cutoff = System.currentTimeMillis() - timeWindow

        viewModelScope.launch(Dispatchers.IO) {
            // Updated call to include forensic time cutoff (V40)
            val sessions = dbHelper.getHistoryLogsFiltered(uid, cutoff)
            _historySessions.postValue(sessions)
            
            // 🚀 QA Fix: Forensic Geocoding Array (Late-Binding Resolution)
            sessions.forEach { row ->
                val addr = row["address"] as? String ?: "Tracing..."
                if (addr == "Tracing..." || addr.isBlank()) {
                    val lat = row["lat"] as? Double ?: 0.0
                    val lng = row["lng"] as? Double ?: 0.0
                    val logId = row["log_id"] as? String ?: ""
                    
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val humanAddress = addresses[0].getAddressLine(0) ?: "Unknown Terrain"
                            dbHelper.updateHistoryAddress(logId, humanAddress)
                            
                            // Re-fetch to update the UI with the newly resolved forensic metadata
                            val refreshed = dbHelper.getHistoryLogsFiltered(uid, cutoff)
                            _historySessions.postValue(refreshed)
                        }
                    } catch (e: Exception) {
                        // Geocoding service may be temporarily unavailable
                    }
                }
            }
        }
    }

    /**
     * 🚀 QA Logic: Network Asset Scanner (SSOT Mirror)
     * Extracts all approved target nodes from the validated Roster Table.
     * Rule: Roster Table is the absolute Forensic SSOT (V36).
     */
    fun fetchTrackingTargets(context: Context) {
        val dbHelper = com.jdroidx.jfindme.data.local.DatabaseHelper(context)
        val myUid = FirebaseAuth.getInstance().uid ?: return
        
        _isLoading.value = true
        
        // 1. Resolve Local Mirror (SSOT)
        val offlineNodes = dbHelper.getOfflineRoster()
        val targets = mutableListOf<TrackingTarget>()
        
        // 🚀 QA Rule: Discard Self-Target to save bandwidth and battery.
        // Primary Owner telemetry is currently redundant for history replay.
        // val myDos = dbHelper.getLocalDossier("OWNER_001")
        // val myName = myDos?.get("full_name") ?: "My Active Device"
        // targets.add(TrackingTarget("$myName (PRIMARY)", myUid))
        
        // 🚀 PILLAR 2: Validated P2P Matrix
        offlineNodes.forEach { row ->
            val uid = row["uid"] as? String ?: ""
            val name = row["display_name"] as? String ?: "Target ($uid)"
            val role = row["role_title"] as? String ?: "PEER"
            
            if (uid.isNotEmpty() && uid != myUid && !uid.contains("OWNER")) {
                targets.add(TrackingTarget("$name ($role)", uid))
            }
        }
        
        if (targets.size > 1) {
            // Already synced locally, bypass network scan to maintain SSOT integrity
            _trackingTargets.postValue(targets)
            _isLoading.postValue(false)
        } else {
            // 🛡️ EMERGENCY FALLBACK: Local table is unpopulated, trigger forensic network scavenge
            FirebaseDatabase.getInstance().getReference("locations")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.children.forEach { child ->
                            val loc = child.getValue(UserLocation::class.java)
                            if (loc != null && loc.userId != myUid) {
                                // If found remotely, save back to SSOT (Persistence Loop)
                                dbHelper.saveRosterNode(loc.userId, loc.userName, "MIRROR_T", "Handshake Restored", isAdmin = false, isFriend = true,
                                    ownerName = "P2P Mirror Recovery", ownerDeviceId = myUid, connectedDeviceId = loc.userId, qrCode = "RECOVERY")
                                targets.add(TrackingTarget("${loc.userName} (P2P MIRROR)", loc.userId))
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
}

