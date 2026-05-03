package com.jdroidx.jfindme.ui.map;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.Interpolator;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;
import com.jdroidx.jfindme.utils.AvatarLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ≡ƒ¢░∩╕Å LIVE MAP DASHBOARD (L7 Enterprise Architect)
 * Objective: Centralized Tactical Command Center for real-time target monitoring.
 * Features: High-fidelity markers, glassmorphic status tiles, and proximity analytics.
 */
public class LiveMapDashboardActivity extends BaseActivityJava implements OnMapReadyCallback {

    private MapViewModel viewModel;
    private GoogleMap googleMap;
    private LinearLayout targetListContainer;
    private ScrollView targetScrollVertical;
    private TextView tvActiveCount;
    
    private final Map<String, Marker> markerMap = new HashMap<>();
    private String selectedTargetId = null;
    
    private static final int ID_MAP_FRAGMENT = 10002;
    private static final String TAG = "LiveMapDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SystemLogger.i(TAG, "LiveMapDashboard [onCreate] executing...");
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(MapViewModel.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // 1. Premium Tactical Header
        root.addView(createTacticalHeader());

        // 2. Main Dashboard Area: Forensic Map with Floating Vertical Matrix
        FrameLayout mainArea = new FrameLayout(this);
        mainArea.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // ≡ƒÜÇ QA Recalibration: 20px Baseline Quota
        int q = scale(20); 

        // Full Map Engine
        FrameLayout mapContainer = new FrameLayout(this);
        mapContainer.setId(ID_MAP_FRAGMENT);
        mainArea.addView(mapContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Floating Target Matrix: Left Side (50% Scaled Vertical)
        targetScrollVertical = new ScrollView(this);
        targetScrollVertical.setClipToPadding(false);
        targetScrollVertical.setVerticalScrollBarEnabled(false);
        targetScrollVertical.setPadding(0, q, 0, q);
        
        targetListContainer = new LinearLayout(this);
        targetListContainer.setOrientation(LinearLayout.VERTICAL); // VERTICAL ORDER
        targetScrollVertical.addView(targetListContainer);

        FrameLayout.LayoutParams scrollLp = new FrameLayout.LayoutParams(scale(220), ViewGroup.LayoutParams.MATCH_PARENT); // 50% approx width shrink
        scrollLp.gravity = Gravity.START;
        scrollLp.setMargins(q, q, 0, scale(180)); // Padding from header and navbar
        mainArea.addView(targetScrollVertical, scrollLp);

        root.addView(mainArea);

        // 3. Global Bottom Navigation
        root.addView(UiArchitect.createBottomNav(this, new String[]{"NAV_NOTICE", "NAV_DEVICE", "NAV_ME"}, "NAV_DEVICE"));

        return root;
    }

    private View createTacticalHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        applyGlobalHeaderGeometry(header);
        
        // Use 15% glassmorphism override for the header
        header.setBackground(com.jdroidx.jfindme.ui.base.ThemeManager.createGlassDrawable(getGradientStart(), 0.15f, 0));

        // Left Branding
        ImageView logo = new ImageView(this);
        logo.setImageResource(android.R.drawable.ic_menu_compass);
        logo.setColorFilter(getGradientOverlayText());
        int lSize = scale(120);
        header.addView(logo, new LinearLayout.LayoutParams(lSize, lSize));

        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.VERTICAL);
        titleLayout.setPadding(scale(30), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("TACTICAL DASHBOARD");
        title.setTextColor(getGradientOverlayText());
        title.setTypeface(null, Typeface.BOLD);
        title.setTextSize(14f);
        titleLayout.addView(title);

        TextView tvStatusPill = new TextView(this);
        tvStatusPill.setText("NODE SYNC ACTIVE");
        tvStatusPill.setTextColor(Color.parseColor("#00E676"));
        tvStatusPill.setTextSize(9f);
        titleLayout.addView(tvStatusPill);

        header.addView(titleLayout, new LinearLayout.LayoutParams(0, -2, 1.0f));

        // Right Metric Pill
        LinearLayout metricPill = new LinearLayout(this);
        metricPill.setOrientation(LinearLayout.VERTICAL);
        metricPill.setGravity(Gravity.CENTER);
        metricPill.setPadding(scale(30), scale(10), scale(30), scale(10));
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor("#99000000"));
        gd.setCornerRadius(30f);
        metricPill.setBackground(gd);

        TextView lbl = new TextView(this);
        lbl.setText("ACTIVE");
        lbl.setTextColor(Color.LTGRAY);
        lbl.setTextSize(7f);
        metricPill.addView(lbl);

        tvActiveCount = new TextView(this);
        tvActiveCount.setText("0");
        tvActiveCount.setTextColor(Color.WHITE);
        tvActiveCount.setTypeface(null, Typeface.BOLD);
        tvActiveCount.setTextSize(14f);
        metricPill.addView(tvActiveCount);

        header.addView(metricPill);

        return header;
    }

    @Override
    protected void setupLogic() {
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(ID_MAP_FRAGMENT, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        viewModel.getUserLocations().observe(this, this::updateDashboard);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, com.jdroidx.jfindme.R.raw.map_style_dark));
            if (!success) SystemLogger.e(TAG, "Style parsing failed.");
        } catch (Exception e) {
            SystemLogger.e(TAG, "Can't find style. Error: ", e);
        }
        
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 2.0f));
    }

    private void updateDashboard(List<UserLocation> locations) {
        if (locations == null) return;
        
        tvActiveCount.setText(String.valueOf(locations.size()));
        targetListContainer.removeAllViews();
        
        for (UserLocation loc : locations) {
            targetListContainer.addView(createTargetTile(loc));
            updateMapMarker(loc);
        }
        
        // Cleanup stale markers
        markerMap.entrySet().removeIf(entry -> {
            boolean exists = false;
            for (UserLocation l : locations) if (l.getResolvedUserId().equals(entry.getKey())) exists = true;
            if (!exists) entry.getValue().remove();
            return !exists;
        });
    }

    private View createTargetTile(UserLocation loc) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        int p = scale(15); // 50% Reduction
        tile.setPadding(p, p, p, p);
        
        UiArchitect.applyCard(this, tile, "CARD_STANDARD");
        
        boolean isSelected = loc.getResolvedUserId().equals(selectedTargetId);
        if (isSelected) {
            tile.setBackground(com.jdroidx.jfindme.ui.base.ThemeManager.createGlassDrawable(getGradientStart(), 0.3f, 25)); // 50% Radius reduction
        } else {
            tile.setBackground(com.jdroidx.jfindme.ui.base.ThemeManager.createGlassDrawable(getCardColor(), 0.15f, 25));
        }

        ImageView avatar = new ImageView(this);
        AvatarLoader.bindAvatar(this, avatar, loc.getAvatar_id());
        int aSize = scale(70); // 50% Reduction
        tile.addView(avatar, new LinearLayout.LayoutParams(aSize, aSize));

        TextView name = new TextView(this);
        name.setText(loc.getUserName().substring(0, Math.min(loc.getUserName().length(), 8)).toUpperCase());
        name.setTextColor(getTextColor());
        name.setTextSize(8f); // 50% approx
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, scale(8), 0, 0);
        tile.addView(name);

        TextView battery = new TextView(this);
        battery.setText(loc.getBatteryLevel() + "%");
        battery.setTextColor(loc.getBatteryLevel() < 20 ? Color.RED : Color.parseColor("#00E676"));
        battery.setTextSize(6f);
        tile.addView(battery);

        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setOnClickListener(v -> focusOnTarget(loc));
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(scale(160), ViewGroup.LayoutParams.WRAP_CONTENT); // 50% Width
        lp.setMargins(0, 0, 0, scale(20)); // Margin-bottom for vertical stack
        tile.setLayoutParams(lp);
        
        return tile;
    }

    private void updateMapMarker(UserLocation loc) {
        if (googleMap == null) return;

        LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
        Marker marker = markerMap.get(loc.getUserId());

        if (marker == null) {
            marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(loc.getUserName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            markerMap.put(loc.getResolvedUserId(), marker);
        } else {
            animateMarker(marker, pos);
        }
    }

    private void animateMarker(final Marker marker, final LatLng toPosition) {
        final LatLng startPosition = marker.getPosition();
        final Interpolator interpolator = new LinearInterpolator();

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(1200);
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            double lng = v * toPosition.longitude + (1 - v) * startPosition.longitude;
            double lat = v * toPosition.latitude + (1 - v) * startPosition.latitude;
            marker.setPosition(new LatLng(lat, lng));
        });
        valueAnimator.start();
    }

    private void focusOnTarget(UserLocation loc) {
        selectedTargetId = loc.getResolvedUserId();
        if (googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 16.5f));
        }
        viewModel.triggerRefresh(); // Direct UI refresh
        updateDashboard(viewModel.getUserLocations().getValue());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}
