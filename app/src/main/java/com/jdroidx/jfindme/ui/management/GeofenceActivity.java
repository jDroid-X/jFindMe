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
 * Hardening: L7 Architectural Registry Parity & Z-Axis Interaction Interception.
 */
public class GeofenceActivity extends BaseActivityJava implements OnMapReadyCallback {

    private final String TAG = "GeofenceActivity";
    private GeofenceHelper geofenceHelper;
    private GeofenceViewModel viewModel;
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
        int q = getResponsivePaddingQuota();
        content.setPadding(q, q, q, scale(120));

        // ADD NEW ZONE BUTTON
        LinearLayout addCard = new LinearLayout(this);
        addCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, addCard, "CARD_STANDARD", 1.0f);
        
        View btnAdd = createIconButton("+ ESTABLISH NEW DOMAIN", android.R.drawable.ic_menu_add, v -> openMapEditorState());
        addCard.addView(btnAdd);
        
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addLp.setMargins(0, 0, 0, q * 3);
        content.addView(addCard, addLp);

        content.addView(createSectionHeader("ACTIVE RADAR BOUNDARIES"));
        activeBoundariesContainer = new LinearLayout(this);
        activeBoundariesContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(activeBoundariesContainer);

        scroll.addView(content);
        listStateContainer.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        
        rootLayer.addView(listStateContainer);

        // STATE 2: FULL SCREEN MAP EDITOR
        mapStateContainer = new FrameLayout(this);
        mapStateContainer.setVisibility(View.GONE); 
        
        FrameLayout mapHardwareContainer = new FrameLayout(this);
        mapHardwareContainer.setId(1005);
        mapStateContainer.addView(mapHardwareContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Floating Search Bar Overlay
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
        edtSearch.setHint("Search Address...");
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
        
        ImageView btnSearch = new ImageView(this);
        btnSearch.setImageResource(android.R.drawable.ic_menu_search);
        btnSearch.setColorFilter(getGradientStart());
        int sIconPad = scale(20);
        btnSearch.setPadding(sIconPad, sIconPad, sIconPad, sIconPad);
        applyAccessibility(btnSearch, "Search Location");
        
        searchBar.addView(edtSearch, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        int searchIconSize = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        searchBar.addView(btnSearch, new LinearLayout.LayoutParams(searchIconSize, searchIconSize));
        
        FrameLayout.LayoutParams searchLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.setMargins(q * 2, q * 10, q * 10, 0); 
        searchLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        mapStateContainer.addView(searchBar, searchLp);

        btnSearch.setOnClickListener(v -> viewModel.executeOSGeocodingQuery(edtSearch.getText().toString()));

        // Confirmation Panel
        LinearLayout bottomConfirmArea = new LinearLayout(this);
        bottomConfirmArea.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, bottomConfirmArea, "CARD_DASHBOARD", 1.5f);
        
        btnConfirmLocation = createIconButton("CONFIRM RADIUS ANCHOR", android.R.drawable.ic_menu_mylocation, v -> showZoneNamingDialog());
        btnConfirmLocation.setVisibility(View.GONE); 
        bottomConfirmArea.addView(btnConfirmLocation);
        
        View btnCancelMap = createIconButton("CANCEL DEPLOYMENT", android.R.drawable.ic_menu_close_clear_cancel, v -> closeMapEditorState());
        bottomConfirmArea.addView(btnCancelMap);

        FrameLayout.LayoutParams confirmLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        confirmLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        confirmLp.setMargins(q * 2, 0, q * 2, q * 3);
        mapStateContainer.addView(bottomConfirmArea, confirmLp);
        
        rootLayer.addView(mapStateContainer);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(1005, mapFragment).commit();
        mapFragment.getMapAsync(this);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { if (mapStateContainer.getVisibility() == View.VISIBLE) closeMapEditorState(); else finish(); }
        });

        return rootLayer;
    }

    private void openMapEditorState() {
        listStateContainer.setVisibility(View.GONE);
        listStateContainer.setClickable(false);
        mapStateContainer.setVisibility(View.VISIBLE);
        mapStateContainer.setClickable(true);
        mapStateContainer.setFocusable(true);
        viewModel.setDraftPin(null);
        btnConfirmLocation.setVisibility(View.GONE);
        Toast.makeText(this, "Long-Press Map to drop Radius Anchor!", Toast.LENGTH_LONG).show();
    }

    private void closeMapEditorState() {
        mapStateContainer.setVisibility(View.GONE);
        mapStateContainer.setClickable(false);
        listStateContainer.setVisibility(View.VISIBLE);
        listStateContainer.setClickable(true);
        listStateContainer.setAlpha(1.0f);
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
            String addr = new com.jdroidx.jfindme.data.local.DatabaseHelper(this).getActiveProfileAddress();
            if (!addr.equals("Unknown Location")) { viewModel.executeOSGeocodingQuery(addr); input.setText("HOME"); }
        }));

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
            if(!name.isEmpty() && draft != null) checkPermissionsAndAdd(name, spinner.getSelectedItem().toString(), draft.latitude, draft.longitude, Double.parseDouble(radiusInput.getText().toString()));
        });
        builder.setNegativeButton("Abort", null).show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.setOnMapLongClickListener(latLng -> viewModel.setDraftPin(latLng));
        LatLng pune = new LatLng(18.5204, 73.8567);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pune, 12f));
        LatLng restored = viewModel.getDraftCoordinateLocation().getValue();
        if (restored != null) { renderMapDraftRings(restored); this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restored, 16f)); }
    }

    private void renderMapDraftRings(LatLng pos) {
        if(googleMap == null) return;
        if(previewMarker != null) previewMarker.remove();
        if(previewCircle != null) previewCircle.remove();
        previewMarker = googleMap.addMarker(new MarkerOptions().position(pos).title("DRAFT ZONE").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        previewCircle = googleMap.addCircle(new CircleOptions().center(pos).radius(200f).strokeWidth(4).strokeColor(Color.GREEN).fillColor(Color.argb(50, 0, 255, 0)));
        if (btnConfirmLocation != null && mapStateContainer.getVisibility() == View.VISIBLE) btnConfirmLocation.setVisibility(View.VISIBLE);
    }

    private void checkPermissionsAndAdd(String n, String a, double lat, double lng, double r) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingZoneName = n; pendingAssignedName = a; pendingLat = lat; pendingLng = lng; pendingRadius = r;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GEOFENCE_PERMISSION_CODE);
        } else {
            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            viewModel.submitCloudCoordinateSync(n, a, lat, lng, r);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> com.jdroidx.jfindme.utils.AngelLoader.hide(this), 3000);
        }
    }

    private void addLocalGeofenceTile(String name, String address, String assignedName, double lat, double lng, double radius, boolean alertsEnabled, boolean isZoneActive) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(UiArchitect.getStage(this) == UiArchitect.DesignStage.LIBERAL ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCardWithOverride(this, item, "CARD_STANDARD", 1.1f);
        
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itemLp.setMargins(0, 0, 0, scale(20));
        item.setLayoutParams(itemLp);
        
        View indicator = new View(this);
        indicator.setBackgroundColor(Color.parseColor("#4CAF50")); 
        item.addView(indicator, new LinearLayout.LayoutParams(scale(15), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        int tp = getResponsivePaddingQuota();
        text.setPadding(tp, tp, tp, tp);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView powerIcon = new ImageView(this);
        powerIcon.setImageResource(android.R.drawable.ic_lock_power_off);
        powerIcon.setColorFilter(isZoneActive ? Color.parseColor("#4CAF50") : Color.GRAY);
        powerIcon.setPadding(0, 0, scale(20), 0);
        applyAccessibility(powerIcon, "Toggle State");
        powerIcon.setOnClickListener(v -> viewModel.toggleZoneActiveState(name, isZoneActive));
        int pIconSize = UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
        nameRow.addView(powerIcon, new LinearLayout.LayoutParams(pIconSize, pIconSize));

        TextView tvName = new TextView(this);
        tvName.setText(name.toUpperCase());
        tvName.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvName, "TEXT_TITLE");
        nameRow.addView(tvName);
        text.addView(nameRow);

        TextView tvDetail = new TextView(this);
        tvDetail.setText("📍 " + address + "\n🎯 Node: " + assignedName + "\n⭕ Radius: " + Math.round(radius) + "m");
        tvDetail.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvDetail, "TEXT_SUB");
        tvDetail.setPadding(0, scale(15), 0, scale(15));
        text.addView(tvDetail);
        item.addView(text);

        LinearLayout rightPanel = new LinearLayout(this);
        rightPanel.setOrientation(LinearLayout.VERTICAL);
        rightPanel.setGravity(Gravity.CENTER);

        ImageView bellIcon = new ImageView(this);
        bellIcon.setImageResource(android.R.drawable.ic_popup_reminder);
        bellIcon.setColorFilter(alertsEnabled ? Color.parseColor("#4CAF50") : Color.GRAY);
        int bp = scale(30);
        int bpy = scale(15);
        bellIcon.setPadding(bp, bpy, bp, bpy);
        applyAccessibility(bellIcon, "Toggle Alert");
        if (isZoneActive) bellIcon.setOnClickListener(v -> viewModel.toggleBellAlertState(name, alertsEnabled));
        rightPanel.addView(bellIcon);

        LinearLayout statusLayout = new LinearLayout(this);
        statusLayout.setGravity(Gravity.CENTER);
        TextView tick = new TextView(this); tick.setText("✅"); tick.setAlpha(0.3f); tick.setPadding(0, scale(10), scale(15), scale(10));
        TextView cross = new TextView(this); cross.setText("❌"); cross.setAlpha(0.3f); cross.setPadding(scale(15), scale(10), 0, scale(10));
        statusLayout.addView(tick); statusLayout.addView(cross);
        rightPanel.addView(statusLayout);

        ImageView historyIcon = new ImageView(this);
        historyIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
        historyIcon.setColorFilter(Color.parseColor("#FF9800"));
        historyIcon.setPadding(bp, bpy, bp, bpy);
        applyAccessibility(historyIcon, "View History");
        if (isZoneActive) historyIcon.setOnClickListener(v -> viewModel.fetchGeofenceHistory(name));
        rightPanel.addView(historyIcon);

        ImageView delete = new ImageView(this);
        delete.setImageResource(android.R.drawable.ic_menu_delete);
        delete.setColorFilter(Color.parseColor("#F44336"));
        delete.setPadding(bp, bpy, bp, bpy);
        applyAccessibility(delete, "Delete Zone");
        delete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this).setTitle("ERASE PERIMETER?")
                .setPositiveButton("ERASE", (d, w) -> { geofenceHelper.removeGeofence(name); viewModel.deleteCloudSafeguardZone(name); })
                .setNegativeButton("Abort", null).show();
        });
        rightPanel.addView(delete);
        
        item.addView(rightPanel, new LinearLayout.LayoutParams(scale(150), ViewGroup.LayoutParams.MATCH_PARENT));
        activeBoundariesContainer.addView(item);
    }

    private View createSectionHeader(String t) {
        TextView h = new TextView(this);
        h.setText(t);
        h.setTextColor(getGradientStart());
        UiArchitect.applyText(this, h, "TEXT_LABEL");
        int p = getResponsivePaddingQuota();
        h.setPadding(p / 2, p, 0, p);
        return h;
    }

    @Override
    protected void setupLogic() {
        geofenceHelper = new GeofenceHelper(this);
        viewModel = new ViewModelProvider(this).get(GeofenceViewModel.class);
        viewModel.getActiveZones().observe(this, renderedGeofences -> {
            if (activeBoundariesContainer != null) activeBoundariesContainer.removeAllViews();
            for (RenderedGeofence zone : renderedGeofences) {
                if (zone.isZoneActive()) geofenceHelper.addGeofence(zone.getName() + "___" + zone.getAssignedName(), zone.getLat(), zone.getLng(), (float)zone.getRadius()); 
                else geofenceHelper.removeGeofence(zone.getName() + "___" + zone.getAssignedName());
                addLocalGeofenceTile(zone.getName(), zone.getAddress(), zone.getAssignedName(), zone.getLat(), zone.getLng(), zone.getRadius(), zone.getAlertsEnabled(), zone.isZoneActive());
            }
        });
        viewModel.getDraftCoordinateLocation().observe(this, latLng -> { if (latLng != null) renderMapDraftRings(latLng); else if (btnConfirmLocation != null) btnConfirmLocation.setVisibility(View.GONE); });
        viewModel.getUiToastEvent().observe(this, e -> { String m = e.getContentIfNotHandled(); if (m != null) Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); });
        viewModel.getCameraMoveEvent().observe(this, e -> { LatLng t = e.getContentIfNotHandled(); if (t != null && googleMap != null) googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(t, 16f)); });
        viewModel.getCloseMapEvent().observe(this, e -> { Boolean a = e.getContentIfNotHandled(); if (a != null && a) closeMapEditorState(); });
        viewModel.getHistoryDialogEvent().observe(this, e -> {
            String[] logs = e.getContentIfNotHandled();
            if (logs != null) {
                ScrollView sv = new ScrollView(this);
                LinearLayout c = new LinearLayout(this);
                c.setOrientation(LinearLayout.VERTICAL);
                int q = getResponsivePaddingQuota();
                c.setPadding(q, q, q, q);
                for (String raw : logs) {
                    LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(q, q, q, q); UiArchitect.applyCardWithOverride(this, card, "CARD_STANDARD", 1.0f);
                    TextView t = new TextView(this); t.setText(raw); t.setTextColor(getTextColor()); UiArchitect.applyText(this, t, "TEXT_BODY"); card.addView(t);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, scale(20)); c.addView(card, lp);
                }
                sv.addView(c);
                new android.app.AlertDialog.Builder(this).setTitle("ZONE AUDIT LEDGER").setView(sv).setPositiveButton("Close", null).show();
            }
        });
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
}
