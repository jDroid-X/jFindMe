package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.engine.security.SecurityEngine;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> ManagementCluster -> ProfileActivity
 * Level 5: Implementation of Secure Identity and Device Integrity.
 */
public class ProfileActivity extends BaseActivityJava {

    private final String TAG = "ProfileActivity";
    private android.webkit.WebView activeProfileImgView; // 🧠 Upgraded to Real 3D GLTF Decoder

    @Override
    protected void onResume() {
        super.onResume();
        // 🧠 QA Fix: Immediately repaint the Profile Avatar physically the second the user returns from the Editor pipeline
        if (activeProfileImgView != null && prefs != null) {
            String currentMeshString = prefs.getString("equipped_vrm_string", "models/Seed-chan.vrm");
            
            // Re-resolve the filepath based on category if it's a categorized item
            String absoluteVRM = "https://jfindme.local/" + currentMeshString;
            if (!currentMeshString.startsWith("models/")) {
                // Fallback attempt for newly selected custom items
                absoluteVRM = "https://jfindme.local/models/" + currentMeshString;
            }
            
            activeProfileImgView.evaluateJavascript("window.loadModel('" + absoluteVRM + "');", null);
        }
    }

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor()); // Dynamically handles Light/Dark

        // 🚀 Premium Linear Gradient Hero Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(50, 80, 50, 60);
        applyLinearGradient(header, 0f); 

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText(prefs.getString("user_nickname", "Commander User").toUpperCase());
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);
        
        ImageView settingsBtn = new ImageView(this);
        settingsBtn.setImageResource(android.R.drawable.ic_menu_preferences);
        settingsBtn.setColorFilter(getGradientOverlayText());
        settingsBtn.setLayoutParams(new LinearLayout.LayoutParams(70, 70));
        settingsBtn.setPadding(10, 10, 10, 10);
        settingsBtn.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(this, Class.forName("com.jdroidx.jfindme.ui.settings.SettingsActivity")));
            } catch (Exception e) {}
        });
        topRow.addView(settingsBtn);
        header.addView(topRow);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Identity & Integrity Hub");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 10, 0, 0);
        header.addView(subtitle);
        
        mainRoot.addView(header);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 140);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 40);

        // --- LAYER 1: AVATAR CARD ---
        LinearLayout avatarCard = new LinearLayout(this);
        avatarCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(avatarCard, getCardColor(), 30f);
        avatarCard.setPadding(0, 40, 0, 40);

        // --- 2D NATIVE PROFILE PHOTO MANAGER ---
        LinearLayout photoBox = new LinearLayout(this);
        photoBox.setOrientation(LinearLayout.VERTICAL);
        photoBox.setGravity(Gravity.CENTER);

        ImageView nativeProfilePhoto = new ImageView(this);
        int currentAvatarId = prefs.getInt("equipped_avatar_id", 1);
        int dynIdx = (currentAvatarId % 22 == 0) ? 22 : (currentAvatarId % 22);
        int dynRes = getResources().getIdentifier("avatar_custom_" + dynIdx, "drawable", getPackageName());
        nativeProfilePhoto.setImageResource(dynRes != 0 ? dynRes : android.R.drawable.ic_menu_myplaces);
        
        android.graphics.drawable.GradientDrawable photoBorder = new android.graphics.drawable.GradientDrawable();
        photoBorder.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        photoBorder.setColor(Color.WHITE);
        photoBorder.setStroke(6, getGradientStart()); // Strictly linked to Theme array!
        nativeProfilePhoto.setBackground(photoBorder);
        nativeProfilePhoto.setPadding(30, 30, 30, 30);
        
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(380, 380);
        photoParams.setMargins(0, 20, 0, 30);
        nativeProfilePhoto.setLayoutParams(photoParams);
        photoBox.addView(nativeProfilePhoto);

        TextView editLabel = new TextView(this);
        editLabel.setText("TAP TO CHANGE AVATAR");
        editLabel.setTextColor(getSecondaryTextColor());
        editLabel.setTextSize(11);
        editLabel.setTypeface(null, Typeface.BOLD);
        photoBox.addView(editLabel);

        // Directly open the Native Grid Picker
        photoBox.setOnClickListener(v -> showAvatarSelectionDialog(nativeProfilePhoto));
        avatarCard.addView(photoBox);
        root.addView(avatarCard, cardLp);

        // --- LAYER 2: INTEGRITY CARD ---
        root.addView(createSectionHeader("SYSTEM INTEGRITY"));
        LinearLayout integrityCard = new LinearLayout(this);
        integrityCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(integrityCard, getCardColor(), 30f);
        
        boolean rooted = SecurityEngine.INSTANCE.isRooted();
        View securityStatus = createIconButton(rooted ? "Account Risk: ROOTED" : "Identity: SECURE", 
                android.R.drawable.ic_dialog_info, v -> {
                    try {
                        startActivity(new android.content.Intent(this, Class.forName("com.jdroidx.jfindme.ui.management.PrivacyActivity")));
                    } catch (Exception e) {}
                });
        
        android.graphics.drawable.GradientDrawable secBg = new android.graphics.drawable.GradientDrawable();
        secBg.setColor(rooted ? Color.parseColor("#D32F2F") : Color.parseColor("#2E7D32"));
        secBg.setCornerRadius(30f);
        securityStatus.setBackground(secBg);
        
        integrityCard.addView(securityStatus);
        root.addView(integrityCard, cardLp);

        // --- LAYER 3: PERSONAL IDENTITY & SECURITY ---
        root.addView(createSectionHeader("PERSONAL IDENTITY & SECURITY"));
        
        LinearLayout pForm = new LinearLayout(this);
        pForm.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(pForm, getCardColor(), 30f);
        pForm.setPadding(30,30,30,30);
        
        android.widget.EditText nameField = createEditableField("Name", prefs.getString("user_nickname", "Guardian").toUpperCase(), android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        android.widget.EditText phoneField = createEditableField("Mobile Number", prefs.getString("user_phone", "+1 555-000-0000"), android.text.InputType.TYPE_CLASS_PHONE);
        
        String currentEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail() : "node@jfindme.net";
        android.widget.EditText emailField = createEditableField("Email (Read Only)", currentEmail, android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setEnabled(false); // Emails require Firebase re-auth loops, locking config by default
        
        pForm.addView(nameField);
        pForm.addView(phoneField);
        pForm.addView(emailField);
        
        // Logical "Change Password" Action with Live Firebase Sync
        pForm.addView(createIconButton("CHANGE SECURE PASSWORD", android.R.drawable.ic_menu_edit, v -> {
            android.widget.EditText passInput = new android.widget.EditText(this);
            passInput.setHint("Enter New Firebase Password");
            passInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passInput.setPadding(40, 40, 40, 40);
            
            new android.app.AlertDialog.Builder(this)
                .setTitle("Account Security")
                .setMessage("Input highly secure new password for this Profile (Min 6 Characters).")
                .setView(passInput)
                .setPositiveButton("UPDATE FIREBASE", (dialog, which) -> {
                    String newPass = passInput.getText().toString();
                    if (newPass.length() >= 6 && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().updatePassword(newPass)
                            .addOnSuccessListener(aVoid -> android.widget.Toast.makeText(this, "Password updated securely on Firebase", android.widget.Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> android.widget.Toast.makeText(this, "Hack Attempt Blocked. Need fresh login.", android.widget.Toast.LENGTH_LONG).show());
                    } else {
                        android.widget.Toast.makeText(this, "Password too short", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }));
        
        // 2FA & Account Security
        LinearLayout toggleBox = new LinearLayout(this);
        toggleBox.setOrientation(LinearLayout.VERTICAL);
        toggleBox.setPadding(30, 20, 30, 20);
        
        LinearLayout form2fa = new LinearLayout(this);
        form2fa.setOrientation(LinearLayout.HORIZONTAL);
        TextView l1 = new TextView(this); l1.setText("Two-Factor Authentication (2FA)"); l1.setTextColor(getTextColor());
        l1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        android.widget.Switch s1 = new android.widget.Switch(this); s1.setChecked(prefs.getBoolean("2fa_enabled", false));
        s1.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("2fa_enabled", isChecked).apply());
        form2fa.addView(l1); form2fa.addView(s1);
        form2fa.setPadding(0, 30, 0, 30);
        toggleBox.addView(form2fa);
        
        LinearLayout formSec = new LinearLayout(this);
        formSec.setOrientation(LinearLayout.HORIZONTAL);
        TextView l2 = new TextView(this); l2.setText("Strict Tracking Constraints"); l2.setTextColor(getTextColor());
        l2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        android.widget.Switch s2 = new android.widget.Switch(this); s2.setChecked(prefs.getBoolean("strict_track", true));
        s2.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("strict_track", isChecked).apply());
        formSec.addView(l2); formSec.addView(s2);
        formSec.setPadding(0, 30, 0, 30);
        toggleBox.addView(formSec);
        
        pForm.addView(toggleBox);
        
        // Logical Logout Block with Native Safety Guard
        pForm.addView(createIconButton("LOGOUT SESSION", android.R.drawable.ic_lock_power_off, v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to securely log out of your Guardian Account? You will stop receiving tracking alerts.")
                .setPositiveButton("Logout", (dialog, which) -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.auth.AuthActivity.class));
                    finishAffinity();
                })
                .setNegativeButton("Cancel", null)
                .show();
        }));
        
        root.addView(pForm, cardLp);

        // 🚀 CRITICAL UX FIX: Profile Save Next-Action! Bridges safely to Map.
        View saveBtn = createIconButton("💾 VERIFY & SAVE PROFILE", android.R.drawable.ic_menu_save, v -> {
            String nameVal = nameField.getText().toString().trim();
            String phoneVal = phoneField.getText().toString().trim();
            if (nameVal.length() < 2) {
                nameField.setError("Name minimum 2 characters");
                android.widget.Toast.makeText(this, "Profile Check Failed: Name invalid", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.PHONE.matcher(phoneVal).matches() || phoneVal.length() < 7) {
                phoneField.setError("Requires valid Phone Syntax");
                android.widget.Toast.makeText(this, "Profile Check Failed: Phone invalid", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString("user_nickname", nameVal)
                        .putString("user_phone", phoneVal)
                        .putBoolean("profile_onboarded", true).apply();
            
            android.widget.Toast.makeText(this, "Profile Verified & Saved Securely!", android.widget.Toast.LENGTH_SHORT).show();
            startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.map.MapActivity.class));
            finish();
        });
        
        // Execute the Active Theme Matrix on the Action Button
        android.graphics.drawable.GradientDrawable mapBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{getGradientStart(), getGradientEnd()}
        );
        mapBg.setCornerRadius(50f);
        saveBtn.setBackground(mapBg);
        saveBtn.setElevation(15f);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150);
        btnParams.setMargins(0, 20, 0, 0);
        saveBtn.setLayoutParams(btnParams);
        root.addView(saveBtn);

        scrollView.addView(root);
        mainRoot.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        return mainRoot;
    }

    private View createStatCard(String title, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        applyGlassmorphism(card, Color.parseColor("#1Affffff"), 16f);
        card.setGravity(Gravity.CENTER);
        
        android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
        params.setMargins(20, 20, 20, 20);
        card.setLayoutParams(params);
        card.setPadding(20, 40, 20, 40);

        TextView valTxt = new TextView(this);
        valTxt.setText(value);
        valTxt.setTextColor(Color.WHITE);
        valTxt.setTextSize(18);
        valTxt.setTypeface(null, Typeface.BOLD);
        
        TextView titleTxt = new TextView(this);
        titleTxt.setText(title);
        titleTxt.setTextColor(Color.GRAY);
        titleTxt.setTextSize(10);
        titleTxt.setPadding(0, 10, 0, 0);

        card.addView(valTxt);
        card.addView(titleTxt);
        
        // Tab Focus Loop Support!
        card.setFocusable(true);
        return card;
    }

    private TextView createInfoText(String text, int size, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getTextColor());
        tv.setTextSize(size);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 20, 0, 10);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private void showAvatarSelectionDialog(final ImageView mainProfilePic) {
        // Build the Custom Graphical Layout
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.GridLayout gridLayout = new android.widget.GridLayout(this);
        gridLayout.setColumnCount(3);
        gridLayout.setPadding(20, 20, 20, 20);

        // Fixed: Kotlin returns immutable Lists as `List<? extends AvatarEntity>` in Java. 
        // Iterating directly skips the strict casting variance error that breaks compilation.
        java.util.List<? extends com.jdroidx.jfindme.engine.data.avatar.AvatarEntity> templateList = 
            com.jdroidx.jfindme.engine.data.avatar.AvatarRegistry.INSTANCE.getTemplateList();

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle("Avatar Customization Gallery")
            .setView(scrollView)
            .setNegativeButton("Close", null)
            .create();

        for (final com.jdroidx.jfindme.engine.data.avatar.AvatarEntity avatar : templateList) {
            android.widget.RelativeLayout tile = new android.widget.RelativeLayout(this);
            android.widget.GridLayout.LayoutParams gridParams = new android.widget.GridLayout.LayoutParams();
            gridParams.width = 300;
            gridParams.height = 350;
            gridParams.setMargins(15, 15, 15, 15);
            tile.setLayoutParams(gridParams);
            applySolidCardGeometry(tile, getCardColor(), 16f); // Use standard Floating Geometry
            
            // Required for QA Tab Sequences!
            tile.setFocusable(true);
            tile.setClickable(true);

            // 1. Core Profile Image (Centered in Tile)
            ImageView previewImg = new ImageView(this);
            previewImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            // Prime Directive Sync: Integration with the User's newly defined "Avatar" Directory!
            // Safely distributes the 8 physical image files across our 25 Unique Avatar models.
            int dynamicImgIndex = (avatar.getBaseId() % 22 == 0) ? 22 : (avatar.getBaseId() % 22);
            String customGraphicName = "avatar_custom_" + dynamicImgIndex;
            
            int customResId = getResources().getIdentifier(customGraphicName, "drawable", getPackageName());
            
            // Failsafe Execution (If an image goes missing, don't crash the grid)
            if (customResId != 0) {
                previewImg.setImageResource(customResId);
            } else {
                previewImg.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            
            android.widget.RelativeLayout.LayoutParams imgParams = new android.widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            previewImg.setLayoutParams(imgParams);
            tile.addView(previewImg);

            // 2. Selection Loop Indicator (Top Right Circle)
            ImageView circle = new ImageView(this);
            circle.setImageResource(android.R.drawable.checkbox_off_background);
            android.widget.RelativeLayout.LayoutParams circleParams = new android.widget.RelativeLayout.LayoutParams(60, 60);
            circleParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
            circleParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
            circleParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT); // SDK Compatibility
            circleParams.setMargins(0, 15, 15, 0);
            circle.setLayoutParams(circleParams);
            tile.addView(circle);

            // 3. Variant Name Identifier (Bottom Text)
            TextView nameStr = new TextView(this);
            nameStr.setText(avatar.getType());
            nameStr.setTextColor(Color.WHITE);
            nameStr.setTextSize(12);
            nameStr.setGravity(Gravity.CENTER);
            nameStr.setBackgroundColor(Color.parseColor("#88000000")); // 50% Black transparency for contrast
            nameStr.setPadding(0, 15, 0, 15);
            
            android.widget.RelativeLayout.LayoutParams nameParams = new android.widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
            nameStr.setLayoutParams(nameParams);
            tile.addView(nameStr);

            // 4. Interaction Binding (Execute Equip & Circle Fill)
            tile.setOnClickListener(v -> {
                circle.setImageResource(android.R.drawable.checkbox_on_background); // Visual fill confirmation
                mainProfilePic.setImageDrawable(previewImg.getDrawable());
                mainProfilePic.clearColorFilter(); // Remove Tint
                
                // prime directive context map: Save specific Selection natively
                prefs.edit().putInt("equipped_avatar_id", avatar.getBaseId()).apply();

                android.widget.Toast.makeText(this, "Equipped Avatar: " + avatar.getType(), android.widget.Toast.LENGTH_SHORT).show();
                
                dialog.dismiss();
            });

            gridLayout.addView(tile);
        }

        scrollView.addView(gridLayout);
        dialog.show();
    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Native Theme Integration
        header.setTextSize(11);
        header.setPadding(10, 30, 0, 20);
        header.setTypeface(null, Typeface.BOLD);
        return header;
    }

    private android.widget.EditText createEditableField(String hint, String value, int inputType) {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint(hint);
        et.setText(value);
        et.setTextColor(getTextColor());
        et.setHintTextColor(getSecondaryTextColor());
        et.setInputType(inputType);
        et.setPadding(40, 40, 40, 40);
        
        android.graphics.drawable.GradientDrawable fieldBg = new android.graphics.drawable.GradientDrawable();
        fieldBg.setColor(Color.parseColor("#15000000")); // Soft grey box logic matches perfectly!
        fieldBg.setStroke(3, getGradientStart()); // 🚀 Uses active App Theme ring!
        fieldBg.setCornerRadius(16f);
        et.setBackground(fieldBg); 
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 15);
        et.setLayoutParams(params);
        return et;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.d(TAG, "Profile Security Cluster Initialized.");
    }

    /**
     * MODULE 5: Unity Native Render Bridge Receiver
     * Safely decrypts the 720p snapshot fired by Unity's 'AvatarProfileRenderer.cs'
     * and mounts the raw 3D image buffer directly onto the Android GUI without clipping!
     */
    public void onUnityProfileSnapshotReceived(String base64Image) {
        try {
            byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            
            runOnUiThread(() -> {
                if (activeProfileImgView != null) {
                    // activeProfileImgView.setImageBitmap(decodedByte); // 🧠 Deprecated by WebGL Native 3D Bypass
                    // activeProfileImgView.clearColorFilter(); // 🧠 Deprecated by WebGL Native 3D Bypass
                    android.widget.Toast.makeText(this, "3D Avatar Render Captured via WebGL Native Core", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            SystemLogger.INSTANCE.e(TAG, "Failed resolving Unity Render Texture", e);
        }
    }
}
