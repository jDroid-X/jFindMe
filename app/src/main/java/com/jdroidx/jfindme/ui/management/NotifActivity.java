package com.jdroidx.jfindme.ui.management;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;
import java.util.List;
import java.util.Map;

/**
 * Main -> UI Engine -> ManagementCluster -> NotifActivity (L4)
 * Rules: 100% Java Programmatic, Alert Status Stream.
 * ENTERPRISE EDITION: Unified Forensic Alert Log & Configuration Hub.
 */
public class NotifActivity extends BaseActivityJava {

    private final String TAG = "NotifActivity";
    private NotifViewModel viewModel;
    private LinearLayout alertLogContainer;
    private LinearLayout syncContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(NotifViewModel.class);
        super.onCreate(onCreateSavedInstanceState(savedInstanceState));
    }

    private Bundle onCreateSavedInstanceState(Bundle b) { return b; }

    @Override
    protected View createLayout() {
        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(getPrimaryColor());

        // 🚀 Premium Linear Gradient Hero Banner
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        applyGlobalHeaderGeometry(header);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("SYSTEM ALERTS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        topRow.addView(title);

        // 🚀 HELP HUB (?) Restored
        TextView helpBtn = new TextView(this);
        helpBtn.setText("?");
        helpBtn.setTextColor(Color.WHITE);
        helpBtn.setTextSize(18);
        helpBtn.setGravity(Gravity.CENTER);
        helpBtn.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable qBg = new android.graphics.drawable.GradientDrawable();
        qBg.setColor(Color.parseColor("#8E24AA")); // Slightly lighter purple for depth
        qBg.setCornerRadius(scale(10));
        helpBtn.setBackground(qBg);
        int hP = scale(15);
        helpBtn.setPadding(hP, hP/2, hP, hP/2);
        topRow.addView(helpBtn);

        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Alert Configuration & Hardware Triggers");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setAlpha(0.9f);
        subtitle.setTextSize(13);
        subtitle.setPadding(0, scale(5), 0, 0);
        header.addView(subtitle);

        // 🚀 ANALYTICAL HUB: Architecture Status Bar (Transparency Pillar)
        header.addView(buildArchitectureStatusBar());

        mainRoot.addView(header);
        mainRoot.setClipChildren(false); 
        mainRoot.setClipToPadding(false);
        
        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);

        LinearLayout contentWrapper = new LinearLayout(this);
        contentWrapper.setOrientation(LinearLayout.VERTICAL);
        contentWrapper.setPadding(0, 0, 0, scale(100)); // Buffer for Nav

        // --- 1. SYNC REQUESTS ZONE (Dynamic) ---
        syncContainer = new LinearLayout(this);
        syncContainer.setOrientation(LinearLayout.VERTICAL);
        contentWrapper.addView(syncContainer);

        // --- 2. SYSTEM CONFIGURATION ZONE (Primary) ---
        contentWrapper.addView(createNotifList());

        scroll.addView(contentWrapper);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 🚀 QA Fix: Global Navigation Integrity
        String[] keys = {"NAV_NOTICE", "NAV_DEVICE", "NAV_ME"};
        mainRoot.addView(UiArchitect.createBottomNav(this, keys, "NAV_NOTICE")); 

        return mainRoot;
    }

    private View createNotifList() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int lPad = getResponsivePaddingQuota();
        list.setPadding(lPad * 2, lPad, lPad * 2, lPad * 2);

        // 🚀 TILE-BY-TILE CLONE (V50.5)
        list.addView(createNotifConfigItem("Panic SOS Override", "Highest Priority. Bypasses DND volume constraints natively.", "alert_sos", true, Color.parseColor("#F44336")));
        list.addView(createNotifConfigItem("Geofence Perimeter Breach", "Triggers autonomously when a target exits or enters a Safe Zone.", "alert_geofence", true, Color.parseColor("#9C27B0")));
        list.addView(createNotifConfigItem("Battery Critical Warning", "Warns Host prior to child device hardware shutdown (Under 15%).", "alert_battery", true, Color.parseColor("#FF9800")));
        
        // Connectivity RESTORED
        list.addView(createNotifConfigItem("Network Timeout / Dropped", "Absolute loss of signal for more than 10 consecutive minutes.", "alert_network", true, Color.parseColor("#9E9E9E")));
        list.addView(createNotifConfigItem("Device Powered Off", "Notifies heavily when Target shuts down hardware physically.", "alert_power", true, Color.parseColor("#9E9E9E")));
        
        // Security RESTORED
        list.addView(createNotifConfigItem("Gps Services Disabled", "Critical security failure if Location is manually turned off.", "alert_gps_off", true, Color.parseColor("#F44336")));
        list.addView(createNotifConfigItem("App Permission Revoked", "Detects if tracking Background rights are removed in OS Settings.", "alert_perms", true, Color.parseColor("#F44336")));
        list.addView(createNotifConfigItem("Root / Jailbreak Detected", "Severe Security Compromise alert. Blocks malicious Mock GPS systems.", "alert_root", false, Color.parseColor("#9E9E9E")));
        
        // 🚀 PERFORMANCE OPTIMIZATION SUITE (V50.6)
        list.addView(createNotifConfigItem("Database Bloat Warning", "Alerts when forensic logs exceed threshold for optimal speed.", "perf_db_bloat", true, Color.parseColor("#4CAF50")));
        list.addView(createNotifConfigItem("Network Latency Monitor", "Provides visibility into telemetry transmission delays.", "perf_net_lag", true, Color.parseColor("#4CAF50")));
        list.addView(createNotifConfigItem("Thermal Tracking Guard", "Warns if continuous GPS causes hardware overheating.", "perf_thermal", false, Color.parseColor("#4CAF50")));

        return list;
    }

    private View createNotifConfigItem(String title, String body, String prefKey, boolean defVal, int accentColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, card, "CARD_STANDARD");
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int cMargin = getResponsivePaddingQuota();
        cardLp.setMargins(0, 0, 0, cMargin);
        card.setLayoutParams(cardLp);
        
        LinearLayout innerRow = new LinearLayout(this);
        innerRow.setOrientation(LinearLayout.HORIZONTAL);
        innerRow.setGravity(Gravity.CENTER_VERTICAL);
        
        View statusRibbon = new View(this);
        statusRibbon.setBackgroundColor(accentColor);
        innerRow.addView(statusRibbon, new LinearLayout.LayoutParams(scale(15), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        int tPad = getResponsivePaddingQuota() * 2;
        textBlock.setPadding(tPad, tPad, tPad / 2, tPad);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title.toUpperCase());
        tvTitle.setTextColor(accentColor);
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        textBlock.addView(tvTitle);

        TextView tvBody = new TextView(this);
        tvBody.setText(body);
        tvBody.setTextColor(getTextColor());
        tvBody.setAlpha(0.85f);
        tvBody.setTextSize(11);
        textBlock.addView(tvBody);
        
        innerRow.addView(textBlock);

        android.widget.Switch toggle = new android.widget.Switch(this);
        android.content.SharedPreferences prefs = getSharedPreferences("com.jdroidx.jfindme.PREFS", android.content.Context.MODE_PRIVATE);
        toggle.setChecked(prefs.getBoolean(prefKey, defVal));
        toggle.setPadding(0, 0, tPad, 0);
        toggle.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean(prefKey, isChecked).apply());
        
        innerRow.addView(toggle);
        card.addView(innerRow);
        
        card.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        return card;
    }


    private View createSyncApprovalCard(Map<String, String> req) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, card, "CARD_DASHBOARD", 1.25f);
        int p = getResponsivePaddingQuota();
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(p * 2, p, p * 2, p); card.setLayoutParams(lp);

        TextView t = new TextView(this); t.setText("SAFEZONE SYNC REQUEST"); UiArchitect.applyText(this, t, "TEXT_LABEL"); card.addView(t);
        TextView b = new TextView(this); b.setText("Admin requests sync of '" + req.get("zone_name") + "'.");
        b.setTextColor(getTextColor()); b.setTextSize(12); b.setPadding(0, p, 0, p); card.addView(b);

        LinearLayout row = new LinearLayout(this);
        row.addView(createCapsuleButton("ACCEPT", getGradientStart(), v -> processSyncAcceptance(req.get("id"), req.get("zone_name"), req.get("group_id"))));
        row.addView(createCapsuleButton("REJECT", Color.GRAY, v -> {
             String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
             if (myUid != null) com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(myUid).child("geofence_sync_requests").child(req.get("id")).child("status").setValue("REJECTED");
        }));
        card.addView(row);
        return card;
    }

    private View createCapsuleButton(String text, int color, View.OnClickListener cl) {
        TextView b = new TextView(this);
        b.setText(text); b.setTextColor(Color.WHITE); b.setGravity(Gravity.CENTER); b.setPadding(30, 20, 30, 20);
        b.setTypeface(null, android.graphics.Typeface.BOLD); b.setTextSize(11);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color); g.setCornerRadius(100f); b.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f); lp.setMargins(10, 10, 10, 10); b.setLayoutParams(lp);
        b.setOnClickListener(cl);
        return b;
    }

    private void processSyncAcceptance(String reqId, String name, String group) {
        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("groups").child(group).child("geofences").child(name)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snap) {
                    if (snap.exists()) {
                        Double lat = snap.child("lat").getValue(Double.class);
                        Double lng = snap.child("lng").getValue(Double.class);
                        Double rad = snap.child("radius").getValue(Double.class);
                        if (lat != null && lng != null && rad != null) {
                            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(NotifActivity.this);
                            android.content.ContentValues v = new android.content.ContentValues();
                            v.put("zone_id", name); v.put("name", name); v.put("lat", lat); v.put("lng", lng); v.put("radius", rad); v.put("st_sync_state", "ACCEPTED");
                            db.saveSafeZone(v);
                            
                            com.jdroidx.jfindme.engine.location.GeofenceHelper helper = new com.jdroidx.jfindme.engine.location.GeofenceHelper(NotifActivity.this);
                            helper.addGeofence(name, lat, lng, rad.floatValue());
                            
                            String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                            if (myUid != null) com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(myUid).child("geofence_sync_requests").child(reqId).child("status").setValue("SYNC_CONFIRMED");
                        }
                    }
                    com.jdroidx.jfindme.utils.AngelLoader.hide(NotifActivity.this);
                }
                @Override public void onCancelled(com.google.firebase.database.DatabaseError error) { com.jdroidx.jfindme.utils.AngelLoader.hide(NotifActivity.this); }
            });
    }

    @Override
    protected void setupLogic() {
        viewModel.getPendingRequests().observe(this, requests -> {
            syncContainer.removeAllViews();
            for (Map<String, String> req : requests) {
                syncContainer.addView(createSyncApprovalCard(req));
            }
        });
    }

    private View buildArchitectureStatusBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(20, 15, 20, 15);
        bar.setBackgroundColor(Color.argb(80, Color.red(getPrimaryColor()), Color.green(getPrimaryColor()), Color.blue(getPrimaryColor()))); 
        
        bar.addView(createArchitectureIcon(android.R.drawable.ic_lock_silent_mode, "Secure", "Analytical Tiering: 5-Tier Normalized Architecture active."));
        bar.addView(createArchitectureIcon(android.R.drawable.ic_menu_save, "Local", "Identity Hub: Data retrieval anchored in Tier 1 Local Profiles."));
        bar.addView(createArchitectureIcon(android.R.drawable.ic_popup_sync, "Matrix", "Analytical Sync: Cloud and Local parity maintained via tiered nodes."));
        bar.addView(createArchitectureIcon(android.R.drawable.ic_menu_recent_history, "Silicon", "Hardware Lock: Tier 2 Silicon Fingerprints verified and archived."));
        bar.addView(createArchitectureIcon(android.R.drawable.checkbox_on_background, "Mesh", "Relationship Mesh: Tier 3 Graph Traversal active for peer discovery."));

        return bar;
    }

    private View createArchitectureIcon(int resId, String label, String toastMsg) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(25, 5, 25, 5);
        box.setClickable(true);
        box.setOnClickListener(v -> android.widget.Toast.makeText(this, toastMsg, android.widget.Toast.LENGTH_SHORT).show());
        
        ImageView iv = new ImageView(this);
        iv.setImageResource(resId);
        iv.setColorFilter(getGradientOverlayText());
        int iSize = scale(60); 
        box.addView(iv, new LinearLayout.LayoutParams(iSize, iSize));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(9);
        tv.setTextColor(getGradientOverlayText());
        tv.setAlpha(0.6f);
        tv.setGravity(Gravity.CENTER);
        box.addView(tv);

        return box;
    }
}

