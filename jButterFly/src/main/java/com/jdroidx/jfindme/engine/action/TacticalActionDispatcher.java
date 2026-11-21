package com.jdroidx.jfindme.engine.action;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.jdroidx.jfindme.R;
import com.jdroidx.jfindme.data.local.DatabaseHelper;
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.data.remote.FirebaseComm;
import com.jdroidx.jfindme.ui.base.BaseActivityJava;
import com.jdroidx.jfindme.ui.base.SecurityDialogManager;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.ui.group.PersonDetailsActivity;
import com.jdroidx.jfindme.ui.management.GeofenceActivity;
import com.jdroidx.jfindme.ui.management.HistoryActivity;
import com.jdroidx.jfindme.ui.map.ButterflyPointDialog;
import com.jdroidx.jfindme.utils.AngelLoader;
import com.jdroidx.jfindme.utils.DeviceIdentityHelper;
import com.jdroidx.jfindme.utils.DevicePresentationResolver;
import com.jdroidx.jfindme.utils.RegistryManager;

import java.util.Locale;

/**
 * 🎯 TacticalActionDispatcher (OOPS MVC Subsystem - Phase 2)
 *
 * Centralizes peer action sheets and operational intent dispatches across all activities
 * (MapActivity, FamilyFriendsActivity, PersonDetailsActivity, NetworkGatewayActivity).
 * Eliminates duplicate raw Intent declarations, ad-hoc dialogs, and inconsistent parameter bundles.
 *
 * Prime Directives:
 * - Anti-Null-Island coordinates validation before launching turn-by-turn navigation or copying.
 * - Centralized offline warning for acoustic siren triggers.
 * - Call-by-reference integration to keep memory footprint minimal.
 *
 * [Variables] uid, displayName, latitude, longitude, role, isOnline, batteryLevel
 * [Constants] OptionItem descriptors, URI schemes, Intent extra keys
 * [Dependencies] UiArchitect, FirebaseComm, RegistryManager, DatabaseHelper, DevicePresentationResolver
 * [Integration Points] MapActivity, FamilyFriendsActivity, PersonDetailsActivity, HistoryActivity, GeofenceActivity
 */
public class TacticalActionDispatcher {

    /**
     * Immutable data carrier representing an actionable target node.
     */
    public static class TacticalTarget {
        public final String uid;
        public final String displayName;
        public final double latitude;
        public final double longitude;
        public final String role;
        public final String roleFamily;
        public final String roleFriend;
        public final String roleTag;
        public final boolean isOnline;
        public final int batteryLevel;
        public final String avatarId;

        public TacticalTarget(String uid, String displayName, double latitude, double longitude,
                              String role, String roleFamily, String roleFriend, String roleTag,
                              boolean isOnline, int batteryLevel, String avatarId) {
            this.uid = uid != null ? uid : "";
            this.displayName = displayName != null ? displayName : "Unknown Device";
            this.latitude = latitude;
            this.longitude = longitude;
            this.role = role != null ? role : "Family Member";
            this.roleFamily = roleFamily != null ? roleFamily : "None";
            this.roleFriend = roleFriend != null ? roleFriend : "None";
            this.roleTag = roleTag != null ? roleTag : "";
            this.isOnline = isOnline;
            this.batteryLevel = batteryLevel;
            this.avatarId = avatarId != null ? avatarId : "butterfly";
        }

        public static TacticalTarget fromUserLocation(Context context, UserLocation loc) {
            if (loc == null) return null;
            String uid = loc.getResolvedUserId();
            String name = DevicePresentationResolver.resolveDisplayName(context, uid, loc.getUserName());
            String role = DevicePresentationResolver.resolveRoleString(loc);
            String roleTag = DevicePresentationResolver.resolveRoleTag(loc);
            boolean online = (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0);
            return new TacticalTarget(
                uid,
                name,
                loc.getLatitude(),
                loc.getLongitude(),
                role,
                loc.getRole_family(),
                loc.getRole_friend(),
                roleTag,
                online,
                loc.getBatteryLevel(),
                loc.getAvatar_id()
            );
        }

        public static TacticalTarget create(String uid, String name, double lat, double lng, String role, boolean isOnline) {
            return new TacticalTarget(uid, name, lat, lng, role, "None", "None", "", isOnline, 100, "butterfly");
        }
    }

    /**
     * Callback interface for operations requiring hosting activity delegation (e.g. Map tracking, custom wizards).
     */
    public interface ActionCallback {
        default void onFindLocationRequested(TacticalTarget target) {}
        default void onDeviceSharingRequested(TacticalTarget target) {}
        default void onButterflyPointRequested(TacticalTarget target) {}
        default void onUnlinkRequested(TacticalTarget target) {}
        default void onSelfActionRequested() {}
        default void onProfileRefreshed(TacticalTarget target) {}
    }

    /**
     * Displays the Master 11-Operation Tactical Target Action Sheet.
     */
    public static void showTacticalActionSheet(BaseActivityJava activity, TacticalTarget target, ActionCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (target == null || target.uid.trim().isEmpty()) {
            UiArchitect.showValidationDialog(activity, "Target Identity Missing",
                "Cannot perform action: Target device identity is undefined or unacquired.", null);
            return;
        }

        String myUid = FirebaseAuth.getInstance().getUid();
        String myAgentId = DeviceIdentityHelper.getOrGenerateAgentId(activity);
        if (target.uid.equals(myUid) || target.uid.equalsIgnoreCase(myAgentId)) {
            if (callback != null) {
                callback.onSelfActionRequested();
            }
            return;
        }

        final int sirenDuration = RegistryManager.getSirenDurationSec(activity);

        UiArchitect.OptionItem[] targetActions = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("📍 Find Current Location (Center & Track Live)", "Lock camera directly on target's live coordinate stream", android.R.drawable.ic_menu_mylocation),
            new UiArchitect.OptionItem("🧭 Turn-by-Turn Navigation", "Launch Google Maps directions to target", android.R.drawable.ic_dialog_map),
            new UiArchitect.OptionItem("⏱️ View Location History", "Inspect chronological path timeline and telemetry logs", R.drawable.ic_location_history),
            new UiArchitect.OptionItem("📱 Device Sharing (Connect / Roles)", "Auto-connect " + target.displayName + " to peers with mutual roles (Family, Friends, Co-Guardians) & 24h cloud sync", R.drawable.ic_family_friends),
            new UiArchitect.OptionItem("🦋 Butterfly Point (Rendezvous & Proximity)", "Set up a shared meeting location, track mutual approach distances, and monitor arrival", R.drawable.ic_butterfly_point),
            new UiArchitect.OptionItem("🛡️ Assign / Create Safe Zone", "Configure geofence boundary perimeter for this member", R.drawable.ic_safe_zone),
            new UiArchitect.OptionItem("🔊 Ring Device (Acoustic Siren)", "Trigger high-volume emergency siren (" + sirenDuration + "s) on target device", android.R.drawable.ic_lock_idle_alarm),
            new UiArchitect.OptionItem("👤 View Member Details & Roles", "Open dossier, ACL permissions, and contact details", R.drawable.ic_user_profile),
            new UiArchitect.OptionItem("🔄 Refresh Latest Profile Data", "Pull and re-sync latest profile dossier, avatar, and callsign from Cloud SSOT", android.R.drawable.ic_popup_sync),
            new UiArchitect.OptionItem("📋 Copy GPS Coordinates", "Copy exact latitude and longitude to clipboard", android.R.drawable.ic_menu_save),
            new UiArchitect.OptionItem("❌ Unlink / Remove Device", "Uncouple this device from your active tracking network", android.R.drawable.ic_menu_delete)
        };

        UiArchitect.showRichOptionsDialog(activity, "🎯 TARGET: " + target.displayName.toUpperCase() + target.roleTag, targetActions, (actIdx, actLabel) -> {
            String label = actLabel != null ? actLabel : "";
            if (label.contains("Find Current Location")) {
                if (callback != null) {
                    callback.onFindLocationRequested(target);
                } else {
                    FirebaseComm.triggerRemoteLocateDevice(target.uid);
                    Toast.makeText(activity, "📍 Satellite ping dispatched to " + target.displayName, Toast.LENGTH_SHORT).show();
                }
            } else if (label.contains("Turn-by-Turn Navigation")) {
                launchNavigation(activity, target.latitude, target.longitude, target.displayName);
            } else if (label.contains("Location History")) {
                launchHistory(activity, target.uid, target.displayName);
            } else if (label.contains("Device Sharing") || label.contains("Connect")) {
                if (callback != null) {
                    callback.onDeviceSharingRequested(target);
                }
            } else if (label.contains("Butterfly Point")) {
                if (callback != null) {
                    callback.onButterflyPointRequested(target);
                } else if (activity instanceof com.jdroidx.jfindme.ui.map.MapActivity) {
                    ButterflyPointDialog.showForTarget((com.jdroidx.jfindme.ui.map.MapActivity) activity, target.uid, target.displayName, target.latitude, target.longitude);
                }
            } else if (label.contains("Safe Zone")) {
                launchSafeZone(activity, target.uid, target.displayName, target.latitude, target.longitude);
            } else if (label.contains("Ring Device")) {
                triggerAcousticSiren(activity, target.uid, target.displayName, target.isOnline);
            } else if (label.contains("Member Details")) {
                launchDossierDetails(activity, target.uid, target.displayName, target.role, target.roleFamily, target.roleFriend);
            } else if (label.contains("Refresh Latest Profile")) {
                refreshTargetProfile(activity, target.uid, target.displayName, () -> {
                    if (callback != null) callback.onProfileRefreshed(target);
                });
            } else if (label.contains("Copy GPS Coordinates")) {
                copyCoordinatesToClipboard(activity, target.latitude, target.longitude, target.displayName);
            } else if (label.contains("Unlink / Remove Device")) {
                if (callback != null) {
                    callback.onUnlinkRequested(target);
                } else {
                    String activeGroup = RegistryManager.getActiveGroup(activity);
                    SecurityDialogManager.showUnlinkDeviceDialog(activity, target.uid, target.displayName, activeGroup, null);
                }
            }
        });
    }

    /**
     * 🧭 Launches Turn-by-Turn GPS Navigation with Anti-Null-Island guard.
     */
    public static void launchNavigation(Context context, double lat, double lng, String targetName) {
        if (context == null) return;
        if (Math.abs(lat) < 0.001 && Math.abs(lng) < 0.001) {
            Toast.makeText(context, "⚠️ Target " + targetName + " has no valid GPS fix.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Uri navUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
            Intent navIntent = new Intent(Intent.ACTION_VIEW, navUri);
            navIntent.setPackage("com.google.android.apps.maps");
            if (navIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(navIntent);
            } else {
                Uri geoUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(targetName) + ")");
                Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
                context.startActivity(geoIntent);
            }
        } catch (Exception e) {
            try {
                Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
                context.startActivity(new Intent(Intent.ACTION_VIEW, webUri));
            } catch (Exception ex) {
                Toast.makeText(context, "Could not open map navigation.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * ⏱️ Launches Location History Activity for target.
     */
    public static void launchHistory(Context context, String targetUid, String targetName) {
        if (context == null || targetUid == null) return;
        Intent intent = new Intent(context, HistoryActivity.class);
        intent.putExtra("target_uid", targetUid);
        intent.putExtra("TARGET_UID", targetUid);
        intent.putExtra("target_name", targetName);
        intent.putExtra("TARGET_NAME", targetName);
        context.startActivity(intent);
    }

    /**
     * 🛡️ Launches Geofence Configuration for target.
     */
    public static void launchSafeZone(Context context, String targetUid, String targetName, double lat, double lng) {
        if (context == null || targetUid == null) return;
        Intent intent = new Intent(context, GeofenceActivity.class);
        intent.putExtra("target_uid", targetUid);
        intent.putExtra("TARGET_UID", targetUid);
        intent.putExtra("target_name", targetName);
        intent.putExtra("TARGET_NAME", targetName);
        intent.putExtra("TARGET_LAT", lat);
        intent.putExtra("TARGET_LNG", lng);
        context.startActivity(intent);
    }

    /**
     * 👤 Launches Member Dossier & Details for target.
     */
    public static void launchDossierDetails(Context context, String targetUid, String targetName,
                                            String role, String roleFamily, String roleFriend) {
        if (context == null || targetUid == null) return;
        Intent intent = new Intent(context, PersonDetailsActivity.class);
        intent.putExtra("TARGET_UID", targetUid);
        intent.putExtra("target_uid", targetUid);
        intent.putExtra("TARGET_NAME", targetName);
        intent.putExtra("target_name", targetName);
        intent.putExtra("TARGET_ROLE_FAMILY", roleFamily);
        intent.putExtra("TARGET_ROLE_FRIEND", roleFriend);
        intent.putExtra("TARGET_ROLE", role);
        intent.putExtra("target_role", role);
        context.startActivity(intent);
    }

    /**
     * 🔊 Triggers Acoustic Siren with Offline Warning and Audio Pipeline Feedback.
     */
    public static void triggerAcousticSiren(BaseActivityJava activity, String targetUid, String targetName, boolean isOnline) {
        if (activity == null || targetUid == null) return;
        if (!isOnline) {
            Toast.makeText(activity, "⚠️ " + targetName + " is offline. Siren command queued via push gateway.", Toast.LENGTH_LONG).show();
        }
        int sirenDuration = RegistryManager.getSirenDurationSec(activity);
        FirebaseComm.triggerRemoteRingDevice(targetUid, sirenDuration);
        UiArchitect.showSlideNotificationBanner(activity, "Acoustic Siren Dispatched",
            sirenDuration + "-second emergency siren sent to " + targetName + ".", false, 4000, null, null);
        Toast.makeText(activity, "🔊 Emergency siren triggered (" + sirenDuration + "s)", Toast.LENGTH_SHORT).show();
    }

    /**
     * 📋 Copies GPS coordinates to clipboard with Anti-Null-Island guard.
     */
    public static void copyCoordinatesToClipboard(Context context, double lat, double lng, String targetName) {
        if (context == null) return;
        if (Math.abs(lat) < 0.001 && Math.abs(lng) < 0.001) {
            Toast.makeText(context, "⚠️ Target " + targetName + " has no acquired GPS coordinates.", Toast.LENGTH_LONG).show();
            return;
        }

        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            String coordStr = String.format(Locale.US, "%.6f, %.6f", lat, lng);
            cm.setPrimaryClip(ClipData.newPlainText("Coordinates", coordStr));
            Toast.makeText(context, "📋 Coordinates Copied: " + coordStr, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 🔄 Fetches latest profile dossier from Cloud SSOT and updates local SQLite.
     */
    public static void refreshTargetProfile(BaseActivityJava activity, String targetUid, String targetName, Runnable onComplete) {
        if (activity == null || targetUid == null) return;
        AngelLoader.show(activity);
        AngelLoader.setStatus(activity, "Fetching latest Cloud Profile for " + targetName + "...");

        FirebaseComm.getProfileRef(targetUid).get().addOnSuccessListener(snapshot -> {
            AngelLoader.hide(activity);
            if (snapshot.exists()) {
                String cloudName = snapshot.child("name").getValue(String.class);
                if (cloudName == null || cloudName.isEmpty()) cloudName = snapshot.child("full_name").getValue(String.class);
                if (cloudName == null || cloudName.isEmpty()) cloudName = snapshot.child("userName").getValue(String.class);
                String cloudAvatar = snapshot.child("avatar_id").getValue(String.class);

                DatabaseHelper localDb = DatabaseHelper.getInstance(activity);
                if (cloudName != null && !cloudName.isEmpty()) {
                    localDb.updateDeviceIdentityAndAvatar(targetUid, cloudName, cloudAvatar);
                }

                UiArchitect.showSlideNotificationBanner(activity, "PROFILE RE-SYNCED",
                    "Refreshed latest dossier for " + (cloudName != null ? cloudName : targetName) + " from Cloud SSOT.",
                    false, 3500, null, null);
                Toast.makeText(activity, "✅ Latest profile data re-synchronized!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "No remote profile found for " + targetName, Toast.LENGTH_SHORT).show();
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }).addOnFailureListener(e -> {
            AngelLoader.hide(activity);
            Toast.makeText(activity, "Failed to refresh profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
