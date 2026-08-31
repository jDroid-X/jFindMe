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
public abstract class BaseActivityJava extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** Intent extra key: suppresses the first onResume mismatch check after safeRecreate(). */
    private static final String EXTRA_SUPPRESS_RECREATE = "__suppress_recreate_check";

    protected SharedPreferences prefs;
    protected int currentThemeId;
    protected int currentStage;
    protected long currentUiHash;
    protected boolean isLandscape;
    protected boolean isUiRebuilding = false; // 🚀 Lifecycle Guard
    private boolean isInForeground = false;    // 🚀 Lifecycle Guard: prevents background activity recreation
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
        
        // 🚀 Register Master Registry Listener for real-time synchronization
        prefs.registerOnSharedPreferenceChangeListener(this);
        
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
            
            // 🚀 QA Fix: Global Floating Help Icon Rebuilt with Theme Border Dark Shade
            android.widget.ImageView globalHelpBtn = new android.widget.ImageView(this);
            globalHelpBtn.setImageResource(android.R.drawable.ic_menu_help);
            int borderDark = ThemeManager.isDarkTheme(getResolvedThemeId()) ? getGradientEnd() : getGradientStart();
            globalHelpBtn.setColorFilter(borderDark); 
            int hPad = scale(20);
            globalHelpBtn.setPadding(hPad, hPad, hPad, hPad);
            globalHelpBtn.setClickable(true);
            globalHelpBtn.setFocusable(true);
            
            android.graphics.drawable.GradientDrawable helpBg = new android.graphics.drawable.GradientDrawable();
            helpBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            helpBg.setColor(getCardColor());
            helpBg.setStroke(scale(4), borderDark);
            globalHelpBtn.setBackground(helpBg);
            
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
            
            globalHelpBtn.setElevation(scale(25)); // Force distinct Z-axis separation
            globalHelpBtn.setOnClickListener(v -> triggerHelpDialog());
            
            if (!(this instanceof com.jdroidx.jfindme.ui.settings.HelpSupportActivity) && 
                !(this instanceof com.jdroidx.jfindme.ui.map.MapActivity) && 
                !(this.getClass().getSimpleName().equals("SplashActivity"))) {
                masterWrap.addView(globalHelpBtn);
            }

            // 🚨 Tactical Offline Security: Red Screen Border if Internet is OFF
            offlineBorderOverlay = new View(this);
            android.graphics.drawable.GradientDrawable borderShape = new android.graphics.drawable.GradientDrawable();
            borderShape.setColor(Color.TRANSPARENT);
            borderShape.setStroke(scale(8), Color.parseColor("#E53935")); // Tactical Alert Red
            offlineBorderOverlay.setBackground(borderShape);
            offlineBorderOverlay.setClickable(false);
            offlineBorderOverlay.setFocusable(false);
            android.widget.FrameLayout.LayoutParams borderLp = new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            masterWrap.addView(offlineBorderOverlay, borderLp);
            updateNetworkBorderState();
            
            setContentView(masterWrap);
        }
        
        setupLogic();
        if (savedInstanceState != null) {
            onUiRebuilt(savedInstanceState);
        } else {
            onUiRebuilt(null);
        }
        SystemLogger.i(getClass().getSimpleName(), "Native Utility Activity Resumed.");
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        onSaveCurrentState(outState);
        outState.putBoolean("BASE_IS_LANDSCAPE", isLandscape);
    }
    
    // contract methods for subclasses
    protected void onSaveCurrentState(Bundle outState) {}
    protected void onUiRebuilt(Bundle savedState) {}
    
    /**
     * AUDIT FIX [AUD-004]: Removed redundant 1500ms polling Runnable.
     * The OnSharedPreferenceChangeListener (registered in onCreate) already handles
     * all registry changes reactively via onSharedPreferenceChanged() callback.
     * Keeping these methods for API compatibility — they are now intentional no-ops.
     */
    private void startRegistryAudit() {
        // NO-OP: OnSharedPreferenceChangeListener handles all reactive updates.
        // The previous 1500ms Runnable was polling the UI thread unnecessarily.
    }

    private void stopRegistryAudit() {
        // NO-OP: Matching no-op for startRegistryAudit().
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInForeground = true;

        // If this Activity was just re-launched by safeRecreate(), skip the first onResume check
        // to prevent an infinite finish()+startActivity() loop.
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_SUPPRESS_RECREATE, false)) {
            getIntent().removeExtra(EXTRA_SUPPRESS_RECREATE);
            SystemLogger.i("BaseActivityJava", "onResume: Suppressed post-recreation mismatch check.");
            return;
        }

        if (prefs != null) {
            int newTheme = prefs.getInt("theme_id", 0);
            long newHash = prefs.getLong("ui_config_hash", 0);
            int newStage = UiArchitect.getStage(this).ordinal();
            if (newTheme != currentThemeId || newHash != currentUiHash || newStage != currentStage) {
                SystemLogger.i("BaseActivityJava", "onResume: Theme or UI config change detected (" + currentThemeId + " -> " + newTheme + "). Executing Hot-Sync.");
                currentThemeId = newTheme;
                currentUiHash = newHash;
                currentStage = newStage;
                safeRecreate();
            }
        }
        registerNetworkMonitor();
        updateNetworkBorderState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInForeground = false;
        unregisterNetworkMonitor();
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
    protected void onDestroy() {
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("theme_id".equals(key) || "ui_config_hash".equals(key) || "stage_override".equals(key)) {
            // Only recreate if this activity is actually in the foreground.
            // Background activities will pick up changes in their own onResume().
            if (!isInForeground) {
                SystemLogger.i("BaseActivityJava", "Master Registry Signal: " + key + " changed (deferred: activity not in foreground).");
                return;
            }
            SystemLogger.i("BaseActivityJava", "Master Registry Signal: " + key + " changed. Executing Hot-Sync.");
            safeRecreate();
        }
    }
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        
        // 🚀 L7 Architecture Sync: Manual UI Re-Injection
        // Rebuilds the geometry hub without destroying the Activity state or memory registry.
        Bundle bridge = new Bundle();
        onSaveCurrentState(bridge); 
        View originalRoot = createLayout();
        if (originalRoot != null) {
            android.widget.FrameLayout masterWrap = new android.widget.FrameLayout(this);
            masterWrap.setClipChildren(false);
            masterWrap.addView(originalRoot, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            
            // Re-inject Help Icon Hub
            reInjectHelpIcon(masterWrap);
            
            setContentView(masterWrap);
            setupLogic();
            onUiRebuilt(bridge); 
        }
    }

    private void reInjectHelpIcon(android.widget.FrameLayout masterWrap) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        android.widget.ImageView globalHelpBtn = new android.widget.ImageView(this);
        globalHelpBtn.setImageResource(android.R.drawable.ic_menu_help);
        int borderDark = ThemeManager.isDarkTheme(getResolvedThemeId()) ? getGradientEnd() : getGradientStart();
        globalHelpBtn.setColorFilter(borderDark); 
        
        android.graphics.drawable.GradientDrawable helpBg = new android.graphics.drawable.GradientDrawable();
        helpBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        helpBg.setColor(getCardColor());
        helpBg.setStroke(scale(4), borderDark);
        globalHelpBtn.setBackground(helpBg); 
        
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

    protected void  triggerHelpDialog() {
        String actName = getClass().getSimpleName().replace("Activity", "");
        String helpText = "Standard Application Screen active. Navigate to Help & Support for the complete Enterprise Matrix.";
        if (actName.contains("Auth")) helpText = "1. New User? Tap 'AUTO SETUP' below to instantly load default enterprise user details.\n2. Already have a device? Tap 'SYNC DEVICE' to securely pair and bypass manual setup.\n3. Themes: All 9 enterprise vectors sync seamlessly securely across your physical node cluster.";
        if (actName.equals("FamilyFriends")) helpText = "🛰️ UNIVERSAL SOCIAL HUB (L7 Architect)\n\n" +
                                                                              "1. MONITORED ROSTER (TABS): View real-time tracking telemetry for Family and Friends.\n\n" +
                                                                              "2. GATEWAY HUB: Tap [+] to trigger the QR Handshake. Align the target's pairing code to finalize the relational bridge.\n\n" +
                                                                              "3. CLOUD RECOVERY: Tap [RECLAIM CLOUD ROSTER] below to perform an aggressive backend sweep and restore your existing network after a reinstall.\n\n" +
                                                                              "4. IDENTITY MIRROR: Your current dossier (Avatar + Name) is automatically mirrored across all connected nodes in milliseconds.";
        if (actName.equals("Map")) helpText = "• Step 1: Access the Map natively from the top Dashboard.\n• Step 2: Observe Live Avatars. Tap any Pin to pull up exact Geolocation data.\n• Step 3: Expand Map using Action Tiles to render unified Family trace paths.";
        if (actName.equals("Geofence")) helpText = "• Step 1: Drag Map to drop the target Radius Anchor Pin.\n• Step 2: Adjust the physical Radius (e.g., 200m) required to trigger Sirens.\n• Step 3: Lock the Geofence into Firebase explicitly.";
        if (actName.equals("Privacy")) helpText = "• Location Services: Authorizes native Android OS GPS and WiFi triangulation.\n\n• Background Scanning: Forces OS tracking while app is sleeping (Android 10+).\n\n• Telemetry Feedback: Sends anonymous crash diagnostics back to network.\n\n• Two-Factor Authentication (2FA): Requires secondary validation via Email/SMS for new device logins.\n\n• Strict Tracking Constraints: Restricts physical tracking specifically to defined geofences/zones.\n\n• Location History Tracking: Archives your live breadcrumbs into the History Database (Default: 7 Days, USER Override supported).";
        if (actName.equals("SystemLog")) helpText = "• Step 1: Navigate to 'System Audits'.\n• Step 2: Tap any Diagnostic Card (e.g., Battery Health, P2P Matrix).\n• Step 3: View the 7-Day Storage Array generated on your physical drive.\n• Step 4: Tap 'Full Report' to aggregate and export the live tracking logs.";
        if (actName.equals("History")) helpText = "• Step 1: Storage Limit - Tap the '7 DAYS' value box to manually override how long history logs remain on your physical device.\n• Step 2: Time Selection - Select a forensic window (e.g., 4h, 12h, 24h) to render tracking trails on the map engine.\n• Step 3: Replay Controls - Tap START to physically replay the target's path with real-time speed/battery telemetry.\n• Step 4: Diagnostic Metrics - '0.0 km/h' indicates as Static/Dwell event. 'N/A' indicates missing forensic sensor telemetry at the time of the ping.";
        if (actName.equals("Notif")) helpText = "• Step 1: Open 'Panic Alerts' (Notifications) from the Dashboard Grid.\n• Step 2: View the chronological history of all emergency sirens triggered.\n• Step 3: Active SOS events bypass Doze mode natively.";
        if (actName.equals("WatchDogAudit")) helpText = "• MISSION: Real-time silicon monitoring & forensic fault detection.\n\n• STEP 1: MONITORING - Observe the live heartbeat of the application. Red nodes indicate critical security or sync failures.\n\n• STEP 2: FILTERING - Use the level rack (INFO, WARN, ERR) to isolate specific diagnostic threads.\n\n• STEP 3: EXPORTING - Tap 'COPY' to export the 7-day master audit trail for forensic review.\n\n• STEP 4: PROMOTION - Critical errors are automatically promoted to the Alert Dashboard with high-priority sirens.";
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

    // 🦋 Master QA Enterprise Fix: OEM-safe theme & UI configuration recreation
    // Uses finish()+startActivity() to bypass Chinese OEM BinderProxy crashes with recreate().
    protected void safeRecreate() {
        long now = System.currentTimeMillis();
        if (now - lastRecreateTime < 2000) return; // 2s debounce to prevent rapid loops
        if (isUiRebuilding) return;
        lastRecreateTime = now;
        isUiRebuilding = true;

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) { isUiRebuilding = false; return; }
            
            // Sync ALL current instance fields using the SAME sources as onCreate
            if (prefs != null) {
                currentThemeId = prefs.getInt("theme_id", currentThemeId);
                currentUiHash = prefs.getLong("ui_config_hash", currentUiHash);
            }
            // CRITICAL: Use UiArchitect.getStage() ordinal, NOT raw prefs.getInt("stage_override")
            currentStage = UiArchitect.getStage(this).ordinal();

            try {
                android.content.Intent hardwareMirror = new android.content.Intent(this, this.getClass());
                hardwareMirror.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION);
                // Mark the new instance so its first onResume() skips the mismatch check
                hardwareMirror.putExtra(EXTRA_SUPPRESS_RECREATE, true);
                finish();
                overridePendingTransition(0, 0);
                startActivity(hardwareMirror);
                overridePendingTransition(0, 0);
            } catch (Exception e) {
                SystemLogger.e("BaseActivityJava", "safeRecreate failed", e);
            } finally {
                isUiRebuilding = false;
            }
        }, 200); // 200ms debounce for window readiness
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
        if (prefs == null) {
            prefs = getSharedPreferences("system_registry", Context.MODE_PRIVATE);
        }
        currentThemeId = prefs.getInt("theme_id", 0);
        int theme = currentThemeId;
        if (theme == 8) { // 🧠 System Default Mode
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            theme = (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) ? 1 : 0;
        }
        // 🚀 QA Guard: Ensure we never exceed the matrix bounds
        if (theme < 0 || theme >= ThemeManager.THEMES.length) theme = 0;
        return theme;
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
    public int getCardColor() { 
        return ThemeManager.getCardBackgroundColor(getResolvedThemeId());
    }
    public int getSecondaryTextColor() { return ThemeManager.getTextSecondaryColor(getResolvedThemeId()); }
    public int getGradientStart() { return ThemeManager.getGradientStart(getResolvedThemeId()); }
    public int getGradientEnd() { return ThemeManager.getGradientEnd(getResolvedThemeId()); }
    public int getGradientMid() { return ThemeManager.getGradientMid(getResolvedThemeId()); }
    public int getGradientOverlayText() { return ThemeManager.getBrightenedGradientOverlayColor(getResolvedThemeId()); }

    // 🧠 QA Fix: Nav Footer Color (Frosted Pottery Match)
    public int getNavCardColor() {
        return ThemeManager.getCardBackgroundColor(getResolvedThemeId());
    }
    
    // 🧠 QA Fix: High-Density Popup Alpha (Frosted Ceramic Wash)
    protected int getPopupColor() {
        return ThemeManager.isDarkTheme(getResolvedThemeId()) ? Color.parseColor("#E6111A28") : Color.parseColor("#F2FFFFFF");
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
    protected View createCompactIconButton(String text, int iconRes, View.OnClickListener listener) {
        return createCompactIconButton(text, iconRes, false, listener);
    }

    protected View createCompactIconButton(String text, int iconRes, boolean isPrimary, View.OnClickListener listener) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER);
        
        int sidePad = scale(12);
        int vertPad = scale(8);
        tile.setPadding(sidePad, vertPad, sidePad, vertPad);
        
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setOnClickListener(listener);

        UiArchitect.applyCardWithOverride(this, tile, isPrimary ? "CARD_CAPSULE_BUTTON" : "CARD_STANDARD");

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(isPrimary ? getGradientOverlayText() : getGradientMid()); 
        
        int iconSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        tile.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(isPrimary ? getGradientOverlayText() : getTextColor());
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setPadding(scale(6), 0, 0, 0);
        tile.addView(tv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        applyAccessibility(tile, text); 
        tile.setFocusableInTouchMode(false);

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

    private View offlineBorderOverlay;
    private android.net.ConnectivityManager.NetworkCallback networkCallback;

    public boolean isNetworkConnected() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.net.Network activeNet = cm.getActiveNetwork();
                if (activeNet == null) return false;
                android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(activeNet);
                return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                android.net.NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }
        } catch (Exception e) {
            return true;
        }
    }

    public void updateNetworkBorderState() {
        boolean isConnected = isNetworkConnected();
        runOnUiThread(() -> {
            if (offlineBorderOverlay != null) {
                offlineBorderOverlay.setVisibility(isConnected ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void registerNetworkMonitor() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                if (networkCallback == null) {
                    networkCallback = new android.net.ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(android.net.Network network) {
                            updateNetworkBorderState();
                        }
                        @Override
                        public void onLost(android.net.Network network) {
                            updateNetworkBorderState();
                        }
                        @Override
                        public void onCapabilitiesChanged(android.net.Network network, android.net.NetworkCapabilities networkCapabilities) {
                            updateNetworkBorderState();
                        }
                    };
                    cm.registerDefaultNetworkCallback(networkCallback);
                }
            }
        } catch (Exception ignored) {}
    }

    private void unregisterNetworkMonitor() {
        try {
            if (networkCallback != null) {
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    cm.unregisterNetworkCallback(networkCallback);
                }
                networkCallback = null;
            }
        } catch (Exception ignored) {}
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
        
        // 🚀 L7 Optimization: Fluid Best-Fit Padding Response
        int sidePad = UiScalingMatrixTable.scalePadding(stage, 20);
        int vertPad = UiScalingMatrixTable.scalePadding(stage, 14);
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
        
        // 🚀 L7 Relational Auto-Calibration: Correct font sizing for optimal density
        UiArchitect.calibrateText(this, tv, "TEXT_LABEL");
        
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

    /**
     * 🚀 PILLAR 4: TACTICAL ICONOGRAPHY GEOMETRY
     * Applies a solid background with a high-contrast stroke and precise corner radius.
     */
    public void applyStrokeGeometry(View view, int solidColor, int strokeColor, float radius, int strokeWidth) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(solidColor);
        gd.setCornerRadius(radius);
        gd.setStroke(strokeWidth, strokeColor);
        view.setBackground(gd);
        view.setClipToOutline(true);
        view.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
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
        // 🚀 L7 Architecture Sync: Dynamically recalibrated for exact Screen Padding metrics (User Baseline: 20px)
        return UiScalingMatrixTable.scalePadding(UiArchitect.getStage(this), 20);
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
        int themeId = getResolvedThemeId();
        int startColor = ThemeManager.getBrightenedGradientStart(themeId);
        int endColor = ThemeManager.getBrightenedGradientEnd(themeId);
        int overlayColor = ThemeManager.getBrightenedGradientOverlayColor(themeId);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{startColor, endColor}
        );
        bg.setCornerRadius(scale(18)); 
        bg.setStroke(scale(2), overlayColor == Color.WHITE ? Color.parseColor("#66FFFFFF") : Color.parseColor("#44000000")); // Radiant Vibrant Glow Halo
        return bg;
    }

    /**
     * 🚀 Master QA Fix: Unified Theme-Aware Forensic Dialog!
     * Ensures all 'Pop Up Messages' synchronize their text colors with the L7 Design Registry.
     */
    public void showForensicDialog(String title, String message) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Dismiss", null)
            .show();
        
        applyDialogTheme(dialog);
    }

    /**
     * 🛡️ Closed-Loop Interactive Confirmation Dialog (Human-in-the-Loop Safeguard)
     * For destructive or critical state changes (Unpair, Remove Geofence, Reset, Signout).
     */
    public void showThemedConfirmationDialog(String title, String message, String positiveText, String negativeText, Runnable onConfirm, Runnable onCancel) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText != null ? positiveText : "Confirm", (d, w) -> {
                if (onConfirm != null) onConfirm.run();
            })
            .setNegativeButton(negativeText != null ? negativeText : "Cancel", (d, w) -> {
                if (onCancel != null) onCancel.run();
            })
            .show();

        applyDialogTheme(dialog);
    }

    /**
     * 🛡️ Themed Action Dialog with single closed-loop callback
     */
    public void showThemedActionDialog(String title, String message, String positiveText, Runnable onConfirm) {
        showThemedConfirmationDialog(title, message, positiveText, "Cancel", onConfirm, null);
    }

    public void applyDialogTheme(android.app.AlertDialog dialog) {
        if (dialog == null) return;
        
        int themeId = getResolvedThemeId();
        int overlayColor = ThemeManager.getBrightenedGradientOverlayColor(themeId);
        
        // 🧱 TITLE LAYER: Dynamic Glowing Header Text
        int titleId = getResources().getIdentifier("alertTitle", "id", "android");
        TextView titleTv = dialog.findViewById(titleId);
        if (titleTv != null) {
            titleTv.setTextColor(overlayColor);
            titleTv.setTypeface(null, Typeface.BOLD);
        }
        
        // 🧱 MESSAGE LAYER: Dynamic Glowing Secondary Text
        TextView messageTv = dialog.findViewById(android.R.id.message);
        if (messageTv != null) {
            messageTv.setTextColor(overlayColor);
            messageTv.setAlpha(0.92f);
            messageTv.setLineSpacing(4f, 1.15f);
        }
        
        // 🧱 BUTTON LAYER: Action Node
        android.widget.Button btnPos = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        if (btnPos != null) {
            btnPos.setTextColor(overlayColor);
            btnPos.setTypeface(null, Typeface.BOLD);
        }
        android.widget.Button btnNeg = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
        if (btnNeg != null) {
            btnNeg.setTextColor(overlayColor);
            btnNeg.setAlpha(0.85f);
        }
        android.widget.Button btnNeu = dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL);
        if (btnNeu != null) {
            btnNeu.setTextColor(overlayColor);
            btnNeu.setAlpha(0.85f);
        }
        
        // 🧱 LIST VIEW LAYER: Options / Selection List Styling
        android.widget.ListView listView = dialog.getListView();
        if (listView != null) {
            listView.setBackgroundColor(Color.TRANSPARENT);
            listView.setCacheColorHint(Color.TRANSPARENT);
            android.graphics.drawable.ColorDrawable dividerColor = new android.graphics.drawable.ColorDrawable(
                overlayColor == Color.WHITE ? Color.parseColor("#33FFFFFF") : Color.parseColor("#33000000")
            );
            listView.setDivider(dividerColor);
            listView.setDividerHeight(scale(1));
        }

        // 🧱 BACKGROUND: Glowing Theme Gradient directly applied to parentPanel & Window
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.60f); // Enhanced Glowing Ambience
        }

        int parentPanelId = getResources().getIdentifier("parentPanel", "id", "android");
        View parentPanel = dialog.findViewById(parentPanelId);
        if (parentPanel != null) {
            parentPanel.setBackground(createPopupBackground());
            int pad = scale(12);
            parentPanel.setPadding(pad, pad, pad, pad);
        } else if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(createPopupBackground());
        }

        // Clear system default background from child containers
        int[] subPanelIds = new int[]{
            getResources().getIdentifier("topPanel", "id", "android"),
            getResources().getIdentifier("contentPanel", "id", "android"),
            getResources().getIdentifier("buttonPanel", "id", "android"),
            getResources().getIdentifier("customPanel", "id", "android")
        };
        for (int pId : subPanelIds) {
            if (pId != 0) {
                View panel = dialog.findViewById(pId);
                if (panel != null) {
                    panel.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
    }

    /**
     * 🛰️ L7 UNIVERSAL ADVISOR: Contextual Help Portal
     */
    protected void showHelpDialog(android.content.Context context, String featureKey) {
        String title = "FORENSIC GUIDANCE";
        StringBuilder msg = new StringBuilder();

        switch (featureKey) {
            case "GEOFENCE_MAP_HUB":
                title = "GEOSPATIAL DEPLOYMENT HUB";
                msg.append("📍 LONG-PRESS: Drop a safezone anchor on the map.\n\n");
                msg.append("🏷️ IDENTITY: Label the zone (e.g., Home, School).\n\n");
                msg.append("⭕ RADIUS: Enter the siren range in meters.\n\n");
                msg.append("🛰️ NODE: Assign this zone to a specific member device.\n\n");
                msg.append("🚀 DEPLOY: Synchronize the perimeter with the network.");
                break;
            default:
                msg.append("Operational protocols for this module are currently being de-encrypted. Please contact your forensic administrator for further tactical support.");
                break;
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(msg.toString())
            .setPositiveButton("Understood", null)
            .show();
        
        applyDialogTheme(dialog);
    }

    /**
     * 👑 Universal Validation Alert Popup
     */
    public void showValidationDialog(String title, String message) {
        UiArchitect.showActionableValidationDialog(this, title, message, "OK", null, null, null);
    }

    /**
     * 👑 Universal Action Confirmation Dialog with Options
     */
    public void showConfirmationDialog(String title, String message, String confirmBtn, String cancelBtn, Runnable onConfirm, Runnable onCancel) {
        UiArchitect.showActionableValidationDialog(this, title, message, confirmBtn, cancelBtn, onConfirm, onCancel);
    }

    /**
     * 👑 Universal Popup Notification with Selectable Options
     */
    public void showNotificationOptionsDialog(String title, String message, String[] options, UiArchitect.OnOptionSelectListener listener) {
        UiArchitect.showPopupNotificationWithOptions(this, title, message, options, listener);
    }
}


