package com.jdroidx.jfindme.ui.map;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.engine.location.LocationEngine;
import com.jdroidx.jfindme.engine.map.MapEngine;
import com.jdroidx.jfindme.engine.map.MarkerClusterer;
import com.jdroidx.jfindme.engine.security.SecurityEngine;
import com.jdroidx.jfindme.engine.alert.AlertManager;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.ThemeManager;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.group.GroupActivity;
import com.jdroidx.jfindme.ui.management.GeofenceActivity;
import com.jdroidx.jfindme.ui.management.HealthActivity;
import com.jdroidx.jfindme.ui.management.HistoryActivity;
import com.jdroidx.jfindme.ui.management.NotifActivity;
import com.jdroidx.jfindme.ui.management.PrivacyActivity;
import com.jdroidx.jfindme.ui.settings.ProfileActivity;
import com.jdroidx.jfindme.ui.settings.SettingsActivity;
import com.jdroidx.jfindme.utils.RegistryManager;
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

    private com.jdroidx.jfindme.databinding.ActivityMapBinding binding;
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
    private List<Circle> safeZoneCircles = new ArrayList<>();
    private List<Polygon> safeZonePolygons = new ArrayList<>();
    
    private TextView tvLocName, tvBatteryPer;
    private android.widget.ViewFlipper headerFlipper;
    private TextView tvAddressMarquee;
    private Location lastKnownLocation;
    private String selectedUserId;
    private boolean hasZoomedToTarget = false;
    private ImageView headerAvatar, navDrawerAvatar;
    private TextView navDrawerTitle;
    
    private boolean isDualPaneActive = true;
    private static final String PREF_DUAL_PANE = "map_dual_pane_active";
    private boolean isMapExpanded = true; // 🚀 Default Full-Screen Tactical Map Mode
    private static final int ID_MAP_CONTAINER = com.jdroidx.jfindme.R.id.map_container;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int RADIUS_METERS = 100;
    private final String TAG = "MapActivity_Enterprise";

    private kotlin.jvm.functions.Function2<String, java.util.Map<String, Object>, kotlin.Unit> closedLoopAvatarCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SystemLogger.i(TAG, "MapActivity [onCreate] executed. Starting UI Engine...");

        // 🚀 QA Fix: Initialize ViewModel BEFORE super.onCreate
        // This prevents NullPointerException in createLayout/createTopZone logic which is triggered by super.onCreate
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

        // Step 1: Force modern renderer to prevent blank map feel
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, renderer -> 
            SystemLogger.i(TAG, "Map SDK Renderer Synchronized: " + renderer.name()));
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        if (savedInstanceState != null) {
            isDualPaneActive = savedInstanceState.getBoolean("isDualPaneActive", true);
        } else {
            // Fallback: restore from SharedPreferences (safeRecreate bypasses bundle)
            isDualPaneActive = getSharedPreferences("system_registry", MODE_PRIVATE)
                .getBoolean(PREF_DUAL_PANE, true);
        }

        extractIntentTarget(getIntent());

        // 🧬 Closed-Loop Avatar & Profile Callback (LOOP-001)
        closedLoopAvatarCallback = (agentId, payload) -> {
            runOnUiThread(() -> {
                // 🛡️ FIX-8: Guard against stale callback referencing destroyed activity
                if (!MapActivity.this.isFinishing() && !MapActivity.this.isDestroyed()) {
                    String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                    if (currentUid == null) return; // skip if user not authenticated
                    if (navDrawerAvatar != null) {
                        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, navDrawerAvatar);
                    }
                    if (navDrawerTitle != null) {
                        navDrawerTitle.setText(com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this));
                    }
                    triggerCalloutRefresh();
                }
            });
            return kotlin.Unit.INSTANCE;
        };
        com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.registerClosedLoopCallback(closedLoopAvatarCallback);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 🔋 Screen Inactive / Hidden: Immediately transition to ultra-low-power HEARTBEAT mode
        com.jdroidx.jfindme.engine.EngineRegistry.INSTANCE.heartbeat(com.jdroidx.jfindme.engine.EngineRegistry.ENGINE_LOCATION);
        com.jdroidx.jfindme.engine.EngineRegistry.INSTANCE.heartbeat(com.jdroidx.jfindme.engine.EngineRegistry.ENGINE_MAP_RENDER);
        // Persist dual-pane state to SharedPreferences so safeRecreate can recover it
        getSharedPreferences("system_registry", MODE_PRIVATE).edit()
            .putBoolean(PREF_DUAL_PANE, isDualPaneActive).apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        com.jdroidx.jfindme.engine.EngineRegistry.INSTANCE.heartbeat(com.jdroidx.jfindme.engine.EngineRegistry.ENGINE_LOCATION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        com.jdroidx.jfindme.engine.EngineRegistry.INSTANCE.heartbeat(com.jdroidx.jfindme.engine.EngineRegistry.ENGINE_LOCATION);
        if (closedLoopAvatarCallback != null) {
            com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.unregisterClosedLoopCallback(closedLoopAvatarCallback);
        }
    }

    private void extractIntentTarget(Intent intent) {
        if (intent != null) {
            String newTarget = intent.getStringExtra("TARGET_UID");
            if (newTarget == null) newTarget = intent.getStringExtra("target_uid");
            if (newTarget == null) newTarget = intent.getStringExtra("selected_uid");
            if (newTarget != null && !newTarget.trim().isEmpty()) {
                selectedUserId = newTarget.trim();
                hasZoomedToTarget = false;
                SystemLogger.i(TAG, "Target Lock Engaged for UID: " + selectedUserId);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        extractIntentTarget(intent);
        if (selectedUserId != null && googleMap != null) {
            locateTargetWithHistoryFallback(selectedUserId);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isDualPaneActive", isDualPaneActive);
        // Also persist to SharedPreferences for safeRecreate resilience
        getSharedPreferences("system_registry", MODE_PRIVATE).edit()
            .putBoolean(PREF_DUAL_PANE, isDualPaneActive).apply();
    }

    @Override
    protected View createLayout() {
        binding = com.jdroidx.jfindme.databinding.ActivityMapBinding.inflate(getLayoutInflater());
        
        drawerLayout = binding.drawerLayout;
        tvLocName = binding.tvLocName;
        tvBatteryPer = binding.tvBatteryPer;
        headerFlipper = binding.headerFlipper;
        tvAddressMarquee = binding.tvAddressMarquee;
        if (tvAddressMarquee != null) {
            int neonColor = ThemeManager.isDarkTheme(getResolvedThemeId()) ? Color.parseColor("#00F0FF") : Color.parseColor("#00FF66");
            tvAddressMarquee.setTextColor(neonColor);
            tvAddressMarquee.setSelected(true);
        }
        headerAvatar = binding.headerAvatar;
        if (headerAvatar != null) {
            headerAvatar.setBackground(null);
            headerAvatar.setOnClickListener(v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class)));
            com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, headerAvatar);
        }
        
        android.graphics.drawable.GradientDrawable headBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BL_TR,
            new int[]{ getGradientStart(), getGradientEnd() }
        );
        binding.topHeroZone.setBackground(headBg);

        binding.menuIcon.setColorFilter(getGradientMid());
        applyAccessibility(binding.menuIcon, "Open Navigation Drawer");
        binding.menuIcon.setOnClickListener(v -> { if (drawerLayout != null) drawerLayout.openDrawer(Gravity.START); });
        
        String currentSsotName = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this);
        if (binding.tvNickName != null) {
            binding.tvNickName.setText(currentSsotName.toUpperCase());
            binding.tvNickName.setTextColor(getTextColor());
        }
        
        viewModel.getUserDossier().observe(this, dossier -> {
            if (dossier == null) return;
            String liveName = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this);
            if (binding.tvNickName != null) binding.tvNickName.setText(liveName.toUpperCase());
        });
        
        if (binding.logo != null) {
            int rawId = getResources().getIdentifier("butterfly_angel", "raw", getPackageName());
            if (rawId != 0) {
                binding.logo.setAnimation(rawId);
                binding.logo.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
                binding.logo.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);
                binding.logo.enableMergePathsForKitKatAndAbove(true);
                binding.logo.setSpeed(1.25f);
                binding.logo.playAnimation();
            }
            binding.logo.setOnClickListener(v -> launchPictureInPictureMode());
        }
        if (binding.activeCard != null) {
            binding.activeCard.setOnClickListener(v -> launchPictureInPictureMode());
        }
        if (binding.btnPipClose != null) {
            binding.btnPipClose.setOnClickListener(v -> {
                if (isMapExpanded) {
                    binding.expandIcon.performClick();
                }
                binding.btnPipClose.setVisibility(View.GONE);
            });
        }
        
        binding.tvLocName.setTextColor(getGradientStart());
        binding.tvBatteryPer.setTextColor(getTextColor());
        
        binding.headerFlipper.setFlipInterval(3500);
        binding.headerFlipper.setInAnimation(this, com.jdroidx.jfindme.R.anim.slide_in_top);
        binding.headerFlipper.setOutAnimation(this, com.jdroidx.jfindme.R.anim.slide_out_bottom);
        
        UiArchitect.applyCard(this, binding.badgeCard, "CARD_STANDARD");
        UiArchitect.applyCard(this, binding.flipperCard, "CARD_STANDARD");
        
        UiArchitect.applyCard(this, binding.mapCardContainer, "CARD_STANDARD");
        
        binding.sideActionPanel.addView(createSmallIcon("ACTION_LOCATE", view -> recenterCamera()));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_REFRESH", view -> {
            if (viewModel != null) {
                viewModel.refreshLocations();
                com.google.firebase.database.FirebaseDatabase.getInstance().goOnline();
                UiArchitect.showSlideNotificationBanner(this, "MATRIX REFRESHED", "Scanning cloud & local tracking nodes...", false, 2500, null, null);
                if (googleMap != null && viewModel.getUserLocations().getValue() != null) {
                    com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.processViewModelLocations(
                        this, googleMap, viewModel.getUserLocations().getValue()
                    );
                }
            }
        }));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_ADD", view -> { if(googleMap!=null) googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomIn()); }));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_MINUS", view -> { if(googleMap!=null) googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomOut()); }));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_LAYERS", view -> showMapLayersDialog()));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_ALERT", view -> triggerEmergencySosDialog()));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_SAFE_ZONE", view -> {
            boolean currentShow = prefs.getBoolean("show_safe_zones_overlay", true);
            boolean newShow = !currentShow;
            prefs.edit().putBoolean("show_safe_zones_overlay", newShow).apply();
            renderSafeZonesOnMap(viewModel != null ? viewModel.getSafeZones().getValue() : null);
            Toast.makeText(this, newShow ? "🛡️ Safe Zones Displayed" : "🛡️ Safe Zones Hidden", Toast.LENGTH_SHORT).show();
        }));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_GHOST", view -> {
            boolean currentGhost = prefs.getBoolean("ghost_mode", false);
            String title = currentGhost ? "Disable Ghost Mode?" : "Enable Ghost Mode?";
            String msg = currentGhost 
                ? "Your live location telemetry will become visible to all authorized family members and guardians."
                : "Your live location will be concealed from all network members. Background tracking will be paused for other users.";
            UiArchitect.showConfirmationDialog(this, title, msg, "PROCEED", "CANCEL", () -> {
                viewModel.toggleGhostMode(prefs);
                boolean nowGhost = prefs.getBoolean("ghost_mode", false);
                Toast.makeText(this, nowGhost ? "GHOST MODE: ACTIVE (Concealed)" : "GHOST MODE: DISABLED (Visible)", Toast.LENGTH_SHORT).show();
            }, null);
        }));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_SEARCH", view -> showTacticalTargetSearchDialog()));
        
        com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, binding.expandIcon, isMapExpanded ? "ACTION_MINIMIZE" : "ACTION_EXPAND");
        applyAccessibility(binding.expandIcon, "Toggle Full Screen Map");
        binding.sideActionPanel.setVisibility(isMapExpanded ? View.VISIBLE : View.GONE);
        binding.dashboardGridZone.setVisibility(isMapExpanded ? View.GONE : View.VISIBLE);
        
        binding.expandIcon.setOnClickListener(v -> {
            isMapExpanded = !isMapExpanded;
            androidx.transition.TransitionManager.beginDelayedTransition(binding.mainContent, new androidx.transition.AutoTransition().setDuration(350));
            if (isMapExpanded) {
                binding.sideActionPanel.setVisibility(View.VISIBLE);
                com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, binding.expandIcon, "ACTION_MINIMIZE");
                binding.dashboardGridZone.setVisibility(View.GONE);
                ViewGroup.LayoutParams rawLp = binding.mapCardContainer.getLayoutParams();
                if (rawLp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) rawLp;
                    lp.setMargins(0, 0, 0, 0);
                    binding.mapCardContainer.setLayoutParams(lp);
                }
            } else {
                binding.sideActionPanel.setVisibility(View.GONE);
                com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, binding.expandIcon, "ACTION_EXPAND");
                binding.dashboardGridZone.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams rawLp = binding.mapCardContainer.getLayoutParams();
                if (rawLp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) rawLp;
                    int rQuota = scale(4); // 🚀 Minimal boundary padding for maximum spatial efficiency
                    lp.setMargins(rQuota, rQuota, rQuota, rQuota);
                    binding.mapCardContainer.setLayoutParams(lp);
                }
            }
        });
        
        UiArchitect.applyText(this, binding.tvAddressMarquee, "TEXT_TITLE");
        // 🔧 THEME FIX: Use theme-aware neon color instead of hardcoded cyan
        int marqueeColor = ThemeManager.isDarkTheme(getResolvedThemeId()) ? getGradientStart() : getGradientEnd();
        binding.tvAddressMarquee.setTextColor(marqueeColor);
        android.graphics.drawable.GradientDrawable marqueeBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BL_TR,
            new int[]{ Color.argb(200, 8, 12, 20), Color.argb(240, 15, 23, 42) }
        );
        marqueeBg.setCornerRadius(scale(6));
        marqueeBg.setStroke(scale(1), Color.argb(160, Color.red(marqueeColor), Color.green(marqueeColor), Color.blue(marqueeColor)));
        binding.tvAddressMarquee.setBackground(marqueeBg);
        binding.tvAddressMarquee.setSelected(true);

        UiArchitect.applyCard(this, binding.dashboardGridZone, "CARD_DASHBOARD");
        int minPad = scale(4);
        binding.dashboardGridZone.setPadding(minPad, minPad, minPad, minPad);
        setupGridMatrix();
        
        binding.bottomNavZone.addView(UiArchitect.createBottomNav(this, new String[]{"NAV_NOTICE", "NAV_ALERT", "NAV_DEVICE", "NAV_ME"}, "NAV_DEVICE"));

        View drawerMenu = createDrawerContent();
        binding.drawerContent.addView(drawerMenu);

        return binding.getRoot();
    }

    @Override
    protected void onUiRebuilt(android.os.Bundle savedState) {
        SystemLogger.i(TAG, "L7 Lifecycle: Map Dashboard Re-Synchronizing after Rotation.");
        
        // 1. Re-Target and Re-Sync Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(com.jdroidx.jfindme.R.id.map_container);
        if (mapFragment != null) {
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

    private void setupGridMatrix() {
        TextView label = new TextView(this);
        label.setText("MONITORING TILE");
        label.setTextColor(getSecondaryTextColor());
        label.setTextSize(9f);
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(scale(6), scale(2), 0, scale(2));
        binding.dashboardGridZone.addView(label);

        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.setPadding(0, 0, 0, scale(2));
        r1.addView(UiArchitect.createTile(this, "NETWORK_LOCATOR", v -> openLocateDrawer()));
        r1.addView(UiArchitect.createTile(this, "SYNC_DEVICES",   v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.class))));
        r1.addView(UiArchitect.createTile(this, "LOCATION_LOGS",  v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.HistoryActivity.class))));
        r1.addView(UiArchitect.createTile(this, "PANIC_ALERTS",   v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.NotifActivity.class))));
        binding.dashboardGridZone.addView(r1);

        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        r2.setPadding(0, 0, 0, scale(2));
        r2.addView(UiArchitect.createTile(this, "SAFE_ZONES",     v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.GeofenceActivity.class))));
        r2.addView(UiArchitect.createTile(this, "FAMILY_FRIENDS", v -> startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class))));
        r2.addView(UiArchitect.createTile(this, "SETTINGS_HUB",   v -> startActivity(new Intent(this, SettingsActivity.class))));
        r2.addView(UiArchitect.createTile(this, "PEOPLE", v -> {
            String current = viewModel.getCurrentFilterMode();
            String next = "FAMILY"; // Start with Family
            if (current.equals("FAMILY")) next = "FRIENDS";
            else if (current.equals("FRIENDS")) next = "FAMILY";
            
            viewModel.setFilterMode(next);
            Toast.makeText(this, "TAGGED MESH: " + next, Toast.LENGTH_SHORT).show();
        }));
        binding.dashboardGridZone.addView(r2);

        // 🦋 Smooth Splash Continuity: Keep Angel butterfly fluttering smoothly for 1s after main screen appears
        if (getIntent() != null && getIntent().getBooleanExtra("EXTRA_FROM_SPLASH", false)) {
            getIntent().removeExtra("EXTRA_FROM_SPLASH");
            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Connecting to Satellite Matrix...");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                com.jdroidx.jfindme.utils.AngelLoader.hide(this);
            }, 1000);
        }
    }

    private void checkPendingSilentUpdateNotification() {
        boolean unacked = com.jdroidx.jfindme.utils.RegistryManager.getBoolean(this, com.jdroidx.jfindme.utils.RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, false);
        if (unacked) {
            com.jdroidx.jfindme.utils.RegistryManager.putBoolean(this, com.jdroidx.jfindme.utils.RegistryManager.KEY_SILENT_UPDATE_UNACKNOWLEDGED, false);
            String status = com.jdroidx.jfindme.utils.RegistryManager.getString(this, com.jdroidx.jfindme.utils.RegistryManager.KEY_SILENT_UPDATE_LAST_STATUS, "");
            String time = com.jdroidx.jfindme.utils.RegistryManager.getString(this, com.jdroidx.jfindme.utils.RegistryManager.KEY_SILENT_UPDATE_LAST_TIME, "");
            String err = com.jdroidx.jfindme.utils.RegistryManager.getString(this, com.jdroidx.jfindme.utils.RegistryManager.KEY_SILENT_UPDATE_LAST_ERROR, "");

            if ("UPDATE_READY".equals(status)) {
                Toast.makeText(this, "📦 System Update Ready to Install! (Downloaded at " + time + ")", Toast.LENGTH_LONG).show();
            } else if ("SIGNATURE_MISMATCH".equals(status)) {
                UiArchitect.showValidationDialog(this,
                    "⚠️ System Update Notice",
                    "A new release build was downloaded at " + time + ".\n\n" +
                    "Because this phone is running a development Debug build, please open Settings > System Update or install the fresh Release APK.",
                    () -> {
                        startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.SystemUpdateActivity.class));
                    });
            } else if ("STORAGE_FULL".equals(status)) {
                Toast.makeText(this, "⚠️ Background update skipped: Insufficient storage space.", Toast.LENGTH_SHORT).show();
            } else if ("DOWNLOAD_FAILED".equals(status)) {
                Toast.makeText(this, "⚠️ Background update download could not complete: " + err, Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Global cleanup successfully managed at the header variables!

    private void showMapLayersDialog() {
        if (googleMap == null) {
            Toast.makeText(this, "Map engine initializing...", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean safeZonesVisible = prefs.getBoolean("show_safe_zones_overlay", true);
        boolean spiderLinesVisible = prefs.getBoolean("show_cluster_spiderweb", false);

        String[] layerOptions = new String[]{
            "🗺️ Vector Normal Map (Default)",
            "🛰️ Satellite High-Res Imagery",
            "🏙️ Hybrid (Satellite + Street Labels)",
            "⛰️ Topographic / Terrain Contours",
            googleMap.isTrafficEnabled() ? "🚦 Disable Live Traffic Overlay" : "🚦 Enable Live Traffic Overlay",
            googleMap.isBuildingsEnabled() ? "🏛️ Hide 3D Buildings" : "🏛️ Show 3D Buildings",
            safeZonesVisible ? "🛡️ Hide Safe Zones On Map" : "🛡️ Show Safe Zones On Map",
            spiderLinesVisible ? "🕸️ Hide Cluster Shape Lines" : "🕸️ Show Cluster Shape Lines",
            "🔄 Clear Traveled Path Trail"
        };

        int currentType = googleMap.getMapType();
        int selectedIndex = 0;
        if (currentType == GoogleMap.MAP_TYPE_SATELLITE) selectedIndex = 1;
        else if (currentType == GoogleMap.MAP_TYPE_HYBRID) selectedIndex = 2;
        else if (currentType == GoogleMap.MAP_TYPE_TERRAIN) selectedIndex = 3;

        UiArchitect.showOptionsDialog(this, "🗺️ MAP DISPLAY & LAYERS", layerOptions, selectedIndex, (which, selectedLabel) -> {
            switch (which) {
                case 0:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    UiArchitect.showSlideNotificationBanner(this, "Layer Changed", "Vector Normal Map", false, 2500, null, null);
                    break;
                case 1:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    UiArchitect.showSlideNotificationBanner(this, "Layer Changed", "Satellite Imagery", false, 2500, null, null);
                    break;
                case 2:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    UiArchitect.showSlideNotificationBanner(this, "Layer Changed", "Hybrid Satellite Map", false, 2500, null, null);
                    break;
                case 3:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    UiArchitect.showSlideNotificationBanner(this, "Layer Changed", "Topographic Terrain", false, 2500, null, null);
                    break;
                case 4:
                    boolean traffic = !googleMap.isTrafficEnabled();
                    googleMap.setTrafficEnabled(traffic);
                    UiArchitect.showSlideNotificationBanner(this, "Traffic Layer", traffic ? "Live Traffic Enabled" : "Traffic Disabled", false, 2500, null, null);
                    break;
                case 5:
                    boolean buildings = !googleMap.isBuildingsEnabled();
                    googleMap.setBuildingsEnabled(buildings);
                    UiArchitect.showSlideNotificationBanner(this, "3D Buildings", buildings ? "3D Architectural Meshes Enabled" : "Buildings Hidden", false, 2500, null, null);
                    break;
                case 6:
                    boolean nowZones = !safeZonesVisible;
                    prefs.edit().putBoolean("show_safe_zones_overlay", nowZones).commit();
                    renderSafeZonesOnMap(viewModel != null ? viewModel.getSafeZones().getValue() : null);
                    UiArchitect.showSlideNotificationBanner(this, "Safe Zones", nowZones ? "Perimeter Overlays Displayed" : "Perimeter Overlays Hidden", false, 2500, null, null);
                    break;
                case 7:
                    boolean nowSpider = !spiderLinesVisible;
                    prefs.edit().putBoolean("show_cluster_spiderweb", nowSpider).commit();
                    if (viewModel != null && viewModel.getUserLocations().getValue() != null && googleMap != null) {
                        com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.processViewModelLocations(
                            this, googleMap, viewModel.getUserLocations().getValue()
                        );
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Spiderweb Clusters", nowSpider ? "Cluster Links Visible" : "Cluster Links Hidden", false, 2500, null, null);
                    break;
                case 8:
                    pathPoints.clear();
                    if (movementTrail != null) {
                        movementTrail.remove();
                        movementTrail = null;
                    }
                    UiArchitect.showSlideNotificationBanner(this, "Path Reset", "Local traveled trail cleared.", false, 2500, null, null);
                    break;
            }
        });
    }

    private void triggerEmergencySosDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("🚨 EMERGENCY SOS SENTINEL");
        builder.setMessage("Broadcasting emergency SOS beacon and high-precision GPS telemetry to all registered guardians in 5 seconds...\n\nTap CANCEL to abort.");
        
        final android.app.AlertDialog dialog = builder.setNegativeButton("CANCEL (ABORT)", (d, w) -> {
            d.dismiss();
            Toast.makeText(this, "SOS Broadcast Aborted.", Toast.LENGTH_SHORT).show();
        }).create();
        
        final int[] countdown = {5};
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing()) return;
                countdown[0]--;
                if (countdown[0] > 0) {
                    dialog.setMessage("Broadcasting emergency SOS beacon to guardians in " + countdown[0] + " seconds...\n\nTap CANCEL to abort.");
                    com.jdroidx.jfindme.engine.alert.AlertManager.vibrate(MapActivity.this, 150L);
                    handler.postDelayed(this, 1000);
                } else {
                    dialog.dismiss();
                    if (viewModel != null) {
                        viewModel.toggleSOS(MapActivity.this, true);
                    }
                    com.jdroidx.jfindme.engine.alert.AcousticBeaconEngine.startSiren(MapActivity.this, 30, "Local SOS Broadcast");
                    Toast.makeText(MapActivity.this, "🚨 EMERGENCY SOS BROADCASTED TO ALL GUARDIANS!", Toast.LENGTH_LONG).show();
                }
            }
        };
        dialog.show();
        com.jdroidx.jfindme.engine.alert.AlertManager.vibrate(MapActivity.this, 200L);
        handler.postDelayed(runnable, 1000);
    }

    private void showTacticalTargetSearchDialog() {
        if (viewModel == null) return;
        List<UserLocation> locs = viewModel.getUserLocations().getValue();
        if (locs == null || locs.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("📡 No Connected Devices Found")
                .setMessage("There are currently no active tracking nodes in your network group.\n\nWould you like to pair a new device or scan a QR pairing code?")
                .setPositiveButton("🔗 PAIR DEVICE", (d, w) -> {
                    startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.class));
                })
                .setNeutralButton("🔄 REFRESH SYNC", (d, w) -> {
                    if (viewModel != null) viewModel.refreshLocations();
                    Toast.makeText(this, "Scanning network matrix...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("CANCEL", null)
                .show();
            return;
        }

        openLocateDrawer();
    }

    private void openLocateDrawer() {
        if (viewModel == null || viewModel.getUserLocations().getValue() == null) {
            Toast.makeText(this, "No active targets locked in network.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<UserLocation> rawLocs = viewModel.getUserLocations().getValue();
        if (rawLocs.isEmpty()) {
            Toast.makeText(this, "Zero connections active.", Toast.LENGTH_SHORT).show();
            return;
        }

        Location refLoc = null;
        if (viewModel != null && viewModel.getMyLocation().getValue() != null) {
            refLoc = viewModel.getMyLocation().getValue();
        }

        final Location myRef = refLoc;
        String myUid = FirebaseAuth.getInstance().getUid();

        // Sort peers by proximity to current device
        List<UserLocation> sortedLocs = new java.util.ArrayList<>(rawLocs);
        if (myRef != null) {
            java.util.Collections.sort(sortedLocs, (a, b) -> {
                float[] resA = new float[1];
                float[] resB = new float[1];
                Location.distanceBetween(myRef.getLatitude(), myRef.getLongitude(), a.getLatitude(), a.getLongitude(), resA);
                Location.distanceBetween(myRef.getLatitude(), myRef.getLongitude(), b.getLatitude(), b.getLongitude(), resB);
                return Float.compare(resA[0], resB[0]);
            });
        }

        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding((int)(12 * dm.density), (int)(8 * dm.density), (int)(12 * dm.density), (int)(12 * dm.density));
        scroller.addView(listLayout);

        android.app.AlertDialog radarDialog = new android.app.AlertDialog.Builder(this)
            .setTitle("📡 Tactical Proximity Radar (" + sortedLocs.size() + " Nodes)")
            .setView(scroller)
            .setPositiveButton("🗺️ FRAME ALL", (d, w) -> {
                if (googleMap != null && sortedLocs.size() > 0) {
                    com.google.android.gms.maps.model.LatLngBounds.Builder boundsBuilder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
                    for (UserLocation uLoc : sortedLocs) {
                        if (uLoc.getLatitude() != 0.0 || uLoc.getLongitude() != 0.0) {
                            boundsBuilder.include(new LatLng(uLoc.getLatitude(), uLoc.getLongitude()));
                        }
                    }
                    if (myRef != null) boundsBuilder.include(new LatLng(myRef.getLatitude(), myRef.getLongitude()));
                    try {
                        com.google.android.gms.maps.model.LatLngBounds bounds = boundsBuilder.build();
                        int padding = (int)(64 * dm.density);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                    } catch (Exception ignored) {}
                }
            })
            .setNegativeButton("Close", null)
            .create();

        // 🚀 PRIORITY 1: FIND MY CURRENT LOCATION (Center & Lock Self Fix)
        LinearLayout mySelfCard = new LinearLayout(this);
        mySelfCard.setOrientation(LinearLayout.HORIZONTAL);
        mySelfCard.setGravity(Gravity.CENTER_VERTICAL);
        mySelfCard.setPadding((int)(12 * dm.density), (int)(12 * dm.density), (int)(12 * dm.density), (int)(12 * dm.density));
        LinearLayout.LayoutParams selfCardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        selfCardLp.setMargins(0, 0, 0, (int)(8 * dm.density));
        mySelfCard.setLayoutParams(selfCardLp);

        android.graphics.drawable.GradientDrawable selfCardBg = new android.graphics.drawable.GradientDrawable();
        selfCardBg.setCornerRadius((int)(12 * dm.density));
        selfCardBg.setColor(Color.argb(35, 0, 230, 118));
        selfCardBg.setStroke((int)(2 * dm.density), Color.parseColor("#00E676"));
        mySelfCard.setBackground(selfCardBg);
        mySelfCard.setFocusable(true);
        mySelfCard.setClickable(true);

        ImageView selfIcon = new ImageView(this);
        selfIcon.setImageResource(android.R.drawable.ic_menu_mylocation);
        selfIcon.setColorFilter(Color.parseColor("#00E676"));
        mySelfCard.addView(selfIcon, new LinearLayout.LayoutParams((int)(36 * dm.density), (int)(36 * dm.density)));

        LinearLayout selfInfoCol = new LinearLayout(this);
        selfInfoCol.setOrientation(LinearLayout.VERTICAL);
        selfInfoCol.setPadding((int)(12 * dm.density), 0, 0, 0);
        selfInfoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView selfTitle = new TextView(this);
        selfTitle.setText("📍 1. FIND CURRENT LOCATION (MY DEVICE)");
        selfTitle.setTextColor(Color.parseColor("#00E676"));
        selfTitle.setTextSize(13);
        selfTitle.setTypeface(null, Typeface.BOLD);
        selfInfoCol.addView(selfTitle);

        TextView selfSub = new TextView(this);
        if (myRef != null) {
            selfSub.setText(String.format(java.util.Locale.US, "Lat: %.5f, Lng: %.5f • Instant GPS Lock", myRef.getLatitude(), myRef.getLongitude()));
        } else {
            selfSub.setText("Tap to center & lock camera to current live fix");
        }
        selfSub.setTextColor(getTextColor());
        selfSub.setTextSize(11);
        selfInfoCol.addView(selfSub);

        mySelfCard.addView(selfInfoCol);

        mySelfCard.setOnClickListener(v -> {
            radarDialog.dismiss();
            recenterCamera();
            Toast.makeText(this, "📍 Locked to My Current Location", Toast.LENGTH_SHORT).show();
        });

        listLayout.addView(mySelfCard);

        String myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String myHwUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(this);
        String myRawAndroidId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getRawAndroidId(this);

        for (final UserLocation u : sortedLocs) {
            String uid = u.getResolvedUserId();
            boolean isSelf = (uid.equals(myUid) || 
                              uid.equals(myAgentId) || 
                              uid.equals(myHwUid) || 
                              uid.equals(myRawAndroidId) || 
                              uid.equalsIgnoreCase("You") || 
                              uid.equals("SELF_USER"));
            if (isSelf) continue; // 🚀 Deduplication: Skip self since mySelfCard is already placed at the top!

            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
            String ssotAvatar = (u.getAvatar_id() != null && !u.getAvatar_id().isEmpty()) ? u.getAvatar_id() : db.getDeviceCentricAvatar(uid);
            String ssotName = db.getDeviceCentricName(uid);
            String displayName = (ssotName != null && !ssotName.startsWith("Agent_") && !ssotName.equalsIgnoreCase("null")) ? ssotName : u.getUserName();

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding((int)(10 * dm.density), (int)(10 * dm.density), (int)(10 * dm.density), (int)(10 * dm.density));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, (int)(4 * dm.density), 0, (int)(4 * dm.density));
            card.setLayoutParams(cardLp);

            android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
            cardBg.setCornerRadius((int)(12 * dm.density));
            cardBg.setColor(getCardColor());
            cardBg.setStroke((int)(1 * dm.density), Color.argb(40, 148, 163, 184));
            card.setBackground(cardBg);
            card.setFocusable(true);
            card.setClickable(true);

            // Avatar Thumbnail
            ImageView avatarImg = new ImageView(this);
            int avatarSize = (int)(44 * dm.density);
            LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(avatarSize, avatarSize);
            avLp.setMargins(0, 0, (int)(10 * dm.density), 0);
            avatarImg.setLayoutParams(avLp);
            avatarImg.setScaleType(ImageView.ScaleType.FIT_CENTER);

            com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, avatarImg, ssotAvatar);
            card.addView(avatarImg);

            // Info Column
            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            String role = !u.getRole_family().equals("None") ? u.getRole_family() : u.getRole_friend();
            if (!role.equals("None") && !role.isEmpty()) {
                displayName = displayName + " • " + role;
            }

            TextView nameTv = new TextView(this);
            nameTv.setText(displayName);
            nameTv.setTextColor(isSelf ? Color.parseColor("#3B82F6") : getTextColor());
            nameTv.setTextSize(13);
            nameTv.setTypeface(null, Typeface.BOLD);
            nameTv.setSingleLine(true);
            infoCol.addView(nameTv);

            // Motion & Distance Subtitle
            String distStr = "Positioned";
            if (myRef != null) {
                float[] distRes = new float[1];
                Location.distanceBetween(myRef.getLatitude(), myRef.getLongitude(), u.getLatitude(), u.getLongitude(), distRes);
                distStr = distRes[0] >= 1000 ? String.format(java.util.Locale.US, "%.1f km away", distRes[0] / 1000f) : String.format(java.util.Locale.US, "%.0f m away", distRes[0]);
            }

            float spd = u.getSpeed();
            String motionBadge = "⏱️ Resting";
            if (spd > 25f) {
                motionBadge = "🚗 " + Math.round(spd) + " km/h";
            } else if (spd > 7f) {
                motionBadge = "🏃 " + Math.round(spd) + " km/h";
            } else if (spd > 1.5f) {
                motionBadge = "🚶 Walking";
            }

            com.jdroidx.jfindme.utils.DeviceRagEngine.EvaluationResult rag = 
                com.jdroidx.jfindme.utils.DeviceRagEngine.evaluate(u.getLongTimestamp(), u.getBatteryLevel(), u.isSosActive());
            card.setAlpha(rag.getCardAlpha());

            TextView subTv = new TextView(this);
            subTv.setText(distStr + " • " + motionBadge + " • " + rag.getStatusText());
            subTv.setTextColor(rag.getStatusColor());
            subTv.setTextSize(11);
            infoCol.addView(subTv);

            card.addView(infoCol);

            // Battery Badge (if available)
            if (u.getBatteryLevel() >= 0) {
                TextView batBadge = new TextView(this);
                batBadge.setText(u.getBatteryLevel() + "% 🔋");
                batBadge.setTextColor(u.getBatteryLevel() < 20 ? Color.parseColor("#EF4444") : Color.parseColor("#00E676"));
                batBadge.setTextSize(10);
                batBadge.setTypeface(null, Typeface.BOLD);
                batBadge.setPadding((int)(6 * dm.density), (int)(3 * dm.density), (int)(6 * dm.density), (int)(3 * dm.density));
                card.addView(batBadge);
            }

            card.setOnClickListener(v -> {
                radarDialog.dismiss();
                if (isSelf) {
                    recenterCamera();
                } else {
                    showTacticalTargetActionSheet(u);
                }
            });

            listLayout.addView(card);
        }

        radarDialog.show();
        applyDialogTheme(radarDialog);
    }

    private void showTacticalTargetActionSheet(UserLocation target) {
        if (target == null) return;
        String targetUid = target.getResolvedUserId();
        String myUid = FirebaseAuth.getInstance().getUid();

        if (targetUid.equals(myUid)) {
            recenterCamera();
            return;
        }

        String resolvedName = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getDeviceCentricName(targetUid);
        if (resolvedName == null || resolvedName.startsWith("Agent_") || resolvedName.isEmpty()) {
            resolvedName = target.getUserName();
        }
        final String finalTargetName = resolvedName;

        String role = !target.getRole_family().equals("None") ? target.getRole_family() : target.getRole_friend();
        String roleTag = (!role.equals("None") && !role.isEmpty()) ? " [" + role + "]" : "";

        UiArchitect.OptionItem[] targetActions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("📍 Find Current Location (Center & Track Live)", "Lock camera directly on target's live coordinate stream", android.R.drawable.ic_menu_mylocation),
            new UiArchitect.OptionItem("🧭 Turn-by-Turn Navigation", "Launch Google Maps directions to target", android.R.drawable.ic_dialog_map),
            new UiArchitect.OptionItem("⏱️ View Location History", "Inspect chronological path timeline and telemetry logs", com.jdroidx.jfindme.R.drawable.ic_location_history),
            new UiArchitect.OptionItem("🛡️ Assign / Create Safe Zone", "Configure geofence boundary perimeter for this member", com.jdroidx.jfindme.R.drawable.ic_safe_zone),
            new UiArchitect.OptionItem("🔊 Ring Device (Acoustic Siren)", "Trigger high-volume emergency siren (30s) on target device", android.R.drawable.ic_lock_idle_alarm),
            new UiArchitect.OptionItem("👤 View Member Details & Roles", "Open dossier, ACL permissions, and contact details", com.jdroidx.jfindme.R.drawable.ic_user_profile),
            new UiArchitect.OptionItem("📋 Copy GPS Coordinates", "Copy exact latitude and longitude to clipboard", android.R.drawable.ic_menu_save),
            new UiArchitect.OptionItem("❌ Unlink / Remove Device", "Uncouple this device from your active tracking network", android.R.drawable.ic_menu_delete)
        };

        UiArchitect.showRichOptionsDialog(this, "🎯 TARGET: " + finalTargetName.toUpperCase() + roleTag, targetActions, (actIdx, actLabel) -> {
            switch (actIdx) {
                case 0: // Find Current Location (Center & Track Live + Remote GPS Wakeup)
                    com.jdroidx.jfindme.data.remote.FirebaseComm.triggerRemoteLocateDevice(targetUid);
                    LatLng finalPos = new LatLng(target.getLatitude(), target.getLongitude());
                    if (googleMap != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(finalPos, 17f));
                    }
                    updateAddressMarquee(target);
                    UiArchitect.showSlideNotificationBanner(this, "🛰️ GPS Locate Ping Sent", "Sent high-accuracy location request to " + finalTargetName + ". Awaiting fresh fix...", false, 3500, null, null);
                    break;
                case 1: // Navigate
                    navigateToTarget(target.getLatitude(), target.getLongitude(), target.getUserName());
                    break;
                case 2: // History
                    Intent hIntent = new Intent(this, com.jdroidx.jfindme.ui.management.HistoryActivity.class);
                    hIntent.putExtra("target_uid", targetUid);
                    startActivity(hIntent);
                    break;
                case 3: // Safe Zone
                    Intent zIntent = new Intent(this, com.jdroidx.jfindme.ui.management.GeofenceActivity.class);
                    zIntent.putExtra("target_uid", targetUid);
                    zIntent.putExtra("TARGET_LAT", target.getLatitude());
                    zIntent.putExtra("TARGET_LNG", target.getLongitude());
                    zIntent.putExtra("TARGET_NAME", target.getUserName());
                    startActivity(zIntent);
                    break;
                case 4: // Acoustic Ring Siren
                    sendAcousticPing(targetUid, finalTargetName);
                    UiArchitect.showSlideNotificationBanner(this, "Acoustic Siren Dispatched", "30-second emergency siren sent to " + finalTargetName + ".", false, 4000, null, null);
                    break;
                case 5: // Person Details
                    Intent pIntent = new Intent(this, com.jdroidx.jfindme.ui.group.PersonDetailsActivity.class);
                    pIntent.putExtra("TARGET_UID", targetUid);
                    startActivity(pIntent);
                    break;
                case 6: // Copy Coordinates
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    String coordStr = String.format(java.util.Locale.US, "%.6f, %.6f", target.getLatitude(), target.getLongitude());
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Coordinates", coordStr));
                    Toast.makeText(this, "📋 Coordinates Copied: " + coordStr, Toast.LENGTH_SHORT).show();
                    break;
                case 7: // Unlink / Delete
                    showUnlinkDeviceDialog(targetUid, finalTargetName);
                    break;
            }
        });
    }

    private void showUnlinkDeviceDialog(String targetUid, String targetName) {
        String safetyPhrase = RegistryManager.getPurgeSafetyPhrase(this);
        UiArchitect.OptionItem[] unlinkOptions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("❌ Standard Unlink Device", "Safely remove device from active tracking and local database", android.R.drawable.ic_menu_delete),
            new UiArchitect.OptionItem("🛡️ Master Password Force Purge", "Administrative override with safety phrase to instantly wipe corrupted or orphan nodes", com.jdroidx.jfindme.R.drawable.ic_privacy_lock)
        };

        UiArchitect.showRichOptionsDialog(this, "Remove " + targetName + "?", unlinkOptions, (idx, label) -> {
            if (idx == 0) {
                // Standard Unlink
                com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
                db.deleteRosterNode(targetUid);
                String myGrp = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);
                if (myGrp != null) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("devices").child(targetUid).removeValue();
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("members").child(targetUid).removeValue();
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("citizen_dossiers").child(targetUid).removeValue();
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("direct_pairs").child(targetUid).removeValue();
                }
                String finalMyId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(finalMyId).child(targetUid).removeValue();
                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(targetUid).child(finalMyId).removeValue();
                Toast.makeText(this, "Device " + targetName + " unlinked successfully.", Toast.LENGTH_SHORT).show();
            } else {
                // Master Password Purge
                UiArchitect.showInputPromptDialog(this,
                    "Master Admin Authorization",
                    "Enter master security password to force purge '" + targetName + "' (" + targetUid + "):",
                    "",
                    inputPass -> {
                        String currentPhrase = RegistryManager.getPurgeSafetyPhrase(this);
                        if (currentPhrase.equals(inputPass != null ? inputPass.trim() : "")) {
                            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
                            db.deleteRosterNode(targetUid);
                            String myGrp = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);
                            if (myGrp != null) {
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("devices").child(targetUid).removeValue();
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("members").child(targetUid).removeValue();
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("citizen_dossiers").child(targetUid).removeValue();
                                com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(myGrp).child("direct_pairs").child(targetUid).removeValue();
                            }
                            String finalMyId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(finalMyId).child(targetUid).removeValue();
                            com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef(targetUid).child(finalMyId).removeValue();
                            Toast.makeText(this, "🛡️ Force Administrative Purge Completed for " + targetName, Toast.LENGTH_LONG).show();
                        } else {
                            UiArchitect.showValidationDialog(this, "Authorization Denied", "Incorrect master password. Device deletion was blocked.", null);
                        }
                    });
            }
        });
    }

    private void navigateToTarget(double lat, double lng, String targetName) {
        try {
            android.net.Uri gmmIntentUri = android.net.Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=d");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent fallback = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng));
                startActivity(fallback);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Direct navigation link opened for " + targetName, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendAcousticPing(String targetUid, String targetName) {
        if (targetUid == null || targetUid.isEmpty() || viewModel == null) return;
        String myName = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this);
        int sirenDuration = com.jdroidx.jfindme.utils.RegistryManager.getSirenDurationSec(this);
        viewModel.pokeUser(targetUid, myName);
        com.jdroidx.jfindme.data.remote.FirebaseComm.triggerRemoteRingDevice(targetUid, sirenDuration);
        Toast.makeText(this, "🔊 Acoustic Siren Dispatched to " + targetName + " (" + sirenDuration + "s)", Toast.LENGTH_LONG).show();
    }

    private void showSelfActionSheet() {
        com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier currentTier = 
            com.jdroidx.jfindme.engine.security.PrivacyEngine.getPrivacyTier(this);
        String tierLabel = currentTier == com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier.PRECISE ? "🟢 PRECISE" :
                           currentTier == com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier.FUZZY ? "🟡 FUZZY (GHOST JITTER)" : "🔵 FROZEN (LAST KNOWN)";

        UiArchitect.OptionItem[] selfActions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("👤 My Profile & 3D Avatar", "Edit identity alias, photo, bio, and credentials", com.jdroidx.jfindme.R.drawable.ic_user_profile),
            new UiArchitect.OptionItem("🔒 Privacy & Ghost Mode [" + tierLabel + "]", "Switch between Precise, Fuzzy (~500m jitter), and Frozen", com.jdroidx.jfindme.R.drawable.ic_privacy_lock),
            new UiArchitect.OptionItem("📍 Recenter Map on My GPS", "Lock viewport to current GNSS coordinates", android.R.drawable.ic_menu_mylocation),
            new UiArchitect.OptionItem("🚨 Emergency SOS Sentinel", "Broadcast 5-second countdown distress beacon to all guardians", android.R.drawable.ic_dialog_alert),
            new UiArchitect.OptionItem("📋 Copy My GPS Coordinates", "Copy current latitude and longitude to clipboard", android.R.drawable.ic_menu_save)
        };

        UiArchitect.showRichOptionsDialog(this, "👑 YOU (HOST DEVICE)", selfActions, (idx, label) -> {
            switch (idx) {
                case 0:
                    startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
                    break;
                case 1:
                    showPrivacyTierSelectorDialog();
                    break;
                case 2:
                    recenterCamera();
                    break;
                case 3:
                    triggerEmergencySosDialog();
                    break;
                case 4:
                    if (lastKnownLocation != null) {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        String coordStr = String.format(java.util.Locale.US, "%.6f, %.6f", lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("Coordinates", coordStr));
                        Toast.makeText(this, "📋 Coordinates Copied: " + coordStr, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        });
    }

    private void showPrivacyTierSelectorDialog() {
        UiArchitect.OptionItem[] tiers = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("🟢 PRECISE (Live GPS Pinpoint)", "Exact meter-level telemetry shared with guardians and circles", com.jdroidx.jfindme.R.drawable.ic_nav_map),
            new UiArchitect.OptionItem("🟡 FUZZY (Neighborhood Jitter ~500m)", "Deterministic circular jitter to protect home privacy while staying reachable", com.jdroidx.jfindme.R.drawable.ic_privacy_lock),
            new UiArchitect.OptionItem("🔵 FROZEN (Ghost Mode)", "Locks your location marker to the last fixed point without streaming new updates", com.jdroidx.jfindme.R.drawable.ic_safe_zone)
        };

        UiArchitect.showRichOptionsDialog(this, "Select Privacy Tier", tiers, (idx, label) -> {
            com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier selected;
            switch (idx) {
                case 0: selected = com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier.PRECISE; break;
                case 1: selected = com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier.FUZZY; break;
                default: selected = com.jdroidx.jfindme.engine.security.PrivacyEngine.PrivacyTier.FROZEN; break;
            }
            com.jdroidx.jfindme.engine.security.PrivacyEngine.setPrivacyTier(this, selected);
            Toast.makeText(this, "Privacy Mode Updated: " + selected.name(), Toast.LENGTH_SHORT).show();
        });
    }
    
    private com.google.android.gms.maps.model.Polyline currentProximityLine;
    private String activeProximityTargetId = null; // 🚀 QA Fix: Master Tracking Lock

    private void updateAddressMarquee(UserLocation ul) {
        if (ul == null) return;
        String timeString = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(ul.getLongTimestamp()));
        String role = (ul.getRole_family() != null && !ul.getRole_family().equals("None")) ? ul.getRole_family() : 
                      (ul.getRole_friend() != null && !ul.getRole_friend().equals("None")) ? ul.getRole_friend() : "";
        updateAddressMarquee(ul.getLatitude(), ul.getLongitude(), ul.getUserName(), timeString, ul.getSpeed(), ul.getBatteryLevel(), ul.isSosActive(), role);
    }

    private void updateAddressMarquee(double lat, double lng, String targetName, String timeStr) {
        updateAddressMarquee(lat, lng, targetName, timeStr, 0f, -1, false, "");
    }

    private void updateAddressMarquee(double lat, double lng, String targetName, String timeStr, float speedMps, int batteryLevel, boolean isSos, String role) {
        if (tvAddressMarquee != null) {
            tvAddressMarquee.setText("🛰️ Locating " + targetName + " • Acquiring forensic fix • • •");
            tvAddressMarquee.setSelected(true);
        }
        
        android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1, new android.location.Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@androidx.annotation.NonNull java.util.List<android.location.Address> addresses) {
                    processMarqueeAddress(addresses, lat, lng, targetName, timeStr, speedMps, batteryLevel, isSos, role);
                }
                @Override
                public void onError(@androidx.annotation.Nullable String errorMessage) {
                    processMarqueeAddress(null, lat, lng, targetName, timeStr, speedMps, batteryLevel, isSos, role);
                }
            });
        } else {
            new Thread(() -> {
                try {
                    @SuppressWarnings("deprecation")
                    java.util.List<android.location.Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    runOnUiThread(() -> processMarqueeAddress(addresses, lat, lng, targetName, timeStr, speedMps, batteryLevel, isSos, role));
                } catch (Exception e) {
                    runOnUiThread(() -> processMarqueeAddress(null, lat, lng, targetName, timeStr, speedMps, batteryLevel, isSos, role));
                }
            }).start();
        }
    }

    private void processMarqueeAddress(java.util.List<android.location.Address> addresses, double lat, double lng, String targetName, String timeStr, float speedMps, int batteryLevel, boolean isSos, String role) {
        if (tvAddressMarquee == null) return;
        String addressStr;
        if (addresses != null && !addresses.isEmpty()) {
            addressStr = addresses.get(0).getAddressLine(0);
        } else {
            addressStr = String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng);
        }
        
        StringBuilder sb = new StringBuilder();
        if (isSos) {
            sb.append("🚨 SOS ALERT ACTIVE • ");
        }
        sb.append("🛰️ ").append(targetName != null && !targetName.isEmpty() ? targetName : "Active Node");
        if (role != null && !role.isEmpty() && !role.equalsIgnoreCase("None")) {
            sb.append(" [").append(role).append("]");
        }
        sb.append(" • 🟢 Live (").append(timeStr).append(")");
        sb.append(" • 📍 ").append(addressStr);
        
        // Speed conversion
        float speedKmh = speedMps * 3.6f;
        if (speedKmh > 1.5f) {
            sb.append(String.format(java.util.Locale.US, " • 🚗 %.1f km/h", speedKmh));
        } else {
            sb.append(" • 🛑 Static (0.0 km/h)");
        }
        
        // Battery status
        if (batteryLevel >= 0) {
            sb.append(" • 🔋 ").append(batteryLevel).append("%");
        }
        
        sb.append(" • 🎯 GPS Lock • • •        ");
        
        String finalPayload = sb.toString();
        tvAddressMarquee.setText(finalPayload);
        tvAddressMarquee.setSelected(true);
    }

    private View createSmallIcon(String tableKey, View.OnClickListener listener) {
        // ≡ƒÜÇ QA Fix: Global Sync! Use the Architect's 5-Stage scaling engine instead of ad-hoc density prefs.
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

        // ≡ƒÜÇ QA Fix: Global Focus State Loop (Enter / D-Pad Support)
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
        // ≡ƒº▒ UX Override: Architectural lifecycle fix -> Safely init after views configure
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);
        
        // ≡ƒöÆ Back-Stack Protocol: Double-Tap Exit on Live Dashboard
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
        
        SupportMapFragment oldFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_CONTAINER);
        if (oldFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(oldFragment).commitNowAllowingStateLoss();
        }
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(ID_MAP_CONTAINER, mapFragment)
                .commitNowAllowingStateLoss();
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
            // ≡ƒÜÇ QA Fix: Strict MVVM Execution bypassing rogue Firebase Listeners
            if (googleMap != null) {
                com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.processViewModelLocations(this, googleMap, locations);
            }
            
            // 🚀 Tactical Zoom Lock for Friend / Peer Locate
            if (selectedUserId != null && !hasZoomedToTarget && googleMap != null) {
                locateTargetWithHistoryFallback(selectedUserId);
            }

            updateRadarScanner(locations);
            
            long count = Math.max(0, locations.size() - 1); // exclude self
            // ≡ƒÜÇ QA Fix: Legacy Proximity Counter decommissioned in favor of Logo Matrix.
            
            // ≡ƒÜÇ QA Fix: Proximity Flipper Removed (Situational Awareness consolidated to Markers)

            String selfUid = FirebaseAuth.getInstance().getUid();
            for (UserLocation ul : locations) {
                if (ul == null) continue;
                String targetUid = ul.getResolvedUserId();
                if (ul.isSosActive() && targetUid != null && (selfUid == null || !targetUid.equals(selfUid))) {
                    AlertManager.showSosNotification(this, ul.getUserName());
                    SystemLogger.w(TAG, "INCOMING_SOS: " + ul.getUserName());
                }
                
                if (selfUid != null && selfUid.equals(targetUid) && ul.isPoked()) {
                    UiArchitect.showSlideNotificationBanner(this, "POKED ALERT", (ul.getPokedBy() != null ? ul.getPokedBy() : "Guardian") + " is looking for you!", true, 5000, null, null);
                    AlertManager.triggerLocalSOSFeedback(this);
                    viewModel.clearPoke();
                }
            }
        });
        
        viewModel.getPendingSafeZoneRequestsCount().observe(this, count -> {
            updateNavBarBadge("NAV_NOTICE", count);
        });
    }

    private void zoomToTargetUser(List<com.jdroidx.jfindme.data.model.UserLocation> locations) {
        if (selectedUserId == null || googleMap == null || locations == null) return;
        for (com.jdroidx.jfindme.data.model.UserLocation ul : locations) {
            String rId = ul.getResolvedUserId();
            String dId = ul.getResolvedDeviceId();
            String uName = ul.getUserName();
            
            if (selectedUserId.equalsIgnoreCase(rId) || 
                selectedUserId.equalsIgnoreCase(dId) || 
                (uName != null && selectedUserId.equalsIgnoreCase(uName))) {
                
                com.google.android.gms.maps.model.LatLng target = new com.google.android.gms.maps.model.LatLng(ul.getLatitude(), ul.getLongitude());
                com.google.android.gms.maps.model.CameraPosition pos = new com.google.android.gms.maps.model.CameraPosition.Builder()
                    .target(target).zoom(17f).tilt(45f).build();
                googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(pos));
                hasZoomedToTarget = true;
                updateAddressMarquee(ul);
                SystemLogger.i(TAG, "Successfully locked camera onto Target: " + ul.getUserName() + " (" + selectedUserId + ")");
                break;
            }
        }
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
        UiArchitect.applyMapStyle(this, this.googleMap);
        
        // 🚀 Dynamic Anti-Overlap Zoom Hook: Recomputes screen-space spiderfication as map zooms/pans
        this.googleMap.setOnCameraIdleListener(() -> {
            if (viewModel != null && viewModel.getUserLocations().getValue() != null && this.googleMap != null) {
                com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.processViewModelLocations(
                    this, this.googleMap, viewModel.getUserLocations().getValue()
                );
            }
        });
        
        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.getTitle() == null && marker.getTag() == null) return true;
            
            String selfUid = FirebaseAuth.getInstance().getUid();
            String selfAgent = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
            String rawTitle = marker.getTitle() != null ? marker.getTitle() : "";
            String markerTag = marker.getTag() != null ? marker.getTag().toString() : "";
            
            if (rawTitle.equalsIgnoreCase("You") || (!markerTag.isEmpty() && (markerTag.equals(selfUid) || markerTag.equals(selfAgent)))) {
                showSelfActionSheet();
                return true;
            }
            
            String markerSnippet = marker.getSnippet() != null ? marker.getSnippet() : "";
            
            List<UserLocation> locs = viewModel.getUserLocations().getValue();
            if (locs != null) {
                com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
                for (UserLocation l : locs) {
                    String uid = l.getResolvedUserId();
                    String devCentricName = db.getDeviceCentricName(uid);
                    String devId = l.getResolvedDeviceId();
                    if ((!markerTag.isEmpty() && (markerTag.equals(uid) || (!devId.isEmpty() && markerTag.equals(devId))))
                        || (!markerSnippet.isEmpty() && (markerSnippet.contains(uid) || (!devId.isEmpty() && markerSnippet.contains(devId))))
                        || rawTitle.equals(l.getUserName())
                        || rawTitle.equals(devCentricName)) {
                        selectedUserId = uid;
                        updateAddressMarquee(l);
                        showTacticalTargetActionSheet(l);
                        return true;
                    }
                }
            }
            
            return true;
        });

        googleMap.setOnInfoWindowClickListener(marker -> {
            if (marker.getTitle() == null || marker.getTitle().equalsIgnoreCase("You")) return;
            List<UserLocation> locs = viewModel.getUserLocations().getValue();
            if (locs != null) {
                for (UserLocation l : locs) {
                    if (marker.getTitle().equals(l.getUserName())) {
                        showTacticalTargetActionSheet(l);
                        break;
                    }
                }
            }
        });

        googleMap.setOnMapClickListener(latLng -> {
            // Dismiss active overlays natively here when needed.
            if (currentProximityLine != null) {
                 currentProximityLine.remove();
                 currentProximityLine = null;
                 activeProximityTargetId = null; // Sever telemetry lock!
            }
        });
        
        // 🛰️ V50: Live Safe Zone Overlays on Google Map
        viewModel.getSafeZones().observe(this, this::renderSafeZonesOnMap);
        
        // 🚨 Geofence Breach Sentinel HUD Observer
        viewModel.getGeofenceAlertTrigger().observe(this, alertMsg -> {
            if (alertMsg != null && !alertMsg.isEmpty()) {
                com.jdroidx.jfindme.engine.alert.AlertManager.vibrate(this, 300L);
                showGeofenceBreachBanner(alertMsg);
                UiArchitect.showSlideNotificationBanner(this, "🚨 PERIMETER ALERT", alertMsg, true, 4000, "INSPECT", () -> {
                    startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.NotifActivity.class));
                });
            }
        });

        googleMap.setOnCircleClickListener(circle -> {
            Object tag = circle.getTag();
            if (tag instanceof com.jdroidx.jfindme.data.model.SafeZoneModel) {
                com.jdroidx.jfindme.data.model.SafeZoneModel z = (com.jdroidx.jfindme.data.model.SafeZoneModel) tag;
                UiArchitect.showConfirmationDialog(this,
                    "🛡️ SAFE ZONE: " + z.getName().toUpperCase(),
                    "Radius: " + (int) z.getRadiusMeters() + "m\nCoordinates: " + String.format(java.util.Locale.US, "%.6f, %.6f", z.getLatitude(), z.getLongitude()),
                    "MANAGE ZONE", "CLOSE",
                    () -> {
                        Intent intent = new Intent(this, com.jdroidx.jfindme.ui.management.GeofenceActivity.class);
                        intent.putExtra("TARGET_ZONE_ID", z.getZoneId());
                        intent.putExtra("TARGET_ZONE_NAME", z.getName());
                        startActivity(intent);
                    }, null);
            }
        });

        googleMap.setOnPolygonClickListener(polygon -> {
            Object tag = polygon.getTag();
            if (tag instanceof com.jdroidx.jfindme.data.model.SafeZoneModel) {
                com.jdroidx.jfindme.data.model.SafeZoneModel z = (com.jdroidx.jfindme.data.model.SafeZoneModel) tag;
                UiArchitect.showConfirmationDialog(this,
                    "🛡️ POLYGON ZONE: " + z.getName().toUpperCase(),
                    "Custom Multi-Point Perimeter Boundary\nTarget: " + z.getAssignedName(),
                    "MANAGE ZONE", "CLOSE",
                    () -> {
                        Intent intent = new Intent(this, com.jdroidx.jfindme.ui.management.GeofenceActivity.class);
                        intent.putExtra("TARGET_ZONE_ID", z.getZoneId());
                        intent.putExtra("TARGET_ZONE_NAME", z.getName());
                        startActivity(intent);
                    }, null);
            }
        });

        // 🛰️ Real-Time Offline Queue Observer
        com.jdroidx.jfindme.data.remote.OfflineSyncManager.getInstance(this)
            .getPendingQueueCountLiveData().observe(this, count -> {
                if (count != null && count > 0) {
                    if (tvAddressMarquee != null) {
                        tvAddressMarquee.setText("⚡ " + count + " TELEMETRY POINTS CACHED OFFLINE • AWAITING NETWORK SYNC • • •");
                        tvAddressMarquee.setSelected(true);
                    }
                }
            });
        
        checkLocationPermission();
        
        // Initial Zoom Fix - Dynamically resolve from Registry / Last Known Location
        double defLat = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultLatitude(this);
        double defLng = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultLongitude(this);
        float defZoom = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultZoom(this);
        LatLng defaultLocation = new LatLng(defLat, defLng);
        com.google.android.gms.maps.model.CameraPosition pos = new com.google.android.gms.maps.model.CameraPosition.Builder()
            .target(defaultLocation).zoom(defZoom).tilt(45f).build();
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));

        if (selectedUserId != null && !selectedUserId.isEmpty()) {
            locateTargetWithHistoryFallback(selectedUserId);
        }
    }

    private void showGeofenceBreachBanner(String alertMsg) {
        UiArchitect.showConfirmationDialog(this,
            "🚨 PERIMETER ALERT",
            alertMsg,
            "VIEW LOGS", "DISMISS",
            () -> {
                Intent intent = new Intent(this, com.jdroidx.jfindme.ui.management.GeofenceNotificationActivity.class);
                startActivity(intent);
            }, null);
    }

    private void renderSafeZonesOnMap(List<com.jdroidx.jfindme.data.model.SafeZoneModel> zones) {
        if (googleMap == null) return;
        for (Circle c : safeZoneCircles) {
            try { c.remove(); } catch (Exception ignored) {}
        }
        safeZoneCircles.clear();
        for (Polygon p : safeZonePolygons) {
            try { p.remove(); } catch (Exception ignored) {}
        }
        safeZonePolygons.clear();

        if (zones == null || zones.isEmpty()) return;
        if (prefs != null && !prefs.getBoolean("show_safe_zones_overlay", true)) return;

        int strokeCol = getGradientStart();
        int fillCol = Color.argb(45, Color.red(strokeCol), Color.green(strokeCol), Color.blue(strokeCol));
        for (com.jdroidx.jfindme.data.model.SafeZoneModel z : zones) {
            if (!z.isZoneActive()) continue;

            // Check if polygon zone
            if (z.isPolygon()) {
                List<LatLng> polyPoints = com.jdroidx.jfindme.engine.location.GisSpatialEngine.parsePolygonJson(z.getPolygonPointsJson());
                if (polyPoints.size() >= 3) {
                    Polygon polygon = googleMap.addPolygon(new PolygonOptions()
                        .addAll(polyPoints)
                        .strokeColor(strokeCol)
                        .strokeWidth(4f)
                        .fillColor(fillCol)
                        .clickable(true));
                    polygon.setTag(z);
                    safeZonePolygons.add(polygon);
                    continue;
                }
            }

            if (z.getLatitude() == 0.0 && z.getLongitude() == 0.0) continue;
            LatLng center = new LatLng(z.getLatitude(), z.getLongitude());
            double rad = z.getRadiusMeters() > 0 ? z.getRadiusMeters() : 100.0;
            Circle circle = googleMap.addCircle(new CircleOptions()
                .center(center)
                .radius(rad)
                .strokeColor(strokeCol)
                .strokeWidth(4f)
                .fillColor(fillCol)
                .clickable(true));
            circle.setTag(z);
            safeZoneCircles.add(circle);
        }
    }

    private static final int FOREGROUND_LOCATION_PERMISSION_CODE = 1001;
    private static final int BACKGROUND_LOCATION_PERMISSION_CODE = 1002;
    private static final int NOTIFICATION_PERMISSION_CODE = 1003;

    private void checkLocationPermission() {
        SystemLogger.i(TAG, "Executing [checkLocationPermission] hardware audit logic.");
        if (SecurityEngine.INSTANCE.isRooted()) {
            Toast.makeText(this, "SECURITY ALERT: Rooted Device Detected.", Toast.LENGTH_LONG).show();
            SystemLogger.w(TAG, "DEVICE_INTEGRITY_COMPROMISED");
        }
        
        // Step 1: Check Foreground Location (Fine & Coarse)
        boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasFine) {
            java.util.List<String> fgPerms = new java.util.ArrayList<>();
            fgPerms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !hasCoarse) {
                fgPerms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            ActivityCompat.requestPermissions(this, fgPerms.toArray(new String[0]), FOREGROUND_LOCATION_PERMISSION_CODE);
            return;
        }

        // Foreground location is granted! Immediately unlock map & active GPS features
        enableLocationFeatures();

        // Step 2: Request Notifications on Android 13+ (Non-blocking)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                return;
            }
        }

        // Step 3: Handle Background Location on Android 10+ gracefully
        checkBackgroundLocationPermission();
    }

    private void checkBackgroundLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                boolean hasPrompted = getSharedPreferences("system_registry", MODE_PRIVATE).getBoolean("bg_location_prompted", false);
                if (!hasPrompted) {
                    getSharedPreferences("system_registry", MODE_PRIVATE).edit().putBoolean("bg_location_prompted", true).apply();
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("🛡️ Background Location Protection")
                        .setMessage("For 24/7 family safety, geofence sentinel alerts, and SOS tracking even when jFindMe is in the background, please select \"Allow all the time\" in Android Settings.")
                        .setPositiveButton("CONTINUE", (d, w) -> {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
                        })
                        .setNegativeButton("LATER", null)
                        .show();
                }
            }
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

                // Synchronize Access Permissions Matrix and Version Update Metadata to Cloud Firebase RTDB (Only if authenticated)
                String agentId = com.jdroidx.jfindme.utils.RegistryManager.getAgentId(this);
                if (!agentId.isEmpty() && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.syncDeviceAccessPermissions(this, agentId);
                    com.jdroidx.jfindme.data.remote.FirebaseComm.syncVersionUpdateMetadata(this, agentId, "INSTALLED");
                }
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
        
        if (requestCode == FOREGROUND_LOCATION_PERMISSION_CODE || requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SystemLogger.i(TAG, "Foreground location granted by user. Booting features.");
                enableLocationFeatures();
                // Check post-notifications & background location gracefully
                checkLocationPermission();
            } else {
                SystemLogger.w(TAG, "Foreground location denied by user.");
                UiArchitect.showPermissionRequiredDialog(this, "Location Permission Required",
                    "Live tracking and map navigation require location permission. Please enable Location in Settings to continue.", null);
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            // Non-blocking notification permission response: proceed to check background location
            checkBackgroundLocationPermission();
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Background tracking secured: Allow all the time ✅", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUserPositionOnMap(Location location) {
        if (googleMap == null || location == null) return;
        this.lastKnownLocation = location;
        com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.saveLastKnownPosition(this, location.getLatitude(), location.getLongitude());
        LatLng myPos = new LatLng(location.getLatitude(), location.getLongitude());
        pathPoints.add(myPos);
        if (pathPoints.size() > 50) pathPoints.remove(0);

        // MultiMarkerEngine owns the unified animated rendering of all nodes (including Self)
        if (userAvatar != null) {
            try { userAvatar.remove(); } catch (Exception ignored) {}
            userAvatar = null;
        }
        
        if (interactionRadius != null) {
            try { interactionRadius.remove(); } catch (Exception ignored) {}
            interactionRadius = null;
        }
        
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
            tvLocName.setText("ME | " + String.format(java.util.Locale.US, "%.6f, %.6f", location.getLatitude(), location.getLongitude()));
            tvLocName.setTextColor(getGradientStart()); // Sync with Flipper Theme
        }

        // ≡ƒÜÇ QA Fix: Master Battery Sync -> Update the ME status card in the flipper!
        if (tvBatteryPer != null) {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = registerReceiver(null, ifilter);
            int level = batteryStatus != null ? batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) : -1;
            String timeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
            tvBatteryPer.setText(timeStr + " | " + (level >= 0 ? level + "% 🔋" : "CALIBRATING"));
            tvBatteryPer.setTextColor(getTextColor());
        }

        if (viewModel != null && viewModel.getUserLocations() != null && viewModel.getUserLocations().getValue() != null) {
            updateRadarScanner(viewModel.getUserLocations().getValue());
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
        
        float speed = (lastKnownLocation != null) ? lastKnownLocation.getSpeed() : 0f;
        float bearing = (lastKnownLocation != null) ? lastKnownLocation.getBearing() : 0f;
        
        android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
        android.content.Intent bIntent = registerReceiver(null, ifilter);
        int battery = (bIntent != null) ? bIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) : -1;
        
        android.graphics.Bitmap bmp = com.jdroidx.jfindme.utils.AvatarLoader.getCurrentUserAvatarBitmap(this, 256);
        return com.jdroidx.jfindme.utils.AvatarCalloutGenerator.generate(
            this, displayName, bmp, showName, true, 0, "DEFAULT", speed, bearing, battery, false, System.currentTimeMillis()
        );
    }
    
    private void triggerCalloutRefresh() {
        if (prefs != null) prefs.edit().putLong("callout_revealed_at", System.currentTimeMillis()).apply();
        if (lastKnownLocation != null) updateUserPositionOnMap(lastKnownLocation);
        
        if (prefs != null && prefs.getBoolean("callout_hide_mode", false)) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (lastKnownLocation != null) updateUserPositionOnMap(lastKnownLocation);
            }, 5100);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 🔋 Screen Active: Activate high accuracy for active user interaction
        com.jdroidx.jfindme.engine.EngineRegistry.INSTANCE.activate(com.jdroidx.jfindme.engine.EngineRegistry.ENGINE_LOCATION);
        com.jdroidx.jfindme.engine.EngineRegistry.INSTANCE.activate(com.jdroidx.jfindme.engine.EngineRegistry.ENGINE_MAP_RENDER);
        checkPendingSilentUpdateNotification();
        
        String currentSsot = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this);
        if (binding != null && binding.tvNickName != null) {
            binding.tvNickName.setText(currentSsot.toUpperCase());
        }
        if (navDrawerTitle != null) {
            navDrawerTitle.setText(currentSsot);
        }
        
        // 🚀 QA Fix: Master Callout Refresh Engine (V14.1)
        triggerCalloutRefresh(); 
        
        // 🚀 QA Fix: Master Hardware Enforcement (V14.1)
        validateLocationHardwareStatus();
        
        if (headerAvatar != null) com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, headerAvatar);
        if (navDrawerAvatar != null) com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, navDrawerAvatar);
        
        if (viewModel != null) {
            viewModel.refreshLocations();
            if (googleMap != null) {
                renderSafeZonesOnMap(viewModel.getSafeZones().getValue());
                if (viewModel.getUserLocations().getValue() != null) {
                    com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.processViewModelLocations(
                        this, googleMap, viewModel.getUserLocations().getValue()
                    );
                }
            }
        }
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
        
        Location referenceLoc = lastKnownLocation;
        if (referenceLoc == null && viewModel != null) {
            referenceLoc = viewModel.getMyLocation().getValue();
        }
        if (referenceLoc == null) {
            // Fallback to SQLite last saved position so devices immediately appear on launch
            android.content.SharedPreferences regPrefs = getSharedPreferences("system_registry", MODE_PRIVATE);
            double savedLat = (double) regPrefs.getFloat("last_known_lat", 0f);
            double savedLng = (double) regPrefs.getFloat("last_known_lng", 0f);
            if (savedLat != 0.0 || savedLng != 0.0) {
                referenceLoc = new Location("fallback");
                referenceLoc.setLatitude(savedLat);
                referenceLoc.setLongitude(savedLng);
            }
        }

        String myUid = FirebaseAuth.getInstance().getUid();
        String myAgentId = getSharedPreferences("system_registry", MODE_PRIVATE).getString("agent_id", "");
        
        java.util.Set<String> activeDeviceIds = new java.util.HashSet<>();

        // 1. In-place update or insert peer device cards (Sorted by Waterfall Priority: SOS emergency first, then newest to oldest fix)
        if (locations != null) {
            List<UserLocation> sortedList = new ArrayList<>(locations);
            java.util.Collections.sort(sortedList, (a, b) -> {
                if (a == null || b == null) return 0;
                if (a.isSosActive() != b.isSosActive()) {
                    return a.isSosActive() ? -1 : 1; // SOS priority
                }
                return Long.compare(b.getLongTimestamp(), a.getLongTimestamp()); // Newest first
            });

            for (UserLocation ul : sortedList) {
                if (ul == null) continue;
                String deviceId = ul.getResolvedUserId();
                if (deviceId.isEmpty() || deviceId.equals(myUid) || deviceId.equals(myAgentId)) continue;
                activeDeviceIds.add(deviceId);

                String distText = "LIVE";
                int row1Color = getGradientStart();
                if (referenceLoc != null && (ul.getLatitude() != 0.0 || ul.getLongitude() != 0.0)) {
                    float[] results = new float[1];
                    Location.distanceBetween(referenceLoc.getLatitude(), referenceLoc.getLongitude(), 
                                            ul.getLatitude(), ul.getLongitude(), results);
                    float distance = results[0];
                    distText = distance >= 1000f ? String.format(java.util.Locale.US, "%.1f km away", distance / 1000f) : String.format(java.util.Locale.US, "%.0f m away", distance);
                    if (distance > RADIUS_METERS) {
                        row1Color = Color.parseColor("#38BDF8"); // Vibrant Sky Blue for external nodes
                    }
                }

                // Speed / Motion telemetry
                float spd = ul.getSpeed();
                String motionStr = "";
                if (spd > 25f) {
                    motionStr = " • 🚗 " + Math.round(spd) + " km/h";
                } else if (spd > 7f) {
                    motionStr = " • 🏃 " + Math.round(spd) + " km/h";
                } else if (spd > 1.5f) {
                    motionStr = " • 🚶 Walk";
                }

                long ts = ul.getLongTimestamp();
                String timeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(ts));

                String displayName = ul.getUserName().trim().isEmpty() ? ("AGENT_" + deviceId) : ul.getUserName().trim();
                String row1Text = displayName.toUpperCase() + " | " + distText;
                String bat = ul.getBatteryLevel() >= 0 ? (ul.getBatteryLevel() + "% 🔋") : "ONLINE ⚡";
                String row2Text = timeStr + " | " + bat + motionStr;

                final UserLocation targetUl = ul;
                View existingCard = headerFlipper.findViewWithTag(deviceId);
                if (existingCard instanceof LinearLayout) {
                    LinearLayout card = (LinearLayout) existingCard;
                    if (card.getChildCount() >= 2 && card.getChildAt(0) instanceof TextView && card.getChildAt(1) instanceof TextView) {
                        TextView tv1 = (TextView) card.getChildAt(0);
                        TextView tv2 = (TextView) card.getChildAt(1);
                        tv1.setText(row1Text);
                        tv1.setTextColor(row1Color);
                        tv2.setText(row2Text);
                        tv2.setTextColor(getTextColor());
                    }
                    card.setOnClickListener(v -> {
                        if (googleMap != null && (targetUl.getLatitude() != 0.0 || targetUl.getLongitude() != 0.0)) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(targetUl.getLatitude(), targetUl.getLongitude()), 17f));
                        }
                        updateAddressMarquee(targetUl);
                    });
                } else {
                    LinearLayout memberCard = new LinearLayout(this);
                    memberCard.setTag(deviceId);
                    memberCard.setOrientation(LinearLayout.VERTICAL);
                    memberCard.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                    memberCard.setPadding(0, 0, 0, 0); 
                    memberCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    memberCard.setClickable(true);
                    memberCard.setFocusable(true);

                    TextView tvRow1 = new TextView(this);
                    tvRow1.setText(row1Text);
                    tvRow1.setTextColor(row1Color);
                    tvRow1.setTextSize(scaleText(11f)); 
                    tvRow1.setTypeface(null, Typeface.BOLD);
                    tvRow1.setGravity(Gravity.END);
                    tvRow1.setSingleLine(true);
                    tvRow1.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    memberCard.addView(tvRow1);

                    TextView tvRow2 = new TextView(this);
                    tvRow2.setText(row2Text);
                    tvRow2.setTextColor(getTextColor());
                    tvRow2.setTextSize(scaleText(9.5f)); 
                    tvRow2.setGravity(Gravity.END);
                    tvRow2.setAlpha(0.95f);
                    tvRow2.setTypeface(null, Typeface.BOLD);
                    tvRow2.setSingleLine(true);
                    tvRow2.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    memberCard.addView(tvRow2);

                    memberCard.setOnClickListener(v -> {
                        if (googleMap != null && (targetUl.getLatitude() != 0.0 || targetUl.getLongitude() != 0.0)) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(targetUl.getLatitude(), targetUl.getLongitude()), 17f));
                        }
                        updateAddressMarquee(targetUl);
                    });

                    headerFlipper.addView(memberCard);
                }
            }
        }

        // 2. Remove stale cards for disconnected devices (preserve child 0 which is status_card)
        for (int i = headerFlipper.getChildCount() - 1; i >= 1; i--) {
            View child = headerFlipper.getChildAt(i);
            Object tag = child.getTag();
            if (tag != null && !activeDeviceIds.contains(tag.toString())) {
                headerFlipper.removeViewAt(i);
            }
        }

        // 3. Smooth sequential auto-flipping management (4-second interval)
        headerFlipper.setFlipInterval(4000);
        if (headerFlipper.getChildCount() > 1) {
            if (!headerFlipper.isFlipping()) {
                headerFlipper.startFlipping();
            }
        } else {
            if (headerFlipper.isFlipping()) {
                headerFlipper.stopFlipping();
            }
            if (headerFlipper.getDisplayedChild() != 0) {
                headerFlipper.setDisplayedChild(0);
            }
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
        // ≡ƒÜÇ QA Fix: Global Horizontal Header Segmentation (V11)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        applyGlobalHeaderGeometry(header); 
        header.setPadding(scale(16), scale(20), scale(16), scale(20));
        
        // --- 1. Left Segment: Avatar Engine (Best Fit) ---
        ImageView drawerAvatar = new ImageView(this);
        navDrawerAvatar = drawerAvatar;
        com.jdroidx.jfindme.utils.AvatarLoader.bindCurrentUserAvatar(this, navDrawerAvatar);
        
        int avatarSize = (int)(getResponsiveIconQuota() * 1.2f);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avLp.setMargins(0, 0, scale(16), 0);
        header.addView(drawerAvatar, avLp);
        
        // --- 2. Right Segment: User Profile Hub ---
        LinearLayout profileInfo = new LinearLayout(this);
        profileInfo.setOrientation(LinearLayout.VERTICAL);
        profileInfo.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView appTitle = new TextView(this);
        navDrawerTitle = appTitle;
        appTitle.setTextColor(getGradientOverlayText()); 
        appTitle.setTextSize(scaleText(16f));
        appTitle.setTypeface(null, Typeface.BOLD);
        appTitle.setText(com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this));
        
        // 🚀 Drawer Identity Sync: Mirrors the Dossier state in the Nav Hub
        viewModel.getUserDossier().observe(this, dossier -> {
            if (dossier == null) return;
            appTitle.setText(com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this));
        });
        
        profileInfo.addView(appTitle);
        
        TextView appStatus = new TextView(this);
        appStatus.setText("MISSION ACTIVE");
        appStatus.setTextColor(getGradientOverlayText());
        appStatus.setAlpha(0.65f);
        appStatus.setTextSize(scaleText(10f));
        appStatus.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        profileInfo.addView(appStatus);
        
        header.addView(profileInfo);
        drawer.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.VERTICAL);

        // --- SUB-GROUP 1: TRACKING ---
        items.addView(createDrawerSectionHeader("TRACKING"));

        // 1. Dashboard (Command Overview)
        items.addView(createDrawerItem("Dashboard", "Mission Overview & Telemetry", com.jdroidx.jfindme.R.drawable.ic_locator_pin, v -> {
            if (isMapExpanded) {
                binding.expandIcon.performClick();
            }
            recenterCamera();
            drawerLayout.closeDrawers();
            Toast.makeText(this, "📊 Command Telemetry Dashboard", Toast.LENGTH_SHORT).show();
        }));

        // 2. Live Map (Spatial Tracking)
        items.addView(createDrawerItem("Live Map", "Real-Time Spatial Grid", com.jdroidx.jfindme.R.drawable.ic_nav_map, v -> {
            if (!isMapExpanded) {
                binding.expandIcon.performClick();
            }
            recenterCamera();
            drawerLayout.closeDrawers();
            Toast.makeText(this, "🛰️ Full-Screen Live Radar Active", Toast.LENGTH_SHORT).show();
        }));

        // 3. Location History (Chronological Trails)
        items.addView(createDrawerItem("Location History", "Review Traveled Paths", com.jdroidx.jfindme.R.drawable.ic_location_history, v -> { 
            startActivity(new Intent(this, HistoryActivity.class)); 
            drawerLayout.closeDrawers(); 
        }));

        // 4. Safe Zones (Geofencing Perimeters)
        items.addView(createDrawerItem("Safe Zones", "Geofence Boundaries & Perimeters", com.jdroidx.jfindme.R.drawable.ic_safe_zone, v -> { 
            startActivity(new Intent(this, GeofenceActivity.class)); 
            drawerLayout.closeDrawers(); 
        }));

        // --- SUB-GROUP 2: NETWORK ---
        items.addView(createDrawerSectionHeader("NETWORK"));

        // 5. Device Pairing (QR Pairing Gateway)
        items.addView(createDrawerItem("Device Pairing", "QR Scan & Link Network Nodes", com.jdroidx.jfindme.R.drawable.ic_pairing_matrix, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.class));
            drawerLayout.closeDrawers();
        }));

        // 6. Guardians / Family (Primary Family Circle)
        items.addView(createDrawerItem("Guardians / Family", "Primary Caregivers & Family Roster", com.jdroidx.jfindme.R.drawable.ic_family_friends, v -> { 
            Intent i = new Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class); 
            i.putExtra("REQUESTED_TAB", 0); 
            i.putExtra("FOCUS_CATEGORY", "GUARDIAN");
            startActivity(i); 
            drawerLayout.closeDrawers(); 
        }));

        // 7. Children Devices (Dependents & Kids)
        items.addView(createDrawerItem("Children Devices", "Kids & Monitored Dependents", com.jdroidx.jfindme.R.drawable.ic_people, v -> { 
            Intent i = new Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class); 
            i.putExtra("REQUESTED_TAB", 0); 
            i.putExtra("FOCUS_CATEGORY", "CHILD");
            startActivity(i); 
            drawerLayout.closeDrawers(); 
        }));

        // 8. MeetMyFriend Groups (Social Circles & Peer Network)
        items.addView(createDrawerItem("MeetMyFriend Groups", "Social Circles & Peer Network", com.jdroidx.jfindme.R.drawable.ic_family_friends, v -> { 
            Intent i = new Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class); 
            i.putExtra("REQUESTED_TAB", 1); 
            i.putExtra("FOCUS_CATEGORY", "FRIEND");
            startActivity(i); 
            drawerLayout.closeDrawers(); 
        }));

        // --- SUB-GROUP 3: SECURITY ---
        items.addView(createDrawerSectionHeader("SECURITY"));

        // 9. Notifications & Alerts (Incident Ledger)
        items.addView(createDrawerItem("Notifications", "System Alerts & SOS Ledger", com.jdroidx.jfindme.R.drawable.ic_nav_notice, v -> { 
            startActivity(new Intent(this, NotifActivity.class)); 
            drawerLayout.closeDrawers(); 
        }));

        // 10. Device Health & Fleet (Hardware Status)
        items.addView(createDrawerItem("Device Health", "Hardware & Battery Status", com.jdroidx.jfindme.R.drawable.ic_device_health, v -> { 
            startActivity(new Intent(this, HealthActivity.class)); 
            drawerLayout.closeDrawers(); 
        }));

        // --- SUB-GROUP 4: SYSTEM ---
        items.addView(createDrawerSectionHeader("SYSTEM"));

        // 11. Profile & Avatar (User Dossier)
        items.addView(createDrawerItem("Profile", "Identity Dossier & 3D Avatar", com.jdroidx.jfindme.R.drawable.ic_user_profile, v -> { 
            startActivity(new Intent(this, ProfileActivity.class)); 
            drawerLayout.closeDrawers(); 
        }));

        // 12. System Settings (App Preferences)
        items.addView(createDrawerItem("Settings", "Control Hub & Customization", com.jdroidx.jfindme.R.drawable.ic_settings_gears, v -> { 
            startActivity(new Intent(this, SettingsActivity.class)); 
            drawerLayout.closeDrawers(); 
        }));

        // 13. Privacy Controls (Security Permissions)
        items.addView(createDrawerItem("Privacy Controls", "Permissions & Security Locks", com.jdroidx.jfindme.R.drawable.ic_privacy_lock, v -> { 
            startActivity(new Intent(this, PrivacyActivity.class)); 
            drawerLayout.closeDrawers(); 
        }));

        // 14. Help & Support (Manual & FAQs)
        items.addView(createDrawerItem("Help & Support", "Knowledge Base & FAQs", com.jdroidx.jfindme.R.drawable.ic_help_support, v -> { 
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.HelpSupportActivity.class)); 
            drawerLayout.closeDrawers(); 
        }));

        // 15. About App (Architecture & Integrity)
        items.addView(createDrawerItem("About App", "Version & Architecture", com.jdroidx.jfindme.R.drawable.ic_about_app, v -> { 
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.AboutActivity.class)); 
            drawerLayout.closeDrawers(); 
        }));

        // --- SUB-GROUP 5: ACCOUNT ---
        items.addView(createDrawerSectionHeader("ACCOUNT"));

        // 16. Logout (Safe Session Disconnect)
        items.addView(createDrawerItem("Logout", "Secure Account Sign Out", com.jdroidx.jfindme.R.drawable.ic_logout, v -> {
            UiArchitect.showConfirmationDialog(this,
                "Confirm Logout",
                "Are you sure you want to securely log out of your Guardian Account? You will stop receiving tracking alerts.",
                "LOGOUT", "CANCEL",
                () -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, com.jdroidx.jfindme.ui.auth.AuthActivity.class));
                    finishAffinity();
                },
                null);
        }));

        // 17. Version & System Update Quick-Access Footer
        TextView tvVersionFooter = new TextView(this);
        tvVersionFooter.setText("jFindMe Enterprise V" + com.jdroidx.jfindme.BuildConfig.VERSION_NAME + " (Build " + com.jdroidx.jfindme.BuildConfig.VERSION_CODE + ")\nTap to Check for System Updates");
        tvVersionFooter.setTextColor(getSecondaryTextColor());
        tvVersionFooter.setTextSize(scaleText(10f));
        tvVersionFooter.setGravity(Gravity.CENTER_HORIZONTAL);
        tvVersionFooter.setPadding(scale(16), scale(20), scale(16), scale(25));
        tvVersionFooter.setClickable(true);
        tvVersionFooter.setFocusable(true);
        tvVersionFooter.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.SystemUpdateActivity.class));
        });
        items.addView(tvVersionFooter);

        scroll.addView(items);
        drawer.addView(scroll);
        return drawer;
    }

    private View createDrawerSectionHeader(String title) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(scale(16), scale(16), scale(16), scale(4));

        TextView tv = new TextView(this);
        tv.setText(title.toUpperCase(java.util.Locale.ROOT));
        tv.setTextColor(getGradientStart());
        tv.setTextSize(scaleText(9.5f));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.06f);
        header.addView(tv);

        View line = new View(this);
        line.setBackgroundColor(Color.parseColor("#33888888"));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scale(1));
        lineLp.setMargins(0, scale(4), 0, 0);
        header.addView(line, lineLp);

        return header;
    }

    private View createDrawerItem(String title, String desc, int iconRes, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(scale(16), scale(10), scale(16), scale(10));
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(listener);

        // ≡ƒÜÇ QA Fix: Global Focus State Loop (Enter / D-Pad Support)
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
        iv.setColorFilter(getGradientMid()); // ≡ƒÜÇ QA Chrominance Sync (Drawer Items)
        
        int dSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        item.addView(iv, new LinearLayout.LayoutParams(dSize, dSize));

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(scale(12), 0, 0, 0);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getTextColor()); // QA Fix: Shifts from Dark to Bright per Theme
        tvTitle.setTextSize(scaleText(13f));
        tvTitle.setTypeface(null, Typeface.BOLD);
        textLayout.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextColor(getSecondaryTextColor()); // QA Fix: Smooth Subtitle Shift
        tvDesc.setTextSize(scaleText(10f));
        textLayout.addView(tvDesc);

        item.addView(textLayout);
        return item;
    }

    private void recenterCamera() {
        if (googleMap != null && lastKnownLocation != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), 17f));
        }
    }

    private void launchPictureInPictureMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                try {
                    android.app.PictureInPictureParams.Builder pipBuilder = new android.app.PictureInPictureParams.Builder();
                    android.util.Rational aspectRatio = new android.util.Rational(16, 9);
                    pipBuilder.setAspectRatio(aspectRatio);
                    enterPictureInPictureMode(pipBuilder.build());
                } catch (Exception e) {
                    Toast.makeText(this, "Picture-in-Picture (PiP) mode failed to launch.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Picture-in-Picture (PiP) option is not available on this device.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Picture-in-Picture (PiP) requires Android 8.0+.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            if (binding.topHeroZone != null) binding.topHeroZone.setVisibility(View.GONE);
            if (binding.dashboardGridZone != null) binding.dashboardGridZone.setVisibility(View.GONE);
            if (binding.bottomNavZone != null) binding.bottomNavZone.setVisibility(View.GONE);
            if (binding.tvAddressMarquee != null) binding.tvAddressMarquee.setVisibility(View.GONE);
            if (binding.sideActionPanel != null) binding.sideActionPanel.setVisibility(View.GONE);
            if (binding.expandIcon != null) binding.expandIcon.setVisibility(View.GONE);
            if (binding.btnPipClose != null) {
                binding.btnPipClose.setVisibility(View.VISIBLE);
                binding.btnPipClose.setColorFilter(Color.WHITE);
                android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
                closeBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                closeBg.setColor(Color.parseColor("#99000000"));
                binding.btnPipClose.setBackground(closeBg);
            }
        } else {
            if (binding.topHeroZone != null) binding.topHeroZone.setVisibility(View.VISIBLE);
            if (binding.dashboardGridZone != null && !isMapExpanded) binding.dashboardGridZone.setVisibility(View.VISIBLE);
            if (binding.bottomNavZone != null) binding.bottomNavZone.setVisibility(View.VISIBLE);
            if (binding.tvAddressMarquee != null) binding.tvAddressMarquee.setVisibility(View.VISIBLE);
            if (binding.expandIcon != null) binding.expandIcon.setVisibility(View.VISIBLE);
            if (binding.btnPipClose != null) binding.btnPipClose.setVisibility(View.GONE);
        }
    }

    /**
     * 🎯 Tactical Device Locator: Zooms to live position if online, or falls back to latest recorded SQLite history.
     */
    private void locateTargetWithHistoryFallback(String targetUid) {
        if (targetUid == null || targetUid.isEmpty() || googleMap == null) return;
        
        // 1. Try finding live location from ViewModel
        List<UserLocation> liveLocs = (viewModel != null && viewModel.getUserLocations() != null) ? viewModel.getUserLocations().getValue() : null;
        UserLocation matchLive = null;
        if (liveLocs != null) {
            for (UserLocation ul : liveLocs) {
                if (ul != null && (targetUid.equals(ul.getResolvedUserId()) || targetUid.equals(ul.getResolvedDeviceId()))) {
                    if (ul.getLatitude() != 0.0 || ul.getLongitude() != 0.0) {
                        matchLive = ul;
                        break;
                    }
                }
            }
        }

        if (matchLive != null) {
            hasZoomedToTarget = true;
            LatLng targetPos = new LatLng(matchLive.getLatitude(), matchLive.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetPos, 17.5f));
            updateAddressMarquee(matchLive);
            Toast.makeText(this, "📍 Locked to live position of " + matchLive.getUserName(), Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Fallback: Query SQLite for latest historical location
        com.jdroidx.jfindme.data.local.DatabaseHelper localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this);
        java.util.Map<String, Object> latestHist = localDb.getLatestHistoricalLocation(targetUid);
        if (latestHist != null) {
            hasZoomedToTarget = true;
            double lat = (Double) latestHist.get("lat");
            double lng = (Double) latestHist.get("lng");
            long ts = (Long) latestHist.get("timestamp");
            String timeStr = new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(ts));
            
            LatLng histPos = new LatLng(lat, lng);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(histPos, 16.5f));
            
            googleMap.addMarker(new MarkerOptions()
                .position(histPos)
                .title("Last Known Fix: " + targetUid)
                .snippet("Offline since " + timeStr)
                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)));
                
            if (tvAddressMarquee != null) {
                tvAddressMarquee.setText("📡 Last Known Historical Fix for [" + targetUid + "] • " + timeStr + " (OFFLINE) • • •");
                tvAddressMarquee.setSelected(true);
            }
            Toast.makeText(this, "Target is offline. Showing latest recorded history (" + timeStr + ")", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Target " + targetUid + " is currently offline. No location history recorded.", Toast.LENGTH_SHORT).show();
        }
    }
}
