package com.jdroidx.jfindme.ui.management;

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
import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.data.repository.LocationRepository;
import com.jdroidx.jfindme.engine.data.SpatialProcessor;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Unit;

/**
 * Main -> UI Engine -> ManagementCluster -> HistoryActivity (L4)
 * Rules: 100% Java Programmatic, 70:30 Split in Landscape.
 * Integration: Path Replay Engine (L5).
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
    private TextView tvSyncMode; // 🚀 Online/Offline Indicator
    private TextView tvLastUpdated; // 🚀 Forensic Footer
    private TextView targetLabel;
    private LinearLayout locationLogsContainer;
    
    // 🚀 QA Fix: Master References for Visual Selection State
    private View btn4h, btn12h, btn24h, btn7days;
    private EditText etMaxDays; // 🛡️ Purge Policy Engine
    
    // QA Flag: Engine Termination State
    private boolean isReplaying = false;
    private ValueAnimator currentAnimator;
    private ScrollView mainScrollView; // Global Reference for Smooth Scrolling Physics
    
    // QA Engine: Dynamic Target Selector
    private String activeTargetUid = "";
    private String activeTargetName = "You";

    @Override
    protected View createLayout() {
        return buildPortraitLayout(); // Unified Dashboard Architecture organic to all Device Rotations
    }

    private View buildPortraitLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // 🚀 Premium Linear        // Header Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header); // 🚀 Native QA Architecture Injection

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView tvTitle = new TextView(this);
        tvTitle.setText("LOCATION LOGS");
        tvTitle.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, tvTitle, "TEXT_HEADER");
        tvTitle.setPadding(0, getResponsiveHeaderTopQuota(), 0, 0);
        header.addView(tvTitle);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Historical GPS Breadcrumbs");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 10, 0, 0);
        header.addView(subtitle);
        
        tvSyncMode = new TextView(this);
        tvSyncMode.setText("OFFLINE SESSION: LOCAL HISTORY");
        tvSyncMode.setTextColor(Color.WHITE);
        tvSyncMode.setBackgroundColor(Color.parseColor("#44FF0000")); // Faded Red for Offline
        tvSyncMode.setPadding(20, 5, 20, 5);
        tvSyncMode.setTextSize(10f);
        tvSyncMode.setTypeface(null, Typeface.BOLD);
        tvSyncMode.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams modeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        modeLp.topMargin = 15;
        header.addView(tvSyncMode, modeLp);

        root.addView(header);

        // Scrollable Control Surface Engine
        mainScrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int hPad = getResponsivePaddingQuota();
        content.setPadding(hPad * 2, hPad * 2, hPad * 2, hPad * 5);
        
        // --- 🚀 NEW: MAX HISTORY PURGE POLICY TILE ---
        LinearLayout purgeCard = new LinearLayout(this);
        purgeCard.setOrientation(LinearLayout.HORIZONTAL);
        purgeCard.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCard(this, purgeCard, "CARD_STANDARD");
        
        ImageView purgeIcon = new ImageView(this);
        purgeIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
        purgeIcon.setColorFilter(getGradientStart());
        purgeCard.addView(purgeIcon, new LinearLayout.LayoutParams(90, 90));
        
        LinearLayout purgeTextWrapper = new LinearLayout(this);
        purgeTextWrapper.setOrientation(LinearLayout.VERTICAL);
        purgeTextWrapper.setPadding(30, 0, 0, 0);
        
        TextView tvPurgeTitle = new TextView(this);
        tvPurgeTitle.setText("MAX LOCAL HISTORY SAVED");
        tvPurgeTitle.setTextColor(getTextColor());
        tvPurgeTitle.setTextSize(11f);
        tvPurgeTitle.setTypeface(null, Typeface.BOLD);
        purgeTextWrapper.addView(tvPurgeTitle);
        
        TextView tvPurgeSub = new TextView(this);
        tvPurgeSub.setText("Auto-purge logs older than:");
        tvPurgeSub.setTextColor(getSecondaryTextColor());
        tvPurgeSub.setTextSize(10f);
        purgeTextWrapper.addView(tvPurgeSub);
        purgeCard.addView(purgeTextWrapper, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        etMaxDays = UiArchitect.createInputField(this, "MAX_HISTORY_DAYS");
        etMaxDays.setText(String.valueOf(prefs.getInt("max_history_days", 7)));
        etMaxDays.setGravity(Gravity.END);
        etMaxDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etMaxDays.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(3) });
        etMaxDays.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        
        // 🚀 QA Fix: Master Policy Enforcement (V13.0)
        etMaxDays.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        
        etMaxDays.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                try {
                    int days = Integer.parseInt(s.toString());
                    if (days > 30) {
                        Toast.makeText(HistoryActivity.this, "SECURITY LIMIT: Max history is 30 days.", Toast.LENGTH_SHORT).show();
                        etMaxDays.setText("030");
                        prefs.edit().putInt("max_history_days", 30).apply();
                    } else {
                        prefs.edit().putInt("max_history_days", days).apply();
                    }
                } catch (Exception e) {}
            }
        });

        // 🚀 L7 Fluid Geometry: Scaling input width per device stage
        int inputWidth = UiScalingMatrixTable.scalePadding(UiArchitect.getStage(this), 130);
        purgeCard.addView(etMaxDays, new LinearLayout.LayoutParams(inputWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        TextView tvDaysSuffix = new TextView(this);
        tvDaysSuffix.setText("DAYS");
        tvDaysSuffix.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvDaysSuffix, "TEXT_LABEL");
        tvDaysSuffix.setPadding(10, 0, 0, 0);
        purgeCard.addView(tvDaysSuffix, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        LinearLayout.LayoutParams purgeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        purgeLp.setMargins(0, 0, 0, getResponsivePaddingQuota() * 2);
        content.addView(purgeCard, purgeLp);

        // --- LAYER 1: FLOATING MAP CARD ---
        LinearLayout mapCard = new LinearLayout(this);
        mapCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, mapCard, "CARD_STANDARD");
        
        FrameLayout mapContainer = new FrameLayout(this);
        mapContainer.setId(ID_MAP_FRAGMENT);
        
        // 🚀 QA Fix: Master Gesture Membrane. Stops ScrollView from stealing Map Pan/Zoom events!
        FrameLayout gestureMembrane = new FrameLayout(this);
        gestureMembrane.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                mainScrollView.requestDisallowInterceptTouchEvent(true);
            } else if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                mainScrollView.requestDisallowInterceptTouchEvent(false);
            }
            return false; // Forward the exact event physically down to the Google Map Fragment!
        });
        
        mapContainer.addView(gestureMembrane, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mapCard.addView(mapContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800));
        
        tvDistance = new TextView(this);
        tvDistance.setTextColor(getTextColor());
        tvDistance.setTextSize(14);
        tvDistance.setText("TOTAL DISTANCE: AWAITING LOAD ENGINE");
        int dPad = getResponsivePaddingQuota();
        tvDistance.setPadding(dPad * 2, dPad, dPad * 2, dPad);
        tvDistance.setTypeface(null, Typeface.BOLD);
        mapCard.addView(tvDistance);
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int cMargin = getResponsivePaddingQuota() * 2;
        cardLp.setMargins(0, 0, 0, cMargin);
        content.addView(mapCard, cardLp);

        // --- LAYER 2: CONTROLS ---
        content.addView(createSectionHeader("REPLAY ENGINE CONTROLS"));
        
        LinearLayout controlsCard = new LinearLayout(this);
        controlsCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, controlsCard, "CARD_STANDARD");
        controlsCard.addView(createHistoryControls());
        content.addView(controlsCard, cardLp);

        // --- LAYER 3: DETAILED TEXTUAL LOGS ---
        content.addView(createSectionHeader("DETAILED LOCATION LOGS"));
        
        tvLastUpdated = new TextView(this);
        tvLastUpdated.setText("LOG ENGINE STANDBY • NO DATA SYNCED");
        tvLastUpdated.setTextColor(getSecondaryTextColor());
        tvLastUpdated.setTextSize(10f);
        tvLastUpdated.setPadding(10, 0, 0, 20);
        content.addView(tvLastUpdated);

        locationLogsContainer = new LinearLayout(this);
        locationLogsContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(locationLogsContainer, cardLp);
        
        mainScrollView.addView(content);
        root.addView(mainScrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return root;
    }

    private View buildLandscapeLayout() {
        return buildPortraitLayout();
    }

    private View createSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getGradientStart()); // Native Theme Tint Map
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);
        int sPad = getResponsivePaddingQuota();
        tv.setPadding(sPad / 2, 0, 0, sPad);
        return tv;
    }

    private View createHistoryControls() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        
        // 🚀 L7 Master Metrics: Initializing Scaling Engine for Hub Parity
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        int hubPad = UiScalingMatrixTable.scalePadding(stage, 25);
        
        int conPad = getResponsivePaddingQuota() * 2;
        container.setPadding(conPad / 2, conPad, conPad / 2, conPad);

        // --- QA TARGET SELECTOR ENGINE ---
        targetLabel = new TextView(this);
        targetLabel.setText("CURRENT TRACK: " + activeTargetName.toUpperCase());
        targetLabel.setTextColor(getGradientStart());
        UiArchitect.applyText(this, targetLabel, "TEXT_BODY"); // 🚀 Reverted to dynamic L7 Body Tier
        targetLabel.setPadding(0, 0, 0, 20);
        container.addView(targetLabel);

        LinearLayout targetRow = new LinearLayout(this);
        targetRow.setOrientation(LinearLayout.HORIZONTAL);
        targetRow.setGravity(Gravity.CENTER_VERTICAL);
        
        View btnSelect = createIconButton("SEARCH TRACKING TARGET", android.R.drawable.ic_menu_search, v -> {
            if (viewModel != null) {
                if (tvStatus != null) tvStatus.setText("Scanning Network for Assets...");
                viewModel.fetchTrackingTargets(this);
            }
        });
        targetRow.addView(btnSelect, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        // 🚀 QA Fix: RADAR DISH NAVIGATION (V13.0)
        LinearLayout dishBox = new LinearLayout(this);
        dishBox.setOrientation(LinearLayout.HORIZONTAL);
        dishBox.setGravity(Gravity.CENTER);
        UiArchitect.applyCard(this, dishBox, "CARD_STANDARD");
        dishBox.setPadding(10, 10, 10, 10);

        ImageView dashIcon = new ImageView(this);
        dashIcon.setImageResource(android.R.drawable.ic_menu_compass); // Tactical Dish change
        dashIcon.setColorFilter(getGradientStart());
        dashIcon.setPadding(20,20,20,20);
        
        // 🛡️ QA Hardening: Standardized Accessibility Mapping
        applyAccessibility(dashIcon, "Focus Tracking Target");
        
        dashIcon.setOnClickListener(v -> {
            if (googleMap != null && !historyPoints.isEmpty()) {
                UserLocation latest = historyPoints.get(historyPoints.size() - 1);
                LatLng pos = new LatLng(latest.getLatitude(), latest.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 18f));
                
                String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(latest.getTimestamp()));
                android.widget.Toast.makeText(this, "FOCUSING TARGET: " + activeTargetName + " • " + time, android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, "NO ACTIVE TRACKING TARGET", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        // 🚀 L7 Optimization: Upscaled to 140px Baseline for Absolute Parity with the Search Lens
        int dishSize = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        dishBox.addView(dashIcon, new LinearLayout.LayoutParams(dishSize, dishSize));
        
        LinearLayout.LayoutParams dishLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dishLp.setMargins(getResponsivePaddingQuota(), 0, 0, 0);
        targetRow.addView(dishBox, dishLp);
        
        // 🚀 L7 Architecture Sync: Forced Vertical Rhythm for Absolute Parity
        LinearLayout.LayoutParams standardRowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        standardRowParams.setMargins(0, hubPad, 0, hubPad); // Equal Vertical Distribution Hub

        int hGap = getResponsivePaddingQuota();
        LinearLayout.LayoutParams horizontalBtnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        // We apply half the gap to each side to ensure perfect 50/50 horizontal distribution
        horizontalBtnParams.setMargins(hGap / 2, 0, hGap / 2, 0);

        container.addView(targetRow);

        btn4h = createIconButton("4 HRS", android.R.drawable.ic_menu_recent_history, v -> selectTimeWindow(btn4h, 4 * 60 * 60 * 1000L));
        btn12h = createIconButton("12 HRS", android.R.drawable.ic_menu_recent_history, v -> selectTimeWindow(btn12h, 12 * 60 * 60 * 1000L));
        btn24h = createIconButton("24 HRS", android.R.drawable.ic_menu_recent_history, v -> selectTimeWindow(btn24h, 24 * 60 * 60 * 1000L));
        btn7days = createIconButton("7 DAYS", android.R.drawable.ic_menu_agenda, v -> selectTimeWindow(btn7days, 7L * 24L * 60L * 60L * 1000L));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setWeightSum(2.0f);
        row1.addView(btn4h, horizontalBtnParams);
        row1.addView(btn12h, horizontalBtnParams);
        container.addView(row1, standardRowParams);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setWeightSum(2.0f);
        row2.setPadding(0, 0, 0, hubPad); // Extra breath for the base grid
        row2.addView(btn24h, horizontalBtnParams);
        row2.addView(btn7days, horizontalBtnParams);
        container.addView(row2, standardRowParams);

        // 🚀 L7 Optimization: REPLAY ENGINE COMMAND HUB (Absolute Symmetry)
        LinearLayout dualControls = new LinearLayout(this);
        dualControls.setOrientation(LinearLayout.HORIZONTAL);
        dualControls.setGravity(Gravity.CENTER_VERTICAL);
        dualControls.setWeightSum(2.0f);

        View playBtn = createIconButton("START REPLAY", android.R.drawable.ic_media_play, v -> {
            isReplaying = true;
            startPathReplay();
        });
        dualControls.addView(playBtn, horizontalBtnParams); 

        View stopBtn = createIconButton("STOP REPLAY", android.R.drawable.ic_media_pause, v -> {
            isReplaying = false; 
            if (currentAnimator != null) currentAnimator.cancel();
            if (tvStatus != null) tvStatus.setText("REPLAY TERMINATED");
        });
        dualControls.addView(stopBtn, horizontalBtnParams); 

        container.addView(dualControls, standardRowParams);

        tvStatus = new TextView(this);
        tvStatus.setText("READY FOR SYNC");
        tvStatus.setTextColor(getSecondaryTextColor()); 
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
        tvStatus.setPadding(0, hubPad, 0, 0);
        container.addView(tvStatus);

        return container;
    }

    private long syncStartTime = 0;

    private void selectTimeWindow(View selectedBtn, long timeWindow) {
        if (btn4h == null || btn12h == null || btn24h == null || btn7days == null) return;
        
        // 🚀 QA Fix: Global Selection Matrix Integration (L7)
        UiArchitect.applySelection(this, btn4h, selectedBtn == btn4h);
        UiArchitect.applySelection(this, btn12h, selectedBtn == btn12h);
        UiArchitect.applySelection(this, btn24h, selectedBtn == btn24h);
        UiArchitect.applySelection(this, btn7days, selectedBtn == btn7days);
        
        // Trigger Data Engine
        loadHistoryData(timeWindow);
    }



    private void loadHistoryData(long timeWindow) {
        if (activeTargetUid == null || activeTargetUid.isEmpty()) {
            activeTargetUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        }
        if (activeTargetUid == null || viewModel == null) return;

        if (locationLogsContainer != null) locationLogsContainer.removeAllViews();
        syncStartTime = System.currentTimeMillis();
        
        if (tvStatus != null) tvStatus.setText("EXTRACTING LATEST " + (timeWindow / 3600000) + "H METRICS...");
        viewModel.loadHistoryData(activeTargetUid, timeWindow);
    }

    private void calculateAnalytics(List<UserLocation> points) {
        double totalDist = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            double delta = SpatialProcessor.getDistance(
                points.get(i).getLatitude(), points.get(i).getLongitude(),
                points.get(i+1).getLatitude(), points.get(i+1).getLongitude()
            );
            
            // 🛑 QA FIX: 15-meter Mechanical Drift Filter. Destroys "56km Stationary Base" Bugs!
            // When phones are indoors, GPS satellites bounce off walls causing 10m jumps. 
            // We software-filter any jump under 15 meters to suppress fake accumulation!
            if (delta > 15.0) {
                totalDist += delta;
            }
        }
        
        if (tvDistance != null) {
            String text = totalDist > 1000 ? 
                String.format("TOTAL DISTANCE: %.2f km", totalDist / 1000) : 
                String.format("TOTAL DISTANCE: %.0f m", totalDist);
            tvDistance.setText(text);
        }
        
        String now = new java.text.SimpleDateFormat("MMM dd, HH:mm:ss").format(new java.util.Date());
        if (tvLastUpdated != null) {
            tvLastUpdated.setText("LOG LAST UPDATED: " + now.toUpperCase());
        }
        
        if (tvSyncMode != null) {
            tvSyncMode.setText("ONLINE SYNC: CLOUD METRICS ACTIVE");
            tvSyncMode.setBackgroundColor(Color.parseColor("#4400FF00")); // Faded Green for Online
        }
    }

    private void renderTextualLogs(List<UserLocation> points) {
        if (locationLogsContainer == null) return;
        locationLogsContainer.removeAllViews();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy | HH:mm a", java.util.Locale.getDefault());

        // 🚀 QA ARCHITECTURE: Clustered Physical Location Accumulation
        // Compresses massive logs (e.g. 1 hour stationary ping loops) into single Master Cards!
        java.util.List<java.util.List<UserLocation>> groupedPoints = new java.util.ArrayList<>();
        java.util.List<UserLocation> currentGroup = new java.util.ArrayList<>();
        
        for (int i = 0; i < points.size(); i++) {
            UserLocation current = points.get(i);
            if (currentGroup.isEmpty()) {
                currentGroup.add(current);
            } else {
                UserLocation anchor = currentGroup.get(0);
                double delta = SpatialProcessor.getDistance(
                    current.getLatitude(), current.getLongitude(),
                    anchor.getLatitude(), anchor.getLongitude()
                );
                if (delta < 20.0) { // Same vicinity (Mechanical drift under 20m)
                    currentGroup.add(current);
                } else {
                    groupedPoints.add(currentGroup);
                    currentGroup = new java.util.ArrayList<>();
                    currentGroup.add(current);
                }
            }
        }
        if (!currentGroup.isEmpty()) groupedPoints.add(currentGroup);

        int renderLimit = 0;
        
        // Render Inversely (Newest Block First)
        for (int i = groupedPoints.size() - 1; i >= 0; i--) {
            if (renderLimit >= 250) break;
            
            java.util.List<UserLocation> block = groupedPoints.get(i);
            UserLocation newestPingInBlock = block.get(block.size() - 1); // Exact latest coordinate recorded here
            UserLocation oldestPingInBlock = block.get(0);
            
            LinearLayout logCard = new LinearLayout(this);
            // 🚀 L7 Adaptive Content: Switching to Vertical in LIBERAL to prevent asset collision!
            logCard.setOrientation(UiArchitect.getStage(this) == UiArchitect.DesignStage.LIBERAL ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            int cp = UiScalingMatrixTable.scalePadding(UiArchitect.getStage(this), 40);
            logCard.setPadding(cp, cp, cp, cp);
            logCard.setGravity(Gravity.CENTER_VERTICAL);
            UiArchitect.applyCard(this, logCard, "CARD_STANDARD");

            LinearLayout textBlock = new LinearLayout(this);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvHeader = new TextView(this);
            if (block.size() > 1) {
                // 🚀 QA Transparency Fix: Explicitly declare the Compression Count directly in the UI Header!
                tvHeader.setText("⏱️ SESSION (" + block.size() + " PINGS) • " + sdf.format(new java.util.Date(oldestPingInBlock.getTimestamp())) + " TO " + sdf.format(new java.util.Date(newestPingInBlock.getTimestamp())));
            } else {
                tvHeader.setText("⏱️ PING LOG  •  " + sdf.format(new java.util.Date(newestPingInBlock.getTimestamp())));
            }
            tvHeader.setTextColor(getGradientStart());
            tvHeader.setTextSize(11f);
            tvHeader.setTypeface(null, Typeface.BOLD);
            textBlock.addView(tvHeader);

            TextView tvCoords = new TextView(this);
            tvCoords.setText(String.format(java.util.Locale.US, "📍 LAT: %.6f, LNG: %.6f", newestPingInBlock.getLatitude(), newestPingInBlock.getLongitude()));
            tvCoords.setTextColor(getTextColor());
            tvCoords.setAlpha(0.85f);
            tvCoords.setTextSize(13f);
            tvCoords.setTypeface(null, Typeface.BOLD);
            tvCoords.setPadding(0, 15, 0, 20);
            textBlock.addView(tvCoords);

            // 🛑 QA FIX: Geocoder Thread Explosion Mitigation
            // Spawning 250 raw threads simultaneously instantly crashes the RAM and CPU!
            // We route requests strictly through the natively queued OS Thread Pool or Async API.
            android.location.Geocoder geocoder = new android.location.Geocoder(HistoryActivity.this, java.util.Locale.getDefault());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(newestPingInBlock.getLatitude(), newestPingInBlock.getLongitude(), 1, new android.location.Geocoder.GeocodeListener() {
                    @Override
                    public void onGeocode(@androidx.annotation.NonNull java.util.List<android.location.Address> addresses) {
                        if (addresses != null && !addresses.isEmpty()) {
                            final String humanStreet = addresses.get(0).getAddressLine(0);
                            runOnUiThread(() -> tvCoords.setText("📍 " + humanStreet));
                        }
                    }
                    @Override
                    public void onError(@androidx.annotation.Nullable String errorMessage) {}
                });
            } else {
                android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                    try {
                        @SuppressWarnings("deprecation")
                        java.util.List<android.location.Address> addresses = geocoder.getFromLocation(newestPingInBlock.getLatitude(), newestPingInBlock.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            final String humanStreet = addresses.get(0).getAddressLine(0);
                            runOnUiThread(() -> tvCoords.setText("📍 " + humanStreet));
                        }
                    } catch (Exception e) {}
                });
            }

            LinearLayout telemetryBlock = new LinearLayout(this);
            telemetryBlock.setOrientation(LinearLayout.HORIZONTAL);
            
            TextView tvSpeed = new TextView(this);
            float speedKmh = (newestPingInBlock.getSpeed() * 3.6f);
            tvSpeed.setText(String.format(java.util.Locale.US, "🚙 %.1f km/h", speedKmh));
            tvSpeed.setTextColor(getTextColor());
            tvSpeed.setTextSize(11f);
            tvSpeed.setTypeface(null, Typeface.BOLD);
            tvSpeed.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            telemetryBlock.addView(tvSpeed);

            TextView tvBattery = new TextView(this);
            int battery = newestPingInBlock.getBatteryLevel();
            tvBattery.setText("🔋 " + (battery > -1 ? battery + "%" : "N/A"));
            tvBattery.setTextColor(getTextColor());
            tvBattery.setTextSize(11f);
            tvBattery.setTypeface(null, Typeface.BOLD);
            tvBattery.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            telemetryBlock.addView(tvBattery);
            
            // 🔥 PHYSICAL CARD COMPRESSION STATS
            if (block.size() > 1) {
                TextView tvCount = new TextView(this);
                tvCount.setText("📍 " + block.size() + " Pings");
                tvCount.setTextColor(getTextColor());
                tvCount.setTextSize(11f);
                tvCount.setTypeface(null, Typeface.BOLD);
                tvCount.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                telemetryBlock.addView(tvCount);
            }

            textBlock.addView(telemetryBlock);

            if (newestPingInBlock.isSosActive()) {
                TextView tvSos = new TextView(this);
                tvSos.setText("🚨 SYSTEM EMERGENCY TRIGGERED");
                tvSos.setTextColor(Color.RED);
                tvSos.setTextSize(11f);
                tvSos.setPadding(0, 15, 0, 0);
                tvSos.setTypeface(null, Typeface.BOLD);
                textBlock.addView(tvSos);
            }

            logCard.addView(textBlock);

            ImageView focusLens = new ImageView(this);
            focusLens.setImageResource(android.R.drawable.ic_menu_mylocation);
            focusLens.setPadding(30, 30, 10, 30);
            focusLens.setColorFilter(getGradientStart()); 
            
            // 🚀 QA Fix: Standardized Accessibility Mapping (V15.2)
            applyAccessibility(focusLens, "Focus on Waypoint");

            // Natively resolve OS-level visual focus feedback (Ripple / Highlight ring)
            android.util.TypedValue outValue = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            focusLens.setBackgroundResource(outValue.resourceId);

            focusLens.setOnClickListener(v -> {
                if (googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(newestPingInBlock.getLatitude(), newestPingInBlock.getLongitude()), 19f));
                    if (mainScrollView != null) mainScrollView.smoothScrollTo(0, 0); 
                    android.widget.Toast.makeText(this, "Focused on " + block.size() + " Tracked Pings", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            
            logCard.addView(focusLens);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 25);
            locationLogsContainer.addView(logCard, lp);
            
            renderLimit++;
        }
    }

    private void drawStaticPath(List<UserLocation> points) {
        if (googleMap == null) return;
        googleMap.clear();
        
        int accent = getGradientStart(); // Safe Premium Override
        int colorWithAlpha = Color.argb(128, Color.red(accent), Color.green(accent), Color.blue(accent));
        
        PolylineOptions options = new PolylineOptions().color(colorWithAlpha).width(12); // Natively Thicken Line
        for (UserLocation p : points) {
            options.add(new LatLng(p.getLatitude(), p.getLongitude()));
        }
        googleMap.addPolyline(options);
        
        if (!points.isEmpty()) {
            UserLocation first = points.get(0);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(first.getLatitude(), first.getLongitude()), 15f));
        }
    }

    private void startPathReplay() {
        if (historyPoints.size() < 2 || googleMap == null) return;
        
        if (replayAvatar != null) replayAvatar.remove();
        
        UserLocation first = historyPoints.get(0);
        
        // 🚀 QA Avatar Injection: Renders authentic Profile Graphics on History Replays natively!
        android.graphics.Bitmap profileBmp = null;
        String safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (safeUid == null) safeUid = "U_MOCK";

        // Attempt to extract local profile HD image if the parent is tracking themselves.
        if (activeTargetUid.equals(safeUid) || activeTargetUid.startsWith("DEV_")) {
            profileBmp = com.jdroidx.jfindme.utils.AvatarLoader.getCurrentUserAvatarBitmap(this, 256);
        }

        com.google.android.gms.maps.model.BitmapDescriptor trueAvatarPin = 
            com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generate(this, activeTargetName, profileBmp, true, false);

        replayAvatar = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(first.getLatitude(), first.getLongitude()))
                .title("You")
                .anchor(0.5f, 0.5f)
                .icon(trueAvatarPin));
        
        animateReplay(0);
    }

    private void animateReplay(final int index) {
        if (!isReplaying) return; // Immediate Engine Termination
        if (index >= historyPoints.size() - 1) {
            if (tvStatus != null) tvStatus.setText("REPLAY ARCHIVE COMPLETED");
            isReplaying = false;
            return;
        }

        final UserLocation start = historyPoints.get(index);
        final UserLocation end = historyPoints.get(index + 1);
        final LatLng startLatLng = new LatLng(start.getLatitude(), start.getLongitude());
        final LatLng endLatLng = new LatLng(end.getLatitude(), end.getLongitude());

        if (tvStatus != null) {
            tvStatus.setText("REPLAYING PING #" + (index+1) + " -> #" + (index+2));
        }

        currentAnimator = ValueAnimator.ofFloat(0, 1);
        currentAnimator.setDuration(800); // Smoother, slower transit
        currentAnimator.setInterpolator(new LinearInterpolator());
        currentAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            double lat = v * endLatLng.latitude + (1 - v) * startLatLng.latitude;
            double lng = v * endLatLng.longitude + (1 - v) * startLatLng.longitude;
            LatLng current = new LatLng(lat, lng);
            if (replayAvatar != null) replayAvatar.setPosition(current);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(current));
        });
        
        currentAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (isReplaying) {
                    animateReplay(index + 1);
                }
            }
        });
        currentAnimator.start();
    }

    @Override
    protected void setupLogic() {
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(HistoryViewModel.class);
        viewModel.init(this);

        // 🚀 MVVM Waterfall Observers
        viewModel.isLoading().observe(this, loading -> {
            if (loading != null && loading) {
                if (tvStatus != null) tvStatus.setText("FETCHING CLOUD METRICS...");
                com.jdroidx.jfindme.utils.AngelLoader.show(this);
            } else {
                // Guardian Angel Delay Architecture
                long timePassed = System.currentTimeMillis() - syncStartTime;
                long forcedDelay = timePassed < 2000 ? 2000L - timePassed : 0L;
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                }, forcedDelay);
            }
        });

        viewModel.getHistoryPoints().observe(this, points -> {
            if (points == null || points.isEmpty()) {
                if (tvStatus != null) tvStatus.setText("0 METRICS FOUND");
                Toast.makeText(this, "No history found for this period", Toast.LENGTH_SHORT).show();
            } else {
                historyPoints = points;
                drawStaticPath(points);
                calculateAnalytics(points);
                renderTextualLogs(points);
                if (tvStatus != null) tvStatus.setText(points.size() + " WAYPOINTS LOADED SECURELY");
            }
        });

        viewModel.getTrackingTargets().observe(this, targets -> {
            if (targets == null || targets.isEmpty()) return;
            
            String[] names = new String[targets.size()];
            for (int i = 0; i < targets.size(); i++) names[i] = targets.get(i).getName();

            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Focus Tracking Asset")
                .setItems(names, (d, which) -> {
                    HistoryViewModel.TrackingTarget selected = targets.get(which);
                    activeTargetUid = selected.getUid();
                    String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                    activeTargetName = (activeTargetUid != null && activeTargetUid.equals(myUid)) ? "You" : selected.getName();
                    // 🚀 Refresh UI Header 
                    runOnUiThread(() -> {
                        if (targetLabel != null) targetLabel.setText("CURRENT TRACK: " + activeTargetName.toUpperCase());
                        // 🚀 QA Fix: Force 4H Default on Target Selection (L4 Enterprise Rule)
                        selectTimeWindow(btn4h, 4 * 60 * 60 * 1000L); 
                    });
                }).show();
            
            applyDialogTheme(dialog);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_FRAGMENT);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(ID_MAP_FRAGMENT, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng pune = new LatLng(18.5204, 73.8567);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pune, 12f));
    }
}
