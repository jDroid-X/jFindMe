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
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.utils.SystemLogger
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
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radius: Double,
    val assignedName: String,
    val address: String,
    val alertsEnabled: Boolean,
    val isZoneActive: Boolean,
    val startTime: Int = 0,
    val endTime: Int = 2359,
    val activeDays: String = "1,2,3,4,5,6,0" // 0=Sun, 1=Mon...
)

/**
 * Hardened MVVM Geofence ViewModel (V36 MASTER)
 * Synchronized with V36 Relational Database Engine (DatabaseHelper).
 */
class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val dbDelegate = DatabaseHelper.getInstance(application)
    
    private val _activeZones = MutableLiveData<List<RenderedGeofence>>()
    val activeZones: LiveData<List<RenderedGeofence>> = _activeZones

    private val _activeTargetDevices = MutableLiveData<List<String>>()
    val activeTargetDevices: LiveData<List<String>> = _activeTargetDevices

    private val _profileAddress = MutableLiveData<String>()
    val profileAddress: LiveData<String> = _profileAddress

    private val _rosterMapping = MutableLiveData<Map<String, String>>()
    val rosterMapping: LiveData<Map<String, String>> = _rosterMapping

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

    private val deviceUidMap = mutableMapOf<String, String>()

    private var geofenceListener: ValueEventListener? = null
    private var currentGroupId: String? = null

    init {
        fetchDynamicGroupId()
        fetchRosterMetadata()
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
        val safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return
        
        loadLocalBackupVault(false)
        performGlobalMatrixSync(safeUid, activeId)
        
        val ref = FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences")
        geofenceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<RenderedGeofence>()
                for (snap in snapshot.children) {
                    val id = snap.key ?: continue
                    val name = snap.child("name").getValue(String::class.java) ?: id
                    val lat = snap.child("lat").value?.toString()?.toDoubleOrNull() ?: continue
                    val lng = snap.child("lng").value?.toString()?.toDoubleOrNull() ?: continue
                    val radius = snap.child("radius").value?.toString()?.toDoubleOrNull() ?: 100.0
                    val assignedName = snap.child("assignedName").getValue(String::class.java) ?: "ALL CONNECTED DEVICES"
                    val address = snap.child("address").getValue(String::class.java) ?: "Network Target Locked"
                    val alertsEnabled = snap.child("alertsEnabled").getValue(Boolean::class.java) ?: true
                    val isZoneActive = snap.child("isZoneActive").getValue(Boolean::class.java) ?: true
                    val startTime = snap.child("startTime").getValue(Int::class.java) ?: 0
                    val endTime = snap.child("endTime").getValue(Int::class.java) ?: 2359
                    val activeDays = snap.child("activeDays").getValue(String::class.java) ?: "1,2,3,4,5,6,0"
                    
                    list.add(RenderedGeofence(id, name, lat, lng, radius, assignedName, address, alertsEnabled, isZoneActive, startTime, endTime, activeDays))
                }
                
                // 🚀 V36 Optimized Sync: Single Batch Commit (Forensic Efficiency)
                viewModelScope.launch(Dispatchers.IO) {
                    list.forEach { zone ->
                        val v = ContentValues().apply {
                            put("zone_id", zone.name); put("name", zone.name); put("target", zone.assignedName)
                            put("lat", zone.lat); put("lng", zone.lng); put("radius", zone.radius)
                            put("sync_status", "CLOUD_SYNCED")
                            put("flag_a", if(zone.alertsEnabled) 1 else 0) // 🚀 Alert Toggle Sync
                            put("flag_b", if(zone.isZoneActive) 1 else 0) // 🚀 Active State Sync
                        }
                        dbDelegate.saveSafeZone(v)
                    }
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
                        map["name"] as? String ?: "Local Node",
                        map["lat"] as? Double ?: 0.0,
                        map["lng"] as? Double ?: 0.0,
                        map["radius"] as? Double ?: 100.0,
                        map["target"] as? String ?: "ALL CONNECTED DEVICES",
                        "Local SQL Memory (V46)",
                        (map["flag_a"] as? Int ?: 1) == 1,
                        (map["flag_b"] as? Int ?: 1) == 1,
                        0, // Default Start
                        2359, // Default End
                        "1,2,3,4,5,6,0" // Default Days
                    )
                }
                _activeZones.postValue(list)
                if (isEmergency) _uiToastEvent.postValue(SingleEvent("Cloud Offline: Forensic Vault Engaged!"))
            }
        } catch (e: Exception) {}
    }
    
    private fun performGlobalMatrixSync(uid: String, groupId: String) {
        val rootRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        
        // Cluster 1: Identity & Network (Profile + Roster + Org)
        syncCloudProfileRegistry(uid)
        syncCloudRosterRegistry(uid)
        rootRef.child("org").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val map = s.value as? Map<String, String> ?: return
                viewModelScope.launch(Dispatchers.IO) { 
                    val v = ContentValues()
                    for ((k, val_str) in map) v.put(k, val_str)
                    dbDelegate.saveOrgDossier(v) 
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Cluster 2: Strategic Missions (Missions + Handshakes)
        rootRef.child("missions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) {
                        dbDelegate.syncSafeZoneGroup(
                            snap.child("zone_id").getValue(String::class.java) ?: "ZONE",
                            snap.child("assigned_nodes").getValue(String::class.java) ?: "",
                            snap.child("start").getValue(Int::class.java) ?: 0,
                            snap.child("end").getValue(Int::class.java) ?: 2359,
                            snap.child("days").getValue(String::class.java) ?: "ALL",
                            1, 1, "CLOUD_RESTORED"
                        )
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        rootRef.child("handshakes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) dbDelegate.logDevicePairing(
                        uid, "ME", snap.child("peer_uid").getValue(String::class.java) ?: "OTHER",
                        snap.child("peer_name").getValue(String::class.java) ?: "NODE", "SUBJECT", "GUARDIAN"
                    )
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Cluster 3: Forensic Audit (Alerts + History + Logs)
        rootRef.child("alerts").limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) dbDelegate.saveAlert(
                        uid,
                        snap.child("title").getValue(String::class.java) ?: "ALERT", 
                        snap.child("msg").getValue(String::class.java) ?: ""
                    )
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        rootRef.child("history").limitToLast(100).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) dbDelegate.saveHistoryPoint(
                        snap.child("uid").getValue(String::class.java) ?: "",
                        "RESTORED",
                        "NODE",
                        snap.child("lat").getValue(Double::class.java) ?: 0.0,
                        snap.child("lng").getValue(Double::class.java) ?: 0.0,
                        snap.child("speed").getValue(Double::class.java)?.toFloat() ?: 0f,
                        snap.child("battery").getValue(Long::class.java)?.toInt() ?: 100,
                        (snap.child("sos").getValue(Long::class.java) ?: 0L) == 1L,
                        snap.child("address").getValue(String::class.java) ?: ""
                    )
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Cluster 4: Hardware Situational Awareness (SIM Inventory + Transactions)
        rootRef.child("sim_inventory").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) dbDelegate.logSimCard(
                        snap.child("slot").getValue(Int::class.java) ?: 0,
                        snap.child("serial").getValue(String::class.java) ?: "RESTORED",
                        snap.child("operator").getValue(String::class.java) ?: "PROVIDER",
                        null, null, null, null, false
                    )
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        rootRef.child("sim_transactions").limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) dbDelegate.logSimTransaction(
                        snap.child("slot").getValue(Int::class.java) ?: 0,
                        snap.child("mcc").getValue(String::class.java),
                        snap.child("mnc").getValue(String::class.java),
                        snap.child("lac").getValue(String::class.java),
                        snap.child("cid").getValue(String::class.java),
                        snap.child("lat").getValue(Double::class.java) ?: 0.0,
                        snap.child("lng").getValue(Double::class.java) ?: 0.0
                    )
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Cluster 5: Mission Diagnostics (Transitions + Sync Queue)
        rootRef.child("transition_logs").limitToLast(100).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) dbDelegate.saveGeofenceLog(
                        uid, snap.child("avatar").getValue(String::class.java) ?: "NODE",
                        snap.child("zone_id").getValue(String::class.java) ?: "GENERAL",
                        snap.child("zone_name").getValue(String::class.java) ?: "ZONE",
                        snap.child("status").getValue(String::class.java) ?: "UNKNOWN"
                    )
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }
    
    private fun syncCloudProfileRegistry(uid: String) {
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("profile")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = snapshot.value as? Map<String, String> ?: return
                    viewModelScope.launch(Dispatchers.IO) {
                        dbDelegate.saveLocalDossier(uid, map)
                        SystemLogger.i("GlobalSync", "PROFILE_SYNC: Identity dossier architected locally for $uid")
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun syncCloudRosterRegistry(uid: String) {
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("roster")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    viewModelScope.launch(Dispatchers.IO) {
                        for (snap in snapshot.children) {
                            val peerUid = snap.key ?: continue
                            val name = snap.child("display_name").getValue(String::class.java) ?: "PEER"
                            val role = snap.child("role_title").getValue(String::class.java) ?: "FAMILY"
                            
                            val v = ContentValues().apply {
                                put("uid", peerUid)
                                put("display_name", name)
                                put("role_title", role)
                                put("status_text", "CONNECTED")
                                put("readable_time", dbDelegate.getReadableNow())
                            }
                            dbDelegate.saveRosterNode(v)
                        }
                        SystemLogger.i("GlobalSync", "ROSTER_SYNC: Network Matrix synchronized with cloud delta.")
                        fetchRosterMetadata() // Update UI mappings
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    fun fetchRosterMetadata() {
        viewModelScope.launch(Dispatchers.IO) {
            val roster = dbDelegate.getOfflineRoster()
            val mapping = mutableMapOf<String, String>()
            for (node in roster) {
                val name = node["display_name"]?.toString()
                val role = node["role_title"]?.toString()
                if (name != null) mapping[name] = role ?: "PEER"
            }
            _rosterMapping.postValue(mapping)
        }
    }
    
    fun fetchTargetableDevices() {
        FirebaseDatabase.getInstance().getReference("locations")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val devices = mutableSetOf<String>(); devices.add("ALL CONNECTED DEVICES")
                    deviceUidMap.clear()
                    for (snap in snapshot.children) { 
                        val name = snap.child("userName").getValue(String::class.java) 
                        val uid = snap.key
                        if (name != null && uid != null) {
                            devices.add(name)
                            deviceUidMap[name] = uid
                        }
                    }
                    _activeTargetDevices.value = devices.toList()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    @kotlin.jvm.JvmOverloads
    fun submitCloudCoordinateSync(rawName: String, assignedName: String, lat: Double, lng: Double, radius: Double, startTime: Int = 0, endTime: Int = 2359, activeDays: String = "1,2,3,4,5,6,0", existingId: String? = null) {
        val name = rawName // 🚀 L7 PURE-SOURCE: Remove cluster redundancy from the name title
        _uiToastEvent.postValue(SingleEvent("Acquiring Hardware Sync..."))
        
        val geocoder = Geocoder(getApplication())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    val addr = if (addresses.isNotEmpty()) addresses[0].getAddressLine(0) else "Unknown Target"
                    finalizeCloudGeofencePush(name, assignedName, lat, lng, radius, addr, startTime, endTime, activeDays, existingId)
                }
                override fun onError(errorMessage: String?) {
                    finalizeCloudGeofencePush(name, assignedName, lat, lng, radius, "Address Offline", startTime, endTime, activeDays, existingId)
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
                finalizeCloudGeofencePush(name, assignedName, lat, lng, radius, addressString, startTime, endTime, activeDays, existingId)
            }
        }
    }

    private fun finalizeCloudGeofencePush(name: String, assignedName: String, lat: Double, lng: Double, radius: Double, addr: String, startTime: Int, endTime: Int, activeDays: String, existingId: String? = null) {
        val activeId = currentGroupId ?: return
        val data = mapOf(
            "name" to name,
            "assignedName" to assignedName, // 🚀 QA Fix: Global Nodal Payload Injection
            "lat" to lat, 
            "lng" to lng, 
            "radius" to radius, 
            "address" to addr, 
            "isZoneActive" to true, 
            "alertsEnabled" to true,
            "startTime" to startTime,
            "endTime" to endTime,
            "activeDays" to activeDays,
            "created_by" to (com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "ADMIN"),
            "timestamp" to System.currentTimeMillis()
        )
        
        // 🚀 UNIQUE IDENTITY: Prevent data collisions for zones with identical labels
        val uniqueZoneId = existingId ?: "${name}_${System.currentTimeMillis()}"
        
        // 📚 GLOBAL LEDGER: Write to the Group's master geofence registry using a unique ID
        FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences").child(uniqueZoneId).setValue(data)

        // 💾 LOCAL FORENSIC VAULT: Immediate SQLite update to ensure off-grid parity
        viewModelScope.launch(Dispatchers.IO) {
            val v = android.content.ContentValues().apply {
                put("zone_id", uniqueZoneId)
                put("name", name)
                put("lat", lat); put("lng", lng); put("radius", radius)
                put("target", assignedName)
                put("sync_status", "CLOUD_SYNCED")
                put("flag_a", 1); put("flag_b", 1)
            }
            dbDelegate.saveSafeZone(v)
            
            // 🚀 MASTER CLUSTER SYNC: Bridging the Relationship Matrix (V41 Architecture)
            dbDelegate.syncSafeZoneGroup(uniqueZoneId, assignedName, startTime, endTime, activeDays, 1, 1, addr)
        }
        
        // 🚀 TARGETED HANDSHAKE: Multi-Node Cluster Assignment
        if (assignedName != "ALL CONNECTED DEVICES") {
            val targets = assignedName.split(", ").map { it.trim() }
            targets.forEach { target ->
                val targetUid = deviceUidMap[target]
                if (targetUid != null) {
                    val syncRequest = mapOf(
                        "type" to "GEOFENCE_ASSIGNMENT",
                        "zone_name" to name,
                        "group_id" to activeId,
                        "status" to "PENDING_ACCEPTANCE",
                        "timestamp" to System.currentTimeMillis()
                    )
                    FirebaseDatabase.getInstance().getReference("users").child(targetUid).child("geofence_sync_requests").push().setValue(syncRequest)
                }
            }
        }
        
        // 🚀 AUDIT TRAIL: Record the deployment action in the forensic history vault
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "ADMIN"
        val missionStatus = if (existingId != null) "UPDATED" else "DEPLOYED"
        dbDelegate.saveAlert(myUid, "SAFEZONE $missionStatus", "Node '$name' has been successfully ${missionStatus.lowercase()} and assigned to: $assignedName")

        _closeMapEvent.postValue(SingleEvent(true)) 
        setDraftPin(null)
    }

    fun executeCameraMove(target: LatLng) {
        _cameraMoveEvent.postValue(SingleEvent(target))
    }
    
    fun deleteCloudSafeguardZone(id: String) {
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").child(id).removeValue() }
    }
    
    fun toggleBellAlertState(id: String, currentState: Boolean) {
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").child(id).child("alertsEnabled").setValue(!currentState) }
    }

    fun toggleZoneActiveState(id: String, currentState: Boolean) {
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").child(id).child("isZoneActive").setValue(!currentState) }
    }
    
    // 🚀 MISSION DEPLOYMENT HUB (V48: Targeted Forensic Handshake)

    fun sendForensicHandshakeRequest(targetName: String) {
        val activeId = currentGroupId ?: return
        val targetUid = deviceUidMap[targetName] ?: return
        
        // 🛰️ MISSION PAYLOAD: High-Level Assignment Context
        val nowFormatted = java.text.SimpleDateFormat("dd/MM/yy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        
        // 🛰️ MISSION PAYLOAD: High-Level Assignment Context (V49.4)
        val syncRequest = mapOf(
            "type" to "GEOFENCE_ASSIGNMENT",
            "zone_name" to "Active Mission Node",
            "group_id" to activeId,
            "status" to "PENDING_ACCEPTANCE",
            "msg" to "MISSION ALERT: Safe Zone sync dispatched at $nowFormatted. STATUS: WAITING FOR INTEGRATION...",
            "timestamp" to System.currentTimeMillis()
        )
        
        FirebaseDatabase.getInstance().getReference("users").child(targetUid)
            .child("geofence_sync_requests").push().setValue(syncRequest)
            
        // 💾 CLOSED-LOOP MASTER ARCHIVAL (L7 Standard): Update the local Master Tables
        val db = DatabaseHelper.getInstance(getApplication())
        val now = System.currentTimeMillis()
        
        // 1. Update SafeZoneGroup_Table (Master Mission Assignment)
        db.syncSafeZoneGroup("ACTIVE_MISSION", targetUid, 0, 100, "ALL", 1, 1, "MISSION_DEPLOYED")
        
        // 2. Update Pairing Log (Confirm Handshake in the TABLE_PAIRING)
        db.logDevicePairing(FirebaseAuth.getInstance().uid ?: "ADMIN", "MISSION_COMMANDER", 
                           targetUid, targetName, "SUBJECT", "GUARDIAN", "DEVICE_A", "DEVICE_B")

        // 3. Log Forensic Deployment (History Archive)
        val missionMsg = "HANDSHAKE SENT: $nowFormatted. DELIVERY: IN_TRANSIT. RECEIVED: PENDING."
        db.saveGeofenceLog(targetUid, targetName, "MISSION_CONTROL", missionMsg, "DEPLOYED")

        _uiToastEvent.postValue(SingleEvent("Mission Deployment Sequence Transmitted to $targetName!"))
    }
    
    fun logManualGeofenceCheck(id: String, logString: String) {
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").child(id).child("history").push().setValue(logString) }
    }

    fun fetchGeofenceHistory(id: String) {
        val activeId = currentGroupId ?: return
        FirebaseDatabase.getInstance().getReference("groups").child(activeId).child("geofences").child(id).child("history")
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

    fun resolveActiveProfileAddress() {
        viewModelScope.launch(Dispatchers.IO) {
            val addr = dbDelegate.getActiveProfileAddress()
            _profileAddress.postValue(addr)
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentGroupId?.let { FirebaseDatabase.getInstance().getReference("groups").child(it).child("geofences").removeEventListener(geofenceListener!!) }
    }
}
