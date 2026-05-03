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

    // 🎭 UI SHADOW FIELDS (Hydrated from Profile Tier - EXCLUDED from direct persistence)
    @get:com.google.firebase.database.Exclude @set:com.google.firebase.database.Exclude
    var userName: String = "",
    
    @get:com.google.firebase.database.Exclude @set:com.google.firebase.database.Exclude
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
        return when (t) {
            is Long -> t
            is Double -> t.toLong()
            is String -> t.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
    }
}
