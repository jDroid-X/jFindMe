package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.ActionButtonTable;
import com.jdroidx.jfindme.ui.base.DashboardTileTable;
import com.jdroidx.jfindme.ui.base.HeaderTable;
import com.jdroidx.jfindme.ui.base.IconTable;
import com.jdroidx.jfindme.ui.base.EnterpriseCardTable;
import com.jdroidx.jfindme.ui.base.ListItemTable;
import com.jdroidx.jfindme.ui.base.StatusPillTable;
import com.jdroidx.jfindme.ui.base.FormInputTable;
import com.jdroidx.jfindme.ui.base.BottomNavTable;
import com.jdroidx.jfindme.ui.base.MarqueeTable;
import com.jdroidx.jfindme.ui.base.SystemGeometryTable;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.model.SafeZoneModel;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> GalleryRegistry -> UiGalleryActivity
 * L7 Architectural Verification: Real-time visual audit of all UI assets.
 */
public class UiGalleryActivity extends BaseActivityJava {

    private final String TAG = "UiGalleryActivity";

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // Header matrix
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView title = new TextView(this);
        title.setText("LIVE COMPONENT GALLERY");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Architectural Audit: All L7 Assets at [Stage: " + UiArchitect.getStage(this) + "]");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.75f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        header.addView(subtitle);

        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false); // ≡ƒÜÇ QA Depth Fix: Prevent shadow clipping
        scroll.setClipToPadding(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        
        int cPad = getResponsivePaddingQuota();
        content.setPadding(cPad * 2, cPad, cPad * 2, cPad * 5);

        // --- LAYER 0: STAGE CONTROL CENTER (SYSTEM OVERRIDE) ---
        renderStageControlCenter(content);

        // --- SECTION 1: ACTION BUTTON MATRIX ---
        renderSectionHeader(content, "ACTION BUTTONS (ACTION_BUTTON_TABLE)");
        renderAuditMatrix(content, "BUTTONS");
        LinearLayout btnList = new LinearLayout(this);
        btnList.setOrientation(LinearLayout.VERTICAL);
        btnList.setClipChildren(false); // ≡ƒÜÇ Enable shadow bleed for individual items
        
        java.util.Map<String, ActionButtonTable.ActionRecord> buttons = ActionButtonTable.getRegistry();
        for (String key : buttons.keySet()) {
            View btn = createIconButton(key, android.R.drawable.ic_menu_compass, v -> {});
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 15, 0, 15);
            btnList.addView(btn, lp);
        }
        content.addView(btnList, getCardParams());

        renderSectionHeader(content, "STATUS PILLS (STATUS_PILL_TABLE)");
        renderAuditMatrix(content, "PILLS");
        
        android.widget.HorizontalScrollView pillScroll = new android.widget.HorizontalScrollView(this);
        pillScroll.setHorizontalScrollBarEnabled(false);
        
        LinearLayout pillPanel = new LinearLayout(this);
        pillPanel.setOrientation(LinearLayout.HORIZONTAL);
        pillPanel.setGravity(Gravity.CENTER_VERTICAL);
        pillPanel.setClipChildren(false); // ≡ƒÜÇ Allow pill shadows to bleed
        pillPanel.setPadding(35, 35, 35, 35);
        
        String[] pillTypes = {"STATUS_ACTIVE", "STATUS_ALERT", "STATUS_OFFLINE", "STATUS_SECURE", "STATUS_BETA"};
        for (String type : pillTypes) {
            View pill = StatusPillTable.createPill(this, type);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.setMargins(15, 0, 15, 0);
            pillPanel.addView(pill, plp);
        }
        pillScroll.addView(pillPanel);
        content.addView(pillScroll, getCardParams());
        
        // --- SECTION 2: DASHBOARD TILES (DASHBOARD_TILE_TABLE) ---
        renderSectionHeader(content, "DASHBOARD TILES (DASHBOARD_TILE_TABLE)");
        renderAuditMatrix(content, "TILES");
        android.widget.GridLayout tileGrid = new android.widget.GridLayout(this);
        tileGrid.setColumnCount(4); // ≡ƒÜÇ QA Density Boost: Switching from 2x2 to 4x4 matrix
        tileGrid.setClipChildren(false); // ≡ƒÜÇ Allow tile shadows to bleed
        
        java.util.Map<String, DashboardTileTable.TileRecord> tiles = DashboardTileTable.getRegistry();
        int tilePad = getResponsivePaddingQuota() / 2;
        for (String key : tiles.keySet()) {
            View tile = UiArchitect.createTile(this, key, v -> {});
            android.widget.GridLayout.LayoutParams tlp = new android.widget.GridLayout.LayoutParams();
            tlp.width = 0;
            tlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            tlp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
            tlp.setMargins(tilePad, tilePad, tilePad, tilePad);
            tileGrid.addView(tile, tlp);
        }
        content.addView(tileGrid, getCardParams());

        // --- SECTION 2.5: LIST ITEMS (LIST_ITEM_TABLE) ---
        renderSectionHeader(content, "LIST ITEMS (LIST_ITEM_TABLE)");
        renderAuditMatrix(content, "LISTS");
        LinearLayout listPanel = new LinearLayout(this);
        listPanel.setOrientation(LinearLayout.VERTICAL);
        listPanel.setClipChildren(false); // Enable nested depth!
        
        java.util.Map<String, ListItemTable.ListRecord> listItems = ListItemTable.getRegistry();
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        int n = 30; // COMPACT baseline
        switch (stage) {
            case LIBERAL:   n = 60; break;
            case STANDARD:  n = 45; break;
            case COMPACT:   n = 30; break;
            case SLIM:      n = 20; break;
            case ULTRA_SLIM:n = 15; break;
        }
        int divHeight = (2 * n) / 3;

        for (String key : listItems.keySet()) {
            View row = ListItemTable.createRow(this, key, key + " Label", "Forensic Subtext...", null);
            listPanel.addView(row);
            
            View div = new View(this);
            div.setBackgroundColor(Color.argb(20, 150, 150, 150));
            listPanel.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, divHeight));
        }
        content.addView(listPanel, getCardParams());

        // --- SECTION 3: HEADER GRADIENTS (HEADER_TABLE) ---
        renderSectionHeader(content, "HEADER ARCHITECTURE (HEADER_TABLE)");
        java.util.Map<String, HeaderTable.HeaderRecord> headers = HeaderTable.getRegistry();
        for (String key : headers.keySet()) {
            LinearLayout hCard = new LinearLayout(this);
            hCard.setOrientation(LinearLayout.VERTICAL);
            UiArchitect.applyHeader(this, hCard, key);
            hCard.setPadding(30, 40, 30, 40);
            
            TextView hLabel = new TextView(this);
            hLabel.setText(key);
            hLabel.setTextColor(Color.WHITE);
            hLabel.setTypeface(null, Typeface.BOLD);
            hLabel.setTextSize(10f);
            hCard.addView(hLabel);
            
            content.addView(hCard, getCardParams());
        }

        // --- SECTION 3.5: UNIFIED TYPOGRAPHY (TECH STACK) ---
        renderSectionHeader(content, "UNIFIED TYPOGRAPHY (UI_ARCHITECT TIERS)");
        LinearLayout typoPanel = new LinearLayout(this);
        typoPanel.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, typoPanel, "CARD_STANDARD");
        typoPanel.setPadding(30, 30, 30, 30);
        
        String[] textTiers = {"TEXT_HEADER", "TEXT_TITLE", "TEXT_BODY", "TEXT_SUB", "TEXT_MINI", "TEXT_LABEL"};
        for (String tier : textTiers) {
            TextView sample = new TextView(this);
            sample.setText(tier + ": Visual Parity Sample");
            sample.setTextColor(getTextColor());
            UiArchitect.applyText(this, sample, tier);
            sample.setPadding(0, 10, 0, 10);
            typoPanel.addView(sample);
        }
        content.addView(typoPanel, getCardParams());

        // --- SECTION 4: ENTERPRISE CARDS (ENTERPRISE_CARD_TABLE) ---
        renderSectionHeader(content, "ENTERPRISE CARDS (ENTERPRISE_CARD_TABLE)");
        java.util.Map<String, EnterpriseCardTable.CardRecord> cards = EnterpriseCardTable.getRegistry();
        for (String key : cards.keySet()) {
            LinearLayout cCard = new LinearLayout(this);
            cCard.setOrientation(LinearLayout.VERTICAL);
            UiArchitect.applyCard(this, cCard, key);
            cCard.setPadding(40, 40, 40, 40);
            
            TextView cLabel = new TextView(this);
            cLabel.setText(key + " (Forensic Geometry)");
            cLabel.setTextColor(getTextColor());
            cLabel.setTextSize(12f);
            cCard.addView(cLabel);
            
            content.addView(cCard, getCardParams());
        }

        // --- SECTION 5: SYSTEM VECTOR MATRIX (ICON_TABLE) ---
        renderSectionHeader(content, "VECTOR ICON MATRIX (ICON_TABLE)");
        android.widget.GridLayout iconGrid = new android.widget.GridLayout(this);
        iconGrid.setColumnCount(4);
        iconGrid.setClipChildren(false);
        iconGrid.setPadding(20, 20, 20, 20);
        
        java.util.Map<String, IconTable.IconRecord> icons = IconTable.getRegistry();
        int iconPad = 25;
        for (String key : icons.keySet()) {
            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setImageResource(icons.get(key).resourceId);
            iv.setColorFilter(getGradientMid()); // ≡ƒÜÇ QA Chrominance Sync (Vibrant Mid Color)
            iv.setAlpha(1.0f); // Ensure zero transparency leak
            int isz = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
            android.widget.GridLayout.LayoutParams ilp = new android.widget.GridLayout.LayoutParams();
            ilp.width = isz;
            ilp.height = isz;
            ilp.setMargins(iconPad, iconPad, iconPad, iconPad);
            iconGrid.addView(iv, ilp);
        }
        content.addView(iconGrid, getCardParams());

        // --- SECTION 6: FORM INPUT MATRIX (FORM_INPUT_TABLE) ---
        renderSectionHeader(content, "FORM INPUT MATRIX (FORM_INPUT_TABLE)");
        LinearLayout formPanel = new LinearLayout(this);
        formPanel.setOrientation(LinearLayout.VERTICAL);
        formPanel.setClipChildren(false);
        formPanel.setPadding(30, 30, 30, 30);
        
        java.util.Map<String, FormInputTable.InputRecord> inputs = FormInputTable.getRegistry();
        for (String key : inputs.keySet()) {
            formPanel.addView(FormInputTable.createField(this, key));
        }
        content.addView(formPanel, getCardParams());

        // --- SECTION 6.5: ENTERPRISE DROPDOWNS (SPINNERS) ---
        renderSectionHeader(content, "ENTERPRISE DROPDOWNS (SPINNERS)");
        LinearLayout spinnerPanel = new LinearLayout(this);
        spinnerPanel.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, spinnerPanel, "CARD_STANDARD");
        // ≡ƒÜÇ QA Fix: Migrating to 'AutoCompleteTextView' for real-time node filtering (Text Mode)
        android.widget.AutoCompleteTextView sampleSpinner = new android.widget.AutoCompleteTextView(this);
        sampleSpinner.setHint("Country Node Selector...");
        sampleSpinner.setHintTextColor(getGradientStart()); // ≡ƒº▒ Synchronized with Border Chrominance
        sampleSpinner.setTextColor(getTextColor());
        java.util.ArrayList<String> items = new java.util.ArrayList<>(java.util.Arrays.asList("Select Country", "United States", "India", "United Kingdom", "Canada", "Australia"));
        android.widget.ArrayAdapter<String> adapter = createEnterpriseDropdownAdapter(items);
        sampleSpinner.setAdapter(adapter);
        sampleSpinner.setThreshold(1); // ≡ƒÜÇ Immediate Filtering
        sampleSpinner.setDropDownBackgroundDrawable(createPopupBackground());
        
        LinearLayout spinRow = new LinearLayout(this);
        spinRow.setOrientation(LinearLayout.HORIZONTAL);
        spinRow.setGravity(Gravity.CENTER_VERTICAL);
        spinRow.setPadding(35, 20, 35, 20);
        android.graphics.drawable.GradientDrawable sbg = new android.graphics.drawable.GradientDrawable();
        sbg.setColor(Color.argb(13, 0, 0, 0)); // 5% Wash
        sbg.setCornerRadius(25f);
        sbg.setStroke(3, getGradientStart());
        spinRow.setBackground(sbg);
        
        android.widget.ImageView sIcon = new android.widget.ImageView(this);
        sIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
        sIcon.setColorFilter(getGradientStart());
        spinRow.addView(sIcon, new LinearLayout.LayoutParams(55, 55));
        spinRow.addView(sampleSpinner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        spinnerPanel.addView(spinRow);
        content.addView(spinnerPanel, getCardParams());

        // --- SECTION 6.6: FORENSIC DIALOG ARCHITECTURE --- (MOVED FOR LOGICAL FLOW)
        renderSectionHeader(content, "FORENSIC DIALOG ARCHITECTURE");
        View dialogBtn = createIconButton("TRIGGER FORENSIC DIALOG", android.R.drawable.ic_dialog_info, v -> {
            showForensicDialog("System Integrity Audit", "The L7 forensic engine has completed a 100% sweep of the management cluster. All nodes operating within standard parameters.");
        });
        content.addView(dialogBtn, getCardParams());

        // --- SECTION 7: GLOBAL NAVIGATION (BOTTOM_NAV_TABLE) ---
        renderSectionHeader(content, "GLOBAL NAVIGATION (BOTTOM_NAV_TABLE)");
        String[] navKeys = {"NAV_NOTICE", "NAV_DEVICE", "NAV_ME"};
        View navBar = BottomNavTable.createNav(this, navKeys, "NAV_DEVICE");
        content.addView(navBar, getCardParams());

        // --- SECTION 8: MARQUEE ALERT TICKERS (MARQUEE_TABLE) ---
        renderSectionHeader(content, "MARQUEE ALERT TICKERS (MARQUEE_TABLE)");
        java.util.Map<String, MarqueeTable.MarqueeRecord> marquees = MarqueeTable.getRegistry();
        for (String key : marquees.keySet()) {
            View bar = MarqueeTable.createBar(this, key, "Forensic data stream active - Analyzing node 0x7FF... No threats detected.");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 20);
            content.addView(bar, lp);
        }

        // --- SECTION 9: SYSTEM GEOMETRY AUDIT (RADIUS & ELEVATION) ---
        renderSectionHeader(content, "SYSTEM GEOMETRY AUDIT (RADIUS & ELEVATION)");
        LinearLayout geomGrid = new LinearLayout(this);
        geomGrid.setOrientation(LinearLayout.HORIZONTAL);
        geomGrid.setWeightSum(4f);
        geomGrid.setClipChildren(false);

        String[] rTiers = {"RADIUS_TIER_1", "RADIUS_TIER_2", "RADIUS_TIER_3", "RADIUS_TIER_ZERO"};
        for (String rt : rTiers) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            
            float rad = com.jdroidx.jfindme.ui.base.SystemGeometryTable.getRadius(this, rt);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(rad);
            bg.setColor(Color.argb(40, 255, 255, 255));
            bg.setStroke(2, getGradientStart());
            box.setBackground(bg);
            
            com.jdroidx.jfindme.ui.base.SystemGeometryTable.applyShadow(this, box, "SHADOW_MEDIUM");
            
            TextView label = new TextView(this);
            label.setText(rt + "\n" + (int)rad + "px");
            UiArchitect.applyText(this, label, "TEXT_LABEL");
            label.setTextSize(8f);
            label.setTextColor(getTextColor());
            label.setGravity(Gravity.CENTER);
            box.addView(label);
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, scale(180), 1f);
            lp.setMargins(scale(10), scale(10), scale(10), scale(10));
            geomGrid.addView(box, lp);
        }
        content.addView(geomGrid, getCardParams());

        // --- LAYER 8.5: ENTERPRISE RICH UI WIDGET SUITE (OOPS MVC/MVVM) ---
        renderEnterpriseWidgetSuite(content);

        // --- LAYER 9: GLOBAL CROSS-LOOKUP MATRIX ---
        renderGlobalCrossLookupMatrix(content);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return root;
    }

    private void renderEnterpriseWidgetSuite(LinearLayout container) {
        renderSectionHeader(container, "ENTERPRISE RICH UI WIDGET SUITE (OOPS MVC/MVVM)");

        LinearLayout suitePanel = new LinearLayout(this);
        suitePanel.setOrientation(LinearLayout.VERTICAL);

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        String dynamicSelfId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
        String hostDeviceName = db.getDeviceCentricName(dynamicSelfId);

        // 1. Tactile On/Off Switch
        View switchView = createInteractiveSwitch("Realtime GNSS Mesh", "Stream location to all paired nodes", true, (buttonView, isChecked) -> {
            showSlideNotification("Tracking Mode Changed", "Live GNSS streaming is now " + (isChecked ? "ENABLED" : "PAUSED"),
                android.R.drawable.ic_dialog_info, getGradientStart(), null, null);
        });
        LinearLayout.LayoutParams slp = getCardParams();
        slp.bottomMargin = scale(12);
        suitePanel.addView(switchView, slp);

        // 2. Segmented Multi-Toggle (Left / Right / Multi-Tab)
        String[] mapTiers = {"STANDARD", "SATELLITE", "TERRAIN"};
        View toggleView = createSegmentedToggle(mapTiers, 0, (idx, opt) -> {
            showSlideNotification("Map Canvas Switched", "Active base layer: " + opt,
                android.R.drawable.ic_menu_mapmode, getGradientStart(), null, null);
        });
        LinearLayout.LayoutParams tlp = getCardParams();
        tlp.bottomMargin = scale(12);
        suitePanel.addView(toggleView, tlp);

        // 3. Dropdown List Box with Dynamic Relational Nodes (Device Group-By)
        java.util.List<java.util.Map<String, Object>> offlineRoster = db.getOfflineRoster();
        java.util.List<String> nodeOptions = new java.util.ArrayList<>();
        nodeOptions.add(dynamicSelfId + " (" + hostDeviceName + " • Host Anchor)");
        for (java.util.Map<String, Object> node : offlineRoster) {
            String u1 = (String) node.get("uid_1");
            String name = (String) node.get("fullName");
            if (u1 != null && !u1.equals(dynamicSelfId) && !u1.isEmpty()) {
                nodeOptions.add(name + " (" + u1 + ")");
            }
        }
        String[] sampleNodes = nodeOptions.toArray(new String[0]);
        View dropdownView = createDropdownListBox("PRIMARY FLEET NODE", sampleNodes, 0, (idx, opt) -> {
            showSlideNotification("Node Focus Updated", "Active monitoring target: " + opt,
                android.R.drawable.ic_menu_myplaces, getGradientStart(), null, null);
        });
        LinearLayout.LayoutParams dlp = getCardParams();
        dlp.bottomMargin = scale(12);
        suitePanel.addView(dropdownView, dlp);

        // 4. Interactive Animated ViewFlipper (Smooth Switch between Dynamic Telemetry & Radar)
        LinearLayout flipperCard = new LinearLayout(this);
        flipperCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, flipperCard, "CARD_STANDARD");
        flipperCard.setPadding(scale(14), scale(14), scale(14), scale(14));

        TextView tvFlipperTitle = new TextView(this);
        tvFlipperTitle.setText("ANIMATED VIEW FLIPPER");
        tvFlipperTitle.setTextColor(getGradientStart());
        tvFlipperTitle.setTypeface(null, Typeface.BOLD);
        tvFlipperTitle.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_LABEL"));
        flipperCard.addView(tvFlipperTitle);

        // Fetch Dynamic Telemetry & Safe Zones from SQLite
        java.util.Map<String, Object> hostLoc = db.getLatestHistoricalLocation(dynamicSelfId);
        String hostSpeed = hostLoc != null && hostLoc.get("speed") != null ? hostLoc.get("speed") + " km/h" : "0 km/h";
        java.util.List<SafeZoneModel> activeZones = db.getActiveSafeZones();
        java.util.Map<String, Object> vitals = db.getDeviceVitals(dynamicSelfId);
        int batteryLevel = vitals != null && vitals.get("battery_level") != null ? ((Number) vitals.get("battery_level")).intValue() : 100;

        // View A: Dynamic Telemetry Snapshot
        LinearLayout viewA = new LinearLayout(this);
        viewA.setOrientation(LinearLayout.VERTICAL);
        viewA.setPadding(0, scale(8), 0, scale(8));
        TextView tvA1 = new TextView(this);
        tvA1.setText("VIEW 1: Host Telemetry Snapshot");
        tvA1.setTextColor(getTextColor());
        tvA1.setTypeface(null, Typeface.BOLD);
        tvA1.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_TITLE"));
        TextView tvA2 = new TextView(this);
        tvA2.setText("Node: " + dynamicSelfId + " (" + hostDeviceName + ") • Speed: " + hostSpeed + " • Battery: " + batteryLevel + "%");
        tvA2.setTextColor(getSecondaryTextColor());
        tvA2.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_SUB"));
        viewA.addView(tvA1);
        viewA.addView(tvA2);

        // View B: Dynamic Security Perimeter Radar
        LinearLayout viewB = new LinearLayout(this);
        viewB.setOrientation(LinearLayout.VERTICAL);
        viewB.setPadding(0, scale(8), 0, scale(8));
        TextView tvB1 = new TextView(this);
        tvB1.setText("VIEW 2: Security Perimeter Radar");
        tvB1.setTextColor(getTextColor());
        tvB1.setTypeface(null, Typeface.BOLD);
        tvB1.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_TITLE"));
        TextView tvB2 = new TextView(this);
        String primaryZoneName = !activeZones.isEmpty() ? activeZones.get(0).getName() : "None Assigned";
        tvB2.setText("Safe Zones: " + activeZones.size() + " Active • Primary: " + primaryZoneName + " • Status: SECURE 🛡️");
        tvB2.setTextColor(getSecondaryTextColor());
        tvB2.setTextSize(UiArchitect.getResponsiveTextSize(this, "TEXT_SUB"));
        viewB.addView(tvB1);
        viewB.addView(tvB2);

        android.widget.ViewFlipper flipper = createAnimatedFlipper(viewA, viewB);
        flipperCard.addView(flipper);

        View btnFlip = createIconButton("FLIP VIEW (SMOOTH TRANSITION)", android.R.drawable.ic_menu_rotate, v -> {
            flipper.showNext();
            int currentChild = flipper.getDisplayedChild();
            showSlideNotification("Flipper Transitioned", "Switched to " + (currentChild == 0 ? "Telemetry Snapshot" : "Security Perimeter"),
                android.R.drawable.ic_menu_slideshow, getGradientStart(), null, null);
        });
        LinearLayout.LayoutParams flipLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flipLp.topMargin = scale(10);
        flipperCard.addView(btnFlip, flipLp);

        LinearLayout.LayoutParams fclp = getCardParams();
        fclp.bottomMargin = scale(12);
        suitePanel.addView(flipperCard, fclp);

        // 5. Interactive Detail List Box with Dynamic Relational Data
        java.util.List<UiArchitect.DetailListItem> detailItems = new java.util.ArrayList<>();
        String hostAddr = hostLoc != null && hostLoc.get("address") != null ? (String) hostLoc.get("address") : "Anchor Fix Established";
        detailItems.add(new UiArchitect.DetailListItem(
            "Host Agent Node (" + dynamicSelfId + ")",
            hostDeviceName + " • Master Anchor",
            "Speed: " + hostSpeed + " • " + hostAddr,
            "LIVE 🟢",
            android.R.drawable.ic_menu_compass,
            () -> showSlideNotification("Node Selected", "Host Agent " + dynamicSelfId + " inspected.", android.R.drawable.ic_menu_info_details, getGradientStart(), null, null)
        ));

        if (!activeZones.isEmpty()) {
            SafeZoneModel firstZone = activeZones.get(0);
            detailItems.add(new UiArchitect.DetailListItem(
                "Safe Zone: " + firstZone.getName(),
                "Radius: " + (int) firstZone.getRadius() + "m • Lat: " + String.format(java.util.Locale.US, "%.4f", firstZone.getLatitude()),
                "Perimeter Sentinel: Armed (" + activeZones.size() + " total zones)",
                "SECURE 🛡️",
                android.R.drawable.ic_dialog_map,
                () -> showSlideNotification("Safe Zone Selected", firstZone.getName() + " perimeter active.", android.R.drawable.ic_dialog_map, Color.parseColor("#00E676"), null, null)
            ));
        } else {
            detailItems.add(new UiArchitect.DetailListItem(
                "Safe Zones Engine",
                "0 Zones configured in local SQLite",
                "Tap to configure perimeters in Safe Zone Manager",
                "STANDBY ⏸️",
                android.R.drawable.ic_dialog_map,
                () -> showSlideNotification("Safe Zones", "Configure perimeters in Safe Zone screen.", android.R.drawable.ic_dialog_map, getGradientStart(), null, null)
            ));
        }

        detailItems.add(new UiArchitect.DetailListItem(
            "Hardware Telemetry Vitals",
            android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " • Android " + android.os.Build.VERSION.RELEASE,
            "Battery: " + batteryLevel + "% • Storage: SQLite 14-Table Sync Active",
            "HEALTHY 📶",
            android.R.drawable.ic_menu_info_details,
            () -> showSlideNotification("Hardware Vitals", "Device hardware operating within nominal parameters.", android.R.drawable.ic_menu_info_details, getGradientStart(), null, null)
        ));

        View detailListBox = createInteractiveDetailListBox("INTERACTIVE DETAIL LIST BOX", detailItems);
        LinearLayout.LayoutParams dlbLp = getCardParams();
        dlbLp.bottomMargin = scale(12);
        suitePanel.addView(detailListBox, dlbLp);

        // 6. Validated Textbox with Inline Validation
        LinearLayout validatedBoxCard = new LinearLayout(this);
        validatedBoxCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, validatedBoxCard, "CARD_STANDARD");
        validatedBoxCard.setPadding(scale(14), scale(14), scale(14), scale(14));

        UiArchitect.ValidatedInputComponent validatedInput = createValidatedInputBox(
            "INLINE VALIDATED INPUT BOX",
            "Enter 8-character Agent ID (e.g. " + dynamicSelfId + ")",
            android.R.drawable.ic_menu_edit,
            new UiArchitect.TextValidator() {
                @Override
                public boolean validate(String text) {
                    return text != null && text.trim().matches("^[A-Z0-9]{8}$");
                }
                @Override
                public String getErrorMessage() {
                    return "Must be exactly 8 uppercase alphanumeric characters.";
                }
            }
        );
        validatedBoxCard.addView(validatedInput.container);

        View btnCheckInput = createIconButton("VALIDATE & SUBMIT INPUT", android.R.drawable.ic_input_add, v -> {
            String entered = validatedInput.editText.getText() != null ? validatedInput.editText.getText().toString().trim() : "";
            if (entered.matches("^[A-Z0-9]{8}$")) {
                showSlideNotification("Validation Success", "Agent ID '" + entered + "' verified and valid!",
                    android.R.drawable.ic_dialog_info, Color.parseColor("#00E676"), null, null);
            } else {
                showActionableValidationDialog("Validation Alert",
                    "Entered value '" + entered + "' does not match standard 8-character hardware format.",
                    "AUTO-CORRECT", "DISMISS",
                    () -> {
                        validatedInput.editText.setText(dynamicSelfId);
                        showSlideNotification("Auto-Corrected", "Reset to host ID: " + dynamicSelfId,
                            android.R.drawable.ic_menu_rotate, getGradientStart(), null, null);
                    }, null);
            }
        });
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbLp.topMargin = scale(8);
        validatedBoxCard.addView(btnCheckInput, cbLp);

        LinearLayout.LayoutParams vbLp = getCardParams();
        vbLp.bottomMargin = scale(12);
        suitePanel.addView(validatedBoxCard, vbLp);

        // 7. Action Row for Dialogs and Slide Notifications
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.VERTICAL);

        View btnSlide = createIconButton("TEST SLIDE NOTIFICATION (BOTTOM RIGHT)", android.R.drawable.ic_dialog_email, v -> {
            showSlideNotification("Safe Zone Alert", "Node " + dynamicSelfId + " entered School Safe Zone",
                android.R.drawable.ic_dialog_alert, Color.parseColor("#00E676"), "VIEW ON MAP", () -> {
                    showValidationDialog("Perimeter Event", "Navigation anchor established for " + dynamicSelfId + ".");
                });
        });
        LinearLayout.LayoutParams blp1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp1.bottomMargin = scale(8);
        actionRow.addView(btnSlide, blp1);

        View btnMultiChoice = createIconButton("TEST MULTI-CHOICE CONDITION DIALOG", android.R.drawable.ic_menu_agenda, v -> {
            String[] conditionOptions = {
                "Realtime GNSS Telemetry Stream",
                "Geofence Boundary Transition Sentinel",
                "Acoustic Siren & Emergency Audio Bridge",
                "Encrypted SQLite Vault Offline Buffer"
            };
            boolean[] initialStates = {true, true, false, true};
            showMultiChoiceOptionsDialog("Select Active Sentinels", conditionOptions, initialStates, (selectedStates, selectedItems) -> {
                int activeCount = 0;
                for (boolean b : selectedStates) if (b) activeCount++;
                showSlideNotification("Conditions Updated", activeCount + " of " + selectedStates.length + " sentinels active.",
                    android.R.drawable.ic_dialog_info, Color.parseColor("#00E676"), null, null);
            });
        });
        LinearLayout.LayoutParams blpMulti = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blpMulti.bottomMargin = scale(8);
        actionRow.addView(btnMultiChoice, blpMulti);

        View btnMultiOption = createIconButton("TEST COMMAND DISPATCH OPTIONS", android.R.drawable.ic_menu_manage, v -> {
            String[] options = {"Send Immediate Acoustic Siren", "Request Live GNSS Burst", "Audit Device Vitals", "Export SQLite Trace"};
            showNotificationOptionsDialog("Forensic Node Actions", "Select command to dispatch to node " + dynamicSelfId + ":", options, (idx, opt) -> {
                showSlideNotification("Command Dispatched", opt + " initiated successfully.",
                    android.R.drawable.ic_media_play, getGradientStart(), null, null);
            });
        });
        LinearLayout.LayoutParams blp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp2.bottomMargin = scale(8);
        actionRow.addView(btnMultiOption, blp2);

        View btnValidation = createIconButton("TEST ACTIONABLE VALIDATION DIALOG", android.R.drawable.ic_menu_edit, v -> {
            showActionableValidationDialog(
                "Cache Synchronization Notice",
                "Local SQLite database contains 14 tables synchronized with cloud telemetry. Would you like to optimize indexes?",
                "OPTIMIZE NOW", "DISMISS",
                () -> {
                    showSlideNotification("Optimization Complete", "Database indexes and SQLite vacuum verified.",
                        android.R.drawable.ic_dialog_info, Color.parseColor("#00E676"), null, null);
                }, null
            );
        });
        actionRow.addView(btnValidation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        suitePanel.addView(actionRow, getCardParams());
        container.addView(suitePanel);
    }

    private void renderSectionHeader(LinearLayout container, String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextColor(getGradientStart());
        UiArchitect.applyText(this, header, "TEXT_LABEL");
        header.setPadding(10, 60, 0, 20);
        container.addView(header);
    }

    private LinearLayout.LayoutParams getCardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 35);
        return lp;
    }

    @Override
    protected void setupLogic() {
        SystemLogger.i(TAG, "L7 Component Gallery Engaged.");
    }

    // ============================================================================================
    // ≡ƒÜÇ FORENSIC ARCHITECT TOOLS: DYNAMIC OVERRIDE & AUDIT
    // ============================================================================================

    private void renderStageControlCenter(LinearLayout container) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, row, "CARD_STANDARD");
        row.setPadding(35, 35, 35, 35);

        TextView label = new TextView(this);
        label.setText("ARCHITECTURAL STAGE SELECTOR");
        label.setTextColor(getGradientStart());
        UiArchitect.applyText(this, label, "TEXT_LABEL");
        row.addView(label);

        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this);
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 20, 0, 20);

        UiArchitect.DesignStage current = UiArchitect.getStage(this);
        for (UiArchitect.DesignStage stage : UiArchitect.STAGES) {
            boolean isActive = (stage == current);
            
            LinearLayout stageBtn = new LinearLayout(this);
            stageBtn.setOrientation(LinearLayout.VERTICAL);
            stageBtn.setGravity(Gravity.CENTER);
            stageBtn.setPadding(30, 20, 30, 20);
            stageBtn.setClickable(true);
            stageBtn.setFocusable(true);
            
            // Apply Selection Logic: Border for active stage
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(isActive ? getGradientStart() : Color.argb(20, 100, 100, 100));
            gd.setCornerRadius(15f);
            if (!isActive) gd.setStroke(2, getGradientStart());
            stageBtn.setBackground(gd);

            TextView btnText = new TextView(this);
            btnText.setText(stage.name());
            btnText.setTextColor(isActive ? Color.WHITE : getTextColor());
            btnText.setTextSize(10f);
            btnText.setTypeface(null, Typeface.BOLD);
            stageBtn.addView(btnText);

            stageBtn.setOnClickListener(v -> {
                prefs.edit().putInt("stage_override", stage.ordinal()).apply();
                safeRecreate();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 15, 0);
            btnRow.addView(stageBtn, lp);
        }
        scroll.addView(btnRow);
        row.addView(scroll);

        View updateBtn = createIconButton("FORCE SYSTEM RE-LAYOUT", android.R.drawable.ic_menu_rotate, true, v -> safeRecreate());
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ulp.setMargins(0, 20, 0, 0);
        row.addView(updateBtn, ulp);

        container.addView(row, getCardParams());
    }

    private void renderAuditMatrix(LinearLayout container, String type) {
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.ScalingRecord record = com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.get(stage);

        LinearLayout matrix = new LinearLayout(this);
        matrix.setOrientation(LinearLayout.VERTICAL);
        matrix.setBackgroundColor(Color.argb(10, 128, 128, 128));
        matrix.setPadding(25, 25, 25, 25);
        
        // Waterfall grouping of properties
        String auditText = "AUDIT: [" + stage.name() + "]\n" +
                  "ΓÇó Text Scale: " + record.bodyTextScale + "x / Header: " + record.headerTextScale + "x\n" +
                  "ΓÇó Icon Scale: " + record.iconScale + "x\n" +
                  "ΓÇó Padding Scale: " + record.paddingScale + "x\n" +
                  "ΓÇó Elevation Scale: " + record.elevationScale + "x";

        TextView tv = new TextView(this);
        tv.setText(auditText);
        tv.setTextColor(getSecondaryTextColor());
        tv.setTextSize(9f);
        tv.setTypeface(Typeface.MONOSPACE);
        matrix.addView(tv);

        container.addView(matrix);
    }

    private void renderGlobalCrossLookupMatrix(LinearLayout container) {
        renderSectionHeader(container, "GLOBAL CROSS-LOOKUP MATRIX (SSOT)");
        
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, table, "CARD_STANDARD");
        table.setPadding(20, 20, 20, 20);

        // Header Row
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.argb(30, 0, 0, 0));
        header.setPadding(10, 10, 10, 10);
        
        String[] cols = {"STAGE", "TXT_H", "TXT_B", "ICON", "PAD", "ELEV"};
        for (String col : cols) {
            TextView tv = new TextView(this);
            tv.setText(col);
            tv.setTextColor(getGradientStart());
            tv.setTextSize(8f);
            tv.setTypeface(null, Typeface.BOLD);
            header.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        table.addView(header);

        // Data Rows - The Waterfall Grouping
        for (UiArchitect.DesignStage stage : UiArchitect.STAGES) {
            com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.ScalingRecord r = com.jdroidx.jfindme.ui.base.UiScalingMatrixTable.get(stage);
            
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(10, 15, 10, 15);
            
            String[] vals = {
                stage.name().substring(0, Math.min(stage.name().length(), 4)),
                String.valueOf(r.headerTextScale),
                String.valueOf(r.bodyTextScale),
                String.valueOf(r.iconScale),
                String.valueOf(r.paddingScale),
                String.valueOf(r.elevationScale)
            };

            for (String val : vals) {
                TextView tv = new TextView(this);
                tv.setText(val);
                tv.setTextColor(getTextColor());
                tv.setTextSize(8f);
                row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
            table.addView(row);
            
            View div = new View(this);
            div.setBackgroundColor(Color.argb(15, 150, 150, 150));
            table.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }

        container.addView(table, getCardParams());
        
        TextView footer = new TextView(this);
        footer.setText("Forensic Integrity: Any change to 'UiScalingMatrixTable.java' will physically propagate through this matrix waterfall to all enterprise nodes.");
        footer.setTextColor(getSecondaryTextColor());
        footer.setTextSize(8f);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(40, 20, 40, 100);
        container.addView(footer);
    }
}
