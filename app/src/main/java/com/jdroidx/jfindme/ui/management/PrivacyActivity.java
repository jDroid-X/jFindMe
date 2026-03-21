package com.jdroidx.jfindme.ui.management;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jdroidx.jfindme.engine.security.SecurityEngine;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> ManagementCluster -> PrivacyActivity (L4)
 * Rules: 100% Java Programmatic, Security Agent Integration.
 */
public class PrivacyActivity extends BaseActivityJava {

    private final String TAG = "PrivacyActivity";

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // 🚀 Premium Linear Gradient Hero Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(50, 80, 50, 60);
        applyLinearGradient(header, 0f);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("SECURITY PROTOCOLS");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Privacy Engines & System Telemetry");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 10, 0, 0);
        header.addView(subtitle);

        mainRoot.addView(header);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(40, 40, 40, 100);

        // --- LAYER 1: PRIVACY SWITCHES ---
        content.addView(createSectionHeader("HARDWARE PERMISSIONS"));
        
        LinearLayout pPanel = new LinearLayout(this);
        pPanel.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(pPanel, getCardColor(), 30f);
        pPanel.setPadding(40,40,40,40);
        
        pPanel.addView(createPermissionToggle("Location Services", true));
        pPanel.addView(createPermissionToggle("Background Scanning", false)); // Match Android Specs
        pPanel.addView(createPermissionToggle("Telemetry Feedback", true));
        
        content.addView(pPanel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // --- LAYER 2: ROOT INTEGRITY CHECKS ---
        content.addView(createSectionHeader("INTEGRITY COMPLIANCE"));
        LinearLayout rootPanel = new LinearLayout(this);
        rootPanel.setOrientation(LinearLayout.VERTICAL);
        applySolidCardGeometry(rootPanel, getCardColor(), 30f);
        
        rootPanel.addView(createIconButton("RUN FULL DIAGNOSTIC", android.R.drawable.ic_dialog_info, v -> {
            boolean isRooted = SecurityEngine.INSTANCE.isRooted();
            android.widget.Toast.makeText(this, isRooted ? "FATAL: Device Integrity Compromised (Rooted Structure)" : "Status Green: Safe Execution Environment Verified", android.widget.Toast.LENGTH_LONG).show();
        }));
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 40);
        content.addView(rootPanel, cardLp);

        scroll.addView(content);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainRoot;
    }

    private View createPermissionToggle(String label, boolean defaultON) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 25, 0, 25);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(getTextColor()); // Binding explicitly removes Light Mode blindspots!
        tvLabel.setTextSize(14);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(tvLabel);

        android.widget.Switch toggle = new android.widget.Switch(this);
        toggle.setChecked(defaultON);
        row.addView(toggle);

        return row;

    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Native OS Theme Lock
        header.setTextSize(11);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(10, 40, 0, 20);
        return header;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.i(TAG, "Privacy Shield Active.");
    }
}
