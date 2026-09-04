package com.jdroidx.jfindme.ui.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.ui.base.BaseViewModel
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ProfileViewModel: Tier 1 Identity Hub Manager.
 */
class ProfileViewModel(application: Application) : BaseViewModel(application) {
    private val _saveSuccess = MutableLiveData<Boolean?>()
    val saveSuccess: LiveData<Boolean?> get() = _saveSuccess

    fun resetSaveState() { _saveSuccess.value = null }

    private val _profileData = MutableLiveData<Map<String, Any>>()
    val profileData: LiveData<Map<String, Any>> get() = _profileData

    private val _loadingState = MutableLiveData<String>()
    val loadingState: LiveData<String> get() = _loadingState

    private val _validationErrorField = MutableLiveData<String>()
    val validationErrorField: LiveData<String> get() = _validationErrorField

    private val _countriesList = MutableLiveData<List<String>>()
    val countriesList: LiveData<List<String>> get() = _countriesList

    private val _statesList = MutableLiveData<List<String>>()
    val statesList: LiveData<List<String>> get() = _statesList

    private val _citiesList = MutableLiveData<List<String>>()
    val citiesList: LiveData<List<String>> get() = _citiesList

    @JvmOverloads
    fun fetchProfile(context: Context = getApplication()) {
        // 🧬 Hardware Root Anchor: Master Identity Resolution anchored to silicon identity
        val safeUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        processProfileWithAgentId(context, safeUid)
    }

    private fun processProfileWithAgentId(context: Context, safeUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = DatabaseHelper.getInstance(context)
            val localDossier = db.getLocalDossier(safeUid)
            
            if (localDossier.isNotEmpty()) {
                _profileData.postValue(localDossier)
                SystemLogger.i("IdentityHub", "Local Tier 1 Truth Verified for $safeUid")
            } else {
                // 🛡️ TIER 2 FALLBACK: Pre-populate from Hardware Identity
                val fallback = db.getIdentityProfileFallback(safeUid).toMutableMap()
                
                // 🧪 Inject Email from Registry if missing
                val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                val regEmail = prefs.getString("email", "")
                if (!regEmail.isNullOrEmpty()) {
                    fallback["email"] = regEmail
                    fallback["userEmail"] = regEmail
                }

                if (fallback.isNotEmpty()) {
                    _profileData.postValue(fallback)
                    SystemLogger.i("IdentityHub", "Tier 2 Fallback Active for $safeUid")
                }
            }

            auditHardwareSignature(context)

            // 🧬 FALLBACK: Deep-Cloud Lookup if local dossier is sparse
            if (localDossier.isEmpty() || localDossier["fullName"].isNullOrEmpty()) {
                FirebaseComm.devicesRef.child(safeUid).child("profile").get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val cloudMap = snapshot.value as? Map<String, Any> ?: return@addOnSuccessListener
                        val mergedMap = _profileData.value?.toMutableMap() ?: mutableMapOf()
                        cloudMap.forEach { (k, v) -> mergedMap[k] = v.toString() }
                        
                        _profileData.postValue(mergedMap)
                        viewModelScope.launch(Dispatchers.IO) { 
                            val dossier = mergedMap.mapValues { (_, value) -> value.toString() }
                            val finalName = (mergedMap["userName"] ?: mergedMap["fullName"])?.toString()
                            db.commitAnalyticalDossier(safeUid, dossier, name = finalName) 
                        }
                    }
                }
            }
        }
    }

    private fun auditHardwareSignature(context: Context) {
        val authUid = FirebaseAuth.getInstance().uid ?: return
        
        // 🧬 Hardware Root Anchor: Unified Forensic Audit anchored to local device
        val finalAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        
        viewModelScope.launch(Dispatchers.IO) {
                val db = DatabaseHelper.getInstance(context)
                
                // 🧬 V69.8: Hydrated Heartbeat (The Waterfall Fix)
                // Fetch the COMPLETE local truth before syncing to prevent cloud-wiping of identity/location metadata.
                val localTruth = db.getForensicDossierComplete(finalAgentId)
                
                val hardwareMetadata = mutableMapOf<String, Any>(
                    "model" to android.os.Build.MODEL,
                    "fingerprint" to android.os.Build.FINGERPRINT,
                    "os_version" to android.os.Build.VERSION.RELEASE,
                    "manufacturer" to android.os.Build.MANUFACTURER
                )

                // Merge Local Truth + Hardware Metadata
                val fullPayload = localTruth.toMutableMap()
                fullPayload.putAll(hardwareMetadata)

                // TIER 2: Hardware Matrix Alignment (Waterfall Heartbeat)
                val authUid = FirebaseAuth.getInstance().uid ?: return@launch
                val hardwareId = fullPayload["uid_2"]?.toString()?.takeIf { it.isNotBlank() } ?: com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(context)
                
                FirebaseComm.syncForensicHeartbeat(authUid, finalAgentId, hardwareId, fullPayload)
                db.syncDeviceAnalytical(finalAgentId, hardwareId, hardwareMetadata["model"]?.toString(), "REDACTED", false, hardwareMetadata["fingerprint"]?.toString())
            }
    }

    @JvmOverloads
    fun verifyAndSaveProfile(
        context: Context,
        nameVal: String, phoneVal: String, dobVal: String, genderIdx: Int, genderVal: String,
        countryIdentifying: Boolean, pincodeVal: String,
        cVal: String, sVal: String, cityVal: String, famStr: String, friendStr: String,
        addressVal: String, emergencyNoteVal: String,
        orgName: String, deptName: String,
        prefs: SharedPreferences,
        ignoreValidation: Boolean = false
    ) {
        val missingFields = mutableListOf<String>()
        if (nameVal.isEmpty()) missingFields.add("Name")
        if (phoneVal.isEmpty()) missingFields.add("Mobile Number")
        if (dobVal.isEmpty() || dobVal == "Select Birthday") missingFields.add("Birthday")
        if (genderVal.isEmpty() || genderVal == "Select Gender") missingFields.add("Gender")
        if (cVal.isEmpty() || cVal == "Select Country") missingFields.add("Country")
        if (sVal.isEmpty() || sVal == "Select State") missingFields.add("State")
        if (cityVal.isEmpty() || cityVal == "Select City") missingFields.add("City")
        if (pincodeVal.isEmpty()) missingFields.add("Pincode / ZipCode")

        val authUid = FirebaseAuth.getInstance().uid ?: return
        _isLoading.value = true
        
        val safeUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        if (safeUid.isEmpty()) {
             _isLoading.postValue(false)
             return
        }
        
        viewModelScope.launch(Dispatchers.Main) {
                val db = DatabaseHelper.getInstance(context)
                val existingTruth = withContext(Dispatchers.IO) { db.getForensicDossierComplete(safeUid) }
                val rawBaseId = prefs.getInt("equipped_avatar_id", 1)
                val dynIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(rawBaseId)
                val avatarId = if (dynIdx == -1) "avatar_custom_photo" else "avatar_custom_$dynIdx"
                val hardwareId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(context)
                
                val profilePayload = existingTruth.toMutableMap()
                profilePayload.putAll(mapOf(
                    "name" to nameVal, 
                    "avatar_id" to avatarId,
                    "phone" to phoneVal, 
                    "dob" to dobVal, 
                    "gender" to genderVal, 
                    "country" to cVal,
                    "state" to sVal, 
                    "city" to cityVal, 
                    "pincode" to pincodeVal,
                    "address" to addressVal, 
                    "emergency_note" to emergencyNoteVal, 
                    "org_name" to orgName, 
                    "dept_name" to deptName,
                    "updated_at" to FirebaseComm.getForensicTimestamp()
                ))

                prefs.edit()
                    .putString("user_nickname", nameVal)
                    .putString("fullName", nameVal)
                    .putString("userName", nameVal)
                    .putString("owner_name", nameVal)
                    .apply()

                val activeGrp = _profileData.value?.get("active_group") as? String
                FirebaseComm.syncForensicHeartbeat(authUid, safeUid, hardwareId, profilePayload, activeGrp)
                FirebaseComm.devicesRef.child(safeUid).child("device").child("live").child("avatar_id").setValue(avatarId)
                
                launch(Dispatchers.IO) {
                    val dossier = profilePayload.mapValues { (_, value) -> value.toString() }
                    val orgDossier = mapOf("orgName" to orgName, "deptName" to deptName, "userAddress" to addressVal, "designation" to "Professional")
                    val effectiveGrp = if (!activeGrp.isNullOrBlank() && activeGrp != "DEFAULT_GROUP") activeGrp else "GRP_$safeUid"
                    val groupData = mapOf("group_id" to effectiveGrp, "group_name" to "My Circle")
                    
                    db.executeTieredHandshake(
                        authUid, safeUid, "HW_${hardwareId.uppercase()}", hardwareId,
                        dossier, orgDossier, null, groupData, null, null, null
                    )
                    
                    FirebaseComm.logChange(safeUid, FirebaseComm.LOG_ITEM_PROFILE, "PROFILE_SAVE", safeUid, profilePayload)
                    com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.propagateAvatarUpdate(safeUid, avatarId, nameVal)
                }
                    
                launch(Dispatchers.Main) {
                    _isLoading.value = false
                    if (missingFields.isEmpty()) {
                        _saveSuccess.value = true
                    } else if (!ignoreValidation) {
                        _saveSuccess.value = false
                        _errorMessage.value = "MULTIPLE_ERRORS:" + missingFields.joinToString("\n")
                    } else {
                        _saveSuccess.value = true
                    }
                }
            }
        }

    init {
        _countriesList.value = listOf(
            "India", "USA", "Indonesia", "UK", "Canada", "Australia", 
            "UAE", "Singapore", "Japan", "Germany", "France", "Brazil", "Russia"
        )
        _statesList.value = listOf("Select Country First")
        _citiesList.value = listOf("Select State First")
    }

    fun onCountrySelected(country: String) {
        val list = when (country) {
            "India" -> listOf("Maharashtra", "Delhi", "Karnataka", "Tamil Nadu", "Gujarat", "Other")
            "USA" -> listOf("California", "Texas", "New York", "Florida", "Illinois", "Other")
            "UK" -> listOf("England", "Scotland", "Wales", "Northern Ireland", "Other")
            else -> listOf("State 1", "State 2", "Select Other")
        }
        _statesList.value = list
        _citiesList.value = emptyList()
    }

    fun onStateSelected(state: String) {
        val list = when (state) {
            "Maharashtra" -> listOf("Mumbai", "Pune", "Nagpur", "Nashik", "Aurangabad", "Other")
            "California" -> listOf("Los Angeles", "San Francisco", "San Diego", "San Jose", "Other")
            "Texas" -> listOf("Houston", "Austin", "Dallas", "San Antonio", "Other")
            "England" -> listOf("London", "Manchester", "Birmingham", "Liverpool", "Other")
            "Delhi" -> listOf("New Delhi", "North Delhi", "South Delhi", "West Delhi", "East Delhi", "Other")
            "Karnataka" -> listOf("Bengaluru", "Mysuru", "Mangaluru", "Hubballi", "Belagavi", "Other")
            "Tamil Nadu" -> listOf("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Other")
            "Gujarat" -> listOf("Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Other")
            else -> listOf("City Zone 1", "City Zone 2", "Select Other")
        }
        _citiesList.value = list
    }

    fun fetchStatesList(country: String) {
        val list = when (country) {
            "India" -> listOf("Maharashtra", "Delhi", "Karnataka", "Tamil Nadu", "Gujarat", "Uttar Pradesh", "West Bengal", "Telangana", "Rajasthan", "Punjab", "Haryana", "Bihar", "Madhya Pradesh")
            "USA" -> listOf("California", "Texas", "New York", "Florida", "Illinois", "Washington", "Pennsylvania", "Ohio", "Georgia", "North Carolina", "Michigan")
            "Indonesia" -> listOf("DKI Jakarta", "Jawa Barat", "Jawa Timur", "Jawa Tengah", "Banten", "Sumatera Utara", "Bali", "Sulawesi Selatan")
            "UK" -> listOf("England", "Scotland", "Wales", "Northern Ireland")
            else -> listOf("State 1", "State 2", "Select Other")
        }
        _statesList.value = list
    }

    fun fetchCitiesList(country: String, state: String) {
        val list = when (state) {
            "Maharashtra" -> listOf("Mumbai", "Pune", "Nagpur", "Nashik", "Aurangabad", "Thane")
            "Delhi" -> listOf("New Delhi", "North Delhi", "South Delhi", "West Delhi", "East Delhi")
            "Karnataka" -> listOf("Bengaluru", "Mysuru", "Hubballi", "Mangaluru", "Belagavi")
            "Gujarat" -> listOf("Ahmedabad", "Surat", "Vadodara", "Rajkot")
            "Tamil Nadu" -> listOf("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli")
            "California" -> listOf("Los Angeles", "San Francisco", "San Diego", "San Jose", "Sacramento")
            "Texas" -> listOf("Houston", "Dallas", "Austin", "San Antonio", "Fort Worth")
            "New York" -> listOf("New York City", "Buffalo", "Rochester", "Yonkers", "Syracuse")
            "DKI Jakarta" -> listOf("Jakarta Pusat", "Jakarta Selatan", "Jakarta Barat", "Jakarta Timur", "Jakarta Utara")
            "Jawa Barat" -> listOf("Bandung", "Bekasi", "Depok", "Bogor", "Cimahi")
            "Jawa Timur" -> listOf("Surabaya", "Malang", "Sidoarjo", "Gresik", "Kediri")
            "Banten" -> listOf("Tangerang", "Serang", "Cilegon")
            else -> listOf("City Zone 1", "City Zone 2", "Select Other")
        }
        _citiesList.value = list
    }

    fun fetchNetworkLocation() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
        }
    }

    fun equipAvatar(prefs: SharedPreferences, id: Int, vrmPath: String?) {
        val editor = prefs.edit().putInt("equipped_avatar_id", id)
        if (vrmPath != null) editor.putString("equipped_vrm_string", vrmPath)
        editor.apply()
        
        val dynIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(id)
        val resolvedAvatarId = if (dynIdx == -1) "avatar_custom_photo" else "avatar_custom_$dynIdx"
        
        val authUid = FirebaseAuth.getInstance().uid ?: return
        val finalId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(getApplication())
        val updates = mutableMapOf<String, Any>(
                "equipped_avatar_id" to id,
                "avatar_id" to resolvedAvatarId,
                "updated_at" to FirebaseComm.getForensicTimestamp()
            )
            if (vrmPath != null) updates["equipped_vrm_string"] = vrmPath
            
            FirebaseComm.devicesRef.child(finalId).child("profile").updateChildren(updates)
            FirebaseComm.devicesRef.child(finalId).child("device").child("live").child("avatar_id").setValue(resolvedAvatarId)
            
            try {
                val db = DatabaseHelper.getInstance(getApplication())
                val nick = prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker"
                db.updateDeviceIdentityAndAvatar(finalId, nick, resolvedAvatarId)
            } catch (e: Exception) {}

            com.jdroidx.jfindme.utils.AvatarLoader.clearCache()

            com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.propagateAvatarUpdate(
                finalId, 
                resolvedAvatarId, 
                prefs.getString("user_nickname", "Active Tracker") ?: "Active Tracker"
            )
            FirebaseComm.logChange(finalId, FirebaseComm.LOG_ITEM_PROFILE, "AVATAR_EQUIP", finalId, updates)
    }

    fun equipAvatar(prefs: SharedPreferences, id: Int) {
        equipAvatar(prefs, id, null)
    }

    fun executeCloudMatrixRestore(prefs: SharedPreferences, group: String, csv: String?) {
        prefs.edit().putString("active_group", group).apply()
        if (csv != null) prefs.edit().putString("linked_devices_csv", csv).apply()
        prefs.edit().remove("unresolved_cloud_hub").remove("unresolved_cloud_links").apply()
    }

    fun executeCloudMatrixIgnore(prefs: SharedPreferences) {
        prefs.edit().remove("unresolved_cloud_hub").remove("unresolved_cloud_links").apply()
    }

    val currentUserId: String? get() = FirebaseAuth.getInstance().uid

    fun equipCustomAvatar(prefs: SharedPreferences) {
        // Equip standard ID -1 for custom hardware photos
        equipAvatar(prefs, -1, null)
    }

    /**
     * 🚀 QA Logic: Identity Matrix Integrity - Check for missing fields
     * Returns a string with missing mandatory fields or null if complete.
     */
    fun getMissingMandatoryFields(prefs: SharedPreferences): String? {
        val missing = mutableListOf<String>()
        if (prefs.getString("user_nickname", "").isNullOrEmpty()) missing.add("Name")
        if (prefs.getString("user_phone", "").isNullOrEmpty()) missing.add("Mobile Number")
        if (prefs.getString("user_dob", "").isNullOrEmpty()) missing.add("Birthday")
        val gender = prefs.getString("user_gender", "Select Gender")
        if (gender.isNullOrEmpty() || gender == "Select Gender") missing.add("Gender")
        val country = prefs.getString("user_country", "Select Country")
        if (country.isNullOrEmpty() || country == "Select Country" || country == "Identifying...") missing.add("Country")
        val state = prefs.getString("user_state", "Select State")
        if (state.isNullOrEmpty() || state == "Select State" || state == "Identifying...") missing.add("State")
        val city = prefs.getString("user_city", "Select City")
        if (city.isNullOrEmpty() || city == "Select City" || city == "Identifying...") missing.add("City")
        if (prefs.getString("user_pincode", "").isNullOrEmpty()) missing.add("Pincode / ZipCode")
        
        return if (missing.isEmpty()) null else missing.joinToString("\n")
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
    }
}
