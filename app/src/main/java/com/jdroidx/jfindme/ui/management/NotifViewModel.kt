package com.jdroidx.jfindme.ui.management

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.data.local.DatabaseHelper

class NotifViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = DatabaseHelper.getInstance(application)
    private val auth = FirebaseAuth.getInstance()

    private val _alerts = MutableLiveData<List<Map<String, String>>>()
    val alerts: LiveData<List<Map<String, String>>> = _alerts

    private val _pendingRequests = MutableLiveData<List<Map<String, String>>>()
    val pendingRequests: LiveData<List<Map<String, String>>> = _pendingRequests

    init {
        loadLocalAlerts()
        listenToPendingRequests()
    }

    fun loadLocalAlerts() {
        // Fetch from TABLE_ALERTS
        val list = dbHelper.getPagedData(DatabaseHelper.TABLE_ALERTS, 50, 0, null)
        _alerts.postValue(list)
    }

    private fun listenToPendingRequests() {
        val uid = auth.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("geofence_sync_requests")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Map<String, String>>()
                for (snap in snapshot.children) {
                    val status = snap.child("status").getValue(String::class.java)
                    if ("PENDING_ACCEPTANCE" == status) {
                        val map = mutableMapOf<String, String>()
                        map["id"] = snap.key ?: ""
                        map["zone_name"] = snap.child("zone_name").getValue(String::class.java) ?: "Unknown"
                        map["group_id"] = snap.child("group_id").getValue(String::class.java) ?: ""
                        list.add(map)
                    }
                }
                _pendingRequests.postValue(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
