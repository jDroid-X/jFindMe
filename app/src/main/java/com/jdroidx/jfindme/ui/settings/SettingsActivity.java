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

        // 🚀 Premium Linear Gradient Hero Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(50, 80, 50, 60);
        applyLinearGradient(header, 0f); // Top hero banner seamlessly bleeds into status bar
        
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

        // --- LAYER 1: THEME CUSTOMIZATION CARD ---
        content.addView(createSectionHeader("AESTHETIC CUSTOMIZATION"));
        LinearLayout themeCard = new LinearLayout(this);
        themeCard.setOrientation(LinearLayout.VERTICAL);
        themeCard.addView(createThemeSelector());
        content.addView(themeCard, getCardParams());

        // --- LAYER 2: ADVANCED MAP ENGINE CARD ---
        content.addView(createSectionHeader("ADVANCED APP TELEMETRY"));
        LinearLayout engineCard = new LinearLayout(this);
        engineCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 15, 0, 15); // Explicit Tile Spacing!

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
        secCard.addView(createIconButton("AES-256 PRIVACY PROTOCOLS", android.R.drawable.ic_lock_idle_lock, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.PrivacyActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("SUPPORT DOCUMENTS", android.R.drawable.ic_menu_help, v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("jFindMe Documentation")
                .setMessage("Enterprise Engine V8 running strictly native Java View hierarchies.")
                .setPositiveButton("OK", null).show();
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

    private View createThemeSelector() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        
        // 🚀 QA Fix: Critcal bug averted. Mapped the literal 9-Dimensional Multi-Shade themes precisely!
        String[] themeNames = {
            "FamiSafe Violet", "Twilight Midnight", "Oceanic Teal", 
            "Sunrise Orange", "Bubblegum Pop", "Silver Chrome", 
            "Electric Azure", "Golden Hour", "⚡ SYSTEM DEFAULT (AUTO)"
        };

        for (int i = 0; i < 9; i++) { // Loop natively extended to explicitly match indices [0] through [8]
            final int themeId = i;
            
            // Execute premium button mapping
            View btn = createIconButton(themeNames[i], android.R.drawable.ic_menu_gallery, v -> {
                currentThemeId = themeId;
                prefs.edit().putInt("theme_id", currentThemeId).apply();
                
                // 🚀 Execute Soft Loading Overlay to prevent UI freezing
                android.widget.ProgressBar pb = new android.widget.ProgressBar(this);
                pb.setPadding(0, 40, 0, 40);
                
                android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("Synchronizing Theme Matrix")
                    .setMessage("Injecting '" + themeNames[themeId] + "' globally...")
                    .setCancelable(false)
                    .setView(pb)
                    .show();
                    
                new android.os.Handler().postDelayed(() -> {
                    dialog.dismiss();
                    // Bypass standard recreate() for a smoother intent refresh
                    startActivity(new android.content.Intent(this, SettingsActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }, 2000); 
            });
            
            if (i == currentThemeId) {
                // If selected, natively highlight the CardView (because applySolidCardGeometry removed the default background!)
                applySolidCardGeometry(btn, getGradientStart(), 30f);
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 15, 0, 15); // Explicit Tile Spacing logic fixes overlapping monolith 
            
            grid.addView(btn, lp);
        }
        return grid;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.d(TAG, "Settings Hub Synchronized.");
    }
}
