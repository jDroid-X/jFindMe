package com.jdroidx.jfindme.ui.management;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Hard Reset Hub (V37 FINAL RECONSTRUCTION)
 * Objective: Violently purge all local forensic assets and reconstruct SSOT.
 * Hardening: L7 Architectural Registry Parity & V36 Database Migration.
 */
public class FactoryResetActivity extends BaseActivityJava {

    private LinearLayout rootContainer;
    private StringBuilder exportDataBuilder = new StringBuilder();
    private DatabaseHelper db;

    @Override
    protected View createLayout() {
        db = DatabaseHelper.getInstance(this);
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(getPrimaryColor());

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);
        
        TextView title = new TextView(this);
        title.setText("FACTORY RESET CONSOLE");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Review Offline Cache Matrix before System Purge.");
        subtitle.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        header.addView(subtitle);
        
        mainLayout.addView(header);

        // Content
        ScrollView scroll = new ScrollView(this);
        rootContainer = new LinearLayout(this);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        int q = getResponsivePaddingQuota();
        rootContainer.setPadding(q * 2, q, q * 2, q * 5);

        // Prepare File/DB sizes
        File dbFile = getDatabasePath("JFindMe_CitizenData.db");
        long dbSizeBytes = dbFile.exists() ? dbFile.length() : 0;
        String sizeFormatted = android.text.format.Formatter.formatFileSize(this, dbSizeBytes);

        exportDataBuilder.append("=== JFINDME OFFLINE DATABASE DUMP ===\n");
        exportDataBuilder.append("TOTAL DATABASE SIZE: ").append(sizeFormatted).append("\n\n");

        LinearLayout dbHeader = new LinearLayout(this);
        dbHeader.setOrientation(LinearLayout.HORIZONTAL);
        dbHeader.setGravity(Gravity.CENTER_VERTICAL);
        dbHeader.setPadding(0, 0, 0, scale(20));

        TextView tvDbSummary = new TextView(this);
        tvDbSummary.setText("DATABASE METRICS (Size: " + sizeFormatted + ")");
        tvDbSummary.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvDbSummary, "TEXT_LABEL");
        dbHeader.addView(tvDbSummary, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        ImageView refreshIcon = new ImageView(this);
        refreshIcon.setImageResource(android.R.drawable.ic_popup_sync);
        refreshIcon.setColorFilter(getGradientStart());
        
        int dishSize = UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dishSize, dishSize);
        int m = scale(15);
        iconLp.setMargins(m, m, m, m);
        
        applyAccessibility(refreshIcon, "Refresh Database Matrix");
        refreshIcon.setOnClickListener(v -> refreshDatabaseMatrix());
        
        dbHeader.addView(refreshIcon, iconLp);
        rootContainer.addView(dbHeader);

        refreshDatabaseMatrix();

        scroll.addView(rootContainer);
        mainLayout.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainLayout;
    }

    private void saveToLocalText() {
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) dir = getFilesDir();
            File file = new File(dir, "JFindMe_Database_Dump.txt");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(exportDataBuilder.toString().getBytes());
            fos.close();
            Toast.makeText(this, "Saved Database Dump to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void executePurge() {
        new AlertDialog.Builder(this)
            .setTitle("WARNING: PERMANENT PURGE")
            .setMessage("This will irrevocably delete the internal offline database, wipe all authentication keys, clear your local SharedPreferences cache, and force close the application.\n\nOn the next launch, the application will behave exactly like a fresh binary install.\n\nAre you absolutely sure you want to Wipe All Data?")
            .setPositiveButton("DELETE ALL & CLOSE APP", (dialog, which) -> {
                getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE).edit().clear().commit(); 
                deleteDatabase("JFindMe_CitizenData.db");
                stopService(new android.content.Intent(this, com.jdroidx.jfindme.service.TrackingService.class));
                
                try {
                    deleteRecursively(getCacheDir());
                    if (getExternalCacheDir() != null) {
                        deleteRecursively(getExternalCacheDir());
                    }
                    java.io.File[] physicalFiles = getFilesDir().listFiles();
                    if (physicalFiles != null) {
                        for (java.io.File f : physicalFiles) deleteRecursively(f);
                    }
                } catch (Exception ignored) {}
                
                android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancelAll();
                
                FirebaseAuth.getInstance().signOut();
                com.jdroidx.jfindme.utils.AngelLoader.show(this);
                
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    Toast.makeText(this, "Master Database Purged. Rebooting Terminal...", Toast.LENGTH_SHORT).show();
                    android.content.Intent restart = new android.content.Intent(this, com.jdroidx.jfindme.ui.base.SplashActivity.class);
                    restart.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(restart);
                    finish();
                }, 3000);
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    @Override
    protected void setupLogic() {}

    private void refreshDatabaseMatrix() {
        if (rootContainer.getChildCount() > 1) {
            rootContainer.removeViews(1, rootContainer.getChildCount() - 1);
        }
        
        exportDataBuilder = new StringBuilder();
        exportDataBuilder.append("=== JFINDME OFFLINE DATABASE DUMP ===\n");
        
        String[] tables = {"personal_table", "roster_table", "org_table", "SafeZone_Table", "GeoFence_Alert_table", "history_table", "pairing_log", "sim_inventory"};
        
        for (String tableName : tables) {
            long rowCount = db.getRecordCount(tableName);
            
            exportDataBuilder.append("--------------------------------------------------\n");
            exportDataBuilder.append("TABLE: ").append(tableName).append(" | ROWS: ").append(rowCount).append("\n");
            exportDataBuilder.append("--------------------------------------------------\n");
            
            TextView tvTableHeader = new TextView(this);
            tvTableHeader.setText("▶ TABLE: " + tableName.toUpperCase() + " (" + rowCount + " RECORDS)");
            tvTableHeader.setTextColor(getTextColor());
            UiArchitect.applyText(this, tvTableHeader, "TEXT_TITLE");
            tvTableHeader.setPadding(0, scale(40), 0, scale(15));
            rootContainer.addView(tvTableHeader);
            
            if (rowCount == 0) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("NO OFFLINE DATA DETECTED IN " + tableName.toUpperCase());
                tvEmpty.setTextColor(getSecondaryTextColor());
                tvEmpty.setAlpha(0.6f);
                tvEmpty.setPadding(scale(20), scale(10), scale(20), scale(10));
                rootContainer.addView(tvEmpty);
                exportDataBuilder.append("No Data.\n\n");
                continue;
            }
            
            HorizontalScrollView hScroll = new HorizontalScrollView(this);
            TableLayout tableLayout = new TableLayout(this);
            UiArchitect.applyCardWithOverride(this, tableLayout, "CARD_STANDARD", 1.0f);
            int tp = scale(10);
            tableLayout.setPadding(tp, tp, tp, tp);
            
            LinearLayout scrollContainer = new LinearLayout(this);
            scrollContainer.setOrientation(LinearLayout.VERTICAL);
            if (rowCount > 10) {
                ScrollView vScroll = new ScrollView(this);
                vScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scale(600)));
                vScroll.addView(tableLayout);
                scrollContainer.addView(vScroll);
            } else {
                scrollContainer.addView(tableLayout);
            }
            
            TableRow headerRow = new TableRow(this);
            List<String> columns = db.getTableColumns(tableName);
            for (String col : columns) {
                exportDataBuilder.append(col).append(" | ");
                TextView tvCol = new TextView(this);
                tvCol.setText(col.toUpperCase());
                tvCol.setTextColor(getPrimaryColor());
                tvCol.setBackgroundColor(getGradientStart());
                tvCol.setPadding(scale(30), scale(20), scale(30), scale(20));
                tvCol.setGravity(Gravity.CENTER);
                UiArchitect.applyText(this, tvCol, "TEXT_LABEL");
                headerRow.addView(tvCol);
            }
            exportDataBuilder.append("\n");
            tableLayout.addView(headerRow);
            
            List<Map<String, String>> rows = db.getPagedData(tableName, 10, 0, null);
            for (Map<String, String> rowMap : rows) {
                TableRow dataRow = new TableRow(this);
                for (String col : columns) {
                    String val = rowMap.containsKey(col) ? rowMap.get(col) : "---";
                    exportDataBuilder.append(val).append(" | ");
                    TextView tvVal = new TextView(this);
                    tvVal.setText(val);
                    tvVal.setTextColor(getSecondaryTextColor());
                    tvVal.setGravity(Gravity.CENTER);
                    UiArchitect.applyText(this, tvVal, "TEXT_MINI");
                    tvVal.setPadding(scale(35), scale(20), scale(35), scale(20));
                    dataRow.addView(tvVal);
                }
                exportDataBuilder.append("\n");
                tableLayout.addView(dataRow);
            }
            exportDataBuilder.append("\n\n");
            hScroll.addView(scrollContainer);
            rootContainer.addView(hScroll);
        }
        
        // Re-add Action Buttons
        LinearLayout btnBox = new LinearLayout(this);
        btnBox.setOrientation(LinearLayout.VERTICAL);
        btnBox.setPadding(0, scale(60), 0, scale(80));

        btnBox.addView(UiArchitect.createButton(this, "SAVE_TO_LOCAL_TXT", v -> saveToLocalText()));
        btnBox.addView(UiArchitect.createButton(this, "DATABASE_EXPLORER_V37", v -> {
            startActivity(new android.content.Intent(this, DatabaseManagementActivity.class));
        }));
        btnBox.addView(UiArchitect.createButton(this, "DELETE_ALL_PURGE_DB", v -> executePurge()));

        rootContainer.addView(btnBox);
        Toast.makeText(this, "Database Matrix Synchronized.", Toast.LENGTH_SHORT).show();
    }

    private void deleteRecursively(File fileOrDirectory) {
        if (fileOrDirectory != null && fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        if (fileOrDirectory != null) fileOrDirectory.delete();
    }
}
