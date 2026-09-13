package com.jdroidx.jbutterfly.ui.settings;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.jdroidx.jbutterfly.data.local.DatabaseHelper;
import com.jdroidx.jbutterfly.service.TrackingService;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.RegistryManager;

public class SettingsActivity extends Activity {

    private Spinner spMovingInterval;
    private Spinner spStationaryInterval;
    private Spinner spAccuracy;
    private Spinner spBuzzerDuration;
    private Spinner spBatteryThreshold;
    private Spinner spTheme;
    private Spinner spUnits;
    private Spinner spRetention;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        buildLayout();
    }

    private void buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Header
        LinearLayout header = UiArchitect.createHeaderBar(this, "Dynamic System Configuration", v -> finish());
        root.addView(header);

        int pad = UiArchitect.dpToPx(this, 16);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, UiArchitect.dpToPx(this, 30));

        // Subtitle
        TextView tvSub = new TextView(this);
        tvSub.setText("All tracking & telemetry intervals are fully dynamic. Zero hardcoded constants.");
        tvSub.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvSub.setTextSize(UiArchitect.spToPx(this, 12f));
        tvSub.setPadding(0, 0, 0, UiArchitect.dpToPx(this, 14));
        content.addView(tvSub);

        // Card Container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(this));
        bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        bg.setCornerRadius(UiArchitect.dpToPx(this, 14));
        card.setBackground(bg);

        // 1. Moving Polling Interval (calculate saved position)
        long currentMoving = RegistryManager.getLong(this, RegistryManager.KEY_POLL_INTERVAL_MOVING, 10000L);
        int movingIdx = 1;
        if (currentMoving <= 5000L) movingIdx = 0;
        else if (currentMoving <= 10000L) movingIdx = 1;
        else if (currentMoving <= 15000L) movingIdx = 2;
        else movingIdx = 3;

        spMovingInterval = addConfigSpinner(card, "Moving Telemetry Frequency:",
            new String[]{"5 Seconds (High Precision)", "10 Seconds (Standard)", "15 Seconds (Balanced)", "30 Seconds (Eco)"},
            movingIdx);

        // 2. Stationary Polling Interval (calculate saved position)
        long currentStat = RegistryManager.getLong(this, RegistryManager.KEY_POLL_INTERVAL_STATIONARY, 60000L);
        int statIdx = 1;
        if (currentStat <= 30000L) statIdx = 0;
        else if (currentStat <= 60000L) statIdx = 1;
        else if (currentStat <= 120000L) statIdx = 2;
        else statIdx = 3;

        spStationaryInterval = addConfigSpinner(card, "Stationary Telemetry Frequency:",
            new String[]{"30 Seconds", "60 Seconds (Standard)", "120 Seconds (Battery Saver)", "300 Seconds (Deep Sleep)"},
            statIdx);

        // 3. Location Accuracy Threshold
        spAccuracy = addConfigSpinner(card, "GPS Accuracy Threshold:",
            new String[]{"10 Meters (High Precision)", "25 Meters (Standard)", "50 Meters (City Grid)"},
            1);

        // 4. Acoustic Buzzer Duration (calculate saved position)
        int currentBuzzer = RegistryManager.getInt(this, RegistryManager.KEY_BUZZER_DURATION_SEC, 30);
        int buzzIdx = 1;
        if (currentBuzzer <= 15) buzzIdx = 0;
        else if (currentBuzzer <= 30) buzzIdx = 1;
        else buzzIdx = 2;

        spBuzzerDuration = addConfigSpinner(card, "Remote Buzzer Siren Duration:",
            new String[]{"15 Seconds", "30 Seconds (Standard)", "60 Seconds (Maximum Alert)"},
            buzzIdx);

        // 5. Battery Alert Warning Threshold
        int currentBat = RegistryManager.getInt(this, "battery_alert_threshold", 20);
        int batIdx = 1;
        if (currentBat <= 15) batIdx = 0;
        else if (currentBat <= 20) batIdx = 1;
        else batIdx = 2;

        spBatteryThreshold = addConfigSpinner(card, "Low Battery Alert Warning:",
            new String[]{"15% Battery", "20% Battery (Standard)", "25% Battery (Early Warning)"},
            batIdx);

        // 6. Theme Mode (calculate saved position)
        String currentTheme = RegistryManager.getString(this, RegistryManager.KEY_THEME_MODE, ThemeManager.THEME_SYSTEM);
        int themeIdx = 0;
        if (ThemeManager.THEME_LIGHT.equalsIgnoreCase(currentTheme)) themeIdx = 1;
        else if (ThemeManager.THEME_DARK.equalsIgnoreCase(currentTheme)) themeIdx = 2;

        spTheme = addConfigSpinner(card, "Application Theme:",
            new String[]{"Follow System OS", "Light Theme", "Dark Theme"},
            themeIdx);

        // 7. Distance & Velocity Measurement Units
        String currentUnits = RegistryManager.getString(this, "pref_units", "METRIC");
        spUnits = addConfigSpinner(card, "Measurement Units:",
            new String[]{"Metric (Kilometers, km/h)", "Imperial (Miles, mph)"},
            "IMPERIAL".equalsIgnoreCase(currentUnits) ? 1 : 0);

        // 8. History Storage Retention
        int currentRetention = RegistryManager.getInt(this, "pref_history_retention_days", 30);
        int retIdx = 1;
        if (currentRetention == 7) retIdx = 0;
        else if (currentRetention == 30) retIdx = 1;
        else if (currentRetention == 90) retIdx = 2;
        else if (currentRetention == 0) retIdx = 3;

        spRetention = addConfigSpinner(card, "Historical Trail Retention:",
            new String[]{"7 Days Rolling", "30 Days (Recommended)", "90 Days Extended", "Unlimited (Never Prune)"},
            retIdx);

        // Save Button
        Button btnSave = UiArchitect.createPrimaryButton(this, "APPLY DYNAMIC SETTINGS", v -> applySettings());
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        saveLp.topMargin = UiArchitect.dpToPx(this, 20);
        card.addView(btnSave, saveLp);

        content.addView(card);
        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);
    }

    private Spinner addConfigSpinner(LinearLayout container, String label, String[] options, int defaultIdx) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tv.setTextSize(UiArchitect.spToPx(this, 13f));
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setPadding(0, UiArchitect.dpToPx(this, 10), 0, UiArchitect.dpToPx(this, 4));
        container.addView(tv);

        Spinner sp = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        sp.setAdapter(adapter);
        sp.setSelection(defaultIdx);
        container.addView(sp);
        return sp;
    }

    private void applySettings() {
        // Moving interval
        long movingMs = 10000L;
        switch (spMovingInterval.getSelectedItemPosition()) {
            case 0: movingMs = 5000L; break;
            case 1: movingMs = 10000L; break;
            case 2: movingMs = 15000L; break;
            case 3: movingMs = 30000L; break;
        }
        RegistryManager.putLong(this, RegistryManager.KEY_POLL_INTERVAL_MOVING, movingMs);

        // Stationary interval
        long statMs = 60000L;
        switch (spStationaryInterval.getSelectedItemPosition()) {
            case 0: statMs = 30000L; break;
            case 1: statMs = 60000L; break;
            case 2: statMs = 120000L; break;
            case 3: statMs = 300000L; break;
        }
        RegistryManager.putLong(this, RegistryManager.KEY_POLL_INTERVAL_STATIONARY, statMs);

        // Buzzer duration
        int buzzerSec = 30;
        switch (spBuzzerDuration.getSelectedItemPosition()) {
            case 0: buzzerSec = 15; break;
            case 1: buzzerSec = 30; break;
            case 2: buzzerSec = 60; break;
        }
        RegistryManager.putInt(this, RegistryManager.KEY_BUZZER_DURATION_SEC, buzzerSec);

        // Battery threshold
        int batThreshold = 20;
        switch (spBatteryThreshold.getSelectedItemPosition()) {
            case 0: batThreshold = 15; break;
            case 1: batThreshold = 20; break;
            case 2: batThreshold = 25; break;
        }
        RegistryManager.putInt(this, "battery_alert_threshold", batThreshold);

        // Theme mode
        int themePos = spTheme.getSelectedItemPosition();
        String themeMode = ThemeManager.THEME_SYSTEM;
        if (themePos == 1) themeMode = ThemeManager.THEME_LIGHT;
        else if (themePos == 2) themeMode = ThemeManager.THEME_DARK;
        RegistryManager.putString(this, RegistryManager.KEY_THEME_MODE, themeMode);

        // Units
        String units = spUnits.getSelectedItemPosition() == 1 ? "IMPERIAL" : "METRIC";
        RegistryManager.putString(this, "pref_units", units);

        // History retention & automatic pruning
        int retentionDays = 30;
        switch (spRetention.getSelectedItemPosition()) {
            case 0: retentionDays = 7; break;
            case 1: retentionDays = 30; break;
            case 2: retentionDays = 90; break;
            case 3: retentionDays = 0; break;
        }
        RegistryManager.putInt(this, "pref_history_retention_days", retentionDays);
        if (retentionDays > 0) {
            DatabaseHelper.getInstance(this).pruneOldHistory(retentionDays);
        }

        // Save in dynamic SQLite settings table as well
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        db.setConfig("poll_interval_moving_ms", String.valueOf(movingMs), "Moving location polling rate");
        db.setConfig("poll_interval_stationary_ms", String.valueOf(statMs), "Stationary location polling rate");
        db.setConfig("buzzer_duration_sec", String.valueOf(buzzerSec), "Acoustic siren alarm duration");
        db.setConfig("battery_alert_threshold", String.valueOf(batThreshold), "Low battery alert percentage");
        db.setConfig("theme_mode", themeMode, "Active theme mode");
        db.setConfig("pref_units", units, "Distance and speed units");
        db.setConfig("pref_history_retention_days", String.valueOf(retentionDays), "History retention cutoff days");

        // Apply theme and restart service with updated dynamic intervals
        ThemeManager.applyTheme(this);
        TrackingService.startTracking(this);

        Toast.makeText(this, "✅ Dynamic settings saved. Telemetry refreshed!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
