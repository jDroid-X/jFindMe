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
import android.util.Pair;
import java.util.Collections;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.model.DeviceRoleCatalog;
import com.jdroidx.jfindme.data.model.DeviceRoleEntry;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private androidx.activity.OnBackPressedCallback backPressedCallback;
    private double restoredCamLat = 0.0;
    private double restoredCamLng = 0.0;
    private float restoredCamZoom = 0f;
    private float restoredCamTilt = 0f;
    private float restoredCamBearing = 0f;
    
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
    private boolean isMapExpanded = true; // 🚀 Live Tactical Map: Opens Full Screen by Default

    // [Variables] cameraController | [Constants] None | [Dependencies] TacticalCameraController | [Integration Points] Viewport Navigation
    private com.jdroidx.jfindme.engine.map.TacticalCameraController cameraController;
    private static final int ID_MAP_CONTAINER = com.jdroidx.jfindme.R.id.map_container;
    private static final int LOCATION_PERMISSION_CODE = 101;
    @Deprecated
    private static final int RADIUS_METERS = 100;

    // 🛰️ Remote Locate State Machine Tracking (Human-in-the-Loop Closed-Loop)
    private String activeLocateTargetUid = null;
    private String activeLocateTargetName = null;
    private long initialTargetTs = 0L;
    private final android.os.Handler locateStateMachineHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable locateStage2Runnable = null;
    private Runnable locateStage3Runnable = null;
    private Runnable locateTimeoutRunnable = null;
    // 🛰️ Delayed GPS Fix Tracking (Post-Timeout Window)
    private String lastTimedOutTargetUid = null;
    private String lastTimedOutTargetName = null;
    private long lastTimedOutInitialTs = 0L;

    // ⚡ Map Render Throttle Handler (Eliminates UI-thread rendering lag)
    private final android.os.Handler mapRenderHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingRenderRunnable = null;
    private long lastTimedOutEpoch = 0L;
    private final String TAG = "MapActivity_Enterprise";

    // ⚡ Avatar Local Memory Cache & Async DB Persistence (Eliminates UI-thread SQLite reads/writes on Redmi Note 7)
    private final java.util.Map<String, String> localAvatarMemoryCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ExecutorService avatarDbExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    // 🦋 Butterfly Point Rendezvous & Interactive Map Selection
    // [Variables] pendingFocusButterflyPointId, isPickingButterflyLocation, onMapLocationPickedCallback
    // [Constants] EXTRA_FOCUS_BUTTERFLY_POINT_ID
    // [Dependencies] ButterflyPoint, ButterflyPointEngine, ButterflyPointDialog, DatabaseHelper
    // [Integration Points] MapActivity, ButterflyPointDialog, TrackingService, AlertManager
    public interface MapLocationPickedCallback {
        void onLocationPicked(double lat, double lng, String address);
    }
    private String pendingFocusButterflyPointId = null;
    private boolean isPickingButterflyLocation = false;
    private MapLocationPickedCallback onMapLocationPickedCallback = null;

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
        
        // 🗺️ OOPS MVC Camera Controller SSOT Initialization
        cameraController = new com.jdroidx.jfindme.engine.map.TacticalCameraController(this);
        cameraController.setCameraStateListener(new com.jdroidx.jfindme.engine.map.TacticalCameraController.CameraStateListener() {
            @Override
            public void onCameraModeChanged(com.jdroidx.jfindme.engine.map.TacticalCameraController.CameraMode newMode, String targetId) {
                SystemLogger.d(TAG, "TacticalCamera state shift: " + newMode + ", target: " + targetId);
            }

            @Override
            public void onTargetFocused(String targetId, String displayName, double lat, double lng, float zoom) {
                if (binding != null && binding.tvAddressMarquee != null && displayName != null) {
                    updateAddressMarquee(lat, lng, displayName, new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date()));
                }
            }
        });
        
        if (savedInstanceState != null) {
            isDualPaneActive = savedInstanceState.getBoolean("isDualPaneActive", true);
            isMapExpanded = savedInstanceState.getBoolean("isMapExpanded", true);
        } else {
            // Fallback: restore from RegistryManager (safeRecreate bypasses bundle)
            isDualPaneActive = com.jdroidx.jfindme.utils.RegistryManager.isDualPaneActive(this);
            // 🚀 Cold-Start Invariant: Always launch into full-screen Live Tactical Map on fresh startup
            boolean isFromSplash = getIntent() != null && getIntent().getBooleanExtra("EXTRA_FROM_SPLASH", false);
            if (isFromSplash) {
                isMapExpanded = true;
                com.jdroidx.jfindme.utils.RegistryManager.setMapExpanded(this, true);
            } else {
                isMapExpanded = com.jdroidx.jfindme.utils.RegistryManager.isMapExpanded(this);
            }
        }

        extractIntentTarget(getIntent());

        // 🧬 Closed-Loop Avatar & Profile Callback (LOOP-001)
        closedLoopAvatarCallback = (agentId, payload) -> {
            if (agentId != null) {
                localAvatarMemoryCache.remove(agentId);
            }
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
        if (binding != null && binding.logo != null && binding.logo.isAnimating()) {
            binding.logo.pauseAnimation();
        }
        if (binding != null && binding.headerFlipper != null && binding.headerFlipper.isFlipping()) {
            binding.headerFlipper.stopFlipping();
        }
        // Persist dual-pane state to RegistryManager so safeRecreate can recover it
        com.jdroidx.jfindme.utils.RegistryManager.setDualPaneActive(this, isDualPaneActive);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 🔋 Clean up foreground-only real-time peer sync listeners when Map is backgrounded
        com.jdroidx.jfindme.engine.map.ButterflyPointEngine.stopAllActivePointPeerSyncs();
        com.jdroidx.jfindme.engine.EngineRegistry.INSTANCE.heartbeat(com.jdroidx.jfindme.engine.EngineRegistry.ENGINE_LOCATION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            clearLocateStateMachine();
            com.jdroidx.jfindme.engine.map.ButterflyPointEngine.clearButterflyOverlay();
            com.jdroidx.jfindme.engine.map.ButterflyPointEngine.stopAllActivePointPeerSyncs();
        }
        com.jdroidx.jfindme.engine.EngineRegistry.INSTANCE.heartbeat(com.jdroidx.jfindme.engine.EngineRegistry.ENGINE_LOCATION);
        if (closedLoopAvatarCallback != null) {
            com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.unregisterClosedLoopCallback(closedLoopAvatarCallback);
        }
        if (mapRenderHandler != null) {
            mapRenderHandler.removeCallbacksAndMessages(null);
        }
        if (avatarDbExecutor != null && !avatarDbExecutor.isShutdown()) {
            avatarDbExecutor.shutdown();
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
            // 🦋 Butterfly Point Rendezvous Deep-Link Intent Handler
            String focusPtId = intent.getStringExtra("EXTRA_FOCUS_BUTTERFLY_POINT_ID");
            if (focusPtId == null) focusPtId = intent.getStringExtra("point_id");
            if (focusPtId != null && !focusPtId.trim().isEmpty()) {
                pendingFocusButterflyPointId = focusPtId.trim();
                SystemLogger.i(TAG, "Butterfly Point Focus Lock Engaged for ID: " + pendingFocusButterflyPointId);
                checkAndFocusPendingButterflyPoint();
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
        // [Variables] intent | [Constants] "LAUNCH_PANEL", "RADAR", "BUTTERFLY_POINT", "BUTTERFLY" | [Dependencies] Handler, Looper, ButterflyPointDialog | [Integration Points] MapActivity onNewIntent deep-link routing
        if (intent != null && "RADAR".equals(intent.getStringExtra("LAUNCH_PANEL"))) {
            intent.removeExtra("LAUNCH_PANEL");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::openLocateDrawer, 300);
        } else if (intent != null && ("BUTTERFLY_POINT".equals(intent.getStringExtra("LAUNCH_PANEL")) || "BUTTERFLY".equals(intent.getStringExtra("LAUNCH_PANEL")))) {
            intent.removeExtra("LAUNCH_PANEL");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> ButterflyPointDialog.show(this), 300);
        }
        checkAndFocusPendingButterflyPoint();
    }

    private void checkAndFocusPendingButterflyPoint() {
        if (pendingFocusButterflyPointId != null && googleMap != null) {
            final String ptId = pendingFocusButterflyPointId;
            pendingFocusButterflyPointId = null;
            new Thread(() -> {
                com.jdroidx.jfindme.data.model.ButterflyPoint pt =
                    com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this).getButterflyPoint(ptId);
                if (pt != null) {
                    runOnUiThread(() -> {
                        focusOnButterflyPoint(pt, true);
                        ButterflyPointDialog.show(this);
                    });
                }
            }).start();
        }
    }

    @Override
    protected void onSaveCurrentState(Bundle outState) {
        super.onSaveCurrentState(outState);
        if (googleMap != null) {
            try {
                com.google.android.gms.maps.model.CameraPosition pos = googleMap.getCameraPosition();
                outState.putDouble("saved_cam_lat", pos.target.latitude);
                outState.putDouble("saved_cam_lng", pos.target.longitude);
                outState.putFloat("saved_cam_zoom", pos.zoom);
                outState.putFloat("saved_cam_tilt", pos.tilt);
                outState.putFloat("saved_cam_bearing", pos.bearing);
            } catch (Exception ignored) {}
        }
        if (selectedUserId != null) {
            outState.putString("saved_selected_user_id", selectedUserId);
        }
        if (activeLocateTargetUid != null) {
            outState.putString("saved_active_locate_uid", activeLocateTargetUid);
            outState.putString("saved_active_locate_name", activeLocateTargetName);
            outState.putLong("saved_active_locate_initial_ts", initialTargetTs);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isDualPaneActive", isDualPaneActive);
        outState.putBoolean("isMapExpanded", isMapExpanded);
        // Also persist to RegistryManager for safeRecreate resilience
        com.jdroidx.jfindme.utils.RegistryManager.setDualPaneActive(this, isDualPaneActive);
        com.jdroidx.jfindme.utils.RegistryManager.setMapExpanded(this, isMapExpanded);
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
                // 🔋 Battery Optimization: Play twice gracefully on screen open, then rest (prevents CPU/GPU burning 45 minutes)
                binding.logo.setRepeatCount(2);
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            binding.mapCardContainer.setClipToOutline(true);
        }
        
        // [Variables] isDarkMap, dockBg, dockPadH, dockPadV | [Constants] None | [Dependencies] GradientDrawable, Color, ThemeManager | [Integration Points] sideActionPanel
        boolean isDarkMap = ThemeManager.isDarkTheme(getResolvedThemeId());
        android.graphics.drawable.GradientDrawable dockBg = new android.graphics.drawable.GradientDrawable();
        dockBg.setCornerRadius(scale(18));
        if (isDarkMap) {
            dockBg.setColor(Color.parseColor("#CC0F172A")); // Frosted slate glass (Dark Theme)
            dockBg.setStroke(scale(1), Color.parseColor("#3338BDF8")); // Subtle neon cyan glow
        } else {
            dockBg.setColor(Color.argb(230, 255, 255, 255)); // Frosted pure white porcelain glaze (Light Theme)
            dockBg.setStroke(scale(1), Color.parseColor("#3364748B")); // Subtle mineral slate border
        }
        binding.sideActionPanel.setBackground(dockBg);
        int dockPadH = scale(3);
        int dockPadV = scale(4);
        binding.sideActionPanel.setPadding(dockPadH, dockPadV, dockPadH, dockPadV);
        binding.sideActionPanel.setElevation(scale(8));
        
        binding.sideActionPanel.removeAllViews();
        // [Variables] view, shareIcon, ghostIcon | [Constants] "ACTION_LOCATE", "ACTION_DEVICE_SHARING", "ACTION_GHOST" | [Dependencies] createSmallIcon, showDeviceSharingHubDialog, ButterflyPointDialog | [Integration Points] sideActionPanel
        binding.sideActionPanel.addView(createSmallIcon("ACTION_LOCATE", view -> recenterCamera()));
        View shareIcon = createSmallIcon("ACTION_DEVICE_SHARING", view -> showDeviceSharingHubDialog());
        shareIcon.setOnLongClickListener(v -> {
            shareIcon.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            frameAllDevices();
            return true;
        });
        binding.sideActionPanel.addView(shareIcon);
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
            } else {
                Toast.makeText(this, "Syncing tracking nodes...", Toast.LENGTH_SHORT).show();
            }
        }));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_ADD", view -> {
            if (cameraController != null) {
                cameraController.zoomIn();
            } else if (googleMap != null) {
                googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomIn());
            } else {
                Toast.makeText(this, "Map engine initializing...", Toast.LENGTH_SHORT).show();
            }
        }));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_MINUS", view -> {
            if (cameraController != null) {
                cameraController.zoomOut();
            } else if (googleMap != null) {
                googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.zoomOut());
            } else {
                Toast.makeText(this, "Map engine initializing...", Toast.LENGTH_SHORT).show();
            }
        }));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_LAYERS", view -> showMapLayersDialog()));
        binding.sideActionPanel.addView(createSmallIcon("ACTION_ALERT", view -> triggerEmergencySosDialog()));
        // Note: Safe Zone button removed per operator specification (SAFEZone not required).
        // 👻 Refactored: Ghost Icon for Butterfly Meet Point (Live Map Right Action Panel) with Long-Press Ghost Mode Fallback
        View ghostIcon = createSmallIcon("ACTION_GHOST", view -> {
            ButterflyPointDialog.show(this);
        });
        ghostIcon.setOnLongClickListener(view -> {
            ghostIcon.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            ButterflyPointDialog.showGhostPrivacyModeToggle(this);
            return true;
        });
        binding.sideActionPanel.addView(ghostIcon);
        binding.sideActionPanel.addView(createSmallIcon("ACTION_SEARCH", view -> showTacticalTargetSearchDialog()));
        
        updateExpandIconState(isMapExpanded);

        binding.sideActionPanel.setVisibility(View.VISIBLE);
        if (binding.sideActionScroll != null) binding.sideActionScroll.setVisibility(View.VISIBLE);
        binding.dashboardGridZone.setVisibility(isMapExpanded ? View.GONE : View.VISIBLE);
        if (isMapExpanded) {
            ViewGroup.LayoutParams initLp = binding.mapCardContainer.getLayoutParams();
            if (initLp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) initLp;
                lp.setMargins(0, 0, 0, 0);
                binding.mapCardContainer.setLayoutParams(lp);
            }
        }
        
        binding.expandIcon.setOnClickListener(v -> {
            isMapExpanded = !isMapExpanded;
            com.jdroidx.jfindme.utils.RegistryManager.setMapExpanded(this, isMapExpanded);
            androidx.transition.TransitionManager.beginDelayedTransition(binding.mainContent, new androidx.transition.AutoTransition().setDuration(350));
            updateExpandIconState(isMapExpanded);
            if (isMapExpanded) {
                binding.sideActionPanel.setVisibility(View.VISIBLE);
                if (binding.sideActionScroll != null) binding.sideActionScroll.setVisibility(View.VISIBLE);
                binding.dashboardGridZone.setVisibility(View.GONE);
                ViewGroup.LayoutParams rawLp = binding.mapCardContainer.getLayoutParams();
                if (rawLp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) rawLp;
                    lp.setMargins(0, 0, 0, 0);
                    binding.mapCardContainer.setLayoutParams(lp);
                }
            } else {
                binding.sideActionPanel.setVisibility(View.VISIBLE);
                if (binding.sideActionScroll != null) binding.sideActionScroll.setVisibility(View.VISIBLE);
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
            new int[]{ Color.argb(220, 8, 12, 20), Color.argb(245, 15, 23, 42) }
        );
        marqueeBg.setCornerRadius(scale(8));
        marqueeBg.setStroke(scale(1), Color.argb(180, Color.red(marqueeColor), Color.green(marqueeColor), Color.blue(marqueeColor)));
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
        // [Variables] | [Constants] | [Dependencies] | [Integration Points]
        // Variables: restoredCamLat, restoredCamLng, restoredCamZoom, restoredCamTilt, restoredCamBearing, selectedUserId, activeLocateTargetUid, activeLocateTargetName, initialTargetTs
        // Constants: saved_cam_lat, saved_cam_lng, saved_cam_zoom, saved_cam_tilt, saved_cam_bearing, saved_selected_user_id, saved_active_locate_uid, saved_active_locate_name, saved_active_locate_initial_ts
        // Dependencies: android.os.Bundle, SystemLogger
        // Integration Points: BaseActivityJava.onCreate lifecycle contract, MapViewModel, SupportMapFragment
        SystemLogger.i(TAG, "L7 Lifecycle: Map Dashboard Re-Synchronizing after Rotation.");
        
        if (savedState != null) {
            restoredCamLat = savedState.getDouble("saved_cam_lat", 0.0);
            restoredCamLng = savedState.getDouble("saved_cam_lng", 0.0);
            restoredCamZoom = savedState.getFloat("saved_cam_zoom", 0f);
            restoredCamTilt = savedState.getFloat("saved_cam_tilt", 0f);
            restoredCamBearing = savedState.getFloat("saved_cam_bearing", 0f);
            String selId = savedState.getString("saved_selected_user_id", null);
            if (selId != null) selectedUserId = selId;

            String restoredLocateUid = savedState.getString("saved_active_locate_uid", null);
            if (restoredLocateUid != null) {
                activeLocateTargetUid = restoredLocateUid;
                activeLocateTargetName = savedState.getString("saved_active_locate_name", null);
                initialTargetTs = savedState.getLong("saved_active_locate_initial_ts", 0L);
                if (tvAddressMarquee != null && activeLocateTargetName != null) {
                    tvAddressMarquee.setText("🛰️ [LOCATE: PING SENT] Awaiting remote GNSS handshake from " + activeLocateTargetName + "... ");
                    tvAddressMarquee.setSelected(true);
                }
            }
        }

        // 1. Re-Target and Re-Sync Map Fragment only if uninitialized
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(com.jdroidx.jfindme.R.id.map_container);
        if (mapFragment != null && googleMap == null) {
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

    // [Variables] label, r1, r2 | [Constants] None | [Dependencies] UiArchitect, Intent | [Integration Points] dashboardGridZone, NetworkGatewayActivity, GeofenceActivity
    private void setupGridMatrix() {
        TextView label = new TextView(this);
        label.setText("⚡ MISSION CONTROL");
        label.setTextColor(Color.parseColor("#94A3B8"));
        label.setTextSize(9.5f);
        label.setTypeface(null, Typeface.BOLD);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            label.setLetterSpacing(0.04f);
        }
        label.setPadding(scale(6), scale(4), 0, scale(2));
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
            else if (current.equals("FRIENDS")) next = "ALL";
            else next = "FAMILY";
            
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

        // [Variables]: launchPanel | [Constants]: LAUNCH_PANEL, RADAR, BUTTERFLY_POINT, BUTTERFLY | [Dependencies]: Handler, Looper, ButterflyPointDialog | [Integration Points]: Tactical Proximity Radar & Butterfly Hub Deep-Link Handshake
        if (getIntent() != null && "RADAR".equals(getIntent().getStringExtra("LAUNCH_PANEL"))) {
            getIntent().removeExtra("LAUNCH_PANEL");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::openLocateDrawer, 600);
        } else if (getIntent() != null && ("BUTTERFLY_POINT".equals(getIntent().getStringExtra("LAUNCH_PANEL")) || "BUTTERFLY".equals(getIntent().getStringExtra("LAUNCH_PANEL")))) {
            getIntent().removeExtra("LAUNCH_PANEL");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> ButterflyPointDialog.show(this), 600);
        }
    }











    // [Variables] pairedIds, selfDevId, options, i | [Constants] None | [Dependencies] DeviceRepository, UiArchitect, FamilyFriendsActivity | [Integration Points] ACTION_DEVICE_SHARING click, Navigation Drawer Device Sharing
    private void showDeviceSharingHubDialog() {
        Set<String> pairedIds = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this).getAllPairedDeviceIds();
        String selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);

        List<UiArchitect.OptionItem> options = new ArrayList<>();
        options.add(new UiArchitect.OptionItem("📲 Share Device with Peer", "Delegate device access to a connected family member or co-guardian", android.R.drawable.ic_menu_share));
        options.add(new UiArchitect.OptionItem("🔗 Connect B to C (Triadic Sharing)", "Open Fleet Roster to link two peer devices with custom roles", com.jdroidx.jfindme.R.drawable.ic_family_friends));
        options.add(new UiArchitect.OptionItem("🗺️ Fit All Devices in Viewport", "Frame all network devices within map view (Frame All)", com.jdroidx.jfindme.R.drawable.ic_nav_map));

        UiArchitect.showRichOptionsDialog(this, "Device Sharing & Mesh Hub", options.toArray(new UiArchitect.OptionItem[0]), (index, label) -> {
            if (index == 0) {
                showShareDevicePicker(pairedIds, selfDevId);
            } else if (index == 1) {
                Intent i = new Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class);
                startActivity(i);
            } else {
                frameAllDevices();
            }
        });
    }

    private void showShareDevicePicker(Set<String> pairedIds, String selfDevId) {
        if (pairedIds.isEmpty()) {
            Toast.makeText(this, "No connected peers found. Pair a device via Network Gateway first.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> peerList = new ArrayList<>();
        for (String p : pairedIds) {
            if (!p.equalsIgnoreCase(selfDevId)) peerList.add(p);
        }

        if (peerList.isEmpty()) {
            Toast.makeText(this, "No other connected devices to share with.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] peerDisplay = new String[peerList.size()];
        for (int i = 0; i < peerList.size(); i++) {
            peerDisplay[i] = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveDisplayName(this, peerList.get(i), null);
        }

        com.jdroidx.jfindme.ui.base.UiArchitect.showOptionsDialog(this, "Select Recipient Co-Guardian", peerDisplay, -1, (which, label) -> {
            if (which < 0 || which >= peerList.size()) return;
            String recipientId = peerList.get(which);
            String recipientName = peerDisplay[which];

                UiArchitect.showConfirmationDialog(
                    this,
                    "Confirm Device Sharing",
                    "Share live telemetry and tracking of your device with " + recipientName + " as Co-Guardian?",
                    "SHARE DEVICE",
                    "CANCEL",
                    () -> {
                        String shareId = "SH_" + System.currentTimeMillis();
                        String sharerName = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this);
                        com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this)
                            .upsertDeviceSharing(shareId, selfDevId, recipientId, selfDevId, "CO_GUARDIAN", 0xFF);

                        // 🛰️ Cross-Device Cloud Dispatch: Push sharing invitation to recipient's inbox
                        java.util.Map<String, Object> sharePayload = new java.util.HashMap<>();
                        sharePayload.put("share_id", shareId);
                        sharePayload.put("sharer_device_id", selfDevId);
                        sharePayload.put("sharer_name", sharerName);
                        sharePayload.put("recipient_device_id", recipientId);
                        sharePayload.put("role", "CO_GUARDIAN");
                        sharePayload.put("status", "PENDING");
                        sharePayload.put("timestamp", System.currentTimeMillis());

                        String canonicalRecipient = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(recipientId);
                        com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(canonicalRecipient)
                            .child("device_sharing_invitations").child(shareId).setValue(sharePayload);

                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                            this, "ROSTER", com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPSERT, shareId, java.util.Collections.emptyMap()
                        );
                        UiArchitect.showSlideNotificationBanner(this, "DEVICE SHARING DISPATCHED", "Invitation sent to " + recipientName, false, 3500, null, null);
                    },
                    null
                );
            });
    }

    // [Variables] locs, builder, validCount, myLoc, bounds, padding | [Constants] scale(70) | [Dependencies] LatLngBounds, CameraUpdateFactory, GoogleMap | [Integration Points] sideActionPanel, dashboardDeviceHubContainer
    public void frameAllDevices() {
        if (googleMap == null) {
            Toast.makeText(this, "🗺️ Map engine initializing...", Toast.LENGTH_SHORT).show();
            return;
        }
        List<UserLocation> locs = (viewModel != null && viewModel.getUserLocations() != null) 
            ? viewModel.getUserLocations().getValue() : null;
        Location myLoc = lastKnownLocation != null ? lastKnownLocation : 
            (viewModel != null && viewModel.getMyLocation() != null ? viewModel.getMyLocation().getValue() : null);

        if (cameraController != null) {
            cameraController.frameAllLocations(locs, myLoc, scale(70));
            Toast.makeText(this, "🗺️ Framing device nodes in viewport", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        int validCount = 0;
        if (locs != null) {
            for (UserLocation ul : locs) {
                if (ul != null && (ul.getLatitude() != 0.0 || ul.getLongitude() != 0.0)) {
                    builder.include(new LatLng(ul.getLatitude(), ul.getLongitude()));
                    validCount++;
                }
            }
        }
        if (myLoc != null && (myLoc.getLatitude() != 0.0 || myLoc.getLongitude() != 0.0)) {
            builder.include(new LatLng(myLoc.getLatitude(), myLoc.getLongitude()));
            validCount++;
        }
        if (validCount == 0) {
            Toast.makeText(this, "No active device coordinates available to frame", Toast.LENGTH_SHORT).show();
            return;
        }
        if (validCount == 1 && myLoc != null) {
            recenterCamera();
            return;
        }
        try {
            LatLngBounds bounds = builder.build();
            int padding = scale(70);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            Toast.makeText(this, "🗺️ Framed " + validCount + " device nodes in viewport", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            recenterCamera();
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

    // [Variables] None | [Constants] None | [Dependencies] openLocateDrawer | [Integration Points] Tactical Proximity Radar Search
    private void showTacticalTargetSearchDialog() {
        if (viewModel == null) return;
        openLocateDrawer();
    }

    private void openLocateDrawer() {
        com.jdroidx.jfindme.data.repository.DeviceRepository db = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this);
        Map<String, UserLocation> uniqueNodes = new java.util.LinkedHashMap<>();
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String selfCanonical = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(selfAgentId);

        // 1. 🛡️ Ingest ViewModel live locations with strict Canonical Agent ID deduplication
        if (viewModel != null && viewModel.getUserLocations().getValue() != null) {
            for (UserLocation ul : viewModel.getUserLocations().getValue()) {
                if (ul == null) continue;
                String rawId = ul.getResolvedUserId();
                if (rawId == null || rawId.trim().isEmpty()) continue;
                String canonId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(rawId);
                if (canonId.isEmpty() || canonId.startsWith("UNKNOWN")) continue;
                if (com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(this, canonId)) continue;

                UserLocation existing = uniqueNodes.get(canonId);
                if (existing == null) {
                    uniqueNodes.put(canonId, ul);
                } else {
                    boolean existingHasCoords = Math.abs(existing.getLatitude()) > 0.001 || Math.abs(existing.getLongitude()) > 0.001;
                    boolean newHasCoords = Math.abs(ul.getLatitude()) > 0.001 || Math.abs(ul.getLongitude()) > 0.001;
                    if (!existingHasCoords && newHasCoords) {
                        uniqueNodes.put(canonId, ul);
                    } else if (existingHasCoords == newHasCoords && ul.getLongTimestamp() > existing.getLongTimestamp()) {
                        uniqueNodes.put(canonId, ul);
                    }
                }
            }
        }

        // 2. 🛰️ Cross-Layer Mesh Reconciliation: Pull all paired & offline roster devices without duplicate injection
        try {
            List<java.util.Map<String, Object>> roster = db.getOfflineRoster();
            for (java.util.Map<String, Object> node : roster) {
                String pId = (String) (node.get("uid_1") != null ? node.get("uid_1") : node.get("connected_id"));
                if (pId == null || pId.trim().isEmpty()) continue;
                String canonId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(pId);
                if (canonId.isEmpty() || canonId.startsWith("UNKNOWN") || uniqueNodes.containsKey(canonId)) continue;
                if (com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(this, canonId)) continue;

                java.util.Map<String, Object> hist = db.getLatestHistoricalLocation(canonId);
                if (hist == null) hist = db.getLatestHistoricalLocation(pId);
                double hLat = hist != null && hist.get("lat") != null ? ((Number) hist.get("lat")).doubleValue() : 0.0;
                double hLng = hist != null && hist.get("lng") != null ? ((Number) hist.get("lng")).doubleValue() : 0.0;
                long hTs = hist != null && hist.get("timestamp") != null ? ((Number) hist.get("timestamp")).longValue() : (System.currentTimeMillis() - 60000);
                String pName = (String) (node.get("fullName") != null ? node.get("fullName") : node.get("name"));
                String pAvatar = (String) (node.get("avatar_id") != null ? node.get("avatar_id") : "1");

                UserLocation peerLoc = new UserLocation(canonId, hLat, hLng);
                peerLoc.setUserName(pName != null ? pName : "Peer " + canonId);
                peerLoc.setAvatar_id(pAvatar != null ? pAvatar : "1");
                peerLoc.setTimestamp(hTs);
                uniqueNodes.put(canonId, peerLoc);
            }

            Set<String> pairedIds = db.getAllPairedDeviceIds();
            for (String pId : pairedIds) {
                if (pId == null || pId.trim().isEmpty()) continue;
                String canonId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(pId);
                if (canonId.isEmpty() || canonId.startsWith("UNKNOWN") || uniqueNodes.containsKey(canonId)) continue;
                if (com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(this, canonId)) continue;

                java.util.Map<String, Object> hist = db.getLatestHistoricalLocation(canonId);
                if (hist == null) hist = db.getLatestHistoricalLocation(pId);
                double hLat = hist != null && hist.get("lat") != null ? ((Number) hist.get("lat")).doubleValue() : 0.0;
                double hLng = hist != null && hist.get("lng") != null ? ((Number) hist.get("lng")).doubleValue() : 0.0;
                long hTs = hist != null && hist.get("timestamp") != null ? ((Number) hist.get("timestamp")).longValue() : (System.currentTimeMillis() - 60000);
                String pName = db.getDeviceCentricName(canonId);
                String pAvatar = db.getDeviceCentricAvatar(canonId);

                UserLocation peerLoc = new UserLocation(canonId, hLat, hLng);
                peerLoc.setUserName(pName != null && !pName.isEmpty() ? pName : "Peer " + canonId);
                peerLoc.setAvatar_id(pAvatar != null && !pAvatar.isEmpty() ? pAvatar : "1");
                peerLoc.setTimestamp(hTs);
                uniqueNodes.put(canonId, peerLoc);
            }
        } catch (Exception ignored) {}

        List<UserLocation> rawLocs = new java.util.ArrayList<>(uniqueNodes.values());

        if (rawLocs.isEmpty()) {
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

        Location refLoc = null;
        if (viewModel != null && viewModel.getMyLocation().getValue() != null) {
            refLoc = viewModel.getMyLocation().getValue();
        }

        final Location myRef = refLoc;
        String myUid = FirebaseAuth.getInstance().getUid();

        // Sort peers by Prime Objective: SOS -> Child/Ward Target (Riddhi) -> Proximity
        List<UserLocation> sortedLocs = new java.util.ArrayList<>(rawLocs);
        java.util.Collections.sort(sortedLocs, (a, b) -> {
            if (a == null || b == null) return 0;
            if (a.isSosActive() != b.isSosActive()) {
                return a.isSosActive() ? -1 : 1;
            }
            boolean aChild = isChildOrPrimeTarget(a);
            boolean bChild = isChildOrPrimeTarget(b);
            if (aChild != bChild) {
                return aChild ? -1 : 1;
            }
            if (myRef != null) {
                float distA = com.jdroidx.jfindme.utils.SpatialMathUtil.computeDistanceBetweenFloat(
                    myRef.getLatitude(), myRef.getLongitude(), a.getLatitude(), a.getLongitude());
                float distB = com.jdroidx.jfindme.utils.SpatialMathUtil.computeDistanceBetweenFloat(
                    myRef.getLatitude(), myRef.getLongitude(), b.getLatitude(), b.getLongitude());
                return Float.compare(distA, distB);
            }
            return Long.compare(b.getLongTimestamp(), a.getLongTimestamp());
        });

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
        String myCanonicalId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(myAgentId);
        String myHwUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(this);
        String myRawAndroidId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getRawAndroidId(this);
        Set<String> renderedCanonicalIds = new java.util.HashSet<>();

        for (final UserLocation u : sortedLocs) {
            if (u == null) continue;
            String uid = u.getResolvedUserId();
            if (uid == null) uid = "";
            String cleanUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(uid);
            if (cleanUid.isEmpty() || cleanUid.startsWith("UNKNOWN")) continue;
            if (com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(this, cleanUid)) continue;

            boolean isSelf = (uid.equals(myUid) || 
                              (myAgentId != null && uid.equals(myAgentId)) || 
                              cleanUid.equalsIgnoreCase(myCanonicalId) ||
                              (myHwUid != null && (uid.equalsIgnoreCase(myHwUid) || cleanUid.equalsIgnoreCase(myHwUid))) || 
                              (myRawAndroidId != null && uid.equalsIgnoreCase(myRawAndroidId)) || 
                              uid.equalsIgnoreCase("You") || 
                              uid.equals("SELF_USER"));
            if (isSelf) continue; // 🚀 Deduplication: Skip self since mySelfCard is already placed at the top!
            if (renderedCanonicalIds.contains(cleanUid)) continue; // 🛡️ Strict Deduplication: No duplicate cards!
            renderedCanonicalIds.add(cleanUid);

            String ssotAvatar = (u.getAvatar_id() != null && !u.getAvatar_id().isEmpty()) ? u.getAvatar_id() : db.getDeviceCentricAvatar(cleanUid);
            String ssotName = db.getDeviceCentricName(cleanUid);
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

            // [Variables] uid, ssotAvatar, avatarImg | [Constants] None | [Dependencies] AvatarLoader | [Integration Points] SSOT Bottom Sheet Roster Avatar
            com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, avatarImg, uid, ssotAvatar);
            card.addView(avatarImg);

            // Info Column
            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            String rf = u.getRole_family();
            String rfr = u.getRole_friend();
            String role = (rf != null && !rf.equals("None") && !rf.isEmpty()) ? rf : 
                          (rfr != null && !rfr.equals("None") && !rfr.isEmpty()) ? rfr : "";
            if (!role.isEmpty()) {
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
            if (u.getLatitude() == 0.0 && u.getLongitude() == 0.0) {
                distStr = "Connected • Awaiting GPS Signal";
            } else if (myRef != null) {
                float dist = com.jdroidx.jfindme.utils.SpatialMathUtil.computeDistanceBetweenFloat(
                    myRef.getLatitude(), myRef.getLongitude(), u.getLatitude(), u.getLongitude());
                distStr = dist >= 1000 ? String.format(java.util.Locale.US, "%.1f km away", dist / 1000f) : String.format(java.util.Locale.US, "%.0f m away", dist);
            }

            float spd = u.getSpeed();
            String motionBadge = com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.getMotionBadge(spd);

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

            final String targetNameStr = displayName;
            card.setOnClickListener(v -> {
                radarDialog.dismiss();
                if (isSelf) {
                    recenterCamera();
                } else if (u.getLatitude() != 0.0 || u.getLongitude() != 0.0) {
                    showTacticalTargetActionSheet(u);
                } else {
                    Toast.makeText(this, targetNameStr + " is connected in network. Awaiting GPS satellite lock.", Toast.LENGTH_LONG).show();
                }
            });

            listLayout.addView(card);
        }

        radarDialog.show();
        applyDialogTheme(radarDialog);
    }

    private void showTacticalTargetActionSheet(UserLocation target) {
        if (target == null) return;
        com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.TacticalTarget t = 
            com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.TacticalTarget.fromUserLocation(this, target);
        
        com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.showTacticalActionSheet(this, t, new com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.ActionCallback() {
            @Override
            public void onFindLocationRequested(com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.TacticalTarget targetItem) {
                initiateRemoteLocateStateMachine(target, targetItem.displayName);
            }

            @Override
            public void onDeviceSharingRequested(com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.TacticalTarget targetItem) {
                showDeviceSharingWizard(targetItem.uid, targetItem.displayName, target);
            }

            @Override
            public void onButterflyPointRequested(com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.TacticalTarget targetItem) {
                ButterflyPointDialog.showForTarget(MapActivity.this, targetItem.uid, targetItem.displayName, targetItem.latitude, targetItem.longitude);
            }

            @Override
            public void onUnlinkRequested(com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.TacticalTarget targetItem) {
                showUnlinkDeviceDialog(targetItem.uid, targetItem.displayName);
            }

            @Override
            public void onSelfActionRequested() {
                showSelfActionSheet();
            }

            @Override
            public void onProfileRefreshed(com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.TacticalTarget targetItem) {
                if (viewModel != null) {
                    viewModel.refreshAllData();
                }
            }
        });
    }

    // [Variables] targetUid: String, targetName: String, target: UserLocation, wizardOptions: UiArchitect.OptionItem[]
    // [Constants] Option Item labels
    // [Dependencies] UiArchitect, PairingRepository, AngelLoader
    // [Integration Points] MapActivity, PairingRepository, NetworkGatewayActivity
    /**
     * 📱 Unified Device Sharing Wizard:
     * Consolidates:
     * 1. Direct Peer Pairing (Connect Device C with mutual Family/Friends roles)
     * 2. Co-Parent / Secondary Guardian Linkage
     * 3. Multi-Device Fleet Connection (Batch Tickbox Selection)
     * 4. Session Pairing PIN Generator (Network Gateway Handshake)
     */
    private void showDeviceSharingWizard(String targetUid, String targetName, UserLocation target) {
        UiArchitect.OptionItem[] wizardOptions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("🔗 Connect with Device C (Family / Friends)", "Directly link " + targetName + " to a second fleet device with reciprocal roles & 24h background sync", com.jdroidx.jfindme.R.drawable.ic_family_friends),
            new UiArchitect.OptionItem("👨‍👩‍👧 Add Co-Parent / Guardian", "Designate a co-guardian or secondary parent to share mutual oversight of " + targetName, com.jdroidx.jfindme.R.drawable.ic_user_profile),
            new UiArchitect.OptionItem("📱 Multi-Device Fleet Link (Tickboxes)", "Pair " + targetName + " with multiple devices simultaneously using batch role selectors", com.jdroidx.jfindme.R.drawable.ic_settings_gears),
            new UiArchitect.OptionItem("🔑 Generate Pairing Code (Network Gateway)", "Issue a secure 6-digit PIN for target to join circle via Network Gateway", android.R.drawable.ic_menu_share)
        };

        UiArchitect.showRichOptionsDialog(this, "📱 DEVICE SHARING: " + targetName.toUpperCase(), wizardOptions, (idx, optLabel) -> {
            String sel = optLabel != null ? optLabel : "";
            if (sel.contains("Device C")) {
                showConnectBToCDialog(targetUid, targetName, target);
            } else if (sel.contains("Co-Parent")) {
                showAddAnotherParentDialog(targetUid, targetName, target);
            } else if (sel.contains("Multi-Device")) {
                showConnectAnotherDeviceDialog(targetUid, targetName, target);
            } else if (sel.contains("Pairing Code")) {
                generateMultiDevicePairingCode(targetUid, targetName);
            }
        });
    }

    /**
     * 🔄 Pulls latest profile & callsign data from Firebase Cloud SSOT into local SQLite and refreshes viewport
     */
    private void refreshTargetProfileData(String targetUid, String targetName) {
        com.jdroidx.jfindme.engine.action.TacticalActionDispatcher.refreshTargetProfile(this, targetUid, targetName, () -> {
            if (viewModel != null) {
                viewModel.refreshAllData();
            }
        });
    }

    // [Variables] devBUid, devBName, devB, candidates, seenIds, displayList, items, chosen | [Constants] None | [Dependencies] DatabaseHelper, UiArchitect, AlertDialog, UserLocation | [Integration Points] MapActivity, PairingRepository
    /**
     * 🔗 Connect Device B to Device C:
     * Step 1: Select Device C from other fleet devices.
     * Step 2: Choose Family vs Friends relationship.
     * Step 3: Auto-connect both devices bidirectionally across SQLite and Firebase RTDB.
     */
    private void showConnectBToCDialog(String devBUid, String devBName, UserLocation devB) {
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String selfUid = FirebaseAuth.getInstance().getUid();
        List<UserLocation> allLocations = (viewModel != null && viewModel.getUserLocations().getValue() != null) ? viewModel.getUserLocations().getValue() : new ArrayList<>();
        
        Set<String> excluded = new HashSet<>();
        excluded.add(devBUid);
        if (selfAgentId != null && !selfAgentId.isEmpty()) excluded.add(selfAgentId);
        if (selfUid != null && !selfUid.isEmpty()) excluded.add(selfUid);

        List<Pair<String, String>> candidates = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this)
            .resolveFleetCandidates(excluded, allLocations, false);

        if (candidates.isEmpty()) {
            UiArchitect.showInputPromptDialog(this, "Connect to Device C", "Enter Device C Agent ID to pair with " + devBName + ":", "", manualId -> {
                if (manualId != null && !manualId.trim().isEmpty()) {
                    String cleanCId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(manualId);
                    if (com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(cleanCId)) {
                        showRelationshipChoiceDialog(devBUid, devBName, cleanCId, "Device " + cleanCId);
                    } else {
                        UiArchitect.showValidationDialog(this, "Invalid ID", "Please enter a valid 8-character Agent ID (e.g. 4379A761).", null);
                    }
                }
            });
            return;
        }

        List<String> displayList = new ArrayList<>();
        for (Pair<String, String> c : candidates) {
            displayList.add("📱 " + c.second + " (" + c.first + ")");
        }
        displayList.add("⌨️ Enter Device C ID Manually...");

        String[] items = displayList.toArray(new String[0]);
        new android.app.AlertDialog.Builder(this)
            .setTitle("🔗 Select Device C to Connect with " + devBName)
            .setItems(items, (dialog, which) -> {
                dialog.dismiss();
                if (which == items.length - 1) {
                    UiArchitect.showInputPromptDialog(this, "Manual Device C Entry", "Enter Device C Agent ID:", "", manualId -> {
                        if (manualId != null && !manualId.trim().isEmpty()) {
                            String cleanCId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(manualId);
                            if (com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(cleanCId)) {
                                showRelationshipChoiceDialog(devBUid, devBName, cleanCId, "Device " + cleanCId);
                            } else {
                                UiArchitect.showValidationDialog(this, "Invalid ID", "Please enter a valid 8-character Agent ID (e.g. 4379A761).", null);
                            }
                        }
                    });
                } else {
                    Pair<String, String> chosen = candidates.get(which);
                    showRelationshipChoiceDialog(devBUid, devBName, chosen.first, chosen.second);
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    /**
     * [Variables] devBUid, devBName, devCUid, devCName, catOptions, familyRoles, friendRoles, roleB, roleC, confirmMsg | [Constants] None | [Dependencies] UiArchitect, AlertDialog | [Integration Points] MapActivity, PairingRepository
     * Step 2: Choose Family vs Friends relationship with role selection.
     */
    private void showRelationshipChoiceDialog(String devBUid, String devBName, String devCUid, String devCName) {
        String[] catOptions = new String[]{
            "👨‍👩‍👧 Family Relationship (Guardian, Child, Spouse, Sibling)",
            "🤝 Friends Relationship (Best Friend, Close Friend, Peer)"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("Relationship: " + devBName + " ↔ " + devCName)
            .setItems(catOptions, (dialog, which) -> {
                dialog.dismiss();
                if (which == 0) {
                    // Family Selection
                    String[] familyRoles = new String[]{
                        "🛡️ Parent / Guardian (" + devBName + " is Guardian ↔ " + devCName + " is Child)",
                        "👶 Child / Dependent (" + devBName + " is Child ↔ " + devCName + " is Guardian)",
                        "💍 Spouse / Partner (Mutual Spouses)",
                        "👨‍👩‍👧 Sibling / Family Member (Mutual Family)"
                    };
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("👨‍👩‍👧 Family Role: " + devBName)
                        .setItems(familyRoles, (famDialog, famWhich) -> {
                            famDialog.dismiss();
                            String roleB;
                            String roleC;
                            switch (famWhich) {
                                case 0: roleB = "Guardian"; roleC = "Child"; break;
                                case 1: roleB = "Child"; roleC = "Guardian"; break;
                                case 2: roleB = "Spouse"; roleC = "Spouse"; break;
                                default: roleB = "Family"; roleC = "Family"; break;
                            }
                            String confirmMsg = "Auto-connect " + devBName + " and " + devCName + " as Family (" + roleB + " ↔ " + roleC + ")?\n\nBoth devices will immediately see their mutual relationship across all screens.";
                            UiArchitect.showConfirmationDialog(this, "Confirm Connection", confirmMsg, "Connect Now", "Cancel", () -> {
                                executeDirectBToCConnection(devBUid, devBName, devCUid, devCName, roleB, roleC, true);
                            }, null);
                        })
                        .setNegativeButton("BACK", (d, w) -> showRelationshipChoiceDialog(devBUid, devBName, devCUid, devCName))
                        .show();
                } else {
                    // Friends Selection
                    String[] friendRoles = new String[]{
                        "🌟 Best Friends (Mutual)",
                        "🤝 Close Friends (Mutual)",
                        "👥 Friends / Peers (Mutual)",
                        "🏢 Colleagues / Work (Mutual)"
                    };
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("🤝 Friends Role: " + devBName)
                        .setItems(friendRoles, (frDialog, frWhich) -> {
                            frDialog.dismiss();
                            String roleB;
                            String roleC;
                            switch (frWhich) {
                                case 0: roleB = "Best Friend"; roleC = "Best Friend"; break;
                                case 1: roleB = "Close Friend"; roleC = "Close Friend"; break;
                                case 2: roleB = "Friend"; roleC = "Friend"; break;
                                default: roleB = "Colleague"; roleC = "Colleague"; break;
                            }
                            String confirmMsg = "Auto-connect " + devBName + " and " + devCName + " as Friends (" + roleB + " ↔ " + roleC + ")?\n\nBoth devices will immediately see their mutual relationship across all screens.";
                            UiArchitect.showConfirmationDialog(this, "Confirm Connection", confirmMsg, "Connect Now", "Cancel", () -> {
                                executeDirectBToCConnection(devBUid, devBName, devCUid, devCName, roleB, roleC, false);
                            }, null);
                        })
                        .setNegativeButton("BACK", (d, w) -> showRelationshipChoiceDialog(devBUid, devBName, devCUid, devCName))
                        .show();
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    /**
     * [Variables] devBUid, devBName, devCUid, devCName, roleB, roleC, isFamily, selfDevId, myGrp, famRoleB, frRoleB, famRoleC, frRoleC, profUpdateB, profUpdateC, msg | [Constants] None | [Dependencies] PairingRepository, DatabaseHelper, FirebaseComm, UiArchitect, AngelLoader | [Integration Points] MapActivity, FamilyFriendsActivity, PersonDetailsActivity
     * Step 3: Establish bidirectional connection in Firebase and SQLite with role metadata.
     */
    // [Variables] devBUid, devBName, devCUid, devCName, roleB, roleC, isFamily, selfDevId, myGrp, msg, cm, netInfo, isOnline | [Constants] None | [Dependencies] PairingRepository, DatabaseHelper, FirebaseComm, UiArchitect, AngelLoader | [Integration Points] MapActivity, FamilyFriendsActivity, TrackingService
    private void executeDirectBToCConnection(String devBUid, String devBName, String devCUid, String devCName, String roleB, String roleC, boolean isFamily) {
        // 📡 Zero-Connectivity Auto-Failover Sentinel
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo netInfo = (cm != null) ? cm.getActiveNetworkInfo() : null;
        boolean isOnline = (netInfo != null && netInfo.isConnected());
        if (!isOnline) {
            com.jdroidx.jfindme.utils.AngelLoader.hide(this);
            UiArchitect.showConfirmationDialog(
                this,
                "📡 No Internet Connection Detected",
                "Cloud Device Sharing requires an active internet connection.\n\nWould you like to switch to Offline P2P Gateway (Wi-Fi Direct / Local PIN) to connect " + devBName + " directly without internet?",
                "Open P2P Gateway",
                "Cancel",
                () -> {
                    Intent p2pIntent = new Intent(this, com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.class);
                    p2pIntent.putExtra("SELECT_TAB", com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.TAB_NEARBY_P2P);
                    startActivity(p2pIntent);
                },
                null
            );
            return;
        }

        String selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);
        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Auto-connecting " + devBName + " ↔ " + devCName + "...");

        com.jdroidx.jfindme.data.repository.PairingRepository.executeClosedLoopDeviceSharing(
            this,
            myGrp,
            devBUid,
            devCUid,
            roleB,
            roleC,
            new com.jdroidx.jfindme.data.repository.PairingRepository.DeviceSharingSyncCallback() {
                @Override
                public void onSyncDispatched(String syncSessionId) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.setStatus(MapActivity.this, "Handshake dispatched (24h sync armed)... Awaiting device ACK...");
                    });
                }

                @Override
                public void onSyncComplete(String devAId, String devBId, long latencyMs) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(MapActivity.this);
                        if (viewModel != null) {
                            viewModel.refreshAllData();
                        }
                        String msg = devBName + " and " + devCName + " are now paired & synchronized (" + latencyMs + "ms). Both devices confirmed.";
                        UiArchitect.showSlideNotificationBanner(MapActivity.this, "DEVICE SHARING COMPLETE", msg, false, 4500, null, null);
                        Toast.makeText(MapActivity.this, "✅ " + msg, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onSyncTimeout(String diagnosticReason) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(MapActivity.this);
                        if (viewModel != null) {
                            viewModel.refreshAllData();
                        }
                        String msg = "Command delivered to Cloud SSOT. Remote peer is offline; 24-hour background sync armed.\n\n" + diagnosticReason;
                        UiArchitect.showSlideNotificationBanner(MapActivity.this, "QUEUED FOR 24H BACKGROUND SYNC", msg, false, 5500, null, null);
                        Toast.makeText(MapActivity.this, "⏳ Device offline: 24h background sync armed", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onSyncFailed(String errorMessage) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(MapActivity.this);
                        UiArchitect.showValidationDialog(MapActivity.this, "Device Sharing Failed", errorMessage, null);
                    });
                }
            }
        );
    }

    /**
     * [Variables] selfAgentId, allLocations, localDb, candidates, seenIds, roles, roleJoined, confirmMsg
     * [Constants] None
     * [Dependencies] UiArchitect, DatabaseHelper, PairingRepository, AngelLoader, UserLocation
     * [Integration Points] MapActivity, PairingRepository, DatabaseHelper, DevicePairingTable
     * 🔗 Displays Compound Device & Role Connector Modal Dialog:
     * - Header: Selected Device 1
     * - To Connect Device List (Tickboxes)
     * - Role List (Tickboxes)
     * On Confirm: Establishes bidirectional connections in SQLite & Firebase SSOT.
     */
    // [Variables] selfAgentId, allLocations, localDb, candidates, seenIds, roles, roleJoined, confirmMsg, pairedIds
    // [Constants] None
    // [Dependencies] UiArchitect, DatabaseHelper, PairingRepository, AngelLoader, UserLocation
    // [Integration Points] MapActivity, PairingRepository, DatabaseHelper, DevicePairingTable
    /**
     * 🔗 Displays Compound Device & Role Connector Modal Dialog:
     * - Header: Selected Device 1
     * - To Connect Device List (Tickboxes)
     * - Role List (Tickboxes)
     * On Confirm: Establishes bidirectional connections in SQLite & Firebase SSOT.
     */
    private void showConnectAnotherDeviceDialog(String targetUid, String targetName, UserLocation target) {
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        List<UserLocation> allLocations = (viewModel != null && viewModel.getUserLocations().getValue() != null) ? viewModel.getUserLocations().getValue() : new ArrayList<>();
        Set<String> excluded = new HashSet<>();
        excluded.add(targetUid);
        if (selfAgentId != null && !selfAgentId.isEmpty()) excluded.add(selfAgentId);

        List<Pair<String, String>> candidates = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this)
            .resolveFleetCandidates(excluded, allLocations, false);

        String[] roles = new String[]{
            "🛡️ Parent / Primary Guardian",
            "👨‍👩‍👧 Co-Parent / Secondary Guardian",
            "👶 Child / Dependent Ward",
            "💍 Spouse / Partner",
            "🤝 Friend / Peer Member",
            "🚨 Emergency Contact"
        };

        if (candidates.isEmpty()) {
            UiArchitect.showInputPromptDialog(this, "Connect Another Device", "Enter Agent ID / Hardware Token of device to connect with " + targetName + ":", "", manualId -> {
                if (manualId != null && !manualId.trim().isEmpty()) {
                    String cleanId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(manualId);
                    List<String> manualList = new ArrayList<>();
                    manualList.add(cleanId);
                    List<String> defRoleList = new ArrayList<>();
                    defRoleList.add("Member");
                    executeBatchDeviceConnection(targetUid, targetName, manualList, defRoleList);
                } else {
                    UiArchitect.showValidationDialog(this, "Invalid ID", "Target Agent ID cannot be empty.", null);
                }
            });
            return;
        }

        UiArchitect.showCompoundDeviceRoleDialog(this, targetUid, targetName, candidates, roles, (selectedDeviceIds, selectedRoles) -> {
            String roleJoined = android.text.TextUtils.join(", ", selectedRoles);
            String confirmMsg = "Connect " + targetName + " (" + targetUid + ") to " + selectedDeviceIds.size() + " device(s) with role(s):\n" + roleJoined + "?\n\nThis will link them mutually in your active mesh.";
            
            UiArchitect.showConfirmationDialog(this, "Confirm Connection", confirmMsg, "Connect Now", "Cancel", () -> {
                executeBatchDeviceConnection(targetUid, targetName, selectedDeviceIds, selectedRoles);
            }, null);
        });
    }

    // [Variables] sourceUid: String, sourceName: String, targetIds: List<String>, roles: List<String>, selfDevId: String, myGrp: String, primaryRole: String, localDb: DatabaseHelper, cm: ConnectivityManager, netInfo: NetworkInfo, isOnline: boolean | [Constants] None | [Dependencies] PairingRepository, AngelLoader, UiArchitect | [Integration Points] MapActivity, PairingRepository
    private void executeBatchDeviceConnection(String sourceUid, String sourceName, List<String> targetIds, List<String> roles) {
        // 📡 Zero-Connectivity Auto-Failover Sentinel
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo netInfo = (cm != null) ? cm.getActiveNetworkInfo() : null;
        boolean isOnline = (netInfo != null && netInfo.isConnected());
        if (!isOnline) {
            com.jdroidx.jfindme.utils.AngelLoader.hide(this);
            UiArchitect.showConfirmationDialog(
                this,
                "📡 No Internet Connection Detected",
                "Batch Device Sharing requires an active internet connection.\n\nWould you like to switch to Offline P2P Gateway (Wi-Fi Direct / Local PIN) to connect fleet devices directly without internet?",
                "Open P2P Gateway",
                "Cancel",
                () -> {
                    Intent p2pIntent = new Intent(this, com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.class);
                    p2pIntent.putExtra("SELECT_TAB", com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.TAB_NEARBY_P2P);
                    startActivity(p2pIntent);
                },
                null
            );
            return;
        }

        String selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);
        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Connecting " + targetIds.size() + " devices with 24h sync...");

        com.jdroidx.jfindme.data.repository.PairingRepository.executeClosedLoopBatchDeviceSharing(
            this,
            myGrp,
            sourceUid,
            targetIds,
            roles,
            new com.jdroidx.jfindme.data.repository.PairingRepository.BatchDeviceSharingSyncCallback() {
                @Override
                public void onBatchDispatched(String syncSessionId, int totalDevices) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.setStatus(MapActivity.this, "Dispatched 24h sync to " + totalDevices + " devices... Awaiting ACKs...");
                    });
                }

                @Override
                public void onBatchProgress(int syncedCount, int totalDevices) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.setStatus(MapActivity.this, "Synchronized " + syncedCount + " of " + totalDevices + " devices...");
                    });
                }

                @Override
                public void onBatchComplete(int syncedCount, int totalDevices, long latencyMs) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(MapActivity.this);
                        if (viewModel != null) viewModel.refreshAllData();
                        String msg = "Successfully synchronized all " + syncedCount + " devices (" + latencyMs + "ms). All peers confirmed.";
                        UiArchitect.showSlideNotificationBanner(MapActivity.this, "BATCH SHARING COMPLETE", msg, false, 4500, null, null);
                        Toast.makeText(MapActivity.this, "✅ " + msg, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onBatchTimeout(int syncedCount, int totalDevices, String diagnosticReason) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(MapActivity.this);
                        if (viewModel != null) viewModel.refreshAllData();
                        String msg = "Command delivered to Cloud SSOT. " + syncedCount + " of " + totalDevices + " devices confirmed. Offline devices queued for 24h background sync.";
                        UiArchitect.showSlideNotificationBanner(MapActivity.this, "BATCH SYNC QUEUED (24H)", msg, false, 5500, null, null);
                        Toast.makeText(MapActivity.this, "⏳ " + syncedCount + "/" + totalDevices + " confirmed; offline devices queued for 24h sync", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onBatchFailed(String errorMessage) {
                    runOnUiThread(() -> {
                        com.jdroidx.jfindme.utils.AngelLoader.hide(MapActivity.this);
                        UiArchitect.showValidationDialog(MapActivity.this, "Batch Sync Failed", errorMessage, null);
                    });
                }
            }
        );
    }

    // [Variables] selfAgentId, myGrp, code, msg | [Constants] None | [Dependencies] PairingRepository, AngelLoader, UiArchitect | [Integration Points] MapActivity, NetworkGatewayActivity
    private void generateMultiDevicePairingCode(String targetUid, String targetName) {
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);
        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Generating Multi-Device Pairing Code...");

        com.jdroidx.jfindme.data.repository.PairingRepository.startMultiDevicePairingSession(
            selfAgentId,
            targetUid,
            myGrp,
            "MEMBER",
            5,
            code -> {
                runOnUiThread(() -> {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                    if (code == null || code.trim().isEmpty()) {
                        UiArchitect.showValidationDialog(this, "Generation Failed", "Unable to generate pairing code. Please check network connection.", null);
                        return;
                    }
                    String msg = "6-Digit Pairing Code: " + code + "\n\nAll 3/4/5 devices can enter this exact same code simultaneously in Network Gateway to attach closely to " + targetName + " in circle [" + myGrp + "].\n\nCode valid for 30 minutes.";
                    UiArchitect.showConfirmationDialog(this, "Multi-Device Pairing Code", msg, "Copy Code", "Done", () -> {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("Pairing Code", code));
                        Toast.makeText(this, "📋 Code Copied: " + code, Toast.LENGTH_SHORT).show();
                    }, null);
                });
            }
        );
    }

    /**
     * [Variables] selfAgentId, allLocations, localDb, coParentCandidates, seenIds, offlineNodes, optionsList, options
     * [Constants] None
     * [Dependencies] DatabaseHelper, UiArchitect, PairingRepository
     * [Integration Points] MapActivity, FamilyFriendsActivity
     * 👨‍👩‍👧 Variable Right Selections for linking a Co-Parent or Secondary Guardian to target
     */
    private void showAddAnotherParentDialog(String targetUid, String targetName, UserLocation target) {
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        List<UserLocation> allLocations = (viewModel != null && viewModel.getUserLocations().getValue() != null) ? viewModel.getUserLocations().getValue() : new ArrayList<>();
        Set<String> excluded = new HashSet<>();
        if (selfAgentId != null && !selfAgentId.isEmpty()) excluded.add(selfAgentId);
        excluded.add(targetUid);

        List<Pair<String, String>> coParentCandidates = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this)
            .resolveFleetCandidates(excluded, allLocations, true);

        List<UiArchitect.OptionItem> optionsList = new ArrayList<>();
        for (Pair<String, String> cand : coParentCandidates) {
            optionsList.add(new UiArchitect.OptionItem(
                "👨‍👩‍👧 Link " + cand.second,
                "Assign " + cand.second + " as Co-Parent/Guardian for " + targetName,
                com.jdroidx.jfindme.R.drawable.ic_user_profile
            ));
        }
        optionsList.add(new UiArchitect.OptionItem(
            "🔑 Generate Co-Guardian Circle Invite Code",
            "Create shareable 6-character code (SMS / WhatsApp) for the second parent",
            com.jdroidx.jfindme.R.drawable.ic_family_friends
        ));
        optionsList.add(new UiArchitect.OptionItem(
            "⌨️ Enter Co-Parent Agent ID Manually",
            "Manually link an existing 8-character hardware/agent ID as co-parent",
            com.jdroidx.jfindme.R.drawable.ic_settings_gears
        ));

        UiArchitect.OptionItem[] options = optionsList.toArray(new UiArchitect.OptionItem[0]);

        UiArchitect.showRichOptionsDialog(this, "Co-Guardian for " + targetName, options, (which, label) -> {
            if (which < coParentCandidates.size()) {
                Pair<String, String> chosen = coParentCandidates.get(which);
                executeCoParentLinkFromMap(chosen.first, chosen.second, targetUid, targetName);
            } else if (label != null && label.contains("Invite Code")) {
                generateAndShowCoParentInviteDialog(targetName);
            } else {
                UiArchitect.showInputPromptDialog(this,
                    "Link Co-Parent",
                    "Enter the 8-character Agent ID / Hardware Token of the other parent / co-guardian:",
                    "",
                    inputToken -> {
                        if (inputToken != null && !inputToken.trim().isEmpty()) {
                            String cleanToken = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(inputToken);
                            executeCoParentLinkFromMap(cleanToken, "Co-Parent (" + cleanToken + ")", targetUid, targetName);
                        } else {
                            UiArchitect.showValidationDialog(this, "Invalid ID", "Co-Parent Agent ID cannot be empty.", null);
                        }
                    });
            }
        });
    }

    private void executeCoParentLinkFromMap(String coParentId, String coParentName, String targetUid, String targetName) {
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);
        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Linking Co-Parent to " + targetName + "...");

        com.jdroidx.jfindme.data.repository.PairingRepository.linkCoParentToChild(
            myGrp,
            coParentId,
            targetUid,
            coParentName,
            targetName,
            (success, errorMsg) -> {
                runOnUiThread(() -> {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                    if (Boolean.TRUE.equals(success)) {
                        com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this)
                            .logCoGuardianPairing(coParentId, targetUid, selfAgentId);
                        UiArchitect.showSlideNotificationBanner(this, "CO-GUARDIAN LINKED",
                            coParentName + " is now linked as co-guardian for " + targetName + "!",
                            false, 4000, null, null);
                        Toast.makeText(this, "✅ Co-Guardian relationship established for " + targetName + "!", Toast.LENGTH_LONG).show();
                        if (viewModel != null) viewModel.refreshAllData();
                    } else {
                        UiArchitect.showValidationDialog(this, "Link Failed",
                            "Unable to link co-parent: " + (errorMsg != null ? errorMsg : "Unknown error"), null);
                    }
                });
            }
        );
    }

    private void generateAndShowCoParentInviteDialog(String targetName) {
        String hostAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String hostGroup = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);

        Toast.makeText(this, "Generating Co-Guardian Invite Code...", Toast.LENGTH_SHORT).show();
        com.jdroidx.jfindme.data.repository.PairingRepository.createCircleInviteCode(hostAgentId, hostGroup, "GUARDIAN", code -> {
            runOnUiThread(() -> {
                if (code == null || code.trim().isEmpty()) {
                    Toast.makeText(this, "Failed to generate invite code. Please retry.", Toast.LENGTH_SHORT).show();
                    return;
                }
                android.app.AlertDialog codeDialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("👨‍👩‍👧 Co-Guardian Invite Code")
                    .setMessage("Share this 6-character code with the other parent/guardian to co-manage " + targetName + " (Valid for 48 Hours):\n\n" +
                                "🔑 CODE: " + code + "\n\n" +
                                "They can enter this code in Network Gateway to join circle and share oversight.")
                    .setPositiveButton("COPY CODE", (d, w) -> {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("jFindMe Co-Guardian Code", code);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Code " + code + " copied to clipboard!", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("SHARE", (d, w) -> {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join as Co-Guardian on jFindMe");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "Join as Co-Guardian to co-manage " + targetName + " on jFindMe! Use this invite code: " + code);
                        startActivity(Intent.createChooser(shareIntent, "Share Co-Guardian Invite Code"));
                    })
                    .setNegativeButton("Done", null)
                    .create();
                codeDialog.show();
            });
        });
    }

    private void clearLocateStateMachine() {
        if (locateStage2Runnable != null) {
            locateStateMachineHandler.removeCallbacks(locateStage2Runnable);
            locateStage2Runnable = null;
        }
        if (locateStage3Runnable != null) {
            locateStateMachineHandler.removeCallbacks(locateStage3Runnable);
            locateStage3Runnable = null;
        }
        if (locateTimeoutRunnable != null) {
            locateStateMachineHandler.removeCallbacks(locateTimeoutRunnable);
            locateTimeoutRunnable = null;
        }
        activeLocateTargetUid = null;
        activeLocateTargetName = null;
        initialTargetTs = 0L;
    }

    private void initiateRemoteLocateStateMachine(UserLocation target, String targetName) {
        if (target == null) return;
        clearLocateStateMachine();

        final String targetUid = target.getResolvedUserId();
        activeLocateTargetUid = targetUid;
        activeLocateTargetName = targetName;
        initialTargetTs = target.getLongTimestamp();

        // 1. Dispatch remote wakeup command
        com.jdroidx.jfindme.data.remote.FirebaseComm.triggerRemoteLocateDevice(targetUid);

        // 2. Animate camera to last known fix via TacticalCameraController SSOT
        if (cameraController != null) {
            cameraController.focusOnPeer(target);
        } else if (googleMap != null && (target.getLatitude() != 0.0 || target.getLongitude() != 0.0)) {
            LatLng pos = new LatLng(target.getLatitude(), target.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17f));
        }

        // Stage 1: DISPATCHED
        if (tvAddressMarquee != null) {
            tvAddressMarquee.setText("🛰️ [LOCATE: DISPATCHED] Wakeup ping sent to " + targetName + "... (Handshake Pending)");
            tvAddressMarquee.setSelected(true);
        }
        UiArchitect.showSlideNotificationBanner(this, "🛰️ Locate Dispatched", "High-accuracy GNSS request transmitted to " + targetName + ".", false, 2500, null, null);

        // Stage 2: PING_SENT (after 1800ms)
        locateStage2Runnable = () -> {
            if (activeLocateTargetUid != null && activeLocateTargetUid.equals(targetUid)) {
                if (tvAddressMarquee != null) {
                    tvAddressMarquee.setText("🛰️ [LOCATE: PING SENT] Awaiting remote GNSS handshake from " + targetName + "... ");
                    tvAddressMarquee.setSelected(true);
                }
            }
        };
        locateStateMachineHandler.postDelayed(locateStage2Runnable, 1800);

        // Stage 3: REMOTE_GPS_WAKING (after 4500ms)
        locateStage3Runnable = () -> {
            if (activeLocateTargetUid != null && activeLocateTargetUid.equals(targetUid)) {
                if (tvAddressMarquee != null) {
                    tvAddressMarquee.setText("🛰️ [LOCATE: REMOTE GPS WAKING] Satellite acquisition in progress for " + targetName + "... ");
                    tvAddressMarquee.setSelected(true);
                }
            }
        };
        locateStateMachineHandler.postDelayed(locateStage3Runnable, 4500);

        // Stage 4: REQUEST_TIMED_OUT (dynamic timeout from RegistryManager)
        long timeoutMs = com.jdroidx.jfindme.utils.RegistryManager.getRemoteLocateTimeoutMs(this);
        locateTimeoutRunnable = () -> {
            if (activeLocateTargetUid != null && activeLocateTargetUid.equals(targetUid)) {
                if (tvAddressMarquee != null) {
                    tvAddressMarquee.setText("⚠️ [LOCATE TIMEOUT] " + targetName + " did not respond within " + (timeoutMs / 1000) + "s. Showing cached position.");
                    tvAddressMarquee.setSelected(true);
                }
                UiArchitect.showSlideNotificationBanner(this, "🛰️ Locate Timeout", targetName + " did not respond within " + (timeoutMs / 1000) + "s. Showing cached telemetry.", true, 4000, null, null);
                lastTimedOutTargetUid = targetUid;
                lastTimedOutTargetName = targetName;
                lastTimedOutInitialTs = initialTargetTs;
                lastTimedOutEpoch = System.currentTimeMillis();
                if (target.getLatitude() == 0.0 && target.getLongitude() == 0.0) {
                    locateTargetWithHistoryFallback(targetUid);
                }
                clearLocateStateMachine();
            }
        };
        locateStateMachineHandler.postDelayed(locateTimeoutRunnable, timeoutMs);
    }

    private void showUnlinkDeviceDialog(String targetUid, String targetName) {
        String myGrp = com.jdroidx.jfindme.utils.RegistryManager.getActiveGroup(this);
        com.jdroidx.jfindme.ui.base.SecurityDialogManager.showUnlinkDeviceDialog(
            this,
            targetUid,
            targetName,
            myGrp,
            new com.jdroidx.jfindme.ui.base.SecurityDialogManager.OnDialogActionListener() {
                @Override
                public void onActionSuccess(String actionType, String targetId) {
                    if (viewModel != null) {
                        viewModel.refreshLocations();
                        viewModel.refreshAllData();
                    }
                    Toast.makeText(MapActivity.this, "Device " + targetName + " unlinked successfully.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onActionCancelled() {
                    // Action cancelled by user or dismissed
                }
            }
        );
    }

    private void navigateToTarget(double lat, double lng, String targetName) {
        if (lat == 0.0 && lng == 0.0) {
            UiArchitect.showValidationDialog(this, "Location Unavailable", "Target " + targetName + " has not reported valid GPS coordinates yet.", null);
            return;
        }
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
            UiArchitect.showValidationDialog(this, "Navigation Failed", "Unable to launch navigation: " + e.getMessage(), null);
        }
    }

    private void sendAcousticPing(String targetUid, String targetName) {
        if (targetUid == null || targetUid.isEmpty() || viewModel == null) return;
        String myName = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this);
        int sirenDuration = com.jdroidx.jfindme.utils.RegistryManager.getSirenDurationSec(this);
        viewModel.pokeUser(targetUid, myName);
        com.jdroidx.jfindme.data.remote.FirebaseComm.triggerRemoteRingDevice(targetUid, sirenDuration);
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
            tvAddressMarquee.setText("📍 Locating " + (targetName != null && !targetName.isEmpty() ? targetName : "Device") + "...");
            tvAddressMarquee.setSelected(true);
        }
        
        // 🌍 OOPS MVC Reverse Geocode Engine (2-Tier LRU & SQLite Cache SSOT)
        com.jdroidx.jfindme.utils.ReverseGeocodeEngine.resolveAddress(this, lat, lng, address -> {
            String roleTag = (role != null && !role.isEmpty()) ? " [" + role + "]" : "";
            String speedStr = com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.formatSpeed(speedMps);
            String batStr = batteryLevel >= 0 ? " • 🔋" + batteryLevel + "%" : "";
            String sosStr = isSos ? " • 🚨 SOS ACTIVE" : "";
            String displayText = "📍 " + (targetName != null ? targetName : "Device") + roleTag + " • " + address + " • ⏱️ " + timeStr + " • " + speedStr + batStr + sosStr;
            if (tvAddressMarquee != null) {
                tvAddressMarquee.setText(displayText);
                tvAddressMarquee.setSelected(true);
            }
        });
    }

    private void processMarqueeAddress(java.util.List<android.location.Address> addresses, double lat, double lng, String targetName, String timeStr, float speedMps, int batteryLevel, boolean isSos, String role) {
        if (tvAddressMarquee == null) return;
        if (activeLocateTargetUid != null) return; // 🛡️ Prevent background geocoder from overwriting active locate state machine
        String addressStr;
        if (addresses != null && !addresses.isEmpty()) {
            addressStr = addresses.get(0).getAddressLine(0);
        } else {
            addressStr = String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng);
        }
        
        StringBuilder sb = new StringBuilder();
        if (isSos) {
            sb.append("🚨 SOS: ");
        } else {
            sb.append("📍 ");
        }
        
        String cleanName = (targetName != null && !targetName.isEmpty() && !targetName.startsWith("Agent_")) ? targetName : "Device";
        sb.append(cleanName);
        sb.append(" • ").append(addressStr);
        
        if (batteryLevel >= 0) {
            sb.append(" • 🔋 ").append(batteryLevel).append("%");
        }
        
        sb.append(com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.getMarqueeSpeedTag(speedMps));
        
        if (timeStr != null && !timeStr.isEmpty()) {
            sb.append(" • ").append(timeStr);
        }
        
        String finalPayload = sb.toString();
        tvAddressMarquee.setText(finalPayload);
        tvAddressMarquee.setSelected(true);
    }

    // [Variables] activeSize, btnSize, dpPadding, frame, frameParams, isAlert, btnBg, iv, ivParams | [Constants] ACTION_ALERT | [Dependencies] FrameLayout, ImageView, GradientDrawable, IconTable, UiArchitect | [Integration Points] sideActionPanel, Map Controls
    private View createSmallIcon(String tableKey, View.OnClickListener listener) {
        int activeSize = UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
        int btnSize = Math.max(activeSize, scale(34));
        int dpPadding = scale(6);

        FrameLayout frame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(btnSize, btnSize);
        frameParams.setMargins(0, scale(2), 0, scale(2));
        frame.setLayoutParams(frameParams);

        boolean isAlert = "ACTION_ALERT".equals(tableKey);
        boolean isDarkMap = ThemeManager.isDarkTheme(getResolvedThemeId());

        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        if (isAlert) {
            btnBg.setColor(Color.parseColor("#4DEF4444"));
            btnBg.setStroke(scale(2), Color.parseColor("#EF4444"));
        } else {
            if (isDarkMap) {
                btnBg.setColor(Color.parseColor("#991E293B"));
                btnBg.setStroke(scale(1), Color.parseColor("#2638BDF8"));
            } else {
                btnBg.setColor(Color.argb(240, 248, 250, 252)); // Light theme porcelain
                btnBg.setStroke(scale(1), Color.parseColor("#3394A3B8")); // Soft slate contour
            }
        }
        frame.setBackground(btnBg);
        frame.setElevation(scale(4));

        ImageView iv = new ImageView(this);
        com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, iv, tableKey);
        
        // 🛡️ Touch & Render Shield: Clean up background, elevation, and touch traps set by IconTable on child ImageView
        iv.setBackground(null);
        iv.setElevation(0f);
        iv.setPadding(dpPadding, dpPadding, dpPadding, dpPadding);
        FrameLayout.LayoutParams ivParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        iv.setLayoutParams(ivParams);
        
        // Ensure child iv doesn't trap touch events away from parent frame
        iv.setClickable(false);
        iv.setFocusable(false);
        iv.setLongClickable(false);
        iv.setOnLongClickListener(null);
        
        // Dual safety dispatch: If touch event somehow triggers iv directly, delegate to parent frame
        iv.setOnClickListener(v -> frame.performClick());
        iv.setOnLongClickListener(v -> frame.performLongClick());

        frame.setTag(tableKey);
        frame.addView(iv);

        // 🔔 Notification Indicator Badge: Emits visual indicator if an inbound meet point invitation is pending
        if ("ACTION_GHOST".equals(tableKey) || "ACTION_BUTTERFLY_POINT".equals(tableKey)) {
            if (ButterflyPointDialog.hasPendingInvitation(this)) {
                View badgeDot = new View(this);
                badgeDot.setTag("BADGE_DOT");
                int badgeSize = scale(9);
                FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(badgeSize, badgeSize);
                badgeLp.gravity = Gravity.TOP | Gravity.END;
                badgeLp.topMargin = scale(2);
                badgeLp.rightMargin = scale(2);
                badgeDot.setLayoutParams(badgeLp);

                android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
                dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                dotBg.setColor(Color.parseColor("#EF4444")); // Red alert indicator dot
                dotBg.setStroke(scale(1), Color.WHITE);
                badgeDot.setBackground(dotBg);
                frame.addView(badgeDot);
            }
        }

        frame.setClickable(true);
        frame.setFocusable(true);
        frame.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (listener != null) {
                listener.onClick(v);
            }
        });

        // 🚀 QA Fix: Global Focus State Loop (Enter / D-Pad Support)
        frame.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });
        return frame;
    }

    // [Variables] panel, hasPending, i, child, frame, badge, badgeDot, badgeSize, badgeLp, dotBg | [Constants] "BADGE_DOT", "ACTION_GHOST", "ACTION_BUTTERFLY_POINT" | [Dependencies] FrameLayout, ButterflyPointDialog, GradientDrawable | [Integration Points] Real-Time Butterfly Meet Point Inbound Indicator
    public void updateButterflyBadge() {
        runOnUiThread(() -> {
            if (binding == null || binding.sideActionPanel == null) return;
            LinearLayout panel = binding.sideActionPanel;
            boolean hasPending = ButterflyPointDialog.hasPendingInvitation(this);
            for (int i = 0; i < panel.getChildCount(); i++) {
                View child = panel.getChildAt(i);
                if (child instanceof FrameLayout && ("ACTION_GHOST".equals(child.getTag()) || "ACTION_BUTTERFLY_POINT".equals(child.getTag()))) {
                    FrameLayout frame = (FrameLayout) child;
                    View badge = frame.findViewWithTag("BADGE_DOT");
                    if (hasPending) {
                        if (badge == null) {
                            View badgeDot = new View(this);
                            badgeDot.setTag("BADGE_DOT");
                            int badgeSize = scale(9);
                            FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(badgeSize, badgeSize);
                            badgeLp.gravity = Gravity.TOP | Gravity.END;
                            badgeLp.topMargin = scale(2);
                            badgeLp.rightMargin = scale(2);
                            badgeDot.setLayoutParams(badgeLp);

                            android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
                            dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                            dotBg.setColor(Color.parseColor("#EF4444")); // Red alert indicator dot
                            dotBg.setStroke(scale(1), Color.WHITE);
                            badgeDot.setBackground(dotBg);
                            frame.addView(badgeDot);
                        }
                    } else {
                        if (badge != null) {
                            frame.removeView(badge);
                        }
                    }
                }
            }
        });
    }

    // [Variables] actionKey, expBg, expPad | [Constants] "ACTION_MINIMIZE", "ACTION_EXPAND" | [Dependencies] IconTable, GradientDrawable | [Integration Points] expandIcon, toggleMapExpansion
    private void updateExpandIconState(boolean expanded) {
        if (binding == null || binding.expandIcon == null) return;
        String actionKey = expanded ? "ACTION_MINIMIZE" : "ACTION_EXPAND";
        com.jdroidx.jfindme.ui.base.IconTable.applyTheme(this, binding.expandIcon, actionKey);
        applyAccessibility(binding.expandIcon, expanded ? "Minimize Map View" : "Toggle Full Screen Map");
        android.graphics.drawable.GradientDrawable expBg = new android.graphics.drawable.GradientDrawable();
        expBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        expBg.setColor(Color.parseColor("#CC0F172A"));
        expBg.setStroke(scale(1), Color.parseColor("#4038BDF8"));
        binding.expandIcon.setBackground(expBg);
        binding.expandIcon.setElevation(scale(6));
        int expPad = scale(6);
        binding.expandIcon.setPadding(expPad, expPad, expPad, expPad);
    }

    @Override
    protected void setupLogic() {
        // ≡ƒº▒ UX Override: Architectural lifecycle fix -> Safely init after views configure
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);
        
        // ⚡ Cross-OEM / Xiaomi MIUI Battery Whitelist Hook: Protect background tracking on Redmi Note 7 Pro, Redmi Note 5 Pro, Redmi 9A/9i
        com.jdroidx.jfindme.utils.MiuiOptimizationHelper.checkAndPromptMiuiOptimization(this);
        
        // 🔒 Back-Stack Protocol: Double-Tap Exit on Live Dashboard (Guarded against duplicate registration)
        if (backPressedCallback == null) {
            backPressedCallback = new androidx.activity.OnBackPressedCallback(true) {
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
                        finishAffinity(); // Liquidates UI task safely while preserving background sentinel
                    } else {
                        Toast.makeText(MapActivity.this, "Press BACK again to exit Map Dashboard", Toast.LENGTH_SHORT).show();
                        backPressedTime = System.currentTimeMillis();
                    }
                }
            };
            getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        }
        
        // 🗺️ Re-attach SupportMapFragment cleanly to fresh container
        SupportMapFragment oldFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(ID_MAP_CONTAINER);
        if (oldFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(oldFragment).commitNowAllowingStateLoss();
            try { getSupportFragmentManager().executePendingTransactions(); } catch (Exception ignored) {}
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
            // 🔄 Step 1: Bidirectional Reconciliation with Local DB (Unified Avatar SSOT)
            reconcileDeviceAvatarsWithLocalDb(locations);

            // ⚡ Optimized Throttled Marker Rendering & Floating Side Panel Refresh
            if (googleMap != null) {
                scheduleThrottledMarkerRender(locations);
            }
            if (viewModel != null) {
                updateFloatingDevicePanel(locations, viewModel.getMyLocation().getValue());
            }

            // 🦋 Real-time Closed Loop: Update live vector lines for active Butterfly Point
            com.jdroidx.jfindme.data.model.ButterflyPoint activePt =
                com.jdroidx.jfindme.engine.map.ButterflyPointEngine.getActiveButterflyPoint();
            if (activePt != null && googleMap != null) {
                focusOnButterflyPoint(activePt, false);
            }
            
            // 🚀 Tactical Zoom Lock for Friend / Peer Locate
            if (selectedUserId != null && !hasZoomedToTarget && googleMap != null) {
                locateTargetWithHistoryFallback(selectedUserId);
            }

            // 🛰️ Remote Locate State Machine Closed-Loop Resolution
            if (activeLocateTargetUid != null && locations != null) {
                for (UserLocation ul : locations) {
                    if (ul != null && activeLocateTargetUid.equals(ul.getResolvedUserId())) {
                        if (ul.getLongTimestamp() > initialTargetTs) {
                            String lockedName = activeLocateTargetName != null ? activeLocateTargetName : ul.getUserName();
                            clearLocateStateMachine();

                            if (cameraController != null) {
                                cameraController.focusOnPeer(ul);
                            } else if (googleMap != null) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(ul.getLatitude(), ul.getLongitude()), 17.5f));
                            }
                            if (tvAddressMarquee != null) {
                                tvAddressMarquee.setText("✅ [LOCATION LOCKED] Live GNSS fix acquired for " + lockedName + " (" + com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.formatSpeed(ul.getSpeed()) + ")");
                                tvAddressMarquee.setSelected(true);
                            }
                            UiArchitect.showSlideNotificationBanner(this, "🎯 GPS Locked", "Fresh coordinates acquired for " + lockedName + " (" + com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.formatSpeed(ul.getSpeed()) + ")", false, 3500, null, null);
                            updateAddressMarquee(ul);
                            break;
                        }
                    }
                }
            }

            // 🛰️ Late GPS Fix Catch: Detect delayed satellite fix for recently timed-out device
            if (lastTimedOutTargetUid != null && locations != null && (System.currentTimeMillis() - lastTimedOutEpoch < 90000L)) {
                for (UserLocation ul : locations) {
                    if (ul != null && lastTimedOutTargetUid.equals(ul.getResolvedUserId())) {
                        if (ul.getLongTimestamp() > lastTimedOutInitialTs) {
                            String lateName = lastTimedOutTargetName != null ? lastTimedOutTargetName : ul.getUserName();
                            lastTimedOutTargetUid = null;
                            if (tvAddressMarquee != null) {
                                tvAddressMarquee.setText("✅ [DELAYED FIX RECEIVED] Satellite lock acquired for " + lateName + " (" + com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.formatSpeed(ul.getSpeed()) + ")");
                                tvAddressMarquee.setSelected(true);
                            }
                            UiArchitect.showSlideNotificationBanner(this, "🛰️ Delayed GPS Arrived", "Late satellite fix acquired for " + lateName + " (" + com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.formatSpeed(ul.getSpeed()) + ")", false, 4000, null, null);
                            if (cameraController != null) {
                                cameraController.focusOnPeer(ul);
                            } else if (googleMap != null) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(ul.getLatitude(), ul.getLongitude())));
                            }
                            updateAddressMarquee(ul);
                            break;
                        }
                    }
                }
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
        if (cameraController != null) {
            cameraController.attachMap(this.googleMap);
        }
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        this.googleMap.setBuildingsEnabled(true); // 🚀 3D Perspective Fix (V12.0)
        this.googleMap.setIndoorEnabled(true);
        this.googleMap.getUiSettings().setCompassEnabled(true);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.googleMap.getUiSettings().setTiltGesturesEnabled(true);
        UiArchitect.applyMapStyle(this, this.googleMap);
        
        // 🚀 Dynamic Anti-Overlap Zoom Hook: Recomputes screen-space spiderfication as map zooms/pans (Throttled)
        this.googleMap.setOnCameraIdleListener(() -> {
            if (viewModel != null && viewModel.getUserLocations().getValue() != null && this.googleMap != null) {
                scheduleThrottledMarkerRender(viewModel.getUserLocations().getValue());
            }
        });
        
        googleMap.setOnMarkerClickListener(marker -> {
            if (marker.getTitle() == null && marker.getTag() == null) return true;
            
            String markerTag = marker.getTag() != null ? marker.getTag().toString() : "";
            // 🦋 Butterfly Point Marker Click: Open Tactical Rendezvous Hub
            if ("BUTTERFLY_POINT".equals(markerTag)) {
                ButterflyPointDialog.show(this);
                return true;
            }

            String selfUid = FirebaseAuth.getInstance().getUid();
            String selfAgent = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
            String rawTitle = marker.getTitle() != null ? marker.getTitle() : "";
            
            if (rawTitle.equalsIgnoreCase("You") || (!markerTag.isEmpty() && (markerTag.equals(selfUid) || markerTag.equals(selfAgent)))) {
                showSelfActionSheet();
                return true;
            }
            
            String markerSnippet = marker.getSnippet() != null ? marker.getSnippet() : "";
            
            List<UserLocation> locs = viewModel.getUserLocations().getValue();
            if (locs != null) {
                com.jdroidx.jfindme.data.repository.DeviceRepository deviceRepo = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this);
                for (UserLocation l : locs) {
                    String uid = l.getResolvedUserId();
                    String devCentricName = deviceRepo.getDeviceCentricName(uid);
                    String devId = l.getResolvedDeviceId();
                    if ((!markerTag.isEmpty() && (markerTag.equals(uid) || (!devId.isEmpty() && markerTag.equals(devId))))
                        || (!markerSnippet.isEmpty() && (markerSnippet.contains(uid) || (!devId.isEmpty() && markerSnippet.contains(devId))))
                        || rawTitle.equals(l.getUserName())
                        || rawTitle.equals(devCentricName)) {
                        selectedUserId = uid;
                        if (cameraController != null) {
                            cameraController.focusOnPeer(l);
                        }
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
            // 🦋 Interactive Map Location Picker for Butterfly Point Studio
            if (isPickingButterflyLocation && onMapLocationPickedCallback != null) {
                isPickingButterflyLocation = false;
                MapLocationPickedCallback cb = onMapLocationPickedCallback;
                onMapLocationPickedCallback = null;
                handleMapPickedLocation(latLng, cb);
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
        
        // Initial Zoom Fix: Restore camera seamlessly if recovering from orientation change
        double defLat = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultLatitude(this);
        double defLng = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultLongitude(this);
        float defZoom = com.jdroidx.jfindme.utils.RegistryManager.INSTANCE.getDefaultZoom(this);
        
        if (restoredCamLat != 0.0 && restoredCamLng != 0.0) {
            com.google.android.gms.maps.model.CameraPosition restoredPos = new com.google.android.gms.maps.model.CameraPosition.Builder()
                .target(new LatLng(restoredCamLat, restoredCamLng))
                .zoom(restoredCamZoom > 0f ? restoredCamZoom : defZoom)
                .tilt(restoredCamTilt)
                .bearing(restoredCamBearing)
                .build();
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(restoredPos));
            restoredCamLat = 0.0;
            restoredCamLng = 0.0;
        } else {
            LatLng defaultLocation = new LatLng(defLat, defLng);
            com.google.android.gms.maps.model.CameraPosition pos = new com.google.android.gms.maps.model.CameraPosition.Builder()
                .target(defaultLocation).zoom(defZoom).tilt(45f).build();
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
        }

        if (selectedUserId != null && !selectedUserId.isEmpty()) {
            locateTargetWithHistoryFallback(selectedUserId);
        }

        // 🦋 Butterfly Point Auto-Restoration & Deep-Link Resolution
        checkAndFocusPendingButterflyPoint();
        restoreActiveButterflyPointOverlay();

        // 🚀 CRITICAL FIX: Immediate Map Initialization Render
        // Guarantees all seeded & live peer avatars are scheduled and drawn as soon as GoogleMap is physically bound natively
        if (viewModel != null && viewModel.getUserLocations() != null && viewModel.getUserLocations().getValue() != null) {
            scheduleThrottledMarkerRender(viewModel.getUserLocations().getValue());
        }
    }

    private void restoreActiveButterflyPointOverlay() {
        new Thread(() -> {
            try {
                java.util.List<com.jdroidx.jfindme.data.model.ButterflyPoint> activePts =
                    com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this).getActiveButterflyPoints();
                if (activePts != null && !activePts.isEmpty()) {
                    com.jdroidx.jfindme.data.model.ButterflyPoint latest = activePts.get(0);
                    runOnUiThread(() -> {
                        if (googleMap != null) {
                            focusOnButterflyPoint(latest, false);
                        }
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    public void engageMapPointPicker(MapLocationPickedCallback callback) {
        this.isPickingButterflyLocation = true;
        this.onMapLocationPickedCallback = callback;
        UiArchitect.showSlideNotificationBanner(
            this,
            "📍 MAP PICKER ENGAGED",
            "Tap anywhere on the live map to set your rendezvous point",
            false,
            6000,
            "CANCEL",
            () -> {
                isPickingButterflyLocation = false;
                onMapLocationPickedCallback = null;
            }
        );
    }

    private void handleMapPickedLocation(LatLng latLng, MapLocationPickedCallback cb) {
        UiArchitect.showSlideNotificationBanner(this, "📍 COORDINATES LOCKED", String.format(java.util.Locale.US, "%.5f, %.5f - Resolving location...", latLng.latitude, latLng.longitude), false, 2500, null, null);
        new Thread(() -> {
            String resolved = "";
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
                java.util.List<android.location.Address> matches = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (matches != null && !matches.isEmpty() && matches.get(0).getAddressLine(0) != null) {
                    resolved = matches.get(0).getAddressLine(0);
                }
            } catch (Exception ignored) {}
            final String finalAddr = resolved;
            runOnUiThread(() -> {
                if (cb != null) {
                    cb.onLocationPicked(latLng.latitude, latLng.longitude, finalAddr);
                }
            });
        }).start();
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
                boolean hasPrompted = com.jdroidx.jfindme.utils.RegistryManager.isBgLocationPrompted(this);
                if (!hasPrompted) {
                    com.jdroidx.jfindme.utils.RegistryManager.setBgLocationPrompted(this, true);
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
                    if (cameraController != null) {
                        cameraController.focusOnHost(location, false);
                    } else {
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        com.google.android.gms.maps.model.CameraPosition pos = new com.google.android.gms.maps.model.CameraPosition.Builder()
                            .target(current).zoom(16.5f).tilt(45f).build();
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
                    }
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
            updateFloatingDevicePanel(viewModel.getUserLocations().getValue(), location);
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
        boolean hideMode = com.jdroidx.jfindme.utils.RegistryManager.isCalloutHideMode(this);
        long revealedAt = com.jdroidx.jfindme.utils.RegistryManager.getLong(this, "callout_revealed_at", 0L);
        boolean showName = !hideMode || (System.currentTimeMillis() - revealedAt < 5000);
        
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
        com.jdroidx.jfindme.utils.RegistryManager.putLong(this, "callout_revealed_at", System.currentTimeMillis());
        if (lastKnownLocation != null) updateUserPositionOnMap(lastKnownLocation);
        
        if (com.jdroidx.jfindme.utils.RegistryManager.isCalloutHideMode(this)) {
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
        com.jdroidx.jfindme.data.model.ButterflyPoint activePt = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.getActiveButterflyPoint();
        if (activePt != null) {
            com.jdroidx.jfindme.engine.map.ButterflyPointEngine.startActivePointPeerSync(this, activePt.getPointId());
        }
        if (binding != null && binding.headerFlipper != null && !binding.headerFlipper.isFlipping()) {
            binding.headerFlipper.startFlipping();
        }
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
        
        // 🔄 Master Identity Reconciliation: Ensure local custom avatar is synchronized to Cloud & SQLite
        com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.ensureLocalAvatarSynchronized(this);
        com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.fetchAndCachePeerAvatars(this);
        
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
            if (viewModel.getUserLocations().getValue() != null) {
                updateRadarScanner(viewModel.getUserLocations().getValue());
                distantDeviceHysteresisSet.clear();
                updateFloatingDevicePanel(viewModel.getUserLocations().getValue(), lastKnownLocation);
            }
        }
        updateButterflyBadge();
        // ⚡ 30-Second Open Handshake Protocol: Host immediately uploads own fresh telemetry & syncs all peer devices
        executeInstant30SecondHandshakeBurst();
    }

    // [Variables] db, myAgentId, myUid, epochId, activeGrp, battery, isCharging, bm, status, hostLat, hostLng, lm, gps, net, now, hostLoc, fused, freshLoc, uid | [Constants] PRIORITY_HIGH_ACCURACY | [Dependencies] BatteryManager, LocationManager, FusedLocationProviderClient, FirebaseComm, DatabaseHelper, LocationRepository, SystemLogger | [Integration Points] 30-Second Rapid Host-Peer Synchronization Handshake
    public void executeInstant30SecondHandshakeBurst() {
        new Thread(() -> {
            try {
                com.jdroidx.jfindme.data.repository.DeviceRepository db = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this);
                String myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
                String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                if (myUid == null) myUid = "";
                String epochId = com.jdroidx.jfindme.utils.RegistryManager.getInstallEpochId(this);
                String activeGrp = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(this);

                // 1. Read battery vitals
                int battery = 100;
                boolean isCharging = false;
                try {
                    android.os.BatteryManager bm = (android.os.BatteryManager) getSystemService(BATTERY_SERVICE);
                    if (bm != null) {
                        battery = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
                        int status = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS);
                        isCharging = (status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL);
                    }
                } catch (Exception ignored) {}

                // 2. Immediate Host GPS Fix & Cloud Push
                double hostLat = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLatitude(this);
                double hostLng = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLongitude(this);

                try {
                    android.location.LocationManager lm = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
                    if (lm != null) {
                        android.location.Location gps = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                        if (gps != null && (gps.getLatitude() != 0.0 || gps.getLongitude() != 0.0)) {
                            hostLat = gps.getLatitude();
                            hostLng = gps.getLongitude();
                        } else {
                            android.location.Location net = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                            if (net != null && (net.getLatitude() != 0.0 || net.getLongitude() != 0.0)) {
                                hostLat = net.getLatitude();
                                hostLng = net.getLongitude();
                            }
                        }
                    }
                } catch (SecurityException ignored) {}

                long now = System.currentTimeMillis();
                String hostNick = com.jdroidx.jfindme.utils.RegistryManager.getUserNickname(this);
                int hostEquipped = com.jdroidx.jfindme.utils.RegistryManager.getEquippedAvatarId(this);
                String hostAvatar = (hostEquipped > 0) ? String.valueOf(hostEquipped) : com.jdroidx.jfindme.utils.RegistryManager.getString(this, "avatar_id", "AV_" + myAgentId + "_01");

                com.jdroidx.jfindme.data.model.UserLocation hostLoc = new com.jdroidx.jfindme.data.model.UserLocation(
                    myAgentId, hostLat, hostLng, 0f
                );
                hostLoc.setUserName(hostNick);
                hostLoc.setAvatar_id(hostAvatar);
                hostLoc.setBatteryLevel(battery);
                hostLoc.setTimestamp(String.valueOf(now));

                // Publish Host to Firebase RTDB immediately
                if (!myUid.isEmpty()) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.syncPartitionedLocationTrace(myUid, myAgentId, epochId, hostLoc, null);
                }

                // Update Host in local SQLite
                db.updateDeviceVitalsTelemetry(myAgentId, battery, now, isCharging, null);

                final int finalBattery = battery;

                // 3. Trigger fresh high-accuracy GPS update via FusedClient if permitted
                try {
                    com.google.android.gms.location.FusedLocationProviderClient fused = 
                        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);
                    fused.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener(freshLoc -> {
                            if (freshLoc != null && (freshLoc.getLatitude() != 0.0 || freshLoc.getLongitude() != 0.0)) {
                                com.jdroidx.jfindme.data.model.UserLocation precisionLoc = new com.jdroidx.jfindme.data.model.UserLocation(
                                    myAgentId, freshLoc.getLatitude(), freshLoc.getLongitude(), freshLoc.getSpeed()
                                );
                                precisionLoc.setUserName(hostNick);
                                precisionLoc.setAvatar_id(hostAvatar);
                                precisionLoc.setBatteryLevel(finalBattery);
                                precisionLoc.setTimestamp(String.valueOf(System.currentTimeMillis()));
                                String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                                if (uid != null && !uid.isEmpty()) {
                                    com.jdroidx.jfindme.data.remote.FirebaseComm.syncPartitionedLocationTrace(uid, myAgentId, epochId, precisionLoc, null);
                                }
                                com.jdroidx.jfindme.data.repository.LocationRepository.Companion.getInstance(this).updateMyLocation(precisionLoc);
                            }
                        });
                } catch (SecurityException ignored) {}

            } catch (Exception e) {
                com.jdroidx.jfindme.utils.SystemLogger.e(TAG, "30s Handshake error: " + e.getMessage());
            }
        }).start();

        // Pulsed sync at T+10s to guarantee all remote responses are rendered
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (viewModel != null && !isFinishing()) {
                viewModel.refreshLocations();
            }
        }, 10000);
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
            double savedLat = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLatitude(this);
            double savedLng = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLongitude(this);
            if (savedLat != 0.0 || savedLng != 0.0) {
                referenceLoc = new Location("fallback");
                referenceLoc.setLatitude(savedLat);
                referenceLoc.setLongitude(savedLng);
            }
        }

        String myUid = FirebaseAuth.getInstance().getUid();
        String myAgentId = com.jdroidx.jfindme.utils.RegistryManager.getAgentId(this);
        
        java.util.Set<String> activeDeviceIds = new java.util.HashSet<>();

        // 1. In-place update or insert peer device cards (Sorted by Waterfall Priority: SOS emergency first, then Prime Objective Child/Ward, then newest to oldest fix)
        if (locations != null) {
            List<UserLocation> sortedList = new ArrayList<>(locations);
            java.util.Collections.sort(sortedList, (a, b) -> {
                if (a == null || b == null) return 0;
                if (a.isSosActive() != b.isSosActive()) {
                    return a.isSosActive() ? -1 : 1; // SOS priority
                }
                boolean aChild = isChildOrPrimeTarget(a);
                boolean bChild = isChildOrPrimeTarget(b);
                if (aChild != bChild) {
                    return aChild ? -1 : 1; // Child / Protected Ward target (dynamic SSOT via RegistryManager)
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
                    float distance = com.jdroidx.jfindme.utils.SpatialMathUtil.computeDistanceBetweenFloat(
                        referenceLoc.getLatitude(), referenceLoc.getLongitude(), 
                        ul.getLatitude(), ul.getLongitude());
                    distText = distance >= 1000f ? String.format(java.util.Locale.US, "%.1f km away", distance / 1000f) : String.format(java.util.Locale.US, "%.0f m away", distance);
                    int radarProximityRadius = com.jdroidx.jfindme.utils.RegistryManager.getRadarProximityRadiusMeters(this);
                    if (distance > radarProximityRadius) {
                        row1Color = Color.parseColor("#38BDF8"); // Vibrant Sky Blue for external nodes
                    }
                }

                // Speed / Motion telemetry
                float spd = ul.getSpeed();
                String motionStr = com.jdroidx.jfindme.utils.SpeedTelemetryFormatter.getRadarMotionTag(spd);

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
                        if (cameraController != null) {
                            cameraController.focusOnPeer(targetUl);
                        } else if (googleMap != null && (targetUl.getLatitude() != 0.0 || targetUl.getLongitude() != 0.0)) {
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
                        if (cameraController != null) {
                            cameraController.focusOnPeer(targetUl);
                        } else if (googleMap != null && (targetUl.getLatitude() != 0.0 || targetUl.getLongitude() != 0.0)) {
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

    /**
     * 🧸 PRIME OBJECTIVE: Evaluates whether a peer node is a Child / Protected Ward target (dynamic SSOT via RegistryManager)
     */
    private boolean isChildOrPrimeTarget(UserLocation ul) {
        if (ul == null) return false;
        String uid = ul.getResolvedUserId();
        String primeTarget = com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(this);
        if (primeTarget.equalsIgnoreCase(uid) || (ul.getDeviceId() != null && ul.getDeviceId().toString().equalsIgnoreCase(primeTarget))) {
            return true;
        }
        String primeName = com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetName(this);
        String uName = ul.getUserName();
        if (uName != null && !primeName.isEmpty() && uName.toUpperCase(java.util.Locale.US).contains(primeName.toUpperCase(java.util.Locale.US))) {
            return true;
        }
        String rf = ul.getRole_family();
        if (rf != null && (rf.equalsIgnoreCase("Child") || rf.equalsIgnoreCase("Daughter") || 
            rf.equalsIgnoreCase("Son") || rf.equalsIgnoreCase("Ward") || 
            rf.equalsIgnoreCase("Dependent") || rf.equalsIgnoreCase("DEPENDENT_CHILD"))) {
            return true;
        }
        return com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this).isChildOrWard(uid);
    }

    public MapViewModel getViewModel() {
        return viewModel;
    }

    public GoogleMap getGoogleMap() {
        return googleMap;
    }

    public void focusOnButterflyPoint(com.jdroidx.jfindme.data.model.ButterflyPoint pt) {
        focusOnButterflyPoint(pt, true);
    }

    // [Variables] pt, shouldAnimateCamera, devPositions, allTargetCoords, bounds, loc, dId, uId, pos | [Constants] scale(80) | [Dependencies] ButterflyPointEngine, LatLng, LatLngBounds, CameraUpdateFactory | [Integration Points] MapActivity, ButterflyPointDialog
    public void focusOnButterflyPoint(com.jdroidx.jfindme.data.model.ButterflyPoint pt, boolean shouldAnimateCamera) {
        if (pt == null || googleMap == null) return;

        java.util.Map<String, LatLng> devPositions = new java.util.HashMap<>();
        java.util.List<LatLng> allTargetCoords = new java.util.ArrayList<>();
        allTargetCoords.add(new LatLng(pt.getLat(), pt.getLng()));

        // Include self device position if participating or creator
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String selfUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        android.location.Location myLoc = (viewModel != null && viewModel.getMyLocation().getValue() != null)
            ? viewModel.getMyLocation().getValue() : lastKnownLocation;
        if (myLoc != null && myLoc.getLatitude() != 0.0 && myLoc.getLongitude() != 0.0) {
            boolean isSelfParticipant = pt.getDevices().containsKey(selfAgentId)
                || (selfUid != null && pt.getDevices().containsKey(selfUid))
                || selfAgentId.equals(pt.getCreatorDeviceId());
            if (isSelfParticipant) {
                LatLng selfPos = new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
                devPositions.put(selfAgentId, selfPos);
                allTargetCoords.add(selfPos);
            }
        }

        if (viewModel != null && viewModel.getUserLocations().getValue() != null) {
            for (UserLocation loc : viewModel.getUserLocations().getValue()) {
                if (loc != null) {
                    String dId = loc.getDeviceId() != null ? loc.getDeviceId().toString() : "";
                    String uId = loc.getUserId() != null ? loc.getUserId().toString() : "";
                    if (pt.getDevices().containsKey(dId) || pt.getDevices().containsKey(uId)) {
                        if (loc.getLatitude() != 0.0 && loc.getLongitude() != 0.0) {
                            LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
                            devPositions.put(!dId.isEmpty() ? dId : uId, pos);
                            allTargetCoords.add(pos);
                        }
                    }
                }
            }
        }

        // 🔄 Peer Coordinate Fallback: Use canonical location history first, then fallback to last known entry
        for (java.util.Map.Entry<String, com.jdroidx.jfindme.data.model.ButterflyDeviceEntry> entry : pt.getDevices().entrySet()) {
            String dId = entry.getKey();
            if (!devPositions.containsKey(dId)) {
                java.util.Map<String, Object> histLoc = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getLatestHistoricalLocation(dId);
                if (histLoc != null && histLoc.containsKey("lat") && histLoc.containsKey("lng")) {
                    double hLat = ((Number) histLoc.get("lat")).doubleValue();
                    double hLng = ((Number) histLoc.get("lng")).doubleValue();
                    if (hLat != 0.0 && hLng != 0.0) {
                        LatLng pos = new LatLng(hLat, hLng);
                        devPositions.put(dId, pos);
                        allTargetCoords.add(pos);
                        continue;
                    }
                }
                com.jdroidx.jfindme.data.model.ButterflyDeviceEntry devEntry = entry.getValue();
                if (devEntry != null && devEntry.getLastLat() != 0.0 && devEntry.getLastLng() != 0.0) {
                    LatLng pos = new LatLng(devEntry.getLastLat(), devEntry.getLastLng());
                    devPositions.put(dId, pos);
                    allTargetCoords.add(pos);
                }
            }
        }

        com.jdroidx.jfindme.engine.map.ButterflyPointEngine.renderButterflyOverlay(googleMap, pt, devPositions);
        com.jdroidx.jfindme.engine.map.ButterflyPointEngine.startActivePointPeerSync(this, pt.getPointId());
        if (shouldAnimateCamera) {
            try {
                com.google.android.gms.maps.model.LatLngBounds bounds =
                    com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateMaxRadiusBounds(pt.getLat(), pt.getLng(), allTargetCoords);
                if (cameraController != null) {
                    cameraController.frameBoundingBox(bounds, scale(80));
                } else {
                    googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, scale(80)));
                }
            } catch (Exception ex) {
                if (cameraController != null) {
                    cameraController.focusOnCoordinate(pt.getLat(), pt.getLng(), 16.0f, pt.getPointName());
                } else {
                    googleMap.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(new LatLng(pt.getLat(), pt.getLng()), 16.0f));
                }
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

        // 5. Butterfly Meeting Point (Tactical Multi-Device Rendezvous & Quorum)
        // [Variables] items, v | [Constants] None | [Dependencies] createDrawerItem, ButterflyPointDialog, drawerLayout | [Integration Points] Navigation Drawer ⟷ Butterfly Rendezvous Hub
        items.addView(createDrawerItem("Butterfly Meeting Point", "Tactical Multi-Device Rendezvous & Quorum", com.jdroidx.jfindme.R.drawable.ic_butterfly_point, v -> {
            drawerLayout.closeDrawers();
            ButterflyPointDialog.show(this);
        }));

        // --- SUB-GROUP 2: NETWORK ---
        items.addView(createDrawerSectionHeader("NETWORK"));

        // 5. Device Pairing (QR Pairing Gateway)
        items.addView(createDrawerItem("Device Pairing", "QR Scan & Link Network Nodes", com.jdroidx.jfindme.R.drawable.ic_pairing_matrix, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.NetworkGatewayActivity.class));
            drawerLayout.closeDrawers();
        }));

        // 6. Device Sharing (Triadic Peer Delegation Hub)
        // [Variables] v | [Constants] None | [Dependencies] createDrawerItem, showDeviceSharingHubDialog, drawerLayout | [Integration Points] Navigation Drawer ⟷ Device Sharing Hub
        items.addView(createDrawerItem("Device Sharing", "Connect & Delegate Access Between Peers", android.R.drawable.ic_menu_share, v -> {
            drawerLayout.closeDrawers();
            showDeviceSharingHubDialog();
        }));

        // 7. Family & Friends (Unified Roster: Guardians, Children & MeetMyFriend Circles)
        // [Variables] i, v | [Constants] None | [Dependencies] createDrawerItem, FamilyFriendsActivity, drawerLayout | [Integration Points] Navigation Drawer ⟷ FamilyFriendsActivity (Consolidated)
        items.addView(createDrawerItem("Family & Friends", "Guardians, Children & MeetMyFriend Circles", com.jdroidx.jfindme.R.drawable.ic_family_friends, v -> { 
            Intent i = new Intent(this, com.jdroidx.jfindme.ui.group.FamilyFriendsActivity.class); 
            i.putExtra("REQUESTED_TAB", 0); 
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

    // [Variables] refLoc, lm, isGpsEnabled | [Constants] 17f | [Dependencies] GoogleMap, CameraUpdateFactory, LatLng, RegistryManager, LocationManager | [Integration Points] sideActionPanel, Map Controls
    private void recenterCamera() {
        if (googleMap == null) {
            Toast.makeText(this, "🗺️ Map engine initializing...", Toast.LENGTH_SHORT).show();
            return;
        }
        android.location.LocationManager lm = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = lm != null && (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER));
        if (!isGpsEnabled) {
            com.jdroidx.jfindme.ui.base.UiArchitect.showConfirmationDialog(
                this,
                "📍 Location Services Disabled",
                "Device GPS is turned off. Please enable Location in System Settings to allow real-time satellite tracking.",
                "SETTINGS",
                "CANCEL",
                () -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)),
                null
            );
            return;
        }
        Location refLoc = lastKnownLocation;
        if (refLoc == null && viewModel != null && viewModel.getMyLocation() != null) {
            refLoc = viewModel.getMyLocation().getValue();
        }
        if (refLoc == null) {
            double savedLat = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLatitude(this);
            double savedLng = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLongitude(this);
            if (savedLat != 0.0 || savedLng != 0.0) {
                refLoc = new Location("Registry");
                refLoc.setLatitude(savedLat);
                refLoc.setLongitude(savedLng);
            }
        }
        if (refLoc != null && (refLoc.getLatitude() != 0.0 || refLoc.getLongitude() != 0.0)) {
            this.lastKnownLocation = refLoc;
            if (cameraController != null) {
                cameraController.focusOnHost(refLoc, false);
            } else {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(refLoc.getLatitude(), refLoc.getLongitude()), 17f));
            }
        } else {
            Toast.makeText(this, "🛰️ Acquiring GPS satellite fix...", Toast.LENGTH_SHORT).show();
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
            if (binding.sideActionScroll != null) binding.sideActionScroll.setVisibility(View.GONE);
            if (binding.sideActionPanel != null) binding.sideActionPanel.setVisibility(View.GONE);
            if (binding.expandIcon != null) binding.expandIcon.setVisibility(View.GONE);
            if (binding.floatingDeviceScroll != null) binding.floatingDeviceScroll.setVisibility(View.GONE);
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
            if (binding.sideActionScroll != null) binding.sideActionScroll.setVisibility(View.VISIBLE);
            if (binding.sideActionPanel != null) binding.sideActionPanel.setVisibility(View.VISIBLE);
            if (binding.expandIcon != null) binding.expandIcon.setVisibility(View.VISIBLE);
            if (binding.floatingDeviceScroll != null && binding.floatingDevicePanel != null && binding.floatingDevicePanel.getChildCount() > 0) {
                binding.floatingDeviceScroll.setVisibility(View.VISIBLE);
            }
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
            if (cameraController != null) {
                cameraController.focusOnPeer(matchLive);
            } else {
                LatLng targetPos = new LatLng(matchLive.getLatitude(), matchLive.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetPos, 17.5f));
            }
            updateAddressMarquee(matchLive);
            Toast.makeText(this, "📍 Locked to live position of " + matchLive.getUserName(), Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Fallback: Query DeviceRepository for latest historical location
        java.util.Map<String, Object> latestHist = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this).getLatestHistoricalLocation(targetUid);
        if (latestHist != null) {
            hasZoomedToTarget = true;
            double lat = latestHist.get("lat") instanceof Number ? ((Number) latestHist.get("lat")).doubleValue() : 0.0;
            double lng = latestHist.get("lng") instanceof Number ? ((Number) latestHist.get("lng")).doubleValue() : 0.0;
            long ts = latestHist.get("timestamp") instanceof Number ? ((Number) latestHist.get("timestamp")).longValue() : System.currentTimeMillis();
            String timeStr = new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(ts));
            
            LatLng histPos = new LatLng(lat, lng);
            if (cameraController != null) {
                cameraController.focusOnCoordinate(lat, lng, 16.5f, "Historical: " + targetUid);
            } else {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(histPos, 16.5f));
            }
            
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

    /**
     * [Variables] locations | [Constants] THROTTLE_WINDOW_MS = 60ms | [Dependencies] Handler, MultiMarkerEngine | [Integration Points] Map Pan, Zoom, and GPS Telemetry
     * Smooths rapid GPS and camera idle events into a high-performance 60fps refresh window to eliminate map lag.
     */
    private void scheduleThrottledMarkerRender(List<UserLocation> locations) {
        if (locations == null) return;
        if (pendingRenderRunnable != null) {
            mapRenderHandler.removeCallbacks(pendingRenderRunnable);
        }
        pendingRenderRunnable = () -> {
            if (googleMap != null && !isFinishing() && !isDestroyed()) {
                com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.processViewModelLocations(MapActivity.this, googleMap, locations);
            }
        };
        mapRenderHandler.postDelayed(pendingRenderRunnable, 60);
    }

    // [Variables] locations, localDb, ul, dId, onlineAvatar, localAvatar, isOnlineCustom, isLocalCustom, targetAvatar, targetName, cleanDId | [Constants] None | [Dependencies] DatabaseHelper, AvatarLoader, DeviceIdentityHelper | [Integration Points] MapActivity, LocationRepository
    /**
     * 🔄 UNIFIED AVATAR RECONCILIATION:
     * Compares the avatars of all online devices with the local database.
     * Uses localAvatarMemoryCache to eliminate repeated main-thread disk reads.
     * Asynchronously offloads SQLite writes to avatarDbExecutor so the UI thread is never blocked.
     */
    private void reconcileDeviceAvatarsWithLocalDb(List<UserLocation> locations) {
        if (locations == null || locations.isEmpty()) return;
        com.jdroidx.jfindme.data.repository.DeviceRepository deviceRepo = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this);

        for (UserLocation ul : locations) {
            if (ul == null) continue;
            String dId = ul.getResolvedUserId();
            if (dId.isEmpty()) continue;

            String onlineAvatar = ul.getAvatar_id();
            String localAvatar = localAvatarMemoryCache.get(dId);
            if (localAvatar == null) {
                localAvatar = deviceRepo.getDeviceCentricAvatar(dId);
                if (localAvatar != null) {
                    localAvatarMemoryCache.put(dId, localAvatar);
                } else {
                    localAvatar = "";
                }
            }

            boolean isOnlineCustom = onlineAvatar != null && !onlineAvatar.isEmpty() && !onlineAvatar.equals("butterfly") && !onlineAvatar.equals("1");
            boolean isLocalCustom = !localAvatar.isEmpty() && !localAvatar.equals("butterfly") && !localAvatar.equals("1");

            if (isOnlineCustom && !onlineAvatar.equals(localAvatar)) {
                // Online device has an updated custom avatar -> Update memory cache instantly & persist to DB asynchronously
                localAvatarMemoryCache.put(dId, onlineAvatar);
                final String targetAvatar = onlineAvatar;
                final String targetName = ul.getUserName();
                if (avatarDbExecutor != null && !avatarDbExecutor.isShutdown()) {
                    avatarDbExecutor.execute(() -> {
                        try {
                            deviceRepo.updateDeviceIdentityAndAvatar(dId, targetName, targetAvatar);
                            com.jdroidx.jfindme.utils.AvatarLoader.evictDevice(dId);
                            String cleanDId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(dId);
                            com.jdroidx.jfindme.utils.AvatarLoader.evictDevice(cleanDId);
                            if (!cleanDId.startsWith("HW_")) com.jdroidx.jfindme.utils.AvatarLoader.evictDevice("HW_" + cleanDId);
                        } catch (Exception ignored) {}
                    });
                }
            } else if (!isOnlineCustom && isLocalCustom) {
                // Local database has the unified avatar -> Unify online model object in-memory (No cache blowup)
                ul.setAvatar_id(localAvatar);
            }
        }
    }

    private final java.util.Set<String> distantDeviceHysteresisSet = new java.util.HashSet<>();
    private final java.util.Map<String, Float> lastDistantDeviceDistances = new java.util.HashMap<>();
    private final java.util.Set<String> expandedFloatingDeviceIds = new java.util.HashSet<>();

    // [Variables] timestamp, now, deltaMs | [Constants] NONE | [Dependencies] System.currentTimeMillis | [Integration Points] Floating Device HUD
    private String formatLastUpdateDuration(long timestamp) {
        if (timestamp <= 0) return "Recent";
        long now = System.currentTimeMillis();
        long deltaMs = now - timestamp;
        if (deltaMs < 0 || deltaMs < 15_000L) {
            return "Live";
        } else if (deltaMs < 60_000L) {
            return (deltaMs / 1000L) + "s ago";
        } else if (deltaMs < 3_600_000L) {
            return (deltaMs / 60_000L) + "m ago";
        } else if (deltaMs < 86_400_000L) {
            return (deltaMs / 3_600_000L) + "h ago";
        } else {
            return (deltaMs / 86_400_000L) + "d ago";
        }
    }

    // [Variables] locations, myLoc, refLoc, thresholdMeters, panel, scroll, distantDevices, distRes, card, roundContainer, roundBg, ivAvatar, textLayout, tvName, tvDist, tvMeta, tvClose, isExpanded, durationText, headerPill, tvHeader, hystLower, hystUpper, wasDistant, isDistant | [Constants] DEFAULT_DISTANT_METERS = 50, HYSTERESIS_BUFFER_RATIO = 0.10f | [Dependencies] Location, AvatarLoader, DatabaseHelper, RegistryManager, CameraUpdateFactory | [Integration Points] Live Map HUD, SettingsActivity
    /**
     * Renders floating side cards for peer devices ONLY if they are strictly > threshold distance (default 50m) away.
     * Includes interactive HUD settings trigger to adjust threshold (less or greater).
     * Incorporates a ±10% distance hysteresis band to prevent visual jitter / fluttering caused by indoor GPS drift.
     */
    private void updateFloatingDevicePanel(List<UserLocation> locations, Location myLoc) {
        if (binding == null || binding.floatingDevicePanel == null || binding.floatingDeviceScroll == null) return;

        String selfUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        String selfAgent = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String selfHwUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(this);
        String selfRawId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getRawAndroidId(this);
        String selfCanonical = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(selfAgent);
        com.jdroidx.jfindme.data.repository.DeviceRepository deviceRepo = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this);

        // 🛡️ Robust SSOT Location Fallback: Ensures floating HUD is never blocked by null myLoc
        Location refLoc = myLoc;
        if (refLoc == null && lastKnownLocation != null) {
            refLoc = lastKnownLocation;
        }
        if (refLoc == null && viewModel != null && viewModel.getMyLocation() != null) {
            refLoc = viewModel.getMyLocation().getValue();
        }
        if (refLoc == null) {
            double savedLat = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLatitude(this);
            double savedLng = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLongitude(this);
            if (Math.abs(savedLat) > 0.001 || Math.abs(savedLng) > 0.001) {
                refLoc = new Location("fallback");
                refLoc.setLatitude(savedLat);
                refLoc.setLongitude(savedLng);
            }
        }
        if (refLoc == null && locations != null) {
            for (UserLocation u : locations) {
                if (u == null) continue;
                String uid = u.getResolvedUserId();
                String canonUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(uid);
                if (uid.equals(selfUid) || uid.equals(selfAgent) || uid.equals("SELF_USER") || "You".equalsIgnoreCase(uid)
                    || canonUid.equalsIgnoreCase(selfCanonical) || uid.equalsIgnoreCase(selfHwUid) || uid.equalsIgnoreCase(selfRawId)) {
                    if (Math.abs(u.getLatitude()) > 0.001 && Math.abs(u.getLongitude()) > 0.001) {
                        refLoc = new Location("self_loc");
                        refLoc.setLatitude(u.getLatitude());
                        refLoc.setLongitude(u.getLongitude());
                        break;
                    }
                }
            }
        }

        if (locations == null || locations.isEmpty() || refLoc == null || (Math.abs(refLoc.getLatitude()) < 0.001 && Math.abs(refLoc.getLongitude()) < 0.001)) {
            binding.floatingDeviceScroll.setVisibility(View.GONE);
            binding.floatingDevicePanel.removeAllViews();
            return;
        }

        // [Variables] thresholdMeters, hystBuffer, exitThreshold, enterThreshold, distantDevices, distRes, newDistantSet, seenCanonicalIds, ul, dId, canonicalDevId, isSelfDevice, distMeters, wasDistant, isDistant, maxCards, count, finalRefLoc, displayName, ssot, finalDisplayName, distText, finalDistText, durationText, isExpanded, roleStr, roleCol, avId, roundContainer, roundSize, rLp, roundBg, ivAvatar, avSize, avLp, card, cardLp, cardBg, textLayout, tvName, tvMeta, metaText, tvClose, target | [Constants] DEFAULT_DISTANT_METERS = 50 | [Dependencies] FrameLayout, ImageView, TextView, LinearLayout, AvatarLoader, GoogleMap, LatLng | [Integration Points] MapActivity Floating Avatar HUD
        int thresholdMeters = com.jdroidx.jfindme.utils.RegistryManager.getDistantDeviceThresholdMeters(this);
        float hystBuffer = Math.max(5.0f, thresholdMeters * 0.10f); // 10% dynamic hysteresis buffer
        float exitThreshold = (float) thresholdMeters - hystBuffer;
        float enterThreshold = (float) thresholdMeters;

        List<Pair<UserLocation, Float>> distantDevices = new ArrayList<>();
        Set<String> newDistantSet = new HashSet<>();
        Set<String> seenCanonicalIds = new HashSet<>();

        for (UserLocation ul : locations) {
            if (ul == null) continue;
            String dId = ul.getResolvedUserId();
            if (dId == null || dId.trim().isEmpty()) continue;
            String canonicalDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(dId);

            // Exclude Host self device (Host is centered on map)
            boolean isSelfDevice = (dId.equals(selfUid) || dId.equals(selfAgent) || dId.equals("SELF_USER") || "You".equalsIgnoreCase(dId)
                || canonicalDevId.equalsIgnoreCase(selfCanonical) || dId.equalsIgnoreCase(selfHwUid) || dId.equalsIgnoreCase(selfRawId));
            if (isSelfDevice) continue;

            // Strict deduplication so same physical node (e.g. 4379A761 and HW_4379A761) is only processed once
            if (seenCanonicalIds.contains(canonicalDevId)) {
                continue;
            }

            // Reject Null Island (lat or lng == 0.0 or near 0)
            if (Math.abs(ul.getLatitude()) < 0.001 || Math.abs(ul.getLongitude()) < 0.001) continue;

            float distMeters = com.jdroidx.jfindme.utils.SpatialMathUtil.computeDistanceBetweenFloat(
                refLoc.getLatitude(), refLoc.getLongitude(), ul.getLatitude(), ul.getLongitude());
            lastDistantDeviceDistances.put(dId, distMeters);
            lastDistantDeviceDistances.put(canonicalDevId, distMeters);

            // [Variables] wasDistant, isDistant | [Constants] None | [Dependencies] distantDeviceHysteresisSet | [Integration Points] MapActivity Floating Avatar HUD
            // 🛰️ Distant Device Hysteresis Filter (Activates when >= enterThreshold, stays active until < exitThreshold)
            boolean wasDistant = distantDeviceHysteresisSet.contains(canonicalDevId) || distantDeviceHysteresisSet.contains(dId);
            boolean isDistant = wasDistant ? (distMeters >= exitThreshold) : (distMeters >= enterThreshold);
            if (isDistant) {
                seenCanonicalIds.add(canonicalDevId);
                newDistantSet.add(canonicalDevId);
                newDistantSet.add(dId);
                distantDevices.add(new Pair<>(ul, distMeters));
            }
        }

        distantDeviceHysteresisSet.clear();
        distantDeviceHysteresisSet.addAll(newDistantSet);
        expandedFloatingDeviceIds.retainAll(newDistantSet);

        // If no devices are within range (missing if > thresholdMeters), cleanly hide the floating panel
        if (distantDevices.isEmpty()) {
            binding.floatingDeviceScroll.setVisibility(View.GONE);
            binding.floatingDevicePanel.removeAllViews();
            return;
        }

        // Sort by distance ascending (nearest device first)
        Collections.sort(distantDevices, (a, b) -> Float.compare(a.second, b.second));

        binding.floatingDeviceScroll.setVisibility(View.VISIBLE);
        binding.floatingDevicePanel.removeAllViews();

        int maxCards = 8;
        int count = 0;
        final Location finalRefLoc = refLoc;

        for (Pair<UserLocation, Float> entry : distantDevices) {
            if (count++ >= maxCards) break;
            UserLocation ul = entry.first;
            float distMeters = entry.second;
            String dId = ul.getResolvedUserId();

            // [Variables] displayName, finalDisplayName, dId, ul | [Constants] None | [Dependencies] DevicePresentationResolver | [Integration Points] Tactical Floating Device Card
            String displayName = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveDisplayName(this, dId, ul.getUserName());

            String distText = (distMeters >= 1000f) 
                ? String.format(java.util.Locale.US, "%.1f km", distMeters / 1000f) 
                : Math.round(distMeters) + " m";

            final String finalDisplayName = displayName;
            final String finalDistText = distText;
            final String durationText = formatLastUpdateDuration(ul.getLongTimestamp());
            final boolean isExpanded = expandedFloatingDeviceIds.contains(dId);

            String roleStr = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveRoleString(ul);
            int roleCol = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveRoleColor(ul);
            String avId = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveAvatarId(this, dId, ul.getAvatar_id());

            if (!isExpanded) {
                // =========================================================================
                // 🔘 STATE 1 (DEFAULT): 56dp Circular Avatar (Exact same size as 56dp Map Avatar)
                // Zero text, zero icons at top; On click -> converts to rectangle metadata card
                // =========================================================================
                FrameLayout roundContainer = new FrameLayout(this);
                int roundSize = scale(56); // 🚀 Exact same size as 56dp Live Map Avatar
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(roundSize, roundSize);
                rLp.bottomMargin = scale(8);
                rLp.gravity = Gravity.CENTER_HORIZONTAL;
                roundContainer.setLayoutParams(rLp);

                android.graphics.drawable.GradientDrawable roundBg = new android.graphics.drawable.GradientDrawable();
                roundBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                roundBg.setColor(0xEE141923); // Deep translucent slate
                roundBg.setStroke(scale(2), roleCol); // Role-accented circular ring
                roundContainer.setBackground(roundBg);
                roundContainer.setElevation(scale(4));

                ImageView ivAvatar = new ImageView(this);
                int avSize = scale(46);
                FrameLayout.LayoutParams avLp = new FrameLayout.LayoutParams(avSize, avSize);
                avLp.gravity = Gravity.CENTER;
                ivAvatar.setLayoutParams(avLp);
                ivAvatar.setClickable(false);
                ivAvatar.setFocusable(false);
                com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, ivAvatar, dId, avId);
                roundContainer.addView(ivAvatar);

                // Step 1: On select circle avatar -> Focus camera directly on peer via TacticalCameraController SSOT
                roundContainer.setOnClickListener(v -> {
                    if (cameraController != null) {
                        cameraController.focusOnPeer(ul);
                    } else if (googleMap != null) {
                        LatLng target = new LatLng(ul.getLatitude(), ul.getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 17.5f));
                    }
                    updateAddressMarquee(ul);
                    Toast.makeText(MapActivity.this, "📍 Focused on " + finalDisplayName + " (" + finalDistText + ")", Toast.LENGTH_SHORT).show();
                    if (expandedFloatingDeviceIds.contains(dId)) {
                        expandedFloatingDeviceIds.remove(dId);
                    } else {
                        expandedFloatingDeviceIds.clear();
                        expandedFloatingDeviceIds.add(dId);
                    }
                    updateFloatingDevicePanel(locations, finalRefLoc);
                });

                // Long-click: opens tactical action sheet
                roundContainer.setOnLongClickListener(v -> {
                    showTacticalTargetActionSheet(ul);
                    return true;
                });

                binding.floatingDevicePanel.addView(roundContainer);

            } else {
                // =========================================================================
                // 📋 STATE 2 (EXPANDED): Rounded Rectangle Card Showing Metadata
                // On click rectangle card -> Camera focuses on actual location of device!
                // =========================================================================
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setPadding(scale(10), scale(8), scale(12), scale(8));

                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardLp.bottomMargin = scale(8);
                card.setLayoutParams(cardLp);

                android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
                cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                cardBg.setColor(0xEE141923); // Deep translucent slate
                cardBg.setCornerRadius(scale(14));
                cardBg.setStroke(scale(2), roleCol); // Role-accented border
                card.setBackground(cardBg);
                card.setElevation(scale(6));

                // Left: Avatar in card (tapping avatar collapses back to circle)
                ImageView ivAvatar = new ImageView(this);
                int avSize = scale(44);
                LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(avSize, avSize);
                avLp.rightMargin = scale(10);
                ivAvatar.setLayoutParams(avLp);
                com.jdroidx.jfindme.utils.AvatarLoader.bindAvatar(this, ivAvatar, dId, avId);
                ivAvatar.setOnClickListener(v -> {
                    expandedFloatingDeviceIds.remove(dId);
                    updateFloatingDevicePanel(locations, finalRefLoc);
                });
                card.addView(ivAvatar);

                // Center: Text Info Column (Name + Metadata)
                LinearLayout textLayout = new LinearLayout(this);
                textLayout.setOrientation(LinearLayout.VERTICAL);

                TextView tvName = new TextView(this);
                tvName.setText(displayName + " • " + roleStr);
                tvName.setTextSize(11.5f);
                tvName.setTextColor(0xFFFFFFFF);
                tvName.setTypeface(null, Typeface.BOLD);
                tvName.setMaxLines(1);
                textLayout.addView(tvName);

                TextView tvMeta = new TextView(this);
                String batteryStr = (ul.getBatteryLevel() >= 0) ? " • 🔋" + ul.getBatteryLevel() + "%" : "";
                String sosStr = ul.isSosActive() ? " • 🚨 SOS" : "";
                String metaText = distText + " • " + durationText + batteryStr + sosStr;
                tvMeta.setText(metaText);
                tvMeta.setTextSize(9.5f);
                tvMeta.setTextColor(0xFF00E5FF); // Neon cyan
                tvMeta.setTypeface(null, Typeface.BOLD);
                textLayout.addView(tvMeta);

                // 🛡️ Touch Pass-Through: Ensure clicking text triggers card camera focus
                textLayout.setClickable(false);
                tvName.setClickable(false);
                tvMeta.setClickable(false);
                textLayout.setOnClickListener(v -> card.performClick());

                card.addView(textLayout);

                // Right: Close / Collapse button (✕)
                TextView tvClose = new TextView(this);
                tvClose.setText(" ✕");
                tvClose.setTextSize(11f);
                tvClose.setTextColor(0x88FFFFFF);
                tvClose.setPadding(scale(6), scale(2), scale(2), scale(2));
                tvClose.setOnClickListener(v -> {
                    expandedFloatingDeviceIds.remove(dId);
                    updateFloatingDevicePanel(locations, finalRefLoc);
                });
                card.addView(tvClose);

                // Step 2: On click of rectangle card -> Camera focuses on actual location of device via TacticalCameraController SSOT
                card.setOnClickListener(v -> {
                    if (cameraController != null) {
                        cameraController.focusOnPeer(ul);
                    } else if (googleMap != null) {
                        LatLng target = new LatLng(ul.getLatitude(), ul.getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 17.5f));
                    }
                    updateAddressMarquee(ul);
                    Toast.makeText(MapActivity.this, "📍 Focused on " + finalDisplayName + " (" + finalDistText + ")", Toast.LENGTH_SHORT).show();
                });

                // Long-click: opens tactical action sheet
                card.setOnLongClickListener(v -> {
                    showTacticalTargetActionSheet(ul);
                    return true;
                });

                binding.floatingDevicePanel.addView(card);
            }
        }
    }

    // [Variables] rawCurrent, current, values, customLabel, options, selected, chosen | [Constants] DEFAULT_DISTANT_METERS = 50 | [Dependencies] RegistryManager, AlertDialog, UiArchitect | [Integration Points] MapActivity, SettingsActivity
    /**
     * ⚙️ Allows setting distant device panel threshold (less or greater than 50m) directly from Map HUD.
     */
    private void showFloatingDistanceThresholdDialog() {
        int rawCurrent = com.jdroidx.jfindme.utils.RegistryManager.getDistantDeviceThresholdMeters(this);
        final int current = (rawCurrent <= 0) ? 50 : rawCurrent;
        int[] values = {25, 50, 100, 200, 500, 1000};
        String customLabel = "✏️ Custom Distance (" + current + "m)...";
        String[] options = {
            "25m (Immediate Vicinity)",
            "50m (Standard Proximity — Default)",
            "100m (Close Proximity — Tight Clustering)",
            "200m (Medium Range)",
            "500m (Wide Area — Campus / Fleet)",
            "1000m (Extended — City-Scale)",
            customLabel
        };
        int selected = 1; // Default to 50m
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                selected = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("📡 Distant Device HUD Threshold")
            .setMessage("Devices farther than this distance appear in the left Floating Avatar Panel. Set distance threshold:")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                dialog.dismiss();
                if (which == options.length - 1) {
                    UiArchitect.showInputPromptDialog(this, "📡 Custom Distant Threshold", "Enter meters (10 - 50000):", String.valueOf(current), input -> {
                        if (input != null && !input.trim().isEmpty()) {
                            try {
                                int val = Integer.parseInt(input.trim());
                                val = Math.max(10, Math.min(50000, val));
                                com.jdroidx.jfindme.utils.RegistryManager.setDistantDeviceThresholdMeters(this, val);
                                distantDeviceHysteresisSet.clear();
                                expandedFloatingDeviceIds.clear();
                                UiArchitect.showSlideNotificationBanner(this, "Threshold Updated", "Floating avatar activates at >" + val + "m", false, 2500, null, null);
                                if (viewModel != null && viewModel.getUserLocations().getValue() != null) {
                                    updateFloatingDevicePanel(viewModel.getUserLocations().getValue(), lastKnownLocation);
                                }
                            } catch (Exception ignored) {}
                        }
                    });
                } else {
                    int chosen = values[which];
                    com.jdroidx.jfindme.utils.RegistryManager.setDistantDeviceThresholdMeters(this, chosen);
                    distantDeviceHysteresisSet.clear();
                    expandedFloatingDeviceIds.clear();
                    UiArchitect.showSlideNotificationBanner(this, "Threshold Updated", "Floating avatar activates at >" + chosen + "m", false, 2500, null, null);
                    if (viewModel != null && viewModel.getUserLocations().getValue() != null) {
                        updateFloatingDevicePanel(viewModel.getUserLocations().getValue(), lastKnownLocation);
                    }
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    @Override
    protected void onMasterDataUpdated(String category, String changeType, String entityId, String senderId) {
        super.onMasterDataUpdated(category, changeType, entityId, senderId);
        runOnUiThread(() -> {
            if ("SAFE_ZONE".equalsIgnoreCase(category)) {
                if (viewModel != null) {
                    renderSafeZonesOnMap(viewModel.getSafeZones().getValue());
                }
            } else if ("ROSTER".equalsIgnoreCase(category) || "PROFILE".equalsIgnoreCase(category)) {
                if (entityId != null && !entityId.isEmpty()) {
                    com.jdroidx.jfindme.utils.AvatarLoader.evictDevice(entityId);
                }
                if (senderId != null && !senderId.isEmpty()) {
                    com.jdroidx.jfindme.utils.AvatarLoader.evictDevice(senderId);
                }
                if (viewModel != null) {
                    viewModel.refreshLocations();
                    if (googleMap != null && viewModel.getUserLocations().getValue() != null) {
                        reconcileDeviceAvatarsWithLocalDb(viewModel.getUserLocations().getValue());
                        com.jdroidx.jfindme.engine.map.MultiMarkerEngine.INSTANCE.processViewModelLocations(
                            this, googleMap, viewModel.getUserLocations().getValue()
                        );
                    }
                }
            } else if ("BUTTERFLY_POINT".equalsIgnoreCase(category) || "BUTTERFLY".equalsIgnoreCase(category)) {
                handleButterflyPointMasterDataUpdate(changeType, entityId);
            }
        });
    }

    private void handleButterflyPointMasterDataUpdate(String changeType, String entityId) {
        new Thread(() -> {
            java.util.List<com.jdroidx.jfindme.data.model.ButterflyPoint> activePts = 
                com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(this).getActiveButterflyPoints();
            runOnUiThread(() -> {
                updateButterflyBadge();
                ButterflyPointDialog.refreshIfOpen();
                if (activePts != null && !activePts.isEmpty()) {
                    com.jdroidx.jfindme.data.model.ButterflyPoint active = null;
                    if (entityId != null && !entityId.isEmpty()) {
                        for (com.jdroidx.jfindme.data.model.ButterflyPoint p : activePts) {
                            if (entityId.equals(p.getPointId())) {
                                active = p;
                                break;
                            }
                        }
                    }
                    if (active == null) {
                        active = activePts.get(0);
                    }
                    if (googleMap != null) {
                        focusOnButterflyPoint(active, false);
                    }
                    if ("CREATE".equalsIgnoreCase(changeType) || "INVITATION".equalsIgnoreCase(changeType) || "INSERT".equalsIgnoreCase(changeType)) {
                        UiArchitect.showSlideNotificationBanner(
                            this,
                            "🦋 BUTTERFLY RENDEZVOUS",
                            "Invitation for: " + active.getPointName(),
                            false,
                            4000,
                            "VIEW",
                            () -> ButterflyPointDialog.show(this)
                        );
                    }
                } else {
                    com.jdroidx.jfindme.engine.map.ButterflyPointEngine.clearButterflyOverlay();
                }
            });
        }).start();
    }
}
