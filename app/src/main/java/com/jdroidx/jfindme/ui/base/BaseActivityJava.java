package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.jdroidx.jfindme.engine.data.SpatialProcessor;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> Architecture -> UI Engine -> BaseActivityJava (L3)
 * Rules: 100% Java Programmatic, 7 Themes, No Button Widgets.
 * Integration: Python Data Engine (SpatialProcessor).
 */
public abstract class BaseActivityJava extends AppCompatActivity {

    protected SharedPreferences prefs;
    protected int currentThemeId;
    protected long currentUiHash;
    protected boolean isLandscape;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE);
        currentThemeId = prefs.getInt("theme_id", 0);
        currentUiHash = prefs.getLong("ui_config_hash", 0);
        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        
        super.onCreate(savedInstanceState);
        
        // Initialize Python Analytics Engine
        SpatialProcessor.init(getApplicationContext());
        
        View originalRoot = createLayout();
        
        // 🧱 UX Override: Clean Native Presentation. No 3D floating engines.
        if (originalRoot != null) {
            android.widget.FrameLayout masterWrap = new android.widget.FrameLayout(this);
            masterWrap.addView(originalRoot, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            
            // 🚀 QA Fix: Global Floating Help Icon recursively injected across all inherited screens!
            android.widget.ImageView globalHelpBtn = new android.widget.ImageView(this);
            globalHelpBtn.setImageResource(android.R.drawable.ic_menu_help);
            
            // Matches dynamic Header Line gradient polarity!
            globalHelpBtn.setColorFilter(getGradientOverlayText()); 
            globalHelpBtn.setPadding(35, 35, 35, 35);
            globalHelpBtn.setClickable(true);
            globalHelpBtn.setFocusable(true);
            
            android.widget.FrameLayout.LayoutParams helpLp = new android.widget.FrameLayout.LayoutParams(150, 150);
            helpLp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            
            int pTop = prefs != null ? prefs.getInt("custom_header_padding_top", 80) : 80;
            int pSide = prefs != null ? prefs.getInt("custom_header_padding_side", 50) : 50;
            helpLp.setMargins(0, (int)(pTop * 0.4f), (int)(pSide * 0.5f), 0);
            globalHelpBtn.setLayoutParams(helpLp);
            
            globalHelpBtn.setOnClickListener(v -> {
                String actName = getClass().getSimpleName().replace("Activity", "");
                String helpText = "Standard Application Screen active. Navigate to Help & Support for the complete Enterprise Matrix.";
                if (actName.equals("Auth")) helpText = "1. New User? Tap 'AUTO SETUP' below to instantly load default enterprise user details.\n2. Already have a device? Tap 'SYNC DEVICE' to securely pair and bypass manual setup.\n3. Themes: All 9 enterprise vectors sync seamlessly securely across your physical node cluster.";
                if (actName.equals("Group")) helpText = "CRITICAL RECOVERY (After Reinstall):\nBecause re-installations are strictly sandboxed as 'Fresh Slates' for security, you MUST tap [RESTORE LOST ROSTER ASSETS] before anything else. This forces an aggressive backend scan to locate your legacy devices and violently yank them back into your current secure Hub.\n\nFRESH PAIRING SEQUENCE:\n1. Open Option A (Remote) or Option B (Local).\n2. Tap [GENERATE UNIQUE TOKEN].\n3. The Target Device scans this token.\n4. An Approval Alert instantly interrupts your screen.\n5. Select the Target's exact Network Tier (Spouse/Child) and tap APPROVE to finalize the cryptographic handshake!";
                if (actName.equals("Map")) helpText = "• Step 1: Access the Map natively from the top Dashboard.\n• Step 2: Observe Live Avatars. Tap any Pin to pull up exact Geolocation data.\n• Step 3: Expand Map using Action Tiles to render unified Family trace paths.";
                if (actName.equals("Geofence")) helpText = "• Step 1: Drag Map to drop the target Radius Anchor Pin.\n• Step 2: Adjust the physical Radius (e.g., 200m) required to trigger Sirens.\n• Step 3: Lock the Geofence into Firebase explicitly.";
                if (actName.equals("Privacy")) helpText = "• Location Services: Authorizes native Android OS GPS and WiFi triangulation.\n\n• Background Scanning: Forces OS tracking while app is sleeping (Android 10+).\n\n• Telemetry Feedback: Sends anonymous crash diagnostics back to network.\n\n• Two-Factor Authentication (2FA): Requires secondary validation via Email/SMS for new device logins.\n\n• Strict Tracking Constraints: Restricts physical tracking specifically to defined geofences/zones.\n\n• Location History Tracking: Archives your live breadcrumbs into the History Database for 24 hours.";
                if (actName.equals("SystemLog")) helpText = "• Step 1: Navigate to 'System Audits'.\n• Step 2: Tap any Diagnostic Card (e.g., Battery Health, P2P Matrix).\n• Step 3: View the 7-Day Storage Array generated on your physical drive.\n• Step 4: Tap 'Full Report' to aggregate and export the live tracking logs.";
                if (actName.equals("History")) helpText = "• Step 1: Open 'Location Logs' from the Dashboard Grid.\n• Step 2: Select a target user from the dropdown.\n• Step 3: Scroll through the timeline to review clustered tracking coordinates.";
                if (actName.equals("Notif")) helpText = "• Step 1: Open 'Panic Alerts' (Notifications) from the Dashboard Grid.\n• Step 2: View the chronological history of all emergency sirens triggered.\n• Step 3: Active SOS events bypass Doze mode natively.";
                if (actName.equals("Profile") || actName.equals("AvatarEditor") || actName.equals("AvatarCrop")) helpText = "• Step 1: Access 'Me' or 'Profile'.\n• Step 2: Edit your Display Name or tap your Profile Picture.\n• Step 3: Use the Native Editor to crop and sync an image directly to the Map Engine.";
                if (actName.equals("FamilyFriends") || actName.equals("PersonDetails")) helpText = "• Step 1: Open 'Family and Friends' from the Navigation Drawer.\n• Step 2: Observe online statuses labeled securely.\n• Step 3: Tap a member to configure exact specific granular permissions individually.";
                if (actName.equals("Settings") || actName.equals("UiConfig")) helpText = "• Step 1: Access 'Settings Hub' from the Map Dashboard.\n• Step 2: Override the 'Tracking Resolution' interval or toggle 'Ghost Mode'.\n• Step 3: Enter the 'GLOBAL UI ARCHITECT' to reprogram the gradient colors physically across the entire App.";

                new android.app.AlertDialog.Builder(this)
                    .setTitle("Screen Guide: " + actName)
                    .setMessage(helpText)
                    .setNeutralButton("OPEN HELP DESK", (d, i) -> {
                        startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.settings.HelpSupportActivity.class));
                    })
                    .setPositiveButton("Dismiss", null)
                    .show();
            });
            
            // Globally deploy into every physical Matrix except explicit Drawers or Splash
            if (!(this instanceof com.jdroidx.jfindme.ui.settings.HelpSupportActivity) && 
                !(this instanceof com.jdroidx.jfindme.ui.map.MapActivity) && 
                !(this.getClass().getSimpleName().equals("SplashActivity"))) {
                masterWrap.addView(globalHelpBtn);
            }
            
            setContentView(masterWrap);
        }
        
        setupLogic();
        SystemLogger.i(getClass().getSimpleName(), "Native Utility Activity Resumed.");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 🚀 QA Fix: Global Theme Synchronization Back-Loop!
        // Detects if the internal Theme Array was altered by a Settings Override while this screen was paused in RAM.
        if (prefs != null) {
            int latestInstalledTheme = prefs.getInt("theme_id", 0);
            long latestUiHash = prefs.getLong("ui_config_hash", 0);
            if (currentThemeId != latestInstalledTheme || currentUiHash != latestUiHash) {
                currentThemeId = latestInstalledTheme;
                currentUiHash = latestUiHash;
                recreate(); // Natively flushes Android's draw-cache and executes createLayout() instantly with New Colors!
            }
        }
    }

    protected abstract View createLayout();
    protected abstract void setupLogic();

    protected int getResolvedThemeId() {
        if (currentThemeId == 8) { // 🧠 System Default Mode
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                return 1; // "Twilight Midnight" (Enterprise Dark Mode)
            } else {
                return 0; // "FamiSafe Violet" (Market Default Light)
            }
        }
        return currentThemeId;
    }

    protected int getPrimaryColor() { return ThemeManager.getScreenBackgroundColor(getResolvedThemeId()); }
    protected int getTextColor() { return ThemeManager.getTextPrimaryColor(getResolvedThemeId()); }
    protected int getAccentColor() { return ThemeManager.getGradientStart(getResolvedThemeId()); } // Fallback
    
    // 🧠 Native Mapping to the new Relational UI Database
    protected int getCardColor() { return ThemeManager.getCardBackgroundColor(getResolvedThemeId()); }
    protected int getSecondaryTextColor() { return ThemeManager.getTextSecondaryColor(getResolvedThemeId()); }
    protected int getGradientStart() { return ThemeManager.getGradientStart(getResolvedThemeId()); }
    protected int getGradientEnd() { return ThemeManager.getGradientEnd(getResolvedThemeId()); }
    protected int getGradientOverlayText() { return ThemeManager.getGradientOverlayColor(getResolvedThemeId()); }
    
    // 🚀 Native Access to Configured Widget Tints for Checkboxes & Radios
    protected int getWidgetTint() {
        if (prefs == null) return getGradientStart();
        String tintMode = prefs.getString("custom_widget_tint", "THEME");
        switch (tintMode) {
            case "WHITE": return android.graphics.Color.WHITE;
            case "BLACK": return android.graphics.Color.BLACK;
            case "GREEN": return android.graphics.Color.parseColor("#00E676");
            case "RED": return android.graphics.Color.parseColor("#FF1744");
            case "THEME":
            default: return getGradientStart();
        }
    }
    /**
     * 🚀 PILLAR 1: Global Theme & Geometry Subsystem Access
     */
    protected View createIconButton(String text, int iconRes, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        
        // 🚀 Global Interconnect: Inject Master UI Padding Density Matrix!
        int activePad = prefs != null ? prefs.getInt("custom_ui_padding", 40) : 40;
        int verticalPad = (int)(activePad * 0.75f); // Maintain structural integrity aspect ratio
        btn.setPadding(activePad, verticalPad, activePad, verticalPad);
        
        btn.setGravity(android.view.Gravity.CENTER_VERTICAL);
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setContentDescription(text); // 🚀 QA Synchronization: Maps the TalkBack semantic array natively into the core layout pipeline!
        
        // 🚀 QA Enforced Accessibility Rule: Explicit Hardware ENTER keyboard execution wrapper!
        btn.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                btn.performClick(); return true;
            } return false;
        });
        
        btn.setOnClickListener(listener);

        applySolidCardGeometry(btn, getCardColor(), 30f);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            btn.setOutlineSpotShadowColor(getGradientStart()); // Physics-based native glow matching Icon!
            btn.setOutlineAmbientShadowColor(getGradientStart());
        }

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getGradientStart()); // 🚀 QA Fix: Dynamically paints the Theme Color (e.g., Violet)!
        
        // 🚀 INTERCEPT: Execute Global Icon Density Overrides
        int activeDensity = prefs != null ? prefs.getInt("custom_icon_density", 140) : 140;
        int innerSize = (int)(activeDensity * 0.65); // Slightly larger relative matrix for List Items vs Grid Tiles
        btn.addView(icon, new LinearLayout.LayoutParams(innerSize, innerSize));

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getTextColor());
        tv.setPadding(35, 0, 0, 0);
        tv.setTextSize(14); // Softer consumer font size
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.addView(tv);

        return btn;
    }

    /**
     * Engine Utility: Paints a pristine solid white card (or dark card) with premium drop shadow
     */
    public void applySolidCardGeometry(View view, int solidColor, float defaultCornerRadius) {
        // QA Fix: Save padding arrays natively! Android OS explicitly destroys paddings when injecting fresh Background Drawables!
        int pL = view.getPaddingLeft(); int pT = view.getPaddingTop(); int pR = view.getPaddingRight(); int pB = view.getPaddingBottom();

        android.graphics.drawable.GradientDrawable nativeBg = new android.graphics.drawable.GradientDrawable();
        nativeBg.setColor(solidColor);
        
        // 🚀 SYSTEM ARCHITECTURE: Automatically intercept passed Geometry to execute User's Master Architectural Override!
        float activeRadius = prefs != null ? prefs.getFloat("custom_geometry_radius", defaultCornerRadius) : defaultCornerRadius;
        nativeBg.setCornerRadius(activeRadius);
        
        view.setBackground(nativeBg);
        view.setElevation(8f); // 🚀 Essential for the $100M App "Floating Card" effect
        view.setPadding(pL, pT, pR, pB); // Restore explicitly to prevent geometry collapse
    }

    /**
     * Legacy Pipeline Support: Intercepts deprecated Glassmorphism calls from older activities
     * and flawlessly injects them directly into the new Market-Ready geometry engine.
     */
    public void applyGlassmorphism(View view, int solidColor, float cornerRadius) {
        applySolidCardGeometry(view, solidColor, cornerRadius);
    }

    /**
     * Engine Utility: Executes the Multi-Shade Dual Gradient for Hero Headers!
     */
    public void applyLinearGradient(View view, float cornerRadius) {
        int pL = view.getPaddingLeft(); int pT = view.getPaddingTop(); int pR = view.getPaddingRight(); int pB = view.getPaddingBottom();

        int[] colors = {getGradientStart(), getGradientEnd()};
        android.graphics.drawable.GradientDrawable grad = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BL_TR, colors);
        grad.setCornerRadius(cornerRadius);
        view.setBackground(grad);
        view.setElevation(15f);
        
        view.setPadding(pL, pT, pR, pB); // Restore explicitly to block native Android collapse
    }

    // 🚀 INTERCEPT: Dynamically applies Custom Header Geometries globally across the Application Matrix
    protected void applyGlobalHeaderGeometry(LinearLayout header) {
        int pTop = prefs != null ? prefs.getInt("custom_header_padding_top", 80) : 80;
        int pBot = prefs != null ? prefs.getInt("custom_header_padding_bottom", 60) : 60;
        int pLeftRight = prefs != null ? prefs.getInt("custom_header_padding_side", 50) : 50;
        
        applyLinearGradient(header, 0f); // Engine executes background calculation explicitly first!
        header.setPadding(pLeftRight, pTop, pLeftRight, pBot); // Padding deployed safely atop the new theme background!
    }
}
