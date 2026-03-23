package com.jdroidx.jfindme.ui.map;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.engine.location.LocationEngine;
import com.jdroidx.jfindme.engine.map.HeatmapEngine;
import com.jdroidx.jfindme.engine.map.MapEngine;
import com.jdroidx.jfindme.engine.map.MarkerClusterer;
import com.jdroidx.jfindme.engine.security.SecurityEngine;
import com.jdroidx.jfindme.engine.alert.AlertManager;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.group.GroupActivity;
import com.jdroidx.jfindme.ui.management.GeofenceActivity;
import com.jdroidx.jfindme.ui.management.HealthActivity;
import com.jdroidx.jfindme.ui.management.HistoryActivity;
import com.jdroidx.jfindme.ui.management.NotifActivity;
import com.jdroidx.jfindme.ui.management.PrivacyActivity;
import com.jdroidx.jfindme.ui.settings.ProfileActivity;
import com.jdroidx.jfindme.ui.settings.SettingsActivity;
import com.jdroidx.jfindme.utils.SystemLogger;

import java.util.ArrayList;
import java.util.List;
import kotlin.Unit;

/**
 * Main -> UI Engine -> MapScreen -> MapActivity (L3-L5)
 * ENTERPRISE EDITION: Restored full feature set with RBAC and Background Sync logic.
 */
public class MapActivity extends BaseActivityJava implements OnMapReadyCallback {

    private MapViewModel viewModel;
    private GoogleMap googleMap;
    private LocationEngine locationEngine;
    private MapEngine mapEngine;
    private MarkerClusterer markerClusterer;
    private FusedLocationProviderClient fusedLocationClient;
    private DrawerLayout drawerLayout;
    
    private Marker userAvatar;
    private Circle interactionRadius;
    private List<LatLng> pathPoints = new ArrayList<>();
    private Polyline movementTrail;
    
    private TextView tvLocName, tvUserStatus, tvBatteryPer, tvActiveCount;
    private TextView tvInfoName, tvInfoDist, tvAddressMarquee;
    private LinearLayout radarPanel, radarList;
    private FrameLayout infoCard;
    private Location lastKnownLocation;
    private String selectedUserId;
    
    private boolean isDualPaneActive = true;
    private boolean isMapExpanded = false;
    private LinearLayout topHeroZone;
    private View dashboardGridZone;
    private LinearLayout mapCardContainer;
    private static final int ID_MAP_CONTAINER = 1001;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int RADIUS_METERS = 100;
    private final String TAG = "MapActivity_Enterprise";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Step 1: Force modern renderer to prevent blank map feel
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, renderer -> 
            SystemLogger.i(TAG, "Map SDK Renderer Synchronized: " + renderer.name()));
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        if (savedInstanceState != null) {
            isDualPaneActive = savedInstanceState.getBoolean("isDualPaneActive", true);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isDualPaneActive", isDualPaneActive);
    }

    @Override
    protected View createLayout() {
        drawerLayout = new DrawerLayout(this);
        drawerLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View mainContent;
        if (isLandscape && isDualPaneActive) {
            mainContent = buildLandscapeLayout();
        } else {
            mainContent = buildPortraitLayout();
        }
        
        drawerLayout.addView(mainContent);
        
        // Navigation Drawer - Rule 7: Zero Dead-Ends
        View drawerMenu = createDrawerContent();
        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(650, ViewGroup.LayoutParams.MATCH_PARENT);
        drawerParams.gravity = Gravity.START;
        drawerMenu.setLayoutParams(drawerParams);
        drawerLayout.addView(drawerMenu);

        return drawerLayout;
    }

    private View buildLandscapeLayout() {
        return buildPortraitLayout(); // Unified Dashboard UI seamlessly stretches across Landscape organically!
    }

    private View buildPortraitLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor()); // Dynamically pulls from Theme Database!

        // 1. The Hero Gradient Header
        topHeroZone = (LinearLayout) createTopZone();
        root.addView(topHeroZone); 

        // --- THE LIVE TRACKER CARD (FULL SCREEN CAMERA ENGINE) ---
        mapCardContainer = new LinearLayout(this);
        mapCardContainer.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(mapCardContainer, getCardColor(), 40f);
        
        // Map natively consumes remaining absolute space above the tiles!
        LinearLayout.LayoutParams mcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        mcLp.setMargins(30, 20, 30, 30);
        mapCardContainer.setLayoutParams(mcLp);

        FrameLayout mapContainer = new FrameLayout(this);
        mapContainer.setId(ID_MAP_CONTAINER);

        // 🚀 Expand Feature Matrix
        ImageView expandIcon = new ImageView(this);
        com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, expandIcon, "ACTION_EXPAND");
        expandIcon.setPadding(25, 25, 25, 25);
        
        // 🚀 Native Vertical 'Old Style' Menu Overlay
        LinearLayout sideActionPanel = new LinearLayout(this);
        sideActionPanel.setOrientation(LinearLayout.VERTICAL);
        sideActionPanel.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        sideActionPanel.setElevation(40f); // QA Fix: Penetrate Google Maps SurfaceView Z-Index!
        sideActionPanel.setVisibility(View.GONE);
        sideActionPanel.addView(createSmallIcon("ACTION_LOCATE", view -> recenterCamera())); // QA Fix: Locate Myself
        sideActionPanel.addView(createSmallIcon("ACTION_ADD", view -> { if(googleMap!=null) googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomIn()); })); // QA Fix: Zoom In Max
        sideActionPanel.addView(createSmallIcon("ACTION_MINUS", view -> { if(googleMap!=null) googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomOut()); })); // QA Fix: Zoom Out Map
        sideActionPanel.addView(createSmallIcon("ACTION_LAYERS", view -> { if(googleMap!=null) googleMap.setMapType(googleMap.getMapType() == com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL ? com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE : com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL); }));
        sideActionPanel.addView(createSmallIcon("ACTION_ZONES", view -> startActivity(new Intent(MapActivity.this, ProfileActivity.class)))); // QA Fix: Open True Profile Tracker
        sideActionPanel.addView(createSmallIcon("ACTION_GHOST", view -> { boolean h = prefs.getBoolean("callout_hide_mode", false); prefs.edit().putBoolean("callout_hide_mode", !h).apply(); triggerCalloutRefresh(); }));
        sideActionPanel.addView(createSmallIcon("ACTION_SEARCH", view -> openLocateDrawer())); // QA Fix: Sweep & Target External Users
        
        expandIcon.setOnClickListener(v -> {
            isMapExpanded = !isMapExpanded;
            // 🚀 QA OS Transition Engine: Auto-morphs all XML layouts frictionlessly!
            android.transition.TransitionManager.beginDelayedTransition(root, new android.transition.AutoTransition().setDuration(350));

            LinearLayout wrapper = (LinearLayout) dashboardGridZone;
            TextView label = (TextView) wrapper.getChildAt(0);
            LinearLayout r1 = (LinearLayout) wrapper.getChildAt(1);
            LinearLayout r2 = (LinearLayout) wrapper.getChildAt(2);

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mapCardContainer.getLayoutParams();

            if (isMapExpanded) {
                sideActionPanel.setVisibility(View.VISIBLE);
                expandIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                
                label.setVisibility(View.GONE);
                lp.setMargins(0, 0, 0, 20); // Map natively bleeds completely out to Full Screen bounds
                
                // Morph R2 natively into R1 (1 Horizontal Ribbon)
                if(r2.getChildCount() > 0) {
                    java.util.List<View> toMove = new java.util.ArrayList<>();
                    for(int i=0; i<r2.getChildCount(); i++) toMove.add(r2.getChildAt(i));
                    r2.removeAllViews();
                    for(View child : toMove) r1.addView(child);
                }
                r2.setVisibility(View.GONE);
                
                // Micro-Shrink and Remove Text completely yielding absolute space!
                for(int i=0; i<r1.getChildCount(); i++) {
                    View cell = r1.getChildAt(i);
                    FrameLayout iconBg = (FrameLayout) ((LinearLayout) cell).getChildAt(0);
                    iconBg.setPadding(25, 25, 25, 25);
                    iconBg.setLayoutParams(new LinearLayout.LayoutParams(110, 110)); 
                    TextView tv = (TextView) ((LinearLayout) cell).getChildAt(1);
                    tv.setVisibility(View.GONE); // QA Fix: NO TEXT
                }

            } else {
                sideActionPanel.setVisibility(View.GONE);
                com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, expandIcon, "ACTION_EXPAND");

                label.setVisibility(View.VISIBLE);
                r2.setVisibility(View.VISIBLE);
                lp.setMargins(30, 20, 30, 30); // Map snaps back into a standardized CardView
                
                // Return bottom 3 items to R2 Stack
                if(r1.getChildCount() > 3) {
                    java.util.List<View> toMove = new java.util.ArrayList<>();
                    for(int i=3; i<r1.getChildCount(); i++) toMove.add(r1.getChildAt(i));
                    r1.removeViews(3, toMove.size());
                    for(View child : toMove) r2.addView(child);
                }
                
                // Restore native Tile sizing
                for(int r=1; r<=2; r++) {
                    LinearLayout activeR = r == 1 ? r1 : r2;
                    for(int i=0; i<activeR.getChildCount(); i++) {
                        View cell = activeR.getChildAt(i);
                        FrameLayout iconBg = (FrameLayout) ((LinearLayout) cell).getChildAt(0);
                        iconBg.setPadding(40, 40, 40, 40);
                        iconBg.setLayoutParams(new LinearLayout.LayoutParams(140, 140));
                        TextView tv = (TextView) ((LinearLayout) cell).getChildAt(1);
                        tv.setVisibility(View.VISIBLE);
                        tv.setTextSize(11f);
                        tv.setPadding(0, 20, 0, 0);
                    }
                }
            }
            mapCardContainer.setLayoutParams(lp);
        });

        // Insert Expand Graphic
        FrameLayout.LayoutParams expLp = new FrameLayout.LayoutParams(110, 110);
        expLp.gravity = Gravity.TOP | Gravity.END;
        expLp.setMargins(30, 30, 30, 30);
        
        FrameLayout.LayoutParams sideLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sideLp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        sideLp.setMargins(0, 0, 30, 30);

        // 🧱 QA Overlay Matrix Z-Index Fix! Ensure Native Fragment doesn't swallow our UI elements
        FrameLayout mapOverlayWrapper = new FrameLayout(this);
        mapOverlayWrapper.addView(mapContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mapCardContainer.addView(mapOverlayWrapper, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // Address Marquee securely locked inside the Map Card
        tvAddressMarquee = new TextView(this);
        tvAddressMarquee.setSingleLine(true);
        tvAddressMarquee.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        tvAddressMarquee.setMarqueeRepeatLimit(-1);
        tvAddressMarquee.setSelected(true);
        tvAddressMarquee.setText("SATELLITE TRIANGULATION STANDBY. AWAITING TARGET...");
        tvAddressMarquee.setTextColor(getTextColor());
        tvAddressMarquee.setPadding(40, 30, 40, 30);
        tvAddressMarquee.setTypeface(null, Typeface.BOLD);
        
        // 🚀 QA Fix: Link the Address Marquee back to the 10% Dynamic Gradient Theme natively!
        android.graphics.drawable.GradientDrawable marqueeBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BL_TR,
            new int[]{
                Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientStart()))),
                Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientEnd())))
            }
        );
        tvAddressMarquee.setBackground(marqueeBg);

        mapCardContainer.addView(tvAddressMarquee);

        // Finalize Hierarchy (Z-Index & HUD Enforcement)
        root.addView(mapCardContainer); // Map rigidly consumes 100% of left-over screen mass!

        // --- THE FLOATING TELEMATICS HUD TILE ---
        dashboardGridZone = buildGridMatrix();
        applySolidCardGeometry(dashboardGridZone, getCardColor(), 40f); // Secure native Tile rendering
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.setMargins(30, 0, 30, 40);
        dashboardGridZone.setLayoutParams(gridLp);
        
        root.addView(dashboardGridZone); // Stacks solidly below map, right above Navbar
        
        mapOverlayWrapper.addView(sideActionPanel, sideLp);
        mapOverlayWrapper.addView(expandIcon, expLp);


        // 3. The Bottom Navigation Ribbon
        root.addView(createBottomZone());
        
        return root;
    }

    private View createTopZone() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        // 🚀 QA Fix: Dynamically bind the Map HUD to the Global Architect Header Bounds!
        int pTop = prefs != null ? prefs.getInt("custom_header_padding_top", 80) : 80;
        int pBot = prefs != null ? prefs.getInt("custom_header_padding_bottom", 60) : 60;
        header.setPadding(20, pTop, 20, pBot / 2); // Map natively requires half the bottom spacer to bleed into the glass effect!
        
        // 🚀 QA Fix: Master Theme 10% Opacity Overlay matching the Native Header specifications!
        android.graphics.drawable.GradientDrawable headBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BL_TR,
            new int[]{
                Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientStart()))),
                Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientEnd())))
            }
        );
        header.setBackground(headBg);
        
        // 1. Left Badge Card (Image Replica)
        LinearLayout badgeCard = new LinearLayout(this);
        badgeCard.setOrientation(LinearLayout.HORIZONTAL);
        badgeCard.setGravity(Gravity.CENTER_VERTICAL);
        badgeCard.setPadding(10, 10, 20, 10);
        applySolidCardGeometry(badgeCard, getCardColor(), 40f); 

        ImageView menuIcon = new ImageView(this);
        menuIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
        menuIcon.setColorFilter(getGradientStart()); // Binds directly to the active Theme Array
        menuIcon.setPadding(5, 10, 5, 10); // Severely restricted X-Axis padding to reduce gap between Icon and Avatar natively!
        menuIcon.setOnClickListener(v -> { if (drawerLayout != null) drawerLayout.openDrawer(Gravity.START); });
        
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(120, 120);
        menuLp.setMargins(0, 0, 5, 0); // Compress Native Margin bounds specifically
        badgeCard.addView(menuIcon, menuLp); 

        ImageView avatar = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, avatar);
        
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(110, 110);
        avLp.setMargins(0, 0, 0, 0);
        badgeCard.addView(avatar, avLp); 

        TextView tvNickName = new TextView(this);
        tvNickName.setText(prefs.getString("user_nickname", "AGENT_1E69").toUpperCase());
        tvNickName.setTextColor(getTextColor());
        tvNickName.setTextSize(11f); // Micro text
        tvNickName.setTypeface(null, Typeface.BOLD);
        tvNickName.setPadding(8, 0, 0, 0); // Removed massive padding barrier to compress text against the Avatar cleanly!
        badgeCard.addView(tvNickName);
        
        header.addView(badgeCard);
        
        // Dynamic spacer #1 securely locks Active Card perfectly in the center
        header.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));

        // 2. Middle Active Counter Tile (Restricted bounds to fit Portrait naturally)
        LinearLayout activeCard = new LinearLayout(this);
        activeCard.setOrientation(LinearLayout.VERTICAL);
        activeCard.setPadding(20, 8, 20, 8); 
        activeCard.setMinimumHeight(125); // 🚀 QA Fix: Mathematically clamps Native Height matching Avatar bounds
        activeCard.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable gA = new android.graphics.drawable.GradientDrawable();
        gA.setCornerRadius(30f);
        gA.setColor(Color.parseColor("#99000000"));
        activeCard.setBackground(gA);

        TextView actLbl = new TextView(this);
        actLbl.setText("ACTIVE");
        actLbl.setTextColor(Color.WHITE);
        actLbl.setAlpha(0.8f);
        actLbl.setTextSize(7f); // Reduced tracking font
        actLbl.setGravity(Gravity.CENTER);
        activeCard.addView(actLbl);

        tvActiveCount = new TextView(this);
        tvActiveCount.setText("0");
        tvActiveCount.setTextColor(Color.parseColor("#00E676")); 
        tvActiveCount.setTextSize(14f); // Shrunk digit natively
        tvActiveCount.setTypeface(null, Typeface.BOLD);
        tvActiveCount.setGravity(Gravity.CENTER);
        activeCard.addView(tvActiveCount);
        header.addView(activeCard);

        // Dynamic spacer #2 balances the right zone natively rendering Active in dead-center
        header.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1.0f));

        // 3. Right Status Pill (Image Replica)
        statusFlipper = new android.widget.ViewFlipper(this);
        // 🚀 QA Fix: Force ViewFlipper to homogenize bounding boxes so ALL tiles are explicitly identical in physical size!
        statusFlipper.setMeasureAllChildren(true); 
        
        android.view.animation.TranslateAnimation inAnim = new android.view.animation.TranslateAnimation(
            android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f, android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f,
            android.view.animation.Animation.RELATIVE_TO_PARENT, -1.0f, android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f
        );
        inAnim.setDuration(450);

        android.view.animation.TranslateAnimation outAnim = new android.view.animation.TranslateAnimation(
            android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f, android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f,
            android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f, android.view.animation.Animation.RELATIVE_TO_PARENT, 1.0f
        );
        outAnim.setDuration(450);

        statusFlipper.setInAnimation(inAnim);
        statusFlipper.setOutAnimation(outAnim);
        statusFlipper.setFlipInterval(20000); // 🚀 QA Fix: Increased animation sweep duration to a precise 20 seconds natively!

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(20, 10, 20, 10); 
        statusCard.setMinimumHeight(125); 
        statusCard.setGravity(Gravity.CENTER);
        nativeStatusCard = statusCard; // 🚀 QA Fix: Master reference established for explicit Queue jumping later
        
        android.graphics.drawable.GradientDrawable glass = new android.graphics.drawable.GradientDrawable();
        glass.setCornerRadius(30f);
        glass.setColor(Color.parseColor("#99000000")); // Dark Frosted Glass 
        glass.setStroke(3, Color.parseColor("#00E676")); // Exact same 3px Border Design
        statusCard.setBackground(glass);

        tvLocName = new TextView(this);
        tvLocName.setText("LIVE LAT/LNG SYNC..."); // Native placeholder, waits for GPS listener matrix to inject coordinates
        tvLocName.setTextColor(Color.WHITE);
        tvLocName.setAlpha(0.9f);
        tvLocName.setTextSize(10f); // Restored scaled up font natively matching the UX
        tvLocName.setTypeface(null, Typeface.BOLD);
        tvLocName.setGravity(Gravity.CENTER);
        statusCard.addView(tvLocName);

        TextView lbl = new TextView(this);
        lbl.setText("STATUS: SCANNING");
        lbl.setTextColor(Color.parseColor("#00E676"));
        lbl.setAlpha(1.0f);
        lbl.setTextSize(8f); // Restored dual-stack size mapping
        lbl.setTypeface(null, Typeface.BOLD);
        lbl.setGravity(Gravity.CENTER);
        statusCard.addView(lbl);

        statusFlipper.addView(statusCard);
        
        LinearLayout.LayoutParams flipperLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flipperLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        header.addView(statusFlipper, flipperLp);
        
        return header;
    }

    private View buildGridMatrix() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(20, 30, 20, 30); // Compact Grid Inner Bounds

        TextView label = new TextView(this);
        label.setText("MONITORING TILE");
        label.setTextColor(getSecondaryTextColor());
        label.setTextSize(10f);
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(20, 0, 0, 30);
        wrapper.addView(label);

        // Row 1
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.addView(createGridItem("Network\nLocator", prefs.getInt("icon_map_radar", com.jdroidx.jfindme.R.drawable.ic_radar), v -> openLocateDrawer())); // QA Fix: Custom Native Radar Symbol Configurable
        r1.addView(createGridItem("Sync\nDevices", prefs.getInt("icon_map_network", android.R.drawable.ic_menu_share), v -> startActivity(new Intent(this, GroupActivity.class))));
        r1.addView(createGridItem("Location\nLogs", prefs.getInt("icon_map_history", android.R.drawable.ic_menu_recent_history), v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.HistoryActivity.class))));
        
        wrapper.addView(r1);

        // Row 2
        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        r2.setWeightSum(3f);
        r2.setPadding(0, 0, 0, 40);
        r2.addView(createGridItem("Panic\nAlerts", prefs.getInt("icon_map_logs", android.R.drawable.ic_popup_reminder), v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.NotifActivity.class))));
        r2.addView(createGridItem("Safe\nZones", prefs.getInt("icon_map_zones", android.R.drawable.ic_dialog_map), v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.GeofenceActivity.class))));
        r2.addView(createGridItem("Settings\nHub", prefs.getInt("icon_map_settings", android.R.drawable.ic_menu_manage), v -> startActivity(new Intent(this, SettingsActivity.class))));
        wrapper.addView(r2);

        return wrapper;
    }

    private View createGridItem(String title, int iconRes, View.OnClickListener listener) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setClickable(true);
        cell.setFocusable(true);
        cell.setOnClickListener(listener);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        FrameLayout iconBg = new FrameLayout(this);
        // Emulate the soft Purple Background Circle behind the vector icon!
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientStart())))); // 10% opacity primary color
        iconBg.setBackground(bg);
        
        // 🚀 INTERCEPT: Execute Global Icon Density Overrides
        int activeDensity = prefs.getInt("custom_icon_density", 140);
        int pad = (int)(activeDensity * 0.28);
        iconBg.setPadding(pad, pad, pad, pad);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart()); // Solid Primary Tint Overlay
        int innerSize = (int)(activeDensity * 0.42);
        iconBg.addView(icon, new FrameLayout.LayoutParams(innerSize, innerSize));

        cell.addView(iconBg, new LinearLayout.LayoutParams(activeDensity, activeDensity));

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(getTextColor());
        tv.setTextSize(11f);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 20, 0, 0);
        cell.addView(tv);

        return cell;
    }

    private View createBottomZone() {
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        
        // 🚀 QA Fix: Dynamically bind NAV bar height to the Global Architect Settings!
        int pBot = prefs != null ? prefs.getInt("custom_header_padding_bottom", 60) : 60;
        int navHeight = Math.max(120, pBot * 3); // Base math: 60px padding = 180px Nav Height. Safely limits bounds.
        bottom.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, navHeight)); 
        
        // 🚀 QA Fix: Global Theme 10% Opacity Overlay instead of solid Card Color!
        android.graphics.drawable.GradientDrawable navBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BL_TR,
            new int[]{
                Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientStart()))),
                Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientEnd())))
            }
        );
        bottom.setBackground(navBg);
        bottom.setElevation(25f);
        
        bottom.addView(createBottomTab("Notice", android.R.drawable.ic_popup_reminder, false));
        bottom.addView(createBottomTab("Device", android.R.drawable.ic_menu_mapmode, true));
        bottom.addView(createBottomTab("Me", android.R.drawable.ic_menu_edit, false));
        return bottom;
    }

    private View createBottomTab(String label, int iconRes, boolean isActive) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);
        tab.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        
        // 🚀 QA Fix: Inject true Theme Colors for inactive states natively via 50% Alpha masks
        int themeStateColor = getGradientStart();
        if (!isActive) {
            themeStateColor = Color.parseColor("#66" + String.format("%06X", (0xFFFFFF & themeStateColor))); // 40% Opacity Fade
        }
        icon.setColorFilter(themeStateColor);
        
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(65, 65);
        iconLp.gravity = Gravity.CENTER_HORIZONTAL;
        tab.addView(icon, iconLp);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(themeStateColor);
        tv.setTextSize(11f);
        tv.setGravity(Gravity.CENTER); // QA Strict Vertical Alignment
        tv.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
        tv.setPadding(0, 10, 0, 0);
        
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLp.gravity = Gravity.CENTER_HORIZONTAL;
        tab.addView(tv, tvLp);
        
        if (!isActive && label.equals("Me")) {
            tab.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        } else if (!isActive && label.equals("Notice")) {
            // QA Fix: Native routing to the comprehensive Alert Config Matrix
            tab.setOnClickListener(v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.NotifActivity.class)));
        } else if (label.equals("Device")) {
            // QA Fix: Re-centers the viewport heavily onto the core device coordinates!
            tab.setOnClickListener(v -> {
                if (googleMap != null && lastKnownLocation != null) {
                    LatLng current = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 16f));
                }
            });
        }
        return tab;
    }

    // Global cleanup successfully managed at the header variables!

    private void openLocateDrawer() {
        if (viewModel == null || viewModel.getUserLocations().getValue() == null) {
            Toast.makeText(this, "No active targets locked in network.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<UserLocation> locs = viewModel.getUserLocations().getValue();
        if (locs.isEmpty()) {
            Toast.makeText(this, "Zero connections active.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[locs.size()];
        for (int i = 0; i < locs.size(); i++) {
            UserLocation u = locs.get(i);
            String safeName = u.getUserName();
            if (u.getUserId().equals(FirebaseAuth.getInstance().getUid())) {
                safeName = "YOU (Recenter Tracker)";
            }
            names[i] = safeName;
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("ACTIVE TARGET SELECTOR")
            .setItems(names, (dialog, which) -> {
                UserLocation target = locs.get(which);
                LatLng finalPos = new LatLng(target.getLatitude(), target.getLongitude());
                
                if (googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(finalPos, 16.5f));
                }
                
                String timeString = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(target.getTimestamp()));
                updateAddressMarquee(target.getLatitude(), target.getLongitude(), target.getUserName(), timeString);
            })
            .setNegativeButton("Cancel Selection", null)
            .show();
    }
    
    private android.widget.ViewFlipper statusFlipper;
    private View nativeStatusCard;
    private com.google.android.gms.maps.model.Polyline currentProximityLine;
    private String activeProximityTargetId = null; // 🚀 QA Fix: Master Tracking Lock

    private View createProximityTile(String avatarName, String targetUserId, String distance, long timeChecked, double hLat, double hLng, double cLat, double cLng) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER);
        
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            if (googleMap != null) {
                activeProximityTargetId = targetUserId; // QA Lock: Binds autonomous rendering pipeline to this exact Avatar!
                LatLng childPos = new LatLng(cLat, cLng);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(childPos, 16.5f));
                
                if (currentProximityLine != null) {
                    currentProximityLine.remove();
                }
                
                com.google.android.gms.maps.model.PolylineOptions lineOpts = new com.google.android.gms.maps.model.PolylineOptions()
                    .add(new LatLng(hLat, hLng))
                    .add(childPos)
                    .color(Color.RED)
                    .width(8f)
                    .geodesic(true);
                
                currentProximityLine = googleMap.addPolyline(lineOpts);
            }
        });
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        bg.setStroke(3, Color.RED); 
        bg.setCornerRadius(30f);
        row.setBackground(bg);
        
        row.setPadding(20, 10, 20, 10); 
        row.setMinimumHeight(125); // 🚀 QA Fix: Mathematically clamps Native Height explicitly matching Active Center box
        
        TextView t1 = new TextView(this);
        // Line 1: High Size Metric (JANE | 8.2 KM)
        t1.setText(avatarName.toUpperCase() + "  |  " + distance);
        // 🚀 QA Fix: Master Dynamic Contrast Override cleanly matches the active global theme
        t1.setTextColor(getTextColor()); 
        t1.setTextSize(12f); // Emphasized Upscale size specifically over background geometry!
        t1.setTypeface(null, Typeface.BOLD);
        t1.setSingleLine(true);
        t1.setGravity(Gravity.CENTER);
        row.addView(t1);

        String timeString = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(timeChecked));
        
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER);

        TextView t2 = new TextView(this);
        t2.setText("AWAY  |  "); // Static layout string
        t2.setTextColor(Color.parseColor("#FF5252")); // Red warning contrast
        t2.setAlpha(0.9f);
        t2.setTextSize(7f); // Reduced footprint for trailing label
        t2.setTypeface(null, Typeface.BOLD);
        t2.setGravity(Gravity.CENTER);
        timeRow.addView(t2);

        TextView tTime = new TextView(this);
        tTime.setText(timeString.toUpperCase()); 
        tTime.setTextColor(Color.WHITE); // High-contrast White for distinct readability natively
        tTime.setTextSize(14f); // 🚀 QA Fix: Font Size mathematically doubled (7f -> 14f) explicitly for the Time String
        tTime.setTypeface(null, Typeface.BOLD);
        tTime.setGravity(Gravity.CENTER);
        timeRow.addView(tTime);

        row.addView(timeRow);

        return row;
    }

    private void updateAddressMarquee(double lat, double lng, String targetName, String timeStr) {
        tvAddressMarquee.setText("Scanning Satellite Coordinates tracking " + targetName + "...");
        
        // Asynchronous Geocoder Thread to prevent freezing the UI Main Loop!
        new Thread(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
                List<android.location.Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                String resultStr;
                
                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address obj = addresses.get(0);
                    resultStr = obj.getAddressLine(0);
                } else {
                    resultStr = "LAT: " + lat + " | LNG: " + lng;
                }
                
                String finalPayload = "📡 TARGET LOCKED: [" + targetName + "] • Last Known Sync: " + timeStr + " • Exact Physical Location: " + resultStr + "        ";
                runOnUiThread(() -> tvAddressMarquee.setText(finalPayload));
            } catch (Exception e) {
                runOnUiThread(() -> tvAddressMarquee.setText("📡 TARGET LOCKED: [" + targetName + "] • Last Known Sync: " + timeStr + " • GPS Offset: " + lat + ", " + lng));
            }
        }).start();
    }

    private View createSmallIcon(String tableKey, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, iv, tableKey); // QA Integration: Dynamic Centralized Engine Call
        iv.setPadding(25, 25, 25, 25);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(110, 110);
        params.setMargins(0, 8, 0, 8); 
        iv.setLayoutParams(params);
        
        iv.setClickable(true);
        iv.setFocusable(true);
        iv.setOnClickListener(listener);
        return iv;
    }

    @Override
    protected void setupLogic() {
        // 🧱 UX Override: Architectural lifecycle fix -> Safely init after views configure
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);
        
        // 🔒 Back-Stack Protocol: Double-Tap Exit on Live Dashboard
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            private long backPressedTime = 0;
            @Override
            public void handleOnBackPressed() {
                // If Drawer Slider is active, Close the UI instead of tracking quit state
                if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
                    drawerLayout.closeDrawer(Gravity.START);
                    return;
                }
                
                // Final 2-Click Native Android Exit Loop (2000ms grace period)
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finishAffinity(); // Liquidates entire application matrix from RAM natively
                    System.exit(0);
                } else {
                    Toast.makeText(MapActivity.this, "Press BACK again to exit Map Dashboard", Toast.LENGTH_SHORT).show();
                    backPressedTime = System.currentTimeMillis();
                }
            }
        });
        
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_CONTAINER);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(ID_MAP_CONTAINER, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        locationEngine = new LocationEngine(this, location -> {
            viewModel.updateMyLocation(this, location);
            runOnUiThread(() -> updateUserPositionOnMap(location));
            return Unit.INSTANCE;
        });

        viewModel.getUserLocations().observe(this, locations -> {
            if (mapEngine != null) mapEngine.updateMarkers(locations);
            if (markerClusterer != null) markerClusterer.updateMarkers(locations);
            updateRadarScanner(locations);
            
            // 🧠 Fulfill QA Request: Live user aggregation rendering
            if (tvUserStatus != null) {
            }
            long count = Math.max(0, locations.size() - 1); // exclude self
            if (tvActiveCount != null) {
                tvActiveCount.setText(String.valueOf(count));
            }
            
            // 🚀 QA Fix: Priority Flipboard Injection - Agents First, Scanners Last
            if (statusFlipper != null) {
                // Flush everything explicitly to rebuild priority matrix
                statusFlipper.removeAllViews();

                UserLocation hostLoc = null;
                String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                if (myUid != null) {
                    for (UserLocation ul : locations) {
                        if (ul.getUserId().equals(myUid)) { hostLoc = ul; break; }
                    }
                    if (hostLoc != null) {
                        android.location.Location hostPoint = new android.location.Location("");
                        hostPoint.setLatitude(hostLoc.getLatitude());
                        hostPoint.setLongitude(hostLoc.getLongitude());
                        
                        UserLocation activeTrackingTarget = null;
                        
                        for (UserLocation ul : locations) {
                            if (!ul.getUserId().equals(myUid)) {
                                if (activeProximityTargetId != null && activeProximityTargetId.equals(ul.getUserId())) {
                                    activeTrackingTarget = ul; // Locate the explicitly hunted target in real-time array
                                }
                                
                                android.location.Location targetPoint = new android.location.Location("");
                                targetPoint.setLatitude(ul.getLatitude());
                                targetPoint.setLongitude(ul.getLongitude());
                                float distMeters = hostPoint.distanceTo(targetPoint);
                                if (distMeters >= 50f) {
                                    String distStr = distMeters >= 1000f ? String.format(java.util.Locale.US, "%.1f km", distMeters/1000f) : ((int)distMeters + " meters");
                                    statusFlipper.addView(createProximityTile(ul.getUserName(), ul.getUserId(), distStr, ul.getTimestamp(), hostLoc.getLatitude(), hostLoc.getLongitude(), ul.getLatitude(), ul.getLongitude()));
                                }
                            }
                        }
                        
                        // 🚀 QA Fix: Dynamically warps tracking line geometry natively, creating seamless chase animation!
                        if (currentProximityLine != null && activeTrackingTarget != null && googleMap != null) {
                            LatLng newH = new LatLng(hostLoc.getLatitude(), hostLoc.getLongitude());
                            LatLng newT = new LatLng(activeTrackingTarget.getLatitude(), activeTrackingTarget.getLongitude());
                            currentProximityLine.setPoints(java.util.Arrays.asList(newH, newT));
                        }
                    }
                }

                // Append the Native Scanning UI strictly at the end of the line!
                if (nativeStatusCard != null) {
                    if (nativeStatusCard.getParent() != null) {
                        ((ViewGroup) nativeStatusCard.getParent()).removeView(nativeStatusCard);
                    }
                    statusFlipper.addView(nativeStatusCard);
                }
                
                int childCountForMath = statusFlipper.getChildCount();
                int awayCount = childCountForMath - 1; // Native count explicitly isolating targets from scanner baseline
                if (awayCount > 0) {
                    int dynamicInterval = 60000 / childCountForMath; // 🚀 QA Fix: Total exact loop perfectly equates to the 60s Firebase update polling cycle!
                    if (dynamicInterval < 3000) dynamicInterval = 3000; // Hard clamp baseline to preserve tracking scale readability limits
                    
                    statusFlipper.setFlipInterval(dynamicInterval);
                    
                    if (!statusFlipper.isFlipping()) {
                        statusFlipper.startFlipping();
                    }
                } else {
                    statusFlipper.stopFlipping();
                }
            }

            for (UserLocation ul : locations) {
                if (ul.isSosActive() && !ul.getUserId().equals(FirebaseAuth.getInstance().getUid())) {
                    AlertManager.showSosNotification(this, ul.getUserName());
                    SystemLogger.w(TAG, "INCOMING_SOS: " + ul.getUserName());
                }
                
                if (ul.getUserId().equals(FirebaseAuth.getInstance().getUid()) && ul.isPoked()) {
                    Toast.makeText(this, "POKED! " + ul.getPokedBy() + " is looking for you", Toast.LENGTH_LONG).show();
                    AlertManager.triggerLocalSOSFeedback(this);
                    viewModel.clearPoke();
                }
            }
        });
        
        // 🚀 QA Option 2: Listen for Geofence Perimeters independently and deploy native Heads-up!
        viewModel.getGeofenceAlertTrigger().observe(this, alertPayload -> {
            if (alertPayload != null) {
                com.jdroidx.jfindme.engine.alert.AlertManager.showGeofenceNotification(this, alertPayload);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        this.googleMap.getUiSettings().setCompassEnabled(true);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.googleMap.getUiSettings().setAllGesturesEnabled(true);
        
        // 🚀 PILLARS 3 & 4 EXECUTION: Initialize Real-Time Multiperson Tracker Sandbox
        com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.initializeRealtimeGroupTracker(this, this.googleMap);
        
        // Shutting down legacy rendering to avoid GPU artifact collision
        // mapEngine = new MapEngine(this, googleMap);
        // markerClusterer = new MarkerClusterer(this, googleMap);
        
        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.getTitle() == null) return false;
            
            selectedUserId = null;
            
            List<UserLocation> locs = viewModel.getUserLocations().getValue();
            String rawTitle = marker.getTitle();
            
            if (locs != null && !rawTitle.equals("YOU")) {
                for (UserLocation l : locs) {
                    if (rawTitle.equals(l.getUserName())) {
                        selectedUserId = l.getUserId();
                        break;
                    }
                }
            }
            
            // 🚀 QA Fix: Force the Native Map Tooltip to become visible over the Custom Profile avatars!
            marker.showInfoWindow();
            
            // Return false allows the standard OS Google Maps behavior to securely execute (Camera pan & Tooltip hover)
            return false;
        });

        googleMap.setOnMapClickListener(latLng -> {
            // Dismiss active overlays natively here when needed.
            if (currentProximityLine != null) {
                 currentProximityLine.remove();
                 currentProximityLine = null;
                 activeProximityTargetId = null; // Sever telemetry lock!
            }
        });
        
        checkLocationPermission();
        
        // Initial Zoom Fix
        LatLng defaultLocation = new LatLng(18.5204, 73.8567);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));
    }

    private void checkLocationPermission() {
        if (SecurityEngine.INSTANCE.isRooted()) {
            Toast.makeText(this, "SECURITY ALERT: Rooted Device Detected.", Toast.LENGTH_LONG).show();
            SystemLogger.w(TAG, "DEVICE_INTEGRITY_COMPROMISED");
        }
        
        java.util.List<String> perms = new java.util.ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        // 🚀 QA Fix: Force OS UI Dialog for Background Tracing Notifications (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (perms.isEmpty()) {
            enableLocationFeatures();
        } else {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), LOCATION_PERMISSION_CODE);
        }
    }

    private void enableLocationFeatures() {
        try {
            if (googleMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // QA Graphic Fix: Disable native Blue Dot overlay to prevent graphic clipping under the custom Avatar PIN.
                googleMap.setMyLocationEnabled(false);
                getCurrentLocation();
            }
        } catch (SecurityException e) { SystemLogger.e(TAG, "Permission error", e); }
    }

    private void getCurrentLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 16f));
                    updateUserPositionOnMap(location);
                }
            });
            locationEngine.startTracking();
            
            // QA Fix: Structural Boot Integration. Force Foreground Service to engage natively!
            Intent serviceIntent = new Intent(this, com.jdroidx.jfindme.service.TrackingService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
        } catch (SecurityException e) { SystemLogger.e(TAG, "GPS Error", e); }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableLocationFeatures();
        }
    }

    private void updateUserPositionOnMap(Location location) {
        if (googleMap == null) return;
        this.lastKnownLocation = location;
        LatLng myPos = new LatLng(location.getLatitude(), location.getLongitude());
        pathPoints.add(myPos);
        if (pathPoints.size() > 50) pathPoints.remove(0);

        // Core Pipeline Integration: Spawns the explicitly configured 2D avatar graphic directly onto the Live Map GPS grid!
        String myName = prefs != null ? prefs.getString("user_nickname", "YOU") : "YOU";
        if (userAvatar == null) {
            userAvatar = googleMap.addMarker(new MarkerOptions()
                .position(myPos)
                .anchor(0.5f, 0.5f) // Calibrated implicitly to center of drawn callout box
                .icon(getCustomAvatarPin(myName)));
        } else { 
            userAvatar.setPosition(myPos); // Update position directly
            userAvatar.setIcon(getCustomAvatarPin(myName)); // Sync instantly if changed in Profile settings
            animateMarkerGlide(userAvatar, myPos, location.getBearing()); 
        }
        
        if (interactionRadius == null) {
            interactionRadius = googleMap.addCircle(new CircleOptions().center(myPos).radius(RADIUS_METERS).strokeWidth(4).strokeColor(getAccentColor()).fillColor(Color.argb(40, 0, 255, 0)));
        } else { interactionRadius.setCenter(myPos); }
        
        if (pathPoints.size() > 1) {
            if (movementTrail != null) movementTrail.remove();
            movementTrail = googleMap.addPolyline(new PolylineOptions().addAll(pathPoints).color(getAccentColor()).width(4).geodesic(true));
        }
        
        if (SecurityEngine.INSTANCE.isMockLocation(location)) {
            tvLocName.setText("MOCK GPS DETECTED!");
            tvLocName.setTextColor(Color.RED);
            SystemLogger.e(TAG, "MOCK_LOCATION_ALERT_ACTIVE");
            return;
        }
        
        if (tvLocName != null) {
            tvLocName.setText("LIVE: " + String.format("%.5f", location.getLatitude()) + ", " + String.format("%.5f", location.getLongitude()));
            tvLocName.setTextColor(Color.WHITE);
        }
    }

    private void animateMarkerGlide(Marker marker, LatLng finalPos, float finalRotation) {
        LatLng startPos = marker.getPosition();
        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(1000);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            double lat = v * finalPos.latitude + (1 - v) * startPos.latitude;
            double lng = v * finalPos.longitude + (1 - v) * startPos.longitude;
            marker.setPosition(new LatLng(lat, lng));
            marker.setRotation(v * finalRotation + (1 - v) * marker.getRotation());
        });
        anim.start();
    }

    /**
     * Engine Integrity: Rips the User's specific uploaded image directly from local storage,
     * scales it safely out of HD bounds to avoid OutOfMemory GPU map crashes, and locks it to the Map Pin.
     */
    private com.google.android.gms.maps.model.BitmapDescriptor getCustomAvatarPin(String displayName) {
        boolean showName = true;
        if (prefs != null) {
            boolean hideMode = prefs.getBoolean("callout_hide_mode", false);
            long revealedAt = prefs.getLong("callout_revealed_at", 0);
            showName = !hideMode || (System.currentTimeMillis() - revealedAt < 5000);
        }
        
        if (prefs == null) return com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generate(this, displayName, null, showName, true);
        
        // 1. Recover the precise state defined by the User in ProfileActivity
        android.graphics.Bitmap bmp = com.jdroidx.jfindme.utils.AvatarLoader.getCurrentUserAvatarBitmap(this, 150);
        return com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generate(this, displayName, bmp, showName, true);
    }
    
    private void triggerCalloutRefresh() {
        if (prefs != null) prefs.edit().putLong("callout_revealed_at", System.currentTimeMillis()).apply();
        if (lastKnownLocation != null) updateUserPositionOnMap(lastKnownLocation);
        if (mapEngine != null && viewModel != null && viewModel.getUserLocations().getValue() != null) {
            mapEngine.updateMarkers(viewModel.getUserLocations().getValue());
        }
        
        if (prefs != null && prefs.getBoolean("callout_hide_mode", false)) {
            new android.os.Handler().postDelayed(() -> {
                if (lastKnownLocation != null) updateUserPositionOnMap(lastKnownLocation);
                if (mapEngine != null && viewModel != null && viewModel.getUserLocations().getValue() != null) {
                    mapEngine.updateMarkers(viewModel.getUserLocations().getValue());
                }
            }, 5100);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        triggerCalloutRefresh(); // Automatically reveals callouts for 5 seconds organically upon screen navigation!
    }

    private void updateRadarScanner(List<UserLocation> locations) {
        if (radarList == null || lastKnownLocation == null) return;
        radarList.removeAllViews();
        String myUid = FirebaseAuth.getInstance().getUid();
        
        for (UserLocation ul : locations) {
            if (ul.getUserId().equals(myUid) || ul.getUserId().isEmpty()) continue;
            
            float[] results = new float[3];
            Location.distanceBetween(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), 
                                    ul.getLatitude(), ul.getLongitude(), results);
            float distance = results[0];
            float initialBearing = results[1];

            TextView tv = new TextView(this);
            String distText = distance > 1000 ? String.format("%.1f km", distance/1000) : String.format("%.0f m", distance);
            tv.setText(ul.getUserName() + " — " + distText + " (" + getCardinal(initialBearing) + ")");
            tv.setTextColor(ul.isSosActive() ? Color.RED : Color.WHITE);
            tv.setPadding(0, 10, 0, 10);
            tv.setTextSize(12);
            radarList.addView(tv);
        }
    }

    private String getCardinal(float bearing) {
        if (bearing < 0) bearing += 360;
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[(int)Math.round(((double)bearing % 360) / 45)];
    }

    private View createDrawerContent() {
        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(getPrimaryColor()); // QA Fix: Native System Dark/Light Mode
        drawer.setPadding(0, 0, 0, 0);

        // Header Section (Profile Photo, Name, Email)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(60, 100, 60, 60);
        applyLinearGradient(header, 0f); // QA Fix: Neon Gradient Aesthetic Sync
        
        ImageView drawerAvatar = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, drawerAvatar);
        header.addView(drawerAvatar, new LinearLayout.LayoutParams(150, 150));
        
        TextView appTitle = new TextView(this);
        appTitle.setText(prefs.getString("user_nickname", "Guardian Name"));
        appTitle.setTextColor(getGradientOverlayText()); // QA Fix: Guaranteed contrast check
        appTitle.setTextSize(18);
        appTitle.setTypeface(null, Typeface.BOLD);
        appTitle.setPadding(0, 20, 0, 0);
        header.addView(appTitle);
        
        TextView appEmail = new TextView(this);
        appEmail.setText("Parent / Administrator Account");
        appEmail.setTextColor(getGradientOverlayText());
        appEmail.setAlpha(0.85f);
        appEmail.setTextSize(12);
        header.addView(appEmail);
        
        drawer.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.VERTICAL);

        items.addView(createDrawerItem("Dashboard (Live Map)", "Map Overview", android.R.drawable.ic_dialog_map, v -> drawerLayout.closeDrawers()));
        items.addView(createDrawerItem("Devices Pairing", "Sync children", android.R.drawable.ic_menu_share, v -> startActivity(new Intent(this, GroupActivity.class))));
        items.addView(createDrawerItem("Location History", "Review paths", android.R.drawable.ic_menu_recent_history, v -> startActivity(new Intent(this, HistoryActivity.class))));
        
        // Safe Zones implicitly available to parent node operators natively
        items.addView(createDrawerItem("Safe Zones", "Geofence rules", android.R.drawable.ic_menu_mylocation, v -> startActivity(new Intent(this, GeofenceActivity.class))));

        items.addView(createDrawerItem("Family and Friends", "Manage members", android.R.drawable.ic_menu_myplaces, v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class))));
        
        items.addView(createDrawerItem("Notifications & Alerts", "System warnings", android.R.drawable.ic_popup_reminder, v -> startActivity(new Intent(this, NotifActivity.class))));
        items.addView(createDrawerItem("Profile", "Name & Details", android.R.drawable.ic_menu_edit, v -> startActivity(new Intent(this, ProfileActivity.class))));
        items.addView(createDrawerItem("Settings", "App Config", android.R.drawable.ic_menu_manage, v -> startActivity(new Intent(this, SettingsActivity.class))));
        
        items.addView(createDrawerItem("Help & Support", "App FAQ", android.R.drawable.ic_menu_help, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.HelpSupportActivity.class));
            drawerLayout.closeDrawers();
        }));
        
        items.addView(createDrawerItem("Privacy & Permissions", "Security Rules", android.R.drawable.ic_lock_idle_lock, v -> startActivity(new Intent(this, PrivacyActivity.class))));
        
        // 🚀 Native Diagnostic/Logging Dashboard Gateway
        items.addView(createDrawerItem("System Audit Log", "Diagnostic Data", android.R.drawable.ic_menu_agenda, v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.SystemLogActivity.class))));
        
        items.addView(createDrawerItem("About Application", "Version 1.0.0", android.R.drawable.ic_menu_info_details, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.AboutActivity.class));
            drawerLayout.closeDrawers();
        }));
        
        items.addView(createDrawerItem("Logout", "Sign Out", android.R.drawable.ic_lock_power_off, v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to securely log out of your Guardian Account? You will stop receiving tracking alerts.")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, com.jdroidx.jfindme.ui.auth.AuthActivity.class));
                    finishAffinity();
                })
                .setNegativeButton("Cancel", null)
                .show();
        }));

        scroll.addView(items);
        drawer.addView(scroll);
        return drawer;
    }

    private View createDrawerItem(String title, String desc, int iconRes, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(40, 30, 40, 30);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(listener);

        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(getAccentColor());
        item.addView(iv, new LinearLayout.LayoutParams(60, 60));

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(30, 0, 0, 0);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getTextColor()); // QA Fix: Shifts from Dark to Bright per Theme
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, Typeface.BOLD);
        textLayout.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextColor(getSecondaryTextColor()); // QA Fix: Smooth Subtitle Shift
        tvDesc.setTextSize(12);
        textLayout.addView(tvDesc);

        item.addView(textLayout);
        return item;
    }

    private void recenterCamera() {
        if (googleMap != null && lastKnownLocation != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), 17f));
        }
    }
}
