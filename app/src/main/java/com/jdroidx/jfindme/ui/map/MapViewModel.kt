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
    val userLocations: LiveData<List<UserLocation>> get() = _filteredLocations
    
    // 🔒 Security Matrix: Restricts the visible universe to local verified family members only
    private val familyMeshIds = mutableSetOf<String>()
    
    private val _currentUserRole = MutableLiveData<String>("VIEWER")
    val currentUserRole: LiveData<String> get() = _currentUserRole

    init {
        SystemLogger.i(TAG, "MapViewModel initialized")
        repository.startListening()
        fetchCurrentUserRole()
        bindSecureFamilyMesh()
        
        repository.userLocations.observeForever { rawLocations ->
            val safeUid = auth.uid ?: return@observeForever
            // Force strict group isolation! Only draw remote users physically recorded in your exact Firebase ring!
            val authorized = rawLocations.filter { familyMeshIds.contains(it.userId) || it.userId == safeUid }
            _filteredLocations.postValue(authorized)
        }
    }
    
    private fun bindSecureFamilyMesh() {
        val safeUid = auth.uid ?: return
        val rootDb = com.google.firebase.database.FirebaseDatabase.getInstance().reference
        
        rootDb.child("users").child(safeUid).child("active_group").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val myGroupId = snapshot.getValue(String::class.java)
                if (myGroupId == null || myGroupId.isEmpty()) {
                    familyMeshIds.clear() // Standalone Sandbox
                    val currentRaw = repository.userLocations.value ?: return
                    val authorized = currentRaw.filter { it.userId == safeUid }
                    _filteredLocations.postValue(authorized)
                    return
                }
                
                // 🚀 QA Architecture Fix: Bypass Un-indexed 'orderByChild' Query!
                // We physically route direct to the Master Group Database node to retrieve exact Paired Hardware arrays!
                rootDb.child("groups").child(myGroupId).child("members").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(groupSnap: com.google.firebase.database.DataSnapshot) {
                        familyMeshIds.clear()
                        groupSnap.children.forEach { familyMeshIds.add(it.key ?: "") }
                        
                        // Retrigger the visual layer organically
                        val currentRaw = repository.userLocations.value ?: return
                        val authorized = currentRaw.filter { familyMeshIds.contains(it.userId) || it.userId == safeUid }
                        _filteredLocations.postValue(authorized)
                    }
                    override fun onCancelled(e: com.google.firebase.database.DatabaseError) {
                        SystemLogger.e(TAG, "Map HUD Sync Dropped", e.toException())
                    }
                })
            }
            override fun onCancelled(e: com.google.firebase.database.DatabaseError) {}
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
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        
        // 🧠 QA Request: Unified DB Pointer. Must strictly target the Main Profile Engine Storage!
        val prefs = context.getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE)
        val customName = prefs.getString("user_nickname", null)
        val userName = customName ?: currentUser.displayName ?: currentUser.email ?: "User"
        
        // Fix Firebase Broadcast Overwrite Bug: Must query the exact physical integers set by ProfileActivity!
        val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
        val dynamicIdx = if (rawBaseId % 22 == 0) 22 else rawBaseId % 22
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
            timestamp = System.currentTimeMillis()
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

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
