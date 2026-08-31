package com.jdroidx.jfindme.ui.management;

import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jdroidx.jfindme.engine.location.GeofenceHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Main -> UI Engine -> ManagementCluster -> GeofenceActivity (L4)
 * STATE-DRIVEN GEOSPATIAL UI (STRICT MVVM CLOUD ENGINE)
 * Hardening: L7 Architectural Registry Parity & Z-Axis Interaction Interception.
 */
public class GeofenceActivity extends BaseActivityJava implements OnMapReadyCallback {

    private final String TAG = "GeofenceActivity";
    private GeofenceHelper geofenceHelper;
    private GeofenceViewModel viewModel;
    private android.widget.ImageView myAvatarField;
    private static final int GEOFENCE_PERMISSION_CODE = 200;

    private GoogleMap googleMap;
    private Marker previewMarker;
    private Circle previewCircle;
    
    private String pendingZoneName;
    private String pendingAssignedName;
    private Double pendingLat;
    private Double pendingLng;
    private Double pendingRadius;
    
    private LinearLayout listStateContainer;
    private FrameLayout mapStateContainer;
    private LinearLayout activeBoundariesContainer;
    private View btnConfirmLocation;
    private final List<String> registeredGeofenceIds = new ArrayList<>();
    
    private final List<String> deviceList = new ArrayList<>();
    private final List<String> selectedNodes = new ArrayList<>(); // Track Multi-Select cluster
    private TextView txtNodes;
    private final Map<String, String> currentRosterMap = new HashMap<>(); // Role Registry
    
    private EditText edtZoneName;
    private EditText edtRadius;
    private String editingZoneId = null; // Track active edit session
    private EditText edtStart, edtEnd;
    private android.widget.CheckBox[] dayChecks;
    private View btnConfirm; // 🚀 Action Terminal Controller
    private String targetUid;
    private String targetName;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        // 🚀 QA Fix: Initialize ViewModel BEFORE super.onCreate
        // This prevents NullPointerException in createLayout which is triggered by super.onCreate (BaseActivityJava logic)
        viewModel = new ViewModelProvider(this).get(GeofenceViewModel.class);
        if (getIntent() != null) {
            targetUid = getIntent().getStringExtra("TARGET_UID");
            if (targetUid == null) targetUid = getIntent().getStringExtra("target_uid");
            targetName = getIntent().getStringExtra("TARGET_NAME");
            if (targetName == null) targetName = getIntent().getStringExtra("target_name");
            double latExtra = getIntent().getDoubleExtra("TARGET_LAT", 0.0);
            double lngExtra = getIntent().getDoubleExtra("TARGET_LNG", 0.0);
            if (latExtra != 0.0 && lngExtra != 0.0) {
                pendingLat = latExtra;
                pendingLng = lngExtra;
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (myAvatarField != null) com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, myAvatarField);
    }

    @Override
    protected View createLayout() {
        FrameLayout rootLayer = new FrameLayout(this);
        rootLayer.setBackgroundColor(getPrimaryColor());

        // STATE 1: ZONE LIST DASHBOARD
        listStateContainer = new LinearLayout(this);
        listStateContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        applyGlobalHeaderGeometry(header);

        LinearLayout titleBundle = new LinearLayout(this);
        titleBundle.setOrientation(LinearLayout.VERTICAL);
        titleBundle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(this);
        title.setText("SAFE ZONES");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        titleBundle.addView(title);

        TextView subTitle = new TextView(this);
        subTitle.setText("Establish and manage forensic perimeters");
        subTitle.setTextColor(getGradientOverlayText());
        subTitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subTitle, "TEXT_TINY");
        titleBundle.addView(subTitle);

        header.addView(titleBundle);
        listStateContainer.addView(header);
        
        // 🚀 IDENTITY CARD (Owner Tile 1): Mapped from Core Profile
        View identityBlock = buildIdentityBlock();
        identityBlock.setOnClickListener(v -> {
            Toast.makeText(this, "Master Sync Initialized...", Toast.LENGTH_SHORT).show();
            viewModel.fetchRosterMetadata();
            viewModel.resolveActiveProfileAddress();
        });
        listStateContainer.addView(identityBlock, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int q = getResponsivePaddingQuota();
        content.setPadding(q, q, q, scale(80));

        // 🚀 TOP CARD: PRESET FORENSIC DOMAINS (Deployment Hub)
        // 🚀 TOP CARD: PRESET FORENSIC DOMAINS (Deployment Templates)
        LinearLayout topCard = new LinearLayout(this);
        topCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, topCard, "CARD_STANDARD", 1.0f);
        topCard.setPadding(q, q, q, q);
        
        topCard.addView(createSectionHeader("ESTABLISH SAFE ZONE DOMAIN", "Select a mission template or configure custom geofence"));

        LinearLayout presetContainer = new LinearLayout(this);
        presetContainer.setOrientation(LinearLayout.VERTICAL);

        presetContainer.addView(createPresetTile("🏠 HOME DOMAIN", "Auto-pull profile address • 24/7 Monitoring", android.R.drawable.ic_menu_myplaces, v -> {
            pendingZoneName = "HOME";
            if (edtZoneName != null) edtZoneName.setText("HOME");
            if (edtRadius != null) edtRadius.setText("100");
            if (edtStart != null) edtStart.setText(formatToAmPm(0));
            if (edtEnd != null) edtEnd.setText(formatToAmPm(2359));
            setDaysSelection(true, true, true, true, true, true, true);
            viewModel.resolveActiveProfileAddress();
            viewModel.getProfileAddress().observe(this, addr -> {
                if (addr != null && !addr.equals("Unknown Location")) viewModel.executeOSGeocodingQuery(addr);
            });
            openMapEditorState();
        }));

        presetContainer.addView(createPresetTile("🏫 SCHOOL / CAMPUS", "08:00 AM - 03:00 PM • Weekdays Only", android.R.drawable.ic_menu_agenda, v -> {
            pendingZoneName = "SCHOOL";
            if (edtZoneName != null) edtZoneName.setText("SCHOOL");
            if (edtRadius != null) edtRadius.setText("150");
            if (edtStart != null) edtStart.setText(formatToAmPm(800));
            if (edtEnd != null) edtEnd.setText(formatToAmPm(1500));
            setDaysSelection(true, true, true, true, true, false, false);
            openMapEditorState();
        }));

        presetContainer.addView(createPresetTile("🏢 WORK / OFFICE", "09:00 AM - 05:00 PM • Weekdays Only", android.R.drawable.ic_menu_compass, v -> {
            pendingZoneName = "OFFICE";
            if (edtZoneName != null) edtZoneName.setText("OFFICE");
            if (edtRadius != null) edtRadius.setText("100");
            if (edtStart != null) edtStart.setText(formatToAmPm(900));
            if (edtEnd != null) edtEnd.setText(formatToAmPm(1700));
            setDaysSelection(true, true, true, true, true, false, false);
            openMapEditorState();
        }));

        presetContainer.addView(createPresetTile("➕ CUSTOM MISSION NODE", "Configure custom location, schedule & radius", android.R.drawable.ic_menu_add, v -> {
            pendingZoneName = "";
            if (edtZoneName != null) edtZoneName.setText("");
            if (edtRadius != null) edtRadius.setText("100");
            if (edtStart != null) edtStart.setText(formatToAmPm(0));
            if (edtEnd != null) edtEnd.setText(formatToAmPm(2359));
            setDaysSelection(true, true, true, true, true, true, true);
            openMapEditorState();
        }));

        topCard.addView(presetContainer);
        content.addView(topCard);

        // Spacer between cards
        content.addView(new View(this), new LinearLayout.LayoutParams(1, q));

        // 🚀 BOTTOM CARD: ACTIVE RADAR BOUNDARIES (Operational Hub)
        LinearLayout bottomCard = new LinearLayout(this);
        bottomCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, bottomCard, "CARD_STANDARD", 1.0f);
        
        // Universal internal padding for the card shell
        bottomCard.setPadding(q, q, q, q);
        
        LinearLayout activeHeaderRow = new LinearLayout(this);
        activeHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        activeHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        activeHeaderRow.addView(createSectionHeader("ACTIVE RADAR BOUNDARIES", "Active geofence monitoring nodes."), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnArchiveVault = createPresetCapsule("📦 ARCHIVE", v -> {
            viewModel.loadArchivedSafeZones();
            showArchivedSafeZonesDialog();
        });
        TextView btnNotifLedger = createPresetCapsule("📜 NOTIFICATIONS", v -> {
            startActivity(new android.content.Intent(this, GeofenceNotificationActivity.class));
        });
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        avLp.setMargins(scale(4), 0, scale(4), 0);
        activeHeaderRow.addView(btnArchiveVault, avLp);
        activeHeaderRow.addView(btnNotifLedger, avLp);
        bottomCard.addView(activeHeaderRow);

        activeBoundariesContainer = new LinearLayout(this);
        activeBoundariesContainer.setOrientation(LinearLayout.VERTICAL);
        bottomCard.addView(activeBoundariesContainer);
        
        content.addView(bottomCard);

        scroll.addView(content);
        listStateContainer.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        
        rootLayer.addView(listStateContainer);

        // STATE 2: L7 ARCHITECT FLOATING CARD EDITOR
        int hubPad = q;
        mapStateContainer = new FrameLayout(this);
        mapStateContainer.setVisibility(View.GONE); 
        mapStateContainer.setBackgroundColor(Color.parseColor("#99000000")); // Forensic Dim Backdrop
        mapStateContainer.setClickable(true); 
        mapStateContainer.setFocusable(true);
        
        LinearLayout mapOverlayCard = new LinearLayout(this);
        mapOverlayCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, mapOverlayCard, "CARD_STANDARD", 1.0f);
        mapOverlayCard.setElevation(scale(40));
        
        FrameLayout mapHardwareContainer = new FrameLayout(this);
        mapHardwareContainer.setId(1005);
        mapOverlayCard.addView(mapHardwareContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 6.0f));

        // Floating Search Bar Overlay (Internal to Card)
        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        android.graphics.drawable.GradientDrawable gSearch = new android.graphics.drawable.GradientDrawable();
        gSearch.setColor(getCardColor());
        gSearch.setCornerRadius(getResponsiveRadiusQuota());
        searchBar.setBackground(gSearch);
        UiArchitect.applyCardWithOverride(this, searchBar, "CARD_STANDARD", 1.25f);
        int sPad = q / 2;
        searchBar.setPadding(sPad * 2, sPad, sPad * 2, sPad);
        
        EditText edtSearch = new EditText(this);
        edtSearch.setHint("Search place or address...");
        edtSearch.setHintTextColor(getSecondaryTextColor());
        edtSearch.setTextColor(getTextColor());
        edtSearch.setBackgroundColor(Color.TRANSPARENT);
        edtSearch.setSingleLine(true);
        edtSearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        UiArchitect.applyText(this, edtSearch, "TEXT_BODY");
        
        ImageView btnSearchClear = new ImageView(this);
        btnSearchClear.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnSearchClear.setColorFilter(getSecondaryTextColor());
        btnSearchClear.setPadding(scale(15), scale(15), scale(15), scale(15));
        btnSearchClear.setVisibility(View.GONE);
        btnSearchClear.setOnClickListener(v -> edtSearch.setText(""));
        
        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSearchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String qStr = edtSearch.getText().toString().trim();
                if (!qStr.isEmpty()) {
                    Toast.makeText(this, "Locating: " + qStr + "...", Toast.LENGTH_SHORT).show();
                    viewModel.executeOSGeocodingQuery(qStr);
                }
                return true;
            }
            return false;
        });
        
        ImageView btnHelp = new ImageView(this);
        btnHelp.setImageResource(android.R.drawable.ic_menu_help);
        btnHelp.setColorFilter(getSecondaryTextColor());
        btnHelp.setPadding(scale(20), scale(20), scale(20), scale(20));
        btnHelp.setFocusable(true);
        btnHelp.setClickable(true);
        btnHelp.setOnClickListener(v -> showHelpDialog(this, "GEOFENCE_MAP_HUB"));
        
        ImageView btnSearch = new ImageView(this);
        btnSearch.setImageResource(android.R.drawable.ic_menu_search);
        btnSearch.setColorFilter(getGradientStart());
        btnSearch.setPadding(scale(20), scale(20), scale(20), scale(20));
        
        int searchIconSize = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        searchBar.addView(btnHelp, new LinearLayout.LayoutParams(searchIconSize, searchIconSize));
        searchBar.addView(edtSearch, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        searchBar.addView(btnSearchClear, new LinearLayout.LayoutParams(searchIconSize, searchIconSize));
        searchBar.addView(btnSearch, new LinearLayout.LayoutParams(searchIconSize, searchIconSize));
        
        FrameLayout.LayoutParams searchLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.setMargins(q * 2, q * 2, q * 2, 0); 
        mapHardwareContainer.addView(searchBar, searchLp); 
        btnSearch.setOnClickListener(v -> {
            String qStr = edtSearch.getText().toString().trim();
            if (!qStr.isEmpty()) {
                Toast.makeText(this, "Locating: " + qStr + "...", Toast.LENGTH_SHORT).show();
                viewModel.executeOSGeocodingQuery(qStr);
            } else {
                Toast.makeText(this, "Please enter an address or place name", Toast.LENGTH_SHORT).show();
            }
        });

        // 📍 FLOATING GPS TARGET BUTTON (Center on Current Location)
        ImageView btnMyGps = new ImageView(this);
        btnMyGps.setImageResource(android.R.drawable.ic_menu_mylocation);
        btnMyGps.setColorFilter(Color.WHITE);
        android.graphics.drawable.GradientDrawable gGps = new android.graphics.drawable.GradientDrawable();
        gGps.setColor(getGradientStart());
        gGps.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        btnMyGps.setBackground(gGps);
        btnMyGps.setElevation(scale(20));
        int gpsPad = scale(16);
        btnMyGps.setPadding(gpsPad, gpsPad, gpsPad, gpsPad);
        btnMyGps.setFocusable(true);
        btnMyGps.setClickable(true);
        btnMyGps.setOnClickListener(v -> {
            FusedLocationProviderClient fused = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fused.getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null) {
                        LatLng cur = new LatLng(loc.getLatitude(), loc.getLongitude());
                        viewModel.setDraftPin(cur);
                        if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cur, 16f));
                        Toast.makeText(this, "📍 Anchor pin set to your current GPS position!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Acquiring GPS location... Please ensure Location Services are ON.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GEOFENCE_PERMISSION_CODE);
            }
        });
        
        FrameLayout.LayoutParams gpsLp = new FrameLayout.LayoutParams(scale(90), scale(90));
        gpsLp.gravity = Gravity.BOTTOM | Gravity.END;
        gpsLp.setMargins(0, 0, q * 2, q * 2);
        mapHardwareContainer.addView(btnMyGps, gpsLp);

        // 🛰️ L7 ARCHITECT: TACTICAL DEPLOYMENT HUD (Scrollable Bottom Input Sheet)
        ScrollView inputScrollView = new ScrollView(this);
        inputScrollView.setVerticalScrollBarEnabled(false);
        inputScrollView.setClipChildren(false);
        inputScrollView.setClipToPadding(false);

        LinearLayout bottomConfirmArea = new LinearLayout(this);
        bottomConfirmArea.setOrientation(LinearLayout.VERTICAL);
        bottomConfirmArea.setPadding(hubPad, scale(8), hubPad, scale(12));
        bottomConfirmArea.setBackgroundColor(getCardColor());
        
        // 📈 L7 BEST-FIT: Consolidated Identity & Radius Row
        LinearLayout identityRadiusRow = new LinearLayout(this);
        identityRadiusRow.setOrientation(LinearLayout.HORIZONTAL);
        identityRadiusRow.setGravity(Gravity.CENTER_VERTICAL);
        identityRadiusRow.setPadding(0, scale(6), 0, scale(6));
        
        edtZoneName = new EditText(this);
        edtZoneName.setHint("Identity (e.g. Home)");
        edtZoneName.setHintTextColor(getSecondaryTextColor());
        edtZoneName.setTextColor(getTextColor());
        edtZoneName.setTextSize(13);
        edtZoneName.setPadding(scale(10), scale(6), scale(10), scale(6));
        applySolidCardGeometry(edtZoneName, getCardColor(), 14f);
        if (pendingZoneName != null) edtZoneName.setText(pendingZoneName);
        else {
            String intentInName = getIntent().getStringExtra("ESTABLISH_DOMAIN_NAME");
            if (intentInName != null) edtZoneName.setText(intentInName);
        }
        identityRadiusRow.addView(edtZoneName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // 🚀 Human-in-the-Loop Radius Stepper: [-] [ 100m ] [+]
        LinearLayout radiusStepperRow = new LinearLayout(this);
        radiusStepperRow.setOrientation(LinearLayout.HORIZONTAL);
        radiusStepperRow.setGravity(Gravity.CENTER_VERTICAL);
        applySolidCardGeometry(radiusStepperRow, getCardColor(), 14f);
        radiusStepperRow.setPadding(scale(4), scale(2), scale(4), scale(2));

        TextView btnMinus = new TextView(this);
        btnMinus.setText("－");
        btnMinus.setTextColor(getGradientStart());
        btnMinus.setTextSize(15);
        btnMinus.setTypeface(null, Typeface.BOLD);
        btnMinus.setPadding(scale(6), scale(2), scale(6), scale(2));
        btnMinus.setOnClickListener(v -> {
            try {
                int r = Integer.parseInt(edtRadius.getText().toString().trim());
                if (r > 50) {
                    edtRadius.setText(String.valueOf(r - 50));
                }
            } catch (Exception ignored) { edtRadius.setText("100"); }
        });
        radiusStepperRow.addView(btnMinus);

        edtRadius = new EditText(this);
        edtRadius.setText("100");
        edtRadius.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        edtRadius.setTextColor(getGradientStart());
        edtRadius.setTextSize(12);
        edtRadius.setTypeface(null, Typeface.BOLD);
        edtRadius.setGravity(Gravity.CENTER);
        edtRadius.setBackground(null);
        radiusStepperRow.addView(edtRadius, new LinearLayout.LayoutParams(scale(48), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView btnPlus = new TextView(this);
        btnPlus.setText("＋");
        btnPlus.setTextColor(getGradientStart());
        btnPlus.setTextSize(15);
        btnPlus.setTypeface(null, Typeface.BOLD);
        btnPlus.setPadding(scale(6), scale(2), scale(6), scale(2));
        btnPlus.setOnClickListener(v -> {
            try {
                int r = Integer.parseInt(edtRadius.getText().toString().trim());
                if (r < 5000) {
                    edtRadius.setText(String.valueOf(r + 50));
                }
            } catch (Exception ignored) { edtRadius.setText("150"); }
        });
        radiusStepperRow.addView(btnPlus);

        LinearLayout.LayoutParams radiusLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        radiusLp.setMargins(scale(8), 0, 0, 0);
        identityRadiusRow.addView(radiusStepperRow, radiusLp);
        bottomConfirmArea.addView(identityRadiusRow);

        // 🚀 Human-in-the-Loop: Quick Radius Preset Buttons
        LinearLayout radiusQuickBar = new LinearLayout(this);
        radiusQuickBar.setOrientation(LinearLayout.HORIZONTAL);
        radiusQuickBar.setGravity(Gravity.CENTER_VERTICAL);
        radiusQuickBar.setPadding(0, scale(2), 0, scale(6));
        
        int[] radiusOptions = {50, 100, 250, 500, 1000, 2000};
        for (int opt : radiusOptions) {
            TextView btnR = new TextView(this);
            btnR.setText(opt >= 1000 ? (opt/1000) + "km" : opt + "m");
            btnR.setTextColor(getTextColor());
            btnR.setTextSize(10);
            btnR.setTypeface(null, Typeface.BOLD);
            btnR.setGravity(Gravity.CENTER);
            int pPad = scale(6);
            btnR.setPadding(pPad, scale(4), pPad, scale(4));
            applySolidCardGeometry(btnR, getCardColor(), 10f);
            btnR.setFocusable(true);
            btnR.setClickable(true);
            btnR.setOnClickListener(v -> {
                edtRadius.setText(String.valueOf(opt));
                LatLng curDraft = (viewModel != null) ? viewModel.getDraftCoordinateLocation().getValue() : null;
                if (curDraft != null) renderMapDraftRings(curDraft);
            });
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            rLp.setMargins(scale(2), 0, scale(2), 0);
            radiusQuickBar.addView(btnR, rLp);
        }
        bottomConfirmArea.addView(radiusQuickBar);

        edtRadius.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                LatLng curDraft = (viewModel != null) ? viewModel.getDraftCoordinateLocation().getValue() : null;
                if (curDraft != null) renderMapDraftRings(curDraft);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // 🚀 L7 TEMPORAL SCHEDULER: Quick Schedule Presets + Time Pickers
        TextView lblSchedule = new TextView(this);
        lblSchedule.setText("MONITORING SCHEDULE");
        lblSchedule.setTextSize(10);
        lblSchedule.setTextColor(getTextColor());
        lblSchedule.setTypeface(null, Typeface.BOLD);
        lblSchedule.setPadding(0, scale(8), 0, scale(4));
        bottomConfirmArea.addView(lblSchedule);

        LinearLayout scheduleQuickBar = new LinearLayout(this);
        scheduleQuickBar.setOrientation(LinearLayout.HORIZONTAL);
        scheduleQuickBar.setGravity(Gravity.CENTER_VERTICAL);
        scheduleQuickBar.setPadding(0, 0, 0, scale(4));

        String[] schedPresets = {"24/7 ALL-DAY", "OFFICE (09-17)", "SCHOOL (08-15)"};
        int[][] schedTimes = {{0, 2359}, {900, 1700}, {800, 1500}};
        for (int i = 0; i < schedPresets.length; i++) {
            final int sT = schedTimes[i][0];
            final int eT = schedTimes[i][1];
            TextView btnSched = new TextView(this);
            btnSched.setText(schedPresets[i]);
            btnSched.setTextColor(getGradientStart());
            btnSched.setTextSize(8.5f);
            btnSched.setTypeface(null, Typeface.BOLD);
            btnSched.setGravity(Gravity.CENTER);
            btnSched.setPadding(scale(6), scale(3), scale(6), scale(3));
            applySolidCardGeometry(btnSched, getCardColor(), 10f);
            btnSched.setOnClickListener(v -> {
                if (edtStart != null) edtStart.setText(formatTime(sT));
                if (edtEnd != null) edtEnd.setText(formatTime(eT));
            });
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            sLp.setMargins(scale(2), 0, scale(2), 0);
            scheduleQuickBar.addView(btnSched, sLp);
        }
        bottomConfirmArea.addView(scheduleQuickBar);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);

        edtStart = new EditText(this);
        edtStart.setHint("Start (HH:mm)"); 
        edtStart.setText("00:00");
        edtStart.setTextSize(11);
        edtStart.setTextColor(getTextColor());
        edtStart.setHintTextColor(getSecondaryTextColor());
        edtStart.setTypeface(null, Typeface.BOLD);
        edtStart.setPadding(scale(8), scale(4), scale(8), scale(4));
        edtStart.setGravity(Gravity.CENTER);
        edtStart.setFocusable(false); 
        edtStart.setClickable(true);
        applySolidCardGeometry(edtStart, getCardColor(), 12f);
        edtStart.setOnClickListener(v -> showTimePicker(edtStart));
        timeRow.addView(edtStart, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView tSeparator = new TextView(this);
        tSeparator.setText(" TO ");
        tSeparator.setTextSize(10);
        tSeparator.setTextColor(getTextColor());
        tSeparator.setTypeface(null, Typeface.BOLD);
        tSeparator.setPadding(scale(8), 0, scale(8), 0);
        timeRow.addView(tSeparator);

        edtEnd = new EditText(this);
        edtEnd.setHint("End (HH:mm)"); 
        edtEnd.setText("23:59");
        edtEnd.setTextSize(11);
        edtEnd.setTextColor(getTextColor());
        edtEnd.setHintTextColor(getSecondaryTextColor());
        edtEnd.setTypeface(null, Typeface.BOLD);
        edtEnd.setPadding(scale(8), scale(4), scale(8), scale(4));
        edtEnd.setGravity(Gravity.CENTER);
        edtEnd.setFocusable(false); 
        edtEnd.setClickable(true);
        applySolidCardGeometry(edtEnd, getCardColor(), 12f);
        edtEnd.setOnClickListener(v -> showTimePicker(edtEnd));
        timeRow.addView(edtEnd, new LinearLayout.LayoutParams(0, -2, 1.0f));
        bottomConfirmArea.addView(timeRow);

        // AUDIT FIX [AUD-019]: Link tab sequence for hardware keyboard accessibility
        com.jdroidx.jfindme.utils.FormUtils.linkTabSequence(edtZoneName, edtRadius);

        // 📅 WEEKDAY CLUSTER: Presets + Multi-Node Day Selection
        LinearLayout dayPresetBar = new LinearLayout(this);
        dayPresetBar.setOrientation(LinearLayout.HORIZONTAL);
        dayPresetBar.setGravity(Gravity.CENTER_VERTICAL);
        dayPresetBar.setPadding(0, scale(6), 0, scale(2));

        TextView btnAllDays = createPresetCapsule("ALL DAYS", v -> setDaysSelection(true, true, true, true, true, true, true));
        TextView btnWeekdays = createPresetCapsule("WEEKDAYS", v -> setDaysSelection(true, true, true, true, true, false, false));
        TextView btnWeekends = createPresetCapsule("WEEKENDS", v -> setDaysSelection(false, false, false, false, false, true, true));

        dayPresetBar.addView(btnAllDays, new LinearLayout.LayoutParams(0, -2, 1.0f));
        dayPresetBar.addView(btnWeekdays, new LinearLayout.LayoutParams(0, -2, 1.0f));
        dayPresetBar.addView(btnWeekends, new LinearLayout.LayoutParams(0, -2, 1.0f));
        bottomConfirmArea.addView(dayPresetBar);

        LinearLayout dayStrip = new LinearLayout(this);
        dayStrip.setOrientation(LinearLayout.HORIZONTAL);
        dayStrip.setGravity(Gravity.CENTER);
        dayStrip.setPadding(0, scale(2), 0, scale(8));
        
        String[] daysLabels = {"M", "T", "W", "T", "F", "S", "S"};
        dayChecks = new android.widget.CheckBox[7];
        for (int i = 0; i < 7; i++) {
            dayChecks[i] = new android.widget.CheckBox(this);
            dayChecks[i].setText(daysLabels[i]);
            dayChecks[i].setTextSize(10);
            dayChecks[i].setChecked(true);
            dayChecks[i].setTextColor(getTextColor());
            dayChecks[i].setPadding(scale(1), 0, scale(1), 0);
            dayStrip.addView(dayChecks[i], new LinearLayout.LayoutParams(0, -2, 1.0f));
        }
        bottomConfirmArea.addView(dayStrip);

        // 👥 TARGET NODES COMPONENT: Group Profile Icon + Multi-Device Count Card + Refresh Button
        LinearLayout targetNodeCard = new LinearLayout(this);
        targetNodeCard.setOrientation(LinearLayout.HORIZONTAL);
        targetNodeCard.setGravity(Gravity.CENTER_VERTICAL);
        targetNodeCard.setPadding(scale(10), scale(6), scale(10), scale(6));
        targetNodeCard.setClickable(true);
        targetNodeCard.setFocusable(true);
        UiArchitect.applyCardWithOverride(this, targetNodeCard, "CARD_STANDARD", 0.95f);
        
        ImageView ivGroupIcon = new ImageView(this);
        ivGroupIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
        ivGroupIcon.setColorFilter(getGradientStart());
        int gIconSize = scale(22);
        targetNodeCard.addView(ivGroupIcon, new LinearLayout.LayoutParams(gIconSize, gIconSize));

        LinearLayout nodeTextCol = new LinearLayout(this);
        nodeTextCol.setOrientation(LinearLayout.VERTICAL);
        nodeTextCol.setPadding(scale(8), 0, scale(8), 0);
        nodeTextCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView lblNode = new TextView(this);
        lblNode.setText("TARGET NODES (DEPENDENT DEVICES)");
        lblNode.setTextColor(getSecondaryTextColor());
        lblNode.setTextSize(9);
        nodeTextCol.addView(lblNode);

        txtNodes = new TextView(this);
        txtNodes.setText("0 ASSIGNED (TAP TO CONFIGURE)"); 
        selectedNodes.clear(); // 🛑 NO AUTO-SELECT: Must be explicitly assigned
        txtNodes.setTextColor(getGradientStart());
        txtNodes.setTextSize(10.5f);
        txtNodes.setTypeface(null, Typeface.BOLD);
        nodeTextCol.addView(txtNodes);

        targetNodeCard.addView(nodeTextCol);

        // 🔄 REFRESH ROSTER BUTTON
        ImageView ivRefreshRoster = new ImageView(this);
        ivRefreshRoster.setImageResource(android.R.drawable.ic_popup_sync);
        ivRefreshRoster.setColorFilter(getGradientStart());
        ivRefreshRoster.setPadding(scale(4), scale(4), scale(4), scale(4));
        ivRefreshRoster.setOnClickListener(v -> {
            Toast.makeText(this, "🔄 Synchronizing live device roster...", Toast.LENGTH_SHORT).show();
            viewModel.fetchRosterMetadata();
        });
        targetNodeCard.addView(ivRefreshRoster, new LinearLayout.LayoutParams(scale(28), scale(28)));

        ImageView ivChevron = new ImageView(this);
        ivChevron.setImageResource(android.R.drawable.ic_menu_more);
        ivChevron.setColorFilter(getGradientStart());
        targetNodeCard.addView(ivChevron, new LinearLayout.LayoutParams(scale(20), scale(20)));

        targetNodeCard.setOnClickListener(v -> showMultiNodeSelector());
        bottomConfirmArea.addView(targetNodeCard);

        // 📈 GRID: Transactional Command Buttons (DEPLOY/ABORT)
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, scale(8), 0, scale(4));

        btnConfirm = createCompactIconButton("DEPLOY SAFE ZONE", android.R.drawable.ic_menu_send, true, v -> {
            try {
                // 🚀 L7 MISSION GUARD: Multi-Gate Validations
                if (edtZoneName == null || edtRadius == null || edtStart == null || edtEnd == null) {
                    Toast.makeText(this, "Mission Interface initializing... Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = edtZoneName.getText().toString().trim();
                String radiusStr = edtRadius.getText().toString().trim();
                int s = parseTo24H(edtStart.getText().toString());
                int e = parseTo24H(edtEnd.getText().toString());
                
                StringBuilder dBuilder = new StringBuilder();
                if (dayChecks != null) {
                    for(int i=0; i<7; i++) if(dayChecks[i] != null && dayChecks[i].isChecked()) dBuilder.append(i).append(",");
                }
                String dStr = dBuilder.length() > 0 ? dBuilder.substring(0, dBuilder.length()-1) : "0,1,2,3,4,5,6";

                LatLng draft = (viewModel != null) ? viewModel.getDraftCoordinateLocation().getValue() : null;
                double r = radiusStr.isEmpty() ? 100.0 : Double.parseDouble(radiusStr);

                if (r < 25.0 || r > 5000.0) {
                    showForensicDialog("INVALID RADIUS", "Safe Zone radius must be between 25 meters and 5000 meters.");
                    return;
                }
                
                // 🛑 MANDATORY TARGET NODE VALIDATION: Target node selection is mandatory
                if (selectedNodes.isEmpty()) {
                    Toast.makeText(this, "Target connected device selection is mandatory for dependent perimeter alerts.", Toast.LENGTH_LONG).show();
                    showMultiNodeSelector();
                    return;
                }
                String assigned = android.text.TextUtils.join(", ", selectedNodes);
                
                if (!name.isEmpty() && draft != null) {
                    checkPermissionsAndAdd(name, assigned, draft.latitude, draft.longitude, r, s, e, dStr, editingZoneId);
                } else {
                    if (name.isEmpty() && draft == null) {
                        showForensicDialog("MISSING INPUTS", "Please enter a Zone Name and tap on the map or tap the GPS button to set a center anchor.");
                    } else if (name.isEmpty()) {
                        showForensicDialog("MISSING ZONE NAME", "Please provide an identity name for this safe zone (e.g. Home, School, Office).");
                    } else {
                        showForensicDialog("NO ANCHOR SET", "Please tap on the map or tap the GPS button to set the center coordinates of this safe zone.");
                    }
                }
            } catch (Exception fatal) {
                com.jdroidx.jfindme.utils.SystemLogger.e("GeofenceActivity", "DEPLOY_FATAL_UI", fatal);
                showForensicDialog("SYNCHRONIZATION ERROR", "Failed to deploy safe zone: " + fatal.getMessage());
            }
        });
        View btnAbort = createCompactIconButton("ABORT", android.R.drawable.ic_menu_close_clear_cancel, false, v -> closeMapEditorState());

        LinearLayout.LayoutParams btnLp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        btnLp1.setMargins(0, 0, scale(4), 0);
        actionRow.addView(btnConfirm, btnLp1);

        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f);
        btnLp2.setMargins(scale(4), 0, 0, 0);
        actionRow.addView(btnAbort, btnLp2);
        bottomConfirmArea.addView(actionRow);

        inputScrollView.addView(bottomConfirmArea, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mapOverlayCard.addView(inputScrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 4.0f));

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * 0.94), (int)(getResources().getDisplayMetrics().heightPixels * 0.90));
        cardLp.gravity = Gravity.CENTER;
        mapStateContainer.addView(mapOverlayCard, cardLp);
        
        rootLayer.addView(mapStateContainer);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(1005, mapFragment).commit(); 
        mapFragment.getMapAsync(this);

        // NOTE: OnBackPressedCallback is registered in setupLogic() — do NOT duplicate here.
        return rootLayer;
    }

    private TextView createPresetCapsule(String text, View.OnClickListener cl) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextColor(getGradientStart());
        b.setTextSize(9);
        b.setTypeface(null, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(scale(8), scale(6), scale(8), scale(6));
        applySolidCardGeometry(b, getCardColor(), 12f);
        b.setOnClickListener(cl);
        return b;
    }

    private void setDaysSelection(boolean... days) {
        if (dayChecks == null) return;
        for (int i = 0; i < Math.min(dayChecks.length, days.length); i++) {
            if (dayChecks[i] != null) dayChecks[i].setChecked(days[i]);
        }
    }

    private void showMultiNodeSelector() {
        if (deviceList.isEmpty()) {
            showForensicDialog(
                "NO LINKED DEVICES DETECTED",
                "You currently have no connected family or team devices. This Safe Zone will be assigned to 'ALL CONNECTED DEVICES' (Global Sector Broadcast) and will automatically protect newly paired members."
            );
            selectedNodes.clear();
            selectedNodes.add("ALL CONNECTED DEVICES");
            if (txtNodes != null) txtNodes.setText("GLOBAL SECTOR BROADCAST (All Devices)");
            return;
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        int q = getResponsivePaddingQuota();
        parent.setPadding(q, q, q, q);

        // Header description
        TextView subHeader = new TextView(this);
        subHeader.setText("Assign target connected devices. Safe Zone notifications trigger exclusively between linked devices (At least 1 device mandatory).");
        subHeader.setTextColor(getSecondaryTextColor());
        subHeader.setPadding(0, 0, 0, scale(10));
        UiArchitect.applyText(this, subHeader, "TEXT_TINY");
        parent.addView(subHeader);

        // 🔍 REALTIME SEARCH BAR
        EditText etSearchDevices = new EditText(this);
        etSearchDevices.setHint("🔍 Search devices by name or role...");
        etSearchDevices.setPadding(scale(14), scale(10), scale(14), scale(10));
        etSearchDevices.setTextColor(getTextColor());
        applySolidCardGeometry(etSearchDevices, getCardColor(), 18f);
        parent.addView(etSearchDevices);

        // 👥 GROUP-SELECT QUICK CHIP BAR
        LinearLayout groupChipBar = new LinearLayout(this);
        groupChipBar.setOrientation(LinearLayout.HORIZONTAL);
        groupChipBar.setGravity(Gravity.CENTER_VERTICAL);
        groupChipBar.setPadding(0, scale(10), 0, scale(12));

        final java.util.List<android.widget.CheckBox> checks = new java.util.ArrayList<>();
        final java.util.Map<String, View> rowViews = new java.util.HashMap<>();

        TextView btnSelChildren = createPresetCapsule("👶 CHILDREN", v -> {
            for (android.widget.CheckBox cb : checks) {
                String dev = (String) cb.getTag();
                String r = currentRosterMap.containsKey(dev) ? currentRosterMap.get(dev).toUpperCase() : "";
                if (r.contains("CHILD") || r.contains("WARD") || r.contains("KID") || r.contains("SUBJECT")) {
                    cb.setChecked(true);
                }
            }
        });
        TextView btnSelGuardians = createPresetCapsule("🛡️ GUARDIANS", v -> {
            for (android.widget.CheckBox cb : checks) {
                String dev = (String) cb.getTag();
                String r = currentRosterMap.containsKey(dev) ? currentRosterMap.get(dev).toUpperCase() : "";
                if (r.contains("GUARDIAN") || r.contains("ADMIN") || r.contains("PARENT") || r.contains("HOST")) {
                    cb.setChecked(true);
                }
            }
        });
        TextView btnSelAll = createPresetCapsule("🌐 ALL", v -> {
            selectedNodes.clear();
            selectedNodes.add("ALL CONNECTED DEVICES");
            for (android.widget.CheckBox cb : checks) {
                cb.setChecked(cb.getText().equals("ALL CONNECTED DEVICES") || "ALL CONNECTED DEVICES".equals(cb.getTag()));
            }
        });
        TextView btnClearAll = createPresetCapsule("❌ CLEAR", v -> {
            selectedNodes.clear();
            for (android.widget.CheckBox cb : checks) cb.setChecked(false);
        });

        groupChipBar.addView(btnSelChildren, new LinearLayout.LayoutParams(0, -2, 1.0f));
        groupChipBar.addView(btnSelGuardians, new LinearLayout.LayoutParams(0, -2, 1.0f));
        groupChipBar.addView(btnSelAll, new LinearLayout.LayoutParams(0, -2, 0.9f));
        groupChipBar.addView(btnClearAll, new LinearLayout.LayoutParams(0, -2, 0.9f));
        parent.addView(groupChipBar);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("👥 ASSIGN TARGET CONNECTED DEVICES");
        
        int accent = getGradientStart();
        android.content.res.ColorStateList csl = new android.content.res.ColorStateList(
            new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
            new int[]{Color.GRAY, accent}
        );

        // Ensure "ALL CONNECTED DEVICES" is available in the list
        java.util.List<String> combinedList = new java.util.ArrayList<>();
        if (!combinedList.contains("ALL CONNECTED DEVICES")) combinedList.add("ALL CONNECTED DEVICES");
        for (String dev : deviceList) {
            if (!dev.equals("ALL CONNECTED DEVICES") && !combinedList.contains(dev)) {
                combinedList.add(dev);
            }
        }

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        for (String device : combinedList) {
            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setPadding(scale(8), scale(10), scale(8), scale(10));
            UiArchitect.applyCardWithOverride(this, itemRow, "CARD_STANDARD", 0.95f);

            android.widget.CheckBox cb = new android.widget.CheckBox(this);
            cb.setTextColor(getTextColor());
            cb.setButtonTintList(csl);
            cb.setChecked(selectedNodes.contains(device));

            // Circular Avatar or Icon
            FrameLayout avatarCircle = new FrameLayout(this);
            int avSize = scale(36);
            android.graphics.drawable.GradientDrawable avBg = new android.graphics.drawable.GradientDrawable();
            avBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            avBg.setColor(device.equals("ALL CONNECTED DEVICES") ? Color.parseColor("#334CAF50") : getGradientStart());
            avBg.setAlpha(50);
            avatarCircle.setBackground(avBg);

            TextView avInitial = new TextView(this);
            avInitial.setText(device.equals("ALL CONNECTED DEVICES") ? "🌐" : device.substring(0, 1).toUpperCase());
            avInitial.setTextColor(getGradientStart());
            avInitial.setTypeface(null, Typeface.BOLD);
            UiArchitect.applyText(this, avInitial, "TEXT_LABEL");
            FrameLayout.LayoutParams iLp = new FrameLayout.LayoutParams(-2, -2);
            iLp.gravity = Gravity.CENTER;
            avatarCircle.addView(avInitial, iLp);

            LinearLayout nameCol = new LinearLayout(this);
            nameCol.setOrientation(LinearLayout.VERTICAL);
            nameCol.setPadding(scale(10), 0, scale(10), 0);
            nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvDeviceName = new TextView(this);
            tvDeviceName.setText(device.equals("ALL CONNECTED DEVICES") ? "GLOBAL SECTOR BROADCAST" : device);
            tvDeviceName.setTextColor(getTextColor());
            tvDeviceName.setTypeface(null, Typeface.BOLD);
            UiArchitect.applyText(this, tvDeviceName, "TEXT_LABEL");
            nameCol.addView(tvDeviceName);

            String role = currentRosterMap.containsKey(device) ? currentRosterMap.get(device) : (device.equals("ALL CONNECTED DEVICES") ? "ALL PAIRED DEVICES" : "CONNECTED NODE");
            TextView tvRoleBadge = new TextView(this);
            tvRoleBadge.setText(role != null ? role.toUpperCase() : "MEMBER");
            tvRoleBadge.setTextColor(getGradientStart());
            UiArchitect.applyText(this, tvRoleBadge, "TEXT_TINY");
            nameCol.addView(tvRoleBadge);

            itemRow.addView(cb);
            itemRow.addView(avatarCircle, new LinearLayout.LayoutParams(avSize, avSize));
            itemRow.addView(nameCol);

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    selectedNodes.remove(device);
                } else {
                    if (device.equals("ALL CONNECTED DEVICES")) {
                        selectedNodes.clear();
                        selectedNodes.add("ALL CONNECTED DEVICES");
                        for (android.widget.CheckBox other : checks) {
                            if (other != cb) other.setChecked(false);
                        }
                    } else {
                        selectedNodes.remove("ALL CONNECTED DEVICES");
                        for (android.widget.CheckBox allCb : checks) {
                            if (allCb.getTag() != null && allCb.getTag().equals("ALL CONNECTED DEVICES")) {
                                allCb.setChecked(false);
                            }
                        }
                        if (!selectedNodes.contains(device)) selectedNodes.add(device);
                    }
                }
            });

            cb.setTag(device);
            checks.add(cb);
            rowViews.put(device.toLowerCase(), itemRow);
            itemRow.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
            
            listContainer.addView(itemRow);
            listContainer.addView(new View(this), new LinearLayout.LayoutParams(1, scale(6))); // spacing
        }

        // Live Search Filter Watcher
        etSearchDevices.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                for (String dev : combinedList) {
                    View row = rowViews.get(dev.toLowerCase());
                    if (row != null) {
                        String role = currentRosterMap.containsKey(dev) ? currentRosterMap.get(dev).toLowerCase() : "";
                        boolean matches = query.isEmpty() || dev.toLowerCase().contains(query) || role.contains(query);
                        row.setVisibility(matches ? View.VISIBLE : View.GONE);
                    }
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        parent.addView(listContainer);
        scroll.addView(parent);
        builder.setView(scroll);
        
        builder.setPositiveButton("CONFIRM", (dialog, which) -> {
            // 🛑 MANDATORY TARGET VALIDATION RULE: At least 1 device must be assigned
            if (selectedNodes.isEmpty()) {
                selectedNodes.add("ALL CONNECTED DEVICES");
                Toast.makeText(this, "Defaulted to 'GLOBAL SECTOR BROADCAST' (At least 1 target node is mandatory).", Toast.LENGTH_SHORT).show();
            }
            if (txtNodes != null) {
                if (selectedNodes.size() == 1 && selectedNodes.contains("ALL CONNECTED DEVICES")) {
                    txtNodes.setText("GLOBAL SECTOR BROADCAST (All Devices)");
                } else if (selectedNodes.size() == 1) {
                    txtNodes.setText("🎯 TARGET NODE: " + selectedNodes.get(0));
                } else {
                    txtNodes.setText("👥 " + selectedNodes.size() + " TARGET NODES ASSIGNED");
                }
            }
        });
        builder.setNegativeButton("CANCEL", null);
        
        android.app.AlertDialog alert = builder.create();
        alert.show();
        applyDialogTheme(alert);
    }

    private void editZone(RenderedGeofence zone) {
        editingZoneId = zone.getId();
        pendingZoneName = zone.getName();
        if (edtZoneName != null) edtZoneName.setText(zone.getName());
        if (edtRadius != null) edtRadius.setText(String.valueOf((int) zone.getRadius()));
        if (edtStart != null) edtStart.setText(formatTime(zone.getStartTime()));
        if (edtEnd != null) edtEnd.setText(formatTime(zone.getEndTime()));

        if (dayChecks != null) {
            String activeDays = zone.getActiveDays() != null ? zone.getActiveDays() : "0,1,2,3,4,5,6";
            for (int i = 0; i < 7; i++) {
                if (dayChecks[i] != null) {
                    dayChecks[i].setChecked(activeDays.contains(String.valueOf(i)));
                }
            }
        }

        selectedNodes.clear();
        if (zone.getAssignedName() != null && !zone.getAssignedName().isEmpty()) {
            String[] assigned = zone.getAssignedName().split(", ");
            for (String node : assigned) {
                selectedNodes.add(node.trim());
            }
        } else {
            selectedNodes.add("ALL CONNECTED DEVICES");
        }
        if (txtNodes != null) {
            if (selectedNodes.size() == 1 && selectedNodes.contains("ALL CONNECTED DEVICES")) {
                txtNodes.setText("GLOBAL SECTOR BROADCAST (All Devices)");
            } else if (selectedNodes.size() == 1) {
                txtNodes.setText("🎯 TARGET NODE: " + selectedNodes.get(0));
            } else {
                txtNodes.setText("👥 " + selectedNodes.size() + " TARGET NODES ASSIGNED");
            }
        }

        LatLng target = new LatLng(zone.getLat(), zone.getLng());
        viewModel.setDraftPin(target);
        if (googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f));
        }

        openMapEditorState();
    }

    private String formatTime(int t) {
        int h = t / 100;
        int m = t % 100;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m);
    }

    private void openMapEditorState() {
        // 🚀 L7 Architecture Sync: Update HUD Identity based on selection
        if (edtZoneName != null && pendingZoneName != null) edtZoneName.setText(pendingZoneName);
        if (edtRadius != null && editingZoneId == null) edtRadius.setText("100");

        // 🚀 Dynamic Action Terminal: Switch DEPLOY vs SAVE
        if (btnConfirm != null) {
            TextView btnText = (TextView) ((LinearLayout) btnConfirm).getChildAt(1);
            ImageView btnIcon = (ImageView) ((LinearLayout) btnConfirm).getChildAt(0);
            if (editingZoneId != null) {
                btnText.setText("UPDATE SAFE ZONE");
                btnIcon.setImageResource(android.R.drawable.ic_menu_save);
            } else {
                btnText.setText("DEPLOY SAFE ZONE");
                btnIcon.setImageResource(android.R.drawable.ic_menu_send);
            }
        }

        listStateContainer.setAlpha(0.3f);
        listStateContainer.setClickable(false);
        mapStateContainer.setVisibility(View.VISIBLE);
        Toast.makeText(this, "📍 Tap map, search place, or tap GPS button to set center anchor!", Toast.LENGTH_SHORT).show();
    }

    private void closeMapEditorState() {
        editingZoneId = null;
        pendingZoneName = null;
        selectedNodes.clear();
        selectedNodes.add("ALL CONNECTED DEVICES");
        if (txtNodes != null) txtNodes.setText("GLOBAL SECTOR BROADCAST (All Devices)");

        mapStateContainer.setVisibility(View.GONE);
        listStateContainer.setAlpha(1.0f);
        listStateContainer.setClickable(true);
    }

    private void showZoneNamingDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Configure Target Zone:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = scale(40);
        layout.setPadding(p, p / 2, p, p / 2);

        final EditText input = new EditText(this);
        input.setHint("Zone Name (e.g. Home, Office)");
        layout.addView(input);

        layout.addView(createIconButton("PULL FROM PROFILE ADDRESS", android.R.drawable.ic_menu_mapmode, v -> {
            viewModel.resolveActiveProfileAddress();
        }));
        
        viewModel.getProfileAddress().observe(this, addr -> {
            if (addr != null && !addr.equals("Unknown Location") && input.getText().toString().isEmpty()) {
                viewModel.executeOSGeocodingQuery(addr);
                input.setText("HOME");
            }
        });

        TextView lblTarget = new TextView(this);
        lblTarget.setText("\nAssign To Mobile Node:");
        lblTarget.setTypeface(null, Typeface.BOLD);
        lblTarget.setPadding(0, scale(30), 0, scale(10));
        layout.addView(lblTarget);

        android.widget.Spinner spinner = new android.widget.Spinner(this);
        List<String> devices = new ArrayList<>();
        devices.add("ALL CONNECTED DEVICES");
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, devices);
        spinner.setAdapter(adapter);
        layout.addView(spinner);

        TextView lblRadius = new TextView(this);
        lblRadius.setText("\nSafety Perimeter (Meters):");
        lblRadius.setTypeface(null, Typeface.BOLD);
        lblRadius.setPadding(0, scale(30), 0, scale(10));
        layout.addView(lblRadius);

        final EditText radiusInput = new EditText(this);
        radiusInput.setText("100"); 
        radiusInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(radiusInput);

        builder.setView(layout);
        viewModel.fetchTargetableDevices();
        viewModel.getActiveTargetDevices().observe(this, newDevices -> { devices.clear(); devices.addAll(newDevices); adapter.notifyDataSetChanged(); });

        builder.setPositiveButton("DEPLOY HUB", (dialog, which) -> {
            String name = input.getText().toString().trim();
            LatLng draft = viewModel.getDraftCoordinateLocation().getValue();
            if (name.isEmpty()) {
                UiArchitect.showActionableValidationDialog(this, "MISSING ZONE NAME",
                    "A descriptive zone name is required (e.g. Home, School, Work). Would you like to use default name 'Safe Zone' or enter custom?",
                    "USE 'SAFE ZONE'", "ENTER CUSTOM",
                    () -> { input.setText("Safe Zone"); showZoneNamingDialog(); }, null);
                return;
            }
            if (draft == null) {
                UiArchitect.showActionableValidationDialog(this, "NO ANCHOR SET",
                    "Please tap or search on the map to set the center coordinate anchor for this Safe Zone before deploying.",
                    "OPEN MAP", "DISMISS",
                    this::openMapEditorState, null);
                return;
            }
            double radius = 100.0;
            try {
                String rStr = radiusInput.getText().toString().trim();
                if (!rStr.isEmpty()) radius = Double.parseDouble(rStr);
            } catch (Exception e) {
                radius = 100.0;
            }
            if (radius < 50.0) radius = 50.0;
            if (radius > 50000.0) radius = 50000.0;

            String selectedTarget = spinner.getSelectedItem() != null ? spinner.getSelectedItem().toString() : "ALL CONNECTED DEVICES";
            checkPermissionsAndAdd(name, selectedTarget, draft.latitude, draft.longitude, radius, 0, 2359, "0,1,2,3,4,5,6", null);
        });
        builder.setNegativeButton("Abort", null);
        android.app.AlertDialog nameDialog = builder.create();
        nameDialog.show();
        applyDialogTheme(nameDialog);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.setOnMapClickListener(latLng -> viewModel.setDraftPin(latLng));
        this.googleMap.setOnMapLongClickListener(latLng -> viewModel.setDraftPin(latLng));
        
        // 🚀 L7 MISSION ACCELERATOR: Prioritize Live GPS Telemetry for Initial Centering
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15f));
                } else {
                    resolveInitialForensicPosition();
                }
            });
        } else {
            resolveInitialForensicPosition();
        }

        LatLng restored = viewModel.getDraftCoordinateLocation().getValue();
        if (restored != null) { renderMapDraftRings(restored); this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restored, 16f)); }
    }

    private void resolveInitialForensicPosition() {
        // 🚀 Intelligent Centering Bridge: Fallback to Profile/Default
        viewModel.resolveActiveProfileAddress();
        viewModel.getProfileAddress().observe(this, addr -> {
            if (addr != null && !addr.equals("Unknown Location") && !addr.isEmpty()) {
                viewModel.executeOSGeocodingQuery(addr);
            } else {
                double defLat = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultLatitude(this);
                double defLng = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultLongitude(this);
                LatLng defaultPos = new LatLng(defLat, defLng);
                if (googleMap != null) googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 12f));
            }
        });
    }

    private void renderMapDraftRings(LatLng pos) {
        if(googleMap == null) return;
        if(previewMarker != null) previewMarker.remove();
        if(previewCircle != null) previewCircle.remove();
        
        double currentRadius = 100.0;
        try {
            String rStr = (edtRadius != null) ? edtRadius.getText().toString().trim() : "100";
            if (!rStr.isEmpty()) currentRadius = Double.parseDouble(rStr);
        } catch (Exception ignored) {}
        if (currentRadius < 20.0) currentRadius = 20.0;
        if (currentRadius > 50000.0) currentRadius = 50000.0;

        previewMarker = googleMap.addMarker(new MarkerOptions()
            .position(pos)
            .title(edtZoneName != null && !edtZoneName.getText().toString().isEmpty() ? edtZoneName.getText().toString() : "DRAFT ZONE")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            
        int accent = getGradientStart();
        previewCircle = googleMap.addCircle(new CircleOptions()
            .center(pos)
            .radius(currentRadius)
            .strokeWidth(5)
            .strokeColor(accent)
            .fillColor(Color.argb(50, Color.red(accent), Color.green(accent), Color.blue(accent))));
            
        if (btnConfirmLocation != null && mapStateContainer.getVisibility() == View.VISIBLE) {
            btnConfirmLocation.setVisibility(View.VISIBLE);
        }
    }

    private void checkPermissionsAndAdd(String n, String a, double lat, double lng, double r, int s, int e, String days, String existingId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingZoneName = n; pendingAssignedName = a; pendingLat = lat; pendingLng = lng; pendingRadius = r;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GEOFENCE_PERMISSION_CODE);
            return;
        }

        // 🛡️ Android 10+ Background Location Check
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                pendingZoneName = n; pendingAssignedName = a; pendingLat = lat; pendingLng = lng; pendingRadius = r;
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, GEOFENCE_PERMISSION_CODE);
                return;
            }
        }

        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        viewModel.submitCloudCoordinateSync(n, a, lat, lng, r, s, e, days, existingId);
    }

    private void addLocalGeofenceTile(String id, String name, String address, String assignedName, double lat, double lng, double radius, boolean alertsEnabled, boolean isZoneActive, int start, int end, String days) {
        LinearLayout mainHost = new LinearLayout(this);
        mainHost.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, mainHost, "CARD_STANDARD", 1.0f);
        int q = getResponsivePaddingQuota();
        mainHost.setPadding(q, q, q, q);
        
        LinearLayout.LayoutParams hostLp = new LinearLayout.LayoutParams(-1, -2);
        hostLp.setMargins(0, 0, 0, q);
        mainHost.setLayoutParams(hostLp);

        // 🚀 SUB-TILE 1: IDENTITY & ANCHOR (Satellite NODE + Geospatial Address)
        LinearLayout card1 = new LinearLayout(this);
        card1.setOrientation(LinearLayout.VERTICAL);
        card1.setPadding(0, 0, 0, q/2);
        
        // Header Row (Indicator + NODE)
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        
        View indicator = new View(this);
        indicator.setBackgroundColor(isZoneActive ? Color.parseColor("#4CAF50") : Color.GRAY); 
        headerRow.addView(indicator, new LinearLayout.LayoutParams(scale(8), scale(40)));

        ImageView nodeIcon = new ImageView(this);
        nodeIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
        nodeIcon.setColorFilter(getGradientStart());
        nodeIcon.setPadding(q, 0, 0, 0);
        int iSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        headerRow.addView(nodeIcon, new LinearLayout.LayoutParams(iSize + q, iSize));

        TextView tvName = new TextView(this);
        tvName.setText("🛰️ NODE: " + name);
        tvName.setTextColor(getTextColor());
        tvName.setPadding(scale(10), 0, 0, 0);
        UiArchitect.applyText(this, tvName, "TEXT_LABEL");
        headerRow.addView(tvName, new LinearLayout.LayoutParams(0, -2, 1.0f));
        
        card1.addView(headerRow);

        // Address Row
        TextView tvAddr = new TextView(this);
        tvAddr.setText("📍 " + address);
        tvAddr.setTextColor(getSecondaryTextColor());
        tvAddr.setPadding(scale(18), scale(8), 0, 0); // Aligned with the NODE text
        UiArchitect.applyText(this, tvAddr, "TEXT_SUB");
        card1.addView(tvAddr);
        
        mainHost.addView(card1);

        // Subtle Group Separator
        View sep1 = new View(this);
        sep1.setBackgroundColor(getSecondaryTextColor());
        sep1.setAlpha(0.3f);
        mainHost.addView(sep1, new LinearLayout.LayoutParams(-1, 1));

        // 🚀 SUB-TILE 2: STATUS (Telemetry & Mission Window)
        LinearLayout card2 = new LinearLayout(this);
        card2.setOrientation(LinearLayout.VERTICAL);
        card2.setPadding(0, q/2, 0, q/2);

        // Row 1: Geospatial Anchor (Radius + Latitude/Longitude)
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView tvRad = new TextView(this);
        tvRad.setText("⭕ RAD: " + Math.round(radius) + "m");
        tvRad.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvRad, "TEXT_TINY");
        row1.addView(tvRad, new LinearLayout.LayoutParams(0, -2, 0.35f));

        TextView tvGeo = new TextView(this);
        tvGeo.setText("📡 GEO: " + String.format("%.4f, %.4f", lat, lng));
        tvGeo.setTextColor(getSecondaryTextColor());
        tvGeo.setGravity(Gravity.END);
        UiArchitect.applyText(this, tvGeo, "TEXT_TINY");
        row1.addView(tvGeo, new LinearLayout.LayoutParams(0, -2, 0.65f));
        
        card2.addView(row1);

        // Spacer between telemetry rows
        card2.addView(new View(this), new LinearLayout.LayoutParams(1, scale(5)));

        // Row 2: Temporal Window (Simplified Mission Matrix)
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        
        String s = formatToAmPm(start); String e = formatToAmPm(end);
        String schedule = "⏱️ " + s + " - " + e;
        String dayKeys[] = {"M", "T", "W", "T", "F", "S", "S"}; // 🚀 Monday-First Sync
        String dLabels = "";
        for(String d : days.split(",")) if(!d.isEmpty()) dLabels += dayKeys[Integer.parseInt(d)] + " ";
        String dayText = "📅 " + (dLabels.isEmpty() ? "NONE" : dLabels.trim());

        TextView tvSched = new TextView(this);
        tvSched.setText(schedule);
        tvSched.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvSched, "TEXT_TINY");
        row2.addView(tvSched, new LinearLayout.LayoutParams(0, -2, 0.5f));

        TextView tvDays = new TextView(this);
        tvDays.setText(dayText);
        tvDays.setTextColor(getSecondaryTextColor());
        tvDays.setGravity(Gravity.END);
        UiArchitect.applyText(this, tvDays, "TEXT_TINY");
        row2.addView(tvDays, new LinearLayout.LayoutParams(0, -2, 0.5f));
        
        card2.addView(row2);
        
        mainHost.addView(card2);

        // Subtle Group Separator
        View sep2 = new View(this);
        sep2.setBackgroundColor(getSecondaryTextColor());
        sep2.setAlpha(0.3f);
        mainHost.addView(sep2, new LinearLayout.LayoutParams(-1, 1));

        // 🚀 SUB-TILE 3: DEVICE CLUSTER (Target Nodes Slide-Out Tray with Count & Group Avatar)
        LinearLayout card3 = new LinearLayout(this);
        card3.setOrientation(LinearLayout.VERTICAL);
        card3.setPadding(0, q/2, 0, q/2);
        card3.setLayoutTransition(new android.animation.LayoutTransition());

        String[] nodeNames = assignedName.split(", ");
        java.util.List<String> cleanNodes = new java.util.ArrayList<>();
        for (String node : nodeNames) {
            String clean = node.trim();
            if (!clean.isEmpty()) cleanNodes.add(clean);
        }
        int nodeCount = cleanNodes.size();
        boolean isBroadcast = cleanNodes.contains("ALL CONNECTED DEVICES");

        LinearLayout clusterHeader = new LinearLayout(this);
        clusterHeader.setOrientation(LinearLayout.HORIZONTAL);
        clusterHeader.setGravity(Gravity.CENTER_VERTICAL);
        clusterHeader.setClickable(true);
        clusterHeader.setFocusable(true);
        clusterHeader.setPadding(0, scale(4), 0, scale(6));

        ImageView ivClusterIcon = new ImageView(this);
        ivClusterIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
        ivClusterIcon.setColorFilter(getGradientStart());
        int cIconSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        clusterHeader.addView(ivClusterIcon, new LinearLayout.LayoutParams(cIconSize, cIconSize));

        TextView lblCluster = new TextView(this);
        if (isBroadcast) {
            lblCluster.setText(" TARGET NODES (Global Sector Broadcast - All Paired)");
        } else if (nodeCount == 1) {
            lblCluster.setText(" TARGET NODES (1 Connected Device)");
        } else {
            lblCluster.setText(" TARGET NODES (" + nodeCount + " Connected Devices)");
        }
        lblCluster.setTextColor(getGradientStart());
        lblCluster.setPadding(scale(8), 0, scale(8), 0);
        lblCluster.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        UiArchitect.applyText(this, lblCluster, "TEXT_LABEL");
        clusterHeader.addView(lblCluster);

        TextView tvTrayToggle = new TextView(this);
        tvTrayToggle.setText("▾ SHOW TRAY");
        tvTrayToggle.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvTrayToggle, "TEXT_TINY");
        clusterHeader.addView(tvTrayToggle);

        card3.addView(clusterHeader);

        View avatarFlow = createGroupedTreeTray(cleanNodes, id);
        avatarFlow.setVisibility(View.GONE);
        card3.addView(avatarFlow);

        clusterHeader.setOnClickListener(v -> {
            if (avatarFlow.getVisibility() == View.VISIBLE) {
                avatarFlow.setVisibility(View.GONE);
                tvTrayToggle.setText("▾ SHOW TRAY");
            } else {
                avatarFlow.setVisibility(View.VISIBLE);
                tvTrayToggle.setText("▴ HIDE TRAY");
            }
        });

        // 🚀 SUB-TILE 4: ACTIONS (Forensic Trigger Terminal)
        LinearLayout card4 = new LinearLayout(this);
        card4.setOrientation(LinearLayout.HORIZONTAL);
        card4.setGravity(Gravity.CENTER_VERTICAL);
        card4.setPadding(0, q/2, 0, 0);
        
        iSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        int bp = scale(30);

        // Power
        ImageView ivPwr = new ImageView(this);
        ivPwr.setImageResource(android.R.drawable.ic_lock_power_off);
        ivPwr.setColorFilter(isZoneActive ? Color.parseColor("#4CAF50") : Color.GRAY);
        ivPwr.setOnClickListener(v -> viewModel.toggleZoneActiveState(id, isZoneActive));
        card4.addView(ivPwr, new LinearLayout.LayoutParams(iSize, iSize));

        // Alert
        ImageView ivBell = new ImageView(this);
        ivBell.setImageResource(android.R.drawable.ic_popup_reminder);
        ivBell.setColorFilter(alertsEnabled && isZoneActive ? Color.parseColor("#4CAF50") : Color.GRAY);
        ivBell.setAlpha(isZoneActive ? 1.0f : 0.4f);
        ivBell.setPadding(bp, 0, 0, 0);
        ivBell.setOnClickListener(v -> {
            if (isZoneActive) {
                viewModel.toggleBellAlertState(id, alertsEnabled);
            } else {
                Toast.makeText(this, "Activate Safe Zone to configure alerts.", Toast.LENGTH_SHORT).show();
            }
        });
        card4.addView(ivBell, new LinearLayout.LayoutParams(iSize + bp, iSize));

        // History
        ImageView ivHist = new ImageView(this);
        ivHist.setImageResource(android.R.drawable.ic_menu_recent_history);
        ivHist.setColorFilter(isZoneActive ? Color.parseColor("#FF9800") : Color.GRAY);
        ivHist.setAlpha(isZoneActive ? 1.0f : 0.4f);
        ivHist.setPadding(bp, 0, 0, 0);
        ivHist.setOnClickListener(v -> {
            if (isZoneActive) {
                viewModel.fetchGeofenceHistory(id);
            } else {
                Toast.makeText(this, "Activate Safe Zone to view history logs.", Toast.LENGTH_SHORT).show();
            }
        });
        card4.addView(ivHist, new LinearLayout.LayoutParams(iSize + bp, iSize));

        // Mission Edit (Assignment Redirect)
        ImageView ivEdit = new ImageView(this);
        ivEdit.setImageResource(android.R.drawable.ic_menu_edit);
        ivEdit.setPadding(bp, 0, 0, 0);
        ivEdit.setColorFilter(getGradientStart());
        ivEdit.setOnClickListener(v -> {
            RenderedGeofence zone = new RenderedGeofence(id, name, lat, lng, radius, assignedName, address, alertsEnabled, isZoneActive, start, end, days);
            editZone(zone);
        });
        card4.addView(ivEdit, new LinearLayout.LayoutParams(iSize + bp, iSize));

        // 🚀 QA Fix: Global Mission Connectivity (All Avatars Report)
        ImageView ivMailGroup = new ImageView(this);
        ivMailGroup.setImageResource(android.R.drawable.ic_dialog_email);
        ivMailGroup.setPadding(bp, 0, 0, 0);
        ivMailGroup.setColorFilter(Color.parseColor("#4CAF50")); // Tactical Green
        ivMailGroup.setOnClickListener(v -> {
            android.content.Intent route = new android.content.Intent(this, GeofenceNotificationActivity.class);
            route.putExtra("TARGET_ZONE_ID", id);
            startActivity(route);
        });
        card4.addView(ivMailGroup, new LinearLayout.LayoutParams(iSize + bp, iSize));

        // Spacer
        card4.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));

        // 📦 ARCHIVE SAFE ZONE (Non-destructive removal with all logs preserved)
        ImageView ivDel = new ImageView(this);
        ivDel.setImageResource(android.R.drawable.ic_menu_delete);
        ivDel.setColorFilter(Color.parseColor("#F44336"));
        ivDel.setOnClickListener(v -> {
            UiArchitect.showConfirmationDialog(this,
                "ARCHIVE SAFE ZONE?",
                "Move '" + name + "' into the Archive Folder? All historical notifications and trigger logs will be preserved in the archive vault.",
                "ARCHIVE ZONE", "CANCEL",
                () -> {
                    geofenceHelper.removeGeofence(id);
                    viewModel.archiveSafeZone(id);
                },
                null);
        });
        card4.addView(ivDel, new LinearLayout.LayoutParams(iSize, iSize));

        mainHost.addView(card3);

        // 🚀 Intense Separator (Waterfall Hardening)
        View sep3 = new View(this);
        sep3.setBackgroundColor(getSecondaryTextColor());
        sep3.setAlpha(0.3f); 
        mainHost.addView(sep3, new LinearLayout.LayoutParams(-1, 1));

        mainHost.addView(card4);

        activeBoundariesContainer.addView(mainHost);
    }
    
    private void addMemberNodeTile(String name) {
        String role = currentRosterMap.get(name);
        if (role == null) role = "PEER";

        LinearLayout nodeTile = new LinearLayout(this);
        nodeTile.setOrientation(LinearLayout.HORIZONTAL);
        nodeTile.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCardWithOverride(this, nodeTile, "CARD_STANDARD", 0.9f);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int q = getResponsivePaddingQuota();
        lp.setMargins(q * 2, 0, 0, q / 4); // 🚀 Indented for linkage
        nodeTile.setLayoutParams(lp);

        // 🚀 TACTICAL SEND: Integrated Mission Handshake Terminal (V48)
        ImageView ivSend = new ImageView(this);
        ivSend.setImageResource(android.R.drawable.ic_menu_send);
        ivSend.setColorFilter(getGradientStart());
        ivSend.setPadding(0, 0, getResponsivePaddingQuota(), 0);
        int sSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        
        ivSend.setOnClickListener(v -> {
            ivSend.setColorFilter(Color.GRAY);
            ivSend.setAlpha(0.5f);
            android.widget.Toast.makeText(this, "Syncing Mission Node: " + name + "...", android.widget.Toast.LENGTH_SHORT).show();
            // 🛰️ MASTER CLUSTER SYNC: Push forensic credentials to Device B
            viewModel.sendForensicHandshakeRequest(name);
        });
        nodeTile.addView(ivSend, new LinearLayout.LayoutParams(sSize + getResponsivePaddingQuota(), sSize));

        FrameLayout circle = new FrameLayout(this);
        int cSize = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        g.setColor(getGradientStart());
        g.setAlpha(40);
        circle.setBackground(g);

        TextView initial = new TextView(this);
        initial.setText(name.substring(0, 1).toUpperCase());
        initial.setTextColor(getGradientStart());
        initial.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, initial, "TEXT_LABEL");
        FrameLayout.LayoutParams iLp = new FrameLayout.LayoutParams(-2, -2);
        iLp.gravity = Gravity.CENTER;
        circle.addView(initial, iLp);
        nodeTile.addView(circle, new LinearLayout.LayoutParams(cSize, cSize));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(getResponsivePaddingQuota(), 0, 0, 0);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvName, "TEXT_LABEL");
        nameRow.addView(tvName);

        TextView tvSeparator = new TextView(this);
        tvSeparator.setText(" • ");
        tvSeparator.setTextColor(getGradientStart());
        nameRow.addView(tvSeparator);

        TextView tvRole = new TextView(this);
        tvRole.setText(role.toUpperCase());
        tvRole.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvRole, "TEXT_TINY");
        nameRow.addView(tvRole);
        
        content.addView(nameRow);

        TextView tvSub = new TextView(this);
        tvSub.setText("NETWORK NODE MONITOR");
        tvSub.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvSub, "TEXT_TINY");
        content.addView(tvSub);

        nodeTile.addView(content, new LinearLayout.LayoutParams(0, -2, 1.0f));
        activeBoundariesContainer.addView(nodeTile);
    }

    private View createPresetTile(String name, String desc, int iconRes, View.OnClickListener clickListener) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        int p = getResponsivePaddingQuota();
        tile.setPadding(p, p, p, p);
        UiArchitect.applyCard(this, tile, "CARD_STANDARD");

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart());
        int iSize = getResponsiveIconQuota();
        tile.addView(icon, new LinearLayout.LayoutParams(iSize, iSize));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(p, 0, 0, 0);

        TextView tTitle = new TextView(this);
        tTitle.setText(name);
        tTitle.setTextColor(getTextColor());
        UiArchitect.applyText(this, tTitle, "TEXT_LABEL");
        text.addView(tTitle);

        TextView tDesc = new TextView(this);
        tDesc.setText(desc);
        tDesc.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tDesc, "TEXT_SUB");
        text.addView(tDesc);

        tile.addView(text, new LinearLayout.LayoutParams(0, -2, 1.0f));

        tile.setFocusable(true);
        tile.setClickable(true);
        applyAccessibility(tile, "Establish " + name + " Node");
        tile.setOnClickListener(clickListener != null ? clickListener : v -> {
            pendingZoneName = name;
            openMapEditorState();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        int q = getResponsivePaddingQuota();
        lp.setMargins(0, 0, 0, q / 2);
        tile.setLayoutParams(lp);
        return tile;
    }

    private void showTimePicker(EditText target) {
        String current = target.getText().toString();
        int h = 0, m = 0;
        try {
            int t24 = parseTo24H(current);
            h = t24 / 100;
            m = t24 % 100;
        } catch (Exception e) {}

        android.app.TimePickerDialog picker = new android.app.TimePickerDialog(this, (view, hour, minute) -> {
            target.setText(formatToAmPm(hour * 100 + minute));
        }, h, m, false); // 🚀 12-Hour AM/PM Mode
        picker.show();
        applyDialogTheme(picker);
    }

    private View createGroupedTreeTray(java.util.List<String> cleanNodes, String zoneId) {
        LinearLayout treeContainer = new LinearLayout(this);
        treeContainer.setOrientation(LinearLayout.VERTICAL);
        treeContainer.setPadding(0, scale(6), 0, scale(4));

        java.util.List<String> guardians = new java.util.ArrayList<>();
        java.util.List<String> children = new java.util.ArrayList<>();
        java.util.List<String> friends = new java.util.ArrayList<>();
        boolean hasBroadcast = false;

        for (String node : cleanNodes) {
            if (node.equals("ALL CONNECTED DEVICES")) {
                hasBroadcast = true;
                continue;
            }
            String role = currentRosterMap.containsKey(node) ? currentRosterMap.get(node).toUpperCase() : "PEER";
            if (role.contains("GUARDIAN") || role.contains("ADMIN") || role.contains("PARENT") || role.contains("HOST")) {
                guardians.add(node);
            } else if (role.contains("CHILD") || role.contains("WARD") || role.contains("KID") || role.contains("SUBJECT")) {
                children.add(node);
            } else {
                friends.add(node);
            }
        }

        if (hasBroadcast) {
            LinearLayout broadcastTile = new LinearLayout(this);
            broadcastTile.setOrientation(LinearLayout.HORIZONTAL);
            broadcastTile.setGravity(Gravity.CENTER_VERTICAL);
            broadcastTile.setPadding(scale(10), scale(8), scale(10), scale(8));
            UiArchitect.applyCardWithOverride(this, broadcastTile, "CARD_STANDARD", 0.95f);

            TextView tvGlobe = new TextView(this);
            tvGlobe.setText("🌐");
            tvGlobe.setPadding(0, 0, scale(10), 0);
            broadcastTile.addView(tvGlobe);

            LinearLayout broadText = new LinearLayout(this);
            broadText.setOrientation(LinearLayout.VERTICAL);
            TextView tvAll = new TextView(this);
            tvAll.setText("GLOBAL SECTOR BROADCAST");
            tvAll.setTextColor(getGradientStart());
            tvAll.setTypeface(null, Typeface.BOLD);
            UiArchitect.applyText(this, tvAll, "TEXT_LABEL");
            broadText.addView(tvAll);

            TextView tvAllSub = new TextView(this);
            tvAllSub.setText("All current and future linked devices will trigger dependent perimeter alerts.");
            tvAllSub.setTextColor(getSecondaryTextColor());
            UiArchitect.applyText(this, tvAllSub, "TEXT_TINY");
            broadText.addView(tvAllSub);

            broadcastTile.addView(broadText);
            treeContainer.addView(broadcastTile);
            treeContainer.addView(new View(this), new LinearLayout.LayoutParams(1, scale(6)));
        }

        if (!guardians.isEmpty()) {
            treeContainer.addView(createTreeGroupHeader("🛡️ GUARDIANS & HOSTS", guardians.size()));
            for (int i = 0; i < guardians.size(); i++) {
                treeContainer.addView(createTreeMemberNodeRow(guardians.get(i), zoneId, i == guardians.size() - 1));
            }
        }

        if (!children.isEmpty()) {
            treeContainer.addView(createTreeGroupHeader("👶 CHILDREN & WARDS", children.size()));
            for (int i = 0; i < children.size(); i++) {
                treeContainer.addView(createTreeMemberNodeRow(children.get(i), zoneId, i == children.size() - 1));
            }
        }

        if (!friends.isEmpty()) {
            treeContainer.addView(createTreeGroupHeader("👥 FRIENDS & PEERS", friends.size()));
            for (int i = 0; i < friends.size(); i++) {
                treeContainer.addView(createTreeMemberNodeRow(friends.get(i), zoneId, i == friends.size() - 1));
            }
        }

        return treeContainer;
    }

    private View createTreeGroupHeader(String title, int count) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(scale(10), scale(8), scale(10), scale(4));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title + " (" + count + ")");
        tvTitle.setTextColor(getGradientStart());
        tvTitle.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvTitle, "TEXT_TINY");
        header.addView(tvTitle);

        View line = new View(this);
        line.setBackgroundColor(getSecondaryTextColor());
        line.setAlpha(0.2f);
        LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(0, 1, 1.0f);
        lLp.setMargins(scale(10), 0, 0, 0);
        header.addView(line, lLp);

        return header;
    }

    private View createTreeMemberNodeRow(String name, String zoneId, boolean isLast) {
        String role = currentRosterMap.get(name);
        if (role == null) role = "PEER";

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(scale(15), scale(4), scale(8), scale(4));

        // Tree Branch Indicator
        TextView tvBranch = new TextView(this);
        tvBranch.setText(isLast ? "└── " : "├── ");
        tvBranch.setTextColor(getSecondaryTextColor());
        tvBranch.setTypeface(Typeface.MONOSPACE);
        row.addView(tvBranch);

        LinearLayout nodeCard = new LinearLayout(this);
        nodeCard.setOrientation(LinearLayout.VERTICAL);
        nodeCard.setPadding(scale(12), scale(6), scale(10), scale(6));
        UiArchitect.applyCardWithOverride(this, nodeCard, "CARD_STANDARD", 0.92f);
        nodeCard.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        int s = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");

        // Profile Avatar Icon
        ImageView ivAvatar = new ImageView(this);
        ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        ivAvatar.setColorFilter(getGradientStart());
        topRow.addView(ivAvatar, new LinearLayout.LayoutParams(s, s));

        TextView tvName = new TextView(this);
        tvName.setText(name + " (" + role.toUpperCase() + ")");
        tvName.setTextColor(getTextColor());
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setPadding(scale(8), 0, 0, 0);
        UiArchitect.applyText(this, tvName, "TEXT_LABEL");
        topRow.addView(tvName, new LinearLayout.LayoutParams(0, -2, 1.0f));

        // Direct Logs Button [📜 LOGS]
        TextView btnLogs = new TextView(this);
        btnLogs.setText("📜 LOGS");
        btnLogs.setTextColor(getGradientStart());
        btnLogs.setTextSize(10);
        btnLogs.setTypeface(null, Typeface.BOLD);
        btnLogs.setPadding(scale(8), scale(4), scale(8), scale(4));
        btnLogs.setOnClickListener(v -> {
            java.util.List<String> logs = viewModel.getDeviceEventLogs(zoneId, name);
            showGeofenceHistoryDialog(logs.toArray(new String[0]));
        });
        topRow.addView(btnLogs);

        // Tactical Send Handshake [✈️]
        ImageView ivSend = new ImageView(this);
        ivSend.setImageResource(android.R.drawable.ic_menu_send);
        ivSend.setColorFilter(getGradientStart());
        ivSend.setPadding(scale(10), 0, scale(6), 0);
        ivSend.setOnClickListener(v -> {
            Toast.makeText(this, "Handshake dispatched to " + name + "...", Toast.LENGTH_SHORT).show();
            viewModel.sendForensicHandshakeRequest(name);
        });
        topRow.addView(ivSend, new LinearLayout.LayoutParams(s + scale(16), s));

        nodeCard.addView(topRow);

        // Latest Event Log Snippet
        java.util.List<String> devLogs = viewModel.getDeviceEventLogs(zoneId, name);
        String latestLog = !devLogs.isEmpty() ? devLogs.get(0) : "No perimeter events recorded yet";
        TextView tvEventSnippet = new TextView(this);
        tvEventSnippet.setText(latestLog);
        tvEventSnippet.setTextColor(getSecondaryTextColor());
        tvEventSnippet.setTextSize(10);
        tvEventSnippet.setPadding(s + scale(8), scale(2), 0, 0);
        UiArchitect.applyText(this, tvEventSnippet, "TEXT_TINY");
        nodeCard.addView(tvEventSnippet);

        row.addView(nodeCard);
        return row;
    }

    private void showArchivedSafeZonesDialog() {
        java.util.List<RenderedGeofence> archived = viewModel.getArchivedZones().getValue();
        if (archived == null || archived.isEmpty()) {
            Toast.makeText(this, "📦 Archive Folder is currently empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int p = getResponsivePaddingQuota();
        container.setPadding(p, p, p, p);

        // 🔍 ARCHIVE SEARCH & BULK ACTIONS
        EditText etSearchArchive = new EditText(this);
        etSearchArchive.setHint("🔍 Search archived safe zones...");
        etSearchArchive.setPadding(scale(12), scale(8), scale(12), scale(8));
        etSearchArchive.setTextColor(getTextColor());
        applySolidCardGeometry(etSearchArchive, getCardColor(), 16f);
        container.addView(etSearchArchive);

        LinearLayout bulkActionBar = new LinearLayout(this);
        bulkActionBar.setOrientation(LinearLayout.HORIZONTAL);
        bulkActionBar.setGravity(Gravity.CENTER_VERTICAL);
        bulkActionBar.setPadding(0, scale(8), 0, scale(12));

        TextView btnRestoreAll = createPresetCapsule("🟢 RESTORE ALL", v -> {
            UiArchitect.showConfirmationDialog(this,
                "RESTORE ALL ARCHIVED ZONES?",
                "Reactivate all archived safe zones back to active perimeter monitoring?",
                "RESTORE ALL", "CANCEL",
                () -> viewModel.restoreAllArchivedSafeZones(),
                null);
        });
        TextView btnPurgeAll = createPresetCapsule("🗑️ PURGE ALL", v -> {
            UiArchitect.showConfirmationDialog(this,
                "PERMANENTLY PURGE ARCHIVE?",
                "Permanently delete all archived safe zones and purge records from the cloud?",
                "PURGE ALL", "ABORT",
                () -> viewModel.purgeAllArchivedSafeZones(),
                null);
        });
        bulkActionBar.addView(btnRestoreAll, new LinearLayout.LayoutParams(0, -2, 1.0f));
        bulkActionBar.addView(btnPurgeAll, new LinearLayout.LayoutParams(0, -2, 1.0f));
        container.addView(bulkActionBar);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        final java.util.Map<String, View> archViews = new java.util.HashMap<>();

        for (RenderedGeofence zone : archived) {
            LinearLayout zoneCard = new LinearLayout(this);
            zoneCard.setOrientation(LinearLayout.VERTICAL);
            UiArchitect.applyCardWithOverride(this, zoneCard, "CARD_STANDARD", 1.0f);
            zoneCard.setPadding(p, p, p, p);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvTitle = new TextView(this);
            tvTitle.setText("📦 " + zone.getName().toUpperCase());
            tvTitle.setTextColor(getTextColor());
            tvTitle.setTypeface(null, Typeface.BOLD);
            UiArchitect.applyText(this, tvTitle, "TEXT_LABEL");
            topRow.addView(tvTitle, new LinearLayout.LayoutParams(0, -2, 1.0f));

            // RESTORE BUTTON
            TextView btnRestore = new TextView(this);
            btnRestore.setText("RESTORE");
            btnRestore.setTextColor(Color.parseColor("#4CAF50"));
            btnRestore.setTypeface(null, Typeface.BOLD);
            btnRestore.setPadding(scale(8), scale(4), scale(8), scale(4));
            applySolidCardGeometry(btnRestore, getCardColor(), 12f);
            btnRestore.setOnClickListener(v -> {
                viewModel.restoreSafeZone(zone.getId());
                Toast.makeText(this, "Safe Zone '" + zone.getName() + "' restored to active radar!", Toast.LENGTH_SHORT).show();
            });
            topRow.addView(btnRestore);

            // PURGE BUTTON
            TextView btnPurge = new TextView(this);
            btnPurge.setText("PURGE");
            btnPurge.setTextColor(Color.parseColor("#F44336"));
            btnPurge.setTypeface(null, Typeface.BOLD);
            btnPurge.setPadding(scale(8), scale(4), scale(8), scale(4));
            applySolidCardGeometry(btnPurge, getCardColor(), 12f);
            btnPurge.setOnClickListener(v -> {
                UiArchitect.showConfirmationDialog(this,
                    "PURGE ARCHIVED ZONE?",
                    "Permanently delete '" + zone.getName() + "' from cloud and memory?",
                    "PURGE", "CANCEL",
                    () -> viewModel.purgeArchivedSafeZone(zone.getId()),
                    null);
            });
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pLp.setMargins(scale(6), 0, 0, 0);
            topRow.addView(btnPurge, pLp);

            zoneCard.addView(topRow);

            TextView tvMeta = new TextView(this);
            tvMeta.setText("Radius: " + (int) zone.getRadius() + "m • Targets: " + (zone.getAssignedName().isEmpty() ? "None" : zone.getAssignedName()));
            tvMeta.setTextColor(getSecondaryTextColor());
            UiArchitect.applyText(this, tvMeta, "TEXT_SUB");
            zoneCard.addView(tvMeta);

            // Preserved Notifications Button
            TextView btnViewLogs = new TextView(this);
            btnViewLogs.setText("📜 VIEW PRESERVED NOTIFICATIONS");
            btnViewLogs.setTextColor(getGradientStart());
            btnViewLogs.setTextSize(11);
            btnViewLogs.setTypeface(null, Typeface.BOLD);
            btnViewLogs.setPadding(0, scale(8), 0, 0);
            btnViewLogs.setOnClickListener(v -> {
                viewModel.fetchGeofenceHistory(zone.getId());
            });
            zoneCard.addView(btnViewLogs);

            listContainer.addView(zoneCard);
            listContainer.addView(new View(this), new LinearLayout.LayoutParams(1, scale(10)));
            archViews.put(zone.getId(), zoneCard);
        }

        // Live Search Filter Watcher
        etSearchArchive.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                for (RenderedGeofence z : archived) {
                    View v = archViews.get(z.getId());
                    if (v != null) {
                        boolean matches = query.isEmpty() || z.getName().toLowerCase().contains(query) || z.getAssignedName().toLowerCase().contains(query);
                        v.setVisibility(matches ? View.VISIBLE : View.GONE);
                    }
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        container.addView(listContainer);
        scroll.addView(container);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle("📦 ARCHIVED SAFE ZONES & NOTIFICATIONS (" + archived.size() + ")")
            .setView(scroll)
            .setPositiveButton("CLOSE", null)
            .create();
        dialog.show();
        applyDialogTheme(dialog);
    }

    private View createSectionHeader(String t, String sub) {
        LinearLayout bundle = new LinearLayout(this);
        bundle.setOrientation(LinearLayout.VERTICAL);
        int p = getResponsivePaddingQuota();
        bundle.setPadding(p, p, p, p / 2);

        TextView h = new TextView(this);
        h.setText(t);
        h.setTextColor(getGradientStart());
        UiArchitect.applyText(this, h, "TEXT_LABEL");
        bundle.addView(h);

        if (sub != null && !sub.isEmpty()) {
            TextView s = new TextView(this);
            s.setText(sub);
            s.setTextColor(getTextColor()); // 🚀 High-Contrast Priority
            s.setAlpha(0.6f);
            UiArchitect.applyText(this, s, "TEXT_TINY");
            bundle.addView(s);
        }
        return bundle;
    }

    @Override
    protected void setupLogic() {
        geofenceHelper = new GeofenceHelper(this);
        viewModel = new ViewModelProvider(this).get(GeofenceViewModel.class);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mapStateContainer != null && mapStateContainer.getVisibility() == View.VISIBLE) {
                    String curName = (edtZoneName != null) ? edtZoneName.getText().toString().trim() : "";
                    LatLng curDraft = (viewModel != null) ? viewModel.getDraftCoordinateLocation().getValue() : null;
                    if (!curName.isEmpty() && curDraft != null) {
                        UiArchitect.showConfirmationDialog(GeofenceActivity.this,
                            "SAVE MISSION NODE?",
                            "You have uncommitted Safe Zone coordinates for '" + curName + "'. Would you like to deploy before leaving?",
                            "DEPLOY NOW", "DISCARD",
                            () -> {
                                if (btnConfirm != null) btnConfirm.performClick();
                            },
                            () -> closeMapEditorState());
                    } else {
                        closeMapEditorState();
                    }
                } else {
                    finish();
                }
            }
        });
        
        // 🚀 "Establish New Domain" Bridge: Handle inbound dossier metadata
        String intentAddress = getIntent().getStringExtra("ESTABLISH_DOMAIN_ADDRESS");
        String intentName = getIntent().getStringExtra("ESTABLISH_DOMAIN_NAME");
        if (intentAddress != null && !intentAddress.isEmpty()) {
            Toast.makeText(this, "De-encrypting Dossier Address: " + intentAddress, Toast.LENGTH_LONG).show();
            viewModel.executeOSGeocodingQuery(intentAddress);
            if (intentName != null) pendingZoneName = intentName;
            openMapEditorState();
        }

        // 🚀 Target UID / Name Pre-selection from Family & Friends / Roster
        String targetUid = getIntent().getStringExtra("TARGET_UID");
        String targetName = getIntent().getStringExtra("TARGET_NAME");
        String effectiveTarget = (targetName != null && !targetName.isEmpty()) ? targetName : targetUid;
        if (effectiveTarget != null && !effectiveTarget.isEmpty()) {
            selectedNodes.clear();
            selectedNodes.add(effectiveTarget);
            if (txtNodes != null) txtNodes.setText(effectiveTarget);
        }

        viewModel.getActiveZones().observe(this, renderedGeofences -> {
            if (activeBoundariesContainer != null) activeBoundariesContainer.removeAllViews();
            
            if (renderedGeofences == null || renderedGeofences.isEmpty()) {
                View emptyCard = UiArchitect.createEmptyStateCard(
                    this,
                    "No Safe Zones Established",
                    "Create virtual geofence boundaries to receive instant alerts when linked devices enter or exit.",
                    android.R.drawable.ic_menu_compass,
                    "CREATE SAFE ZONE",
                    () -> {
                        if (mapStateContainer != null) {
                            openMapEditorState();
                        }
                    }
                );
                if (activeBoundariesContainer != null) activeBoundariesContainer.addView(emptyCard);
                return;
            }

            // 🚀 Forensic Diffing Engine: Track current IDs to detect orphans
            List<String> currentIds = new ArrayList<>();
            String selfUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            String selfAgentId = com.jdroidx.jfindme.utils.RegistryManager.getAgentId(this);
            int ownerZonesCount = 0;
            
            for (RenderedGeofence zone : renderedGeofences) {
                String gId = zone.getId(); // 🚀 Use Forensic UID as the hardware anchor
                currentIds.add(gId);
                
                if (zone.isZoneActive()) {
                    geofenceHelper.addGeofence(gId, zone.getLat(), zone.getLng(), (float)zone.getRadius());
                } else {
                    geofenceHelper.removeGeofence(gId);
                }
                
                // 🛡️ OWNER ENFORCEMENT: Full boundary editor tiles show only to the Creator / Administrator
                boolean isOwner = (zone.getCreatorUid() != null && zone.getCreatorUid().equals(selfUid))
                    || (zone.getCreatorAgentId() != null && zone.getCreatorAgentId().equals(selfAgentId))
                    || (zone.getCreatorUid() == null && zone.getCreatorAgentId() == null);

                if (isOwner) {
                    ownerZonesCount++;
                    addLocalGeofenceTile(zone.getId(), zone.getName(), zone.getAddress(), zone.getAssignedName(), zone.getLat(), zone.getLng(), zone.getRadius(), zone.getAlertsEnabled(), zone.isZoneActive(), zone.getStartTime(), zone.getEndTime(), zone.getActiveDays());
                }
            }

            if (ownerZonesCount == 0) {
                LinearLayout nonOwnerCard = new LinearLayout(this);
                nonOwnerCard.setOrientation(LinearLayout.VERTICAL);
                nonOwnerCard.setGravity(Gravity.CENTER);
                int p = getResponsivePaddingQuota();
                nonOwnerCard.setPadding(p * 2, p * 2, p * 2, p * 2);
                UiArchitect.applyCard(this, nonOwnerCard, "CARD_STANDARD");

                TextView tvNoOwner = new TextView(this);
                tvNoOwner.setText("🛡️ NO SELF-CREATED BOUNDARIES");
                tvNoOwner.setTextColor(getGradientMid());
                tvNoOwner.setTypeface(null, Typeface.BOLD);
                UiArchitect.applyText(this, tvNoOwner, "TEXT_LABEL");
                nonOwnerCard.addView(tvNoOwner);

                TextView tvSub = new TextView(this);
                tvSub.setText("Active radar boundary editor tiles are only shown to the Safe Zone Creator / Administrator. Connected devices view assigned Safe Zones directly under their name in Family & Friends.");
                tvSub.setTextColor(getSecondaryTextColor());
                tvSub.setTextSize(11f);
                tvSub.setGravity(Gravity.CENTER);
                tvSub.setPadding(0, scale(6), 0, 0);
                nonOwnerCard.addView(tvSub);

                activeBoundariesContainer.addView(nonOwnerCard);
            }
            
            // 📦 ARCHIVE VAULT BUTTON
            java.util.List<RenderedGeofence> archList = viewModel.getArchivedZones().getValue();
            int archCount = archList != null ? archList.size() : 0;
            TextView btnOpenArchive = createPresetCapsule("📦 VIEW ARCHIVED SAFE ZONES (" + archCount + ")", v -> showArchivedSafeZonesDialog());
            btnOpenArchive.setPadding(scale(16), scale(12), scale(16), scale(12));
            LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            aLp.setMargins(0, scale(15), 0, scale(10));
            activeBoundariesContainer.addView(btnOpenArchive, aLp);
            
            // Kill Orphans (Decommission zones no longer in cloud matrix)
            for (String oldId : new ArrayList<>(registeredGeofenceIds)) {
                if (!currentIds.contains(oldId)) {
                    geofenceHelper.removeGeofence(oldId);
                    registeredGeofenceIds.remove(oldId);
                }
            }
            registeredGeofenceIds.clear();
            registeredGeofenceIds.addAll(currentIds);
        });

        viewModel.getArchivedZones().observe(this, archived -> {
            // Live update for archived zones vault
        });
        viewModel.loadArchivedSafeZones();

        viewModel.getDraftCoordinateLocation().observe(this, latLng -> { if (latLng != null) renderMapDraftRings(latLng); else if (btnConfirmLocation != null) btnConfirmLocation.setVisibility(View.GONE); });
        
        // 🛰️ OBSERVER: Sync Targetable Nodes (L7 Lifecycle Fix)
        viewModel.fetchTargetableDevices();
        viewModel.getActiveTargetDevices().observe(this, newDevices -> {
            deviceList.clear();
            deviceList.addAll(newDevices);
            if (targetUid != null) {
                for (String dev : newDevices) {
                    if (dev.equalsIgnoreCase(targetUid)) {
                        selectedNodes.clear();
                        selectedNodes.add(dev);
                        if (txtNodes != null) txtNodes.setText(dev);
                        break;
                    }
                }
            }
        });
        
        viewModel.getUiToastEvent().observe(this, e -> { String m = e.getContentIfNotHandled(); if (m != null) Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); });
        viewModel.getCameraMoveEvent().observe(this, e -> { LatLng t = e.getContentIfNotHandled(); if (t != null && googleMap != null) googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(t, 16f)); });
        viewModel.getCloseMapEvent().observe(this, e -> { 
            Boolean a = e.getContentIfNotHandled(); 
            if (a != null && a) {
                com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                closeMapEditorState(); 
            }
        });
        viewModel.getHistoryDialogEvent().observe(this, e -> {
            String[] logs = e.getContentIfNotHandled();
            if (logs != null) {
                showGeofenceHistoryDialog(logs);
            }
        });

        // 🚀 SYNC: Roster Member Metadata
        viewModel.getRosterMapping().observe(this, mapping -> {
            currentRosterMap.clear();
            currentRosterMap.putAll(mapping);
            if (targetName != null && !targetName.trim().isEmpty()) {
                selectedNodes.clear();
                selectedNodes.add(targetName.trim());
                if (txtNodes != null) txtNodes.setText(targetName.trim());
            } else if (targetUid != null && !targetUid.trim().isEmpty()) {
                String resolvedName = mapping.get(targetUid.trim());
                if (resolvedName != null && !resolvedName.trim().isEmpty()) {
                    selectedNodes.clear();
                    selectedNodes.add(resolvedName.trim());
                    if (txtNodes != null) txtNodes.setText(resolvedName.trim());
                } else if (mapping.containsKey(targetUid.trim())) {
                    selectedNodes.clear();
                    selectedNodes.add(targetUid.trim());
                    if (txtNodes != null) txtNodes.setText(targetUid.trim());
                }
            }
        });
    }

    // 🚀 L7 ORIENTATION SHIELD: Preserve Forensic Editor State
    private boolean isEditorActive = false;

    @Override
    protected void onSaveCurrentState(android.os.Bundle outState) {
        // Capture live state before layout teardown
        isEditorActive = (mapStateContainer != null && mapStateContainer.getVisibility() == View.VISIBLE);
    }

    @Override
    protected void onUiRebuilt(android.os.Bundle savedState) {
        if (savedState != null) {
            // Restore visibility after orientation matrix re-calculation
            if (isEditorActive) {
                openMapEditorState();
            }
            
            // 🛰️ Map Fragment Re-Injection: Forced handshake after layout re-pivot
            com.google.android.gms.maps.SupportMapFragment mapFragment = com.google.android.gms.maps.SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(1005, mapFragment).commit();
            mapFragment.getMapAsync(this);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GEOFENCE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingZoneName != null && pendingLat != null && pendingLng != null) {
                com.jdroidx.jfindme.utils.AngelLoader.show(this);
                viewModel.submitCloudCoordinateSync(pendingZoneName, pendingAssignedName, 
                    pendingLat, pendingLng, pendingRadius != null ? pendingRadius : 100.0);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                    closeMapEditorState(); // 🚀 QA Fix: Force navigation back after permission acquisition sync
                }, 3000);
            }
        }
    }

    private String formatToAmPm(int time24H) {
        int h = time24H / 100;
        int m = time24H % 100;
        String ampm = (h >= 12) ? "PM" : "AM";
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return String.format("%d:%02d %s", h12, m, ampm);
    }

    private int parseTo24H(String ampmTime) {
        if (ampmTime == null || ampmTime.isEmpty() || !ampmTime.contains(":")) return 0;
        try {
            String[] main = ampmTime.split(" ");
            String[] parts = main[0].split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (main.length > 1) {
                String ampm = main[1].toUpperCase();
                if (ampm.equals("PM") && h < 12) h += 12;
                if (ampm.equals("AM") && h == 12) h = 0;
            }
            return h * 100 + m;
        } catch (Exception e) { return 0; }
    }

    private void showGeofenceHistoryDialog(String[] logs) {
        int p = getResponsivePaddingQuota();
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(p, p, p, p);

        // Header Subtitle
        TextView tvSub = new TextView(this);
        tvSub.setText("Realtime Data Tree • 1x Notice per Device (Dormant until opposite transition)");
        tvSub.setTextColor(getSecondaryTextColor());
        tvSub.setPadding(0, 0, 0, scale(10));
        UiArchitect.applyText(this, tvSub, "TEXT_TINY");
        rootLayout.addView(tvSub);

        // Search Bar
        EditText edtFilter = new EditText(this);
        edtFilter.setHint("🔍 Search by device, date, or transition...");
        edtFilter.setHintTextColor(Color.parseColor("#888888"));
        edtFilter.setTextColor(getTextColor());
        android.graphics.drawable.GradientDrawable fieldBg = new android.graphics.drawable.GradientDrawable();
        fieldBg.setCornerRadius(scale(10));
        fieldBg.setColor(getCardColor());
        fieldBg.setStroke(1, Color.parseColor("#33888888"));
        edtFilter.setBackground(fieldBg);
        edtFilter.setPadding(scale(12), scale(10), scale(12), scale(10));
        UiArchitect.applyText(this, edtFilter, "TEXT_BODY");
        rootLayout.addView(edtFilter);

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        sLp.setMargins(0, scale(10), 0, scale(10));
        scroll.setLayoutParams(sLp);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        rootLayout.addView(scroll);

        // Renderer for timeline entries
        java.util.List<String> rawLogList = new ArrayList<>();
        if (logs != null) {
            for (String l : logs) {
                if (l != null && !l.trim().isEmpty()) rawLogList.add(l.trim());
            }
        }

        Runnable populateList = () -> {
            listContainer.removeAllViews();
            String filter = edtFilter.getText().toString().trim().toLowerCase();
            List<String> filtered = new ArrayList<>();
            for (String item : rawLogList) {
                if (filter.isEmpty() || item.toLowerCase().contains(filter)) {
                    filtered.add(item);
                }
            }

            if (filtered.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("🛡️ No perimeter trigger events match your search.");
                empty.setTextColor(getSecondaryTextColor());
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(p, p * 2, p, p * 2);
                UiArchitect.applyText(this, empty, "TEXT_BODY");
                listContainer.addView(empty);
                return;
            }

            String lastDate = "";
            for (String item : filtered) {
                // Extract Date Header if formatted as 📅 [YYYY-MM-DD]
                String currentDate = "";
                if (item.contains("📅 [") && item.contains("]")) {
                    int startIdx = item.indexOf("📅 [") + 4;
                    int endIdx = item.indexOf("]", startIdx);
                    if (endIdx > startIdx) currentDate = item.substring(startIdx, endIdx);
                }

                if (!currentDate.isEmpty() && !currentDate.equalsIgnoreCase(lastDate)) {
                    lastDate = currentDate;
                    TextView dateHeader = new TextView(this);
                    dateHeader.setText("📅 " + currentDate);
                    dateHeader.setTextColor(getGradientStart());
                    dateHeader.setTypeface(null, Typeface.BOLD);
                    dateHeader.setPadding(scale(4), scale(12), scale(4), scale(4));
                    UiArchitect.applyText(this, dateHeader, "TEXT_LABEL");
                    listContainer.addView(dateHeader);
                }

                // Event Card
                LinearLayout logItem = new LinearLayout(this);
                logItem.setOrientation(LinearLayout.VERTICAL);
                UiArchitect.applyCardWithOverride(this, logItem, "CARD_STANDARD", 0.9f);
                logItem.setPadding(p, p / 2, p, p / 2);
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, scale(6));
                logItem.setLayoutParams(lp);

                // Row: Status Pill + Details
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);

                boolean isEnter = item.contains("ENTER") || item.contains("(IN)");
                TextView badge = new TextView(this);
                badge.setText(isEnter ? "🟢 IN" : "🔴 OUT");
                badge.setTextColor(Color.WHITE);
                badge.setTypeface(null, Typeface.BOLD);
                badge.setPadding(scale(8), scale(3), scale(8), scale(3));
                
                android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
                badgeBg.setCornerRadius(scale(12));
                badgeBg.setColor(isEnter ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
                badge.setBackground(badgeBg);
                UiArchitect.applyText(this, badge, "TEXT_TINY");
                row.addView(badge);

                TextView tvLog = new TextView(this);
                String displayLog = item;
                if (item.contains("•")) {
                    String[] parts = item.split("•");
                    displayLog = parts[parts.length - 1].trim();
                }
                tvLog.setText("  " + displayLog);
                tvLog.setTextColor(getTextColor());
                tvLog.setTypeface(null, Typeface.BOLD);
                tvLog.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                UiArchitect.applyText(this, tvLog, "TEXT_BODY");
                row.addView(tvLog);
                logItem.addView(row);

                // Dormancy State Subtitle
                TextView tvDormant = new TextView(this);
                tvDormant.setText("⚡ 1x Notice Emitted • State Latched (Dormant until opposite transition)");
                tvDormant.setTextColor(getSecondaryTextColor());
                tvDormant.setPadding(0, scale(4), 0, 0);
                UiArchitect.applyText(this, tvDormant, "TEXT_TINY");
                logItem.addView(tvDormant);

                listContainer.addView(logItem);
            }
        };

        populateList.run();

        edtFilter.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                populateList.run();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle("📜 SAFE ZONE PERIMETER TIMELINE")
            .setView(rootLayout)
            .setPositiveButton("Close", null)
            .create();
        dialog.show();
        applyDialogTheme(dialog);
    }

    private View buildIdentityBlock() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(scale(25), scale(25), scale(25), scale(25)); 
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        myAvatarField = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, myAvatarField);
        myAvatarField.setBackground(null);
        
        int avatarSize = scale(168); 
        row.addView(myAvatarField, new LinearLayout.LayoutParams(avatarSize, avatarSize));
        
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(scale(40), 0, 0, 0);
        
        // --- 1. OWNER NAME ---
        com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        
        // 🚀 QA Fix: Anchor to SSOT (DatabaseHelper)
        String agentId = (myUid != null) ? db.resolveAgentIdLocally(myUid) : null;
        String fallbackOwner = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getOwnerAgentId(this);
        java.util.Map<String, String> myDos = db.getLocalDossier(agentId != null ? agentId : fallbackOwner);
        
        String nameStr = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this);
        
        TextView myName = new TextView(this);
        myName.setText("👤 Owner: " + nameStr);
        myName.setTextColor(getTextColor());
        myName.setTextSize(scaleText(15));
        myName.setTypeface(null, Typeface.BOLD);
        textBlock.addView(myName);
        
        // Fetch Persistent UIDs
        android.content.SharedPreferences jPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
        String hardwareId = jPrefs.getString("hardware_device_id", null);
        if (hardwareId == null) {
            hardwareId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            jPrefs.edit().putString("hardware_device_id", hardwareId).apply();
        }
        String deviceId = jPrefs.getString("device_uid", null);
        if (deviceId == null) {
            deviceId = "UID-" + java.util.UUID.randomUUID().toString().substring(0, 7).toUpperCase();
            jPrefs.edit().putString("device_uid", deviceId).apply();
        }

        String myUidCheck = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        String safeUid = (myUidCheck != null) ? myUidCheck : (deviceId != null ? deviceId : "LOCAL_NODE");
        String upperFirebaseId = safeUid.toUpperCase();

        // --- 2. DEVICE UID ---
        TextView tvDeviceUid = new TextView(this);
        tvDeviceUid.setText("📱 Device ID: - " + deviceId);
        tvDeviceUid.setTextColor(getSecondaryTextColor());
        tvDeviceUid.setTextSize(10);
        textBlock.addView(tvDeviceUid);

        // --- 3. HARDWARE UID ---
        TextView tvHardwareUid = new TextView(this);
        tvHardwareUid.setText("⚙️ Hardware UID: - " + hardwareId);
        tvHardwareUid.setTextColor(getSecondaryTextColor());
        tvHardwareUid.setTextSize(10);
        textBlock.addView(tvHardwareUid);

        // --- 4. NETWORK ID ---
        TextView tvNetworkId = new TextView(this);
        tvNetworkId.setText("🌐 Network ID: - " + upperFirebaseId);
        tvNetworkId.setTextColor(getSecondaryTextColor());
        tvNetworkId.setTextSize(9);
        textBlock.addView(tvNetworkId);
        
        // 🛡️ ARCHITECTURAL SILENCE: Check local persistence for Hub status
        String localActiveGroup = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);
        if (localActiveGroup != null) {
            TextView tvStatus = new TextView(this);
            tvStatus.setText("LOCKED TO HUB: " + localActiveGroup);
            tvStatus.setTextColor(Color.parseColor("#00E676"));
            tvStatus.setTextSize(10);
            tvStatus.setTypeface(null, Typeface.BOLD);
            tvStatus.setPadding(0, 10, 0, 0);
            textBlock.addView(tvStatus);
        }
        
        row.addView(textBlock);
        return row;
    }
}
