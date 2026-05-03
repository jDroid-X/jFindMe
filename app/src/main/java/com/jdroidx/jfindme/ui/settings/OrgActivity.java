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

        TextView title = new TextView(this);
        title.setText("PROFESSIONAL IDENTITY HUB");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Forensic Organization Dossier | Authority: Enterprise");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.75f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
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
        fieldBg.setColor(Color.argb(15, 150, 150, 150)); // Glass Tint
        fieldBg.setStroke(2, getGradientStart()); 
        fieldBg.setCornerRadius(scaleRadius(12));
        row.setBackground(fieldBg);
        
        inner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        row.addView(inner);
        return row;
    }

    @Override
    protected void setupLogic() {
        loadActiveOrgDossier();
    }

    private void loadActiveOrgDossier() {
        String safeUid = FirebaseAuth.getInstance().getUid();
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
        if (safeUid == null) {
            Toast.makeText(this, "Hardware Identity Missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues v = new ContentValues();
        Map<String, Object> cloudMap = new HashMap<>();
        
        for (String key : fieldMap.keySet()) {
            String val = fieldMap.get(key).getText().toString().trim();
            v.put(key, val);
            cloudMap.put(key, val);
        }
        
        v.put("uid", safeUid);

        try {
            // ≡ƒÜÇ Phase 1: Local Forensic Commit
            db.saveOrgDossier(v);
            
            // ≡ƒÜÇ Phase 2: Waterfall Cloud Sync (Absolute Parity)
            FirebaseDatabase.getInstance().getReference("users").child(safeUid).child("org").updateChildren(cloudMap);
            
            Toast.makeText(this, "Organization Timeline Secured Locally & Cloud.", Toast.LENGTH_SHORT).show();
            SystemLogger.INSTANCE.i("OrgActivity", "Corporate Matrix Hardened for ID: " + safeUid);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Forensic Transaction Error.", Toast.LENGTH_LONG).show();
        }
    }
}
