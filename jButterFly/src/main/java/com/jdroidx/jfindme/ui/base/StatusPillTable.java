package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 StatusPillTable: Enterprise Configuration Registry
 * Manages all status badges, health indicators, and level markers.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class StatusPillTable {

    public static class PillRecord {
        public String id;
        public String label;
        public int tintColor;
        public String talkBackDescription;
        public float elevation;
        public float glowIntensity;
        public boolean usesDynamicTheme;
        public String interactionType; // NONE, LIGHT
        public String statusType; // INFO, SUCCESS, WARNING, CRITICAL
        public int fallbackColor;

        public PillRecord(String id, String label, int tintColor, String talkBackDescription, 
                          float elevation, float glowIntensity, boolean usesDynamicTheme, 
                          String interactionType, String statusType, int fallbackColor) {
            this.id = id;
            this.label = label;
            this.tintColor = tintColor;
            this.talkBackDescription = talkBackDescription;
            this.elevation = elevation;
            this.glowIntensity = glowIntensity;
            this.usesDynamicTheme = usesDynamicTheme;
            this.interactionType = interactionType;
            this.statusType = statusType;
            this.fallbackColor = fallbackColor;
        }
    }

    private static final Map<String, PillRecord> registry = new HashMap<>();
    public static Map<String, PillRecord> getRegistry() { return registry; }

    static {
        registry.put("STATUS_ONLINE",  new PillRecord("STATUS_ONLINE",  "Online",  Color.GREEN, "Node is transmitting data", 5f, 0.4f, true, "NONE", "SUCCESS", Color.GREEN));
        registry.put("STATUS_OFFLINE", new PillRecord("STATUS_OFFLINE", "Offline", Color.RED,   "Node is disconnected", 5f, 0.2f, true, "NONE", "CRITICAL", Color.RED));
        registry.put("STATUS_SYNC",    new PillRecord("STATUS_SYNC",    "Syncing", Color.CYAN,  "Data reconciliation in progress", 5f, 0.5f, true, "LIGHT", "INFO", Color.CYAN));
        registry.put("STATUS_L6",      new PillRecord("STATUS_L6",      "L6 Arch", Color.MAGENTA, "Enterprise System Maturity L6", 12f, 0.8f, true, "LIGHT", "INFO", Color.WHITE));
        
        registry.put("STATUS_ACTIVE",  new PillRecord("STATUS_ACTIVE",  "Active",  Color.parseColor("#00E676"), "System is operationally active", 5f, 0.4f, true, "NONE", "SUCCESS", Color.GREEN));
        registry.put("STATUS_ALERT",   new PillRecord("STATUS_ALERT",   "Alert",   Color.parseColor("#FF1744"), "Emergency alert triggered", 8f, 0.6f, true, "LIGHT", "CRITICAL", Color.RED));
        registry.put("STATUS_SECURE",  new PillRecord("STATUS_SECURE",  "Secure",  Color.parseColor("#2979FF"), "Cryptographic lock engaged", 5f, 0.3f, true, "NONE", "SUCCESS", Color.BLUE));
        registry.put("STATUS_BETA",    new PillRecord("STATUS_BETA",    "Beta",    Color.parseColor("#FFD600"), "Experimental design stage", 5f, 0.2f, true, "NONE", "WARNING", Color.YELLOW));
    }

    public static View createPill(BaseActivityJava activity, String tableKey) {
        PillRecord record = registry.get(tableKey);
        if (record == null) return new View(activity);

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        TextView pill = new TextView(activity);
        pill.setText(record.label.toUpperCase());
        pill.setTextColor(Color.WHITE);
        
        float textSize = UiArchitect.getResponsiveTextSize(activity, "TEXT_MINI");
        int padH = 20, padV = 10;
        float elevation = record.elevation;

        switch (stage) {
            case LIBERAL:   padH = 40; padV = 20; elevation *= 2.0f; break;
            case STANDARD:  padH = 30; padV = 15; elevation *= 1.2f; break;
            case COMPACT:   break; 
            case SLIM:      padH = 15; padV = 8;  elevation *= 0.6f; break;
            case ULTRA_SLIM:padH = 10; padV = 5;  elevation *= 0.3f; break;
        }

        pill.setTextSize(textSize);
        pill.setPadding(padH, padV, padH, padV);
        pill.setGravity(Gravity.CENTER);
        pill.setTypeface(null, Typeface.BOLD);
        pill.setContentDescription(record.talkBackDescription);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(100f); // Perfect Capsule
        
        int color = record.usesDynamicTheme ? record.tintColor : record.fallbackColor;
        gd.setColor(Color.argb(200, Color.red(color), Color.green(color), Color.blue(color)));
        pill.setBackground(gd);
        
        // 🚀 QA Fix: Global Shadow Consistency (Neutral Forensic Grey)
        UiArchitect.applyNeutralShadow(activity, pill, elevation);
        pill.setClipToOutline(true);

        if (record.interactionType.equals("LIGHT")) {
            pill.setOnLongClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); return true; });
        }

        return pill;
    }
}
