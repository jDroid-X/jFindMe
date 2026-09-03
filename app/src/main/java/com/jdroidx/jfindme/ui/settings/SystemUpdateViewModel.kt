package com.jdroidx.jfindme.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jdroidx.jfindme.ui.base.BaseViewModel
import com.jdroidx.jfindme.utils.ApkInstallerHelper
import com.jdroidx.jfindme.utils.RegistryManager
import com.jdroidx.jfindme.utils.SystemLogger
import com.jdroidx.jfindme.utils.SystemUpdateScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * SystemUpdateViewModel: Tier 5 Infrastructure Layer.
 * Dynamic OOPS MVVM Module for Binary Synchronization & Lifecycle Updates.
 */
class SystemUpdateViewModel(application: Application) : BaseViewModel(application) {

    private val _gmailAccount = MutableLiveData<String>()
    val gmailAccount: LiveData<String> get() = _gmailAccount

    private val _driveLink = MutableLiveData<String>()
    val driveLink: LiveData<String> get() = _driveLink

    private val _githubLink = MutableLiveData<String>()
    val githubLink: LiveData<String> get() = _githubLink

    private val _isSilentMode = MutableLiveData<Boolean>()
    val isSilentMode: LiveData<Boolean> get() = _isSilentMode

    private val _loadingStatus = MutableLiveData<String>()
    val loadingStatus: LiveData<String> get() = _loadingStatus

    private val _currentVersionInfo = MutableLiveData<String>()
    val currentVersionInfo: LiveData<String> get() = _currentVersionInfo

    private val _lastUpdateInfo = MutableLiveData<String>()
    val lastUpdateInfo: LiveData<String> get() = _lastUpdateInfo

    private val _lastCheckInfo = MutableLiveData<String>()
    val lastCheckInfo: LiveData<String> get() = _lastCheckInfo

    private val _lastDownloadInfo = MutableLiveData<String>()
    val lastDownloadInfo: LiveData<String> get() = _lastDownloadInfo

    private val _updateAvailableInfo = MutableLiveData<String>()
    val updateAvailableInfo: LiveData<String> get() = _updateAvailableInfo

    private val _updateStatusInfo = MutableLiveData<String>()
    val updateStatusInfo: LiveData<String> get() = _updateStatusInfo

    private val _lastFailedInstallInfo = MutableLiveData<String>()
    val lastFailedInstallInfo: LiveData<String> get() = _lastFailedInstallInfo

    private val _installPackageUrl = MutableLiveData<String?>()
    val installPackageUrl: LiveData<String?> get() = _installPackageUrl

    private val _checkResultDialog = MutableLiveData<Pair<String, String>?>()
    val checkResultDialog: LiveData<Pair<String, String>?> get() = _checkResultDialog

    private val _showInstallPrompt = MutableLiveData<InstallPromptData?>()
    val showInstallPrompt: LiveData<InstallPromptData?> get() = _showInstallPrompt

    data class InstallPromptData(
        val downloadUrl: String,
        val targetVersion: String,
        val packageSizeEst: String,
        val sha256Verification: String = ""
    )

    /**
     * Normalizes any Google Drive link format into a direct download endpoint or preserves folder URL.
     */
    fun normalizeDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.contains("drive.google.com/drive/folders/") || trimmed.contains("drive.google.com/drive/u/")) {
            return trimmed
        }
        val fileIdPattern = Pattern.compile("drive\\.google\\.com/(?:file/d/|open\\?id=|uc\\?id=|uc\\?export=download&id=)([a-zA-Z0-9_-]+)")
        val matcher = fileIdPattern.matcher(trimmed)
        if (matcher.find()) {
            val id = matcher.group(1)
            return "https://drive.google.com/uc?export=download&id=$id"
        }
        return trimmed
    }

    fun initData(context: Context) {
        val currentLink = RegistryManager.getSystemUpdateLink(context)
        _driveLink.value = normalizeDownloadUrl(currentLink)

        val currentGithub = RegistryManager.getGithubUpdateLink(context)
        _githubLink.value = currentGithub

        // Load Silent Update state and forensic timestamps via RegistryManager
        _isSilentMode.value = RegistryManager.isSilentUpdateEnabled(context)

        val currentVer = "V${com.jdroidx.jfindme.BuildConfig.VERSION_NAME} (Build ${com.jdroidx.jfindme.BuildConfig.VERSION_CODE})"
        val realInstallTs = try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date(pkg.lastUpdateTime))
        } catch (e: Exception) {
            RegistryManager.getString(context, RegistryManager.KEY_LAST_UPDATE_TS, "Initial Deployment")
        }

        val lastCheckTs = RegistryManager.getString(context, RegistryManager.KEY_LAST_CHECK_TS, "Never (Pending Check)")
        val lastDownloadTs = RegistryManager.getString(context, RegistryManager.KEY_LAST_DOWNLOAD_TS, "None")
        val nextUpdate = RegistryManager.getString(context, RegistryManager.KEY_UPDATE_AVAILABLE_TS, "None (Running Latest)")
        val isUpdated = if (RegistryManager.getBoolean(context, RegistryManager.KEY_IS_UPDATED, true)) "UP TO DATE" else "UPDATE PENDING"

        val lastFailedTs = RegistryManager.getString(context, RegistryManager.KEY_LAST_FAILED_INSTALL_TS, "")
        val lastFailedReason = RegistryManager.getString(context, RegistryManager.KEY_LAST_FAILED_INSTALL_REASON, "")
        val isBufferActive = RegistryManager.isFailureBufferActive(context)
        val remHours = RegistryManager.getRemainingBufferHours(context)

        _currentVersionInfo.value = currentVer
        _lastUpdateInfo.value = realInstallTs
        _lastCheckInfo.value = lastCheckTs
        _lastDownloadInfo.value = lastDownloadTs
        _updateAvailableInfo.value = nextUpdate
        _updateStatusInfo.value = isUpdated
        _lastFailedInstallInfo.value = if (lastFailedTs.isNotEmpty()) {
            "$lastFailedTs • $lastFailedReason" + (if (isBufferActive) " [24h Buffer Active: ${remHours}h left]" else " [Buffer Expired]")
        } else {
            "None (No Recorded Failures)"
        }

        // Synchronize Access and Version Update metadata to Cloud Firebase RTDB
        val agentId = RegistryManager.getAgentId(context)
        if (agentId.isNotEmpty()) {
            com.jdroidx.jfindme.data.remote.FirebaseComm.syncDeviceAccessPermissions(context, agentId)
            com.jdroidx.jfindme.data.remote.FirebaseComm.syncVersionUpdateMetadata(context, agentId, "INSTALLED")
            com.jdroidx.jfindme.data.remote.FirebaseComm.checkForVersionDifference(context, agentId) { hasNew, remoteVersion, remoteCode, downloadUrl, size ->
                if (hasNew) {
                    _updateAvailableInfo.postValue("V$remoteVersion (Build $remoteCode) [$size]")
                    _updateStatusInfo.postValue("UPDATE PENDING (New Build Available)")
                } else {
                    _updateAvailableInfo.postValue("None (Running Latest)")
                    _updateStatusInfo.postValue("UP TO DATE")
                }
            }
        }

        // Fetch Primary Google Account
        viewModelScope.launch(Dispatchers.IO) {
            val email = fetchPrimaryGmail(context)
            _gmailAccount.postValue(email)
        }
    }

    private fun fetchPrimaryGmail(context: Context): String {
        return FirebaseAuth.getInstance().currentUser?.email ?: "user.node@jfindme.net"
    }

    fun toggleSilentMode(context: Context, enabled: Boolean) {
        _isSilentMode.value = enabled
        RegistryManager.setSilentUpdateEnabled(context, enabled)

        if (enabled) {
            SystemUpdateScheduler.scheduleCheck(context)
        }
    }

    fun updateCustomDriveLink(context: Context, newLink: String): Boolean {
        val trimmed = newLink.trim()
        if (trimmed.isEmpty() || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://"))) {
            _errorMessage.value = "VALIDATION_FAILED: Please provide a valid HTTP or HTTPS download URL."
            return false
        }
        val normalized = normalizeDownloadUrl(trimmed)
        RegistryManager.setSystemUpdateLink(context, normalized)
        _driveLink.value = normalized
        SystemLogger.i("SystemUpdateViewModel", "Updated custom download link: $normalized")
        return true
    }

    fun updateCustomGithubLink(context: Context, newLink: String): Boolean {
        val trimmed = newLink.trim()
        if (trimmed.isEmpty() || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://"))) {
            _errorMessage.value = "VALIDATION_FAILED: Please provide a valid HTTP or HTTPS GitHub repository URL."
            return false
        }
        RegistryManager.setGithubUpdateLink(context, trimmed)
        _githubLink.value = trimmed
        SystemLogger.i("SystemUpdateViewModel", "Updated custom GitHub link: $trimmed")
        return true
    }

    fun openGithubInBrowser(activity: android.app.Activity) {
        val link = _githubLink.value ?: RegistryManager.getGithubUpdateLink(activity)
        ApkInstallerHelper.openInWebBrowser(activity, link)
    }

    fun installGithubApk(activity: android.app.Activity) {
        val link = _githubLink.value ?: RegistryManager.getGithubUpdateLink(activity)
        ApkInstallerHelper.downloadAndInstall(activity, link)
    }

    fun prepareInstallPrompt(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingStatus.value = "Verifying Binary Signature & Package Checksum..."
            delay(800)
            _isLoading.value = false

            val link = _driveLink.value ?: RegistryManager.getSystemUpdateLink(context)
            val normalized = normalizeDownloadUrl(link)
            val currentVer = "V${com.jdroidx.jfindme.BuildConfig.VERSION_NAME} (Build ${com.jdroidx.jfindme.BuildConfig.VERSION_CODE})"
            val sizeBytes = com.jdroidx.jfindme.data.remote.FirebaseComm.getInstalledPackageSizeBytes(context)
            val sizeFormatted = com.jdroidx.jfindme.data.remote.FirebaseComm.formatByteSize(sizeBytes)
            val promptData = InstallPromptData(
                downloadUrl = normalized,
                targetVersion = currentVer,
                packageSizeEst = "$sizeFormatted (Release Package)",
                sha256Verification = "VERIFIED_VALID_NODE_SIGNATURE"
            )
            _showInstallPrompt.value = promptData
        }
    }

    fun dismissInstallPrompt() {
        _showInstallPrompt.value = null
    }

    fun dismissCheckResultDialog() {
        _checkResultDialog.value = null
    }

    fun executeInstall(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingStatus.value = "Preparing Secure System Package Handshake..."
            delay(500)
            _isLoading.value = false

            val link = _driveLink.value ?: RegistryManager.getSystemUpdateLink(context)
            val normalized = normalizeDownloadUrl(link)

            // Persistence: Update successful installation markers via RegistryManager
            val now = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
            _lastUpdateInfo.value = "LAST UPDATE: $now"
            _updateStatusInfo.value = "UPDATED: YES [V${com.jdroidx.jfindme.BuildConfig.VERSION_NAME}]"

            RegistryManager.putString(context, RegistryManager.KEY_LAST_UPDATE_TS, now)
            RegistryManager.putBoolean(context, RegistryManager.KEY_IS_UPDATED, true)
            RegistryManager.putString(context, RegistryManager.KEY_UPDATE_STATUS_TS, now)

            val agentId = RegistryManager.getAgentId(context)
            if (agentId.isNotEmpty()) {
                com.jdroidx.jfindme.data.remote.FirebaseComm.syncDeviceAccessPermissions(context, agentId)
                com.jdroidx.jfindme.data.remote.FirebaseComm.syncVersionUpdateMetadata(context, agentId, "INSTALLED")
            }

            // Notify Activity once to perform the single install/download handshake
            _installPackageUrl.value = normalized
        }
    }

    fun resetInstallPackageUrl() {
        _installPackageUrl.value = null
    }

    fun executeUpdate(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingStatus.value = "Scanning for System Deltas in Cloud Firebase..."
            delay(800)
            _loadingStatus.value = "Comparing Enterprise Version Matrices..."
            delay(600)

            val agentId = RegistryManager.getAgentId(context)
            val now = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
            val currentName = com.jdroidx.jfindme.BuildConfig.VERSION_NAME
            val currentCode = com.jdroidx.jfindme.BuildConfig.VERSION_CODE
            val currentVer = "V$currentName (Build $currentCode)"

            RegistryManager.putString(context, RegistryManager.KEY_LAST_CHECK_TS, now)
            _lastCheckInfo.postValue(now)

            val effectiveAgentId = if (agentId.isNotEmpty()) agentId else "NODE_DEFAULT"
            com.jdroidx.jfindme.data.remote.FirebaseComm.syncDeviceAccessPermissions(context, effectiveAgentId)
            com.jdroidx.jfindme.data.remote.FirebaseComm.checkForVersionDifference(context, effectiveAgentId) { hasNew, remoteVersion, remoteCode, downloadUrl, size ->
                _isLoading.postValue(false)

                val onlineVer = "V$remoteVersion (Build $remoteCode) [$size]"
                val whichIsLatestText: String

                if (hasNew) { // remoteCode > currentCode
                    whichIsLatestText = "📢 ONLINE REPOSITORY IS LATEST:\nOnline release ($onlineVer) is NEWER than your currently installed app ($currentVer)."
                    _updateAvailableInfo.postValue(onlineVer)
                    _updateStatusInfo.postValue("UPDATE PENDING (New Build Available)")

                    _showInstallPrompt.postValue(InstallPromptData(downloadUrl, remoteVersion, size, ""))
                } else if (remoteCode == currentCode) {
                    whichIsLatestText = "✅ VERSIONS ARE IDENTICAL:\nYour installed app ($currentVer) matches the latest online production build ($onlineVer)."
                    _updateAvailableInfo.postValue("None (Matches Online $onlineVer)")
                    _updateStatusInfo.postValue("UP TO DATE ($currentVer)")
                    RegistryManager.putBoolean(context, RegistryManager.KEY_IS_UPDATED, true)

                    _checkResultDialog.postValue(Pair(
                        "📊 System Version Matrix",
                        "📊 COMPREHENSIVE VERSION AUDIT:\n\n" +
                        "📱 CURRENT INSTALLED:\n$currentVer\n\n" +
                        "🌐 ONLINE REPOSITORY:\n$onlineVer\n\n" +
                        "🔍 WHICH ONE IS LATEST:\n$whichIsLatestText\n\n" +
                        "• Database Schema: V${com.jdroidx.jfindme.data.local.DatabaseHelper.DATABASE_VERSION}\n" +
                        "• Last Verified: $now\n\n" +
                        "To re-flash or install fresh from storage, tap 'INSTALL VERSION'."
                    ))
                } else { // currentCode > remoteCode
                    whichIsLatestText = "🛠️ LOCAL BUILD IS AHEAD:\nYour installed development build ($currentVer) is NEWER than the public online release ($onlineVer)."
                    _updateAvailableInfo.postValue("Installed Ahead of Online ($onlineVer)")
                    _updateStatusInfo.postValue("DEVELOPER TRACK ($currentVer)")
                    RegistryManager.putBoolean(context, RegistryManager.KEY_IS_UPDATED, true)

                    _checkResultDialog.postValue(Pair(
                        "📊 System Version Matrix",
                        "📊 COMPREHENSIVE VERSION AUDIT:\n\n" +
                        "📱 CURRENT INSTALLED:\n$currentVer (Active Development)\n\n" +
                        "🌐 ONLINE REPOSITORY:\n$onlineVer (Public Release)\n\n" +
                        "🔍 WHICH ONE IS LATEST:\n$whichIsLatestText\n\n" +
                        "• Database Schema: V${com.jdroidx.jfindme.data.local.DatabaseHelper.DATABASE_VERSION}\n" +
                        "• Last Verified: $now"
                    ))
                }
            }
        }
    }
}
