package com.jdroidx.jfindme.engine.map

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * MarkerClusterer: Manages marker clustering for efficient map rendering.
 * Main -> Map Visualization -> Map Engine -> MarkerClusterer
 */
class MarkerClusterer(private val context: Context, private val googleMap: GoogleMap) {

    private val clusterManager: ClusterManager<UserLocationClusterItem> by lazy {
        ClusterManager<UserLocationClusterItem>(context, googleMap)
    }

    init {
        // Use setOnCameraIdleListener and setOnMarkerClickListener to integrate with ClusterManager
        googleMap.setOnCameraIdleListener(clusterManager)
        googleMap.setOnMarkerClickListener(clusterManager)
    }

    fun updateMarkers(userLocations: List<UserLocation>) {
        clusterManager.clearItems()
        userLocations.take(50).forEach { location ->
            clusterManager.addItem(UserLocationClusterItem(location))
        }
        clusterManager.cluster()
        SystemLogger.d("MarkerClusterer", "Clustering updated with ${userLocations.size} markers")
    }
}
