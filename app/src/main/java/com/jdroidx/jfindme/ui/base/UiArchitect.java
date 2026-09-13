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

    public static View createButton(BaseActivityJava activity, String tableKeyOrText, View.OnClickListener listener) {
        if (ActionButtonTable.getRegistry().containsKey(tableKeyOrText)) {
            return ActionButtonTable.createButton(activity, tableKeyOrText, listener);
        }
        return createButton(activity, tableKeyOrText, ButtonStyle.PRIMARY, listener);
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

    // ============================================================================================
    // V-A. ENTERPRISE INTERACTIVE CONTROLS (Toggles, Switches, Flippers & Slide Notifications)
    // ============================================================================================

    public interface ToggleCallback extends OnToggleChangeListener {
    }

    public interface SwitchCallback {
        void onSwitchChanged(boolean isChecked);
    }

    public interface MultiConditionCallback {
        void onChoicesSelected(int[] selectedIndices, String[] selectedLabels);
    }

    public interface DropdownCallback {
        void onItemSelected(int index, String value);
    }

    /**
     * 🔘 DYNAMIC ON/OFF SWITCH WITH LABELS
     */
    public static View createSwitchWithLabel(BaseActivityJava activity, String label, String sublabel, 
                                             boolean initialChecked, SwitchCallback callback) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = activity.scale(12);
        row.setPadding(pad, pad, pad, pad);
        applyCard(activity, row, "CARD_STANDARD");

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvLabel = new TextView(activity);
        tvLabel.setText(label);
        tvLabel.setTextColor(activity.getTextColor());
        tvLabel.setTextSize(14f);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvLabel);

        if (sublabel != null && !sublabel.isEmpty()) {
            TextView tvSub = new TextView(activity);
            tvSub.setText(sublabel);
            tvSub.setTextColor(activity.getSecondaryTextColor());
            tvSub.setTextSize(11.5f);
            textCol.addView(tvSub);
        }
        row.addView(textCol);

        android.widget.Switch sw = new android.widget.Switch(activity);
        sw.setChecked(initialChecked);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (callback != null) callback.onSwitchChanged(isChecked);
        });
        row.addView(sw);

        row.setOnClickListener(v -> sw.toggle());
        return row;
    }

    /**
     * 📽️ ANIMATED VIEW FLIPPER
     */
    public static android.widget.ViewFlipper createAnimatedFlipper(Context context, View... views) {
        android.widget.ViewFlipper flipper = new android.widget.ViewFlipper(context);
        flipper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        android.view.animation.Animation inAnim = new android.view.animation.AlphaAnimation(0f, 1f);
        inAnim.setDuration(250);
        android.view.animation.Animation outAnim = new android.view.animation.AlphaAnimation(1f, 0f);
        outAnim.setDuration(250);

        flipper.setInAnimation(inAnim);
        flipper.setOutAnimation(outAnim);

        for (View v : views) {
            if (v != null) flipper.addView(v);
        }
        return flipper;
    }

    /**
     * 🔔 SLIDE-IN NOTIFICATION POPUP (Bottom-Right / Floating Docked Banner)
     */
    public static void showSlideNotification(BaseActivityJava activity, String title, String message, 
                                             int statusType, Runnable onAction) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        
        LinearLayout banner = new LinearLayout(activity);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        int pad = activity.scale(14);
        banner.setPadding(pad, pad, pad, pad);

        int accentColor = (statusType == 1) ? Color.parseColor("#22C55E") : (statusType == 2 ? Color.parseColor("#F59E0B") : Color.parseColor("#EF4444"));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(245, 15, 23, 42));
        bg.setCornerRadius(activity.scale(16));
        bg.setStroke(activity.scale(2), accentColor);
        banner.setBackground(bg);
        banner.setElevation(activity.scale(12));

        TextView tvIcon = new TextView(activity);
        tvIcon.setText(statusType == 1 ? "🟢" : (statusType == 2 ? "🟡" : "🔴"));
        tvIcon.setTextSize(16f);
        tvIcon.setPadding(0, 0, activity.scale(10), 0);
        banner.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(13.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvTitle);

        TextView tvMsg = new TextView(activity);
        tvMsg.setText(message);
        tvMsg.setTextColor(Color.parseColor("#94A3B8"));
        tvMsg.setTextSize(11.5f);
        textCol.addView(tvMsg);
        banner.addView(textCol);

        if (onAction != null) {
            TextView btn = new TextView(activity);
            btn.setText("VIEW");
            btn.setTextColor(accentColor);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setTextSize(12f);
            btn.setPadding(activity.scale(12), activity.scale(6), activity.scale(12), activity.scale(6));
            btn.setOnClickListener(v -> {
                decor.removeView(banner);
                onAction.run();
            });
            banner.addView(btn);
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        int m = activity.scale(16);
        lp.setMargins(m, 0, m, activity.scale(32));
        banner.setLayoutParams(lp);

        // Slide In Animation from Bottom-Right
        banner.setTranslationY(activity.scale(100));
        banner.setAlpha(0f);
        decor.addView(banner);

        banner.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();

        // Auto dismiss after 4 seconds
        banner.postDelayed(() -> {
            if (banner.getParent() != null) {
                banner.animate()
                    .translationY(activity.scale(100))
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> decor.removeView(banner))
                    .start();
            }
        }, 4000);
    }

    /**
     * 📋 INTERACTIVE DROPDOWN LIST BOX (With Dynamic Item Selection)
     */
    public static View createDropdownListBox(BaseActivityJava activity, String hint, java.util.List<String> items, 
                                             DropdownCallback callback) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        int padH = activity.scale(14);
        int padV = activity.scale(10);
        box.setPadding(padH, padV, padH, padV);
        applyCard(activity, box, "CARD_STANDARD");

        TextView tvSelected = new TextView(activity);
        tvSelected.setText(hint != null ? hint : "Select from list...");
        tvSelected.setTextColor(activity.getTextColor());
        tvSelected.setTextSize(13.5f);
        tvSelected.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        box.addView(tvSelected);

        TextView tvArrow = new TextView(activity);
        tvArrow.setText("▾");
        tvArrow.setTextColor(activity.getGradientStart());
        tvArrow.setTextSize(16f);
        tvArrow.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(tvArrow);

        box.setClickable(true);
        box.setFocusable(true);
        box.setOnClickListener(v -> {
            if (items == null || items.isEmpty()) {
                android.widget.Toast.makeText(activity, "No items available in list", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            String[] arr = items.toArray(new String[0]);
            showOptionsDialog(activity, hint != null ? hint : "Select Option", arr, -1, (index, label) -> {
                tvSelected.setText(label);
                if (callback != null) callback.onItemSelected(index, label);
            });
        });

        return box;
    }

    /**
     * ☑️ MULTI-CHOICE CONDITION DIALOG WITH POPUP OPTIONS
     */
    public static void showMultiConditionDialog(BaseActivityJava activity, String title, String[] choices, 
                                                 boolean[] initialChecked, MultiConditionCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || choices == null) return;

        boolean[] checkedItems = initialChecked != null ? initialChecked.clone() : new boolean[choices.length];

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : "Select Conditions");
        builder.setMultiChoiceItems(choices, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });
        builder.setPositiveButton("CONFIRM", (dialog, which) -> {
            java.util.List<Integer> selIdx = new java.util.ArrayList<>();
            java.util.List<String> selLbl = new java.util.ArrayList<>();
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    selIdx.add(i);
                    selLbl.add(choices[i]);
                }
            }
            int[] outIndices = new int[selIdx.size()];
            for (int i = 0; i < selIdx.size(); i++) outIndices[i] = selIdx.get(i);
            if (callback != null) callback.onChoicesSelected(outIndices, selLbl.toArray(new String[0]));
        });
        builder.setNegativeButton("CANCEL", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
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

    /**
     * Shows/hides error message below a validated text field.
     */
    public static void showFieldError(EditText editText, String errorMsg) {
        if (editText == null) return;
        
        // Find or create error TextView
        LinearLayout parent = (LinearLayout) editText.getParent();
        if (parent == null) return;
        
        TextView errorView = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView && ((TextView) child).getTag() != null 
                && "field_error".equals((String) ((TextView) child).getTag())) {
                errorView = (TextView) child;
                break;
            }
        }
        
        if (errorView == null) {
            errorView = new TextView(editText.getContext());
            errorView.setTag("field_error");
            errorView.setTextColor(Color.parseColor("#FF5252"));
            errorView.setTextSize(11f);
            errorView.setPadding(4, 4, 0, 0);
            errorView.setVisibility(View.GONE);
            parent.addView(errorView, 1); // Insert after EditText
        }
        
        if (errorMsg != null && !errorMsg.isEmpty()) {
            errorView.setText("⚠ " + errorMsg);
            errorView.setVisibility(View.VISIBLE);
            editText.setHintTextColor(Color.parseColor("#FF8A80"));
        } else {
            errorView.setVisibility(View.GONE);
            editText.setHintTextColor(Color.GRAY);
        }
    }

    /**
     * Validates common field types and returns error message (null if valid).
     */
    public static String validateField(String type, String value) {
        if (value == null) value = "";
        value = value.trim();
        
        switch (type) {
            case "email":
                if (value.isEmpty()) return "Email required";
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) 
                    return "Invalid email format";
                return null;
            case "phone":
                if (value.isEmpty()) return null; // Optional
                if (value.length() < 7) return "Phone too short";
                if (!value.matches("[+]?[0-9\\-\\s()]+")) return "Invalid phone format";
                return null;
            case "name":
                if (value.isEmpty()) return "Name required";
                if (value.length() < 2) return "Name too short";
                if (!value.matches("[a-zA-Z0-9\\s._'-]+")) return "Invalid characters";
                return null;
            case "number":
                if (value.isEmpty()) return "Required";
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return "Must be a number";
                }
                return null;
            default:
                return null;
        }
    }

    /**
     * Creates a TextWatcher for real-time field validation.
     */
    public static android.text.TextWatcher createValidationWatcher(
            EditText editText, String fieldType, String fieldName) {
        return new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String error = validateField(fieldType, s.toString());
                showFieldError(editText, error);
            }
        };
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
        void onConfirmed(boolean[] selectedStates, String[] selectedItems);
        default void onItemsSelected(boolean[] checkedItems) {
            onConfirmed(checkedItems, new String[0]);
        }
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
        boolean validate(String text);
        default boolean isValid(String input) { return validate(input); }
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

    // ============================================================================================
    // IX. ENTERPRISE RICH UI WIDGET SUITE (OOPS MVC/MVVM Component Engine)
    // ============================================================================================

    /**
     * 🔘 Tactile On/Off Switch Row with Dynamic Haptic & Visual Feedback
     */
    public static View createInteractiveSwitch(BaseActivityJava activity, String title, String subtitle, 
                                               boolean initialChecked, android.widget.CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = activity.scale(12);
        row.setPadding(pad, pad, pad, pad);
        applyCard(activity, row, "CARD_STANDARD");

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(activity.getTextColor());
        tvTitle.setTextSize(getResponsiveTextSize(activity, "TEXT_TITLE"));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(activity);
            tvSub.setText(subtitle);
            tvSub.setTextColor(activity.getSecondaryTextColor());
            tvSub.setTextSize(getResponsiveTextSize(activity, "TEXT_SUB"));
            textCol.addView(tvSub);
        }
        row.addView(textCol);

        androidx.appcompat.widget.SwitchCompat switchCompat = new androidx.appcompat.widget.SwitchCompat(activity);
        switchCompat.setChecked(initialChecked);
        switchCompat.setOnCheckedChangeListener(listener);
        row.addView(switchCompat);

        return row;
    }

    /**
     * ↔️ Segmented Multi-Toggle Switcher (Left/Right/Tab selection)
     */
    public static View createSegmentedToggle(BaseActivityJava activity, String[] segments, int initialIndex, OptionSelectedCallback listener) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        int p = activity.scale(4);
        container.setPadding(p, p, p, p);

        boolean isDark = ThemeManager.isDarkTheme(activity.getResolvedThemeId());
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isDark ? Color.parseColor("#26FFFFFF") : Color.parseColor("#14000000"));
        bg.setCornerRadius(activity.scale(25));
        container.setBackground(bg);

        TextView[] tabViews = new TextView[segments.length];
        final int[] currentIndex = new int[]{initialIndex};

        for (int i = 0; i < segments.length; i++) {
            final int idx = i;
            TextView tab = new TextView(activity);
            tab.setText(segments[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(getResponsiveTextSize(activity, "BUTTON_TEXT"));
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            int vPad = activity.scale(8);
            int hPad = activity.scale(16);
            tab.setPadding(hPad, vPad, hPad, vPad);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tab.setLayoutParams(lp);

            tab.setOnClickListener(v -> {
                currentIndex[0] = idx;
                for (int j = 0; j < tabViews.length; j++) {
                    boolean isSel = (j == idx);
                    GradientDrawable tabBg = new GradientDrawable();
                    if (isSel) {
                        tabBg.setColor(activity.getGradientStart());
                        tabBg.setCornerRadius(activity.scale(20));
                        tabViews[j].setBackground(tabBg);
                        tabViews[j].setTextColor(Color.WHITE);
                    } else {
                        tabViews[j].setBackgroundColor(Color.TRANSPARENT);
                        tabViews[j].setTextColor(activity.getSecondaryTextColor());
                    }
                }
                if (listener != null) listener.onOptionSelected(idx, segments[idx]);
            });

            tabViews[i] = tab;
            container.addView(tab);
        }

        // Apply initial selection
        for (int j = 0; j < tabViews.length; j++) {
            boolean isSel = (j == initialIndex);
            GradientDrawable tabBg = new GradientDrawable();
            if (isSel) {
                tabBg.setColor(activity.getGradientStart());
                tabBg.setCornerRadius(activity.scale(20));
                tabViews[j].setBackground(tabBg);
                tabViews[j].setTextColor(Color.WHITE);
            } else {
                tabViews[j].setBackgroundColor(Color.TRANSPARENT);
                tabViews[j].setTextColor(activity.getSecondaryTextColor());
            }
        }

        return container;
    }

    /**
     * 📑 Dynamic Dropdown List Box with Selection Anchor
     */
    public static View createDropdownListBox(BaseActivityJava activity, String label, String[] items, int selectedIndex, OptionSelectedCallback listener) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);

        if (label != null && !label.isEmpty()) {
            TextView tvLabel = new TextView(activity);
            tvLabel.setText(label);
            tvLabel.setTextColor(activity.getSecondaryTextColor());
            tvLabel.setTextSize(getResponsiveTextSize(activity, "TEXT_LABEL"));
            tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
            tvLabel.setPadding(activity.scale(4), 0, 0, activity.scale(4));
            root.addView(tvLabel);
        }

        LinearLayout dropdownBtn = new LinearLayout(activity);
        dropdownBtn.setOrientation(LinearLayout.HORIZONTAL);
        dropdownBtn.setGravity(Gravity.CENTER_VERTICAL);
        int pad = activity.scale(12);
        dropdownBtn.setPadding(pad, pad, pad, pad);
        applyCard(activity, dropdownBtn, "CARD_STANDARD");

        TextView tvSelected = new TextView(activity);
        String selText = (selectedIndex >= 0 && selectedIndex < items.length) ? items[selectedIndex] : "Select Option...";
        tvSelected.setText(selText);
        tvSelected.setTextColor(activity.getTextColor());
        tvSelected.setTextSize(getResponsiveTextSize(activity, "TEXT_BODY"));
        dropdownBtn.addView(tvSelected, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        ImageView arrow = new ImageView(activity);
        arrow.setImageResource(android.R.drawable.arrow_down_float);
        arrow.setColorFilter(activity.getGradientStart());
        int is = activity.scale(16);
        dropdownBtn.addView(arrow, new LinearLayout.LayoutParams(is, is));

        dropdownBtn.setOnClickListener(v -> {
            showOptionsDialog(activity, label != null ? label : "Select Option", items, selectedIndex, (idx, optionText) -> {
                tvSelected.setText(optionText);
                if (listener != null) listener.onOptionSelected(idx, optionText);
            });
        });

        root.addView(dropdownBtn);
        return root;
    }

    /**
     * 🚀 Slide-In / Out Notification Banner from Bottom-Right
     */
    public static void showSlideNotification(BaseActivityJava activity, String title, String message, 
                                             int iconRes, int accentColor, String actionText, Runnable onAction) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        FrameLayout banner = new FrameLayout(activity);

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int pad = activity.scale(14);
        card.setPadding(pad, pad, pad, pad);

        boolean isDark = ThemeManager.isDarkTheme(activity.getResolvedThemeId());
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isDark ? Color.parseColor("#E61E293B") : Color.parseColor("#F2FFFFFF"));
        bg.setCornerRadius(activity.scale(16));
        bg.setStroke(activity.scale(2), accentColor != 0 ? accentColor : activity.getGradientStart());
        card.setBackground(bg);

        if (iconRes != 0) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(iconRes);
            iv.setColorFilter(accentColor != 0 ? accentColor : activity.getGradientStart());
            int is = activity.scale(24);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(is, is);
            ivLp.setMargins(0, 0, activity.scale(12), 0);
            card.addView(iv, ivLp);
        }

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(activity.getTextColor());
        tvTitle.setTextSize(getResponsiveTextSize(activity, "TEXT_TITLE"));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvTitle);

        TextView tvMsg = new TextView(activity);
        tvMsg.setText(message);
        tvMsg.setTextColor(activity.getSecondaryTextColor());
        tvMsg.setTextSize(getResponsiveTextSize(activity, "TEXT_SUB"));
        textCol.addView(tvMsg);
        card.addView(textCol);

        if (actionText != null && !actionText.isEmpty()) {
            TextView btn = new TextView(activity);
            btn.setText(actionText);
            btn.setTextColor(accentColor != 0 ? accentColor : activity.getGradientStart());
            btn.setTextSize(getResponsiveTextSize(activity, "BUTTON_TEXT"));
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setPadding(activity.scale(10), activity.scale(6), activity.scale(10), activity.scale(6));
            btn.setOnClickListener(v -> {
                decor.removeView(banner);
                if (onAction != null) onAction.run();
            });
            card.addView(btn);
        }

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        flp.gravity = Gravity.BOTTOM | Gravity.END;
        int m = activity.scale(16);
        flp.setMargins(m, 0, m, m + activity.scale(40));
        banner.addView(card, flp);

        // Slide-in translation animation
        card.setTranslationY(activity.scale(120));
        card.setAlpha(0f);
        card.animate().translationY(0f).alpha(1f).setDuration(350).start();

        decor.addView(banner);

        // Auto-dismiss after 4 seconds
        card.postDelayed(() -> {
            try {
                card.animate().translationY(activity.scale(120)).alpha(0f).setDuration(300).withEndAction(() -> {
                    decor.removeView(banner);
                }).start();
            } catch (Exception ignored) {}
        }, 4000);
    }

    // ============================================================================================
    // X. STANDARDIZED ENTERPRISE UI CONTROLS SUITE (Agent 3 Protocol)
    // ============================================================================================

    public interface OnToggleChangeListener {
        void onToggle(boolean isLeft);
    }

    public interface OnSwitchChangeListener {
        void onCheckChanged(boolean isChecked);
    }

    // 1. BUTTON SUITE (Primary, Secondary, Destructive, Outlined, Icon)
    public static View createPrimaryButton(BaseActivityJava activity, String text, int iconRes, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(activity);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        int hPad = activity.scale(20);
        int vPad = activity.scale(12);
        btn.setPadding(hPad, vPad, hPad, vPad);
        btn.setClickable(true);
        btn.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColors(new int[]{activity.getGradientStart(), activity.getGradientEnd()});
        bg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        bg.setCornerRadius(activity.scale(25));
        btn.setBackground(bg);
        btn.setElevation(activity.scale(8));

        if (iconRes != 0) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(iconRes);
            iv.setColorFilter(Color.WHITE);
            int is = activity.scale(18);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(is, is);
            ivLp.setMargins(0, 0, activity.scale(8), 0);
            btn.addView(iv, ivLp);
        }

        TextView tv = new TextView(activity);
        tv.setText(text != null ? text.toUpperCase() : "");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(getResponsiveTextSize(activity, "BUTTON_TEXT"));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        btn.addView(tv);

        if (listener != null) btn.setOnClickListener(listener);
        return btn;
    }

    public static View createSecondaryButton(BaseActivityJava activity, String text, int iconRes, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(activity);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        int hPad = activity.scale(18);
        int vPad = activity.scale(12);
        btn.setPadding(hPad, vPad, hPad, vPad);
        btn.setClickable(true);
        btn.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(activity.getCardColor());
        bg.setStroke(activity.scale(2), activity.getGradientStart());
        bg.setCornerRadius(activity.scale(25));
        btn.setBackground(bg);
        btn.setElevation(activity.scale(4));

        if (iconRes != 0) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(iconRes);
            iv.setColorFilter(activity.getGradientStart());
            int is = activity.scale(18);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(is, is);
            ivLp.setMargins(0, 0, activity.scale(8), 0);
            btn.addView(iv, ivLp);
        }

        TextView tv = new TextView(activity);
        tv.setText(text != null ? text.toUpperCase() : "");
        tv.setTextColor(activity.getTextColor());
        tv.setTextSize(getResponsiveTextSize(activity, "BUTTON_TEXT"));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        btn.addView(tv);

        if (listener != null) btn.setOnClickListener(listener);
        return btn;
    }

    public static View createDestructiveButton(BaseActivityJava activity, String text, int iconRes, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(activity);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        int hPad = activity.scale(18);
        int vPad = activity.scale(12);
        btn.setPadding(hPad, vPad, hPad, vPad);
        btn.setClickable(true);
        btn.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#DC2626")); // Crimson Red
        bg.setCornerRadius(activity.scale(25));
        btn.setBackground(bg);
        btn.setElevation(activity.scale(6));

        if (iconRes != 0) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(iconRes);
            iv.setColorFilter(Color.WHITE);
            int is = activity.scale(18);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(is, is);
            ivLp.setMargins(0, 0, activity.scale(8), 0);
            btn.addView(iv, ivLp);
        }

        TextView tv = new TextView(activity);
        tv.setText(text != null ? text.toUpperCase() : "");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(getResponsiveTextSize(activity, "BUTTON_TEXT"));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        btn.addView(tv);

        if (listener != null) btn.setOnClickListener(listener);
        return btn;
    }

    public static View createOutlinedButton(BaseActivityJava activity, String text, int iconRes, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(activity);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        int hPad = activity.scale(16);
        int vPad = activity.scale(10);
        btn.setPadding(hPad, vPad, hPad, vPad);
        btn.setClickable(true);
        btn.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.TRANSPARENT);
        bg.setStroke(activity.scale(2), activity.getSecondaryTextColor());
        bg.setCornerRadius(activity.scale(25));
        btn.setBackground(bg);

        if (iconRes != 0) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(iconRes);
            iv.setColorFilter(activity.getSecondaryTextColor());
            int is = activity.scale(16);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(is, is);
            ivLp.setMargins(0, 0, activity.scale(6), 0);
            btn.addView(iv, ivLp);
        }

        TextView tv = new TextView(activity);
        tv.setText(text != null ? text : "");
        tv.setTextColor(activity.getSecondaryTextColor());
        tv.setTextSize(getResponsiveTextSize(activity, "BUTTON_TEXT"));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        btn.addView(tv);

        if (listener != null) btn.setOnClickListener(listener);
        return btn;
    }

    public static View createIconButton(BaseActivityJava activity, int iconRes, View.OnClickListener listener) {
        ImageView iv = new ImageView(activity);
        iv.setImageResource(iconRes);
        iv.setColorFilter(activity.getGradientStart());
        iv.setClickable(true);
        iv.setFocusable(true);

        int size = activity.scale(44);
        int pad = activity.scale(10);
        iv.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(activity.getCardColor());
        bg.setStroke(activity.scale(2), activity.getGradientStart());
        iv.setBackground(bg);
        iv.setElevation(activity.scale(6));

        iv.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        if (listener != null) iv.setOnClickListener(listener);
        return iv;
    }

    // 2. VALIDATED TEXTBOX (Label with red asterisk, clear button, dynamic error banner)
    public static View createValidatedTextBox(BaseActivityJava activity, String label, String hint, boolean isRequired, TextValidator validator) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, activity.scale(4), 0, activity.scale(8));

        // Label row
        LinearLayout labelRow = new LinearLayout(activity);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        labelRow.setPadding(activity.scale(4), 0, 0, activity.scale(4));

        TextView tvLabel = new TextView(activity);
        tvLabel.setText(label != null ? label : "");
        tvLabel.setTextColor(activity.getTextColor());
        tvLabel.setTextSize(getResponsiveTextSize(activity, "TEXT_LABEL"));
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        labelRow.addView(tvLabel);

        if (isRequired) {
            TextView tvStar = new TextView(activity);
            tvStar.setText(" *");
            tvStar.setTextColor(Color.parseColor("#EF4444"));
            tvStar.setTextSize(getResponsiveTextSize(activity, "TEXT_LABEL"));
            tvStar.setTypeface(Typeface.DEFAULT_BOLD);
            labelRow.addView(tvStar);
        }
        root.addView(labelRow);

        // Input container (Box + Clear button)
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        int pad = activity.scale(12);
        box.setPadding(pad, pad / 2, pad, pad / 2);

        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(activity.getCardColor());
        boxBg.setCornerRadius(activity.scale(14));
        boxBg.setStroke(activity.scale(1), Color.parseColor("#33888888"));
        box.setBackground(boxBg);

        EditText edt = new EditText(activity);
        edt.setHint(hint != null ? (hint + (isRequired ? " *" : "")) : "");
        edt.setHintTextColor(activity.getSecondaryTextColor());
        edt.setTextColor(activity.getTextColor());
        edt.setTextSize(getResponsiveTextSize(activity, "TEXT_BODY"));
        edt.setBackground(null);
        edt.setSingleLine(true);
        box.addView(edt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        ImageView btnClear = new ImageView(activity);
        btnClear.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClear.setColorFilter(activity.getSecondaryTextColor());
        int is = activity.scale(18);
        btnClear.setVisibility(View.GONE);
        box.addView(btnClear, new LinearLayout.LayoutParams(is, is));

        btnClear.setOnClickListener(v -> edt.setText(""));

        root.addView(box);

        // Error message banner (hidden by default)
        TextView tvError = new TextView(activity);
        tvError.setTextColor(Color.parseColor("#EF4444"));
        tvError.setTextSize(getResponsiveTextSize(activity, "TEXT_MINI"));
        tvError.setTypeface(Typeface.DEFAULT_BOLD);
        tvError.setPadding(activity.scale(6), activity.scale(4), 0, 0);
        tvError.setVisibility(View.GONE);
        root.addView(tvError);

        edt.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString();
                btnClear.setVisibility(str.length() > 0 ? View.VISIBLE : View.GONE);
                if (validator != null) {
                    boolean valid = validator.validate(str);
                    if (!valid) {
                        tvError.setText("⚠️ " + validator.getErrorMessage());
                        tvError.setVisibility(View.VISIBLE);
                        boxBg.setStroke(activity.scale(2), Color.parseColor("#EF4444"));
                    } else {
                        tvError.setVisibility(View.GONE);
                        boxBg.setStroke(activity.scale(1), Color.parseColor("#33888888"));
                    }
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        root.setTag(edt); // Allow calling view.getTag() to retrieve the EditText
        return root;
    }

    // 3. DYNAMIC ON/OFF SWITCH
    public static View createSwitch(BaseActivityJava activity, boolean initialChecked, OnSwitchChangeListener listener) {
        androidx.appcompat.widget.SwitchCompat sc = new androidx.appcompat.widget.SwitchCompat(activity);
        sc.setChecked(initialChecked);
        if (listener != null) {
            sc.setOnCheckedChangeListener((btn, isChecked) -> listener.onCheckChanged(isChecked));
        }
        return sc;
    }

    public static View createSwitch(BaseActivityJava activity, boolean initialChecked, 
                                    android.widget.CompoundButton.OnCheckedChangeListener listener) {
        androidx.appcompat.widget.SwitchCompat sc = new androidx.appcompat.widget.SwitchCompat(activity);
        sc.setChecked(initialChecked);
        if (listener != null) sc.setOnCheckedChangeListener(listener);
        return sc;
    }

    public static View createSwitch(BaseActivityJava activity, String label, boolean initialChecked, 
                                    android.widget.CompoundButton.OnCheckedChangeListener listener) {
        return createInteractiveSwitch(activity, label, null, initialChecked, listener);
    }

    // 4. SEGMENTED LEFT/RIGHT TOGGLE
    public static View createSegmentedToggle(BaseActivityJava activity, String leftOption, String rightOption, 
                                             boolean isLeftSelected, OnToggleChangeListener listener) {
        String[] segments = new String[]{leftOption, rightOption};
        return createSegmentedToggle(activity, segments, isLeftSelected ? 0 : 1, (idx, option) -> {
            if (listener != null) listener.onToggle(idx == 0);
        });
    }

    // 5. ANIMATED VIEW FLIPPER
    public static android.widget.ViewFlipper createAnimatedFlipper(BaseActivityJava activity, View... childViews) {
        android.widget.ViewFlipper flipper = new android.widget.ViewFlipper(activity);
        
        // Slide left animation programmatically
        android.view.animation.TranslateAnimation inLeft = new android.view.animation.TranslateAnimation(
            android.view.animation.Animation.RELATIVE_TO_PARENT, 1.0f,
            android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f,
            android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f,
            android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f
        );
        inLeft.setDuration(250);
        flipper.setInAnimation(inLeft);

        android.view.animation.TranslateAnimation outLeft = new android.view.animation.TranslateAnimation(
            android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f,
            android.view.animation.Animation.RELATIVE_TO_PARENT, -1.0f,
            android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f,
            android.view.animation.Animation.RELATIVE_TO_PARENT, 0.0f
        );
        outLeft.setDuration(250);
        flipper.setOutAnimation(outLeft);

        if (childViews != null) {
            for (View cv : childViews) {
                if (cv != null) flipper.addView(cv);
            }
        }
        return flipper;
    }

    // 6. DROPDOWN LIST BOX (ACCORDION WITH LIST DETAILS)
    public static View createExpandableListBox(BaseActivityJava activity, String title, String subtitle, 
                                               int iconRes, View detailContentView) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        applyCard(activity, container, "CARD_STANDARD");
        int pad = activity.scale(14);
        container.setPadding(pad, pad, pad, pad);

        // Header Row
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setClickable(true);
        headerRow.setFocusable(true);

        if (iconRes != 0) {
            ImageView iv = new ImageView(activity);
            iv.setImageResource(iconRes);
            iv.setColorFilter(activity.getGradientStart());
            int is = activity.scale(24);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(is, is);
            ivLp.setMargins(0, 0, activity.scale(12), 0);
            headerRow.addView(iv, ivLp);
        }

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(activity.getTextColor());
        tvTitle.setTextSize(getResponsiveTextSize(activity, "TEXT_TITLE"));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(activity);
            tvSub.setText(subtitle);
            tvSub.setTextColor(activity.getSecondaryTextColor());
            tvSub.setTextSize(getResponsiveTextSize(activity, "TEXT_SUB"));
            textCol.addView(tvSub);
        }
        headerRow.addView(textCol);

        ImageView chevron = new ImageView(activity);
        chevron.setImageResource(android.R.drawable.arrow_down_float);
        chevron.setColorFilter(activity.getGradientStart());
        int cs = activity.scale(18);
        headerRow.addView(chevron, new LinearLayout.LayoutParams(cs, cs));

        container.addView(headerRow);

        // Details Body Container
        if (detailContentView != null) {
            detailContentView.setVisibility(View.GONE);
            detailContentView.setPadding(0, activity.scale(10), 0, 0);
            container.addView(detailContentView);

            headerRow.setOnClickListener(v -> {
                boolean isExpanded = detailContentView.getVisibility() == View.VISIBLE;
                detailContentView.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                chevron.animate().rotation(isExpanded ? 0f : 180f).setDuration(200).start();
            });
        }

        return container;
    }

    // 7. DIALOG BOX WITH MULTIPLE OPTIONS (MULTI-CHOICE)
    public static void showMultiChoiceDialog(BaseActivityJava activity, String title, String[] options, 
                                             boolean[] initialChecked, MultiChoiceCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || options == null) return;

        boolean[] states = new boolean[options.length];
        if (initialChecked != null) {
            System.arraycopy(initialChecked, 0, states, 0, Math.min(initialChecked.length, states.length));
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : "Select Options");
        builder.setMultiChoiceItems(options, states, (dialog, which, isChecked) -> {
            states[which] = isChecked;
        });

        builder.setPositiveButton("CONFIRM", (dialog, which) -> {
            if (callback != null) {
                java.util.List<String> selected = new java.util.ArrayList<>();
                for (int i = 0; i < options.length; i++) {
                    if (states[i]) selected.add(options[i]);
                }
                callback.onConfirmed(states, selected.toArray(new String[0]));
            }
        });

        builder.setNegativeButton("CANCEL", null);

        builder.setNeutralButton("TOGGLE ALL", (dialog, which) -> {
            boolean allChecked = true;
            for (boolean b : states) if (!b) { allChecked = false; break; }
            for (int i = 0; i < states.length; i++) states[i] = !allChecked;
            showMultiChoiceDialog(activity, title, options, states, callback);
        });

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    // 8. SLIDE IN/OUT BOTTOM-RIGHT NOTIFICATION BANNER
    public static void showSlideNotificationBanner(BaseActivityJava activity, String title, String message, 
                                                   boolean isAlert, long durationMs, String actionText, Runnable actionCallback) {
        int accentColor = isAlert ? Color.parseColor("#EF4444") : activity.getGradientStart();
        int iconRes = isAlert ? android.R.drawable.ic_dialog_alert : android.R.drawable.ic_dialog_info;
        showSlideNotification(activity, title, message, iconRes, accentColor, actionText, actionCallback);
    }
}


