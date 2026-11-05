package com.jdroidx.jfindme;

import android.app.Application;
import com.jdroidx.jfindme.utils.SystemLogger;

public class JFindMeApplication extends Application {
    private static JFindMeApplication instance;

    public static JFindMeApplication getContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // 🛡️ Enterprise FinalizerDaemon & Conscrypt Socket Crash Shield
        // Neutralizes known OEM/Conscrypt finalize() NullPointerExceptions thrown during SSL socket garbage collection
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof NullPointerException && 
                (thread.getName().contains("FinalizerDaemon") || 
                 (throwable.getMessage() != null && throwable.getMessage().contains("monitor-enter")) ||
                 (throwable.getStackTrace().length > 0 && throwable.getStackTrace()[0].getClassName().contains("Conscrypt")))) {
                SystemLogger.w("FinalizerShield", "Swallowed harmless Conscrypt/FinalizerDaemon SSL socket NPE: " + throwable.getMessage());
                return;
            }
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });

        // 🚀 Step 2: Global Enterprise Database Persistence (10MB bounded cache to prevent CursorWindow overflow)
        try {
            com.google.firebase.database.FirebaseDatabase fb = com.google.firebase.database.FirebaseDatabase.getInstance();
            fb.setPersistenceEnabled(true);
            fb.setPersistenceCacheSizeBytes(10 * 1024 * 1024L);
        } catch (Exception e) {}
        
        // 🚀 Step 3: Global System Logger Initialization
        SystemLogger.init(this);
        SystemLogger.i("AppLifecycle", "JFindMeApplication onCreate - App Started with Secure Offline Sync");
        // [Variables] | [Constants] | [Dependencies] | [Integration Points]
        // Proactive SHA-256 Migration: Ensures master purge safety phrase is cryptographically encrypted
        com.jdroidx.jfindme.utils.RegistryManager.ensurePurgePhraseEncrypted(this);
        
        // 🚀 Step 4: Synchronous Notification Channel Registration (Pre-requisite for all Services & Alerts)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "tracking_channel",
                    "jFindMe Active Tracking Sentinel",
                    android.app.NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Maintains continuous real-time telemetry and geofence safety monitoring.");
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);

                android.app.NotificationChannel alertChannel = new android.app.NotificationChannel(
                    "jfindme_alerts",
                    "jFindMe Security & SOS Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                );
                alertChannel.setDescription("High-priority SafeZone breaches and Emergency SOS broadcasts.");
                alertChannel.enableVibration(true);
                nm.createNotificationChannel(alertChannel);
            }
        }

        // 🚀 Step 5: Initialize background jobs & sentinels
        com.jdroidx.jfindme.service.BackgroundJobManager.INSTANCE.initializeAllJobs(this);

        // 🚀 Step 6: Silent Update Check
        com.jdroidx.jfindme.utils.SystemUpdateScheduler.scheduleCheck(this);

        // 🚀 Step 6.5: Initialize Stealth Alarm Sentinel Engine (AlarmClock RTC Wakeup)
        try {
            com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.scheduleNextStealthPulse(this, 0L);
            SystemLogger.i("AppLifecycle", "StealthAlarmSentinelEngine scheduled on application launch.");
        } catch (Exception e) {
            SystemLogger.e("AppLifecycle", "Failed to initialize StealthAlarmSentinelEngine", e);
        }

        // 🚀 Step 7: Safe Persistent Tracking Service Auto-Start (Only if permissions are verified)
        try {
            boolean hasLoc = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                             androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;

            if (hasLoc && (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ||
                com.jdroidx.jfindme.utils.RegistryManager.isAutoLoginEnabled(this))) {
                android.content.Intent serviceIntent = new android.content.Intent(this, com.jdroidx.jfindme.service.TrackingService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                SystemLogger.i("AppLifecycle", "TrackingService auto-started in foreground on app boot.");
            }
        } catch (Exception e) {
            SystemLogger.e("AppLifecycle", "Failed to auto-start TrackingService on app boot", e);
        }
    }
}
