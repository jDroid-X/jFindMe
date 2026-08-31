package com.jdroidx.jfindme.engine.data.avatar

/**
 * Avatar Registry (Instance Factory)
 * QA ARCHITECTURE MANDATE: Injects the 50 Popular Avatar models from the Diverse Collection.
 * Provides the templates used to spawn copies across the spatial map grid and 3D studio.
 */
object AvatarRegistry {
    
    // Master Registry containing all 50 Popular Avatars from Diverse Collection
    val templateList: List<AvatarEntity> = listOf(
        // --- 🟢 CIVILIAN & CORE (1-10) ---
        createBaseAvatar(1, "Avatar Jane", AvatarRarity.COMMON, MovementType.WANDER),
        createBaseAvatar(2, "Pixel Max", AvatarRarity.RARE, MovementType.PATROL, 2.5f),
        createBaseAvatar(3, "Med Fam", AvatarRarity.COMMON, MovementType.STATIC),
        createBaseAvatar(4, "22D Kawaii", AvatarRarity.RARE, MovementType.FOLLOW, 1.5f, InteractionType.PROXIMITY),
        createBaseAvatar(5, "Business", AvatarRarity.COMMON, MovementType.WANDER),
        createBaseAvatar(6, "Robot Zorp", AvatarRarity.EPIC, MovementType.PATROL, 3.0f),
        createBaseAvatar(7, "Anime Girl", AvatarRarity.RARE, MovementType.WANDER),
        createBaseAvatar(8, "Anime Grl", AvatarRarity.RARE, MovementType.WANDER),
        createBaseAvatar(9, "Avatar Lite", AvatarRarity.COMMON, MovementType.STATIC),
        createBaseAvatar(10, "Avatar Emo", AvatarRarity.COMMON, MovementType.STATIC),

        // --- 🔵 TECH & FANTASY (11-20) ---
        createBaseAvatar(11, "Avatar Eanh", AvatarRarity.COMMON, MovementType.WANDER),
        createBaseAvatar(12, "Avatar Cro", AvatarRarity.RARE, MovementType.PATROL, 3.0f),
        createBaseAvatar(13, "Emo Emo", AvatarRarity.RARE, MovementType.STATIC),
        createBaseAvatar(14, "Punk Rock", AvatarRarity.RARE, MovementType.PATROL, 4.0f),
        createBaseAvatar(15, "Wizard", AvatarRarity.EPIC, MovementType.EVENT_BASED, 0f, InteractionType.TIME_BOUND),
        createBaseAvatar(16, "Gamer", AvatarRarity.RARE, MovementType.STATIC),
        createBaseAvatar(17, "Surgeon", AvatarRarity.COMMON, MovementType.STATIC),
        createBaseAvatar(18, "Dog", AvatarRarity.COMMON, MovementType.FOLLOW, 1.5f, InteractionType.PROXIMITY),
        createBaseAvatar(19, "Neon Cyber Girl", AvatarRarity.EPIC, MovementType.WANDER, 2.0f),
        createBaseAvatar(20, "Archer", AvatarRarity.RARE, MovementType.PATROL, 3.5f),

        // --- 🟣 COSMIC & MYTHICAL (21-30) ---
        createBaseAvatar(21, "Archar", AvatarRarity.COMMON, MovementType.STATIC),
        createBaseAvatar(22, "CEO", AvatarRarity.COMMON, MovementType.STATIC),
        createBaseAvatar(23, "Byindima", AvatarRarity.COMMON, MovementType.STATIC),
        createBaseAvatar(24, "Aslianson", AvatarRarity.EPIC, MovementType.FOLLOW, 2.0f),
        createBaseAvatar(25, "Avitayor", AvatarRarity.LEGENDARY, MovementType.EVENT_BASED, 0f, InteractionType.TAP),
        createBaseAvatar(26, "Bob Turtleneck", AvatarRarity.COMMON, MovementType.FOLLOW, 1.2f, InteractionType.PROXIMITY),
        createBaseAvatar(27, "Ruby Updo", AvatarRarity.EPIC, MovementType.WANDER),
        createBaseAvatar(28, "Amber Waves", AvatarRarity.LEGENDARY, MovementType.STATIC),
        createBaseAvatar(29, "Pink Crewneck", AvatarRarity.RARE, MovementType.PATROL, 3.0f),
        createBaseAvatar(30, "Floral Chic", AvatarRarity.COMMON, MovementType.WANDER),

        // --- 🟡 ROYAL & SUPERNATURAL (31-40) ---
        createBaseAvatar(31, "Solar Collared", AvatarRarity.EPIC, MovementType.PATROL, 1.0f),
        createBaseAvatar(32, "Autumn Knit", AvatarRarity.EPIC, MovementType.STATIC),
        createBaseAvatar(33, "Scholar Specs", AvatarRarity.RARE, MovementType.PATROL, 2.5f),
        createBaseAvatar(34, "Pastel Beanie", AvatarRarity.EPIC, MovementType.STATIC),
        createBaseAvatar(35, "Retro Suspenders", AvatarRarity.EPIC, MovementType.WANDER, 3.0f),
        createBaseAvatar(36, "Crimson V-Neck", AvatarRarity.EPIC, MovementType.STATIC),
        createBaseAvatar(37, "Dungaree Artist", AvatarRarity.COMMON, MovementType.WANDER),
        createBaseAvatar(38, "Queen", AvatarRarity.LEGENDARY, MovementType.STATIC),
        createBaseAvatar(39, "Pandin", AvatarRarity.RARE, MovementType.FOLLOW),
        createBaseAvatar(40, "Knights", AvatarRarity.RARE, MovementType.PATROL),

        // --- 👑 LEGENDS & MASTERS (41-50) ---
        createBaseAvatar(41, "King", AvatarRarity.LEGENDARY, MovementType.STATIC),
        createBaseAvatar(42, "Dragon", AvatarRarity.LEGENDARY, MovementType.EVENT_BASED, 4.0f, InteractionType.CHASE),
        createBaseAvatar(43, "Superhero", AvatarRarity.LEGENDARY, MovementType.PATROL, 5.0f),
        createBaseAvatar(44, "Construction", AvatarRarity.COMMON, MovementType.STATIC),
        createBaseAvatar(45, "Pop Art", AvatarRarity.RARE, MovementType.STATIC),
        createBaseAvatar(46, "Pangfou", AvatarRarity.RARE, MovementType.FOLLOW),
        createBaseAvatar(47, "Magician", AvatarRarity.EPIC, MovementType.EVENT_BASED, 0f, InteractionType.TIME_BOUND),
        createBaseAvatar(48, "Geisha", AvatarRarity.RARE, MovementType.STATIC),
        createBaseAvatar(49, "Hipster", AvatarRarity.COMMON, MovementType.WANDER),
        createBaseAvatar(50, "Detective", AvatarRarity.RARE, MovementType.PATROL, 2.0f)
    )

    fun getAvatarName(id: Int): String {
        val safeId = if (id in 1..50) id else (Math.abs(id) % 50 + 1)
        return templateList.getOrNull(safeId - 1)?.type ?: "Avatar #$safeId"
    }

    /**
     * Factory function generating the Base Templates preventing massive code duplication.
     * Generates asset URIs (e.g., "asset://models/avatar_base_1.gltf") automatically.
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
            instanceId = "TEMPLATE_$baseId",
            baseId = baseId,
            type = typeName,
            rarity = rarityTier,
            lat = 0.0, lng = 0.0,
            headingVector = 0f,
            geoCellId = "NULL_CELL",
            collisionRadius = 2.5f,
            movementType = moveMap,
            baseSpeed = speed,
            interactionType = interactionLevel,
            spawnTimestamp = 0L,
            despawnTimestamp = 0L,
            assetUri = "asset://models/avatar_base_$baseId.gltf",
            audioUri = "asset://sounds/ping_$rarityTier.mp3",
            rewardId = "RWD_TIER_$rarityTier",
            claimHash = null
        )
    }
}
