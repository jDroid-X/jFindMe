package com.jdroidx.jbutterfly.ui.management;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.RegistryManager;

public class PrivacyActivity extends Activity {

    private LinearLayout contentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        root.addView(UiArchitect.createHeaderBar(this, "Privacy Controls & Permissions", v -> finish()));

        ScrollView scroll = new ScrollView(this);
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 16);
        contentLayout.setPadding(pad, pad, pad, UiArchitect.dpToPx(this, 30));
        scroll.addView(contentLayout);

        root.addView(scroll);
        setContentView(root);

        buildPrivacyControls();
    }

    private void buildPrivacyControls() {
        contentLayout.removeAllViews();
        int pad = UiArchitect.dpToPx(this, 16);

        // 1. Ghost Cloaking Switch Card
        boolean isGhost = RegistryManager.getBoolean(this, "ghost_mode_active", false);
        View ghostSwitch = UiArchitect.createSwitch(
            this,
            "Ghost Cloaking Mode",
            "When enabled, your device will pause broadcasting live coordinates to the cloud mesh while remaining protected locally.",
            isGhost,
            (buttonView, isChecked) -> {
                RegistryManager.putBoolean(this, "ghost_mode_active", isChecked);
                UiArchitect.showSlideNotificationBanner(this,
                    "Ghost Mode",
                    isChecked ? "Ghost mode ACTIVE: Coordinates hidden from remote radar." : "Ghost mode DISABLED: Visible to circle.",
                    isChecked, 3000, null, null
                );
            }
        );
        contentLayout.addView(ghostSwitch);

        // 2. System Permissions Health Checklist Card
        LinearLayout permCard = createCard();
        TextView tvPermTitle = createCardTitle("🛡️ System Permissions Sentinel");
        permCard.addView(tvPermTitle);

        boolean fineLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean bgLoc = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean notif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean batOpt = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName()));

        permCard.addView(createPermissionRow("Precise GPS Location", fineLoc));
        permCard.addView(createPermissionRow("Background Location ('Allow all the time')", bgLoc));
        permCard.addView(createPermissionRow("Incident Push Notifications", notif));
        permCard.addView(createPermissionRow("Battery Optimization Whitelist", batOpt));

        Button btnSettings = UiArchitect.createPrimaryButton(this, "⚙️ MANAGE APP PERMISSIONS IN SETTINGS", v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnLp.topMargin = UiArchitect.dpToPx(this, 12);
        permCard.addView(btnSettings, btnLp);

        contentLayout.addView(permCard);

        // 3. Cryptographic Privacy Guarantee Card
        LinearLayout infoCard = createCard();
        infoCard.addView(createCardTitle("🔒 Zero-Data-Brokerage Architecture"));

        TextView tvInfo = new TextView(this);
        tvInfo.setText(
            "• Peer-to-Peer Encryption: Telemetry coordinates are strictly shared with your paired Family and Friends nodes.\n\n" +
            "• Physical Silicon Anchor: Identifiers are cryptographically anchored to physical silicon keys, eliminating software ID hijacking.\n\n" +
            "• Local SQLite Sovereignty: Historical location crumbs are preserved locally on-device and can be purged at any time."
        );
        tvInfo.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvInfo.setTextSize(UiArchitect.spToPx(this, 12f));
        tvInfo.setLineSpacing(0f, 1.25f);
        tvInfo.setPadding(0, UiArchitect.dpToPx(this, 8), 0, 0);
        infoCard.addView(tvInfo);

        contentLayout.addView(infoCard);
    }

    private View createPermissionRow(String label, boolean granted) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, UiArchitect.dpToPx(this, 6), 0, UiArchitect.dpToPx(this, 6));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvLabel.setTextSize(UiArchitect.spToPx(this, 12f));
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(tvLabel, labelLp);

        TextView tvBadge = UiArchitect.createStatusPill(
            this,
            granted ? "GRANTED" : "DENIED",
            granted ? "#22C55E" : "#EF4444"
        );
        row.addView(tvBadge);
        return row;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 16);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(this));
        bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        bg.setCornerRadius(UiArchitect.dpToPx(this, 12));
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = UiArchitect.dpToPx(this, 12);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createCardTitle(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(ThemeManager.getBrandColor(this));
        tv.setTextSize(UiArchitect.spToPx(this, 13f));
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return tv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildPrivacyControls();
    }
}
