package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;

public class AboutActivity extends BaseActivityJava {
    @Override
    protected void setupLogic() {
        // Static Enterprise Page: No live listeners or data bindings required natively.
    }

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scroll.setClipToPadding(false);
        scroll.setPadding(40, 80, 40, 80);

        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        master.setGravity(Gravity.CENTER_HORIZONTAL);
        
        // App Icon natively tinted
        ImageView logo = new ImageView(this);
        logo.setImageResource(android.R.drawable.ic_dialog_info); 
        logo.setColorFilter(getGradientStart()); // Overridden mathematically by current Theme
        int lSize = scale(100);
        master.addView(logo, new LinearLayout.LayoutParams(lSize, lSize));

        // Enterprise Title
        TextView title = new TextView(this);
        title.setText("jFIND-ME TRACKING ENGINE");
        title.setTextColor(getTextColor());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, scale(15), 0, scale(5));
        master.addView(title);

        TextView ver = new TextView(this);
        ver.setText("ENTERPRISE BUILD: V" + com.jdroidx.jfindme.BuildConfig.VERSION_NAME + " (BUILD " + com.jdroidx.jfindme.BuildConfig.VERSION_CODE + ")");
        ver.setTextColor(getSecondaryTextColor());
        ver.setAlpha(0.8f);
        UiArchitect.applyText(this, ver, "TEXT_MINI");
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, 0, 0, scale(20));
        master.addView(ver);

        // Core Values Injection
        master.addView(buildDetailCard("Telemetry Architecture", "MVVM Synchronous Data Engine natively bound to the Firebase Realtime Protocol with zero-latency transmission states."));
        master.addView(buildDetailCard("Network Hardware Utilization", "Multithreaded Background Subsystem. Location Engine explicitly runs autonomously even when Application drops into Doze (Sleep) bounds."));
        master.addView(buildDetailCard("End-to-End Security", "AES-256 GCM Transport Standard. Constant GPS Location pings strictly verified. Data payloads bypass third-party physical servers."));
        master.addView(buildDetailCard("API & Dependency Stack", "Google Maps V2 Geometry Interface • Google Fused Location Telemetry Core • Firebase Auth Protocol • jDroidx Custom Engine."));
        master.addView(buildDetailCard("Software Licensing", "Proprietary Enterprise Software. Licensed exclusively for Private Internal Network operations only. Distribution absolutely prohibited."));
        master.addView(buildDetailCard("Development Protocol", "JDroid Studio | Core Application Development Tier 1"));

        // Interactive Enterprise Action Handlers dynamically drawn
        master.addView(buildActionButton("Check for System Updates", "Ping secure servers for latest binaries", v -> {
            startActivity(new android.content.Intent(this, SystemUpdateActivity.class));
        }));
        
        master.addView(buildActionButton("View Enterprise License (EULA)", "Read corporate bounds and authorization limitations", v -> {
            UiArchitect.showValidationDialog(this,
                "Enterprise End-User License (EULA)",
                "1. GRANT OF LICENSE: Licensed for internal family & guardian safety operations.\n\n" +
                "2. TELEMETRY CONSENT: Real-time GPS coordinates are transmitted exclusively to paired guardian devices via end-to-end encryption.\n\n" +
                "3. DATA RETENTION: Historical breadcrumbs are stored locally on device for up to 7 days and can be permanently purged at any time.\n\n" +
                "4. RESTRICTIONS: Reverse engineering, unauthorized distribution, or unauthorized location spoofing is strictly prohibited.",
                null);
        }));

        master.addView(buildActionButton("Privacy Policy & Terms", "Review active Data Cryptography and Retention laws", v -> {
            UiArchitect.showValidationDialog(this,
                "Privacy & Data Protection Policy",
                "• ZERO THIRD-PARTY SHARING: Your spatial telemetry and identity dossiers are never sold or transmitted to third-party data brokers.\n\n" +
                "• LOCAL-FIRST ENCRYPTION: Sensitive biometrics, medical notes, and local breadcrumbs are encrypted with AES-256 GCM.\n\n" +
                "• GHOST MODE: You can engage Ghost Mode at any time in Privacy Settings to temporarily halt outbound telemetry pings.\n\n" +
                "• COMPLIANCE: Adheres to GDPR Data Minimization and Children's Online Privacy guidelines.",
                null);
        }));

        scroll.addView(master);
        root.addView(scroll);
        return root;
    }

    private View buildDetailCard(String titleStr, String descStr) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(45, 40, 45, 40);
        
        android.graphics.drawable.GradientDrawable glass = new android.graphics.drawable.GradientDrawable();
        glass.setCornerRadius(30f);
        glass.setColor(getCardColor());
        card.setBackground(glass);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 40);
        card.setLayoutParams(lp);

        TextView t = new TextView(this);
        t.setText(titleStr.toUpperCase());
        t.setTextColor(getGradientStart()); // Physically ties strings to active BaseActivity themes seamlessly!
        t.setTextSize(13f);
        t.setTypeface(null, Typeface.BOLD);
        card.addView(t);

        TextView d = new TextView(this);
        d.setText(descStr);
        d.setTextColor(getTextColor());
        d.setAlpha(0.85f);
        d.setTextSize(12f);
        d.setPadding(0, 20, 0, 0); // Spacer between title and paragraph natively
        card.addView(d);

        return card;
    }

    private View buildActionButton(String titleStr, String subStr, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(40, 30, 40, 30);
        
        android.graphics.drawable.GradientDrawable glass = new android.graphics.drawable.GradientDrawable();
        glass.setCornerRadius(20f);
        // Emulate a sleek interactive tactile button array matching the Custom Global Theme mathematically!
        glass.setColor(Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        glass.setStroke(3, getGradientStart());
        
        row.setBackground(glass);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);
        applyAccessibility(row, titleStr);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 30);
        row.setLayoutParams(lp);

        TextView t = new TextView(this);
        t.setText(titleStr.toUpperCase());
        t.setTextColor(getTextColor());
        t.setTextSize(14f);
        t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        row.addView(t);

        TextView s = new TextView(this);
        s.setText(subStr);
        s.setTextColor(getSecondaryTextColor());
        s.setTextSize(11f);
        s.setPadding(0, 10, 0, 0);
        s.setGravity(Gravity.CENTER);
        row.addView(s);

        return row;
    }
}
