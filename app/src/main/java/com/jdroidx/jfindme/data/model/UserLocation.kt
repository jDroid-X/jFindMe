package com.jdroidx.jfindme.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * UserLocation POJO for Firebase Realtime Database updates.
 * geohash is used for spatial queries.
 * timestamp ensures event-driven update ordering.
 */
@IgnoreExtraProperties
data class UserLocation(
    val userId: Any? = "",
    val deviceId: Any? = "", // 🚀 V49 Mandate: Hardware Hub Anchor
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geohash: String = "",

    @get:com.google.firebase.database.PropertyName("timestamp")
    @set:com.google.firebase.database.PropertyName("timestamp")
    var timestamp: Any? = "",

    val bearing: Float = 0f,
    val speed: Float = 0f,

    @get:com.google.firebase.database.PropertyName("sos")
    @set:com.google.firebase.database.PropertyName("sos")
    var isSosActive: Boolean = false,

    @get:com.google.firebase.database.PropertyName("battery")
    @set:com.google.firebase.database.PropertyName("battery")
    var batteryLevel: Int = -1,

    val isPoked: Boolean = false,
    val pokedBy: String = "",
    val allowDependents: Boolean = true,
    val active_group: String = "",
    val isForensicAudit: Boolean = true, // ✅ Closed Loop Validation Flag
    val address: String? = null,        // 🌍 V58.7 Human-Readable Spatial Context

    // 🎭 UI IDENTITY FIELDS (Synchronized natively across Firebase cloud matrix)
    var userName: String = "",
    var avatar_id: String = "butterfly",
 
    @get:com.google.firebase.database.Exclude @set:com.google.firebase.database.Exclude
    var role_family: String = "None",
 
    @get:com.google.firebase.database.Exclude @set:com.google.firebase.database.Exclude
    var role_friend: String = "None"
) {
    @get:com.google.firebase.database.PropertyName("timestamp_raw")
    @set:com.google.firebase.database.PropertyName("timestamp_raw")
    var timestamp_raw: Any?
        get() = timestamp
        set(value) {
            if (value != null && value != "") {
                timestamp = value
            }
        }

    @get:com.google.firebase.database.Exclude @set:com.google.firebase.database.Exclude
    var isSos: Boolean
        get() = isSosActive
        set(value) { isSosActive = value }

    // 🛡️ RECOVERY HELPERS: Ensures legacy numerical data doesn't break the UI thread
    @com.google.firebase.database.Exclude
    fun getResolvedUserId(): String = userId?.toString() ?: ""

    @com.google.firebase.database.Exclude
    fun getResolvedDeviceId(): String = deviceId?.toString() ?: ""

    @com.google.firebase.database.Exclude
    fun getResolvedTimestamp(): String {
        // Use FirebaseComm's forensic normalization if available
        return com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(timestamp)
    }

    @com.google.firebase.database.Exclude
    fun getLongTimestamp(): Long {
        val t = timestamp
        if (t is Number) {
            val num = t.toLong()
            if (num in 1_000_000_000_000L..4_102_444_800_000L) return num
            if (num in 1_000_000_000L..4_102_444_800L) return num * 1000L
            return parseForensicStringToMillis(num.toString())
        }
        if (t is String) {
            return parseForensicStringToMillis(t)
        }
        return System.currentTimeMillis()
    }

    private fun parseForensicStringToMillis(str: String?): Long {
        if (str.isNullOrBlank() || str == "0") return System.currentTimeMillis()
        val s = str.trim()
        val num = s.toLongOrNull()
        if (num != null && num in 1_000_000_000_000L..4_102_444_800_000L) return num
        if (num != null && num in 1_000_000_000L..4_102_444_800L) return num * 1000L
        
        val patterns = arrayOf("yyyyMMddHHmmssSS", "yyyyMMddHHmmss", "yyyyMMdd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss")
        for (p in patterns) {
            try {
                val sdf = java.text.SimpleDateFormat(p, java.util.Locale.US)
                val d = sdf.parse(if (s.length > p.length) s.substring(0, p.length) else s)
                if (d != null) return d.time
            } catch (e: Exception) {}
        }
        return System.currentTimeMillis()
    }

    @com.google.firebase.database.Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "deviceId" to deviceId,
            "latitude" to latitude,
            "longitude" to longitude,
            "geohash" to geohash,
            "timestamp" to timestamp,
            "bearing" to bearing,
            "speed" to speed,
            "sos" to isSosActive,
            "battery" to batteryLevel,
            "address" to address,
            "active_group" to active_group
        )
    }

    /**
     * 🛰️ PURE HISTORY TELEMETRY METADATA (NO REDUNDANCY):
     * Excludes userId, deviceId, and active_group since they are already
     * defined in the parent key path /History/{agentId} and /devices/{agentId}.
     */
    @com.google.firebase.database.Exclude
    fun toHistoryMap(): Map<String, Any?> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "speed" to speed,
            "bearing" to bearing,
            "battery" to batteryLevel,
            "sos" to isSosActive,
            "geohash" to geohash,
            "address" to (address ?: ""),
            "timestamp" to getLongTimestamp()
        )
    }

    /**
     * 🛡️ Anti-GIGO Validation Guard: Rejects null island (0,0), NaNs, infinities,
     * invalid bounding coordinates, and wildly skewed future timestamps (>24h).
     */
    @com.google.firebase.database.Exclude
    fun isValidLocation(): Boolean {
        if (latitude == 0.0 && longitude == 0.0) return false
        if (latitude.isNaN() || longitude.isNaN() || latitude.isInfinite() || longitude.isInfinite()) return false
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) return false
        val ts = getLongTimestamp()
        val now = System.currentTimeMillis()
        if (ts > now + 86400000L) return false
        return true
    }
}

