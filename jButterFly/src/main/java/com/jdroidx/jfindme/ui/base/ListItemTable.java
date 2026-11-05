package com.jdroidx.jfindme.ui.base;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 ListItemTable: Enterprise Configuration Registry
 * Standardizes diagnostic rows, history logs, and device listing nodes.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class ListItemTable {

    public static class ListRecord {
        public String id;
        public String defaultTitle;
        public String defaultSub;
        public int iconRes;
        public String talkBackDescription;
        public float elevation;
        public float glowIntensity;
        public boolean usesDynamicTheme;
        public String interactionType; // NONE, LIGHT, HEAVY
        public int fallbackColor;

        public ListRecord(String id, String defaultTitle, String defaultSub, int iconRes, 
                        String talkBackDescription, float elevation, float glowIntensity, 
                        boolean usesDynamicTheme, String interactionType, int fallbackColor) {
            this.id = id;
            this.defaultTitle = defaultTitle;
            this.defaultSub = defaultSub;
            this.iconRes = iconRes;
            this.talkBackDescription = talkBackDescription;
            this.elevation = elevation;
            this.glowIntensity = glowIntensity;
            this.usesDynamicTheme = usesDynamicTheme;
            this.interactionType = interactionType;
            this.fallbackColor = fallbackColor;
        }
    }

    private static final Map<String, ListRecord> registry = new HashMap<>();
    public static Map<String, ListRecord> getRegistry() { return registry; }

    static {
        registry.put("ROW_HISTORY",  new ListRecord("ROW_HISTORY",  "Location Ping", "Timestamp: --", android.R.drawable.ic_menu_recent_history, "Historical tracking point", 5f, 0.3f, true, "LIGHT", 0));
        registry.put("ROW_DIAG",     new ListRecord("ROW_DIAG",     "System Event", "Metric: --", android.R.drawable.ic_menu_info_details, "Diagnostic log entry", 5f, 0.4f, true, "HEAVY", 0));
        registry.put("ROW_DEVICE",   new ListRecord("ROW_DEVICE",   "Mobile Node", "Status: Active", android.R.drawable.ic_menu_mylocation, "Device identity record", 8f, 0.5f, true, "LIGHT", 0));
        registry.put("ROW_BATTERY",  new ListRecord("ROW_BATTERY",  "Telemetry Ping", "Level: Optimal", android.R.drawable.ic_lock_power_off, "Battery optimization stage", 4f, 0.2f, true, "LIGHT", 0));
        registry.put("ROW_STORAGE",  new ListRecord("ROW_STORAGE",  "Storage Recap", "Local History Override", android.R.drawable.ic_menu_save, "Diagnostic storage terminal", 8f, 0.5f, true, "LIGHT", 0));
        registry.put("ROSTER_ITEM_MASTER", new ListRecord("ROSTER_ITEM_MASTER", "Guardian Node", "Admin Rights Active", com.jdroidx.jfindme.R.drawable.ic_user_profile, "Master identity registry", 10f, 0.6f, true, "LIGHT", 0));
        registry.put("ROSTER_ITEM_CHILD",  new ListRecord("ROSTER_ITEM_CHILD",  "Managed Node",  "Tracking Enabled", com.jdroidx.jfindme.R.drawable.ic_user_profile, "Subordinate node registry", 10f, 0.3f, true, "LIGHT", 0));
    }

    public static View createRow(BaseActivityJava activity, String tableKey, String main, String sub) {
        return createRow(activity, tableKey, main, sub, null);
    }

    public static View createRow(BaseActivityJava activity, String tableKey, String main, String sub, View rightAction) {
        ListRecord record = registry.get(tableKey);
        if (record == null) return new View(activity);

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setBackground(activity.getSelectableItemBackground());
        
        int pad = 30;
        int iconSize = 100;
        float mainSize = 14f;
        float subSize = 11f;

        switch (stage) {
            case LIBERAL:   pad = 60; iconSize = 160; mainSize = 20f; subSize = 16f; break;
            case STANDARD:  pad = 45; iconSize = 130; mainSize = 18f; subSize = 14f; break;
            case COMPACT:   pad = 30; iconSize = 100; mainSize = 14f; subSize = 11f; break;
            case SLIM:      pad = 20; iconSize = 80;  mainSize = 12f; subSize = 10f; break;
            case ULTRA_SLIM:pad = 15; iconSize = 60;  mainSize = 10f; subSize = 9f;  break;
        }

        row.setPadding(pad, pad, pad, pad);
        EnterpriseCardTable.applyCard(activity, row, "CARD_STANDARD");
        
        float responsiveElevation = UiScalingMatrixTable.scaleElevation(stage, record.elevation);
        UiArchitect.applyNeutralShadow(activity, row, responsiveElevation);        
        ImageView icon = new ImageView(activity);
        icon.setImageResource(record.iconRes);
        icon.setColorFilter(activity.getGradientStart());
        row.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));
        
        LinearLayout textZone = new LinearLayout(activity);
        textZone.setOrientation(LinearLayout.VERTICAL);
        textZone.setPadding(pad, 0, 0, 0);
        
        TextView tMain = new TextView(activity);
        tMain.setText(main != null ? main : record.defaultTitle);
        tMain.setTextColor(activity.getTextColor());
        tMain.setTextSize(mainSize);
        tMain.setTypeface(null, Typeface.BOLD);
        tMain.setSingleLine(false);
        tMain.setMaxLines(2);
        tMain.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textZone.addView(tMain);
        
        TextView tSub = new TextView(activity);
        tSub.setText(sub != null ? sub : record.defaultSub);
        tSub.setTextColor(activity.getSecondaryTextColor());
        tSub.setTextSize(subSize);
        textZone.addView(tSub);
        
        row.addView(textZone, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        if (rightAction != null) {
            row.addView(rightAction, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        row.setContentDescription(record.talkBackDescription + ": " + tMain.getText());

        if (record.interactionType.equals("LIGHT")) {
            row.setOnLongClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); return true; });
        } else if (record.interactionType.equals("HEAVY")) {
            row.setOnLongClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); return true; });
        }

        return row;
    }
}
