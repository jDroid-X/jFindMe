package com.jdroidx.jfindme.ui.settings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Phase 3 Architecture: Avatar Editor (Marketplace + Identity Mirror).
 * Implements strict Horizontal Tab Categories, Component Selectors, and Animation Toggles.
 */
public class AvatarEditorActivity extends BaseActivityJava {

    private final String TAG = "AvatarEditorActivity";
    private android.webkit.WebView renderingStageWindow;
    private LinearLayout inventoryOptionsGrid;

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor()); // Theme Array Bound

        // 🚀 Premium Linear Gradient Hero Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("AVATAR WORKSPACE");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Marketplace & Genetic Blueprint Engine");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 10, 0, 0);
        header.addView(subtitle);

        root.addView(header);

        // --- 2. 3D Avatar Preview Environment ---
        root.addView(buildPreviewStage());

        // --- 3. Animation Controls (Important UX Addition) ---
        root.addView(buildAnimationToggles());

        // --- 4. Main Editor Workspace ---
        // Pushes the tab bar and inventory grid into the remaining screen space
        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.VERTICAL);
        workspace.setBackgroundColor(getPrimaryColor());
        workspace.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        
        workspace.addView(buildCategoryTabs());
        
        // Scrollable Options Grid
        ScrollView scrollZone = new ScrollView(this);
        scrollZone.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        inventoryOptionsGrid = new LinearLayout(this);
        inventoryOptionsGrid.setOrientation(LinearLayout.VERTICAL);
        inventoryOptionsGrid.setPadding(20, 20, 20, 300); // Pad for Bottom Action Bar
        scrollZone.addView(inventoryOptionsGrid);
        
        workspace.addView(scrollZone);
        
        // Wrap everything into a clean RelativeLayout to allow overlays
        android.widget.RelativeLayout wrap = new android.widget.RelativeLayout(this);
        wrap.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wrap.addView(root, new android.widget.RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        // 6. Bottom Action Bar (Apply & Reset)
        LinearLayout bottomActionBlock = new LinearLayout(this);
        bottomActionBlock.setOrientation(LinearLayout.HORIZONTAL);
        bottomActionBlock.setGravity(Gravity.CENTER);
        
        TextView resetBtn = new TextView(this);
        resetBtn.setText("✗ RESET     ");
        resetBtn.setTextColor(getTextColor());
        resetBtn.setTypeface(null, Typeface.BOLD);
        
        TextView applyBtn = new TextView(this);
        applyBtn.setText("     ✓ APPLY");
        applyBtn.setTextColor(getGradientOverlayText());
        applyBtn.setTypeface(null, Typeface.BOLD);
        
        // Apply (Primary Glowing Gradient)
        android.graphics.drawable.GradientDrawable applyBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{getGradientStart(), getGradientEnd()} // Sync Universal Array!
        );
        applyBg.setCornerRadius(50f);
        applyBtn.setBackground(applyBg);
        applyBtn.setPadding(50, 30, 50, 30);
        applyBtn.setElevation(15f);
        
        // Reset (Secondary Outline)
        android.graphics.drawable.GradientDrawable resetBg = new android.graphics.drawable.GradientDrawable();
        resetBg.setColor(Color.TRANSPARENT);
        resetBg.setStroke(3, getSecondaryTextColor());
        resetBg.setCornerRadius(50f);
        resetBtn.setBackground(resetBg);
        resetBtn.setPadding(50, 30, 50, 30);
        
        bottomActionBlock.addView(resetBtn);
        bottomActionBlock.addView(applyBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        android.widget.RelativeLayout.LayoutParams baParams = new android.widget.RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        baParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
        baParams.setMargins(60, 0, 60, 40);
        applySolidCardGeometry(bottomActionBlock, getCardColor(), 50f); // Enforce floating geometry directly into the matrix!
        
        // Action Handlers
        applyBtn.setOnClickListener(v -> saveCustomizationPayload());
        resetBtn.setOnClickListener(v -> finish()); // Canceling essentially resets
        
        wrap.addView(bottomActionBlock, baParams);

        // Load Default Tab
        loadInventoryItems("AVATARS");

        return wrap;
    }

    // Purged buildTopNav() physically from the database to enforce Top-Down Hero Header matrix parity.

    private View buildPreviewStage() {
        RelativeLayout stage = new RelativeLayout(this);
        int stageHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.40);
        stage.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, stageHeight));

        // 🧠 Dynamic Theme Injection
        android.graphics.drawable.GradientDrawable stageBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.BR_TL,
            new int[]{getGradientStart(), getCardColor(), getPrimaryColor()}
        );
        stage.setBackground(stageBg);

        renderingStageWindow = new android.webkit.WebView(this);
        renderingStageWindow.getSettings().setJavaScriptEnabled(true);
        renderingStageWindow.getSettings().setAllowFileAccessFromFileURLs(true);
        renderingStageWindow.getSettings().setAllowUniversalAccessFromFileURLs(true);
        renderingStageWindow.setBackgroundColor(Color.TRANSPARENT);
        renderingStageWindow.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // 🧠 QA Fix: Stream HTML Payload directly to Memory, mocking Base URL to trick Chrome Engine bounds natively!
        try {
            java.io.InputStream is = getAssets().open("web/vrm_viewer.html");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            renderingStageWindow.loadDataWithBaseURL("https://jfindme.local/", new String(buffer, "UTF-8"), "text/html", "UTF-8", null);
        } catch (Exception e) {}
        
        String currentMeshString = prefs.getString("equipped_vrm_string", "models/Seed-chan.vrm");
        renderingStageWindow.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(android.webkit.WebView view, android.webkit.WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://jfindme.local/")) {
                    try {
                        String assetPath = url.replace("https://jfindme.local/", "");
                        String mime = assetPath.endsWith(".vrm") ? "application/octet-stream" : "text/plain";
                        return new android.webkit.WebResourceResponse(mime, "UTF-8", getAssets().open(assetPath));
                    } catch (Exception e) {}
                }
                return super.shouldInterceptRequest(view, request);
            }

            public void onPageFinished(android.webkit.WebView view, String url) {
                String jsPath = "https://jfindme.local/" + (currentMeshString.startsWith("models") ? currentMeshString : "models/" + currentMeshString);
                renderingStageWindow.evaluateJavascript("window.loadModel('" + jsPath + "');", null);
            }
        });
        
        RelativeLayout.LayoutParams stageParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        stageParams.setMargins(0, 0, 0, 80); // Create room for Suggestion Bar
        stage.addView(renderingStageWindow, stageParams);

        // 🧠 RULE-BASED CUSTOMIZATION (Contextual AI Map Engine)
        TextView aiSuggestionBar = new TextView(this);
        // Using sample current GPS (e.g. 18.52, 73.85 from our Map Bridge) 
        String smartestOutfit = com.jdroidx.jfindme.engine.avatar.AvatarSmartEngine.INSTANCE.generateLocationBasedSuggestions(18.52, 73.85);
        aiSuggestionBar.setText("💡 " + smartestOutfit);
        aiSuggestionBar.setTextColor(Color.parseColor("#00E676"));
        aiSuggestionBar.setTextSize(12);
        aiSuggestionBar.setGravity(Gravity.CENTER);
        aiSuggestionBar.setBackgroundColor(Color.parseColor("#33004400")); // Dark green translucent block
        aiSuggestionBar.setPadding(0, 20, 0, 20);

        RelativeLayout.LayoutParams suggestionParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        suggestionParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        suggestionParams.setMargins(0, 0, 0, 30);
        stage.addView(aiSuggestionBar, suggestionParams);

        // Interactive Zoom/Pan Hint
        TextView hint = new TextView(this);
        hint.setText("🔍 PINCH & CUBE ROTATE");
        hint.setTextColor(Color.parseColor("#555555"));
        hint.setTextSize(10);
        
        RelativeLayout.LayoutParams hintParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        hintParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        hintParams.setMargins(0, 0, 0, 20);
        stage.addView(hint, hintParams);

        return stage;
    }

    private View buildAnimationToggles() {
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        applySolidCardGeometry(hScroll, getCardColor(), 0f); // Stripped glassmorphism, injected flat geometry

        LinearLayout animRow = new LinearLayout(this);
        animRow.setOrientation(LinearLayout.HORIZONTAL);
        animRow.setPadding(30, 30, 30, 30);
        animRow.setGravity(Gravity.CENTER);

        String[] behaviors = {"IDLE", "WALK", "RUN", "EMOTE", "JUMP"};
        for (String b : behaviors) {
            TextView animBtn = new TextView(this);
            animBtn.setText(b);
            animBtn.setTextColor(getTextColor());
            animBtn.setTextSize(10f);
            animBtn.setTypeface(null, Typeface.BOLD);
            animBtn.setGravity(Gravity.CENTER);
            
            // Central Modules
            android.graphics.drawable.GradientDrawable modBg = new android.graphics.drawable.GradientDrawable();
            modBg.setColor(Color.TRANSPARENT);
            modBg.setCornerRadius(100f);
            modBg.setStroke(3, getGradientStart()); // Active Gradient Sync Hook
            animBtn.setBackground(modBg);
            animBtn.setElevation(10f);
            
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(120, 120);
            p.setMargins(15, 0, 15, 0);
            animBtn.setLayoutParams(p);
            
            animBtn.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "▶ " + b, android.widget.Toast.LENGTH_SHORT).show();
                triggerNativeFallbackAnimation(b);
                com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.INSTANCE.syncAnimationState(b);
            });
            animRow.addView(animBtn);
        }
        
        hScroll.addView(animRow);
        return hScroll;
    }
    
    // 🧠 Native 3D WebGL Integration via Javascript Bridge
    private void triggerNativeFallbackAnimation(String state) {
        if (renderingStageWindow == null) return;
        renderingStageWindow.evaluateJavascript("if(window.playAnimation) window.playAnimation('" + state + "');", null);
    }

    private View buildCategoryTabs() {
        HorizontalScrollView hScrollTab = new HorizontalScrollView(this);
        hScrollTab.setHorizontalScrollBarEnabled(false);
        
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(30, 40, 30, 30);
        
        String[] cats = {"AVATARS", "BODY", "HAIR", "OUTFIT", "SHOES", "ACCESS", "AURA"};
        for (String cat : cats) {
            TextView tab = new TextView(this);
            tab.setText(cat);
            tab.setTextColor(getTextColor());
            tab.setTextSize(12);
            tab.setTypeface(null, Typeface.BOLD);
            tab.setPadding(50, 20, 50, 20);
            
            // Native UI Tab Indicator
            android.graphics.drawable.GradientDrawable chipBg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{getCardColor(), getPrimaryColor()}
            );
            chipBg.setStroke(2, getGradientStart());
            chipBg.setCornerRadius(50f);
            tab.setBackground(chipBg);
            
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(10, 0, 10, 0);
            tab.setLayoutParams(cp);
            
            tab.setOnClickListener(v -> loadInventoryItems(cat));
            tabRow.addView(tab);
        }
        
        hScrollTab.addView(tabRow);
        return hScrollTab;
    }

    private void loadInventoryItems(String category) {
        inventoryOptionsGrid.removeAllViews();
        com.jdroidx.jfindme.engine.avatar.AvatarSmartEngine.INSTANCE.preemptivelyCacheAssets(category.toLowerCase(), 1);

        if (category.equalsIgnoreCase("AVATARS")) {
            try {
                boolean foundMesh = false;
                String[] physicalModels = getAssets().list("models");
                if (physicalModels != null && physicalModels.length > 0) {
                    for (String vrmFilename : physicalModels) {
                        if (vrmFilename.endsWith(".vrm")) {
                            inventoryOptionsGrid.addView(createInventoryCard("AVATARS", vrmFilename, 0, null));
                            foundMesh = true;
                        }
                    }
                }
            } catch (java.io.IOException e) {
                SystemLogger.INSTANCE.e(TAG, "Local Storage Assets Mount Failure", e);
            }
        } else {
            // 🧠 COMPONENT STYLING SYSTEM:
            // Since VRM models are physically monolithic, we simulate 'interchangeable parts' by executing dynamic
            // Material Shader Recoloring over the WebGL bridge when a user tweaks their Body, Hair, or Outfit tabs!
            String[] hexColors = {"#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF", "#FFB6C1", "#8B4513"};
            String[] styleNames = {"Base Standard", "Obsidian Black", "Crimson Red", "Neon Green", "Sapphire Blue", "Sakura Pink", "Leather Brown"};
            
            for (int i = 0; i < hexColors.length; i++) {
                inventoryOptionsGrid.addView(createInventoryCard(category, "Style: " + styleNames[i], 0, hexColors[i]));
            }
        }
    }

    private View createInventoryCard(String category, String itemName, int coinCost, String hexColorCommand) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        applySolidCardGeometry(row, getCardColor(), 30f); // Material 3 UI Standard
        row.setPadding(30, 30, 30, 30);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 15, 0, 15);
        row.setLayoutParams(rowParams);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        // Thumbnail Hook
        ImageView thumb = new ImageView(this);
        
        // 🧠 QA Fix: Visually map 3D .vrm names to their respective 2D proxy image hashes natively!
        if (hexColorCommand != null) {
            // It's a color style swatch
            android.graphics.drawable.GradientDrawable td = new android.graphics.drawable.GradientDrawable();
            td.setColor(Color.parseColor(hexColorCommand));
            td.setCornerRadius(20f);
            td.setStroke(3, Color.parseColor("#5500DDFF")); // Neon stroke frame
            thumb.setBackground(td);
            android.widget.LinearLayout.LayoutParams p = new android.widget.LinearLayout.LayoutParams(80, 80);
            p.setMargins(10, 10, 10, 10);
            thumb.setLayoutParams(p);
        } else {
            // It's a root Avatar VRM File
            int dynamicIdx = Math.abs(itemName.hashCode()) % 22 + 1;
            int resId = getResources().getIdentifier("avatar_custom_" + dynamicIdx, "drawable", getPackageName());
            thumb.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_gallery);
            thumb.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
        }
        row.addView(thumb);

        // Text Matrix
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textBlock.setPadding(40, 0, 0, 0);
        
        // 🧠 QA Fix: Human Readable Identity (Strip extension boundaries)
        String cleanName = itemName.replace(".vrm", "").replace("_", " ").toUpperCase();
        TextView name = new TextView(this);
        name.setText(cleanName);
        name.setTextColor(getTextColor()); // Resolves missing bounds on light themes
        name.setTextSize(16);
        name.setTypeface(null, Typeface.BOLD);
        textBlock.addView(name);
        
        if (coinCost > 0) {
            TextView lockStatus = new TextView(this);
            lockStatus.setText("🔒 LOCKED - " + coinCost + " Coins");
            lockStatus.setTextColor(Color.parseColor("#FF00DD"));
            lockStatus.setTextSize(12);
            textBlock.addView(lockStatus);
        } else {
            TextView equipStatus = new TextView(this);
            equipStatus.setText("🔓 UNLOCKED");
            equipStatus.setTextColor(Color.parseColor("#00DDFF"));
            equipStatus.setTextSize(12);
            textBlock.addView(equipStatus);
        }
        row.addView(textBlock);

        // Interaction Hook (Avatar Engine Sync)
        row.setOnClickListener(v -> {
            if (coinCost > 0) {
                android.widget.Toast.makeText(this, "INSUFFICIENT GEMS TO UNLOCK!", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, "Equipping: " + itemName, android.widget.Toast.LENGTH_SHORT).show();
                
                // 🧠 MODULE 4 INTEGRATION: Dynamic Mesh Switching vs Property Manipulation
                if (hexColorCommand != null) {
                    // It's a nested component! Apply Material Tint.
                    renderingStageWindow.evaluateJavascript("window.changeComponentColor('" + category.toLowerCase() + "', '" + hexColorCommand + "');", null);
                    
                    // Blueprint Rule 7 (Animation Sync): Track the selection payload for Firebase Sync
                    com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.INSTANCE.queueCustomizationUpdate(category.toLowerCase(), hexColorCommand);
                } else {
                    // It's a Root VRM File Swap!
                    String relativeUri = itemName;
                    prefs.edit().putString("equipped_vrm_string", relativeUri).apply();
                    
                    // Apply visual proxy integer for backward compatibility
                    prefs.edit().putInt("equipped_avatar_id", Math.abs(itemName.hashCode())).apply();
                    
                    String absoluteVRM = "https://jfindme.local/models/" + relativeUri;
                    renderingStageWindow.evaluateJavascript("window.loadModel('" + absoluteVRM + "');", null);
                    
                    android.widget.Toast.makeText(this, "Master Mesh Root Extracted Successfully!", android.widget.Toast.LENGTH_SHORT).show();
                    com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.INSTANCE.queueCustomizationUpdate("avatar_root", itemName);
                }
            }
        });
        
        return row;
    }

    private View createPillButton(String text, View.OnClickListener action) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#333333"));
        btn.setPadding(40, 15, 40, 15);
        
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(10, 0, 10, 0);
        btn.setLayoutParams(p);
        btn.setOnClickListener(action);
        return btn;
    }

    private void saveCustomizationPayload() {
        android.widget.Toast.makeText(this, "Finalizing Customization Payload. Broadcasting to Multi-User DB.", android.widget.Toast.LENGTH_LONG).show();
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void setupLogic() {
        SystemLogger.INSTANCE.i(TAG, "Avatar Customizer Workspace Booted.");
    }
}
