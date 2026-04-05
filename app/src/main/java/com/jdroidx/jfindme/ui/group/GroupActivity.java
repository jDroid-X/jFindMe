package com.jdroidx.jfindme.ui.group;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.view.KeyEvent;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.jdroidx.jfindme.engine.sync.FirebasePairingManager;
import com.jdroidx.jfindme.engine.sync.LocalPairingManager;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;

import androidx.activity.result.ActivityResultLauncher;

import com.google.firebase.database.FirebaseDatabase;

/**
 * 🔗 UNIFIED ARCHITECTURE GATEWAY SCREEN
 * Re-engineered to support both Cloud Firebase Approvals (Remote) & P2P Networks (Nearby).
 */
public class GroupActivity extends BaseActivityJava {

    private final String TAG = "GroupGatewayScreen";
    
    // UI Panels
    private LinearLayout optionGatewayPanel;
    private LinearLayout remotePairingPanel;
    private LinearLayout localPairingPanel;
    private LinearLayout rosterCard;
    private View identityCardField; // 🚀 Master Trigger Persistence
    
    // Architecturally sound MVVM Background Execution Pipeline
    private GroupViewModel viewModel;
    
    // Dynamic Fields Option A
    private TextView tvCode, tvSyncStatus;
    private ImageView ivQrCode;
    private EditText etCode;

    // Dynamic Fields Option B
    private TextView tvLocalCode, tvLocalSyncStatus;
    private ImageView ivLocalQrCode;
    private EditText etLocalCode;
    
    // Scanner State Route
    private boolean isScanningLocal = false;

    // QR Code Scanner Result Payload
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() != null) {
                    String rawCode = result.getContents().trim();
                    showTacticalHandshakeOverlay(rawCode);
                    
                    // 🛡️ INTELLIGENT SCAN ROUTER: Detects length and routes automatically
                    // Prevents "Invalid Code" errors caused by scanning local tokens in remote mode
                    if (rawCode.length() == 8) {
                        etLocalCode.setText(rawCode);
                        executeLocalJoinRequest(rawCode);
                        Toast.makeText(this, "Local Node Detected (8-Digit). Routing to P2P Matrix...", Toast.LENGTH_SHORT).show();
                    } else if (rawCode.length() == 6) {
                        etCode.setText(rawCode);
                        showRoleSelectionDialogBeforeJoin(rawCode);
                        Toast.makeText(this, "Cloud Node Detected (6-Digit). Opening Identity Matrix...", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Unknown Forensic Token Length (" + rawCode.length() + ")", Toast.LENGTH_LONG).show();
                    }
                }
            });

    private void showRoleSelectionDialogBeforeJoin(String code) {
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(scale(40), scale(40), scale(40), scale(40));

        TextView title = new TextView(this);
        title.setText("IDENTIFY YOUR ROLE");
        title.setTextColor(getTextColor());
        title.setTextSize(scaleText(20));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, scale(40));
        container.addView(title);

        String[] options = {"Friends", "Spouse", "Father", "Mother", "Child", "Guardian"};
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        
        for (String role : options) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(scale(50), scale(40), scale(50), scale(40));
            
            // 🚀 Upgraded UI: Dynamic Theme Gradient Button
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{getGradientStart(), getGradientEnd()}
            );
            gd.setCornerRadius(25f);
            card.setBackground(gd);
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 30);
            card.setLayoutParams(lp);

            TextView rText = new TextView(this);
            rText.setText(role.toUpperCase());
            rText.setTextColor(getGradientOverlayText()); // 🚀 High Contrast Gradient Overlay
            rText.setTypeface(null, Typeface.BOLD);
            card.addView(rText);

            card.setOnClickListener(v -> {
                dialog.dismiss();
                executeChildJoinRequest(code, role);
            });
            container.addView(card);
        }

        sv.addView(container);
        dialog.setView(sv);
        
        // 🚀 QA Fix: Master Lifecycle Guard (Zero-Leak Policy)
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            applyDialogTheme(dialog);
        }
    }

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor()); // Grey/Dark contrast background

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        com.jdroidx.jfindme.ui.base.UiArchitect.applyHeader(this, header, "HEADER_MAIN");
        
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(this);
        title.setText("NETWORK GATEWAY");
        title.setTextColor(getGradientOverlayText());
        com.jdroidx.jfindme.ui.base.UiArchitect.applyText(this, title, "TEXT_HEADER");
        textBlock.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Device Pairing & Active Rosters");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.9f);
        com.jdroidx.jfindme.ui.base.UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        textBlock.addView(subtitle);
        header.addView(textBlock); // 🚀 CRITICAL RESTORATION: Title & Subtitle logic
        
        TextView helpBtn = new TextView(this);
        helpBtn.setText("?");
        helpBtn.setTextColor(getGradientOverlayText());
        helpBtn.setTextSize(com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.scaleText(com.jdroidx.jfindme.ui.base.UiArchitect.getStage(this), 30));
        helpBtn.setTypeface(null, Typeface.BOLD);
        helpBtn.setPadding(com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.scalePadding(com.jdroidx.jfindme.ui.base.UiArchitect.getStage(this), 30), 0, com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.scalePadding(com.jdroidx.jfindme.ui.base.UiArchitect.getStage(this), 30), 0);
        helpBtn.setGravity(Gravity.CENTER);
        helpBtn.setOnClickListener(v -> startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.settings.HelpSupportActivity.class)));
        
        // 🚀 QA Fix: Global Selection Feedback (Enter Key Support)
        helpBtn.setFocusable(true);
        helpBtn.setClickable(true);
        android.util.TypedValue outVal = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outVal, true);
        helpBtn.setBackgroundResource(outVal.resourceId);
        
        header.addView(helpBtn);
        mainRoot.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        // Padding is now managed by the Enterprise Matrix within Cards

        // --- LAYER 1: IDENTITY CARD (Owner Tile 1) ---
        LinearLayout identityCard = new LinearLayout(this);
        identityCard.setOrientation(LinearLayout.VERTICAL);
        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, identityCard, "CARD_STANDARD");
        identityCard.addView(buildIdentityBlock());
        identityCard.setFocusable(true);
        identityCard.setClickable(true);
        this.identityCardField = identityCard; // 🚀 Store for Re-hydration
        
        int quota = getResponsivePaddingQuota();
        int topGap = getResponsiveHeaderTopQuota();
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(quota, topGap, quota, 30);
        root.addView(identityCard, cardLp);

        // --- LAYER 2: INTERACTIVE OPTION BLOCKS (DYNAMICALLY ROUTED) ---
        optionGatewayPanel = buildGatewaySelectionLayout();
        remotePairingPanel = buildRemoteOptionLayout();
        localPairingPanel = buildLocalOptionLayout();

        // 🚀 L7 Architecture Sync: Unified Margin Matrix for all Gateway Selection Panels
        LinearLayout.LayoutParams standardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        standardLp.setMargins(quota, 0, quota, scale(30));

        root.addView(optionGatewayPanel, standardLp);
        root.addView(remotePairingPanel, standardLp);
        root.addView(localPairingPanel, standardLp);

        // --- LAYER 3: MASTER ROSTER WATERFALL (ORCHESTRATED) ---
        rosterCard = (LinearLayout) buildActiveRosterList(identityCard, root);
        root.addView(rosterCard);

        // --- LAYER 4: TECHNICAL FOOTER & RECOVERY ASSETS ---
        root.addView(buildTechnicalFooter());
        
        // Default State
        optionGatewayPanel.setVisibility(View.VISIBLE);
        remotePairingPanel.setVisibility(View.GONE);
        localPairingPanel.setVisibility(View.GONE);

        scroll.addView(root);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // --- LAYER 5: ARCHITECTURE STATUS BAR (Strategic Feature Audit) ---
        // Provides visual confirmation of the 5 pillars of the jFindMe Forensic Hardening protocol
        mainRoot.addView(buildArchitectureStatusBar());

        // Initial setup calls to ensure visibility states
        resetGateway();

        // 🛰️ ARCHITECTURAL LISTENER: Watch for Remote Identity Matrix Changes (The "WhatsApp" Factor)
        setupSignalMatrixListener();

        return mainRoot;
    }

    /**
     * Strategic Bottom Bar: Confirms Hardening Features via Tactical Icons
     * Matches the User Request for "Silent, Local Only, Manual, Single Fetch, Compliant"
     */
    private View buildArchitectureStatusBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        int bp = scale(15);
        bar.setPadding(scale(20), bp, scale(20), bp);
        bar.setBackgroundColor(Color.argb(80, Color.red(getPrimaryColor()), Color.green(getPrimaryColor()), Color.blue(getPrimaryColor()))); 
        
        // Feature 1: Silent Startup (🔇)
        bar.addView(createArchitectureIcon(android.R.drawable.ic_lock_silent_mode, "Silent", "Silent Startup: No automatic background sync on entry."));
        // Feature 2: Local Identity (📂)
        bar.addView(createArchitectureIcon(android.R.drawable.ic_menu_save, "Local", "Identity Hub: Primary data is retrieved from local secure storage."));
        // Feature 3: Manual Roster (🛠️)
        bar.addView(createArchitectureIcon(android.R.drawable.ic_popup_sync, "Manual", "Master Roster sync requires explicit user trigger via Refresh."));
        // Feature 4: Single Fetch (⏹️)
        bar.addView(createArchitectureIcon(android.R.drawable.ic_menu_recent_history, "Single", "Battery Saving: Node listeners set to single fetch mode."));
        // Feature 5: Accessibility (✅)
        bar.addView(createArchitectureIcon(android.R.drawable.checkbox_on_background, "Access", "Accessibility: Full Tab-navigation and Enter-key support active."));

        return bar;
    }

    private View createArchitectureIcon(int resId, String label, String toastMsg) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        int verticalPad = scale(5);
        box.setPadding(scale(25), verticalPad, scale(25), verticalPad);
        box.setFocusable(true);
        box.setClickable(true);
        box.setOnClickListener(v -> Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show());
        box.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });

        ImageView iv = new ImageView(this);
        iv.setImageResource(resId);
        iv.setColorFilter(getGradientStart());
        box.addView(iv, new LinearLayout.LayoutParams(scale(45), scale(45)));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(scaleText(8));
        tv.setTextColor(getTextColor());
        tv.setAlpha(0.6f);
        tv.setGravity(Gravity.CENTER);
        box.addView(tv);

        return box;
    }


    private View buildIdentityBlock() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(scale(25), scale(25), scale(25), scale(25)); 
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView myAvatar = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, myAvatar);
        
        // 🚀 Premium Circular Avatar (Responsive Scaling)
        int avatarSize = scale(168); 
        android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
        c.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        c.setColor(Color.TRANSPARENT);
        c.setStroke(scale(4), getGradientStart());
        myAvatar.setBackground(c);
        
        row.addView(myAvatar, new LinearLayout.LayoutParams(avatarSize, avatarSize));
        
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(scale(40), 0, 0, 0);
        
        // --- 1. OWNER NAME ---
        com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
        java.util.Map<String, String> myDos = db.getLocalDossier("OWNER_001");
        String nameStr = myDos != null ? myDos.get("full_name") : (prefs != null ? prefs.getString("user_nickname", "User Identity") : "User Identity");
        
        TextView myName = new TextView(this);
        myName.setText("👤 Owner: " + nameStr);
        myName.setTextColor(getTextColor());
        myName.setTextSize(scaleText(15));
        myName.setTypeface(null, Typeface.BOLD);
        textBlock.addView(myName);
        
        // Fetch Persistent UIDs
        android.content.SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String hardwareId = jPrefs.getString("hardware_device_id", null);
        if (hardwareId == null) {
            hardwareId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            jPrefs.edit().putString("hardware_device_id", hardwareId).apply();
        }
        String deviceId = jPrefs.getString("device_uid", null);
        if (deviceId == null) {
            deviceId = "UID-" + java.util.UUID.randomUUID().toString().substring(0, 7).toUpperCase();
            jPrefs.edit().putString("device_uid", deviceId).apply();
        }

        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        String safeUid = (myUid != null) ? myUid : "MOCK_U123";
        String upperFirebaseId = safeUid.toUpperCase();

        // --- 2. DEVICE UID ---
        TextView tvDeviceUid = new TextView(this);
        tvDeviceUid.setText("📱 Device ID: - " + deviceId);
        tvDeviceUid.setTextColor(getSecondaryTextColor());
        tvDeviceUid.setTextSize(10);
        textBlock.addView(tvDeviceUid);

        // --- 3. HARDWARE UID ---
        TextView tvHardwareUid = new TextView(this);
        tvHardwareUid.setText("⚙️ Hardware UID: - " + hardwareId);
        tvHardwareUid.setTextColor(getSecondaryTextColor());
        tvHardwareUid.setTextSize(10);
        textBlock.addView(tvHardwareUid);

        // --- 4. NETWORK ID ---
        TextView tvNetworkId = new TextView(this);
        tvNetworkId.setText("🌐 Network ID: - " + upperFirebaseId);
        tvNetworkId.setTextColor(getSecondaryTextColor());
        tvNetworkId.setTextSize(9);
        textBlock.addView(tvNetworkId);
        
        // 🛡️ ARCHITECTURAL SILENCE: Check local persistence for Hub status
        String localActiveGroup = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getString("active_group", null);
        if (localActiveGroup != null) {
            TextView tvStatus = new TextView(this);
            tvStatus.setText("LOCKED TO HUB: " + localActiveGroup);
            tvStatus.setTextColor(Color.parseColor("#00E676"));
            tvStatus.setTextSize(10);
            tvStatus.setTypeface(null, Typeface.BOLD);
            tvStatus.setPadding(0, 10, 0, 0);
            textBlock.addView(tvStatus);
        }
        
        row.addView(textBlock);
        return row;
    }


    private View buildActiveRosterList(View trigger, ViewGroup animationRoot) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        
        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        String safeUid = (myUid != null) ? myUid : "MOCK_U123";
        com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(GroupActivity.this);
        
        // 🚀 1. INDEPENDENT GROUP HEADER TILE (Stats Card)
        int rosterCount = db.getOfflineRoster().size() + 1;
        LinearLayout statsCard = new LinearLayout(this);
        statsCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(statsCard, getCardColor(), 25f);
        statsCard.setPadding(scale(40), scale(20), scale(40), scale(20));
        
        TextView tvStats = new TextView(this);
        tvStats.setText("TOTAL ACTIVE DEVICES IN GROUP: " + rosterCount);
        tvStats.setTextColor(Color.parseColor("#FF5722")); 
        tvStats.setTextSize(13f);
        tvStats.setTypeface(null, Typeface.BOLD);
        statsCard.addView(tvStats);
        
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.setMargins(scale(40), 10, scale(40), scale(30));
        container.addView(statsCard, headerLp);

        int famCount = 0;
        int friCount = 0;
        java.util.List<java.util.Map<String, Object>> liveData = db.getOfflineRoster();
        java.util.List<java.util.Map<String, Object>> famList = new java.util.ArrayList<>();
        java.util.List<java.util.Map<String, Object>> friList = new java.util.ArrayList<>();

        for (java.util.Map<String, Object> node : liveData) {
            Object fr = node.get("is_friend");
            boolean isF = false;
            if (fr instanceof Boolean) isF = (Boolean) fr;
            else if (fr instanceof Integer) isF = ((Integer) fr == 1);
            else if (fr instanceof String) isF = ((String) fr).equals("1") || ((String) fr).equalsIgnoreCase("true");
            
            if (isF) {
                friCount++;
                friList.add(node);
            } else {
                famCount++;
                famList.add(node);
            }
        }

        // 🚀 2. INDEPENDENT FAMILY NETWORK CONTAINER (Shadow Partitioned)
        LinearLayout famMainContainer = new LinearLayout(this);
        famMainContainer.setOrientation(LinearLayout.VERTICAL);
        famMainContainer.setVisibility(View.GONE); // Default Hidden (Waterfall)

        TextView famTitle = new TextView(this);
        famTitle.setText("FAMILY NETWORK (" + famCount + ")");
        famTitle.setTextColor(getGradientStart());
        famTitle.setTextSize(13f);
        famTitle.setTypeface(null, Typeface.BOLD);
        famTitle.setPadding(scale(40), scale(30), 0, scale(20));
        famMainContainer.addView(famTitle);

        famMainContainer.addView(buildForensicMemberStack(famList, getGradientStart()));
        
        LinearLayout.LayoutParams famLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        famLp.setMargins(0, 0, 0, scale(30));
        container.addView(famMainContainer, famLp);

        // 🚀 3. INDEPENDENT FRIENDS NETWORK CONTAINER (Shadow Partitioned)
        LinearLayout friMainContainer = new LinearLayout(this);
        friMainContainer.setOrientation(LinearLayout.VERTICAL);
        friMainContainer.setVisibility(View.GONE); // Default Hidden (Waterfall)

        TextView friTitle = new TextView(this);
        friTitle.setText("FRIENDS NETWORK (" + friCount + ")");
        friTitle.setTextColor(getGradientEnd());
        friTitle.setTextSize(13f);
        friTitle.setTypeface(null, Typeface.BOLD);
        friTitle.setPadding(scale(40), scale(10), 0, scale(20));
        friMainContainer.addView(friTitle);

        friMainContainer.addView(buildForensicMemberStack(friList, getGradientEnd()));
        
        LinearLayout.LayoutParams friLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        friLp.setMargins(0, 0, 0, scale(30));
        container.addView(friMainContainer, friLp);

        // 🏎️ Master Waterfall Controller: Owner Member Card -> Recursive Reveal
        trigger.setOnClickListener(v -> {
            android.transition.TransitionManager.beginDelayedTransition(animationRoot, new android.transition.AutoTransition().setDuration(400));
            int targetVis = (famMainContainer.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
            famMainContainer.setVisibility(targetVis);
            friMainContainer.setVisibility(targetVis);
            
            // Interaction Feedback (Subtitle Update)
            if (trigger instanceof ViewGroup) {
                View textLayer = ((ViewGroup) trigger).getChildAt(0);
                if (textLayer instanceof LinearLayout) {
                   for (int i=0; i<((LinearLayout)textLayer).getChildCount(); i++) {
                       View c = ((LinearLayout)textLayer).getChildAt(i);
                       if (c instanceof TextView && ((TextView)c).getText().toString().contains("TAP TO")) {
                           ((TextView)c).setText(targetVis == View.VISIBLE ? "SHRINK NETWORK MATRIX ▲" : "TAP TO EXPAND NETWORK MATRIX ▼");
                       }
                   }
                }
            }
        });

        // 🚀 Add refresh and linked assets (Nested inside container now for better orchestration)
        TextView refreshBtn = new TextView(this);
        refreshBtn.setText("🔄 FORCE CLOUD MATRIX REFRESH");
        refreshBtn.setTextColor(getSecondaryTextColor());
        refreshBtn.setTextSize(scaleText(9));
        refreshBtn.setAlpha(0.7f);
        refreshBtn.setPadding(scale(40), scale(20), scale(40), scale(20));
        refreshBtn.setClickable(true);
        refreshBtn.setFocusable(true);
        refreshBtn.setOnClickListener(v -> initiateCloudSync(safeUid, new LinearLayout(this), null));
        container.addView(refreshBtn);

        // 🛡️ GATHER LOCAL ROSTER (Legacy Partitioning)
        java.util.List<java.util.Map<String, Object>> localRoster = db.getOfflineRoster();
        if (!localRoster.isEmpty()) {
            LinearLayout branchList = new LinearLayout(this);
            branchList.setOrientation(LinearLayout.VERTICAL);
            branchList.setPadding(scale(40), 0, scale(40), 0);
            renderLocalRoster(safeUid, branchList, null, localRoster);
            container.addView(branchList);
        }

        return container;
    }

    private void initiateCloudSync(String safeUid, LinearLayout branchList, TextView header) {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(safeUid).child("active_group").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot myGroupSnap) {
                com.jdroidx.jfindme.utils.AngelLoader.show(GroupActivity.this);
                com.jdroidx.jfindme.utils.AngelLoader.setStatus(GroupActivity.this, "Connecting to Cloud Hub...");

                String myGroupId = myGroupSnap.getValue(String.class);
                StringBuilder authorizedUids = new StringBuilder(safeUid).append(",");
                
                if (myGroupId == null || myGroupId.isEmpty()) {
                    fetchFriendMatrix(safeUid, branchList, authorizedUids, header, 1);
                    com.jdroidx.jfindme.utils.AngelLoader.hide(GroupActivity.this);
                    return;
                }

                com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").orderByChild("active_group").equalTo(myGroupId)
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                            fetchFriendMatrix(safeUid, branchList, authorizedUids, header, snapshot.getChildrenCount());
                            com.jdroidx.jfindme.utils.AngelLoader.hide(GroupActivity.this);
                        }
                        @Override
                        public void onCancelled(com.google.firebase.database.DatabaseError error) {
                            com.jdroidx.jfindme.utils.AngelLoader.hide(GroupActivity.this);
                        }
                    });
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    private View createGroupHeading(String title) {
        android.widget.CheckBox cb = new android.widget.CheckBox(this);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint()));
        cb.setText(title.toUpperCase());
        cb.setTextColor(getTextColor());
        cb.setChecked(true);
        cb.setClickable(true);
        cb.setFocusable(true);
        cb.setPadding(scale(40), scale(20), scale(40), scale(10));
        
        android.widget.LinearLayout.LayoutParams cbLp = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        cbLp.setMargins(0, scale(60), 0, scale(10)); 
        cb.setLayoutParams(cbLp);
        
        com.jdroidx.jfindme.ui.base.UiArchitect.applyText(this, cb, "TEXT_TITLE");
        
        // QA Focus Mapping: Tab/Enter sequence
        cb.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == android.view.KeyEvent.ACTION_UP && (k == android.view.KeyEvent.KEYCODE_ENTER || k == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                cb.setChecked(!cb.isChecked());
                return true;
            }
            return false;
        });

        return cb;
    }
    
    private LinearLayout buildTreeNode(String uid, String defaultName, boolean isRoot, boolean hasChildren, View payloadToExpand, String roleTag) {
        LinearLayout node = new LinearLayout(this);
        node.setOrientation(LinearLayout.HORIZONTAL);
        node.setGravity(Gravity.CENTER_VERTICAL);
        node.setFocusable(true);
        node.setClickable(true);

        // QA Focus Mapping: Handle Enter Key for all devices!
        node.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == android.view.KeyEvent.ACTION_UP && (k == android.view.KeyEvent.KEYCODE_ENTER || k == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(getCardColor());
        gd.setCornerRadius(25f);
        node.setBackground(gd);
        node.setElevation(8f);
        
        LinearLayout.LayoutParams nodeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nodeLp.setMargins(isRoot ? 0 : scale(30), 0, 0, scale(30)); 
        node.setLayoutParams(nodeLp);
        node.setPadding(scale(30), scale(20), scale(30), scale(20)); 
        
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        avatar.setPadding(scale(10), scale(10), scale(10), scale(10));
        
        android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
        c.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        c.setCornerRadius(scale(60)); 
        c.setColor(Color.TRANSPARENT);
        c.setStroke(scale(4), getGradientStart());
        avatar.setBackground(c);
        
        int avatarSize = com.jdroidx.jfindme.ui.base.UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        node.addView(avatar, new LinearLayout.LayoutParams(avatarSize, avatarSize)); 
        
        LinearLayout textData = new LinearLayout(this);
        textData.setOrientation(LinearLayout.VERTICAL);
        textData.setPadding(scale(25), 0, 0, 0); 
        
        TextView nameView = new TextView(this);
        nameView.setText(defaultName);
        nameView.setTextColor(isRoot ? getGradientStart() : getTextColor()); 
        nameView.setTypeface(null, Typeface.BOLD);
        textData.addView(nameView);
        
        TextView idView = new TextView(this);
        idView.setText(roleTag != null ? roleTag : (isRoot ? "TAP TO EXPAND/COLLAPSE GROUP ▲▼" : "Encrypted Network Node"));
        idView.setTextColor(getSecondaryTextColor());
        idView.setTextSize(scaleText(10));
        textData.addView(idView);
        
        node.addView(textData, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        if (!isRoot) {
            // 🚀 TIGHTLY COUPLED FORENSIC ACTION HUB
            LinearLayout actionHub = new LinearLayout(this);
            actionHub.setOrientation(LinearLayout.HORIZONTAL);
            actionHub.setGravity(Gravity.CENTER_VERTICAL);

            // 🚀 IDENTITY OWNER ICON (Manual Dossier Change)
            ImageView ownerIcon = new ImageView(this);
            ownerIcon.setImageResource(android.R.drawable.ic_menu_info_details);
            ownerIcon.setColorFilter(getGradientStart(), android.graphics.PorterDuff.Mode.SRC_IN);
            ownerIcon.setPadding(scale(10), scale(10), 0, scale(10));
            int iconSize = com.jdroidx.jfindme.ui.base.UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
            actionHub.addView(ownerIcon, new LinearLayout.LayoutParams(iconSize, iconSize));

            // 🚀 RELATIONSHIP REASSIGNMENT ICON (Waterfall Select)
            ImageView groupIcon = new ImageView(this);
            groupIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
            groupIcon.setColorFilter(getGradientEnd(), android.graphics.PorterDuff.Mode.SRC_IN);
            groupIcon.setPadding(scale(5), scale(10), scale(10), scale(10));
            groupIcon.setFocusable(true);
            groupIcon.setClickable(true);
            
            groupIcon.setOnClickListener(v -> {
                final String[] categories = {"Family", "Friends"};
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Select Hierarchy Category")
                    .setItems(categories, (d1, cIdx) -> {
                        String category = categories[cIdx];
                        if (category.equals("Friends")) {
                            executeForensicRoleChange(uid, "Friends", true);
                        } else {
                            // 🚀 WATERFALL LEVEL 2: ROLE SELECT
                            final String[] famRoles = {"Mother", "Father", "Child", "Spouse", "Guardian"};
                            new android.app.AlertDialog.Builder(this)
                                .setTitle("Select Family Identity")
                                .setItems(famRoles, (d2, rIdx) -> {
                                    String role = famRoles[rIdx];
                                    if (role.equals("Child")) {
                                        // 🚀 WATERFALL LEVEL 3: GENDER SPECIFIC
                                        final String[] genders = {"Son", "Daughter"};
                                        new android.app.AlertDialog.Builder(this)
                                            .setTitle("Select Child Detail")
                                            .setItems(genders, (d3, gIdx) -> {
                                                executeForensicRoleChange(uid, "Child (" + genders[gIdx] + ")", false);
                                            }).show();
                                    } else {
                                        executeForensicRoleChange(uid, role, false);
                                    }
                                }).show();
                        }
                    }).show();
            });
            actionHub.addView(groupIcon, new LinearLayout.LayoutParams((int)(iconSize * 1.25f), iconSize));
            node.addView(actionHub);

            ImageView deleteIcon = new ImageView(this);
            deleteIcon.setImageResource(android.R.drawable.ic_menu_delete);
            deleteIcon.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN); 
            deleteIcon.setPadding(scale(10), scale(10), scale(10), scale(10)); 
            
            deleteIcon.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Eject Target Node?")
                    .setMessage("WARNING: This will permanently purge (" + uid + ") from your Secure Tracking Matrix!")
                    .setPositiveButton("PURGE", (d, w) -> {
                        String myGrp = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getString("active_group", null);
                        if (myGrp != null) {
                            com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(myGrp).child("members").child(uid).removeValue();
                            com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(uid).child("active_group").removeValue();
                            rebuildActiveRosterList();
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
            });
            
            node.addView(deleteIcon, new LinearLayout.LayoutParams(iconSize, iconSize)); 
        }
        
        // Accordion Interaction
        if (hasChildren && payloadToExpand != null) {
            node.setOnClickListener(v -> {
                payloadToExpand.setVisibility(payloadToExpand.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });
        } else if (!isRoot) {
            // Nested recursion illusion if requested
            node.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Fetching Sub-Connections for " + uid, android.widget.Toast.LENGTH_SHORT).show();
            });
        }
        
        // Live Profile Data Hydration for this specific Avatar!
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("locations").child(uid)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot locSnap) {
                        String realName = locSnap.child("userName").getValue(String.class);
                        String finalNameRaw = (realName != null && !realName.trim().isEmpty()) ? realName : defaultName;
                        nameView.setText(finalNameRaw);
                        
                        // 🚀 QA Fix: Hybrid Role Priority (V9.9)
                        String cloudFamRole = locSnap.child("role_family").getValue(String.class);
                        String cloudFriendRole = locSnap.child("role_friend").getValue(String.class);
                        String activeTag = "Syncing";

                        if (cloudFriendRole != null && cloudFriendRole.contains("Friend")) {
                            activeTag = "FRIEND";
                        } else if (cloudFamRole != null && !cloudFamRole.equals("None")) {
                            activeTag = cloudFamRole.toUpperCase();
                        } else {
                            // Local Database Fallback (V2.0 Engine)
                            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(GroupActivity.this);
                            java.util.Map<String, String> localDos = db.getLocalDossier(uid);
                            if (localDos != null) {
                                String fRole = localDos.get("role_family");
                                String frRole = localDos.get("role_friend");
                                if (fRole != null && !fRole.equals("None")) activeTag = fRole.toUpperCase();
                                if (frRole != null && frRole.contains("Friend")) activeTag = "FRIEND";
                            }
                        }
                        nameView.setText(finalNameRaw + " (" + activeTag + ")");
                        
                        // 🚀 QA Fix: Live Network Telemetry Subtitle (V9.6)
                        Long timestamp = locSnap.child("timestamp").getValue(Long.class);
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
                        String timeStr = (timestamp != null) ? sdf.format(new java.util.Date(timestamp)) : "Offline";

                        if (timestamp != null) {
                            long diff = System.currentTimeMillis() - timestamp;
                            boolean isOnline = diff < 120000;
                            
                            if (isOnline) {
                                idView.setText("🟢 ONLINE NOW @ " + timeStr);
                                idView.setTextColor(Color.parseColor("#00E676"));
                            } else {
                                idView.setText("⚪ LAST PULSE @ " + timeStr);
                                idView.setTextColor(Color.GRAY);
                            }
                        } else {
                            idView.setText("⚪ DISCONNECTED / NO SIGNAL");
                            idView.setTextColor(Color.LTGRAY);
                        }

                        // Remove legacy indicator if present from previous loops
                        View legacyIndicator = node.findViewWithTag("online_dot_" + uid);
                        if (legacyIndicator != null) node.removeView(legacyIndicator);

                        // 🚀 Tactile Interaction: Clicking name opens Location Lock Screen!
                        View.OnClickListener mapLock = v -> {
                            android.content.Intent mapIntent = new android.content.Intent(GroupActivity.this, com.jdroidx.jfindme.ui.map.MapActivity.class);
                            mapIntent.putExtra("target_uid", uid);
                            startActivity(mapIntent);
                        };
                        nameView.setOnClickListener(mapLock);
                        idView.setOnClickListener(mapLock);
                        
                        // 🚀 QA Fix: Delete legacy '.vrm' restrictions. Fully render the Graphic locally!
                        String avatarId = locSnap.child("avatar_id").getValue(String.class);
                        android.graphics.Bitmap dynamicBmp = null;
                        if (avatarId != null && !avatarId.isEmpty()) {
                            int resId = getResources().getIdentifier(avatarId, "drawable", getPackageName());
                            if (resId != 0) {
                                dynamicBmp = android.graphics.BitmapFactory.decodeResource(getResources(), resId);
                            }
                        }

                        if (dynamicBmp != null) {
                            avatar.setImageBitmap(dynamicBmp);
                        } else {
                            avatar.setImageResource(android.R.drawable.ic_menu_myplaces);
                            avatar.setColorFilter(getGradientStart()); // Applies dynamic hardware overlay
                        }
                }
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
            
        return node;
    }

    /**
     * Helper to build high-fidelity Gateway Cards as seen in Image 1.
     */
    private View buildGatewayCard(String title, String subtitle, int iconRes, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int padding = scale(25);
        card.setPadding(padding, padding, padding, padding);
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(listener);

        // QA Focus Mapping: Handle Enter Key for all devices!
        card.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == android.view.KeyEvent.ACTION_UP && (k == android.view.KeyEvent.KEYCODE_ENTER || k == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });

        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, card, "CARD_STANDARD");

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart()); // Synergetic Theme Color
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int iSize = scale(120);
        card.addView(icon, new LinearLayout.LayoutParams(iSize, iSize));

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(scale(40), 0, 0, 0);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getTextColor());
        tvTitle.setTextSize(scaleText(18));
        tvTitle.setTypeface(null, Typeface.BOLD);
        textLayout.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(getSecondaryTextColor());
        tvSub.setTextSize(scaleText(12));
        tvSub.setLineSpacing(0, 1.1f);
        textLayout.addView(tvSub);

        card.addView(textLayout, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        return card;
    }

    /**
     * Image 1 - Main Gateway Selection UI
     */
    private LinearLayout buildGatewaySelectionLayout() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(30));

        // OPTION A [Remote]
        container.addView(buildGatewayCard("☁️ OPTION A [Remote]", 
            "Firebase Pairing + QR Codes\nWorks globally over Cellular Data.", 
            android.R.drawable.ic_menu_myplaces, v -> {
                optionGatewayPanel.setVisibility(View.GONE);
                remotePairingPanel.setVisibility(View.VISIBLE);
            }), lp);

        // OPTION B [Nearby]
        container.addView(buildGatewayCard("🛰️ OPTION B [Nearby]", 
            "Local Pairing (No Internet)\nWorks via Bluetooth & Wi-Fi Direct.", 
            android.R.drawable.ic_menu_mylocation, v -> {
                optionGatewayPanel.setVisibility(View.GONE);
                localPairingPanel.setVisibility(View.VISIBLE);
            }), lp);

        // Section Divider removed: Logic moved to Footer
        // RESTORE LOST ROSTER ASSETS logic migrated to separate buildTechnicalFooter() method per user request

        return container;
    }

    /**
     * Image 2 - Option A / Remote Pairing Interface
     */
    private LinearLayout buildRemoteOptionLayout() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        
        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, container, "CARD_DASHBOARD");
        
        // --- HOST SECTION ---
        TextView hostTitle = new TextView(this);
        hostTitle.setText("HOST A FAMILY SESSION");
        hostTitle.setTextColor(getGradientStart());
        hostTitle.setTextSize(12);
        hostTitle.setTypeface(null, Typeface.BOLD);
        hostTitle.setGravity(Gravity.CENTER);
        hostTitle.setPadding(0, scale(20), 0, scale(40));
        container.addView(hostTitle);

        tvCode = new TextView(this);
        tvCode.setText("------");
        tvCode.setTextColor(getTextColor());
        tvCode.setTextSize(42);
        tvCode.setGravity(Gravity.CENTER);
        tvCode.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        container.addView(tvCode);

        tvSyncStatus = new TextView(this);
        tvSyncStatus.setText("READY");
        tvSyncStatus.setTextColor(Color.parseColor("#00E676")); // Green
        tvSyncStatus.setGravity(Gravity.CENTER);
        tvSyncStatus.setTextSize(12);
        tvSyncStatus.setPadding(0, 20, 0, 30);
        container.addView(tvSyncStatus);

        ivQrCode = new ImageView(this);
        ivQrCode.setPadding(scale(20), scale(20), scale(20), scale(20));
        ivQrCode.setBackgroundColor(Color.WHITE);
        ivQrCode.setVisibility(View.GONE);
        int qrSize = scale(400);
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(qrSize, qrSize);
        qrLp.gravity = Gravity.CENTER;
        qrLp.setMargins(0, 0, 0, scale(30));
        container.addView(ivQrCode, qrLp);
        
        // Action Button: Generate Token
        View btnGenerate = createActionTile("GENERATE UNIQUE TOKEN", android.R.drawable.ic_popup_sync, v -> {
            executeHostSessionSpinup("AWAITING_ROLE");
        });
        container.addView(btnGenerate);

        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(80, Color.red(getSecondaryTextColor()), Color.green(getSecondaryTextColor()), Color.blue(getSecondaryTextColor())));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        divLp.setMargins(0, 40, 0, 40);
        container.addView(divider, divLp);

        // --- JOIN SECTION ---
        TextView joinTitle = new TextView(this);
        joinTitle.setText("JOIN A FAMILY SESSION");
        joinTitle.setTextColor(getGradientEnd()); 
        joinTitle.setTextSize(12);
        joinTitle.setTypeface(null, Typeface.BOLD);
        joinTitle.setGravity(Gravity.CENTER);
        joinTitle.setPadding(0, 0, 0, 20);
        container.addView(joinTitle);

        TextView enterCodeLabel = new TextView(this);
        enterCodeLabel.setText("ENTER 6-DIGIT CODE");
        enterCodeLabel.setTextColor(getSecondaryTextColor());
        enterCodeLabel.setTextSize(scaleText(22));
        enterCodeLabel.setGravity(Gravity.CENTER);
        enterCodeLabel.setPadding(0, 0, 0, scale(20));
        container.addView(enterCodeLabel);

        etCode = com.jdroidx.jfindme.ui.base.UiArchitect.createInputField(this, "FIELD_CODE");
        container.addView(etCode);
        
        View btnManual = createActionTile("LINK MANUALLY", android.R.drawable.ic_menu_edit, v -> {
            showRoleSelectionDialogBeforeJoin(etCode.getText().toString());
        });
        container.addView(btnManual);

        View btnScan = createActionTile("SCAN QR MATRIX", android.R.drawable.ic_menu_camera, v -> {
            isScanningLocal = false;
            barcodeLauncher.launch(new ScanOptions()
                .setOrientationLocked(false)
                .setBeepEnabled(true)
                .setPrompt("Align Target QR Securely within the Frame"));
        });
        container.addView(btnScan);

        container.addView(createSectionDivider());

        View btnBack = createActionTile("BACK TO GATEWAY", android.R.drawable.ic_menu_revert, v -> resetGateway());
        container.addView(btnBack);

        return container;
    }

    /**
     * Helper to create the rounded action tiles with icons as seen in Image 2.
     */
    private View createActionTile(String label, int iconRes, View.OnClickListener listener) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(scale(40), scale(30), scale(40), scale(30));
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setOnClickListener(listener);

        // Accessibility: Tab/Enter
        tile.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == android.view.KeyEvent.ACTION_UP && (k == android.view.KeyEvent.KEYCODE_ENTER || k == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });

        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, tile, "CARD_STANDARD");
        LinearLayout.LayoutParams tileLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tileLp.setMargins(0, scale(10), 0, scale(10));
        tile.setLayoutParams(tileLp);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientEnd());
        int iconSize = com.jdroidx.jfindme.ui.base.UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
        tile.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView tv = new TextView(this);
        tv.setText(label.toUpperCase());
        tv.setTextColor(getTextColor());
        tv.setTextSize(scaleText(14));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(scale(40), 0, 0, 0);
        tile.addView(tv);

        return tile;
    }

    /**
     * Image 2 - Option B / Local Pairing Interface (Mirrors Remote Option A)
     */
    private LinearLayout buildLocalOptionLayout() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, container, "CARD_DASHBOARD");

        TextView hostTitle = new TextView(this);
        hostTitle.setText("HOST A LOCAL SESSION");
        hostTitle.setTextColor(getGradientStart());
        hostTitle.setTextSize(12);
        hostTitle.setTypeface(null, Typeface.BOLD);
        hostTitle.setGravity(Gravity.CENTER);
        hostTitle.setPadding(0, scale(20), 0, scale(40));
        container.addView(hostTitle);

        tvLocalCode = new TextView(this);
        tvLocalCode.setText("--------");
        tvLocalCode.setTextColor(getTextColor());
        tvLocalCode.setTextSize(42);
        tvLocalCode.setGravity(Gravity.CENTER);
        tvLocalCode.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        container.addView(tvLocalCode);

        tvLocalSyncStatus = new TextView(this);
        tvLocalSyncStatus.setText("READY");
        tvLocalSyncStatus.setTextColor(Color.parseColor("#00E676"));
        tvLocalSyncStatus.setGravity(Gravity.CENTER);
        tvLocalSyncStatus.setTextSize(12);
        tvLocalSyncStatus.setPadding(0, scale(20), 0, scale(30));
        container.addView(tvLocalSyncStatus);

        ivLocalQrCode = new ImageView(this);
        ivLocalQrCode.setPadding(scale(20), scale(20), scale(20), scale(20));
        ivLocalQrCode.setBackgroundColor(Color.WHITE);
        ivLocalQrCode.setVisibility(View.GONE);
        int qrSize = scale(400);
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(qrSize, qrSize);
        qrLp.gravity = Gravity.CENTER;
        qrLp.setMargins(0, 0, 0, scale(30));
        container.addView(ivLocalQrCode, qrLp);

        View btnGenerate = createActionTile("GENERATE UNIQUE TOKEN", android.R.drawable.ic_popup_sync, v -> {
            tvLocalCode.setText("GENERATING...");
            ivLocalQrCode.setVisibility(View.GONE);
            executeLocalHostSessionSpinup();
        });
        container.addView(btnGenerate);

        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(80, Color.red(getSecondaryTextColor()), Color.green(getSecondaryTextColor()), Color.blue(getSecondaryTextColor())));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        divLp.setMargins(0, 40, 0, 40);
        container.addView(divider, divLp);

        TextView enterCodeLabel = new TextView(this);
        enterCodeLabel.setText("ENTER 8-DIGIT LOCAL CODE");
        enterCodeLabel.setTextColor(getSecondaryTextColor());
        enterCodeLabel.setTextSize(22);
        enterCodeLabel.setGravity(Gravity.CENTER);
        enterCodeLabel.setPadding(0, 0, 0, 20);
        container.addView(enterCodeLabel);

        etLocalCode = com.jdroidx.jfindme.ui.base.UiArchitect.createInputField(this, "FIELD_CODE");
        container.addView(etLocalCode);

        View btnManual = createActionTile("LINK MANUALLY", android.R.drawable.ic_menu_edit, v -> {
            executeLocalJoinRequest(etLocalCode.getText().toString());
        });
        container.addView(btnManual);

        View btnScan = createActionTile("SCAN QR MATRIX", android.R.drawable.ic_menu_camera, v -> {
            isScanningLocal = true;
            barcodeLauncher.launch(new ScanOptions()
                .setOrientationLocked(false)
                .setBeepEnabled(true)
                .setPrompt("Align Local Node QR Securely within the Frame"));
        });
        container.addView(btnScan);

        container.addView(createSectionDivider());

        View btnBack = createActionTile("BACK TO GATEWAY", android.R.drawable.ic_menu_revert, v -> resetGateway());
        container.addView(btnBack);

        return container;
    }

    private void executeLocalHostSessionSpinup() {
        LocalPairingManager.INSTANCE.startLocalHostSession(this, localToken -> {
            runOnUiThread(() -> {
                tvLocalCode.setText(localToken);
                tvLocalSyncStatus.setText("BROADCASTING P2P TOKEN...");
                generateQrCode(localToken, ivLocalQrCode);
            });
            return kotlin.Unit.INSTANCE;
        });
    }

    private void executeLocalJoinRequest(String inputMatrix) {
        String cleaned = inputMatrix.trim().toUpperCase();
        if(cleaned.length() != 8) {
            Toast.makeText(this, "Local Offline Codes must be exactly 8 characters.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Scanning Local Radio Channels: " + cleaned, Toast.LENGTH_SHORT).show();
        LocalPairingManager.INSTANCE.startLocalDiscoveryBinding(cleaned, this, hostId -> {
            runOnUiThread(() -> promptApprovalDialog(hostId, "Nearby Target", true));
            return kotlin.Unit.INSTANCE;
        });
    }

    // 📡 --- ARCHITECTURE EVENT HANDLERS ---

    private void executeHostSessionSpinup(String assignedRoles) {
        tvCode.setText("NEGOTIATING...");
        tvSyncStatus.setText("ALLOCATING CLOUD NODE...");
        tvSyncStatus.setTextColor(Color.YELLOW);
        ivQrCode.setVisibility(View.GONE);

        FirebasePairingManager.INSTANCE.startHostPairingSession(this, assignedRoles, (qrBitmap, code) -> {
            runOnUiThread(() -> {
                if (code != null && !code.isEmpty()) {
                    tvCode.setText(code);
                    tvSyncStatus.setText("LISTENING FOR PAIRED DEVICES");
                    if (qrBitmap != null) {
                        ivQrCode.setImageBitmap(qrBitmap);
                        ivQrCode.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvCode.setText("NETWORK ERROR");
                    tvSyncStatus.setText("FIREBASE REJECTED");
                    tvSyncStatus.setTextColor(Color.RED);
                }
            });
            return kotlin.Unit.INSTANCE;
        }, (childDeviceId, childName) -> {
            // CRITICAL HANDSHAKE! Parent intercepted a child entering the code!
            runOnUiThread(() -> promptApprovalDialog(childDeviceId, childName, false));
            return kotlin.Unit.INSTANCE;
        });
    }

    private void executeChildJoinRequest(String inputMatrix, String selectedRole) {
        String cleaned = inputMatrix.trim().toUpperCase();
        if(cleaned.length() != 6) {
            Toast.makeText(this, "Invalid code length.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Injecting Handshake Identity [" + selectedRole + "]...", Toast.LENGTH_SHORT).show();
        
        FirebasePairingManager.INSTANCE.requestJoinGroup(GroupActivity.this, cleaned, selectedRole, (success, message) -> {
            runOnUiThread(() -> {
                if (success && "FULLY_SYNCED".equals(message)) {
                    Toast.makeText(this, "Network Sync Settled! ✅ Locking Assets...", Toast.LENGTH_LONG).show();
                    getWindow().getDecorView().postDelayed(this::finish, 2000);
                } else if (success && "PENDING_HANDSHAKE".equals(message)) {
                    Toast.makeText(this, "Handshake Received. Waiting for Host to Physically Approve...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, message, success ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
                }
            });
            return kotlin.Unit.INSTANCE;
        });
    }
    private void promptApprovalDialog(String childId, String childName, boolean isLocalMerge) {
        final String activeCode = FirebasePairingManager.INSTANCE.getActivePairingCode();
        
        if (activeCode != null && !isLocalMerge) {
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("pairing_requests").child(activeCode)
                .get().addOnSuccessListener(snap -> {
                    String prelimRole = snap.child("child_role_preliminary").getValue(String.class);
                    if (prelimRole == null) prelimRole = "Friends";
                    
                    final String finalRole = prelimRole;
                    runOnUiThread(() -> renderHostApprovalDialog(childId, childName, finalRole, activeCode));
                });
        }
    }

    private void renderHostApprovalDialog(String childId, String childName, String childPrelimRole, String activeCode) {
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 40, 40, 40);

        TextView t1 = new TextView(this);
        t1.setText("HANDSHAKE REQUEST");
        t1.setTextColor(getTextColor());
        t1.setTextSize(20);
        t1.setTypeface(null, Typeface.BOLD);
        container.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText("Target (" + childName + ") identifies as: " + childPrelimRole);
        t2.setTextColor(getSecondaryTextColor());
        t2.setPadding(0, 10, 0, 40);
        container.addView(t2);

        java.util.List<String> validHostRoles = new java.util.ArrayList<>();
        if (childPrelimRole.equals("Friends")) validHostRoles.add("Friends");
        else if (childPrelimRole.equals("Spouse")) validHostRoles.add("Spouse");
        else if (childPrelimRole.equals("Child")) {
            validHostRoles.add("Father");
            validHostRoles.add("Mother");
            validHostRoles.add("Guardian");
        } else if (java.util.Arrays.asList("Father", "Mother", "Guardian").contains(childPrelimRole)) {
            validHostRoles.add("Child");
        }

        AlertDialog dialog = new AlertDialog.Builder(this).create();

        for (String hostRole : validHostRoles) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(50, 40, 50, 40);
            applySolidCardGeometry(card, getCardColor(), 25f);
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 30);
            card.setLayoutParams(lp);

            TextView rText = new TextView(this);
            rText.setText("ACCEPT AS: " + hostRole.toUpperCase());
            rText.setTextColor(getGradientStart());
            rText.setTypeface(null, Typeface.BOLD);
            card.addView(rText);

            card.setOnClickListener(v -> {
                dialog.dismiss();
                FirebasePairingManager.INSTANCE.approveHandshake(GroupActivity.this, activeCode, childId, hostRole, childPrelimRole, success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(this, "Handshake Completed! Assets Linked.", Toast.LENGTH_SHORT).show();
                            boolean isFriend = childPrelimRole.equals("Friends");
                            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).updateCitizenRole(childId, isFriend ? "None" : childPrelimRole, isFriend ? "Friends" : "None", isFriend);
                        } else {
                            Toast.makeText(this, "Handshake Failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return kotlin.Unit.INSTANCE;
                });
            });
            container.addView(card);
        }

        TextView btnReject = new TextView(this);
        btnReject.setText("REJECT REQUEST");
        btnReject.setTextColor(Color.RED);
        btnReject.setGravity(Gravity.CENTER);
        btnReject.setPadding(0, 20, 0, 20);
        btnReject.setClickable(true);
        btnReject.setOnClickListener(v -> {
            dialog.dismiss();
            FirebasePairingManager.INSTANCE.rejectHandshake(activeCode);
        });
        container.addView(btnReject);

        sv.addView(container);
        dialog.setView(sv);
        dialog.setCancelable(false);
        
        // 🚀 QA Fix: Master Lifecycle Guard (Zero-Leak Policy)
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            applyDialogTheme(dialog);
        }
    }

    // 🎨 --- GUI UTILITIES ---

    private void showRestoreLostRosterAssetsDialog() {
        android.widget.Toast.makeText(this, "Scanning History Database for Legacy Connections...", android.widget.Toast.LENGTH_SHORT).show();
        
        final String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        final String safeUid = (myUid != null) ? myUid : "U_MOCK";

        com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(safeUid).child("active_group").get().addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.database.DataSnapshot>() {
            @Override
            public void onSuccess(com.google.firebase.database.DataSnapshot grpSnap) {
                String myGrp = grpSnap.getValue(String.class);
                final String finalGrp = (myGrp == null) ? "" : myGrp;
                if (finalGrp.isEmpty()) {
                    android.widget.Toast.makeText(GroupActivity.this, "Network Hub is empty. You must build a new Matrix.", android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                
                final java.util.List<String> currentlyPairedUids = new java.util.ArrayList<>();

                java.util.function.Consumer<Void> executeAssetScan = new java.util.function.Consumer<Void>() {
                    @Override
                    public void accept(Void v) {
                        com.jdroidx.jfindme.data.remote.FirebaseComm.INSTANCE.getLocationsRef().get().addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.database.DataSnapshot>() {
                            @Override
                            public void onSuccess(com.google.firebase.database.DataSnapshot snapshot) {
                                final java.util.List<String> assetNames = new java.util.ArrayList<>();
                                final java.util.List<String> assetUids = new java.util.ArrayList<>();
                                
                                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                                    com.jdroidx.jfindme.data.model.UserLocation loc = child.getValue(com.jdroidx.jfindme.data.model.UserLocation.class);
                                    if (loc != null && !loc.getUserId().equals(safeUid)) {
                                        if (!assetUids.contains(loc.getUserId()) && currentlyPairedUids.contains(loc.getUserId())) {
                                            assetNames.add(loc.getUserName());
                                            assetUids.add(loc.getUserId());
                                        }
                                    }
                                }
                                
                                if (assetNames.isEmpty()) {
                                    android.widget.Toast.makeText(GroupActivity.this, "No legacy assets detected in History Database.", android.widget.Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                final String[] items = assetNames.toArray(new String[0]);
                                final boolean[] checkedItems = new boolean[items.length];
                                for (int i=0; i < items.length; i++) {
                                    if (currentlyPairedUids.contains(assetUids.get(i))) checkedItems[i] = true;
                                }
                                
                                new android.app.AlertDialog.Builder(GroupActivity.this)
                                    .setTitle("Select Assets to Recover")
                                    .setMultiChoiceItems(items, checkedItems, new android.content.DialogInterface.OnMultiChoiceClickListener() {
                                        @Override
                                        public void onClick(android.content.DialogInterface dialogEx, int which, boolean isChecked) {
                                            checkedItems[which] = isChecked;
                                        }
                                    })
                                    .setPositiveButton("RESTORE", new android.content.DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(android.content.DialogInterface dialogEx, int which) {
                                            String targetGroup = finalGrp;
                                            if (targetGroup.isEmpty()) {
                                                targetGroup = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("groups").push().getKey();
                                                com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(safeUid).child("active_group").setValue(targetGroup);
                                                com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(targetGroup).child("members").child(safeUid).setValue(true);
                                                getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).edit().putString("active_group", targetGroup).apply();
                                            }
                                            
                                            int restoredCount = 0;
                                            for (int i=0; i < checkedItems.length; i++) {
                                                if (checkedItems[i] && !currentlyPairedUids.contains(assetUids.get(i))) {
                                                    String targetNode = assetUids.get(i);
                                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(targetGroup).child("members").child(targetNode).setValue(true);
                                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(targetNode).child("active_group").setValue(targetGroup);
                                                    restoredCount++;
                                                } else if (!checkedItems[i] && currentlyPairedUids.contains(assetUids.get(i))) {
                                                    String targetNode = assetUids.get(i);
                                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(targetGroup).child("members").child(targetNode).removeValue();
                                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(targetNode).child("active_group").removeValue();
                                                }
                                            }
                                            android.widget.Toast.makeText(GroupActivity.this, "Successfully Synchronized Array with " + restoredCount + " new assets!", android.widget.Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .setNegativeButton("CLOSE", null)
                                    .show();
                            }
                        });
                    }
                };

                if (!finalGrp.isEmpty()) {
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(finalGrp).child("members").get().addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.database.DataSnapshot>() {
                        @Override
                        public void onSuccess(com.google.firebase.database.DataSnapshot memSnap) {
                            if (memSnap.exists()) {
                                for (com.google.firebase.database.DataSnapshot m : memSnap.getChildren()) {
                                    currentlyPairedUids.add(m.getKey());
                                }
                            }
                            executeAssetScan.accept(null);
                        }
                    });
                } else {
                    executeAssetScan.accept(null);
                }
            }
        });
    }


    private void resetGateway() {
        optionGatewayPanel.setVisibility(View.VISIBLE);
        remotePairingPanel.setVisibility(View.GONE);
        localPairingPanel.setVisibility(View.GONE);
    }

    private View createBigTouchButton(String title, String desc, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(40, 40, 40, 40);
        applySolidCardGeometry(row, getCardColor(), 30f); // Make buttons native 3D Floating White Cards!
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 40);
        row.setLayoutParams(p);

        TextView tText = new TextView(this);
        tText.setText(title);
        tText.setTextColor(getTextColor());
        tText.setTextSize(14);
        tText.setTypeface(null, Typeface.BOLD);
        row.addView(tText);

        TextView dText = new TextView(this);
        dText.setText(desc);
        dText.setTextColor(getSecondaryTextColor());
        dText.setTextSize(11);
        dText.setPadding(0, 10, 0, 0);
        row.addView(dText);

        return row;
    }

    private View createSectionHeader(String text) {
        com.jdroidx.jfindme.ui.base.UiArchitect.DesignStage stage = com.jdroidx.jfindme.ui.base.UiArchitect.getStage(this);
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Native Theme Tint (Thin, Elegant)
        
        // 🚀 L7 Architecture Sync: Standardizing Section Header size to Header tier (18f baseline)
        header.setTextSize(com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.scaleText(stage, 18, true));
        
        int pT = com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.scalePadding(stage, 40);
        int pB = com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.scalePadding(stage, 20);
        int sp = scale(40);
        header.setPadding(sp, pT, sp, pB); // Aligns precisely with card geometries natively!
        header.setTypeface(null, Typeface.BOLD);
        return header;
    }

    private View createSectionDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.DKGRAY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        lp.setMargins(0, 60, 0, 40);
        divider.setLayoutParams(lp);
        return divider;
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
            SystemLogger.INSTANCE.e(TAG, "Native QR Graphics skipped.", e);
        }
    }

    private void rebuildActiveRosterList() {
        if (rosterCard != null && identityCardField != null) {
            ViewGroup parent = (ViewGroup) rosterCard.getParent();
            if (parent != null) {
                int index = parent.indexOfChild(rosterCard);
                parent.removeViewAt(index);
                rosterCard = (LinearLayout) buildActiveRosterList(identityCardField, parent);
                parent.addView(rosterCard, index);
            }
        }
    }

    /**
     * 🚀 Forensic Role Change: Delegated to ViewModel for Close-Loop MVVM Parity.
     */
    private void executeForensicRoleChange(String uid, String newRole, boolean isFriend) {
        String myGrp = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getString("active_group", "GRP_78822");
        
        viewModel.updateForensicIdentity(this, uid, myGrp, newRole, isFriend, () -> {
            Toast.makeText(this, "Identity Matrix Finalized: " + newRole, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSignalMatrixListener() {
        // Redundant: Managed by GroupViewModel SignalMatrix observer
    }

    private View buildTechnicalFooter() {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        int dp = scale(40);
        footer.setPadding(dp, 0, dp, dp);

        // 🚀 RESTORE LOST ROSTER ASSETS (Prominent Recovery Action)
        LinearLayout restoreAction = new LinearLayout(this);
        restoreAction.setOrientation(LinearLayout.HORIZONTAL);
        restoreAction.setGravity(Gravity.CENTER_VERTICAL);
        restoreAction.setPadding(scale(40), scale(40), scale(40), scale(40));
        restoreAction.setFocusable(true);
        restoreAction.setClickable(true);
        restoreAction.setOnClickListener(v -> showRestoreLostRosterAssetsDialog());
        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, restoreAction, "CARD_STANDARD");

        ImageView searchIcon = new ImageView(this);
        searchIcon.setImageResource(android.R.drawable.ic_menu_search);
        searchIcon.setColorFilter(getGradientEnd());
        int rIconSize = scale(80);
        restoreAction.addView(searchIcon, new LinearLayout.LayoutParams(rIconSize, rIconSize));

        TextView restoreText = new TextView(this);
        restoreText.setText("RESTORE LOST ROSTER ASSETS");
        restoreText.setTextColor(getTextColor());
        restoreText.setTextSize(scaleText(14));
        restoreText.setTypeface(null, Typeface.BOLD);
        restoreText.setPadding(20, 0, 0, 0);
        restoreAction.addView(restoreText);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(40));
        footer.addView(restoreAction, lp);

        return footer;
    }

    private void renderLocalRoster(String safeUid, LinearLayout branchList, TextView header, java.util.List<java.util.Map<String, Object>> roster) {
        branchList.removeAllViews();
        int count = 0;
        
        java.util.Set<String> processedUids = new java.util.HashSet<>();
        java.util.List<String> familyIds = new java.util.ArrayList<>();
        java.util.List<String> friendIds = new java.util.ArrayList<>();

        // 🚀 STEP 1: GATHER UNIQUE TARGETS (Exclude Owner - represented by Identity Card)
        for (java.util.Map<String, Object> node : roster) {
            String rUid = (String) node.get("uid");
            if (rUid == null || rUid.equals(safeUid)) continue;
            if (!processedUids.add(rUid)) continue;
            
            // 🏎️ FORENSIC PARTITIONING: The Shuffling Decider
            Object frFlag = node.get("is_friend");
            boolean isFriendNode = false;
            if (frFlag instanceof Boolean) isFriendNode = (Boolean) frFlag;
            else if (frFlag instanceof Integer) isFriendNode = ((Integer) frFlag == 1);
            else if (frFlag instanceof String) isFriendNode = ((String) frFlag).equals("1") || ((String) frFlag).equalsIgnoreCase("true");

            if (isFriendNode) friendIds.add(rUid);
            else familyIds.add(rUid);
            count++;
        }

        // 🚀 FAMILY GROUP HIERARCHY
        if (!familyIds.isEmpty()) {
            branchList.addView(createGroupHeading("FAMILY NETWORK (" + familyIds.size() + ")"));
            for (String fid : familyIds) {
                com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
                java.util.Map<String, String> fDos = db.getLocalDossier(fid);
                String roleStr = "Family Member";
                if (fDos != null) {
                    String rFam = fDos.get("role_family");
                    if (rFam != null && !rFam.equals("None")) roleStr = rFam;
                }
                branchList.addView(buildTreeNode(fid, "Linked Asset", false, false, null, roleStr));
            }
        }

        // 🚀 FRIENDS GROUP HIERARCHY
        if (!friendIds.isEmpty()) {
            branchList.addView(createGroupHeading("FRIENDS NETWORK (" + friendIds.size() + ")"));
            for (String frid : friendIds) {
                branchList.addView(buildTreeNode(frid, "Friend", false, false, null, "Friend"));
            }
        }

        if (header != null) header.setText("TOTAL ACTIVE DEVICES IN GROUP: " + (count + 1));
    }
    
    private void fetchFriendMatrix(String safeUid, LinearLayout branchList, StringBuilder authorizedUids, TextView header, long currentCount) {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(safeUid).child("friend_matrix")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot fmSnap) {
                    java.util.List<String> familyIds = new java.util.ArrayList<>();
                    java.util.List<String> friendIds = new java.util.ArrayList<>();
                    
                    com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(GroupActivity.this);
                    int totalP2P = 0;
                    
                    // 🚀 SSOT Integration: Use the Forensic Roster Table for Group Partitioning
                    java.util.List<java.util.Map<String, Object>> roster = db.getOfflineRoster();

                    // 🚀 STEP 1: ZERO-GAP SYNC - Ensure every member in the cloud has a local Roster presence
                    for (com.google.firebase.database.DataSnapshot p2pMember : fmSnap.getChildren()) {
                        String memberId = p2pMember.getKey();
                        if (memberId == null || memberId.equals(safeUid)) continue;
                        
                        boolean exists = false;
                        for (java.util.Map<String, Object> node : roster) {
                            if (memberId.equals(node.get("uid"))) { exists = true; break; }
                        }
                        
                        if (!exists) {
                            // 🛰️ Architectural Heal: Inject missing node into the Forensic Roster
                            db.saveRosterNode(memberId, "Relational Identity (" + memberId.substring(0, 5) + ")", "Member", "P2P Synchronized", false, false, "System Root", safeUid, memberId, "AUTO_SYNC");
                        }
                    }

                    // 🚀 STEP 2: ROSTER-DRIVEN GROUPING - Only the local table decides what to display
                    // Re-fetch roster after sync to ensure 100% parity
                    java.util.List<java.util.Map<String, Object>> liveRoster = db.getOfflineRoster();
                    
                    for (java.util.Map<String, Object> node : liveRoster) {
                        String rUid = (String) node.get("uid");
                        if (rUid == null || rUid.equals(safeUid)) continue;
                        if (authorizedUids.toString().contains(rUid)) continue; 
                        
                        // Verification: Only display nodes that are part of the current active group in the cloud
                        if (fmSnap.hasChild(rUid)) {
                            authorizedUids.append(rUid).append(",");
                            totalP2P++;
                            
                            Object frFlag = node.get("is_friend");
                            boolean isFriendNode = (frFlag instanceof Boolean) ? (Boolean) frFlag : false;
                            
                            if (isFriendNode) friendIds.add(rUid);
                            else familyIds.add(rUid);
                        }
                    }
                    
                    if (header != null) header.setText("TOTAL ACTIVE DEVICES IN GROUP: " + (currentCount + totalP2P));
                    branchList.removeAllViews();

                    java.util.Map<String, String> myDos = db.getLocalDossier("OWNER_001");
                    String myGender = myDos != null ? myDos.get("user_gender") : "Male";

                    String myName = (myDos != null && myDos.containsKey("full_name")) ? myDos.get("full_name") : getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getString("user_nickname", "Network Admin");
                    
                    // 🚀 NETWORK GATEWAY IDENTITY (SELF)
                    branchList.addView(createGroupHeading("NETWORK GATEWAY (PRIMARY OWNER)"));
                    branchList.addView(buildTreeNode(safeUid, myName, true, true, null, "OWNER"));
                    
                    // 🚀 FAMILY GROUP HIERARCHY
                    if (!familyIds.isEmpty()) {
                        View familyHeader = createGroupHeading("FAMILY NETWORK (" + familyIds.size() + ")");
                        branchList.addView(familyHeader);
                        
                        int childCount = 0;
                        for (String fid : familyIds) {
                            java.util.Map<String, String> fDos = db.getLocalDossier(fid);
                            String roleStr = "Member";
                            if (fDos != null) {
                                String rFam = fDos.get("role_family");
                                String gender = fDos.get("user_gender");
                                if (rFam != null) {
                                    if (rFam.equals("Child")) {
                                        childCount++;
                                        String sonDaughter = (gender != null && gender.equalsIgnoreCase("Female")) ? "Daughter" : "Son";
                                        roleStr = "Child #" + childCount + " (" + sonDaughter + ")";
                                    } else if (rFam.equals("Spouse")) {
                                        if (gender != null && gender.equalsIgnoreCase(myGender)) {
                                            roleStr = "Spouse";
                                        } else {
                                            roleStr = (gender != null && gender.equalsIgnoreCase("Female")) ? "Wife" : "Husband";
                                        }
                                    } else {
                                        roleStr = rFam;
                                    }
                                }
                            }
                            branchList.addView(buildTreeNode(fid, "Linked Asset", false, false, null, roleStr));
                        }
                    }

                    // 🚀 FRIENDS GROUP HIERARCHY
                    if (!friendIds.isEmpty()) {
                        branchList.addView(createGroupHeading("FRIENDS NETWORK (" + friendIds.size() + ")"));
                        for (String frid : friendIds) {
                            branchList.addView(buildTreeNode(frid, "Friend", false, false, null, "Friend"));
                        }
                    }
                }
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
    }

    /**
     * 🛡️ QR Handshake Toast Automation (V39 FINAL)
     * Renders a high-fidelity tactical overlay upon QR recognition.
     */
    private void showTacticalHandshakeOverlay(String code) {
        // Physical Trigger: Haptic Pulse
        android.os.Vibrator v = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE));

        // UI Layer: Glassmorphism Card
        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(40, 0, 0, 0)); // 15.6% Dark Wash
        overlay.setClickable(true);
        overlay.setFocusable(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(60, 80, 60, 80);
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.argb(217, 255, 255, 255)); // 85% White Glass
        gd.setCornerRadius(50f);
        gd.setStroke(4, getGradientStart());
        card.setBackground(gd);

        ImageView check = new ImageView(this);
        check.setImageResource(android.R.drawable.checkbox_on_background);
        check.setColorFilter(getGradientStart());
        card.addView(check, new LinearLayout.LayoutParams(180, 180));

        TextView tv = new TextView(this);
        tv.setText("IDENTITY RESOLVED");
        tv.setTextColor(getTextColor()); // 🚀 Synergetic Theme Color
        tv.setPadding(0, 40, 0, 10);
        com.jdroidx.jfindme.ui.base.UiArchitect.applyText(this, tv, "TEXT_TITLE");
        card.addView(tv);

        TextView tvCodeRaw = new TextView(this);
        tvCodeRaw.setText("SCAN MATRIX: " + code);
        tvCodeRaw.setTextColor(getSecondaryTextColor()); // 🚀 Synergetic Theme Color
        tvCodeRaw.setTextSize(11);
        card.addView(tvCodeRaw);

        android.widget.FrameLayout.LayoutParams cardLp = new android.widget.FrameLayout.LayoutParams(600, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.CENTER;
        overlay.addView(card, cardLp);

        ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();
        root.addView(overlay, new android.view.ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Auto-Dismiss with Forensic Fade
        overlay.animate().alpha(0f).setDuration(400).setStartDelay(2800).withEndAction(() -> root.removeView(overlay));
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.i(TAG, "Forensic Gateway Engine Successfully Executed.");
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(GroupViewModel.class);
        
        // 🧱 Observer: Roster Matrix Synchronization
        viewModel.getRosterData().observe(this, roster -> {
            if (roster != null) {
                rebuildActiveRosterList();
            }
        });
        
        // 🦋 Observer: AngelLoader Lifecycle (The Butterfly Guard)
        viewModel.isLoading().observe(this, loading -> {
            if (loading != null && loading) {
                com.jdroidx.jfindme.utils.AngelLoader.show(this);
            } else {
                new android.os.Handler().postDelayed(() -> com.jdroidx.jfindme.utils.AngelLoader.hide(this), 1000);
            }
        });
        
        // 🛰️ Observer: Signal Matrix (WhatsApp-style UI re-hydration)
        viewModel.getSignalReceived().observe(this, received -> {
            if (received != null && received) {
                Toast.makeText(this, "Network Identity Synchronized: Roster Updated.", Toast.LENGTH_SHORT).show();
                rebuildActiveRosterList();
            }
        });
        
        // 🚀 Observer: Forensic Sync Status
        viewModel.getSyncStatus().observe(this, status -> {
             if (status != null) com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, status);
        });

        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (myUid != null) {
            viewModel.setupSignalListener(myUid);
            rebuildActiveRosterList();
        }
    }

    private View buildForensicMemberStack(java.util.List<java.util.Map<String, Object>> nodes, int accentColor) {
        if (nodes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No active network nodes in this segment.");
            empty.setTextColor(getSecondaryTextColor());
            empty.setTextSize(scaleText(9));
            empty.setPadding(scale(40), 0, 0, 0);
            return empty;
        }

        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setPadding(scale(40), 10, scale(40), 10);
        
        for (java.util.Map<String, Object> node : nodes) {
            String uid = (String) node.get("uid");
            String name = (String) node.get("display_name");
            String role = (String) node.get("role_title");
            if (name == null) name = "Unknown Node";
            if (role == null) role = "Encrypted Node";

            // 🧥 DOSSIER CARD (Independent Shadow Partition)
            LinearLayout dossier = new LinearLayout(this);
            dossier.setOrientation(LinearLayout.HORIZONTAL);
            dossier.setGravity(Gravity.CENTER_VERTICAL);
            applySolidCardGeometry(dossier, getCardColor(), 20f);
            dossier.setPadding(scale(30), scale(30), scale(30), scale(30));
            
            // 🛡️ Avatar Block (Circular Shadow)
            ImageView av = new ImageView(this);
            av.setImageResource(android.R.drawable.ic_menu_myplaces);
            av.setPadding(8, 8, 8, 8);
            android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
            c.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            c.setColor(getPrimaryColor());
            c.setStroke(4, accentColor);
            av.setBackground(c);
            dossier.addView(av, new LinearLayout.LayoutParams(scale(110), scale(110)));

            // 🧱 Metadata Block
            LinearLayout meta = new LinearLayout(this);
            meta.setOrientation(LinearLayout.VERTICAL);
            meta.setPadding(scale(30), 0, 0, 0);
            meta.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvName = new TextView(this);
            tvName.setText(name.toUpperCase());
            tvName.setTextColor(getTextColor());
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextSize(13);
            meta.addView(tvName);

            TextView tvRole = new TextView(this);
            tvRole.setText(role);
            tvRole.setTextColor(getSecondaryTextColor());
            tvRole.setTextSize(10);
            tvRole.setAlpha(0.8f);
            meta.addView(tvRole);
            
            dossier.addView(meta);

            // 🏎️ Forensic Telemetry Icon (Right Aligned)
            ImageView telemetry = new ImageView(this);
            telemetry.setImageResource(android.R.drawable.ic_media_play);
            telemetry.setColorFilter(Color.parseColor("#FF5722"));
            dossier.addView(telemetry, new LinearLayout.LayoutParams(scale(40), scale(40)));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, scale(20)); // Secondary Shadow Gap
            stack.addView(dossier, lp);
        }
        
        return stack;
    }

    private View buildMiniAvatarStack(java.util.List<java.util.Map<String, Object>> nodes, int borderColor) {
        FrameLayout stack = new FrameLayout(this);
        int size = scale(55);
        int overlap = scale(35);
        
        if (nodes.isEmpty()) {
             ImageView empty = new ImageView(this);
             empty.setImageResource(android.R.drawable.ic_menu_add);
             empty.setColorFilter(Color.GRAY);
             empty.setAlpha(0.3f);
             stack.addView(empty, new FrameLayout.LayoutParams(size, size));
             return stack;
        }

        for (int i = 0; i < nodes.size(); i++) {
            ImageView av = new ImageView(this);
            av.setImageResource(android.R.drawable.ic_menu_myplaces);
            av.setPadding(5, 5, 5, 5);
            
            android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
            c.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            c.setColor(getPrimaryColor());
            c.setStroke(2, borderColor);
            av.setBackground(c);
            
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
            lp.leftMargin = i * overlap;
            stack.addView(av, lp);
        }
        
        // Wrap in a sizing container to ensure it centers in the vertical parent
        LinearLayout wrap = new LinearLayout(this);
        wrap.setGravity(Gravity.CENTER);
        wrap.addView(stack, new LinearLayout.LayoutParams(size + (Math.max(0, nodes.size()-1) * overlap), size));
        return wrap;
    }
}

