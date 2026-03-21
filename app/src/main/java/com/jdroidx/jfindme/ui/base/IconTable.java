package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * QA Architecture: Centralized Icon Config Database
 * Automatically reconciles static Icon resources dynamically against the active System Theme bounds.
 */
public class IconTable {

    public static class IconRecord {
        public String heading;
        public int resourceId;
        public int fallbackColorCode;
        public boolean usesDynamicTheme; 
        
        public IconRecord(String heading, int resourceId, int fallbackColorCode, boolean usesDynamicTheme) {
            this.heading = heading; 
            this.resourceId = resourceId; 
            this.fallbackColorCode = fallbackColorCode; 
            this.usesDynamicTheme = usesDynamicTheme;
        }
    }

    private static final Map<String, IconRecord> registry = new HashMap<>();

    static {
        // Core Array Matrix - Bound dynamically to ThemeEngine
        registry.put("ACTION_EXPAND", new IconRecord("Scale Map Bounds", android.R.drawable.ic_menu_zoom, Color.WHITE, true));
        registry.put("ACTION_LOCATE", new IconRecord("Lock GPS Coordinate", android.R.drawable.ic_menu_mylocation, Color.WHITE, true));
        registry.put("ACTION_ADD",    new IconRecord("Pair New User Module", android.R.drawable.ic_input_add, Color.WHITE, true));
        registry.put("ACTION_MINUS",  new IconRecord("Zoom Out Map", com.jdroidx.jfindme.R.drawable.ic_minus, Color.WHITE, true));
        registry.put("ACTION_LAYERS", new IconRecord("Toggle Graphics Renderer", android.R.drawable.ic_menu_gallery, Color.WHITE, true));
        registry.put("ACTION_ZONES",  new IconRecord("Configure Safe Boundaries", android.R.drawable.ic_menu_myplaces, Color.WHITE, true));
        registry.put("ACTION_GHOST",  new IconRecord("Toggle Radar Stealth", android.R.drawable.ic_menu_view, Color.WHITE, true));
        registry.put("ACTION_SEARCH", new IconRecord("Execute Array Query", android.R.drawable.ic_menu_search, Color.WHITE, true));
    }

    /**
     * Called whenever a screen builds. Hooks into BaseActivityJava to guarantee
     * 100% color consistency with the globally assigned User Preferences Theme.
     */
    public static void applyTheme(Context context, android.widget.ImageView view, String tableKey) {
        IconRecord record = registry.get(tableKey);
        if (record == null) return;
        
        view.setImageResource(record.resourceId);
        
        // Dynamic Interceptor Matrix
        if (record.usesDynamicTheme && context instanceof BaseActivityJava) {
            BaseActivityJava themeBase = (BaseActivityJava) context;
            
            // Extract the true Theme Colors natively instead of hardcoding
            int dynamicThemeIcon = themeBase.getGradientStart(); 
            int modeLuminance = themeBase.getTextColor();
            
            // Reverse engineering contrast: If primary text is Dark (Bright Theme), UI backgrounds must be Bright 
            int dynamicGlassBackground = Color.luminance(modeLuminance) < 0.5f ? 
                                         Color.parseColor("#E6FFFFFF") : 
                                         Color.parseColor("#E6000000");

            // Apply calculated theme colors securely
            view.setColorFilter(dynamicThemeIcon); 
            
            android.graphics.drawable.GradientDrawable geometry = new android.graphics.drawable.GradientDrawable();
            geometry.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            geometry.setCornerRadius(24f); 
            geometry.setColor(dynamicGlassBackground);
            
            view.setBackground(geometry);
            view.setElevation(35f); // Increased hardware float
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                view.setOutlineSpotShadowColor(dynamicThemeIcon); // 🚀 PHYSICAL GLOW GENERATOR!
                view.setOutlineAmbientShadowColor(dynamicThemeIcon);
            }
        } else {
            // Un-themed Native Override
            view.setColorFilter(record.fallbackColorCode);
        }
    }
}
