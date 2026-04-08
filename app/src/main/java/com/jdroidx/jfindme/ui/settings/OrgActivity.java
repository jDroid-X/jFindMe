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
 * 🚀 L7 REVISED: Enterprise Organization Suite
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

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        TextView title = new TextView(this);
        title.setText("ORGANIZATION STRUCTURE");
        title.setTextColor(getGradientOverlayText());
        UiArchitect.applyText(this, title, "TEXT_HEADER");
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Professional Identity & Clearance Hub");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        UiArchitect.applyText(this, subtitle, "TEXT_SUB");
        header.addView(subtitle);

        mainRoot.addView(header);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(q, q, q, q * 2);

        String[] fields = {
            "org_name|🏢 ORGANIZATION NAME", "department|📂 DEPARTMENT", "employee_id|🔢 EMPLOYEE ID",
            "designation|🎓 RANK", "supervisor|👮 SUPERVISOR", "shift_timing|🕒 SHIFT",
            "work_location|🗺️ LOCATION", "badge_number|🔑 BADGE ID", "emergency_ext|☎️ EXTENSION",
            "access_level|⚖️ CLEARANCE"
        };

        for (String field : fields) {
            String[] parts = field.split("\\|");
            
            TextView tvLabel = new TextView(this);
            tvLabel.setText(parts[1]);
            tvLabel.setTextColor(getGradientStart());
            tvLabel.setTextSize(12);
            tvLabel.setTypeface(null, Typeface.BOLD);
            tvLabel.setPadding(10, 30, 0, 10);
            content.addView(tvLabel);

            EditText et = new EditText(this);
            et.setHint("Awaiting input...");
            et.setBackgroundColor(Color.TRANSPARENT);
            fieldMap.put(parts[0], et);
            
            // 🚀 QA Fix: Wrap in L7 Enterprise Capsule
            content.addView(wrapInL7Capsule(et));
        }

        View saveBtn = createIconButton("COMMIT PROFESSIONAL DOSSIER", android.R.drawable.ic_menu_save, true, v -> handleOrgSave());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 160);
        btnLp.setMargins(0, 60, 0, 100);
        saveBtn.setLayoutParams(btnLp);
        content.addView(saveBtn);

        scrollView.addView(content);
        mainRoot.addView(scrollView);

        return mainRoot;
    }

    private View wrapInL7Capsule(View inner) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(35, 20, 35, 20);
        
        android.graphics.drawable.GradientDrawable fieldBg = new android.graphics.drawable.GradientDrawable();
        fieldBg.setColor(Color.parseColor("#1A000000")); // Glassmorphism Tint
        fieldBg.setStroke(3, getGradientStart()); 
        fieldBg.setCornerRadius(60f);
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

        android.database.sqlite.SQLiteDatabase sdb = db.getReadableDatabase();
        // 🚀 QA Fix: Strict UID Filtering (Forensic Identity Logic)
        android.database.Cursor cursor = sdb.query("org_table", null, "uid=? AND end_datetime = 253402300799000", new String[]{safeUid}, null, null, null);
        if (cursor.moveToFirst()) {
            for (String key : fieldMap.keySet()) {
                int idx = cursor.getColumnIndex(key);
                if (idx != -1) {
                    EditText et = fieldMap.get(key);
                    if (et != null) et.setText(cursor.getString(idx));
                }
            }
        }
        cursor.close();
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
            // 🚀 Phase 1: Local Forensic Commit
            db.saveOrgDossier(v);
            
            // 🚀 Phase 2: Waterfall Cloud Sync (Absolute Parity)
            FirebaseDatabase.getInstance().getReference("users").child(safeUid).child("org").updateChildren(cloudMap);
            
            Toast.makeText(this, "Organization Timeline Secured Locally & Cloud.", Toast.LENGTH_SHORT).show();
            SystemLogger.INSTANCE.i("OrgActivity", "Corporate Matrix Hardened for ID: " + safeUid);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Forensic Transaction Error.", Toast.LENGTH_LONG).show();
        }
    }
}
