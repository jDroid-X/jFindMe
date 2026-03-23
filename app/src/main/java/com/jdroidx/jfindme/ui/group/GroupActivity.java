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

import androidx.activity.result.ActivityResultLauncher;

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
                    if (isScanningLocal) {
                        etLocalCode.setText(result.getContents());
                        executeLocalJoinRequest(result.getContents());
                    } else {
                        etCode.setText(result.getContents());
                        executeChildJoinRequest(result.getContents());
                    }
                }
            });

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor()); // Grey/Dark contrast background

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(50, 80, 50, 60);
        applyLinearGradient(header, 0f); 
        
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(this);
        title.setText("NETWORK GATEWAY");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        textBlock.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Device Pairing & Active Rosters");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        textBlock.addView(subtitle);
        header.addView(textBlock);
        
        mainRoot.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 100);

        // --- LAYER 1: IDENTITY CARD (Floating Box) ---
        LinearLayout identityCard = new LinearLayout(this);
        identityCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(identityCard, getCardColor(), 30f);
        identityCard.addView(buildIdentityBlock());
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 40);
        root.addView(identityCard, cardLp);

        // Initialize Virtual Navigation Stacks
        optionGatewayPanel = buildGatewaySelectionLayout();
        remotePairingPanel = buildRemoteOptionLayout();
        localPairingPanel = buildLocalOptionLayout();

        root.addView(optionGatewayPanel);
        root.addView(remotePairingPanel);
        root.addView(localPairingPanel);

        // --- LAYER 3: ROSTER CARD ---
        LinearLayout rosterCard = new LinearLayout(this);
        rosterCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(rosterCard, getCardColor(), 30f);
        rosterCard.setPadding(0, 20, 0, 30);
        rosterCard.addView(buildActiveRosterList());
        root.addView(rosterCard, cardLp);

        // Default State
        optionGatewayPanel.setVisibility(View.VISIBLE);
        remotePairingPanel.setVisibility(View.GONE);
        localPairingPanel.setVisibility(View.GONE);

        scroll.addView(root);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        return mainRoot;
    }

    private View buildIdentityBlock() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(30, 40, 30, 40); // Internal padding inside the card
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView myAvatar = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, myAvatar);
        
        // Premium Transparent Avatar Squircle (Rounded Square) with Neon Ring
        android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
        c.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        c.setCornerRadius(60f); // High curve iOS-style borders
        c.setColor(Color.TRANSPARENT);
        c.setStroke(4, getGradientStart());
        myAvatar.setBackground(c);
        
        row.addView(myAvatar, new LinearLayout.LayoutParams(140, 140));
        
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(40, 0, 0, 0);
        
        TextView myName = new TextView(this);
        myName.setText(prefs != null ? prefs.getString("user_nickname", "User Identity") : "User Identity");
        myName.setTextColor(getTextColor()); // Adapts natively locally!
        myName.setTextSize(16);
        myName.setTypeface(null, Typeface.BOLD);
        textBlock.addView(myName);
        
        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        String safeUid = (myUid != null) ? myUid : "MOCK_U123";
        
        TextView myId = new TextView(this);
        myId.setText(safeUid.toUpperCase());
        myId.setTextColor(getSecondaryTextColor()); // Premium soft text
        myId.setTextSize(11);
        textBlock.addView(myId);
        
        // Asynchronously check Firebase for Pairing Group!
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(safeUid).child("active_group").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                String activeGroupId = snapshot.getValue(String.class);
                if (activeGroupId != null) {
                    textBlock.removeView(textBlock.findViewWithTag("status_tag"));
                    TextView tvStatus = new TextView(GroupActivity.this);
                    tvStatus.setTag("status_tag");
                    tvStatus.setText("LOCKED TO HUB: " + activeGroupId);
                    tvStatus.setTextColor(Color.parseColor("#00E676")); // Neon Pulse Green
                    tvStatus.setTextSize(10);
                    tvStatus.setTypeface(null, Typeface.BOLD);
                    tvStatus.setPadding(0, 10, 0, 0);
                    textBlock.addView(tvStatus);
                }
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
        
        row.addView(textBlock);
        return row;
    }

    private View buildActiveRosterList() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        
        TextView header = (TextView) createSectionHeader("DEVICES IN YOUR PAIRING GROUP: CALCULATING...");
        container.addView(header);

        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        String safeUid = (myUid != null) ? myUid : "MOCK_U123";
        
        // The Expandable Dropdown Container for Connected Avatars
        LinearLayout branchList = new LinearLayout(this);
        branchList.setOrientation(LinearLayout.VERTICAL);
        branchList.setVisibility(View.GONE); // Hidden initially per Accordion UI rules
        
        // Root Node (The Primary Mobile User)
        LinearLayout rootNode = buildTreeNode(safeUid, "YOU (Primary Mobile User)", true, true, branchList);
        container.addView(rootNode);
        container.addView(branchList);

        // Fetch Local Group State to Isolate Network
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(safeUid).child("active_group").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot myGroupSnap) {
                String myGroupId = myGroupSnap.getValue(String.class);
                if (myGroupId == null || myGroupId.isEmpty()) {
                    header.setText("TOTAL ACTIVE DEVICES IN GROUP: 1 (Only You)");
                    branchList.removeAllViews();
                    return;
                }

                // Query ONLY users who share the identical active_group link!
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").orderByChild("active_group").equalTo(myGroupId)
                    .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                            branchList.removeAllViews();
                            long groupSize = snapshot.getChildrenCount();
                            header.setText("TOTAL ACTIVE DEVICES IN GROUP: " + groupSize);
                            
                            // QA Universal Sync Arrays
                            java.util.List<LinearLayout> guardNodes = new java.util.ArrayList<>();
                            java.util.List<LinearLayout> spouseNodes = new java.util.ArrayList<>();
                            java.util.List<LinearLayout> childNodes = new java.util.ArrayList<>();
                            java.util.List<LinearLayout> friendNodes = new java.util.ArrayList<>();
                            
                            com.jdroidx.jfindme.data.local.CitizenDbHelper db = new com.jdroidx.jfindme.data.local.CitizenDbHelper(GroupActivity.this);
                            
                            for (com.google.firebase.database.DataSnapshot groupMember : snapshot.getChildren()) {
                                String memberId = groupMember.getKey();
                                if (memberId == null || memberId.equals(safeUid)) continue; // Skip self in the branch
                                
                                java.util.Map<String, String> dos = db.getLocalDossier(memberId);
                                String matchedRole = "CHILD";
                                if (dos != null) {
                                    String rFam = dos.get("role_family");
                                    String rFr = dos.get("role_friend");
                                    if (rFr != null && rFr.toUpperCase().contains("FRIEND")) matchedRole = "FRIEND";
                                    else if (rFam != null) {
                                        String rFamUp = rFam.toUpperCase();
                                        if (rFamUp.contains("FATHER") || rFamUp.contains("MOTHER") || rFamUp.contains("OWNER")) matchedRole = "OWNER";
                                        else if (rFamUp.contains("SPOUSE")) matchedRole = "SPOUSE";
                                    }
                                } else {
                                    Boolean isDad = groupMember.child("role_father").getValue(Boolean.class);
                                    Boolean isMom = groupMember.child("role_mother").getValue(Boolean.class);
                                    Boolean isSpo = groupMember.child("role_spouse").getValue(Boolean.class);
                                    Boolean isFri = groupMember.child("role_friends").getValue(Boolean.class);
                                    if (Boolean.TRUE.equals(isDad) || Boolean.TRUE.equals(isMom)) matchedRole = "OWNER";
                                    else if (Boolean.TRUE.equals(isSpo)) matchedRole = "SPOUSE";
                                    else if (Boolean.TRUE.equals(isFri)) matchedRole = "FRIEND";
                                }
                                
                                LinearLayout childNodeLayout = buildTreeNode(memberId, "Syncing Profile...", false, false, null);
                                switch (matchedRole) {
                                    case "OWNER": guardNodes.add(childNodeLayout); break;
                                    case "SPOUSE": spouseNodes.add(childNodeLayout); break;
                                    case "FRIEND": friendNodes.add(childNodeLayout); break;
                                    default: childNodes.add(childNodeLayout); break;
                                }
                            }
                            
                            // Native Array Builder Injection
                            String[] titles = {"OWNER", "SPOUSE", "CHILD", "FRIEND"};
                            java.util.List[] matrix = {guardNodes, spouseNodes, childNodes, friendNodes};
                            
                            for (int i = 0; i < titles.length; i++) {
                                java.util.List<LinearLayout> targetList = matrix[i];
                                if (!targetList.isEmpty()) {
                                    // Render Checkbox identical to Profile and Citizen Activities
                                    android.widget.CheckBox cb = new android.widget.CheckBox(GroupActivity.this);
                                    cb.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint())); // 🚀 Theme Fix Matrix
                                    cb.setText(titles[i] + " Network (" + targetList.size() + " Connected)");
                                    cb.setTextColor(getTextColor());
                                    cb.setChecked(true);
                                    cb.setClickable(false); // Display only physically
                                    cb.setPadding(20, 20, 20, 20);
                                    
                                    android.widget.LinearLayout.LayoutParams cbLp = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                                    cbLp.setMargins(60, 10, 0, 5);
                                    branchList.addView(cb, cbLp);
                                    
                                    for (LinearLayout node : targetList) {
                                        // Padding natively preserved from buildTreeNode!
                                        branchList.addView(node);
                                    }
                                }
                            }
                        }
                        @Override
                        public void onCancelled(com.google.firebase.database.DatabaseError error) {}
                    });
            }
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });

        return container;
    }
    
    // Abstracted Tree Builder for UX Hierarchies
    private LinearLayout buildTreeNode(String uid, String defaultName, boolean isRoot, boolean hasChildren, View payloadToExpand) {
        LinearLayout node = new LinearLayout(this);
        node.setOrientation(LinearLayout.HORIZONTAL);
        node.setGravity(Gravity.CENTER_VERTICAL);
        
        // Render as a Professional Compact Tile Stringent!
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(getCardColor());
        gd.setCornerRadius(25f);
        node.setBackground(gd);
        node.setElevation(8f);
        
        LinearLayout.LayoutParams nodeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        // Sync vertical gap mathematically identical to Identity Card gap (40px) from above section!
        nodeLp.setMargins(isRoot ? 0 : 40, 0, 0, 40); 
        node.setLayoutParams(nodeLp);
        node.setPadding(30, 20, 30, 20); // Restoring perfect native internal cushion!
        
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        avatar.setPadding(10, 10, 10, 10);
        
        // Exact geometric mirror of the top Owner Tile's Squircle Frame!
        android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
        c.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        c.setCornerRadius(60f); 
        c.setColor(Color.TRANSPARENT);
        c.setStroke(4, getGradientStart());
        avatar.setBackground(c);
        
        node.addView(avatar, new LinearLayout.LayoutParams(140, 140)); // Restored native hardware UI footprint
        
        LinearLayout textData = new LinearLayout(this);
        textData.setOrientation(LinearLayout.VERTICAL);
        textData.setPadding(25, 0, 0, 0); // Tighter lateral alignment against Avatar
        
        TextView nameView = new TextView(this);
        nameView.setText(defaultName);
        nameView.setTextColor(isRoot ? getGradientStart() : getTextColor()); // Flawless DB Thematic Injection!
        nameView.setTypeface(null, Typeface.BOLD);
        textData.addView(nameView);
        
        TextView idView = new TextView(this);
        idView.setText(isRoot ? "TAP TO EXPAND/COLLAPSE GROUP ▲▼" : "Encrypted Network Node");
        idView.setTextColor(getSecondaryTextColor());
        idView.setTextSize(10);
        textData.addView(idView);
        
        node.addView(textData, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        if (!isRoot) {
            ImageView playIcon = new ImageView(this);
            playIcon.setImageResource(android.R.drawable.ic_media_play);
            playIcon.setColorFilter(getGradientStart(), android.graphics.PorterDuff.Mode.SRC_IN); // Thematically maps the Play Icon!
            playIcon.setPadding(10, 10, 10, 10); // Minimum interaction padding
            
            playIcon.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Hardware Edge ID: " + uid, android.widget.Toast.LENGTH_LONG).show();
            });
            
            node.addView(playIcon, new LinearLayout.LayoutParams(70, 70)); // Slim fixed bounding box!
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
            .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot locSnap) {
                        String realName = locSnap.child("userName").getValue(String.class);
                        String finalNameRaw = (realName != null && !realName.trim().isEmpty()) ? realName : defaultName;
                        if (isRoot) {
                            finalNameRaw = "YOU (" + finalNameRaw + ")";
                        } else {
                            com.jdroidx.jfindme.data.local.CitizenDbHelper db = new com.jdroidx.jfindme.data.local.CitizenDbHelper(GroupActivity.this);
                            java.util.Map<String, String> localDos = db.getLocalDossier(uid);
                            if (localDos != null) {
                                String fRole = localDos.get("role_family");
                                String frRole = localDos.get("role_friend");
                                String tag = "Syncing";
                                if (fRole != null && !fRole.equals("None")) tag = fRole.toUpperCase();
                                if (frRole != null && frRole.contains("Friend")) tag = "FRIEND";
                                finalNameRaw = finalNameRaw + " (" + tag + ")";
                            } else {
                                finalNameRaw = finalNameRaw + " (GUEST)";
                            }
                        }
                        nameView.setText(finalNameRaw);
                        
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
     * The Master Branch: Users dictate network architecture.
     */
    private LinearLayout buildGatewaySelectionLayout() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        container.addView(createBigTouchButton("☁️ OPTION A [Remote]", "Firebase Pairing + QR Codes\nWorks globally over Cellular Data.", v -> {
            optionGatewayPanel.setVisibility(View.GONE);
            remotePairingPanel.setVisibility(View.VISIBLE);
        }));

        container.addView(createBigTouchButton("📡 OPTION B [Nearby]", "Local Pairing (No Internet)\nWorks via Bluetooth & Wi-Fi Direct.", v -> {
            optionGatewayPanel.setVisibility(View.GONE);
            localPairingPanel.setVisibility(View.VISIBLE);
        }));

        // 🛠 QA Backdoor: Direct Array Binding Support
        container.addView(createSectionDivider());
        container.addView(createIconButton("🛠 RESTORE LOST ROSTER ASSETS", android.R.drawable.ic_menu_search, v -> {
            showRestoreLostRosterAssetsDialog();
        }));

        return container;
    }

    /**
     * Option A logic! Handles 'pairing_requests' -> 'groups' -> 'locations'
     */
    private LinearLayout buildRemoteOptionLayout() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        
        applySolidCardGeometry(container, getCardColor(), 30f); // Sandbox the Gateway Interface natively!
        container.setPadding(40, 20, 40, 60);
        
        container.addView(createSectionHeader("HOST A FAMILY SESSION"));
        
        tvCode = new TextView(this);
        tvCode.setText("------");
        tvCode.setTextColor(getTextColor());
        tvCode.setTextSize(36);
        tvCode.setGravity(Gravity.CENTER);
        container.addView(tvCode);

        ivQrCode = new ImageView(this);
        ivQrCode.setPadding(20, 20, 20, 20);
        ivQrCode.setBackgroundColor(Color.WHITE);
        ivQrCode.setVisibility(View.GONE);
        
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(400, 400);
        qrLp.gravity = Gravity.CENTER;
        qrLp.setMargins(0, 20, 0, 20);
        container.addView(ivQrCode, qrLp);
        
        tvSyncStatus = new TextView(this);
        tvSyncStatus.setText("READY");
        tvSyncStatus.setTextColor(Color.parseColor("#00E676")); // Premium Vector Pulse Green
        tvSyncStatus.setGravity(Gravity.CENTER);
        tvSyncStatus.setPadding(0, 10, 0, 20);
        container.addView(tvSyncStatus);

        container.addView(createIconButton("GENERATE UNIQUE TOKEN", android.R.drawable.ic_menu_rotate, v -> {
            executeHostSessionSpinup("AWAITING_ROLE"); // 🚀 Matrix automatically maps physical roles via the intercept prompt now!
        }));

        container.addView(createSectionDivider());

        container.addView(createSectionHeader("JOIN A FAMILY SESSION"));
        
        etCode = new EditText(this);
        etCode.setHint("ENTER 6-DIGIT CODE");
        etCode.setHintTextColor(getSecondaryTextColor()); // Bound to Theme Database
        etCode.setTextColor(getTextColor());          // Bound to Theme Database
        etCode.setTextSize(22);
        etCode.setGravity(Gravity.CENTER);
        etCode.setPadding(0, 40, 0, 20);
        etCode.setFocusableInTouchMode(true);
        etCode.requestFocus();
        etCode.setSingleLine(true);
        etCode.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        etCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                executeChildJoinRequest(etCode.getText().toString());
                return true;
            }
            return false;
        });
        container.addView(etCode);
        
        container.addView(createIconButton("LINK MANUALLY", android.R.drawable.ic_menu_edit, v -> {
            executeChildJoinRequest(etCode.getText().toString());
        }));
        
        container.addView(createIconButton("SCAN QR MATRIX", android.R.drawable.ic_menu_camera, v -> {
            isScanningLocal = false;
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan Parent's Secure Tracking Matrix");
            options.setBeepEnabled(true);
            options.setCameraId(0);
            barcodeLauncher.launch(options);
        }));

        container.addView(createSectionDivider());
        container.addView(createIconButton("BACK TO GATEWAY", android.R.drawable.ic_menu_revert, v -> resetGateway()));

        return container;
    }

    /**
     * Option B logic! Re-engineered to exactly mirror Option A UX with 8-character offline tokens!
     */
    private LinearLayout buildLocalOptionLayout() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        applySolidCardGeometry(container, getCardColor(), 30f); // Safely isolate experimental functions!
        container.setPadding(40, 20, 40, 60);

        container.addView(createSectionHeader("LOCAL P2P NETWORK"));
        
        TextView warning = new TextView(this);
        warning.setText("⚠️ WARNING: This mode bypasses the internet using experimental BLUETOOTH LE and WI-FI DIRECT.");
        warning.setTextColor(Color.parseColor("#FFCC00"));
        warning.setPadding(40, 0, 40, 20);
        warning.setTextSize(12);
        container.addView(warning);

        tvLocalCode = new TextView(this);
        tvLocalCode.setText("--------");
        tvLocalCode.setTextColor(getTextColor());
        tvLocalCode.setTextSize(36);
        tvLocalCode.setGravity(Gravity.CENTER);
        container.addView(tvLocalCode);

        ivLocalQrCode = new ImageView(this);
        ivLocalQrCode.setPadding(20, 20, 20, 20);
        ivLocalQrCode.setBackgroundColor(Color.WHITE);
        ivLocalQrCode.setVisibility(View.GONE);
        
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(400, 400);
        qrLp.gravity = Gravity.CENTER;
        qrLp.setMargins(0, 20, 0, 20);
        container.addView(ivLocalQrCode, qrLp);

        tvLocalSyncStatus = new TextView(this);
        tvLocalSyncStatus.setText("READY");
        tvLocalSyncStatus.setTextColor(Color.parseColor("#00E676"));
        tvLocalSyncStatus.setGravity(Gravity.CENTER);
        tvLocalSyncStatus.setPadding(0, 10, 0, 20);
        container.addView(tvLocalSyncStatus);

        container.addView(createIconButton("GENERATE OFFLINE MATRIX", android.R.drawable.ic_menu_rotate, v -> {
            tvLocalCode.setText("GENERATING...");
            ivLocalQrCode.setVisibility(View.GONE);
            executeLocalHostSessionSpinup();
        }));

        container.addView(createSectionDivider());

        etLocalCode = new EditText(this);
        etLocalCode.setHint("ENTER 8-DIGIT OFFLINE CODE");
        etLocalCode.setHintTextColor(getSecondaryTextColor()); // Inherit Theme Sync
        etLocalCode.setTextColor(getTextColor());          // Inherit Theme Sync
        etLocalCode.setTextSize(22);
        etLocalCode.setGravity(Gravity.CENTER);
        etLocalCode.setPadding(0, 40, 0, 20);
        etLocalCode.setFocusableInTouchMode(true);
        etLocalCode.requestFocus();
        etLocalCode.setSingleLine(true);
        etLocalCode.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        etLocalCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                executeLocalJoinRequest(etLocalCode.getText().toString());
                return true;
            }
            return false;
        });
        container.addView(etLocalCode);

        container.addView(createIconButton("LINK MANUALLY", android.R.drawable.ic_menu_edit, v -> {
            executeLocalJoinRequest(etLocalCode.getText().toString());
        }));

        container.addView(createIconButton("SCAN OFFLINE QR", android.R.drawable.ic_menu_search, v -> {
            isScanningLocal = true;
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan Parent's Offline Matrix");
            options.setBeepEnabled(true);
            options.setCameraId(0);
            barcodeLauncher.launch(options);
        }));

        container.addView(createSectionDivider());
        container.addView(createIconButton("BACK TO GATEWAY", android.R.drawable.ic_menu_revert, v -> resetGateway()));

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

        FirebasePairingManager.INSTANCE.startHostPairingSession(assignedRoles, (qrBitmap, code) -> {
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
        }, childDeviceId -> {
            // CRITICAL HANDSHAKE! Parent intercepted a child entering the code!
            runOnUiThread(() -> promptApprovalDialog(childDeviceId, "Family Member", false));
            return kotlin.Unit.INSTANCE;
        });
    }

    private void executeChildJoinRequest(String inputMatrix) {
        String cleaned = inputMatrix.trim().toUpperCase();
        if(cleaned.length() != 6) {
            Toast.makeText(this, "Invalid code length.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Injecting Handshake Identity...", Toast.LENGTH_SHORT).show();
        
        FirebasePairingManager.INSTANCE.requestJoinGroup(cleaned, (success, message) -> {
            runOnUiThread(() -> {
                Toast.makeText(this, message, success ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
                if (success) {
                    getWindow().getDecorView().postDelayed(this::finish, 1500); 
                }
            });
            return kotlin.Unit.INSTANCE;
        });
    }
    private void promptApprovalDialog(String childId, String labelName, boolean isLocalMerge) {
        ScrollView sv = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(40, 20, 40, 20);

        TextView l1 = new TextView(this);
        l1.setText("Assign Tracked Relationship:");
        l1.setTypeface(null, Typeface.BOLD);
        form.addView(l1);

        android.widget.CheckBox cbSpouse = new android.widget.CheckBox(this);
        cbSpouse.setText("Spouse");
        cbSpouse.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint()));
        
        android.widget.CheckBox cbChild = new android.widget.CheckBox(this);
        cbChild.setText("Child");
        cbChild.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint()));
        
        android.widget.CheckBox cbFriend = new android.widget.CheckBox(this);
        cbFriend.setText("Friend");
        cbFriend.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint()));
        
        android.widget.CheckBox[] groupBoxes = {cbSpouse, cbChild, cbFriend};
        
        for (android.widget.CheckBox cb : groupBoxes) {
            cb.setTextColor(getTextColor());
            cb.setPadding(30, 20, 30, 20);
            form.addView(cb);
        }

        sv.addView(form);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle(isLocalMerge ? "Local P2P Link Detected" : "Cloud Handshake Requested")
            .setMessage("Device Identity {" + childId + "} (" + labelName + ") has intercepted your Ping. \n\nSelect Relationship to Authorize:")
            .setView(sv)
            .setCancelable(false)
            .setPositiveButton("APPROVE 🟢", null) // Disabled implicitly until checked
            .setNegativeButton("REJECT ❌", (d, which) -> {
                String activeCode = FirebasePairingManager.INSTANCE.getActivePairingCode();
                if(activeCode != null && !isLocalMerge) {
                    FirebasePairingManager.INSTANCE.rejectHandshake(activeCode);
                }
                Toast.makeText(this, "Threat Terminated.", Toast.LENGTH_SHORT).show();
            })
            .show();
            
        android.widget.Button btnApprove = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        btnApprove.setEnabled(false); // Hard-lock the accept matrix
        
        for (android.widget.CheckBox cb : groupBoxes) {
            cb.setOnCheckedChangeListener((btnView, isChecked) -> {
                if (isChecked) {
                    for (android.widget.CheckBox other : groupBoxes) {
                        if (other != cb) other.setChecked(false); // Force Mutual Exclusivity!
                    }
                    btnApprove.setEnabled(true);
                } else {
                    boolean anyChecked = false;
                    for (android.widget.CheckBox other : groupBoxes) {
                        if (other.isChecked()) anyChecked = true;
                    }
                    btnApprove.setEnabled(anyChecked);
                }
            });
        }
        
        btnApprove.setOnClickListener(v -> {
            String rawSelection = "Child";
            for (android.widget.CheckBox cb : groupBoxes) {
                if (cb.isChecked()) rawSelection = cb.getText().toString();
            }
            
            // Parse Singular Button into Dual-Tier Offline Structure
            String famRole = rawSelection.equals("Friend") ? "Child" : rawSelection; // Friend devices must map gracefully
            String friendRole = rawSelection.equals("Friend") ? "Friend (Team)" : "None";
            
            // 🚀 PRIME DIRECTIVE COMPLIANT: Save to Local Database FIRST! (Data Preserved)
            com.jdroidx.jfindme.data.local.CitizenDbHelper localDb = new com.jdroidx.jfindme.data.local.CitizenDbHelper(this);
            localDb.updateCitizenRole(childId, famRole, friendRole);
            
            Toast.makeText(this, "Local Database Backup Secured. Merging Firebase Arrays...", Toast.LENGTH_SHORT).show();

            String myGrp = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getString("active_group", null);
            if (myGrp != null) {
                java.util.Map<String, Object> cloudPay = new java.util.HashMap<>();
                cloudPay.put("role_family", famRole);
                cloudPay.put("role_friend", friendRole);
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(myGrp).child("citizen_dossiers").child(childId).updateChildren(cloudPay);
            }

            if (isLocalMerge) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Local Identity Resolved! Locking Sync Array", Toast.LENGTH_SHORT).show();
                    getWindow().getDecorView().postDelayed(this::finish, 800);
                });
            } else {
                String activeCode = FirebasePairingManager.INSTANCE.getActivePairingCode();
                if (activeCode != null) {
                    FirebasePairingManager.INSTANCE.approveHandshake(activeCode, childId);
                    
                    dialog.dismiss(); // Cleanly close the initial intercept dialog
                    
                    // Spawn the exact requested popup for the HOST to reconstruct their physical hub
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("Connection Established")
                        .setMessage("Handshake Approved! Syncing Pipeline...\n\nPlease check the old device pairing. If it is gone then restore.")
                        .setCancelable(false)
                        .setPositiveButton("RESTORE", (d2, which) -> {
                            showRestoreLostRosterAssetsDialog();
                        })
                        .setNegativeButton("CLOSE", (d2, which) -> {
                            finish();
                        })
                        .show();
                    return; // Prevent standard dismiss execution since we hijacked it
                }
            }
            dialog.dismiss(); // Cleanly close the dialog!
        });
    }

    // 🎨 --- GUI UTILITIES ---

    private void showRestoreLostRosterAssetsDialog() {
        android.widget.Toast.makeText(this, "Scanning History Database for Legacy Connections...", android.widget.Toast.LENGTH_SHORT).show();
        
        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        String safeUid = (myUid != null) ? myUid : "U_MOCK";

        // Fetch user's current group first to know who is ALREADY paired!
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(safeUid).child("active_group").get().addOnSuccessListener(grpSnap -> {
            String myGrp = grpSnap.getValue(String.class);
            final String finalGrp = (myGrp == null) ? "" : myGrp;
            
            java.util.List<String> currentlyPairedUids = new java.util.ArrayList<>();

            Runnable executeAssetScan = () -> {
                com.jdroidx.jfindme.data.remote.FirebaseComm.INSTANCE.getLocationsRef().get().addOnSuccessListener(snapshot -> {
                    java.util.List<String> assetNames = new java.util.ArrayList<>();
                    java.util.List<String> assetUids = new java.util.ArrayList<>();
                    
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        com.jdroidx.jfindme.data.model.UserLocation loc = child.getValue(com.jdroidx.jfindme.data.model.UserLocation.class);
                        if (loc != null && !loc.getUserId().equals(safeUid)) {
                            // 🧠 QA Fix: Strict deduplication mapping
                            if (!assetUids.contains(loc.getUserId())) {
                                assetNames.add(loc.getUserName());
                                assetUids.add(loc.getUserId());
                            }
                        }
                    }
                    
                    if (assetNames.isEmpty()) {
                        android.widget.Toast.makeText(this, "No legacy assets detected in History Database.", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String[] items = assetNames.toArray(new String[0]);
                    boolean[] checkedItems = new boolean[items.length];
                    for (int i=0; i < items.length; i++) {
                        if (currentlyPairedUids.contains(assetUids.get(i))) checkedItems[i] = true;
                    }
                    
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("Select Assets to Recover")
                        .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                            checkedItems[which] = isChecked;
                        })
                        .setPositiveButton("RESTORE", (dialog, which) -> {
                            if (finalGrp.isEmpty()) {
                                android.widget.Toast.makeText(this, "You must create/host a group first before absorbing legacy targets.", android.widget.Toast.LENGTH_LONG).show();
                                return;
                            }
                            
                            int restoredCount = 0;
                            for (int i=0; i < checkedItems.length; i++) {
                                if (checkedItems[i] && !currentlyPairedUids.contains(assetUids.get(i))) {
                                    // Bind Missing Target
                                    String targetNode = assetUids.get(i);
                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(finalGrp).child("members").child(targetNode).setValue(true);
                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(targetNode).child("active_group").setValue(finalGrp);
                                    restoredCount++;
                                } else if (!checkedItems[i] && currentlyPairedUids.contains(assetUids.get(i))) {
                                    // 🧠 QA Fix: If they physically UNTICKED an active device, Unbind it!
                                    String targetNode = assetUids.get(i);
                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(finalGrp).child("members").child(targetNode).removeValue();
                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(targetNode).child("active_group").removeValue();
                                }
                            }
                            android.widget.Toast.makeText(this, "Successfully Synchronized Array with " + restoredCount + " new assets!", android.widget.Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("CLOSE", null)
                        .show();
                });
            };

            if (!finalGrp.isEmpty()) {
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(finalGrp).child("members").get().addOnSuccessListener(memSnap -> {
                    if (memSnap.exists()) {
                        for (com.google.firebase.database.DataSnapshot m : memSnap.getChildren()) {
                            currentlyPairedUids.add(m.getKey());
                        }
                    }
                    executeAssetScan.run();
                });
            } else {
                executeAssetScan.run();
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
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Native Theme Tint
        header.setTextSize(11);
        header.setPadding(40, 40, 40, 20); // Aligns precisely with card geometries natively!
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

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.i(TAG, "Unified Gateway Screen Successfully Executed.");
    }
}
