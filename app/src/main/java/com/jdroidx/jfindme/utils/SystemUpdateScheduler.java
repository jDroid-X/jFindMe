package com.jdroidx.jfindme.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Utility to manage the scheduling of silent system updates and maintenance window.
 * Default maintenance schedule: 02:39 AM daily with 10-15 minute execution limit.
 */
public class SystemUpdateScheduler {

    public static final int MAINTENANCE_HOUR = 2;
    public static final int MAINTENANCE_MINUTE = 39;
    public static final String WORK_TAG_SILENT_UPDATE = "SILENT_SYSTEM_UPDATE";
    public static final String UNIQUE_WORK_NAME = "SILENT_SYSTEM_UPDATE_WORK";
    public static final String KEY_UPDATE_DEFERRED_OFFLINE = "update_deferred_offline";
    public static final String ACTION_MAINTENANCE_WAKEUP = "com.jdroidx.jfindme.action.MAINTENANCE_WAKEUP";

    /**
     * Checks update criteria and schedules the 02:39 AM maintenance window upon app startup.
     */
    public static void scheduleCheck(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("silent_update_enabled", true)) return;

        // 🛡️ ZERO-TRUST OFFLINE GATE: Do not schedule cloud checks for unauthenticated devices
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            SystemLogger.i("SystemUpdateScheduler", "Device unauthenticated. Cloud update scheduler deferred until login.");
            return;
        }

        scheduleMaintenanceWindow(context);
    }

    /**
     * Master Scheduler: Schedules the SilentUpdateWorker to trigger at 02:39 AM with power & network gates.
     */
    public static void scheduleMaintenanceWindow(Context context) {
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        
        // Target: Read dynamic editable maintenance window from RegistryManager (Defaults: 02:39 AM)
        SharedPreferences prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE);
        int targetHour = prefs.getInt(RegistryManager.KEY_MAINTENANCE_WINDOW_HOUR, MAINTENANCE_HOUR);
        int targetMinute = prefs.getInt(RegistryManager.KEY_MAINTENANCE_WINDOW_MINUTE, MAINTENANCE_MINUTE);

        calendar.set(Calendar.HOUR_OF_DAY, targetHour);
        calendar.set(Calendar.MINUTE, targetMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If it's already past 02:39 AM, schedule for tomorrow
        if (calendar.getTimeInMillis() <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long delay = calendar.getTimeInMillis() - now;

        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // Battery must be >= 20%
            .build();

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(SilentUpdateWorker.class)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG_SILENT_UPDATE)
            .build();

        // Use UNIQUE work to prevent duplicate race conditions
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            updateRequest
        );

        // ⏰ AlarmManager Doze-Punch Wakeup Backup (RAG-02: ±60s timing precision)
        try {
            android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                android.content.Intent alarmIntent = new android.content.Intent(context, com.jdroidx.jfindme.receiver.StealthAlarmReceiver.class);
                alarmIntent.setAction(ACTION_MAINTENANCE_WAKEUP);
                android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                    context,
                    9021,
                    alarmIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ? android.app.PendingIntent.FLAG_IMMUTABLE : 0)
                );
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    android.app.AlarmManager.AlarmClockInfo clockInfo = new android.app.AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pi);
                    am.setAlarmClock(clockInfo, pi);
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
                } else {
                    am.setExact(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
                }
                SystemLogger.i("SystemUpdateScheduler", "Doze-punch AlarmClock set for maintenance window.");
            }
        } catch (Exception e) {
            SystemLogger.w("SystemUpdateScheduler", "AlarmManager wakeup scheduling warning: " + e.getMessage());
        }

        SystemLogger.i("SystemUpdateScheduler", "Forensic 02:39 AM maintenance update scheduled with " + (delay / 3600000) + "h " + ((delay % 3600000) / 60000) + "m delay.");
    }

    /**
     * Backward-compatible alias for existing callers.
     */
    public static void scheduleUpdateAt303AM(Context context) {
        scheduleMaintenanceWindow(context);
    }

    /**
     * Reactive Reconnect Handler: Triggered when internet is restored after an offline maintenance window.
     */
    public static void triggerImmediateMaintenance(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build();

        OneTimeWorkRequest immediateRequest = new OneTimeWorkRequest.Builder(SilentUpdateWorker.class)
            .setConstraints(constraints)
            .addTag(WORK_TAG_SILENT_UPDATE)
            .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
            "MaintenanceAlarmWakeup",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        );
        SystemLogger.i("SystemUpdateScheduler", "AlarmManager Doze-punch triggered immediate maintenance worker.");
    }

    public static void checkDeferredUpdateOnReconnect(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE);
        boolean isDeferred = prefs.getBoolean(KEY_UPDATE_DEFERRED_OFFLINE, false);
        if (isDeferred) {
            SystemLogger.i("SystemUpdateScheduler", "Network restored. Triggering deferred system update check.");
            prefs.edit().putBoolean(KEY_UPDATE_DEFERRED_OFFLINE, false).apply();
            
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

            OneTimeWorkRequest immediateRequest = new OneTimeWorkRequest.Builder(SilentUpdateWorker.class)
                .setConstraints(constraints)
                .addTag(WORK_TAG_SILENT_UPDATE)
                .build();

            WorkManager.getInstance(context).enqueueUniqueWork(
                "DeferredUpdateImmediate",
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            );
        }
    }
}
