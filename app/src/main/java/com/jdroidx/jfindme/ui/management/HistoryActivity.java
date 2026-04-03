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
        
        ImageView helpIcon = new ImageView(this);
        helpIcon.setImageResource(android.R.drawable.ic_menu_help);
        helpIcon.setColorFilter(getGradientOverlayText());
        int helpSize = scale(60);
        helpIcon.setPadding(scale(10), hTop, scale(10), 0);
        helpIcon.setOnClickListener(v -> triggerHelpDialog());
        titleRow.addView(helpIcon, new LinearLayout.LayoutParams(helpSize + scale(20), helpSize + hTop));
        
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
        content.setPadding(hPad * 2, hPad * 2, hPad * 2, hPad * 5);

        // --- LAYER 1: MAP ENGINE ---
        LinearLayout mapCard = new LinearLayout(this);
        mapCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, mapCard, "CARD_STANDARD", 1.2f);
        
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
        mapCard.addView(mapContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mapHeight));
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, hPad * 2);
        content.addView(mapCard, cardLp);

        // --- LAYER 1.5: DISTANCE CARD ---
        LinearLayout distanceCard = new LinearLayout(this);
        distanceCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, distanceCard, "CARD_STANDARD", 1.0f);
        
        tvDistance = new TextView(this);
        tvDistance.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvDistance, "TEXT_BODY");
        tvDistance.setText("TOTAL DISTANCE: AWAITING LOAD ENGINE");
        tvDistance.setTypeface(null, Typeface.BOLD);
        tvDistance.setGravity(Gravity.CENTER);
        distanceCard.addView(tvDistance);
        
        content.addView(distanceCard, cardLp);

        // --- LAYER 2: REPLAY ENGINE CONTROLS ---
        LinearLayout replayCard = new LinearLayout(this);
        replayCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, replayCard, "CARD_STANDARD", 1.0f);
        
        LinearLayout replayButtons = new LinearLayout(this);
        replayButtons.setOrientation(LinearLayout.HORIZONTAL);
        int rPad = getResponsivePaddingQuota();
        replayButtons.setPadding(rPad, rPad, rPad, 0);
        
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        
        View playBtn = createReplayButton("START\nREPLAY", android.R.drawable.ic_media_play);
        playBtn.setOnClickListener(v -> startPathReplay());
        
        View stopBtn = createReplayButton("STOP\nREPLAY", android.R.drawable.ic_media_pause);
        stopBtn.setOnClickListener(v -> { isReplaying = false; Toast.makeText(this, "Replay Terminated", Toast.LENGTH_SHORT).show(); });

        replayButtons.addView(wrapInRow(playBtn), btnLp);
        replayButtons.addView(wrapInRow(stopBtn), btnLp);
        replayCard.addView(replayButtons);

        tvWaypointsCount = new TextView(this);
        tvWaypointsCount.setText("0 WAYPOINTS LOADED SECURELY");
        tvWaypointsCount.setTextColor(getSecondaryTextColor());
        tvWaypointsCount.setGravity(Gravity.CENTER);
        UiArchitect.applyText(this, tvWaypointsCount, "TEXT_MINI");
        tvWaypointsCount.setPadding(0, scale(20), 0, rPad);
        replayCard.addView(tvWaypointsCount);
        
        content.addView(replayCard, cardLp);

        // --- LAYER 3: TARGET SELECTOR ---
        content.addView(createSectionHeader("TARGET SELECTOR"));
        LinearLayout targetCard = new LinearLayout(this);
        targetCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, targetCard, "CARD_STANDARD", 1.0f);
        targetCard.addView(createHistoryControls());
        content.addView(targetCard, cardLp);

        // --- LAYER 3: DETAILED LOGS ---
        content.addView(createSectionHeader("DETAILED LOCATION LOGS"));
        logsContainer = new LinearLayout(this);
        logsContainer.setOrientation(LinearLayout.VERTICAL);
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
        // 🧱 L7 Aesthetic Sync: Matches Media (today 12:01pm) subtle capsule style
        btn.setBackground(getBorderDrawable(android.graphics.Color.parseColor("#E8E8E8"), 2, 60));
        int p = scale(25);
        btn.setPadding(p, p, p, p);
        
        ImageView icon = new ImageView(this);
        icon.setImageResource(resId);
        icon.setColorFilter(android.graphics.Color.RED); 
        int s = scale(55);
        btn.addView(icon, new LinearLayout.LayoutParams(s, s));
        
        TextView tv = new TextView(this);
        tv.setText(text.toUpperCase());
        tv.setTextColor(android.graphics.Color.BLACK); 
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, scaleText(28));
        tv.setPadding(scale(20), 0, 0, 0);
        btn.addView(tv);
        
        return btn;
    }

    private View createTimeTile(String text, int iconRes, int index) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        int p = scale(25);
        tile.setPadding(p, p / 2, p, p / 2);
        tile.setBackground(getBorderDrawable(getGradientStart(), 3, 30));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart());
        tile.addView(icon, new LinearLayout.LayoutParams(scale(45), scale(45)));

        TextView tv = new TextView(this);
        tv.setText(text);
        UiArchitect.applyText(this, tv, "TEXT_MINI");
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(getTextColor());
        tv.setPadding(scale(15), 0, 0, 0);
        tile.addView(tv);

        tile.setOnClickListener(v -> {
            loadHistoryData(DURATIONS[index]);
            updateTimeTileStyles(index);
        });

        timeButtons.add(tile);
        return tile;
    }

    private void updateTimeTileStyles(int selectedIndex) {
        for (int i = 0; i < timeButtons.size(); i++) {
            boolean selected = (i == selectedIndex);
            View v = timeButtons.get(i);
            if (v instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) v;
                layout.setBackground(selected ? getSolidDrawable(getGradientStart(), 30) : getBorderDrawable(getGradientStart(), 3, 30));

                for (int j = 0; j < layout.getChildCount(); j++) {
                    View child = layout.getChildAt(j);
                    if (child instanceof ImageView) {
                        ((ImageView) child).setColorFilter(selected ? Color.WHITE : getGradientStart());
                    } else if (child instanceof TextView) {
                        ((TextView) child).setTextColor(selected ? Color.WHITE : getTextColor());
                    }
                }
            }
        }
    }

    private View createHistoryControls() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int conPad = getResponsivePaddingQuota();
        container.setPadding(conPad, conPad, conPad, conPad);

        tvTrackLabel = new TextView(this);
        updateTrackLabel();
        tvTrackLabel.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvTrackLabel, "TEXT_MINI");
        tvTrackLabel.setTypeface(null, Typeface.BOLD);
        tvTrackLabel.setPadding(0, 0, 0, scale(20));
        container.addView(tvTrackLabel);

        View selectTargetBtn = createIconButton("SELECT TRACKING TARGET", android.R.drawable.ic_menu_search, v -> {
            showTargetSelector();
        });
        container.addView(selectTargetBtn);

        // Time Selection Grid (2x2)
        android.widget.GridLayout timeGrid = new android.widget.GridLayout(this);
        timeGrid.setColumnCount(2);
        timeGrid.setPadding(0, scale(20), 0, scale(20));

        timeButtons.clear();
        View btn4h = createTimeTile("4 HRS", android.R.drawable.ic_menu_recent_history, 0);
        View btn12h = createTimeTile("12 HRS", android.R.drawable.ic_menu_recent_history, 1);
        View btn24h = createTimeTile("24 HRS", android.R.drawable.ic_menu_recent_history, 2);
        View btn7d = createTimeTile("7 DAYS", android.R.drawable.ic_menu_today, 3);

        timeGrid.addView(wrapInGrid(btn4h));
        timeGrid.addView(wrapInGrid(btn12h));
        timeGrid.addView(wrapInGrid(btn24h));
        timeGrid.addView(wrapInGrid(btn7d));
        container.addView(timeGrid);

        tvStatus = new TextView(this);
        tvStatus.setText("READY FOR SYNC");
        tvStatus.setTextColor(getSecondaryTextColor()); 
        tvStatus.setGravity(Gravity.CENTER);
        UiArchitect.applyText(this, tvStatus, "TEXT_MINI");
        tvStatus.setPadding(0, scale(10), 0, 0);
        container.addView(tvStatus);

        return container;
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
        if (currentTargetUid == null) {
            currentTargetUid = FirebaseAuth.getInstance().getUid();
        }
        
        if (currentTargetUid == null) {
            Toast.makeText(this, "Authentication Required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (viewModel != null) {
            viewModel.loadHistoryData(currentTargetUid, durationMs);
            viewModel.loadHistorySessions(this, currentTargetUid);
        }
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
                
        animateReplay(0);
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
                if (isReplaying) animateReplay(index + 1);
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
                updateHistoryTable(sessions);
            }
        });

        viewModel.isLoading().observe(this, loading -> {
            if (tvStatus != null) {
                tvStatus.setText(loading ? "SYNCING CLOUD DATA..." : "READY FOR SYNC");
            }
        });

        viewModel.fetchTrackingTargets(this);
        
        // --- 🚀 Intent Linkage Protocol ---
        currentTargetUid = getIntent().getStringExtra("TARGET_UID");
        currentTargetName = getIntent().getStringExtra("TARGET_NAME");
        
        if (currentTargetUid == null) {
            currentTargetUid = FirebaseAuth.getInstance().getUid();
            currentTargetName = "MY DEVICE (ADMIN)";
        }

        viewModel.loadHistorySessions(this, currentTargetUid);
        loadHistoryData(DURATIONS[0]); // Default: 4h
        updateTimeTileStyles(0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_FRAGMENT);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(ID_MAP_FRAGMENT, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }


    private void updateTrackLabel() {
        if (tvTrackLabel != null) {
            tvTrackLabel.setText("CURRENT TRACK: " + (currentTargetName != null ? currentTargetName.toUpperCase() : "UNKNOWN"));
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
        UiArchitect.applyCardWithOverride(this, outer, "CARD_STANDARD", 1.0f);
        int margin = scale(15);
        LinearLayout.LayoutParams outerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        outerLp.setMargins(0, 0, 0, margin);
        outer.setLayoutParams(outerLp);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        int p = scale(25);
        row1.setPadding(p, p, p, p);

        // Icon
        ImageView icClock = new ImageView(this);
        icClock.setImageResource(android.R.drawable.ic_menu_recent_history);
        icClock.setColorFilter(getGradientStart());
        row1.addView(icClock, new LinearLayout.LayoutParams(scale(40), scale(40)));

        // Session Label
        TextView tvLabel = new TextView(this);
        Object pingObj = session.get("ping_count");
        int pings = (pingObj instanceof Integer) ? (Integer) pingObj : 0;
        
        Object fromObj = session.get("from_datetime");
        long from = (fromObj instanceof Long) ? (Long) fromObj : 0L;
        
        Object toObj = session.get("to_datetime");
        long to = (toObj instanceof Long) ? (Long) toObj : 0L;
        
        String dateStr = new java.text.SimpleDateFormat("MMM dd, yyyy | hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(from));
        String toStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(to));
        
        tvLabel.setText("SESSION (" + pings + " PINGS) • " + dateStr + " TO " + toStr);
        tvLabel.setTextColor(Color.RED);
        tvLabel.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvLabel, "TEXT_MINI");
        tvLabel.setPadding(scale(20), 0, 0, 0);
        row1.addView(tvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Right Target Icon
        ImageView icTarget = new ImageView(this);
        icTarget.setImageResource(android.R.drawable.ic_menu_mylocation);
        icTarget.setColorFilter(Color.RED);
        row1.addView(icTarget, new LinearLayout.LayoutParams(scale(50), scale(50)));

        outer.addView(row1);

        // Address
        TextView tvAddr = new TextView(this);
        tvAddr.setText(session.get("address").toString());
        tvAddr.setTextColor(getGradientStart());
        tvAddr.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvAddr, "TEXT_MINI");
        tvAddr.setPadding(p + scale(60), 0, p, scale(10));
        outer.addView(tvAddr);

        // Stats Row
        LinearLayout rowStats = new LinearLayout(this);
        rowStats.setOrientation(LinearLayout.HORIZONTAL);
        rowStats.setPadding(p + scale(60), 0, p, p);
        
        rowStats.addView(createMiniStat("🚗", session.get("speed").toString() + " km/h"));
        rowStats.addView(createMiniStat("🔋", "N/A"));
        rowStats.addView(createMiniStat("📍", pings + " Pings"));
        
        outer.addView(rowStats);

        outer.setOnClickListener(v -> {
            if (googleMap != null) {
                LatLng pos = new LatLng((double)session.get("lat"), (double)session.get("lng"));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
                Toast.makeText(this, "Inspecting Node: " + pings + " Pings at this Radius", Toast.LENGTH_SHORT).show();
            }
        });

        return outer;
    }

    private View createMiniStat(String icon, String text) {
        TextView tv = new TextView(this);
        tv.setText(icon + " " + text);
        tv.setTextColor(Color.parseColor("#2E7D32")); // Dark Green 
        tv.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tv, "TEXT_MINI");
        tv.setPadding(0, 0, scale(30), 0);
        return tv;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        LatLng pune = new LatLng(18.5204, 73.8567);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pune, 12f));
    }
}
