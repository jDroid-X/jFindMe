package com.jdroidx.jbutterfly.ui.settings;

import android.app.Activity;
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
import com.jdroidx.jbutterfly.data.model.LiveDeviceModel;
import com.jdroidx.jbutterfly.data.remote.FirebaseComm;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.AvatarLoader;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;
import com.jdroidx.jbutterfly.utils.RegistryManager;

public class ProfileActivity extends Activity {

    private static final int REQ_AVATAR = 3001;

    private ImageView ivCurrentAvatar;
    private EditText etNickname;
    private Spinner spRole;

    private String myAgentId = "";
    private String myHwKey = "";
    private String equippedAvatar = "avatar_custom_1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        myHwKey = DeviceIdentityHelper.getRawHardwareKey(this);
        equippedAvatar = RegistryManager.getString(this, RegistryManager.KEY_EQUIPPED_AVATAR, "avatar_custom_1");

        buildLayout();
    }

    private void buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Header
        LinearLayout header = UiArchitect.createHeaderBar(this, "Device Profile & Dossier", v -> finish());
        root.addView(header);

        int pad = UiArchitect.dpToPx(this, 16);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, pad);

        // Avatar Preview Card
        LinearLayout avCard = new LinearLayout(this);
        avCard.setOrientation(LinearLayout.VERTICAL);
        avCard.setGravity(Gravity.CENTER_HORIZONTAL);
        avCard.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(this));
        bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        bg.setCornerRadius(UiArchitect.dpToPx(this, 14));
        avCard.setBackground(bg);

        ivCurrentAvatar = new ImageView(this);
        int size = UiArchitect.dpToPx(this, 90);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(size, size);
        avLp.bottomMargin = UiArchitect.dpToPx(this, 12);
        AvatarLoader.loadAvatar(this, ivCurrentAvatar, equippedAvatar);
        avCard.addView(ivCurrentAvatar, avLp);

        Button btnChangeAvatar = UiArchitect.createSecondaryButton(this, "🎨 CHANGE AVATAR / EMOJI", v -> {
            startActivityForResult(new Intent(this, AvatarEditorActivity.class), REQ_AVATAR);
        });
        avCard.addView(btnChangeAvatar);

        content.addView(avCard);

        // Hardware Identity Snapshot Card
        LinearLayout hwCard = new LinearLayout(this);
        hwCard.setOrientation(LinearLayout.VERTICAL);
        hwCard.setPadding(pad, pad, pad, pad);
        hwCard.setBackground(bg);

        LinearLayout.LayoutParams hwLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hwLp.topMargin = UiArchitect.dpToPx(this, 16);
        hwCard.setLayoutParams(hwLp);

        TextView tvHwTitle = new TextView(this);
        tvHwTitle.setText("🔒 Hardware Root Anchor (Immutable SSOT)");
        tvHwTitle.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvHwTitle.setTextSize(UiArchitect.spToPx(this, 13f));
        tvHwTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        hwCard.addView(tvHwTitle);

        TextView tvHwDetails = new TextView(this);
        tvHwDetails.setText("• Hardware Agent ID: " + myAgentId + "\n• Silicon Key: " + myHwKey + "\n• Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        tvHwDetails.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvHwDetails.setTextSize(UiArchitect.spToPx(this, 11f));
        tvHwDetails.setPadding(0, UiArchitect.dpToPx(this, 8), 0, 0);
        hwCard.addView(tvHwDetails);

        content.addView(hwCard);

        // Profile Form Card
        LinearLayout formCard = new LinearLayout(this);
        formCard.setOrientation(LinearLayout.VERTICAL);
        formCard.setPadding(pad, pad, pad, pad);
        formCard.setBackground(bg);

        LinearLayout.LayoutParams formLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        formLp.topMargin = UiArchitect.dpToPx(this, 16);
        formCard.setLayoutParams(formLp);

        TextView tvNickLabel = new TextView(this);
        tvNickLabel.setText("Device Friendly Nickname:");
        tvNickLabel.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvNickLabel.setTextSize(UiArchitect.spToPx(this, 13f));
        tvNickLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        formCard.addView(tvNickLabel);

        etNickname = UiArchitect.createInputField(this, "Enter Nickname (e.g. My Phone)", InputType.TYPE_CLASS_TEXT);
        String currentNick = RegistryManager.getString(this, RegistryManager.KEY_USER_NICKNAME, "My Device (" + myAgentId + ")");
        etNickname.setText(currentNick);
        formCard.addView(etNickname);

        TextView tvRoleLabel = new TextView(this);
        tvRoleLabel.setText("Family Role:");
        tvRoleLabel.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvRoleLabel.setTextSize(UiArchitect.spToPx(this, 13f));
        tvRoleLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvRoleLabel.setPadding(0, UiArchitect.dpToPx(this, 12), 0, UiArchitect.dpToPx(this, 6));
        formCard.addView(tvRoleLabel);

        spRole = new Spinner(this);
        String[] roles = {"GUARDIAN", "CHILD", "FRIEND"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spRole.setAdapter(adapter);
        String savedRole = RegistryManager.getString(this, RegistryManager.KEY_USER_ROLE, "GUARDIAN");
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equalsIgnoreCase(savedRole)) spRole.setSelection(i);
        }
        formCard.addView(spRole);

        Button btnSave = UiArchitect.createPrimaryButton(this, "SAVE PROFILE CHANGES", v -> saveProfile());
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        saveLp.topMargin = UiArchitect.dpToPx(this, 18);
        formCard.addView(btnSave, saveLp);

        content.addView(formCard);

        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);
    }

    private void saveProfile() {
        String nickname = etNickname.getText().toString().trim();
        if (nickname.isEmpty()) nickname = "Device " + myAgentId;
        String role = spRole.getSelectedItem().toString();

        RegistryManager.putString(this, RegistryManager.KEY_USER_NICKNAME, nickname);
        RegistryManager.putString(this, RegistryManager.KEY_USER_ROLE, role);

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        LiveDeviceModel existing = db.getDeviceByAgentId(myAgentId);

        double lat = existing != null ? existing.latitude : 0.0;
        double lng = existing != null ? existing.longitude : 0.0;
        float speed = existing != null ? existing.speed : 0f;
        float heading = existing != null ? existing.heading : 0f;
        int bat = existing != null ? existing.batteryPct : 100;

        LiveDeviceModel profile = new LiveDeviceModel(
            myAgentId,
            "HW_" + myAgentId,
            nickname,
            role,
            "FAMILY",
            equippedAvatar,
            lat, lng, speed, heading, bat,
            System.currentTimeMillis(),
            false, false
        );

        // Update local SQLite SSOT immediately
        db.insertOrUpdateDevice(profile);

        // Sync to cloud Realtime Database
        FirebaseComm.publishProfile(profile);

        UiArchitect.showSlideNotificationBanner(this, "Profile Saved", "Profile updated and synchronized across circle.", false, 2500, "DONE", () -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_AVATAR && resultCode == RESULT_OK) {
            equippedAvatar = RegistryManager.getString(this, RegistryManager.KEY_EQUIPPED_AVATAR, "avatar_custom_1");
            AvatarLoader.loadAvatar(this, ivCurrentAvatar, equippedAvatar);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
