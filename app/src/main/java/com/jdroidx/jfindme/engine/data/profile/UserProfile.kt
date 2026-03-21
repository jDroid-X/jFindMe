package com.jdroidx.jfindme.engine.data.profile

/**
 * User Profile SDK Model
 * QA Architecture: Anchors the Avatar Customization system directly into the Player's Profile identity.
 * Serves as the source of truth for generating their 2D Profile Picture from complex 3D arrays.
 */
data class UserProfile(
    val userId: String,
    val username: String,
    val playerLevel: Int,
    val xpProgress: Long,
    
    // SUBSET: The precise configuration of the Player's customized Avatar
    var avatarConfiguration: AvatarCustomization,
    
    // QA PERFORMANCE TARGET: The cached, composite 2D snapshot to prevent GPU exhaust on listviews.
    var profilePictureUri: String? = null
) {
    /**
     * Recompiles or changes the profile picture whenever the Avatar Configuration updates.
     */
    fun updateProfilePicture() {
        this.profilePictureUri = avatarConfiguration.generateCompositeSnapshotUri()
    }
}

/**
 * Subset Configuration of the User's specific 3D Avatar layout.
 * Mirrors the detailed apparel layers observed in the UI references (Hats, Shirts, Poses, Buddies).
 */
data class AvatarCustomization(
    // Biological / Base Features
    var baseMeshId: String = "BASE_BODY_1", 
    var skinToneHex: String = "#FFDBAC",
    var hairStyleId: String = "HAIR_DEFAULT",
    var eyeColorHex: String = "#000000",
    
    // Modular Apparel Inventory Links
    var hatId: String? = null,
    var topId: String = "SHIRT_STANDARD_WHITE",
    var bottomId: String = "PANTS_ACTIVE_BLACK",
    var footwearId: String = "SHOES_BASIC_TENNIS",
    var glassesId: String? = null,
    var backpackId: String? = null,
    
    // Companion Pet Link (Shown in references beside the Avatar)
    var companionPetId: String? = null
) {
    /**
     * Engine assembly logic mapping the customized IDs to a server-side generated profile image.
     */
    fun generateCompositeSnapshotUri(): String {
        // Generates a unique state ID resolving into a dynamic WebP or PNG picture file.
        // e.g., If the user changes their hat, it triggers a new image compile.
        val hash = "${baseMeshId}_${hairStyleId}_${topId}_${bottomId}_${hatId ?: "basic"}".hashCode()
        return "content://media/avatar_cache/composite_$hash.webp"
    }
}
