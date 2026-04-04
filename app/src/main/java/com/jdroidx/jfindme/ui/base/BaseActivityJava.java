package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.os.Looper;
import com.jdroidx.jfindme.engine.data.SpatialProcessor;
import com.jdroidx.jfindme.utils.SystemLogger;
import com.jdroidx.jfindme.ui.base.UiArchitect;

/**
 * Main -> Architecture -> UI Engine -> BaseActivityJava (L3)
 * Rules: 100% Java Programmatic, 7 Themes, No Button Widgets.
 * Integration: Python Data Engine (SpatialProcessor).
 */
public abstract class BaseActivityJava extends AppCompatActivity {

    protected SharedPreferences prefs;
    protected int currentThemeId;
    protected int currentStage;
    protected long currentUiHash;
    protected boolean isLandscape;
    private static long lastRecreateTime = 0;
    private static final Handler registryHandler = new Handler(Looper.getMainLooper());
    private Runnable registryCheckRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = getSharedPreferences("system_registry", Context.MODE_PRIVATE);
        currentThemeId = prefs.getInt("theme_id", 0);
        currentUiHash = prefs.getLong("ui_config_hash", 0);
        currentStage = UiArchitect.getStage(this).ordinal();
        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        
        super.onCreate(savedInstanceState);
        
        // Initialize Python Analytics Engine
        new Thread(() -> {
            try { SpatialProcessor.init(getApplicationContext()); } catch (Exception ignored) {}
        }).start();
        
        View originalRoot = createLayout();
        
        if (originalRoot != null) {
            android.widget.FrameLayout masterWrap = new android.widget.FrameLayout(this);
            masterWrap.setClipChildren(false); 
            masterWrap.setClipToPadding(false);
            
            if (originalRoot instanceof android.view.ViewGroup) {
                android.view.ViewGroup vg = (android.view.ViewGroup) originalRoot;
                vg.setClipChildren(false);
                vg.setClipToPadding(false);
            }

            masterWrap.addView(originalRoot, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            
            // 🚀 QA Fix: Global Floating Help Icon Rebuilt!
            android.widget.ImageView globalHelpBtn = new android.widget.ImageView(this);
            globalHelpBtn.setImageResource(android.R.drawable.ic_menu_help);
            globalHelpBtn.setColorFilter(getGradientOverlayText()); 
            int hPad = scale(20);
            globalHelpBtn.setPadding(hPad, hPad, hPad, hPad);
            globalHelpBtn.setClickable(true);
            globalHelpBtn.setFocusable(true);
            
            // 🚀 Universal Architecture Sync: Calibrated for precise Header-Row centering
            int helpSize = scale(100); 
            android.widget.FrameLayout.LayoutParams helpLp = new android.widget.FrameLayout.LayoutParams(helpSize, helpSize);
            helpLp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            
            // 🧠 Forensic Alignment: Centered within the 140px standard header height
            // (45px Universal Gap + ~15px vertical offset correction)
            int mTop = scale(55); 
            int mEnd = scale(25);
            helpLp.setMargins(0, mTop, mEnd, 0);
            globalHelpBtn.setLayoutParams(helpLp);
            
            UiArchitect.applyCardWithOverride(this, globalHelpBtn, "CARD_STANDARD", 1.2f);
            globalHelpBtn.setElevation(scale(25)); // Force distinct Z-axis separation
            globalHelpBtn.setOnClickListener(v -> triggerHelpDialog());
            
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
    
    private void startRegistryAudit() {
        if (registryCheckRunnable != null) return;
        
        registryCheckRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    int latestInstalledTheme = prefs.getInt("theme_id", 0);
                    long latestUiHash = prefs.getLong("ui_config_hash", 0);
                    int latestStage = prefs.getInt("stage_override", -1);
                    
                    UiArchitect.DesignStage currentStageEnum = UiArchitect.getStage(BaseActivityJava.this);
                    int currentStageVal = currentStageEnum.ordinal();
                    
                    if (currentThemeId != latestInstalledTheme || currentUiHash != latestUiHash || (latestStage != -1 && currentStage != latestStage)) {
                        if (System.currentTimeMillis() - lastRecreateTime > 2000) {
                            lastRecreateTime = System.currentTimeMillis();
                            currentThemeId = latestInstalledTheme;
                            currentUiHash = latestUiHash;
                            if (latestStage != -1) currentStage = latestStage;
                            
                            SystemLogger.i("BaseActivityJava", "Registry Mismatch. Rebooting UI Hub.");
                            safeRecreate();
                        }
                    }
                } catch (Exception e) {
                    SystemLogger.e("BaseActivityJava", "Audit Error", e);
                }
                
                registryHandler.postDelayed(this, 1500);
            }
        };
        registryHandler.post(registryCheckRunnable);
    }

    private void stopRegistryAudit() {
        if (registryCheckRunnable != null) {
            registryHandler.removeCallbacks(registryCheckRunnable);
            registryCheckRunnable = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRegistryAudit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRegistryAudit();
    }
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        
        // 🚀 L7 Architecture Sync: Manual UI Re-Injection
        // Rebuilds the geometry hub without destroying the Activity state or memory registry.
        onSaveCurrentState(); 
        View originalRoot = createLayout();
        if (originalRoot != null) {
            android.widget.FrameLayout masterWrap = new android.widget.FrameLayout(this);
            masterWrap.setClipChildren(false);
            masterWrap.addView(originalRoot, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            
            // Re-inject Help Icon Hub
            reInjectHelpIcon(masterWrap);
            
            setContentView(masterWrap);
            onUiRebuilt(); 
        }
    }

    private void reInjectHelpIcon(android.widget.FrameLayout masterWrap) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        android.widget.ImageView globalHelpBtn = new android.widget.ImageView(this);
        globalHelpBtn.setImageResource(android.R.drawable.ic_menu_help);
        globalHelpBtn.setColorFilter(getGradientOverlayText()); 
        
        // 🚀 L7 Symmetric Matrix: Recalibrated for Perfect Center-Axis Alignment
        int helpSize = 100;
        int mTop = 35;
        int mEnd = 30;
        float elevation = 25f;

        switch (stage) {
            case LIBERAL:   helpSize = 140; mTop = 45; mEnd = 40; elevation = 40f; break;
            case STANDARD:  helpSize = 100; mTop = 35; mEnd = 30; elevation = 25f; break;
            case COMPACT:   helpSize = 80;  mTop = 15; mEnd = 20; elevation = 15f; break;
            case SLIM:      helpSize = 65;  mTop = 10; mEnd = 15; elevation = 10f; break;
            case ULTRA_SLIM:helpSize = 50;  mTop = 8;  mEnd = 10; elevation = 5f;  break;
        }

        int scaledSize = scale(helpSize);
        android.widget.FrameLayout.LayoutParams helpLp = new android.widget.FrameLayout.LayoutParams(scaledSize, scaledSize);
        helpLp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        helpLp.setMargins(0, scale(mTop), scale(mEnd), 0);
        globalHelpBtn.setLayoutParams(helpLp);
        
        int hPad = scaledSize / 4;
        globalHelpBtn.setPadding(hPad, hPad, hPad, hPad);
        globalHelpBtn.setClickable(true);
        globalHelpBtn.setFocusable(true);

        UiArchitect.applyNeutralShadow(this, globalHelpBtn, UiScalingMatrixTable.scaleElevation(stage, elevation));
        globalHelpBtn.setClipToOutline(true);
        globalHelpBtn.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        
        globalHelpBtn.setOnClickListener(v -> triggerHelpDialog());
        
        if (!(this instanceof com.jdroidx.jfindme.ui.settings.HelpSupportActivity) && 
            !(this instanceof com.jdroidx.jfindme.ui.map.MapActivity) && 
            !(this.getClass().getSimpleName().equals("SplashActivity"))) {
            masterWrap.addView(globalHelpBtn);
        }
    }

    protected void onUiRebuilt() {} 
    protected void onSaveCurrentState() {} 
    protected void triggerHelpDialog() {
        String actName = getClass().getSimpleName().replace("Activity", "");
        String helpText = "Standard Application Screen active. Navigate to Help & Support for the complete Enterprise Matrix.";
        if (actName.contains("Auth")) helpText = "1. New User? Tap 'AUTO SETUP' below to instantly load default enterprise user details.\n2. Already have a device? Tap 'SYNC DEVICE' to securely pair and bypass manual setup.\n3. Themes: All 9 enterprise vectors sync seamlessly securely across your physical node cluster.";
        if (actName.equals("Group")) helpText = "CRITICAL RECOVERY (After Reinstall):\nBecause re-installations are strictly sandboxed as 'Fresh Slates' for security, you MUST tap [RESTORE LOST ROSTER ASSETS] before anything else. This forces an aggressive backend scan to locate your legacy devices and violently yank them back into your current secure Hub.\n\nFRESH PAIRING SEQUENCE:\n1. Open Option A (Remote) or Option B (Local).\n2. Tap [GENERATE UNIQUE TOKEN].\n3. The Target Device scans this token.\n4. An Approval Alert instantly interrupts your screen.\n5. Select the Target's exact Network Tier (Spouse/Child) and tap APPROVE to finalize the cryptographic handshake!";
        if (actName.equals("Map")) helpText = "• Step 1: Access the Map natively from the top Dashboard.\n• Step 2: Observe Live Avatars. Tap any Pin to pull up exact Geolocation data.\n• Step 3: Expand Map using Action Tiles to render unified Family trace paths.";
        if (actName.equals("Geofence")) helpText = "• Step 1: Drag Map to drop the target Radius Anchor Pin.\n• Step 2: Adjust the physical Radius (e.g., 200m) required to trigger Sirens.\n• Step 3: Lock the Geofence into Firebase explicitly.";
        if (actName.equals("Privacy")) helpText = "• Location Services: Authorizes native Android OS GPS and WiFi triangulation.\n\n• Background Scanning: Forces OS tracking while app is sleeping (Android 10+).\n\n• Telemetry Feedback: Sends anonymous crash diagnostics back to network.\n\n• Two-Factor Authentication (2FA): Requires secondary validation via Email/SMS for new device logins.\n\n• Strict Tracking Constraints: Restricts physical tracking specifically to defined geofences/zones.\n\n• Location History Tracking: Archives your live breadcrumbs into the History Database (Default: 7 Days, USER Override supported).";
        if (actName.equals("SystemLog")) helpText = "• Step 1: Navigate to 'System Audits'.\n• Step 2: Tap any Diagnostic Card (e.g., Battery Health, P2P Matrix).\n• Step 3: View the 7-Day Storage Array generated on your physical drive.\n• Step 4: Tap 'Full Report' to aggregate and export the live tracking logs.";
        if (actName.equals("History")) helpText = "• Step 1: Storage Limit - Tap the '7 DAYS' value box to manually override how long history logs remain on your physical device.\n• Step 2: Time Selection - Select a forensic window (e.g., 4h, 12h, 24h) to render tracking trails on the map engine.\n• Step 3: Replay Controls - Tap START to physically replay the target's path with real-time speed/battery telemetry.\n• Step 4: Diagnostic Metrics - '0.0 km/h' indicates as Static/Dwell event. 'N/A' indicates missing forensic sensor telemetry at the time of the ping.";
        if (actName.equals("Notif")) helpText = "• Step 1: Open 'Panic Alerts' (Notifications) from the Dashboard Grid.\n• Step 2: View the chronological history of all emergency sirens triggered.\n• Step 3: Active SOS events bypass Doze mode natively.";
        if (actName.equals("Profile") || actName.equals("AvatarEditor") || actName.equals("AvatarCrop")) helpText = "• Step 1: Access 'Me' or 'Profile'.\n• Step 2: Edit your Display Name or tap your Profile Picture.\n• Step 3: Use the Native Editor to crop and sync an image directly to the Map Engine.";
        if (actName.equals("FamilyFriends") || actName.equals("PersonDetails")) helpText = "• Step 1: Access individual member dossiers from the roster.\n• Step 2: Configure the Privacy & ACL Matrix (Live Sync, Biometrics).\n\n" +
                                                                             "PRIVACY PERMISSIONS (PROS vs CONS):\n\n" +
                                                                             "| State | Pros | Cons |\n" +
                                                                             "| :--- | :--- | :--- |\n" +
                                                                             "| ✅ GRANTED | Full recovery sync, real-time safety, remote admin. | Higher data usage, cloud-stored records. |\n" +
                                                                             "| ❌ DENIED | Absolute local-only privacy, GDPR minimization. | No remote recovery, limited emergency alerts. |\n\n" +
                                                                             "• Step 3: Enable 'Global Cloud Sync' ONLY if you require multi-device dossier recovery.";
        if (actName.equals("Settings") || actName.equals("UiConfig")) helpText = "• Step 1: Access 'Settings Hub' from the Map Dashboard.\n• Step 2: Override the 'Tracking Resolution' interval or toggle 'Ghost Mode'.\n• Step 3: Enter the 'GLOBAL UI ARCHITECT' to reprogram the gradient colors physically across the entire App.";

        showForensicDialog("Screen Guide: " + actName, helpText);
    }

    // 🦋 Master QA Enterprise Fix: Completely Bypasses Chinese OEM 'BinderProxy' crashes natively!
    protected void safeRecreate() {
        // 🚀 QA Fix: Input Channel Protection (Ensures window is ready for handover)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            android.content.Intent hardwareMirror = getIntent();
            hardwareMirror.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);
            startActivity(hardwareMirror);
            overridePendingTransition(0, 0);
        }, 150); // 🧩 Forensic Debounce (150ms)
    }

    protected abstract View createLayout();
    protected abstract void setupLogic();

    public void applyAccessibility(View v, String desc) {
        if (v == null) return;
        v.setFocusable(true);
        if (v instanceof ImageView && desc != null) {
            v.setContentDescription(desc);
        }
        
        // 🚀 QA Fix: Global Interaction Balancing (L7 Standard)
        // We allow FOCUS (Tab key/Enter support) but DISABLE FocusInTouchMode to prevent the 'Double-Tap' bug.
        if (!(v instanceof android.widget.EditText)) {
            v.setFocusable(true);
            v.setFocusableInTouchMode(false); // Enable one-touch execution
            v.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                   (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                    view.performClick();
                    return true;
                }
                return false;
            });
        }
    }

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
    
    public android.graphics.drawable.GradientDrawable getBorderDrawable(int color, int strokeWidth, float radius) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(android.graphics.Color.TRANSPARENT);
        gd.setStroke(strokeWidth, color);
        gd.setCornerRadius(radius);
        return gd;
    }

    public android.graphics.drawable.GradientDrawable getSolidDrawable(int color, float radius) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }
    
    // 🧠 Native Mapping to the new Relational UI Database
    protected int getCardColor() { 
        int start = getGradientStart();
        String mAlpha = ThemeManager.THEMES[getResolvedThemeId()][1].substring(1, 3);
        return Color.parseColor("#" + mAlpha + String.format("%06X", (0xFFFFFF & start)));
    }
    protected int getSecondaryTextColor() { return ThemeManager.getTextSecondaryColor(getResolvedThemeId()); }
    public int getGradientStart() { return ThemeManager.getGradientStart(getResolvedThemeId()); }
    protected int getGradientEnd() { return ThemeManager.getGradientEnd(getResolvedThemeId()); }
    public int getGradientMid() { return ThemeManager.getGradientMid(getResolvedThemeId()); }
    protected int getGradientOverlayText() { return ThemeManager.getGradientOverlayColor(getResolvedThemeId()); }

    // 🧠 QA Fix: Nav Footer Color (Gradient End Override)
    public int getNavCardColor() {
        int end = getGradientEnd();
        String mAlpha = com.jdroidx.jfindme.ui.base.ThemeManager.THEMES[getResolvedThemeId()][1].substring(1, 3);
        return android.graphics.Color.parseColor("#" + mAlpha + String.format("%06X", (0xFFFFFF & end)));
    }
    
    // 🧠 QA Fix: High-Density Popup Alpha (85% Opacity Override)
    protected int getPopupColor() {
        return Color.parseColor("#99000000"); // 🧱 Tier III: Solid Glass (60% Wash)
    }
    
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
        return createIconButton(text, iconRes, false, listener);
    }

    protected View createIconButton(String text, int iconRes, boolean isPrimary, View.OnClickListener listener) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        
        // 🚀 L7 Optimization: Fluid Padding Response
        int sidePad = UiScalingMatrixTable.scalePadding(stage, 40);
        int vertPad = UiScalingMatrixTable.scalePadding(stage, 30);
        tile.setPadding(sidePad, vertPad, sidePad, vertPad);
        
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setOnClickListener(listener);

        // 🚀 L7 Architecture Sync: Primary Actions use Gradients, Secondary use Glass/Solid
        UiArchitect.applyCardWithOverride(this, tile, isPrimary ? "CARD_CAPSULE_BUTTON" : "CARD_STANDARD");

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(isPrimary ? getGradientOverlayText() : getGradientMid()); 
        
        int iconSize = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        tile.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); // Anchor to the Left/Icon
        tv.setTextColor(isPrimary ? getGradientOverlayText() : getTextColor());
        
        // 🚀 L7 Optimization: Allow Text-Wrapping while keeping the Icon on the Left
        tv.setSingleLine(false);
        tv.setMaxLines(2);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        // 🚀 L7 Relational Auto-Calibration: Correct the '14sp' feel into the '18f' Standard
        UiArchitect.calibrateText(this, tv, "TEXT_TITLE");
        
        tv.setPadding(sidePad/2, 0, 0, 0);
        tile.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // 🧱 QA Fix: Force One-Touch Deployment & Enter Key Logic!
        applyAccessibility(tile, text); 
        tile.setFocusableInTouchMode(false); // Immediate tactile response override

        tile.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });

        return tile;
    }

    /**
     * Engine Utility: Paints a pristine solid white card (or dark card) with premium drop shadow
     */
    public void applySolidCardGeometry(View view, int solidColor, float defaultCornerRadius) {
        UiArchitect.applyCard(this, view, "CARD_STANDARD");
    }

    /**
     * Legacy Pipeline Support
     */
    public void applyGlassmorphism(View view, int solidColor, float cornerRadius) {
        UiArchitect.applyCard(this, view, "CARD_STANDARD");
    }

    /**
     * Engine Utility: Executes the Multi-Shade Dual Gradient for Hero Headers!
     */
    public void applyLinearGradient(View view, float cornerRadius) {
        UiArchitect.applyCard(this, view, "CARD_HEADER");
    }

    // 🚀 INTERCEPT: Dynamically applies Custom Header Geometries globally across the Application Matrix
    protected void applyGlobalHeaderGeometry(LinearLayout header) {
        UiArchitect.applyHeader(this, header, "HEADER_MAIN");
    }

    // 📱 =========================================================================================
    // 🚀 PILLAR 5: ENTERPRISE RESPONSIVE AUTO-SCALING ENGINE (MULTI-DEVICE QUOTAS)
    // =========================================================================================
    
    public int scale(int base) {
        return UiScalingMatrixTable.scalePadding(UiArchitect.getStage(this), base);
    }

    public float scaleText(float base) {
        return UiScalingMatrixTable.scaleText(UiArchitect.getStage(this), base, false);
    }

    public float scaleText(float base, boolean isHeader) {
        return UiScalingMatrixTable.scaleText(UiArchitect.getStage(this), base, isHeader);
    }

    public float scaleRadius(float base) {
        return UiScalingMatrixTable.scaleRadius(UiArchitect.getStage(this), base);
    }

    public int scaleIcon(int base) {
        return UiScalingMatrixTable.scaleIcon(UiArchitect.getStage(this), base);
    }

    protected int getResponsivePaddingQuota() {
        // 🚀 L7 Architecture Sync: Dynamically recalibrated for exact Screen Padding metrics (Baseline: 30px)
        return UiScalingMatrixTable.scalePadding(UiArchitect.getStage(this), 30);
    }

    protected int getResponsiveHeaderTopQuota() {
        // 🚀 L7 Architecture Sync: 45px Universal Gap Interceptor
        return UiScalingMatrixTable.getUniversalGap(UiArchitect.getStage(this));
    }

    protected int getResponsiveIconQuota() {
        return UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
    }

    protected float getResponsiveRadiusQuota() {
        return UiArchitect.getRadiusTier(this, "RADIUS_TIER_1");
    }

    /**
     * 🚀 PILLAR 6: NATIVE COMPONENT FEEDBACK
     */
    public android.graphics.drawable.Drawable getSelectableItemBackground() {
        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        return getResources().getDrawable(outValue.resourceId, getTheme());
    }

    protected android.widget.ArrayAdapter<String> createEnterpriseDropdownAdapter(java.util.ArrayList<String> data) {
        return new android.widget.ArrayAdapter<String>(this, 0, data) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return createEnterpriseNode(position);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return createEnterpriseNode(position);
            }

            private View createEnterpriseNode(int position) {
                LinearLayout layout = new LinearLayout(BaseActivityJava.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setBackgroundColor(Color.TRANSPARENT); // 🚀 QA Fix: Avoid stacking washes on individual nodes
                
                TextView tv = new TextView(BaseActivityJava.this);
                tv.setText(getItem(position));
                tv.setTextColor(getGradientStart()); // 🌈 High-Contrast Primary Node (Gradient Start)
                UiArchitect.applyText(BaseActivityJava.this, tv, "TEXT_BODY");
                int p = UiScalingMatrixTable.scalePadding(UiArchitect.getStage(BaseActivityJava.this), 40);
                tv.setPadding(p, p/2, p, p/2);
                layout.addView(tv);
                
                // Physical divider strictly forced under every node
                View divider = new View(BaseActivityJava.this);
                divider.setBackgroundColor(Color.argb(40, 150, 150, 150));
                layout.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
                
                return layout;
            }
        };
    }

    protected android.graphics.drawable.GradientDrawable createPopupBackground() {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(getPopupColor()); // Use high-density alpha
        bg.setCornerRadius(25f); 
        bg.setStroke(3, getGradientStart()); 
        return bg;
    }

    /**
     * 🚀 Master QA Fix: Unified Theme-Aware Forensic Dialog!
     * Ensures all 'Pop Up Messages' synchronize their text colors with the L7 Design Registry.
     */
    protected void showForensicDialog(String title, String message) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Dismiss", null)
            .show();
        
        applyDialogTheme(dialog);
    }

    protected void applyDialogTheme(android.app.AlertDialog dialog) {
        if (dialog == null) return;
        
        int start = getGradientStart();
        int end = getGradientEnd();
        
        // 🧱 TITLE LAYER: Dynamic Injection (Gradient End)
        int titleId = getResources().getIdentifier("alertTitle", "id", "android");
        TextView titleTv = dialog.findViewById(titleId);
        if (titleTv != null) {
            titleTv.setTextColor(end);
            titleTv.setTypeface(null, Typeface.BOLD);
        }
        
        // 🧱 MESSAGE LAYER: Dynamic Injection (Gradient Start)
        TextView messageTv = dialog.findViewById(android.R.id.message);
        if (messageTv != null) {
            messageTv.setTextColor(start);
        }
        
        // 🧱 BUTTON LAYER: Action Node (Gradient End)
        android.widget.Button btnPos = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        if (btnPos != null) {
            btnPos.setTextColor(end);
            btnPos.setTypeface(null, Typeface.BOLD);
        }
        android.widget.Button btnNeg = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
        if (btnNeg != null) btnNeg.setTextColor(Color.GRAY);
        
        // 🧱 BACKGROUND: Forensic Glassmorphism (Neutralized to earlier standard alert breakdown)
        // if (dialog.getWindow() != null) {
        //    dialog.getWindow().setBackgroundDrawable(createPopupBackground());
        // }
    }
}

