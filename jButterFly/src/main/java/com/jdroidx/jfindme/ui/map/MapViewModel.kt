package com.jdroidx.jfindme.ui.map

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.repository.LocationRepository
import com.jdroidx.jfindme.data.repository.DeviceRepository
import com.jdroidx.jfindme.engine.location.GeohashHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.data.remote.FirebaseComm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * MapViewModel: Handles business logic for map tracking.
 * Updated to AndroidViewModel to provide application context to repository.
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MapViewModel"
    private val repository = LocationRepository(application)
    private val auth = FirebaseAuth.getInstance()
    
    private val _filteredLocations = MutableLiveData<List<UserLocation>>()
    val userLocations: LiveData<List<UserLocation>> = _filteredLocations
    
    private val _myLocation = MutableLiveData<Location>()
    val myLocation: LiveData<Location> = _myLocation
    
    private val _userDossier = MutableLiveData<Map<String, String>>()
    val userDossier: LiveData<Map<String, String>> = _userDossier
    
    // ≡ƒÅ╖∩╕Å Filter State Matrix
    private var currentFilterMode = "ALL" // Default
    private val rawLocationCache = mutableListOf<UserLocation>()
    private val deletedDeviceCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val pairedDeviceCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val roleDeviceCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    // ≡ƒÜÇ Geofence Sentinel Engine: Natively pushes dependent alerts back to the Guardian UI
    private val _geofenceAlertTrigger = MutableLiveData<String>()
    val geofenceAlertTrigger: LiveData<String> = _geofenceAlertTrigger

    private val _safeZones = MutableLiveData<List<com.jdroidx.jfindme.data.model.SafeZoneModel>>()
    val safeZones: LiveData<List<com.jdroidx.jfindme.data.model.SafeZoneModel>> = _safeZones

    // 🛡️ Security Matrix: Restricts the visible universe to local verified family members only
    private val familyMeshIds = mutableSetOf<String>()
    private var activeGroupAlertListener: com.google.firebase.database.ChildEventListener? = null
    private var activeAlertRef: com.google.firebase.database.DatabaseReference? = null
    private val activeValueListeners = mutableMapOf<com.google.firebase.database.Query, com.google.firebase.database.ValueEventListener>()
    
    private val _currentUserRole = MutableLiveData<String>("VIEWER")
    val currentUserRole: LiveData<String> get() = _currentUserRole

    private val _pendingSafeZoneRequestsCount = MutableLiveData<Int>(0)
    val pendingSafeZoneRequestsCount: LiveData<Int> = _pendingSafeZoneRequestsCount

    private var userLocationsObserver: androidx.lifecycle.Observer<List<UserLocation>>? = null

    init {
        SystemLogger.i(TAG, "MapViewModel initialized")
        syncLocalMeshIds()
        repository.startListening()
        fetchCurrentUserRole()
        bindSecureFamilyMesh()
        bindPendingRequestsListener()
        
        userLocationsObserver = androidx.lifecycle.Observer { rawLocations ->
            rawLocationCache.clear()
            rawLocationCache.addAll(rawLocations)
            applyTacticalFilter()
        }
        repository.userLocations.observeForever(userLocationsObserver!!)
        
        loadLocalDossier()
        loadSafeZones()
    }

    // [Variables] db, myAgentId, activeGroup, scopedIds, primeTarget, cleanId | [Constants] None | [Dependencies] DatabaseHelper, DeviceIdentityHelper, RegistryManager, SystemLogger | [Integration Points] applyTacticalFilter, MapActivity
    fun syncLocalMeshIds() {
        try {
            val db = DeviceRepository.getInstance(getApplication())
            val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(getApplication())
            val activeGroup = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(getApplication())
            familyMeshIds.clear()
            val scopedIds = db.getConnectedDeviceIds(myAgentId)
            for (rawId in scopedIds) {
                val clean = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(rawId)
                if (clean.isNotBlank() && !db.isDeviceDeleted(clean, activeGroup) && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(getApplication(), clean)) {
                    familyMeshIds.add(clean)
                }
            }
            // 🧸 PRIME OBJECTIVE: Dynamically anchor Child / Protected Ward target via RegistryManager
            val isHost = com.jdroidx.jfindme.utils.DevicePresentationResolver.isCircleHost(getApplication(), myAgentId)
            val primeTarget = com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(getApplication())
            if ((isHost || scopedIds.contains(primeTarget)) && primeTarget.isNotBlank() && !db.isDeviceDeleted(primeTarget, activeGroup) && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(getApplication(), primeTarget)) {
                familyMeshIds.add(primeTarget)
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed syncing local mesh IDs", e)
        }
    }

    private fun loadLocalDossier() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = DeviceRepository.getInstance(getApplication())
            val dossier = db.getLocalDossier(auth.uid ?: "")
            _userDossier.postValue(dossier)
        }
    }

    fun loadSafeZones() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = DeviceRepository.getInstance(getApplication())
            val localZones = db.getActiveSafeZones()
            if (localZones.isNotEmpty()) {
                _safeZones.postValue(localZones)
            }
            
            val currentGroupId = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(getApplication())

            FirebaseDatabase.getInstance().reference.child("safe_zones").child(currentGroupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        viewModelScope.launch(Dispatchers.IO) {
                            val cloudZones = mutableListOf<com.jdroidx.jfindme.data.model.SafeZoneModel>()
                            for (child in snapshot.children) {
                                try {
                                    val model = child.getValue(com.jdroidx.jfindme.data.model.SafeZoneModel::class.java)
                                    if (model != null && (model.lat != 0.0 || model.lng != 0.0 || model.isPolygon)) {
                                        if (model.zoneId.isEmpty()) model.zoneId = child.key ?: ""
                                        if (model.polygonPointsJson.isBlank()) {
                                            val polyRaw = child.child("polygon_points").value?.toString() ?: ""
                                            if (polyRaw.isNotBlank()) model.polygonPointsJson = polyRaw
                                        }
                                        if (model.dwellThresholdMinutes == 0) {
                                            val dwellRaw = child.child("dwell_minutes").value?.toString()?.toIntOrNull() ?: 0
                                            model.dwellThresholdMinutes = dwellRaw
                                        }
                                        cloudZones.add(model)
                                        db.saveSafeZoneDirect(
                                            model.zoneId, model.name, model.lat, model.lng,
                                            model.radius.toDouble(), null, model.assignedName,
                                            model.alertsEnabled, model.isZoneActive,
                                            false, model.polygonPointsJson, model.dwellThresholdMinutes
                                        )
                                    }
                                } catch (e: Exception) {}
                            }
                            if (cloudZones.isNotEmpty()) {
                                _safeZones.postValue(cloudZones)
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    fun refreshLocations() {
        deletedDeviceCache.clear()
        pairedDeviceCache.clear()
        roleDeviceCache.clear()
        syncLocalMeshIds()
        com.jdroidx.jfindme.engine.map.MultiMarkerEngine.invalidateRoleAndNameCaches()
        fetchCurrentUserRole()
        bindSecureFamilyMesh()
        loadLocalDossier()
        loadSafeZones()
        // 🛰️ Enterprise Matrix Refresh: Batch sync all nodes across SQLite and Cloud
        repository.forceBatchSyncAllNodes()
    }

    fun refreshAllData() {
        refreshLocations()
        triggerRefresh()
    }

    fun setFilterMode(mode: String) {
        this.currentFilterMode = mode
        applyTacticalFilter()
    }

    fun getCurrentFilterMode(): String = currentFilterMode

    /**
     * 🛰️ WATERFALL PRIORITY & CHRONOLOGY PIPELINE (SSOT)
     * Filters and sorts user locations strictly:
     * 1. Priority 1: isSosActive == true (Highest Emergency Priority)
     * 2. Priority 2: Newest GPS timestamp to Oldest (Descending)
     * 3. Excludes soft-deleted nodes (DatabaseHelper.isDeviceDeleted)
     * 4. Validates mesh authorization via Cloud Mesh or Local SQLite pairing
     */
    fun applyTacticalFilter(mode: String = currentFilterMode) {
        this.currentFilterMode = mode
        val safeUid = auth.uid ?: ""
        val selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(getApplication())
        val db = DeviceRepository.getInstance(getApplication())
        
        // Step 1: Baseline Group Mesh Isolation (Cloud Mesh OR Local SQLite Handshake)
        // [Variables] cleanSelfId, activeGroup, uid, cleanUid, isDel, isPair, isPrime, inMesh | [Constants] None | [Dependencies] DeviceIdentityHelper, DatabaseHelper, RegistryManager | [Integration Points] Tactical Map Ingestion Authorization
        val cleanSelfId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(selfAgentId)
        val activeGroup = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(getApplication())
        val authorized = rawLocationCache.filter { loc ->
            val uid = loc.getResolvedUserId()
            val cleanUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(uid)
            val isDel = deletedDeviceCache.getOrPut(uid) { 
                db.isDeviceDeleted(uid, activeGroup) || db.isDeviceDeleted(cleanUid, activeGroup) ||
                com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(getApplication(), uid) ||
                com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(getApplication(), cleanUid)
            }
            val isPair = pairedDeviceCache.getOrPut(uid) { 
                (!isDel) && (db.isDevicePaired(uid) || db.isDevicePaired(cleanUid)) 
            }
            val isHost = com.jdroidx.jfindme.utils.DevicePresentationResolver.isCircleHost(getApplication(), cleanSelfId)
            val isPrime = isChildOrPrimeTarget(uid, loc.role_family, loc.userName) || isChildOrPrimeTarget(cleanUid, loc.role_family, loc.userName)
            val inMesh = familyMeshIds.any { 
                val cleanMesh = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(it)
                cleanMesh.equals(cleanUid, ignoreCase = true) || it.equals(uid, ignoreCase = true)
            }

            val isAuthorizedPeer = if (isHost) {
                isPrime || inMesh || isPair || cleanUid.equals(cleanSelfId, ignoreCase = true) || (safeUid.isNotEmpty() && uid == safeUid)
            } else {
                cleanUid.equals(cleanSelfId, ignoreCase = true) || (safeUid.isNotEmpty() && uid == safeUid) || inMesh || isPair
            }

            !isDel && isAuthorizedPeer
        }
        
        // Step 2: User-Driven Tactical Filter Application
        // [Variables] uid, cleanUid, role | [Constants] "FAMILY", "FRIENDS", "None" | [Dependencies] roleDeviceCache, isChildOrPrimeTarget, DatabaseHelper | [Integration Points] MapActivity marker roster
        val filtered = when (currentFilterMode) {
            "FAMILY" -> authorized.filter { 
                val uid = it.getResolvedUserId()
                val cleanUid = uid.replace("HW_", "").trim()
                val role = roleDeviceCache.getOrPut(uid) { db.getRoleForDevice(uid) ?: "" }
                uid == selfAgentId || (safeUid.isNotEmpty() && uid == safeUid) || 
                    isChildOrPrimeTarget(uid, it.role_family, it.userName) ||
                    isChildOrPrimeTarget(cleanUid, it.role_family, it.userName) ||
                    (!it.role_family.equals("None", true) && it.role_family.isNotBlank()) ||
                    (!role.isNullOrBlank() && !role.contains("Friend", true)) ||
                    (role.isBlank() && (it.role_family.isBlank() || it.role_family.equals("None", true)))
            }
            "FRIENDS" -> authorized.filter { 
                val uid = it.getResolvedUserId()
                val role = roleDeviceCache.getOrPut(uid) { db.getRoleForDevice(uid) ?: "" }
                uid == selfAgentId || (safeUid.isNotEmpty() && uid == safeUid) || 
                    (!it.role_friend.equals("None", true) && it.role_friend.isNotBlank()) ||
                    (role.contains("Friend", true))
            }
            else -> authorized // ALL
        }.sortedWith(
            compareByDescending<com.jdroidx.jfindme.data.model.UserLocation> { it.isSosActive }
                .thenByDescending { isChildOrPrimeTarget(it.getResolvedUserId(), it.role_family, it.userName) }
                .thenByDescending { it.getLongTimestamp() }
        )
        
        _filteredLocations.postValue(filtered)
    }

    /**
     * 🧸 PRIME OBJECTIVE: Evaluates whether a peer node is a Child / Protected Ward target (dynamic SSOT via RegistryManager)
     */
    private fun isChildOrPrimeTarget(uid: String, roleFamily: String?, userName: String?): Boolean {
        val primeTarget = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(getApplication()))
        val cleanUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(uid)
        if (cleanUid.equals(primeTarget, ignoreCase = true)) return true
        val primeName = com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetName(getApplication())
        if (userName != null && primeName.isNotBlank() && userName.contains(primeName, ignoreCase = true)) return true
        val role = roleFamily ?: ""
        if (role.equals("Child", ignoreCase = true) ||
            role.equals("Daughter", ignoreCase = true) ||
            role.equals("Son", ignoreCase = true) ||
            role.equals("Ward", ignoreCase = true) ||
            role.equals("Dependent", ignoreCase = true) ||
            role.equals("DEPENDENT_CHILD", ignoreCase = true)) {
            return true
        }
        val db = DeviceRepository.getInstance(getApplication())
        val dbRole = roleDeviceCache.getOrPut(uid) { db.getRoleForDevice(uid) ?: "" }
        return dbRole.contains("Child", ignoreCase = true) ||
               dbRole.contains("Daughter", ignoreCase = true) ||
               dbRole.contains("Son", ignoreCase = true) ||
               dbRole.contains("Ward", ignoreCase = true) ||
               dbRole.contains("Dependent", ignoreCase = true)
    }
    
    private fun bindSecureFamilyMesh() {
        val safeUid = auth.uid ?: return
        val rootDb = com.google.firebase.database.FirebaseDatabase.getInstance().reference
        
                // 🚀 V67.2: Relational Bridge - Use Agent Hub, NOT Legacy users node
                FirebaseComm.resolveHardwareIdentity(safeUid) { agentId ->
                    val finalId = agentId ?: safeUid
                    val deviceRef = rootDb.child(FirebaseComm.NODE_DEVICES).child(finalId)
                    
                    activeValueListeners[deviceRef]?.let { deviceRef.removeEventListener(it) }

                    val deviceListener = object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            val myGroupId = snapshot.child("profile").child("active_group").getValue(String::class.java)
                            val linkCsvRaw = snapshot.child("profile").child("valid_linked_uids").getValue(String::class.java) ?: ""
                            
                            // Construct Explicit One-Scan-One-Device Allowed List
                            val approvedUids = linkCsvRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            
                            val friendMatrix = snapshot.child("profile").child("friend_matrix")
                            val explicitFriendUids = mutableSetOf<String>()
                            friendMatrix.children.forEach { explicitFriendUids.add(it.key ?: "") }
                            
                            // Seed mesh from local SQLite Roster (SSOT) - Host only
                            val isHost = com.jdroidx.jfindme.utils.DevicePresentationResolver.isCircleHost(getApplication(), finalId)
                            if (isHost) {
                                try {
                                    val db = DeviceRepository.getInstance(getApplication())
                                    val offlineRoster = db.getOfflineRoster()
                                    for (node in offlineRoster) {
                                        val u1 = node["uid_1"] as? String
                                        val u2 = node["uid_2"] as? String
                                        if (!u1.isNullOrBlank()) familyMeshIds.add(u1.trim())
                                        if (!u2.isNullOrBlank()) familyMeshIds.add(u2.trim())
                                    }
                                } catch (e: Exception) {
                                    SystemLogger.e(TAG, "Offline roster mesh seed error", e)
                                }
                            }

                            if (myGroupId == null || myGroupId.isEmpty()) {
                                familyMeshIds.addAll(explicitFriendUids) // 🚀 QA Fix: P2P Friendship Stability (V9.8)
                                syncLocalMeshIds()
                                applyTacticalFilter()
                                return
                            }
                            
                            val effectiveGroup = if (!myGroupId.isNullOrBlank() && myGroupId != "DEFAULT_GROUP") myGroupId else com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(getApplication())
                            
                            // 🚀 Strict Security: Only query real private group relationships, never global DEFAULT_GROUP
                            val relMembersRef = FirebaseComm.relationshipsRef.child(effectiveGroup)
                            activeValueListeners[relMembersRef]?.let { relMembersRef.removeEventListener(it) }

                            val relListener = object : com.google.firebase.database.ValueEventListener {
                                override fun onDataChange(groupSnap: com.google.firebase.database.DataSnapshot) {
                                    val newMesh = mutableSetOf<String>()
                                    newMesh.add(finalId)
                                    newMesh.add(safeUid)
                                    newMesh.addAll(explicitFriendUids)
                                    newMesh.addAll(approvedUids)

                                    val isHost = com.jdroidx.jfindme.utils.DevicePresentationResolver.isCircleHost(getApplication(), finalId)
                                    if (isHost) {
                                        // Add offline verified members from SQLite SSOT for Host only
                                        try {
                                            val db = DeviceRepository.getInstance(getApplication())
                                            for (node in db.getOfflineRoster()) {
                                                val u1 = node["uid_1"] as? String
                                                val u2 = node["uid_2"] as? String
                                                val connId = node["connected_id"] as? String
                                                if (!u1.isNullOrBlank()) newMesh.add(u1.trim())
                                                if (!u2.isNullOrBlank()) newMesh.add(u2.trim())
                                                if (!connId.isNullOrBlank()) newMesh.add(connId.trim())
                                            }
                                        } catch (e: Exception) { }

                                        // 1. Add verified circle devices from unified `devices/` node
                                        groupSnap.child("devices").children.forEach { devNode ->
                                            val dId = devNode.key ?: ""
                                            if (dId.isNotEmpty()) newMesh.add(dId)
                                        }

                                        // 2. Add verified cloud circle members from legacy `members/` node
                                        groupSnap.child("members").children.forEach { memberNode ->
                                            val mId = memberNode.key ?: ""
                                            if (mId.isNotEmpty()) newMesh.add(mId)
                                        }
                                    }

                                    familyMeshIds.addAll(newMesh)
                                    syncLocalMeshIds()
                                    applyTacticalFilter()
                                }
                                override fun onCancelled(e: com.google.firebase.database.DatabaseError) {
                                    SystemLogger.e(TAG, "Relationships Sync Dropped: ${e.message}")
                                }
                            }
                            relMembersRef.addValueEventListener(relListener)
                            activeValueListeners[relMembersRef] = relListener

                            // 🛡️ PHASE 2 QA: Listen strictly for Safe Zone Notifications & Perimeter Breaches (Live Only & Dormancy Latched)
                            val alertRef = FirebaseComm.relationshipsRef.child(myGroupId).child("notifications")
                            if (activeGroupAlertListener != null && activeAlertRef != null) {
                                activeAlertRef!!.removeEventListener(activeGroupAlertListener!!)
                            }
                            
                            val listenerAttachTime = System.currentTimeMillis()
                            val lastSeenZoneStateMap = mutableMapOf<String, String>()
                            
                            activeGroupAlertListener = object : com.google.firebase.database.ChildEventListener {
                                override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                                    val map = snapshot.value as? Map<*, *>
                                    val alertTs = snapshot.child("timestamp").value?.toString()?.toLongOrNull() ?: 0L
                                    
                                    // 🛑 SUPPRESS STALE ALERTS: Ignore alerts older than when the Map screen opened (5s buffer)
                                    if (alertTs > 0 && alertTs < listenerAttachTime - 5000L) {
                                        return
                                    }

                                    val avatar = map?.get("avatar_name")?.toString() ?: "Asset"
                                    val zone = map?.get("zone_name")?.toString() ?: "Safe Zone"
                                    val status = map?.get("status")?.toString() ?: "TRANSITION"
                                    val stateKey = "${avatar}_${zone}"
                                    
                                    // 🛑 DORMANCY LATCH: Do not pop HUD banner if already in this state
                                    if (lastSeenZoneStateMap[stateKey] == status) {
                                        SystemLogger.i(TAG, "GEOFENCE_HUD_DORMANT: State already rendered ($stateKey -> $status)")
                                        return
                                    }
                                    lastSeenZoneStateMap[stateKey] = status

                                    val timeMs = if (alertTs > 0) alertTs else System.currentTimeMillis()
                                    val dateStr = map?.get("date")?.toString() ?: java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(timeMs))
                                    val timeStr = map?.get("time")?.toString() ?: java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(timeMs))

                                    val alertMessage = if (map != null) {
                                        "Target: $avatar\nAction: $status Safe Zone '$zone'\n\n🕒 Date: $dateStr\n⏰ Time: $timeStr"
                                    } else {
                                        "${snapshot.getValue(String::class.java)}\n\n🕒 Date: $dateStr | Time: $timeStr"
                                    }
                                    
                                    SystemLogger.w(TAG, "Intercepted Live Geofence Breach: $alertMessage")
                                    _geofenceAlertTrigger.postValue(alertMessage)
                                }
                                override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
                                override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
                                override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
                                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                            }
                            
                            alertRef.limitToLast(5).addChildEventListener(activeGroupAlertListener!!)
                            activeAlertRef = alertRef

                            // 🛰️ V50: Sync Group SafeZone Overlays for Map Rendering
                            val geoRef = FirebaseComm.relationshipsRef.child(myGroupId).child("geofences")
                            val geoListener = object : com.google.firebase.database.ValueEventListener {
                                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                                    val list = mutableListOf<com.jdroidx.jfindme.data.model.SafeZoneModel>()
                                    for (child in snapshot.children) {
                                        val name = child.child("name").getValue(String::class.java) ?: child.key ?: ""
                                        val lat = child.child("lat").value?.toString()?.toDoubleOrNull() ?: 0.0
                                        val lng = child.child("lng").value?.toString()?.toDoubleOrNull() ?: 0.0
                                        val rad = child.child("radius").value?.toString()?.toFloatOrNull() ?: 100f
                                        val isActive = child.child("isZoneActive").getValue(Boolean::class.java) ?: true
                                        val polyJson = child.child("polygon_points").value?.toString() 
                                            ?: child.child("polygonPointsJson").value?.toString() ?: ""
                                        val dwellMin = child.child("dwell_minutes").value?.toString()?.toIntOrNull() 
                                            ?: child.child("dwellThresholdMinutes").value?.toString()?.toIntOrNull() ?: 0

                                        if (isActive && (lat != 0.0 || lng != 0.0 || polyJson.isNotBlank())) {
                                            list.add(com.jdroidx.jfindme.data.model.SafeZoneModel(
                                                zoneId = child.key ?: name,
                                                name = name,
                                                lat = lat,
                                                lng = lng,
                                                radius = rad,
                                                polygonPointsJson = polyJson,
                                                dwellThresholdMinutes = dwellMin
                                            ))
                                        }
                                    }
                                    _safeZones.postValue(list)
                                }
                                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                            }
                            geoRef.addValueEventListener(geoListener)
                            activeValueListeners[geoRef] = geoListener
                        }
                        override fun onCancelled(e: com.google.firebase.database.DatabaseError) {}
                    }
                    deviceRef.addValueEventListener(deviceListener)
                    activeValueListeners[deviceRef] = deviceListener
                }
        
        // 🚀 V66.7: P2P Strict Tracking Array Matrix! Ensure offline strictly paired 1-to-1 users ALSO load!
        val friendMatrixRef = FirebaseComm.getProfileRef(safeUid).child("friend_matrix")
        activeValueListeners[friendMatrixRef]?.let { friendMatrixRef.removeEventListener(it) }
        
        val friendMatrixListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                snapshot.children.forEach { familyMeshIds.add(it.key ?: "") }
                applyTacticalFilter()
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        friendMatrixRef.addValueEventListener(friendMatrixListener)
        activeValueListeners[friendMatrixRef] = friendMatrixListener
    }

    private fun fetchCurrentUserRole() {
        val safeUid = auth.uid ?: return
        FirebaseComm.resolveHardwareIdentity(safeUid) { agentId: String? ->
            val finalId = agentId ?: safeUid
            FirebaseComm.devicesRef.child(finalId).child("profile").get().addOnSuccessListener { snapshot ->
                val device = snapshot.getValue(com.jdroidx.jfindme.data.model.DeviceRegistry::class.java)
                _currentUserRole.postValue(device?.userRole ?: "VIEWER")
            }
        }
    }

    fun isAdmin(): Boolean = _currentUserRole.value == "ADMIN"

    fun updateMyLocation(context: Context, location: Location) {
        _myLocation.postValue(location)
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = auth.currentUser
            val selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            val userId = currentUser?.uid ?: selfAgentId
            
            // ≡ƒºá QA Request: Unified DB Pointer. Must strictly target the Main Profile Engine Storage!
            val db = DeviceRepository.getInstance(getApplication())
            val dossier = db.getLocalDossier(userId)
            val fullName = dossier["fullName"]
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val customName = prefs.getString("user_nickname", null)
            
            val userName = if (!fullName.isNullOrBlank()) fullName else (customName ?: currentUser?.displayName ?: currentUser?.email ?: "User Identity")
            
            // Fix Firebase Broadcast Overwrite Bug: Must strictly enforce positive integer modulo math mathematically.
            val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
            val genderStr = prefs.getString("user_gender", "Male") ?: "Male"
            
            // ≡ƒÜÇ QA Fix: Dynamic Gender-Based Proxy for Cloud Sync (V8 Fallback)
            val safeBaseId = if (rawBaseId == -1) {
                 if (genderStr.contains("Female", true)) 4 else 1 // Fallback to 'Student' (F) or 'Walker' (M)
            } else {
                 rawBaseId 
            }
            
            val dynamicIdx = if (safeBaseId % 22 == 0) 22 else Math.abs(safeBaseId % 22)
            val resolvedAvatarId = "avatar_custom_$dynamicIdx"
            
            val publish = { finalAgentId: String ->
                val userLocation = UserLocation(
                    userId = finalAgentId,
                    userName = userName,
                    avatar_id = resolvedAvatarId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    geohash = GeohashHelper.encode(location.latitude, location.longitude),
                    bearing = location.bearing,
                    speed = location.speed,
                    timestamp = System.currentTimeMillis(),
                    active_group = prefs.getString("active_group", "") ?: "",
                    role_family = prefs.getString("role_family", "None") ?: "None",
                    role_friend = prefs.getString("role_friend", "None") ?: "None"
                )
                repository.updateMyLocation(userLocation)
            }

            if (currentUser != null) {
                FirebaseComm.resolveHardwareIdentity(currentUser.uid) { agentId: String? ->
                    val finalAgentId = agentId ?: selfAgentId
                    publish(finalAgentId)
                }
            } else {
                publish(selfAgentId)
            }
        }
    }

    fun toggleSOS(context: Context, state: Boolean) {
        val selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val currentLocs = userLocations.value ?: emptyList()
        val myLoc = currentLocs.find { it.userId == selfAgentId || it.userId == auth.currentUser?.uid }

        val intentFilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1

        val updatedLoc = (myLoc ?: UserLocation(userId = selfAgentId)).copy(
            userId = selfAgentId,
            isSosActive = state,
            batteryLevel = level,
            timestamp = System.currentTimeMillis()
        )
        repository.updateMyLocation(updatedLoc)
        val sirenDur = com.jdroidx.jfindme.utils.RegistryManager.getSirenDurationSec(context)
        com.jdroidx.jfindme.data.remote.FirebaseComm.broadcastEmergencySos(context, sirenDur)
    }

    fun pokeUser(targetUserId: String, myName: String) {
        val currentLocs = userLocations.value ?: return
        val targetLoc = currentLocs.find { it.userId == targetUserId } ?: return
        
        val updatedLoc = targetLoc.copy(
            isPoked = true,
            pokedBy = myName,
            timestamp = System.currentTimeMillis()
        )
        repository.updateUserLocation(updatedLoc)
    }

    fun clearPoke() {
        val currentUser = auth.currentUser ?: return
        val currentLocs = userLocations.value ?: return
        val myLoc = currentLocs.find { it.userId == currentUser.uid } ?: return
        
        if (myLoc.isPoked) {
            val updatedLoc = myLoc.copy(
                isPoked = false,
                pokedBy = "",
                timestamp = System.currentTimeMillis()
            )
            repository.updateMyLocation(updatedLoc)
        }
    }

    fun stopListening() {
        repository.stopListening()
    }

    private fun bindPendingRequestsListener() {
        val safeUid = auth.uid ?: return
        val rootDb = FirebaseDatabase.getInstance().reference
        FirebaseComm.resolveHardwareIdentity(safeUid) { agentId: String? ->
            val finalId = agentId ?: safeUid
            val ref = rootDb.child(FirebaseComm.NODE_DEVICES).child(finalId).child("Identity").child("geofence_sync_requests")
            
            activeValueListeners[ref]?.let { ref.removeEventListener(it) }
            
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var count = 0
                    for (snap in snapshot.children) {
                        val status = snap.child("status").getValue(String::class.java)
                        if ("PENDING_ACCEPTANCE" == status) {
                            count++
                        }
                    }
                    _pendingSafeZoneRequestsCount.postValue(count)
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            activeValueListeners[ref] = listener
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
        
        userLocationsObserver?.let {
            repository.userLocations.removeObserver(it)
        }
        userLocationsObserver = null
        
        activeValueListeners.forEach { (query, listener) ->
            query.removeEventListener(listener)
        }
        activeValueListeners.clear()
        
        if (activeGroupAlertListener != null && activeAlertRef != null) {
            activeAlertRef!!.removeEventListener(activeGroupAlertListener!!)
        }
    }

    // ≡ƒÜÇ Refresh trigger for Callouts (Can be observed by Activity)
    private val _refreshTrigger = MutableLiveData<Long>()
    val refreshTrigger: LiveData<Long> = _refreshTrigger

    @kotlin.jvm.JvmOverloads
    fun triggerRefresh(time: Long = System.currentTimeMillis()) {
        _refreshTrigger.value = time
    }
    
    // 🚀 OOPS MVC SSOT: Centralized Ghost Mode Sync via RegistryManager!
    // [Variables] h, context, newVal
    // [Constants] RegistryManager.KEY_CALLOUT_HIDE_MODE
    // [Dependencies] RegistryManager, SystemLogger
    // [Integration Points] MapActivity, MapEngine
    fun toggleGhostMode(context: android.content.Context) {
        val newVal = com.jdroidx.jfindme.utils.RegistryManager.toggleCalloutHideMode(context)
        SystemLogger.i(TAG, "Ghost Mode Toggled: $newVal")
        triggerRefresh()
    }

    @Deprecated("Use toggleGhostMode(context) instead to enforce RegistryManager SSOT")
    fun toggleGhostMode(prefs: android.content.SharedPreferences) {
        val h = prefs.getBoolean(com.jdroidx.jfindme.utils.RegistryManager.KEY_CALLOUT_HIDE_MODE, false)
        prefs.edit().putBoolean(com.jdroidx.jfindme.utils.RegistryManager.KEY_CALLOUT_HIDE_MODE, !h).apply()
        SystemLogger.i(TAG, "Ghost Mode Toggled: ${!h}")
        triggerRefresh()
    }

}
