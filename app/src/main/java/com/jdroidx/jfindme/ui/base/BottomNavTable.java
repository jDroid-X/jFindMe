package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.GradientDrawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 BottomNavTable: Enterprise Configuration Registry
 * Manages footer navigation tabs and session state indicators.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class BottomNavTable {

    public static class NavRecord {
        public String id;
        public String label;
        public int iconRes;
        public String talkBackDescription;
        public boolean dynamicBehavior;
        public int fallbackColor;
        public float elevation;
        public String hapticType;
        public String clickAction;
        public boolean isVisible;

        public NavRecord(String id, String label, int iconRes, String talkBackDescription, 
                         boolean dynamicBehavior, int fallbackColor, float elevation, 
                         String hapticType, String clickAction, boolean isVisible) {
            this.id = id;
            this.label = label;
            this.iconRes = iconRes;
            this.talkBackDescription = talkBackDescription;
            this.dynamicBehavior = dynamicBehavior;
            this.fallbackColor = fallbackColor;
            this.elevation = elevation;
            this.hapticType = hapticType;
            this.clickAction = clickAction;
            this.isVisible = isVisible;
        }
    }

    private static final Map<String, NavRecord> registry = new HashMap<>();
    public static Map<String, NavRecord> getRegistry() { return registry; }

    static {
        registry.put("NAV_NOTICE", new NavRecord("NAV_NOTICE", "Notice",  com.jdroidx.jfindme.R.drawable.ic_nav_notice, "Alerts Matrix", true, Color.GRAY, 25f, "LIGHT", "OPEN_NOTIF", true));
        registry.put("NAV_ALERT",  new NavRecord("NAV_ALERT",  "Alert",   android.R.drawable.ic_dialog_alert, "System Alerts", true, Color.YELLOW, 25f, "LIGHT", "OPEN_ALERT", true));
        registry.put("NAV_DEVICE", new NavRecord("NAV_DEVICE", "Map",     com.jdroidx.jfindme.R.drawable.ic_nav_map, "Active Nodes", true, Color.BLUE, 25f, "HEAVY", "OPEN_MAP", true));
        registry.put("NAV_ME",     new NavRecord("NAV_ME",     "Profile", com.jdroidx.jfindme.R.drawable.ic_nav_profile, "Identity Hub", true, Color.MAGENTA, 25f, "LIGHT", "OPEN_PROFILE", true));

        // --- 🛰️ L7 Profile Hub Navigation (Synchronized) ---
        registry.put("NAV_PINGS",   new NavRecord("NAV_PINGS",   "Pings",    com.jdroidx.jfindme.R.drawable.ic_nav_notice, "Notifications Hub", true, Color.GRAY, 25f, "LIGHT", "OPEN_NOTIF", true));
        registry.put("NAV_MAP",     new NavRecord("NAV_MAP",     "Map Hub",  com.jdroidx.jfindme.R.drawable.ic_nav_map, "Geospatial View", true, Color.BLUE, 25f, "HEAVY", "OPEN_MAP", true));
        registry.put("NAV_PROFILE", new NavRecord("NAV_PROFILE", "Profile",  com.jdroidx.jfindme.R.drawable.ic_nav_profile, "Identity Matrix", true, Color.MAGENTA, 25f, "LIGHT", "OPEN_PROFILE", true));
    }

    public static View createNav(BaseActivityJava activity, String[] tableKeys, String activeKey) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        LinearLayout nav = new LinearLayout(activity);
        nav.setId(View.generateViewId()); // Or a fixed ID if shared
        nav.setTag("nav_bar_root"); // Tag is safer for finding across activities
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        
        int height = 126; // Default (180 * 0.7)
        switch (stage) {
            case LIBERAL:   height = 210; break; // 300 * 0.7
            case STANDARD:  height = 168; break; // 240 * 0.7
            case COMPACT:   height = 140; break; // 200 * 0.7
            case SLIM:      height = 119; break; // 170 * 0.7
            case ULTRA_SLIM:height = 105; break; // 150 * 0.7
        }
        
        nav.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        EnterpriseCardTable.applyCard(activity, nav, "CARD_STANDARD");
        UiArchitect.applyNeutralShadow(activity, nav, UiScalingMatrixTable.scaleElevation(stage, 25f));
        
        // 🚀 QA Fix: Global Footer Override (Use Gradient End Color)
        android.graphics.drawable.Drawable bg = nav.getBackground();
        if (bg instanceof android.graphics.drawable.GradientDrawable) {
            ((android.graphics.drawable.GradientDrawable)bg).setColor(activity.getNavCardColor());
        }
        
        nav.setPadding(20, 0, 20, 0); // QA Fix: Overriding global card padding to prevent icon/text clipping
        
        // Ensure no clipping for the new Icon Shadows
        nav.setClipChildren(false);
        nav.setClipToPadding(false);
        
        for (String key : tableKeys) {
            NavRecord record = registry.get(key);
            if (record != null && record.isVisible) {
                nav.addView(createTab(activity, record, key.equals(activeKey)));
            }
        }
        return nav;
    }

    private static View createTab(BaseActivityJava activity, NavRecord record, boolean isActive) {
        LinearLayout tab = new LinearLayout(activity);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);
        tab.setClipChildren(false); // No shadow clipping
        tab.setClipToPadding(false);
        tab.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
        tab.setContentDescription(record.talkBackDescription);

        // 🚀 QA Fix: Global Focus State Loop (Enter / D-Pad Support)
        tab.setFocusable(true);
        tab.setClickable(true);

        int baseIconSize = UiArchitect.getResponsiveIconSize(activity, "ICON_TINY");
        int iconSize = isActive ? (int)(baseIconSize * 1.15f) : baseIconSize;

        ImageView icon = new ImageView(activity);
        icon.setImageResource(record.iconRes);
        
        // 🚀 QA Fix: Force Extreme Contrast for Active/Inactive Transitions
        // Inactive states now follow the Theme's Secondary Text Color for perfect visual harmony.
        int color = isActive ? activity.getGradientMid() : activity.getSecondaryTextColor();
        
        if (activity.getResolvedThemeId() == 7 || activity.getResolvedThemeId() == 3) {
            color = isActive ? Color.BLACK : Color.parseColor("#424242");
        }
        
        // 🚀 Enterprise Hardening: Force mutation to prevent resource sharing side-effects
        if (icon.getDrawable() != null) {
            android.graphics.drawable.Drawable mutateDrawable = icon.getDrawable().mutate();
            mutateDrawable.setColorFilter(new android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN));
            icon.setImageDrawable(mutateDrawable);
        } else {
            icon.setColorFilter(color); // Fallback for raw resources
        }
        
        // Apply Global L7 Shadow to Navigation Iconography
        float elevation = 8f;
        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        switch (stage) {
            case LIBERAL:   elevation = 16f; break;
            case STANDARD:  elevation = 13f; break;
            case COMPACT:   elevation = 11f; break;
            case SLIM:      elevation = 9f; break;
            case ULTRA_SLIM:elevation = 8f; break;
        }
        UiArchitect.applyNeutralShadow(activity, icon, elevation);
        
        // 🚀 QA Fix: Global Badge Integration (WhatsApp Style)
        FrameLayout iconWrapper = new FrameLayout(activity);
        iconWrapper.setClipChildren(false);
        iconWrapper.setClipToPadding(false);
        
        iconWrapper.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
        
        TextView badge = new TextView(activity);
        badge.setTag("badge_" + record.id);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(9f);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.RED);
        badge.setBackground(bg);
        
        int bP = (int) UiArchitect.getResponsiveTextSize(activity, "TEXT_MINI") / 2;
        badge.setPadding(bP, 2, bP, 2);
        badge.setVisibility(View.GONE);
        
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.gravity = Gravity.TOP | Gravity.END;
        iconWrapper.addView(badge, badgeLp);
        
        tab.addView(iconWrapper, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tv = new TextView(activity);
        tv.setText(record.label.toUpperCase());
        tv.setTextColor(color);
        tv.setAlpha(isActive ? 1.0f : 0.7f);
        UiArchitect.applyText(activity, tv, "TEXT_SUB");
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, stage == UiArchitect.DesignStage.LIBERAL ? 20 : 10, 0, 0);
        tv.setVisibility(View.GONE); // 🚀 QA Fix: Suppressing Navigation Labels for Max Viewport space
        tab.addView(tv);

        // [Stage 4: Haptic & Action Routing]
        tab.setOnClickListener(v -> {
            if (record.hapticType.equals("HEAVY")) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            } else if (record.hapticType.equals("LIGHT")) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            }
            handleAction(activity, record.clickAction);
        });

        tab.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performClick();
                return true;
            }
            return false;
        });

        return tab;
    }

    public static void updateBadge(View navRoot, String key, int count) {
        if (navRoot == null || key == null) return;
        View b = navRoot.findViewWithTag("badge_" + key);
        if (b instanceof TextView) {
            TextView tv = (TextView) b;
            if (count > 0) {
                tv.setText(String.valueOf(count));
                tv.setVisibility(View.VISIBLE);
            } else {
                tv.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Centralized Navigation Controller for the Enterprise Bottom Matrix
     */
    private static void handleAction(BaseActivityJava activity, String action) {
        if (action == null) return;
        
        switch (action) {
            case "OPEN_NOTIF":
            case "OPEN_EMAIL": // Legacy support
                if (!activity.getClass().getSimpleName().contains("NotifActivity")) {
                    activity.startActivity(new android.content.Intent(activity, com.jdroidx.jfindme.ui.management.NotifActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                }
                break;
            case "OPEN_ALERT":
                // 🚨 HIGH-PRIORITY PANIC / ACOUSTIC SIREN TRIGGER
                activity.startActivity(new android.content.Intent(activity, com.jdroidx.jfindme.ui.alert.AcousticSirenActivity.class)
                    .putExtra("MODE", "SOS")
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case "OPEN_MAP":
                if (activity.getClass().getSimpleName().contains("MapActivity")) {
                    // Central Map Trigger
                } else {
                    activity.startActivity(new android.content.Intent(activity, com.jdroidx.jfindme.ui.map.MapActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                }
                break;
            case "OPEN_PROFILE":
                if (!activity.getClass().getSimpleName().contains("ProfileActivity")) {
                    activity.startActivity(new android.content.Intent(activity, com.jdroidx.jfindme.ui.settings.ProfileActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                }
                break;
        }
    }
}
