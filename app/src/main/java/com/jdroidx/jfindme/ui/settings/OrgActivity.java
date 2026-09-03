package com.jdroidx.jfindme.ui.settings;

import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;
import java.util.HashMap;
import java.util.Map;

/**
 * ≡ƒÜÇ L7 REVISED: Enterprise Organization Suite
 * Closed-Loop Synchronization with Profile Identity and Cloud Matrix.
 */
public class OrgActivity extends BaseActivityJava {

    private DatabaseHelper db;
    private Map<String, EditText> fieldMap = new HashMap<>();

    @Override
    protected View createLayout() {
        db = DatabaseHelper.getInstance(this);
        int q = getResponsivePaddingQuota();

        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // 🚀 TIER 0: GEOMETRIC HEADER MATRIX
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
        backBtn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        topRow.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("PROFESSIONAL IDENTITY HUB");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        topRow.addView(title);
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Forensic Organization Dossier | Authority: Enterprise");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.75f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        subtitle.setPadding(0, scale(5), 0, 0);
        header.addView(subtitle);

        mainRoot.addView(header);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setClipChildren(false);
        scrollView.setClipToPadding(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(q * 2, q, q * 2, q * 4);
        content.setClipChildren(false);

        // 🔗 CLUSTER 1: EMPLOYMENT DOSSIER
        renderSectionHeader(content, "IDENTITY & EMPLOYMENT DOSSIER");
        LinearLayout identityCard = new LinearLayout(this);
        identityCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, identityCard, "CARD_STANDARD");
        identityCard.setPadding(35, 35, 35, 35);

        String[] identityFields = {
            "org_name|ORGANIZATION NAME", "department|DEPARTMENT", 
            "employee_id|EMPLOYEE ID", "designation|RANK"
        };
        renderFields(identityCard, identityFields);
        content.addView(identityCard, getCardParams());

        // 🔗 CLUSTER 2: LOGISTICS & OVERSIGHT
        renderSectionHeader(content, "FORENSIC LOGISTICS & OVERSIGHT");
        LinearLayout oversightCard = new LinearLayout(this);
        oversightCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, oversightCard, "CARD_STANDARD");
        oversightCard.setPadding(35, 35, 35, 35);

        String[] oversightFields = {
            "supervisor|SUPERVISOR", "shift_timing|SHIFT", "work_location|LOCATION"
        };
        renderFields(oversightCard, oversightFields);
        content.addView(oversightCard, getCardParams());

        // 🔗 CLUSTER 3: SECURITY & ACCESS PROTOCOLS
        renderSectionHeader(content, "SECURITY & ACCESS PROTOCOLS");
        LinearLayout accessCard = new LinearLayout(this);
        accessCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, accessCard, "CARD_STANDARD");
        accessCard.setPadding(35, 35, 35, 35);

        String[] accessFields = {
            "badge_number|BADGE ID", "emergency_ext|EXTENSION", "access_level|CLEARANCE"
        };
        renderFields(accessCard, accessFields);
        content.addView(accessCard, getCardParams());

        // 🚀 ACTION MATRIX
        View saveBtn = createIconButton("COMMIT ANALYTICAL DOSSIER", android.R.drawable.ic_menu_save, true, v -> handleOrgSave());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scale(140));
        btnLp.setMargins(0, 40, 0, 100);
        content.addView(saveBtn, btnLp);

        scrollView.addView(content);
        mainRoot.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        return mainRoot;
    }

    private void renderFields(LinearLayout container, String[] fields) {
        for (String field : fields) {
            String[] parts = field.split("\\|");
            
            TextView tvLabel = new TextView(this);
            tvLabel.setText(parts[1]);
            tvLabel.setTextColor(getGradientStart());
            UiArchitect.applyText(this, tvLabel, "TEXT_LABEL");
            tvLabel.setTextSize(10f);
            tvLabel.setPadding(10, 25, 0, 10);
            container.addView(tvLabel);

            EditText et = new EditText(this);
            et.setHint("Awaiting node input...");
            et.setHintTextColor(Color.argb(60, 128, 128, 128));
            et.setTextColor(getTextColor());
            et.setBackgroundColor(Color.TRANSPARENT);
            et.setTextSize(14f);
            fieldMap.put(parts[0], et);
            
            container.addView(wrapInEnterpriseCapsule(et));
        }
    }

    private void renderSectionHeader(LinearLayout container, String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextColor(getSecondaryTextColor());
        UiArchitect.applyText(this, header, "TEXT_MINI");
        header.setAlpha(0.6f);
        header.setPadding(10, 45, 0, 15);
        container.addView(header);
    }

    private LinearLayout.LayoutParams getCardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 30);
        return lp;
    }

    private View wrapInEnterpriseCapsule(View inner) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(40, 15, 40, 15);
        
        android.graphics.drawable.GradientDrawable fieldBg = new android.graphics.drawable.GradientDrawable();
        fieldBg.setColor(getCardColor()); // Floating pottery/ceramic card color
        fieldBg.setStroke(2, getGradientStart()); 
        fieldBg.setCornerRadius(scaleRadius(12));
        row.setBackground(fieldBg);
        
        inner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(inner);
        return row;
    }

    @Override
    protected void setupLogic() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                UiArchitect.showConfirmationDialog(OrgActivity.this, 
                    "Exit Professional Hub?", 
                    "Would you like to save your organization details before exiting?", 
                    "SAVE & EXIT", "DISCARD", 
                    () -> { handleOrgSave(); finish(); }, 
                    () -> finish());
            }
        });
        loadActiveOrgDossier();
    }

    private void loadActiveOrgDossier() {
        String safeUid = FirebaseAuth.getInstance().getUid();
        if (safeUid == null || safeUid.isEmpty()) {
            safeUid = com.jdroidx.jfindme.utils.RegistryManager.getAgentId(this);
        }
        if (safeUid == null) return;

        // 🚀 V61 Forensic Sync: Use the centralized Dossier Retrieval bridge
        java.util.Map<String, String> dossier = db.getLocalDossier(safeUid);
        if (dossier != null && !dossier.isEmpty()) {
            for (String key : fieldMap.keySet()) {
                String val = dossier.get(key);
                if (val != null) {
                    EditText et = fieldMap.get(key);
                    if (et != null) et.setText(val);
                }
            }
        }
    }

    private void handleOrgSave() {
        String safeUid = FirebaseAuth.getInstance().getUid();
        if (safeUid == null || safeUid.isEmpty()) {
            safeUid = com.jdroidx.jfindme.utils.RegistryManager.getAgentId(this);
        }
        if (safeUid == null) {
            UiArchitect.showSlideNotificationBanner(this, "Identity Unresolved", "Hardware identity anchor not found.", true, 3000, null, null);
            return;
        }

        String orgName = fieldMap.containsKey("org_name") && fieldMap.get("org_name") != null ? fieldMap.get("org_name").getText().toString().trim() : "";
        if (orgName.isEmpty()) {
            UiArchitect.showActionableValidationDialog(this, "Organization Name Required", 
                "Please enter an Organization or Company name to bind your enterprise profile.", 
                "OK", "DISMISS", null, null);
            return;
        }

        ContentValues v = new ContentValues();
        Map<String, Object> cloudMap = new HashMap<>();
        android.content.SharedPreferences.Editor editor = getSharedPreferences("system_registry", MODE_PRIVATE).edit();
        
        for (String key : fieldMap.keySet()) {
            String val = fieldMap.get(key).getText().toString().trim();
            v.put(key, val);
            cloudMap.put(key, val);
            if ("org_name".equals(key)) {
                editor.putString("org_name", val);
            } else if ("department".equals(key)) {
                editor.putString("department", val);
            }
        }
        editor.apply();
        
        v.put("uid", safeUid);

        try {
            // 🚀 Phase 1: Local Forensic Commit
            db.saveOrgDossier(v);
            
            // 🚀 Phase 2: Waterfall Cloud Sync (Absolute Parity with SSOT Profile)
            com.jdroidx.jfindme.data.remote.FirebaseComm.resolveHardwareIdentity(safeUid, agentId -> {
                if (agentId == null || !com.jdroidx.jfindme.data.remote.FirebaseComm.isAgentIdValid(agentId)) return null;
                com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(agentId).child("org").updateChildren(cloudMap);
                
                // Keep top-level SSOT profile fields in sync
                Map<String, Object> topLevelUpdates = new HashMap<>();
                if (cloudMap.containsKey("org_name")) topLevelUpdates.put("org_name", cloudMap.get("org_name"));
                if (cloudMap.containsKey("department")) topLevelUpdates.put("dept_name", cloudMap.get("department"));
                if (!topLevelUpdates.isEmpty()) {
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getProfileRef(agentId).updateChildren(topLevelUpdates);
                }
                return null;
            });
            
            Toast.makeText(this, "Organization Timeline Secured Locally & Cloud.", Toast.LENGTH_SHORT).show();
            SystemLogger.INSTANCE.i("OrgActivity", "Corporate Matrix Hardened for ID: " + safeUid);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Forensic Transaction Error.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveCurrentState(android.os.Bundle outState) {
        if (outState == null || fieldMap == null) return;
        for (String key : fieldMap.keySet()) {
            EditText et = fieldMap.get(key);
            if (et != null) {
                outState.putString("ORG_FIELD_" + key, et.getText().toString());
            }
        }
    }

    @Override
    protected void onUiRebuilt(android.os.Bundle savedState) {
        if (savedState == null || fieldMap == null) return;
        for (String key : fieldMap.keySet()) {
            String savedVal = savedState.getString("ORG_FIELD_" + key);
            if (savedVal != null) {
                EditText et = fieldMap.get(key);
                if (et != null) et.setText(savedVal);
            }
        }
    }
}
