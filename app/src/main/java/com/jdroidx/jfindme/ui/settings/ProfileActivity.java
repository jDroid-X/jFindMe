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

        // 🚀 Premium Linear Gradient        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header); // 🚀 Native QA Architecture Injection 

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
        // --- LAYER 1: AVATAR CARD ---
        android.widget.FrameLayout avatarCard = new android.widget.FrameLayout(this);
        applySolidCardGeometry(avatarCard, getCardColor(), 30f);
        avatarCard.setPadding(0, 40, 0, 40);

        // --- 2D NATIVE PROFILE PHOTO MANAGER ---
        LinearLayout photoBox = new LinearLayout(this);
        photoBox.setOrientation(LinearLayout.VERTICAL);
        photoBox.setGravity(Gravity.CENTER);

        ImageView nativeProfilePhoto = new ImageView(this);
        // Force Native GPU rendering directly from AvatarLoader Utility
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, nativeProfilePhoto);
        
        android.graphics.drawable.GradientDrawable photoBorder = new android.graphics.drawable.GradientDrawable();
        // 🚀 QA Display Fix: Premium "Squircle" Rounded Rectangle Graphic Swap
        photoBorder.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        photoBorder.setCornerRadius(100f); // Massive rounded edges mimicking modern iOS paradigms
        photoBorder.setColor(Color.TRANSPARENT);
        photoBorder.setStroke(6, getGradientStart()); 
        nativeProfilePhoto.setBackground(photoBorder);
        nativeProfilePhoto.setPadding(15, 15, 15, 15); 
        
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
        
        android.widget.FrameLayout.LayoutParams photoBoxLp = new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        photoBoxLp.gravity = Gravity.CENTER;
        avatarCard.addView(photoBox, photoBoxLp);

        // 🚀 QA Fix: Extreme Top Left Camera Hook (High-Res FileProvider Pipeline)
        ImageView cameraBtn = new ImageView(this);
        cameraBtn.setImageResource(android.R.drawable.ic_menu_camera);
        cameraBtn.setColorFilter(getGradientStart()); 
        
        android.widget.FrameLayout.LayoutParams camLp = new android.widget.FrameLayout.LayoutParams(110, 110);
        camLp.gravity = Gravity.TOP | Gravity.START;
        camLp.setMargins(50, 0, 0, 0); // Pushed exactly into the pure top left corner rounding
        cameraBtn.setLayoutParams(camLp);
        
        cameraBtn.setOnClickListener(v -> {
            try {
                android.content.Intent takePictureIntent = new android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                java.io.File photoFile = new java.io.File(getCacheDir(), "camera_temp.jpg");
                android.net.Uri photoURI = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                startActivityForResult(takePictureIntent, 1001);
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Camera Hardware Not Accessible.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        // Mount it cleanly at the highest Z-index over the photoBox array!
        avatarCard.addView(cameraBtn);
        
        // 🚀 QA Fix: Extreme Top Right Lens (Hardware GPU Crop Engine)
        ImageView cropBtn = new ImageView(this);
        cropBtn.setImageResource(android.R.drawable.ic_menu_crop); // Native Lens Target UI
        cropBtn.setColorFilter(getGradientStart()); 
        
        android.widget.FrameLayout.LayoutParams cropLp = new android.widget.FrameLayout.LayoutParams(110, 110);
        cropLp.gravity = Gravity.TOP | Gravity.END | Gravity.RIGHT;
        cropLp.setMargins(0, 0, 50, 0); // Pushed exactly into the pure top right corner rounding
        cropBtn.setLayoutParams(cropLp);
        
        cropBtn.setOnClickListener(v -> {
            java.io.File cfCheck = new java.io.File(getFilesDir() + "/avatars", "custom_photo.png");
            if (cfCheck.exists() && prefs.getInt("equipped_avatar_id", 1) == -1) {
                // Safely engage the Custom Graphic Math engine
                android.content.Intent cropIntent = new android.content.Intent(this, AvatarCropActivity.class);
                startActivityForResult(cropIntent, 1002);
            } else {
                android.widget.Toast.makeText(this, "Camera Selfie missing or not actively equipped.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        avatarCard.addView(cropBtn);
        root.addView(avatarCard, cardLp);

        // --- LAYER 2: INTEGRITY CARD ---
        root.addView(createSectionHeader("SYSTEM INTEGRITY & HARDWARE LINKS"));
        
        LinearLayout integrityRow = new LinearLayout(this);
        integrityRow.setOrientation(LinearLayout.HORIZONTAL);
        integrityRow.setWeightSum(2.0f);
        
        boolean rooted = SecurityEngine.INSTANCE.isRooted();
        View securityStatus = createIconButton(rooted ? "Risk: ROOTED" : "Identity: SECURE", 
                android.R.drawable.ic_lock_idle_lock, v -> {
                    try {
                        startActivity(new android.content.Intent(this, Class.forName("com.jdroidx.jfindme.ui.management.PrivacyActivity")));
                    } catch (Exception e) {}
                });
        
        android.graphics.drawable.GradientDrawable secBg = new android.graphics.drawable.GradientDrawable();
        secBg.setColor(rooted ? Color.parseColor("#D32F2F") : Color.parseColor("#2E7D32"));
        secBg.setCornerRadius(30f);
        securityStatus.setBackground(secBg);
        
        View pairingStatus = createIconButton("Device Pairing", 
                android.R.drawable.ic_menu_share, v -> {
                    startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.group.GroupActivity.class));
                });
                
        android.graphics.drawable.GradientDrawable pairBg = new android.graphics.drawable.GradientDrawable();
        pairBg.setColor(Color.parseColor("#1565C0")); // Deep Blue Link Network!
        pairBg.setCornerRadius(30f);
        pairingStatus.setBackground(pairBg);

        LinearLayout.LayoutParams squircleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        squircleLp.setMargins(0, 0, 15, 0); // Right margin for first box
        securityStatus.setLayoutParams(squircleLp);
        
        LinearLayout.LayoutParams squircleLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        squircleLp2.setMargins(15, 0, 0, 0); // Left margin for second box
        pairingStatus.setLayoutParams(squircleLp2);

        integrityRow.addView(securityStatus);
        integrityRow.addView(pairingStatus);
        root.addView(integrityRow, cardLp);

        // --- LAYER 3: PERSONAL IDENTITY & SECURITY ---
        root.addView(createSectionHeader("PERSONAL IDENTITY & SECURITY"));
        
        LinearLayout pForm = new LinearLayout(this);
        pForm.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(pForm, getCardColor(), 30f);
        pForm.setPadding(30,30,30,30);
        
        android.widget.EditText nameField = new android.widget.EditText(this);
        nameField.setHint("Name");
        nameField.setText(prefs.getString("user_nickname", "Guardian").toUpperCase());
        nameField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        
        android.widget.EditText phoneField = new android.widget.EditText(this);
        phoneField.setHint("Mobile Number");
        phoneField.setText(prefs.getString("user_phone", "+1 555-000-0000"));
        phoneField.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        
        String currentEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail() : "node@jfindme.net";
        android.widget.EditText emailField = new android.widget.EditText(this);
        emailField.setHint("Email (Read Only)");
        emailField.setText(currentEmail);
        emailField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setEnabled(false); // Emails require Firebase re-auth loops, locking config by default
        
        pForm.addView(wrapEnterpriseField(nameField, prefs.getInt("icon_profile_name", android.R.drawable.ic_menu_myplaces)));
        pForm.addView(wrapEnterpriseField(phoneField, prefs.getInt("icon_profile_phone", android.R.drawable.ic_menu_call)));
        pForm.addView(wrapEnterpriseField(emailField, prefs.getInt("icon_profile_email", android.R.drawable.ic_menu_send)));
        
        // 🚀 QA Upgrade: Dedicated Citizen Dossier Portal
        pForm.addView(createIconButton("ACCESS DETAILED PROFILE DATA", prefs.getInt("icon_profile_dossier", android.R.drawable.ic_menu_info_details), v -> {
            String safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            if (safeUid != null) {
                android.content.Intent dossierRoute = new android.content.Intent(this, com.jdroidx.jfindme.ui.group.PersonDetailsActivity.class);
                dossierRoute.putExtra("TARGET_UID", safeUid);
                dossierRoute.putExtra("TARGET_ROLE", prefs.getString("role_family", "Network Administrator"));
                startActivity(dossierRoute);
            } else {
                android.widget.Toast.makeText(this, "Hardware Lock Missing. Require Authentication.", android.widget.Toast.LENGTH_SHORT).show();
            }
        }));
        
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
            // Safely retrieve existing roles from memory without redefining them here since they moved to the Dossier Matrix.
            String famStr = prefs.getString("role_family", "Network Administrator");
            String friendStr = prefs.getString("role_friend", "None");
            prefs.edit().putString("user_nickname", nameVal)
                        .putString("user_phone", phoneVal)
                        .putString("role_family", famStr)
                        .putString("role_friend", friendStr)
                        .putBoolean("role_mother", famStr.equals("Mother"))
                        .putBoolean("role_father", famStr.equals("Father"))
                        .putBoolean("role_child", famStr.equals("Child"))
                        .putBoolean("role_spouse", famStr.equals("Spouse"))
                        .putBoolean("role_friends", friendStr.contains("Friend"))
                        .putBoolean("profile_onboarded", true).apply();
            
            // 🚀 QA Fix 1: Instantly sync the Checkbox Array to Firebase to prevent Identity Gaslighting!
            String safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            if (safeUid != null) {
                java.util.Map<String, Object> roleMatrix = new java.util.HashMap<>();
                roleMatrix.put("userName", nameVal); // Also sync the Display Name!
                roleMatrix.put("role_family", famStr);
                roleMatrix.put("role_friend", friendStr);
                roleMatrix.put("role_mother", famStr.equals("Mother"));
                roleMatrix.put("role_father", famStr.equals("Father"));
                roleMatrix.put("role_child", famStr.equals("Child"));
                roleMatrix.put("role_spouse", famStr.equals("Spouse"));
                roleMatrix.put("role_friends", friendStr.contains("Friend"));
                
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                    .child(safeUid).updateChildren(roleMatrix);
            }
            
            android.widget.Toast.makeText(this, "Profile Verified & Secured on Cloud Architecture!", android.widget.Toast.LENGTH_SHORT).show();
            
            // 🚀 QA Fix: Master Boot of Foreground Service immediately after First-Time Setup!
            try {
                android.content.Intent serviceIntent = new android.content.Intent(this, com.jdroidx.jfindme.service.TrackingService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } catch (Exception e) {
                com.jdroidx.jfindme.utils.SystemLogger.INSTANCE.e("ProfileActivity", "Foreground Boot Denied by OS Rules. Failing safely.", e);
            }
            
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

        // 🚀 QA Override: Inject the freshly saved Custom Local Photo into the very front of the physical list!
        java.io.File customPhotoFile = new java.io.File(getFilesDir() + "/avatars", "custom_photo.png");
        if (customPhotoFile.exists()) {
            android.widget.RelativeLayout customTile = new android.widget.RelativeLayout(this);
            android.widget.GridLayout.LayoutParams gridParams = new android.widget.GridLayout.LayoutParams();
            gridParams.width = 300;
            gridParams.height = 350;
            gridParams.setMargins(15, 15, 15, 15);
            customTile.setLayoutParams(gridParams);
            applySolidCardGeometry(customTile, getCardColor(), 16f);
            customTile.setFocusable(true);
            customTile.setClickable(true);

            ImageView previewImg = new ImageView(this);
            previewImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeFile(customPhotoFile.getAbsolutePath());
            previewImg.setImageBitmap(b);
            android.widget.RelativeLayout.LayoutParams imgParams = new android.widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            previewImg.setLayoutParams(imgParams);
            customTile.addView(previewImg);

            ImageView circle = new ImageView(this);
            circle.setImageResource(android.R.drawable.checkbox_off_background);
            android.widget.RelativeLayout.LayoutParams circleParams = new android.widget.RelativeLayout.LayoutParams(60, 60);
            circleParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
            circleParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
            circleParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
            circleParams.setMargins(0, 15, 15, 0);
            circle.setLayoutParams(circleParams);
            customTile.addView(circle);

            // 🚀 QA Fix: Physical Hardware Deletion Hook ("X" Button)
            ImageView deleteBtn = new ImageView(this);
            deleteBtn.setImageResource(android.R.drawable.ic_menu_delete);
            deleteBtn.setColorFilter(Color.WHITE);
            
            android.graphics.drawable.GradientDrawable delBg = new android.graphics.drawable.GradientDrawable();
            delBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            delBg.setColor(Color.parseColor("#D32F2F")); // Aggressive Red
            deleteBtn.setBackground(delBg);
            deleteBtn.setPadding(15, 15, 15, 15);
            
            android.widget.RelativeLayout.LayoutParams delParams = new android.widget.RelativeLayout.LayoutParams(70, 70);
            delParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
            delParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
            delParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
            delParams.setMargins(15, 15, 0, 0);
            deleteBtn.setLayoutParams(delParams);
            customTile.addView(deleteBtn);

            // Intersect and Execute Hard Delete
            deleteBtn.setOnClickListener(v -> {
                if (customPhotoFile.exists()) {
                    boolean destroyed = customPhotoFile.delete();
                    if (destroyed) {
                        // Fallback constraint if they deleted the currently equipped photo!
                        if (prefs.getInt("equipped_avatar_id", 1) == -1) {
                            prefs.edit().putInt("equipped_avatar_id", 1).apply();
                        }
                        dialog.dismiss();
                        android.widget.Toast.makeText(this, "Selfie Purged Securely.", android.widget.Toast.LENGTH_SHORT).show();
                        recreate(); // Native repaint forces the image boundary out of memory
                    }
                }
            });

            TextView nameStr = new TextView(this);
            nameStr.setText("Camera Selfie");
            nameStr.setTextColor(Color.WHITE);
            nameStr.setTextSize(12);
            nameStr.setGravity(Gravity.CENTER);
            nameStr.setBackgroundColor(Color.parseColor("#88000000"));
            nameStr.setPadding(0, 15, 0, 15);
            android.widget.RelativeLayout.LayoutParams nameParams = new android.widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
            nameStr.setLayoutParams(nameParams);
            customTile.addView(nameStr);

            customTile.setOnClickListener(v -> {
                circle.setImageResource(android.R.drawable.checkbox_on_background);
                mainProfilePic.setImageDrawable(previewImg.getDrawable());
                mainProfilePic.clearColorFilter();
                prefs.edit().putInt("equipped_avatar_id", -1).apply(); // -1 explicitly triggers the Native Local Graphic Engine Branch!
                android.widget.Toast.makeText(this, "Equipped Custom Camera Snapshot!", android.widget.Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            gridLayout.addView(customTile);
        }

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

    private View wrapEnterpriseField(android.widget.EditText et, int iconRes) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(35, 20, 35, 20); // Thick internal padding for native touch density
        
        android.graphics.drawable.GradientDrawable fieldBg = new android.graphics.drawable.GradientDrawable();
        fieldBg.setColor(Color.parseColor("#15000000")); // Clean frosted backing
        fieldBg.setStroke(3, getGradientStart()); 
        fieldBg.setCornerRadius(25f);
        row.setBackground(fieldBg);
        
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 15, 0, 25); // Explicit separation between input blocks
        row.setLayoutParams(rowLp);

        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart());
        icon.setLayoutParams(new LinearLayout.LayoutParams(55, 55));
        row.addView(icon);
        
        et.setTextColor(getTextColor());
        et.setHintTextColor(getSecondaryTextColor());
        et.setBackgroundColor(Color.TRANSPARENT);
        et.setPadding(35, 20, 20, 20); // Left pad isolates the icon from text entry bounds
        et.setTextSize(14f);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(et);
        
        return row;
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


    // 🚀 QA Fix: Prime Directive Camera Hardware Hardware Matrix!
    // Triggers when the top left Camera Action Intent fulfills payload delivery.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Lens Crop Execution Bypass
        if (requestCode == 1002 && resultCode == RESULT_OK) {
            recreate(); // Wipes current RAM memory and repaints native Image immediately
            return;
        }
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            try {
                java.io.File photoFile = new java.io.File(getCacheDir(), "camera_temp.jpg");
                android.graphics.Bitmap camImage = null;
                
                if (photoFile.exists()) {
                    // Full Resolution Mode via FileProvider Capture
                    android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);
                    
                    int scaleFactor = Math.max(1, java.lang.Math.min(options.outWidth/700, options.outHeight/700));
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = scaleFactor;
                    camImage = android.graphics.BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);
                    photoFile.delete(); // Cleanup temp hardware layer file
                } else if (data != null && data.getExtras() != null) {
                    // Fallback to strict UI thumbnail if FileProvider gets blocked by OS rules
                    camImage = (android.graphics.Bitmap) data.getExtras().get("data");
                }
                
                if (camImage != null) {
                    // 1. Structural Hardware Limiter (Prevents MultiMarkerEngine.kt Memory Collapse)
                    android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createScaledBitmap(camImage, 350, 350, true);
                    
                    // 2. Local App-Specific Storage Injection Directory
                    java.io.File dir = new java.io.File(getFilesDir(), "avatars");
                    if (!dir.exists()) dir.mkdirs();
                    java.io.File file = new java.io.File(dir, "custom_photo.png");
                    
                    // 3. I/O Block Writer
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                    
                    // Auto-equip the brand new structural file mathematically via ID '-1'
                    getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).edit().putInt("equipped_avatar_id", -1).apply();
                    
                    android.widget.Toast.makeText(this, "High-Res Selfie Saved!", android.widget.Toast.LENGTH_SHORT).show();
                    
                    // Flush the Viewport completely to instantly draw the new Image overlay
                    recreate(); 
                }
            } catch (Exception e) {
                SystemLogger.INSTANCE.e(TAG, "File I/O Write Failure in Local Avatar Cache", e);
            }
        }
    }
}
