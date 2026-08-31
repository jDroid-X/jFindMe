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

    @get:com.google.firebase.database.PropertyName("timestamp_raw")
    @set:com.google.firebase.database.PropertyName("timestamp_raw")
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
    var avatar_id: String = "avatar_custom_1",
 
    @get:com.google.firebase.database.Exclude @set:com.google.firebase.database.Exclude
    var role_family: String = "None",
 

    @get:com.google.firebase.database.Exclude @set:com.google.firebase.database.Exclude
    var role_friend: String = "None"
) {
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
}
