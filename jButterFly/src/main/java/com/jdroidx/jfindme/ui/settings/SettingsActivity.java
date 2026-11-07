package com.jdroidx.jfindme.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jdroidx.jfindme.BuildConfig;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.ui.auth.AuthActivity;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.map.MapActivity;
import com.jdroidx.jfindme.ui.map.LiveMapDashboardActivity;
import com.jdroidx.jfindme.ui.group.NetworkGatewayActivity;
import com.jdroidx.jfindme.ui.group.FamilyFriendsActivity;
import com.jdroidx.jfindme.ui.group.GroupActivity;
import com.jdroidx.jfindme.ui.group.PersonDetailsActivity;
import com.jdroidx.jfindme.ui.management.PrivacyActivity;
import com.jdroidx.jfindme.ui.management.HealthActivity;
import com.jdroidx.jfindme.ui.management.FactoryResetActivity;
import com.jdroidx.jfindme.ui.management.NotifActivity;
import com.jdroidx.jfindme.ui.management.GeofenceActivity;
import com.jdroidx.jfindme.ui.management.GeofenceNotificationActivity;
import com.jdroidx.jfindme.ui.management.HistoryActivity;
import com.jdroidx.jfindme.ui.management.ForensicVaultExplorerActivity;
import com.jdroidx.jfindme.ui.management.WatchDogAuditActivity;
import com.jdroidx.jfindme.ui.management.DatabaseManagementActivity;
import com.jdroidx.jfindme.ui.management.FirebaseCloudMatrixActivity;
import com.jdroidx.jfindme.ui.management.SystemLogActivity;
import com.jdroidx.jfindme.ui.alert.AcousticSirenActivity;
import com.jdroidx.jfindme.ui.base.ThemeManager;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;
import com.jdroidx.jfindme.utils.RegistryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 🛠️ SettingsActivity: Master Control Hub & Dual-Tab Architectural Console
 *
 * RESTORATION & DUAL-TAB REDESIGN (Enterprise OOPS MVC / MVVM Closed-Loop Standard):
 * - Tab 1: UNIVERSAL SETTINGS (Cross-screen SSOT global configurations, high-to-low priority encapsulation, parent/child reactive bindings)
 * - Tab 2: SCREEN MODULES (Screen-wise direct launch cards, live dynamic badges, interactive instant search filter)
 *
 * Human-in-the-Loop Resilience:
 * 1. Zero screen flickers: Live value mutation updates card subtitles in-place without jarring delayedSafeRecreate().
 * 2. Active tab persistence: User remains on their chosen tab across configuration changes and device rotations.
 * 3. Searchable screen matrix: Instant multi-keyword search across all 28 screen modules.
 * 4. Asynchronous live telemetry badges: Dynamic device counts, safe zone counts, battery % and version tags.
 * 5. Crash-proof activity resolution: Safe-launch boundary with user-facing diagnostic alert.
 */
public class SettingsActivity extends BaseActivityJava {

    private final String TAG = "SettingsActivity";
    private static final String PREF_SETTINGS_ACTIVE_TAB = "settings_active_tab";

    // Tab Switcher Elements
    private LinearLayout tabUniversalBtn;
    private LinearLayout tabScreensBtn;
    private TextView tvTabUniversal;
    private TextView tvTabScreens;
    private ScrollView scrollUniversal;
    private ScrollView scrollScreens;
    private int activeTab = 0; // 0: Universal Settings, 1: Screen Modules

    // In-Place Dynamic Subtitle Mutators (Tab 1 Universal Settings)
    private TextView tvPingIntervalVal;
    private TextView tvStealthCadenceVal;
    private TextView tvMotionDutyVal;
    private TextView tvDisplacementVal;
    private TextView tvRoadSmoothingVal;
    private TextView tvJitterVal;
    private TextView tvRadiusVal;
    private TextView tvLowBatteryVal;
    private TextView tvStaleVal;
    private TextView tvCriticalOfflineVal;
    private TextView tvDistantVal;
    private TextView tvRadarRadiusVal;
    private TextView tvRemoteLocateTimeoutVal;
    private TextView tvSirenDurationVal;
    private TextView tvSosCountdownVal;
    private TextView tvSosCooldownVal;
    private TextView tvSosAckVal;
    private TextView tvSosFanoutVal;
    private TextView tvOfflineQueueVal;
    private TextView tvGuardianPinVal;
    private TextView tvPauseDurationVal;
    private TextView tvPinTtlVal;
    private TextView tvPurgePhraseVal;
    private TextView tvClosedLoopTimeoutVal;
    private TextView tvMaxAckFailuresVal;
    private TextView tvFailoverSmsVal;

    // Live Badges (Tab 2 Screen Modules)
    private TextView badgeRosterTv;
    private TextView badgeZonesTv;
    private TextView badgeHistoryTv;
    private TextView badgeHealthTv;
    private TextView badgeProfileTv;

    // Search Matrix for Screen Cards
    private static class ScreenCardHolder {
        final View cardView;
        final String searchTag;
        ScreenCardHolder(View cardView, String searchTag) {
            this.cardView = cardView;
            this.searchTag = searchTag.toLowerCase(Locale.US);
        }
    }
    private final List<ScreenCardHolder> screenCardHolders = new ArrayList<>();

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // ─── 1. MASTER HEADER MATRIX ──────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView backBtn = new ImageView(this);
        backBtn.setImageResource(android.R.drawable.ic_menu_revert);
        backBtn.setColorFilter(getGradientOverlayText());
        int bSize = scale(60);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(bSize, bSize);
        bLp.setMargins(0, 0, scale(20), 0);
        backBtn.setLayoutParams(bLp);
        backBtn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        titleRow.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("CONTROL HUB");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_HEADER"));
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        titleRow.addView(title);

        // Master Reset Defaults Button
        ImageView resetDefaultsBtn = new ImageView(this);
        resetDefaultsBtn.setImageResource(android.R.drawable.ic_menu_rotate);
        resetDefaultsBtn.setColorFilter(getGradientOverlayText());
        int rPad = scale(15);
        resetDefaultsBtn.setPadding(rPad, rPad, rPad, rPad);
        resetDefaultsBtn.setContentDescription("Restore Recommended Defaults");
        resetDefaultsBtn.setOnClickListener(v -> promptRestoreDefaults());
        titleRow.addView(resetDefaultsBtn, new LinearLayout.LayoutParams(scale(80), scale(80)));

        // Master Theme Switcher
        ImageView themeToggle = new ImageView(this);
        themeToggle.setImageResource(getResolvedThemeId() == 1 ? android.R.drawable.ic_menu_day : android.R.drawable.ic_menu_recent_history);
        themeToggle.setColorFilter(getGradientOverlayText());
        int tPad = scale(15);
        themeToggle.setPadding(tPad, tPad, tPad, tPad);
        themeToggle.setContentDescription("Toggle Theme");
        themeToggle.setOnClickListener(v -> {
            int current = prefs.getInt("theme_id", 0);
            int totalThemes = ThemeManager.THEMES.length;
            RegistryManager.setThemeId(this, (current + 1) % totalThemes);
            safeRecreate();
        });
        titleRow.addView(themeToggle, new LinearLayout.LayoutParams(scale(80), scale(80)));

        header.addView(titleRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Enterprise Engine Architecture & Configuration Matrix");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.85f);
        subtitle.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_SUB"));
        header.addView(subtitle);

        root.addView(header);

        // ─── 2. DUAL-TAB SEGMENTED CONTROLLER ─────────────────────────────────
        root.addView(buildSegmentedTabBar());

        // ─── 3. TAB 1: UNIVERSAL SETTINGS CONTAINER ───────────────────────────
        scrollUniversal = new ScrollView(this);
        scrollUniversal.setVerticalScrollBarEnabled(false);
        scrollUniversal.addView(buildUniversalSettingsContent());

        // ─── 4. TAB 2: SCREEN MODULES CONTAINER ───────────────────────────────
        scrollScreens = new ScrollView(this);
        scrollScreens.setVerticalScrollBarEnabled(false);
        scrollScreens.addView(buildScreensNavigationContent());

        // Attach scroll containers to root
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        root.addView(scrollUniversal, scrollLp);
        root.addView(scrollScreens, scrollLp);

        // Restore previously selected tab or intent override (supports ACTIVE_TAB, EXTRA_ACTIVE_TAB, ACTIVE_TAB_INDEX)
        int savedTab = prefs.getInt(PREF_SETTINGS_ACTIVE_TAB, 0);
        String startTab = null;
        if (getIntent() != null) {
            startTab = getIntent().getStringExtra("ACTIVE_TAB");
            if (startTab == null) startTab = getIntent().getStringExtra("EXTRA_ACTIVE_TAB");
            if (startTab == null) {
                int idx = getIntent().getIntExtra("ACTIVE_TAB_INDEX", -1);
                if (idx == 1) startTab = "SCREENS";
                else if (idx == 0) startTab = "UNIVERSAL";
            }
        }
        if ("SCREENS".equalsIgnoreCase(startTab)) {
            savedTab = 1;
        } else if ("UNIVERSAL".equalsIgnoreCase(startTab)) {
            savedTab = 0;
        }
        switchTab(savedTab);

        // Asynchronously load real-time database badges for Screen Modules
        loadLiveMetricsForScreens();

        return root;
    }

    /**
     * Builds the interactive 2-Tab Segmented Selector.
     */
    private View buildSegmentedTabBar() {
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setGravity(Gravity.CENTER_VERTICAL);
        int pad = getResponsivePaddingQuota();
        tabRow.setPadding(pad * 2, pad, pad * 2, pad);

        LinearLayout tabHolder = new LinearLayout(this);
        tabHolder.setOrientation(LinearLayout.HORIZONTAL);
        tabHolder.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scale(90)));
        tabHolder.setBackground(getSolidDrawable(getCardColor(), scaleRadius(45)));
        tabHolder.setPadding(scale(8), scale(8), scale(8), scale(8));

        // Tab 1: Universal Settings
        tabUniversalBtn = new LinearLayout(this);
        tabUniversalBtn.setOrientation(LinearLayout.HORIZONTAL);
        tabUniversalBtn.setGravity(Gravity.CENTER);
        tabUniversalBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        tabUniversalBtn.setClickable(true);
        tabUniversalBtn.setFocusable(true);
        tabUniversalBtn.setOnClickListener(v -> switchTab(0));

        tvTabUniversal = new TextView(this);
        tvTabUniversal.setText("🌐 UNIVERSAL SETTINGS");
        tvTabUniversal.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_LABEL"));
        tvTabUniversal.setGravity(Gravity.CENTER);
        tabUniversalBtn.addView(tvTabUniversal);

        // Tab 2: Screen Modules
        tabScreensBtn = new LinearLayout(this);
        tabScreensBtn.setOrientation(LinearLayout.HORIZONTAL);
        tabScreensBtn.setGravity(Gravity.CENTER);
        tabScreensBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        tabScreensBtn.setClickable(true);
        tabScreensBtn.setFocusable(true);
        tabScreensBtn.setOnClickListener(v -> switchTab(1));

        tvTabScreens = new TextView(this);
        tvTabScreens.setText("📱 SCREEN MODULES");
        tvTabScreens.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_LABEL"));
        tvTabScreens.setGravity(Gravity.CENTER);
        tabScreensBtn.addView(tvTabScreens);

        tabHolder.addView(tabUniversalBtn);
        tabHolder.addView(tabScreensBtn);
        tabRow.addView(tabHolder);

        return tabRow;
    }

    /**
     * Switches between Universal Settings (Tab 0) and Screen Modules (Tab 1) with state persistence.
     */
    private void switchTab(int index) {
        activeTab = index;
        prefs.edit().putInt(PREF_SETTINGS_ACTIVE_TAB, index).apply();

        if (index == 0) {
            scrollUniversal.setVisibility(View.VISIBLE);
            scrollScreens.setVisibility(View.GONE);

            tabUniversalBtn.setBackground(getSolidDrawable(getGradientStart(), scaleRadius(40)));
            tvTabUniversal.setTextColor(getGradientOverlayText());
            tvTabUniversal.setTypeface(null, Typeface.BOLD);

            tabScreensBtn.setBackgroundColor(Color.TRANSPARENT);
            tvTabScreens.setTextColor(getTextColor());
            tvTabScreens.setTypeface(null, Typeface.NORMAL);
        } else {
            scrollUniversal.setVisibility(View.GONE);
            scrollScreens.setVisibility(View.VISIBLE);

            tabScreensBtn.setBackground(getSolidDrawable(getGradientStart(), scaleRadius(40)));
            tvTabScreens.setTextColor(getGradientOverlayText());
            tvTabScreens.setTypeface(null, Typeface.BOLD);

            tabUniversalBtn.setBackgroundColor(Color.TRANSPARENT);
            tvTabUniversal.setTextColor(getTextColor());
            tvTabUniversal.setTypeface(null, Typeface.NORMAL);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 🌐 TAB 1: UNIVERSAL SETTINGS (High-to-Low Priority Context Encapsulation)
    // ──────────────────────────────────────────────────────────────────────────

    private View buildUniversalSettingsContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int setPad = getResponsivePaddingQuota();
        content.setPadding(setPad * 2, setPad / 2, setPad * 2, setPad * 4);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, scale(10), 0, scale(10));

        // ══════════════════════════════════════════════════════════════════════
        // CARD 1: 🛡️ CHILD PROTECTION & MASTER SECURITY (Priority 1)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🛡️ CHILD PROTECTION & MASTER SECURITY (PRIORITY 1)"));
        LinearLayout secCard = new LinearLayout(this);
        secCard.setOrientation(LinearLayout.VERTICAL);

        boolean isChildWard = RegistryManager.isChildWardMode(this);
        final boolean[] isProgrammaticChildSwitch = {false};
        final androidx.appcompat.widget.SwitchCompat[] childSwitchRef = new androidx.appcompat.widget.SwitchCompat[1];

        View childModeSwitch = UiArchitect.createSwitch(this, "Master Child / Ward Protection (Tamper-Proof)", isChildWard, (v, isChecked) -> {
            if (isProgrammaticChildSwitch[0]) return;
            if (!isChecked) {
                String storedPin = RegistryManager.getGuardianPin(this);
                if (storedPin != null && !storedPin.trim().isEmpty()) {
                    showPinPromptForChildMode(childSwitchRef[0], isProgrammaticChildSwitch, storedPin);
                    return;
                }
            }
            RegistryManager.setChildWardMode(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Child Safety Mode", isChecked ? "Child protection locked. Location shut-off restricted." : "Child mode deactivated.", false, 2500, null, null);
        });

        // Resolve direct SwitchCompat reference for zero-flicker in-place reversion
        if (childModeSwitch instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) childModeSwitch;
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (vg.getChildAt(i) instanceof androidx.appcompat.widget.SwitchCompat) {
                    childSwitchRef[0] = (androidx.appcompat.widget.SwitchCompat) vg.getChildAt(i);
                    break;
                }
            }
        }
        secCard.addView(childModeSwitch, btnLp);

        String currentPin = RegistryManager.getGuardianPin(this);
        String pinStatus = (currentPin != null && !currentPin.trim().isEmpty()) ? "●●●● (PIN Configured & Active)" : "Not Set (Click to Establish Master PIN)";
        secCard.addView(createSettingTile("GUARDIAN MASTER SECURITY PIN", pinStatus, com.jdroidx.jfindme.R.drawable.ic_privacy_lock, v -> showGuardianMasterPinDialog(), tv -> tvGuardianPinVal = tv), btnLp);

        int pauseMins = RegistryManager.getLocationPauseDurationMin(this);
        secCard.addView(createSettingTile("LOCATION PAUSE APPROVAL WINDOW", "Window: " + pauseMins + " Min (Standby pause duration before auto-resume)", android.R.drawable.ic_menu_recent_history, v -> showLocationPauseDurationDialog(), tv -> tvPauseDurationVal = tv), btnLp);

        int pinTtl = prefs.getInt("pairing_pin_ttl_minutes", 5);
        secCard.addView(createSettingTile("PAIRING PIN EXPIRY TTL", "TTL: " + pinTtl + " Min (NIST SP 800-63B pairing countdown)", com.jdroidx.jfindme.R.drawable.ic_location_history, v -> showPinTtlSelectionDialog(), tv -> tvPinTtlVal = tv), btnLp);

        String purgePhrase = RegistryManager.getPurgeSafetyPhrase(this);
        String displayPhrase = purgePhrase.startsWith("SHA256:") ? "●●●●●●●● (Encrypted)" : purgePhrase;
        secCard.addView(createSettingTile("MASTER PURGE SAFETY PASSWORD", "Passphrase: " + displayPhrase + " (Required for factory wipe)", com.jdroidx.jfindme.R.drawable.ic_privacy_lock, v -> showPurgeSafetyPhraseDialog(), tv -> tvPurgePhraseVal = tv), btnLp);

        content.addView(secCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 2: 🛰️ GPS SAMPLING CADENCE & BATTERY DEFENSE (Priority 2)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🔋 GPS CADENCE & BATTERY DEFENSE (PRIORITY 2)"));
        LinearLayout gpsCard = new LinearLayout(this);
        gpsCard.setOrientation(LinearLayout.VERTICAL);

        long currInterval = RegistryManager.getTrackingInterval(this) / 1000;
        String pingSub = "Active Cadence: " + currInterval + "s | Preset/Stepper Mode";
        gpsCard.addView(createSettingTile("BATTERY OPTIMIZER & PING HUB", pingSub, com.jdroidx.jfindme.R.drawable.ic_device_health, v -> showBatteryOptimizerDialog(), tv -> tvPingIntervalVal = tv), btnLp);

        // Hardware Motion Co-Processor (Parent Switch)
        boolean actRecogEnabled = RegistryManager.isActivityRecognitionEnabled(this);
        final View[] dutyCycleContainer = new View[1];
        View actRecogSwitch = UiArchitect.createSwitch(this, "Hardware Motion Co-Processor (Zero Still GPS)", actRecogEnabled, (v, isChecked) -> {
            RegistryManager.setActivityRecognitionEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Motion Sentinel", isChecked ? "Hardware co-processor active (GPS sleeps when still)." : "Continuous background tracking preference active.", false, 2500, null, null);
            if (dutyCycleContainer[0] != null) {
                dutyCycleContainer[0].setAlpha(isChecked ? 1.0f : 0.45f);
            }
        });
        gpsCard.addView(actRecogSwitch, btnLp);

        // Child Duty Cycles
        long stillInt = RegistryManager.getMotionStillIntervalMs(this) / 1000;
        long vehInt = RegistryManager.getMotionVehicleIntervalMs(this) / 1000;
        long walkInt = RegistryManager.getMotionWalkingIntervalMs(this) / 1000;
        String motionSub = "Still: " + stillInt + "s | Car: " + vehInt + "s | Walk: " + walkInt + "s";
        dutyCycleContainer[0] = createSettingTile("MOTION DUTY CYCLES CONFIGURATION", motionSub, com.jdroidx.jfindme.R.drawable.ic_settings_gears, v -> {
            if (!RegistryManager.isActivityRecognitionEnabled(this)) {
                UiArchitect.showSlideNotificationBanner(this, "Co-Processor Disabled", "Enable Hardware Motion Co-Processor above to engage duty cycles.", true, 2500, null, null);
            }
            showMotionDutyCyclesDialog();
        }, tv -> tvMotionDutyVal = tv);
        dutyCycleContainer[0].setAlpha(actRecogEnabled ? 1.0f : 0.45f);
        gpsCard.addView(dutyCycleContainer[0], btnLp);

        float currentDisp = RegistryManager.getMinDisplacementMeters(this);
        String dispSub = "Displacement: " + (int) currentDisp + "m (Suppresses stationary GPS jitter)";
        gpsCard.addView(createSettingTile("MINIMUM DISPLACEMENT FILTER", dispSub, android.R.drawable.ic_menu_compass, v -> showDisplacementDialog(), tv -> tvDisplacementVal = tv), btnLp);

        boolean strictWait = RegistryManager.isStrictAccuracyWait(this);
        View strictWaitSwitch = UiArchitect.createSwitch(this, "Wait for High-Accuracy GNSS Fix (< 10m)", strictWait, (v, isChecked) -> {
            RegistryManager.setStrictAccuracyWait(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "GNSS Accuracy", isChecked ? "Waits for high-accuracy fix before publish." : "Fast fix mode enabled (lower latency).", false, 2500, null, null);
        });
        gpsCard.addView(strictWaitSwitch, btnLp);

                // ⏰ STEALTH DORMANT CADENCE (APP CLOSED / BACKGROUND)
        long stealthMins = DatabaseHelper.getInstance(this).getStealthIntervalMins();
        String stealthSub = "Cadence: " + stealthMins + " mins | Autonomous SIM & Alarm Sentinel";
        gpsCard.addView(createSettingTile("STEALTH DORMANT CADENCE (APP CLOSED)", stealthSub, com.jdroidx.jfindme.R.drawable.ic_settings_gears, v -> showStealthCadenceDialog(), tv -> tvStealthCadenceVal = tv), btnLp);

        content.addView(gpsCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 3: 🛣️ ROAD ALIGNMENT & 2D KALMAN FILTERING (Priority 3)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🛣️ ROAD ALIGNMENT & MOTION SMOOTHING (PRIORITY 3)"));
        LinearLayout roadCard = new LinearLayout(this);
        roadCard.setOrientation(LinearLayout.VERTICAL);

        boolean roadSmoothingEnabled = RegistryManager.isRoadSmoothingEnabled(this);
        final View[] roadSensContainer = new View[1];
        View roadSmoothingSwitch = UiArchitect.createSwitch(this, "Road Alignment & 2D Kalman Filter", roadSmoothingEnabled, (v, isChecked) -> {
            RegistryManager.setRoadSmoothingEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Road Smoothing", isChecked ? "Kalman filtering and road-aligned azimuth active." : "Raw GNSS coordinates rendered without filter.", false, 2500, null, null);
            if (roadSensContainer[0] != null) {
                roadSensContainer[0].setAlpha(isChecked ? 1.0f : 0.45f);
            }
        });
        roadCard.addView(roadSmoothingSwitch, btnLp);

        float emaFactor = RegistryManager.getHeadingEmaFactor(this);
        float kalNoise = RegistryManager.getKalmanMeasurementNoise(this);
        String roadSub = "Heading EMA: " + emaFactor + " | Measurement Noise R: " + (int) kalNoise;
        roadSensContainer[0] = createSettingTile("KALMAN SENSITIVITY & HEADING EMA", roadSub, com.jdroidx.jfindme.R.drawable.ic_nav_map, v -> {
            if (!RegistryManager.isRoadSmoothingEnabled(this)) {
                UiArchitect.showSlideNotificationBanner(this, "Kalman Disabled", "Enable Road Alignment & 2D Kalman Filter above to apply sensitivity.", true, 2500, null, null);
            }
            showRoadSmoothingSensitivityDialog();
        }, tv -> tvRoadSmoothingVal = tv);
        roadSensContainer[0].setAlpha(roadSmoothingEnabled ? 1.0f : 0.45f);
        roadCard.addView(roadSensContainer[0], btnLp);

        content.addView(roadCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 4: 🌐 GEOFENCING SENTINEL & PERIMETER BOUNDARIES (Priority 4)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🌐 GEOFENCING SENTINEL & PERIMETER BOUNDARIES (PRIORITY 4)"));
        LinearLayout geoCard = new LinearLayout(this);
        geoCard.setOrientation(LinearLayout.VERTICAL);

        long currentJitterMs = RegistryManager.getGeofenceJitterDebounceMs(this);
        String jitterSub = "Debounce: " + (currentJitterMs / 1000) + "s (Temporal boundary anti-flutter)";
        geoCard.addView(createSettingTile("PERIMETER JITTER FILTER", jitterSub, android.R.drawable.ic_menu_recent_history, v -> showGeofenceJitterDialog(), tv -> tvJitterVal = tv), btnLp);

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

        int defaultRadius = RegistryManager.getDefaultSafeZoneRadiusMeters(this);
        String radiusSub = "Default Radius: " + defaultRadius + "m (Base safe zone perimeter)";
        geoCard.addView(createSettingTile("DEFAULT SAFE ZONE RADIUS", radiusSub, com.jdroidx.jfindme.R.drawable.ic_safe_zone, v -> showDefaultRadiusDialog(), tv -> tvRadiusVal = tv), btnLp);

        content.addView(geoCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 5: 🚦 RAG TELEMETRY & ALERT THRESHOLDS (Priority 5)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🚦 RAG TELEMETRY & ALERT THRESHOLDS (PRIORITY 5)"));
        LinearLayout ragCard = new LinearLayout(this);
        ragCard.setOrientation(LinearLayout.VERTICAL);

        int lowBat = RegistryManager.getLowBatteryThreshold(this);
        String batSub = "Threshold: " + lowBat + "% (Triggers amber distress telemetry)";
        ragCard.addView(createSettingTile("LOW BATTERY ALERT THRESHOLD", batSub, com.jdroidx.jfindme.R.drawable.ic_device_health, v -> showLowBatteryDialog(), tv -> tvLowBatteryVal = tv), btnLp);

        int staleMins = RegistryManager.getStaleInactiveMinutes(this);
        String staleSub = "Stale Window: " + staleMins + " Min (Marks node amber inactive)";
        ragCard.addView(createSettingTile("INACTIVE STALE THRESHOLD (AMBER)", staleSub, android.R.drawable.ic_menu_recent_history, v -> showStaleInactiveDialog(), tv -> tvStaleVal = tv), btnLp);

        int critHours = RegistryManager.getCriticalOfflineHours(this);
        String critSub = "Offline Window: " + critHours + " Hours (Marks node red offline)";
        ragCard.addView(createSettingTile("CRITICAL OFFLINE THRESHOLD (RED)", critSub, android.R.drawable.ic_dialog_alert, v -> showCriticalOfflineDialog(), tv -> tvCriticalOfflineVal = tv), btnLp);

        int distThreshold = RegistryManager.getDistantDeviceThresholdMeters(this);
        String distSub = "Panel Threshold: " + distThreshold + "m (Distance to float HUD panel)";
        ragCard.addView(createSettingTile("DISTANT DEVICE PANEL THRESHOLD", distSub, android.R.drawable.ic_menu_compass, v -> showDistantDeviceThresholdDialog(), tv -> tvDistantVal = tv), btnLp);

        int radarRadius = RegistryManager.getRadarProximityRadiusMeters(this);
        String radarSub = "Radar Proximity Radius: " + radarRadius + "m (Sky blue highlight threshold)";
        ragCard.addView(createSettingTile("RADAR PROXIMITY RADIUS", radarSub, android.R.drawable.ic_menu_compass, v -> showRadarProximityRadiusDialog(), tv -> tvRadarRadiusVal = tv), btnLp);

        long locateTimeout = RegistryManager.getRemoteLocateTimeoutMs(this) / 1000;
        String locateSub = "Remote Locate Timeout: " + locateTimeout + "s (Maximum GNSS response window)";
        ragCard.addView(createSettingTile("REMOTE LOCATE TIMEOUT", locateSub, android.R.drawable.ic_menu_mylocation, v -> showRemoteLocateTimeoutDialog(), tv -> tvRemoteLocateTimeoutVal = tv), btnLp);

        content.addView(ragCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 6: 🔊 EMERGENCY ACOUSTIC BEACON & SOS SENTINEL (Priority 6)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🚨 EMERGENCY ACOUSTIC BEACON & SOS SENTINEL (PRIORITY 6)"));
        LinearLayout sosCard = new LinearLayout(this);
        sosCard.setOrientation(LinearLayout.VERTICAL);

        int sirenSec = RegistryManager.getSirenDurationSec(this);
        String sirenSub = "Duration: " + sirenSec + "s (Acoustic horn auto-silence timer)";
        sosCard.addView(createSettingTile("SIREN AUTO-SILENCE DURATION", sirenSub, android.R.drawable.ic_lock_idle_alarm, v -> showSirenDurationDialog(), tv -> tvSirenDurationVal = tv), btnLp);

        boolean vibeEnabled = RegistryManager.isSirenVibrationEnabled(this);
        View vibeSwitch = UiArchitect.createSwitch(this, "Continuous Haptic Vibration with Siren", vibeEnabled, (v, isChecked) -> {
            RegistryManager.setSirenVibrationEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Emergency Vibration", isChecked ? "Haptic motor pulses during acoustic siren." : "Vibration muted during siren.", false, 2500, null, null);
        });
        sosCard.addView(vibeSwitch, btnLp);

        boolean dndEnabled = RegistryManager.isSirenDndBypassEnabled(this);
        View dndSwitch = UiArchitect.createSwitch(this, "Do-Not-Disturb (DND) Audio Override", dndEnabled, (v, isChecked) -> {
            RegistryManager.setSirenDndBypassEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "DND Override", isChecked ? "Siren plays at 100% volume even in Silent mode." : "Respects device volume and silent profile.", false, 2500, null, null);
        });
        sosCard.addView(dndSwitch, btnLp);

        boolean confirmRequired = RegistryManager.isSosConfirmationRequired(this);
        final View[] countdownContainer = new View[1];
        View confirmSwitch = UiArchitect.createSwitch(this, "Require Explicit Confirmation Before Broadcast", confirmRequired, (v, isChecked) -> {
            RegistryManager.setSosConfirmationRequired(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "SOS Confirmation", isChecked ? "User must confirm before emergency broadcast." : "Broadcast triggers immediately on activation.", false, 2500, null, null);
            if (countdownContainer[0] != null) {
                countdownContainer[0].setAlpha(isChecked ? 1.0f : 0.45f);
            }
        });
        sosCard.addView(confirmSwitch, btnLp);

        int countdownSec = RegistryManager.getSosCountdownSec(this);
        String countdownSub = "Countdown: " + countdownSec + "s (Emergency abort cancellation window)";
        countdownContainer[0] = createSettingTile("SOS CONFIRMATION COUNTDOWN", countdownSub, android.R.drawable.ic_menu_manage, v -> {
            if (!RegistryManager.isSosConfirmationRequired(this)) {
                UiArchitect.showSlideNotificationBanner(this, "Instant Mode Active", "Countdown timer bypassed when explicit confirmation is disabled.", false, 2500, null, null);
            }
            showSosCountdownDialog();
        }, tv -> tvSosCountdownVal = tv);
        countdownContainer[0].setAlpha(confirmRequired ? 1.0f : 0.45f);
        sosCard.addView(countdownContainer[0], btnLp);

        int cooldownSec = RegistryManager.getSosCooldownSec(this);
        String cooldownSub = "Cooldown: " + cooldownSec + "s (Anti-duplicate trigger lockout)";
        sosCard.addView(createSettingTile("SOS COOLDOWN LOCKOUT WINDOW", cooldownSub, android.R.drawable.ic_lock_idle_lock, v -> showSosCooldownDialog(), tv -> tvSosCooldownVal = tv), btnLp);

        int ackTimeoutSec = RegistryManager.getSosAckTimeoutSec(this);
        String ackSub = "Timeout: " + (ackTimeoutSec / 60) + " Min (Guardian acknowledgment reminder)";
        sosCard.addView(createSettingTile("SOS ACKNOWLEDGMENT TIMEOUT", ackSub, android.R.drawable.ic_dialog_email, v -> showSosAckTimeoutDialog(), tv -> tvSosAckVal = tv), btnLp);

        String fanoutScope = RegistryManager.getSosFanoutScope(this);
        String fanoutSub = "Scope: " + fanoutScope.toUpperCase() + " (Contact distribution filter)";
        sosCard.addView(createSettingTile("SOS FAN-OUT BROADCAST SCOPE", fanoutSub, android.R.drawable.ic_menu_share, v -> showSosFanoutScopeDialog(), tv -> tvSosFanoutVal = tv), btnLp);

        boolean cancelPin = RegistryManager.isSosCancelRequiresPin(this);
        View cancelPinSwitch = UiArchitect.createSwitch(this, "Require Master PIN to Cancel Active SOS", cancelPin, (v, isChecked) -> {
            RegistryManager.setSosCancelRequiresPin(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "SOS Cancel Security", isChecked ? "PIN required to cancel active SOS." : "Cancellation immediate without authentication.", false, 2500, null, null);
        });
        sosCard.addView(cancelPinSwitch, btnLp);

        content.addView(sosCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 7: 📦 OFFLINE VAULT & ENGINE SENTINELS (Priority 7)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("📦 OFFLINE VAULT & ENGINE SENTINELS (PRIORITY 7)"));
        LinearLayout vaultCard = new LinearLayout(this);
        vaultCard.setOrientation(LinearLayout.VERTICAL);

        int maxQueue = RegistryManager.getOfflineQueueMaxPoints(this);
        String queueSub = "Buffer Capacity: " + maxQueue + " Points (Circular offline queue)";
        vaultCard.addView(createSettingTile("OFFLINE GPS VAULT CAPACITY", queueSub, com.jdroidx.jfindme.R.drawable.ic_location_history, v -> showOfflineQueueCapacityDialog(), tv -> tvOfflineQueueVal = tv), btnLp);

        boolean wifiOnly = RegistryManager.isSyncWifiOnly(this);
        View wifiSwitch = UiArchitect.createSwitch(this, "Deferred Offline Sync on Wi-Fi Only", wifiOnly, (v, isChecked) -> {
            RegistryManager.setSyncWifiOnly(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Network Sync Policy", isChecked ? "Queued telemetry flushes only over unmetered Wi-Fi." : "Flushes over Cellular & Wi-Fi automatically.", false, 2500, null, null);
        });
        vaultCard.addView(wifiSwitch, btnLp);

        // [Variables]: currentCacheCap, cacheSub, ttlSub, bgJobsLauncher
        // [Constants]: Icon drawables
        // [Dependencies]: RegistryManager, BackgroundJobsConfigDialog
        // [Integration Points]: Card 7 Offline Vault & Sentinels, Database Vacuum & TTLs
        View bgJobsLauncher = createIconButton("⚙️ 12 BACKGROUND JOBS & ENGINE SENTINELS", com.jdroidx.jfindme.R.drawable.ic_settings_gears, true, v -> BackgroundJobsConfigDialog.show(this));
        vaultCard.addView(createSettingTile("DATABASE VACUUM & STORAGE RETENTION", "Rolling Retention: " + RegistryManager.getStorageRetentionDays(this) + " Days • WAL Compaction", android.R.drawable.ic_menu_manage, v -> showStorageRetentionDialog(), null), btnLp);
        int currentCacheCap = RegistryManager.getMaxGeocodeCacheSize(this);
        String cacheSub = "Max Cap: " + currentCacheCap + " Entries (L7 Reverse Geocode Cache)";
        vaultCard.addView(createSettingTile("REVERSE GEOCODE CACHE CAPACITY", cacheSub, android.R.drawable.ic_menu_mapmode, v -> showGeocodeCacheCapDialog(), null), btnLp);
        String ttlSub = "Sync Queue: " + RegistryManager.getSyncQueueTtlHours(this) + "h • Logs: " + RegistryManager.getSysLogTtlHours(this) + "h • Page Limit: " + RegistryManager.getDefaultPageLimit(this);
        vaultCard.addView(createSettingTile("DATABASE ENGINE TTL & QUERY LIMITS", ttlSub, android.R.drawable.ic_menu_preferences, v -> showDatabaseEngineTtlDialog(), null), btnLp);
        vaultCard.addView(bgJobsLauncher, btnLp);

        content.addView(vaultCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 7B: ☁️ FIREBASE SPARK PLAN & BANDWIDTH QUOTA SENTINEL
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("☁️ FIREBASE SPARK PLAN & QUOTA SENTINEL"));
        LinearLayout sparkCard = new LinearLayout(this);
        sparkCard.setOrientation(LinearLayout.VERTICAL);

        String currentTier = RegistryManager.getFirebasePlanTier(this);
        int dailyMb = RegistryManager.getSparkDailyDownloadMb(this);
        int monthlyGb = RegistryManager.getSparkMonthlyDownloadGb(this);
        String resetIst = RegistryManager.getSparkDailyResetTimeIst(this);
        String planSub = "Plan: " + currentTier + " • Daily Cap: " + dailyMb + " MB (Resets ~" + resetIst + ") • Monthly Cap: " + monthlyGb + " GB";
        
        sparkCard.addView(createSettingTile(
            "FIREBASE QUOTA & RESET RULES", 
            planSub, 
            android.R.drawable.ic_dialog_info, 
            v -> showSparkPlanQuotaDialog(), 
            null
        ), btnLp);

        boolean bwSaver = RegistryManager.isSparkBandwidthSaverEnabled(this);
        View bwSaverSwitch = UiArchitect.createSwitch(
            this, 
            "Bandwidth Sentinel Guard (Save Quota)", 
            bwSaver, 
            (v, isChecked) -> {
                RegistryManager.setSparkBandwidthSaverEnabled(this, isChecked);
                UiArchitect.showSlideNotificationBanner(
                    this, 
                    "Bandwidth Guard Policy", 
                    isChecked ? "Active: Deduplicates stationary GPS and enforces lean payload." : "Standard bandwidth mode active.", 
                    false, 
                    2500, 
                    null, 
                    null
                );
            }
        );
        sparkCard.addView(bwSaverSwitch, btnLp);

        // [Variables]: deadReckoningRadius, deadReckoningSub, pulseIntervalMs, pulseSub, currentGroup, groupSub
        // [Constants]: Icon drawables
        // [Dependencies]: RegistryManager
        // [Integration Points]: Card 7B Dynamic Telemetry, Dead-Reckoning, Presence Pulse & Group-By SSOT
        float deadReckoningRadius = RegistryManager.getDeadReckoningRadiusMeters(this);
        String deadReckoningSub = "Noise Gate: " + (int)deadReckoningRadius + "m • Suppresses Stationary GPS (97% Data Saving)";
        sparkCard.addView(createSettingTile(
            "SPATIAL DEAD-RECKONING NOISE GATE",
            deadReckoningSub,
            android.R.drawable.ic_menu_compass,
            v -> showDeadReckoningRadiusDialog(),
            null
        ), btnLp);

        long pulseIntervalMs = RegistryManager.getPresencePulseIntervalMs(this);
        String pulseSub = "Cadence: " + (pulseIntervalMs / 1000) + "s • 28-Byte Presence Pulse (Keeps Server Online)";
        sparkCard.addView(createSettingTile(
            "PRESENCE PULSE HEARTBEAT INTERVAL",
            pulseSub,
            android.R.drawable.ic_menu_recent_history,
            v -> showPresencePulseIntervalDialog(),
            null
        ), btnLp);

        String currentGroup = RegistryManager.getDeviceCorrespondingGroup(this);
        String groupSub = "Hierarchy Group: " + currentGroup + " • Relative Group-By Partition";
        sparkCard.addView(createSettingTile(
            "DEVICE CORRESPONDING GROUP (GROUP-BY)",
            groupSub,
            android.R.drawable.ic_menu_myplaces,
            v -> showDeviceGroupDialog(),
            null
        ), btnLp);

        content.addView(sparkCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 7C: 🔀 TELEMETRY FAILOVER & CLOSED-LOOP SENTINEL (PRIORITY 7C)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🔀 TELEMETRY FAILOVER & CLOSED LOOP (PRIORITY 7C)"));
        LinearLayout failoverCard = new LinearLayout(this);
        failoverCard.setOrientation(LinearLayout.VERTICAL);

        long ackTimeout = RegistryManager.getClosedLoopAckTimeoutMs(this);
        String closedLoopAckSub = "Timeout: " + ackTimeout + "ms (Server ACK round-trip deadline before failover)";
        failoverCard.addView(createSettingTile(
            "CLOSED-LOOP SERVER ACK TIMEOUT",
            closedLoopAckSub,
            android.R.drawable.ic_dialog_info,
            v -> showClosedLoopAckTimeoutDialog(),
            tv -> tvClosedLoopTimeoutVal = tv
        ), btnLp);

        int maxFailures = RegistryManager.getMaxAckFailures(this);
        String maxFailuresSub = "Threshold: " + maxFailures + " Timeouts (Circuit breaker threshold before route switch)";
        failoverCard.addView(createSettingTile(
            "CIRCUIT BREAKER FAILURE THRESHOLD",
            maxFailuresSub,
            android.R.drawable.ic_lock_idle_lock,
            v -> showMaxAckFailuresDialog(),
            tv -> tvMaxAckFailuresVal = tv
        ), btnLp);

        String smsTarget = RegistryManager.getFailoverSmsTarget(this);
        String smsTargetSub = (smsTarget != null && !smsTarget.trim().isEmpty())
            ? "Recipient: " + smsTarget + " (Level N3 zero-data beacon)"
            : "Not Configured (Tap to set Emergency SOS SMS Number)";
        failoverCard.addView(createSettingTile(
            "BASEBAND SOS SMS BEACON TARGET",
            smsTargetSub,
            android.R.drawable.ic_dialog_email,
            v -> showFailoverSmsDialog(),
            tv -> tvFailoverSmsVal = tv
        ), btnLp);

        content.addView(failoverCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 8: 🗺️ MAP DISPLAY & GLOBAL AESTHETICS (Priority 8)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🗺️ MAP DISPLAY & GLOBAL AESTHETICS (PRIORITY 8)"));
        LinearLayout mapPrefsCard = new LinearLayout(this);
        mapPrefsCard.setOrientation(LinearLayout.VERTICAL);

        boolean isMetric = RegistryManager.isUnitMetric(this);
        View unitToggle = UiArchitect.createSegmentedToggle(this, "METRIC (KM/H)", "IMPERIAL (MPH)", isMetric, isLeft -> {
            RegistryManager.setUnitMetric(this, isLeft);
            UiArchitect.showSlideNotificationBanner(this, "Units Preference", isLeft ? "Distance Units: Metric (km/h)" : "Distance Units: Imperial (mph)", false, 2500, null, null);
        });
        mapPrefsCard.addView(unitToggle, btnLp);

        boolean currentCluster = RegistryManager.isSmartAvatarClusteringEnabled(this);
        View clusterSwitch = UiArchitect.createSwitch(this, "Smart Avatar Clustering (10m grouping)", currentCluster, (v, isChecked) -> {
            RegistryManager.setSmartAvatarClusteringEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Clustering Updated", isChecked ? "Avatars within 10m will be grouped." : "Individual avatar pins active.", false, 2500, null, null);
        });
        mapPrefsCard.addView(clusterSwitch, btnLp);

        mapPrefsCard.addView(createIconButton("GLOBAL UI & ICON ARCHITECT", com.jdroidx.jfindme.R.drawable.ic_edit, v -> safeLaunch(UiConfigActivity.class)), btnLp);
        mapPrefsCard.addView(createIconButton("VISUAL THEME & COMPONENT GALLERY", com.jdroidx.jfindme.R.drawable.ic_nav_map, v -> safeLaunch(UiGalleryActivity.class)), btnLp);

        content.addView(mapPrefsCard, getCardParams());

        return content;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 📱 TAB 2: SCREEN MODULES (Encapsulated Grouped Navigation & Control Cards)
    // ──────────────────────────────────────────────────────────────────────────

    private View buildScreensNavigationContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int setPad = getResponsivePaddingQuota();
        content.setPadding(setPad * 2, setPad / 2, setPad * 2, setPad * 4);

        screenCardHolders.clear();

        // 🔍 Interactive Screen Search Box
        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setBackground(getSolidDrawable(getCardColor(), scaleRadius(25)));
        searchBox.setPadding(scale(25), scale(10), scale(25), scale(10));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.setMargins(0, 0, 0, scale(25));
        searchBox.setLayoutParams(searchLp);

        ImageView searchIcon = new ImageView(this);
        searchIcon.setImageResource(android.R.drawable.ic_menu_search);
        searchIcon.setColorFilter(getSecondaryTextColor());
        int sIconSize = scale(45);
        searchBox.addView(searchIcon, new LinearLayout.LayoutParams(sIconSize, sIconSize));

        EditText searchEt = new EditText(this);
        searchEt.setHint("🔍 Search screens, tools or settings...");
        searchEt.setTextColor(getTextColor());
        searchEt.setHintTextColor(getSecondaryTextColor());
        searchEt.setBackgroundColor(Color.TRANSPARENT);
        searchEt.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_LABEL"));
        searchEt.setPadding(scale(20), 0, scale(20), 0);
        searchEt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterScreenCards(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        searchBox.addView(searchEt);
        content.addView(searchBox);

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 1: 🗺️ TACTICAL LIVE MAPS & RADAR
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🗺️ TACTICAL LIVE MAPS & RADAR"));
        content.addView(registerScreenCard("PRIMARY LIVE MAP", "Vector Map with Marker Callouts, Direction Chevrons & SOS Trigger", "LIVE", null, com.jdroidx.jfindme.R.drawable.ic_nav_map, "map live tracking gps pins sos chevron", v -> safeLaunch(MapActivity.class)));
        content.addView(registerScreenCard("TACTICAL DASHBOARD HUD", "Dual-Pane Real-Time Radar & Multi-Node Coordinate Telemetry", "RADAR", null, com.jdroidx.jfindme.R.drawable.ic_radar, "radar dashboard telemetry coordinates dual pane", v -> safeLaunch(LiveMapDashboardActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 2: 👥 CIRCLE MESH & PAIRING GATEWAYS
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("👥 CIRCLE MESH & PAIRING GATEWAYS"));
        content.addView(registerScreenCard("NETWORK PAIRING GATEWAY", "Cloud PIN, QR Matrix Scanner & Nearby P2P Offline Discovery", "PAIRING", tv -> badgeRosterTv = tv, com.jdroidx.jfindme.R.drawable.ic_pairing_matrix, "pairing qr pin p2p wifi direct connect network gateway", v -> safeLaunch(NetworkGatewayActivity.class)));
        content.addView(registerScreenCard("FAMILY & GUARDIANS ROSTER", "Multi-Tier Circle Management & Immediate Callout Matrix", "CIRCLE", null, com.jdroidx.jfindme.R.drawable.ic_family_friends, "family friends guardians roster members contacts", v -> safeLaunch(FamilyFriendsActivity.class)));
        content.addView(registerScreenCard("CIRCLE GROUPS PARTITION", "Create, Edit & Swap Active Team/Family Circles", "GROUPS", null, com.jdroidx.jfindme.R.drawable.ic_people, "groups teams partition circles swap", v -> safeLaunch(GroupActivity.class)));
        content.addView(registerScreenCard("MEMBER DOSSIER & ACL", "Node Profile, Medical Notes & Granular Access Control List", "ACL", null, com.jdroidx.jfindme.R.drawable.ic_user_profile, "dossier acl permissions member medical notes person details", v -> safeLaunch(PersonDetailsActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 3: 👤 USER IDENTITY, 3D AVATAR & GOVERNANCE
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("👤 USER IDENTITY, 3D AVATAR & GOVERNANCE"));
        content.addView(registerScreenCard("PUPIL IDENTITY PROFILE", "Hardware Silicon Anchor, Demographics & Medical Ledger", getNicknameBadgeText(), tv -> badgeProfileTv = tv, com.jdroidx.jfindme.R.drawable.ic_user_profile, "profile identity pupil nickname phone email address avatar", v -> safeLaunch(ProfileActivity.class)));
        content.addView(registerScreenCard("3D VRM AVATAR EDITOR", "Interactive 3D Model Customization, Mesh Rotation & Shaders", "3D VRM", null, com.jdroidx.jfindme.R.drawable.ic_edit, "avatar 3d vrm model mesh rotation editor", v -> safeLaunch(AvatarEditorActivity.class)));
        content.addView(registerScreenCard("BIOMETRIC AVATAR CROP", "Circular Photo Masking & Aspect-Ratio Biometric Generator", "CROP", null, com.jdroidx.jfindme.R.drawable.ic_crop, "crop avatar photo camera picture biometric", v -> safeLaunch(AvatarCropActivity.class)));
        content.addView(registerScreenCard("ENTERPRISE ORGANIZATION", "Corporate Multi-Tenancy Domain & Department Hierarchy", "TENANT", null, com.jdroidx.jfindme.R.drawable.ic_about_app, "organization corporate enterprise tenant department domain", v -> safeLaunch(OrgActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 4: 🌐 GEOSPATIAL & SAFE ZONE CLUSTER
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🌐 GEOSPATIAL & SAFE ZONE CLUSTER"));
        content.addView(registerScreenCard("SAFE ZONE MANAGER", "Circular & Polygon Perimeter Geofence Editor with Schedules", "GEOFENCE", tv -> badgeZonesTv = tv, com.jdroidx.jfindme.R.drawable.ic_safe_zone, "safe zones geofence polygon circle perimeter schedule", v -> safeLaunch(GeofenceActivity.class)));
        content.addView(registerScreenCard("TRANSITION AUDIT LEDGER", "Perimeter Breach Ingress/Egress History, Filters & CSV Export", "LOGS", null, com.jdroidx.jfindme.R.drawable.ic_nav_notice, "geofence notification transitions audit ingress egress breach export", v -> safeLaunch(GeofenceNotificationActivity.class)));
        content.addView(registerScreenCard("LOCATION LOGS & REPLAY", "Historical GPS Trajectory Playback with 1x/2x/5x Speeds", "REPLAY", tv -> badgeHistoryTv = tv, com.jdroidx.jfindme.R.drawable.ic_location_history, "history breadcrumbs replay trajectory route waypoints", v -> safeLaunch(HistoryActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 5: 🛡️ DEVICE PRIVACY, HEALTH & ALERTS
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🛡️ DEVICE PRIVACY, HEALTH & ALERTS"));
        content.addView(registerScreenCard("IDENTITY SECURITY PROTOCOL", "Hardware Permissions, Root Kernel Audit & Ghost Mode Locks", "SECURITY", null, com.jdroidx.jfindme.R.drawable.ic_privacy_lock, "privacy security root jailbreak ghost permissions", v -> safeLaunch(PrivacyActivity.class)));
        content.addView(registerScreenCard("DEVICE HEALTH & VITALS", "Battery Draw, Doze Mode State, RAM & Sensor Telemetry", getBatteryBadgeText(), tv -> badgeHealthTv = tv, com.jdroidx.jfindme.R.drawable.ic_device_health, "health battery doze ram vitals sensors diagnostics", v -> safeLaunch(HealthActivity.class)));
        content.addView(registerScreenCard("SYSTEM NOTIFICATIONS ROUTING", "Alert Triggers, DND Bypass & Hardware Notification Pokes", "ALERTS", null, com.jdroidx.jfindme.R.drawable.ic_nav_notice, "notifications alerts sound vibration triggers dnd poke", v -> safeLaunch(NotifActivity.class)));
        content.addView(registerScreenCard("ACOUSTIC EMERGENCY SIREN", "Emergency Horn & 5Hz Torch Flashlight Beacon Test", "SIREN", null, android.R.drawable.ic_lock_idle_alarm, "siren horn alarm flashlight emergency beacon", v -> safeLaunch(AcousticSirenActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 6: 🔍 FORENSIC VAULT, AUDIT & DIAGNOSTICS
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🔍 FORENSIC VAULT, AUDIT & DIAGNOSTICS"));
        content.addView(registerScreenCard("FORENSIC VAULT EXPLORER", "Encrypted Offline Telemetry Ledger & Direct Raw SQLite View", "VAULT", null, com.jdroidx.jfindme.R.drawable.ic_system_audit, "vault forensic raw sqlite database encrypted explorer", v -> safeLaunch(ForensicVaultExplorerActivity.class)));
        content.addView(registerScreenCard("WATCHDOG SENTINEL AUDIT", "Process Liveness, AlarmManager Heartbeat & Recovery Logs", "WATCHDOG", null, com.jdroidx.jfindme.R.drawable.ic_watchdog, "watchdog audit heartbeat liveness recovery sentinel", v -> {
            Intent wd = new Intent(this, WatchDogAuditActivity.class);
            wd.putExtra("VIEW_MODE", "STATE");
            startActivity(wd);
        }));
        content.addView(registerScreenCard("DATABASE MANAGEMENT", "17 SQLite Tables Vacuum, Index Rebuild & Schema Optimization", "SQLITE", null, com.jdroidx.jfindme.R.drawable.ic_system_audit, "database sqlite vacuum index schema tables purge", v -> safeLaunch(DatabaseManagementActivity.class)));
        content.addView(registerScreenCard("FIREBASE CLOUD MATRIX", "Realtime Database Connection Health, Latency Pings & Sync Tree", "CLOUD", null, com.jdroidx.jfindme.R.drawable.ic_pairing_matrix, "firebase cloud matrix realtime latency connection sync tree", v -> safeLaunch(FirebaseCloudMatrixActivity.class)));
        content.addView(registerScreenCard("SYSTEM AUDIT LOG MATRIX", "Rolling Runtime Traces, Memory Dumps & Filtered Event Log", "LOGS", null, com.jdroidx.jfindme.R.drawable.ic_settings_gears, "system logs audit memory dumps traces rolling", v -> safeLaunch(SystemLogActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 7: ⚡ SYSTEM UPDATE, PURGE & MAINTENANCE
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("⚡ SYSTEM UPDATE, PURGE & MAINTENANCE"));
        content.addView(registerScreenCard("SYSTEM SILENT UPDATE", "In-Place Package Synchronization, GitHub & Drive Channels", getVersionBadgeText(), null, android.R.drawable.ic_menu_upload, "update upgrade version apk github drive download", v -> safeLaunch(SystemUpdateActivity.class)));
        content.addView(registerScreenCard("FACTORY RESET & PURGE CONSOLE", "Selective SQLite Wipe, Pre-Uninstall Teardown & Reset", "RESET", null, com.jdroidx.jfindme.R.drawable.ic_system_audit, "factory reset purge uninstall teardown wipe erase", v -> safeLaunch(FactoryResetActivity.class)));
        content.addView(registerScreenCard("HELP & SUPPORT CENTER", "Documentation, FAQs, Diagnostics Export & Helpdesk", "SUPPORT", null, com.jdroidx.jfindme.R.drawable.ic_help_support, "help support faqs documentation manual helpdesk", v -> safeLaunch(HelpSupportActivity.class)));
        content.addView(registerScreenCard("ABOUT JFINDME", "Licensing, EULA, Engine Manifest & Version Signatures", "ABOUT", null, android.R.drawable.ic_menu_info_details, "about licensing eula build manifest architecture", v -> safeLaunch(AboutActivity.class)));

        return content;
    }

    /**
     * Registers and renders a Screen Navigation Card with search index.
     */
    private View registerScreenCard(String title, String subtitle, String initialBadge, ViewBinder binder, int iconRes, String searchKeywords, View.OnClickListener listener) {
        View card = createScreenCard(title, subtitle, initialBadge, binder, iconRes, listener);
        screenCardHolders.add(new ScreenCardHolder(card, title + " " + subtitle + " " + searchKeywords));
        return card;
    }

    /**
     * Filters registered screen cards based on interactive search query.
     */
    private void filterScreenCards(String query) {
        String q = query != null ? query.trim().toLowerCase(Locale.US) : "";
        for (ScreenCardHolder holder : screenCardHolders) {
            if (q.isEmpty() || holder.searchTag.contains(q)) {
                holder.cardView.setVisibility(View.VISIBLE);
            } else {
                holder.cardView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Asynchronously queries SQLite database for real-time telemetry badges.
     */
    private void loadLiveMetricsForScreens() {
        new Thread(() -> {
            int rosterCount = 0;
            int safeZoneCount = 0;
            int historyCount = 0;
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db != null && db.isOpen()) {
                    Cursor c1 = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.MASTER_ROSTER, null);
                    if (c1 != null) {
                        if (c1.moveToFirst()) rosterCount = c1.getInt(0);
                        c1.close();
                    }
                    Cursor c2 = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_SAFE_ZONES, null);
                    if (c2 != null) {
                        if (c2.moveToFirst()) safeZoneCount = c2.getInt(0);
                        c2.close();
                    }
                    Cursor c3 = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_HISTORY, null);
                    if (c3 != null) {
                        if (c3.moveToFirst()) historyCount = c3.getInt(0);
                        c3.close();
                    }
                }
            } catch (Exception e) {
                SystemLogger.w(TAG, "Database metric query non-fatal: " + e.getMessage());
            }

            final int fRoster = rosterCount;
            final int fZones = safeZoneCount;
            final int fHist = historyCount;

            runOnUiThread(() -> {
                if (badgeRosterTv != null) badgeRosterTv.setText(fRoster > 0 ? fRoster + " NODES" : "NO NODES");
                if (badgeZonesTv != null) badgeZonesTv.setText(fZones > 0 ? fZones + " ZONES" : "EMPTY");
                if (badgeHistoryTv != null) badgeHistoryTv.setText(fHist > 0 ? fHist + " PTS" : "EMPTY");
            });
        }).start();
    }

    private String getBatteryBadgeText() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    int pct = (int) ((level / (float) scale) * 100);
                    return pct + "% BATT";
                }
            }
        } catch (Exception ignored) {}
        return "VITALS";
    }

    private String getNicknameBadgeText() {
        String nick = RegistryManager.getUserNickname(this);
        if (nick != null && !nick.trim().isEmpty() && !"Active Tracker".equals(nick)) {
            return nick.toUpperCase();
        }
        return "PROFILE";
    }

    private String getVersionBadgeText() {
        return "V" + BuildConfig.VERSION_NAME;
    }

    /**
     * Crash-Proof Activity Launcher with UI boundary error handling.
     */
    private void safeLaunch(Class<?> activityClass) {
        try {
            Intent intent = new Intent(this, activityClass);
            startActivity(intent);
        } catch (Exception e) {
            SystemLogger.e(TAG, "Failed to launch " + activityClass.getSimpleName(), e);
            UiArchitect.showSlideNotificationBanner(this, "Module Launch Error", "Unable to open " + activityClass.getSimpleName() + ": " + e.getMessage(), true, 3000, null, null);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 🎨 UI COMPONENT BUILDERS (Reusable OOPS MVC Pattern)
    // ──────────────────────────────────────────────────────────────────────────

    private interface ViewBinder {
        void onBound(TextView textView);
    }

    private View createSettingTile(String title, String subtitle, int iconRes, View.OnClickListener listener, ViewBinder binder) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, tile, "CARD_STANDARD");
        int pad = scale(25);
        tile.setPadding(pad, pad, pad, pad);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart());
        int iconSize = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        tile.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(scale(25), 0, scale(15), 0);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getTextColor());
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_LABEL"));
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(getSecondaryTextColor());
        tvSub.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
        tvSub.setPadding(0, scale(6), 0, 0);
        textCol.addView(tvSub);

        if (binder != null) {
            binder.onBound(tvSub);
        }

        tile.addView(textCol);

        ImageView editIcon = new ImageView(this);
        editIcon.setImageResource(com.jdroidx.jfindme.R.drawable.ic_edit);
        editIcon.setColorFilter(getSecondaryTextColor());
        int chSize = scale(35);
        tile.addView(editIcon, new LinearLayout.LayoutParams(chSize, chSize));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, scale(8), 0, scale(8));
        tile.setLayoutParams(lp);
        return tile;
    }

    private View createScreenCard(String title, String subtitle, String initialBadge, ViewBinder badgeBinder, int iconRes, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, card, "CARD_STANDARD");
        int pad = scale(25);
        card.setPadding(pad, pad, pad, pad);
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart());
        int iconSize = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        card.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(scale(25), 0, scale(15), 0);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getTextColor());
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_LABEL"));
        titleRow.addView(tvTitle);

        if (initialBadge != null && !initialBadge.isEmpty()) {
            TextView tvBadge = new TextView(this);
            tvBadge.setText(initialBadge);
            tvBadge.setTextColor(getGradientOverlayText());
            tvBadge.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_TINY"));
            tvBadge.setTypeface(null, Typeface.BOLD);
            tvBadge.setBackground(getSolidDrawable(getGradientStart(), scaleRadius(8)));
            int bPad = scale(10);
            tvBadge.setPadding(bPad, bPad / 3, bPad, bPad / 3);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bLp.leftMargin = scale(15);
            tvBadge.setLayoutParams(bLp);
            titleRow.addView(tvBadge);

            if (badgeBinder != null) {
                badgeBinder.onBound(tvBadge);
            }
        }
        textCol.addView(titleRow);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(getSecondaryTextColor());
        tvSub.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
        tvSub.setPadding(0, scale(6), 0, 0);
        textCol.addView(tvSub);

        card.addView(textCol);

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(android.R.drawable.ic_media_play);
        chevron.setColorFilter(getSecondaryTextColor());
        int chSize = scale(35);
        card.addView(chevron, new LinearLayout.LayoutParams(chSize, chSize));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, scale(15));
        card.setLayoutParams(cardLp);
        return card;
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
        header.setTextColor(getGradientStart());
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

    // ──────────────────────────────────────────────────────────────────────────
    // ⚙️ INTERACTIVE CONFIGURATION DIALOGS (With In-Place Instant Mutation)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Master Human-in-the-Loop Safe Recovery: Restores all universal engine parameters to enterprise defaults.
     */
        private void showStealthCadenceDialog() {
        long currentMins = DatabaseHelper.getInstance(this).getStealthIntervalMins();
        final long[] options = { 15L, 30L, 45L, 60L, 120L };
        final String[] labels = {
            "15 Mins (Fast Cadence - Moderate Battery)",
            "30 Mins (Balanced - Recommended for School Hours)",
            "45 Mins (Extended Battery)",
            "60 Mins (Default - Maximum Battery Preservation)",
            "120 Mins (Ultra Low Power - 2 Hour Pings)"
        };
        int selected = 3;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == currentMins) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏰ Stealth Dormant Cadence")
            .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                long chosen = options[which];
                DatabaseHelper.getInstance(this).setStealthIntervalMins(chosen);
                com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.scheduleNextStealthPulse(this, chosen);
                if (tvStealthCadenceVal != null) {
                    tvStealthCadenceVal.setText("Cadence: " + chosen + " mins | Autonomous SIM and Alarm Sentinel");
                }
                UiArchitect.showSlideNotificationBanner(this, "Stealth Cadence", "Dormant wake cycle set to " + chosen + " minutes.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void promptRestoreDefaults() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("↺ Restore Recommended Defaults")
            .setMessage("Are you sure you want to restore all universal telemetry, cadence, geofencing, and SOS parameters to enterprise recommended defaults?")
            .setPositiveButton("RESTORE DEFAULTS", (dialog, which) -> {
                // Tracking Cadence & Battery
                prefs.edit().putBoolean("pref_stepper_active", false).apply();
                RegistryManager.setTrackingInterval(this, 15000L);
                RegistryManager.setActivityRecognitionEnabled(this, true);
                RegistryManager.setMotionStillIntervalMs(this, 300000L);
                RegistryManager.setMotionVehicleIntervalMs(this, 10000L);
                RegistryManager.setMotionWalkingIntervalMs(this, 20000L);
                RegistryManager.setMinDisplacementMeters(this, 0.0f);
                RegistryManager.setStrictAccuracyWait(this, false);
                DatabaseHelper.getInstance(this).setStealthIntervalMins(60L);
                com.jdroidx.jfindme.service.StealthAlarmSentinelEngine.scheduleNextStealthPulse(this, 60L);

                // Road Alignment & Kalman
                RegistryManager.setRoadSmoothingEnabled(this, true);
                RegistryManager.setHeadingEmaFactor(this, 0.35f);
                RegistryManager.setKalmanMeasurementNoise(this, 25.0f);

                // Geofence Sentinel
                RegistryManager.setGeofenceJitterDebounceMs(this, 15000L);
                RegistryManager.setGeofenceLatchEnabled(this, true);
                RegistryManager.setGeofenceSnapCenterEnabled(this, true);
                RegistryManager.setGeofenceLocalNotifEnabled(this, true);
                RegistryManager.setGeofenceFcmRelayEnabled(this, true);
                RegistryManager.setDefaultSafeZoneRadiusMeters(this, 100);

                // RAG Telemetry
                RegistryManager.setLowBatteryThreshold(this, 20);
                RegistryManager.setStaleInactiveMinutes(this, 15);
                RegistryManager.setCriticalOfflineHours(this, 6);
                RegistryManager.setDistantDeviceThresholdMeters(this, 200);

                // Emergency SOS & Siren
                RegistryManager.setSirenDurationSec(this, 30);
                RegistryManager.setSirenVibrationEnabled(this, true);
                RegistryManager.setSirenDndBypassEnabled(this, true);
                RegistryManager.setSosConfirmationRequired(this, true);
                RegistryManager.setSosCountdownSec(this, 5);
                RegistryManager.setSosCooldownSec(this, 60);
                RegistryManager.setSosAckTimeoutSec(this, 1800);
                RegistryManager.setSosFanoutScope(this, "all_contacts");
                RegistryManager.setSosCancelRequiresPin(this, false);

                // Offline Vault & Units
                RegistryManager.setOfflineQueueMaxPoints(this, 500);
                RegistryManager.setSyncWifiOnly(this, false);
                RegistryManager.setUnitMetric(this, true);
                RegistryManager.setSmartAvatarClusteringEnabled(this, true);
                RegistryManager.setLocationPauseDurationMin(this, 30);
                prefs.edit().putInt("pairing_pin_ttl_minutes", 5).apply();

                // Mutate all in-place UI TextViews
                refreshAllUniversalTextViews();

                UiArchitect.showSlideNotificationBanner(this, "Defaults Restored", "All engine parameters reset to enterprise recommended defaults.", false, 3500, null, null);
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void refreshAllUniversalTextViews() {
        if (tvPingIntervalVal != null) tvPingIntervalVal.setText("Active Cadence: 15s | Balanced (15s)");
        if (tvStealthCadenceVal != null) tvStealthCadenceVal.setText("Cadence: 60 mins | Autonomous SIM & Alarm Sentinel");
        if (tvMotionDutyVal != null) tvMotionDutyVal.setText("Still: 300s | Car: 10s | Walk: 20s");
        if (tvDisplacementVal != null) tvDisplacementVal.setText("Displacement: 0m (Suppresses stationary GPS jitter)");
        if (tvRoadSmoothingVal != null) tvRoadSmoothingVal.setText("Heading EMA: 0.35 | Measurement Noise R: 25");
        if (tvJitterVal != null) tvJitterVal.setText("Debounce: 15s (Temporal boundary anti-flutter)");
        if (tvRadiusVal != null) tvRadiusVal.setText("Default Radius: 100m (Base safe zone perimeter)");
        if (tvLowBatteryVal != null) tvLowBatteryVal.setText("Threshold: 20% (Triggers amber distress telemetry)");
        if (tvStaleVal != null) tvStaleVal.setText("Stale Window: 15 Min (Marks node amber inactive)");
        if (tvCriticalOfflineVal != null) tvCriticalOfflineVal.setText("Offline Window: 6 Hours (Marks node red offline)");
        if (tvDistantVal != null) tvDistantVal.setText("Panel Threshold: 200m (Distance to float HUD panel)");
        if (tvRadarRadiusVal != null) {
            tvRadarRadiusVal.setText("Radar Proximity Radius: " + RegistryManager.getRadarProximityRadiusMeters(this) + "m (Sky blue highlight threshold)");
        }
        if (tvRemoteLocateTimeoutVal != null) {
            tvRemoteLocateTimeoutVal.setText("Remote Locate Timeout: " + (RegistryManager.getRemoteLocateTimeoutMs(this) / 1000) + "s (Maximum GNSS response window)");
        }
        if (tvSirenDurationVal != null) tvSirenDurationVal.setText("Duration: 30s (Acoustic horn auto-silence timer)");
        if (tvSosCountdownVal != null) tvSosCountdownVal.setText("Countdown: 5s (Emergency abort cancellation window)");
        if (tvSosCooldownVal != null) tvSosCooldownVal.setText("Cooldown: 60s (Anti-duplicate trigger lockout)");
        if (tvSosAckVal != null) tvSosAckVal.setText("Timeout: 30 Min (Guardian acknowledgment reminder)");
        if (tvSosFanoutVal != null) tvSosFanoutVal.setText("Scope: ALL_CONTACTS (Contact distribution filter)");
        if (tvOfflineQueueVal != null) tvOfflineQueueVal.setText("Buffer Capacity: 500 Points (Circular offline queue)");
        if (tvPauseDurationVal != null) tvPauseDurationVal.setText("Window: 30 Min (Standby pause duration before auto-resume)");
        if (tvPinTtlVal != null) tvPinTtlVal.setText("TTL: 5 Min (NIST SP 800-63B pairing countdown)");
        if (tvClosedLoopTimeoutVal != null) tvClosedLoopTimeoutVal.setText("Timeout: " + RegistryManager.getClosedLoopAckTimeoutMs(this) + "ms (Server ACK round-trip deadline before failover)");
        if (tvMaxAckFailuresVal != null) tvMaxAckFailuresVal.setText("Threshold: " + RegistryManager.getMaxAckFailures(this) + " Timeouts (Circuit breaker threshold before route switch)");
        if (tvFailoverSmsVal != null) {
            String sms = RegistryManager.getFailoverSmsTarget(this);
            tvFailoverSmsVal.setText((sms != null && !sms.trim().isEmpty()) ? "Recipient: " + sms + " (Level N3 zero-data beacon)" : "Not Configured (Tap to set Emergency SOS SMS Number)");
        }
    }

    /**
     * Guardian PIN prompt for tamper-proof child mode with in-place zero-flicker reversion.
     */
    private void showPinPromptForChildMode(androidx.appcompat.widget.SwitchCompat switchCompat, boolean[] programmaticFlag, String storedPin) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = getResponsivePaddingQuota();
        container.setPadding(pad, pad / 2, pad, pad / 2);

        int overlayColor = getGradientOverlayText();
        EditText et = new EditText(this);
        et.setHint("Enter 4-6 digit Guardian PIN");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setTextColor(overlayColor);
        et.setHintTextColor(overlayColor == Color.WHITE ? Color.parseColor("#99FFFFFF") : Color.GRAY);
        et.setPadding(30, 25, 30, 25);
        et.setBackground(getSolidDrawable(overlayColor == Color.WHITE ? Color.parseColor("#26FFFFFF") : Color.parseColor("#26000000"), 20f));
        container.addView(et);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle("🛡️ Guardian PIN Verification")
            .setMessage("Enter Master Guardian PIN to unlock and disable Child Protection:")
            .setView(container)
            .setPositiveButton("UNLOCK", (d, which) -> {
                String entered = et.getText().toString().trim();
                if (storedPin.equals(entered)) {
                    RegistryManager.setChildWardMode(this, false);
                    UiArchitect.showSlideNotificationBanner(this, "Protection Unlocked", "Child mode disabled with Guardian authorization.", false, 3000, null, null);
                } else {
                    revertSwitch(switchCompat, programmaticFlag);
                    UiArchitect.showSlideNotificationBanner(this, "Access Denied", "Incorrect Guardian PIN. Child mode remains active.", true, 3000, null, null);
                }
            })
            .setNegativeButton("CANCEL", (d, which) -> revertSwitch(switchCompat, programmaticFlag))
            .setOnCancelListener(d -> revertSwitch(switchCompat, programmaticFlag))
            .create();

        dialog.show();
        applyDialogTheme(dialog);
    }

    private void revertSwitch(androidx.appcompat.widget.SwitchCompat switchCompat, boolean[] programmaticFlag) {
        if (switchCompat != null) {
            programmaticFlag[0] = true;
            switchCompat.setChecked(true);
            programmaticFlag[0] = false;
        }
    }

    private interface NumericValueCallback {
        void onValueConfirmed(int value);
    }

    /**
     * Reusable numeric prompt dialog with bounds validation and in-place feedback.
     */
    private void showCustomNumericInputDialog(String title, String hint, int min, int max, int currentVal, String unit, NumericValueCallback callback) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = getResponsivePaddingQuota();
        container.setPadding(pad, pad / 2, pad, pad / 2);

        TextView rangeTv = new TextView(this);
        rangeTv.setText("Allowed range: " + min + " to " + max + (unit != null ? " " + unit : ""));
        rangeTv.setTextColor(getSecondaryTextColor());
        rangeTv.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
        rangeTv.setPadding(0, 0, 0, scale(15));
        container.addView(rangeTv);

        int overlayColor = getGradientOverlayText();
        EditText et = new EditText(this);
        et.setHint(hint != null ? hint : "Enter number...");
        et.setText(String.valueOf(currentVal));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(overlayColor);
        et.setHintTextColor(overlayColor == Color.WHITE ? Color.parseColor("#99FFFFFF") : Color.GRAY);
        et.setPadding(30, 25, 30, 25);
        et.setBackground(getSolidDrawable(overlayColor == Color.WHITE ? Color.parseColor("#26FFFFFF") : Color.parseColor("#26000000"), 20f));
        container.addView(et);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("SAVE", (d, which) -> {
                String input = et.getText().toString().trim();
                try {
                    int val = Integer.parseInt(input);
                    if (val >= min && val <= max) {
                        if (callback != null) callback.onValueConfirmed(val);
                    } else {
                        UiArchitect.showSlideNotificationBanner(this, "Out of Range", "Value must be between " + min + " and " + max + (unit != null ? " " + unit : ""), true, 3000, null, null);
                    }
                } catch (NumberFormatException e) {
                    UiArchitect.showSlideNotificationBanner(this, "Invalid Format", "Please enter a valid numeric value.", true, 3000, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .create();

        dialog.show();
        applyDialogTheme(dialog);
    }

    private void showBatteryOptimizerDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());
        int p = scale(40);
        root.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("🔋 ENERGY & PING OPTIMIZER");
        title.setTextColor(getGradientStart());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        title.setPadding(0, 0, 0, 40);
        root.addView(title);

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

        int tacticalSize = scale(110);

        ImageView decBtn = new ImageView(this);
        decBtn.setImageResource(com.jdroidx.jfindme.R.drawable.ic_minus);
        decBtn.setColorFilter(getGradientOverlayText());
        decBtn.setBackground(getSolidDrawable(getGradientStart(), scaleRadius(15)));
        decBtn.setPadding(30, 30, 30, 30);
        decBtn.setOnClickListener(v -> {
            if (!masterSwitch.isChecked()) return;
            long val = Long.parseLong(valTv.getText().toString().replace("s", ""));
            if (val > 90) {
                val -= 10;
                valTv.setText(val + "s");
                prefs.edit().putLong("pref_manual_pulse", val * 1000).apply();
                RegistryManager.setTrackingInterval(this, val * 1000);
                if (tvPingIntervalVal != null) {
                    tvPingIntervalVal.setText("Active Cadence: " + val + "s | Manual Stepper");
                }
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            }
        });

        ImageView incBtn = new ImageView(this);
        incBtn.setImageResource(android.R.drawable.ic_input_add);
        incBtn.setColorFilter(getGradientOverlayText());
        incBtn.setBackground(getSolidDrawable(getGradientStart(), scaleRadius(15)));
        incBtn.setPadding(30, 30, 30, 30);
        incBtn.setOnClickListener(v -> {
            if (!masterSwitch.isChecked()) return;
            long val = Long.parseLong(valTv.getText().toString().replace("s", ""));
            val += 10;
            valTv.setText(val + "s");
            prefs.edit().putLong("pref_manual_pulse", val * 1000).apply();
            RegistryManager.setTrackingInterval(this, val * 1000);
            if (tvPingIntervalVal != null) {
                tvPingIntervalVal.setText("Active Cadence: " + val + "s | Manual Stepper");
            }
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        });

        stepperControls.addView(decBtn, new LinearLayout.LayoutParams(tacticalSize, tacticalSize));
        stepperControls.addView(valTv);
        stepperControls.addView(incBtn, new LinearLayout.LayoutParams(tacticalSize, tacticalSize));
        stepperCard.addView(stepperControls);
        root.addView(stepperCard);

        TextView presetTitle = new TextView(this);
        presetTitle.setText("🔋 PRESET OPTIMIZATION NODES");
        presetTitle.setTextColor(getSecondaryTextColor());
        presetTitle.setPadding(0, 40, 0, 20);
        root.addView(presetTitle);

        LinearLayout presetCard = new LinearLayout(this);
        presetCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, presetCard, "CARD_STANDARD");

        final long[] vals = {60000L, 30000L, 15000L, 7000L, 1000L};
        final String[] names = {"ECO (60s)", "BALANCED (30s)", "ACTIVE (15s)", "HIGH (7s)", "EXTREME (1s)"};
        final java.util.List<android.widget.Switch> switches = new java.util.ArrayList<>();

        for (int i = 0; i < vals.length; i++) {
            final long v = vals[i];
            final String n = names[i];
            android.widget.Switch sw = new android.widget.Switch(this);
            sw.setText(n);
            sw.setTextColor(getTextColor());
            sw.setPadding(20, 20, 20, 20);
            sw.setChecked(!masterSwitch.isChecked() && prefs.getLong("pref_tracking_interval", 15000L) == v);
            switches.add(sw);
            sw.setOnClickListener(view -> {
                if (masterSwitch.isChecked()) {
                    sw.setChecked(false);
                    return;
                }
                for (android.widget.Switch s : switches) s.setChecked(false);
                sw.setChecked(true);
                RegistryManager.setTrackingInterval(this, v);
                if (tvPingIntervalVal != null) {
                    tvPingIntervalVal.setText("Active Cadence: " + (v / 1000) + "s | " + n);
                }
            });
            presetCard.addView(sw);
        }
        root.addView(presetCard);

        masterSwitch.setOnCheckedChangeListener((b, isChecked) -> {
            prefs.edit().putBoolean("pref_stepper_active", isChecked).apply();
            stepperControls.setAlpha(isChecked ? 1.0f : 0.3f);
            if (isChecked) {
                for (android.widget.Switch s : switches) s.setChecked(false);
                long val = Long.parseLong(valTv.getText().toString().replace("s", "")) * 1000;
                RegistryManager.setTrackingInterval(this, val);
                if (tvPingIntervalVal != null) {
                    tvPingIntervalVal.setText("Active Cadence: " + (val / 1000) + "s | Manual Stepper");
                }
            }
        });

        LinearLayout.LayoutParams oemBtnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        oemBtnLp.setMargins(0, 25, 0, 10);
        View oemWhitelistBtn = createIconButton("⚡ CROSS-OEM AUTOSTART & BATTERY WHITELIST", com.jdroidx.jfindme.R.drawable.ic_device_health, v -> {
            dialog.dismiss();
            com.jdroidx.jfindme.utils.MiuiOptimizationHelper.INSTANCE.launchBatteryOptimizationSettings(SettingsActivity.this);
        });
        root.addView(oemWhitelistBtn, oemBtnLp);

        dialog.setContentView(root);
        dialog.show();

        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setAttributes(lp);
        }
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
            if (values[i] == currentTtl) {
                selectedIdx = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Pairing PIN Expiry TTL")
            .setSingleChoiceItems(options, selectedIdx, (dialog, which) -> {
                int chosen = values[which];
                prefs.edit().putInt("pairing_pin_ttl_minutes", chosen).apply();
                if (tvPinTtlVal != null) {
                    tvPinTtlVal.setText("TTL: " + chosen + " Min (NIST SP 800-63B pairing countdown)");
                }
                UiArchitect.showSlideNotificationBanner(this, "PIN TTL Saved", "Pairing PIN Expiry TTL set to " + chosen + " minutes.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
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
        int selectedIdx = 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentMs) {
                selectedIdx = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Perimeter Jitter Filter (Anti-Flutter)")
            .setSingleChoiceItems(options, selectedIdx, (dialog, which) -> {
                long chosenMs = values[which];
                RegistryManager.setGeofenceJitterDebounceMs(this, chosenMs);
                if (tvJitterVal != null) {
                    tvJitterVal.setText("Debounce: " + (chosenMs / 1000) + "s (Temporal boundary anti-flutter)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Jitter Filter Updated", "Debounce threshold set to " + (chosenMs / 1000) + "s", false, 2500, null, null);
                dialog.dismiss();
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
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📍 Minimum Displacement Threshold")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                float chosen = values[which];
                RegistryManager.setMinDisplacementMeters(this, chosen);
                if (tvDisplacementVal != null) {
                    tvDisplacementVal.setText("Displacement: " + (int) chosen + "m (Suppresses stationary GPS jitter)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Displacement Updated", "Displacement threshold set to " + (int) chosen + "m", false, 2500, null, null);
                dialog.dismiss();
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
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🟡 Low Battery Warning Level")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setLowBatteryThreshold(this, chosen);
                if (tvLowBatteryVal != null) {
                    tvLowBatteryVal.setText("Threshold: " + chosen + "% (Triggers amber distress telemetry)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Low Battery Threshold", "Warning trigger set to " + chosen + "%", false, 2500, null, null);
                dialog.dismiss();
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
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🟡 Inactive Stale Threshold (Amber)")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setStaleInactiveMinutes(this, chosen);
                if (tvStaleVal != null) {
                    tvStaleVal.setText("Stale Window: " + chosen + " Min (Marks node amber inactive)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Stale Threshold Updated", "Amber warning set to " + chosen + " minutes", false, 2500, null, null);
                dialog.dismiss();
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
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🔴 Critical Offline Threshold (Red)")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setCriticalOfflineHours(this, chosen);
                if (tvCriticalOfflineVal != null) {
                    tvCriticalOfflineVal.setText("Offline Window: " + chosen + " Hours (Marks node red offline)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Offline Threshold Updated", "Red alert trigger set to " + chosen + " hours", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showSirenDurationDialog() {
        int current = RegistryManager.getSirenDurationSec(this);
        int[] values = {15, 30, 60, 120};
        String customLabel = "✏️ Custom Duration (" + current + "s)...";
        String[] options = {
            "15 Seconds (Brief Alert)",
            "30 Seconds (Standard - Recommended / Default)",
            "60 Seconds (Extended Siren)",
            "120 Seconds (Maximum Distress)",
            customLabel
        };
        int selected = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🔊 Siren Auto-Silence Duration")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("🔊 Custom Siren Duration", "Seconds (5 - 600)", 5, 600, current, "s", chosen -> {
                        RegistryManager.setSirenDurationSec(this, chosen);
                        if (tvSirenDurationVal != null) {
                            tvSirenDurationVal.setText("Duration: " + chosen + "s (Acoustic horn auto-silence timer)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Siren Duration Updated", "Acoustic alarm set to " + chosen + " seconds", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setSirenDurationSec(this, chosen);
                    if (tvSirenDurationVal != null) {
                        tvSirenDurationVal.setText("Duration: " + chosen + "s (Acoustic horn auto-silence timer)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Siren Duration Updated", "Acoustic alarm set to " + chosen + " seconds", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showSosCountdownDialog() {
        int current = RegistryManager.getSosCountdownSec(this);
        int[] values = {1, 3, 5, 10, 15};
        String customLabel = "✏️ Custom Countdown (" + current + "s)...";
        String[] options = {
            "1 Second (Minimal Delay)",
            "3 Seconds (Quick Confirm)",
            "5 Seconds (Standard - Recommended / Default)",
            "10 Seconds (Deliberate)",
            "15 Seconds (Maximum Safety)",
            customLabel
        };
        int selected = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Emergency SOS Confirmation Countdown")
            .setMessage("Time remaining after tap until broadcast begins. During this window, user can CANCEL the trigger.")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("⏱️ Custom SOS Countdown", "Seconds (0 - 60)", 0, 60, current, "s", chosen -> {
                        RegistryManager.setSosCountdownSec(this, chosen);
                        if (tvSosCountdownVal != null) {
                            tvSosCountdownVal.setText("Countdown: " + chosen + "s (Emergency abort cancellation window)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Countdown Updated", "SOS confirmation timer: " + chosen + " seconds", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setSosCountdownSec(this, chosen);
                    if (tvSosCountdownVal != null) {
                        tvSosCountdownVal.setText("Countdown: " + chosen + "s (Emergency abort cancellation window)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Countdown Updated", "SOS confirmation timer: " + chosen + " seconds", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showSosCooldownDialog() {
        int current = RegistryManager.getSosCooldownSec(this);
        int[] values = {10, 30, 60, 120, 300};
        String customLabel = "✏️ Custom Cooldown (" + current + "s)...";
        String[] options = {
            "10 Seconds (Quick Re-trigger)",
            "30 Seconds (Balanced)",
            "60 Seconds (Standard - Recommended / Default)",
            "120 Seconds (Conservative)",
            "300 Seconds (Maximum Protection)",
            customLabel
        };
        int selected = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🔒 SOS Cooldown Window")
            .setMessage("Minimum time between consecutive SOS broadcasts. Prevents accidental duplicate triggers.")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("🔒 Custom SOS Cooldown", "Seconds (5 - 600)", 5, 600, current, "s", chosen -> {
                        RegistryManager.setSosCooldownSec(this, chosen);
                        if (tvSosCooldownVal != null) {
                            tvSosCooldownVal.setText("Cooldown: " + chosen + "s (Anti-duplicate trigger lockout)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Cooldown Updated", "SOS cooldown: " + chosen + " seconds", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setSosCooldownSec(this, chosen);
                    if (tvSosCooldownVal != null) {
                        tvSosCooldownVal.setText("Cooldown: " + chosen + "s (Anti-duplicate trigger lockout)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Cooldown Updated", "SOS cooldown: " + chosen + " seconds", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showSosAckTimeoutDialog() {
        int currentSec = RegistryManager.getSosAckTimeoutSec(this);
        int[] minutes = {1, 5, 10, 15, 30};
        String customLabel = "✏️ Custom Timeout (" + (currentSec / 60) + " Min)...";
        String[] labels = {"1 Minute", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes (Default)", customLabel};
        int selected = labels.length - 1;
        for (int i = 0; i < minutes.length; i++) {
            if (minutes[i] * 60 == currentSec) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("👤 SOS Acknowledgment Timeout")
            .setMessage("How long before guardian acknowledgment is considered 'missed'. Guardian sees this as a reminder to confirm receipt.")
            .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == labels.length - 1) {
                    showCustomNumericInputDialog("👤 Custom SOS ACK Timeout", "Minutes (1 - 180)", 1, 180, currentSec / 60, "Min", chosenMins -> {
                        int chosenSec = chosenMins * 60;
                        RegistryManager.setSosAckTimeoutSec(this, chosenSec);
                        if (tvSosAckVal != null) {
                            tvSosAckVal.setText("Timeout: " + chosenMins + " Min (Guardian acknowledgment reminder)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "ACK Timeout Updated", "Guardian acknowledgment timeout: " + chosenMins + " minute(s)", false, 2500, null, null);
                    });
                } else {
                    int chosenSec = minutes[which] * 60;
                    RegistryManager.setSosAckTimeoutSec(this, chosenSec);
                    if (tvSosAckVal != null) {
                        tvSosAckVal.setText("Timeout: " + (chosenSec / 60) + " Min (Guardian acknowledgment reminder)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "ACK Timeout Updated", "Guardian acknowledgment timeout: " + minutes[which] + " minute(s)", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showSosFanoutScopeDialog() {
        String current = RegistryManager.getSosFanoutScope(this);
        String[] options = {"All Contacts (Global Broadcast)", "Group Only (Team/Family)", "Family Only (Ward Mode)"};
        int selected = 0;
        if ("group_only".equals(current)) selected = 1;
        else if ("family_only".equals(current)) selected = 2;

        new android.app.AlertDialog.Builder(this)
            .setTitle("📡 SOS Fan-Out Scope")
            .setMessage("Control which contacts receive the emergency alert when SOS is triggered.\n\n• All Contacts: Everyone in your network\n• Group Only: Current group members\n• Family Only: Ward/family mode only")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                String scope = which == 0 ? "all_contacts" : (which == 1 ? "group_only" : "family_only");
                RegistryManager.setSosFanoutScope(this, scope);
                if (tvSosFanoutVal != null) {
                    tvSosFanoutVal.setText("Scope: " + scope.toUpperCase() + " (Contact distribution filter)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Fan-out Scope Updated", "SOS alerts will target: " + options[which], false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showOfflineQueueCapacityDialog() {
        int current = RegistryManager.getOfflineQueueMaxPoints(this);
        int[] values = {100, 250, 500, 1000};
        String customLabel = "✏️ Custom Capacity (" + current + " Points)...";
        String[] options = {
            "100 Points (~1 Hour @ 30s interval)",
            "250 Points (~2-3 Hours @ 30s interval)",
            "500 Points (Standard - Recommended / Default)",
            "1000 Points (Forensic Extended Queue)",
            customLabel
        };
        int selected = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📦 Offline GPS Vault Capacity")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("📦 Custom Vault Capacity", "Points (50 - 5000)", 50, 5000, current, "Points", chosen -> {
                        RegistryManager.setOfflineQueueMaxPoints(this, chosen);
                        if (tvOfflineQueueVal != null) {
                            tvOfflineQueueVal.setText("Buffer Capacity: " + chosen + " Points (Circular offline queue)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Vault Capacity Updated", "Max offline buffer set to " + chosen + " points", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setOfflineQueueMaxPoints(this, chosen);
                    if (tvOfflineQueueVal != null) {
                        tvOfflineQueueVal.setText("Buffer Capacity: " + chosen + " Points (Circular offline queue)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Vault Capacity Updated", "Max offline buffer set to " + chosen + " points", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // SHA-256 Masked Configuration Dialog: Saves updated purge phrase through cryptographic hash pipeline
    private void showPurgeSafetyPhraseDialog() {
        UiArchitect.showInputPromptDialog(this,
            "Master Purge Safety Password",
            "Configure administrative override password for force purging unlinked nodes:",
            "",
            newPhrase -> {
                if (newPhrase != null && newPhrase.trim().length() >= 4) {
                    RegistryManager.setPurgeSafetyPhrase(this, newPhrase.trim());
                    if (tvPurgePhraseVal != null) {
                        tvPurgePhraseVal.setText("Passphrase: ●●●●●●●● (Encrypted) (Required for factory wipe)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Safety Phrase Updated", "Master purge override password saved securely.", false, 3000, null, null);
                } else {
                    UiArchitect.showSlideNotificationBanner(this, "Invalid Phrase", "Safety phrase must contain at least 4 characters.", true, 3000, null, null);
                }
            }
        );
    }

    private void showClosedLoopAckTimeoutDialog() {
        long current = RegistryManager.getClosedLoopAckTimeoutMs(this);
        long[] values = {2000L, 3000L, 5000L, 8000L, 10000L};
        String[] options = {
            "2000 ms (Aggressive Failover • High Sensitivity)",
            "3000 ms (Rapid Adaptive Failover)",
            "5000 ms (Recommended Standard Default)",
            "8000 ms (Tolerant • High-Jitter Cell Tower)",
            "10000 ms (Extended • Slow 2G/3G Cellular)"
        };
        int selected = 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Closed-Loop Server ACK Timeout")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                long chosen = values[which];
                RegistryManager.setClosedLoopAckTimeoutMs(this, chosen);
                if (tvClosedLoopTimeoutVal != null) {
                    tvClosedLoopTimeoutVal.setText("Timeout: " + chosen + "ms (Server ACK round-trip deadline before failover)");
                }
                UiArchitect.showSlideNotificationBanner(this, "ACK Timeout Mutated", "Closed-loop timeout set to " + chosen + "ms", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showMaxAckFailuresDialog() {
        int current = RegistryManager.getMaxAckFailures(this);
        int[] values = {1, 2, 3, 5};
        String[] options = {
            "1 Missed ACK (Instant Trip • Zero Tolerance)",
            "2 Missed ACKs (Recommended • Balanced Circuit Breaker)",
            "3 Missed ACKs (Moderate Tolerance)",
            "5 Missed ACKs (High Tolerance • Flaky Coverage)"
        };
        int selected = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⚡ Circuit Breaker Failure Threshold")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setMaxAckFailures(this, chosen);
                if (tvMaxAckFailuresVal != null) {
                    tvMaxAckFailuresVal.setText("Threshold: " + chosen + " Timeouts (Circuit breaker threshold before route switch)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Failure Threshold Mutated", "Route fails over after " + chosen + " consecutive missed ACKs.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showFailoverSmsDialog() {
        String currentSms = RegistryManager.getFailoverSmsTarget(this);
        UiArchitect.showInputPromptDialog(this,
            "Baseband SOS SMS Beacon Target",
            "Enter emergency phone number to receive zero-data SMS beacon telemetry when data is blocked (Level N3 SOS Failover):",
            currentSms != null ? currentSms : "",
            newNumber -> {
                if (newNumber != null) {
                    String clean = newNumber.trim();
                    RegistryManager.setFailoverSmsTarget(this, clean);
                    if (tvFailoverSmsVal != null) {
                        tvFailoverSmsVal.setText(!clean.isEmpty() ? "Recipient: " + clean + " (Level N3 zero-data beacon)" : "Not Configured (Tap to set Emergency SOS SMS Number)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "SOS SMS Target Saved", !clean.isEmpty() ? "Failover SMS recipient set to: " + clean : "Failover SMS beacon disabled.", false, 3000, null, null);
                }
            }
        );
    }

    private void showGuardianMasterPinDialog() {
        String currentPin = RegistryManager.getGuardianPin(this);
        boolean hasPin = currentPin != null && !currentPin.trim().isEmpty();
        String prompt = hasPin
            ? "Configure Master Guardian PIN (4 to 6 numeric digits). Required to pause tracking or toggle Child Mode:"
            : "Establish new Master Guardian PIN (4 to 6 numeric digits). Protects device from unauthorized location shutdown:";

        UiArchitect.showInputPromptDialog(this,
            "Guardian Master Security PIN",
            prompt,
            hasPin ? currentPin : "",
            newPin -> {
                if (newPin != null && newPin.trim().length() >= 4 && newPin.trim().matches("^[0-9]+$")) {
                    RegistryManager.setGuardianPin(this, newPin.trim());
                    if (tvGuardianPinVal != null) {
                        tvGuardianPinVal.setText("●●●● (PIN Configured & Active)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Guardian PIN Updated", "Master Guardian PIN saved successfully.", false, 3000, null, null);
                } else {
                    UiArchitect.showSlideNotificationBanner(this, "Invalid PIN", "PIN must be 4 to 6 numeric digits.", true, 3000, null, null);
                }
            }
        );
    }

    private void showLocationPauseDurationDialog() {
        int current = RegistryManager.getLocationPauseDurationMin(this);
        int[] values = {15, 30, 60, 120};
        String customLabel = "✏️ Custom Duration (" + current + " Min)...";
        String[] options = {
            "15 Minutes (Brief Standby)",
            "30 Minutes (Standard / Recommended)",
            "60 Minutes (1 Hour Exemption)",
            "120 Minutes (2 Hours Extended)",
            customLabel
        };
        int selectedIdx = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selectedIdx = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Location Pause Duration (Approval Window)")
            .setSingleChoiceItems(options, selectedIdx, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("⏱️ Custom Location Pause Window", "Minutes (5 - 1440)", 5, 1440, current, "Min", chosen -> {
                        RegistryManager.setLocationPauseDurationMin(this, chosen);
                        if (tvPauseDurationVal != null) {
                            tvPauseDurationVal.setText("Window: " + chosen + " Min (Standby pause duration before auto-resume)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Pause Window Updated", "Guardian approval window set to " + chosen + " minutes.", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setLocationPauseDurationMin(this, chosen);
                    if (tvPauseDurationVal != null) {
                        tvPauseDurationVal.setText("Window: " + chosen + " Min (Standby pause duration before auto-resume)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Pause Window Updated", "Guardian approval window set to " + chosen + " minutes.", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
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
            if (values[i][0] == currStill) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🏃 Motion & Battery Duty Cycles")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                long[] chosen = values[which];
                RegistryManager.setMotionStillIntervalMs(this, chosen[0]);
                RegistryManager.setMotionVehicleIntervalMs(this, chosen[1]);
                RegistryManager.setMotionWalkingIntervalMs(this, chosen[2]);
                if (tvMotionDutyVal != null) {
                    tvMotionDutyVal.setText("Still: " + (chosen[0] / 1000) + "s | Car: " + (chosen[1] / 1000) + "s | Walk: " + (chosen[2] / 1000) + "s");
                }
                UiArchitect.showSlideNotificationBanner(this, "Duty Cycles Saved", "Still: " + (chosen[0] / 1000) + "s, Car: " + (chosen[1] / 1000) + "s, Walk: " + (chosen[2] / 1000) + "s", false, 3000, null, null);
                dialog.dismiss();
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
            if (Math.abs(values[i][0] - currEma) < 0.05f) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🛣️ Road Smoothing & Kalman Sensitivity")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                float[] chosen = values[which];
                RegistryManager.setHeadingEmaFactor(this, chosen[0]);
                RegistryManager.setKalmanMeasurementNoise(this, chosen[1]);
                if (tvRoadSmoothingVal != null) {
                    tvRoadSmoothingVal.setText("Heading EMA: " + chosen[0] + " | Measurement Noise R: " + (int) chosen[1]);
                }
                UiArchitect.showSlideNotificationBanner(this, "Sensitivity Updated", "Heading EMA: " + chosen[0] + ", Noise R: " + chosen[1], false, 3000, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showRadarProximityRadiusDialog() {
        int current = RegistryManager.getRadarProximityRadiusMeters(this);
        int[] values = {50, 100, 200, 500};
        String customLabel = "✏️ Custom Radius (" + current + "m)...";
        String[] options = {
            "50m (Immediate Perimeter)",
            "100m (Standard Radar — Default)",
            "200m (Extended Proximity)",
            "500m (Wide Area Radar)",
            customLabel
        };
        int selected = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📡 Radar Proximity Radius")
            .setMessage("Sets the proximity distance threshold (in meters). Devices beyond this radius are highlighted in sky blue in the Tactical Radar scanner.")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("📡 Custom Radar Radius", "Meters (10 - 5000)", 10, 5000, current, "m", chosen -> {
                        RegistryManager.setRadarProximityRadiusMeters(this, chosen);
                        if (tvRadarRadiusVal != null) {
                            tvRadarRadiusVal.setText("Radar Proximity Radius: " + chosen + "m (Sky blue highlight threshold)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Radar Radius Updated", "Radar highlights external nodes at " + chosen + "m", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setRadarProximityRadiusMeters(this, chosen);
                    if (tvRadarRadiusVal != null) {
                        tvRadarRadiusVal.setText("Radar Proximity Radius: " + chosen + "m (Sky blue highlight threshold)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Radar Radius Updated", "Radar highlights external nodes at " + chosen + "m", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showRemoteLocateTimeoutDialog() {
        long currentMs = RegistryManager.getRemoteLocateTimeoutMs(this);
        int currentSec = (int) (currentMs / 1000);
        int[] values = {10, 15, 20, 30, 45, 60};
        String customLabel = "✏️ Custom Timeout (" + currentSec + "s)...";
        String[] options = {
            "10s (High Speed / Active Networks)",
            "15s (Aggressive Handshake)",
            "20s (Standard / Balanced — Recommended)",
            "30s (Extended Satellite Acquisition)",
            "45s (Deep Sleep / Power Saver Devices)",
            "60s (Maximum Forensic Window)",
            customLabel
        };
        int selected = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentSec) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🛰️ Remote Locate Timeout")
            .setMessage("Sets the duration (in seconds) the application waits for a remote target device to wake its GNSS co-processor and return a fresh GPS fix before falling back to cached telemetry.")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("🛰️ Custom Remote Locate Timeout", "Seconds (5 - 120)", 5, 120, currentSec, "s", chosen -> {
                        RegistryManager.setRemoteLocateTimeoutMs(this, chosen * 1000L);
                        if (tvRemoteLocateTimeoutVal != null) {
                            tvRemoteLocateTimeoutVal.setText("Remote Locate Timeout: " + chosen + "s (Maximum GNSS response window)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Locate Timeout Updated", "Remote GPS handshake deadline set to " + chosen + "s", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setRemoteLocateTimeoutMs(this, chosen * 1000L);
                    if (tvRemoteLocateTimeoutVal != null) {
                        tvRemoteLocateTimeoutVal.setText("Remote Locate Timeout: " + chosen + "s (Maximum GNSS response window)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Locate Timeout Updated", "Remote GPS handshake deadline set to " + chosen + "s", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showDistantDeviceThresholdDialog() {
        int current = RegistryManager.getDistantDeviceThresholdMeters(this);
        int[] values = {100, 200, 500, 1000};
        String customLabel = "✏️ Custom Distance (" + current + "m)...";
        String[] options = {
            "100m (Close Proximity — Tight Clustering)",
            "200m (Standard — Recommended / Default)",
            "500m (Wide Area — Campus / Fleet)",
            "1000m (Extended — City-Scale)",
            customLabel
        };
        int selected = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📡 Distant Device Panel Threshold")
            .setMessage("Devices farther than this distance from your location appear in the floating Distant Device Panel on the map. Closer devices show as direct map markers.")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("📡 Custom Distant Device Threshold", "Meters (20 - 50000)", 20, 50000, current, "m", chosen -> {
                        RegistryManager.setDistantDeviceThresholdMeters(this, chosen);
                        if (tvDistantVal != null) {
                            tvDistantVal.setText("Panel Threshold: " + chosen + "m (Distance to float HUD panel)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Distant Threshold Updated", "Floating panel activates at " + chosen + "m distance", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setDistantDeviceThresholdMeters(this, chosen);
                    if (tvDistantVal != null) {
                        tvDistantVal.setText("Panel Threshold: " + chosen + "m (Distance to float HUD panel)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Distant Threshold Updated", "Floating panel activates at " + chosen + "m distance", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showDefaultRadiusDialog() {
        int current = RegistryManager.getDefaultSafeZoneRadiusMeters(this);
        int[] values = {50, 100, 150, 250, 500, 1000};
        String customLabel = "✏️ Custom Radius (" + current + "m)...";
        String[] options = {
            "50m (Micro-Perimeter — Residence / House)",
            "100m (Standard — Recommended / Default)",
            "150m (Neighborhood / Small Park)",
            "250m (School / Corporate Campus)",
            "500m (Industrial Zone / Facility)",
            "1000m (City District / Transit Hub)",
            customLabel
        };
        int selected = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🎯 Default Safe Zone Radius")
            .setMessage("Sets the baseline perimeter radius (in meters) automatically assigned when creating new safe zones or missions.")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("🎯 Custom Safe Zone Radius", "Meters (10 - 5000)", 10, 5000, current, "m", chosen -> {
                        RegistryManager.setDefaultSafeZoneRadiusMeters(this, chosen);
                        if (tvRadiusVal != null) {
                            tvRadiusVal.setText("Default Radius: " + chosen + "m (Base safe zone perimeter)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Default Radius Updated", "New safe zones default to " + chosen + "m", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setDefaultSafeZoneRadiusMeters(this, chosen);
                    if (tvRadiusVal != null) {
                        tvRadiusVal.setText("Default Radius: " + chosen + "m (Base safe zone perimeter)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Default Radius Updated", "New safe zones default to " + chosen + "m", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showGeocodeCacheCapDialog() {
        int currentCap = RegistryManager.getMaxGeocodeCacheSize(this);
        String[] options = {
            "⚡ 1,000 Entries (Ultra Compact • Low Memory)",
            "⚡ 2,500 Entries (Balanced Efficiency)",
            "🛡️ 5,000 Entries (Standard Recommended)",
            "📅 10,000 Entries (High Density Cache)",
            "📅 20,000 Entries (Maximum Cache Capacity)"
        };
        int selected = -1;
        if (currentCap == 1000) selected = 0;
        else if (currentCap == 2500) selected = 1;
        else if (currentCap == 5000) selected = 2;
        else if (currentCap == 10000) selected = 3;
        else if (currentCap == 20000) selected = 4;

        new android.app.AlertDialog.Builder(this)
            .setTitle("🌐 Geocode Cache Capacity")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int[] capsArr = {1000, 2500, 5000, 10000, 20000};
                int chosen = capsArr[which];
                RegistryManager.setMaxGeocodeCacheSize(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Cache Cap Updated", "Reverse geocode cache limit set to " + chosen + " entries.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    // [Variables]: actions, dialog, which
    // [Constants]: Dialog action labels
    // [Dependencies]: RegistryManager, AlertDialog
    // [Integration Points]: DatabaseHelper.setDynamicConfig, SettingsActivity Card 7
    private void showDatabaseEngineTtlDialog() {
        String[] actions = {
            "⏳ Sync Queue TTL (Current: " + RegistryManager.getSyncQueueTtlHours(this) + "h)",
            "📋 Diagnostic SysLog TTL (Current: " + RegistryManager.getSysLogTtlHours(this) + "h)",
            "📡 P2P Mesh Packets TTL (Current: " + RegistryManager.getP2pMeshTtlHours(this) + "h)",
            "✉️ Pairing Invitations TTL (Current: " + RegistryManager.getInvitationTtlDays(this) + "d)",
            "🐕 Watchdog Resurrection TTL (Current: " + RegistryManager.getWatchdogLogTtlDays(this) + "d)",
            "🚨 Alert & Distress Ledger TTL (Current: " + RegistryManager.getAlertHistoryTtlDays(this) + "d)",
            "📄 Default Explorer Page Limit (Current: " + RegistryManager.getDefaultPageLimit(this) + " rows)"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Database Engine TTL & Query Limits")
            .setItems(actions, (dialog, which) -> {
                dialog.dismiss();
                switch (which) {
                    case 0: showSyncQueueTtlDialog(); break;
                    case 1: showSysLogTtlDialog(); break;
                    case 2: showP2pMeshTtlDialog(); break;
                    case 3: showInvitationTtlDialog(); break;
                    case 4: showWatchdogTtlDialog(); break;
                    case 5: showAlertHistoryTtlDialog(); break;
                    case 6: showDefaultPageLimitDialog(); break;
                }
            })
            .setNegativeButton("CLOSE", null)
            .show();
    }

    private void showSyncQueueTtlDialog() {
        int current = RegistryManager.getSyncQueueTtlHours(this);
        String[] options = {"⚡ 12 Hours", "⚡ 24 Hours (1 Day)", "🛡️ 48 Hours (2 Days • Default)", "📅 72 Hours (3 Days)", "📅 168 Hours (7 Days)"};
        int[] vals = {12, 24, 48, 72, 168};
        int selected = -1;
        for (int i = 0; i < vals.length; i++) if (vals[i] == current) { selected = i; break; }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏳ Sync Queue Retention TTL")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = vals[which];
                RegistryManager.setSyncQueueTtlHours(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Sync TTL Updated", "Pending sync queue retention set to " + chosen + " hours.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showSysLogTtlDialog() {
        int current = RegistryManager.getSysLogTtlHours(this);
        String[] options = {"⚡ 12 Hours", "⚡ 24 Hours (1 Day)", "🛡️ 48 Hours (2 Days • Default)", "📅 72 Hours (3 Days)", "📅 168 Hours (7 Days)"};
        int[] vals = {12, 24, 48, 72, 168};
        int selected = -1;
        for (int i = 0; i < vals.length; i++) if (vals[i] == current) { selected = i; break; }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📋 System Diagnostic Log TTL")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = vals[which];
                RegistryManager.setSysLogTtlHours(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "SysLog TTL Updated", "Diagnostic system log TTL set to " + chosen + " hours.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showP2pMeshTtlDialog() {
        int current = RegistryManager.getP2pMeshTtlHours(this);
        String[] options = {"⚡ 6 Hours", "⚡ 12 Hours", "🛡️ 24 Hours (1 Day • Default)", "📅 48 Hours (2 Days)"};
        int[] vals = {6, 12, 24, 48};
        int selected = -1;
        for (int i = 0; i < vals.length; i++) if (vals[i] == current) { selected = i; break; }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📡 P2P Mesh Packets TTL")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = vals[which];
                RegistryManager.setP2pMeshTtlHours(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Mesh TTL Updated", "P2P mesh packet retention set to " + chosen + " hours.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showInvitationTtlDialog() {
        int current = RegistryManager.getInvitationTtlDays(this);
        String[] options = {"⚡ 1 Day", "⚡ 3 Days", "🛡️ 7 Days (1 Week • Default)", "📅 14 Days (2 Weeks)", "📅 30 Days (1 Month)"};
        int[] vals = {1, 3, 7, 14, 30};
        int selected = -1;
        for (int i = 0; i < vals.length; i++) if (vals[i] == current) { selected = i; break; }

        new android.app.AlertDialog.Builder(this)
            .setTitle("✉️ Pairing Invitation TTL")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = vals[which];
                RegistryManager.setInvitationTtlDays(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Invitation TTL Updated", "Pairing invitation expiry set to " + chosen + " days.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showWatchdogTtlDialog() {
        int current = RegistryManager.getWatchdogLogTtlDays(this);
        String[] options = {"⚡ 1 Day", "⚡ 3 Days", "🛡️ 7 Days (1 Week • Default)", "📅 14 Days (2 Weeks)", "📅 30 Days (1 Month)"};
        int[] vals = {1, 3, 7, 14, 30};
        int selected = -1;
        for (int i = 0; i < vals.length; i++) if (vals[i] == current) { selected = i; break; }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🐕 Watchdog Resurrection TTL")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = vals[which];
                RegistryManager.setWatchdogLogTtlDays(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Watchdog TTL Updated", "Daemon resurrection log TTL set to " + chosen + " days.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showAlertHistoryTtlDialog() {
        int current = RegistryManager.getAlertHistoryTtlDays(this);
        String[] options = {"⚡ 7 Days (1 Week)", "⚡ 14 Days (2 Weeks)", "🛡️ 30 Days (1 Month • Default)", "📅 60 Days (2 Months)", "📅 90 Days (3 Months)"};
        int[] vals = {7, 14, 30, 60, 90};
        int selected = -1;
        for (int i = 0; i < vals.length; i++) if (vals[i] == current) { selected = i; break; }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🚨 Alert & Distress Ledger TTL")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = vals[which];
                RegistryManager.setAlertHistoryTtlDays(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Alert TTL Updated", "Alert history ledger TTL set to " + chosen + " days.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showDefaultPageLimitDialog() {
        int current = RegistryManager.getDefaultPageLimit(this);
        String[] options = {"⚡ 25 Rows (Fastest)", "🛡️ 50 Rows (Balanced • Default)", "📅 100 Rows (Extended View)", "📅 200 Rows (Full Matrix)"};
        int[] vals = {25, 50, 100, 200};
        int selected = -1;
        for (int i = 0; i < vals.length; i++) if (vals[i] == current) { selected = i; break; }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📄 Default Explorer Page Limit")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                int chosen = vals[which];
                RegistryManager.setDefaultPageLimit(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Page Limit Updated", "Vault & Explorer default limit set to " + chosen + " rows.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    // [Variables]: current, options, vals, selected, chosen
    // [Constants]: Dialog labels
    // [Dependencies]: RegistryManager, UiArchitect, AlertDialog
    // [Integration Points]: TrackingService dead-reckoning threshold SSOT
    private void showDeadReckoningRadiusDialog() {
        float current = RegistryManager.getDeadReckoningRadiusMeters(this);
        String[] options = {
            "⚡ 5 Meters (Ultra Sensitive • High Battery/Data)",
            "⚡ 10 Meters (City Walking • Moderate Tolerance)",
            "🛡️ 15 Meters (Default Recommended • 97% Bandwidth Cut)",
            "📅 25 Meters (Suburban Road Filter • High Tolerance)",
            "📅 50 Meters (Macro Station Gate • Minimalist Data)"
        };
        float[] vals = {5.0f, 10.0f, 15.0f, 25.0f, 50.0f};
        int selected = 2;
        for (int i = 0; i < vals.length; i++) {
            if (Math.abs(vals[i] - current) < 0.1f) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🛡️ Spatial Dead-Reckoning Noise Gate")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                float chosen = vals[which];
                RegistryManager.setDeadReckoningRadiusMeters(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Dead-Reckoning Updated", "Noise gate set to " + (int)chosen + "m. Stationary GPS uploads suppressed.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    // [Variables]: current, options, vals, selected, chosen
    // [Constants]: Dialog labels
    // [Dependencies]: RegistryManager, UiArchitect, AlertDialog
    // [Integration Points]: TrackingService presence pulse keep-alive SSOT
    private void showPresencePulseIntervalDialog() {
        long current = RegistryManager.getPresencePulseIntervalMs(this);
        String[] options = {
            "⚡ 60 Seconds (1 Minute • Real-Time Dynamic Active)",
            "⚡ 120 Seconds (2 Minutes • Fast Mesh Heartbeat)",
            "🛡️ 180 Seconds (3 Minutes • Default Recommended)",
            "📅 300 Seconds (5 Minutes • Ultra Lean Bandwidth)",
            "📅 600 Seconds (10 Minutes • Minimalist Standby)"
        };
        long[] vals = {60_000L, 120_000L, 180_000L, 300_000L, 600_000L};
        int selected = 2;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("💓 Presence Pulse Heartbeat Interval")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                long chosen = vals[which];
                RegistryManager.setPresencePulseIntervalMs(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Presence Pulse Updated", "Stationary heartbeat interval set to " + (chosen / 1000) + " seconds.", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    // [Variables]: currentGroup, presetGroups, selected, chosen, input, container, lp
    // [Constants]: Preset group keys
    // [Dependencies]: RegistryManager, UiArchitect, AlertDialog
    // [Integration Points]: Database group-by hierarchy & multi-tenancy SSOT
    private void showDeviceGroupDialog() {
        String currentGroup = RegistryManager.getDeviceCorrespondingGroup(this);
        String[] presetGroups = {
            "UNIVERSAL_FLEET",
            "FAMILY_CORE",
            "ALPHA_TEAM",
            "SECURITY_PATROL",
            "LOGISTICS_TRACK",
            "✏️ Custom Group..."
        };
        int selected = -1;
        for (int i = 0; i < presetGroups.length - 1; i++) {
            if (presetGroups[i].equalsIgnoreCase(currentGroup)) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("👥 Device Corresponding Group (Group-By)")
            .setSingleChoiceItems(presetGroups, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == presetGroups.length - 1) {
                    showCustomDeviceGroupPrompt();
                } else {
                    String chosen = presetGroups[which];
                    RegistryManager.setDeviceCorrespondingGroup(this, chosen);
                    UiArchitect.showSlideNotificationBanner(this, "Device Group Updated", "Node assigned to group: " + chosen, false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCustomDeviceGroupPrompt() {
        String currentGroup = RegistryManager.getDeviceCorrespondingGroup(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setText(currentGroup);
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.leftMargin = pad;
        lp.rightMargin = pad;
        input.setLayoutParams(lp);
        container.addView(input);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Custom Device Group")
            .setMessage("Enter the organizational group or partition name:")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                String val = input.getText().toString().trim();
                if (!val.isEmpty()) {
                    RegistryManager.setDeviceCorrespondingGroup(this, val);
                    UiArchitect.showSlideNotificationBanner(this, "Device Group Updated", "Node assigned to: " + val, false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showStorageRetentionDialog() {
        int currentDays = RegistryManager.getStorageRetentionDays(this);
        String[] options = {
            "⚡ 1 Day (Ultra Compact • 24h History)",
            "⚡ 2 Days (High Efficiency • 48h History)",
            "🛡️ 3 Days (Recommended Balanced • 72h History)",
            "📅 7 Days (Standard Week • 168h History)",
            "📅 14 Days (Extended Fortnight)",
            "📅 30 Days (Maximum Monthly History)",
            "🧹 VACUUM & PURGE NOW (Force Compaction)"
        };
        int selected = -1;
        if (currentDays == 1) selected = 0;
        else if (currentDays == 2) selected = 1;
        else if (currentDays == 3) selected = 2;
        else if (currentDays == 7) selected = 3;
        else if (currentDays == 14) selected = 4;
        else if (currentDays == 30) selected = 5;

        new android.app.AlertDialog.Builder(this)
            .setTitle("🧹 Storage Retention & Compaction")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                if (which == 6) {
                    dialog.dismiss();
                    UiArchitect.showSlideNotificationBanner(this, "Compacting Database", "Executing SQLite WAL Truncate & VACUUM sweep...", false, 2500, null, null);
                    java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(getApplicationContext());
                            long deleted = db.pruneExpiredData(RegistryManager.getStorageRetentionDays(getApplicationContext()));
                            long cacheReclaimed = db.pruneCacheAndTempFiles();
                            runOnUiThread(() -> {
                                String msg = "Purged " + deleted + " records. Reclaimed " + String.format(java.util.Locale.US, "%.1f", cacheReclaimed / (1024.0 * 1024.0)) + " MB. DB Size: " + String.format(java.util.Locale.US, "%.2f", db.getDatabaseStorageSizeMb()) + " MB";
                                UiArchitect.showSlideNotificationBanner(SettingsActivity.this, "Compaction Complete", msg, false, 3500, null, null);
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> UiArchitect.showSlideNotificationBanner(SettingsActivity.this, "Compaction Failed", e.getMessage(), true, 3000, null, null));
                        }
                    });
                    return;
                }
                int[] daysArr = {1, 2, 3, 7, 14, 30};
                int chosen = daysArr[which];
                RegistryManager.setStorageRetentionDays(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Retention Updated", "Rolling history retention set to " + chosen + " day(s).", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showSparkPlanQuotaDialog() {
        String currentTier = RegistryManager.getFirebasePlanTier(this);
        int dailyMb = RegistryManager.getSparkDailyDownloadMb(this);
        int monthlyGb = RegistryManager.getSparkMonthlyDownloadGb(this);
        String resetIst = RegistryManager.getSparkDailyResetTimeIst(this);

        String infoMessage = "📊 FIREBASE REALTIME DATABASE QUOTA SENTINEL\n\n"
            + "• Plan Tier: " + currentTier + " Plan (Free Tier)\n"
            + "• Daily Download Quota: " + dailyMb + " MB / day\n"
            + "  - Reset Time: Midnight Pacific Time (PT)\n"
            + "  - In IST: ~" + resetIst + " every day\n"
            + "  - Automatic Resume: Access resumes automatically after daily reset.\n\n"
            + "• Monthly Download Quota: " + monthlyGb + " GB / month\n"
            + "  - Reset Time: 1st day of next calendar month (00:00 UTC)\n"
            + "  - Fallback: Upgrade to Blaze (Pay-as-you-go) if 10 GB limit exceeded.\n\n"
            + "• Lean Payload Guard: Active (<80 bytes/push)\n"
            + "• Stationary Deduplication: Active (10m / 30s threshold)\n";

        String[] actions = {
            "⚡ Switch Tier: " + (currentTier.equals("SPARK") ? "BLAZE (Pay-as-you-go)" : "SPARK (Free)"),
            "⚙️ Edit Daily Download Limit (Current: " + dailyMb + " MB)",
            "⚙️ Edit Monthly Download Limit (Current: " + monthlyGb + " GB)",
            "ℹ️ Detailed Firebase Plan Rules & Documentation"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("☁️ Firebase Spark Plan & Quotas")
            .setMessage(infoMessage)
            .setItems(actions, (d, which) -> {
                if (which == 0) {
                    String newTier = currentTier.equals("SPARK") ? "BLAZE" : "SPARK";
                    RegistryManager.setFirebasePlanTier(this, newTier);
                    UiArchitect.showSlideNotificationBanner(this, "Firebase Tier Updated", "Configured Plan Tier: " + newTier, false, 2500, null, null);
                } else if (which == 1) {
                    showEditDailyLimitDialog();
                } else if (which == 2) {
                    showEditMonthlyLimitDialog();
                } else if (which == 3) {
                    UiArchitect.showSlideNotificationBanner(this, "Spark Rules", "Daily resets ~12:30 PM IST. Monthly resets 1st of month.", false, 3500, null, null);
                }
            })
            .setPositiveButton("OK", null)
            .show();
    }

    private void showEditDailyLimitDialog() {
        String[] options = {"100 MB (Ultra Conservative)", "200 MB (Balanced)", "360 MB (Official Spark Cap - Default)", "500 MB (Extended)", "1000 MB (1 GB)"};
        int[] vals = {100, 200, 360, 500, 1000};
        int curr = RegistryManager.getSparkDailyDownloadMb(this);
        int sel = 2;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == curr) { sel = i; break; }
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Daily Download Quota (MB)")
            .setSingleChoiceItems(options, sel, (dialog, which) -> {
                int chosen = vals[which];
                RegistryManager.setSparkDailyDownloadMb(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Daily Limit Updated", "Daily quota cap set to " + chosen + " MB", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showEditMonthlyLimitDialog() {
        String[] options = {"5 GB (Conservative)", "10 GB (Official Spark Cap - Default)", "15 GB (Extended)", "25 GB (Heavy)"};
        int[] vals = {5, 10, 15, 25};
        int curr = RegistryManager.getSparkMonthlyDownloadGb(this);
        int sel = 1;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == curr) { sel = i; break; }
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Monthly Download Quota (GB)")
            .setSingleChoiceItems(options, sel, (dialog, which) -> {
                int chosen = vals[which];
                RegistryManager.setSparkMonthlyDownloadGb(this, chosen);
                UiArchitect.showSlideNotificationBanner(this, "Monthly Limit Updated", "Monthly quota cap set to " + chosen + " GB", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

}
