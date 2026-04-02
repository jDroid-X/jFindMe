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
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.engine.data.SpatialProcessor;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main -> UI Engine -> ManagementCluster -> HistoryActivity (L4)
 * Hardening: L7 Architectural Registry Parity & V36 Database Migration.
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
    private TextView tvSyncMode; 
    private TextView tvLastUpdated; 
    private TextView targetLabel;
    private LinearLayout locationLogsContainer;
    
    private View btn4h, btn12h, btn24h, btn7days;
    private EditText etMaxDays; 
    
    private boolean isReplaying = false;
    private ValueAnimator currentAnimator;
    private ScrollView mainScrollView; 
    
    private String activeTargetUid = "";
    private String activeTargetName = "You";

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("LOCATION LOGS");
        tvTitle.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, tvTitle, "TEXT_HEADER");
        int hTop = getResponsiveHeaderTopQuota();
        tvTitle.setPadding(0, hTop, 0, 0);
        header.addView(tvTitle);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Historical GPS Breadcrumbs");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        subtitle.setPadding(0, scale(10), 0, 0);
        header.addView(subtitle);
        
        tvSyncMode = new TextView(this);
        tvSyncMode.setText("OFFLINE SESSION: LOCAL HISTORY");
        tvSyncMode.setTextColor(Color.WHITE);
        tvSyncMode.setBackgroundColor(Color.parseColor("#44FF0000"));
        int spPad = scale(20);
        tvSyncMode.setPadding(spPad, scale(5), spPad, scale(5));
        UiArchitect.applyText(this, tvSyncMode, "TEXT_MINI");
        tvSyncMode.setTypeface(null, Typeface.BOLD);
        tvSyncMode.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams modeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        modeLp.topMargin = scale(15);
        header.addView(tvSyncMode, modeLp);

        root.addView(header);

        // Control Surface
        mainScrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int hPad = getResponsivePaddingQuota();
        content.setPadding(hPad * 2, hPad * 2, hPad * 2, hPad * 5);
        
        // Purge Policy Tile
        LinearLayout purgeCard = new LinearLayout(this);
        purgeCard.setOrientation(LinearLayout.HORIZONTAL);
        purgeCard.setGravity(Gravity.CENTER_VERTICAL);
        UiArchitect.applyCardWithOverride(this, purgeCard, "CARD_STANDARD", 1.0f);
        
        ImageView purgeIcon = new ImageView(this);
        purgeIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
        purgeIcon.setColorFilter(getGradientStart());
        int piSize = scale(60);
        purgeCard.addView(purgeIcon, new LinearLayout.LayoutParams(piSize, piSize));
        
        LinearLayout purgeTextWrapper = new LinearLayout(this);
        purgeTextWrapper.setOrientation(LinearLayout.VERTICAL);
        purgeTextWrapper.setPadding(scale(30), 0, 0, 0);
        
        TextView tvPurgeTitle = new TextView(this);
        tvPurgeTitle.setText("MAX LOCAL HISTORY SAVED");
        tvPurgeTitle.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvPurgeTitle, "TEXT_LABEL");
        tvPurgeTitle.setTypeface(null, Typeface.BOLD);
        purgeTextWrapper.addView(tvPurgeTitle);
        
        TextView tvPurgeSub = new TextView(this);
        tvPurgeSub.setText("Auto-purge logs older than:");
        tvPurgeSub.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvPurgeSub, "TEXT_MINI");
        purgeTextWrapper.addView(tvPurgeSub);
        purgeCard.addView(purgeTextWrapper, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        etMaxDays = UiArchitect.createInputField(this, "MAX_HISTORY_DAYS");
        etMaxDays.setText("7");
        etMaxDays.setGravity(Gravity.END);
        etMaxDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        int inputWidth = scale(100);
        purgeCard.addView(etMaxDays, new LinearLayout.LayoutParams(inputWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        TextView tvDaysSuffix = new TextView(this);
        tvDaysSuffix.setText(" DAYS");
        tvDaysSuffix.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvDaysSuffix, "TEXT_LABEL");
        purgeCard.addView(tvDaysSuffix);
        
        LinearLayout.LayoutParams purgeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        purgeLp.setMargins(0, 0, 0, hPad * 2);
        content.addView(purgeCard, purgeLp);

        // Map Card
        LinearLayout mapCard = new LinearLayout(this);
        mapCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, mapCard, "CARD_STANDARD", 1.2f);
        
        FrameLayout mapContainer = new FrameLayout(this);
        mapContainer.setId(ID_MAP_FRAGMENT);
        
        FrameLayout gestureMembrane = new FrameLayout(this);
        gestureMembrane.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) mainScrollView.requestDisallowInterceptTouchEvent(true);
            else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) mainScrollView.requestDisallowInterceptTouchEvent(false);
            return false;
        });
        
        mapContainer.addView(gestureMembrane, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int mapHeight = scale(600);
        mapCard.addView(mapContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mapHeight));
        
        tvDistance = new TextView(this);
        tvDistance.setTextColor(getTextColor());
        UiArchitect.applyText(this, tvDistance, "TEXT_BODY");
        tvDistance.setText("TOTAL DISTANCE: AWAITING LOAD ENGINE");
        tvDistance.setPadding(hPad * 2, hPad, hPad * 2, hPad);
        tvDistance.setTypeface(null, Typeface.BOLD);
        mapCard.addView(tvDistance);
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, hPad * 2);
        content.addView(mapCard, cardLp);

        // Controls
        content.addView(createSectionHeader("REPLAY ENGINE CONTROLS"));
        
        LinearLayout controlsCard = new LinearLayout(this);
        controlsCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, controlsCard, "CARD_STANDARD", 1.0f);
        controlsCard.addView(createHistoryControls());
        content.addView(controlsCard, cardLp);

        // Logs
        content.addView(createSectionHeader("DETAILED LOCATION LOGS"));
        
        tvLastUpdated = new TextView(this);
        tvLastUpdated.setText("LOG ENGINE STANDBY • NO DATA SYNCED");
        tvLastUpdated.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvLastUpdated, "TEXT_MINI");
        tvLastUpdated.setPadding(scale(10), 0, 0, scale(20));
        content.addView(tvLastUpdated);

        locationLogsContainer = new LinearLayout(this);
        locationLogsContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(locationLogsContainer, cardLp);
        
        mainScrollView.addView(content);
        root.addView(mainScrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return root;
    }

    private View createSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tv, "TEXT_TITLE");
        int sPad = getResponsivePaddingQuota();
        tv.setPadding(sPad / 2, 0, 0, sPad);
        return tv;
    }

    private View createHistoryControls() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int conPad = getResponsivePaddingQuota() * 2;
        container.setPadding(conPad / 2, conPad, conPad / 2, conPad);

        targetLabel = new TextView(this);
        targetLabel.setText("CURRENT TRACK: " + activeTargetName.toUpperCase());
        targetLabel.setTextColor(getGradientStart());
        UiArchitect.applyText(this, targetLabel, "TEXT_BODY");
        targetLabel.setPadding(0, 0, 0, scale(20));
        container.addView(targetLabel);

        LinearLayout targetRow = new LinearLayout(this);
        targetRow.setOrientation(LinearLayout.HORIZONTAL);
        targetRow.setGravity(Gravity.CENTER_VERTICAL);
        
        btn4h = UiArchitect.createButton(this, "4 HRS", v -> selectTimeWindow(btn4h, 4 * 60 * 60 * 1000L));
        btn12h = UiArchitect.createButton(this, "12 HRS", v -> selectTimeWindow(btn12h, 12 * 60 * 60 * 1000L));
        btn24h = UiArchitect.createButton(this, "24 HRS", v -> selectTimeWindow(btn24h, 24 * 60 * 60 * 1000L));
        btn7days = UiArchitect.createButton(this, "7 DAYS", v -> selectTimeWindow(btn7days, 7L * 24L * 60L * 60L * 1000L));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        int m = scale(10);
        btnLp.setMargins(m, m, m, m);
        row1.addView(btn4h, btnLp); row1.addView(btn12h, btnLp);
        container.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(btn24h, btnLp); row2.addView(btn7days, btnLp);
        container.addView(row2);

        LinearLayout dualControls = new LinearLayout(this);
        dualControls.setOrientation(LinearLayout.HORIZONTAL);
        View playBtn = UiArchitect.createButton(this, "START REPLAY", v -> { isReplaying = true; startPathReplay(); });
        View stopBtn = UiArchitect.createButton(this, "STOP REPLAY", v -> { isReplaying = false; if (currentAnimator != null) currentAnimator.cancel(); });
        dualControls.addView(playBtn, btnLp); dualControls.addView(stopBtn, btnLp);
        container.addView(dualControls);

        tvStatus = new TextView(this);
        tvStatus.setText("READY FOR SYNC");
        tvStatus.setTextColor(getSecondaryTextColor()); 
        tvStatus.setGravity(Gravity.CENTER);
        UiArchitect.applyText(this, tvStatus, "TEXT_MINI");
        tvStatus.setPadding(0, scale(20), 0, 0);
        container.addView(tvStatus);

        return container;
    }

    private void selectTimeWindow(View b, long t) {
        loadHistoryData(t);
    }

    private void loadHistoryData(long t) {
        if (viewModel != null) viewModel.loadHistoryData(activeTargetUid, t);
    }

    private void calculateAnalytics(List<UserLocation> p) {
        double dist = 0;
        for (int i = 0; i < p.size() - 1; i++) {
            double d = SpatialProcessor.getDistance(p.get(i).getLatitude(), p.get(i).getLongitude(), p.get(i+1).getLatitude(), p.get(i+1).getLongitude());
            if (d > 15.0) dist += d;
        }
        if (tvDistance != null) tvDistance.setText(String.format("TOTAL DISTANCE: %.2f km", dist / 1000));
    }

    private void renderTextualLogs(List<UserLocation> points) {
        if (locationLogsContainer == null) return;
        locationLogsContainer.removeAllViews();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm a", java.util.Locale.getDefault());

        for (int i = points.size() - 1; i >= 0; i--) {
            UserLocation p = points.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int cp = scale(35);
            card.setPadding(cp, cp, cp, cp);
            UiArchitect.applyCardWithOverride(this, card, "CARD_STANDARD", 1.0f);

            TextView tvTime = new TextView(this);
            tvTime.setText("⏱️ LOG • " + sdf.format(new java.util.Date(p.getTimestamp())));
            tvTime.setTextColor(getGradientStart());
            UiArchitect.applyText(this, tvTime, "TEXT_MINI");
            card.addView(tvTime);

            TextView tvCoords = new TextView(this);
            tvCoords.setText(String.format("📍 %.6f, %.6f", p.getLatitude(), p.getLongitude()));
            tvCoords.setTextColor(getTextColor());
            UiArchitect.applyText(this, tvCoords, "TEXT_BODY");
            card.addView(tvCoords);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, scale(20));
            locationLogsContainer.addView(card, lp);
        }
    }

    private void drawStaticPath(List<UserLocation> p) {
        if (googleMap == null) return;
        googleMap.clear();
        PolylineOptions opt = new PolylineOptions().color(getGradientStart()).width(10);
        for (UserLocation loc : p) opt.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        googleMap.addPolyline(opt);
        if (!p.isEmpty()) googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(p.get(0).getLatitude(), p.get(0).getLongitude()), 15f));
    }

    private void startPathReplay() {
        if (historyPoints.size() < 2 || googleMap == null) return;
        animateReplay(0);
    }

    private void animateReplay(final int index) {
        if (!isReplaying || index >= historyPoints.size() - 1) return;
        UserLocation start = historyPoints.get(index);
        UserLocation end = historyPoints.get(index + 1);
        currentAnimator = ValueAnimator.ofFloat(0, 1);
        currentAnimator.setDuration(1000);
        currentAnimator.addUpdateListener(a -> {
            float v = a.getAnimatedFraction();
            double lat = v * end.getLatitude() + (1 - v) * start.getLatitude();
            double lng = v * end.getLongitude() + (1 - v) * start.getLongitude();
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
        });
        currentAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator a) { if (isReplaying) animateReplay(index + 1); }
        });
        currentAnimator.start();
    }

    @Override
    protected void setupLogic() {
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(HistoryViewModel.class);
        viewModel.init(this);
        viewModel.getHistoryPoints().observe(this, p -> { if (p != null) { historyPoints = p; drawStaticPath(p); calculateAnalytics(p); renderTextualLogs(p); } });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_FRAGMENT);
        if (mapFragment == null) { mapFragment = SupportMapFragment.newInstance(); getSupportFragmentManager().beginTransaction().add(ID_MAP_FRAGMENT, mapFragment).commit(); }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
    }
}
