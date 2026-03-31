package com.jdroidx.jfindme.ui.group;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;

/**
 * 📝 Phase 5 Profile Details System
 * Captures strict Profile-Form logic mapping physical traits, medical requirements, 
 * and emergency protocols directly into the Family's root Firebase Group payload.
 */
public class PersonDetailsActivity extends BaseActivityJava {

    private String targetUid;
    private String targetRoleTitle;
    private android.content.SharedPreferences prefs;
    private PersonDetailsViewModel viewModel;
    
    // UI Elements
    private EditText etFullName, etPhone, etDob, etAddress, etEmergencyNote;
    private AutoCompleteTextView etGender, etCountry, etState, etCity;
    private EditText etPincode;
    private EditText etSafeZoneName, etSafeZoneRadius;
    private android.widget.CheckBox cbAclLiveTracking, cbAclHistoryAudit, cbAclGeofence, cbAclAdminStatus, cbAclGhostOverride;
    private android.widget.CheckBox cbAclBiometrics;
    private android.widget.CheckBox cbGlobalCloudSync;
    private boolean isSelf = false;
    private android.widget.LinearLayout dynamicTaggingContainer;
    private android.widget.LinearLayout formBox;

    @Override
    protected View createLayout() {
        targetUid = getIntent().getStringExtra("TARGET_UID");
        targetRoleTitle = getIntent().getStringExtra("TARGET_ROLE");
        prefs = getSharedPreferences("system_registry", MODE_PRIVATE);

        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setBackgroundColor(getPrimaryColor());

        // 1. HERO BANNER
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header); // 🚀 Native QA Architecture Injection
        TextView title = new TextView(this);
        title.setText("PROFILE DETAILS");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Target Profile: " + (targetRoleTitle != null ? targetRoleTitle : "UNKNOWN"));
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        header.addView(subtitle);
        
        mainContainer.addView(header);
        
        // --- 🚀 PILLAR 1: NETWORK GATEWAY (OWNER TILE) ---
        LinearLayout ownerTile = new LinearLayout(this);
        ownerTile.setOrientation(LinearLayout.HORIZONTAL);
        ownerTile.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int sideMargin = getResponsivePaddingQuota();
        ownerTile.setPadding(sideMargin, sideMargin, sideMargin, sideMargin);
        
        // 🚀 Premium Gateway Avatar geometry
        android.widget.FrameLayout avatarFrame = new android.widget.FrameLayout(this);
        UiArchitect.applyCard(this, avatarFrame, "CARD_STANDARD");
        
        android.widget.ImageView gatewayIcon = new android.widget.ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, gatewayIcon);
        gatewayIcon.setPadding(10, 10, 10, 10); 
        int iSize = getResponsiveIconQuota();
        avatarFrame.addView(gatewayIcon, new android.widget.FrameLayout.LayoutParams(iSize, iSize));
        
        // --- 🚀 AVATAR CLICK: Open Profile ---
        avatarFrame.setFocusable(true);
        avatarFrame.setClickable(true);
        avatarFrame.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
        });
        
        ownerTile.addView(avatarFrame, new LinearLayout.LayoutParams(iSize, iSize));
        
        LinearLayout metaText = new LinearLayout(this);
        metaText.setOrientation(LinearLayout.VERTICAL);
        metaText.setPadding(getResponsivePaddingQuota(), 0, 0, 0);
        
        TextView gatewayTitle = new TextView(this);
        String nameStr = prefs.getString("user_nickname", "User Identity");
        gatewayTitle.setText("👤 Owner: " + nameStr);
        gatewayTitle.setTextColor(getTextColor());
        UiArchitect.applyText(this, gatewayTitle, "TEXT_LABEL");
        metaText.addView(gatewayTitle);
        
        TextView gatewayName = new TextView(this);
        String gName = prefs.getString("user_group_nickname", "SECURE_HUB_X01");
        String gId = prefs.getString("active_group", "UNLINKED");
        gatewayName.setText(gName + " [" + gId + "]");
        gatewayName.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, gatewayName, "TEXT_SUB");
        metaText.addView(gatewayName);
        
        ownerTile.addView(metaText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
 
        // --- 🚀 PLAY BUTTON
        android.widget.ImageView playBtn = new android.widget.ImageView(this);
        playBtn.setImageResource(android.R.drawable.ic_media_play);
        playBtn.setColorFilter(getGradientStart());
        playBtn.setPadding(10, 10, 10, 10);
        int playSize = (int)(iSize * 0.7f);
        ownerTile.addView(playBtn, new LinearLayout.LayoutParams(playSize, playSize));
        
        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, ownerTile, "CARD_STANDARD");
        
        ownerTile.setFocusable(true);
        ownerTile.setClickable(true);
        ownerTile.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class));
        });
        
        // 🚀 QA Fix: Global Key Interceptor (Accessibility)
        ownerTile.setOnKeyListener((v, k, e) -> {
            if (e.getAction() == android.view.KeyEvent.ACTION_UP && (k == android.view.KeyEvent.KEYCODE_ENTER || k == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick(); return true;
            } return false;
        });
 
        LinearLayout.LayoutParams ownerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int topGap = getResponsiveHeaderTopQuota();
        ownerLp.setMargins(sideMargin, topGap, sideMargin, sideMargin);
        mainContainer.addView(ownerTile, ownerLp);

        // 2. SCROLLING CITIZEN FORM FRAMEWORK
        ScrollView scrollEngine = new ScrollView(this);
        formBox = new LinearLayout(this);
        formBox.setOrientation(LinearLayout.VERTICAL);
        int formPad = getResponsivePaddingQuota();
        formBox.setPadding(formPad, getResponsiveHeaderTopQuota(), formPad, formPad * 4);
        
        isSelf = targetUid != null && targetUid.equals(com.google.firebase.auth.FirebaseAuth.getInstance().getUid());

        formBox.addView(createSectionHeader("PERSONAL IDENTITY"));
        etFullName = createFormField("Full Name", "Legal Identity", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_myplaces, true);
        etPhone = createFormField("Contact Phone", "Enterprise Link", InputType.TYPE_CLASS_PHONE, android.R.drawable.ic_menu_call, true);
        etDob = createFormField("Date of Birth", "DD/MM/YYYY Protocol", InputType.TYPE_CLASS_DATETIME, android.R.drawable.ic_menu_today, true);
        
        // 🚀 QA Fix: Master Dropdown Matrix (Adaptive Waterfall MVVM)
        etGender = createDropdownField("Biological Protocol", "Select Gender Identity", android.R.drawable.ic_menu_info_details, true);
        etGender.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new String[]{"Male", "Female", "Other", "Prefer Not to Say"}));
        
        etCountry = createDropdownField("Matrix Country", "Locate Territory", android.R.drawable.ic_menu_mapmode, true);
        etState = createDropdownField("Zone State", "Locate Sub-Territory", android.R.drawable.ic_menu_directions, true);
        etCity = createDropdownField("Node City", "Locate Logical Zone", android.R.drawable.ic_menu_myplaces, true);
        
        etPincode = createFormField("Postal Matrix", "Zip / Pin Code", InputType.TYPE_CLASS_NUMBER, android.R.drawable.ic_menu_compass, true);

        formBox.addView(createSectionHeader("EMERGENCY PROTOCOL"));
        etAddress = createFormField("Secure Residence", "Physical Point of Interest", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_mapmode, false);
        etEmergencyNote = createFormField("Siren Protocol", "Emergency Medical Notes", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_dialog_info, false);

        formBox.addView(createSectionHeader("SAFE ZONE (GEOFENCE) RELATIONS"));
        etSafeZoneName = createFormField("Zone Label", "e.g. Master School", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_mylocation, false);
        etSafeZoneRadius = createFormField("Siren Radius", "Meters from Anchor", InputType.TYPE_CLASS_NUMBER, android.R.drawable.ic_menu_compass, false);
        formBox.addView(createSectionHeader("ENTERPRISE ACCESS CONTROL (ACL)"));
        
        android.widget.GridLayout aclGrid = new android.widget.GridLayout(this);
        aclGrid.setColumnCount(1); // 🚀 UX FIX: Single Column for detailed descriptions
        aclGrid.setPadding(0, 10, 0, 10);
        
        cbAclLiveTracking = createPermissionCheckbox("Live Tracking Protocol", "Authorize real-time synchronization of GPS and sensor data with the tracking engine.");
        cbAclHistoryAudit = createPermissionCheckbox("History Audit Extraction", "Permit the extraction of historical location logs and event chronologies for audit.");
        cbAclGeofence = createPermissionCheckbox("Boundary Breach Alerts", "Authorize automated broadcast alerts when geofence radii are breached.");
        cbAclAdminStatus = createPermissionCheckbox("Administrative Overrides", "Grant global administrative permissions to modify profile settings remotely.");
        cbAclGhostOverride = createPermissionCheckbox("Stealth Oversite Logic", "Allow the system to override 'Ghost Mode' when an emergency protocol is triggered.");
        
        cbAclBiometrics = createPermissionCheckbox("Biometric Identity Lock", "Securely store and verify fingerprint, face, or iris biometric signatures for identity handshake.");
        
        formBox.addView(cbAclLiveTracking);
        formBox.addView(cbAclHistoryAudit);
        formBox.addView(cbAclGeofence);
        formBox.addView(cbAclAdminStatus);
        formBox.addView(cbAclGhostOverride);
        formBox.addView(cbAclBiometrics);

        // 🚀 PHASE 8 GLOBAL PRIVACY DIRECTIVE: GDPR Data Minimization
        formBox.addView(createSectionHeader("GLOBAL CLOUD PRIVACY"));
        cbGlobalCloudSync = createPermissionCheckbox("Synchronize Profile to Cloud Hub", "If disabled, heavily sensitive profiles (Biometrics, Medical, Insurance) stay offline locally on this hardware.");
        cbGlobalCloudSync.setChecked(true); // Enterprise Default: Sync all data
        formBox.addView(cbGlobalCloudSync);

        if (!isSelf) {
            // Strict Read-Only Lockdown execution!
            etFullName.setEnabled(false);
            etDob.setEnabled(false);
            etGender.setEnabled(false);
            etAddress.setEnabled(false);
            etCountry.setEnabled(false);
            etState.setEnabled(false);
            etCity.setEnabled(false);
            etPincode.setEnabled(false);
            etEmergencyNote.setEnabled(false);
            etSafeZoneName.setEnabled(false);
            etSafeZoneRadius.setEnabled(false);
            
            cbAclLiveTracking.setEnabled(false);
            cbAclHistoryAudit.setEnabled(false);
            cbAclGeofence.setEnabled(false);
            cbAclAdminStatus.setEnabled(false);
            cbAclGhostOverride.setEnabled(false);
            cbAclBiometrics.setEnabled(false);
            cbGlobalCloudSync.setEnabled(false);
            
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnLp.setMargins(0, 0, 0, formPad);
            View syncBtn = createIconButton("REQUEST DEVICE SYNC", android.R.drawable.ic_popup_sync, v -> {
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference()
                    .child("users").child(targetUid).child("force_cloud_sync_trigger")
                    .setValue(System.currentTimeMillis());
                android.widget.Toast.makeText(this, "Sync Pull Request sent to Target Device network edge...", android.widget.Toast.LENGTH_LONG).show();
            });
            formBox.addView(syncBtn, btnLp);
        } else {
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnLp.setMargins(0, 0, 0, formPad);
            View saveDossierBtn = createIconButton("Save Profile", android.R.drawable.ic_menu_save, true, v -> saveCitizenData());
            formBox.addView(saveDossierBtn, btnLp);
            
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(targetUid).child("force_cloud_sync_trigger")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            saveCitizenData();
                            android.widget.Toast.makeText(PersonDetailsActivity.this, "Remote Group Administrator forced Profile Data sync...", android.widget.Toast.LENGTH_SHORT).show();
                            com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(targetUid).child("force_cloud_sync_trigger").removeValue();
                        }
                    }
                    @Override public void onCancelled(com.google.firebase.database.DatabaseError e) {}
                });
        }
        
        LinearLayout.LayoutParams stdLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stdLp.setMargins(0, 0, 0, formPad);

        View cloudInspectBtn = createIconButton("Cloud Data Portal", android.R.drawable.ic_menu_upload, v -> showCloudPrivacyPayload());
        formBox.addView(cloudInspectBtn, stdLp);
        
        formBox.addView(createIconButton("VIEW ARCHIVED CHANGE LOG", android.R.drawable.ic_menu_recent_history, v -> {
            viewModel.fetchChangeLog(prefs.getString("active_group", null), targetUid);
        }), stdLp);
        
        formBox.addView(createIconButton("RETURN TO ROSTER", android.R.drawable.ic_menu_revert, v -> finish()), stdLp);

        scrollEngine.addView(formBox);
        mainContainer.addView(scrollEngine, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainContainer;
    }

    private void rebuildTaggingUI() {
        if (dynamicTaggingContainer == null) return;
        dynamicTaggingContainer.removeAllViews();
        if (!isSelf) return; // Only allow tagging locally on your own profile
        
        com.jdroidx.jfindme.data.local.CitizenDbHelper localDb = new com.jdroidx.jfindme.data.local.CitizenDbHelper(this);
        java.util.List<java.util.Map<String, Object>> roster = localDb.getOfflineRoster();
        
        java.util.Map<String, String> pairedSpouses = new java.util.HashMap<>();
        java.util.Map<String, String> pairedChildren = new java.util.HashMap<>();
        java.util.Map<String, String> pairedFriends = new java.util.HashMap<>();
        java.util.Map<String, String> pairedParents = new java.util.HashMap<>();
        
        for (java.util.Map<String, Object> node : roster) {
            String rTitle = (String) node.get("role_title");
            String rName = (String) node.get("display_name");
            String rUid = (String) node.get("uid");
            if (rTitle == null || rName == null || rUid == null) continue;
            
            if (rTitle.contains("SPOUSE")) pairedSpouses.put(rUid, rName);
            else if (rTitle.contains("CHILD")) pairedChildren.put(rUid, rName);
            else if (rTitle.contains("FRIEND")) pairedFriends.put(rUid, rName);
            else if (rTitle.contains("FATHER") || rTitle.contains("MOTHER")) pairedParents.put(rUid, rTitle + " (" + rName + ")");
        }

        // QA Fix: Directly pull the Active Target's localized identity (Not the Admin's Global Name)
        String myName = etFullName != null && etFullName.getText().toString().trim().length() > 0 
                ? etFullName.getText().toString().trim() 
                : "Target";

        // 🚀 PRIME DIRECTIVE: Universal Single Source of Truth
        dynamicTaggingContainer.addView(createSectionHeader("UNIVERSAL DEVICE NETWORK"));
        dynamicTaggingContainer.addView(createTaggedTreeItem("GUARDIAN", pairedParents));
        dynamicTaggingContainer.addView(createTaggedTreeItem("SPOUSE", pairedSpouses));
        dynamicTaggingContainer.addView(createTaggedTreeItem("CHILD", pairedChildren));
        dynamicTaggingContainer.addView(createTaggedTreeItem("FRIEND", pairedFriends));
    }

    private android.view.View createTaggedTreeItem(String roleName, java.util.Map<String, String> assignedNodes) {
        android.widget.LinearLayout categoryGroup = new android.widget.LinearLayout(this);
        categoryGroup.setOrientation(android.widget.LinearLayout.VERTICAL);

        // 1. The Head Checkbox (Ticked if mapped, Unticked if empty)
        android.widget.CheckBox cb = new android.widget.CheckBox(this);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint())); // 🚀 Theme Fix Matrix
        cb.setText("Tag " + roleName + " Device");
        cb.setTextColor(getTextColor());
        cb.setPadding(getResponsivePaddingQuota(), getResponsivePaddingQuota(), getResponsivePaddingQuota(), getResponsivePaddingQuota());
        
        if (!assignedNodes.isEmpty()) {
            cb.setChecked(true);
            cb.setText(roleName + " Network (" + assignedNodes.size() + " Linked)");
        }
        
        cb.setOnClickListener(v -> {
            boolean active = ((android.widget.CheckBox)v).isChecked();
            if (active) {
                // If checking manually, pop Scanner!
                ((android.widget.CheckBox)v).setChecked(false); // Visual reset before opening scanner
                final String intentRole = roleName.toUpperCase().contains("FRIEND") ? "FRIEND" : (roleName.toUpperCase().contains("CHILD") ? "CHILD" : "SPOUSE");
                
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Initialize " + roleName + " Scope")
                    .setMessage("Entering physical pairing loop. The QR Scanner will now open specifically locking targets as a [" + intentRole + "].")
                    .setPositiveButton("LAUNCH SCANNER", (d, w) -> {
                        android.content.Intent i = new android.content.Intent(this, com.jdroidx.jfindme.ui.group.GroupActivity.class);
                        i.putExtra("requested_network_role", intentRole);
                        startActivity(i);
                    })
                    .setNegativeButton("SAVE LATER", null)
                    .show();
            } else {
                // Clicking an already checked box cancels it visually, instruct user to use indent targets
                ((android.widget.CheckBox)v).setChecked(true);
                android.widget.Toast.makeText(this, "Tap the indented avatars below to Reassign or Unassign directly.", android.widget.Toast.LENGTH_LONG).show();
            }
        });
        categoryGroup.addView(cb);

        // 2. The File Explorer Branches
        for (String nodeUid : assignedNodes.keySet()) {
            String childName = assignedNodes.get(nodeUid);
            TextView branch = new TextView(this);
            branch.setText("   └── 👤 " + childName + " (Tap Edit)");
            branch.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Green locked
            branch.setTextSize(14);
            branch.setTypeface(null, android.graphics.Typeface.BOLD);
            branch.setPadding(90, 5, 0, 15); // Explicit physical tree indent
            
            // Interactive reassignment attached directly to the hierarchical branch
            branch.setFocusable(true);
            branch.setClickable(true);
            branch.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_UP && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                    branch.performClick(); return true;
                } return false;
            });
            branch.setOnClickListener(v -> {
                String[] roles = {"Spouse", "Child", "Friend", "Unassign Role"};
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Reassign: " + childName)
                    .setItems(roles, (dialog, which) -> {
                        String selected = roles[which];
                        String famRole = "None";
                        String frRole = "None";
                        if (selected.equals("Spouse")) { famRole = "Spouse"; frRole = "None"; }
                        else if (selected.equals("Child")) { famRole = "Child"; frRole = "None"; }
                        else if (selected.equals("Friend")) { famRole = "Child"; frRole = "Friend (Team)"; } 
                        
                        com.jdroidx.jfindme.data.local.CitizenDbHelper localDb = new com.jdroidx.jfindme.data.local.CitizenDbHelper(this);
                        localDb.updateCitizenRole(nodeUid, famRole, frRole, frRole.contains("Friend"));
                        
                        String myGrp = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);
                        if (myGrp != null) {
                            java.util.Map<String, Object> cp = new java.util.HashMap<>();
                            cp.put("role_family", famRole);
                            cp.put("role_friend", frRole);
                            com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("groups").child(myGrp).child("citizen_dossiers").child(nodeUid).updateChildren(cp);
                        }
                        android.widget.Toast.makeText(this, "Network Array Re-Mapped! Hit Save.", android.widget.Toast.LENGTH_SHORT).show();
                        rebuildTaggingUI(); 
                    }).show();
            });
            categoryGroup.addView(branch);
        }
        
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 5, 0, 15);
        categoryGroup.setLayoutParams(lp);
        
        return categoryGroup;
    }

    private android.widget.CheckBox createTaggingCheckbox(String label, String targetRole) {
        android.widget.CheckBox cb = new android.widget.CheckBox(this);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint())); // 🚀 Theme Fix Matrix
        cb.setText(label);
        cb.setTextColor(getTextColor());
        cb.setPadding(20, 20, 20, 20);
        cb.setFocusable(true);
        cb.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                cb.performClick(); return true;
            } return false;
        });
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Device Pairing Sequence")
                    .setMessage("This will launch the QR Handshake Scanner to tag a " + targetRole + " device to the network. Proceed?")
                    .setPositiveButton("LAUNCH SCANNER", (d, w) -> {
                        android.content.Intent intent = new android.content.Intent(this, GroupActivity.class);
                        intent.putExtra("requested_network_role", targetRole);
                        startActivity(intent);
                        buttonView.setChecked(false); // Resets tick to allow multiple loops
                    })
                    .setNegativeButton("SAVE LATER", (d, w) -> buttonView.setChecked(false))
                    .show();
            }
        });
        return cb;
    }

    private android.widget.CheckBox createPermissionCheckbox(String ruleTitle, String description) {
        android.widget.CheckBox cb = new android.widget.CheckBox(this);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint())); // 🚀 Theme Fix Matrix
        cb.setText(ruleTitle + "\n" + description);
        cb.setTextColor(getTextColor());
        cb.setPadding(getResponsivePaddingQuota(), getResponsivePaddingQuota() + 10, getResponsivePaddingQuota(), getResponsivePaddingQuota() + 10);
        cb.setChecked(false);
        cb.setEnabled(isSelf); // Dynamic UX lock: Only admins or the individual themselves can edit permissions natively!
        
        cb.setFocusable(true);
        cb.setClickable(true);
        cb.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                cb.performClick(); return true;
            } return false;
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 15);
        cb.setLayoutParams(lp);
        return cb;
    }

    private void showCloudPrivacyPayload() {
        String msg;
        if (cbGlobalCloudSync.isChecked()) {
            msg = "☁️ FULL SYNC AUTHORIZED:\n\n" +
                  "The application will encrypt and securely push the entire Profile Details, including Biometrics, Medical Records, Insurance Data, and Identification Marks to the tracking Cloud.";
        } else {
            msg = "🔒 DATA MINIMIZATION ACTIVE:\n\n" +
                  "Absolute privacy enforced! Biometrics, Medical protocols, and Home Addresses will be STRIPPED before touching the Cloud. \n\n" +
                  "Only basic tracking elements (Name Token, Current GPS Coordinates, Role Status, and Geofence Radii) are permitted to transmit externally.";
        }
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("Cloud Privacy Inspector")
            .setMessage(msg)
            .setPositiveButton("Understood", null)
            .show();
    }

    private View createSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getGradientStart());
        tv.setTextSize(14); // 🚀 QA Architecture Sync: Increased for high-contrast visibility
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(10, getResponsivePaddingQuota(), 0, getResponsivePaddingQuota() / 2);
        return tv;
    }

    private View wrapInAclGrid(View v) {
        android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
        lp.width = 0;
        lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
        lp.setMargins(10, 10, 10, 10);
        v.setLayoutParams(lp);
        return v;
    }

    private EditText createFormField(String label, String hint, int inputType, int iconRes, boolean isMandatory) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 0, 0, 25);

        // 🚀 PILLAR 11: Label-and-Box Format
        TextView tvLabel = new TextView(this);
        String labelHtml = isMandatory ? "<b>" + label + " <font color='#FF1744'>(*)</font></b>" : "<b>" + label + "</b>";
        tvLabel.setText(android.text.Html.fromHtml(labelHtml));
        tvLabel.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvLabel, "TEXT_LABEL");
        tvLabel.setPadding(10, 0, 0, 15);
        wrapper.addView(tvLabel);

        EditText et = new EditText(this);
        String hintHtml = isMandatory ? hint + " <font color='#FF1744'>(*)</font>" : hint;
        et.setHint(android.text.Html.fromHtml(hintHtml));
        et.setHintTextColor(getSecondaryTextColor());
        et.setTextColor(getTextColor());
        int boxPad = getResponsivePaddingQuota() + 10;
        et.setPadding(boxPad, boxPad, boxPad, boxPad);
        et.setTextSize(16);
        et.setInputType(inputType);
        et.setSingleLine(true);
        et.setEnabled(isSelf);
        
        if (inputType == InputType.TYPE_CLASS_NUMBER) {
            et.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
            et.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    saveCitizenData();
                    return true;
                }
                return false;
            });
        } else {
            et.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        }

        android.graphics.drawable.Drawable icon = getResources().getDrawable(iconRes, null);
        icon.setTint(getGradientStart());
        et.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        et.setCompoundDrawablePadding(30);
        
        applySolidCardGeometry(et, getCardColor(), 30f); // 🚀 L7 Standard: 30f radius
        wrapper.addView(et);
        
        formBox.addView(wrapper);
        return et;
    }

    @Override
    protected void setupLogic() {
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(PersonDetailsViewModel.class);
        
        if (targetUid == null) {
            Toast.makeText(this, "Awaiting Target Matrix. Internal Storage Blocked.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🚀 LIVE OBSERVATION PROTOCOL: UI reacts natively to Data Source (Offline SQL & Cloud)
        viewModel.getDossierLiveData().observe(this, data -> {
            if (data == null) return;
            etFullName.setText(data.get("fullName"));
            etPhone.setText(data.get("phone"));
            etDob.setText(data.get("dob"));
            etGender.setText(data.get("gender"));
            etAddress.setText(data.get("address"));
            etCountry.setText(data.get("country"));
            etState.setText(data.get("state"));
            etCity.setText(data.get("city"));
            etPincode.setText(data.get("pincode"));
            etEmergencyNote.setText(data.get("emergencyNote"));
            etSafeZoneName.setText(data.get("safeZoneName"));
            etSafeZoneRadius.setText(data.get("safeZoneRadius"));
            
            // ACL MVVM Matrix Restore
            cbAclLiveTracking.setChecked("GRANTED".equals(data.get("acl_live_tracking")));
            cbAclHistoryAudit.setChecked("GRANTED".equals(data.get("acl_history_audit")));
            cbAclGeofence.setChecked("GRANTED".equals(data.get("acl_geofence")));
            cbAclAdminStatus.setChecked("GRANTED".equals(data.get("acl_admin")));
            cbAclGhostOverride.setChecked("GRANTED".equals(data.get("acl_ghost_override")));
            
            cbAclBiometrics.setChecked("GRANTED".equals(data.get("acl_biometrics")));
            
            cbGlobalCloudSync.setChecked(!"FALSE".equals(data.get("cloud_sync_enabled"))); // Defaults to TRUE if missing
            
            // 🚀 Trigger Country fetch after data Load to initialize dropdowns if needed
            viewModel.fetchCountriesList();
        });

        // 🚀 DYNAMIC GEOGRAPHY SUITE (Mirroring Profile Engine)
        viewModel.getCountriesList().observe(this, list -> {
            etCountry.setAdapter(createEnterpriseDropdownAdapter(new java.util.ArrayList<>(list)));
        });

        viewModel.getStatesList().observe(this, list -> {
            etState.setAdapter(createEnterpriseDropdownAdapter(new java.util.ArrayList<>(list)));
        });

        viewModel.getCitiesList().observe(this, list -> {
            etCity.setAdapter(createEnterpriseDropdownAdapter(new java.util.ArrayList<>(list)));
        });

        // Cascading Dropdown Logic
        etCountry.setOnItemClickListener((parent, view, position, id) -> {
            String country = parent.getItemAtPosition(position).toString();
            if (!country.equals("Select Country")) {
                etState.setText("");
                etCity.setText("");
                viewModel.fetchStatesList(country);
            }
        });

        etState.setOnItemClickListener((parent, view, position, id) -> {
            String state = parent.getItemAtPosition(position).toString();
            if (!state.equals("Select State")) {
                etCity.setText("");
                viewModel.fetchCitiesList(etCountry.getText().toString(), state);
            }
        });

        // 🚀 Triggers the DB fetch entirely off the Main UI Thread architecture!
        viewModel.fetchDossier(this, prefs.getString("active_group", null), targetUid);
        
        viewModel.getChangeLogLiveData().observe(this, logs -> {
            if (logs == null || logs.isEmpty()) {
                Toast.makeText(this, "No audit logs found for this Target.", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (String l : logs) {
                sb.append(l).append("\n\n");
            }
            new android.app.AlertDialog.Builder(this)
                .setTitle("Dossier Change History")
                .setMessage(sb.toString())
                .setPositiveButton("Close Archive", null)
                .show();
        });

        viewModel.getSaveStatus().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Local Offline Cache & Cloud Sync Verified!", Toast.LENGTH_LONG).show();
                finish(); // Graceful architectural teardown back to Roster Map
            } else if (Boolean.FALSE.equals(success)) {
                Toast.makeText(this, "Save Failed. Review Network Stack.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private AutoCompleteTextView createDropdownField(String label, String hint, int iconRes, boolean isMandatory) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 0, 0, 25);

        TextView tvLabel = new TextView(this);
        String labelHtml = isMandatory ? "<b>" + label + " <font color='#FF1744'>(*)</font></b>" : "<b>" + label + "</b>";
        tvLabel.setText(android.text.Html.fromHtml(labelHtml));
        tvLabel.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvLabel, "TEXT_LABEL");
        tvLabel.setPadding(10, 0, 0, 15);
        wrapper.addView(tvLabel);

        AutoCompleteTextView actv = new AutoCompleteTextView(this);
        String hintHtml = isMandatory ? hint + " <font color='#FF1744'>(*)</font>" : hint;
        actv.setHint(android.text.Html.fromHtml(hintHtml));
        actv.setHintTextColor(getSecondaryTextColor());
        actv.setTextColor(getTextColor());
        actv.setTextSize(16);
        actv.setFocusable(false);
        actv.setClickable(true);
        actv.setOnClickListener(v -> actv.showDropDown());
        actv.setDropDownBackgroundDrawable(createPopupBackground());
        
        applySolidCardGeometry(actv, getCardColor(), 30f);
        int ddPad = getResponsivePaddingQuota() + 10;
        actv.setPadding(ddPad, ddPad, ddPad, ddPad);
        android.graphics.drawable.Drawable icon = getResources().getDrawable(iconRes, null);
        icon.setTint(getGradientStart());
        actv.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        actv.setCompoundDrawablePadding(getResponsivePaddingQuota());
        
        actv.setEnabled(isSelf);
        wrapper.addView(actv);
        
        formBox.addView(wrapper);
        return actv;
    }

    private void saveCitizenData() {
        if (targetUid == null) {
            Toast.makeText(this, "Cannot synchronize Ghost target.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("fullName", etFullName.getText().toString());
        payload.put("phone", etPhone.getText().toString());
        payload.put("dob", etDob.getText().toString());
        payload.put("gender", etGender.getText().toString());
        payload.put("address", etAddress.getText().toString());
        payload.put("country", etCountry.getText().toString());
        payload.put("state", etState.getText().toString());
        payload.put("city", etCity.getText().toString());
        payload.put("pincode", etPincode.getText().toString());
        payload.put("emergencyNote", etEmergencyNote.getText().toString());
        payload.put("safeZoneName", etSafeZoneName.getText().toString());
        payload.put("safeZoneRadius", etSafeZoneRadius.getText().toString());
        
        // ACL Payload Bundling
        payload.put("acl_live_tracking", cbAclLiveTracking.isChecked() ? "GRANTED" : "DENIED");
        payload.put("acl_history_audit", cbAclHistoryAudit.isChecked() ? "GRANTED" : "DENIED");
        payload.put("acl_geofence", cbAclGeofence.isChecked() ? "GRANTED" : "DENIED");
        payload.put("acl_admin", cbAclAdminStatus.isChecked() ? "GRANTED" : "DENIED");
        payload.put("acl_ghost_override", cbAclGhostOverride.isChecked() ? "GRANTED" : "DENIED");
        payload.put("acl_biometrics", cbAclBiometrics.isChecked() ? "GRANTED" : "DENIED");
        
        // Core Cloud Privacy Setting Extraction
        payload.put("cloud_sync_enabled", cbGlobalCloudSync.isChecked() ? "TRUE" : "FALSE");
        
        payload.put("role_family", "None"); 
        payload.put("role_friend", "None");
        
        // 🚀 MVVM Architecture: Route payload down into exactly one Repository ViewModel layer!
        if (isSelf) {
            // Instantly sync the local profile cache so ProfileActivity remains synchronized
            prefs.edit().putString("user_nickname", etFullName.getText().toString())
                .putString("user_phone", etPhone.getText().toString())
                .putString("user_dob", etDob.getText().toString())
                .putString("user_gender", etGender.getText().toString())
                .putString("user_country", etCountry.getText().toString())
                .putString("user_state", etState.getText().toString())
                .putString("user_city", etCity.getText().toString())
                .putString("user_pincode", etPincode.getText().toString())
                .putString("user_address", etAddress.getText().toString())
                .putString("user_emergency_note", etEmergencyNote.getText().toString())
                .apply();
        }
        
        String groupId = prefs.getString("active_group", null);
        if (groupId == null) {
            Toast.makeText(this, "Enterprise Group Offline. Securing to Personal Vault...", Toast.LENGTH_SHORT).show();
        }
        
        viewModel.saveDossier(this, groupId, targetUid, payload);
    }
}
