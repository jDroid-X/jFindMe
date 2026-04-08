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
        LinearLayout topCard = new LinearLayout(this);
        topCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, topCard, "CARD_STANDARD", 1.0f);
        
        // Universal internal padding for the card shell
        topCard.setPadding(q, q, q, q);
        
        topCard.addView(createSectionHeader("PRESET FORENSIC DOMAINS", "Identify-linked deployment templates"));

        LinearLayout presetContainer = new LinearLayout(this);
        presetContainer.setOrientation(LinearLayout.VERTICAL);
        presetContainer.addView(createPresetTile("ESTABLISH MISSION NODE", "Deploy Home, School or Office Zone", android.R.drawable.ic_menu_add));
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
        
        bottomCard.addView(createSectionHeader("ACTIVE RADAR BOUNDARIES", "See Zone of active radar boundaries."));
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
        mapOverlayCard.addView(mapHardwareContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

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
        edtSearch.setHint("Locate Domain...");
        edtSearch.setHintTextColor(getSecondaryTextColor());
        edtSearch.setTextColor(getTextColor());
        edtSearch.setBackgroundColor(Color.TRANSPARENT);
        edtSearch.setSingleLine(true);
        edtSearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        UiArchitect.applyText(this, edtSearch, "TEXT_BODY");
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                viewModel.executeOSGeocodingQuery(edtSearch.getText().toString());
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
        searchBar.addView(btnSearch, new LinearLayout.LayoutParams(searchIconSize, searchIconSize));
        
        FrameLayout.LayoutParams searchLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.setMargins(q * 2, q * 2, q * 2, 0); 
        mapHardwareContainer.addView(searchBar, searchLp); 
        btnSearch.setOnClickListener(v -> viewModel.executeOSGeocodingQuery(edtSearch.getText().toString()));

        // 🛰️ L7 ARCHITECT: TACTICAL DEPLOYMENT HUD (Integrated into Card Bottom)
        LinearLayout bottomConfirmArea = new LinearLayout(this);
        bottomConfirmArea.setOrientation(LinearLayout.VERTICAL);
        bottomConfirmArea.setPadding(hubPad, hubPad/2, hubPad, hubPad);
        bottomConfirmArea.setBackgroundColor(getCardColor());
        
        // 📈 L7 BEST-FIT: Consolidated Identity & Radius Row
        LinearLayout identityRadiusRow = new LinearLayout(this);
        identityRadiusRow.setOrientation(LinearLayout.HORIZONTAL);
        identityRadiusRow.setGravity(Gravity.CENTER_VERTICAL);
        identityRadiusRow.setPadding(0, scale(10), 0, scale(10));
        
        edtZoneName = new EditText(this);
        edtZoneName.setHint("Identity (e.g. Home)");
        edtZoneName.setHintTextColor(getSecondaryTextColor());
        edtZoneName.setTextColor(getTextColor());
        applySolidCardGeometry(edtZoneName, getCardColor(), 20f);
        if (pendingZoneName != null) edtZoneName.setText(pendingZoneName);
        else {
            String intentInName = getIntent().getStringExtra("ESTABLISH_DOMAIN_NAME");
            if (intentInName != null) edtZoneName.setText(intentInName);
        }
        identityRadiusRow.addView(edtZoneName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.3f));

        edtRadius = new EditText(this);
        edtRadius.setText("100");
        edtRadius.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        edtRadius.setTextColor(getGradientStart());
        edtRadius.setTypeface(null, Typeface.BOLD);
        edtRadius.setGravity(Gravity.CENTER);
        applySolidCardGeometry(edtRadius, getCardColor(), 20f);
        
        LinearLayout.LayoutParams radiusLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        radiusLp.setMargins(scale(15), 0, 0, 0);
        identityRadiusRow.addView(edtRadius, radiusLp);
        bottomConfirmArea.addView(identityRadiusRow);

        // 🚀 L7 TEMPORAL SCHEDULER: Define the Monitoring Mission Window
        TextView lblSchedule = new TextView(this);
        lblSchedule.setText("MONITORING WINDOW (24H FORMAT)");
        UiArchitect.applyText(this, lblSchedule, "TEXT_LABEL");
        lblSchedule.setPadding(0, scale(20), 0, scale(10));
        bottomConfirmArea.addView(lblSchedule);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);

        edtStart = new EditText(this);
        edtStart.setHint("Start (HH:mm)"); edtStart.setText("00:00");
        edtStart.setGravity(Gravity.CENTER);
        edtStart.setFocusable(false); edtStart.setClickable(true);
        applySolidCardGeometry(edtStart, getCardColor(), 20f);
        edtStart.setOnClickListener(v -> showTimePicker(edtStart));
        timeRow.addView(edtStart, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView tSeparator = new TextView(this);
        tSeparator.setText(" TO ");
        tSeparator.setPadding(scale(15), 0, scale(15), 0);
        UiArchitect.applyText(this, tSeparator, "TEXT_LABEL");
        timeRow.addView(tSeparator);

        edtEnd = new EditText(this);
        edtEnd.setHint("End (HH:mm)"); edtEnd.setText("23:59");
        edtEnd.setGravity(Gravity.CENTER);
        edtEnd.setFocusable(false); edtEnd.setClickable(true);
        applySolidCardGeometry(edtEnd, getCardColor(), 20f);
        edtEnd.setOnClickListener(v -> showTimePicker(edtEnd));
        timeRow.addView(edtEnd, new LinearLayout.LayoutParams(0, -2, 1.0f));
        bottomConfirmArea.addView(timeRow);

        // 📅 WEEKDAY CLUSTER: Multi-Node Day Selection
        LinearLayout dayStrip = new LinearLayout(this);
        dayStrip.setOrientation(LinearLayout.HORIZONTAL);
        dayStrip.setGravity(Gravity.CENTER);
        dayStrip.setPadding(0, scale(20), 0, scale(20));
        
        String[] daysLabels = {"M", "T", "W", "T", "F", "S", "S"};
        dayChecks = new android.widget.CheckBox[7];
        for (int i = 0; i < 7; i++) {
            dayChecks[i] = new android.widget.CheckBox(this);
            dayChecks[i].setText(daysLabels[i]);
            dayChecks[i].setChecked(true);
            dayChecks[i].setTextColor(getTextColor());
            dayStrip.addView(dayChecks[i], new LinearLayout.LayoutParams(0, -2, 1.0f));
        }
        bottomConfirmArea.addView(dayStrip);

        TextView lblNode = new TextView(this);
        lblNode.setText("ASSIGN TO NODE");
        UiArchitect.applyText(this, lblNode, "TEXT_LABEL");
        bottomConfirmArea.addView(lblNode);

        txtNodes = new TextView(this);
        txtNodes.setText("ALL CONNECTED DEVICES"); 
        selectedNodes.add("ALL CONNECTED DEVICES");
        txtNodes.setTextColor(getGradientStart());
        txtNodes.setPadding(0, scale(15), 0, scale(15));
        txtNodes.setFocusable(true);
        txtNodes.setClickable(true);
        UiArchitect.applyText(this, txtNodes, "TEXT_TITLE");
        txtNodes.setOnClickListener(v -> showMultiNodeSelector());
        bottomConfirmArea.addView(txtNodes);

        // 📈 GRID: Transactional Command Buttons (DEPLOY/ABORT)
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, scale(25), 0, 0);

        btnConfirm = createIconButton("DEPLOY", android.R.drawable.ic_menu_send, true, v -> {
            String name = edtZoneName.getText().toString().trim();
            String radiusStr = edtRadius.getText().toString().trim();
            int s = parseTo24H(edtStart.getText().toString());
            int e = parseTo24H(edtEnd.getText().toString());
            
            StringBuilder dBuilder = new StringBuilder();
            for(int i=0; i<7; i++) if(dayChecks[i].isChecked()) dBuilder.append(i).append(",");
            String dStr = dBuilder.length() > 0 ? dBuilder.substring(0, dBuilder.length()-1) : "";

            LatLng draft = viewModel.getDraftCoordinateLocation().getValue();
            double r = radiusStr.isEmpty() ? 100.0 : Double.parseDouble(radiusStr);
            String assigned = android.text.TextUtils.join(", ", selectedNodes);
            if (!name.isEmpty() && draft != null) {
                checkPermissionsAndAdd(name, assigned, draft.latitude, draft.longitude, r, s, e, dStr, editingZoneId);
            }
        });
        View btnAbort = createIconButton("ABORT", android.R.drawable.ic_menu_close_clear_cancel, v -> closeMapEditorState());

        actionRow.addView(btnConfirm, new LinearLayout.LayoutParams(0, -2, 1.0f));
        actionRow.addView(btnAbort, new LinearLayout.LayoutParams(0, -2, 1.0f));
        bottomConfirmArea.addView(actionRow);

        mapOverlayCard.addView(bottomConfirmArea);

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * 0.9), (int)(getResources().getDisplayMetrics().heightPixels * 0.8));
        cardLp.gravity = Gravity.CENTER;
        mapStateContainer.addView(mapOverlayCard, cardLp);
        
        rootLayer.addView(mapStateContainer);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(1005, mapFragment).commit(); 
        mapFragment.getMapAsync(this);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { if (mapStateContainer.getVisibility() == View.VISIBLE) closeMapEditorState(); else finish(); }
        });

        return rootLayer;
    }

    private void showMultiNodeSelector() {
        if (deviceList.isEmpty()) return;

        ScrollView scroll = new ScrollView(this);
        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        int q = getResponsivePaddingQuota();
        parent.setPadding(q, q, q, q);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("ASSIGN TO CLUSTER");
        
        final java.util.List<android.widget.CheckBox> checks = new java.util.ArrayList<>();
        int accent = getGradientStart();
        android.content.res.ColorStateList csl = new android.content.res.ColorStateList(
            new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
            new int[]{Color.GRAY, accent}
        );

        for (String device : deviceList) {
            android.widget.CheckBox cb = new android.widget.CheckBox(this);
            cb.setText(device);
            cb.setTextColor(getTextColor());
            cb.setButtonTintList(csl);
            cb.setChecked(selectedNodes.contains(device));
            
            // 🚀 Vertically Compact Logic
            cb.setPadding(scale(10), scale(15), scale(10), scale(15));
            
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
                            if (allCb.getText().equals("ALL CONNECTED DEVICES")) allCb.setChecked(false);
                        }
                        if (!selectedNodes.contains(device)) selectedNodes.add(device);
                    }
                }
            });
            
            checks.add(cb);
            parent.addView(cb);
        }
        
        scroll.addView(parent);
        builder.setView(scroll);
        
        builder.setPositiveButton("CONFIRM", (dialog, which) -> {
            if (selectedNodes.isEmpty()) {
                selectedNodes.add("ALL CONNECTED DEVICES");
            }
            if (txtNodes != null) {
                if (selectedNodes.size() == 1) txtNodes.setText(selectedNodes.get(0));
                else txtNodes.setText(selectedNodes.size() + " NODES SELECTED");
            }
        });
        builder.setNegativeButton("Abort", null);
        
        android.app.AlertDialog alert = builder.create();
        alert.show();
        applyDialogTheme(alert);
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
                btnText.setText("SAVE CHANGES");
                btnIcon.setImageResource(android.R.drawable.ic_menu_save);
            } else {
                btnText.setText("DEPLOY NODE");
                btnIcon.setImageResource(android.R.drawable.ic_menu_send);
            }
        }

        listStateContainer.setAlpha(0.3f);
        listStateContainer.setClickable(false);
        mapStateContainer.setVisibility(View.VISIBLE);
        viewModel.setDraftPin(null);
        Toast.makeText(this, "Long-press map to drop Anchor!", Toast.LENGTH_LONG).show();
    }

    private void closeMapEditorState() {
        editingZoneId = null;
        pendingZoneName = null;
        selectedNodes.clear();
        selectedNodes.add("ALL CONNECTED DEVICES");
        if (txtNodes != null) txtNodes.setText("ALL CONNECTED DEVICES");

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
            if(!name.isEmpty() && draft != null) checkPermissionsAndAdd(name, spinner.getSelectedItem().toString(), draft.latitude, draft.longitude, Double.parseDouble(radiusInput.getText().toString()), 0, 2359, "0,1,2,3,4,5,6", null);
        });
        builder.setNegativeButton("Abort", null).show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.setOnMapLongClickListener(latLng -> viewModel.setDraftPin(latLng));
        
        // 🚀 Intelligent Centering Bridge
        viewModel.resolveActiveProfileAddress();
        viewModel.getProfileAddress().observe(this, addr -> {
            if (addr != null && !addr.equals("Unknown Location")) {
                viewModel.executeOSGeocodingQuery(addr);
            } else {
                LatLng pune = new LatLng(18.5204, 73.8567);
                this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pune, 12f));
            }
        });

        LatLng restored = viewModel.getDraftCoordinateLocation().getValue();
        if (restored != null) { renderMapDraftRings(restored); this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restored, 16f)); }
    }

    private void renderMapDraftRings(LatLng pos) {
        if(googleMap == null) return;
        if(previewMarker != null) previewMarker.remove();
        if(previewCircle != null) previewCircle.remove();
        previewMarker = googleMap.addMarker(new MarkerOptions().position(pos).title("DRAFT ZONE").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        previewCircle = googleMap.addCircle(new CircleOptions().center(pos).radius(200f).strokeWidth(4).strokeColor(Color.GREEN).fillColor(Color.argb(50, 0, 255, 0)));
        if (btnConfirmLocation != null && mapStateContainer.getVisibility() == View.VISIBLE) {
            btnConfirmLocation.setVisibility(View.VISIBLE);
        }
    }

    private void checkPermissionsAndAdd(String n, String a, double lat, double lng, double r, int s, int e, String days, String existingId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingZoneName = n; pendingAssignedName = a; pendingLat = lat; pendingLng = lng; pendingRadius = r;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GEOFENCE_PERMISSION_CODE);
        } else {
            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            viewModel.submitCloudCoordinateSync(n, a, lat, lng, r, s, e, days, existingId);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> com.jdroidx.jfindme.utils.AngelLoader.hide(this), 3000);
        }
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

        // 🚀 SUB-TILE 3: DEVICE CLUSTER (Assigned Member Nodes - High-Fidelity Avatar Matrix)
        LinearLayout card3 = new LinearLayout(this);
        card3.setOrientation(LinearLayout.VERTICAL);
        card3.setPadding(0, q/2, 0, q/2);
        
        TextView lblCluster = new TextView(this);
        lblCluster.setText("👥 TARGET NODES:");
        lblCluster.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, lblCluster, "TEXT_TINY");
        card3.addView(lblCluster);

        LinearLayout avatarFlow = new LinearLayout(this);
        avatarFlow.setOrientation(LinearLayout.VERTICAL);
        avatarFlow.setGravity(Gravity.START);
        
        String[] nodeNames = assignedName.split(", ");
        for (String node : nodeNames) {
            String clean = node.trim();
            if (!clean.isEmpty()) {
                if (clean.equals("ALL CONNECTED DEVICES")) {
                    TextView tvAll = new TextView(this);
                    tvAll.setText("GLOBAL SECTOR BROADCAST");
                    tvAll.setTextColor(getGradientStart());
                    UiArchitect.applyText(this, tvAll, "TEXT_LABEL");
                    avatarFlow.addView(tvAll);
                } else {
                    avatarFlow.addView(createVerticalMemberRow(clean));
                }
            }
        }
        
        card3.addView(avatarFlow);

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
        ivBell.setColorFilter(alertsEnabled ? Color.parseColor("#4CAF50") : Color.GRAY);
        ivBell.setPadding(bp, 0, 0, 0);
        if (isZoneActive) ivBell.setOnClickListener(v -> viewModel.toggleBellAlertState(id, alertsEnabled));
        card4.addView(ivBell, new LinearLayout.LayoutParams(iSize + bp, iSize));

        // History
        ImageView ivHist = new ImageView(this);
        ivHist.setImageResource(android.R.drawable.ic_menu_recent_history);
        ivHist.setColorFilter(Color.parseColor("#FF9800"));
        ivHist.setPadding(bp, 0, 0, 0);
        if (isZoneActive) ivHist.setOnClickListener(v -> viewModel.fetchGeofenceHistory(id));
        card4.addView(ivHist, new LinearLayout.LayoutParams(iSize + bp, iSize));

        // Mission Edit (Assigment Redirect)
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

        // Erase
        ImageView ivDel = new ImageView(this);
        ivDel.setImageResource(android.R.drawable.ic_menu_delete);
        ivDel.setColorFilter(Color.parseColor("#F44336"));
        ivDel.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this).setTitle("ERASE PERIMETER?")
                .setMessage("Are you sure you want to decommission this forensic safe zone?")
                .setPositiveButton("ERASE", (d, w) -> { geofenceHelper.removeGeofence(id); viewModel.deleteCloudSafeguardZone(id); })
                .setNegativeButton("Abort", null).show();
        });
        card4.addView(ivDel, new LinearLayout.LayoutParams(iSize, iSize));

        mainHost.addView(card4);

        // 🚀 Intense Separator (Waterfall Hardening)
        View sep3 = new View(this);
        sep3.setBackgroundColor(getSecondaryTextColor());
        sep3.setAlpha(0.3f); 
        mainHost.addView(sep3, new LinearLayout.LayoutParams(-1, 1));

        mainHost.addView(card3);

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

    private View createPresetTile(String name, String desc, int iconRes) {
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
        tile.setOnClickListener(v -> {
            pendingZoneName = name;
            openMapEditorState();
            if (name.contains("Home")) {
                viewModel.resolveActiveProfileAddress();
                viewModel.getProfileAddress().observe(this, addr -> {
                    if (addr != null && !addr.equals("Unknown Location")) viewModel.executeOSGeocodingQuery(addr);
                });
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        int q = getResponsivePaddingQuota();
        lp.setMargins(0, 0, 0, q / 2);
        tile.setLayoutParams(lp);
        return tile;
    }

    private void editZone(RenderedGeofence zone) {
        editingZoneId = zone.getId();
        pendingZoneName = zone.getName();
        
        selectedNodes.clear();
        if (zone.getAssignedName() != null) {
            String[] parts = zone.getAssignedName().split(", ");
            for (String p : parts) selectedNodes.add(p);
        }
        if (selectedNodes.isEmpty()) selectedNodes.add("ALL CONNECTED DEVICES");
        
        openMapEditorState();
        
        // 🚀 Restore HUD State
        if (edtRadius != null) edtRadius.setText(String.valueOf((int)zone.getRadius()));
        if (txtNodes != null) {
            txtNodes.setText(selectedNodes.size() + " NODES SELECTED");
            if (selectedNodes.size() == 1) txtNodes.setText(selectedNodes.get(0));
        }
        
        LatLng target = new LatLng(zone.getLat(), zone.getLng());
        viewModel.setDraftPin(target);
        viewModel.executeCameraMove(target);

        // 🚀 Temporal UI Restoration (AM/PM Baseline)
        if (edtStart != null) {
            edtStart.setText(formatToAmPm(zone.getStartTime()));
        }
        if (edtEnd != null) {
            edtEnd.setText(formatToAmPm(zone.getEndTime()));
        }
        if (dayChecks != null) {
            String days = zone.getActiveDays();
            for(int i=0; i<7; i++) dayChecks[i].setChecked(days.contains(String.valueOf(i)));
        }
    }

    private void showTimePicker(EditText target) {
        String current = target.getText().toString();
        int h = 0, m = 0;
        try {
            String[] parts = current.split(":");
            h = Integer.parseInt(parts[0]);
            m = Integer.parseInt(parts[1]);
        } catch (Exception e) {}

        android.app.TimePickerDialog picker = new android.app.TimePickerDialog(this, (view, hour, minute) -> {
            target.setText(formatToAmPm(hour * 100 + minute));
        }, h, m, false); // 🚀 Alarm Mode: Enable AM/PM Selection
        picker.show();
    }

    private View createVerticalMemberRow(String name) {
        String role = currentRosterMap.get(name);
        if (role == null) role = "PEER";

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // 🚀 Hierarchy Indentation: Added 'Fab Space' for structural clarity
        row.setPadding(scale(30), scale(5), 0, scale(5));

        int s = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");

        // 🚀 Connectivity Indicator: Email Icon
        ImageView ivMail = new ImageView(this);
        ivMail.setImageResource(android.R.drawable.ic_dialog_email);
        ivMail.setColorFilter(getGradientStart());
        ivMail.setOnClickListener(v -> {
            android.content.Intent route = new android.content.Intent(this, GeofenceNotificationActivity.class);
            route.putExtra("TARGET_AVATAR", name);
            startActivity(route);
        });
        row.addView(ivMail, new LinearLayout.LayoutParams(s, s));

        TextView tvDetails = new TextView(this);
        tvDetails.setText(" " + name + " (" + role.toUpperCase() + ")");
        tvDetails.setTextColor(getTextColor());
        tvDetails.setPadding(scale(10), 0, 0, 0);
        UiArchitect.applyText(this, tvDetails, "TEXT_SUB");
        row.addView(tvDetails, new LinearLayout.LayoutParams(0, -2, 1.0f));

        // 🚀 TACTICAL SEND: Integrated Mission Handshake Terminal (Trailing Edge)
        ImageView ivSend = new ImageView(this);
        ivSend.setImageResource(android.R.drawable.ic_menu_send);
        ivSend.setColorFilter(getGradientStart());
        ivSend.setPadding(0, 0, getResponsivePaddingQuota(), 0);
        ivSend.setOnClickListener(v -> {
            ivSend.setColorFilter(Color.GRAY);
            ivSend.setAlpha(0.5f);
            android.widget.Toast.makeText(this, "Mission Handshake: " + name + "...", android.widget.Toast.LENGTH_SHORT).show();
            viewModel.sendForensicHandshakeRequest(name);
        });
        row.addView(ivSend, new LinearLayout.LayoutParams(s + getResponsivePaddingQuota(), s));

        return row;
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
        
        // 🚀 "Establish New Domain" Bridge: Handle inbound dossier metadata
        String intentAddress = getIntent().getStringExtra("ESTABLISH_DOMAIN_ADDRESS");
        String intentName = getIntent().getStringExtra("ESTABLISH_DOMAIN_NAME");
        if (intentAddress != null && !intentAddress.isEmpty()) {
            Toast.makeText(this, "De-encrypting Dossier Address: " + intentAddress, Toast.LENGTH_LONG).show();
            viewModel.executeOSGeocodingQuery(intentAddress);
            if (intentName != null) pendingZoneName = intentName;
        }

        viewModel.getActiveZones().observe(this, renderedGeofences -> {
            if (activeBoundariesContainer != null) activeBoundariesContainer.removeAllViews();
            
            // 🚀 Forensic Diffing Engine: Track current IDs to detect orphans
            List<String> currentIds = new ArrayList<>();
            
            for (RenderedGeofence zone : renderedGeofences) {
                String gId = zone.getId(); // 🚀 Use Forensic UID as the hardware anchor
                currentIds.add(gId);
                
                if (zone.isZoneActive()) {
                    geofenceHelper.addGeofence(gId, zone.getLat(), zone.getLng(), (float)zone.getRadius());
                } else {
                    geofenceHelper.removeGeofence(gId);
                }
                
                // 🚀 UNIFIED MISSION NODE: Group all geospatial, temporal, and nodal data into a single card
                addLocalGeofenceTile(zone.getId(), zone.getName(), zone.getAddress(), zone.getAssignedName(), zone.getLat(), zone.getLng(), zone.getRadius(), zone.getAlertsEnabled(), zone.isZoneActive(), zone.getStartTime(), zone.getEndTime(), zone.getActiveDays());
            }
            
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
        viewModel.getDraftCoordinateLocation().observe(this, latLng -> { if (latLng != null) renderMapDraftRings(latLng); else if (btnConfirmLocation != null) btnConfirmLocation.setVisibility(View.GONE); });
        
        // 🛰️ OBSERVER: Sync Targetable Nodes (L7 Lifecycle Fix)
        viewModel.fetchTargetableDevices();
        viewModel.getActiveTargetDevices().observe(this, newDevices -> {
            deviceList.clear();
            deviceList.addAll(newDevices);
        });
        
        viewModel.getUiToastEvent().observe(this, e -> { String m = e.getContentIfNotHandled(); if (m != null) Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); });
        viewModel.getCameraMoveEvent().observe(this, e -> { LatLng t = e.getContentIfNotHandled(); if (t != null && googleMap != null) googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(t, 16f)); });
        viewModel.getCloseMapEvent().observe(this, e -> { Boolean a = e.getContentIfNotHandled(); if (a != null && a) closeMapEditorState(); });
        viewModel.getHistoryDialogEvent().observe(this, e -> {
            String[] logs = e.getContentIfNotHandled();
            if (logs != null) {
                // ... (existing history logic) ...
            }
        });

        // 🚀 SYNC: Roster Member Metadata
        viewModel.getRosterMapping().observe(this, mapping -> {
            currentRosterMap.clear();
            currentRosterMap.putAll(mapping);
            if (viewModel.getActiveZones().getValue() != null) {
                // Refresh list to show updated roles/avatars
                viewModel.fetchRosterMetadata();
            }
        });
    }

    // 🚀 L7 ORIENTATION SHIELD: Preserve Forensic Editor State
    private boolean isEditorActive = false;

    @Override
    protected void onSaveCurrentState() {
        // Capture live state before layout teardown
        isEditorActive = (mapStateContainer != null && mapStateContainer.getVisibility() == View.VISIBLE);
    }

    @Override
    protected void onUiRebuilt() {
        // Restore visibility after orientation matrix re-calculation
        if (isEditorActive) {
            openMapEditorState();
        }
        
        // 🛰️ Map Fragment Re-Injection: Forced handshake after layout re-pivot
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(1005, mapFragment).commit();
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GEOFENCE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            viewModel.submitCloudCoordinateSync(pendingZoneName, pendingAssignedName, pendingLat, pendingLng, pendingRadius != null ? pendingRadius : 100.0);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> com.jdroidx.jfindme.utils.AngelLoader.hide(this), 3000);
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

    private View buildIdentityBlock() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(scale(25), scale(25), scale(25), scale(25)); 
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        myAvatarField = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, myAvatarField);
        
        // 🚀 Premium Circular Avatar (Responsive Scaling)
        int avatarSize = scale(168); 
        android.graphics.drawable.GradientDrawable c = new android.graphics.drawable.GradientDrawable();
        c.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        c.setColor(Color.TRANSPARENT);
        c.setStroke(scale(4), getGradientStart());
        myAvatarField.setBackground(c);
        
        row.addView(myAvatarField, new LinearLayout.LayoutParams(avatarSize, avatarSize));
        
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(scale(40), 0, 0, 0);
        
        // --- 1. OWNER NAME ---
        com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        java.util.Map<String, String> myDos = db.getLocalDossier(myUid != null ? myUid : "OWNER_001");
        if (myDos == null) myDos = db.getLocalDossier("OWNER_001");
        
        android.content.SharedPreferences prefs = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE);
        String nameStr = myDos != null ? myDos.get("full_name") : (prefs != null ? prefs.getString("user_nickname", "User Identity") : "User Identity");
        
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
        String safeUid = (myUidCheck != null) ? myUidCheck : "MOCK_U123";
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
        String localActiveGroup = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE).getString("active_group", null);
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
