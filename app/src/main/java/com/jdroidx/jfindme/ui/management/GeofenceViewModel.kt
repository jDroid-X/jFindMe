package com.jdroidx.jfindme.ui.management

import android.app.Application
import android.content.ContentValues
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
import com.jdroidx.jfindme.data.local.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * QA Architectural Solution: Single-Shot State Flow Wrapper
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
    val radius: Double,
    val assignedName: String,
    val address: String,
    val alertsEnabled: Boolean,
    val isZoneActive: Boolean
)

/**
 * Hardened MVVM Geofence ViewModel (V36 MASTER)
 * Synchronized with V36 Relational Database Engine (DatabaseHelper).
 */
class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val dbDelegate = DatabaseHelper(application)
    
    private val _activeZones = MutableLiveData<List<RenderedGeofence>>()
    val activeZones: LiveData<List<RenderedGeofence>> = _activeZones

    private val _activeTargetDevices = MutableLiveData<List<String>>()
    val activeTargetDevices: LiveData<List<String>> = _activeTargetDevices

    private val _draftCoordinateLocation = MutableLiveData<LatLng?>(null)
    val draftCoordinateLocation: LiveData<LatLng?> = _draftCoordinateLocation
    
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
        val prefs = getApplication<Application>().getSharedPreferences("system_registry", Application.MODE_PRIVATE)
        val localGroupId = prefs.getString("active_group", null)
        if (!localGroupId.isNullOrEmpty()) {
            currentGroupId = localGroupId
            initFirebaseCloudLink()
            return
        }
        val safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
        if (safeUid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(safeUid).child("active_group")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val realId = snapshot.getValue(String::class.java)
                        if (!realId.isNullOrEmpty()) {
                            currentGroupId = realId
                            prefs.edit().putString("active_group", realId).apply()
                            initFirebaseCloudLink()
                        } else { engageStandaloneSandbox(prefs) }
                    }
                    override fun onCancelled(error: DatabaseError) { engageStandaloneSandbox(prefs) }
                })
        } else { engageStandaloneSandbox(prefs) }
    }

    private fun engageStandaloneSandbox(prefs: android.content.SharedPreferences) {
        val safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "ORPHANED_NODE"
        currentGroupId = "P2P_MATRIX_$safeUid"
        prefs.edit().putString("active_group", currentGroupId).apply()
        initFirebaseCloudLink()
    }
    
    fun setDraftPin(latLng: LatLng?) { _draftCoordinateLocation.value = latLng }

    private fun initFirebaseCloudLink() {
        val activeId = currentGroupId ?: return
        loadLocalBackupVault(false)
        val ref = FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences")
        geofenceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<RenderedGeofence>()
                for (snap in snapshot.children) {
                    val name = snap.key ?: continue
                    val lat = snap.child("lat").value?.toString()?.toDoubleOrNull() ?: continue
                    val lng = snap.child("lng").value?.toString()?.toDoubleOrNull() ?: continue
                    val radius = snap.child("radius").value?.toString()?.toDoubleOrNull() ?: 100.0
                    val assignedName = snap.child("assignedName").getValue(String::class.java) ?: "ALL CONNECTED DEVICES"
                    val address = snap.child("address").getValue(String::class.java) ?: "Network Target Locked"
                    val alertsEnabled = snap.child("alertsEnabled").getValue(Boolean::class.java) ?: true
                    val isZoneActive = snap.child("isZoneActive").getValue(Boolean::class.java) ?: true
                    
                    list.add(RenderedGeofence(name, lat, lng, radius, assignedName, address, alertsEnabled, isZoneActive))
                    
                    // 🚀 V36 Forensic Commit: Sync to Master Schema
                    val v = ContentValues().apply {
                        put("zone_id", name); put("name", name); put("target", assignedName)
                        put("lat", lat); put("lng", lng); put("radius", radius)
                        put("sync_status", "CLOUD_SYNCED")
                    }
                    dbDelegate.saveSafeZone(v)
                }
                _activeZones.postValue(list)
            }
            override fun onCancelled(error: DatabaseError) { loadLocalBackupVault(true) }
        }
        ref.addValueEventListener(geofenceListener!!)
    }
    
    private fun loadLocalBackupVault(isEmergency: Boolean) {
        try {
            val localZones = dbDelegate.getOfflineSafeZones()
            if (localZones.isNotEmpty()) {
                val list = localZones.map { map ->
                    RenderedGeofence(
                        map["zone_id"] as String,
                        map["lat"] as Double,
                        map["lng"] as Double,
                        map["radius"] as Double,
                        map["target"] as String,
                        "Local SQL Memory (V36)",
                        true,
                        true
                    )
                }
                _activeZones.postValue(list)
                if (isEmergency) _uiToastEvent.postValue(SingleEvent("Cloud Offline: Forensic Vault Engaged!"))
            }
        } catch (e: Exception) {}
    }
    
    fun fetchTargetableDevices() {
        FirebaseDatabase.getInstance().getReference("locations")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val devices = mutableSetOf<String>(); devices.add("ALL CONNECTED DEVICES")
                    for (snap in snapshot.children) { snap.child("userName").getValue(String::class.java)?.let { devices.add(it) } }
                    _activeTargetDevices.value = devices.toList()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun submitCloudCoordinateSync(rawName: String, assignedName: String, lat: Double, lng: Double, radius: Double) {
        val name = if (assignedName == "ALL CONNECTED DEVICES") rawName else "$rawName ($assignedName)"
        _uiToastEvent.postValue(SingleEvent("Acquiring Hardware Sync..."))
        
        val geocoder = Geocoder(getApplication())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    val addr = if (addresses.isNotEmpty()) addresses[0].getAddressLine(0) else "Unknown Target"
                    finalizeCloudGeofencePush(name, assignedName, lat, lng, radius, addr)
                }
                override fun onError(errorMessage: String?) {
                    finalizeCloudGeofencePush(name, assignedName, lat, lng, radius, "Address Offline")
                }
            })
        } else {
            viewModelScope.launch {
                val addressString = withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        val res = geocoder.getFromLocation(lat, lng, 1)
                        if (!res.isNullOrEmpty()) res[0].getAddressLine(0) else "Unknown Target"
                    } catch (e: Exception) { "Address Offline" }
                }
                finalizeCloudGeofencePush(name, assignedName, lat, lng, radius, addressString)
            }
        }
    }

    private fun finalizeCloudGeofencePush(name: String, assignedName: String, lat: Double, lng: Double, radius: Double, addr: String) {
        val activeId = currentGroupId ?: return
        val data = mapOf("lat" to lat, "lng" to lng, "radius" to radius, "assignedName" to assignedName, "address" to addr, "isZoneActive" to true, "alertsEnabled" to true)
        FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences").child(name).setValue(data)
        _uiToastEvent.postValue(SingleEvent("Forensic Vault Synchronized"))
        _closeMapEvent.postValue(SingleEvent(true)) 
        setDraftPin(null)
    }
    
    fun deleteCloudSafeguardZone(name: String) {
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").child(name).removeValue() }
    }
    
    fun toggleBellAlertState(name: String, currentState: Boolean) {
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").child(name).child("alertsEnabled").setValue(!currentState) }
    }

    fun toggleZoneActiveState(name: String, currentState: Boolean) {
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").child(name).child("isZoneActive").setValue(!currentState) }
    }
    
    fun logManualGeofenceCheck(name: String, logString: String) {
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").child(name).child("history").push().setValue(logString) }
    }

    fun fetchGeofenceHistory(name: String) {
        val activeId = currentGroupId ?: return
        FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences").child(name).child("history")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val logs = mutableListOf<String>()
                    for (snap in snapshot.children) { snap.getValue(String::class.java)?.let { logs.add(it) } }
                    if (logs.isEmpty()) logs.add("No manual checks recorded yet.")
                    _historyDialogEvent.postValue(SingleEvent(logs.reversed().toTypedArray()))
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun executeOSGeocodingQuery(query: String) {
        if (query.isEmpty()) return
        val geocoder = Geocoder(getApplication())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    if (addresses.isNotEmpty()) {
                        val target = LatLng(addresses[0].latitude, addresses[0].longitude)
                        _cameraMoveEvent.postValue(SingleEvent(target))
                        _draftCoordinateLocation.postValue(target)
                    } else {
                        _uiToastEvent.postValue(SingleEvent("Target Not Found"))
                    }
                }
                override fun onError(errorMessage: String?) {
                    _uiToastEvent.postValue(SingleEvent("Geocoding Error"))
                }
            })
        } else {
            viewModelScope.launch {
                val target = withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        val res = geocoder.getFromLocationName(query, 1)
                        if (!res.isNullOrEmpty()) LatLng(res[0].latitude, res[0].longitude) else null
                    } catch (e: Exception) { null }
                }
                if (target != null) {
                    _cameraMoveEvent.value = SingleEvent(target)
                    _draftCoordinateLocation.value = target
                } else { _uiToastEvent.value = SingleEvent("Target Not Found") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").removeEventListener(geofenceListener!!) }
    }
}
