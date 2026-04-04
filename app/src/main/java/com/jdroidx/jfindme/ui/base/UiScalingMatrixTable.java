package com.jdroidx.jfindme.ui.base;

import java.util.HashMap;
import java.util.Map;

/**
 * 👑 UI SCscalingMatrixTable: The Global Source of Truth (L7)
 * This centralized registry manages the 5-Stage Universal Sizing Matrix.
 * 
 * DESIGN STAGES:
 * 1. LIBERAL (Tablet)
 * 2. STANDARD (Large Phone)
 * 3. COMPACT (Standard)
 * 4. SLIM (Small Phone)
 * 5. ULTRA_SLIM (Micro)
 */
public class UiScalingMatrixTable {

    public static class ScalingRecord {
        public float headerTextScale;  // 🚀 L7 Weighted: High-Impact Tier (Headers, Titles)
        public float bodyTextScale;    // 🚀 L7 Weighted: Readability Tier (Sub, Body, Mini)
        public float iconScale;
        public float paddingScale;
        public float elevationScale;
        public float radiusScale;
        public float translationZFactor; // 🚀 L7 Depth Hub: Layer separation logic
        public float selectionStroke;
        public float borderStroke; // 🚀 L7 Forensic: Dynamic border thickness

        public ScalingRecord(float hTxt, float bTxt, float icon, float pad, float elev, float rad, float tz, float ss, float bs) {
            this.headerTextScale = hTxt;
            this.bodyTextScale = bTxt;
            this.iconScale = icon;
            this.paddingScale = pad;
            this.elevationScale = elev;
            this.radiusScale = rad;
            this.translationZFactor = tz;
            this.selectionStroke = ss;
            this.borderStroke = bs;
        }
    }

    private static final Map<UiArchitect.DesignStage, ScalingRecord> matrix = new HashMap<>();

    static {
        // --- 🚀 THE MASTER SCALING ENGINE: L7 Weighted Contrast Implementation ---
        // Format: HeaderTxt, BodyTxt, Icon, Padding, Elevation, Radius, TZFactor(Z), SelectionStroke, BorderStroke
        
        matrix.put(UiArchitect.DesignStage.LIBERAL,   new ScalingRecord(1.40f, 1.30f, 1.40f, 1.40f, 2.20f, 2.00f, 1.10f, 11f, 8f)); 
        matrix.put(UiArchitect.DesignStage.STANDARD,  new ScalingRecord(1.20f, 1.15f, 1.20f, 1.20f, 1.80f, 1.40f, 0.90f, 10f, 6f)); 
        matrix.put(UiArchitect.DesignStage.COMPACT,   new ScalingRecord(1.00f, 1.00f, 1.00f, 1.00f, 1.50f, 1.00f, 0.75f, 9f, 4f));  
        matrix.put(UiArchitect.DesignStage.SLIM,      new ScalingRecord(0.90f, 0.90f, 0.90f, 0.90f, 1.20f, 0.60f, 0.60f, 8f, 3f));  
        matrix.put(UiArchitect.DesignStage.ULTRA_SLIM,new ScalingRecord(0.80f, 0.80f, 0.80f, 0.80f, 1.00f, 0.40f, 0.50f, 6f, 2f));  
    }

    public static ScalingRecord get(UiArchitect.DesignStage stage) {
        return matrix.get(stage);
    }

    // --- 🛠️ HELPER METHODS FOR DIRECT INJECTION ---
    
    /** 🚀 L7 Compatibility Overload: Default to Body Scaling */
    public static float scaleText(UiArchitect.DesignStage stage, float baseSize) {
        return scaleText(stage, baseSize, false);
    }

    /** 🚀 New L7 Weighted Matrix Interceptor */
    public static float scaleText(UiArchitect.DesignStage stage, float baseSize, boolean isHeaderScale) {
        ScalingRecord record = get(stage);
        float factor = isHeaderScale ? record.headerTextScale : record.bodyTextScale;
        return baseSize * factor;
    }

    public static int scaleIcon(UiArchitect.DesignStage stage, int baseSize) {
        return (int)(baseSize * get(stage).iconScale);
    }

    public static int scalePadding(UiArchitect.DesignStage stage, int basePadding) {
        return (int)(basePadding * get(stage).paddingScale);
    }

    public static float scaleElevation(UiArchitect.DesignStage stage, float baseElevation) {
        return baseElevation * get(stage).elevationScale;
    }

    public static float scaleRadius(UiArchitect.DesignStage stage, float baseRadius) {
        return baseRadius * get(stage).radiusScale;
    }

    public static float scaleSelectionStroke(UiArchitect.DesignStage stage) {
        return get(stage).selectionStroke;
    }
    
    public static float scaleBorderStroke(UiArchitect.DesignStage stage) {
        return get(stage).borderStroke;
    }
    
    public static float getTZ(UiArchitect.DesignStage stage, float currentElevation) {
        return currentElevation * get(stage).translationZFactor;
    }

    /** 🚀 L7 Architecture Sync: Established Universal Gap (Header-to-Tile standard) */
    public static int getUniversalGap(UiArchitect.DesignStage stage) {
        return scalePadding(stage, 45); // Established Baseline: 45px
    }
}
