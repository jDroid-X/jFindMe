package com.jdroidx.jfindme.ui.management;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;
import java.util.*;

import android.content.ContentValues;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jdroidx.jfindme.utils.AngelLoader;

/**
 * 👑 FORENSIC STRUCTURE ANALYZER (L7 Global UI Architect Sync)
 * Objective: Transposed detail view with Command Hub (Edit, Delete, CloudSync).
 * Features: Horizontal Scroll, Compact Vertical Rows, Atomic Local-to-Cloud Sync.
 */
public class ForensicStructureActivity extends BaseActivityJava {

    private String tableName, tableLabel;
    private DatabaseHelper db;
    private LinearLayout contentMatrix;
    private int currentPage = 0;
    private final int PAGE_SIZE = 3;

    @Override
    protected View createLayout() {
        tableName = getIntent().getStringExtra("TABLE_NAME");
        tableLabel = getIntent().getStringExtra("TABLE_LABEL");
        db = DatabaseHelper.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        // 🚀 L7 Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);
        
        TextView title = new TextView(this);
        title.setText("ANALYTICAL SCHEMA HUB");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);
        
        TextView sub = new TextView(this);
        sub.setText(tableLabel + " | Master Node Structure");
        sub.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, sub, "TEXT_SUB");
        header.addView(sub);
        root.addView(header);

        // 🚀 Horizontal & Vertical Matrix
        ScrollView vScroll = new ScrollView(this);
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        contentMatrix = new LinearLayout(this);
        contentMatrix.setOrientation(LinearLayout.VERTICAL);
        int p = getResponsivePaddingQuota();
        contentMatrix.setPadding(p, p, p, p * 3);

        hScroll.addView(contentMatrix);
        vScroll.addView(hScroll);
        root.addView(vScroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        syncView();

        return root;
    }

    private void syncView() {
        contentMatrix.removeAllViews();
        // Fetch up to 100 records for the matrix pool
        List<Map<String, String>> data = db.getPagedData(tableName, 100, 0, null);
        List<String> columns = db.getTableColumns(tableName);

        if (columns.isEmpty()) {
            renderSectionHeader("CRITICAL: Schema Discovery Failed.");
        } else if (data.isEmpty()) {
            renderSectionHeader("NO FORENSIC RECORDS DETECTED IN [" + tableName + "]");
        } else {
            renderTransposedMatrix(contentMatrix, columns, data);
        }
    }

    private void renderSectionHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(getTextColor());
        tv.setPadding(0, scale(40), 0, scale(20));
        UiArchitect.applyText(this, tv, "TEXT_SUB");
        contentMatrix.addView(tv);
    }

    private void renderTransposedMatrix(LinearLayout container, List<String> cols, List<Map<String, String>> allData) {
        // 🚀 Analytical Command Hub (L7 Global Activity Hub)
        LinearLayout hub = new LinearLayout(this);
        hub.setOrientation(LinearLayout.HORIZONTAL);
        hub.setGravity(Gravity.CENTER_VERTICAL);
        hub.setPadding(0, 0, 0, scale(35));
        
        // Navigation & Retrieval Bar
        if (currentPage > 0) {
            hub.addView(createHubIcon(android.R.drawable.ic_media_previous, getGradientStart(), v -> {
                currentPage--;
                syncView();
            }));
        }

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allData.size());
        Map<String, String> primaryNode = allData.get(start);

        // 🚀 Restored Primary Node Action Bar
        hub.addView(createHubIcon(android.R.drawable.ic_menu_edit, getGradientStart(), v -> editRecord(primaryNode)));
        hub.addView(createHubIcon(android.R.drawable.ic_menu_delete, Color.RED, v -> deleteRecord(primaryNode)));
        hub.addView(createHubIcon(android.R.drawable.ic_popup_sync, Color.parseColor("#43A047"), v -> syncToCloud(primaryNode)));
        hub.addView(createHubIcon(android.R.drawable.ic_menu_rotate, getGradientStart(), v -> syncView()));

        if (end < allData.size()) {
            hub.addView(createHubIcon(android.R.drawable.ic_media_next, getGradientStart(), v -> {
                currentPage++;
                syncView();
            }));
        }

        TextView pageInfo = new TextView(this);
        pageInfo.setText("NODES " + (start+1) + "-" + end + " OF " + allData.size());
        pageInfo.setTextColor(getTextColor());
        pageInfo.setPadding(scale(30), 0, 0, 0);
        hub.addView(pageInfo);

        container.addView(hub);

        TableLayout table = new TableLayout(this);
        UiArchitect.applyCardWithOverride(this, table, "CARD_STANDARD");
        
        // Dynamic Header Row
        TableRow head = new TableRow(this);
        head.setBackgroundColor(getGradientStart());
        head.addView(createCell("FIELD ATTRIBUTE", true));
        for (int i = start; i < end; i++) {
            head.addView(createCell("NODE " + (i + 1), true));
        }
        table.addView(head);

        // 📚 Compact Data Rows
        int idx = 0;
        for (String col : cols) {
            TableRow row = new TableRow(this);
            row.setBackgroundColor(idx % 2 == 0 ? Color.argb(10, 0, 0, 0) : Color.TRANSPARENT);
            
            // 🚀 Attribute Cell with Q Icon (Prefix Mode)
            LinearLayout attrLayout = new LinearLayout(this);
            attrLayout.setOrientation(LinearLayout.HORIZONTAL);
            attrLayout.setGravity(Gravity.CENTER_VERTICAL);
            attrLayout.setPadding(scale(50), scale(20), scale(50), scale(20));

            ImageView qIcon = new ImageView(this);
            qIcon.setImageResource(android.R.drawable.ic_menu_help);
            qIcon.setColorFilter(getGradientEnd());
            int qs = scale(45);
            LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(qs, qs);
            qLp.setMargins(0, 0, scale(15), 0);
            qIcon.setLayoutParams(qLp);
            qIcon.setOnClickListener(v -> showQueryOracle(col));
            attrLayout.addView(qIcon);

            TextView tvKey = new TextView(this);
            tvKey.setText(col.toUpperCase());
            tvKey.setTypeface(null, Typeface.BOLD);
            tvKey.setTextColor(getGradientStart());
            UiArchitect.applyText(this, tvKey, "TEXT_BODY");
            attrLayout.addView(tvKey);

            row.addView(attrLayout);

            for (int j = start; j < end; j++) {
                final Map<String, String> nodeData = allData.get(j);
                String val = nodeData.containsKey(col) ? nodeData.get(col) : "---";
                TextView tvVal = createCell(val != null ? val : "---", false);
                tvVal.setTextColor(getTextColor());
                tvVal.setOnClickListener(v -> actionHub(nodeData));
                row.addView(tvVal);
            }
            
            table.addView(row);
            idx++;
        }

        container.addView(table);
    }

    private void showQueryOracle(String col) {
        String source = "Local SQLite Engine (CitizenData.db)";
        String query = "SELECT " + col + " FROM " + tableName + " ORDER BY rowid DESC";
        
        // 🧪 Data Lineage Context
        String lineage = "Data is captured via background forensic sensors and archived during atomic commit cycles.";
        if (col.contains("phone")) lineage = "Source: System Telephony Node / User Input";
        else if (col.contains("lat") || col.contains("lng")) lineage = "Source: GPS Satellite Constellation / Network Node";
        else if (tableName.contains("SafeZone")) lineage = "Source: Geofence Spatial Engine";
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("Q-Oracle: Data Lineage")
            .setMessage("ATTRIBUTE: " + col.toUpperCase() + "\n\n" +
                       "SOURCE: " + source + "\n\n" +
                       "CONTEXT: " + lineage + "\n\n" +
                       "QUERY: " + query)
            .setPositiveButton("RESOLVED", null).show();
    }

    private void actionHub(Map<String, String> data) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Node Command Console")
            .setItems(new String[]{"Edit Node", "Delete Node", "Sync to Cloud"}, (d, w) -> {
                if (w == 0) editRecord(data);
                else if (w == 1) deleteRecord(data);
                else if (w == 2) syncToCloud(data);
            }).show();
    }

    private ImageView createHubIcon(int res, int color, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(res);
        iv.setColorFilter(color);
        int s = scale(110);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.setMargins(0, 0, scale(25), 0);
        iv.setLayoutParams(lp);
        iv.setPadding(scale(20), scale(20), scale(20), scale(20));
        iv.setBackgroundColor(Color.argb(20, 0, 0, 0));
        iv.setOnClickListener(listener);
        return iv;
    }

    private TextView createCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        // 🚀 Compact Vertical Padding
        int py = isHeader ? scale(35) : scale(20);
        tv.setPadding(scale(50), py, scale(50), py);
        if (isHeader) {
            tv.setTextColor(getGradientOverlayText());
            UiArchitect.applyText(this, tv, "TEXT_LABEL");
        } else {
            UiArchitect.applyText(this, tv, "TEXT_BODY");
        }
        return tv;
    }

    private void editRecord(Map<String, String> data) {
        String idCol = findIdColumn(data);
        if (idCol == null) return;
        
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int p = scale(40);
        form.setPadding(p, p, p, p);
        
        Map<String, EditText> fields = new HashMap<>();
        for (String key : data.keySet()) {
            if (key.equals("IDENTITY_RESOLVED") || key.equals(idCol)) continue;
            TextView label = new TextView(this);
            label.setText(key.toUpperCase());
            form.addView(label);
            
            EditText et = UiArchitect.createInputField(this, "EDIT_" + key);
            et.setText(data.get(key));
            fields.put(key, et);
            form.addView(et);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Forensic Update: " + data.get(idCol))
            .setView(scroll)
            .setPositiveButton("COMMIT LOCAL", (d, w) -> {
                ContentValues cv = new ContentValues();
                for (Map.Entry<String, EditText> entry : fields.entrySet()) {
                    cv.put(entry.getKey(), entry.getValue().getText().toString());
                }
                if (db.updateRecord(tableName, idCol, data.get(idCol), cv)) {
                    Toast.makeText(this, "Local Registry Updated.", Toast.LENGTH_SHORT).show();
                    syncView();
                }
            })
            .setNegativeButton("Abort", null).show();
    }

    private void deleteRecord(Map<String, String> data) {
        String idCol = findIdColumn(data);
        if (idCol == null) return;
        String idVal = data.get(idCol);
        new android.app.AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Irrevocably purge record [" + idVal + "]?")
            .setPositiveButton("DELETE", (d, w) -> {
                if (db.deleteRecord(tableName, idCol, idVal)) {
                    Toast.makeText(this, "Record Purged.", Toast.LENGTH_SHORT).show();
                    syncView();
                }
            })
            .setNegativeButton("Abort", null).show();
    }

    private void syncToCloud(Map<String, String> data) {
        String idCol = findIdColumn(data);
        if (idCol == null) return;
        String idVal = data.get(idCol);
        
        AngelLoader.show(this);
        AngelLoader.setStatus(this, "Pushing Forensic Node to Cloud...");
        
        DatabaseReference cloudRef = FirebaseDatabase.getInstance().getReference().child(tableName).child(idVal);
        cloudRef.setValue(data).addOnCompleteListener(task -> {
            AngelLoader.hide(this);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Cloud Hub Synchronized.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Sync Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String findIdColumn(Map<String, String> row) {
        if (row.containsKey("log_id")) return "log_id";
        if (row.containsKey("uid")) return "uid";
        if (row.containsKey("zone_id")) return "zone_id";
        if (row.containsKey("alert_id")) return "alert_id";
        if (row.containsKey("pairing_id")) return "pairing_id";
        if (row.containsKey("sim_id")) return "sim_id";
        return null;
    }

    @Override
    protected void setupLogic() {}
}
