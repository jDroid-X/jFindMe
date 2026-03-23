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
    private TextView tvStatus;
    private LinearLayout locationLogsContainer;
    
    // QA Flag: Engine Termination State
    private boolean isReplaying = false;
    private ValueAnimator currentAnimator;
    private ScrollView mainScrollView; // Global Reference for Smooth Scrolling Physics
    
    // QA Engine: Dynamic Target Selector
    private String activeTargetUid = "";
    private String activeTargetName = "My Device (Admin)";

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
        mainScrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(40, 40, 40, 100);

        // --- LAYER 1: FLOATING MAP CARD ---
        LinearLayout mapCard = new LinearLayout(this);
        mapCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(mapCard, getCardColor(), 40f);
        
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

        // --- LAYER 3: DETAILED TEXTUAL LOGS ---
        content.addView(createSectionHeader("DETAILED LOCATION LOGS"));
        
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
        tv.setPadding(10, 0, 0, 20);
        return tv;
    }

    private View createHistoryControls() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 40, 30, 40);

        // --- QA TARGET SELECTOR ENGINE ---
        TextView targetLabel = new TextView(this);
        targetLabel.setText("CURRENT TRACK: " + activeTargetName.toUpperCase());
        targetLabel.setTextColor(getGradientStart());
        targetLabel.setTextSize(12f);
        targetLabel.setTypeface(null, Typeface.BOLD);
        targetLabel.setPadding(0, 0, 0, 20);
        container.addView(targetLabel);

        container.addView(createIconButton("SELECT TRACKING TARGET", android.R.drawable.ic_menu_search, v -> {
            // 🛑 QA Phase 3 Privacy Lockdown: Role Matrix Authority Engine
            android.content.SharedPreferences prefs = getSharedPreferences("jFindMe_Prefs", android.content.Context.MODE_PRIVATE);
            boolean isGuardian = prefs.getBoolean("role_mother", false) || 
                                 prefs.getBoolean("role_father", false) || 
                                 prefs.getBoolean("role_spouse", false);

            if (tvStatus != null) tvStatus.setText("Scanning Network for Assets...");
            com.jdroidx.jfindme.data.remote.FirebaseComm.INSTANCE.getLocationsRef().get().addOnSuccessListener(snapshot -> {
                java.util.List<String> names = new java.util.ArrayList<>();
                java.util.List<String> uids = new java.util.ArrayList<>();
                
                names.add("My Device (Admin)");
                uids.add(FirebaseAuth.getInstance().getUid());
                
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    UserLocation loc = child.getValue(UserLocation.class);
                    if (loc != null && !loc.getUserId().equals(FirebaseAuth.getInstance().getUid())) {
                        // 🟢 QA Dynamic Privacy Filter: Display target if User is Guardian OR Target explicitly permits dependent tracking!
                        if (isGuardian || loc.getAllowDependents()) {
                            names.add(loc.getUserName() + " (Linked)");
                            uids.add(loc.getUserId());
                        }
                    }
                }
                
                String[] items = names.toArray(new String[0]);
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Focus New Target")
                    .setItems(items, (dialog, which) -> {
                        activeTargetUid = uids.get(which);
                        activeTargetName = names.get(which);
                        targetLabel.setText("CURRENT TRACK: " + activeTargetName.toUpperCase());
                        Toast.makeText(this, "Target locked to: " + activeTargetName, Toast.LENGTH_SHORT).show();
                    }).show();
            });
        }));

        // QA Advanced Filter Geometry: 2x2 Grid Architecture for Flawless Margins!
        LinearLayout timeSelectorsRow1 = new LinearLayout(this);
        timeSelectorsRow1.setOrientation(LinearLayout.HORIZONTAL);
        timeSelectorsRow1.setPadding(0, 0, 0, 15);

        LinearLayout timeSelectorsRow2 = new LinearLayout(this);
        timeSelectorsRow2.setOrientation(LinearLayout.HORIZONTAL);
        timeSelectorsRow2.setPadding(0, 0, 0, 25);

        View btn4h = createIconButton("4 HRS", android.R.drawable.ic_menu_recent_history, v -> {
            loadHistoryData(4 * 60 * 60 * 1000L); // 4 Hours
        });
        View btn12h = createIconButton("12 HRS", android.R.drawable.ic_menu_recent_history, v -> {
            loadHistoryData(12 * 60 * 60 * 1000L); // 12 Hours
        });
        View btn24h = createIconButton("24 HRS", android.R.drawable.ic_menu_recent_history, v -> {
            loadHistoryData(24 * 60 * 60 * 1000L); // 24 Hours
        });
        // 🚀 QA Fix: Deep Archive Override allows extracting legacy data lost behind the 24H threshold!
        View btn7days = createIconButton("7 DAYS", android.R.drawable.ic_menu_agenda, v -> {
            loadHistoryData(7L * 24L * 60L * 60L * 1000L); // 7 Absolute Days
        });

        // Strict Bounding Box Constraint: Forces exact margins to prevent text/icon overlap crushing!
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnLp.setMargins(15, 10, 15, 10);

        timeSelectorsRow1.addView(btn4h, btnLp);
        timeSelectorsRow1.addView(btn12h, btnLp);
        timeSelectorsRow2.addView(btn24h, btnLp);
        timeSelectorsRow2.addView(btn7days, btnLp);
        
        container.addView(timeSelectorsRow1);
        container.addView(timeSelectorsRow2);

        // PLAY / STOP ENGINE BLOCK
        LinearLayout dualControls = new LinearLayout(this);
        dualControls.setOrientation(LinearLayout.HORIZONTAL);
        dualControls.setPadding(0, 15, 0, 0);

        View playBtn = createIconButton("START REPLAY", android.R.drawable.ic_media_play, v -> {
            isReplaying = true;
            startPathReplay();
        });
        dualControls.addView(playBtn, btnLp); // Shared Margin Physics!

        View stopBtn = createIconButton("STOP REPLAY", android.R.drawable.ic_media_pause, v -> {
            isReplaying = false; // Kill Recursive Animators
            if (currentAnimator != null) currentAnimator.cancel();
            if (tvStatus != null) tvStatus.setText("REPLAY TERMINATED");
        });
        dualControls.addView(stopBtn, btnLp); // Shared Margin Physics!

        container.addView(dualControls);

        tvStatus = new TextView(this);
        tvStatus.setText("READY FOR SYNC");
        tvStatus.setTextColor(getSecondaryTextColor()); // Exact Map Target Line
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextSize(11f);
        tvStatus.setPadding(0, 40, 0, 0);
        container.addView(tvStatus);

        return container;
    }

    private void loadHistoryData(long timeWindow) {
        if (activeTargetUid == null || activeTargetUid.isEmpty()) {
            activeTargetUid = FirebaseAuth.getInstance().getUid();
        }
        String uid = activeTargetUid;
        if (uid == null) return;

        long now = System.currentTimeMillis();
        long targetPast = now - timeWindow;
        if (tvStatus != null) tvStatus.setText("FETCHING CLOUD METRICS...");
        
        // 🚀 QA Fix: Instantly render native OS indeterminate spinning wheel into the DOM
        if (locationLogsContainer != null) {
            locationLogsContainer.removeAllViews();
            android.widget.ProgressBar pb = new android.widget.ProgressBar(this);
            pb.setIndeterminate(true);
            pb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            pb.setPadding(0, 100, 0, 100);
            locationLogsContainer.addView(pb);
        }

        repository.fetchHistory(uid, targetPast, now, points -> {
            if (points.isEmpty()) {
                if (locationLogsContainer != null) locationLogsContainer.removeAllViews(); // Kill Spinner
                if (tvStatus != null) tvStatus.setText("0 METRICS FOUND");
                Toast.makeText(this, "No history found for this period", Toast.LENGTH_SHORT).show();
            } else {
                historyPoints = points;
                drawStaticPath(points);
                calculateAnalytics(points);
                renderTextualLogs(points); // Generate physical text database
                if (tvStatus != null) tvStatus.setText(points.size() + " WAYPOINTS LOADED SECURELY");
                Toast.makeText(this, "Loaded " + points.size() + " waypoints", Toast.LENGTH_SHORT).show();
            }
            return Unit.INSTANCE;
        });
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
            logCard.setOrientation(LinearLayout.HORIZONTAL);
            logCard.setPadding(40, 40, 40, 40);
            logCard.setGravity(Gravity.CENTER_VERTICAL);
            applySolidCardGeometry(logCard, getCardColor(), 40f);

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
            // We route requests strictly through the natively queued OS Thread Pool.
            android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                try {
                    android.location.Geocoder geocoder = new android.location.Geocoder(HistoryActivity.this, java.util.Locale.getDefault());
                    java.util.List<android.location.Address> addresses = geocoder.getFromLocation(newestPingInBlock.getLatitude(), newestPingInBlock.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        final String humanStreet = addresses.get(0).getAddressLine(0);
                        runOnUiThread(() -> tvCoords.setText("📍 " + humanStreet));
                    }
                } catch (Exception e) {}
            });

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

            android.widget.ImageView focusLens = new android.widget.ImageView(this);
            focusLens.setImageResource(android.R.drawable.ic_menu_mylocation);
            focusLens.setPadding(30, 30, 10, 30);
            focusLens.setColorFilter(getGradientStart()); 
            
            // 🚀 QA Fix: Foolproof & Lightweight Native Hardware Integration
            // Android OS natively maps 'Enter' & D-Pad to any view that is focusable + clickable.
            // No custom KeyListeners, no object overhead. 100% stable.
            focusLens.setFocusable(true);
            focusLens.setClickable(true);

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
            profileBmp = com.jdroidx.jfindme.utils.AvatarLoader.getCurrentUserAvatarBitmap(this, 150);
        }

        com.google.android.gms.maps.model.BitmapDescriptor trueAvatarPin = 
            com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generate(this, activeTargetName, profileBmp, true, false);

        replayAvatar = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(first.getLatitude(), first.getLongitude()))
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
