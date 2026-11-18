package com.jdroidx.jfindme.ui.map;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.jdroidx.jfindme.ui.base.UiArchitect;
import com.jdroidx.jfindme.utils.SystemLogger;

/**
 * [Variables]: context, googleMap, uiSettings
 * [Constants]: TAG
 * [Dependencies]: GoogleMap, UiArchitect, SystemLogger
 * [Integration Points]: MapActivity, HistoryActivity, GeofenceActivity
 *
 * 🚀 MapLifecycleDelegate (OOPS-based MVC GIS Controller SSOT)
 * Standardizes GoogleMap initialization, UI button positioning, zoom constraints,
 * and dynamic Theme-styled JSON injection.
 * Eliminates duplicate map initialization boilerplate across MapActivity, HistoryActivity, and GeofenceActivity.
 */
public final class MapLifecycleDelegate {

    private static final String TAG = "MapLifecycleDelegate";

    private MapLifecycleDelegate() {
        // Static OOPS Controller
    }

    /**
     * Configures canonical GoogleMap settings, UI buttons, and theme styles.
     */
    public static void configureMapDefaults(@NonNull Context context, @NonNull GoogleMap googleMap) {
        try {
            UiSettings settings = googleMap.getUiSettings();
            settings.setZoomControlsEnabled(true);
            settings.setMyLocationButtonEnabled(false); // Custom tactile button used instead
            settings.setCompassEnabled(true);
            settings.setMapToolbarEnabled(false);
            settings.setRotateGesturesEnabled(true);
            settings.setTiltGesturesEnabled(true);
            settings.setScrollGesturesEnabled(true);
            settings.setZoomGesturesEnabled(true);

            // Apply active Theme (Dark / Night / Cyberpunk styled JSON)
            UiArchitect.applyMapStyle(context, googleMap);
            SystemLogger.d(TAG, "Configured canonical map settings and theme styling.");
        } catch (Exception e) {
            SystemLogger.e(TAG, "Error configuring map defaults: " + e.getMessage());
        }
    }
}
