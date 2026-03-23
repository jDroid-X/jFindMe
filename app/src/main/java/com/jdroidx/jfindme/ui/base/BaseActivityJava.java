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
            setContentView(originalRoot);
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
        android.graphics.drawable.GradientDrawable nativeBg = new android.graphics.drawable.GradientDrawable();
        nativeBg.setColor(solidColor);
        
        // 🚀 SYSTEM ARCHITECTURE: Automatically intercept passed Geometry to execute User's Master Architectural Override!
        float activeRadius = prefs != null ? prefs.getFloat("custom_geometry_radius", defaultCornerRadius) : defaultCornerRadius;
        nativeBg.setCornerRadius(activeRadius);
        
        view.setBackground(nativeBg);
        view.setElevation(8f); // 🚀 Essential for the $100M App "Floating Card" effect
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
        int[] colors = {getGradientStart(), getGradientEnd()};
        android.graphics.drawable.GradientDrawable grad = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BL_TR, colors);
        grad.setCornerRadius(cornerRadius);
        view.setBackground(grad);
        view.setElevation(15f);
    }

    // 🚀 INTERCEPT: Dynamically applies Custom Header Geometries globally across the Application Matrix
    protected void applyGlobalHeaderGeometry(LinearLayout header) {
        int pTop = prefs != null ? prefs.getInt("custom_header_padding_top", 80) : 80;
        int pBot = prefs != null ? prefs.getInt("custom_header_padding_bottom", 60) : 60;
        int pLeftRight = prefs != null ? prefs.getInt("custom_header_padding_side", 50) : 50;
        
        header.setPadding(pLeftRight, pTop, pLeftRight, pBot);
        applyLinearGradient(header, 0f);
    }
}
