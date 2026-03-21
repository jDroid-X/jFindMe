package com.jdroidx.jfindme.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jdroidx.jfindme.ui.base.BaseViewModel

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

    fun login(email: String, pass: String) {
        _isLoading.value = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isLoading.value = false
                    _user.value = auth.currentUser
                } else {
                    // 🧠 QA Fix: If the Suggested Random Identity doesn't exist yet, auto-register it!
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { regTask ->
                            _isLoading.value = false
                            if (regTask.isSuccessful) {
                                _user.value = auth.currentUser
                            } else {
                                // If registration ALSO fails, surface the original login error (e.g., Bad Password)
                                _errorMessage.value = task.exception?.message ?: regTask.exception?.message
                            }
                        }
                }
            }
    }

    fun logout() {
        auth.signOut()
        _user.value = null
    }
}
