package com.jdroidx.jfindme.engine.map

import android.content.Context
import android.location.Address

/**
 * 📍 REVERSE GEOCODE ENGINE (OOPS MVC Subsystem 3 Bridge)
 * ======================================================
 * Architectural bridge delegating to com.jdroidx.jfindme.utils.ReverseGeocodeEngine.
 * Guarantees zero code duplication and call-by-reference execution per Prime Directive 4.
 *
 * Implements 2-Tier Caching (RAM LRU 100 ⟷ SQLite ReverseGeocodeCacheTable ⟷ Geocoder).
 *
 * [Variables] context, lat, lng, callback, result
 * [Constants] MIN_VALID_COORD = 0.001
 * [Dependencies] com.jdroidx.jfindme.utils.ReverseGeocodeEngine
 * [Integration Points] MapActivity, GeofenceViewModel, ButterflyPointDialog, HistoryViewModel
 */
object ReverseGeocodeEngine {

    private const val MIN_VALID_COORD = 0.001

    data class AddressResult(
        val formattedAddress: String,
        val isCached: Boolean,
        val isSuccess: Boolean,
        val latitude: Double,
        val longitude: Double
    )

    interface AddressCallback {
        fun onAddressResolved(result: AddressResult)
    }

    /**
     * Resolves human-readable street address delegating to the 2-Tier SSOT Engine.
     */
    @JvmStatic
    fun resolveAddress(context: Context, lat: Double, lng: Double, callback: AddressCallback) {
        if (Math.abs(lat) < MIN_VALID_COORD && Math.abs(lng) < MIN_VALID_COORD) {
            callback.onAddressResolved(AddressResult("Invalid GPS Coordinates", isCached = false, isSuccess = false, lat, lng))
            return
        }

        com.jdroidx.jfindme.utils.ReverseGeocodeEngine.resolveAddress(context, lat, lng) { resolvedAddr ->
            val isSuccess = resolvedAddr.isNotBlank() && !resolvedAddr.startsWith("Lat:") && !resolvedAddr.contains("Offline")
            callback.onAddressResolved(AddressResult(resolvedAddr, isCached = false, isSuccess = isSuccess, lat, lng))
        }
    }

    /**
     * Resolves coordinates from query address string asynchronously.
     */
    @JvmStatic
    fun resolveCoordinates(context: Context, query: String, callback: (Address?) -> Unit) {
        com.jdroidx.jfindme.utils.ReverseGeocodeEngine.resolveCoordinates(context, query, callback)
    }

    /**
     * Clears in-memory LRU cache if low memory signal is received.
     */
    @JvmStatic
    fun clearCache() {
        com.jdroidx.jfindme.utils.ReverseGeocodeEngine.clearCache()
    }
}
