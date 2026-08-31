package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 DashboardTileTable: Enterprise Configuration Registry
 * Manages all functional dashboard grids and monitoring tiles.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class DashboardTileTable {

    public static class TileRecord {
        public String id;
        public String label;
        public int iconRes;
        public String talkBackDescription;
        public float elevation;
        public float glowIntensity;
        public String interactionType; // NONE, LIGHT, HEAVY
        public boolean usesDynamicTheme;
        public int fallbackColor;
        public String clickAction; // Route or event string

        public TileRecord(String id, String label, int iconRes, String talkBackDescription, 
                          float elevation, float glowIntensity, String interactionType, 
                          boolean usesDynamicTheme, int fallbackColor, String clickAction) {
            this.id = id;
            this.label = label;
            this.iconRes = iconRes;
            this.talkBackDescription = talkBackDescription;
            this.elevation = elevation;
            this.glowIntensity = glowIntensity;
            this.interactionType = interactionType;
            this.usesDynamicTheme = usesDynamicTheme;
            this.fallbackColor = fallbackColor;
            this.clickAction = clickAction;
        }
    }

    private static final Map<String, TileRecord> registry = new HashMap<>();

    public static Map<String, TileRecord> getRegistry() { return registry; }

    static {
        // [Enterprise Registry] - Core Monitoring Hierarchy
        registry.put("NETWORK_LOCATOR", new TileRecord("NETWORK_LOCATOR", "Network\nLocator", com.jdroidx.jfindme.R.drawable.ic_locator_pin, "Scan Unified Satellite Network", 15f, 0.6f, "LIGHT", true, Color.CYAN, "DASH_RADAR"));
        registry.put("SYNC_DEVICES",   new TileRecord("SYNC_DEVICES",   "Device\nPairing", com.jdroidx.jfindme.R.drawable.ic_pairing_matrix, "Manage Associated Hardware Nodes", 15f, 0.5f, "HEAVY", true, Color.GREEN, "DASH_PAIRING"));
        registry.put("LOCATION_LOGS",  new TileRecord("LOCATION_LOGS",  "Location\nLogs", com.jdroidx.jfindme.R.drawable.ic_location_history, "Access Clustered Tracking Data", 15f, 0.4f, "LIGHT", true, Color.GRAY, "DASH_HISTORY"));
        registry.put("PANIC_ALERTS",   new TileRecord("PANIC_ALERTS",   "Panic\nAlerts", android.R.drawable.ic_dialog_alert, "View Critical Siren History", 15f, 0.8f, "HEAVY", true, Color.RED, "DASH_NOTIFS"));
        registry.put("SAFE_ZONES",     new TileRecord("SAFE_ZONES",     "Safe\nZones", com.jdroidx.jfindme.R.drawable.ic_safe_zone, "Configure Geographic Protected Domains", 15f, 0.6f, "LIGHT", true, Color.BLUE, "DASH_GEOFENCE"));
        registry.put("FAMILY_FRIENDS", new TileRecord("FAMILY_FRIENDS", "Family\nFriends", com.jdroidx.jfindme.R.drawable.ic_family_friends, "Coordinate with Trusted Contacts", 15f, 0.5f, "LIGHT", true, Color.MAGENTA, "DASH_FAMILY"));
        registry.put("SETTINGS_HUB",   new TileRecord("SETTINGS_HUB",   "Settings\nHub", com.jdroidx.jfindme.R.drawable.ic_settings_gears, "Override Global Application Directives", 15f, 0.3f, "LIGHT", true, Color.DKGRAY, "DASH_SETTINGS"));
        registry.put("PEOPLE",         new TileRecord("PEOPLE",         "People\nMatrix", com.jdroidx.jfindme.R.drawable.ic_people, "Audit Connected Target Roster", 15f, 0.5f, "LIGHT", true, Color.MAGENTA, "DASH_PEOPLE"));
        
        // --- 🚀 QA Optimization Hub: Battery Pulse Matrix ---
        registry.put("BATTERY_1S",  new TileRecord("BATTERY_1S",  "1s\nPing",  android.R.drawable.ic_menu_rotate, "Max Tracking Intensity (-25%/day)", 20f, 0.8f, "HEAVY", true, Color.RED, "PULSE_1S"));
        registry.put("BATTERY_7S",  new TileRecord("BATTERY_7S",  "7s\nPing",  android.R.drawable.ic_menu_send,   "High Freq Tracking (-15%/day)", 15f, 0.6f, "LIGHT", true, Color.YELLOW, "PULSE_7S"));
        registry.put("BATTERY_15S", new TileRecord("BATTERY_15S", "15s\nPing", android.R.drawable.ic_menu_compass, "Smooth Tracking (-8%/day)", 15f, 0.5f, "LIGHT", true, Color.CYAN, "PULSE_15S"));
        registry.put("BATTERY_30S", new TileRecord("BATTERY_30S", "30s\nPing", android.R.drawable.ic_menu_mylocation,"Budget Tracking (-4%/day)", 15f, 0.4f, "LIGHT", true, Color.GREEN, "PULSE_30S"));
        registry.put("BATTERY_60S", new TileRecord("BATTERY_60S", "60s\nPing", android.R.drawable.ic_menu_view,      "Eco Tracking (-2%/day)", 15f, 0.3f, "LIGHT", true, Color.GRAY, "PULSE_60S"));
    }

    public static View createTile(BaseActivityJava activity, String tableKey, View.OnClickListener listener) {
        TileRecord record = registry.get(tableKey);
        if (record == null) return new View(activity); // Silent Guard

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        LinearLayout cell = new LinearLayout(activity);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setClickable(true);
        cell.setFocusable(true);
        cell.setOnClickListener(listener);
        cell.setContentDescription(record.talkBackDescription);
        
        // 🚀 QA Fix: Global Depth Restoration (Zero-Clip)
        cell.setClipChildren(false); 
        cell.setClipToPadding(false);

        // [Stage Architecture Interceptor]
        int baseBoxSize = UiArchitect.getResponsiveIconSize(activity, "ICON_STANDARD");
        int iconInternalPadding = 40; 
        float labelSize = 11f;

        // 🚀 MASTER SCALING INTERCEPTOR: Single Source of Truth Override (L7 Matrix)
        iconInternalPadding = UiScalingMatrixTable.scalePadding(stage, 10);
        labelSize = UiArchitect.getResponsiveTextSize(activity, "TEXT_MINI") * 1.10f; // 🚀 +10% Scale Factor

        // Layout Parameters with Weights for Row Integrity
        LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        cell.setLayoutParams(cellParams);

        // Master Background Matrix
        FrameLayout iconBg = new FrameLayout(activity);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        
        int tintStart = activity.getGradientStart();
        int tintEnd = activity.getGradientEnd();
        
        if (record.usesDynamicTheme) {
            bg.setColor(activity.getCardColor());
            bg.setStroke(2, tintStart);
        } else {
            bg.setColor(record.fallbackColor);
        }
        
        iconBg.setBackground(bg);
        iconBg.setPadding(iconInternalPadding, iconInternalPadding, iconInternalPadding, iconInternalPadding);

        // 🚀 MASTER SCALING INTERCEPTOR: Single Source of Truth
        float responsiveElevation = UiScalingMatrixTable.scaleElevation(stage, record.elevation);
        iconBg.setElevation(responsiveElevation);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            iconBg.setOutlineSpotShadowColor(Color.argb(160, 0, 0, 0));
            iconBg.setOutlineAmbientShadowColor(Color.argb(160, 0, 0, 0));
        }
        iconBg.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View v, android.graphics.Outline outline) {
                outline.setOval(0, 0, v.getWidth(), v.getHeight());
                outline.setAlpha(1.0f);
            }
        });
        iconBg.setClipToOutline(true);

        ImageView icon = new ImageView(activity);
        icon.setImageResource(record.iconRes);
        icon.setColorFilter(activity.getGradientMid()); // 🚀 QA Chrominance Sync (Vibrant Mid Color)
        icon.setAlpha(1.0f); // Ensure zero transparency leak
        iconBg.addView(icon, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        cell.addView(iconBg, new LinearLayout.LayoutParams(baseBoxSize, baseBoxSize));

        TextView tv = new TextView(activity);
        tv.setText(record.label);
        tv.setTextColor(activity.getTextColor());
        tv.setTextSize(labelSize);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setMaxLines(2);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setPadding(0, UiScalingMatrixTable.scalePadding(stage, 3), 0, 0);
        cell.addView(tv);

        // [Stage 4: Haptic Interaction Matrix]
        if (record.interactionType.equals("LIGHT")) {
            cell.setOnLongClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); return true; });
        } else if (record.interactionType.equals("HEAVY")) {
            cell.setOnLongClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); return true; });
        }

        // 🚀 QA Fix: Global Focus State Loop (Enter / D-Pad Support)
        cell.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });

        return cell;
    }

    public static void setSelectionState(View cell, boolean isSelected, int themeColor) {
        if (cell == null) return; // Guard
        
        // 🚀 QA Fix: Global Selection Matrix Integration (L7)
        // We defer the physical border and color math to the UiArchitect to ensure 1:1 parity 
        // with the History and Map selection engines.
        if (cell.getContext() instanceof BaseActivityJava) {
            UiArchitect.applySelection((BaseActivityJava)cell.getContext(), cell, isSelected);
        }
    }
}
