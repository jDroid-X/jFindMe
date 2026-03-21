package com.jdroidx.jfindme.engine.data.avatar

/**
 * Avatar Registry (Instance Factory)
 * QA ARCHITECTURE MANDATE: Directly injects the specified 25 Avatar models based on the initial system blueprint.
 * Provides the templates used to spawn copies across the spatial map grid.
 */
object AvatarRegistry {
    
    // Master Registry containing all 25 specific entity blueprints
    val templateList: List<AvatarEntity> = listOf(
        
        // --- 🟢 COMMON (10) ---
        createBaseAvatar(1, "Walker", AvatarRarity.COMMON, MovementType.WANDER),
        createBaseAvatar(2, "Runner", AvatarRarity.COMMON, MovementType.PATROL, 2.5f),
        createBaseAvatar(3, "Shopper", AvatarRarity.COMMON, MovementType.STATIC),
        createBaseAvatar(4, "Student", AvatarRarity.COMMON, MovementType.WANDER),
        createBaseAvatar(5, "Tourist", AvatarRarity.COMMON, MovementType.WANDER),
        createBaseAvatar(6, "Driver", AvatarRarity.COMMON, MovementType.PATROL, 6.0f),
        createBaseAvatar(7, "Cyclist", AvatarRarity.COMMON, MovementType.PATROL, 4.0f),
        createBaseAvatar(8, "Pet Companion", AvatarRarity.COMMON, MovementType.FOLLOW, 1.5f, InteractionType.PROXIMITY),
        createBaseAvatar(9, "Foodie", AvatarRarity.COMMON, MovementType.STATIC),
        createBaseAvatar(10, "Office Worker", AvatarRarity.COMMON, MovementType.STATIC),

        // --- 🔵 RARE (8) ---
        createBaseAvatar(11, "Night Explorer", AvatarRarity.RARE, MovementType.WANDER),
        createBaseAvatar(12, "Fitness Trainer", AvatarRarity.RARE, MovementType.PATROL, 3.0f),
        createBaseAvatar(13, "Tech Hacker", AvatarRarity.RARE, MovementType.STATIC, 0f, InteractionType.TIME_BOUND),
        createBaseAvatar(14, "Delivery Agent", AvatarRarity.RARE, MovementType.PATROL, 5.0f),
        createBaseAvatar(15, "Influencer", AvatarRarity.RARE, MovementType.STATIC),
        createBaseAvatar(16, "Street Performer", AvatarRarity.RARE, MovementType.STATIC),
        createBaseAvatar(17, "Photographer", AvatarRarity.RARE, MovementType.WANDER),
        createBaseAvatar(18, "Navigator", AvatarRarity.RARE, MovementType.PATROL),

        // --- 🟣 EPIC (5) ---
        createBaseAvatar(19, "Time Traveler", AvatarRarity.EPIC, MovementType.EVENT_BASED, 0f, InteractionType.TIME_BOUND),
        createBaseAvatar(20, "Geo Master", AvatarRarity.EPIC, MovementType.STATIC),
        createBaseAvatar(21, "Data Collector", AvatarRarity.EPIC, MovementType.PATROL),
        createBaseAvatar(22, "Energy Guardian", AvatarRarity.EPIC, MovementType.STATIC, 0f, InteractionType.PROXIMITY),
        createBaseAvatar(23, "Signal Hunter", AvatarRarity.EPIC, MovementType.FOLLOW, 3.0f, InteractionType.CHASE),

        // --- 🟡 LEGENDARY (2) ---
        createBaseAvatar(24, "City King", AvatarRarity.LEGENDARY, MovementType.STATIC, 0f, InteractionType.TAP),
        createBaseAvatar(25, "Shadow Walker", AvatarRarity.LEGENDARY, MovementType.EVENT_BASED, 4.0f, InteractionType.CHASE)
    )

    /**
     * Factory function generating the Base Templates preventing massive code duplication.
     * Generates dummy asset URIs (e.g., "asset://models/avatar_base_1.gltf") automatically.
     */
    private fun createBaseAvatar(
        baseId: Int,
        typeName: String,
        rarityTier: AvatarRarity,
        moveMap: MovementType,
        speed: Float = 1.0f,
        interactionLevel: InteractionType = InteractionType.TAP
    ): AvatarEntity {
        return AvatarEntity(
            instanceId = "TEMPLATE_$baseId", // Replaced by a real UUID when spawned on the map
            baseId = baseId,
            type = typeName,
            rarity = rarityTier,
            lat = 0.0, lng = 0.0, // Instantiated accurately by the Spawn Logic later
            headingVector = 0f,
            geoCellId = "NULL_CELL",
            collisionRadius = 2.5f, // Standard AR hitbox radius
            movementType = moveMap,
            baseSpeed = speed,
            interactionType = interactionLevel,
            spawnTimestamp = 0L,
            despawnTimestamp = 0L,
            assetUri = "asset://models/avatar_base_$baseId.gltf", // Hooks to 3D model engine
            audioUri = "asset://sounds/ping_$rarityTier.mp3",
            rewardId = "RWD_TIER_$rarityTier",
            claimHash = null
        )
    }
}
