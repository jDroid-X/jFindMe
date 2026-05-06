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
import android.content.Intent;
import android.content.SharedPreferences;

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
import com.jdroidx.jfindme.data.local.DatabaseHelper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 🔗 UNIFIED ARCHITECTURE GATEWAY SCREEN
 * Re-engineered to support both Cloud Firebase Approvals (Remote) & P2P Networks (Nearby).
 * Restored from Forensic Archive (Commit: 1dfae0e)
 */
public class NetworkGatewayActivity extends BaseActivityJava {

    private final String TAG = "NetworkGatewayActivity";
    
    // UI Panels
    private LinearLayout optionGatewayPanel;
    private LinearLayout remotePairingPanel;
    private LinearLayout localPairingPanel;
    
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

    // Persisted References for Cloud Sync refresh
    private String safeUid = "MOCK_U123";
    private String forensicAgentId = "OWNER_001";
    private LinearLayout branchList;
    private TextView activeSyncHeader;

    // QR Code Scanner Result Payload
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() != null) {
                    String rawCode = result.getContents().trim();
                    showTacticalHandshakeOverlay(rawCode);
                    
                    if (rawCode.length() == 8) {
                        if (etLocalCode != null) etLocalCode.setText(rawCode);
                        executeLocalJoinRequest(rawCode);
                        Toast.makeText(this, "Local Node Detected (8-Digit). Routing to P2P Matrix...", Toast.LENGTH_SHORT).show();
                    } else if (rawCode.length() == 6) {
                        if (etCode != null) etCode.setText(rawCode);
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
            rText.setTextColor(getGradientOverlayText()); 
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
        
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            applyDialogTheme(dialog);
        }
    }

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

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
        header.addView(textBlock);
        
        mainRoot.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout identityCard = new LinearLayout(this);
        identityCard.setOrientation(LinearLayout.VERTICAL);
        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, identityCard, "CARD_STANDARD");
        identityCard.addView(buildIdentityBlock());
        
        // 🛰️ Group Activity Tile (Hub Identification)
        LinearLayout groupCard = new LinearLayout(this);
        groupCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, groupCard, "CARD_STANDARD");
        
        LinearLayout.LayoutParams groupLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        groupLp.setMargins(0, 0, 0, 40);
        groupCard.setLayoutParams(groupLp);
        
        groupCard.addView(buildGroupTile());
        root.addView(groupCard, groupLp);
        
        int quota = getResponsivePaddingQuota();
        int topGap = getResponsiveHeaderTopQuota();
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(quota, topGap, quota, 40);
        root.addView(identityCard, cardLp);

        optionGatewayPanel = buildGatewaySelectionLayout();
        remotePairingPanel = buildRemoteOptionLayout();
        localPairingPanel = buildLocalOptionLayout();

        LinearLayout.LayoutParams standardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        standardLp.setMargins(quota, 0, quota, scale(40));

        root.addView(optionGatewayPanel, standardLp);
        root.addView(remotePairingPanel, standardLp);
        root.addView(localPairingPanel, standardLp);

        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(80, Color.red(getSecondaryTextColor()), Color.green(getSecondaryTextColor()), Color.blue(getSecondaryTextColor())));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        divLp.setMargins(quota, 10, quota, 40);
        root.addView(divider, divLp);

        LinearLayout rosterCard = new LinearLayout(this);
        rosterCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(rosterCard, getCardColor(), 30f);
        rosterCard.setPadding(0, 20, 0, 30);
        rosterCard.addView(buildActiveRosterList());
        root.addView(rosterCard, standardLp);

        scroll.addView(root);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        root.addView(buildRestoreActionTile());

        mainRoot.addView(buildArchitectureStatusBar());

        resetGateway();

        return mainRoot;
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
        int iSize = scaleIcon(80);
        box.addView(iv, new LinearLayout.LayoutParams(iSize, iSize));

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
        row.setPadding(scale(30), scale(40), scale(30), scale(40)); 
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView myAvatar = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, myAvatar);
        
        int avatarSize = scale(240); 
        android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
        c.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        c.setColor(Color.TRANSPARENT);
        c.setStroke(6, getGradientStart());
        myAvatar.setBackground(c);
        
        row.addView(myAvatar, new LinearLayout.LayoutParams(avatarSize, avatarSize));
        
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(scale(40), 0, 0, 0);
        
        SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String finalAgentId = jPrefs.getString("agent_id", "AGENT_ERROR");
        
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        Map<String, String> myDos = db.getLocalDossier(finalAgentId);
        String nameStr = (myDos != null && myDos.get("fullName") != null) ? myDos.get("fullName") : jPrefs.getString("user_nickname", "User Identity");
        
        TextView myName = new TextView(this);
        myName.setText("👤 Owner: " + nameStr);
        myName.setTextColor(getTextColor());
        myName.setTextSize(scaleText(15));
        myName.setTypeface(null, Typeface.BOLD);
        textBlock.addView(myName);
        
        String hardwareId = jPrefs.getString("hardware_device_id", null);
        if (hardwareId == null) {
            hardwareId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            jPrefs.edit().putString("hardware_device_id", hardwareId).apply();
        }
        String deviceId = jPrefs.getString("device_uid", null);
        if (deviceId == null) {
            deviceId = "UID-" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
            jPrefs.edit().putString("device_uid", deviceId).apply();
        }

        TextView tvDeviceUid = new TextView(this);
        tvDeviceUid.setText("📱 Device ID: - " + deviceId);
        tvDeviceUid.setTextColor(getSecondaryTextColor());
        tvDeviceUid.setTextSize(10);
        textBlock.addView(tvDeviceUid);

        TextView tvHardwareUid = new TextView(this);
        tvHardwareUid.setText("⚙️ Hardware UID: - " + hardwareId);
        tvHardwareUid.setTextColor(getSecondaryTextColor());
        tvHardwareUid.setTextSize(10);
        textBlock.addView(tvHardwareUid);

        // 🧬 V59.7: Show the FORENSIC Agent ID as the Network ID (Eliminate Blue Node)
        TextView tvNetworkId = new TextView(this);
        tvNetworkId.setText("🌐 Network ID: - " + finalAgentId);
        tvNetworkId.setTextColor(getSecondaryTextColor());
        tvNetworkId.setTextSize(9);
        textBlock.addView(tvNetworkId);
        
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

    private View buildGroupTile() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 30, 40, 30);

        SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String groupId = jPrefs.getString("active_group", "NONE");
        
        Map<String, String> groupDetails = DatabaseHelper.getInstance(this).getGroupDetails(groupId);
        String groupName = (groupDetails != null) ? groupDetails.get("name") : "SECURE_HUB";

        TextView tvTitle = new TextView(this);
        tvTitle.setText("GROUP ACTIVITY TILE");
        tvTitle.setTextColor(getGradientStart());
        tvTitle.setTextSize(12);
        tvTitle.setTypeface(null, Typeface.BOLD);
        container.addView(tvTitle);

        TextView tvName = new TextView(this);
        tvName.setText(groupName);
        tvName.setTextColor(getTextColor());
        tvName.setTextSize(22);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setPadding(0, 10, 0, 5);
        container.addView(tvName);

        TextView tvId = new TextView(this);
        tvId.setText("HUB ID: " + groupId);
        tvId.setTextColor(getSecondaryTextColor());
        tvId.setTextSize(10);
        container.addView(tvId);

        return container;
    }

    private View buildActiveRosterList() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        
        activeSyncHeader = (TextView) createSectionHeader("DEVICES IN YOUR PAIRING GROUP: CALCULATING...");
        container.addView(activeSyncHeader);

        String myUid = FirebaseAuth.getInstance().getUid();
        safeUid = (myUid != null) ? myUid : "MOCK_U123";
        
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        // 🧬 V69.9: Standardized Auth-to-Agent Resolution
        Map<String, String> myDos = db.getIdentityByAuthUid(safeUid);
        forensicAgentId = (myDos != null && myDos.containsKey("uid_1")) ? myDos.get("uid_1") : "AGENT_RESOLVING";
        
        SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String myName = (myDos != null && myDos.get("fullName") != null) ? myDos.get("fullName") : jPrefs.getString("user_nickname", "User Identity");
        
        branchList = new LinearLayout(this);
        branchList.setOrientation(LinearLayout.VERTICAL);
        branchList.setVisibility(View.VISIBLE); 
        
        LinearLayout rootNode = buildTreeNode(forensicAgentId, myName + " (Primary Admin)", true, true, branchList, "OWNER");
        container.addView(rootNode);
        container.addView(branchList);

        List<Map<String, Object>> localRoster = db.getOfflineRoster();
        if (localRoster.isEmpty()) {
            activeSyncHeader.setText("NO LOCAL TRACKING DATA FOUND.");
        } else {
            renderLocalRoster(safeUid, branchList, activeSyncHeader, localRoster);
        }

        TextView refreshBtn = new TextView(this);
        refreshBtn.setText("🔄 REFRESH CLOUD MATRIX");
        refreshBtn.setTextColor(getGradientStart());
        refreshBtn.setTextSize(10);
        refreshBtn.setPadding(40, 0, 0, 20);
        refreshBtn.setFocusable(true);
        refreshBtn.setClickable(true);
        refreshBtn.setOnClickListener(v -> initiateCloudSync(forensicAgentId, branchList, activeSyncHeader));
        
        refreshBtn.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == KeyEvent.ACTION_UP && (k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });
        container.addView(refreshBtn);

        return container;
    }

    private void initiateCloudSync(String safeUid, LinearLayout branchList, TextView header) {
        com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(safeUid).child("active_group").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot myGroupSnap) {
                com.jdroidx.jfindme.utils.AngelLoader.show(NetworkGatewayActivity.this);
                com.jdroidx.jfindme.utils.AngelLoader.setStatus(NetworkGatewayActivity.this, "Syncing Tiered Identity Matrix...");

                String myGroupId = myGroupSnap.getValue(String.class);
                StringBuilder authorizedUids = new StringBuilder(safeUid).append(",");
                
                if (myGroupId == null || myGroupId.isEmpty()) {
                    fetchFriendMatrix(safeUid, null, branchList, authorizedUids, header, 1);
                    com.jdroidx.jfindme.utils.AngelLoader.hide(NetworkGatewayActivity.this);
                    return;
                }

                com.jdroidx.jfindme.data.remote.FirebaseComm.getDevicesRef().orderByChild("Identity/active_group").equalTo(myGroupId)
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                            fetchFriendMatrix(safeUid, myGroupId, branchList, authorizedUids, header, snapshot.getChildrenCount());
                            com.jdroidx.jfindme.utils.AngelLoader.hide(NetworkGatewayActivity.this);
                        }
                        @Override
                        public void onCancelled(com.google.firebase.database.DatabaseError error) {
                            com.jdroidx.jfindme.utils.AngelLoader.hide(NetworkGatewayActivity.this);
                        }
                    });
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void renderLocalRoster(String safeUid, LinearLayout branchList, TextView header, List<Map<String, Object>> roster) {
        branchList.removeAllViews();
        int count = 0;
        
        List<Map<String, Object>> familyIds = new ArrayList<>();
        List<Map<String, Object>> friendIds = new ArrayList<>();

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        
        branchList.addView(createGroupHeading("NETWORK GATEWAY (PRIMARY OWNER)"));
        Map<String, String> myDos = db.getLocalDossier(safeUid);
        SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String myName = (myDos != null && myDos.get("fullName") != null) ? myDos.get("fullName") : jPrefs.getString("user_nickname", "User Identity");
        branchList.addView(buildTreeNode(safeUid, myName, true, true, null, "OWNER"));

        for (Map<String, Object> node : roster) {
            String rUid = (String) node.get("uid_1");
            if (rUid == null || rUid.equals(safeUid)) continue;
            
            Object frFlag = node.get("is_friend");
            boolean isFriendNode = false;
            if (frFlag instanceof Number) isFriendNode = ((Number)frFlag).intValue() == 1;
            else if (frFlag instanceof Boolean) isFriendNode = (Boolean)frFlag;
            
            if (isFriendNode) friendIds.add(node);
            else familyIds.add(node);
            count++;
        }

        if (!familyIds.isEmpty()) {
            branchList.addView(createGroupHeading("FAMILY NETWORK (" + familyIds.size() + ")"));
            for (Map<String, Object> fNode : familyIds) {
                String fid = (String) fNode.get("uid_1");
                String name = (String) fNode.get("fullName");
                String roleStr = (String) fNode.get("role_type");
                if (roleStr == null) roleStr = "Member";
                
                branchList.addView(buildTreeNode(fid, name != null ? name : "Linked Asset", false, false, null, roleStr));
            }
        }

        if (!friendIds.isEmpty()) {
            branchList.addView(createGroupHeading("FRIENDS NETWORK (" + friendIds.size() + ")"));
            for (Map<String, Object> frNode : friendIds) {
                String frid = (String) frNode.get("uid_1");
                String name = (String) frNode.get("fullName");
                String roleStr = (String) frNode.get("role_type");
                if (roleStr == null) roleStr = "Friend";
                
                branchList.addView(buildTreeNode(frid, name != null ? name : "Friend", false, false, null, roleStr));
            }
        }

        header.setText("TOTAL ACTIVE DEVICES IN GROUP: " + (count + 1));
    }
    
    private void fetchFriendMatrix(String safeUid, String myGroupId, LinearLayout branchList, StringBuilder authorizedUids, TextView header, long currentCount) {
        com.google.firebase.database.DatabaseReference targetRef = (myGroupId != null) 
            ? com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGroupId).child("members")
            : com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(safeUid);

        targetRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot fmSnap) {
                    List<Map<String, Object>> familyIds = new ArrayList<>();
                    List<Map<String, Object>> friendIds = new ArrayList<>();
                    
                    DatabaseHelper db = DatabaseHelper.getInstance(NetworkGatewayActivity.this);
                    int totalP2P = 0;
                    
                    List<Map<String, Object>> roster = db.getOfflineRoster();

                    for (com.google.firebase.database.DataSnapshot p2pMember : fmSnap.getChildren()) {
                        String memberId = p2pMember.getKey();
                        if (memberId == null || memberId.equals(safeUid)) continue;
                        
                        boolean exists = false;
                        for (Map<String, Object> node : roster) {
                            if (memberId.equals(node.get("uid_1"))) { exists = true; break; }
                        }
                        
                        if (!exists) {
                            String displayName = "Relational Identity (" + (memberId.length() > 5 ? memberId.substring(0, 5) : memberId) + ")";
                            db.saveRosterNode(memberId, displayName, "Member", "P2P Synchronized", false, false, "System Root", safeUid, memberId, "AUTO_SYNC");
                        }
                    }

                    List<Map<String, Object>> liveRoster = db.getOfflineRoster();
                    
                    for (Map<String, Object> node : liveRoster) {
                        String rUid = (String) node.get("uid_1");
                        if (rUid == null || rUid.equals(safeUid)) continue;
                        if (authorizedUids.toString().contains(rUid)) continue; 
                        
                        if (fmSnap.hasChild(rUid)) {
                            authorizedUids.append(rUid).append(",");
                            totalP2P++;
                            
                            Object frFlag = node.get("is_friend");
                            boolean isFriendNode = false;
                            if (frFlag instanceof Number) isFriendNode = ((Number)frFlag).intValue() == 1;
                            else if (frFlag instanceof Boolean) isFriendNode = (Boolean)frFlag;
                            
                            if (isFriendNode) friendIds.add(node);
                            else familyIds.add(node);
                        }
                    }
                    
                    header.setText("TOTAL ACTIVE DEVICES IN GROUP: " + (fmSnap.getChildrenCount() + 1));
                    branchList.removeAllViews();

                    Map<String, String> myDos = db.getLocalDossier(safeUid);
                    SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
                    String myName = (myDos != null && myDos.get("fullName") != null) ? myDos.get("fullName") : jPrefs.getString("user_nickname", "User Identity");
                    
                    branchList.addView(createGroupHeading("NETWORK GATEWAY (PRIMARY OWNER)"));
                    branchList.addView(buildTreeNode(safeUid, myName, true, true, null, "OWNER"));
                    
                    if (!familyIds.isEmpty()) {
                        View familyHeader = createGroupHeading("FAMILY NETWORK (" + familyIds.size() + ")");
                        branchList.addView(familyHeader);
                        
                        for (Map<String, Object> fNode : familyIds) {
                            String fid = (String) fNode.get("uid_1");
                            String name = (String) fNode.get("fullName");
                            String roleStr = (String) fNode.get("role_type");
                            if (roleStr == null) roleStr = "Member";
                            
                            branchList.addView(buildTreeNode(fid, name != null ? name : "Linked Asset", false, false, null, roleStr));
                        }
                    }

                    if (!friendIds.isEmpty()) {
                        branchList.addView(createGroupHeading("FRIENDS NETWORK (" + friendIds.size() + ")"));
                        for (Map<String, Object> frNode : friendIds) {
                            String frid = (String) frNode.get("uid_1");
                            String name = (String) frNode.get("fullName");
                            String roleStr = (String) frNode.get("role_type");
                            if (roleStr == null) roleStr = "Friend";
                            
                            branchList.addView(buildTreeNode(frid, name != null ? name : "Friend", false, false, null, roleStr));
                        }
                    }
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
        cb.setPadding(20, 20, 20, 20);
        
        cb.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == KeyEvent.ACTION_UP && (k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_DPAD_CENTER)) {
                cb.setChecked(!cb.isChecked());
                return true;
            }
            return false;
        });

        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbLp.setMargins(60, 10, 0, 5);
        cb.setLayoutParams(cbLp);
        return cb;
    }
    
    private LinearLayout buildTreeNode(String uid, String defaultName, boolean isRoot, boolean hasChildren, View payloadToExpand, String roleTag) {
        LinearLayout node = new LinearLayout(this);
        node.setOrientation(LinearLayout.HORIZONTAL);
        node.setGravity(Gravity.CENTER_VERTICAL);
        node.setFocusable(true);
        node.setClickable(true);

        node.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == KeyEvent.ACTION_UP && (k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_DPAD_CENTER)) {
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
        nodeLp.setMargins(isRoot ? 0 : scale(40), 0, 0, scale(40)); 
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
            ImageView deleteIcon = new ImageView(this);
            deleteIcon.setImageResource(android.R.drawable.ic_menu_delete);
            deleteIcon.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN); 
            deleteIcon.setPadding(scale(10), scale(10), scale(10), scale(10)); 
            
            deleteIcon.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Eject Target Node?")
                    .setMessage("WARNING: This will permanently purge (" + uid + ") from your Secure Tracking Matrix!")
                    .setPositiveButton("PURGE", (d, w) -> {
                        String myGrp = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);
                        if (myGrp != null) {
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("members").child(uid).removeValue();
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(uid).child("active_group").removeValue();
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
            });
            
            int delSize = com.jdroidx.jfindme.ui.base.UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
            node.addView(deleteIcon, new LinearLayout.LayoutParams(delSize, delSize)); 
        }
        
        if (hasChildren && payloadToExpand != null) {
            node.setOnClickListener(v -> {
                payloadToExpand.setVisibility(payloadToExpand.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });
        }
        
        com.jdroidx.jfindme.data.remote.FirebaseComm.getLocationsRef().child(uid)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot locSnap) {
                        String realName = locSnap.child("userName").getValue(String.class);
                        String finalNameRaw = (realName != null && !realName.trim().isEmpty()) ? realName : defaultName;
                        
                        String cloudFamRole = locSnap.child("role_family").getValue(String.class);
                        String cloudFriendRole = locSnap.child("role_friend").getValue(String.class);
                        String activeTag = "Syncing";

                        if (cloudFriendRole != null && cloudFriendRole.contains("Friend")) {
                            activeTag = "FRIEND";
                        } else if (cloudFamRole != null && !cloudFamRole.equals("None")) {
                            activeTag = cloudFamRole.toUpperCase();
                        } else {
                            DatabaseHelper db = DatabaseHelper.getInstance(NetworkGatewayActivity.this);
                            Map<String, String> localDos = db.getLocalDossier(uid);
                            if (localDos != null) {
                                String rType = localDos.get("role_type");
                                if (rType != null && !rType.equals("None")) activeTag = rType.toUpperCase();
                            }
                        }
                        nameView.setText(finalNameRaw + " (" + activeTag + ")");
                        
                        Long timestamp = locSnap.child("timestamp").getValue(Long.class);
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        String timeStr = (timestamp != null) ? sdf.format(new Date(timestamp)) : "Offline";

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

                        View.OnClickListener mapLock = v -> {
                            Intent mapIntent = new Intent(NetworkGatewayActivity.this, com.jdroidx.jfindme.ui.map.MapActivity.class);
                            mapIntent.putExtra("target_uid", uid);
                            startActivity(mapIntent);
                        };
                        nameView.setOnClickListener(mapLock);
                        idView.setOnClickListener(mapLock);
                        
                        String avatarId = locSnap.child("avatar_id").getValue(String.class);
                        Bitmap dynamicBmp = null;
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
                            avatar.setColorFilter(getGradientStart());
                        }
                }
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
            
        return node;
    }

    private View buildGatewayCard(String title, String subtitle, int iconRes, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(scale(40), scale(50), scale(40), scale(50));
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(listener);

        card.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == KeyEvent.ACTION_UP && (k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });

        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, card, "CARD_STANDARD");

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart()); 
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

    private LinearLayout buildGatewaySelectionLayout() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 30);

        container.addView(buildGatewayCard("☁️ OPTION A [Remote]", 
            "Firebase Pairing + QR Codes\nWorks globally over Cellular Data.", 
            android.R.drawable.ic_popup_sync, v -> {
                optionGatewayPanel.setVisibility(View.GONE);
                remotePairingPanel.setVisibility(View.VISIBLE);
            }), lp);

        container.addView(buildGatewayCard("🛰️ OPTION B [Nearby]", 
            "Local Pairing (No Internet)\nWorks via Bluetooth & Wi-Fi Direct.", 
            android.R.drawable.ic_menu_mylocation, v -> {
                optionGatewayPanel.setVisibility(View.GONE);
                localPairingPanel.setVisibility(View.VISIBLE);
            }), lp);

        // 🧬 V70.1: Integrate Restore Action directly into selection matrix
        container.addView(buildRestoreActionTile(), lp);

        return container;
    }

    private View buildRestoreActionTile() {
        LinearLayout lp = new LinearLayout(this);
        lp.setOrientation(LinearLayout.VERTICAL);
        lp.setPadding(0, 40, 0, 40);

        View card = buildGatewayCard("🔄 RESTORE OLD GROUP", 
            "Synchronize and reclaim legacy device links from cloud history.", 
            android.R.drawable.ic_menu_search, v -> showRestoreOldGroupDialog());
        
        lp.addView(card);
        return lp;
    }

    private LinearLayout buildRemoteOptionLayout() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        
        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, container, "CARD_DASHBOARD");
        
        TextView hostTitle = new TextView(this);
        hostTitle.setText("HOST A FAMILY SESSION");
        hostTitle.setTextColor(getGradientStart());
        hostTitle.setTextSize(12);
        hostTitle.setTypeface(null, Typeface.BOLD);
        hostTitle.setGravity(Gravity.CENTER);
        hostTitle.setPadding(0, 20, 0, 40);
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
        tvSyncStatus.setTextColor(Color.parseColor("#00E676"));
        tvSyncStatus.setGravity(Gravity.CENTER);
        tvSyncStatus.setTextSize(12);
        tvSyncStatus.setPadding(0, 20, 0, 30);
        container.addView(tvSyncStatus);

        ivQrCode = new ImageView(this);
        ivQrCode.setPadding(20, 20, 20, 20);
        ivQrCode.setBackgroundColor(Color.WHITE);
        ivQrCode.setVisibility(View.GONE);
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(400, 400);
        qrLp.gravity = Gravity.CENTER;
        qrLp.setMargins(0, 0, 0, 30);
        container.addView(ivQrCode, qrLp);
        
        View btnGenerate = createActionTile("GENERATE UNIQUE TOKEN", android.R.drawable.ic_popup_sync, v -> {
            // FIX #1: No role argument — role is negotiated AFTER child knocks, not at token creation.
            executeHostSessionSpinup();
        });
        container.addView(btnGenerate);

        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(80, Color.red(getSecondaryTextColor()), Color.green(getSecondaryTextColor()), Color.blue(getSecondaryTextColor())));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        divLp.setMargins(0, 40, 0, 40);
        container.addView(divider, divLp);

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

    private View createActionTile(String label, int iconRes, View.OnClickListener listener) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(scale(40), scale(30), scale(40), scale(30));
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setOnClickListener(listener);

        tile.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == KeyEvent.ACTION_UP && (k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_DPAD_CENTER)) {
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
        hostTitle.setPadding(0, 20, 0, 40);
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
        tvLocalSyncStatus.setPadding(0, 20, 0, 30);
        container.addView(tvLocalSyncStatus);

        ivLocalQrCode = new ImageView(this);
        ivLocalQrCode.setPadding(20, 20, 20, 20);
        ivLocalQrCode.setBackgroundColor(Color.WHITE);
        ivLocalQrCode.setVisibility(View.GONE);
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(400, 400);
        qrLp.gravity = Gravity.CENTER;
        qrLp.setMargins(0, 0, 0, 30);
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

    /**
     * FIX #1: Removed 'assignedRoles' parameter — role is assigned AFTER child knocks.
     * The FirebasePairingManager.startHostPairingSession signature no longer accepts assignedRoles.
     */
    private void executeHostSessionSpinup() {
        if (tvCode != null) tvCode.setText("NEGOTIATING...");
        if (tvSyncStatus != null) {
            tvSyncStatus.setText("ALLOCATING CLOUD NODE...");
            tvSyncStatus.setTextColor(Color.YELLOW);
        }
        if (ivQrCode != null) ivQrCode.setVisibility(View.GONE);

        FirebasePairingManager.INSTANCE.startHostPairingSession(this, (qrBitmap, code) -> {
            runOnUiThread(() -> {
                if (code != null && !code.isEmpty()) {
                    if (tvCode != null) tvCode.setText(code);
                    if (tvSyncStatus != null) tvSyncStatus.setText("LISTENING FOR PAIRED DEVICES");
                    if (qrBitmap != null && ivQrCode != null) {
                        ivQrCode.setImageBitmap(qrBitmap);
                        ivQrCode.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvCode != null) tvCode.setText("NETWORK ERROR");
                    if (tvSyncStatus != null) {
                        tvSyncStatus.setText("FIREBASE REJECTED");
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

    private void executeChildJoinRequest(String inputMatrix, String selectedRole) {
        String cleaned = inputMatrix.trim().toUpperCase();
        if(cleaned.length() != 6) {
            Toast.makeText(this, "Invalid code length.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Injecting Handshake Identity [" + selectedRole + "]...", Toast.LENGTH_SHORT).show();
        
        FirebasePairingManager.INSTANCE.requestJoinGroup(this, cleaned, selectedRole, (success, message) -> {
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
            com.jdroidx.jfindme.data.remote.FirebaseComm.getPairingRef().child(activeCode)
                .get().addOnSuccessListener(snap -> {
                    String prelimRole = snap.child("child_role_preliminary").getValue(String.class);

                    // FIX #2: Do NOT silently default to "Friends" if the role is null or blank.
                    // The child's role write may not have propagated yet (race condition).
                    // Alert the host to ask Device B to re-tap their role selection.
                    if (prelimRole == null || prelimRole.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(this,
                            "\u26a0\ufe0f Child role not received yet. Ask Device B to re-submit their code.",
                            Toast.LENGTH_LONG).show());
                        return;
                    }

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

        List<String> validHostRoles = new ArrayList<>();
        if (childPrelimRole.equals("Friends")) validHostRoles.add("Friends");
        else if (childPrelimRole.equals("Spouse")) validHostRoles.add("Spouse");
        else if (childPrelimRole.equals("Child")) {
            validHostRoles.add("Father");
            validHostRoles.add("Mother");
            validHostRoles.add("Guardian");
        } else if (Arrays.asList("Father", "Mother", "Guardian").contains(childPrelimRole)) {
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
                
                // 🧬 V68.1: Reciprocal Role Matrix Resolution
                String childRoleToSave = childPrelimRole; 
                String hostRoleToSave = hostRole;

                if (childPrelimRole.equals("Child")) {
                    childRoleToSave = "Child";
                    hostRoleToSave = hostRole; // Father, Mother, or Guardian
                } else if (java.util.Arrays.asList("Father", "Mother", "Guardian").contains(childPrelimRole)) {
                    childRoleToSave = childPrelimRole;
                    hostRoleToSave = "Child";
                } else if (childPrelimRole.equals("Spouse")) {
                    childRoleToSave = "Spouse";
                    hostRoleToSave = "Spouse";
                }

                boolean isFriend = hostRole.equals("Friends");
                final String finalChildRole = childRoleToSave;
                final String finalHostRole = hostRoleToSave;
                final boolean finalIsFriend = isFriend;

                FirebasePairingManager.INSTANCE.approveHandshake(this, activeCode, childId, finalHostRole, finalChildRole, success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(this, "Handshake Completed! Assets Linked.", Toast.LENGTH_SHORT).show();
                            boolean localUpdateSuccess = DatabaseHelper.getInstance(this).updateCitizenRole(childId, finalIsFriend ? "None" : finalChildRole, finalIsFriend ? "Friends" : "None", finalIsFriend);
                            Toast.makeText(this, "Handshake Completed! Local Update: " + (localUpdateSuccess ? "PASS" : "FAIL"), Toast.LENGTH_SHORT).show();
                            initiateCloudSync(safeUid, branchList, activeSyncHeader); // Force refresh
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
        
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
            applyDialogTheme(dialog);
        }
    }

    @Override
    protected void triggerHelpDialog() {
        triggerTacticalHelpDialog();
    }

    private void triggerTacticalHelpDialog() {
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 40, 40, 40);

        TextView t1 = new TextView(this);
        t1.setText("TACTICAL HANDSHAKE GUIDE");
        t1.setTextColor(getGradientStart());
        t1.setTextSize(20);
        t1.setTypeface(null, Typeface.BOLD);
        t1.setPadding(0, 0, 0, 20);
        container.addView(t1);

        addHelpSection(container, "1. ROLE SELECTION", "Before pairing, identify your relationship. Assets are categorized to prevent relational drift in the matrix.");
        addHelpSection(container, "2. OPTION A (GLOBAL)", "Use Cloud Handshake if the target is on a different network. Scan QR to propagate UID securely.");
        addHelpSection(container, "3. OPTION B (LOCAL)", "Use P2P Matrix for offline pairing. Requires Bluetooth & Wi-Fi Direct proximity.");
        addHelpSection(container, "4. HANDSHAKE STATUS", "Diagnostic icons at the bottom indicate sync health and accessibility status.");

        new AlertDialog.Builder(this)
            .setView(sv)
            .setPositiveButton("UNDERSTOOD", null)
            .show();
    }

    private void addHelpSection(LinearLayout container, String title, String body) {
        TextView st = new TextView(this);
        st.setText(title);
        st.setTextColor(getTextColor());
        st.setTextSize(14);
        st.setTypeface(null, Typeface.BOLD);
        st.setPadding(0, 20, 0, 5);
        container.addView(st);

        TextView sb = new TextView(this);
        sb.setText(body);
        sb.setTextColor(getSecondaryTextColor());
        sb.setTextSize(12);
        sb.setLineSpacing(0, 1.2f);
        container.addView(sb);
    }

    private void showRestoreOldGroupDialog() {
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("SCANNING FORENSIC CLOUD MATRIX...");
        pd.setCancelable(false);
        pd.show();
        
        final String agentId = getSharedPreferences("system_registry", MODE_PRIVATE).getString("agent_id", "AGENT_NOT_FOUND");
        final String safeAgentId = agentId;

        com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(safeAgentId).child("active_group").get().addOnSuccessListener(grpSnap -> {
            String myGrp = grpSnap.getValue(String.class);
            final String finalGrp = (myGrp == null) ? "" : myGrp;
            if (finalGrp.isEmpty()) {
                pd.dismiss();
                Toast.makeText(this, "Network Hub is empty. You must build a new Matrix.", Toast.LENGTH_LONG).show();
                return;
            }
            
            final List<String> currentlyPairedUids = new ArrayList<>();

            java.util.function.Consumer<Void> executeAssetScan = unused -> {
                com.jdroidx.jfindme.data.remote.FirebaseComm.INSTANCE.getLocationsRef().get().addOnSuccessListener(snapshot -> {
                    final List<String> assetNames = new ArrayList<>();
                    final List<String> assetUids = new ArrayList<>();
                    
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        com.jdroidx.jfindme.data.model.UserLocation loc = child.getValue(com.jdroidx.jfindme.data.model.UserLocation.class);
                        if (loc != null) {
                            String rId = loc.getResolvedUserId();
                            if (!rId.equals(safeAgentId)) {
                                if (!assetUids.contains(rId) && currentlyPairedUids.contains(rId)) {
                                    assetNames.add(loc.getUserName());
                                    assetUids.add(rId);
                                }
                            }
                        }
                    }
                    
                    pd.dismiss();
                    if (assetNames.isEmpty()) {
                        Toast.makeText(this, "No legacy assets detected in History Database.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    final String[] items = assetNames.toArray(new String[0]);
                    final boolean[] checkedItems = new boolean[items.length];
                    for (int i=0; i < items.length; i++) {
                        if (currentlyPairedUids.contains(assetUids.get(i))) checkedItems[i] = true;
                    }
                    
                    new AlertDialog.Builder(this)
                        .setTitle("Select Assets to Recover")
                        .setMultiChoiceItems(items, checkedItems, (dialogEx, which, isChecked) -> checkedItems[which] = isChecked)
                        .setPositiveButton("RESTORE", (dialogEx, which) -> {
                            String targetGroup = finalGrp;
                            if (targetGroup.isEmpty()) {
                                targetGroup = com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().push().getKey();
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(safeAgentId).child("active_group").setValue(targetGroup);
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(targetGroup).child("members").child(safeAgentId).setValue(true);
                                getSharedPreferences("system_registry", MODE_PRIVATE).edit().putString("active_group", targetGroup).apply();
                            }
                            
                            int restoredCount = 0;
                            for (int i=0; i < checkedItems.length; i++) {
                                if (checkedItems[i] && !currentlyPairedUids.contains(assetUids.get(i))) {
                                    String targetNode = assetUids.get(i);
                                    com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(targetGroup).child("members").child(targetNode).setValue(true);
                                    com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(targetNode).child("active_group").setValue(targetGroup);
                                    restoredCount++;
                                } else if (!checkedItems[i] && currentlyPairedUids.contains(assetUids.get(i))) {
                                    String targetNode = assetUids.get(i);
                                    com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(targetGroup).child("members").child(targetNode).removeValue();
                                    com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(targetNode).child("active_group").removeValue();
                                }
                            }
                            Toast.makeText(this, "Successfully Synchronized Array with " + restoredCount + " new assets!", Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("CLOSE", null)
                        .show();
                });
            };

            if (!finalGrp.isEmpty()) {
                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(finalGrp).child("members").get().addOnSuccessListener(memSnap -> {
                    if (memSnap.exists()) {
                        for (com.google.firebase.database.DataSnapshot m : memSnap.getChildren()) {
                            currentlyPairedUids.add(m.getKey());
                        }
                    }
                    executeAssetScan.accept(null);
                });
            } else {
                executeAssetScan.accept(null);
            }
        });
    }

    private void resetGateway() {
        if (optionGatewayPanel != null) optionGatewayPanel.setVisibility(View.VISIBLE);
        if (remotePairingPanel != null) remotePairingPanel.setVisibility(View.GONE);
        if (localPairingPanel != null) localPairingPanel.setVisibility(View.GONE);
        
        String launchMode = getIntent().getStringExtra("LAUNCH_MODE");
        if ("RECOVERY".equals(launchMode)) {
            // 🚀 QA Fix: Do NOT removeExtra here; setupLogic needs it to trigger sync
            showRecoveryModeOverlay();
            // Hide the panels to avoid confusion during recovery
            if (optionGatewayPanel != null) optionGatewayPanel.setVisibility(View.GONE);
        }
    }

    private void showRecoveryModeOverlay() {
        ScrollView sv = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 40, 40, 40);

        TextView t1 = new TextView(this);
        t1.setText("RECOVERY ASSISTANT");
        t1.setTextColor(getGradientStart());
        t1.setTextSize(20);
        t1.setTypeface(null, Typeface.BOLD);
        t1.setPadding(0, 0, 0, 20);
        container.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText("Your Cloud Roster scan returned zero assets. This usually happens if you are using a new account or your devices are not yet paired to this group.");
        t2.setTextColor(getTextColor());
        t2.setTextSize(14);
        t2.setPadding(0, 0, 0, 40);
        container.addView(t2);

        // Targeted Action: Start Pairing
        View btnPair = createActionTile("PAIR NEW DEVICE", android.R.drawable.ic_popup_sync, v -> {
            optionGatewayPanel.setVisibility(View.GONE);
            remotePairingPanel.setVisibility(View.VISIBLE);
        });
        container.addView(btnPair);

        // Targeted Action: Force Re-Sync
        View btnSync = createActionTile("FORCE CLOUD RE-SYNC", android.R.drawable.ic_popup_sync, v -> {
            initiateCloudSync(safeUid, branchList, activeSyncHeader);
        });
        container.addView(btnSync);

        // Targeted Action: Check Identity
        View btnId = createActionTile("VIEW MY CLOUD IDENTITY", android.R.drawable.ic_menu_info_details, v -> {
            SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
            String agentId = jPrefs.getString("agent_id", "N/A");
            String group = jPrefs.getString("active_group", "NONE");
            new AlertDialog.Builder(this)
                .setTitle("Cloud Registry")
                .setMessage("My ID: " + agentId + "\nActive Hub: " + group)
                .setPositiveButton("OK", null)
                .show();
        });
        container.addView(btnId);

        new AlertDialog.Builder(this)
            .setView(sv)
            .setPositiveButton("DISMISS", null)
            .show();
    }

    private View createSectionHeader(String text) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart());
        header.setTextSize(UiScalingMatrixTable.scaleText(stage, 18, true));
        int pT = UiScalingMatrixTable.scalePadding(stage, 40);
        int pB = UiScalingMatrixTable.scalePadding(stage, 20);
        header.setPadding(40, pT, 40, pB);
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

    private void showTacticalHandshakeOverlay(String code) {
        android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE));

        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(40, 0, 0, 0));
        overlay.setClickable(true);
        overlay.setFocusable(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(60, 80, 60, 80);
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.argb(217, 255, 255, 255));
        gd.setCornerRadius(50f);
        gd.setStroke(4, getGradientStart());
        card.setBackground(gd);

        ImageView check = new ImageView(this);
        check.setImageResource(android.R.drawable.checkbox_on_background);
        check.setColorFilter(getGradientStart());
        card.addView(check, new LinearLayout.LayoutParams(180, 180));

        TextView tv = new TextView(this);
        tv.setText("IDENTITY RESOLVED");
        tv.setTextColor(getTextColor());
        tv.setPadding(0, 40, 0, 10);
        UiArchitect.applyText(this, tv, "TEXT_TITLE");
        card.addView(tv);

        TextView tvCodeRaw = new TextView(this);
        tvCodeRaw.setText("SCAN MATRIX: " + code);
        tvCodeRaw.setTextColor(getSecondaryTextColor());
        tvCodeRaw.setTextSize(11);
        card.addView(tvCodeRaw);

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(600, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.CENTER;
        overlay.addView(card, cardLp);

        ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();
        root.addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlay.animate().alpha(0f).setDuration(400).setStartDelay(2800).withEndAction(() -> root.removeView(overlay));
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.i(TAG, "Unified Gateway Screen Successfully Executed.");
        viewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        
        viewModel.getSyncStatus().observe(this, status -> {
            if (status != null && tvSyncStatus != null) {
                tvSyncStatus.setText(status);
            }
        });

        // 🛰️ RECOVERY PROTOCOL: Auto-trigger sync if launched from Reclaim failure
        String mode = getIntent().getStringExtra("LAUNCH_MODE");
        if ("RECOVERY".equals(mode)) {
            SystemLogger.INSTANCE.w(TAG, "RECOVERY MODE ACTIVE: Initiating Auto-Resolution...");
            
            new android.app.AlertDialog.Builder(this)
                .setTitle("🛰️ RECOVERY MODE ACTIVE")
                .setMessage("We are performing an aggressive scan of the forensic cloud matrix to rebuild your safety roster. \n\nPlease stay on this screen while we sync your existing nodes...")
                .setPositiveButton("UNDERSTOOD", null)
                .show();

            getIntent().removeExtra("LAUNCH_MODE"); // 🚀 Consume only AFTER sync trigger
            new android.os.Handler().postDelayed(() -> {
                initiateCloudSync(forensicAgentId, branchList, activeSyncHeader);
            }, 1000);
        }
    }

}
