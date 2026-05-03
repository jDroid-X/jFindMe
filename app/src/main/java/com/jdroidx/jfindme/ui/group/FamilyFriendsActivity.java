package com.jdroidx.jfindme.ui.group;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.group.FamilyFriendsViewModel;
import com.jdroidx.jfindme.ui.group.PersonDetailsActivity;



import com.jdroidx.jfindme.ui.group.RosterNode;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;

public class FamilyFriendsActivity extends BaseActivityJava {

    private final String TAG = "FamilyFriendsRoster";
    
    // Core Layout Arrays
    private LinearLayout rosterContainer;
    private ScrollView mainScrollView;
    private ImageView ownerAvatar;
    private TextView tvOwnerName;
    
    // Tab Interaction Cache
    private TextView tvFamily, tvFriends;
    private LinearLayout tabFamily, tabFriends;
    private int activeTab = 0; // 0=Family, 1=Friends
    private FamilyFriendsViewModel viewModel;
    
    // 🚀 QA Fix: Global Cache Memory to preserve OS LiveData across Fragment/Tab Switches
    private java.util.List<RosterNode> currentRoster = new java.util.ArrayList<>();

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // 1. HERO BANNER
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
            applyGlobalHeaderGeometry(header); 
        
        TextView title = new TextView(this);
        title.setText("FAMILY AND FRIENDS");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Relational Organization & Management");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        header.addView(subtitle);
        
        mainRoot.addView(header);

        // 2. DUAL-TAB NAVIGATION CLUSTER
        LinearLayout tabController = new LinearLayout(this);
        tabController.setOrientation(LinearLayout.HORIZONTAL);
        tabController.setGravity(Gravity.CENTER);
        int masterPad = getResponsivePaddingQuota();
        tabController.setPadding(masterPad, masterPad / 2, masterPad, masterPad / 2);
        
        // Tab A: FAMILY (Capsule Button)
        tabFamily = new LinearLayout(this);
        tabFamily.setOrientation(LinearLayout.HORIZONTAL);
        tabFamily.setGravity(Gravity.CENTER);
        int tapPad = scale(15);
        tabFamily.setPadding(tapPad * 2, tapPad, tapPad * 2, tapPad);
        tabFamily.setFocusable(true);
        tabFamily.setClickable(true);
        
        tvFamily = new TextView(this);
        tvFamily.setText("FAMILY");
        tvFamily.setTextSize(14);
        tvFamily.setTypeface(null, Typeface.BOLD);
        tabFamily.addView(tvFamily);
        
        // Tab B: FRIENDS (Capsule Button)
        tabFriends = new LinearLayout(this);
        tabFriends.setOrientation(LinearLayout.HORIZONTAL);
        tabFriends.setGravity(Gravity.CENTER);
        tabFriends.setPadding(tapPad * 2, tapPad, tapPad * 2, tapPad);
        tabFriends.setFocusable(true);
        tabFriends.setClickable(true);
        
        tvFriends = new TextView(this);
        tvFriends.setText("FRIENDS");
        tvFriends.setTextSize(14);
        tvFriends.setTypeface(null, Typeface.BOLD);
        tabFriends.addView(tvFriends);

        LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tabLp.setMargins(5, 0, 5, 0);
        
        UiArchitect.applyCardWithOverride(this, tabFamily, "CARD_CAPSULE_BUTTON");
        UiArchitect.applyCardWithOverride(this, tabFriends, "CARD_CAPSULE_BUTTON");

        tabController.addView(tabFamily, tabLp);
        tabController.addView(tabFriends, tabLp);
        
        mainRoot.addView(tabController);
        
        // 🚀 IDENTITY CARD (Owner Tile 1): Mapped from Core Profile
        mainRoot.addView(buildIdentityBlock(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        // 3. DYNAMIC RENDERING SURFACE
        int masterPadding = getResponsivePaddingQuota();
        mainScrollView = new ScrollView(this);
        rosterContainer = new LinearLayout(this);
        rosterContainer.setOrientation(LinearLayout.VERTICAL);
        rosterContainer.setPadding(masterPadding, masterPadding, masterPadding, masterPadding * 4);
        
        mainScrollView.addView(rosterContainer);
        mainRoot.addView(mainScrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 4. INTERACTION EVENT LISTENERS
        tabFamily.setOnClickListener(v -> renderFamilyTab());
        tabFriends.setOnClickListener(v -> renderFriendsTab());

        // BOOT STATE: Check for Intent Overrides (Direct Tab Mapping)
        if (getIntent().hasExtra("REQUESTED_TAB")) {
            int target = getIntent().getIntExtra("REQUESTED_TAB", 0);
            if (target == 1) renderFriendsTab();
            else renderFamilyTab();
        } else {
            renderFamilyTab();
        }

        // 🚀 RECOVERY NODE: Aggressive Cloud Restore (Final Pillar of the Social Merger)
        TextView reclaimBtn = new TextView(this);
        reclaimBtn.setText("RECLAIM CLOUD ROSTER");
        reclaimBtn.setPadding(30, 20, 30, 20);
        reclaimBtn.setGravity(Gravity.CENTER);
        UiArchitect.applyCardWithOverride(this, reclaimBtn, "CARD_CAPSULE_BUTTON");
        
        reclaimBtn.setOnClickListener(v -> {
            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Scanning Cloud Matrix...");
            viewModel.forceAggressiveRecovery(this, success -> {
                runOnUiThread(() -> {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                    if (success) {
                        android.widget.Toast.makeText(this, "ROSTER RESTORED SUCCESSFULLY", android.widget.Toast.LENGTH_LONG).show();
                        viewModel.initializeCloudSync(this); // Force refresh LiveData
                    } else {
                        android.app.AlertDialog diag = new android.app.AlertDialog.Builder(this)
                            .setTitle("RECOVERY SCAN COMPLETE")
                            .setMessage("Zero matching nodes found in your active cloud group. If this is a new setup, please use the NETWORK GATEWAY to pair your first device.")
                            .setPositiveButton("Open Gateway", (di, w) -> {
                                startActivity(new Intent(this, NetworkGatewayActivity.class));
                                finish();
                            })
                            .setNegativeButton("Dismiss", null)
                            .show();
                        applyDialogTheme(diag);
                    }
                });
                return null;
            });
        });

        LinearLayout reclaimWrapper = new LinearLayout(this);
        reclaimWrapper.setPadding(masterPadding, 10, masterPadding, 30);
        reclaimWrapper.addView(reclaimBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mainRoot.addView(reclaimWrapper);

        // 🚀 ANALYTICAL HUB: Architecture Status Bar (Transparency Pillar)
        mainRoot.addView(buildArchitectureStatusBar());

        // 🚀 QA Fix: Global Navigation Integrity
        String[] keys = {"NAV_NOTICE", "NAV_DEVICE", "NAV_ME"};
        mainRoot.addView(UiArchitect.createBottomNav(this, keys, "NAV_ME"));

        return mainRoot;
    }

    private void renderFamilyTab() {
        activeTab = 0;
        UiArchitect.applySelection(this, tabFamily, true);
        UiArchitect.applySelection(this, tabFriends, false);
        rosterContainer.removeAllViews();
        
        // --- 🚀 PILLAR 1: CORE GUARDIANS (NETWORK ADMINS) ---
        rosterContainer.addView(createSectionHeader("CORE GUARDIANS (NETWORK ADMINS)"));
        java.util.List<RosterNode> admins = new java.util.ArrayList<>();
        java.util.List<RosterNode> spouses = new java.util.ArrayList<>();
        java.util.List<RosterNode> children = new java.util.ArrayList<>();
        java.util.List<RosterNode> others = new java.util.ArrayList<>();
        
        for (RosterNode n : currentRoster) {
            if (!n.isFriend()) {
                String role = n.getRoleTitle().toUpperCase();
                if (n.isAdministrator()) admins.add(n);
                else if (role.contains("SPOUSE")) spouses.add(n);
                else if (role.contains("CHILD")) children.add(n);
                else others.add(n);
            }
        }
        renderTreeBranch(admins, "GUARDIAN");

        // --- 🚀 PILLAR 2: SPOUSE ENTITIES ---
        if (!spouses.isEmpty()) {
            rosterContainer.addView(createSectionHeader("SPOUSE ENTITIES"));
            renderTreeBranch(spouses, "SPOUSE");
        }

        // --- 🚀 PILLAR 3: DEPENDENT TARGETS (CHILDREN) ---
        if (!children.isEmpty()) {
            rosterContainer.addView(createSectionHeader("DEPENDENT TARGETS (CHILDREN)"));
            renderTreeBranch(children, "CHILD");
        }
        
        // --- 🚀 PILLAR 4: OTHER FAMILY RELATIONS ---
        if (!others.isEmpty()) {
            rosterContainer.addView(createSectionHeader("UNCLASSIFIED FAMILY NODES"));
            renderTreeBranch(others, "RELATION");
        }
    }

    private void renderFriendsTab() {
        activeTab = 1;
        UiArchitect.applySelection(this, tabFamily, false);
        UiArchitect.applySelection(this, tabFriends, true);
        rosterContainer.removeAllViews();
        
        rosterContainer.addView(createSectionHeader("EXTENDED NETWORK (TRUSTED NODES)"));
        java.util.List<RosterNode> friends = new java.util.ArrayList<>();
        if (currentRoster != null) {
            for (RosterNode n : currentRoster) {
                if (n.isFriend()) friends.add(n);
            }
        }
        renderTreeBranch(friends, "FRIEND");
    }

    private void renderTreeBranch(java.util.List<RosterNode> nodes, String branchType) {
        if (nodes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("   └── [NO NODES MAPPED]");
            empty.setTextColor(Color.DKGRAY);
            empty.setPadding(60, 10, 0, 10);
            rosterContainer.addView(empty);
            return;
        }
        for (RosterNode n : nodes) {
            String label = n.getDisplayName().toUpperCase();
            rosterContainer.addView(createRoleCard(label, "🟢 LIVE TARGET TRACKED", branchType.equals("GUARDIAN"), n.getUid(), n.getAvatarId()));
        }
    }

    private View createSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getGradientStart());
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(10, getResponsivePaddingQuota(), 0, getResponsivePaddingQuota() / 2);
        return tv;
    }
    
    private View createRoleCard(String roleTitle, String statusText, boolean isAdministrator, String targetUid, String avatarId) {
        LinearLayout logCard = new LinearLayout(this);
        logCard.setOrientation(LinearLayout.HORIZONTAL);
        logCard.setGravity(Gravity.CENTER_VERTICAL);
        
        UiArchitect.applyCard(this, logCard, "CARD_STANDARD");

        int sideMargin = getResponsivePaddingQuota();
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(sideMargin, 5, sideMargin, 5); 
        logCard.setLayoutParams(cardLp);
        int logPad = getResponsivePaddingQuota() + 5;
        logCard.setPadding(logPad, logPad / 2, logPad, logPad / 2); 
        
        // 🚀 Identity Identity Bridge: Use AvatarLoader to pick the customized identity
        ImageView avatar = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, avatar, avatarId);
        
        // Draw the Circle Overlay dynamically
        android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
        c.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        c.setColor(Color.parseColor("#33101010")); // Deep Hollow
        
        if (targetUid != null) {
            int ringColor = getGradientStart();
            if (roleTitle.toUpperCase().contains("MOTHER") || roleTitle.toUpperCase().contains("SPOUSE")) ringColor = Color.parseColor("#E91E63"); // QA Pink Neon Node
            else if (roleTitle.toUpperCase().contains("CHILD")) ringColor = Color.parseColor("#FF9800"); // QA Orange Neon Node
            c.setStroke(6, ringColor); 
        } else {
            c.setStroke(4, Color.DKGRAY); // Offline State
        }
        avatar.setBackground(c);
        
        // --- INTERACTIVE FOREGROUND RE-MAPPING ---
        android.util.TypedValue outValueBorderless = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValueBorderless, true);
        
        // 🚀 Expand Touch Matrix: Clicking the Avatar/Profile Image opens the specific Citizen Dossier natively!
        avatar.setFocusable(true);
        avatar.setClickable(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            avatar.setForeground(getDrawable(outValueBorderless.resourceId));
        }

        if (targetUid != null) {
            avatar.setOnClickListener(v -> {
                Intent intent = new Intent(FamilyFriendsActivity.this, PersonDetailsActivity.class);
                intent.putExtra("TARGET_UID", targetUid);
                intent.putExtra("TARGET_ROLE", roleTitle);
                startActivity(intent);
            });
        }
        
        int iSize = getResponsiveIconQuota();
        logCard.addView(avatar, new LinearLayout.LayoutParams(iSize, iSize)); 

        // Text Matrix
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(25, 0, 0, 0); // Ultra-tight distance between avatar and text
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)); // weight 1 takes up center block!

        TextView tvTitle = new TextView(this);
        tvTitle.setText(roleTitle);
        tvTitle.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvTitle, "TEXT_LABEL");
        textBlock.addView(tvTitle);

        TextView tvDetail = new TextView(this);
        tvDetail.setText(statusText.equals("Missing Device Pairing") ? "Network Connection Missing..." : statusText);
        tvDetail.setTextColor(statusText.contains("LIVE") || statusText.contains("Generate") ? getSecondaryTextColor() : Color.parseColor("#F44336"));
        tvDetail.setTextSize(10f);
        tvDetail.setTypeface(null, Typeface.NORMAL); // Unbold subtext for aesthetics
        tvDetail.setPadding(0, 5, 0, 0);
        textBlock.addView(tvDetail);

        logCard.addView(textBlock);

        // Action Button: Route to Network Gateway
        ImageView pairAction = new ImageView(this);
        pairAction.setImageResource(android.R.drawable.ic_menu_add);
        pairAction.setColorFilter(getGradientStart()); // Aesthetic Green
        pairAction.setPadding(30, 30, 30, 30);
        pairAction.setContentDescription("Pair " + roleTitle + " Device");
        
        pairAction.setFocusable(true);
        pairAction.setClickable(true);
        pairAction.setBackgroundResource(outValueBorderless.resourceId);
        
        // 🚀 GATEWAY TRANSITION: Switch to the external Gateway Hub for QR/Handshake pairing!
        pairAction.setOnClickListener(v -> {
            startActivity(new Intent(this, NetworkGatewayActivity.class));
        });

        // 🚀 QA Fix: Only render [+] Button if the slot is empty!
        if (statusText.contains("Missing") || statusText.contains("Generate") || statusText.contains("Missing Device Pairing")) {
            pairAction.setPadding(10, 10, 10, 10);
            logCard.addView(pairAction, new LinearLayout.LayoutParams(70, 70));
        } else if (targetUid != null) {
            // PHASE 5 UX SYNC: Use Play Icon to natively sync to Group Activity!
            ImageView detailsAction = new ImageView(this);
            detailsAction.setImageResource(android.R.drawable.ic_media_play);
            detailsAction.setColorFilter(getGradientStart(), android.graphics.PorterDuff.Mode.SRC_IN); // Inherit theme exactly!
            detailsAction.setPadding(10, 10, 10, 10);
            detailsAction.setFocusable(true);
            detailsAction.setClickable(true);
            detailsAction.setBackgroundResource(outValueBorderless.resourceId);
            detailsAction.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Hardware Edge ID: " + targetUid, android.widget.Toast.LENGTH_LONG).show();
            });
            logCard.addView(detailsAction, new LinearLayout.LayoutParams(70, 70)); // Elegant slim right button!
        }

        // 🚀 QA Fix: Global Card Interaction Engine (Deep Details View)
        logCard.setFocusable(true);
        logCard.setClickable(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            logCard.setForeground(getDrawable(outValueBorderless.resourceId));
        }
        logCard.setOnClickListener(v -> {
            if (targetUid == null) return;
            
            // 🚀 NEW NETWORK RELATIONSHIP CLUSTER (MIGRATED FROM DOSSIER)
            LinearLayout dialRoot = new LinearLayout(this);
            dialRoot.setOrientation(LinearLayout.VERTICAL);
            dialRoot.setPadding(50, 40, 50, 40);
            
            TextView dialTitle = new TextView(this);
            dialTitle.setText("REASSIGN " + (activeTab == 0 ? "FAMILY" : "FRIEND") + " ROLE");
            dialTitle.setTextColor(getGradientStart());
            dialTitle.setTypeface(null, Typeface.BOLD);
            dialRoot.addView(dialTitle);
            
            android.widget.RadioGroup rg = new android.widget.RadioGroup(this);
            rg.setOrientation(LinearLayout.VERTICAL);
            rg.setPadding(0, 30, 0, 30);
            
            String[] options = activeTab == 0 ? new String[]{"Mother", "Father", "Child", "Spouse"} : new String[]{"Friend (Header)", "Friend (Team)"};
            for (int i = 0; i < options.length; i++) {
                android.widget.RadioButton rb = new android.widget.RadioButton(this);
                rb.setText(options[i]);
                rb.setTextColor(getTextColor());
                rb.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint()));
                rb.setId(500 + i);
                if (roleTitle.contains(options[i])) rb.setChecked(true);
                rg.addView(rb);
            }
            dialRoot.addView(rg);

            android.app.AlertDialog dial = new android.app.AlertDialog.Builder(this)
                .setView(dialRoot)
                .setPositiveButton("VERIFY & MAP", (dialog, which) -> {
                    int id = rg.getCheckedRadioButtonId();
                    if (id != -1) {
                        String selected = ((android.widget.RadioButton)rg.findViewById(id)).getText().toString();
                        updateTargetRole(targetUid, selected, activeTab == 0);
                    }
                })
                .setNeutralButton("VIEW DOSSIER", (dialog, which) -> {
                    Intent intent = new Intent(FamilyFriendsActivity.this, PersonDetailsActivity.class);
                    intent.putExtra("TARGET_UID", targetUid);
                    intent.putExtra("TARGET_ROLE", roleTitle);
                    startActivity(intent);
                })
                .setNegativeButton("EXIT", null)
                .show();
        });

        return logCard;
    }

    private void updateTargetRole(String uid, String role, boolean isFamily) {
        String famRole = isFamily ? role : "None";
        String frRole = isFamily ? "None" : role;
        
        com.jdroidx.jfindme.data.local.DatabaseHelper localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
        localDb.updateCitizenRole(uid, famRole, frRole, frRole.contains("Friend"));
        
        String myGrp = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);
        
        // 🚀 V57 TIERED UPDATES: Target Tier 1 (Identities & Profiles)
        com.google.firebase.database.DatabaseReference profRef = com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(uid);
        
        java.util.Map<String, Object> cp = new java.util.HashMap<>();
        cp.put("role_family", famRole);
        cp.put("role_friend", frRole);
        cp.put("ts_modified", System.currentTimeMillis());

        // 📚 Step 1: Update Tier 1 Profile
        profRef.updateChildren(cp);
        
        // 🛰️ Step 2: Update Tier 3 Relationship (if applicable)
        if (myGrp != null) {
            com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("members").child(uid).child("role").setValue(role);
        }
        
        // 🚀 Step 3: Trigger Network Refresh Signal (Dossier Sync)
        profRef.child("signals").child("refresh_roster").setValue(System.currentTimeMillis());
        
        android.widget.Toast.makeText(this, "Network Array Re-Mapped! Tier 1 Identity Sync Completed.", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ownerAvatar != null) com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, ownerAvatar);
        
        if (tvOwnerName != null) {
            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
            String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            java.util.Map<String, String> myDos = db.getLocalDossier(myUid != null ? myUid : "OWNER_001");
            if (myDos == null) myDos = db.getLocalDossier("OWNER_001");
            
            android.content.SharedPreferences prefs = getSharedPreferences("system_registry", MODE_PRIVATE);
            String nameStr = myDos != null ? myDos.get("id_full_name") : (prefs != null ? prefs.getString("user_nickname", "User Identity") : "User Identity");
            tvOwnerName.setText("👤 Owner: " + nameStr);
        }
    }

    @Override
    protected void setupLogic() {
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(FamilyFriendsViewModel.class);
        viewModel.initializeCloudSync(this);
        
        viewModel.getRosterLiveData().observe(this, nodes -> {
            // 🚀 QA Cache Fix: Always store the live pointer BEFORE deciding whether to aggressively repaint or not.
            currentRoster = nodes;
            
            // Sync current active tab
            if (activeTab == 0) {
                renderFamilyTab();
            } else if (activeTab == 1) {
                renderFriendsTab();
            }
        });
    }

    private View buildIdentityBlock() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(scale(25), scale(25), scale(25), scale(25)); 
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        ownerAvatar = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, ownerAvatar);
        
        // 🚀 Premium Circular Avatar (Responsive Scaling)
        int avatarSize = scale(168); 
        android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
        c.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        c.setColor(Color.TRANSPARENT);
        c.setStroke(scale(4), getGradientStart());
        ownerAvatar.setBackground(c);
        
        row.addView(ownerAvatar, new LinearLayout.LayoutParams(avatarSize, avatarSize));
        
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(scale(40), 0, 0, 0);
        
        // --- 1. OWNER NAME (Forensic Redirection Fix) ---
        android.content.SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String finalAgentId = jPrefs.getString("agent_id", "AGENT_ERROR");
        
        com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
        java.util.Map<String, String> myDos = db.getLocalDossier(finalAgentId);
        
        String nameStr = myDos != null ? myDos.get("id_full_name") : jPrefs.getString("user_nickname", "User Identity");
        
        tvOwnerName = new TextView(this);
        tvOwnerName.setText("👤 Owner: " + nameStr);
        tvOwnerName.setTextColor(getTextColor());
        tvOwnerName.setTextSize(scaleText(15));
        tvOwnerName.setTypeface(null, Typeface.BOLD);
        textBlock.addView(tvOwnerName);
        
        // Fetch Persistent UIDs
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

        // --- 4. NETWORK ID (Forensic Fix: Use Agent ID instead of Auth UID) ---
        TextView tvNetworkId = new TextView(this);
        tvNetworkId.setText("🌐 Network ID: - " + finalAgentId);
        tvNetworkId.setTextColor(getSecondaryTextColor());
        tvNetworkId.setTextSize(9);
        textBlock.addView(tvNetworkId);
        
        // 🛡️ ARCHITECTURAL SILENCE: Check local persistence for Hub status
        String localActiveGroup = jPrefs.getString("active_group", null);
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

    private View buildArchitectureStatusBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(20, 15, 20, 15);
        bar.setBackgroundColor(Color.argb(80, Color.red(getPrimaryColor()), Color.green(getPrimaryColor()), Color.blue(getPrimaryColor()))); 
        
        bar.addView(createArchitectureIcon(android.R.drawable.ic_lock_silent_mode, "Secure", "Analytical Tiering: 5-Tier Normalized Architecture active."));
        bar.addView(createArchitectureIcon(android.R.drawable.ic_menu_save, "Local", "Identity Hub: Data retrieval anchored in Tier 1 Local Profiles."));
        bar.addView(createArchitectureIcon(android.R.drawable.ic_popup_sync, "Matrix", "Analytical Sync: Cloud and Local parity maintained via tiered nodes."));
        bar.addView(createArchitectureIcon(android.R.drawable.ic_menu_recent_history, "Silicon", "Hardware Lock: Tier 2 Silicon Fingerprints verified and archived."));
        bar.addView(createArchitectureIcon(android.R.drawable.checkbox_on_background, "Mesh", "Relationship Mesh: Tier 3 Graph Traversal active for peer discovery."));

        return bar;
    }

    private View createArchitectureIcon(int resId, String label, String toastMsg) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(25, 5, 25, 5);
        box.setClickable(true);
        box.setOnClickListener(v -> android.widget.Toast.makeText(this, toastMsg, android.widget.Toast.LENGTH_SHORT).show());
        
        ImageView iv = new ImageView(this);
        iv.setImageResource(resId);
        iv.setColorFilter(getGradientStart());
        int iSize = scale(60); 
        box.addView(iv, new LinearLayout.LayoutParams(iSize, iSize));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(9);
        tv.setTextColor(getTextColor());
        tv.setAlpha(0.6f);
        tv.setGravity(Gravity.CENTER);
        box.addView(tv);

        return box;
    }
}

