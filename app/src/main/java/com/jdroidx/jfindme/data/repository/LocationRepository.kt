package com.jdroidx.jfindme.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.utils.SystemLogger
import java.util.*
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.data.remote.OfflineSyncManager

/**
 * LocationRepository manages real-time location data flow, offline synchronization,
 * and historical route fetching for the Replay Engine.
 */
class LocationRepository(private val context: Context) {

    private val offlineSyncManager = OfflineSyncManager(context)
    private val _userLocations = MutableLiveData<List<UserLocation>>()
    val userLocations: LiveData<List<UserLocation>> get() = _userLocations

    private val locationListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val validUids = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getString("linked_devices_csv", "") ?: ""
            val friendUids = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getString("friend_matrix_csv", "") ?: ""
            val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "null_u"
            val myGroup = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getString("active_group", "") ?: ""
            val allowedList = validUids.split(",").map { it.trim() }
            val friendList = friendUids.split(",").map { it.trim() }
            val locations = mutableListOf<UserLocation>()
            
            snapshot.children.forEach { child ->
                child.getValue(UserLocation::class.java)?.let { loc ->
                    val isMember = (loc.active_group == myGroup && myGroup.isNotEmpty())
                    val isLinked = allowedList.contains(loc.userId)
                    val isFriend = friendList.contains(loc.userId)

                    if (loc.userId == myUid || isMember || isLinked || isFriend) {
                        locations.add(loc)
                    }
                }
            }
            _userLocations.postValue(locations)
        }

        override fun onCancelled(error: DatabaseError) {
            // Log error
        }
    }

    fun startListening() {
        FirebaseComm.locationsRef.addValueEventListener(locationListener)
        
        // 🚀 QA Fix: Global Security Synchronizer! Automatically maintains explicit connection array in the background.
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(myUid).child("active_group")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val myGroup = snap.getValue(String::class.java)
                    if (myGroup.isNullOrEmpty()) return
                    
                    // 🚀 QA Fix: Sync the active group to Prefs for the Location Filter!
                    prefs.edit().putString("active_group", myGroup).apply()
                    
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").orderByChild("active_group").equalTo(myGroup)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(groupSnap: DataSnapshot) {
                                val csv = java.lang.StringBuilder(myUid).append(",")
                                groupSnap.children.forEach { childSnap -> csv.append(childSnap.key).append(",") }
                                prefs.edit().putString("linked_devices_csv", csv.toString()).apply()
                            }
                            override fun onCancelled(err: DatabaseError) {}
                        })
                }
                override fun onCancelled(err: DatabaseError) {}
            })
            
        // 🚀 QA Fix: Also synchronize the strict 1-to-1 friend_matrix pairings!
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(myUid).child("friend_matrix")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    // We append to the existing CSV locally. 
                    // To prevent overwriting active_group, we fetch current, but must be careful of timing.
                    // Instead of complex locking, we just maintain a dedicated preference for isolated devices.
                    val csv = java.lang.StringBuilder()
                    snap.children.forEach { childSnap -> csv.append(childSnap.key).append(",") }
                    prefs.edit().putString("friend_matrix_csv", csv.toString()).apply()
                }
                override fun onCancelled(err: DatabaseError) {}
            })
    }

    fun stopListening() {
        FirebaseComm.locationsRef.removeEventListener(locationListener)
    }

    /**
     * Strategic Update: Integration with OfflineSyncManager (Production Hardening)
     */
    /**
     * Strategic Update: Integration with OfflineSyncManager (Production Hardening)
     */
    fun updateMyLocation(userLocation: UserLocation) {
        // 🚀 QA Fix: Resolve Physical Address before Forensic Commit
        val geocoder = Geocoder(context, Locale.getDefault())
        val db = com.jdroidx.jfindme.data.local.DatabaseHelper(context)
        
        // 🧱 QA Hardening: Use a Background Thread or Async Listener to prevent ANR
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(userLocation.latitude, userLocation.longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    val humanAddress = if (addresses.isNotEmpty()) addresses[0].getAddressLine(0) else null
                    finalizeLocationPush(userLocation, humanAddress)
                }
                override fun onError(errorMessage: String?) {
                    SystemLogger.e("LocationRepository", "Forensic Geocoding Blocked: $errorMessage")
                    finalizeLocationPush(userLocation, null)
                }
            })
        } else {
            Thread {
                var humanAddress: String? = null
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(userLocation.latitude, userLocation.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        humanAddress = addresses[0].getAddressLine(0)
                    }
                } catch (e: Exception) {
                    SystemLogger.e("LocationRepository", "Forensic Geocoding Blocked: ${e.message}")
                }
                
                Handler(Looper.getMainLooper()).post {
                    finalizeLocationPush(userLocation, humanAddress)
                }
            }.start()
        }
        
        offlineSyncManager.syncLocation(userLocation)
    }

    private fun finalizeLocationPush(userLocation: UserLocation, finalAddress: String?) {
        val db = com.jdroidx.jfindme.data.local.DatabaseHelper(context)
        val rel = if (userLocation.role_family != "None") "FAMILY (${userLocation.role_family})"
                  else if (userLocation.role_friend != "None") "FRIEND (${userLocation.role_friend})"
                  else "UNLINKED"

        db.saveHistoryPoint(
            userLocation.userId,
            userLocation.userName,
            rel,
            userLocation.latitude,
            userLocation.longitude,
            userLocation.speed,
            userLocation.batteryLevel.toInt(),
            userLocation.isSosActive,
            finalAddress
        )
    }

    fun updateUserLocation(userLocation: UserLocation) {
        FirebaseComm.updateMyLocation(userLocation)
    }

    /**
     * History Engine: Fetches points for a specific date range.
     */
    fun fetchHistory(userId: String, startTs: Long, endTs: Long, callback: (List<UserLocation>) -> Unit) {
        FirebaseComm.historyRef.child(userId)
            .orderByKey()
            .startAt(startTs.toString())
            .endAt(endTs.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val points = mutableListOf<UserLocation>()
                    val db = com.jdroidx.jfindme.data.local.DatabaseHelper(context)
                    
                    snapshot.children.forEach { child ->
                        child.getValue(UserLocation::class.java)?.let { point ->
                            points.add(point)
                            
                            // 🚀 QA Fix: Sync fetched cloud segments into local Forensic Vault
                            // This ensures the Factory Reset matrix shows data even for remote targets!
                            db.saveHistoryPoint(
                                userId,
                                point.userName,
                                "CLOUD_SYNC",
                                point.latitude,
                                point.longitude,
                                point.speed,
                                point.batteryLevel.toInt(),
                                point.isSosActive,
                                null
                            )
                        }
                    }
                    callback(points)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }
}

