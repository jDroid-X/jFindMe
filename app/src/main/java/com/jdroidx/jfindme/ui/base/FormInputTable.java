package com.jdroidx.jfindme.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.os.Build;
import java.util.HashMap;
import java.util.Map;

/**
 * 👑 FormInputTable: Enterprise Configuration Registry
 * Standardizes text entry, password security, and identity aliases.
 * Follows the 10-property "Enterprise Matrix" and 5-stage Universal Design rules.
 */
public class FormInputTable {

    public static class InputRecord {
        public String id;
        public String hint;
        public int iconRes;
        public String talkBackLabel;
        public float elevation;
        public float glowIntensity;
        public String interactionType; // NONE, LIGHT, HEAVY
        public boolean dynamicBehavior;
        public int fallbackColor;
        public int inputType;
        public int imeOptions;

        public InputRecord(String id, String hint, int iconRes, String talkBackLabel, 
                          float elevation, float glowIntensity, String interactionType, 
                          boolean dynamicBehavior, int fallbackColor, int inputType, int imeOptions) {
            this.id = id;
            this.hint = hint;
            this.iconRes = iconRes;
            this.talkBackLabel = talkBackLabel;
            this.elevation = elevation;
            this.glowIntensity = glowIntensity;
            this.interactionType = interactionType;
            this.dynamicBehavior = dynamicBehavior;
            this.fallbackColor = fallbackColor;
            this.inputType = inputType;
            this.imeOptions = imeOptions;
        }
    }

    private static final Map<String, InputRecord> registry = new HashMap<>();

    public static Map<String, InputRecord> getRegistry() { return registry; }

    static {
        // [Enterprise Registry] - Core Data Input Matrix
        registry.put("FIELD_IDENTITY", new InputRecord("FIELD_IDENTITY", "Identity Alias", android.R.drawable.ic_menu_myplaces, "Input your unique network nickname", 5f, 0.4f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_EMAIL",    new InputRecord("FIELD_EMAIL",    "Enterprise Email", android.R.drawable.ic_dialog_email, "Input registered gateway credentials", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_PASSWORD", new InputRecord("FIELD_PASSWORD", "Secure Password", android.R.drawable.ic_lock_idle_lock, "Master cryptographic session key", 8f, 0.6f, "HEAVY", true, Color.WHITE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, android.view.inputmethod.EditorInfo.IME_ACTION_DONE));
        registry.put("FIELD_NICKNAME", new InputRecord("FIELD_NICKNAME", "Device Label", com.jdroidx.jfindme.R.drawable.ic_edit, "Assign human-readable name to node", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_CODE",     new InputRecord("FIELD_CODE",     "Access Token", android.R.drawable.ic_secure, "6-Digit network authorization token", 8f, 0.5f, "HEAVY", true, Color.WHITE, InputType.TYPE_CLASS_NUMBER, android.view.inputmethod.EditorInfo.IME_ACTION_DONE));

        // --- 🛰️ Profile Dossier Extension (L7 Master Sync) ---
        registry.put("FIELD_PHONE",  new InputRecord("FIELD_PHONE",  "* Mobile Num", android.R.drawable.ic_menu_call, "Forensic contact link", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_PHONE, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_DOB",    new InputRecord("FIELD_DOB",    "* BirthDay", android.R.drawable.ic_menu_today, "Node creation/activation date", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_DATETIME, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_GENDER", new InputRecord("FIELD_GENDER", "Select Gender", android.R.drawable.ic_menu_info_details, "Biological identity protocol", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_COUNTRY",new InputRecord("FIELD_COUNTRY","* Country", android.R.drawable.ic_menu_mapmode, "Regional jurisdiction", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_STATE",  new InputRecord("FIELD_STATE",  "* State/Province", android.R.drawable.ic_menu_compass, "Provincial sector", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_CITY",   new InputRecord("FIELD_CITY",   "* City/Zone", android.R.drawable.ic_menu_mylocation, "Localized operational zone", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_ZIP",    new InputRecord("FIELD_ZIP",    "* ZipCode", android.R.drawable.ic_menu_info_details, "Postal matrix hub", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_NUMBER, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_NOTE",   new InputRecord("FIELD_NOTE",   "Emergency Action Protocol / Medical N", android.R.drawable.ic_dialog_info, "Critical medical/safety instructions", 10f, 0.4f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_ORG",    new InputRecord("FIELD_ORG",    "Organization / Hub Name", android.R.drawable.ic_menu_agenda, "Entity identity", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT, android.view.inputmethod.EditorInfo.IME_ACTION_NEXT));
        registry.put("FIELD_DEPT",   new InputRecord("FIELD_DEPT",   "Department / Sector", android.R.drawable.ic_menu_directions, "Division/Team identifier", 5f, 0.3f, "LIGHT", true, Color.WHITE, InputType.TYPE_CLASS_TEXT, android.view.inputmethod.EditorInfo.IME_ACTION_DONE));
    }

    public static EditText createField(BaseActivityJava activity, String tableKey) {
        InputRecord record = registry.get(tableKey);
        if (record == null) return new EditText(activity);

        UiArchitect.DesignStage stage = UiArchitect.getStage(activity);
        EditText et = new EditText(activity);
        et.setHint(record.hint);
        et.setHintTextColor(activity.getSecondaryTextColor());
        et.setTextColor(activity.getTextColor());
        et.setInputType(record.inputType);
        et.setImeOptions(record.imeOptions);
        et.setContentDescription(record.talkBackLabel);
        
        // [Stage Architecture Interceptor]
        int pad = UiScalingMatrixTable.scalePadding(stage, 40);
        float radius = UiArchitect.getResolvedRadius(activity, 16f);
        float elevation = UiScalingMatrixTable.scaleElevation(stage, record.elevation);

        et.setPadding(pad, pad, pad, pad);
        et.setCompoundDrawablePadding(20);
        et.setCompoundDrawablesWithIntrinsicBounds(record.iconRes, 0, 0, 0);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            et.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(activity.getGradientMid()));
        }

        GradientDrawable bg = new GradientDrawable();
        int tint = activity.getGradientStart();
        if (record.dynamicBehavior) {
            bg.setColor(Color.argb(20, 0, 0, 0)); // 8% Adaptive Overlay
            // 🚀 QA Fix: Global Border Toggle (System Registry)
            boolean bordersEnabled = activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getBoolean("borders_enabled", true);
            float borderStroke = bordersEnabled ? UiScalingMatrixTable.scaleBorderStroke(stage) : 0f;
            bg.setStroke((int)borderStroke, tint);
            bg.setCornerRadius(radius);
        } else {
            bg.setColor(record.fallbackColor);
            bg.setCornerRadius(radius);
        }
        
        et.setBackground(bg);
        UiArchitect.applyNeutralShadow(activity, et, elevation);
        et.setClipToOutline(radius > 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 15, 0, 15);
        et.setLayoutParams(lp);
        
        return et;
    }
}
