/**
 * Auto-Login Architecture: Enterprise Forensic ViewModel
 * Waterfall Method: Secured Data Layer & Relational Sync
 */
package com.jdroidx.jfindme.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.jdroidx.jfindme.ui.base.BaseViewModel
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.remote.FirebaseComm
import android.content.Context
import android.app.Application

/**
 * AuthViewModel handles user authentication and session management.
 * Integration Point: Firebase Auth for secure login.
 */
class AuthViewModel(application: Application) : BaseViewModel(application) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    // 🛡️ Forensic Gate: Prevents automatic jump to dashboard after reset/re-install
    private val _isManualStartTriggered = MutableLiveData<Boolean>(false)
    val isManualStartTriggered: LiveData<Boolean> get() = _isManualStartTriggered

    init {
        _user.value = auth.currentUser
    }

    fun triggerManualStart() {
        _isManualStartTriggered.value = true
    }

    @JvmOverloads
    fun login(email: String, pass: String, nick: String = "", autoLoginEnabled: Boolean = false, 
              lat: Double = 0.0, lng: Double = 0.0, addr: String = "") {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()
        val isSystemNode = cleanEmail.endsWith("@jfindme.net")
        val dbHelper = DatabaseHelper.getInstance(getApplication())
        _isLoading.value = true
        
        SystemLogger.i("Handshake", "Initiating Analytical Identity Sync for Node: $cleanEmail")

        auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val authUid = auth.currentUser?.uid ?: ""
                    
                    // 🛡️ Forensic Bridge: Unify Hardware IDENTITY Protocol (V58.1 Standard)
                    val rawId = android.provider.Settings.Secure.getString(getApplication<android.app.Application>().contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "AGENT_L7_NODE"
                    val hardwareUid = "HW_" + rawId.uppercase()
                    val agentId = dbHelper.resolveForensicAgentId(hardwareUid)

                    // 🚀 Step 1: Cloud Meta-Data Recovery (Fetch existing profile markers)
                    FirebaseComm.devicesRef.child(agentId).child("profile").get().addOnCompleteListener { profileTask ->
                        var finalName = nick
                        var finalEmail = cleanEmail
                        var activeGroup = "DEFAULT_GROUP"
                        
                        if (profileTask.isSuccessful && profileTask.result != null && profileTask.result!!.exists()) {
                            val snap = profileTask.result!!
                            val cloudName = snap.child("name").getValue(String::class.java) ?: snap.child("full_name").getValue(String::class.java)
                            if (cloudName != null) finalName = cloudName
                            
                            val cloudEmail = snap.child("email").getValue(String::class.java) ?: snap.child("email_id").getValue(String::class.java)
                            if (cloudEmail != null) finalEmail = cloudEmail
                            
                            activeGroup = snap.child("active_group").getValue(String::class.java) ?: "DEFAULT_GROUP"
                        }
                        
                        // 🧬 V58.2 Alias-Identity Parity Enforcement
                        if (finalName.isEmpty() || finalName.startsWith("Agent_")) {
                            finalName = agentId
                        }

                        // 🧬 STEP 2: Execute Master Analytical Handshake (SQLite Ledger)
                        val profileData = mapOf(
                            "name" to finalName,
                            "email" to finalEmail,
                            "model" to android.os.Build.MODEL,
                            "fingerprint" to android.os.Build.FINGERPRINT,
                            "imei" to "REDACTED"
                        )
                        
                        val groupData = mapOf(
                            "group_id" to activeGroup,
                            "group_name" to "Primary Hub"
                        )

                        dbHelper.executeTieredHandshake(
                            authUid, agentId, hardwareUid, rawId,
                            profileData, null, null, groupData, null, null, null,
                            lat, lng, addr
                        )

                        // 📡 STEP 3: One-Click Cloud Matrix Parity (Waterfall Heartbeat)
                        val forensicTs = FirebaseComm.getForensicTimestamp()
                        val cloudPayload = mapOf(
                            "name" to finalName,
                            "email" to finalEmail,
                            "model" to android.os.Build.MODEL,
                            "fingerprint" to android.os.Build.FINGERPRINT,
                            "active_group" to activeGroup,
                            "updated_at" to forensicTs
                        )
                        
                        FirebaseComm.syncForensicHeartbeat(authUid, agentId, hardwareUid, cloudPayload, activeGroup)

                        // Persist session flags natively
                        val prefs = getApplication<Application>().getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putBoolean("auto_login_enabled", autoLoginEnabled)
                            putString("user_nickname", finalName)
                            putString("agent_id", agentId)
                            putBoolean("profile_bypass_nudge", false) // Reset nudge bypass for new session
                            apply()
                        }

                        _isLoading.value = false
                        _user.value = auth.currentUser
                        _isManualStartTriggered.value = true // Login success triggers manual start parity
                    }
                } else {
                    val exception = task.exception
                    val errorCode = (exception as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    
                    val isNewNode = exception is com.google.firebase.auth.FirebaseAuthInvalidUserException || 
                                    errorCode == "ERROR_USER_NOT_FOUND" || 
                                    errorCode == "ERROR_INVALID_CREDENTIAL"
                    
                    if (isSystemNode && isNewNode) {
                        auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
                            .addOnCompleteListener { regTask ->
                                if (regTask.isSuccessful) {
                                     val uid = auth.currentUser?.uid ?: ""
                                     initializeNodeProfile(getApplication(), uid, cleanEmail, nick, lat, lng, addr)
                                    _isLoading.value = false
                                    _user.value = auth.currentUser
                                    _isManualStartTriggered.value = true // Registration success triggers manual start parity
                                } else {
                                    _isLoading.value = false
                                    _errorMessage.value = "Handshake Failed: ${regTask.exception?.message}"
                                }
                            }
                    } else {
                        _isLoading.value = false
                        _errorMessage.value = "Auth Denied: ${exception?.message}"
                    }
                }
            }
    }
    
    private fun initializeNodeProfile(context: Context, uid: String, email: String, nick: String, 
                                      manualLat: Double = 0.0, manualLng: Double = 0.0, manualAddr: String = "") {
        if (uid.isEmpty()) return
        val dbHelper = DatabaseHelper.getInstance(context)
        val rawHw = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "NODE"
        
        // 🧬 V58.2 Alias-Identity Alignment
        val hardwareUid = "HW_" + rawHw.uppercase()
        val agentId = dbHelper.resolveForensicAgentId(hardwareUid)
        
        // Ensure the Display Name defaults to the Forensic Agent ID to maintain parity
        val name = if (nick.isEmpty() || nick.startsWith("Agent_")) agentId else nick
        
        val timeStampSign = FirebaseComm.getForensicTimestamp()

        val profileData = mapOf(
            "name" to name,
            "email" to email,
            "auth_uid" to uid,
            "system_mode" to "ACTIVE_NODE",
            "ts_onboarded" to timeStampSign
        )
        
        // 🧬 V61.5 Registration Location Baseline (Latitude, Longitude, Address)
        val deviceData = mutableMapOf<String, Any>(
            "model" to android.os.Build.MODEL,
            "fingerprint" to android.os.Build.FINGERPRINT,
            "os_version" to android.os.Build.VERSION.RELEASE,
            "ts_registered" to timeStampSign,
            "latitude" to if (manualLat != 0.0) manualLat else 0.0,
            "longitude" to if (manualLng != 0.0) manualLng else 0.0,
            "Address" to if (manualAddr.isNotEmpty()) manualAddr else "Locating..."
        )

        // Attempt to capture registration-time location baseline (V61.5 Precise Anchor)
        try {
            if (manualLat != 0.0 && manualLng != 0.0) {
                deviceData["latitude"] = manualLat
                deviceData["longitude"] = manualLng
                if (manualAddr.isNotEmpty()) deviceData["Address"] = manualAddr
                SystemLogger.i("AuthVM", "Manual Geographic Override Accepted: $manualAddr")
            } else {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                var lastLoc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                if (lastLoc == null) lastLoc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                if (lastLoc == null) lastLoc = lm.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)

                if (lastLoc != null) {
                    deviceData["latitude"] = lastLoc.latitude
                    deviceData["longitude"] = lastLoc.longitude
                    
                    // Fetch Address at time of registration (Reverse Geocoding)
                    if (android.location.Geocoder.isPresent()) {
                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0].getAddressLine(0)
                            deviceData["Address"] = addr
                            SystemLogger.i("AuthVM", "High-Precision Baseline Fixed: $addr")
                        } else {
                            deviceData["Address"] = "Unmarked Digital Node [${lastLoc.latitude}, ${lastLoc.longitude}]"
                        }
                    }
                } else {
                    deviceData["Address"] = "No GNSS Baseline Found"
                }
            }
        } catch (e: Exception) { 
            deviceData["Address"] = "Location Permission / Sensor Disabled"
            SystemLogger.e("AuthVM", "Baseline Failure: ${e.message}")
        }

        // 🧬 TIER 1: Atomic Cloud & Local Core Sync (Waterfall Heartbeat)
        FirebaseComm.syncForensicHeartbeat(uid, agentId, hardwareUid, profileData + deviceData)
        
        // 🚀 V61.0: Static One-Time Anchor (Registration Baseline)
        FirebaseComm.syncRegistrationBaseline(agentId, deviceData)
        
        // 💾 V67.5: Master Forensic Handshake (Atomic Tiered Sync)
        val groupData = mapOf("group_id" to "DEFAULT_GROUP", "group_name" to "Family Circle")
        dbHelper.executeTieredHandshake(
            uid, agentId, hardwareUid, hardwareUid,
            profileData.mapValues { it.value.toString() },
            null, null, groupData, null, null, null,
            (deviceData["latitude"] as? Double) ?: 0.0,
            (deviceData["longitude"] as? Double) ?: 0.0,
            deviceData["Address"] as? String,
            "ADMIN"
        )
        
        SystemLogger.i("AuthVM", "5-Tier Identity Core Initialized for Agent: $agentId")
    }

    fun logout() {
        auth.signOut()
        _user.value = null
    }
}
