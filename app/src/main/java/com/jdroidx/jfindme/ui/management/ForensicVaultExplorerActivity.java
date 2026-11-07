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
    private DatabaseReference activeTarget;
    
    private LinearLayout contentMatrix;
    private TextView tvTableMeta;
    private boolean isGridView = true; // 🚀 Toggle: GRID (Spreadsheet) vs LIST (Transposed)
    private int listPageOffset = 0;

    @Override
    protected View createLayout() {
        tableName = getIntent().getStringExtra("TABLE_NAME");
        if (tableName == null || tableName.trim().isEmpty()) {
            tableName = DatabaseHelper.PROFILE_TABLE;
        }
        tableLabel = getIntent().getStringExtra("TABLE_LABEL");
        if (tableLabel == null || tableLabel.trim().isEmpty()) {
            tableLabel = tableName.replace("_", " ").toUpperCase();
        }
        db = DatabaseHelper.getInstance(this);
        cloudRef = FirebaseDatabase.getInstance().getReference().child(tableName);

        // 🚀 NAVIGATION OVERRIDE: Check for requested View Mode
        String modeStr = getIntent().getStringExtra("VIEW_MODE");
        if ("LIST".equals(modeStr)) isGridView = false;

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
        View btnMode = UiArchitect.createButton(this, "TOGGLE_MODE", v -> {
            isGridView = !isGridView;
            renderSnapshot(lastSnapshot);
        });
        
        LinearLayout.LayoutParams toolLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        toolbar.addView(btnRefresh, toolLp);
        toolbar.addView(btnMode, toolLp);
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
        // 🚀 ADAPTIVE ARCHITECT: Only force landscape if in Grid Mode
        if (isGridView) setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private com.google.firebase.database.DataSnapshot lastSnapshot;

    @Override
    protected void setupLogic() {
        syncExplorer();
    }

    private void syncExplorer() {
        if (!com.jdroidx.jfindme.data.remote.FirebaseComm.isAuthenticated()) {
            AngelLoader.hide(this);
            lastSnapshot = null;
            renderSnapshot(null);
            Toast.makeText(this, "Offline Vault: Showing Local SQLite Data", Toast.LENGTH_SHORT).show();
            return;
        }
        AngelLoader.show(this);
        AngelLoader.setStatus(this, "SYNCHRONIZING FORENSIC MATRIX...");
        
        // [Variables]: myUid, currentAgentId, activeGroup, lowerTable, activeTarget
        // [Constants]: relationships, safe_zones, deepHistoryRef
        // [Dependencies]: RegistryManager, FirebaseComm, FirebaseDatabase
        // [Integration Points]: Master Cloud Analytical Node Resolver (Schema V81.2 SSOT)
        String myUid = com.jdroidx.jfindme.utils.RegistryManager.getAgentId(this);
        if (myUid == null || myUid.isEmpty()) {
            myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        }
        String currentAgentId = (myUid != null && !myUid.isEmpty()) ? myUid : "PRIMARY_NODE";
        String activeGroup = com.jdroidx.jfindme.utils.RegistryManager.getActiveGroup(this);
        if (activeGroup == null || activeGroup.trim().isEmpty()) {
            activeGroup = "GRP_" + currentAgentId;
        }
        
        // 5-Tier Analytical Node Mapping (Normalized Case-Insensitive Matching)
        String lowerTable = (tableName != null) ? tableName.toLowerCase() : "";
        if (lowerTable.contains("profile")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(currentAgentId);
        } else if (lowerTable.contains("identity")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(currentAgentId);
        } else if (lowerTable.contains("vitals")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getTelemetryRef(currentAgentId);
        } else if (lowerTable.contains("device") && !lowerTable.contains("pairing")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(currentAgentId);
        } else if (lowerTable.contains("roster") || lowerTable.contains("group") || lowerTable.contains("relationship") || lowerTable.contains("member")) {
            activeTarget = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("relationships").child(activeGroup).child("members");
        } else if (lowerTable.contains("safezone") || lowerTable.contains("geofence") || lowerTable.contains("safe_zone")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getGeofencingSafeZonesRef(currentAgentId);
        } else if (lowerTable.contains("history") || lowerTable.contains("location")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getDeepHistoryRef(currentAgentId);
        } else if (lowerTable.contains("security") || lowerTable.contains("key")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getIdentityRef(currentAgentId).child("security");
        } else if (lowerTable.contains("telemetry") || lowerTable.contains("failover") || lowerTable.contains("route")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(currentAgentId).child("telemetry_routes");
        } else if (lowerTable.contains("mesh")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(currentAgentId).child("mesh_packets");
        } else if (lowerTable.contains("invitation")) {
            activeTarget = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("relationships").child(activeGroup).child("invitations");
        } else if (lowerTable.contains("sim_transaction") || lowerTable.contains("sim")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getDeviceRef(currentAgentId).child("sim_transactions");
        } else if (lowerTable.contains("pairing")) {
            activeTarget = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("relationships").child(activeGroup).child("devices");
        } else if (lowerTable.contains("log") || lowerTable.contains("audit") || lowerTable.contains("alert")) {
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getAlertsRef(currentAgentId);
        } else {
            // Fallback for custom or root nodes
            activeTarget = com.jdroidx.jfindme.data.remote.FirebaseComm.getDevicesRef();
        }
        
        activeTarget.limitToLast(20).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AngelLoader.hide(ForensicVaultExplorerActivity.this);
                lastSnapshot = snapshot;
                renderSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AngelLoader.hide(ForensicVaultExplorerActivity.this);
                lastSnapshot = null;
                renderSnapshot(null);
                Toast.makeText(ForensicVaultExplorerActivity.this, "Cloud Sync Offline", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSnapshot(DataSnapshot cloudSnapshot) {
        contentMatrix.removeAllViews();
        List<String> columns = db.getTableColumns(tableName);
        List<Map<String, String>> localRows = db.getPagedData(tableName, 30, 0, null);
        
        tvTableMeta.setText("VAULT: [" + tableName + "] | MODE: " + (isGridView ? "GRID" : "LIST") + " | LOCAL: " + localRows.size());

        if (isGridView) {
            renderSectionHeader("OFFLINE LEDGER (GRID)");
            renderTable(localRows, columns, true);
            if (cloudSnapshot != null) {
                renderSectionHeader("ONLINE REGISTRY (FIREBASE)");
                // [Variables]: cloudRows, ds, row, v, m, e
                // [Constants]: None
                // [Dependencies]: java.util.Collections, DataSnapshot
                // [Integration Points]: Master Temporal Invariant (Descending Latest First)
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
                // Reverse to display newest records first (DESC) matching offline ledger
                Collections.reverse(cloudRows);
                renderTable(cloudRows, columns, false);
            }
        } else {
            renderSectionHeader("FORENSIC STRUCTURE (TRANSPOSED)");
            renderTransposedMatrix(localRows, columns);
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

    private void renderTransposedMatrix(List<Map<String, String>> allData, List<String> columns) {
        if (allData == null || allData.isEmpty()) {
            View emptyCard = UiArchitect.createEmptyStateCard(
                this,
                "No Records to Inspect",
                "This database table currently contains 0 records.",
                android.R.drawable.ic_menu_info_details,
                "REFRESH MATRIX",
                () -> syncExplorer()
            );
            contentMatrix.addView(emptyCard);
            return;
        }
        
        // 🚀 Analytical Command Hub (List Page Controller)
        LinearLayout hub = new LinearLayout(this);
        hub.setOrientation(LinearLayout.HORIZONTAL);
        hub.setGravity(Gravity.CENTER_VERTICAL);
        hub.setPadding(0, 0, 0, scale(35));
        
        if (listPageOffset > 0) {
            hub.addView(createSmallIcon(android.R.drawable.ic_media_previous, getGradientStart(), v -> {
                listPageOffset--;
                renderSnapshot(lastSnapshot);
            }));
        }

        int max = Math.min(listPageOffset + 1, allData.size()); // Render 1 node in deep list mode
        Map<String, String> data = allData.get(listPageOffset);

        hub.addView(createSmallIcon(com.jdroidx.jfindme.R.drawable.ic_edit, getGradientStart(), v -> editLocalRecord(data)));
        hub.addView(createSmallIcon(android.R.drawable.ic_menu_delete, Color.RED, v -> deleteLocalRecord(data)));
        hub.addView(createSmallIcon(android.R.drawable.ic_popup_sync, Color.parseColor("#43A047"), v -> syncToCloud(data)));

        if (listPageOffset + 1 < allData.size()) {
            hub.addView(createSmallIcon(android.R.drawable.ic_media_next, getGradientStart(), v -> {
                listPageOffset++;
                renderSnapshot(lastSnapshot);
            }));
        }

        TextView pageInfo = new TextView(this);
        pageInfo.setText("INSPECTING NODE " + (listPageOffset + 1) + " OF " + allData.size());
        pageInfo.setTextColor(getTextColor());
        pageInfo.setPadding(scale(30), 0, 0, 0);
        hub.addView(pageInfo);
        contentMatrix.addView(hub);

        TableLayout table = new TableLayout(this);
        UiArchitect.applyCard(this, table, "CARD_STANDARD");

        for (String col : columns) {
            TableRow row = new TableRow(this);
            row.addView(createCell(col.toUpperCase(), true));
            String val = data.get(col);
            row.addView(createCell(val != null ? val : "---", false));
            table.addView(row);
        }
        contentMatrix.addView(table);
    }

    private ImageView createSmallIcon(int res, int color, View.OnClickListener listener) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(res);
        iv.setColorFilter(color);
        int s = scale(100);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.setMargins(0, 0, scale(20), 0);
        iv.setLayoutParams(lp);
        iv.setBackgroundColor(Color.argb(20, 0, 0, 0));
        iv.setOnClickListener(listener);
        return iv;
    }

    private void syncToCloud(Map<String, String> data) {
        String idCol = findIdColumn(data);
        if (idCol == null) return;
        String idVal = data.get(idCol);
        
        AngelLoader.show(this);
        AngelLoader.setStatus(this, "Mirroring Node to Cloud...");
        DatabaseReference targetRef = activeTarget != null ? activeTarget : cloudRef;
        targetRef.child(idVal).setValue(data).addOnCompleteListener(t -> {
            AngelLoader.hide(this);
            if (t.isSuccessful()) Toast.makeText(this, "Cloud Sync Completed", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Sync Blocked: " + t.getException().getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void renderTable(List<Map<String, String>> rows, List<String> columns, boolean isLocal) {
        if (rows == null || rows.isEmpty()) {
            View emptyCard = UiArchitect.createEmptyStateCard(
                this,
                "No Records Found",
                isLocal ? "This local table currently has 0 rows recorded." : "No matching records found in Firebase Cloud node.",
                android.R.drawable.ic_menu_info_details,
                "REFRESH DATA",
                () -> syncExplorer()
            );
            contentMatrix.addView(emptyCard);
            return;
        }
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
                edit.setImageResource(com.jdroidx.jfindme.R.drawable.ic_edit);
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
        // 🚀 V54 Forensic Trust Pair: uid_1 is the primary account key
        String[] possibleIds = {"log_id", "zone_id", "alert_id", "pairing_id", "sim_id",
                "trans_id", "sync_id", "member_log_id", "uid_1", "id"};
        for (String id : possibleIds) {
            if (row.containsKey(id)) return id;
        }
        return null;
    }
}
