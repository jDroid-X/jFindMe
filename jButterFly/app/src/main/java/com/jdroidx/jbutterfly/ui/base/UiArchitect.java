package com.jdroidx.jbutterfly.ui.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import androidx.appcompat.widget.SwitchCompat;
import com.jdroidx.jbutterfly.R;
import java.util.ArrayList;
import java.util.List;

/**
 * 👑 UiArchitect — Master Responsive Enterprise UI Component Engine.
 * Implements complete standard UI/UX features:
 * - Buttons (Primary, Secondary, Accent, Destructive, Outlined, Icon)
 * - Validated Textbox with floating labels, required indicator, and error banners
 * - Dynamic On/Off Switch
 * - Segmented Left/Right Toggle
 * - Animated View Flipper
 * - List Box with list details (Dropdown / Accordion)
 * - Dialog box with multiple options (Multi-choice & Single-choice)
 * - Slide-in/out notification banner from right-bottom with actions
 */
public class UiArchitect {

    // ─── INTERFACES ───────────────────────────────────────────────────

    public interface InputValidator {
        boolean validate(String input);
        String getErrorMessage();
    }

    public interface OnToggleChangeListener {
        void onToggle(boolean isLeft);
    }

    public interface DropdownCallback {
        void onItemSelected(int index, String item);
    }

    public interface MultiChoiceCallback {
        void onConfirmed(boolean[] checkedStates, String[] selectedItems);
    }

    public interface SingleChoiceCallback {
        void onSelected(int index, String item);
    }

    // ─── RESPONSIVE SCALING ENGINE ────────────────────────────────────

    public static float getDeviceScaleFactor(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float widthDp = dm.widthPixels / dm.density;
        if (widthDp >= 720) return 1.35f; // Large tablet (10"+)
        if (widthDp >= 600) return 1.20f; // 7" Tablet / Foldable
        return 1.0f; // Standard smartphone
    }

    public static int dpToPx(Context context, int dp) {
        float scale = getDeviceScaleFactor(context);
        return Math.round(dp * context.getResources().getDisplayMetrics().density * scale);
    }

    public static float spToPx(Context context, float sp) {
        float scale = getDeviceScaleFactor(context);
        return sp * scale;
    }

    // ─── 1. BUTTONS (PRIMARY, SECONDARY, ACCENT, DESTRUCTIVE, OUTLINED, ICON) ───

    public static Button createPrimaryButton(Context context, String text, View.OnClickListener onClick) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(spToPx(context, 13f));
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dpToPx(context, 20), dpToPx(context, 12), dpToPx(context, 20), dpToPx(context, 12));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getBrandColor(context));
        bg.setCornerRadius(dpToPx(context, 10));
        btn.setBackground(bg);

        if (onClick != null) btn.setOnClickListener(onClick);
        return btn;
    }

    public static Button createSecondaryButton(Context context, String text, View.OnClickListener onClick) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(ThemeManager.getPrimaryTextColor(context));
        btn.setTextSize(spToPx(context, 13f));
        btn.setPadding(dpToPx(context, 16), dpToPx(context, 10), dpToPx(context, 16), dpToPx(context, 10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getCardColor(context));
        bg.setStroke(dpToPx(context, 1), ThemeManager.getBorderColor(context));
        bg.setCornerRadius(dpToPx(context, 8));
        btn.setBackground(bg);

        if (onClick != null) btn.setOnClickListener(onClick);
        return btn;
    }

    public static Button createAccentButton(Context context, String text, View.OnClickListener onClick) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(spToPx(context, 13f));
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dpToPx(context, 18), dpToPx(context, 10), dpToPx(context, 18), dpToPx(context, 10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#8B5CF6")); // Purple Accent
        bg.setCornerRadius(dpToPx(context, 8));
        btn.setBackground(bg);

        if (onClick != null) btn.setOnClickListener(onClick);
        return btn;
    }

    public static Button createDestructiveButton(Context context, String text, View.OnClickListener onClick) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(spToPx(context, 13f));
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dpToPx(context, 16), dpToPx(context, 10), dpToPx(context, 16), dpToPx(context, 10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#DC2626")); // Crimson
        bg.setCornerRadius(dpToPx(context, 8));
        btn.setBackground(bg);

        if (onClick != null) btn.setOnClickListener(onClick);
        return btn;
    }

    public static Button createOutlinedButton(Context context, String text, View.OnClickListener onClick) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(ThemeManager.getBrandColor(context));
        btn.setTextSize(spToPx(context, 13f));
        btn.setPadding(dpToPx(context, 16), dpToPx(context, 10), dpToPx(context, 16), dpToPx(context, 10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.TRANSPARENT);
        bg.setStroke(dpToPx(context, 2), ThemeManager.getBrandColor(context));
        bg.setCornerRadius(dpToPx(context, 8));
        btn.setBackground(bg);

        if (onClick != null) btn.setOnClickListener(onClick);
        return btn;
    }

    public static ImageView createIconButton(Context context, int iconRes, View.OnClickListener onClick) {
        ImageView iv = new ImageView(context);
        iv.setImageResource(iconRes);
        iv.setColorFilter(ThemeManager.getPrimaryTextColor(context));
        int pad = dpToPx(context, 10);
        iv.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getCardColor(context));
        bg.setStroke(dpToPx(context, 1), ThemeManager.getBorderColor(context));
        bg.setCornerRadius(dpToPx(context, 8));
        iv.setBackground(bg);

        if (onClick != null) iv.setOnClickListener(onClick);
        return iv;
    }

    // ─── 2. VALIDATED TEXTBOX WITH FLOATING LABELS & CONDITIONS ───────

    public static EditText createInputField(Context context, String hint, int inputType) {
        EditText et = new EditText(context);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextSize(spToPx(context, 14f));
        et.setTextColor(ThemeManager.getPrimaryTextColor(context));
        et.setHintTextColor(ThemeManager.getSecondaryTextColor(context));
        et.setPadding(dpToPx(context, 16), dpToPx(context, 14), dpToPx(context, 16), dpToPx(context, 14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(context));
        bg.setStroke(dpToPx(context, 1), ThemeManager.getBorderColor(context));
        bg.setCornerRadius(dpToPx(context, 8));
        et.setBackground(bg);

        return et;
    }

    public static View createValidatedInputField(
        Context context,
        String label,
        String hint,
        boolean isRequired,
        InputValidator validator
    ) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        // Label Row
        LinearLayout labelRow = new LinearLayout(context);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvLabel = new TextView(context);
        tvLabel.setText(label);
        tvLabel.setTextColor(ThemeManager.getPrimaryTextColor(context));
        tvLabel.setTextSize(spToPx(context, 12f));
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        labelRow.addView(tvLabel);

        if (isRequired) {
            TextView tvStar = new TextView(context);
            tvStar.setText(" *");
            tvStar.setTextColor(Color.parseColor("#EF4444"));
            tvStar.setTextSize(spToPx(context, 12f));
            tvStar.setTypeface(Typeface.DEFAULT_BOLD);
            labelRow.addView(tvStar);
        }
        container.addView(labelRow);

        // Input Box + Clear "X" Button
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dpToPx(context, 12);
        box.setPadding(pad, pad / 2, pad, pad / 2);

        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(ThemeManager.getSurfaceColor(context));
        boxBg.setCornerRadius(dpToPx(context, 8));
        boxBg.setStroke(dpToPx(context, 1), ThemeManager.getBorderColor(context));
        box.setBackground(boxBg);

        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        boxLp.topMargin = dpToPx(context, 6);
        box.setLayoutParams(boxLp);

        EditText edt = new EditText(context);
        edt.setHint(hint);
        edt.setHintTextColor(ThemeManager.getSecondaryTextColor(context));
        edt.setTextColor(ThemeManager.getPrimaryTextColor(context));
        edt.setTextSize(spToPx(context, 13f));
        edt.setBackground(null);
        edt.setSingleLine(true);
        box.addView(edt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        ImageView btnClear = new ImageView(context);
        btnClear.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClear.setColorFilter(ThemeManager.getSecondaryTextColor(context));
        int is = dpToPx(context, 18);
        btnClear.setVisibility(View.GONE);
        box.addView(btnClear, new LinearLayout.LayoutParams(is, is));

        btnClear.setOnClickListener(v -> edt.setText(""));
        container.addView(box);

        // Error message banner
        TextView tvError = new TextView(context);
        tvError.setTextColor(Color.parseColor("#EF4444"));
        tvError.setTextSize(spToPx(context, 11f));
        tvError.setTypeface(Typeface.DEFAULT_BOLD);
        tvError.setPadding(dpToPx(context, 6), dpToPx(context, 4), 0, 0);
        tvError.setVisibility(View.GONE);
        container.addView(tvError);

        edt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString();
                btnClear.setVisibility(str.length() > 0 ? View.VISIBLE : View.GONE);
                if (validator != null) {
                    boolean valid = validator.validate(str);
                    if (!valid) {
                        tvError.setText("⚠️ " + validator.getErrorMessage());
                        tvError.setVisibility(View.VISIBLE);
                        boxBg.setStroke(dpToPx(context, 2), Color.parseColor("#EF4444"));
                    } else {
                        tvError.setVisibility(View.GONE);
                        boxBg.setStroke(dpToPx(context, 1), ThemeManager.getBorderColor(context));
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        container.setTag(edt);
        return container;
    }

    // ─── 3. DYNAMIC ON/OFF SWITCH ─────────────────────────────────────

    public static View createSwitch(
        Context context,
        String title,
        String subtitle,
        boolean initialChecked,
        CompoundButton.OnCheckedChangeListener listener
    ) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dpToPx(context, 14);
        row.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(context));
        bg.setStroke(dpToPx(context, 1), ThemeManager.getBorderColor(context));
        bg.setCornerRadius(dpToPx(context, 10));
        row.setBackground(bg);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(textLp);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeManager.getPrimaryTextColor(context));
        tvTitle.setTextSize(spToPx(context, 13f));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(context);
            tvSub.setText(subtitle);
            tvSub.setTextColor(ThemeManager.getSecondaryTextColor(context));
            tvSub.setTextSize(spToPx(context, 11f));
            textCol.addView(tvSub);
        }
        row.addView(textCol);

        SwitchCompat sc = new SwitchCompat(context);
        sc.setChecked(initialChecked);
        if (listener != null) sc.setOnCheckedChangeListener(listener);
        row.addView(sc);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowLp.bottomMargin = dpToPx(context, 10);
        row.setLayoutParams(rowLp);

        return row;
    }

    // ─── 4. SEGMENTED LEFT/RIGHT TOGGLE ───────────────────────────────

    public static View createSegmentedToggle(
        Context context,
        String leftOption,
        String rightOption,
        boolean isLeftSelected,
        OnToggleChangeListener listener
    ) {
        LinearLayout toggleBox = new LinearLayout(context);
        toggleBox.setOrientation(LinearLayout.HORIZONTAL);
        int pad = dpToPx(context, 4);
        toggleBox.setPadding(pad, pad, pad, pad);

        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(ThemeManager.getCardColor(context));
        boxBg.setStroke(dpToPx(context, 1), ThemeManager.getBorderColor(context));
        boxBg.setCornerRadius(dpToPx(context, 12));
        toggleBox.setBackground(boxBg);

        Button btnLeft = new Button(context);
        btnLeft.setText(leftOption);
        btnLeft.setTextSize(spToPx(context, 12f));

        Button btnRight = new Button(context);
        btnRight.setText(rightOption);
        btnRight.setTextSize(spToPx(context, 12f));

        final boolean[] leftState = {isLeftSelected};

        Runnable updateState = () -> {
            boolean left = leftState[0];
            GradientDrawable activeBg = new GradientDrawable();
            activeBg.setColor(ThemeManager.getBrandColor(context));
            activeBg.setCornerRadius(dpToPx(context, 8));

            GradientDrawable inactiveBg = new GradientDrawable();
            inactiveBg.setColor(Color.TRANSPARENT);

            btnLeft.setBackground(left ? activeBg : inactiveBg);
            btnLeft.setTextColor(left ? Color.WHITE : ThemeManager.getSecondaryTextColor(context));
            btnLeft.setTypeface(left ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

            btnRight.setBackground(!left ? activeBg : inactiveBg);
            btnRight.setTextColor(!left ? Color.WHITE : ThemeManager.getSecondaryTextColor(context));
            btnRight.setTypeface(!left ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        };

        btnLeft.setOnClickListener(v -> {
            leftState[0] = true;
            updateState.run();
            if (listener != null) listener.onToggle(true);
        });

        btnRight.setOnClickListener(v -> {
            leftState[0] = false;
            updateState.run();
            if (listener != null) listener.onToggle(false);
        });

        updateState.run();

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        toggleBox.addView(btnLeft, btnLp);
        toggleBox.addView(btnRight, btnLp);

        return toggleBox;
    }

    // ─── 5. ANIMATED VIEW FLIPPER ─────────────────────────────────────

    public static ViewFlipper createAnimatedFlipper(Context context, View... childViews) {
        ViewFlipper flipper = new ViewFlipper(context);

        TranslateAnimation inLeft = new TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f
        );
        inLeft.setDuration(250);
        flipper.setInAnimation(inLeft);

        TranslateAnimation outLeft = new TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, -1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f
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

    // ─── 6. DROPDOWN LIST BOX (ACCORDION & EXPANDABLE DETAILS) ────────

    public static View createExpandableListBox(
        Context context,
        String title,
        String subtitle,
        int iconRes,
        View detailContentView
    ) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(context, 14);
        container.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(context));
        bg.setStroke(dpToPx(context, 1), ThemeManager.getBorderColor(context));
        bg.setCornerRadius(dpToPx(context, 12));
        container.setBackground(bg);

        // Header Row
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setClickable(true);
        headerRow.setFocusable(true);

        if (iconRes != 0) {
            ImageView iv = new ImageView(context);
            iv.setImageResource(iconRes);
            iv.setColorFilter(ThemeManager.getBrandColor(context));
            int is = dpToPx(context, 24);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(is, is);
            ivLp.setMargins(0, 0, dpToPx(context, 12), 0);
            headerRow.addView(iv, ivLp);
        }

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeManager.getPrimaryTextColor(context));
        tvTitle.setTextSize(spToPx(context, 13.5f));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(context);
            tvSub.setText(subtitle);
            tvSub.setTextColor(ThemeManager.getSecondaryTextColor(context));
            tvSub.setTextSize(spToPx(context, 11f));
            textCol.addView(tvSub);
        }
        headerRow.addView(textCol);

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(android.R.drawable.arrow_down_float);
        chevron.setColorFilter(ThemeManager.getBrandColor(context));
        int cs = dpToPx(context, 18);
        headerRow.addView(chevron, new LinearLayout.LayoutParams(cs, cs));

        container.addView(headerRow);

        // Details Body Container
        if (detailContentView != null) {
            detailContentView.setVisibility(View.GONE);
            detailContentView.setPadding(0, dpToPx(context, 10), 0, 0);
            container.addView(detailContentView);

            headerRow.setOnClickListener(v -> {
                boolean isExpanded = detailContentView.getVisibility() == View.VISIBLE;
                detailContentView.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                chevron.animate().rotation(isExpanded ? 0f : 180f).setDuration(200).start();
            });
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dpToPx(context, 10);
        container.setLayoutParams(lp);

        return container;
    }

    public static View createDropdownListBox(
        Context context,
        String label,
        List<String> items,
        int selectedIndex,
        DropdownCallback callback
    ) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextColor(ThemeManager.getPrimaryTextColor(context));
        tv.setTextSize(spToPx(context, 12f));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        container.addView(tv);

        Spinner spinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            spinner.setSelection(selectedIndex);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (callback != null) callback.onItemSelected(position, items.get(position));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        spLp.topMargin = dpToPx(context, 6);
        spinner.setLayoutParams(spLp);
        container.addView(spinner);

        return container;
    }

    // ─── 7. MULTI-CHOICE & SINGLE-CHOICE DIALOG BOXES ─────────────────

    public static void showMultiChoiceDialog(
        Activity activity,
        String title,
        String[] options,
        boolean[] initialChecked,
        MultiChoiceCallback callback
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || options == null) return;

        boolean[] states = new boolean[options.length];
        if (initialChecked != null) {
            System.arraycopy(initialChecked, 0, states, 0, Math.min(initialChecked.length, states.length));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : "Select Options");
        builder.setMultiChoiceItems(options, states, (dialog, which, isChecked) -> {
            states[which] = isChecked;
        });

        builder.setPositiveButton("CONFIRM", (dialog, which) -> {
            if (callback != null) {
                List<String> selected = new ArrayList<>();
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

        builder.show();
    }

    public static void showSingleChoiceDialog(
        Activity activity,
        String title,
        String[] options,
        int selectedIndex,
        SingleChoiceCallback callback
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || options == null) return;

        final int[] chosen = {selectedIndex};
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setSingleChoiceItems(options, selectedIndex, (dialog, which) -> chosen[0] = which);
        builder.setPositiveButton("SELECT", (dialog, which) -> {
            if (callback != null && chosen[0] >= 0 && chosen[0] < options.length) {
                callback.onSelected(chosen[0], options[chosen[0]]);
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    // ─── 8. SLIDE-IN/OUT BOTTOM-RIGHT NOTIFICATION BANNER ─────────────

    public static void showSlideNotificationBanner(
        Activity activity,
        String title,
        String message,
        boolean isAlert,
        long durationMs,
        String actionText,
        Runnable actionCallback
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();

        LinearLayout banner = new LinearLayout(activity);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dpToPx(activity, 14);
        banner.setPadding(pad, pad, pad, pad);

        int accentColor = isAlert ? Color.parseColor("#EF4444") : Color.parseColor("#22C55E");

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(245, 15, 23, 42)); // Dark Navy Slate
        bg.setCornerRadius(dpToPx(activity, 14));
        bg.setStroke(dpToPx(activity, 2), accentColor);
        banner.setBackground(bg);
        banner.setElevation(dpToPx(activity, 10));

        TextView tvIcon = new TextView(activity);
        tvIcon.setText(isAlert ? "🚨" : "🔔");
        tvIcon.setTextSize(spToPx(activity, 16f));
        tvIcon.setPadding(0, 0, dpToPx(activity, 10), 0);
        banner.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(spToPx(activity, 13f));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvTitle);

        TextView tvMsg = new TextView(activity);
        tvMsg.setText(message);
        tvMsg.setTextColor(Color.parseColor("#94A3B8"));
        tvMsg.setTextSize(spToPx(activity, 11f));
        textCol.addView(tvMsg);
        banner.addView(textCol);

        if (actionText != null && !actionText.isEmpty()) {
            TextView btn = new TextView(activity);
            btn.setText(actionText);
            btn.setTextColor(accentColor);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setTextSize(spToPx(activity, 12f));
            btn.setPadding(dpToPx(activity, 10), dpToPx(activity, 6), dpToPx(activity, 10), dpToPx(activity, 6));
            btn.setOnClickListener(v -> {
                decor.removeView(banner);
                if (actionCallback != null) actionCallback.run();
            });
            banner.addView(btn);
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        int m = dpToPx(activity, 16);
        lp.setMargins(m, 0, m, dpToPx(activity, 24));
        banner.setLayoutParams(lp);

        // Slide In Animation from Bottom
        banner.setTranslationY(dpToPx(activity, 100));
        banner.setAlpha(0f);
        decor.addView(banner);

        banner.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(new DecelerateInterpolator())
            .start();

        // Auto dismiss after duration
        long dismissDelay = durationMs > 0 ? durationMs : 3500;
        banner.postDelayed(() -> {
            if (banner.getParent() != null) {
                banner.animate()
                    .translationY(dpToPx(activity, 100))
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> decor.removeView(banner))
                    .start();
            }
        }, dismissDelay);
    }

    // ─── STATUS PILL & HEADER ─────────────────────────────────────────

    public static TextView createStatusPill(Context context, String text, String hexColor) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(hexColor));
        tv.setTextSize(spToPx(context, 11f));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dpToPx(context, 10), dpToPx(context, 4), dpToPx(context, 10), dpToPx(context, 4));

        GradientDrawable bg = new GradientDrawable();
        int pillBg = Color.parseColor(hexColor);
        int semiTransparent = Color.argb(35, Color.red(pillBg), Color.green(pillBg), Color.blue(pillBg));
        bg.setColor(semiTransparent);
        bg.setStroke(dpToPx(context, 1), pillBg);
        bg.setCornerRadius(dpToPx(context, 20));
        tv.setBackground(bg);

        return tv;
    }

    public static LinearLayout createHeaderBar(Context context, String title, View.OnClickListener onBackClick) {
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(ThemeManager.getSurfaceColor(context));
        bar.setPadding(dpToPx(context, 16), dpToPx(context, 14), dpToPx(context, 16), dpToPx(context, 14));

        if (onBackClick != null) {
            ImageView back = new ImageView(context);
            back.setImageResource(android.R.drawable.ic_menu_revert);
            back.setColorFilter(ThemeManager.getPrimaryTextColor(context));
            back.setPadding(dpToPx(context, 4), dpToPx(context, 4), dpToPx(context, 16), dpToPx(context, 4));
            back.setOnClickListener(onBackClick);
            bar.addView(back);
        }

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeManager.getPrimaryTextColor(context));
        tvTitle.setTextSize(spToPx(context, 18f));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(tvTitle);

        return bar;
    }

    public static void showConfirmationDialog(
        Context context,
        String title,
        String message,
        String positiveText,
        String negativeText,
        Runnable onConfirm,
        Runnable onCancel
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText, (d, w) -> {
            if (onConfirm != null) onConfirm.run();
        });
        builder.setNegativeButton(negativeText, (d, w) -> {
            if (onCancel != null) onCancel.run();
        });
        builder.show();
    }
}
