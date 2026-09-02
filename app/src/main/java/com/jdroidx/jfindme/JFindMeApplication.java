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

        // 🚀 QA Fix: Global Enterprise Database Persistence
        // MUST execute immediately on application start before any ViewModels map Firebase nodes
        try {
            com.google.firebase.database.FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {}
        
        SystemLogger.init(this);
        SystemLogger.i("AppLifecycle", "JFindMeApplication onCreate - App Started with Secure Offline Sync");
        
        // 🚀 L7 Silent Update Check: Execute update check at boot-up
        com.jdroidx.jfindme.utils.SystemUpdateScheduler.scheduleCheck(this);

        // 🛠️ Initialize background jobs & sentinels with persisted frequencies
        com.jdroidx.jfindme.service.BackgroundJobManager.INSTANCE.initializeAllJobs(this);

        // 🚀 Persistent WhatsApp-Grade Background Service Auto-Start
        try {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ||
                com.jdroidx.jfindme.utils.RegistryManager.isAutoLoginEnabled(this)) {
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
