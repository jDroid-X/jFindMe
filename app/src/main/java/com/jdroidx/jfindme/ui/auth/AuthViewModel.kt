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
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.FirebaseErrorMapper
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.ValidationEngine
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.remote.FirebaseComm
import android.content.Context
import android.app.Application
import kotlinx.coroutines.*

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

        // AUDIT FIX [AUD-009]: Validate email and password BEFORE calling Firebase
        val emailCheck = ValidationEngine.validateEmail(cleanEmail)
        if (!emailCheck.isValid) {
            _errorMessage.value = emailCheck.errorMessage
            return
        }
        val passCheck = ValidationEngine.validatePassword(cleanPass)
        if (!passCheck.isValid) {
            _errorMessage.value = passCheck.errorMessage
            return
        }

        val domain = RegistryManager.getEnterpriseDomain(getApplication())
        val isSystemNode = cleanEmail.endsWith("@$domain")
        val dbHelper = DatabaseHelper.getInstance(getApplication())
        _isLoading.value = true

        SystemLogger.i("Handshake", "Initiating Analytical Identity Sync for Node: $cleanEmail")

        auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val authUid = auth.currentUser?.uid ?: ""
                    
                    // 🛡️ Forensic Bridge: Unify Hardware IDENTITY Protocol via DeviceIdentityHelper
                    val rawId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getRawAndroidId(getApplication())
                    val agentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(getApplication())
                    val hardwareUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(getApplication())

                    // 🚀 Step 1: Cloud Meta-Data Recovery (Fetch existing profile markers)
                    FirebaseComm.devicesRef.child(agentId).child("profile").get().addOnCompleteListener { profileTask ->
                        var finalName = nick
                        var finalEmail = cleanEmail
                        val defaultPersonalGroup = "GRP_" + agentId
                        var activeGroup = defaultPersonalGroup

                        if (profileTask.isSuccessful && profileTask.result != null && profileTask.result!!.exists()) {
                            val snap = profileTask.result!!
                            val cloudName = snap.child("name").getValue(String::class.java) ?: snap.child("full_name").getValue(String::class.java)
                            if (cloudName != null) finalName = cloudName

                            val cloudEmail = snap.child("email").getValue(String::class.java) ?: snap.child("email_id").getValue(String::class.java)
                            if (cloudEmail != null) finalEmail = cloudEmail

                            val savedGroup = snap.child("active_group").getValue(String::class.java)
                            activeGroup = if (!savedGroup.isNullOrBlank() && savedGroup != "DEFAULT_GROUP") savedGroup else defaultPersonalGroup
                            if (savedGroup.isNullOrBlank() || savedGroup == "DEFAULT_GROUP") {
                                FirebaseComm.devicesRef.child(agentId).child("profile").child("active_group").setValue(activeGroup)
                                FirebaseComm.relationshipsRef.child(activeGroup).child("members").child(agentId).child("role").setValue("OWNER")
                            }
                        } else {
                            // First time cloud registration or legacy profile recovery
                            FirebaseComm.devicesRef.child(agentId).child("profile").child("active_group").setValue(activeGroup)
                            FirebaseComm.relationshipsRef.child(activeGroup).child("members").child(agentId).child("role").setValue("OWNER")
                        }

                        // 🧬 V58.2 Alias-Identity Parity Enforcement
                        if (finalName.isEmpty() || finalName.startsWith("Agent_")) {
                            finalName = agentId
                        }

                        // 🧬 STEP 2: Execute Master Analytical Handshake with Epoch Isolation (SQLite Ledger)
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

                        val epochId = "EP_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().take(6)

                        dbHelper.executeTieredHandshake(
                            authUid, agentId, hardwareUid, rawId,
                            profileData, null, null, groupData, null, null, null,
                            lat, lng, addr, "ADMIN", epochId
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

                        // 🔄 STEP 4: Prune abandoned in-flight pairing sessions & purge rogue Auth UID nodes
                        FirebaseComm.cleanupAbandonedPairingSessions(agentId)
                        FirebaseComm.purgeLegacyAuthUidNodes()

                        // 📱 STEP 5: Push fresh FCM Token to Unified Schema
                        try {
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                                if (!fcmToken.isNullOrEmpty()) {
                                    FirebaseComm.devicesRef.child(agentId).child("Identity").child("fcm_token").setValue(fcmToken)
                                    FirebaseComm.devicesRef.child(agentId).child("device").child("fcm_token").setValue(fcmToken)
                                }
                            }
                        } catch (e: Exception) { }
                        FirebaseComm.registerLookupIndex(authUid, agentId)

                        // 🔄 STEP 6: Auto-rehydrate cloud history if local is clean
                        FirebaseComm.rehydrateCloudLocationHistory(authUid, agentId) { cloudPoints ->
                            if (cloudPoints.isNotEmpty()) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    for (pt in cloudPoints) {
                                        dbHelper.saveHistoryPoint(
                                            agentId, hardwareUid,
                                            pt["lat"] as Double, pt["lng"] as Double,
                                            pt["speed"] as Float, pt["battery"] as Int,
                                            false, pt["address"] as? String, (pt["timestamp"] as? String) ?: FirebaseComm.getForensicTimestamp()
                                        )
                                    }
                                    SystemLogger.i("AuthVM", "Rehydrated ${cloudPoints.size} historical trace points from cloud.")
                                }
                            }
                        }

                        // Persist session flags natively
                        val prefs = getApplication<Application>().getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putBoolean("auto_login_enabled", autoLoginEnabled)
                            putString("user_nickname", finalName)
                            putString("agent_id", agentId)
                            putString("install_epoch_id", epochId)
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
                        // AUDIT FIX [AUD-010]: Map raw Firebase error to user-friendly message
                        val errorCode = (exception as? FirebaseAuthException)?.errorCode
                        _errorMessage.value = FirebaseErrorMapper.mapAuthError(errorCode, exception?.message)
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

        // AUDIT FIX [AUD-003]: Move synchronous Geocoder off calling thread using coroutines
        // Geocoder.getFromLocation() is a blocking network call — must NOT run on Main or ViewModel scoped coroutine
        CoroutineScope(Dispatchers.IO).launch {
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
                        if (android.location.Geocoder.isPresent()) {
                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val resolvedAddr = addresses[0].getAddressLine(0)
                                deviceData["Address"] = resolvedAddr
                                SystemLogger.i("AuthVM", "High-Precision Baseline Fixed: $resolvedAddr")
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

            val personalGroupId = "GRP_" + agentId
            val profilePayload = profileData + mapOf("active_group" to personalGroupId)

            // Sync happens after geocoding completes (on IO thread — FirebaseComm is thread-safe)
            // 🧬 TIER 1: Atomic Cloud & Local Core Sync (Waterfall Heartbeat)
            FirebaseComm.syncForensicHeartbeat(uid, agentId, hardwareUid, profilePayload + deviceData, personalGroupId)

            // 🚀 V61.0: Static One-Time Anchor (Registration Baseline)
            FirebaseComm.syncRegistrationBaseline(agentId, deviceData)

            // 👑 Register personal root relationship group
            val nowMs = System.currentTimeMillis()
            FirebaseComm.relationshipsRef.child(personalGroupId).child("members").child(agentId).setValue(
                mapOf("role" to "OWNER", "ts_joined" to nowMs, "name" to name)
            )
            
            // 💾 V67.5: Master Forensic Handshake (Atomic Tiered Sync) with Epoch Isolation
            val epochId = "EP_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().take(6)
            val groupData = mapOf("group_id" to personalGroupId, "group_name" to "My Circle")
            dbHelper.executeTieredHandshake(
                uid, agentId, hardwareUid, rawHw,
                profilePayload.mapValues { it.value.toString() },
                null, null, groupData, null, null, null,
                (deviceData["latitude"] as? Double) ?: 0.0,
                (deviceData["longitude"] as? Double) ?: 0.0,
                deviceData["Address"] as? String,
                "ADMIN", epochId
            )

            // Persist session flags natively
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("user_nickname", name)
                putString("agent_id", agentId)
                putString("active_group", personalGroupId)
                putString("install_epoch_id", epochId)
                apply()
            }

            // 📱 Push fresh FCM Token to Unified Schema
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                    if (!fcmToken.isNullOrEmpty()) {
                        FirebaseComm.devicesRef.child(agentId).child("Identity").child("fcm_token").setValue(fcmToken)
                        FirebaseComm.devicesRef.child(agentId).child("device").child("fcm_token").setValue(fcmToken)
                    }
                }
            } catch (e: Exception) { }
            FirebaseComm.registerLookupIndex(uid, agentId)
            
            SystemLogger.i("AuthVM", "5-Tier Identity Core Initialized for Agent: $agentId (Epoch: $epochId)")
        }
    }

    fun logout() {
        auth.signOut()
        _user.value = null
    }
}
