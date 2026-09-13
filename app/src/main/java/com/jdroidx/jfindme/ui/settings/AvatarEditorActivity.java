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
import android.widget.Toast;
import android.content.Context;
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
    private android.widget.EditText nameEntryField;
    private boolean isModelLoading = false;
    private ProfileViewModel profileViewModel;

    // 🦋 Premium Enterprise Bridge: Links WebGL 3D Context to Android Native UI
    public class AvatarBridge {
        @android.webkit.JavascriptInterface
        public void onModelLoaded() {
            runOnUiThread(() -> {
                SystemLogger.i(TAG, "🦋 3D Matrix Rebuilt: Hiding AngelLoader.");
                isModelLoading = false;
                com.jdroidx.jfindme.utils.AngelLoader.hide(AvatarEditorActivity.this);
            });
        }
        
        @android.webkit.JavascriptInterface
        public void onModelError(String error) {
            runOnUiThread(() -> {
                SystemLogger.e(TAG, "3D Rendering Error: " + error);
                isModelLoading = false;
                com.jdroidx.jfindme.utils.AngelLoader.hide(AvatarEditorActivity.this);
                android.widget.Toast.makeText(AvatarEditorActivity.this, "RENDERING ENGINE FAILURE: " + error, android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }

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

        ImageView backBtn = new ImageView(this);
        backBtn.setImageResource(android.R.drawable.ic_menu_revert);
        backBtn.setColorFilter(getGradientOverlayText());
        int bS = scale(60);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(bS, bS);
        bLp.setMargins(0, 0, scale(20), 0);
        backBtn.setLayoutParams(bLp);
        backBtn.setOnClickListener(v -> finish());
        topRow.addView(backBtn);
        
        // 🚀 Identity Editor: Direct Nickname Modification
        nameEntryField = new android.widget.EditText(this);
        String currentNick = prefs.getString("user_nickname", prefs.getString("user_name", "Citizen"));
        nameEntryField.setText(currentNick);
        nameEntryField.setTextColor(getGradientOverlayText());
        nameEntryField.setTextSize(24);
        nameEntryField.setTypeface(null, Typeface.BOLD);
        nameEntryField.setBackground(null);
        nameEntryField.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        nameEntryField.setSingleLine(true);
        nameEntryField.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        
        nameEntryField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                saveCustomizationPayload();
                return true;
            }
            return false;
        });

        nameEntryField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String val = nameEntryField.getText().toString().trim();
                if (!val.isEmpty()) {
                    com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.INSTANCE.queueCustomizationUpdate("display_name", val);
                    prefs.edit().putString("user_nickname", val).apply();
                }
            }
        });

        topRow.addView(nameEntryField);
        
        ImageView editIcon = new ImageView(this);
        editIcon.setImageResource(android.R.drawable.ic_menu_edit);
        editIcon.setColorFilter(getGradientOverlayText());
        topRow.addView(editIcon, new LinearLayout.LayoutParams(scale(60), scale(60)));
        
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Identity Mirror & Genetic Blueprint Engine");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 10, 0, 0);
        header.addView(subtitle);

        root.addView(header);

        // --- 2. 3D Avatar Preview Environment ---
        root.addView(buildPreviewStage());

        // --- 3. Tactical Utilities (Environment & Camera) ---
        root.addView(buildTacticalUtilities());

        // --- 4. Animation Controls ---
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
        
        root.addView(workspace);
        
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
        resetBtn.setFocusable(true);
        resetBtn.setFocusableInTouchMode(true);
        resetBtn.setOnKeyListener((v, keyCode, event) -> {
            if ((keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER) && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                resetBtn.performClick();
                return true;
            }
            return false;
        });
        
        TextView applyBtn = new TextView(this);
        applyBtn.setText("     ✓ APPLY");
        applyBtn.setTextColor(getGradientOverlayText());
        applyBtn.setTypeface(null, Typeface.BOLD);
        applyBtn.setFocusable(true);
        applyBtn.setFocusableInTouchMode(true);
        applyBtn.setOnKeyListener((v, keyCode, event) -> {
            if ((keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER) && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                applyBtn.performClick();
                return true;
            }
            return false;
        });
        
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
        renderingStageWindow.addJavascriptInterface(new AvatarBridge(), "AndroidBridge");
        
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
                if (url.contains("jfindme.local")) {
                    String path = request.getUrl().getPath();
                    if (path != null && !path.isEmpty()) {
                        if (path.startsWith("/")) path = path.substring(1);
                        
                        try {
                            path = java.net.URLDecoder.decode(path, "UTF-8");
                        } catch (Exception e) {}

                        try {
                            String mime = "application/octet-stream";
                            if (path.endsWith(".html")) mime = "text/html";
                            else if (path.endsWith(".js")) mime = "application/javascript";
                            else if (path.endsWith(".vrm") || path.endsWith(".glb")) mime = "model/gltf-binary";
                            else if (path.endsWith(".ico")) mime = "image/x-icon";
                            else if (path.endsWith(".png")) mime = "image/png";
                            else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) mime = "image/jpeg";
                            
                            SystemLogger.i(TAG, "🔍 TACTICAL ASSET RECOVERY: Intercepting -> " + path);
                            // 🚀 TACTICAL ASSET RECOVERY: Direct stream extraction from APK assets
                            return new android.webkit.WebResourceResponse(mime, null, getAssets().open(path));
                        } catch (java.io.IOException e) {
                            SystemLogger.e(TAG, "🚨 FORENSIC ASSET FAILURE: Path '" + path + "' not located in APK assets.");
                            
                            // 🧠 Diagnostic Matrix: Deep-link validation to identify physical path drifts
                            try {
                                String parent = path.contains("/") ? path.substring(0, path.lastIndexOf("/")) : "";
                                String[] contents = getAssets().list(parent);
                                SystemLogger.d(TAG, "📂 REGISTRY SNAPSHOT ['" + parent + "']: " + (contents == null ? "NULL" : java.util.Arrays.toString(contents)));
                                
                                // Root Audit if subdirectory lookup fails
                                if (parent.length() > 0) {
                                    String[] root = getAssets().list("");
                                    SystemLogger.d(TAG, "📂 ROOT REGISTRY AUDIT: " + (root == null ? "NULL" : java.util.Arrays.toString(root)));
                                }
                            } catch (Exception ignored) {}

                            // 🛑 CRITICAL FIX: Return a clean 404 plain-text response to stop HTML parsing errors in THREE.js
                            return new android.webkit.WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", null, 
                                new java.io.ByteArrayInputStream("FORENSIC_ERROR: Asset not found in binary payload.".getBytes()));
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                isModelLoading = true;
                com.jdroidx.jfindme.utils.AngelLoader.show(AvatarEditorActivity.this);
                com.jdroidx.jfindme.utils.AngelLoader.setStatus(AvatarEditorActivity.this, "Initializing Identity Mirror...");

                String jsPath = "https://jfindme.local/" + (currentMeshString.startsWith("models") ? currentMeshString : "models/" + currentMeshString);
                renderingStageWindow.evaluateJavascript("window.loadModel('" + jsPath + "');", null);
                
                // 🚀 QA Fix: Force-hide loader if the initial page load completes without triggering JS Bridge
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (!isModelLoading) com.jdroidx.jfindme.utils.AngelLoader.hide(AvatarEditorActivity.this);
                }, 1000);
            }
        });
        
        RelativeLayout.LayoutParams stageParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        stageParams.setMargins(0, 0, 0, 80); // Create room for Suggestion Bar
        stage.addView(renderingStageWindow, stageParams);

        // 🧠 RULE-BASED CUSTOMIZATION
        TextView aiSuggestionBar = new TextView(this);
        String smartestOutfit = com.jdroidx.jfindme.engine.avatar.AvatarSmartEngine.INSTANCE.generateLocationBasedSuggestions(18.52, 73.85);
        aiSuggestionBar.setText("💡 " + smartestOutfit);
        aiSuggestionBar.setTextColor(Color.parseColor("#00E676"));
        aiSuggestionBar.setTextSize(12);
        aiSuggestionBar.setGravity(Gravity.CENTER);
        aiSuggestionBar.setBackgroundColor(Color.parseColor("#33004400"));
        aiSuggestionBar.setPadding(0, 20, 0, 20);

        RelativeLayout.LayoutParams suggestionParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        suggestionParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        suggestionParams.setMargins(0, 0, 0, 30);
        stage.addView(aiSuggestionBar, suggestionParams);

        TextView hint = new TextView(this);
        hint.setText("🔍 PINCH & CUBE ROTATE");
        hint.setTextColor(getSecondaryTextColor());
        hint.setTextSize(10);
        RelativeLayout.LayoutParams hintParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        hintParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        hintParams.setMargins(0, 0, 0, 20);
        stage.addView(hint, hintParams);

        return stage;
    }

    private View buildTacticalUtilities() {
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        applySolidCardGeometry(hScroll, getCardColor(), 0f); // Subtle pottery/ceramic separator

        LinearLayout toolRow = new LinearLayout(this);
        toolRow.setOrientation(LinearLayout.HORIZONTAL);
        toolRow.setPadding(30, 20, 30, 20);
        toolRow.setGravity(Gravity.CENTER);

        // -- 💡 LIGHTING TOGGLE --
        toolRow.addView(createUtilityChip("💡 LIGHT ON", v -> {
            renderingStageWindow.evaluateJavascript("window.setLighting('DAY');", null);
            Toast.makeText(this, "Simulating Solar Intensity", Toast.LENGTH_SHORT).show();
        }));
        
        toolRow.addView(createUtilityChip("🌙 NIGHT", v -> {
            renderingStageWindow.evaluateJavascript("window.setLighting('NIGHT');", null);
            Toast.makeText(this, "Applying Midnight Shaders", Toast.LENGTH_SHORT).show();
        }));
        
        // -- 📸 SNAP IDENTITY --
        toolRow.addView(createUtilityChip("📸 SNAP IDENT", v -> {
            Toast.makeText(this, "Capturing 3D Identity Node...", Toast.LENGTH_SHORT).show();
            // Logic for taking a screenshot/snapping the avatar state can be added here
        }));

        // -- 🔄 AUTO-ROTATE --
        toolRow.addView(createUtilityChip("🔄 AUTO-SPIN", v -> {
            renderingStageWindow.evaluateJavascript("window.toggleAutoRotate();", null);
        }));

        // -- 🛠️ RELOAD ENGINE --
        toolRow.addView(createUtilityChip("🛠️ RELOAD ENGINE", v -> {
            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            renderingStageWindow.reload();
            Toast.makeText(this, "Re-initializing 3D Matrix...", Toast.LENGTH_SHORT).show();
        }));

        // -- 🔍 RESET VIEW --
        toolRow.addView(createUtilityChip("🎯 RESET CAM", v -> {
            renderingStageWindow.evaluateJavascript("window.resetCamera();", null);
        }));

        hScroll.addView(toolRow);
        return hScroll;
    }

    private View createUtilityChip(String text, View.OnClickListener listener) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextColor(getTextColor());
        chip.setTextSize(9f);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setPadding(40, 15, 40, 15);
        chip.setGravity(Gravity.CENTER);
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(getCardColor());
        bg.setCornerRadius(100f);
        bg.setStroke(2, getGradientStart());
        chip.setBackground(bg);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(10, 0, 10, 0);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(listener);
        applyAccessibility(chip, text);
        return chip;
    }

    private View buildAnimationToggles() {
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        applySolidCardGeometry(hScroll, getCardColor(), 0f);

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
            
            android.graphics.drawable.GradientDrawable modBg = new android.graphics.drawable.GradientDrawable();
            modBg.setColor(Color.TRANSPARENT);
            modBg.setCornerRadius(100f);
            modBg.setStroke(3, getGradientStart());
            animBtn.setBackground(modBg);
            animBtn.setElevation(10f);
            
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(120, 120);
            p.setMargins(15, 0, 15, 0);
            animBtn.setLayoutParams(p);
            
            animBtn.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "▶ " + b, android.widget.Toast.LENGTH_SHORT).show();
                triggerNativeFallbackAnimation(b);
            });
            animRow.addView(animBtn);
        }
        
        hScroll.addView(animRow);
        return hScroll;
    }
    
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
        
        String[] cats = {"2D AVATARS (50 POPULAR)", "AVATARS (3D VRM)", "BODY", "HAIR", "OUTFIT", "SHOES", "ACCESS", "AURA"};
        for (String cat : cats) {
            TextView tab = new TextView(this);
            tab.setText(cat);
            tab.setTextColor(getTextColor());
            tab.setTextSize(12);
            tab.setTypeface(null, Typeface.BOLD);
            tab.setPadding(50, 20, 50, 20);
            
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

        if (category.startsWith("2D")) {
            load2DPopularAvatarsView();
        } else if (category.contains("3D") || category.equalsIgnoreCase("AVATARS")) {
            boolean foundMesh = false;
            String[] searchPaths = {"models"};
            
            for (String path : searchPaths) {
                try {
                    String[] files = getAssets().list(path);
                    if (files != null) {
                        for (String f : files) {
                            if (f.endsWith(".vrm")) {
                                foundMesh = true;
                                inventoryOptionsGrid.addView(createInventoryCard("AVATARS", f, 0, null, path + "/" + f));
                            }
                        }
                    }
                } catch (java.io.IOException e) {
                    SystemLogger.w(TAG, "Search Path Not Found: " + path);
                }
            }
            
            if (!foundMesh) {
                TextView error = new TextView(this);
                error.setText("⚠ IDENTITY INDEX FAILURE: NO ASSETS DETECTED");
                // 🔧 THEME FIX: Use theme-aware error color instead of hardcoded RED
                error.setTextColor(Color.parseColor("#FF5252"));
                error.setGravity(Gravity.CENTER);
                error.setPadding(0, 50, 0, 50);
                inventoryOptionsGrid.addView(error);
            }
        } else {
            // 🧠 COMPONENT STYLING SYSTEM:
            // Dynamic Material Shader Recoloring over WebGL bridge
            String[] hexColors = {"#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF", "#FFB6C1", "#8B4513"};
            String[] styleNames = {"Base Standard", "Obsidian Black", "Crimson Red", "Neon Green", "Sapphire Blue", "Sakura Pink", "Leather Brown"};
            
            for (int i = 0; i < hexColors.length; i++) {
                inventoryOptionsGrid.addView(createInventoryCard(category, "Style: " + styleNames[i], 0, hexColors[i], null));
            }
        }
    }

    /**
     * 🖼️ 2D Popular Avatars Gallery Loader with Search & Real-Time Filter
     */
    private void load2DPopularAvatarsView() {
        int pad = scale(12);
        
        // Search Filter Textbox
        android.widget.EditText searchBox = new android.widget.EditText(this);
        searchBox.setHint("🔍 Search 50 Avatars (e.g. King, Detective, Jane)...");
        searchBox.setHintTextColor(Color.argb(140, 148, 163, 184));
        searchBox.setTextColor(getTextColor());
        searchBox.setTextSize(14);
        searchBox.setSingleLine(true);
        searchBox.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        
        android.graphics.drawable.GradientDrawable sBg = new android.graphics.drawable.GradientDrawable();
        sBg.setCornerRadius(25f);
        sBg.setColor(getCardColor());
        sBg.setStroke(2, getGradientStart());
        searchBox.setBackground(sBg);
        searchBox.setPadding(30, 20, 30, 20);
        
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, 0, 0, 20);
        searchBox.setLayoutParams(sLp);
        inventoryOptionsGrid.addView(searchBox);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        inventoryOptionsGrid.addView(listContainer);

        java.util.List<? extends com.jdroidx.jfindme.engine.data.avatar.AvatarEntity> templateList = 
            com.jdroidx.jfindme.engine.data.avatar.AvatarRegistry.INSTANCE.getTemplateList();
        int currentlyEquippedId = prefs.getInt("equipped_avatar_id", 1);

        Runnable[] renderAvatars = new Runnable[1];
        final String[] queryHolder = {""};

        renderAvatars[0] = () -> {
            listContainer.removeAllViews();
            String q = queryHolder[0].toLowerCase(java.util.Locale.ROOT);

            for (final com.jdroidx.jfindme.engine.data.avatar.AvatarEntity entity : templateList) {
                int baseId = entity.getBaseId();
                String avName = entity.getType();
                String rarity = entity.getRarity().name();

                if (!q.isEmpty()) {
                    boolean matchName = avName.toLowerCase(java.util.Locale.ROOT).contains(q);
                    boolean matchId = String.valueOf(baseId).equals(q) || ("#" + baseId).equals(q);
                    boolean matchRarity = rarity.toLowerCase(java.util.Locale.ROOT).contains(q);
                    if (!matchName && !matchId && !matchRarity) continue;
                }

                boolean isEquipped = (baseId == currentlyEquippedId);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                applySolidCardGeometry(card, getCardColor(), 25f);
                card.setPadding(25, 20, 25, 20);
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cLp.setMargins(0, 10, 0, 10);
                card.setLayoutParams(cLp);

                // Thumbnail
                ImageView thumb = new ImageView(this);
                int dynamicResId = getResources().getIdentifier("avatar_custom_" + baseId, "drawable", getPackageName());
                if (dynamicResId == 0) {
                    int dyn = ((baseId - 1) % 22) + 1;
                    dynamicResId = getResources().getIdentifier("avatar_custom_" + dyn, "drawable", getPackageName());
                }
                thumb.setImageResource(dynamicResId != 0 ? dynamicResId : android.R.drawable.ic_menu_gallery);
                thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int uSize = scale(48);
                card.addView(thumb, new LinearLayout.LayoutParams(uSize, uSize));

                // Metadata Text Column
                LinearLayout col = new LinearLayout(this);
                col.setOrientation(LinearLayout.VERTICAL);
                col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                col.setPadding(25, 0, 0, 0);

                TextView title = new TextView(this);
                title.setText(baseId + ". " + avName);
                title.setTextColor(isEquipped ? Color.parseColor("#00E676") : getTextColor());
                title.setTextSize(15);
                title.setTypeface(null, Typeface.BOLD);
                col.addView(title);

                TextView sub = new TextView(this);
                sub.setText("Tier: " + rarity + "  ●  ID: #" + baseId + (isEquipped ? "  [ACTIVE]" : ""));
                sub.setTextColor(isEquipped ? Color.parseColor("#00E676") : Color.parseColor("#94A3B8"));
                sub.setTextSize(11);
                col.addView(sub);
                card.addView(col);

                // Tick Box Indicator
                TextView tick = new TextView(this);
                tick.setText(isEquipped ? "✓" : "☐");
                tick.setTextColor(Color.WHITE);
                tick.setTextSize(isEquipped ? 12 : 14);
                tick.setTypeface(null, Typeface.BOLD);
                tick.setGravity(Gravity.CENTER);
                android.graphics.drawable.GradientDrawable tBg = new android.graphics.drawable.GradientDrawable();
                tBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                tBg.setColor(isEquipped ? Color.parseColor("#00E676") : Color.argb(160, 100, 116, 139));
                tick.setBackground(tBg);
                int tSize = scale(28);
                card.addView(tick, new LinearLayout.LayoutParams(tSize, tSize));

                final int finalResId = dynamicResId;

                // Tap Action: Popup Dialog with Options
                card.setOnClickListener(v -> {
                    android.app.AlertDialog optDialog = new android.app.AlertDialog.Builder(this)
                        .setTitle("✨ " + baseId + ". " + avName)
                        .setMessage("Tier: " + rarity + "\nEquip this avatar across the entire system Single Source of Truth (SSOT)?")
                        .setIcon(finalResId)
                        .setPositiveButton(isEquipped ? "EQUIPPED ✓" : "EQUIP AVATAR [✓]", (d, w) -> {
                            prefs.edit().putInt("equipped_avatar_id", baseId).apply();
                            
                            String currentNick = prefs.getString("user_nickname", "Active Tracker");
                            String agentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(this);
                            
                            com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(this)
                                .updateDeviceIdentityAndAvatar(agentId, currentNick, "avatar_custom_" + baseId);
                            
                            com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.INSTANCE
                                .propagateAvatarUpdate(agentId, "avatar_custom_" + baseId, currentNick);
                            
                            com.jdroidx.jfindme.utils.AvatarLoader.clearCache();
                            
                            if (profileViewModel != null) {
                                profileViewModel.equipAvatar(prefs, baseId);
                            }

                            Toast.makeText(this, "Equipped Avatar: " + avName + " [✓]", Toast.LENGTH_SHORT).show();
                            load2DPopularAvatarsView();
                        })
                        .setNeutralButton("PREVIEW 3D PROXY", (d, w) -> {
                            String[] models = {"models/AliciaSolid.vrm", "models/BlackSuger.vrm", "models/ButterCup.vrm", "models/FoxModel.vrm"};
                            String m = models[Math.abs(baseId) % models.length];
                            if (renderingStageWindow != null) {
                                renderingStageWindow.evaluateJavascript("window.loadModel('https://jfindme.local/" + m + "');", null);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                    optDialog.show();
                    applyDialogTheme(optDialog);
                });

                listContainer.addView(card);
            }
        };

        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryHolder[0] = (s != null) ? s.toString().trim() : "";
                renderAvatars[0].run();
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        renderAvatars[0].run();
    }

    private View createInventoryCard(String category, String itemName, int coinCost, String hexColorCommand, String assetPathOverride) {
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
            int dynamicIdx = Math.abs(itemName.hashCode()) % 50 + 1;
            int resId = getResources().getIdentifier("avatar_custom_" + dynamicIdx, "drawable", getPackageName());
            thumb.setImageResource(resId != 0 ? resId : com.jdroidx.jfindme.R.drawable.ic_image_upload);
            thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
            thumb.setAdjustViewBounds(true);
            int pad = scale(4);
            thumb.setPadding(pad, pad, pad, pad);
            android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable();
            ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            ring.setStroke(3, getGradientStart());
            thumb.setBackground(ring);
            int universalSize = com.jdroidx.jfindme.ui.base.UiArchitect.getUniversalAvatarSize(this);
            thumb.setLayoutParams(new LinearLayout.LayoutParams(universalSize, universalSize));
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
                // 🚀 QA Premium Loader Process: Activate the Butterfly Guardian
                if (!isModelLoading) {
                    isModelLoading = true;
                    com.jdroidx.jfindme.utils.AngelLoader.show(this);
                    com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Reconfiguring 3D Mesh...");
                }

                android.widget.Toast.makeText(this, "Equipping: " + itemName, android.widget.Toast.LENGTH_SHORT).show();
                
                // 🧠 MODULE 4 INTEGRATION: Dynamic Mesh Switching vs Property Manipulation
                if (hexColorCommand != null) {
                    // It's a nested component! Apply Material Tint.
                    renderingStageWindow.evaluateJavascript("window.changeComponentColor('" + category.toLowerCase() + "', '" + hexColorCommand + "');", null);
                    
                    // Blueprint Rule 7 (Animation Sync): Track the selection payload for Firebase Sync
                    com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.INSTANCE.queueCustomizationUpdate(category.toLowerCase(), hexColorCommand);
                } else {
                    // It's a Root VRM File Swap!
                    String relativeUri = (assetPathOverride != null) ? assetPathOverride : itemName;
                    prefs.edit().putString("equipped_vrm_string", relativeUri).apply();
                    
                    // Apply visual proxy integer for backward compatibility
                    prefs.edit().putInt("equipped_avatar_id", Math.abs(itemName.hashCode())).apply();
                    
                    String absoluteVRM = "https://jfindme.local/" + relativeUri;
                    renderingStageWindow.evaluateJavascript("window.loadModel('" + absoluteVRM + "');", null);
                    
                    com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.INSTANCE.queueCustomizationUpdate("avatar_root", itemName);
                }
            }
        });

        // 🧱 QA Fix: Force One-Touch Deployment & Enter Key Logic (L7 Standard)
        applyAccessibility(row, cleanName);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                row.performClick();
                return true;
            }
            return false;
        });
        
        return row;
    }

    private void saveCustomizationPayload() {
        // Enforce final name sync and hide keyboard
        String finalName = nameEntryField.getText().toString().trim();
        nameEntryField.clearFocus();
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(nameEntryField.getWindowToken(), 0);
        
        // Validation Guard (Enterprise Standard)
        if (finalName.isEmpty() || finalName.length() < 2) {
            android.widget.Toast.makeText(this, "INVALID IDENTITY: Name too short.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.jdroidx.jfindme.utils.AngelLoader.setStatus(this, "Broadcasting Identity Matrix...");

        // 🚀 ATOMIC RIPPLE: Execute the 8-Pillar Sync via ProfileViewModel (ignoreValidation = true for avatar/identity quick customization)
        profileViewModel.verifyAndSaveProfile(
            this,
            finalName, 
            prefs.getString("user_phone", ""),
            prefs.getString("user_dob", ""),
            0, 
            prefs.getString("user_gender", "Other"),
            false,
            prefs.getString("user_pincode", ""),
            prefs.getString("user_country", ""),
            prefs.getString("user_state", ""),
            prefs.getString("user_city", ""),
            "None", "None", 
            prefs.getString("user_address", ""),
            prefs.getString("user_emergency_note", ""),
            prefs.getString("org_name", ""),
            prefs.getString("department", ""),
            getSharedPreferences("system_registry", MODE_PRIVATE),
            true
        );
    }

    @Override
    protected void setupLogic() {
        profileViewModel = new androidx.lifecycle.ViewModelProvider(this).get(ProfileViewModel.class);
        
        // 🚀 REACTIVE OBSERVER: Listen for the high-fidelity commit ACK
        profileViewModel.getSaveSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                com.jdroidx.jfindme.utils.AvatarLoader.clearCache(); // 🚀 Closed-Loop Sync: Evict bitmap cache so all screens reflect new avatar immediately
                com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                android.widget.Toast.makeText(this, "🛡️ Identity Synchronized Successfully.", android.widget.Toast.LENGTH_LONG).show();
                profileViewModel.resetSaveState();
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else if (Boolean.FALSE.equals(success)) {
                com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                android.widget.Toast.makeText(this, "🚨 SYNC FAILURE: Cloud Matrix unstable.", android.widget.Toast.LENGTH_SHORT).show();
                profileViewModel.resetSaveState();
            }
        });

        SystemLogger.i(TAG, "Avatar Customizer Workspace Booted.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nameEntryField != null) {
            String val = nameEntryField.getText().toString().trim();
            if (!val.isEmpty()) {
                String existing = prefs.getString("user_nickname", "");
                if (!val.equals(existing)) {
                    prefs.edit().putString("user_nickname", val).apply();
                    com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.INSTANCE.queueCustomizationUpdate("display_name", val);
                    SystemLogger.i(TAG, "onPause: Flushed uncommitted nickname to preferences and sync engine: " + val);
                }
            }
        }
    }
}
