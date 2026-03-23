package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;

public class UiConfigActivity extends BaseActivityJava {

    private final String TAG = "UiConfigActivity";

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header); 
        
        TextView title = new TextView(this);
        title.setText("GLOBAL UI ARCHITECT");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24f);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("System Geometry & Vector Matrices");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12f);
        subtitle.setPadding(0, 10, 0, 0);
        header.addView(subtitle);
        
        mainRoot.addView(header);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        // Load Dynamic Padding Overrides!
        int paddingLevel = prefs.getInt("custom_ui_padding", 40);
        content.setPadding(paddingLevel, paddingLevel, paddingLevel, 100);

        float activeGeo = prefs.getFloat("custom_geometry_radius", 30f);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 40);

        // --- LAYER 0: AESTHETIC MONITOR (MASTER THEMES) ---
        content.addView(createSectionHeader("AESTHETIC MONITOR (MASTER THEMES)"));
        LinearLayout themeMonitorCard = new LinearLayout(this);
        themeMonitorCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(themeMonitorCard, getCardColor(), activeGeo);
        themeMonitorCard.setPadding(paddingLevel, paddingLevel, paddingLevel, paddingLevel);

        LinearLayout.LayoutParams elementLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elementLp.setMargins(0, 15, 0, 15);

        themeMonitorCard.addView(createIconButton("AESTHETIC MONITOR", android.R.drawable.ic_media_play, v -> {
            String[] themeNames = {
                "FamiSafe Violet", "Twilight Midnight", "Oceanic Teal", 
                "Sunrise Orange", "Bubblegum Pop", "Silver Chrome", 
                "Electric Azure", "Golden Hour"
            };
            int activeTheme = prefs.getInt("theme_id", 0);
            
            new android.app.AlertDialog.Builder(this)
                .setTitle("Select Global Aesthetic")
                .setSingleChoiceItems(themeNames, activeTheme, (dialog, which) -> {
                    prefs.edit().putInt("theme_id", which).apply();
                    android.widget.Toast.makeText(this, "Theme Engaged: " + themeNames[which], android.widget.Toast.LENGTH_SHORT).show();
                    recreate();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
        }), elementLp);

        content.addView(themeMonitorCard, cardLp);

        // --- LAYER 1: MULTI-SCALAR CORNER RADIUS ENGINE ---
        content.addView(createSectionHeader("STRUCTURAL GEOMETRY (CORNER RADII)"));
        LinearLayout radCard = new LinearLayout(this);
        radCard.setOrientation(LinearLayout.VERTICAL);
        
        applySolidCardGeometry(radCard, getCardColor(), activeGeo);
        radCard.setPadding(paddingLevel, paddingLevel, paddingLevel, paddingLevel);
        
        // Dynamic Sliders will go here
        radCard.addView(createGeometryOption("Sharp Enterprise Layout (10px)", 10f));
        radCard.addView(createGeometryOption("Modern Premium UI (30px)", 30f));
        radCard.addView(createGeometryOption("Maximum Organic Squircle (60px)", 60f));

        content.addView(radCard, cardLp);
        
        // --- LAYER 2: HEADER GEOMETRY (HEIGHT & DENSITY) ---
        content.addView(createSectionHeader("HEADER ARCHITECTURE (PANEL HEIGHT)"));
        LinearLayout headCard = new LinearLayout(this);
        headCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(headCard, getCardColor(), activeGeo);
        headCard.setPadding(paddingLevel, paddingLevel, paddingLevel, paddingLevel);
        
        headCard.addView(createHeaderOption("Massive Branding Display (120px Top)", 50, 120, 100));
        headCard.addView(createHeaderOption("Standard Enterprise Hub (80px Top)", 50, 80, 60));
        headCard.addView(createHeaderOption("Compact Information HUD (40px Top)", 30, 40, 30));
        headCard.addView(createHeaderOption("Ultra-Slim Navigation Ribbon (20px Top)", 20, 20, 10));

        content.addView(headCard, cardLp);
        
        // --- LAYER 3: GLOBAL PADDING DENSITY CONTROLLER ---
        content.addView(createSectionHeader("COMPONENT DENSITY CONTROLLER"));
        LinearLayout paddingCard = new LinearLayout(this);
        paddingCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(paddingCard, getCardColor(), activeGeo);
        paddingCard.setPadding(paddingLevel, paddingLevel, paddingLevel, paddingLevel);
        
        paddingCard.addView(createPaddingOption("Ultra Dense Information Array (15px padding)", 15));
        paddingCard.addView(createPaddingOption("Standard Native Architecture (40px padding)", 40));

        content.addView(paddingCard, cardLp);

        // --- LAYER 3: ICONOGRAPHIC DENSITY SCALE ---
        content.addView(createSectionHeader("VECTOR ICON DENSITY SCALING"));
        LinearLayout scaleCard = new LinearLayout(this);
        scaleCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(scaleCard, getCardColor(), activeGeo);
        scaleCard.setPadding(paddingLevel, paddingLevel, paddingLevel, paddingLevel);
        
        scaleCard.addView(createIconDensityOption("Massive Accessibility Targets (180px Bounds)", 180));
        scaleCard.addView(createIconDensityOption("Standard Enterprise Size (140px Bounds)", 140));
        scaleCard.addView(createIconDensityOption("Micro UI Information Matrix (100px Bounds)", 100));

        content.addView(scaleCard, cardLp);

        // --- LAYER 4: WIDGET TINT COLOR MATRIX ---
        content.addView(createSectionHeader("WIDGET & CHECKBOX BORDER TINT"));
        LinearLayout tintCard = new LinearLayout(this);
        tintCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(tintCard, getCardColor(), activeGeo);
        tintCard.setPadding(paddingLevel, paddingLevel, paddingLevel, paddingLevel);
        
        tintCard.addView(createTintOption("Adaptive Theme (Dynamic Sync)", "THEME"));
        tintCard.addView(createTintOption("Solid White (High Contrast Void)", "WHITE"));
        tintCard.addView(createTintOption("Solid Black (Deep Ink Canvas)", "BLACK"));
        tintCard.addView(createTintOption("Terminal Green (Success Status)", "GREEN"));
        tintCard.addView(createTintOption("Emergency Red (Alert Target)", "RED"));

        content.addView(tintCard, cardLp);

        // --- LAYER 5: VECTOR ICONOGRAPHY MATRIX Gateway ---
        content.addView(createSectionHeader("ICONOGRAPHY & VECTOR DEPLOYMENT"));
        LinearLayout iconCard = new LinearLayout(this);
        iconCard.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(iconCard, getCardColor(), activeGeo);
        iconCard.setPadding(paddingLevel, paddingLevel, paddingLevel, paddingLevel);
        
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 15, 0, 15);
        
        iconCard.addView(createIconButton("DASHBOARD TILE VECTORS", android.R.drawable.ic_dialog_map, v -> {
            String[] keys = {"icon_map_history", "icon_map_network", "icon_map_zones", "icon_map_radar", "icon_map_settings", "icon_map_logs"};
            String[] labels = {"History Module", "Network Module", "Zones Module", "Radar Module", "Privacy Settings", "System Logs"};
            int[] defaults = {android.R.drawable.ic_menu_recent_history, android.R.drawable.ic_menu_share, android.R.drawable.ic_popup_reminder, android.R.drawable.ic_dialog_map, android.R.drawable.ic_menu_manage, android.R.drawable.ic_menu_info_details};
            showVectorGallery("Dashboard Tiles", keys, labels, defaults);
        }), btnLp);
        
        iconCard.addView(createIconButton("PROFILE LAYER VECTORS", android.R.drawable.ic_menu_myplaces, v -> {
            String[] keys = {"icon_profile_name", "icon_profile_phone", "icon_profile_email", "icon_profile_dossier"};
            String[] labels = {"Name Input", "Mobile Number Input", "Email Target", "Dossier Link"};
            int[] defaults = {android.R.drawable.ic_menu_myplaces, android.R.drawable.ic_menu_call, android.R.drawable.ic_menu_send, android.R.drawable.ic_menu_info_details};
            showVectorGallery("Profile Layer", keys, labels, defaults);
        }), btnLp);
        
        iconCard.addView(createIconButton("PRIVACY LAYER VECTORS", android.R.drawable.ic_lock_idle_lock, v -> {
            String[] keys = {"icon_priv_gps", "icon_priv_scan", "icon_priv_telemetry", "icon_priv_2fa", "icon_priv_strict", "icon_priv_history"};
            String[] labels = {"Location Services", "Background Scanning", "Telemetry Report", "2FA Security", "Strict Bounds", "History Archive"};
            int[] defaults = {android.R.drawable.ic_menu_mylocation, android.R.drawable.ic_menu_rotate, android.R.drawable.ic_menu_info_details, android.R.drawable.ic_lock_idle_lock, android.R.drawable.ic_menu_compass, android.R.drawable.ic_menu_recent_history};
            showVectorGallery("Privacy Modules", keys, labels, defaults);
        }), btnLp);
        
        iconCard.addView(createIconButton("FACTORY OVERRIDE PURGE", android.R.drawable.ic_menu_revert, v -> {
            prefs.edit().clear().apply(); // Aggressive Purge for Developer Rest
            android.widget.Toast.makeText(this, "Global System Memory Wiped.", android.widget.Toast.LENGTH_SHORT).show();
            System.exit(0); // Hardware Restart Enforced
        }), btnLp);

        content.addView(iconCard, cardLp);

        scroll.addView(content);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainRoot;
    }

    private void showVectorGallery(String category, String[] keys, String[] labels, int[] defaults) {
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(40,40,40,40);
        
        int[] availableOps = {
            android.R.drawable.ic_menu_mapmode, android.R.drawable.ic_menu_myplaces, android.R.drawable.ic_menu_compass,
            android.R.drawable.ic_menu_directions, android.R.drawable.ic_dialog_map, android.R.drawable.ic_menu_view,
            android.R.drawable.ic_menu_share, android.R.drawable.ic_menu_manage, android.R.drawable.ic_menu_info_details,
            android.R.drawable.ic_lock_idle_lock, android.R.drawable.ic_popup_reminder, android.R.drawable.ic_menu_recent_history,
            android.R.drawable.ic_menu_call, android.R.drawable.ic_menu_send, android.R.drawable.ic_menu_camera,
            android.R.drawable.ic_menu_gallery, android.R.drawable.ic_menu_edit, android.R.drawable.ic_lock_power_off,
            android.R.drawable.ic_menu_save, android.R.drawable.ic_menu_mylocation, android.R.drawable.ic_menu_rotate,
            android.R.drawable.ic_menu_add, android.R.drawable.ic_menu_delete, android.R.drawable.ic_menu_search
        };

        for (int i=0; i<keys.length; i++) {
            final String k = keys[i];
            final int def = defaults[i];
            
            TextView tv = new TextView(this);
            tv.setText(labels[i]);
            tv.setTextColor(getGradientStart());
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(0, 40, 0, 20);
            list.addView(tv);
            
            android.widget.HorizontalScrollView hScroll = new android.widget.HorizontalScrollView(this);
            LinearLayout grid = new LinearLayout(this);
            grid.setOrientation(LinearLayout.HORIZONTAL);
            
            for (int resId : availableOps) {
                android.widget.ImageView img = new android.widget.ImageView(this);
                img.setImageResource(resId);
                int activeRes = prefs.getInt(k, def);
                img.setColorFilter(activeRes == resId ? getGradientStart() : Color.GRAY);
                
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setCornerRadius(15f);
                bg.setColor(Color.parseColor("#15000000"));
                if (activeRes == resId) bg.setStroke(4, getGradientStart());
                
                img.setBackground(bg);
                img.setPadding(20,20,20,20);
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(120, 120);
                lp.setMargins(0,0,20,0);
                grid.addView(img, lp);
                
                img.setOnClickListener(v -> {
                    prefs.edit().putInt(k, resId).apply();
                    android.widget.Toast.makeText(this, "Vector Configured. Press DONE.", android.widget.Toast.LENGTH_SHORT).show();
                    img.setColorFilter(getGradientStart());
                    bg.setStroke(4, getGradientStart());
                });
            }
            hScroll.addView(grid);
            list.addView(hScroll);
        }
        
        sv.addView(list);
        new android.app.AlertDialog.Builder(this)
            .setTitle(category + " Gallery")
            .setView(sv)
            .setPositiveButton("COMPILE MATRIX", (d,w) -> recreate())
            .show();
    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart());
        header.setTextSize(11);
        header.setPadding(10, 40, 0, 20);
        header.setTypeface(null, Typeface.BOLD);
        return header;
    }
    
    // Natively changes Global UI Padding Density via SharedPreferences injection!
    private View createPaddingOption(String label, int overrideValue) {
        TextView box = new TextView(this);
        box.setText(label);
        
        int activePadding = prefs.getInt("custom_ui_padding", 40);
        boolean active = activePadding == overrideValue;
        
        box.setTextColor(active ? getGradientStart() : getTextColor());
        if (active) box.setTypeface(null, Typeface.BOLD);
        
        box.setPadding(10, 30, 0, 30);
        box.setTextSize(14f);
        
        box.setClickable(true);
        box.setOnClickListener(v -> {
            // Apply Physical Core OS override
            prefs.edit().putInt("custom_ui_padding", overrideValue).apply();
            android.widget.Toast.makeText(this, "Re-aligning UI Geometry Engine...", android.widget.Toast.LENGTH_SHORT).show();
            
            // Mathematically Reboot the UI Array instantly so the Cards visually morph!
            recreate();
        });
        
        return box;
    }
    
    // Natively changes Global Icon Density Scaling
    private View createIconDensityOption(String label, int overrideValue) {
        TextView box = new TextView(this);
        box.setText(label);
        
        int activeDensity = prefs.getInt("custom_icon_density", 140);
        boolean active = activeDensity == overrideValue;
        
        box.setTextColor(active ? getGradientStart() : getTextColor());
        if (active) box.setTypeface(null, Typeface.BOLD);
        
        box.setPadding(10, 30, 0, 30);
        box.setTextSize(14f);
        
        box.setClickable(true);
        box.setOnClickListener(v -> {
            // Appends the Icon Scale into native memory
            prefs.edit().putInt("custom_icon_density", overrideValue).apply();
            android.widget.Toast.makeText(this, "Re-scaling SVG Vector Matrices globally...", android.widget.Toast.LENGTH_SHORT).show();
            recreate();
        });
        
        return box;
    }
    
    // Natively changes Global Widget Tint Color Matrix
    private View createTintOption(String label, String tintType) {
        TextView box = new TextView(this);
        box.setText(label);
        
        String activeTint = prefs.getString("custom_widget_tint", "THEME");
        boolean active = activeTint.equals(tintType);
        
        box.setTextColor(active ? getGradientStart() : getTextColor());
        if (active) box.setTypeface(null, Typeface.BOLD);
        
        box.setPadding(10, 30, 0, 30);
        box.setTextSize(14f);
        
        box.setClickable(true);
        box.setOnClickListener(v -> {
            prefs.edit().putString("custom_widget_tint", tintType).apply();
            android.widget.Toast.makeText(this, "Re-compiling Checkbox Tint Array globally...", android.widget.Toast.LENGTH_SHORT).show();
            recreate();
        });
        
        return box;
    }

    // Natively changes Global Header Geometry Matrix
    private View createHeaderOption(String label, int paddingSide, int paddingTop, int paddingBottom) {
        TextView box = new TextView(this);
        box.setText(label);
        
        int activeTop = prefs.getInt("custom_header_padding_top", 80);
        boolean active = activeTop == paddingTop;
        
        box.setTextColor(active ? getGradientStart() : getTextColor());
        if (active) box.setTypeface(null, Typeface.BOLD);
        
        box.setPadding(10, 30, 0, 30);
        box.setTextSize(14f);
        
        box.setClickable(true);
        box.setOnClickListener(v -> {
            prefs.edit().putInt("custom_header_padding_top", paddingTop).apply();
            prefs.edit().putInt("custom_header_padding_bottom", paddingBottom).apply();
            prefs.edit().putInt("custom_header_padding_side", paddingSide).apply();
            android.widget.Toast.makeText(this, "Re-aligning Header Geometries...", android.widget.Toast.LENGTH_SHORT).show();
            recreate();
        });
        
        return box;
    }

    // Natively changes Global Card Geometry via SharedPreferences injection!
    private View createGeometryOption(String label, float overrideValue) {
        TextView box = new TextView(this);
        box.setText(label);
        
        // Highlight active config
        float activeGeo = prefs.getFloat("custom_geometry_radius", 30f);
        boolean active = activeGeo == overrideValue;
        
        box.setTextColor(active ? getGradientStart() : getTextColor());
        if (active) box.setTypeface(null, Typeface.BOLD);
        
        box.setPadding(10, 30, 0, 30);
        box.setTextSize(14f);
        
        box.setClickable(true);
        box.setOnClickListener(v -> {
            // Apply Physical Core OS override
            prefs.edit().putFloat("custom_geometry_radius", overrideValue).apply();
            android.widget.Toast.makeText(this, "Re-compiling Native Radius Array...", android.widget.Toast.LENGTH_SHORT).show();
            
            // Mathematically Reboot the UI Array instantly so the Cards visually morph!
            recreate();
        });
        
        return box;
    }

    @Override
    protected void setupLogic() {
        com.jdroidx.jfindme.utils.SystemLogger.INSTANCE.i(TAG, "Architectural Configuration Matrix Online.");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 🚀 QA Fix: Force the rest of the application back-stack to completely redraw itself by changing the Master Hash!
        prefs.edit().putLong("ui_config_hash", System.currentTimeMillis()).apply();
    }
}
