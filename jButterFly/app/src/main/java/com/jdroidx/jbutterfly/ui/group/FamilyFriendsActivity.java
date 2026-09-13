package com.jdroidx.jbutterfly.ui.group;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.jdroidx.jbutterfly.R;
import com.jdroidx.jbutterfly.data.local.DatabaseHelper;
import com.jdroidx.jbutterfly.data.model.DeviceRagStatus;
import com.jdroidx.jbutterfly.data.model.LiveDeviceModel;
import com.jdroidx.jbutterfly.data.remote.FirebaseComm;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.ui.management.HistoryActivity;
import com.jdroidx.jbutterfly.ui.map.MapActivity;
import com.jdroidx.jbutterfly.utils.AvatarLoader;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;
import com.jdroidx.jbutterfly.utils.SystemLogger;
import java.util.ArrayList;
import java.util.List;

public class FamilyFriendsActivity extends Activity {

    private static final String TAG = "FamilyFriendsActivity";
    private String currentCategory = "FAMILY"; // FAMILY or FRIENDS
    private LinearLayout listContainer;
    private Button btnTabFamily;
    private Button btnTabFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        String passedCat = getIntent().getStringExtra("CATEGORY");
        if (passedCat != null && !passedCat.isEmpty()) {
            currentCategory = passedCat;
        }

        buildLayout();
        refreshDeviceList();
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        // Header
        LinearLayout header = UiArchitect.createHeaderBar(this, "Family & Friends Circle", v -> finish());
        root.addView(header);

        // Segmented Tabs: Family vs Friends
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        int pad = UiArchitect.dpToPx(this, 12);
        tabRow.setPadding(pad, pad, pad, pad);

        btnTabFamily = new Button(this);
        btnTabFamily.setText("👨‍👩‍👦 FAMILY CIRCLE");
        btnTabFamily.setTextSize(UiArchitect.spToPx(this, 12f));
        btnTabFamily.setOnClickListener(v -> {
            currentCategory = "FAMILY";
            updateTabStyles();
            refreshDeviceList();
        });
        LinearLayout.LayoutParams famLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        famLp.rightMargin = UiArchitect.dpToPx(this, 6);
        tabRow.addView(btnTabFamily, famLp);

        btnTabFriends = new Button(this);
        btnTabFriends.setText("👥 FRIENDS NETWORK");
        btnTabFriends.setTextSize(UiArchitect.spToPx(this, 12f));
        btnTabFriends.setOnClickListener(v -> {
            currentCategory = "FRIENDS";
            updateTabStyles();
            refreshDeviceList();
        });
        LinearLayout.LayoutParams frndLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tabRow.addView(btnTabFriends, frndLp);

        root.addView(tabRow);
        updateTabStyles();

        // Scrollable List Container
        ScrollView scroll = new ScrollView(this);
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

    private void updateTabStyles() {
        boolean isFam = "FAMILY".equals(currentCategory);
        btnTabFamily.setBackgroundColor(isFam ? ThemeManager.getBrandColor(this) : ThemeManager.getCardColor(this));
        btnTabFamily.setTextColor(isFam ? Color.WHITE : ThemeManager.getPrimaryTextColor(this));

        btnTabFriends.setBackgroundColor(!isFam ? ThemeManager.getBrandColor(this) : ThemeManager.getCardColor(this));
        btnTabFriends.setTextColor(!isFam ? Color.WHITE : ThemeManager.getPrimaryTextColor(this));
    }

    private void refreshDeviceList() {
        listContainer.removeAllViews();

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<LiveDeviceModel> devices = db.getDevicesByCategory(currentCategory);

        if (devices.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No connected devices in " + currentCategory + " circle.\nTap + Pair Device on the Map to link a new node.");
            tvEmpty.setTextColor(ThemeManager.getSecondaryTextColor(this));
            tvEmpty.setTextSize(UiArchitect.spToPx(this, 13f));
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(0, UiArchitect.dpToPx(this, 60), 0, 0);
            listContainer.addView(tvEmpty);
            return;
        }

        if ("FAMILY".equals(currentCategory)) {
            List<LiveDeviceModel> guardians = new ArrayList<>();
            List<LiveDeviceModel> dependents = new ArrayList<>();

            for (LiveDeviceModel d : devices) {
                if ("GUARDIAN".equalsIgnoreCase(d.role)) {
                    guardians.add(d);
                } else {
                    dependents.add(d);
                }
            }

            if (!guardians.isEmpty()) {
                listContainer.addView(createSectionHeader("👨‍👩‍👧 Guardians & Family Leaders (" + guardians.size() + ")"));
                for (LiveDeviceModel dev : guardians) {
                    listContainer.addView(createDeviceCard(dev));
                }
            }

            if (!dependents.isEmpty()) {
                listContainer.addView(createSectionHeader("👶 Children & Monitored Dependents (" + dependents.size() + ")"));
                for (LiveDeviceModel dev : dependents) {
                    listContainer.addView(createDeviceCard(dev));
                }
            }
        } else {
            listContainer.addView(createSectionHeader("👥 Peer & Social Circle (" + devices.size() + ")"));
            for (LiveDeviceModel dev : devices) {
                listContainer.addView(createDeviceCard(dev));
            }
        }
    }

    private TextView createSectionHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(ThemeManager.getBrandColor(this));
        tv.setTextSize(UiArchitect.spToPx(this, 13f));
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setPadding(0, UiArchitect.dpToPx(this, 12), 0, UiArchitect.dpToPx(this, 8));
        return tv;
    }

    private View createDeviceCard(LiveDeviceModel dev) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 16);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(this));
        bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        bg.setCornerRadius(UiArchitect.dpToPx(this, 12));
        card.setBackground(bg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.bottomMargin = UiArchitect.dpToPx(this, 12);
        card.setLayoutParams(cardLp);

        // Top Row: Avatar, Name, Hardware Node, and RAG Status
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView ivAvatar = new ImageView(this);
        int avSize = UiArchitect.dpToPx(this, 44);
        AvatarLoader.loadAvatar(this, ivAvatar, dev.avatarName);
        topRow.addView(ivAvatar, new LinearLayout.LayoutParams(avSize, avSize));

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        nameLp.leftMargin = UiArchitect.dpToPx(this, 12);

        TextView tvName = new TextView(this);
        tvName.setText(dev.deviceName);
        tvName.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvName.setTextSize(UiArchitect.spToPx(this, 14f));
        tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        nameCol.addView(tvName);

        TextView tvDetails = new TextView(this);
        tvDetails.setText("Agent ID: " + dev.agentId + " • " + dev.role + " • 🔋 " + dev.batteryPct + "%");
        tvDetails.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvDetails.setTextSize(UiArchitect.spToPx(this, 11f));
        nameCol.addView(tvDetails);

        topRow.addView(nameCol, nameLp);

        // RAG Status Badge
        TextView statusPill = UiArchitect.createStatusPill(this, dev.getRagStatus().name(), dev.getRagColorHex());
        topRow.addView(statusPill);

        card.addView(topRow);

        // Action Buttons Row: Locate, Buzz, Trails, Edit, Delete
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, UiArchitect.dpToPx(this, 12), 0, 0);

        Button btnLocate = UiArchitect.createSecondaryButton(this, "🛰️ LOCATE", v -> {
            Intent mapIntent = new Intent(this, MapActivity.class);
            mapIntent.putExtra("TARGET_AGENT_ID", dev.agentId);
            startActivity(mapIntent);
        });
        LinearLayout.LayoutParams locLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        locLp.rightMargin = UiArchitect.dpToPx(this, 6);
        actionRow.addView(btnLocate, locLp);

        Button btnTrails = UiArchitect.createSecondaryButton(this, "📜 TRAILS", v -> {
            Intent histIntent = new Intent(this, HistoryActivity.class);
            histIntent.putExtra("TARGET_AGENT_ID", dev.agentId);
            startActivity(histIntent);
        });
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        trLp.rightMargin = UiArchitect.dpToPx(this, 6);
        actionRow.addView(btnTrails, trLp);

        Button btnBuzz = UiArchitect.createDestructiveButton(this, "🚨 BUZZ", v -> {
            String myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
            UiArchitect.showSlideNotificationBanner(this, "Buzzing Device", "Sending Acoustic Siren Buzz to " + dev.deviceName + "...", false, 2500, null, null);
            FirebaseComm.sendBuzzCommand(
                dev.agentId, myAgentId,
                () -> UiArchitect.showSlideNotificationBanner(this, "Siren Active", "Emergency Buzz triggered on " + dev.deviceName + "!", true, 3500, null, null),
                err -> UiArchitect.showSlideNotificationBanner(this, "Buzz Failed", "Could not reach " + dev.deviceName + ": " + err, true, 3000, null, null)
            );
        });
        LinearLayout.LayoutParams buzzLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        buzzLp.rightMargin = UiArchitect.dpToPx(this, 6);
        actionRow.addView(btnBuzz, buzzLp);

        Button btnEdit = UiArchitect.createSecondaryButton(this, "✏️", v -> showEditDeviceDialog(dev));
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(UiArchitect.dpToPx(this, 40), LinearLayout.LayoutParams.WRAP_CONTENT);
        editLp.rightMargin = UiArchitect.dpToPx(this, 6);
        actionRow.addView(btnEdit, editLp);

        Button btnDelete = UiArchitect.createSecondaryButton(this, "✕", v -> {
            UiArchitect.showConfirmationDialog(this,
                "Remove Device",
                "Unlink " + dev.deviceName + " (" + dev.agentId + ") from your " + currentCategory + " circle?",
                "UNLINK", "CANCEL",
                () -> {
                    DatabaseHelper.getInstance(this).removeDevice(dev.agentId);
                    refreshDeviceList();
                    UiArchitect.showSlideNotificationBanner(this, "Device Unlinked", dev.deviceName + " removed from circle", true, 2500, null, null);
                }, null);
        });
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(UiArchitect.dpToPx(this, 40), LinearLayout.LayoutParams.WRAP_CONTENT);
        actionRow.addView(btnDelete, delLp);

        card.addView(actionRow);
        return card;
    }

    private void showEditDeviceDialog(LiveDeviceModel dev) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Node: " + dev.agentId);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 20);
        form.setPadding(pad, pad, pad, pad);

        TextView tvNameLabel = new TextView(this);
        tvNameLabel.setText("Friendly Name:");
        tvNameLabel.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvNameLabel.setTextSize(UiArchitect.spToPx(this, 12f));
        form.addView(tvNameLabel);

        EditText etName = UiArchitect.createInputField(this, "Device Nickname", InputType.TYPE_CLASS_TEXT);
        etName.setText(dev.deviceName);
        form.addView(etName);

        TextView tvRoleLabel = new TextView(this);
        tvRoleLabel.setText("Role in Circle:");
        tvRoleLabel.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvRoleLabel.setTextSize(UiArchitect.spToPx(this, 12f));
        tvRoleLabel.setPadding(0, UiArchitect.dpToPx(this, 10), 0, UiArchitect.dpToPx(this, 4));
        form.addView(tvRoleLabel);

        Spinner spRole = new Spinner(this);
        String[] roles = {"GUARDIAN", "CHILD", "FRIEND"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spRole.setAdapter(roleAdapter);
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equalsIgnoreCase(dev.role)) spRole.setSelection(i);
        }
        form.addView(spRole);

        TextView tvCatLabel = new TextView(this);
        tvCatLabel.setText("Network Circle Category:");
        tvCatLabel.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvCatLabel.setTextSize(UiArchitect.spToPx(this, 12f));
        tvCatLabel.setPadding(0, UiArchitect.dpToPx(this, 10), 0, UiArchitect.dpToPx(this, 4));
        form.addView(tvCatLabel);

        Spinner spCat = new Spinner(this);
        String[] cats = {"FAMILY", "FRIENDS"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats);
        spCat.setAdapter(catAdapter);
        spCat.setSelection("FAMILY".equalsIgnoreCase(dev.category) ? 0 : 1);
        form.addView(spCat);

        builder.setView(form);
        builder.setPositiveButton("SAVE CHANGES", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) newName = "Node " + dev.agentId;
            String newRole = spRole.getSelectedItem().toString();
            String newCat = spCat.getSelectedItem().toString();

            DatabaseHelper.getInstance(this).updateDeviceProfile(dev.agentId, newName, newRole, newCat, dev.avatarName);

            // Update Firebase profile
            LiveDeviceModel updated = new LiveDeviceModel(
                dev.agentId, dev.hardwareId, newName, newRole, newCat, dev.avatarName,
                dev.latitude, dev.longitude, dev.speed, dev.heading, dev.batteryPct,
                System.currentTimeMillis(), dev.isSos, dev.isGhost
            );
            FirebaseComm.publishProfile(updated);

            refreshDeviceList();
            UiArchitect.showSlideNotificationBanner(this, "Profile Saved", newName + " updated in " + newCat, false, 2500, null, null);
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDeviceList();
    }
}
