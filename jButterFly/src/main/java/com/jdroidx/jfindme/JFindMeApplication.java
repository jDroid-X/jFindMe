package com.jdroidx.jfindme;

import android.app.Application;
import com.jdroidx.jfindme.utils.SystemLogger;

public class JFindMeApplication extends Application {
    private static JFindMeApplication instance;
    private static int runningActivityCount = 0;
    private static final java.util.List<AppLifecycleListener> lifecycleListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public interface AppLifecycleListener {
        void onAppForegroundStateChanged(boolean isForeground);
    }

    public static boolean isAppInForeground() {
        return runningActivityCount > 0;
    }

    public static void addLifecycleListener(AppLifecycleListener listener) {
        if (listener != null && !lifecycleListeners.contains(listener)) {
            lifecycleListeners.add(listener);
            listener.onAppForegroundStateChanged(isAppInForeground());
        }
    }

    public static void removeLifecycleListener(AppLifecycleListener listener) {
        if (listener != null) {
            lifecycleListeners.remove(listener);
        }
    }

    private static void notifyLifecycleListeners(boolean isForeground) {
        for (AppLifecycleListener l : lifecycleListeners) {
            try {
                l.onAppForegroundStateChanged(isForeground);
            } catch (Exception ignored) {}
        }
    }

    public static JFindMeApplication getContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 🛰️ Phase 1 SSOT: Real-Time Application Lifecycle Tracking
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {}
            @Override public void onActivityStarted(android.app.Activity activity) {
                runningActivityCount++;
                if (runningActivityCount == 1) {
                    SystemLogger.i("AppLifecycle", "App transitioned to FOREGROUND (Active Activities: " + runningActivityCount + ")");
                    notifyLifecycleListeners(true);
                }
            }
            @Override public void onActivityResumed(android.app.Activity activity) {}
            @Override public void onActivityPaused(android.app.Activity activity) {}
            @Override public void onActivityStopped(android.app.Activity activity) {
                runningActivityCount = Math.max(0, runningActivityCount - 1);
                if (runningActivityCount == 0) {
                    SystemLogger.i("AppLifecycle", "App transitioned to BACKGROUND (All Activities Stopped)");
                    notifyLifecycleListeners(false);
                }
            }
            @Override public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle outState) {}
            @Override public void onActivityDestroyed(android.app.Activity activity) {}
        });
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
                // 🛡️ FORENSIC UPGRADE: Register v2 channel to guarantee IMPORTANCE_MIN even on existing app installs
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "tracking_channel_v2",
                    "jFindMe Active Tracking Sentinel",
                    android.app.NotificationManager.IMPORTANCE_MIN
                );
                channel.setDescription("Maintains continuous real-time telemetry and geofence safety monitoring.");
                channel.setShowBadge(false);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, null);
                nm.createNotificationChannel(channel);
                try {
                    nm.deleteNotificationChannel("tracking_channel");
                } catch (Exception ignored) {}

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

        // 🚀 Asynchronous Background Task Initializer: Keeps cold-start boot time under 10ms
        new Thread(() -> {
            try {
                // Step 5: Initialize background jobs & sentinels
                com.jdroidx.jfindme.service.BackgroundJobManager.INSTANCE.initializeAllJobs(JFindMeApplication.this);

                // Step 6: Silent Update Check
                com.jdroidx.jfindme.utils.SystemUpdateScheduler.scheduleCheck(JFindMeApplication.this);

                // Step 6.5: Initialize Stealth Alarm Sentinel Engine (AlarmClock RTC Wakeup)
                try {
                    com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.scheduleNextStealthPulse(JFindMeApplication.this, 0L);
                    SystemLogger.i("AppLifecycle", "StealthAlarmSentinelEngine scheduled on background boot.");
                } catch (Exception e) {
                    SystemLogger.w("AppLifecycle", "StealthAlarmSentinelEngine init notice: " + e.getMessage());
                }

                // Step 7: Safe Persistent Tracking Service Auto-Start (Only if permissions are verified)
                try {
                    boolean hasLoc = androidx.core.content.ContextCompat.checkSelfPermission(JFindMeApplication.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                     androidx.core.content.ContextCompat.checkSelfPermission(JFindMeApplication.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;

                    if (hasLoc && (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ||
                        com.jdroidx.jfindme.utils.RegistryManager.isAutoLoginEnabled(JFindMeApplication.this))) {
                        android.content.Intent serviceIntent = new android.content.Intent(JFindMeApplication.this, com.jdroidx.jfindme.service.TrackingService.class);
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent);
                        } else {
                            startService(serviceIntent);
                        }
                        SystemLogger.i("AppLifecycle", "TrackingService auto-started in foreground on app boot.");
                    }
                } catch (Exception e) {
                    SystemLogger.w("AppLifecycle", "TrackingService auto-start notice: " + e.getMessage());
                }
            } catch (Exception ex) {
                SystemLogger.e("AppLifecycle", "Background bootstrap exception: " + ex.getMessage());
            }
        }).start();
    }
}
