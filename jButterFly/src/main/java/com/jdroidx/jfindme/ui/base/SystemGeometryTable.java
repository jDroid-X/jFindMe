package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 SystemGeometryTable: Universal Preset Registry
 * Manages all depth-based shadows and corner radius hierarchies.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class SystemGeometryTable {

    public static class GeometryRecord {
        public String id;
        public float baseValue;
        public float liberalFactor;
        public float slimFactor;
        public float ultraSlimFactor;
        public String description;

        public GeometryRecord(String id, float baseValue, float liberalFactor, 
                             float slimFactor, float ultraSlimFactor, String description) {
            this.id = id;
            this.baseValue = baseValue;
            this.liberalFactor = liberalFactor;
            this.slimFactor = slimFactor;
            this.ultraSlimFactor = ultraSlimFactor;
            this.description = description;
        }
    }

    private static final Map<String, GeometryRecord> shadowRegistry = new HashMap<>();
    private static final Map<String, GeometryRecord> radiusRegistry = new HashMap<>();

    static {
        // Shadow Presets (Elevation)
        shadowRegistry.put("SHADOW_LOW",    new GeometryRecord("SHADOW_LOW",    4f,  2.0f, 0.5f, 0.2f, "Subtle separation"));
        shadowRegistry.put("SHADOW_MEDIUM", new GeometryRecord("SHADOW_MEDIUM", 12f, 2.0f, 0.5f, 0.2f, "Standard elevation"));
        shadowRegistry.put("SHADOW_HIGH",   new GeometryRecord("SHADOW_HIGH",   25f, 2.0f, 0.5f, 0.2f, "Maximum depth for overlays"));

        // Corner Radius Tiers
        radiusRegistry.put("RADIUS_TIER_1", new GeometryRecord("RADIUS_TIER_1", 25f,  2.0f, 0.6f, 0.4f, "Small accent corners"));
        radiusRegistry.put("RADIUS_TIER_2", new GeometryRecord("RADIUS_TIER_2", 50f,  2.0f, 0.6f, 0.4f, "Standard card rounding"));
        radiusRegistry.put("RADIUS_TIER_3", new GeometryRecord("RADIUS_TIER_3", 100f, 2.0f, 0.6f, 0.4f, "Capsule or circular bounds"));
    }

    public static void applyShadow(BaseActivityJava activity, View view, String presetKey) {
        GeometryRecord record = shadowRegistry.get(presetKey);
        if (record == null) return;

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        float elevation = record.baseValue * UiScalingMatrixTable.get(stage).elevationScale;
        UiArchitect.applyNeutralShadow(activity, view, elevation);
    }

    public static float getRadius(BaseActivityJava activity, String tierKey) {
        GeometryRecord record = radiusRegistry.get(tierKey);
        if (record == null) return 0f;

        return UiArchitect.getResolvedRadius(activity, record.baseValue);
    }
}
