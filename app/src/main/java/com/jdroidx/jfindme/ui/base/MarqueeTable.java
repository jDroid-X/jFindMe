package com.jdroidx.jfindme.ui.base;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 MarqueeTable: Enterprise Configuration Registry
 * Standardizes scrolling text notifications, ticker bars, and moving state alerts.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class MarqueeTable {

    public static class MarqueeRecord {
        public String id;
        public String labelPrefix;
        public float scrollSpeed; // Not directly used in View but for future animator hooks
        public String talkBackPrefix;
        public float elevation;
        public float glowIntensity;
        public boolean usesDynamicTheme;
        public String interactionType; // NONE, LIGHT
        public int fallbackColor;
        public int marqueeRepeatLimit;

        public MarqueeRecord(String id, String labelPrefix, float scrollSpeed, String talkBackPrefix, 
                           float elevation, float glowIntensity, boolean usesDynamicTheme, 
                           String interactionType, int fallbackColor, int marqueeRepeatLimit) {
            this.id = id;
            this.labelPrefix = labelPrefix;
            this.scrollSpeed = scrollSpeed;
            this.talkBackPrefix = talkBackPrefix;
            this.elevation = elevation;
            this.glowIntensity = glowIntensity;
            this.usesDynamicTheme = usesDynamicTheme;
            this.interactionType = interactionType;
            this.fallbackColor = fallbackColor;
            this.marqueeRepeatLimit = marqueeRepeatLimit;
        }
    }

    private static final Map<String, MarqueeRecord> registry = new HashMap<>();
    public static Map<String, MarqueeRecord> getRegistry() { return registry; }

    static {
        registry.put("TICKER_SYSTEM",  new MarqueeRecord("TICKER_SYSTEM",  "[SYSTEM] ", 1.0f, "System broadcast: ", 5f, 0.4f, true, "NONE", Color.WHITE, -1));
        registry.put("TICKER_ALERT",   new MarqueeRecord("TICKER_ALERT",   "[CRITICAL] ", 2.0f, "Critical warning: ", 8f, 0.8f, true, "LIGHT", Color.RED, -1));
        registry.put("TICKER_WELCOME", new MarqueeRecord("TICKER_WELCOME", "[WELCOME] ", 0.5f, "User greeting: ", 5f, 0.2f, true, "NONE", Color.WHITE, 3));
    }

    public static TextView createBar(BaseActivityJava activity, String tableKey, String dynamicContent) {
        MarqueeRecord record = registry.get(tableKey);
        if (record == null) return new TextView(activity);

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        TextView tv = new TextView(activity);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        tv.setMarqueeRepeatLimit(record.marqueeRepeatLimit);
        tv.setSelected(true);
        tv.setText(record.labelPrefix + dynamicContent);
        tv.setTextColor(activity.getTextColor());
        tv.setContentDescription(record.talkBackPrefix + dynamicContent);

        int padH = 40, padV = 25;
        float textSize = 11f;
        float elevation = record.elevation;

        switch (stage) {
            case LIBERAL:   padH = 80; padV = 50; textSize = 15f; elevation *= 2.0f; break;
            case STANDARD:  padH = 60; padV = 35; textSize = 14f; elevation *= 1.2f; break;
            case COMPACT:   padH = 40; padV = 25; textSize = 13f; break;
            case SLIM:      padH = 25; padV = 15; textSize = 12f; elevation *= 0.6f; break;
            case ULTRA_SLIM:padH = 15; padV = 10; textSize = 11f; elevation *= 0.3f; break;
        }

        tv.setPadding(padH, padV, padH, padV);
        tv.setTextSize(textSize);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setElevation(elevation);
        tv.setTextColor(activity.getGradientEnd()); // 🚀 Theme Dynamic Sync
        
        GradientDrawable gd = new GradientDrawable();
        int tint = activity.getGradientStart();
        if (record.usesDynamicTheme) {
            gd.setColor(Color.argb(15, Color.red(tint), Color.green(tint), Color.blue(tint)));
        } else {
            gd.setColor(record.fallbackColor);
        }
        tv.setBackground(gd);
        
        return tv;
    }
}
