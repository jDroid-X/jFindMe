package com.jdroidx.jfindme.ui.settings;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.auth.AuthActivity;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.ThemeManager;
import com.jdroidx.jfindme.ui.base.ListItemTable;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;
import com.jdroidx.jfindme.utils.RegistryManager;

/**
 * Main -> UI Engine -> SettingsScreen -> SettingsActivity
 * Level 5: Implementation of Theme Management, Multi-Device Pairing Hub, and Account Controls.
 */
public class SettingsActivity extends BaseActivityJava {

    private final String TAG = "SettingsActivity";

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // ≡ƒÜÇ Header Matrix: Restored as per Control Hub Screenshot
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header); 

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("CONTROL HUB");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_HEADER"));
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        titleRow.addView(title);

        // 🧬 V62.8: Master Theme Switcher Correction
        android.widget.ImageView themeToggle = new android.widget.ImageView(this);
        themeToggle.setImageResource(getResolvedThemeId() == 1 ? android.R.drawable.ic_menu_day : android.R.drawable.ic_menu_recent_history); 
        themeToggle.setColorFilter(getGradientOverlayText());
        int tPad = scale(15);
        themeToggle.setPadding(tPad, tPad, tPad, tPad);
        themeToggle.setOnClickListener(v -> {
            int current = prefs.getInt("theme_id", 0);
            // 🔧 FIX: Use actual theme count from ThemeManager instead of hardcoded 8
            int totalThemes = com.jdroidx.jfindme.ui.base.ThemeManager.THEMES.length;
            prefs.edit().putInt("theme_id", (current + 1) % totalThemes).apply();
            safeRecreate(); // 🛡️ Stability Patch: Bypasses OEM Lifecycle crashes
        });
        titleRow.addView(themeToggle, new LinearLayout.LayoutParams(scale(80), scale(80)));

        header.addView(titleRow);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Device Telemetry & Engine Architecture");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_SUB"));
        header.addView(subtitle);
        
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int setPad = getResponsivePaddingQuota();
        content.setPadding(setPad * 2, setPad, setPad * 2, setPad * 4);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 15, 0, 15);

        // --- LAYER 1: MASTER UI OVERRIDE ---
        content.addView(createSectionHeader("MASTER UI OVERRIDE"));
        LinearLayout themeCard = new LinearLayout(this);
        themeCard.setOrientation(LinearLayout.VERTICAL);
        
        themeCard.addView(createIconButton("GLOBAL UI & ICON ARCHITECT", com.jdroidx.jfindme.R.drawable.ic_edit, v -> {
            startActivity(new Intent(this, UiConfigActivity.class));
        }), btnLp);
        content.addView(themeCard, getCardParams());
        // --- LAYER 1.5: 🔋 BATTERY & PING INTELLIGENCE ENTRY ---
        content.addView(createSectionHeader("🔋 BATTERY OPTIMIZATION & PING INTELLIGENCE"));
        View batteryLauncher = createIconButton("🔋 BATTERY OPTIMIZATION & PING HUB", com.jdroidx.jfindme.R.drawable.ic_device_health, true, v -> showBatteryOptimizerDialog());
        content.addView(batteryLauncher, btnLp);

        // --- LAYER 1.6: 🏃 HARDWARE MOTION & ACTIVITY RECOGNITION ---
        content.addView(createSectionHeader("🏃 HARDWARE MOTION & ACTIVITY RECOGNITION"));
        LinearLayout motionCard = new LinearLayout(this);
        motionCard.setOrientation(LinearLayout.VERTICAL);

        boolean actRecogEnabled = RegistryManager.isActivityRecognitionEnabled(this);
        View actRecogSwitch = UiArchitect.createSwitch(this, "Hardware Motion Co-Processor (Zero Still GPS)", actRecogEnabled, (v, isChecked) -> {
            RegistryManager.setActivityRecognitionEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Motion Sentinel", isChecked ? "Hardware co-processor active (GPS sleeps when still)." : "Continuous background tracking preference active.", false, 2500, null, null);
        });
        motionCard.addView(actRecogSwitch, btnLp);

        long stillInt = RegistryManager.getMotionStillIntervalMs(this) / 1000;
        long vehInt = RegistryManager.getMotionVehicleIntervalMs(this) / 1000;
        long walkInt = RegistryManager.getMotionWalkingIntervalMs(this) / 1000;
        String motionLabel = "🏃 CONFIGURE DUTY CYCLES (STILL: " + stillInt + "S | CAR: " + vehInt + "S | WALK: " + walkInt + "S)";
        motionCard.addView(createIconButton(motionLabel, com.jdroidx.jfindme.R.drawable.ic_settings_gears, v -> showMotionDutyCyclesDialog()), btnLp);

        content.addView(motionCard, getCardParams());

        // --- LAYER 1.7: 🛣️ ROAD SMOOTHING & KALMAN SENSITIVITY ---
        content.addView(createSectionHeader("🛣️ ROAD SMOOTHING & KALMAN SENSITIVITY"));
        LinearLayout roadCard = new LinearLayout(this);
        roadCard.setOrientation(LinearLayout.VERTICAL);

        boolean roadSmoothingEnabled = RegistryManager.isRoadSmoothingEnabled(this);
        View roadSmoothingSwitch = UiArchitect.createSwitch(this, "Road Alignment & 2D Kalman Filter", roadSmoothingEnabled, (v, isChecked) -> {
            RegistryManager.setRoadSmoothingEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Road Smoothing", isChecked ? "Kalman filtering and road-aligned azimuth active." : "Raw GNSS coordinates rendered without filter.", false, 2500, null, null);
        });
        roadCard.addView(roadSmoothingSwitch, btnLp);

        float emaFactor = RegistryManager.getHeadingEmaFactor(this);
        float kalNoise = RegistryManager.getKalmanMeasurementNoise(this);
        String roadLabel = "🛣️ KALMAN NOISE (R=" + (int)kalNoise + ") & EMA FACTOR (" + emaFactor + ")";
        roadCard.addView(createIconButton(roadLabel, com.jdroidx.jfindme.R.drawable.ic_nav_map, v -> showRoadSmoothingSensitivityDialog()), btnLp);

        content.addView(roadCard, getCardParams());

        // --- LAYER 1.8: ⚙️ BACKGROUND JOBS & SENTINELS ---
        content.addView(createSectionHeader("⚙️ BACKGROUND JOBS & SENTINELS"));
        View bgJobsLauncher = createIconButton("⚙️ BACKGROUND JOBS & ENGINE SENTINELS", com.jdroidx.jfindme.R.drawable.ic_settings_gears, true, v -> BackgroundJobsConfigDialog.show(this));
        content.addView(bgJobsLauncher, btnLp);

        // --- LAYER 2: ADVANCED APP TELEMETRY ---
        content.addView(createSectionHeader("ADVANCED APP TELEMETRY"));
        LinearLayout engineCard = new LinearLayout(this);
        engineCard.setOrientation(LinearLayout.VERTICAL);

        boolean currentCluster = prefs.getBoolean("pref_smart_avatar_clustering", true);
        View clusterSwitch = UiArchitect.createSwitch(this, "Smart Avatar Clustering (10m grouping)", currentCluster, (v, isChecked) -> {
            prefs.edit().putBoolean("pref_smart_avatar_clustering", isChecked).apply();
            UiArchitect.showSlideNotificationBanner(this, "Clustering Updated", isChecked ? "Avatars within 10m will be grouped." : "Individual avatar pins active.", false, 2500, null, null);
        });
        engineCard.addView(clusterSwitch, btnLp);

        boolean isMetric = prefs.getBoolean("pref_unit_metric", true);
        View unitToggle = UiArchitect.createSegmentedToggle(this, "METRIC (KM)", "IMPERIAL (MI)", isMetric, isLeft -> {
            prefs.edit().putBoolean("pref_unit_metric", isLeft).apply();
            UiArchitect.showSlideNotificationBanner(this, "Units Preference", isLeft ? "Distance Units: Metric (km/h)" : "Distance Units: Imperial (mph)", false, 2500, null, null);
        });
        engineCard.addView(unitToggle, btnLp);

        engineCard.addView(createIconButton("VECTOR MAP RENDERER", com.jdroidx.jfindme.R.drawable.ic_nav_map, v -> {
            UiArchitect.showSlideNotificationBanner(this, "Vector Renderer", "Hardware Accelerated Vector Engine Active", false, 2500, null, null);
        }), btnLp);
        
        content.addView(engineCard, getCardParams());

        // --- LAYER 2.5: 🌐 GEOFENCING SENTINEL & PERIMETER SETTINGS ---
        content.addView(createSectionHeader("GEOFENCING SENTINEL & PERIMETER SETTINGS"));
        LinearLayout geoCard = new LinearLayout(this);
        geoCard.setOrientation(LinearLayout.VERTICAL);

        long currentJitterMs = RegistryManager.getGeofenceJitterDebounceMs(this);
        String jitterLabel = "⏱️ PERIMETER JITTER FILTER (" + (currentJitterMs / 1000) + "S DEBOUNCE)";
        geoCard.addView(createIconButton(jitterLabel, android.R.drawable.ic_menu_recent_history, v -> showGeofenceJitterDialog()), btnLp);

        boolean latchEnabled = RegistryManager.isGeofenceLatchEnabled(this);
        View latchSwitch = UiArchitect.createSwitch(this, "Dormancy State Latch (Anti-Duplicate)", latchEnabled, (v, isChecked) -> {
            RegistryManager.setGeofenceLatchEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Perimeter Latch", isChecked ? "Dormant until opposite transition occurs." : "All perimeter events will trigger.", false, 2500, null, null);
        });
        geoCard.addView(latchSwitch, btnLp);

        boolean snapEnabled = RegistryManager.isGeofenceSnapCenterEnabled(this);
        View snapSwitch = UiArchitect.createSwitch(this, "Snap GPS to Safe Zone Centroid on Entry", snapEnabled, (v, isChecked) -> {
            RegistryManager.setGeofenceSnapCenterEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Centroid Snapping", isChecked ? "GPS snaps to centroid inside perimeter." : "Raw GNSS coordinates preserved.", false, 2500, null, null);
        });
        geoCard.addView(snapSwitch, btnLp);

        boolean localNotifEnabled = RegistryManager.isGeofenceLocalNotifEnabled(this);
        View localNotifSwitch = UiArchitect.createSwitch(this, "Local Device Geofence Sound & Banner", localNotifEnabled, (v, isChecked) -> {
            RegistryManager.setGeofenceLocalNotifEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Local Notifications", isChecked ? "OS alert pops when entering/exiting zones." : "Local alerts muted on this hardware.", false, 2500, null, null);
        });
        geoCard.addView(localNotifSwitch, btnLp);

        boolean fcmEnabled = RegistryManager.isGeofenceFcmRelayEnabled(this);
        View fcmSwitch = UiArchitect.createSwitch(this, "Cloud FCM Guardian Push Broadcast", fcmEnabled, (v, isChecked) -> {
            RegistryManager.setGeofenceFcmRelayEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Cloud Push Relay", isChecked ? "Events broadcasted to remote guardian devices." : "Cloud push relay suspended.", false, 2500, null, null);
        });
        geoCard.addView(fcmSwitch, btnLp);

        content.addView(geoCard, getCardParams());

        // --- LAYER 2.6: 📍 ADAPTIVE GPS & LOCATION DISPLACEMENT ---
        content.addView(createSectionHeader("ADAPTIVE GPS & LOCATION DISPLACEMENT"));
        LinearLayout gpsCard = new LinearLayout(this);
        gpsCard.setOrientation(LinearLayout.VERTICAL);

        float currentDisp = RegistryManager.getMinDisplacementMeters(this);
        String dispLabel = "📍 MINIMUM DISPLACEMENT FILTER: " + (int) currentDisp + "M";
        gpsCard.addView(createIconButton(dispLabel, android.R.drawable.ic_menu_compass, v -> showDisplacementDialog()), btnLp);

        boolean strictWait = RegistryManager.isStrictAccuracyWait(this);
        View strictWaitSwitch = UiArchitect.createSwitch(this, "Wait for High-Accuracy GNSS Fix (< 10m)", strictWait, (v, isChecked) -> {
            RegistryManager.setStrictAccuracyWait(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "GNSS Accuracy", isChecked ? "Waits for high-accuracy fix before publish." : "Fast fix mode enabled (lower latency).", false, 2500, null, null);
        });
        gpsCard.addView(strictWaitSwitch, btnLp);

        content.addView(gpsCard, getCardParams());

        // --- LAYER 2.7: 🚦 DEVICE RAG TELEMETRY & ALERT THRESHOLDS ---
        content.addView(createSectionHeader("DEVICE RAG TELEMETRY & ALERT THRESHOLDS"));
        LinearLayout ragCard = new LinearLayout(this);
        ragCard.setOrientation(LinearLayout.VERTICAL);

        int lowBat = RegistryManager.getLowBatteryThreshold(this);
        String batLabel = "🟡 LOW BATTERY ALERT THRESHOLD: " + lowBat + "%";
        ragCard.addView(createIconButton(batLabel, com.jdroidx.jfindme.R.drawable.ic_device_health, v -> showLowBatteryDialog()), btnLp);

        int staleMins = RegistryManager.getStaleInactiveMinutes(this);
        String staleLabel = "🟡 INACTIVE STALE THRESHOLD: " + staleMins + " MINUTES";
        ragCard.addView(createIconButton(staleLabel, android.R.drawable.ic_menu_recent_history, v -> showStaleInactiveDialog()), btnLp);

        int critHours = RegistryManager.getCriticalOfflineHours(this);
        String critLabel = "🔴 CRITICAL OFFLINE THRESHOLD: " + critHours + " HOURS";
        ragCard.addView(createIconButton(critLabel, android.R.drawable.ic_dialog_alert, v -> showCriticalOfflineDialog()), btnLp);

        content.addView(ragCard, getCardParams());

        // --- LAYER 2.8: 🔊 EMERGENCY ACOUSTIC BEACON & SIREN ---
        content.addView(createSectionHeader("EMERGENCY ACOUSTIC BEACON & SIREN"));
        LinearLayout sirenCard = new LinearLayout(this);
        sirenCard.setOrientation(LinearLayout.VERTICAL);

        int sirenSec = RegistryManager.getSirenDurationSec(this);
        String sirenLabel = "🔊 SIREN AUTO-SILENCE DURATION: " + sirenSec + "S";
        sirenCard.addView(createIconButton(sirenLabel, android.R.drawable.ic_lock_idle_alarm, v -> showSirenDurationDialog()), btnLp);

        boolean vibeEnabled = RegistryManager.isSirenVibrationEnabled(this);
        View vibeSwitch = UiArchitect.createSwitch(this, "Continuous Haptic Vibration with Siren", vibeEnabled, (v, isChecked) -> {
            RegistryManager.setSirenVibrationEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Emergency Vibration", isChecked ? "Haptic motor pulses during acoustic siren." : "Vibration muted during siren.", false, 2500, null, null);
        });
        sirenCard.addView(vibeSwitch, btnLp);

        boolean dndEnabled = RegistryManager.isSirenDndBypassEnabled(this);
        View dndSwitch = UiArchitect.createSwitch(this, "Do-Not-Disturb (DND) Volume Override", dndEnabled, (v, isChecked) -> {
            RegistryManager.setSirenDndBypassEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "DND Override", isChecked ? "Siren plays at 100% volume even in Silent mode." : "Respects device volume and silent profile.", false, 2500, null, null);
        });
        sirenCard.addView(dndSwitch, btnLp);

        content.addView(sirenCard, getCardParams());

        // --- LAYER 2.9: 📦 OFFLINE VAULT & RESILIENCE QUEUE ---
        content.addView(createSectionHeader("OFFLINE VAULT & RESILIENCE QUEUE"));
        LinearLayout vaultCard = new LinearLayout(this);
        vaultCard.setOrientation(LinearLayout.VERTICAL);

        int maxQueue = RegistryManager.getOfflineQueueMaxPoints(this);
        String queueLabel = "📦 OFFLINE GPS QUEUE CAPACITY: " + maxQueue + " POINTS";
        vaultCard.addView(createIconButton(queueLabel, com.jdroidx.jfindme.R.drawable.ic_location_history, v -> showOfflineQueueCapacityDialog()), btnLp);

        boolean wifiOnly = RegistryManager.isSyncWifiOnly(this);
        View wifiSwitch = UiArchitect.createSwitch(this, "Deferred Offline Sync on Wi-Fi Only", wifiOnly, (v, isChecked) -> {
            RegistryManager.setSyncWifiOnly(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Network Sync Policy", isChecked ? "Queued telemetry flushes only over unmetered Wi-Fi." : "Flushes over Cellular & Wi-Fi automatically.", false, 2500, null, null);
        });
        vaultCard.addView(wifiSwitch, btnLp);

        content.addView(vaultCard, getCardParams());

        // --- LAYER 3: SECURITY & ACCESS CONTROLS ---
        content.addView(createSectionHeader("SECURITY & ACCESS CONTROLS"));
        LinearLayout secCard = new LinearLayout(this);
        secCard.setOrientation(LinearLayout.VERTICAL);
        
        secCard.addView(createIconButton("PUPIL PROFILE ARCHITECTURE", com.jdroidx.jfindme.R.drawable.ic_user_profile, v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("NOTIFICATION ROUTING", com.jdroidx.jfindme.R.drawable.ic_nav_notice, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.NotifActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("IDENTITY SECURITY PROTOCOL", com.jdroidx.jfindme.R.drawable.ic_privacy_lock, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.PrivacyActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("PAIRING PIN EXPIRY TTL", com.jdroidx.jfindme.R.drawable.ic_location_history, v -> {
            showPinTtlSelectionDialog();
        }), btnLp);
        secCard.addView(createIconButton("MASTER PURGE SAFETY PHRASE", com.jdroidx.jfindme.R.drawable.ic_privacy_lock, v -> {
            showPurgeSafetyPhraseDialog();
        }), btnLp);
        secCard.addView(createIconButton("SUPPORT DOCUMENTS", com.jdroidx.jfindme.R.drawable.ic_help_support, v -> {
            startActivity(new Intent(this, HelpSupportActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("DEVICE HEALTH DIAGNOSTICS", com.jdroidx.jfindme.R.drawable.ic_device_health, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.HealthActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("SYSTEM AUDIT LOG MATRIX", com.jdroidx.jfindme.R.drawable.ic_system_audit, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.SystemLogActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("FORENSIC WATCHDOG AUDIT", com.jdroidx.jfindme.R.drawable.ic_watchdog, v -> {
            android.content.Intent watchdog = new android.content.Intent(this, com.jdroidx.jfindme.ui.management.WatchDogAuditActivity.class);
            watchdog.putExtra("VIEW_MODE", "STATE");
            startActivity(watchdog);
        }), btnLp);
        secCard.addView(createIconButton("FACTORY RESET CONSOLE", com.jdroidx.jfindme.R.drawable.ic_system_audit, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.FactoryResetActivity.class));
        }), btnLp);
        
        content.addView(secCard, getCardParams());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return root;
    }

    private LinearLayout.LayoutParams getCardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int cMargin = getResponsivePaddingQuota() + 10;
        lp.setMargins(0, 0, 0, cMargin);
        return lp;
    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Seamless UI integration
        header.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_HEADER"));
        header.setTypeface(null, Typeface.BOLD);
        int hPad = getResponsivePaddingQuota();
        header.setPadding(hPad / 2, hPad * 2, 0, hPad);
        return header;
    }


    @Override
    protected void setupLogic() {
        SystemLogger.d(TAG, "Settings Hub Synchronized.");
    }

    private void showBatteryOptimizerDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());
        int p = scale(40);
        root.setPadding(p, p, p, p);
        
        // --- 🔋 HEADER ---
        TextView title = new TextView(this);
        title.setText("🔋 ENERGY & PING OPTIMIZER");
        title.setTextColor(getGradientStart());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        title.setPadding(0, 0, 0, 40);
        root.addView(title);

        // --- STEPPER SECTION ---
        LinearLayout stepperCard = new LinearLayout(this);
        stepperCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, stepperCard, "CARD_STANDARD");
        stepperCard.setPadding(30, 30, 30, 30);

        android.widget.Switch masterSwitch = new android.widget.Switch(this);
        masterSwitch.setText("🔋 MANUAL OVERRIDE (90s+)   ");
        masterSwitch.setTextColor(getTextColor());
        masterSwitch.setChecked(prefs.getBoolean("pref_stepper_active", false));
        stepperCard.addView(masterSwitch);

        final LinearLayout stepperControls = new LinearLayout(this);
        stepperControls.setGravity(Gravity.CENTER);
        stepperControls.setAlpha(masterSwitch.isChecked() ? 1.0f : 0.4f);
        
        TextView valTv = new TextView(this);
        long currentVal = prefs.getLong("pref_manual_pulse", 90000L) / 1000;
        valTv.setText(currentVal + "s");
        valTv.setTextColor(getTextColor());
        valTv.setTextSize(24f);
        valTv.setTypeface(null, Typeface.BOLD);
        valTv.setPadding(40, 0, 40, 0);

        // --- ➕ INCREMENT / ➖ DECREMENT VECTOR NODES ---
        int tacticalSize = scale(110);
        
        android.widget.ImageView decBtn = new android.widget.ImageView(this);
        decBtn.setImageResource(com.jdroidx.jfindme.R.drawable.ic_minus); // Forensic Minus Vector
        decBtn.setColorFilter(getGradientOverlayText());
        decBtn.setBackground(getSolidDrawable(getGradientStart(), scaleRadius(15)));
        decBtn.setPadding(30, 30, 30, 30);
        decBtn.setOnClickListener(v -> {
            if (!masterSwitch.isChecked()) return;
            long val = Long.parseLong(valTv.getText().toString().replace("s", ""));
            if (val > 90) { 
                val -= 10; valTv.setText(val + "s"); 
                prefs.edit().putLong("pref_manual_pulse", val * 1000).apply();
                prefs.edit().putLong("pref_tracking_interval", val * 1000).apply();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            }
        });

        android.widget.ImageView incBtn = new android.widget.ImageView(this);
        incBtn.setImageResource(android.R.drawable.ic_input_add); // Native Android Plus Vector
        incBtn.setColorFilter(getGradientOverlayText());
        incBtn.setBackground(getSolidDrawable(getGradientStart(), scaleRadius(15)));
        incBtn.setPadding(30, 30, 30, 30);
        incBtn.setOnClickListener(v -> {
            if (!masterSwitch.isChecked()) return;
            long val = Long.parseLong(valTv.getText().toString().replace("s", ""));
            val += 10; valTv.setText(val + "s"); 
            prefs.edit().putLong("pref_manual_pulse", val * 1000).apply();
            prefs.edit().putLong("pref_tracking_interval", val * 1000).apply();
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        });

        stepperControls.addView(decBtn, new LinearLayout.LayoutParams(tacticalSize, tacticalSize));
        stepperControls.addView(valTv);
        stepperControls.addView(incBtn, new LinearLayout.LayoutParams(tacticalSize, tacticalSize));
        stepperCard.addView(stepperControls);
        root.addView(stepperCard);

        // --- PRESET SECTION ---
        TextView presetTitle = new TextView(this);
        presetTitle.setText("🔋 PRESET OPTIMIZATION NODES");
        presetTitle.setTextColor(getSecondaryTextColor());
        presetTitle.setPadding(0, 40, 0, 20);
        root.addView(presetTitle);

        LinearLayout presetCard = new LinearLayout(this);
        presetCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, presetCard, "CARD_STANDARD");
        
        final long[] vals = {60000L, 30000L, 15000L, 7000L, 1000L};
        final String[] names = {"ECO (60s)", "BALANCED (30s)", "ACTIVE (15s)", "HIGH (7s)", "EXREME (1s)"};
        final java.util.List<android.widget.Switch> switches = new java.util.ArrayList<>();

        for(int i=0; i<vals.length; i++){
            final long v = vals[i];
            android.widget.Switch sw = new android.widget.Switch(this);
            sw.setText(names[i]);
            sw.setTextColor(getTextColor());
            sw.setPadding(20, 20, 20, 20);
            sw.setChecked(!masterSwitch.isChecked() && prefs.getLong("pref_tracking_interval", 60000L) == v);
            switches.add(sw);
            sw.setOnClickListener(view -> {
                if(masterSwitch.isChecked()) { sw.setChecked(false); return; }
                for(android.widget.Switch s : switches) s.setChecked(false);
                sw.setChecked(true);
                prefs.edit().putLong("pref_tracking_interval", v).apply();
            });
            presetCard.addView(sw);
        }
        root.addView(presetCard);

        masterSwitch.setOnCheckedChangeListener((b, isChecked) -> {
            prefs.edit().putBoolean("pref_stepper_active", isChecked).apply();
            stepperControls.setAlpha(isChecked ? 1.0f : 0.3f);
            if(isChecked) {
                for(android.widget.Switch s : switches) s.setChecked(false);
                long val = Long.parseLong(valTv.getText().toString().replace("s", "")) * 1000;
                prefs.edit().putLong("pref_tracking_interval", val).apply();
            }
        });

        dialog.setContentView(root);
        dialog.show();
        
        android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(lp);
    }

    private void showPinTtlSelectionDialog() {
        String[] options = {
            "5 Minutes (Recommended - NIST SP 800-63B)",
            "10 Minutes",
            "15 Minutes",
            "20 Minutes",
            "30 Minutes (Extended Pairing Window)"
        };
        int[] values = {5, 10, 15, 20, 30};
        int currentTtl = prefs.getInt("pairing_pin_ttl_minutes", 5);
        int selectedIdx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentTtl) { selectedIdx = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Pairing PIN Expiry TTL")
            .setSingleChoiceItems(options, selectedIdx, (dialog, which) -> {
                int chosen = values[which];
                prefs.edit().putInt("pairing_pin_ttl_minutes", chosen).apply();
                android.widget.Toast.makeText(this, "Pairing PIN Expiry TTL set to " + chosen + " minutes.", android.widget.Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void delayedSafeRecreate() {
        // 🛡️ FIX-11: Guard against recreating activity while app is backgrounded
        if (!isInForeground) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::safeRecreate, 2600);
    }

    private void showGeofenceJitterDialog() {
        String[] options = {
            "0s (Disabled - Instant Trigger)",
            "5s (High Sensitivity - Min Range)",
            "15s (Balanced Standard - Mid Range / Recommended)",
            "30s (Extended Boundary Filter)",
            "60s (Highway & Rapid Travel - Max Range)"
        };
        long[] values = {0L, 5000L, 15000L, 30000L, 60000L};
        long currentMs = RegistryManager.getGeofenceJitterDebounceMs(this);
        int selectedIdx = 2; // Default: 15s
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentMs) { selectedIdx = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Perimeter Jitter Filter (Anti-Flutter)")
            .setSingleChoiceItems(options, selectedIdx, (dialog, which) -> {
                long chosenMs = values[which];
                RegistryManager.setGeofenceJitterDebounceMs(this, chosenMs);
                UiArchitect.showSlideNotificationBanner(this, "Jitter Filter Updated", "Debounce threshold set to " + (chosenMs / 1000) + "s", false, 2500, null, null);
                dialog.dismiss();
                delayedSafeRecreate();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showDisplacementDialog() {
        String[] options = {
            "0m (Continuous Telemetry - Max Sensitivity / Default)",
            "5m (Fine Pedestrian Tracking)",
            "10m (City Balanced Mode)",
            "25m (Vehicle / Battery Saver - Max Range)"
        };
        float[] values = {0f, 5f, 10f, 25f};
        float current = RegistryManager.getMinDisplacementMeters(this);
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📍 Minimum Displacement Threshold")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                float chosen = values[which];
                RegistryManager.setMinDisplacementMeters(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Displacement Updated", "Displacement threshold set to " + (int) chosen + "m", false, 2500, null, null);
                dialog.dismiss();
                delayedSafeRecreate();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showLowBatteryDialog() {
        String[] options = {
            "10% (Critical Level Only)",
            "15% (Medium Warning)",
            "20% (Standard Level - Recommended / Default)",
            "25% (Early Warning Level)"
        };
        int[] values = {10, 15, 20, 25};
        int current = RegistryManager.getLowBatteryThreshold(this);
        int selected = 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🟡 Low Battery Warning Level")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setLowBatteryThreshold(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Low Battery Threshold", "Warning trigger set to " + chosen + "%", false, 2500, null, null);
                dialog.dismiss();
                delayedSafeRecreate();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showStaleInactiveDialog() {
        String[] options = {
            "5 Minutes (Strict / High Alert)",
            "15 Minutes (Balanced Standard - Default)",
            "30 Minutes (Relaxed Mode)",
            "60 Minutes (Casual Mode)"
        };
        int[] values = {5, 15, 30, 60};
        int current = RegistryManager.getStaleInactiveMinutes(this);
        int selected = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🟡 Inactive Stale Threshold (Amber)")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setStaleInactiveMinutes(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Stale Threshold Updated", "Amber warning set to " + chosen + " minutes", false, 2500, null, null);
                dialog.dismiss();
                delayedSafeRecreate();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCriticalOfflineDialog() {
        String[] options = {
            "2 Hours (Strict Fleet)",
            "6 Hours (Balanced - Recommended / Default)",
            "12 Hours (Half-Day Extended)",
            "24 Hours (Full Day Extended)"
        };
        int[] values = {2, 6, 12, 24};
        int current = RegistryManager.getCriticalOfflineHours(this);
        int selected = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🔴 Critical Offline Threshold (Red)")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setCriticalOfflineHours(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Offline Threshold Updated", "Red alert trigger set to " + chosen + " hours", false, 2500, null, null);
                dialog.dismiss();
                delayedSafeRecreate();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showSirenDurationDialog() {
        String[] options = {
            "15 Seconds (Brief Alert)",
            "30 Seconds (Standard - Recommended / Default)",
            "60 Seconds (Extended Siren)",
            "120 Seconds (Maximum Distress)"
        };
        int[] values = {15, 30, 60, 120};
        int current = RegistryManager.getSirenDurationSec(this);
        int selected = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🔊 Siren Auto-Silence Duration")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setSirenDurationSec(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Siren Duration Updated", "Acoustic alarm set to " + chosen + " seconds", false, 2500, null, null);
                dialog.dismiss();
                delayedSafeRecreate();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showOfflineQueueCapacityDialog() {
        String[] options = {
            "100 Points (~1 Hour @ 30s interval)",
            "250 Points (~2-3 Hours @ 30s interval)",
            "500 Points (Standard - Recommended / Default)",
            "1000 Points (Forensic Extended Queue)"
        };
        int[] values = {100, 250, 500, 1000};
        int current = RegistryManager.getOfflineQueueMaxPoints(this);
        int selected = 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📦 Offline GPS Vault Capacity")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setOfflineQueueMaxPoints(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Vault Capacity Updated", "Max offline buffer set to " + chosen + " points", false, 2500, null, null);
                dialog.dismiss();
                delayedSafeRecreate();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showPurgeSafetyPhraseDialog() {
        String currentPhrase = RegistryManager.getPurgeSafetyPhrase(this);
        UiArchitect.showInputPromptDialog(this,
            "Master Purge Safety Phrase",
            "Configure administrative override password for force purging unlinked nodes (Current: " + currentPhrase + "):",
            currentPhrase,
            newPhrase -> {
                if (newPhrase != null && newPhrase.trim().length() >= 4) {
                    RegistryManager.setPurgeSafetyPhrase(this, newPhrase.trim());
                    UiArchitect.showSlideNotificationBanner(this, "Safety Phrase Updated", "Master purge override password saved successfully.", false, 3000, null, null);
                } else {
                    UiArchitect.showSlideNotificationBanner(this, "Invalid Phrase", "Safety phrase must contain at least 4 characters.", true, 3000, null, null);
                }
            }
        );
    }

    private void showMotionDutyCyclesDialog() {
        String[] options = {
            "⚡ Ultra Aggressive (Still: 60s, Vehicle: 5s, Walk: 10s)",
            "🚗 Balanced Road Standard (Still: 300s, Vehicle: 10s, Walk: 20s - Recommended)",
            "🔋 Maximum Battery Eco (Still: 600s, Vehicle: 15s, Walk: 30s)",
            "🛡️ Custom Dynamic Setup (Still: 180s, Vehicle: 10s, Walk: 15s)"
        };
        long[][] values = {
            {60000L, 5000L, 10000L},
            {300000L, 10000L, 20000L},
            {600000L, 15000L, 30000L},
            {180000L, 10000L, 15000L}
        };

        long currStill = RegistryManager.getMotionStillIntervalMs(this);
        int selected = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i][0] == currStill) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🏃 Motion & Battery Duty Cycles")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                long[] chosen = values[which];
                RegistryManager.setMotionStillIntervalMs(this, chosen[0]);
                RegistryManager.setMotionVehicleIntervalMs(this, chosen[1]);
                RegistryManager.setMotionWalkingIntervalMs(this, chosen[2]);
                UiArchitect.showSlideNotificationBanner(this, "Duty Cycles Saved", "Still: " + (chosen[0]/1000) + "s, Car: " + (chosen[1]/1000) + "s, Walk: " + (chosen[2]/1000) + "s", false, 3000, null, null);
                dialog.dismiss();
                delayedSafeRecreate();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showRoadSmoothingSensitivityDialog() {
        String[] options = {
            "🛣️ High Responsiveness (EMA: 0.50, Noise: 15.0 - City Corners)",
            "⚖️ Balanced Road Alignment (EMA: 0.35, Noise: 25.0 - Default)",
            "🌊 Ultra Smooth Glide (EMA: 0.20, Noise: 40.0 - Highway/Zenly Standard)",
            "🛡️ High Jitter Rejection (EMA: 0.15, Noise: 60.0 - Dense Skyscrapers)"
        };
        float[][] values = {
            {0.50f, 15.0f},
            {0.35f, 25.0f},
            {0.20f, 40.0f},
            {0.15f, 60.0f}
        };

        float currEma = RegistryManager.getHeadingEmaFactor(this);
        int selected = 1;
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i][0] - currEma) < 0.05f) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🛣️ Road Smoothing & Kalman Sensitivity")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                float[] chosen = values[which];
                RegistryManager.setHeadingEmaFactor(this, chosen[0]);
                RegistryManager.setKalmanMeasurementNoise(this, chosen[1]);
                UiArchitect.showSlideNotificationBanner(this, "Sensitivity Updated", "Heading EMA: " + chosen[0] + ", Noise R: " + chosen[1], false, 3000, null, null);
                dialog.dismiss();
                delayedSafeRecreate();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }
}
