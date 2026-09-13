package com.jdroidx.jbutterfly.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jbutterfly.R;
import com.jdroidx.jbutterfly.data.local.DatabaseHelper;
import com.jdroidx.jbutterfly.data.model.LiveDeviceModel;
import com.jdroidx.jbutterfly.data.model.SafeZoneModel;
import com.jdroidx.jbutterfly.data.remote.FirebaseComm;
import com.jdroidx.jbutterfly.service.TrackingService;
import com.jdroidx.jbutterfly.ui.auth.AuthActivity;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.ui.group.FamilyFriendsActivity;
import com.jdroidx.jbutterfly.ui.group.NetworkGatewayActivity;
import com.jdroidx.jbutterfly.ui.management.*;
import com.jdroidx.jbutterfly.ui.settings.*;
import com.jdroidx.jbutterfly.utils.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final int PERMISSION_REQ_CODE = 4001;

    private static class MarkerInfo {
        String agentId;
        String name;
        String avatarName;
        float heading;
        String ragHexColor;
        boolean isSelf;

        MarkerInfo(String agentId, String name, String avatarName, float heading, String ragHexColor, boolean isSelf) {
            this.agentId = agentId;
            this.name = name;
            this.avatarName = avatarName;
            this.heading = heading;
            this.ragHexColor = ragHexColor;
            this.isSelf = isSelf;
        }
    }

    private DrawerLayout drawerLayout;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedClient;

    private final Map<String, Marker> deviceMarkers = new HashMap<>();
    private final Map<String, Circle> safeZoneCircles = new HashMap<>();
    private final Map<String, LatLng> rawPositions = new HashMap<>();
    private final Map<String, MarkerInfo> markerInfoCache = new HashMap<>();
    private com.google.firebase.database.ValueEventListener incomingConnListener;

    private boolean isFollowMeActive = true;
    private String myAgentId = "";
    private LatLng myLastLatLng = null;
    private int currentMapTypeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        checkAndRequestPermissions();
        buildLayout();

        // Start background telemetry sentinel
        TrackingService.startTracking(this);
    }

    private void buildLayout() {
        drawerLayout = new DrawerLayout(this);

        // ─── MAIN CONTENT VIEW ────────────────────────────────────────
        RelativeLayout content = new RelativeLayout(this);
        content.setLayoutParams(new DrawerLayout.LayoutParams(
            DrawerLayout.LayoutParams.MATCH_PARENT,
            DrawerLayout.LayoutParams.MATCH_PARENT
        ));

        // Map View Container
        FrameLayout mapContainer = new FrameLayout(this);
        mapContainer.setId(View.generateViewId());
        content.addView(mapContainer, new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        ));

        // Top Tactical App Bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        int pad = UiArchitect.dpToPx(this, 12);
        topBar.setPadding(pad, pad, pad, pad);
        topBar.setBackgroundColor(Color.argb(220, 8, 12, 20));

        RelativeLayout.LayoutParams topLp = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        topLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        // Menu Toggle Hamburger Button
        ImageView btnMenu = new ImageView(this);
        btnMenu.setImageResource(android.R.drawable.ic_menu_sort_by_size);
        btnMenu.setColorFilter(Color.WHITE);
        btnMenu.setPadding(pad, pad, pad, pad);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));
        topBar.addView(btnMenu);

        // Title and Hardware Node Badge
        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleColLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleColLp.leftMargin = UiArchitect.dpToPx(this, 8);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("jButterFly Live Radar");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(UiArchitect.spToPx(this, 16f));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvTitle);

        TextView tvNode = new TextView(this);
        tvNode.setText("Agent ID: " + myAgentId + " • 🟢 LIVE MESH");
        tvNode.setTextColor(Color.parseColor("#22C55E"));
        tvNode.setTextSize(UiArchitect.spToPx(this, 11f));
        titleCol.addView(tvNode);

        topBar.addView(titleCol, titleColLp);

        // Map Mode Switcher
        Button btnMapMode = new Button(this);
        btnMapMode.setText("RADAR");
        btnMapMode.setTextSize(UiArchitect.spToPx(this, 10f));
        btnMapMode.setTextColor(Color.WHITE);
        btnMapMode.setBackgroundColor(Color.parseColor("#1E293B"));
        btnMapMode.setPadding(UiArchitect.dpToPx(this, 8), UiArchitect.dpToPx(this, 6), UiArchitect.dpToPx(this, 8), UiArchitect.dpToPx(this, 6));
        LinearLayout.LayoutParams mmLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mmLp.rightMargin = UiArchitect.dpToPx(this, 6);
        btnMapMode.setOnClickListener(v -> cycleMapMode(btnMapMode));
        topBar.addView(btnMapMode, mmLp);

        // Follow-Me Toggle Button
        Button btnFollow = new Button(this);
        btnFollow.setText(isFollowMeActive ? "FOLLOW ON" : "FOLLOW OFF");
        btnFollow.setTextSize(UiArchitect.spToPx(this, 10f));
        btnFollow.setTextColor(Color.WHITE);
        btnFollow.setBackgroundColor(isFollowMeActive ? Color.parseColor("#2563EB") : Color.parseColor("#475569"));
        btnFollow.setPadding(UiArchitect.dpToPx(this, 10), UiArchitect.dpToPx(this, 6), UiArchitect.dpToPx(this, 10), UiArchitect.dpToPx(this, 6));
        btnFollow.setOnClickListener(v -> {
            isFollowMeActive = !isFollowMeActive;
            btnFollow.setText(isFollowMeActive ? "FOLLOW ON" : "FOLLOW OFF");
            btnFollow.setBackgroundColor(isFollowMeActive ? Color.parseColor("#2563EB") : Color.parseColor("#475569"));
            if (isFollowMeActive && myLastLatLng != null && googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLastLatLng, 16.0f));
            }
        });
        topBar.addView(btnFollow);

        content.addView(topBar, topLp);

        // Recenter Floating Button
        ImageView btnRecenter = new ImageView(this);
        btnRecenter.setImageResource(android.R.drawable.ic_menu_mylocation);
        btnRecenter.setColorFilter(Color.WHITE);
        int fabSize = UiArchitect.dpToPx(this, 46);
        RelativeLayout.LayoutParams fabLp = new RelativeLayout.LayoutParams(fabSize, fabSize);
        fabLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        fabLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        fabLp.rightMargin = UiArchitect.dpToPx(this, 16);
        fabLp.bottomMargin = UiArchitect.dpToPx(this, 72);
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setColor(Color.parseColor("#0F172A"));
        fabBg.setCornerRadius(fabSize / 2f);
        fabBg.setStroke(UiArchitect.dpToPx(this, 2), Color.parseColor("#3B82F6"));
        btnRecenter.setBackground(fabBg);
        int fabPad = UiArchitect.dpToPx(this, 11);
        btnRecenter.setPadding(fabPad, fabPad, fabPad, fabPad);
        btnRecenter.setOnClickListener(v -> {
            if (myLastLatLng != null && googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLastLatLng, 16.5f));
                UiArchitect.showSlideNotificationBanner(this, "Radar Recenter", "Camera focused on your GPS node.", false, 2000, null, null);
            } else {
                fetchCurrentLocation();
            }
        });
        content.addView(btnRecenter, fabLp);

        // Bottom Action Radar Bar (Pair Device, SafeZone, Recenter)
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(pad, pad, pad, pad);
        bottomBar.setBackgroundColor(Color.argb(220, 8, 12, 20));

        RelativeLayout.LayoutParams bottomLp = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        bottomLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        Button btnPair = UiArchitect.createPrimaryButton(this, "+ PAIR DEVICE", v -> {
            startActivity(new Intent(this, NetworkGatewayActivity.class));
        });
        LinearLayout.LayoutParams pairLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        pairLp.rightMargin = UiArchitect.dpToPx(this, 8);
        bottomBar.addView(btnPair, pairLp);

        Button btnFamily = UiArchitect.createSecondaryButton(this, "ROSTER", v -> {
            startActivity(new Intent(this, FamilyFriendsActivity.class));
        });
        LinearLayout.LayoutParams famLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        bottomBar.addView(btnFamily, famLp);

        content.addView(bottomBar, bottomLp);
        drawerLayout.addView(content);

        // ─── 15-NODE ENTERPRISE NAVIGATION DRAWER ───────────────────────
        View navDrawer = buildEnterpriseNavigationDrawer();
        drawerLayout.addView(navDrawer);

        setContentView(drawerLayout);

        // Embed Map Fragment
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(mapContainer.getId(), mapFragment).commit();
        mapFragment.getMapAsync(this);
    }

    private View buildEnterpriseNavigationDrawer() {
        DrawerLayout.LayoutParams drawerLp = new DrawerLayout.LayoutParams(
            UiArchitect.dpToPx(this, 300),
            DrawerLayout.LayoutParams.MATCH_PARENT
        );
        drawerLp.gravity = Gravity.START;

        LinearLayout drawer = new LinearLayout(this);
        drawer.setLayoutParams(drawerLp);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(ThemeManager.getSurfaceColor(this));

        // Drawer Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.parseColor("#080C14"));
        int pad = UiArchitect.dpToPx(this, 20);
        header.setPadding(pad, pad + UiArchitect.dpToPx(this, 16), pad, pad);

        ImageView headerLogo = new ImageView(this);
        headerLogo.setImageResource(R.drawable.butterfly);
        int logoSize = UiArchitect.dpToPx(this, 48);
        header.addView(headerLogo, new LinearLayout.LayoutParams(logoSize, logoSize));

        TextView tvHeaderTitle = new TextView(this);
        tvHeaderTitle.setText("jButterFly Light");
        tvHeaderTitle.setTextColor(Color.WHITE);
        tvHeaderTitle.setTextSize(UiArchitect.spToPx(this, 16f));
        tvHeaderTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeaderTitle.setPadding(0, UiArchitect.dpToPx(this, 8), 0, 0);
        header.addView(tvHeaderTitle);

        TextView tvHeaderSub = new TextView(this);
        tvHeaderSub.setText("Silicon Node: " + myAgentId);
        tvHeaderSub.setTextColor(Color.parseColor("#94A3B8"));
        tvHeaderSub.setTextSize(UiArchitect.spToPx(this, 11f));
        header.addView(tvHeaderSub);

        drawer.addView(header);

        // Drawer Scrollable Items
        ScrollView scroll = new ScrollView(this);
        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.VERTICAL);
        items.setPadding(0, UiArchitect.dpToPx(this, 8), 0, UiArchitect.dpToPx(this, 24));

        // 1. Dashboard
        items.addView(createDrawerItem("Dashboard", "Mission Overview & Telemetry", android.R.drawable.ic_dialog_info, v -> {
            drawerLayout.closeDrawers();
            Toast.makeText(this, "Radar Telemetry Dashboard Active", Toast.LENGTH_SHORT).show();
        }));

        // 2. Children Devices
        items.addView(createDrawerItem("Children Devices", "Kids & Monitored Dependents", android.R.drawable.ic_menu_myplaces, v -> {
            drawerLayout.closeDrawers();
            Intent i = new Intent(this, FamilyFriendsActivity.class);
            i.putExtra("CATEGORY", "FAMILY");
            startActivity(i);
        }));

        // 3. Live Map
        items.addView(createDrawerItem("Live Map", "Real-Time Spatial Grid", android.R.drawable.ic_menu_mapmode, v -> {
            drawerLayout.closeDrawers();
            if (myLastLatLng != null && googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLastLatLng, 16f));
            }
        }));

        // 4. Location History
        items.addView(createDrawerItem("Location History", "Review Traveled Paths", android.R.drawable.ic_menu_recent_history, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, HistoryActivity.class));
        }));

        // 5. MeetMyFriend Groups
        items.addView(createDrawerItem("MeetMyFriend Groups", "Social Circles & Peer Network", android.R.drawable.ic_menu_share, v -> {
            drawerLayout.closeDrawers();
            Intent i = new Intent(this, FamilyFriendsActivity.class);
            i.putExtra("CATEGORY", "FRIENDS");
            startActivity(i);
        }));

        // 6. Safe Zones
        items.addView(createDrawerItem("Safe Zones", "Geofence Boundaries & Perimeters", android.R.drawable.ic_lock_idle_lock, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, GeofenceActivity.class));
        }));

        // 7. Guardians / Family
        items.addView(createDrawerItem("Guardians / Family", "Primary Family Circle", android.R.drawable.ic_menu_agenda, v -> {
            drawerLayout.closeDrawers();
            Intent i = new Intent(this, FamilyFriendsActivity.class);
            i.putExtra("CATEGORY", "FAMILY");
            startActivity(i);
        }));

        // 8. Notifications
        items.addView(createDrawerItem("Notifications", "System Alerts & Incident Ledger", android.R.drawable.ic_popup_reminder, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, NotifActivity.class));
        }));

        // 9. Device Health
        items.addView(createDrawerItem("Device Health", "Battery & Sensor Telemetry", android.R.drawable.ic_menu_compass, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, HealthActivity.class));
        }));

        // 10. Profile
        items.addView(createDrawerItem("Profile", "Identity Dossier & Avatars", android.R.drawable.ic_menu_crop, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, ProfileActivity.class));
        }));

        // 11. Settings
        items.addView(createDrawerItem("Settings", "Dynamic Tuning & Preferences", android.R.drawable.ic_menu_preferences, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, SettingsActivity.class));
        }));

        // UI/UX Controls Studio
        items.addView(createDrawerItem("UI/UX Controls Studio", "Interactive Feature Showcase & Studio", android.R.drawable.ic_menu_manage, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, UiConfigActivity.class));
        }));

        // 12. Privacy Controls
        items.addView(createDrawerItem("Privacy Controls", "Permissions & Security Locks", android.R.drawable.ic_secure, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, PrivacyActivity.class));
        }));

        // 13. Help & Support
        items.addView(createDrawerItem("Help & Support", "Knowledge Base & FAQs", android.R.drawable.ic_menu_help, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, HelpSupportActivity.class));
        }));

        // 14. About App
        items.addView(createDrawerItem("About App", "Version & Security Integrity", android.R.drawable.ic_menu_info_details, v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, AboutActivity.class));
        }));

        // 15. Logout
        items.addView(createDrawerItem("Logout", "Secure Session Disconnect", android.R.drawable.ic_lock_power_off, v -> {
            drawerLayout.closeDrawers();
            UiArchitect.showConfirmationDialog(this,
                "Confirm Logout",
                "Are you sure you want to log out? Real-time background safety will pause.",
                "LOGOUT", "CANCEL",
                () -> {
                    FirebaseAuth.getInstance().signOut();
                    RegistryManager.putBoolean(this, RegistryManager.KEY_IS_LOGGED_IN, false);
                    TrackingService.stopTracking(this);
                    startActivity(new Intent(this, AuthActivity.class));
                    finishAffinity();
                }, null);
        }));

        scroll.addView(items);
        drawer.addView(scroll);
        return drawer;
    }

    private View createDrawerItem(String title, String subtitle, int iconRes, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = UiArchitect.dpToPx(this, 16);
        int padV = UiArchitect.dpToPx(this, 12);
        row.setPadding(padH, padV, padH, padV);
        row.setClickable(true);
        row.setFocusable(true);

        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(ThemeManager.getBrandColor(this));
        int iconSize = UiArchitect.dpToPx(this, 22);
        row.addView(iv, new LinearLayout.LayoutParams(iconSize, iconSize));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textLp.leftMargin = UiArchitect.dpToPx(this, 14);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvTitle.setTextSize(UiArchitect.spToPx(this, 13f));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvSub.setTextSize(UiArchitect.spToPx(this, 10f));
        textCol.addView(tvSub);

        row.addView(textCol, textLp);
        row.setOnClickListener(onClick);
        return row;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        googleMap.setOnMarkerClickListener(marker -> {
            String tag = (String) marker.getTag();
            if (tag != null) {
                showDeviceDossier(tag);
                return true;
            }
            return false;
        });

        applyMapStyle();
        loadSafeZones();
        listenToConnectedDevices();
        fetchCurrentLocation();
        handleTargetIntent();

        // Realtime 2-way incoming pairing sync: auto-adds nodes when paired from other devices
        incomingConnListener = FirebaseComm.listenForIncomingConnections(myAgentId, dev -> {
            runOnUiThread(() -> {
                DatabaseHelper db = DatabaseHelper.getInstance(this);
                if (db.getDeviceByAgentId(dev.agentId) == null) {
                    db.insertOrUpdateDevice(dev);
                    UiArchitect.showSlideNotificationBanner(this, "Device Linked", dev.deviceName + " connected to your radar", false, 3000, null, null);
                }
                listenToConnectedDevices();
            });
        });
    }

    private void applyMapStyle() {
        if (googleMap == null) return;
        try {
            boolean dark = ThemeManager.isDarkMode(this);
            int styleRes = dark ? R.raw.map_style_night : R.raw.map_style_dark;
            MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(this, styleRes);
            googleMap.setMapStyle(style);
        } catch (Exception ignored) {}
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation() {
        if (!hasLocationPermission()) return;
        fusedClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                updateSelfLocation(loc);
            }
        });
    }

    private void updateSelfLocation(Location loc) {
        myLastLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        updateMarkerOnMap(
            myAgentId,
            "Me (" + myAgentId + ")",
            "avatar_custom_1",
            myLastLatLng,
            loc.getBearing(),
            "#22C55E",
            true
        );

        if (isFollowMeActive && googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLastLatLng, 16.0f));
        }
    }

    private void listenToConnectedDevices() {
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<LiveDeviceModel> devices = db.getAllPairedDevices();

        for (LiveDeviceModel dev : devices) {
            if (dev.agentId.equals(myAgentId)) continue;

            // Render existing location from local SQLite SSOT if available
            if (dev.latitude != 0.0 || dev.longitude != 0.0) {
                LatLng pos = new LatLng(dev.latitude, dev.longitude);
                updateMarkerOnMap(
                    dev.agentId,
                    dev.deviceName,
                    dev.avatarName,
                    pos,
                    dev.heading,
                    dev.getRagColorHex(),
                    false
                );
            }

            // Realtime Firebase location subscription
            FirebaseComm.listenToDeviceLocation(dev.agentId, userLoc -> {
                runOnUiThread(() -> {
                    if (userLoc.latitude != 0.0 || userLoc.longitude != 0.0) {
                        // Persist to local SQLite SSOT
                        db.updateDeviceLocation(
                            userLoc.agentId,
                            userLoc.latitude,
                            userLoc.longitude,
                            userLoc.speed,
                            userLoc.heading,
                            userLoc.batteryPct
                        );

                        // Fetch updated device model for accurate RAG calculation
                        LiveDeviceModel updatedDev = db.getDeviceByAgentId(userLoc.agentId);
                        String ragColor = (updatedDev != null) ? updatedDev.getRagColorHex() : "#22C55E";
                        String devName = (updatedDev != null && !updatedDev.deviceName.isEmpty()) ? updatedDev.deviceName : dev.deviceName;
                        String avatar = (updatedDev != null && !updatedDev.avatarName.isEmpty()) ? updatedDev.avatarName : dev.avatarName;

                        LatLng newPos = new LatLng(userLoc.latitude, userLoc.longitude);
                        updateMarkerOnMap(
                            userLoc.agentId,
                            devName,
                            avatar,
                            newPos,
                            userLoc.heading,
                            ragColor,
                            false
                        );
                    }
                });
            });
        }
    }

    private void loadSafeZones() {
        if (googleMap == null) return;
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<SafeZoneModel> zones = db.getAllSafeZones();

        for (SafeZoneModel zone : zones) {
            CircleOptions opts = new CircleOptions()
                .center(new LatLng(zone.latitude, zone.longitude))
                .radius(zone.radiusMeters)
                .strokeColor(Color.argb(180, 59, 130, 246))
                .strokeWidth(4f)
                .fillColor(Color.argb(40, 59, 130, 246));
            Circle circle = googleMap.addCircle(opts);
            safeZoneCircles.put(zone.zoneId, circle);
        }
    }

    // ─── TACTICAL MAP MARKER & DE-COLLISION SPIDERIFIER ENGINE ────────

    private void updateMarkerOnMap(
        String agentId,
        String name,
        String avatarName,
        LatLng pos,
        float heading,
        String ragHexColor,
        boolean isSelf
    ) {
        if (googleMap == null || (pos.latitude == 0.0 && pos.longitude == 0.0)) return;

        rawPositions.put(agentId, pos);
        markerInfoCache.put(agentId, new MarkerInfo(agentId, name, avatarName, heading, ragHexColor, isSelf));

        refreshClusteredMarkersAround(pos);
    }

    private void refreshClusteredMarkersAround(LatLng centerPos) {
        if (googleMap == null || centerPos == null) return;

        // Group all active devices whose coordinates are within 25 meters of centerPos
        List<String> clusterIds = new ArrayList<>();
        for (Map.Entry<String, LatLng> entry : rawPositions.entrySet()) {
            LatLng pos = entry.getValue();
            if (pos == null || (pos.latitude == 0.0 && pos.longitude == 0.0)) continue;
            double dist = GeofenceEngine.haversineDistance(centerPos.latitude, centerPos.longitude, pos.latitude, pos.longitude);
            if (dist <= 25.0) {
                clusterIds.add(entry.getKey());
            }
        }

        Collections.sort(clusterIds); // Deterministic ordering

        int total = clusterIds.size();
        for (int i = 0; i < total; i++) {
            String aid = clusterIds.get(i);
            LatLng raw = rawPositions.get(aid);
            MarkerInfo info = markerInfoCache.get(aid);
            if (raw == null || info == null) continue;

            LatLng displayPos = raw;
            if (total > 1) {
                // Spiderifier de-collision: fan out overlapping avatars in a radial ring
                double angle = (2.0 * Math.PI * i) / total;
                double offsetMeters = 20.0;
                double dLat = (offsetMeters * Math.cos(angle)) / 111111.0;
                double dLng = (offsetMeters * Math.sin(angle)) / (111111.0 * Math.cos(Math.toRadians(raw.latitude)));
                displayPos = new LatLng(raw.latitude + dLat, raw.longitude + dLng);
            }

            renderMarkerAtPosition(aid, info, displayPos);
        }
    }

    private void renderMarkerAtPosition(String agentId, MarkerInfo info, LatLng displayPos) {
        if (googleMap == null || displayPos == null || (displayPos.latitude == 0.0 && displayPos.longitude == 0.0)) return;

        Bitmap icon = createTacticalMarkerBitmap(info.name, info.avatarName, info.heading, info.ragHexColor, info.isSelf);
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(icon);

        Marker existing = deviceMarkers.get(agentId);
        if (existing == null) {
            Marker m = googleMap.addMarker(new MarkerOptions()
                .position(displayPos)
                .icon(descriptor)
                .anchor(0.5f, 0.65f)
                .title(info.name)
            );
            if (m != null) {
                m.setTag(agentId);
                deviceMarkers.put(agentId, m);
            }
        } else {
            existing.setPosition(displayPos);
            existing.setIcon(descriptor);
            existing.setTitle(info.name);
            existing.setTag(agentId);
        }
    }

    private Bitmap createTacticalMarkerBitmap(
        String name,
        String avatarName,
        float heading,
        String ragHexColor,
        boolean isSelf
    ) {
        // High-resolution bitmap canvas (160dp x 130dp)
        int width = UiArchitect.dpToPx(this, 160);
        int height = UiArchitect.dpToPx(this, 130);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float centerX = width / 2f;
        float centerY = height / 2f + UiArchitect.dpToPx(this, 8);
        float radius = UiArchitect.dpToPx(this, 22);

        // 1. Heading Direction Chevron (Uber / Ola directional indicator)
        if (heading > 0) {
            canvas.save();
            canvas.rotate(heading, centerX, centerY);
            paint.setColor(Color.parseColor(ragHexColor));
            paint.setStyle(Paint.Style.FILL);
            Path chevron = new Path();
            float topY = centerY - radius - UiArchitect.dpToPx(this, 6);
            chevron.moveTo(centerX, topY - UiArchitect.dpToPx(this, 12));
            chevron.lineTo(centerX - UiArchitect.dpToPx(this, 9), topY);
            chevron.lineTo(centerX + UiArchitect.dpToPx(this, 9), topY);
            chevron.close();
            canvas.drawPath(chevron, paint);
            canvas.restore();
        }

        // 2. Avatar Outer Glow / Shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawCircle(centerX, centerY + UiArchitect.dpToPx(this, 2), radius + UiArchitect.dpToPx(this, 2), paint);

        // 3. Avatar Center Ring Border with RAG Status Color
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(UiArchitect.dpToPx(this, 3));
        paint.setColor(Color.parseColor(ragHexColor));
        canvas.drawCircle(centerX, centerY, radius, paint);

        // 4. Avatar bitmap inside circle
        int avatarRes = AvatarLoader.getAvatarResId(this, avatarName);
        Bitmap rawAvatar = BitmapFactory.decodeResource(getResources(), avatarRes);
        if (rawAvatar != null) {
            Bitmap circular = AvatarLoader.getCircularBitmap(rawAvatar);
            int innerRadius = Math.round(radius - UiArchitect.dpToPx(this, 2));
            Rect dest = new Rect(
                Math.round(centerX - innerRadius),
                Math.round(centerY - innerRadius),
                Math.round(centerX + innerRadius),
                Math.round(centerY + innerRadius)
            );
            canvas.drawBitmap(circular, null, dest, null);
        }

        // 5. Ultra-Readable Top Name Callout Pill
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(UiArchitect.dpToPx(this, 12)); // Full 12dp text size (ultra-sharp)
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setColor(Color.WHITE);

        String label = (name == null || name.trim().isEmpty()) ? "Node" : name.trim();
        if (label.length() > 16) {
            label = label.substring(0, 14) + "..";
        }
        float textWidth = textPaint.measureText(label);

        float dotRadius = UiArchitect.dpToPx(this, 4);
        float pillPadH = UiArchitect.dpToPx(this, 8);
        float pillWidth = textWidth + dotRadius * 2 + pillPadH * 2 + UiArchitect.dpToPx(this, 6);
        float pillHeight = UiArchitect.dpToPx(this, 22);

        float pillLeft = centerX - pillWidth / 2f;
        float pillTop = centerY - radius - pillHeight - UiArchitect.dpToPx(this, 8);
        float pillRight = pillLeft + pillWidth;
        float pillBottom = pillTop + pillHeight;

        RectF pillRect = new RectF(pillLeft, pillTop, pillRight, pillBottom);

        // Pill Shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(120, 0, 0, 0));
        RectF shadowRect = new RectF(pillRect.left, pillRect.top + UiArchitect.dpToPx(this, 2), pillRect.right, pillRect.bottom + UiArchitect.dpToPx(this, 2));
        canvas.drawRoundRect(shadowRect, UiArchitect.dpToPx(this, 11), UiArchitect.dpToPx(this, 11), paint);

        // Pill Solid Background (High-contrast dark slate)
        paint.setColor(Color.parseColor("#090E1A"));
        canvas.drawRoundRect(pillRect, UiArchitect.dpToPx(this, 11), UiArchitect.dpToPx(this, 11), paint);

        // Pill Border Stroke with RAG color
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(UiArchitect.dpToPx(this, 2));
        paint.setColor(Color.parseColor(ragHexColor));
        canvas.drawRoundRect(pillRect, UiArchitect.dpToPx(this, 11), UiArchitect.dpToPx(this, 11), paint);

        // RAG Status Indicator Dot
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor(ragHexColor));
        float dotX = pillRect.left + pillPadH + dotRadius;
        float dotY = pillRect.centerY();
        canvas.drawCircle(dotX, dotY, dotRadius, paint);

        // Device Name Text (Vertically centered)
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textX = dotX + dotRadius + UiArchitect.dpToPx(this, 5);
        float textY = pillRect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(label, textX, textY, textPaint);

        return bitmap;
    }

    // ─── PERMISSION INFRASTRUCTURE ────────────────────────────────────

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkAndRequestPermissions() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                PERMISSION_REQ_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
            TrackingService.startTracking(this);
        }
    }

    private void cycleMapMode(Button btn) {
        if (googleMap == null) return;
        currentMapTypeIndex = (currentMapTypeIndex + 1) % 3;
        if (currentMapTypeIndex == 0) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            applyMapStyle();
            btn.setText("RADAR");
            UiArchitect.showSlideNotificationBanner(this, "Radar Style", "Tactical Night Radar active.", false, 2000, null, null);
        } else if (currentMapTypeIndex == 1) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            btn.setText("SATELLITE");
            UiArchitect.showSlideNotificationBanner(this, "Radar Style", "Photorealistic Satellite active.", false, 2000, null, null);
        } else {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            googleMap.setMapStyle(null);
            btn.setText("ROAD");
            UiArchitect.showSlideNotificationBanner(this, "Radar Style", "Standard Vector Road active.", false, 2000, null, null);
        }
    }

    private void handleTargetIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        double targetLat = intent.getDoubleExtra("TARGET_LAT", 0.0);
        double targetLng = intent.getDoubleExtra("TARGET_LNG", 0.0);
        String targetId = intent.getStringExtra("TARGET_AGENT_ID");

        if (targetLat != 0.0 && targetLng != 0.0 && googleMap != null) {
            LatLng target = new LatLng(targetLat, targetLng);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 17f));
            UiArchitect.showSlideNotificationBanner(this, "Target Located", "Focused on historical trail coordinate.", false, 2500, null, null);
        } else if (targetId != null && !targetId.isEmpty() && googleMap != null) {
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            LiveDeviceModel dev = db.getDeviceByAgentId(targetId);
            if (dev != null && (dev.latitude != 0.0 || dev.longitude != 0.0)) {
                LatLng target = new LatLng(dev.latitude, dev.longitude);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 17f));
                showDeviceDossier(dev.agentId);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTargetIntent();
    }

    private void showDeviceDossier(String agentId) {
        boolean isSelf = agentId.equals(myAgentId);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getSurfaceColor(this));
        int pad = UiArchitect.dpToPx(this, 20);
        root.setPadding(pad, pad, pad, pad);

        if (isSelf) {
            TextView tvTitle = new TextView(this);
            tvTitle.setText("🛰️ Host Device (Me)");
            tvTitle.setTextColor(ThemeManager.getPrimaryTextColor(this));
            tvTitle.setTextSize(UiArchitect.spToPx(this, 16f));
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            root.addView(tvTitle);

            TextView tvSub = new TextView(this);
            tvSub.setText("Agent ID: " + myAgentId + "\nSilicon Key: " + DeviceIdentityHelper.getRawHardwareKey(this));
            tvSub.setTextColor(ThemeManager.getSecondaryTextColor(this));
            tvSub.setTextSize(UiArchitect.spToPx(this, 12f));
            tvSub.setPadding(0, UiArchitect.dpToPx(this, 6), 0, UiArchitect.dpToPx(this, 12));
            root.addView(tvSub);

            Button btnRecenterSelf = UiArchitect.createPrimaryButton(this, "CENTER CAMERA ON ME", v -> {
                if (myLastLatLng != null && googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLastLatLng, 17f));
                }
            });
            root.addView(btnRecenterSelf);
        } else {
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            LiveDeviceModel dev = db.getDeviceByAgentId(agentId);
            if (dev == null) return;

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            ImageView ivAv = new ImageView(this);
            int avSize = UiArchitect.dpToPx(this, 48);
            AvatarLoader.loadAvatar(this, ivAv, dev.avatarName);
            topRow.addView(ivAv, new LinearLayout.LayoutParams(avSize, avSize));

            LinearLayout nameCol = new LinearLayout(this);
            nameCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            nLp.leftMargin = UiArchitect.dpToPx(this, 12);

            TextView tvName = new TextView(this);
            tvName.setText(dev.deviceName);
            tvName.setTextColor(ThemeManager.getPrimaryTextColor(this));
            tvName.setTextSize(UiArchitect.spToPx(this, 15f));
            tvName.setTypeface(Typeface.DEFAULT_BOLD);
            nameCol.addView(tvName);

            TextView tvDetails = new TextView(this);
            tvDetails.setText("Agent ID: " + dev.agentId + " • " + dev.category + " (" + dev.role + ")");
            tvDetails.setTextColor(ThemeManager.getSecondaryTextColor(this));
            tvDetails.setTextSize(UiArchitect.spToPx(this, 11f));
            nameCol.addView(tvDetails);
            topRow.addView(nameCol, nLp);

            TextView pill = UiArchitect.createStatusPill(this, dev.getRagStatus().name(), dev.getRagColorHex());
            topRow.addView(pill);
            root.addView(topRow);

            double distKm = 0.0;
            if (myLastLatLng != null && dev.latitude != 0.0 && dev.longitude != 0.0) {
                double distMeters = GeofenceEngine.haversineDistance(myLastLatLng.latitude, myLastLatLng.longitude, dev.latitude, dev.longitude);
                distKm = distMeters / 1000.0;
            }

            TextView tvTel = new TextView(this);
            tvTel.setText(
                "• Distance: " + (distKm > 0 ? String.format(Locale.US, "%.2f km away", distKm) : "Locating...") + "\n" +
                "• Battery: " + dev.batteryPct + "%\n" +
                "• Speed: " + String.format(Locale.US, "%.1f m/s", dev.speed) + " • Heading: " + (int) dev.heading + "°"
            );
            tvTel.setTextColor(ThemeManager.getPrimaryTextColor(this));
            tvTel.setTextSize(UiArchitect.spToPx(this, 12f));
            tvTel.setLineSpacing(0f, 1.25f);
            tvTel.setPadding(0, UiArchitect.dpToPx(this, 12), 0, UiArchitect.dpToPx(this, 14));
            root.addView(tvTel);

            LinearLayout btnRow1 = new LinearLayout(this);
            btnRow1.setOrientation(LinearLayout.HORIZONTAL);

            Button btnBuzz = UiArchitect.createDestructiveButton(this, "🚨 BUZZ SIREN", v -> {
                UiArchitect.showSlideNotificationBanner(this, "Buzzing Node", "Sending Acoustic Siren Buzz to " + dev.deviceName, false, 2500, null, null);
                FirebaseComm.sendBuzzCommand(dev.agentId, myAgentId,
                    () -> UiArchitect.showSlideNotificationBanner(this, "Siren Active", "Emergency Buzz triggered on " + dev.deviceName, true, 3500, null, null),
                    err -> UiArchitect.showSlideNotificationBanner(this, "Buzz Failed", err, true, 3000, null, null)
                );
            });
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            bLp.rightMargin = UiArchitect.dpToPx(this, 6);
            btnRow1.addView(btnBuzz, bLp);

            Button btnTrails = UiArchitect.createSecondaryButton(this, "📜 TRAILS", v -> {
                Intent hist = new Intent(this, HistoryActivity.class);
                hist.putExtra("TARGET_AGENT_ID", dev.agentId);
                startActivity(hist);
            });
            btnRow1.addView(btnTrails, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            root.addView(btnRow1);

            Button btnFollowNode = UiArchitect.createPrimaryButton(this, "🛰️ FOCUS ON NODE", v -> {
                if (googleMap != null && (dev.latitude != 0.0 || dev.longitude != 0.0)) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(dev.latitude, dev.longitude), 17f));
                    UiArchitect.showSlideNotificationBanner(this, "Node Focused", "Camera locked to " + dev.deviceName, false, 2000, null, null);
                }
            });
            LinearLayout.LayoutParams fnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            fnLp.topMargin = UiArchitect.dpToPx(this, 8);
            root.addView(btnFollowNode, fnLp);
        }

        builder.setView(root);
        builder.setPositiveButton("CLOSE", null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyMapStyle();
        if (googleMap != null) {
            loadSafeZones();
            listenToConnectedDevices();
            handleTargetIntent();
        }
    }
}
