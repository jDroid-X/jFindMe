package com.jdroidx.jfindme.ui.settings;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.auth.AuthActivity;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.ThemeManager;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> SettingsScreen -> SettingsActivity
 * Level 5: Implementation of Theme Management, Multi-Device Pairing Hub, and Account Controls.
 */
public class SettingsActivity extends BaseActivityJava {

    private final String TAG = "SettingsActivity";

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor()); // Grey/Dark contrast background

        // 🚀 Premium Linear Gradient        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header); // 🚀 Native QA Architecture Injection // Top hero banner seamlessly bleeds into status bar
        
        TextView title = new TextView(this);
        title.setText("CONTROL HUB");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Device Telemetry & Engine Architecture");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        header.addView(subtitle);
        
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(40, 20, 40, 80);

        // --- LAYER 1: THE CONFIGURATOR GATEWAY ---
        content.addView(createSectionHeader("MASTER UI OVERRIDE"));
        LinearLayout themeCard = new LinearLayout(this);
        themeCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 15, 0, 15); // Explicit Tile Spacing!
        
        // 🚀 PHASE 1: The Configurator Gateway Portal
        themeCard.addView(createIconButton("GLOBAL UI & ICON ARCHITECT", android.R.drawable.ic_menu_gallery, v -> {
            try {
                startActivity(new Intent(this, Class.forName("com.jdroidx.jfindme.ui.settings.UiConfigActivity")));
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Compiling Master UI Engine. Awaiting Phase 2...", android.widget.Toast.LENGTH_SHORT).show();
            }
        }), btnLp);

        content.addView(themeCard, getCardParams());

        // --- LAYER 2: ADVANCED MAP ENGINE CARD ---
        content.addView(createSectionHeader("ADVANCED APP TELEMETRY"));
        LinearLayout engineCard = new LinearLayout(this);
        engineCard.setOrientation(LinearLayout.VERTICAL);

        // Reuses the identical btnLp parameter object declared during Layer 1

        engineCard.addView(createIconButton("GHOST MODE (OFFLINE TRACKING)", android.R.drawable.ic_menu_view, v -> {
            boolean currentState = prefs.getBoolean("pref_ghost_mode_persistent", true);
            new android.app.AlertDialog.Builder(this)
                .setTitle("Configure Ghost Mode")
                .setMessage("PROS:\n- Sleeping devices remain on the map as 50% transparent ghosts.\n\nCONS:\n- Visually clutters the map if users are left offline for several days.\n\nCurrent State: " + (currentState ? "ENABLED" : "DISABLED"))
                .setPositiveButton("Enable", (dialog, which) -> prefs.edit().putBoolean("pref_ghost_mode_persistent", true).apply())
                .setNegativeButton("Disable", (dialog, which) -> prefs.edit().putBoolean("pref_ghost_mode_persistent", false).apply())
                .show();
        }), btnLp);

        engineCard.addView(createIconButton("SMART AVATAR CLUSTERING", android.R.drawable.ic_menu_sort_by_size, v -> {
            boolean currentCluster = prefs.getBoolean("pref_smart_avatar_clustering", true);
            new android.app.AlertDialog.Builder(this)
                .setTitle("Configure Smart Clustering")
                .setMessage("PROS:\n- Stacks users within 10-meters into a clean '+X' Red Badge.\n\nCONS:\n- You cannot physically see overlapping avatars until fully zoomed in.\n\nCurrent State: " + (currentCluster ? "ENABLED" : "DISABLED"))
                .setPositiveButton("Enable", (dialog, which) -> prefs.edit().putBoolean("pref_smart_avatar_clustering", true).apply())
                .setNegativeButton("Disable", (dialog, which) -> prefs.edit().putBoolean("pref_smart_avatar_clustering", false).apply())
                .show();
        }), btnLp);
        
        // QA Battery Configuration Override
        engineCard.addView(createIconButton("TRACKING RESOLUTION (INTERVAL)", android.R.drawable.ic_menu_recent_history, v -> {
            long currentInterval = prefs.getLong("pref_tracking_interval", 60000L);
            String currentStr = (currentInterval / 1000) + " Seconds";
            
            String[] options = {"Aggressive (10s) - High Battery Drain", "Recommend (60s) - Best Balance", "Passive (5m) - Maximum Battery Use"};
            long[] values = {10000L, 60000L, 300000L};
            
            new android.app.AlertDialog.Builder(this)
                .setTitle("GPS Ping Interval")
                .setMessage("Higher thresholds dramatically reduce daily battery drain. We strictly recommend the default 60s configuration.\n\nCurrent Target: " + currentStr)
                .setSingleChoiceItems(options, -1, (dialog, which) -> {
                    long selected = values[which];
                    prefs.edit().putLong("pref_tracking_interval", selected).apply();
                    android.widget.Toast.makeText(this, "Interval updated to " + (selected/1000) + "s. Service will adapt natively.", android.widget.Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
        }), btnLp);
        
        engineCard.addView(createIconButton("VECTOR MAP RENDERER", android.R.drawable.ic_menu_mapmode, v -> {
            android.widget.Toast.makeText(this, "Hardware Accelerated Vector Engine Active", android.widget.Toast.LENGTH_SHORT).show();
        }), btnLp);
        
        content.addView(engineCard, getCardParams());

        // --- LAYER 3: SECURITY & PROFILES CARD ---
        content.addView(createSectionHeader("SECURITY & ACCESS CONTROLS"));
        LinearLayout secCard = new LinearLayout(this);
        secCard.setOrientation(LinearLayout.VERTICAL);
        
        secCard.addView(createIconButton("PUPIL PROFILE ARCHITECTURE", android.R.drawable.ic_menu_edit, v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("NOTIFICATION ROUTING", android.R.drawable.ic_popup_reminder, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.NotifActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("IDENTITY SECURITY PROTOCOL", android.R.drawable.ic_lock_idle_lock, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.PrivacyActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("SUPPORT DOCUMENTS", android.R.drawable.ic_menu_help, v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("jFindMe Documentation")
                .setMessage("Enterprise Engine V8 running strictly native Java View hierarchies.")
                .setPositiveButton("OK", null).show();
        }), btnLp);
        secCard.addView(createIconButton("SYSTEM LOGS & AUDIT TRAIL", android.R.drawable.ic_menu_info_details, v -> {
            startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.management.SystemLogActivity.class));
        }), btnLp);
        
        content.addView(secCard, getCardParams());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return root;
    }

    private LinearLayout.LayoutParams getCardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 30);
        return lp;
    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Seamless UI integration
        header.setTextSize(11);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(10, 50, 0, 20);
        return header;
    }


    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.d(TAG, "Settings Hub Synchronized.");
    }
}
