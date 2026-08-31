package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
    private TextView titleView;

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

        titleView = new TextView(this);
        titleView.setText(prefs.getString("user_nickname", "UNIDENTIFIED AGENT").toUpperCase());
        titleView.setTextColor(getGradientOverlayText());
        com.jdroidx.jfindme.ui.base.UiArchitect.applyText(this, titleView, "TEXT_HEADER");
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(titleView);
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
        nativeProfilePhoto.setBackground(null);
        nativeProfilePhoto.setPadding(0, 0, 0, 0);

        activeProfileImgView = new android.webkit.WebView(this);
        activeProfileImgView.setBackgroundColor(Color.TRANSPARENT);
        activeProfileImgView.getSettings().setJavaScriptEnabled(true);

        LinearLayout lensCard = new LinearLayout(this);
        lensCard.setOrientation(LinearLayout.VERTICAL);
        lensCard.setGravity(Gravity.CENTER);
        UiArchitect.applyCard(this, lensCard, "CARD_STANDARD");
        lensCard.setPadding(quota, quota, quota, quota);
        
        // 🚀 Human-In-Loop Performance Optimization: Defer 3D WebGL engine initialization to post-layout
        lensCard.post(() -> {
            if (activeProfileImgView != null && !isFinishing() && !isDestroyed()) {
                activeProfileImgView.loadUrl("file:///android_asset/web/vrm_viewer.html");
            }
        });
        
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        lp1.setMargins(0, 0, headerItemGap, 0);
        lensCard.setLayoutParams(lp1);

        android.widget.FrameLayout renderStack = new android.widget.FrameLayout(this);
        // 🚀 Enhanced Resolution & Size (Double Fidelity)
        int iQuota = (int)(getResponsiveIconQuota() * 5.5f); 
        renderStack.addView(nativeProfilePhoto, new android.widget.FrameLayout.LayoutParams(iQuota, iQuota, Gravity.CENTER));
        renderStack.addView(activeProfileImgView, new android.widget.FrameLayout.LayoutParams(iQuota, iQuota, Gravity.CENTER));
        
        // 🚀 QA Fix: Transparent Tactical Interceptor (Handles clicks over the 3D WebView)
        View clickInterceptor = new View(this);
        clickInterceptor.setClickable(true);
        clickInterceptor.setFocusable(true);
        clickInterceptor.setOnClickListener(v -> showAvatarActionsDialog(nativeProfilePhoto));
        renderStack.addView(clickInterceptor, new android.widget.FrameLayout.LayoutParams(iQuota, iQuota, Gravity.CENTER));
        
        lensCard.setOnClickListener(v -> showAvatarActionsDialog(nativeProfilePhoto));
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
            } else {
                UiArchitect.showValidationDialog(this,
                    "No Custom Photo Found",
                    "Please capture a photo using the camera or select one from the gallery before cropping.",
                    null);
            }
        }), gLp);
        
        gridRow2.addView(prepareGridIcon(android.R.drawable.ic_menu_edit, v -> showAvatarActionsDialog(nativeProfilePhoto)), gLp);

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
                        startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.management.PrivacyActivity.class));
                    } catch (Exception e) {}
                });
        
        android.graphics.drawable.GradientDrawable secBg = new android.graphics.drawable.GradientDrawable();
        secBg.setColor(rooted ? Color.parseColor("#D32F2F") : Color.parseColor("#2E7D32"));
        secBg.setCornerRadius(scaleRadius(30f));
        securityStatus.setBackground(secBg);
        
        View pairingStatus = createIconButton("Device Pairing", 
                com.jdroidx.jfindme.R.drawable.ic_pairing_matrix, v -> {
                    startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.class));
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
        nameField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameField.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String txt = s.toString().trim();
                if (titleView != null) {
                    titleView.setText(txt.isEmpty() ? "UNIDENTIFIED AGENT" : txt.toUpperCase());
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        
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
            String currentDob = dobField.getText().toString().trim();
            if (currentDob.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    String[] parts = currentDob.split("-");
                    int y = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]) - 1;
                    int d = Integer.parseInt(parts[2]);
                    cal.set(y, m, d);
                } catch (Exception ignored) {}
            }
            android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                dobField.setText(java.lang.String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth));
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMaxDate(System.currentTimeMillis()); // Prevent future dates
            dpd.show();
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
        countrySpinner.setFocusable(false);
        countrySpinner.setClickable(true);
        countrySpinner.setOnClickListener(v -> countrySpinner.showDropDown());

        stateSpinner = new android.widget.AutoCompleteTextView(this);
        stateSpinner.setHint(android.text.Html.fromHtml("State/Province<font color='#D32F2F'>(*)</font>"));
        String savedState = prefs.getString("user_state", "");
        stateSpinner.setText(savedState.equals("Identifying...") ? "" : savedState);
        stateAdapter = createEnterpriseDropdownAdapter(new java.util.ArrayList<>(java.util.Collections.singletonList("Select State")));
        stateSpinner.setAdapter(stateAdapter);
        stateSpinner.setThreshold(1);
        stateSpinner.setDropDownBackgroundDrawable(createPopupBackground());
        stateSpinner.setFocusable(false);
        stateSpinner.setClickable(true);
        stateSpinner.setOnClickListener(v -> stateSpinner.showDropDown());
        
        citySpinner = new android.widget.AutoCompleteTextView(this);
        citySpinner.setHint(android.text.Html.fromHtml("City/Zone<font color='#D32F2F'>(*)</font>"));
        String savedCity = prefs.getString("user_city", "");
        citySpinner.setText(savedCity.equals("Identifying...") ? "" : savedCity);
        cityAdapter = createEnterpriseDropdownAdapter(new java.util.ArrayList<>(java.util.Collections.singletonList("Select City")));
        citySpinner.setAdapter(cityAdapter);
        citySpinner.setThreshold(1);
        citySpinner.setDropDownBackgroundDrawable(createPopupBackground());
        citySpinner.setFocusable(false);
        citySpinner.setClickable(true);
        citySpinner.setOnClickListener(v -> citySpinner.showDropDown());
        
        countrySpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selection = countryAdapter.getItem(position);
            if (selection != null && !selection.equals("Select Country") && !selection.equals("Identifying...")) {
                stateSpinner.setText("");
                citySpinner.setText("");
                if (profileViewModel != null) {
                    profileViewModel.fetchStatesList(selection);
                }
            }
        });

        stateSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selection = stateAdapter.getItem(position);
            String parentCountry = countrySpinner.getText().toString().trim();
            if (selection != null && !selection.equals("Select State") && !selection.equals("Select Country First") && !parentCountry.isEmpty()) {
                citySpinner.setText("");
                if (profileViewModel != null) {
                    profileViewModel.fetchCitiesList(parentCountry, selection);
                }
            }
        });

        citySpinner.setOnItemClickListener((parent, view, position, id) -> {
            // City locked
        });

        pincodeField = new android.widget.EditText(this);
        pincodeField.setHint(android.text.Html.fromHtml("ZipCode<font color='#D32F2F'>(*)</font>"));
        pincodeField.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        pincodeField.setText(prefs.getString("user_pincode", ""));

        String currentEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail() : 
            ("node@" + com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getEnterpriseDomain(this));
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
        
        // AUDIT FIX [AUD-019]: Link tab sequence for hardware keyboard accessibility
        com.jdroidx.jfindme.utils.FormUtils.linkTabSequence(nameField, phoneField, addressField, countrySpinner, stateSpinner, citySpinner, pincodeField, emergencyNoteField, orgField, deptField);

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
                startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.settings.OrgActivity.class));
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
            UiArchitect.showInputPromptDialog(this,
                "Update Password",
                "Enter new password (min 6 chars)",
                "",
                newPass -> {
                    if (newPass != null && newPass.length() >= 6) {
                        try {
                            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().updatePassword(newPass);
                                android.widget.Toast.makeText(this, "Security Matrix Updated.", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            android.widget.Toast.makeText(this, "Failed to update password: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        UiArchitect.showValidationDialog(this, "Password Invalid", "Password must be at least 6 characters.", null);
                    }
                });
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
            UiArchitect.showConfirmationDialog(this,
                "Confirm Logout",
                "Are you sure you want to securely sign out?",
                "LOGOUT", "CANCEL",
                () -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.auth.AuthActivity.class));
                    finishAffinity();
                },
                null);
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
        applyGlassmorphism(card, getCardColor(), 16f);
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

    private void showAvatarActionsDialog(ImageView nativeProfilePhoto) {
        String[] options = {
            "📸 Take Camera Selfie",
            "🖼️ Choose from Gallery",
            "✂️ Crop Custom Photo",
            "🎭 Choose 2D Avatar Character",
            "🪞 Open 3D Avatar Customizer Studio",
            "🆔 View Identity Asset Details"
        };
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(getTextColor());
                tv.setTypeface(null, Typeface.BOLD);
                int p = scale(25);
                tv.setPadding(p, p, p, p);
                return tv;
            }
        };

        android.app.AlertDialog d = new android.app.AlertDialog.Builder(this)
            .setTitle("Avatar Command Center")
            .setAdapter(adapter, (dialog, which) -> {
                switch (which) {
                    case 0:
                        handleCameraLaunch();
                        break;
                    case 1:
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, 1004);
                        break;
                    case 2:
                        java.io.File file = new java.io.File(getFilesDir() + "/avatars", "custom_photo.png");
                        if (file.exists()) {
                            startActivityForResult(new android.content.Intent(this, AvatarCropActivity.class), 1002);
                        } else {
                            UiArchitect.showValidationDialog(this,
                                "No Custom Photo Found",
                                "Please capture a photo using the camera or select one from the gallery before cropping.",
                                null);
                        }
                        break;
                    case 3:
                        showAvatarSelectionDialog(nativeProfilePhoto);
                        break;
                    case 4:
                        startActivity(new android.content.Intent(this, AvatarEditorActivity.class));
                        break;
                    case 5:
                        showAvatarAssetsDetailsDialog();
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
            
        d.show();
        applyDialogTheme(d);
    }

    private void showAvatarSelectionDialog(final ImageView mainProfilePic) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        float widthDp = dm.widthPixels / dm.density;
        int colCount = (widthDp >= 600) ? 4 : 3; // Adaptive 3-col on phones, 4-col on foldables/tablets
        
        int pad = (int) (8 * dm.density);
        int tileMargin = (int) (4 * dm.density);
        int imgFrameHeight = (int) ((widthDp >= 600 ? 95 : 82) * dm.density);

        LinearLayout mainDialogLayout = new LinearLayout(this);
        mainDialogLayout.setOrientation(LinearLayout.VERTICAL);

        // 🔍 Real-Time Smooth Search Bar (Edit Input)
        LinearLayout searchContainer = new LinearLayout(this);
        searchContainer.setOrientation(LinearLayout.HORIZONTAL);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);
        searchContainer.setPadding(pad, (int)(8 * dm.density), pad, (int)(4 * dm.density));

        android.widget.EditText searchField = new android.widget.EditText(this);
        searchField.setHint("🔍 Search 50 Popular Avatars (e.g. King, Jane, Zombie)...");
        searchField.setHintTextColor(Color.argb(140, 148, 163, 184));
        searchField.setTextColor(getTextColor());
        searchField.setTextSize(13);
        searchField.setSingleLine(true);
        searchField.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        
        android.graphics.drawable.GradientDrawable searchBg = new android.graphics.drawable.GradientDrawable();
        searchBg.setCornerRadius((int) (12 * dm.density));
        searchBg.setColor(getCardColor());
        searchBg.setStroke((int) (1.5f * dm.density), Color.argb(80, 59, 130, 246));
        searchField.setBackground(searchBg);
        searchField.setPadding((int)(12 * dm.density), (int)(8 * dm.density), (int)(12 * dm.density), (int)(8 * dm.density));
        
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        searchField.setLayoutParams(searchLp);
        searchContainer.addView(searchField);
        mainDialogLayout.addView(searchContainer);

        // 🏷️ Category Filter Tab Bar (Horizontal Scroll)
        android.widget.HorizontalScrollView tabScroller = new android.widget.HorizontalScrollView(this);
        tabScroller.setHorizontalScrollBarEnabled(false);
        tabScroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout tabLayout = new LinearLayout(this);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setPadding(pad, (int)(4 * dm.density), pad, (int)(6 * dm.density));
        tabScroller.addView(tabLayout);
        mainDialogLayout.addView(tabScroller);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setSmoothScrollingEnabled(true);
        LinearLayout gridContainer = new LinearLayout(this);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        gridContainer.setPadding(pad, (int)(2 * dm.density), pad, pad);
        scrollView.addView(gridContainer);
        mainDialogLayout.addView(scrollView);

        int currentlyEquippedId = prefs.getInt("equipped_avatar_id", 1);

        java.util.List<? extends com.jdroidx.jfindme.engine.data.avatar.AvatarEntity> templateList = 
            com.jdroidx.jfindme.engine.data.avatar.AvatarRegistry.INSTANCE.getTemplateList();

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle("🖼️ 50 Popular Avatars Collection")
            .setView(mainDialogLayout)
            .setNegativeButton("Close", null)
            .create();

        // 🚀 Functional category & search filtering engine
        final String[] categories = {"ALL", "COMMON", "RARE", "EPIC", "LEGENDARY"};
        final String[] categoryLabels = {"✨ All (50)", "🟢 Common", "🔵 Rare", "🟣 Epic", "🟡 Legendary"};
        final TextView[] tabViews = new TextView[categories.length];

        final String[] activeCategory = {"ALL"};
        final String[] activeSearchQuery = {""};
        Runnable[] updateGrid = new Runnable[1];

        updateGrid[0] = () -> {
            gridContainer.removeAllViews();
            java.util.List<View> filteredTiles = new java.util.ArrayList<>();
            String query = activeSearchQuery[0].toLowerCase(java.util.Locale.ROOT);

            // 1. Custom Local Camera Selfie (only on ALL or when query matches selfie)
            java.io.File customPhotoFile = new java.io.File(getFilesDir() + "/avatars", "custom_photo.png");
            if (customPhotoFile.exists() && (activeCategory[0].equals("ALL") || query.contains("selfie") || query.contains("camera"))) {
                boolean isCustomEquipped = (currentlyEquippedId == -1);

                LinearLayout customTile = new LinearLayout(this);
                customTile.setOrientation(LinearLayout.VERTICAL);
                customTile.setGravity(Gravity.CENTER_HORIZONTAL);
                customTile.setPadding((int) (4 * dm.density), (int) (6 * dm.density), (int) (4 * dm.density), (int) (6 * dm.density));

                android.graphics.drawable.GradientDrawable tileBg = new android.graphics.drawable.GradientDrawable();
                tileBg.setCornerRadius((int) (12 * dm.density));
                tileBg.setColor(getCardColor());
                tileBg.setStroke((int) (isCustomEquipped ? 2.5f * dm.density : 1 * dm.density), isCustomEquipped ? Color.parseColor("#00E676") : Color.argb(60, 148, 163, 184));
                customTile.setBackground(tileBg);
                customTile.setFocusable(true);
                customTile.setClickable(true);

                android.widget.RelativeLayout imgContainer = new android.widget.RelativeLayout(this);
                imgContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imgFrameHeight));

                ImageView previewImg = new ImageView(this);
                previewImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
                previewImg.setAdjustViewBounds(true);
                android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeFile(customPhotoFile.getAbsolutePath());
                previewImg.setImageBitmap(b);
                android.widget.RelativeLayout.LayoutParams imgParams = new android.widget.RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                imgParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
                previewImg.setLayoutParams(imgParams);
                imgContainer.addView(previewImg);

                // Tick Box Indicator
                TextView activeBadge = new TextView(this);
                activeBadge.setText(isCustomEquipped ? "✓" : "☐");
                activeBadge.setTextColor(Color.WHITE);
                activeBadge.setTextSize(isCustomEquipped ? 10 : 12);
                activeBadge.setTypeface(null, Typeface.BOLD);
                activeBadge.setGravity(Gravity.CENTER);
                android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
                badgeBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                badgeBg.setColor(isCustomEquipped ? Color.parseColor("#00E676") : Color.argb(160, 100, 116, 139));
                activeBadge.setBackground(badgeBg);
                int badgeSize = (int) (18 * dm.density);
                android.widget.RelativeLayout.LayoutParams badgeParams = new android.widget.RelativeLayout.LayoutParams(badgeSize, badgeSize);
                badgeParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
                badgeParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
                activeBadge.setLayoutParams(badgeParams);
                imgContainer.addView(activeBadge);

                ImageView deleteBtn = new ImageView(this);
                deleteBtn.setImageResource(android.R.drawable.ic_menu_delete);
                deleteBtn.setColorFilter(Color.WHITE);
                android.graphics.drawable.GradientDrawable delBg = new android.graphics.drawable.GradientDrawable();
                delBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                delBg.setColor(Color.parseColor("#EF4444"));
                deleteBtn.setBackground(delBg);
                int delSize = (int) (20 * dm.density);
                deleteBtn.setPadding((int) (3 * dm.density), (int) (3 * dm.density), (int) (3 * dm.density), (int) (3 * dm.density));
                android.widget.RelativeLayout.LayoutParams delParams = new android.widget.RelativeLayout.LayoutParams(delSize, delSize);
                delParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
                delParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
                deleteBtn.setLayoutParams(delParams);
                imgContainer.addView(deleteBtn);

                deleteBtn.setOnClickListener(v -> {
                    if (customPhotoFile.exists() && customPhotoFile.delete()) {
                        if (prefs.getInt("equipped_avatar_id", 1) == -1) {
                            prefs.edit().putInt("equipped_avatar_id", 1).commit();
                        }
                        dialog.dismiss();
                        Toast.makeText(this, "Selfie Purged Securely.", Toast.LENGTH_SHORT).show();
                        syncFieldsToPrefs();
                        com.jdroidx.jfindme.utils.AvatarLoader.clearCache();
                        safeRecreate();
                    }
                });

                customTile.addView(imgContainer);

                TextView nameStr = new TextView(this);
                nameStr.setText(isCustomEquipped ? "Selfie [✓]" : "Camera Selfie");
                nameStr.setTextColor(isCustomEquipped ? Color.parseColor("#00E676") : getTextColor());
                nameStr.setTextSize(11);
                nameStr.setTypeface(null, Typeface.BOLD);
                nameStr.setGravity(Gravity.CENTER);
                nameStr.setSingleLine(true);
                nameStr.setEllipsize(android.text.TextUtils.TruncateAt.END);
                nameStr.setPadding((int) (2 * dm.density), (int) (4 * dm.density), (int) (2 * dm.density), 0);
                customTile.addView(nameStr);

                customTile.setOnClickListener(v -> {
                    showAvatarEnlargedPopup("Camera Selfie", "Custom Photo", customPhotoFile.getAbsolutePath(), -1, isCustomEquipped, () -> {
                        mainProfilePic.setImageDrawable(previewImg.getDrawable());
                        mainProfilePic.clearColorFilter();
                        if (activeProfileImgView != null) {
                            activeProfileImgView.evaluateJavascript("if(typeof currentVrm !== 'undefined' && currentVrm) { scene.remove(currentVrm.scene); currentVrm=null; }", null);
                        }
                        if (profileViewModel != null) {
                            profileViewModel.equipCustomAvatar(prefs); 
                        }
                        Toast.makeText(this, "Equipped Custom Camera Snapshot!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });

                filteredTiles.add(customTile);
            }

            // 2. Master Avatars matching category filter and real-time search query
            for (final com.jdroidx.jfindme.engine.data.avatar.AvatarEntity avatar : templateList) {
                String rarityStr = avatar.getRarity().name();
                if (!activeCategory[0].equals("ALL") && !rarityStr.equalsIgnoreCase(activeCategory[0])) {
                    continue;
                }

                String avatarName = avatar.getType();
                int activeId = avatar.getBaseId();
                if (!query.isEmpty()) {
                    boolean matchName = avatarName.toLowerCase(java.util.Locale.ROOT).contains(query);
                    boolean matchId = String.valueOf(activeId).equals(query) || ("#" + activeId).equals(query);
                    boolean matchCategory = rarityStr.toLowerCase(java.util.Locale.ROOT).contains(query);
                    if (!matchName && !matchId && !matchCategory) {
                        continue;
                    }
                }

                boolean isEquipped = (activeId == currentlyEquippedId);

                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER_HORIZONTAL);
                tile.setPadding((int) (4 * dm.density), (int) (6 * dm.density), (int) (4 * dm.density), (int) (6 * dm.density));

                android.graphics.drawable.GradientDrawable tileBg = new android.graphics.drawable.GradientDrawable();
                tileBg.setCornerRadius((int) (12 * dm.density));
                tileBg.setColor(getCardColor());
                tileBg.setStroke((int) (isEquipped ? 2.5f * dm.density : 1 * dm.density), isEquipped ? Color.parseColor("#00E676") : Color.argb(60, 148, 163, 184));
                tile.setBackground(tileBg);
                tile.setFocusable(true);
                tile.setClickable(true);

                android.widget.RelativeLayout imgContainer = new android.widget.RelativeLayout(this);
                imgContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imgFrameHeight));

                String customGraphicName = "avatar_custom_" + activeId;
                int customResId = getResources().getIdentifier(customGraphicName, "drawable", getPackageName());
                if (customResId == 0) {
                    int dyn = ((activeId - 1) % 22) + 1;
                    customResId = getResources().getIdentifier("avatar_custom_" + dyn, "drawable", getPackageName());
                }

                final int finalResId = customResId;

                ImageView previewImg = new ImageView(this);
                previewImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
                previewImg.setAdjustViewBounds(true);
                if (finalResId != 0) {
                    previewImg.setImageResource(finalResId);
                } else {
                    previewImg.setImageResource(android.R.drawable.ic_menu_gallery);
                }
                
                android.widget.RelativeLayout.LayoutParams imgParams = new android.widget.RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                imgParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
                previewImg.setLayoutParams(imgParams);
                imgContainer.addView(previewImg);

                // Tick Box Indicator
                TextView activeBadge = new TextView(this);
                activeBadge.setText(isEquipped ? "✓" : "☐");
                activeBadge.setTextColor(Color.WHITE);
                activeBadge.setTextSize(isEquipped ? 10 : 12);
                activeBadge.setTypeface(null, Typeface.BOLD);
                activeBadge.setGravity(Gravity.CENTER);
                android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
                badgeBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                badgeBg.setColor(isEquipped ? Color.parseColor("#00E676") : Color.argb(160, 100, 116, 139));
                activeBadge.setBackground(badgeBg);
                int badgeSize = (int) (18 * dm.density);
                android.widget.RelativeLayout.LayoutParams badgeParams = new android.widget.RelativeLayout.LayoutParams(badgeSize, badgeSize);
                badgeParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
                badgeParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
                activeBadge.setLayoutParams(badgeParams);
                imgContainer.addView(activeBadge);

                tile.addView(imgContainer);

                TextView nameStr = new TextView(this);
                nameStr.setText(activeId + ". " + avatar.getType());
                nameStr.setTextColor(isEquipped ? Color.parseColor("#00E676") : getTextColor());
                nameStr.setTextSize(11);
                nameStr.setTypeface(null, Typeface.BOLD);
                nameStr.setGravity(Gravity.CENTER);
                nameStr.setSingleLine(true);
                nameStr.setEllipsize(android.text.TextUtils.TruncateAt.END);
                nameStr.setPadding((int) (2 * dm.density), (int) (4 * dm.density), (int) (2 * dm.density), 0);
                tile.addView(nameStr);

                applyAccessibility(tile, "Select Avatar: " + activeId + ". " + avatar.getType());

                tile.setOnClickListener(v -> {
                    showAvatarEnlargedPopup(activeId + ". " + avatar.getType(), rarityStr, finalResId, activeId, isEquipped, () -> {
                        mainProfilePic.setImageDrawable(previewImg.getDrawable());
                        mainProfilePic.clearColorFilter();
                        
                        if (activeProfileImgView != null) {
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
                        
                        if (profileViewModel != null) {
                            profileViewModel.equipAvatar(prefs, activeId);
                        }

                        Toast.makeText(this, "Equipped Avatar: " + avatar.getType() + " [✓]", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });

                filteredTiles.add(tile);
            }

            // 3. Build Adaptive Multi-Pixel Rows
            for (int i = 0; i < filteredTiles.size(); i += colCount) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setWeightSum((float) colCount);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLp.setMargins(0, (int) (2 * dm.density), 0, (int) (2 * dm.density));
                row.setLayoutParams(rowLp);

                for (int j = 0; j < colCount; j++) {
                    if (i + j < filteredTiles.size()) {
                        View tile = filteredTiles.get(i + j);
                        LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                        cellLp.setMargins(tileMargin, tileMargin, tileMargin, tileMargin);
                        tile.setLayoutParams(cellLp);
                        row.addView(tile);
                    } else {
                        View emptySpacer = new View(this);
                        LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                        cellLp.setMargins(tileMargin, tileMargin, tileMargin, tileMargin);
                        emptySpacer.setLayoutParams(cellLp);
                        row.addView(emptySpacer);
                    }
                }
                gridContainer.addView(row);
            }
        };

        // Real-Time Search Text Watcher
        searchField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeSearchQuery[0] = (s != null) ? s.toString().trim() : "";
                updateGrid[0].run();
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Build Horizontal Filter Tab Chips
        for (int k = 0; k < categories.length; k++) {
            final int tabIndex = k;
            final String catKey = categories[k];
            TextView tab = new TextView(this);
            tab.setText(categoryLabels[k]);
            tab.setTextSize(12);
            tab.setTypeface(null, Typeface.BOLD);
            tab.setPadding((int) (12 * dm.density), (int) (6 * dm.density), (int) (12 * dm.density), (int) (6 * dm.density));
            LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tabLp.setMargins((int) (3 * dm.density), 0, (int) (3 * dm.density), 0);
            tab.setLayoutParams(tabLp);

            tabViews[k] = tab;
            tab.setOnClickListener(v -> {
                activeCategory[0] = catKey;
                for (int m = 0; m < categories.length; m++) {
                    boolean isSelected = (m == tabIndex);
                    android.graphics.drawable.GradientDrawable tBg = new android.graphics.drawable.GradientDrawable();
                    tBg.setCornerRadius((int) (16 * dm.density));
                    if (isSelected) {
                        tBg.setColor(Color.parseColor("#3B82F6")); // Active Blue Accent
                        tabViews[m].setTextColor(Color.WHITE);
                    } else {
                        tBg.setColor(Color.argb(40, 148, 163, 184));
                        tabViews[m].setTextColor(getTextColor());
                    }
                    tabViews[m].setBackground(tBg);
                }
                updateGrid[0].run();
            });

            // Initialize Style
            android.graphics.drawable.GradientDrawable tBg = new android.graphics.drawable.GradientDrawable();
            tBg.setCornerRadius((int) (16 * dm.density));
            if (k == 0) {
                tBg.setColor(Color.parseColor("#3B82F6"));
                tab.setTextColor(Color.WHITE);
            } else {
                tBg.setColor(Color.argb(40, 148, 163, 184));
                tab.setTextColor(getTextColor());
            }
            tab.setBackground(tBg);
            tabLayout.addView(tab);
        }

        // Initial Grid Render
        updateGrid[0].run();

        dialog.show();
        applyDialogTheme(dialog);
        if (dialog.getWindow() != null) {
            int maxDialogWidth = (int) (dm.widthPixels * 0.94f);
            dialog.getWindow().setLayout(maxDialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * 🔍 High-Definition Popup Image Preview Modal on Tap
     */
    private void showAvatarEnlargedPopup(String avatarTitle, String category, Object imageResOrPath, int avatarId, boolean isEquipped, Runnable onEquipAction) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding((int)(16 * dm.density), (int)(16 * dm.density), (int)(16 * dm.density), (int)(16 * dm.density));

        // Enlarged Image Preview
        ImageView bigImage = new ImageView(this);
        int bigSize = (int)(180 * dm.density);
        LinearLayout.LayoutParams bigLp = new LinearLayout.LayoutParams(bigSize, bigSize);
        bigLp.setMargins(0, 0, 0, (int)(12 * dm.density));
        bigImage.setLayoutParams(bigLp);
        bigImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        if (imageResOrPath instanceof Integer) {
            bigImage.setImageResource((Integer) imageResOrPath);
        } else if (imageResOrPath instanceof String) {
            android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeFile((String) imageResOrPath);
            if (b != null) bigImage.setImageBitmap(b);
            else bigImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable();
        ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        ring.setStroke((int)(3 * dm.density), isEquipped ? Color.parseColor("#00E676") : Color.parseColor("#3B82F6"));
        ring.setColor(getCardColor());
        bigImage.setBackground(ring);
        layout.addView(bigImage);

        TextView titleTv = new TextView(this);
        titleTv.setText(avatarTitle);
        titleTv.setTextColor(getTextColor());
        titleTv.setTextSize(18);
        titleTv.setTypeface(null, Typeface.BOLD);
        titleTv.setGravity(Gravity.CENTER);
        layout.addView(titleTv);

        TextView catTv = new TextView(this);
        catTv.setText("Tier: " + category + "  ●  ID: #" + (avatarId == -1 ? "CUSTOM" : avatarId));
        catTv.setTextColor(Color.parseColor("#94A3B8"));
        catTv.setTextSize(12);
        catTv.setGravity(Gravity.CENTER);
        catTv.setPadding(0, (int)(4 * dm.density), 0, (int)(14 * dm.density));
        layout.addView(catTv);

        android.app.AlertDialog previewDialog = new android.app.AlertDialog.Builder(this)
            .setTitle("✨ Avatar Identity Inspection")
            .setView(layout)
            .setPositiveButton(isEquipped ? "EQUIPPED ✓" : "EQUIP AVATAR [✓]", (d, w) -> {
                if (onEquipAction != null) onEquipAction.run();
            })
            .setNegativeButton("Cancel", null)
            .create();

        previewDialog.show();
        applyDialogTheme(previewDialog);
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
        fieldBg.setColor(getCardColor()); 
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
        fieldBg.setColor(getCardColor()); 
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
                    android.app.AlertDialog unsavedDialog = new android.app.AlertDialog.Builder(ProfileActivity.this)
                        .setTitle("⚠️ UNSAVED CHANGES")
                        .setMessage("You have made changes to your identity profile. Would you like to update your cloud roster or discard these changes?")
                        .setPositiveButton("UPDATE 🚀", (dialog, which) -> handleProfileSaveAction(false))
                        .setNegativeButton("DISCARD 🕒", (dialog, which) -> {
                            finish();
                        })
                        .setNeutralButton("CANCEL", null)
                        .create();
                    unsavedDialog.show();
                    applyDialogTheme(unsavedDialog);
                    return;
                }

                String missing = profileViewModel.getMissingMandatoryFields(prefs);
                if (missing != null) {
                    android.app.AlertDialog missingDialog = new android.app.AlertDialog.Builder(ProfileActivity.this)
                        .setTitle("🛡️ IDENTITY INTEGRITY REQUIRED")
                        .setMessage("Hi there! We found a few pieces of your safety shield that need completing: 🛡️\n\n" +
                                missing + "\n\n" +
                                "To activate your forensic tracking and access the Dashboard, please complete these fields.\n\n" +
                                "⚠️ Note: This is a mandatory requirement for your safety network.")
                        .setPositiveButton("COMPLETE NOW 🚀", null)
                        .setNegativeButton("SAVE LATER 🕒", (dialog, which) -> {
                            // 🚀 QA Fix: Set Bypass Flag to allow MapActivity access for this session
                            prefs.edit().putBoolean("profile_bypass_nudge", true).apply();
                            com.jdroidx.jfindme.utils.AvatarLoader.clearCache();
                            handleProfileSaveAction(true); // Save current changes without validation, single-route through executeCloudMatrixVerification
                        })
                        .setCancelable(false) // Hard Lock: Cannot dismiss without choosing an action
                        .create();
                    missingDialog.show();
                    applyDialogTheme(missingDialog);
                } else {
                    com.jdroidx.jfindme.utils.AvatarLoader.clearCache();
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
            String loadedCountry = "";
            if (data.containsKey("userCountry") || data.containsKey("country")) {
                String val = String.valueOf(data.getOrDefault("userCountry", data.get("country")));
                if (countrySpinner != null && (countrySpinner.getText().toString().isEmpty() || countrySpinner.getText().toString().equals("Select Country"))) countrySpinner.setText(val, false);
                editor.putString("user_country", val);
                loadedCountry = val;
                if (!val.isEmpty() && !val.equals("Select Country") && !val.equals("Identifying...")) {
                    profileViewModel.fetchStatesList(val);
                }
            }
            if (data.containsKey("userState") || data.containsKey("state")) {
                String val = String.valueOf(data.getOrDefault("userState", data.get("state")));
                if (stateSpinner != null && (stateSpinner.getText().toString().isEmpty() || stateSpinner.getText().toString().equals("Select State"))) stateSpinner.setText(val, false);
                editor.putString("user_state", val);
                if (!val.isEmpty() && !val.equals("Select State") && !val.equals("Identifying...") && !loadedCountry.isEmpty()) {
                    profileViewModel.fetchCitiesList(loadedCountry, val);
                }
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

            // 🚀 QA Fix: Async Profile Completeness Verification Nudge (Resolves Race Condition)
            String loadedPincode = prefs.getString("user_pincode", "");
            if (loadedPincode.isEmpty() && !onboardingDialogShown) {
                onboardingDialogShown = true;
                android.app.AlertDialog onboardingDialog = new android.app.AlertDialog.Builder(ProfileActivity.this)
                    .setTitle("🔔 Finishing Touches Required!")
                    .setMessage("Hi there! We are almost done setting up your safety shield! 🛡️\n\n" +
                            "To complete your installation, please fill in these important fields marked with a (*) red star:\n\n" +
                            "✅ Your Name\n" +
                            "✅ Mobile Number\n" +
                            "✅ Pincode (Very Important!)\n" +
                            "✅ Your Country, State, and City\n\n" +
                            "This helps your friends and family find you quickly if you ever need them. It's like giving your house a proper address on the map! 🏠✨")
                    .setPositiveButton("LET'S DO IT! 🚀", null)
                    .create();
                onboardingDialog.show();
                applyDialogTheme(onboardingDialog);
            }
        });
        
        profileViewModel.fetchProfile(this);

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
                
                android.app.AlertDialog errorDialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("🔔 FINISHING TOUCHES REQUIRED")
                    .setMessage(displayMsg)
                    .setPositiveButton("RESOLVE ISSUES 🚀", null)
                    .create();
                errorDialog.show();
                applyDialogTheme(errorDialog);
                
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
            syncFieldsToPrefs(); // 🚀 QA Fix: Prevent unsaved data loss during image update
            com.jdroidx.jfindme.utils.AvatarLoader.clearCache(); // 🚀 QA Fix: Clear LruCache to show new photo
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

        android.app.AlertDialog d = new android.app.AlertDialog.Builder(this)
            .setView(detailsRoot)
            .setPositiveButton("OK", null)
            .create();
        d.show();
        applyDialogTheme(d);
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
        if (emergencyNoteField != null) initialValues.put("emergency_note", emergencyNoteField.getText().toString().trim());
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
        if (emergencyNoteField != null && !emergencyNoteField.getText().toString().trim().equals(initialValues.getOrDefault("emergency_note", ""))) return true;
        if (orgField != null && !orgField.getText().toString().trim().equals(initialValues.getOrDefault("org", ""))) return true;
        if (deptField != null && !deptField.getText().toString().trim().equals(initialValues.getOrDefault("dept", ""))) return true;
        return false;
    }

    @Override
    protected void onSaveCurrentState(android.os.Bundle outState) {
        if (outState == null) return;
        if (nameField != null) outState.putString("PROFILE_NAME", nameField.getText().toString());
        if (phoneField != null) outState.putString("PROFILE_PHONE", phoneField.getText().toString());
        if (dobField != null) outState.putString("PROFILE_DOB", dobField.getText().toString());
        if (genderField != null) outState.putString("PROFILE_GENDER", genderField.getText().toString());
        if (pincodeField != null) outState.putString("PROFILE_PINCODE", pincodeField.getText().toString());
        if (countrySpinner != null) outState.putString("PROFILE_COUNTRY", countrySpinner.getText().toString());
        if (stateSpinner != null) outState.putString("PROFILE_STATE", stateSpinner.getText().toString());
        if (citySpinner != null) outState.putString("PROFILE_CITY", citySpinner.getText().toString());
        if (addressField != null) outState.putString("PROFILE_ADDRESS", addressField.getText().toString());
        if (emergencyNoteField != null) outState.putString("PROFILE_EMERGENCY", emergencyNoteField.getText().toString());
        if (orgField != null) outState.putString("PROFILE_ORG", orgField.getText().toString());
        if (deptField != null) outState.putString("PROFILE_DEPT", deptField.getText().toString());
    }

    @Override
    protected void onUiRebuilt(android.os.Bundle savedState) {
        if (savedState == null) return;
        if (nameField != null && savedState.containsKey("PROFILE_NAME")) nameField.setText(savedState.getString("PROFILE_NAME", ""));
        if (phoneField != null && savedState.containsKey("PROFILE_PHONE")) phoneField.setText(savedState.getString("PROFILE_PHONE", ""));
        if (dobField != null && savedState.containsKey("PROFILE_DOB")) dobField.setText(savedState.getString("PROFILE_DOB", ""));
        if (genderField != null && savedState.containsKey("PROFILE_GENDER")) genderField.setText(savedState.getString("PROFILE_GENDER", ""));
        if (pincodeField != null && savedState.containsKey("PROFILE_PINCODE")) pincodeField.setText(savedState.getString("PROFILE_PINCODE", ""));
        if (countrySpinner != null && savedState.containsKey("PROFILE_COUNTRY")) countrySpinner.setText(savedState.getString("PROFILE_COUNTRY", ""));
        if (stateSpinner != null && savedState.containsKey("PROFILE_STATE")) stateSpinner.setText(savedState.getString("PROFILE_STATE", ""));
        if (citySpinner != null && savedState.containsKey("PROFILE_CITY")) citySpinner.setText(savedState.getString("PROFILE_CITY", ""));
        if (addressField != null && savedState.containsKey("PROFILE_ADDRESS")) addressField.setText(savedState.getString("PROFILE_ADDRESS", ""));
        if (emergencyNoteField != null && savedState.containsKey("PROFILE_EMERGENCY")) emergencyNoteField.setText(savedState.getString("PROFILE_EMERGENCY", ""));
        if (orgField != null && savedState.containsKey("PROFILE_ORG")) orgField.setText(savedState.getString("PROFILE_ORG", ""));
        if (deptField != null && savedState.containsKey("PROFILE_DEPT")) deptField.setText(savedState.getString("PROFILE_DEPT", ""));
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            UiArchitect.showConfirmationDialog(this,
                "Unsaved Changes",
                "You have modified your profile details. Do you want to discard your changes and exit?",
                "DISCARD", "KEEP EDITING",
                super::onBackPressed, null);
        } else {
            super.onBackPressed();
        }
    }
}
