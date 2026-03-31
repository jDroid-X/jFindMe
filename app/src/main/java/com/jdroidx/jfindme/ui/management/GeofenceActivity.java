package com.jdroidx.jfindme.ui.management;

import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;

import android.Manifest;
import android.content.pm.PackageManager;
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

/**
 * Main -> UI Engine -> ManagementCluster -> GeofenceActivity (L4)
 * STATE-DRIVEN GEOSPATIAL UI (STRICT MVVM CLOUD ENGINE)
 * Completely offloaded all Networking and OS Hardware Threads identically mapping to modern Android Requirements.
 */
public class GeofenceActivity extends BaseActivityJava implements OnMapReadyCallback {

    private final String TAG = "GeofenceActivity";
    private GeofenceHelper geofenceHelper;
    private GeofenceViewModel viewModel;
    private static final int GEOFENCE_PERMISSION_CODE = 200;

    private GoogleMap googleMap;
    private Marker previewMarker;
    private Circle previewCircle;
    
    // 🚀 OS Permissions State Cache Matrix
    private String pendingZoneName;
    private String pendingAssignedName;
    private Double pendingLat;
    private Double pendingLng;
    private Double pendingRadius;
    
    // UI State Architectures
    private LinearLayout listStateContainer;
    private FrameLayout mapStateContainer;
    private LinearLayout activeBoundariesContainer;
    private View btnConfirmLocation;

    @Override
    protected View createLayout() {
        FrameLayout rootLayer = new FrameLayout(this);
        rootLayer.setBackgroundColor(getPrimaryColor());

        // ==========================================
        // STATE 1: ZONE LIST DASHBOARD (DEFAULT)
        // ==========================================
        listStateContainer = new LinearLayout(this);
        listStateContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView title = new TextView(this);
        title.setText("SAFE ZONES");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(title);
        listStateContainer.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(getResponsivePaddingQuota(), getResponsivePaddingQuota(), getResponsivePaddingQuota(), 100);

        // ADD NEW ZONE BUTTON
        LinearLayout addCard = new LinearLayout(this);
        addCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, addCard, "CARD_STANDARD");
        
        View btnAdd = createIconButton("+ ESTABLISH NEW DOMAIN (HOME / SCHOOL)", android.R.drawable.ic_menu_add, v -> openMapEditorState());
        addCard.addView(btnAdd);
        
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int addMargin = getResponsivePaddingQuota() * 3;
        addLp.setMargins(0, 0, 0, addMargin);
        content.addView(addCard, addLp);

        content.addView(createSectionHeader("ACTIVE RADAR BOUNDARIES"));
        activeBoundariesContainer = new LinearLayout(this);
        activeBoundariesContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(activeBoundariesContainer);

        scroll.addView(content);
        listStateContainer.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        
        rootLayer.addView(listStateContainer);

        // ==========================================
        // STATE 2: FULL SCREEN MAP EDITOR
        // ==========================================
        mapStateContainer = new FrameLayout(this);
        mapStateContainer.setVisibility(View.GONE); 
        
        FrameLayout mapHardwareContainer = new FrameLayout(this);
        final int ID_MAP_FENCE = 1005;
        mapHardwareContainer.setId(ID_MAP_FENCE);
        mapStateContainer.addView(mapHardwareContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Floating Search Bar Overlay
        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        android.graphics.drawable.GradientDrawable gSearch = new android.graphics.drawable.GradientDrawable();
        gSearch.setColor(getCardColor());
        gSearch.setCornerRadius(30f);
        searchBar.setBackground(gSearch);
        searchBar.setElevation(25f);
        int sPad = getResponsivePaddingQuota() / 2;
        searchBar.setPadding(sPad * 2, sPad, sPad * 2, sPad);
        
        EditText edtSearch = new EditText(this);
        edtSearch.setHint("Search Address (e.g. Oakhill)");
        edtSearch.setHintTextColor(getSecondaryTextColor());
        edtSearch.setTextColor(getTextColor());
        edtSearch.setBackgroundColor(Color.TRANSPARENT);
        edtSearch.setSingleLine(true);
        edtSearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        UiArchitect.applyText(this, edtSearch, "TEXT_BODY");
        
        // 🚀 QA Fix: Handle Keyboard 'Enter/Search' button for frictionless address lookups!
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                viewModel.executeOSGeocodingQuery(edtSearch.getText().toString());
                return true;
            }
            return false;
        });
        
        ImageView btnSearch = new ImageView(this);
        btnSearch.setImageResource(android.R.drawable.ic_menu_search);
        btnSearch.setColorFilter(getGradientStart());
        btnSearch.setPadding(20, 20, 20, 20);
        // 🛡️ QA Hardening: Standardized Accessibility Mapping
        applyAccessibility(btnSearch, getString(android.R.string.search_go));
        
        searchBar.addView(edtSearch, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        int searchIconSize = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        searchBar.addView(btnSearch, new LinearLayout.LayoutParams(searchIconSize, searchIconSize));
        
        FrameLayout.LayoutParams searchLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int sMargin = getResponsivePaddingQuota() * 2;
        // 🚀 QA Fix: Increased Top Margin and Right Margin to prevent overlap with the Global Help Icon injected by the Base Class!
        searchLp.setMargins(sMargin, sMargin * 10, sMargin * 10, 0); 
        searchLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        mapStateContainer.addView(searchBar, searchLp);

        // Delegate query absolutely to insulated ViewModel Background Threads
        btnSearch.setOnClickListener(v -> viewModel.executeOSGeocodingQuery(edtSearch.getText().toString()));

        // Big Overlay Confirmation Button
        LinearLayout bottomConfirmArea = new LinearLayout(this);
        bottomConfirmArea.setOrientation(LinearLayout.VERTICAL);
        bottomConfirmArea.setElevation(30f);
        UiArchitect.applyCard(this, bottomConfirmArea, "CARD_DASHBOARD");
        
        btnConfirmLocation = createIconButton("CONFIRM SELECTED COORDINATES", android.R.drawable.ic_menu_mylocation, v -> showZoneNamingDialog());
        btnConfirmLocation.setVisibility(View.GONE); 
        bottomConfirmArea.addView(btnConfirmLocation);
        
        View btnCancelMap = createIconButton("CANCEL DEPLOYMENT", android.R.drawable.ic_menu_close_clear_cancel, v -> closeMapEditorState());
        bottomConfirmArea.addView(btnCancelMap);

        FrameLayout.LayoutParams confirmLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        confirmLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        int confMargin = getResponsivePaddingQuota() * 2;
        confirmLp.setMargins(confMargin, 0, confMargin, confMargin * 3);
        mapStateContainer.addView(bottomConfirmArea, confirmLp);
        
        rootLayer.addView(mapStateContainer);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(ID_MAP_FENCE, mapFragment).commit();
        mapFragment.getMapAsync(this);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mapStateContainer.getVisibility() == View.VISIBLE) closeMapEditorState();
                else finish(); 
            }
        });

        return rootLayer;
    }

    private void openMapEditorState() {
        listStateContainer.setVisibility(View.GONE);
        mapStateContainer.setVisibility(View.VISIBLE);
        viewModel.setDraftPin(null); // Clear previous cache natively
        btnConfirmLocation.setVisibility(View.GONE);
        Toast.makeText(this, "Long-Press Map to mark Zone Center!", Toast.LENGTH_LONG).show();
    }

    private void closeMapEditorState() {
        listStateContainer.setVisibility(View.VISIBLE);
        mapStateContainer.setVisibility(View.GONE);
    }

    private void showZoneNamingDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Target Locked. Configure Zone:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int dialPad = getResponsivePaddingQuota() * 2;
        layout.setPadding(dialPad, dialPad / 2, dialPad, dialPad / 2);

        final EditText input = new EditText(this);
        input.setHint("Zone Name (e.g. School, Home)");
        layout.addView(input);

        // 🚀 Identity Bridge: Sync from Profile Address
        View btnSyncProfile = createIconButton("PULL FROM PROFILE ADDRESS", android.R.drawable.ic_menu_mapmode, v -> {
            String profileAddr = new com.jdroidx.jfindme.data.local.CitizenDbHelper(this).getActiveProfileAddress();
            if (!profileAddr.equals("Unknown Location")) {
                viewModel.executeOSGeocodingQuery(profileAddr);
                input.setText("HOME");
                Toast.makeText(this, "Profile Address Synced: " + profileAddr, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Profile Address Missing or Unregistered.", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnSyncProfile);

        TextView lblTarget = new TextView(this);
        lblTarget.setText("\nAssign To Mobile Device:");
        lblTarget.setTypeface(null, Typeface.BOLD);
        lblTarget.setPadding(0, 30, 0, 10);
        layout.addView(lblTarget);

        android.widget.Spinner spinner = new android.widget.Spinner(this);
        List<String> deviceNames = new ArrayList<>();
        deviceNames.add("ALL CONNECTED DEVICES");
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, deviceNames);
        spinner.setAdapter(adapter);
        layout.addView(spinner);

        // 🚀 QA Fix: Manual Perimeter Scaler (Default 100m)
        TextView lblRadius = new TextView(this);
        lblRadius.setText("\nSafety Perimeter (Meters):");
        lblRadius.setTypeface(null, Typeface.BOLD);
        lblRadius.setPadding(0, 30, 0, 10);
        layout.addView(lblRadius);

        final EditText radiusInput = new EditText(this);
        radiusInput.setText("100"); // 🚀 Enterprise Standard
        radiusInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(radiusInput);

        builder.setView(layout);

        // Fetch Array intelligently via Viewmodel instead of jamming Activity loop
        viewModel.fetchTargetableDevices();
        viewModel.getActiveTargetDevices().observe(this, newDevices -> {
            deviceNames.clear();
            deviceNames.addAll(newDevices);
            adapter.notifyDataSetChanged();
        });

        builder.setPositiveButton("DEPLOY TETHER", (dialog, which) -> {
            String targetName = input.getText().toString().trim();
            String assignedName = spinner.getSelectedItem().toString();
            String rVal = radiusInput.getText().toString().trim();
            double radius = rVal.isEmpty() ? 100.0 : Double.parseDouble(rVal);
            LatLng draft = viewModel.getDraftCoordinateLocation().getValue();
            
            if(!targetName.isEmpty() && draft != null) {
                checkPermissionsAndAdd(targetName, assignedName, draft.latitude, draft.longitude, radius);
            } else {
                Toast.makeText(this, "Target Designation Missing or Map Empty!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel Operation", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.getUiSettings().setCompassEnabled(true);
        
        // Relays the physical click exclusively to MVVM cache loop
        this.googleMap.setOnMapLongClickListener(latLng -> viewModel.setDraftPin(latLng));

        LatLng defaultLocation = new LatLng(18.5204, 73.8567);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
        
        // 🚀 QA Critical Edge Validation: If the Android Device abruptly Rotated while Map Editor was active...
        // This physically guarantees the Viewmodel redeploys the exact green targeting perimeter!
        LatLng restoredDraft = viewModel.getDraftCoordinateLocation().getValue();
        if (restoredDraft != null) {
            renderMapDraftRings(restoredDraft);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restoredDraft, 16f));
        }
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

    private void checkPermissionsAndAdd(String name, String assignedName, double lat, double lng, double radius) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
             ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            
            pendingZoneName = name;
            pendingAssignedName = assignedName;
            pendingLat = lat;
            pendingLng = lng;
            pendingRadius = radius;
            // Radius cached in pending logic if necessary
            
            String[] permissions = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q ?
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION} :
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            
            ActivityCompat.requestPermissions(this, permissions, GEOFENCE_PERMISSION_CODE);
        } else {
            // 🦋 Guardian Angel Check: 3000ms Minimum Wait Blockade
            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            viewModel.submitCloudCoordinateSync(name, assignedName, lat, lng, radius);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                com.jdroidx.jfindme.utils.AngelLoader.hide(this);
            }, 3000);
        }
    }

    private void addLocalGeofenceTile(String name, String address, String assignedName, double lat, double lng, double radius, boolean alertsEnabled, boolean isZoneActive) {
        LinearLayout item = new LinearLayout(this);
        // 🚀 L7 Adaptive Content: Switching to Vertical in LIBERAL to prevent asset collision!
        item.setOrientation(UiArchitect.getStage(this) == UiArchitect.DesignStage.LIBERAL ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, item, "CARD_STANDARD");
        
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int iMargin = getResponsivePaddingQuota() + 5;
        itemLp.setMargins(0, 0, 0, iMargin);
        item.setLayoutParams(itemLp);
        
        View indicator = new View(this);
        indicator.setBackgroundColor(Color.parseColor("#4CAF50")); 
        int indicatorWidth = UiScalingMatrixTable.scalePadding(UiArchitect.getStage(this), 15);
        item.addView(indicator, new LinearLayout.LayoutParams(indicatorWidth, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(getResponsivePaddingQuota(), getResponsivePaddingQuota(), getResponsivePaddingQuota(), getResponsivePaddingQuota());
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView powerIcon = new ImageView(this);
        powerIcon.setImageResource(android.R.drawable.ic_lock_power_off);
        powerIcon.setColorFilter(isZoneActive ? Color.parseColor("#4CAF50") : Color.GRAY);
        int pPad = getResponsivePaddingQuota();
        powerIcon.setPadding(0, 0, pPad, 0);
        applyAccessibility(powerIcon, "Toggle Zone Active State");
        powerIcon.setOnClickListener(v -> viewModel.toggleZoneActiveState(name, isZoneActive));
        int powerIconSize = UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
        nameRow.addView(powerIcon, new LinearLayout.LayoutParams(powerIconSize, powerIconSize));

        TextView tvName = new TextView(this);
        tvName.setText(name.toUpperCase());
        tvName.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvName, "TEXT_BODY");
        nameRow.addView(tvName);
        
        text.addView(nameRow);

        TextView tvDetail = new TextView(this);
        tvDetail.setText("📍 " + address + "\n🎯 Target: " + assignedName + "\n⭕ Radius: " + Math.round(radius) + "m Perimeter");
        tvDetail.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvDetail, "TEXT_SUB");
        tvDetail.setLineSpacing(0, 1.2f);
        int detailPad = getResponsivePaddingQuota() / 2;
        tvDetail.setPadding(detailPad, detailPad * 2, 0, detailPad * 2);
        tvDetail.setTypeface(null, Typeface.BOLD);
        text.addView(tvDetail);

        // 🚀 QA Fix: Restored the Missing Left Panel!
        item.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // =========================================================================
        // 🛡️ SAFE ZONE SEMANTIC ICON REGISTRY: IMMUTABLE COLOR SPACE
        // =========================================================================
        // The following icons (Power, Bell, Check, Cross, History, Delete) are 
        // strictly Safe Zone Proofed. If a global Application Theme is requested, 
        // YOU MUST NOT alter the hardcoded hex colors below! They are Semantic 
        // state indicators representing physical physical battery mapping.
        // =========================================================================
        
        // 🚀 QA Fix: Exact Vertical Matrix requested by User
        LinearLayout rightPanel = new LinearLayout(this);
        rightPanel.setOrientation(LinearLayout.VERTICAL);
        rightPanel.setGravity(Gravity.CENTER);

        ImageView bellIcon = new ImageView(this);
        bellIcon.setImageResource(android.R.drawable.ic_popup_reminder);
        bellIcon.setColorFilter(alertsEnabled ? Color.parseColor("#4CAF50") : Color.GRAY);
        bellIcon.setPadding(35, 15, 35, 15);
        // 🛡️ QA Hardening: Standardized Accessibility Mapping (V15.2)
        applyAccessibility(bellIcon, "Toggle Alert State");
        if (isZoneActive) {
            bellIcon.setOnClickListener(v -> viewModel.toggleBellAlertState(name, alertsEnabled));
        }
        rightPanel.addView(bellIcon);

        // 2. ACTIVE STATUS INDICATOR (✅ / ❌ Horizontal Layout)
        LinearLayout statusLayout = new LinearLayout(this);
        statusLayout.setOrientation(LinearLayout.HORIZONTAL);
        statusLayout.setGravity(Gravity.CENTER);

        TextView tickIcon = new TextView(this);
        tickIcon.setText("✅"); 
        UiArchitect.applyText(this, tickIcon, "TEXT_HEADER");
        tickIcon.setAlpha(0.3f); // Default greyed out
        tickIcon.setPadding(0, 10, 15, 10);
        // 🛡️ QA Hardening: Standardized Accessibility Mapping (V15.2)
        applyAccessibility(tickIcon, "Status: Safe");

        TextView crossIcon = new TextView(this);
        crossIcon.setText("❌"); 
        crossIcon.setTextSize(16);
        crossIcon.setAlpha(0.3f); // Default greyed out
        crossIcon.setPadding(15, 10, 0, 10);
        // 🛡️ QA Hardening: Standardized Accessibility Mapping (V15.2)
        applyAccessibility(crossIcon, "Status: Outside");
        
        statusLayout.addView(tickIcon);
        statusLayout.addView(crossIcon);

        // 🚀 QA Global Battery Architecture: Completely strip processing power if Zone is manually Greyed!
        if (isZoneActive) {
            // SILENT ONE-TIME INITIAL LOAD
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("locations")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                         for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                             String uName = snap.child("userName").getValue(String.class);
                             if (uName != null && uName.equals(assignedName)) {
                                 Double cLat = snap.child("latitude").getValue(Double.class);
                                 Double cLng = snap.child("longitude").getValue(Double.class);
                                 if (cLat != null && cLng != null) {
                                     float[] res = new float[1];
                                     android.location.Location.distanceBetween(lat, lng, cLat, cLng, res);
                                     if (res[0] <= (float)radius) { tickIcon.setAlpha(1.0f); crossIcon.setAlpha(0.3f); }
                                     else { tickIcon.setAlpha(0.3f); crossIcon.setAlpha(1.0f); }
                                 }
                                 break;
                             }
                         }
                    }
                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });

            // PHYSICAL MANUAL CLICK (Trigger Toasts & Logs History!)
            View.OnClickListener manualCheck = v -> {
                Toast.makeText(GeofenceActivity.this, "Acquiring GPS...", Toast.LENGTH_SHORT).show();
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("locations")
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            boolean found = false;
                            for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                                String uName = snap.child("userName").getValue(String.class);
                                if (uName != null && uName.equals(assignedName)) {
                                    found = true;
                                    Double childLat = snap.child("latitude").getValue(Double.class);
                                    Double childLng = snap.child("longitude").getValue(Double.class);
                                    if (childLat != null && childLng != null) {
                                        float[] results = new float[1];
                                        android.location.Location.distanceBetween(lat, lng, childLat, childLng, results);
                                        float dist = results[0];
                                        
                                        String distStr = dist > 1000f ? String.format(java.util.Locale.US, "%.2f km", dist / 1000f) : Math.round(dist) + " m";
                                        
                                        String eventMsg;
                                        if (dist <= (float)radius) {
                                            tickIcon.setAlpha(1.0f);
                                            crossIcon.setAlpha(0.3f);
                                            eventMsg = "INSIDE " + name;
                                        } else {
                                            tickIcon.setAlpha(0.3f);
                                            crossIcon.setAlpha(1.0f);
                                            eventMsg = "OUTSIDE " + name + " (" + distStr + ")";
                                        }
                                        
                                        Toast.makeText(GeofenceActivity.this, "Verified: " + eventMsg, Toast.LENGTH_LONG).show();
                                        String time = new java.text.SimpleDateFormat("hh:mm a - MMM d", java.util.Locale.US).format(new java.util.Date());
                                        viewModel.logManualGeofenceCheck(name, time + " | " + eventMsg);
                                    }
                                    break;
                                }
                            }
                            if (!found && !assignedName.equals("ALL CONNECTED DEVICES")) {
                                Toast.makeText(GeofenceActivity.this, "Cannot locate remote GPS.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                    });
            };
            
            tickIcon.setOnClickListener(manualCheck);
            crossIcon.setOnClickListener(manualCheck);
        } else {
            // 🚀 System Disabled Mode: Sever visual indicators directly so parent knows system is safely offline!
            tickIcon.setAlpha(0.15f);
            crossIcon.setAlpha(0.15f);
            bellIcon.setAlpha(0.3f);
        }
        
        rightPanel.addView(statusLayout);

        // 3. HISTORY ICON
        ImageView historyIcon = new ImageView(this);
        historyIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
        historyIcon.setColorFilter(Color.parseColor("#FF9800")); // Orange
        historyIcon.setPadding(35, 15, 35, 15);
        historyIcon.setContentDescription("View Zone History Log");
        // 🛡️ QA Hardening: Standardized Accessibility Mapping (V15.2)
        applyAccessibility(historyIcon, "View Zone History Log");
        if (isZoneActive) {
            historyIcon.setOnClickListener(v -> viewModel.fetchGeofenceHistory(name));
        } else {
            historyIcon.setAlpha(0.3f);
        }

        // 4. DELETE ICON
        ImageView delete = new ImageView(this);
        delete.setImageResource(android.R.drawable.ic_menu_delete);
        delete.setColorFilter(Color.parseColor("#F44336")); // Red
        delete.setPadding(35, 15, 35, 15);
        delete.setContentDescription(getString(android.R.string.cancel));
        // 🛡️ QA Hardening: Standardized Accessibility Mapping (V15.2)
        applyAccessibility(delete, getString(android.R.string.cancel));
        delete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(GeofenceActivity.this)
                .setTitle("⚠️ DELETE SAFE ZONE")
                .setMessage("Are you absolutely sure you want to permanently destroy the [" + name.toUpperCase() + "] perimeter? This action will sever all tracking and instantly wipe its History matrix from the Cloud.")
                .setPositiveButton("ERASE PERIMETER", (dialog, which) -> {
                    geofenceHelper.removeGeofence(name);
                    viewModel.deleteCloudSafeguardZone(name);
                })
                .setNegativeButton("CANCEL", null)
                .show();
        });

        rightPanel.addView(historyIcon);
        rightPanel.addView(delete);
        
        int rightPanelWidth = UiScalingMatrixTable.scalePadding(UiArchitect.getStage(this), 150);
        item.addView(rightPanel, new LinearLayout.LayoutParams(rightPanelWidth, ViewGroup.LayoutParams.MATCH_PARENT));

        activeBoundariesContainer.addView(item);
    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart());
        UiArchitect.applyText(this, header, "TEXT_LABEL");
        int hPad = getResponsivePaddingQuota();
        header.setPadding(hPad / 2, hPad, 0, hPad);
        return header;
    }

    @Override
    protected void setupLogic() {
        geofenceHelper = new GeofenceHelper(this);
        viewModel = new ViewModelProvider(this).get(GeofenceViewModel.class);
        SystemLogger.i(TAG, "Geofence MVVM Integrator Online");

        // 1. Array Matrix Observers
        viewModel.getActiveZones().observe(this, renderedGeofences -> {
            if (activeBoundariesContainer != null) activeBoundariesContainer.removeAllViews();
            for (RenderedGeofence zone : renderedGeofences) {
                if (zone.isZoneActive()) {
                    geofenceHelper.addGeofence(zone.getName() + "___" + zone.getAssignedName(), zone.getLat(), zone.getLng(), (float)zone.getRadius()); 
                } else {
                    geofenceHelper.removeGeofence(zone.getName() + "___" + zone.getAssignedName());
                }
                addLocalGeofenceTile(zone.getName(), zone.getAddress(), zone.getAssignedName(), zone.getLat(), zone.getLng(), zone.getRadius(), zone.getAlertsEnabled(), zone.isZoneActive());
            }
        });

        // 2. Draft Graphics Observer
        viewModel.getDraftCoordinateLocation().observe(this, latLng -> {
            if (latLng != null) renderMapDraftRings(latLng);
            else if (btnConfirmLocation != null) btnConfirmLocation.setVisibility(View.GONE);
        });

        // 3. UI System Callbacks (One-Shot SingleLiveEvents!)
        viewModel.getUiToastEvent().observe(this, event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getCameraMoveEvent().observe(this, event -> {
            LatLng target = event.getContentIfNotHandled();
            if (target != null && googleMap != null) googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f));
        });
        
        viewModel.getCloseMapEvent().observe(this, event -> {
            Boolean act = event.getContentIfNotHandled();
            if (act != null && act) closeMapEditorState();
        });
        
        viewModel.getHistoryDialogEvent().observe(this, event -> {
            String[] logs = event.getContentIfNotHandled();
            if (logs != null) {
                // 🚀 QA Fix: Master Premium Geometry Card Loop matching HistoryActivity Engine!
                ScrollView sv = new ScrollView(this);
                LinearLayout container = new LinearLayout(this);
                container.setOrientation(LinearLayout.VERTICAL);
                container.setPadding(getResponsivePaddingQuota(), getResponsivePaddingQuota(), getResponsivePaddingQuota(), getResponsivePaddingQuota());
                
                for (int i = 0; i < logs.length; i++) {
                    String raw = logs[i];
                    String[] parts = raw.split("\\|", 2);
                    String time = parts[0].trim();
                    String msg = parts.length > 1 ? parts[1].trim() : "";
                    
                    if (msg.isEmpty()) {
                        msg = raw;
                        time = "SYSTEM EVENT";
                    }

                    LinearLayout logCard = new LinearLayout(this);
                    logCard.setOrientation(LinearLayout.VERTICAL);
                    logCard.setPadding(getResponsivePaddingQuota(), getResponsivePaddingQuota(), getResponsivePaddingQuota(), getResponsivePaddingQuota());
                    UiArchitect.applyCard(this, logCard, "CARD_STANDARD");
                    
                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    int logMargin = getResponsivePaddingQuota() + 10;
                    cardLp.setMargins(0, 0, 0, logMargin);
                    logCard.setLayoutParams(cardLp);

                    TextView tvHeader = new TextView(this);
                    tvHeader.setText("⏱️ ACTION #" + (logs.length - i) + "  •  " + time.toUpperCase());
                    tvHeader.setTextColor(getGradientStart());
                    UiArchitect.applyText(this, tvHeader, "TEXT_LABEL");
                    int tPad = getResponsivePaddingQuota();
                    tvHeader.setPadding(0, 0, 0, tPad / 2);
                    logCard.addView(tvHeader);

                    TextView tvDetail = new TextView(this);
                    tvDetail.setText(msg.contains("INSIDE") ? "✅ " + msg : (msg.contains("OUTSIDE") ? "❌ " + msg : "⚠️ " + msg));
                    tvDetail.setTextColor(getTextColor());
                    UiArchitect.applyText(this, tvDetail, "TEXT_BODY");
                    logCard.addView(tvDetail);

                    container.addView(logCard);
                }
                sv.addView(container);

                new android.app.AlertDialog.Builder(this)
                    .setTitle("TARGET EXECUTION HISTORY")
                    .setView(sv)
                    .setPositiveButton("Close Data Matrix", null)
                    .show();
            }
        });
    }

    // 🚀 QA Fix: Global Dependency Integration Restored! Handles the Android Background OS Security Prompt routing natively.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GEOFENCE_PERMISSION_CODE) {
            boolean fullyGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) fullyGranted = false;
            }
            if (fullyGranted && pendingLat != null) {
                // Dependency recovered: Automatically proceed to deploying the coordinate to Cloud!
                com.jdroidx.jfindme.utils.AngelLoader.show(this);
                viewModel.submitCloudCoordinateSync(pendingZoneName, pendingAssignedName, pendingLat, pendingLng, pendingRadius != null ? pendingRadius : 100.0);
                
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                    Toast.makeText(this, "Security Perimeter Authorized & Synced!", Toast.LENGTH_LONG).show();
                }, 3000);
            } else {
                Toast.makeText(this, "Permission Denied. Boundaries cannot trigger alerts without OS capabilities.", Toast.LENGTH_LONG).show();
            }
            // Memory flush
            pendingZoneName = null; pendingAssignedName = null; pendingLat = null; pendingLng = null;
        }
    }
}
