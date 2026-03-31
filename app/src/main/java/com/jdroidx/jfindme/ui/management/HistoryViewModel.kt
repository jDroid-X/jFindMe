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
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * 🚀 QA Logic: Network Asset Scanner (SSOT Mirror)
     * Extracts all approved target nodes from the validated Roster Table.
     * Rule: Roster Table is the absolute Forensic SSOT (V36).
     */
    fun fetchTrackingTargets(context: Context) {
        val dbHelper = com.jdroidx.jfindme.data.local.CitizenDbHelper(context)
        val myUid = FirebaseAuth.getInstance().uid ?: return
        
        _isLoading.value = true
        
        // 1. Resolve Local Mirror (SSOT)
        val offlineNodes = dbHelper.getOfflineRoster()
        val targets = mutableListOf<TrackingTarget>()
        
        // 🚀 PILLAR 1: Target Identity (OWNER)
        val myDos = dbHelper.getLocalDossier("OWNER_001")
        val myName = myDos?.get("full_name") ?: "My Active Device"
        targets.add(TrackingTarget("$myName (PRIMARY)", myUid))
        
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
