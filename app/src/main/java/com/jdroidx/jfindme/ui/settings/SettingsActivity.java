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
import com.jdroidx.jfindme.ui.base.ListItemTable;
import com.jdroidx.jfindme.ui.base.UiArchitect;
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
        root.setBackgroundColor(getPrimaryColor());

        // 🚀 Header Matrix: Restored as per Control Hub Screenshot
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header); 

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("CONTROL HUB");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_HEADER"));
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        titleRow.addView(title);
        header.addView(titleRow);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Device Telemetry & Engine Architecture");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_SUB"));
        header.addView(subtitle);
        
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int setPad = getResponsivePaddingQuota();
        content.setPadding(setPad * 2, setPad, setPad * 2, setPad * 4);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 15, 0, 15);

        // --- LAYER 1: MASTER UI OVERRIDE ---
        content.addView(createSectionHeader("MASTER UI OVERRIDE"));
        LinearLayout themeCard = new LinearLayout(this);
        themeCard.setOrientation(LinearLayout.VERTICAL);
        
        themeCard.addView(createIconButton("GLOBAL UI & ICON ARCHITECT", android.R.drawable.ic_menu_gallery, v -> {
            try {
                startActivity(new Intent(this, Class.forName("com.jdroidx.jfindme.ui.settings.UiConfigActivity")));
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Compiling Master UI Engine...", android.widget.Toast.LENGTH_SHORT).show();
            }
        }), btnLp);
        content.addView(themeCard, getCardParams());
        
        // --- LAYER 1.5: 🔋 BATTERY OPTIMIZATION COMMAND HUB ---
        content.addView(createSectionHeader("BATTERY OPTIMIZATION & PING INTELLIGENCE"));

        final long currentPulseValue = prefs.getLong("pref_tracking_interval", 60000L);
        final java.util.List<android.widget.Switch> batterySwitches = new java.util.ArrayList<>();

        String[] pingNames = {"60s PING (ECO)", "30s PING (BALANCED)", "15s PING (ACTIVE)", "7s PING (HIGH FREQ)", "1s PING (MAX INTENSITY)"};
        String[] consumptionStats = {"-2%/day", "-4%/day", "-8%/day", "-15%/day", "-25%/day"};
        final long[] pingValues = {60000L, 30000L, 15000L, 7000L, 1000L};

        LinearLayout batteryList = new LinearLayout(this);
        batteryList.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, batteryList, "CARD_STANDARD");

        for (int i = 0; i < pingValues.length; i++) {
            final int pos = i;
            final long val = pingValues[i];

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(30, 40, 30, 40);

            // Icon: Battery Matrix
            android.widget.ImageView bIcon = new android.widget.ImageView(this);
            bIcon.setImageResource(android.R.drawable.ic_lock_idle_low_battery);
            bIcon.setColorFilter(getGradientStart());
            int iconSz = UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
            row.addView(bIcon, new LinearLayout.LayoutParams(iconSz, iconSz));

            // Text Detail Group
            LinearLayout textGrp = new LinearLayout(this);
            textGrp.setOrientation(LinearLayout.VERTICAL);
            textGrp.setPadding(30, 0, 0, 0);
            textGrp.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView nameTv = new TextView(this);
            nameTv.setText(pingNames[i]);
            nameTv.setTextColor(getTextColor());
            nameTv.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_TITLE"));
            nameTv.setTypeface(null, Typeface.BOLD);
            textGrp.addView(nameTv);

            TextView statsTv = new TextView(this);
            statsTv.setText("Consumption: " + consumptionStats[i] + " | Status: DISCONNECTED");
            statsTv.setTextColor(getSecondaryTextColor());
            statsTv.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_SUB"));
            textGrp.addView(statsTv);
            row.addView(textGrp);

            // Toggle Switch
            android.widget.Switch bSwitch = new android.widget.Switch(this);
            bSwitch.setChecked(currentPulseValue == val);
            if (currentPulseValue == val) statsTv.setText("Consumption: " + consumptionStats[i] + " | Status: ACTIVE");
            
            batterySwitches.add(bSwitch);
            bSwitch.setOnCheckedChangeListener((v, isChecked) -> {
                if (isChecked) {
                    prefs.edit().putLong("pref_tracking_interval", val).apply();
                    for (android.widget.Switch s : batterySwitches) {
                        if (s != bSwitch) s.setChecked(false);
                    }
                    statsTv.setText("Consumption: " + consumptionStats[pos] + " | Status: ACTIVE");
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    android.widget.Toast.makeText(this, "Telemetry Node Adjusted: " + val + "ms Pulse Active.", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    statsTv.setText("Consumption: " + consumptionStats[pos] + " | Status: DISCONNECTED");
                }
            });

            bSwitch.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                    (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                    bSwitch.toggle();
                    return true;
                }
                return false;
            });
            row.addView(bSwitch);

            batteryList.addView(row);
            
            if (i < pingValues.length - 1) {
                View div = new View(this);
                div.setBackgroundColor(Color.argb(20, 150, 150, 150));
                batteryList.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
            }
        }


        content.addView(batteryList, getCardParams());

        // --- LAYER 2: ADVANCED APP TELEMETRY ---
        content.addView(createSectionHeader("ADVANCED APP TELEMETRY"));
        LinearLayout engineCard = new LinearLayout(this);
        engineCard.setOrientation(LinearLayout.VERTICAL);

        engineCard.addView(createIconButton("SMART AVATAR CLUSTERING", android.R.drawable.ic_menu_sort_by_size, v -> {
            boolean currentCluster = prefs.getBoolean("pref_smart_avatar_clustering", true);
            new android.app.AlertDialog.Builder(this)
                .setTitle("Configure Smart Clustering")
                .setMessage("PROS:\n- Stacks users within 10-meters into a clean badge.\n\nCONS:\n- Overlapping avatars hidden until zoomed in.\n\nCurrent State: " + (currentCluster ? "ENABLED" : "DISABLED"))
                .setPositiveButton("Enable", (dialog, which) -> prefs.edit().putBoolean("pref_smart_avatar_clustering", true).apply())
                .setNegativeButton("Disable", (dialog, which) -> prefs.edit().putBoolean("pref_smart_avatar_clustering", false).apply())
                .show();
        }), btnLp);
        
        engineCard.addView(createIconButton("VECTOR MAP RENDERER", android.R.drawable.ic_menu_mapmode, v -> {
            android.widget.Toast.makeText(this, "Hardware Accelerated Vector Engine Active", android.widget.Toast.LENGTH_SHORT).show();
        }), btnLp);
        
        content.addView(engineCard, getCardParams());

        // --- LAYER 3: SECURITY & ACCESS CONTROLS ---
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
            startActivity(new Intent(this, HelpSupportActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("SYSTEM LOGS & AUDIT TRAIL", android.R.drawable.ic_menu_info_details, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.SystemLogActivity.class));
        }), btnLp);
        secCard.addView(createIconButton("FACTORY RESET CONSOLE", android.R.drawable.ic_menu_revert, v -> {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.management.FactoryResetActivity.class));
        }), btnLp);
        
        content.addView(secCard, getCardParams());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return root;
    }

    private LinearLayout.LayoutParams getCardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int cMargin = getResponsivePaddingQuota() + 10;
        lp.setMargins(0, 0, 0, cMargin);
        return lp;
    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Seamless UI integration
        header.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_HEADER"));
        header.setTypeface(null, Typeface.BOLD);
        int hPad = getResponsivePaddingQuota();
        header.setPadding(hPad / 2, hPad * 2, 0, hPad);
        return header;
    }


    @Override
    protected void setupLogic() {
        SystemLogger.d(TAG, "Settings Hub Synchronized.");
    }
}
