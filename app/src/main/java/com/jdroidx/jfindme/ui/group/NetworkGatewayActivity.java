package com.jdroidx.jfindme.ui.group;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputFilter;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.jdroidx.jfindme.data.repository.PairingRepository;
import com.jdroidx.jfindme.engine.sync.FirebasePairingManager;
import com.jdroidx.jfindme.engine.sync.LocalPairingManager;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.map.MapActivity;
import com.jdroidx.jfindme.utils.AvatarLoader;
import com.jdroidx.jfindme.utils.DeviceIdentityHelper;
import com.jdroidx.jfindme.utils.RegistryManager;
import com.jdroidx.jfindme.utils.SystemLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 🔗 UNIFIED ENTERPRISE NETWORK GATEWAY SCREEN (L7 MVVM / Clean Architecture)
 * Modernized Space Management & Layout (Phases 1-3 Complete):
 * 1. Roster Tab as FIRST item with prominent font sizing and dynamic search.
 * 2. High-Density, Ultra-Compact Owner Identity Banner.
 * 3. Symmetrical 2x2 Grid for Host Session:
 *    - R1C1: Square Action Button ("GENERATE CLOUD PIN" / "BROADCAST LOCAL TOKEN")
 *    - R1C2: Square QR Code Box (Matching proportional size)
 *    - R2C1: Copy & Share Buttons side-by-side
 *    - R2C2: Monospace Code Display Text
 * 4. Closed-Loop SSOT SQLite + Firebase Parity and reactive ViewModel binding.
 */
public class NetworkGatewayActivity extends BaseActivityJava {

    private static final String TAG = "NetworkGatewayActivity";

    // Tab Constants: ROSTER IS FIRST TAB
    private static final int TAB_ACTIVE_ROSTER = 0;
    private static final int TAB_REMOTE_CLOUD = 1;
    private static final int TAB_NEARBY_P2P = 2;
    private int selectedTab = TAB_ACTIVE_ROSTER;

    // UI Tab Containers & Segment Buttons
    private LinearLayout tabRosterBtn, tabRemoteBtn, tabNearbyBtn;
    private TextView tvTabRoster, tvTabRemote, tvTabNearby;
    private LinearLayout panelRoster, panelRemote, panelNearby;
    private LinearLayout rosterContainer;
    private LinearLayout masterScrollRoot;
    private ImageView ownerAvatarView;
    private TextView ownerNameView;
    private TextView ownerMetaView;

    // Option A (Remote Cloud) Fields
    private TextView tvCode, tvSyncStatus;
    private ProgressBar pbCodeLoading;
    private ImageView ivQrCode;
    private View qrPlaceholderCloud;
    private EditText etCode;

    // Option B (Nearby P2P) Fields
    private TextView tvLocalCode, tvLocalSyncStatus;
    private ProgressBar pbLocalLoading;
    private ImageView ivLocalQrCode;
    private View qrPlaceholderLocal;
    private EditText etLocalCode;

    // Architectural MVVM
    private GroupViewModel viewModel;
    private String forensicAgentId = "";
    private String safeUid = "LOCAL_DEVICE";

    // QR Code Scanner Result Launcher
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String rawCode = result.getContents().trim().toUpperCase();
                    triggerHapticFeedback();
                    showTacticalHandshakeOverlay(rawCode);

                    if (rawCode.length() == 8) {
                        if (etLocalCode != null) etLocalCode.setText(rawCode);
                        switchToTab(TAB_NEARBY_P2P);
                        showRoleSelectionDialogForLocalJoin(rawCode);
                        UiArchitect.showSlideNotificationBanner(this, "Local Node Scanned", "8-digit hardware PIN identified. Confirm role to connect.", false, 3000, null, null);
                    } else if (rawCode.length() == 6) {
                        if (etCode != null) etCode.setText(rawCode);
                        switchToTab(TAB_REMOTE_CLOUD);
                        showRoleSelectionDialogBeforeJoin(rawCode);
                        UiArchitect.showSlideNotificationBanner(this, "Cloud Invite Scanned", "6-digit circle code verified. Select relationship role.", false, 3000, null, null);
                    } else {
                        UiArchitect.showSlideNotificationBanner(this, "QR Format Warning", "Unrecognized QR code (" + rawCode.length() + " chars). Expected 6 or 8 digits.", true, 3500, null, null);
                    }
                }
            }
    );

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // 1. Sleek Compact Header with Live Sync Action
        mainRoot.addView(buildHeaderBar());

        // 2. Scrollable Body
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        masterScrollRoot = new LinearLayout(this);
        masterScrollRoot.setOrientation(LinearLayout.VERTICAL);
        int pad = getResponsivePaddingQuota();
        masterScrollRoot.setPadding(pad, pad / 3, pad, pad);

        // 3. Ultra-Compact Owner Identity Banner
        masterScrollRoot.addView(buildCompactIdentityStrip());

        // 4. Segmented Control Switcher (ROSTER FIRST)
        masterScrollRoot.addView(buildSegmentedTabSwitcher());

        // 5. Modality Panels (In-Place Swapping)
        panelRoster = buildActiveRosterPanel();
        panelRemote = buildRemoteOptionPanel();
        panelNearby = buildNearbyOptionPanel();

        masterScrollRoot.addView(panelRoster);
        masterScrollRoot.addView(panelRemote);
        masterScrollRoot.addView(panelNearby);

        scroll.addView(masterScrollRoot);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // Initialize state to Roster tab (First Item)
        updateTabVisibility();

        return mainRoot;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.i(TAG, "Initializing NetworkGatewayActivity MVVM logic pipeline.");
        viewModel = new ViewModelProvider(this).get(GroupViewModel.class);

        // Initialize safe owner ID
        SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        String authUid = FirebaseAuth.getInstance().getUid();
        String resolvedAgentId = (authUid != null) ? db.resolveAgentIdLocally(authUid) : null;
        forensicAgentId = (resolvedAgentId != null) ? resolvedAgentId : jPrefs.getString("agent_id", RegistryManager.getOwnerAgentId(this));
        safeUid = (authUid != null) ? authUid : forensicAgentId;

        // Reactive Observers
        viewModel.getSyncStatus().observe(this, status -> {
            if (status != null) {
                if (tvSyncStatus != null) tvSyncStatus.setText(status);
                if (tvLocalSyncStatus != null) tvLocalSyncStatus.setText(status);
            }
        });

        viewModel.isLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            if (pbCodeLoading != null) pbCodeLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (pbLocalLoading != null) pbLocalLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getRosterData().observe(this, this::rebuildRosterView);

        viewModel.getTimerDisplay().observe(this, timeStr -> {
            if (timeStr != null && !timeStr.isEmpty()) {
                if (selectedTab == TAB_REMOTE_CLOUD && tvSyncStatus != null) {
                    tvSyncStatus.setText("🟢 ACTIVE PIN (EXPIRES IN " + timeStr + ")");
                    tvSyncStatus.setTextColor(Color.parseColor("#00E676"));
                } else if (selectedTab == TAB_NEARBY_P2P && tvLocalSyncStatus != null) {
                    tvLocalSyncStatus.setText("🟢 ACTIVE TOKEN (EXPIRES IN " + timeStr + ")");
                    tvLocalSyncStatus.setTextColor(Color.parseColor("#00E676"));
                }
            }
        });

        viewModel.isTimerExpired().observe(this, expired -> {
            if (Boolean.TRUE.equals(expired)) {
                if (tvCode != null) tvCode.setText("EXPIRED");
                if (tvLocalCode != null) tvLocalCode.setText("EXPIRED");
                if (ivQrCode != null) ivQrCode.setVisibility(View.GONE);
                if (qrPlaceholderCloud != null) qrPlaceholderCloud.setVisibility(View.VISIBLE);
                if (ivLocalQrCode != null) ivLocalQrCode.setVisibility(View.GONE);
                if (qrPlaceholderLocal != null) qrPlaceholderLocal.setVisibility(View.VISIBLE);
                FirebasePairingManager.INSTANCE.cancelActiveSession();
            }
        });

        // Trigger initial background roster fetch (Zero-Latency Local Boot) & Prune Zombie Duplicates
        db.pruneZombieNodes(forensicAgentId, null);
        viewModel.refreshLocalRoster(db);
        com.jdroidx.jfindme.utils.MiuiOptimizationHelper.checkAndPromptMiuiOptimization(this);

        // Async Cloud Group Sync & Real-time Signal Listener
        String activeGrp = jPrefs.getString("active_group", null);
        viewModel.syncCloudGroupToLocalDb(this, activeGrp);
        viewModel.setupSignalListener(safeUid, activeGrp, () -> {
            runOnUiThread(() -> viewModel.syncCloudGroupToLocalDb(this, activeGrp));
            return kotlin.Unit.INSTANCE;
        });

        // Check for Recovery Launch Mode
        String mode = getIntent().getStringExtra("LAUNCH_MODE");
        if ("RECOVERY".equals(mode)) {
            getIntent().removeExtra("LAUNCH_MODE");
            showRestoreOldGroupDialog();
        }

        // 🚀 Deep Link Universal Pairing Handler (jfindme://pair?code=... or https://jfindme.app/pair?code=...)
        handleDeepLinkIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLinkIntent(intent);
    }

    private void handleDeepLinkIntent(Intent intent) {
        if (intent == null) return;
        String pairCode = null;
        if (intent.getData() != null) {
            android.net.Uri uri = intent.getData();
            pairCode = uri.getQueryParameter("code");
            if (pairCode == null || pairCode.isEmpty()) {
                String path = uri.getPath();
                if (path != null && path.contains("/pair/")) {
                    pairCode = path.substring(path.indexOf("/pair/") + 6);
                }
            }
        }
        if (pairCode == null || pairCode.isEmpty()) {
            pairCode = intent.getStringExtra("PAIRING_CODE");
        }
        if (pairCode != null && !pairCode.trim().isEmpty()) {
            final String cleanCode = pairCode.trim().toUpperCase(Locale.US);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (cleanCode.length() == 6) {
                    switchToTab(TAB_REMOTE_CLOUD);
                    if (etCode != null) etCode.setText(cleanCode);
                    showRoleSelectionDialogBeforeJoin(cleanCode);
                } else if (cleanCode.length() == 8) {
                    switchToTab(TAB_NEARBY_P2P);
                    if (etLocalCode != null) etLocalCode.setText(cleanCode);
                    showRoleSelectionDialogForLocalJoin(cleanCode);
                }
            }, 300);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Closed-Loop: Refresh identity strip and roster on return (e.g. from ProfileActivity)
        refreshOwnerIdentity();
        if (viewModel != null) {
            viewModel.refreshLocalRoster(DatabaseHelper.getInstance(this));
        }
    }

    private void refreshOwnerIdentity() {
        if (ownerAvatarView != null) {
            AvatarLoader.bindCurrentUserAvatar(this, ownerAvatarView);
        }
        if (ownerNameView != null) {
            String nameStr = RegistryManager.getUserNickname(this);
            ownerNameView.setText("👤 " + nameStr.toUpperCase());
        }
        if (ownerMetaView != null) {
            SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
            String deviceId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
            String activeGroup = jPrefs.getString("active_group", "SECURE_HUB");
            ownerMetaView.setText("📱 AGENT: " + deviceId + "  ●  🌐 Hub: " + activeGroup);
        }
    }

    // =========================================================================
    // I. HEADER & COMPACT IDENTITY COMPONENTS
    // =========================================================================

    private View buildHeaderBar() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyHeader(this, header, "HEADER_MAIN");
        int pad = getResponsivePaddingQuota();
        header.setPadding(pad, pad / 2, pad, pad / 2);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(this);
        title.setText("NETWORK GATEWAY");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        textBlock.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Device Pairing & Network Access");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.85f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        textBlock.addView(subtitle);

        header.addView(textBlock);

        // Compact Refresh Action Button
        ImageView btnSync = new ImageView(this);
        btnSync.setImageResource(android.R.drawable.ic_popup_sync);
        btnSync.setColorFilter(getGradientOverlayText());
        int iSize = UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
        LinearLayout.LayoutParams syncLp = new LinearLayout.LayoutParams(iSize, iSize);
        btnSync.setLayoutParams(syncLp);
        btnSync.setFocusable(true);
        btnSync.setClickable(true);
        btnSync.setOnClickListener(v -> {
            triggerHapticFeedback();
            initiateCloudSync(forensicAgentId);
        });
        header.addView(btnSync);

        return header;
    }

    private View buildCompactIdentityStrip() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, card, "CARD_STANDARD");
        int pad = scale(12);
        card.setPadding(pad, pad, pad, pad);

        // Owner Avatar (Unified Brand Size & Smart Ring Fit)
        ownerAvatarView = new ImageView(this);
        AvatarLoader.bindCurrentUserAvatar(this, ownerAvatarView);
        ownerAvatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.drawable.GradientDrawable gRing = new android.graphics.drawable.GradientDrawable();
        gRing.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gRing.setColor(Color.parseColor("#1A000000"));
        gRing.setStroke(scale(3), getGradientStart());
        ownerAvatarView.setBackground(gRing);
        int avSize = UiArchitect.getUniversalAvatarSize(this);
        card.addView(ownerAvatarView, new LinearLayout.LayoutParams(avSize, avSize));

        // Details Block
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(scale(14), 0, 0, 0);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String nameStr = RegistryManager.getUserNickname(this);
        String deviceId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String activeGroup = jPrefs.getString("active_group", "SECURE_HUB");

        ownerNameView = new TextView(this);
        ownerNameView.setText("👤 " + nameStr.toUpperCase());
        ownerNameView.setTextColor(getTextColor());
        ownerNameView.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, ownerNameView, "TEXT_TITLE");
        details.addView(ownerNameView);

        ownerMetaView = new TextView(this);
        ownerMetaView.setText("📱 AGENT: " + deviceId + "  ●  🌐 Hub: " + activeGroup);
        ownerMetaView.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, ownerMetaView, "TEXT_MINI");
        details.addView(ownerMetaView);

        card.addView(details);

        // Quick profile link icon
        ImageView editIcon = new ImageView(this);
        editIcon.setImageResource(android.R.drawable.ic_menu_manage);
        editIcon.setColorFilter(getGradientStart());
        int iconSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        card.addView(editIcon, new LinearLayout.LayoutParams(iconSize, iconSize));

        // Tap on identity to open User Profile
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Intent pIntent = new Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class);
            startActivity(pIntent);
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(12));
        card.setLayoutParams(lp);

        return card;
    }

    // =========================================================================
    // II. SEGMENTED MODALITY CONTROLLER (ROSTER FIRST)
    // =========================================================================

    private View buildSegmentedTabSwitcher() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");
        container.setPadding(scale(4), scale(4), scale(4), scale(4));

        // Tab 1: Active Roster (FIRST ITEM)
        tabRosterBtn = buildSegmentButton("📋 ROSTER", true);
        tvTabRoster = (TextView) tabRosterBtn.getChildAt(0);
        tabRosterBtn.setOnClickListener(v -> switchToTab(TAB_ACTIVE_ROSTER));

        // Tab 2: Remote Cloud
        tabRemoteBtn = buildSegmentButton("☁️ REMOTE", false);
        tvTabRemote = (TextView) tabRemoteBtn.getChildAt(0);
        tabRemoteBtn.setOnClickListener(v -> switchToTab(TAB_REMOTE_CLOUD));

        // Tab 3: Nearby P2P
        tabNearbyBtn = buildSegmentButton("🛰️ NEARBY", false);
        tvTabNearby = (TextView) tabNearbyBtn.getChildAt(0);
        tabNearbyBtn.setOnClickListener(v -> switchToTab(TAB_NEARBY_P2P));

        container.addView(tabRosterBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        container.addView(tabRemoteBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        container.addView(tabNearbyBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(12));
        container.setLayoutParams(lp);

        return container;
    }

    private LinearLayout buildSegmentButton(String label, boolean isActive) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        int padH = scale(12);
        int padV = scale(10);
        btn.setPadding(padH, padV, padH, padV);
        btn.setFocusable(true);
        btn.setClickable(true);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(scaleText(12f));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        btn.addView(tv);

        applySegmentStyle(btn, tv, isActive);
        return btn;
    }

    private void applySegmentStyle(LinearLayout btn, TextView tv, boolean isActive) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(16f);
        if (isActive) {
            gd.setColors(new int[]{getGradientStart(), getGradientEnd()});
            gd.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            tv.setTextColor(getGradientOverlayText());
        } else {
            gd.setColor(Color.TRANSPARENT);
            tv.setTextColor(getSecondaryTextColor());
        }
        btn.setBackground(gd);
    }

    private void switchToTab(int tabIndex) {
        if (selectedTab == tabIndex) return;
        selectedTab = tabIndex;
        triggerHapticFeedback();
        updateTabVisibility();
    }

    private void updateTabVisibility() {
        if (masterScrollRoot != null) {
            TransitionManager.beginDelayedTransition(masterScrollRoot, new AutoTransition().setDuration(200));
        }

        applySegmentStyle(tabRosterBtn, tvTabRoster, selectedTab == TAB_ACTIVE_ROSTER);
        applySegmentStyle(tabRemoteBtn, tvTabRemote, selectedTab == TAB_REMOTE_CLOUD);
        applySegmentStyle(tabNearbyBtn, tvTabNearby, selectedTab == TAB_NEARBY_P2P);

        if (panelRoster != null) panelRoster.setVisibility(selectedTab == TAB_ACTIVE_ROSTER ? View.VISIBLE : View.GONE);
        if (panelRemote != null) panelRemote.setVisibility(selectedTab == TAB_REMOTE_CLOUD ? View.VISIBLE : View.GONE);
        if (panelNearby != null) panelNearby.setVisibility(selectedTab == TAB_NEARBY_P2P ? View.VISIBLE : View.GONE);
    }

    // =========================================================================
    // III. OPTION A: REMOTE CLOUD PANEL (SYMMETRICAL 2x2 GRID LAYOUT)
    // =========================================================================

    private LinearLayout buildRemoteOptionPanel() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // Host Section Card (2x2 Grid Architecture)
        LinearLayout hostCard = new LinearLayout(this);
        hostCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, hostCard, "CARD_DASHBOARD");
        int cardPad = scale(14);
        hostCard.setPadding(cardPad, cardPad, cardPad, cardPad);

        TextView hostTitle = new TextView(this);
        hostTitle.setText("HOST CLOUD SESSION");
        hostTitle.setTextColor(getGradientStart());
        hostTitle.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, hostTitle, "TEXT_LABEL");
        hostCard.addView(hostTitle);

        tvSyncStatus = new TextView(this);
        tvSyncStatus.setText("READY FOR PAIRING");
        tvSyncStatus.setTextColor(Color.parseColor("#00E676"));
        tvSyncStatus.setTextSize(scaleText(10.5f));
        tvSyncStatus.setPadding(0, scale(2), 0, scale(10));
        hostCard.addView(tvSyncStatus);

        // 2x2 Grid Container
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        int squareSize = getSmartSquareSize();

        // --- ROW 1: [ R1C1: Square Button ]  [ R1C2: Square QR Box ] ---
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);

        // R1C1: Square Button (Generate Cloud PIN)
        LinearLayout btnGenSquare = new LinearLayout(this);
        btnGenSquare.setOrientation(LinearLayout.VERTICAL);
        btnGenSquare.setGravity(Gravity.CENTER);
        btnGenSquare.setFocusable(true);
        btnGenSquare.setClickable(true);
        GradientDrawable genBg = new GradientDrawable();
        genBg.setCornerRadius(20f);
        genBg.setColors(new int[]{getGradientStart(), getGradientEnd()});
        genBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        btnGenSquare.setBackground(genBg);
        btnGenSquare.setPadding(scale(12), scale(12), scale(12), scale(12));

        ImageView ivGenIcon = new ImageView(this);
        ivGenIcon.setImageResource(android.R.drawable.ic_popup_sync);
        ivGenIcon.setColorFilter(getGradientOverlayText());
        int genIconSize = scale(44);
        btnGenSquare.addView(ivGenIcon, new LinearLayout.LayoutParams(genIconSize, genIconSize));

        TextView tvGenLabel = new TextView(this);
        tvGenLabel.setText("GENERATE\nCLOUD PIN");
        tvGenLabel.setTextColor(getGradientOverlayText());
        tvGenLabel.setTextSize(scaleText(13.5f));
        tvGenLabel.setTypeface(null, Typeface.BOLD);
        tvGenLabel.setGravity(Gravity.CENTER);
        tvGenLabel.setPadding(0, scale(8), 0, 0);
        btnGenSquare.addView(tvGenLabel);

        pbCodeLoading = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        pbCodeLoading.setVisibility(View.GONE);
        btnGenSquare.addView(pbCodeLoading);

        btnGenSquare.setOnClickListener(v -> executeHostSessionSpinup());

        LinearLayout.LayoutParams r1c1Lp = new LinearLayout.LayoutParams(0, squareSize, 1.0f);
        r1c1Lp.setMargins(0, 0, scale(8), 0);
        row1.addView(btnGenSquare, r1c1Lp);

        // R1C2: Square QR Code Box (Exact same square size)
        FrameLayout qrBox = new FrameLayout(this);
        applySolidCardGeometry(qrBox, Color.WHITE, 20f);
        qrBox.setPadding(scale(10), scale(10), scale(10), scale(10));

        LinearLayout qrPlaceholder = new LinearLayout(this);
        qrPlaceholder.setOrientation(LinearLayout.VERTICAL);
        qrPlaceholder.setGravity(Gravity.CENTER);
        ImageView ivQrPlaceIcon = new ImageView(this);
        ivQrPlaceIcon.setImageResource(android.R.drawable.ic_menu_camera);
        ivQrPlaceIcon.setColorFilter(Color.DKGRAY);
        int qrIconSize = scale(44);
        qrPlaceholder.addView(ivQrPlaceIcon, new LinearLayout.LayoutParams(qrIconSize, qrIconSize));
        TextView tvQrPlaceText = new TextView(this);
        tvQrPlaceText.setText("QR MATRIX\nPREVIEW");
        tvQrPlaceText.setTextColor(Color.DKGRAY);
        tvQrPlaceText.setTextSize(scaleText(11.5f));
        tvQrPlaceText.setTypeface(null, Typeface.BOLD);
        tvQrPlaceText.setGravity(Gravity.CENTER);
        tvQrPlaceText.setPadding(0, scale(6), 0, 0);
        qrPlaceholder.addView(tvQrPlaceText);
        qrBox.addView(qrPlaceholder, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.qrPlaceholderCloud = qrPlaceholder;

        ivQrCode = new ImageView(this);
        ivQrCode.setVisibility(View.GONE);
        ivQrCode.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrBox.addView(ivQrCode, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout.LayoutParams r1c2Lp = new LinearLayout.LayoutParams(0, squareSize, 1.0f);
        r1c2Lp.setMargins(scale(8), 0, 0, 0);
        row1.addView(qrBox, r1c2Lp);

        grid.addView(row1);

        // --- ROW 2: [ R2C1: Copy & Share Buttons ]  [ R2C2: Code Display Text ] ---
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        row2.setPadding(0, scale(10), 0, 0);

        // R2C1: Copy & Share Cluster
        LinearLayout copyShareRow = new LinearLayout(this);
        copyShareRow.setOrientation(LinearLayout.HORIZONTAL);
        copyShareRow.setGravity(Gravity.CENTER);

        Button btnCopy = new Button(this);
        btnCopy.setText("📋 COPY");
        btnCopy.setTextColor(Color.WHITE);
        btnCopy.setTextSize(scaleText(11f));
        btnCopy.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyCardWithOverride(this, btnCopy, "CARD_CAPSULE_BUTTON", 0.9f);
        btnCopy.setOnClickListener(v -> copyToClipboard(tvCode.getText().toString(), "Pairing PIN"));
        copyShareRow.addView(btnCopy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnShare = new Button(this);
        btnShare.setText("📤 SHARE");
        btnShare.setTextColor(Color.WHITE);
        btnShare.setTextSize(scaleText(11f));
        btnShare.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        shareLp.setMargins(scale(6), 0, 0, 0);
        btnShare.setLayoutParams(shareLp);
        UiArchitect.applyCardWithOverride(this, btnShare, "CARD_CAPSULE_BUTTON", 0.9f);
        btnShare.setOnClickListener(v -> sharePairingCode(tvCode.getText().toString()));
        copyShareRow.addView(btnShare);

        LinearLayout.LayoutParams r2c1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        r2c1Lp.setMargins(0, 0, scale(8), 0);
        row2.addView(copyShareRow, r2c1Lp);

        // R2C2: Pin Code Box
        LinearLayout codeBox = new LinearLayout(this);
        codeBox.setOrientation(LinearLayout.VERTICAL);
        codeBox.setGravity(Gravity.CENTER);
        applySolidCardGeometry(codeBox, getCardColor(), 16f);
        codeBox.setPadding(scale(8), scale(6), scale(8), scale(6));

        tvCode = new TextView(this);
        tvCode.setText("------");
        tvCode.setTextColor(getTextColor());
        tvCode.setTextSize(scaleText(20));
        tvCode.setLetterSpacing(0.12f);
        tvCode.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvCode.setGravity(Gravity.CENTER);
        codeBox.addView(tvCode);

        LinearLayout.LayoutParams r2c2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        r2c2Lp.setMargins(scale(8), 0, 0, 0);
        row2.addView(codeBox, r2c2Lp);

        grid.addView(row2);
        hostCard.addView(grid);

        TextView tvTtlChip = new TextView(this);
        int curTtl = getSharedPreferences("system_registry", MODE_PRIVATE).getInt("pairing_pin_ttl_minutes", 5);
        tvTtlChip.setText("⏱️ PIN Expiry TTL: " + curTtl + " mins (Tap to adjust)");
        tvTtlChip.setTextColor(getSecondaryTextColor());
        tvTtlChip.setTextSize(scaleText(10f));
        tvTtlChip.setGravity(Gravity.CENTER);
        tvTtlChip.setPadding(0, scale(8), 0, 0);
        tvTtlChip.setFocusable(true);
        tvTtlChip.setClickable(true);
        tvTtlChip.setOnClickListener(v -> {
            showPinTtlSelectionDialog();
            int updatedTtl = getSharedPreferences("system_registry", MODE_PRIVATE).getInt("pairing_pin_ttl_minutes", 5);
            tvTtlChip.setText("⏱️ PIN Expiry TTL: " + updatedTtl + " mins (Tap to adjust)");
        });
        hostCard.addView(tvTtlChip);

        container.addView(hostCard);

        // Join Section Card
        LinearLayout joinCard = new LinearLayout(this);
        joinCard.setOrientation(LinearLayout.VERTICAL);
        joinCard.setGravity(Gravity.CENTER_HORIZONTAL);
        UiArchitect.applyCard(this, joinCard, "CARD_STANDARD");
        joinCard.setPadding(scale(16), scale(14), scale(16), scale(14));

        LinearLayout.LayoutParams joinCardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        joinCardLp.setMargins(0, scale(12), 0, 0);
        joinCard.setLayoutParams(joinCardLp);

        TextView joinTitle = new TextView(this);
        joinTitle.setText("JOIN CLOUD SESSION");
        joinTitle.setTextColor(getGradientEnd());
        joinTitle.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, joinTitle, "TEXT_LABEL");
        joinCard.addView(joinTitle);

        etCode = UiArchitect.createInputField(this, "FIELD_CODE");
        etCode.setHint("ENTER 6-DIGIT PIN");
        etCode.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(6)});
        joinCard.addView(etCode);

        LinearLayout joinActions = new LinearLayout(this);
        joinActions.setOrientation(LinearLayout.HORIZONTAL);
        joinActions.setGravity(Gravity.CENTER);
        joinActions.setPadding(0, scale(8), 0, 0);

        Button btnLink = new Button(this);
        btnLink.setText("🔗 CONNECT");
        btnLink.setTextColor(Color.WHITE);
        btnLink.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyCardWithOverride(this, btnLink, "CARD_CAPSULE_BUTTON", 0.9f);
        btnLink.setOnClickListener(v -> validateAndShowRoleDialog(etCode.getText().toString(), 6, false));
        joinActions.addView(btnLink, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnScan = new Button(this);
        btnScan.setText("📷 SCAN QR");
        btnScan.setTextColor(Color.WHITE);
        btnScan.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        scanLp.setMargins(scale(10), 0, 0, 0);
        btnScan.setLayoutParams(scanLp);
        UiArchitect.applyCardWithOverride(this, btnScan, "CARD_CAPSULE_BUTTON", 0.9f);
        btnScan.setOnClickListener(v -> launchBarcodeScanner());
        joinActions.addView(btnScan);

        joinCard.addView(joinActions);
        container.addView(joinCard);

        return container;
    }

    // =========================================================================
    // IV. OPTION B: NEARBY P2P PANEL (SYMMETRICAL 2x2 GRID LAYOUT)
    // =========================================================================

    private LinearLayout buildNearbyOptionPanel() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // Host Nearby Card (2x2 Grid Architecture)
        LinearLayout hostCard = new LinearLayout(this);
        hostCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, hostCard, "CARD_DASHBOARD");
        int cardPad = scale(14);
        hostCard.setPadding(cardPad, cardPad, cardPad, cardPad);

        TextView hostTitle = new TextView(this);
        hostTitle.setText("HOST LOCAL P2P SESSION");
        hostTitle.setTextColor(getGradientStart());
        hostTitle.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, hostTitle, "TEXT_LABEL");
        hostCard.addView(hostTitle);

        tvLocalSyncStatus = new TextView(this);
        tvLocalSyncStatus.setText("READY FOR LOCAL P2P");
        tvLocalSyncStatus.setTextColor(Color.parseColor("#00E676"));
        tvLocalSyncStatus.setTextSize(scaleText(10.5f));
        tvLocalSyncStatus.setPadding(0, scale(2), 0, scale(10));
        hostCard.addView(tvLocalSyncStatus);

        // 2x2 Grid Container
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        int squareSize = getSmartSquareSize();

        // --- ROW 1: [ R1C1: Square Button ]  [ R1C2: Square QR Box ] ---
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);

        // R1C1: Square Button (Broadcast Local Token)
        LinearLayout btnGenLocalSquare = new LinearLayout(this);
        btnGenLocalSquare.setOrientation(LinearLayout.VERTICAL);
        btnGenLocalSquare.setGravity(Gravity.CENTER);
        btnGenLocalSquare.setFocusable(true);
        btnGenLocalSquare.setClickable(true);
        GradientDrawable genBg = new GradientDrawable();
        genBg.setCornerRadius(20f);
        genBg.setColors(new int[]{getGradientStart(), getGradientEnd()});
        genBg.setOrientation(GradientDrawable.Orientation.TL_BR);
        btnGenLocalSquare.setBackground(genBg);
        btnGenLocalSquare.setPadding(scale(12), scale(12), scale(12), scale(12));

        ImageView ivGenLocalIcon = new ImageView(this);
        ivGenLocalIcon.setImageResource(android.R.drawable.ic_menu_mylocation);
        ivGenLocalIcon.setColorFilter(getGradientOverlayText());
        int genIconSize = scale(44);
        btnGenLocalSquare.addView(ivGenLocalIcon, new LinearLayout.LayoutParams(genIconSize, genIconSize));

        TextView tvGenLocalLabel = new TextView(this);
        tvGenLocalLabel.setText("BROADCAST\nLOCAL TOKEN");
        tvGenLocalLabel.setTextColor(getGradientOverlayText());
        tvGenLocalLabel.setTextSize(scaleText(13.5f));
        tvGenLocalLabel.setTypeface(null, Typeface.BOLD);
        tvGenLocalLabel.setGravity(Gravity.CENTER);
        tvGenLocalLabel.setPadding(0, scale(8), 0, 0);
        btnGenLocalSquare.addView(tvGenLocalLabel);

        pbLocalLoading = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        pbLocalLoading.setVisibility(View.GONE);
        btnGenLocalSquare.addView(pbLocalLoading);

        btnGenLocalSquare.setOnClickListener(v -> executeLocalHostSessionSpinup());

        LinearLayout.LayoutParams r1c1Lp = new LinearLayout.LayoutParams(0, squareSize, 1.0f);
        r1c1Lp.setMargins(0, 0, scale(8), 0);
        row1.addView(btnGenLocalSquare, r1c1Lp);

        // R1C2: Square Local QR Box (Exact same square size)
        FrameLayout qrBox = new FrameLayout(this);
        applySolidCardGeometry(qrBox, Color.WHITE, 20f);
        qrBox.setPadding(scale(10), scale(10), scale(10), scale(10));

        LinearLayout qrPlaceholder = new LinearLayout(this);
        qrPlaceholder.setOrientation(LinearLayout.VERTICAL);
        qrPlaceholder.setGravity(Gravity.CENTER);
        ImageView ivQrPlaceIcon = new ImageView(this);
        ivQrPlaceIcon.setImageResource(android.R.drawable.ic_menu_camera);
        ivQrPlaceIcon.setColorFilter(Color.DKGRAY);
        int qrIconSize = scale(44);
        qrPlaceholder.addView(ivQrPlaceIcon, new LinearLayout.LayoutParams(qrIconSize, qrIconSize));
        TextView tvQrPlaceText = new TextView(this);
        tvQrPlaceText.setText("P2P MATRIX\nPREVIEW");
        tvQrPlaceText.setTextColor(Color.DKGRAY);
        tvQrPlaceText.setTextSize(scaleText(11.5f));
        tvQrPlaceText.setTypeface(null, Typeface.BOLD);
        tvQrPlaceText.setGravity(Gravity.CENTER);
        tvQrPlaceText.setPadding(0, scale(6), 0, 0);
        qrPlaceholder.addView(tvQrPlaceText);
        qrBox.addView(qrPlaceholder, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.qrPlaceholderLocal = qrPlaceholder;

        ivLocalQrCode = new ImageView(this);
        ivLocalQrCode.setVisibility(View.GONE);
        ivLocalQrCode.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrBox.addView(ivLocalQrCode, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout.LayoutParams r1c2Lp = new LinearLayout.LayoutParams(0, squareSize, 1.0f);
        r1c2Lp.setMargins(scale(8), 0, 0, 0);
        row1.addView(qrBox, r1c2Lp);

        grid.addView(row1);

        // --- ROW 2: [ R2C1: Copy & Share Buttons ]  [ R2C2: Local Code Display Text ] ---
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        row2.setPadding(0, scale(10), 0, 0);

        // R2C1: Copy & Share Cluster
        LinearLayout copyShareRow = new LinearLayout(this);
        copyShareRow.setOrientation(LinearLayout.HORIZONTAL);
        copyShareRow.setGravity(Gravity.CENTER);

        Button btnLocalCopy = new Button(this);
        btnLocalCopy.setText("📋 COPY");
        btnLocalCopy.setTextColor(Color.WHITE);
        btnLocalCopy.setTextSize(scaleText(11f));
        btnLocalCopy.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyCardWithOverride(this, btnLocalCopy, "CARD_CAPSULE_BUTTON", 0.9f);
        btnLocalCopy.setOnClickListener(v -> copyToClipboard(tvLocalCode.getText().toString(), "Local Token"));
        copyShareRow.addView(btnLocalCopy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnLocalShare = new Button(this);
        btnLocalShare.setText("📤 SHARE");
        btnLocalShare.setTextColor(Color.WHITE);
        btnLocalShare.setTextSize(scaleText(11f));
        btnLocalShare.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        shareLp.setMargins(scale(6), 0, 0, 0);
        btnLocalShare.setLayoutParams(shareLp);
        UiArchitect.applyCardWithOverride(this, btnLocalShare, "CARD_CAPSULE_BUTTON", 0.9f);
        btnLocalShare.setOnClickListener(v -> sharePairingCode(tvLocalCode.getText().toString()));
        copyShareRow.addView(btnLocalShare);

        LinearLayout.LayoutParams r2c1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        r2c1Lp.setMargins(0, 0, scale(8), 0);
        row2.addView(copyShareRow, r2c1Lp);

        // R2C2: Local Code Box
        LinearLayout codeBox = new LinearLayout(this);
        codeBox.setOrientation(LinearLayout.VERTICAL);
        codeBox.setGravity(Gravity.CENTER);
        applySolidCardGeometry(codeBox, getCardColor(), 16f);
        codeBox.setPadding(scale(8), scale(6), scale(8), scale(6));

        tvLocalCode = new TextView(this);
        tvLocalCode.setText("--------");
        tvLocalCode.setTextColor(getTextColor());
        tvLocalCode.setTextSize(scaleText(20));
        tvLocalCode.setLetterSpacing(0.12f);
        tvLocalCode.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvLocalCode.setGravity(Gravity.CENTER);
        codeBox.addView(tvLocalCode);

        LinearLayout.LayoutParams r2c2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        r2c2Lp.setMargins(scale(8), 0, 0, 0);
        row2.addView(codeBox, r2c2Lp);

        grid.addView(row2);
        hostCard.addView(grid);

        TextView tvLocalTtlChip = new TextView(this);
        int curLocalTtl = getSharedPreferences("system_registry", MODE_PRIVATE).getInt("pairing_pin_ttl_minutes", 5);
        tvLocalTtlChip.setText("⏱️ PIN Expiry TTL: " + curLocalTtl + " mins (Tap to adjust)");
        tvLocalTtlChip.setTextColor(getSecondaryTextColor());
        tvLocalTtlChip.setTextSize(scaleText(10f));
        tvLocalTtlChip.setGravity(Gravity.CENTER);
        tvLocalTtlChip.setPadding(0, scale(8), 0, 0);
        tvLocalTtlChip.setFocusable(true);
        tvLocalTtlChip.setClickable(true);
        tvLocalTtlChip.setOnClickListener(v -> {
            showPinTtlSelectionDialog();
            int updatedTtl = getSharedPreferences("system_registry", MODE_PRIVATE).getInt("pairing_pin_ttl_minutes", 5);
            tvLocalTtlChip.setText("⏱️ PIN Expiry TTL: " + updatedTtl + " mins (Tap to adjust)");
        });
        hostCard.addView(tvLocalTtlChip);

        container.addView(hostCard);

        // Join Local Section Card
        LinearLayout joinCard = new LinearLayout(this);
        joinCard.setOrientation(LinearLayout.VERTICAL);
        joinCard.setGravity(Gravity.CENTER_HORIZONTAL);
        UiArchitect.applyCard(this, joinCard, "CARD_STANDARD");
        joinCard.setPadding(scale(16), scale(14), scale(16), scale(14));

        LinearLayout.LayoutParams joinCardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        joinCardLp.setMargins(0, scale(12), 0, 0);
        joinCard.setLayoutParams(joinCardLp);

        TextView joinTitle = new TextView(this);
        joinTitle.setText("JOIN LOCAL P2P SESSION");
        joinTitle.setTextColor(getGradientEnd());
        joinTitle.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, joinTitle, "TEXT_LABEL");
        joinCard.addView(joinTitle);

        etLocalCode = UiArchitect.createInputField(this, "FIELD_CODE");
        etLocalCode.setHint("ENTER 8-DIGIT LOCAL TOKEN");
        etLocalCode.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(8)});
        joinCard.addView(etLocalCode);

        LinearLayout joinActions = new LinearLayout(this);
        joinActions.setOrientation(LinearLayout.HORIZONTAL);
        joinActions.setGravity(Gravity.CENTER);
        joinActions.setPadding(0, scale(8), 0, 0);

        Button btnLink = new Button(this);
        btnLink.setText("🔗 CONNECT");
        btnLink.setTextColor(Color.WHITE);
        btnLink.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyCardWithOverride(this, btnLink, "CARD_CAPSULE_BUTTON", 0.9f);
        btnLink.setOnClickListener(v -> validateAndShowRoleDialog(etLocalCode.getText().toString(), 8, true));
        joinActions.addView(btnLink, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnScan = new Button(this);
        btnScan.setText("📷 SCAN QR");
        btnScan.setTextColor(Color.WHITE);
        btnScan.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        scanLp.setMargins(scale(10), 0, 0, 0);
        btnScan.setLayoutParams(scanLp);
        UiArchitect.applyCardWithOverride(this, btnScan, "CARD_CAPSULE_BUTTON", 0.9f);
        btnScan.setOnClickListener(v -> launchBarcodeScanner());
        joinActions.addView(btnScan);

        joinCard.addView(joinActions);
        container.addView(joinCard);

        return container;
    }

    // Roster Sub-Tab Constants
    private static final int ROSTER_SUBTAB_FAMILY = 0;
    private static final int ROSTER_SUBTAB_FRIENDS = 1;
    private int selectedRosterSubTab = ROSTER_SUBTAB_FAMILY;
    private LinearLayout subTabFamilyBtn, subTabFriendsBtn;
    private TextView tvSubTabFamily, tvSubTabFriends;
    private String rosterSearchFilter = "";
    private List<Map<String, Object>> currentCachedRoster = new ArrayList<>();

    // =========================================================================
    // V. ACTIVE ROSTER PANEL & MEMBER CARDS (FAMILY vs FRIENDS SUB-TABS)
    // =========================================================================

    private LinearLayout buildActiveRosterPanel() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // 1. Roster Sub-Tab Controller: [ 👨‍👩‍👧 FAMILY ]  [ 🤝 FRIENDS ]
        LinearLayout subTabController = new LinearLayout(this);
        subTabController.setOrientation(LinearLayout.HORIZONTAL);
        subTabController.setGravity(Gravity.CENTER);
        UiArchitect.applyCard(this, subTabController, "CARD_STANDARD");
        subTabController.setPadding(scale(4), scale(4), scale(4), scale(4));

        subTabFamilyBtn = buildRosterSubTabButton("👨‍👩‍👧 FAMILY", selectedRosterSubTab == ROSTER_SUBTAB_FAMILY);
        tvSubTabFamily = (TextView) subTabFamilyBtn.getChildAt(0);
        subTabFamilyBtn.setOnClickListener(v -> switchRosterSubTab(ROSTER_SUBTAB_FAMILY));

        subTabFriendsBtn = buildRosterSubTabButton("🤝 FRIENDS", selectedRosterSubTab == ROSTER_SUBTAB_FRIENDS);
        tvSubTabFriends = (TextView) subTabFriendsBtn.getChildAt(0);
        subTabFriendsBtn.setOnClickListener(v -> switchRosterSubTab(ROSTER_SUBTAB_FRIENDS));

        subTabController.addView(subTabFamilyBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        subTabController.addView(subTabFriendsBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.setMargins(0, 0, 0, scale(10));
        subTabController.setLayoutParams(stLp);
        container.addView(subTabController);

        // 2. Real-Time Search / Filter Input
        container.addView(buildRosterSearchBar());

        // 3. Dynamic Roster List Container
        rosterContainer = new LinearLayout(this);
        rosterContainer.setOrientation(LinearLayout.VERTICAL);
        container.addView(rosterContainer);

        // 4. Reclaim Legacy Cloud Assets Button
        Button btnRestore = new Button(this);
        btnRestore.setText("🔄 RECLAIM / RESTORE LEGACY GROUP");
        btnRestore.setTextColor(Color.WHITE);
        btnRestore.setTextSize(scaleText(11));
        UiArchitect.applyCardWithOverride(this, btnRestore, "CARD_CAPSULE_BUTTON", 0.9f);
        btnRestore.setOnClickListener(v -> showRestoreOldGroupDialog());

        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.setMargins(0, scale(12), 0, 0);
        btnRestore.setLayoutParams(rLp);
        container.addView(btnRestore);

        return container;
    }

    private LinearLayout buildRosterSubTabButton(String label, boolean isActive) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        int padH = scale(12);
        int padV = scale(8);
        btn.setPadding(padH, padV, padH, padV);
        btn.setFocusable(true);
        btn.setClickable(true);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(scaleText(12f));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        btn.addView(tv);

        applySegmentStyle(btn, tv, isActive);
        return btn;
    }

    private void switchRosterSubTab(int subTab) {
        if (selectedRosterSubTab == subTab) return;
        selectedRosterSubTab = subTab;
        triggerHapticFeedback();
        applySegmentStyle(subTabFamilyBtn, tvSubTabFamily, selectedRosterSubTab == ROSTER_SUBTAB_FAMILY);
        applySegmentStyle(subTabFriendsBtn, tvSubTabFriends, selectedRosterSubTab == ROSTER_SUBTAB_FRIENDS);
        rebuildRosterView(currentCachedRoster);
    }

    private View buildRosterSearchBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(scale(12), scale(6), scale(12), scale(6));
        UiArchitect.applyCard(this, bar, "CARD_STANDARD");

        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_search);
        icon.setColorFilter(getGradientStart());
        int iSize = scale(28);
        bar.addView(icon, new LinearLayout.LayoutParams(iSize, iSize));

        EditText etSearch = new EditText(this);
        etSearch.setHint("Search by name, role, or device ID...");
        etSearch.setHintTextColor(Color.GRAY);
        etSearch.setTextColor(getTextColor());
        etSearch.setTextSize(scaleText(11.5f));
        etSearch.setBackground(null);
        etSearch.setPadding(scale(10), scale(4), scale(10), scale(4));
        etSearch.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                rosterSearchFilter = (s != null) ? s.toString().trim() : "";
                rebuildRosterView(currentCachedRoster);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        bar.addView(etSearch);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(10));
        bar.setLayoutParams(lp);

        return bar;
    }

    private View buildRootGuardianAnchor() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, card, "CARD_STANDARD");
        int pad = scale(12);
        card.setPadding(pad, pad, pad, pad);

        // Owner Avatar
        ImageView avatar = new ImageView(this);
        AvatarLoader.bindCurrentUserAvatar(this, avatar);
        int avSize = scale(52);
        card.addView(avatar, new LinearLayout.LayoutParams(avSize, avSize));

        // Details
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(scale(12), 0, 0, 0);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        String myName = RegistryManager.getUserNickname(this);
        TextView tvName = new TextView(this);
        tvName.setText("👑 YOU (MASTER GUARDIAN)");
        tvName.setTextColor(getTextColor());
        tvName.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvName, "TEXT_TITLE");
        details.addView(tvName);

        TextView tvSub = new TextView(this);
        tvSub.setText("🟢 ONLINE NOW ● " + myName.toUpperCase());
        tvSub.setTextColor(Color.parseColor("#00E676"));
        UiArchitect.applyText(this, tvSub, "TEXT_MINI");
        details.addView(tvSub);

        card.addView(details);

        // Action: Manage profile
        ImageView pBtn = new ImageView(this);
        pBtn.setImageResource(android.R.drawable.ic_menu_manage);
        pBtn.setColorFilter(getGradientStart());
        int actSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        pBtn.setLayoutParams(new LinearLayout.LayoutParams(actSize, actSize));
        pBtn.setFocusable(true);
        pBtn.setClickable(true);
        pBtn.setOnClickListener(v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class)));
        card.addView(pBtn);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(10));
        card.setLayoutParams(lp);

        return card;
    }

    private boolean isSelfNode(String uid, Map<String, Object> node) {
        if (uid == null) return false;
        if (uid.equals(safeUid) || uid.equals(forensicAgentId)) return true;

        String authUid = FirebaseAuth.getInstance().getUid();
        if (authUid != null && uid.equals(authUid)) return true;

        SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String agentId = jPrefs.getString("agent_id", null);
        if (agentId != null && uid.equals(agentId)) return true;

        String deviceUid = jPrefs.getString("device_uid", null);
        if (deviceUid != null && uid.equals(deviceUid)) return true;

        String myHwId = FirebasePairingManager.INSTANCE.getDeviceId(this);
        if (myHwId != null && uid.equals(myHwId)) return true;

        String uid2 = (String) node.get("uid_2");
        if (uid2 != null && (uid2.equals(deviceUid) || uid2.equals(myHwId))) return true;

        return false;
    }

    private boolean matchesSearchFilter(String name, String role, String uid) {
        if (rosterSearchFilter == null || rosterSearchFilter.isEmpty()) return true;
        String q = rosterSearchFilter.toLowerCase();
        return (name != null && name.toLowerCase().contains(q))
                || (role != null && role.toLowerCase().contains(q))
                || (uid != null && uid.toLowerCase().contains(q));
    }

    private int getSmartSquareSize() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int padTotal = (getResponsivePaddingQuota() * 2) + scale(32);
        int available = (screenWidth - padTotal) / 2;
        return Math.max(scale(135), Math.min(scale(200), available));
    }

    private void rebuildRosterView(List<Map<String, Object>> liveData) {
        currentCachedRoster = (liveData != null) ? liveData : new ArrayList<>();
        if (rosterContainer == null) return;
        rosterContainer.removeAllViews();

        List<Map<String, Object>> guardians = new ArrayList<>();
        List<Map<String, Object>> spouses = new ArrayList<>();
        List<Map<String, Object>> children = new ArrayList<>();
        List<Map<String, Object>> friends = new ArrayList<>();

        for (Map<String, Object> node : currentCachedRoster) {
            String uid = (String) node.get("uid_1");
            if (uid == null) uid = (String) node.get("uid");
            if (uid == null) uid = (String) node.get("identity_id");
            if (uid == null) uid = "UNKNOWN_UID";

            if (isSelfNode(uid, node)) {
                continue; // Skip self in member list (rendered via Root Guardian Anchor)
            }

            String name = (String) node.get("fullName");
            if (name == null) name = (String) node.get("display_name");
            String role = (String) node.get("role_type");
            if (role == null) role = (String) node.get("role_title");
            if (role == null) role = "Member";

            if (!matchesSearchFilter(name, role, uid)) continue;

            Object fr = node.get("is_friend");
            boolean isF = false;
            if (fr instanceof Boolean) isF = (Boolean) fr;
            else if (fr instanceof Number) isF = ((Number) fr).intValue() == 1;
            else if (fr instanceof String) isF = "1".equals(fr) || "true".equalsIgnoreCase((String) fr);

            Object adm = node.get("is_admin");
            boolean isAdm = false;
            if (adm instanceof Boolean) isAdm = (Boolean) adm;
            else if (adm instanceof Number) isAdm = ((Number) adm).intValue() == 1;
            else if (adm instanceof String) isAdm = "1".equals(adm) || "true".equalsIgnoreCase((String) adm);

            String upper = role.toUpperCase();

            if (isF || upper.contains("FRIEND") || upper.contains("COLLEAGUE") || upper.contains("PEER") || upper.contains("ACQUAINTANCE") || upper.contains("NEIGHBOR")) {
                friends.add(node);
            } else if (upper.contains("SPOUSE") || upper.contains("WIFE") || upper.contains("HUSBAND") || upper.contains("PARTNER")) {
                spouses.add(node);
            } else if (isAdm || upper.contains("FATHER") || upper.contains("MOTHER") || upper.contains("GUARDIAN") || upper.contains("PARENT") || upper.contains("ADMIN")) {
                guardians.add(node);
            } else {
                children.add(node);
            }
        }

        // Accurate total count: exact distinct peers + 1 (Master Anchor)
        int familyCount = guardians.size() + spouses.size() + children.size() + 1;
        int friendsCount = friends.size();
        int totalCount = (selectedRosterSubTab == ROSTER_SUBTAB_FAMILY) ? familyCount : friendsCount;

        // Statistics Chip
        LinearLayout statsCard = new LinearLayout(this);
        statsCard.setOrientation(LinearLayout.HORIZONTAL);
        statsCard.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, statsCard, "CARD_STANDARD");
        statsCard.setPadding(scale(12), scale(8), scale(12), scale(8));

        TextView tvStats = new TextView(this);
        String label = (selectedRosterSubTab == ROSTER_SUBTAB_FAMILY) 
                ? "📊 ACTIVE FAMILY NODES: " + totalCount 
                : "📊 LINKED P2P FRIENDS: " + totalCount;
        tvStats.setText(label);
        tvStats.setTextColor(getGradientStart());
        tvStats.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvStats, "TEXT_TITLE");
        statsCard.addView(tvStats);

        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, 0, 0, scale(10));
        rosterContainer.addView(statsCard, sLp);

        if (selectedRosterSubTab == ROSTER_SUBTAB_FAMILY) {
            // Render Root Guardian Anchor for Family tab
            rosterContainer.addView(buildRootGuardianAnchor());

            // 1. Co-Guardians & Parents
            if (!guardians.isEmpty()) {
                renderRosterCategoryHeader("👑 CO-GUARDIANS & PARENTS (" + guardians.size() + ")", getGradientStart());
                for (Map<String, Object> node : guardians) {
                    rosterContainer.addView(buildMemberCard(node, getGradientStart()));
                }
            }

            // 2. Spouse Entities
            if (!spouses.isEmpty()) {
                renderRosterCategoryHeader("💍 SPOUSE ENTITIES (" + spouses.size() + ")", getGradientStart());
                for (Map<String, Object> node : spouses) {
                    rosterContainer.addView(buildMemberCard(node, getGradientStart()));
                }
            }

            // 3. Dependent Children & Members
            if (!children.isEmpty()) {
                renderRosterCategoryHeader("🧸 DEPENDENT TARGETS / CHILDREN (" + children.size() + ")", getGradientStart());
                for (Map<String, Object> node : children) {
                    rosterContainer.addView(buildMemberCard(node, getGradientStart()));
                }
            }

            if (guardians.isEmpty() && spouses.isEmpty() && children.isEmpty()) {
                boolean isFiltered = rosterSearchFilter != null && !rosterSearchFilter.isEmpty();
                String desc = isFiltered
                        ? "No family members match \"" + rosterSearchFilter + "\". Tap below to clear filter."
                        : "Pair a family member using the Remote Cloud or Nearby P2P tab.";
                String action = isFiltered ? "CLEAR SEARCH" : "GENERATE CLOUD PIN";
                Runnable run = isFiltered 
                        ? () -> { rosterSearchFilter = ""; rebuildRosterView(currentCachedRoster); }
                        : () -> switchToTab(TAB_REMOTE_CLOUD);

                View empty = UiArchitect.createEmptyStateCard(
                        this,
                        isFiltered ? "No Matching Family" : "No Family Members Added",
                        desc,
                        android.R.drawable.ic_menu_add,
                        action,
                        run
                );
                rosterContainer.addView(empty);
            }
        } else {
            // Selected Sub-Tab: FRIENDS
            if (!friends.isEmpty()) {
                renderRosterCategoryHeader("🌐 EXTENDED FRIEND MESH (P2P) (" + friends.size() + ")", getGradientEnd());
                for (Map<String, Object> node : friends) {
                    rosterContainer.addView(buildMemberCard(node, getGradientEnd()));
                }
            } else {
                boolean isFiltered = rosterSearchFilter != null && !rosterSearchFilter.isEmpty();
                String desc = isFiltered
                        ? "No friends match \"" + rosterSearchFilter + "\". Tap below to clear filter."
                        : "Pair a friend using the Nearby P2P or Remote Cloud tab.";
                String action = isFiltered ? "CLEAR SEARCH" : "START P2P SESSION";
                Runnable run = isFiltered 
                        ? () -> { rosterSearchFilter = ""; rebuildRosterView(currentCachedRoster); }
                        : () -> switchToTab(TAB_NEARBY_P2P);

                View empty = UiArchitect.createEmptyStateCard(
                        this,
                        isFiltered ? "No Matching Friends" : "No Friends Linked",
                        desc,
                        android.R.drawable.ic_menu_share,
                        action,
                        run
                );
                rosterContainer.addView(empty);
            }
        }
    }

    private void renderRosterCategoryHeader(String titleText, int color) {
        TextView tv = new TextView(this);
        tv.setText(titleText);
        tv.setTextColor(color);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(scale(6), scale(10), 0, scale(4));
        UiArchitect.applyText(this, tv, "TEXT_LABEL");
        rosterContainer.addView(tv);
    }

    private View buildMemberCard(Map<String, Object> node, int accentColor) {
        String uid = (String) node.get("uid_1");
        if (uid == null) uid = (String) node.get("uid");
        if (uid == null) uid = (String) node.get("identity_id");
        if (uid == null) uid = "UNKNOWN_UID";

        String name = (String) node.get("fullName");
        if (name == null) name = (String) node.get("display_name");
        if (name == null) name = "Secure Node";

        String role = (String) node.get("role_type");
        if (role == null) role = (String) node.get("role_title");
        if (role == null) role = "Member";

        final String finalUid = uid;
        final String finalName = name;
        final String finalRole = role;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, card, "CARD_STANDARD");
        int pad = scale(12);
        card.setPadding(pad, pad, pad, pad);
        card.setFocusable(true);
        card.setClickable(true);

        // Member Avatar
        // Member Avatar with SSOT Lookup & Smart Fallback
        ImageView avatar = new ImageView(this);
        com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
        String avatarRef = (String) node.get("avatar_id");
        if (avatarRef == null || avatarRef.isEmpty() || avatarRef.equals("1")) {
            avatarRef = db.getDeviceCentricAvatar(finalUid);
        }
        if (avatarRef == null || avatarRef.isEmpty()) {
            avatarRef = "butterfly";
        }
        com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, avatar, avatarRef);

        avatar.setPadding(3, 3, 3, 3);
        GradientDrawable avBg = new GradientDrawable();
        avBg.setShape(GradientDrawable.OVAL);
        avBg.setColor(getPrimaryColor());
        avBg.setStroke(2, accentColor);
        avatar.setBackground(avBg);
        int avSize = scale(52);
        card.addView(avatar, new LinearLayout.LayoutParams(avSize, avSize));

        // Details Block
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(scale(12), 0, 0, 0);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        String rUpper = finalRole.toUpperCase();
        String roleIcon = "👥";
        if (rUpper.contains("FATHER") || rUpper.contains("MOTHER") || rUpper.contains("GUARDIAN") || rUpper.contains("ADMIN")) roleIcon = "👑";
        else if (rUpper.contains("SPOUSE") || rUpper.contains("WIFE") || rUpper.contains("HUSBAND") || rUpper.contains("PARTNER")) roleIcon = "💍";
        else if (rUpper.contains("CHILD") || rUpper.contains("SON") || rUpper.contains("DAUGHTER") || rUpper.contains("KID")) roleIcon = "🧸";
        else if (rUpper.contains("FRIEND") || rUpper.contains("PEER") || rUpper.contains("COLLEAGUE")) roleIcon = "🤝";
        final String finalRoleIcon = roleIcon;

        String ssotName = db.getDeviceCentricName(finalUid);
        String displayNodeName = (ssotName != null && !ssotName.startsWith("Agent_") && !ssotName.equalsIgnoreCase("null")) ? ssotName : finalName;

        TextView tvName = new TextView(this);
        tvName.setText("👤 " + displayNodeName.toUpperCase());
        tvName.setTextColor(getTextColor());
        tvName.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvName, "TEXT_TITLE");
        details.addView(tvName);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(roleIcon + " " + finalRole);
        tvStatus.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvStatus, "TEXT_MINI");
        details.addView(tvStatus);

        card.addView(details);

        // Action 1: Settings / Person Details
        ImageView infoBtn = new ImageView(this);
        infoBtn.setImageResource(android.R.drawable.ic_menu_manage);
        infoBtn.setColorFilter(accentColor);
        int actSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(actSize, actSize);
        iLp.setMargins(scale(4), 0, scale(4), 0);
        infoBtn.setLayoutParams(iLp);
        infoBtn.setFocusable(true);
        infoBtn.setClickable(true);
        infoBtn.setOnClickListener(v -> {
            triggerHapticFeedback();
            Intent pdIntent = new Intent(this, PersonDetailsActivity.class);
            pdIntent.putExtra("TARGET_UID", finalUid);
            pdIntent.putExtra("TARGET_ROLE", finalRole);
            pdIntent.putExtra("TARGET_NAME", finalName);
            startActivity(pdIntent);
        });
        card.addView(infoBtn);

        // Action 2: Delete / Purge
        ImageView delBtn = new ImageView(this);
        delBtn.setImageResource(android.R.drawable.ic_menu_delete);
        delBtn.setColorFilter(Color.parseColor("#FF5252"));
        delBtn.setLayoutParams(iLp);
        delBtn.setFocusable(true);
        delBtn.setClickable(true);
        delBtn.setOnClickListener(v -> promptPurgeMemberDialog(finalUid, finalName));
        card.addView(delBtn);

        // Tap card to navigate directly to Live Map focus
        card.setOnClickListener(v -> {
            triggerHapticFeedback();
            Intent mapIntent = new Intent(this, MapActivity.class);
            mapIntent.putExtra("target_uid", finalUid);
            startActivity(mapIntent);
        });

        // Async resolve Firebase live status & avatar with full Live Map parity
        FirebaseComm.INSTANCE.getDevicesRef().child(finalUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot locSnap) {
                if (!isFinishing() && !isDestroyed()) {
                    DataSnapshot prof = locSnap.child("profile");
                    DataSnapshot live = locSnap.child("device").child("live");

                    String avId = prof.child("avatar_id").getValue(String.class);
                    if (avId == null) avId = prof.child("equipped_avatar_id").getValue(String.class);
                    if (avId == null) avId = live.child("avatar_id").getValue(String.class);
                    if (avId == null) avId = locSnap.child("avatar_id").getValue(String.class);
                    if (avId == null || avId.isEmpty()) avId = "butterfly";
                    AvatarLoader.bindAvatar(NetworkGatewayActivity.this, avatar, avId);

                    String cloudName = prof.child("fullName").getValue(String.class);
                    if (cloudName == null) cloudName = prof.child("full_name").getValue(String.class);
                    if (cloudName == null) cloudName = prof.child("name").getValue(String.class);
                    if (cloudName == null) cloudName = live.child("userName").getValue(String.class);
                    if (cloudName != null && !cloudName.isEmpty() && !cloudName.startsWith("Agent_")) {
                        tvName.setText("👤 " + cloudName.toUpperCase());
                    }

                    long ts = 0L;
                    Object tsObj = live.child("updated_at").getValue();
                    if (tsObj == null) tsObj = locSnap.child("timestamp").getValue();
                    if (tsObj instanceof Number) {
                        ts = ((Number) tsObj).longValue();
                    } else if (tsObj instanceof String) {
                        try {
                            String s = (String) tsObj;
                            if (s.length() == 16) {
                                SimpleDateFormat sdfIn = new SimpleDateFormat("yyyyMMddHHmmssSS", Locale.US);
                                Date d = sdfIn.parse(s);
                                if (d != null) ts = d.getTime();
                            } else {
                                ts = Long.parseLong(s);
                            }
                        } catch (Exception ignored) {
                            ts = 0L;
                        }
                    }

                    int batt = -1;
                    Object batObj = live.child("battery").getValue();
                    if (batObj instanceof Number) batt = ((Number) batObj).intValue();

                    boolean isSos = Boolean.TRUE.equals(live.child("sos").getValue(Boolean.class));
                    boolean isDeleted = db.isDeviceDeleted(finalUid);
                    boolean isPendingUnlink = db.isDevicePendingUnlink(finalUid);

                    // 🚦 Evaluate Unified Enterprise RAG Status
                    com.jdroidx.jfindme.utils.DeviceRagEngine.EvaluationResult rag = 
                        com.jdroidx.jfindme.utils.DeviceRagEngine.evaluate(ts, batt, isSos, isDeleted, false, isPendingUnlink);

                    tvStatus.setText(finalRoleIcon + " " + finalRole + " • " + rag.getStatusText());
                    tvStatus.setTextColor(rag.getStatusColor());
                    card.setAlpha(rag.getCardAlpha());
                    avBg.setStroke(2, rag.getStrokeColor());
                    AvatarLoader.bindAvatar(NetworkGatewayActivity.this, avatar, avId, rag.isDesaturated());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, scale(8));
        card.setLayoutParams(cardLp);

        return card;
    }

    // =========================================================================
    // VI. PAIRING EXECUTION, VALIDATION & APPROVAL DIALOGS
    // =========================================================================

    private void launchBarcodeScanner() {
        triggerHapticFeedback();
        barcodeLauncher.launch(new ScanOptions()
                .setOrientationLocked(false)
                .setBeepEnabled(true)
                .setPrompt("Align Pairing QR Matrix within the Frame"));
    }

    private void validateAndShowRoleDialog(String input, int expectedLength, boolean isLocal) {
        if (input == null || input.trim().length() != expectedLength) {
            Toast.makeText(this, "Please enter a valid " + expectedLength + "-digit code.", Toast.LENGTH_SHORT).show();
            return;
        }
        triggerHapticFeedback();
        if (isLocal) {
            showRoleSelectionDialogForLocalJoin(input.trim().toUpperCase());
        } else {
            showRoleSelectionDialogBeforeJoin(input.trim().toUpperCase());
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
        int currentTtl = getSharedPreferences("system_registry", MODE_PRIVATE).getInt("pairing_pin_ttl_minutes", 5);
        int selectedIdx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentTtl) { selectedIdx = i; break; }
        }

        new AlertDialog.Builder(this)
            .setTitle("⏱️ Pairing PIN Expiry TTL")
            .setSingleChoiceItems(options, selectedIdx, (dialog, which) -> {
                int chosen = values[which];
                getSharedPreferences("system_registry", MODE_PRIVATE).edit().putInt("pairing_pin_ttl_minutes", chosen).apply();
                Toast.makeText(this, "Pairing PIN Expiry TTL set to " + chosen + " minutes.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                viewModel.startCountdown(chosen, selectedTab == TAB_NEARBY_P2P, () -> {
                    Toast.makeText(this, "PIN / Token Expired.", Toast.LENGTH_SHORT).show();
                    return kotlin.Unit.INSTANCE;
                });
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void executeHostSessionSpinup() {
        triggerHapticFeedback();
        if (tvCode != null) tvCode.setText("NEGOTIATING...");
        if (tvSyncStatus != null) {
            tvSyncStatus.setText("ALLOCATING CLOUD PIN...");
            tvSyncStatus.setTextColor(Color.YELLOW);
        }
        if (pbCodeLoading != null) pbCodeLoading.setVisibility(View.VISIBLE);

        int ttlMinutes = getSharedPreferences("system_registry", MODE_PRIVATE).getInt("pairing_pin_ttl_minutes", 5);

        FirebasePairingManager.INSTANCE.startHostPairingSession(this, (qrBitmap, code) -> {
            runOnUiThread(() -> {
                if (pbCodeLoading != null) pbCodeLoading.setVisibility(View.GONE);
                if (code != null && !code.isEmpty()) {
                    if (tvCode != null) tvCode.setText(code);
                    viewModel.startCountdown(ttlMinutes, false, () -> {
                        if (!isFinishing() && !isDestroyed()) {
                            new AlertDialog.Builder(this)
                                .setTitle("🔴 Cloud Pairing PIN Expired")
                                .setMessage("The current pairing session has timed out for security.\n\nWould you like to generate a fresh pairing PIN now?")
                                .setPositiveButton("⚡ REGENERATE PIN", (d, w) -> executeHostSessionSpinup())
                                .setNegativeButton("DISMISS", null)
                                .show();
                        }
                        return kotlin.Unit.INSTANCE;
                    });
                    if (qrBitmap != null && ivQrCode != null) {
                        if (qrPlaceholderCloud != null) qrPlaceholderCloud.setVisibility(View.GONE);
                        ivQrCode.setImageBitmap(qrBitmap);
                        ivQrCode.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvCode != null) tvCode.setText("RETRY");
                    if (tvSyncStatus != null) {
                        tvSyncStatus.setText("NETWORK ALLOCATION TIMED OUT");
                        tvSyncStatus.setTextColor(Color.RED);
                    }
                }
            });
            return kotlin.Unit.INSTANCE;
        }, (childDeviceId, childName, childRole) -> {
            runOnUiThread(() -> renderHostApprovalDialog(childDeviceId, childName, childRole, FirebasePairingManager.INSTANCE.getActivePairingCode()));
            return kotlin.Unit.INSTANCE;
        });
    }

    private void executeLocalHostSessionSpinup() {
        triggerHapticFeedback();
        if (tvLocalCode != null) tvLocalCode.setText("SPINNING UP...");
        if (pbLocalLoading != null) pbLocalLoading.setVisibility(View.VISIBLE);

        int ttlMinutes = getSharedPreferences("system_registry", MODE_PRIVATE).getInt("pairing_pin_ttl_minutes", 5);

        LocalPairingManager.INSTANCE.setOnLocalMergeCompletedCallback(() -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Local Node Linked Successfully! ✅", Toast.LENGTH_LONG).show();
                viewModel.refreshLocalRoster(DatabaseHelper.getInstance(this));
                initiateCloudSync(forensicAgentId);
            });
            return kotlin.Unit.INSTANCE;
        });

        LocalPairingManager.INSTANCE.startLocalHostSession(this, localToken -> {
            runOnUiThread(() -> {
                if (pbLocalLoading != null) pbLocalLoading.setVisibility(View.GONE);
                if (tvLocalCode != null) tvLocalCode.setText(localToken);
                viewModel.startCountdown(ttlMinutes, true, () -> {
                    if (!isFinishing() && !isDestroyed()) {
                        new AlertDialog.Builder(this)
                            .setTitle("🔴 Local P2P Token Expired")
                            .setMessage("The local broadcast token has timed out.\n\nWould you like to spin up a fresh local token?")
                            .setPositiveButton("⚡ REGENERATE TOKEN", (d, w) -> executeLocalHostSessionSpinup())
                            .setNegativeButton("DISMISS", null)
                            .show();
                    }
                    return kotlin.Unit.INSTANCE;
                });
                if (ivLocalQrCode != null) {
                    if (qrPlaceholderLocal != null) qrPlaceholderLocal.setVisibility(View.GONE);
                    generateQrCode(localToken, ivLocalQrCode);
                }
            });
            return kotlin.Unit.INSTANCE;
        });
    }

    private void showRoleSelectionDialogBeforeJoin(String code) {
        final String cleaned = code.trim().toUpperCase();
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(scale(20), scale(16), scale(20), scale(16));

        TextView title = new TextView(this);
        title.setText("🏷️ IDENTIFY YOUR ROLE (CLOUD)");
        title.setTextColor(getTextColor());
        title.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, title, "TEXT_TITLE");
        title.setPadding(0, 0, 0, scale(16));
        container.addView(title);

        String[] roles = {"Father", "Mother", "Guardian", "Spouse", "Child", "Friends"};
        String[] icons = {"👑", "👑", "🛡️", "💍", "🧸", "🤝"};
        AlertDialog dialog = new AlertDialog.Builder(this).create();

        for (int i = 0; i < roles.length; i++) {
            final String role = roles[i];
            final String icon = icons[i];
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(scale(16), scale(12), scale(16), scale(12));
            UiArchitect.applyCard(this, card, "CARD_STANDARD");

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, scale(10));
            card.setLayoutParams(lp);

            TextView rText = new TextView(this);
            rText.setText(icon + "  " + role.toUpperCase());
            rText.setTextColor(getGradientStart());
            rText.setTypeface(null, Typeface.BOLD);
            UiArchitect.applyText(this, rText, "TEXT_BODY");
            card.addView(rText);

            card.setOnClickListener(v -> {
                dialog.dismiss();
                promptGdprConsentAndJoin(cleaned, role, false);
            });
            container.addView(card);
        }

        sv.addView(container);
        dialog.setView(sv);
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            applyDialogTheme(dialog);
        }
    }

    private void showRoleSelectionDialogForLocalJoin(String code) {
        final String cleaned = code.trim().toUpperCase();
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(scale(20), scale(16), scale(20), scale(16));

        TextView title = new TextView(this);
        title.setText("🏷️ IDENTIFY YOUR ROLE (NEARBY)");
        title.setTextColor(getTextColor());
        title.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, title, "TEXT_TITLE");
        title.setPadding(0, 0, 0, scale(16));
        container.addView(title);

        String[] roles = {"Father", "Mother", "Guardian", "Spouse", "Child", "Friends"};
        String[] icons = {"👑", "👑", "🛡️", "💍", "🧸", "🤝"};
        AlertDialog dialog = new AlertDialog.Builder(this).create();

        for (int i = 0; i < roles.length; i++) {
            final String role = roles[i];
            final String icon = icons[i];
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(scale(16), scale(12), scale(16), scale(12));
            UiArchitect.applyCard(this, card, "CARD_STANDARD");

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, scale(10));
            card.setLayoutParams(lp);

            TextView rText = new TextView(this);
            rText.setText(icon + "  " + role.toUpperCase());
            rText.setTextColor(getGradientEnd());
            rText.setTypeface(null, Typeface.BOLD);
            UiArchitect.applyText(this, rText, "TEXT_BODY");
            card.addView(rText);

            card.setOnClickListener(v -> {
                dialog.dismiss();
                promptGdprConsentAndJoin(cleaned, role, true);
            });
            container.addView(card);
        }

        sv.addView(container);
        dialog.setView(sv);
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            applyDialogTheme(dialog);
        }
    }

    private void promptGdprConsentAndJoin(String cleanedCode, String selectedRole, boolean isLocal) {
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(scale(20), scale(16), scale(20), scale(16));

        TextView title = new TextView(this);
        title.setText("🔒 PRIVACY & LOCATION DISCLOSURE");
        title.setTextColor(getTextColor());
        title.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, title, "TEXT_TITLE");
        title.setPadding(0, 0, 0, scale(10));
        container.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Under GDPR Article 6 & international privacy standards, by connecting as a [" + selectedRole + "], you authorize real-time telemetry synchronization with this private network:");
        desc.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, desc, "TEXT_BODY");
        desc.setPadding(0, 0, 0, scale(12));
        container.addView(desc);

        String[] clauses = {
            "✓ Continuous GPS & Geofence Boundary Alerts",
            "✓ Device Battery & Telemetry Shared with Group",
            "✓ 30-Day Rolling Storage Limit on Location History",
            "✓ One-Click Right to Erasure / Unlink at any time"
        };
        for (String clause : clauses) {
            TextView cTv = new TextView(this);
            cTv.setText(clause);
            cTv.setTextColor(Color.parseColor("#00E676"));
            cTv.setTextSize(scaleText(11f));
            cTv.setTypeface(null, Typeface.BOLD);
            cTv.setPadding(0, 0, 0, scale(6));
            container.addView(cTv);
        }

        AlertDialog dialog = new AlertDialog.Builder(this).create();

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, scale(14), 0, 0);

        Button btnDecline = new Button(this);
        btnDecline.setText("❌ DECLINE");
        btnDecline.setTextColor(Color.WHITE);
        btnDecline.setBackgroundColor(Color.parseColor("#757575"));
        btnDecline.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnDecline, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnAgree = new Button(this);
        btnAgree.setText("✅ AGREE & CONNECT");
        btnAgree.setTextColor(Color.WHITE);
        btnAgree.setBackgroundColor(Color.parseColor("#00E676"));
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        aLp.setMargins(scale(8), 0, 0, 0);
        btnAgree.setLayoutParams(aLp);
        btnAgree.setOnClickListener(v -> {
            dialog.dismiss();
            if (isLocal) {
                executeLocalJoinRequestWithRole(cleanedCode, selectedRole);
            } else {
                executeChildJoinRequest(cleanedCode, selectedRole);
            }
        });
        btnRow.addView(btnAgree);

        container.addView(btnRow);
        sv.addView(container);
        dialog.setView(sv);
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            applyDialogTheme(dialog);
        }
    }

    private void executeChildJoinRequest(String inputMatrix, String selectedRole) {
        if (inputMatrix != null && inputMatrix.length() == 6) {
            Toast.makeText(this, "Connecting to Circle [" + selectedRole + "]...", Toast.LENGTH_SHORT).show();
            String myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
            PairingRepository.joinCircleWithInviteCode(inputMatrix, myAgentId, (success, message) -> {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(NetworkGatewayActivity.this, "🎉 Joined Circle! Opening Map...", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(NetworkGatewayActivity.this, MapActivity.class));
                        finish();
                    } else {
                        // Fallback to active pin session verification
                        FirebasePairingManager.INSTANCE.requestJoinGroup(NetworkGatewayActivity.this, inputMatrix, selectedRole, (pinSuccess, pinMessage) -> {
                            runOnUiThread(() -> {
                                if (pinSuccess && "FULLY_SYNCED".equals(pinMessage)) {
                                    Toast.makeText(NetworkGatewayActivity.this, "Pairing Complete! Opening Map...", Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(NetworkGatewayActivity.this, MapActivity.class));
                                    new Handler(Looper.getMainLooper()).postDelayed(NetworkGatewayActivity.this::finish, 1500);
                                } else if (pinSuccess && "PENDING_HANDSHAKE".equals(pinMessage)) {
                                    Toast.makeText(NetworkGatewayActivity.this, "Handshake Sent! Waiting for Host Approval...", Toast.LENGTH_SHORT).show();
                                    listenForHandshakeApproval(inputMatrix);
                                } else {
                                    Toast.makeText(NetworkGatewayActivity.this, message != null ? message : (pinMessage != null ? pinMessage : "Pairing Failed"), Toast.LENGTH_SHORT).show();
                                }
                            });
                            return kotlin.Unit.INSTANCE;
                        });
                    }
                });
            });
            return;
        }

        Toast.makeText(this, "Connecting to Cloud Session [" + selectedRole + "]...", Toast.LENGTH_SHORT).show();
        FirebasePairingManager.INSTANCE.requestJoinGroup(this, inputMatrix, selectedRole, (success, message) -> {
            runOnUiThread(() -> {
                if (success && "FULLY_SYNCED".equals(message)) {
                    Toast.makeText(this, "Pairing Complete! Opening Map...", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MapActivity.class));
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
                } else if (success && "PENDING_HANDSHAKE".equals(message)) {
                    Toast.makeText(this, "Handshake Sent! Waiting for Host Approval...", Toast.LENGTH_SHORT).show();
                    listenForHandshakeApproval(inputMatrix);
                } else {
                    Toast.makeText(this, message != null ? message : "Pairing Failed", Toast.LENGTH_SHORT).show();
                }
            });
            return kotlin.Unit.INSTANCE;
        });
    }

    private void executeLocalJoinRequestWithRole(String inputMatrix, String selectedRole) {
        Toast.makeText(this, "Searching for Local Node: " + inputMatrix + "...", Toast.LENGTH_SHORT).show();
        LocalPairingManager.INSTANCE.startLocalDiscoveryBinding(inputMatrix, this, selectedRole, hostId -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Local Offline Pairing Completed! ✅", Toast.LENGTH_LONG).show();
                viewModel.refreshLocalRoster(DatabaseHelper.getInstance(this));
                initiateCloudSync(forensicAgentId);
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
            });
            return kotlin.Unit.INSTANCE;
        });
    }

    private void listenForHandshakeApproval(String activeCode) {
        viewModel.registerHandshakeApprovalListener(activeCode, () -> {
            runOnUiThread(() -> {
                Toast.makeText(NetworkGatewayActivity.this, "Host Approved! ✅", Toast.LENGTH_LONG).show();
                startActivity(new Intent(NetworkGatewayActivity.this, MapActivity.class));
                new Handler(Looper.getMainLooper()).postDelayed(NetworkGatewayActivity.this::finish, 1500);
            });
            return kotlin.Unit.INSTANCE;
        }, () -> {
            runOnUiThread(() -> Toast.makeText(NetworkGatewayActivity.this, "Host rejected the request.", Toast.LENGTH_LONG).show());
            return kotlin.Unit.INSTANCE;
        });
    }

    private void renderHostApprovalDialog(String childId, String childName, String childPrelimRole, String activeCode) {
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        Map<String, String> pairingHistory = db.getPairingHistoryAndLocation(childId);

        if (pairingHistory != null) {
            new AlertDialog.Builder(this)
                    .setTitle("🚨 Node Already Paired")
                    .setMessage("Node (" + childName + ") is already in your trust matrix.\n\nDo you want to re-authorize and update role?")
                    .setPositiveButton("RE-AUTHORIZE", (d, w) -> showRoleApprovalOptions(childId, childName, childPrelimRole, activeCode))
                    .setNegativeButton("CANCEL", (d, w) -> FirebasePairingManager.INSTANCE.rejectHandshake(activeCode))
                    .setCancelable(false)
                    .show();
        } else {
            showRoleApprovalOptions(childId, childName, childPrelimRole, activeCode);
        }
    }

    private void showRoleApprovalOptions(String childId, String childName, String childPrelimRole, String activeCode) {
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(scale(20), scale(16), scale(20), scale(16));

        TextView t1 = new TextView(this);
        t1.setText("HANDSHAKE REQUEST");
        t1.setTextColor(getTextColor());
        t1.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, t1, "TEXT_TITLE");
        container.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText("Target (" + childName + ") requested role: " + childPrelimRole);
        t2.setTextColor(getSecondaryTextColor());
        t2.setPadding(0, scale(4), 0, scale(12));
        UiArchitect.applyText(this, t2, "TEXT_SUB");
        container.addView(t2);

        List<String> validHostRoles = new ArrayList<>();
        if ("Friends".equals(childPrelimRole)) validHostRoles.add("Friends");
        else if ("Spouse".equals(childPrelimRole)) validHostRoles.add("Spouse");
        else if ("Child".equals(childPrelimRole)) {
            validHostRoles.add("Father");
            validHostRoles.add("Mother");
            validHostRoles.add("Guardian");
        } else if (Arrays.asList("Father", "Mother", "Guardian").contains(childPrelimRole)) {
            validHostRoles.add("Child");
        } else {
            validHostRoles.addAll(Arrays.asList("Guardian", "Family Member", "Friends"));
        }

        AlertDialog dialog = new AlertDialog.Builder(this).create();

        for (String hostRole : validHostRoles) {
            String hUpper = hostRole.toUpperCase();
            String hIcon = "👥";
            if (hUpper.contains("FATHER") || hUpper.contains("MOTHER") || hUpper.contains("GUARDIAN") || hUpper.contains("ADMIN")) hIcon = "👑";
            else if (hUpper.contains("SPOUSE") || hUpper.contains("WIFE") || hUpper.contains("HUSBAND") || hUpper.contains("PARTNER")) hIcon = "💍";
            else if (hUpper.contains("CHILD") || hUpper.contains("SON") || hUpper.contains("DAUGHTER") || hUpper.contains("KID")) hIcon = "🧸";
            else if (hUpper.contains("FRIEND") || hUpper.contains("PEER") || hUpper.contains("COLLEAGUE")) hIcon = "🤝";

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(scale(16), scale(12), scale(16), scale(12));
            UiArchitect.applyCard(this, card, "CARD_STANDARD");

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, scale(10));
            card.setLayoutParams(lp);

            TextView rText = new TextView(this);
            rText.setText(hIcon + "  ACCEPT AS: " + hostRole.toUpperCase());
            rText.setTextColor(getGradientStart());
            rText.setTypeface(null, Typeface.BOLD);
            UiArchitect.applyText(this, rText, "TEXT_BODY");
            card.addView(rText);

            card.setOnClickListener(v -> {
                dialog.dismiss();
                String childRoleToSave = childPrelimRole;
                String hostRoleToSave = hostRole;

                if ("Child".equals(childPrelimRole)) {
                    childRoleToSave = "Child";
                    hostRoleToSave = hostRole;
                } else if (Arrays.asList("Father", "Mother", "Guardian").contains(childPrelimRole)) {
                    childRoleToSave = childPrelimRole;
                    hostRoleToSave = "Child";
                }

                boolean isFriend = "Friends".equals(hostRole);
                final String finalChild = childRoleToSave;
                final boolean finalIsFriend = isFriend;

                FirebasePairingManager.INSTANCE.approveHandshake(this, activeCode, childId, hostRoleToSave, childRoleToSave, success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            String activeGroupId = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", "DEFAULT_GROUP");
                            DatabaseHelper.getInstance(this).updateCitizenRole(
                                    childId,
                                    finalIsFriend ? "None" : finalChild,
                                    finalIsFriend ? "Friends" : "None",
                                    finalIsFriend,
                                    activeGroupId
                            );
                            Toast.makeText(this, "🛡️ Handshake Approved! Node Added.", Toast.LENGTH_SHORT).show();
                            viewModel.refreshLocalRoster(DatabaseHelper.getInstance(this));
                            startActivity(new Intent(this, MapActivity.class));
                            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
                        } else {
                            Toast.makeText(this, "Handshake failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return kotlin.Unit.INSTANCE;
                });
            });
            container.addView(card);
        }

        Button btnReject = new Button(this);
        btnReject.setText("🚫 REJECT REQUEST");
        btnReject.setTextColor(Color.WHITE);
        btnReject.setBackgroundColor(Color.parseColor("#FF5252"));
        btnReject.setOnClickListener(v -> {
            dialog.dismiss();
            FirebasePairingManager.INSTANCE.rejectHandshake(activeCode);
        });
        container.addView(btnReject);

        sv.addView(container);
        dialog.setView(sv);
        dialog.setCancelable(false);
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            applyDialogTheme(dialog);
        }
    }

    private void promptPurgeMemberDialog(String uid, String name) {
        triggerHapticFeedback();
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        boolean isParked = db.isDevicePendingUnlink(uid);
        String grp = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);

        if (isParked) {
            new AlertDialog.Builder(this)
                    .setTitle("⏳ Manage Parked Unlink")
                    .setMessage("Device (" + name + ") is currently PARKED for unlinking in Grey state. What would you like to do?")
                    .setPositiveButton("✅ FINALIZE UNLINK", (d, w) -> {
                        viewModel.deleteMember(uid, grp, db, () -> {
                            Toast.makeText(this, "Device unlinked and permanently purged.", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .setNeutralButton("↩️ RESTORE ACTIVE", (d, w) -> {
                        viewModel.cancelParkedDeletion(uid, grp, db, () -> {
                            Toast.makeText(this, "Connection restored to Active.", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .setNegativeButton("CLOSE", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Manage Connection: " + name)
                    .setMessage("Choose unlinking method for (" + name + "):")
                    .setPositiveButton("🔘 PARK & NOTIFY", (d, w) -> {
                        viewModel.parkMemberForDeletion(uid, grp, db, () -> {
                            Toast.makeText(this, "Device parked in Grey state & push notification sent.", Toast.LENGTH_LONG).show();
                        });
                    })
                    .setNeutralButton("🗑️ IMMEDIATE PURGE", (d, w) -> {
                        viewModel.deleteMember(uid, grp, db, () -> {
                            Toast.makeText(this, "Device purged immediately.", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        }
    }

    // =========================================================================
    // VII. RECLAIM LEGACY CLOUD GROUP RECOVERY
    // =========================================================================

    private void showRestoreOldGroupDialog() {
        triggerHapticFeedback();
        final String safeAgentId = forensicAgentId != null && !forensicAgentId.isEmpty() ? forensicAgentId : DeviceIdentityHelper.getOrGenerateAgentId(this);
        Toast.makeText(this, "Scanning cloud history for legacy assets...", Toast.LENGTH_SHORT).show();

        FirebaseComm.INSTANCE.getIdentityRef(safeAgentId).child("active_group").get().addOnCompleteListener(grpTask -> {
            String myGrp = null;
            if (grpTask.isSuccessful() && grpTask.getResult() != null) {
                myGrp = grpTask.getResult().getValue(String.class);
            }
            if (myGrp == null || myGrp.isEmpty() || myGrp.equals("DEFAULT_GROUP")) {
                myGrp = "GRP_" + safeAgentId;
            }
            final String finalGrp = myGrp;

            // 🛰️ Precise Circle Discovery: Check direct relationships + shared circle history
            FirebaseComm.getDeviceRef(safeAgentId).child("relationships").get().addOnCompleteListener(relTask -> {
                final Set<String> authorizedLinkedUids = new HashSet<>();
                final Map<String, String> assetRoles = new HashMap<>();
                final Map<String, Boolean> assetIsAdmin = new HashMap<>();
                final Map<String, Boolean> assetIsFriend = new HashMap<>();

                if (relTask.isSuccessful() && relTask.getResult() != null) {
                    for (DataSnapshot child : relTask.getResult().getChildren()) {
                        String key = child.getKey();
                        if (key != null && !key.equals(safeAgentId)) {
                            authorizedLinkedUids.add(key);
                            String r = null;
                            if (child.hasChild("role_title")) r = child.child("role_title").getValue(String.class);
                            else if (child.hasChild("role")) r = child.child("role").getValue(String.class);

                            String branch = child.child("branch_type").getValue(String.class);
                            boolean isFr = "friends".equalsIgnoreCase(branch) || (r != null && r.toLowerCase().contains("friend"));
                            boolean isAdm = (r != null && (r.toUpperCase().contains("GUARDIAN") || r.toUpperCase().contains("ADMIN") || r.toUpperCase().contains("MOTHER") || r.toUpperCase().contains("FATHER") || r.toUpperCase().contains("PARENT")));

                            if (r == null || r.isEmpty()) {
                                r = isFr ? "Friend" : "Child";
                            }
                            assetRoles.put(key, r);
                            assetIsFriend.put(key, isFr);
                            assetIsAdmin.put(key, isAdm);
                        }
                    }
                }

                // Also check all shared groups where safeAgentId is a member
                FirebaseComm.getRelationshipsRef().get().addOnCompleteListener(groupsTask -> {
                    if (groupsTask.isSuccessful() && groupsTask.getResult() != null) {
                        for (DataSnapshot grpChild : groupsTask.getResult().getChildren()) {
                            if (grpChild.hasChild("members") && grpChild.child("members").hasChild(safeAgentId)) {
                                for (DataSnapshot m : grpChild.child("members").getChildren()) {
                                    String mKey = m.getKey();
                                    if (mKey != null && !mKey.equals(safeAgentId)) {
                                        authorizedLinkedUids.add(mKey);
                                        if (!assetRoles.containsKey(mKey)) {
                                            String mRole = m.child("role").getValue(String.class);
                                            if (mRole == null || mRole.isEmpty() || mRole.equalsIgnoreCase("Member")) {
                                                mRole = "Child";
                                            }
                                            boolean isFr = mRole.toLowerCase().contains("friend");
                                            boolean isAdm = mRole.toUpperCase().contains("GUARDIAN") || mRole.toUpperCase().contains("ADMIN") || mRole.toUpperCase().contains("PARENT");
                                            assetRoles.put(mKey, mRole);
                                            assetIsFriend.put(mKey, isFr);
                                            assetIsAdmin.put(mKey, isAdm);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Query profiles for verified linked nodes only
                    FirebaseComm.getDevicesRef().get().addOnCompleteListener(devicesTask -> {
                        final List<String> assetNames = new ArrayList<>();
                        final List<String> assetUids = new ArrayList<>();
                        final Map<String, String> assetAvatars = new HashMap<>();

                        if (devicesTask.isSuccessful() && devicesTask.getResult() != null) {
                            DataSnapshot devSnap = devicesTask.getResult();
                            for (String linkedId : authorizedLinkedUids) {
                                if (devSnap.hasChild(linkedId)) {
                                    DataSnapshot child = devSnap.child(linkedId);
                                    String name = null;
                                    if (child.hasChild("profile") && child.child("profile").hasChild("name")) {
                                        name = child.child("profile").child("name").getValue(String.class);
                                    } else if (child.hasChild("Identity") && child.child("Identity").hasChild("name")) {
                                        name = child.child("Identity").child("name").getValue(String.class);
                                    } else if (child.hasChild("name")) {
                                        name = child.child("name").getValue(String.class);
                                    }
                                    if (name == null || name.isEmpty() || name.startsWith("Agent_")) {
                                        name = "Node " + linkedId;
                                    }
                                    String avatarId = "1";
                                    if (child.hasChild("profile") && child.child("profile").hasChild("avatar_id")) {
                                        avatarId = String.valueOf(child.child("profile").child("avatar_id").getValue());
                                    }

                                    String rTitle = assetRoles.containsKey(linkedId) ? assetRoles.get(linkedId) : "Family Member";
                                    assetNames.add(name + " [" + rTitle + "] (" + linkedId + ")");
                                    assetUids.add(linkedId);
                                    assetAvatars.put(linkedId, avatarId);
                                }
                            }
                        }
                        displayRestoreDialog(safeAgentId, finalGrp, assetNames, assetUids, assetAvatars, assetRoles, assetIsAdmin, assetIsFriend);
                    });
                });
            });
        });
    }

    private void displayRestoreDialog(String safeAgentId, String finalGrp, List<String> assetNames, List<String> assetUids, Map<String, String> assetAvatars,
                                       Map<String, String> assetRoles, Map<String, Boolean> assetIsAdmin, Map<String, Boolean> assetIsFriend) {
        if (assetNames.isEmpty()) {
            showValidationDialog("NO LEGACY NODES FOUND", "No other registered device nodes found in Cloud Matrix. Use the QR Scan or PIN Pairing tabs to link new devices.");
            return;
        }

        final String[] items = assetNames.toArray(new String[0]);
        final boolean[] checkedItems = new boolean[items.length];
        Arrays.fill(checkedItems, true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("🔄 Reclaim Legacy Circle Nodes")
                .setMultiChoiceItems(items, checkedItems, (d, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("RESTORE SELECTED", (d, which) -> {
                    String targetGroup = finalGrp.isEmpty() ? ("GRP_" + safeAgentId) : finalGrp;
                    DatabaseHelper dbLocal = DatabaseHelper.getInstance(this);

                    // 1. Ensure Self is registered as ADMIN in Cloud Group with uniform structure
                    Map<String, Object> groupInfo = new HashMap<>();
                    groupInfo.put("group_name", "Primary Circle");
                    groupInfo.put("host_device_id", safeAgentId);
                    groupInfo.put("created_at", System.currentTimeMillis());
                    FirebaseComm.INSTANCE.getRelationshipsRef().child(targetGroup).child("group_info").setValue(groupInfo);

                    Map<String, Object> selfIdentity = new HashMap<>();
                    selfIdentity.put("name", RegistryManager.getUserNickname(this));
                    selfIdentity.put("full_name", RegistryManager.getUserNickname(this));
                    selfIdentity.put("avatar_id", String.valueOf(getSharedPreferences("system_registry", MODE_PRIVATE).getInt("equipped_avatar_id", 0)));
                    
                    Map<String, Object> selfMembership = new HashMap<>();
                    selfMembership.put("role", "ADMIN");
                    selfMembership.put("role_family", "Parent");
                    selfMembership.put("role_friend", "None");
                    selfMembership.put("status", "Active");
                    selfMembership.put("ts_joined", System.currentTimeMillis());

                    FirebaseComm.INSTANCE.getRelationshipsRef().child(targetGroup).child("devices").child(safeAgentId).child("identity").setValue(selfIdentity);
                    FirebaseComm.INSTANCE.getRelationshipsRef().child(targetGroup).child("devices").child(safeAgentId).child("membership").setValue(selfMembership);
                    FirebaseComm.INSTANCE.getIdentityRef(safeAgentId).child("active_group").setValue(targetGroup);
                    getSharedPreferences("system_registry", MODE_PRIVATE).edit().putString("active_group", targetGroup).apply();

                    int count = 0;
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            String nodeUid = assetUids.get(i);
                            String rawName = assetNames.get(i);
                            String cleanName = rawName.contains(" [") ? rawName.substring(0, rawName.indexOf(" [")) : rawName;
                            String avatarId = assetAvatars.get(nodeUid);
                            if (avatarId == null) avatarId = "1";

                            String nodeRole = assetRoles.containsKey(nodeUid) ? assetRoles.get(nodeUid) : "Child";
                            boolean isAdmin = assetIsAdmin.containsKey(nodeUid) && Boolean.TRUE.equals(assetIsAdmin.get(nodeUid));
                            boolean isFriend = assetIsFriend.containsKey(nodeUid) && Boolean.TRUE.equals(assetIsFriend.get(nodeUid));

                            // Cloud Member Payload (Uniform devices/ schema)
                            Map<String, Object> nodeIdentity = new HashMap<>();
                            nodeIdentity.put("name", cleanName);
                            nodeIdentity.put("full_name", cleanName);
                            nodeIdentity.put("avatar_id", avatarId);

                            Map<String, Object> nodeMembership = new HashMap<>();
                            nodeMembership.put("role", nodeRole);
                            nodeMembership.put("role_family", nodeRole);
                            nodeMembership.put("role_friend", isFriend ? "Friend" : "None");
                            nodeMembership.put("status", "Active");
                            nodeMembership.put("ts_joined", System.currentTimeMillis());

                            FirebaseComm.INSTANCE.getRelationshipsRef().child(targetGroup).child("devices").child(nodeUid).child("identity").setValue(nodeIdentity);
                            FirebaseComm.INSTANCE.getRelationshipsRef().child(targetGroup).child("devices").child(nodeUid).child("membership").setValue(nodeMembership);
                            FirebaseComm.INSTANCE.getRelationshipsRef().child(targetGroup).child("devices").child(safeAgentId).child("paired_with").child(nodeUid).setValue(true);
                            FirebaseComm.INSTANCE.getRelationshipsRef().child(targetGroup).child("devices").child(nodeUid).child("paired_with").child(safeAgentId).setValue(true);
                            FirebaseComm.INSTANCE.getIdentityRef(nodeUid).child("active_group").setValue(targetGroup);

                            // Local SQLite Mirror with explicit role, isAdmin, and isFriend allocation
                            dbLocal.saveRosterNode(nodeUid, cleanName, nodeRole, "Active", isAdmin, isFriend, "Cloud Group", null, null, null, avatarId, targetGroup);
                            count++;
                        }
                    }

                    showSlideNotification("Circle Restored", "Successfully restored " + count + " node(s) with family/friends roles to group " + targetGroup,
                            android.R.drawable.ic_dialog_info, Color.parseColor("#00E676"), null, null);
                    initiateCloudSync(safeAgentId);
                })
                .setNegativeButton("CANCEL", null)
                .create();

        dialog.show();
        applyDialogTheme(dialog);
    }

    private void initiateCloudSync(String agentId) {
        Toast.makeText(this, "Synchronizing Roster Matrix...", Toast.LENGTH_SHORT).show();
        String activeGrp = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);
        viewModel.syncCloudGroupToLocalDb(this, activeGrp);
    }

    // =========================================================================
    // VIII. UTILITY & HELPER METHODS
    // =========================================================================

    private void copyToClipboard(String text, String label) {
        if (text == null || text.contains("-") || text.contains(".")) {
            Toast.makeText(this, "Generate a valid code first!", Toast.LENGTH_SHORT).show();
            return;
        }
        triggerHapticFeedback();
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cb != null) {
            cb.setPrimaryClip(ClipData.newPlainText(label, text));
            Toast.makeText(this, label + " Copied: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePairingCode(String text) {
        if (text == null || text.contains("-") || text.contains(".")) {
            Toast.makeText(this, "Generate a valid code first!", Toast.LENGTH_SHORT).show();
            return;
        }
        triggerHapticFeedback();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "jFindMe Pairing Invitation");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Join my private family network on jFindMe! Pairing PIN: " + text);
        startActivity(Intent.createChooser(shareIntent, "Share Pairing Code"));
    }

    private void generateQrCode(String text, ImageView targetView) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(bitMatrix);
            runOnUiThread(() -> {
                targetView.setImageBitmap(bitmap);
                targetView.setVisibility(View.VISIBLE);
            });
        } catch (Exception e) {
            SystemLogger.INSTANCE.e(TAG, "QR generation failed", e);
        }
    }

    private void triggerHapticFeedback() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception ignored) {}
    }

    private void showTacticalHandshakeOverlay(String code) {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(50, 0, 0, 0));
        overlay.setClickable(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(scale(24), scale(24), scale(24), scale(24));
        UiArchitect.applyCard(this, card, "CARD_STANDARD");

        TextView tv = new TextView(this);
        tv.setText("QR SCANNED: " + code);
        tv.setTextColor(getTextColor());
        tv.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tv, "TEXT_TITLE");
        card.addView(tv);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        overlay.addView(card, lp);

        ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();
        root.addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.animate().alpha(0f).setDuration(300).setStartDelay(1200).withEndAction(() -> root.removeView(overlay));
    }

    @Override
    protected void onSaveCurrentState(Bundle outState) {
        if (outState == null) return;
        outState.putInt("SAVED_TAB", selectedTab);
        if (etCode != null) outState.putString("SAVED_CODE", etCode.getText().toString());
        if (etLocalCode != null) outState.putString("SAVED_LOCAL_CODE", etLocalCode.getText().toString());
    }

    @Override
    protected void onUiRebuilt(Bundle savedState) {
        if (savedState == null) return;
        selectedTab = savedState.getInt("SAVED_TAB", TAB_ACTIVE_ROSTER);
        String c = savedState.getString("SAVED_CODE");
        if (etCode != null && c != null) etCode.setText(c);
        String lc = savedState.getString("SAVED_LOCAL_CODE");
        if (etLocalCode != null && lc != null) etLocalCode.setText(lc);
        updateTabVisibility();
    }
}
