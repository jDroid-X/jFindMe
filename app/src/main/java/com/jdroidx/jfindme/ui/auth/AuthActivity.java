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

        root.addView(createHelpIcon());

        return root;
    }

    private View createHelpIcon() {
        ImageView helpIcon = new ImageView(this);
        helpIcon.setImageResource(android.R.drawable.ic_menu_help);
        helpIcon.setColorFilter(getTextColor());
        helpIcon.setPadding(40, 40, 40, 40);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(150, 150);
        params.gravity = Gravity.TOP | Gravity.END;
        helpIcon.setLayoutParams(params);
        helpIcon.setOnClickListener(v -> showHelpDialog());
        return helpIcon;
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Enterprise Access Guide")
            .setMessage("1. New User? Use traditional login with a unique nickname.\n" +
                       "2. Already have a device? Use 'DEVICE PAIRING' below to sync instantly.\n" +
                       "3. Themes: All 7 enterprise themes are synchronized across devices.")
            .setPositiveButton("UNDERSTOOD", null)
            .show();
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
        welcome.setText("WELCOME TO JFINDME");
        welcome.setTextColor(getGradientOverlayText());
        welcome.setTextSize(22f);
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
        card.setPadding(40, 60, 40, 60);
        applySolidCardGeometry(card, getCardColor(), 40f); // Floats beautifully on the native grey framework

        TextView title = new TextView(this);
        title.setText("Identity Authorization");
        title.setTextColor(getTextColor());
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(20, 0, 0, 50);
        card.addView(title);

        etNickname = createField("Identity Nickname (e.g. Rahul_P1)");
        etNickname.setSingleLine(true);
        etNickname.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        etNickname.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        card.addView(etNickname);

        etEmail = createField("Enterprise Email");
        etEmail.setSingleLine(true);
        etEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        card.addView(etEmail);

        etPass = createField("Secure Password");
        etPass.setSingleLine(true);
        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPass.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        
        // 🧠 QA Fix: Password Visibility Eye Toggle
        etPass.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_view, 0);
        etPass.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPass.getRight() - etPass.getCompoundDrawables()[2].getBounds().width() - 80)) {
                    int currentType = etPass.getInputType();
                    if (currentType == (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        etPass.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_secure, 0);
                    } else {
                        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        etPass.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_view, 0);
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
                    viewModel.login(email, pass);
                }
                return true;
            }
            return false;
        });
        card.addView(etPass);

        card.addView(createIconButton("💡 SUGGEST RANDOM LOGIN", android.R.drawable.ic_dialog_info, v -> {
            String hwId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            if (hwId != null && hwId.length() >= 4) {
                String shortHw = hwId.substring(0, 4).toLowerCase();
                etNickname.setText("Agent_" + shortHw.toUpperCase());
                etEmail.setText("node_" + shortHw + "@jfindme.net");
                etPass.setText("PassNode" + shortHw + "!");
                Toast.makeText(this, "Generated Unique Device Identity!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Hardware Node ID missing. Please type manually.", Toast.LENGTH_SHORT).show();
            }
        }));

        View authBtn = createIconButton("AUTHORIZE ACCESS", android.R.drawable.ic_lock_lock, v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String nick = etNickname.getText().toString().trim();
            
            if (nick.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Required fields missing", Toast.LENGTH_SHORT).show();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Invalid Email Format");
                Toast.makeText(this, "Invalid Email Protocol", Toast.LENGTH_SHORT).show();
            } else if (pass.length() < 6) {
                etPass.setError("Must be 6+ Characters");
                Toast.makeText(this, "Password too short for Firebase", Toast.LENGTH_SHORT).show();
            } else {
                viewModel.login(email, pass);
            }
        });
        
        // 🧠 Dynamic Matrix: Inject the Active Linear Gradient immediately into the Primary Call to Action
        android.graphics.drawable.GradientDrawable authBg = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{getGradientStart(), getGradientEnd()} // Native Database Call!
        );
        authBg.setCornerRadius(50f);
        authBtn.setBackground(authBg);
        authBtn.setElevation(15f); // Hardware Button Pop!
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150);
        btnParams.setMargins(0, 40, 0, 40);
        authBtn.setLayoutParams(btnParams);
        
        card.addView(authBtn);

        card.addView(createIconButton("DEVICE PAIRING (SYNC)", android.R.drawable.ic_menu_share, v -> {
            startActivity(new Intent(this, GroupActivity.class));
        }));

        card.addView(createIconButton("TOGGLE THEME", android.R.drawable.ic_menu_manage, v -> {
            currentThemeId = (currentThemeId + 1) % 9; // 🚀 Extended safely to capture all 9 Arrays (System Included)!
            prefs.edit().putInt("theme_id", currentThemeId).apply();
            
            android.widget.ProgressBar pb = new android.widget.ProgressBar(this);
            pb.setPadding(0, 40, 0, 40);
            
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Cycling System Colors")
                .setMessage("Applying UI Pattern Configuration...")
                .setCancelable(false)
                .setView(pb)
                .show();
                
            new android.os.Handler().postDelayed(() -> {
                dialog.dismiss();
                startActivity(new android.content.Intent(this, AuthActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }, 2000); 
        }));

        wrapper.addView(card);
        scroll.addView(wrapper);
        return scroll;
    }

    private EditText createField(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(getSecondaryTextColor()); // Bound exclusively to DB Values!
        et.setTextColor(getTextColor());
        et.setPadding(40, 40, 40, 40);
        et.setGravity(Gravity.CENTER);
        
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
        
        // 🧠 QA Fix: Intelligent Routing Logic
        // Checks if this physical device has previously assembled an Avatar Profile.
        boolean isReturningVeteran = prefs.getBoolean("profile_onboarded", false);
        
        if (isReturningVeteran) {
            // Drop straight into the Live Map!
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.map.MapActivity.class));
        } else {
            // First time boot -> Route immediately to Profile Customization Workspace
            startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
        }
        
        finish();
    }
}
