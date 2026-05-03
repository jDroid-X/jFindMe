package com.jdroidx.jfindme.ui.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.model.DeviceRegistry
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * BaseViewModel: Core state management with Role Guard.
 */
abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    protected val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    protected val _currentUserRole = MutableLiveData<String>("VIEWER")
    val currentUserRole: LiveData<String> get() = _currentUserRole

    init {
        fetchCurrentUserRole()
    }

    private fun fetchCurrentUserRole() {
        val authUid = FirebaseAuth.getInstance().uid ?: return
        
        // 🧬 V59.5.3: Resolve Agent ID before querying the Hardware Matrix
        FirebaseComm.resolveHardwareIdentity(authUid) { agentId ->
            val finalId = agentId ?: return@resolveHardwareIdentity
            
            FirebaseComm.devicesRef.child(finalId).limitToFirst(1)
                .get().addOnSuccessListener { snapshot ->
                    // 🚀 V59.5 Compatibility: Handle both legacy POJO and new Map-based registry
                    val firstChild = snapshot.children.firstOrNull()
                    val role = firstChild?.child("userRole")?.getValue(String::class.java) ?: "VIEWER"
                    _currentUserRole.postValue(role)
                }
        }
    }

    fun isAdmin(): Boolean = _currentUserRole.value == "ADMIN"

    fun clearError() {
        _errorMessage.value = null
    }
}
