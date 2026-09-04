package com.jdroidx.jfindme.ui.alert;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.jdroidx.jfindme.engine.alert.AcousticBeaconEngine;
import com.jdroidx.jfindme.utils.AvatarLoader;
import com.jdroidx.jfindme.utils.RegistryManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EmergencyAlertHelper: Presents an interactive target-selection dialog showing
 * all connected family & peer devices before dispatching high-priority acoustic sirens.
 */
public class EmergencyAlertHelper {

    public static void showPanicDispatchDialog(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        DatabaseHelper db = DatabaseHelper.getInstance(activity);
        List<Map<String, Object>> roster = db.getOfflineRoster();

        SharedPreferences prefs = activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE);
        String selfAuthUid = FirebaseAuth.getInstance().getUid();
        String selfAgentId = prefs.getString("agent_id", "");
        String selfDevUid = prefs.getString("device_uid", "");
        String resolvedSelf = (selfAuthUid != null && !selfAuthUid.isEmpty()) ? db.resolveAgentIdLocally(selfAuthUid) : null;

        // Filter valid connected peer nodes (exclude self)
        List<ConnectedDeviceTarget> targets = new ArrayList<>();
        Set<String> seenUids = new HashSet<>();

        if (roster != null) {
            for (Map<String, Object> item : roster) {
                String u1 = item.get("uid_1") != null ? item.get("uid_1").toString().trim() : "";
                String u2 = item.get("uid_2") != null ? item.get("uid_2").toString().trim() : "";
                String rawUid = !u1.isEmpty() ? u1 : u2;
                String uid = rawUid.replace("HW_", "").trim().toUpperCase();

                if (uid.isEmpty() || seenUids.contains(uid)) continue;
                if (uid.equalsIgnoreCase(selfAuthUid) || uid.equalsIgnoreCase(selfAgentId) || uid.equalsIgnoreCase(selfDevUid)) continue;
                if (resolvedSelf != null && uid.equalsIgnoreCase(resolvedSelf)) continue;

                seenUids.add(uid);
                String name = item.get("fullName") != null ? item.get("fullName").toString() : "Connected Node";
                String role = item.get("role_type") != null ? item.get("role_type").toString() : "Peer";
                String avatarId = item.get("avatar_id") != null ? item.get("avatar_id").toString() : "1";

                targets.add(new ConnectedDeviceTarget(uid, name, role, avatarId));
            }
        }

        // Build Dialog View
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F172A")); // Deep Slate / Emergency Night
        int pad = (int) (18 * activity.getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        // Header Bundle
        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView alertIcon = new ImageView(activity);
        alertIcon.setImageResource(android.R.drawable.ic_dialog_alert);
        alertIcon.setColorFilter(Color.parseColor("#EF4444")); // Crimson
        int iconSize = (int) (32 * activity.getResources().getDisplayMetrics().density);
        titleRow.addView(alertIcon, new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("🚨 EMERGENCY SOS BROADCAST");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding((int) (10 * activity.getResources().getDisplayMetrics().density), 0, 0, 0);
        titleRow.addView(tvTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(titleRow);

        TextView tvSub = new TextView(activity);
        tvSub.setText("Select connected devices to trigger high-priority acoustic siren (30s):");
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setTextSize(12f);
        tvSub.setPadding(0, (int) (8 * activity.getResources().getDisplayMetrics().density), 0, (int) (12 * activity.getResources().getDisplayMetrics().density));
        root.addView(tvSub);

        // Local Device Ring Checkbox
        CheckBox cbLocalDevice = new CheckBox(activity);
        cbLocalDevice.setText("🔊 Sound Siren on THIS Device Also");
        cbLocalDevice.setTextColor(Color.parseColor("#FCD34D")); // Amber gold
        cbLocalDevice.setTextSize(13f);
        cbLocalDevice.setTypeface(null, Typeface.BOLD);
        cbLocalDevice.setChecked(true);
        cbLocalDevice.setPadding((int) (8 * activity.getResources().getDisplayMetrics().density), 0, 0, (int) (8 * activity.getResources().getDisplayMetrics().density));
        root.addView(cbLocalDevice);

        // Separator
        View divider = new View(activity);
        divider.setBackgroundColor(Color.parseColor("#334155"));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        divLp.setMargins(0, 4, 0, 8);
        root.addView(divider, divLp);

        // Scrollable Connected Devices List
        ScrollView scroll = new ScrollView(activity);
        LinearLayout deviceList = new LinearLayout(activity);
        deviceList.setOrientation(LinearLayout.VERTICAL);

        final List<CheckBox> targetCheckboxes = new ArrayList<>();

        if (targets.isEmpty()) {
            TextView emptyNotice = new TextView(activity);
            emptyNotice.setText("No external connected family or friend devices discovered.\nYou can still trigger the acoustic siren locally on this hardware.");
            emptyNotice.setTextColor(Color.parseColor("#CBD5E1"));
            emptyNotice.setTextSize(12f);
            emptyNotice.setPadding(0, 16, 0, 16);
            deviceList.addView(emptyNotice);
        } else {
            for (ConnectedDeviceTarget target : targets) {
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 10, 0, 10);

                GradientDrawable rowBg = new GradientDrawable();
                rowBg.setColor(Color.parseColor("#1E293B"));
                rowBg.setCornerRadius(16);
                row.setBackground(rowBg);
                row.setPadding(16, 12, 16, 12);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLp.setMargins(0, 6, 0, 6);

                ImageView av = new ImageView(activity);
                int aSize = (int) (36 * activity.getResources().getDisplayMetrics().density);
                AvatarLoader.bindAvatar(activity, av, target.avatarId);
                row.addView(av, new LinearLayout.LayoutParams(aSize, aSize));

                LinearLayout meta = new LinearLayout(activity);
                meta.setOrientation(LinearLayout.VERTICAL);
                meta.setPadding(16, 0, 16, 0);

                TextView nameTv = new TextView(activity);
                nameTv.setText(target.name);
                nameTv.setTextColor(Color.WHITE);
                nameTv.setTextSize(13f);
                nameTv.setTypeface(null, Typeface.BOLD);
                meta.addView(nameTv);

                TextView roleTv = new TextView(activity);
                roleTv.setText(target.role + " • " + target.uid);
                roleTv.setTextColor(Color.parseColor("#38BDF8")); // Sky Blue
                roleTv.setTextSize(11f);
                meta.addView(roleTv);

                row.addView(meta, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                CheckBox cb = new CheckBox(activity);
                cb.setChecked(true);
                cb.setTag(target);
                targetCheckboxes.add(cb);
                row.addView(cb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                row.setOnClickListener(v -> cb.toggle());
                deviceList.addView(row, rowLp);
            }
        }

        scroll.addView(deviceList);
        int maxHeight = (int) (260 * activity.getResources().getDisplayMetrics().density);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight));

        // Build Dialog
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(root)
            .setPositiveButton("🚨 DISPATCH EMERGENCY SIREN", null)
            .setNegativeButton("CANCEL", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#EF4444"));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int dispatchedCount = 0;
                List<String> dispatchedNames = new ArrayList<>();

                int sirenDuration = RegistryManager.getSirenDurationSec(activity);
                for (CheckBox cb : targetCheckboxes) {
                    if (cb.isChecked() && cb.getTag() instanceof ConnectedDeviceTarget) {
                        ConnectedDeviceTarget dt = (ConnectedDeviceTarget) cb.getTag();
                        FirebaseComm.triggerRemoteRingDevice(dt.uid, sirenDuration);
                        FirebaseComm.triggerRemoteLocateDevice(dt.uid);
                        dispatchedCount++;
                        dispatchedNames.add(dt.name);
                    }
                }

                // Global circle fan-out
                FirebaseComm.broadcastEmergencySos(activity, sirenDuration);

                boolean ringLocal = cbLocalDevice.isChecked();
                if (ringLocal) {
                    AcousticBeaconEngine.startSiren(activity, sirenDuration, "Local Panic Trigger");
                }

                if (dispatchedCount > 0) {
                    Toast.makeText(activity, "🚨 Emergency siren dispatched to " + dispatchedCount + " connected device(s)!", Toast.LENGTH_LONG).show();
                } else if (ringLocal) {
                    Toast.makeText(activity, "🚨 Local acoustic siren activated (" + sirenDuration + "s)!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Emergency SOS beacon broadcasted to network.", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
            });
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#94A3B8"));
        });

        dialog.show();
    }

    private static class ConnectedDeviceTarget {
        final String uid;
        final String name;
        final String role;
        final String avatarId;

        ConnectedDeviceTarget(String uid, String name, String role, String avatarId) {
            this.uid = uid;
            this.name = name;
            this.role = role;
            this.avatarId = avatarId;
        }
    }
}
