package com.jdroidx.jfindme.ui.management;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
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
    private LocationRepository repository;
    private GoogleMap googleMap;
    private Marker replayAvatar;
    private Polyline replayPath;
    private List<UserLocation> historyPoints = new ArrayList<>();
    private static final int ID_MAP_FRAGMENT = 2001;
    
    private TextView tvDistance;

    @Override
    protected View createLayout() {
        return buildPortraitLayout(); // Unified Dashboard Architecture organic to all Device Rotations
    }

    private View buildPortraitLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // 🚀 Premium Linear Gradient Hero Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(50, 80, 50, 60);
        applyLinearGradient(header, 0f);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("LOCATION LOGS");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Historical GPS Breadcrumbs & Action Replay");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 10, 0, 0);
        header.addView(subtitle);

        root.addView(header);

        // Scrollable Control Surface Engine
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(40, 40, 40, 100);

        // --- LAYER 1: FLOATING MAP CARD ---
        LinearLayout mapCard = new LinearLayout(this);
        mapCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(mapCard, getCardColor(), 40f);
        
        FrameLayout mapContainer = new FrameLayout(this);
        mapContainer.setId(ID_MAP_FRAGMENT);
        mapCard.addView(mapContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800));
        
        tvDistance = new TextView(this);
        tvDistance.setTextColor(getTextColor());
        tvDistance.setTextSize(14);
        tvDistance.setText("TOTAL DISTANCE: AWAITING LOAD ENGINE");
        tvDistance.setPadding(40, 30, 40, 30);
        tvDistance.setTypeface(null, Typeface.BOLD);
        mapCard.addView(tvDistance);
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 40);
        content.addView(mapCard, cardLp);

        // --- LAYER 2: CONTROLS ---
        content.addView(createSectionHeader("REPLAY ENGINE CONTROLS"));
        
        LinearLayout controlsCard = new LinearLayout(this);
        controlsCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(controlsCard, getCardColor(), 40f);
        controlsCard.addView(createHistoryControls());
        content.addView(controlsCard, cardLp);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

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
        tv.setPadding(10, 0, 0, 20);
        return tv;
    }

    private View createHistoryControls() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 40, 30, 40);

        container.addView(createIconButton("LOAD LAST 24 HOURS", android.R.drawable.ic_menu_recent_history, v -> {
            loadHistoryData();
        }));

        container.addView(createIconButton("START REPLAY", android.R.drawable.ic_media_play, v -> {
            startPathReplay();
        }));

        TextView tvStatus = new TextView(this);
        tvStatus.setText("READY FOR SYNC");
        tvStatus.setTextColor(getSecondaryTextColor()); // Exact Map Target Line
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextSize(11f);
        tvStatus.setPadding(0, 40, 0, 0);
        container.addView(tvStatus);

        return container;
    }

    private void loadHistoryData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        long now = System.currentTimeMillis();
        long dayAgo = now - (24 * 60 * 60 * 1000);

        repository.fetchHistory(uid, dayAgo, now, points -> {
            if (points.isEmpty()) {
                Toast.makeText(this, "No history found for this period", Toast.LENGTH_SHORT).show();
            } else {
                historyPoints = points;
                drawStaticPath(points);
                calculateAnalytics(points);
                Toast.makeText(this, "Loaded " + points.size() + " waypoints", Toast.LENGTH_SHORT).show();
            }
            return Unit.INSTANCE;
        });
    }

    private void calculateAnalytics(List<UserLocation> points) {
        // Integrate with Python Data Engine
        List<Map<String, Object>> pathList = new ArrayList<>();
        for (UserLocation p : points) {
            Map<String, Object> map = new HashMap<>();
            map.put("latitude", p.getLatitude());
            map.put("longitude", p.getLongitude());
            map.put("timestamp", p.getTimestamp());
            pathList.add(map);
        }
        
        // This is a placeholder for the actual total distance calculation if added to data_processor.py
        // Currently data_processor has calculate_distance and predict_next_point
        // Let's use the calculate_distance bridge to sum up
        double totalDist = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            totalDist += SpatialProcessor.getDistance(
                points.get(i).getLatitude(), points.get(i).getLongitude(),
                points.get(i+1).getLatitude(), points.get(i+1).getLongitude()
            );
        }
        
        if (tvDistance != null) {
            String text = totalDist > 1000 ? 
                String.format("TOTAL DISTANCE: %.2f km", totalDist / 1000) : 
                String.format("TOTAL DISTANCE: %.0f m", totalDist);
            tvDistance.setText(text);
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
        replayAvatar = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(first.getLatitude(), first.getLongitude()))
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        
        animateReplay(0);
    }

    private void animateReplay(final int index) {
        if (index >= historyPoints.size() - 1) {
            Toast.makeText(this, "Replay Completed", Toast.LENGTH_SHORT).show();
            return;
        }

        final UserLocation start = historyPoints.get(index);
        final UserLocation end = historyPoints.get(index + 1);
        final LatLng startLatLng = new LatLng(start.getLatitude(), start.getLongitude());
        final LatLng endLatLng = new LatLng(end.getLatitude(), end.getLongitude());

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(500); // 500ms between each historical point
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            double lat = v * endLatLng.latitude + (1 - v) * startLatLng.latitude;
            double lng = v * endLatLng.longitude + (1 - v) * startLatLng.longitude;
            LatLng current = new LatLng(lat, lng);
            if (replayAvatar != null) replayAvatar.setPosition(current);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(current));
        });
        
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                animateReplay(index + 1);
            }
        });
        animator.start();
    }

    @Override
    protected void setupLogic() {
        repository = new LocationRepository(this);
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
