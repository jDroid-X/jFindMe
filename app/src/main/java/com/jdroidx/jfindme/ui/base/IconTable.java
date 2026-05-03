package com.jdroidx.jfindme.ui.base;

import com.jdroidx.jfindme.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.widget.ImageView;
import java.util.HashMap;
import java.util.Map;

/**
 * QA Architecture: Centralized Icon Config Database (PRO)
 * Automatically reconciles static Icon resources dynamically against the active System Theme bounds.
 * Implements 10-property "Enterprise Matrix" and 5-stage Universal Design Sizing.
 */
public class IconTable {


    public static class IconRecord {
        public String id;
        public int resourceId;
        public String talkBackLabel;
        public float elevation;
        public int internalPadding;
        public float glowIntensity;
        public String interactionType; // NONE, LIGHT, HEAVY
        public boolean dynamicBehavior;
        public int fallbackColor;
        public String clickAction; // Destination route or event key

        public IconRecord(String id, int resourceId, String talkBackLabel, float elevation, 
                          int internalPadding, float glowIntensity, String interactionType, 
                          boolean dynamicBehavior, int fallbackColor, String clickAction) {
            this.id = id;
            this.resourceId = resourceId;
            this.talkBackLabel = talkBackLabel;
            this.elevation = elevation;
            this.internalPadding = internalPadding;
            this.glowIntensity = glowIntensity;
            this.interactionType = interactionType;
            this.dynamicBehavior = dynamicBehavior;
            this.fallbackColor = fallbackColor;
            this.clickAction = clickAction;
        }
    }

    private static final Map<String, IconRecord> registry = new HashMap<>();

    public static Map<String, IconRecord> getRegistry() { return registry; }

    static {
        // [Stage 1: Enterprise Registry] - Initializing the "Master Map Mirror"
        // Properties: ID, ResID, TalkBack, Elev, Pad, Glow, Haptic, Dynamic, Fallback, Route
        registry.put("ACTION_EXPAND", new IconRecord("ACTION_EXPAND", android.R.drawable.ic_dialog_map, "Enlarge Live Viewport", 35f, 25, 0.8f, "LIGHT", true, Color.WHITE, "MAP_ZOOM_BOUNDS"));
        registry.put("ACTION_LOCATE", new IconRecord("ACTION_LOCATE", android.R.drawable.ic_dialog_map, "Recenter Satellite Tracker", 35f, 25, 0.6f, "HEAVY", true, Color.WHITE, "MAP_RECENTER"));
        registry.put("ACTION_ADD",    new IconRecord("ACTION_ADD",    android.R.drawable.ic_input_add, "Pair New Device Node", 35f, 25, 0.5f, "LIGHT", true, Color.WHITE, "ROUTE_PAIRING"));
        registry.put("ACTION_MINUS",  new IconRecord("ACTION_MINUS",  R.drawable.ic_minus, "Reduce Map Bounds", 25f, 20, 0.4f, "LIGHT", true, Color.WHITE, "MAP_ZOOM_OUT"));
        registry.put("ACTION_LAYERS", new IconRecord("ACTION_LAYERS", android.R.drawable.ic_dialog_dialer, "Switch Visual Layers", 35f, 25, 0.6f, "LIGHT", true, Color.WHITE, "MAP_TOGGLE_SATELLITE"));
        registry.put("ACTION_ZONES",  new IconRecord("ACTION_ZONES",  android.R.drawable.ic_dialog_map, "Define Geo-Bounds", 35f, 25, 0.7f, "HEAVY", true, Color.WHITE, "ROUTE_GEOFENCE"));
        registry.put("ACTION_GHOST",  new IconRecord("ACTION_GHOST",  android.R.drawable.ic_dialog_info, "Stealth Visibility Mask", 35f, 25, 0.5f, "LIGHT", true, Color.WHITE, "TOGGLE_CALLOUTS"));
        registry.put("ACTION_SEARCH", new IconRecord("ACTION_SEARCH", android.R.drawable.ic_dialog_map, "Query Target Array", 35f, 25, 0.6f, "LIGHT", true, Color.WHITE, "MAP_SEARCH_NODES"));
        registry.put("ACTION_WATCHDOG", new IconRecord("ACTION_WATCHDOG", R.drawable.ic_watchdog, "WatchDog Security Heartbeat", 45f, 20, 0.9f, "HEAVY", true, Color.WHITE, "ROUTE_WATCHDOG"));
    }


    /**
     * Called whenever a screen builds. Hooks into BaseActivityJava to guarantee
     * 100% color consistency with the globally assigned User Preferences Theme.
     */
    public static void applyTheme(Context context, ImageView view, String tableKey) {
        IconRecord record = registry.get(tableKey);
        if (record == null) return;

        UiArchitect.DesignStage stage = UiArchitect.getStage(context);
        view.setImageResource(record.resourceId);
        view.setContentDescription(record.talkBackLabel);

        // [Stage 2: Design Size Geometry Interceptor]
        float responsiveRadius = 24f;
        float responsiveElevation = record.elevation;
        int responsivePadding = record.internalPadding;

        switch (stage) {
            case LIBERAL:   
                responsiveRadius = 60f; 
                responsiveElevation *= 2.0f; 
                responsivePadding = 30; 
                break;
            case STANDARD:  
                responsiveRadius = 40f; 
                responsiveElevation *= 1.6f; 
                responsivePadding = 25; 
                break;
            case COMPACT:   
                responsiveRadius = 24f; 
                responsiveElevation *= 1.4f;
                responsivePadding = 20;
                break;
            case SLIM:      
                responsiveRadius = 16f; 
                responsiveElevation *= 1.2f; 
                responsivePadding = 15; 
                break;
            case ULTRA_SLIM:
                responsiveRadius = 12f; 
                responsiveElevation *= 1.0f; 
                responsivePadding = 5; 
                break;
        }

        view.setPadding(responsivePadding, responsivePadding, responsivePadding, responsivePadding);

        // [Stage 3: Dynamic Theme Integration]
        if (record.dynamicBehavior && context instanceof BaseActivityJava) {
            BaseActivityJava themeBase = (BaseActivityJava) context;
            int dynamicThemeColor = themeBase.getGradientStart();
            int modeLuminance = themeBase.getTextColor();

            int dynamicGlassBackground = Color.luminance(modeLuminance) < 0.5f ? 
                                         Color.parseColor("#E6FFFFFF") : 
                                         Color.parseColor("#E6000000");

            view.setColorFilter(themeBase.getGradientMid());
            view.setAlpha(1.0f);

            GradientDrawable geometry = new GradientDrawable();
            geometry.setShape(GradientDrawable.RECTANGLE);
            geometry.setCornerRadius(responsiveRadius);
            geometry.setColor(dynamicGlassBackground);
            
            view.setBackground(geometry);
            UiArchitect.applyNeutralShadow(themeBase, view, responsiveElevation);
            view.setClipToOutline(true);
        } else {
            view.setColorFilter(record.fallbackColor);
        }

        // [Stage 4: Haptic Interaction Matrix]
        if (record.interactionType.equals("LIGHT")) {
            view.setOnLongClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); return false; });
        } else if (record.interactionType.equals("HEAVY")) {
            view.setOnLongClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); return false; });
        }
    }
}

