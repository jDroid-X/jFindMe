package com.jdroidx.jfindme.ui.management;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> ManagementCluster -> NotifActivity (L4)
 * Rules: 100% Java Programmatic, Alert Status Stream.
 */
public class NotifActivity extends BaseActivityJava {

    private final String TAG = "NotifActivity";

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // 🚀 Premium Linear Gradient Hero Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("SYSTEM ALERTS");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        topRow.addView(title);
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Alert Configuration & Hardware Triggers");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, scale(10), 0, 0);
        header.addView(subtitle);

        mainRoot.addView(header);
        mainRoot.setClipChildren(false); // Global shadow enablement
        mainRoot.setClipToPadding(false);
        
        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);

        scroll.addView(createNotifList());
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 🚀 QA Fix: Global Navigation Integrity
        String[] keys = {"NAV_NOTICE", "NAV_DEVICE", "NAV_ME"};
        mainRoot.addView(UiArchitect.createBottomNav(this, keys, "NAV_NOTICE"));

        return mainRoot;
    }
    private View createNotifList() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false); // No shadow clipping
        list.setClipToPadding(false);

        int lPad = getResponsivePaddingQuota();
        list.setPadding(lPad * 2, lPad * 2, lPad * 2, lPad * 5);

        // 🚀 QA Fix: Enterprise 8-Point Configuration Matrix injected with explicit native SharedPreferences routing
        list.addView(createNotifConfigItem("Panic SOS Override", "Highest Priority. Bypasses DND volume constraints natively.", "alert_sos", true, Color.parseColor("#F44336")));
        list.addView(createNotifConfigItem("Geofence Perimeter Breach", "Triggers autonomously when a target exits or enters a Safe Zone.", "alert_geofence", true, getGradientStart()));
        list.addView(createNotifConfigItem("Battery Critical Warning", "Warns Host prior to child device hardware shutdown (Under 15%).", "alert_battery", true, Color.parseColor("#FF9800")));
        list.addView(createNotifConfigItem("Network Timeout / Dropped", "Absolute loss of signal for more than 10 consecutive minutes.", "alert_network", true, Color.parseColor("#9E9E9E")));
        list.addView(createNotifConfigItem("Device Powered Off", "Notifies heavily when Target shuts down hardware physically.", "alert_power", true, Color.parseColor("#9E9E9E")));
        // Expanded Security Architecture Configurations
        list.addView(createNotifConfigItem("GPS Services Disabled", "Critical security failure if Location is manually turned off.", "alert_gps_off", true, Color.parseColor("#F44336")));
        list.addView(createNotifConfigItem("App Permission Revoked", "Detects if tracking Background rights are removed in OS Settings.", "alert_perms", true, Color.parseColor("#F44336")));
        list.addView(createNotifConfigItem("Root / Jailbreak Detected", "Severe Security Compromise alert. Blocks malicious Mock GPS systems.", "alert_root", false, Color.parseColor("#9E9E9E")));

        return list;
    }

    private View createNotifConfigItem(String title, String body, String prefKey, boolean defVal, int accentColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, card, "CARD_STANDARD");
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int cMargin = getResponsivePaddingQuota();
        cardLp.setMargins(0, 0, 0, cMargin + scale(10));
        card.setLayoutParams(cardLp);
        
        // Matrix Render: Horizontal Colored Severity Stripe bridging seamlessly!
        LinearLayout innerRow = new LinearLayout(this);
        innerRow.setOrientation(LinearLayout.HORIZONTAL);
        innerRow.setGravity(Gravity.CENTER_VERTICAL);
        
        // Severity Ribbon
        View statusRibbon = new View(this);
        statusRibbon.setBackgroundColor(accentColor);
        innerRow.addView(statusRibbon, new LinearLayout.LayoutParams(scale(15), ViewGroup.LayoutParams.MATCH_PARENT));

        // Content Area
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        int tPad = getResponsivePaddingQuota() * 2;
        textBlock.setPadding(tPad, tPad, tPad / 2, tPad);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title.toUpperCase());
        tvTitle.setTextColor(accentColor);
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        textBlock.addView(tvTitle);

        TextView tvBody = new TextView(this);
        tvBody.setText(body);
        tvBody.setTextColor(getTextColor());
        tvBody.setAlpha(0.85f);
        tvBody.setTextSize(11);
        tvBody.setPadding(0, scale(10), 0, 0);
        textBlock.addView(tvBody);
        
        innerRow.addView(textBlock);

        // 🚀 QA Fix: Native Enterprise Interactive Switch Component
        android.widget.Switch toggle = new android.widget.Switch(this);
        android.content.SharedPreferences prefs = getSharedPreferences("com.jdroidx.jfindme.PREFS", android.content.Context.MODE_PRIVATE);
        toggle.setChecked(prefs.getBoolean(prefKey, defVal));
        int togglePad = getResponsivePaddingQuota() * 2;
        toggle.setPadding(0, 0, togglePad, 0);
        toggle.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(prefKey, isChecked).apply();
            com.jdroidx.jfindme.utils.SystemLogger.i(TAG, prefKey + " configured to: " + isChecked);
        });
        
        innerRow.addView(toggle);
        card.addView(innerRow);
        
        // Ensure the entire card bounds trigger the toggle natively securely!
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                toggle.setChecked(!toggle.isChecked()); return true;
            } return false;
        });
        card.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));

        return card;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.i(TAG, "Notification Stream Standby.");
    }
}
