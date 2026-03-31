package com.jdroidx.jfindme.ui.management;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import com.jdroidx.jfindme.data.local.CitizenDbHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;

import java.io.File;
import java.io.FileOutputStream;

public class FactoryResetActivity extends BaseActivityJava {

    private LinearLayout rootContainer;
    private StringBuilder exportDataBuilder = new StringBuilder();

    @Override
    protected View createLayout() {
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
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Review Offline Cache Matrix before System Purge.");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setTextSize(12);
        subtitle.setAlpha(0.8f);
        header.addView(subtitle);
        
        mainLayout.addView(header);

        // Content
        ScrollView scroll = new ScrollView(this);
        rootContainer = new LinearLayout(this);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setPadding(40, 20, 40, 100);

        // Prepare File/DB sizes
        File dbFile = getDatabasePath("JFindMe_CitizenData.db");
        long dbSizeBytes = dbFile.exists() ? dbFile.length() : 0;
        String sizeFormatted = android.text.format.Formatter.formatFileSize(this, dbSizeBytes);

        exportDataBuilder.append("=== JFINDME OFFLINE DATABASE DUMP ===\n");
        exportDataBuilder.append("TOTAL DATABASE SIZE: ").append(sizeFormatted).append("\n\n");

        LinearLayout dbHeader = new LinearLayout(this);
        dbHeader.setOrientation(LinearLayout.HORIZONTAL);
        dbHeader.setGravity(Gravity.CENTER_VERTICAL);
        dbHeader.setPadding(0, 0, 0, 20);

        TextView tvDbSummary = new TextView(this);
        tvDbSummary.setText("DATABASE METRICS (Size: " + sizeFormatted + ")");
        tvDbSummary.setTextColor(getGradientStart());
        tvDbSummary.setTextSize(14);
        tvDbSummary.setTypeface(null, Typeface.BOLD);
        dbHeader.addView(tvDbSummary, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        ImageView refreshIcon = new ImageView(this);
        // 🚀 QA Fix: Force Theme-Aware Sync Icon from Master Registry
        refreshIcon.setImageResource(android.R.drawable.ic_popup_sync);
        refreshIcon.setColorFilter(getGradientStart());
        
        // 🚀 L7 Architecture Sync: Dynamically scale icon per device stage
        int dishSize = com.jdroidx.jfindme.ui.base.UiArchitect.getResponsiveIconSize(this, "ICON_STANDARD");
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dishSize, dishSize);
        iconLp.setMargins(20, 20, 20, 20);
        
        applyAccessibility(refreshIcon, "Refresh Database Matrix");
        refreshIcon.setOnClickListener(v -> refreshDatabaseMatrix());
        
        dbHeader.addView(refreshIcon, iconLp);
        rootContainer.addView(dbHeader);

        refreshDatabaseMatrix();

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
                // 1. Wipe SharedPreferences (SYNC FORCE COMMIT)
                getSharedPreferences("jFindMe_Prefs", Context.MODE_PRIVATE).edit().clear().commit(); 
                
                // 2. Erase the Entire SQLite Database structure
                deleteDatabase("JFindMe_CitizenData.db");
                
                // 🚀 QA Fix: Stop the OS Background Daemon so it stops writing Location telemetry!
                stopService(new android.content.Intent(this, com.jdroidx.jfindme.service.TrackingService.class));
                
                // 🚀 QA Fix: Obliterate completely Android OS internal/external App Cache + Secure Files Director!
                try {
                    deleteRecursively(getCacheDir());
                    if (getExternalCacheDir() != null) {
                        deleteRecursively(getExternalCacheDir());
                    }
                    // Wipes all cropped User Avatars and Custom internal app files dynamically
                    java.io.File[] physicalFiles = getFilesDir().listFiles();
                    if (physicalFiles != null) {
                        for (java.io.File f : physicalFiles) deleteRecursively(f);
                    }
                } catch (Exception ignored) {}
                
                // 🚀 QA Fix: Flush the Android Display Notification tray in case Sirens are actively ringing!
                android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancelAll();
                
                // 3. Sever Firebase Keys (This triggers an async background thread internally!)
                FirebaseAuth.getInstance().signOut();
                
                com.jdroidx.jfindme.utils.AngelLoader.show(this);
                
                // 4. Wrap the process destruct sequence in an intelligent Handler.
                // This grants Firebase absolute asynchronous freedom to shred backend matrices 
                // while treating the user to a gorgeous, calming 3.0-second UI fading transition.
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
    protected void setupLogic() {
        // Render triggers implicitly above via createLayout pipeline.
    }

    private void refreshDatabaseMatrix() {
        // Clear previous render but keep the header
        if (rootContainer.getChildCount() > 1) {
            rootContainer.removeViews(1, rootContainer.getChildCount() - 1);
        }
        
        exportDataBuilder = new StringBuilder();
        exportDataBuilder.append("=== JFINDME OFFLINE DATABASE DUMP ===\n");
        
        try {
            CitizenDbHelper helper = new CitizenDbHelper(this);
            SQLiteDatabase db = helper.getReadableDatabase();
            
            // 🚀 QA Fix: Force re-indexing of all silicon-mapped tables including roster_table
            android.database.Cursor cTables = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'", null);
            
            while (cTables.moveToNext()) {
                String tableName = cTables.getString(0);
                
                android.database.Cursor cRows = db.rawQuery("SELECT * FROM " + tableName, null);
                int rowCount = cRows.getCount();
                
                exportDataBuilder.append("--------------------------------------------------\n");
                exportDataBuilder.append("TABLE: ").append(tableName).append(" | ROWS: ").append(rowCount).append("\n");
                exportDataBuilder.append("--------------------------------------------------\n");
                
                TextView tvTableHeader = new TextView(this);
                tvTableHeader.setText("▶ TABLE: " + tableName.toUpperCase() + " (" + rowCount + " RECORDS)");
                tvTableHeader.setTextColor(getTextColor());
                tvTableHeader.setTextSize(14f);
                tvTableHeader.setTypeface(null, Typeface.BOLD);
                tvTableHeader.setPadding(0, 40, 0, 10);
                rootContainer.addView(tvTableHeader);
                
                if (rowCount == 0) {
                    TextView tvEmpty = new TextView(this);
                    tvEmpty.setText("NO OFFLINE DATA DETECTED IN " + tableName.toUpperCase());
                    tvEmpty.setTextColor(getSecondaryTextColor());
                    tvEmpty.setAlpha(0.6f);
                    tvEmpty.setPadding(20, 10, 20, 10);
                    rootContainer.addView(tvEmpty);
                    exportDataBuilder.append("No Data.\n\n");
                    cRows.close();
                    continue;
                }
                
                HorizontalScrollView hScroll = new HorizontalScrollView(this);
                TableLayout tableLayout = new TableLayout(this);
                // Apply a deep frosted glass card effect for the matrix tiles
                applySolidCardGeometry(tableLayout, getCardColor(), 10f);
                tableLayout.setPadding(5, 5, 5, 5);
                
                // 🚀 L7 Optimization: Vertical Scroll Container (MAX 10 ROWS)
                LinearLayout scrollContainer = new LinearLayout(this);
                scrollContainer.setOrientation(LinearLayout.VERTICAL);
                if (rowCount > 10) {
                    ScrollView vScroll = new ScrollView(this);
                    // Standardize height quota to prevent screen explosion
                    vScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800));
                    vScroll.addView(tableLayout);
                    scrollContainer.addView(vScroll);
                } else {
                    scrollContainer.addView(tableLayout);
                }
                
                TableRow headerRow = new TableRow(this);
                String[] columns = cRows.getColumnNames();
                for (String col : columns) {
                    exportDataBuilder.append(col).append(" | ");
                    
                    TextView tvCol = new TextView(this);
                    tvCol.setText(col.toUpperCase());
                    tvCol.setTextColor(getPrimaryColor());
                    tvCol.setBackgroundColor(getGradientStart());
                    tvCol.setPadding(30, 20, 30, 20);
                    tvCol.setTextSize(11f);
                    tvCol.setTypeface(null, Typeface.BOLD);
                    
                    TableRow.LayoutParams tlp = new TableRow.LayoutParams();
                    tlp.setMargins(2, 2, 2, 2);
                    headerRow.addView(tvCol, tlp);
                }
                exportDataBuilder.append("\n");
                tableLayout.addView(headerRow);
                
                // 🚀 V37 FINAL RECONSTRUCTION: Simplified Explorer (Redirects to DatabaseManagementActivity for clusters)
                while (cRows.moveToNext()) {
                    TableRow dataRow = new TableRow(this);
                    for (int i = 0; i < columns.length; i++) {
                        String val = "---";
                        try { val = cRows.getString(i); } catch (Exception ignore) {}
                        if (val == null) val = "NULL";
                        exportDataBuilder.append(val).append(" | ");
                        
                        TextView tvVal = new TextView(this);
                        tvVal.setText(val);
                        tvVal.setTextColor(getSecondaryTextColor());
                        tvVal.setTextSize(11f);
                        tvVal.setPadding(35, 20, 35, 20);
                        
                        if (cRows.getPosition() % 2 == 0) tvVal.setBackgroundColor(Color.argb(15, 255, 255, 255));
                        else tvVal.setBackgroundColor(Color.TRANSPARENT);
                        
                        TableRow.LayoutParams tlp = new TableRow.LayoutParams();
                        tlp.setMargins(2, 2, 2, 2);
                        dataRow.addView(tvVal, tlp);
                    }
                    exportDataBuilder.append("\n");
                    tableLayout.addView(dataRow);
                }
                cRows.close();
                exportDataBuilder.append("\n\n");
                
                hScroll.addView(scrollContainer);
                rootContainer.addView(hScroll);
            }
            cTables.close();
            db.close();
            
            // Re-add Action Buttons at the bottom
            LinearLayout btnBox = new LinearLayout(this);
            btnBox.setOrientation(LinearLayout.VERTICAL);
            btnBox.setPadding(0, 60, 0, 80);

            btnBox.addView(createIconButton("SAVE TO LOCAL TXT", android.R.drawable.ic_menu_save, v -> saveToLocalText()));
            btnBox.addView(createIconButton("DATABASE EXPLORER (V37)", android.R.drawable.ic_menu_edit, v -> {
                startActivity(new android.content.Intent(this, DatabaseManagementActivity.class));
            }));
            btnBox.addView(createIconButton("DELETE ALL & PURGE DB", android.R.drawable.ic_menu_delete, v -> executePurge()));

            rootContainer.addView(btnBox);
            
            Toast.makeText(this, "Database Matrix Synchronized.", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setText("Database Sync Error: " + e.getMessage());
            error.setTextColor(Color.RED);
            rootContainer.addView(error);
        }
    }

    private void addValToRow(TableRow row, String val) {
        TextView tv = new TextView(this);
        tv.setText(val != null ? val : "N/A");
        tv.setTextColor(getSecondaryTextColor());
        tv.setTextSize(11f);
        tv.setPadding(35, 20, 35, 20);
        row.addView(tv);
    }

    // 🚀 QA Tool: Extremely Violent Native File Recursion (Destroys Cache directories structurally)
    private void deleteRecursively(File fileOrDirectory) {
        if (fileOrDirectory != null && fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (fileOrDirectory != null) {
            fileOrDirectory.delete(); // OS-Level Direct Instruction
        }
    }
}
