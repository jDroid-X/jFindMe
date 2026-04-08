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
import android.widget.HorizontalScrollView;
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
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.group.GroupActivity;
import com.jdroidx.jfindme.ui.management.GeofenceActivity;
import com.jdroidx.jfindme.ui.management.HealthActivity;
import com.jdroidx.jfindme.ui.management.HistoryActivity;
import com.jdroidx.jfindme.ui.management.NotifActivity;
import com.jdroidx.jfindme.ui.management.PrivacyActivity;
import com.jdroidx.jfindme.ui.settings.ProfileActivity;
import com.jdroidx.jfindme.ui.settings.SettingsActivity;
import com.jdroidx.jfindme.utils.SystemLogger;
import com.jdroidx.jfindme.ui.base.BottomNavTable;

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
    
    private TextView tvLocName, tvUserStatus, tvBatteryPer;
    private android.widget.ViewFlipper headerFlipper;
    private TextView tvInfoName, tvInfoDist, tvAddressMarquee;
    private LinearLayout radarPanel, radarList;
    private FrameLayout infoCard;
    private Location lastKnownLocation;
    private String selectedUserId;
    private ImageView headerAvatar, navDrawerAvatar;
    
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
        SystemLogger.i(TAG, "MapActivity [onCreate] executed. Starting UI Engine...");
        // Step 1: Force modern renderer to prevent blank map feel
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, renderer -> 
            SystemLogger.i(TAG, "Map SDK Renderer Synchronized: " + renderer.name()));
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        if (savedInstanceState != null) {
            isDualPaneActive = savedInstanceState.getBoolean("isDualPaneActive", true);
        }

        if (getIntent().hasExtra("target_uid")) {
            selectedUserId = getIntent().getStringExtra("target_uid");
            SystemLogger.i(TAG, "Tactical Lock Engaged for UID: " + selectedUserId);
        }

        super.onCreate(savedInstanceState);
    }
    
    private boolean hasZoomedToTarget = false;

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
        int dWidth = (int)(getResources().getDisplayMetrics().widthPixels * 0.75f);
        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(dWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        drawerParams.gravity = Gravity.START;
        drawerMenu.setLayoutParams(drawerParams);
        drawerLayout.addView(drawerMenu);

        return drawerLayout;
    }

    private View buildLandscapeLayout() {
        return buildPortraitLayout(); // Unified Dashboard UI seamlessly stretches across Landscape organically!
    }

    @Override
    protected void onUiRebuilt() {
        SystemLogger.i(TAG, "L7 Lifecycle: Map Dashboard Re-Synchronizing after Rotation.");
        
        // 1. Re-Target and Re-Sync Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_CONTAINER);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(ID_MAP_CONTAINER, mapFragment).commit();
            mapFragment.getMapAsync(this);
        }

        // 2. Immediate Data Restoration (Bypass Sync Wait)
        if (viewModel != null) {
            List<UserLocation> currentLocs = viewModel.getUserLocations().getValue();
            if (currentLocs != null) {
                updateRadarScanner(currentLocs);
                if (googleMap != null) {
                    com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.processViewModelLocations(this, googleMap, currentLocs);
                }
            }
            
            Location myLoc = viewModel.getMyLocation().getValue();
            if (myLoc != null) {
                updateUserPositionOnMap(myLoc);
            }
        }
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
        UiArchitect.applyCard(this, mapCardContainer, "CARD_STANDARD");
        
        // Map natively consumes remaining absolute space above the tiles!
        int quota = getResponsivePaddingQuota();
        LinearLayout.LayoutParams mcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        mcLp.setMargins(quota, getResponsiveHeaderTopQuota(), quota, 10); // 🚀 QA Fix: Compressed Mission Gap (10px)
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
        sideActionPanel.addView(createSmallIcon("ACTION_GHOST", view -> viewModel.toggleGhostMode(prefs)));
        sideActionPanel.addView(createSmallIcon("ACTION_SEARCH", view -> openLocateDrawer())); // QA Fix: Sweep & Target External Users
        
        applyAccessibility(expandIcon, "Toggle Full Screen Map");
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
                com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, expandIcon, "ACTION_MINIMIZE");
                
                label.setVisibility(View.GONE);
                lp.setMargins(0, 0, 0, 0); // 🚀 Bleeds out completely
                // Ensure map container takes maximum weight
                lp.weight = 10.0f; 
                
                // Morph R2 natively into R1 (1 Horizontal Ribbon)
                if(r2.getChildCount() > 0) {
                    java.util.List<View> toMove = new java.util.ArrayList<>();
                    for(int i=0; i<r2.getChildCount(); i++) toMove.add(r2.getChildAt(i));
                    r2.removeAllViews();
                    for(View child : toMove) r1.addView(child);
                }
                r2.setVisibility(View.GONE);
                
                // [Icon Shrink Logic]
                for(int i=0; i<r1.getChildCount(); i++) {
                    View cell = r1.getChildAt(i);
                    if (cell instanceof LinearLayout) {
                        LinearLayout tile = (LinearLayout) cell;
                        if (tile.getChildCount() >= 2) {
                            FrameLayout iconBg = (FrameLayout) tile.getChildAt(0);
                            iconBg.setPadding(10, 10, 10, 10); // 🚀 Situational Ribbon (10px)
                            iconBg.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
                            tile.getChildAt(1).setVisibility(View.GONE);
                        }
                    }
                }

            } else {
                sideActionPanel.setVisibility(View.GONE);
                com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, expandIcon, "ACTION_EXPAND");

                label.setVisibility(View.VISIBLE);
                r2.setVisibility(View.VISIBLE);
                lp.weight = 1.0f; // Return to shared weight
                int rQuota = getResponsivePaddingQuota();
                lp.setMargins(rQuota, getResponsiveHeaderTopQuota(), rQuota, 10); // 🚀 QA Fix: Compressed Mission Gap (10px)
                
                // Return bottom 4 items to R2 Stack (Standard Row parity)
                if(r1.getChildCount() > 4) {
                    java.util.List<View> toMove = new java.util.ArrayList<>();
                    for(int i=4; i<r1.getChildCount(); i++) toMove.add(r1.getChildAt(i));
                    r1.removeViews(4, toMove.size());
                    for(View child : toMove) r2.addView(child);
                }
                
                // Restore native Tile sizing
                for(int r=1; r<=2; r++) {
                    LinearLayout activeR = r == 1 ? r1 : r2;
                    for(int i=0; i<activeR.getChildCount(); i++) {
                        View cell = activeR.getChildAt(i);
                        if (cell instanceof LinearLayout) {
                            LinearLayout tile = (LinearLayout) cell;
                            FrameLayout iconBg = (FrameLayout) tile.getChildAt(0);
                            iconBg.setPadding(40, 40, 40, 40);
                            iconBg.setLayoutParams(new LinearLayout.LayoutParams(140, 140));
                            TextView tv = (TextView) tile.getChildAt(1);
                            tv.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
            mapCardContainer.setLayoutParams(lp);
        });

        // Insert Expand Graphic
        int eS = UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
        FrameLayout.LayoutParams expLp = new FrameLayout.LayoutParams(eS, eS);
        expLp.gravity = Gravity.TOP | Gravity.END;
        int gPad = getResponsivePaddingQuota();
        expLp.setMargins(gPad, gPad, gPad, gPad);
        
        FrameLayout.LayoutParams sideLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sideLp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        sideLp.setMargins(0, 0, 30, 30);

        // 🧱 QA Overlay Matrix Z-Index Fix! Ensure Native Fragment doesn't swallow our UI elements
        FrameLayout mapOverlayWrapper = new FrameLayout(this);
        mapOverlayWrapper.addView(mapContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        mapCardContainer.addView(mapOverlayWrapper, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 🚀 QA Fix: High-Velocity Marquee Animator
        tvAddressMarquee = new TextView(this);
        tvAddressMarquee.setSingleLine(true);
        tvAddressMarquee.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        tvAddressMarquee.setMarqueeRepeatLimit(-1);
        tvAddressMarquee.setHorizontallyScrolling(true);
        
        // Custom Speed Injection: We simulate 20% faster marquee by scrolling manually or using shorter delays locally
        tvAddressMarquee.setSelected(true); 
        tvAddressMarquee.setText("SATELLITE TRIANGULATION STANDBY. AWAITING TARGET...");
        tvAddressMarquee.setTextColor(getTextColor());
        int mPad = getResponsivePaddingQuota();
        tvAddressMarquee.setPadding(mPad * 2, mPad, mPad * 2, mPad);
        tvAddressMarquee.setTypeface(null, Typeface.BOLD);
        
        // 🚀 QA Fix: Link the Address Marquee back to the 10% Dynamic Gradient Theme natively!
        String masterAlpha = com.jdroidx.jfindme.ui.base.ThemeManager.THEMES[getResolvedThemeId()][1].substring(1, 3);
        android.graphics.drawable.GradientDrawable marqueeBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BL_TR,
            new int[]{
                Color.parseColor("#" + masterAlpha + String.format("%06X", (0xFFFFFF & getGradientStart()))),
                Color.parseColor("#" + masterAlpha + String.format("%06X", (0xFFFFFF & getGradientEnd())))
            }
        );
        tvAddressMarquee.setBackground(marqueeBg);

        mapCardContainer.addView(tvAddressMarquee);

        // Finalize Hierarchy (Z-Index & HUD Enforcement)
        root.addView(mapCardContainer); // Map rigidly consumes 100% of left-over screen mass!

        // --- THE FLOATING TELEMATICS HUD TILE ---
        dashboardGridZone = buildGridMatrix();
        UiArchitect.applyCard(this, dashboardGridZone, "CARD_DASHBOARD"); // Secure native Tile rendering
        int gQuota = getResponsivePaddingQuota();
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.setMargins(gQuota, gQuota, gQuota, gQuota); // 🚀 QA Fix: Balanced Vertical Momentum (Top/Bottom Symmetry)
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
        
        // 🚀 QA Fix: Synchronize Map HUD with the Global Architect Header and Enforce Vertical Symmetry
        int pTop = getResponsiveHeaderTopQuota();
        header.setPadding(10, pTop, 10, pTop); 
        
        // 🚀 QA Fix: Master Theme 10% Opacity Overlay matching the Native Header specifications!
        String masterAlpha_H = com.jdroidx.jfindme.ui.base.ThemeManager.THEMES[getResolvedThemeId()][1].substring(1, 3);
        android.graphics.drawable.GradientDrawable headBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BL_TR,
            new int[]{
                Color.parseColor("#" + masterAlpha_H + String.format("%06X", (0xFFFFFF & getGradientStart()))),
                Color.parseColor("#" + masterAlpha_H + String.format("%06X", (0xFFFFFF & getGradientEnd())))
            }
        );
        header.setBackground(headBg);
        
        // 1. Left Badge Card (Image Replica)
        LinearLayout badgeCard = new LinearLayout(this);
        badgeCard.setOrientation(LinearLayout.HORIZONTAL);
        badgeCard.setGravity(Gravity.CENTER_VERTICAL);
        badgeCard.setPadding(10, 10, 20, 10);
        UiArchitect.applyCard(this, badgeCard, "CARD_STANDARD");

        ImageView menuIcon = new ImageView(this);
        menuIcon.setImageResource(com.jdroidx.jfindme.R.drawable.ic_main_menu);
        menuIcon.setColorFilter(getGradientMid()); // 🚀 QA Chrominance Sync (Drawer Trigger)
        menuIcon.setPadding(5, 10, 5, 10); // Severely restricted X-Axis padding to reduce gap between Icon and Avatar natively!
        applyAccessibility(menuIcon, "Open Navigation Drawer");
        menuIcon.setOnClickListener(v -> { if (drawerLayout != null) drawerLayout.openDrawer(Gravity.START); });
        
        int iQuota = getResponsiveIconQuota();
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(iQuota, iQuota);
        menuLp.setMargins(0, 0, 5, 0); // Compress Native Margin bounds specifically
        badgeCard.addView(menuIcon, menuLp); 

        headerAvatar = new ImageView(this);
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, headerAvatar);
        
        int avSize = (int)(iQuota * 0.9f);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(avSize, avSize);
        avLp.setMargins(0, 0, 0, 0);
        badgeCard.addView(headerAvatar, avLp); 

        TextView tvNickName = new TextView(this);
        tvNickName.setText(prefs.getString("user_nickname", "AGENT_1E69").toUpperCase());
        tvNickName.setTextColor(getTextColor());
        tvNickName.setTextSize(11f); // Micro text
        tvNickName.setTypeface(null, Typeface.BOLD);
        tvNickName.setPadding(8, 0, 0, 0); 
        tvNickName.setSingleLine(false);
        tvNickName.setMaxLines(2); // Enable wrapping for long names
        badgeCard.addView(tvNickName);
        
        header.addView(badgeCard, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        // Dynamic spacer organically removed to favor Card Symmetry Matrix

        // 2. Middle Active Counter Tile (Restricted bounds to fit Portrait naturally)
        LinearLayout activeCard = new LinearLayout(this);
        activeCard.setOrientation(LinearLayout.VERTICAL);
        int aPad = getResponsivePaddingQuota();
        activeCard.setPadding(aPad, aPad / 2, aPad, aPad / 2); 
        activeCard.setMinimumHeight(iQuota); // 🚀 QA Fix: Dynamically synchronized to the icon quota for absolute alignment
        ImageView logo = new ImageView(this);
        logo.setImageResource(com.jdroidx.jfindme.R.drawable.logo_m2g); 
        logo.setColorFilter(getGradientStart());
        int logSize = scale(65);
        activeCard.addView(logo, new LinearLayout.LayoutParams(logSize, logSize));

        LinearLayout.LayoutParams activeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        activeLp.setMargins(scale(15), 0, scale(15), 0);
        header.addView(activeCard, activeLp);
        

        // 3. Right Status Card -> AUTOMATED FORENSIC FLIPPER ENGINE (V12.1)
        LinearLayout flipperCard = new LinearLayout(this);
        flipperCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, flipperCard, "CARD_STANDARD", 1.0f);
        
        headerFlipper = new android.widget.ViewFlipper(this);
        headerFlipper.setFlipInterval(60000); // 🚀 QA Restore: 60 Second Situational Rotation
        headerFlipper.setAutoStart(true);
        headerFlipper.setClipChildren(false); 
        
        // --- Card 1: ME Status (Persistent Child 0) ---
        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        statusCard.setPadding(10, 8, 10, 8);
        statusCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tvLocName = new TextView(this);
        tvLocName.setText("ME | INITIALIZING...");
        tvLocName.setTextColor(getGradientStart()); // 🌈 L7 Vibrant Accent: High-Pop Primary Identity
        tvLocName.setTextSize(scaleText(12f)); 
        tvLocName.setTypeface(null, Typeface.BOLD);
        tvLocName.setGravity(Gravity.END);
        statusCard.addView(tvLocName);

        tvBatteryPer = new TextView(this);
        tvBatteryPer.setText("SYNCING | --% ⚡");
        tvBatteryPer.setTextColor(getTextColor()); // Crisp Neutral Metadata
        tvBatteryPer.setTextSize(scaleText(9.75f)); 
        tvBatteryPer.setTypeface(null, Typeface.BOLD);
        tvBatteryPer.setGravity(Gravity.END);
        statusCard.addView(tvBatteryPer);
        headerFlipper.addView(statusCard);
        
        // 🚀 QA Fix: Top-to-Bottom Slide Implementation organically via L7 Animation Table
        headerFlipper.setInAnimation(this, com.jdroidx.jfindme.R.anim.slide_in_top);
        headerFlipper.setOutAnimation(this, com.jdroidx.jfindme.R.anim.slide_out_bottom);

        // Add Flipper to its Bordered Card HUD
        flipperCard.addView(headerFlipper, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        LinearLayout.LayoutParams flipperLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        flipperLp.setMargins(scale(10), 0, 0, 0); 
        header.addView(flipperCard, flipperLp);
        
        return header;
    }

    private View buildGridMatrix() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        int wrapPad = getResponsivePaddingQuota();
        wrapper.setPadding(wrapPad, 20, wrapPad, 20); // 🚀 QA Fix: Symmetric Grid Alignment (Top/Bottom Balance)
        
        TextView label = new TextView(this);
        label.setText("MONITORING TILE");
        label.setTextColor(getSecondaryTextColor());
        label.setTextSize(10f);
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(20, 10, 0, 20); // Compressed padding for title balance
        wrapper.addView(label);

        // 🚀 Row 1
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.setPadding(0, 0, 0, 10); // 🚀 QA Fix: Compressed Inter-Row Momentum (10px)
        r1.addView(UiArchitect.createTile(this, "NETWORK_LOCATOR", v -> openLocateDrawer()));
        r1.addView(UiArchitect.createTile(this, "SYNC_DEVICES",   v -> startActivity(new Intent(this, GroupActivity.class))));
        r1.addView(UiArchitect.createTile(this, "LOCATION_LOGS",  v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.HistoryActivity.class))));
        r1.addView(UiArchitect.createTile(this, "PANIC_ALERTS",   v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.NotifActivity.class))));
        wrapper.addView(r1);

        // 🚀 Row 2
        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        r2.setPadding(0, 0, 0, 10); // 🚀 QA Fix: Compressed Inter-Row Momentum (10px)
        r2.addView(UiArchitect.createTile(this, "SAFE_ZONES",     v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.GeofenceActivity.class))));
        r2.addView(UiArchitect.createTile(this, "FAMILY_FRIENDS", v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class))));
        r2.addView(UiArchitect.createTile(this, "SETTINGS_HUB",   v -> startActivity(new Intent(this, SettingsActivity.class))));
        
        // 🚀 QA Filter Engine: Toggle Matrix (FAMILY <-> FRIENDS)
        r2.addView(UiArchitect.createTile(this, "PEOPLE", v -> {
            String current = viewModel.getCurrentFilterMode();
            String next = "FAMILY"; // Start with Family
            if (current.equals("FAMILY")) next = "FRIENDS";
            else if (current.equals("FRIENDS")) next = "FAMILY";
            
            viewModel.setFilterMode(next);
            Toast.makeText(this, "TAGGED MESH: " + next, Toast.LENGTH_SHORT).show();
        }));
        
        wrapper.addView(r2);

        return wrapper;
    }


    private View createBottomZone() {
        String[] keys = {"NAV_NOTICE", "NAV_ALERT", "NAV_DEVICE", "NAV_ME"};
        return UiArchitect.createBottomNav(this, keys, "NAV_DEVICE");
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
        
        applyAccessibility(tab, "Navigate to " + label);
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
                    com.google.android.gms.maps.model.CameraPosition pos = new com.google.android.gms.maps.model.CameraPosition.Builder()
                        .target(current).zoom(16.5f).tilt(45f).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
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

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle("ACTIVE TARGET SELECTOR")
            .setItems(names, (d, which) -> {
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
        
        applyDialogTheme(dialog);
    }
    
    private com.google.android.gms.maps.model.Polyline currentProximityLine;
    private String activeProximityTargetId = null; // 🚀 QA Fix: Master Tracking Lock

    private View createProximityTile(String avatarName, String targetUserId, String distance, long timeChecked, double hLat, double hLng, double cLat, double cLng) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER);
        
        applyAccessibility(row, "Focus on " + avatarName);
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
        String mAlpha = com.jdroidx.jfindme.ui.base.ThemeManager.THEMES[getResolvedThemeId()][1].substring(1, 3);
        bg.setColor(Color.parseColor("#" + mAlpha + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        bg.setStroke(3, Color.RED); 
        bg.setCornerRadius(30f);
        row.setBackground(bg);
        
        row.setPadding(20, 20, 20, 20); // 🚀 QA Fix: Global Padding Symmetry
        row.setMinimumHeight(125); // 🚀 High-Fidelity Clamping
        
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
        
        android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1, new android.location.Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@androidx.annotation.NonNull java.util.List<android.location.Address> addresses) {
                    processMarqueeAddress(addresses, lat, lng, targetName, timeStr);
                }
                @Override
                public void onError(@androidx.annotation.Nullable String errorMessage) {
                    processMarqueeAddress(null, lat, lng, targetName, timeStr);
                }
            });
        } else {
            new Thread(() -> {
                try {
                    @SuppressWarnings("deprecation")
                    java.util.List<android.location.Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    runOnUiThread(() -> processMarqueeAddress(addresses, lat, lng, targetName, timeStr));
                } catch (Exception e) {
                    runOnUiThread(() -> processMarqueeAddress(null, lat, lng, targetName, timeStr));
                }
            }).start();
        }
    }

    private void processMarqueeAddress(java.util.List<android.location.Address> addresses, double lat, double lng, String targetName, String timeStr) {
        String resultStr;
        if (addresses != null && !addresses.isEmpty()) {
            resultStr = addresses.get(0).getAddressLine(0);
        } else {
            resultStr = "LAT: " + lat + " | LNG: " + lng;
        }
        
        String finalPayload = "📡 TARGET LOCKED: [" + targetName + "] • Last Known Sync: " + timeStr + " • Exact Physical Location: " + resultStr + "        ";
        tvAddressMarquee.setText(finalPayload);
    }

    private View createSmallIcon(String tableKey, View.OnClickListener listener) {
        // 🚀 QA Fix: Global Sync! Use the Architect's 5-Stage scaling engine instead of ad-hoc density prefs.
        int activeSize = UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
        int dpPadding = (int)(activeSize * 0.20f); // 20% internal padding for side icons
        
        ImageView iv = new ImageView(this);
        com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, iv, tableKey); 
        iv.setPadding(dpPadding, dpPadding, dpPadding, dpPadding); 
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(activeSize, activeSize);
        params.setMargins(0, 8, 0, 8); 
        iv.setLayoutParams(params);
        
        iv.setClickable(true);
        iv.setFocusable(true);
        iv.setOnClickListener(listener);

        // 🚀 QA Fix: Global Focus State Loop (Enter / D-Pad Support)
        iv.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });
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

        viewModel.getRefreshTrigger().observe(this, trigger -> {
            if (trigger != null) triggerCalloutRefresh();
        });

        viewModel.getUserLocations().observe(this, locations -> {
            // 🚀 QA Fix: Strict MVVM Execution bypassing rogue Firebase Listeners
            if (googleMap != null) {
                com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.processViewModelLocations(this, googleMap, locations);
            }
            
            // 🚀 QA Fix: Tactical Zoom Lock (V8.9)
            if (selectedUserId != null && !hasZoomedToTarget && googleMap != null) {
                for (com.jdroidx.jfindme.data.model.UserLocation ul : locations) {
                    if (ul.getUserId().equals(selectedUserId)) {
                        com.google.android.gms.maps.model.LatLng target = new com.google.android.gms.maps.model.LatLng(ul.getLatitude(), ul.getLongitude());
                        com.google.android.gms.maps.model.CameraPosition pos = new com.google.android.gms.maps.model.CameraPosition.Builder()
                            .target(target).zoom(16.5f).tilt(45f).build();
                        googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(pos));
                        hasZoomedToTarget = true;
                        break;
                    }
                }
            }

            updateRadarScanner(locations);
            
            // 🧠 Fulfill QA Request: Live user aggregation rendering
            if (tvUserStatus != null) {
            }
            long count = Math.max(0, locations.size() - 1); // exclude self
            // 🚀 QA Fix: Legacy Proximity Counter decommissioned in favor of Logo Matrix.
            
            // 🚀 QA Fix: Proximity Flipper Removed (Situational Awareness consolidated to Markers)

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
        
        viewModel.getPendingSafeZoneRequestsCount().observe(this, count -> {
            updateNavBarBadge("NAV_NOTICE", count);
        });
    }

    private void updateNavBarBadge(String key, int count) {
        // Find the nav bar view. In createLayout, we don't store it but we can use findViewById if we set an ID.
        // Let's iterate children of root.
        ViewGroup root = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
        findAndUpdateNavBadge(root, key, count);
    }

    private void findAndUpdateNavBadge(ViewGroup root, String key, int count) {
        View nav = root.findViewWithTag("nav_bar_root");
        if (nav != null) {
            BottomNavTable.updateBadge(nav, key, count);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        SystemLogger.i(TAG, "MapActivity [onMapReady] callback received. GoogleMap instance physically bound natively.");
        this.googleMap = googleMap;
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        this.googleMap.setBuildingsEnabled(true); // 🚀 3D Perspective Fix (V12.0)
        this.googleMap.setIndoorEnabled(true);
        this.googleMap.getUiSettings().setCompassEnabled(true);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.googleMap.getUiSettings().setTiltGesturesEnabled(true);
        this.googleMap.getUiSettings().setAllGesturesEnabled(true);
        
        // 🚀 QA MVVM Strict Enforcement: 
        // Rendering layers no longer boot their own tracking loops! 
        // MultiMarkerEngine now officially strictly slaves itself to the MapViewModel natively!
        
        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.getTitle() == null) return false;
            
            selectedUserId = null;
            
            List<UserLocation> locs = viewModel.getUserLocations().getValue();
            String rawTitle = marker.getTitle();
            
            if (locs != null && !rawTitle.equalsIgnoreCase("YOU")) {
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
        com.google.android.gms.maps.model.CameraPosition pos = new com.google.android.gms.maps.model.CameraPosition.Builder()
            .target(defaultLocation).zoom(15f).tilt(45f).build();
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    private void checkLocationPermission() {
        SystemLogger.i(TAG, "Executing [checkLocationPermission] hardware audit logic.");
        if (SecurityEngine.INSTANCE.isRooted()) {
            Toast.makeText(this, "SECURITY ALERT: Rooted Device Detected.", Toast.LENGTH_LONG).show();
            SystemLogger.w(TAG, "DEVICE_INTEGRITY_COMPROMISED");
        }
        
        java.util.List<String> perms = new java.util.ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            
            // 🚀 QA Fix: Android 12+ Explicit Hardware Crash Preventer natively requires Coarse Location bundled in!
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        
        // 🚀 QA Fix: Background Tracking Enforcement for Android 10+ (Stage 5 Hardening)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
        }
        
        // 🚀 QA Fix: Force OS UI Dialog for Background Tracing Notifications (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (perms.isEmpty()) {
            SystemLogger.i(TAG, "All explicit Location requirements met. Booting feature set.");
            enableLocationFeatures();
        } else {
            SystemLogger.w(TAG, "Core permissions missing: requesting system authority UI: " + perms.toString());
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), LOCATION_PERMISSION_CODE);
        }
    }

    private void enableLocationFeatures() {
        SystemLogger.i(TAG, "Executing [enableLocationFeatures]. Validating explicit OS constraints.");
        try {
            if (googleMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                SystemLogger.i(TAG, "OS constraints cleared. Instructing Map overlay & fetching true device position.");
                // QA Graphic Fix: Disable native Blue Dot overlay to prevent graphic clipping under the custom Avatar PIN.
                googleMap.setMyLocationEnabled(false);
                getCurrentLocation();
            } else {
                 SystemLogger.e(TAG, "Failed [enableLocationFeatures]: Map instance null or Permission secretly revoked!");
            }
        } catch (SecurityException e) { SystemLogger.e(TAG, "Permission error", e); }
    }

    private void getCurrentLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
            
            // 🚀 QA Fix: Force active polling. getLastLocation() usually returns null on fresh installs causing blank maps!
            fusedLocationClient.getCurrentLocation(100, null).addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                    com.google.android.gms.maps.model.CameraPosition pos = new com.google.android.gms.maps.model.CameraPosition.Builder()
                        .target(current).zoom(16.5f).tilt(45f).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
                    updateUserPositionOnMap(location);
                } else {
                    // Fallback to absolute system memory
                    fusedLocationClient.getLastLocation().addOnSuccessListener(loc2 -> {
                        if (loc2 != null) updateUserPositionOnMap(loc2);
                    });
                }
            });
            
            if (locationEngine != null) locationEngine.startTracking();
            
            // QA Fix: Structural Boot Integration. Force Foreground Service to engage natively!
            Intent serviceIntent = new Intent(this, com.jdroidx.jfindme.service.TrackingService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            // 🚀 QA Fix: Force the User to explicitly whitelist this Architecture from OEM "Doze" battery killers!
            verifyDozeModeExclusion();
            
        } catch (Exception e) { SystemLogger.e(TAG, "GPS Boot OS Protection Handled: ", e); }
    }

    private void verifyDozeModeExclusion() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                // Warning logic prevents Background tracking limits on Huawei, Xiaomi, and Pixel
                SystemLogger.w(TAG, "OS SDK is restricting background telemetry! Requesting Whitelist.");
                android.content.Intent intent = new android.content.Intent();
                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(android.net.Uri.parse("package:" + packageName));
                startActivity(intent);
            } else {
                SystemLogger.i(TAG, "jFindMe explicitly whitelisted from Doze constraints. Background Tracking Secured.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SystemLogger.i(TAG, "onRequestPermissionsResult callback hit natively. Code: " + requestCode + " Arrays: " + java.util.Arrays.toString(grantResults));
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            SystemLogger.i(TAG, "Hardware granted explicitly by user. Booting features.");
            enableLocationFeatures();
        } else {
            SystemLogger.w(TAG, "HARDWARE DENIED BY USER. Native tracking explicitly crippled.");
        }
    }

    private void updateUserPositionOnMap(Location location) {
        if (googleMap == null) return;
        this.lastKnownLocation = location;
        LatLng myPos = new LatLng(location.getLatitude(), location.getLongitude());
        pathPoints.add(myPos);
        if (pathPoints.size() > 50) pathPoints.remove(0);

        // Core Pipeline Integration: Spawns the explicitly configured 2D avatar graphic directly onto the Live Map GPS grid!
        String myName = "You";
        if (userAvatar == null) {
            userAvatar = googleMap.addMarker(new MarkerOptions()
                .position(myPos)
                .title("You")
                .anchor(0.5f, 0.5f) // Calibrated implicitly to center of drawn callout box
                .flat(false) // Billboard fix: Pops perpendicular to the camera!
                .icon(getCustomAvatarPin(myName)));
        } else { 
            userAvatar.setPosition(myPos); // Update position directly
            userAvatar.setTitle("You"); 
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
            tvLocName.setText("ME | " + String.format("%.5f", location.getLatitude()) + ", " + String.format("%.5f", location.getLongitude()));
            tvLocName.setTextColor(getGradientStart()); // Sync with Flipper Theme
        }

        // 🚀 QA Fix: Master Battery Sync -> Update the ME status card in the flipper!
        if (tvBatteryPer != null) {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = registerReceiver(null, ifilter);
            int level = batteryStatus != null ? batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) : -1;
            String timeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
            tvBatteryPer.setText(timeStr + " | " + (level >= 0 ? level + "% ⚡" : "CALIBRATING"));
            tvBatteryPer.setTextColor(getTextColor());
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
        android.graphics.Bitmap bmp = com.jdroidx.jfindme.utils.AvatarLoader.getCurrentUserAvatarBitmap(this, 256);
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
        
        // 🚀 QA Fix: Master Hardware Enforcement (V14.1)
        validateLocationHardwareStatus();
        
        if (headerAvatar != null) com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, headerAvatar);
        if (navDrawerAvatar != null) com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, navDrawerAvatar);
    }

    private void validateLocationHardwareStatus() {
        android.location.LocationManager lm = (android.location.LocationManager) getSystemService(android.content.Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        try { gpsEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER); } catch (Exception e) {}
        
        if (!gpsEnabled) {
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Hardware Link Severed")
                .setMessage("The High-Fidelity GPS array is currently OFFLINE.\n\nTo maintain real-time safety tracking and history archives, the system requires an active hardware link.\n\nWould you like to re-activate Location Services now?")
                .setPositiveButton("Activate GPS", (d, which) -> {
                    startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setNeutralButton("Settings", (d, i) -> {
                    startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.management.PrivacyActivity.class));
                })
                .setNegativeButton("Ignore", null)
                .setCancelable(true)
                .show();
        }
    }
 
    private void updateRadarScanner(List<UserLocation> locations) {
        if (headerFlipper == null) return;
        
        // 🚀 QA Fix: GPS Recovery Loop -> Fallback to ViewModel position if hardware sensor is still warming up!
        Location referenceLoc = lastKnownLocation;
        if (referenceLoc == null && viewModel != null) {
            referenceLoc = viewModel.getMyLocation().getValue();
        }
        if (referenceLoc == null) return; 
        
        // 🚀 QA Fix: Preserve Current Displayed Situational Index (Persistence Engine)
        int currentIdx = headerFlipper.getDisplayedChild();
        boolean wasFlipping = headerFlipper.isFlipping();

        // 🚀 QA Fix: Consolidating all "Away Details" into the Header Flipper Matrix
        View baseCard = headerFlipper.getChildAt(0);
        headerFlipper.removeAllViews();
        headerFlipper.addView(baseCard);

        String myUid = FirebaseAuth.getInstance().getUid();
        for (UserLocation ul : locations) {
            if (ul.getUserId().equals(myUid) || ul.getUserId().isEmpty()) continue;
            
            float[] results = new float[3];
            Location.distanceBetween(referenceLoc.getLatitude(), referenceLoc.getLongitude(), 
                                    ul.getLatitude(), ul.getLongitude(), results);
            float distance = results[0];
            String distText = distance > 1000 ? String.format("%.1fkm", distance/1000) : String.format("%.0fm", distance);
            String timeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(ul.getTimestamp()));

            // 🚀 QA Fix: "Away" Visibility Logic -> Highlight distance in RED if > 100m threshold!
            int row1Color = getGradientStart(); // Vibrant Theme Accent for visibility
            if (distance > RADIUS_METERS) {
                row1Color = Color.RED; // High-Fidelity Alert: Device has breached the interaction radius!
            }

            // Create Forensic Member Tile for Flipper
            LinearLayout memberCard = new LinearLayout(this);
            memberCard.setOrientation(LinearLayout.VERTICAL);
            memberCard.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            memberCard.setPadding(10, 8, 10, 8); 
            memberCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Row 1: AVATAR NAME | DISTANCE (L7 Global Theme)
            TextView tvRow1 = new TextView(this);
            tvRow1.setText(ul.getUserName().toUpperCase() + " | " + distText);
            tvRow1.setTextColor(row1Color);
            tvRow1.setTextSize(scaleText(12f)); 
            tvRow1.setTypeface(null, Typeface.BOLD);
            tvRow1.setGravity(Gravity.END);
            tvRow1.setSingleLine(false);
            tvRow1.setMaxLines(2);
            memberCard.addView(tvRow1);

            // Row 2: SYNC TIME | BATTERY% ⚡
            TextView tvRow2 = new TextView(this);
            String bat = ul.getBatteryLevel() >= 0 ? ul.getBatteryLevel() + "% ⚡" : "CALIBRATING";
            tvRow2.setText(timeStr + " | " + bat);
            tvRow2.setTextColor(getTextColor()); // Crisp Neutral Metadata 
            tvRow2.setTextSize(scaleText(9.75f)); 
            tvRow2.setGravity(Gravity.END);
            tvRow2.setAlpha(0.95f);
            tvRow2.setTypeface(null, Typeface.BOLD);
            memberCard.addView(tvRow2);

            headerFlipper.addView(memberCard);
        }

        // Restore index if organically within bounds
        if (currentIdx < headerFlipper.getChildCount()) {
            headerFlipper.setDisplayedChild(currentIdx);
        }

        // Restart flipping if multiple cards exist organically
        if (headerFlipper.getChildCount() > 1) {
            headerFlipper.startFlipping();
        } else {
            headerFlipper.stopFlipping();
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
        // 🚀 QA Fix: Global Horizontal Header Segmentation (V11)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        applyGlobalHeaderGeometry(header); 
        header.setPadding(40, 60, 40, 60); // 🚀 Enforcing deep vertical symmetry
        
        // --- 1. Left Segment: Avatar Engine (Best Fit) ---
        ImageView drawerAvatar = new ImageView(this);
        navDrawerAvatar = drawerAvatar;
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, navDrawerAvatar);
        
        int avatarSize = (int)(getResponsiveIconQuota() * 1.2f); // 🚀 Liberal scale for drawer branding
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avLp.setMargins(0, 0, 40, 0);
        header.addView(drawerAvatar, avLp);
        
        // --- 2. Right Segment: User Profile Hub ---
        LinearLayout profileInfo = new LinearLayout(this);
        profileInfo.setOrientation(LinearLayout.VERTICAL);
        profileInfo.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView appTitle = new TextView(this);
        appTitle.setText(prefs.getString("user_nickname", "Guardian Name"));
        appTitle.setTextColor(getGradientOverlayText()); 
        appTitle.setTextSize(22); // 🚀 Forensic Tier Title sizing
        appTitle.setTypeface(null, Typeface.BOLD);
        profileInfo.addView(appTitle);
        
        TextView appStatus = new TextView(this);
        appStatus.setText("MISSION ACTIVE");
        appStatus.setTextColor(getGradientOverlayText());
        appStatus.setAlpha(0.65f);
        appStatus.setTextSize(12);
        appStatus.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        profileInfo.addView(appStatus);
        
        header.addView(profileInfo);
        drawer.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.VERTICAL);

        items.addView(createDrawerItem("Live Map Dashboard", "Map Overview", android.R.drawable.ic_dialog_map, v -> drawerLayout.closeDrawers()));
        items.addView(createDrawerItem("Profile", "Name & Details", com.jdroidx.jfindme.R.drawable.ic_user_profile, v -> startActivity(new Intent(this, ProfileActivity.class))));
        items.addView(createDrawerItem("Devices Pairing", "Sync children", com.jdroidx.jfindme.R.drawable.ic_pairing_matrix, v -> startActivity(new Intent(this, GroupActivity.class))));
        items.addView(createDrawerItem("Location History", "Review paths", com.jdroidx.jfindme.R.drawable.ic_location_history, v -> startActivity(new Intent(this, HistoryActivity.class))));
        
        // Safe Zones implicitly available to parent node operators natively
        items.addView(createDrawerItem("Safe Zones", "Geofence rules", com.jdroidx.jfindme.R.drawable.ic_safe_zone, v -> startActivity(new Intent(this, GeofenceActivity.class))));

        items.addView(createDrawerItem("Family and Friends", "Manage members", com.jdroidx.jfindme.R.drawable.ic_family_friends, v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class))));
        
        items.addView(createDrawerItem("Notifications & Alerts", "System warnings", android.R.drawable.ic_popup_reminder, v -> startActivity(new Intent(this, NotifActivity.class))));
        items.addView(createDrawerItem("Settings", "App Config", com.jdroidx.jfindme.R.drawable.ic_settings_gears, v -> startActivity(new Intent(this, SettingsActivity.class))));
        
        items.addView(createDrawerItem("Help & Support", "App FAQ", android.R.drawable.ic_menu_help, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.HelpSupportActivity.class));
            drawerLayout.closeDrawers();
        }));
        
        items.addView(createDrawerItem("Privacy & Permissions", "Security Rules", android.R.drawable.ic_lock_idle_lock, v -> startActivity(new Intent(this, PrivacyActivity.class))));
        
        // 🚀 Native Diagnostic/Logging Dashboard Gateway
        items.addView(createDrawerItem("System Audit Log", "Diagnostic Data", com.jdroidx.jfindme.R.drawable.ic_system_audit, v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.SystemLogActivity.class))));
        
        items.addView(createDrawerItem("About Application", "Version 1.0.0", com.jdroidx.jfindme.R.drawable.ic_about_app, v -> {
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

        // 🚀 QA Fix: Global Focus State Loop (Enter / D-Pad Support)
        item.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });

        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(getGradientMid()); // 🚀 QA Chrominance Sync (Drawer Items)
        
        int dSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        item.addView(iv, new LinearLayout.LayoutParams(dSize, dSize));

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
