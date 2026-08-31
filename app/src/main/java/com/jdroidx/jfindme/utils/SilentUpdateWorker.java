package com.jdroidx.jfindme.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.jdroidx.jfindme.data.remote.FirebaseComm;

/**
 * Executes a silent update and permissions audit at the scheduled target time (03:03 AM).
 */
public class SilentUpdateWorker extends Worker {

    public SilentUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SystemLogger.i("SilentUpdateWorker", "Checking update criteria & hardware access matrix at 03:03 AM.");
        
        String agentId = RegistryManager.getAgentId(context);
        if (!agentId.isEmpty()) {
            // 1. Synchronize Access Permissions Matrix to Firebase
            FirebaseComm.syncDeviceAccessPermissions(context, agentId);
            
            // 2. Synchronize Current Version Metadata to Firebase
            FirebaseComm.syncVersionUpdateMetadata(context, agentId, "INSTALLED", 106615515L, "101.67 MB");
        }

        boolean isEnabled = RegistryManager.isSilentUpdateEnabled(context);
        if (!isEnabled) {
            SystemLogger.i("SilentUpdateWorker", "Silent update disabled by user preference. Skipping binary download.");
            return Result.success();
        }

        try {
            SystemLogger.i("SilentUpdateWorker", "Scanning for remote version difference in Cloud Firebase RTDB...");
            String nowStr = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_TIME, nowStr);
            RegistryManager.putString(context, RegistryManager.KEY_LAST_CHECK_TS, nowStr);

            if (!agentId.isEmpty()) {
                FirebaseComm.checkForVersionDifference(context, agentId, (hasNew, remoteVersion, remoteCode, downloadUrl, size) -> {
                    if (hasNew) {
                        SystemLogger.i("SilentUpdateWorker", "🚀 New Remote Version Detected: V" + remoteVersion + " (Build " + remoteCode + ")");
                        RegistryManager.putString(context, RegistryManager.KEY_UPDATE_AVAILABLE_TS, "V" + remoteVersion + " (Build " + remoteCode + ")");
                        RegistryManager.putBoolean(context, RegistryManager.KEY_IS_UPDATED, false);

                        if (!ApkInstallerHelper.INSTANCE.hasAvailableStorage(context, 150 * 1024 * 1024L)) {
                            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "STORAGE_FULL");
                            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, "Insufficient storage to download update package.");
                            RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
                            return kotlin.Unit.INSTANCE;
                        }

                        // Trigger Change Log Audit
                        FirebaseComm.logChange(
                            agentId,
                            "System_Update",
                            "UPDATE_AVAILABLE_DETECTED",
                            "BUILD_" + remoteCode,
                            java.util.Collections.singletonMap("remote_version", "V" + remoteVersion),
                            agentId
                        );

                        // 🚀 Automatic Background APK Download from Google Drive / GitHub
                        String targetUrl = (downloadUrl != null && !downloadUrl.isEmpty()) 
                            ? downloadUrl 
                            : RegistryManager.getSystemUpdateLink(context);

                        SystemLogger.i("SilentUpdateWorker", "Initiating background APK download from: " + targetUrl);
                        ApkInstallerHelper.downloadApkInBackground(context, targetUrl, (success, apkFile) -> {
                            if (success && apkFile != null) {
                                ApkInstallerHelper.SignatureStatus sigStatus = ApkInstallerHelper.INSTANCE.checkSignatureMatch(context, apkFile);
                                if (sigStatus == ApkInstallerHelper.SignatureStatus.MISMATCH) {
                                    RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "SIGNATURE_MISMATCH");
                                    RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, "Downloaded Release package signature does not match installed Debug certificate.");
                                    RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
                                    SystemLogger.w("SilentUpdateWorker", "⚠️ Downloaded APK signature differs from running debug build.");
                                } else {
                                    RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "UPDATE_READY");
                                    RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
                                    SystemLogger.i("SilentUpdateWorker", "✅ Background APK downloaded and verified.");
                                }
                                ApkInstallerHelper.installSilentlyOrPrompt(context, apkFile, remoteVersion);
                            } else {
                                RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "DOWNLOAD_FAILED");
                                RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, "Background download connection failed.");
                                RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
                                SystemLogger.w("SilentUpdateWorker", "⚠️ Background APK download encountered an issue.");
                            }
                            return kotlin.Unit.INSTANCE;
                        });
                    } else {
                        RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "UP_TO_DATE");
                        RegistryManager.putString(context, RegistryManager.KEY_UPDATE_AVAILABLE_TS, "None (Running Latest)");
                        RegistryManager.putBoolean(context, RegistryManager.KEY_IS_UPDATED, true);
                        SystemLogger.i("SilentUpdateWorker", "✅ Local system is up to date with Cloud Firebase.");
                    }
                    return kotlin.Unit.INSTANCE;
                });
            }

            return Result.success();
        } catch (Exception e) {
            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "EXECUTION_ERROR");
            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, e.getMessage());
            RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
            SystemLogger.e("SilentUpdateWorker", "Forensic update execution error", e);
            return Result.retry();
        }
    }
}
