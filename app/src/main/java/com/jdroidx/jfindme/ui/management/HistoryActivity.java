package com.jdroidx.jfindme.ui.management;

import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.data.repository.LocationRepository;
import com.jdroidx.jfindme.engine.data.SpatialProcessor;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main -> UI Engine -> ManagementCluster -> HistoryActivity (L4)
 * RESTORATION BASELINE: April 1 23:00 (Structural Parity)
 * Rules: 100% Java Programmatic, 70:30 Split in Landscape.
 */
public class HistoryActivity extends BaseActivityJava implements OnMapReadyCallback {

    private final String TAG = "HistoryActivity";
    private HistoryViewModel viewModel;
    private GoogleMap googleMap;
    private Marker replayAvatar;
    private Polyline replayPath;
    private List<UserLocation> historyPoints = new ArrayList<>();
    private static final int ID_MAP_FRAGMENT = 2001;
    
    private TextView tvDistance;
    private TextView tvStatus;
    private TextView tvTrackLabel;
    private TextView tvWaypointsCount;
    private List<View> timeButtons = new java.util.ArrayList<>();
    private static final long[] DURATIONS = {
        4 * 60 * 60 * 1000L,
        12 * 60 * 60 * 1000L,
        24 * 60 * 60 * 1000L,
        7 * 24 * 60 * 60 * 1000L
    };
    private LinearLayout logsContainer;
    private ScrollView mainScrollView; 
    
    private boolean isReplaying = false;
    private String currentTargetUid;
    private String currentTargetName;
    private int selectedTimeIndex = 0;
    private List<Map<String, Object>> currentSessions = new ArrayList<>();
    
    // 🧱 QA Guard: Registry of already scavenged forensic windows in this app session
    private java.util.Set<String> warmedVaults = new java.util.HashSet<>();
    private boolean minLoadTimerExpired = true;

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // --- HEADER ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        // Header Title Row (with help icon)
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView tvTitle = new TextView(this);
        tvTitle.setText("LOCATION LOGS");
        tvTitle.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, tvTitle, "TEXT_HEADER");
        int hTop = getResponsiveHeaderTopQuota();
        tvTitle.setPadding(0, hTop, 0, 0);
        titleRow.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        header.addView(titleRow);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Historical GPS Breadcrumbs & Action Replay");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        subtitle.setPadding(0, scale(10), 0, 0);
        header.addView(subtitle);
        
        root.addView(header);

        // --- CONTENT ---
        mainScrollView = new ScrollView(this);
        mainScrollView.setClipToPadding(false);
        mainScrollView.setClipChildren(false);
        
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipToPadding(false);
        content.setClipChildren(false);
        int hPad = getResponsivePaddingQuota();
        int hSideGap = com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.getUniversalGap(com.jdroidx.jfindme.ui.base.UiArchitect.getStage(this));
        content.setPadding(hSideGap, hSideGap, hSideGap, hPad * 5);

        // --- LAYER 0: FORENSIC STORAGE RECAP (L7 Shared Component) ---
        // Final Diagnostic Pill (Floating Right Action)
        LinearLayout pillBox = new LinearLayout(this);
        pillBox.setOrientation(LinearLayout.HORIZONTAL);
        pillBox.setGravity(Gravity.CENTER);
        pillBox.setBackground(getSolidDrawable(Color.argb(15, 128, 128, 128), 40));
        int vPIll = scale(8);
        int hPIll = scale(30);
        pillBox.setPadding(hPIll, vPIll, hPIll, vPIll);
        
        EditText etSeven = new EditText(this);
        etSeven.setText("7");
        etSeven.setTextColor(getGradientStart());
        etSeven.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, etSeven, "TEXT_LABEL"); // Re-aligned to Gallery Standard
        etSeven.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etSeven.setBackground(null);
        etSeven.setGravity(Gravity.CENTER);
        etSeven.setMinWidth(scale(50));
        etSeven.setFocusable(true);
        etSeven.setFocusableInTouchMode(true);
        etSeven.setCursorVisible(true);
        pillBox.addView(etSeven);
        
        TextView tvDays = new TextView(this);
        tvDays.setText(" DAYS");
        tvDays.setTextColor(Color.GRAY);
        UiArchitect.applyText(this, tvDays, "TEXT_MINI");
        pillBox.addView(tvDays);

        // 🚀 QA Fix: Global UI Architecture Sync! Use the official ListItem engine.
        View storageCap = com.jdroidx.jfindme.ui.base.ListItemTable.createRow(
            this, 
            "ROW_STORAGE", 
            "MAXIMUM LOCAL HISTORY SAVED", 
            "Forensic Storage Override", 
            pillBox
        );
        
        // 🧱 L7 Aesthetic Sync: Disable row-wide click to prevent 'Double-Tap' focus lag on nested EditText
        storageCap.setFocusable(false);
        storageCap.setClickable(false);
        
        LinearLayout.LayoutParams capLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        capLp.setMargins(0, 0, 0, hSideGap);
        content.addView(storageCap, capLp);

        // --- LAYER 1: MAP ENGINE (With Embedded Distance) ---
        LinearLayout mapCard = new LinearLayout(this);
        mapCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, mapCard, "CARD_STANDARD", 1.2f);
        
        FrameLayout mapStack = new FrameLayout(this);
        
        FrameLayout mapContainer = new FrameLayout(this);
        mapContainer.setId(ID_MAP_FRAGMENT);
        
        // Map Interaction Interceptor
        FrameLayout gestureMembrane = new FrameLayout(this);
        gestureMembrane.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) mainScrollView.requestDisallowInterceptTouchEvent(true);
            else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) mainScrollView.requestDisallowInterceptTouchEvent(false);
            return false;
        });
        mapContainer.addView(gestureMembrane, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int mapHeight = scale(600);
        mapStack.addView(mapContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mapHeight));
        
        // Embedded Distance Footer (L7 Aesthetic - White Pill)
        LinearLayout distFooterWrap = new LinearLayout(this);
        distFooterWrap.setGravity(Gravity.CENTER);
        distFooterWrap.setPadding(0, 0, 0, scale(20));
        
        LinearLayout distFooter = new LinearLayout(this);
        distFooter.setOrientation(LinearLayout.VERTICAL);
        // Using a white capsule background with shadow
        distFooter.setBackground(getSolidDrawable(Color.WHITE, 60));
        int dfPadHorizontal = scale(60);
        int dfPadVertical = scale(20);
        distFooter.setPadding(dfPadHorizontal, dfPadVertical, dfPadHorizontal, dfPadVertical);
        
        tvDistance = new TextView(this);
        tvDistance.setTextColor(Color.BLACK);
        UiArchitect.applyText(this, tvDistance, "TEXT_BODY");
        tvDistance.setText("TOTAL DISTANCE: 0 m");
        tvDistance.setTypeface(null, Typeface.BOLD);
        distFooter.addView(tvDistance);
        
        distFooterWrap.addView(distFooter);
        
        FrameLayout.LayoutParams distLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        distLp.gravity = Gravity.BOTTOM;
        mapStack.addView(distFooterWrap, distLp);
        
        mapCard.addView(mapStack);
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, hPad * 2);
        content.addView(mapCard, cardLp);

        // --- LAYER 2: UNIFIED REPLAY ENGINE CONTROLS ---
        content.addView(createSectionHeader("REPLAY ENGINE CONTROLS"));
        LinearLayout replayCard = new LinearLayout(this);
        replayCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, replayCard, "CARD_STANDARD", 1.0f);
        
        int rPad = getResponsivePaddingQuota();
        replayCard.setPadding(rPad, rPad, rPad, rPad);

        tvTrackLabel = new TextView(this);
        updateTrackLabel();
        tvTrackLabel.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvTrackLabel, "TEXT_MINI");
        tvTrackLabel.setTypeface(null, Typeface.BOLD);
        tvTrackLabel.setPadding(0, 0, 0, scale(25));
        replayCard.addView(tvTrackLabel);

        LinearLayout targetSelectorWrap = new LinearLayout(this);
        targetSelectorWrap.setOrientation(LinearLayout.HORIZONTAL);
        targetSelectorWrap.setGravity(Gravity.CENTER_VERTICAL);
        
        // 🧱 L7 Aesthetic Sync: Upgraded to High-Elevation Tile with Shadow
        UiArchitect.applyCardWithOverride(this, targetSelectorWrap, "CARD_STANDARD", 1.2f);
        targetSelectorWrap.setOnClickListener(v -> showTargetSelector());
        
        int tsPad = scale(25);
        targetSelectorWrap.setPadding(tsPad, tsPad, tsPad, tsPad);
        
        ImageView targetIcon = new ImageView(this);
        targetIcon.setImageResource(android.R.drawable.ic_menu_search);
        targetIcon.setColorFilter(Color.parseColor("#FF5722"));
        targetSelectorWrap.addView(targetIcon, new LinearLayout.LayoutParams(scale(55), scale(55)));
        
        TextView tvSelect = new TextView(this);
        tvSelect.setText("SELECT TRACKING TARGET");
        tvSelect.setTextColor(Color.BLACK);
        tvSelect.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvSelect, "TEXT_TITLE");
        tvSelect.setPadding(scale(30), 0, 0, 0);
        targetSelectorWrap.addView(tvSelect);
        
        applyAccessibility(targetSelectorWrap, "SELECT TRACKING TARGET");
        replayCard.addView(targetSelectorWrap);

        // Time Selection Grid (2x2)
        android.widget.GridLayout timeGrid = new android.widget.GridLayout(this);
        timeGrid.setColumnCount(2);
        timeGrid.setPadding(0, scale(30), 0, scale(30));

        timeButtons.clear();
        View btn4h = createTimeTile("4 HRS", android.R.drawable.ic_menu_recent_history, 0);
        View btn12h = createTimeTile("12 HRS", android.R.drawable.ic_menu_recent_history, 1);
        View btn24h = createTimeTile("24 HRS", android.R.drawable.ic_menu_recent_history, 2);
        View btn7d = createTimeTile("7 DAYS", android.R.drawable.ic_menu_today, 3);

        timeGrid.addView(wrapInGrid(btn4h));
        timeGrid.addView(wrapInGrid(btn12h));
        timeGrid.addView(wrapInGrid(btn24h));
        timeGrid.addView(wrapInGrid(btn7d));
        replayCard.addView(timeGrid);

        // Play/Stop Row
        LinearLayout replayButtonsRow = new LinearLayout(this);
        replayButtonsRow.setOrientation(LinearLayout.HORIZONTAL);
        
        int mGrid = scale(10);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnLp.setMargins(mGrid, mGrid, mGrid, mGrid);
        
        View playBtn = createReplayButton("START\nREPLAY", android.R.drawable.ic_media_play);
        playBtn.setOnClickListener(v -> startPathReplay());
        
        View stopBtn = createReplayButton("STOP\nREPLAY", android.R.drawable.ic_media_pause);
        stopBtn.setOnClickListener(v -> { isReplaying = false; Toast.makeText(this, "Replay Terminated", Toast.LENGTH_SHORT).show(); });

        replayButtonsRow.addView(wrapInRow(playBtn), btnLp);
        replayButtonsRow.addView(wrapInRow(stopBtn), btnLp);
        replayCard.addView(replayButtonsRow);

        tvWaypointsCount = new TextView(this);
        tvWaypointsCount.setText("0 WAYPOINTS LOADED SECURELY");
        tvWaypointsCount.setTextColor(getSecondaryTextColor());
        tvWaypointsCount.setGravity(Gravity.CENTER);
        UiArchitect.applyText(this, tvWaypointsCount, "TEXT_MINI");
        tvWaypointsCount.setPadding(0, scale(35), 0, 0);
        replayCard.addView(tvWaypointsCount);
        
        content.addView(replayCard, cardLp);

        // --- LAYER 3: DETAILED LOGS ---
        content.addView(createSectionHeader("DETAILED LOCATION LOGS"));
        logsContainer = new LinearLayout(this);
        logsContainer.setOrientation(LinearLayout.VERTICAL);
        // 🚀 QA Fix: Force Clipping Disable to enable floating shadows on all data segments
        logsContainer.setClipToPadding(false);
        logsContainer.setClipChildren(false);
        content.addView(logsContainer);

        mainScrollView.addView(content);
        root.addView(mainScrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return root;
    }

    private View createSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tv, "TEXT_TITLE");
        tv.setTypeface(null, Typeface.BOLD);
        int sPad = getResponsivePaddingQuota();
        tv.setPadding(sPad / 2, scale(20), 0, sPad);
        return tv;
    }

    private View createReplayButton(String text, int resId) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        
        // 🧱 L7 Aesthetic Sync: Upgraded to High-Elevation Tile with Shadow
        UiArchitect.applyCardWithOverride(this, btn, "CARD_STANDARD", 1.2f);
        // 🧩 Note: We rely on applyCard's themed background to preserve outline provider
        
        int p = scale(25);
        btn.setPadding(p, p, p, p);
        
        ImageView icon = new ImageView(this);
        icon.setImageResource(resId);
        icon.setColorFilter(android.graphics.Color.parseColor("#FF5722")); // Orange-Red 
        int s = scale(55);
        btn.addView(icon, new LinearLayout.LayoutParams(s, s));
        
        TextView tv = new TextView(this);
        tv.setText(text.toUpperCase());
        tv.setTextColor(android.graphics.Color.BLACK); 
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, scaleText(26));
        tv.setPadding(scale(20), 0, 0, 0);
        btn.addView(tv);
        
        applyAccessibility(btn, text);
        return btn;
    }

    private View createTimeTile(String text, int iconRes, int index) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        
        // 🧱 L7 Aesthetic Sync: Upgraded to High-Elevation Tile with Shadow
        UiArchitect.applyCardWithOverride(this, tile, "CARD_STANDARD", 1.0f);
        // 🧩 Note: Preserving card geometry base for shadow fidelity

        int p = scale(25);
        tile.setPadding(p, p, p, p);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart());
        tile.addView(icon, new LinearLayout.LayoutParams(scale(55), scale(55)));

        TextView tv = new TextView(this);
        tv.setText(text);
        UiArchitect.applyText(this, tv, "TEXT_MINI");
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(getTextColor());
        tv.setPadding(scale(15), 0, 0, 0);
        tile.addView(tv);

        tile.setOnClickListener(v -> {
            selectedTimeIndex = index;
            loadHistoryData(DURATIONS[index]);
            updateTimeTileStyles(index);
        });

        applyAccessibility(tile, text);
        timeButtons.add(tile);
        return tile;
    }

    private void updateTimeTileStyles(int selectedIndex) {
        for (int i = 0; i < timeButtons.size(); i++) {
            boolean selected = (i == selectedIndex);
            View v = timeButtons.get(i);
            if (v instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) v;
                layout.setBackground(selected ? getSolidDrawable(Color.argb(40, 255, 87, 34), 30) : null);
                if (!selected) UiArchitect.applyCard(this, layout, "CARD_STANDARD");
                

                for (int j = 0; j < layout.getChildCount(); j++) {
                    View child = layout.getChildAt(j);
                    if (child instanceof ImageView) {
                        ((ImageView) child).setAlpha(selected ? 1.0f : 0.7f);
                    } else if (child instanceof TextView) {
                        ((TextView) child).setTextColor(selected ? Color.BLACK : getTextColor());
                    }
                }
            }
        }
    }

    private View wrapInGrid(View v) {
        android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
        lp.width = 0;
        lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
        int m = scale(10);
        lp.setMargins(m, m, m, m);
        v.setLayoutParams(lp);
        return v;
    }

    private View wrapInRow(View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = scale(10);
        lp.setMargins(m, m, m, m);
        v.setLayoutParams(lp);
        return v;
    }

    private void loadHistoryData(long durationMs) {
        if (currentTargetUid == null || currentTargetUid.equals(FirebaseAuth.getInstance().getUid())) {
            // 🚀 QA Rule: Battery Guard (Rule 5)
            // Block self-tracking fetch. 
            if (tvStatus != null) tvStatus.setText("SELECT A TARGET TO START FETCH");
            return;
        }
        
        // 🚀 QA Rule: Redundancy Guard (The 'Warm' Registry)
        String lockKey = currentTargetUid + "_" + durationMs;
        if (warmedVaults.contains(lockKey)) {
            viewModel.loadHistorySessions(this, currentTargetUid, durationMs);
            return;
        }

        // 🦋 QA Fix: The Guardian Angel Loading Matrix (3s Mandatory Guard)
        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Retrieving the History Data");
        minLoadTimerExpired = false;

        if (viewModel != null) {
            viewModel.loadHistoryData(currentTargetUid, durationMs);
            warmedVaults.add(lockKey);
        }

        new android.os.Handler().postDelayed(() -> {
             minLoadTimerExpired = true;
             if (viewModel.isLoading().getValue() != Boolean.TRUE) {
                 com.jdroidx.jfindme.utils.AngelLoader.hide(this);
             }
        }, 3000);
    }


    private void calculateAnalytics(List<UserLocation> points) {
        double dist = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            dist += SpatialProcessor.getDistance(
                points.get(i).getLatitude(), points.get(i).getLongitude(),
                points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude()
            );
        }
        if (tvDistance != null) {
            String text = dist > 1000 ? 
                String.format("TOTAL DISTANCE: %.2f km", dist / 1000) : 
                String.format("TOTAL DISTANCE: %.0f m", dist);
            tvDistance.setText(text);
        }
    }

    @Override
    protected void onUiRebuilt() {
        updateTrackLabel();
        updateTimeTileStyles(selectedTimeIndex);
        updateHistoryTable(currentSessions);
        
        if (!historyPoints.isEmpty()) {
            calculateAnalytics(historyPoints);
            tvWaypointsCount.setText(historyPoints.size() + " WAYPOINTS LOADED SECURELY");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_FRAGMENT);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(ID_MAP_FRAGMENT, mapFragment).commit();
            mapFragment.getMapAsync(this);
        }
    }

    private void drawStaticPath(List<UserLocation> points) {
        if (googleMap == null) return;
        googleMap.clear();
        
        int accent = getGradientStart();
        int semiTransparentColor = Color.argb(153, Color.red(accent), Color.green(accent), Color.blue(accent));
        
        PolylineOptions options = new PolylineOptions()
            .color(semiTransparentColor)
            .width(12);
            
        for (UserLocation p : points) {
            options.add(new LatLng(p.getLatitude(), p.getLongitude()));
        }
        googleMap.addPolyline(options);
        
        if (!points.isEmpty()) {
            LatLng first = new LatLng(points.get(0).getLatitude(), points.get(0).getLongitude());
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 15f));
        }
    }

    private void startPathReplay() {
        if (historyPoints.size() < 2 || googleMap == null) return;
        
        isReplaying = true;
        if (replayAvatar != null) replayAvatar.remove();
        
        UserLocation first = historyPoints.get(0);
        replayAvatar = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(first.getLatitude(), first.getLongitude()))
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                
        updateReplayStatusAddress(first.getLatitude(), first.getLongitude());
        animateReplay(0);
    }

    private void updateReplayStatusAddress(double lat, double lng) {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
            geocoder.getFromLocation(lat, lng, 1, new android.location.Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(List<android.location.Address> addresses) {
                    if (!addresses.isEmpty()) {
                        String addr = addresses.get(0).getAddressLine(0);
                        runOnUiThread(() -> {
                            if (tvStatus != null) tvStatus.setText("TRACING: " + addr);
                            if (replayAvatar != null) {
                                replayAvatar.setTitle("LIVE TRACE");
                                replayAvatar.setSnippet(addr);
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            tvStatus.setText(String.format("TRACING: %.5f, %.5f", lat, lng));
        }
    }

    private void animateReplay(final int index) {
        if (!isReplaying || index >= historyPoints.size() - 1) {
            if (isReplaying) Toast.makeText(this, "Replay Completed", Toast.LENGTH_SHORT).show();
            return;
        }

        final UserLocation start = historyPoints.get(index);
        final UserLocation end = historyPoints.get(index + 1);

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(500); 
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            double lat = v * end.getLatitude() + (1 - v) * start.getLatitude();
            double lng = v * end.getLongitude() + (1 - v) * start.getLongitude();
            LatLng current = new LatLng(lat, lng);
            if (replayAvatar != null) replayAvatar.setPosition(current);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(current));
        });
        
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (isReplaying) {
                    updateReplayStatusAddress(end.getLatitude(), end.getLongitude());
                    animateReplay(index + 1);
                }
            }
        });
        animator.start();
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        viewModel.init(this);
        
        viewModel.getHistoryPoints().observe(this, points -> {
            if (points == null || points.isEmpty()) {
                Toast.makeText(this, "No forensic data segments located.", Toast.LENGTH_SHORT).show();
                if (tvWaypointsCount != null) tvWaypointsCount.setText("0 WAYPOINTS LOADED SECURELY");
            } else {
                historyPoints = points;
                drawStaticPath(points);
                calculateAnalytics(points);
                if (tvWaypointsCount != null) tvWaypointsCount.setText(points.size() + " WAYPOINTS LOADED SECURELY");
            }
        });

        viewModel.getHistorySessions().observe(this, sessions -> {
            if (sessions != null) {
                currentSessions = sessions;
                updateHistoryTable(sessions);
            }
        });

        viewModel.isLoading().observe(this, loading -> {
            if (tvStatus != null) {
                tvStatus.setText(loading ? "SYNCING CLOUD DATA..." : "READY FOR SYNC");
            }
            
            // 🦋 QA Fix: Post-Commit Butterfly Dismissal (Dynamic Guard)
            // Rule: Hide only if the 3S Mandatory Timer AND the Cloud Sync are both finished.
            if (!loading && minLoadTimerExpired) {
                new android.os.Handler().postDelayed(() -> {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                }, 800);
            }
        });

        viewModel.fetchTrackingTargets(this);
        
        // --- 🚀 Intent Linkage Protocol (V40 Standard) ---
        currentTargetUid = getIntent().getStringExtra("TARGET_UID");
        currentTargetName = getIntent().getStringExtra("TARGET_NAME");
        
        if (currentTargetUid != null && !currentTargetUid.equals(FirebaseAuth.getInstance().getUid())) {
            // Initial Waterfall Trigger (Cloud Sync)
            loadHistoryData(DURATIONS[0]); 
            updateTimeTileStyles(0);
        } else {
            updateTrackLabel();
            if (tvStatus != null) tvStatus.setText("READY FOR SYNC • SELECT TARGET");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_FRAGMENT);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(ID_MAP_FRAGMENT, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }


    private void updateTrackLabel() {
        if (tvTrackLabel != null) {
            String myUid = FirebaseAuth.getInstance().getUid();
            boolean isSelf = (currentTargetUid == null || currentTargetUid.equals(myUid));
            String name = isSelf ? "NO SELECTION" : (currentTargetName != null ? currentTargetName.toUpperCase() : "UNKNOWN PLAYER");
            tvTrackLabel.setText("CURRENT TRACK: " + name);
            tvTrackLabel.setAlpha(isSelf ? 0.6f : 1.0f);
        }
    }

    private void showTargetSelector() {
        List<HistoryViewModel.TrackingTarget> targets = viewModel.getTrackingTargets().getValue();
        
        if (targets == null || targets.isEmpty()) {
            Toast.makeText(this, "Refilling Target Matrix...", Toast.LENGTH_SHORT).show();
            viewModel.fetchTrackingTargets(this);
            return;
        }

        String[] names = new String[targets.size()];
        String[] uids = new String[targets.size()];
        
        for (int i = 0; i < targets.size(); i++) {
            names[i] = targets.get(i).getName();
            uids[i] = targets.get(i).getUid();
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("Select Tracking Target")
            .setItems(names, (dialog, index) -> {
                currentTargetUid = uids[index];
                currentTargetName = names[index];
                updateTrackLabel();
                Toast.makeText(this, "Target Synchronized: " + currentTargetName, Toast.LENGTH_SHORT).show();
            })
            .show();
    }


    private void updateHistoryTable(List<Map<String, Object>> sessions) {
        if (logsContainer == null) return;
        logsContainer.removeAllViews();
        
        if (sessions == null || sessions.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No chronological data sessions found in local DB.");
            emptyTv.setTextColor(getSecondaryTextColor());
            emptyTv.setGravity(Gravity.CENTER);
            UiArchitect.applyText(this, emptyTv, "TEXT_MINI");
            int p = scale(40);
            emptyTv.setPadding(p, p, p, p);
            logsContainer.addView(emptyTv);
            return;
        }

        for (Map<String, Object> session : sessions) {
            logsContainer.addView(createSessionTile(session));
        }
    }

    private View createSessionTile(Map<String, Object> session) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        // 🧱 L7 Aesthetic Sync: Applying High-Contrast shadow to log segments
        UiArchitect.applyCardWithOverride(this, outer, "CARD_STANDARD", 1.2f);
        
        // 🚀 Baseline Synchronization: Margin between tiles set to 45px Universal Gap
        int margin = com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.getUniversalGap(com.jdroidx.jfindme.ui.base.UiArchitect.getStage(this));
        
        LinearLayout.LayoutParams outerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        outerLp.setMargins(0, 0, 0, margin);
        outer.setLayoutParams(outerLp);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        int p = scale(25);
        row1.setPadding(p, p, p, 0); // Reduced bottom padding

        // Session Label (⏱️ Inline Character Logic for 100% Forensic Parity)
        TextView tvLabel = new TextView(this);
        Object pingObj = session.get("ping_count");
        int pings = (pingObj instanceof Integer) ? (Integer) pingObj : 0;
        
        Object fromObj = session.get("from_datetime");
        long from = (fromObj instanceof Long) ? (Long) fromObj : 0L;
        
        Object toObj = session.get("to_datetime");
        long to = (toObj instanceof Long) ? (Long) toObj : 0L;
        
        String dateStr = new java.text.SimpleDateFormat("MMM dd, yyyy | hh:mm:ss a", java.util.Locale.getDefault()).format(new java.util.Date(from));
        String toStr = new java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(new java.util.Date(to));
        
        tvLabel.setText("⏱️ SESSION (" + pings + " PINGS) • " + dateStr + " TO " + toStr);
        tvLabel.setTextColor(android.graphics.Color.parseColor("#A34121")); // Brown-Orange
        tvLabel.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvLabel, "TEXT_MINI");
        // Adjusted padding for the absence of the separate icon
        tvLabel.setPadding(0, 0, 0, 0); 
        row1.addView(tvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Right Target Icon
        ImageView icTarget = new ImageView(this);
        icTarget.setImageResource(android.R.drawable.ic_menu_mylocation);
        icTarget.setColorFilter(android.graphics.Color.parseColor("#FF5722"));
        row1.addView(icTarget, new LinearLayout.LayoutParams(scale(55), scale(55)));

        outer.addView(row1);

        // Address
        TextView tvAddr = new TextView(this);
        tvAddr.setText(session.get("address").toString());
        tvAddr.setTextColor(android.graphics.Color.BLACK); // Bold black as per image
        tvAddr.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvAddr, "TEXT_MINI");
        tvAddr.setPadding(p + scale(65), 0, p, scale(15));
        outer.addView(tvAddr);

        // Stats Row
        LinearLayout rowStats = new LinearLayout(this);
        rowStats.setOrientation(LinearLayout.HORIZONTAL);
        rowStats.setPadding(p + scale(65), 0, p, p);
        
        // 🧱 QA Hardening: Null-Safety for Forensic Stats (Anti-Rebound)
        String speed = session.get("speed") != null ? session.get("speed").toString() : "0";
        String battery = session.get("battery") != null ? session.get("battery").toString() : "--";
        
        rowStats.addView(createMiniStat("🚗", speed + " km/h"));
        rowStats.addView(createMiniStat("🔋", battery + "%"));
        rowStats.addView(createMiniStat("📍", pings + " Pings"));
        
        outer.addView(rowStats);

        outer.setOnClickListener(v -> {
            if (googleMap != null) {
                LatLng pos = new LatLng((double)session.get("lat"), (double)session.get("lng"));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
                
                // --- 🚀 QA Fix: Force Avatar Display on Node Inspection ---
                if (replayAvatar != null) replayAvatar.remove();
                
                String addr = session.get("address").toString();
                replayAvatar = googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title("INSPECTING NODE")
                        .snippet(addr)
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                        
                replayAvatar.showInfoWindow();
                
                if (tvStatus != null) tvStatus.setText("NODE: " + addr);
                Toast.makeText(this, "Inspecting Node: " + pings + " Pings at this Radius", Toast.LENGTH_SHORT).show();
            }
        });

        return outer;
    }

    private View createMiniStat(String icon, String text) {
        TextView tv = new TextView(this);
        tv.setText(icon + " " + text);
        tv.setTextColor(android.graphics.Color.parseColor("#2E7D32")); // Success Green
        tv.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tv, "TEXT_MINI");
        tv.setPadding(0, 0, scale(40), 0);
        return tv;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        UiArchitect.applyMapStyle(this, googleMap);
        
        if (!historyPoints.isEmpty()) {
            drawStaticPath(historyPoints);
            LatLng first = new LatLng(historyPoints.get(0).getLatitude(), historyPoints.get(0).getLongitude());
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 15f));
        } else {
            // 🚀 QA Rule: Contextual Anchor (Owner Location)
            new Thread(() -> {
                com.jdroidx.jfindme.data.local.DatabaseHelper db = new com.jdroidx.jfindme.data.local.DatabaseHelper(this);
                String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                if (myUid != null) {
                    List<Map<String, Object>> logs = db.getHistoryLogsFiltered(myUid, 0L);
                    if (!logs.isEmpty()) {
                        double lat = (double) logs.get(0).get("lat");
                        double lng = (double) logs.get(0).get("lng");
                        runOnUiThread(() -> {
                            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15f));
                        });
                        return;
                    }
                }
                // Fallback to Headquarters if admin location is unknown
                runOnUiThread(() -> {
                    this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(18.5204, 73.8567), 12f));
                });
            }).start();
        }
    }
}
