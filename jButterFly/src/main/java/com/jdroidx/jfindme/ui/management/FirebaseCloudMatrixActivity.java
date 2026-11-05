package com.jdroidx.jfindme.ui.management;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.AngelLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 🛰️ FORENSIC WORLD EXPLORER (OOM-Hardened V51)
 * Objective: High-fidelity spreadsheet with Sequence-Correct Back Navigation.
 * Enhancement: Universal Back-Hook to Forensic Console.
 */
public class FirebaseCloudMatrixActivity extends BaseActivityJava {

    private LinearLayout contentMatrix;
    private DatabaseReference rootRef;
    
    private static final String[] MASTER_WHITELIST = {
        "profile_table", "roster_table", "org_table", "SafeZone_Table", 
        "GeoFence_Alert_table", "history_table", "pairing_log", 
        "sim_inventory", "sim_transaction", "geofenc_sync_queue_table", 
        "SafeZoneGroup_Table", "GeofenceLog_Table", "device_table"
    };

    @Override
    protected View createLayout() {
        rootRef = FirebaseDatabase.getInstance().getReference();
        
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(getPrimaryColor());
        mainLayout.setClipChildren(false);
        mainLayout.setClipToPadding(false);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        applyGlobalHeaderGeometry(header);
        
        // 🚀 NAVIGATION FIX: Back Button Integration
        ImageView backBtn = new ImageView(this);
        backBtn.setImageResource(android.R.drawable.ic_menu_revert); 
        backBtn.setColorFilter(getGradientOverlayText());
        int bSize = scale(120);
        backBtn.setPadding(scale(20), scale(20), scale(20), scale(20));
        backBtn.setOnClickListener(v -> finish());
        header.addView(backBtn, new LinearLayout.LayoutParams(bSize, bSize));

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(scale(30), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("FORENSIC WORLD EXPLORER");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        textLayout.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Cloud Intelligence Matrix | Verified Node Registry");
        subtitle.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        textLayout.addView(subtitle);
        
        header.addView(textLayout, new LinearLayout.LayoutParams(0, -2, 1.0f));
        mainLayout.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        
        contentMatrix = new LinearLayout(this);
        contentMatrix.setOrientation(LinearLayout.VERTICAL);
        contentMatrix.setClipChildren(false);
        contentMatrix.setClipToPadding(false);
        int q = getResponsivePaddingQuota();
        contentMatrix.setPadding(q, q, q, q * 5);
        
        scroll.addView(contentMatrix);
        mainLayout.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainLayout;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    protected void setupLogic() {
        fetchCloudTables();
    }

    private void fetchCloudTables() {
        contentMatrix.removeAllViews();

        TextView label = new TextView(this);
        label.setText("GLOBAL CLOUD ASSET REGISTRY:");
        label.setTextColor(getGradientStart());
        UiArchitect.applyText(this, label, "TEXT_LABEL");
        label.setPadding(0, 0, 0, scale(20));
        contentMatrix.addView(label);

        for (String tableName : MASTER_WHITELIST) {
            contentMatrix.addView(createTableTile(tableName));
        }
    }

    private View createTableTile(String name) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        
        UiArchitect.applyCardWithOverride(this, tile, "CARD_STANDARD", 1.3f);
        tile.setClipToOutline(false); 
        
        int p = scale(40);
        tile.setPadding(p, p, p, p);
        
        TextView tvName = new TextView(this);
        tvName.setText("🌐 " + name.toUpperCase().replace("_TABLE", ""));
        tvName.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvName, "TEXT_TITLE");
        tile.addView(tvName, new LinearLayout.LayoutParams(0, -2, 1.0f));
        
        TextView tvAction = new TextView(this);
        tvAction.setText("EXTRACT MATRIX ▶");
        tvAction.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvAction, "TEXT_MINI");
        tile.addView(tvAction);
        
        tile.setOnClickListener(v -> fetchTableData(name));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, scale(30)); 
        tile.setLayoutParams(lp);
        return tile;
    }

    private void fetchTableData(String tablePath) {
        AngelLoader.show(this);
        AngelLoader.setStatus(this, "INFILTRATING " + tablePath.toUpperCase() + "...");
        
        rootRef.child(tablePath).limitToLast(50).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AngelLoader.hide(FirebaseCloudMatrixActivity.this);
                if (snapshot.exists()) {
                    renderSnapshotAsExcel(tablePath, snapshot);
                } else {
                    Toast.makeText(FirebaseCloudMatrixActivity.this, "Handshake Terminated: Node Offline.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AngelLoader.hide(FirebaseCloudMatrixActivity.this);
                Toast.makeText(FirebaseCloudMatrixActivity.this, "Access Denied: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSnapshotAsExcel(String title, DataSnapshot snapshot) {
        contentMatrix.removeAllViews();
        
        View backBtn = UiArchitect.createButton(this, "BTN_SECONDARY", v -> fetchCloudTables());
        ((TextView)backBtn).setText("◀ RETURN TO ASSET REGISTRY");
        contentMatrix.addView(backBtn);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("NODE EXPLORER: " + title.toUpperCase());
        tvTitle.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvTitle, "TEXT_LABEL");
        tvTitle.setPadding(0, scale(40), 0, scale(20));
        contentMatrix.addView(tvTitle);

        Set<String> columnSet = new LinkedHashSet<>();
        columnSet.add("ATTR_NODE_ID");
        for (DataSnapshot ds : snapshot.getChildren()) {
            if (ds.getValue() instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) ds.getValue();
                for (Object key : map.keySet()) columnSet.add(key.toString());
            }
        }
        if (columnSet.size() == 1) columnSet.add("VALUE");
        List<String> columns = new ArrayList<>(columnSet);

        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(true);
        
        TableRow hRow = new TableRow(this);
        hRow.setBackgroundColor(getGradientStart());
        for (String col : columns) {
            hRow.addView(createCell(col.toUpperCase(), true));
        }
        table.addView(hRow);

        for (DataSnapshot ds : snapshot.getChildren()) {
            TableRow dRow = new TableRow(this);
            dRow.addView(createCell(ds.getKey(), false));

            Object valObj = ds.getValue();
            if (valObj instanceof Map) {
                Map<?, ?> valMap = (Map<?, ?>) valObj;
                for (int i = 1; i < columns.size(); i++) {
                    String key = columns.get(i);
                    Object v = valMap.get(key);
                    dRow.addView(createCell(v != null ? String.valueOf(v) : "---", false));
                }
            } else {
                dRow.addView(createCell(String.valueOf(valObj), false));
            }
            table.addView(dRow);
        }

        hScroll.addView(table);
        contentMatrix.addView(hScroll);
    }

    private TextView createCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(scale(25), scale(20), scale(25), scale(20));
        tv.setSingleLine(true);
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(isHeader ? getGradientStart() : Color.TRANSPARENT);
        gd.setStroke(1, Color.GRAY); 
        tv.setBackground(gd);
        
        if (isHeader) {
            tv.setTextColor(getGradientOverlayText());
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextSize(10f);
        } else {
            tv.setTextColor(getSecondaryTextColor()); 
            tv.setTextSize(12f);
        }
        return tv;
    }
}
