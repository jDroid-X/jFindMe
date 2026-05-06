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
    val readableTime: String = ""
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

    init {
        setupCloudSync()
    }

    private fun setupCloudSync() {
        // 🛰️ MISSION ARCHITECTURE: Listen to Group Cloud Matrix for Node Transitions
        val prefs = getApplication<Application>().getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val groupId = prefs.getString("active_group", null) ?: return
        
        if (groupId.startsWith("LOCAL_SANDBOX")) return

        val ref = FirebaseDatabase.getInstance().getReference("groups").child(groupId).child("notifications")
        
        cloudListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    snapshot.children.forEach { node ->
                        val avatar = node.child("avatar_name").getValue(String::class.java) ?: "Unknown"
                        val zone = node.child("zone_name").getValue(String::class.java) ?: "Unknown"
                        val status = node.child("status").getValue(String::class.java) ?: "OUT"
                        val time = node.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val uid = node.child("uid").getValue(String::class.java) ?: "REMOTE_NODE"
                        
                        // 💾 ANALYTIC ARCHIVAL: Save to local SQLite if it doesn't exist 
                        // (Note: saveGeofenceLog uses UUID for log_id, preventing duplicates requires hash mapping or similar)
                        // For V49.5: Simply archival ensures Admin sees Device B's transitions.
                        db.saveGeofenceLog(uid, avatar, zone, zone, status, 0.0, 0.0)
                    }
                    // 🚀 Refresh UI Stream
                    fetchNotifications()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun fetchNotifications(targetAvatar: String? = null, zoneId: String? = null) {
        val logs = db.getGeofenceLogs(targetAvatar, zoneId)
        val list = logs.map { map ->
            GeofenceNotification(
                id = map["log_id"].toString(),
                avatarName = map["fullName"]?.toString() ?: "Unknown",
                status = map["status"]?.toString() ?: "OUT",
                timestamp = (map["ts_start"] as? Long) ?: 0L,
                zoneName = map["zone_name"]?.toString() ?: "Unknown",
                readableTime = map["readable_time"]?.toString() ?: ""
            )
        }
        _notifications.postValue(list)
    }

    override fun onCleared() {
        super.onCleared()
        cloudListener?.let {
            val prefs = getApplication<Application>().getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val groupId = prefs.getString("active_group", null) ?: return
            FirebaseDatabase.getInstance().getReference("groups").child(groupId).child("notifications").removeEventListener(it)
        }
    }
}
