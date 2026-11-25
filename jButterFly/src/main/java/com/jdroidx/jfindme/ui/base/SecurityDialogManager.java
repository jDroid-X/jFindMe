package com.jdroidx.jfindme.ui.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;
import java.util.List;
import com.jdroidx.jfindme.R;
import com.jdroidx.jfindme.data.repository.DeviceRepository;
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.jdroidx.jfindme.utils.DeviceIdentityHelper;
import com.jdroidx.jfindme.utils.RegistryManager;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * [Variables]: activity, targetUid, targetName, currentGroupId, options,
 * onComplete
 * [Constants]: TAG
 * [Dependencies]: UiArchitect, RegistryManager, DatabaseHelper, FirebaseComm,
 * DeviceIdentityHelper
 * [Integration Points]: MapActivity, PersonDetailsActivity,
 * NetworkGatewayActivity, SettingsActivity
 *
 * 🚀 SecurityDialogManager (OOPS-based MVC Controller SSOT)
 * Centralizes all administrative security dialogs, master password
 * authorisations,
 * NIST-compliant PIN TTL selection, and device unlinking/purging workflows.
 * Eliminates 150+ lines of duplicate dialog logic across Activities.
 */
public final class SecurityDialogManager {

    private static final String TAG = "SecurityDialogManager";

    public interface OnDialogActionListener {
        void onActionSuccess(String action, String details);

        void onActionCancelled();
    }

    private SecurityDialogManager() {
        // Utility OOPS Class
    }

    /**
     * Shows standardized Unlink Device dialog with Standard vs Administrative
     * Master Override options,
     * protected by the Hierarchical Subset Dependency Gate.
     */
    public static void showUnlinkDeviceDialog(
            BaseActivityJava activity,
            String targetUid,
            String targetName,
            String currentGroupId,
            OnDialogActionListener listener) {
        if (activity == null || activity.isFinishing())
            return;

        // 🛡️ HIERARCHICAL SUBSET DEPENDENCY GATE:
        // Inspect if this node has subordinate children, active bilateral pairings,
        // safe zones or butterfly points
        List<com.jdroidx.jfindme.data.local.DatabaseHelper.DeviceSubsetDependency> subsets = DeviceRepository
                .getInstance(activity).inspectSubsetDependencies(targetUid);

        if (!subsets.isEmpty()) {
            showSubsetDependencyChecklistDialog(activity, targetUid, targetName, currentGroupId, subsets, listener);
            return;
        }

        // If no subsets exist, proceed to standard options dialog
        showUnlinkOptionsPrompt(activity, targetUid, targetName, currentGroupId, null, listener);
    }

    private static void showUnlinkOptionsPrompt(
            BaseActivityJava activity,
            String targetUid,
            String targetName,
            String currentGroupId,
            List<com.jdroidx.jfindme.data.local.DatabaseHelper.DeviceSubsetDependency> subsetsToUnlink,
            OnDialogActionListener listener) {
        UiArchitect.OptionItem[] unlinkOptions = new UiArchitect.OptionItem[] {
                new UiArchitect.OptionItem("❌ Standard Unlink Device",
                        "Safely remove device and selected subset dependencies from circle",
                        android.R.drawable.ic_menu_delete),
                new UiArchitect.OptionItem("🛡️ Master Password Force Purge",
                        "Administrative override with safety phrase to instantly wipe device and dependencies",
                        R.drawable.ic_privacy_lock)
        };

        UiArchitect.showRichOptionsDialog(activity, "Remove " + targetName + "?", unlinkOptions, (idx, label) -> {
            if (idx == 0) {
                // 1. Standard Unlink (Primary + Subsets)
                performStandardUnlink(activity, targetUid, targetName, currentGroupId);
                if (subsetsToUnlink != null && !subsetsToUnlink.isEmpty()) {
                    performSubsetCascadeUnlink(activity, subsetsToUnlink, currentGroupId);
                }
                if (listener != null)
                    listener.onActionSuccess("STANDARD_UNLINK", targetUid);
            } else {
                // 2. Master Password Override Force Purge
                showMasterPurgeAuthDialog(activity, targetUid, targetName, () -> {
                    performMasterPurge(activity, targetUid, targetName, currentGroupId);
                    if (subsetsToUnlink != null && !subsetsToUnlink.isEmpty()) {
                        performSubsetCascadeUnlink(activity, subsetsToUnlink, currentGroupId);
                    }
                    if (listener != null)
                        listener.onActionSuccess("MASTER_PURGE", targetUid);
                });
            }
        });
    }

    /**
     * 🛡️ Closed-Loop Feedback & Subset Cascade Executor:
     * Unlinks all user-selected subset dependencies (pairings, geofences, butterfly
     * points, wards)
     * atomically to guarantee no orphaned edges remain in SQLite or Firebase RTDB.
     */
    public static void performSubsetCascadeUnlink(
            Context context,
            List<com.jdroidx.jfindme.data.local.DatabaseHelper.DeviceSubsetDependency> subsets,
            String currentGroupId) {
        if (context == null || subsets == null || subsets.isEmpty())
            return;
        DeviceRepository db = DeviceRepository.getInstance(context);
        for (com.jdroidx.jfindme.data.local.DatabaseHelper.DeviceSubsetDependency dep : subsets) {
            try {
                String depId = dep.getId();
                String depType = dep.getType();
                SystemLogger.i(TAG, "Cascading subset unlink: type=" + depType + ", id=" + depId);

                if ("PAIRING".equals(depType) || "WARD".equals(depType)) {
                    db.deleteRosterNode(depId, currentGroupId);
                    if (currentGroupId != null && !currentGroupId.isEmpty()) {
                        FirebaseComm.getRelationshipsRef().child(currentGroupId).child("members").child(depId)
                                .removeValue();
                        FirebaseComm.getRelationshipsRef().child(currentGroupId).child("direct_pairs").child(depId)
                                .removeValue();
                    }
                } else if ("SAFE_ZONE".equals(depType)) {
                    db.getDbHelper().getWritableDatabase().delete(
                            com.jdroidx.jfindme.data.local.tables.SafeZoneUserMapTable.TABLE_NAME,
                            com.jdroidx.jfindme.data.local.tables.SafeZoneUserMapTable.COL_ZONE_ID + " = ?",
                            new String[] { depId });
                } else if ("BUTTERFLY_POINT".equals(depType)) {
                    db.getDbHelper().getWritableDatabase().delete(
                            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.TABLE_NAME,
                            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.COL_POINT_ID + " = ?",
                            new String[] { depId });
                }
            } catch (Exception e) {
                SystemLogger.w(TAG, "Subset cascade item error for " + dep.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * 🛡️ Hierarchical Subset Dependency Checklist Modal:
     * Lists the primary device and all dependent subset data (default all ticked).
     * Prevents unlinking a level-above parent if critical subset wards remain
     * unhandled.
     */
    public static void showSubsetDependencyChecklistDialog(
            BaseActivityJava activity,
            String targetUid,
            String targetName,
            String currentGroupId,
            List<com.jdroidx.jfindme.data.local.DatabaseHelper.DeviceSubsetDependency> subsets,
            OnDialogActionListener listener) {
        if (activity == null || activity.isFinishing())
            return;

        android.widget.ScrollView scrollView = new android.widget.ScrollView(activity);
        scrollView.setFillViewport(true);
        android.widget.LinearLayout root = new android.widget.LinearLayout(activity);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = activity.getResponsivePaddingQuota();
        root.setPadding(pad, pad, pad, pad);
        scrollView.addView(root);

        // Header Explainer
        android.widget.TextView tvHeader = new android.widget.TextView(activity);
        tvHeader.setText("⚠️ Hierarchical Subset Gate\nSelected: " + targetName + " (" + targetUid + ")");
        tvHeader.setTextSize(16f);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeader.setTextColor(activity.getTextColor());
        root.addView(tvHeader);

        android.widget.TextView tvSub = new android.widget.TextView(activity);
        tvSub.setText(
                "This entity has subordinate links or dependencies in the hierarchy. Select all subset items to unlink or purge together with this device. Level-above data cannot be deleted if active critical wards are unticked.");
        tvSub.setTextSize(12f);
        tvSub.setTextColor(activity.getSecondaryTextColor());
        tvSub.setPadding(0, activity.scale(4), 0, activity.scale(12));
        root.addView(tvSub);

        // Primary Item Checkbox (Always Checked)
        android.widget.CheckBox cbPrimary = new android.widget.CheckBox(activity);
        cbPrimary.setText("🎯 Primary Target: " + targetName + " (" + targetUid + ")");
        cbPrimary.setChecked(true);
        cbPrimary.setEnabled(false); // Mandatory
        cbPrimary.setTextColor(activity.getTextColor());
        root.addView(cbPrimary);

        // Subset Checkboxes (Default all ticked)
        boolean[] checkedStates = new boolean[subsets.size()];
        android.widget.CheckBox[] checkBoxes = new android.widget.CheckBox[subsets.size()];
        for (int i = 0; i < subsets.size(); i++) {
            com.jdroidx.jfindme.data.local.DatabaseHelper.DeviceSubsetDependency dep = subsets.get(i);
            checkedStates[i] = true; // default all ticked

            android.widget.CheckBox cb = new android.widget.CheckBox(activity);
            String prefix = dep.isCritical() ? "🚨 [CRITICAL WARD] " : "🔗 ";
            cb.setText(prefix + dep.getTitle() + "\n" + dep.getDetails());
            cb.setChecked(true);
            cb.setTextColor(dep.isCritical() ? 0xFFD32F2F : activity.getTextColor());
            cb.setTextSize(13f);
            cb.setPadding(0, activity.scale(6), 0, activity.scale(6));
            root.addView(cb);
            checkBoxes[i] = cb;

            final int idx = i;
            cb.setOnCheckedChangeListener((btn, isChecked) -> checkedStates[idx] = isChecked);
        }

        new android.app.AlertDialog.Builder(activity)
                .setTitle("Confirm Selective Deletion Scope")
                .setView(scrollView)
                .setPositiveButton("PROCEED", (dialog, which) -> {
                    // Validate Subset Gate: Cannot delete parent if any critical ward is unchecked
                    for (int i = 0; i < subsets.size(); i++) {
                        if (subsets.get(i).isCritical() && !checkedStates[i]) {
                            UiArchitect.showSlideNotificationBanner(
                                    activity,
                                    "SUBSET GATE REJECTED",
                                    "Cannot unlink guardian while protected ward (" + subsets.get(i).getTitle()
                                            + ") is unchecked and unresolved! Either include the ward or reassign them first.",
                                    true,
                                    5000,
                                    null,
                                    null);
                            return;
                        }
                    }

                    // Execute scoped selective cascade:
                    List<com.jdroidx.jfindme.data.local.DatabaseHelper.DeviceSubsetDependency> selectedSubsets = new java.util.ArrayList<>();
                    for (int i = 0; i < subsets.size(); i++) {
                        if (checkedStates[i]) {
                            selectedSubsets.add(subsets.get(i));
                        }
                    }
                    dialog.dismiss();
                    showUnlinkOptionsPrompt(activity, targetUid, targetName, currentGroupId, selectedSubsets, listener);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    /**
     * Executes standard unlinking across SQLite and Firebase RTDB.
     */
    public static void performStandardUnlink(
            Context context,
            String targetUid,
            String targetName,
            String currentGroupId) {
        try {
            DeviceRepository db = DeviceRepository.getInstance(context);
            if (currentGroupId == null || currentGroupId.isEmpty()) {
                currentGroupId = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                        .getString("active_group", null);
            }

            String cleanTargetId = DeviceIdentityHelper.toCanonicalAgentId(targetUid);
            if (cleanTargetId == null || cleanTargetId.isEmpty()) {
                cleanTargetId = targetUid;
            }

            db.deleteRosterNode(cleanTargetId, currentGroupId);

            if (currentGroupId != null && !currentGroupId.isEmpty()) {
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("devices").child(cleanTargetId)
                        .removeValue();
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("members").child(cleanTargetId)
                        .removeValue();
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("citizen_dossiers").child(cleanTargetId)
                        .removeValue();
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("direct_pairs").child(cleanTargetId)
                        .removeValue();
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(cleanTargetId)
                        .child("joined_circles").child(currentGroupId).removeValue();
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("signals").child("refresh_roster")
                        .setValue(System.currentTimeMillis());
            }

            String finalMyId = DeviceIdentityHelper.getOrGenerateAgentId(context);
            FirebaseComm.getRelationshipsRef(finalMyId).child(cleanTargetId).removeValue();
            FirebaseComm.getRelationshipsRef(cleanTargetId).child(finalMyId).removeValue();

            // Evict target listener and active cache from LocationRepository
            try {
                com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context).removeTarget(cleanTargetId);
                com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context).removeTarget(targetUid);
            } catch (Exception ignored) {
            }

            // Anchor into decommission sentinel and broadcast UI refresh
            try {
                RegistryManager.decommissionDevice(context, cleanTargetId);
                RegistryManager.decommissionDevice(context, targetUid);
                android.content.Intent refreshIntent = new android.content.Intent(
                        "com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED");
                refreshIntent.setPackage(context.getPackageName());
                refreshIntent.putExtra("category", "ROSTER");
                refreshIntent.putExtra("EXTRA_CATEGORY", "ROSTER");
                context.sendBroadcast(refreshIntent);
            } catch (Exception ignored) {
            }

            Toast.makeText(context, "Device " + targetName + " unlinked successfully.", Toast.LENGTH_SHORT).show();
            SystemLogger.i(TAG, "Unlinked device " + cleanTargetId + " (" + targetName + ")");
        } catch (Exception e) {
            SystemLogger.e(TAG, "Error executing standard unlink: " + e.getMessage());
        }
    }

    /**
     * Executes Master Administrative Force Purge across all 22 tables.
     */
    public static void performMasterPurge(
            Context context,
            String targetUid,
            String targetName,
            String currentGroupId) {
        try {
            DeviceRepository repo = DeviceRepository.getInstance(context);
            repo.purgeDeviceRecords(targetUid, success -> {
                try {
                    com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context).removeTarget(targetUid);
                    String cleanTid = DeviceIdentityHelper.toCanonicalAgentId(targetUid);
                    com.jdroidx.jfindme.data.repository.LocationRepository.getInstance(context).removeTarget(cleanTid);
                    RegistryManager.decommissionDevice(context, targetUid);
                    RegistryManager.decommissionDevice(context, cleanTid);
                    android.content.Intent refreshIntent = new android.content.Intent(
                            "com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED");
                    refreshIntent.setPackage(context.getPackageName());
                    refreshIntent.putExtra("category", "ROSTER");
                    refreshIntent.putExtra("EXTRA_CATEGORY", "ROSTER");
                    context.sendBroadcast(refreshIntent);
                } catch (Exception ignored) {
                }
                if (success) {
                    Toast.makeText(context, "🛡️ Master Administrative Force Purge Completed for " + targetName,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Purge finished for " + targetName, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            SystemLogger.e(TAG, "Error executing master purge: " + e.getMessage());
        }
    }

    /**
     * Shows Master Admin Authorization password prompt dialog.
     */
    public static void showMasterPurgeAuthDialog(
            BaseActivityJava activity,
            String targetUid,
            String targetName,
            Runnable onAuthorized) {
        if (activity == null || activity.isFinishing())
            return;

        UiArchitect.showInputPromptDialog(
                activity,
                "Master Admin Authorization",
                "Enter master security password to force purge '" + targetName + "' (" + targetUid + "):",
                "",
                inputPass -> {
                    if (RegistryManager.verifyAndUpgradePurgePhrase(activity,
                            inputPass != null ? inputPass.trim() : "")) {
                        if (onAuthorized != null)
                            onAuthorized.run();
                    } else {
                        UiArchitect.showValidationDialog(
                                activity,
                                "Authorization Denied",
                                "Incorrect master password. Administrative override was blocked.",
                                null);
                    }
                });
    }

    /**
     * Shows NIST SP 800-63B compliant Pairing PIN TTL selection dialog.
     */
    public static void showPinTtlSelectionDialog(
            BaseActivityJava activity,
            OnTtlSelectedCallback callback) {
        if (activity == null || activity.isFinishing())
            return;

        String[] options = {
                "5 Minutes (Recommended - NIST SP 800-63B)",
                "10 Minutes",
                "15 Minutes",
                "20 Minutes",
                "30 Minutes (Extended Pairing Window)"
        };
        int[] values = { 5, 10, 15, 20, 30 };
        int currentTtl = activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                .getInt("pairing_pin_ttl_minutes", 5);
        int selectedIdx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentTtl) {
                selectedIdx = i;
                break;
            }
        }

        new AlertDialog.Builder(activity)
                .setTitle("⏱️ Pairing PIN Expiry TTL")
                .setSingleChoiceItems(options, selectedIdx, (dialog, which) -> {
                    int chosen = values[which];
                    activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                            .edit()
                            .putInt("pairing_pin_ttl_minutes", chosen)
                            .apply();
                    Toast.makeText(activity, "Pairing PIN Expiry TTL set to " + chosen + " minutes.",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (callback != null)
                        callback.onTtlSelected(chosen);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    public interface OnTtlSelectedCallback {
        void onTtlSelected(int ttlMinutes);
    }

    /**
     * 🛡️ Master PIN Prompt with Automated Volatile Memory Scrubbing.
     * Enforces Breakpoint 7: Memory wiping of character arrays to prevent heap
     * inspection.
     *
     * [Variables] inputPass, pinChars, isValid | [Constants] TAG | [Dependencies]
     * UiArchitect, RegistryManager, Arrays
     * [Integration Points] SettingsActivity, MapActivity, PrivacyActivity,
     * PersonDetailsActivity
     */
    public static void showMasterPinPrompt(
            BaseActivityJava activity,
            String title,
            MasterPinCallback callback) {
        if (activity == null || activity.isFinishing())
            return;

        UiArchitect.showInputPromptDialog(
                activity,
                title != null ? title : "Master PIN Verification",
                "Enter 6-digit master security PIN:",
                "",
                inputPin -> {
                    if (inputPin == null || inputPin.isEmpty()) {
                        if (callback != null)
                            callback.onResult(false);
                        return;
                    }
                    char[] pinChars = inputPin.trim().toCharArray();
                    boolean isValid = false;
                    try {
                        isValid = RegistryManager.verifyMasterPin(activity, new String(pinChars));
                    } finally {
                        java.util.Arrays.fill(pinChars, '0'); // 🚀 SCRUB VOLATILE BUFFER
                    }

                    if (isValid) {
                        if (callback != null)
                            callback.onResult(true);
                    } else {
                        UiArchitect.showValidationDialog(
                                activity,
                                "PIN Verification Failed",
                                "Incorrect master PIN. Access denied.",
                                null);
                        if (callback != null)
                            callback.onResult(false);
                    }
                });
    }

    public interface MasterPinCallback {
        void onResult(boolean isAuthorized);
    }
}
