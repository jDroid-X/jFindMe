package com.jdroidx.jfindme.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * UserLocation POJO for Firebase Realtime Database updates.
 * geohash is used for spatial queries.
 * timestamp ensures event-driven update ordering.
 */
@IgnoreExtraProperties
data class UserLocation(
    val userId: String = "",
    val userName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geohash: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val avatar_id: String = "avatar_custom_1",
    val avatarUrl: String? = null,
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val isSosActive: Boolean = false,
    val batteryLevel: Int = -1,
    val isPoked: Boolean = false,
    val pokedBy: String = ""
)
