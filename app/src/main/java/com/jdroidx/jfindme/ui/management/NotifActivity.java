package com.jdroidx.jfindme.ui.management;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.base.UiScalingMatrixTable;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * Main -> UI Engine -> ManagementCluster -> NotifActivity (L4)
 * Rules: 100% Java Programmatic, Alert Status Stream.
 */
public class NotifActivity extends BaseActivityJava {

    private final String TAG = "NotifActivity";

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
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        topRow.addView(title);
        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Alert Configuration & Hardware Triggers");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.8f);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, scale(10), 0, 0);
        header.addView(subtitle);

        mainRoot.addView(header);
        mainRoot.setClipChildren(false); // Global shadow enablement
        mainRoot.setClipToPadding(false);
        
        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);

        LinearLayout syncContainer = new LinearLayout(this);
        syncContainer.setOrientation(LinearLayout.VERTICAL);
        syncContainer.setClipChildren(false);
        syncContainer.setClipToPadding(false);
        
        LinearLayout listRoot = new LinearLayout(this);
        listRoot.setOrientation(LinearLayout.VERTICAL);
        listRoot.addView(syncContainer);
        listRoot.addView(createNotifList());

        scroll.addView(listRoot);
        mainRoot.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 🛰️ L7 HANDSHAKE: Live Sync Approval Listener
        setupSyncHandshakeListener(syncContainer);

        // 🚀 QA Fix: Global Navigation Integrity
        String[] keys = {"NAV_NOTICE", "NAV_ALERT", "NAV_DEVICE", "NAV_ME"};
        mainRoot.addView(UiArchitect.createBottomNav(this, keys, "NAV_NOTICE"));

        return mainRoot;
    }
    private View createNotifList() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false); // No shadow clipping
        list.setClipToPadding(false);

        int lPad = getResponsivePaddingQuota();
        list.setPadding(lPad * 2, lPad * 2, lPad * 2, lPad * 5);

        // 🚀 QA Fix: Enterprise 8-Point Configuration Matrix injected with explicit native SharedPreferences routing
        list.addView(createNotifConfigItem("Panic SOS Override", "Highest Priority. Bypasses DND volume constraints natively.", "alert_sos", true, Color.parseColor("#F44336")));
        list.addView(createNotifConfigItem("Geofence Perimeter Breach", "Triggers autonomously when a target exits or enters a Safe Zone.", "alert_geofence", true, getGradientStart()));
        list.addView(createNotifConfigItem("Battery Critical Warning", "Warns Host prior to child device hardware shutdown (Under 15%).", "alert_battery", true, Color.parseColor("#FF9800")));
        list.addView(createNotifConfigItem("Network Timeout / Dropped", "Absolute loss of signal for more than 10 consecutive minutes.", "alert_network", true, Color.parseColor("#9E9E9E")));
        list.addView(createNotifConfigItem("Device Powered Off", "Notifies heavily when Target shuts down hardware physically.", "alert_power", true, Color.parseColor("#9E9E9E")));
        // Expanded Security Architecture Configurations
        list.addView(createNotifConfigItem("GPS Services Disabled", "Critical security failure if Location is manually turned off.", "alert_gps_off", true, Color.parseColor("#F44336")));
        list.addView(createNotifConfigItem("App Permission Revoked", "Detects if tracking Background rights are removed in OS Settings.", "alert_perms", true, Color.parseColor("#F44336")));
        list.addView(createNotifConfigItem("Root / Jailbreak Detected", "Severe Security Compromise alert. Blocks malicious Mock GPS systems.", "alert_root", false, Color.parseColor("#9E9E9E")));

        return list;
    }

    private View createNotifConfigItem(String title, String body, String prefKey, boolean defVal, int accentColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCard(this, card, "CARD_STANDARD");
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int cMargin = getResponsivePaddingQuota();
        cardLp.setMargins(0, 0, 0, cMargin + scale(10));
        card.setLayoutParams(cardLp);
        
        // Matrix Render: Horizontal Colored Severity Stripe bridging seamlessly!
        LinearLayout innerRow = new LinearLayout(this);
        innerRow.setOrientation(LinearLayout.HORIZONTAL);
        innerRow.setGravity(Gravity.CENTER_VERTICAL);
        
        // Severity Ribbon
        View statusRibbon = new View(this);
        statusRibbon.setBackgroundColor(accentColor);
        innerRow.addView(statusRibbon, new LinearLayout.LayoutParams(scale(15), ViewGroup.LayoutParams.MATCH_PARENT));

        // Content Area
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
        tvBody.setPadding(0, scale(10), 0, 0);
        textBlock.addView(tvBody);
        
        innerRow.addView(textBlock);

        // 🚀 QA Fix: Native Enterprise Interactive Switch Component
        android.widget.Switch toggle = new android.widget.Switch(this);
        android.content.SharedPreferences prefs = getSharedPreferences("com.jdroidx.jfindme.PREFS", android.content.Context.MODE_PRIVATE);
        toggle.setChecked(prefs.getBoolean(prefKey, defVal));
        int togglePad = getResponsivePaddingQuota() * 2;
        toggle.setPadding(0, 0, togglePad, 0);
        toggle.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(prefKey, isChecked).apply();
            com.jdroidx.jfindme.utils.SystemLogger.i(TAG, prefKey + " configured to: " + isChecked);
        });
        
        innerRow.addView(toggle);
        card.addView(innerRow);
        
        // Ensure the entire card bounds trigger the toggle natively securely!
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                toggle.setChecked(!toggle.isChecked()); return true;
            } return false;
        });
        card.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));

        return card;
    }

    private void setupSyncHandshakeListener(LinearLayout container) {
        String myUid = getSharedPreferences("system_registry", MODE_PRIVATE).getString("user_uid", null);
        if (myUid == null) myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users").child(myUid).child("geofence_sync_requests");

        ref.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                container.removeAllViews();
                for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                    String status = snap.child("status").getValue(String.class);
                    if ("PENDING_ACCEPTANCE".equals(status)) {
                        String zoneName = snap.child("zone_name").getValue(String.class);
                        String groupId = snap.child("group_id").getValue(String.class);
                        container.addView(createSyncApprovalCard(snap.getKey(), zoneName, groupId));
                    }
                }
            }
            @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    private View createSyncApprovalCard(String requestId, String name, String group) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, card, "CARD_DASHBOARD", 1.25f);
        int p = getResponsivePaddingQuota();
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(p * 2, p * 2, p * 2, p); card.setLayoutParams(lp);

        TextView t = new TextView(this); t.setText("SAFEZONE ASSIGNMENT REQUEST"); UiArchitect.applyText(this, t, "TEXT_LABEL"); card.addView(t);
        TextView b = new TextView(this); b.setText("Admin has assigned '" + name + "' to your hardware node. Accept to sync coordinates?");
        b.setTextColor(getTextColor()); b.setTextSize(12); b.setPadding(0, p, 0, p); card.addView(b);

        LinearLayout row = new LinearLayout(this);
        row.addView(createCapsuleButton("ACCEPT & SYNC", getGradientStart(), v -> processSyncAcceptance(requestId, name, group)));
        row.addView(createCapsuleButton("REJECT", Color.GRAY, v -> {
            String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            if (myUid != null) com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(myUid).child("geofence_sync_requests").child(requestId).child("status").setValue("REJECTED");
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
                            v.put("zone_id", name); v.put("name", name); v.put("lat", lat); v.put("lng", lng); v.put("radius", rad); v.put("sync_status", "ACCEPTED");
                            db.saveSafeZone(v);
                            
                            // 🚀 TARGETED REFRESH: Force re-registration of hardware geofence
                            com.jdroidx.jfindme.engine.location.GeofenceHelper helper = new com.jdroidx.jfindme.engine.location.GeofenceHelper(NotifActivity.this);
                            String assignedTo = snap.child("assignedName").getValue(String.class);
                            if (assignedTo == null) assignedTo = "ALL CONNECTED DEVICES";
                            helper.addGeofence(name + "___" + assignedTo, lat, lng, rad.floatValue());
                            
                            String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                            if (myUid != null) com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(myUid).child("geofence_sync_requests").child(reqId).child("status").setValue("SYNC_CONFIRMED");
                            android.widget.Toast.makeText(NotifActivity.this, "Forensic SafeZone Synchronized!", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                    com.jdroidx.jfindme.utils.AngelLoader.hide(NotifActivity.this);
                }
                @Override public void onCancelled(com.google.firebase.database.DatabaseError error) { com.jdroidx.jfindme.utils.AngelLoader.hide(NotifActivity.this); }
            });
    }

    @Override
    protected void setupLogic() {
        SystemLogger.i(TAG, "Notification Stream Standby.");
    }
}
