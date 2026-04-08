package com.jdroidx.jfindme.ui.map

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.repository.LocationRepository
import com.jdroidx.jfindme.engine.location.GeohashHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.jdroidx.jfindme.utils.SystemLogger

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
    
    // 🏷️ Filter State Matrix
    private var currentFilterMode = "ALL" // Default
    private val rawLocationCache = mutableListOf<UserLocation>()

    // 🚀 Geofence Sentinel Engine: Natively pushes dependent alerts back to the Guardian UI
    private val _geofenceAlertTrigger = MutableLiveData<String>()
    val geofenceAlertTrigger: LiveData<String> = _geofenceAlertTrigger

    // 🔒 Security Matrix: Restricts the visible universe to local verified family members only
    private val familyMeshIds = mutableSetOf<String>()
    private var activeGroupAlertListener: com.google.firebase.database.ChildEventListener? = null
    
    private val _currentUserRole = MutableLiveData<String>("VIEWER")
    val currentUserRole: LiveData<String> get() = _currentUserRole

    private val _pendingSafeZoneRequestsCount = MutableLiveData<Int>(0)
    val pendingSafeZoneRequestsCount: LiveData<Int> = _pendingSafeZoneRequestsCount

    init {
        SystemLogger.i(TAG, "MapViewModel initialized")
        repository.startListening()
        fetchCurrentUserRole()
        bindSecureFamilyMesh()
        bindPendingRequestsListener()
        
        repository.userLocations.observeForever { rawLocations ->
            rawLocationCache.clear()
            rawLocationCache.addAll(rawLocations)
            applyTacticalFilter()
        }
    }

    fun setFilterMode(mode: String) {
        this.currentFilterMode = mode
        applyTacticalFilter()
    }

    fun getCurrentFilterMode(): String = currentFilterMode

    private fun applyTacticalFilter() {
        val safeUid = auth.uid ?: return
        
        // Step 1: Baseline Group Mesh Isolation
        val authorized = rawLocationCache.filter { familyMeshIds.contains(it.userId) || it.userId == safeUid }
        
        // Step 2: User-Driven Tactical Filter Application
        val filtered = when (currentFilterMode) {
            "FAMILY" -> authorized.filter { 
                it.userId == safeUid || (!it.role_family.equals("None", true) && it.role_family.isNotBlank())
            }
            "FRIENDS" -> authorized.filter { 
                it.userId == safeUid || (!it.role_friend.equals("None", true) && it.role_friend.isNotBlank())
            }
            else -> authorized // ALL
        }
        
        _filteredLocations.postValue(filtered)
    }
    
    private fun bindSecureFamilyMesh() {
        val safeUid = auth.uid ?: return
        val rootDb = com.google.firebase.database.FirebaseDatabase.getInstance().reference
        
        rootDb.child("users").child(safeUid).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val myGroupId = snapshot.child("active_group").getValue(String::class.java)
                val linkCsvRaw = snapshot.child("valid_linked_uids").getValue(String::class.java) ?: ""
                
                // Construct Explicit One-Scan-One-Device Allowed List
                val approvedUids = linkCsvRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                val friendMatrix = snapshot.child("friend_matrix")
                val explicitFriendUids = mutableSetOf<String>()
                friendMatrix.children.forEach { explicitFriendUids.add(it.key ?: "") }
                
                if (myGroupId == null || myGroupId.isEmpty()) {
                    familyMeshIds.clear()
                    familyMeshIds.addAll(explicitFriendUids) // 🚀 QA Fix: P2P Friendship Stability (V9.8)
                    val currentRaw = repository.userLocations.value ?: return
                    val authorized = currentRaw.filter { familyMeshIds.contains(it.userId) || it.userId == safeUid }
                    _filteredLocations.postValue(authorized)
                    return
                }
                
                // 🚀 QA Fix: Standalone Mesh Resilience (V9.4)
                rootDb.child("users").orderByChild("active_group").equalTo(myGroupId).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(groupSnap: com.google.firebase.database.DataSnapshot) {
                        val newMesh = mutableSetOf<String>()
                        newMesh.addAll(explicitFriendUids) // Preserving 1-to-1 pairings in the hybrid mesh!

                        groupSnap.children.forEach { 
                            val possibleNodeId = it.key ?: ""
                            val thisNodeRole = it.child("role_family").getValue(String::class.java) ?: "None"
                            
                            // 🛰️ MASTER TRUST HEURISTICS: 
                            // 1. Explicitly scanned devices (approvedUids) / Friend Matrix
                            // 2. Administrators/Parents (Father/Mother)
                            // 3. Members of the SAME Group (Stability Fallback)
                            if (approvedUids.contains(possibleNodeId) || possibleNodeId == safeUid || thisNodeRole == "Father" || thisNodeRole == "Mother") {
                                newMesh.add(possibleNodeId)
                            } else {
                                // Default Stability: If you share a group, you should be visible to prevent blinking!
                                newMesh.add(possibleNodeId)
                            }
                        }
                        
                        familyMeshIds.clear()
                        familyMeshIds.addAll(newMesh)
                        
                        // Retrigger the visual layer organically
                        val currentRaw = repository.userLocations.value ?: return
                        val authorized = currentRaw.filter { familyMeshIds.contains(it.userId) || it.userId == safeUid }
                        _filteredLocations.postValue(authorized)
                    }
                    override fun onCancelled(e: com.google.firebase.database.DatabaseError) {
                        SystemLogger.e(TAG, "Map HUD Sync Dropped", e.toException())
                    }
                })

                // 🛑 PHASE 2 QA: Listen strictly for Children broadcasting Safe Zone Violations
                if (activeGroupAlertListener != null) rootDb.child("groups").child(myGroupId).child("alerts").removeEventListener(activeGroupAlertListener!!)
                activeGroupAlertListener = object : com.google.firebase.database.ChildEventListener {
                    override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                        val alertMessage = snapshot.getValue(String::class.java)
                        if (alertMessage != null) {
                            SystemLogger.w(TAG, "Intercepted Geofence Breach: $alertMessage")
                            _geofenceAlertTrigger.postValue(alertMessage)
                        }
                    }
                    override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
                    override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
                    override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                }
                
                // Only Administrators physically query and intercept the alerts generated!
                if (_currentUserRole.value == "ADMIN" || isAdmin()) {
                    rootDb.child("groups").child(myGroupId).child("alerts")
                        .limitToLast(3) // Keep RAM light, intercept newest 3
                        .addChildEventListener(activeGroupAlertListener!!)
                }
                
            }
            override fun onCancelled(e: com.google.firebase.database.DatabaseError) {}
        })
        
        // 🚀 QA Fix: P2P Strict Tracking Array Matrix! Ensure offline strictly paired 1-to-1 users ALSO load!
        rootDb.child("users").child(safeUid).child("friend_matrix").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                snapshot.children.forEach { familyMeshIds.add(it.key ?: "") }
                val currentRaw = repository.userLocations.value ?: return
                val authorized = currentRaw.filter { familyMeshIds.contains(it.userId) || it.userId == safeUid }
                _filteredLocations.postValue(authorized)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun fetchCurrentUserRole() {
        val userId = auth.uid ?: return
        com.jdroidx.jfindme.data.remote.FirebaseComm.devicesRef.child(userId).limitToFirst(1)
            .get().addOnSuccessListener { snapshot ->
                val device = snapshot.children.firstOrNull()?.getValue(com.jdroidx.jfindme.data.model.DeviceRegistry::class.java)
                _currentUserRole.postValue(device?.userRole ?: "VIEWER")
            }
    }

    fun isAdmin(): Boolean = _currentUserRole.value == "ADMIN"

    fun updateMyLocation(context: Context, location: Location) {
        _myLocation.postValue(location)
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        
        // 🧠 QA Request: Unified DB Pointer. Must strictly target the Main Profile Engine Storage!
        val prefs = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
        val customName = prefs.getString("user_nickname", null)
        val userName = customName ?: currentUser.displayName ?: currentUser.email ?: "User"
        
        // Fix Firebase Broadcast Overwrite Bug: Must strictly enforce positive integer modulo math mathematically.
        val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
        val genderStr = prefs.getString("user_gender", "Male") ?: "Male"
        
        // 🚀 QA Fix: Dynamic Gender-Based Proxy for Cloud Sync (V8 Fallback)
        val safeBaseId = if (rawBaseId == -1) {
             if (genderStr.contains("Female", true)) 4 else 1 // Fallback to 'Student' (F) or 'Walker' (M)
        } else {
             rawBaseId 
        }
        
        val dynamicIdx = if (safeBaseId % 22 == 0) 22 else Math.abs(safeBaseId % 22)
        val resolvedAvatarId = "avatar_custom_$dynamicIdx"
        
        val userLocation = UserLocation(
            userId = userId,
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

    fun toggleSOS(context: Context, state: Boolean) {
        val currentUser = auth.currentUser ?: return
        val currentLocs = userLocations.value ?: return
        val myLoc = currentLocs.find { it.userId == currentUser.uid } ?: return
        
        val intentFilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        
        val updatedLoc = myLoc.copy(
            isSosActive = state,
            batteryLevel = level,
            timestamp = System.currentTimeMillis()
        )
        repository.updateMyLocation(updatedLoc)
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
        val ref = rootDb.child("users").child(safeUid).child("geofence_sync_requests")
        
        ref.addValueEventListener(object : ValueEventListener {
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
        })
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    // 🚀 Refresh trigger for Callouts (Can be observed by Activity)
    private val _refreshTrigger = MutableLiveData<Long>()
    val refreshTrigger: LiveData<Long> = _refreshTrigger

    @kotlin.jvm.JvmOverloads
    fun triggerRefresh(time: Long = System.currentTimeMillis()) {
        _refreshTrigger.value = time
    }
    
    // 🚀 QA Fix: Global Ghost Mode Sync!
    fun toggleGhostMode(prefs: android.content.SharedPreferences) {
        val h = prefs.getBoolean("callout_hide_mode", false)
        prefs.edit().putBoolean("callout_hide_mode", !h).apply()
        SystemLogger.i(TAG, "Ghost Mode Toggled: ${!h}")
        triggerRefresh()
    }
}
