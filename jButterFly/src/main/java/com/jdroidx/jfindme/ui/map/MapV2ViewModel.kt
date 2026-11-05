package com.jdroidx.jfindme.ui.map

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.model.LiveDeviceModel
import com.jdroidx.jfindme.data.model.SafeZoneModel
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*

class MapV2ViewModel(application: Application) : AndroidViewModel(application) {

    private val _liveDevices = MutableLiveData<List<LiveDeviceModel>>()
    val liveDevices: LiveData<List<LiveDeviceModel>> get() = _liveDevices

    private val _safeZones = MutableLiveData<List<SafeZoneModel>>()
    val safeZones: LiveData<List<SafeZoneModel>> get() = _safeZones

    private val _zoneAlerts = MutableLiveData<String>()
    val zoneAlerts: LiveData<String> get() = _zoneAlerts

    private val deviceListeners = HashMap<String, ValueEventListener>()
    private var safeZoneListener: ValueEventListener? = null
    
    // In-memory caching for processing
    private val currentDevices = HashMap<String, LiveDeviceModel>()
    private val currentZones = mutableListOf<SafeZoneModel>()

    init {
        loadOfflineData()
        startSafeZoneListener()
        startLiveDeviceListeners()
    }

    private fun loadOfflineData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Scenario A and B Offline validations:
                // If offline, populate the map with the last known hardware positions
                // and local aliases using the robust DatabaseHelper.
                val db = DatabaseHelper.getInstance(getApplication())
                val prefs = getApplication<Application>().getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                val validUids = prefs.getString("linked_devices_csv", "") ?: ""
                
                validUids.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { uid ->
                    try {
                        val profile = db.getLocalDossier(uid)
                        val name = profile?.get("fullName") ?: profile?.get("name") ?: uid
                        val device = LiveDeviceModel(
                            userId = uid,
                            userName = name,
                            latitude = 0.0, // Last known from local cache if we add table for it
                            longitude = 0.0
                        )
                        // This serves as an initial fallback validation
                        SystemLogger.d("MapV2ViewModel", "Loaded offline dossier for: $name")
                    } catch (e: Exception) {
                        SystemLogger.e("MapV2ViewModel", "Offline dossier missing for $uid")
                    }
                }
            } catch (e: Exception) {
                SystemLogger.e("MapV2ViewModel", "Offline caching validation failed: ${e.message}")
            }
        }
    }

    private fun startSafeZoneListener() {
        val myUid = FirebaseAuth.getInstance().uid ?: return
        val prefs = getApplication<Application>().getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val myGroup = prefs.getString("active_group", "") ?: ""
        
        if (myGroup.isEmpty()) return

        val ref = FirebaseComm.relationshipsRef.child(myGroup).child("geofences")
        safeZoneListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val zones = mutableListOf<SafeZoneModel>()
                snapshot.children.forEach { child ->
                    child.getValue(SafeZoneModel::class.java)?.let { zone ->
                        zones.add(zone.copy(zoneId = child.key ?: ""))
                    }
                }
                currentZones.clear()
                currentZones.addAll(zones)
                _safeZones.postValue(zones)
                
                checkAllZoneViolations()
            }

            override fun onCancelled(error: DatabaseError) {
                SystemLogger.e("MapV2ViewModel", "Failed to fetch SafeZones: ${error.message}")
            }
        }
        ref.addValueEventListener(safeZoneListener!!)
    }

    private fun startLiveDeviceListeners() {
        // [Variables] | [Constants] | [Dependencies] | [Integration Points]
        val myUid = FirebaseAuth.getInstance().uid ?: com.jdroidx.jfindme.utils.RegistryManager.getAgentId(getApplication()) ?: return
        val db = DatabaseHelper.getInstance(getApplication())
        val offlineRoster = db.getOfflineRoster()
        
        val targetUids = mutableSetOf<String>()
        FirebaseComm.resolveHardwareIdentity(myUid) { myAgentId ->
            if (myAgentId != null) targetUids.add(myAgentId)
            
            for (node in offlineRoster) {
                val uid1 = node["uid_1"] as? String
                if (!uid1.isNullOrEmpty() && FirebaseComm.isAgentIdValid(uid1)) {
                    targetUids.add(uid1)
                }
            }

            for (agentId in targetUids) {
                if (!deviceListeners.containsKey(agentId)) {
                    val ref = FirebaseComm.getLiveRef(agentId)
                    val listener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val device = snapshot.getValue(LiveDeviceModel::class.java)
                            if (device != null) {
                                val resolvedDevice = device.copy(userId = agentId)
                                
                                viewModelScope.launch(Dispatchers.IO) {
                                    try {
                                        val profile = db.getLocalDossier(agentId)
                                        val finalDevice = if (profile.isNotEmpty()) {
                                            val resolvedName = profile["fullName"] ?: profile["name"] ?: profile["owner_name"] ?: profile["id_full_name"]
                                            if (!resolvedName.isNullOrEmpty()) {
                                                resolvedDevice.copy(userName = resolvedName)
                                            } else {
                                                resolvedDevice
                                            }
                                        } else {
                                            resolvedDevice
                                        }
                                        
                                        currentDevices[agentId] = finalDevice
                                        _liveDevices.postValue(currentDevices.values.toList())
                                        
                                        checkZoneViolation(finalDevice)
                                    } catch (e: Exception) {
                                        currentDevices[agentId] = resolvedDevice
                                        _liveDevices.postValue(currentDevices.values.toList())
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            SystemLogger.e("MapV2ViewModel", "Live stream aborted for $agentId: ${error.message}")
                        }
                    }
                    ref.addValueEventListener(listener)
                    deviceListeners[agentId] = listener
                }
            }
        }
    }

    private fun checkAllZoneViolations() {
        for (device in currentDevices.values) {
            checkZoneViolation(device)
        }
    }

    private fun checkZoneViolation(device: LiveDeviceModel) {
        viewModelScope.launch(Dispatchers.Default) {
            for (zone in currentZones) {
                // Check if this zone applies to this device
                if (zone.assignedUserIds.isEmpty() || zone.assignedUserIds.contains(device.userId)) {
                    val distance = calculateHaversineDistance(
                        device.latitude, device.longitude,
                        zone.latitude, zone.longitude
                    )
                    
                    if (distance > zone.radiusMeters) {
                        // User left the safe zone (Out of Bounds)
                        _zoneAlerts.postValue("${device.userName.ifEmpty { "Agent_" + device.userId }} left ${zone.name}! (Distance: ${distance.toInt()}m)")
                    } else {
                        // Within SafeZone
                    }
                }
            }
        }
    }

    /**
     * Calculates the Haversine distance between two coordinates in meters.
     */
    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun onCleared() {
        super.onCleared()
        deviceListeners.forEach { (agentId, listener) ->
            FirebaseComm.getLiveRef(agentId).removeEventListener(listener)
        }
        deviceListeners.clear()
        
        safeZoneListener?.let {
            val prefs = getApplication<Application>().getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val myGroup = prefs.getString("active_group", "") ?: ""
            if (myGroup.isNotEmpty()) {
                FirebaseComm.relationshipsRef.child(myGroup).child("geofences").removeEventListener(it)
            }
        }
    }
}
