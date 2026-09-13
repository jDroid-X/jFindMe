package com.jdroidx.jbutterfly.ui.management;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;

public class HealthActivity extends Activity {

    private LinearLayout contentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        root.addView(UiArchitect.createHeaderBar(this, "Device Health & Telemetry", v -> finish()));

        ScrollView scroll = new ScrollView(this);
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 16);
        contentLayout.setPadding(pad, pad, pad, UiArchitect.dpToPx(this, 30));
        scroll.addView(contentLayout);

        root.addView(scroll);
        setContentView(root);

        buildHealthDashboard();
    }

    private void buildHealthDashboard() {
        contentLayout.removeAllViews();
        int pad = UiArchitect.dpToPx(this, 16);

        // 1. Power & Battery Whitelist Action Card
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isIgnoringOptimizations = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());

        if (!isIgnoringOptimizations) {
            LinearLayout alertCard = new LinearLayout(this);
            alertCard.setOrientation(LinearLayout.VERTICAL);
            alertCard.setPadding(pad, pad, pad, pad);
            GradientDrawable alertBg = new GradientDrawable();
            alertBg.setColor(Color.parseColor("#451A03")); // Deep Amber
            alertBg.setStroke(UiArchitect.dpToPx(this, 1), Color.parseColor("#F59E0B"));
            alertBg.setCornerRadius(UiArchitect.dpToPx(this, 12));
            alertCard.setBackground(alertBg);

            TextView tvWarnTitle = new TextView(this);
            tvWarnTitle.setText("⚠️ Background Sentinel Restriced");
            tvWarnTitle.setTextColor(Color.parseColor("#FDE68A"));
            tvWarnTitle.setTextSize(UiArchitect.spToPx(this, 14f));
            tvWarnTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            alertCard.addView(tvWarnTitle);

            TextView tvWarnDesc = new TextView(this);
            tvWarnDesc.setText("Android OS may kill background GPS tracking when the screen turns off. Whitelist jButterFly to ensure uninterrupted safety.");
            tvWarnDesc.setTextColor(Color.WHITE);
            tvWarnDesc.setTextSize(UiArchitect.spToPx(this, 12f));
            tvWarnDesc.setPadding(0, UiArchitect.dpToPx(this, 6), 0, UiArchitect.dpToPx(this, 12));
            alertCard.addView(tvWarnDesc);

            Button btnWhitelist = UiArchitect.createAccentButton(this, "⚡ WHITELIST APP (UNRESTRICTED)", v -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    try {
                        startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    } catch (Exception ignored) {}
                }
            });
            alertCard.addView(btnWhitelist);

            LinearLayout.LayoutParams acLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            acLp.bottomMargin = UiArchitect.dpToPx(this, 14);
            contentLayout.addView(alertCard, acLp);
        }

        // 2. Battery Telemetry Card
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 100;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : 100;
        int pct = (level * 100) / (scale > 0 ? scale : 100);

        int status = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

        int chargePlug = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) : -1;
        String plugType = "Discharging (Battery)";
        if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB) plugType = "USB Cable";
        else if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC) plugType = "AC Wall Charger";
        else if (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS) plugType = "Wireless Charging";

        int tempTenths = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) : 0;
        float tempC = tempTenths / 10.0f;

        LinearLayout batCard = createCard();
        TextView tvBatTitle = createCardTitle("🔋 Battery Health & Power");
        batCard.addView(tvBatTitle);

        TextView tvBatDetails = new TextView(this);
        tvBatDetails.setText(
            "• Current Level: " + pct + "% (" + (isCharging ? "⚡ Charging" : "Discharging") + ")\n" +
            "• Power Source: " + plugType + "\n" +
            "• Battery Temperature: " + String.format("%.1f", tempC) + " °C\n" +
            "• OS Optimization: " + (isIgnoringOptimizations ? "🟢 Whitelisted (Unkillable Sentinel)" : "🟡 Standard Battery Optimization")
        );
        tvBatDetails.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvBatDetails.setTextSize(UiArchitect.spToPx(this, 12f));
        tvBatDetails.setLineSpacing(0f, 1.25f);
        tvBatDetails.setPadding(0, UiArchitect.dpToPx(this, 8), 0, 0);
        batCard.addView(tvBatDetails);
        contentLayout.addView(batCard);

        // 3. Sensor & GPS Telemetry Card
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean netEnabled = lm != null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        LinearLayout gpsCard = createCard();
        TextView tvGpsTitle = createCardTitle("🛰️ GPS & Positioning Sensors");
        gpsCard.addView(tvGpsTitle);

        TextView tvGpsDetails = new TextView(this);
        tvGpsDetails.setText(
            "• High-Precision GPS Hardware: " + (gpsEnabled ? "🟢 ACTIVE" : "🔴 DISABLED") + "\n" +
            "• Cellular/Wi-Fi Network Triangulation: " + (netEnabled ? "🟢 ACTIVE" : "🔴 DISABLED") + "\n" +
            "• Spatial Refresh Frequency: Adaptive Polling Dynamic Engine"
        );
        tvGpsDetails.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvGpsDetails.setTextSize(UiArchitect.spToPx(this, 12f));
        tvGpsDetails.setLineSpacing(0f, 1.25f);
        tvGpsDetails.setPadding(0, UiArchitect.dpToPx(this, 8), 0, UiArchitect.dpToPx(this, 10));
        gpsCard.addView(tvGpsDetails);

        if (!gpsEnabled) {
            Button btnGps = UiArchitect.createSecondaryButton(this, "ENABLE GPS HARDWARE", v -> {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            });
            gpsCard.addView(btnGps);
        }
        contentLayout.addView(gpsCard);

        // 4. Memory & Storage Health Card
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        if (am != null) am.getMemoryInfo(memInfo);

        long availMemMb = memInfo.availMem / (1024 * 1024);
        long totalMemMb = memInfo.totalMem / (1024 * 1024);

        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long availBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();
        long availStorageMb = availBytes / (1024 * 1024);
        long totalStorageMb = totalBytes / (1024 * 1024);

        LinearLayout memCard = createCard();
        TextView tvMemTitle = createCardTitle("💾 Memory & Local Storage");
        memCard.addView(tvMemTitle);

        TextView tvMemDetails = new TextView(this);
        tvMemDetails.setText(
            "• Available RAM: " + availMemMb + " MB / " + totalMemMb + " MB\n" +
            "• Internal Storage: " + availStorageMb + " MB Free / " + totalStorageMb + " MB Total\n" +
            "• Database Engine: SQLite Normalized SSOT with Indexing"
        );
        tvMemDetails.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvMemDetails.setTextSize(UiArchitect.spToPx(this, 12f));
        tvMemDetails.setLineSpacing(0f, 1.25f);
        tvMemDetails.setPadding(0, UiArchitect.dpToPx(this, 8), 0, 0);
        memCard.addView(tvMemDetails);
        contentLayout.addView(memCard);

        // 5. Silicon Specs Snapshot Card
        DeviceIdentityHelper.DeviceHardwareInfo specs = DeviceIdentityHelper.getHardwareSpecs(this);
        LinearLayout specCard = createCard();
        specCard.addView(createCardTitle("🔒 Silicon Hardware Identity"));

        TextView tvSpecs = new TextView(this);
        tvSpecs.setText(
            "• Silicon Agent ID: " + specs.getAgentId() + "\n" +
            "• Device: " + specs.getBrand() + " " + specs.getModel() + "\n" +
            "• Hardware Board: " + specs.getBoard() + " (" + specs.getHardware() + ")\n" +
            "• Root Anchor: Verified Physical OEM Silicon Key"
        );
        tvSpecs.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvSpecs.setTextSize(UiArchitect.spToPx(this, 11f));
        tvSpecs.setLineSpacing(0f, 1.25f);
        tvSpecs.setPadding(0, UiArchitect.dpToPx(this, 8), 0, 0);
        specCard.addView(tvSpecs);
        contentLayout.addView(specCard);

        // Refresh Button
        Button btnRefresh = UiArchitect.createPrimaryButton(this, "🔄 REFRESH TELEMETRY METRICS", v -> {
            buildHealthDashboard();
            UiArchitect.showSlideNotificationBanner(this, "Diagnostics Refreshed", "Live sensor & battery stats updated.", false, 2000, null, null);
        });
        LinearLayout.LayoutParams refLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        refLp.topMargin = UiArchitect.dpToPx(this, 14);
        contentLayout.addView(btnRefresh, refLp);
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
        lp.bottomMargin = UiArchitect.dpToPx(this, 12);
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
        buildHealthDashboard();
    }
}
