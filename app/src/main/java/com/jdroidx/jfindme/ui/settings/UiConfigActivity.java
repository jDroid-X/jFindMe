package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.DashboardTileTable;
import com.jdroidx.jfindme.ui.base.EnterpriseCardTable;
import com.jdroidx.jfindme.ui.base.HeaderTable;
import com.jdroidx.jfindme.ui.base.IconTable;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;
import com.jdroidx.jfindme.ui.base.AngelLoad;
import java.util.Map;

/**
 * 👑 GLOBAL UI ARCHITECT: Waterfall MVVM Implementation (L6)
 * Diagnostics & Management Hub for the Enterprise UI Matrix.
 */
public class UiConfigActivity extends BaseActivityJava {

    private UiConfigViewModel viewModel;
    private LinearLayout content;

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());
        mainRoot.setClipChildren(false);
        mainRoot.setClipToPadding(false);

        // 1. Header (Delegated to HeaderTable)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyHeader(this, header, "HEADER_MAIN");
        
        TextView title = new TextView(this);
        title.setText("GLOBAL UI ARCHITECT");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Waterfall MVVM Registry Diagnostics");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        header.addView(subtitle);
        
        mainRoot.addView(header);

        // 2. Body Matrix
        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        
        int masterPadding = getResponsivePaddingQuota();
        content.setPadding(masterPadding, 20, masterPadding, 100);
        
        scroll.addView(content);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainRoot;
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(UiConfigViewModel.class);

        // --- LAYER 1: THEME & GEOMETRY (AESTHETIC MONITOR) ---
        renderAestheticMonitor();

        // --- LAYER 0: DESIGN STAGES (UNIVERSAL SCALING) ---
        renderStageMonitor();

        // --- LAYER -1: VISUAL ARCHITECTURE PREVIEW ---
        renderGalleryLauncher();

        // --- LAYER 1.1: UI SCALING MATRIX TABLE (L7 ARCHITECT) ---
        renderGlobalScalingMatrixTable();

        // --- LAYER 1.1.5: UNIFIED TYPOGRAPHY TIERS (UI ARCHITECT) ---
        renderUnifiedTypographyMatrix();

        // --- LAYER 0.2: UNIFIED PADDING MATRIX (LIVE DIAGNOSTICS) ---
        renderUnifiedPaddingMatrix();

        // --- LAYER 0.3: SHADOW ELEVATION MATRIX (DEPTH STATS) ---
        renderShadowMatrixMonitor();
        
        // --- LAYER 0.3.5: SHADOW VISIBILITY LAB (QA PREVIEW) ---
        renderShadowLabMonitor();

        // --- LAYER 0.4: ARCHITECTURAL SCALING AWARENESS (RULES) ---
        renderScalingAwarenessMonitor();
        
        // --- LAYER 0.5: HEADER ARCHITECTURE (PANEL HEIGHT) ---
        renderHeaderArchitectureMonitor();

        // --- LAYER 0.6: CARD ARCHITECTURE (GEOMETRY MATRIX) ---
        renderCardArchitectureMonitor();

        // --- LAYER 0.6.5: CARD PHYSICAL AUDIT (DYNAMIC METRICS) ---
        renderCardPhysicalAudit();

        // --- LAYER 0.7: STRUCTURAL GEOMETRY (SHADOW & RADIUS TIERS) ---
        renderStructuralGeometryMonitor();

        // --- LAYER 0.8: COMPONENT DENSITY (PADDING & SPACING) ---
        renderDensityControllerMonitor();

        // --- LAYER 0.9: VECTOR ICON DENSITY (SCALING & GLOW) ---
        renderIconDensityMonitor();

        // --- LAYER 1.0: WIDGET TINTS (CHECKBOX & BODY THEME) ---
        renderWidgetTintMonitor();

        // --- LAYER 1.1: FORM INPUT MATRIX (FIELD SCALING) ---
        renderFormInputMonitor();

        // --- LAYER 1.2: DASHBOARD TILE MATRIX (GRID SCALING) ---
        renderTileMonitor();

        // --- LAYER 1.3: SYSTEM FONT MATRIX (TEXT PROTOCOL) ---
        renderFontMatrixMonitor();

        // --- LAYER 2: ENTERPRISE CARD MATRIX (Waterfall) ---
        viewModel.getEnterpriseCards().observe(this, cards -> {
            renderMatrixSection("ENTERPRISE CARD MATRIX (GEOMETRY)", cards);
        });

        // --- LAYER 3: DASHBOARD TILE MATRIX (Waterfall) ---
        viewModel.getDashboardTiles().observe(this, tiles -> {
            renderMatrixSection("DASHBOARD TILE MATRIX (GRID)", tiles);
        });

        // --- LAYER 4: ICON ARCHITECT MATRIX (Waterfall) ---
        viewModel.getIconMatrix().observe(this, icons -> {
            renderMatrixSection("ICON ARCHITECT MATRIX (VECTORS)", icons);
        });

        // --- LAYER 5: HEADER ARCHITECTURE MATRIX (Waterfall) ---
        viewModel.getHeaderMatrix().observe(this, headers -> {
            renderMatrixSection("HEADER ARCHITECTURE MATRIX (GRADIENTS)", headers);
        });
        
        // --- LAYER 6: FORM INPUT MATRIX (Waterfall) ---
        viewModel.getInputMatrix().observe(this, inputs -> {
           renderMatrixSection("FORM INPUT MATRIX (SECURITY)", inputs);
        });

        // --- LAYER 7: ACTION BUTTON MATRIX (Waterfall) ---
        viewModel.getActionButtons().observe(this, buttons -> {
            renderMatrixSection("ACTION BUTTON MATRIX (WORKFLOW)", buttons);
        });

        // --- LAYER 8: NAVIGATION MATRIX (Waterfall) ---
        viewModel.getNavMatrix().observe(this, navs -> {
            renderMatrixSection("NAVIGATION MATRIX (SESSION)", navs);
        });

        // --- LAYER 9: LIST ITEM MATRIX (Waterfall) ---
        viewModel.getListMatrix().observe(this, lists -> {
            renderMatrixSection("LIST ITEM MATRIX (DIAG)", lists);
        });

        // --- LAYER 10: MARQUEE MATRIX (Waterfall) ---
        viewModel.getMarqueeMatrix().observe(this, marques -> {
            renderMatrixSection("MARQUEE MATRIX (TICKER)", marques);
        });

        // --- LAYER 11: STATUS PILL MATRIX (Waterfall) ---
        viewModel.getStatusMatrix().observe(this, status -> {
            renderMatrixSection("STATUS PILL MATRIX (HEALTH)", status);
        });

        // --- LAYER 12: SYSTEM ERASE & FACTORY RESET (CONSOLE) ---
        renderFactoryResetConsole();
    }

    private void renderStageMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");

        // --- STAGE SELECTOR (SINGLE ROW HORIZONTAL) ---
        android.widget.HorizontalScrollView hScroll = new android.widget.HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        
        LinearLayout stageRow = new LinearLayout(this);
        stageRow.setOrientation(LinearLayout.HORIZONTAL);
        stageRow.setGravity(Gravity.CENTER_VERTICAL);
        stageRow.setPadding(0, 10, 0, 10);

        UiArchitect.DesignStage currentStage = UiArchitect.getStage(this);
        String[] stageNames = {"LIBERAL", "STANDARD", "COMPACT", "SLIM", "ULTRA-SLIM"};
        String[] stageWidths = {"800dp+", "600dp+", "400dp+", "320dp+", "<320dp"};

        for (int i = 0; i < 5; i++) {
            final int index = i;
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(35, 30, 35, 30);
            tile.setFocusable(true);
            tile.setClickable(true);

            // 🚀 QA Fix: Global Focus State Loop (Enter / D-Pad Support)
            tile.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                    (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                    v.performClick();
                    return true;
                }
                return false;
            });
            
            TextView tIcon = new TextView(this);
            tIcon.setText(stageNames[i].substring(0, 2)); // Use first 2 chars
            tIcon.setTextColor(Color.WHITE);
            tIcon.setTextSize(18);
            tIcon.setTypeface(null, Typeface.BOLD);
            tIcon.setGravity(Gravity.CENTER);
            
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            gd.setCornerRadius(25f);
            
            if (currentStage.ordinal() == i) {
                gd.setColor(getGradientStart()); // ACTIVE STAGE Focus
                tIcon.setAlpha(1.0f);
            } else {
                gd.setColor(Color.LTGRAY);
                tIcon.setAlpha(0.6f);
            }
            tIcon.setBackground(gd);
            int sSize = UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
            tile.addView(tIcon, new LinearLayout.LayoutParams(sSize, sSize));

            TextView tName = new TextView(this);
            tName.setText(stageNames[i]);
            tName.setTextColor(getTextColor());
            tName.setTextSize(10);
            tName.setGravity(Gravity.CENTER);
            tName.setTypeface(null, Typeface.BOLD);
            tName.setPadding(0, 5, 0, 0);
            tile.addView(tName);

            tile.setOnClickListener(v -> {
                String msg = "SYNCHRONIZING SYSTEM STAGE TO [" + stageNames[index] + "]...";
                AngelLoad.engage(this, msg, () -> {
                    viewModel.updateStage(index);
                    safeRecreate();
                });
            });

            // 🚀 QA Fix: Global Focus State Loop (Enter / D-Pad Support)
            tile.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                    (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                    v.performClick();
                    return true;
                }
                return false;
            });

            stageRow.addView(tile);
        }
        hScroll.addView(stageRow);
        container.addView(hScroll);

        TextView footer = new TextView(this);
        footer.setText("Universal Design Scaling Factor: Tap a Stage to Engage Override.");
        footer.setTextColor(getSecondaryTextColor());
        footer.setTextSize(9f);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 10, 0, 0);
        container.addView(footer);

        content.addView(createSectionHeader("SYSTEM STAGE (UNIVERSAL SCALING)"));
        content.addView(container, getSectionParams());
    }

    private void renderGlobalScalingMatrixTable() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");

        // --- TABLE HEADER ---
        LinearLayout tableHeader = new LinearLayout(this);
        tableHeader.setOrientation(LinearLayout.HORIZONTAL);
        tableHeader.setBackgroundColor(Color.parseColor("#10FFFFFF"));
        tableHeader.setPadding(20, 20, 20, 20);
        
        String[] headers = {"STAGE", "HEAD", "BODY", "PAD X", "RESULT PX"};
        for (String h : headers) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setTextColor(getGradientStart());
            tv.setTextSize(10f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            tableHeader.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        }
        container.addView(tableHeader);

        // --- STAGE-BY-STAGE REGISTRY ---
        UiArchitect.DesignStage currentStage = UiArchitect.getStage(this);
        UiArchitect.DesignStage[] stages = UiArchitect.DesignStage.values();
        
        for (UiArchitect.DesignStage s : stages) {
            UiScalingMatrixTable.ScalingRecord record = UiScalingMatrixTable.get(s);
            boolean isActive = (s == currentStage);
            
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(20, 30, 20, 30);
            row.setGravity(Gravity.CENTER_VERTICAL);
            
            if (isActive) {
                // 🚀 L7 High-Visibility Selection: Apply Gradient for 'Selected' item
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setColors(new int[]{ Color.parseColor("#15000000"), getGradientEnd() });
                gd.setAlpha(40);
                row.setBackground(gd);
            }

            // Columns: High-Impact Scaling Interceptor (Base: 16.0f / 15.0f)
            float baseFactor = isActive ? 16.0f : 15.0f;
            float scaledSize = UiScalingMatrixTable.scaleText(s, baseFactor, isActive);

            String[] values = {
                s.name(), 
                String.format("%.2fx", record.headerTextScale), 
                String.format("%.2fx", record.bodyTextScale),
                String.format("%.2fx", record.paddingScale), 
                (int)(25 * record.paddingScale) + " px"
            };

            for (int i = 0; i < 5; i++) {
                TextView tv = new TextView(this);
                tv.setText(values[i]);
                tv.setTextColor(isActive ? getGradientStart() : getTextColor());
                tv.setTextSize(scaledSize);
                tv.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
                tv.setGravity(Gravity.CENTER);
                row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            }
            
            container.addView(row);
            if (s.ordinal() < stages.length - 1) {
                View div = new View(this);
                div.setBackgroundColor(Color.argb(20, 150, 150, 150));
                container.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            }
        }

        content.addView(createSectionHeader("UI SCALING MATRIX TABLE (L7 ARCHITECT)"));
        content.addView(container, getSectionParams());
        
        // --- SELECTION LEGEND: L7 Dynamic Scaling Keys ---
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER);
        legend.setPadding(0, 15, 0, 15);
        
        TextView headLegend = new TextView(this);
        headLegend.setText("● HEAD: Scaling for Branding & Headers");
        headLegend.setTextColor(getGradientStart());
        headLegend.setTextSize(9f);
        headLegend.setPadding(20, 0, 20, 0);
        legend.addView(headLegend);
        
        TextView bodyLegend = new TextView(this);
        bodyLegend.setText("● BODY: Scaling for Lists & Text Content");
        bodyLegend.setTextColor(getTextColor());
        bodyLegend.setTextSize(9f);
        bodyLegend.setAlpha(0.6f);
        bodyLegend.setPadding(20, 0, 20, 0);
        legend.addView(bodyLegend);
        
        content.addView(legend);
        
        // --- SECONDARY FOOTER ---
        TextView info = new TextView(this);
        info.setText("Universal Sizing Matrix: Your active selection is highlighted with the Sunrise Orange gradient.");
        info.setTextColor(getSecondaryTextColor());
        info.setTextSize(9f);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 5, 0, 20);
        content.addView(info);
    }

    private void renderUnifiedPaddingMatrix() {
        android.widget.GridLayout grid = new android.widget.GridLayout(this);
        grid.setColumnCount(2);
        grid.setPadding(0, 10, 0, 10);
        
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        UiScalingMatrixTable.ScalingRecord record = UiScalingMatrixTable.get(stage);
        
        int pad = getResponsivePaddingQuota();
        int head = getResponsiveHeaderTopQuota();
        
        float headFactor = record.headerTextScale;
        float bodyFactor = record.bodyTextScale;
        float iconFactor = record.iconScale;

        grid.addView(createDiagnosticTile("SCREEN PADDING", pad + "dp"));
        grid.addView(createDiagnosticTile("HEADER TOP GAP", head + "dp"));
        grid.addView(createDiagnosticTile("HEAD SCALE (TIER1)", headFactor + "x"));
        grid.addView(createDiagnosticTile("BODY SCALE (TIER2)", bodyFactor + "x"));
        grid.addView(createDiagnosticTile("ICON SCALING", iconFactor + "x"));

        content.addView(createSectionHeader("UNIFIED SCREEN PADDING MATRIX (LIVE STATS)"));
        content.addView(grid, getSectionParams());
    }

    private void renderShadowMatrixMonitor() {
        android.widget.GridLayout grid = new android.widget.GridLayout(this);
        grid.setColumnCount(3);
        grid.setPadding(0, 10, 0, 10);
        
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        float factor = 1.0f;
        switch (stage) {
            case LIBERAL:   factor = 2.0f; break;
            case STANDARD:  factor = 1.6f; break;
            case COMPACT:   factor = 1.4f; break;
            case SLIM:      factor = 1.2f; break;
            case ULTRA_SLIM:factor = 1.0f; break;
        }

        grid.addView(createDiagnosticTile("LOW DEPTH", String.format("%.1ff", 4.0f * factor)));
        grid.addView(createDiagnosticTile("MEDIUM DEPTH", String.format("%.1ff", 12.0f * factor)));
        grid.addView(createDiagnosticTile("HIGH DEPTH", String.format("%.1ff", 25.0f * factor)));

        content.addView(createSectionHeader("SHADOW & DEPTH MATRIX (ELEVATION TIERS)"));
        content.addView(grid, getSectionParams());
    }

    private void renderShadowLabMonitor() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 20, 0, 20);
        
        // --- PREVIEW 1: Standard 'Soft' Shadow ---
        LinearLayout card1 = new LinearLayout(this);
        card1.setOrientation(LinearLayout.VERTICAL);
        card1.setGravity(Gravity.CENTER);
        card1.setPadding(30, 50, 30, 50);
        UiArchitect.applyCard(this, card1, "CARD_STANDARD");
        
        TextView t1 = new TextView(this);
        t1.setText("SOFT\nGLOW");
        t1.setTextSize(10f);
        t1.setGravity(Gravity.CENTER);
        t1.setTextColor(getTextColor());
        card1.addView(t1);
        
        // --- PREVIEW 2: High 'Floating' Shadow ---
        LinearLayout card2 = new LinearLayout(this);
        card2.setOrientation(LinearLayout.VERTICAL);
        card2.setGravity(Gravity.CENTER);
        card2.setPadding(30, 50, 30, 50);
        UiArchitect.applyCard(this, card2, "CARD_DASHBOARD");
        
        TextView t2 = new TextView(this);
        t2.setText("DASH\nFLOAT");
        t2.setTextSize(10f);
        t2.setGravity(Gravity.CENTER);
        t2.setTextColor(getTextColor());
        card2.addView(t2);

        // --- PREVIEW 3: Ultra 'Modal' Shadow ---
        LinearLayout card3 = new LinearLayout(this);
        card3.setOrientation(LinearLayout.VERTICAL);
        card3.setGravity(Gravity.CENTER);
        card3.setPadding(30, 50, 30, 50);
        UiArchitect.applyCard(this, card3, "CARD_MODAL");
        
        TextView t3 = new TextView(this);
        t3.setText("ULTRA\nMODAL");
        t3.setTextSize(10f);
        t3.setGravity(Gravity.CENTER);
        t3.setTextColor(getTextColor());
        card3.addView(t3);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(15, 15, 15, 15);
        
        row.addView(card1, lp);
        row.addView(card2, lp);
        row.addView(card3, lp);

        content.addView(createSectionHeader("SHADOW VISIBILITY LAB (DEPTH PREVIEW)"));
        content.addView(row, getSectionParams());
    }

    private void renderScalingAwarenessMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");

        UiArchitect.DesignStage currentStage = UiArchitect.getStage(this);
        
        String[][] scalingData = {
            {"LIBERAL (x2.0 Multiplier | Tablets)", "LIBERAL"},
            {"STANDARD (x1.6 Multiplier | Smartphones)", "STANDARD"},
            {"COMPACT (x1.4 Multiplier | Baseline)", "COMPACT"},
            {"SLIM (x1.2 Multiplier | Small)", "SLIM"},
            {"ULTRA_SLIM (x1.0 Baseline | Micro)", "ULTRA_SLIM"}
        };

        for (String[] row : scalingData) {
            TextView rule = new TextView(this);
            rule.setText("• " + row[0]);
            rule.setPadding(30, 20, 30, 20);
            rule.setTextSize(12.5f);
            
            if (currentStage.name().equals(row[1])) {
                rule.setTextColor(getGradientStart()); // Highlight active architectural awareness
                rule.setTypeface(null, Typeface.BOLD);
            } else {
                rule.setTextColor(getTextColor());
                rule.setAlpha(0.65f);
            }
            container.addView(rule);
        }

        TextView info = new TextView(this);
        info.setText("Geometric depth is calculated in real-time based on the active structural multiplier above.");
        info.setTextColor(getSecondaryTextColor());
        info.setTextSize(9f);
        info.setPadding(30, 10, 30, 20);
        info.setAlpha(0.8f);
        container.addView(info);

        content.addView(createSectionHeader("ARCHITECTURAL SCALING AWARENESS (DEPTH MATRIX)"));
        content.addView(container, getSectionParams());
    }

    private View createDiagnosticTile(String label, String value) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(30, 40, 30, 40);
        UiArchitect.applyCard(this, tile, "CARD_STANDARD");

        TextView valTxt = new TextView(this);
        valTxt.setText(value);
        valTxt.setTextColor(getGradientStart());
        valTxt.setTextSize(20f);
        valTxt.setTypeface(null, Typeface.BOLD);
        tile.addView(valTxt);

        TextView lblTxt = new TextView(this);
        lblTxt.setText(label);
        lblTxt.setTextColor(getTextColor());
        lblTxt.setTextSize(9f);
        lblTxt.setAlpha(0.7f);
        lblTxt.setPadding(0, 10, 0, 0);
        lblTxt.setGravity(Gravity.CENTER);
        tile.addView(lblTxt);

        android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
        int margin = 10;
        lp.setMargins(margin, margin, margin, margin);
        tile.setLayoutParams(lp);

        return tile;
    }

    private void renderAestheticMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");

        TextView label = new TextView(this);
        label.setText("SELECT GLOBAL AESTHETIC");
        label.setTextColor(getTextColor());
        label.setTypeface(null, Typeface.BOLD);
        label.setTextSize(14f);
        label.setPadding(20, 0, 0, 20);
        container.addView(label);

        // --- THEME SYNC MATRIX (WRAPPING GRID) ---
        android.widget.GridLayout themeGrid = new android.widget.GridLayout(this);
        themeGrid.setColumnCount(4);
        themeGrid.setPadding(0, 10, 0, 30);

        String[] themeNames = {"FamiSafe Violet", "Twilight Midnight", "Oceanic Teal", "Sunrise Orange", "Bubblegum Pop", "Silver Chrome", "Electric Azure", "Golden Hour"};
        int tSize = UiArchitect.getResponsiveIconSize(this, "ICON_SMALL");
        
        for (int i = 0; i < 8; i++) {
            final int themeIndex = i;
            LinearLayout themeItem = new LinearLayout(this);
            themeItem.setOrientation(LinearLayout.VERTICAL);
            themeItem.setGravity(Gravity.CENTER);
            themeItem.setPadding(20, 20, 20, 20); 
            themeItem.setFocusable(true);
            themeItem.setClickable(true);

            View circle = new View(this);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            
            int s = com.jdroidx.jfindme.ui.base.ThemeManager.getGradientStart(i);
            int e = com.jdroidx.jfindme.ui.base.ThemeManager.getGradientEnd(i);
            gd.setColors(new int[]{s, e});
            gd.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TL_BR);
            
            if (currentThemeId == i) {
                gd.setStroke(8, getGradientStart()); // Professional Highlight
            }
            circle.setBackground(gd);
            LinearLayout.LayoutParams circleLp = new LinearLayout.LayoutParams(tSize, tSize);
            circleLp.gravity = Gravity.CENTER_HORIZONTAL;
            themeItem.addView(circle, circleLp);

            TextView tLabel = new TextView(this);
            tLabel.setText(themeNames[i]);
            tLabel.setTextColor(getTextColor());
            tLabel.setTextSize(10);
            tLabel.setTypeface(null, Typeface.BOLD);
            tLabel.setPadding(0, 10, 0, 0);
            tLabel.setGravity(Gravity.CENTER);
            themeItem.addView(tLabel);

            themeItem.setOnClickListener(v -> {
                String msg = "APPLYING [" + themeNames[themeIndex].toUpperCase() + "] AESTHETIC...";
                AngelLoad.engage(this, msg, () -> {
                    viewModel.updateTheme(themeIndex);
                    // 🚀 QA Fix: Stay on Screen! Instant repaint of the current hub.
                    safeRecreate();
                });
            });

            // 🚀 QA Fix: Global Focus State Loop (Enter / D-Pad Support)
            themeItem.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                    (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                    v.performClick();
                    return true;
                }
                return false;
            });

            android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
            themeItem.setLayoutParams(lp);
            themeGrid.addView(themeItem);
        }
        container.addView(themeGrid);

        content.addView(createSectionHeader("AESTHETIC MONITOR (MASTER THEMES)"));
        content.addView(container, getSectionParams());
    }

    private void renderFactoryResetConsole() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");
        container.setPadding(35, 35, 35, 35);

        TextView label = new TextView(this);
        label.setText("DESTRUCTIVE SYSTEM CONSOLE");
        label.setTextColor(Color.RED);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextSize(14f);
        label.setPadding(20, 0, 0, 20);
        container.addView(label);

        container.addView(createIconButton("FACTORY RESET CONSOLE (HARD PURGE)", android.R.drawable.ic_menu_revert, v -> {
            startActivity(new android.content.Intent(this, com.jdroidx.jfindme.ui.management.FactoryResetActivity.class));
        }));

        TextView warning = new TextView(this);
        warning.setText("WARNING: This action liquidates all forensic logs, node pairings, and local identity data natively. This cannot be undone.");
        warning.setTextColor(getSecondaryTextColor());
        warning.setTextSize(9f);
        warning.setPadding(20, 20, 20, 0);
        container.addView(warning);

        content.addView(createSectionHeader("SYSTEM ERASE & FACTORY RESET (HARD)"));
        content.addView(container, getSectionParams());
    }

    private View createSectionDividerLittle() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        lp.setMargins(40, 20, 40, 20);
        divider.setLayoutParams(lp);
        return divider;
    }

    private void renderHeaderArchitectureMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");

        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        
        String[][] data = {
            {"Liberal Branding Display (120px Top)", "LIBERAL"},
            {"Standard Enterprise Hub (80px Top)", "STANDARD"},
            {"Compact Information HUD (40px Top)", "COMPACT"},
            {"Slim Utility Bar (30px Top)", "SLIM"},
            {"Ultra-Slim Navigation Ribbon (20px Top)", "ULTRA_SLIM"}
        };

        for (String[] row : data) {
            TextView tv = new TextView(this);
            tv.setText(row[0]);
            tv.setPadding(20, 20, 20, 20);
            tv.setTextSize(13f);
            
            if (stage.name().equals(row[1])) {
                tv.setTextColor(getGradientStart()); // Highlight active architectural tier
                tv.setTypeface(null, Typeface.BOLD);
            } else {
                tv.setTextColor(getTextColor());
                tv.setAlpha(0.7f);
            }
            container.addView(tv);
        }

        content.addView(createSectionHeader("HEADER ARCHITECTURE (PANEL HEIGHT)"));
        content.addView(container, getSectionParams());
    }

    private void renderCardArchitectureMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");

        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        
        String[][] data = {
            {"Liberal Card (Radius 1.8x | Elev 2.0x)", "LIBERAL"},
            {"Standard Card (Radius 1.4x | Elev 1.6x)", "STANDARD"},
            {"Compact Card (Radius 1.0x | Elev 1.4x)", "COMPACT"},
            {"Slim Card (Radius 0.7x | Elev 1.2x)", "SLIM"},
            {"Ultra-Slim Card (Radius 0.4x | Elev 1.0x)", "ULTRA_SLIM"}
        };

        for (String[] row : data) {
            TextView tv = new TextView(this);
            tv.setText(row[0]);
            tv.setPadding(20, 20, 20, 20);
            tv.setTextSize(13f);
            
            if (stage.name().equals(row[1])) {
                tv.setTextColor(getGradientStart()); // Highlight active geometry tier
                tv.setTypeface(null, Typeface.BOLD);
            } else {
                tv.setTextColor(getTextColor());
                tv.setAlpha(0.7f);
            }
            container.addView(tv);
        }

        content.addView(createSectionHeader("ENTERPRISE CARD MATRIX (GEOMETRY WEIGHT)"));
        content.addView(container, getSectionParams());
    }

    private void renderCardPhysicalAudit() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");

        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        float rFactor = 1.0f, eFactor = 1.0f;
        switch (stage) {
            case LIBERAL:   rFactor = 1.8f; eFactor = 2.0f; break;
            case STANDARD:  rFactor = 1.4f; eFactor = 1.6f; break;
            case COMPACT:   rFactor = 1.0f; eFactor = 1.4f; break;
            case SLIM:      rFactor = 0.7f; eFactor = 1.2f; break;
            case ULTRA_SLIM:rFactor = 0.4f; eFactor = 1.0f; break;
        }

        Map<String, com.jdroidx.jfindme.ui.base.EnterpriseCardTable.CardRecord> registry = com.jdroidx.jfindme.ui.base.EnterpriseCardTable.getRegistry();
        
        // --- LIVE MULTIPLIER HEADER ---
        TextView headerInfo = new TextView(this);
        headerInfo.setText("ACTIVE MULTIPLIERS FOR [" + stage.name() + "]: Radius x" + rFactor + " | Elevation x" + eFactor);
        headerInfo.setTextColor(getGradientStart());
        headerInfo.setTypeface(null, Typeface.BOLD);
        headerInfo.setTextSize(10f);
        headerInfo.setPadding(30, 25, 30, 25);
        headerInfo.setBackgroundColor(Color.parseColor("#15000000")); // Subtle contrast
        container.addView(headerInfo);

        for (com.jdroidx.jfindme.ui.base.EnterpriseCardTable.CardRecord record : registry.values()) {
            float effRadius = record.baseRadius * rFactor;
            float effElevation = record.defaultElevation * eFactor;
            
            // Format for high-fidelity audit visibility
            String logText = record.id + ": R " + (int)record.baseRadius + " -> " + (int)effRadius + "px | Elev " + (int)record.defaultElevation + " -> " + (int)effElevation + "px";
            
            TextView tv = new TextView(this);
            tv.setText(logText);
            tv.setPadding(30, 20, 30, 20);
            tv.setTextSize(11f);
            tv.setTextColor(getTextColor());
            tv.setAlpha(0.85f);
            
            // Highlight the "Effective" values for architectural clarity
            // Note: In a professional UI, we could use SpannableString for inline highlighting
            container.addView(tv);
        }

        content.addView(createSectionHeader("CARDVIEW PHYSICAL MATRIX (DYNAMIC AUDIT)"));
        content.addView(container, getSectionParams());
    }

    private void renderStructuralGeometryMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        String[][] data = {
            {"Deep Structural (2.0x Radius | 2.0x Shadow)", "LIBERAL"},
            {"Floating Structural (1.4x Radius | 1.6x Shadow)", "STANDARD"},
            {"Static Baseline (1.0x Radius | 1.4x Shadow)", "COMPACT"},
            {"Slim Structural (0.6x Radius | 1.2x Shadow)", "SLIM"},
            {"Ultra-Slim Structural (0.4x Radius | 1.0x Shadow)", "ULTRA_SLIM"}
        };
        for (String[] row : data) { container.addView(createDiagnosticRow(row[0], stage.name().equals(row[1]))); }
        content.addView(createSectionHeader("STRUCTURAL GEOMETRY (DEPTH MATRIX)"));
        content.addView(container, getSectionParams());
    }

    private void renderDensityControllerMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        String[][] data = {
            {"Liberal Density (30px Header | 25px Pad)", "LIBERAL"},
            {"Standard Density (20px Header | 20px Pad)", "STANDARD"},
            {"Compact Density (15px Header | 15px Pad)", "COMPACT"},
            {"Slim Density (10px Header | 10px Pad)", "SLIM"},
            {"Micro Density (5px Header | 5px Pad)", "ULTRA_SLIM"}
        };
        for (String[] row : data) { container.addView(createDiagnosticRow(row[0], stage.name().equals(row[1]))); }
        content.addView(createSectionHeader("COMPONENT DENSITY (HEADER & SPACING)"));
        content.addView(container, getSectionParams());
    }

    private void renderIconDensityMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        String[][] data = {
            {"Immersive Iconography (1.8x Bounds | 1.5x Glow)", "LIBERAL"},
            {"Rich Iconography (1.4x Bounds | 1.2x Glow)", "STANDARD"},
            {"Standard Iconography (1.0x Baseline)", "COMPACT"},
            {"Lean Iconography (0.7x Bounds | 0.6x Glow)", "SLIM"},
            {"Stealth Iconography (0.5x Bounds | 0.4x Glow)", "ULTRA_SLIM"}
        };
        for (String[] row : data) { container.addView(createDiagnosticRow(row[0], stage.name().equals(row[1]))); }
        content.addView(createSectionHeader("VECTOR ICON DENSITY (SCALER PROTOCOL)"));
        content.addView(container, getSectionParams());
    }

    private void renderWidgetTintMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        Integer currentTheme = viewModel.getThemeId().getValue();
        String[][] data = {
            {"Themed Control Tint (Theme Index: " + (currentTheme != null ? currentTheme : 0) + ")", "LIBERAL"},
            {"High-Contrast Widget Tints (80% Luminance)", "STANDARD"},
            {"Balanced Widget Intensity (Baseline)", "COMPACT"},
            {"Subtle Micro-Controls (Narrow Tints)", "SLIM"},
            {"Survival Mode (Minimum Alpha Tints)", "ULTRA_SLIM"}
        };
        for (String[] row : data) { container.addView(createDiagnosticRow(row[0], stage.name().equals(row[1]))); }
        content.addView(createSectionHeader("WIDGET TINTS (CHECKBOX & BODY)"));
        content.addView(container, getSectionParams());
    }

    private void renderFormInputMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        String[][] data = {
            {"Deep Form (60px Pad | 24r Corner | 2.0x Elev)", "LIBERAL"},
            {"Rich Form (45px Pad | 20r Corner | 1.2x Elev)", "STANDARD"},
            {"Standard Form (40px Pad | 16r Corner)", "COMPACT"},
            {"Slim Form (30px Pad | 12r Corner | 0.6x Elev)", "SLIM"},
            {"Micro Form (20px Pad | 8r Corner | 0.3x Elev)", "ULTRA_SLIM"}
        };
        for (String[] row : data) { container.addView(createDiagnosticRow(row[0], stage.name().equals(row[1]))); }
        content.addView(createSectionHeader("FORM INPUT MATRIX (FIELD SCALING)"));
        content.addView(container, getSectionParams());
    }

    private void renderTileMonitor() {
        android.widget.GridLayout grid = new android.widget.GridLayout(this);
        grid.setColumnCount(2);
        
        int spacing = getResponsivePaddingQuota() / 2;
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        
        String[] samples = {"TILE_MAP", "TILE_PAIRING"};
        for (String key : samples) {
            View tile = UiArchitect.createTile(this, key, v -> {});
            android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
            lp.setMargins(spacing, spacing, spacing, spacing);
            grid.addView(tile, lp);
        }
        
        content.addView(createSectionHeader("INTERACTIVE TILE MONITOR (L7 GRID RESPONSE)"));
        content.addView(grid, getSectionParams());
    }

    private void renderFontMatrixMonitor() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");
        UiArchitect.DesignStage stage = UiArchitect.getStage(this);
        String[][] data = {
            {"Massive Typography (24sp / 20sp / 16sp)", "LIBERAL"},
            {"Rich Typography (18sp / 14sp / 12sp)", "STANDARD"},
            {"Standard Baseline (16sp / 13sp / 11sp)", "COMPACT"},
            {"Lean Typography (14sp / 12sp / 10sp)", "SLIM"},
            {"Micro Typography (12sp / 10sp / 9sp)", "ULTRA_SLIM"}
        };
        for (String[] row : data) { container.addView(createDiagnosticRow(row[0], stage.name().equals(row[1]))); }
        content.addView(createSectionHeader("SYSTEM FONT MATRIX (TYPOGRAPHY TIERS)"));
        content.addView(container, getSectionParams());
    }

    private View createDiagnosticRow(String text, boolean isActive) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(12.5f);
        if (isActive) {
            tv.setTextColor(getGradientStart());
            tv.setTypeface(null, Typeface.BOLD);
        } else {
            tv.setTextColor(getTextColor());
            tv.setAlpha(0.65f);
        }
        return tv;
    }

    private void renderMatrixSection(String title, Map<String, ?> registry) {
        content.addView(createSectionHeader(title));
        LinearLayout scrollWrapper = new LinearLayout(this);
        scrollWrapper.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, scrollWrapper, "CARD_STANDARD");
        UiArchitect.applyCard(this, scrollWrapper, "CARD_STANDARD");

        boolean first = true;
        for (String key : registry.keySet()) {
            TextView item = new TextView(this);
            item.setText("• " + key);
            item.setTextColor(getGradientStart());
            UiArchitect.applyText(this, item, "TEXT_LABEL");
            item.setPadding(20, 15, 20, 0);
            scrollWrapper.addView(item);

            if (first) {
                TextView details = new TextView(this);
                details.setText("  [SCHEMA-VALID]: Elevation, TalkBack, Interaction, Dynamic, Glow, Fallback...");
                details.setTextColor(getSecondaryTextColor());
                details.setAlpha(0.6f);
                UiArchitect.applyText(this, details, "TEXT_SUB");
                details.setPadding(35, 5, 20, 15);
                scrollWrapper.addView(details);
                first = false;
            } else {
                item.setPadding(20, 15, 20, 15);
            }
        }
        
        content.addView(scrollWrapper, getSectionParams());
    }

    private View createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(getGradientStart());
        UiArchitect.applyText(this, header, "TEXT_LABEL");
        header.setPadding(10, 50, 0, 20);
        return header;
    }

    private LinearLayout.LayoutParams getSectionParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = getResponsivePaddingQuota();
        lp.setMargins(margin, 0, margin, 35);
        return lp;
    }

    private void renderUnifiedTypographyMatrix() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");

        String[] headers = {"Tier", "Size", "Track", "Visuals"};
        LinearLayout tableHeader = new LinearLayout(this);
        tableHeader.setOrientation(LinearLayout.HORIZONTAL);
        tableHeader.setPadding(20, 20, 20, 20);
        tableHeader.setBackgroundColor(Color.parseColor("#10FFFFFF"));
        for (String h : headers) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setTextColor(getGradientStart());
            tv.setTextSize(9f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            tableHeader.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        }
        container.addView(tableHeader);

        String[][] tiers = {
            {"TEXT_HEADER", "22.0f", "Header", "Bold, Hero Zones"},
            {"TEXT_TITLE",  "18.0f", "Header", "Semi-Bold, Section"},
            {"TEXT_BODY",   "12.0f / 16.0f", "Body", "Standard Dossier"},
            {"TEXT_SUB",    "12.0f / 13.0f", "Body", "Metadata, History"},
            {"TEXT_MINI",   "11.5f", "Body", "Telemetry labels"},
            {"TEXT_LABEL",  "11.0f", "Body", "All-Caps Bold"},
            {"BUTTON_TEXT", "12.0f", "Body", "Opt. Grid CTAs"}
        };

        for (int i = 0; i < tiers.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(20, 25, 20, 25);
            row.setGravity(Gravity.CENTER_VERTICAL);

            for (int j = 0; j < 4; j++) {
                TextView tv = new TextView(this);
                tv.setText(tiers[i][j]);
                tv.setTextColor(getTextColor());
                tv.setTextSize(9f);
                tv.setGravity(Gravity.CENTER);
                if (j == 0) tv.setTypeface(null, Typeface.BOLD);
                row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            }
            container.addView(row);
            if (i < tiers.length - 1) {
                View div = new View(this);
                div.setBackgroundColor(Color.argb(15, 120, 120, 120));
                container.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            }
        }

        content.addView(createSectionHeader("UNIFIED TYPOGRAPHY TIERS (UI ARCHITECT)"));
        content.addView(container, getSectionParams());
        
        TextView footer = new TextView(this);
        footer.setText("Relational Auto-Calibration: All legacy fonts are intercepted and forced into these 7 L7 Tiers.");
        footer.setTextColor(getSecondaryTextColor());
        footer.setTextSize(8f);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 10, 0, 20);
        content.addView(footer);
    }

    private void renderGalleryLauncher() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, container, "CARD_STANDARD");
        
        container.addView(createIconButton("OPEN LIVE UI COMPONENT GALLERY", android.R.drawable.ic_menu_gallery, v -> {
            startActivity(new android.content.Intent(this, UiGalleryActivity.class));
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }));

        content.addView(createSectionHeader("VISUAL ARCHITECT & PREVIEW"));
        content.addView(container, getSectionParams());
    }
}
