package com.jdroidx.jfindme.engine.location

import com.jdroidx.jfindme.utils.SystemLogger

/**
 * GeohashHelper: Handles spatial queries and Geohash generation for efficient tracking.
 * Main -> Tracking Engine -> GeohashHelper
 */
object GeohashHelper {
    private const val TAG = "GeohashHelper"
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(lat: Double, lon: Double, precision: Int = 9): String {
        val latInterval = doubleArrayOf(-90.0, 90.0)
        val lonInterval = doubleArrayOf(-180.0, 180.0)
        val geohash = StringBuilder()
        var isEven = true
        var bit = 0
        var ch = 0

        while (geohash.length < precision) {
            val mid: Double
            if (isEven) {
                mid = (lonInterval[0] + lonInterval[1]) / 2
                if (lon > mid) {
                    ch = ch or (1 shl (4 - bit))
                    lonInterval[0] = mid
                } else {
                    lonInterval[1] = mid
                }
            } else {
                mid = (latInterval[0] + latInterval[1]) / 2
                if (lat > mid) {
                    ch = ch or (1 shl (4 - bit))
                    latInterval[0] = mid
                } else {
                    latInterval[1] = mid
                }
            }

            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                geohash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        SystemLogger.d(TAG, "Encoded Geohash: $geohash for ($lat, $lon)")
        return geohash.toString()
    }
}
