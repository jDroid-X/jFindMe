package com.jdroidx.jbutterfly.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jbutterfly.R;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.ui.map.MapActivity;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;
import com.jdroidx.jbutterfly.utils.RegistryManager;
import com.jdroidx.jbutterfly.utils.SystemLogger;

public class AuthActivity extends Activity {

    private static final String TAG = "AuthActivity";
    private EditText etEmail;
    private EditText etPassword;
    private ProgressBar progressBar;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = UiArchitect.dpToPx(this, 24);
        root.setPadding(pad, pad, pad, pad);

        // Logo
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.butterfly);
        int logoSize = UiArchitect.dpToPx(this, 80);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoLp.topMargin = UiArchitect.dpToPx(this, 30);
        logoLp.bottomMargin = UiArchitect.dpToPx(this, 12);
        root.addView(logo, logoLp);

        // App Title & Tagline
        TextView tvTitle = new TextView(this);
        tvTitle.setText(R.string.title_auth);
        tvTitle.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvTitle.setTextSize(UiArchitect.spToPx(this, 20f));
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText(R.string.app_tagline);
        tvSubtitle.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvSubtitle.setTextSize(UiArchitect.spToPx(this, 12f));
        tvSubtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subLp.bottomMargin = UiArchitect.dpToPx(this, 24);
        root.addView(tvSubtitle, subLp);

        // Hardware Agent ID Badge
        String agentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        TextView tvHwBadge = UiArchitect.createStatusPill(this, "HARDWARE NODE: " + agentId, "#3B82F6");
        root.addView(tvHwBadge);

        // Form Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(ThemeManager.getSurfaceColor(this));
        int cardPad = UiArchitect.dpToPx(this, 20);
        card.setPadding(cardPad, cardPad, cardPad, cardPad);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(ThemeManager.getSurfaceColor(this));
        cardBg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        cardBg.setCornerRadius(UiArchitect.dpToPx(this, 14));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.topMargin = UiArchitect.dpToPx(this, 20);
        cardLp.bottomMargin = UiArchitect.dpToPx(this, 16);

        // Email Field
        etEmail = UiArchitect.createInputField(this, getString(R.string.hint_email), InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        card.addView(etEmail);

        // Password Field
        etPassword = UiArchitect.createInputField(this, getString(R.string.hint_password), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams pwLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        pwLp.topMargin = UiArchitect.dpToPx(this, 12);
        pwLp.bottomMargin = UiArchitect.dpToPx(this, 20);
        card.addView(etPassword, pwLp);

        // Authorize Button
        Button btnAuth = UiArchitect.createPrimaryButton(this, getString(R.string.btn_authorize), v -> performAuthorization());
        card.addView(btnAuth);

        // Progress Bar
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        pbLp.gravity = Gravity.CENTER_HORIZONTAL;
        pbLp.topMargin = UiArchitect.dpToPx(this, 12);
        card.addView(progressBar, pbLp);

        tvStatus = new TextView(this);
        tvStatus.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvStatus.setTextSize(UiArchitect.spToPx(this, 12f));
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setVisibility(View.GONE);
        card.addView(tvStatus);

        root.addView(card, cardLp);

        // Quick Offline / Sentinel Bypass Link
        TextView tvBypass = new TextView(this);
        tvBypass.setText("Continue with Hardware Sentinel Mode (Offline / Direct)");
        tvBypass.setTextColor(ThemeManager.getBrandColor(this));
        tvBypass.setTextSize(UiArchitect.spToPx(this, 13f));
        tvBypass.setGravity(Gravity.CENTER);
        tvBypass.setPadding(0, UiArchitect.dpToPx(this, 10), 0, UiArchitect.dpToPx(this, 10));
        tvBypass.setOnClickListener(v -> proceedToApp("Hardware Sentinel Node"));
        root.addView(tvBypass);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void performAuthorization() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.msg_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Authenticating with Cloud Mesh...");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> proceedToApp(email))
            .addOnFailureListener(e -> {
                // If account doesn't exist, auto-create
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> proceedToApp(email))
                    .addOnFailureListener(err -> {
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setText("Cloud auth bypassed. Local Hardware Mode activated.");
                        Toast.makeText(this, "Local Hardware Node activated.", Toast.LENGTH_SHORT).show();
                        proceedToApp(email);
                    });
            });
    }

    private void proceedToApp(String nickname) {
        RegistryManager.putBoolean(this, RegistryManager.KEY_IS_LOGGED_IN, true);
        RegistryManager.putString(this, RegistryManager.KEY_USER_NICKNAME, nickname);
        SystemLogger.i(TAG, "User authorized: " + nickname);

        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
        finish();
    }

    // Custom ScrollView helper
    private static class ScrollScrollView extends ScrollView {
        public ScrollScrollView(Activity context) {
            super(context);
        }
    }
}
