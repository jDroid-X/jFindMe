package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 HeaderTable: Enterprise Configuration Registry
 * Manages all hero branding zones, top gradients, and title headers.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class HeaderTable {

    public static class HeaderRecord {
        public String id;
        public String label;
        public float elevation;
        public float glowIntensity;
        public boolean usesGradient;
        public String talkBackDescription;
        public boolean isVisible;
        public String clickAction;
        public int fallbackColor;
        public String designPaddingMode; // DEEP, SLIM, ULTRA

        public HeaderRecord(String id, String label, float elevation, float glowIntensity, 
                            boolean usesGradient, String talkBackDescription, boolean isVisible, 
                            String clickAction, int fallbackColor, String designPaddingMode) {
            this.id = id;
            this.label = label;
            this.elevation = elevation;
            this.glowIntensity = glowIntensity;
            this.usesGradient = usesGradient;
            this.talkBackDescription = talkBackDescription;
            this.isVisible = isVisible;
            this.clickAction = clickAction;
            this.fallbackColor = fallbackColor;
            this.designPaddingMode = designPaddingMode;
        }
    }

    private static final Map<String, HeaderRecord> registry = new HashMap<>();

    public static Map<String, HeaderRecord> getRegistry() { return registry; }

    static {
        registry.put("HEADER_MAIN", new HeaderRecord("HEADER_MAIN", "Enterprise Hub", 25f, 0.8f, true, "Application Title Header", true, "RELOAD", Color.BLUE, "DEEP"));
        registry.put("HEADER_AUTH", new HeaderRecord("HEADER_AUTH", "Secure Gateway", 15f, 0.4f, true, "Identity Validation Portal", true, "NONE", Color.BLACK, "SLIM"));
        registry.put("HEADER_MAP",  new HeaderRecord("HEADER_MAP",  "Live Matrix", 25f, 0.6f, true, "Live Tracking Dashboard", true, "CENTER", Color.BLUE, "DEEP"));
    }

    public static void applyHeader(BaseActivityJava activity, LinearLayout header, String tableKey) {
        HeaderRecord record = registry.get(tableKey);
        if (record == null) return;
        if (!record.isVisible) { header.setVisibility(android.view.View.GONE); return; }

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        
        // [Stage Architecture Interceptor]
        int pT = 80, pB = 40, pS = 60;
        switch (stage) {
            case LIBERAL:   pT = 120; pB = 60; pS = 80; break;
            case STANDARD:  pT = 80;  pB = 40; pS = 60; break;
            case COMPACT:   pT = 40;  pB = 20; pS = 40; break;
            case SLIM:      pT = 30;  pB = 15; pS = 30; break;
            case ULTRA_SLIM:pT = 20;  pB = 10; pS = 20; break;
        }

        EnterpriseCardTable.applyCard(activity, header, "CARD_HEADER");
        header.setPadding(pS, pT, pS, pB);
        header.setElevation(record.elevation);
        header.setContentDescription(record.talkBackDescription);
    }
}
