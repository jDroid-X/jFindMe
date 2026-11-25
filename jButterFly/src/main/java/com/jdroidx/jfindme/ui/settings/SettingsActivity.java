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
import com.jdroidx.jfindme.data.repository.DeviceRepository;
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
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.google.firebase.auth.FirebaseAuth;

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
    private TextView tvMapThrottleVal;
    private TextView tvClusterSpacingVal;
    private TextView tvRadarRadiusVal;
    private TextView tvRemoteLocateTimeoutVal;
    private TextView tvSirenDurationVal;
    private TextView tvSosCountdownVal;
    private TextView tvSosCooldownVal;
    private TextView tvSosAckVal;
    private TextView tvSosFanoutVal;
    private TextView tvOfflineQueueVal;
    private TextView tvGuardianPinVal;
    private TextView tvPrimeTargetVal;
    private TextView tvPrimeTargetNameVal;
    private TextView tvCircleHostIdVal;
    private TextView tvCircleHostNameVal;
    private TextView tvActiveCircleGroupVal;
    private TextView tvLocalDeviceRoleVal;
    private TextView tvUnifiedDeviceIdSyncVal;
    private TextView tvPauseDurationVal;
    private TextView tvPinTtlVal;
    private TextView tvPurgePhraseVal;
    private TextView tvClosedLoopTimeoutVal;
    private TextView tvDeviceSharingTimeoutVal;
    private TextView tvPairingTimeoutVal;
    private TextView tvMaxAckFailuresVal;
    private TextView tvFailoverSmsVal;
    private TextView tvGoogleMapsApiKeyVal;

    // [Variables]: settingsController, settingsModel, tvSparkPlanQuotaVal, tvDeadReckoningVal, tvPresencePulseVal, tvDeviceGroupVal, tvStorageRetentionVal, tvGeocodeCacheVal, tvDatabaseEngineTtlVal
    // [Constants]: None
    // [Dependencies]: SettingsController, SettingsModel, TextView
    // [Integration Points]: Dynamic OOPS MVC Two-Way Data Binding for Cards 7, 7B, 7C
    private SettingsController settingsController;
    private SettingsModel settingsModel;
    private TextView tvSparkPlanQuotaVal;
    private TextView tvDeadReckoningVal;
    private TextView tvPresencePulseVal;
    private TextView tvDeviceGroupVal;
    private TextView tvFleetHardwareMapVal;
    private TextView tvDecommissionedAgentIdsVal;
    private TextView tvRelativeRolesMapVal;
    private TextView tvStorageRetentionVal;
    private TextView tvGeocodeCacheVal;
    private TextView tvDatabaseEngineTtlVal;
    private TextView tvButterflyArrivalRadiusVal;
    private TextView tvButterflyExpiryVal;
    private TextView tvButterflyHysteresisVal;
    private TextView tvButterflyMaxDistanceVal;
    private TextView tvButterflyInvitationTimeoutVal;
    private TextView tvButterflyOverdueGraceVal;
    private TextView tvButterflyForegroundSyncVal;
    private TextView tvButterflyMaxAvailabilityVal;
    private TextView tvButterflyNominalSpeedVal;
    private TextView tvButterflyApproachWindowVal;
    private TextView tvButterflyTacticalRadiusVal;

    // Live Badges (Tab 2 Screen Modules)
    private TextView badgeRosterTv;
    private TextView badgeZonesTv;
    private TextView badgeHistoryTv;
    private TextView badgeHealthTv;
    private TextView badgeProfileTv;

    // [Variables] permissionsHubHeaderView, tvPermissionsSummaryVal, badgePermissionsTv, permissionToggleBindings, isRefreshingPermissionToggles
    // [Constants] None | [Dependencies] ProfilePermissionsManager, SwitchCompat | [Integration Points] Card 1B Device Permissions & ACL Hub
    private View permissionsHubHeaderView;
    private TextView tvPermissionsSummaryVal;
    private TextView badgePermissionsTv;
    private boolean isRefreshingPermissionToggles = false;

    private static class PermissionToggleBinding {
        final com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionGateSpec spec;
        final androidx.appcompat.widget.SwitchCompat toggleSwitch;
        final TextView tvStatus;
        PermissionToggleBinding(
                com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionGateSpec spec,
                androidx.appcompat.widget.SwitchCompat toggleSwitch,
                TextView tvStatus) {
            this.spec = spec;
            this.toggleSwitch = toggleSwitch;
            this.tvStatus = tvStatus;
        }
    }
    private final List<PermissionToggleBinding> permissionToggleBindings = new ArrayList<>();

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
        // [Variables]: settingsController, settingsModel | [Constants]: None | [Dependencies]: SettingsController, SettingsModel | [Integration Points]: OOPS MVC Initialization
        if (settingsController == null) {
            settingsController = new SettingsController(this);
            settingsModel = settingsController.loadSettings();
        }

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

        if (getIntent() != null && getIntent().getBooleanExtra("SCROLL_TO_PERMISSIONS", false)) {
            switchTab(0);
            if (scrollUniversal != null) {
                scrollUniversal.post(() -> {
                    if (permissionsHubHeaderView != null) {
                        scrollUniversal.smoothScrollTo(0, permissionsHubHeaderView.getTop());
                    }
                });
            }
        }

        // Asynchronously load real-time database badges for Screen Modules
        loadLiveMetricsForScreens();

        return root;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionToggleStates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshPermissionToggleStates();
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
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_child_protection_master_security_priority));
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

        String primeTarget = RegistryManager.getPrimeTargetDeviceId(this);
        secCard.addView(createSettingTile("PRIMARY CHILD / WARD TARGET DEVICE", "Target ID: " + primeTarget + " (Priority 0 Monitored Ward)", com.jdroidx.jfindme.R.drawable.ic_family_friends, v -> showPrimeTargetDeviceDialog(), tv -> tvPrimeTargetVal = tv), btnLp);

        String primeName = RegistryManager.getPrimeTargetName(this);
        secCard.addView(createSettingTile("PRIMARY CHILD / WARD TARGET NAME", "Target Name: " + primeName + " (Priority 0 Monitored Ward Name)", com.jdroidx.jfindme.R.drawable.ic_user_profile, v -> showPrimeTargetNameDialog(), tv -> tvPrimeTargetNameVal = tv), btnLp);

        String hostId = RegistryManager.getCircleHostDeviceId(this);
        secCard.addView(createSettingTile("ROOT CIRCLE HOST DEVICE ID", "Host ID: " + hostId + " (Master Guardian Silicon Anchor)", com.jdroidx.jfindme.R.drawable.ic_privacy_lock, v -> showCircleHostIdDialog(), tv -> tvCircleHostIdVal = tv), btnLp);

        String hostName = RegistryManager.getCircleHostName(this);
        secCard.addView(createSettingTile("ROOT CIRCLE HOST NAME", "Host Name: " + hostName + " (Circle Authority Anchor)", com.jdroidx.jfindme.R.drawable.ic_user_profile, v -> showCircleHostNameDialog(), tv -> tvCircleHostNameVal = tv), btnLp);

        String activeCircle = RegistryManager.getActiveGroup(this);
        if (activeCircle == null || activeCircle.isEmpty() || activeCircle.equals("DEFAULT_GROUP")) activeCircle = RegistryManager.getEffectiveActiveGroup(this);
        secCard.addView(createSettingTile("PRIMARY CIRCLE GROUP PARTITION", "Active Circle: " + activeCircle + " (Shared SSOT Group)", com.jdroidx.jfindme.R.drawable.ic_family_friends, v -> showActiveCircleGroupDialog(), tv -> tvActiveCircleGroupVal = tv), btnLp);

        String localRole = RegistryManager.getRoleFamily(this, "Master Guardian");
        secCard.addView(createSettingTile("LOCAL DEVICE ROLE IN CIRCLE", "Assigned Role: " + localRole + " (Social Authority Tier)", com.jdroidx.jfindme.R.drawable.ic_user_profile, v -> showLocalDeviceRoleDialog(), tv -> tvLocalDeviceRoleVal = tv), btnLp);

        int pauseMins = RegistryManager.getLocationPauseDurationMin(this);
        secCard.addView(createSettingTile("LOCATION PAUSE APPROVAL WINDOW", "Window: " + pauseMins + " Min (Standby pause duration before auto-resume)", android.R.drawable.ic_menu_recent_history, v -> showLocationPauseDurationDialog(), tv -> tvPauseDurationVal = tv), btnLp);

        int pinTtl = prefs.getInt("pairing_pin_ttl_minutes", 5);
        secCard.addView(createSettingTile("PAIRING PIN EXPIRY TTL", "TTL: " + pinTtl + " Min (NIST SP 800-63B pairing countdown)", com.jdroidx.jfindme.R.drawable.ic_location_history, v -> showPinTtlSelectionDialog(), tv -> tvPinTtlVal = tv), btnLp);

        String purgePhrase = RegistryManager.getPurgeSafetyPhrase(this);
        String displayPhrase = purgePhrase.startsWith("SHA256:") ? "●●●●●●●● (Encrypted)" : purgePhrase;
        secCard.addView(createSettingTile("MASTER PURGE SAFETY PASSWORD", "Passphrase: " + displayPhrase + " (Required for factory wipe)", com.jdroidx.jfindme.R.drawable.ic_privacy_lock, v -> showPurgeSafetyPhraseDialog(), tv -> tvPurgePhraseVal = tv), btnLp);

        content.addView(secCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 1B: 🔐 DEVICE PERMISSIONS & ACCESS CONTROL HUB (Unified Priority 1B)
        // ══════════════════════════════════════════════════════════════════════
        buildPermissionsHubCard(content, btnLp);

        // ══════════════════════════════════════════════════════════════════════
        // CARD 2: 🛰️ GPS SAMPLING CADENCE & BATTERY DEFENSE (Priority 2)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_gps_cadence_battery_defense_priority));
        LinearLayout gpsCard = new LinearLayout(this);
        gpsCard.setOrientation(LinearLayout.VERTICAL);

        long currInterval = RegistryManager.getTrackingInterval(this) / 1000;
        String drainWarning = currInterval < 30 ? " ⚠️ HIGH BATTERY DRAIN" : "";
        String pingSub = "Active Cadence: " + currInterval + "s" + drainWarning + " | Preset/Stepper Mode";
        gpsCard.addView(createSettingTile("BATTERY OPTIMIZER & PING HUB", pingSub, com.jdroidx.jfindme.R.drawable.ic_device_health, v -> showBatteryOptimizerDialog(), tv -> tvPingIntervalVal = tv), btnLp);

        gpsCard.addView(createSettingTile("OEM AUTOSTART & BATTERY SHIELD", "Punch through MIUI, OneUI, ColorOS, OriginOS background process killing across 15+ OEMs", com.jdroidx.jfindme.R.drawable.ic_device_health, v -> {
            com.jdroidx.jfindme.engine.power.OEMPowerWhitelistManager.showOemGuidanceDialog(this);
        }, null), btnLp);

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
        long stealthMins = RegistryManager.getStealthIntervalMins(this);
        String stealthSub = "Cadence: " + stealthMins + " mins | Autonomous SIM & Alarm Sentinel";
        gpsCard.addView(createSettingTile("STEALTH DORMANT CADENCE (APP CLOSED)", stealthSub, com.jdroidx.jfindme.R.drawable.ic_settings_gears, v -> showStealthCadenceDialog(), tv -> tvStealthCadenceVal = tv), btnLp);

        content.addView(gpsCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 3: 🛣️ ROAD ALIGNMENT & 2D KALMAN FILTERING (Priority 3)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_road_alignment_motion_smoothing_priority));
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
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_geofencing_sentinel_perimeter_boundaries_priority));
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
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_rag_telemetry_alert_thresholds_priority));
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
        // CARD 5.5: 🔔 NOTIFICATION SETTINGS & VISIBILITY POLICIES (Priority 5.5)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader("🔔 NOTIFICATION SETTINGS & VISIBILITY POLICIES"));
        LinearLayout notifCard = new LinearLayout(this);
        notifCard.setOrientation(LinearLayout.VERTICAL);

        com.jdroidx.jfindme.engine.alert.NotificationPolicyController notifController =
            com.jdroidx.jfindme.engine.alert.NotificationPolicyController.getInstance(this);

        // 1. Mandatory Security & Authentication (Always On / Mandatory)
        View authNotifSwitch = UiArchitect.createSwitch(this, "Authentication & Security Alerts (Mandatory)", notifController.isAuthSecurityAllowed(), (v, isChecked) -> {
            if (!isChecked) {
                UiArchitect.showSlideNotificationBanner(this, "Authentication Mandatory", "Security & authentication notifications are required and remain active.", true, 3000, null, null);
                // Revert
                if (v instanceof androidx.appcompat.widget.SwitchCompat) {
                    ((androidx.appcompat.widget.SwitchCompat) v).setChecked(true);
                }
                notifController.setSetting(com.jdroidx.jfindme.engine.alert.NotificationPolicyController.PREF_NOTIF_AUTH_SECURITY, true);
                return;
            }
            notifController.setSetting(com.jdroidx.jfindme.engine.alert.NotificationPolicyController.PREF_NOTIF_AUTH_SECURITY, true);
        });
        notifCard.addView(authNotifSwitch, btnLp);

        // 2. Device Sharing & Mesh Sync (Default: OFF / Silent)
        View shareNotifSwitch = UiArchitect.createSwitch(this, "Device Sharing & Peer Sync Notifications", notifController.isDeviceSharingAllowed(), (v, isChecked) -> {
            notifController.setSetting(com.jdroidx.jfindme.engine.alert.NotificationPolicyController.PREF_NOTIF_DEVICE_SHARING, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Device Sharing Alerts", isChecked ? "Device sharing & sync notifications enabled." : "Device sharing notifications silenced.", false, 2500, null, null);
        });
        notifCard.addView(shareNotifSwitch, btnLp);

        // 3. Geofence Perimeter Transition Alerts (Default: OFF / Silent)
        View geoNotifSwitch = UiArchitect.createSwitch(this, "Geofence Perimeter Breaches", notifController.isGeofenceAllowed(), (v, isChecked) -> {
            notifController.setSetting(com.jdroidx.jfindme.engine.alert.NotificationPolicyController.PREF_NOTIF_GEOFENCE, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Geofence Alerts", isChecked ? "Perimeter breach notifications enabled." : "Perimeter alerts silenced.", false, 2500, null, null);
        });
        notifCard.addView(geoNotifSwitch, btnLp);

        // 4. Butterfly Point Rendezvous (Default: OFF / Silent)
        View butterflyNotifSwitch = UiArchitect.createSwitch(this, "Butterfly Rendezvous & Quorum Alerts", notifController.isButterflyAllowed(), (v, isChecked) -> {
            notifController.setSetting(com.jdroidx.jfindme.engine.alert.NotificationPolicyController.PREF_NOTIF_BUTTERFLY, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Butterfly Rendezvous Alerts", isChecked ? "Rendezvous notifications enabled." : "Rendezvous notifications silenced.", false, 2500, null, null);
        });
        notifCard.addView(butterflyNotifSwitch, btnLp);

        // 5. Low Battery Warning (Default: OFF / Silent)
        View batteryNotifSwitch = UiArchitect.createSwitch(this, "Low Battery Warnings (<20%)", notifController.isBatteryAllowed(), (v, isChecked) -> {
            notifController.setSetting(com.jdroidx.jfindme.engine.alert.NotificationPolicyController.PREF_NOTIF_BATTERY, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Battery Alerts", isChecked ? "Battery distress alerts enabled." : "Battery alerts silenced.", false, 2500, null, null);
        });
        notifCard.addView(batteryNotifSwitch, btnLp);

        // 6. System Package Update Notifications (Default: OFF / Silent)
        View updateNotifSwitch = UiArchitect.createSwitch(this, "System Update Progress & Ready Alerts", notifController.isSystemUpdateAllowed(), (v, isChecked) -> {
            notifController.setSetting(com.jdroidx.jfindme.engine.alert.NotificationPolicyController.PREF_NOTIF_SYSTEM_UPDATES, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Update Alerts", isChecked ? "System update notifications enabled." : "System update notifications silenced.", false, 2500, null, null);
        });
        notifCard.addView(updateNotifSwitch, btnLp);

        // 7. Silent Background Sentinel Channel (Default: ON / Silent MIN Priority)
        View silentServiceSwitch = UiArchitect.createSwitch(this, "Silent Sentinel Status (Min Importance / No Pulsing)", notifController.isSilentForeground(), (v, isChecked) -> {
            notifController.setSetting(com.jdroidx.jfindme.engine.alert.NotificationPolicyController.PREF_NOTIF_SILENT_FOREGROUND, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "Sentinel Notification", isChecked ? "Foreground status clamped to silent minimum priority." : "Foreground status uses default priority.", false, 2500, null, null);
        });
        notifCard.addView(silentServiceSwitch, btnLp);

        content.addView(notifCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 6: 🔊 EMERGENCY ACOUSTIC BEACON & SOS SENTINEL (Priority 6)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_emergency_acoustic_beacon_sos_sentinel));
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
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_offline_vault_engine_sentinels_priority));
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
        View bgJobsLauncher = createIconButton("⚙️ 14 BACKGROUND JOBS & ENGINE SENTINELS", com.jdroidx.jfindme.R.drawable.ic_settings_gears, true, v -> BackgroundJobsConfigDialog.show(this));
        vaultCard.addView(createSettingTile("DATABASE VACUUM & STORAGE RETENTION", "Rolling Retention: " + RegistryManager.getStorageRetentionDays(this) + " Days • WAL Compaction", android.R.drawable.ic_menu_manage, v -> showStorageRetentionDialog(), tv -> tvStorageRetentionVal = tv), btnLp);
        int currentCacheCap = RegistryManager.getMaxGeocodeCacheSize(this);
        String cacheSub = "Max Cap: " + currentCacheCap + " Entries (L7 Reverse Geocode Cache)";
        vaultCard.addView(createSettingTile("REVERSE GEOCODE CACHE CAPACITY", cacheSub, android.R.drawable.ic_menu_mapmode, v -> showGeocodeCacheCapDialog(), tv -> tvGeocodeCacheVal = tv), btnLp);
        String ttlSub = "Sync Queue: " + RegistryManager.getSyncQueueTtlHours(this) + "h • Logs: " + RegistryManager.getSysLogTtlHours(this) + "h • Page Limit: " + RegistryManager.getDefaultPageLimit(this);
        vaultCard.addView(createSettingTile("DATABASE ENGINE TTL & QUERY LIMITS", ttlSub, android.R.drawable.ic_menu_preferences, v -> showDatabaseEngineTtlDialog(), tv -> tvDatabaseEngineTtlVal = tv), btnLp);
        vaultCard.addView(bgJobsLauncher, btnLp);

        content.addView(vaultCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 7B: ☁️ FIREBASE SPARK PLAN & BANDWIDTH QUOTA SENTINEL
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_firebase_spark_plan_quota_sentinel));
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
            tv -> tvSparkPlanQuotaVal = tv
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
            tv -> tvDeadReckoningVal = tv
        ), btnLp);

        long pulseIntervalMs = RegistryManager.getPresencePulseIntervalMs(this);
        String pulseSub = "Cadence: " + (pulseIntervalMs / 1000) + "s • 28-Byte Presence Pulse (Keeps Server Online)";
        sparkCard.addView(createSettingTile(
            "PRESENCE PULSE HEARTBEAT INTERVAL",
            pulseSub,
            android.R.drawable.ic_menu_recent_history,
            v -> showPresencePulseIntervalDialog(),
            tv -> tvPresencePulseVal = tv
        ), btnLp);

        String currentGroup = RegistryManager.getDeviceCorrespondingGroup(this);
        String groupSub = "Hierarchy Group: " + currentGroup + " • Relative Group-By Partition";
        sparkCard.addView(createSettingTile(
            "DEVICE CORRESPONDING GROUP (GROUP-BY)",
            groupSub,
            android.R.drawable.ic_menu_myplaces,
            v -> showDeviceGroupDialog(),
            tv -> tvDeviceGroupVal = tv
        ), btnLp);

        String fleetMap = RegistryManager.getString(this, RegistryManager.KEY_FLEET_HARDWARE_MAP, RegistryManager.DEFAULT_FLEET_HARDWARE_MAP);
        String fleetSub = "Dynamic Silicon Fleet: " + fleetMap;
        sparkCard.addView(createSettingTile(
            "DYNAMIC FLEET HARDWARE MAP",
            fleetSub,
            android.R.drawable.ic_menu_agenda,
            v -> showFleetHardwareMapDialog(),
            tv -> tvFleetHardwareMapVal = tv
        ), btnLp);

        String decomIds = RegistryManager.getString(this, RegistryManager.KEY_DECOMMISSIONED_AGENT_IDS, RegistryManager.DEFAULT_DECOMMISSIONED_AGENT_IDS);
        String decomSub = "Excluded Ghost Nodes: " + decomIds;
        sparkCard.addView(createSettingTile(
            "DECOMMISSIONED GHOST NODES (BLACKLIST)",
            decomSub,
            android.R.drawable.ic_menu_delete,
            v -> showDecommissionedAgentIdsDialog(),
            tv -> tvDecommissionedAgentIdsVal = tv
        ), btnLp);

        String relMap = RegistryManager.getRelativeRolesMap(this);
        String relSub = "Relative Roles Matrix: " + relMap;
        sparkCard.addView(createSettingTile(
            "DYNAMIC RELATIONAL ROLE MATRIX",
            relSub,
            android.R.drawable.ic_menu_share,
            v -> showRelativeRolesMapDialog(),
            tv -> tvRelativeRolesMapVal = tv
        ), btnLp);

        content.addView(sparkCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 7C: 🔀 TELEMETRY FAILOVER & CLOSED-LOOP SENTINEL (PRIORITY 7C)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_telemetry_failover_closed_loop_priority));
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

        long shareTimeout = RegistryManager.getDeviceSharingTimeoutMs(this);
        String shareTimeoutSub = "Deadline: " + (shareTimeout / 1000) + "s (Angel Butterfly progress & sync watchdog)";
        failoverCard.addView(createSettingTile(
            "DEVICE SHARING SYNC TIMEOUT",
            shareTimeoutSub,
            com.jdroidx.jfindme.R.drawable.ic_family_friends,
            v -> showDeviceSharingTimeoutDialog(),
            tv -> tvDeviceSharingTimeoutVal = tv
        ), btnLp);

        long pairingTimeout = RegistryManager.getPairingTimeoutMs(this);
        String pairingTimeoutSub = "Validity: " + (pairingTimeout / 60000) + " Mins (Invite code & multi-device session TTL)";
        failoverCard.addView(createSettingTile(
            "PAIRING CODE EXPIRATION TIMEOUT",
            pairingTimeoutSub,
            com.jdroidx.jfindme.R.drawable.ic_settings_gears,
            v -> showPairingTimeoutDialog(),
            tv -> tvPairingTimeoutVal = tv
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

        boolean smsFailoverEnabled = RegistryManager.isFailoverSmsEnabled(this);
        View smsFailoverSwitch = UiArchitect.createSwitch(this, "Baseband SMS Failover (Zero-Data Beacon)", smsFailoverEnabled, (v, isChecked) -> {
            RegistryManager.setFailoverSmsEnabled(this, isChecked);
            UiArchitect.showSlideNotificationBanner(this, "SMS Failover", isChecked ? "SMS Beacon failover active when offline." : "SMS Failover deactivated.", false, 2500, null, null);
        });
        failoverCard.addView(smsFailoverSwitch, btnLp);

        boolean isUnifiedSync = RegistryManager.isUnifiedDeviceIdSyncEnabled(this);
        String unifiedSyncSub = isUnifiedSync
            ? "Status: ACTIVE (Canonical single-path egress, zero HW_ duplication)"
            : "Status: DUAL-PATH (Legacy mirror enabled: writes to both canonical & HW_)";
        failoverCard.addView(createSettingTile(
            "UNIFIED DEVICE ID RTDB SYNC",
            unifiedSyncSub,
            com.jdroidx.jfindme.R.drawable.ic_settings_gears,
            v -> toggleUnifiedDeviceIdSync(),
            tv -> tvUnifiedDeviceIdSyncVal = tv
        ), btnLp);

        content.addView(failoverCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 8: 🗺️ MAP DISPLAY & GLOBAL AESTHETICS (Priority 8)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_map_display_global_aesthetics_priority));
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

        // [Variables] mapThrottle, mapThrottleSub | [Constants] DEFAULT_RENDER_THROTTLE_MS = 350 | [Dependencies] RegistryManager, UiArchitect | [Integration Points] MapActivity Throttled Marker Rendering, CARD 8 Map Preferences
        long mapThrottle = RegistryManager.getMapRenderThrottleMs(this);
        String mapThrottleSub = "Render Throttle: " + mapThrottle + "ms (Sliding window prevents marker jitter)";
        mapPrefsCard.addView(createSettingTile(
            "MAP RENDER THROTTLE WINDOW",
            mapThrottleSub,
            android.R.drawable.ic_menu_agenda,
            v -> showMapRenderThrottleDialog(),
            tv -> tvMapThrottleVal = tv
        ), btnLp);

        // [Variables] clusterSpacing, clusterSpacingSub | [Constants] DEFAULT_CLUSTER_SPACING_DP = 64 | [Dependencies] RegistryManager, UiArchitect | [Integration Points] MultiMarkerEngine Anti-Overlap Offset, CARD 8 Map Preferences
        int clusterSpacing = (int) Math.round(RegistryManager.getClusterSpacingDp(this));
        String clusterSpacingSub = "Cluster Spacing: " + clusterSpacing + "dp (Minimum avatar separation distance)";
        mapPrefsCard.addView(createSettingTile(
            "ANTI-OVERLAP CLUSTER SPACING",
            clusterSpacingSub,
            android.R.drawable.ic_menu_sort_by_size,
            v -> showClusterSpacingDialog(),
            tv -> tvClusterSpacingVal = tv
        ), btnLp);

        // [Variables] distThreshold, distSub | [Constants] DEFAULT_DISTANT_METERS = 50 | [Dependencies] RegistryManager, UiArchitect | [Integration Points] MapActivity Floating Avatar HUD, CARD 8 Map Preferences
        distThreshold = RegistryManager.getDistantDeviceThresholdMeters(this);
        distSub = "Panel Threshold: " + distThreshold + "m (Distance to float HUD panel)";
        mapPrefsCard.addView(createSettingTile(
            "DISTANT DEVICE PANEL THRESHOLD",
            distSub,
            android.R.drawable.ic_menu_compass,
            v -> showDistantDeviceThresholdDialog(),
            tv -> tvDistantVal = tv
        ), btnLp);

        String mapApiKey = RegistryManager.getGoogleMapsApiKey(this);
        String maskedKey = (mapApiKey != null && mapApiKey.length() > 10)
            ? mapApiKey.substring(0, 8) + "..." + mapApiKey.substring(mapApiKey.length() - 4)
            : "Default Key";
        String mapKeySub = "Credentials: " + maskedKey + " (Tap to edit Google Cloud Maps API Key)";
        mapPrefsCard.addView(createSettingTile(
            "GOOGLE MAPS API CREDENTIALS",
            mapKeySub,
            com.jdroidx.jfindme.R.drawable.ic_nav_map,
            v -> showGoogleMapsApiKeyDialog(),
            tv -> tvGoogleMapsApiKeyVal = tv
        ), btnLp);

        mapPrefsCard.addView(createIconButton("GLOBAL UI & ICON ARCHITECT", com.jdroidx.jfindme.R.drawable.ic_edit, v -> safeLaunch(UiConfigActivity.class)), btnLp);
        mapPrefsCard.addView(createIconButton("VISUAL THEME & COMPONENT GALLERY", com.jdroidx.jfindme.R.drawable.ic_nav_map, v -> safeLaunch(UiGalleryActivity.class)), btnLp);

        content.addView(mapPrefsCard, getCardParams());

        // ══════════════════════════════════════════════════════════════════════
        // CARD 9: 🦋 BUTTERFLY POINT & RENDEZVOUS HUB (PRIORITY 9)
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_butterfly_point_rendezvous_hub_priority));
        LinearLayout butterflyCard = new LinearLayout(this);
        butterflyCard.setOrientation(LinearLayout.VERTICAL);

        float arrivalRad = RegistryManager.getButterflyArrivalRadius(this);
        String arrivalSub = "Perimeter: " + ((int) arrivalRad) + "m (Dynamic geofence arrival detection)";
        butterflyCard.addView(createSettingTile(
            "ARRIVAL DETECTION PERIMETER",
            arrivalSub,
            com.jdroidx.jfindme.R.drawable.ic_butterfly_point,
            v -> showButterflyArrivalRadiusDialog(),
            tv -> tvButterflyArrivalRadiusVal = tv
        ), btnLp);

        int expiryHrs = RegistryManager.getButterflyExpiryHours(this);
        String expirySub = "Lifespan: " + expiryHrs + " Hours (Default auto-archive TTL window)";
        butterflyCard.addView(createSettingTile(
            "RENDEZVOUS EXPIRATION WINDOW",
            expirySub,
            android.R.drawable.ic_menu_recent_history,
            v -> showButterflyExpiryDialog(),
            tv -> tvButterflyExpiryVal = tv
        ), btnLp);

        float hystMeters = RegistryManager.getButterflyHysteresisMeters(this);
        String hystSub = "Deadband: " + ((int) hystMeters) + "m (Anti-flutter arrival latch threshold)";
        butterflyCard.addView(createSettingTile(
            "ARRIVAL HYSTERESIS DEADBAND",
            hystSub,
            android.R.drawable.ic_menu_compass,
            v -> showButterflyHysteresisDialog(),
            tv -> tvButterflyHysteresisVal = tv
        ), btnLp);

        float maxDistKm = RegistryManager.getButterflyMaxDistanceKm(this);
        String maxDistSub = "Safeguard: " + ((int) maxDistKm) + " km (Human-in-the-loop warning confirmation)";
        butterflyCard.addView(createSettingTile(
            "DISTANCE SAFEGUARD THRESHOLD",
            maxDistSub,
            android.R.drawable.ic_dialog_alert,
            v -> showButterflyMaxDistanceDialog(),
            tv -> tvButterflyMaxDistanceVal = tv
        ), btnLp);

        int timeoutHrs = RegistryManager.getButterflyInvitationTimeoutHours(this);
        String timeoutSub = "Timeout: " + timeoutHrs + " Hours (Auto-revoke stale pending invitations)";
        butterflyCard.addView(createSettingTile(
            "INVITATION TIMEOUT (DEFAULT 24H)",
            timeoutSub,
            android.R.drawable.ic_menu_recent_history,
            v -> showButterflyInvitationTimeoutDialog(),
            tv -> tvButterflyInvitationTimeoutVal = tv
        ), btnLp);

        int graceMins = RegistryManager.getButterflyOverdueGraceMinutes(this);
        String graceSub = "Grace Window: " + graceMins + " Minutes (Arrival window before auto-archive)";
        butterflyCard.addView(createSettingTile(
            "OVERDUE GRACE PERIOD (DEFAULT 2H)",
            graceSub,
            android.R.drawable.ic_menu_agenda,
            v -> showButterflyOverdueGraceDialog(),
            tv -> tvButterflyOverdueGraceVal = tv
        ), btnLp);

        int syncSec = RegistryManager.getButterflyForegroundSyncIntervalSec(this);
        String syncSub = "Interval: " + syncSec + "s (Active rendezvous GPS motion cadence)";
        butterflyCard.addView(createSettingTile(
            "FOREGROUND TELEMETRY INTERVAL",
            syncSub,
            android.R.drawable.ic_menu_mylocation,
            v -> showButterflyForegroundSyncDialog(),
            tv -> tvButterflyForegroundSyncVal = tv
        ), btnLp);

        int maxDays = RegistryManager.getButterflyMaxAvailabilityDays(this);
        String maxDaysSub = "Availability: " + maxDays + " Days (Rendezvous point retention before archival purge)";
        butterflyCard.addView(createSettingTile(
            "MAX AVAILABILITY WINDOW (DEFAULT 7 DAYS)",
            maxDaysSub,
            android.R.drawable.ic_menu_recent_history,
            v -> showButterflyMaxAvailabilityDialog(),
            tv -> tvButterflyMaxAvailabilityVal = tv
        ), btnLp);

        float nominalSpeed = RegistryManager.getButterflyNominalSpeedKmh(this);
        String nominalSub = "Nominal Speed: " + ((int) nominalSpeed) + " km/h (Fallback speed for travel ETA estimation)";
        butterflyCard.addView(createSettingTile(
            "NOMINAL TRAVEL SPEED (KM/H)",
            nominalSub,
            android.R.drawable.ic_menu_directions,
            v -> showButterflyNominalSpeedDialog(),
            tv -> tvButterflyNominalSpeedVal = tv
        ), btnLp);

        int approachMins = RegistryManager.getButterflyApproachWindowMinutes(this);
        String approachSub = "TTR Horizon: " + approachMins + " Minutes (Time window to trigger active approach tracking)";
        butterflyCard.addView(createSettingTile(
            "APPROACH MONITORING WINDOW",
            approachSub,
            android.R.drawable.ic_menu_recent_history,
            v -> showButterflyApproachWindowDialog(),
            tv -> tvButterflyApproachWindowVal = tv
        ), btnLp);

        float tacticalKm = RegistryManager.getButterflyTacticalRadiusKm(this);
        String tacticalSub = "Proximity Radius: " + ((int) tacticalKm) + " km (Threshold for high-velocity tactical vectors)";
        butterflyCard.addView(createSettingTile(
            "TACTICAL QUORUM RADIUS",
            tacticalSub,
            android.R.drawable.ic_menu_compass,
            v -> showButterflyTacticalRadiusDialog(),
            tv -> tvButterflyTacticalRadiusVal = tv
        ), btnLp);

        content.addView(butterflyCard, getCardParams());

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
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_tactical_live_maps_radar));
        content.addView(registerScreenCard("PRIMARY LIVE MAP", "Vector Map with Marker Callouts, Direction Chevrons & SOS Trigger", "LIVE", null, com.jdroidx.jfindme.R.drawable.ic_nav_map, "map live tracking gps pins sos chevron", v -> safeLaunch(MapActivity.class)));
        // [Variables]: intent | [Constants]: LAUNCH_PANEL, INITIAL_TAB | [Dependencies]: LiveMapDashboardActivity, GroupActivity | [Integration Points]: Deep-Linked Screen Card Ingress
        content.addView(registerScreenCard("TACTICAL DASHBOARD HUD", "Dual-Pane Real-Time Radar & Multi-Node Coordinate Telemetry", "RADAR", null, com.jdroidx.jfindme.R.drawable.ic_radar, "radar dashboard telemetry coordinates dual pane", v -> safeLaunch(LiveMapDashboardActivity.class, intent -> intent.putExtra("LAUNCH_PANEL", "RADAR"))));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 2: 👥 CIRCLE MESH & PAIRING GATEWAYS
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_circle_mesh_pairing_gateways));
        content.addView(registerScreenCard("NETWORK PAIRING GATEWAY", "Cloud PIN, QR Matrix Scanner & Nearby P2P Offline Discovery", "PAIRING", tv -> badgeRosterTv = tv, com.jdroidx.jfindme.R.drawable.ic_pairing_matrix, "pairing qr pin p2p wifi direct connect network gateway", v -> safeLaunch(NetworkGatewayActivity.class)));
        content.addView(registerScreenCard("DEVICE SHARING & MESH DELEGATION", "Delegate Access & Connect Peer Nodes via Master Orchestrator", "SHARING", null, com.jdroidx.jfindme.R.drawable.ic_pairing_matrix, "device sharing peer delegate link b to c mesh", v -> safeLaunch(FamilyFriendsActivity.class, intent -> intent.putExtra("OPEN_DEVICE_SHARING", true))));
        content.addView(registerScreenCard("FAMILY & FRIENDS ROSTER", "Multi-Tier Circle Management, Guardians & Social Circles", "CIRCLE", null, com.jdroidx.jfindme.R.drawable.ic_family_friends, "family friends guardians roster members contacts", v -> safeLaunch(FamilyFriendsActivity.class)));
        content.addView(registerScreenCard("MEMBER DOSSIER & ACL", "Node Profile, Medical Notes & Granular Access Control List", "ACL", null, com.jdroidx.jfindme.R.drawable.ic_user_profile, "dossier acl permissions member medical notes person details", v -> safeLaunch(PersonDetailsActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 3: 👤 USER IDENTITY, 3D AVATAR & GOVERNANCE
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_user_identity_3d_avatar_governance));
        content.addView(registerScreenCard("PUPIL IDENTITY PROFILE", "Hardware Silicon Anchor, Demographics & Medical Ledger", getNicknameBadgeText(), tv -> badgeProfileTv = tv, com.jdroidx.jfindme.R.drawable.ic_user_profile, "profile identity pupil nickname phone email address avatar", v -> safeLaunch(ProfileActivity.class)));
        content.addView(registerScreenCard("3D VRM AVATAR EDITOR", "Interactive 3D Model Customization, Mesh Rotation & Shaders", "3D VRM", null, com.jdroidx.jfindme.R.drawable.ic_edit, "avatar 3d vrm model mesh rotation editor", v -> safeLaunch(AvatarEditorActivity.class)));
        content.addView(registerScreenCard("BIOMETRIC AVATAR CROP", "Circular Photo Masking & Aspect-Ratio Biometric Generator", "CROP", null, com.jdroidx.jfindme.R.drawable.ic_crop, "crop avatar photo camera picture biometric", v -> safeLaunch(AvatarCropActivity.class)));
        content.addView(registerScreenCard("ENTERPRISE ORGANIZATION", "Corporate Multi-Tenancy Domain & Department Hierarchy", "TENANT", null, com.jdroidx.jfindme.R.drawable.ic_about_app, "organization corporate enterprise tenant department domain", v -> safeLaunch(OrgActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 4: 🌐 GEOSPATIAL & SAFE ZONE CLUSTER
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_geospatial_safe_zone_cluster));
        // [Variables] content, v | [Constants] "BUTTERFLY_POINT", "LAUNCH_PANEL" | [Dependencies] registerScreenCard, MapActivity, ButterflyPointDialog | [Integration Points] Settings Screen Directory ⟷ Butterfly Rendezvous Hub
        content.addView(registerScreenCard("BUTTERFLY RENDEZVOUS HUB", "Dynamic Multi-Node Meeting Points, Vector Navigation & Fleet Quorum", "RENDEZVOUS", null, com.jdroidx.jfindme.R.drawable.ic_butterfly_point, "butterfly meeting point rendezvous rendezvous hub tracking fleet", v -> safeLaunch(MapActivity.class, intent -> intent.putExtra("LAUNCH_PANEL", "BUTTERFLY_POINT"))));
        content.addView(registerScreenCard("SAFE ZONE MANAGER", "Circular & Polygon Perimeter Geofence Editor with Schedules", "GEOFENCE", tv -> badgeZonesTv = tv, com.jdroidx.jfindme.R.drawable.ic_safe_zone, "safe zones geofence polygon circle perimeter schedule", v -> safeLaunch(GeofenceActivity.class)));
        content.addView(registerScreenCard("TRANSITION AUDIT LEDGER", "Perimeter Breach Ingress/Egress History, Filters & CSV Export", "LOGS", null, com.jdroidx.jfindme.R.drawable.ic_nav_notice, "geofence notification transitions audit ingress egress breach export", v -> safeLaunch(GeofenceNotificationActivity.class)));
        content.addView(registerScreenCard("LOCATION LOGS & REPLAY", "Historical GPS Trajectory Playback with 1x/2x/5x Speeds", "REPLAY", tv -> badgeHistoryTv = tv, com.jdroidx.jfindme.R.drawable.ic_location_history, "history breadcrumbs replay trajectory route waypoints", v -> safeLaunch(HistoryActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 5: 🛡️ DEVICE PRIVACY, HEALTH & ALERTS
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_device_privacy_health_alerts));
        content.addView(registerScreenCard("DEVICE PERMISSIONS & ACL HUB", "Grouped Mandatory, Background, OEM, Optional & ACL Toggles", "PERMS", tv -> badgePermissionsTv = tv, com.jdroidx.jfindme.R.drawable.ic_privacy_lock, "permissions mandatory optional background oem acl toggles location camera bluetooth", v -> {
            switchTab(0);
            if (scrollUniversal != null) {
                scrollUniversal.post(() -> {
                    if (permissionsHubHeaderView != null) {
                        scrollUniversal.smoothScrollTo(0, permissionsHubHeaderView.getTop());
                    }
                });
            }
        }));
        content.addView(registerScreenCard("IDENTITY SECURITY PROTOCOL", "Hardware Permissions, Root Kernel Audit & Ghost Mode Locks", "SECURITY", null, com.jdroidx.jfindme.R.drawable.ic_privacy_lock, "privacy security root jailbreak ghost permissions", v -> safeLaunch(PrivacyActivity.class)));
        content.addView(registerScreenCard("DEVICE HEALTH & VITALS", "Battery Draw, Doze Mode State, RAM & Sensor Telemetry", getBatteryBadgeText(), tv -> badgeHealthTv = tv, com.jdroidx.jfindme.R.drawable.ic_device_health, "health battery doze ram vitals sensors diagnostics", v -> safeLaunch(HealthActivity.class)));
        content.addView(registerScreenCard("SYSTEM NOTIFICATIONS ROUTING", "Alert Triggers, DND Bypass & Hardware Notification Pokes", "ALERTS", null, com.jdroidx.jfindme.R.drawable.ic_nav_notice, "notifications alerts sound vibration triggers dnd poke", v -> safeLaunch(NotifActivity.class)));
        content.addView(registerScreenCard("ACOUSTIC EMERGENCY SIREN", "Emergency Horn & 5Hz Torch Flashlight Beacon Test", "SIREN", null, android.R.drawable.ic_lock_idle_alarm, "siren horn alarm flashlight emergency beacon", v -> safeLaunch(AcousticSirenActivity.class)));

        // ══════════════════════════════════════════════════════════════════════
        // MODULE 6: 🔍 FORENSIC VAULT, AUDIT & DIAGNOSTICS
        // ══════════════════════════════════════════════════════════════════════
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_forensic_vault_audit_diagnostics));
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
        content.addView(createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_system_update_purge_maintenance));
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
                com.jdroidx.jfindme.data.repository.DeviceRepository repo = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this);
                rosterCount = (int) repo.getRecordCount("master_roster");
                safeZoneCount = (int) repo.getRecordCount("safe_zones");
                historyCount = (int) repo.getRecordCount("location_history");
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

    // [Variables]: activityClass, intent, customizer | [Constants]: TAG | [Dependencies]: Intent, SystemLogger, UiArchitect | [Integration Points]: Screen Navigation Cards & Deep Linking
    public interface IntentCustomizer {
        void customize(Intent intent);
    }

    /**
     * Crash-Proof Activity Launcher with UI boundary error handling and deep-link parameter customization.
     */
    private void safeLaunch(Class<?> activityClass, IntentCustomizer customizer) {
        try {
            Intent intent = new Intent(this, activityClass);
            if (customizer != null) {
                customizer.customize(intent);
            }
            startActivity(intent);
        } catch (Exception e) {
            SystemLogger.e(TAG, "Failed to launch " + activityClass.getSimpleName(), e);
            UiArchitect.showSlideNotificationBanner(this, "Module Launch Error", "Unable to open " + activityClass.getSimpleName() + ": " + e.getMessage(), true, 3000, null, null);
        }
    }

    private void safeLaunch(Class<?> activityClass) {
        safeLaunch(activityClass, null);
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

    @Override
    protected View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart());
        header.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_HEADER"));
        header.setTypeface(null, Typeface.BOLD);
        int hPad = getResponsivePaddingQuota();
        header.setPadding(hPad / 2, hPad * 2, 0, hPad);
        return header;
    }

    protected View createSectionHeader(int resId) {
        return createSectionHeader(getString(resId));
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
        long currentMins = RegistryManager.getStealthIntervalMins(this);
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
                RegistryManager.setStealthIntervalMins(this, chosen);
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
                com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this).setStealthIntervalMins(60L);
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
        if (tvPingIntervalVal != null) {
            long pingSec = RegistryManager.getTrackingInterval(this) / 1000;
            String warning = pingSec < 30 ? " ⚠️ HIGH BATTERY DRAIN" : "";
            tvPingIntervalVal.setText("Active Cadence: " + pingSec + "s" + warning + " | Preset/Stepper Mode");
        }
        if (tvStealthCadenceVal != null) tvStealthCadenceVal.setText("Cadence: 60 mins | Autonomous SIM & Alarm Sentinel");
        if (tvMotionDutyVal != null) tvMotionDutyVal.setText("Still: 300s | Car: 10s | Walk: 20s");
        if (tvDisplacementVal != null) tvDisplacementVal.setText("Displacement: 0m (Suppresses stationary GPS jitter)");
        if (tvRoadSmoothingVal != null) tvRoadSmoothingVal.setText("Heading EMA: 0.35 | Measurement Noise R: 25");
        if (tvJitterVal != null) tvJitterVal.setText("Debounce: 15s (Temporal boundary anti-flutter)");
        if (tvRadiusVal != null) tvRadiusVal.setText("Default Radius: 100m (Base safe zone perimeter)");
        if (tvLowBatteryVal != null) tvLowBatteryVal.setText("Threshold: 20% (Triggers amber distress telemetry)");
        if (tvStaleVal != null) tvStaleVal.setText("Stale Window: 15 Min (Marks node amber inactive)");
        if (tvCriticalOfflineVal != null) tvCriticalOfflineVal.setText("Offline Window: 6 Hours (Marks node red offline)");
        if (tvDistantVal != null) tvDistantVal.setText("Panel Threshold: " + RegistryManager.getDistantDeviceThresholdMeters(this) + "m (Distance to float HUD panel)");
        if (tvMapThrottleVal != null) tvMapThrottleVal.setText("Render Throttle: " + RegistryManager.getMapRenderThrottleMs(this) + "ms (Sliding window prevents marker jitter)");
        if (tvClusterSpacingVal != null) tvClusterSpacingVal.setText("Cluster Spacing: " + (int) Math.round(RegistryManager.getClusterSpacingDp(this)) + "dp (Minimum avatar separation distance)");
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
        if (tvDeviceSharingTimeoutVal != null) tvDeviceSharingTimeoutVal.setText("Deadline: " + (RegistryManager.getDeviceSharingTimeoutMs(this) / 1000) + "s (Angel Butterfly progress & sync watchdog)");
        if (tvPairingTimeoutVal != null) tvPairingTimeoutVal.setText("Validity: " + (RegistryManager.getPairingTimeoutMs(this) / 60000) + " Mins (Invite code & multi-device session TTL)");
        if (tvMaxAckFailuresVal != null) tvMaxAckFailuresVal.setText("Threshold: " + RegistryManager.getMaxAckFailures(this) + " Timeouts (Circuit breaker threshold before route switch)");
        if (tvFailoverSmsVal != null) {
            String sms = RegistryManager.getFailoverSmsTarget(this);
            tvFailoverSmsVal.setText((sms != null && !sms.trim().isEmpty()) ? "Recipient: " + sms + " (Level N3 zero-data beacon)" : "Not Configured (Tap to set Emergency SOS SMS Number)");
        }
        if (tvGoogleMapsApiKeyVal != null) {
            String mKey = RegistryManager.getGoogleMapsApiKey(this);
            String masked = (mKey != null && mKey.length() > 10)
                ? mKey.substring(0, 8) + "..." + mKey.substring(mKey.length() - 4)
                : "Default Key";
            tvGoogleMapsApiKeyVal.setText("Credentials: " + masked + " (Tap to edit Google Cloud Maps API Key)");
        }
        if (tvButterflyArrivalRadiusVal != null) {
            tvButterflyArrivalRadiusVal.setText("Perimeter: " + ((int) RegistryManager.getButterflyArrivalRadius(this)) + "m (Dynamic geofence arrival detection)");
        }
        if (tvButterflyExpiryVal != null) {
            tvButterflyExpiryVal.setText("Lifespan: " + RegistryManager.getButterflyExpiryHours(this) + " Hours (Default auto-archive TTL window)");
        }
        if (tvButterflyHysteresisVal != null) {
            tvButterflyHysteresisVal.setText("Deadband: " + ((int) RegistryManager.getButterflyHysteresisMeters(this)) + "m (Anti-flutter arrival latch threshold)");
        }
        if (tvButterflyMaxDistanceVal != null) {
            tvButterflyMaxDistanceVal.setText("Safeguard: " + ((int) RegistryManager.getButterflyMaxDistanceKm(this)) + " km (Human-in-the-loop warning confirmation)");
        }
        if (tvButterflyInvitationTimeoutVal != null) {
            tvButterflyInvitationTimeoutVal.setText("Timeout: " + RegistryManager.getButterflyInvitationTimeoutHours(this) + " Hours (Auto-revoke stale pending invitations)");
        }
        if (tvButterflyOverdueGraceVal != null) {
            tvButterflyOverdueGraceVal.setText("Grace Window: " + RegistryManager.getButterflyOverdueGraceMinutes(this) + " Minutes (Arrival window before auto-archive)");
        }
        if (tvButterflyForegroundSyncVal != null) {
            tvButterflyForegroundSyncVal.setText("Interval: " + RegistryManager.getButterflyForegroundSyncIntervalSec(this) + "s (Active rendezvous GPS motion cadence)");
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
        masterSwitch.setText("🔋 DYNAMIC CADENCE STEPPER (10s - 3600s)");
        masterSwitch.setTextColor(getTextColor());
        masterSwitch.setChecked(prefs.getBoolean("pref_stepper_active", false));
        stepperCard.addView(masterSwitch);

        final LinearLayout stepperControls = new LinearLayout(this);
        stepperControls.setGravity(Gravity.CENTER);
        stepperControls.setAlpha(masterSwitch.isChecked() ? 1.0f : 0.4f);

        TextView valTv = new TextView(this);
        long currTrackMs = RegistryManager.getTrackingInterval(this);
        long currentVal = currTrackMs > 0 ? (currTrackMs / 1000) : (prefs.getLong("pref_manual_pulse", 30000L) / 1000);
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
            if (val > 10) {
                val -= 10;
                valTv.setText(val + "s");
                prefs.edit().putLong("pref_manual_pulse", val * 1000).apply();
                RegistryManager.setTrackingInterval(this, val * 1000);
                com.jdroidx.jfindme.service.BackgroundJobManager.INSTANCE.updateJobFrequency(this, "JOB_TRACKING_SERVICE", val);
                updatePingCadenceSubtitle(val, "Manual Stepper");
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
            if (val < 3600) {
                val += 10;
                valTv.setText(val + "s");
                prefs.edit().putLong("pref_manual_pulse", val * 1000).apply();
                RegistryManager.setTrackingInterval(this, val * 1000);
                com.jdroidx.jfindme.service.BackgroundJobManager.INSTANCE.updateJobFrequency(this, "JOB_TRACKING_SERVICE", val);
                updatePingCadenceSubtitle(val, "Manual Stepper");
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            }
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
                updatePingCadenceSubtitle(v / 1000, n);
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
                updatePingCadenceSubtitle(val / 1000, "Manual Stepper");
            }
        });

        LinearLayout.LayoutParams oemBtnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        oemBtnLp.setMargins(0, 25, 0, 10);
        View oemWhitelistBtn = createIconButton("⚡ CROSS-OEM AUTOSTART & BATTERY WHITELIST", com.jdroidx.jfindme.R.drawable.ic_device_health, v -> {
            dialog.dismiss();
            com.jdroidx.jfindme.engine.power.OEMPowerWhitelistManager.showOemGuidanceDialog(SettingsActivity.this);
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
        int currentTtl = prefs.getInt("pairing_pin_ttl_minutes", 5);
        int[] values = {5, 10, 15, 20, 30};
        String customLabel = "✏️ Custom TTL (" + currentTtl + " Min)...";
        String[] options = {
            "5 Minutes (Recommended - NIST SP 800-63B)",
            "10 Minutes",
            "15 Minutes",
            "20 Minutes",
            "30 Minutes (Extended Pairing Window)",
            customLabel
        };
        int selectedIdx = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentTtl) {
                selectedIdx = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Pairing PIN Expiry TTL")
            .setSingleChoiceItems(options, selectedIdx, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("⏱️ Custom PIN TTL", "Minutes (1 - 120)", 1, 120, currentTtl, "Min", chosen -> {
                        prefs.edit().putInt("pairing_pin_ttl_minutes", chosen).apply();
                        if (tvPinTtlVal != null) {
                            tvPinTtlVal.setText("TTL: " + chosen + " Min (NIST SP 800-63B pairing countdown)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "PIN TTL Saved", "Pairing PIN Expiry TTL set to " + chosen + " minutes.", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    prefs.edit().putInt("pairing_pin_ttl_minutes", chosen).apply();
                    if (tvPinTtlVal != null) {
                        tvPinTtlVal.setText("TTL: " + chosen + " Min (NIST SP 800-63B pairing countdown)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "PIN TTL Saved", "Pairing PIN Expiry TTL set to " + chosen + " minutes.", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showGeofenceJitterDialog() {
        long currentMs = RegistryManager.getGeofenceJitterDebounceMs(this);
        long[] values = {0L, 5000L, 15000L, 30000L, 60000L};
        String customLabel = "✏️ Custom Debounce (" + (currentMs / 1000) + "s)...";
        String[] options = {
            "0s (Disabled - Instant Trigger)",
            "5s (High Sensitivity - Min Range)",
            "15s (Balanced Standard - Mid Range / Recommended)",
            "30s (Extended Boundary Filter)",
            "60s (Highway & Rapid Travel - Max Range)",
            customLabel
        };
        int selectedIdx = options.length - 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentMs) {
                selectedIdx = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Perimeter Jitter Filter (Anti-Flutter)")
            .setSingleChoiceItems(options, selectedIdx, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("⏱️ Custom Jitter Debounce", "Seconds (0 - 300)", 0, 300, (int)(currentMs / 1000), "s", chosen -> {
                        long chosenMs = chosen * 1000L;
                        RegistryManager.setGeofenceJitterDebounceMs(this, chosenMs);
                        if (tvJitterVal != null) {
                            tvJitterVal.setText("Debounce: " + chosen + "s (Temporal boundary anti-flutter)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Jitter Filter Updated", "Debounce threshold set to " + chosen + "s", false, 2500, null, null);
                    });
                } else {
                    long chosenMs = values[which];
                    RegistryManager.setGeofenceJitterDebounceMs(this, chosenMs);
                    if (tvJitterVal != null) {
                        tvJitterVal.setText("Debounce: " + (chosenMs / 1000) + "s (Temporal boundary anti-flutter)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Jitter Filter Updated", "Debounce threshold set to " + (chosenMs / 1000) + "s", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showDisplacementDialog() {
        float current = RegistryManager.getMinDisplacementMeters(this);
        float[] values = {0f, 5f, 10f, 25f};
        String customLabel = "✏️ Custom Displacement (" + (int)current + "m)...";
        String[] options = {
            "0m (Continuous Telemetry - Max Sensitivity / Default)",
            "5m (Fine Pedestrian Tracking)",
            "10m (City Balanced Mode)",
            "25m (Vehicle / Battery Saver - Max Range)",
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
            .setTitle("📍 Minimum Displacement Threshold")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("📍 Custom Displacement Threshold", "Meters (0 - 500)", 0, 500, (int)current, "m", chosen -> {
                        RegistryManager.setMinDisplacementMeters(this, (float) chosen);
                        if (tvDisplacementVal != null) {
                            tvDisplacementVal.setText("Displacement: " + chosen + "m (Suppresses stationary GPS jitter)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Displacement Updated", "Displacement threshold set to " + chosen + "m", false, 2500, null, null);
                    });
                } else {
                    float chosen = values[which];
                    RegistryManager.setMinDisplacementMeters(this, chosen);
                    if (tvDisplacementVal != null) {
                        tvDisplacementVal.setText("Displacement: " + (int) chosen + "m (Suppresses stationary GPS jitter)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Displacement Updated", "Displacement threshold set to " + (int) chosen + "m", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showLowBatteryDialog() {
        int current = RegistryManager.getLowBatteryThreshold(this);
        int[] values = {10, 15, 20, 25};
        String customLabel = "✏️ Custom Threshold (" + current + "%)...";
        String[] options = {
            "10% (Critical Level Only)",
            "15% (Medium Warning)",
            "20% (Standard Level - Recommended / Default)",
            "25% (Early Warning Level)",
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
            .setTitle("🟡 Low Battery Warning Level")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("🟡 Custom Low Battery Threshold", "Percentage (5 - 50)", 5, 50, current, "%", chosen -> {
                        RegistryManager.setLowBatteryThreshold(this, chosen);
                        if (tvLowBatteryVal != null) {
                            tvLowBatteryVal.setText("Threshold: " + chosen + "% (Triggers amber distress telemetry)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Low Battery Threshold", "Warning trigger set to " + chosen + "%", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setLowBatteryThreshold(this, chosen);
                    if (tvLowBatteryVal != null) {
                        tvLowBatteryVal.setText("Threshold: " + chosen + "% (Triggers amber distress telemetry)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Low Battery Threshold", "Warning trigger set to " + chosen + "%", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showStaleInactiveDialog() {
        int current = RegistryManager.getStaleInactiveMinutes(this);
        int[] values = {5, 15, 30, 60};
        String customLabel = "✏️ Custom Window (" + current + " Min)...";
        String[] options = {
            "5 Minutes (Strict / High Alert)",
            "15 Minutes (Balanced Standard - Default)",
            "30 Minutes (Relaxed Mode)",
            "60 Minutes (Casual Mode)",
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
            .setTitle("🟡 Inactive Stale Threshold (Amber)")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("🟡 Custom Stale Window", "Minutes (1 - 360)", 1, 360, current, "Min", chosen -> {
                        RegistryManager.setStaleInactiveMinutes(this, chosen);
                        if (tvStaleVal != null) {
                            tvStaleVal.setText("Stale Window: " + chosen + " Min (Marks node amber inactive)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Stale Threshold Updated", "Amber warning set to " + chosen + " minutes", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setStaleInactiveMinutes(this, chosen);
                    if (tvStaleVal != null) {
                        tvStaleVal.setText("Stale Window: " + chosen + " Min (Marks node amber inactive)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Stale Threshold Updated", "Amber warning set to " + chosen + " minutes", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCriticalOfflineDialog() {
        int current = RegistryManager.getCriticalOfflineHours(this);
        int[] values = {2, 6, 12, 24};
        String customLabel = "✏️ Custom Window (" + current + " Hours)...";
        String[] options = {
            "2 Hours (Strict Fleet)",
            "6 Hours (Balanced - Recommended / Default)",
            "12 Hours (Half-Day Extended)",
            "24 Hours (Full Day Extended)",
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
            .setTitle("🔴 Critical Offline Threshold (Red)")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("🔴 Custom Critical Offline Window", "Hours (1 - 168)", 1, 168, current, "Hours", chosen -> {
                        RegistryManager.setCriticalOfflineHours(this, chosen);
                        if (tvCriticalOfflineVal != null) {
                            tvCriticalOfflineVal.setText("Offline Window: " + chosen + " Hours (Marks node red offline)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Offline Threshold Updated", "Red alert trigger set to " + chosen + " hours", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setCriticalOfflineHours(this, chosen);
                    if (tvCriticalOfflineVal != null) {
                        tvCriticalOfflineVal.setText("Offline Window: " + chosen + " Hours (Marks node red offline)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Offline Threshold Updated", "Red alert trigger set to " + chosen + " hours", false, 2500, null, null);
                }
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

    private void showDeviceSharingTimeoutDialog() {
        String[] options = new String[]{
            "⏱️ 10 Seconds (Fast Mesh / Local Wi-Fi)",
            "⏱️ 15 Seconds (Standard SLA Baseline - Recommended)",
            "⏱️ 30 Seconds (High-Latency Cellular / Low Signal)",
            "⏱️ 60 Seconds (Extended Global Roaming Timeout)"
        };
        long[] values = new long[]{10000L, 15000L, 30000L, 60000L};
        long current = RegistryManager.getDeviceSharingTimeoutMs(this);
        int selected = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Device Sharing Sync Timeout")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                long chosen = values[which];
                RegistryManager.setDeviceSharingTimeoutMs(this, chosen);
                if (tvDeviceSharingTimeoutVal != null) {
                    tvDeviceSharingTimeoutVal.setText("Deadline: " + (chosen / 1000) + "s (Angel Butterfly progress & sync watchdog)");
                }
                UiArchitect.showSlideNotificationBanner(this, "SHARING TIMEOUT MUTATED", "Device sharing timeout set to " + (chosen / 1000) + "s", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showPairingTimeoutDialog() {
        String[] options = new String[]{
            "⏱️ 5 Minutes (Strict Security)",
            "⏱️ 10 Minutes (Standard Pairing)",
            "⏱️ 15 Minutes (Fleet Multi-Device Rollout)",
            "⏱️ 30 Minutes (Extended Classroom / Family Session)",
            "⏱️ 60 Minutes (Max Administrative Grace Period)"
        };
        long[] values = new long[]{5 * 60 * 1000L, 10 * 60 * 1000L, 15 * 60 * 1000L, 30 * 60 * 1000L, 60 * 60 * 1000L};
        long current = RegistryManager.getPairingTimeoutMs(this);
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Pairing Code Expiration Timeout")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                long chosen = values[which];
                RegistryManager.setPairingTimeoutMs(this, chosen);
                if (tvPairingTimeoutVal != null) {
                    tvPairingTimeoutVal.setText("Validity: " + (chosen / 60000) + " Mins (Invite code & multi-device session TTL)");
                }
                UiArchitect.showSlideNotificationBanner(this, "PAIRING TIMEOUT MUTATED", "Pairing code expiration set to " + (chosen / 60000) + " minutes", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showGoogleMapsApiKeyDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = getResponsivePaddingQuota();
        container.setPadding(pad, pad / 2, pad, pad / 2);

        int overlayColor = getGradientOverlayText();
        TextView tvHelp = new TextView(this);
        tvHelp.setText("Enter your Google Cloud Console Maps API Key for package com.jdroidx.jfindme. This key is used dynamically by the Map Engine to authenticate vector tiles and Google Play Services.");
        tvHelp.setTextSize(11f);
        tvHelp.setTextColor(overlayColor == Color.WHITE ? Color.parseColor("#CCFFFFFF") : Color.DKGRAY);
        tvHelp.setPadding(0, 0, 0, 16);
        container.addView(tvHelp);

        EditText et = new EditText(this);
        et.setHint("AIzaSy...");
        et.setText(RegistryManager.getGoogleMapsApiKey(this));
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        et.setTextColor(overlayColor);
        et.setHintTextColor(overlayColor == Color.WHITE ? Color.parseColor("#99FFFFFF") : Color.GRAY);
        et.setPadding(30, 25, 30, 25);
        et.setBackground(getSolidDrawable(overlayColor == Color.WHITE ? Color.parseColor("#26FFFFFF") : Color.parseColor("#26000000"), 20f));
        container.addView(et);

        new android.app.AlertDialog.Builder(this)
            .setTitle("🗺️ Google Maps API Key")
            .setView(container)
            .setPositiveButton("SAVE KEY", (d, which) -> {
                String input = et.getText().toString().trim();
                RegistryManager.setGoogleMapsApiKey(this, input);
                if (tvGoogleMapsApiKeyVal != null) {
                    String masked = (input.length() > 10) ? input.substring(0, 8) + "..." + input.substring(input.length() - 4) : "Default Key";
                    tvGoogleMapsApiKeyVal.setText("Credentials: " + masked + " (Tap to edit Google Cloud Maps API Key)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Maps Credentials Saved", "New Google Maps API Key persisted. Restart Map to apply.", false, 3500, null, null);
                android.widget.Toast.makeText(this, "✅ Google Maps API Key saved dynamically.", android.widget.Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("RESET DEFAULT", (d, which) -> {
                RegistryManager.setGoogleMapsApiKey(this, "");
                if (tvGoogleMapsApiKeyVal != null) {
                    tvGoogleMapsApiKeyVal.setText("Credentials: Default Key (Tap to edit Google Cloud Maps API Key)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Reset to Default", "Reverted to application default Google Maps key.", false, 2500, null, null);
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

    // [Variables] currentTarget, clean | [Constants] DEFAULT_PRIME_TARGET_ID | [Dependencies] RegistryManager, UiArchitect, FirebaseComm | [Integration Points] SettingsActivity, MapActivity, MapViewModel, LocationRepository
    private void showPrimeTargetDeviceDialog() {
        String currentTarget = RegistryManager.getPrimeTargetDeviceId(this);
        UiArchitect.showInputPromptDialog(this,
            "Primary Child / Ward Target Device",
            "Enter the 8-character Agent ID for the primary Child / Protected Ward to anchor at Priority 0 (Default: 4379A761):",
            currentTarget != null ? currentTarget : "4379A761",
            newTarget -> {
                if (newTarget != null) {
                    String clean = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(newTarget).trim();
                    if (!clean.isEmpty()) {
                        RegistryManager.setPrimeTargetDeviceId(this, clean);
                        if (tvPrimeTargetVal != null) {
                            tvPrimeTargetVal.setText("Target ID: " + clean + " (Priority 0 Monitored Ward)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Target Ward Updated", "Primary monitored ward set to: " + clean, false, 3000, null, null);
                    } else {
                        UiArchitect.showSlideNotificationBanner(this, "Invalid Target ID", "Device ID cannot be empty.", true, 3000, null, null);
                    }
                }
            }
        );
    }

    // [Variables] currentName, clean | [Constants] DEFAULT_PRIME_TARGET_NAME | [Dependencies] RegistryManager, UiArchitect | [Integration Points] SettingsActivity, MapActivity, MapViewModel, LocationRepository
    private void showPrimeTargetNameDialog() {
        String currentName = RegistryManager.getPrimeTargetName(this);
        UiArchitect.showInputPromptDialog(this,
            "Primary Child / Ward Target Name",
            "Enter the display name for the primary Child / Protected Ward to anchor at Priority 0 (Default: RIDDHI):",
            currentName != null ? currentName : "RIDDHI",
            newName -> {
                if (newName != null) {
                    String clean = newName.trim();
                    if (!clean.isEmpty()) {
                        RegistryManager.setPrimeTargetName(this, clean);
                        if (tvPrimeTargetNameVal != null) {
                            tvPrimeTargetNameVal.setText("Target Name: " + clean + " (Priority 0 Monitored Ward Name)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Target Ward Name Updated", "Primary monitored ward name set to: " + clean, false, 3000, null, null);
                    } else {
                        UiArchitect.showSlideNotificationBanner(this, "Invalid Target Name", "Name cannot be empty.", true, 3000, null, null);
                    }
                }
            }
        );
    }

    // [Variables] currentHost, clean, newHost | [Constants] DEFAULT_CIRCLE_HOST_DEVICE_ID | [Dependencies] RegistryManager, UiArchitect, DatabaseHelper, FirebaseComm | [Integration Points] SettingsActivity, DevicePresentationResolver
    private void showCircleHostIdDialog() {
        String currentHost = RegistryManager.getCircleHostDeviceId(this);
        UiArchitect.showInputPromptDialog(this,
            "Root Circle Host Device ID",
            "Enter the 8-character Agent ID for the Circle Host / Master Guardian (Default: 2T929153):",
            currentHost != null ? currentHost : "2T929153",
            newHost -> {
                if (newHost != null) {
                    String clean = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(newHost).trim();
                    if (!clean.isEmpty()) {
                        RegistryManager.setCircleHostDeviceId(this, clean);
                        try {
                            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this)
                                .setDynamicConfig("circle_host_device_id", clean, "Primary Circle Host Device ID", 0L);
                        } catch (Exception ignored) {}
                        if (tvCircleHostIdVal != null) {
                            tvCircleHostIdVal.setText("Host ID: " + clean + " (Master Guardian Silicon Anchor)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Host Device Updated", "Circle Host Anchor set to: " + clean, false, 3000, null, null);
                    } else {
                        UiArchitect.showSlideNotificationBanner(this, "Invalid Host ID", "Host ID cannot be empty.", true, 3000, null, null);
                    }
                }
            }
        );
    }

    // [Variables] currentName, clean, newName | [Constants] DEFAULT_CIRCLE_HOST_NAME | [Dependencies] RegistryManager, UiArchitect, DatabaseHelper | [Integration Points] SettingsActivity, DevicePresentationResolver
    private void showCircleHostNameDialog() {
        String currentName = RegistryManager.getCircleHostName(this);
        UiArchitect.showInputPromptDialog(this,
            "Root Circle Host Name",
            "Enter the display name for the Master Guardian / Host (Default: JEET):",
            currentName != null ? currentName : "JEET",
            newName -> {
                if (newName != null) {
                    String clean = newName.trim();
                    if (!clean.isEmpty()) {
                        RegistryManager.setCircleHostName(this, clean);
                        try {
                            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this)
                                .setDynamicConfig("circle_host_name", clean, "Primary Circle Host Name", 0L);
                        } catch (Exception ignored) {}
                        if (tvCircleHostNameVal != null) {
                            tvCircleHostNameVal.setText("Host Name: " + clean + " (Circle Authority Anchor)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Host Name Updated", "Circle Host display name set to: " + clean, false, 3000, null, null);
                    } else {
                        UiArchitect.showSlideNotificationBanner(this, "Invalid Host Name", "Name cannot be empty.", true, 3000, null, null);
                    }
                }
            }
        );
    }

    // [Variables] currentGroup, clean, newGroup | [Constants] None | [Dependencies] RegistryManager, UiArchitect, DatabaseHelper | [Integration Points] SettingsActivity, DevicePresentationResolver, GroupMemberTable
    private void showActiveCircleGroupDialog() {
        String currentGroup = RegistryManager.getActiveGroup(this);
        if (currentGroup == null || currentGroup.isEmpty() || currentGroup.equals("DEFAULT_GROUP")) {
            currentGroup = RegistryManager.getEffectiveActiveGroup(this);
        }
        UiArchitect.showInputPromptDialog(this,
            "Primary Circle Group Partition",
            "Enter the shared group partition (e.g. GRP_2T929153):",
            currentGroup,
            newGroup -> {
                if (newGroup != null) {
                    String clean = newGroup.trim().toUpperCase();
                    if (!clean.isEmpty()) {
                        RegistryManager.setActiveGroup(this, clean);
                        RegistryManager.setDeviceCorrespondingGroup(this, clean);
                        try {
                            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this)
                                .setDynamicConfig("active_group", clean, "Active circle/group partition", 0L);
                        } catch (Exception ignored) {}
                        if (tvActiveCircleGroupVal != null) {
                            tvActiveCircleGroupVal.setText("Active Circle: " + clean + " (Shared SSOT Group)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Circle Group Updated", "Node assigned to group: " + clean, false, 3000, null, null);
                    } else {
                        UiArchitect.showSlideNotificationBanner(this, "Invalid Group", "Group partition cannot be empty.", true, 3000, null, null);
                    }
                }
            }
        );
    }

    // [Variables] currentRole, roles, selectedIdx, cleanRole | [Constants] None | [Dependencies] RegistryManager, UiArchitect, DatabaseHelper | [Integration Points] SettingsActivity, DevicePresentationResolver, GroupMemberTable
    private void showLocalDeviceRoleDialog() {
        String currentRole = RegistryManager.getRoleFamily(this, "Master Guardian");
        String[] roles = {"Master Guardian", "Co-Guardian", "Member", "Child", "Spouse", "Friend"};
        int selectedIdx = -1;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equalsIgnoreCase(currentRole)) {
                selectedIdx = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("👑 Local Device Role in Circle")
            .setSingleChoiceItems(roles, selectedIdx, (dialog, which) -> {
                dialog.dismiss();
                if (which >= 0 && which < roles.length) {
                    String chosenRole = roles[which];
                    RegistryManager.setRoleFamily(this, chosenRole);
                    if (chosenRole.equalsIgnoreCase("Friend")) {
                        RegistryManager.setRoleFriend(this, chosenRole);
                    }
                    try {
                        String selfAgent = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
                        com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this)
                            .updateUnifiedEntity(selfAgent, null, null, chosenRole, null);
                    } catch (Exception ignored) {}
                    if (tvLocalDeviceRoleVal != null) {
                        tvLocalDeviceRoleVal.setText("Assigned Role: " + chosenRole + " (Social Authority Tier)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Device Role Updated", "Assigned circle role: " + chosenRole, false, 3000, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    // [Variables] current, updated | [Constants] None | [Dependencies] RegistryManager, UiArchitect | [Integration Points] FirebaseComm, SettingsActivity
    private void toggleUnifiedDeviceIdSync() {
        boolean current = RegistryManager.isUnifiedDeviceIdSyncEnabled(this);
        boolean updated = !current;
        RegistryManager.setUnifiedDeviceIdSyncEnabled(this, updated);
        if (tvUnifiedDeviceIdSyncVal != null) {
            tvUnifiedDeviceIdSyncVal.setText(updated
                ? "Status: ACTIVE (Canonical single-path egress, zero HW_ duplication)"
                : "Status: DUAL-PATH (Legacy mirror enabled: writes to both canonical & HW_)");
        }
        UiArchitect.showSlideNotificationBanner(this,
            "RTDB Sync Mode Updated",
            updated ? "Unified DeviceID SSOT mode active (zero duplicate writes)." : "Legacy dual-path sync active.",
            false, 3000, null, null);
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
            "🛡️ Custom Dynamic Setup (Configure Still, Vehicle, Walking...)"
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
                dialog.dismiss();
                if (which == 3) {
                    showCustomMotionDutyDialog();
                } else {
                    long[] chosen = values[which];
                    RegistryManager.setMotionStillIntervalMs(this, chosen[0]);
                    RegistryManager.setMotionVehicleIntervalMs(this, chosen[1]);
                    RegistryManager.setMotionWalkingIntervalMs(this, chosen[2]);
                    if (tvMotionDutyVal != null) {
                        tvMotionDutyVal.setText("Still: " + (chosen[0] / 1000) + "s | Car: " + (chosen[1] / 1000) + "s | Walk: " + (chosen[2] / 1000) + "s");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Duty Cycles Saved", "Still: " + (chosen[0] / 1000) + "s, Car: " + (chosen[1] / 1000) + "s, Walk: " + (chosen[2] / 1000) + "s", false, 3000, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCustomMotionDutyDialog() {
        long currStill = RegistryManager.getMotionStillIntervalMs(this) / 1000L;
        long currVeh = RegistryManager.getMotionVehicleIntervalMs(this) / 1000L;
        long currWalk = RegistryManager.getMotionWalkingIntervalMs(this) / 1000L;

        showCustomNumericInputDialog("🛑 Still Motion Interval", "Seconds (30 - 3600)", 30, 3600, (int) currStill, "s", stillSec -> {
            showCustomNumericInputDialog("🚗 Vehicle Motion Interval", "Seconds (3 - 120)", 3, 120, (int) currVeh, "s", vehSec -> {
                showCustomNumericInputDialog("🚶 Walking Motion Interval", "Seconds (5 - 300)", 5, 300, (int) currWalk, "s", walkSec -> {
                    RegistryManager.setMotionStillIntervalMs(this, stillSec * 1000L);
                    RegistryManager.setMotionVehicleIntervalMs(this, vehSec * 1000L);
                    RegistryManager.setMotionWalkingIntervalMs(this, walkSec * 1000L);
                    if (tvMotionDutyVal != null) {
                        tvMotionDutyVal.setText("Still: " + stillSec + "s | Car: " + vehSec + "s | Walk: " + walkSec + "s");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Duty Cycles Saved", "Still: " + stillSec + "s, Car: " + vehSec + "s, Walk: " + walkSec + "s", false, 3000, null, null);
                });
            });
        });
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

    // [Variables] current, values, customLabel, options, selected, chosen | [Constants] DEFAULT_DISTANT_METERS = 50 | [Dependencies] RegistryManager, AlertDialog, UiArchitect | [Integration Points] MapActivity, SettingsActivity
    private void showDistantDeviceThresholdDialog() {
        int current = RegistryManager.getDistantDeviceThresholdMeters(this);
        int[] values = {25, 50, 100, 200, 500, 1000};
        String customLabel = "✏️ Custom Distance (" + current + "m)...";
        String[] options = {
            "25m (Immediate Vicinity)",
            "50m (Standard Proximity — Default)",
            "100m (Close Proximity — Tight Clustering)",
            "200m (Medium Range)",
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
                    showCustomNumericInputDialog("📡 Custom Distant Device Threshold", "Meters (10 - 50000)", 10, 50000, current, "m", chosen -> {
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

    // [Variables] current, values, options, selected, chosen | [Constants] None | [Dependencies] RegistryManager, UiArchitect, AlertDialog | [Integration Points] MapActivity Render Throttling
    private void showMapRenderThrottleDialog() {
        long current = RegistryManager.getMapRenderThrottleMs(this);
        long[] values = {100, 250, 350, 500, 1000, 2000};
        String customLabel = "✏️ Custom Window (" + current + "ms)...";
        String[] options = {
            "100ms (High Frequency — Flagship Devices)",
            "250ms (Responsive — Balanced Devices)",
            "350ms (Standard — Recommended / Default)",
            "500ms (Conservative — Battery Saver)",
            "1000ms (1 Second — Low Resource Mode)",
            "2000ms (2 Seconds — Aggressive Battery Mode)",
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
            .setTitle("⏱️ Map Render Throttle Window")
            .setMessage("Controls the minimum interval between map marker layout updates. Higher values reduce CPU/GPU thermal load and battery consumption on mid-range devices.")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("⏱️ Custom Render Throttle", "Milliseconds (50 - 5000)", 50, 5000, (int) current, "ms", chosen -> {
                        RegistryManager.setMapRenderThrottleMs(this, (long) chosen);
                        if (tvMapThrottleVal != null) {
                            tvMapThrottleVal.setText("Render Throttle: " + chosen + "ms (Sliding window prevents marker jitter)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Throttle Window Updated", "Map renders throttled to " + chosen + "ms window", false, 2500, null, null);
                    });
                } else {
                    long chosen = values[which];
                    RegistryManager.setMapRenderThrottleMs(this, chosen);
                    if (tvMapThrottleVal != null) {
                        tvMapThrottleVal.setText("Render Throttle: " + chosen + "ms (Sliding window prevents marker jitter)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Throttle Window Updated", "Map renders throttled to " + chosen + "ms window", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    // [Variables] current, values, options, selected, chosen | [Constants] None | [Dependencies] RegistryManager, UiArchitect, AlertDialog | [Integration Points] MultiMarkerEngine Anti-Overlap Calculations
    private void showClusterSpacingDialog() {
        int current = (int) Math.round(RegistryManager.getClusterSpacingDp(this));
        int[] values = {40, 50, 64, 75, 90};
        String customLabel = "✏️ Custom Spacing (" + current + "dp)...";
        String[] options = {
            "40dp (Tight — Maximum Density)",
            "50dp (Compact — Medium Density)",
            "64dp (Standard — Recommended / Default)",
            "75dp (Relaxed — High Visibility)",
            "90dp (Wide — Maximum Separation)",
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
            .setTitle("📐 Anti-Overlap Cluster Spacing")
            .setMessage("Sets the physical pixel radius used to fan out overlapping avatar markers when multiple devices are co-located in the same building or safe zone.")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("📐 Custom Cluster Spacing", "Density Pixels (20 - 150)", 20, 150, current, "dp", chosen -> {
                        RegistryManager.setClusterSpacingDp(this, chosen);
                        if (tvClusterSpacingVal != null) {
                            tvClusterSpacingVal.setText("Cluster Spacing: " + chosen + "dp (Minimum avatar separation distance)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Cluster Spacing Updated", "Avatar offset spacing set to " + chosen + "dp", false, 2500, null, null);
                    });
                } else {
                    int chosen = values[which];
                    RegistryManager.setClusterSpacingDp(this, chosen);
                    if (tvClusterSpacingVal != null) {
                        tvClusterSpacingVal.setText("Cluster Spacing: " + chosen + "dp (Minimum avatar separation distance)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Cluster Spacing Updated", "Avatar offset spacing set to " + chosen + "dp", false, 2500, null, null);
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

    private void updateDatabaseEngineTtlSubtitle() {
        if (tvDatabaseEngineTtlVal != null) {
            String ttlSub = "Sync Queue: " + RegistryManager.getSyncQueueTtlHours(this) + "h • Logs: " + RegistryManager.getSysLogTtlHours(this) + "h • Page Limit: " + RegistryManager.getDefaultPageLimit(this);
            tvDatabaseEngineTtlVal.setText(ttlSub);
        }
    }

    private void updateSparkPlanQuotaSubtitle() {
        if (tvSparkPlanQuotaVal != null) {
            String currentTier = RegistryManager.getFirebasePlanTier(this);
            int dailyMb = RegistryManager.getSparkDailyDownloadMb(this);
            int monthlyGb = RegistryManager.getSparkMonthlyDownloadGb(this);
            String resetIst = RegistryManager.getSparkDailyResetTimeIst(this);
            tvSparkPlanQuotaVal.setText("Plan: " + currentTier + " • Daily Cap: " + dailyMb + " MB (Resets ~" + resetIst + ") • Monthly Cap: " + monthlyGb + " GB");
        }
        if (settingsModel != null) {
            settingsModel.setFirebasePlanTier(RegistryManager.getFirebasePlanTier(this));
            settingsModel.setSparkDailyDownloadMb(RegistryManager.getSparkDailyDownloadMb(this));
            settingsModel.setSparkMonthlyDownloadGb(RegistryManager.getSparkMonthlyDownloadGb(this));
        }
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
                updateDatabaseEngineTtlSubtitle();
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
                updateDatabaseEngineTtlSubtitle();
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
                updateDatabaseEngineTtlSubtitle();
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
                updateDatabaseEngineTtlSubtitle();
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
                updateDatabaseEngineTtlSubtitle();
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
                updateDatabaseEngineTtlSubtitle();
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
                updateDatabaseEngineTtlSubtitle();
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
        String customLabel = "✏️ Custom Radius (" + (int)current + "m)...";
        String[] options = {
            "⚡ 5 Meters (Ultra Sensitive • High Battery/Data)",
            "⚡ 10 Meters (City Walking • Moderate Tolerance)",
            "🛡️ 15 Meters (Default Recommended • 97% Bandwidth Cut)",
            "📅 25 Meters (Suburban Road Filter • High Tolerance)",
            "📅 50 Meters (Macro Station Gate • Minimalist Data)",
            customLabel
        };
        float[] vals = {5.0f, 10.0f, 15.0f, 25.0f, 50.0f};
        int selected = options.length - 1;
        for (int i = 0; i < vals.length; i++) {
            if (Math.abs(vals[i] - current) < 0.1f) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("🛡️ Spatial Dead-Reckoning Noise Gate")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("🛡️ Custom Dead-Reckoning Radius", "Meters (1 - 200)", 1, 200, (int)current, "m", chosen -> {
                        RegistryManager.setDeadReckoningRadiusMeters(this, (float) chosen);
                        if (settingsModel != null) settingsModel.setDeadReckoningRadiusMeters((float) chosen);
                        if (tvDeadReckoningVal != null) {
                            tvDeadReckoningVal.setText("Noise Gate: " + chosen + "m • Suppresses Stationary GPS (97% Data Saving)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Dead-Reckoning Updated", "Noise gate set to " + chosen + "m. Stationary GPS uploads suppressed.", false, 2500, null, null);
                    });
                } else {
                    float chosen = vals[which];
                    RegistryManager.setDeadReckoningRadiusMeters(this, chosen);
                    if (settingsModel != null) settingsModel.setDeadReckoningRadiusMeters(chosen);
                    if (tvDeadReckoningVal != null) {
                        tvDeadReckoningVal.setText("Noise Gate: " + (int)chosen + "m • Suppresses Stationary GPS (97% Data Saving)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Dead-Reckoning Updated", "Noise gate set to " + (int)chosen + "m. Stationary GPS uploads suppressed.", false, 2500, null, null);
                }
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
        String customLabel = "✏️ Custom Cadence (" + (current / 1000) + "s)...";
        String[] options = {
            "⚡ 60 Seconds (1 Minute • Real-Time Dynamic Active)",
            "⚡ 120 Seconds (2 Minutes • Fast Mesh Heartbeat)",
            "🛡️ 180 Seconds (3 Minutes • Default Recommended)",
            "📅 300 Seconds (5 Minutes • Ultra Lean Bandwidth)",
            "📅 600 Seconds (10 Minutes • Minimalist Standby)",
            customLabel
        };
        long[] vals = {60_000L, 120_000L, 180_000L, 300_000L, 600_000L};
        int selected = options.length - 1;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("💓 Presence Pulse Heartbeat Interval")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomNumericInputDialog("💓 Custom Presence Pulse Interval", "Seconds (30 - 1800)", 30, 1800, (int)(current / 1000), "s", chosen -> {
                        long chosenMs = chosen * 1000L;
                        RegistryManager.setPresencePulseIntervalMs(this, chosenMs);
                        if (settingsModel != null) settingsModel.setPresencePulseIntervalMs(chosenMs);
                        if (tvPresencePulseVal != null) {
                            tvPresencePulseVal.setText("Cadence: " + chosen + "s • 28-Byte Presence Pulse (Keeps Server Online)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Presence Pulse Updated", "Stationary heartbeat interval set to " + chosen + " seconds.", false, 2500, null, null);
                    });
                } else {
                    long chosen = vals[which];
                    RegistryManager.setPresencePulseIntervalMs(this, chosen);
                    if (settingsModel != null) settingsModel.setPresencePulseIntervalMs(chosen);
                    if (tvPresencePulseVal != null) {
                        tvPresencePulseVal.setText("Cadence: " + (chosen / 1000) + "s • 28-Byte Presence Pulse (Keeps Server Online)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Presence Pulse Updated", "Stationary heartbeat interval set to " + (chosen / 1000) + " seconds.", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    // [Variables]: agentId, newGroup | [Constants]: None | [Dependencies]: FirebaseAuth, RegistryManager, FirebaseComm | [Integration Points]: Real-Time RTDB Device Group Sync
    private void syncDeviceGroupToCloud(String newGroup) {
        try {
            String agentId = RegistryManager.getAgentId(this);
            if (agentId == null || agentId.isEmpty()) {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    agentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                }
            }
            if (agentId != null && !agentId.isEmpty()) {
                FirebaseComm.INSTANCE.updateDeviceGroup(agentId, newGroup, success -> {
                    if (success != null && success) {
                        SystemLogger.i("SettingsActivity", "Device group partition '" + newGroup + "' synchronized to Firebase cloud mesh.");
                    }
                    return kotlin.Unit.INSTANCE;
                });
            }
        } catch (Exception e) {
            SystemLogger.e("SettingsActivity", "Cloud group sync error: " + e.getMessage());
        }
    }

    // [Variables] intervalSec: long, mode: String, warning: String | [Constants] None | [Dependencies] tvPingIntervalVal, settingsModel, settingsController | [Integration Points] OOPS Two-Way Binding
    private void updatePingCadenceSubtitle(long intervalSec, String mode) {
        if (tvPingIntervalVal != null) {
            String warning = intervalSec < 30 ? " ⚠️ HIGH BATTERY DRAIN" : "";
            tvPingIntervalVal.setText("Active Cadence: " + intervalSec + "s" + warning + " | " + mode);
        }
        if (settingsModel != null) {
            settingsModel.setPingIntervalSec((int) intervalSec);
        }
    }
    // [Constants]: Preset group keys
    // [Dependencies]: RegistryManager, UiArchitect, AlertDialog
    // [Integration Points]: Database group-by hierarchy & multi-tenancy SSOT
    private void showDeviceGroupDialog() {
        String currentGroup = RegistryManager.getDeviceCorrespondingGroup(this);
        String[] presetGroups = {
            "PRIMARY_CIRCLE",
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
                    syncDeviceGroupToCloud(chosen);
                    if (settingsModel != null) settingsModel.setDeviceCorrespondingGroup(chosen);
                    if (tvDeviceGroupVal != null) {
                        tvDeviceGroupVal.setText("Hierarchy Group: " + chosen + " • Relative Group-By Partition");
                    }
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
                    syncDeviceGroupToCloud(val);
                    if (settingsModel != null) settingsModel.setDeviceCorrespondingGroup(val);
                    if (tvDeviceGroupVal != null) {
                        tvDeviceGroupVal.setText("Hierarchy Group: " + val + " • Relative Group-By Partition");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Device Group Updated", "Node assigned to: " + val, false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showRelativeRolesMapDialog() {
        String current = RegistryManager.getRelativeRolesMap(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(false);
        input.setText(current);
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
            .setTitle("⚙️ RELATIVE ROLES MATRIX")
            .setMessage("Configure dynamic relative role mappings.\nFormat: OBSERVER>TARGET:Role;...\n(e.g. 2T929153>2C811042:Friend;2C811042>4379A761:Co-Guardian):")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                String val = input.getText().toString().trim();
                RegistryManager.setRelativeRolesMap(this, val);
                if (tvRelativeRolesMapVal != null) {
                    tvRelativeRolesMapVal.setText("Relative Roles Matrix: " + val);
                }
                UiArchitect.showSlideNotificationBanner(this, "Relational Role Matrix Saved", "Updated dynamic perspective roles.", false, 2500, null, null);
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showFleetHardwareMapDialog() {
        String current = RegistryManager.getString(this, RegistryManager.KEY_FLEET_HARDWARE_MAP, RegistryManager.DEFAULT_FLEET_HARDWARE_MAP);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(false);
        input.setText(current);
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
            .setTitle("Dynamic Fleet Hardware Map")
            .setMessage("Configure comma-separated MODEL:AGENT_ID mappings (e.g. VIOLET:2T929153,WHYRED:2C811042):")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                String val = input.getText().toString().trim();
                if (!val.isEmpty()) {
                    RegistryManager.putString(this, RegistryManager.KEY_FLEET_HARDWARE_MAP, val);
                    try {
                        com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this)
                            .setDynamicConfig("fleet_hardware_map", val, "Dynamic Hardware to Agent ID Fleet Mapping", 0L);
                    } catch (Exception ignored) {}
                    if (tvFleetHardwareMapVal != null) {
                        tvFleetHardwareMapVal.setText("Dynamic Silicon Fleet: " + val);
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Fleet Mapping Saved", "Updated dynamic hardware roster.", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showDecommissionedAgentIdsDialog() {
        String current = RegistryManager.getString(this, RegistryManager.KEY_DECOMMISSIONED_AGENT_IDS, RegistryManager.DEFAULT_DECOMMISSIONED_AGENT_IDS);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(false);
        input.setText(current);
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
            .setTitle("Decommissioned Ghost Nodes")
            .setMessage("Configure comma-separated Agent IDs to filter from sync & maps (e.g. A7D5968D):")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                String val = input.getText().toString().trim();
                java.util.Set<String> set = new java.util.HashSet<>();
                for (String s : val.split(",")) {
                    String clean = s.trim().toUpperCase();
                    if (!clean.isEmpty()) set.add(clean);
                }
                RegistryManager.setDecommissionedAgentIds(this, set);
                if (tvDecommissionedAgentIdsVal != null) {
                    tvDecommissionedAgentIdsVal.setText("Excluded Ghost Nodes: " + val);
                }
                UiArchitect.showSlideNotificationBanner(this, "Ghost Nodes Blacklist Updated", "Excluded " + set.size() + " agent IDs from telemetry.", false, 2500, null, null);
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
                            com.jdroidx.jfindme.data.repository.DeviceRepository db = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(getApplicationContext());
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
                if (settingsModel != null) settingsModel.setStorageRetentionDays(chosen);
                if (tvStorageRetentionVal != null) {
                    tvStorageRetentionVal.setText("Rolling Retention: " + chosen + " Days • WAL Compaction");
                }
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
                    updateSparkPlanQuotaSubtitle();
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
                updateSparkPlanQuotaSubtitle();
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
                updateSparkPlanQuotaSubtitle();
                UiArchitect.showSlideNotificationBanner(this, "Monthly Limit Updated", "Monthly quota cap set to " + chosen + " GB", false, 2500, null, null);
                dialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyArrivalRadiusDialog() {
        float current = RegistryManager.getButterflyArrivalRadius(this);
        String[] options = new String[]{
            "25m (Tactical / Precision Building)",
            "50m (Standard Perimeter)",
            "100m (Neighborhood Block)",
            "250m (Staging / Large Campus)",
            "500m (Rural / Wide Area)",
            "Custom Perimeter..."
        };
        float[] values = new float[]{25.0f, 50.0f, 100.0f, 250.0f, 500.0f, -1.0f};
        int selected = -1;
        for (int i = 0; i < values.length - 1; i++) {
            if (Math.abs(values[i] - current) < 1.0f) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("Butterfly Arrival Perimeter")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomButterflyArrivalRadiusPrompt();
                } else {
                    float chosen = values[which];
                    RegistryManager.setButterflyArrivalRadius(this, chosen);
                    if (settingsModel != null) settingsModel.setButterflyArrivalRadiusM(chosen);
                    if (tvButterflyArrivalRadiusVal != null) {
                        tvButterflyArrivalRadiusVal.setText("Perimeter: " + ((int) chosen) + "m (Dynamic geofence arrival detection)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Arrival Perimeter Updated", "Perimeter set to " + ((int) chosen) + "m", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCustomButterflyArrivalRadiusPrompt() {
        float current = RegistryManager.getButterflyArrivalRadius(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((int) current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = pad;
        lp.rightMargin = pad;
        input.setLayoutParams(lp);
        container.addView(input);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Custom Arrival Perimeter")
            .setMessage("Enter arrival perimeter radius in meters (10 - 5000):")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    float val = Float.parseFloat(input.getText().toString().trim());
                    if (val >= 10.0f && val <= 5000.0f) {
                        RegistryManager.setButterflyArrivalRadius(this, val);
                        if (settingsModel != null) settingsModel.setButterflyArrivalRadiusM(val);
                        if (tvButterflyArrivalRadiusVal != null) {
                            tvButterflyArrivalRadiusVal.setText("Perimeter: " + ((int) val) + "m (Dynamic geofence arrival detection)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Arrival Perimeter Updated", "Perimeter set to " + ((int) val) + "m", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Please enter a radius between 10m and 5000m", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {}
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyExpiryDialog() {
        int current = RegistryManager.getButterflyExpiryHours(this);
        String[] options = new String[]{
            "1 Hour (Short Rendezvous)",
            "2 Hours (Standard Meeting)",
            "4 Hours (Recommended Half-Day)",
            "8 Hours (Full Working Shift)",
            "24 Hours (Full Day Operations)",
            "Custom Lifespan..."
        };
        int[] values = new int[]{1, 2, 4, 8, 24, -1};
        int selected = -1;
        for (int i = 0; i < values.length - 1; i++) {
            if (values[i] == current) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("Rendezvous Default Lifespan")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomButterflyExpiryPrompt();
                } else {
                    int chosen = values[which];
                    RegistryManager.setButterflyExpiryHours(this, chosen);
                    if (settingsModel != null) settingsModel.setButterflyExpiryHours(chosen);
                    if (tvButterflyExpiryVal != null) {
                        tvButterflyExpiryVal.setText("Lifespan: " + chosen + " Hours (Default auto-archive TTL window)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Lifespan Updated", "Rendezvous expiry set to " + chosen + " hours", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCustomButterflyExpiryPrompt() {
        int current = RegistryManager.getButterflyExpiryHours(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = pad;
        lp.rightMargin = pad;
        input.setLayoutParams(lp);
        container.addView(input);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Custom Rendezvous Lifespan")
            .setMessage("Enter default lifespan in hours (1 - 168):")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    int val = Integer.parseInt(input.getText().toString().trim());
                    if (val >= 1 && val <= 168) {
                        RegistryManager.setButterflyExpiryHours(this, val);
                        if (settingsModel != null) settingsModel.setButterflyExpiryHours(val);
                        if (tvButterflyExpiryVal != null) {
                            tvButterflyExpiryVal.setText("Lifespan: " + val + " Hours (Default auto-archive TTL window)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Lifespan Updated", "Rendezvous expiry set to " + val + " hours", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Enter lifespan between 1 and 168 hours", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {}
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyHysteresisDialog() {
        float current = RegistryManager.getButterflyHysteresisMeters(this);
        String[] options = new String[]{
            "5m (Tight / Low Jitter Area)",
            "10m (City Street / Nominal)",
            "15m (Recommended Standard Deadband)",
            "25m (Wide / High Urban Multi-path)",
            "50m (Rural / Low Accuracy)",
            "Custom Deadband..."
        };
        float[] values = new float[]{5.0f, 10.0f, 15.0f, 25.0f, 50.0f, -1.0f};
        int selected = -1;
        for (int i = 0; i < values.length - 1; i++) {
            if (Math.abs(values[i] - current) < 1.0f) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("Arrival Hysteresis Deadband")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomButterflyHysteresisPrompt();
                } else {
                    float chosen = values[which];
                    RegistryManager.setButterflyHysteresisMeters(this, chosen);
                    if (settingsModel != null) settingsModel.setButterflyHysteresisM(chosen);
                    if (tvButterflyHysteresisVal != null) {
                        tvButterflyHysteresisVal.setText("Deadband: " + ((int) chosen) + "m (Anti-flutter arrival latch threshold)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Hysteresis Updated", "Arrival deadband set to " + ((int) chosen) + "m", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCustomButterflyHysteresisPrompt() {
        float current = RegistryManager.getButterflyHysteresisMeters(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((int) current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = pad;
        lp.rightMargin = pad;
        input.setLayoutParams(lp);
        container.addView(input);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Custom Hysteresis Deadband")
            .setMessage("Enter deadband filter in meters (2 - 200):")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    float val = Float.parseFloat(input.getText().toString().trim());
                    if (val >= 2.0f && val <= 200.0f) {
                        RegistryManager.setButterflyHysteresisMeters(this, val);
                        if (settingsModel != null) settingsModel.setButterflyHysteresisM(val);
                        if (tvButterflyHysteresisVal != null) {
                            tvButterflyHysteresisVal.setText("Deadband: " + ((int) val) + "m (Anti-flutter arrival latch threshold)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Hysteresis Updated", "Arrival deadband set to " + ((int) val) + "m", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Enter deadband between 2m and 200m", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {}
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyMaxDistanceDialog() {
        float current = RegistryManager.getButterflyMaxDistanceKm(this);
        String[] options = new String[]{
            "100 km (Local Metropolitan)",
            "250 km (Regional District)",
            "500 km (Recommended Standard Threshold)",
            "1000 km (National Inter-State)",
            "2500 km (Cross-Continental)",
            "Custom Safeguard..."
        };
        float[] values = new float[]{100.0f, 250.0f, 500.0f, 1000.0f, 2500.0f, -1.0f};
        int selected = -1;
        for (int i = 0; i < values.length - 1; i++) {
            if (Math.abs(values[i] - current) < 1.0f) { selected = i; break; }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("Distance Safeguard Warning Threshold")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    showCustomButterflyMaxDistancePrompt();
                } else {
                    float chosen = values[which];
                    RegistryManager.setButterflyMaxDistanceKm(this, chosen);
                    if (settingsModel != null) settingsModel.setButterflyMaxDistanceKm(chosen);
                    if (tvButterflyMaxDistanceVal != null) {
                        tvButterflyMaxDistanceVal.setText("Safeguard: " + ((int) chosen) + " km (Human-in-the-loop warning confirmation)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Safeguard Updated", "Warning threshold set to " + ((int) chosen) + " km", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCustomButterflyMaxDistancePrompt() {
        float current = RegistryManager.getButterflyMaxDistanceKm(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((int) current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = pad;
        lp.rightMargin = pad;
        input.setLayoutParams(lp);
        container.addView(input);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Custom Distance Safeguard")
            .setMessage("Enter safeguard distance in km (10 - 20000):")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    float val = Float.parseFloat(input.getText().toString().trim());
                    if (val >= 10.0f && val <= 20000.0f) {
                        RegistryManager.setButterflyMaxDistanceKm(this, val);
                        if (settingsModel != null) settingsModel.setButterflyMaxDistanceKm(val);
                        if (tvButterflyMaxDistanceVal != null) {
                            tvButterflyMaxDistanceVal.setText("Safeguard: " + ((int) val) + " km (Human-in-the-loop warning confirmation)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Safeguard Updated", "Warning threshold set to " + ((int) val) + " km", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Enter distance between 10 km and 20000 km", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {}
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyInvitationTimeoutDialog() {
        int current = RegistryManager.getButterflyInvitationTimeoutHours(this);
        String[] options = {
            "6 Hours (Fast-paced)",
            "12 Hours (Half-day)",
            "24 Hours (Standard Default)",
            "48 Hours (Extended)",
            "Custom Hours..."
        };
        int[] values = {6, 12, 24, 48};
        new android.app.AlertDialog.Builder(this)
            .setTitle("Invitation Timeout Duration")
            .setItems(options, (dialog, which) -> {
                if (which == 4) {
                    showCustomButterflyInvitationTimeoutPrompt();
                } else {
                    int chosen = values[which];
                    RegistryManager.setButterflyInvitationTimeoutHours(this, chosen);
                    if (settingsModel != null) settingsModel.setButterflyInvitationTimeoutHours(chosen);
                    if (tvButterflyInvitationTimeoutVal != null) {
                        tvButterflyInvitationTimeoutVal.setText("Timeout: " + chosen + " Hours (Auto-revoke stale pending invitations)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Timeout Configured", "Pending invitations expire after " + chosen + " hours", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCustomButterflyInvitationTimeoutPrompt() {
        int current = RegistryManager.getButterflyInvitationTimeoutHours(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = pad;
        lp.rightMargin = pad;
        input.setLayoutParams(lp);
        container.addView(input);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Custom Invitation Timeout")
            .setMessage("Enter timeout in hours (1 - 168):")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    int val = Integer.parseInt(input.getText().toString().trim());
                    if (val >= 1 && val <= 168) {
                        RegistryManager.setButterflyInvitationTimeoutHours(this, val);
                        if (settingsModel != null) settingsModel.setButterflyInvitationTimeoutHours(val);
                        if (tvButterflyInvitationTimeoutVal != null) {
                            tvButterflyInvitationTimeoutVal.setText("Timeout: " + val + " Hours (Auto-revoke stale pending invitations)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Timeout Configured", "Pending invitations expire after " + val + " hours", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Enter value between 1 and 168 hours", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {}
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyOverdueGraceDialog() {
        int current = RegistryManager.getButterflyOverdueGraceMinutes(this);
        String[] options = {
            "30 Minutes (Tight arrival window)",
            "60 Minutes (1 Hour)",
            "120 Minutes (2 Hours - Default)",
            "240 Minutes (4 Hours)",
            "Custom Minutes..."
        };
        int[] values = {30, 60, 120, 240};
        new android.app.AlertDialog.Builder(this)
            .setTitle("Overdue Grace Period")
            .setItems(options, (dialog, which) -> {
                if (which == 4) {
                    showCustomButterflyOverdueGracePrompt();
                } else {
                    int chosen = values[which];
                    RegistryManager.setButterflyOverdueGraceMinutes(this, chosen);
                    if (settingsModel != null) settingsModel.setButterflyOverdueGraceMinutes(chosen);
                    if (tvButterflyOverdueGraceVal != null) {
                        tvButterflyOverdueGraceVal.setText("Grace Window: " + chosen + " Minutes (Arrival window before auto-archive)");
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Grace Period Configured", "Arrival window set to " + chosen + " minutes past rendezvous", false, 2500, null, null);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showCustomButterflyOverdueGracePrompt() {
        int current = RegistryManager.getButterflyOverdueGraceMinutes(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = pad;
        lp.rightMargin = pad;
        input.setLayoutParams(lp);
        container.addView(input);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Custom Overdue Grace Period")
            .setMessage("Enter grace period in minutes (5 - 1440):")
            .setView(container)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    int val = Integer.parseInt(input.getText().toString().trim());
                    if (val >= 5 && val <= 1440) {
                        RegistryManager.setButterflyOverdueGraceMinutes(this, val);
                        if (settingsModel != null) settingsModel.setButterflyOverdueGraceMinutes(val);
                        if (tvButterflyOverdueGraceVal != null) {
                            tvButterflyOverdueGraceVal.setText("Grace Window: " + val + " Minutes (Arrival window before auto-archive)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Grace Period Configured", "Arrival window set to " + val + " minutes past rendezvous", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Enter value between 5 and 1440 minutes", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {}
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyForegroundSyncDialog() {
        int current = RegistryManager.getButterflyForegroundSyncIntervalSec(this);
        String[] options = {
            "2 Seconds (High precision / Fast motion)",
            "4 Seconds (Standard Balance - Default)",
            "8 Seconds (Battery Saver)",
            "15 Seconds (Low Bandwidth)"
        };
        int[] values = {2, 4, 8, 15};
        new android.app.AlertDialog.Builder(this)
            .setTitle("Foreground Telemetry Cadence")
            .setItems(options, (dialog, which) -> {
                int chosen = values[which];
                RegistryManager.setButterflyForegroundSyncIntervalSec(this, chosen);
                if (settingsModel != null) settingsModel.setButterflyForegroundSyncIntervalSec(chosen);
                if (tvButterflyForegroundSyncVal != null) {
                    tvButterflyForegroundSyncVal.setText("Interval: " + chosen + "s (Active rendezvous GPS motion cadence)");
                }
                UiArchitect.showSlideNotificationBanner(this, "Cadence Configured", "Foreground rendezvous sync set to every " + chosen + " seconds", false, 2500, null, null);
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyMaxAvailabilityDialog() {
        int current = RegistryManager.getButterflyMaxAvailabilityDays(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Max Availability Days (1 - 30)")
            .setMessage("Set the maximum number of days a Butterfly Meeting Point remains active/available before automated archival and cloud purge.\n\nExchange of tracking telemetry and vectors only occurs during this window.")
            .setView(input)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    int val = Integer.parseInt(input.getText().toString().trim());
                    if (val >= 1 && val <= 30) {
                        RegistryManager.setButterflyMaxAvailabilityDays(this, val);
                        if (settingsModel != null) settingsModel.setButterflyMaxAvailabilityDays(val);
                        if (tvButterflyMaxAvailabilityVal != null) {
                            tvButterflyMaxAvailabilityVal.setText("Availability: " + val + " Days (Rendezvous point retention before archival purge)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Retention Window Saved", "Meeting point max availability set to " + val + " days", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Enter value between 1 and 30 days", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ex) {
                    android.widget.Toast.makeText(this, "Invalid number", android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyNominalSpeedDialog() {
        float current = RegistryManager.getButterflyNominalSpeedKmh(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf((int) current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nominal Travel Speed (10 - 140 km/h)")
            .setMessage("Set the fallback average speed used to calculate real-time estimated travel time (ETA) when a device is stationary or walking.\n\nRelative to rendezvous distance, dynamically adjusts predicted arrival times.")
            .setView(input)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    float val = Float.parseFloat(input.getText().toString().trim());
                    if (val >= 10.0f && val <= 140.0f) {
                        RegistryManager.setButterflyNominalSpeedKmh(this, val);
                        if (settingsModel != null) settingsModel.setButterflyNominalSpeedKmh(val);
                        if (tvButterflyNominalSpeedVal != null) {
                            tvButterflyNominalSpeedVal.setText("Nominal Speed: " + ((int) val) + " km/h (Fallback speed for travel ETA estimation)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Speed Saved", "Nominal travel speed set to " + ((int) val) + " km/h", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Enter value between 10 and 140 km/h", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ex) {
                    android.widget.Toast.makeText(this, "Invalid number", android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyApproachWindowDialog() {
        int current = RegistryManager.getButterflyApproachWindowMinutes(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Approach Monitoring Window (15 - 180 min)")
            .setMessage("Set the Time-to-Rendezvous (TTR) window before the scheduled meeting when active approach monitoring begins.\n\nPrior to this window, devices remain in low-cost deep hibernation with zero redundant cloud updates.")
            .setView(input)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    int val = Integer.parseInt(input.getText().toString().trim());
                    if (val >= 15 && val <= 180) {
                        RegistryManager.setButterflyApproachWindowMinutes(this, val);
                        if (settingsModel != null) settingsModel.setButterflyApproachWindowMinutes(val);
                        if (tvButterflyApproachWindowVal != null) {
                            tvButterflyApproachWindowVal.setText("TTR Horizon: " + val + " Minutes (Time window to trigger active approach tracking)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Approach Window Saved", "Active approach horizon set to " + val + " minutes", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Enter value between 15 and 180 minutes", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ex) {
                    android.widget.Toast.makeText(this, "Invalid number", android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void showButterflyTacticalRadiusDialog() {
        float current = RegistryManager.getButterflyTacticalRadiusKm(this);
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf((int) current));
        input.setSelection(input.getText().length());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Tactical Quorum Radius (1 - 25 km)")
            .setMessage("Set the proximity radius around the Butterfly Point within which tactical high-velocity vector tracking and live spiderweb lines activate.\n\nOptimizes cellular bandwidth and battery life outside this boundary.")
            .setView(input)
            .setPositiveButton("SAVE", (d, w) -> {
                try {
                    float val = Float.parseFloat(input.getText().toString().trim());
                    if (val >= 1.0f && val <= 25.0f) {
                        RegistryManager.setButterflyTacticalRadiusKm(this, val);
                        if (settingsModel != null) settingsModel.setButterflyTacticalRadiusKm(val);
                        if (tvButterflyTacticalRadiusVal != null) {
                            tvButterflyTacticalRadiusVal.setText("Proximity Radius: " + ((int) val) + " km (Threshold for high-velocity tactical vectors)");
                        }
                        UiArchitect.showSlideNotificationBanner(this, "Tactical Radius Saved", "Proximity radius set to " + ((int) val) + " km", false, 2500, null, null);
                    } else {
                        android.widget.Toast.makeText(this, "Enter value between 1 and 25 km", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ex) {
                    android.widget.Toast.makeText(this, "Invalid number", android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 🔐 UNIFIED DEVICE PERMISSIONS & ACCESS CONTROL HUB (OOPS MVC SSOT)
    // ──────────────────────────────────────────────────────────────────────────

    private void buildPermissionsHubCard(LinearLayout content, LinearLayout.LayoutParams btnLp) {
        permissionToggleBindings.clear();
        permissionsHubHeaderView = createSectionHeader(com.jdroidx.jfindme.R.string.title_settings_device_permissions_hub);
        content.addView(permissionsHubHeaderView);

        LinearLayout permCard = new LinearLayout(this);
        permCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout summaryBox = new LinearLayout(this);
        summaryBox.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, summaryBox, "CARD_STANDARD");
        int sPad = scale(20);
        summaryBox.setPadding(sPad, sPad, sPad, sPad);

        TextView summaryTitle = new TextView(this);
        summaryTitle.setText("UNIFIED PERMISSION & ACCESS CONTROL STATUS");
        summaryTitle.setTextColor(getTextColor());
        summaryTitle.setTypeface(null, Typeface.BOLD);
        summaryTitle.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_LABEL"));
        summaryBox.addView(summaryTitle);

        tvPermissionsSummaryVal = new TextView(this);
        tvPermissionsSummaryVal.setTextColor(getSecondaryTextColor());
        tvPermissionsSummaryVal.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
        tvPermissionsSummaryVal.setPadding(0, scale(6), 0, scale(14));
        summaryBox.addView(tvPermissionsSummaryVal);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setWeightSum(2.0f);

        View grantAllBtn = createIconButton("GRANT MANDATORY", android.R.drawable.ic_menu_myplaces, v -> {
            com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.requestMandatoryBatch(
                this,
                com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.RC_MANDATORY_BATCH
            );
            UiArchitect.showSlideNotificationBanner(this, "Permission Escalation", "Requested core mandatory OS permissions.", false, 2500, null, null);
        });
        View sysAppBtn = createIconButton("OS APP SETTINGS", android.R.drawable.ic_menu_preferences, v ->
            com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.openAppDetailsSettings(this)
        );
        LinearLayout.LayoutParams actLp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        actLp1.setMargins(0, 0, scale(8), 0);
        LinearLayout.LayoutParams actLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        actLp2.setMargins(scale(8), 0, 0, 0);
        actionRow.addView(grantAllBtn, actLp1);
        actionRow.addView(sysAppBtn, actLp2);
        summaryBox.addView(actionRow);

        permCard.addView(summaryBox, btnLp);

        appendPermissionSectionGroup(permCard, "SECTION A: CORE MANDATORY PERMISSIONS",
            com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionTier.MANDATORY, btnLp);
        appendPermissionSectionGroup(permCard, "SECTION B: 24/7 BACKGROUND & OEM SURVIVABILITY",
            com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionTier.BACKGROUND_OEM, btnLp);
        appendPermissionSectionGroup(permCard, "SECTION C: OPTIONAL HARDWARE & FEATURE PERMISSIONS",
            com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionTier.OPTIONAL, btnLp);
        appendPermissionSectionGroup(permCard, "SECTION D: IN-APP PROFILE ACL PERMISSIONS (SSOT)",
            com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionTier.IN_APP_ACL, btnLp);

        content.addView(permCard, getCardParams());
        refreshPermissionToggleStates();
    }

    private void appendPermissionSectionGroup(
            LinearLayout parent,
            String sectionTitle,
            com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionTier tier,
            LinearLayout.LayoutParams btnLp) {
        TextView subHeader = new TextView(this);
        subHeader.setText(sectionTitle);
        subHeader.setTextColor(getGradientStart());
        subHeader.setTypeface(null, Typeface.BOLD);
        subHeader.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
        subHeader.setPadding(scale(10), scale(22), 0, scale(8));
        parent.addView(subHeader);

        for (com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionGateSpec spec :
                com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.getSpecsByTier(tier)) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            UiArchitect.applyCard(this, row, "CARD_STANDARD");
            int pad = scale(18);
            row.setPadding(pad, pad, pad, pad);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            textCol.setPadding(0, 0, scale(12), 0);

            LinearLayout titleBadgeRow = new LinearLayout(this);
            titleBadgeRow.setOrientation(LinearLayout.HORIZONTAL);
            titleBadgeRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(spec.getTitle());
            tvTitle.setTextColor(getTextColor());
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_LABEL"));
            tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            titleBadgeRow.addView(tvTitle);

            TextView tvBadge = new TextView(this);
            tvBadge.setText(spec.getBadge());
            tvBadge.setTextColor(Color.WHITE);
            tvBadge.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_TINY"));
            tvBadge.setTypeface(null, Typeface.BOLD);
            int badgeColor = spec.isMandatory()
                ? Color.parseColor("#C62828")
                : (tier == com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionTier.IN_APP_ACL
                    ? Color.parseColor("#6A1B9A")
                    : Color.parseColor("#1565C0"));
            tvBadge.setBackground(getSolidDrawable(badgeColor, scaleRadius(8)));
            tvBadge.setPadding(scale(10), scale(3), scale(10), scale(3));
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeLp.leftMargin = scale(8);
            titleBadgeRow.addView(tvBadge, badgeLp);
            textCol.addView(titleBadgeRow);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(spec.getDescription());
            tvDesc.setTextColor(getSecondaryTextColor());
            tvDesc.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
            tvDesc.setPadding(0, scale(4), 0, scale(4));
            textCol.addView(tvDesc);

            TextView tvStatus = new TextView(this);
            tvStatus.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
            tvStatus.setTypeface(null, Typeface.BOLD);
            textCol.addView(tvStatus);

            row.addView(textCol);

            androidx.appcompat.widget.SwitchCompat toggle = new androidx.appcompat.widget.SwitchCompat(this);
            PermissionToggleBinding binding = new PermissionToggleBinding(spec, toggle, tvStatus);
            permissionToggleBindings.add(binding);

            toggle.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isRefreshingPermissionToggles) return;
                handlePermissionToggleAction(binding, isChecked);
            });
            row.setOnClickListener(v -> toggle.toggle());
            row.addView(toggle);
            parent.addView(row, btnLp);
        }
    }

    private void handlePermissionToggleAction(PermissionToggleBinding binding, boolean targetChecked) {
        com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionGateSpec spec = binding.spec;
        if (spec.getTier() == com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionTier.IN_APP_ACL) {
            com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.setAclGateState(this, spec.getKey(), targetChecked);
            refreshPermissionToggleStates();
            UiArchitect.showSlideNotificationBanner(this, spec.getTitle(), targetChecked ? "ACL policy ENABLED & synced." : "ACL policy RESTRICTED.", !targetChecked, 2500, null, null);
            return;
        }

        boolean osGranted = com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.isOsGateGranted(this, spec.getKey());
        if (targetChecked) {
            if (!osGranted) {
                com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.requestGate(
                    this,
                    spec.getKey(),
                    com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.RC_SINGLE_GATE
                );
                refreshPermissionToggleStates();
            } else {
                com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.setSoftGateDisabled(this, spec.getKey(), false);
                refreshPermissionToggleStates();
                UiArchitect.showSlideNotificationBanner(this, spec.getTitle(), "Permission gate ACTIVE.", false, 2000, null, null);
            }
        } else {
            if (spec.isMandatory()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("⚠️ Disable Mandatory Gate?")
                    .setMessage(spec.getTitle() + " is mandatory for live tracking & safety.\n\nChoose 'Pause App Gate' to halt usage in-app, or 'OS Settings' to revoke system permission.")
                    .setPositiveButton("PAUSE APP GATE", (d, w) -> {
                        com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.setSoftGateDisabled(this, spec.getKey(), true);
                        refreshPermissionToggleStates();
                    })
                    .setNeutralButton("OS SETTINGS", (d, w) -> {
                        refreshPermissionToggleStates();
                        com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.openAppDetailsSettings(this);
                    })
                    .setNegativeButton("CANCEL", (d, w) -> refreshPermissionToggleStates())
                    .setOnCancelListener(d -> refreshPermissionToggleStates())
                    .show();
            } else {
                com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.setSoftGateDisabled(this, spec.getKey(), true);
                refreshPermissionToggleStates();
                UiArchitect.showSlideNotificationBanner(this, spec.getTitle(), "Optional feature paused in app. Manage OS grant in App Settings.", false, 2500, null, null);
            }
        }
    }

    private void refreshPermissionToggleStates() {
        if (permissionToggleBindings.isEmpty()) return;
        isRefreshingPermissionToggles = true;
        int mandatoryTotal = 0;
        int mandatoryActive = 0;
        int optionalTotal = 0;
        int optionalActive = 0;
        int aclTotal = 0;
        int aclActive = 0;

        try {
            for (PermissionToggleBinding binding : permissionToggleBindings) {
                com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionGateSpec spec = binding.spec;
                boolean active = com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.isEffectiveGateActive(this, spec.getKey());
                boolean osGranted = active || com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.isOsGateGranted(this, spec.getKey());
                binding.toggleSwitch.setChecked(active);

                if (spec.getTier() == com.jdroidx.jfindme.engine.permission.ProfilePermissionsManager.PermissionTier.IN_APP_ACL) {
                    aclTotal++;
                    if (active) aclActive++;
                    binding.tvStatus.setText(active ? "🟢 ALLOWED • Enforced in Profile & Telemetry" : "🟡 RESTRICTED • Disabled by ACL");
                    binding.tvStatus.setTextColor(active ? Color.parseColor("#2E7D32") : Color.parseColor("#D97706"));
                } else {
                    if (spec.isMandatory()) {
                        mandatoryTotal++;
                        if (active) mandatoryActive++;
                    } else {
                        optionalTotal++;
                        if (active) optionalActive++;
                    }

                    if (active) {
                        binding.tvStatus.setText("🟢 GRANTED & ACTIVE");
                        binding.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                    } else if (osGranted) {
                        binding.tvStatus.setText("🟡 PAUSED BY APP TOGGLE (OS Granted)");
                        binding.tvStatus.setTextColor(Color.parseColor("#D97706"));
                    } else {
                        binding.tvStatus.setText(spec.isMandatory()
                            ? "🔴 MANDATORY OS PERMISSION DENIED — Toggle ON to grant"
                            : "🟡 OPTIONAL NOT GRANTED — Toggle ON if needed");
                        binding.tvStatus.setTextColor(spec.isMandatory() ? Color.parseColor("#D32F2F") : Color.parseColor("#D97706"));
                    }
                }
            }
            if (tvPermissionsSummaryVal != null) {
                tvPermissionsSummaryVal.setText(
                    "Mandatory: " + mandatoryActive + "/" + mandatoryTotal +
                    " Active  •  Optional: " + optionalActive + "/" + optionalTotal +
                    " Active  •  ACLs: " + aclActive + "/" + aclTotal + " Enabled"
                );
            }
            if (badgePermissionsTv != null) {
                badgePermissionsTv.setText(mandatoryActive + "/" + mandatoryTotal + " MANDATORY");
            }
        } finally {
            isRefreshingPermissionToggles = false;
        }
    }

}
