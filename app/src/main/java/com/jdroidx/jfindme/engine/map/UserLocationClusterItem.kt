package com.jdroidx.jfindme.engine.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.jdroidx.jfindme.data.model.UserLocation

/**
 * UserLocationClusterItem: Wrapper for UserLocation to support marker clustering.
 * Main -> Map Visualization -> Map Engine -> MarkerClusterer -> UserLocationClusterItem
 */
class UserLocationClusterItem(private val userLocation: UserLocation) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(userLocation.latitude, userLocation.longitude)
    override fun getTitle(): String = userLocation.userName
    override fun getSnippet(): String? = null
    override fun getZIndex(): Float? = null
    
    fun getUserId(): String = userLocation.userId
}
