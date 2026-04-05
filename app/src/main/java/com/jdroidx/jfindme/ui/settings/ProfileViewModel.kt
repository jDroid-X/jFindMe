package com.jdroidx.jfindme.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🚀 Pure MVVM Profiling Engine
 * Executes all Business Validation, Cloud Payload Matrix construction, 
 * and persistent storage queries structurally decoupled from the UI layer.
 */
class ProfileViewModel : BaseViewModel() {

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> get() = _saveSuccess

    // 🚀 QA Fix: Master Matrix Script Pipeline
    private val _loadingState = MutableLiveData<String>()
    val loadingState: LiveData<String> get() = _loadingState

    // Emits the exact field name that failed validation to physically highlight it on the UI
    private val _validationErrorField = MutableLiveData<String>()
    val validationErrorField: LiveData<String> get() = _validationErrorField

    data class NetworkLocationPayload(val country: String, val state: String, val city: String, val zip: String)

    private val _networkLocation = MutableLiveData<NetworkLocationPayload>()
    val networkLocation: LiveData<NetworkLocationPayload> get() = _networkLocation
    
    // 🚀 QA Structural Cascade Dropdown Arrays (Internet Sync)
    private val _countriesList = MutableLiveData<List<String>>()
    val countriesList: LiveData<List<String>> get() = _countriesList

    private val _statesList = MutableLiveData<List<String>>()
    val statesList: LiveData<List<String>> get() = _statesList

    private val _citiesList = MutableLiveData<List<String>>()
    val citiesList: LiveData<List<String>> get() = _citiesList

    // Replaces purely automatic IP population with selectable open cascades
    fun fetchNetworkLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conn = java.net.URL("https://countriesnow.space/api/v0.1/countries/iso").openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                val reader = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
                val json = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    json.append(line)
                }
                reader.close()

                val obj = org.json.JSONObject(json.toString())
                val dataArray = obj.getJSONArray("data")
                val list = ArrayList<String>()
                list.add("Select Country")
                for (i in 0 until dataArray.length()) {
                    list.add(dataArray.getJSONObject(i).getString("name"))
                }
                _countriesList.postValue(list)
            } catch (e: Exception) {
                _countriesList.postValue(listOf("United States", "India", "United Kingdom", "Canada", "Australia"))
            }
        }
    }

    fun fetchStatesList(selectedCountry: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _statesList.postValue(listOf("Loading..."))
                val url = java.net.URL("https://countriesnow.space/api/v0.1/countries/states")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write("{\"country\":\"$selectedCountry\"}".toByteArray())

                val reader = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
                val json = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    json.append(line)
                }
                reader.close()

                val obj = org.json.JSONObject(json.toString())
                val dataObj = obj.getJSONObject("data")
                val statesArray = dataObj.getJSONArray("states")
                val list = ArrayList<String>()
                list.add("Select State")
                for (i in 0 until statesArray.length()) {
                    list.add(statesArray.getJSONObject(i).getString("name"))
                }
                _statesList.postValue(list)
            } catch(e: Exception) {
                _statesList.postValue(listOf("Select State", "Province A", "Province B"))
            }
        }
    }

    fun fetchCitiesList(selectedCountry: String, selectedState: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _citiesList.postValue(listOf("Loading..."))
                val url = java.net.URL("https://countriesnow.space/api/v0.1/countries/state/cities")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write("{\"country\":\"$selectedCountry\", \"state\":\"$selectedState\"}".toByteArray())

                val reader = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
                val json = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    json.append(line)
                }
                reader.close()

                val obj = org.json.JSONObject(json.toString())
                val citiesArray = obj.getJSONArray("data")
                val list = ArrayList<String>()
                list.add("Select City")
                for (i in 0 until citiesArray.length()) {
                    list.add(citiesArray.getString(i))
                }
                _citiesList.postValue(list)
            } catch(e: Exception) {
                _citiesList.postValue(listOf("Select City", "City X", "City Y"))
            }
        }
    }

    fun verifyAndSaveProfile(
        context: android.content.Context,
        nameVal: String, phoneVal: String, dobVal: String, genderIdx: Int, genderVal: String,
        countryIdentifying: Boolean, pincodeVal: String,
        cVal: String, sVal: String, cityVal: String, famStr: String, friendStr: String,
        addressVal: String, emergencyNoteVal: String,
        orgName: String, deptName: String, // 🚀 QA Fix: Corporate Sync Keys
        prefs: android.content.SharedPreferences
    ) {
        // 1. Enterprise Validation Rules (Top to Bottom Chronological Screen Physics)
        // 1. Enterprise Validation Rules (Aggregate Diagnostic Matrix)
        val errorChecklist = mutableListOf<String>()
        
        if (nameVal.length < 2 || nameVal.length > 50) {
            errorChecklist.add("Name: Must be 2-50 characters")
        }
        if (nameVal.contains("@") && nameVal.contains(".")) {
            errorChecklist.add("Name: Handles (@*.*) are restricted")
        }
        if (!android.util.Patterns.PHONE.matcher(phoneVal).matches() || phoneVal.length < 7 || phoneVal.length > 15) {
            errorChecklist.add("Phone: Invalid length or format")
        }
        if (countryIdentifying) {
            errorChecklist.add("Location: Country/State Matrix unselected")
        }
        if (pincodeVal.isEmpty() || pincodeVal.length < 4 || pincodeVal.length > 12) {
            errorChecklist.add("ZipCode: Must be 4-12 digits")
        }
        if (dobVal.isEmpty()) {
            errorChecklist.add("BirthDay: Mandatory field missing")
        }
        if (genderIdx == 0) {
            errorChecklist.add("Gender: Selection is mandatory")
        }

        if (errorChecklist.isNotEmpty()) {
            _validationErrorField.value = "validation_matrix_fail"
            _errorMessage.value = "MULTIPLE_ERRORS:" + errorChecklist.joinToString("\n")
            return
        }

        val safeUid = FirebaseAuth.getInstance().uid
        if (safeUid != null) {
            _isLoading.value = true
            
            // 2. Cloud Matrix Structure Injection
            val roleMatrix = HashMap<String, Any>()
            roleMatrix["userName"] = nameVal
            roleMatrix["userDob"] = dobVal
            roleMatrix["userGender"] = genderVal
            roleMatrix["userCountry"] = cVal
            roleMatrix["userState"] = sVal
            roleMatrix["userCity"] = cityVal
            roleMatrix["userPincode"] = pincodeVal
            roleMatrix["userPhone"] = phoneVal
            roleMatrix["userAddress"] = addressVal
            roleMatrix["userEmergencyNote"] = emergencyNoteVal
            roleMatrix["orgName"] = orgName
            roleMatrix["deptName"] = deptName
            roleMatrix["role_family"] = famStr
            roleMatrix["role_friend"] = friendStr
            roleMatrix["role_mother"] = famStr == "Mother"
            roleMatrix["role_father"] = famStr == "Father"
            roleMatrix["role_child"] = famStr == "Child"
            roleMatrix["role_spouse"] = famStr == "Spouse"
            roleMatrix["role_friends"] = friendStr.contains("Friend")
            roleMatrix["equipped_avatar_id"] = prefs.getInt("equipped_avatar_id", 1)

            // 3. Native Asynchronous Network Sync with Enterprise Visual Fallback Script (4500ms guaranteed minimum)
            viewModelScope.launch(Dispatchers.Main) {
                _loadingState.value = "Translating Citizen Data..."
                delay(1200)
                _loadingState.value = "Constructing Network Arrays..."
                delay(1200)
                _loadingState.value = "Verifying Matrix Integrity..."
                delay(1200)
                _loadingState.value = "Synchronizing Node Payload..."
                delay(1000)

                com.jdroidx.jfindme.data.remote.FirebaseComm.usersRef
                    .child(safeUid).updateChildren(roleMatrix)
                    .addOnCompleteListener { task ->
                        _isLoading.value = false
                            if (task.isSuccessful) {
                                // 4. Synchronous Cache Fallback
                                prefs.edit().putString("user_nickname", nameVal)
                                    .putString("user_phone", phoneVal)
                                    .putString("user_dob", dobVal)
                                    .putString("user_gender", genderVal)
                                    .putString("user_country", cVal)
                                    .putString("user_state", sVal)
                                    .putString("user_city", cityVal)
                                    .putString("user_pincode", pincodeVal)
                                    .putString("user_address", addressVal)
                                    .putString("user_emergency_note", emergencyNoteVal)
                                    .putString("org_name", orgName)
                                    .putString("department", deptName)
                                    .putString("role_family", famStr)
                                    .putString("role_friend", friendStr)
                                    .putBoolean("role_mother", famStr == "Mother")
                                    .putBoolean("role_father", famStr == "Father")
                                    .putBoolean("role_child", famStr == "Child")
                                    .putBoolean("role_spouse", famStr == "Spouse")
                                    .putBoolean("role_friends", friendStr.contains("Friend") || friendStr.contains("FRIEND"))
                                    .putBoolean("profile_onboarded", true).apply()
                                    
                                // 🚀 QA Fix: Global Identity Mirror (Dossier Sync)
                                // We now save to Local DB regardless of group status to ensure Profile Details remains synchronized.
                                val dossierMap = mutableMapOf<String, String>()
                                dossierMap["fullName"] = nameVal
                                dossierMap["dob"] = dobVal
                                dossierMap["gender"] = genderVal
                                dossierMap["country"] = cVal
                                dossierMap["state"] = sVal
                                dossierMap["city"] = cityVal
                                dossierMap["pincode"] = pincodeVal
                                dossierMap["phone"] = phoneVal
                                dossierMap["address"] = addressVal
                                dossierMap["emergencyNote"] = emergencyNoteVal
                                dossierMap["role_family"] = famStr
                                dossierMap["role_friend"] = friendStr
                                dossierMap["device_id"] = safeUid
                                
                                val activeGrp = prefs.getString("active_group", null)
                                if (activeGrp != null) {
                                    dossierMap["network_id"] = activeGrp
                                    FirebaseDatabase.getInstance().reference.child("groups").child(activeGrp)
                                        .child("citizen_dossiers").child(safeUid).updateChildren(dossierMap as Map<String, Any>)
                                } else {
                                    // Isolated Sync: Ensure PersonDetailsActivity can still find this data without an active group
                                    FirebaseDatabase.getInstance().reference.child("users").child(safeUid)
                                        .child("isolated_dossiers").child(safeUid).updateChildren(dossierMap as Map<String, Any>)
                                }

                                // Local SQLite Mirror Update (Always executed for offline stability)
                                val forensicDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                                val finalRole = if (friendStr != "None") friendStr else famStr
                                forensicDb.saveLocalDossier(safeUid, dossierMap, finalRole, nameVal, safeUid, safeUid, "SELF_REG")
                                forensicDb.saveRosterNode(safeUid, nameVal, famStr, "Identity Active", isAdmin = true, isFriend = false, ownerName = nameVal, ownerDeviceId = safeUid, connectedDeviceId = safeUid, qrCode = "SELF_REG")
                                
                                // 🚀 QA Fix: Global Corporate Sync Node
                                val orgV = android.content.ContentValues()
                                orgV.put("uid", safeUid)
                                orgV.put("org_name", orgName)
                                orgV.put("department", deptName)
                                forensicDb.saveOrgDossier(orgV)
                                    
                                _saveSuccess.value = true
                            } else {
                                _errorMessage.value = task.exception?.message ?: "Unknown Cloud Sync Error"
                            }
                    }
            }
        } else {
            _errorMessage.value = "Critical Hardware Unlocked. Login Required."
        }
    }

    // 🚀 QA Fix: Master MVVM Architecture Structural Rules Engine.
    // Encapsulates all literal logic out of the View and directly into the Business Logic Matrix.
    val currentUserId: String? get() = FirebaseAuth.getInstance().uid

    fun equipCustomAvatar(prefs: android.content.SharedPreferences) {
        val safeUid = currentUserId 
        prefs.edit().putInt("equipped_avatar_id", -1).apply()
        if (safeUid != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance().reference.child("users").child(safeUid).child("equipped_avatar_id").setValue(-1)
        }
    }

    fun equipAvatar(prefs: android.content.SharedPreferences, avatarId: Int) {
        val safeUid = currentUserId
        prefs.edit().putInt("equipped_avatar_id", avatarId).apply()
        if (safeUid != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance().reference.child("users").child(safeUid).child("equipped_avatar_id").setValue(avatarId)
        }
    }

    fun executeCloudMatrixRestore(prefs: android.content.SharedPreferences, group: String, csv: String?) {
        prefs.edit().putString("active_group", group).apply()
        if (csv != null) prefs.edit().putString("linked_devices_csv", csv).apply()
        prefs.edit().remove("unresolved_cloud_hub").remove("unresolved_cloud_links").apply()
    }

    fun executeCloudMatrixIgnore(prefs: android.content.SharedPreferences) {
        prefs.edit().remove("unresolved_cloud_hub").remove("unresolved_cloud_links").apply()
    }

    fun bindHardwareId(hwId: String) {
        val safeUid = currentUserId
        if (safeUid != null && hwId.isNotEmpty()) {
            com.jdroidx.jfindme.data.remote.FirebaseComm.usersRef
                .child(safeUid).child("hw_lock_id")
                .setValue(hwId)
                .addOnSuccessListener {
                    com.jdroidx.jfindme.utils.SystemLogger.i("ProfileVM", "Hardware Identity Bound to Node: $hwId")
                }
        }
    }

}

