package com.jdroidx.jbutterfly.ui.settings;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import com.jdroidx.jbutterfly.R;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        root.addView(UiArchitect.createHeaderBar(this, "About jButterFly Light", v -> finish()));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = UiArchitect.dpToPx(this, 20);
        content.setPadding(pad, pad, pad, pad);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.butterfly);
        int logoSize = UiArchitect.dpToPx(this, 72);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoLp.topMargin = UiArchitect.dpToPx(this, 20);
        logoLp.bottomMargin = UiArchitect.dpToPx(this, 12);
        content.addView(logo, logoLp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("jButterFly Light Enterprise");
        tvTitle.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvTitle.setTextSize(UiArchitect.spToPx(this, 18f));
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        content.addView(tvTitle);

        TextView tvVer = new TextView(this);
        tvVer.setText("Version 1.0.0 (Build 1) • Dynamic MVC Engine");
        tvVer.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvVer.setTextSize(UiArchitect.spToPx(this, 12f));
        tvVer.setPadding(0, UiArchitect.dpToPx(this, 4), 0, UiArchitect.dpToPx(this, 20));
        content.addView(tvVer);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(this));
        bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        bg.setCornerRadius(UiArchitect.dpToPx(this, 12));
        card.setBackground(bg);

        String myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        TextView tvDetails = new TextView(this);
        tvDetails.setText("🦋 jButterFly Light is an enterprise-grade spatial telemetry and device safety matrix.\n\n" +
            "• Node Identifier: " + myAgentId + "\n" +
            "• Silicon Hardware Identity Sentinel: Active\n" +
            "• Google Play & Android Maps Integration: Active\n" +
            "• Firebase Realtime Cloud Synchronization: Active\n" +
            "• WhatsApp-Style Unkillable Watchdog: Active\n" +
            "• 50 Bundled Avatars & Tactical Map Heading Chevrons\n\n" +
            "Designed and built for Android Studio, supporting Android 8.0 (Oreo) to latest Android versions across smartphones, foldables, and tablets.");
        tvDetails.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvDetails.setTextSize(UiArchitect.spToPx(this, 12f));
        tvDetails.setLineSpacing(0f, 1.25f);
        card.addView(tvDetails);

        content.addView(card);
        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);
    }
}
