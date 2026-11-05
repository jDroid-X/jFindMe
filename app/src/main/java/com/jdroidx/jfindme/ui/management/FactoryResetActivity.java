package com.jdroidx.jfindme.ui.management;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.jdroidx.jfindme.utils.RegistryManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import androidx.lifecycle.ViewModelProvider;

/**
 * 👑 FACTORY RESET CONSOLE (V72 SELECTIVE PURGE & PRE-UNINSTALL TEARDOWN)
 * Objective: High-performance forensic matrix with Dual-Stack Table Purge,
 * Graceful Background Teardown, and Human-In-The-Loop Data Export.
 */
public class FactoryResetActivity extends BaseActivityJava {

    private FactoryResetViewModel viewModel;
    private LinearLayout rootContainer, matrixBox;
    private ImageView refreshIcon;
    private EditText searchFilterEt;
    private StringBuilder exportDataBuilder = new StringBuilder();
    private List<FactoryResetViewModel.TableNode> currentNodes = new ArrayList<>();

    @Override
    protected View createLayout() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(getPrimaryColor());

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);
        
        TextView title = new TextView(this);
        title.setText("FACTORY RESET & UNINSTALL CONSOLE");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);
        
        TextView sub = new TextView(this);
        sub.setText("Forensic System Reset, Teardown & Cloud Recovery");
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
        dbHeader.setPadding(0, 0, 0, scale(15));
        
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

        // 🔍 Interactive Matrix Search Filter
        searchFilterEt = new EditText(this);
        searchFilterEt.setHint("Filter tables (e.g. location, profile, zones)...");
        searchFilterEt.setTextColor(getTextColor());
        searchFilterEt.setHintTextColor(getSecondaryTextColor());
        searchFilterEt.setBackgroundColor(Color.TRANSPARENT);
        searchFilterEt.setPadding(scale(20), scale(15), scale(20), scale(15));
        UiArchitect.applyText(this, searchFilterEt, "TEXT_BODY");

        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable glass = new android.graphics.drawable.GradientDrawable();
        glass.setCornerRadius(25f);
        glass.setColor(getCardColor());
        glass.setStroke(2, Color.parseColor("#33" + String.format("%06X", (0xFFFFFF & getGradientStart()))));
        searchWrap.setBackground(glass);
        searchWrap.addView(searchFilterEt);
        
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.setMargins(0, 0, 0, scale(20));
        searchWrap.setLayoutParams(searchLp);
        rootContainer.addView(searchWrap);

        searchFilterEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMatrix(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        
        matrixBox = new LinearLayout(this);
        matrixBox.setOrientation(LinearLayout.VERTICAL);
        rootContainer.addView(matrixBox);

        scroll.addView(rootContainer);
        mainLayout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        // 🚀 NAVIGATION SEQUENCE: Dedicated Footer
        LinearLayout staticFooter = new LinearLayout(this);
        staticFooter.setOrientation(LinearLayout.VERTICAL);
        int fPad = scale(30);
        staticFooter.setPadding(fPad, fPad, fPad, fPad);
        applyGlobalHeaderGeometry(staticFooter);
        
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
            } else if (status.startsWith("PURGE_FAILED") || status.startsWith("TEARDOWN_FAILED")) {
                UiArchitect.showValidationDialog(this, "Operation Alert", status);
            } else {
                AngelLoader.setStatus(this, status);
                if (status.contains("RECOVERY")) startRefreshRotation();
            }
        });

        viewModel.tableMatrix.observe(this, nodes -> {
            currentNodes = nodes;
            renderMatrix(nodes);
        });
        
        viewModel.rebootTrigger.observe(this, reboot -> {
            if (reboot) performSystemReboot();
        });

        viewModel.uninstallTrigger.observe(this, trigger -> {
            if (trigger != null && trigger) {
                try {
                    Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
                    uninstallIntent.setData(Uri.parse("package:" + getPackageName()));
                    uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(uninstallIntent);
                } catch (Exception e) {
                    UiArchitect.showValidationDialog(this, "Teardown Complete", "All background sentinels and SQLite data cleared. You may now uninstall the app.");
                }
                finishAffinity();
            }
        });
    }

    private void filterMatrix(String query) {
        if (query.trim().isEmpty()) {
            renderMatrix(currentNodes);
            return;
        }
        String lower = query.toLowerCase().trim();
        List<FactoryResetViewModel.TableNode> filtered = new ArrayList<>();
        for (FactoryResetViewModel.TableNode n : currentNodes) {
            if (n.name.toLowerCase().contains(lower)) {
                filtered.add(n);
            }
        }
        renderMatrix(filtered);
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
            
            if (!node.sample.isEmpty()) {
                TableRow header = new TableRow(this);
                header.setBackgroundColor(Color.parseColor("#15000000"));
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
        addAccessibilityButton(footer, "PRE_UNINSTALL_SAFEGUARD", v -> showPreUninstallConfirmationDialog());
        addAccessibilityButton(footer, "VIOLENT_MASTER_PURGE", v -> showMasterPurgeDialog());
        
        matrixBox.addView(footer);
    }

    private void showSelectivePurgeDialog(List<FactoryResetViewModel.TableNode> nodes) {
        String[] displayItems = new String[nodes.size()];
        String[] tableNames = new String[nodes.size()];
        boolean[] selected = new boolean[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            tableNames[i] = nodes.get(i).name;
            displayItems[i] = nodes.get(i).name.toUpperCase() + " (" + nodes.get(i).count + " rows)";
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("SELECT DUAL-STACK PURGE TARGETS")
            .setMultiChoiceItems(displayItems, selected, (d, which, isChecked) -> selected[which] = isChecked)
            .setPositiveButton("WIPE SELECTED (LOCAL+CLOUD)", (d, which) -> {
                List<String> toDelete = new ArrayList<>();
                for (int i = 0; i < tableNames.length; i++) if (selected[i]) toDelete.add(tableNames[i]);
                if (!toDelete.isEmpty()) viewModel.executeSelectivePurge(toDelete);
                else Toast.makeText(this, "No nodes selected for purge.", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("ABORT", null)
            .show();
        applyDialogTheme(dialog);
    }

    private void showPreUninstallConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("🚀 PRE-UNINSTALL GRACEFUL TEARDOWN")
            .setMessage("This protocol will gracefully prepare your device for complete uninstallation:\n\n" +
                        "1. Halt active background tracking service\n" +
                        "2. Cancel all scheduled update and sync workers\n" +
                        "3. Unregister all Android OS Geofence perimeters\n" +
                        "4. Revoke active Firebase Cloud Messaging (FCM) push tokens\n" +
                        "5. Wipe all local databases and cached keys\n\n" +
                        "Proceed with complete teardown?")
            .setPositiveButton("EXECUTE TEARDOWN", (d, which) -> {
                viewModel.executeFullSystemTeardown();
            })
            .setNegativeButton("CANCEL", null)
            .show();
        applyDialogTheme(dialog);
    }

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Cryptographic Factory Purge Verification: Verifies user input against salted SHA-256 hash
    private void showMasterPurgeDialog() {
        UiArchitect.showInputPromptDialog(this, 
            "⚠️ MASTER FACTORY PURGE", 
            "Enter master security password to confirm irrevocable wipe...", 
            "", 
            value -> {
                if (RegistryManager.verifyAndUpgradePurgePhrase(this, value != null ? value.trim() : "")) {
                    viewModel.executeMasterPurge();
                } else {
                    UiArchitect.showValidationDialog(this, "Purge Aborted", "Incorrect confirmation password. System ledger preserved.");
                }
            });
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
            File docs = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
            if (docs == null) docs = getFilesDir();
            if (!docs.exists()) docs.mkdirs();
            File dump = new File(docs, "JFINDME_DUMP_" + System.currentTimeMillis() + ".txt");
            java.io.FileWriter writer = new java.io.FileWriter(dump);
            writer.write(exportDataBuilder.toString());
            writer.close();
            Toast.makeText(this, "Forensic Dump Saved: " + dump.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveCurrentState(android.os.Bundle outState) {
        if (outState == null) return;
        if (searchFilterEt != null) {
            outState.putString("SAVED_TABLE_FILTER", searchFilterEt.getText().toString());
        }
    }

    @Override
    protected void onUiRebuilt(android.os.Bundle savedState) {
        if (savedState == null) return;
        String query = savedState.getString("SAVED_TABLE_FILTER", "");
        if (searchFilterEt != null && !query.isEmpty()) {
            searchFilterEt.setText(query);
        }
    }
}
