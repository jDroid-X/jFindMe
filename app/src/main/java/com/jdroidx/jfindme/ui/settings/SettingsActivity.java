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

        // ≡ƒÜÇ Header Matrix: Restored as per Control Hub Screenshot
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

        // 🧬 V62.8: Master Theme Switcher Correction
        android.widget.ImageView themeToggle = new android.widget.ImageView(this);
        themeToggle.setImageResource(getResolvedThemeId() == 1 ? android.R.drawable.ic_menu_day : android.R.drawable.ic_menu_recent_history); 
        themeToggle.setColorFilter(getGradientOverlayText());
        int tPad = scale(15);
        themeToggle.setPadding(tPad, tPad, tPad, tPad);
        themeToggle.setOnClickListener(v -> {
            int current = prefs.getInt("theme_id", 0);
            prefs.edit().putInt("theme_id", (current + 1) % 8).apply();
            safeRecreate(); // 🛡️ Stability Patch: Bypasses OEM Lifecycle crashes
        });
        titleRow.addView(themeToggle, new LinearLayout.LayoutParams(scale(80), scale(80)));

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
        // --- LAYER 1.5: 🔋 BATTERY & PING INTELLIGENCE ENTRY ---
        content.addView(createSectionHeader("🔋 BATTERY OPTIMIZATION & PING INTELLIGENCE"));
        View batteryLauncher = createIconButton("🔋 BATTERY OPTIMIZATION & PING HUB", android.R.drawable.ic_lock_power_off, true, v -> showBatteryOptimizerDialog());
        content.addView(batteryLauncher, btnLp);

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
            android.content.Intent watchdog = new android.content.Intent(this, com.jdroidx.jfindme.ui.management.WatchDogAuditActivity.class);
            watchdog.putExtra("VIEW_MODE", "STATE");
            startActivity(watchdog);
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

    private void showBatteryOptimizerDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());
        int p = scale(40);
        root.setPadding(p, p, p, p);
        
        // --- 🔋 HEADER ---
        TextView title = new TextView(this);
        title.setText("🔋 ENERGY & PING OPTIMIZER");
        title.setTextColor(getGradientStart());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        title.setPadding(0, 0, 0, 40);
        root.addView(title);

        // --- STEPPER SECTION ---
        LinearLayout stepperCard = new LinearLayout(this);
        stepperCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, stepperCard, "CARD_STANDARD");
        stepperCard.setPadding(30, 30, 30, 30);

        android.widget.Switch masterSwitch = new android.widget.Switch(this);
        masterSwitch.setText("🔋 MANUAL OVERRIDE (90s+)   ");
        masterSwitch.setTextColor(getTextColor());
        masterSwitch.setChecked(prefs.getBoolean("pref_stepper_active", false));
        stepperCard.addView(masterSwitch);

        final LinearLayout stepperControls = new LinearLayout(this);
        stepperControls.setGravity(Gravity.CENTER);
        stepperControls.setAlpha(masterSwitch.isChecked() ? 1.0f : 0.4f);
        
        TextView valTv = new TextView(this);
        long currentVal = prefs.getLong("pref_manual_pulse", 90000L) / 1000;
        valTv.setText(currentVal + "s");
        valTv.setTextColor(getTextColor());
        valTv.setTextSize(24f);
        valTv.setTypeface(null, Typeface.BOLD);
        valTv.setPadding(40, 0, 40, 0);

        // --- ➕ INCREMENT / ➖ DECREMENT VECTOR NODES ---
        int tacticalSize = scale(110);
        
        android.widget.ImageView decBtn = new android.widget.ImageView(this);
        decBtn.setImageResource(com.jdroidx.jfindme.R.drawable.ic_minus); // Forensic Minus Vector
        decBtn.setColorFilter(getGradientOverlayText());
        decBtn.setBackground(getSolidDrawable(getGradientStart(), scaleRadius(15)));
        decBtn.setPadding(30, 30, 30, 30);
        decBtn.setOnClickListener(v -> {
            if (!masterSwitch.isChecked()) return;
            long val = Long.parseLong(valTv.getText().toString().replace("s", ""));
            if (val > 90) { 
                val -= 10; valTv.setText(val + "s"); 
                prefs.edit().putLong("pref_manual_pulse", val * 1000).apply();
                prefs.edit().putLong("pref_tracking_interval", val * 1000).apply();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            }
        });

        android.widget.ImageView incBtn = new android.widget.ImageView(this);
        incBtn.setImageResource(android.R.drawable.ic_input_add); // Native Android Plus Vector
        incBtn.setColorFilter(getGradientOverlayText());
        incBtn.setBackground(getSolidDrawable(getGradientStart(), scaleRadius(15)));
        incBtn.setPadding(30, 30, 30, 30);
        incBtn.setOnClickListener(v -> {
            if (!masterSwitch.isChecked()) return;
            long val = Long.parseLong(valTv.getText().toString().replace("s", ""));
            val += 10; valTv.setText(val + "s"); 
            prefs.edit().putLong("pref_manual_pulse", val * 1000).apply();
            prefs.edit().putLong("pref_tracking_interval", val * 1000).apply();
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        });

        stepperControls.addView(decBtn, new LinearLayout.LayoutParams(tacticalSize, tacticalSize));
        stepperControls.addView(valTv);
        stepperControls.addView(incBtn, new LinearLayout.LayoutParams(tacticalSize, tacticalSize));
        stepperCard.addView(stepperControls);
        root.addView(stepperCard);

        // --- PRESET SECTION ---
        TextView presetTitle = new TextView(this);
        presetTitle.setText("🔋 PRESET OPTIMIZATION NODES");
        presetTitle.setTextColor(getSecondaryTextColor());
        presetTitle.setPadding(0, 40, 0, 20);
        root.addView(presetTitle);

        LinearLayout presetCard = new LinearLayout(this);
        presetCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, presetCard, "CARD_STANDARD");
        
        final long[] vals = {60000L, 30000L, 15000L, 7000L, 1000L};
        final String[] names = {"ECO (60s)", "BALANCED (30s)", "ACTIVE (15s)", "HIGH (7s)", "EXREME (1s)"};
        final java.util.List<android.widget.Switch> switches = new java.util.ArrayList<>();

        for(int i=0; i<vals.length; i++){
            final long v = vals[i];
            android.widget.Switch sw = new android.widget.Switch(this);
            sw.setText(names[i]);
            sw.setTextColor(getTextColor());
            sw.setPadding(20, 20, 20, 20);
            sw.setChecked(!masterSwitch.isChecked() && prefs.getLong("pref_tracking_interval", 60000L) == v);
            switches.add(sw);
            sw.setOnClickListener(view -> {
                if(masterSwitch.isChecked()) { sw.setChecked(false); return; }
                for(android.widget.Switch s : switches) s.setChecked(false);
                sw.setChecked(true);
                prefs.edit().putLong("pref_tracking_interval", v).apply();
            });
            presetCard.addView(sw);
        }
        root.addView(presetCard);

        masterSwitch.setOnCheckedChangeListener((b, isChecked) -> {
            prefs.edit().putBoolean("pref_stepper_active", isChecked).apply();
            stepperControls.setAlpha(isChecked ? 1.0f : 0.3f);
            if(isChecked) {
                for(android.widget.Switch s : switches) s.setChecked(false);
                long val = Long.parseLong(valTv.getText().toString().replace("s", "")) * 1000;
                prefs.edit().putLong("pref_tracking_interval", val).apply();
            }
        });

        dialog.setContentView(root);
        dialog.show();
        
        android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(lp);
    }
}
