package com.jdroidx.jbutterfly.ui.management;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.jdroidx.jbutterfly.data.local.DatabaseHelper;
import com.jdroidx.jbutterfly.data.model.LiveDeviceModel;
import com.jdroidx.jbutterfly.data.model.UserLocation;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.ui.map.MapActivity;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {

    private Spinner spDevice;
    private Spinner spLimit;
    private LinearLayout listContainer;
    private final List<String> deviceAgentIds = new ArrayList<>();
    private String selectedAgentId = "";
    private int selectedLimit = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        String targetAgent = getIntent().getStringExtra("TARGET_AGENT_ID");
        String myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        selectedAgentId = (targetAgent != null && !targetAgent.isEmpty()) ? targetAgent : myAgentId;

        buildLayout();
        refreshHistory();
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        // Header
        root.addView(UiArchitect.createHeaderBar(this, "Location History Trails", v -> finish()));

        int pad = UiArchitect.dpToPx(this, 16);

        // Filter Controls Card
        LinearLayout filterCard = new LinearLayout(this);
        filterCard.setOrientation(LinearLayout.VERTICAL);
        filterCard.setPadding(pad, pad, pad, pad);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(ThemeManager.getSurfaceColor(this));
        cardBg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        cardBg.setCornerRadius(UiArchitect.dpToPx(this, 12));
        filterCard.setBackground(cardBg);

        LinearLayout.LayoutParams fcLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        fcLp.setMargins(pad, pad, pad, UiArchitect.dpToPx(this, 10));
        filterCard.setLayoutParams(fcLp);

        // Device Selection Dropdown
        TextView tvDevLabel = new TextView(this);
        tvDevLabel.setText("Select Device to Review:");
        tvDevLabel.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvDevLabel.setTextSize(UiArchitect.spToPx(this, 12f));
        tvDevLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        filterCard.addView(tvDevLabel);

        spDevice = new Spinner(this);
        loadDeviceSpinnerOptions();
        spDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < deviceAgentIds.size()) {
                    selectedAgentId = deviceAgentIds.get(position);
                    refreshHistory();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        filterCard.addView(spDevice);

        // Row with Record Limit and Clear Button
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, UiArchitect.dpToPx(this, 10), 0, 0);

        TextView tvLimitLabel = new TextView(this);
        tvLimitLabel.setText("Points:");
        tvLimitLabel.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvLimitLabel.setTextSize(UiArchitect.spToPx(this, 12f));
        row.addView(tvLimitLabel);

        spLimit = new Spinner(this);
        ArrayAdapter<String> limitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"25 Points", "50 Points", "100 Points"});
        spLimit.setAdapter(limitAdapter);
        spLimit.setSelection(1);
        spLimit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) selectedLimit = 25;
                else if (position == 1) selectedLimit = 50;
                else if (position == 2) selectedLimit = 100;
                refreshHistory();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(spLimit, spLp);

        Button btnClear = UiArchitect.createDestructiveButton(this, "CLEAR HISTORY", v -> {
            UiArchitect.showConfirmationDialog(this,
                "Clear Device History",
                "Purge all recorded GPS breadcrumbs for " + selectedAgentId + "?",
                "PURGE", "CANCEL",
                () -> {
                    DatabaseHelper.getInstance(this).clearAllHistory(selectedAgentId);
                    refreshHistory();
                    UiArchitect.showSlideNotificationBanner(this, "History Purged", "Trail records cleared.", false, 2500, null, null);
                }, null);
        });
        row.addView(btnClear);
        filterCard.addView(row);

        root.addView(filterCard);

        // Scrollable List
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(pad, 0, pad, UiArchitect.dpToPx(this, 30));
        scroll.addView(listContainer);

        root.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }

    private void loadDeviceSpinnerOptions() {
        deviceAgentIds.clear();
        List<String> labels = new ArrayList<>();

        String myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        deviceAgentIds.add(myAgentId);
        labels.add("Me (" + myAgentId + ") - Host Device");

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<LiveDeviceModel> paired = db.getAllPairedDevices();
        int targetPos = 0;

        for (LiveDeviceModel dev : paired) {
            if (dev.agentId.equals(myAgentId)) continue;
            deviceAgentIds.add(dev.agentId);
            labels.add(dev.deviceName + " (" + dev.agentId + ") • " + dev.category);
            if (dev.agentId.equalsIgnoreCase(selectedAgentId)) {
                targetPos = deviceAgentIds.size() - 1;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
        spDevice.setAdapter(adapter);
        spDevice.setSelection(targetPos);
    }

    private void refreshHistory() {
        listContainer.removeAllViews();

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<UserLocation> history = db.getHistoryForAgent(selectedAgentId, selectedLimit);

        if (history.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("📍 No historical coordinates recorded for " + selectedAgentId + ".\nEnsure the device is active and moving to register breadcrumbs.");
            tvEmpty.setTextColor(ThemeManager.getSecondaryTextColor(this));
            tvEmpty.setTextSize(UiArchitect.spToPx(this, 13f));
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(0, UiArchitect.dpToPx(this, 50), 0, 0);
            listContainer.addView(tvEmpty);
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        int pad = UiArchitect.dpToPx(this, 14);

        for (int i = 0; i < history.size(); i++) {
            UserLocation loc = history.get(i);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(pad, pad, pad, pad);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ThemeManager.getSurfaceColor(this));
            bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
            bg.setCornerRadius(UiArchitect.dpToPx(this, 10));
            item.setBackground(bg);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvTime = new TextView(this);
            tvTime.setText("#" + (i + 1) + " • 🕒 " + df.format(new Date(loc.timestamp)));
            tvTime.setTextColor(ThemeManager.getPrimaryTextColor(this));
            tvTime.setTextSize(UiArchitect.spToPx(this, 13f));
            tvTime.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            topRow.addView(tvTime, timeLp);

            TextView tvPill = UiArchitect.createStatusPill(this, String.format(Locale.US, "%.1f m/s", loc.speed), "#3B82F6");
            topRow.addView(tvPill);
            item.addView(topRow);

            TextView tvCoord = new TextView(this);
            tvCoord.setText("Lat: " + String.format(Locale.US, "%.5f", loc.latitude) + ", Lng: " + String.format(Locale.US, "%.5f", loc.longitude) + " • Accuracy: ±" + String.format(Locale.US, "%.1f", loc.accuracy) + "m");
            tvCoord.setTextColor(ThemeManager.getSecondaryTextColor(this));
            tvCoord.setTextSize(UiArchitect.spToPx(this, 11f));
            tvCoord.setPadding(0, UiArchitect.dpToPx(this, 6), 0, UiArchitect.dpToPx(this, 8));
            item.addView(tvCoord);

            Button btnShow = UiArchitect.createSecondaryButton(this, "🛰️ SHOW ON RADAR MAP", v -> {
                Intent mapIntent = new Intent(this, MapActivity.class);
                mapIntent.putExtra("TARGET_AGENT_ID", loc.agentId);
                mapIntent.putExtra("TARGET_LAT", loc.latitude);
                mapIntent.putExtra("TARGET_LNG", loc.longitude);
                startActivity(mapIntent);
            });
            item.addView(btnShow);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = UiArchitect.dpToPx(this, 10);
            item.setLayoutParams(lp);

            listContainer.addView(item);
        }
    }
}
