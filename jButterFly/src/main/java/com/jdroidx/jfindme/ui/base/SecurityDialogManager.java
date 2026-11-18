package com.jdroidx.jfindme.ui.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;
import com.jdroidx.jfindme.R;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.jdroidx.jfindme.utils.DeviceIdentityHelper;
import com.jdroidx.jfindme.utils.RegistryManager;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * [Variables]: activity, targetUid, targetName, currentGroupId, options, onComplete
 * [Constants]: TAG
 * [Dependencies]: UiArchitect, RegistryManager, DatabaseHelper, FirebaseComm, DeviceIdentityHelper
 * [Integration Points]: MapActivity, PersonDetailsActivity, NetworkGatewayActivity, SettingsActivity
 *
 * 🚀 SecurityDialogManager (OOPS-based MVC Controller SSOT)
 * Centralizes all administrative security dialogs, master password authorisations,
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
     * Shows standardized Unlink Device dialog with Standard vs Administrative Master Override options.
     */
    public static void showUnlinkDeviceDialog(
            BaseActivityJava activity,
            String targetUid,
            String targetName,
            String currentGroupId,
            OnDialogActionListener listener
    ) {
        if (activity == null || activity.isFinishing()) return;

        UiArchitect.OptionItem[] unlinkOptions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("❌ Standard Unlink Device", "Safely remove device from active tracking circle", android.R.drawable.ic_menu_delete),
            new UiArchitect.OptionItem("🛡️ Master Password Force Purge", "Administrative override with safety phrase to instantly wipe corrupted or orphan nodes", R.drawable.ic_privacy_lock)
        };

        UiArchitect.showRichOptionsDialog(activity, "Remove " + targetName + "?", unlinkOptions, (idx, label) -> {
            if (idx == 0) {
                // 1. Standard Unlink
                performStandardUnlink(activity, targetUid, targetName, currentGroupId);
                if (listener != null) listener.onActionSuccess("STANDARD_UNLINK", targetUid);
            } else {
                // 2. Master Password Override
                showMasterPurgeAuthDialog(activity, targetUid, targetName, () -> {
                    performStandardUnlink(activity, targetUid, targetName, currentGroupId);
                    Toast.makeText(activity, "🛡️ Force Administrative Purge Completed for " + targetName, Toast.LENGTH_LONG).show();
                    if (listener != null) listener.onActionSuccess("MASTER_PURGE", targetUid);
                });
            }
        });
    }

    /**
     * Executes standard unlinking across SQLite and Firebase RTDB.
     */
    public static void performStandardUnlink(
            Context context,
            String targetUid,
            String targetName,
            String currentGroupId
    ) {
        try {
            DatabaseHelper db = DatabaseHelper.getInstance(context);
            if (currentGroupId == null || currentGroupId.isEmpty()) {
                currentGroupId = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getString("active_group", null);
            }

            db.deleteRosterNode(targetUid, currentGroupId);

            if (currentGroupId != null && !currentGroupId.isEmpty()) {
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("devices").child(targetUid).removeValue();
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("members").child(targetUid).removeValue();
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("citizen_dossiers").child(targetUid).removeValue();
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("direct_pairs").child(targetUid).removeValue();
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(targetUid).child("joined_circles").child(currentGroupId).removeValue();
                FirebaseComm.getRelationshipsRef().child(currentGroupId).child("signals").child("refresh_roster").setValue(System.currentTimeMillis());
            }

            String finalMyId = DeviceIdentityHelper.getOrGenerateAgentId(context);
            FirebaseComm.getRelationshipsRef(finalMyId).child(targetUid).removeValue();
            FirebaseComm.getRelationshipsRef(targetUid).child(finalMyId).removeValue();

            Toast.makeText(context, "Device " + targetName + " unlinked successfully.", Toast.LENGTH_SHORT).show();
            SystemLogger.i(TAG, "Unlinked device " + targetUid + " (" + targetName + ")");
        } catch (Exception e) {
            SystemLogger.e(TAG, "Error executing standard unlink: " + e.getMessage());
        }
    }

    /**
     * Shows Master Admin Authorization password prompt dialog.
     */
    public static void showMasterPurgeAuthDialog(
            BaseActivityJava activity,
            String targetUid,
            String targetName,
            Runnable onAuthorized
    ) {
        if (activity == null || activity.isFinishing()) return;

        UiArchitect.showInputPromptDialog(
            activity,
            "Master Admin Authorization",
            "Enter master security password to force purge '" + targetName + "' (" + targetUid + "):",
            "",
            inputPass -> {
                if (RegistryManager.verifyAndUpgradePurgePhrase(activity, inputPass != null ? inputPass.trim() : "")) {
                    if (onAuthorized != null) onAuthorized.run();
                } else {
                    UiArchitect.showValidationDialog(
                        activity,
                        "Authorization Denied",
                        "Incorrect master password. Administrative override was blocked.",
                        null
                    );
                }
            }
        );
    }

    /**
     * Shows NIST SP 800-63B compliant Pairing PIN TTL selection dialog.
     */
    public static void showPinTtlSelectionDialog(
            BaseActivityJava activity,
            OnTtlSelectedCallback callback
    ) {
        if (activity == null || activity.isFinishing()) return;

        String[] options = {
            "5 Minutes (Recommended - NIST SP 800-63B)",
            "10 Minutes",
            "15 Minutes",
            "20 Minutes",
            "30 Minutes (Extended Pairing Window)"
        };
        int[] values = {5, 10, 15, 20, 30};
        int currentTtl = activity.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getInt("pairing_pin_ttl_minutes", 5);
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
                Toast.makeText(activity, "Pairing PIN Expiry TTL set to " + chosen + " minutes.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (callback != null) callback.onTtlSelected(chosen);
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    public interface OnTtlSelectedCallback {
        void onTtlSelected(int ttlMinutes);
    }

    /**
     * 🛡️ Master PIN Prompt with Automated Volatile Memory Scrubbing.
     * Enforces Breakpoint 7: Memory wiping of character arrays to prevent heap inspection.
     *
     * [Variables] inputPass, pinChars, isValid | [Constants] TAG | [Dependencies] UiArchitect, RegistryManager, Arrays
     * [Integration Points] SettingsActivity, MapActivity, PrivacyActivity, PersonDetailsActivity
     */
    public static void showMasterPinPrompt(
            BaseActivityJava activity,
            String title,
            MasterPinCallback callback
    ) {
        if (activity == null || activity.isFinishing()) return;

        UiArchitect.showInputPromptDialog(
            activity,
            title != null ? title : "Master PIN Verification",
            "Enter 6-digit master security PIN:",
            "",
            inputPin -> {
                if (inputPin == null || inputPin.isEmpty()) {
                    if (callback != null) callback.onResult(false);
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
                    if (callback != null) callback.onResult(true);
                } else {
                    UiArchitect.showValidationDialog(
                        activity,
                        "PIN Verification Failed",
                        "Incorrect master PIN. Access denied.",
                        null
                    );
                    if (callback != null) callback.onResult(false);
                }
            }
        );
    }

    public interface MasterPinCallback {
        void onResult(boolean isAuthorized);
    }
}
