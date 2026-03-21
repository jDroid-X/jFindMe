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

        // 🚀 Premium Linear Gradient Hero Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(50, 80, 50, 60);
        applyLinearGradient(header, 0f); 
        
        TextView title = new TextView(this);
        title.setText("NETWORK GATEWAY");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Device Pairing & Active Rosters");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        header.addView(subtitle);
        
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
        if (prefs != null) {
            int baseId = prefs.getInt("equipped_avatar_id", 1);
            int dynamicImgIndex = (baseId % 22 == 0) ? 22 : (baseId % 22);
            int resId = getResources().getIdentifier("avatar_custom_" + dynamicImgIndex, "drawable", getPackageName());
            if (resId != 0) myAvatar.setImageResource(resId);
        } else {
            myAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
        
        // Emulate Transparent Avatar Circle with Neon Ring
        android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
        c.setShape(android.graphics.drawable.GradientDrawable.OVAL);
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
                            
                            for (com.google.firebase.database.DataSnapshot groupMember : snapshot.getChildren()) {
                                String memberId = groupMember.getKey();
                                if (memberId == null || memberId.equals(safeUid)) continue; // Skip self in the branch
                                
                                // Insert the member as an expandable leaf node connected to YOU!
                                LinearLayout childNode = buildTreeNode(memberId, "Syncing Profile...", false, false, null);
                                branchList.addView(childNode);
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
        // Stripped dark background! Node inherently floats on the clean white Roster Card!
        
        LinearLayout.LayoutParams nodeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nodeLp.setMargins(isRoot ? 0 : 80, 0, 0, 0); // Indent the children!
        node.setLayoutParams(nodeLp);
        node.setPadding(30, 30, 30, 30);
        
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        node.addView(avatar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        LinearLayout textData = new LinearLayout(this);
        textData.setOrientation(LinearLayout.VERTICAL);
        textData.setPadding(40, 0, 0, 0);
        
        TextView nameView = new TextView(this);
        nameView.setText(defaultName);
        nameView.setTextColor(isRoot ? getGradientStart() : getTextColor()); // Flawless DB Thematic Injection!
        nameView.setTypeface(null, Typeface.BOLD);
        textData.addView(nameView);
        
        TextView idView = new TextView(this);
        idView.setText(isRoot ? "TAP TO EXPAND/COLLAPSE GROUP ▲▼" : ("Network Identifier: " + uid));
        idView.setTextColor(getSecondaryTextColor());
        idView.setTextSize(10);
        textData.addView(idView);
        node.addView(textData);
        
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
                        if (isRoot) finalNameRaw = "YOU (" + finalNameRaw + ")";
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

                        // Burn the Map Pin configuration natively into the Device Tracking Item
                        android.content.SharedPreferences prefs = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE);
                        boolean hideMode = prefs.getBoolean("callout_hide_mode", false);
                        long revealedAt = prefs.getLong("callout_revealed_at", 0);
                        boolean showName = !hideMode || ((System.currentTimeMillis() - revealedAt) < 5000);
                        
                        android.graphics.Bitmap pinGraphic = com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generateRawBitmap(GroupActivity.this, finalNameRaw, dynamicBmp, showName, isRoot);
                        avatar.setImageBitmap(pinGraphic);
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
            executeHostSessionSpinup();
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

    private void executeHostSessionSpinup() {
        tvCode.setText("NEGOTIATING...");
        tvSyncStatus.setText("ALLOCATING CLOUD NODE...");
        tvSyncStatus.setTextColor(Color.YELLOW);
        ivQrCode.setVisibility(View.GONE);

        FirebasePairingManager.INSTANCE.startHostPairingSession((qrBitmap, code) -> {
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
                    getWindow().getDecorView().postDelayed(this::finish, 1500); // 🚀 Transition back to LIVE MAP automatically!
                }
            });
            return kotlin.Unit.INSTANCE;
        });
    }

    private void promptApprovalDialog(String childId, String labelName, boolean isLocalMerge) {
        new AlertDialog.Builder(this)
            .setTitle(isLocalMerge ? "Local P2P Link Detected" : "Cloud Handshake Requested")
            .setMessage("Device Identity {" + childId + "} (" + labelName + ") has intercepted your Ping and is requesting full Map Sync capabilities. \n\nAuthorize Link?")
            .setCancelable(false)
            .setPositiveButton("APPROVE 🟢", (dialog, which) -> {
                Toast.makeText(this, "Merging Unified Cloud Arrays...", Toast.LENGTH_SHORT).show();
                
                if (isLocalMerge) {
                    // QA Native Proximity Link: The Host Node handles the Firebase Sync natively!
                    // Actively closing the Gateway confirms the local P2P route!
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Local Identity Resolved! Locking Sync Array", Toast.LENGTH_SHORT).show();
                        getWindow().getDecorView().postDelayed(this::finish, 800);
                    });
                } else {
                    String activeCode = FirebasePairingManager.INSTANCE.getActivePairingCode();
                    if(activeCode != null) {
                        FirebasePairingManager.INSTANCE.approveHandshake(activeCode, childId);
                        finish(); // Force close - return to Native Android Map render loop
                    }
                }
            })
            .setNegativeButton("REJECT ❌", (dialog, which) -> {
                String activeCode = FirebasePairingManager.INSTANCE.getActivePairingCode();
                if(activeCode != null && !isLocalMerge) {
                    FirebasePairingManager.INSTANCE.rejectHandshake(activeCode);
                }
                Toast.makeText(this, "Threat Terminated.", Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    // 🎨 --- GUI UTILITIES ---

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
