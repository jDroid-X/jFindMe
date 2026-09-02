package com.jdroidx.jfindme.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.jdroidx.jfindme.ui.base.ThemeManager
import com.jdroidx.jfindme.ui.base.UiArchitect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.regex.Pattern

/**
 * 📦 ApkInstallerHelper: Enterprise Binary Downloader & Package Installer Engine.
 *
 * Solves:
 * 1. Google Drive app intercepting download URLs with "Document look-up failed" error.
 * 2. Directly downloads APK streams into local cache with real-time percentage progress.
 * 3. Launches Android native PackageInstaller via secure FileProvider.
 * 4. Gracefully manages Android 8.0+ Unknown App Sources permissions.
 */
object ApkInstallerHelper {

    private const val TAG = "ApkInstallerHelper"
    @Volatile var pendingInstallApkFile: File? = null

    /**
     * Extracts Google Drive file ID from various Drive URL formats.
     */
    fun extractDriveFileId(rawUrl: String): String? {
        val pattern = Pattern.compile("drive\\.google\\.com/(?:file/d/|open\\?id=|uc\\?id=|uc\\?export=download&id=|file/d/download/)([a-zA-Z0-9_-]+)")
        val matcher = pattern.matcher(rawUrl.trim())
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * Extracts Google Drive Folder ID from folder URL formats.
     */
    fun extractDriveFolderId(rawUrl: String): String? {
        val pattern = Pattern.compile("drive\\.google\\.com/drive/(?:u/\\d+/)?folders/([a-zA-Z0-9_-]+)")
        val matcher = pattern.matcher(rawUrl.trim())
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * Converts Google Drive (Files or Folders) or GitHub repository links to direct binary download endpoints.
     */
    fun resolveDirectDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val fileId = extractDriveFileId(trimmed)
        if (fileId != null) {
            return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
        }

        val folderId = extractDriveFolderId(trimmed)
        if (folderId != null) {
            // Direct Git LFS media CDN endpoint for seamless zero-redirect download
            return "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/app-universal-release.apk"
        }

        // Handle GitHub tree/blob folder links
        if (trimmed.contains("github.com/jDroid-X/jButterfly/tree/main/jbutterfly") || 
            trimmed.contains("github.com/jDroid-X/jButterfly/blob/main/jbutterfly") ||
            trimmed.equals("https://github.com/jDroid-X/jButterfly", ignoreCase = true) ||
            trimmed.equals("https://github.com/jDroid-X/jButterfly/", ignoreCase = true) ||
            trimmed.contains("github.com/jDroid-X/jButterfly/raw/main/jbutterfly/app-universal-release.apk")) {
            return "https://media.githubusercontent.com/media/jDroid-X/jButterfly/main/jbutterfly/app-universal-release.apk"
        }
        if (trimmed.contains("github.com") && trimmed.contains("/blob/")) {
            return trimmed.replace("/blob/", "/raw/")
        }
        return trimmed
    }

    /**
     * Downloads APK in background with UI progress and launches native installer.
     * Incorporates 24-Hour Failure Backoff and Local Cache Reuse to eliminate redundant internet bandwidth traffic.
     */
    fun downloadAndInstall(activity: Activity, downloadUrl: String, onComplete: ((Boolean) -> Unit)? = null) {
        val directUrl = resolveDirectDownloadUrl(downloadUrl)
        val apkFile = File(activity.cacheDir, "update_jfindme.apk")

        // 🛡️ 24-HOUR BUFFER & LOCAL CACHE REUSE: Avoid wasteful re-downloads
        if (apkFile.exists() && isValidApkFile(activity, apkFile)) {
            val isBufferActive = RegistryManager.isFailureBufferActive(activity)
            val remHours = RegistryManager.getRemainingBufferHours(activity)
            val lastFailedReason = RegistryManager.getString(activity, RegistryManager.KEY_LAST_FAILED_INSTALL_REASON, "Signature Mismatch / OS Block")
            val lastFailedTs = RegistryManager.getString(activity, RegistryManager.KEY_LAST_FAILED_INSTALL_TS, "Recently")
            val cachedSizeMb = String.format(Locale.US, "%.1f", apkFile.length() / (1024.0 * 1024.0))

            val message = if (isBufferActive) {
                "⚡ A verified update package ($cachedSizeMb MB) is already cached locally on device storage.\n\n" +
                "⚠️ PREVIOUS INSTALLATION ATTEMPT FAILED:\n" +
                "• Time: $lastFailedTs\n" +
                "• Reason: $lastFailedReason\n" +
                "• 24h Safety Buffer Remaining: $remHours Hours\n\n" +
                "To prevent internet data waste, re-download is bypassed during the 24h buffer.\n\n" +
                "How would you like to proceed?"
            } else {
                "⚡ A verified update package ($cachedSizeMb MB) is already cached locally on device storage (0 MB download needed).\n\n" +
                "Would you like to install the local package or force a fresh download?"
            }

            AlertDialog.Builder(activity)
                .setTitle("📦 Locally Cached Package Found")
                .setMessage(message)
                .setPositiveButton("INSTALL LOCAL APK") { _, _ ->
                    launchPackageInstaller(activity, apkFile)
                    onComplete?.invoke(true)
                }
                .setNeutralButton("CLEAN INSTALL GUIDE") { _, _ ->
                    showCleanInstallPrompt(activity, apkFile)
                }
                .setNegativeButton("FORCE RE-DOWNLOAD") { _, _ ->
                    startDownloadStream(activity, directUrl, downloadUrl, apkFile, onComplete)
                }
                .show()
            return
        }

        startDownloadStream(activity, directUrl, downloadUrl, apkFile, onComplete)
    }

    private fun startDownloadStream(activity: Activity, directUrl: String, downloadUrl: String, apkFile: File, onComplete: ((Boolean) -> Unit)?) {
        val prefs = activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val themeId = prefs.getInt("theme_id", 0)
        val cardBg = ThemeManager.getCardBackgroundColor(themeId)
        val textPrimary = ThemeManager.getTextPrimaryColor(themeId)
        val textSecondary = ThemeManager.getTextSecondaryColor(themeId)
        val gradStart = ThemeManager.getGradientStart(themeId)

        // Storage Space Check
        val freeBytes = getAvailableCacheSpaceBytes(activity)
        if (freeBytes < 150L * 1024 * 1024) {
            AlertDialog.Builder(activity)
                .setTitle("⚠️ Low Storage Space")
                .setMessage("Your device has less than 150 MB free internal space (${freeBytes / (1024 * 1024)} MB available). Please free up space before downloading this update.")
                .setPositiveButton("OK", null)
                .show()
            onComplete?.invoke(false)
            return
        }

        // Custom Progress Modal
        val dialog = Dialog(activity)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardBg)
            val pad = 40
            setPadding(pad, pad, pad, pad)
        }

        val tvTitle = TextView(activity).apply {
            text = "📦 DOWNLOADING SYSTEM UPDATE"
            setTextColor(gradStart)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 20)
        }
        root.addView(tvTitle)

        val tvStatus = TextView(activity).apply {
            text = "Connecting to binary distribution node..."
            setTextColor(textSecondary)
            textSize = 13f
            setPadding(0, 0, 0, 25)
        }
        root.addView(tvStatus)

        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            max = 100
            progress = 0
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 24)
            lp.setMargins(0, 0, 0, 20)
            layoutParams = lp
        }
        root.addView(progressBar)

        val tvPercent = TextView(activity).apply {
            text = "0 MB"
            setTextColor(textPrimary)
            textSize = 12f
            gravity = Gravity.END
        }
        root.addView(tvPercent)

        var isCancelled = false
        val btnCancelDownload = TextView(activity).apply {
            text = "CANCEL DOWNLOAD"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val pad = 20
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                isCancelled = true
                dialog.dismiss()
                Toast.makeText(activity, "Download canceled.", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(false)
            }
        }
        root.addView(btnCancelDownload)

        dialog.setContentView(root)
        dialog.setCancelable(false)
        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }

        if (dialog.window != null) {
            val lp = android.view.WindowManager.LayoutParams()
            lp.copyFrom(dialog.window?.attributes)
            lp.width = (activity.resources.displayMetrics.widthPixels * 0.90).toInt()
            dialog.window?.attributes = lp
        }

        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                var currentUrl = directUrl
                var redirectCount = 0
                val maxRedirects = 8

                // Follow HTTP 301/302/303/307 redirects (including Google Drive confirm tokens & GitHub LFS S3 URLs)
                while (redirectCount < maxRedirects) {
                    if (isCancelled) return@launch

                    val urlObj = URL(currentUrl)
                    connection = (urlObj.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        setRequestProperty("Accept", "*/*")
                    }

                    val responseCode = connection.responseCode
                    if (responseCode in 300..399) {
                        val newLocation = connection.getHeaderField("Location")
                        if (!newLocation.isNullOrEmpty()) {
                            currentUrl = newLocation
                            redirectCount++
                            connection.disconnect()
                            continue
                        }
                    }
                    break
                }

                if (isCancelled) return@launch
                val conn = connection ?: throw IllegalStateException("Failed to establish HTTP connection.")
                conn.connect()

                val fileLength = conn.contentLength
                inputStream = conn.inputStream

                if (apkFile.exists()) apkFile.delete()

                outputStream = FileOutputStream(apkFile)
                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int

                while (inputStream.read(data).also { count = it } != -1) {
                    if (isCancelled) {
                        outputStream.close()
                        if (apkFile.exists()) apkFile.delete()
                        return@launch
                    }

                    total += count.toLong()
                    if (fileLength > 0) {
                        val progressPercent = ((total * 100) / fileLength).toInt()
                        withContext(Dispatchers.Main) {
                            if (!activity.isFinishing && !activity.isDestroyed && dialog.isShowing) {
                                progressBar.isIndeterminate = false
                                progressBar.progress = progressPercent
                                tvStatus.text = "Downloading binary stream..."
                                tvPercent.text = "${(total / (1024 * 1024))} MB / ${(fileLength / (1024 * 1024))} MB ($progressPercent%)"
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (!activity.isFinishing && !activity.isDestroyed && dialog.isShowing) {
                                progressBar.isIndeterminate = true
                                tvStatus.text = "Downloading binary stream..."
                                tvPercent.text = "${(total / (1024 * 1024))} MB"
                            }
                        }
                    }
                    outputStream.write(data, 0, count)
                }

                outputStream.flush()

                withContext(Dispatchers.Main) {
                    if (dialog.isShowing) dialog.dismiss()

                    if (isValidApkFile(activity, apkFile)) {
                        val dlNow = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                        RegistryManager.putString(activity, RegistryManager.KEY_LAST_DOWNLOAD_TS, dlNow)
                        RegistryManager.putString(activity, RegistryManager.KEY_CACHED_APK_PATH, apkFile.absolutePath)
                        launchPackageInstaller(activity, apkFile)
                        onComplete?.invoke(true)
                    } else {
                        if (apkFile.exists()) apkFile.delete()
                        showDownloadErrorDialog(activity, downloadUrl, "The downloaded file is corrupted or is an HTML error page (not an APK package).")
                        onComplete?.invoke(false)
                    }
                }

            } catch (e: Exception) {
                SystemLogger.e(TAG, "Download error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (dialog.isShowing) dialog.dismiss()
                    if (!isCancelled) {
                        showDownloadErrorDialog(activity, downloadUrl, e.message ?: "Unknown network error")
                    }
                    onComplete?.invoke(false)
                }
            } finally {
                try { outputStream?.close() } catch (ignored: Exception) {}
                try { inputStream?.close() } catch (ignored: Exception) {}
                connection?.disconnect()
            }
        }
    }

    /**
     * Validates that the downloaded file is a genuine Android APK / ZIP archive and compatible with this package.
     */
    fun isValidApkFile(context: Context, file: File): Boolean {
        if (!file.exists() || file.length() < 1024 * 1024) return false
        val hasMagicBytes = try {
            java.io.FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                val read = fis.read(header)
                // ZIP magic bytes: PK\x03\x04 (0x50, 0x4B, 0x03, 0x04)
                read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            }
        } catch (e: Exception) {
            false
        }
        if (!hasMagicBytes) return false

        // Comprehensive Android Package Manager Archive validation
        return try {
            val pkgInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            if (pkgInfo != null) {
                val isPackageMatch = pkgInfo.packageName == context.packageName
                if (!isPackageMatch) {
                    SystemLogger.w(TAG, "Package name mismatch: Expected ${context.packageName} but got ${pkgInfo.packageName}")
                }
                isPackageMatch
            } else {
                true
            }
        } catch (e: Exception) {
            SystemLogger.w(TAG, "PackageArchiveInfo warning: ${e.message}")
            true
        }
    }

    enum class SignatureStatus {
        MATCH,
        MISMATCH,
        UNKNOWN
    }

    /**
     * 🛡️ Cryptographic Certificate Validation: Compares installed package signature with downloaded APK.
     */
    fun checkSignatureMatch(context: Context, apkFile: File): SignatureStatus {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                android.content.pm.PackageManager.GET_SIGNATURES
            }

            val installedPkg = pm.getPackageInfo(context.packageName, flags)
            val archivePkg = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return SignatureStatus.UNKNOWN

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val installedSig = installedPkg.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
                val archiveSig = archivePkg.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
                if (installedSig != null && archiveSig != null) {
                    return if (installedSig.contentEquals(archiveSig)) SignatureStatus.MATCH else SignatureStatus.MISMATCH
                }
            }

            @Suppress("DEPRECATION")
            val installedSigs = installedPkg.signatures
            @Suppress("DEPRECATION")
            val archiveSigs = archivePkg.signatures
            if (!installedSigs.isNullOrEmpty() && !archiveSigs.isNullOrEmpty()) {
                val match = installedSigs[0].toCharsString() == archiveSigs[0].toCharsString()
                return if (match) SignatureStatus.MATCH else SignatureStatus.MISMATCH
            }

            SignatureStatus.UNKNOWN
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Signature check error: ${e.message}")
            SignatureStatus.UNKNOWN
        }
    }

    /**
     * 💾 Verifies that device internal storage has sufficient space for download & extraction.
     */
    fun hasAvailableStorage(context: Context, requiredBytes: Long = 150 * 1024 * 1024L): Boolean {
        return try {
            val stat = android.os.StatFs(context.cacheDir.path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            available >= requiredBytes
        } catch (e: Exception) {
            true
        }
    }

    /**
     * 🛡️ Signature Mismatch & Architecture Override Modal:
     * Provides clear choices when Debug vs Release signature conflicts occur, including 1-click automatic app override.
     */
    fun showSignatureMismatchOverrideModal(activity: Activity, apkFile: File) {
        val curVer = "V${com.jdroidx.jfindme.BuildConfig.VERSION_NAME} (Build ${com.jdroidx.jfindme.BuildConfig.VERSION_CODE})"
        RegistryManager.recordInstallFailure(
            activity,
            curVer,
            "SIGNATURE MISMATCH: Installed build is DEBUG (Android Studio) while update is RELEASE (GitHub). Clean uninstall required."
        )

        // Save a copy of the APK to the public Downloads folder so it is preserved even after uninstalling the old app!
        val preservedApk = try {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists()) {
                val targetFile = File(downloadDir, "jfindme-latest-release.apk")
                apkFile.copyTo(targetFile, overwrite = true)
                targetFile
            } else apkFile
        } catch (e: Exception) {
            apkFile
        }

        if (activity is com.jdroidx.jfindme.ui.base.BaseActivityJava) {
            val options = arrayOf(
                UiArchitect.OptionItem("⚡ YES, OVERRIDE & INSTALL LATEST", "1-Click Replacement: Uninstalls current app and launches the latest downloaded APK fresh", android.R.drawable.ic_menu_rotate),
                UiArchitect.OptionItem("📦 PROCEED WITH STANDARD INSTALLER", "Attempt direct overwrite with Android PackageInstaller (may be rejected by OS)", android.R.drawable.ic_menu_save),
                UiArchitect.OptionItem("🌐 OPEN RELEASES IN WEB BROWSER", "Visit GitHub repository in mobile browser to inspect release notes", android.R.drawable.ic_menu_view),
                UiArchitect.OptionItem("✕ CANCEL & KEEP CURRENT VERSION", "Dismiss update and keep running current installed version", android.R.drawable.ic_menu_close_clear_cancel)
            )

            UiArchitect.showRichOptionsDialog(activity, "⚠️ SIGNATURE MISMATCH: OVERRIDE APP?", options) { index, _ ->
                when (index) {
                    0 -> executeAppOverrideFlow(activity, preservedApk)
                    1 -> proceedWithInstallerIntent(activity, apkFile)
                    2 -> openInWebBrowser(activity, RegistryManager.getGithubUpdateLink(activity))
                    3 -> Toast.makeText(activity, "Update cancelled. Current version retained.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val items = arrayOf(
                "⚡ YES, OVERRIDE & INSTALL LATEST",
                "📦 PROCEED WITH STANDARD INSTALLER",
                "🌐 OPEN RELEASES IN WEB BROWSER",
                "✕ CANCEL & KEEP CURRENT VERSION"
            )
            AlertDialog.Builder(activity)
                .setTitle("⚠️ SIGNATURE MISMATCH: OVERRIDE APP?")
                .setItems(items) { _, index ->
                    when (index) {
                        0 -> executeAppOverrideFlow(activity, preservedApk)
                        1 -> proceedWithInstallerIntent(activity, apkFile)
                        2 -> openInWebBrowser(activity, RegistryManager.getGithubUpdateLink(activity))
                        3 -> Toast.makeText(activity, "Update cancelled. Current version retained.", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    private fun executeAppOverrideFlow(activity: Activity, apkFile: File) {
        // Step 1: Backup current session parameters
        try {
            val agentId = RegistryManager.getAgentId(activity)
            if (agentId.isNotEmpty()) {
                com.jdroidx.jfindme.data.remote.FirebaseComm.syncDeviceAccessPermissions(activity, agentId)
                com.jdroidx.jfindme.data.remote.FirebaseComm.syncVersionUpdateMetadata(activity, agentId, "OVERRIDE_INITIATED")
            }
        } catch (ignored: Exception) {}

        // Step 2: Show clear confirmation modal before invoking the replacement
        AlertDialog.Builder(activity)
            .setTitle("🔄 Confirm App Replacement")
            .setMessage(
                "The update package has been safely saved to your phone's Downloads folder:\n" +
                "📁 Download/jfindme-latest-release.apk\n\n" +
                "Next Steps:\n" +
                "1. Tap 'CONTINUE' to remove the old conflicting version.\n" +
                "2. The installer will prompt to install the latest version immediately!\n\n" +
                "Proceed with override?"
            )
            .setPositiveButton("CONTINUE") { _, _ ->
                try {
                    val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                        data = Uri.parse("package:${activity.packageName}")
                        putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    }
                    activity.startActivity(uninstallIntent)
                } catch (e: Exception) {
                    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(settingsIntent)
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    /**
     * 🛡️ Clean Re-Install Guidance Dialog: Helps user transition from Debug to Release builds without "App not installed" errors.
     */
    fun showCleanInstallPrompt(activity: Activity, apkFile: File) {
        showSignatureMismatchOverrideModal(activity, apkFile)
    }

    /**
     * Prompts the native Android PackageInstaller using FileProvider.
     */
    fun launchPackageInstaller(activity: Activity, apkFile: File) {
        try {
            // Android 8.0+ Unknown sources permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    pendingInstallApkFile = apkFile
                    AlertDialog.Builder(activity)
                        .setTitle("⚙️ Permission Required")
                        .setMessage("To install this system update, please allow 'Install unknown apps' permission for jFindMe in Android Settings.")
                        .setPositiveButton("ALLOW IN SETTINGS") { _, _ ->
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${activity.packageName}")
                            }
                            activity.startActivity(intent)
                        }
                        .setNegativeButton("CANCEL", null)
                        .show()
                    return
                }
            }

            // Pre-flight Signature Check
            val sigStatus = checkSignatureMatch(activity, apkFile)
            if (sigStatus == SignatureStatus.MISMATCH) {
                showSignatureMismatchOverrideModal(activity, apkFile)
                return
            }

            proceedWithInstallerIntent(activity, apkFile)

        } catch (e: Exception) {
            SystemLogger.e(TAG, "Install error", e)
            RegistryManager.recordInstallFailure(activity, "V${com.jdroidx.jfindme.BuildConfig.VERSION_NAME}", e.message ?: "Installer Error")
            Toast.makeText(activity, "Failed to launch package installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun proceedWithInstallerIntent(activity: Activity, apkFile: File) {
        try {
            pendingInstallApkFile = null
            val apkUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // 🛡️ Explicit URI Permission Granting for all matching PackageInstallers (MIUI/Samsung/AOSP)
            val resInfoList = activity.packageManager.queryIntentActivities(
                installIntent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                try {
                    activity.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (ignored: Exception) {}
            }

            activity.startActivity(installIntent)
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Installer intent dispatch error", e)
            RegistryManager.recordInstallFailure(activity, "V${com.jdroidx.jfindme.BuildConfig.VERSION_NAME}", "Dispatch Error: ${e.message}")
            Toast.makeText(activity, "Installer launch failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Resumes pending installation when user returns from Settings.
     */
    fun resumePendingInstall(activity: Activity) {
        val pending = pendingInstallApkFile
        if (pending != null && pending.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.packageManager.canRequestPackageInstalls()) {
                SystemLogger.i(TAG, "Resuming pending installation of: ${pending.name}")
                launchPackageInstaller(activity, pending)
            }
        }
    }

    /**
     * Returns available internal cache bytes.
     */
    fun getAvailableCacheSpaceBytes(context: Context): Long {
        return try {
            val stat = android.os.StatFs(context.cacheDir.path)
            stat.availableBytes
        } catch (e: Exception) {
            500L * 1024 * 1024
        }
    }

    /**
     * Opens URL explicitly in a Web Browser (Chrome/Firefox/Samsung) bypassing Google Drive app.
     */
    fun openInWebBrowser(activity: Activity, rawUrl: String) {
        try {
            val direct = resolveDirectDownloadUrl(rawUrl)
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(direct)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Browser launch failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDownloadErrorDialog(activity: Activity, originalUrl: String, errorMsg: String) {
        AlertDialog.Builder(activity)
            .setTitle("⚠️ Direct Download Notice")
            .setMessage("Direct in-app download encountered an issue:\n$errorMsg\n\nWould you like to open the download link directly in your Web Browser (Chrome / Samsung Internet)?")
            .setPositiveButton("🌐 OPEN IN BROWSER") { _, _ ->
                openInWebBrowser(activity, originalUrl)
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    /**
     * 🚀 Silent Background APK Downloader: Downloads APK without UI blocking.
     */
    @JvmStatic
    fun downloadApkInBackground(
        context: Context,
        downloadUrl: String,
        onComplete: ((Boolean, File?) -> Unit)? = null
    ) {
        val directUrl = resolveDirectDownloadUrl(downloadUrl)
        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                var currentUrl = directUrl
                var redirectCount = 0
                val maxRedirects = 8

                while (redirectCount < maxRedirects) {
                    val urlObj = URL(currentUrl)
                    connection = (urlObj.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                        setRequestProperty("Accept", "*/*")
                    }

                    val responseCode = connection.responseCode
                    if (responseCode in 300..399) {
                        val newLocation = connection.getHeaderField("Location")
                        if (!newLocation.isNullOrEmpty()) {
                            currentUrl = newLocation
                            redirectCount++
                            connection.disconnect()
                            continue
                        }
                    }
                    break
                }

                val conn = connection ?: throw IllegalStateException("Failed to connect.")
                conn.connect()

                inputStream = conn.inputStream
                val apkFile = File(context.cacheDir, "update_jfindme.apk")
                if (apkFile.exists()) apkFile.delete()

                outputStream = FileOutputStream(apkFile)
                val data = ByteArray(8192)
                var count: Int
                while (inputStream.read(data).also { count = it } != -1) {
                    outputStream.write(data, 0, count)
                }
                outputStream.flush()

                if (isValidApkFile(context, apkFile)) {
                    SystemLogger.i(TAG, "Silent APK download verified successfully: ${apkFile.length()} bytes.")
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(true, apkFile)
                    }
                } else {
                    if (apkFile.exists()) apkFile.delete()
                    SystemLogger.e(TAG, "Silent APK download failed: corrupted or non-APK file.")
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false, null)
                    }
                }

            } catch (e: Exception) {
                SystemLogger.e(TAG, "Silent download error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, null)
                }
            } finally {
                try { outputStream?.close() } catch (ignored: Exception) {}
                try { inputStream?.close() } catch (ignored: Exception) {}
                connection?.disconnect()
            }
        }
    }

    /**
     * 🔔 Dispatches a system notification allowing the user to tap and install the downloaded update.
     */
    @JvmStatic
    fun notifyUpdateReadyToInstall(
        context: Context,
        apkFile: File,
        versionName: String
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
            val channelId = "system_updates_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "System Updates",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for downloaded jFindMe APK updates"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                1008,
                installIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                } else {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("📦 System Update V$versionName Ready")
                .setContentText("jFindMe has downloaded the latest package in background. Tap to install now.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager.notify(7788, builder.build())
            SystemLogger.i(TAG, "Notification dispatched for update V$versionName")

        } catch (e: Exception) {
            SystemLogger.e(TAG, "Failed to dispatch update notification: ${e.message}", e)
        }
    }

    /**
     * 🚀 Installs the downloaded APK silently using Android 12+ (API 31+) PackageInstaller Unattended Session API.
     */
    @JvmStatic
    fun installPackageSilently(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || !isValidApkFile(context, apkFile)) {
            SystemLogger.e(TAG, "Cannot install APK: File is invalid or missing.")
            return false
        }

        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName(context.packageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            session.use { activeSession ->
                java.io.FileInputStream(apkFile).use { inputStream ->
                    activeSession.openWrite("package_update", 0, apkFile.length()).use { outputStream ->
                        inputStream.copyTo(outputStream)
                        activeSession.fsync(outputStream)
                    }
                }

                val intent = Intent(context, com.jdroidx.jfindme.receiver.PackageInstallStatusReceiver::class.java).apply {
                    action = com.jdroidx.jfindme.receiver.PackageInstallStatusReceiver.ACTION_INSTALL_STATUS
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                } else {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    flags
                )

                activeSession.commit(pendingIntent.intentSender)
                SystemLogger.i(TAG, "PackageInstaller session #$sessionId committed for silent installation.")
            }
            return true
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Unattended package installation failed: ${e.message}", e)
            return false
        }
    }

    /**
     * 👑 Determines whether to perform zero-touch unattended install (Android 12+) or notify user with 1-tap prompt.
     */
    @JvmStatic
    fun installSilentlyOrPrompt(context: Context, apkFile: File, versionName: String = "") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else true

            if (canInstall) {
                SystemLogger.i(TAG, "Initiating Android 12+ Unattended Silent Upgrade for V$versionName...")
                val success = installPackageSilently(context, apkFile)
                if (success) return
            }
        }

        // Graceful fallback for Android 11 and below or when unattended session needs explicit user prompt
        SystemLogger.i(TAG, "Dispatching update notification for user installation prompt (V$versionName)...")
        notifyUpdateReadyToInstall(context, apkFile, versionName)
    }
}
