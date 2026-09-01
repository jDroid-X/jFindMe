package com.jdroidx.jfindme.ui.management;

import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.ui.base.ThemeManager;
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
    private String currentTargetAvatarId = "butterfly";
    private ImageView targetAvatarView;
    private TextView tvSelectTarget;
    private int selectedTimeIndex = 0;
    private List<Map<String, Object>> currentSessions = new ArrayList<>();
    
    // ≡ƒº▒ QA Guard: Registry of already scavenged forensic windows in this app session
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

        // ≡ƒÜÇ QA Fix: Global UI Architecture Sync! Use the official ListItem engine.
        View storageCap = com.jdroidx.jfindme.ui.base.ListItemTable.createRow(
            this, 
            "ROW_STORAGE", 
            "MAXIMUM LOCAL HISTORY SAVED", 
            "Forensic Storage Override", 
            pillBox
        );
        
        // ≡ƒº▒ L7 Aesthetic Sync: Disable row-wide click to prevent 'Double-Tap' focus lag on nested EditText
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
        
        // Embedded Distance Footer (Theme Aware Capsule)
        LinearLayout distFooterWrap = new LinearLayout(this);
        distFooterWrap.setGravity(Gravity.CENTER);
        distFooterWrap.setPadding(0, 0, 0, scale(20));
        
        LinearLayout distFooter = new LinearLayout(this);
        distFooter.setOrientation(LinearLayout.VERTICAL);
        distFooter.setBackground(createPopupBackground());
        int dfPadHorizontal = scale(40);
        int dfPadVertical = scale(14);
        distFooter.setPadding(dfPadHorizontal, dfPadVertical, dfPadHorizontal, dfPadVertical);
        
        tvDistance = new TextView(this);
        tvDistance.setTextColor(getGradientOverlayText());
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
        
        UiArchitect.applyCardWithOverride(this, targetSelectorWrap, "CARD_STANDARD", 1.2f);
        targetSelectorWrap.setOnClickListener(v -> showTargetSelector());
        targetSelectorWrap.setFocusable(true);
        targetSelectorWrap.setClickable(true);
        
        int tsPad = scale(14);
        targetSelectorWrap.setPadding(tsPad, tsPad, tsPad, tsPad);
        
        targetAvatarView = new ImageView(this);
        int avSize = scale(48);
        targetAvatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.drawable.GradientDrawable gRing = new android.graphics.drawable.GradientDrawable();
        gRing.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gRing.setColor(Color.parseColor("#1A000000"));
        gRing.setStroke(scale(2), getGradientStart());
        targetAvatarView.setBackground(gRing);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, targetAvatarView);
        targetSelectorWrap.addView(targetAvatarView, new LinearLayout.LayoutParams(avSize, avSize));
        
        LinearLayout targetInfoLayout = new LinearLayout(this);
        targetInfoLayout.setOrientation(LinearLayout.VERTICAL);
        targetInfoLayout.setPadding(scale(14), 0, 0, 0);
        targetInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        tvSelectTarget = new TextView(this);
        tvSelectTarget.setText("SELECT TRACKING TARGET");
        tvSelectTarget.setTextColor(getTextColor());
        tvSelectTarget.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvSelectTarget, "TEXT_TITLE");
        targetInfoLayout.addView(tvSelectTarget);

        TextView tvTapHint = new TextView(this);
        tvTapHint.setText("Tap to switch tracking target");
        tvTapHint.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvTapHint, "TEXT_MINI");
        targetInfoLayout.addView(tvTapHint);

        targetSelectorWrap.addView(targetInfoLayout);

        ImageView switchIcon = new ImageView(this);
        switchIcon.setImageResource(android.R.drawable.ic_menu_more);
        switchIcon.setColorFilter(getGradientStart());
        int icSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        targetSelectorWrap.addView(switchIcon, new LinearLayout.LayoutParams(icSize, icSize));
        
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
        // ≡ƒÜÇ QA Fix: Force Clipping Disable to enable floating shadows on all data segments
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
        
        UiArchitect.applyCardWithOverride(this, btn, "CARD_STANDARD", 1.2f);
        
        int p = scale(25);
        btn.setPadding(p, p, p, p);
        
        ImageView icon = new ImageView(this);
        icon.setImageResource(resId);
        icon.setColorFilter(getGradientStart()); 
        int s = scale(55);
        btn.addView(icon, new LinearLayout.LayoutParams(s, s));
        
        TextView tv = new TextView(this);
        tv.setText(text.toUpperCase());
        tv.setTextColor(getTextColor()); 
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
        
        UiArchitect.applyCardWithOverride(this, tile, "CARD_STANDARD", 1.0f);

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
                if (selected) {
                    android.graphics.drawable.GradientDrawable selBg = new android.graphics.drawable.GradientDrawable();
                    selBg.setColor(ThemeManager.isDarkTheme(getResolvedThemeId()) ? Color.parseColor("#330EA5E9") : Color.parseColor("#331B4332"));
                    selBg.setCornerRadius(scale(14));
                    selBg.setStroke(scale(2), getGradientStart());
                    layout.setBackground(selBg);
                } else {
                    layout.setBackground(null);
                    UiArchitect.applyCard(this, layout, "CARD_STANDARD");
                }

                for (int j = 0; j < layout.getChildCount(); j++) {
                    View child = layout.getChildAt(j);
                    if (child instanceof ImageView) {
                        ((ImageView) child).setAlpha(selected ? 1.0f : 0.7f);
                    } else if (child instanceof TextView) {
                        ((TextView) child).setTextColor(selected ? getGradientStart() : getTextColor());
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
        if (currentTargetUid == null || currentTargetUid.isEmpty()) {
            if (tvStatus != null) tvStatus.setText("SELECT A TARGET TO START FETCH");
            return;
        }
        
        String myUid = FirebaseAuth.getInstance().getUid();
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        if (currentTargetUid.equals(selfAgentId) || (myUid != null && currentTargetUid.equals(myUid))) {
            // Load local offline records for self
            viewModel.loadHistorySessions(this, currentTargetUid, durationMs);
            if (tvStatus != null) tvStatus.setText("READY • LOCAL LOGS LOADED");
            return;
        }

        // Redundancy Guard (The 'Warm' Registry)
        String lockKey = currentTargetUid + "_" + durationMs;
        if (warmedVaults.contains(lockKey)) {
            viewModel.loadHistorySessions(this, currentTargetUid, durationMs);
            return;
        }

        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Retrieving the History Data");
        minLoadTimerExpired = false;

        if (viewModel != null) {
            viewModel.loadHistoryData(currentTargetUid, durationMs);
            warmedVaults.add(lockKey);
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
             minLoadTimerExpired = true;
             if (viewModel != null && viewModel.isLoading().getValue() != Boolean.TRUE) {
                 com.jdroidx.jfindme.utils.AngelLoader.hide(this);
             }
        }, 2000);
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
    protected void onUiRebuilt(android.os.Bundle savedState) {
        if (savedState != null) {
            String tUid = savedState.getString("SAVED_TARGET_UID");
            if (tUid != null) currentTargetUid = tUid;
            String tName = savedState.getString("SAVED_TARGET_NAME");
            if (tName != null) currentTargetName = tName;
            selectedTimeIndex = savedState.getInt("SAVED_TIME_INDEX", 0);
            isReplaying = savedState.getBoolean("SAVED_IS_REPLAYING", false);
        }
        
        updateTrackLabel();
        updateTimeTileStyles(selectedTimeIndex);
        updateHistoryTable(currentSessions);
        
        if (!historyPoints.isEmpty()) {
            calculateAnalytics(historyPoints);
            tvWaypointsCount.setText(historyPoints.size() + " WAYPOINTS LOADED SECURELY");
            drawStaticPath(historyPoints);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_FRAGMENT);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private com.google.android.gms.maps.model.BitmapDescriptor getSelectedTargetAvatarDescriptor(int sizeDp) {
        try {
            String myUid = FirebaseAuth.getInstance().getUid();
            String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
            boolean isSelf = (currentTargetUid == null || currentTargetUid.equals(selfAgentId) || (myUid != null && currentTargetUid.equals(myUid)));
            
            android.graphics.Bitmap rawBmp;
            if (isSelf) {
                rawBmp = com.jdroidx.jfindme.utils.AvatarLoader.getCurrentUserAvatarBitmap(this, sizeDp);
            } else {
                rawBmp = com.jdroidx.jfindme.utils.AvatarLoader.getRemoteAvatarBitmap(this, currentTargetAvatarId, sizeDp);
            }
            
            if (rawBmp != null) {
                int px = scale(sizeDp);
                android.graphics.Bitmap circular = com.jdroidx.jfindme.utils.AvatarLoader.createCircularBitmap(rawBmp, px);
                if (circular != null) {
                    return BitmapDescriptorFactory.fromBitmap(circular);
                }
            }
        } catch (Exception e) {
            SystemLogger.e(TAG, "Failed to build avatar descriptor for history map", e);
        }
        return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
    }

    private void drawStaticPath(List<UserLocation> points) {
        if (googleMap == null || points == null || points.isEmpty()) return;
        googleMap.clear();
        
        int accent = getGradientStart();
        int semiTransparentColor = Color.argb(180, Color.red(accent), Color.green(accent), Color.blue(accent));
        
        PolylineOptions options = new PolylineOptions()
            .color(semiTransparentColor)
            .width(14)
            .geodesic(true);
            
        for (UserLocation p : points) {
            options.add(new LatLng(p.getLatitude(), p.getLongitude()));
        }
        googleMap.addPolyline(options);
        
        // 1. Start Landmark Marker (Green Beacon)
        UserLocation first = points.get(0);
        LatLng startPos = new LatLng(first.getLatitude(), first.getLongitude());
        String startTimeStr = first.getResolvedTimestamp();
        googleMap.addMarker(new MarkerOptions()
            .position(startPos)
            .title("🏁 JOURNEY START")
            .snippet("Recorded: " + startTimeStr)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // 2. Finish / Latest Fix Marker (Selected Target's Custom Avatar Pin!)
        UserLocation last = points.get(points.size() - 1);
        LatLng endPos = new LatLng(last.getLatitude(), last.getLongitude());
        String endTimeStr = last.getResolvedTimestamp();
        
        com.google.android.gms.maps.model.BitmapDescriptor avatarDesc = getSelectedTargetAvatarDescriptor(48);
        googleMap.addMarker(new MarkerOptions()
            .position(endPos)
            .title("📍 " + (currentTargetName != null ? currentTargetName.toUpperCase() : "TARGET"))
            .snippet("Latest Fix: " + endTimeStr)
            .anchor(0.5f, 0.5f)
            .icon(avatarDesc));

        // 3. Smooth Camera Bounds Fit
        if (points.size() == 1) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startPos, 16f));
        } else {
            com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
            for (UserLocation p : points) {
                builder.include(new LatLng(p.getLatitude(), p.getLongitude()));
            }
            try {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), scale(60)));
            } catch (Exception e) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(endPos, 15f));
            }
        }
    }

    private void startPathReplay() {
        if (googleMap == null) return;
        if (historyPoints.size() < 2) {
            UiArchitect.showValidationDialog(this, "Insufficient Route Data", "At least 2 GPS waypoints are required to simulate a route replay. Please select a time or date with recorded movement.", null);
            return;
        }
        
        isReplaying = true;
        if (replayAvatar != null) replayAvatar.remove();
        
        UserLocation first = historyPoints.get(0);
        com.google.android.gms.maps.model.BitmapDescriptor avatarDesc = getSelectedTargetAvatarDescriptor(52);
        
        replayAvatar = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(first.getLatitude(), first.getLongitude()))
                .anchor(0.5f, 0.5f)
                .title("TRACING: " + (currentTargetName != null ? currentTargetName.toUpperCase() : "TARGET"))
                .icon(avatarDesc));
                
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
            if (googleMap != null) googleMap.moveCamera(CameraUpdateFactory.newLatLng(current));
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
            
            if (!loading && minLoadTimerExpired) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                }, 400);
            }
        });

        viewModel.getTrackingTargets().observe(this, targets -> {
            if (targets != null && !targets.isEmpty()) {
                if (currentTargetUid == null || currentTargetUid.isEmpty()) {
                    HistoryViewModel.TrackingTarget firstT = targets.get(0);
                    currentTargetUid = firstT.getUid();
                    currentTargetName = firstT.getName();
                    currentTargetAvatarId = firstT.getAvatarId();
                    updateTrackLabel();
                    loadHistoryData(DURATIONS[selectedTimeIndex]);
                } else {
                    for (HistoryViewModel.TrackingTarget t : targets) {
                        if (currentTargetUid.equals(t.getUid())) {
                            currentTargetAvatarId = t.getAvatarId();
                            if (currentTargetName == null || currentTargetName.isEmpty()) currentTargetName = t.getName();
                            updateTrackLabel();
                            break;
                        }
                    }
                }
            }
        });

        viewModel.fetchTrackingTargets(this);
        
        // --- Intent Linkage Protocol (Case Insensitive) ---
        currentTargetUid = getIntent().getStringExtra("TARGET_UID");
        if (currentTargetUid == null || currentTargetUid.isEmpty()) {
            currentTargetUid = getIntent().getStringExtra("target_uid");
        }
        currentTargetName = getIntent().getStringExtra("TARGET_NAME");
        if (currentTargetName == null || currentTargetName.isEmpty()) {
            currentTargetName = getIntent().getStringExtra("target_name");
        }
        
        if (currentTargetUid != null && !currentTargetUid.isEmpty()) {
            loadHistoryData(DURATIONS[0]); 
            updateTimeTileStyles(0);
        } else {
            updateTrackLabel();
            if (tvStatus != null) tvStatus.setText("READY FOR SYNC • SELECT TARGET");
        }

        // Clean up stale map fragment to support robust landscape/portrait rotation
        SupportMapFragment oldFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_FRAGMENT);
        if (oldFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(oldFragment).commitNowAllowingStateLoss();
        }

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(ID_MAP_FRAGMENT, mapFragment)
                .commitNowAllowingStateLoss();
        mapFragment.getMapAsync(this);
    }

    private void updateTrackLabel() {
        String myUid = FirebaseAuth.getInstance().getUid();
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        boolean isSelf = (currentTargetUid == null || currentTargetUid.equals(selfAgentId) || (myUid != null && currentTargetUid.equals(myUid)));
        
        if ((currentTargetName == null || currentTargetName.isEmpty() || currentTargetName.equalsIgnoreCase("UNKNOWN NODE")) && currentTargetUid != null && !isSelf) {
            try {
                com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
                Map<String, String> dos = db.getLocalDossier(currentTargetUid);
                String resolved = dos.get("fullName");
                if (resolved != null && !resolved.isEmpty()) currentTargetName = resolved;
                String av = dos.get("avatar_id");
                if (av != null && !av.isEmpty()) currentTargetAvatarId = av;
            } catch (Exception ignored) {}
        }

        String name = (currentTargetName != null && !currentTargetName.isEmpty()) ? 
            currentTargetName.toUpperCase() : 
            (isSelf ? "MY DEVICE (LOCAL LOGS)" : ("AGENT_" + currentTargetUid));

        if (tvTrackLabel != null) {
            tvTrackLabel.setText("CURRENT TRACK: " + name);
            tvTrackLabel.setAlpha(1.0f);
        }

        if (tvSelectTarget != null) {
            tvSelectTarget.setText(name);
        }

        if (targetAvatarView != null) {
            if (isSelf) {
                com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, targetAvatarView);
            } else {
                com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, targetAvatarView, currentTargetAvatarId);
            }
        }
    }

    private void showTargetSelector() {
        List<HistoryViewModel.TrackingTarget> targets = viewModel.getTrackingTargets().getValue();
        
        if (targets == null || targets.isEmpty()) {
            Toast.makeText(this, "Refilling Target Matrix...", Toast.LENGTH_SHORT).show();
            viewModel.fetchTrackingTargets(this);
            return;
        }

        UiArchitect.OptionItem[] options = new UiArchitect.OptionItem[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            HistoryViewModel.TrackingTarget t = targets.get(i);
            int iconRes = com.jdroidx.jfindme.R.drawable.ic_family_friends;
            if (t.getRole().equalsIgnoreCase("Host")) {
                iconRes = android.R.drawable.ic_menu_myplaces;
            } else if (t.getRole().toUpperCase().contains("CHILD") || t.getRole().toUpperCase().contains("SON") || t.getRole().toUpperCase().contains("DAUGHTER")) {
                iconRes = com.jdroidx.jfindme.R.drawable.ic_user_profile;
            }
            options[i] = new UiArchitect.OptionItem("👤 " + t.getName(), "Role: " + t.getRole() + " • Node: " + t.getUid(), iconRes);
        }

        UiArchitect.showRichOptionsDialog(this, "Select Tracking Target", options, (index, label) -> {
            HistoryViewModel.TrackingTarget selected = targets.get(index);
            currentTargetUid = selected.getUid();
            currentTargetName = selected.getName();
            currentTargetAvatarId = selected.getAvatarId();
            updateTrackLabel();
            loadHistoryData(DURATIONS[selectedTimeIndex]);
            updateTimeTileStyles(selectedTimeIndex);
            Toast.makeText(this, "Target Synchronized: " + currentTargetName, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateHistoryTable(List<Map<String, Object>> sessions) {
        if (logsContainer == null) return;
        logsContainer.removeAllViews();
        
        if (sessions == null || sessions.isEmpty()) {
            View emptyCard = UiArchitect.createEmptyStateCard(
                this,
                "No Location History Recorded",
                "Historical breadcrumbs will automatically populate here as location points are tracked over time.",
                android.R.drawable.ic_menu_recent_history,
                null,
                null
            );
            logsContainer.addView(emptyCard);
            return;
        }

        for (Map<String, Object> session : sessions) {
            logsContainer.addView(createSessionTile(session));
        }
    }

    private View createSessionTile(Map<String, Object> session) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, outer, "CARD_STANDARD", 1.2f);
        
        int margin = com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.getUniversalGap(com.jdroidx.jfindme.ui.base.UiArchitect.getStage(this));
        
        LinearLayout.LayoutParams outerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        outerLp.setMargins(0, 0, 0, margin);
        outer.setLayoutParams(outerLp);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        int p = scale(25);
        row1.setPadding(p, p, p, 0);

        TextView tvLabel = new TextView(this);
        Object pingObj = session.get("ping_count");
        long pings = (pingObj instanceof Number) ? ((Number) pingObj).longValue() : 1L;
        
        Object fromObj = session.get("from_datetime") != null ? session.get("from_datetime") : session.get("ts_start");
        long from = parseEpochMillis(fromObj);
        
        Object toObj = session.get("to_datetime") != null ? session.get("to_datetime") : session.get("ts_end");
        long to = parseEpochMillis(toObj);
        if (to < from) to = from;
        
        String fromDateStr = new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(new java.util.Date(from));
        String fromTimeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(from));
        String toTimeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(to));
        
        long durationMs = to - from;
        String stayDurationStr;
        if (durationMs >= 3600000L) {
            long hours = durationMs / 3600000L;
            long mins = (durationMs % 3600000L) / 60000L;
            stayDurationStr = hours + "h " + mins + "m stay";
        } else if (durationMs >= 60000L) {
            long mins = durationMs / 60000L;
            stayDurationStr = mins + "m stay";
        } else {
            stayDurationStr = pings + (pings == 1 ? " Fix" : " Fixes");
        }
        
        if (durationMs >= 60000L && !fromTimeStr.equals(toTimeStr)) {
            tvLabel.setText("📍 " + fromDateStr + " • " + fromTimeStr + " - " + toTimeStr + " (" + stayDurationStr + ")");
        } else {
            tvLabel.setText("📍 " + fromDateStr + " • " + fromTimeStr + " (" + stayDurationStr + ")");
        }
        tvLabel.setTextColor(getGradientStart());
        tvLabel.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvLabel, "TEXT_MINI");
        tvLabel.setPadding(0, 0, 0, 0); 
        row1.addView(tvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        ImageView icTarget = new ImageView(this);
        icTarget.setImageResource(android.R.drawable.ic_menu_mylocation);
        icTarget.setColorFilter(getGradientStart());
        row1.addView(icTarget, new LinearLayout.LayoutParams(scale(55), scale(55)));

        outer.addView(row1);

        // Address
        TextView tvAddr = new TextView(this);
        Object addrObj = session.get("address") != null ? session.get("address") : session.get("geo_address");
        String safeAddr = (addrObj != null && !addrObj.toString().trim().isEmpty()) ? addrObj.toString() : "Recorded Coordinate Fix";
        tvAddr.setText(safeAddr);
        tvAddr.setTextColor(getTextColor());
        tvAddr.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvAddr, "TEXT_TITLE");
        tvAddr.setPadding(p, scale(6), p, scale(8));
        outer.addView(tvAddr);

        // Stats Row
        LinearLayout rowStats = new LinearLayout(this);
        rowStats.setOrientation(LinearLayout.HORIZONTAL);
        rowStats.setPadding(p, 0, p, p);
        
        Object speedObj = session.get("speed");
        float spdVal = (speedObj instanceof Number) ? ((Number) speedObj).floatValue() : 0f;
        String speedStr = (spdVal > 0) ? String.format(java.util.Locale.US, "%.1f km/h", spdVal * 3.6f) : "Resting";
        
        Object batObj = session.get("battery");
        int batVal = (batObj instanceof Number) ? ((Number) batObj).intValue() : -1;
        String battery = (batVal >= 0) ? (batVal + "%") : "--";
        
        rowStats.addView(createMiniStat("📍", pings + " Fixes"));
        rowStats.addView(createMiniStat("🚗", speedStr));
        rowStats.addView(createMiniStat("🔋", battery));
        
        outer.addView(rowStats);

        outer.setOnClickListener(v -> {
            if (googleMap != null) {
                Object latObj = session.get("lat");
                Object lngObj = session.get("lng");
                double lat = latObj instanceof Double ? (Double) latObj : (latObj instanceof Number ? ((Number) latObj).doubleValue() : 0.0);
                double lng = lngObj instanceof Double ? (Double) lngObj : (lngObj instanceof Number ? ((Number) lngObj).doubleValue() : 0.0);
                
                LatLng pos = new LatLng(lat, lng);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
                
                if (replayAvatar != null) replayAvatar.remove();
                
                replayAvatar = googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title("INSPECTING NODE")
                        .snippet(safeAddr)
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                        
                replayAvatar.showInfoWindow();
                
                if (tvStatus != null) tvStatus.setText("NODE: " + safeAddr);
                Toast.makeText(this, "Inspecting Node: " + pings + " Pings at this Radius", Toast.LENGTH_SHORT).show();
            }
        });

        return outer;
    }

    private long parseEpochMillis(Object obj) {
        if (obj == null) return System.currentTimeMillis();
        if (obj instanceof Number) {
            long val = ((Number) obj).longValue();
            if (val >= 1000000000000L && val <= 4102444800000L) {
                return val;
            }
            if (val >= 1000000000L && val <= 4102444800L) {
                return val * 1000L;
            }
            return parseForensicString(String.valueOf(val));
        }
        return parseForensicString(obj.toString());
    }

    private long parseForensicString(String str) {
        if (str == null || str.trim().isEmpty() || "0".equals(str.trim())) return System.currentTimeMillis();
        String s = str.trim();
        try {
            if (s.matches("^\\d{13}$")) {
                return Long.parseLong(s);
            }
            if (s.matches("^\\d{10}$")) {
                return Long.parseLong(s) * 1000L;
            }
            String[] patterns = new String[]{"yyyyMMddHHmmssSS", "yyyyMMddHHmmss", "yyyyMMdd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss"};
            for (String p : patterns) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(p, java.util.Locale.US);
                    java.util.Date d = sdf.parse(s.length() > p.length() ? s.substring(0, p.length()) : s);
                    if (d != null) return d.getTime();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return System.currentTimeMillis();
    }

    private View createMiniStat(String icon, String text) {
        TextView tv = new TextView(this);
        tv.setText(icon + " " + text);
        tv.setTextColor(getSecondaryTextColor());
        tv.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tv, "TEXT_MINI");
        tv.setPadding(0, 0, scale(35), 0);
        return tv;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isReplaying = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isReplaying = false;
        if (replayAvatar != null) {
            replayAvatar.remove();
            replayAvatar = null;
        }
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
            // ≡ƒÜÇ QA Rule: Contextual Anchor (Owner Location)
            new Thread(() -> {
                com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
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
                // Fallback to Headquarters/Dynamic default if admin location is unknown
                runOnUiThread(() -> {
                    double defLat = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultLatitude(this);
                    double defLng = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultLongitude(this);
                    this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(defLat, defLng), 12f));
                });
            }).start();
        }
    }

    @Override
    protected void onSaveCurrentState(android.os.Bundle outState) {
        if (outState == null) return;
        outState.putString("SAVED_TARGET_UID", currentTargetUid);
        outState.putString("SAVED_TARGET_NAME", currentTargetName);
        outState.putInt("SAVED_TIME_INDEX", selectedTimeIndex);
        outState.putBoolean("SAVED_IS_REPLAYING", isReplaying);
    }
}
