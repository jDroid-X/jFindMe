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
        pane.setPadding(60, 100, 60, 100);
        pane.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("jFindMe Enterprise");
        title.setTextColor(getTextColor());
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 60);
        pane.addView(title);

        EditText etEmail = createField("Enter Email Address");
        pane.addView(etEmail);

        EditText etPass = createField("Enter Secure Password");
        pane.addView(etPass);

        // Rule: Icons with text in place of Buttons
        View btnLogin = createIconButton("AUTHORIZE ACCESS", android.R.drawable.ic_lock_lock, v -> {
            SystemLogger.INSTANCE.i(TAG, "Auth request initiated via Python Data Bridge");
            viewModel.login(etEmail.getText().toString(), etPass.getText().toString());
        });
        pane.addView(btnLogin);

        // Icon for Theme Switcher (7 Themes)
        View btnTheme = createIconButton("SWITCH THEME", android.R.drawable.ic_menu_manage, v -> {
            currentThemeId = (currentThemeId + 1) % 7;
            prefs.edit().putInt("theme_id", currentThemeId).apply();
            recreate();
        });
        btnTheme.setAlpha(0.7f);
        pane.addView(btnTheme);

        return pane;
    }

    private EditText createField(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(Color.LTGRAY);
        et.setTextColor(getTextColor());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 20, 0, 20);
        et.setLayoutParams(lp);
        return et;
    }

    @Override
    protected void setupLogic() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        viewModel.getUser().observe(this, user -> {
            if (user != null) {
                SystemLogger.INSTANCE.i(TAG, "Access Granted. Redirecting to Map.");
                startActivity(new Intent(this, MapActivityJava.class));
                finish();
            }
        });
    }
}
