package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 EnterpriseCardTable: Universal Geometry Registry
 * Manages all floating containers, dashboard backgrounds, and UI panels.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class EnterpriseCardTable {

    public static class CardRecord {
        public String id;
        public String description;
        public float baseRadius;
        public float defaultElevation;
        public float glowIntensity;
        public boolean useLinearGradient;
        public boolean usesDynamicTheme;
        public int fallbackColor;
        public String shadowMode; // SOFT, GLOW, HARD
        public String interactionType; // NONE, LIGHT

        public CardRecord(String id, String description, float baseRadius, float defaultElevation, 
                          float glowIntensity, boolean useLinearGradient, boolean usesDynamicTheme, 
                          int fallbackColor, String shadowMode, String interactionType) {
            this.id = id;
            this.description = description;
            this.baseRadius = baseRadius;
            this.defaultElevation = defaultElevation;
            this.glowIntensity = glowIntensity;
            this.useLinearGradient = useLinearGradient;
            this.usesDynamicTheme = usesDynamicTheme;
            this.fallbackColor = fallbackColor;
            this.shadowMode = shadowMode;
            this.interactionType = interactionType;
        }
    }

    private static final Map<String, CardRecord> registry = new HashMap<>();

    public static Map<String, CardRecord> getRegistry() { return registry; }

    static {
        // [Enterprise Registry] - Core Geometry Hierarchy
        // 🚀 QA Depth Boost: Increased base elevation values for stronger floating presence.
        registry.put("CARD_STANDARD", new CardRecord("CARD_STANDARD", "Primary UI Container", 25f, 25f, 0.45f, false, true, Color.WHITE, "SOFT", "NONE"));
        registry.put("CARD_DASHBOARD", new CardRecord("CARD_DASHBOARD", "Monitoring Zone Panel", 25f, 35f, 0.65f, false, true, Color.WHITE, "GLOW", "NONE"));
        registry.put("CARD_HEADER",    new CardRecord("CARD_HEADER",    "Hero Branding Zone", 0f, 50f, 0.85f, true, true, Color.BLUE, "GLOW", "NONE"));
        registry.put("CARD_MODAL",     new CardRecord("CARD_MODAL",     "Alert Dialog Overlay", 55f, 80f, 0.95f, false, true, Color.WHITE, "HARD", "LIGHT"));
        registry.put("CARD_CAPSULE_BUTTON", new CardRecord("CARD_CAPSULE_BUTTON", "Primary Action Geometry", 50f, 40f, 0.75f, true, true, Color.WHITE, "GLOW", "LIGHT"));
    }

    public static void applyCard(BaseActivityJava activity, View view, String tableKey) {
        CardRecord record = registry.get(tableKey);
        if (record == null) return;

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        
        // [Stage Architecture Interceptor]
        float responsiveRadius = UiScalingMatrixTable.scaleRadius(stage, record.baseRadius);
        float responsiveElevation = UiScalingMatrixTable.scaleElevation(stage, record.defaultElevation);
        int responsivePadding = activity.getResponsivePaddingQuota();

        view.setPadding(responsivePadding, responsivePadding, responsivePadding, responsivePadding);

        GradientDrawable gd = new GradientDrawable();
        int tintStart = activity.getGradientStart();
        int tintEnd = activity.getGradientEnd();

        if (record.useLinearGradient) {
            gd.setOrientation(GradientDrawable.Orientation.BL_TR);
            gd.setColors(new int[]{tintStart, tintEnd});
        } else {
            gd.setColor(activity.getCardColor());
        }
        
        // 🚀 QA Fix: Visual Edge! Synchronized with Spinner Profile: 3px @ 100% Gradient Start
        gd.setStroke(3, tintStart);
        
        gd.setCornerRadius(responsiveRadius);
        view.setBackground(gd);
        view.setElevation(responsiveElevation);
        view.setTranslationZ(UiScalingMatrixTable.getTZ(stage, responsiveElevation)); // 🚀 Centralized Physical Factor
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            float intensity = record.glowIntensity;
            // 🚀 QA Fix: Universal Depth Overdrive (L7 High-Vibrant Shadows)
            intensity = 0.95f; 

            int alpha = (int)(intensity * 255);
            int glowColor = Color.argb(alpha, Color.red(tintStart), Color.green(tintStart), Color.blue(tintStart));
            view.setOutlineSpotShadowColor(glowColor);
            view.setOutlineAmbientShadowColor(glowColor);
        }

        // 🚀 QA Fix: Force the Native Android Elevation Engine to recognize the dynamic background shape!
        view.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        view.setClipToOutline(responsiveRadius > 0);
    }
}
