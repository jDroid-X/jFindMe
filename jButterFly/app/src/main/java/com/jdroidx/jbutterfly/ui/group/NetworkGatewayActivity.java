package com.jdroidx.jbutterfly.ui.group;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.jdroidx.jbutterfly.R;
import com.jdroidx.jbutterfly.data.local.DatabaseHelper;
import com.jdroidx.jbutterfly.data.model.LiveDeviceModel;
import com.jdroidx.jbutterfly.data.remote.FirebaseComm;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;
import com.jdroidx.jbutterfly.utils.RegistryManager;
import com.jdroidx.jbutterfly.utils.SystemLogger;

public class NetworkGatewayActivity extends Activity {

    private static final String TAG = "NetworkGatewayActivity";

    private String myAgentId = "";
    private String myHwKey = "";
    private String selectedCategory = "FAMILY"; // FAMILY or FRIENDS
    private String selectedRole = "GUARDIAN"; // GUARDIAN, CHILD, or FRIEND

    private EditText etPairCode;
    private EditText etDeviceName;
    private ImageView ivQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        myHwKey = DeviceIdentityHelper.getRawHardwareKey(this);

        buildLayout();
    }

    private void buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Header
        LinearLayout header = UiArchitect.createHeaderBar(this, "Device Pairing Matrix", v -> finish());
        root.addView(header);

        int pad = UiArchitect.dpToPx(this, 16);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, pad);

        // ─── SECTION 1: MY PAIRING QR CODE & HARDWARE IDENTITY ─────────
        LinearLayout myCard = createSectionCard("1. Share Your Device (Scan or Code)");
        
        TextView tvHw = new TextView(this);
        tvHw.setText("Hardware Node: " + myAgentId + " • " + myHwKey);
        tvHw.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvHw.setTextSize(UiArchitect.spToPx(this, 11f));
        tvHw.setGravity(Gravity.CENTER);
        myCard.addView(tvHw);

        ivQrCode = new ImageView(this);
        int qrSize = UiArchitect.dpToPx(this, 180);
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(qrSize, qrSize);
        qrLp.gravity = Gravity.CENTER_HORIZONTAL;
        qrLp.topMargin = UiArchitect.dpToPx(this, 12);
        qrLp.bottomMargin = UiArchitect.dpToPx(this, 12);
        myCard.addView(ivQrCode, qrLp);
        generateMyQrCode();

        content.addView(myCard);

        // ─── SECTION 2: PAIR WITH ANOTHER DEVICE ──────────────────────
        LinearLayout pairCard = createSectionCard("2. Pair a New Device");

        // Category Selection: Family vs Friends
        TextView tvCatLabel = new TextView(this);
        tvCatLabel.setText("Select Category:");
        tvCatLabel.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvCatLabel.setTextSize(UiArchitect.spToPx(this, 13f));
        tvCatLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        pairCard.addView(tvCatLabel);

        View toggleCategory = UiArchitect.createSegmentedToggle(
            this, "FAMILY (GUARDIAN)", "FRIENDS (PEER)", true,
            isLeft -> {
                selectedCategory = isLeft ? "FAMILY" : "FRIENDS";
                selectedRole = isLeft ? "GUARDIAN" : "FRIEND";
            }
        );
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        toggleLp.topMargin = UiArchitect.dpToPx(this, 6);
        toggleLp.bottomMargin = UiArchitect.dpToPx(this, 12);
        pairCard.addView(toggleCategory, toggleLp);

        // Target Device Nickname
        etDeviceName = UiArchitect.createInputField(this, "Friendly Name (e.g. Mom's Phone, John's Tablet)", InputType.TYPE_CLASS_TEXT);
        pairCard.addView(etDeviceName);

        // Target 8-Digit Code Field
        etPairCode = UiArchitect.createInputField(this, "Enter 8-Digit Agent ID (e.g. BE5A234A)", InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams codeLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        codeLp.topMargin = UiArchitect.dpToPx(this, 10);
        codeLp.bottomMargin = UiArchitect.dpToPx(this, 16);
        pairCard.addView(etPairCode, codeLp);

        // Action Buttons: Manual Link & Scan QR
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnManualLink = UiArchitect.createPrimaryButton(this, "LINK DEVICE", v -> performManualPairing());
        LinearLayout.LayoutParams manualLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        manualLp.rightMargin = UiArchitect.dpToPx(this, 8);
        btnRow.addView(btnManualLink, manualLp);

        Button btnScan = UiArchitect.createSecondaryButton(this, "📷 SCAN QR", v -> startQrScanner());
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnRow.addView(btnScan, scanLp);

        pairCard.addView(btnRow);
        content.addView(pairCard);

        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);
    }

    private LinearLayout createSectionCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = UiArchitect.dpToPx(this, 16);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurfaceColor(this));
        bg.setStroke(UiArchitect.dpToPx(this, 1), ThemeManager.getBorderColor(this));
        bg.setCornerRadius(UiArchitect.dpToPx(this, 12));
        card.setBackground(bg);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeManager.getPrimaryTextColor(this));
        tvTitle.setTextSize(UiArchitect.spToPx(this, 14f));
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, UiArchitect.dpToPx(this, 10));
        card.addView(tvTitle);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = UiArchitect.dpToPx(this, 16);
        card.setLayoutParams(lp);

        return card;
    }

    private void generateMyQrCode() {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            // Payload format: jbutterfly://pair?id=AGENT_ID&hw=HW_KEY
            String payload = "jbutterfly://pair?id=" + myAgentId + "&hw=" + myHwKey;
            Bitmap bitmap = encoder.encodeBitmap(payload, BarcodeFormat.QR_CODE, 400, 400);
            ivQrCode.setImageBitmap(bitmap);

            // Also publish pairing session ready to cloud
            String nickname = RegistryManager.getString(this, RegistryManager.KEY_USER_NICKNAME, "My Device");
            FirebaseComm.createPairingSession(
                myAgentId, myAgentId, myHwKey, nickname, selectedCategory,
                () -> SystemLogger.i(TAG, "Pairing handshake published for " + myAgentId),
                err -> SystemLogger.e(TAG, "Pairing handshake failed: " + err, null)
            );
        } catch (Exception e) {
            SystemLogger.e(TAG, "Error generating QR Code", e);
        }
    }

    @SuppressWarnings("deprecation")
    private void startQrScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a jButterFly Pairing QR Code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            String scanned = result.getContents();
            handleScannedPayload(scanned);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedPayload(String payload) {
        // e.g. jbutterfly://pair?id=BE5A234A&hw=...
        String targetAgentId = "";
        String targetHw = "";
        if (payload.contains("id=")) {
            String[] parts = payload.split("id=");
            if (parts.length > 1) {
                targetAgentId = parts[1].split("&")[0].trim().toUpperCase();
            }
        } else {
            targetAgentId = payload.trim().toUpperCase();
        }

        if (DeviceIdentityHelper.isValidAgentId(targetAgentId)) {
            etPairCode.setText(targetAgentId);
            performManualPairing();
        } else {
            Toast.makeText(this, "Unrecognized QR Code format: " + payload, Toast.LENGTH_SHORT).show();
        }
    }

    private void performManualPairing() {
        String targetAgentId = etPairCode.getText().toString().trim().toUpperCase();
        String devName = etDeviceName.getText().toString().trim();

        if (targetAgentId.isEmpty()) {
            Toast.makeText(this, "Please enter the target device 8-Digit Agent ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetAgentId.equals(myAgentId)) {
            Toast.makeText(this, "Cannot pair with your own device ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalDevName = devName.isEmpty() ? "Node " + targetAgentId : devName;

        // Register in local SQLite SSOT
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        LiveDeviceModel device = new LiveDeviceModel(
            targetAgentId,
            "HW_" + targetAgentId,
            finalDevName,
            selectedRole,
            selectedCategory,
            "avatar_custom_1",
            0.0, 0.0, 0f, 0f, 100,
            System.currentTimeMillis(),
            false, false
        );
        db.insertOrUpdateDevice(device);
        db.insertNotification("🔗 Paired New Device", "Linked with " + finalDevName + " (" + selectedCategory + ")", "PAIRING");

        // Synchronize 2-way pairing state with Firebase
        String myNickname = RegistryManager.getString(this, RegistryManager.KEY_USER_NICKNAME, "My Node (" + myAgentId + ")");
        String myAvatar = RegistryManager.getString(this, "user_avatar_name", "avatar_custom_1");
        FirebaseComm.syncPairingConnection(
            myAgentId,
            myHwKey,
            myNickname,
            myAvatar,
            targetAgentId,
            finalDevName,
            selectedCategory,
            selectedRole,
            () -> {
                runOnUiThread(() -> {
                    UiArchitect.showSlideNotificationBanner(
                        this,
                        "Device Linked 2-Way",
                        "Successfully linked " + finalDevName + " to " + selectedCategory,
                        false, 2500, "DONE", () -> finish()
                    );
                });
            },
            err -> {
                // Offline fallback - still saved locally in SQLite
                runOnUiThread(() -> {
                    UiArchitect.showSlideNotificationBanner(
                        this,
                        "Linked Locally",
                        "Saved in SQLite as " + selectedCategory + " (" + finalDevName + ")",
                        false, 2500, "DONE", () -> finish()
                    );
                });
            }
        );
    }
}
