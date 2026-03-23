package com.jdroidx.jfindme.ui.group

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.data.local.CitizenDbHelper
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * STRICT MVVM INTEGRATION FOR CITIZEN DOSSIERS
 * Abstracting all Firebase and SQLite local database logic out of the physical Activity!
 */
class PersonDetailsViewModel : ViewModel() {

    private val TAG = "PersonDetailsViewModel"

    private val _dossierLiveData = MutableLiveData<Map<String, String>>()
    val dossierLiveData: LiveData<Map<String, String>> = _dossierLiveData

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus
    
    private val _changeLogLiveData = MutableLiveData<List<String>>()
    val changeLogLiveData: LiveData<List<String>> = _changeLogLiveData

    /**
     * Executes the Dual-Pull Protocol: Intercepts SQLite instantly, then listens to Cloud Firebase.
     */
    fun fetchDossier(context: Context, groupId: String?, targetUid: String) {
        // 1. Instantly Query Local SQLite Fallback Engine
        val localDb = CitizenDbHelper(context)
        val localData = localDb.getLocalDossier(targetUid)
        if (localData != null) {
            SystemLogger.i(TAG, "Dossier Local SQL Pull Successful")
            _dossierLiveData.postValue(localData)
        }

        // 2. Query Firebase Cloud for the latest Synced State
        if (!groupId.isNullOrEmpty()) {
            val citizenRef = FirebaseDatabase.getInstance().getReference()
                .child("groups").child(groupId).child("citizen_dossiers").child(targetUid)

            citizenRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val cloudMap = mutableMapOf<String, String>()
                    cloudMap["fullName"] = snapshot.child("fullName").getValue(String::class.java) ?: ""
                    cloudMap["dob"] = snapshot.child("dob").getValue(String::class.java) ?: ""
                    cloudMap["gender"] = snapshot.child("gender").getValue(String::class.java) ?: ""
                    cloudMap["address"] = snapshot.child("address").getValue(String::class.java) ?: ""
                    cloudMap["emergencyNote"] = snapshot.child("emergencyNote").getValue(String::class.java) ?: ""
                    cloudMap["role_family"] = snapshot.child("role_family").getValue(String::class.java) ?: ""
                    cloudMap["role_friend"] = snapshot.child("role_friend").getValue(String::class.java) ?: ""
                    cloudMap["safeZoneName"] = snapshot.child("safeZoneName").getValue(String::class.java) ?: ""
                    cloudMap["safeZoneRadius"] = snapshot.child("safeZoneRadius").getValue(String::class.java) ?: ""
                    
                    cloudMap["acl_live_tracking"] = snapshot.child("acl_live_tracking").getValue(String::class.java) ?: "GRANTED"
                    cloudMap["acl_history_audit"] = snapshot.child("acl_history_audit").getValue(String::class.java) ?: "GRANTED"
                    cloudMap["acl_geofence"] = snapshot.child("acl_geofence").getValue(String::class.java) ?: "GRANTED"
                    cloudMap["acl_admin"] = snapshot.child("acl_admin").getValue(String::class.java) ?: "DENIED"
                    cloudMap["acl_ghost_override"] = snapshot.child("acl_ghost_override").getValue(String::class.java) ?: "DENIED"
                    cloudMap["cloud_sync_enabled"] = snapshot.child("cloud_sync_enabled").getValue(String::class.java) ?: "TRUE"
                    
                    SystemLogger.i(TAG, "Dossier Firebase Sync Successful")
                    
                    // 🚀 LIVE QA FIX: Instantly sync Cloud override down to SQLite so other systems (like Map Engine) read the truth!
                    localDb.saveLocalDossier(targetUid, cloudMap)
                    localDb.updateCitizenRole(targetUid, cloudMap["role_family"] ?: "None", cloudMap["role_friend"] ?: "None")
                    
                    _dossierLiveData.postValue(cloudMap) // Overrides the Local payload with newest Cloud data!
                }
            }
        }
    }

    /**
     * Executes the Dual-Push Protocol: Secures data offline, then uploads to Firebase.
     */
    fun saveDossier(context: Context, groupId: String?, targetUid: String, payload: Map<String, String>) {
        val crossValidatedPayload = payload.toMutableMap()
        crossValidatedPayload["network_id"] = groupId ?: "ORPHANED_NET"
        crossValidatedPayload["device_id"] = targetUid  // Hard links the Node identity perfectly!

        // 1. Upsert securely down into the Local SQLite Table
        val localDb = CitizenDbHelper(context)
        localDb.saveLocalDossier(targetUid, crossValidatedPayload)

        // 2. Evaluate Privacy Setting before touching Cloud Arrays
        val cloudSyncEnabled = payload["cloud_sync_enabled"] ?: "TRUE"

        val finalPayload = if (cloudSyncEnabled == "FALSE") {
            // 🔒 PRIVACY PROTOCOL INITIATED: Strip all biometrics, medicals, and physical addresses.
            // Only purely foundational networking tags are allowed out.
            val stripped = mutableMapOf<String, String>()
            stripped["fullName"] = payload["fullName"] ?: "Private Citizen"
            stripped["role_family"] = payload["role_family"] ?: ""
            stripped["role_friend"] = payload["role_friend"] ?: ""
            stripped["safeZoneName"] = payload["safeZoneName"] ?: ""
            stripped["safeZoneRadius"] = payload["safeZoneRadius"] ?: ""
            stripped["acl_live_tracking"] = payload["acl_live_tracking"] ?: "GRANTED"
            stripped["acl_history_audit"] = payload["acl_history_audit"] ?: "GRANTED"
            stripped["acl_geofence"] = payload["acl_geofence"] ?: "GRANTED"
            stripped["acl_admin"] = payload["acl_admin"] ?: "DENIED"
            stripped["acl_ghost_override"] = payload["acl_ghost_override"] ?: "DENIED"
            stripped["cloud_sync_enabled"] = "FALSE"
            stripped
        } else {
            payload // Standard Cloud Upload Matrix
        }

        // 3. Push finalized (and potentially stripped) array to Cloud Firebase
        if (!groupId.isNullOrEmpty()) {
            FirebaseDatabase.getInstance().reference
                .child("groups").child(groupId).child("citizen_dossiers").child(targetUid)
                .setValue(finalPayload).addOnSuccessListener {
                    SystemLogger.i(TAG, if(cloudSyncEnabled == "FALSE") "Private Skeleton Saved to Cloud." else "Full Dossier Successfully Saved to Cloud Matrix")
                    _saveStatus.postValue(true)
                }.addOnFailureListener {
                    SystemLogger.w(TAG, "Cloud Save Failed. Local Cache Preserved.")
                    _saveStatus.postValue(false)
                }
                
            val logMap = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "editorUid" to (com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: "Unknown"),
                "action" to if(cloudSyncEnabled == "FALSE") "Data Minimization Override Activated. Stripped Dossier mapped." else "Full Metadata Sync Updated."
            )
            FirebaseDatabase.getInstance().reference
                .child("groups").child(groupId).child("citizen_dossier_logs").child(targetUid)
                .push().setValue(logMap)
        } else {
            SystemLogger.w(TAG, "No Group Hub Found. Saving Offline Only.")
            _saveStatus.postValue(true) // Consider it a success since offline secured it
        }
    }

    fun fetchChangeLog(groupId: String?, targetUid: String) {
        if (groupId == null) return
        FirebaseDatabase.getInstance().reference
            .child("groups").child(groupId).child("citizen_dossier_logs").child(targetUid)
            .orderByChild("timestamp").limitToLast(10)
            .get().addOnSuccessListener { snap ->
                val logs = mutableListOf<String>()
                snap.children.forEach { child ->
                    val ts = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val action = child.child("action").getValue(String::class.java) ?: ""
                    val editor = child.child("editorUid").getValue(String::class.java) ?: ""
                    val date = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
                    logs.add("[$date] $action (Admin Node: $editor)")
                }
                _changeLogLiveData.postValue(logs.reversed())
            }
    }
}
