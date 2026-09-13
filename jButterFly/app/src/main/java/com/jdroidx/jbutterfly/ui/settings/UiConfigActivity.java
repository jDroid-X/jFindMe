package com.jdroidx.jbutterfly.ui.settings;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.jdroidx.jbutterfly.R;
import com.jdroidx.jbutterfly.service.TrackingService;
import com.jdroidx.jbutterfly.ui.base.ThemeManager;
import com.jdroidx.jbutterfly.ui.base.UiArchitect;
import com.jdroidx.jbutterfly.utils.DeviceIdentityHelper;
import com.jdroidx.jbutterfly.utils.RegistryManager;
import java.util.ArrayList;
import java.util.List;

public class UiConfigActivity extends Activity {

    private String dynamicAgentId = "";
    private String dynamicHwKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        dynamicAgentId = DeviceIdentityHelper.getOrGenerateAgentId(this);
        dynamicHwKey = DeviceIdentityHelper.getRawHardwareKey(this);

        buildLayout();
    }

    private void buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Header
        root.addView(UiArchitect.createHeaderBar(this, "Master UI/UX Controls Studio", v -> finish()));

        int pad = UiArchitect.dpToPx(this, 16);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, UiArchitect.dpToPx(this, 30));

        // Subtitle
        TextView tvSub = new TextView(this);
        tvSub.setText("Interactive testbed for buttons, switches, toggles, flippers, validated textboxes, accordions, and slide notifications.");
        tvSub.setTextColor(ThemeManager.getSecondaryTextColor(this));
        tvSub.setTextSize(UiArchitect.spToPx(this, 12f));
        tvSub.setPadding(0, 0, 0, UiArchitect.dpToPx(this, 14));
        content.addView(tvSub);

        // ─── 1. BUTTONS SUITE ─────────────────────────────────────────
        content.addView(createSectionHeader("1. Standardized Buttons"));
        LinearLayout btnRow1 = new LinearLayout(this);
        btnRow1.setOrientation(LinearLayout.HORIZONTAL);

        Button btnPrimary = UiArchitect.createPrimaryButton(this, "PRIMARY", v -> {
            UiArchitect.showSlideNotificationBanner(this, "Primary Action", "Primary button action dispatched.", false, 3000, "OK", null);
        });
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        pLp.rightMargin = UiArchitect.dpToPx(this, 6);
        btnRow1.addView(btnPrimary, pLp);

        Button btnSec = UiArchitect.createSecondaryButton(this, "SECONDARY", v -> {
            UiArchitect.showSlideNotificationBanner(this, "Secondary Action", "Secondary button triggered.", false, 3000, null, null);
        });
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        sLp.rightMargin = UiArchitect.dpToPx(this, 6);
        btnRow1.addView(btnSec, sLp);

        Button btnAccent = UiArchitect.createAccentButton(this, "ACCENT", v -> {
            UiArchitect.showSlideNotificationBanner(this, "Accent Action", "Accent button selected.", false, 3000, null, null);
        });
        btnRow1.addView(btnAccent, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        content.addView(btnRow1);

        LinearLayout btnRow2 = new LinearLayout(this);
        btnRow2.setOrientation(LinearLayout.HORIZONTAL);
        btnRow2.setPadding(0, UiArchitect.dpToPx(this, 8), 0, UiArchitect.dpToPx(this, 14));

        Button btnDestruct = UiArchitect.createDestructiveButton(this, "DESTRUCTIVE", v -> {
            UiArchitect.showSlideNotificationBanner(this, "Critical Alert", "Destructive command executed!", true, 3500, "UNDO", () -> {
                UiArchitect.showSlideNotificationBanner(this, "Action Reverted", "Destructive command was cancelled.", false, 2500, null, null);
            });
        });
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        dLp.rightMargin = UiArchitect.dpToPx(this, 8);
        btnRow2.addView(btnDestruct, dLp);

        Button btnOutlined = UiArchitect.createOutlinedButton(this, "OUTLINED", v -> {
            UiArchitect.showSlideNotificationBanner(this, "Outlined Action", "Outlined button clicked.", false, 2500, null, null);
        });
        btnRow2.addView(btnOutlined, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        content.addView(btnRow2);

        // ─── 2. VALIDATED TEXTBOXES ───────────────────────────────────
        content.addView(createSectionHeader("2. Validated Textboxes (Live Condition Rules)"));

        // Rule A: Exactly 8-character alphanumeric Agent ID
        View valAgentId = UiArchitect.createValidatedInputField(
            this,
            "Hardware Agent ID Validator",
            "Enter 8-digit hardware ID (e.g. " + dynamicAgentId + ")",
            true,
            new UiArchitect.InputValidator() {
                @Override
                public boolean validate(String input) {
                    return input.length() == 8 && input.matches("^[a-zA-Z0-9]+$");
                }
                @Override
                public String getErrorMessage() {
                    return "Agent ID must be exactly 8 alphanumeric characters.";
                }
            }
        );
        content.addView(valAgentId);

        // Rule B: SafeZone Radius Range (50m - 5000m)
        View valRadius = UiArchitect.createValidatedInputField(
            this,
            "SafeZone Radius Limit (50m - 5000m)",
            "Enter meters (e.g. 250)",
            false,
            new UiArchitect.InputValidator() {
                @Override
                public boolean validate(String input) {
                    try {
                        double r = Double.parseDouble(input.trim());
                        return r >= 50.0 && r <= 5000.0;
                    } catch (Exception e) {
                        return false;
                    }
                }
                @Override
                public String getErrorMessage() {
                    return "Radius must be between 50 and 5000 meters.";
                }
            }
        );
        LinearLayout.LayoutParams radLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        radLp.topMargin = UiArchitect.dpToPx(this, 10);
        radLp.bottomMargin = UiArchitect.dpToPx(this, 14);
        content.addView(valRadius, radLp);

        // ─── 3. DYNAMIC ON/OFF SWITCHES ───────────────────────────────
        content.addView(createSectionHeader("3. Dynamic On/Off Switches"));

        View swTracking = UiArchitect.createSwitch(
            this,
            "Live Background Sentinel",
            "Continuous GPS polling and WhatsApp-style watchdog",
            true,
            (buttonView, isChecked) -> {
                if (isChecked) {
                    TrackingService.startTracking(this);
                } else {
                    TrackingService.stopTracking(this);
                }
                UiArchitect.showSlideNotificationBanner(this,
                    "Sentinel Mode",
                    isChecked ? "Background tracking is now ACTIVE." : "Background tracking SUSPENDED.",
                    !isChecked, 3000, null, null
                );
            }
        );
        content.addView(swTracking);

        View swGhost = UiArchitect.createSwitch(
            this,
            "Ghost Cloaking Mode",
            "Hides current device from remote map radar views",
            false,
            (buttonView, isChecked) -> {
                RegistryManager.putBoolean(this, "ghost_mode_active", isChecked);
                UiArchitect.showSlideNotificationBanner(this,
                    "Ghost Cloaking",
                    isChecked ? "Device cloaked. Radar broadcasting paused." : "Device visible to trusted circle.",
                    isChecked, 3000, null, null
                );
            }
        );
        content.addView(swGhost);

        // ─── 4. SEGMENTED LEFT/RIGHT TOGGLES ──────────────────────────
        content.addView(createSectionHeader("4. Segmented Left/Right Toggles"));

        View toggleUnits = UiArchitect.createSegmentedToggle(
            this, "METRIC (KM/H)", "IMPERIAL (MPH)", true,
            isLeft -> {
                RegistryManager.putString(this, "pref_units", isLeft ? "METRIC" : "IMPERIAL");
                UiArchitect.showSlideNotificationBanner(this,
                    "Unit Selection",
                    isLeft ? "Velocity units: Metric (km/h)" : "Velocity units: Imperial (mph)",
                    false, 2500, null, null
                );
            }
        );
        content.addView(toggleUnits);

        View toggleCircle = UiArchitect.createSegmentedToggle(
            this, "FAMILY CIRCLE", "FRIENDS NETWORK", true,
            isLeft -> {
                UiArchitect.showSlideNotificationBanner(this,
                    "Circle Focus",
                    isLeft ? "Viewing Primary Family Circle" : "Viewing Friends Peer Network",
                    false, 2500, null, null
                );
            }
        );
        LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tcLp.topMargin = UiArchitect.dpToPx(this, 10);
        tcLp.bottomMargin = UiArchitect.dpToPx(this, 14);
        content.addView(toggleCircle, tcLp);

        // ─── 5. ANIMATED VIEW FLIPPER ─────────────────────────────────
        content.addView(createSectionHeader("5. Animated View Flipper"));

        TextView fView1 = new TextView(this);
        fView1.setText("🛰️ FLIPPER VIEW A: Live Spatial Telemetry\n• GPS: High Accuracy Fix\n• Battery: Nominal (100%)\n• Active Circle: Family Guardians");
        fView1.setTextColor(ThemeManager.getPrimaryTextColor(this));
        fView1.setTextSize(UiArchitect.spToPx(this, 12f));
        fView1.setPadding(pad, pad, pad, pad);

        TextView fView2 = new TextView(this);
        fView2.setText("🛡️ FLIPPER VIEW B: SafeZone Defense Matrix\n• Home Zone: 200m Perimeter\n• School Zone: 350m Perimeter\n• Acoustic Siren: Armed");
        fView2.setTextColor(ThemeManager.getPrimaryTextColor(this));
        fView2.setTextSize(UiArchitect.spToPx(this, 12f));
        fView2.setPadding(pad, pad, pad, pad);

        ViewFlipper flipper = UiArchitect.createAnimatedFlipper(this, fView1, fView2);
        content.addView(flipper);

        Button btnFlip = UiArchitect.createSecondaryButton(this, "FLIP TO NEXT DECK ➔", v -> {
            flipper.showNext();
            int currentChild = flipper.getDisplayedChild();
            UiArchitect.showSlideNotificationBanner(this,
                "Flipper Switched",
                currentChild == 0 ? "Switched to Telemetry Deck" : "Switched to SafeZone Defense Deck",
                false, 2500, null, null
            );
        });
        LinearLayout.LayoutParams flipLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        flipLp.topMargin = UiArchitect.dpToPx(this, 8);
        flipLp.bottomMargin = UiArchitect.dpToPx(this, 14);
        content.addView(btnFlip, flipLp);

        // ─── 6. EXPANDABLE LIST BOX (ACCORDION & DROPDOWN) ────────────
        content.addView(createSectionHeader("6. Accordion List Box & Dropdown"));

        TextView detailsBody = new TextView(this);
        detailsBody.setText("• Hardware Agent ID: " + dynamicAgentId +
                            "\n• Silicon Key: " + dynamicHwKey +
                            "\n• Architecture: ARM64 / Java 17" +
                            "\n• Encryption: AES-256 E2EE Mesh" +
                            "\n• Storage: Normalized SQLite SSOT");
        detailsBody.setTextColor(ThemeManager.getSecondaryTextColor(this));
        detailsBody.setTextSize(UiArchitect.spToPx(this, 11f));
        detailsBody.setLineSpacing(0f, 1.25f);

        View accordion = UiArchitect.createExpandableListBox(
            this,
            "Hardware Diagnostics Dossier",
            "Tap to expand silicon specifications",
            android.R.drawable.ic_menu_info_details,
            detailsBody
        );
        content.addView(accordion);

        List<String> mapModes = new ArrayList<>();
        mapModes.add("Vector Road Map (Normal)");
        mapModes.add("Satellite Photorealistic");
        mapModes.add("Night Tactical Radar");
        mapModes.add("Topographic Contours");

        View dropdown = UiArchitect.createDropdownListBox(
            this, "Select Base Map Style:", mapModes, 0,
            (index, item) -> {
                UiArchitect.showSlideNotificationBanner(this,
                    "Map Layer Updated",
                    "Switched to " + item,
                    false, 2500, null, null
                );
            }
        );
        LinearLayout.LayoutParams ddLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ddLp.bottomMargin = UiArchitect.dpToPx(this, 14);
        content.addView(dropdown, ddLp);

        // ─── 7. MULTI-CHOICE & SINGLE-CHOICE DIALOGS ──────────────────
        content.addView(createSectionHeader("7. Multi-Choice & Single-Choice Dialogs"));

        Button btnMulti = UiArchitect.createPrimaryButton(this, "OPEN MULTI-CHOICE CONDITIONS DIALOG", v -> {
            String[] alertTriggers = new String[]{
                "Geofence Entry Alert",
                "Geofence Exit Alert",
                "Acoustic Siren SOS Trigger",
                "Low Battery Alert (< 20%)",
                "Watchdog Doze Wakeup Alert"
            };
            boolean[] states = new boolean[]{true, true, true, false, true};

            UiArchitect.showMultiChoiceDialog(this, "Configure Active Sentinel Triggers", alertTriggers, states,
                (checkedStates, selectedItems) -> {
                    UiArchitect.showSlideNotificationBanner(this,
                        "Triggers Updated",
                        selectedItems.length + " of " + alertTriggers.length + " sentinel rules active.",
                        false, 3000, null, null
                    );
                });
        });
        content.addView(btnMulti);

        Button btnSingle = UiArchitect.createSecondaryButton(this, "OPEN SINGLE-CHOICE DIALOG", v -> {
            String[] speeds = new String[]{"Strict Mode (Highest Security)", "Balanced Mode (Standard)", "Eco Mode (Battery Saver)"};
            UiArchitect.showSingleChoiceDialog(this, "Select Tracking Profile", speeds, 1,
                (index, item) -> {
                    UiArchitect.showSlideNotificationBanner(this, "Profile Saved", item, false, 2500, null, null);
                });
        });
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        scLp.topMargin = UiArchitect.dpToPx(this, 8);
        scLp.bottomMargin = UiArchitect.dpToPx(this, 20);
        content.addView(btnSingle, scLp);

        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);
    }

    private TextView createSectionHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(ThemeManager.getBrandColor(this));
        tv.setTextSize(UiArchitect.spToPx(this, 14f));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, UiArchitect.dpToPx(this, 12), 0, UiArchitect.dpToPx(this, 8));
        return tv;
    }
}
