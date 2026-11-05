package com.jdroidx.jfindme.ui.management

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jdroidx.jfindme.data.local.DatabaseHelper
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.content.Context
import kotlinx.coroutines.launch

data class GeofenceNotification(
    val id: String = "",
    val avatarName: String = "",
    val status: String = "", // "IN" or "OUT"
    val timestamp: Long = 0,
    val zoneName: String = "",
    val readableTime: String = "",
    val date: String = "",
    val time: String = ""
)

/**
 * GeofenceNotificationViewModel: Reactive Logic for Transition Reporting Hub.
 * Hybrid Mode: Prioritizes V42 Forensic Local Database for analytic integrity.
 */
class GeofenceNotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseHelper.getInstance(application)
    private val _notifications = MutableLiveData<List<GeofenceNotification>>()
    val notifications: LiveData<List<GeofenceNotification>> = _notifications
    
    private var cloudListener: ValueEventListener? = null
    private var geofencingTreeListener: ValueEventListener? = null

    init {
        fetchNotifications()
        setupCloudSync()
    }

    private val processedNodeKeys = mutableSetOf<String>()

    private fun setupCloudSync() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        // 🛰️ MISSION ARCHITECTURE: Listen to Group Cloud Matrix for Node Transitions
        val prefs = getApplication<Application>().getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val groupId = prefs.getString("active_group", null) ?: return
        
        if (groupId.startsWith("LOCAL_SANDBOX")) return
        val cleanGroupId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(groupId)

        // Stream 1: Relationship Notifications Node
        val ref = com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef.child(cleanGroupId).child("notifications")
        cloudListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    var hasNewLogs = false
                    snapshot.children.forEach { node ->
                        val nodeKey = node.key ?: ""
                        if (nodeKey.isNotEmpty() && !processedNodeKeys.contains(nodeKey)) {
                            processedNodeKeys.add(nodeKey)
                            val avatar = node.child("avatar_name").getValue(String::class.java) ?: "Unknown"
                            val zone = node.child("zone_name").getValue(String::class.java) ?: "Unknown"
                            val status = node.child("status").getValue(String::class.java) ?: "OUT"
                            val time = node.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val uid = node.child("uid").getValue(String::class.java) ?: "REMOTE_NODE"
                            
                            // 💾 ANALYTIC ARCHIVAL: Deduplicated safe archival to local SQLite
                            db.saveGeofenceLog(uid, avatar, zone, zone, status, 0.0, 0.0)
                            hasNewLogs = true
                        }
                    }
                    if (hasNewLogs) fetchNotifications()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Stream 2: Dedicated /Geofencing/{creatorDeviceCode}/safe_zones/ tree
        val hostCode = prefs.getString("agent_id", null)
            ?: db.resolveAgentIdLocally(com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "")
            ?: cleanGroupId
        val cleanHostCode = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(hostCode)
        val geofencingRef = com.jdroidx.jfindme.data.remote.FirebaseComm.getGeofencingSafeZonesRef(cleanHostCode)
        geofencingTreeListener = geofencingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    var hasNewLogs = false
                    for (zoneSnap in snapshot.children) {
                        val zoneName = zoneSnap.child("zone_name").getValue(String::class.java) ?: zoneSnap.key ?: "Zone"
                        val devicesSnap = zoneSnap.child("connected_devices")
                        for (devSnap in devicesSnap.children) {
                            val devUid = devSnap.key ?: "REMOTE"
                            val devName = devSnap.child("device_name").getValue(String::class.java) ?: devUid
                            val notifSnap = devSnap.child("notifications")
                            for (dateSnap in notifSnap.children) {
                                for (evtSnap in dateSnap.children) {
                                    val evtKey = evtSnap.key ?: ""
                                    if (evtKey.isNotEmpty() && !processedNodeKeys.contains(evtKey)) {
                                        processedNodeKeys.add(evtKey)
                                        val transition = evtSnap.child("transition").getValue(String::class.java) ?: "EVENT"
                                        val status = if (transition.contains("ENTER") || transition.equals("IN", ignoreCase = true)) "ENTERED" else "EXITED"
                                        val rawZone = evtSnap.child("zone_name").getValue(String::class.java) ?: zoneName
                                        db.saveGeofenceLog(devUid, devName, rawZone, rawZone, status, 0.0, 0.0)
                                        hasNewLogs = true
                                    }
                                }
                            }
                        }
                    }
                    if (hasNewLogs) fetchNotifications()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun fetchNotifications(targetAvatar: String? = null, zoneId: String? = null) {
        val logs = db.getGeofenceLogs(targetAvatar, zoneId)
        val list = logs.map { map ->
            val ts = (map["ts_start"] as? Long) ?: 0L
            val dateStr = map["date"]?.toString() ?: if (ts > 0) java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ts)) else ""
            val timeStr = map["time"]?.toString() ?: if (ts > 0) java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(ts)) else ""
            GeofenceNotification(
                id = map["log_id"].toString(),
                avatarName = map["fullName"]?.toString() ?: "Unknown",
                status = map["status"]?.toString() ?: "OUT",
                timestamp = ts,
                zoneName = map["zone_name"]?.toString() ?: "Unknown",
                readableTime = map["readable_time"]?.toString() ?: "",
                date = dateStr,
                time = timeStr
            )
        }
        _notifications.postValue(list)
    }

    fun clearAllLogs(targetAvatar: String? = null, zoneId: String? = null) {
        viewModelScope.launch {
            db.clearGeofenceLogs()
            fetchNotifications(targetAvatar, zoneId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val prefs = getApplication<Application>().getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val groupId = prefs.getString("active_group", null) ?: return
        val cleanGroupId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(groupId)
        val hostCode = prefs.getString("agent_id", null)
            ?: db.resolveAgentIdLocally(com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "")
            ?: cleanGroupId
        val cleanHostCode = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(hostCode)
        cloudListener?.let {
            com.jdroidx.jfindme.data.remote.FirebaseComm.relationshipsRef.child(cleanGroupId).child("notifications").removeEventListener(it)
        }
        geofencingTreeListener?.let {
            com.jdroidx.jfindme.data.remote.FirebaseComm.getGeofencingSafeZonesRef(cleanHostCode).removeEventListener(it)
        }
    }

    fun dismissAlert(notificationId: String) {
        // Dismiss notification
    }
}
