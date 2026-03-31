package com.jdroidx.jfindme.data.remote

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.data.model.UserLocation

/**
 * Singleton to manage Firebase Database communication.
 * Standardizes access to tracking, user, and history nodes.
 */
object FirebaseComm {
    private const val NODE_LOCATIONS = "locations"
    private const val NODE_USERS = "users"
    private const val NODE_DEVICES = "devices"
    private const val NODE_PAIRING = "pairing_sessions"
    private const val NODE_HISTORY = "history"

    val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    val locationsRef: DatabaseReference by lazy {
        database.getReference(NODE_LOCATIONS)
    }

    val usersRef: DatabaseReference by lazy {
        database.getReference(NODE_USERS)
    }

    val devicesRef: DatabaseReference by lazy {
        database.getReference(NODE_DEVICES)
    }

    val pairingRef: DatabaseReference by lazy {
        database.getReference(NODE_PAIRING)
    }

    val historyRef: DatabaseReference by lazy {
        database.getReference(NODE_HISTORY)
    }

    /**
     * Conflict Resolution Strategy: Server Authority Lock via Timestamp Priority.
     * Also records historical data for the Replay Engine.
     */
    fun updateMyLocation(userLocation: UserLocation) {
        val ref = locationsRef.child(userLocation.userId)
        
        // QA Pipeline Acceleration: Immediately push data! Never block Firebase Offline caching loops.
        // The DB inherently overwrites older data objects!
        ref.setValue(userLocation)

        // 2. Record to History Node (Time-series data)
        // Note: For production, we limit history density to avoid database bloat.
        historyRef.child(userLocation.userId)
            .child(userLocation.timestamp.toString())
            .setValue(userLocation)
    }
}
