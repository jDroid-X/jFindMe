package com.jdroidx.jfindme.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.data.model.DeviceRegistry
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * BaseViewModel: Core state management with Role Guard.
 */
abstract class BaseViewModel : ViewModel() {
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
        val userId = FirebaseAuth.getInstance().uid ?: return
        FirebaseComm.devicesRef.child(userId).limitToFirst(1)
            .get().addOnSuccessListener { snapshot ->
                val device = snapshot.children.firstOrNull()?.getValue(DeviceRegistry::class.java)
                _currentUserRole.postValue(device?.userRole ?: "VIEWER")
            }
    }

    fun isAdmin(): Boolean = _currentUserRole.value == "ADMIN"

    fun clearError() {
        _errorMessage.value = null
    }
}
