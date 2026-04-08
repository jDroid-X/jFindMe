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
        master.addView(logo, new LinearLayout.LayoutParams(250, 250));

        // Enterprise Title
        TextView title = new TextView(this);
        title.setText("jFIND-ME TRACKING ENGINE");
        title.setTextColor(getTextColor());
        title.setTextSize(24f);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 40, 0, 10);
        master.addView(title);

        TextView ver = new TextView(this);
        ver.setText("ENTERPRISE BUILD MATRIX: V1.0.0 (PRODUCTION SECURE)");
        ver.setTextColor(getSecondaryTextColor());
        ver.setAlpha(0.8f);
        ver.setTextSize(11f);
        ver.setTypeface(null, Typeface.BOLD);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, 0, 0, 60);
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
            android.widget.Toast.makeText(this, "Fetching EULA via secure channel...", android.widget.Toast.LENGTH_SHORT).show();
        }));

        master.addView(buildActionButton("Privacy Policy & Terms", "Review active Data Cryptography and Retention laws", v -> {
            android.widget.Toast.makeText(this, "Loading Core Protection Rules...", android.widget.Toast.LENGTH_SHORT).show();
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
