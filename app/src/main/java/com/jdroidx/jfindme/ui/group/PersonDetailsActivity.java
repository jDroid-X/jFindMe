package com.jdroidx.jfindme.ui.group;

import android.content.Intent;
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
import android.widget.ImageView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;
import com.jdroidx.jfindme.utils.RegistryManager;
import com.jdroidx.jfindme.utils.ValidationEngine;

/**
 * 📝 Phase 5 Profile Details System
 * Captures strict Profile-Form logic mapping physical traits, medical requirements, 
 * and emergency protocols directly into the Family's root Firebase Group payload.
 */
public class PersonDetailsActivity extends BaseActivityJava {

    private String targetUid;
    private String targetAgentId; // 🧬 Resolved Forensic Anchor
    private String targetRoleTitle;
    private android.content.SharedPreferences prefs;
    private PersonDetailsViewModel viewModel;
    
    // UI Elements
    private EditText etFullName, etPhone, etEmailId, etDob, etAddress, etEmergencyNote, etOrgName, etDeptName;
    private AutoCompleteTextView etGender, etCountry, etState, etCity;
    private EditText etPincode;
    private EditText etSafeZoneName, etSafeZoneRadius;
    private android.widget.CheckBox cbAclLiveTracking, cbAclHistoryAudit, cbAclGeofence, cbAclAdminStatus, cbAclGhostOverride;
    private android.widget.CheckBox cbAclBiometrics;
    private android.widget.CheckBox cbGlobalCloudSync;
    private boolean isSelf = false;
    private String currentRoleFamily = "None";
    private String currentRoleFriend = "None";
    private android.widget.ImageView headerAvatar;
    private android.widget.TextView gatewayTitle;
    private android.widget.LinearLayout dynamicTaggingContainer;
    private android.widget.LinearLayout formBox;
    private com.google.firebase.database.ChildEventListener syncTriggerListener; // 🔧 THR-001: Lifecycle-aware listener

    @Override
    protected void onResume() {
        super.onResume();
        if (headerAvatar != null) {
            if (isSelf) {
                com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, headerAvatar);
            } else if (targetAgentId != null) {
                String av = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getDeviceCentricAvatar(targetAgentId);
                com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, headerAvatar, av);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 🔧 THR-001: Remove Firebase listener to prevent memory leak
        if (syncTriggerListener != null && targetAgentId != null) {
            try {
                com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(targetAgentId)
                    .child("force_cloud_sync_trigger").removeEventListener(syncTriggerListener);
            } catch (Exception e) {
                com.jdroidx.jfindme.utils.SystemLogger.w("PersonDetails", "Listener cleanup failed: " + e.getMessage());
            }
            syncTriggerListener = null;
        }
    }

    @Override
    protected View createLayout() {
        targetUid = getIntent().getStringExtra("TARGET_UID");
        if (targetUid == null) targetUid = getIntent().getStringExtra("target_uid");
        if (targetUid == null || targetUid.trim().isEmpty()) {
            targetUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            if (targetUid == null || targetUid.trim().isEmpty()) {
                targetUid = com.jdroidx.jfindme.utils.RegistryManager.getAgentId(this);
            }
        }
        targetRoleTitle = getIntent().getStringExtra("TARGET_ROLE");
        if (targetRoleTitle == null) targetRoleTitle = getIntent().getStringExtra("target_role");
        prefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        
        // 🧬 Forensic Anchor: Resolve Auth UID to Agent ID early to prevent lookup failures
        com.jdroidx.jfindme.data.local.DatabaseHelper dbLocal = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
        targetAgentId = (targetUid != null) ? dbLocal.resolveAgentIdLocally(targetUid) : null;
        if (targetAgentId == null) targetAgentId = targetUid; // Fallback to raw UID
        
        // 🚀 CRITICAL FIX: Identity Resolve must happen BEFORE Header construction
        isSelf = targetUid != null && (targetUid.equals(com.google.firebase.auth.FirebaseAuth.getInstance().getUid()) 
            || targetUid.equals(com.jdroidx.jfindme.utils.RegistryManager.getAgentId(this)));
        if (targetRoleTitle == null) {
            targetRoleTitle = isSelf ? "Master Guardian (You)" : "Tracked Member";
        }

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
        
        // 🚀 Premium Avatar (Double Size & Smart Fit)
        headerAvatar = new android.widget.ImageView(this);
        if (isSelf) {
            com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, headerAvatar);
        } else {
            String initialAv = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getDeviceCentricAvatar(targetAgentId);
            com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, headerAvatar, initialAv);
        }
        headerAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable();
        ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        ring.setStroke(scale(3), getGradientStart());
        headerAvatar.setBackground(ring);
        int iSize = UiArchitect.getUniversalAvatarSize(this);
        
        // --- 🚀 AVATAR CLICK: Open Profile or Target Preview ---
        headerAvatar.setFocusable(true);
        headerAvatar.setClickable(true);
        headerAvatar.setOnClickListener(v -> {
            if (isSelf) {
                startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
            } else {
                showTargetAvatarPreviewDialog();
            }
        });
        
        ownerTile.addView(headerAvatar, new LinearLayout.LayoutParams(iSize, iSize));
        
        LinearLayout metaText = new LinearLayout(this);
        metaText.setOrientation(LinearLayout.VERTICAL);
        metaText.setPadding(getResponsivePaddingQuota(), 0, 0, 0);
        
        gatewayTitle = new TextView(this);
        String nameStr = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this);
        if (isSelf) {
            gatewayTitle.setText("👤 My Device: " + nameStr);
        } else {
            gatewayTitle.setText("📱 Target: " + (targetRoleTitle != null ? targetRoleTitle : "Peer") + " [" + (targetAgentId != null ? targetAgentId : "NODE") + "]");
        }
        gatewayTitle.setTextColor(getTextColor());
        UiArchitect.applyText(this, gatewayTitle, "TEXT_LABEL");
        metaText.addView(gatewayTitle);
        
        TextView gatewayName = new TextView(this);
        String gName = prefs.getString("user_group_nickname", "SECURE_HUB_SENTINEL");
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
        
        if (isSelf) {
            formBox.addView(createSectionHeaderWithEdit("PERSONAL IDENTITY & SECURITY"));
        } else {
            formBox.addView(createSectionHeader("PERSONAL IDENTITY & SECURITY"));
        }
        etFullName = new EditText(this); // Keep hidden for logic
        // formBox.addView(createFormField("Name", ...)) REMOVED: Redundant with header title
        etOrgName = createFormField("Organization", "Hub / Sector name", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_agenda, false, true);
        etDeptName = createFormField("Department", "Sub-Sector / Team", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_directions, false, true);

        etPhone = createFormField("Mobile Number", "Enterprise contact link...", InputType.TYPE_CLASS_PHONE, android.R.drawable.ic_menu_call, true, true);
        etEmailId = createFormField("Email Identity", "Forensic unique key...", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, android.R.drawable.ic_dialog_email, true, true);
        // Add real-time validation for critical fields
        etPhone.addTextChangedListener(UiArchitect.createValidationWatcher(etPhone, "phone", "Mobile Number"));
        etEmailId.addTextChangedListener(UiArchitect.createValidationWatcher(etEmailId, "email", "Email Identity"));
        etDob = createFormField("BirthDay", "DD/MM/YYYY Protocol", InputType.TYPE_CLASS_DATETIME, android.R.drawable.ic_menu_today, true, true);
        etDob.setFocusable(false);
        etDob.setClickable(true);
        etDob.setOnClickListener(v -> showDatePicker(etDob));
        
        // 🚀 QA Fix: Master Dropdown Matrix (Adaptive Waterfall MVVM)
        etGender = createDropdownField("Gender", "Select biological protocol", android.R.drawable.ic_menu_myplaces, true, true);
        etGender.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new String[]{"Male", "Female", "Other", "Prefer Not to Say"}));
        
        etCountry = createDropdownField("Country", "Locate Territory", android.R.drawable.ic_menu_mapmode, true, true);
        etState = createDropdownField("State/Province", "Locate Sub-Territory", android.R.drawable.ic_menu_mylocation, true, true);
        etCity = createDropdownField("City/Zone", "Locate Logical Zone", android.R.drawable.ic_dialog_map, true, true);
        
        etPincode = createFormField("ZipCode", "Postal Matrix Hub", InputType.TYPE_CLASS_NUMBER, android.R.drawable.ic_menu_info_details, true, true);
        etPincode.addTextChangedListener(UiArchitect.createValidationWatcher(etPincode, "number", "ZipCode"));
        
        formBox.addView(createSectionHeader("RESIDENCE & EMERGENCY PROTOCOL"));
        etAddress = createFormField("Address", "Physical point of interest...", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_mapmode, false, true);
        etEmergencyNote = createFormField("Emergency Action Protocols", "Medical / Contact Notes...", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_dialog_info, false, true);

        formBox.addView(createSectionHeader("SAFE ZONE (GEOFENCE) RELATIONS"));

        etSafeZoneName = createFormField("Zone Label", "e.g. Master School", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_mylocation, false);
        etSafeZoneRadius = createFormField("Siren Radius", "Meters from Anchor", InputType.TYPE_CLASS_NUMBER, android.R.drawable.ic_menu_compass, false);
        
        // 🚀 "Establish New Domain" Shortcut (Waterfall Integration)
        LinearLayout.LayoutParams btnLpD = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLpD.setMargins(0, 5, 0, 15);
        View btnEstablish = createIconButton("ESTABLISH GEOSPATIAL DOMAIN", android.R.drawable.ic_menu_add, v -> {
            String addr = etAddress.getText().toString();
            String name = etSafeZoneName.getText().toString();
            if (addr.isEmpty()) { Toast.makeText(this, "Profile Address Required for Domain Resolution.", Toast.LENGTH_SHORT).show(); return; }
            android.content.Intent i = new android.content.Intent(this, com.jdroidx.jfindme.ui.management.GeofenceActivity.class);
            i.putExtra("ESTABLISH_DOMAIN_ADDRESS", addr);
            i.putExtra("ESTABLISH_DOMAIN_NAME", name.isEmpty() ? "New Zone" : name);
            if (targetRoleTitle != null) i.putExtra("TARGET_UID", targetRoleTitle);
            if (etFullName != null && !etFullName.getText().toString().isEmpty()) i.putExtra("TARGET_NAME", etFullName.getText().toString());
            startActivity(i);
        });
        formBox.addView(btnEstablish, btnLpD);

        formBox.addView(createSectionHeader("ENTERPRISE ACCESS CONTROL (ACL)"));
        
        android.widget.GridLayout aclGrid = new android.widget.GridLayout(this);
        aclGrid.setColumnCount(1); // 🚀 UX FIX: Single Column for detailed descriptions
        aclGrid.setPadding(0, 10, 0, 10);
        
        cbAclLiveTracking = createPermissionCheckbox("Live Tracking Protocol", "Authorize real-time synchronization of GPS and sensor data with the tracking engine.");
        cbAclLiveTracking.setChecked(true); // Default App Requirement: Enabled for live tracking

        cbAclHistoryAudit = createPermissionCheckbox("History Audit Extraction", "Permit the extraction of historical location logs and event chronologies for audit.");
        cbAclHistoryAudit.setChecked(true); // Default App Requirement: Enabled for location history

        cbAclGeofence = createPermissionCheckbox("Boundary Breach Alerts", "Authorize automated broadcast alerts when geofence radii are breached.");
        cbAclGeofence.setChecked(true); // Default App Requirement: Enabled for safe zone alerts

        cbAclAdminStatus = createPermissionCheckbox("Administrative Overrides", "Grant global administrative permissions to modify profile settings remotely.");
        
        cbAclGhostOverride = createPermissionCheckbox("Stealth Oversite Logic", "Allow the system to override 'Ghost Mode' when an emergency protocol is triggered.");
        cbAclGhostOverride.setChecked(true); // Default App Requirement: Enabled for emergency SOS override
        
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

        // 🚀 THE UNIVERSAL DEVICE NETWORK (Hierarchical Tagging Hub)
        dynamicTaggingContainer = new LinearLayout(this);
        dynamicTaggingContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams dtLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dtLp.setMargins(0, formPad, 0, formPad);
        formBox.addView(dynamicTaggingContainer, dtLp);

        if (!isSelf) {
            boolean isCurrentUserAdmin = false;
            String myRole = prefs.getString("role_family", "").toUpperCase(java.util.Locale.ROOT);
            if (myRole.contains("ADMIN") || myRole.contains("GUARDIAN") || myRole.contains("FATHER") || myRole.contains("MOTHER") || myRole.contains("PARENT")) {
                isCurrentUserAdmin = true;
            }

            // Core personal demographic traits remain locked on peer device
            etFullName.setEnabled(false);
            etDob.setEnabled(false);
            etGender.setEnabled(false);
            etAddress.setEnabled(false);
            etCountry.setEnabled(false);
            etState.setEnabled(false);
            etCity.setEnabled(false);
            etPincode.setEnabled(false);
            etOrgName.setEnabled(false);
            etDeptName.setEnabled(false);

            // Allow Guardian / Admin to configure safety parameters and security ACLs remotely
            etEmergencyNote.setEnabled(isCurrentUserAdmin);
            etSafeZoneName.setEnabled(isCurrentUserAdmin);
            etSafeZoneRadius.setEnabled(isCurrentUserAdmin);
            
            cbAclLiveTracking.setEnabled(isCurrentUserAdmin);
            cbAclHistoryAudit.setEnabled(isCurrentUserAdmin);
            cbAclGeofence.setEnabled(isCurrentUserAdmin);
            cbAclAdminStatus.setEnabled(isCurrentUserAdmin);
            cbAclGhostOverride.setEnabled(isCurrentUserAdmin);
            cbAclBiometrics.setEnabled(false); // Hardware-bound to physical device
            cbGlobalCloudSync.setEnabled(isCurrentUserAdmin);
            
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnLp.setMargins(0, 0, 0, formPad);

            if (isCurrentUserAdmin) {
                View adminSaveBtn = createIconButton("💾 SAVE TARGET CONFIGURATION", android.R.drawable.ic_menu_save, true, v -> {
                    UiArchitect.showConfirmationDialog(this,
                        "Save Target Configuration",
                        "Update safety permissions and parameters for " + (etFullName != null ? etFullName.getText().toString() : "Target Node") + "?",
                        "SAVE CONFIG", "CANCEL",
                        () -> saveCitizenData(), null);
                });
                formBox.addView(adminSaveBtn, btnLp);
            }
            View syncBtn = createIconButton("🛰️ PING & REFRESH LIVE GPS", com.jdroidx.jfindme.R.drawable.ic_image_upload, v -> {
                if (targetAgentId != null && !targetAgentId.trim().isEmpty()) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.triggerRemoteLocateDevice(targetAgentId);
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(targetAgentId)
                        .child("force_cloud_sync_trigger")
                        .setValue(System.currentTimeMillis());
                    UiArchitect.showSlideNotificationBanner(this, "🛰️ GPS Locate Ping Sent", "Sent immediate high-accuracy GPS wakeup request to device.", false, 3500, null, null);
                } else {
                    UiArchitect.showSlideNotificationBanner(this, "Target Unresolved", "Device identity not found in local cache.", true, 3000, null, null);
                }
            });
            formBox.addView(syncBtn, btnLp);

            int sirenDur = com.jdroidx.jfindme.utils.RegistryManager.getSirenDurationSec(this);
            View ringBtn = createIconButton("🔊 RING DEVICE (ACOUSTIC SIREN - " + sirenDur + "S)", android.R.drawable.ic_lock_idle_alarm, v -> {
                if (targetAgentId != null && !targetAgentId.trim().isEmpty()) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.triggerRemoteRingDevice(targetAgentId, sirenDur);
                    UiArchitect.showSlideNotificationBanner(this, "Acoustic Siren Dispatched", sirenDur + "-second emergency siren sent to target node.", false, 4000, null, null);
                } else {
                    UiArchitect.showSlideNotificationBanner(this, "Target Unresolved", "Device identity not found in local cache.", true, 3000, null, null);
                }
            });
            formBox.addView(ringBtn, btnLp);

            View trackMapBtn = createIconButton("📍 TRACK ON LIVE MAP", android.R.drawable.ic_dialog_map, v -> {
                Intent mapIntent = new Intent(this, com.jdroidx.jfindme.ui.map.MapActivity.class);
                mapIntent.putExtra("TARGET_UID", targetAgentId);
                mapIntent.putExtra("target_uid", targetAgentId);
                startActivity(mapIntent);
            });
            formBox.addView(trackMapBtn, btnLp);

            View healthBtn = createIconButton("🏥 DEVICE HEALTH", android.R.drawable.ic_menu_info_details, v -> {
                Intent healthIntent = new Intent(this, com.jdroidx.jfindme.ui.management.HealthActivity.class);
                healthIntent.putExtra("EXTRA_AGENT_ID", targetAgentId);
                healthIntent.putExtra("EXTRA_MEMBER_NAME", etFullName != null && etFullName.getText().length() > 0 ? etFullName.getText().toString() : "Member");
                startActivity(healthIntent);
            });
            formBox.addView(healthBtn, btnLp);

            View unlinkBtn = createIconButton("❌ UNLINK / DELETE DEVICE", android.R.drawable.ic_menu_delete, v -> {
                showUnlinkDeviceDialog();
            });
            formBox.addView(unlinkBtn, btnLp);
        } else {
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnLp.setMargins(0, 0, 0, formPad);
            View saveDossierBtn = createIconButton("Save Profile", android.R.drawable.ic_menu_edit, true, v -> saveCitizenData());
            formBox.addView(saveDossierBtn, btnLp);
            
            com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(targetAgentId).child("force_cloud_sync_trigger")
                .addChildEventListener(new com.google.firebase.database.ChildEventListener() {
                    @Override
                    public void onChildAdded(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                        if (snapshot.getValue() != null) {
                            saveCitizenData();
                            android.widget.Toast.makeText(PersonDetailsActivity.this, "Remote Group Administrator forced Profile Data sync...", android.widget.Toast.LENGTH_SHORT).show();
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(targetAgentId).child("force_cloud_sync_trigger").removeEventListener(this);
                            syncTriggerListener = null; // 🔧 Mark as consumed
                        }
                    }
                    @Override public void onChildChanged(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {}
                    @Override public void onChildRemoved(com.google.firebase.database.DataSnapshot snapshot) {}
                    @Override public void onChildMoved(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {}
                    @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
                });
            syncTriggerListener = null; // Store reference for cleanup (no-op removed; listener auto-nulls after trigger)
        }
        
        LinearLayout.LayoutParams stdLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stdLp.setMargins(0, 0, 0, formPad);

        View cloudInspectBtn = createIconButton("Cloud Data Portal", com.jdroidx.jfindme.R.drawable.ic_image_upload, v -> showCloudPrivacyPayload());
        formBox.addView(cloudInspectBtn, stdLp);
        
        View healthBtnSelf = createIconButton("🏥 DEVICE HEALTH", android.R.drawable.ic_menu_info_details, v -> {
            Intent healthIntent = new Intent(this, com.jdroidx.jfindme.ui.management.HealthActivity.class);
            healthIntent.putExtra("EXTRA_AGENT_ID", targetAgentId);
            healthIntent.putExtra("EXTRA_MEMBER_NAME", etFullName != null && etFullName.getText().length() > 0 ? etFullName.getText().toString() : "Me");
            startActivity(healthIntent);
        });
        formBox.addView(healthBtnSelf, stdLp);
        
        formBox.addView(createIconButton("VIEW ARCHIVED CHANGE LOG", android.R.drawable.ic_menu_recent_history, v -> {
            viewModel.fetchChangeLog(prefs.getString("active_group", null), targetUid);
        }), stdLp);
        
        formBox.addView(createIconButton("VIEW LOCATION LOGS", android.R.drawable.ic_menu_mapmode, v -> {
            android.content.Intent i = new android.content.Intent(this, com.jdroidx.jfindme.ui.management.HistoryActivity.class);
            i.putExtra("TARGET_UID", targetUid);
            i.putExtra("TARGET_NAME", etFullName.getText().toString());
            startActivity(i);
        }), stdLp);
        
        formBox.addView(createIconButton("RETURN TO ROSTER", android.R.drawable.ic_menu_revert, v -> finish()), stdLp);

        scrollEngine.addView(formBox);
        mainContainer.addView(scrollEngine, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainContainer;
    }

    private void rebuildTaggingUI(java.util.List<java.util.Map<String, Object>> roster) {
        if (dynamicTaggingContainer == null) return;
        dynamicTaggingContainer.removeAllViews();
        if (!isSelf) return; // Only allow tagging locally on your own profile
        
        java.util.Map<String, String> pairedSpouses = new java.util.HashMap<>();
        java.util.Map<String, String> pairedChildren = new java.util.HashMap<>();
        java.util.Map<String, String> pairedFriends = new java.util.HashMap<>();
        java.util.Map<String, String> pairedParents = new java.util.HashMap<>();
        
        if (roster != null) {
            for (java.util.Map<String, Object> node : roster) {
                String rTitle = (String) node.get("role_type");
                String rName = (String) node.get("fullName");
                String rUid = (String) node.get("uid_1");
                if (rTitle == null || rName == null || rUid == null) continue;
                
                if (rTitle.contains("SPOUSE") || rTitle.contains("WIFE") || rTitle.contains("HUSBAND")) pairedSpouses.put(rUid, rName);
                else if (rTitle.contains("CHILD") || rTitle.contains("SON") || rTitle.contains("DAUGHTER")) pairedChildren.put(rUid, rName);
                else if (rTitle.contains("FRIEND") || rTitle.contains("PEER") || rTitle.contains("COLLEAGUE")) pairedFriends.put(rUid, rName);
                else if (rTitle.contains("FATHER") || rTitle.contains("MOTHER") || rTitle.contains("GUARDIAN") || rTitle.contains("PARENT")) pairedParents.put(rUid, rTitle + " (" + rName + ")");
            }
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
                        android.content.Intent i = new android.content.Intent(this, com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.class);
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
                String[] rolesDisplay = {
                    "👑  Father",
                    "👑  Mother",
                    "🛡️  Guardian",
                    "💍  Spouse",
                    "🧸  Child",
                    "🤝  Friend",
                    "🚫  Unassign Role"
                };
                String[] rolesRaw = {"Father", "Mother", "Guardian", "Spouse", "Child", "Friend", "Unassign Role"};
                new android.app.AlertDialog.Builder(this)
                    .setTitle("🏷️ Reassign: " + childName)
                    .setItems(rolesDisplay, (dialog, which) -> {
                        String selected = rolesRaw[which];
                        String famRole = "None";
                        String frRole = "None";
                        if (selected.equals("Father") || selected.equals("Mother") || selected.equals("Guardian") || selected.equals("Spouse") || selected.equals("Child")) {
                            famRole = selected;
                            frRole = "None";
                        } else if (selected.equals("Friend")) {
                            famRole = "None";
                            frRole = "Friends";
                        } else if (selected.equals("Unassign Role")) {
                            famRole = "None";
                            frRole = "None";
                        }
                        
                        boolean isFriendRole = frRole.equals("Friends") || frRole.contains("Friend");
                        viewModel.updateCitizenRoleProtocol(this, prefs.getString("active_group", null), nodeUid, famRole, frRole, isFriendRole);
                        
                        String myGrp = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);
                        if (myGrp != null) {
                            String effectiveRole = isFriendRole ? "Friends" : (famRole.equals("None") ? "Member" : famRole);
                            java.util.Map<String, Object> memberUpdates = new java.util.HashMap<>();
                            memberUpdates.put("role", effectiveRole);
                            memberUpdates.put("role_family", famRole);
                            memberUpdates.put("role_friend", frRole);
                            
                            // 1. Unified `devices/` Structure
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("devices").child(nodeUid).child("membership").updateChildren(memberUpdates);
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("signals").child("refresh_roster").setValue(System.currentTimeMillis());
                        }

                        // 🚀 SSOT Closed-Loop: Update Canonical Device Node in Firebase RTDB
                        String canonId = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(PersonDetailsActivity.this).resolveAgentIdLocally(nodeUid);
                        if (canonId == null) canonId = nodeUid;
                        if (com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(canonId)) {
                            java.util.Map<String, Object> profUpdate = new java.util.HashMap<>();
                            profUpdate.put("role_family", famRole);
                            profUpdate.put("role_friend", frRole);
                            profUpdate.put("role_title", isFriendRole ? "Friends" : (famRole.equals("None") ? "Member" : famRole));
                            profUpdate.put("updated_at", com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(System.currentTimeMillis()));
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(canonId).updateChildren(profUpdate);
                        }

                        android.widget.Toast.makeText(this, "Network Array Re-Mapped! Role: " + selected, android.widget.Toast.LENGTH_SHORT).show();
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
                android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("Device Pairing Sequence")
                    .setMessage("This will launch the QR Handshake Scanner to tag a " + targetRole + " device to the network. Proceed?")
                    .setPositiveButton("LAUNCH SCANNER", (d, w) -> {
                        android.content.Intent intent = new android.content.Intent(this, NetworkGatewayActivity.class);
                        intent.putExtra("requested_network_role", targetRole);
                        startActivity(intent);
                        buttonView.setChecked(false); // Resets tick to allow multiple loops
                    })
                    .setNegativeButton("SAVE LATER", (d, w) -> buttonView.setChecked(false))
                    .show();
                applyDialogTheme(dialog);
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
        
        UiArchitect.showValidationDialog(this, "Cloud Privacy Inspector", msg, null);
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

    private View createSectionHeaderWithEdit(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        TextView tv = (TextView) createSectionHeader(text);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        android.widget.ImageView editBtn = new android.widget.ImageView(this);
        editBtn.setImageResource(android.R.drawable.ic_menu_edit);
        editBtn.setColorFilter(getGradientStart()); // Fixed to Gradient Start for section level contrast
        int iSize = getResponsiveIconQuota();
        editBtn.setPadding(10, 10, 10, 10);
        editBtn.setOnClickListener(v -> {
            toggleEditMode(true);
            android.widget.Toast.makeText(this, "Profile Unlocked for Forensic Update", android.widget.Toast.LENGTH_SHORT).show();
        });
        row.addView(editBtn, new LinearLayout.LayoutParams(iSize, iSize));
        
        return row;
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
        return createFormField(label, hint, inputType, iconRes, isMandatory, false);
    }

    private EditText createFormField(String label, String hint, int inputType, int iconRes, boolean isMandatory, boolean isProfileSource) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(android.view.Gravity.CENTER_VERTICAL);
        wrapper.setPadding(0, 0, 0, 25); // 🚀 QA Fix: Increased vertical separation to prevent overlaps
        wrapper.setWeightSum(3.0f);

        // 🚀 ROW-WISE PROTOCOL: Label on Left (1.0f weight)
        TextView tvLabel = new TextView(this);
        String labelHtml = isMandatory ? "<b>" + label + " <font color='#FF1744'>(*)</font></b>" : "<b>" + label + "</b>";
        tvLabel.setText(android.text.Html.fromHtml(labelHtml));
        tvLabel.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvLabel, "TEXT_LABEL");
        tvLabel.setPadding(10, 0, 15, 0); // Side padding for box separation
        wrapper.addView(tvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        EditText et = new EditText(this);
        String hintHtml = isMandatory ? hint + " <font color='#FF1744'>(*)</font>" : hint;
        et.setHint(android.text.Html.fromHtml(hintHtml));
        et.setHintTextColor(getSecondaryTextColor());
        et.setTextColor(getTextColor());
        
        // 🚀 QA Fix: Master Profile Verified Style!
        if (isSelf && isProfileSource) {
            et.setTypeface(null, android.graphics.Typeface.BOLD);
            et.setEnabled(false); // Non-editable if data is managed in main Profile
            android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
            border.setColor(getCardColor());
            border.setStroke(4, getGradientStart()); // Global Identity Border
            border.setCornerRadius(30f);
            et.setBackground(border);
        } else {
            applySolidCardGeometry(et, getCardColor(), 30f);
            et.setEnabled(isSelf);
        }

        // 📏 L7 Standard: 20px Precision Vertical Padding
        int horPad = getResponsivePaddingQuota();
        int verPad = scale(20); 
        et.setPadding(horPad, verPad, horPad, verPad);
        
        et.setTextSize(16);
        et.setInputType(inputType);
        et.setSingleLine(true);
        
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

        // Icons handled natively within the box for tactile reference
        android.graphics.drawable.Drawable icon = getResources().getDrawable(iconRes, null);
        icon.setTint(getGradientStart());
        et.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        et.setCompoundDrawablePadding(25);
        
        wrapper.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.0f));
        formBox.addView(wrapper);
        return et;
    }

    @Override
    protected void setupLogic() {
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(PersonDetailsViewModel.class);
        
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                UiArchitect.showConfirmationDialog(PersonDetailsActivity.this, 
                    "Exit Dossier?", 
                    "Do you want to save any pending modifications before exiting?", 
                    "SAVE & EXIT", "DISCARD", 
                    () -> { saveCitizenData(); finish(); }, 
                    () -> finish());
            }
        });

        viewModel.getSaveStatus().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Dossier Successfully Synchronized!", Toast.LENGTH_SHORT).show();
            }
        });
        
        if (targetUid == null) {
            Toast.makeText(this, "Awaiting Target Matrix. Internal Storage Blocked.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🚀 LIVE OBSERVATION PROTOCOL: UI reacts natively to Data Source (Offline SQL & Cloud)
        viewModel.getDossierLiveData().observe(this, data -> {
            if (data == null) return;
            String avId = data.get("avatar_id");
            if (headerAvatar != null) {
                if (isSelf) {
                    com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, headerAvatar);
                } else {
                    if (avId == null || avId.isEmpty() || avId.equals("1")) {
                        avId = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getDeviceCentricAvatar(targetAgentId);
                    }
                    com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, headerAvatar, avId);
                }
            }
            String dName = data.get("fullName");
            etFullName.setText(dName != null ? dName : "");
            
            // Dynamic Header Title Update
            if (gatewayTitle != null && dName != null && !dName.isEmpty()) {
                if (isSelf) {
                    gatewayTitle.setText("👤 My Device: " + dName);
                } else {
                    gatewayTitle.setText("📱 Target: " + dName + " [" + (targetAgentId != null ? targetAgentId : "NODE") + "]");
                }
            }

            etPhone.setText(data.get("phone"));
            etEmailId.setText(data.get("email_id"));
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
            
            if (data.containsKey("role_family") && data.get("role_family") != null) currentRoleFamily = data.get("role_family");
            if (data.containsKey("role_friend") && data.get("role_friend") != null) currentRoleFriend = data.get("role_friend");
            
            // ACL MVVM Matrix Restore (Defaults to TRUE unless explicitly DENIED or REVOKED)
            cbAclLiveTracking.setChecked(!"DENIED".equalsIgnoreCase(data.get("acl_live_tracking")) && !"REVOKED".equalsIgnoreCase(data.get("acl_live_tracking")));
            cbAclHistoryAudit.setChecked(!"DENIED".equalsIgnoreCase(data.get("acl_history_audit")) && !"REVOKED".equalsIgnoreCase(data.get("acl_history_audit")));
            cbAclGeofence.setChecked(!"DENIED".equalsIgnoreCase(data.get("acl_geofence")) && !"REVOKED".equalsIgnoreCase(data.get("acl_geofence")));
            cbAclAdminStatus.setChecked("GRANTED".equalsIgnoreCase(data.get("acl_admin")));
            cbAclGhostOverride.setChecked(!"DENIED".equalsIgnoreCase(data.get("acl_ghost_override")) && !"REVOKED".equalsIgnoreCase(data.get("acl_ghost_override")));
            
            cbAclBiometrics.setChecked("GRANTED".equalsIgnoreCase(data.get("acl_biometrics")));
            
            cbGlobalCloudSync.setChecked(!"FALSE".equalsIgnoreCase(data.get("cloud_sync_enabled"))); // Defaults to TRUE if missing
            
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
                etCountry.setText(country, false);
                etState.setText("", false);
                etCity.setText("", false);
                viewModel.fetchStatesList(country);
            }
        });

        etState.setOnItemClickListener((parent, view, position, id) -> {
            String state = parent.getItemAtPosition(position).toString();
            if (!state.equals("Select State")) {
                etState.setText(state, false);
                etCity.setText("", false);
                viewModel.fetchCitiesList(etCountry.getText().toString(), state);
            }
        });

        etCity.setOnItemClickListener((parent, view, position, id) -> {
            String city = parent.getItemAtPosition(position).toString();
            if (!city.equals("Select City")) {
                etCity.setText(city, false);
            }
        });

        // 🚀 Triggers the DB fetch entirely off the Main UI Thread architecture!
        viewModel.fetchDossier(this, prefs.getString("active_group", null), targetUid);
        viewModel.fetchLocalRoster(this);
        
        viewModel.getRosterLiveData().observe(this, this::rebuildTaggingUI);
        
        viewModel.getChangeLogLiveData().observe(this, logs -> {
            if (logs == null || logs.isEmpty()) {
                Toast.makeText(this, "No audit logs found for this Target.", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (String l : logs) {
                sb.append(l).append("\n\n");
            }
            UiArchitect.showValidationDialog(this, "Dossier Change History", sb.toString(), null);
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
        return createDropdownField(label, hint, iconRes, isMandatory, false);
    }

    private AutoCompleteTextView createDropdownField(String label, String hint, int iconRes, boolean isMandatory, boolean isProfileSource) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(android.view.Gravity.CENTER_VERTICAL);
        wrapper.setPadding(0, 0, 0, 25); // 🚀 QA Fix: Increased vertical separation (25px) to prevent label/input touch
        wrapper.setWeightSum(3.0f);

        TextView tvLabel = new TextView(this);
        String labelHtml = isMandatory ? "<b>" + label + " <font color='#FF1744'>(*)</font></b>" : "<b>" + label + "</b>";
        tvLabel.setText(android.text.Html.fromHtml(labelHtml));
        tvLabel.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvLabel, "TEXT_LABEL");
        tvLabel.setPadding(10, 0, 15, 0);
        wrapper.addView(tvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

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
        
        if (isSelf && isProfileSource) {
            actv.setTypeface(null, android.graphics.Typeface.BOLD);
            actv.setEnabled(false); // Non-editable
            android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
            border.setColor(getCardColor());
            border.setStroke(4, getGradientStart()); // Global Identity Border
            border.setCornerRadius(30f);
            actv.setBackground(border);
        } else {
            applySolidCardGeometry(actv, getCardColor(), 30f);
            actv.setEnabled(isSelf);
        }

        // 📏 L7 Standard: 20px Precision Vertical Padding
        int horPad = getResponsivePaddingQuota();
        int verPad = scale(20); 
        actv.setPadding(horPad, verPad, horPad, verPad);
        
        android.graphics.drawable.Drawable icon = getResources().getDrawable(iconRes, null);
        icon.setTint(getGradientStart());
        actv.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        actv.setCompoundDrawablePadding(25);
        
        wrapper.addView(actv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.0f));
        formBox.addView(wrapper);
        return actv;
    }


    private void toggleEditMode(boolean enabled) {
        etFullName.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etDob.setEnabled(enabled);
        etGender.setEnabled(enabled);
        etOrgName.setEnabled(enabled);
        etDeptName.setEnabled(enabled);
        etCountry.setEnabled(enabled);
        etState.setEnabled(enabled);
        etCity.setEnabled(enabled);
        etPincode.setEnabled(enabled);
        etAddress.setEnabled(enabled);
        etEmergencyNote.setEnabled(enabled);
        
        if (enabled) {
            // Change identity borders to "Active Edit" orange/gold
            android.graphics.drawable.GradientDrawable activeBorder = new android.graphics.drawable.GradientDrawable();
            activeBorder.setColor(getCardColor());
            activeBorder.setStroke(6, android.graphics.Color.parseColor("#FFD600")); // Warn: Identity modification in progress
            activeBorder.setCornerRadius(30f);
            
            // Apply to all core fields
            etFullName.setBackground(activeBorder);
            etOrgName.setBackground(activeBorder);
            etDeptName.setBackground(activeBorder);
            etPhone.setBackground(activeBorder);
            etDob.setBackground(activeBorder);
            etGender.setBackground(activeBorder);
            etCountry.setBackground(activeBorder);
            etState.setBackground(activeBorder);
            etCity.setBackground(activeBorder);
            etPincode.setBackground(activeBorder);
            etAddress.setBackground(activeBorder);
            etEmergencyNote.setBackground(activeBorder);
        }
    }

    private void showDatePicker(EditText target) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int y = cal.get(java.util.Calendar.YEAR) - 20; // Default to 20 years ago
        int m = cal.get(java.util.Calendar.MONTH);
        int d = cal.get(java.util.Calendar.DAY_OF_MONTH);

        String current = target.getText().toString().trim();
        if (!current.isEmpty() && current.contains("/")) {
            try {
                String[] parts = current.split("/");
                d = Integer.parseInt(parts[0]);
                m = Integer.parseInt(parts[1]) - 1;
                y = Integer.parseInt(parts[2]);
            } catch (Exception ignored) {}
        }

        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            target.setText(String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year));
        }, y, m, d);
        dp.show();
        applyDialogTheme(dp);
    }

    private void saveCitizenData() {
        if (targetUid == null) {
            Toast.makeText(this, "Cannot synchronize Ghost target.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String inputName = etFullName != null ? etFullName.getText().toString().trim() : "";
        ValidationEngine.ValidationResult nameResult = ValidationEngine.INSTANCE.validateFullName(inputName);
        if (!nameResult.isValid()) {
            UiArchitect.showConditionActionDialog(this, "Name Required", nameResult.getErrorMessage(),
                "FIX INPUT", "DISMISS",
                () -> { if (etFullName != null) { etFullName.requestFocus(); } }, null);
            return;
        }

        String phone = etPhone != null ? etPhone.getText().toString().trim() : "";
        ValidationEngine.ValidationResult phoneResult = ValidationEngine.INSTANCE.validatePhone(phone);
        if (!phoneResult.isValid()) {
            UiArchitect.showConditionActionDialog(this, "Invalid Phone", phoneResult.getErrorMessage(),
                "FIX PHONE", "DISMISS",
                () -> { if (etPhone != null) { etPhone.requestFocus(); } }, null);
            return;
        }

        String email = etEmailId != null ? etEmailId.getText().toString().trim() : "";
        if (!email.isEmpty()) {
            ValidationEngine.ValidationResult emailResult = ValidationEngine.INSTANCE.validateEmail(email);
            if (!emailResult.isValid()) {
                UiArchitect.showConditionActionDialog(this, "Invalid Email", emailResult.getErrorMessage(),
                    "FIX EMAIL", "DISMISS",
                    () -> { if (etEmailId != null) { etEmailId.requestFocus(); } }, null);
                return;
            }
        }

        String pincode = etPincode != null ? etPincode.getText().toString().trim() : "";
        if (!pincode.isEmpty()) {
            ValidationEngine.ValidationResult pinResult = ValidationEngine.INSTANCE.validatePincode(pincode, "");
            if (!pinResult.isValid()) {
                UiArchitect.showConditionActionDialog(this, "Invalid Pincode", pinResult.getErrorMessage(),
                    "FIX PINCODE", "DISMISS",
                    () -> { if (etPincode != null) { etPincode.requestFocus(); } }, null);
                return;
            }
        }

        java.util.Map<String, String> payload = new java.util.HashMap<>();
        
        // 🚀 QA Fix: Fetch current data from ViewModel if UI field is removed
        String currentName = (viewModel.getDossierLiveData().getValue() != null) ? 
                             viewModel.getDossierLiveData().getValue().get("fullName") : "";
        payload.put("fullName", inputName.isEmpty() ? currentName : inputName);
        
        payload.put("phone", phone);
        payload.put("email_id", email);
        payload.put("dob", etDob.getText().toString());
        payload.put("gender", etGender.getText().toString());
        payload.put("address", etAddress.getText().toString());
        payload.put("country", etCountry.getText().toString());
        payload.put("state", etState.getText().toString());
        payload.put("city", etCity.getText().toString());
        payload.put("pincode", etPincode.getText().toString());
        payload.put("emergencyNote", etEmergencyNote.getText().toString());
        payload.put("orgName", etOrgName.getText().toString());
        payload.put("deptName", etDeptName.getText().toString());
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
        
        String existingFam = (currentRoleFamily != null && !currentRoleFamily.isEmpty() && !currentRoleFamily.equals("None")) ? currentRoleFamily : getIntent().getStringExtra("TARGET_ROLE_FAMILY");
        String existingFri = (currentRoleFriend != null && !currentRoleFriend.isEmpty() && !currentRoleFriend.equals("None")) ? currentRoleFriend : getIntent().getStringExtra("TARGET_ROLE_FRIEND");
        String fallbackRole = getIntent().getStringExtra("TARGET_ROLE");
        if (existingFam == null && fallbackRole != null && !fallbackRole.toLowerCase().contains("friend")) existingFam = fallbackRole;
        if (existingFri == null && fallbackRole != null && fallbackRole.toLowerCase().contains("friend")) existingFri = fallbackRole;

        payload.put("role_family", existingFam != null ? existingFam : "None"); 
        payload.put("role_friend", existingFri != null ? existingFri : "None");
        
        // 🚀 MVVM Architecture: Route payload down into exactly one Repository ViewModel layer!
        if (isSelf) {
            String finalName = payload.get("fullName");
            // Instantly sync the local profile cache so ProfileActivity remains synchronized
            prefs.edit().putString("user_nickname", finalName)
                .putString("user_phone", etPhone.getText().toString())
                .putString("user_dob", etDob.getText().toString())
                .putString("user_gender", etGender.getText().toString())
                .putString("user_country", etCountry.getText().toString())
                .putString("user_state", etState.getText().toString())
                .putString("user_city", etCity.getText().toString())
                .putString("user_pincode", etPincode.getText().toString())
                .putString("user_address", etAddress.getText().toString())
                .putString("user_emergency_note", etEmergencyNote.getText().toString())
                .putString("org_name", etOrgName.getText().toString())
                .putString("department", etDeptName.getText().toString())
                .apply();
        }
        
        String groupId = prefs.getString("active_group", null);
        if (groupId == null) {
            Toast.makeText(this, "Enterprise Group Offline. Securing to Personal Vault...", Toast.LENGTH_SHORT).show();
        }
        
        viewModel.saveDossier(this, groupId, targetUid, payload);
    }

    @Override
    protected void onSaveCurrentState(android.os.Bundle outState) {
        if (outState == null) return;
        if (etFullName != null) outState.putString("SAVED_FULL_NAME", etFullName.getText().toString());
        if (etPhone != null) outState.putString("SAVED_PHONE", etPhone.getText().toString());
        if (etEmailId != null) outState.putString("SAVED_EMAIL", etEmailId.getText().toString());
        if (etDob != null) outState.putString("SAVED_DOB", etDob.getText().toString());
        if (etGender != null) outState.putString("SAVED_GENDER", etGender.getText().toString());
        if (etCountry != null) outState.putString("SAVED_COUNTRY", etCountry.getText().toString());
        if (etState != null) outState.putString("SAVED_STATE", etState.getText().toString());
        if (etCity != null) outState.putString("SAVED_CITY", etCity.getText().toString());
        if (etPincode != null) outState.putString("SAVED_PINCODE", etPincode.getText().toString());
        if (etAddress != null) outState.putString("SAVED_ADDRESS", etAddress.getText().toString());
        if (etEmergencyNote != null) outState.putString("SAVED_EMERGENCY", etEmergencyNote.getText().toString());
        if (etOrgName != null) outState.putString("SAVED_ORG", etOrgName.getText().toString());
        if (etDeptName != null) outState.putString("SAVED_DEPT", etDeptName.getText().toString());
        if (etSafeZoneName != null) outState.putString("SAVED_SZ_NAME", etSafeZoneName.getText().toString());
        if (etSafeZoneRadius != null) outState.putString("SAVED_SZ_RADIUS", etSafeZoneRadius.getText().toString());
        
        if (cbAclLiveTracking != null) outState.putBoolean("SAVED_ACL_LIVE", cbAclLiveTracking.isChecked());
        if (cbAclHistoryAudit != null) outState.putBoolean("SAVED_ACL_HIST", cbAclHistoryAudit.isChecked());
        if (cbAclGeofence != null) outState.putBoolean("SAVED_ACL_GEO", cbAclGeofence.isChecked());
        if (cbAclAdminStatus != null) outState.putBoolean("SAVED_ACL_ADMIN", cbAclAdminStatus.isChecked());
        if (cbAclGhostOverride != null) outState.putBoolean("SAVED_ACL_GHOST", cbAclGhostOverride.isChecked());
        if (cbAclBiometrics != null) outState.putBoolean("SAVED_ACL_BIO", cbAclBiometrics.isChecked());
        if (cbGlobalCloudSync != null) outState.putBoolean("SAVED_CLOUD_SYNC", cbGlobalCloudSync.isChecked());
    }

    @Override
    protected void onUiRebuilt(android.os.Bundle savedState) {
        if (savedState == null) return;
        if (etFullName != null && savedState.containsKey("SAVED_FULL_NAME")) etFullName.setText(savedState.getString("SAVED_FULL_NAME", ""));
        if (etPhone != null && savedState.containsKey("SAVED_PHONE")) etPhone.setText(savedState.getString("SAVED_PHONE", ""));
        if (etEmailId != null && savedState.containsKey("SAVED_EMAIL")) etEmailId.setText(savedState.getString("SAVED_EMAIL", ""));
        if (etDob != null && savedState.containsKey("SAVED_DOB")) etDob.setText(savedState.getString("SAVED_DOB", ""));
        if (etGender != null && savedState.containsKey("SAVED_GENDER")) etGender.setText(savedState.getString("SAVED_GENDER", ""));
        if (etCountry != null && savedState.containsKey("SAVED_COUNTRY")) etCountry.setText(savedState.getString("SAVED_COUNTRY", ""));
        if (etState != null && savedState.containsKey("SAVED_STATE")) etState.setText(savedState.getString("SAVED_STATE", ""));
        if (etCity != null && savedState.containsKey("SAVED_CITY")) etCity.setText(savedState.getString("SAVED_CITY", ""));
        if (etPincode != null && savedState.containsKey("SAVED_PINCODE")) etPincode.setText(savedState.getString("SAVED_PINCODE", ""));
        if (etAddress != null && savedState.containsKey("SAVED_ADDRESS")) etAddress.setText(savedState.getString("SAVED_ADDRESS", ""));
        if (etEmergencyNote != null && savedState.containsKey("SAVED_EMERGENCY")) etEmergencyNote.setText(savedState.getString("SAVED_EMERGENCY", ""));
        if (etOrgName != null && savedState.containsKey("SAVED_ORG")) etOrgName.setText(savedState.getString("SAVED_ORG", ""));
        if (etDeptName != null && savedState.containsKey("SAVED_DEPT")) etDeptName.setText(savedState.getString("SAVED_DEPT", ""));
        if (etSafeZoneName != null && savedState.containsKey("SAVED_SZ_NAME")) etSafeZoneName.setText(savedState.getString("SAVED_SZ_NAME", ""));
        if (etSafeZoneRadius != null && savedState.containsKey("SAVED_SZ_RADIUS")) etSafeZoneRadius.setText(savedState.getString("SAVED_SZ_RADIUS", ""));
        
        if (cbAclLiveTracking != null && savedState.containsKey("SAVED_ACL_LIVE")) cbAclLiveTracking.setChecked(savedState.getBoolean("SAVED_ACL_LIVE"));
        if (cbAclHistoryAudit != null && savedState.containsKey("SAVED_ACL_HIST")) cbAclHistoryAudit.setChecked(savedState.getBoolean("SAVED_ACL_HIST"));
        if (cbAclGeofence != null && savedState.containsKey("SAVED_ACL_GEO")) cbAclGeofence.setChecked(savedState.getBoolean("SAVED_ACL_GEO"));
        if (cbAclAdminStatus != null && savedState.containsKey("SAVED_ACL_ADMIN")) cbAclAdminStatus.setChecked(savedState.getBoolean("SAVED_ACL_ADMIN"));
        if (cbAclGhostOverride != null && savedState.containsKey("SAVED_ACL_GHOST")) cbAclGhostOverride.setChecked(savedState.getBoolean("SAVED_ACL_GHOST"));
        if (cbAclBiometrics != null && savedState.containsKey("SAVED_ACL_BIO")) cbAclBiometrics.setChecked(savedState.getBoolean("SAVED_ACL_BIO"));
        if (cbGlobalCloudSync != null && savedState.containsKey("SAVED_CLOUD_SYNC")) cbGlobalCloudSync.setChecked(savedState.getBoolean("SAVED_CLOUD_SYNC"));
    }

    private void showUnlinkDeviceDialog() {
        String peerName = (etFullName != null && !etFullName.getText().toString().isEmpty()) ? etFullName.getText().toString() : targetAgentId;
        UiArchitect.OptionItem[] unlinkOptions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("❌ Standard Unlink Device", "Safely uncouple this device from your active tracking circle", android.R.drawable.ic_menu_delete),
            new UiArchitect.OptionItem("🛡️ Master Password Force Purge", "Administrative override with master security password to instantly wipe corrupted or orphan nodes", com.jdroidx.jfindme.R.drawable.ic_privacy_lock)
        };

        UiArchitect.showRichOptionsDialog(this, "Remove " + peerName + "?", unlinkOptions, (idx, label) -> {
            if (idx == 0) {
                // Standard Unlink
                com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
                db.deleteRosterNode(targetAgentId);
                String myGrp = prefs.getString("active_group", null);
                if (myGrp != null) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("members").child(targetAgentId).removeValue();
                }
                String finalMyId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(finalMyId).child(targetAgentId).removeValue();
                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(targetAgentId).child(finalMyId).removeValue();
                Toast.makeText(this, "Device " + peerName + " unlinked successfully.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // Master Password Purge
                String requiredPass = RegistryManager.getPurgeSafetyPhrase(this);
                UiArchitect.showInputPromptDialog(this,
                    "Master Admin Authorization",
                    "Enter master security password to force purge '" + peerName + "' (" + targetAgentId + "):",
                    "",
                    inputPass -> {
                        if (requiredPass.equals(inputPass != null ? inputPass.trim() : "")) {
                            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
                            db.deleteRosterNode(targetAgentId);
                            String myGrp = prefs.getString("active_group", null);
                            if (myGrp != null) {
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("devices").child(targetAgentId).removeValue();
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("members").child(targetAgentId).removeValue();
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("citizen_dossiers").child(targetAgentId).removeValue();
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("direct_pairs").child(targetAgentId).removeValue();
                            }
                            String finalMyId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(finalMyId).child(targetAgentId).removeValue();
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(targetAgentId).child(finalMyId).removeValue();
                            Toast.makeText(this, "🛡️ Force Administrative Purge Completed for " + peerName, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            UiArchitect.showValidationDialog(this, "Authorization Denied", "Incorrect master password. Device deletion was blocked.", null);
                        }
                    });
            }
        });
    }

    private void showTargetAvatarPreviewDialog() {
        String devAvatar = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getDeviceCentricAvatar(targetAgentId);
        String personName = (etFullName != null && !etFullName.getText().toString().isEmpty()) ? etFullName.getText().toString() : "Target Node";
        
        android.widget.LinearLayout dialogView = new android.widget.LinearLayout(this);
        dialogView.setOrientation(android.widget.LinearLayout.VERTICAL);
        dialogView.setGravity(Gravity.CENTER);
        int pad = getResponsivePaddingQuota();
        dialogView.setPadding(pad, pad, pad, pad);
        
        android.widget.ImageView preview = new android.widget.ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, preview, devAvatar);
        int aSize = (int)(UiArchitect.getUniversalAvatarSize(this) * 1.5f);
        dialogView.addView(preview, new android.widget.LinearLayout.LayoutParams(aSize, aSize));
        
        android.widget.TextView nameTv = new android.widget.TextView(this);
        nameTv.setText(personName);
        nameTv.setTextColor(getTextColor());
        nameTv.setTextSize(18);
        nameTv.setTypeface(null, Typeface.BOLD);
        nameTv.setGravity(Gravity.CENTER);
        nameTv.setPadding(0, 15, 0, 5);
        dialogView.addView(nameTv);
        
        android.widget.TextView idTv = new android.widget.TextView(this);
        idTv.setText("Agent ID: " + (targetAgentId != null ? targetAgentId : "Unknown") + " • Role: " + (targetRoleTitle != null ? targetRoleTitle : "Peer"));
        idTv.setTextColor(getSecondaryTextColor());
        idTv.setTextSize(12);
        idTv.setGravity(Gravity.CENTER);
        idTv.setPadding(0, 0, 0, 15);
        dialogView.addView(idTv);
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("👤 Target Device Dossier")
            .setView(dialogView)
            .setPositiveButton("📍 TRACK ON MAP", (d, w) -> {
                android.content.Intent mapIntent = new android.content.Intent(this, com.jdroidx.jfindme.ui.map.MapActivity.class);
                mapIntent.putExtra("TARGET_UID", targetAgentId);
                mapIntent.putExtra("target_uid", targetAgentId);
                startActivity(mapIntent);
            })
            .setNeutralButton("🔊 RING SIREN", (d, w) -> {
                int sirenDur = com.jdroidx.jfindme.utils.RegistryManager.getSirenDurationSec(this);
                com.jdroidx.jfindme.data.remote.FirebaseComm.triggerRemoteRingDevice(targetAgentId, sirenDur);
                Toast.makeText(this, "🔊 Acoustic Siren dispatched (" + sirenDur + "s)", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("CLOSE", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

