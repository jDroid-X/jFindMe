package com.jdroidx.jfindme.engine.map;

import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.jdroidx.jfindme.data.model.UserLocation;
import com.jdroidx.jfindme.utils.SystemLogger;

import java.util.List;

/**
 * 🗺️ TACTICAL CAMERA CONTROLLER (OOPS-based MVC Master Camera Engine)
 * ====================================================================
 * Single Source of Truth (SSOT) governing all viewport movements, zoom escalations,
 * bounding-box framing, and camera state transitions across the application.
 *
 * Implements Prime Directive 4: Dynamic OOPS-based MVC standard, call-by-reference,
 * eliminating fragmented imperative camera calls across screens and click listeners.
 *
 * [Variables] googleMap, context, currentMode, lockedTargetId, lastKnownHostLocation, stateListener
 * [Constants] ZOOM_TACTICAL = 17.5f, ZOOM_CLOSE = 18.5f, ZOOM_NEIGHBORHOOD = 15.5f, ZOOM_CITY = 12.0f,
 *             ANIMATION_DURATION_MS = 450, MIN_VALID_COORD = 0.001
 * [Dependencies] GoogleMap, CameraUpdateFactory, LatLng, LatLngBounds, CameraPosition, Location, UserLocation
 * [Integration Points] MapActivity, MultiMarkerEngine, Left Floating HUD, Tactical Action Sheet, Side Action Panel
 */
public class TacticalCameraController {

    private static final String TAG = "TacticalCameraController";

    // 🎯 SSOT Zoom Standard Constants
    public static final float ZOOM_TACTICAL = 17.5f;     // Standard node tactical lock
    public static final float ZOOM_CLOSE = 18.5f;        // High precision inspection
    public static final float ZOOM_NEIGHBORHOOD = 15.5f; // Local cluster / zone overview
    public static final float ZOOM_CITY = 12.0f;         // City / district fleet overview
    public static final int ANIMATION_DURATION_MS = 450; // Smooth 450ms easing curve
    public static final double MIN_VALID_COORD = 0.001;  // Strict anti-Null-Island gate

    public enum CameraMode {
        FOLLOW_HOST,       // Actively tracking Host operator coordinates & bearing
        LOCK_PEER,         // Locked onto a remote peer / child / target node
        FRAME_ALL,         // Bounding box overview of multiple nodes
        FREE_PAN           // Operator manually dragging map canvas; auto-recenter paused
    }

    public interface CameraStateListener {
        void onCameraModeChanged(CameraMode newMode, @Nullable String targetId);
        void onTargetFocused(@Nullable String targetId, @Nullable String displayName, double lat, double lng, float zoom);
    }

    private final Context context;
    private GoogleMap googleMap;
    private CameraMode currentMode = CameraMode.FOLLOW_HOST;
    private String lockedTargetId = null;
    private Location lastKnownHostLocation = null;
    private CameraStateListener stateListener;

    public TacticalCameraController(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void attachMap(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.googleMap.setOnCameraMoveStartedListener(reason -> {
            // REASON_GESTURE = 1: User physically swiped/pinched map canvas
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                setCameraMode(CameraMode.FREE_PAN, null);
            }
        });
    }

    public void setCameraStateListener(@Nullable CameraStateListener listener) {
        this.stateListener = listener;
    }

    public CameraMode getCurrentMode() {
        return currentMode;
    }

    @Nullable
    public String getLockedTargetId() {
        return lockedTargetId;
    }

    private void setCameraMode(CameraMode mode, @Nullable String targetId) {
        this.currentMode = mode;
        this.lockedTargetId = targetId;
        if (stateListener != null) {
            stateListener.onCameraModeChanged(mode, targetId);
        }
    }

    // =========================================================================
    // 🛡️ SANITIZATION & NULL-ISLAND GATE
    // =========================================================================
    public static boolean isValidCoordinate(double lat, double lng) {
        return !Double.isNaN(lat) && !Double.isNaN(lng) &&
               Math.abs(lat) >= MIN_VALID_COORD && Math.abs(lng) >= MIN_VALID_COORD &&
               Math.abs(lat) <= 90.0 && Math.abs(lng) <= 180.0;
    }

    // =========================================================================
    // 📍 1. HOST FOCUS & TRACKING (FOLLOW_HOST)
    // =========================================================================
    public void focusOnHost(@Nullable Location hostLoc, boolean withBearing) {
        if (hostLoc != null) {
            this.lastKnownHostLocation = hostLoc;
        } else {
            hostLoc = this.lastKnownHostLocation;
        }

        if (hostLoc == null || !isValidCoordinate(hostLoc.getLatitude(), hostLoc.getLongitude())) {
            Toast.makeText(context, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show();
            return;
        }

        setCameraMode(CameraMode.FOLLOW_HOST, "HOST");

        if (googleMap == null) return;

        LatLng target = new LatLng(hostLoc.getLatitude(), hostLoc.getLongitude());
        if (withBearing && hostLoc.hasBearing() && hostLoc.getBearing() != 0.0f) {
            CameraPosition pos = new CameraPosition.Builder()
                .target(target)
                .zoom(ZOOM_TACTICAL)
                .bearing(hostLoc.getBearing())
                .tilt(googleMap.getCameraPosition().tilt)
                .build();
            animateTo(CameraUpdateFactory.newCameraPosition(pos));
        } else {
            animateTo(CameraUpdateFactory.newLatLngZoom(target, ZOOM_TACTICAL));
        }

        if (stateListener != null) {
            stateListener.onTargetFocused("HOST", "You (Host)", hostLoc.getLatitude(), hostLoc.getLongitude(), ZOOM_TACTICAL);
        }
    }

    /**
     * Called during continuous location ticks. Updates camera ONLY if in FOLLOW_HOST mode.
     */
    public void onHostLocationUpdate(@NonNull Location location) {
        this.lastKnownHostLocation = location;
        if (currentMode != CameraMode.FOLLOW_HOST || googleMap == null) {
            return; // Operator is inspecting a peer or in free-pan; do not yank camera!
        }

        if (!isValidCoordinate(location.getLatitude(), location.getLongitude())) return;

        LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
        if (location.hasBearing() && location.getBearing() != 0.0f) {
            CameraPosition pos = new CameraPosition.Builder()
                .target(target)
                .zoom(googleMap.getCameraPosition().zoom > 10f ? googleMap.getCameraPosition().zoom : ZOOM_TACTICAL)
                .bearing(location.getBearing())
                .tilt(googleMap.getCameraPosition().tilt)
                .build();
            animateTo(CameraUpdateFactory.newCameraPosition(pos));
        } else {
            animateTo(CameraUpdateFactory.newLatLng(target));
        }
    }

    // =========================================================================
    // 🎯 2. PEER FOCUS & TRACKING (LOCK_PEER)
    // =========================================================================
    public void focusOnPeer(@NonNull UserLocation peer) {
        double lat = peer.getLatitude();
        double lng = peer.getLongitude();
        String devId = peer.getResolvedUserId();
        String name = peer.getUserName() != null && !peer.getUserName().isEmpty() ? peer.getUserName() : "Peer Node";

        if (!isValidCoordinate(lat, lng)) {
            try {
                com.jdroidx.jfindme.data.repository.DeviceRepository repo = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context);
                java.util.Map<String, Object> hist = repo.getLatestHistoricalLocation(devId);
                if (hist != null && hist.get("lat") instanceof Double && hist.get("lng") instanceof Double) {
                    lat = (Double) hist.get("lat");
                    lng = (Double) hist.get("lng");
                }
            } catch (Exception ignored) {}
        }

        if (!isValidCoordinate(lat, lng)) {
            Toast.makeText(context, "Peer coordinates unacquired", Toast.LENGTH_SHORT).show();
            return;
        }

        setCameraMode(CameraMode.LOCK_PEER, devId);

        if (googleMap == null) return;

        LatLng target = new LatLng(lat, lng);
        animateTo(CameraUpdateFactory.newLatLngZoom(target, ZOOM_TACTICAL));

        if (stateListener != null) {
            stateListener.onTargetFocused(devId, name, lat, lng, ZOOM_TACTICAL);
        }
    }

    // =========================================================================
    // 📌 3. ARBITRARY COORDINATE / RENDEZVOUS FOCUS
    // =========================================================================
    public void focusOnCoordinate(double lat, double lng, float zoom, @Nullable String label) {
        if (!isValidCoordinate(lat, lng)) {
            Toast.makeText(context, "Invalid coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        setCameraMode(CameraMode.LOCK_PEER, label);

        if (googleMap == null) return;

        float targetZoom = (zoom > 0f) ? zoom : ZOOM_TACTICAL;
        animateTo(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), targetZoom));

        if (stateListener != null) {
            stateListener.onTargetFocused(label, label != null ? label : "Coordinate", lat, lng, targetZoom);
        }
    }

    // =========================================================================
    // 🗺️ 4. BOUNDING BOX & FLEET OVERVIEW (FRAME_ALL)
    // =========================================================================
    public void frameBoundingBox(@NonNull LatLngBounds bounds, int paddingPx) {
        setCameraMode(CameraMode.FRAME_ALL, null);
        if (googleMap == null) return;

        try {
            animateTo(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx));
        } catch (Exception ex) {
            SystemLogger.w(TAG, "Bounding box framing fallback: " + ex.getMessage());
            animateTo(CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), ZOOM_NEIGHBORHOOD));
        }
    }

    public void frameAllLocations(@Nullable List<UserLocation> locations, @Nullable Location hostLoc, int paddingPx) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        int count = 0;

        if (hostLoc != null && isValidCoordinate(hostLoc.getLatitude(), hostLoc.getLongitude())) {
            builder.include(new LatLng(hostLoc.getLatitude(), hostLoc.getLongitude()));
            count++;
        }

        if (locations != null) {
            for (UserLocation ul : locations) {
                if (ul != null && isValidCoordinate(ul.getLatitude(), ul.getLongitude())) {
                    builder.include(new LatLng(ul.getLatitude(), ul.getLongitude()));
                    count++;
                }
            }
        }

        if (count == 0) {
            Toast.makeText(context, "No active device locations to frame", Toast.LENGTH_SHORT).show();
            return;
        }

        if (count == 1) {
            focusOnHost(hostLoc, false);
            return;
        }

        try {
            frameBoundingBox(builder.build(), paddingPx);
        } catch (Exception ex) {
            SystemLogger.e(TAG, "Failed to build LatLngBounds", ex);
        }
    }

    // =========================================================================
    // 🔍 5. INCREMENTAL ZOOM CONTROLS
    // =========================================================================
    public void zoomIn() {
        if (googleMap != null) {
            animateTo(CameraUpdateFactory.zoomIn());
        }
    }

    public void zoomOut() {
        if (googleMap != null) {
            animateTo(CameraUpdateFactory.zoomOut());
        }
    }

    // =========================================================================
    // ⚙️ INTERNAL ANIMATION DISPATCHER & STATIC CONVENIENCE WRAPPERS
    // =========================================================================
    public static void animateTo(@Nullable GoogleMap map, @NonNull LatLng target, float zoom) {
        if (map == null || !isValidCoordinate(target.latitude, target.longitude)) return;
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom), ANIMATION_DURATION_MS, null);
        } catch (Exception ex) {
            try { map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, zoom)); } catch (Exception ignored) {}
        }
    }

    public static void moveTo(@Nullable GoogleMap map, @NonNull LatLng target, float zoom) {
        if (map == null || !isValidCoordinate(target.latitude, target.longitude)) return;
        try {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, zoom));
        } catch (Exception ignored) {}
    }

    private void animateTo(@NonNull CameraUpdate update) {
        if (googleMap == null) return;
        try {
            googleMap.animateCamera(update, ANIMATION_DURATION_MS, null);
        } catch (Exception ex) {
            SystemLogger.e(TAG, "Failed camera animation, falling back to moveCamera", ex);
            try {
                googleMap.moveCamera(update);
            } catch (Exception ignored) {}
        }
    }
}
