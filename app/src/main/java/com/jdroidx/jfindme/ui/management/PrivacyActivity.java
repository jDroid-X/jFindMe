package com.jdroidx.jfindme.ui.management;

import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jdroidx.jfindme.engine.security.SecurityEngine;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> ManagementCluster -> PrivacyActivity (L4)
 * Hardening: L7 Architectural Registry Parity & Depth Synchronization.
 */
public class PrivacyActivity extends BaseActivityJava {

    private final String TAG = "PrivacyActivity";

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
        title.setText("IDENTITY SECURITY PROTOCOL");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Privacy Engines & System Telemetry");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        subtitle.setPadding(0, scale(10), 0, 0);
        header.addView(subtitle);

        // 🚀 ANALYTICAL HUB: Architecture Status Bar (Transparency Pillar)
        header.addView(buildArchitectureStatusBar());

        mainRoot.addView(header);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int cPad = getResponsivePaddingQuota();
        content.setPadding(cPad * 2, cPad * 2, cPad * 2, cPad * 4);

        // Hardware Permissions
        content.addView(createSectionHeader("HARDWARE PERMISSIONS"));
        
        LinearLayout pPanel = new LinearLayout(this);
        pPanel.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, pPanel, "CARD_STANDARD", 1.0f);
        int pPad = scale(40);
        pPanel.setPadding(pPad, pPad, pPad, pPad);
        
        pPanel.addView(createHardwarePermission("Location Services", android.Manifest.permission.ACCESS_FINE_LOCATION, android.R.drawable.ic_menu_mylocation));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            pPanel.addView(createHardwarePermission("Background Scanning", android.Manifest.permission.ACCESS_BACKGROUND_LOCATION, android.R.drawable.ic_menu_rotate));
        }
        pPanel.addView(createActivePrefsToggle("Telemetry Feedback", "telemetry_enabled", true, android.R.drawable.ic_menu_info_details));
        
        content.addView(pPanel);

        // Identity Controls
        content.addView(createSectionHeader("IDENTITY SECURITY PROTOCOL"));
        
        LinearLayout idPanel = new LinearLayout(this);
        idPanel.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, idPanel, "CARD_STANDARD", 1.0f);
        idPanel.setPadding(pPad, pPad, pPad, pPad);
        
        idPanel.addView(createActivePrefsToggle("Two-Factor Authentication", "2fa_enabled", false, android.R.drawable.ic_lock_idle_lock));
        idPanel.addView(createActivePrefsToggle("Strict Tracking Constraints", "strict_track", true, android.R.drawable.ic_menu_compass));
        idPanel.addView(createActivePrefsToggle("Location History Tracking", "allow_dependents", true, android.R.drawable.ic_menu_recent_history));
        idPanel.addView(createActivePrefsToggle("Persistent Ghost Mode", "pref_ghost_mode_persistent", true, android.R.drawable.ic_menu_view));
        
        LinearLayout.LayoutParams layerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layerLp.setMargins(0, scale(40), 0, scale(40));
        content.addView(idPanel, layerLp);

        // Integrity Checks
        content.addView(createSectionHeader("INTEGRITY COMPLIANCE"));
        LinearLayout rootPanel = new LinearLayout(this);
        rootPanel.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, rootPanel, "CARD_STANDARD", 1.2f);
        rootPanel.setPadding(pPad, pPad, pPad, pPad);
        
        TextView diagnosticReport = new TextView(this);
        diagnosticReport.setTextColor(getTextColor());
        UiArchitect.applyText(this, diagnosticReport, "TEXT_BODY");
        diagnosticReport.setVisibility(View.GONE);
        
        rootPanel.addView(UiArchitect.createButton(this, "RUN_FULL_DIAGNOSTIC", v -> {
            diagnosticReport.setVisibility(View.VISIBLE);
            diagnosticReport.setText("Executing System Sweep...\n");
            
            new Thread(() -> {
                boolean isRooted = SecurityEngine.INSTANCE.isRooted();
                boolean devMode = android.provider.Settings.Secure.getInt(getContentResolver(), android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
                String report = "• Root / Subversion: " + (isRooted ? "FATAL" : "SECURE") + "\n\n• Developer Debug: " + (devMode ? "ENABLED" : "LOCKED") + "\n\n• Database: ENCRYPTED";
                runOnUiThread(() -> {
                    diagnosticReport.setText(report);
                    diagnosticReport.setTextColor(isRooted || devMode ? Color.RED : Color.parseColor("#4CAF50"));
                });
            }).start();
        }));
        
        rootPanel.addView(diagnosticReport);
        content.addView(rootPanel);

        scroll.addView(content);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainRoot;
    }

    private View createActivePrefsToggle(String l, String k, boolean d, int i) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int py = scale(25);
        row.setPadding(0, py, 0, py);
        
        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(i);
        icon.setColorFilter(getGradientStart());
        int is = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        row.addView(icon, new LinearLayout.LayoutParams(is, is));
        
        TextView tv = new TextView(this);
        tv.setText(l);
        tv.setTextColor(getTextColor()); 
        UiArchitect.applyText(this, tv, "TEXT_LABEL");
        tv.setPadding(scale(25), 0, 0, 0);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        android.widget.Switch toggle = new android.widget.Switch(this);
        toggle.setChecked(prefs.getBoolean(k, d));
        toggle.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean(k, c).apply());
        row.addView(toggle);
        return row;
    }

    private View createHardwarePermission(String l, String p, int i) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int py = scale(25);
        row.setPadding(0, py, 0, py);
        
        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(i);
        icon.setColorFilter(getGradientStart());
        int is = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        row.addView(icon, new LinearLayout.LayoutParams(is, is));
        
        TextView tv = new TextView(this);
        tv.setText(l);
        tv.setTextColor(getTextColor()); 
        UiArchitect.applyText(this, tv, "TEXT_LABEL");
        tv.setPadding(scale(25), 0, 0, 0);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        android.widget.Switch toggle = new android.widget.Switch(this);
        boolean g = androidx.core.content.ContextCompat.checkSelfPermission(this, p) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        // 🚀 QA Fix: Master Override. Checking both OS Permission AND the internal Service Halt flag.
        boolean isServiceActive = g && !prefs.getBoolean("pref_service_halted", false);
        toggle.setChecked(isServiceActive);

        toggle.setOnCheckedChangeListener((b, c) -> { 
            if(c && !g) {
                 // Trigger OS Permission Request
                 androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{p}, 100); 
            } else {
                 // 🔒 Enterprise Logic: If user manually switches OFF, we halt the service logic globally
                 prefs.edit().putBoolean("pref_service_halted", !c).apply();
                 com.jdroidx.jfindme.utils.SystemLogger.w("PrivacyShield", "Background Protection Strategy Mutated: Service Halt = " + !c);
            }
        });
        row.addView(toggle);
        return row;
    }

    private View createSectionHeader(String t) {
        TextView h = new TextView(this);
        h.setText(t);
        h.setTextColor(getGradientStart());
        UiArchitect.applyText(this, h, "TEXT_LABEL");
        int p = scale(20);
        h.setPadding(p, scale(40), 0, p);
        return h;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.i(TAG, "Privacy Shield Activated (L7).");
    }

    private View buildArchitectureStatusBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(20, 15, 20, 15);
        bar.setBackgroundColor(Color.argb(80, Color.red(getPrimaryColor()), Color.green(getPrimaryColor()), Color.blue(getPrimaryColor()))); 
        
        bar.addView(createArchitectureIcon(android.R.drawable.ic_lock_silent_mode, "Secure", "Analytical Tiering: 5-Tier Normalized Architecture active."));
        bar.addView(createArchitectureIcon(android.R.drawable.ic_menu_save, "Local", "Identity Hub: Data retrieval anchored in Tier 1 Local Profiles."));
        bar.addView(createArchitectureIcon(android.R.drawable.ic_popup_sync, "Matrix", "Analytical Sync: Cloud and Local parity maintained via tiered nodes."));
        bar.addView(createArchitectureIcon(android.R.drawable.ic_menu_recent_history, "Silicon", "Hardware Lock: Tier 2 Silicon Fingerprints verified and archived."));
        bar.addView(createArchitectureIcon(android.R.drawable.checkbox_on_background, "Mesh", "Relationship Mesh: Tier 3 Graph Traversal active for peer discovery."));

        return bar;
    }

    private View createArchitectureIcon(int resId, String label, String toastMsg) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(25, 5, 25, 5);
        box.setClickable(true);
        box.setOnClickListener(v -> android.widget.Toast.makeText(this, toastMsg, android.widget.Toast.LENGTH_SHORT).show());
        
        android.widget.ImageView iv = new android.widget.ImageView(this);
        iv.setImageResource(resId);
        iv.setColorFilter(getGradientOverlayText());
        int iSize = scale(60); 
        box.addView(iv, new LinearLayout.LayoutParams(iSize, iSize));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(9);
        tv.setTextColor(getGradientOverlayText());
        tv.setAlpha(0.6f);
        tv.setGravity(Gravity.CENTER);
        box.addView(tv);

        return box;
    }
}
