package com.jdroidx.jfindme.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.map.MapActivityJava;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> AuthScreen -> AuthActivityJava
 * Level 5: Implementation of Programmatic Dual-Pane Auth UI.
 * Rules: Java Layout, 7 Themes, No Buttons, Python Data Integration.
 */
public class AuthActivityJava extends BaseActivityJava {

    private AuthViewModel viewModel;
    private final String TAG = "AuthActivity_Enterprise";
    private static final int ID_LEFT_PANE = View.generateViewId();
    private static final int ID_RIGHT_PANE = View.generateViewId();

    @Override
    protected View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(isLandscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.setBackgroundColor(getPrimaryColor());

        if (isLandscape) {
            // Level 4: LayoutSplitter Implementation
            root.addView(createImagePane(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.4f));
            root.addView(createLoginPane(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.6f));
        } else {
            root.addView(createLoginPane(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        return root;
    }

    private View createImagePane() {
        LinearLayout pane = new LinearLayout(this);
        pane.setId(ID_LEFT_PANE);
        pane.setGravity(Gravity.CENTER);
        pane.setBackgroundColor(Color.parseColor("#11000000")); // Contrast shade

        ImageView logo = new ImageView(this);
        logo.setImageResource(android.R.drawable.ic_dialog_map);
        pane.addView(logo, new LinearLayout.LayoutParams(200, 200));
        
        return pane;
    }

    private View createLoginPane() {
        LinearLayout pane = new LinearLayout(this);
        pane.setId(ID_RIGHT_PANE);
        pane.setOrientation(LinearLayout.VERTICAL);
        int authPad = getResponsivePaddingQuota() * 2;
        pane.setPadding(authPad, authPad * 2, authPad, authPad * 2);
        pane.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("jFindMe Enterprise");
        title.setTextColor(getTextColor());
        title.setTextSize(24);
        title.setPadding(0, 0, 0, getResponsivePaddingQuota() * 2);
        pane.addView(title);

        // Level 6: Enterprise Matrix Integration
        EditText etEmail = com.jdroidx.jfindme.ui.base.UiArchitect.createInputField(this, "FIELD_EMAIL");
        pane.addView(etEmail);

        EditText etPass = com.jdroidx.jfindme.ui.base.UiArchitect.createInputField(this, "FIELD_PASSWORD");
        pane.addView(etPass);

        // Rule: Structured CTAs replacing legacy icon buttons
        View btnLogin = com.jdroidx.jfindme.ui.base.UiArchitect.createButton(this, "BTN_AUTH", v -> {
            SystemLogger.INSTANCE.i(TAG, "Auth request initiated via Python Data Bridge");
            viewModel.login(etEmail.getText().toString(), etPass.getText().toString());
        });
        pane.addView(btnLogin);

        // Secondary Action for Theme Switcher
        View btnTheme = com.jdroidx.jfindme.ui.base.UiArchitect.createButton(this, "BTN_SECONDARY", v -> {
            currentThemeId = (currentThemeId + 1) % 7;
            prefs.edit().putInt("theme_id", currentThemeId).apply();
            safeRecreate();
        });
        pane.addView(btnTheme);

        return pane;
    }

    private void performAutoSetup() {
        android.widget.Toast.makeText(this, "Enterprise Encrypted Identity Array Injected", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        if (getIntent().getBooleanExtra("TRIGGER_AUTO_SETUP", false)) {
            performAutoSetup();
        }

        viewModel.getUser().observe(this, user -> {
            if (user != null) {
                SystemLogger.INSTANCE.i(TAG, "Access Granted. Redirecting to Profile Dossier.");
                startActivity(new Intent(this, com.jdroidx.jfindme.ui.settings.ProfileActivity.class));
                finish();
            }
        });
    }
}
