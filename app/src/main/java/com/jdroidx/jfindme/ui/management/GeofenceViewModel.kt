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
import com.jdroidx.jfindme.data.remote.FirebaseComm
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

data class RenderedGeofence @JvmOverloads constructor(
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
    val activeDays: String = "1,2,3,4,5,6,0", // 0=Sun, 1=Mon...
    val creatorUid: String? = null,
    val creatorAgentId: String? = null,
    val polygonPointsJson: String? = null,
    val dwellMinutes: Int = 0
) {
    val isPolygon: Boolean
        get() = !polygonPointsJson.isNullOrBlank()
}

/**
 * Hardened MVVM Geofence ViewModel (V36 MASTER)
 * Synchronized with V36 Relational Database Engine (DatabaseHelper).
 */
class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val dbDelegate = DatabaseHelper.getInstance(application)
    
    private val _activeZones = MutableLiveData<List<RenderedGeofence>>()
    val activeZones: LiveData<List<RenderedGeofence>> = _activeZones

    private val _archivedZones = MutableLiveData<List<RenderedGeofence>>()
    val archivedZones: LiveData<List<RenderedGeofence>> = _archivedZones

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
        loadInitialData()
    }

    fun loadInitialData() {
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
            FirebaseComm.resolveHardwareIdentity(safeUid) { agentId ->
                val finalId = agentId ?: safeUid
                FirebaseComm.getIdentityRef(finalId).child("active_group")
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
            }
        } else { engageStandaloneSandbox(prefs) }
    }

    private fun engageStandaloneSandbox(prefs: android.content.SharedPreferences) {
        val safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "ORPHANED_NODE"
        currentGroupId = "P2P_MATRIX_$safeUid"
        prefs.edit().putString("active_group", currentGroupId).apply()
        initFirebaseCloudLink()
    }
    
    fun setDraftPin(latLng: LatLng?) { _draftCoordinateLocation.value = latLng }

    fun getHostDeviceCode(): String {
        val prefs = getApplication<Application>().getSharedPreferences("system_registry", Application.MODE_PRIVATE)
        val localAgentId = prefs.getString("agent_id", null)
        if (!localAgentId.isNullOrBlank()) return localAgentId
        
        val safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
        if (!safeUid.isNullOrBlank()) {
            val resolved = dbDelegate.resolveAgentIdLocally(safeUid)
            if (!resolved.isNullOrBlank()) return resolved
        }
        val fallbackHw = com.jdroidx.jfindme.engine.sync.FirebasePairingManager.getDeviceId(getApplication())
        return dbDelegate.resolveForensicAgentId(fallbackHw)
    }

    private fun initFirebaseCloudLink() {
        // [Variables] | [Constants] | [Dependencies] | [Integration Points]
        // Load local SQLite geofence vault immediately so offline perimeters are always visible
        loadLocalBackupVault(false)
        val activeId = currentGroupId ?: return
        val safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return
        val hostCode = getHostDeviceCode()
        performGlobalMatrixSync(safeUid, activeId)
        
        val ref = FirebaseComm.relationshipsRef.child(activeId).child("geofences")
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
                    // 🛡️ Type-Safe: alertsEnabled/isZoneActive can be Boolean, Int(0/1), or String
                    val alertsRaw = snap.child("alertsEnabled").value
                    val alertsEnabled = when (alertsRaw) { is Boolean -> alertsRaw; is Number -> alertsRaw.toInt() != 0; is String -> alertsRaw.equals("true", true); else -> true }
                    val activeRaw = snap.child("isZoneActive").value
                    val isZoneActive = when (activeRaw) { is Boolean -> activeRaw; is Number -> activeRaw.toInt() != 0; is String -> activeRaw.equals("true", true); else -> true }
                    val startTime = snap.child("startTime").value?.toString()?.toIntOrNull() ?: 0
                    val endTime = snap.child("endTime").value?.toString()?.toIntOrNull() ?: 2359
                    val activeDays = snap.child("activeDays").getValue(String::class.java) ?: "1,2,3,4,5,6,0"
                    val creatorUid = snap.child("creator_uid").getValue(String::class.java)
                    val creatorAgentId = snap.child("creator_agent_id").getValue(String::class.java)
                    val polyPoints = snap.child("polygon_points").getValue(String::class.java)
                    val dwellMin = snap.child("dwell_minutes").value?.toString()?.toIntOrNull() ?: 0
                    
                    list.add(RenderedGeofence(id, name, lat, lng, radius, assignedName, address, alertsEnabled, isZoneActive, startTime, endTime, activeDays, creatorUid, creatorAgentId, polyPoints, dwellMin))
                }
                
                // 🚀 V36 Optimized Sync: Single Batch Commit (Forensic Efficiency)
                if (list.isNotEmpty()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        list.forEach { zone ->
                            val v = ContentValues().apply {
                                put("zone_id", zone.id); put("name", zone.name); put("target", zone.assignedName)
                                put("lat", zone.lat); put("lng", zone.lng); put("radius", zone.radius)
                                put("sync_status", "CLOUD_SYNCED")
                                put("flag_a", if (zone.alertsEnabled) 1 else 0)
                                put("flag_b", if (zone.isZoneActive) 1 else 0)
                                put("polygon_points", zone.polygonPointsJson)
                                put("dwell_minutes", zone.dwellMinutes)
                            }
                            dbDelegate.saveSafeZone(v)
                        }
                        
                        // 🚀 QA Fix: Force Merge with Local Cache (Prevent Vanishing Nodes)
                        val localZones = dbDelegate.getOfflineSafeZones()
                        val mergedList = list.toMutableList()
                        localZones.forEach { map ->
                            val id = map["zone_id"] as String
                            if (mergedList.none { it.id == id }) {
                                mergedList.add(RenderedGeofence(
                                    id, map["name"] as? String ?: "Local Node",
                                    map["lat"] as? Double ?: 0.0, map["lng"] as? Double ?: 0.0,
                                    map["radius"] as? Double ?: 100.0, map["target"] as? String ?: "All",
                                    "Local Forensic Cache", (map["flag_a"] as? Int ?: 1) == 1,
                                    (map["flag_b"] as? Int ?: 1) == 1, 0, 2359, "1,2,3,4,5,6,0",
                                    null, null,
                                    map["polygon_points"] as? String,
                                    (map["dwell_minutes"] as? Int) ?: 0
                                ))
                            }
                        }
                        _activeZones.postValue(mergedList)
                        seedAndSyncGeofencingTree(hostCode, mergedList)
                    }
                } else {
                     // Empty Cloud: We keep the Local Cache (V47 Protocol)
                     loadLocalBackupVault(false)
                }
            }
            override fun onCancelled(error: DatabaseError) { loadLocalBackupVault(true) }
        }
        ref.addValueEventListener(geofenceListener!!)

        // Immediate Seeding of Creator Root under /Geofencing/{creatorDeviceCode}
        val prefs = getApplication<Application>().getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("user_nickname", "Host Device") ?: "Host Device"
        FirebaseComm.getGeofencingCreatorRef(hostCode).updateChildren(mapOf(
            "device_name" to userName,
            "owner_uid" to safeUid,
            "updated_at" to System.currentTimeMillis()
        ))
    }
    
    private fun seedAndSyncGeofencingTree(creatorId: String, safeZones: List<RenderedGeofence>) {
        val hostCode = getHostDeviceCode()
        val cleanCreatorId = FirebaseComm.sanitizePathKey(hostCode)
        if (cleanCreatorId.isBlank()) return

        try {
            val prefs = getApplication<Application>().getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE)
            val userName = prefs.getString("user_nickname", "Host Device") ?: "Host Device"
            val safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: cleanCreatorId

            val creatorRef = FirebaseComm.getGeofencingCreatorRef(cleanCreatorId)
            creatorRef.updateChildren(mapOf(
                "device_name" to userName,
                "owner_uid" to safeUid,
                "updated_at" to System.currentTimeMillis()
            ))

            // 🧹 PURGE LEGACY GRP_ BRANCH if different from canonical Host Device Code
            if (creatorId.startsWith("GRP_") && creatorId != cleanCreatorId) {
                try {
                    FirebaseComm.geofencingRef.child(FirebaseComm.sanitizePathKey(creatorId)).removeValue()
                } catch (e: Exception) { }
            }

            // Build canonical lookup from offline roster
            val roster = dbDelegate.getOfflineRoster()
            val nameToCodeMap = mutableMapOf<String, String>()
            val codeToNameMap = mutableMapOf<String, String>()
            val codeToRoleMap = mutableMapOf<String, String>()
            val codeToAvatarMap = mutableMapOf<String, String>()
            for (node in roster) {
                val cName = node["fullName"]?.toString() ?: ""
                val cCode = node["uid_1"]?.toString() ?: ""
                val cRole = node["role_type"]?.toString() ?: "MEMBER"
                val cAvatar = node["avatar_id"]?.toString() ?: "butterfly"
                if (cCode.isNotEmpty()) {
                    if (cName.isNotEmpty()) nameToCodeMap[cName] = cCode
                    nameToCodeMap[cCode] = cCode
                    codeToNameMap[cCode] = cName.ifEmpty { cCode }
                    codeToRoleMap[cCode] = cRole
                    codeToAvatarMap[cCode] = cAvatar
                }
            }

            for (zone in safeZones) {
                try {
                    val cleanZoneId = FirebaseComm.sanitizePathKey(zone.id)
                    val zoneData = mapOf(
                        "zone_id" to zone.id,
                        "zone_name" to zone.name,
                        "lat" to zone.lat,
                        "lng" to zone.lng,
                        "radius" to zone.radius,
                        "assigned_targets" to zone.assignedName,
                        "is_active" to zone.isZoneActive,
                        "alerts_enabled" to zone.alertsEnabled,
                        "startTime" to zone.startTime,
                        "endTime" to zone.endTime,
                        "activeDays" to zone.activeDays,
                        "address" to zone.address,
                        "created_on" to FirebaseComm.getForensicTimestamp(),
                        "updated_at" to System.currentTimeMillis()
                    )
                    val zRef = creatorRef.child("safe_zones").child(cleanZoneId)
                    zRef.updateChildren(zoneData)

                    // 📝 AUDIT CHANGE LOG: Record Safehouse mutation under /devices/{cleanCreatorId}/Change_Log/Safehouse
                    FirebaseComm.logChange(cleanCreatorId, FirebaseComm.LOG_ITEM_SAFEHOUSE, "ZONE_UPDATE", cleanZoneId, zoneData)

                    // 🎯 DEDUPLICATED CANONICAL DEVICE CODES ONLY
                    val targetCodes = if (zone.assignedName == "ALL CONNECTED DEVICES") {
                        nameToCodeMap.values.filter { it.isNotEmpty() }.distinct()
                    } else {
                        zone.assignedName.split(", ")
                            .mapNotNull { nameToCodeMap[it.trim()] ?: deviceUidMap[it.trim()] ?: it.trim().takeIf { s -> s.isNotEmpty() } }
                            .distinct()
                    }

                    for (code in targetCodes) {
                        try {
                            val cleanTargetCode = FirebaseComm.sanitizePathKey(code)
                            val targetName = codeToNameMap[code] ?: code
                            val targetRole = codeToRoleMap[code] ?: rosterMapping.value?.get(targetName) ?: "MEMBER"
                            val targetAvatar = codeToAvatarMap[code] ?: "butterfly"
                            val connectedDeviceData = mapOf(
                                "device_id" to cleanTargetCode,
                                "device_name" to targetName,
                                "avatar_id" to targetAvatar,
                                "role" to targetRole,
                                "assigned_at" to System.currentTimeMillis()
                            )
                            zRef.child("connected_devices").child(cleanTargetCode).updateChildren(connectedDeviceData)
                        } catch (tErr: Exception) {
                            SystemLogger.e("GeofenceSync", "TARGET_NODE_SYNC_ERR: $code", tErr)
                        }
                    }
                } catch (zErr: Exception) {
                    SystemLogger.e("GeofenceSync", "ZONE_NODE_SYNC_ERR: ${zone.id}", zErr)
                }
            }
        } catch (e: Exception) {
            SystemLogger.e("GeofenceSync", "SEED_TREE_ERR", e)
        }
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
                        "1,2,3,4,5,6,0", // Default Days
                        null, null,
                        map["polygon_points"] as? String,
                        (map["dwell_minutes"] as? Int) ?: 0
                    )
                }
                _activeZones.postValue(list)
                currentGroupId?.let { seedAndSyncGeofencingTree(it, list) }
                if (isEmergency) _uiToastEvent.postValue(SingleEvent("Cloud Offline: Forensic Vault Engaged!"))
            }
        } catch (e: Exception) {}
    }
    
    private fun performGlobalMatrixSync(uid: String, groupId: String) {
        FirebaseComm.resolveHardwareIdentity(uid) { agentId ->
            val finalId = agentId ?: return@resolveHardwareIdentity
            val rootRef = FirebaseComm.getDeviceRef(finalId)
            
            // Cluster 1: Identity & Network (Profile + Roster + Org) - Single-value fetch to prevent perpetual listeners
            syncCloudProfileRegistry(finalId)
            syncCloudRosterRegistry(finalId)
            rootRef.child("profile").child("org").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val rawMap = s.value as? Map<*, *> ?: return
                    val map = rawMap.entries.associate { it.key.toString() to (it.value?.toString() ?: "") }
                    viewModelScope.launch(Dispatchers.IO) { 
                        try {
                            val v = ContentValues()
                            for ((k, val_str) in map) v.put(k, val_str)
                            dbDelegate.saveOrgDossier(v) 
                        } catch (e: Exception) {
                            SystemLogger.e("GlobalSync", "ORG_DOSSIER_ERR", e)
                        }
                    }
                }
                override fun onCancelled(e: DatabaseError) {}
            })

        // Cluster 2: Strategic Missions (Missions + Handshakes) - Single-value fetch
        rootRef.child("missions").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) {
                        try {
                            dbDelegate.syncSafeZoneGroup(
                                snap.child("zone_id").value?.toString() ?: "ZONE",
                                snap.child("assigned_nodes").value?.toString() ?: "",
                                snap.child("start").value?.toString()?.toIntOrNull() ?: 0,
                                snap.child("end").value?.toString()?.toIntOrNull() ?: 2359,
                                snap.child("days").value?.toString() ?: "ALL",
                                1, 1, "CLOUD_RESTORED"
                            )
                        } catch (e: Exception) {
                            SystemLogger.e("GlobalSync", "MISSIONS_PARSE_ERR", e)
                        }
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        rootRef.child("handshakes").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) {
                        try {
                            dbDelegate.logDevicePairing(
                                uid, "ME", "SELF", // Host identity
                                snap.child("peer_uid").value?.toString() ?: "OTHER",
                                snap.child("peer_name").value?.toString() ?: "NODE", 
                                "SUBJECT", "GUARDIAN" // Peer identity & Role
                            )
                        } catch (e: Exception) {
                            SystemLogger.e("GlobalSync", "HANDSHAKE_PARSE_ERR", e)
                        }
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Cluster 3: Forensic Audit (Alerts + History + Logs) - Single-value fetch
        rootRef.child("alerts").limitToLast(50).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) {
                        try {
                            dbDelegate.saveSysLog("I", 
                                snap.child("title").value?.toString() ?: "ALERT", 
                                snap.child("msg").value?.toString() ?: ""
                            )
                        } catch (e: Exception) {
                            SystemLogger.e("GlobalSync", "ALERTS_PARSE_ERR", e)
                        }
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        rootRef.child("history").limitToLast(100).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) {
                        try {
                            val historyUid = snap.child("uid").value?.toString() ?: ""
                            // 🚀 QA Fix: Identity Resolution Bridge
                            val tsStr = snap.child("timestamp").value?.toString() ?: dbDelegate.getReadableNow()
                            val latVal = snap.child("lat").value?.toString()?.toDoubleOrNull() ?: 0.0
                            val lngVal = snap.child("lng").value?.toString()?.toDoubleOrNull() ?: 0.0
                            val speedVal = snap.child("speed").value?.toString()?.toFloatOrNull() ?: 0f
                            // 🛡️ Type-Safe Firebase Read: battery can be Long, Int, or Boolean in mixed-schema nodes
                            val batteryVal = snap.child("battery").value?.toString()?.toIntOrNull() ?: 100
                            // 🛡️ Type-Safe Firebase Read: sos can be Boolean (true/false) or Long (0/1)
                            val sosRaw = snap.child("sos").value
                            val sosVal = when (sosRaw) {
                                is Boolean -> sosRaw
                                is Number  -> sosRaw.toLong() == 1L
                                is String  -> sosRaw.equals("true", ignoreCase = true) || sosRaw == "1"
                                else       -> false
                            }
                            dbDelegate.saveHistoryPoint(
                                historyUid,
                                "IDENTITY_MIRROR",
                                latVal, lngVal, speedVal, batteryVal, sosVal,
                                "CLOUD_SYNC",
                                tsStr
                            )
                        } catch (e: Exception) {
                            SystemLogger.e("GlobalSync", "HISTORY_PARSE_ERR", e)
                        }
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Cluster 4: Hardware Situational Awareness (SIM Inventory + Transactions) - Single-value fetch
        rootRef.child("sim_inventory").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) {
                        try {
                            dbDelegate.logSimCard(
                                snap.child("slot").value?.toString()?.toIntOrNull() ?: 0,
                                snap.child("serial").value?.toString() ?: "RESTORED",
                                snap.child("operator").value?.toString() ?: "PROVIDER",
                                "", "", "", "", false
                            )
                        } catch (e: Exception) {
                            SystemLogger.e("GlobalSync", "SIM_INV_ERR", e)
                        }
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        rootRef.child("sim_transactions").limitToLast(50).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) {
                        try {
                            dbDelegate.logSimTransaction(
                                snap.child("slot").value?.toString()?.toIntOrNull() ?: 0,
                                snap.child("mcc").value?.toString() ?: "",
                                snap.child("mnc").value?.toString() ?: "",
                                snap.child("lac").value?.toString() ?: "",
                                snap.child("cid").value?.toString() ?: "",
                                snap.child("lat").value?.toString()?.toDoubleOrNull() ?: 0.0,
                                snap.child("lng").value?.toString()?.toDoubleOrNull() ?: 0.0,
                                "CLOUD"
                            )
                        } catch (e: Exception) {
                            SystemLogger.e("GlobalSync", "SIM_TX_ERR", e)
                        }
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Cluster 5: Mission Diagnostics (Transitions + Sync Queue) - Single-value fetch
        rootRef.child("transition_logs").limitToLast(100).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    for (snap in s.children) {
                        try {
                            dbDelegate.saveGeofenceLog(
                                uid, snap.child("avatar").value?.toString() ?: "NODE",
                                snap.child("zone_id").value?.toString() ?: "GENERAL",
                                snap.child("zone_name").value?.toString() ?: "ZONE",
                                snap.child("status").value?.toString() ?: "UNKNOWN"
                            )
                        } catch (e: Exception) {
                            SystemLogger.e("GlobalSync", "TRANSITION_LOG_ERR", e)
                        }
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        }
    }
    
    private fun syncCloudProfileRegistry(uid: String) {
        FirebaseComm.getProfileRef(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rawMap = snapshot.value as? Map<*, *> ?: return
                    val stringMap = rawMap.entries.associate { it.key.toString() to (it.value?.toString() ?: "") }
                    
                    viewModelScope.launch(Dispatchers.IO) {
                        dbDelegate.commitAnalyticalDossier(uid, stringMap)
                        SystemLogger.i("GlobalSync", "PROFILE_SYNC: Root identity dossier architected locally for $uid")
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun syncCloudRosterRegistry(uid: String) {
        FirebaseComm.getRelationshipsRef(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    viewModelScope.launch(Dispatchers.IO) {
                        for (snap in snapshot.children) {
                            val peerUid = snap.key ?: continue
                            val name = snap.child("name").getValue(String::class.java) ?: snap.child("display_name").getValue(String::class.java) ?: "PEER"
                            val role = snap.child("role").getValue(String::class.java) ?: snap.child("role_title").getValue(String::class.java) ?: "FAMILY"
                            
                            val v = ContentValues().apply {
                                put("uid_1", peerUid)
                                put("fullName", name)
                                put("role_type", role)
                                put("st_session_state", "CONNECTED")
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
                val name = node["fullName"]?.toString()
                val role = node["role_type"]?.toString()
                if (name != null) mapping[name] = role ?: "PEER"
            }
            _rosterMapping.postValue(mapping)
        }
    }
    
    fun fetchTargetableDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            val roster = dbDelegate.getOfflineRoster()
            val devices = mutableSetOf<String>()
            devices.add("ALL CONNECTED DEVICES")
            
            deviceUidMap.clear()
            for (node in roster) {
                val name = node["fullName"]?.toString() ?: continue
                val deviceCode = node["uid_1"]?.toString() ?: continue
                devices.add(name)
                deviceUidMap[name] = deviceCode
                deviceUidMap[deviceCode] = deviceCode
            }
            withContext(Dispatchers.Main) {
                _activeTargetDevices.value = devices.toList()
            }
        }
    }

    @JvmOverloads
    fun submitCloudCoordinateSync(
        rawName: String,
        assignedName: String,
        lat: Double,
        lng: Double,
        radius: Double,
        startTime: Int = 0,
        endTime: Int = 2359,
        activeDays: String = "1,2,3,4,5,6,0",
        existingId: String? = null,
        polygonPointsJson: String? = null,
        dwellMinutes: Int = 0
    ) {
        try {
            val name = rawName // 🚀 L7 PURE-SOURCE: Remove cluster redundancy from the name title
            _uiToastEvent.postValue(SingleEvent("Acquiring Hardware Sync..."))
            
            val geocoder = Geocoder(getApplication())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        val addr = if (addresses.isNotEmpty()) addresses[0].getAddressLine(0) else "Unknown Target"
                        finalizeCloudGeofencePush(name, assignedName, lat, lng, radius, addr, startTime, endTime, activeDays, existingId, polygonPointsJson, dwellMinutes)
                    }
                    override fun onError(errorMessage: String?) {
                        finalizeCloudGeofencePush(name, assignedName, lat, lng, radius, "Address Offline", startTime, endTime, activeDays, existingId, polygonPointsJson, dwellMinutes)
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
                    finalizeCloudGeofencePush(name, assignedName, lat, lng, radius, addressString, startTime, endTime, activeDays, existingId, polygonPointsJson, dwellMinutes)
                }
            }
        } catch (e: Exception) {
            SystemLogger.e("GeofenceViewModel", "SUBMIT_SYNC_FATAL", e)
            _uiToastEvent.postValue(SingleEvent("Hardware Sync Fatal: " + e.message))
        }
    }

    private fun finalizeCloudGeofencePush(
        name: String,
        assignedName: String,
        lat: Double,
        lng: Double,
        radius: Double,
        addr: String,
        startTime: Int,
        endTime: Int,
        activeDays: String,
        existingId: String? = null,
        polygonPointsJson: String? = null,
        dwellMinutes: Int = 0
    ) {
        val activeId = currentGroupId ?: return
        val hostCode = getHostDeviceCode()
        val cleanHostCode = FirebaseComm.sanitizePathKey(hostCode)
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
            "polygon_points" to (polygonPointsJson ?: ""),
            "dwell_minutes" to dwellMinutes,
            "created_by" to (com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "ADMIN"),
            "timestamp" to System.currentTimeMillis()
        )
        
        // 🚀 UNIQUE IDENTITY: Prevent data collisions for zones with identical labels
        val rawZoneId = existingId ?: "${name}_${System.currentTimeMillis()}"
        val uniqueZoneId = FirebaseComm.sanitizePathKey(rawZoneId)
        
        // 📚 GLOBAL LEDGER: Write to the Group's master geofence registry using a unique ID
        if (FirebaseComm.isAuthenticated()) {
            try {
                FirebaseComm.relationshipsRef.child(FirebaseComm.sanitizePathKey(activeId)).child("geofences").child(uniqueZoneId).setValue(data)
            } catch (e: Exception) {
                SystemLogger.e("GeofenceViewModel", "RELATIONSHIPS_GEOFENCE_WRITE_ERR", e)
            }
        }

        // 🌳 GEOFENCING TOP-LEVEL MATRIX SYNC: /Geofencing/{creatorDeviceCode}/safe_zones/{zoneId}/connected_devices
        if (FirebaseComm.isAuthenticated()) {
            try {
            val geofencingZoneData = mapOf(
                "zone_id" to uniqueZoneId,
                "zone_name" to name,
                "lat" to lat,
                "lng" to lng,
                "radius" to radius,
                "assigned_targets" to assignedName,
                "is_active" to true,
                "alerts_enabled" to true,
                "startTime" to startTime,
                "endTime" to endTime,
                "activeDays" to activeDays,
                "polygon_points" to (polygonPointsJson ?: ""),
                "dwell_minutes" to dwellMinutes,
                "address" to addr,
                "created_on" to FirebaseComm.getForensicTimestamp(),
                "updated_at" to System.currentTimeMillis()
            )
            val geofencingSafeZoneRef = FirebaseComm.getGeofencingSafeZoneRef(cleanHostCode, uniqueZoneId)

            // Build canonical lookup from offline roster
            val roster = dbDelegate.getOfflineRoster()
            val nameToCodeMap = mutableMapOf<String, String>()
            val codeToNameMap = mutableMapOf<String, String>()
            val codeToRoleMap = mutableMapOf<String, String>()
            val codeToAvatarMap = mutableMapOf<String, String>()
            for (node in roster) {
                val cName = node["fullName"]?.toString() ?: ""
                val cCode = node["uid_1"]?.toString() ?: ""
                val cRole = node["role_type"]?.toString() ?: "MEMBER"
                val cAvatar = node["avatar_id"]?.toString() ?: "butterfly"
                if (cCode.isNotEmpty()) {
                    if (cName.isNotEmpty()) nameToCodeMap[cName] = cCode
                    nameToCodeMap[cCode] = cCode
                    codeToNameMap[cCode] = cName.ifEmpty { cCode }
                    codeToRoleMap[cCode] = cRole
                    codeToAvatarMap[cCode] = cAvatar
                }
            }

            // Register each assigned connected device using its UNIQUE canonical Device Code
            val targetCodes = if (assignedName == "ALL CONNECTED DEVICES") {
                nameToCodeMap.values.filter { it.isNotEmpty() }.distinct()
            } else {
                assignedName.split(", ")
                    .mapNotNull { nameToCodeMap[it.trim()] ?: deviceUidMap[it.trim()] ?: it.trim().takeIf { s -> s.isNotEmpty() } }
                    .distinct()
            }

            val myAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
            for (code in targetCodes) {
                try {
                    val cleanTargetCode = FirebaseComm.sanitizePathKey(code)
                    // 🛡️ Filter out Creator Self-Device (Self device cannot be a connected target to its own safe zone)
                    if (cleanTargetCode == cleanHostCode || cleanTargetCode == myAuthUid || cleanTargetCode.equals(cleanHostCode, ignoreCase = true)) {
                        continue;
                    }
                    val targetName = codeToNameMap[code] ?: code
                    val role = codeToRoleMap[code] ?: rosterMapping.value?.get(targetName) ?: "MEMBER"
                    val targetAvatar = codeToAvatarMap[code] ?: "butterfly"
                    val connectedDeviceData = mapOf(
                        "device_id" to cleanTargetCode,
                        "device_name" to targetName,
                        "avatar_id" to targetAvatar,
                        "role" to role,
                        "assigned_at" to System.currentTimeMillis()
                    )
                    geofencingSafeZoneRef.child("connected_devices").child(cleanTargetCode).updateChildren(connectedDeviceData)
                } catch (tErr: Exception) {
                    SystemLogger.e("GeofenceViewModel", "TARGET_NODE_PUSH_ERR: $code", tErr)
                }
            }
            } catch (e: Exception) {
                SystemLogger.e("GeofenceViewModel", "GEOFENCING_MATRIX_WRITE_ERR", e)
            }
        }

        // 💾 LOCAL FORENSIC VAULT: Immediate SQLite update to ensure off-grid parity
        viewModelScope.launch(Dispatchers.IO) {
            val v = android.content.ContentValues().apply {
                put("zone_id", uniqueZoneId)
                put("name", name)
                put("lat", lat); put("lng", lng); put("radius", radius)
                put("target", assignedName)
                put("sync_status", "CLOUD_SYNCED")
                put("is_active", 1)
                put("is_alerts_enabled", 1)
                put("polygon_points", polygonPointsJson)
                put("dwell_minutes", dwellMinutes)
                put("flag_a", 1); put("flag_b", 1)
            }
            dbDelegate.saveSafeZone(v)
            
            // 🚀 MASTER CLUSTER SYNC: Bridging the Relationship Matrix (V41 Architecture)
            dbDelegate.syncSafeZoneGroup(uniqueZoneId, assignedName, startTime, endTime, activeDays, 1, 1, addr)
        }
        
        // 🚀 TARGETED HANDSHAKE: Multi-Node Cluster Assignment (V50.0 - Full Payload)
        if (FirebaseComm.isAuthenticated() && assignedName != "ALL CONNECTED DEVICES") {
            val targets = assignedName.split(", ").map { it.trim() }
            targets.forEach { target ->
                val targetUid = deviceUidMap[target]
                if (targetUid != null) {
                    val syncRequest = mapOf(
                        "type" to "GEOFENCE_ASSIGNMENT",
                        "zone_id" to uniqueZoneId,
                        "zone_name" to name,
                        "group_id" to activeId,
                        "lat" to lat,
                        "lng" to lng,
                        "radius" to radius,
                        "status" to "PENDING_ACCEPTANCE",
                        "timestamp" to System.currentTimeMillis()
                    )
                    FirebaseComm.resolveHardwareIdentity(targetUid) { agentId ->
                        val finalTargetId = agentId ?: targetUid
                        FirebaseDatabase.getInstance().getReference(FirebaseComm.NODE_DEVICES)
                            .child(finalTargetId)
                            .child("Identity")
                            .child("geofence_sync_requests")
                            .push()
                            .setValue(syncRequest)
                    }
                }
            }
        }
        
        // 🚀 AUDIT TRAIL: Record the deployment action in the forensic history vault
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "ADMIN"
        val missionStatus = if (existingId != null) "UPDATED" else "DEPLOYED"
        dbDelegate.saveSysLog("I", "SAFEZONE $missionStatus", "Node '$name' has been successfully ${missionStatus.lowercase()} and assigned to: $assignedName")

        _closeMapEvent.postValue(SingleEvent(true)) 
        setDraftPin(null)
    }

    fun executeCameraMove(target: LatLng) {
        _cameraMoveEvent.postValue(SingleEvent(target))
    }
    
    fun archiveSafeZone(id: String) {
        val grp = currentGroupId
        if (FirebaseComm.isAuthenticated() && grp != null) {
            val srcRef = FirebaseComm.relationshipsRef.child(grp).child("geofences").child(id)
            val destRef = FirebaseComm.relationshipsRef.child(grp).child("archived_geofences").child(id)
            srcRef.get().addOnSuccessListener { snap ->
                if (snap.exists() && snap.value != null) {
                    destRef.setValue(snap.value)
                    srcRef.removeValue()
                } else {
                    srcRef.removeValue()
                }
            }.addOnFailureListener {
                srcRef.removeValue()
            }

            // 🌳 GEOFENCING MATRIX PARITY: Update is_active and is_archived flags
            FirebaseComm.getGeofencingSafeZoneRef(grp, id).updateChildren(mapOf(
                "is_active" to false,
                "is_archived" to true,
                "archived_at" to System.currentTimeMillis()
            ))
            FirebaseComm.logChange(grp, FirebaseComm.LOG_ITEM_SAFEHOUSE, "ZONE_ARCHIVE", id, mapOf("zone_id" to id))
        }
        viewModelScope.launch(Dispatchers.IO) {
            dbDelegate.archiveSafeZone(id)
            loadArchivedSafeZones()
        }
        _uiToastEvent.postValue(SingleEvent("📦 Safe Zone archived. All notifications preserved."))
    }

    fun restoreSafeZone(id: String) {
        val grp = currentGroupId
        if (FirebaseComm.isAuthenticated() && grp != null) {
            val srcRef = FirebaseComm.relationshipsRef.child(grp).child("archived_geofences").child(id)
            val destRef = FirebaseComm.relationshipsRef.child(grp).child("geofences").child(id)
            srcRef.get().addOnSuccessListener { snap ->
                if (snap.exists() && snap.value != null) {
                    destRef.setValue(snap.value)
                    srcRef.removeValue()
                }
            }

            // 🌳 GEOFENCING MATRIX PARITY: Restore is_active and clear is_archived flag
            FirebaseComm.getGeofencingSafeZoneRef(grp, id).updateChildren(mapOf(
                "is_active" to true,
                "is_archived" to false,
                "restored_at" to System.currentTimeMillis()
            ))
            FirebaseComm.logChange(grp, FirebaseComm.LOG_ITEM_SAFEHOUSE, "ZONE_RESTORE", id, mapOf("zone_id" to id))
        }
        viewModelScope.launch(Dispatchers.IO) {
            dbDelegate.restoreArchivedSafeZone(id)
            loadArchivedSafeZones()
        }
        _uiToastEvent.postValue(SingleEvent("🟢 Safe Zone restored to Active Monitoring!"))
    }

    fun purgeArchivedSafeZone(id: String) {
        val grp = currentGroupId
        if (grp != null) {
            FirebaseComm.relationshipsRef.child(grp).child("archived_geofences").child(id).removeValue()
        }
        viewModelScope.launch(Dispatchers.IO) {
            dbDelegate.purgeArchivedSafeZone(id)
            loadArchivedSafeZones()
        }
        _uiToastEvent.postValue(SingleEvent("🗑️ Archived Safe Zone permanently purged."))
    }

    fun restoreAllArchivedSafeZones() {
        val grp = currentGroupId
        if (grp != null) {
            val srcRef = FirebaseComm.relationshipsRef.child(grp).child("archived_geofences")
            val destRef = FirebaseComm.relationshipsRef.child(grp).child("geofences")
            srcRef.get().addOnSuccessListener { snap ->
                if (snap.exists() && snap.value != null) {
                    for (child in snap.children) {
                        val k = child.key ?: continue
                        destRef.child(k).setValue(child.value)
                    }
                    srcRef.removeValue()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dbDelegate.restoreAllArchivedSafeZones()
            loadArchivedSafeZones()
        }
        _uiToastEvent.postValue(SingleEvent("🟢 All Archived Safe Zones restored to Active Monitoring!"))
    }

    fun purgeAllArchivedSafeZones() {
        val grp = currentGroupId
        if (grp != null) {
            FirebaseComm.relationshipsRef.child(grp).child("archived_geofences").removeValue()
        }
        viewModelScope.launch(Dispatchers.IO) {
            dbDelegate.purgeAllArchivedSafeZones()
            loadArchivedSafeZones()
        }
        _uiToastEvent.postValue(SingleEvent("🗑️ All Archived Safe Zones permanently purged."))
    }

    fun loadArchivedSafeZones() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val archivedList = dbDelegate.getArchivedSafeZones()
                val list = archivedList.map { map ->
                    RenderedGeofence(
                        map["zone_id"] as String,
                        map["name"] as? String ?: "Archived Node",
                        map["lat"] as? Double ?: 0.0,
                        map["lng"] as? Double ?: 0.0,
                        map["radius"] as? Double ?: 100.0,
                        map["target"] as? String ?: "",
                        "Archive Vault",
                        (map["flag_a"] as? Int ?: 1) == 1,
                        false, // Inactive
                        0, 2359, "1,2,3,4,5,6,0",
                        null, null,
                        map["polygon_points"] as? String,
                        (map["dwell_minutes"] as? Int) ?: 0
                    )
                }
                _archivedZones.postValue(list)
            } catch (e: Exception) {}
        }
    }

    fun getDeviceEventLogs(zoneId: String, deviceName: String): List<String> {
        val rawLogs = dbDelegate.getGeofenceLogsByDeviceAndZone(zoneId, deviceName)
        if (rawLogs.isEmpty()) return listOf("No trigger events recorded for $deviceName in this zone.")
        return rawLogs.map { log ->
            val status = log["status"]?.toString() ?: "EVENT"
            val ts = log["ts_start"]?.toString() ?: ""
            val formattedTime = if (ts.isNotEmpty()) {
                try {
                    val millis = ts.toLongOrNull() ?: 0L
                    if (millis > 0) java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(millis)) else ts
                } catch (e: Exception) { ts }
            } else "Recent"
            "🔔 [$formattedTime] $deviceName $status"
        }
    }

    fun deleteCloudSafeguardZone(id: String) {
        archiveSafeZone(id)
    }

    fun deleteGeofence(zoneId: String) {
        archiveSafeZone(zoneId)
    }
    
    fun toggleBellAlertState(id: String, currentState: Boolean) {
        val grp = currentGroupId ?: return
        val newState = !currentState
        FirebaseComm.relationshipsRef.child(grp).child("geofences").child(id).child("alertsEnabled").setValue(newState)
        FirebaseComm.getGeofencingSafeZoneRef(grp, id).child("alerts_enabled").setValue(newState)
        viewModelScope.launch(Dispatchers.IO) {
            dbDelegate.updateSafeZoneAlerts(id, newState)
        }
        _uiToastEvent.postValue(SingleEvent(if (newState) "🔔 Perimeter Alerts Enabled" else "🔕 Perimeter Alerts Muted"))
    }

    fun toggleZoneActiveState(id: String, currentState: Boolean) {
        val grp = currentGroupId ?: return
        val newState = !currentState
        FirebaseComm.relationshipsRef.child(grp).child("geofences").child(id).child("isZoneActive").setValue(newState)
        FirebaseComm.getGeofencingSafeZoneRef(grp, id).child("is_active").setValue(newState)
        viewModelScope.launch(Dispatchers.IO) {
            dbDelegate.updateSafeZoneActive(id, newState)
        }
        _uiToastEvent.postValue(SingleEvent(if (newState) "🟢 Safe Zone Monitoring Active" else "⏸️ Safe Zone Monitoring Paused"))
    }
    
    // 🚀 MISSION DEPLOYMENT HUB (V50: Targeted Forensic Handshake)

    fun sendForensicHandshakeRequest(targetName: String) {
        val activeId = currentGroupId ?: return
        val targetUid = deviceUidMap[targetName] ?: return
        
        val nowFormatted = java.text.SimpleDateFormat("dd/MM/yy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        
        val syncRequest = mapOf(
            "type" to "GEOFENCE_ASSIGNMENT",
            "zone_id" to "ACTIVE_MISSION",
            "zone_name" to "Active Mission Node",
            "group_id" to activeId,
            "status" to "PENDING_ACCEPTANCE",
            "msg" to "MISSION ALERT: Safe Zone sync dispatched at $nowFormatted. STATUS: WAITING FOR INTEGRATION...",
            "timestamp" to System.currentTimeMillis()
        )
        
        FirebaseComm.resolveHardwareIdentity(targetUid) { agentId ->
            val finalTargetId = agentId ?: targetUid
            FirebaseDatabase.getInstance().getReference(FirebaseComm.NODE_DEVICES)
                .child(finalTargetId)
                .child("Identity")
                .child("geofence_sync_requests")
                .push()
                .setValue(syncRequest)
        }
            
        // 💾 CLOSED-LOOP MASTER ARCHIVAL (L7 Standard): Update the local Master Tables
        val jPrefs = getApplication<android.app.Application>().getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE);
        val finalAgentId = jPrefs.getString("agent_id", FirebaseAuth.getInstance().getUid());
        val db = DatabaseHelper.getInstance(getApplication())
        
        // 1. Update SafeZoneGroup_Table (Master Mission Assignment)
        db.syncSafeZoneGroup("ACTIVE_MISSION", targetUid, 0, 100, "ALL", 1, 1, "MISSION_DEPLOYED")
        
        // 2. Update Pairing Log (Confirm Handshake in the TABLE_PAIRING)
        db.logDevicePairing(finalAgentId ?: "ADMIN", "MISSION_COMMANDER", "DEVICE_A",
                           targetUid, targetName, "DEVICE_B", "GUARDIAN", activeId)

        // 3. Log Forensic Deployment (History Archive)
        val missionMsg = "HANDSHAKE SENT: $nowFormatted. DELIVERY: IN_TRANSIT. RECEIVED: PENDING."
        db.saveGeofenceLog(targetUid, targetName, "MISSION_CONTROL", missionMsg, "DEPLOYED")

        _uiToastEvent.postValue(SingleEvent("Mission Deployment Sequence Transmitted to $targetName!"))
    }
    
    fun logManualGeofenceCheck(id: String, logString: String) {
        currentGroupId?.let { FirebaseComm.relationshipsRef.child(it).child("geofences").child(id).child("history").push().setValue(logString) }
    }

    fun fetchGeofenceHistory(id: String) {
        val activeId = currentGroupId ?: return
        val localLogs = dbDelegate.getGeofenceLogs(zoneId = id).map { 
            val status = it["status"]?.toString() ?: "EVENT"
            val transition = if (status.contains("ENTER") || status.equals("IN", ignoreCase = true)) "IN" else "OUT"
            "📅 [${it["date"] ?: "RECENT"}] • [${if (transition == "IN") "🟢 ENTERED (IN)" else "🔴 EXITED (OUT)"}] • ${it["id_full_name"] ?: "Device"} at ${it["readable_time"] ?: it["time"] ?: "Unknown"}"
        }

        // Query Date-Grouped notifications tree from /Geofencing/{creatorId}/safe_zones/{id}/connected_devices
        FirebaseComm.getGeofencingConnectedDevicesRef(activeId, id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val logs = LinkedHashSet<String>()
                    logs.addAll(localLogs)

                    for (devSnap in snapshot.children) {
                        val devName = devSnap.child("device_name").getValue(String::class.java) ?: devSnap.key ?: "Device"
                        val notifSnap = devSnap.child("notifications")
                        for (dateSnap in notifSnap.children) {
                            val dateKey = dateSnap.key ?: "RECENT"
                            for (evtSnap in dateSnap.children) {
                                val trans = evtSnap.child("transition").getValue(String::class.java) ?: "IN"
                                val timeStr = evtSnap.child("time").getValue(String::class.java) 
                                    ?: evtSnap.child("readable_time").getValue(String::class.java) 
                                    ?: "00:00"
                                val badge = if (trans.equals("IN", ignoreCase = true)) "🟢 ENTERED (IN)" else "🔴 EXITED (OUT)"
                                val line = "📅 [$dateKey] • [$badge] • $devName at $timeStr"
                                logs.add(line)
                            }
                        }
                    }
                    if (logs.isEmpty()) logs.add("🛡️ No perimeter trigger events recorded yet.")
                    _historyDialogEvent.postValue(SingleEvent(logs.reversed().toTypedArray()))
                }
                override fun onCancelled(error: DatabaseError) {
                    val fallback = if (localLogs.isNotEmpty()) localLogs.reversed().toTypedArray() else arrayOf("🛡️ No perimeter trigger events recorded yet.")
                    _historyDialogEvent.postValue(SingleEvent(fallback))
                }
            })
    }

    fun executeOSGeocodingQuery(query: String) {
        if (query.isEmpty()) return

        // 1. Direct Lat/Lng coordinate parsing support (e.g. "28.6139, 77.2090" or "28.6139 77.2090")
        val coordParts = query.trim().split(Regex("[,\\s]+"))
        if (coordParts.size == 2) {
            val lat = coordParts[0].toDoubleOrNull()
            val lng = coordParts[1].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                val target = LatLng(lat, lng)
                _cameraMoveEvent.postValue(SingleEvent(target))
                _draftCoordinateLocation.postValue(target)
                _uiToastEvent.postValue(SingleEvent("📍 Coordinate anchor set: $lat, $lng"))
                return
            }
        }

        val geocoder = Geocoder(getApplication())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    if (addresses.isNotEmpty()) {
                        val target = LatLng(addresses[0].latitude, addresses[0].longitude)
                        _cameraMoveEvent.postValue(SingleEvent(target))
                        _draftCoordinateLocation.postValue(target)
                        _uiToastEvent.postValue(SingleEvent("📍 Target pinned: ${addresses[0].locality ?: query}"))
                    } else {
                        _uiToastEvent.postValue(SingleEvent("Address '$query' not found. Tap directly on the map or tap the GPS button."))
                    }
                }
                override fun onError(errorMessage: String?) {
                    _uiToastEvent.postValue(SingleEvent("Geocoding lookup unavailable. Tap directly on the map."))
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
                    _cameraMoveEvent.postValue(SingleEvent(target))
                    _draftCoordinateLocation.postValue(target)
                    _uiToastEvent.postValue(SingleEvent("📍 Target location pinned!"))
                } else { 
                    _uiToastEvent.postValue(SingleEvent("Address '$query' not found. Tap directly on the map or tap the GPS button.")) 
                }
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
        geofenceListener?.let { listener ->
            currentGroupId?.let { gid ->
                FirebaseComm.relationshipsRef.child(gid).child("geofences").removeEventListener(listener)
            }
        }
    }
}
