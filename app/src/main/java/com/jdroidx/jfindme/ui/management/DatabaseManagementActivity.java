package com.jdroidx.jfindme.ui.management;

import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.graphics.drawable.GradientDrawable;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.utils.SystemLogger;
import com.jdroidx.jfindme.utils.AngelLoader;
import java.util.*;

/**
 * Universal Analytical Hub (V37 FINAL RECONSTRUCTION)
 * Objective: High-impact Gradient Menu transitioning to Dynamic Forensic Explorer.
 * Hardening: L7 Architectural Registry Parity & V36 Database Migration.
 */
public class DatabaseManagementActivity extends BaseActivityJava {

    private DatabaseHelper db;
    private FrameLayout rootLayer;
    private LinearLayout menuState, explorerState;
    private TableLayout tableHeader, tableData;
    private HorizontalScrollView headerScroll, dataScroll;
    private TextView tvStats;
    
    private String activeTable = "";
    private int pSize = 15, pOffset = 0;
    private String currentSearch = null;

    @Override
    protected View createLayout() {
        db = new DatabaseHelper(this);
        rootLayer = new FrameLayout(this);
        rootLayer.setBackgroundColor(getPrimaryColor());

        // 🧱 STATE 1: HIGH-IMPACT GRADIENT MENU
        menuState = new LinearLayout(this);
        menuState.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout menuHeader = new LinearLayout(this);
        menuHeader.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(menuHeader);
        
        TextView menuTitle = new TextView(this);
        menuTitle.setText("FORENSIC VAULT");
        menuTitle.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, menuTitle, "TEXT_HEADER");
        menuHeader.addView(menuTitle);
        
        TextView menuSub = new TextView(this);
        menuSub.setText("L7 Universal Analytical Registry");
        menuSub.setTextColor(getGradientOverlayText());
        menuSub.setAlpha(0.8f);
        UiArchitect.applyText(this, menuSub, "TEXT_SUB");
        menuHeader.addView(menuSub);
        menuState.addView(menuHeader);

        ScrollView menuScroll = new ScrollView(this);
        LinearLayout menuContent = new LinearLayout(this);
        menuContent.setOrientation(LinearLayout.VERTICAL);
        int q = getResponsivePaddingQuota();
        menuContent.setPadding(q, q, q, q * 2);

        String[] tables = {
            "personal_table|Identity Dossier", 
            "roster_table|P2P Network", 
            "history_table|Tracking Archive", 
            "alerts_table|Siren Logs", 
            "SafeZone_Table|Boundary Matrix",
            "pairing_log|Device Pairing Ledger", // 🚀 V37 Hub
            "sim_inventory|SIM Inventory Archive" // 🚀 V38 Early-Access
        };
        for (String tEntry : tables) {
            String[] parts = tEntry.split("\\|");
            String tName = parts[0];
            String tLabel = parts[1];

            View btn = createTableMenuButton(tName, tLabel);
            int btnHeight = scale(180); // 🚀 Purged hardcoded 220
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, btnHeight);
            lp.setMargins(0, 0, 0, scale(30));
            menuContent.addView(btn, lp);
        }
        
        // 🚀 QA Fix: Master Factory Reset Hub (With Butterfly Transition)
        View btnReset = UiArchitect.createButton(this, "FACTORY_RESET", v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Hard Reset Matrix?")
                .setMessage("This will violently wipe the local SQLite Single Source of Truth and delete all forensic assets. Proceed with absolute caution.")
                .setPositiveButton("WIPE ALL", (d, w) -> {
                    AngelLoader.show(this);
                    AngelLoader.setStatus(this, "Violently Purging Database...");
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        db.factoryReset();
                        AngelLoader.setStatus(this, "Reconstructing Forensic SSOT (V37)...");
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            AngelLoader.hide(this);
                            Toast.makeText(this, "Global Wipe Complete. Matrix Restored.", Toast.LENGTH_LONG).show();
                            safeRecreate();
                        }, 2500);
                    }, 2500);
                })
                .setNegativeButton("Abort", null).show();
        });
        menuContent.addView(btnReset);
        
        menuScroll.addView(menuContent);
        menuState.addView(menuScroll);
        rootLayer.addView(menuState);

        // 🧱 STATE 2: DYNAMIC FORENSIC EXPLORER
        explorerState = new LinearLayout(this);
        explorerState.setOrientation(LinearLayout.VERTICAL);
        explorerState.setVisibility(View.GONE);
        explorerState.setPadding(q, q, q, q);

        tvStats = new TextView(this);
        tvStats.setTextColor(getGradientStart());
        tvStats.setPadding(0, scale(15), 0, scale(15));
        UiArchitect.applyText(this, tvStats, "TEXT_LABEL");
        explorerState.addView(tvStats);

        EditText etSearch = UiArchitect.createInputField(this, "FIELD_SEARCH_RECORDS");
        etSearch.setHint("Search Dynamic Records...");
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void afterTextChanged(android.text.Editable s) { currentSearch = s.toString(); pOffset = 0; renderExplorerUI(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        explorerState.addView(etSearch);

        headerScroll = new HorizontalScrollView(this);
        tableHeader = new TableLayout(this);
        headerScroll.addView(tableHeader);
        explorerState.addView(headerScroll);

        dataScroll = new HorizontalScrollView(this);
        ScrollView verticalScroll = new ScrollView(this);
        tableData = new TableLayout(this);
        verticalScroll.addView(tableData);
        dataScroll.addView(verticalScroll);
        dataScroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> headerScroll.scrollTo(x, 0));
        explorerState.addView(dataScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        
        View btnPrev = UiArchitect.createButton(this, "PREV", v -> { if(pOffset >= pSize) { pOffset -= pSize; renderExplorerUI(); } });
        View btnNext = UiArchitect.createButton(this, "NEXT", v -> { pOffset += pSize; renderExplorerUI(); });

        View btnWipe = UiArchitect.createButton(this, "WIPE_TABLE", v -> {
            new android.app.AlertDialog.Builder(this).setTitle("Purge Table?")
                .setMessage("Delete all records in " + activeTable + "?")
                .setPositiveButton("DELETE", (d, w) -> { db.truncateTable(activeTable); renderExplorerUI(); })
                .setNegativeButton("Abort", null).show();
        });

        View btnBack = UiArchitect.createButton(this, "BACK", v -> { explorerState.setVisibility(View.GONE); menuState.setVisibility(View.VISIBLE); });

        int navMargin = scale(10);
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        navLp.setMargins(navMargin, navMargin, navMargin, navMargin);
        
        nav.addView(btnPrev, navLp); 
        nav.addView(btnNext, navLp); 
        nav.addView(btnWipe, navLp); 
        nav.addView(btnBack, navLp);
        explorerState.addView(nav);

        rootLayer.addView(explorerState);

        return rootLayer;
    }

    private View createTableMenuButton(String tName, String tLabel) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int rp = scale(50);
        row.setPadding(rp, 0, rp, 0);

        // 🚀 L7 Enterprise Gradient Button (Aligned with UiArchitect Radius and Physics)
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{getGradientStart(), getGradientEnd()});
        gd.setCornerRadius(getResponsiveRadiusQuota());
        gd.setStroke(scale(3), Color.WHITE);
        row.setBackground(gd);
        
        // 🚀 Depth & Shadow Optimization: Unified Z-axis elevation via applyCardWithOverride
        UiArchitect.applyCardWithOverride(this, row, "CARD_STANDARD", 1.2f);

        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_agenda);
        icon.setColorFilter(getGradientOverlayText());
        int iconSize = scale(80);
        row.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(scale(30), 0, 0, 0);

        TextView tvName = new TextView(this);
        tvName.setText(tName.replace("_table", "").toUpperCase());
        tvName.setTextColor(getGradientOverlayText());
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        UiArchitect.applyText(this, tvName, "TEXT_TITLE");
        text.addView(tvName);

        TextView tvSub = new TextView(this);
        tvSub.setText(tLabel);
        tvSub.setTextColor(getGradientOverlayText());
        tvSub.setAlpha(0.85f);
        UiArchitect.applyText(this, tvSub, "TEXT_LABEL"); // 🚀 Purged hardcoded 12
        text.addView(tvSub);

        row.addView(text);
        row.setClickable(true);
        row.setFocusable(true);
        applyAccessibility(row, "Navigate to " + tLabel);
        row.setOnClickListener(v -> {
            activeTable = tName;
            pOffset = 0;
            menuState.setVisibility(View.GONE);
            explorerState.setVisibility(View.VISIBLE);
            renderExplorerUI();
        });

        return row;
    }

    private void renderExplorerUI() {
        tableHeader.removeAllViews();
        tableData.removeAllViews();

        try {
            List<String> cols = db.getTableColumns(activeTable);
            List<Map<String, String>> rows = db.getPagedData(activeTable, pSize, pOffset, currentSearch);
            long total = db.getRecordCount(activeTable);

            // 🚀 DYNAMIC FORENSIC HEADER
            TableRow hRow = new TableRow(this);
            int hp = scale(45);
            for (String c : cols) {
                TextView tv = new TextView(this);
                tv.setText(c.toUpperCase());
                tv.setPadding(hp, scale(30), hp, scale(30));
                tv.setTextColor(getGradientOverlayText());
                tv.setBackgroundColor(getGradientStart());
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                UiArchitect.applyText(this, tv, "TEXT_LABEL");
                hRow.addView(tv);
            }
            tableHeader.addView(hRow);

            // 🚀 DYNAMIC DATA ROWS (With Alternating Contrast)
            int count = 0;
            int bp = scale(45);
            for (Map<String, String> r : rows) {
                TableRow bRow = new TableRow(this);
                bRow.setBackgroundColor(count % 2 == 0 ? Color.argb(15, 255, 255, 255) : Color.TRANSPARENT);
                for (String c : cols) {
                    TextView tv = new TextView(this);
                    tv.setText(r.containsKey(c) ? r.get(c) : "---");
                    tv.setTextColor(getTextColor());
                    tv.setPadding(bp, scale(25), bp, scale(25));
                    UiArchitect.applyText(this, tv, "TEXT_BODY");
                    bRow.addView(tv);
                }
                tableData.addView(bRow);
                count++;
            }
            tvStats.setText("INVESTIGATING: [ " + activeTable.toUpperCase() + " ] | Records: " + (pOffset + 1) + " - " + (pOffset + rows.size()) + " of " + total);
        } catch (Exception e) {
            tvStats.setText("CRITICAL: [ " + e.getMessage() + " ]");
            SystemLogger.e("AnalyticalHub", "Explorer Failure", e);
        }
    }

    @Override
    protected void setupLogic() {}
}
