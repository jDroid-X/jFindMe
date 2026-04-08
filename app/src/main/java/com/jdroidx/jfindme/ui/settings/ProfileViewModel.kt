package com.jdroidx.jfindme.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.telephony.TelephonyManager
import android.telephony.SubscriptionManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.ui.base.BaseViewModel
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.HashMap
import java.util.Locale

/**
 * 🚀 SENTINEL REVISED: Pure MVVM Profiling Engine
 * Waterfall Logic: Database -> ViewModel -> Activity (One-Way Reactive Flow)
 */
class ProfileViewModel : BaseViewModel() {

    private val _saveSuccess = MutableLiveData<Boolean?>()
    val saveSuccess: LiveData<Boolean?> get() = _saveSuccess

    fun resetSaveState() {
        _saveSuccess.value = null
    }

    private val _loadingState = MutableLiveData<String>()
    val loadingState: LiveData<String> get() = _loadingState

    private val _validationErrorField = MutableLiveData<String>()
    val validationErrorField: LiveData<String> get() = _validationErrorField

    // --- 🏛️ REACTIVE DATA MATRIX ---
    
    private val _profileData = MutableLiveData<Map<String, String>>()
    val profileData: LiveData<Map<String, String>> get() = _profileData

    private val _countriesList = MutableLiveData<List<String>>()
    val countriesList: LiveData<List<String>> get() = _countriesList

    private val _statesList = MutableLiveData<List<String>>()
    val statesList: LiveData<List<String>> get() = _statesList

    private val _citiesList = MutableLiveData<List<String>>()
    val citiesList: LiveData<List<String>> get() = _citiesList

    // --- 🏛️ WATERFALL INITIALIZATION ---

    fun loadForensicProfile(context: Context) {
        val safeUid = FirebaseAuth.getInstance().uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val db = DatabaseHelper.getInstance(context)
            
            // 🧪 STEP 1: PRIORITIZE LOCAL FORENSIC VAULT (Zero Latency)
            val localDossier = db.getLocalDossier(safeUid)
            var hasMandatoryData = false
            
            if (localDossier != null) {
                // Check for Mandatory Identify Tokens (Name, Phone, DOB)
                val name = localDossier["full_name"] ?: localDossier["userName"] ?: ""
                val phone = localDossier["user_phone"] ?: localDossier["userPhone"] ?: ""
                val dob = localDossier["user_dob"] ?: localDossier["userDob"] ?: ""
                
                if (name.isNotEmpty() && phone.isNotEmpty() && dob.isNotEmpty()) {
                    hasMandatoryData = true
                    _profileData.postValue(localDossier)
                    SystemLogger.i("IdentityHub", "Local Forensic Truth Verified for $safeUid")
                }
            }

            // 🧪 STEP 3: SILICON FINGERPRINT AUDIT (Hardware Ledger Sync)
            auditHardwareSignature(context)

            // 🧪 STEP 2: CLOUD-FALLBACK (Only if Local is Empty or Incomplete)
            if (!hasMandatoryData) {
                SystemLogger.w("IdentityHub", "Local Data Incomplete. Synchronizing with Cloud Matrix...")
                val userRef = FirebaseDatabase.getInstance().getReference("users").child(safeUid)
                userRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val rawData = snapshot.value as? Map<String, Any> ?: return@addOnSuccessListener
                        val source = if (rawData.containsKey("profile")) {
                            rawData["profile"] as? Map<String, Any> ?: rawData
                        } else {
                            rawData
                        }
                        
                        val cloudMap = mutableMapOf<String, String>()
                        source.forEach { (k, v) -> cloudMap[k] = v.toString() }
                        
                        // Display and Atomically Update Local Vault
                        _profileData.postValue(cloudMap)
                        viewModelScope.launch(Dispatchers.IO) { 
                            db.commitForensicDossier(safeUid, cloudMap) 
                        }
                        SystemLogger.i("IdentityHub", "Cloud Backfill Successful for $safeUid")
                    } else {
                        SystemLogger.e("IdentityHub", "Identity Not Found in Cloud. Manual Entry Required.")
                    }
                }
            }
        }
    }

    // --- 🌍 GEO-VIBRANCY ENGINE ---

    fun fetchNetworkLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conn = java.net.URL("https://countriesnow.space/api/v0.1/countries/iso").openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                val reader = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
                val json = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) json.append(line)
                reader.close()

                val obj = org.json.JSONObject(json.toString())
                val dataArray = obj.getJSONArray("data")
                val list = ArrayList<String>()
                list.add("Select Country")
                for (i in 0 until dataArray.length()) list.add(dataArray.getJSONObject(i).getString("name"))
                _countriesList.postValue(list)
            } catch (e: Exception) {
                _countriesList.postValue(listOf("United States", "India", "United Kingdom", "Canada"))
            }
        }
    }

    fun fetchStatesList(selectedCountry: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://countriesnow.space/api/v0.1/countries/states")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write("{\"country\":\"$selectedCountry\"}".toByteArray())
                val reader = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
                val json = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) json.append(line)
                reader.close()

                val obj = org.json.JSONObject(json.toString())
                val statesArray = obj.getJSONObject("data").getJSONArray("states")
                val list = ArrayList<String>()
                list.add("Select State")
                for (i in 0 until statesArray.length()) list.add(statesArray.getJSONObject(i).getString("name"))
                _statesList.postValue(list)
            } catch(e: Exception) { _statesList.postValue(listOf("Select State")) }
        }
    }

    fun fetchCitiesList(selectedCountry: String, selectedState: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://countriesnow.space/api/v0.1/countries/state/cities")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write("{\"country\":\"$selectedCountry\", \"state\":\"$selectedState\"}".toByteArray())
                val reader = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
                val json = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) json.append(line)
                reader.close()

                val obj = org.json.JSONObject(json.toString())
                val citiesArray = obj.getJSONArray("data")
                val list = ArrayList<String>()
                list.add("Select City")
                for (i in 0 until citiesArray.length()) list.add(citiesArray.getString(i))
                _citiesList.postValue(list)
            } catch(e: Exception) { _citiesList.postValue(listOf("Select City")) }
        }
    }

    /**
     * 🚀 SILICON FINGERPRINT AUDIT (L7 Global Architect)
     * Collects and archives absolute hardware-bound identities into the forensic ledger.
     */
    private fun auditHardwareSignature(context: Context) {
        val safeUid = FirebaseAuth.getInstance().uid ?: return
        val db = DatabaseHelper.getInstance(context)
        
        viewModelScope.launch(Dispatchers.IO) {
            val build = android.os.Build::class.java
            val manufacturer = android.os.Build.MANUFACTURER
            val model = android.os.Build.MODEL
            val os = android.os.Build.VERSION.RELEASE
            val patch = if (android.os.Build.VERSION.SDK_INT >= 23) android.os.Build.VERSION.SECURITY_PATCH else "PRE-M"
            val kernel = System.getProperty("os.version") ?: "n/a"
            val fingerprint = android.os.Build.FINGERPRINT
            val cpu = Runtime.getRuntime().availableProcessors()
            
            // 🚀 UNIVERSAL HARDWARE SIGNATURE (L7 PRIVACY COMPLIANCE)
            val hardwareId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "NULL_HARDWARE_NODE"
            
            // 🧠 RAM & STORAGE DIAGNOSTICS (L7 Matrix)
            var totalRam = "n/a"
            var totalStorage = "n/a"
            try {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val memInfo = android.app.ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                totalRam = (memInfo.totalMem / (1024 * 1024 * 1024)).toString() + " GB"
                val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                totalStorage = (stat.totalBytes / (1024 * 1024 * 1024)).toString() + " GB"
            } catch (e: Exception) {}

            // 🚀 TELEPHONY AUDIT (Multi-Slot Matrix)
            var imei1 = "RESTRICTED (OS 10)"
            var imei2 = "RESTRICTED (OS 10)"
            var iccid1 = "ISOLATED"
            var iccid2 = "ISOLATED"
            var op1 = "n/a"
            var op2 = "n/a"
            var secured = "STANDARD_OS"

            try {
                val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                op1 = telephony.simOperatorName ?: "OFFLINE"
                
                // Subscription Manager Bridge (Multi-SIM Forensic Matrix)
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val subs = subManager.activeSubscriptionInfoList
                if (!subs.isNullOrEmpty()) {
                    val s1 = if (subs.size > 0) subs[0] else null
                    val s2 = if (subs.size > 1) subs[1] else null
                    
                    if (s1 != null) {
                        op1 = s1.displayName?.toString() ?: s1.carrierName?.toString() ?: "Unknown Slot 1"
                        iccid1 = "ICCID: " + (s1.iccId?.takeLast(5) ?: "Hidden")
                    }
                    if (s2 != null) {
                        op2 = s2.displayName?.toString() ?: s2.carrierName?.toString() ?: "Unknown Slot 2"
                        iccid2 = "ICCID: " + (s2.iccId?.takeLast(5) ?: "Hidden")
                    }
                }

                if (android.os.Build.TAGS != null && android.os.Build.TAGS.contains("test-keys")) secured = "FORENSIC_ALERT (ROOT)"
            } catch (e: Exception) {
                secured = "SECURITY_ISOLATION_ACTIVE"
            }

            // 🚀 Atomic Hardware Registry Commit
            db.commitHardwareForensic(
                safeUid, 
                hardwareId,
                model ?: "UNKNOWN_MODEL",
                manufacturer ?: "UNKNOWN_MFG",
                "2026-ARCH-BATCH", 
                os ?: "n/a",
                patch ?: "n/a",
                kernel,
                fingerprint ?: "n/a",
                cpu,
                totalRam,
                totalStorage,
                secured, 
                imei1, imei2, 
                iccid1, iccid2, 
                op1, op2, 
                secured.contains("ROOT"), 
                "REDACTED" 
            )
            SystemLogger.i("HardwareAudit", "Silicon Fingerprint Archived for $safeUid")
        }
    }

    // --- 🏛️ CLOSED-LOOP COMMIT MOTOR ---

    /**
     * 🚀 PILLAR 3: THE UNIVERSAL IDENTITY COMMAND (Triple-Ledger Commit)
     * Rule 1: Local SQLite (Forensic Truth)
     * Rule 2: Cloud Root (User Identity Node)
     * Rule 3: Group Dossier (Relational Visibility)
     * Rule 4: Group Roster (Map Pin Handshake)
     */
    fun verifyAndSaveProfile(
        context: Context,
        nameVal: String, phoneVal: String, dobVal: String, genderIdx: Int, genderVal: String,
        countryIdentifying: Boolean, pincodeVal: String,
        cVal: String, sVal: String, cityVal: String, famStr: String, friendStr: String,
        addressVal: String, emergencyNoteVal: String,
        orgName: String, deptName: String,
        prefs: SharedPreferences
    ) {
        val errorChecklist = mutableListOf<String>()
        if (nameVal.length < 2) errorChecklist.add("Name: 2+ characters required")
        if (phoneVal.length < 7) errorChecklist.add("Phone: Handset Node missing digits")
        if (pincodeVal.isEmpty()) errorChecklist.add("Address: Area Pincode mandatory")

        if (errorChecklist.isNotEmpty()) {
            _validationErrorField.value = "FAIL"
            _errorMessage.value = "SENTINEL ALERT:\n" + errorChecklist.joinToString("\n")
            return
        }

        val safeUid = FirebaseAuth.getInstance().uid ?: return
        _isLoading.value = true
        
        viewModelScope.launch(Dispatchers.Main) {
            _loadingState.value = "Committing Forensic Identity..."
            delay(500)
            
            // --- 🧪 LEDGER 1: CLOUD ROOT (IDENTITY NODE) ---
            val rootPayload = HashMap<String, Any>()
            rootPayload["userName"] = nameVal
            rootPayload["userPhone"] = phoneVal
            rootPayload["userDob"] = dobVal
            rootPayload["userGender"] = genderVal
            rootPayload["userCountry"] = cVal
            rootPayload["userState"] = sVal
            rootPayload["userCity"] = cityVal
            rootPayload["userPincode"] = pincodeVal
            rootPayload["userAddress"] = addressVal
            rootPayload["userEmergencyNote"] = emergencyNoteVal
            rootPayload["orgName"] = orgName
            rootPayload["deptName"] = deptName
            rootPayload["profile_onboarded"] = true
            
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(safeUid)
            userRef.updateChildren(rootPayload)

            // --- 🧪 LEDGER 2: LOCAL REGISTRY (SHARED PREFS) ---
            prefs.edit().putString("user_nickname", nameVal)
                .putString("user_phone", phoneVal)
                .putString("user_country", cVal)
                .putString("user_state", sVal)
                .putString("user_city", cityVal)
                .putString("user_dob", dobVal)
                .putString("user_gender", genderVal)
                .putString("user_pincode", pincodeVal)
                .putString("user_address", addressVal)
                .putString("user_emergency_note", emergencyNoteVal)
                .putString("org_name", orgName)
                .putString("department", deptName)
                .putBoolean("profile_onboarded", true).apply()

            // --- 🧪 LEDGER 3: FORENSIC SQLITE (PERSONAL DOSSIER) ---
            val dossierMap = mutableMapOf<String, String>()
            dossierMap["full_name"] = nameVal
            dossierMap["user_phone"] = phoneVal
            dossierMap["user_dob"] = dobVal
            dossierMap["user_gender"] = genderVal
            dossierMap["user_country"] = cVal
            dossierMap["user_state"] = sVal
            dossierMap["user_city"] = cityVal
            dossierMap["user_pincode"] = pincodeVal
            dossierMap["user_address"] = addressVal
            dossierMap["user_emergency_note"] = emergencyNoteVal
            dossierMap["org_name"] = orgName
            dossierMap["department"] = deptName
            
            val db = DatabaseHelper.getInstance(context)
            val finalRole = if (friendStr != "None") friendStr else famStr
            db.commitForensicDossier(safeUid, dossierMap, finalRole, nameVal, safeUid, safeUid, "LOCAL_PROFILE_COMMIT")
            
            // --- 🧪 LEDGER 4: ROSTER RIPPLE (GROUP SYNC) ---
            val activeGroup = prefs.getString("active_group", null)
            if (!activeGroup.isNullOrEmpty()) {
                val groupRef = FirebaseDatabase.getInstance().getReference("groups").child(activeGroup)
                
                // A. Update Group Roster (For Map Pins/Tiles)
                val rosterUpdate = mapOf("display_name" to nameVal, "role_title" to finalRole)
                groupRef.child("roster").child(safeUid).updateChildren(rosterUpdate)
                
                // B. Update Group Dossier (For Detailed Profile)
                val cloudDossierPayload = HashMap<String, Any>()
                cloudDossierPayload["fullName"] = nameVal
                cloudDossierPayload["phone"] = phoneVal
                cloudDossierPayload["dob"] = dobVal
                cloudDossierPayload["gender"] = genderVal
                cloudDossierPayload["country"] = cVal
                cloudDossierPayload["state"] = sVal
                cloudDossierPayload["city"] = cityVal
                cloudDossierPayload["pincode"] = pincodeVal
                cloudDossierPayload["address"] = addressVal
                cloudDossierPayload["emergencyNote"] = emergencyNoteVal
                cloudDossierPayload["orgName"] = orgName
                cloudDossierPayload["deptName"] = deptName
                cloudDossierPayload["last_updated"] = System.currentTimeMillis()
                
                groupRef.child("citizen_dossiers").child(safeUid).updateChildren(cloudDossierPayload)
                
                // C. Update Local Roster Table
                db.saveRosterNode(safeUid, nameVal, finalRole, "Active", true, false, nameVal, safeUid, safeUid, "LOCAL_SYNC")
            }

            _isLoading.value = false
            _saveSuccess.value = true
        }
    }

    val currentUserId: String? get() = FirebaseAuth.getInstance().uid

    fun equipAvatar(prefs: SharedPreferences, id: Int, vrmPath: String?) {
        val editor = prefs.edit().putInt("equipped_avatar_id", id)
        if (vrmPath != null) editor.putString("equipped_vrm_string", vrmPath)
        editor.apply()
        
        currentUserId?.let { uid ->
            val ref = FirebaseDatabase.getInstance().reference.child("users").child(uid)
            val updates = HashMap<String, Any>()
            updates["equipped_avatar_id"] = id
            if (vrmPath != null) updates["equipped_vrm_string"] = vrmPath
            ref.updateChildren(updates)
        }
    }
}
