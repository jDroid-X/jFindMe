package com.jdroidx.jfindme.ui.management;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.*;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.AngelLoader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;

/**
 * 👑 FACTORY RESET CONSOLE (V43 SELECTIVE PURGE)
 * Objective: High-performance forensic matrix with Dual-Stack Table Purge.
 * Enhancement: Checkbox-driven selective deletion (Local + Cloud).
 */
public class FactoryResetActivity extends BaseActivityJava {

    private FactoryResetViewModel viewModel;
    private LinearLayout rootContainer, matrixBox;
    private ImageView refreshIcon;
    private StringBuilder exportDataBuilder = new StringBuilder();

    @Override
    protected View createLayout() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(getPrimaryColor());

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);
        
        TextView title = new TextView(this);
        title.setText("FACTORY RESET CONSOLE");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);
        
        TextView sub = new TextView(this);
        sub.setText("Forensic System Reset & Cloud Matrix Recovery");
        sub.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, sub, "TEXT_SUB");
        header.addView(sub);
        mainLayout.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        rootContainer = new LinearLayout(this);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        int q = getResponsivePaddingQuota();
        rootContainer.setPadding(q, q, q, q * 3);
        
        LinearLayout dbHeader = new LinearLayout(this);
        dbHeader.setOrientation(LinearLayout.HORIZONTAL);
        dbHeader.setGravity(Gravity.CENTER_VERTICAL);
        dbHeader.setPadding(0, 0, 0, scale(30));
        
        TextView tvLabel = new TextView(this);
        tvLabel.setText("DATABASE MATRIX SUMMARY");
        tvLabel.setTextColor(getGradientStart());
        UiArchitect.applyText(this, tvLabel, "TEXT_LABEL");
        dbHeader.addView(tvLabel, new LinearLayout.LayoutParams(0, -2, 1.0f));

        refreshIcon = new ImageView(this);
        refreshIcon.setImageResource(android.R.drawable.ic_menu_rotate);
        refreshIcon.setColorFilter(getGradientStart());
        int iconSize = scale(100);
        dbHeader.addView(refreshIcon, new LinearLayout.LayoutParams(iconSize, iconSize));
        refreshIcon.setOnClickListener(v -> viewModel.startCloudRecovery());
        
        rootContainer.addView(dbHeader);
        
        matrixBox = new LinearLayout(this);
        matrixBox.setOrientation(LinearLayout.VERTICAL);
        rootContainer.addView(matrixBox);

        scroll.addView(rootContainer);
        mainLayout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        // 🚀 NAVIGATION SEQUENCE: Dedicated Footer for absolute visibility
        LinearLayout staticFooter = new LinearLayout(this);
        staticFooter.setOrientation(LinearLayout.VERTICAL);
        int fPad = scale(30);
        staticFooter.setPadding(fPad, fPad, fPad, fPad);
        applyGlobalHeaderGeometry(staticFooter); // Re-use gradient style
        
        View btnHome = UiArchitect.createButton(this, "BTN_SECONDARY", v -> finish());
        ((TextView)btnHome).setText("RETURN TO HOME DASHBOARD");
        staticFooter.addView(btnHome);
        
        mainLayout.addView(staticFooter);

        return mainLayout;
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(FactoryResetViewModel.class);

        viewModel.isLoading.observe(this, loading -> {
            if (loading) AngelLoader.show(this);
            else AngelLoader.hide(this);
        });

        viewModel.syncStatus.observe(this, status -> {
            if (status == null) return;
            if (status.equals("SYNC_COMPLETE") || status.equals("SELECTIVE_PURGE_SUCCESS")) {
                if (refreshIcon != null) refreshIcon.clearAnimation();
                Toast.makeText(this, status.equals("SYNC_COMPLETE") ? "Master Matrix Synchronized." : "Selective Purge Finalized.", Toast.LENGTH_SHORT).show();
            } else if (status.startsWith("PURGE_FAILED")) {
                Toast.makeText(this, status, Toast.LENGTH_LONG).show();
            } else {
                AngelLoader.setStatus(this, status);
                if (status.contains("RECOVERY")) startRefreshRotation();
            }
        });

        viewModel.tableMatrix.observe(this, this::renderMatrix);
        
        viewModel.rebootTrigger.observe(this, reboot -> {
            if (reboot) performSystemReboot();
        });
    }

    private void renderMatrix(List<FactoryResetViewModel.TableNode> nodes) {
        matrixBox.removeAllViews();
        exportDataBuilder = new StringBuilder("=== JFINDME FORENSIC SYSTEM DUMP ===\n\n");
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        exportDataBuilder.append("TIME: ").append(db.getReadableNow()).append("\n\n");
        
        for (FactoryResetViewModel.TableNode node : nodes) {
            View card = createForensicCard(node);
            matrixBox.addView(card);
            exportDataBuilder.append("TABLE: ").append(node.name).append(" | ROWS: ").append(node.count).append("\n");
        }
        renderActionFooter(nodes);
    }

    private View createForensicCard(FactoryResetViewModel.TableNode node) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, card, "CARD_STANDARD");
        int p = scale(40);
        card.setPadding(p, p, p, p);
        
        TextView tvTitle = new TextView(this);
        tvTitle.setText(node.name.toUpperCase());
        tvTitle.setTextColor(getTextColor());
        tvTitle.setTypeface(null, Typeface.BOLD);
        UiArchitect.applyText(this, tvTitle, "TEXT_TITLE");
        card.addView(tvTitle);
        
        TextView tvStats = new TextView(this);
        tvStats.setText("Forensic Integrity: 100% | Rows Detected: " + node.count);
        tvStats.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, tvStats, "TEXT_MINI");
        card.addView(tvStats);

        if (node.count > 0) {
            HorizontalScrollView hScroll = new HorizontalScrollView(this);
            TableLayout table = new TableLayout(this);
            
            // 🚀 QA Fix: Master Table Header Restoration
            if (!node.sample.isEmpty()) {
                TableRow header = new TableRow(this);
                header.setBackgroundColor(Color.parseColor("#15000000")); // Sharp tacticle tint
                for (String key : node.sample.get(0).keySet()) {
                    TextView tv = new TextView(this);
                    tv.setText(key.toUpperCase());
                    tv.setPadding(scale(20), scale(15), scale(20), scale(15));
                    tv.setTextColor(getGradientStart());
                    tv.setTypeface(null, Typeface.BOLD);
                    UiArchitect.applyText(this, tv, "TEXT_MINI");
                    header.addView(tv);
                }
                table.addView(header);
            }

            for (Map<String, String> row : node.sample) {
                TableRow tr = new TableRow(this);
                for (String val : row.values()) {
                    TextView tv = new TextView(this);
                    tv.setText(val != null ? val : "---");
                    tv.setPadding(scale(20), scale(10), scale(20), scale(10));
                    tv.setTextColor(getSecondaryTextColor());
                    UiArchitect.applyText(this, tv, "TEXT_BODY");
                    tr.addView(tv);
                }
                table.addView(tr);
            }
            hScroll.addView(table);
            card.addView(hScroll);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, scale(30));
        card.setLayoutParams(lp);
        return card;
    }

    private void renderActionFooter(List<FactoryResetViewModel.TableNode> nodes) {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(0, scale(40), 0, 0);

        addAccessibilityButton(footer, "SAVE_TO_LOCAL_TXT", v -> saveToLocalText());
        addAccessibilityButton(footer, "DATABASE_EXPLORER_V37", v -> startActivity(new android.content.Intent(this, DatabaseManagementActivity.class)));
        addAccessibilityButton(footer, "FIREBASE_CLOUD_MATRIX", v -> startActivity(new android.content.Intent(this, FirebaseCloudMatrixActivity.class)));
        addAccessibilityButton(footer, "SELECTIVE_SYSTEM_PURGE", v -> showSelectivePurgeDialog(nodes));
        addAccessibilityButton(footer, "VIOLENT_MASTER_PURGE", v -> showMasterPurgeDialog());
        
        // 🚀 AUDIT FIX: Buttons must be added to matrixBox so they are cleared during re-renders!
        matrixBox.addView(footer);
    }

    private void showSelectivePurgeDialog(List<FactoryResetViewModel.TableNode> nodes) {
        String[] tableNames = new String[nodes.size()];
        boolean[] selected = new boolean[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) tableNames[i] = nodes.get(i).name;

        new AlertDialog.Builder(this)
            .setTitle("SELECT DUAL-STACK PURGE TARGETS")
            .setMultiChoiceItems(tableNames, selected, (dialog, which, isChecked) -> selected[which] = isChecked)
            .setPositiveButton("WIPE SELECTED (LOCAL+CLOUD)", (dialog, which) -> {
                List<String> toDelete = new ArrayList<>();
                for (int i = 0; i < tableNames.length; i++) if (selected[i]) toDelete.add(tableNames[i]);
                if (!toDelete.isEmpty()) viewModel.executeSelectivePurge(toDelete);
                else Toast.makeText(this, "No nodes selected for purge.", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("ABORT", null)
            .show();
    }

    private void showMasterPurgeDialog() {
        new AlertDialog.Builder(this)
            .setTitle("VIOLENT MASTER PURGE?")
            .setMessage("Irrevocably delete all database tables and assets? This will purge both Local and Cloud copies.")
            .setPositiveButton("WIPE EVERYTHING", (d, w) -> viewModel.executeMasterPurge())
            .setNegativeButton("Abort", null).show();
    }

    private void addAccessibilityButton(LinearLayout parent, String rId, View.OnClickListener listener) {
        View btn = UiArchitect.createButton(this, rId, listener);
        btn.setOnKeyListener((v, code, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN && code == android.view.KeyEvent.KEYCODE_ENTER) {
                listener.onClick(v);
                return true;
            }
            return false;
        });
        parent.addView(btn);
    }

    private void startRefreshRotation() {
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1200);
        rotate.setRepeatCount(Animation.INFINITE);
        if (refreshIcon != null) refreshIcon.startAnimation(rotate);
    }

    private void performSystemReboot() {
        android.content.Intent restart = new android.content.Intent(this, com.jdroidx.jfindme.ui.base.SplashActivity.class);
        restart.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(restart);
        finish();
    }

    private void saveToLocalText() {
        try {
            File docs = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS);
            File dump = new File(docs, "JFINDME_DUMP_" + System.currentTimeMillis() + ".txt");
            java.io.FileWriter writer = new java.io.FileWriter(dump);
            writer.write(exportDataBuilder.toString());
            writer.close();
            Toast.makeText(this, "Forensic Dump Saved.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
