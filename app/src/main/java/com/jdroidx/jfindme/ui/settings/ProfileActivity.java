package com.jdroidx.jfindme.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.group.GroupActivity;
import com.jdroidx.jfindme.ui.group.PersonDetailsActivity;
import com.jdroidx.jfindme.ui.management.PrivacyActivity;
import com.jdroidx.jfindme.utils.AngelLoader;
import com.jdroidx.jfindme.utils.AvatarLoader;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.BottomNavTable;
import com.jdroidx.jfindme.ui.settings.OrgActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 🚀 CITIZEN HUB: Full UI & Logic Restoration (V63)
 * Features: Maximized 420px Hub, Front Camera Protocol, Multi-Identity Library, 3D Workspace Link.
 */
public class ProfileActivity extends BaseActivityJava {

    private static final int REQUEST_IMAGE_CAPTURE = 1101;
    private static final int REQUEST_IMAGE_PICK = 1102;

    private ProfileViewModel profileViewModel;
    private SharedPreferences prefs;

    private EditText nameField, phoneField, dobField, pincodeField, addressField, emergencyNoteField, orgField, deptField, emailField;
    private AutoCompleteTextView genderField, countrySpinner, stateSpinner, citySpinner;
    private ImageView mainProfilePic;
    private TextView headerNickname;
    private boolean isSyncInProgress = false;
    private View btnActionSave;
    
    // 🏠 State Storage
    private String c_name = "", c_phone = "", c_dob = "", c_email = "", c_pin = "", c_addr = "", c_note = "", c_org = "", c_dept = "";

    @Override
    protected View createLayout() {
        prefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        root.addView(createProfileHubHeader());

        ScrollView scroll = new ScrollView(this);
        scroll.setScrollBarSize(0);
        scroll.setClipToPadding(false);
        
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(scale(25), scale(20), scale(25), scale(120));
        
        // 🧪 Card Architecture (Tinted Background via Global UI Architect)
        LinearLayout avatarCard = new LinearLayout(this);
        avatarCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(avatarCard, Color.parseColor("#F5F9FF"), 28f); // Soft L7 Blue Tint
        avatarCard.addView(createAvatarHubMatrix());
        content.addView(avatarCard);

        addSectionTitle(content, "SYSTEM INTEGRITY & HARDWARE LINKS");
        content.addView(createIntegrityControlRow());

        addSectionTitle(content, "PERSONAL IDENTITY & SECURITY");
        String pEmail = prefs.getString("email", "node_identity@jfindme.net");
        emailField = createGhostBox(content, pEmail, android.R.drawable.ic_media_play, true);
        emailField.setText(pEmail);
        
        LinearLayout row1 = createDualRow();
        String pNick = prefs.getString("user_nickname", "AGENT_NICKNAME");
        nameField = createGhostBox(row1, pNick, android.R.drawable.ic_menu_edit, false);
        nameField.setHint(getMandatoryHint("AGENT NICKNAME"));
        nameField.setText(pNick);
        genderField = createGhostDropdown(row1, "Select Gender", android.R.drawable.ic_menu_info_details);
        content.addView(row1);

        LinearLayout row2 = createDualRow();
        phoneField = createGhostBox(row2, "Mobile Number", android.R.drawable.ic_menu_call, false);
        phoneField.setHint(getMandatoryHint("Mobile Number"));
        dobField = createGhostBox(row2, "BirthDay", android.R.drawable.ic_menu_today, false);
        dobField.setHint(getMandatoryHint("BirthDay"));
        content.addView(row2);

        addressField = createGhostBox(content, "Primary Residence Address", android.R.drawable.ic_menu_directions, true);

        LinearLayout row3 = createDualRow();
        countrySpinner = createGhostDropdown(row3, "Country", android.R.drawable.ic_menu_mylocation);
        countrySpinner.setHint(getMandatoryHint("Country"));
        stateSpinner = createGhostDropdown(row3, "State/Province", android.R.drawable.ic_menu_compass);
        stateSpinner.setHint(getMandatoryHint("State/Province"));
        content.addView(row3);

        LinearLayout row4 = createDualRow();
        citySpinner = createGhostDropdown(row4, "City/Zone", android.R.drawable.ic_dialog_map);
        citySpinner.setHint(getMandatoryHint("City/Zone"));
        pincodeField = createGhostBox(row4, "ZipCode", android.R.drawable.ic_menu_info_details, false);
        pincodeField.setHint(getMandatoryHint("ZipCode"));
        content.addView(row4);

        emergencyNoteField = createGhostBox(content, "Emergency Action Protocol / Medical Notes", android.R.drawable.ic_dialog_info, true);
        orgField = createGhostBox(content, "Organization / Hub Name", android.R.drawable.ic_menu_agenda, true);
        deptField = createGhostBox(content, "Department / Sector", android.R.drawable.ic_menu_view, true);

        addSectionTitle(content, "SYSTEM ACTIONS & SECURITY");
        content.addView(createSystemActionGrid());
        
        content.addView(createVerticalSpacer(scale(30)));
        content.addView(createLogoutSessionButton());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 🚀 QA Fix: Global Navigation Integrity
        String[] keys = {"NAV_NOTICE", "NAV_ALERT", "NAV_DEVICE", "NAV_ME"};
        root.addView(UiArchitect.createBottomNav(this, keys, "NAV_ME"));

        return root;
    }

    private View createProfileHubHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(scale(40), scale(50), scale(40), scale(50));
        applyLinearGradient(header, 0f);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        headerNickname = new TextView(this);
        headerNickname.setText(prefs.getString("user_nickname", "CITIZEN_HUB").toUpperCase());
        headerNickname.setTextColor(Color.WHITE);
        headerNickname.setTextSize(24);
        headerNickname.setTypeface(Typeface.DEFAULT_BOLD);
        textLayout.addView(headerNickname);

        TextView subTv = new TextView(this);
        subTv.setText("Identity & Integrity Hub");
        subTv.setTextColor(Color.WHITE);
        subTv.setAlpha(0.8f);
        subTv.setTextSize(14);
        textLayout.addView(subTv);

        header.addView(textLayout);

        TextView helpBtn = new TextView(this);
        helpBtn.setText("?");
        helpBtn.setTextColor(Color.WHITE);
        helpBtn.setTextSize(18);
        helpBtn.setGravity(Gravity.CENTER);
        helpBtn.setTypeface(Typeface.DEFAULT_BOLD);
        int dot = scale(65);
        helpBtn.setLayoutParams(new LinearLayout.LayoutParams(dot, dot));
        applySolidCardGeometry(helpBtn, Color.parseColor("#44FFFFFF"), 14f);
        header.addView(helpBtn);
        helpBtn.setOnClickListener(v -> triggerHelpDialog());

        return header;
    }

    private View createAvatarHubMatrix() {
        LinearLayout hub = new LinearLayout(this);
        hub.setOrientation(LinearLayout.HORIZONTAL);
        hub.setPadding(scale(20), scale(30), scale(20), scale(30));
        hub.setWeightSum(2.0f);

        FrameLayout left = new FrameLayout(this);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0, scale(460), 1.0f);
        llp.setMargins(0, 0, scale(12), 0);
        left.setLayoutParams(llp);
        left.setElevation(scale(15));
        left.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        left.setClipToOutline(true);
        applySolidCardGeometry(left, Color.TRANSPARENT, 24f);

        View halo = new View(this);
        int hSize = scale(440);
        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(hSize, hSize, Gravity.CENTER);
        halo.setLayoutParams(hlp);
        applyStrokeGeometry(halo, Color.TRANSPARENT, getGradientStart(), (float)hSize/2f, 3);
        left.addView(halo);

        mainProfilePic = new ImageView(this);
        mainProfilePic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int iSize = scale(420);
        mainProfilePic.setLayoutParams(new FrameLayout.LayoutParams(iSize, iSize, Gravity.CENTER));
        AvatarLoader.bindCurrentUserAvatar(this, mainProfilePic);
        left.addView(mainProfilePic);
        
        mainProfilePic.setOnClickListener(v -> showAvatarSelectionDialog());
        halo.setOnClickListener(v -> showAvatarSelectionDialog());
        
        hub.addView(left);

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, scale(460), 1.0f);
        rlp.setMargins(scale(12), 0, 0, 0);
        right.setLayoutParams(rlp);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);
        row1.addView(createTinyIcon(android.R.drawable.ic_menu_camera, v -> dispatchTakePictureIntent()));
        row1.addView(createTinyIcon(android.R.drawable.ic_menu_gallery, v -> dispatchGalleryIntent()));
        right.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);
        row2.addView(createTinyIcon(android.R.drawable.ic_menu_crop, v -> startActivity(new Intent(this, AvatarCropActivity.class))));
        row2.addView(createTinyIcon(android.R.drawable.ic_menu_edit, v -> startActivity(new Intent(this, AvatarEditorActivity.class))));
        right.addView(row2);

        hub.addView(right);
        return hub;
    }

    private View createTinyIcon(int res, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(res);
        iv.setPadding(scale(22), scale(22), scale(22), scale(22));
        iv.setColorFilter(getGradientStart());
        int s = scale(120);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.setMargins(scale(15), scale(15), scale(15), scale(15));
        iv.setLayoutParams(lp);
        applyStrokeGeometry(iv, Color.WHITE, getGradientStart(), 60f, 2);
        iv.setOnClickListener(listener);
        return iv;
    }

    private View createIntegrityControlRow() {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, scale(12), 0, scale(25));
        row.setWeightSum(2.0f);

        // 🧪 Gradient Background shading for tactical buttons
        View secureBtn = createDashBtn("Identity:\nSECURE", android.R.drawable.ic_lock_lock, true);
        secureBtn.setOnClickListener(v -> startActivity(new Intent(this, PrivacyActivity.class)));
        row.addView(secureBtn, new LinearLayout.LayoutParams(0, scale(130), 1.0f));
        
        View pairBtn = createDashBtn("Device\nPairing", android.R.drawable.ic_menu_share, false);
        pairBtn.setOnClickListener(v -> startActivity(new Intent(this, GroupActivity.class)));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, scale(130), 1.0f);
        plp.setMargins(scale(18), 0, 0, 0);
        row.addView(pairBtn, plp);

        return row;
    }

    private View createDashBtn(String txt, int icon, boolean isPrimary) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(scale(22), 0, scale(22), 0);
        
        // 🧪 L7 Tactical Shading (Like header/buttons)
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, 
            isPrimary ? new int[]{getGradientStart(), getGradientEnd()} : new int[]{Color.parseColor("#444444"), Color.parseColor("#222222")});
        gd.setCornerRadius(scaleRadius(18f));
        btn.setBackground(gd);

        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(Color.WHITE);
        btn.addView(iv, new LinearLayout.LayoutParams(scale(55), scale(55)));

        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(14);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(scale(22), 0, 0, 0);
        btn.addView(tv);
        return btn;
    }

    private LinearLayout createDualRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(2.0f);
        return row;
    }

    private EditText createGhostBox(ViewGroup parent, String hint, int icon, boolean full) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextSize(14);
        et.setPadding(scale(38), scale(28), scale(38), scale(28));
        et.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        et.setCompoundDrawablePadding(scale(22));
        et.setHintTextColor(Color.GRAY);
        et.setTextColor(Color.BLACK);
        et.setSingleLine(true);
        et.setBackground(null);
        
        FrameLayout wrap = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(full ? -1 : 0, -2, 1.0f);
        lp.setMargins(scale(6), scale(9), scale(6), scale(9));
        wrap.setLayoutParams(lp);
        applyStrokeGeometry(wrap, Color.parseColor("#FAFAFA"), getGradientStart(), 16f, 2);
        wrap.addView(et);
        
        parent.addView(wrap);
        return et;
    }

    private AutoCompleteTextView createGhostDropdown(ViewGroup parent, String hint, int icon) {
        AutoCompleteTextView actv = new AutoCompleteTextView(this);
        actv.setHint(hint);
        actv.setTextSize(14);
        actv.setPadding(scale(38), scale(28), scale(38), scale(28));
        actv.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        actv.setCompoundDrawablePadding(scale(22));
        actv.setHintTextColor(Color.GRAY);
        actv.setTextColor(Color.BLACK);
        actv.setBackground(null);
        actv.setOnClickListener(v -> actv.showDropDown());

        FrameLayout wrap = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        lp.setMargins(scale(6), scale(9), scale(6), scale(9));
        wrap.setLayoutParams(lp);
        applyStrokeGeometry(wrap, Color.parseColor("#FAFAFA"), getGradientStart(), 16f, 2);
        wrap.addView(actv);
        
        parent.addView(wrap);
        return actv;
    }

    private View createSystemActionGrid() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        LinearLayout rA = createDualRow();
        rA.addView(createHugeBtn("PASSWORD", android.R.drawable.ic_menu_edit, v -> showPasswordDialog()), new LinearLayout.LayoutParams(0, scale(125), 1.0f));
        
        btnActionSave = createHugeBtn("VERIFY & SAVE", android.R.drawable.ic_menu_save, v -> doSave());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, scale(125), 1.0f);
        slp.setMargins(scale(18), 0, 0, 0);
        rA.addView(btnActionSave, slp);
        grid.addView(rA);

        LinearLayout rB = createDualRow();
        rB.setPadding(0, scale(18), 0, 0);
        rB.addView(createHugeBtn("DETAIL PROFILE", android.R.drawable.ic_menu_info_details, v -> {
            Intent i = new Intent(this, PersonDetailsActivity.class);
            i.putExtra("TARGET_UID", FirebaseAuth.getInstance().getUid());
            startActivity(i);
        }), new LinearLayout.LayoutParams(0, scale(125), 1.0f));
        View oBtn = createHugeBtn("ORGANIZATION", android.R.drawable.ic_menu_agenda, v -> startActivity(new Intent(this, OrgActivity.class)));
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(0, scale(125), 1.0f);
        olp.setMargins(scale(18), 0, 0, 0);
        rB.addView(oBtn, olp);
        grid.addView(rB);

        return grid;
    }

    private void showPasswordDialog() {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Enter New Verification PIN/Password");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("Identity Credential Update")
            .setMessage("Define a new forensic access pin for this terminal.")
            .setView(wrapInL7Capsule(et))
            .setPositiveButton("COMMIT", (d, w) -> {
                String pin = et.getText().toString().trim();
                if (pin.length() >= 4) {
                    pincodeField.setText(pin);
                    Toast.makeText(this, "Pinned for Verification Hub.", Toast.LENGTH_SHORT).show();
                } else Toast.makeText(this, "Invalid Pin Density.", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Abort", null).show();
    }

    private View wrapInL7Capsule(View v) {
        FrameLayout f = new FrameLayout(this);
        int p = scale(50);
        f.setPadding(p, p, p, p);
        f.addView(v);
        return f;
    }

    private View createHugeBtn(String label, int icon, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER_VERTICAL);
        btn.setPadding(scale(20), 0, scale(20), 0);
        btn.setOnClickListener(listener);
        
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{getGradientStart(), getGradientEnd()});
        gd.setCornerRadius(scaleRadius(22));
        btn.setBackground(gd);

        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(Color.WHITE);
        btn.addView(iv, new LinearLayout.LayoutParams(scale(65), scale(65)));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(scale(18), 0, 0, 0);
        btn.addView(tv);
        return btn;
    }

    private View createLogoutSessionButton() {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, scale(35), 0, scale(35));
        applyStrokeGeometry(btn, Color.parseColor("#FFF5F8"), getGradientStart(), 28f, 2);
        btn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.auth.AuthActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });

        ImageView iv = new ImageView(this);
        iv.setImageResource(android.R.drawable.ic_lock_power_off);
        iv.setColorFilter(getGradientStart());
        btn.addView(iv, new LinearLayout.LayoutParams(scale(65), scale(65)));

        TextView tv = new TextView(this);
        tv.setText("LOGOUT SESSION");
        tv.setTextColor(getGradientStart());
        tv.setTextSize(16);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(scale(25), 0, 0, 0);
        btn.addView(tv);
        return btn;
    }

    private void addSectionTitle(LinearLayout content, String txt) {
        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setTextColor(getGradientStart());
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(scale(10), scale(35), 0, scale(12));
        content.addView(tv);
    }

    private View createVerticalSpacer(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, height));
        return v;
    }

    private void applyStrokeGeometry(View v, int solid, int stroke, float radius, int width) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(solid);
        gd.setStroke(scale(width), stroke);
        gd.setCornerRadius(scaleRadius(radius));
        v.setBackground(gd);
    }

    @Override
    protected void setupLogic() {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        profileViewModel.loadForensicProfile(this);
        
        profileViewModel.getProfileData().observe(this, data -> {
            if (data == null) return;
            String nick = lookup(data, "userName", "full_name", "user_nickname");
            nameField.setText(nick);
            if (headerNickname != null) headerNickname.setText(nick.toUpperCase());
            
            phoneField.setText(lookup(data, "userPhone", "user_phone", "phone"));
            dobField.setText(lookup(data, "userDob", "user_dob", "dob"));
            emailField.setText(lookup(data, "userEmail", "email"));
            genderField.setText(lookup(data, "userGender", "user_gender", "gender"), false);
            String country = lookup(data, "userCountry", "user_country", "country");
            String state = lookup(data, "userState", "user_state", "state");
            String city = lookup(data, "userCity", "user_city", "city");
            
            countrySpinner.setText(country, false);
            stateSpinner.setText(state, false);
            citySpinner.setText(city, false);
            pincodeField.setText(lookup(data, "userPincode", "user_pincode", "pincode"));
            addressField.setText(lookup(data, "userAddress", "user_address", "address"));
            emergencyNoteField.setText(lookup(data, "userEmergencyNote", "user_emergency_note", "emergency_note"));
            orgField.setText(lookup(data, "orgName", "org_name"));
            deptField.setText(lookup(data, "deptName", "department"));

            // 🚀 GEO-REGISTRY AUTO-CASCADE: Populate buffers if parents are valid
            if (!country.isEmpty() && !country.equalsIgnoreCase("Country")) {
                profileViewModel.fetchStatesList(country);
                if (!state.isEmpty() && !state.equalsIgnoreCase("State/Province")) {
                    profileViewModel.fetchCitiesList(country, state);
                }
            }
        });

        dobField.setFocusable(false);
        dobField.setClickable(true);
        dobField.setCursorVisible(false);
        dobField.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                dobField.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        profileViewModel.getCountriesList().observe(this, list -> {
            countrySpinner.setAdapter(createEnterpriseDropdownAdapter(new ArrayList<>(list)));
            countrySpinner.setThreshold(0);
        });
        profileViewModel.getStatesList().observe(this, list -> {
            stateSpinner.setAdapter(createEnterpriseDropdownAdapter(new ArrayList<>(list)));
            stateSpinner.setThreshold(0);
        });
        profileViewModel.getCitiesList().observe(this, list -> {
            citySpinner.setAdapter(createEnterpriseDropdownAdapter(new ArrayList<>(list)));
            citySpinner.setThreshold(0);
        });

        countrySpinner.setOnItemClickListener((p, v, pos, id) -> {
            profileViewModel.fetchStatesList((String) p.getItemAtPosition(pos));
            stateSpinner.setText("");
            citySpinner.setText("");
        });
        stateSpinner.setOnItemClickListener((p, v, pos, id) -> {
            profileViewModel.fetchCitiesList(countrySpinner.getText().toString(), (String) p.getItemAtPosition(pos));
            citySpinner.setText("");
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(ProfileActivity.this, com.jdroidx.jfindme.ui.map.MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                finish();
            }
        });
        profileViewModel.fetchNetworkLocation();

        profileViewModel.getSaveSuccess().observe(this, success -> {
            AngelLoader.hide(this);
            if (success != null && success) {
                profileViewModel.resetSaveState(); // Prevent re-trigger on config change
                if (!isSyncInProgress) {
                    isSyncInProgress = true;
                    com.jdroidx.jfindme.utils.AvatarLoader.clearCache();
                    Toast.makeText(this, "🛡️ Identity Synchronized & Secured.", Toast.LENGTH_SHORT).show();
                    // Guard against duplicate ripple messages for 3.5s
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> isSyncInProgress = false, 3500);
                }
            }
        });
    }

    private String lookup(Map<String, String> data, String... keys) {
        if (data == null) return "";
        for (String k : keys) if (data.containsKey(k) && data.get(k) != null) return data.get(k);
        return "";
    }

    private void doSave() {
        String name = nameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        String dob = dobField.getText().toString().trim();
        String pin = pincodeField.getText().toString().trim();
        String country = countrySpinner.getText().toString().trim();
        String state = stateSpinner.getText().toString().trim();
        String city = citySpinner.getText().toString().trim();

        List<String> errorList = new ArrayList<>();
        
        // 🧪 ROBUST VALIDATION: Placeholder detection + mandatory state/city checks
        if (name.isEmpty() || name.equalsIgnoreCase("AGENT_NICKNAME") || name.length() < 2) 
            errorList.add("AGENT NICKNAME: Mandatory entry required");
            
        if (phone.isEmpty() || phone.equalsIgnoreCase("Mobile Number") || phone.length() < 7) 
            errorList.add("MOBILE NUMBER: Forensic handset link required");
            
        if (dob.isEmpty() || dob.equalsIgnoreCase("BirthDay")) 
            errorList.add("BIRTHDAY: Historical identity token required");
            
        if (pin.isEmpty() || pin.equalsIgnoreCase("ZipCode") || pin.equalsIgnoreCase("Zip Code") || pin.length() < 3) 
            errorList.add("ZIPCODE: Geospatial area code required");
            
        if (country.isEmpty() || country.equalsIgnoreCase("Country") || country.contains("Select")) 
            errorList.add("GEOGRAPHY: Country selection mandatory");
            
        if (state.isEmpty() || state.equalsIgnoreCase("State/Province") || state.contains("Select")) 
            errorList.add("REGIONAL: State/Province mandatory");
            
        if (city.isEmpty() || city.equalsIgnoreCase("City/Zone") || city.contains("Select")) 
            errorList.add("LOCALITY: City/Zone mandatory");

        if (!errorList.isEmpty()) {
            showValidationErrorDialog(errorList);
            return;
        }

        AngelLoader.show(this);
        profileViewModel.verifyAndSaveProfile(
            this,
            name, phone, dob,
            0, genderField.getText().toString(),
            false, pin,
            country, state, city,
            "None", "None",
            addressField.getText().toString(), emergencyNoteField.getText().toString(),
            orgField.getText().toString(), deptField.getText().toString(),
            prefs
        );
    }

    private void showValidationErrorDialog(List<String> errors) {
        StringBuilder msg = new StringBuilder("SENTINEL ALERT: Incomplete Forensic Identity\n\nThe following parameters are MISSING and required for system verification:\n\n");
        for (String e : errors) msg.append("❌ ").append(e).append("\n");
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("Identity Incomplete")
            .setMessage(msg.toString())
            .setPositiveButton("UPDATE ENTRIES", (d, w) -> d.dismiss())
            .show();
    }

    private CharSequence getMandatoryHint(String raw) {
        android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder("* " + raw);
        ssb.setSpan(new android.text.style.ForegroundColorSpan(Color.RED), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    // ============================================================================================
    // 🚀 AVATAR'S DIALOG (FULL UI MIRROR)
    // ============================================================================================

    private void showAvatarSelectionDialog() {
        LinearLayout dialogRoot = new LinearLayout(this);
        dialogRoot.setOrientation(LinearLayout.VERTICAL);
        dialogRoot.setPadding(scale(30), scale(30), scale(30), scale(30));
        
        TextView title = new TextView(this);
        title.setText("AVATAR'S");
        title.setTextColor(getGradientStart());
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, scale(30));
        dialogRoot.addView(title);

        GridView gridView = new GridView(this);
        gridView.setNumColumns(2);
        gridView.setVerticalSpacing(scale(30));
        gridView.setHorizontalSpacing(scale(30));
        gridView.setLayoutParams(new LinearLayout.LayoutParams(-1, scale(850)));
        
        List<IdentityRecord> identityList = loadIdentities();
        IdentityAdapter adapter = new IdentityAdapter(identityList);
        gridView.setAdapter(adapter);
        dialogRoot.addView(gridView);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, scale(30), 0, 0);

        TextView addBtn = createActionBtn("IMPORT", Color.parseColor("#43A047"), v -> dispatchGalleryIntent());
        TextView wkBtn = createActionBtn("3D WORKSPACE", Color.parseColor("#2979FF"), v -> startActivity(new Intent(this, AvatarEditorActivity.class)));
        TextView delBtn = createActionBtn("DELETE", Color.parseColor("#E53935"), v -> deleteSelectedIdentities(adapter));
        
        actions.addView(addBtn);
        
        LinearLayout.LayoutParams wkLp = new LinearLayout.LayoutParams(-2, -2);
        wkLp.setMargins(scale(15), 0, scale(15), 0);
        actions.addView(wkBtn, wkLp);
        
        actions.addView(delBtn);
        dialogRoot.addView(actions);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setView(dialogRoot)
            .create();
        dialog.show();
    }

    private TextView createActionBtn(String txt, int color, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(scale(25), scale(20), scale(25), scale(20));
        applySolidCardGeometry(tv, color, 12f);
        tv.setOnClickListener(listener);
        return tv;
    }

    private List<IdentityRecord> loadIdentities() {
        List<IdentityRecord> list = new ArrayList<>();
        File dir = new File(getFilesDir() + "/avatars");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) for (File f : files) list.add(new IdentityRecord(f.getAbsolutePath(), true));
        }
        for (int i = 1; i <= 22; i++) {
            list.add(new IdentityRecord("avatar_custom_" + i, false));
        }
        return list;
    }

    private void deleteSelectedIdentities(IdentityAdapter adapter) {
        List<IdentityRecord> items = adapter.items;
        boolean changed = false;
        for (int i = items.size() - 1; i >= 0; i--) {
            IdentityRecord r = items.get(i);
            if (r.isSelected && r.isCustom) {
                new File(r.path).delete();
                items.remove(i);
                changed = true;
            }
        }
        if (changed) adapter.notifyDataSetChanged();
    }

    class IdentityRecord {
        String path; boolean isCustom; boolean isSelected = false;
        IdentityRecord(String p, boolean c) { path = p; isCustom = c; }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainProfilePic != null) AvatarLoader.bindCurrentUserAvatar(this, mainProfilePic);
    }

    class IdentityAdapter extends BaseAdapter {
        List<IdentityRecord> items;
        int activeId;
        IdentityAdapter(List<IdentityRecord> l) { 
            items = l; 
            activeId = prefs.getInt("equipped_avatar_id", 1);
        }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int p) { return items.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup parent) {
            FrameLayout container = new FrameLayout(ProfileActivity.this);
            int s = scale(350);
            container.setLayoutParams(new GridView.LayoutParams(s, s));
            
            IdentityRecord r = items.get(p);
            boolean isActive = false;
            String currentPath = prefs.getString("equipped_vrm_string", "");
            
            if (activeId == -1) {
                // If custom mode, match by exact path to prevent multiple selection
                if (r.isCustom && r.path.equals(currentPath)) isActive = true;
            } else if (!r.isCustom) {
                try {
                    int pId = Integer.parseInt(r.path.replace("avatar_custom_", ""));
                    if (pId == activeId) isActive = true;
                } catch(Exception e) {}
            }

            ImageView iv = new ImageView(ProfileActivity.this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            applyStrokeGeometry(iv, Color.WHITE, isActive ? getGradientStart() : Color.LTGRAY, 16f, isActive ? 5 : 1);
            
            if (r.isCustom) {
                Bitmap b = BitmapFactory.decodeFile(r.path);
                if (b != null) iv.setImageBitmap(b);
            } else {
                int resId = getResources().getIdentifier(r.path, "drawable", getPackageName());
                iv.setImageResource(resId);
            }
            container.addView(iv);

            // 🚀 HIGH-VISIBILITY DELETE ICON (Top-Left Circular Button)
            if (r.isCustom) {
                ImageView delIcon = new ImageView(ProfileActivity.this);
                delIcon.setImageResource(android.R.drawable.ic_menu_delete);
                delIcon.setColorFilter(Color.WHITE);
                int ds = scale(110);
                FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(ds, ds, Gravity.TOP | Gravity.START);
                dlp.setMargins(scale(5), scale(5), 0, 0);
                delIcon.setLayoutParams(dlp);
                delIcon.setPadding(scale(25), scale(25), scale(25), scale(25));
                applySolidCardGeometry(delIcon, Color.parseColor("#E53935"), (float)ds/2f); // Bright Red Circle
                delIcon.setElevation(scale(12));
                delIcon.setOnClickListener(v1 -> {
                    new File(r.path).delete();
                    items.remove(p);
                    notifyDataSetChanged();
                });
                container.addView(delIcon);
            }

            // 🚀 SINGLE-SELECTION INDICATOR (Top-Right Active Tick)
            if (isActive) {
                ImageView tick = new ImageView(ProfileActivity.this);
                tick.setImageResource(android.R.drawable.checkbox_on_background);
                tick.setColorFilter(getGradientStart());
                int ts = scale(75);
                FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(ts, ts, Gravity.TOP | Gravity.END);
                tick.setLayoutParams(tlp);
                applySolidCardGeometry(tick, Color.WHITE, (float)ts/2f);
                container.addView(tick);
            }

            container.setOnClickListener(view -> {
                if (r.isCustom) {
                    prefs.edit().putInt("equipped_avatar_id", -1)
                              .putString("equipped_vrm_string", r.path).apply();
                } else {
                    try {
                        int pId = Integer.parseInt(r.path.replace("avatar_custom_", ""));
                        prefs.edit().putInt("equipped_avatar_id", pId)
                                  .putString("equipped_vrm_string", "").apply();
                    } catch(Exception e) {}
                }
                com.jdroidx.jfindme.utils.AvatarLoader.clearCache();
                AvatarLoader.bindCurrentUserAvatar(ProfileActivity.this, mainProfilePic);
                activeId = prefs.getInt("equipped_avatar_id", 1);
                notifyDataSetChanged();
            });

            return container;
        }
    }



    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 🚀 FRONT CAMERA PROTOCOL
        intent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
        intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
        if (intent.resolveActivity(getPackageManager()) != null) startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    private void dispatchGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                Bitmap bitmap = null;
                if (requestCode == REQUEST_IMAGE_CAPTURE && data != null && data.getExtras() != null) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                }
                
                if (bitmap != null) {
                    // 🚀 Save with unique name to allow multi-identity library
                    File dir = new File(getFilesDir() + "/avatars");
                    if (!dir.exists()) dir.mkdirs();
                    File cf = new File(dir, "Identity_" + System.currentTimeMillis() + ".png");
                    // ALSO save to the standard "custom_photo.png" for the CROP activity bypass
                    File cropSource = new File(dir, "custom_photo.png");
                    
                    FileOutputStream fos = new FileOutputStream(cf);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush(); fos.close();
                    
                    FileOutputStream fos2 = new FileOutputStream(cropSource);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos2);
                    fos2.flush(); fos2.close();
                    
                    startActivity(new Intent(this, AvatarCropActivity.class));
                }
            } catch (Exception e) {
                Toast.makeText(this, "Hardware Persistence Failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onSaveCurrentState() {
        c_name = nameField.getText().toString();
        c_phone = phoneField.getText().toString();
        c_dob = dobField.getText().toString();
        c_email = emailField.getText().toString();
        c_pin = pincodeField.getText().toString();
        c_addr = addressField.getText().toString();
        c_note = emergencyNoteField.getText().toString();
        c_org = orgField.getText().toString();
        c_dept = deptField.getText().toString();
    }

    @Override
    protected void onUiRebuilt() {
        if (nameField != null) nameField.setText(c_name);
        if (phoneField != null) phoneField.setText(c_phone);
        if (dobField != null) dobField.setText(c_dob);
        if (emailField != null) emailField.setText(c_email);
        if (pincodeField != null) pincodeField.setText(c_pin);
        if (addressField != null) addressField.setText(c_addr);
        if (emergencyNoteField != null) emergencyNoteField.setText(c_note);
        if (orgField != null) orgField.setText(c_org);
        if (deptField != null) deptField.setText(c_dept);
    }
}
