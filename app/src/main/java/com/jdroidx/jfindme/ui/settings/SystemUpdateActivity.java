package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.AngelLoader;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

/**
 * 👑 GLOBAL UI ARCHITECT: System Update Center
 * Waterfall MVVM Architecture Implementation (L7 Standard)
 * Manages secure binary synchronization and system identification.
 */
public class SystemUpdateActivity extends BaseActivityJava {

    private SystemUpdateViewModel viewModel;
    private EditText emailField;
    private EditText driveLinkField;
    private android.widget.CheckBox silentUpdateCheck;

    @Override
    protected void setupLogic() {
        // Initialize Waterfall MVVM Engine
        viewModel = new ViewModelProvider(this).get(SystemUpdateViewModel.class);
        viewModel.initData(this);

        // --- OBSERVERS: Synchronization Hub ---
        
        viewModel.getGmailAccount().observe(this, email -> {
            if (emailField != null) emailField.setText(email);
        });

        viewModel.getDriveLink().observe(this, link -> {
            if (driveLinkField != null) driveLinkField.setText(link);
        });

        viewModel.isSilentMode().observe(this, isEnabled -> {
            if (silentUpdateCheck != null) {
                silentUpdateCheck.setOnCheckedChangeListener(null);
                silentUpdateCheck.setChecked(isEnabled);
                silentUpdateCheck.setOnCheckedChangeListener((bv, checked) -> viewModel.toggleSilentMode(this, checked));
            }
        });

        viewModel.getLoadingStatus().observe(this, status -> {
            if (status != null) AngelLoader.setStatus(this, status);
        });

        viewModel.isLoading().observe(this, loading -> {
            if (loading) AngelLoader.show(this);
            else AngelLoader.hide(this);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                // Parse Waterfall Error Code (SYNC_FAILED: ...)
                String[] parts = error.split(":");
                String title = parts.length > 0 ? parts[0].replace("_", " ") : "Execution Error";
                String msg = parts.length > 1 ? parts[1] : error;
                showForensicDialog(title, msg);
                viewModel.clearError();
            }
        });

        viewModel.getActionComplete().observe(this, complete -> {
            if (complete) {
                triggerBinaryUpdate();
                viewModel.resetActionState();
            }
        });
    }

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // --- HEADER: L7 Waterfall Branding ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView title = new TextView(this);
        title.setText("SYSTEM UPDATE CENTER");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Secure Binary Synchronization Protocol");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        subtitle.setPadding(0, scale(10), 0, 0);
        header.addView(subtitle);

        root.addView(header);

        // --- MATRIX: Core Input Architecture ---
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        scroll.setClipToPadding(false);

        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        int quota = getResponsivePaddingQuota();
        master.setPadding(quota, quota, quota, scale(60));

        // Label-and-Box Strategy (Google Account)
        master.addView(createLabel("REGISTERED GOOGLE ACCOUNT (*)"));
        emailField = createEnterpriseField("Reading hardware id...", android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setEnabled(false);
        emailField.setFocusable(false);
        master.addView(wrapField(emailField));

        // Label-and-Box Strategy (Binary Source)
        master.addView(createLabel("BINARY SOURCE LINK (*)"));
        driveLinkField = createEnterpriseField("Constructing link...", android.text.InputType.TYPE_TEXT_VARIATION_URI);
        driveLinkField.setEnabled(false);
        driveLinkField.setFocusable(false);
        master.addView(wrapField(driveLinkField));

        // Interaction Toggle: Silent Update
        master.addView(createSilentUpdateNode());

        master.addView(createSpacer(50));

        // --- ACTIONS: Compact Integrated Row (3.0 Span) ---
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setWeightSum(3.0f);

        View btnInstall = createSmallButton("INSTALL", android.R.drawable.ic_menu_save, true, v -> viewModel.executeInstall());
        View btnUpdate = createSmallButton("UPDATE", android.R.drawable.ic_menu_edit, true, v -> viewModel.executeUpdate());
        View btnCancel = createSmallButton("CANCEL", android.R.drawable.ic_menu_close_clear_cancel, false, v -> finish());

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        actionRow.addView(btnInstall, btnLp);

        LinearLayout.LayoutParams midLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        midLp.setMargins(scale(10), 0, scale(10), 0);
        actionRow.addView(btnUpdate, midLp);
        
        actionRow.addView(btnCancel, btnLp);

        master.addView(actionRow);

        // --- FOOTER: Design Balance Matrix ---
        master.addView(createArchitecturalFooter());

        scroll.addView(master);
        root.addView(scroll);

        return root;
    }

    private View createSilentUpdateNode() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(scale(20), scale(20), scale(20), scale(20));
        
        android.graphics.drawable.GradientDrawable glass = new android.graphics.drawable.GradientDrawable();
        glass.setCornerRadius(30f);
        glass.setColor(Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        glass.setStroke(2, getGradientStart());
        row.setBackground(glass);

        TextView label = new TextView(this);
        label.setText("SILENT UPDATE");
        label.setTextColor(getTextColor());
        UiArchitect.applyText(this, label, "TEXT_TITLE");
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(label);

        silentUpdateCheck = new android.widget.CheckBox(this);
        silentUpdateCheck.setButtonTintList(android.content.res.ColorStateList.valueOf(getGradientStart()));
        row.addView(silentUpdateCheck);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, scale(15), 0, scale(10));
        row.setLayoutParams(lp);
        
        return row;
    }

    private View createSmallButton(String text, int iconRes, boolean isPrimary, View.OnClickListener listener) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, scale(20), 0, scale(20));
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setOnClickListener(listener);

        UiArchitect.applyCardWithOverride(this, btn, isPrimary ? "CARD_CAPSULE_BUTTON" : "CARD_STANDARD");

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(isPrimary ? getGradientOverlayText() : getTextColor());
        UiArchitect.applyText(this, tv, "BUTTON_TEXT");
        tv.setGravity(Gravity.CENTER);
        btn.addView(tv);

        applyAccessibility(btn, text);
        return btn;
    }

    private View createArchitecturalFooter() {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, scale(60), 0, 0);

        int lottieId = getResources().getIdentifier("butterfly_angel", "raw", getPackageName());
        if (lottieId != 0) {
            LottieAnimationView lottie = new LottieAnimationView(this);
            lottie.setAnimation(lottieId);
            lottie.setRepeatCount(LottieDrawable.INFINITE);
            lottie.playAnimation();
            int bSize = scale(200);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(bSize, bSize);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            footer.addView(lottie, lp);
        }

        TextView name = new TextView(this);
        name.setText("jFindMe Tracking Engine");
        name.setTextColor(getTextColor());
        UiArchitect.applyText(this, name, "TEXT_BODY");
        name.setAlpha(0.9f);
        footer.addView(name);

        TextView ver = new TextView(this);
        ver.setText("ENTERPRISE BUILD: V1.0.0");
        ver.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, ver, "TEXT_MINI");
        ver.setAlpha(0.6f);
        footer.addView(ver);

        return footer;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(getGradientStart());
        UiArchitect.applyText(this, label, "TEXT_LABEL");
        label.setPadding(scale(10), scale(15), 0, scale(8));
        return label;
    }

    private EditText createEnterpriseField(String hint, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextColor(getTextColor());
        et.setHintTextColor(getSecondaryTextColor());
        et.setBackgroundColor(Color.TRANSPARENT);
        et.setPadding(scale(25), scale(20), scale(25), scale(20));
        UiArchitect.applyText(this, et, "TEXT_BODY");
        return et;
    }

    private View wrapField(View field) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable glass = new android.graphics.drawable.GradientDrawable();
        glass.setCornerRadius(30f);
        glass.setColor(getCardColor());
        glass.setStroke(2, Color.parseColor("#33" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        wrap.setBackground(glass);
        wrap.addView(field);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, scale(15));
        wrap.setLayoutParams(lp);
        
        return wrap;
    }

    private View createSpacer(int dpSize) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, scale(dpSize)));
        return v;
    }

    private void triggerBinaryUpdate() {
        try {
            String link = viewModel.getDriveLink().getValue();
            if (link != null) {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link));
                startActivity(intent);
            }
        } catch (Exception e) {
            showForensicDialog("Protocol Execution Blocked", "The hardware browser node could not be activated for binary sync.");
        }
    }
}
