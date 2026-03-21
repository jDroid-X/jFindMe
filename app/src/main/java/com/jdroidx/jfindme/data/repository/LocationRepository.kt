package com.jdroidx.jfindme.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.data.remote.OfflineSyncManager

/**
 * LocationRepository manages real-time location data flow, offline synchronization,
 * and historical route fetching for the Replay Engine.
 */
class LocationRepository(context: Context) {

    private val offlineSyncManager = OfflineSyncManager(context)
    private val _userLocations = MutableLiveData<List<UserLocation>>()
    val userLocations: LiveData<List<UserLocation>> get() = _userLocations

    private val locationListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val locations = mutableListOf<UserLocation>()
            snapshot.children.forEach { child ->
                child.getValue(UserLocation::class.java)?.let { locations.add(it) }
            }
            _userLocations.postValue(locations)
        }

        override fun onCancelled(error: DatabaseError) {
            // Log error
        }
    }

    fun startListening() {
        FirebaseComm.locationsRef.addValueEventListener(locationListener)
    }

    fun stopListening() {
        FirebaseComm.locationsRef.removeEventListener(locationListener)
    }

    /**
     * Strategic Update: Integration with OfflineSyncManager (Production Hardening)
     */
    fun updateMyLocation(userLocation: UserLocation) {
        offlineSyncManager.syncLocation(userLocation)
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
                    snapshot.children.forEach { child ->
                        child.getValue(UserLocation::class.java)?.let { points.add(it) }
                    }
                    callback(points)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }
}
