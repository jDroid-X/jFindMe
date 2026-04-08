package com.jdroidx.jfindme.ui.management;

import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.AngelLoader;
import com.jdroidx.jfindme.utils.SystemLogger;
import java.util.*;

/**
 * 👑 FORENSIC VAULT EXPLORER (L7 Global UI Architect Sync)
 * Objective: Standardized, high-fidelity spreadsheet view for Online/Offline data comparison.
 * Format: Compact Excel Sheet with Cell Borders & Landscape Optimization.
 */
public class ForensicVaultExplorerActivity extends BaseActivityJava {

    private String tableName, tableLabel;
    private DatabaseHelper db;
    private DatabaseReference cloudRef;
    
    private LinearLayout contentMatrix;
    private TextView tvTableMeta;
    private boolean isComparisonMode = true;

    @Override
    protected View createLayout() {
        tableName = getIntent().getStringExtra("TABLE_NAME");
        tableLabel = getIntent().getStringExtra("TABLE_LABEL");
        db = DatabaseHelper.getInstance(this);
        cloudRef = FirebaseDatabase.getInstance().getReference().child(tableName);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // 🚀 L7 Header Geometry
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);
        
        TextView title = new TextView(this);
        title.setText(tableLabel != null ? tableLabel.toUpperCase() : "VAULT EXPLORER");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);
        
        TextView sub = new TextView(this);
        sub.setText("Forensic Vault Explorer | " + tableName);
        sub.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, sub, "TEXT_SUB");
        header.addView(sub);
        root.addView(header);

        // 🚀 Toolbar / Switcher
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(scale(30), scale(20), scale(30), scale(20));
        toolbar.setBackgroundColor(Color.argb(30, 0, 0, 0));
        
        View btnRefresh = UiArchitect.createButton(this, "REFRESH", v -> syncExplorer());
        View btnExport = UiArchitect.createButton(this, "EXPORT_XLS", v -> Toast.makeText(this, "Exporting to Forensic Excel...", Toast.LENGTH_SHORT).show());
        
        LinearLayout.LayoutParams toolLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        toolbar.addView(btnRefresh, toolLp);
        toolbar.addView(btnExport, toolLp);
        root.addView(toolbar);

        tvTableMeta = new TextView(this);
        tvTableMeta.setPadding(scale(40), scale(20), scale(40), scale(20));
        tvTableMeta.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvTableMeta, "TEXT_LABEL");
        root.addView(tvTableMeta);

        ScrollView vScroll = new ScrollView(this);
        contentMatrix = new LinearLayout(this);
        contentMatrix.setOrientation(LinearLayout.VERTICAL);
        int p = getResponsivePaddingQuota();
        contentMatrix.setPadding(p, p, p, p * 4);
        
        vScroll.addView(contentMatrix);
        root.addView(vScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return root;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 🚀 FORCE LANDSCAPE FOR MAXIMUM EXCEL VISIBILITY
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    protected void setupLogic() {
        syncExplorer();
    }

    private void syncExplorer() {
        AngelLoader.show(this);
        AngelLoader.setStatus(this, "SYNCHRONIZING FORENSIC MATRIX...");
        
        // 🚀 CLOUD PATH RESOLVER: Map local table names to their Firebase JSON architecture
        DatabaseReference activeTarget = cloudRef;
        String activeGroup = getSharedPreferences("system_registry", MODE_PRIVATE).getString("active_group", null);
        
        if (tableName.equals("profile_table")) {
            activeTarget = FirebaseDatabase.getInstance().getReference("users");
        } else if (tableName.equals("roster_table") && activeGroup != null) {
            activeTarget = FirebaseDatabase.getInstance().getReference("groups").child(activeGroup).child("roster");
        } else if (tableName.equals("history_table")) {
            activeTarget = FirebaseDatabase.getInstance().getReference("tracking_history");
        } else if (tableName.equals("device_table")) {
            activeTarget = FirebaseDatabase.getInstance().getReference("users"); // Hardware often nested or in bulk users
        }
        
        activeTarget.limitToLast(20).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AngelLoader.hide(ForensicVaultExplorerActivity.this);
                renderSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AngelLoader.hide(ForensicVaultExplorerActivity.this);
                renderSnapshot(null);
                Toast.makeText(ForensicVaultExplorerActivity.this, "Cloud Sync Offline", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSnapshot(DataSnapshot cloudSnapshot) {
        contentMatrix.removeAllViews();
        List<String> columns = db.getTableColumns(tableName);
        List<Map<String, String>> localRows = db.getPagedData(tableName, 30, 0, null);
        
        tvTableMeta.setText("VAULT: [" + tableName + "] | OFFLINE: " + localRows.size() + " | ONLINE: " + (cloudSnapshot != null ? cloudSnapshot.getChildrenCount() : 0));

        renderSectionHeader("OFFLINE LEDGER (LOCAL SQLITE)");
        renderTable(localRows, columns, true);

        if (cloudSnapshot != null) {
            renderSectionHeader("ONLINE REGISTRY (FIREBASE HUB)");
            List<Map<String, String>> cloudRows = new ArrayList<>();
            for (DataSnapshot ds : cloudSnapshot.getChildren()) {
                Map<String, String> row = new HashMap<>();
                row.put("_LOG_ID", ds.getKey());
                Object v = ds.getValue();
                if (v instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) v;
                    for (Map.Entry<?, ?> e : m.entrySet()) row.put(e.getKey().toString(), String.valueOf(e.getValue()));
                }
                cloudRows.add(row);
            }
            renderTable(cloudRows, columns, false);
        }
    }

    private void renderSectionHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(getTextColor());
        tv.setPadding(0, scale(40), 0, scale(20));
        tv.setGravity(Gravity.CENTER);
        UiArchitect.applyText(this, tv, "TEXT_SUB");
        contentMatrix.addView(tv);
    }
    private void renderTable(List<Map<String, String>> rows, List<String> columns, boolean isLocal) {
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(true);
        
        // 🚀 L7 DEPTH: Apply subtle shadow and corner radius to the table matrix
        UiArchitect.applyCard(this, hScroll, "CARD_STANDARD");
        
        // 🚀 Header Row: L7 Theme Background + Cell Borders
        TableRow hRow = new TableRow(this);
        hRow.setBackgroundColor(getGradientStart());
        hRow.setLayoutParams(new TableLayout.LayoutParams(-1, -2));
        
        hRow.addView(createCell("COMMANDS", true));
        for (String col : columns) {
            hRow.addView(createCell(col.toUpperCase(), true));
        }
        table.addView(hRow);

        // 🚀 Data Rows: Compact Matrix with Cell Isolation
        for (Map<String, String> row : rows) {
            TableRow dRow = new TableRow(this);
            
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER);
            actions.setBackground(createCellBorder(false));
            
            if (isLocal) {
                ImageView del = new ImageView(this);
                del.setImageResource(android.R.drawable.ic_menu_delete);
                del.setColorFilter(Color.RED);
                int s = scale(60); 
                del.setPadding(scale(5), scale(5), scale(5), scale(5));
                del.setOnClickListener(v -> deleteLocalRecord(row));
                actions.addView(del, new LinearLayout.LayoutParams(s, s));

                ImageView edit = new ImageView(this);
                edit.setImageResource(android.R.drawable.ic_menu_edit);
                edit.setColorFilter(getGradientStart());
                edit.setPadding(scale(5), scale(5), scale(5), scale(5));
                edit.setOnClickListener(v -> editLocalRecord(row));
                actions.addView(edit, new LinearLayout.LayoutParams(s, s));
            } else {
                TextView tvCloud = new TextView(this);
                tvCloud.setText("CLOUD");
                tvCloud.setTextSize(9);
                tvCloud.setPadding(scale(10), 0, scale(10), 0);
                actions.addView(tvCloud);
            }
            
            dRow.addView(actions);

            for (String col : columns) {
                String val = row.get(col);
                dRow.addView(createCell(val != null ? val : "---", false));
            }
            table.addView(dRow);
        }
        
        hScroll.addView(table);
        contentMatrix.addView(hScroll);
    }

    private android.graphics.drawable.Drawable createCellBorder(boolean isHeader) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        if (isHeader) {
            gd.setColor(getGradientStart());
            gd.setStroke(scale(2), getGradientOverlayText());
        } else {
            gd.setColor(Color.TRANSPARENT);
            gd.setStroke(scale(1), Color.argb(100, 255, 255, 255)); // Subtle White Border for Dark Mode
        }
        return gd;
    }

    private TextView createCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(scale(30), scale(20), scale(30), scale(20)); 
        tv.setSingleLine(true);
        tv.setBackground(createCellBorder(isHeader));
        tv.setGravity(Gravity.CENTER);
        
        if (isHeader) {
            tv.setTextColor(getGradientOverlayText());
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextSize(14f); 
        } else {
            // 🚀 L7 VIBRANCY: Ensure data is clearly visible on the dark background matrix
            tv.setTextColor(Color.WHITE); 
            tv.setTextSize(13f);
        }
        return tv;
    }

    private void deleteLocalRecord(Map<String, String> row) {
        String idCol = findIdColumn(row);
        if (idCol == null) {
            Toast.makeText(this, "Deletion Failed: No ID Detected", Toast.LENGTH_SHORT).show();
            return;
        }
        String idVal = row.get(idCol);
        new android.app.AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Purge record [" + idVal + "]?")
            .setPositiveButton("DELETE", (d, w) -> {
                if (db.deleteRecord(tableName, idCol, idVal)) {
                    Toast.makeText(this, "Record Purged", Toast.LENGTH_SHORT).show();
                    syncExplorer();
                }
            })
            .setNegativeButton("Abort", null).show();
    }

    private void editLocalRecord(Map<String, String> row) {
        String idCol = findIdColumn(row);
        if (idCol == null) return;
        
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int p = scale(40);
        form.setPadding(p, p, p, p);
        
        Map<String, EditText> fields = new HashMap<>();
        for (String key : row.keySet()) {
            if (key.equals("IDENTITY_RESOLVED") || key.equals(idCol)) continue;
            TextView label = new TextView(this);
            label.setText(key.toUpperCase());
            form.addView(label);
            EditText et = UiArchitect.createInputField(this, "FIELD_" + key);
            et.setText(row.get(key));
            fields.put(key, et);
            form.addView(et);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Edit Record")
            .setView(scroll)
            .setPositiveButton("UPDATE", (d, w) -> {
                ContentValues cv = new ContentValues();
                for (Map.Entry<String, EditText> entry : fields.entrySet()) {
                    cv.put(entry.getKey(), entry.getValue().getText().toString());
                }
                if (db.updateRecord(tableName, idCol, row.get(idCol), cv)) {
                    Toast.makeText(this, "Record Updated", Toast.LENGTH_SHORT).show();
                    syncExplorer();
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    private String findIdColumn(Map<String, String> row) {
        String[] possibleIds = {"log_id", "uid", "zone_id", "alert_id", "pairing_id", "sim_id", "trans_id", "sync_id", "id"};
        for (String id : possibleIds) {
            if (row.containsKey(id)) return id;
        }
        return null;
    }
}
