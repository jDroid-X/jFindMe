package com.jdroidx.jfindme.ui.base;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 ActionButtonTable: Enterprise Configuration Registry
 * Standardizes primary CTAs, destructive actions, and workflow triggers.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class ActionButtonTable {

    public static class ActionRecord {
        public String id;
        public String label;
        public String talkBackDescription;
        public float elevation;
        public float glowIntensity;
        public boolean usesDynamicTheme;
        public String interactionType; // NONE, LIGHT, HEAVY
        public int fallbackColor;
        public String clickAction;
        public boolean isPrimary;

        public ActionRecord(String id, String label, String talkBackDescription, float elevation, 
                          float glowIntensity, boolean usesDynamicTheme, String interactionType, 
                          int fallbackColor, String clickAction, boolean isPrimary) {
            this.id = id;
            this.label = label;
            this.talkBackDescription = talkBackDescription;
            this.elevation = elevation;
            this.glowIntensity = glowIntensity;
            this.usesDynamicTheme = usesDynamicTheme;
            this.interactionType = interactionType;
            this.fallbackColor = fallbackColor;
            this.clickAction = clickAction;
            this.isPrimary = isPrimary;
        }
    }

    private static final Map<String, ActionRecord> registry = new HashMap<>();
    public static Map<String, ActionRecord> getRegistry() { return registry; }

    static {
        registry.put("BTN_PRIMARY",   new ActionRecord("BTN_PRIMARY",   "Confirm Action", "Execute primary workflow", 10f, 0.6f, true, "LIGHT", 0, "FLOW_NEXT", true));
        registry.put("BTN_SECONDARY", new ActionRecord("BTN_SECONDARY", "Cancel", "Abort current operation", 5f, 0.3f, true, "NONE", 0, "FLOW_BACK", false));
        registry.put("BTN_DANGER",    new ActionRecord("BTN_DANGER",    "Terminated Node", "Wipe device configuration", 12f, 0.9f, false, "HEAVY", android.graphics.Color.RED, "FLOW_WIPE", true));
        registry.put("BTN_AUTH",      new ActionRecord("BTN_AUTH",      "Authorize", "Grant enterprise access", 15f, 0.7f, true, "HEAVY", 0, "AUTH_EXECUTE", true));
    }

    public static View createButton(BaseActivityJava activity, String tableKey, View.OnClickListener listener) {
        ActionRecord record = registry.get(tableKey);
        if (record == null) return new View(activity);

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        TextView btn = new TextView(activity);
        btn.setText(record.label.toUpperCase());
        btn.setTextColor(activity.getGradientOverlayText());
        btn.setGravity(Gravity.CENTER);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setOnClickListener(listener);
        btn.setContentDescription(record.talkBackDescription);
        
        int height = UiScalingMatrixTable.scalePadding(stage, 140);
        float radius = UiScalingMatrixTable.scaleRadius(stage, 20f);
        float textSize = UiArchitect.getResponsiveTextSize(activity, "BUTTON_TEXT");
        float elevation = UiScalingMatrixTable.scaleElevation(stage, record.elevation);

        btn.setTextSize(textSize);
        btn.setElevation(elevation);

        GradientDrawable gd;
        if (record.usesDynamicTheme) {
            gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, 
                new int[]{activity.getGradientStart(), activity.getGradientEnd()});
        } else {
            gd = new GradientDrawable();
            gd.setColor(record.fallbackColor);
        }
        btn.setTextSize(textSize);
        btn.setElevation(elevation);
        btn.setTranslationZ(elevation / 4f); // 🚀 Heavy CTA Depth Force

        // 🚀 QA Fix: Global Button Depth (L7 Matrix)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            int tint = activity.getGradientStart();
            int alpha = (int)(record.glowIntensity * 255);
            int glowColor = android.graphics.Color.argb(alpha, android.graphics.Color.red(tint), android.graphics.Color.green(tint), android.graphics.Color.blue(tint));
            btn.setOutlineSpotShadowColor(glowColor);
            btn.setOutlineAmbientShadowColor(glowColor);
        }
        btn.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        btn.setClipToOutline(radius > 0);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        lp.setMargins(0, 20, 0, 20);
        btn.setLayoutParams(lp);

        if (record.interactionType.equals("LIGHT")) {
            btn.setOnLongClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); return true; });
        } else if (record.interactionType.equals("HEAVY")) {
            btn.setOnLongClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); return true; });
        }

        // 🚀 QA Fix: Global Focus State Loop (Enter / D-Pad Support)
        btn.setFocusable(true);
        btn.setClickable(true);
        btn.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });

        return btn;
    }
}
