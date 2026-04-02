package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * 👑 GLOBAL UI ARCHITECT: Enterprise Design System (L7 Waterfall)
 * Manages the "5-Stage Universal Sizing" matrix through a logical top-down gravity model.
 */
public class UiArchitect {

    public enum DesignStage {
        LIBERAL, STANDARD, COMPACT, SLIM, ULTRA_SLIM
    }

    // ============================================================================================
    // I. ARCHITECTURAL FOUNDATION (Stage Detection & Environmental Controls)
    // ============================================================================================

    public static DesignStage getStage(Context context) {
        // --- ARCHITECTURAL INTERCEPTOR: MANUAL OVERRIDE ---
        int override = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getInt("stage_override", -1);
        if (override != -1 && override < DesignStage.values().length) {
            return DesignStage.values()[override];
        }

        // 🚀 L7 Global Default Hub: Hard-Syncing to COMPACT stage as per user requirement!
        return DesignStage.COMPACT;
    }

    // ============================================================================================
    // II. MASTER SCALING MATRIX (Viewport-to-Pixel Responsive Engines)
    // ============================================================================================

    public static float getResponsiveTextSize(Context context, String tier) {
        DesignStage stage = getStage(context);
        float base = 16f; // Standard L7 Baseline
        boolean isHeader = false;
        switch (tier) {
            case "TEXT_HEADER": base = 22f; isHeader = true; break;
            case "TEXT_TITLE":  base = 18f; isHeader = true; break;
            case "TEXT_SUB":    base = 13f; break;
            case "TEXT_BODY":   base = 16f; break;
            case "TEXT_MINI":   base = 11.5f;break;
            case "TEXT_LABEL":  base = 18.0f; isHeader = true; break;
            case "BUTTON_TEXT": base = 12.0f; isHeader = true; break;
        }
        return UiScalingMatrixTable.scaleText(stage, base, isHeader);
    }

    public static float scaleText(Context context, float baseSize) {
        return UiScalingMatrixTable.scaleText(getStage(context), baseSize, false);
    }

    public static int getResponsiveIconSize(Context context, String tier) {
        DesignStage stage = getStage(context);
        int base = 120; // 🚀 L7 RECALIBRATED: 120px Universal Baseline
        switch (tier) {
            case "ICON_TINY":     base = 80;  break;
            case "ICON_SMALL":    base = 100; break;
            case "ICON_STANDARD": base = 120; break;
            case "ICON_LARGE":    base = 140; break;
            case "ICON_HERO":     base = 400; break;
        }
        return UiScalingMatrixTable.scaleIcon(stage, base);
    }

    // ============================================================================================
    // III. RELATIONAL TYPOGRAPHY ENGINE (Auto-calibration & Tier Mapping)
    // ============================================================================================

    public static void applyText(Context context, TextView tv, String tier) {
        DesignStage stage = getStage(context);
        float size = 16f;
        switch (tier) {
            case "TEXT_HEADER": size = 22f; break;
            case "TEXT_TITLE":  size = 18f; break;
            case "TEXT_BODY":   size = 16f; break;
            case "TEXT_SUB":    size = 13f; break;
            case "TEXT_MINI":   size = 11.5f;break;
            case "TEXT_LABEL":  size = 18f; break;
            case "BUTTON_TEXT": size = 12f; break;
            default:            size = 16f; break;
        }
        if (tier.contains("HEADER") || tier.contains("TITLE") || tier.equals("BUTTON_TEXT")) {
            tv.setTypeface(null, Typeface.BOLD);
        }
        tv.setTextSize(UiScalingMatrixTable.scaleText(stage, size));
    }

    public static void calibrateText(Context context, TextView tv, String tier) {
        if (tv == null || context == null) return;
        float targetSize = getResponsiveTextSize(context, tier);
        tv.setTextSize(targetSize);
        if (tier.contains("HEADER") || tier.contains("TITLE")) {
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    // ============================================================================================
    // IV. GLOBAL GEOMETRY PHYSICS (Card Depth, Selection States, Shadow Logic)
    // ============================================================================================

    public static void applyCard(BaseActivityJava activity, View view, String tableKey) {
        EnterpriseCardTable.applyCard(activity, view, tableKey);
    }

    public static void applyCardWithOverride(BaseActivityJava activity, View view, String tableKey) {
        applyCardWithOverride(activity, view, tableKey, 1.0f);
    }

    public static void applyCardWithOverride(BaseActivityJava activity, View view, String tableKey, float elevationFactor) {
        applyCard(activity, view, tableKey);
        
        // 🚀 L7 Depth Hub: Dynamic elevation factor application
        float currentElev = view.getElevation();
        if (currentElev > 0) {
            view.setElevation(UiScalingMatrixTable.scaleElevation(getStage(activity), currentElev * elevationFactor));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                view.setOutlineAmbientShadowColor(activity.getGradientStart());
                view.setOutlineSpotShadowColor(activity.getGradientStart());
            }
        }

        view.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        view.setClipToOutline(true);
    }

    public static void applySelection(BaseActivityJava activity, View view, boolean isSelected) {
        if (view == null) return;
        DesignStage stage = getStage(activity);
        int themeColor = activity.getGradientStart();
        android.graphics.drawable.Drawable background = view.getBackground();
        if (background instanceof android.graphics.drawable.GradientDrawable) {
            android.graphics.drawable.GradientDrawable gd = (android.graphics.drawable.GradientDrawable) background;
            if (isSelected) {
                int strokePx = (int) UiScalingMatrixTable.scaleSelectionStroke(stage);
                gd.setStroke(strokePx, themeColor); 
                gd.setColors(new int[]{ activity.getCardColor(), activity.getGradientEnd() });
                gd.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.BR_TL);
                view.setAlpha(1.0f);
            } else {
                gd.setStroke(3, themeColor);
                gd.setColor(activity.getCardColor());
                view.setAlpha(0.6f);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageView) {
                    ((ImageView)child).setColorFilter(isSelected ? activity.getGradientOverlayText() : activity.getGradientMid());
                    ((ImageView)child).setAlpha(1.0f);
                }
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    tv.setTextColor(isSelected ? activity.getGradientOverlayText() : activity.getSecondaryTextColor());
                    tv.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
                }
            }
        }
    }

    public static void applyThemedShadow(BaseActivityJava activity, View view, String presetKey) {
        SystemGeometryTable.applyShadow(activity, view, presetKey);
    }

    public static float getRadiusTier(BaseActivityJava activity, String tierKey) {
        return SystemGeometryTable.getRadius(activity, tierKey);
    }

    // ============================================================================================
    // V. ENTERPRISE COMPONENT REGISTRY (Nav, Headers, Buttons, Marquees, Tiles)
    // ============================================================================================

    public static void applyHeader(BaseActivityJava activity, LinearLayout header, String tableKey) {
        HeaderTable.applyHeader(activity, header, tableKey);
    }

    public static View createButton(BaseActivityJava activity, String tableKey, View.OnClickListener listener) {
        return ActionButtonTable.createButton(activity, tableKey, listener);
    }

    public static View createBottomNav(BaseActivityJava activity, String[] tableKeys, String activeKey) {
        return BottomNavTable.createNav(activity, tableKeys, activeKey);
    }

    public static View createTile(BaseActivityJava activity, String tableKey, View.OnClickListener listener) {
        return DashboardTileTable.createTile(activity, tableKey, listener);
    }

    public static void setTileSelection(View tile, boolean isSelected, int themeColor) {
        DashboardTileTable.setSelectionState(tile, isSelected, themeColor);
    }

    public static TextView createMarqueeBar(BaseActivityJava activity, String tableKey, String content) {
        return MarqueeTable.createBar(activity, tableKey, content);
    }

    public static View createStatusPill(BaseActivityJava activity, String tableKey) {
        return StatusPillTable.createPill(activity, tableKey);
    }

    public static View createListItem(BaseActivityJava activity, String tableKey, String main, String sub) {
        return ListItemTable.createRow(activity, tableKey, main, sub, null);
    }

    public static View createListItem(BaseActivityJava activity, String tableKey, String main, String sub, View rightAction) {
        return ListItemTable.createRow(activity, tableKey, main, sub, rightAction);
    }

    public static EditText createInputField(BaseActivityJava activity, String tableKey) {
        return FormInputTable.createField(activity, tableKey);
    }

    public static void applyThemedIcon(Context context, ImageView view, String iconKey) {
        IconTable.applyTheme(context, view, iconKey);
    }
}
