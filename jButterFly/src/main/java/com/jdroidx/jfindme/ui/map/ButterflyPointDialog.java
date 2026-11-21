package com.jdroidx.jfindme.ui.map;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import com.google.android.gms.maps.model.LatLng;
import com.jdroidx.jfindme.data.repository.DeviceRepository;
import com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable;
import com.jdroidx.jfindme.data.model.ButterflyDeviceEntry;
import com.jdroidx.jfindme.data.model.ButterflyPoint;
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.engine.map.ButterflyPointEngine;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * [Variables] selectedLat, selectedLng, selectedAddress, selectedName, scheduledTimestamp, selectedDeviceIds, activePoints
 * [Constants] TAG = "ButterflyPointDialog", PADDING_STANDARD = 16, RADIUS_DEFAULT = 50.0
 * [Dependencies] DatabaseHelper, ButterflyPointEngine, UiArchitect, MapActivity
 * [Integration Points] MapActivity.java (ACTION_GHOST click), Firebase RTDB /butterfly_points/{id}, SQLite butterfly_point & butterfly_point_device_map
 *
 * Enterprise Dynamic OOPS-based MVC Dialog for Butterfly Point Rendezvous & Hub Operations.
 */
public class ButterflyPointDialog {

    private static final String TAG = "ButterflyPointDialog";

    // 🎨 Dynamic OOPS Theme Palette Constants
    public static final int COLOR_EMERALD_GREEN = Color.parseColor("#10B981");
    public static final int COLOR_ACCENT_GREEN  = Color.parseColor("#00E676");
    public static final int COLOR_TACTICAL_RED  = Color.parseColor("#EF4444");
    public static final int COLOR_TACTICAL_AMBER = Color.parseColor("#F59E0B");
    public static final int COLOR_TACTICAL_BLUE = Color.parseColor("#3B82F6");
    public static final int COLOR_TACTICAL_CYAN = Color.parseColor("#0EA5E9");
    public static final int COLOR_TEXT_CYAN     = Color.parseColor("#38BDF8");

    // [Variables] activePointsDialogRef, activeDialogActivityRef | [Dependencies] WeakReference, AlertDialog, MapActivity | [Integration Points] Real-time dialog refresh hook
    private static java.lang.ref.WeakReference<AlertDialog> activePointsDialogRef = null;
    private static java.lang.ref.WeakReference<MapActivity> activeDialogActivityRef = null;

    public static void refreshIfOpen() {
        if (activePointsDialogRef != null && activePointsDialogRef.get() != null) {
            AlertDialog dlg = activePointsDialogRef.get();
            if (dlg != null && dlg.isShowing()) {
                MapActivity act = activeDialogActivityRef != null ? activeDialogActivityRef.get() : null;
                if (act != null && !act.isFinishing()) {
                    act.runOnUiThread(() -> {
                        try {
                            if (dlg.isShowing()) {
                                dlg.dismiss();
                                show(act);
                            }
                        } catch (Exception ignored) {}
                    });
                }
            }
        }
    }

    /**
     * Entry point from MapActivity Ghost Icon click
     */
    public static void show(MapActivity activity) {
        if (activity == null || activity.isFinishing()) return;

        DeviceRepository dbHelper = DeviceRepository.getInstance(activity);
        List<ButterflyPoint> activePoints = dbHelper.getActiveButterflyPoints();

        if (activePoints != null && !activePoints.isEmpty()) {
            showActivePointsSheet(activity, activePoints);
        } else {
            showNoActivePointsActionSheet(activity);
        }
    }

    // [Variables] targetPoints, activePoints, preselectedTargetUid, presetLat, presetLng, presetAddress
    // [Constants] TAG
    // [Dependencies] DatabaseHelper, ButterflyPoint, ButterflyPointModel, UiArchitect, MapActivity
    // [Integration Points] MapActivity.java (Target Avatar action sheet), ButterflyPointEngine, Firebase RTDB
    /**
     * Context-aware Entry point when invoked directly for a specific target avatar
     */
    public static void showForTarget(
        MapActivity activity,
        String targetUid,
        String targetName,
        Double targetLat,
        Double targetLng
    ) {
        if (activity == null || activity.isFinishing()) return;

        DeviceRepository dbHelper = DeviceRepository.getInstance(activity);
        List<ButterflyPoint> activePoints = dbHelper.getActiveButterflyPoints();

        String displayName = (targetName != null && !targetName.isEmpty()) ? targetName : "Peer";
        Double validLat = (targetLat != null && (targetLat != 0.0 || (targetLng != null && targetLng != 0.0))) ? targetLat : null;
        Double validLng = (targetLng != null && ((targetLat != null && targetLat != 0.0) || targetLng != 0.0)) ? targetLng : null;
        String defaultAddr = validLat != null ? displayName + "'s Current Location" : "";

        if (activePoints != null && !activePoints.isEmpty()) {
            UiArchitect.OptionItem[] options = new UiArchitect.OptionItem[]{
                new UiArchitect.OptionItem("🦋 Active Rendezvous Points (" + activePoints.size() + ")", "View approach vectors, distances, and live telemetry for active points", android.R.drawable.ic_menu_myplaces),
                new UiArchitect.OptionItem("➕ Set New Rendezvous with " + displayName, "Establish a new meeting waypoint and invite " + displayName + " directly", com.jdroidx.jfindme.R.drawable.ic_butterfly_point)
            };
            UiArchitect.showRichOptionsDialog(activity, "🦋 BUTTERFLY HUB: " + displayName.toUpperCase(Locale.ROOT), options, (idx, label) -> {
                if (idx == 0) {
                    showActivePointsSheet(activity, activePoints);
                } else {
                    showCreateButterflyPointStudio(activity, validLat, validLng, defaultAddr, targetUid);
                }
            });
        } else {
            // Direct launch to creation studio pre-populated with target device and coordinates
            showCreateButterflyPointStudio(activity, validLat, validLng, defaultAddr, targetUid);
        }
    }

    /**
     * [Variables] options, activity | [Constants] None | [Dependencies] UiArchitect, ButterflyPointDialog | [Integration Points] MapActivity ⟷ Butterfly Hub
     * Action sheet when zero active rendezvous points exist
     */
    private static void showNoActivePointsActionSheet(MapActivity activity) {
        UiArchitect.OptionItem[] options = new UiArchitect.OptionItem[]{
            new UiArchitect.OptionItem("➕ Set Butterfly Meet Point", "Create a rendezvous point and share with connected devices to meet and track", com.jdroidx.jfindme.R.drawable.ic_butterfly_point),
            new UiArchitect.OptionItem("📜 Past Rendezvous Archives", "Review completed, archived, or expired meeting history", android.R.drawable.ic_menu_recent_history),
            new UiArchitect.OptionItem("👻 Toggle Privacy Ghost Mode", "Conceal or jitter your live location from family & guardians", com.jdroidx.jfindme.R.drawable.ic_privacy_lock)
        };

        UiArchitect.showRichOptionsDialog(activity, "🦋 Butterfly Meeting Hub", options, (index, label) -> {
            if (index == 0) {
                showCreateButterflyPointStudio(activity, null, null, null, null);
            } else if (index == 1) {
                showArchivedPointsDialog(activity);
            } else {
                showGhostPrivacyModeToggle(activity);
            }
        });
    }

    /**
     * Dedicated Privacy Ghost Mode toggle (invoked via secondary long-click or options dialog)
     */
    public static void showGhostPrivacyModeToggle(MapActivity activity) {
        if (activity == null || activity.isFinishing()) return;
        SharedPreferences prefs = activity.getSharedPreferences("jfindme_prefs", Context.MODE_PRIVATE);
        boolean currentGhost = prefs.getBoolean("ghost_mode", false);
        String title = currentGhost ? "Disable Ghost Mode?" : "Enable Ghost Mode?";
        String msg = currentGhost 
            ? "Your live location telemetry will become visible to all authorized family members and guardians."
            : "Your live location will be concealed from all network members. Background tracking will be paused for other users.";
        UiArchitect.showConfirmationDialog(activity, title, msg, "PROCEED", "CANCEL", () -> {
            if (activity.getViewModel() != null) {
                activity.getViewModel().toggleGhostMode(prefs);
            }
            boolean nowGhost = prefs.getBoolean("ghost_mode", false);
            Toast.makeText(activity, nowGhost ? "GHOST MODE: ACTIVE (Concealed)" : "GHOST MODE: DISABLED (Visible)", Toast.LENGTH_SHORT).show();
        }, null);
    }

    /**
     * Checks if there are any inbound pending meeting point invitations for the local device.
     */
    public static boolean hasPendingInvitation(Context context) {
        try {
            DeviceRepository dbHelper = DeviceRepository.getInstance(context);
            String selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context);
            List<ButterflyPoint> active = dbHelper.getActiveButterflyPoints();
            for (ButterflyPoint pt : active) {
                String devStatus = dbHelper.getButterflyDeviceStatus(pt.getPointId(), selfDevId);
                if (ButterflyPointDeviceMapTable.STATUS_PENDING.equalsIgnoreCase(devStatus)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Active Butterfly Points Viewer (Expandable/Collapsible CardView Tiles)
     */
    public static void showActivePointsSheet(MapActivity activity, List<ButterflyPoint> points) {
        int pad = activity.scale(16);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        // Header
        TextView tvHeader = new TextView(activity);
        tvHeader.setText("🦋 ACTIVE BUTTERFLY MEET POINTS (" + points.size() + ")");
        tvHeader.setTextSize(18f);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(activity.getTextColor());
        tvHeader.setPadding(0, 0, 0, activity.scale(12));
        root.addView(tvHeader);

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(scroll)
            .setNegativeButton("CLOSE", null)
            .create();

        activePointsDialogRef = new java.lang.ref.WeakReference<>(dialog);
        activeDialogActivityRef = new java.lang.ref.WeakReference<>(activity);
        dialog.setOnDismissListener(d -> {
            if (activePointsDialogRef != null && activePointsDialogRef.get() == dialog) {
                activePointsDialogRef = null;
            }
        });

        // 📬 Inbound Meeting Invitations Section (Prominent Accept / Reject affordances)
        DeviceRepository dbHelper = DeviceRepository.getInstance(activity);
        String selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(activity);
        for (ButterflyPoint pt : points) {
            String devStatus = dbHelper.getButterflyDeviceStatus(pt.getPointId(), selfDevId);
            if (ButterflyPointDeviceMapTable.STATUS_PENDING.equalsIgnoreCase(devStatus)) {
                LinearLayout invCard = new LinearLayout(activity);
                invCard.setOrientation(LinearLayout.VERTICAL);
                invCard.setPadding(activity.scale(12), activity.scale(12), activity.scale(12), activity.scale(12));
                LinearLayout.LayoutParams invLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                invLp.setMargins(0, 0, 0, activity.scale(12));
                invCard.setLayoutParams(invLp);

                android.graphics.drawable.GradientDrawable invBg = new android.graphics.drawable.GradientDrawable();
                invBg.setColor(Color.argb(45, 14, 165, 233));
                invBg.setStroke(activity.scale(2), COLOR_TACTICAL_CYAN);
                invBg.setCornerRadius(activity.scale(12));
                invCard.setBackground(invBg);

                TextView tvInvTitle = new TextView(activity);
                tvInvTitle.setText("📬 NEW MEET POINT INVITATION");
                tvInvTitle.setTextColor(COLOR_TEXT_CYAN);
                tvInvTitle.setTextSize(13f);
                tvInvTitle.setTypeface(null, Typeface.BOLD);
                invCard.addView(tvInvTitle);

                TextView tvInvDetails = new TextView(activity);
                String orgName = pt.getCreatorName().isEmpty() ? pt.getCreatorDeviceId() : pt.getCreatorName();
                tvInvDetails.setText("Meeting: " + pt.getPointName() + "\nHost: " + orgName + "\nAddress: " + pt.getAddress());
                tvInvDetails.setTextColor(activity.getTextColor());
                tvInvDetails.setTextSize(11.5f);
                tvInvDetails.setPadding(0, activity.scale(4), 0, activity.scale(8));
                invCard.addView(tvInvDetails);

                LinearLayout btnRow = new LinearLayout(activity);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);

                Button btnAccept = new Button(activity);
                btnAccept.setText("✓ ACCEPT");
                btnAccept.setTextColor(Color.WHITE);
                btnAccept.setBackgroundColor(COLOR_EMERALD_GREEN);
                LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                aLp.rightMargin = activity.scale(6);
                btnAccept.setLayoutParams(aLp);
                btnAccept.setOnClickListener(v -> {
                    dialog.dismiss();
                    ButterflyPointEngine.submitPeerResponse(activity, pt.getPointId(), selfDevId, true, null);
                    activity.focusOnButterflyPoint(pt);
                    UiArchitect.showSlideNotificationBanner(activity, "RENDEZVOUS ACCEPTED", "Accepted invitation to " + pt.getPointName(), false, 3500, null, null);
                });
                btnRow.addView(btnAccept);

                Button btnDecline = new Button(activity);
                btnDecline.setText("✗ DECLINE");
                btnDecline.setTextColor(Color.WHITE);
                btnDecline.setBackgroundColor(COLOR_TACTICAL_RED);
                LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                btnDecline.setLayoutParams(dLp);
                btnDecline.setOnClickListener(v -> {
                    dialog.dismiss();
                    ButterflyPointEngine.submitPeerResponse(activity, pt.getPointId(), selfDevId, false, "Declined by user");
                    UiArchitect.showSlideNotificationBanner(activity, "RENDEZVOUS DECLINED", "Declined invitation to " + pt.getPointName(), false, 3500, null, null);
                });
                btnRow.addView(btnDecline);

                invCard.addView(btnRow);
                root.addView(invCard);
            }
        }

        // Render each point as an expandable card
        for (ButterflyPoint pt : points) {
            root.addView(createExpandablePointCard(activity, pt, dialog));
        }

        // Action: Create New Butterfly Point Button
        Button btnCreateNew = new Button(activity);
        btnCreateNew.setText("➕ SET BUTTERFLY MEET POINT");
        btnCreateNew.setTextColor(Color.WHITE);
        btnCreateNew.setBackgroundColor(activity.getGradientStart());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, activity.scale(16), 0, 0);
        btnCreateNew.setLayoutParams(btnLp);

        btnCreateNew.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateButterflyPointStudio(activity, null, null, null, null);
        });

        root.addView(btnCreateNew);

        // Action: Past Rendezvous Archives Button
        Button btnArchives = new Button(activity);
        btnArchives.setText("📜 PAST RENDEZVOUS ARCHIVES");
        btnArchives.setTextColor(activity.getTextColor());
        btnArchives.setTextSize(12f);
        UiArchitect.applyCardWithOverride(activity, btnArchives, "CARD_STANDARD", 0.95f);
        LinearLayout.LayoutParams archLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        archLp.setMargins(0, activity.scale(8), 0, 0);
        btnArchives.setLayoutParams(archLp);
        btnArchives.setOnClickListener(v -> {
            dialog.dismiss();
            showArchivedPointsDialog(activity);
        });
        root.addView(btnArchives);
        scroll.addView(root);
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    /**
     * Creates an individual Expandable / Contractible CardView for an active rendezvous point
     */
    private static View createExpandablePointCard(MapActivity activity, ButterflyPoint pt, AlertDialog parentDialog) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.scale(14);
        card.setPadding(pad, pad, pad, pad);
        UiArchitect.applyCardWithOverride(activity, card, "CARD_STANDARD", 1.0f);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, activity.scale(12));
        card.setLayoutParams(cardLp);

        // Date and Time Metadata Formatters
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault());
        String meetingTimeStr = sdf.format(new Date(pt.getRendezvousTimestamp()));

        // Header Row: Title & Scheduled Countdown
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("🦋 " + pt.getPointName());
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(activity.getTextColor());
        headerRow.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTimeBadge = new TextView(activity);
        long diffMs = pt.getRendezvousTimestamp() - System.currentTimeMillis();
        String timeStr = diffMs > 0 ? "In " + (diffMs / 60000) + "m" : "Overdue";
        tvTimeBadge.setText(timeStr);
        tvTimeBadge.setTextSize(12f);
        tvTimeBadge.setTextColor(activity.getGradientStart());
        tvTimeBadge.setTypeface(Typeface.DEFAULT_BOLD);
        headerRow.addView(tvTimeBadge);

        card.addView(headerRow);

        // Subtitle: Date & Time Metadata
        TextView tvSchedule = new TextView(activity);
        tvSchedule.setText("⏰ Meeting Time: " + meetingTimeStr);
        tvSchedule.setTextSize(12f);
        tvSchedule.setTextColor(activity.getGradientStart());
        tvSchedule.setPadding(0, activity.scale(2), 0, activity.scale(2));
        card.addView(tvSchedule);

        // Subtitle: Address / Coordinates
        TextView tvAddr = new TextView(activity);
        tvAddr.setText(pt.getAddress().isEmpty() ? "Coordinates: " + String.format(Locale.US, "%.5f, %.5f", pt.getLat(), pt.getLng()) : pt.getAddress());
        tvAddr.setTextSize(13f);
        tvAddr.setTextColor(activity.getSecondaryTextColor());
        tvAddr.setPadding(0, 0, 0, activity.scale(8));
        card.addView(tvAddr);

        // 🛡️ GAP-03 Concurrency Shield: Use defensive thread-safe snapshot to eliminate ConcurrentModificationException
        Map<String, ButterflyDeviceEntry> devicesSnapshot = pt.getDevicesSnapshot();

        // 🏁 Quorum Assembly Evaluation
        int totalDevs = devicesSnapshot.size();
        int reachedCount = 0;
        for (ButterflyDeviceEntry devEntry : devicesSnapshot.values()) {
            if (ButterflyPointDeviceMapTable.STATUS_REACHED.equalsIgnoreCase(devEntry.getStatus())) {
                reachedCount++;
            }
        }
        final int finalReachedCount = reachedCount;
        boolean isQuorumAssembled = totalDevs > 0 && reachedCount >= totalDevs;

        if (isQuorumAssembled) {
            LinearLayout quorumBanner = new LinearLayout(activity);
            quorumBanner.setOrientation(LinearLayout.HORIZONTAL);
            quorumBanner.setGravity(Gravity.CENTER_VERTICAL);
            quorumBanner.setPadding(activity.scale(10), activity.scale(8), activity.scale(10), activity.scale(8));
            android.graphics.drawable.GradientDrawable qBg = new android.graphics.drawable.GradientDrawable();
            qBg.setColor(Color.argb(45, 16, 185, 129));
            qBg.setStroke(activity.scale(1), COLOR_EMERALD_GREEN);
            qBg.setCornerRadius(activity.scale(8));
            quorumBanner.setBackground(qBg);
            LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            qLp.setMargins(0, activity.scale(4), 0, activity.scale(8));
            quorumBanner.setLayoutParams(qLp);

            TextView tvQuorum = new TextView(activity);
            tvQuorum.setText("🎉 QUORUM ASSEMBLED • All " + reachedCount + "/" + totalDevs + " participants reached point!");
            tvQuorum.setTextColor(COLOR_EMERALD_GREEN);
            tvQuorum.setTextSize(12f);
            tvQuorum.setTypeface(null, Typeface.BOLD);
            quorumBanner.addView(tvQuorum);
            card.addView(quorumBanner);
        }

        // Collapsible Detail Section
        LinearLayout detailSection = new LinearLayout(activity);
        detailSection.setOrientation(LinearLayout.VERTICAL);
        detailSection.setVisibility(View.GONE);

        // Metadata: Coordinates & Arrival Radius
        TextView tvMeta = new TextView(activity);
        tvMeta.setText("🎯 Arrival Threshold: " + ((int) pt.getArrivalRadiusMeters()) + "m | Geohash: " + pt.getGeohash());
        tvMeta.setTextSize(12f);
        tvMeta.setTextColor(activity.getSecondaryTextColor());
        detailSection.addView(tvMeta);

        // Connected Devices Roster
        TextView tvDevHeader = new TextView(activity);
        tvDevHeader.setText("👥 Connected Devices Selected (Tap Device to Focus View):");
        tvDevHeader.setTextSize(13f);
        tvDevHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvDevHeader.setTextColor(activity.getTextColor());
        tvDevHeader.setPadding(0, activity.scale(8), 0, activity.scale(4));
        detailSection.addView(tvDevHeader);

        DeviceRepository dbHelper = DeviceRepository.getInstance(activity);
        List<UserLocation> liveLocations = (activity.getViewModel() != null && activity.getViewModel().getUserLocations().getValue() != null)
            ? activity.getViewModel().getUserLocations().getValue() : Collections.emptyList();
        String selfAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(activity);
        String selfUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (selfUid == null) selfUid = "";

        if (devicesSnapshot.isEmpty()) {
            TextView tvNoDev = new TextView(activity);
            tvNoDev.setText("  • No connected devices attached yet");
            tvNoDev.setTextSize(12f);
            tvNoDev.setTextColor(activity.getSecondaryTextColor());
            tvNoDev.setPadding(0, activity.scale(4), 0, activity.scale(4));
            detailSection.addView(tvNoDev);
        }

        for (Map.Entry<String, ButterflyDeviceEntry> entry : devicesSnapshot.entrySet()) {
            ButterflyDeviceEntry dev = entry.getValue();
            String devId = dev.getDeviceId();
            String resolvedDevName = dbHelper.getDeviceCentricName(devId);
            if (resolvedDevName.isEmpty()) resolvedDevName = "Device " + devId;

            boolean isSelf = devId.equalsIgnoreCase(selfAgentId) || (!selfUid.isEmpty() && devId.equalsIgnoreCase(selfUid));
            if (isSelf && !resolvedDevName.contains("(You)")) {
                resolvedDevName = resolvedDevName + " (You)";
            }

            // Find live location for distance & focus (Host location SSOT integration)
            UserLocation matchedLoc = null;
            if (isSelf && activity.getViewModel() != null && activity.getViewModel().getMyLocation().getValue() != null) {
                android.location.Location myGps = activity.getViewModel().getMyLocation().getValue();
                matchedLoc = new UserLocation(selfAgentId, myGps.getLatitude(), myGps.getLongitude(), myGps.getSpeed());
            } else {
                for (UserLocation ul : liveLocations) {
                    if (ul != null && devId.equalsIgnoreCase(ul.getResolvedUserId())) {
                        matchedLoc = ul;
                        break;
                    }
                }
            }
            if (matchedLoc == null) {
                java.util.Map<String, Object> histLoc = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(activity).getLatestHistoricalLocation(devId);
                if (histLoc != null && histLoc.containsKey("lat") && histLoc.containsKey("lng")) {
                    double hLat = ((Number) histLoc.get("lat")).doubleValue();
                    double hLng = ((Number) histLoc.get("lng")).doubleValue();
                    float hSpd = histLoc.containsKey("speed") ? ((Number) histLoc.get("speed")).floatValue() : 0f;
                    if (hLat != 0.0 && hLng != 0.0) {
                        matchedLoc = new UserLocation(devId, hLat, hLng, hSpd);
                    }
                }
            }
            if (matchedLoc == null && dev.getLastLat() != 0.0 && dev.getLastLng() != 0.0) {
                matchedLoc = new UserLocation(devId, dev.getLastLat(), dev.getLastLng(), dev.getSpeedKmh());
            }

            double distToPt = dev.getDistanceMeters();
            if (matchedLoc != null && matchedLoc.getLatitude() != 0.0) {
                distToPt = ButterflyPointEngine.calculateHaversineDistance(
                    matchedLoc.getLatitude(), matchedLoc.getLongitude(), pt.getLat(), pt.getLng()
                );
                dev.setDistanceMeters(distToPt);
                dev.setLastLat(matchedLoc.getLatitude());
                dev.setLastLng(matchedLoc.getLongitude());
                if (matchedLoc.getSpeed() > 0) {
                    float spdKmh = (float) (matchedLoc.getSpeed() * 3.6f);
                    dev.setSpeedKmh(spdKmh);
                    int dynamicEta = ButterflyPointEngine.calculateEtaMinutes(distToPt, spdKmh, activity);
                    dev.setEtaMinutes(dynamicEta);
                }
            }

            // 🚦 OOPS MVC: Dynamic RAG Telemetry Evaluation
            com.jdroidx.jfindme.utils.DeviceRagEngine.RagStatus rag =
                ButterflyPointEngine.evaluateButterflyRag(activity, dev, pt.getRendezvousTimestamp());

            int statusColor = rag.getColorInt();
            String statusSymbol = rag.getEmoji() + " " + (!dev.getRagReason().isEmpty() ? dev.getRagReason() : rag.getLabel());

            LinearLayout devRow = new LinearLayout(activity);
            devRow.setOrientation(LinearLayout.HORIZONTAL);
            devRow.setGravity(Gravity.CENTER_VERTICAL);
            devRow.setPadding(0, activity.scale(4), 0, activity.scale(4));

            TextView tvDevName = new TextView(activity);
            String distToGoStr = distToPt > 0 ? " • " + ButterflyPointEngine.formatDistance(distToPt) + " to go" : "";
            tvDevName.setText("  • " + resolvedDevName + distToGoStr);
            tvDevName.setTextSize(12f);
            tvDevName.setTextColor(activity.getTextColor());
            devRow.addView(tvDevName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvDevStatus = new TextView(activity);
            tvDevStatus.setText(statusSymbol);
            tvDevStatus.setTextSize(11f);
            tvDevStatus.setTypeface(Typeface.DEFAULT_BOLD);
            tvDevStatus.setTextColor(statusColor);
            devRow.addView(tvDevStatus);

            // On select device: Human-in-the-Loop SOP Action Sheet
            final UserLocation finalLoc = matchedLoc;
            final String finalDevName = resolvedDevName;
            final double finalDist = distToPt;
            devRow.setOnClickListener(v -> {
                List<UiArchitect.OptionItem> sopOptionsList = new ArrayList<>();
                sopOptionsList.add(new UiArchitect.OptionItem("🎯 Focus on Live Map", "Pan and zoom to " + finalDevName + " with line-of-sight vector", android.R.drawable.ic_menu_mylocation));
                sopOptionsList.add(new UiArchitect.OptionItem("🔔 Nudge / Poke Node", "Send immediate rendezvous prompt to " + finalDevName, android.R.drawable.ic_dialog_email));
                sopOptionsList.add(new UiArchitect.OptionItem("⏰ Extend Rendezvous Time", "Adjust rendezvous time (+15m, +30m, +1h) for delay", android.R.drawable.ic_menu_recent_history));

                String devStatus = dev.getStatus();
                boolean isRevoked = ButterflyPointDeviceMapTable.STATUS_REVOKED.equalsIgnoreCase(devStatus);
                boolean isPendingOrTimedOut = ButterflyPointDeviceMapTable.STATUS_PENDING.equalsIgnoreCase(devStatus)
                    || ButterflyPointDeviceMapTable.STATUS_TIMED_OUT.equalsIgnoreCase(devStatus)
                    || ButterflyPointDeviceMapTable.STATUS_REJECTED.equalsIgnoreCase(devStatus);

                if (isPendingOrTimedOut || isRevoked) {
                    sopOptionsList.add(new UiArchitect.OptionItem("🔄 Re-Invite Node", "Reset invitation status and re-dispatch rendezvous to " + finalDevName, android.R.drawable.ic_menu_rotate));
                }
                if (!isRevoked) {
                    sopOptionsList.add(new UiArchitect.OptionItem("❌ Revoke Invitation", "Cancel invitation for " + finalDevName + " and clear tracking", android.R.drawable.ic_delete));
                }

                UiArchitect.OptionItem[] sopOptions = sopOptionsList.toArray(new UiArchitect.OptionItem[0]);

                UiArchitect.showRichOptionsDialog(activity, "Tactical SOP: " + finalDevName, sopOptions, (sopIdx, label) -> {
                    if (label.contains("Focus on Live Map")) {
                        if (parentDialog != null) parentDialog.dismiss();
                        if (finalLoc != null && finalLoc.getLatitude() != 0.0 && activity.getGoogleMap() != null) {
                            com.jdroidx.jfindme.engine.map.TacticalCameraController.animateTo(
                                activity.getGoogleMap(),
                                new LatLng(finalLoc.getLatitude(), finalLoc.getLongitude()), 17.0f
                            );
                            Toast.makeText(activity, "Focus on " + finalDevName + " (" + ButterflyPointEngine.formatDistance(finalDist) + " to rendezvous)", Toast.LENGTH_LONG).show();
                        } else if (dev.getLastLat() != 0.0 && dev.getLastLng() != 0.0 && activity.getGoogleMap() != null) {
                            com.jdroidx.jfindme.engine.map.TacticalCameraController.animateTo(
                                activity.getGoogleMap(),
                                new LatLng(dev.getLastLat(), dev.getLastLng()), 17.0f
                            );
                            Toast.makeText(activity, "Focus on " + finalDevName + " (" + ButterflyPointEngine.formatDistance(finalDist) + " to rendezvous)", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(activity, finalDevName + " has no active GPS fix yet", Toast.LENGTH_SHORT).show();
                        }
                    } else if (label.contains("Nudge / Poke Node")) {
                        ButterflyPointEngine.nudgeParticipant(activity, dev.getDeviceId(), pt.getPointName());
                        UiArchitect.showSlideNotificationBanner(activity, "NUDGE DISPATCHED", "Sent rendezvous prompt to " + finalDevName, false, 3000, null, null);
                    } else if (label.contains("Extend Rendezvous Time")) {
                        showExtendRendezvousDialog(activity, pt, parentDialog);
                    } else if (label.contains("Re-Invite Node")) {
                        ButterflyPointEngine.reInviteDevice(activity, pt.getPointId(), dev.getDeviceId());
                        UiArchitect.showSlideNotificationBanner(activity, "INVITATION RESENT", "Re-invited " + finalDevName + " to rendezvous", false, 3000, null, null);
                    } else if (label.contains("Revoke Invitation")) {
                        UiArchitect.showConfirmationDialog(activity, "Revoke Invitation", "Revoke rendezvous invitation for " + finalDevName + "? Tracking and alerts will be terminated for this node.", "REVOKE", "KEEP", () -> {
                            ButterflyPointEngine.revokeDeviceInvitation(activity, pt.getPointId(), dev.getDeviceId());
                            UiArchitect.showSlideNotificationBanner(activity, "INVITATION REVOKED", "Revoked invitation for " + finalDevName, false, 3000, null, null);
                        }, null);
                    }
                });
            });

            detailSection.addView(devRow);
        }

        // Action Buttons inside Expanded Area: Locate on Map & Focus
        Button btnLocate = new Button(activity);
        btnLocate.setText("🎯 LOCATE ON LIVE MAP");
        btnLocate.setTextColor(Color.WHITE);
        btnLocate.setBackgroundColor(COLOR_ACCENT_GREEN);
        LinearLayout.LayoutParams locLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        locLp.setMargins(0, activity.scale(10), 0, 0);
        btnLocate.setLayoutParams(locLp);

        btnLocate.setOnClickListener(v -> {
            if (parentDialog != null) parentDialog.dismiss();
            activity.focusOnButterflyPoint(pt);
            UiArchitect.showSlideNotificationBanner(activity, "TACTICAL VIEW LOCKED", "Focused on " + pt.getPointName(), false, 3000, null, null);
        });

        detailSection.addView(btnLocate);

        // Action: Invite More Connected Peers (Delegated mesh sharing)
        Button btnInviteMore = new Button(activity);
        btnInviteMore.setText("➕ INVITE MORE CONNECTED PEERS");
        btnInviteMore.setTextColor(Color.WHITE);
        btnInviteMore.setBackgroundColor(COLOR_TACTICAL_BLUE);
        LinearLayout.LayoutParams inviteLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inviteLp.setMargins(0, activity.scale(8), 0, 0);
        btnInviteMore.setLayoutParams(inviteLp);
        btnInviteMore.setOnClickListener(v -> showInviteMorePeersDialog(activity, pt, parentDialog));
        detailSection.addView(btnInviteMore);

        // Action: Turn-by-Turn Navigation (Google Maps / Waze / Geo Browser)
        Button btnNavigate = new Button(activity);
        btnNavigate.setText("🧭 LAUNCH TURN-BY-TURN NAVIGATION");
        btnNavigate.setTextColor(Color.WHITE);
        btnNavigate.setBackgroundColor(COLOR_EMERALD_GREEN);
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        navLp.setMargins(0, activity.scale(8), 0, 0);
        btnNavigate.setLayoutParams(navLp);
        btnNavigate.setOnClickListener(v -> {
            try {
                android.net.Uri gmmIntentUri = android.net.Uri.parse("google.navigation:q=" + pt.getLat() + "," + pt.getLng() + "&mode=d");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(mapIntent);
                } else {
                    android.net.Uri fallbackUri = android.net.Uri.parse("geo:" + pt.getLat() + "," + pt.getLng() + "?q=" + pt.getLat() + "," + pt.getLng() + "(" + android.net.Uri.encode(pt.getPointName()) + ")");
                    Intent geoIntent = new Intent(Intent.ACTION_VIEW, fallbackUri);
                    if (geoIntent.resolveActivity(activity.getPackageManager()) != null) {
                        activity.startActivity(geoIntent);
                    } else {
                        android.net.Uri browserUri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + pt.getLat() + "," + pt.getLng());
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
                    }
                }
            } catch (Exception ex) {
                Toast.makeText(activity, "Unable to launch navigation: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        detailSection.addView(btnNavigate);

        // Action: Quorum Completion Action Button (HITL Milestone)
        if (isQuorumAssembled) {
            Button btnCompletePt = new Button(activity);
            btnCompletePt.setText("🏁 COMPLETE & ARCHIVE RENDEZVOUS");
            btnCompletePt.setTextColor(Color.WHITE);
            btnCompletePt.setBackgroundColor(COLOR_EMERALD_GREEN);
            LinearLayout.LayoutParams compLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            compLp.setMargins(0, activity.scale(8), 0, 0);
            btnCompletePt.setLayoutParams(compLp);

            btnCompletePt.setOnClickListener(v -> {
                UiArchitect.showConfirmationDialog(activity, "Complete Rendezvous", "All " + finalReachedCount + " members assembled at '" + pt.getPointName() + "'. Complete meeting and archive cleanly?", "COMPLETE", "KEEP ACTIVE", () -> {
                    if (parentDialog != null) parentDialog.dismiss();
                    ButterflyPointEngine.archiveButterflyPoint(activity, pt.getPointId(), () -> {
                        activity.runOnUiThread(() -> {
                            UiArchitect.showSlideNotificationBanner(activity, "RENDEZVOUS COMPLETED", "'" + pt.getPointName() + "' completed & archived.", false, 3500, null, null);
                            if (activity.getViewModel() != null) activity.getViewModel().refreshLocations();
                        });
                        return kotlin.Unit.INSTANCE;
                    });
                }, null);
            });
            detailSection.addView(btnCompletePt);
        }

        // Action: Extend Rendezvous Time (Human-in-the-Loop SOP)
        Button btnExtTime = new Button(activity);
        btnExtTime.setText("⏰ EXTEND RENDEZVOUS TIME");
        btnExtTime.setTextColor(Color.WHITE);
        btnExtTime.setBackgroundColor(COLOR_TACTICAL_AMBER);
        LinearLayout.LayoutParams extLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        extLp.setMargins(0, activity.scale(8), 0, 0);
        btnExtTime.setLayoutParams(extLp);
        btnExtTime.setOnClickListener(v -> showExtendRendezvousDialog(activity, pt, parentDialog));
        detailSection.addView(btnExtTime);

        // Action: End / Archive Rendezvous
        Button btnCancelPt = new Button(activity);
        btnCancelPt.setText("🛑 END / ARCHIVE RENDEZVOUS");
        btnCancelPt.setTextColor(Color.WHITE);
        btnCancelPt.setBackgroundColor(COLOR_TACTICAL_RED);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cancelLp.setMargins(0, activity.scale(8), 0, 0);
        btnCancelPt.setLayoutParams(cancelLp);

        btnCancelPt.setOnClickListener(v -> {
            UiArchitect.showConfirmationDialog(activity, "End Rendezvous", "End and archive '" + pt.getPointName() + "'? Tracking lines and overlays will be cleared.", "END MEETING", "KEEP ACTIVE", () -> {
                if (parentDialog != null) parentDialog.dismiss();
                ButterflyPointEngine.archiveButterflyPoint(activity, pt.getPointId(), () -> {
                    activity.runOnUiThread(() -> {
                        UiArchitect.showSlideNotificationBanner(activity, "RENDEZVOUS ARCHIVED", "'" + pt.getPointName() + "' archived cleanly.", false, 3500, null, null);
                        if (activity.getViewModel() != null) activity.getViewModel().refreshLocations();
                    });
                    return kotlin.Unit.INSTANCE;
                });
            }, null);
        });

        detailSection.addView(btnCancelPt);
        card.addView(detailSection);

        // Expand / Contract click toggle
        card.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition((ViewGroup) card.getParent(), new AutoTransition().setDuration(200));
            boolean isExpanded = detailSection.getVisibility() == View.VISIBLE;
            detailSection.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
        });

        return card;
    }

    /**
     * Creation Studio: Step-by-step Location Search, Time Picker, Connected Devices Tick-Box & Share
     */
    public static void showCreateButterflyPointStudio(
        MapActivity activity,
        Double presetLat,
        Double presetLng,
        String presetAddress
    ) {
        showCreateButterflyPointStudio(activity, presetLat, presetLng, presetAddress, null);
    }

    // [Variables] activity, presetLat, presetLng, presetAddress, preselectedTargetUid, dialogHolder, pad, scroll, root
    // [Constants] PADDING_STANDARD
    // [Dependencies] MapActivity, UiArchitect, DatabaseHelper, Geocoder, Calendar
    // [Integration Points] MapActivity (Avatar options), ButterflyPointEngine, Firebase RTDB
    public static void showCreateButterflyPointStudio(
        MapActivity activity,
        Double presetLat,
        Double presetLng,
        String presetAddress,
        String preselectedTargetUid
    ) {
        showCreateButterflyPointStudio(activity, presetLat, presetLng, presetAddress, preselectedTargetUid, null);
    }

    // [Variables] activity, presetLat, presetLng, presetAddress, preselectedTargetUid, presetPointName, dialogHolder, pad, scroll, root
    // [Constants] PADDING_STANDARD
    // [Dependencies] MapActivity, UiArchitect, DatabaseHelper, Geocoder, Calendar
    // [Integration Points] MapActivity (Avatar options), ButterflyPointEngine, Firebase RTDB
    public static void showCreateButterflyPointStudio(
        MapActivity activity,
        Double presetLat,
        Double presetLng,
        String presetAddress,
        String preselectedTargetUid,
        String presetPointName
    ) {
        kotlin.Pair<Boolean, String> check = ButterflyPointEngine.canCreateButterflyPoint(activity);
        if (!check.getFirst()) {
            UiArchitect.showSlideNotificationBanner(
                activity,
                "RENDEZVOUS UNAVAILABLE",
                check.getSecond(),
                false,
                4500,
                null,
                null
            );
            return;
        }
        final AlertDialog[] dialogHolder = new AlertDialog[1];
        int pad = activity.scale(16);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        // 1. Header
        TextView tvTitle = new TextView(activity);
        tvTitle.setText("🦋 SET BUTTERFLY MEET POINT");
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(activity.getTextColor());
        tvTitle.setPadding(0, 0, 0, activity.scale(10));
        root.addView(tvTitle);

        // 2. Point Name Field
        EditText etName = new EditText(activity);
        etName.setHint("Meeting point name (e.g., Central Mall Cafe)");
        if (presetPointName != null && !presetPointName.trim().isEmpty()) {
            etName.setText(presetPointName.trim());
        } else if (presetAddress != null && !presetAddress.trim().isEmpty()) {
            etName.setText("Rendezvous: " + presetAddress.trim());
        }
        etName.setTextColor(activity.getTextColor());
        etName.setHintTextColor(activity.getSecondaryTextColor());
        UiArchitect.applyCardWithOverride(activity, etName, "CARD_STANDARD", 1.0f);
        etName.setPadding(pad, pad, pad, pad);
        root.addView(etName);

        // 3. Location Identification Selector
        TextView tvLocSection = new TextView(activity);
        tvLocSection.setText("📍 Step 1: Identify Rendezvous Location");
        tvLocSection.setTypeface(Typeface.DEFAULT_BOLD);
        tvLocSection.setTextColor(activity.getTextColor());
        tvLocSection.setPadding(0, activity.scale(12), 0, activity.scale(6));
        root.addView(tvLocSection);

        // Search Bar Container for Address / Place
        LinearLayout searchContainer = new LinearLayout(activity);
        searchContainer.setOrientation(LinearLayout.HORIZONTAL);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchContainer.setLayoutParams(scLp);

        EditText etSearch = new EditText(activity);
        etSearch.setHint("🔍 Search place, landmark, or address...");
        etSearch.setTextColor(activity.getTextColor());
        etSearch.setHintTextColor(activity.getSecondaryTextColor());
        etSearch.setSingleLine(true);
        etSearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        UiArchitect.applyCardWithOverride(activity, etSearch, "CARD_STANDARD", 1.0f);
        etSearch.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        searchLp.rightMargin = activity.scale(8);
        etSearch.setLayoutParams(searchLp);
        searchContainer.addView(etSearch);

        Button btnSearchAction = new Button(activity);
        btnSearchAction.setText("SEARCH");
        btnSearchAction.setTextSize(12f);
        btnSearchAction.setTextColor(Color.WHITE);
        btnSearchAction.setBackgroundColor(activity.getGradientStart());
        btnSearchAction.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        searchContainer.addView(btnSearchAction);

        root.addView(searchContainer);

        // Selected Location Preview
        TextView tvPreview = new TextView(activity);
        final double[] targetCoords = new double[]{
            presetLat != null ? presetLat : 0.0,
            presetLng != null ? presetLng : 0.0
        };
        final String[] resolvedAddress = new String[]{presetAddress != null ? presetAddress : ""};

        if (targetCoords[0] != 0.0 && targetCoords[1] != 0.0) {
            tvPreview.setText("Selected: " + (resolvedAddress[0].isEmpty() ? targetCoords[0] + ", " + targetCoords[1] : resolvedAddress[0]));
        } else {
            tvPreview.setText("No coordinates locked yet.");
        }
        tvPreview.setTextSize(12f);
        tvPreview.setTextColor(activity.getGradientStart());
        tvPreview.setPadding(0, activity.scale(4), 0, activity.scale(8));
        root.addView(tvPreview);

        // [Variables] executeSearch, query, geocoder, matches, match, parts, pLat, pLng, db, localPts, matchedPt, fPt | [Constants] None | [Dependencies] Geocoder, DatabaseHelper, ButterflyPoint, Toast | [Integration Points] ButterflyPointDialog, MapActivity
        Runnable executeSearch = () -> {
            String query = etSearch.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(activity, "Enter a location name, landmark, or coordinates to search", Toast.LENGTH_SHORT).show();
                return;
            }
            tvPreview.setText("Locating '" + query + "'...");
            new Thread(() -> {
                try {
                    // Check if query is direct Lat,Lng coordinates
                    String[] parts = query.split("[, ]+");
                    if (parts.length == 2) {
                        try {
                            double pLat = Double.parseDouble(parts[0]);
                            double pLng = Double.parseDouble(parts[1]);
                            if (pLat >= -90.0 && pLat <= 90.0 && pLng >= -180.0 && pLng <= 180.0) {
                                activity.runOnUiThread(() -> {
                                    targetCoords[0] = pLat;
                                    targetCoords[1] = pLng;
                                    resolvedAddress[0] = String.format(Locale.US, "Coordinates (%.5f, %.5f)", pLat, pLng);
                                    tvPreview.setText("📍 Locked: " + resolvedAddress[0]);
                                    if (etName.getText().toString().trim().isEmpty()) {
                                        etName.setText("Meeting Point (" + String.format(Locale.US, "%.4f, %.4f", pLat, pLng) + ")");
                                    }
                                });
                                return;
                            }
                        } catch (Exception ignored) {}
                    }

                    // Check local database for matching saved rendezvous points or landmarks first
                    DeviceRepository db = DeviceRepository.getInstance(activity);
                    List<ButterflyPoint> localPts = db.getActiveButterflyPoints();
                    ButterflyPoint matchedPt = null;
                    if (localPts != null) {
                        for (ButterflyPoint p : localPts) {
                            if (p.getPointName().toLowerCase().contains(query.toLowerCase()) || 
                                (p.getAddress() != null && p.getAddress().toLowerCase().contains(query.toLowerCase()))) {
                                matchedPt = p;
                                break;
                            }
                        }
                    }

                    if (matchedPt != null) {
                        final ButterflyPoint fPt = matchedPt;
                        activity.runOnUiThread(() -> {
                            targetCoords[0] = fPt.getLat();
                            targetCoords[1] = fPt.getLng();
                            resolvedAddress[0] = fPt.getAddress().isEmpty() ? fPt.getPointName() : fPt.getAddress();
                            tvPreview.setText("📍 Locked from Saved: " + resolvedAddress[0]);
                            if (etName.getText().toString().trim().isEmpty()) {
                                etName.setText(fPt.getPointName());
                            }
                        });
                        return;
                    }

                    // OOPS MVC: Dynamic coordinate resolution via ReverseGeocodeEngine SSOT
                    // [Variables] match, query | [Dependencies] ReverseGeocodeEngine | [Integration Points] ButterflyPointDialog ⟷ ReverseGeocodeEngine
                    com.jdroidx.jfindme.engine.map.ReverseGeocodeEngine.resolveCoordinates(activity, query, match -> {
                        activity.runOnUiThread(() -> {
                            if (match != null) {
                                targetCoords[0] = match.getLatitude();
                                targetCoords[1] = match.getLongitude();
                                resolvedAddress[0] = match.getAddressLine(0) != null ? match.getAddressLine(0) : query;
                                tvPreview.setText("📍 Locked: " + resolvedAddress[0]);
                                if (etName.getText().toString().trim().isEmpty()) {
                                    etName.setText(query);
                                }
                            } else {
                                tvPreview.setText("📍 Named: '" + query + "' (Use 'Tap on Map' or 'My GPS' to lock coordinates)");
                                if (etName.getText().toString().trim().isEmpty()) {
                                    etName.setText(query);
                                }
                            }
                        });
                        return null;
                    });
                } catch (Exception e) {
                    activity.runOnUiThread(() -> {
                        tvPreview.setText("📍 Named: '" + query + "' (Select 'My GPS' or 'Tap on Map' to lock)");
                        if (etName.getText().toString().trim().isEmpty()) {
                            etName.setText(query);
                        }
                    });
                }
            }).start();
        };

        btnSearchAction.setOnClickListener(v -> executeSearch.run());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            executeSearch.run();
            return true;
        });

        // Preset Pills: MY GPS | PEER GPS | MAP CENTER | TAP ON MAP
        LinearLayout pillsRow1 = new LinearLayout(activity);
        pillsRow1.setOrientation(LinearLayout.HORIZONTAL);

        Button btnMyGps = createPresetButton(activity, "📍 My GPS", v -> {
            double myLat = 0.0;
            double myLng = 0.0;
            if (activity.getViewModel() != null && activity.getViewModel().getMyLocation().getValue() != null) {
                android.location.Location myLoc = activity.getViewModel().getMyLocation().getValue();
                myLat = myLoc.getLatitude();
                myLng = myLoc.getLongitude();
            }
            if (myLat == 0.0 && myLng == 0.0) {
                myLat = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLatitude(activity);
                myLng = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLongitude(activity);
            }
            if (myLat == 0.0 && myLng == 0.0) {
                try {
                    android.location.LocationManager lm = (android.location.LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
                    if (lm != null) {
                        android.location.Location gpsLoc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                        if (gpsLoc != null && (gpsLoc.getLatitude() != 0.0 || gpsLoc.getLongitude() != 0.0)) {
                            myLat = gpsLoc.getLatitude();
                            myLng = gpsLoc.getLongitude();
                        } else {
                            android.location.Location netLoc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                            if (netLoc != null && (netLoc.getLatitude() != 0.0 || netLoc.getLongitude() != 0.0)) {
                                myLat = netLoc.getLatitude();
                                myLng = netLoc.getLongitude();
                            }
                        }
                    }
                } catch (SecurityException ignored) {}
            }
            if (myLat != 0.0 || myLng != 0.0) {
                targetCoords[0] = myLat;
                targetCoords[1] = myLng;
                resolvedAddress[0] = "Host Current GPS Fix";
                tvPreview.setText(String.format(Locale.US, "📍 Locked to My GPS (%.5f, %.5f)", myLat, myLng));
                if (etName.getText().toString().trim().isEmpty()) {
                    etName.setText("My Meeting Location");
                }
            } else {
                Toast.makeText(activity, "Acquiring host satellite fix...", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnPeerGps = createPresetButton(activity, "👥 Peer GPS", v -> {
            DeviceRepository dbH = DeviceRepository.getInstance(activity);
            List<Map<String, Object>> roster = dbH.getOfflineRoster();
            List<UserLocation> peerLocs = activity.getViewModel() != null && activity.getViewModel().getUserLocations().getValue() != null
                ? activity.getViewModel().getUserLocations().getValue() : Collections.emptyList();

            Map<String, String> peerNamesMap = new LinkedHashMap<>();
            Map<String, double[]> peerCoordsMap = new HashMap<>();

            for (UserLocation ul : peerLocs) {
                String uId = ul.getResolvedUserId();
                if (uId.equals("You") || uId.equals("SELF_USER")) continue;
                peerNamesMap.put(uId, ul.getUserName());
                peerCoordsMap.put(uId, new double[]{ul.getLatitude(), ul.getLongitude()});
            }
            for (Map<String, Object> rNode : roster) {
                String pId = (String) (rNode.get("uid_1") != null ? rNode.get("uid_1") : rNode.get("connected_id"));
                if (pId == null || peerNamesMap.containsKey(pId)) continue;
                String rName = (String) (rNode.get("fullName") != null ? rNode.get("fullName") : rNode.get("name"));
                Map<String, Object> hLoc = dbH.getLatestHistoricalLocation(pId);
                double hLat = hLoc != null && hLoc.get("lat") != null ? ((Number) hLoc.get("lat")).doubleValue() : 0.0;
                double hLng = hLoc != null && hLoc.get("lng") != null ? ((Number) hLoc.get("lng")).doubleValue() : 0.0;
                peerNamesMap.put(pId, rName != null ? rName : pId);
                peerCoordsMap.put(pId, new double[]{hLat, hLng});
            }

            if (peerNamesMap.isEmpty()) {
                Toast.makeText(activity, "No connected peers in network.", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> peerKeys = new ArrayList<>(peerNamesMap.keySet());
            String[] displayOptions = new String[peerKeys.size()];
            for (int i = 0; i < peerKeys.size(); i++) {
                String pName = peerNamesMap.get(peerKeys.get(i));
                double[] pCoords = peerCoordsMap.get(peerKeys.get(i));
                boolean hasFix = pCoords != null && (pCoords[0] != 0.0 || pCoords[1] != 0.0);
                displayOptions[i] = pName + (hasFix ? " (GPS Locked)" : " (Awaiting Fix)");
            }

            new AlertDialog.Builder(activity)
                .setTitle("Select Peer Location Anchor")
                .setItems(displayOptions, (d, which) -> {
                    String selectedKey = peerKeys.get(which);
                    String selectedName = peerNamesMap.get(selectedKey);
                    double[] coords = peerCoordsMap.get(selectedKey);
                    if (coords != null && (coords[0] != 0.0 || coords[1] != 0.0)) {
                        targetCoords[0] = coords[0];
                        targetCoords[1] = coords[1];
                        resolvedAddress[0] = "Peer: " + selectedName;
                        tvPreview.setText("📍 Locked to Peer: " + selectedName);
                    } else {
                        Toast.makeText(activity, selectedName + " has no GPS coordinates recorded yet.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
        });

        pillsRow1.addView(btnMyGps);
        pillsRow1.addView(btnPeerGps);
        root.addView(pillsRow1);

        LinearLayout pillsRow2 = new LinearLayout(activity);
        pillsRow2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p2Lp.setMargins(0, activity.scale(6), 0, 0);
        pillsRow2.setLayoutParams(p2Lp);

        Button btnMapCenter = createPresetButton(activity, "🗺️ Map Center", v -> {
            if (activity.getGoogleMap() != null) {
                LatLng center = activity.getGoogleMap().getCameraPosition().target;
                if (center != null && (center.latitude != 0.0 || center.longitude != 0.0)) {
                    targetCoords[0] = center.latitude;
                    targetCoords[1] = center.longitude;
                    resolvedAddress[0] = String.format(Locale.US, "Map Center (%.4f, %.4f)", center.latitude, center.longitude);
                    tvPreview.setText("📍 Locked to: " + resolvedAddress[0]);
                    return;
                }
            }
            // Fallback to cached location
            double lat = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLatitude(activity);
            double lng = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLongitude(activity);
            if (lat != 0.0 || lng != 0.0) {
                targetCoords[0] = lat;
                targetCoords[1] = lng;
                resolvedAddress[0] = "Map Center (Cached)";
                tvPreview.setText(String.format(Locale.US, "📍 Locked to Map Center (%.4f, %.4f)", lat, lng));
            }
        });

        Button btnTapOnMap = createPresetButton(activity, "🎯 Tap on Map", v -> {
            String currentEnteredName = etName.getText().toString().trim();
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            activity.engageMapPointPicker((pickedLat, pickedLng, pickedAddress) -> {
                showCreateButterflyPointStudio(activity, pickedLat, pickedLng, pickedAddress, preselectedTargetUid, currentEnteredName);
            });
        });

        pillsRow2.addView(btnMapCenter);
        pillsRow2.addView(btnTapOnMap);
        root.addView(pillsRow2);

        // 4. Temporal Affordance: Date & Time Picker
        TextView tvTimeSection = new TextView(activity);
        tvTimeSection.setText("⏰ Step 2: Set Meeting Date & Time");
        tvTimeSection.setTypeface(Typeface.DEFAULT_BOLD);
        tvTimeSection.setTextColor(activity.getTextColor());
        tvTimeSection.setPadding(0, activity.scale(12), 0, activity.scale(6));
        root.addView(tvTimeSection);

        final Calendar scheduleCal = Calendar.getInstance();
        scheduleCal.add(Calendar.HOUR_OF_DAY, 1); // Default 1 hour from now

        Button btnPickDateTime = new Button(activity);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy @ HH:mm", Locale.getDefault());
        btnPickDateTime.setText("📅 " + sdf.format(scheduleCal.getTime()));
        btnPickDateTime.setTextColor(activity.getTextColor());
        UiArchitect.applyCardWithOverride(activity, btnPickDateTime, "CARD_STANDARD", 1.0f);

        btnPickDateTime.setOnClickListener(v -> {
            new DatePickerDialog(activity, (dp, year, month, day) -> {
                scheduleCal.set(Calendar.YEAR, year);
                scheduleCal.set(Calendar.MONTH, month);
                scheduleCal.set(Calendar.DAY_OF_MONTH, day);
                new TimePickerDialog(activity, (tp, hour, minute) -> {
                    scheduleCal.set(Calendar.HOUR_OF_DAY, hour);
                    scheduleCal.set(Calendar.MINUTE, minute);
                    btnPickDateTime.setText("📅 " + sdf.format(scheduleCal.getTime()));
                }, scheduleCal.get(Calendar.HOUR_OF_DAY), scheduleCal.get(Calendar.MINUTE), true).show();
            }, scheduleCal.get(Calendar.YEAR), scheduleCal.get(Calendar.MONTH), scheduleCal.get(Calendar.DAY_OF_MONTH)).show();
        });
        root.addView(btnPickDateTime);

        // 4b. Arrival Radius Threshold Dropdown / Picker
        final double[] selectedArrivalRadius = new double[]{
            (double) com.jdroidx.jfindme.utils.RegistryManager.getButterflyArrivalRadius(activity)
        };
        Button btnArrivalRadius = new Button(activity);
        btnArrivalRadius.setText("🎯 Arrival Threshold: " + ((int) selectedArrivalRadius[0]) + "m (Tap to Change)");
        btnArrivalRadius.setTextColor(activity.getTextColor());
        btnArrivalRadius.setTextSize(12f);
        UiArchitect.applyCardWithOverride(activity, btnArrivalRadius, "CARD_STANDARD", 1.0f);
        btnArrivalRadius.setOnClickListener(v -> {
            int currentDef = (int) selectedArrivalRadius[0];
            String[] radiusOptions = new String[]{
                "Configured Default (" + currentDef + "m)",
                "25m (Tactical / Precision)",
                "50m (Standard Perimeter)",
                "100m (Neighborhood Perimeter)",
                "250m (Staging / Rural Zone)",
                "Custom Radius..."
            };
            double[] radiusValues = new double[]{(double) currentDef, 25.0, 50.0, 100.0, 250.0, -1.0};
            new AlertDialog.Builder(activity)
                .setTitle("Select Arrival Radius")
                .setItems(radiusOptions, (d, which) -> {
                    if (which == radiusOptions.length - 1) {
                        final EditText etCustom = new EditText(activity);
                        etCustom.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                        etCustom.setHint("Radius in meters (10 - 5000)");
                        new AlertDialog.Builder(activity)
                            .setTitle("Custom Arrival Radius")
                            .setView(etCustom)
                            .setPositiveButton("SET", (cd, cw) -> {
                                try {
                                    double val = Double.parseDouble(etCustom.getText().toString().trim());
                                    if (val >= 10.0 && val <= 5000.0) {
                                        selectedArrivalRadius[0] = val;
                                        btnArrivalRadius.setText("🎯 Arrival Threshold: " + ((int) selectedArrivalRadius[0]) + "m (Custom)");
                                    } else {
                                        Toast.makeText(activity, "Enter between 10m and 5000m", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception ignored) {}
                            })
                            .setNegativeButton("CANCEL", null)
                            .show();
                    } else {
                        selectedArrivalRadius[0] = radiusValues[which];
                        btnArrivalRadius.setText("🎯 Arrival Threshold: " + ((int) selectedArrivalRadius[0]) + "m (" + radiusOptions[which] + ")");
                    }
                })
                .show();
        });
        root.addView(btnArrivalRadius);

        // 4c. Auto-Expire Rendezvous Toggle Switch
        int defaultExpiryHours = com.jdroidx.jfindme.utils.RegistryManager.getButterflyExpiryHours(activity);
        LinearLayout toggleRow = new LinearLayout(activity);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);
        toggleRow.setPadding(0, activity.scale(8), 0, activity.scale(4));

        TextView tvToggleLabel = new TextView(activity);
        tvToggleLabel.setText("⏱️ Auto-expire rendezvous (" + defaultExpiryHours + "h window)");
        tvToggleLabel.setTextColor(activity.getTextColor());
        tvToggleLabel.setTextSize(13f);
        toggleRow.addView(tvToggleLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        final boolean[] autoExpireChecked = new boolean[]{true};
        android.widget.Switch swAutoExpire = new android.widget.Switch(activity);
        swAutoExpire.setChecked(true);
        swAutoExpire.setOnCheckedChangeListener((btn, isChecked) -> autoExpireChecked[0] = isChecked);
        toggleRow.addView(swAutoExpire);
        root.addView(toggleRow);

        // 5. Connected Devices Selector: Group-By Hierarchy (Device Group Relationship)
        TextView tvDevSection = new TextView(activity);
        tvDevSection.setText("👥 Step 3: Select Connected Devices (Grouped Hierarchy)");
        tvDevSection.setTypeface(Typeface.DEFAULT_BOLD);
        tvDevSection.setTextColor(activity.getTextColor());
        tvDevSection.setPadding(0, activity.scale(12), 0, activity.scale(6));
        root.addView(tvDevSection);

        DeviceRepository dbHelper = DeviceRepository.getInstance(activity);
        String selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(activity);
        Set<String> connectedSet = dbHelper.getConnectedDeviceIds(selfDevId);
        List<String> pairedIds = new ArrayList<>(connectedSet);
        if (pairedIds.isEmpty()) {
            pairedIds.addAll(dbHelper.getAllPairedDeviceIds());
            pairedIds.remove(selfDevId);
        }
        if (pairedIds.isEmpty()) {
            String activeGroup = com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(activity);
            List<Map<String, Object>> roster = dbHelper.getOfflineRoster(
                activeGroup.isEmpty() || "UNIVERSAL_FLEET".equalsIgnoreCase(activeGroup) || "DEFAULT_GROUP".equalsIgnoreCase(activeGroup) ? null : activeGroup
            );
            if (roster != null) {
                for (Map<String, Object> entry : roster) {
                    if (entry != null) {
                        Object devObj = entry.get("device_id");
                        if (devObj == null) devObj = entry.get("uid");
                        if (devObj != null) {
                            String id = devObj.toString();
                            if (!id.isEmpty() && !id.equals(selfDevId) && !pairedIds.contains(id)) {
                                pairedIds.add(id);
                            }
                        }
                    }
                }
            }
        }
        Set<String> selectedDeviceSet = new HashSet<>();
        if (preselectedTargetUid != null && !preselectedTargetUid.isEmpty()) {
            String cleanTarget = preselectedTargetUid.toUpperCase(Locale.ROOT);
            if (!pairedIds.contains(cleanTarget) && !cleanTarget.equalsIgnoreCase(selfDevId)) {
                pairedIds.add(0, cleanTarget);
            }
        }

        // Group devices by corresponding Group / Circle Hierarchy
        Map<String, List<String>> groupMap = new LinkedHashMap<>();
        Map<String, String> groupTitleMap = new HashMap<>();
        for (String devId : pairedIds) {
            kotlin.Pair<String, String> gInfo = dbHelper.getDeviceGroupInfo(devId);
            String gId = gInfo.getFirst();
            String gName = gInfo.getSecond();
            if (!groupMap.containsKey(gId)) {
                groupMap.put(gId, new ArrayList<>());
                groupTitleMap.put(gId, gName);
            }
            groupMap.get(gId).add(devId);
        }

        CheckBox cbSelectAll = new CheckBox(activity);
        cbSelectAll.setText("Select All Connected Devices (" + pairedIds.size() + ")");
        cbSelectAll.setTextColor(activity.getTextColor());
        root.addView(cbSelectAll);

        List<CheckBox> allDeviceCheckBoxes = new ArrayList<>();
        LinearLayout devListContainer = new LinearLayout(activity);
        devListContainer.setOrientation(LinearLayout.VERTICAL);

        for (Map.Entry<String, List<String>> groupEntry : groupMap.entrySet()) {
            String gId = groupEntry.getKey();
            String gName = groupTitleMap.get(gId);
            List<String> devsInGroup = groupEntry.getValue();

            LinearLayout groupCard = new LinearLayout(activity);
            groupCard.setOrientation(LinearLayout.VERTICAL);
            groupCard.setPadding(activity.scale(10), activity.scale(6), activity.scale(10), activity.scale(8));
            UiArchitect.applyCardWithOverride(activity, groupCard, "CARD_STANDARD", 0.95f);
            LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gLp.setMargins(0, activity.scale(6), 0, activity.scale(6));
            groupCard.setLayoutParams(gLp);

            TextView tvGroupName = new TextView(activity);
            tvGroupName.setText("👥 " + (gName != null ? gName.toUpperCase(Locale.ROOT) : "GENERAL") + " (" + devsInGroup.size() + " devices)");
            tvGroupName.setTextColor(activity.getAccentColor());
            tvGroupName.setTypeface(Typeface.DEFAULT_BOLD);
            tvGroupName.setTextSize(12f);
            groupCard.addView(tvGroupName);

            for (String devId : devsInGroup) {
                String devName = dbHelper.getDeviceCentricName(devId);
                if (devName.isEmpty()) devName = "Device " + devId;
                CheckBox cb = new CheckBox(activity);
                cb.setText("📱 " + devName + "  [" + devId + "]");
                cb.setTextColor(activity.getTextColor());
                if (preselectedTargetUid != null && (preselectedTargetUid.equalsIgnoreCase(devId) || preselectedTargetUid.endsWith(devId) || devId.endsWith(preselectedTargetUid))) {
                    cb.setChecked(true);
                    selectedDeviceSet.add(devId);
                }
                cb.setOnCheckedChangeListener((btn, isChecked) -> {
                    if (isChecked) selectedDeviceSet.add(devId);
                    else selectedDeviceSet.remove(devId);
                });
                allDeviceCheckBoxes.add(cb);
                groupCard.addView(cb);
            }
            devListContainer.addView(groupCard);
        }
        root.addView(devListContainer);

        cbSelectAll.setOnCheckedChangeListener((btn, isChecked) -> {
            for (CheckBox cb : allDeviceCheckBoxes) {
                cb.setChecked(isChecked);
            }
        });

        // 6. Action Button: CONFIRM & SHARE BUTTERFLY POINT
        Button btnShare = new Button(activity);
        btnShare.setText("🚀 CONFIRM & SHARE BUTTERFLY POINT");
        btnShare.setTextColor(Color.WHITE);
        btnShare.setBackgroundColor(activity.getGradientStart());
        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shareLp.setMargins(0, activity.scale(16), 0, 0);
        btnShare.setLayoutParams(shareLp);

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(scroll)
            .setNegativeButton("CANCEL", null)
            .create();
        dialogHolder[0] = dialog;

        btnShare.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Meeting name is required");
                return;
            }
            if ((targetCoords[0] == 0.0 && targetCoords[1] == 0.0) || targetCoords[0] < -90.0 || targetCoords[0] > 90.0 || targetCoords[1] < -180.0 || targetCoords[1] > 180.0) {
                Toast.makeText(activity, "Please lock valid coordinates using search, map tap, or presets.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedDeviceSet.isEmpty()) {
                Toast.makeText(activity, "Select at least 1 connected device.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (scheduleCal.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(activity, "Meeting time must be in the future.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🛡️ Availability Horizon Validation: Prevent scheduling beyond maximum configured retention
            int maxDays = com.jdroidx.jfindme.utils.RegistryManager.getButterflyMaxAvailabilityDays(activity);
            long maxHorizonMs = System.currentTimeMillis() + (maxDays * 24L * 3600L * 1000L);
            if (scheduleCal.getTimeInMillis() > maxHorizonMs) {
                Toast.makeText(activity, "Meeting time cannot exceed maximum availability window (" + maxDays + " days).", Toast.LENGTH_LONG).show();
                return;
            }

            // 🛡️ Duplicate Active Rendezvous Check: Avoid duplicate identical anchors
            DeviceRepository dbHelperCheck = DeviceRepository.getInstance(activity);
            List<ButterflyPoint> activePts = dbHelperCheck.getActiveButterflyPoints();
            if (activePts != null) {
                for (ButterflyPoint existingPt : activePts) {
                    if (existingPt.getPointName().equalsIgnoreCase(name)) {
                        double dMeters = ButterflyPointEngine.calculateHaversineDistance(existingPt.getLat(), existingPt.getLng(), targetCoords[0], targetCoords[1]);
                        if (dMeters < 100.0) {
                            Toast.makeText(activity, "An active rendezvous '" + name + "' already exists at this location.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }

            // 🛡️ Human-in-the-Loop Validation: Distant Rendezvous Safeguard
            if (activity.getViewModel() != null && activity.getViewModel().getMyLocation().getValue() != null) {
                android.location.Location myLoc = activity.getViewModel().getMyLocation().getValue();
                if (myLoc.getLatitude() != 0.0 || myLoc.getLongitude() != 0.0) {
                    double distMeters = ButterflyPointEngine.calculateHaversineDistance(myLoc.getLatitude(), myLoc.getLongitude(), targetCoords[0], targetCoords[1]);
                    double maxDistKm = com.jdroidx.jfindme.utils.RegistryManager.getButterflyMaxDistanceKm(activity);
                    if (distMeters > (maxDistKm * 1000.0)) {
                        String distStr = ButterflyPointEngine.formatDistance(distMeters);
                        UiArchitect.showConfirmationDialog(
                            activity,
                            "Distant Rendezvous Warning",
                            "The selected location is " + distStr + " away from your current location (Exceeds " + ((int) maxDistKm) + " km threshold).\n\nAre you sure you want to schedule here?",
                            "PROCEED",
                            "REVISE",
                            () -> executeDispatchPoint(activity, dialog, name, targetCoords, resolvedAddress, scheduleCal, selectedArrivalRadius[0], autoExpireChecked[0], selfDevId, selectedDeviceSet),
                            null
                        );
                        return;
                    }
                }
            }

            executeDispatchPoint(activity, dialog, name, targetCoords, resolvedAddress, scheduleCal, selectedArrivalRadius[0], autoExpireChecked[0], selfDevId, selectedDeviceSet);
        });

        root.addView(btnShare);
        scroll.addView(root);
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    private static void executeDispatchPoint(
        MapActivity activity,
        AlertDialog dialog,
        String name,
        double[] targetCoords,
        String[] resolvedAddress,
        Calendar scheduleCal,
        double arrivalRadius,
        boolean autoExpire,
        String selfDevId,
        Set<String> selectedDeviceSet
    ) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy @ HH:mm", Locale.getDefault());
        String schedStr = sdf.format(scheduleCal.getTime());
        DeviceRepository dbHelper = DeviceRepository.getInstance(activity);
        StringBuilder devListSummary = new StringBuilder();
        for (String devId : selectedDeviceSet) {
            String dName = dbHelper.getDeviceCentricName(devId);
            if (dName.isEmpty()) dName = devId;
            if (devListSummary.length() > 0) devListSummary.append(", ");
            devListSummary.append(dName);
        }

        String confirmMsg = "Confirm Rendezvous Details:\n\n" +
            "📍 Location: " + (resolvedAddress[0].isEmpty() ? targetCoords[0] + ", " + targetCoords[1] : resolvedAddress[0]) + "\n" +
            "⏰ Scheduled: " + schedStr + "\n" +
            "👥 Devices (" + selectedDeviceSet.size() + "): " + devListSummary.toString() + "\n\n" +
            "Send invitation to all selected connected devices to accept or reject meeting?";

        UiArchitect.showConfirmationDialog(activity, "Share Butterfly Point", confirmMsg, "CONFIRM & DISPATCH", "CANCEL", () -> {
            String pointId = "BP_" + System.currentTimeMillis();
            String creatorDisplayName = com.jdroidx.jfindme.utils.RegistryManager.getString(
                activity, "user_display_name", com.jdroidx.jfindme.utils.RegistryManager.getAgentId(activity));
            int dynamicExpiryHours = com.jdroidx.jfindme.utils.RegistryManager.getButterflyExpiryHours(activity);
            int maxAvailabilityDays = com.jdroidx.jfindme.utils.RegistryManager.getButterflyMaxAvailabilityDays(activity);
            long maxAvailabilityMs = maxAvailabilityDays * 24L * 3600L * 1000L;
            long expiry = autoExpire
                ? scheduleCal.getTimeInMillis() + (dynamicExpiryHours * 3600 * 1000L)
                : scheduleCal.getTimeInMillis() + (24 * 3600 * 1000L);
            long maxCeilingTs = System.currentTimeMillis() + maxAvailabilityMs;
            if (expiry > maxCeilingTs) {
                expiry = maxCeilingTs;
            }

            ButterflyPoint pt = new ButterflyPoint(
                pointId,
                selfDevId,
                creatorDisplayName,
                name,
                resolvedAddress[0].isEmpty() ? "Rendezvous Point" : resolvedAddress[0],
                targetCoords[0],
                targetCoords[1],
                "",
                scheduleCal.getTimeInMillis(),
                expiry,
                arrivalRadius,
                "",
                "BUTTERFLY_STANDARD",
                true,
                false,
                "",
                "",
                new java.util.concurrent.ConcurrentHashMap<>()
            );

            ButterflyPointEngine.dispatchButterflyPoint(
                activity,
                pt,
                new ArrayList<>(selectedDeviceSet),
                () -> {
                    ButterflyPointEngine.startActivePointPeerSync(activity, pt.getPointId());
                    activity.runOnUiThread(() -> {
                        if (dialog != null) dialog.dismiss();
                        activity.focusOnButterflyPoint(pt);
                        UiArchitect.showSlideNotificationBanner(
                            activity,
                            "🦋 RENDEZVOUS DISPATCHED",
                            "Shared '" + name + "' with " + selectedDeviceSet.size() + " devices",
                            false,
                            4500,
                            "LOCATE",
                            () -> activity.focusOnButterflyPoint(pt)
                        );
                    });
                    return kotlin.Unit.INSTANCE;
                },
                err -> {
                    activity.runOnUiThread(() -> {
                        UiArchitect.showSlideNotificationBanner(
                            activity,
                            "DISPATCH FAILED",
                            "Error: " + err,
                            true,
                            4000,
                            null,
                            null
                        );
                    });
                    return kotlin.Unit.INSTANCE;
                }
            );
        }, null);
    }

    private static Button createPresetButton(MapActivity activity, String text, View.OnClickListener l) {
        Button b = new Button(activity);
        b.setText(text);
        b.setTextSize(12f);
        b.setTextColor(activity.getTextColor());
        UiArchitect.applyCardWithOverride(activity, b, "CARD_STANDARD", 0.9f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(0, 0, activity.scale(6), 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        return b;
    }

    /**
     * Human-in-the-Loop SOP: Extends rendezvous time with presets or custom picker
     */
    public static void showExtendRendezvousDialog(MapActivity activity, ButterflyPoint pt, AlertDialog parentDialog) {
        String[] options = {
            "⏱️ +15 Minutes (Traffic / Minor Delay)",
            "⏱️ +30 Minutes (Standard Extension)",
            "⏱️ +45 Minutes (Significant Delay)",
            "⏱️ +1 Hour (Reschedule Outing)"
        };
        long[] values = {15L, 30L, 45L, 60L};
        new AlertDialog.Builder(activity)
            .setTitle("⏰ Extend Rendezvous: " + pt.getPointName())
            .setMessage("Adds additional time to the scheduled rendezvous and propagates the new arrival target to all connected devices in real time.")
            .setItems(options, (d, which) -> {
                d.dismiss();
                long chosen = values[which];
                ButterflyPointEngine.extendRendezvousTime(activity, pt.getPointId(), chosen);
                UiArchitect.showSlideNotificationBanner(activity, "RENDEZVOUS EXTENDED", "'" + pt.getPointName() + "' extended by " + chosen + " minutes", false, 3500, null, null);
                if (parentDialog != null) parentDialog.dismiss();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    /**
     * Delegated Mesh Sharing: Allows any connected device (Device B/C/X) or Creator (Device A)
     * to invite additional bilaterally connected peers from their scoped circle/roster into this Butterfly Point.
     */
    public static void showInviteMorePeersDialog(MapActivity activity, ButterflyPoint pt, AlertDialog parentDialog) {
        if (activity == null || activity.isFinishing()) return;
        DeviceRepository dbHelper = DeviceRepository.getInstance(activity);
        String selfDevId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(activity);

        Set<String> connectedSet = dbHelper.getConnectedDeviceIds(selfDevId);
        List<String> candidateIds = new ArrayList<>(connectedSet);
        if (candidateIds.isEmpty()) {
            candidateIds.addAll(dbHelper.getAllPairedDeviceIds());
        }
        candidateIds.remove(selfDevId);

        // Filter out devices already part of this Butterfly Point (Thread-Safe Snapshot)
        Set<String> alreadyInvited = pt.getDevicesSnapshot().keySet();
        List<String> uninvitedIds = new ArrayList<>();
        for (String id : candidateIds) {
            if (!alreadyInvited.contains(id)) {
                uninvitedIds.add(id);
            }
        }

        if (uninvitedIds.isEmpty()) {
            Toast.makeText(activity, "All connected devices are already invited to this rendezvous.", Toast.LENGTH_SHORT).show();
            return;
        }

        int pad = activity.scale(16);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("➕ INVITE CONNECTED PEERS");
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(activity.getTextColor());
        tvTitle.setPadding(0, 0, 0, activity.scale(8));
        root.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText("Select additional connected peers to join '" + pt.getPointName() + "':");
        tvSub.setTextSize(13f);
        tvSub.setTextColor(activity.getSecondaryTextColor());
        tvSub.setPadding(0, 0, 0, activity.scale(12));
        root.addView(tvSub);

        Set<String> selectedToAdd = new HashSet<>();
        List<CheckBox> checkBoxes = new ArrayList<>();

        CheckBox cbSelectAll = new CheckBox(activity);
        cbSelectAll.setText("Select All (" + uninvitedIds.size() + ")");
        cbSelectAll.setTextColor(activity.getTextColor());
        root.addView(cbSelectAll);

        LinearLayout listContainer = new LinearLayout(activity);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        for (String devId : uninvitedIds) {
            String devName = dbHelper.getDeviceCentricName(devId);
            if (devName.isEmpty()) devName = "Device " + devId;

            CheckBox cb = new CheckBox(activity);
            cb.setText("📱 " + devName + "  [" + devId + "]");
            cb.setTextColor(activity.getTextColor());
            cb.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) selectedToAdd.add(devId);
                else selectedToAdd.remove(devId);
            });
            checkBoxes.add(cb);
            listContainer.addView(cb);
        }
        root.addView(listContainer);

        cbSelectAll.setOnCheckedChangeListener((btn, isChecked) -> {
            for (CheckBox cb : checkBoxes) {
                cb.setChecked(isChecked);
            }
        });

        Button btnConfirm = new Button(activity);
        btnConfirm.setText("🚀 SEND INVITATIONS");
        btnConfirm.setTextColor(Color.WHITE);
        btnConfirm.setBackgroundColor(activity.getGradientStart());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, activity.scale(16), 0, 0);
        btnConfirm.setLayoutParams(btnLp);

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(scroll)
            .setNegativeButton("CANCEL", null)
            .create();

        btnConfirm.setOnClickListener(v -> {
            if (selectedToAdd.isEmpty()) {
                Toast.makeText(activity, "Please select at least 1 device to invite.", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            List<String> listToAdd = new ArrayList<>(selectedToAdd);
            ButterflyPointEngine.inviteAdditionalDevices(
                activity,
                pt.getPointId(),
                listToAdd,
                selfDevId,
                () -> {
                    activity.runOnUiThread(() -> {
                        UiArchitect.showSlideNotificationBanner(
                            activity,
                            "INVITATIONS SENT",
                            "Invited " + listToAdd.size() + " peer(s) to " + pt.getPointName(),
                            false,
                            3500,
                            null,
                            null
                        );
                        if (parentDialog != null) parentDialog.dismiss();
                    });
                    return kotlin.Unit.INSTANCE;
                }
            );
        });

        root.addView(btnConfirm);
        scroll.addView(root);
        dialog.show();
        activity.applyDialogTheme(dialog);
    }

    /**
     * [Variables] activity, dbHelper, archivedPoints, scroll, root, pad, tvHeader, dialog, sdf, card, tvTitle, tvSched, tvAddr, tvDevs
     * [Constants] TAG
     * [Dependencies] MapActivity, DeviceRepository, ButterflyPoint, UiArchitect
     * [Integration Points] ButterflyPointDialog ⟷ DatabaseHelper.getArchivedButterflyPoints
     * Displays completed and archived Butterfly Rendezvous points.
     */
    public static void showArchivedPointsDialog(MapActivity activity) {
        if (activity == null || activity.isFinishing()) return;

        DeviceRepository dbHelper = DeviceRepository.getInstance(activity);
        List<ButterflyPoint> archivedPoints = dbHelper.getArchivedButterflyPoints(30);

        int pad = activity.scale(16);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView tvHeader = new TextView(activity);
        tvHeader.setText("📜 BUTTERFLY RENDEZVOUS ARCHIVES (" + archivedPoints.size() + ")");
        tvHeader.setTextSize(18f);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(activity.getTextColor());
        tvHeader.setPadding(0, 0, 0, activity.scale(12));
        root.addView(tvHeader);

        if (archivedPoints.isEmpty()) {
            TextView tvEmpty = new TextView(activity);
            tvEmpty.setText("No archived or completed rendezvous points found in local ledger.");
            tvEmpty.setTextSize(13f);
            tvEmpty.setTextColor(activity.getSecondaryTextColor());
            tvEmpty.setPadding(0, activity.scale(12), 0, activity.scale(12));
            root.addView(tvEmpty);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault());
            for (ButterflyPoint pt : archivedPoints) {
                LinearLayout card = new LinearLayout(activity);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(activity.scale(12), activity.scale(10), activity.scale(12), activity.scale(10));
                UiArchitect.applyCardWithOverride(activity, card, "CARD_STANDARD", 0.95f);
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardLp.setMargins(0, 0, 0, activity.scale(10));
                card.setLayoutParams(cardLp);

                TextView tvTitle = new TextView(activity);
                tvTitle.setText("🏁 " + pt.getPointName() + " (Archived)");
                tvTitle.setTextSize(15f);
                tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
                tvTitle.setTextColor(activity.getTextColor());
                card.addView(tvTitle);

                TextView tvSched = new TextView(activity);
                tvSched.setText("⏰ Meeting Time: " + sdf.format(new Date(pt.getRendezvousTimestamp())));
                tvSched.setTextSize(12f);
                tvSched.setTextColor(activity.getSecondaryTextColor());
                tvSched.setPadding(0, activity.scale(2), 0, activity.scale(2));
                card.addView(tvSched);

                TextView tvAddr = new TextView(activity);
                tvAddr.setText("📍 " + (pt.getAddress().isEmpty() ? String.format(Locale.US, "%.5f, %.5f", pt.getLat(), pt.getLng()) : pt.getAddress()));
                tvAddr.setTextSize(12f);
                tvAddr.setTextColor(activity.getSecondaryTextColor());
                card.addView(tvAddr);

                Map<String, ButterflyDeviceEntry> snap = pt.getDevicesSnapshot();
                if (!snap.isEmpty()) {
                    TextView tvDevs = new TextView(activity);
                    tvDevs.setText("👥 Participants: " + snap.size() + " devices");
                    tvDevs.setTextSize(11f);
                    tvDevs.setTextColor(activity.getGradientStart());
                    tvDevs.setPadding(0, activity.scale(4), 0, 0);
                    card.addView(tvDevs);
                }

                root.addView(card);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(scroll)
            .setPositiveButton("CLOSE", null)
            .create();
        dialog.show();
        activity.applyDialogTheme(dialog);
    }
}
