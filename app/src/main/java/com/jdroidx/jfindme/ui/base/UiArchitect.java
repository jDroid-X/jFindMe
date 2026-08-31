package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * 👑 GLOBAL UI ARCHITECT: Enterprise Design System (L7 Waterfall)
 * Manages the "5-Stage Universal Sizing" matrix through a logical top-down gravity model.
 */
public class UiArchitect {

    public enum DesignStage {
        LIBERAL, STANDARD, COMPACT, SLIM, ULTRA_SLIM
    }

    public static final DesignStage[] STAGES = DesignStage.values();

    // ============================================================================================
    // I. ARCHITECTURAL FOUNDATION (Stage Detection & Environmental Controls)
    // ============================================================================================

    public static DesignStage getStage(Context context) {
        // --- ARCHITECTURAL INTERCEPTOR: MANUAL OVERRIDE ---
        int override = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getInt("stage_override", -1);
        if (override != -1 && override < STAGES.length) {
            return STAGES[override];
        }

        // 🚀 L7 Global Default Hub: Hard-Syncing to COMPACT stage as per user requirement!
        return DesignStage.COMPACT;
    }

    // ============================================================================================
    // II. MASTER SCALING MATRIX (Viewport-to-Pixel Responsive Engines)
    // ============================================================================================

    public static float getResponsiveTextSize(Context context, String tier) {
        DesignStage stage = getStage(context);
        float base = 14f; // Modern Balanced Baseline
        boolean isHeader = false;
        switch (tier) {
            case "TEXT_HEADER": base = 17.5f; isHeader = true; break; // Modern sleek header
            case "TEXT_TITLE":  base = 15.0f; isHeader = true; break; // Section/Card title
            case "TEXT_SUB":    base = 12.0f; break; // Subtitles / Meta text (+10%)
            case "TEXT_BODY":   base = 13.5f; break; // Standard body
            case "TEXT_MINI":   base = 11.0f; break; // Status badge & Icon text (+10%)
            case "TEXT_LABEL":  base = 12.0f; isHeader = true; break; // Form labels
            case "BUTTON_TEXT": base = 13.0f; isHeader = true; break; // Action buttons
        }
        return UiScalingMatrixTable.scaleText(stage, base, isHeader);
    }

    public static float scaleText(Context context, float baseSize) {
        return UiScalingMatrixTable.scaleText(getStage(context), baseSize, false);
    }

    public static int getResponsiveIconSize(Context context, String tier) {
        DesignStage stage = getStage(context);
        int base = 120; // 🚀 L7 RECALIBRATED: 120px Universal Baseline
        switch (tier) {
            case "ICON_TINY":     base = 80;  break;
            case "ICON_SMALL":    base = 100; break;
            case "ICON_STANDARD": base = 120; break;
            case "AVATAR_STANDARD": base = 120; break; // 🚀 Unified Brand Standard Avatar Dimension
            case "ICON_LARGE":    base = 140; break;
            case "ICON_HERO":     base = 400; break;
        }
        return UiScalingMatrixTable.scaleIcon(stage, base);
    }

    /**
     * 👑 Universal Brand Avatar Sizing (Single Source of Truth across all screens)
     */
    public static int getUniversalAvatarSize(Context context) {
        return getResponsiveIconSize(context, "AVATAR_STANDARD");
    }

    // ============================================================================================
    // III. RELATIONAL TYPOGRAPHY ENGINE (Auto-calibration & Tier Mapping)
    // ============================================================================================

    public static void applyText(Context context, TextView tv, String tier) {
        DesignStage stage = getStage(context);
        float size = 14f;
        boolean isHeader = false;
        switch (tier) {
            case "TEXT_HEADER": size = 17.5f; isHeader = true; break;
            case "TEXT_TITLE":  size = 15.0f; isHeader = true; break;
            case "TEXT_BODY":   size = 13.5f; break;
            case "TEXT_SUB":    size = 11.5f; break;
            case "TEXT_MINI":   size = 10.0f; break;
            case "TEXT_LABEL":  size = 12.0f; isHeader = true; break;
            case "BUTTON_TEXT": size = 13.0f; isHeader = true; break;
            default:            size = 14.0f; break;
        }
        if (isHeader || tier.contains("HEADER") || tier.contains("TITLE") || tier.equals("BUTTON_TEXT")) {
            tv.setTypeface(null, Typeface.BOLD);
        } else {
            tv.setTypeface(null, Typeface.NORMAL);
        }
        tv.setTextSize(UiScalingMatrixTable.scaleText(stage, size, isHeader));
    }

    public static void calibrateText(Context context, TextView tv, String tier) {
        if (tv == null || context == null) return;
        float targetSize = getResponsiveTextSize(context, tier);
        tv.setTextSize(targetSize);
        if (tier.contains("HEADER") || tier.contains("TITLE")) {
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    // ============================================================================================
    // IV. GLOBAL GEOMETRY PHYSICS (Card Depth, Selection States, Shadow Logic)
    // ============================================================================================

    public static float getResolvedRadius(BaseActivityJava activity, float defaultRadius) {
        android.content.SharedPreferences prefs = activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE);
        boolean isOverrideEnabled = prefs.getBoolean("radius_override_enabled", false);
        float baseRadius = defaultRadius;

        if (isOverrideEnabled) {
            int radiusMode = prefs.getInt("radius_mode", 1);
            if (radiusMode == 0) baseRadius = 10f; // SHARP
            else if (radiusMode == 1) baseRadius = 30f; // MODERN
            else if (radiusMode == 2) baseRadius = 60f; // MAXIMUM
        }
        return UiScalingMatrixTable.scaleRadius(getStage(activity), baseRadius);
    }

    public static void applyNeutralShadow(BaseActivityJava activity, View view, float elevation) {
        view.setElevation(elevation);
        view.setTranslationZ(UiScalingMatrixTable.getTZ(getStage(activity), elevation));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int alpha = (int)(0.85f * 255); // 🚀 L7 Standard: 217 Alpha (85%)
            int standardShadowColor = Color.argb(alpha, 0, 0, 0); 
            view.setOutlineSpotShadowColor(standardShadowColor);
            view.setOutlineAmbientShadowColor(standardShadowColor);
        }
        final float radius = getResolvedRadius(activity, 25f);
        view.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View v, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
                outline.setAlpha(1.0f);
            }
        });
    }

    public static void applyCard(BaseActivityJava activity, View view, String tableKey) {
        EnterpriseCardTable.applyCard(activity, view, tableKey);
    }

    public static void applyCardWithOverride(BaseActivityJava activity, View view, String tableKey) {
        applyCardWithOverride(activity, view, tableKey, 1.0f);
    }

    public static void applyCardWithOverride(BaseActivityJava activity, View view, String tableKey, float elevationFactor) {
        applyCard(activity, view, tableKey);
        
        // 🚀 L7 Depth Hub: Dynamic elevation factor application
        EnterpriseCardTable.CardRecord record = EnterpriseCardTable.getRegistry().get(tableKey);
        if (record != null) {
            float baseElevation = record.defaultElevation;
            float targetElevation = UiScalingMatrixTable.scaleElevation(getStage(activity), baseElevation * elevationFactor);
            view.setElevation(targetElevation);
            view.setTranslationZ(UiScalingMatrixTable.getTZ(getStage(activity), targetElevation));
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                int glowColor = activity.getGradientStart();
                view.setOutlineAmbientShadowColor(glowColor);
                view.setOutlineSpotShadowColor(glowColor);
            }
            final float radius = getResolvedRadius(activity, record.baseRadius);
            view.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View v, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
                    outline.setAlpha(1.0f);
                }
            });
        }

        view.setClipToOutline(true);
    }

    public static void applySelection(BaseActivityJava activity, View view, boolean isSelected) {
        if (view == null) return;
        DesignStage stage = getStage(activity);
        int themeColor = activity.getGradientStart();
        android.graphics.drawable.Drawable background = view.getBackground();
        if (background instanceof android.graphics.drawable.GradientDrawable) {
            android.graphics.drawable.GradientDrawable gd = (android.graphics.drawable.GradientDrawable) background;
            if (isSelected) {
                int strokePx = (int) UiScalingMatrixTable.scaleSelectionStroke(stage);
                gd.setStroke(strokePx, themeColor); 
                gd.setColors(new int[]{ activity.getCardColor(), activity.getGradientEnd() });
                gd.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.BR_TL);
                view.setAlpha(1.0f);
            } else {
                // 🚀 QA Fix: Global Border Toggle (System Registry)
                boolean bordersEnabled = activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getBoolean("borders_enabled", true);
                float borderStroke = bordersEnabled ? UiScalingMatrixTable.scaleBorderStroke(stage) : 0f;
                gd.setStroke((int)borderStroke, themeColor);
                gd.setColor(activity.getCardColor());
                view.setAlpha(0.6f);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageView) {
                    ((ImageView)child).setColorFilter(isSelected ? activity.getGradientOverlayText() : activity.getGradientMid());
                    ((ImageView)child).setAlpha(1.0f);
                }
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    tv.setTextColor(isSelected ? activity.getGradientOverlayText() : activity.getSecondaryTextColor());
                    tv.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
                }
            }
        }
    }

    public static void applyThemedShadow(BaseActivityJava activity, View view, String presetKey) {
        SystemGeometryTable.applyShadow(activity, view, presetKey);
    }

    public static float getRadiusTier(BaseActivityJava activity, String tierKey) {
        return SystemGeometryTable.getRadius(activity, tierKey);
    }

    // ============================================================================================
    // V. ENTERPRISE COMPONENT REGISTRY (Nav, Headers, Buttons, Marquees, Tiles)
    // ============================================================================================

    public static void applyHeader(BaseActivityJava activity, LinearLayout header, String tableKey) {
        HeaderTable.applyHeader(activity, header, tableKey);
    }

    public static View createButton(BaseActivityJava activity, String tableKey, View.OnClickListener listener) {
        return ActionButtonTable.createButton(activity, tableKey, listener);
    }

    public static View createBottomNav(BaseActivityJava activity, String[] tableKeys, String activeKey) {
        return BottomNavTable.createNav(activity, tableKeys, activeKey);
    }

    public static View createTile(BaseActivityJava activity, String tableKey, View.OnClickListener listener) {
        return DashboardTileTable.createTile(activity, tableKey, listener);
    }

    public static void setTileSelection(View tile, boolean isSelected, int themeColor) {
        DashboardTileTable.setSelectionState(tile, isSelected, themeColor);
    }

    public static TextView createMarqueeBar(BaseActivityJava activity, String tableKey, String content) {
        return MarqueeTable.createBar(activity, tableKey, content);
    }

    public static View createStatusPill(BaseActivityJava activity, String tableKey) {
        return StatusPillTable.createPill(activity, tableKey);
    }

    public static View createListItem(BaseActivityJava activity, String tableKey, String main, String sub) {
        return ListItemTable.createRow(activity, tableKey, main, sub, null);
    }

    public static View createListItem(BaseActivityJava activity, String tableKey, String main, String sub, View rightAction) {
        return ListItemTable.createRow(activity, tableKey, main, sub, rightAction);
    }

    public static EditText createInputField(BaseActivityJava activity, String tableKey) {
        return FormInputTable.createField(activity, tableKey);
    }

    public static void applyThemedIcon(Context context, ImageView view, String iconKey) {
        IconTable.applyTheme(context, view, iconKey);
    }

    /**
     * 🗺️ L7 MAP STYLING ENGINE
     * Applies the authorized forensic theme (Night/Day/Default) to Google Maps.
     */
    public static void applyMapStyle(Context context, com.google.android.gms.maps.GoogleMap map) {
        if (map == null || context == null) return;
        try {
            int themeId = 0;
            if (context instanceof BaseActivityJava) {
                themeId = ((BaseActivityJava) context).getResolvedThemeId();
            } else {
                themeId = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getInt("theme_id", 0);
            }
            
            if (ThemeManager.isDarkTheme(themeId)) {
                int styleRes = context.getResources().getIdentifier("map_style_night", "raw", context.getPackageName());
                if (styleRes != 0) {
                    map.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(context, styleRes));
                }
            } else {
                // Restore clean daylight vector styling for Light / Default Mobile Themes
                map.setMapStyle(null);
            }
        } catch (Exception e) {
            com.jdroidx.jfindme.utils.SystemLogger.e("UiArchitect", "Forensic Map Rendering Blocked: " + e.getMessage());
        }
    }

    // ============================================================================================
    // VI. UNIVERSAL HITL DIALOG & VALIDATION ENGINE (OOPS MVC/MVVM)
    // ============================================================================================

    public interface OptionSelectedCallback {
        void onOptionSelected(int index, String option);
    }

    public interface InputPromptCallback {
        void onInputConfirmed(String value);
    }

    public static class OptionItem {
        public final String title;
        public final String description;
        public final int iconRes;

        public OptionItem(String title, String description, int iconRes) {
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
        }
    }

    public static void showConfirmationDialog(BaseActivityJava activity, String title, String message, 
                                            String posBtn, String negBtn, 
                                            Runnable onConfirm, Runnable onCancel) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : "Confirmation Required");
        builder.setMessage(message != null ? message : "Are you sure you want to proceed?");
        builder.setPositiveButton(posBtn != null ? posBtn : "CONFIRM", (dialog, which) -> {
            if (onConfirm != null) onConfirm.run();
        });
        builder.setNegativeButton(negBtn != null ? negBtn : "CANCEL", (dialog, which) -> {
            if (onCancel != null) onCancel.run();
        });
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    public static void showConditionActionDialog(BaseActivityJava activity, String title, String message, 
                                                 String posBtn, String negBtn, 
                                                 Runnable onPos, Runnable onNeg) {
        showConfirmationDialog(activity, title, message, posBtn, negBtn, onPos, onNeg);
    }

    public static void showRichOptionsDialog(BaseActivityJava activity, String title, OptionItem[] items, OptionSelectedCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || items == null) return;

        LinearLayout listContainer = new LinearLayout(activity);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.getResponsivePaddingQuota();
        listContainer.setPadding(pad, pad / 2, pad, pad / 2);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(activity)
            .setTitle(title != null ? title : "Select Action")
            .setView(listContainer)
            .setNegativeButton("CANCEL", null)
            .create();

        int textColor = activity.getTextColor();
        int subTextColor = activity.getSecondaryTextColor();
        int cardColor = activity.getCardColor();

        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            OptionItem item = items[i];

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(activity.scale(16), activity.scale(12), activity.scale(16), activity.scale(12));
            card.setClickable(true);
            card.setFocusable(true);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(cardColor);
            cardBg.setCornerRadius(20f);
            card.setBackground(cardBg);

            if (item.iconRes != 0) {
                ImageView icon = new ImageView(activity);
                icon.setImageResource(item.iconRes);
                icon.setColorFilter(activity.getGradientStart());
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(activity.scale(24), activity.scale(24));
                iconParams.rightMargin = activity.scale(14);
                card.addView(icon, iconParams);
            }

            LinearLayout textCol = new LinearLayout(activity);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvTitle = new TextView(activity);
            tvTitle.setText(item.title);
            tvTitle.setTextColor(textColor);
            tvTitle.setTextSize(14.5f);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            textCol.addView(tvTitle);

            if (item.description != null && !item.description.isEmpty()) {
                TextView tvDesc = new TextView(activity);
                tvDesc.setText(item.description);
                tvDesc.setTextColor(subTextColor);
                tvDesc.setTextSize(12f);
                textCol.addView(tvDesc);
            }

            card.addView(textCol);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = activity.scale(8);
            card.setLayoutParams(cardParams);

            card.setOnClickListener(v -> {
                dialog.dismiss();
                if (callback != null) {
                    callback.onOptionSelected(idx, item.title);
                }
            });

            listContainer.addView(card);
        }

        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    public static void showValidationDialog(BaseActivityJava activity, String title, String message) {
        showValidationDialog(activity, title, message, null);
    }

    public static void showValidationDialog(BaseActivityJava activity, String title, String message, Runnable onDismiss) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : "Validation Notice");
        builder.setMessage(message != null ? message : "Please check your inputs and try again.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            if (onDismiss != null) onDismiss.run();
        });
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    public static void showOptionsDialog(BaseActivityJava activity, String title, String[] options, 
                                        int selectedIndex, OptionSelectedCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || options == null) return;

        int textColor = activity.getGradientOverlayText();
        int itemPadHorizontal = activity.scale(16);
        int itemPadVertical = activity.scale(12);

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(
            activity, 
            android.R.layout.simple_list_item_1, 
            options
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                view.setBackgroundColor(Color.TRANSPARENT);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    tv.setTextColor(textColor);
                    tv.setTextSize(14.5f);
                    tv.setPadding(itemPadHorizontal, itemPadVertical, itemPadHorizontal, itemPadVertical);
                }
                return view;
            }
        };

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : "Select Option");
        builder.setAdapter(adapter, (dialog, which) -> {
            if (callback != null && which >= 0 && which < options.length) {
                callback.onOptionSelected(which, options[which]);
            }
        });
        builder.setNegativeButton("CANCEL", null);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    public static void showInputPromptDialog(BaseActivityJava activity, String title, String hint, 
                                             String initialValue, InputPromptCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.getResponsivePaddingQuota();
        container.setPadding(pad, pad / 2, pad, pad / 2);

        int overlayColor = activity.getGradientOverlayText();
        EditText et = new EditText(activity);
        et.setHint(hint != null ? hint : "Enter value...");
        et.setText(initialValue != null ? initialValue : "");
        et.setTextColor(overlayColor);
        et.setHintTextColor(overlayColor == Color.WHITE ? Color.parseColor("#99FFFFFF") : Color.GRAY);
        et.setPadding(30, 25, 30, 25);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(overlayColor == Color.WHITE ? Color.parseColor("#26FFFFFF") : Color.parseColor("#26000000"));
        bg.setCornerRadius(20f);
        bg.setStroke(2, overlayColor == Color.WHITE ? Color.parseColor("#4DFFFFFF") : Color.parseColor("#4D000000"));
        et.setBackground(bg);
        container.addView(et);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : "Input Required");
        builder.setView(container);
        builder.setPositiveButton("SUBMIT", (dialog, which) -> {
            String val = et.getText().toString().trim();
            if (callback != null) callback.onInputConfirmed(val);
        });
        builder.setNegativeButton("CANCEL", null);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    public static View createValidationBadge(Context context, String text, boolean isValid) {
        TextView badge = new TextView(context);
        badge.setText((isValid ? "✓ " : "⚠ ") + text);
        badge.setTextColor(isValid ? Color.parseColor("#00E676") : Color.parseColor("#FF5252"));
        badge.setTextSize(11f);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setPadding(15, 6, 15, 6);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isValid ? Color.parseColor("#1A00E676") : Color.parseColor("#1AFF5252"));
        gd.setCornerRadius(30f);
        gd.setStroke(1, isValid ? Color.parseColor("#00E676") : Color.parseColor("#FF5252"));
        badge.setBackground(gd);
        return badge;
    }

    /**
     * 📦 HITL EMPTY STATE CARD
     * Creates a reusable empty-state container with optional actionable CTA.
     */
    public static View createEmptyStateCard(BaseActivityJava activity, String title, String description, 
                                           int iconRes, String actionText, Runnable onAction) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        int pad = activity.getResponsivePaddingQuota();
        card.setPadding(pad, pad * 2, pad, pad * 2);
        applyCard(activity, card, "CARD_STANDARD");

        if (iconRes != 0) {
            ImageView icon = new ImageView(activity);
            icon.setImageResource(iconRes);
            icon.setColorFilter(activity.getGradientStart());
            int iconSize = getResponsiveIconSize(activity, "ICON_LARGE");
            card.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));
        }

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title != null ? title : "No Data Found");
        tvTitle.setTextColor(activity.getTextColor());
        tvTitle.setTextSize(scaleText(activity, 16f));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 20, 0, 10);
        card.addView(tvTitle);

        TextView tvDesc = new TextView(activity);
        tvDesc.setText(description != null ? description : "There are currently no items to display.");
        tvDesc.setTextColor(activity.getSecondaryTextColor());
        tvDesc.setTextSize(scaleText(activity, 13f));
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setPadding(20, 0, 20, actionText != null ? 25 : 0);
        card.addView(tvDesc);

        if (actionText != null && onAction != null) {
            TextView btnAction = new TextView(activity);
            btnAction.setText(actionText);
            btnAction.setTextColor(activity.getGradientOverlayText());
            btnAction.setTextSize(scaleText(activity, 14f));
            btnAction.setTypeface(null, Typeface.BOLD);
            btnAction.setGravity(Gravity.CENTER);
            btnAction.setPadding(40, 20, 40, 20);

            GradientDrawable btnBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{activity.getGradientStart(), activity.getGradientEnd()}
            );
            btnBg.setCornerRadius(30f);
            btnAction.setBackground(btnBg);
            btnAction.setElevation(10f);
            btnAction.setOnClickListener(v -> onAction.run());

            card.addView(btnAction);
        }

        return card;
    }

    /**
     * 🛡️ PERMISSION REQUIRED DIALOG
     */
    public static void showPermissionRequiredDialog(BaseActivityJava activity, String permissionTitle, 
                                                    String rationale, Runnable onOpenSettings) {
        showConfirmationDialog(activity, 
            permissionTitle != null ? permissionTitle : "Permission Required",
            rationale != null ? rationale : "This feature requires hardware permission to function correctly. Open settings to grant access?",
            "OPEN SETTINGS", "NOT NOW",
            onOpenSettings != null ? onOpenSettings : () -> {
                try {
                    android.content.Intent intent = new android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", activity.getPackageName(), null)
                    );
                    activity.startActivity(intent);
                } catch (Exception ignored) {}
            },
            null
        );
    }

    /**
     * 💾 UNSAVED CHANGES DIALOG
     */
    public static void showUnsavedChangesDialog(BaseActivityJava activity, Runnable onDiscard, Runnable onSave) {
        showConfirmationDialog(activity,
            "Unsaved Changes",
            "You have unsaved changes that will be lost. Would you like to save before exiting?",
            "SAVE CHANGES", "DISCARD",
            onSave, onDiscard
        );
    }

    // ============================================================================================
    // V. ENTERPRISE UI/UX COMPONENT SUITE (Buttons, Textboxes, List Details, Action Sheets)
    // ============================================================================================

    public enum ButtonStyle {
        PRIMARY, SECONDARY, ACCENT, DESTRUCTIVE, OUTLINED
    }

    public static View createButton(BaseActivityJava activity, String text, ButtonStyle style, View.OnClickListener listener) {
        TextView btn = new TextView(activity);
        btn.setText(text);
        btn.setGravity(Gravity.CENTER);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextSize(getResponsiveTextSize(activity, "BUTTON_TEXT"));

        int padH = activity.scale(16);
        int padV = activity.scale(10);
        btn.setPadding(padH, padV, padH, padV);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(getResolvedRadius(activity, 20f));

        switch (style) {
            case PRIMARY:
                bg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                bg.setColors(new int[]{activity.getGradientStart(), activity.getGradientEnd()});
                btn.setTextColor(activity.getGradientOverlayText());
                btn.setElevation(activity.scale(4));
                break;
            case SECONDARY:
                bg.setColor(Color.parseColor("#37474F"));
                btn.setTextColor(Color.WHITE);
                btn.setElevation(activity.scale(2));
                break;
            case ACCENT:
                bg.setColor(Color.parseColor("#00E5FF"));
                btn.setTextColor(Color.BLACK);
                btn.setElevation(activity.scale(3));
                break;
            case DESTRUCTIVE:
                bg.setColor(Color.parseColor("#D50000"));
                btn.setTextColor(Color.WHITE);
                btn.setElevation(activity.scale(3));
                break;
            case OUTLINED:
                bg.setColor(Color.TRANSPARENT);
                bg.setStroke(activity.scale(1), activity.getGradientStart());
                btn.setTextColor(activity.getGradientStart());
                break;
        }
        btn.setBackground(bg);
        if (listener != null) btn.setOnClickListener(listener);

        return btn;
    }

    public static View createCapsuleChipButton(BaseActivityJava activity, String text, int color, View.OnClickListener listener) {
        TextView chip = new TextView(activity);
        chip.setText(text);
        chip.setTextColor(Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setTextSize(getResponsiveTextSize(activity, "TEXT_MINI"));

        int padH = activity.scale(12);
        int padV = activity.scale(6);
        chip.setPadding(padH, padV, padH, padV);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(100f);
        chip.setBackground(bg);

        if (listener != null) chip.setOnClickListener(listener);
        return chip;
    }

    public static class ValidatedTextFieldResult {
        public LinearLayout container;
        public EditText editText;
        public TextView errorText;
        public TextView labelText;
    }

    public static ValidatedTextFieldResult createValidatedTextField(BaseActivityJava activity, String label, String hint, boolean isPassword, android.text.TextWatcher validator) {
        ValidatedTextFieldResult result = new ValidatedTextFieldResult();
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = activity.getResponsivePaddingQuota();
        box.setPadding(0, p / 3, 0, p / 3);

        int overlayColor = activity.getGradientOverlayText();
        int textColor = activity.getTextColor();

        TextView tvLabel = new TextView(activity);
        tvLabel.setText(label != null ? label : "");
        tvLabel.setTextColor(textColor);
        tvLabel.setTextSize(getResponsiveTextSize(activity, "TEXT_LABEL"));
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setPadding(0, 0, 0, activity.scale(4));
        box.addView(tvLabel);
        result.labelText = tvLabel;

        FrameLayout inputWrapper = new FrameLayout(activity);
        EditText et = new EditText(activity);
        et.setHint(hint != null ? hint : "");
        et.setTextColor(textColor);
        et.setHintTextColor(Color.GRAY);
        et.setTextSize(getResponsiveTextSize(activity, "TEXT_BODY"));
        et.setPadding(activity.scale(14), activity.scale(10), activity.scale(isPassword ? 40 : 14), activity.scale(10));

        if (isPassword) {
            et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(20, 128, 128, 128));
        bg.setCornerRadius(activity.scale(8));
        bg.setStroke(1, Color.argb(60, 128, 128, 128));
        et.setBackground(bg);
        inputWrapper.addView(et, new FrameLayout.LayoutParams(-1, -2));

        if (isPassword) {
            ImageView toggleEye = new ImageView(activity);
            toggleEye.setImageResource(android.R.drawable.ic_menu_view);
            toggleEye.setColorFilter(Color.GRAY);
            int iconSize = activity.scale(22);
            FrameLayout.LayoutParams eyeLp = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.END | Gravity.CENTER_VERTICAL);
            eyeLp.setMargins(0, 0, activity.scale(12), 0);
            toggleEye.setLayoutParams(eyeLp);

            final boolean[] isVisible = {false};
            toggleEye.setOnClickListener(v -> {
                isVisible[0] = !isVisible[0];
                if (isVisible[0]) {
                    et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    toggleEye.setColorFilter(activity.getGradientStart());
                } else {
                    et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    toggleEye.setColorFilter(Color.GRAY);
                }
                et.setSelection(et.getText().length());
            });
            inputWrapper.addView(toggleEye);
        }

        box.addView(inputWrapper);
        result.editText = et;

        TextView tvError = new TextView(activity);
        tvError.setTextColor(Color.parseColor("#FF5252"));
        tvError.setTextSize(getResponsiveTextSize(activity, "TEXT_MINI"));
        tvError.setVisibility(View.GONE);
        tvError.setPadding(activity.scale(4), activity.scale(2), 0, 0);
        box.addView(tvError);
        result.errorText = tvError;

        if (validator != null) {
            et.addTextChangedListener(validator);
        }

        result.container = box;
        return result;
    }

    public static View createDetailListItem(BaseActivityJava activity, String title, String subtitle, String statusBadge, 
                                            int iconRes, View.OnClickListener onClick, View.OnClickListener onMoreOptions) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int p = activity.getResponsivePaddingQuota();
        row.setPadding(p, p, p, p);
        applyCard(activity, row, "CARD_STANDARD");

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, activity.scale(8));
        row.setLayoutParams(rowLp);

        if (iconRes != 0) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(iconRes);
            iv.setColorFilter(activity.getGradientStart());
            int s = getResponsiveIconSize(activity, "ICON_STANDARD");
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(s, s);
            ivLp.setMargins(0, 0, activity.scale(12), 0);
            row.addView(iv, ivLp);
        }

        LinearLayout info = new LinearLayout(activity);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        info.setLayoutParams(infoLp);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title != null ? title : "");
        tvTitle.setTextColor(activity.getTextColor());
        tvTitle.setTextSize(getResponsiveTextSize(activity, "TEXT_TITLE"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        info.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(activity);
            tvSub.setText(subtitle);
            tvSub.setTextColor(activity.getSecondaryTextColor());
            tvSub.setTextSize(getResponsiveTextSize(activity, "TEXT_SUB"));
            info.addView(tvSub);
        }
        row.addView(info);

        if (statusBadge != null && !statusBadge.isEmpty()) {
            TextView badge = new TextView(activity);
            badge.setText(statusBadge);
            badge.setTextColor(activity.getGradientStart());
            badge.setTextSize(getResponsiveTextSize(activity, "TEXT_MINI"));
            badge.setTypeface(null, Typeface.BOLD);
            badge.setPadding(activity.scale(8), activity.scale(3), activity.scale(8), activity.scale(3));

            GradientDrawable bbg = new GradientDrawable();
            bbg.setColor(Color.argb(30, 0, 229, 255));
            bbg.setCornerRadius(activity.scale(12));
            badge.setBackground(bbg);
            row.addView(badge);
        }

        if (onMoreOptions != null) {
            ImageView ivMore = new ImageView(activity);
            ivMore.setImageResource(android.R.drawable.ic_menu_more);
            ivMore.setColorFilter(activity.getSecondaryTextColor());
            int ms = activity.scale(24);
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ms, ms);
            mlp.setMargins(activity.scale(8), 0, 0, 0);
            ivMore.setLayoutParams(mlp);
            ivMore.setOnClickListener(onMoreOptions);
            row.addView(ivMore);
        }

        if (onClick != null) {
            row.setOnClickListener(onClick);
        }

        return row;
    }

    public static class ActionOption {
        public String title;
        public String subtitle;
        public int iconRes;
        public Runnable action;

        public ActionOption(String title, String subtitle, int iconRes, Runnable action) {
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
            this.action = action;
        }
    }

    public static void showActionSheetDialog(BaseActivityJava activity, String title, java.util.List<ActionOption> options) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || options == null || options.isEmpty()) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        if (title != null) builder.setTitle(title);

        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.getResponsivePaddingQuota();
        container.setPadding(pad, pad / 2, pad, pad / 2);

        final android.app.AlertDialog[] dialogHolder = new android.app.AlertDialog[1];

        for (ActionOption opt : options) {
            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(activity.scale(12), activity.scale(10), activity.scale(12), activity.scale(10));

            if (opt.iconRes != 0) {
                ImageView icon = new ImageView(activity);
                icon.setImageResource(opt.iconRes);
                icon.setColorFilter(activity.getGradientStart());
                int is = activity.scale(24);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(is, is);
                ilp.setMargins(0, 0, activity.scale(12), 0);
                item.addView(icon, ilp);
            }

            LinearLayout textCol = new LinearLayout(activity);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView tvTitle = new TextView(activity);
            tvTitle.setText(opt.title != null ? opt.title : "");
            tvTitle.setTextColor(activity.getTextColor());
            tvTitle.setTextSize(getResponsiveTextSize(activity, "TEXT_TITLE"));
            tvTitle.setTypeface(null, Typeface.BOLD);
            textCol.addView(tvTitle);

            if (opt.subtitle != null && !opt.subtitle.isEmpty()) {
                TextView tvSub = new TextView(activity);
                tvSub.setText(opt.subtitle);
                tvSub.setTextColor(activity.getSecondaryTextColor());
                tvSub.setTextSize(getResponsiveTextSize(activity, "TEXT_SUB"));
                textCol.addView(tvSub);
            }
            item.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1.0f));

            item.setOnClickListener(v -> {
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                if (opt.action != null) opt.action.run();
            });

            container.addView(item);
        }

        builder.setView(container);
        builder.setNegativeButton("DISMISS", null);
        android.app.AlertDialog dialog = builder.create();
        dialogHolder[0] = dialog;
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    public static void showActionableValidationDialog(BaseActivityJava activity, String title, String message, 
                                                      String actionBtn, String dismissBtn, 
                                                      Runnable onAction, Runnable onDismiss) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : "Validation Notice");
        builder.setMessage(message != null ? message : "Please verify the information provided.");

        if (actionBtn != null) {
            builder.setPositiveButton(actionBtn, (dialog, which) -> {
                if (onAction != null) onAction.run();
            });
        }
        builder.setNegativeButton(dismissBtn != null ? dismissBtn : "DISMISS", (dialog, which) -> {
            if (onDismiss != null) onDismiss.run();
        });

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    public interface MultiChoiceCallback {
        void onItemsSelected(boolean[] checkedItems);
    }

    public static void showMultiChoiceOptionsDialog(BaseActivityJava activity, String title, String[] items, 
                                                    boolean[] checkedItems, MultiChoiceCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || items == null) return;

        final boolean[] localChecked = checkedItems != null ? checkedItems.clone() : new boolean[items.length];

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : "Select Items");
        builder.setMultiChoiceItems(items, localChecked, (dialog, which, isChecked) -> {
            if (which >= 0 && which < localChecked.length) {
                localChecked[which] = isChecked;
            }
        });
        builder.setPositiveButton("CONFIRM", (dialog, which) -> {
            if (callback != null) callback.onItemsSelected(localChecked);
        });
        builder.setNegativeButton("CANCEL", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    // ============================================================================================
    // IX. ENTERPRISE INTERACTIVE COMPONENTS: Validated Textboxes, Detail List Boxes & Option Dialogs
    // ============================================================================================

    public static class DetailListItem {
        public String title;
        public String subtitle;
        public String detailText;
        public String statusBadge;
        public int iconRes;
        public Runnable onClickAction;

        public DetailListItem(String title, String subtitle, String detailText, String statusBadge, int iconRes, Runnable onClickAction) {
            this.title = title;
            this.subtitle = subtitle;
            this.detailText = detailText;
            this.statusBadge = statusBadge;
            this.iconRes = iconRes;
            this.onClickAction = onClickAction;
        }
    }

    public static View createInteractiveDetailListBox(BaseActivityJava activity, String headerTitle, java.util.List<DetailListItem> items) {
        if (activity == null || items == null) return new View(activity);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.getResponsivePaddingQuota();
        box.setPadding(pad, pad / 2, pad, pad / 2);

        android.graphics.drawable.GradientDrawable boxBg = new android.graphics.drawable.GradientDrawable();
        boxBg.setCornerRadius(getResolvedRadius(activity, 16f));
        boxBg.setColor(activity.getCardColor());
        boxBg.setStroke(activity.scale(2), activity.getGradientMid());
        box.setBackground(boxBg);

        if (headerTitle != null && !headerTitle.isEmpty()) {
            TextView tvHeader = new TextView(activity);
            tvHeader.setText(headerTitle);
            tvHeader.setTextColor(activity.getGradientStart());
            tvHeader.setTypeface(null, Typeface.BOLD);
            tvHeader.setTextSize(getResponsiveTextSize(activity, "TEXT_TITLE"));
            tvHeader.setPadding(activity.scale(8), activity.scale(8), activity.scale(8), activity.scale(12));
            box.addView(tvHeader);
        }

        for (DetailListItem item : items) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int rPad = activity.scale(10);
            row.setPadding(rPad, rPad, rPad, rPad);
            row.setClickable(true);
            row.setFocusable(true);

            android.graphics.drawable.GradientDrawable rowBg = new android.graphics.drawable.GradientDrawable();
            rowBg.setCornerRadius(getResolvedRadius(activity, 10f));
            rowBg.setColor(activity.getCardColor());
            rowBg.setStroke(activity.scale(1), activity.getGradientMid());
            row.setBackground(rowBg);

            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.setMargins(0, activity.scale(4), 0, activity.scale(4));
            row.setLayoutParams(rLp);

            if (item.iconRes != 0) {
                ImageView iv = new ImageView(activity);
                iv.setImageResource(item.iconRes);
                iv.setColorFilter(activity.getGradientStart());
                int is = activity.scale(26);
                LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(is, is);
                ivLp.setMargins(0, 0, activity.scale(10), 0);
                row.addView(iv, ivLp);
            }

            LinearLayout col = new LinearLayout(activity);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);

            TextView tvT = new TextView(activity);
            tvT.setText(item.title != null ? item.title : "");
            tvT.setTextColor(activity.getTextColor());
            tvT.setTextSize(getResponsiveTextSize(activity, "TEXT_LABEL"));
            tvT.setTypeface(null, Typeface.BOLD);
            col.addView(tvT);

            if (item.subtitle != null && !item.subtitle.isEmpty()) {
                TextView tvS = new TextView(activity);
                tvS.setText(item.subtitle);
                tvS.setTextColor(activity.getSecondaryTextColor());
                tvS.setTextSize(getResponsiveTextSize(activity, "TEXT_MINI"));
                col.addView(tvS);
            }

            if (item.detailText != null && !item.detailText.isEmpty()) {
                TextView tvD = new TextView(activity);
                tvD.setText(item.detailText);
                tvD.setTextColor(activity.getSecondaryTextColor());
                tvD.setTextSize(getResponsiveTextSize(activity, "TEXT_MINI"));
                col.addView(tvD);
            }
            row.addView(col, colLp);

            if (item.statusBadge != null && !item.statusBadge.isEmpty()) {
                TextView tvBadge = new TextView(activity);
                tvBadge.setText(item.statusBadge);
                tvBadge.setTextColor(Color.WHITE);
                tvBadge.setTextSize(getResponsiveTextSize(activity, "TEXT_MINI"));
                tvBadge.setTypeface(null, Typeface.BOLD);
                tvBadge.setPadding(activity.scale(8), activity.scale(3), activity.scale(8), activity.scale(3));

                android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
                badgeBg.setCornerRadius(activity.scale(12));
                badgeBg.setColor(activity.getGradientMid());
                tvBadge.setBackground(badgeBg);
                row.addView(tvBadge);
            }

            if (item.onClickAction != null) {
                row.setOnClickListener(v -> item.onClickAction.run());
            }

            box.addView(row);
        }

        return box;
    }

    public interface TextValidator {
        boolean isValid(String input);
        String getErrorMessage();
    }

    public static class ValidatedInputComponent {
        public View container;
        public EditText editText;
        public TextView errorText;
        public boolean validate() { return true; }
    }

    public static ValidatedInputComponent createValidatedInputBox(BaseActivityJava activity, String label, String hint, int iconRes, TextValidator validator) {
        ValidatedInputComponent comp = new ValidatedInputComponent();
        if (activity == null) return comp;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.getResponsivePaddingQuota() / 2;
        root.setPadding(0, pad, 0, pad);

        if (label != null && !label.isEmpty()) {
            TextView tvLabel = new TextView(activity);
            tvLabel.setText(label);
            tvLabel.setTextColor(activity.getTextColor());
            tvLabel.setTextSize(getResponsiveTextSize(activity, "TEXT_LABEL"));
            tvLabel.setTypeface(null, Typeface.BOLD);
            tvLabel.setPadding(0, 0, 0, activity.scale(6));
            root.addView(tvLabel);
        }

        LinearLayout inputWrapper = new LinearLayout(activity);
        inputWrapper.setOrientation(LinearLayout.HORIZONTAL);
        inputWrapper.setGravity(Gravity.CENTER_VERTICAL);

        android.graphics.drawable.GradientDrawable normalBorder = new android.graphics.drawable.GradientDrawable();
        normalBorder.setCornerRadius(getResolvedRadius(activity, 12f));
        normalBorder.setColor(activity.getCardColor());
        normalBorder.setStroke(activity.scale(2), activity.getGradientMid());
        inputWrapper.setBackground(normalBorder);

        int inPad = activity.scale(12);
        inputWrapper.setPadding(inPad, activity.scale(4), inPad, activity.scale(4));

        if (iconRes != 0) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(iconRes);
            iv.setColorFilter(activity.getGradientStart());
            int is = activity.scale(22);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(is, is);
            ivLp.setMargins(0, 0, activity.scale(8), 0);
            inputWrapper.addView(iv, ivLp);
        }

        EditText et = new EditText(activity);
        et.setHint(hint != null ? hint : "");
        et.setHintTextColor(activity.getSecondaryTextColor());
        et.setTextColor(activity.getTextColor());
        et.setTextSize(getResponsiveTextSize(activity, "TEXT_BODY"));
        et.setBackgroundColor(Color.TRANSPARENT);
        inputWrapper.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        root.addView(inputWrapper);

        TextView tvErr = new TextView(activity);
        tvErr.setTextColor(Color.parseColor("#E53935"));
        tvErr.setTextSize(getResponsiveTextSize(activity, "TEXT_MINI"));
        tvErr.setVisibility(View.GONE);
        tvErr.setPadding(activity.scale(4), activity.scale(3), 0, 0);
        root.addView(tvErr);

        comp.container = root;
        comp.editText = et;
        comp.errorText = tvErr;

        if (validator != null) {
            et.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    boolean ok = validator.isValid(s.toString());
                    if (!ok && s.length() > 0) {
                        tvErr.setText(validator.getErrorMessage());
                        tvErr.setVisibility(View.VISIBLE);
                        normalBorder.setStroke(activity.scale(2), Color.parseColor("#E53935"));
                    } else {
                        tvErr.setVisibility(View.GONE);
                        normalBorder.setStroke(activity.scale(2), activity.getGradientMid());
                    }
                }
            });
        }

        return comp;
    }

    public static void showEnhancedOptionDialog(BaseActivityJava activity, String title, String message, 
                                                String posBtn, String negBtn, String neuBtn, 
                                                Runnable onPos, Runnable onNeg, Runnable onNeu) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        if (title != null) builder.setTitle(title);
        if (message != null) builder.setMessage(message);

        if (posBtn != null) {
            builder.setPositiveButton(posBtn, (d, w) -> { if (onPos != null) onPos.run(); });
        }
        if (negBtn != null) {
            builder.setNegativeButton(negBtn, (d, w) -> { if (onNeg != null) onNeg.run(); });
        }
        if (neuBtn != null) {
            builder.setNeutralButton(neuBtn, (d, w) -> { if (onNeu != null) onNeu.run(); });
        }

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    public interface OnOptionSelectListener {
        void onOptionSelected(int index, String optionText);
    }

    public static void showPopupNotificationWithOptions(BaseActivityJava activity, String title, String message, 
                                                         String[] options, OnOptionSelectListener listener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || options == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        if (title != null) builder.setTitle(title);
        if (message != null) builder.setMessage(message);

        builder.setItems(options, (dialog, which) -> {
            if (listener != null && which >= 0 && which < options.length) {
                listener.onOptionSelected(which, options[which]);
            }
        });
        builder.setNegativeButton("DISMISS", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }
}

