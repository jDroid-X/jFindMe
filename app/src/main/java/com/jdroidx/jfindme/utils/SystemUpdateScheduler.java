package com.jdroidx.jfindme.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Utility to manage the scheduling of silent system updates.
 * Schedules updates for 3:03 AM in the morning.
 */
public class SystemUpdateScheduler {

    /**
     * Periodically check for updates as requested (silently on app startup).
     */
    public static void scheduleCheck(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("silent_update_enabled", true)) return;

        // 🛡️ ZERO-TRUST OFFLINE GATE: Do not schedule cloud checks for unauthenticated devices
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            SystemLogger.i("SystemUpdateScheduler", "Device unauthenticated. Cloud update scheduler deferred until login.");
            return;
        }

        // Simulate a silent check. In a real scenario, this would be an API call.
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait for boot-up stabilization
                boolean updateFound = true; // Simulated update detection
                
                if (updateFound) {
                    SystemLogger.i("SystemUpdateScheduler", "Deltas detected. Scheduling silent sync for 03:03 AM.");
                    scheduleUpdateAt303AM(context);
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }

    /**
     * Specifically schedules the SilentUpdateWorker to trigger around 3:03 AM.
     */
    public static void scheduleUpdateAt303AM(Context context) {
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        
        // Target: Today at 03:03 AM
        calendar.set(Calendar.HOUR_OF_DAY, 3);
        calendar.set(Calendar.MINUTE, 3);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If it's already past 3:03 AM, schedule for tomorrow
        if (calendar.getTimeInMillis() <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long delay = calendar.getTimeInMillis() - now;

        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Handles Data or WiFi requirement
            .build();

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(SilentUpdateWorker.class)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag("SILENT_SYSTEM_UPDATE")
            .build();

        WorkManager.getInstance(context).enqueue(updateRequest);
        SystemLogger.i("SystemUpdateScheduler", "Forensic update scheduled with " + (delay / 3600000) + "h delay.");
    }
}
