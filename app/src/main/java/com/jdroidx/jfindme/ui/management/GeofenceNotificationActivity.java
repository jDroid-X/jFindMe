package com.jdroidx.jfindme.ui.management;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Geofencing Notification Interface (L7 Hardened - Master Single-Row Architecture)
 * High-Density Forensic Reporting Hub for Mission-Critical Transitions.
 */
public class GeofenceNotificationActivity extends com.jdroidx.jfindme.ui.base.BaseActivityJava {

    private GeofenceNotificationViewModel viewModel;
    private LinearLayout container;
    private LinearLayout filterBar;
    private String targetAvatar;
    private String targetZoneId;
    private int activeFilter = 0; // 0: ALL, 1: IN, 2: OUT, 3: CHATS
    private List<GeofenceNotification> currentList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        targetAvatar = getIntent().getStringExtra("TARGET_AVATAR");
        targetZoneId = getIntent().getStringExtra("TARGET_ZONE_ID");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // 🚀 L7 MASTER HEADER: High-Density Mission Reporting Hub (V49)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        applyGlobalHeaderGeometry(header);

        ImageButton btnBack = new ImageButton(this);
        btnBack.setImageResource(android.R.drawable.ic_menu_revert);
        btnBack.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnBack.setColorFilter(getGradientOverlayText());
        btnBack.setOnClickListener(v -> finish());
        header.addView(btnBack);

        LinearLayout titleBundle = new LinearLayout(this);
        titleBundle.setOrientation(LinearLayout.VERTICAL);
        titleBundle.setPadding(scale(20), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("MISSION NOTIFICATIONS");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_LABEL");
        titleBundle.addView(title);

        TextView subTitle = new TextView(this);
        subTitle.setText("FORENSIC ANALYTIC LEDGER");
        subTitle.setTextColor(getGradientOverlayText());
        subTitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subTitle, "TEXT_TINY");
        titleBundle.addView(subTitle);

        header.addView(titleBundle);
        root.addView(header);

        // Filter Matrix
        filterBar = new LinearLayout(this);
        filterBar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(createFilterBarWrapper());

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int p = getResponsivePaddingQuota();
        container.setPadding(p, p/2, p, p/2);
        scroll.addView(container);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        return root;
    }

    private View createHeader(String title, View.OnClickListener backListener) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        int p = getResponsivePaddingQuota();
        header.setPadding(p, scale(70), p, p/2); // Final calibration for L7 viewport
        header.setBackgroundColor(getPrimaryColor());

        ImageButton back = new ImageButton(this);
        back.setImageResource(android.R.drawable.ic_menu_revert);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setColorFilter(Color.WHITE);
        back.setOnClickListener(backListener);
        header.addView(back, new LinearLayout.LayoutParams(scale(60), scale(60)));

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(p, 0, 0, 0);
        UiArchitect.applyText(this, tv, "TEXT_HEAD");
        header.addView(tv);

        return header;
    }

    private View createFilterBarWrapper() {
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        refreshFilterBar();
        hScroll.addView(filterBar);
        return hScroll;
    }

    private void refreshFilterBar() {
        if (filterBar == null) return;
        filterBar.removeAllViews();
        int p = getResponsivePaddingQuota();
        filterBar.setPadding(p, p/4, p, p/4);

        String[] filters = {"ALL LOGS", "IN (GREEN)", "OUT (RED)", "HISTORY"};
        for (int i = 0; i < filters.length; i++) {
            final int idx = i;
            TextView tab = new TextView(this);
            tab.setText(filters[i]);
            tab.setPadding(p, p/4, p, p/4);
            tab.setGravity(Gravity.CENTER);
            
            if (activeFilter == i) {
                tab.setTextColor(Color.WHITE);
                tab.setBackgroundColor(getGradientStart());
            } else {
                tab.setTextColor(getSecondaryTextColor());
                tab.setBackgroundColor(Color.TRANSPARENT);
            }
            
            UiArchitect.applyText(this, tab, "TEXT_TINY");
            tab.setOnClickListener(v -> {
                activeFilter = idx;
                refreshFilterBar();
                renderNotifications(currentList);
            });
            filterBar.addView(tab);
        }
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(GeofenceNotificationViewModel.class);
        viewModel.getNotifications().observe(this, list -> {
            this.currentList = list;
            renderNotifications(list);
        });
        viewModel.fetchNotifications(targetAvatar, targetZoneId);
    }

    private void renderNotifications(List<GeofenceNotification> list) {
        if (container == null) return;
        container.removeAllViews();

        if (list == null) list = new ArrayList<>();
        
        // 🚀 ANALYTIC GUARD: Only display samples if the forensic local archival is completely sterile
        if (list.isEmpty() && targetAvatar == null && targetZoneId == null) {
            list.add(new GeofenceNotification("S1", "SUSAN", "IN", System.currentTimeMillis() - 3600000, "HOME", ""));
            list.add(new GeofenceNotification("S2", "SUSAN", "OUT", System.currentTimeMillis() - 7200000, "SCHOOL", ""));
            list.add(new GeofenceNotification("S3", "JOHN", "IN", System.currentTimeMillis() - 10800000, "SCHOOL", ""));
        }

        for (GeofenceNotification n : list) {
            boolean isEntry = n.getStatus().equalsIgnoreCase("IN") || n.getStatus().equalsIgnoreCase("ENTERED");
            boolean isExit = n.getStatus().equalsIgnoreCase("OUT") || n.getStatus().equalsIgnoreCase("EXITED");

            if (activeFilter == 1 && !isEntry) continue; // 🟢 'IN' Filter: Show both IN and ENTERED
            if (activeFilter == 2 && !isExit) continue;  // 🔴 'OUT' Filter: Show both OUT and EXITED
            
            container.addView(createForensicRowTile(n));
        }
    }

    private View createForensicRowTile(GeofenceNotification n) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int p = getResponsivePaddingQuota();
        row.setPadding(p, p/2, p, p/2);
        
        UiArchitect.applyCardWithOverride(this, row, "CARD_STANDARD", 1.2f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, scale(12));
        row.setLayoutParams(lp);

        // 🚀 1. Email Icon (Forensic Connectivity Hub)
        ImageView ivMail = new ImageView(this);
        ivMail.setImageResource(android.R.drawable.ic_dialog_email);
        ivMail.setColorFilter(getGradientStart());
        int sSize = UiArchitect.getResponsiveIconSize(this, "ICON_TINY");
        row.addView(ivMail, new LinearLayout.LayoutParams(sSize, sSize));

        // 2. Identity & Message (Dynamic Color Gradient Matrix - V49.2)
        TextView tvMsg = new TextView(this);
        String agentName = (n.getAvatarName() == null || n.getAvatarName().isEmpty() || n.getAvatarName().equals("Unknown")) ? "MISSION ASSET" : n.getAvatarName().toUpperCase();
        
        String action;
        int statusColor;
        boolean highlightActionOnly = true;
        
        if (n.getStatus().equalsIgnoreCase("IN") || n.getStatus().equalsIgnoreCase("ENTERED")) {
            action = " ARRIVED AT ";
            statusColor = Color.parseColor("#4CAF50"); // Tactical Green
        } else if (n.getStatus().equalsIgnoreCase("OUT") || n.getStatus().equalsIgnoreCase("EXITED")) {
            action = " LEFT ";
            statusColor = Color.parseColor("#F44336"); // Alert Red
        } else if (n.getStatus().equalsIgnoreCase("DEPLOYED")) {
            action = " MISSION SYNC SENT: ";
            statusColor = getGradientStart(); // Theme Primary (Blue)
            highlightActionOnly = false;
        } else {
            action = " - " + n.getStatus().toUpperCase() + " - ";
            statusColor = getSecondaryTextColor();
            highlightActionOnly = false;
        }
        
        String combined = agentName + action + n.getZoneName();
        
        if (highlightActionOnly) {
            android.text.SpannableString span = new android.text.SpannableString(combined);
            int start = agentName.length();
            int end = start + action.length();
            span.setSpan(new android.text.style.ForegroundColorSpan(statusColor), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvMsg.setText(span);
            tvMsg.setTextColor(getTextColor());
        } else {
            tvMsg.setText(combined);
            tvMsg.setTextColor(statusColor);
        }
        tvMsg.setPadding(p, 0, p, 0);
        // 🚀 MISSION VISIBILITY: Enable multi-line wrapping for long Zone names / Identities
        tvMsg.setSingleLine(false);
        tvMsg.setHorizontallyScrolling(false);
        UiArchitect.applyText(this, tvMsg, "TEXT_SUB");
        row.addView(tvMsg, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // 3. Time Signature
        TextView tvTime = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvTime.setText(sdf.format(new Date(n.getTimestamp())));
        tvTime.setTextColor(getSecondaryTextColor());
        tvTime.setPadding(0, 0, p, 0);
        UiArchitect.applyText(this, tvTime, "TEXT_MINI");
        row.addView(tvTime);

        // 4. Map Action Icon (Situational Awareness)
        ImageView ivMap = new ImageView(this);
        ivMap.setImageResource(android.R.drawable.ic_dialog_map);
        ivMap.setColorFilter(getGradientStart());
        ivMap.setOnClickListener(v -> {}); // 0.0, 0.0 Analytics
        row.addView(ivMap, new LinearLayout.LayoutParams(sSize, sSize));

        return row;
    }
}
