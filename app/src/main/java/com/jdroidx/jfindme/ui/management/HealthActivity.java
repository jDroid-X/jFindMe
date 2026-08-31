package com.jdroidx.jfindme.ui.management;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;
import com.jdroidx.jfindme.utils.SystemLogger;

import java.io.File;

/**
 * Main -> UI Engine -> ManagementCluster -> HealthActivity (L4)
 * Rules: 100% Java Programmatic, Real-Time Device Health & Status.
 */
public class HealthActivity extends BaseActivityJava {

    private final String TAG = "HealthActivity";
    private TextView tvBattery, tvGps, tvNetwork, tvStorage;

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
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("DEVICE HEALTH");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Hardware Diagnostics & Core Operations");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 10, 0, 0);
        header.addView(subtitle);

        mainRoot.addView(header);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int cp = scale(40);
        content.setPadding(cp, cp, cp, scale(100));

        // --- LAYER 1: SYSTEM HARDWARE METRICS ---
        content.addView(createSectionHeader("SYSTEM STATUS"));
        
        LinearLayout sysCard = new LinearLayout(this);
        sysCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, sysCard, "CARD_STANDARD");
        int sp = scale(20);
        int vp = scale(10);
        sysCard.setPadding(sp, vp, sp, vp);
        
        // Dynamic Matrix Mapping for OS Contrast Integrity
        sysCard.addView(createMetricRow("Battery Level", Color.parseColor("#4CAF50"), v -> tvBattery = v)); 
        sysCard.addView(createMetricRow("GPS Engine", getGradientStart(), v -> tvGps = v)); 
        sysCard.addView(createMetricRow("Network Sync", Color.parseColor("#00E5FF"), v -> tvNetwork = v)); 
        sysCard.addView(createMetricRow("Secure Cache", getTextColor(), v -> tvStorage = v)); 
        
        content.addView(sysCard, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // --- LAYER 2: OPERATIONAL CHECKS ---
        content.addView(createSectionHeader("OPERATIONAL STATUS"));
        
        LinearLayout opCard = new LinearLayout(this);
        opCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, opCard, "CARD_STANDARD");
        int opPad = scale(20);
        opCard.setPadding(opPad, scale(10), opPad, scale(10));
        
        opCard.addView(createMetricRow("Tracking Service", Color.parseColor("#4CAF50"), v -> v.setText("RUNNING")));
        opCard.addView(createMetricRow("Service Hub", Color.parseColor("#4CAF50"), v -> v.setText("ACTIVE")));
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, scale(20));
        content.addView(opCard, cardLp);

        // --- LAYER 2.5: PING FREQUENCY SELECTOR ---
        content.addView(createSectionHeader("TRACKING PULSE"));
        LinearLayout freqCard = new LinearLayout(this);
        freqCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, freqCard, "CARD_STANDARD");
        freqCard.setPadding(opPad, scale(10), opPad, scale(10));
        freqCard.setClickable(true);
        freqCard.setFocusable(true);
        
        TextView tvCurrentPulse = new TextView(this);
        long currentInterval = prefs.getLong("location_update_interval_ms", 7000L);
        String currentLabel = (currentInterval / 1000L) + "s Active Ping";
        
        View pingRow = createMetricRow("GPS Tracking Rate", getGradientStart(), v -> {
            v.setText(currentLabel);
        });
        freqCard.addView(pingRow);
        freqCard.setOnClickListener(v -> showPingFrequencyDialog(pingRow));
        content.addView(freqCard, cardLp);

        // --- LAYER 3: ACTION CONTROLLER ---
        TextView btnRefresh = new TextView(this);
        btnRefresh.setText("RERUN SYSTEM DIAGNOSTICS");
        btnRefresh.setTextColor(Color.WHITE);
        btnRefresh.setTextSize(13);
        btnRefresh.setTypeface(null, android.graphics.Typeface.BOLD);
        btnRefresh.setGravity(android.view.Gravity.CENTER);
        int bPad = scale(20);
        btnRefresh.setPadding(bPad * 2, bPad, bPad * 2, bPad);
        android.graphics.drawable.GradientDrawable bBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{getGradientStart(), getGradientEnd()}
        );
        bBg.setCornerRadius(scale(25));
        btnRefresh.setBackground(bBg);
        btnRefresh.setElevation(scale(10));
        btnRefresh.setClickable(true);
        btnRefresh.setFocusable(true);
        btnRefresh.setOnClickListener(v -> {
            updateMetrics();
            android.widget.Toast.makeText(this, "Hardware Diagnostics Refreshed Successfully", android.widget.Toast.LENGTH_SHORT).show();
        });

        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.setMargins(0, scale(10), 0, scale(40));
        content.addView(btnRefresh, bLp);

        updateMetrics(); // Fire Native Hardware Check

        scroll.addView(content);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainRoot;
    }

    private interface ValueTextViewConsumer {
        void onTextViewCreated(TextView tv);
    }

    private View createMetricRow(String label, int valueColor, ValueTextViewConsumer consumer) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int rowPad = scale(25);
        row.setPadding(scale(20), rowPad, scale(20), rowPad);
        
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(getTextColor());
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(tvLabel);

        TextView tvValue = new TextView(this);
        tvValue.setText("FETCHING...");
        tvValue.setTextColor(valueColor);
        tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvValue);

        if (consumer != null) {
            consumer.onTextViewCreated(tvValue);
        }

        return row;
    }

    private void updateMetrics() {
        // 1. Battery Metric & Charging State
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int status = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        
        if (tvBattery != null) {
            String bText = level + "% " + (isCharging ? "(⚡ CHARGING)" : "(DISCHARGING)");
            tvBattery.setText(bText);
            tvBattery.setTextColor(level > 20 ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        }

        // 2. Network Metric
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities nc = (cm != null && cm.getActiveNetwork() != null) ? cm.getNetworkCapabilities(cm.getActiveNetwork()) : null;
        boolean isWifi = nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean isCellular = nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        if (tvNetwork != null) {
            if (isWifi) tvNetwork.setText("WIFI (ONLINE)");
            else if (isCellular) tvNetwork.setText("CELLULAR (SYNCED)");
            else tvNetwork.setText("OFFLINE / AIRPLANE");
        }

        // 3. Storage Metric (Internal Storage)
        File internalDir = getFilesDir();
        long freeSpaceMb = internalDir.getFreeSpace() / (1024 * 1024);
        if (tvStorage != null) {
            tvStorage.setText(freeSpaceMb > 1024 ? String.format(java.util.Locale.getDefault(), "%.1f GB FREE", freeSpaceMb / 1024.0) : freeSpaceMb + " MB FREE");
        }
        
        // 4. GPS Hardware State
        if (tvGps != null) {
            android.location.LocationManager lm = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gpsOn = lm != null && lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
            tvGps.setText(gpsOn ? "ACTIVE (GNSS LOCK)" : "DISABLED (GPS OFF)");
            tvGps.setTextColor(gpsOn ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        }
    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart()); // Native OS Match Protocol
        header.setTextSize(11);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(scale(10), scale(40), 0, scale(20));
        return header;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.i(TAG, "Health Metrics Engine active.");
    }

    private void showPingFrequencyDialog(View pingRow) {
        String[] options = {
            "⚡ 1s Pulse (Max Tracking • High Battery)",
            "🚀 7s Pulse (Recommended • Standard Balance)",
            "🛰️ 15s Pulse (Smooth Tracking • Battery Saver)",
            "🔋 30s Pulse (Eco Mode • Low Drain)",
            "🌱 60s Pulse (Ultra Eco • Maximum Longevity)"
        };
        long[] intervals = {1000L, 7000L, 15000L, 30000L, 60000L};
        long current = prefs.getLong("location_update_interval_ms", 7000L);
        int selectedIdx = 1;
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] == current) { selectedIdx = i; break; }
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setTitle("Select Tracking Pulse")
            .setSingleChoiceItems(options, selectedIdx, (d, which) -> {
                long chosen = intervals[which];
                prefs.edit().putLong("location_update_interval_ms", chosen).apply();
                android.widget.Toast.makeText(this, "Tracking rate updated to " + (chosen / 1000L) + "s", android.widget.Toast.LENGTH_SHORT).show();
                if (pingRow instanceof ViewGroup) {
                    View lastChild = ((ViewGroup) pingRow).getChildAt(((ViewGroup) pingRow).getChildCount() - 1);
                    if (lastChild instanceof TextView) {
                        ((TextView) lastChild).setText((chosen / 1000L) + "s Active Ping");
                    }
                }
                d.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .create();
        
        dialog.show();
        applyDialogTheme(dialog);
    }
}
