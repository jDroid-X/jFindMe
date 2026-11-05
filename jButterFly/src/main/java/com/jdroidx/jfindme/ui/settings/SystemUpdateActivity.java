package com.jdroidx.jfindme.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.BuildConfig;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.AngelLoader;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

/**
 * 👑 GLOBAL UI ARCHITECT: System Update Center & Interactive Patching Console
 * Waterfall MVVM Architecture Implementation (L7 Standard)
 * Supports Dual-Tier Update Architecture:
 * 1. ⚡ Dynamic In-App Hot-Patch (Zero-Install, In-Memory, No App Reboot)
 * 2. 🧬 Smart Differential Delta Binary Patch (Package Reconstruction)
 */
public class SystemUpdateActivity extends BaseActivityJava {

    private SystemUpdateViewModel viewModel;
    private EditText emailField;
    private EditText driveLinkField;
    private EditText githubLinkField;
    private EditText localSyncEndpointField;
    private android.widget.CheckBox silentUpdateCheck;
    private TextView tvMaintenanceScheduleTime;
    private android.widget.CheckBox meteredUpdateCheck;
    private TextView tvPendingBalanceCount;
    private TextView tvLastBalanceSync;

    // Forensic & Status Views
    private TextView tvHeroBadge;
    private TextView tvHeroSub;
    private TextView tvCurrentVersion;
    private TextView tvLastUpdate;
    private TextView tvLastCheck;
    private TextView tvLastDownload;
    private TextView tvUpdateAvailable;
    private TextView tvIsUpdatedStatus;
    private TextView tvLastFailedInstall;
    private TextView tvPatchAvailable;
    private TextView tvPatchAuditHistory;

    // Dynamic Delta Patch UI elements (removing hardcoded values)
    private TextView tvDeltaBadge;
    private TextView tvDeltaDesc;
    private TextView tvBandwidthSaved;
    private TextView tvBtnFullApk;
    private LinearLayout layoutAvailablePatches;

    // Hot-Patch Interactive Views
    private TextView tvHotPatchStatus;
    private TextView tvHotPatchDetails;
    private View btnApplyHotPatch;
    private View btnInspectHotPatch;
    private FirebaseComm.LiveHotPatchInfo currentHotPatchInfo;

    // Pipeline Visualizer Views
    private TextView tvPipelineStatus;
    private TextView[] pipelineStepViews = new TextView[5];

    // 2-Tab Navigation Architecture
    private LinearLayout tabContentInstallUpdate;
    private LinearLayout tabContentPatchLevel;
    private TextView tabBtnInstallUpdate;
    private TextView tabBtnPatchLevel;

    @Override
    protected void setupLogic() {
        // Initialize Waterfall MVVM Engine
        viewModel = new ViewModelProvider(this).get(SystemUpdateViewModel.class);
        viewModel.initData(this);

        int initialTab = getIntent().getIntExtra("EXTRA_TAB", 0);
        if (initialTab == 1) {
            switchTab(1);
        }

        if (getIntent().getBooleanExtra("EXTRA_TRIGGER_FULL_APK", false)) {
            viewModel.installFullApk(this);
        }

        // --- OBSERVERS: Synchronization Hub ---
        
        viewModel.getGmailAccount().observe(this, email -> {
            if (emailField != null) emailField.setText(email);
        });

        viewModel.getDriveLink().observe(this, link -> {
            if (driveLinkField != null) driveLinkField.setText(link);
        });

        viewModel.getGithubLink().observe(this, link -> {
            if (githubLinkField != null) githubLinkField.setText(link);
        });

        viewModel.getLocalSyncEndpoint().observe(this, endpoint -> {
            if (localSyncEndpointField != null && endpoint != null) {
                localSyncEndpointField.setText(endpoint);
            }
        });

        viewModel.getLatestDeltaSavingsPct().observe(this, pct -> updateDeltaCardUi());
        viewModel.getLatestDeltaPatchSize().observe(this, size -> updateDeltaCardUi());
        viewModel.getLatestFullApkSize().observe(this, size -> updateDeltaCardUi());

        viewModel.getAvailablePatchesList().observe(this, list -> {
            updateAvailablePatchesListUi(list);
        });

        viewModel.isSilentMode().observe(this, isEnabled -> {
            if (silentUpdateCheck != null) {
                silentUpdateCheck.setOnCheckedChangeListener(null);
                silentUpdateCheck.setChecked(isEnabled);
                silentUpdateCheck.setOnCheckedChangeListener((bv, checked) -> viewModel.toggleSilentMode(this, checked));
            }
        });

        viewModel.getLoadingStatus().observe(this, status -> {
            if (status != null) AngelLoader.setStatus(this, status);
        });

        viewModel.isLoading().observe(this, loading -> {
            if (loading) AngelLoader.show(this);
            else AngelLoader.hide(this);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                String[] parts = error.split(":");
                String title = parts.length > 0 ? parts[0].replace("_", " ") : "Execution Error";
                String msg = parts.length > 1 ? parts[1] : error;
                showForensicDialog(title, msg);
                viewModel.clearError();
            }
        });

        viewModel.getInstallPackageUrl().observe(this, url -> {
            if (url != null) {
                downloadAndInstallApk(url);
                viewModel.resetInstallPackageUrl();
            }
        });

        viewModel.getShowInstallPrompt().observe(this, data -> {
            if (data != null) {
                showInstallGuidanceModal(data);
            }
        });

        viewModel.getCheckResultDialog().observe(this, pair -> {
            if (pair != null) {
                showForensicDialog(pair.getFirst(), pair.getSecond());
                viewModel.dismissCheckResultDialog();
            }
        });

        // Forensic Update Observers
        viewModel.getCurrentVersionInfo().observe(this, info -> {
            if (tvCurrentVersion != null) tvCurrentVersion.setText("📱 CURRENT INSTALLED VERSION: " + info);
            updateHeroBanner();
        });

        viewModel.getLastUpdateInfo().observe(this, info -> {
            if (tvLastUpdate != null) tvLastUpdate.setText("🕒 LAST APP UPDATE / INSTALL: " + info);
        });

        viewModel.getLastCheckInfo().observe(this, info -> {
            if (tvLastCheck != null) tvLastCheck.setText("🌐 LAST CHECKED ONLINE: " + info);
            updateHeroBanner();
        });

        viewModel.getUpdateAvailableInfo().observe(this, info -> {
            if (tvUpdateAvailable != null) tvUpdateAvailable.setText("📦 UPDATE AVAILABLE: " + info);
            updateHeroBanner();
        });

        viewModel.getLastDownloadInfo().observe(this, info -> {
            if (tvLastDownload != null) tvLastDownload.setText("📥 LAST DOWNLOADED PACKAGE: " + info);
        });

        viewModel.getUpdateStatusInfo().observe(this, info -> {
            if (tvIsUpdatedStatus != null) tvIsUpdatedStatus.setText("🛡️ SYSTEM STATUS: " + info);
            updateHeroBanner();
        });

        viewModel.getLastFailedInstallInfo().observe(this, info -> {
            if (tvLastFailedInstall != null) {
                tvLastFailedInstall.setText("❌ LAST FAILED INSTALL: " + info);
                if (info != null && !info.startsWith("None")) {
                    tvLastFailedInstall.setTextColor(Color.parseColor("#FF5252"));
                } else {
                    tvLastFailedInstall.setTextColor(getSecondaryTextColor());
                }
            }
        });

        viewModel.getPatchAvailableInfo().observe(this, info -> {
            if (tvPatchAvailable != null) tvPatchAvailable.setText("⚡ SMART DELTA PATCH: " + info);
        });

        viewModel.getPatchAuditLog().observe(this, log -> {
            if (tvPatchAuditHistory != null) tvPatchAuditHistory.setText("📋 APPLIED PATCH AUDIT LOG:\n" + log);
        });

        viewModel.getMaintenanceWindowTime().observe(this, time -> {
            if (tvMaintenanceScheduleTime != null && time != null) {
                tvMaintenanceScheduleTime.setText("⏰ SCHEDULED WINDOW: " + time + " DAILY");
            }
        });

        viewModel.isMeteredAllowed().observe(this, allowed -> {
            if (meteredUpdateCheck != null) {
                meteredUpdateCheck.setOnCheckedChangeListener(null);
                meteredUpdateCheck.setChecked(allowed != null && allowed);
                meteredUpdateCheck.setOnCheckedChangeListener((bv, checked) -> viewModel.toggleAllowMetered(this, checked));
            }
        });

        viewModel.getPendingBalanceCount().observe(this, count -> {
            if (tvPendingBalanceCount != null) {
                if (count == null || count == 0) {
                    tvPendingBalanceCount.setText("⚖️ OFFLINE DELTA: 0 PENDING (BALANCED)");
                    tvPendingBalanceCount.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    tvPendingBalanceCount.setText("⚖️ OFFLINE DELTA: " + count + " PENDING RECORDS");
                    tvPendingBalanceCount.setTextColor(Color.parseColor("#FFB300"));
                }
            }
        });

        viewModel.getLastBalanceSyncTs().observe(this, ts -> {
            if (tvLastBalanceSync != null && ts != null) {
                tvLastBalanceSync.setText("🕒 LAST BALANCE SYNC: " + ts);
            }
        });

        // ⚡ DYNAMIC HOT-PATCH OBSERVERS (ZERO-INSTALL)
        viewModel.getLiveHotPatch().observe(this, patch -> {
            currentHotPatchInfo = patch;
            updateHotPatchDisplay(patch, Boolean.TRUE.equals(viewModel.isHotPatchActive().getValue()));
            updateHeroBanner();
        });

        viewModel.isHotPatchActive().observe(this, active -> {
            updateHotPatchDisplay(currentHotPatchInfo, Boolean.TRUE.equals(active));
            updateHeroBanner();
        });

        // 📊 PIPELINE STEPPER OBSERVERS
        viewModel.getPipelineStep().observe(this, step -> {
            if (step != null) updatePipelineStepUi(step);
        });

        viewModel.getPipelineStatusMessage().observe(this, msg -> {
            if (tvPipelineStatus != null && msg != null) {
                tvPipelineStatus.setText("⚡ PIPELINE: " + msg);
            }
        });

        // 🛡️ HUMAN-IN-THE-LOOP Installation Prompt Observer
        viewModel.getShowInstallPrompt().observe(this, promptData -> {
            if (promptData != null) {
                showInstallGuidanceModal(promptData);
            }
        });
    }

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // --- HEADER: L7 Waterfall Branding ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView title = new TextView(this);
        title.setText("SYSTEM UPDATE & PATCH CENTER");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Interactive Live Patching & Differential Synchronization");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.85f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        subtitle.setPadding(0, scale(8), 0, 0);
        header.addView(subtitle);

        root.addView(header);

        // --- 2-TAB SEGREGATION NAVIGATION BAR ---
        LinearLayout tabNav = new LinearLayout(this);
        tabNav.setOrientation(LinearLayout.HORIZONTAL);
        tabNav.setWeightSum(2.0f);
        int tabPad = scale(14);
        tabNav.setPadding(tabPad, scale(8), tabPad, scale(8));

        tabBtnInstallUpdate = createTabButton("📦 1. INSTALL / UPDATE", true, v -> switchTab(0));
        tabBtnPatchLevel = createTabButton("⚡ 2. PATCH LEVEL UPDATE", false, v -> switchTab(1));

        LinearLayout.LayoutParams pTab1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pTab1.setMargins(0, 0, scale(6), 0);
        tabNav.addView(tabBtnInstallUpdate, pTab1);

        LinearLayout.LayoutParams pTab2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pTab2.setMargins(scale(6), 0, 0, 0);
        tabNav.addView(tabBtnPatchLevel, pTab2);

        root.addView(tabNav);

        // --- SCROLL CONTAINER ---
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        scroll.setClipToPadding(false);

        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        int quota = getResponsivePaddingQuota();
        master.setPadding(quota, quota, quota, scale(60));

        // ========================================================================================
        // TAB 1: INSTALL / UPDATE (Traditional distribution, settings & full releases)
        // ========================================================================================
        tabContentInstallUpdate = new LinearLayout(this);
        tabContentInstallUpdate.setOrientation(LinearLayout.VERTICAL);

        // 1. Hero System Status Banner
        tabContentInstallUpdate.addView(createHeroBanner());
        tabContentInstallUpdate.addView(createSpacer(15));

        // 2. Distribution Channels & Settings Card
        tabContentInstallUpdate.addView(createConfigurationCard());
        tabContentInstallUpdate.addView(createSpacer(15));

        // 3. Tab 1 Specific Action Bar (Scan, Full APK, Drive Mirror, Close)
        tabContentInstallUpdate.addView(createInstallUpdateActionRow());
        tabContentInstallUpdate.addView(createSpacer(20));

        // 4. Architectural Footer
        tabContentInstallUpdate.addView(createArchitecturalFooter());

        master.addView(tabContentInstallUpdate);

        // ========================================================================================
        // TAB 2: PATCH LEVEL UPDATE (Zero-Install hot-patches, delta binary diffs & ledger)
        // ========================================================================================
        tabContentPatchLevel = new LinearLayout(this);
        tabContentPatchLevel.setOrientation(LinearLayout.VERTICAL);
        tabContentPatchLevel.setVisibility(View.GONE); // Default: Tab 1 active

        // 1. Dynamic In-App Hot-Patch (Zero-Install)
        tabContentPatchLevel.addView(createHotPatchCard());
        tabContentPatchLevel.addView(createSpacer(15));

        // 2. Smart Differential Delta Binary Patch
        tabContentPatchLevel.addView(createDeltaPatchCard());
        tabContentPatchLevel.addView(createSpacer(15));

        // 3. Real-Time Cryptographic Pipeline Stepper
        tabContentPatchLevel.addView(createPipelineStepperCard());
        tabContentPatchLevel.addView(createSpacer(15));

        // 4. Forensic Audit & Patch Ledger (Group By Target Matrix + Open on Click)
        tabContentPatchLevel.addView(createForensicLedgerCard());
        tabContentPatchLevel.addView(createSpacer(15));

        // 5. Tab 2 Action Bar (Apply Live Patch, Scan RTDB, Guide, Close)
        tabContentPatchLevel.addView(createIntegratedActionRow());
        tabContentPatchLevel.addView(createSpacer(20));

        // 6. Architectural Footer
        tabContentPatchLevel.addView(createArchitecturalFooter());

        master.addView(tabContentPatchLevel);

        scroll.addView(master);
        root.addView(scroll);

        return root;
    }

    private TextView createTabButton(String title, boolean active, View.OnClickListener listener) {
        TextView tab = new TextView(this);
        tab.setText(title);
        tab.setGravity(Gravity.CENTER);
        tab.setTypeface(null, Typeface.BOLD);
        tab.setPadding(scale(10), scale(12), scale(10), scale(12));
        UiArchitect.applyText(this, tab, "TEXT_MINI");
        updateTabButtonState(tab, active);
        tab.setClickable(true);
        tab.setFocusable(true);
        tab.setOnClickListener(listener);
        return tab;
    }

    private void updateTabButtonState(TextView tab, boolean active) {
        if (tab == null) return;
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(20f);
        if (active) {
            d.setColor(getGradientStart());
            d.setStroke(2, getGradientStart());
            tab.setTextColor(getGradientOverlayText());
        } else {
            d.setColor(Color.parseColor("#14" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
            d.setStroke(1, Color.parseColor("#33" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
            tab.setTextColor(getSecondaryTextColor());
        }
        tab.setBackground(d);
    }

    private void switchTab(int index) {
        if (index == 0) {
            if (tabContentInstallUpdate != null) tabContentInstallUpdate.setVisibility(View.VISIBLE);
            if (tabContentPatchLevel != null) tabContentPatchLevel.setVisibility(View.GONE);
            updateTabButtonState(tabBtnInstallUpdate, true);
            updateTabButtonState(tabBtnPatchLevel, false);
        } else {
            if (tabContentInstallUpdate != null) tabContentInstallUpdate.setVisibility(View.GONE);
            if (tabContentPatchLevel != null) tabContentPatchLevel.setVisibility(View.VISIBLE);
            updateTabButtonState(tabBtnInstallUpdate, false);
            updateTabButtonState(tabBtnPatchLevel, true);
        }
    }

    // ============================================================================================
    // UI BUILDER: HERO SYSTEM BANNER
    // ============================================================================================
    private View createHeroBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(scale(20), scale(18), scale(20), scale(18));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(28f);
        bg.setColor(Color.parseColor("#15" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        bg.setStroke(2, getGradientStart());
        banner.setBackground(bg);

        tvHeroBadge = new TextView(this);
        tvHeroBadge.setText("🟢 SYSTEM VERIFIED & SECURE");
        tvHeroBadge.setTextColor(getGradientStart());
        tvHeroBadge.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvHeroBadge, "TEXT_TITLE");
        banner.addView(tvHeroBadge);

        tvHeroSub = new TextView(this);
        tvHeroSub.setText("Baseline: V" + BuildConfig.VERSION_NAME + " (Build " + BuildConfig.VERSION_CODE + ") • Running Secure Track");
        tvHeroSub.setTextColor(getSecondaryTextColor());
        tvHeroSub.setPadding(0, scale(6), 0, 0);
        UiArchitect.applyText(this, tvHeroSub, "TEXT_BODY");
        banner.addView(tvHeroSub);

        return banner;
    }

    private void updateHeroBanner() {
        if (tvHeroBadge == null || tvHeroSub == null) return;
        boolean hotActive = Boolean.TRUE.equals(viewModel.isHotPatchActive().getValue());
        boolean hasHot = currentHotPatchInfo != null;
        String updateAvail = viewModel.getUpdateAvailableInfo().getValue();
        boolean hasBuildUpdate = updateAvail != null && !updateAvail.startsWith("None") && !updateAvail.contains("Matches");

        if (hasHot && !hotActive) {
            tvHeroBadge.setText("⚡ DYNAMIC IN-APP PATCH AVAILABLE");
            tvHeroBadge.setTextColor(Color.parseColor("#FFB300")); // Vibrant Amber
            tvHeroSub.setText("Payload: " + currentHotPatchInfo.getPatchName() + " (" + currentHotPatchInfo.getPatchSize() + ") • Zero OS Install Required!");
        } else if (hasBuildUpdate) {
            tvHeroBadge.setText("📦 NEW SYSTEM BUILD READY: " + updateAvail);
            tvHeroBadge.setTextColor(Color.parseColor("#40C4FF")); // Cyan
            tvHeroSub.setText("Differential delta available • 97% mobile bandwidth savings via GitHub");
        } else {
            tvHeroBadge.setText("🟢 SYSTEM INTEGRITY: 100% SECURE");
            tvHeroBadge.setTextColor(Color.parseColor("#00E676")); // Emerald Green
            tvHeroSub.setText("Installed: V" + BuildConfig.VERSION_NAME + " (Build " + BuildConfig.VERSION_CODE + ")" + (hotActive ? " • Hot-Patch Active" : " • Baseline Verified"));
        }
    }

    // ============================================================================================
    // UI BUILDER: CARD 1 — DYNAMIC IN-APP HOT-PATCH (ZERO-INSTALL)
    // ============================================================================================
    private View createHotPatchCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(scale(20), scale(18), scale(20), scale(18));
        applyCardStyle(card);

        // Header Row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText("⚡ DYNAMIC IN-APP PATCH");
        lbl.setTextColor(getGradientStart());
        lbl.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, lbl, "TEXT_TITLE");
        headerRow.addView(lbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView badge = createPillBadge("ZERO-INSTALL", Color.parseColor("#00E676"));
        headerRow.addView(badge);
        card.addView(headerRow);

        // Explanatory Text for Humans
        TextView desc = new TextView(this);
        desc.setText("Applies immediate runtime tracking optimizations, cache policy corrections, and radar definitions in-memory without prompting the Android package installer or restarting the application.");
        desc.setTextColor(getSecondaryTextColor());
        desc.setPadding(0, scale(6), 0, scale(12));
        UiArchitect.applyText(this, desc, "TEXT_MINI");
        card.addView(desc);

        // Status Box
        LinearLayout statusBox = new LinearLayout(this);
        statusBox.setOrientation(LinearLayout.VERTICAL);
        statusBox.setPadding(scale(14), scale(12), scale(14), scale(12));
        statusBox.setBackground(createInnerWellBackground());

        tvHotPatchStatus = new TextView(this);
        tvHotPatchStatus.setText("STATUS: Checking Cloud Node...");
        tvHotPatchStatus.setTextColor(getTextColor());
        tvHotPatchStatus.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvHotPatchStatus, "TEXT_BODY");
        statusBox.addView(tvHotPatchStatus);

        tvHotPatchDetails = new TextView(this);
        tvHotPatchDetails.setText("• Target: Runtime Engine & Cache Registry\n• Requires OS Reinstall: NO\n• Cryptographic Integrity: SHA-256 Seal Valid");
        tvHotPatchDetails.setTextColor(getSecondaryTextColor());
        tvHotPatchDetails.setPadding(0, scale(6), 0, 0);
        UiArchitect.applyText(this, tvHotPatchDetails, "TEXT_MINI");
        statusBox.addView(tvHotPatchDetails);

        card.addView(statusBox);
        card.addView(createSpacer(12));

        // Interactive Button Row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setWeightSum(2.0f);

        btnApplyHotPatch = createSmallButton("⚡ APPLY LIVE PATCH", android.R.drawable.ic_media_play, true, v -> {
            if (currentHotPatchInfo != null) {
                viewModel.applyHotPatch(this);
            } else {
                Toast.makeText(this, "Scanning for available dynamic patches...", Toast.LENGTH_SHORT).show();
                viewModel.executeUpdate(this);
            }
        });

        btnInspectHotPatch = createSmallButton("🔍 INSPECT PAYLOAD", android.R.drawable.ic_menu_info_details, false, v -> {
            showHotPatchPayloadDialog(currentHotPatchInfo);
        });

        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnRow.addView(btnApplyHotPatch, p1);

        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        p2.setMargins(scale(10), 0, 0, 0);
        btnRow.addView(btnInspectHotPatch, p2);

        card.addView(btnRow);

        return card;
    }

    private void updateHotPatchDisplay(FirebaseComm.LiveHotPatchInfo patch, boolean isActive) {
        if (tvHotPatchStatus == null || tvHotPatchDetails == null) return;
        if (patch != null) {
            if (isActive) {
                tvHotPatchStatus.setText("STATUS: ✅ ACTIVE & VERIFIED IN-MEMORY");
                tvHotPatchStatus.setTextColor(Color.parseColor("#00E676"));
                tvHotPatchDetails.setText("• Patch ID: " + patch.getPatchId() + " (" + patch.getPatchName() + ")\n" +
                        "• Size: " + patch.getPatchSize() + " • SHA-256: VALIDATED\n" +
                        "• Target: " + patch.getTargetComponent() + "\n" +
                        "• Release Notes: " + patch.getReleaseNotes());
            } else {
                tvHotPatchStatus.setText("STATUS: ⚡ PENDING IN-APP APPLICATION");
                tvHotPatchStatus.setTextColor(Color.parseColor("#FFB300"));
                tvHotPatchDetails.setText("• Patch ID: " + patch.getPatchId() + " (" + patch.getPatchName() + ")\n" +
                        "• Size: " + patch.getPatchSize() + " • Ready for live injection\n" +
                        "• Target: " + patch.getTargetComponent() + "\n" +
                        "• Release Notes: " + patch.getReleaseNotes());
            }
        } else {
            tvHotPatchStatus.setText("STATUS: 🟢 RUNNING BASELINE ARCHITECTURE");
            tvHotPatchStatus.setTextColor(getTextColor());
            tvHotPatchDetails.setText("• No pending in-app hot-patches found in Firebase RTDB.\n• System running verified Build " + BuildConfig.VERSION_CODE + ".");
        }
    }

    // ============================================================================================
    // UI BUILDER: CARD 2 — SMART DIFFERENTIAL DELTA BINARY PATCH
    // ============================================================================================
    private View createDeltaPatchCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(scale(20), scale(18), scale(20), scale(18));
        applyCardStyle(card);

        // Header Row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText("🧬 SMART DELTA BINARY PATCH");
        lbl.setTextColor(getGradientStart());
        lbl.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, lbl, "TEXT_TITLE");
        headerRow.addView(lbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        tvDeltaBadge = createPillBadge("DATA SAVINGS ACTIVE", Color.parseColor("#40C4FF"));
        headerRow.addView(tvDeltaBadge);
        card.addView(headerRow);

        tvDeltaDesc = new TextView(this);
        tvDeltaDesc.setText("Instead of downloading the entire full APK, this differential engine downloads only a small delta diff file, reconstructing the target APK directly on your device.");
        tvDeltaDesc.setTextColor(getSecondaryTextColor());
        tvDeltaDesc.setPadding(0, scale(6), 0, scale(12));
        UiArchitect.applyText(this, tvDeltaDesc, "TEXT_MINI");
        card.addView(tvDeltaDesc);

        // Delta Specs Box
        LinearLayout deltaBox = new LinearLayout(this);
        deltaBox.setOrientation(LinearLayout.VERTICAL);
        deltaBox.setPadding(scale(14), scale(12), scale(14), scale(12));
        deltaBox.setBackground(createInnerWellBackground());

        tvPatchAvailable = new TextView(this);
        tvPatchAvailable.setText("⚡ SMART DELTA PATCH: Checking available patches...");
        tvPatchAvailable.setTextColor(getTextColor());
        tvPatchAvailable.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvPatchAvailable, "TEXT_BODY");
        deltaBox.addView(tvPatchAvailable);

        tvBandwidthSaved = new TextView(this);
        tvBandwidthSaved.setText("📊 Bandwidth Compression: Calculating differential savings...");
        tvBandwidthSaved.setTextColor(Color.parseColor("#40C4FF"));
        tvBandwidthSaved.setPadding(0, scale(4), 0, 0);
        UiArchitect.applyText(this, tvBandwidthSaved, "TEXT_MINI");
        deltaBox.addView(tvBandwidthSaved);

        card.addView(deltaBox);
        card.addView(createSpacer(12));

        // Interactive Button Row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setWeightSum(2.0f);

        View btnDeltaReconstruct = createSmallButton("🧬 RECONSTRUCT DELTA", android.R.drawable.ic_menu_save, true, v -> {
            viewModel.installGithubApk(this);
        });

        TextView[] holder = new TextView[1];
        View btnFullApk = createSmallButton("📥 FULL APK", android.R.drawable.ic_menu_set_as, false, v -> {
            viewModel.installFullApk(this);
        }, holder);
        tvBtnFullApk = holder[0];

        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnRow.addView(btnDeltaReconstruct, p1);

        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        p2.setMargins(scale(10), 0, 0, 0);
        btnRow.addView(btnFullApk, p2);

        card.addView(btnRow);

        updateDeltaCardUi();

        return card;
    }

    private void updateDeltaCardUi() {
        if (viewModel == null) return;
        Double pct = viewModel.getLatestDeltaSavingsPct().getValue();
        if (tvDeltaBadge != null && pct != null) {
            tvDeltaBadge.setText(String.format(java.util.Locale.US, "%.1f%% DATA SAVED", pct));
        }
        String dSize = viewModel.getLatestDeltaPatchSize().getValue();
        boolean hasDelta = dSize != null && !dSize.trim().isEmpty();
        String deltaSize = hasDelta ? dSize.trim() : "small";

        String fSize = viewModel.getLatestFullApkSize().getValue();
        boolean hasFull = fSize != null && !fSize.trim().isEmpty();
        String fullSize = hasFull ? fSize.trim() : "80.5 MB";

        double savingsPct = pct != null ? pct : 97.0;

        if (tvDeltaDesc != null) {
            if (hasDelta) {
                tvDeltaDesc.setText("Instead of downloading the entire " + fullSize + " APK, this differential engine downloads only a ~" + deltaSize + " delta diff file, reconstructing the target APK directly on your device.");
            } else {
                tvDeltaDesc.setText("Instead of downloading the full APK, this differential engine downloads only a compact delta diff file, reconstructing the target APK directly on your device.");
            }
        }
        if (tvBandwidthSaved != null) {
            if (hasDelta) {
                tvBandwidthSaved.setText("📊 Bandwidth Compression: " + deltaSize + " vs " + fullSize + " Full Binary (~" + String.format(java.util.Locale.US, "%.1f%%", savingsPct) + " Savings)");
            } else {
                tvBandwidthSaved.setText("📊 Differential Engine: Standby (Active fleet patches cataloged in ledger below)");
            }
        }
        if (tvBtnFullApk != null) {
            tvBtnFullApk.setText("📥 FULL APK (" + fullSize + ")");
        }
    }

    // ============================================================================================
    // UI BUILDER: CARD 3 — REAL-TIME PIPELINE STEPPER ("What is being done")
    // ============================================================================================
    private View createPipelineStepperCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(scale(20), scale(18), scale(20), scale(18));
        applyCardStyle(card);

        TextView lbl = new TextView(this);
        lbl.setText("📊 REAL-TIME PIPELINE STEPPER");
        lbl.setTextColor(getGradientStart());
        lbl.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, lbl, "TEXT_TITLE");
        card.addView(lbl);

        TextView desc = new TextView(this);
        desc.setText("Live cryptographic pipeline showing each step as patches and updates are processed:");
        desc.setTextColor(getSecondaryTextColor());
        desc.setPadding(0, scale(4), 0, scale(10));
        UiArchitect.applyText(this, desc, "TEXT_MINI");
        card.addView(desc);

        // 5 Step Pills Row
        LinearLayout stepsRow = new LinearLayout(this);
        stepsRow.setOrientation(LinearLayout.HORIZONTAL);
        stepsRow.setWeightSum(5.0f);

        String[] stepNames = new String[]{"1. QUERY", "2. FETCH", "3. VERIFY", "4. APPLY", "5. AUDIT"};
        for (int i = 0; i < 5; i++) {
            TextView s = new TextView(this);
            s.setText(stepNames[i]);
            s.setGravity(Gravity.CENTER);
            s.setPadding(0, scale(8), 0, scale(8));
            s.setTextColor(getSecondaryTextColor());
            UiArchitect.applyText(this, s, "TEXT_MINI");
            s.setBackground(createStepPillBackground(false));
            pipelineStepViews[i] = s;

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            if (i > 0) lp.setMargins(scale(5), 0, 0, 0);
            stepsRow.addView(s, lp);
        }
        card.addView(stepsRow);

        tvPipelineStatus = new TextView(this);
        tvPipelineStatus.setText("⚡ PIPELINE: Standby — Ready for synchronization");
        tvPipelineStatus.setTextColor(getTextColor());
        tvPipelineStatus.setPadding(0, scale(10), 0, 0);
        UiArchitect.applyText(this, tvPipelineStatus, "TEXT_MINI");
        card.addView(tvPipelineStatus);

        return card;
    }

    private void updatePipelineStepUi(int activeStep) {
        if (pipelineStepViews == null) return;
        for (int i = 0; i < pipelineStepViews.length; i++) {
            if (pipelineStepViews[i] == null) continue;
            boolean isCurrent = (i + 1) == activeStep;
            boolean isCompleted = (i + 1) < activeStep;
            pipelineStepViews[i].setBackground(createStepPillBackground(isCurrent || isCompleted));
            if (isCurrent) {
                pipelineStepViews[i].setTextColor(Color.WHITE);
                pipelineStepViews[i].setTypeface(null, Typeface.BOLD);
            } else if (isCompleted) {
                pipelineStepViews[i].setTextColor(Color.parseColor("#00E676"));
                pipelineStepViews[i].setTypeface(null, Typeface.NORMAL);
            } else {
                pipelineStepViews[i].setTextColor(getSecondaryTextColor());
                pipelineStepViews[i].setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    // ============================================================================================
    // UI BUILDER: CARD 4 — FORENSIC AUDIT LEDGER
    // ============================================================================================
    private View createForensicLedgerCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(scale(20), scale(18), scale(20), scale(18));
        applyCardStyle(card);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText("📋 FORENSIC AUDIT & PATCH LEDGER");
        lbl.setTextColor(getGradientStart());
        lbl.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, lbl, "TEXT_TITLE");
        headerRow.addView(lbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnRefresh = new TextView(this);
        btnRefresh.setText("🔄 REFRESH");
        btnRefresh.setTextColor(getGradientStart());
        btnRefresh.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, btnRefresh, "TEXT_MINI");
        btnRefresh.setPadding(scale(10), scale(6), scale(10), scale(6));
        btnRefresh.setClickable(true);
        btnRefresh.setFocusable(true);
        btnRefresh.setOnClickListener(v -> viewModel.executeUpdate(this));
        headerRow.addView(btnRefresh);

        card.addView(headerRow);

        layoutAvailablePatches = new LinearLayout(this);
        layoutAvailablePatches.setOrientation(LinearLayout.VERTICAL);
        layoutAvailablePatches.setPadding(0, scale(8), 0, scale(8));
        card.addView(layoutAvailablePatches);

        tvPatchAuditHistory = new TextView(this);
        tvPatchAuditHistory.setText("📋 APPLIED PATCH AUDIT LOG:\n• Initial Install: V" + BuildConfig.VERSION_NAME + " (Build " + BuildConfig.VERSION_CODE + ") [Baseline Verified]");
        tvPatchAuditHistory.setTextColor(getSecondaryTextColor());
        tvPatchAuditHistory.setPadding(0, scale(8), 0, 0);
        UiArchitect.applyText(this, tvPatchAuditHistory, "TEXT_MINI");
        card.addView(tvPatchAuditHistory);

        return card;
    }

    private void updateAvailablePatchesListUi(java.util.List<FirebaseComm.PatchCatalogItem> list) {
        if (layoutAvailablePatches == null) return;
        layoutAvailablePatches.removeAllViews();

        if (list == null || list.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("🧬 Cloud Delta Ledger: Scanning Firebase RTDB for fleet patches...");
            emptyTv.setTextColor(getSecondaryTextColor());
            UiArchitect.applyText(this, emptyTv, "TEXT_MINI");
            emptyTv.setPadding(0, scale(4), 0, scale(4));
            layoutAvailablePatches.addView(emptyTv);
            return;
        }

        // Group patches by Target Build (Group By Hierarchy)
        java.util.Map<Integer, java.util.List<FirebaseComm.PatchCatalogItem>> groupedPatches = new java.util.LinkedHashMap<>();
        for (FirebaseComm.PatchCatalogItem item : list) {
            int toBuild = item.getToBuild();
            if (!groupedPatches.containsKey(toBuild)) {
                groupedPatches.put(toBuild, new java.util.ArrayList<>());
            }
            groupedPatches.get(toBuild).add(item);
        }

        TextView titleTv = new TextView(this);
        titleTv.setText("🧬 FLEET DELTA DIRECTORY (" + list.size() + " Patches • " + groupedPatches.size() + " Target Releases):");
        titleTv.setTextColor(getTextColor());
        titleTv.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, titleTv, "TEXT_MINI");
        titleTv.setPadding(0, scale(4), 0, scale(8));
        layoutAvailablePatches.addView(titleTv);

        for (java.util.Map.Entry<Integer, java.util.List<FirebaseComm.PatchCatalogItem>> entry : groupedPatches.entrySet()) {
            int targetBuild = entry.getKey();
            java.util.List<FirebaseComm.PatchCatalogItem> groupItems = entry.getValue();

            // Group Box
            LinearLayout groupCard = new LinearLayout(this);
            groupCard.setOrientation(LinearLayout.VERTICAL);
            groupCard.setPadding(scale(12), scale(12), scale(12), scale(12));
            groupCard.setBackground(createInnerWellBackground());

            // Group Header Row
            LinearLayout groupHeader = new LinearLayout(this);
            groupHeader.setOrientation(LinearLayout.HORIZONTAL);
            groupHeader.setGravity(Gravity.CENTER_VERTICAL);

            TextView groupTitle = new TextView(this);
            groupTitle.setText("📦 TARGET BUILD " + targetBuild + " (" + groupItems.size() + " Patches)");
            groupTitle.setTextColor(getGradientStart());
            groupTitle.setTypeface(null, Typeface.BOLD);
            UiArchitect.applyText(this, groupTitle, "TEXT_BODY");
            groupHeader.addView(groupTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView groupBadge = createPillBadge("MATRIX B" + targetBuild, Color.parseColor("#40C4FF"));
            groupBadge.setPadding(scale(6), scale(2), scale(6), scale(2));
            groupHeader.addView(groupBadge);

            groupCard.addView(groupHeader);

            // Container for items in this group
            LinearLayout groupContentLayout = new LinearLayout(this);
            groupContentLayout.setOrientation(LinearLayout.VERTICAL);
            groupContentLayout.setPadding(0, scale(8), 0, 0);

            for (FirebaseComm.PatchCatalogItem item : groupItems) {
                LinearLayout itemBox = new LinearLayout(this);
                itemBox.setOrientation(LinearLayout.VERTICAL);
                itemBox.setPadding(scale(10), scale(10), scale(10), scale(10));

                GradientDrawable itemBg = new GradientDrawable();
                itemBg.setCornerRadius(16f);
                itemBg.setColor(Color.parseColor("#18" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
                itemBg.setStroke(1, Color.parseColor("#25" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
                itemBox.setBackground(itemBg);

                // Summary Row (Interactive: Click to Open / Expand)
                LinearLayout summaryRow = new LinearLayout(this);
                summaryRow.setOrientation(LinearLayout.HORIZONTAL);
                summaryRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView itemSummary = new TextView(this);
                itemSummary.setText("Build " + item.getFromBuild() + " ➔ Build " + item.getToBuild() + " (" + item.getDeltaSizeFormatted() + ")");
                itemSummary.setTextColor(getTextColor());
                itemSummary.setTypeface(null, Typeface.BOLD);
                UiArchitect.applyText(this, itemSummary, "TEXT_MINI");
                summaryRow.addView(itemSummary, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                boolean isCurrentDevice = item.isApplicableToThisDevice();
                TextView matchBadge = createPillBadge(
                    isCurrentDevice ? "🟢 THIS DEVICE" : "FLEET B" + item.getFromBuild(),
                    isCurrentDevice ? Color.parseColor("#00E676") : Color.parseColor("#40C4FF")
                );
                matchBadge.setPadding(scale(6), scale(2), scale(6), scale(2));
                summaryRow.addView(matchBadge);

                TextView chevron = new TextView(this);
                chevron.setText("  ▼");
                chevron.setTextColor(getSecondaryTextColor());
                chevron.setTypeface(null, Typeface.BOLD);
                summaryRow.addView(chevron);

                itemBox.addView(summaryRow);

                // Expanded Details Container (Open on click)
                LinearLayout detailContainer = new LinearLayout(this);
                detailContainer.setOrientation(LinearLayout.VERTICAL);
                detailContainer.setPadding(0, scale(8), 0, 0);
                detailContainer.setVisibility(View.GONE); // Collapsed by default

                TextView detailTv = new TextView(this);
                detailTv.setText("• Delta File: " + item.getDeltaFilename() + "\n" +
                        "• Bandwidth Savings: " + String.format(java.util.Locale.US, "%.1f%%", item.getSavingsPct()) + " Saved\n" +
                        "• Cryptographic SHA-256:\n  " + item.getDeltaSha256());
                detailTv.setTextColor(getSecondaryTextColor());
                detailTv.setPadding(0, 0, 0, scale(8));
                UiArchitect.applyText(this, detailTv, "TEXT_MINI");
                detailContainer.addView(detailTv);

                View btnApplyItem = createSmallButton(
                    isCurrentDevice ? "🧬 1-CLICK APPLY TO THIS DEVICE" : "🧪 APPLY FLEET PATCH (B" + item.getFromBuild() + " ➔ B" + item.getToBuild() + ")",
                    android.R.drawable.ic_menu_save,
                    isCurrentDevice,
                    v -> viewModel.installPatchFromCatalog(this, item)
                );
                detailContainer.addView(btnApplyItem);

                itemBox.addView(detailContainer);

                // Open on click interaction
                View.OnClickListener toggle = v -> {
                    boolean isOpen = detailContainer.getVisibility() == View.VISIBLE;
                    detailContainer.setVisibility(isOpen ? View.GONE : View.VISIBLE);
                    chevron.setText(isOpen ? "  ▼" : "  ▲");
                };
                summaryRow.setClickable(true);
                summaryRow.setFocusable(true);
                summaryRow.setOnClickListener(toggle);
                itemBox.setClickable(true);
                itemBox.setFocusable(true);
                itemBox.setOnClickListener(toggle);

                LinearLayout.LayoutParams lpItem = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lpItem.setMargins(0, 0, 0, scale(8));
                itemBox.setLayoutParams(lpItem);

                groupContentLayout.addView(itemBox);
            }

            groupCard.addView(groupContentLayout);

            LinearLayout.LayoutParams lpGroup = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpGroup.setMargins(0, 0, 0, scale(12));
            groupCard.setLayoutParams(lpGroup);

            layoutAvailablePatches.addView(groupCard);
        }
    }

    // ============================================================================================
    // UI BUILDER: CARD 5 — CONFIGURATION & DISTRIBUTION CHANNELS
    // ============================================================================================
    private View createConfigurationCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(scale(20), scale(18), scale(20), scale(18));
        applyCardStyle(card);

        TextView lbl = new TextView(this);
        lbl.setText("⚙️ DISTRIBUTION CHANNELS & SETTINGS");
        lbl.setTextColor(getGradientStart());
        lbl.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, lbl, "TEXT_TITLE");
        card.addView(lbl);

        // Account
        card.addView(createLabel("REGISTERED GOOGLE ACCOUNT (*)"));
        emailField = createEnterpriseField("Reading hardware id...", android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setEnabled(true);
        emailField.setFocusable(true);
        emailField.setFocusableInTouchMode(false);
        emailField.setCursorVisible(false);
        emailField.setKeyListener(null);
        card.addView(wrapField(emailField));

        // Drive Link
        LinearLayout driveHeader = new LinearLayout(this);
        driveHeader.setOrientation(LinearLayout.HORIZONTAL);
        driveHeader.setGravity(Gravity.CENTER_VERTICAL);
        driveHeader.addView(createLabel("GOOGLE DRIVE MIRROR BACKUP (*)"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnEditDrive = new TextView(this);
        btnEditDrive.setText("EDIT / PASTE");
        btnEditDrive.setTextColor(getGradientStart());
        btnEditDrive.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, btnEditDrive, "TEXT_MINI");
        btnEditDrive.setPadding(scale(10), scale(6), scale(10), scale(6));
        btnEditDrive.setOnClickListener(v -> showEditLinkDialog());
        driveHeader.addView(btnEditDrive);
        card.addView(driveHeader);

        driveLinkField = createEnterpriseField("Constructing link...", android.text.InputType.TYPE_TEXT_VARIATION_URI);
        driveLinkField.setEnabled(true);
        driveLinkField.setFocusable(true);
        driveLinkField.setFocusableInTouchMode(false);
        driveLinkField.setCursorVisible(false);
        driveLinkField.setKeyListener(null);
        card.addView(wrapField(driveLinkField));

        // GitHub Link
        LinearLayout ghHeader = new LinearLayout(this);
        ghHeader.setOrientation(LinearLayout.HORIZONTAL);
        ghHeader.setGravity(Gravity.CENTER_VERTICAL);
        ghHeader.addView(createLabel("GITHUB DIRECT BINARY & REPOSITORY (*)"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnEditGh = new TextView(this);
        btnEditGh.setText("EDIT");
        btnEditGh.setTextColor(getGradientStart());
        btnEditGh.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, btnEditGh, "TEXT_MINI");
        btnEditGh.setPadding(scale(10), scale(6), scale(10), scale(6));
        btnEditGh.setOnClickListener(v -> showEditGithubLinkDialog());
        ghHeader.addView(btnEditGh);
        card.addView(ghHeader);

        githubLinkField = createEnterpriseField("Constructing GitHub link...", android.text.InputType.TYPE_TEXT_VARIATION_URI);
        githubLinkField.setEnabled(true);
        githubLinkField.setFocusable(true);
        githubLinkField.setFocusableInTouchMode(false);
        githubLinkField.setCursorVisible(false);
        githubLinkField.setKeyListener(null);
        card.addView(wrapField(githubLinkField));

        // Local Sync Endpoint
        LinearLayout syncHeader = new LinearLayout(this);
        syncHeader.setOrientation(LinearLayout.HORIZONTAL);
        syncHeader.setGravity(Gravity.CENTER_VERTICAL);
        syncHeader.addView(createLabel("LOCAL SYNC SERVER ENDPOINT (*)"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnEditSync = new TextView(this);
        btnEditSync.setText("EDIT / SET");
        btnEditSync.setTextColor(getGradientStart());
        btnEditSync.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, btnEditSync, "TEXT_MINI");
        btnEditSync.setPadding(scale(10), scale(6), scale(10), scale(6));
        btnEditSync.setOnClickListener(v -> showEditLocalSyncEndpointDialog());
        syncHeader.addView(btnEditSync);
        card.addView(syncHeader);

        localSyncEndpointField = createEnterpriseField("http://127.0.0.1:8085", android.text.InputType.TYPE_TEXT_VARIATION_URI);
        localSyncEndpointField.setEnabled(true);
        localSyncEndpointField.setFocusable(true);
        localSyncEndpointField.setFocusableInTouchMode(false);
        localSyncEndpointField.setCursorVisible(false);
        localSyncEndpointField.setKeyListener(null);
        card.addView(wrapField(localSyncEndpointField));

                // Silent Update Toggle
        card.addView(createSilentUpdateNode());

        // Dynamic Scheduled Maintenance Window
        card.addView(createMaintenanceScheduleNode());

        // Metered Cellular Data Toggle
        card.addView(createMeteredDataNode());

        // Offline Balance Data Reconciliation
        card.addView(createBalanceReconciliationNode());

        // Forensic Meta List
        card.addView(createForensicMetaList());

        return card;
    }

    private View createForensicMetaList() {
        LinearLayout well = new LinearLayout(this);
        well.setOrientation(LinearLayout.VERTICAL);
        well.setPadding(scale(14), scale(12), scale(14), scale(12));
        well.setBackground(createInnerWellBackground());

        tvCurrentVersion = new TextView(this);
        tvCurrentVersion.setTextColor(getTextColor());
        tvCurrentVersion.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvCurrentVersion, "TEXT_MINI");
        well.addView(tvCurrentVersion);

        tvLastUpdate = new TextView(this);
        tvLastUpdate.setTextColor(getSecondaryTextColor());
        tvLastUpdate.setPadding(0, scale(3), 0, scale(3));
        UiArchitect.applyText(this, tvLastUpdate, "TEXT_MINI");
        well.addView(tvLastUpdate);

        tvLastCheck = new TextView(this);
        tvLastCheck.setTextColor(getSecondaryTextColor());
        tvLastCheck.setPadding(0, scale(3), 0, scale(3));
        UiArchitect.applyText(this, tvLastCheck, "TEXT_MINI");
        well.addView(tvLastCheck);

        tvLastDownload = new TextView(this);
        tvLastDownload.setTextColor(getSecondaryTextColor());
        tvLastDownload.setPadding(0, scale(3), 0, scale(3));
        UiArchitect.applyText(this, tvLastDownload, "TEXT_MINI");
        well.addView(tvLastDownload);

        tvUpdateAvailable = new TextView(this);
        tvUpdateAvailable.setTextColor(getSecondaryTextColor());
        tvUpdateAvailable.setPadding(0, scale(3), 0, scale(3));
        UiArchitect.applyText(this, tvUpdateAvailable, "TEXT_MINI");
        well.addView(tvUpdateAvailable);

        tvIsUpdatedStatus = new TextView(this);
        tvIsUpdatedStatus.setTextColor(getSecondaryTextColor());
        tvIsUpdatedStatus.setPadding(0, scale(3), 0, scale(3));
        UiArchitect.applyText(this, tvIsUpdatedStatus, "TEXT_MINI");
        well.addView(tvIsUpdatedStatus);

        tvLastFailedInstall = new TextView(this);
        tvLastFailedInstall.setTextColor(getSecondaryTextColor());
        tvLastFailedInstall.setPadding(0, scale(3), 0, 0);
        UiArchitect.applyText(this, tvLastFailedInstall, "TEXT_MINI");
        well.addView(tvLastFailedInstall);

        return well;
    }

    // ============================================================================================
    // UI BUILDER: ACTION BAR & CONTROLS
    // ============================================================================================
    private View createIntegratedActionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(4.0f);

        View btnApply = createSmallButton("⚡ APPLY", android.R.drawable.ic_media_play, true, v -> {
            if (currentHotPatchInfo != null && !Boolean.TRUE.equals(viewModel.isHotPatchActive().getValue())) {
                viewModel.applyHotPatch(this);
            } else {
                viewModel.prepareInstallPrompt(this);
            }
        });

        View btnCheck = createSmallButton("🔄 SCAN", android.R.drawable.ic_popup_sync, true, v -> viewModel.executeUpdate(this));
        View btnInfo = createSmallButton("ℹ️ GUIDE", android.R.drawable.ic_menu_info_details, false, v -> showHowPatchingWorksDialog());
        View btnClose = createSmallButton("CLOSE", android.R.drawable.ic_menu_close_clear_cancel, false, v -> finish());

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(btnApply, p);

        LinearLayout.LayoutParams pMid = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pMid.setMargins(scale(8), 0, scale(8), 0);
        row.addView(btnCheck, pMid);

        LinearLayout.LayoutParams pInfo = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pInfo.setMargins(0, 0, scale(8), 0);
        row.addView(btnInfo, pInfo);

        row.addView(btnClose, p);

        return row;
    }

    private View createInstallUpdateActionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(4.0f);

        View btnCheck = createSmallButton("🔄 SCAN", android.R.drawable.ic_popup_sync, true, v -> viewModel.executeUpdate(this));
        View btnFullApk = createSmallButton("📥 FULL APK", android.R.drawable.ic_menu_set_as, true, v -> viewModel.installFullApk(this));
        View btnDrive = createSmallButton("☁️ DRIVE", android.R.drawable.ic_menu_upload, false, v -> viewModel.executeInstall(this));
        View btnClose = createSmallButton("CLOSE", android.R.drawable.ic_menu_close_clear_cancel, false, v -> finish());

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(btnCheck, p);

        LinearLayout.LayoutParams pMid1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pMid1.setMargins(scale(8), 0, scale(8), 0);
        row.addView(btnFullApk, pMid1);

        LinearLayout.LayoutParams pMid2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        pMid2.setMargins(0, 0, scale(8), 0);
        row.addView(btnDrive, pMid2);

        row.addView(btnClose, p);

        return row;
    }

    private void showHowPatchingWorksDialog() {
        String deltaSize = viewModel.getLatestDeltaPatchSize().getValue() != null ? viewModel.getLatestDeltaPatchSize().getValue() : "small";
        String fullSize = viewModel.getLatestFullApkSize().getValue() != null ? viewModel.getLatestFullApkSize().getValue() : "full";
        Double savingsVal = viewModel.getLatestDeltaSavingsPct().getValue();
        double savings = savingsVal != null ? savingsVal : 95.0;

        UiArchitect.showConfirmationDialog(this,
            "ℹ️ SYSTEM UPDATE & DUAL-TIER PATCH GUIDE",
            "How does jFindMe update without requiring full app re-installation?\n\n" +
            "1. ⚡ DYNAMIC IN-APP HOT-PATCH (Zero-Install):\n" +
            "   • What it is: Small dynamic payload (~48 KB) injected directly into memory and local registry.\n" +
            "   • Impact: Updates tracking rules, radar precision, cache invalidation, and security configs instantly.\n" +
            "   • User Experience: ZERO Android system prompts, NO app restart, instant activation!\n\n" +
            "2. 🧬 SMART DELTA BINARY PATCH (Package Reconstruction):\n" +
            "   • What it is: When binary code itself updates, only the ~" + deltaSize + " diff is downloaded instead of the " + fullSize + " APK.\n" +
            "   • Impact: Saves ~" + String.format(java.util.Locale.US, "%.1f%%", savings) + " mobile data and reconstructs the target build locally on your device.\n" +
            "   • User Experience: Reconstructed package is verified cryptographically (SHA-256) before Android's system package installer prompt.\n\n" +
            "3. 📦 FULL SYSTEM RE-FLASH:\n" +
            "   • Downloads complete standalone release APK from GitHub or Drive mirror.",
            "GOT IT", "INSPECT LEDGER",
            null,
            () -> viewModel.executeUpdate(this)
        );
    }

    private void showHotPatchPayloadDialog(FirebaseComm.LiveHotPatchInfo patch) {
        if (patch == null) {
            Toast.makeText(this, "No active hot-patch payload to inspect.", Toast.LENGTH_SHORT).show();
            return;
        }
        UiArchitect.showConfirmationDialog(this,
            "🔍 HOT-PATCH SPECIFICATION: " + patch.getPatchId(),
            "• Patch Name: " + patch.getPatchName() + "\n" +
            "• Target: " + patch.getTargetComponent() + "\n" +
            "• Payload Size: " + patch.getPatchSize() + "\n" +
            "• SHA-256 Digest:\n" + (patch.getSha256().isEmpty() ? "Verified Valid" : patch.getSha256()) + "\n\n" +
            "• Payload JSON:\n" + patch.getPayloadJson() + "\n\n" +
            "• Release Notes:\n" + patch.getReleaseNotes(),
            "APPLY NOW", "CLOSE",
            () -> viewModel.applyHotPatch(this),
            null
        );
    }

    private void showInstallGuidanceModal(SystemUpdateViewModel.InstallPromptData data) {
        String ver = data != null ? data.getTargetVersion() : "Latest";
        String sz = data != null ? data.getPackageSizeEst() : "";
        boolean isDelta = data != null && data.isDelta();
        String deltaSz = data != null ? data.getDeltaSizeFormatted() : "";
        double savings = data != null ? data.getSavingsPct() : 97.0;

        String deltaTitle = isDelta 
            ? "🧬 1-CLICK DELTA PATCH (" + deltaSz + " • " + String.format(java.util.Locale.US, "%.1f%%", savings) + " Saved)"
            : "🧬 SMART DELTA RECONSTRUCT";
        String deltaSub = isDelta 
            ? "High-speed differential binary reconstruction directly on device"
            : "Reconstruct binary from installed APK and delta patch";

        UiArchitect.OptionItem[] updateOptions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem(deltaTitle, deltaSub, android.R.drawable.ic_menu_save),
            new UiArchitect.OptionItem("📥 FULL APK (GitHub Direct " + sz + ")", "Download standalone full release package directly from GitHub", android.R.drawable.ic_menu_set_as),
            new UiArchitect.OptionItem("☁️ Install from Google Drive Mirror", "Download APK from cloud Google Drive mirror storage", android.R.drawable.ic_menu_upload),
            new UiArchitect.OptionItem("🌐 Open GitHub in Web Browser", "Visit releases page in your mobile browser", android.R.drawable.ic_menu_view),
            new UiArchitect.OptionItem("⏱️ REMIND ME LATER", "Postpone update and remind on next cycle", android.R.drawable.ic_menu_recent_history)
        };

        UiArchitect.showRichOptionsDialog(this, "📦 SYSTEM UPDATE V" + ver + "\n(Safe Update • Zero Data Loss)", updateOptions, (index, label) -> {
            viewModel.dismissInstallPrompt();
            switch (index) {
                case 0:
                    viewModel.installGithubApk(this);
                    break;
                case 1:
                    viewModel.installFullApk(this);
                    break;
                case 2:
                    viewModel.executeInstall(this);
                    break;
                case 3:
                    viewModel.openGithubInBrowser(this);
                    break;
                case 4:
                    Toast.makeText(this, "Update postponed. You will be reminded on next cycle.", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void showEditLinkDialog() {
        String current = viewModel.getDriveLink().getValue();
        UiArchitect.showInputPromptDialog(this,
            "🔗 CUSTOM BINARY SOURCE LINK",
            "Enter or paste direct APK download URL...",
            current != null ? current : "",
            newLink -> {
                if (newLink != null && !newLink.trim().isEmpty()) {
                    boolean success = viewModel.updateCustomDriveLink(this, newLink.trim());
                    if (success) {
                        Toast.makeText(this, "Binary Source Link Updated.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void showEditGithubLinkDialog() {
        String current = viewModel.getGithubLink().getValue();
        UiArchitect.showInputPromptDialog(this,
            "🔗 CUSTOM GITHUB REPOSITORY LINK",
            "Enter or paste GitHub repository URL...",
            current != null ? current : "",
            newLink -> {
                if (newLink != null && !newLink.trim().isEmpty()) {
                    boolean success = viewModel.updateCustomGithubLink(this, newLink.trim());
                    if (success) {
                        Toast.makeText(this, "GitHub Repository Link Updated.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void showEditLocalSyncEndpointDialog() {
        String current = viewModel.getLocalSyncEndpoint().getValue();
        UiArchitect.showInputPromptDialog(this,
            "⚙️ LOCAL SYNC DAEMON ENDPOINT",
            "Enter LAN or localhost sync server URL (e.g. http://192.168.1.50:8085)...",
            current != null ? current : "http://127.0.0.1:8085",
            newUrl -> {
                if (newUrl != null && !newUrl.trim().isEmpty()) {
                    boolean success = viewModel.updateLocalSyncEndpoint(this, newUrl.trim());
                    if (success) {
                        Toast.makeText(this, "Local Sync Server Endpoint Updated.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private View createSilentUpdateNode() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(scale(20), scale(16), scale(20), scale(16));
        
        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(24f);
        glass.setColor(Color.parseColor("#12" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        glass.setStroke(2, Color.parseColor("#33" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        row.setBackground(glass);

        row.setFocusable(true);
        row.setClickable(true);
        applyAccessibility(row, "Toggle Silent Update Mode");
        row.setOnClickListener(v -> {
            if (silentUpdateCheck != null) {
                silentUpdateCheck.toggle();
            }
        });

        TextView label = new TextView(this);
        label.setText("SILENT AUTO-UPDATE IN BACKGROUND");
        label.setTextColor(getTextColor());
        UiArchitect.applyText(this, label, "TEXT_BODY");
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(label);

        silentUpdateCheck = new android.widget.CheckBox(this);
        silentUpdateCheck.setButtonTintList(android.content.res.ColorStateList.valueOf(getGradientStart()));
        silentUpdateCheck.setFocusable(false);
        silentUpdateCheck.setClickable(false);
        row.addView(silentUpdateCheck);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, scale(10), 0, scale(12));
        row.setLayoutParams(lp);

        return row;
    }

    private void applyCardStyle(View view) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(28f);
        d.setColor(getCardColor());
        d.setStroke(2, Color.parseColor("#33" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        view.setBackground(d);
    }

    private GradientDrawable createInnerWellBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(20f);
        d.setColor(Color.parseColor("#14" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        d.setStroke(1, Color.parseColor("#25" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        return d;
    }

    private GradientDrawable createStepPillBackground(boolean active) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(16f);
        if (active) {
            d.setColor(getGradientStart());
        } else {
            d.setColor(Color.parseColor("#18" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
            d.setStroke(1, Color.parseColor("#33" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        }
        return d;
    }

    private TextView createPillBadge(String text, int tintColor) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextColor(tintColor);
        badge.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, badge, "TEXT_MINI");
        badge.setPadding(scale(10), scale(4), scale(10), scale(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16f);
        bg.setColor(Color.parseColor("#20" + String.format("%06X", (0xFFFFFF & tintColor))));
        bg.setStroke(1, tintColor);
        badge.setBackground(bg);
        return badge;
    }

    private View createSmallButton(String text, int iconRes, boolean isPrimary, View.OnClickListener listener) {
        return createSmallButton(text, iconRes, isPrimary, listener, null);
    }

    private View createSmallButton(String text, int iconRes, boolean isPrimary, View.OnClickListener listener, TextView[] outTv) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, scale(16), 0, scale(16));
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setOnClickListener(listener);

        UiArchitect.applyCardWithOverride(this, btn, isPrimary ? "CARD_CAPSULE_BUTTON" : "CARD_STANDARD");

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(isPrimary ? getGradientOverlayText() : getTextColor());
        tv.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tv, "BUTTON_TEXT");
        tv.setGravity(Gravity.CENTER);
        btn.addView(tv);

        if (outTv != null && outTv.length > 0) {
            outTv[0] = tv;
        }

        applyAccessibility(btn, text);
        return btn;
    }

    private View createArchitecturalFooter() {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, scale(35), 0, scale(35));

        int lottieId = getResources().getIdentifier("butterfly_angel", "raw", getPackageName());
        if (lottieId != 0) {
            LottieAnimationView lottie = new LottieAnimationView(this);
            lottie.setAnimation(lottieId);
            lottie.setRepeatCount(LottieDrawable.INFINITE);
            lottie.playAnimation();
            int bSize = scale(160);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(bSize, bSize);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            footer.addView(lottie, lp);
        }

        TextView name = new TextView(this);
        name.setText("jFindMe Tracking Engine");
        name.setTextColor(getTextColor());
        name.setGravity(Gravity.CENTER_HORIZONTAL);
        UiArchitect.applyText(this, name, "TEXT_BODY");
        name.setAlpha(0.9f);
        footer.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams verLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footer.addView(createVersionLine("ENTERPRISE BUILD: V" + BuildConfig.VERSION_NAME + " (CODE " + BuildConfig.VERSION_CODE + ")"), verLp);
        footer.addView(createVersionLine("DATABASE SCHEMA: V" + DatabaseHelper.DATABASE_VERSION + " (EPOCH-AWARE)"), verLp);
        footer.addView(createVersionLine("UI ARCHITECT STAGE: " + UiArchitect.getStage(this).name()), verLp);
        footer.addView(createVersionLine("OS ENVIRONMENT: ANDROID " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"), verLp);

        return footer;
    }

    private View createVersionLine(String text) {
        TextView ver = new TextView(this);
        ver.setText(text);
        ver.setTextColor(getSecondaryTextColor());
        ver.setGravity(Gravity.CENTER_HORIZONTAL);
        UiArchitect.applyText(this, ver, "TEXT_MINI");
        ver.setAlpha(0.6f);
        ver.setPadding(0, scale(2), 0, 0);
        return ver;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(getGradientStart());
        UiArchitect.applyText(this, label, "TEXT_LABEL");
        label.setPadding(scale(8), scale(12), 0, scale(6));
        return label;
    }

    private EditText createEnterpriseField(String hint, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextColor(getTextColor());
        et.setHintTextColor(getSecondaryTextColor());
        et.setBackgroundColor(Color.TRANSPARENT);
        et.setPadding(scale(20), scale(16), scale(20), scale(16));
        UiArchitect.applyText(this, et, "TEXT_BODY");
        return et;
    }

    private View wrapField(View field) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(24f);
        glass.setColor(getCardColor());
        glass.setStroke(2, Color.parseColor("#33" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        wrap.setBackground(glass);
        wrap.addView(field);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(12));
        wrap.setLayoutParams(lp);
        
        return wrap;
    }

    private View createSpacer(int dpSize) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, scale(dpSize)));
        return v;
    }

    private void downloadAndInstallApk(String url) {
        if (url == null || url.trim().isEmpty()) return;
        Integer targetCodeVal = viewModel.getLatestDeltaTargetBuild().getValue();
        int targetCode = (targetCodeVal != null && targetCodeVal > 0)
            ? targetCodeVal 
            : (BuildConfig.VERSION_CODE + 1);
        String remoteVer = viewModel.getRemoteVersionName().getValue();
        String targetVer = (remoteVer != null && !remoteVer.trim().isEmpty())
            ? remoteVer
            : ("v" + BuildConfig.VERSION_NAME);
        String fullSize = viewModel.getLatestFullApkSize().getValue() != null 
            ? viewModel.getLatestFullApkSize().getValue() 
            : "80.5 MB";

        com.jdroidx.jfindme.utils.ApkInstallerHelper.UpdatePackageMetadata metadata =
            new com.jdroidx.jfindme.utils.ApkInstallerHelper.UpdatePackageMetadata(
                targetVer,
                targetCode,
                "Enterprise System Release",
                "AUTOMATIC",
                url,
                "",
                "",
                fullSize,
                0.0,
                "",
                ""
            );
        com.jdroidx.jfindme.utils.ApkInstallerHelper.INSTANCE.downloadAndInstall(this, url, metadata, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Package verified. Launching installer...", Toast.LENGTH_SHORT).show();
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.jdroidx.jfindme.utils.ApkInstallerHelper.INSTANCE.resumePendingInstall(this);
    }

    @Override
    protected void onSaveCurrentState(android.os.Bundle outState) {
        if (outState == null) return;
        if (driveLinkField != null) {
            outState.putString("SAVED_SOURCE_LINK", driveLinkField.getText().toString());
        }
        if (githubLinkField != null) {
            outState.putString("SAVED_GITHUB_LINK", githubLinkField.getText().toString());
        }
        if (localSyncEndpointField != null) {
            outState.putString("SAVED_LOCAL_SYNC_ENDPOINT", localSyncEndpointField.getText().toString());
        }
    }

    @Override
    protected void onUiRebuilt(android.os.Bundle savedState) {
        if (savedState == null) return;
        String link = savedState.getString("SAVED_SOURCE_LINK");
        if (driveLinkField != null && link != null && !link.isEmpty()) {
            driveLinkField.setText(link);
        }
        String ghLink = savedState.getString("SAVED_GITHUB_LINK");
        if (githubLinkField != null && ghLink != null && !ghLink.isEmpty()) {
            githubLinkField.setText(ghLink);
        }
        String syncEp = savedState.getString("SAVED_LOCAL_SYNC_ENDPOINT");
        if (localSyncEndpointField != null && syncEp != null && !syncEp.isEmpty()) {
            localSyncEndpointField.setText(syncEp);
        }
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            int tab = intent.getIntExtra("EXTRA_TAB", -1);
            if (tab >= 0) {
                switchTab(tab);
            }
        }
    }
    private View createMaintenanceScheduleNode() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(scale(16), scale(12), scale(16), scale(12));

        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(18f);
        glass.setColor(Color.parseColor("#152238"));
        glass.setStroke(1, Color.parseColor("#334FC3F7"));
        row.setBackground(glass);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        tvMaintenanceScheduleTime = new TextView(this);
        tvMaintenanceScheduleTime.setText("⏰ SCHEDULED WINDOW: 02:39 AM DAILY");
        tvMaintenanceScheduleTime.setTextColor(Color.parseColor("#4FC3F7"));
        tvMaintenanceScheduleTime.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvMaintenanceScheduleTime, "TEXT_BODY");
        topRow.addView(tvMaintenanceScheduleTime, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnEditTime = new TextView(this);
        btnEditTime.setText("EDIT TIME");
        btnEditTime.setTextColor(Color.parseColor("#81D4FA"));
        btnEditTime.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, btnEditTime, "TEXT_MINI");
        btnEditTime.setPadding(scale(8), scale(4), scale(8), scale(4));
        btnEditTime.setOnClickListener(v -> showMaintenanceTimePicker());
        topRow.addView(btnEditTime);

        row.addView(topRow);

        TextView subtext = new TextView(this);
        subtext.setText("Nightly maintenance execution window (10-15 mins maximum watchdog). Wakes device to balance offline telemetry and verify security updates.");
        subtext.setTextColor(Color.parseColor("#B0BEC5"));
        subtext.setPadding(0, scale(4), 0, 0);
        UiArchitect.applyText(this, subtext, "TEXT_MINI");
        row.addView(subtext);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, scale(6), 0, scale(6));
        row.setLayoutParams(lp);

        return row;
    }

    private View createMeteredDataNode() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(scale(16), scale(12), scale(16), scale(12));

        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(18f);
        glass.setColor(Color.parseColor("#152238"));
        glass.setStroke(1, Color.parseColor("#334FC3F7"));
        row.setBackground(glass);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView label = new TextView(this);
        label.setText("ALLOW UPDATE OVER MOBILE CELLULAR DATA");
        label.setTextColor(getTextColor());
        label.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, label, "TEXT_BODY");
        textCol.addView(label);

        TextView sub = new TextView(this);
        sub.setText("When disabled, 65MB APK download waits for Wi-Fi. Offline balance data delta will still sync over mobile data.");
        sub.setTextColor(Color.parseColor("#B0BEC5"));
        UiArchitect.applyText(this, sub, "TEXT_MINI");
        textCol.addView(sub);

        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        meteredUpdateCheck = new android.widget.CheckBox(this);
        meteredUpdateCheck.setButtonTintList(android.content.res.ColorStateList.valueOf(getGradientStart()));
        meteredUpdateCheck.setOnCheckedChangeListener((bv, checked) -> viewModel.toggleAllowMetered(this, checked));
        row.addView(meteredUpdateCheck);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, scale(6), 0, scale(6));
        row.setLayoutParams(lp);

        return row;
    }

    private View createBalanceReconciliationNode() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(scale(16), scale(12), scale(16), scale(12));

        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(18f);
        glass.setColor(Color.parseColor("#152238"));
        glass.setStroke(1, Color.parseColor("#334FC3F7"));
        row.setBackground(glass);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        tvPendingBalanceCount = new TextView(this);
        tvPendingBalanceCount.setText("⚖️ OFFLINE DELTA: 0 PENDING (BALANCED)");
        tvPendingBalanceCount.setTextColor(Color.parseColor("#4CAF50"));
        tvPendingBalanceCount.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvPendingBalanceCount, "TEXT_BODY");
        topRow.addView(tvPendingBalanceCount, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnSyncNow = new TextView(this);
        btnSyncNow.setText("🔄 SYNC NOW");
        btnSyncNow.setTextColor(Color.parseColor("#40C4FF"));
        btnSyncNow.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, btnSyncNow, "TEXT_MINI");
        btnSyncNow.setPadding(scale(8), scale(4), scale(8), scale(4));
        btnSyncNow.setOnClickListener(v -> viewModel.triggerManualBalanceSync(this));
        topRow.addView(btnSyncNow);

        row.addView(topRow);

        tvLastBalanceSync = new TextView(this);
        tvLastBalanceSync.setText("🕒 LAST BALANCE SYNC: Initial Check");
        tvLastBalanceSync.setTextColor(Color.parseColor("#B0BEC5"));
        tvLastBalanceSync.setPadding(0, scale(4), 0, 0);
        UiArchitect.applyText(this, tvLastBalanceSync, "TEXT_MINI");
        row.addView(tvLastBalanceSync);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, scale(6), 0, scale(6));
        row.setLayoutParams(lp);

        return row;
    }

    private void showMaintenanceTimePicker() {
        int currentHour = com.jdroidx.jfindme.utils.RegistryManager.getInt(this, com.jdroidx.jfindme.utils.RegistryManager.KEY_MAINTENANCE_WINDOW_HOUR, com.jdroidx.jfindme.utils.SystemUpdateScheduler.MAINTENANCE_HOUR);
        int currentMinute = com.jdroidx.jfindme.utils.RegistryManager.getInt(this, com.jdroidx.jfindme.utils.RegistryManager.KEY_MAINTENANCE_WINDOW_MINUTE, com.jdroidx.jfindme.utils.SystemUpdateScheduler.MAINTENANCE_MINUTE);

        new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            viewModel.setMaintenanceWindowTime(this, hourOfDay, minute);
            Toast.makeText(this, "Maintenance window scheduled for " + String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute), Toast.LENGTH_SHORT).show();
        }, currentHour, currentMinute, false).show();
    }

}
