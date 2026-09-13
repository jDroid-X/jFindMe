package com.jdroidx.jbutterfly.ui.management;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.jdroidx.jbutterfly.data.local.DatabaseHelper;
import com.jdroidx.jbutterfly.data.model.NotificationItem;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotifActivity extends Activity {

    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        // Header with Clear All button
        LinearLayout header = UiArchitect.createHeaderBar(this, "Incident Alerts Ledger", v -> finish());
        root.addView(header);

        // Subheader with Clear Action
        LinearLayout subRow = new LinearLayout(this);
        subRow.setOrientation(LinearLayout.HORIZONTAL);
        subRow.setGravity(Gravity.CENTER_VERTICAL);
        int pad = UiArchitect.dpToPx(this, 16);
        subRow.setPadding(pad, UiArchitect.dpToPx(this, 8), pad, UiArchitect.dpToPx(this, 8));

        TextView tvSub = new TextView(this);
        tvSub.setText("Live security & geofence notifications");
        tvSub.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvSub.setTextSize(UiArchitect.spToPx(this, 12f));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        subRow.addView(tvSub, subLp);

        Button btnClear = UiArchitect.createSecondaryButton(this, "CLEAR ALL", v -> {
            UiArchitect.showConfirmationDialog(this,
                "Clear Notifications",
                "Purge all recorded incident entries?",
                "PURGE", "CANCEL",
                () -> {
                    DatabaseHelper.getInstance(this).clearAllNotifications();
                    refreshLedger();
                    UiArchitect.showSlideNotificationBanner(this, "Ledger Cleared", "All incident notifications purged.", false, 2500, null, null);
                }, null);
        });
        subRow.addView(btnClear);
        root.addView(subRow);

        ScrollView scroll = new ScrollScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(pad, 0, pad, UiArchitect.dpToPx(this, 24));
        scroll.addView(listContainer);

        root.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
        refreshLedger();
    }

    private void refreshLedger() {
        listContainer.removeAllViews();
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<NotificationItem> items = db.getAllNotifications();

        if (items.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("🛡️ No incidents recorded yet.\nSentinel background monitoring is active & nominal.");
            tvEmpty.setTextColor(ThemeManager.getSecondaryTextColor(this));
            tvEmpty.setTextSize(UiArchitect.spToPx(this, 13f));
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(0, UiArchitect.dpToPx(this, 60), 0, 0);
            listContainer.addView(tvEmpty);
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US);
        for (NotificationItem item : items) {
            listContainer.addView(createAlertCard(item, df.format(new Date(item.timestamp))));
        }
    }

    private View createAlertCard(NotificationItem item, String timeStr) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 14);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(this));
        bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        bg.setCornerRadius(UiArchitect.dpToPx(this, 10));
        card.setBackground(bg);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        String icon = "⚙️";
        String tagColor = "#64748B";
        if ("GEOFENCE".equalsIgnoreCase(item.category)) {
            icon = "🛡️";
            tagColor = "#3B82F6";
        } else if ("SOS".equalsIgnoreCase(item.category)) {
            icon = "🚨";
            tagColor = "#EF4444";
        } else if ("BATTERY".equalsIgnoreCase(item.category)) {
            icon = "🔋";
            tagColor = "#F59E0B";
        } else if ("PAIRING".equalsIgnoreCase(item.category)) {
            icon = "🔗";
            tagColor = "#10B981";
        }

        TextView tvTitle = new TextView(this);
        tvTitle.setText(icon + " " + item.title);
        tvTitle.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvTitle.setTextSize(UiArchitect.spToPx(this, 13f));
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        topRow.addView(tvTitle, titleLp);

        TextView tvPill = UiArchitect.createStatusPill(this, item.category, tagColor);
        topRow.addView(tvPill);
        card.addView(topRow);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(item.message);
        tvDesc.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvDesc.setTextSize(UiArchitect.spToPx(this, 12f));
        tvDesc.setPadding(0, UiArchitect.dpToPx(this, 6), 0, UiArchitect.dpToPx(this, 4));
        card.addView(tvDesc);

        TextView tvTime = new TextView(this);
        tvTime.setText("🕒 " + timeStr);
        tvTime.setTextColor(ThemeManager.getBrandColor(this));
        tvTime.setTextSize(UiArchitect.spToPx(this, 10f));
        card.addView(tvTime);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = UiArchitect.dpToPx(this, 10);
        card.setLayoutParams(lp);

        return card;
    }

    private static class ScrollScrollView extends ScrollView {
        public ScrollScrollView(Activity context) {
            super(context);
        }
    }
}
