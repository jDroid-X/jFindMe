package com.jdroidx.jfindme.ui.group;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import com.jdroidx.jfindme.ui.group.GroupActivity;
import com.jdroidx.jfindme.ui.group.RosterNode;

public class FamilyFriendsActivity extends BaseActivityJava {

    private final String TAG = "FamilyFriendsRoster";
    
    // Core Layout Arrays
    private LinearLayout rosterContainer;
    private ScrollView mainScrollView;
    
    // Tab Interaction Cache
    private LinearLayout tabFamily;
    private LinearLayout tabFriends;
    private TextView tvFamily, tvFriends;
    private int activeTab = 0;
    
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
        tabController.setBackgroundColor(Color.parseColor("#151515")); // Deep Dark Separator Constraint
        
        // Tab A: FAMILY
        tabFamily = new LinearLayout(this);
        tabFamily.setOrientation(LinearLayout.VERTICAL);
        tabFamily.setGravity(Gravity.CENTER);
        int tabPad = getResponsivePaddingQuota() + 15;
        tabFamily.setPadding(0, tabPad, 0, tabPad);
        tabFamily.setFocusable(true);
        tabFamily.setClickable(true);
        android.util.TypedValue outValueTabs = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValueTabs, true);
        tabFamily.setBackgroundResource(outValueTabs.resourceId);
        
        tvFamily = new TextView(this);
        tvFamily.setText("👨‍👩‍👧‍👦 FAMILY");
        tvFamily.setTextColor(getGradientStart()); // Active Green
        tvFamily.setTextSize(14);
        tvFamily.setTypeface(null, Typeface.BOLD);
        tabFamily.addView(tvFamily);
        
        // Tab B: FRIENDS
        tabFriends = new LinearLayout(this);
        tabFriends.setOrientation(LinearLayout.VERTICAL);
        tabFriends.setGravity(Gravity.CENTER);
        tabPad = getResponsivePaddingQuota() + 15;
        tabFriends.setPadding(0, tabPad, 0, tabPad);
        tabFriends.setFocusable(true);
        tabFriends.setClickable(true);
        tabFriends.setBackgroundResource(outValueTabs.resourceId);
        
        tvFriends = new TextView(this);
        tvFriends.setText("🤝 FRIENDS");
        tvFriends.setTextColor(Color.GRAY); // Inactive Base
        tvFriends.setTextSize(14);
        tvFriends.setTypeface(null, Typeface.BOLD);
        tabFriends.addView(tvFriends);

        LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tabController.addView(tabFamily, tabLp);
        tabController.addView(tabFriends, tabLp);
        
        mainRoot.addView(tabController);

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

        // BOOT STATE
        renderFamilyTab();

        return mainRoot;
    }

    private void renderFamilyTab() {
        activeTab = 0;
        tvFamily.setTextColor(getGradientStart()); tvFamily.setAlpha(1.0f);
        tvFriends.setTextColor(Color.GRAY); tvFriends.setAlpha(0.5f);
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
        tvFriends.setTextColor(getGradientStart()); tvFriends.setAlpha(1.0f);
        tvFamily.setTextColor(Color.GRAY); tvFamily.setAlpha(0.5f);
        rosterContainer.removeAllViews();
        
        rosterContainer.addView(createSectionHeader("EXTENDED NETWORK (TRUSTED NODES)"));
        java.util.List<RosterNode> friends = new java.util.ArrayList<>();
        for (RosterNode n : currentRoster) {
            if (n.isFriend()) friends.add(n);
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
            rosterContainer.addView(createRoleCard(label, "🟢 LIVE TARGET TRACKED", branchType.equals("GUARDIAN"), n.getUid()));
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

    private View createRoleCard(String roleTitle, String statusText, boolean isAdministrator) {
        return createRoleCard(roleTitle, statusText, isAdministrator, null);
    }
    
    private View createRoleCard(String roleTitle, String statusText, boolean isAdministrator, String targetUid) {
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
        
        // Avatar Emulation
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(targetUid != null ? android.R.drawable.ic_menu_gallery : android.R.drawable.ic_menu_camera);
        avatar.setColorFilter(targetUid != null ? Color.WHITE : Color.GRAY); 
        
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
        
        // Critical Intent: Passes the target Role into the old Scanner Gateway!
        pairAction.setOnClickListener(v -> {
            Intent intent = new Intent(FamilyFriendsActivity.this, GroupActivity.class);
            intent.putExtra("requested_network_role", roleTitle); 
            startActivity(intent);
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
        
        com.jdroidx.jfindme.data.local.DatabaseHelper localDb = new com.jdroidx.jfindme.data.local.DatabaseHelper(this);
        localDb.updateCitizenRole(uid, famRole, frRole, frRole.contains("Friend"));
        
        String myGrp = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getString("active_group", null);
        if (myGrp != null) {
            java.util.Map<String, Object> cp = new java.util.HashMap<>();
            cp.put("role_family", famRole);
            cp.put("role_friend", frRole);
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference()
                .child("groups").child(myGrp).child("citizen_dossiers").child(uid).updateChildren(cp);
        }
        android.widget.Toast.makeText(this, "Network Array Re-Mapped! Role Verified.", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void setupLogic() {
        FamilyFriendsViewModel viewModel = new androidx.lifecycle.ViewModelProvider(this).get(FamilyFriendsViewModel.class);
        viewModel.initializeCloudSync(this);
        
        viewModel.getRosterLiveData().observe(this, nodes -> {
            // 🚀 QA Cache Fix: Always store the live pointer BEFORE deciding whether to aggressively repaint or not.
            currentRoster = nodes;
            
            // Sync current active tab
            if (activeTab == 0) {
                renderFamilyTab();
            } else {
                renderFriendsTab();
            }
        });
    }
}

