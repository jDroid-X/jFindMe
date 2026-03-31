package com.jdroidx.jfindme.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jdroidx.jfindme.ui.base.BaseViewModel
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * AuthViewModel handles user authentication and session management.
 * Integration Point: Firebase Auth for secure login.
 */
class AuthViewModel : BaseViewModel() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    init {
        _user.value = auth.currentUser
    }

    @JvmOverloads
    fun login(email: String, pass: String, nick: String = "") {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()
        val isSystemNode = cleanEmail.endsWith("@jfindme.net")
        _isLoading.value = true
        
        SystemLogger.i("Handshake", "Initiating Identity Sync for Node: $cleanEmail")

        // 🚀 Simplified Phase: Direct Sign-In Gate
        auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isLoading.value = false
                    _user.value = auth.currentUser
                } else {
                    val exception = task.exception
                    val errorCode = (exception as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    
                    // 🚀 QA Fix: Master Onboarding Loop! 
                    // Treats credential rejection as a cue for fresh registration for System Nodes.
                    val isNewNode = exception is com.google.firebase.auth.FirebaseAuthInvalidUserException || 
                                    errorCode == "ERROR_USER_NOT_FOUND" || 
                                    errorCode == "ERROR_INVALID_CREDENTIAL" ||
                                    exception?.message?.contains("no user record", true) == true
                    
                    // 🚀 Simplified Phase 2: Silent Registration for System Nodes
                    if (isSystemNode && isNewNode) {
                        auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
                            .addOnCompleteListener { regTask ->
                                if (regTask.isSuccessful) {
                                    val uid = auth.currentUser?.uid ?: ""
                                    initializeNodeProfile(uid, cleanEmail, nick)
                                    _isLoading.value = false
                                    _user.value = auth.currentUser
                                } else {
                                    _isLoading.value = false
                                    val err = regTask.exception?.message ?: "Handshake Refused"
                                    val regCode = (regTask.exception as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                                    _errorMessage.value = "Handshake Failed: ${err.trimEnd('.')} [${regCode ?: "NULL"}]"
                                }
                            }
                    } else {
                        _isLoading.value = false
                        val msg = exception?.message ?: "Auth Denied"
                        val diagnostic = if (errorCode != null) " [$errorCode]" else ""
                        
                        _errorMessage.value = if (isSystemNode) 
                            "Node Sync Failed: ${msg.trimEnd('.')} $diagnostic. (Sync Device Time)" 
                            else "Login Failed: ${msg.trimEnd('.')} $diagnostic"
                        
                        SystemLogger.e("Handshake", "AUTH_FAILURE$diagnostic: $msg")
                    }
                }
            }
    }
    
    private fun initializeNodeProfile(uid: String, email: String, nick: String) {
        if (uid.isEmpty()) return
        val profileData = mapOf(
            "userName" to if (nick.isEmpty()) "User_${uid.takeLast(4)}" else nick,
            "email" to email,
            "system_mode" to "ACTIVE_NODE",
            "userDob" to "Pending",
            "userGender" to "Pending"
        )
        
        com.jdroidx.jfindme.data.remote.FirebaseComm.usersRef.child(uid).updateChildren(profileData)
            .addOnSuccessListener {
                SystemLogger.i("AuthVM", "Basic Identity Profile Initialized for UID: $uid")
            }
    }

    fun logout() {
        auth.signOut()
        _user.value = null
    }
}
