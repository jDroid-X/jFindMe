package com.jdroidx.jfindme.engine.data.avatar

/**
 * AVATAR ENGINE SCHEMA v2.0
 * Includes: Spatial properties, real-time sync nodes, security hashes, 
 * and accessibility UX Hooks (Tab sequence priorities).
 */
data class AvatarEntity(
    // 1. Core Identity & Classification
    val instanceId: String,             // Unique ID for this specific spawn instance
    val baseId: Int,                    // 1-25 referencing the base template
    val type: String,                   // e.g., "Tech Hacker", "Shadow Walker"
    val rarity: AvatarRarity,

    // 2. Spatial & Navigation (Linked to SpatialProcessor.kt)
    val lat: Double,
    val lng: Double,
    val elevation: Float = 0f,
    val headingVector: Float,           // 0-360° facing direction
    val geoCellId: String,              // Map node (e.g., S2 Cell ID)
    val collisionRadius: Float,         // Hitbox radius in meters for proximity triggers

    // 3. Behavior & Movement AI
    val movementType: MovementType,
    val baseSpeed: Float = 0f,          // meters/second

    // 4. Interaction & Event Lifecycle
    val interactionType: InteractionType,
    val spawnTimestamp: Long,           // UTC creation time
    val despawnTimestamp: Long,         // Expiration time (preventing time-theft exploits)
    val cooldownTimer: Long = 0L,       // Prevents interaction spam/macros

    // 5. Asset Integrations (The Missing Visual/Sensory Links)
    val assetUri: String,               // Network link to the 3D map model/texture bundle
    val audioUri: String?,              // Accessibility: Spatial audio proximity alert
    val hapticProfileId: Int = 0,       // Accessibility: Unique vibration pattern identifier

    // 6. Economic & Profile Integration
    val rewardId: String?,              // Foreign key to User Inventory Database
    val rewardQuantity: Int = 0,
    val claimHash: String?,             // Encrypted token preventing duplicate claim exploits

    // 7. UX & Accessibility Focus (Calculated Dynamically on Client for Screen Readers)
    @Transient var distanceFromPlayer: Float = Float.MAX_VALUE,
    @Transient var focusPriority: Int = 0       // Handled by [Tab] Sorting sequence
)

enum class AvatarRarity {
    COMMON, RARE, EPIC, LEGENDARY
}

enum class MovementType {
    STATIC, WANDER, FOLLOW, PATROL, EVENT_BASED
}

enum class InteractionType {
    TAP, PROXIMITY, CHASE, TIME_BOUND
}
