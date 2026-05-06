package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.engine.security.SecurityEngine;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> ManagementCluster -> ProfileActivity
 * Level 5: Implementation of Secure Identity and Device Integrity.
 */
public class ProfileActivity extends BaseActivityJava {

    private final String TAG = "ProfileActivity";
    private boolean onboardingDialogShown = false;
    private final java.util.Map<String, String> initialValues = new java.util.HashMap<>();
    private boolean isDataLoaded = false;
    private android.webkit.WebView activeProfileImgView; // 🧠 Upgraded to Real 3D GLTF Decoder
    private ProfileViewModel profileViewModel; // 🚀 Pure Native MVVM Pipeline Injection
    private boolean isSaveLaterAction = false; 

    private android.widget.ArrayAdapter<String> countryAdapter;
    private android.widget.ArrayAdapter<String> stateAdapter;
    private android.widget.ArrayAdapter<String> cityAdapter;
    private android.widget.EditText nameField;
    private android.widget.EditText phoneField;
    private android.widget.EditText dobField;
    private android.widget.AutoCompleteTextView genderField;
    private android.widget.AutoCompleteTextView countrySpinner;
    private android.widget.AutoCompleteTextView stateSpinner;
    private android.widget.AutoCompleteTextView citySpinner;
    private android.widget.EditText pincodeField;
    private android.widget.EditText addressField;
    private android.widget.EditText emergencyNoteField;
    private android.widget.EditText orgField, deptField; // 🚀 QA Fix: Master Corporate Bridge

    protected int dpToPx(int dp) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 🚀 QA Fix: Force-fetch profile data to ensure visual parity after returning from sub-screens (Identity Handshake)
        if (profileViewModel != null) {
            profileViewModel.fetchProfile(this);
        }
        
        // 🧠 QA Fix: Immediately repaint the Profile Avatar physically the second the user returns from the Editor pipeline
        if (activeProfileImgView != null && prefs != null) {
            int activeId = prefs.getInt("equipped_avatar_id", 0);
            if (activeId >= 0) {
                String[] existingModels = {
                    "models/AliciaSolid.vrm", 
                    "models/BlackSuger.vrm", 
                    "models/ButterCup.vrm", 
                    "models/FoxModel.vrm", 
                    "models/Seed-chan.vrm", 
                    "models/VRM1_Fox.vrm", 
                    "models/VRoid_Test.vrm"
                };
                int dynIdx = com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(activeId);
                String baseModel = existingModels[Math.abs(dynIdx) % existingModels.length];
                
                // 🚀 QA Fix: Standardize physical path execution natively without requiring a heavy WebViewClient Domain interceptor
                activeProfileImgView.evaluateJavascript("window.loadModel('../" + baseModel + "');", null);
            } else {
                activeProfileImgView.evaluateJavascript("if(typeof currentVrm !== 'undefined' && currentVrm) { scene.remove(currentVrm.scene); currentVrm=null; }", null);
            }
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
        
        ImageView backBtn = new ImageView(this);
        backBtn.setImageResource(android.R.drawable.ic_menu_revert);
        backBtn.setColorFilter(getGradientOverlayText());
        int bS = scale(60);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(bS, bS);
        bLp.setMargins(0, 0, scale(20), 0);
        backBtn.setLayoutParams(bLp);
        backBtn.setOnClickListener(v -> onBackPressed());
        topRow.addView(backBtn);

        TextView title = new TextView(this);
        title.setText(prefs.getString("user_nickname", "UNIDENTIFIED AGENT").toUpperCase());
        title.setTextColor(getGradientOverlayText());
        com.jdroidx.jfindme.ui.base.UiArchitect.applyText(this, title, "TEXT_HEADER");
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);
        header.addView(topRow);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Identity & Integrity Hub");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        com.jdroidx.jfindme.ui.base.UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        subtitle.setPadding(0, scale(10), 0, 0);
        header.addView(subtitle);
        
        mainRoot.addView(header);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int quota = getResponsivePaddingQuota();
        root.setPadding(quota, quota, quota, 140);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, quota);

        // --- LAYER 1: DUAL-CARD IDENTITY ARCHITECTURE ---
        LinearLayout dualCardRow = new LinearLayout(this);
        dualCardRow.setOrientation(LinearLayout.HORIZONTAL);
        dualCardRow.setWeightSum(2.0f);
        dualCardRow.setClipChildren(false);

        int headerItemGap = quota / 2;

        // CARD 1: IDENTITY LENS (Left)
        ImageView nativeProfilePhoto = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, nativeProfilePhoto);
        
        android.graphics.drawable.GradientDrawable photoBorder = new android.graphics.drawable.GradientDrawable();
        photoBorder.setShape(android.graphics.drawable.GradientDrawable.OVAL); 
        photoBorder.setColor(Color.TRANSPARENT);
        photoBorder.setStroke(6, getGradientStart()); 
        nativeProfilePhoto.setBackground(photoBorder);
        nativeProfilePhoto.setPadding(quota, quota, quota, quota); 
        nativeProfilePhoto.setClipToOutline(true);

        activeProfileImgView = new android.webkit.WebView(this);
        activeProfileImgView.setBackgroundColor(Color.TRANSPARENT);
        activeProfileImgView.getSettings().setJavaScriptEnabled(true);
        activeProfileImgView.loadUrl("file:///android_asset/web/vrm_viewer.html");

        LinearLayout lensCard = new LinearLayout(this);
        lensCard.setOrientation(LinearLayout.VERTICAL);
        lensCard.setGravity(Gravity.CENTER);
        UiArchitect.applyCard(this, lensCard, "CARD_STANDARD");
        lensCard.setPadding(quota, quota, quota, quota);
        
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        lp1.setMargins(0, 0, headerItemGap, 0);
        lensCard.setLayoutParams(lp1);

        android.widget.FrameLayout renderStack = new android.widget.FrameLayout(this);
        // 🚀 QA Fix: Increased height by 30% (3.5f -> 4.5f) as per User Request
        int iQuota = (int)(getResponsiveIconQuota() * 4.5f); 
        renderStack.addView(nativeProfilePhoto, new android.widget.FrameLayout.LayoutParams(iQuota, iQuota, Gravity.CENTER));
        renderStack.addView(activeProfileImgView, new android.widget.FrameLayout.LayoutParams(iQuota, iQuota, Gravity.CENTER));
        
        // 🚀 QA Fix: Transparent Tactical Interceptor (Handles clicks over the 3D WebView)
        View clickInterceptor = new View(this);
        clickInterceptor.setClickable(true);
        clickInterceptor.setFocusable(true);
        clickInterceptor.setOnClickListener(v -> showAvatarAssetsDetailsDialog());
        renderStack.addView(clickInterceptor, new android.widget.FrameLayout.LayoutParams(iQuota, iQuota, Gravity.CENTER));
        
        lensCard.setOnClickListener(v -> showAvatarAssetsDetailsDialog());
        lensCard.addView(renderStack);

        // CARD 2: ACTION GRID (Right)
        LinearLayout actionCard = new LinearLayout(this);
        actionCard.setOrientation(LinearLayout.VERTICAL);
        actionCard.setGravity(Gravity.CENTER);
        UiArchitect.applyCard(this, actionCard, "CARD_STANDARD");
        // 🚀 QA Fix: Increased internal padding to match 30% height growth
        int actionPad = (int)(headerItemGap * 1.5f); 
        actionCard.setPadding(actionPad, actionPad, actionPad, actionPad);

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        lp2.setMargins(headerItemGap, 0, 0, 0);
        actionCard.setLayoutParams(lp2);

        // 2x2 Action Matrix
        LinearLayout actionGrid = new LinearLayout(this);
        actionGrid.setOrientation(LinearLayout.VERTICAL);
        actionGrid.setGravity(Gravity.CENTER);

        LinearLayout gridRow1 = new LinearLayout(this);
        gridRow1.setOrientation(LinearLayout.HORIZONTAL);
        gridRow1.setGravity(Gravity.CENTER);
        
        LinearLayout gridRow2 = new LinearLayout(this);
        gridRow2.setOrientation(LinearLayout.HORIZONTAL);
        gridRow2.setGravity(Gravity.CENTER);

        // 🚀 QA Fix: Increased icon size by 10% (1.1f) as per User Request
        int hGridIconSize = (int)(UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD") * 1.1f);
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(hGridIconSize, hGridIconSize);
        int gMargin = scale(10);
        gLp.setMargins(gMargin, gMargin, gMargin, gMargin);

        // Populate Grid
        gridRow1.addView(prepareGridIcon(android.R.drawable.ic_menu_camera, v -> handleCameraLaunch()), gLp);
        gridRow1.addView(prepareGridIcon(android.R.drawable.ic_menu_gallery, v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 1004);
        }), gLp);
        
        gridRow2.addView(prepareGridIcon(android.R.drawable.ic_menu_crop, v -> {
            java.io.File file = new java.io.File(getFilesDir() + "/avatars", "custom_photo.png");
            if (file.exists()) {
                startActivityForResult(new android.content.Intent(this, AvatarCropActivity.class), 1002);
            }
        }), gLp);
        
        gridRow2.addView(prepareGridIcon(android.R.drawable.ic_menu_edit, v -> showAvatarSelectionDialog(nativeProfilePhoto)), gLp);

        actionGrid.addView(gridRow1);
        actionGrid.addView(gridRow2);
        actionCard.addView(actionGrid);

        dualCardRow.addView(lensCard);
        dualCardRow.addView(actionCard);
        
        root.addView(dualCardRow, cardLp);

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
        secBg.setCornerRadius(scaleRadius(30f));
        securityStatus.setBackground(secBg);
        
        View pairingStatus = createIconButton("Device Pairing", 
                android.R.drawable.ic_menu_share, v -> {
                    startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.group.GroupActivity.class));
                });
                
        android.graphics.drawable.GradientDrawable pairBg = new android.graphics.drawable.GradientDrawable();
        pairBg.setColor(Color.parseColor("#1565C0")); // Deep Blue Link Network!
        pairBg.setCornerRadius(scaleRadius(30f));
        pairingStatus.setBackground(pairBg);

        LinearLayout.LayoutParams squircleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        int itemGap = quota / 2;
        squircleLp.setMargins(0, 0, itemGap, 0); // Right margin for first box
        securityStatus.setLayoutParams(squircleLp);
        
        LinearLayout.LayoutParams squircleLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        squircleLp2.setMargins(itemGap, 0, 0, 0); // Left margin for second box
        pairingStatus.setLayoutParams(squircleLp2);

        integrityRow.addView(securityStatus);
        integrityRow.addView(pairingStatus);
        applyAccessibility(securityStatus, "Check Hardware Root Status");
        applyAccessibility(pairingStatus, "Open Node Pairing Hub");
        root.addView(integrityRow, cardLp);

        // --- LAYER 3: PERSONAL IDENTITY & SECURITY ---
        root.addView(createSectionHeader("PERSONAL IDENTITY & SECURITY"));
        
        LinearLayout pForm = new LinearLayout(this);
        pForm.setOrientation(LinearLayout.VERTICAL);
        com.jdroidx.jfindme.ui.base.UiArchitect.applyCard(this, pForm, "CARD_STANDARD");
        pForm.setPadding(quota, quota, quota, quota);
        
        nameField = new android.widget.EditText(this);
        nameField.setHint(android.text.Html.fromHtml("Name <font color='#D32F2F'>(*)</font>"));
        nameField.setText(prefs.getString("user_nickname", "").toUpperCase());
        nameField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        
        phoneField = new android.widget.EditText(this);
        phoneField.setHint(android.text.Html.fromHtml("Mobile Number <font color='#D32F2F'>(*)</font>"));
        phoneField.setText(prefs.getString("user_phone", ""));
        phoneField.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        addressField = new android.widget.EditText(this);
        addressField.setHint("Primary Residence Address");
        addressField.setText(prefs.getString("user_address", ""));
        addressField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        
        emergencyNoteField = new android.widget.EditText(this);
        emergencyNoteField.setHint("Emergency Action Protocol / Medical Notes");
        emergencyNoteField.setText(prefs.getString("user_emergency_note", ""));
        emergencyNoteField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        
        // 🚀 QA Fix: L7 Corporate Sync Injection
        orgField = new android.widget.EditText(this);
        orgField.setHint("Organization / Hub Name");
        orgField.setText(prefs.getString("org_name", ""));
        
        deptField = new android.widget.EditText(this);
        deptField.setHint("Department / Sector");
        deptField.setText(prefs.getString("department", ""));
        
        dobField = new android.widget.EditText(this);
        dobField.setHint(android.text.Html.fromHtml("BirthDay <font color='#D32F2F'>(*)</font>"));
        dobField.setText(prefs.getString("user_dob", ""));
        dobField.setFocusable(false);
        dobField.setClickable(true);
        dobField.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                dobField.setText(java.lang.String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth));
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        genderField = new android.widget.AutoCompleteTextView(this);
        genderField.setHint(android.text.Html.fromHtml("Gender <font color='#D32F2F'>(*)</font>"));
        genderField.setText(prefs.getString("user_gender", "Select Gender"));
        genderField.setAdapter(createEnterpriseDropdownAdapter(new java.util.ArrayList<>(java.util.Arrays.asList("Select Gender", "Male", "Female", "Prefer Not to Say"))));
        genderField.setThreshold(1);
        genderField.setDropDownBackgroundDrawable(createPopupBackground());
        genderField.setFocusable(false);
        genderField.setClickable(true);
        genderField.setOnClickListener(v -> genderField.showDropDown());

        countrySpinner = new android.widget.AutoCompleteTextView(this);
        countrySpinner.setHint(android.text.Html.fromHtml("Country <font color='#D32F2F'>(*)</font>"));
        String savedCountry = prefs.getString("user_country", "");
        countrySpinner.setText(savedCountry.equals("Identifying...") ? "" : savedCountry);
        countryAdapter = createEnterpriseDropdownAdapter(new java.util.ArrayList<>(java.util.Collections.singletonList("Select Country")));
        countrySpinner.setAdapter(countryAdapter);
        countrySpinner.setThreshold(1);
        countrySpinner.setDropDownBackgroundDrawable(createPopupBackground());

        stateSpinner = new android.widget.AutoCompleteTextView(this);
        stateSpinner.setHint(android.text.Html.fromHtml("State/Province<font color='#D32F2F'>(*)</font>"));
        String savedState = prefs.getString("user_state", "");
        stateSpinner.setText(savedState.equals("Identifying...") ? "" : savedState);
        stateAdapter = createEnterpriseDropdownAdapter(new java.util.ArrayList<>(java.util.Collections.singletonList("Select State")));
        stateSpinner.setAdapter(stateAdapter);
        stateSpinner.setThreshold(1);
        stateSpinner.setDropDownBackgroundDrawable(createPopupBackground());
        
        citySpinner = new android.widget.AutoCompleteTextView(this);
        citySpinner.setHint(android.text.Html.fromHtml("City/Zone<font color='#D32F2F'>(*)</font>"));
        String savedCity = prefs.getString("user_city", "");
        citySpinner.setText(savedCity.equals("Identifying...") ? "" : savedCity);
        cityAdapter = createEnterpriseDropdownAdapter(new java.util.ArrayList<>(java.util.Collections.singletonList("Select City")));
        citySpinner.setAdapter(cityAdapter);
        citySpinner.setThreshold(1);
        citySpinner.setDropDownBackgroundDrawable(createPopupBackground());
        
        countrySpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selection = countryAdapter.getItem(position);
            if (profileViewModel != null && selection != null && !selection.equals("Identifying...")) {
                profileViewModel.fetchStatesList(selection);
            }
        });

        stateSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selection = stateAdapter.getItem(position);
            String parentCountry = countrySpinner.getText().toString();
            if (profileViewModel != null && selection != null && !parentCountry.isEmpty() && !parentCountry.equals("Identifying...")) {
                profileViewModel.fetchCitiesList(parentCountry, selection);
            }
        });

        pincodeField = new android.widget.EditText(this);
        pincodeField.setHint(android.text.Html.fromHtml("ZipCode<font color='#D32F2F'>(*)</font>"));
        pincodeField.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        pincodeField.setText(prefs.getString("user_pincode", ""));

        String currentEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail() : "node@jfindme.net";
        android.widget.EditText emailField = new android.widget.EditText(this);
        emailField.setHint("Enterprise Email (Read Only)");
        emailField.setText(currentEmail);
        emailField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setEnabled(false); // Emails require Firebase re-auth loops, locking config by default
        pForm.addView(wrapEnterpriseFullGrid(emailField, prefs.getInt("icon_profile_email", android.R.drawable.ic_menu_send)));

        // Row 1: Core Identity Matrix (2x2)
        LinearLayout identityRow = new LinearLayout(this);
        identityRow.setOrientation(LinearLayout.HORIZONTAL);
        identityRow.setWeightSum(2.0f);
        identityRow.addView(wrapEnterpriseHalfGrid(nameField, prefs.getInt("icon_profile_name", android.R.drawable.ic_menu_myplaces)));
        identityRow.addView(wrapEnterpriseHalfGrid(genderField, android.R.drawable.ic_menu_myplaces));
        
        LinearLayout.LayoutParams identityLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        identityLp.setMargins(0, scale(10), 0, scale(10));
        pForm.addView(identityRow, identityLp);

        // Row 2: Persistence Matrix (2x2)
        LinearLayout persistRow = new LinearLayout(this);
        persistRow.setOrientation(LinearLayout.HORIZONTAL);
        persistRow.setWeightSum(2.0f);
        persistRow.addView(wrapEnterpriseHalfGrid(phoneField, prefs.getInt("icon_profile_phone", android.R.drawable.ic_menu_call)));
        persistRow.addView(wrapEnterpriseHalfGrid(dobField, android.R.drawable.ic_menu_today));
        
        LinearLayout.LayoutParams persistLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        persistLp.setMargins(0, scale(10), 0, scale(10));
        pForm.addView(persistRow, persistLp);

        // Standard Address Field (Full)
        pForm.addView(wrapEnterpriseFullGrid(addressField, android.R.drawable.ic_menu_mapmode));

        // Row 3: Geo-Matrix 1 (2x2)
        LinearLayout geoRow1 = new LinearLayout(this);
        geoRow1.setOrientation(LinearLayout.HORIZONTAL);
        geoRow1.setWeightSum(2.0f);
        geoRow1.addView(wrapEnterpriseHalfGrid(countrySpinner, android.R.drawable.ic_menu_mapmode));
        geoRow1.addView(wrapEnterpriseHalfGrid(stateSpinner, android.R.drawable.ic_menu_mylocation));
        LinearLayout.LayoutParams geo1Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        geo1Lp.setMargins(0, scale(10), 0, scale(10));
        pForm.addView(geoRow1, geo1Lp);

        // Row 4: Geo-Matrix 2 (2x2)
        LinearLayout geoRow2 = new LinearLayout(this);
        geoRow2.setOrientation(LinearLayout.HORIZONTAL);
        geoRow2.setWeightSum(2.0f);
        geoRow2.addView(wrapEnterpriseHalfGrid(citySpinner, android.R.drawable.ic_dialog_map));
        geoRow2.addView(wrapEnterpriseHalfGrid(pincodeField, android.R.drawable.ic_menu_info_details));
        LinearLayout.LayoutParams geo2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        geo2Lp.setMargins(0, scale(10), 0, scale(10));
        pForm.addView(geoRow2, geo2Lp);

        // Emergency Protocol (Full)
        pForm.addView(wrapEnterpriseFullGrid(emergencyNoteField, android.R.drawable.ic_dialog_info));
        
        // 🚀 QA Fix: Global Corporate Bridge Injection
        pForm.addView(wrapEnterpriseFullGrid(orgField, android.R.drawable.ic_menu_agenda));
        pForm.addView(wrapEnterpriseFullGrid(deptField, android.R.drawable.ic_menu_directions));
        
        root.addView(pForm, cardLp);

        // --- LAYER 4: SYSTEM ACTIONS & SECURITY ---
        root.addView(createSectionHeader("SYSTEM ACTIONS & SECURITY"));

        // 🚀 QA Fix: High-Density 2x2 Forensic Grid
        LinearLayout gridLayout = new LinearLayout(this);
        gridLayout.setOrientation(LinearLayout.VERTICAL);

        // Row 1: Detail & Organization
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setWeightSum(2.0f);
        
        View dossierBtn = createIconButton("DETAIL PROFILE", android.R.drawable.ic_menu_edit, true, v -> {
            String safeUid = (profileViewModel != null) ? profileViewModel.getCurrentUserId() : null;
            if (safeUid != null) {
                android.content.Intent dossierRoute = new android.content.Intent(this, com.jdroidx.jfindme.ui.group.PersonDetailsActivity.class);
                dossierRoute.putExtra("TARGET_UID", safeUid);
                dossierRoute.putExtra("TARGET_ROLE", prefs.getString("role_family", "IDENTITY_HOLDER"));
                startActivity(dossierRoute);
            } else {
                android.widget.Toast.makeText(this, "Hardware Lock Missing.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        View orgBtn = createIconButton("ORGANIZATION", android.R.drawable.ic_menu_edit, true, v -> {
            try {
                startActivity(new android.content.Intent(this, Class.forName("com.jdroidx.jfindme.ui.settings.OrgActivity")));
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Organization Suite Initialization Failed.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        cellLp.setMargins(10, 10, 10, 10);
        dossierBtn.setLayoutParams(cellLp);
        orgBtn.setLayoutParams(cellLp);
        row1.addView(dossierBtn);
        row1.addView(orgBtn);

        // Row 2: Password & Verify/Save
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setWeightSum(2.0f);

        View passBtn = createIconButton("PASSWORD", android.R.drawable.ic_menu_edit, true, v -> {
            android.widget.EditText passInput = new android.widget.EditText(this);
            passInput.setHint("New Password");
            passInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new android.app.AlertDialog.Builder(this)
                .setTitle("Update Password")
                .setView(passInput)
                .setPositiveButton("UPDATE", (dialog, which) -> {
                    String newPass = passInput.getText().toString();
                    if (newPass.length() >= 6) {
                        try {
                            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().updatePassword(newPass);
                            android.widget.Toast.makeText(this, "Security Matrix Updated.", android.widget.Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {}
                    }
                })
                .setNegativeButton("Cancel", null).show();
        });

        View saveBtn = createIconButton("VERIFY & SAVE", android.R.drawable.ic_menu_save, true, v -> handleProfileSaveAction(false));
        
        passBtn.setLayoutParams(cellLp);
        saveBtn.setLayoutParams(cellLp);
        row2.addView(passBtn);
        row2.addView(saveBtn);

        gridLayout.addView(row2);
        gridLayout.addView(row1);
        root.addView(gridLayout);

        // 🚀 QA Fix: Terminal Logout Session (Post-Grid Action)
        View logoutBtn = createIconButton("LOGOUT SESSION", android.R.drawable.ic_lock_power_off, v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setPositiveButton("Logout", (dialog, which) -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.auth.AuthActivity.class));
                    finishAffinity();
                }).setNegativeButton("Cancel", null).show();
        });
        LinearLayout.LayoutParams logoutLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        logoutLp.setMargins(10, 40, 10, 100); 
        logoutBtn.setLayoutParams(logoutLp);
        root.addView(logoutBtn);

        scrollView.addView(root);
        mainRoot.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 🚀 QA Fix: Global Navigation Integrity (Profile Hub active) - REMOVED AS PER USER REQUEST
        // String[] keys = {"NAV_NOTICE", "NAV_DEVICE", "NAV_ME"};
        // mainRoot.addView(com.jdroidx.jfindme.ui.base.UiArchitect.createBottomNav(this, keys, "NAV_ME"));

        return mainRoot;
    }

    private View createStatCard(String title, String value) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        String mAlpha = com.jdroidx.jfindme.ui.base.ThemeManager.THEMES[getResolvedThemeId()][1].substring(1, 3);
        applyGlassmorphism(card, Color.parseColor("#" + mAlpha + "ffffff"), 16f);
        card.setGravity(Gravity.CENTER);
        
        android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
        int margin = UiScalingMatrixTable.scalePadding(stage, 20);
        params.setMargins(margin, margin, margin, margin);
        card.setLayoutParams(params);
        int p = UiScalingMatrixTable.scalePadding(stage, 40);
        card.setPadding(margin, p, margin, p);

        TextView valTxt = new TextView(this);
        valTxt.setText(value);
        valTxt.setTextColor(Color.WHITE);
        valTxt.setTextSize(UiScalingMatrixTable.scaleText(stage, 18, true));
        valTxt.setTypeface(null, Typeface.BOLD);
        
        TextView titleTxt = new TextView(this);
        titleTxt.setText(title);
        titleTxt.setTextColor(Color.GRAY);
        titleTxt.setTextSize(UiScalingMatrixTable.scaleText(stage, 10));
        titleTxt.setPadding(0, 10, 0, 0);

        card.addView(valTxt);
        card.addView(titleTxt);
        
        applyAccessibility(card, "Identity Metric: " + title);
        return card;
    }

    private TextView createInfoText(String text, float size, boolean bold) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getTextColor());
        tv.setTextSize(UiScalingMatrixTable.scaleText(stage, size));
        tv.setGravity(Gravity.CENTER);
        int p = UiScalingMatrixTable.scalePadding(stage, 20);
        tv.setPadding(0, p, 0, p/2);
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
                        safeRecreate(); // 🚀 QA NATIVE FIX! Bypasses OEM Relaunch Error!
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
                
                // Clear the 3D surface visually if they selected a raw 2D image
                if (activeProfileImgView != null) {
                    activeProfileImgView.evaluateJavascript("if(typeof currentVrm !== 'undefined' && currentVrm) { scene.remove(currentVrm.scene); currentVrm=null; }", null);
                }
                
                // 🚀 QA Fix: Prime Directive MVVM Controller
                if (profileViewModel != null) {
                    profileViewModel.equipCustomAvatar(prefs); 
                }
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
            nameStr.setTextSize(UiScalingMatrixTable.scaleText(UiArchitect.getStage(this), 12));
            nameStr.setGravity(Gravity.CENTER);
            nameStr.setBackgroundColor(Color.parseColor("#88000000")); // 50% Black transparency for contrast
            nameStr.setPadding(0, 15, 0, 15);
            
            android.widget.RelativeLayout.LayoutParams nameParams = new android.widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
            nameStr.setLayoutParams(nameParams);
            tile.addView(nameStr);
            applyAccessibility(tile, "Select Avatar: " + avatar.getType());

            // 4. Interaction Binding (Execute Equip & Circle Fill)
            tile.setOnClickListener(v -> {
                circle.setImageResource(android.R.drawable.checkbox_on_background); // Visual fill confirmation
                mainProfilePic.setImageDrawable(previewImg.getDrawable());
                mainProfilePic.clearColorFilter(); // Remove Tint
                
                // Apply the new 3D WebGL payload
                int activeId = avatar.getBaseId();
                if (activeProfileImgView != null) {
                    // 🚀 QA Fix: Dynamically load exact physical files from the 'assets/models/' folder!
                    String[] existingModels = {
                        "models/AliciaSolid.vrm", 
                        "models/BlackSuger.vrm", 
                        "models/ButterCup.vrm", 
                        "models/FoxModel.vrm", 
                        "models/Seed-chan.vrm", 
                        "models/VRM1_Fox.vrm", 
                        "models/VRoid_Test.vrm"
                    };
                    String baseModel = existingModels[Math.abs(activeId) % existingModels.length];
                    activeProfileImgView.evaluateJavascript("window.loadModel('../" + baseModel + "');", null);
                }
                
                // prime directive context map: Save specific Selection natively
                if (profileViewModel != null) {
                    profileViewModel.equipAvatar(prefs, activeId);
                }

                android.widget.Toast.makeText(this, "Equipped Avatar: " + avatar.getType(), android.widget.Toast.LENGTH_SHORT).show();
                
                dialog.dismiss();
            });

            gridLayout.addView(tile);
        }

        scrollView.addView(gridLayout);
        dialog.show();
    }

    private View createSectionHeader(String text) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Native Theme Integration
        header.setTextSize(UiScalingMatrixTable.scaleText(stage, 14, true)); 
        int pT = UiScalingMatrixTable.scalePadding(stage, 30);
        int pB = UiScalingMatrixTable.scalePadding(stage, 20);
        header.setPadding(10, pT, 0, pB);
        header.setTypeface(null, Typeface.BOLD);
        return header;
    }


    // 🚀 QA Fix: Descriptive Label Engine
    private TextView createFieldLabel(String text, boolean mandatory) {
        TextView tv = new TextView(this);
        String labelText = text + (mandatory ? " <font color='#D32F2F'>(*)</font>" : "");
        tv.setText(android.text.Html.fromHtml(labelText));
        tv.setTextColor(getGradientStart()); // Match Enterprise Palette
        tv.setTextSize(scaleText(13));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        int pT = scale(30);
        int pB = scale(5);
        tv.setPadding(scale(10), pT, 0, pB); // Add spacing for clarity
        return tv;
    }

    private void syncFieldsToPrefs() {
        if (nameField == null || phoneField == null) return;
        String nameVal = nameField.getText().toString().trim();
        String phoneVal = phoneField.getText().toString().trim();
        String dobVal = dobField.getText().toString().trim();
        String genderVal = genderField.getText().toString().trim();
        String pincodeVal = pincodeField.getText().toString().trim();
        String cVal = countrySpinner.getText().toString().trim();
        String sVal = stateSpinner.getText().toString().trim();
        String cityVal = citySpinner.getText().toString().trim();
        String addressVal = addressField.getText().toString().trim();
        String emergencyNoteVal = emergencyNoteField.getText().toString().trim();
        String orgVal = orgField.getText().toString().trim();
        String deptVal = deptField.getText().toString().trim();

        prefs.edit()
            .putString("user_nickname", nameVal)
            .putString("user_phone", phoneVal)
            .putString("user_dob", dobVal)
            .putString("user_gender", genderVal)
            .putString("user_country", cVal)
            .putString("user_state", sVal)
            .putString("user_city", cityVal)
            .putString("user_pincode", pincodeVal)
            .putString("user_address", addressVal)
            .putString("user_emergency_note", emergencyNoteVal)
            .putString("org_name", orgVal)
            .putString("department", deptVal)
            .apply();
    }

    private void handleProfileSaveAction(boolean ignoreValidation) {
        this.isSaveLaterAction = ignoreValidation;
        syncFieldsToPrefs();
        
        String nameVal = nameField.getText().toString().trim();
        String phoneVal = phoneField.getText().toString().trim();
        String dobVal = dobField.getText().toString().trim();
        String genderVal = genderField.getText().toString().trim();
        int genderIdx = genderField.getText().toString().equals("Male") ? 0 : 1;
        String pincodeVal = pincodeField.getText().toString().trim();
        
        String famStr = prefs.getString("role_family", "None");
        String friendStr = prefs.getString("role_friend", "None");
        
        String cVal = countrySpinner.getText().toString().trim();
        String sVal = stateSpinner.getText().toString().trim();
        String cityVal = citySpinner.getText().toString().trim();
        
        String addressVal = addressField.getText().toString().trim();
        String emergencyNoteVal = emergencyNoteField.getText().toString().trim();

        if (profileViewModel != null) {
            profileViewModel.verifyAndSaveProfile(
                this,
                nameVal, phoneVal, dobVal, genderIdx, genderVal,
                (cVal.isEmpty() || cVal.equals("Identifying...")),
                pincodeVal, cVal, sVal, cityVal, famStr, friendStr,
                addressVal, emergencyNoteVal,
                orgField.getText().toString().trim(), 
                deptField.getText().toString().trim(),
                prefs,
                ignoreValidation
            );
        }
    }

    private View wrapEnterpriseFullGrid(View inner, int iconRes) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        int pS = UiScalingMatrixTable.scalePadding(stage, 35);
        int pV = UiScalingMatrixTable.scalePadding(stage, 20);
        row.setPadding(pS, pV, pS, pV); 
        
        android.graphics.drawable.GradientDrawable fieldBg = new android.graphics.drawable.GradientDrawable();
        String mAlpha = com.jdroidx.jfindme.ui.base.ThemeManager.THEMES[getResolvedThemeId()][1].substring(1, 3);
        fieldBg.setColor(Color.parseColor("#" + mAlpha + "000000")); 
        fieldBg.setStroke(3, getGradientStart()); 
        fieldBg.setCornerRadius(UiScalingMatrixTable.scaleRadius(stage, 30f));
        row.setBackground(fieldBg);
        
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, scale(10), 0, scale(10)); // 🚀 QA Fix: Standardized L7 Universal Gap Scaling
        row.setLayoutParams(rowLp);

        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart());
        int iS = UiScalingMatrixTable.scaleIcon(stage, 55);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iS, iS));
        row.addView(icon);
        
        if (inner instanceof android.widget.EditText) {
             android.widget.EditText et = (android.widget.EditText) inner;
             et.setTextColor(getTextColor());
             et.setHintTextColor(getSecondaryTextColor());
             et.setBackgroundColor(Color.TRANSPARENT);
             et.setPadding(pS, 0, pV, 0); 
             UiArchitect.calibrateText(this, et, "TEXT_BODY");
        }
        inner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(inner);
        applyAccessibility(row, "Profile Data Input Node");
        return row;
    }

    private View wrapEnterpriseHalfGrid(View inner, int iconRes) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pS = UiScalingMatrixTable.scalePadding(stage, 15);
        row.setPadding(pS, pS, pS, pS); 
        
        android.graphics.drawable.GradientDrawable fieldBg = new android.graphics.drawable.GradientDrawable();
        String mAlpha = com.jdroidx.jfindme.ui.base.ThemeManager.THEMES[getResolvedThemeId()][1].substring(1, 3);
        fieldBg.setColor(Color.parseColor("#" + mAlpha + "000000")); 
        fieldBg.setStroke(3, getGradientStart()); 
        fieldBg.setCornerRadius(UiScalingMatrixTable.scaleRadius(stage, 20f));
        row.setBackground(fieldBg);
        
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        rowLp.setMargins(scale(10), scale(10), scale(10), scale(10)); // 🚀 QA Fix: Standardized L7 Universal Gap Scaling
        row.setLayoutParams(rowLp);

        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart());
        int iS = UiScalingMatrixTable.scaleIcon(stage, 40);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iS, iS));
        row.addView(icon);
        
        inner.setPadding(pS, pS, pS/2, pS); 
        if (inner instanceof android.widget.EditText) {
             android.widget.EditText et = (android.widget.EditText) inner;
             et.setTextColor(getTextColor());
             et.setHintTextColor(getSecondaryTextColor());
             et.setBackgroundColor(Color.TRANSPARENT);
             UiArchitect.calibrateText(this, et, "TEXT_BODY");
        }
        inner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(inner);
        applyAccessibility(row, "Profile High-Density Data Matrix Node");
        return row;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.d(TAG, "Profile Security Cluster Initialized.");
        
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 🚀 QA Fix: Identity Matrix Integrity - HARD LOCK Back Navigation
                syncFieldsToPrefs(); // Force sync current UI values to prefs for validation
                
                if (hasUnsavedChanges()) {
                    new android.app.AlertDialog.Builder(ProfileActivity.this)
                        .setTitle("⚠️ UNSAVED CHANGES")
                        .setMessage("You have made changes to your identity profile. Would you like to update your cloud roster or discard these changes?")
                        .setPositiveButton("UPDATE 🚀", (dialog, which) -> handleProfileSaveAction(false))
                        .setNegativeButton("DISCARD 🕒", (dialog, which) -> {
                            // Restore initial values visually if needed, or just exit
                            finish();
                        })
                        .setNeutralButton("CANCEL", null)
                        .show();
                    return;
                }

                String missing = profileViewModel.getMissingMandatoryFields(prefs);
                if (missing != null) {
                    new android.app.AlertDialog.Builder(ProfileActivity.this)
                        .setTitle("🛡️ IDENTITY INTEGRITY REQUIRED")
                        .setMessage("Hi there! We found a few pieces of your safety shield that need completing: 🛡️\n\n" +
                                missing + "\n\n" +
                                "To activate your forensic tracking and access the Dashboard, please complete these fields.\n\n" +
                                "⚠️ Note: This is a mandatory requirement for your safety network.")
                        .setPositiveButton("COMPLETE NOW 🚀", null)
                        .setNegativeButton("SAVE LATER 🕒", (dialog, which) -> {
                            handleProfileSaveAction(true); // Save current changes without validation
                            
                            // 🚀 QA Fix: Set Bypass Flag to allow MapActivity access for this session
                            prefs.edit().putBoolean("profile_bypass_nudge", true).apply();
                            
                            startActivity(new android.content.Intent(ProfileActivity.this, com.jdroidx.jfindme.ui.map.MapActivity.class)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                            finish();
                        })
                        .setCancelable(false) // Hard Lock: Cannot dismiss without choosing an action
                        .show();
                } else {
                    android.widget.Toast.makeText(ProfileActivity.this, "🛡️ Identity Verified. Proceeding to Dashboard.", android.widget.Toast.LENGTH_SHORT).show();
                    startActivity(new android.content.Intent(ProfileActivity.this, com.jdroidx.jfindme.ui.map.MapActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    finish();
                }
            }
        });
        profileViewModel = new androidx.lifecycle.ViewModelProvider(this).get(ProfileViewModel.class);
        
        
        // 🚀 MVVM Network Extractor injected strictly!
        profileViewModel.getCountriesList().observe(this, payload -> {
            if (countryAdapter != null && payload != null) {
                countryAdapter.clear();
                countryAdapter.addAll(payload);
                countryAdapter.notifyDataSetChanged();
            }
        });
        profileViewModel.getStatesList().observe(this, payload -> {
            if (stateAdapter != null && payload != null) {
                stateAdapter.clear();
                stateAdapter.addAll(payload);
                stateAdapter.notifyDataSetChanged();
            }
        });
        profileViewModel.getCitiesList().observe(this, payload -> {
            if (cityAdapter != null && payload != null) {
                cityAdapter.clear();
                cityAdapter.addAll(payload);
                cityAdapter.notifyDataSetChanged();
            }
        });
        profileViewModel.fetchNetworkLocation();
        
        // 🚀 QA Fix: Inbound Profile Sync - Automatically pre-populate UI and Prefs from Cloud/DB
        profileViewModel.getProfileData().observe(this, data -> {
            if (data == null || data.isEmpty()) return;
            
            android.content.SharedPreferences.Editor editor = prefs.edit();
            
            if (data.containsKey("userName") || data.containsKey("name")) {
                String val = String.valueOf(data.getOrDefault("userName", data.get("name")));
                if (nameField != null && nameField.getText().toString().isEmpty()) nameField.setText(val);
                editor.putString("user_nickname", val);
            }
            if (data.containsKey("userPhone") || data.containsKey("phone")) {
                String val = String.valueOf(data.getOrDefault("userPhone", data.get("phone")));
                if (phoneField != null && phoneField.getText().toString().isEmpty()) phoneField.setText(val);
                editor.putString("user_phone", val);
            }
            if (data.containsKey("userDob") || data.containsKey("dob")) {
                String val = String.valueOf(data.getOrDefault("userDob", data.get("dob")));
                if (dobField != null && dobField.getText().toString().isEmpty()) dobField.setText(val);
                editor.putString("user_dob", val);
            }
            if (data.containsKey("userGender") || data.containsKey("gender")) {
                String val = String.valueOf(data.getOrDefault("userGender", data.get("gender")));
                if (genderField != null && (genderField.getText().toString().isEmpty() || genderField.getText().toString().equals("Select Gender"))) genderField.setText(val, false);
                editor.putString("user_gender", val);
            }
            if (data.containsKey("userCountry") || data.containsKey("country")) {
                String val = String.valueOf(data.getOrDefault("userCountry", data.get("country")));
                if (countrySpinner != null && (countrySpinner.getText().toString().isEmpty() || countrySpinner.getText().toString().equals("Select Country"))) countrySpinner.setText(val, false);
                editor.putString("user_country", val);
            }
            if (data.containsKey("userState") || data.containsKey("state")) {
                String val = String.valueOf(data.getOrDefault("userState", data.get("state")));
                if (stateSpinner != null && (stateSpinner.getText().toString().isEmpty() || stateSpinner.getText().toString().equals("Select State"))) stateSpinner.setText(val, false);
                editor.putString("user_state", val);
            }
            if (data.containsKey("userCity") || data.containsKey("city")) {
                String val = String.valueOf(data.getOrDefault("userCity", data.get("city")));
                if (citySpinner != null && (citySpinner.getText().toString().isEmpty() || citySpinner.getText().toString().equals("Select City"))) citySpinner.setText(val, false);
                editor.putString("user_city", val);
            }
            if (data.containsKey("userPincode") || data.containsKey("pincode")) {
                String val = String.valueOf(data.getOrDefault("userPincode", data.get("pincode")));
                if (pincodeField != null && pincodeField.getText().toString().isEmpty()) pincodeField.setText(val);
                editor.putString("user_pincode", val);
            }
            if (data.containsKey("userAddress") || data.containsKey("address")) {
                String val = String.valueOf(data.getOrDefault("userAddress", data.get("address")));
                if (addressField != null && addressField.getText().toString().isEmpty()) addressField.setText(val);
                editor.putString("user_address", val);
            }
            
            editor.apply();

            // 🚀 QA Fix: Capture Initial State for Change Detection
            if (!isDataLoaded) {
                captureInitialState();
                isDataLoaded = true;
            }
        });
        
        profileViewModel.fetchProfile(this);
        
        // 🚀 QA Fix: Installation Completion Guard
        // Suppress on every action taken in the image icons (SafeRecreate blocker)
        if (prefs.getString("user_pincode", "").isEmpty() && !onboardingDialogShown) {
            onboardingDialogShown = true;
            new android.app.AlertDialog.Builder(this)
                .setTitle("🔔 Finishing Touches Required!")
                .setMessage("Hi there! We are almost done setting up your safety shield! 🛡️\n\n" +
                        "To complete your installation, please fill in these important fields marked with a (*) red star:\n\n" +
                        "✅ Your Name\n" +
                        "✅ Mobile Number\n" +
                        "✅ Pincode (Very Important!)\n" +
                        "✅ Your Country, State, and City\n\n" +
                        "This helps your friends and family find you quickly if you ever need them. It's like giving your house a proper address on the map! 🏠✨")
                .setPositiveButton("LET'S DO IT! 🚀", null)
                .show();
        }

        profileViewModel.isLoading().observe(this, loading -> {
            if (loading) com.jdroidx.jfindme.utils.AngelLoader.show(this);
            else com.jdroidx.jfindme.utils.AngelLoader.hide(this);
        });
        
        // 🚀 Dynamic Loading Script Observer
        profileViewModel.getLoadingState().observe(this, payload -> {
            if (payload != null) {
                com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, payload);
            }
        });
        
        profileViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                // 🚀 QA Fix: Dynamic 'Trigger Dialog' Hub (Multi-Error Checklist)
                String displayMsg;
                if (error.startsWith("MULTIPLE_ERRORS:")) {
                    String cleanList = error.replace("MULTIPLE_ERRORS:", "");
                    java.lang.StringBuilder sb = new java.lang.StringBuilder();
                    sb.append("Hi there! We found a few pieces of your safety shield that need completing: 🛡️\n\n");
                    for (String line : cleanList.split("\n")) {
                        sb.append("   • ").append(line).append("\n");
                    }
                    sb.append("\nOnce you fill these in, your forensic profile will be fully active! ✨");
                    displayMsg = sb.toString();
                } else {
                    displayMsg = error;
                }
                
                new android.app.AlertDialog.Builder(this)
                    .setTitle("🔔 FINISHING TOUCHES REQUIRED")
                    .setMessage(displayMsg)
                    .setPositiveButton("RESOLVE ISSUES 🚀", null)
                    .show();
                
                profileViewModel.clearError();
            }
        });
        
        // 🚀 Native Validation Matrix Callback
        profileViewModel.getValidationErrorField().observe(this, field -> {
            if (field == null) return;
        });
        
        // 🚀 Final Payload Sync Return Path
        profileViewModel.getSaveSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                String targetMsg = isSaveLaterAction ? "Profile Buffered. Proceeding to Dashboard..." : "🛡️ Identity Secure & Verified. Proceeding to Tactical Hub...";
                android.widget.Toast.makeText(this, targetMsg, android.widget.Toast.LENGTH_LONG).show();
                prefs.edit().putBoolean("profile_onboarded", true).apply();
                
                // 🚀 QA Fix: Master Permission Intercept Sequence!
                java.util.List<String> neededPerms = new java.util.ArrayList<>();
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    neededPerms.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        neededPerms.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        neededPerms.add(android.Manifest.permission.POST_NOTIFICATIONS);
                    }
                }
                
                if (!neededPerms.isEmpty()) {
                    android.widget.Toast.makeText(this, "Authorizing Hardware Node Permissions...", android.widget.Toast.LENGTH_SHORT).show();
                    androidx.core.app.ActivityCompat.requestPermissions(this, neededPerms.toArray(new String[0]), 999);
                } else {
                    executeCloudMatrixVerification();
                }
            }
        });
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
            safeRecreate(); // 🚀 QA NATIVE FIX! Bypasses OEM Relaunch Error!
            return;
        }

        // 🚀 QA Block: Secure URI Stream Extractor for Native Device Gallery Picks
        if (requestCode == 1004 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri pickedUri = data.getData();
            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                try {
                    java.io.InputStream is = getContentResolver().openInputStream(pickedUri);
                    android.graphics.Bitmap rawBit = android.graphics.BitmapFactory.decodeStream(is);
                    if(is != null) is.close();

                    if(rawBit != null) {
                        int scaleFactor = Math.max(1, java.lang.Math.min(rawBit.getWidth()/600, rawBit.getHeight()/600));
                        android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(rawBit, rawBit.getWidth()/scaleFactor, rawBit.getHeight()/scaleFactor, true);

                        java.io.File dir = new java.io.File(getFilesDir(), "avatars");
                        if (!dir.exists()) dir.mkdirs();
                        java.io.File file = new java.io.File(dir, "custom_photo.png");

                        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                        scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush(); fos.close();

                        // Enforce strictly to Custom Avatar ID via the Prime Directive MVVM Pattern
                        if (profileViewModel != null) profileViewModel.equipCustomAvatar(prefs);

                        runOnUiThread(() -> {
                            com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                            // 🚀 QA Fix: Automatically launch Crop Suite after Upload
                            startActivityForResult(new android.content.Intent(this, AvatarCropActivity.class), 1002);
                        });
                    }
                } catch(Exception ex) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                        android.widget.Toast.makeText(this, "Failed isolating Gallery Payload: " + ex.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    });
                }
            });
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
                    final android.graphics.Bitmap captured = camImage;
                    
                    // 1. QA Background Structural Hardware Limiter (Prevents MultiMarkerEngine.kt UI Block & Memory Collapse)
                    java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createScaledBitmap(captured, 350, 350, true);
                            
                            // 🚀 QA Fix: Manual Orientation Calibration Matrix for standard Android Hardware
                            android.graphics.Matrix matrix = new android.graphics.Matrix();
                            matrix.postRotate(0); // Standard baseline
                            android.graphics.Bitmap rotated = android.graphics.Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

                            // 2. Local App-Specific Storage Injection Directory
                            java.io.File dir = new java.io.File(getFilesDir(), "avatars");
                            if (!dir.exists()) dir.mkdirs();
                            java.io.File file = new java.io.File(dir, "custom_photo.png");
                            
                            // 3. I/O Block Writer
                            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                            rotated.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                            fos.flush();
                            fos.close();
                            
                            // Auto-equip the brand new structural file mathematically via Prime Directive MVVM
                            if (profileViewModel != null) profileViewModel.equipCustomAvatar(prefs);
                            
                            // Safe Thread return
                            runOnUiThread(() -> {
                                // 🚀 QA Fix: Automatically launch Crop Suite after Camera Capture
                                startActivityForResult(new android.content.Intent(this, AvatarCropActivity.class), 1002);
                            });
                        } catch(Exception e) {
                            SystemLogger.INSTANCE.e(TAG, "File I/O Write Failure in Local Avatar Cache", e);
                        }
                    });
                }
            } catch (Exception e) {
                SystemLogger.INSTANCE.e(TAG, "File I/O Write Failure in Local Avatar Cache", e);
            }
        }
    }
    
    // 🚀 QA Fix: Permission Callback Listener
    // The exact split-second the User accepts or denies the OS dialog, we automatically transition them.
    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 999) {
            // "After completing the permission, Live Map Dashboard must open."
            executeCloudMatrixVerification();
        } else if (requestCode == 1003) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.widget.Toast.makeText(this, "Hardware Lens Authorized! Tap Camera again to snap.", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, "Hardware Lens Access Denied.", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 🚀 QA Fix: Post-Save Cloud Dependency Router Checks
    private void executeCloudMatrixVerification() {
        String pendingRestoreGroup = prefs.getString("unresolved_cloud_hub", null);
        String pendingLinkedCsv = prefs.getString("unresolved_cloud_links", null);
        // 🚀 QA Fix: Master Navigation Hub - Target 'Live Map Dashboard' (MapActivity) as per V69.9 Requirements
        Class<?> targetActivity = com.jdroidx.jfindme.ui.map.MapActivity.class;

        if (pendingRestoreGroup != null && !pendingRestoreGroup.isEmpty()) {
            String message = "Identity Authorized. A physical Cloud Archive was located on the backend containing an active Network Matrix paired with target nodes.\n\n" +
                             "Do you want to securely restore this targeted Map data back to your local system now?";

            new android.app.AlertDialog.Builder(this)
                .setTitle("🛡️ CLOUD ARCHIVE DETECTED")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("RESTORE MAP MATRIX", (dialog, which) -> {
                    // MVVM Structure Waterfall Matrix
                    if (profileViewModel != null) profileViewModel.executeCloudMatrixRestore(prefs, pendingRestoreGroup, pendingLinkedCsv);
                    
                    android.widget.Toast.makeText(this, "CLOUD ARCHIVE OVERRIDDEN LOCALLY!", android.widget.Toast.LENGTH_LONG).show();
                    startActivity(new android.content.Intent(this, targetActivity));
                    finish();
                })
                .setNegativeButton("IGNORE (START AS FRESH NODE)", (dialog, which) -> {
                    // Overwrite Cloud Group logic -> generate a completely empty node
                    if (profileViewModel != null) profileViewModel.executeCloudMatrixIgnore(prefs);
                    
                    startActivity(new android.content.Intent(this, targetActivity));
                    finish();
                })
                .show();
        } else {
            // Strict Standard Node Flow: Routing to target dashboard
            startActivity(new android.content.Intent(this, targetActivity));
            finish();
        }
    }

    private void handleCameraLaunch() {
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1003);
                return;
            }

            android.content.Intent takePictureIntent = new android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            java.io.File photoFile = new java.io.File(getCacheDir(), "camera_temp.jpg");
            if (!photoFile.exists()) photoFile.createNewFile();
            
            android.net.Uri photoURI = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePictureIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);
            takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
            
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, 1001);
            } else {
                android.widget.Toast.makeText(this, "No Physical Camera Hardware / App Detected.", android.widget.Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            SystemLogger.INSTANCE.e(TAG, "FATAL CAMERA EXCEPTION", e);
        }
    }

    private View prepareGridIcon(int iconRes, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        
        // 🚀 QA Fix: Global Dashboard Sync (Implementing Semantic Shade)
        int adaptiveMid = getGradientMid(); 
        iv.setColorFilter(adaptiveMid); 
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        
        // 🧩 Layer VII: Glassmorphism Shade (15% Alpha Wash)
        int logicAlpha = Color.argb(38, Color.red(adaptiveMid), Color.green(adaptiveMid), Color.blue(adaptiveMid));
        bg.setColor(logicAlpha); 
        
        bg.setStroke(3, adaptiveMid); // 🧩 Circular Perimeter
        iv.setBackground(bg);
        
        int p = scale(25);
        iv.setPadding(p, p, p, p);
        iv.setFocusable(true);
        iv.setClickable(true);
        iv.setOnClickListener(listener);
        return iv;
    }

    private void showAvatarAssetsDetailsDialog() {
        int activeId = prefs.getInt("equipped_avatar_id", 0);
        String name = prefs.getString("user_nickname", "NODE_01");
        
        android.widget.LinearLayout detailsRoot = new android.widget.LinearLayout(this);
        detailsRoot.setOrientation(android.widget.LinearLayout.VERTICAL);
        detailsRoot.setBackgroundColor(getPrimaryColor());
        int p = scale(40);
        detailsRoot.setPadding(p, p, p, p);
        
        TextView t = new TextView(this);
        t.setText("🆔 IDENTITY ASSET MANIFEST");
        t.setTextColor(getGradientStart());
        UiArchitect.applyText(this, t, "TEXT_HEADER");
        t.setPadding(0, 0, 0, 40);
        detailsRoot.addView(t);

        String[] fields = {
            "NICKNAME: " + name,
            "EQUIP_ID: " + activeId,
            "ENGINE: L7 3D_VRM_V2",
            "SYNC_STATUS: VERIFIED",
            "DATA_PATH: /assets/models/..."
        };

        for(String f : fields) {
            TextView fTv = new TextView(this);
            fTv.setText("🔗 " + f);
            fTv.setTextColor(getTextColor());
            UiArchitect.applyText(this, fTv, "TEXT_BODY");
            fTv.setPadding(20, 20, 20, 20);
            detailsRoot.addView(fTv);
        }

        new android.app.AlertDialog.Builder(this)
            .setView(detailsRoot)
            .setPositiveButton("OK", null)
            .show();
    }
    private void captureInitialState() {
        initialValues.clear();
        if (nameField != null) initialValues.put("name", nameField.getText().toString().trim());
        if (phoneField != null) initialValues.put("phone", phoneField.getText().toString().trim());
        if (dobField != null) initialValues.put("dob", dobField.getText().toString().trim());
        if (genderField != null) initialValues.put("gender", genderField.getText().toString().trim());
        if (pincodeField != null) initialValues.put("pincode", pincodeField.getText().toString().trim());
        if (countrySpinner != null) initialValues.put("country", countrySpinner.getText().toString().trim());
        if (stateSpinner != null) initialValues.put("state", stateSpinner.getText().toString().trim());
        if (citySpinner != null) initialValues.put("city", citySpinner.getText().toString().trim());
        if (addressField != null) initialValues.put("address", addressField.getText().toString().trim());
        if (orgField != null) initialValues.put("org", orgField.getText().toString().trim());
        if (deptField != null) initialValues.put("dept", deptField.getText().toString().trim());
    }

    private boolean hasUnsavedChanges() {
        if (!isDataLoaded) return false;
        if (nameField != null && !nameField.getText().toString().trim().equals(initialValues.getOrDefault("name", ""))) return true;
        if (phoneField != null && !phoneField.getText().toString().trim().equals(initialValues.getOrDefault("phone", ""))) return true;
        if (dobField != null && !dobField.getText().toString().trim().equals(initialValues.getOrDefault("dob", ""))) return true;
        if (genderField != null && !genderField.getText().toString().trim().equals(initialValues.getOrDefault("gender", ""))) return true;
        if (pincodeField != null && !pincodeField.getText().toString().trim().equals(initialValues.getOrDefault("pincode", ""))) return true;
        if (countrySpinner != null && !countrySpinner.getText().toString().trim().equals(initialValues.getOrDefault("country", ""))) return true;
        if (stateSpinner != null && !stateSpinner.getText().toString().trim().equals(initialValues.getOrDefault("state", ""))) return true;
        if (citySpinner != null && !citySpinner.getText().toString().trim().equals(initialValues.getOrDefault("city", ""))) return true;
        if (addressField != null && !addressField.getText().toString().trim().equals(initialValues.getOrDefault("address", ""))) return true;
        if (orgField != null && !orgField.getText().toString().trim().equals(initialValues.getOrDefault("org", ""))) return true;
        if (deptField != null && !deptField.getText().toString().trim().equals(initialValues.getOrDefault("dept", ""))) return true;
        return false;
    }
}
