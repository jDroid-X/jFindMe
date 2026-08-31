package com.jdroidx.jfindme.ui.group

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

/**
 * STRICT MVVM INTEGRATION FOR PROFILE DETAILS
 * Abstracting all Firebase and SQLite local database logic out of the physical Activity!
 */
class PersonDetailsViewModel : ViewModel() {

    private val TAG = "PersonDetailsViewModel"

    private val _rosterLiveData = MutableLiveData<List<Map<String, Any>>>()
    val rosterLiveData: LiveData<List<Map<String, Any>>> = _rosterLiveData

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
        // 🧬 Forensic Anchor: Resolve Auth UID to Agent ID early to prevent lookup failures
        val localDb = DatabaseHelper.getInstance(context)
        val resolvedAgentId = localDb.resolveAgentIdLocally(targetUid) ?: targetUid
        
        // 1. Instantly Query SharedPreferences for 'Self' profile (Master Hot-path)
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return
        if (targetUid == myUid) {
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val prefsMap = mutableMapOf<String, String>()
            prefsMap["fullName"] = prefs.getString("user_nickname", "") ?: ""
            prefsMap["phone"] = prefs.getString("user_phone", "") ?: ""
            prefsMap["email_id"] = prefs.getString("email", "") ?: ""
            prefsMap["dob"] = prefs.getString("user_dob", "") ?: ""
            prefsMap["gender"] = prefs.getString("user_gender", "") ?: ""
            prefsMap["country"] = prefs.getString("user_country", "") ?: ""
            prefsMap["state"] = prefs.getString("user_state", "") ?: ""
            prefsMap["city"] = prefs.getString("user_city", "") ?: ""
            prefsMap["pincode"] = prefs.getString("user_pincode", "") ?: ""
            prefsMap["address"] = prefs.getString("user_address", "") ?: ""
            prefsMap["emergencyNote"] = prefs.getString("user_emergency_note", "") ?: ""
            prefsMap["orgName"] = prefs.getString("org_name", "") ?: ""
            prefsMap["deptName"] = prefs.getString("department", "") ?: ""
            prefsMap["role_family"] = prefs.getString("role_family", "None") ?: "None"
            prefsMap["role_friend"] = prefs.getString("role_friend", "None") ?: "None"
            
            if (prefsMap["fullName"]?.isNotEmpty() == true) {
                _dossierLiveData.postValue(prefsMap)
                SystemLogger.i(TAG, "Dossier Hot-path Pull (Prefs) Successful for Self")
            }
        }

        // 2. Query Local SQLite Fallback Engine using Resolved Identity
        val localRaw = localDb.getLocalDossier(resolvedAgentId)
        if (localRaw.isNotEmpty()) {
            val localMapped = mutableMapOf<String, String>()
            localMapped["fullName"] = localRaw["fullName"] ?: ""
            localMapped["owner_name"] = localRaw["owner_name"] ?: ""
            localMapped["phone"] = localRaw["phone"] ?: ""
            localMapped["email_id"] = localRaw["email"] ?: ""
            localMapped["dob"] = localRaw["dob"] ?: ""
            localMapped["gender"] = localRaw["gender"] ?: ""
            localMapped["address"] = localRaw["address"] ?: ""
            localMapped["country"] = localRaw["country"] ?: ""
            localMapped["state"] = localRaw["state"] ?: ""
            localMapped["city"] = localRaw["city"] ?: ""
            localMapped["pincode"] = localRaw["pincode"] ?: ""
            localMapped["emergencyNote"] = localRaw["emergency_note"] ?: ""
            localMapped["orgName"] = localRaw["org_name"] ?: ""
            localMapped["deptName"] = localRaw["department"] ?: ""
            localMapped["role_family"] = localRaw["role_family"] ?: ""
            localMapped["role_friend"] = localRaw["role_friend"] ?: ""
            
            SystemLogger.i(TAG, "Dossier Local SQL Pull Normalized (Identity: $resolvedAgentId) & Pushed.")
            _dossierLiveData.postValue(localMapped)
        }

        // 3. Resolve Cloud Identity and Query Cloud Matrix
        FirebaseComm.resolveHardwareIdentity(targetUid) { agentId ->
            val cloudAgentId = agentId ?: resolvedAgentId
            
            val citizenRef = if (!groupId.isNullOrEmpty()) {
                FirebaseComm.relationshipsRef.child(groupId).child("citizen_dossiers").child(cloudAgentId)
            } else {
                FirebaseComm.getProfileRef(cloudAgentId)
            }

            citizenRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val cloudMap = mutableMapOf<String, String>()
                    cloudMap["fullName"] = snapshot.child("fullName").getValue(String::class.java) 
                                         ?: snapshot.child("name").getValue(String::class.java) 
                                         ?: snapshot.child("full_name").getValue(String::class.java) ?: ""
                    cloudMap["phone"] = snapshot.child("phone").getValue(String::class.java) ?: ""
                    cloudMap["email_id"] = snapshot.child("email_id").getValue(String::class.java) 
                                         ?: snapshot.child("email").getValue(String::class.java) ?: ""
                    cloudMap["dob"] = snapshot.child("dob").getValue(String::class.java) ?: ""
                    cloudMap["gender"] = snapshot.child("gender").getValue(String::class.java) ?: ""
                    cloudMap["address"] = snapshot.child("address").getValue(String::class.java) ?: ""
                    cloudMap["country"] = snapshot.child("country").getValue(String::class.java) ?: ""
                    cloudMap["state"] = snapshot.child("state").getValue(String::class.java) ?: ""
                    cloudMap["city"] = snapshot.child("city").getValue(String::class.java) ?: ""
                    cloudMap["pincode"] = snapshot.child("pincode").getValue(String::class.java) ?: ""
                    cloudMap["emergencyNote"] = snapshot.child("emergencyNote").getValue(String::class.java) 
                                             ?: snapshot.child("note").getValue(String::class.java) ?: ""
                    cloudMap["orgName"] = snapshot.child("orgName").getValue(String::class.java) 
                                       ?: snapshot.child("org_name").getValue(String::class.java) ?: ""
                    cloudMap["deptName"] = snapshot.child("deptName").getValue(String::class.java) 
                                        ?: snapshot.child("dept_name").getValue(String::class.java) ?: ""
                    cloudMap["role_family"] = snapshot.child("role_family").getValue(String::class.java) ?: ""
                    cloudMap["role_friend"] = snapshot.child("role_friend").getValue(String::class.java) ?: ""
                    cloudMap["safeZoneName"] = snapshot.child("safeZoneName").getValue(String::class.java) ?: ""
                    cloudMap["safeZoneRadius"] = snapshot.child("safeZoneRadius").getValue(String::class.java) ?: ""
                    
                    cloudMap["acl_live_tracking"] = snapshot.child("acl_live_tracking").getValue(String::class.java) ?: "GRANTED"
                    cloudMap["acl_history_audit"] = snapshot.child("acl_history_audit").getValue(String::class.java) ?: "GRANTED"
                    cloudMap["acl_geofence"] = snapshot.child("acl_geofence").getValue(String::class.java) ?: "GRANTED"
                    cloudMap["acl_admin"] = snapshot.child("acl_admin").getValue(String::class.java) ?: "DENIED"
                    cloudMap["acl_ghost_override"] = snapshot.child("acl_ghost_override").getValue(String::class.java) ?: "DENIED"
                    cloudMap["acl_biometrics"] = snapshot.child("acl_biometrics").getValue(String::class.java) ?: "DENIED"
                    cloudMap["cloud_sync_enabled"] = snapshot.child("cloud_sync_enabled").getValue(String::class.java) ?: "TRUE"
                    
                    SystemLogger.i(TAG, "Dossier Firebase Sync Successful for Agent: $cloudAgentId")
                    
                    // 🚀 LIVE QA FIX: Instantly sync Cloud override down to SQLite using the correct Agent ID
                    val fRole = (if ((cloudMap["role_friend"] ?: "None") != "None") cloudMap["role_friend"] else cloudMap["role_family"]) ?: "MEMBER"
                    localDb.commitHandshakeDossier(cloudAgentId, cloudMap, fRole, cloudMap["fullName"] ?: "Asset Node", cloudAgentId, cloudAgentId, "CLOUD_SYNC", "CLOUD")
                    val rFri = cloudMap["role_friend"] ?: "None"
                    localDb.updateCitizenRole(cloudAgentId, cloudMap["role_family"] ?: "None", rFri, rFri.contains("Friend"))
                    
                    _dossierLiveData.postValue(cloudMap) // Overrides the Local payload with newest Cloud data!
                }
            }.addOnFailureListener {
                SystemLogger.w(TAG, "Cloud Node Awaited for $cloudAgentId. Local DB remains current Truth.")
            }
        }
    }

    // 🚀 PHASE 2: Dynamic Geography Infrastructure (Enterprise Dropdowns)
    private val _countriesList = MutableLiveData<List<String>>()
    val countriesList: LiveData<List<String>> = _countriesList

    private val _statesList = MutableLiveData<List<String>>()
    val statesList: LiveData<List<String>> = _statesList

    private val _citiesList = MutableLiveData<List<String>>()
    val citiesList: LiveData<List<String>> = _citiesList

    fun fetchCountriesList() {
        // Optimized Cache Rule: Countries rarely change. 
        if (_countriesList.value != null && _countriesList.value!!.size > 1) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://countriesnow.space/api/v0.1/countries/iso")
                val response = url.readText()
                val obj = org.json.JSONObject(response)
                val dataArray = obj.getJSONArray("data")
                val list = mutableListOf("Select Country")
                for (i in 0 until dataArray.length()) {
                    list.add(dataArray.getJSONObject(i).getString("name"))
                }
                _countriesList.postValue(list)
            } catch (e: Exception) {
                _countriesList.postValue(listOf("Select Country", "India", "USA", "UK"))
            }
        }
    }

    /**
     * 🚀 Roster Bridge: Fetches the local offline roster for device tagging.
     */
    fun fetchLocalRoster(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val localDb = DatabaseHelper.getInstance(context)
            _rosterLiveData.postValue(localDb.getOfflineRoster())
        }
    }

    /**
     * 🚀 Role Mapping Bridge: Updates citizen roles in both Local SQL and Cloud.
     */
    fun updateCitizenRoleProtocol(context: Context, groupId: String?, targetUid: String, famRole: String, frRole: String, isFriend: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Local Commit — 🔧 FIX ISSUE-05: Pass groupId so DB writes to correct group, not DEFAULT_GROUP
            val localDb = DatabaseHelper.getInstance(context)
            localDb.updateCitizenRole(targetUid, famRole, frRole, isFriend, groupId ?: "DEFAULT_GROUP")
            
            // 2. Cloud Commit
            if (!groupId.isNullOrEmpty()) {
                val cp = mapOf("role_family" to famRole, "role_friend" to frRole)
                FirebaseComm.relationshipsRef.child(groupId)
                    .child("citizen_dossiers").child(targetUid).updateChildren(cp)
                val effectiveRole = if (isFriend) "Friends" else if (famRole != "None" && famRole.isNotBlank()) famRole else "Member"
                FirebaseComm.relationshipsRef.child(groupId)
                    .child("members").child(targetUid).child("role").setValue(effectiveRole)
                FirebaseComm.relationshipsRef.child(groupId)
                    .child("signals").child("refresh_roster").setValue(System.currentTimeMillis())
            }

            // 📝 AUDIT CHANGE LOG: Record Family & Friends role change under /devices/{targetUid}/Change_Log/Family_and_Friends
            FirebaseComm.logChange(targetUid, FirebaseComm.LOG_ITEM_FAMILY_FRIENDS, "ROLE_UPDATE", targetUid, mapOf("role_family" to famRole, "role_friend" to frRole, "isFriend" to isFriend))
            
            // 3. Refresh Roster
            _rosterLiveData.postValue(localDb.getOfflineRoster())
        }
    }

    fun fetchStatesList(country: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://countriesnow.space/api/v0.1/countries/states")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write("{\"country\":\"$country\"}".toByteArray())
                
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = org.json.JSONObject(response)
                val statesArray = obj.getJSONObject("data").getJSONArray("states")
                val list = mutableListOf("Select State")
                for (i in 0 until statesArray.length()) {
                    list.add(statesArray.getJSONObject(i).getString("name"))
                }
                _statesList.postValue(list)
            } catch (e: Exception) {
                _statesList.postValue(listOf("Select State", "Province A", "Province B"))
            }
        }
    }

    fun fetchCitiesList(country: String, state: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://countriesnow.space/api/v0.1/countries/state/cities")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write("{\"country\":\"$country\", \"state\":\"$state\"}".toByteArray())
                
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = org.json.JSONObject(response)
                val citiesArray = obj.getJSONArray("data")
                val list = mutableListOf("Select City")
                for (i in 0 until citiesArray.length()) {
                    list.add(citiesArray.getString(i))
                }
                _citiesList.postValue(list)
            } catch (e: Exception) {
                _citiesList.postValue(listOf("Select City", "City X", "City Y"))
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
        
        // 🚀 Enterprise Prime Directive: Global Identity Shield Sync
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return
        val nameToVerify = payload["fullName"] ?: ""
        if (targetUid == myUid && nameToVerify.contains("@") && nameToVerify.contains(".")) {
            SystemLogger.w(TAG, "Dossier rejected. Identity Node contains restricted handles (@*.*)")
            _saveStatus.postValue(false)
            return
        }

        // 1. Upsert securely down into the Local SQLite Table
        val localDb = DatabaseHelper.getInstance(context)
        val sRole = if ((crossValidatedPayload["role_friend"] ?: "None") != "None") crossValidatedPayload["role_friend"] else crossValidatedPayload["role_family"]
        localDb.commitHandshakeDossier(targetUid, crossValidatedPayload, sRole ?: "MEMBER", crossValidatedPayload["fullName"] ?: "Asset Node", targetUid, targetUid, "USER_EDIT", "LOCAL")
        
        // 🚀 Real-time Livemap Sync (Managed Target): Push identity override to the target's location node
        val targetName = crossValidatedPayload["fullName"] ?: "Asset Node"
        val targetAvatar = crossValidatedPayload["avatar_id"] ?: "avatar_custom_1"
        val locSync = mapOf("userName" to targetName, "avatar_id" to targetAvatar)
        
        FirebaseComm.resolveHardwareIdentity(targetUid) { resolvedTargetAgent ->
            val finalTargetId = resolvedTargetAgent ?: targetUid
            if (FirebaseComm.isAgentIdValid(finalTargetId)) {
                FirebaseComm.getLiveRef(finalTargetId).updateChildren(locSync)
                
                // Push to Group Roster if active
                val myGrp = context.getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE).getString("active_group", null)
                if (myGrp != null) {
                    val rosterUpdate = mapOf(
                        "fullName" to targetName,
                        "name" to targetName,
                        "avatar_id" to targetAvatar,
                        "role" to (sRole ?: "Member")
                    )
                    FirebaseComm.relationshipsRef.child(myGrp).child("members").child(finalTargetId).updateChildren(rosterUpdate)
                    FirebaseComm.relationshipsRef.child(myGrp).child("roster").child(finalTargetId).updateChildren(rosterUpdate)
                    // Send group signal
                    FirebaseComm.relationshipsRef.child(myGrp).child("signals").child("refresh_roster").setValue(System.currentTimeMillis())
                }
            }
        }

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
            stripped["acl_biometrics"] = payload["acl_biometrics"] ?: "DENIED"
            stripped["cloud_sync_enabled"] = "FALSE"
            stripped
        } else {
            payload // Standard Cloud Upload Matrix
        }

        // 3. Resolve Forensic Agent ID and Push finalized (and potentially stripped) array to Cloud Matrix
        FirebaseComm.resolveHardwareIdentity(targetUid) { agentId ->
            val finalAgentId = agentId ?: targetUid
            if (!FirebaseComm.isAgentIdValid(finalAgentId)) {
                _saveStatus.postValue(true)
                return@resolveHardwareIdentity
            }
            
            val targetRef = FirebaseComm.getProfileRef(finalAgentId)

            targetRef.setValue(finalPayload).addOnSuccessListener {
                SystemLogger.i(TAG, if(cloudSyncEnabled == "FALSE") "Private Skeleton Saved to Cloud." else "Full Dossier Successfully Saved to Cloud Matrix for $finalAgentId")
                
                // 🚀 QA Fix: Global Identity Mirror!
                // If we are updating OUR OWN dossier, mirroring core fields to the main Profile node in the Hub!
                if (targetUid == myUid) {
                    val mirrorMap = mutableMapOf<String, Any>()
                    val fn = payload["fullName"] ?: ""
                    mirrorMap["name"] = fn
                    mirrorMap["fullName"] = fn
                    mirrorMap["dob"] = payload["dob"] ?: ""
                    mirrorMap["gender"] = payload["gender"] ?: ""
                    mirrorMap["country"] = payload["country"] ?: ""
                    mirrorMap["state"] = payload["state"] ?: ""
                    mirrorMap["city"] = payload["city"] ?: ""
                    mirrorMap["pincode"] = payload["pincode"] ?: ""
                    mirrorMap["phone"] = payload["phone"] ?: ""
                    mirrorMap["email"] = payload["email_id"] ?: ""
                    mirrorMap["address"] = payload["address"] ?: ""
                    mirrorMap["note"] = payload["emergencyNote"] ?: ""
                    mirrorMap["emergencyNote"] = payload["emergencyNote"] ?: ""
                    mirrorMap["org_name"] = payload["orgName"] ?: ""
                    mirrorMap["dept_name"] = payload["deptName"] ?: ""
                    mirrorMap["updated_at"] = FirebaseComm.getForensicTimestamp()
                    
                    FirebaseComm.getProfileRef(finalAgentId).updateChildren(mirrorMap)
                }

                // 📝 AUDIT CHANGE LOG: Record Profile dossier change under /devices/{finalAgentId}/Change_Log/Profile
                FirebaseComm.logChange(finalAgentId, FirebaseComm.LOG_ITEM_PROFILE, "DOSSIER_SAVE", finalAgentId, finalPayload)
                
                _saveStatus.postValue(true)
            }.addOnFailureListener {
                SystemLogger.w(TAG, "Cloud Save Failed. Local Cache Preserved.")
                _saveStatus.postValue(false)
            }
                
            val logMap = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "editorUid" to myUid,
                "action" to if(cloudSyncEnabled == "FALSE") "Data Minimization Override Activated. Stripped Dossier mapped." else "Full Metadata Sync Updated."
            )
            
            val logRef = if (!groupId.isNullOrEmpty()) {
                FirebaseComm.relationshipsRef.child(groupId).child("citizen_dossier_logs").child(finalAgentId)
            } else {
                FirebaseComm.getProfileRef(finalAgentId).child("isolated_dossier_logs")
            }
            logRef.push().setValue(logMap)
        }
    }

    fun fetchChangeLog(groupId: String?, targetUid: String) {
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return
        
        FirebaseComm.resolveHardwareIdentity(targetUid) { agentId ->
            val finalAgentId = agentId ?: targetUid
            val logRef = if (!groupId.isNullOrEmpty()) {
                FirebaseComm.relationshipsRef.child(groupId).child("citizen_dossier_logs").child(finalAgentId)
            } else {
                FirebaseComm.getProfileRef(finalAgentId).child("isolated_dossier_logs")
            }
            
            logRef.orderByChild("timestamp").limitToLast(10)
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
}

