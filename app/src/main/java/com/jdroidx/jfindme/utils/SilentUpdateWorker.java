package com.jdroidx.jfindme.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.jdroidx.jfindme.data.remote.OfflineSyncManager;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 👑 SilentUpdateWorker: Robust Headless Background Maintenance & In-Place System Upgrades.
 * Enforces a strict 14-minute execution watchdog (02:39 AM nominal window).
 * Features:
 * 1. Synchronous latch-guarded WorkManager execution.
 * 2. Power gate: Battery >= 20% or AC charging.
 * 3. Network metering gate: Cellular protects user data; Wi-Fi streams full package.
 * 4. Yesterday ↔ Today balance data delta reconciliation.
 * 5. Pre-install SQLite WAL checkpointing to prevent corrupted journals.
 * 6. Android 12+ (API 31+) unattended in-place package session commit.
 */
public class SilentUpdateWorker extends Worker {

    private static final String TAG = "SilentUpdateWorker";
    private static final long MIN_STORAGE_REQUIRED = 200 * 1024 * 1024L; // 200 MB
    private static final int MIN_BATTERY_LEVEL = 20;

    public SilentUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // 🛡️ ZERO-TRUST OFFLINE GATE: Suppress cloud update scans for unauthenticated devices
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            SystemLogger.i(TAG, "Node unauthenticated. Silent cloud update check deferred until authorized user login.");
            return Result.success();
        }

        SystemLogger.i(TAG, "Initiating 02:39 AM maintenance window, balance delta sync & update audit.");

        String agentId = RegistryManager.getAgentId(context);
        if (!agentId.isEmpty()) {
            // 1. Synchronize Access Permissions Matrix to Firebase
            FirebaseComm.syncDeviceAccessPermissions(context, agentId);
            
            // 2. Synchronize Current Version Metadata to Firebase
            FirebaseComm.syncVersionUpdateMetadata(context, agentId, "MAINTENANCE_SCAN");
        }

        // 3. Reconcile Yesterday ↔ Today Balance Data Delta First
        try {
            int balanced = OfflineSyncManager.getInstance(context).reconcileYesterdayTodayBalance();
            SystemLogger.i(TAG, "Balance delta reconciliation completed: " + balanced + " offline records processed.");
        } catch (Exception e) {
            SystemLogger.w(TAG, "Balance delta reconciliation warning: " + e.getMessage());
        }

        boolean isEnabled = RegistryManager.isSilentUpdateEnabled(context);
        if (!isEnabled) {
            SystemLogger.i(TAG, "Silent update disabled by user preference. Skipping package check.");
            return Result.success();
        }

        // 4. Power & Battery Gate
        if (!ApkInstallerHelper.INSTANCE.hasAdequateBattery(context, MIN_BATTERY_LEVEL)) {
            SystemLogger.w(TAG, "Battery level below " + MIN_BATTERY_LEVEL + "%. Aborting background update to protect power.");
            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "LOW_BATTERY");
            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, "Battery < " + MIN_BATTERY_LEVEL + "%");
            return Result.success();
        }

        // 5. Network Metering Check
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities caps = cm != null ? cm.getNetworkCapabilities(cm.getActiveNetwork()) : null;
        boolean isMetered = caps != null && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        boolean allowMetered = RegistryManager.getBoolean(context, "allow_metered_updates", false);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Result> finalResult = new AtomicReference<>(Result.success());

        try {
            String nowStr = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(new Date());
            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_TIME, nowStr);
            RegistryManager.putString(context, RegistryManager.KEY_LAST_CHECK_TS, nowStr);

            if (!agentId.isEmpty()) {
                FirebaseComm.checkForVersionDifference(context, agentId, (hasNew, remoteVersion, remoteCode, downloadUrl, size) -> {
                    try {
                        if (hasNew) {
                            SystemLogger.i(TAG, "🚀 New Remote Version Detected: V" + remoteVersion + " (Build " + remoteCode + ")");
                            RegistryManager.putString(context, RegistryManager.KEY_UPDATE_AVAILABLE_TS, "V" + remoteVersion + " (Build " + remoteCode + ")");
                            RegistryManager.putBoolean(context, RegistryManager.KEY_IS_UPDATED, false);

                            // Storage Gate
                            if (!ApkInstallerHelper.INSTANCE.hasAvailableStorage(context, MIN_STORAGE_REQUIRED)) {
                                RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "STORAGE_FULL");
                                RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, "Insufficient storage for update package.");
                                RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
                                latch.countDown();
                                return kotlin.Unit.INSTANCE;
                            }

                            // Cellular Metering Protection
                            if (isMetered && !allowMetered) {
                                SystemLogger.i(TAG, "Metered cellular connection detected. Deferring 65MB APK download until unmetered Wi-Fi.");
                                RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "METERED_DEFERRED");
                                RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, "Waiting for unmetered Wi-Fi.");
                                latch.countDown();
                                return kotlin.Unit.INSTANCE;
                            }

                            // Trigger Change Log Audit
                            FirebaseComm.logChange(
                                agentId,
                                "System_Update",
                                "UPDATE_AVAILABLE_DETECTED",
                                "BUILD_" + remoteCode,
                                Collections.singletonMap("remote_version", "V" + remoteVersion),
                                agentId
                            );

                            String targetUrl = (downloadUrl != null && !downloadUrl.isEmpty())
                                ? downloadUrl
                                : RegistryManager.getSystemUpdateLink(context);

                            File existingTargetApk = new File(context.getCacheDir(), "update_jfindme.apk");
                            if (existingTargetApk.exists() && ApkInstallerHelper.isValidApkFile(context, existingTargetApk)) {
                                ApkInstallerHelper.VersionStatus vStatus = ApkInstallerHelper.INSTANCE.checkVersionStatus(context, existingTargetApk);
                                if (vStatus == ApkInstallerHelper.VersionStatus.UPGRADE) {
                                    SystemLogger.i(TAG, "Update package already exists in cache and is a verified upgrade. Proceeding to silent install.");
                                    
                                    // Flush SQLite WAL pages before installation
                                    DatabaseHelper.getInstance(context).forceWalCheckpoint();
                                    
                                    ApkInstallerHelper.installSilentlyOrPrompt(context, existingTargetApk, remoteVersion, "");
                                    latch.countDown();
                                    return kotlin.Unit.INSTANCE;
                                } else {
                                    try { existingTargetApk.delete(); } catch (Exception ignored) {}
                                }
                            }

                            SystemLogger.i(TAG, "Initiating background APK streaming from: " + targetUrl);
                            ApkInstallerHelper.downloadApkInBackground(context, targetUrl, (success, apkFile) -> {
                                try {
                                    if (success && apkFile != null) {
                                        ApkInstallerHelper.SignatureStatus sigStatus = ApkInstallerHelper.INSTANCE.checkSignatureMatch(context, apkFile);
                                        if (sigStatus == ApkInstallerHelper.SignatureStatus.MISMATCH) {
                                            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "SIGNATURE_MISMATCH");
                                            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, "Downloaded Release package signature does not match installed Debug certificate.");
                                            RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
                                            SystemLogger.w(TAG, "⚠️ Downloaded APK signature differs from running build.");
                                        } else {
                                            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "UPDATE_READY");
                                            RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
                                            SystemLogger.i(TAG, "✅ Background APK verified. Checkpointing SQLite and committing install.");
                                            
                                            // Force SQLite WAL checkpoint before replacing binary
                                            DatabaseHelper.getInstance(context).forceWalCheckpoint();
                                            
                                            ApkInstallerHelper.installSilentlyOrPrompt(context, apkFile, remoteVersion, "");
                                        }
                                    } else {
                                        RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "DOWNLOAD_FAILED");
                                        RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, "Background download connection failed.");
                                        RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
                                        finalResult.set(Result.retry());
                                    }
                                } finally {
                                    latch.countDown();
                                }
                                return kotlin.Unit.INSTANCE;
                            });
                        } else {
                            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "UP_TO_DATE");
                            RegistryManager.putString(context, RegistryManager.KEY_UPDATE_AVAILABLE_TS, "None (Running Latest)");
                            RegistryManager.putBoolean(context, RegistryManager.KEY_IS_UPDATED, true);
                            SystemLogger.i(TAG, "✅ Local system is up to date with Cloud Firebase.");
                            latch.countDown();
                        }
                    } catch (Exception ex) {
                        SystemLogger.e(TAG, "Version processing error", ex);
                        latch.countDown();
                    }
                    return kotlin.Unit.INSTANCE;
                });
            } else {
                latch.countDown();
            }

            // Enforce 14-minute watchdog limit so background execution never runs wild
            boolean completedInTime = latch.await(14, TimeUnit.MINUTES);
            if (!completedInTime) {
                SystemLogger.w(TAG, "14-minute maintenance window watchdog reached. Releasing background execution.");
                return Result.retry();
            }

            return finalResult.get();
        } catch (Exception e) {
            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "EXECUTION_ERROR");
            RegistryManager.putString(context, RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, e.getMessage());
            RegistryManager.putBoolean(context, RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, true);
            SystemLogger.e(TAG, "Forensic update execution error", e);
            return Result.retry();
        }
    }
}
