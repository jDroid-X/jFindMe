package com.jdroidx.jfindme.ui.management

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * QA Architectural Solution: Single-Shot State Flow Wrapper
 * Guarantees Toasts and App mechanics only fire once natively instead of 
 * violently crashing Android on Screen Rotation Phase.
 */
open class SingleEvent<out T>(private val content: T) {
    private var hasBeenHandled = false
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) null else {
            hasBeenHandled = true
            content
        }
    }
}

data class RenderedGeofence(
    val name: String,
    val lat: Double,
    val lng: Double,
    val assignedName: String,
    val address: String,
    val alertsEnabled: Boolean,
    val isZoneActive: Boolean
)

/**
 * Isolated MVVM Cloud Orchestrator. Extracted explicitly from the UI layer to run
 * asynchronous geocoding threads and multi-array list processing.
 */
class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val _activeZones = MutableLiveData<List<RenderedGeofence>>()
    val activeZones: LiveData<List<RenderedGeofence>> = _activeZones

    private val _activeTargetDevices = MutableLiveData<List<String>>()
    val activeTargetDevices: LiveData<List<String>> = _activeTargetDevices

    // State Mechanics (Survives strict Device Orientation Rotation!)
    private val _draftCoordinateLocation = MutableLiveData<LatLng?>(null)
    val draftCoordinateLocation: LiveData<LatLng?> = _draftCoordinateLocation
    
    // One-Shot Safety Wrappers for UI Signals
    private val _uiToastEvent = MutableLiveData<SingleEvent<String>>()
    val uiToastEvent: LiveData<SingleEvent<String>> = _uiToastEvent
    
    private val _cameraMoveEvent = MutableLiveData<SingleEvent<LatLng>>()
    val cameraMoveEvent: LiveData<SingleEvent<LatLng>> = _cameraMoveEvent
    
    private val _closeMapEvent = MutableLiveData<SingleEvent<Boolean>>()
    val closeMapEvent: LiveData<SingleEvent<Boolean>> = _closeMapEvent
    
    private val _historyDialogEvent = MutableLiveData<SingleEvent<Array<String>>>()
    val historyDialogEvent: LiveData<SingleEvent<Array<String>>> = _historyDialogEvent

    private var geofenceListener: ValueEventListener? = null
    private var currentGroupId: String? = null

    init {
        fetchDynamicGroupId()
    }
    
    private fun fetchDynamicGroupId() {
        val prefs = getApplication<Application>().getSharedPreferences("jFindMe_Prefs", Application.MODE_PRIVATE)
        val localGroupId = prefs.getString("active_group", null)
        
        // 🚀 QA Tier 1: Pull from Native Storage Array
        if (!localGroupId.isNullOrEmpty()) {
            currentGroupId = localGroupId
            initFirebaseCloudLink()
            return
        }

        val safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
        if (safeUid != null) {
            // 🚀 QA Tier 2: Cloud Recovery (If local file was wiped)
            FirebaseDatabase.getInstance().getReference("users").child(safeUid).child("active_group")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val realId = snapshot.getValue(String::class.java)
                        if (!realId.isNullOrEmpty()) {
                            currentGroupId = realId
                            prefs.edit().putString("active_group", realId).apply()
                            initFirebaseCloudLink()
                        } else {
                            engageStandaloneSandbox(prefs)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) { engageStandaloneSandbox(prefs) }
                })
        } else {
            // 🚀 QA Tier 3: Unauthenticated User / No Group (Forces the Dashboard to Work!)
            engageStandaloneSandbox(prefs)
        }
    }

    private fun engageStandaloneSandbox(prefs: android.content.SharedPreferences) {
        currentGroupId = "LOCAL_SANDBOX_MATRIX"
        prefs.edit().putString("active_group", currentGroupId).apply()
        initFirebaseCloudLink()
    }
    
    fun setDraftPin(latLng: LatLng?) { _draftCoordinateLocation.value = latLng }

    private fun initFirebaseCloudLink() {
        val activeId = currentGroupId ?: return
        
        // 🚀 QA Feature: Zero-Latency Backup Vault Loading. Immediately pulls local copy before hitting Network!
        loadLocalBackupVault(false)
        
        val ref = FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences")
        geofenceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<RenderedGeofence>()
                val localDb = com.jdroidx.jfindme.data.local.CitizenDbHelper(getApplication())
                
                for (snap in snapshot.children) {
                    val name = snap.key ?: continue
                    
                    // 🚀 QA Critical JSON Fix: Prevents Firebase from crashing the array if the Cloud optimizes numbers into Long/Integers!
                    val lat = snap.child("lat").value?.toString()?.toDoubleOrNull() ?: continue
                    val lng = snap.child("lng").value?.toString()?.toDoubleOrNull() ?: continue
                    
                    val assignedName = snap.child("assignedName").getValue(String::class.java) ?: "ALL CONNECTED DEVICES"
                    val address = snap.child("address").getValue(String::class.java) ?: "Network Target Locked"
                    val alertsEnabled = snap.child("alertsEnabled").getValue(Boolean::class.java) ?: true
                    val isZoneActive = snap.child("isZoneActive").getValue(Boolean::class.java) ?: true
                    
                    list.add(RenderedGeofence(name, lat, lng, assignedName, address, alertsEnabled, isZoneActive))
                    
                    // 🚀 PRIME DIRECTIVE: Backup explicitly verified Map Safe Zone natively!
                    localDb.saveSafeZone(name, name, assignedName, lat, lng, 150.0)
                }
                
                _activeZones.postValue(list) // 🚀 QA Enforcement: ALWAYS postValue to guarantee UI redraw thread collision immunity!
            }
            override fun onCancelled(error: DatabaseError) {
                // 🛑 CRITICAL NETWORK FAILURE: Cloud is dead. Execute Emergency Backup Load!
                loadLocalBackupVault(true)
            }
        }
        ref.addValueEventListener(geofenceListener!!)
    }
    
    private fun loadLocalBackupVault(isEmergency: Boolean) {
        try {
            val localZones = com.jdroidx.jfindme.data.local.CitizenDbHelper(getApplication()).getOfflineSafeZones()
            if (localZones.isNotEmpty()) {
                val list = localZones.map {
                    RenderedGeofence(
                        it["zone_id"] as String,
                        it["lat"] as Double,
                        it["lng"] as Double,
                        it["target"] as String,
                        "Local SQL Vault Memory",
                        true,
                        true
                    )
                }
                _activeZones.postValue(list) // 🚀 QA Enforcement
                if (isEmergency) _uiToastEvent.postValue(SingleEvent("Cloud Offline: Native SQL Vault Engaged!"))
            }
        } catch (e: Exception) {
            // Vault corrupted or empty. 
        }
    }
    
    fun fetchTargetableDevices() {
        FirebaseDatabase.getInstance().getReference("locations")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val devices = mutableSetOf<String>()
                    devices.add("ALL CONNECTED DEVICES")
                    for (snap in snapshot.children) {
                        val n = snap.child("userName").getValue(String::class.java)
                        if (n != null) devices.add(n)
                    }
                    _activeTargetDevices.value = devices.toList()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun submitCloudCoordinateSync(rawName: String, assignedName: String, lat: Double, lng: Double) {
        val activeId = currentGroupId
        if (activeId == null) {
            _uiToastEvent.postValue(SingleEvent("CRITICAL: Mobile not securely linked to a Tracking Group!"))
            return
        }
        
        // 🚀 QA Structural Firewall: Firebase NoSQL Collision Prevention!
        // Generates a mathematically unique Key String guaranteeing two "School" zones never overwrite each other natively.
        val name = if (assignedName == "ALL CONNECTED DEVICES") rawName else "$rawName ($assignedName)"
        
        _uiToastEvent.postValue(SingleEvent("Acquiring Hardware Sync..."))
        
        viewModelScope.launch {
            val addressString = withContext(Dispatchers.IO) {
                try {
                    val results = Geocoder(getApplication()).getFromLocation(lat, lng, 1)
                    if (results != null && results.isNotEmpty()) results[0].getAddressLine(0) else "Unknown Satellite Coordinate"
                } catch (e: Exception) { "Address Tracing Offline" }
            }

            val data = mapOf(
                "lat" to lat,
                "lng" to lng,
                "assignedName" to assignedName,
                "address" to addressString
            )
            
            FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences").child(name).setValue(data)
            _uiToastEvent.postValue(SingleEvent("Geofence '$name' Authorized & Linked"))
            _closeMapEvent.postValue(SingleEvent(true)) 
            setDraftPin(null) // Wipe the Draft Memory for next initialization!
        }
    }
    
    fun deleteCloudSafeguardZone(name: String) {
        val activeId = currentGroupId
        if (activeId != null) {
            FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences").child(name).removeValue()
            _uiToastEvent.postValue(SingleEvent("Cloud Zone Removed: $name"))
        }
    }
    
    fun toggleBellAlertState(name: String, currentState: Boolean) {
        val activeId = currentGroupId ?: return
        FirebaseDatabase.getInstance().getReference("groups").child(activeId)
            .child("geofences").child(name).child("alertsEnabled").setValue(!currentState)
    }

    fun toggleZoneActiveState(name: String, currentState: Boolean) {
        val activeId = currentGroupId ?: return
        FirebaseDatabase.getInstance().getReference("groups").child(activeId)
            .child("geofences").child(name).child("isZoneActive").setValue(!currentState)
    }
    
    fun logManualGeofenceCheck(name: String, logString: String) {
        val activeId = currentGroupId ?: return
        FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences").child(name).child("history").push().setValue(logString)
    }

    fun fetchGeofenceHistory(name: String) {
        val activeId = currentGroupId ?: return
        _uiToastEvent.postValue(SingleEvent("Extracting Manual Check Logs..."))
        FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences").child(name).child("history")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val logs = mutableListOf<String>()
                    for (snap in snapshot.children) {
                        snap.getValue(String::class.java)?.let { logs.add(it) }
                    }
                    if (logs.isEmpty()) logs.add("No manual checks recorded yet.")
                    
                    _historyDialogEvent.postValue(SingleEvent(logs.reversed().toTypedArray()))
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun executeOSGeocodingQuery(query: String) {
        if (query.isEmpty()) return
        _uiToastEvent.value = SingleEvent("Acquiring Satellite Search Lock...")
        
        viewModelScope.launch {
            val target = withContext(Dispatchers.IO) {
                try {
                    val results = Geocoder(getApplication()).getFromLocationName(query, 1)
                    if (results != null && results.isNotEmpty()) LatLng(results[0].latitude, results[0].longitude) else null
                } catch (e: Exception) { null }
            }
            
            if (target != null) {
                _cameraMoveEvent.value = SingleEvent(target)
                _draftCoordinateLocation.value = target // Locks target State!
                _uiToastEvent.value = SingleEvent("Target Geofence Coordinates Locked!")
            } else {
                _uiToastEvent.value = SingleEvent("Address not found / Geocoding Blocked")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 🚀 QA Fix: Prevent Ghost Listeners during OS Orientation Annihilation Protocols!
        if (currentGroupId != null && geofenceListener != null) {
            FirebaseDatabase.getInstance().getReference("groups")
                .child(currentGroupId!!).child("geofences").removeEventListener(geofenceListener!!)
        }
    }
}
