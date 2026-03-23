package com.jdroidx.jfindme.ui.management;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jdroidx.jfindme.ui.base.BaseActivityJava;

import org.json.JSONObject;

/**
 * 📝 Phase 6 Diagnostics & Auditing
 * SystemLogActivity: Provides Administrators extreme transparency into the background 
 * memory states, configurations, and cache mapping currently driving the device logic!
 */
public class SystemLogActivity extends BaseActivityJava {

    private SharedPreferences prefs;

    @Override
    protected View createLayout() {
        prefs = getSharedPreferences("jFindMe_Prefs", MODE_PRIVATE);

        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // 1. HERO BANNER
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView title = new TextView(this);
        title.setText("SYSTEM AUDIT LOG");
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Device Configuration & Cache Diagnostics");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        header.addView(subtitle);
        
        mainRoot.addView(header);

        // 2. SCROLLING CATEGORY MENU
        ScrollView scrollView = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(40, 40, 40, 100);

        // Inject the Diagnostic Categories
        listContainer.addView(createDiagnosticItem("Device UUID Identity", "Core Authentication & Firebase Node Matrix", () -> dumpDeviceIdentity()));
        listContainer.addView(createDiagnosticItem("Role & Relational Cache", "Checkbox Truth Table for Mother/Father/Child Rules", () -> dumpRoleCache()));
        listContainer.addView(createDiagnosticItem("Active Master Group", "Currently Enrolled P2P Network Hash", () -> dumpGroupCache()));
        listContainer.addView(createDiagnosticItem("UI Theme & Aesthetics", "Current Global Hex Variable Assignments", () -> dumpThemeCache()));
        listContainer.addView(createDiagnosticItem("Security Settings", "Advanced Application Defense Lock States", () -> dumpSecurityCache()));

        listContainer.addView(createIconButton("RETURN TO DASHBOARD", android.R.drawable.ic_menu_revert, v -> finish()));

        scrollView.addView(listContainer);
        mainRoot.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainRoot;
    }

    @Override
    protected void setupLogic() {
        // Static Architecture, Logic is handled dynamically inside the lambdas.
    }

    // ==========================================
    // DIAGNOSTIC ENGINES & UI BUILDERS
    // ==========================================

    private View createDiagnosticItem(String catTitle, String desc, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(40, 40, 40, 40);
        applySolidCardGeometry(row, getCardColor(), 20f);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 30);
        row.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(catTitle);
        tvTitle.setTextColor(getGradientStart()); // Themes Dynamically!
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, Typeface.BOLD);
        row.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextColor(getSecondaryTextColor());
        tvDesc.setTextSize(12);
        tvDesc.setPadding(0, 10, 0, 0);
        row.addView(tvDesc);

        row.setFocusable(true);
        row.setClickable(true);
        row.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                row.performClick(); return true;
            } return false;
        });
        row.setOnClickListener(v -> onClick.run());

        return row;
    }

    private void showAuditModal(String title, JSONObject dump) {
        try {
            String prettyJson = dump.toString(4); // Indent precisely for Readability!
            new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(prettyJson)
                .setPositiveButton("Close Diagnostic", null)
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- DATA CAPTURE HOOKS ---

    private void dumpDeviceIdentity() {
        try {
            JSONObject json = new JSONObject();
            json.put("Firebase_UID", com.google.firebase.auth.FirebaseAuth.getInstance().getUid() != null ? com.google.firebase.auth.FirebaseAuth.getInstance().getUid() : "Null/Ghost State");
            json.put("Hardware_ID", prefs.getString("hardware_id", "Not Assigned"));
            json.put("User_Nickname", prefs.getString("user_nickname", "Unknown"));
            json.put("User_Phone", prefs.getString("user_phone", "No Phone Linked"));
            json.put("Profile_Onboarded", prefs.getBoolean("profile_onboarded", false));
            showAuditModal("Identity Engine Trace", json);
        } catch (Exception ignored) {}
    }

    private void dumpRoleCache() {
        try {
            JSONObject json = new JSONObject();
            json.put("Role_Mother", prefs.getBoolean("role_mother", false));
            json.put("Role_Father", prefs.getBoolean("role_father", false));
            json.put("Role_Child", prefs.getBoolean("role_child", false));
            json.put("Role_Spouse", prefs.getBoolean("role_spouse", false));
            json.put("Role_Friends", prefs.getBoolean("role_friends", false));
            showAuditModal("Role & Relational Truths", json);
        } catch (Exception ignored) {}
    }

    private void dumpGroupCache() {
        try {
            JSONObject json = new JSONObject();
            json.put("Active_Group_ID", prefs.getString("active_group", "Orphaned"));
            json.put("Last_Scanned_Target", prefs.getString("last_scanned_target", "None"));
            showAuditModal("P2P Array Diagnostics", json);
        } catch (Exception ignored) {}
    }

    private void dumpThemeCache() {
        try {
            JSONObject json = new JSONObject();
            json.put("Active_Theme_Index", prefs.getInt("theme_index", 0));
            json.put("Legacy_Map_Rendering", false); // Hardcoded since deprecated
            showAuditModal("Aesthetic Engine Configuration", json);
        } catch (Exception ignored) {}
    }
    
    private void dumpSecurityCache() {
        try {
            JSONObject json = new JSONObject();
            json.put("Strict_Tracking_Enforced", prefs.getBoolean("strict_tracking", false));
            json.put("Two_Factor_Auth_Enabled", prefs.getBoolean("two_factor", false));
            json.put("Biometric_Signature", "Bypassed (Phase 5)");
            showAuditModal("Defense Lock Constraints", json);
        } catch (Exception ignored) {}
    }
}
