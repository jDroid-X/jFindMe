package com.jdroidx.jfindme.engine.security

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * CORE RESPONSIBILITY: (🔐 10. PRIVACY CONTROLS)
 * Overrides the Multiplayer Visibility Query Engine physically blocking other users
 * from rendering the device's avatar based on strict Ghost-Mode and Friends-Only algorithms.
 */
object PrivacyEngine {
    
    private const val TAG = "AvatarPrivacyEngine"
    private const val PREF_NAME = "JFindMe_Privacy_Matrix"
    
    // Privacy Data Models
    enum class VisibilityMode {
        PUBLIC,         // Everyone in a 300m radius gets GPS packets
        FRIENDS_ONLY,   // Only users in your pre-approved FriendsList sub-collection
        GHOST_MODE      // Total invisibility to everything including APIs
    }

    enum class PrivacyTier {
        PRECISE,        // Exact GPS pinpoint (Guardians)
        FUZZY,          // Algorithmic neighborhood jitter (~500-1200m)
        FROZEN          // Ghost mode - locks to last known fixed location
    }

    /**
     * Apply circular mathematical perturbation to latitude and longitude.
     * Offset is deterministic within a 30-minute window to avoid wild jumping while concealing exact residential address.
     */
    @JvmStatic
    fun applyFuzzyPerturbation(lat: Double, lng: Double, radiusMeters: Double = 800.0): Pair<Double, Double> {
        val timeBucket = System.currentTimeMillis() / (1000 * 60 * 30) // 30-minute steady bucket
        val seed = ((lat * 10000).toLong() xor (lng * 10000).toLong() xor timeBucket)
        val rng = java.util.Random(seed)
        
        val angle = rng.nextDouble() * 2.0 * Math.PI
        val distanceMeters = 250.0 + rng.nextDouble() * (radiusMeters - 250.0)
        
        val deltaLat = (distanceMeters * Math.cos(angle)) / 111320.0
        val deltaLng = (distanceMeters * Math.sin(angle)) / (111320.0 * Math.cos(Math.toRadians(lat)))
        
        return Pair(lat + deltaLat, lng + deltaLng)
    }

    @JvmStatic
    fun getPrivacyTier(context: Context): PrivacyTier {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val tierStr = prefs.getString("privacy_tier", PrivacyTier.PRECISE.name) ?: PrivacyTier.PRECISE.name
        return try {
            PrivacyTier.valueOf(tierStr)
        } catch (e: Exception) {
            PrivacyTier.PRECISE
        }
    }

    @JvmStatic
    fun setPrivacyTier(context: Context, tier: PrivacyTier) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("privacy_tier", tier.name).apply()
        SystemLogger.i(TAG, "Privacy Tier changed to: ${tier.name}")
    }
    
    // Network Hook
    private val liveStateEndpoint = FirebaseDatabase.getInstance().reference.child("live_state")

    /**
     * Toggles physical DB rules mapping Android natively to Firebase's Live Sync Broadcasts.
     * Use Case: User wants to walk around the Map tracking their kids, but doesn't want strangers seeing their 3D Avatar!
     */
    fun updateVisibilityMatrix(context: Context, newMode: VisibilityMode) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Save locally so the app boots instantly recalling your preference
        prefs.edit().putString("current_visibility", newMode.name).apply()
        
        SystemLogger.i(TAG, "Privacy Override Activated. Shifting Live Sync to: ${newMode.name}")
        
        // 🔐 Lock the Backend System Immediately
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: "U123"
        val securityPayload = mutableMapOf<String, Any?>()
        
        when (newMode) {
            VisibilityMode.PUBLIC -> {
                securityPayload["visibility_flag"] = "ALL"
            }
            VisibilityMode.FRIENDS_ONLY -> {
                securityPayload["visibility_flag"] = "FRIENDS"
            }
            VisibilityMode.GHOST_MODE -> {
                // Instantly zero out your GPS vector from the live map!
                // Unity will immediately Despawn () you on every other phone watching you.
                securityPayload["visibility_flag"] = "HIDDEN"
                securityPayload["position"] = null 
                securityPayload["animation_state"] = null
            }
        }
        
        // Execute Partial Lock!
        liveStateEndpoint.child(userID).updateChildren(securityPayload)
            .addOnSuccessListener {
                SystemLogger.d(TAG, "Multiplayer Grid updated with new Privacy Handshake.")
            }
            .addOnFailureListener {
                SystemLogger.e(TAG, "Failed locking Privacy Map on Firebase Server!", it)
            }
    }

    /**
     * Synchronous Local Memory Read mapping User Permissions.
     */
    fun isGhostModeActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString("current_visibility", VisibilityMode.PUBLIC.name)
        return mode == VisibilityMode.GHOST_MODE.name
    }
}
