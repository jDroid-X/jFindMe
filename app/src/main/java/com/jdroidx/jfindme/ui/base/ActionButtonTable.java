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
        public int iconRes;
        public String clickAction;
        public boolean isPrimary;

        public ActionRecord(String id, String label, String talkBackDescription, int iconRes, float elevation, 
                          float glowIntensity, boolean usesDynamicTheme, String interactionType, 
                          int fallbackColor, String clickAction, boolean isPrimary) {
            this.id = id;
            this.label = label;
            this.talkBackDescription = talkBackDescription;
            this.iconRes = iconRes;
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
        registry.put("BTN_PRIMARY",   new ActionRecord("BTN_PRIMARY",   "Confirm Action", "Execute primary workflow", 0, 10f, 0.6f, true, "LIGHT", 0, "FLOW_NEXT", true));
        registry.put("BTN_SECONDARY", new ActionRecord("BTN_SECONDARY", "Cancel", "Abort current operation", 0, 5f, 0.3f, true, "NONE", 0, "FLOW_BACK", false));
        registry.put("BTN_DANGER",    new ActionRecord("BTN_DANGER",    "Terminated Node", "Wipe device configuration", 0, 12f, 0.9f, false, "HEAVY", android.graphics.Color.RED, "FLOW_WIPE", true));
        registry.put("BTN_AUTH",      new ActionRecord("BTN_AUTH",      "Authorize", "Grant enterprise access", 0, 15f, 0.7f, true, "HEAVY", 0, "AUTH_EXECUTE", true));
        
        // --- 🚀 Enterprise Factory Reset Matrix (V37 Integration) ---
        registry.put("SAVE_TO_LOCAL_TXT",    new ActionRecord("SAVE_TO_LOCAL_TXT",    "Export Matrix to FS", "Save database dump to local text file", 0, 10f, 0.4f, true, "LIGHT", 0, "SAVE_TX", false));
        registry.put("DATABASE_EXPLORER_V37",new ActionRecord("DATABASE_EXPLORER_V37","Launch Data Explorer", "Open advanced database manager", 0, 12f, 0.5f, true, "LIGHT", 0, "OPEN_NAV", true));
        registry.put("FIREBASE_CLOUD_MATRIX",new ActionRecord("FIREBASE_CLOUD_MATRIX","FIREBASE CLOUD MATRIX", "Explore raw data assets in Firebase Realtime Hub", 0, 15f, 0.6f, true, "LIGHT", 0, "OPEN_CLOUD", true));
        registry.put("SELECTIVE_SYSTEM_PURGE",new ActionRecord("SELECTIVE_SYSTEM_PURGE","SELECTIVE SYSTEM PURGE", "Target specific tables for dual-stack removal", 0, 20f, 0.8f, false, "HEAVY", android.graphics.Color.RED, "PURGE_SELECT", true));
        registry.put("VIOLENT_MASTER_PURGE",  new ActionRecord("VIOLENT_MASTER_PURGE",  "FACTORY HARD RESET (WIPE ALL)", "Irrevocably purge all local and cloud data", 0, 25f, 0.9f, false, "HEAVY", android.graphics.Color.parseColor("#C62828"), "PURGE_ALL", true));
        registry.put("FACTORY_RESET",         new ActionRecord("FACTORY_RESET",         "RETURN TO CONSOLE", "Return to the factory reset management hub", 0, 10f, 0.3f, true, "LIGHT", 0, "FLOW_BACK", false));
        registry.put("RETURN_TO_HOME",        new ActionRecord("RETURN_TO_HOME",        "RETURN TO HOME", "Exit diagnostic mode and return to map", 0, 10f, 0.3f, true, "LIGHT", 0, "FLOW_BACK", false));

        // --- 🛰️ Profile Hub Matrix (Restored with Icons) ---
        registry.put("BTN_PASSWORD",       new ActionRecord("BTN_PASSWORD",       "PASSWORD", "Modify security challenge", android.R.drawable.ic_lock_idle_lock, 10f, 0.4f, true, "LIGHT", 0, "OPEN_PWD", true));
        registry.put("BTN_SAVE_PROFILE",   new ActionRecord("BTN_SAVE_PROFILE",   "VERIFY & SAVE", "Synchronize identity with cloud", android.R.drawable.ic_menu_save, 15f, 0.7f, true, "HEAVY", 0, "SAVE_PF", true));
        registry.put("BTN_DETAIL_PROFILE", new ActionRecord("BTN_DETAIL_PROFILE", "DETAIL PROFILE", "View advanced configuration", android.R.drawable.ic_menu_info_details, 10f, 0.3f, true, "LIGHT", 0, "OPEN_DET", true));
        registry.put("BTN_ORG",            new ActionRecord("BTN_ORG",            "ORGANIZATION", "Manage hub membership", android.R.drawable.ic_menu_my_calendar, 10f, 0.3f, true, "LIGHT", 0, "OPEN_ORG", true));
        registry.put("BTN_LOGOUT",         new ActionRecord("BTN_LOGOUT",         "LOGOUT SESSION", "Terminate active session", android.R.drawable.ic_lock_power_off, 5f, 0.2f, false, "HEAVY", Color.WHITE, "EXEC_LOGOUT", true));

        // --- 🔐 Integrity Management Nodes (L7 Status Bars) ---
        registry.put("BTN_IDENTITY_SECURE", new ActionRecord("BTN_IDENTITY_SECURE", "Identity:\nSECURE", "Identity verified and encrypted", android.R.drawable.ic_lock_lock, 5f, 0.3f, false, "NONE", Color.parseColor("#AD1457"), "STATE_INFO", true));
        registry.put("BTN_DEVICE_PAIRING",  new ActionRecord("BTN_DEVICE_PAIRING",  "Device\nPairing", "Scan and pair forensic hardware", android.R.drawable.ic_menu_share, 5f, 0.3f, false, "NONE", Color.parseColor("#424242"), "OPEN_PAIRING", true));
    }

    public static View createButton(BaseActivityJava activity, String tableKey, View.OnClickListener listener) {
        ActionRecord record = registry.get(tableKey);
        if (record == null) {
            // 🛡️ Enterprise Fallback: Render visible primary button with human-readable label if key not in registry
            String label = (tableKey != null) ? tableKey.replace('_', ' ') : "ACTION";
            return UiArchitect.createPrimaryButton(activity, label, 0, listener);
        }

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        TextView btn = new TextView(activity);
        btn.setText(record.label.toUpperCase());
        
        // 🚀 QA Fix: Master Font Baseline (15sp translated to scaled pixels)
        float baseTextSize = 15f; 
        float textSize = UiScalingMatrixTable.scaleText(stage, baseTextSize, true);
        btn.setTextSize(textSize);
        btn.setGravity(Gravity.CENTER);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setOnClickListener(listener);
        btn.setContentDescription(record.talkBackDescription);
        
        // 🚀 Icons Integration: Left Compound Drawable
        if (record.iconRes != 0) {
            android.graphics.drawable.Drawable icon = activity.getResources().getDrawable(record.iconRes, null);
            icon.setTint(record.id.equals("BTN_LOGOUT") ? activity.getGradientStart() : activity.getGradientOverlayText());
            btn.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            btn.setCompoundDrawablePadding(20);
            btn.setPadding(30, 0, 10, 0); 
        }

        // 🚀 Height recalibration: 160px baseline (Synchronized with Image)
        int height = UiScalingMatrixTable.scalePadding(stage, 160);
        float radius = UiScalingMatrixTable.scaleRadius(stage, 20f);
        float elevation = UiScalingMatrixTable.scaleElevation(stage, record.elevation);

        if (record.id.equals("BTN_LOGOUT")) {
            btn.setTextColor(activity.getGradientStart());
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.WHITE);
            gd.setStroke(4, activity.getGradientStart());
            gd.setCornerRadius(UiArchitect.getResolvedRadius(activity, 20f));
            btn.setBackground(gd);
        } else {
            btn.setTextColor(activity.getGradientOverlayText());
            GradientDrawable gd;
            if (record.usesDynamicTheme) {
                gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, 
                    new int[]{activity.getGradientStart(), activity.getGradientEnd()});
            } else {
                gd = new GradientDrawable();
                gd.setColor(record.fallbackColor);
            }
            gd.setCornerRadius(UiArchitect.getResolvedRadius(activity, 20f));
            btn.setBackground(gd);
        }
        
        // 🚀 QA Fix: Global Button Depth (L7 Matrix - Neutral Grey Standard)
        UiArchitect.applyNeutralShadow(activity, btn, elevation);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        int margin = UiScalingMatrixTable.getUniversalGap(stage) / 2;
        lp.setMargins(margin, margin, margin, margin);
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
