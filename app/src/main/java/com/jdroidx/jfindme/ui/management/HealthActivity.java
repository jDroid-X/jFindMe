package com.jdroidx.jfindme.ui.management;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
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
    private TextView tvBattery, tvGps, tvNetwork, tvStorage, tvVitalsBody, tvHwBody;
    private String targetAgentId;
    private String targetMemberName;
    private boolean isRemoteMember = false;

    @Override
    protected View createLayout() {
        targetAgentId = getIntent().getStringExtra("EXTRA_AGENT_ID");
        if (targetAgentId == null) targetAgentId = getIntent().getStringExtra("target_agent_id");
        targetMemberName = getIntent().getStringExtra("EXTRA_MEMBER_NAME");
        if (targetMemberName == null) targetMemberName = getIntent().getStringExtra("target_name");

        String myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        if (targetAgentId != null && !targetAgentId.trim().isEmpty() && !targetAgentId.equals(myAgentId)) {
            isRemoteMember = true;
        } else {
            targetAgentId = myAgentId;
            isRemoteMember = false;
        }

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
        
        android.widget.ImageView backBtn = new android.widget.ImageView(this);
        backBtn.setImageResource(android.R.drawable.ic_menu_revert);
        backBtn.setColorFilter(getGradientOverlayText());
        int bSize = scale(120);
        backBtn.setPadding(scale(20), scale(20), scale(20), scale(20));
        backBtn.setOnClickListener(v -> finish());
        topRow.addView(backBtn, new LinearLayout.LayoutParams(bSize, bSize));

        TextView title = new TextView(this);
        title.setText("DEVICE HEALTH");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        if (isRemoteMember) {
            subtitle.setText((targetMemberName != null ? targetMemberName : "Member") + " • Node [" + targetAgentId + "]");
        } else {
            subtitle.setText("Hardware Diagnostics & Core Operations");
        }
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(scale(20), 10, 0, 0);
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
        
        boolean isTrackingEnabled = prefs.getBoolean("tracking_service_enabled", true);
        opCard.addView(createMetricRow("Tracking Engine", isTrackingEnabled ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"), v -> v.setText(isTrackingEnabled ? "ENABLED (SENTINEL)" : "DISABLED")));
        
        long totalFixes = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getRecordCount("location_event");
        opCard.addView(createMetricRow("Location Ledger", getGradientStart(), v -> v.setText(totalFixes + " POINTS INDEXED")));
        
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

        // --- LAYER 2.6: UNIT SYSTEM TOGGLE ---
        content.addView(createSectionHeader("DISPLAY UNITS"));
        boolean isMetric = prefs.getBoolean("pref_metric_units", true);
        View unitToggle = UiArchitect.createSegmentedToggle(this, "METRIC (KM/H)", "IMPERIAL (MPH)", isMetric, isLeft -> {
            prefs.edit().putBoolean("pref_metric_units", isLeft).apply();
            UiArchitect.showSlideNotificationBanner(
                this,
                "Telemetry Units Mutated",
                isLeft ? "Velocity: km/h | Elevation: meters" : "Velocity: mph | Elevation: feet",
                false,
                2500,
                null,
                null
            );
        });
        content.addView(unitToggle, cardLp);

        // --- LAYER 2.7: DEEP SENSOR TELEMETRY ACCORDION ---
        content.addView(createSectionHeader("DEEP HARDWARE VITALS"));
        String myHwId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(this);
        java.util.Map<String, Object> vitals = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getDeviceVitals(myHwId);
        int pendingQueueCount = com.jdroidx.jfindme.data.remote.OfflineSyncManager.getInstance(this).getPendingQueueCount();

        StringBuilder vitalsText = new StringBuilder();
        vitalsText.append("• Cellular Signal: ").append(vitals != null ? vitals.get("signal_dbm") + " dBm" : "Optimal RSSI").append("\n");
        vitalsText.append("• Network Interface: ").append(vitals != null ? vitals.get("network_type") : "Cellular/WiFi Bridge").append("\n");
        boolean isMock = vitals != null && (vitals.get("is_mock_gps") instanceof Number ? ((Number) vitals.get("is_mock_gps")).intValue() == 1 : Boolean.TRUE.equals(vitals.get("is_mock_gps")));
        boolean isRoot = vitals != null && (vitals.get("is_rooted") instanceof Number ? ((Number) vitals.get("is_rooted")).intValue() == 1 : Boolean.TRUE.equals(vitals.get("is_rooted")));
        vitalsText.append("• Mock GPS Spoofing: ").append(isMock ? "⚠️ DETECTED" : "✅ CLEAN").append("\n");
        vitalsText.append("• Root Anchor: ").append(isRoot ? "⚠️ TRIPPED" : "✅ SECURE").append("\n");
        vitalsText.append("• Offline Vault Queue: ").append(pendingQueueCount).append(" PENDING BREADCRUMBS");

        tvVitalsBody = new TextView(this);
        tvVitalsBody.setText(vitalsText.toString());
        tvVitalsBody.setTextColor(getSecondaryTextColor());
        tvVitalsBody.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
        tvVitalsBody.setLineSpacing(scale(4), 1.0f);

        View vitalsAccordion = UiArchitect.createExpandableListBox(
            this,
            "Hardware Sensor Dossier",
            "Tap to inspect real-time sensor vitals",
            android.R.drawable.ic_menu_info_details,
            tvVitalsBody
        );
        content.addView(vitalsAccordion, cardLp);

        // --- LAYER 2.8: SILICON & TELEPHONY DOSSIER ---
        tvHwBody = new TextView(this);
        tvHwBody.setTextColor(getSecondaryTextColor());
        tvHwBody.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_MINI"));
        tvHwBody.setLineSpacing(scale(4), 1.0f);

        loadHardwareDossier();

        View hwAccordion = UiArchitect.createExpandableListBox(
            this,
            "Physical Silicon & Telephony Dossier",
            "Brand, Board, Model, Chipset & IMEI Telemetry",
            android.R.drawable.ic_menu_agenda,
            tvHwBody
        );
        content.addView(hwAccordion, cardLp);

        // --- LAYER 3: ACTION CONTROLLER ---
        View btnRefresh = UiArchitect.createPrimaryButton(this, "RERUN SYSTEM DIAGNOSTICS", android.R.drawable.ic_popup_sync, v -> {
            updateMetrics();
            UiArchitect.showSlideNotificationBanner(this, "Diagnostics Refreshed", "Hardware & network diagnostics updated successfully.", false, 3000, null, null);
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

    private void loadHardwareDossier() {
        if (tvHwBody == null) return;
        if (isRemoteMember) {
            // 1. Load from local SQLite cache
            java.util.Map<String, Object> localDossier = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getForensicDossierComplete(targetAgentId);
            renderHardwareMap(localDossier);

            // 2. Query cloud device node
            com.jdroidx.jfindme.data.remote.FirebaseComm.getDevicesRef()
                .child(targetAgentId)
                .child("device")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        java.util.Map<String, Object> cloudDossier = new java.util.HashMap<>();
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            cloudDossier.put(child.getKey(), child.getValue());
                        }
                        renderHardwareMap(cloudDossier);
                    }
                });
        } else {
            com.jdroidx.jfindme.utils.DeviceIdentityHelper.DeviceHardwareInfo hw = 
                com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareSpecs(this);
            StringBuilder hwText = new StringBuilder();
            hwText.append("• Device Name: ").append(hw.getDeviceName()).append("\n");
            hwText.append("• Brand / OEM: ").append(hw.getBrand()).append("\n");
            hwText.append("• Model: ").append(hw.getModel()).append("\n");
            hwText.append("• Motherboard: ").append(hw.getBoard()).append("\n");
            hwText.append("• Hardware Chipset: ").append(hw.getHardware()).append("\n");
            hwText.append("• SIM Configuration: ").append(hw.isDualSim() ? "Dual SIM Architecture (2 Modems)" : "Single SIM Node").append("\n");
            hwText.append("• Primary IMEI: ").append(hw.getImei1().isEmpty() ? "UNAVAILABLE" : hw.getImei1());
            if (hw.isDualSim()) {
                hwText.append("\n• Secondary IMEI: ").append(hw.getImei2().isEmpty() ? "UNAVAILABLE" : hw.getImei2());
            }
            tvHwBody.setText(hwText.toString());
        }
    }

    private void renderHardwareMap(java.util.Map<String, Object> map) {
        if (tvHwBody == null || map == null || map.isEmpty()) return;
        String devName = map.containsKey("device_name") && map.get("device_name") != null ? map.get("device_name").toString() : (targetMemberName != null ? targetMemberName : "Remote Device");
        String brand = map.containsKey("brand") && map.get("brand") != null ? map.get("brand").toString() : "UNKNOWN";
        String model = map.containsKey("model") && map.get("model") != null ? map.get("model").toString() : (map.containsKey("device_model") && map.get("device_model") != null ? map.get("device_model").toString() : "UNKNOWN");
        String board = map.containsKey("board") && map.get("board") != null ? map.get("board").toString() : "UNKNOWN";
        String hardware = map.containsKey("hardware") && map.get("hardware") != null ? map.get("hardware").toString() : "UNKNOWN";
        boolean isDual = Boolean.TRUE.equals(map.get("is_dual_sim")) || "true".equalsIgnoreCase(String.valueOf(map.get("is_dual_sim"))) || (map.get("is_dual_sim") instanceof Number && ((Number) map.get("is_dual_sim")).intValue() == 1);
        String imei1 = map.containsKey("imei_1") && map.get("imei_1") != null ? map.get("imei_1").toString() : (map.containsKey("imei") && map.get("imei") != null ? map.get("imei").toString() : "UNAVAILABLE");
        String imei2 = map.containsKey("imei_2") && map.get("imei_2") != null ? map.get("imei_2").toString() : "UNAVAILABLE";

        StringBuilder hwText = new StringBuilder();
        hwText.append("• Device Name: ").append(devName).append("\n");
        hwText.append("• Brand / OEM: ").append(brand).append("\n");
        hwText.append("• Model: ").append(model).append("\n");
        hwText.append("• Motherboard: ").append(board).append("\n");
        hwText.append("• Hardware Chipset: ").append(hardware).append("\n");
        hwText.append("• SIM Configuration: ").append(isDual ? "Dual SIM Architecture (2 Modems)" : "Single SIM Node").append("\n");
        hwText.append("• Primary IMEI: ").append(imei1.isEmpty() ? "UNAVAILABLE" : imei1);
        if (isDual) {
            hwText.append("\n• Secondary IMEI: ").append(imei2.isEmpty() ? "UNAVAILABLE" : imei2);
        }
        tvHwBody.setText(hwText.toString());
    }

    private void updateRemoteMetrics() {
        if (targetAgentId == null) return;
        com.jdroidx.jfindme.data.remote.FirebaseComm.getDevicesRef()
            .child(targetAgentId)
            .child("device")
            .child("live")
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    Long bat = snapshot.child("battery_level").getValue(Long.class);
                    Boolean charging = snapshot.child("is_charging").getValue(Boolean.class);
                    String net = snapshot.child("network_type").getValue(String.class);
                    Boolean mock = snapshot.child("is_mock_gps").getValue(Boolean.class);
                    Boolean root = snapshot.child("is_rooted").getValue(Boolean.class);
                    Long signal = snapshot.child("signal_dbm").getValue(Long.class);

                    if (tvBattery != null && bat != null) {
                        tvBattery.setText(bat + "% " + (Boolean.TRUE.equals(charging) ? "(⚡ CHARGING)" : "(DISCHARGING)"));
                        tvBattery.setTextColor(bat > 20 ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
                    }
                    if (tvNetwork != null && net != null) {
                        tvNetwork.setText(net + " (SYNCED)");
                    }
                    if (tvGps != null) {
                        tvGps.setText("LIVE TRACKING ACTIVE");
                        tvGps.setTextColor(Color.parseColor("#4CAF50"));
                    }
                    if (tvStorage != null) {
                        tvStorage.setText("REMOTE STORAGE MANAGED");
                    }
                    if (tvVitalsBody != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("• Cellular Signal: ").append(signal != null ? signal + " dBm" : "Optimal RSSI").append("\n");
                        sb.append("• Network Interface: ").append(net != null ? net : "Cellular/WiFi Bridge").append("\n");
                        sb.append("• Mock GPS Spoofing: ").append(Boolean.TRUE.equals(mock) ? "⚠️ DETECTED" : "✅ CLEAN").append("\n");
                        sb.append("• Root Anchor: ").append(Boolean.TRUE.equals(root) ? "⚠️ TRIPPED" : "✅ SECURE").append("\n");
                        sb.append("• Cloud Telemetry: ACTIVE STREAM");
                        tvVitalsBody.setText(sb.toString());
                    }
                }
            });
    }

    private void updateMetrics() {
        if (isRemoteMember) {
            updateRemoteMetrics();
            loadHardwareDossier();
            return;
        }

        // 1. Battery Metric & Charging State — use sticky broadcast (no receiver leak)
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
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

        // 5. Deep Hardware Vitals Refresh
        if (tvVitalsBody != null) {
            String myHwId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(this);
            java.util.Map<String, Object> vitals = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this).getDeviceVitals(myHwId);
            int pendingQueueCount = com.jdroidx.jfindme.data.remote.OfflineSyncManager.getInstance(this).getPendingQueueCount();

            StringBuilder vitalsText = new StringBuilder();
            vitalsText.append("• Cellular Signal: ").append(vitals != null && vitals.containsKey("signal_dbm") ? vitals.get("signal_dbm") + " dBm" : "Optimal RSSI").append("\n");
            vitalsText.append("• Network Interface: ").append(vitals != null && vitals.containsKey("network_type") ? vitals.get("network_type") : "Cellular/WiFi Bridge").append("\n");
            boolean isMock = vitals != null && (vitals.get("is_mock_gps") instanceof Number ? ((Number) vitals.get("is_mock_gps")).intValue() == 1 : Boolean.TRUE.equals(vitals.get("is_mock_gps")));
            boolean isRoot = vitals != null && (vitals.get("is_rooted") instanceof Number ? ((Number) vitals.get("is_rooted")).intValue() == 1 : Boolean.TRUE.equals(vitals.get("is_rooted")));
            vitalsText.append("• Mock GPS Spoofing: ").append(isMock ? "⚠️ DETECTED" : "✅ CLEAN").append("\n");
            vitalsText.append("• Root Anchor: ").append(isRoot ? "⚠️ TRIPPED" : "✅ SECURE").append("\n");
            vitalsText.append("• Offline Vault Queue: ").append(pendingQueueCount).append(" PENDING BREADCRUMBS");

            tvVitalsBody.setText(vitalsText.toString());
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
        updateMetrics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMetrics();
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
