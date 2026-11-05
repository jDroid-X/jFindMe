package com.jdroidx.jfindme.ui.management

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger

class NotifViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotifViewModel"
    private val dbHelper = DatabaseHelper.getInstance(application)
    private val auth = FirebaseAuth.getInstance()

    private val _alerts = MutableLiveData<List<Map<String, String>>>()
    val alerts: LiveData<List<Map<String, String>>> = _alerts

    private val _pendingRequests = MutableLiveData<List<Map<String, String>>>()
    val pendingRequests: LiveData<List<Map<String, String>>> = _pendingRequests

    private val cloudAlertsList = mutableListOf<Map<String, String>>()

    init {
        loadLocalAlerts()
        listenToPendingRequests()
        listenToCloudNotifications()
    }

    fun loadLocalAlerts() {
        val list = dbHelper.getPagedData(DatabaseHelper.TABLE_ALERTS, 50, 0, null)
        val combined = mutableListOf<Map<String, String>>()
        combined.addAll(cloudAlertsList)
        combined.addAll(list)
        _alerts.postValue(combined)
    }

    private var activeAgentId: String? = null

    private var pendingRequestsListener: ValueEventListener? = null
    private var pendingRequestsRef: com.google.firebase.database.DatabaseReference? = null
    private var cloudNotificationsListener: ValueEventListener? = null
    private var cloudNotificationsRef: com.google.firebase.database.DatabaseReference? = null

    private fun listenToPendingRequests() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        val finalId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(getApplication())
        activeAgentId = finalId
        val ref = FirebaseComm.devicesRef.child(finalId).child("Identity").child("geofence_sync_requests")
        pendingRequestsRef = ref
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Map<String, String>>()
                for (snap in snapshot.children) {
                    val status = snap.child("status").getValue(String::class.java)
                    if ("PENDING_ACCEPTANCE" == status) {
                        val map = mutableMapOf<String, String>()
                        map["id"] = snap.key ?: ""
                        map["zone_id"] = snap.child("zone_id").getValue(String::class.java) ?: snap.key ?: ""
                        map["zone_name"] = snap.child("zone_name").getValue(String::class.java) ?: "Unknown"
                        map["group_id"] = snap.child("group_id").getValue(String::class.java) ?: ""
                        map["lat"] = snap.child("lat").value?.toString() ?: ""
                        map["lng"] = snap.child("lng").value?.toString() ?: ""
                        map["radius"] = snap.child("radius").value?.toString() ?: "100"
                        list.add(map)
                    }
                }
                _pendingRequests.postValue(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        pendingRequestsListener = listener
        ref.addValueEventListener(listener)
    }

    /**
     * 🛰️ Real-Time Mesh Notification Stream: Listens to incoming cloud notifications
     */
    private fun listenToCloudNotifications() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        val context = getApplication<Application>()
        val groupId = com.jdroidx.jfindme.utils.RegistryManager.getActiveGroup(context).ifEmpty {
            com.jdroidx.jfindme.utils.RegistryManager.getString(context, "current_group_id", "")
        }
        if (groupId.isEmpty()) return
        val cleanGroupId = FirebaseComm.sanitizePathKey(groupId)

        val notifRef = FirebaseComm.relationshipsRef.child(cleanGroupId).child("notifications")
        cloudNotificationsRef = notifRef
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cloudAlertsList.clear()
                for (snap in snapshot.children) {
                    val map = mutableMapOf<String, String>()
                    map["id"] = snap.key ?: ""
                    map["avatar_name"] = snap.child("avatar_name").getValue(String::class.java) ?: snap.child("target_name").getValue(String::class.java) ?: "Device"
                    map["zone_name"] = snap.child("zone_name").getValue(String::class.java) ?: "Safe Zone"
                    map["status"] = snap.child("status").getValue(String::class.java) ?: snap.child("transition").getValue(String::class.java) ?: "ENTERED"
                    map["datetime"] = snap.child("datetime").getValue(String::class.java) ?: snap.child("readable_time").getValue(String::class.java) ?: ""
                    map["time"] = snap.child("time").getValue(String::class.java) ?: ""
                    map["title"] = "${map["avatar_name"]} ${map["status"]?.lowercase()} ${map["zone_name"]}"
                    map["msg"] = "Safe zone perimeter transition detected at ${map["datetime"]}."
                    map["timestamp"] = snap.child("timestamp").value?.toString() ?: ""
                    cloudAlertsList.add(0, map) // Latest first
                }
                loadLocalAlerts()
            }
            override fun onCancelled(error: DatabaseError) {
                SystemLogger.e(TAG, "Cloud notifications stream failed: ${error.message}")
            }
        }
        cloudNotificationsListener = listener
        notifRef.limitToLast(30).addValueEventListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        pendingRequestsListener?.let { l ->
            pendingRequestsRef?.removeEventListener(l)
        }
        cloudNotificationsListener?.let { l ->
            cloudNotificationsRef?.removeEventListener(l)
        }
    }
}
