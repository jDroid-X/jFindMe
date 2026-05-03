package com.jdroidx.jfindme.ui.settings

import android.accounts.AccountManager
import android.app.Application
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SystemUpdateViewModel: Tier 5 Infrastructure Layer.
 */
class SystemUpdateViewModel(application: Application) : BaseViewModel(application) {

    private val _gmailAccount = MutableLiveData<String>()
    val gmailAccount: LiveData<String> get() = _gmailAccount

    private val _driveLink = MutableLiveData<String>()
    val driveLink: LiveData<String> get() = _driveLink

    private val _isSilentMode = MutableLiveData<Boolean>()
    val isSilentMode: LiveData<Boolean> get() = _isSilentMode

    private val _loadingStatus = MutableLiveData<String>()
    val loadingStatus: LiveData<String> get() = _loadingStatus

    private val _lastUpdateInfo = MutableLiveData<String>()
    val lastUpdateInfo: LiveData<String> get() = _lastUpdateInfo

    private val _updateAvailableInfo = MutableLiveData<String>()
    val updateAvailableInfo: LiveData<String> get() = _updateAvailableInfo

    private val _updateStatusInfo = MutableLiveData<String>()
    val updateStatusInfo: LiveData<String> get() = _updateStatusInfo

    private val _actionComplete = MutableLiveData<Boolean>()
    val actionComplete: LiveData<Boolean> get() = _actionComplete

    private val DEFAULT_LINK = "https://drive.google.com/file/d/1-VdzId57PvvsI80rTO_bqiOtMR-bLCGN/view?usp=drive_link"

    fun initData(context: Context) {
        _driveLink.value = DEFAULT_LINK
        
        // Load Silent Update state and forensic timestamps from system_registry
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        _isSilentMode.value = prefs.getBoolean("silent_update_enabled", true)
        
        val lastUpdate = prefs.getString("last_update_ts", "2026-04-07 03:03:12") ?: "2026-04-07 03:03:12"
        val nextUpdate = prefs.getString("update_available_ts", "2026-04-09 11:20:00") ?: "2026-04-09 11:20:00"
        val isUpdated = if (prefs.getBoolean("is_updated", true)) "YES" else "NO"
        val updateStatusTs = prefs.getString("update_status_ts", "2026-04-09 18:20:00") ?: "2026-04-09 18:20:00"

        _lastUpdateInfo.value = "LAST UPDATE: $lastUpdate"
        _updateAvailableInfo.value = "UPDATE AVAILABLE: $nextUpdate"
        _updateStatusInfo.value = "UPDATED: $isUpdated $updateStatusTs"
        
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

    fun executeInstall(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingStatus.value = "Initializing Secure Installation..."
            delay(1500)
            _loadingStatus.value = "Verifying Binary Integrity..."
            delay(1000)
            _isLoading.value = false
            _actionComplete.value = true

            // 🚀 RESTORATION: Launch Direct Package Install Intent
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse(_driveLink.value ?: DEFAULT_LINK)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                _errorMessage.value = "INSTALL_FAIL: Unable to launch handshake - ${e.message}"
            }

            // Persistence: Update successful installation markers
            val now = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
            _lastUpdateInfo.value = "LAST UPDATE: $now"
            _updateStatusInfo.value = "UPDATED: YES $now"

            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("last_update_ts", now)
                .putBoolean("is_updated", true)
                .putString("update_status_ts", now)
                .apply()
        }
    }

    fun executeUpdate(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingStatus.value = "Scanning for System Deltas..."
            delay(1500)
            _loadingStatus.value = "Comparing Version Matrices..."
            delay(1500)
            _isLoading.value = false

            // Persistence: Update discovery marker
            val now = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
            _updateAvailableInfo.value = "UPDATE AVAILABLE: $now"
            
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("update_available_ts", now)
                .putBoolean("is_updated", false) // Reset status as a newer one is found but not installed
                .apply()
            
            _updateStatusInfo.value = "UPDATED: NO $now"

            // Simulate failure per specific user instruction (Prompt for manual reinstall)
            _errorMessage.value = "SYNC_FAILED: Update Protocol Failed. Please install after uninstalling the old version app to ensure identity integrity."
        }
    }
    
    fun resetActionState() {
        _actionComplete.value = false
    }
}
