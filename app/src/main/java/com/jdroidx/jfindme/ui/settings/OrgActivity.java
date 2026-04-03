package com.jdroidx.jfindme.ui.settings;

import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Organization Suite (V35 PRO-Standard)
 * Fully compliant with BaseActivityJava (L3) Architecture.
 */
public class OrgActivity extends BaseActivityJava {

    private DatabaseHelper db;
    private Map<String, EditText> fieldMap = new HashMap<>();

    @Override
    protected View createLayout() {
        db = new DatabaseHelper(this);
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

        LinearLayout formCard = new LinearLayout(this);
        formCard.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, formCard, "CARD_STANDARD");
        formCard.setPadding(q, q, q, q);

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
            formCard.addView(tvLabel);

            EditText et = new EditText(this);
            et.setHint("Entry node...");
            et.setTextColor(getTextColor());
            et.setHintTextColor(getSecondaryTextColor());
            et.setPadding(35, 30, 35, 30);
            et.setTextSize(14);
            fieldMap.put(parts[0], et);
            formCard.addView(et);
        }

        content.addView(formCard);

        View saveBtn = createIconButton("COMMIT PROFESSIONAL DOSSIER", android.R.drawable.ic_menu_save, true, v -> handleOrgSave());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 160);
        btnLp.setMargins(0, 60, 0, 100);
        saveBtn.setLayoutParams(btnLp);
        content.addView(saveBtn);

        scrollView.addView(content);
        mainRoot.addView(scrollView);

        return mainRoot;
    }

    @Override
    protected void setupLogic() {
        loadActiveOrgDossier();
    }

    private void loadActiveOrgDossier() {
        android.database.sqlite.SQLiteDatabase sdb = db.getReadableDatabase();
        android.database.Cursor cursor = sdb.query("org_table", null, "end_datetime = 253402300799000", null, null, null, null);
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
        ContentValues v = new ContentValues();
        for (String key : fieldMap.keySet()) v.put(key, fieldMap.get(key).getText().toString().trim());
        
        String safeUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        v.put("uid", safeUid != null ? safeUid : "OWNER_001");
        try {
            db.saveOrgDossier(v);
            Toast.makeText(this, "Organization Timeline Secured.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Forensic Transaction Error.", Toast.LENGTH_LONG).show();
        }
    }
}

