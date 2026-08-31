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
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;
import java.util.List;
import java.util.Locale;
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
        title.setTextColor(getGradientOverlayText());
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        topRow.addView(title);

        // 🚀 HELP HUB (?) Restored
        TextView helpBtn = new TextView(this);
        helpBtn.setText("?");
        helpBtn.setTextColor(getGradientOverlayText());
        helpBtn.setTextSize(18);
        helpBtn.setGravity(Gravity.CENTER);
        helpBtn.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable qBg = new android.graphics.drawable.GradientDrawable();
        qBg.setColor(getGradientStart());
        qBg.setCornerRadius(scale(10));
        helpBtn.setBackground(qBg);
        int hP = scale(15);
        helpBtn.setPadding(hP, hP/2, hP, hP/2);
        helpBtn.setClickable(true);
        helpBtn.setFocusable(true);
        helpBtn.setOnClickListener(v -> triggerHelpDialog());
        topRow.addView(helpBtn);

        header.addView(topRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Alert Configuration & Hardware Triggers");
        subtitle.setTextColor(getGradientOverlayText());
        subtitle.setAlpha(0.9f);
        subtitle.setTextSize(13);
        subtitle.setPadding(0, scale(5), 0, 0);
        header.addView(subtitle);

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
        list.addView(createNotifConfigItem(
            "Geofence Perimeter Breach", 
            "Triggers autonomously when a target exits or enters a Safe Zone.", 
            "alert_geofence", 
            true, 
            Color.parseColor("#9C27B0"),
            "10-Sec Persistent Haptic Alert",
            "pref_geofence_persistent_haptic",
            false
        ));
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
        return createNotifConfigItem(title, body, prefKey, defVal, accentColor, null, null, false);
    }

    private View createNotifConfigItem(String title, String body, String prefKey, boolean defVal, int accentColor, String subOptionTitle, String subOptionPrefKey, boolean defSubVal) {
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
        android.content.SharedPreferences prefs = getSharedPreferences("system_registry", android.content.Context.MODE_PRIVATE);
        toggle.setChecked(prefs.getBoolean(prefKey, defVal));
        toggle.setPadding(0, 0, tPad, 0);
        toggle.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean(prefKey, isChecked).apply());
        
        innerRow.addView(toggle);
        card.addView(innerRow);

        if (subOptionPrefKey != null) {
            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#15000000"));
            card.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scale(1)));

            LinearLayout subRow = new LinearLayout(this);
            subRow.setOrientation(LinearLayout.HORIZONTAL);
            subRow.setGravity(Gravity.CENTER_VERTICAL);
            subRow.setPadding(scale(24), scale(8), scale(16), scale(12));

            android.widget.CheckBox cbSub = new android.widget.CheckBox(this);
            cbSub.setChecked(prefs.getBoolean(subOptionPrefKey, defSubVal));
            cbSub.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean(subOptionPrefKey, isChecked).apply());
            subRow.addView(cbSub);

            LinearLayout subTextBlock = new LinearLayout(this);
            subTextBlock.setOrientation(LinearLayout.VERTICAL);
            subTextBlock.setPadding(scale(8), 0, 0, 0);
            subTextBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvSubTitle = new TextView(this);
            tvSubTitle.setText("📳 " + (subOptionTitle != null ? subOptionTitle : "10-Sec Persistent Haptic Alert"));
            tvSubTitle.setTextColor(getTextColor());
            tvSubTitle.setTextSize(12);
            tvSubTitle.setTypeface(null, Typeface.BOLD);
            subTextBlock.addView(tvSubTitle);

            TextView tvSubDesc = new TextView(this);
            tvSubDesc.setText("[Checked]: Vibrate 10 seconds continuous | [Unchecked]: 1-time short pulse");
            tvSubDesc.setTextColor(getSecondaryTextColor());
            tvSubDesc.setTextSize(10);
            subTextBlock.addView(tvSubDesc);

            subRow.addView(subTextBlock);
            subRow.setOnClickListener(v -> cbSub.setChecked(!cbSub.isChecked()));
            card.addView(subRow);
        }
        
        return card;
    }


    private View createSyncApprovalCard(Map<String, String> req) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        UiArchitect.applyCardWithOverride(this, card, "CARD_DASHBOARD", 1.25f);
        int p = getResponsivePaddingQuota();
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(p * 2, p, p * 2, p); card.setLayoutParams(lp);

        String zoneName = req.get("zone_name") != null ? req.get("zone_name") : "Safe Zone";
        String zoneId = req.get("zone_id") != null ? req.get("zone_id") : zoneName;

        TextView t = new TextView(this); t.setText("SAFEZONE SYNC REQUEST"); UiArchitect.applyText(this, t, "TEXT_LABEL"); card.addView(t);
        TextView b = new TextView(this); b.setText("Admin requests sync of '" + zoneName + "'.");
        b.setTextColor(getTextColor()); b.setTextSize(12); b.setPadding(0, p, 0, p); card.addView(b);

        LinearLayout row = new LinearLayout(this);
        row.addView(createCapsuleButton("ACCEPT", getGradientStart(), v -> processSyncAcceptance(req.get("id"), zoneId, zoneName, req.get("group_id"), req)));
        row.addView(createCapsuleButton("DETAILS", Color.parseColor("#455A64"), v -> showSyncRequestDetailsDialog(req, zoneId, zoneName)));
        row.addView(createCapsuleButton("REJECT", Color.GRAY, v -> {
            UiArchitect.showConfirmationDialog(this,
                "DECLINE SYNC?",
                "Are you sure you want to decline protection for '" + zoneName + "'?",
                "DECLINE", "CANCEL",
                () -> {
                    String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                    if (myUid != null) {
                        com.jdroidx.jfindme.data.remote.FirebaseComm.resolveHardwareIdentity(myUid, agentId -> {
                             String finalId = agentId != null ? agentId : myUid;
                             com.jdroidx.jfindme.data.remote.FirebaseComm.getDevicesRef().child(finalId).child("Identity").child("geofence_sync_requests").child(req.get("id")).child("status").setValue("REJECTED");
                             return null;
                        });
                    }
                }, null);
        }));
        card.addView(row);
        return card;
    }

    private void showSyncRequestDetailsDialog(Map<String, String> req, String zoneId, String zoneName) {
        String group = req.get("group_id");
        if (group == null) return;
        com.jdroidx.jfindme.utils.AngelLoader.show(this);

        String cleanGroup = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(group);
        com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(cleanGroup).child("geofences")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(NotifActivity.this);
                    com.google.firebase.database.DataSnapshot snap = null;
                    String cleanZoneId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(zoneId);
                    if (snapshot.hasChild(cleanZoneId)) snap = snapshot.child(cleanZoneId);
                    else if (zoneId != null && snapshot.hasChild(zoneId)) snap = snapshot.child(zoneId);
                    else {
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            String cName = child.child("name").getValue(String.class);
                            if (cName != null && cName.equalsIgnoreCase(zoneName)) {
                                snap = child;
                                break;
                            }
                        }
                    }

                    String addr = (snap != null && snap.hasChild("address")) ? snap.child("address").getValue(String.class) : "Perimeter Anchor";
                    Double rad = (snap != null && snap.hasChild("radius")) ? snap.child("radius").getValue(Double.class) : 100.0;
                    Integer start = (snap != null && snap.hasChild("startTime")) ? snap.child("startTime").getValue(Integer.class) : 0;
                    Integer end = (snap != null && snap.hasChild("endTime")) ? snap.child("endTime").getValue(Integer.class) : 2359;

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(NotifActivity.this);
                    builder.setTitle("🛡️ SAFE ZONE SPECIFICATIONS");

                    LinearLayout root = new LinearLayout(NotifActivity.this);
                    root.setOrientation(LinearLayout.VERTICAL);
                    int p = getResponsivePaddingQuota();
                    root.setPadding(p, p, p, p);

                    TextView tvName = new TextView(NotifActivity.this);
                    tvName.setText("Zone: " + zoneName);
                    tvName.setTextColor(getTextColor());
                    tvName.setTypeface(null, Typeface.BOLD);
                    UiArchitect.applyText(NotifActivity.this, tvName, "TEXT_TITLE");
                    root.addView(tvName);

                    TextView tvAddr = new TextView(NotifActivity.this);
                    tvAddr.setText("📍 Location: " + addr);
                    tvAddr.setTextColor(getSecondaryTextColor());
                    tvAddr.setPadding(0, scale(8), 0, scale(8));
                    UiArchitect.applyText(NotifActivity.this, tvAddr, "TEXT_BODY");
                    root.addView(tvAddr);

                    TextView tvRad = new TextView(NotifActivity.this);
                    tvRad.setText("📏 Radius: " + (rad != null ? rad.intValue() : 100) + " meters");
                    tvRad.setTextColor(getSecondaryTextColor());
                    UiArchitect.applyText(NotifActivity.this, tvRad, "TEXT_BODY");
                    root.addView(tvRad);

                    TextView tvTime = new TextView(NotifActivity.this);
                    int sH = (start != null ? start : 0) / 100; int sM = (start != null ? start : 0) % 100;
                    int eH = (end != null ? end : 2359) / 100; int eM = (end != null ? end : 2359) % 100;
                    tvTime.setText(String.format(Locale.getDefault(), "🕒 Active Hours: %02d:%02d to %02d:%02d", sH, sM, eH, eM));
                    tvTime.setTextColor(getSecondaryTextColor());
                    tvTime.setPadding(0, scale(8), 0, scale(8));
                    UiArchitect.applyText(NotifActivity.this, tvTime, "TEXT_BODY");
                    root.addView(tvTime);

                    builder.setView(root);
                    builder.setPositiveButton("ACCEPT ZONE", (d, w) -> processSyncAcceptance(req.get("id"), zoneId, zoneName, group, req));
                    builder.setNegativeButton("DISMISS", null);

                    android.app.AlertDialog dialog = builder.create();
                    dialog.show();
                    applyDialogTheme(dialog);
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    com.jdroidx.jfindme.utils.AngelLoader.hide(NotifActivity.this);
                    Toast.makeText(NotifActivity.this, "Failed to load zone details.", Toast.LENGTH_SHORT).show();
                }
            });
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

    private void processSyncAcceptance(String reqId, String zoneId, String name, String group, Map<String, String> req) {
        String cleanZoneId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(zoneId);
        Double reqLat = null; Double reqLng = null; Double reqRad = null;
        if (req != null) {
            try {
                if (req.get("lat") != null && !req.get("lat").isEmpty()) reqLat = Double.parseDouble(req.get("lat"));
                if (req.get("lng") != null && !req.get("lng").isEmpty()) reqLng = Double.parseDouble(req.get("lng"));
                if (req.get("radius") != null && !req.get("radius").isEmpty()) reqRad = Double.parseDouble(req.get("radius"));
            } catch (Exception ignored) {}
        }

        if (reqLat != null && reqLng != null && reqRad != null) {
            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(NotifActivity.this);
            android.content.ContentValues v = new android.content.ContentValues();
            v.put("zone_id", cleanZoneId); v.put("name", name); v.put("lat", reqLat); v.put("lng", reqLng); v.put("radius", reqRad); v.put("st_sync_state", "ACCEPTED");
            v.put("flag_a", 1); v.put("flag_b", 1);
            db.saveSafeZone(v);
            
            com.jdroidx.jfindme.engine.location.GeofenceHelper helper = new com.jdroidx.jfindme.engine.location.GeofenceHelper(NotifActivity.this);
            String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            helper.addGeofence(cleanZoneId + (myUid != null ? "___" + myUid : ""), reqLat, reqLng, reqRad.floatValue());
            
            if (myUid != null) {
                com.jdroidx.jfindme.data.remote.FirebaseComm.resolveHardwareIdentity(myUid, agentId -> {
                    String finalId = agentId != null ? agentId : myUid;
                    String cleanReqId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(reqId);
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getDevicesRef().child(finalId).child("Identity").child("geofence_sync_requests").child(cleanReqId).child("status").setValue("SYNC_CONFIRMED");
                    return null;
                });
            }
            Toast.makeText(NotifActivity.this, "Safe Zone '" + name + "' activated on hardware! ✅", Toast.LENGTH_SHORT).show();
            return;
        }

        com.jdroidx.jfindme.utils.AngelLoader.show(this);
        String cleanGroup = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(group);
        com.jdroidx.jfindme.data.remote.FirebaseComm.getRelationshipsRef().child(cleanGroup).child("geofences")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    com.google.firebase.database.DataSnapshot snap = null;
                    if (snapshot.hasChild(cleanZoneId)) snap = snapshot.child(cleanZoneId);
                    else if (zoneId != null && snapshot.hasChild(zoneId)) snap = snapshot.child(zoneId);
                    else {
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            String cName = child.child("name").getValue(String.class);
                            if (cName != null && cName.equalsIgnoreCase(name)) {
                                snap = child;
                                break;
                            }
                        }
                    }

                    if (snap != null && snap.exists()) {
                        Double lat = snap.child("lat").getValue(Double.class);
                        Double lng = snap.child("lng").getValue(Double.class);
                        Double rad = snap.child("radius").getValue(Double.class);
                        if (lat != null && lng != null && rad != null) {
                            com.jdroidx.jfindme.data.local.DatabaseHelper db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(NotifActivity.this);
                            android.content.ContentValues v = new android.content.ContentValues();
                            v.put("zone_id", cleanZoneId); v.put("name", name); v.put("lat", lat); v.put("lng", lng); v.put("radius", rad); v.put("st_sync_state", "ACCEPTED");
                            v.put("flag_a", 1); v.put("flag_b", 1);
                            db.saveSafeZone(v);
                            
                            com.jdroidx.jfindme.engine.location.GeofenceHelper helper = new com.jdroidx.jfindme.engine.location.GeofenceHelper(NotifActivity.this);
                            String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                            helper.addGeofence(cleanZoneId + (myUid != null ? "___" + myUid : ""), lat, lng, rad.floatValue());
                            
                            if (myUid != null) {
                                com.jdroidx.jfindme.data.remote.FirebaseComm.resolveHardwareIdentity(myUid, agentId -> {
                                    String finalId = agentId != null ? agentId : myUid;
                                    String cleanReqId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizePathKey(reqId);
                                    com.jdroidx.jfindme.data.remote.FirebaseComm.getDevicesRef().child(finalId).child("Identity").child("geofence_sync_requests").child(cleanReqId).child("status").setValue("SYNC_CONFIRMED");
                                    return null;
                                });
                            }
                            Toast.makeText(NotifActivity.this, "Safe Zone '" + name + "' activated on hardware! ✅", Toast.LENGTH_SHORT).show();
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
            if (requests == null || requests.isEmpty()) {
                LinearLayout emptyBox = new LinearLayout(this);
                emptyBox.setOrientation(LinearLayout.VERTICAL);
                emptyBox.setGravity(Gravity.CENTER);
                int p = getResponsivePaddingQuota();
                emptyBox.setPadding(p * 2, p, p * 2, p);
                
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("🟢 ALL SYNC REQUESTS PROCESSED");
                tvEmpty.setTextColor(Color.parseColor("#00E676"));
                tvEmpty.setTypeface(null, Typeface.BOLD);
                UiArchitect.applyText(this, tvEmpty, "TEXT_LABEL");
                emptyBox.addView(tvEmpty);
                
                TextView tvSub = new TextView(this);
                tvSub.setText("No pending geofence or pairing requests awaiting approval.");
                tvSub.setTextColor(getSecondaryTextColor());
                tvSub.setTextSize(11f);
                tvSub.setPadding(0, 5, 0, 0);
                emptyBox.addView(tvSub);
                
                syncContainer.addView(emptyBox);
            } else {
                for (Map<String, String> req : requests) {
                    syncContainer.addView(createSyncApprovalCard(req));
                }
            }
        });
    }
}

