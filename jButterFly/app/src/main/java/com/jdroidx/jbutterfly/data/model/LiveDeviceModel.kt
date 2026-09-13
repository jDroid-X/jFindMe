package com.jdroidx.jbutterfly.data.model

enum class DeviceRagStatus {
    ACTIVE,
    INACTIVE_SHORT,
    INACTIVE_LONG,
    EMERGENCY_SOS,
    GHOST
}

data class LiveDeviceModel(
    @JvmField val agentId: String = "",
    @JvmField val hardwareId: String = "",
    @JvmField val deviceName: String = "",
    @JvmField val role: String = "FRIEND", // GUARDIAN / CHILD / FRIEND
    @JvmField val category: String = "FAMILY", // FAMILY / FRIENDS
    @JvmField val avatarName: String = "avatar_custom_1",
    @JvmField val latitude: Double = 0.0,
    @JvmField val longitude: Double = 0.0,
    @JvmField val speed: Float = 0.0f,
    @JvmField val heading: Float = 0.0f,
    @JvmField val batteryPct: Int = 100,
    @JvmField val lastSeenTs: Long = 0L,
    @JvmField val isSos: Boolean = false,
    @JvmField val isGhost: Boolean = false
) {
    fun getRagStatus(): DeviceRagStatus {
        if (isSos) return DeviceRagStatus.EMERGENCY_SOS
        if (isGhost) return DeviceRagStatus.GHOST

        val elapsedMillis = System.currentTimeMillis() - lastSeenTs
        val fifteenMinutesMillis = 15 * 60 * 1000L
        val sixHoursMillis = 6 * 60 * 60 * 1000L

        return when {
            elapsedMillis < fifteenMinutesMillis && batteryPct > 20 -> DeviceRagStatus.ACTIVE
            elapsedMillis < sixHoursMillis || batteryPct <= 20 -> DeviceRagStatus.INACTIVE_SHORT
            else -> DeviceRagStatus.INACTIVE_LONG
        }
    }

    fun getRagColorHex(): String {
        return when (getRagStatus()) {
            DeviceRagStatus.ACTIVE -> "#22C55E"
            DeviceRagStatus.INACTIVE_SHORT -> "#F59E0B"
            DeviceRagStatus.INACTIVE_LONG -> "#EF4444"
            DeviceRagStatus.EMERGENCY_SOS -> "#DC2626"
            DeviceRagStatus.GHOST -> "#64748B"
        }
    }
}
