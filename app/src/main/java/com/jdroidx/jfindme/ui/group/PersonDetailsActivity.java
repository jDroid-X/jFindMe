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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;

/**
 * 📝 Phase 5 Citizen Dossier System
 * Captures strict Citizen-Form logic mapping physical traits, medical requirements, 
 * and emergency protocols directly into the Family's root Firebase Group payload.
 */
public class PersonDetailsActivity extends BaseActivityJava {

    private String targetUid;
    private String targetRoleTitle;
    private android.content.SharedPreferences prefs;
    private PersonDetailsViewModel viewModel;
    
    // UI Elements
    private EditText etFullName, etDob, etGender, etAddress, etEmergencyNote;
    private EditText etSafeZoneName, etSafeZoneRadius;
    private android.widget.RadioGroup rgFamilyRole, rgFriendRole;
    private android.widget.CheckBox cbAclLiveTracking, cbAclHistoryAudit, cbAclGeofence, cbAclAdminStatus, cbAclGhostOverride;
    private android.widget.CheckBox cbGlobalCloudSync;
    private boolean isSelf = false;
    private android.widget.LinearLayout dynamicTaggingContainer;

    @Override
    protected View createLayout() {
        targetUid = getIntent().getStringExtra("TARGET_UID");
        targetRoleTitle = getIntent().getStringExtra("TARGET_ROLE");
        prefs = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE);

        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setBackgroundColor(getPrimaryColor());

        // 1. HERO BANNER
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header); // 🚀 Native QA Architecture Injection
        TextView title = new TextView(this);
        title.setText("CITIZEN DOSSIER");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Target Profile: " + (targetRoleTitle != null ? targetRoleTitle : "UNKNOWN"));
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        header.addView(subtitle);
        
        mainContainer.addView(header);

        // 2. SCROLLING CITIZEN FORM FRAMEWORK
        ScrollView scrollEngine = new ScrollView(this);
        LinearLayout formBox = new LinearLayout(this);
        formBox.setOrientation(LinearLayout.VERTICAL);
        formBox.setPadding(40, 40, 40, 100);
        
        isSelf = targetUid != null && targetUid.equals(com.google.firebase.auth.FirebaseAuth.getInstance().getUid());

        formBox.addView(createSectionHeader("NETWORK RELATIONSHIP (ROLE)"));
        
        TextView labelFamily = new TextView(this);
        labelFamily.setText("👨‍👩‍👧‍👦 FAMILY TIER (Max 1)");
        labelFamily.setTextColor(getGradientStart());
        labelFamily.setPadding(10, 20, 0, 10);
        labelFamily.setTypeface(null, Typeface.BOLD);
        formBox.addView(labelFamily);
        
        rgFamilyRole = new android.widget.RadioGroup(this);
        rgFamilyRole.setOrientation(LinearLayout.VERTICAL);
        rgFamilyRole.setPadding(0, 0, 0, 30);
        String[] familyRoles = {"Mother", "Father", "Child", "Spouse"};
        for (int i = 0; i < familyRoles.length; i++) {
            android.widget.RadioButton rb = new android.widget.RadioButton(this);
            rb.setText(familyRoles[i]);
            rb.setTextColor(getTextColor());
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint())); // 🚀 QA Visual Fix for FamiSafe Theme
            rb.setId(100 + i); 
            rb.setEnabled(isSelf);
            rgFamilyRole.addView(rb);
        }
        formBox.addView(rgFamilyRole);
        
        TextView labelFriends = new TextView(this);
        labelFriends.setText("🤝 FRIENDS TIER (Max 1)");
        labelFriends.setTextColor(Color.parseColor("#03A9F4")); // Accent Blue
        labelFriends.setPadding(10, 30, 0, 10);
        labelFriends.setTypeface(null, Typeface.BOLD);
        formBox.addView(labelFriends);
        
        rgFriendRole = new android.widget.RadioGroup(this);
        rgFriendRole.setOrientation(LinearLayout.VERTICAL);
        rgFriendRole.setPadding(0, 0, 0, 30);
        String[] friendRoles = {"Friend (Header)", "Friend (Team)"};
        for (int i = 0; i < friendRoles.length; i++) {
            android.widget.RadioButton rb = new android.widget.RadioButton(this);
            rb.setText(friendRoles[i]);
            rb.setTextColor(getTextColor());
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(getWidgetTint())); // 🚀 QA Visual Fix for FamiSafe Theme
            rb.setId(200 + i); 
            rb.setEnabled(isSelf);
            rgFriendRole.addView(rb);
        }
        formBox.addView(rgFriendRole);

        dynamicTaggingContainer = new android.widget.LinearLayout(this);
        dynamicTaggingContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        formBox.addView(dynamicTaggingContainer);
        
        rgFamilyRole.setOnCheckedChangeListener((group, checkedId) -> rebuildTaggingUI());
        rgFriendRole.setOnCheckedChangeListener((group, checkedId) -> rebuildTaggingUI());

        formBox.addView(createSectionHeader("PERSONAL IDENTITY"));
        etFullName = createFormField("Full Legal Name", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_myplaces);
        etDob = createFormField("Date of Birth (DD/MM/YYYY)", InputType.TYPE_CLASS_DATETIME, android.R.drawable.ic_menu_today);
        etGender = createFormField("Legal Gender", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_info_details);
        formBox.addView(etFullName);
        formBox.addView(etDob);
        formBox.addView(etGender);

        formBox.addView(createSectionHeader("EMERGENCY PROTOCOL"));
        etAddress = createFormField("Primary Residence / School Address", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_mapmode);
        etEmergencyNote = createFormField("Emergency Action Protocol", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_dialog_info);
        formBox.addView(etAddress);
        formBox.addView(etEmergencyNote);

        formBox.addView(createSectionHeader("SAFE ZONE (GEOFENCE) RELATIONS"));
        etSafeZoneName = createFormField("Primary Safe Zone (e.g. School)", InputType.TYPE_CLASS_TEXT, android.R.drawable.ic_menu_mylocation);
        etSafeZoneRadius = createFormField("Safe Zone Radius (Meters)", InputType.TYPE_CLASS_NUMBER, android.R.drawable.ic_menu_compass);
        formBox.addView(etSafeZoneName);
        formBox.addView(etSafeZoneRadius);

        // 🚀 PHASE 7 AGILE INJECTION: Enterprise Access Control Mechanisms
        formBox.addView(createSectionHeader("ENTERPRISE ACCESS CONTROL (ACL)"));
        cbAclLiveTracking = createPermissionCheckbox("Continuous Live GPS Sync", "Permits this device to transmit and strictly track Real-Time Coordinates across the network array.");
        cbAclHistoryAudit = createPermissionCheckbox("Historical Path Auditing", "Authorizes remote extraction of localized 24-hour GPS Movement logs.");
        cbAclGeofence = createPermissionCheckbox("Broadcast Security Alerts", "Forces Push Notification events seamlessly to this endpoint when Safe Zones are breached.");
        cbAclAdminStatus = createPermissionCheckbox("Network Administrator", "Elevates read/write capabilities permitting the user to dictate structural overrides globally.");
        cbAclGhostOverride = createPermissionCheckbox("Bypass Ghost Protocols", "Hard-locks this device to report coordinates even if Ghost Mode is selectively toggled (Overwatch Protocol).");
        
        formBox.addView(cbAclLiveTracking);
        formBox.addView(cbAclHistoryAudit);
        formBox.addView(cbAclGeofence);
        formBox.addView(cbAclAdminStatus);
        formBox.addView(cbAclGhostOverride);

        // 🚀 PHASE 8 GLOBAL PRIVACY DIRECTIVE: GDPR Data Minimization
        formBox.addView(createSectionHeader("GLOBAL CLOUD PRIVACY"));
        cbGlobalCloudSync = createPermissionCheckbox("Allow Cloud Synchronization", "If disabled, heavily sensitive Medical and Biometric profiles stay 100% offline physically locally on this specific hardware. Only pure tracking tokens will safely transmit to the Cloud matrix.");
        cbGlobalCloudSync.setChecked(true); // Enterprise Default: Sync all data
        formBox.addView(cbGlobalCloudSync);

        if (!isSelf) {
            // Strict Read-Only Lockdown execution!
            etFullName.setEnabled(false);
            etDob.setEnabled(false);
            etGender.setEnabled(false);
            etAddress.setEnabled(false);
            etEmergencyNote.setEnabled(false);
            etSafeZoneName.setEnabled(false);
            etSafeZoneRadius.setEnabled(false);
            
            cbAclLiveTracking.setEnabled(false);
            cbAclHistoryAudit.setEnabled(false);
            cbAclGeofence.setEnabled(false);
            cbAclAdminStatus.setEnabled(false);
            cbAclGhostOverride.setEnabled(false);
            cbGlobalCloudSync.setEnabled(false);
            
            View syncBtn = createIconButton("REQUEST DEVICE SYNC", android.R.drawable.ic_popup_sync, v -> {
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference()
                    .child("users").child(targetUid).child("force_cloud_sync_trigger")
                    .setValue(System.currentTimeMillis());
                android.widget.Toast.makeText(this, "Sync Pull Request sent to Target Device network edge...", android.widget.Toast.LENGTH_LONG).show();
            });
            formBox.addView(syncBtn);
        } else {
            View saveDossierBtn = createIconButton("Save Profile", android.R.drawable.ic_menu_save, v -> saveCitizenData());
            formBox.addView(saveDossierBtn);
            
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
        
        View cloudInspectBtn = createIconButton("Cloud Data Portal", android.R.drawable.ic_menu_upload, v -> showCloudPrivacyPayload());
        formBox.addView(cloudInspectBtn);
        
        formBox.addView(createIconButton("VIEW ARCHIVED CHANGE LOG", android.R.drawable.ic_menu_recent_history, v -> {
            viewModel.fetchChangeLog(prefs.getString("active_group", null), targetUid);
        }));
        
        formBox.addView(createIconButton("RETURN TO ROSTER", android.R.drawable.ic_menu_revert, v -> finish()));

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

        String fRole = "";
        for (int i = 0; i < rgFamilyRole.getChildCount(); i++) {
            android.view.View c = rgFamilyRole.getChildAt(i);
            if (c instanceof android.widget.RadioButton) {
                android.widget.RadioButton rb = (android.widget.RadioButton) c;
                if (rb.getTag() == null) rb.setTag(rb.getText().toString());
                String baseText = rb.getTag().toString();
                if (c.getId() == rgFamilyRole.getCheckedRadioButtonId()) {
                    fRole = baseText;
                    rb.setText(baseText + " (" + myName + ")");
                } else { rb.setText(baseText); }
            }
        }
        
        String frRole = "";
        for (int i = 0; i < rgFriendRole.getChildCount(); i++) {
            android.view.View c = rgFriendRole.getChildAt(i);
            if (c instanceof android.widget.RadioButton) {
                android.widget.RadioButton rb = (android.widget.RadioButton) c;
                if (rb.getTag() == null) rb.setTag(rb.getText().toString());
                String baseText = rb.getTag().toString();
                if (c.getId() == rgFriendRole.getCheckedRadioButtonId()) {
                    frRole = baseText;
                    rb.setText(baseText + " (" + myName + ")");
                } else { rb.setText(baseText); }
            }
        }

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
        cb.setPadding(20, 20, 20, 20);
        
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
                        localDb.updateCitizenRole(nodeUid, famRole, frRole);
                        
                        String myGrp = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getString("active_group", null);
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
        cb.setPadding(20, 30, 20, 30);
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
                  "The application will encrypt and securely push the entire Citizen Dossier, including Biometrics, Medical Records, Insurance Data, and Identification Marks to the tracking Cloud.";
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
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(10, 30, 0, 20);
        return tv;
    }

    private EditText createFormField(String hint, int inputType, int iconRes) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(getSecondaryTextColor()); // Follows Dynamic Dark Mode Engine!
        et.setTextColor(getTextColor());
        et.setPadding(40, 40, 40, 40);
        et.setTextSize(16);
        et.setInputType(inputType);
        et.setSingleLine(true);
        et.setEnabled(isSelf); // Dynamic Read-Only Lock Protocol
        
        // 🚀 Expand Vector Identity Icons (Themed to User Preferences)
        android.graphics.drawable.Drawable icon = getResources().getDrawable(iconRes, null);
        icon.setTint(getGradientStart()); // Binds directly to the Global Theme Ring!
        et.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        et.setCompoundDrawablePadding(30);
        
        applySolidCardGeometry(et, getCardColor(), 20f);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 25);
        et.setLayoutParams(lp);
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
            etDob.setText(data.get("dob"));
            etGender.setText(data.get("gender"));
            etAddress.setText(data.get("address"));
            etEmergencyNote.setText(data.get("emergencyNote"));
            etSafeZoneName.setText(data.get("safeZoneName"));
            etSafeZoneRadius.setText(data.get("safeZoneRadius"));
            
            // ACL MVVM Matrix Restore
            cbAclLiveTracking.setChecked("GRANTED".equals(data.get("acl_live_tracking")));
            cbAclHistoryAudit.setChecked("GRANTED".equals(data.get("acl_history_audit")));
            cbAclGeofence.setChecked("GRANTED".equals(data.get("acl_geofence")));
            cbAclAdminStatus.setChecked("GRANTED".equals(data.get("acl_admin")));
            cbAclGhostOverride.setChecked("GRANTED".equals(data.get("acl_ghost_override")));
            
            cbGlobalCloudSync.setChecked(!"FALSE".equals(data.get("cloud_sync_enabled"))); // Defaults to TRUE if missing
            
            setRadioRoles(data.get("role_family"), data.get("role_friend"));
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
    
    private void setRadioRoles(String familyStr, String friendStr) {
        if (familyStr != null && !familyStr.isEmpty()) {
            for (int i = 0; i < rgFamilyRole.getChildCount(); i++) {
                View v = rgFamilyRole.getChildAt(i);
                if (v instanceof android.widget.RadioButton) {
                    android.widget.RadioButton rb = (android.widget.RadioButton) v;
                    if (rb.getText().toString().equals(familyStr)) {
                        rgFamilyRole.check(rb.getId());
                        break;
                    }
                }
            }
        }
        if (friendStr != null && !friendStr.isEmpty()) {
            for (int i = 0; i < rgFriendRole.getChildCount(); i++) {
                View v = rgFriendRole.getChildAt(i);
                if (v instanceof android.widget.RadioButton) {
                    android.widget.RadioButton rb = (android.widget.RadioButton) v;
                    if (rb.getText().toString().equals(friendStr)) {
                        rgFriendRole.check(rb.getId());
                        break;
                    }
                }
            }
        }
        rebuildTaggingUI();
    }

    private void saveCitizenData() {
        if (targetUid == null) {
            Toast.makeText(this, "Cannot synchronize Ghost target.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String groupId = prefs.getString("active_group", null);
        if (groupId != null) {
            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("fullName", etFullName.getText().toString());
            payload.put("dob", etDob.getText().toString());
            payload.put("gender", etGender.getText().toString());
            payload.put("address", etAddress.getText().toString());
            payload.put("emergencyNote", etEmergencyNote.getText().toString());
            payload.put("safeZoneName", etSafeZoneName.getText().toString());
            payload.put("safeZoneRadius", etSafeZoneRadius.getText().toString());
            
            // ACL Payload Bundling
            payload.put("acl_live_tracking", cbAclLiveTracking.isChecked() ? "GRANTED" : "DENIED");
            payload.put("acl_history_audit", cbAclHistoryAudit.isChecked() ? "GRANTED" : "DENIED");
            payload.put("acl_geofence", cbAclGeofence.isChecked() ? "GRANTED" : "DENIED");
            payload.put("acl_admin", cbAclAdminStatus.isChecked() ? "GRANTED" : "DENIED");
            payload.put("acl_ghost_override", cbAclGhostOverride.isChecked() ? "GRANTED" : "DENIED");
            
            // Core Cloud Privacy Setting Extraction
            payload.put("cloud_sync_enabled", cbGlobalCloudSync.isChecked() ? "TRUE" : "FALSE");
            
            // Extract actively checked Radio Button Targets
            // Extract actively checked Radio Button Targets executing off the pure Tag constant!
            String selectedFamRole = "";
            int checkedFamId = rgFamilyRole.getCheckedRadioButtonId();
            for (int i = 0; i < rgFamilyRole.getChildCount(); i++) {
                android.view.View child = rgFamilyRole.getChildAt(i);
                if (child.getId() == checkedFamId && child instanceof android.widget.RadioButton) {
                    Object tag = child.getTag();
                    selectedFamRole = tag != null ? tag.toString() : ((android.widget.RadioButton) child).getText().toString().replaceAll(" \\(.*\\)", "");
                    break;
                }
            }
            payload.put("role_family", selectedFamRole);
            
            String selectedFriendRole = "";
            int checkedFriendId = rgFriendRole.getCheckedRadioButtonId();
            for (int i = 0; i < rgFriendRole.getChildCount(); i++) {
                android.view.View child = rgFriendRole.getChildAt(i);
                if (child.getId() == checkedFriendId && child instanceof android.widget.RadioButton) {
                    Object tag = child.getTag();
                    selectedFriendRole = tag != null ? tag.toString() : ((android.widget.RadioButton) child).getText().toString().replaceAll(" \\(.*\\)", "");
                    break;
                }
            }
            payload.put("role_friend", selectedFriendRole);
            
            // 🚀 MVVM Architecture: Route payload down into exactly one Repository ViewModel layer!
            viewModel.saveDossier(this, prefs.getString("active_group", null), targetUid, payload);
        } else {
             Toast.makeText(this, "No Active Master Group found. Save Offline Only.", Toast.LENGTH_SHORT).show();
        }
    }
}
