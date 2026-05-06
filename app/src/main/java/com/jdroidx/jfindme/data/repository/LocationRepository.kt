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
import android.content.SharedPreferences
import java.lang.StringBuilder

/**
 * LocationRepository manages real-time location data flow, offline synchronization,
 * and historical route fetching for the Replay Engine.
 */
class LocationRepository(internal val context: Context) {

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
            val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val locations = mutableListOf<UserLocation>()
            
            snapshot.children.forEach { child ->
                val agentId = child.key ?: return@forEach
                val liveNode = child.child("device/live")
                val rawLoc = liveNode.getValue(UserLocation::class.java) ?: return@forEach
                val loc = rawLoc.copy(userId = agentId)
                
                val resolvedId = agentId
                val isMember = (loc.active_group == myGroup && myGroup.isNotEmpty())
                val isLinked = allowedList.contains(resolvedId)
                val isFriend = friendList.contains(resolvedId)

                if (resolvedId == myUid || isMember || isLinked || isFriend) {
                    var profile: Map<String, String>? = null
                    try {
                        profile = db.getLocalDossier(resolvedId)
                    } catch (e: Exception) {
                        com.jdroidx.jfindme.utils.SystemLogger.e("LocationRepository", "Forensic Cache Access Blocked", e)
                    }
                    
                    val cloudName = loc.userName
                    
                    if (profile != null) {
                        loc.role_family = profile["role_family"] ?: "None"
                        loc.role_friend = profile["role_friend"] ?: "None"
                        if (cloudName.isEmpty()) {
                            loc.userName = profile["id_full_name"] ?: profile["full_name"] ?: "Asset Node"
                        }
                    } else if (cloudName.isEmpty()) {
                        loc.userName = "Asset Node"
                    }
                    
                    if (cloudName.isNotEmpty() && cloudName != profile?.get("id_full_name")) {
                        try {
                            val uid2 = profile?.get("uid_2") 
                            db.saveRosterNode(resolvedId, cloudName, "Active Tracker", "LIVE TARGET TRACKED", false, true, uid2 = uid2, qrCode = loc.avatar_id)
                        } catch (e: Exception) {
                            com.jdroidx.jfindme.utils.SystemLogger.e("LocationRepository", "Forensic Backfill Blocked", e)
                        }
                    }

                    locations.add(loc)
                }
            }
            _userLocations.postValue(locations)
        }

        override fun onCancelled(error: DatabaseError) {
            SystemLogger.e("LocationRepository", "Cloud Stream Aborted: ${error.message}")
        }
    }

    fun startListening() {
        val myAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)

        FirebaseComm.resolveHardwareIdentity(myAuthUid) { agentId ->
            val finalId = agentId ?: "LEGACY_NODE"
            
            FirebaseComm.devicesRef.addValueEventListener(locationListener)
            
            FirebaseComm.devicesRef.child(finalId).child("profile").child("active_group")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        val myGroup = snap.getValue(String::class.java) ?: ""
                        if (myGroup.isEmpty()) return
                        
                        prefs.edit().putString("active_group", myGroup).apply()
                        
                        FirebaseComm.devicesRef.orderByChild("profile/active_group").equalTo(myGroup)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(groupSnap: DataSnapshot) {
                                    val csv = StringBuilder(finalId).append(",")
                                    groupSnap.children.forEach { childSnap -> csv.append(childSnap.key).append(",") }
                                    prefs.edit().putString("linked_devices_csv", csv.toString()).apply()
                                }
                                override fun onCancelled(err: DatabaseError) {
                                    SystemLogger.e("LocationRepository", "Active Group Sync Failed: ${err.message}")
                                }
                            })
                    }
                    override fun onCancelled(err: DatabaseError) {
                        SystemLogger.e("LocationRepository", "Group Lookup Failed: ${err.message}")
                    }
                })
                
            FirebaseComm.devicesRef.child(finalId).child("profile").child("friend_matrix")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        val csv = StringBuilder()
                        snap.children.forEach { childSnap -> csv.append(childSnap.key).append(",") }
                        prefs.edit().putString("friend_matrix_csv", csv.toString()).apply()
                    }
                    override fun onCancelled(err: DatabaseError) {
                        SystemLogger.e("LocationRepository", "Friend Matrix Sync Failed: ${err.message}")
                    }
                })
        }
    }

    fun stopListening() {
        FirebaseComm.devicesRef.removeEventListener(locationListener)
    }

    fun updateMyLocation(userLocation: UserLocation) {
        val geocoder = Geocoder(context, Locale.getDefault())
        
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
            
            finalizeLocationPush(userLocation, humanAddress)
        }.start()
        
        offlineSyncManager.syncLocation(userLocation)
    }

    private fun finalizeLocationPush(userLocation: UserLocation, finalAddress: String?) {
        val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val enrichedLocation = userLocation.copy(address = finalAddress)

        db.saveHistoryPoint(
            enrichedLocation.getResolvedUserId(),
            enrichedLocation.getResolvedDeviceId(),
            enrichedLocation.latitude,
            enrichedLocation.longitude,
            enrichedLocation.speed,
            enrichedLocation.batteryLevel.toInt(),
            enrichedLocation.isSosActive,
            finalAddress,
            enrichedLocation.getResolvedTimestamp()
        )
        
        updateUserLocation(enrichedLocation)
    }

    fun updateUserLocation(userLocation: UserLocation) {
        val rId = userLocation.getResolvedUserId()
        if (rId.isNotEmpty()) {
            FirebaseComm.syncAnalyticalEvent(rId, userLocation)
        }
    }

    fun fetchHistory(userId: String, startTs: Long, endTs: Long, callback: (List<UserLocation>) -> Unit) {
        val startKey = FirebaseComm.getInvertedForensicKey(endTs)
        val endKey = FirebaseComm.getInvertedForensicKey(startTs)

        FirebaseComm.devicesRef.child(userId).child("history")
            .orderByKey()
            .startAt(startKey)
            .endAt(endKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val points = mutableListOf<UserLocation>()
                    Thread {
                        val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                        snapshot.children.forEach { child ->
                            child.getValue(UserLocation::class.java)?.let { point ->
                                points.add(point)
                                
                                db.saveHistoryPoint(
                                    userId,
                                    point.getResolvedDeviceId(),
                                    point.latitude,
                                    point.longitude,
                                    point.speed,
                                    point.batteryLevel.toInt(),
                                    point.isSosActive,
                                    null,
                                    point.getResolvedTimestamp()
                                )
                            }
                        }
                        Handler(Looper.getMainLooper()).post { callback(points) }
                    }.start()
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }
}
