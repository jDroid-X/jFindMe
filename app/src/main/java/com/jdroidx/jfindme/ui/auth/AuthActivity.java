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
import com.jdroidx.jfindme.ui.map.MapActivity;
import com.jdroidx.jfindme.ui.group.GroupActivity;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> AuthScreen -> AuthActivity
 * Level 5: Redone for Enterprise Sync & Multi-Device Pairing.
 * Supports: Traditional Login, Theme Switching, and Fast Pairing Link.
 */
public class AuthActivity extends BaseActivityJava {

    private AuthViewModel viewModel;
    private final String TAG = "AuthActivity";
    private EditText etEmail, etPass, etNickname;
    private ProgressBar progressBar;



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
            content.addView(createLoginPane(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        root.addView(content);

        // Progress Overlay
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        FrameLayout.LayoutParams pbParams = new FrameLayout.LayoutParams(100, 100);
        pbParams.gravity = Gravity.CENTER;
        root.addView(progressBar, pbParams);

        return root;
    }

    private View createImagePane() {
        LinearLayout pane = new LinearLayout(this);
        pane.setGravity(Gravity.CENTER);
        pane.setOrientation(LinearLayout.VERTICAL);
        pane.setPadding(0, 100, 0, 100);
        applyLinearGradient(pane, 0f); // Securely renders the Active Hero Gradient across the Launch Header!

        ImageView logo = new ImageView(this);
        int m2gId = getResources().getIdentifier("logo_m2g", "drawable", getPackageName());
        if (m2gId != 0) {
            logo.setImageResource(m2gId);
        } else {
            logo.setImageResource(android.R.drawable.ic_dialog_map);
            logo.setColorFilter(getGradientOverlayText());
        }
        
        pane.addView(logo, new LinearLayout.LayoutParams(250, 250));
        
        TextView welcome = new TextView(this);
        welcome.setText("JFINDME SECURE GATEWAY");
        welcome.setTextColor(getGradientOverlayText());
        welcome.setTextSize(24f); 
        welcome.setTypeface(null, android.graphics.Typeface.BOLD);
        welcome.setPadding(0, 50, 0, 0);
        pane.addView(welcome);
        
        return pane;
    }

    private View createLoginPane() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(40, 50, 40, 100);

        // --- THE MASTER LOGIN CARD ---
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(50, 60, 50, 60);
        applySolidCardGeometry(card, getCardColor(), 40f); 

        TextView title = new TextView(this);
        title.setText("System Authentication");
        title.setTextColor(getTextColor());
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(10, 0, 0, 10);
        card.addView(title);
        
        TextView subTitle = new TextView(this);
        subTitle.setText("Secure gateway access for active agents.");
        subTitle.setTextColor(getSecondaryTextColor());
        subTitle.setTextSize(12);
        subTitle.setPadding(10, 0, 0, 50);
        card.addView(subTitle);

        etNickname = createField("Identity Alias", android.R.drawable.ic_menu_myplaces);
        etNickname.setSingleLine(true);
        etNickname.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        etNickname.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        card.addView(etNickname);

        etEmail = createField("Enterprise Email", android.R.drawable.ic_dialog_email);
        etEmail.setSingleLine(true);
        etEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        card.addView(etEmail);

        etPass = createField("Secure Password", android.R.drawable.ic_lock_idle_lock);
        etPass.setSingleLine(true);
        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPass.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        
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
                if (!email.isEmpty() && !pass.isEmpty()) viewModel.login(email, pass);
                return true;
            }
            return false;
        });
        card.addView(etPass);

        // --- PRIMARY CTA ---
        View authBtn = createIconButton("AUTHENTICATE SYSTEM", android.R.drawable.ic_menu_send, v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String nick = etNickname.getText().toString().trim();
            
            if (nick.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Use 'AUTO SETUP' for auto login details.", Toast.LENGTH_LONG).show();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Invalid format");
            } else if (pass.length() < 6) {
                etPass.setError("6+ characters required");
            } else {
                prefs.edit().putBoolean("route_to_sync", false).apply(); // Clear implicit flag
                viewModel.login(email, pass);
            }
        });
        
        android.graphics.drawable.GradientDrawable authBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{getGradientStart(), getGradientEnd()} 
        );
        authBg.setCornerRadius(16f); // Sharper enterprise geometry
        authBtn.setBackground(authBg);
        authBtn.setElevation(8f); 
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 140);
        btnParams.setMargins(0, 50, 0, 40);
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
                return tv;
            }
        }
        ActionBuilder builder = new ActionBuilder();

        footerArray.addView(builder.build("AUTO SETUP", v -> {
            String hwId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            if (hwId != null && hwId.length() >= 4) {
                String shortHw = hwId.substring(0, 4).toLowerCase();
                etNickname.setText("Agent_" + shortHw.toUpperCase());
                etEmail.setText("node_" + shortHw + "@jfindme.net");
                etPass.setText("PassNode" + shortHw + "!");
                Toast.makeText(this, "Auto-Setup Array Injected.", Toast.LENGTH_SHORT).show();
            }
        }));

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
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || pass.length() < 6) {
                    Toast.makeText(this, "Fields filled but invalid format. Please check email and password.", Toast.LENGTH_LONG).show();
                } else {
                    // Fields are perfectly filled! Self-Authorize immediately.
                    Toast.makeText(this, "Self-Authorizing Identity...", Toast.LENGTH_SHORT).show();
                    prefs.edit().putBoolean("route_to_sync", true).apply();
                    viewModel.login(email, pass);
                }
            } else {
                startActivity(new Intent(this, GroupActivity.class));
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
        lp.setMargins(0, 15, 0, 15);
        et.setLayoutParams(lp);
        return et;
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        viewModel.getUser().observe(this, user -> {
            if (user != null) {
                Toast.makeText(this, "Identity Verified! Granting Matrix Entry...", Toast.LENGTH_SHORT).show();
                
                // 🧠 QA Fix: Bypass Database connection hanging. Directly authorize the Node natively.
                String hostHwId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                try {
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("users").child(user.getUid()).child("hw_lock_id")
                        .setValue(hostHwId); // Fire and Forget implicitly
                } catch(Exception e) {
                    SystemLogger.INSTANCE.e(TAG, "Non-fatal background lock trace issue.", e);
                }
                
                // Instantly Transition the UI without waiting for Cloud ACKs
                finalizeLogin(user, user.getUid());
            }
        });

        viewModel.isLoading().observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "AUTH FAILURE: " + error, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    private void finalizeLogin(com.google.firebase.auth.FirebaseUser user, String uid) {
        String name = etNickname.getText().toString().trim();
        prefs.edit().putString("user_nickname", name.isEmpty() ? user.getEmail() : name).apply();
        SystemLogger.INSTANCE.i(TAG, "User Authenticated. Hardware Locked to Session: " + uid);
        
        // 🚀 QA Architecture Override: The Fresh Install "Slate Wipe" Directive
        boolean isFreshInstance = prefs.getBoolean("is_fresh_install_p2p_wipe", true);
        if (isFreshInstance) {
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("users").child(uid).child("active_group").removeValue();
            prefs.edit().putBoolean("is_fresh_install_p2p_wipe", false).apply();
            SystemLogger.INSTANCE.w(TAG, "LEGACY CLOUD DETACHED: Second installation defaulting to pure fresh state.");
        }
        
        // 🧠 QA Fix: Intelligent Routing Logic
        // Checks if this physical device has previously assembled an Avatar Profile.
        boolean isReturningVeteran = prefs.getBoolean("profile_onboarded", false);
        boolean routeToSync = prefs.getBoolean("route_to_sync", false);
        
        // Always reset implicit route!
        prefs.edit().putBoolean("route_to_sync", false).apply();
        
        if (routeToSync) {
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.group.GroupActivity.class));
        } else if (isReturningVeteran) {
            // Drop straight into the Live Map!
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.map.MapActivity.class));
        } else {
            // First time boot -> Route immediately to Profile Customization Workspace
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
        }
        
        finish();
    }
}
