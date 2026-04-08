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
 * Universal Analytical Hub (V38 NAVIGATION HARDENING)
 * Objective: High-impact Gradient Menu with Sequence-Correct Back Navigation.
 * Hardening: L7 Architectural Registry Parity & V36 Database Migration.
 */
public class DatabaseManagementActivity extends BaseActivityJava {

    private DatabaseHelper db;
    private FrameLayout rootLayer;
    private LinearLayout menuState;

    @Override
    protected View createLayout() {
        db = DatabaseHelper.getInstance(this);
        rootLayer = new FrameLayout(this);
        rootLayer.setBackgroundColor(getPrimaryColor());

        // 🧱 STATE 1: HIGH-IMPACT GRADIENT MENU
        menuState = new LinearLayout(this);
        menuState.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout menuHeader = new LinearLayout(this);
        menuHeader.setOrientation(LinearLayout.HORIZONTAL);
        menuHeader.setGravity(Gravity.CENTER_VERTICAL);
        applyGlobalHeaderGeometry(menuHeader);
        
        // 🚀 NAVIGATION FIX: Back Button Integration
        ImageView backBtn = new ImageView(this);
        backBtn.setImageResource(android.R.drawable.ic_menu_revert); 
        backBtn.setColorFilter(getGradientOverlayText());
        int bSize = scale(120);
        backBtn.setPadding(scale(20), scale(20), scale(20), scale(20));
        backBtn.setOnClickListener(v -> finish());
        menuHeader.addView(backBtn, new LinearLayout.LayoutParams(bSize, bSize));

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(scale(30), 0, 0, 0);

        TextView menuTitle = new TextView(this);
        menuTitle.setText("FORENSIC VAULT");
        menuTitle.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, menuTitle, "TEXT_HEADER");
        textLayout.addView(menuTitle);
        
        TextView menuSub = new TextView(this);
        menuSub.setText("L7 Universal Analytical Registry");
        menuSub.setTextColor(getGradientOverlayText());
        menuSub.setAlpha(0.8f);
        UiArchitect.applyText(this, menuSub, "TEXT_SUB");
        textLayout.addView(menuSub);
        
        menuHeader.addView(textLayout, new LinearLayout.LayoutParams(0, -2, 1.0f));
        menuState.addView(menuHeader);

        ScrollView menuScroll = new ScrollView(this);
        LinearLayout menuContent = new LinearLayout(this);
        menuContent.setOrientation(LinearLayout.VERTICAL);
        int q = getResponsivePaddingQuota();
        menuContent.setPadding(q, q, q, q * 2);
        menuContent.setClipChildren(false); 
        menuContent.setClipToPadding(false);

        String[] tables = db.getAllTables();
        for (String tName : tables) {
            String tLabel = getTableFriendlyLabel(tName);
            View btn = createTableMenuButton(tName, tLabel);
            int btnHeight = scale(180); 
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, btnHeight);
            lp.setMargins(0, 0, 0, scale(30));
            menuContent.addView(btn, lp);
        }
        
        // 🚀 MASTER HOME REDIRECT: Sending user back to the Factory Reset Console
        View btnReset = UiArchitect.createButton(this, "FACTORY_RESET", v -> finish());
        if (btnReset instanceof TextView) {
            ((TextView)btnReset).setText("RETURN TO SYSTEM CONSOLE");
        }
        menuContent.addView(btnReset);
        
        menuScroll.addView(menuContent);
        menuState.addView(menuScroll);
        rootLayer.addView(menuState);

        return rootLayer;
    }

    private String getTableFriendlyLabel(String tName) {
        if (tName.equals("profile_table")) return "Identity Dossier";
        if (tName.equals("device_table")) return "Hardware Identity Ledger";
        if (tName.equals("roster_table")) return "P2P Network Matrix";
        if (tName.equals("org_table")) return "Enterprise Dossier";
        if (tName.equals("history_table")) return "Forensic Archive";
        if (tName.equals("SafeZone_Table")) return "Boundary Matrix";
        if (tName.equals("GeoFence_Alert_table")) return "Siren Logs";
        if (tName.equals("pairing_log")) return "Pairing Ledger";
        if (tName.equals("sim_inventory")) return "SIM Inventory";
        if (tName.equals("sim_transaction")) return "SIM Transitions";
        if (tName.equals("geofenc_sync_queue_table")) return "Sync Queue";
        if (tName.equals("SafeZoneGroup_Table")) return "Zone Clusters";
        if (tName.equals("GeofenceLog_Table")) return "Geofence Transitions";
        return tName.replace("_table", "").toUpperCase();
    }

    private View createTableMenuButton(String tName, String tLabel) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int rp = scale(50);
        row.setPadding(rp, 0, rp, 0);

        UiArchitect.applyCardWithOverride(this, row, "CARD_STANDARD", 1.5f);
        row.setClipToOutline(false); 
        
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{getGradientStart(), getGradientEnd()});
        gd.setCornerRadius(getResponsiveRadiusQuota());
        gd.setStroke(scale(3), Color.WHITE);
        row.setBackground(gd);

        ImageView gear = new ImageView(this);
        gear.setImageResource(android.R.drawable.ic_menu_preferences); 
        gear.setColorFilter(getGradientOverlayText());
        int gearSize = scale(100);
        row.addView(gear, new LinearLayout.LayoutParams(gearSize, gearSize));
        gear.setOnClickListener(v -> {
            android.content.Intent structure = new android.content.Intent(this, ForensicStructureActivity.class);
            structure.putExtra("TABLE_NAME", tName);
            structure.putExtra("TABLE_LABEL", tLabel);
            startActivity(structure);
        });

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
        UiArchitect.applyText(this, tvSub, "TEXT_LABEL"); 
        text.addView(tvSub);

        row.addView(text);
        row.setClickable(true);
        row.setFocusable(true);
        applyAccessibility(row, "Navigate to " + tLabel);
        row.setOnClickListener(v -> {
            android.content.Intent explorer = new android.content.Intent(this, ForensicVaultExplorerActivity.class);
            explorer.putExtra("TABLE_NAME", tName);
            explorer.putExtra("TABLE_LABEL", tLabel);
            startActivity(explorer);
        });

        return row;
    }

    @Override
    protected void setupLogic() {}
}
