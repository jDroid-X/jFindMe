package com.jdroidx.jfindme.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.collection.LruCache
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.local.tables.ReverseGeocodeCacheTable
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 🌍 REVERSE GEOCODE ENGINE (OOPS-based MVC Master Geocoding Subsystem)
 * ====================================================================
 * High-performance, 2-Tier Caching reverse-geocoding engine.
 * Eliminates redundant cellular network queries and deduplicates scattered
 * Geocoder implementations across Activities and Dialogs.
 *
 * Tier 1: In-Memory LruCache (0ms RAM lookup)
 * Tier 2: SQLite ReverseGeocodeCacheTable (~0.5ms indexed B-Tree seek)
 * Tier 3: Asynchronous android.location.Geocoder (with Android 13+ Tiramisu support)
 */
object ReverseGeocodeEngine {

    fun interface GeocodeCallback {
        fun onAddressResolved(address: String)
    }

    private const val TAG = "ReverseGeocodeEngine"
    private const val RAM_CACHE_MAX_ENTRIES = 100

    // 🚀 Tier 1 In-Memory LRU Cache (~11m spatial grid cell resolution)
    private val ramCache = LruCache<String, String>(RAM_CACHE_MAX_ENTRIES)
    private val bgExecutor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun makeGridKey(lat: Double, lng: Double): String {
        return String.format(Locale.US, "%.4f,%.4f", lat, lng)
    }

    /**
     * Resolves human-readable street address for given geographic coordinates.
     */
    @JvmStatic
    fun resolveAddress(context: Context, lat: Double, lng: Double, callback: GeocodeCallback?) {
        if (callback == null) return
        if (Math.abs(lat) < 0.0001 && Math.abs(lng) < 0.0001) {
            callback.onAddressResolved("Unknown Location")
            return
        }

        val gridKey = makeGridKey(lat, lng)

        // Tier 1: 0ms RAM LRU cache
        val memCached = ramCache.get(gridKey)
        if (!memCached.isNullOrBlank()) {
            callback.onAddressResolved(memCached)
            return
        }

        // Background lookup (Tier 2 SQLite + Tier 3 Geocoder)
        bgExecutor.execute {
            try {
                // Tier 2: SQLite ReverseGeocodeCacheTable B-Tree seek
                val db = DatabaseHelper.getInstance(context)
                val dbCached = ReverseGeocodeCacheTable.getCachedAddress(db.readableDatabase, lat, lng)
                if (!dbCached.isNullOrBlank()) {
                    ramCache.put(gridKey, dbCached)
                    mainHandler.post { callback.onAddressResolved(dbCached) }
                    return@execute
                }

                // Tier 3: Asynchronous android.location.Geocoder
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(lat, lng, 1) { addresses ->
                            val resolved = formatAddress(addresses)
                            if (resolved.isNotBlank()) {
                                ramCache.put(gridKey, resolved)
                                try {
                                    ReverseGeocodeCacheTable.putAddress(db.writableDatabase, lat, lng, resolved)
                                } catch (_: Exception) {}
                                mainHandler.post { callback.onAddressResolved(resolved) }
                            } else {
                                mainHandler.post { callback.onAddressResolved(fallbackCoordString(lat, lng)) }
                            }
                        }
                        return@execute
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        val resolved = formatAddress(addresses)
                        if (resolved.isNotBlank()) {
                            ramCache.put(gridKey, resolved)
                            try {
                                ReverseGeocodeCacheTable.putAddress(db.writableDatabase, lat, lng, resolved)
                            } catch (_: Exception) {}
                            mainHandler.post { callback.onAddressResolved(resolved) }
                            return@execute
                        }
                    }
                }
            } catch (e: Exception) {
                SystemLogger.w(TAG, "Geocode lookup fallback: ${e.message}")
            }

            mainHandler.post { callback.onAddressResolved(fallbackCoordString(lat, lng)) }
        }
    }

    /**
     * Resolves coordinates from query address string asynchronously.
     */
    @JvmStatic
    fun resolveCoordinates(context: Context, query: String, callback: (Address?) -> Unit) {
        if (query.isBlank()) {
            callback(null)
            return
        }
        bgExecutor.execute {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocationName(query, 1) { addresses ->
                            val match = addresses.firstOrNull()
                            mainHandler.post { callback(match) }
                        }
                        return@execute
                    } else {
                        @Suppress("DEPRECATION")
                        val list = geocoder.getFromLocationName(query, 1)
                        val match = list?.firstOrNull()
                        mainHandler.post { callback(match) }
                        return@execute
                    }
                }
            } catch (e: Exception) {
                SystemLogger.w(TAG, "Coordinate resolution error: ${e.message}")
            }
            mainHandler.post { callback(null) }
        }
    }

    @JvmStatic
    fun clearCache() {
        ramCache.evictAll()
    }

    private fun formatAddress(addresses: List<Address>?): String {
        if (addresses.isNullOrEmpty()) return ""
        val addr = addresses[0]
        val sb = StringBuilder()
        for (i in 0..addr.maxAddressLineIndex) {
            val line = addr.getAddressLine(i)
            if (!line.isNullOrBlank()) {
                if (sb.isNotEmpty()) sb.append(", ")
                sb.append(line)
            }
        }
        return sb.toString().trim()
    }

    private fun fallbackCoordString(lat: Double, lng: Double): String {
        return String.format(Locale.US, "Lat: %.5f, Lng: %.5f", lat, lng)
    }
}
