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
        // 🚀 QA Fix: Global Enterprise Database Persistence
        // MUST execute immediately on application start before any ViewModels map Firebase nodes
        try {
            com.google.firebase.database.FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {}
        
        SystemLogger.init(this);
        SystemLogger.i("AppLifecycle", "JFindMeApplication onCreate - App Started with Secure Offline Sync");
    }
}
