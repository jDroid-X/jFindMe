package com.jdroidx.jfindme.ui.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.map.MapActivity;
import com.jdroidx.jfindme.ui.group.GroupActivity;
import com.jdroidx.jfindme.utils.SystemLogger;
import android.view.KeyEvent;

/**
 * Main -> UI Engine -> AuthScreen -> AuthActivity
 * Level 5: Redone for Enterprise Sync & Multi-Device Pairing.
 * Supports: Traditional Login, Theme Switching, and Fast Pairing Link.
 */
public class AuthActivity extends BaseActivityJava {

    private AuthViewModel viewModel;
    private final String TAG = "AuthActivity";
    private EditText etEmail, etPass, etNickname;
    private String cachedNick = "", cachedEmail = "", cachedPass = ""; 
    private android.app.AlertDialog onboardingDialog;



    @Override
    protected View createLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getPrimaryColor());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(isLandscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        
        if (isLandscape) {
            content.addView(createImagePane(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.4f));
            content.addView(createLoginPane(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.6f));
        } else {
            // 🚀 L7 Architecture Sync: Branding on Top for Portrait!
            content.addView(createImagePane(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(createLoginPane(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        }
        root.addView(content);

        return root;
    }

    private View createImagePane() {
        LinearLayout pane = new LinearLayout(this);
        pane.setGravity(Gravity.CENTER);
        pane.setOrientation(LinearLayout.VERTICAL);
        int vPad = isLandscape ? scale(40) : scale(100); 
        pane.setPadding(0, vPad, 0, vPad);
        applyLinearGradient(pane, 0f); // Securely renders the Active Hero Gradient across the Launch Header!

        ImageView logo = new ImageView(this);
        int butterflyId = getResources().getIdentifier("butterfly", "drawable", getPackageName());
        if (butterflyId != 0) {
            logo.setImageResource(butterflyId);
        } else {
            logo.setImageResource(android.R.drawable.ic_dialog_map);
            logo.setColorFilter(getGradientOverlayText());
        }
        
        int heroSize = isLandscape ? scale(180) : scale(250); 
        pane.addView(logo, new LinearLayout.LayoutParams(heroSize, heroSize));
        
        // 🚀 L7 Animation: Waving Wings Protocol (ScaleX Oscillation)
        android.animation.ObjectAnimator wings = android.animation.ObjectAnimator.ofFloat(logo, "scaleX", 1f, 0.7f);
        wings.setDuration(1200); // Rhythmic enterprise tempo
        wings.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        wings.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        wings.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        wings.start();
        
        TextView welcome = new TextView(this);
        welcome.setText("ButterFly\nSecure\nGateWay"); // 3 Words | 3 Rows
        welcome.setGravity(Gravity.CENTER);
        welcome.setTextColor(getGradientOverlayText());
        welcome.setTextSize(isLandscape ? scaleText(18) : scaleText(24)); 
        welcome.setTypeface(null, android.graphics.Typeface.BOLD);
        welcome.setPadding(0, isLandscape ? scale(20) : scale(40), 0, 0);
        pane.addView(welcome);
        
        return pane;
    }

    private View createLoginPane() {
        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false); 
        scroll.setClipToPadding(false);
        
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);
        
        int topPad = isLandscape ? scale(20) : scale(30);
        wrapper.setPadding(getResponsivePaddingQuota(), topPad, getResponsivePaddingQuota(), scale(100));
 
        // --- THE MASTER LOGIN CARD ---
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        
        int cardPadv = scale(60);
        card.setPadding(getResponsivePaddingQuota(), cardPadv, getResponsivePaddingQuota(), cardPadv);
        
        // 🚀 L7 Architecture Sync: Borderless Corporate Card Hub
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setColor(getCardColor());
        cardBg.setCornerRadius(scaleRadius(24)); 
        card.setBackground(cardBg);
        card.setElevation(15f); 
        card.setTranslationZ(20f); 
        card.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        card.setClipToOutline(true); // 🚀 QA Fix: Master Shadow Activation!

        TextView title = new TextView(this);
        title.setText("jButterFly Authentication");
        title.setTextColor(getTextColor());
        title.setTextSize(scaleText(22));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(scale(10), 0, 0, scale(10));
        card.addView(title);
        
        TextView subTitle = new TextView(this);
        subTitle.setText("Secure gateway access for active agents.");
        subTitle.setTextColor(getSecondaryTextColor());
        subTitle.setTextSize(scaleText(12));
        subTitle.setPadding(scale(10), 0, 0, scale(50));
        card.addView(subTitle);

        etNickname = UiArchitect.createInputField(this, "FIELD_IDENTITY");
        card.addView(etNickname);

        etEmail = UiArchitect.createInputField(this, "FIELD_EMAIL");
        card.addView(etEmail);

        etPass = UiArchitect.createInputField(this, "FIELD_PASSWORD");
        
        // Eye Toggle Implementation
        etPass.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_idle_lock, 0, android.R.drawable.ic_menu_view, 0);
        etPass.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPass.getRight() - etPass.getCompoundDrawables()[2].getBounds().width() - 80)) {
                    int currentType = etPass.getInputType();
                    if (currentType == (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        etPass.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_idle_lock, 0, android.R.drawable.ic_secure, 0);
                    } else {
                        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        etPass.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_idle_lock, 0, android.R.drawable.ic_menu_view, 0);
                    }
                    etPass.setSelection(etPass.getText().length());
                    return true;
                }
            }
            return false;
        });
        
        etPass.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                String email = etEmail.getText().toString().trim();
                String pass = etPass.getText().toString().trim();
                if (!email.isEmpty() && !pass.isEmpty()) {
                    viewModel.login(email, pass, etNickname.getText().toString().trim());
                }
                return true;
            }
            return false;
        });
        card.addView(etPass);

        // --- PRIMARY CTA ---
        // 🧪 UI Calibrator: Downscale text for 100% visibility in L7 Matrix
        View authBtn = createIconButton("AUTHENTICATE SYSTEM", android.R.drawable.ic_menu_send, v -> {
            String nick = etNickname.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            
            if (nick.length() < 2) {
                etNickname.setError("Valid Alias Required (Min 2 chars)");
                Toast.makeText(this, "Auth Failed: Missing Identity Alias", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Invalid Email Format");
                Toast.makeText(this, "Auth Failed: Malformed Security Protocol", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pass.length() < 6) {
                etPass.setError("6+ characters required");
                Toast.makeText(this, "Auth Failed: Weak Authentication Matrix", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 🚀 Finalizing Secure Handshake...
            com.jdroidx.jfindme.utils.AngelLoader.show(this);
            viewModel.login(email, pass, nick);
        });

        // 🧱 Force Tactical Scaling Hub
        if (authBtn instanceof android.view.ViewGroup) {
             android.view.ViewGroup vg = (android.view.ViewGroup) authBtn;
             for (int i = 0; i < vg.getChildCount(); i++) {
                 android.view.View child = vg.getChildAt(i);
                 if (child instanceof android.widget.TextView) {
                     ((android.widget.TextView) child).setTextSize(scaleText(12)); 
                     child.setPadding(0, 0, 0, 0); // Purge excessive padding
                 }
             }
        }
        
        android.graphics.drawable.GradientDrawable authBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{getGradientStart(), getGradientEnd()} 
        );
        authBg.setCornerRadius(scaleRadius(16)); // Standard L7 Geometry
        authBtn.setBackground(authBg);
        authBtn.setElevation(8f); 
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scale(120));
        btnParams.setMargins(0, scale(30), 0, scale(20));
        authBtn.setLayoutParams(btnParams);
        card.addView(authBtn);

        // --- ENTERPRISE FOOTER ARRAY (Secondary Actions) ---
        LinearLayout footerArray = new LinearLayout(this);
        footerArray.setOrientation(LinearLayout.HORIZONTAL);
        footerArray.setGravity(Gravity.CENTER);
        footerArray.setWeightSum(3.0f);
        footerArray.setPadding(0, 20, 0, 0);

        // Utility: Factory for simple text action buttons
        class ActionBuilder {
            TextView build(String txt, View.OnClickListener listener) {
                TextView tv = new TextView(AuthActivity.this);
                tv.setText(txt);
                tv.setTextColor(getGradientOverlayText()); // 🚀 QA Fix: Force bright contrast over colorful matrix!
                tv.setTextSize(11);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(5, 30, 5, 30);
                
                android.graphics.drawable.GradientDrawable itemBg = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{getGradientStart(), getGradientEnd()} 
                );
                itemBg.setCornerRadius(16f);
                tv.setBackground(itemBg);
                tv.setElevation(8f);
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                lp.setMargins(10, 0, 10, 0); // Isolate geometry as highly independent buttons
                tv.setLayoutParams(lp);
                
                tv.setOnClickListener(listener);
                tv.setFocusable(true);
                tv.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == android.view.KeyEvent.ACTION_UP && 
                        (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
                        v.performClick();
                        return true;
                    }
                    return false;
                });
                applyAccessibility(tv, null);
                return tv;
            }
        }
        ActionBuilder builder = new ActionBuilder();

        footerArray.addView(builder.build("AUTO SETUP", v -> performAutoSetup()));

        footerArray.addView(builder.build("SYNC DEVICE", v -> {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
                String email = etEmail.getText().toString().trim();
                String pass = etPass.getText().toString().trim();
                
                if (email.isEmpty() || pass.isEmpty()) {
                    new AlertDialog.Builder(this)
                        .setTitle("Authentication Required")
                        .setMessage("You must explicitly Authorise your User ID via the Login Gateway first before linking to external hardware.")
                        .setPositiveButton("UNDERSTOOD", null)
                        .show();
                } else {
                    viewModel.login(email, pass, etNickname.getText().toString().trim());
                }
            } else {
                // 🚀 QA Fix: Implicit Guard! Even if logged in permanently via Cache, absolutely verify their physical Profile Array before granting Gateway Access!
                boolean isReturningVeteran = prefs.getBoolean("profile_onboarded", false);
                if (!isReturningVeteran) {
                    Toast.makeText(this, "Profile Missing! Rerouting to Identity Setup...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
                } else {
                    startActivity(new Intent(this, GroupActivity.class));
                }
            }
        }));

        footerArray.addView(builder.build("UI THEME", v -> {
            currentThemeId = (currentThemeId + 1) % 9; 
            prefs.edit().putInt("theme_id", currentThemeId).apply();
            startActivity(new android.content.Intent(this, AuthActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }));

        card.addView(footerArray);
        wrapper.addView(card);
        scroll.addView(wrapper);
        return scroll;
    }



    private EditText createField(String hint, int iconRes) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(getSecondaryTextColor()); // Bound exclusively to DB Values!
        et.setTextColor(getTextColor());
        et.setPadding(40, 40, 40, 40);
        et.setGravity(Gravity.CENTER_VERTICAL); // Align text flush with icon
        et.setCompoundDrawablePadding(20); // Cushion between icon and text vector
        et.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0); // Native Icon Implementation

        // 🚀 Dynamic Thematic Boxing Overlay
        android.graphics.drawable.GradientDrawable fieldBg = new android.graphics.drawable.GradientDrawable();
        fieldBg.setColor(Color.parseColor("#15000000")); // Ultra-soft transparent 8% overlay. Safely darkens White Theme boxes / brightens Dark boxes!
        fieldBg.setStroke(3, getGradientStart()); // Native Component Stroke matches specific app Theme (Pink, Blue, Red, etc)!
        fieldBg.setCornerRadius(16f);
        et.setBackground(fieldBg);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = scale(15);
        lp.setMargins(0, margin, 0, margin);
        et.setLayoutParams(lp);
        return et;
    }

    private void performAutoSetup() {
        String hwId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if (hwId == null || hwId.length() < 4) hwId = "AGENT_NODE_L7";
        
        // 🚀 Legacy Re-Indexing: Extracting FIRST 4 digits as per original protocol
        String shortHw = hwId.substring(0, 4).toLowerCase();
        
        // 🚀 Legacy Identity Restoration
        etNickname.setText("Agent_" + shortHw.toUpperCase());
        etEmail.setText("node_" + shortHw + "@jfindme.net");
        etPass.setText("PassCode" + shortHw + "!");
        
        Toast.makeText(this, "Enterprise Encrypted Identity Array Injected", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // 🚀 QA Fix: Master Onboarding Interceptor (Selective Re-Integration)
        // If the Identity cache is empty (Fresh Start/First Installation), guide the agent toward the Encrypted Matrix immediately!
        // 🚀 QA Fix: Global Identity Check
        // If the fields are already populated (Portrait -> Landscape sync), do NOT overlap the dialog!
        if (etNickname.getText().toString().isEmpty() && etEmail.getText().toString().isEmpty() && 
            prefs.getString("user_nickname", "").isEmpty()) {
            
            if (onboardingDialog != null && onboardingDialog.isShowing()) {
                onboardingDialog.dismiss();
            }
            
            onboardingDialog = new android.app.AlertDialog.Builder(this)
                .setTitle("🚀 SECURE ONBOARDING")
                .setMessage("No Identity Node detected for this hardware. Please use 'AUTO SETUP' for a quick, Encrypted login.")
                .setPositiveButton("AUTO SETUP", (d, w) -> {
                    performAutoSetup();
                    onboardingDialog = null;
                })
                .setNegativeButton("EXIT", (d, w) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
        }
        


        viewModel.getUser().observe(this, user -> {
            if (user != null) {
                // 🚀 QA Fix: Ported Ephemeral Toast to a Persistent Enterprise Handshake Alert!
                finalizeLogin(user, user.getUid());
            }
        });

        viewModel.isLoading().observe(this, loading -> {
            if (isDestroyed() || isFinishing()) return;
            if (loading) {
                com.jdroidx.jfindme.utils.AngelLoader.show(this);
            } else {
                com.jdroidx.jfindme.utils.AngelLoader.hide(this);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (isDestroyed() || isFinishing()) return;
            if (error != null) {
                Toast.makeText(this, "AUTH FAILURE: " + error, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    private void finalizeLogin(com.google.firebase.auth.FirebaseUser user, String uid) {
        if (isDestroyed() || isFinishing()) return;
        
        final String name = etNickname.getText().toString().trim();
        SystemLogger.INSTANCE.i(TAG, "User Authenticated. Hardware Locked: " + uid);
        
        prefs.edit().putBoolean("is_fresh_install_p2p_wipe", false).apply();
        prefs.edit().putBoolean("route_to_sync", false).apply();

        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("users").child(uid).get()
            .addOnCompleteListener(task -> {
                if (isDestroyed() || isFinishing()) return;
                com.jdroidx.jfindme.utils.AngelLoader.hide(this);
                
                // 🚀 Sync Protocol: Use the local nickname (Agent_XXXX) unless it's empty, in which case we fallback to Cloud!
                String finalInjectedName = name.isEmpty() ? "Resident Node" : name;
                String cloudGroup = null, linkedCsv = null;
                
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    com.google.firebase.database.DataSnapshot snap = task.getResult();
                    cloudGroup = snap.child("active_group").getValue(String.class);
                    linkedCsv = snap.child("valid_linked_uids").getValue(String.class);
                    
                    // If we locally selected the generic 'Resident Node', check for a custom Cloud name!
                    if (finalInjectedName.equals("Resident Node")) {
                        String fetchedName = snap.child("userName").getValue(String.class);
                        if (fetchedName != null && !fetchedName.isEmpty()) finalInjectedName = fetchedName;
                    }
                }
                
                prefs.edit().putString("user_nickname", finalInjectedName).apply();
                if (cloudGroup != null) prefs.edit().putString("active_group", cloudGroup).apply();
                if (linkedCsv != null) prefs.edit().putString("linked_devices_csv", linkedCsv).apply();
                
                SystemLogger.INSTANCE.i(TAG, "Authorization Success. Redirecting to Profile Screen.");
                Toast.makeText(this, "Session Verified. Finalizing Dossier...", Toast.LENGTH_SHORT).show();
                
                // 🧱 QA Fix: Post-Transition Buffer (150ms)
                // Prevents 'No registered input channel' errors during rapid screen hopping!
                getWindow().getDecorView().postDelayed(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
                        finish();
                    }
                }, 150);
            });
    }

    @Override
    protected void onSaveCurrentState() {
        if (etNickname != null) cachedNick = etNickname.getText().toString();
        if (etEmail != null) cachedEmail = etEmail.getText().toString();
        if (etPass != null) cachedPass = etPass.getText().toString();
    }

    @Override
    protected void onUiRebuilt() {
        if (etNickname != null) etNickname.setText(cachedNick);
        if (etEmail != null) etEmail.setText(cachedEmail);
        if (etPass != null) etPass.setText(cachedPass);
        
        // 🚀 L7 Optimization: Re-Calibrate Keyboard focus if needed
        if (!cachedNick.isEmpty() && etPass != null) {
            etPass.requestFocus();
        }
    }

    @Override
    protected void onStop() {
        if (onboardingDialog != null && onboardingDialog.isShowing()) {
            onboardingDialog.dismiss();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (onboardingDialog != null && onboardingDialog.isShowing()) {
            onboardingDialog.dismiss();
        }
        com.jdroidx.jfindme.utils.AngelLoader.hide(this); // Force hide global loader
        super.onDestroy();
    }
}
