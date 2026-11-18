package com.jdroidx.jfindme.ui.group;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
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

import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.model.DeviceRoleCatalog;
import com.jdroidx.jfindme.data.model.DeviceRoleEntry;
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.jdroidx.jfindme.data.repository.PairingRepository;
import com.jdroidx.jfindme.engine.security.PrivacyEngine;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.map.MapActivity;
import com.jdroidx.jfindme.ui.management.GeofenceActivity;
import com.jdroidx.jfindme.ui.settings.ProfileActivity;
import com.jdroidx.jfindme.utils.AngelLoader;
import com.jdroidx.jfindme.utils.AvatarLoader;
import com.jdroidx.jfindme.utils.DeviceIdentityHelper;
import com.jdroidx.jfindme.utils.RegistryManager;
import com.jdroidx.jfindme.utils.SystemLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 👨‍👩‍👧‍👦 ENTERPRISE FAMILY & FRIENDS RELATIONAL ACTIVITY (L7 OOPS-MVVM)
 * Human-in-the-Loop Architectural Due Diligence:
 * 1. Dual-Tab Navigation with Dynamic Member Count Badges.
 * 2. Instant Search with Clear (X) trigger & Multi-Criteria Sorting (All, Online First, Battery Warning).
 * 3. Interactive Member Cards with Vitals (🟢 Online/🔴 Offline, Battery Pills, Role Tags).
 * 4. 1-Tap Quick Action Bar (Live Map Pin, Remote Acoustic Siren Ring, Role Switcher, Safe Zone).
 * 5. Full Closed-Loop SQLite + Firebase RTDB State Synchronization.
 */
public class FamilyFriendsActivity extends BaseActivityJava {

    private static final String TAG = "FamilyFriendsActivity";

    // Tab Constants
    private static final int TAB_FAMILY = 0;
    private static final int TAB_FRIENDS = 1;
    private int activeTab = TAB_FAMILY;

    // Filter / Sort Constants
    private static final int SORT_DEFAULT = 0;
    private static final int SORT_ONLINE_FIRST = 1;
    private static final int SORT_BATTERY_LOW = 2;
    private static final int SORT_ALPHABETICAL = 3;
    private int activeSortMode = SORT_DEFAULT;

    // UI View References
    private LinearLayout rosterContainer;
    private ScrollView mainScrollView;
    private ImageView ownerAvatar;
    private TextView tvOwnerName;
    private TextView tvFamily, tvFriends;
    private LinearLayout tabFamily, tabFriends;
    private EditText etSearchInput;
    private ImageView ivClearSearch;

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // 300ms Search Debounce Pipeline: Eliminates main-thread view rebuild stutter on rapid typing
    private final android.os.Handler searchDebounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    // Architectural MVVM
    private FamilyFriendsViewModel viewModel;
    private List<RosterNode> currentRoster = new ArrayList<>();
    private boolean hasReceivedRosterData = false;
    private String searchFilter = "";
    private final Map<String, Boolean> collapsedBranches = new HashMap<>();
    private String requestedFocusCategory = null;
    private TextView tvHeaderTitle;
    private TextView tvHeaderSubtitle;

    @Override
    protected View createLayout() {
        Intent it = getIntent();
        if (it != null) {
            if (it.hasExtra("REQUESTED_TAB")) {
                activeTab = it.getIntExtra("REQUESTED_TAB", TAB_FAMILY);
            }
            if (it.hasExtra("FOCUS_CATEGORY")) {
                requestedFocusCategory = it.getStringExtra("FOCUS_CATEGORY");
            }
        }

        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        int masterPad = getResponsivePaddingQuota();

        // ─── 1. HERO HEADER WITH QUICK SYNC & ADD ACTIONS ───────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        applyGlobalHeaderGeometry(header);

        LinearLayout titleCluster = new LinearLayout(this);
        titleCluster.setOrientation(LinearLayout.VERTICAL);
        titleCluster.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        tvHeaderTitle = new TextView(this);
        tvHeaderTitle.setText("FAMILY & FRIENDS");
        tvHeaderTitle.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, tvHeaderTitle, "TEXT_HEADER");
        titleCluster.addView(tvHeaderTitle);

        tvHeaderSubtitle = new TextView(this);
        tvHeaderSubtitle.setText("Relational Organization & Telemetry Mesh");
        tvHeaderSubtitle.setTextColor(getGradientOverlayText());
        tvHeaderSubtitle.setAlpha(0.85f);
        UiArchitect.applyText(this, tvHeaderSubtitle, "TEXT_SUB");
        titleCluster.addView(tvHeaderSubtitle);

        // Apply contextual titles and initial branch focus
        if ("CHILD".equalsIgnoreCase(requestedFocusCategory)) {
            tvHeaderTitle.setText("CHILDREN DEVICES");
            tvHeaderSubtitle.setText("Monitored Dependents & Kids Roster");
            collapsedBranches.put("👑 CO-GUARDIANS & PARENTS", true);
            collapsedBranches.put("💍 SPOUSE ENTITIES", true);
            collapsedBranches.put("🧸 DEPENDENT TARGETS (CHILDREN)", false);
        } else if ("GUARDIAN".equalsIgnoreCase(requestedFocusCategory)) {
            tvHeaderTitle.setText("GUARDIANS & FAMILY");
            tvHeaderSubtitle.setText("Primary Caregivers & Family Roster");
            collapsedBranches.put("👑 CO-GUARDIANS & PARENTS", false);
            collapsedBranches.put("💍 SPOUSE ENTITIES", true);
            collapsedBranches.put("🧸 DEPENDENT TARGETS (CHILDREN)", true);
        } else if ("FRIEND".equalsIgnoreCase(requestedFocusCategory) || activeTab == TAB_FRIENDS) {
            tvHeaderTitle.setText("MEETMYFRIEND GROUPS");
            tvHeaderSubtitle.setText("Social Circles & Peer Network");
            activeTab = TAB_FRIENDS;
        }

        header.addView(titleCluster);

        // Header Action 0: Privacy Mode Selector
        ImageView btnPrivacy = new ImageView(this);
        btnPrivacy.setImageResource(android.R.drawable.ic_menu_view);
        btnPrivacy.setColorFilter(getGradientOverlayText());
        btnPrivacy.setPadding(scale(10), scale(10), scale(10), scale(10));
        btnPrivacy.setClickable(true);
        btnPrivacy.setFocusable(true);
        btnPrivacy.setOnClickListener(v -> showPrivacyTierDialog());
        header.addView(btnPrivacy, new LinearLayout.LayoutParams(scale(48), scale(48)));

        // Header Action 1: Refresh / Re-Sync Socket Button
        ImageView btnSync = new ImageView(this);
        btnSync.setImageResource(android.R.drawable.ic_popup_sync);
        btnSync.setColorFilter(getGradientOverlayText());
        btnSync.setPadding(scale(10), scale(10), scale(10), scale(10));
        btnSync.setClickable(true);
        btnSync.setFocusable(true);
        btnSync.setOnClickListener(v -> {
            Toast.makeText(this, "🔄 Synchronizing relational matrix...", Toast.LENGTH_SHORT).show();
            if (viewModel != null) viewModel.initializeCloudSync(this);
        });
        header.addView(btnSync, new LinearLayout.LayoutParams(scale(48), scale(48)));

        // Header Action 2: Add / Pair Member FAB
        ImageView btnAdd = new ImageView(this);
        btnAdd.setImageResource(android.R.drawable.ic_input_add);
        btnAdd.setColorFilter(getGradientOverlayText());
        btnAdd.setPadding(scale(10), scale(10), scale(10), scale(10));
        btnAdd.setClickable(true);
        btnAdd.setFocusable(true);
        btnAdd.setOnClickListener(v -> showAddMemberOptionsDialog());
        header.addView(btnAdd, new LinearLayout.LayoutParams(scale(48), scale(48)));

        mainRoot.addView(header);

        // ─── 2. DUAL-TAB CONTROLLER WITH DYNAMIC BADGES ─────────────────────────
        LinearLayout tabController = new LinearLayout(this);
        tabController.setOrientation(LinearLayout.HORIZONTAL);
        tabController.setGravity(Gravity.CENTER);
        tabController.setPadding(masterPad, masterPad / 3, masterPad, masterPad / 3);

        // Tab A: FAMILY
        tabFamily = new LinearLayout(this);
        tabFamily.setOrientation(LinearLayout.HORIZONTAL);
        tabFamily.setGravity(Gravity.CENTER);
        int tapPad = scale(12);
        tabFamily.setPadding(tapPad * 2, tapPad, tapPad * 2, tapPad);
        tabFamily.setFocusable(true);
        tabFamily.setClickable(true);

        tvFamily = new TextView(this);
        tvFamily.setText("FAMILY");
        tvFamily.setTextSize(14);
        tvFamily.setTypeface(null, Typeface.BOLD);
        tabFamily.addView(tvFamily);

        // Tab B: FRIENDS
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
        tabLp.setMargins(scale(6), 0, scale(6), 0);

        UiArchitect.applyCardWithOverride(this, tabFamily, "CARD_CAPSULE_BUTTON");
        UiArchitect.applyCardWithOverride(this, tabFriends, "CARD_CAPSULE_BUTTON");

        tabController.addView(tabFamily, tabLp);
        tabController.addView(tabFriends, tabLp);

        mainRoot.addView(tabController);

        // ─── 3. OWNER IDENTITY TILE ─────────────────────────────────────────────
        mainRoot.addView(buildIdentityBlock(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mainScrollView = new ScrollView(this);
        rosterContainer = new LinearLayout(this);
        rosterContainer.setOrientation(LinearLayout.VERTICAL);
        rosterContainer.setPadding(masterPad, masterPad / 2, masterPad, masterPad * 4);

        mainScrollView.addView(rosterContainer);
        mainRoot.addView(mainScrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        tabFamily.setOnClickListener(v -> renderFamilyTab());
        tabFriends.setOnClickListener(v -> renderFriendsTab());

        LinearLayout bottomActionDock = new LinearLayout(this);
        bottomActionDock.setOrientation(LinearLayout.VERTICAL);
        bottomActionDock.setPadding(masterPad, scale(6), masterPad, scale(12));

        TextView btnPairUnified = new TextView(this);
        btnPairUnified.setText("🔗 PAIR NEW DEVICE");
        btnPairUnified.setPadding(scale(24), scale(15), scale(24), scale(15));
        btnPairUnified.setGravity(Gravity.CENTER);
        btnPairUnified.setTypeface(null, Typeface.BOLD);
        btnPairUnified.setTextSize(14);
        UiArchitect.applyCardWithOverride(this, btnPairUnified, "CARD_CAPSULE_BUTTON");
        btnPairUnified.setOnClickListener(v -> startActivity(new Intent(this, NetworkGatewayActivity.class)));
        bottomActionDock.addView(btnPairUnified, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView btnReclaimSubtle = new TextView(this);
        btnReclaimSubtle.setText("🔄 Restore Cloud Circle / Re-Sync Roster");
        btnReclaimSubtle.setTextColor(getSecondaryTextColor());
        btnReclaimSubtle.setTextSize(11);
        btnReclaimSubtle.setGravity(Gravity.CENTER);
        btnReclaimSubtle.setPadding(0, scale(6), 0, 0);
        btnReclaimSubtle.setClickable(true);
        btnReclaimSubtle.setFocusable(true);
        btnReclaimSubtle.setOnClickListener(v -> triggerRosterReclamation());
        bottomActionDock.addView(btnReclaimSubtle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mainRoot.addView(bottomActionDock);

        String[] navKeys = {"NAV_NOTICE", "NAV_DEVICE", "NAV_ME"};
        mainRoot.addView(UiArchitect.createBottomNav(this, navKeys, "NAV_DEVICE"));

        return mainRoot;
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(FamilyFriendsViewModel.class);
        viewModel.initializeCloudSync(this);

        viewModel.getRosterLiveData().observe(this, rosterNodes -> {
            if (rosterNodes == null) return;
            hasReceivedRosterData = true;
            currentRoster = new ArrayList<>(rosterNodes);
            updateTabBadges();
            if (activeTab == TAB_FAMILY) {
                renderFamilyTab();
            } else {
                renderFriendsTab();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.initializeCloudSync(this);
        if (ownerAvatar != null) AvatarLoader.bindCurrentUserAvatar(this, ownerAvatar);
        if (tvOwnerName != null) {
            String nameStr = RegistryManager.getUserNickname(this);
            tvOwnerName.setText("👤 " + (nameStr.isEmpty() ? "Owner" : nameStr));
        }
    }

    private void updateTabBadges() {
        int familyCount = 0;
        int friendsCount = 0;

        String myUid = FirebaseAuth.getInstance().getUid();
        String finalAgentId = RegistryManager.getAgentId(this);
        DatabaseHelper dbLocal = DatabaseHelper.getInstance(this);
        String resolvedLocalAgentId = (myUid != null) ? dbLocal.resolveAgentIdLocally(myUid) : null;

        for (RosterNode n : currentRoster) {
            String uid = n.getUid();
            if (uid.equals(myUid) || uid.equals(finalAgentId) || (resolvedLocalAgentId != null && uid.equals(resolvedLocalAgentId))) {
                continue;
            }
            if (n.isFriend()) {
                friendsCount++;
            } else {
                familyCount++;
            }
        }

        tvFamily.setText("FAMILY (" + familyCount + ")");
        tvFriends.setText("FRIENDS (" + friendsCount + ")");
    }

    private void renderFamilyTab() {
        activeTab = TAB_FAMILY;
        UiArchitect.applySelection(this, tabFamily, true);
        UiArchitect.applySelection(this, tabFriends, false);
        rosterContainer.removeAllViews();

        rosterContainer.addView(buildSearchAndFilterCluster());
        rosterContainer.addView(buildRootGuardianAnchor());

        // Show loading skeleton only on fresh start before any roster snapshot arrives
        if (!hasReceivedRosterData && (currentRoster == null || currentRoster.isEmpty())) {
            View loading = buildLoadingSkeleton("SYNCING_ROSTER");
            rosterContainer.addView(loading);
            return;
        }

    List<RosterNode> admins = new ArrayList<>();
        List<RosterNode> spouses = new ArrayList<>();
        List<RosterNode> children = new ArrayList<>();

        String myUid = FirebaseAuth.getInstance().getUid();
        String finalAgentId = RegistryManager.getAgentId(this);
        SharedPreferences ffPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String myDevUid = ffPrefs.getString("device_uid", "");
        DatabaseHelper dbLocal = DatabaseHelper.getInstance(this);
        String resolvedLocalAgentId = (myUid != null) ? dbLocal.resolveAgentIdLocally(myUid) : null;

        for (RosterNode n : currentRoster) {
            String uid = n.getUid();
            if (uid.equals(myUid) || uid.equals(finalAgentId) || (!myDevUid.isEmpty() && uid.equals(myDevUid)) || (resolvedLocalAgentId != null && uid.equals(resolvedLocalAgentId))) {
                continue;
            }
            if (matchesSearch(n) && !n.isFriend()) {
                String cat = n.getCategory();
                if ("GUARDIAN".equals(cat)) admins.add(n);
                else if ("SPOUSE".equals(cat)) spouses.add(n);
                else children.add(n);
            }
        }

        applySorting(admins);
        applySorting(spouses);
        applySorting(children);

        if (admins.isEmpty() && spouses.isEmpty() && children.isEmpty()) {
            rosterContainer.addView(buildEmptyRosterView(true, !searchFilter.isEmpty()));
        } else {
            // 🧸 PRIME OBJECTIVE: Dependent Children (Riddhi) rendered FIRST at the very top of Roster!
            if (!children.isEmpty() || searchFilter.isEmpty()) {
                renderCollapsibleTreeBranch("🧸 DEPENDENT TARGETS (CHILDREN)", children, "Child");
            }
            if (!admins.isEmpty() || searchFilter.isEmpty()) {
                renderCollapsibleTreeBranch("👑 CO-GUARDIANS & PARENTS", admins, "Guardian");
            }
            if (!spouses.isEmpty() || searchFilter.isEmpty()) {
                renderCollapsibleTreeBranch("💍 SPOUSE ENTITIES", spouses, "Spouse");
            }
        }
    }

    private void renderFriendsTab() {
        activeTab = TAB_FRIENDS;
        UiArchitect.applySelection(this, tabFamily, false);
        UiArchitect.applySelection(this, tabFriends, true);
        rosterContainer.removeAllViews();

        // Show loading skeleton only on fresh start before any roster snapshot arrives
        if (!hasReceivedRosterData && (currentRoster == null || currentRoster.isEmpty())) {
            View loading = buildLoadingSkeleton("SYNCING_FRIENDS");
            rosterContainer.addView(loading);
            return;
        }

        rosterContainer.addView(buildSearchAndFilterCluster());

        List<RosterNode> friends = new ArrayList<>();
        String myUid = FirebaseAuth.getInstance().getUid();
        String finalAgentId = RegistryManager.getAgentId(this);
        DatabaseHelper dbLocal = DatabaseHelper.getInstance(this);
        String resolvedLocalAgentId = (myUid != null) ? dbLocal.resolveAgentIdLocally(myUid) : null;

        for (RosterNode n : currentRoster) {
            String uid = n.getUid();
            if (uid.equals(myUid) || uid.equals(finalAgentId) || (resolvedLocalAgentId != null && uid.equals(resolvedLocalAgentId))) {
                continue;
            }
            if (matchesSearch(n) && n.isFriend()) {
                friends.add(n);
            }
        }

        applySorting(friends);
        if (friends.isEmpty()) {
            rosterContainer.addView(buildEmptyRosterView(false, !searchFilter.isEmpty()));
        } else {
            renderCollapsibleTreeBranch("🌐 EXTENDED FRIEND MESH (P2P)", friends, "Friend");
        }
    }

    private View buildEmptyRosterView(boolean isFamilyTab, boolean isFiltered) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        int pad = scale(24);
        card.setPadding(pad, pad * 2, pad, pad * 2);
        UiArchitect.applyCard(this, card, "CARD_STANDARD");

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, scale(16), 0, scale(24));
        card.setLayoutParams(clp);

        ImageView iv = new ImageView(this);
        iv.setImageResource(isFiltered ? android.R.drawable.ic_menu_search : (isFamilyTab ? com.jdroidx.jfindme.R.drawable.ic_people : android.R.drawable.ic_menu_share));
        iv.setColorFilter(getGradientStart());
        int is = scale(48);
        card.addView(iv, new LinearLayout.LayoutParams(is, is));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(isFiltered ? "No Matching Members" : (isFamilyTab ? "No Family Members Linked" : "No Friends In Mesh"));
        tvTitle.setTextColor(getTextColor());
        tvTitle.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_TITLE"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, scale(12), 0, scale(6));
        card.addView(tvTitle);

        TextView tvSub = new TextView(this);
        if (isFiltered) {
            tvSub.setText("No active devices match \"" + searchFilter + "\". Check for typos or reset your filter.");
        } else if (isFamilyTab) {
            tvSub.setText("Link your children, spouse, or co-guardians to track their live coordinates, battery health, and safe zone departures.");
        } else {
            tvSub.setText("Pair with friends nearby or connect via cloud invite to create a temporary peer-to-peer tracking mesh.");
        }
        tvSub.setTextColor(getSecondaryTextColor());
        tvSub.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_SUB"));
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(scale(16), 0, scale(16), scale(16));
        card.addView(tvSub);

        if (isFiltered) {
            View btnClear = UiArchitect.createOutlinedButton(this, "CLEAR SEARCH FILTER", android.R.drawable.ic_menu_close_clear_cancel, v -> {
                if (etSearchInput != null) etSearchInput.setText("");
                searchFilter = "";
            });
            card.addView(btnClear);
        } else {
            View btnPair = UiArchitect.createPrimaryButton(this, isFamilyTab ? "+ PAIR FAMILY DEVICE" : "+ ADD FRIEND P2P", android.R.drawable.ic_menu_add, v -> {
                Intent intent = new Intent(this, NetworkGatewayActivity.class);
                intent.putExtra("LAUNCH_MODE", isFamilyTab ? "FAMILY_PAIR" : "FRIEND_PAIR");
                startActivity(intent);
            });
            card.addView(btnPair);
        }

        return card;
    }

    private View buildLoadingSkeleton(String state) {
        LinearLayout loading = new LinearLayout(this);
        loading.setOrientation(LinearLayout.VERTICAL);
        loading.setGravity(Gravity.CENTER);
        int pad = scale(32);
        loading.setPadding(pad, pad * 2, pad, pad * 2);
        UiArchitect.applyCard(this, loading, "CARD_STANDARD");

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, scale(16), 0, scale(24));
        loading.setLayoutParams(lp);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        loading.addView(progressBar, new LinearLayout.LayoutParams(scale(48), scale(48)));

        TextView tvLoading = new TextView(this);
        tvLoading.setText(state.equals("SYNCING_ROSTER") ? "Sincronizando lista..." : "Cargando...");
        tvLoading.setTextColor(getSecondaryTextColor());
        tvLoading.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_SUB"));
        tvLoading.setGravity(Gravity.CENTER);
        tvLoading.setPadding(0, scale(12), 0, 0);
        loading.addView(tvLoading);

        return loading;
    }

    private void applySorting(List<RosterNode> list) {
        if (list == null || list.size() <= 1) return;
        switch (activeSortMode) {
            case SORT_ONLINE_FIRST:
                Collections.sort(list, (a, b) -> {
                    boolean aChild = isChildOrPrimeNode(a);
                    boolean bChild = isChildOrPrimeNode(b);
                    if (aChild != bChild) return aChild ? -1 : 1;
                    return Boolean.compare(b.isOnline(), a.isOnline());
                });
                break;
            case SORT_BATTERY_LOW:
                Collections.sort(list, (a, b) -> {
                    boolean aChild = isChildOrPrimeNode(a);
                    boolean bChild = isChildOrPrimeNode(b);
                    if (aChild != bChild) return aChild ? -1 : 1;
                    return Integer.compare(a.getBatteryPercent(), b.getBatteryPercent());
                });
                break;
            case SORT_ALPHABETICAL:
                Collections.sort(list, (a, b) -> {
                    boolean aChild = isChildOrPrimeNode(a);
                    boolean bChild = isChildOrPrimeNode(b);
                    if (aChild != bChild) return aChild ? -1 : 1;
                    return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
                });
                break;
            default:
                Collections.sort(list, (a, b) -> {
                    boolean aChild = isChildOrPrimeNode(a);
                    boolean bChild = isChildOrPrimeNode(b);
                    if (aChild != bChild) return aChild ? -1 : 1;
                    return 0;
                });
                break;
        }
    }

    /**
     * 🧸 PRIME OBJECTIVE: Evaluates whether a roster node is a Child / Protected Ward target (dynamic SSOT via RegistryManager)
     */
    private boolean isChildOrPrimeNode(RosterNode n) {
        if (n == null) return false;
        String uid = n.getUid();
        String primeTarget = com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(this);
        if (primeTarget.equalsIgnoreCase(uid)) return true;
        String name = n.getDisplayName();
        if (name != null && name.toUpperCase(Locale.US).contains("RIDDHI")) return true;
        String cat = n.getCategory();
        if ("DEPENDENT_CHILD".equalsIgnoreCase(cat)) return true;
        String role = n.getRoleTitle();
        return role != null && (role.contains("Child") || role.contains("Daughter") || 
            role.contains("Son") || role.contains("Ward") || role.contains("Dependent"));
    }

    private boolean matchesSearch(RosterNode node) {
        if (searchFilter.isEmpty()) return true;
        String query = searchFilter.toLowerCase(Locale.getDefault());
        return node.getDisplayName().toLowerCase(Locale.getDefault()).contains(query)
            || node.getRoleTitle().toLowerCase(Locale.getDefault()).contains(query);
    }

    private View buildSearchAndFilterCluster() {
        LinearLayout cluster = new LinearLayout(this);
        cluster.setOrientation(LinearLayout.VERTICAL);
        cluster.setPadding(0, 0, 0, scale(12));

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, searchBar, "CARD_STANDARD");
        searchBar.setPadding(scale(16), scale(4), scale(16), scale(4));

        ImageView searchIcon = new ImageView(this);
        searchIcon.setImageResource(android.R.drawable.ic_menu_search);
        searchIcon.setColorFilter(getSecondaryTextColor());
        searchBar.addView(searchIcon, new LinearLayout.LayoutParams(scale(24), scale(24)));

        etSearchInput = new EditText(this);
        etSearchInput.setHint("Search family & friends...");
        etSearchInput.setHintTextColor(getSecondaryTextColor());
        etSearchInput.setTextColor(getTextColor());
        etSearchInput.setTextSize(13);
        etSearchInput.setBackground(null);
        etSearchInput.setSingleLine(true);
        etSearchInput.setPadding(scale(12), scale(8), scale(12), scale(8));
        etSearchInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchFilter = s.toString().trim();
                ivClearSearch.setVisibility(searchFilter.isEmpty() ? View.GONE : View.VISIBLE);
                if (searchDebounceRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                }
                searchDebounceRunnable = () -> {
                    if (activeTab == TAB_FAMILY) renderFamilyTab(); else renderFriendsTab();
    
                };
                searchDebounceHandler.postDelayed(searchDebounceRunnable, 300);

            }
            @Override public void afterTextChanged(Editable s) {}
        });
        searchBar.addView(etSearchInput);

        ivClearSearch = new ImageView(this);
        ivClearSearch.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        ivClearSearch.setColorFilter(getSecondaryTextColor());
        ivClearSearch.setVisibility(searchFilter.isEmpty() ? View.GONE : View.VISIBLE);
        ivClearSearch.setPadding(scale(8), scale(8), scale(8), scale(8));
        ivClearSearch.setClickable(true);
        ivClearSearch.setFocusable(true);
        ivClearSearch.setOnClickListener(v -> {
            etSearchInput.setText("");
            searchFilter = "";
        });
        searchBar.addView(ivClearSearch, new LinearLayout.LayoutParams(scale(36), scale(36)));

        ImageView sortIcon = new ImageView(this);
        sortIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
        sortIcon.setColorFilter(getGradientStart());
        sortIcon.setPadding(scale(8), scale(8), scale(8), scale(8));
        sortIcon.setClickable(true);
        sortIcon.setFocusable(true);
        sortIcon.setOnClickListener(v -> showSortOptionsDialog());
        searchBar.addView(sortIcon, new LinearLayout.LayoutParams(scale(36), scale(36)));

        cluster.addView(searchBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return cluster;
    }

    private void showSortOptionsDialog() {
        UiArchitect.OptionItem[] options = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("⚡ Default Order", "Display members by registration order", android.R.drawable.ic_menu_sort_by_size),
            new UiArchitect.OptionItem("🟢 Online Devices First", "Prioritize currently active and broadcasting nodes", com.jdroidx.jfindme.R.drawable.ic_nav_map),
            new UiArchitect.OptionItem("🔋 Low Battery Warning First (<20%)", "Show critical battery devices at the top for guardian attention", com.jdroidx.jfindme.R.drawable.ic_device_health),
            new UiArchitect.OptionItem("🔤 Alphabetical (A-Z)", "Sort roster alphabetically by member name", android.R.drawable.ic_menu_sort_alphabetically)
        };

        UiArchitect.showRichOptionsDialog(this, "Sort Device Roster", options, (which, label) -> {
            activeSortMode = which;
            if (activeTab == TAB_FAMILY) renderFamilyTab();
            else renderFriendsTab();
        });
    }

    private View buildRootGuardianAnchor() {
        LinearLayout rootCard = new LinearLayout(this);
        rootCard.setOrientation(LinearLayout.HORIZONTAL);
        rootCard.setGravity(Gravity.CENTER_VERTICAL);
        rootCard.setPadding(scale(24), scale(20), scale(24), scale(20));

        UiArchitect.applyCardWithOverride(this, rootCard, "CARD_DASHBOARD", 1.0f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(20));
        rootCard.setLayoutParams(lp);

        rootCard.setClickable(true);
        rootCard.setFocusable(true);
        rootCard.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        ImageView rootAvatar = new ImageView(this);
        AvatarLoader.bindCurrentUserAvatar(this, rootAvatar);
        GradientDrawable rootRing = new GradientDrawable();
        rootRing.setShape(GradientDrawable.OVAL);
        rootRing.setColor(Color.parseColor("#1A000000"));
        rootRing.setStroke(scale(3), getGradientStart());
        rootAvatar.setBackground(rootRing);
        int universalSize = UiArchitect.getUniversalAvatarSize(this);
        rootCard.addView(rootAvatar, new LinearLayout.LayoutParams(universalSize, universalSize));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(scale(20), 0, 0, 0);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        String myName = RegistryManager.getUserNickname(this);
        if (myName.isEmpty()) myName = "You";

        SharedPreferences prefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String userRole = prefs.getString("role_family", "Primary Guardian");
        if (userRole == null || userRole.equals("None")) userRole = "Primary Guardian";

        TextView tvTitle = new TextView(this);
        tvTitle.setText(myName);
        tvTitle.setTextColor(getTextColor());
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        text.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("👑 " + userRole + "  •  🟢 Active Network Anchor");
        tvSub.setTextColor(Color.parseColor("#00E676"));
        tvSub.setTextSize(11);
        text.addView(tvSub);

        rootCard.addView(text);

        ImageView arrow = new ImageView(this);
        arrow.setImageResource(android.R.drawable.ic_media_play);
        arrow.setColorFilter(getGradientStart());
        rootCard.addView(arrow, new LinearLayout.LayoutParams(scale(24), scale(24)));

        return rootCard;
    }

    private void renderCollapsibleTreeBranch(String branchTitle, List<RosterNode> nodes, String branchType) {
        boolean isCollapsed = Boolean.TRUE.equals(collapsedBranches.get(branchTitle));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(scale(10), scale(12), scale(10), scale(8));
        headerRow.setClickable(true);
        headerRow.setFocusable(true);

        TextView tvHeader = new TextView(this);
        tvHeader.setText(branchTitle + " (" + nodes.size() + ")");
        tvHeader.setTextColor(getGradientStart());
        tvHeader.setTextSize(12);
        tvHeader.setTypeface(null, Typeface.BOLD);
        tvHeader.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        headerRow.addView(tvHeader);

        TextView tvCaret = new TextView(this);
        tvCaret.setText(isCollapsed ? "▼ EXPAND" : "▲ COLLAPSE");
        tvCaret.setTextColor(getSecondaryTextColor());
        tvCaret.setTextSize(11);
        headerRow.addView(tvCaret);

        headerRow.setOnClickListener(v -> {
            collapsedBranches.put(branchTitle, !isCollapsed);
            if (activeTab == TAB_FAMILY) renderFamilyTab();
            else renderFriendsTab();
        });

        rosterContainer.addView(headerRow);

        if (isCollapsed) return;

        if (nodes.isEmpty()) {
            TextView emptyRow = new TextView(this);
            emptyRow.setText("   └── (No " + branchType.toLowerCase(Locale.getDefault()) + "s linked)");
            emptyRow.setTextColor(getSecondaryTextColor());
            emptyRow.setTextSize(12);
            emptyRow.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC);
            emptyRow.setPadding(scale(16), scale(4), scale(16), scale(8));
            rosterContainer.addView(emptyRow);
            return;
        }

        for (int i = 0; i < nodes.size(); i++) {
            RosterNode n = nodes.get(i);
            boolean isLast = (i == nodes.size() - 1);
            rosterContainer.addView(createTreeNodeCard(n, isLast));
        }
    }

    private View createTreeNodeCard(RosterNode node, boolean isLast) {
        LinearLayout branchRow = new LinearLayout(this);
        branchRow.setOrientation(LinearLayout.HORIZONTAL);
        branchRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView treeLine = new TextView(this);
        treeLine.setText(isLast ? " └── " : " ├── ");
        treeLine.setTextColor(getGradientStart());
        treeLine.setTextSize(16);
        treeLine.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        treeLine.setPadding(scale(8), 0, scale(4), 0);
        branchRow.addView(treeLine);

        LinearLayout logCard = new LinearLayout(this);
        logCard.setOrientation(LinearLayout.HORIZONTAL);
        logCard.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, logCard, "CARD_STANDARD");

        int batt = (node.getBatteryPercent() > 0) ? node.getBatteryPercent() : 100;
        long lastSeenMs = node.getLastSeenTimestamp() > 0L ? node.getLastSeenTimestamp() : (node.isOnline() ? System.currentTimeMillis() : 0L);
        com.jdroidx.jfindme.utils.DeviceRagEngine.EvaluationResult rag = 
            com.jdroidx.jfindme.utils.DeviceRagEngine.evaluate(lastSeenMs, batt, false, false, false);

        logCard.setAlpha(rag.getCardAlpha());

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        cardLp.setMargins(scale(4), scale(4), scale(4), scale(4));
        logCard.setLayoutParams(cardLp);
        int logPad = getResponsivePaddingQuota();
        logCard.setPadding(logPad, logPad / 2, logPad, logPad / 2);
        logCard.setClickable(true);
        logCard.setFocusable(true);
        logCard.setOnClickListener(v -> showNodeQuickActionsDialog(node));

        // [Variables] node, avId, avatar, rag | [Constants] "butterfly" | [Dependencies] DatabaseHelper, AvatarLoader | [Integration Points] SSOT Family & Friends Roster Avatar
        String avId = node.getAvatarId();
        if (avId == null || avId.isEmpty() || avId.equals("1")) {
            avId = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getDeviceCentricAvatar(node.getUid());
        }
        if (avId == null || avId.isEmpty()) avId = "butterfly";

        ImageView avatar = new ImageView(this);
        AvatarLoader.bindAvatar(this, avatar, node.getUid(), avId, rag.isDesaturated());
        GradientDrawable ringDrawable = new GradientDrawable();
        ringDrawable.setShape(GradientDrawable.OVAL);
        ringDrawable.setColor(Color.parseColor("#1A000000"));
        ringDrawable.setStroke(scale(3), rag.getStrokeColor());
        avatar.setBackground(ringDrawable);

        int universalSize = UiArchitect.getUniversalAvatarSize(this);
        logCard.addView(avatar, new LinearLayout.LayoutParams(universalSize, universalSize));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(scale(16), 0, 0, 0);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        String display = node.getDisplayName().trim();
        // [Variables] display, node | [Constants] None | [Dependencies] DatabaseHelper | [Integration Points] SSOT Name Resolution Fallback
        if (display.isEmpty() || display.startsWith("Agent_") || display.startsWith("HW_")) {
            String ssotName = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getDeviceCentricName(node.getUid());
            if (ssotName != null && !ssotName.trim().isEmpty() && !ssotName.startsWith("Agent_")) {
                display = ssotName.trim();
            }
        }
        boolean isRawToken = display.startsWith("HW_") || display.startsWith("Agent_") || display.length() > 18;
        String friendlyTitle = isRawToken ? "Paired Device (" + (display.length() > 10 ? display.substring(0, 10) + "..." : display) + ")" : display;

        TextView tvTitle = new TextView(this);
        tvTitle.setText(friendlyTitle);
        tvTitle.setTextColor(rag.isDesaturated() ? getSecondaryTextColor() : getTextColor());
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        textBlock.addView(tvTitle);

        if (isRawToken) {
            TextView tvRenameHint = new TextView(this);
            tvRenameHint.setText("✏️ Tap to assign custom name");
            tvRenameHint.setTextColor(getSecondaryTextColor());
            tvRenameHint.setTextSize(10);
            tvRenameHint.setPadding(0, scale(1), 0, scale(2));
            textBlock.addView(tvRenameHint);
        }

        TextView tvDetail = new TextView(this);
        String roleText = node.getRoleTitle();
        // [Variables] | [Constants] | [Dependencies] | [Integration Points]
        // Telemetry Privacy Gate: Visual indicator if node has tracking paused via ACL
        boolean isTrackingActive = (node.getPermissionsMask() & 0x01) != 0;
        String permStatus = isTrackingActive ? "" : "  \u2022  \uD83D\uDEAB GPS Paused";
        tvDetail.setText(roleText + permStatus + "  •  " + rag.getStatusText());
        tvDetail.setTextColor(rag.getStatusColor());
        tvDetail.setTextSize(11);
        textBlock.addView(tvDetail);

        // 🛡️ SAFE ZONE TREE LINK: Displays assigned Safe Zone name and address for the respective device
        if (node.getSafeZoneName() != null && !node.getSafeZoneName().isEmpty()) {
            LinearLayout szRow = new LinearLayout(this);
            szRow.setOrientation(LinearLayout.HORIZONTAL);
            szRow.setGravity(Gravity.CENTER_VERTICAL);
            szRow.setPadding(0, scale(3), 0, 0);

            TextView szText = new TextView(this);
            String szAddress = (node.getSafeZoneAddress() != null && !node.getSafeZoneAddress().isEmpty()) ? " • " + node.getSafeZoneAddress() : "";
            szText.setText("🛡️ Safe Zone: " + node.getSafeZoneName() + szAddress);
            szText.setTextColor(getGradientStart());
            szText.setTextSize(10.5f);
            szText.setTypeface(null, Typeface.BOLD);
            szText.setMaxLines(1);
            szText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            szRow.addView(szText);

            textBlock.addView(szRow);
        }

        logCard.addView(textBlock);

        // Action 1: Direct Map Locate Shortcut
        ImageView locateBtn = new ImageView(this);
        locateBtn.setImageResource(android.R.drawable.ic_dialog_map);
        locateBtn.setColorFilter(getGradientStart());
        locateBtn.setPadding(scale(12), scale(12), scale(12), scale(12));
        locateBtn.setClickable(true);
        locateBtn.setFocusable(true);
        locateBtn.setOnClickListener(v -> {
            boolean isLiveActive = (node.getPermissionsMask() & 0x01) != 0;
            if (!isLiveActive) {
                new AlertDialog.Builder(this)
                    .setTitle("\uD83D\uDEAB Live Tracking Paused")
                    .setMessage("Live GPS tracking is currently paused for " + node.getDisplayName() + " in Person Details privacy settings.\n\nEnable tracking in Person Details to view live markers on the map.")
                    .setPositiveButton("Manage Profile", (d, w) -> {
                        Intent pIntent = new Intent(this, PersonDetailsActivity.class);
                        pIntent.putExtra("TARGET_UID", node.getUid());
                        pIntent.putExtra("TARGET_ROLE", node.getRoleTitle());
                        startActivity(pIntent);
                    })
                    .setNegativeButton("Open Map Anyway", (d, w) -> {
                        Intent mapIntent = new Intent(this, MapActivity.class);
                        mapIntent.putExtra("TARGET_UID", node.getUid());
                        mapIntent.putExtra("target_uid", node.getUid());
                        startActivity(mapIntent);
                    })
                    .show();
                return;
            }
            Intent mapIntent = new Intent(this, MapActivity.class);
            mapIntent.putExtra("TARGET_UID", node.getUid());
            mapIntent.putExtra("target_uid", node.getUid());
            startActivity(mapIntent);
        });
        logCard.addView(locateBtn, new LinearLayout.LayoutParams(scale(56), scale(56)));

        // Action 2: More Options Dialog
        ImageView moreBtn = new ImageView(this);
        moreBtn.setImageResource(android.R.drawable.ic_menu_more);
        moreBtn.setColorFilter(getSecondaryTextColor());
        moreBtn.setPadding(scale(12), scale(12), scale(12), scale(12));
        moreBtn.setClickable(true);
        moreBtn.setFocusable(true);
        moreBtn.setOnClickListener(v -> showNodeQuickActionsDialog(node));
        logCard.addView(moreBtn, new LinearLayout.LayoutParams(scale(56), scale(56)));

        branchRow.addView(logCard);
        return branchRow;
    }

    private void showNodeQuickActionsDialog(RosterNode node) {
        // [Variables] isChild: boolean, actionsList: List<UiArchitect.OptionItem>, actions: UiArchitect.OptionItem[], actLabel: String
        // [Constants] OptionItem descriptions and icons
        // [Dependencies] UiArchitect, FirebaseComm, PairingRepository, DatabaseHelper, DevicePairingTable
        // [Integration Points] MapActivity, HistoryActivity, PersonDetailsActivity, GeofenceActivity, showRoleChangeDialog, showLinkCoParentDialog, confirmUnlinkDevice
        boolean isChild = node.getRoleTitle() != null && (
            "Child".equalsIgnoreCase(node.getRoleTitle())
            || node.getRoleTitle().toLowerCase().contains("child")
            || node.getRoleTitle().toLowerCase().contains("ward")
        );

        List<UiArchitect.OptionItem> actionsList = new ArrayList<>();
        actionsList.add(new UiArchitect.OptionItem("📱 Device Sharing", "Share " + node.getDisplayName() + " (Device B) to peer device(s) (Device C) with dynamic roles, multi-tickbox selection, or 1-code mesh sync", com.jdroidx.jfindme.R.drawable.ic_family_friends));
        actionsList.add(new UiArchitect.OptionItem("📍 Track on Live Map", "Open real-time GPS tracking stream", android.R.drawable.ic_dialog_map));
        actionsList.add(new UiArchitect.OptionItem("⏱️ View Location History", "View past telemetry logs and timeline", android.R.drawable.ic_menu_recent_history));
        actionsList.add(new UiArchitect.OptionItem("🔊 Ring Device (30s Acoustic Alarm)", "Override DND and trigger high-volume siren", android.R.drawable.ic_lock_idle_alarm));
        actionsList.add(new UiArchitect.OptionItem("✏️ Rename Device (Custom Callsign)", "Assign an identifiable custom name or nickname to this node", android.R.drawable.ic_menu_edit));
        actionsList.add(new UiArchitect.OptionItem("📝 View / Edit Dossier & Role", "Inspect full profile, permissions, and contacts", com.jdroidx.jfindme.R.drawable.ic_user_profile));
        actionsList.add(new UiArchitect.OptionItem("🛡️ Assign Safe Zone", "Configure geofence boundary perimeter", com.jdroidx.jfindme.R.drawable.ic_safe_zone));
        actionsList.add(new UiArchitect.OptionItem("👑 Change Relationship Category", "Reclassify as Guardian, Spouse, Child, or Friend", com.jdroidx.jfindme.R.drawable.ic_family_friends));
        actionsList.add(new UiArchitect.OptionItem("🗑️ Unlink Device from Group", "Revoke authorization and remove from network", android.R.drawable.ic_menu_delete));

        UiArchitect.OptionItem[] actions = actionsList.toArray(new UiArchitect.OptionItem[0]);

        UiArchitect.showRichOptionsDialog(this, node.getDisplayName() + " (" + node.getRoleTitle() + ")", actions, (which, label) -> {
            String actLabel = label != null ? label : "";
            if (actLabel.contains("Device Sharing") || actLabel.contains("Connect with Device C") || actLabel.contains("Connect Another Device") || actLabel.contains("Link Other Parent")) {
                showDeviceSharingHubForNode(node);
            } else if (actLabel.contains("Live Map")) {
                FirebaseComm.triggerRemoteLocateDevice(node.getUid());
                Intent mapIntent = new Intent(this, MapActivity.class);
                mapIntent.putExtra("TARGET_UID", node.getUid());
                mapIntent.putExtra("target_uid", node.getUid());
                startActivity(mapIntent);
            } else if (actLabel.contains("Location History")) {
                Intent historyIntent = new Intent(this, com.jdroidx.jfindme.ui.management.HistoryActivity.class);
                historyIntent.putExtra("TARGET_UID", node.getUid());
                historyIntent.putExtra("TARGET_NAME", node.getDisplayName());
                startActivity(historyIntent);
            } else if (actLabel.contains("Ring Device")) {
                if (!node.isOnline()) {
                    Toast.makeText(this, "⚠️ Device is currently offline — siren may not reach it", Toast.LENGTH_LONG).show();
                }
                int sirenDur = RegistryManager.getSirenDurationSec(this);
                FirebaseComm.triggerRemoteRingDevice(node.getUid(), sirenDur);
                Toast.makeText(this, "🔊 Acoustic siren triggered for " + node.getDisplayName() + " (" + sirenDur + "s)", Toast.LENGTH_LONG).show();
            } else if (actLabel.contains("Rename Device")) {
                showRenameNodeDialog(node);
            } else if (actLabel.contains("View / Edit Dossier")) {
                Intent intent = new Intent(this, PersonDetailsActivity.class);
                intent.putExtra("TARGET_UID", node.getUid());
                intent.putExtra("TARGET_ROLE", node.getRoleTitle());
                startActivity(intent);
            } else if (actLabel.contains("Assign Safe Zone")) {
                Intent safeZoneIntent = new Intent(this, GeofenceActivity.class);
                safeZoneIntent.putExtra("TARGET_UID", node.getUid());
                safeZoneIntent.putExtra("TARGET_NAME", node.getDisplayName());
                startActivity(safeZoneIntent);
            } else if (actLabel.contains("Change Relationship")) {
                showRoleChangeDialog(node);
            } else if (actLabel.contains("Connect Another Device")) {
                showConnectDeviceDialogForNode(node);
            } else if (actLabel.contains("Link Other Parent")) {
                showLinkCoParentDialog(node);
            } else if (actLabel.contains("Unlink Device")) {
                confirmUnlinkDevice(node);
            }
        });
    }

    /**
     * 📱 Consolidated Master Device Sharing Wizard for Family & Friends Roster
     * Unifies the 4 connection methods into a clean 3-step master flow:
     * Step 1: Device Selection (Peer C / Multi-Device / Co-Parent / 1-Code)
     * Step 2: Role Selection (Reciprocal Role B ↔ Role C)
     * Step 3: Closed-Loop Angel Butterfly Handshake with physical peer ACK and retry/exit watchdog
     */
    private void showDeviceSharingHubForNode(RosterNode node) {
        showConnectBToCDialogForNode(node);
    }

    // [Variables] selfAgentId, myGrp, code, msg | [Constants] None | [Dependencies] PairingRepository, AngelLoader, UiArchitect | [Integration Points] FamilyFriendsActivity, NetworkGatewayActivity
    private void generateMultiDevicePairingCodeForNode(String targetUid, String targetName) {
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);
        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Generating Multi-Device Pairing Code...");

        com.jdroidx.jfindme.data.repository.PairingRepository.startMultiDevicePairingSession(
            selfAgentId,
            targetUid,
            myGrp,
            "MEMBER",
            5,
            code -> {
                runOnUiThread(() -> {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                    if (code == null || code.trim().isEmpty()) {
                        UiArchitect.showValidationDialog(this, "Generation Failed", "Unable to generate pairing code. Please check network connection.", null);
                        return;
                    }
                    String msg = "6-Digit Pairing Code: " + code + "\n\nAll 3/4/5 devices can enter this exact same code simultaneously in Network Gateway to attach closely to " + targetName + " in circle [" + myGrp + "].\n\nCode valid for 30 minutes.";
                    UiArchitect.showConfirmationDialog(this, "Multi-Device Pairing Code", msg, "Copy Code", "Done", () -> {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("Pairing Code", code));
                        Toast.makeText(this, "📋 Code Copied: " + code, Toast.LENGTH_SHORT).show();
                    }, null);
                });
            }
        );
    }

    private void showConnectBToCDialogForNode(RosterNode node) {
        if (node == null || node.getUid() == null) return;
        String devBUid = node.getUid();
        String devBName = (node.getDisplayName() != null) ? node.getDisplayName() : "Device " + devBUid;

        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String selfUid = FirebaseAuth.getInstance().getUid();
        com.jdroidx.jfindme.data.local.DatabaseHelper localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);

        List<Pair<String, String>> candidates = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        seenIds.add(devBUid.toUpperCase(Locale.getDefault()));
        if (selfAgentId != null && !selfAgentId.isEmpty()) seenIds.add(selfAgentId.toUpperCase(Locale.getDefault()));
        if (selfUid != null && !selfUid.isEmpty()) seenIds.add(selfUid.toUpperCase(Locale.getDefault()));

        List<RosterNode> allNodes = (viewModel != null && viewModel.getRosterNodes().getValue() != null) ? viewModel.getRosterNodes().getValue() : new ArrayList<>();
        for (RosterNode n : allNodes) {
            if (n == null) continue;
            String uid = n.getUid();
            if (uid != null && !uid.isEmpty() && !seenIds.contains(uid.toUpperCase(Locale.getDefault()))) {
                seenIds.add(uid.toUpperCase(Locale.getDefault()));
                String dName = (n.getDisplayName() != null && !n.getDisplayName().isEmpty()) ? n.getDisplayName() : "Node " + uid;
                candidates.add(new Pair<>(uid, dName));
            }
        }

        List<Map<String, Object>> offlineNodes = localDb.getOfflineRoster();
        for (Map<String, Object> offNode : offlineNodes) {
            String uid1 = (String) offNode.get("uid_1");
            if (uid1 != null && !uid1.isEmpty() && !seenIds.contains(uid1.toUpperCase(Locale.getDefault()))) {
                seenIds.add(uid1.toUpperCase(Locale.getDefault()));
                String name = (String) offNode.get("name_alias");
                if (name == null || name.isEmpty()) name = (String) offNode.get("device_name");
                if (name == null || name.isEmpty()) name = "Node " + uid1;
                candidates.add(new Pair<>(uid1, name));
            }
        }

        List<String> displayList = new ArrayList<>();
        for (Pair<String, String> c : candidates) {
            displayList.add("📱 " + c.second + " (" + c.first + ")");
        }
        final int candidateCount = candidates.size();
        displayList.add("📋 Multi-Device Fleet Selection (Tickbox Selection)...");
        displayList.add("👨‍👩‍👧 Link Secondary Parent / Co-Guardian...");
        displayList.add("🔑 Generate 1-Code Multi-Device Attachment...");
        displayList.add("⌨️ Enter Device C ID Manually...");

        String[] items = displayList.toArray(new String[0]);
        new android.app.AlertDialog.Builder(this)
            .setTitle("📱 Device Sharing: Share " + devBName + " (Step 1 of 3)")
            .setItems(items, (dialog, which) -> {
                dialog.dismiss();
                if (which < candidateCount) {
                    Pair<String, String> chosen = candidates.get(which);
                    showRelationshipChoiceDialogForNode(devBUid, devBName, chosen.first, chosen.second);
                } else {
                    int offset = which - candidateCount;
                    switch (offset) {
                        case 0:
                            showConnectDeviceDialogForNode(node);
                            break;
                        case 1:
                            showLinkCoParentDialog(node);
                            break;
                        case 2:
                            generateMultiDevicePairingCodeForNode(devBUid, devBName);
                            break;
                        case 3:
                        default:
                            UiArchitect.showInputPromptDialog(this, "Manual Device C Entry", "Enter Device C Agent ID to pair with " + devBName + ":", "", manualId -> {
                                if (manualId != null && !manualId.trim().isEmpty()) {
                                    String cleanCId = manualId.trim();
                                    if (cleanCId.equalsIgnoreCase(devBUid)) {
                                        UiArchitect.showSlideNotificationBanner(this, "INVALID TARGET DEVICE", "Target Device ID cannot be identical to Source Device (" + devBName + ").", true, 4000, null, null);
                                        return;
                                    }
                                    if (cleanCId.equalsIgnoreCase(selfAgentId) || (selfUid != null && cleanCId.equalsIgnoreCase(selfUid))) {
                                        UiArchitect.showSlideNotificationBanner(this, "INVALID TARGET DEVICE", "Target Device ID cannot be your own agent ID.", true, 4000, null, null);
                                        return;
                                    }
                                    if (seenIds.contains(cleanCId.toUpperCase())) {
                                        UiArchitect.showSlideNotificationBanner(this, "DEVICE ALREADY IN CIRCLE", "Device " + cleanCId + " is already in the circle.", true, 4000, null, null);
                                        return;
                                    }
                                    showRelationshipChoiceDialogForNode(devBUid, devBName, cleanCId, "Device " + cleanCId);
                                }
                            });
                            break;
                    }
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    /**
     * [Variables] devBUid, devBName, devCUid, devCName, catOptions, familyEntries, friendEntries, familyRoles, friendRoles, chosenEntry, roleB, roleC
     * [Constants] None
     * [Dependencies] UiArchitect, AlertDialog, DeviceRoleCatalog, DeviceRoleEntry
     * [Integration Points] FamilyFriendsActivity, PairingRepository
     * Step 2: Choose Family vs Friends relationship with role selection from SSOT DeviceRoleCatalog.
     */
    private void showRelationshipChoiceDialogForNode(String devBUid, String devBName, String devCUid, String devCName) {
        String[] catOptions = new String[]{
            "👨‍👩‍👧 Family Relationship (Guardian, Child, Spouse, Sibling)",
            "🤝 Friends Relationship (Best Friend, Close Friend, Peer)"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("Relationship: " + devBName + " ↔ " + devCName)
            .setItems(catOptions, (dialog, which) -> {
                dialog.dismiss();
                if (which == 0) {
                    // Family Selection from SSOT Catalog
                    List<DeviceRoleEntry> familyEntries = DeviceRoleCatalog.INSTANCE.getFAMILY_ROLES();
                    String[] familyRoles = new String[familyEntries.size()];
                    for (int i = 0; i < familyEntries.size(); i++) {
                        DeviceRoleEntry entry = familyEntries.get(i);
                        familyRoles[i] = entry.getIcon() + " " + entry.getTitle() + "\n   " + entry.getDescription();
                    }
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("👨‍👩‍👧 Family Role: " + devBName)
                        .setItems(familyRoles, (famDialog, famWhich) -> {
                            famDialog.dismiss();
                            DeviceRoleEntry chosenEntry = familyEntries.get(famWhich);
                            String roleB;
                            String roleC;
                            if ("CO_PARENT".equalsIgnoreCase(chosenEntry.getId())) {
                                roleB = "Guardian";
                                roleC = "Child";
                            } else if ("DEPENDENT_CHILD".equalsIgnoreCase(chosenEntry.getId())) {
                                roleB = "Child";
                                roleC = "Guardian";
                            } else if ("SPOUSE".equalsIgnoreCase(chosenEntry.getId())) {
                                roleB = "Spouse";
                                roleC = "Spouse";
                            } else if ("ELDER_CARE".equalsIgnoreCase(chosenEntry.getId())) {
                                roleB = "Elder Care";
                                roleC = "Guardian";
                            } else {
                                roleB = chosenEntry.getTitle();
                                roleC = chosenEntry.getTitle();
                            }
                            showSharingPermissionsWizardForNode(devBUid, devBName, devCUid, devCName, roleB, roleC, true, chosenEntry.getIcon(), chosenEntry.getDescription());
                        })
                        .setNegativeButton("BACK", (d, w) -> showRelationshipChoiceDialogForNode(devBUid, devBName, devCUid, devCName))
                        .show();
                } else {
                    // Friends Selection from SSOT Catalog
                    List<DeviceRoleEntry> friendEntries = DeviceRoleCatalog.INSTANCE.getFRIEND_ROLES();
                    String[] friendRoles = new String[friendEntries.size()];
                    for (int i = 0; i < friendEntries.size(); i++) {
                        DeviceRoleEntry entry = friendEntries.get(i);
                        friendRoles[i] = entry.getIcon() + " " + entry.getTitle() + "\n   " + entry.getDescription();
                    }
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("🤝 Friends Role: " + devBName)
                        .setItems(friendRoles, (frDialog, frWhich) -> {
                            frDialog.dismiss();
                            DeviceRoleEntry chosenEntry = friendEntries.get(frWhich);
                            String roleB = chosenEntry.getTitle();
                            String roleC = chosenEntry.getTitle();
                            showSharingPermissionsWizardForNode(devBUid, devBName, devCUid, devCName, roleB, roleC, false, chosenEntry.getIcon(), chosenEntry.getDescription());
                        })
                        .setNegativeButton("BACK", (d, w) -> showRelationshipChoiceDialogForNode(devBUid, devBName, devCUid, devCName))
                        .show();
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    /**
     * [Variables] devBUid, devBName, devCUid, devCName, roleB, roleC, isFamily, icon, desc, container, pad, header, sub, lp, permissions, gpsSwitch, space, sosSwitch
     * [Constants] None
     * [Dependencies] UiArchitect, BaseActivityJava, AlertDialog
     * [Integration Points] FamilyFriendsActivity, PairingRepository
     * Step 3: Interactive Stepped Wizard UI Controls for Device Sharing & Permissions.
     */
    private void showSharingPermissionsWizardForNode(String devBUid, String devBName, String devCUid, String devCName, String roleB, String roleC, boolean isFamily, String icon, String desc) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = scale(16);
        container.setPadding(pad, pad, pad, pad);

        TextView header = new TextView(this);
        header.setText(icon + " " + roleB + " ↔ " + roleC);
        header.setTextSize(16f);
        header.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.setTextColor(getTextColor());
        container.addView(header);

        TextView sub = new TextView(this);
        sub.setText("Connecting " + devBName + " with " + devCName + "\n" + desc);
        sub.setTextSize(12.5f);
        sub.setTextColor(getSecondaryTextColor());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, scale(6), 0, scale(12));
        sub.setLayoutParams(lp);
        container.addView(sub);

        final boolean[] permissions = new boolean[]{true, true};

        View gpsSwitch = UiArchitect.createSwitchWithLabel(this, "🛰️ GPS Telemetry Sharing", "Live location tracking & history exchange", true, isChecked -> permissions[0] = isChecked);
        container.addView(gpsSwitch);

        View space = new View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scale(8)));
        container.addView(space);

        View sosSwitch = UiArchitect.createSwitchWithLabel(this, "🚨 Emergency SOS Sharing", "Emergency siren override & distress broadcasts", true, isChecked -> permissions[1] = isChecked);
        container.addView(sosSwitch);

        new android.app.AlertDialog.Builder(this)
            .setTitle("📱 Share Permissions (Step 3 of 3)")
            .setView(container)
            .setPositiveButton("CONNECT NOW", (dialog, which) -> {
                dialog.dismiss();
                executeDirectBToCConnectionForNode(devBUid, devBName, devCUid, devCName, roleB, roleC, isFamily);
            })
            .setNegativeButton("BACK", (dialog, which) -> {
                dialog.dismiss();
                showRelationshipChoiceDialogForNode(devBUid, devBName, devCUid, devCName);
            })
            .show();
    }

    // [Variables] devBUid, devBName, devCUid, devCName, roleB, roleC, isFamily, selfDevId, myGrp, STANDARD_SYNC_TIMEOUT_MS, startTime, isFinished, timeoutHandler, timeoutRunnable, latency, metaMsg, reasonMsg, failMsg
    // [Constants] STANDARD_SYNC_TIMEOUT_MS
    // [Dependencies] PairingRepository, DatabaseHelper, FirebaseComm, UiArchitect, AngelLoader, RegistryManager
    // [Integration Points] FamilyFriendsActivity, MapActivity, PairingRepository
    // [Variables] devBUid, devBName, devCUid, devCName, roleB, roleC, isFamily, selfDevId, myGrp, famRoleB, frRoleB, famRoleC, frRoleC, profUpdateB, profUpdateC, metaMsg | [Constants] None | [Dependencies] PairingRepository, AngelLoader, UiArchitect, DatabaseHelper, FirebaseComm | [Integration Points] FamilyFriendsActivity, PairingRepository, TrackingService, FCMService
    private void executeDirectBToCConnectionForNode(String devBUid, String devBName, String devCUid, String devCName, String roleB, String roleC, boolean isFamily) {
        String selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);

        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Syncing " + devBName + " ↔ " + devCName + "...");

        com.jdroidx.jfindme.data.repository.PairingRepository.executeClosedLoopDeviceSharing(
            this,
            myGrp,
            devBUid,
            devCUid,
            roleB,
            roleC,
            new com.jdroidx.jfindme.data.repository.PairingRepository.DeviceSharingSyncCallback() {
                @Override
                public void onSyncDispatched(String syncSessionId) {
                    runOnUiThread(() -> com.jdroidx.jfindme.utils.AngelLoader.setStatus(FamilyFriendsActivity.this, "Awaiting peer acknowledgment from " + devCName + "..."));
                }

                @Override
                public void onSyncComplete(String devAId, String devBId, long latencyMs) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(FamilyFriendsActivity.this);
                        com.jdroidx.jfindme.data.local.DatabaseHelper localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(FamilyFriendsActivity.this);

                        String famRoleB = isFamily ? roleB : "None";
                        String frRoleB = isFamily ? "None" : roleB;
                        String famRoleC = isFamily ? roleC : "None";
                        String frRoleC = isFamily ? "None" : roleC;

                        localDb.updateCitizenRole(devBUid, famRoleB, frRoleB, !isFamily, myGrp);
                        localDb.updateCitizenRole(devCUid, famRoleC, frRoleC, !isFamily, myGrp);

                        localDb.saveRosterNode(devBUid, devBName, roleB, "LIVE", false, !isFamily);
                        localDb.saveRosterNode(devCUid, devCName, roleC, "LIVE", false, !isFamily);

                        java.util.Map<String, Object> profUpdateB = new java.util.HashMap<>();
                        profUpdateB.put("role_family", famRoleB);
                        profUpdateB.put("role_friend", frRoleB);
                        profUpdateB.put("role_title", roleB);
                        profUpdateB.put("is_friend", !isFamily);
                        com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(devBUid).updateChildren(profUpdateB);

                        java.util.Map<String, Object> profUpdateC = new java.util.HashMap<>();
                        profUpdateC.put("role_family", famRoleC);
                        profUpdateC.put("role_friend", frRoleC);
                        profUpdateC.put("role_title", roleC);
                        profUpdateC.put("is_friend", !isFamily);
                        com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(devCUid).updateChildren(profUpdateC);

                        com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("signals").child("refresh_roster").setValue(System.currentTimeMillis());

                        if (viewModel != null) {
                            viewModel.initializeCloudSync(FamilyFriendsActivity.this);
                        }

                        String metaMsg = "Device " + devBName + " is synced with Device " + devCName + " [Role: " + roleB + " ↔ " + roleC + "] | Circle: " + myGrp + " | Latency: " + latencyMs + "ms";
                        UiArchitect.showSlideNotificationBanner(FamilyFriendsActivity.this, "DEVICE SHARING SYNCHRONIZED", metaMsg, false, 5000, null, null);
                        Toast.makeText(FamilyFriendsActivity.this, "✅ " + metaMsg, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onSyncTimeout(String diagnosticReason) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(FamilyFriendsActivity.this);
                        UiArchitect.showConfirmationDialog(
                            FamilyFriendsActivity.this,
                            "⚠️ Device Sharing: Sync Timeout",
                            diagnosticReason + "\n\nWould you like to retry synchronization now or exit and try later?",
                            "RETRY",
                            "EXIT (TRY LATER)",
                            () -> executeDirectBToCConnectionForNode(devBUid, devBName, devCUid, devCName, roleB, roleC, isFamily),
                            () -> Toast.makeText(FamilyFriendsActivity.this, "Sync deferred. Will resume automatically when peer connects.", Toast.LENGTH_SHORT).show()
                        );
                    });
                }

                @Override
                public void onSyncFailed(String errorMessage) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(FamilyFriendsActivity.this);
                        UiArchitect.showConfirmationDialog(
                            FamilyFriendsActivity.this,
                            "⚠️ Device Sharing Failed",
                            "Synchronization failed: " + errorMessage + "\n\nWould you like to retry or exit?",
                            "RETRY",
                            "EXIT",
                            () -> executeDirectBToCConnectionForNode(devBUid, devBName, devCUid, devCName, roleB, roleC, isFamily),
                            null
                        );
                    });
                }
            }
        );
    }

    /**
     * [Variables] candidates, seenIds, allNodes, roles, roleJoined, confirmMsg, myGrp, primaryRole, remaining, successCount, STANDARD_SYNC_TIMEOUT_MS, startTime, isFinished, timeoutHandler, timeoutRunnable, latency, msg, reasonMsg
     * [Constants] STANDARD_SYNC_TIMEOUT_MS
     * [Dependencies] UiArchitect, PairingRepository, DatabaseHelper, AngelLoader, RegistryManager
     * [Integration Points] FamilyFriendsActivity, PairingRepository, DatabaseHelper
     * 🔗 Displays Compound Device & Role Connector Modal Dialog from Family & Friends roster.
     */
    private void showConnectDeviceDialogForNode(RosterNode node) {
        if (node == null) return;
        String targetUid = node.getUid();
        String targetName = node.getDisplayName();

        List<Pair<String, String>> candidates = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        seenIds.add(targetUid.toUpperCase());

        List<RosterNode> allNodes = (viewModel != null && viewModel.getRosterNodes().getValue() != null) ? viewModel.getRosterNodes().getValue() : new ArrayList<>();
        for (RosterNode n : allNodes) {
            if (n == null) continue;
            String uid = n.getUid();
            if (uid != null && !uid.isEmpty() && !seenIds.contains(uid.toUpperCase())) {
                seenIds.add(uid.toUpperCase());
                candidates.add(new Pair<>(uid, n.getDisplayName()));
            }
        }

        String[] roles = new String[]{
            "🛡️ Parent / Primary Guardian",
            "👨‍👩‍👧 Co-Parent / Secondary Guardian",
            "👶 Child / Dependent Ward",
            "💍 Spouse / Partner",
            "🤝 Friend / Peer Member",
            "🚨 Emergency Contact"
        };

        UiArchitect.showCompoundDeviceRoleDialog(this, targetUid, targetName, candidates, roles, (selectedDeviceIds, selectedRoles) -> {
            String roleJoined = TextUtils.join(", ", selectedRoles);
            String confirmMsg = "Connect " + targetName + " (" + targetUid + ") to " + selectedDeviceIds.size() + " device(s) with role(s):\n" + roleJoined + "?\n\nThis will link them mutually in your active mesh.";
            
            UiArchitect.showConfirmationDialog(this, "Confirm Connection", confirmMsg, "Connect Now", "Cancel", () -> {
                executeBatchDeviceConnectionForNode(node, selectedDeviceIds, selectedRoles);
            }, null);
        });
    }

    // [Variables] node, selectedDeviceIds, selectedRoles, myGrp, primaryRole
    // [Constants] None
    // [Dependencies] PairingRepository, AngelLoader, UiArchitect, DatabaseHelper
    // [Integration Points] FamilyFriendsActivity, PairingRepository
    private void executeBatchDeviceConnectionForNode(RosterNode node, List<String> selectedDeviceIds, List<String> selectedRoles) {
        String targetUid = node.getUid();
        String targetName = (node.getDisplayName() != null && !node.getDisplayName().isEmpty()) ? node.getDisplayName() : "Device " + targetUid;
        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);
        String primaryRole = !selectedRoles.isEmpty() ? selectedRoles.get(0).replaceAll("[^a-zA-Z /]", "").trim() : "Member";

        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Connecting " + selectedDeviceIds.size() + " devices...");

        com.jdroidx.jfindme.data.repository.PairingRepository.executeClosedLoopBatchDeviceSharing(
            this,
            myGrp,
            targetUid,
            selectedDeviceIds,
            selectedRoles,
            new com.jdroidx.jfindme.data.repository.PairingRepository.BatchDeviceSharingSyncCallback() {
                @Override
                public void onBatchDispatched(String syncSessionId, int totalDevices) {
                    runOnUiThread(() -> com.jdroidx.jfindme.utils.AngelLoader.setStatus(FamilyFriendsActivity.this, "Awaiting physical acknowledgments (" + totalDevices + " devices)..."));
                }

                @Override
                public void onBatchProgress(int syncedCount, int totalDevices) {
                    runOnUiThread(() -> com.jdroidx.jfindme.utils.AngelLoader.setStatus(FamilyFriendsActivity.this, "Syncing: " + syncedCount + " of " + totalDevices + " devices confirmed..."));
                }

                @Override
                public void onBatchComplete(int syncedCount, int totalDevices, long latencyMs) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(FamilyFriendsActivity.this);
                        if (viewModel != null) {
                            viewModel.initializeCloudSync(FamilyFriendsActivity.this);
                        }
                        String msg = targetName + " is synced with " + syncedCount + " of " + totalDevices + " devices as [" + primaryRole + "] | Circle: " + myGrp + " | Latency: " + latencyMs + "ms";
                        UiArchitect.showSlideNotificationBanner(FamilyFriendsActivity.this, "DEVICES SYNCHRONIZED", msg, false, 5000, null, null);
                        Toast.makeText(FamilyFriendsActivity.this, "✅ " + msg, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onBatchTimeout(int syncedCount, int totalDevices, String diagnosticReason) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(FamilyFriendsActivity.this);
                        UiArchitect.showConfirmationDialog(
                            FamilyFriendsActivity.this,
                            "⚠️ Device Sharing: Batch Timeout",
                            diagnosticReason + "\n\nWould you like to retry synchronization or exit and try later?",
                            "RETRY",
                            "EXIT (TRY LATER)",
                            () -> executeBatchDeviceConnectionForNode(node, selectedDeviceIds, selectedRoles),
                            null
                        );
                    });
                }

                @Override
                public void onBatchFailed(String errorMessage) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(FamilyFriendsActivity.this);
                        UiArchitect.showValidationDialog(FamilyFriendsActivity.this, "Batch Sync Error", errorMessage, null);
                    });
                }
            }
        );
    }

    private void showRenameNodeDialog(RosterNode node) {
        String currentName = node.getDisplayName();
        if (currentName.startsWith("HW_") || currentName.startsWith("Agent_") || currentName.length() > 20) {
            currentName = "";
        }
        UiArchitect.showInputPromptDialog(this,
            "Rename Device",
            "Enter a friendly custom callsign / name for " + node.getDisplayName() + " (" + node.getUid() + "):",
            currentName,
            enteredName -> {
                if (enteredName != null && !enteredName.trim().isEmpty()) {
                    String cleanName = enteredName.trim();
                    DatabaseHelper.getInstance(this).updateDeviceIdentityAndAvatar(node.getUid(), cleanName, null);
                    
                    // Cloud update
                    Map<String, Object> nameMap = new HashMap<>();
                    nameMap.put("full_name", cleanName);
                    nameMap.put("display_name", cleanName);
                    nameMap.put("nickname", cleanName);
                    nameMap.put("ts_modified", System.currentTimeMillis());
                    FirebaseComm.getProfileRef(node.getUid()).updateChildren(nameMap);
                    
                    String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);
                    if (myGrp != null) {
                        FirebaseComm.getRelationshipsRef().child(myGrp).child("members").child(node.getUid()).child("name").setValue(cleanName);
                        FirebaseComm.getRelationshipsRef().child(myGrp).child("signals").child("refresh_roster").setValue(System.currentTimeMillis());
                    }
                    
                    UiArchitect.showSlideNotificationBanner(this, "DEVICE RENAMED", "Callsign updated to '" + cleanName + "'", false, 3000, null, null);
                    if (viewModel != null) viewModel.initializeCloudSync(this);
                } else {
                    UiArchitect.showValidationDialog(this, "Invalid Name", "Device name cannot be empty.", null);
                }
            });
    }

    private void showRoleChangeDialog(RosterNode node) {
        UiArchitect.OptionItem[] roles = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("👑 Primary Guardian / Parent", "Full administrative oversight and geofence controls", com.jdroidx.jfindme.R.drawable.ic_user_profile),
            new UiArchitect.OptionItem("💍 Spouse / Co-Guardian", "Equal co-management of dependent members", com.jdroidx.jfindme.R.drawable.ic_family_friends),
            new UiArchitect.OptionItem("🧸 Dependent Child / Ward", "Monitored asset with geofence breach tracking", com.jdroidx.jfindme.R.drawable.ic_people),
            new UiArchitect.OptionItem("🌐 Friend / Colleague (Peer)", "Mutual peer tracking without administrative control", com.jdroidx.jfindme.R.drawable.ic_family_friends)
        };

        UiArchitect.showRichOptionsDialog(this, "Change Role: " + node.getDisplayName(), roles, (which, label) -> {
            String newRole;
            boolean isFriend = false;
            switch (which) {
                case 0: newRole = "Guardian"; break;
                case 1: newRole = "Spouse"; break;
                case 2: newRole = "Child"; break;
                default: newRole = "Friend"; isFriend = true; break;
            }
            updateTargetRole(node.getUid(), newRole, !isFriend);
            if ("Child".equals(newRole)) {
                promptAddOtherParentDialog(node);
            }
        });
    }

    /**
     * 👨‍👩‍👧 Prompt to link a secondary parent/co-guardian when a member is marked as a Child
     */
    private void promptAddOtherParentDialog(RosterNode childNode) {
        new AlertDialog.Builder(this)
            .setTitle("👨‍👩‍👧 Link Other Parent / Co-Guardian")
            .setMessage("Child role assigned to " + childNode.getDisplayName() + "!\n\nWould you like to link a second parent or co-guardian (such as your spouse) to co-manage this child?\n\nThis assigns shared parental oversight so both parents can track and protect this child with the same device identity.")
            .setPositiveButton("Link Co-Parent", (d, w) -> showLinkCoParentDialog(childNode))
            .setNegativeButton("Skip for Now", null)
            .show();
    }

    /**
     * 👨‍👩‍👧 Co-Parent Link Selection Sheet: Lists existing circle guardians or offers invite/manual entry
     */
    private void showLinkCoParentDialog(RosterNode childNode) {
        if (childNode == null) return;
        String myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);
        List<RosterNode> coParentCandidates = new ArrayList<>();
        java.util.Set<String> seenIds = new java.util.HashSet<>();
        if (myAgentId != null) seenIds.add(myAgentId.toUpperCase());
        if (childNode != null && childNode.getUid() != null) seenIds.add(childNode.getUid().toUpperCase());

        if (viewModel != null && viewModel.getRosterLiveData().getValue() != null) {
            for (RosterNode n : viewModel.getRosterLiveData().getValue()) {
                if (n == null) continue;
                String uid = n.getUid();
                if (uid == null || seenIds.contains(uid.toUpperCase())) continue;
                String r = n.getRoleTitle() != null ? n.getRoleTitle().toLowerCase() : "";
                if (r.contains("guardian") || r.contains("parent") || r.contains("spouse") || r.contains("admin")) {
                    coParentCandidates.add(n);
                    seenIds.add(uid.toUpperCase());
                }
            }
        }

        // Merge candidates from local SQLite offline roster
        try {
            com.jdroidx.jfindme.data.local.DatabaseHelper localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
            List<Map<String, Object>> offlineRoster = localDb.getOfflineRoster(myGrp);
            if (offlineRoster != null) {
                for (Map<String, Object> node : offlineRoster) {
                    String uid = (String) node.get("uid_1");
                    if (uid == null || uid.isEmpty() || seenIds.contains(uid.toUpperCase())) continue;
                    String role = (String) node.get("role");
                    String r = role != null ? role.toLowerCase() : "";
                    if (r.contains("guardian") || r.contains("parent") || r.contains("spouse") || r.contains("admin")) {
                        String name = (String) node.get("display_name");
                        if (name == null || name.isEmpty()) name = "Guardian (" + uid + ")";
                        coParentCandidates.add(new RosterNode(
                            uid, name, role != null ? role : "Guardian", "Offline", false, false,
                            null, "ROOT_GUARDIAN", 100, false, "Offline", 0L, null, null, null, 0xFF
                        ));
                        seenIds.add(uid.toUpperCase());
                    }
                }
            }
        } catch (Exception ignored) {}

        List<UiArchitect.OptionItem> optionsList = new ArrayList<>();
        for (RosterNode cand : coParentCandidates) {
            optionsList.add(new UiArchitect.OptionItem(
                "👨‍👩‍👧 Link " + cand.getDisplayName(),
                "Assign " + cand.getDisplayName() + " (" + cand.getRoleTitle() + ") as Co-Parent for " + childNode.getDisplayName(),
                com.jdroidx.jfindme.R.drawable.ic_user_profile
            ));
        }
        optionsList.add(new UiArchitect.OptionItem(
            "🔑 Generate Co-Guardian Invite Code",
            "Generate a 6-character code for the second parent to join circle and co-manage this child",
            com.jdroidx.jfindme.R.drawable.ic_family_friends
        ));
        optionsList.add(new UiArchitect.OptionItem(
            "⌨️ Enter Co-Parent Agent ID Manually",
            "Manually link an existing 8-character hardware/agent token as co-parent",
            com.jdroidx.jfindme.R.drawable.ic_settings_gears
        ));

        UiArchitect.OptionItem[] options = optionsList.toArray(new UiArchitect.OptionItem[0]);

        UiArchitect.showRichOptionsDialog(this, "Co-Parent for " + childNode.getDisplayName(), options, (which, label) -> {
            if (which < coParentCandidates.size()) {
                RosterNode chosen = coParentCandidates.get(which);
                executeCoParentLink(chosen.getUid(), chosen.getDisplayName(), childNode);
            } else if (label != null && label.contains("Invite Code")) {
                generateAndShowCircleCodeDialog();
            } else {
                UiArchitect.showInputPromptDialog(this,
                    "Link Co-Parent",
                    "Enter the 8-character Agent ID / Hardware Token of the other parent / co-guardian:",
                    "",
                    inputToken -> {
                        if (inputToken != null && !inputToken.trim().isEmpty()) {
                            String cleanToken = inputToken.trim().toUpperCase();
                            executeCoParentLink(cleanToken, "Co-Parent (" + cleanToken + ")", childNode);
                        } else {
                            UiArchitect.showValidationDialog(this, "Invalid ID", "Co-Parent Agent ID cannot be empty.", null);
                        }
                    });
            }
        });
    }

    /**
     * ⚡ Executes Co-Parent atomic relationship binding across Firebase and local SQLite with Closed-Loop Handshake
     */
    private void executeCoParentLink(String coParentId, String coParentName, RosterNode childNode) {
        executeDirectBToCConnectionForNode(childNode.getUid(), childNode.getDisplayName(), coParentId, coParentName, "Child", "Parent / Guardian", true);
    }

    private void showAddMemberOptionsDialog() {
        UiArchitect.OptionItem[] options = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("📱 Scan QR Code (Fast Pair)", "Launch optical camera scanner for immediate handshake", com.jdroidx.jfindme.R.drawable.ic_nav_map),
            new UiArchitect.OptionItem("🔑 Generate 6-Char Circle Invite Code", "Create shareable 6-character code (SMS / WhatsApp)", com.jdroidx.jfindme.R.drawable.ic_family_friends),
            new UiArchitect.OptionItem("⌨️ Enter Pairing Token Manually", "Input 6-char Circle Code or 8-char hardware token", com.jdroidx.jfindme.R.drawable.ic_settings_gears)
        };

        UiArchitect.showRichOptionsDialog(this, "Add Family Member or Friend", options, (which, label) -> {
            if (which == 1) {
                generateAndShowCircleCodeDialog();
            } else {
                Intent gwIntent = new Intent(this, NetworkGatewayActivity.class);
                gwIntent.putExtra("REQUESTED_TAB", (which == 0) ? 2 : 1);
                startActivity(gwIntent);
            }
        });
    }

    private void generateAndShowCircleCodeDialog() {
        String hostAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        String hostGroup = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);

        Toast.makeText(this, "Generating time-limited Circle Invite Code...", Toast.LENGTH_SHORT).show();
        PairingRepository.createCircleInviteCode(hostAgentId, hostGroup, "MEMBER", code -> {
            runOnUiThread(() -> {
                if (code == null || code.trim().isEmpty()) {
                    UiArchitect.showValidationDialog(this, "Generation Failed", "Could not generate circle invite code. Please check your internet connection.", null);
                    return;
                }
                AlertDialog codeDialog = new AlertDialog.Builder(this)
                    .setTitle("Circle Invite Code")
                    .setMessage("Share this 6-character code with your family member or friend to instantly join your circle (Valid for 48 Hours):\n\n" +
                                "🔑 CODE: " + code + "\n\n" +
                                "They can enter this code in Network Gateway to pair without hardware conflicts.")
                    .setPositiveButton("COPY CODE", (d, w) -> {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("jFindMe Circle Code", code);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Code " + code + " copied to clipboard!", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("SHARE", (d, w) -> {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join my jFindMe Circle");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "Join my private family/friend circle on jFindMe! Use this 6-character invite code: " + code);
                        startActivity(Intent.createChooser(shareIntent, "Share Circle Invite Code"));
                    })
                    .setNegativeButton("Done", null)
                    .create();
                codeDialog.show();
                applyDialogTheme(codeDialog);
            });
        });
    }

    private void showPrivacyTierDialog() {
        PrivacyEngine.PrivacyTier currentTier = PrivacyEngine.getPrivacyTier(this);
        String[] tiers = {
            "🎯 Precise Mode (100% Exact GPS Pinpoint)",
            "🌫️ Fuzzy Mode (Neighborhood ~800m Jitter Blur)",
            "👻 Ghost / Frozen Mode (Pause Broadcast / Inactive)"
        };
        int selectedIdx = currentTier == PrivacyEngine.PrivacyTier.PRECISE ? 0 : (currentTier == PrivacyEngine.PrivacyTier.FUZZY ? 1 : 2);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Privacy Visibility Controls")
            .setSingleChoiceItems(tiers, selectedIdx, (di, which) -> {
                PrivacyEngine.PrivacyTier newTier = (which == 0) ? PrivacyEngine.PrivacyTier.PRECISE : (which == 1 ? PrivacyEngine.PrivacyTier.FUZZY : PrivacyEngine.PrivacyTier.FROZEN);
                PrivacyEngine.setPrivacyTier(this, newTier);
                Toast.makeText(this, "Visibility shifted to " + newTier.name(), Toast.LENGTH_SHORT).show();
                di.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .create();

        dialog.show();
        applyDialogTheme(dialog);
    }

    private void confirmUnlinkDevice(RosterNode node) {
        UiArchitect.OptionItem[] unlinkOptions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("❌ Standard Unlink Device", "Safely uncouple device from active circle and local storage", android.R.drawable.ic_menu_delete),
            new UiArchitect.OptionItem("🛡️ Master Password Force Purge", "Administrative override with master security password to instantly wipe corrupted or orphan nodes", com.jdroidx.jfindme.R.drawable.ic_privacy_lock)
        };

        UiArchitect.showRichOptionsDialog(this, "Remove " + node.getDisplayName() + "?", unlinkOptions, (idx, label) -> {
            if (idx == 0) {
                // Standard Unlink
                viewModel.unlinkDevice(this, node.getUid(), success -> {
                    Toast.makeText(this, "Device " + node.getDisplayName() + " unlinked successfully.", Toast.LENGTH_SHORT).show();
                    return null;
                });
            } else {
                // Master Password Purge
                String requiredPass = RegistryManager.getPurgeSafetyPhrase(this);
                UiArchitect.showInputPromptDialog(this,
                    "Master Admin Authorization",
                    "Enter master security password to force purge '" + node.getDisplayName() + "' (" + node.getUid() + "):",
                    "",
                    inputPass -> {
                        if (RegistryManager.verifyAndUpgradePurgePhrase(this, inputPass != null ? inputPass.trim() : "")) {
                            viewModel.unlinkDevice(this, node.getUid(), success -> {
                                Toast.makeText(this, "🛡️ Force Administrative Purge Completed for " + node.getDisplayName(), Toast.LENGTH_LONG).show();
                                return null;
                            });
                        } else {
                            UiArchitect.showValidationDialog(this, "Authorization Denied", "Incorrect master password. Device deletion was blocked.", null);
                        }
                    });
            }
        });
    }

    private void updateTargetRole(String uid, String role, boolean isFamily) {
        String famRole = isFamily ? role : "None";
        String frRole = isFamily ? "None" : role;

        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);
        DatabaseHelper localDb = DatabaseHelper.getInstance(this);
        localDb.updateCitizenRole(uid, famRole, frRole, frRole.contains("Friend"), myGrp);

        Map<String, Object> updates = new HashMap<>();
        updates.put("role_family", famRole);
        updates.put("role_friend", frRole);
        updates.put("role_title", role);
        updates.put("ts_modified", System.currentTimeMillis());

        FirebaseComm.getProfileRef(uid).updateChildren(updates);

        if (myGrp != null) {
            FirebaseComm.getRelationshipsRef().child(myGrp).child("members").child(uid).child("role").setValue(role);
            FirebaseComm.getRelationshipsRef().child(myGrp).child("signals").child("refresh_roster").setValue(System.currentTimeMillis());
        }

        Toast.makeText(this, "✅ Role updated to " + role + " successfully!", Toast.LENGTH_SHORT).show();
        if (viewModel != null) viewModel.initializeCloudSync(this);
    }

    private void triggerRosterReclamation() {
        AngelLoader.show(this);
        AngelLoader.setStatus(this, "Scanning Cloud Matrix for lost nodes...");
        viewModel.forceAggressiveRecovery(this, success -> {
            runOnUiThread(() -> {
                AngelLoader.hide(this);
                if (success) {
                    Toast.makeText(this, "ROSTER RESTORED SUCCESSFULLY", Toast.LENGTH_LONG).show();
                    viewModel.initializeCloudSync(this);
                } else {
                    AlertDialog diag = new AlertDialog.Builder(this)
                        .setTitle("RECOVERY SCAN COMPLETE")
                        .setMessage("Zero unlinked nodes found in your active cloud group. If this is a new setup, use the NETWORK GATEWAY to pair your first device.")
                        .setPositiveButton("Open Gateway", (di, w) -> {
                            Intent gwIntent = new Intent(this, NetworkGatewayActivity.class);
                            gwIntent.putExtra("LAUNCH_MODE", "RECOVERY");
                            startActivity(gwIntent);
                            finish();
                        })
                        .setNegativeButton("Dismiss", null)
                        .create();
                    diag.show();
                    applyDialogTheme(diag);
                }
            });
            return null;
        });
    }

    private View buildIdentityBlock() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(scale(24), scale(20), scale(24), scale(20));
        row.setGravity(Gravity.CENTER_VERTICAL);

        ownerAvatar = new ImageView(this);
        AvatarLoader.bindCurrentUserAvatar(this, ownerAvatar);
        GradientDrawable ownerRing = new GradientDrawable();
        ownerRing.setShape(GradientDrawable.OVAL);
        ownerRing.setColor(Color.parseColor("#1A000000"));
        ownerRing.setStroke(scale(3), getGradientStart());
        ownerAvatar.setBackground(ownerRing);

        int universalSize = UiArchitect.getUniversalAvatarSize(this);
        row.addView(ownerAvatar, new LinearLayout.LayoutParams(universalSize, universalSize));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(scale(30), 0, 0, 0);

        String finalAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        String nameStr = RegistryManager.getUserNickname(this);

        tvOwnerName = new TextView(this);
        tvOwnerName.setText("👤 Owner: " + nameStr);
        tvOwnerName.setTextColor(getTextColor());
        tvOwnerName.setTextSize(scaleText(15));
        tvOwnerName.setTypeface(null, Typeface.BOLD);
        textBlock.addView(tvOwnerName);

        SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String deviceId = jPrefs.getString("device_uid", "UID-" + finalAgentId.substring(0, Math.min(6, finalAgentId.length())).toUpperCase(Locale.getDefault()));

        TextView tvDeviceUid = new TextView(this);
        tvDeviceUid.setText("📱 Device ID: " + deviceId);
        tvDeviceUid.setTextColor(getSecondaryTextColor());
        tvDeviceUid.setTextSize(10);
        textBlock.addView(tvDeviceUid);

        TextView tvNetworkId = new TextView(this);
        tvNetworkId.setText("🌐 Network Node: " + finalAgentId);
        tvNetworkId.setTextColor(getSecondaryTextColor());
        tvNetworkId.setTextSize(10);
        textBlock.addView(tvNetworkId);

        row.addView(textBlock);
        return row;
    }

    @Override
    protected void onSaveCurrentState(Bundle outState) {
        if (outState == null) return;
        outState.putInt("SAVED_ACTIVE_TAB", activeTab);
        outState.putInt("SAVED_SORT_MODE", activeSortMode);
        outState.putString("SAVED_SEARCH_FILTER", (searchFilter != null) ? searchFilter : "");
    }

    @Override
    protected void onUiRebuilt(Bundle savedState) {
        if (savedState == null) return;
        searchFilter = savedState.getString("SAVED_SEARCH_FILTER", "");
        activeSortMode = savedState.getInt("SAVED_SORT_MODE", SORT_DEFAULT);
        int savedTab = savedState.getInt("SAVED_ACTIVE_TAB", TAB_FAMILY);
        if (savedTab == TAB_FRIENDS) {
            renderFriendsTab();
        } else {
            renderFamilyTab();
        }
    }

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Lifecycle Guard: Purges debouncer queue on activity destruction / orientation change
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchDebounceHandler != null && searchDebounceRunnable != null) {
            searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
        }
    }

    @Override
    protected void onMasterDataUpdated(String category, String changeType, String entityId, String senderId) {
        super.onMasterDataUpdated(category, changeType, entityId, senderId);
        if ("ROSTER".equalsIgnoreCase(category) || "PROFILE".equalsIgnoreCase(category)) {
            runOnUiThread(() -> {
                if (viewModel != null) {
                    viewModel.initializeCloudSync(this);
                }
            });
        }
    }
}
