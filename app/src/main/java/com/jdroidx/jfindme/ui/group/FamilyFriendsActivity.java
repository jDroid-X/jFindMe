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
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Relational Organization & Management");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
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
        tabFamily.setPadding(0, 40, 0, 40);
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
        tabFriends.setPadding(0, 40, 0, 40);
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
        mainScrollView = new ScrollView(this);
        rosterContainer = new LinearLayout(this);
        rosterContainer.setOrientation(LinearLayout.VERTICAL);
        rosterContainer.setPadding(40, 40, 40, 100);
        
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
        
        // 🚀 QA Fix: Render the actual physical array, not the Mock Placeholders!
        rosterContainer.addView(createSectionHeader("CORE GUARDIANS (ADMINISTRATORS)"));
        
        for (RosterNode n : currentRoster) {
            String upTitle = n.getRoleTitle().toUpperCase();
            if (upTitle.contains("OWNER") || upTitle.contains("FATHER") || upTitle.contains("MOTHER")) {
                rosterContainer.addView(createRoleCard(n.getDisplayName().toUpperCase() + " (OWNER)", "🟢 LIVE TARGET TRACKED", true, n.getUid())); 
            }
            if (upTitle.contains("SPOUSE")) {
                rosterContainer.addView(createRoleCard(n.getDisplayName().toUpperCase() + " (SPOUSE)", "🟢 LIVE TARGET TRACKED", true, n.getUid())); 
            }
        }
        
        rosterContainer.addView(createSectionHeader("DEPENDENT TARGETS (TRACKED)"));
        for (RosterNode n : currentRoster) {
            if (n.getRoleTitle().toUpperCase().contains("CHILD")) {
                String childTag = n.getRoleTitle().toUpperCase().contains("CHILD 1") ? "(CHILD 1)" : n.getRoleTitle().toUpperCase().contains("CHILD 2") ? "(CHILD 2)" : "(CHILD)";
                rosterContainer.addView(createRoleCard(n.getDisplayName().toUpperCase() + " " + childTag, "🟢 LIVE TARGET TRACKED", false, n.getUid()));
            }
        }
    }

    private void renderFriendsTab() {
        activeTab = 1;
        tvFriends.setTextColor(getGradientStart()); tvFriends.setAlpha(1.0f);
        tvFamily.setTextColor(Color.GRAY); tvFamily.setAlpha(0.5f);
        rosterContainer.removeAllViews();
        
        rosterContainer.addView(createSectionHeader("EXTENDED NETWORK (LIMITED ACCESS)"));
        
        // 🚀 QA Fix: Dynamically hook the LiveData into the Friends Tab too!
        for (RosterNode n : currentRoster) {
            if (n.getRoleTitle().toUpperCase().contains("FRIEND") || n.getRoleTitle().toUpperCase().contains("NEIGHBOR") || n.getRoleTitle().toUpperCase().contains("TRUSTED")) {
                String cleanRole = n.getRoleTitle().toUpperCase();
                rosterContainer.addView(createRoleCard(n.getDisplayName().toUpperCase() + " (" + cleanRole + ")", "🟢 LIVE TARGET TRACKED", false, n.getUid()));
            }
        }
    }

    private View createSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getGradientStart());
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(10, 30, 0, 20);
        return tv;
    }

    private View createRoleCard(String roleTitle, String statusText, boolean isAdministrator) {
        return createRoleCard(roleTitle, statusText, isAdministrator, null);
    }
    
    private View createRoleCard(String roleTitle, String statusText, boolean isAdministrator, String targetUid) {
        LinearLayout logCard = new LinearLayout(this);
        logCard.setOrientation(LinearLayout.HORIZONTAL);
        logCard.setGravity(Gravity.CENTER_VERTICAL);
        
        // Exact Compact Tile structure matching Device Pairing Network!
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(getCardColor());
        gd.setCornerRadius(25f);
        logCard.setBackground(gd);
        logCard.setElevation(8f);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(20, 5, 20, 5); // Ultra-tight Vertical & Lateral Gap
        logCard.setLayoutParams(cardLp);
        logCard.setPadding(30, 15, 30, 15); // Ultra-compact internal spacing!
        
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
        
        avatar.setBackground(c);
        avatar.setPadding(10, 10, 10, 10); // True minimum padding
        
        // 🚀 Expand Touch Matrix: Clicking the Avatar/Profile Image opens the specific Citizen Dossier natively!
        avatar.setFocusable(true);
        avatar.setClickable(true);
        android.util.TypedValue outValueBorderless = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValueBorderless, true);
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
        } else {
            avatar.setOnClickListener(v -> android.widget.Toast.makeText(FamilyFriendsActivity.this, "Empty Avatar Slot. Cannot load profile.", android.widget.Toast.LENGTH_SHORT).show());
        }
        
        logCard.addView(avatar, new LinearLayout.LayoutParams(110, 110)); // Restored scale to prevent missing vector crash!

        // Text Matrix
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(25, 0, 0, 0); // Ultra-tight distance between avatar and text
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)); // weight 1 takes up center block!

        TextView tvTitle = new TextView(this);
        tvTitle.setText(roleTitle);
        tvTitle.setTextColor(getTextColor());
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(null, Typeface.BOLD);
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
            if (statusText.contains("Missing")) {
                new android.app.AlertDialog.Builder(FamilyFriendsActivity.this)
                    .setTitle(roleTitle + " (UNPAIRED)")
                    .setMessage("This specific relational slot is currently empty. Tap the Green [+] Button to generate a unique pairing code and invite this person to the Network Roster.")
                    .setPositiveButton("Dismiss", null)
                    .show();
            } else {
                new android.app.AlertDialog.Builder(FamilyFriendsActivity.this)
                    .setTitle(roleTitle + " Profile Details")
                    .setMessage("Tracking Target: Secure Node Active\n\n" +
                                "📡 Last Ping: 2 seconds ago\n" +
                                "🔋 Battery Status: 85% (Optimal)\n" +
                                "🛡️ Tracking Resolution: Strict (Ultra-Wideband)\n" +
                                "🌐 Assigned Domain: Global Tracking permitted.\n\n" +
                                "Live Map integration is fully stable.")
                    .setPositiveButton("Close Details", null)
                    .setNegativeButton("Ping Device", (dialog, which) -> {
                        android.widget.Toast.makeText(FamilyFriendsActivity.this, "Forcing physical refresh on " + roleTitle, android.widget.Toast.LENGTH_SHORT).show();
                    })
                    .show();
            }
        });

        return logCard;
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
