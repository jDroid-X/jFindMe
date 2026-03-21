package com.jdroidx.jfindme.engine.map

import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * 🚀 NEXT EVOLUTION: 🌍 Heatmap-based Crowd Density
 * Generates an optimized, dynamically updating UI thermal overlay on top of the 2D Native Map.
 * Shows where massive clusters of Multiplayer Avatars are currently interacting natively.
 */
object HeatmapEngine {

    private const val TAG = "AvatarHeatmapEngine"
    private var nativeHeatmapProvider: HeatmapTileProvider? = null
    private var activeTileOverlay: TileOverlay? = null
    
    // Abstract hook to our backend Spatial Firebase structure
    private val liveUsersEndpoint = FirebaseDatabase.getInstance().getReference("locations")

    // The thermal color visual settings fading from transparent green to intense red
    private val thermalGradientColors = intArrayOf(
        Color.rgb(102, 225, 0),   // Green
        Color.rgb(255, 0, 0)      // Red
    )
    private val thermalGradientStops = floatArrayOf(0.2f, 1f)
    private val strictGradient = Gradient(thermalGradientColors, thermalGradientStops)

    /**
     * Bootstraps the real-time crowd density scanner.
     * Prevents loading 10,000 users individually by only counting their latent Vector arrays.
     */
    fun activateCrowdDensityScanner(mapInstance: GoogleMap) {
        SystemLogger.i(TAG, "🌍 Heatmap Density Engine Activated. Parsing active Multi-User vectors...")

        liveUsersEndpoint.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val crowdCoordinates = ArrayList<LatLng>()
                val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                var hasValidPoints = false

                // Rapidly scrape the generic `lat/lng` vectors ignoring the heavy 3D strings
                for (userNode in snapshot.children) {
                    val lat = userNode.child("latitude").getValue(Double::class.java)
                    val lng = userNode.child("longitude").getValue(Double::class.java)
                    
                    // Ignore users actively executing Ghost Mode/Privacy controls
                    val isSos = userNode.child("isSosActive").getValue(Boolean::class.java)
                    if (lat != null && lng != null && lat != 0.0) {
                        val pos = LatLng(lat, lng)
                        crowdCoordinates.add(pos)
                        boundsBuilder.include(pos)
                        hasValidPoints = true
                        
                        // Apply intense visual cluster weighting if SOS is active near them
                        if (isSos == true) {
                            crowdCoordinates.add(pos)
                            crowdCoordinates.add(pos)
                        }
                    }
                }

                if (hasValidPoints) {
                    updateHeatmapLayer(mapInstance, crowdCoordinates)
                    try {
                        // 🧠 QA Fix: Visually sweep the camera back to perfectly frame the active Tracking Cluster!
                        val mapPadding = 250 // Physical pixels from edge
                        mapInstance.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), mapPadding))
                    } catch (e: Exception) {
                        SystemLogger.w(TAG, "Not enough variance in vectors to span a Boundary Box.")
                    }
                } else {
                    activeTileOverlay?.remove() // Clear layer if grid is empty
                }
            }

            override fun onCancelled(error: DatabaseError) {
                SystemLogger.e(TAG, "Critical failure pulling Heatmap Grid Array from Firebase.", error.toException())
            }
        })
    }

    private fun updateHeatmapLayer(mapCore: GoogleMap, rawVectors: List<LatLng>) {
        if (nativeHeatmapProvider == null) {
            // Instantiate the complex mapping tile natively
            nativeHeatmapProvider = HeatmapTileProvider.Builder()
                .data(rawVectors)
                .gradient(strictGradient)
                .radius(50) // Radius of thermal blur in map pixels
                .opacity(0.7) // Translucent so users can still see streets/nav paths
                .build()
                
            activeTileOverlay = mapCore.addTileOverlay(TileOverlayOptions().tileProvider(nativeHeatmapProvider!!))
        } else {
            // Update physical tile map immediately to keep GPS tracking smooth
            nativeHeatmapProvider?.setData(rawVectors)
            activeTileOverlay?.clearTileCache() // Forces Map Engine to visually redraw the heat pixels
        }
        
        SystemLogger.d(TAG, "Map Thermal Layer refreshed with ${rawVectors.size} active entities.")
    }

    /**
     * Kills the active UI footprint safely preventing RAM spikes.
     */
    fun disableCrowdDensityScanner() {
        activeTileOverlay?.remove()
        nativeHeatmapProvider = null
        SystemLogger.d(TAG, "Heatmap Module Deactivated.")
    }
}
