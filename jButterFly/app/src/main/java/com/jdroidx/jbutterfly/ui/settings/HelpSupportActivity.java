package com.jdroidx.jbutterfly.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;

public class HelpSupportActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        root.addView(UiArchitect.createHeaderBar(this, "Help & Support Knowledge Base", v -> finish()));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 16);
        content.setPadding(pad, pad, pad, pad);

        content.addView(createFaqItem("How do I pair a family member or friend's device?",
            "1. Open the Menu and tap 'Device Pairing' or tap '+ PAIR DEVICE' on the radar.\n" +
            "2. Choose whether the device is in your 'Family' or 'Friends' circle.\n" +
            "3. Either scan their pairing QR code with your camera, or enter their 8-character hardware Agent ID."));

        content.addView(createFaqItem("How does the Acoustic Siren Buzz work?",
            "Tap 'ROSTER' from the Live Radar, locate the target device, and tap 'BUZZ SIREN'. The target phone will immediately sound a loud acoustic alarm and vibrate, even if the device screen is off or on silent."));

        content.addView(createFaqItem("Does jButterFly Light continue tracking when closed?",
            "Yes! jButterFly Light operates an ongoing foreground sentinel service with an AlarmManager watchdog, exactly like WhatsApp or Uber, ensuring continuous background protection even after reboots."));

        content.addView(createFaqItem("How do SafeZones notify me?",
            "Go to 'Safe Zones' in the menu, tap '+ CREATE NEW SAFEZONE', set your desired radius (e.g. 200 meters), and toggle Entry / Exit alerts. You will receive real-time notifications as devices cross perimeter boundaries."));

        Button btnDiag = UiArchitect.createPrimaryButton(this, "📋 VIEW HARDWARE DIAGNOSTICS & SPECS", v -> {
            DeviceIdentityHelper.DeviceHardwareInfo specs = DeviceIdentityHelper.getHardwareSpecs(this);
            String info = "• Agent ID: " + specs.getAgentId() +
                "\n• Hardware Key: " + specs.getHardwareKey() +
                "\n• Device: " + specs.getBrand() + " " + specs.getModel() +
                "\n• Board: " + specs.getBoard() +
                "\n• Telemetry Engine: Dynamic OOPS MVC" +
                "\n• Database: SQLite SSOT with Cascade";
            new AlertDialog.Builder(this)
                .setTitle("Hardware Node Diagnostics")
                .setMessage(info)
                .setPositiveButton("COPY / CLOSE", (d, w) -> {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("Diagnostics", info));
                        UiArchitect.showSlideNotificationBanner(this, "Copied", "Specs copied to clipboard.", false, 2000, null, null);
                    }
                })
                .show();
        });
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dLp.topMargin = UiArchitect.dpToPx(this, 10);
        dLp.bottomMargin = UiArchitect.dpToPx(this, 20);
        content.addView(btnDiag, dLp);

        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);
    }

    private LinearLayout createFaqItem(String question, String answer) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 14);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(this));
        bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        bg.setCornerRadius(UiArchitect.dpToPx(this, 10));
        card.setBackground(bg);

        TextView tvQ = new TextView(this);
        tvQ.setText("❓ " + question);
        tvQ.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvQ.setTextSize(UiArchitect.spToPx(this, 13f));
        tvQ.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(tvQ);

        TextView tvA = new TextView(this);
        tvA.setText(answer);
        tvA.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvA.setTextSize(UiArchitect.spToPx(this, 11f));
        tvA.setPadding(0, UiArchitect.dpToPx(this, 6), 0, 0);
        tvA.setLineSpacing(0f, 1.2f);
        card.addView(tvA);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = UiArchitect.dpToPx(this, 12);
        card.setLayoutParams(lp);

        return card;
    }
}
