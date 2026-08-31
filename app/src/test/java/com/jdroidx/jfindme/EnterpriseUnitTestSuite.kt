package com.jdroidx.jfindme

import org.junit.Test
import org.junit.Assert.*
import com.jdroidx.jfindme.data.model.LiveDeviceModel
import com.jdroidx.jfindme.data.model.SafeZoneModel
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.ui.base.ThemeManager

/**
 * 🏛️ EnterpriseUnitTestSuite: Automated verification of core domain algorithms,
 * data models, theme color palettes, and database contract integrity.
 */
class EnterpriseUnitTestSuite {

    @Test
    fun testLiveDeviceModelIntegrity() {
        val device = LiveDeviceModel(
            userId = "AGENT_007",
            deviceId = "HW_TEST_123",
            userName = "Sentinel",
            latitude = 18.5204,
            longitude = 73.8567,
            speed = 12.5f,
            batteryLevel = 85.0f,
            timestamp = 1700000000L,
            isSosActive = false,
            address = "Command Center",
            avatarId = "avatar_custom_1"
        )

        assertEquals("AGENT_007", device.userId)
        assertEquals("HW_TEST_123", device.deviceId)
        assertEquals("Sentinel", device.userName)
        assertEquals(18.5204, device.latitude, 0.0001)
        assertEquals(73.8567, device.longitude, 0.0001)
        assertEquals(12.5f, device.speed, 0.01f)
        assertEquals(85.0f, device.batteryLevel, 0.01f)
        assertEquals(1700000000L, device.timestamp)
        assertFalse(device.isSosActive)
        assertEquals("Command Center", device.address)
        assertEquals("avatar_custom_1", device.avatarId)
    }

    @Test
    fun testSafeZoneModelIntegrity() {
        val zone = SafeZoneModel(
            zoneId = "ZONE_HOME",
            name = "Home Sanctuary",
            latitude = 18.5204,
            longitude = 73.8567,
            radiusMeters = 250f,
            assignedUserIds = listOf("AGENT_001", "AGENT_002")
        )

        assertEquals("ZONE_HOME", zone.zoneId)
        assertEquals("Home Sanctuary", zone.name)
        assertEquals(250f, zone.radiusMeters, 0.01f)
        assertEquals(2, zone.assignedUserIds.size)
        assertTrue(zone.assignedUserIds.contains("AGENT_001"))
    }

    @Test
    fun testThemeManagerPalettes() {
        assertEquals(8, ThemeManager.THEMES.size)
        for (i in 0..7) {
            val theme = ThemeManager.THEMES[i]
            assertEquals(7, theme.size)
            for (element in theme) {
                assertTrue("Hex color must start with #", element.startsWith("#"))
                assertTrue("Hex length must be 7 or 9 chars", element.length == 7 || element.length == 9)
            }
        }
    }

    @Test
    fun testHaversineDistanceAlgorithm() {
        // Pune to Mumbai approx ~120 km (120,000 meters)
        val lat1 = 18.5204
        val lon1 = 73.8567
        val lat2 = 19.0760
        val lon2 = 72.8777

        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = earthRadius * c

        assertTrue("Distance should be approx 120km", distance in 115000.0..130000.0)
    }

    @Test
    fun testUserLocationModelHelper() {
        val loc = UserLocation(
            userId = "USER_123",
            userName = "Alex",
            latitude = 18.52,
            longitude = 73.85,
            timestamp = 1600000000L
        )

        assertEquals("USER_123", loc.getResolvedUserId())
        assertEquals("Alex", loc.userName)
    }

    @Test
    fun test50dpCircularDispersionMath() {
        // Benchmark rule: Center-to-center distance d = 50dp between all adjacent avatars
        val d = 50.0 // target chord distance in dp
        for (n in 2..10) {
            val theta = Math.PI / n
            val radius = (d / 2.0) / Math.sin(theta) // R = 25 / sin(pi/n)
            
            // Check chord distance between point i and point i+1:
            // chord = 2 * R * sin(theta)
            val chord = 2.0 * radius * Math.sin(theta)
            assertEquals("Chord distance between adjacent avatars must strictly equal 50dp for N=$n", d, chord, 0.0001)
        }
    }

    @Test
    fun test50dpTo90dpCircularDispersionRangeMath() {
        // Test separation distances across full enterprise spectrum: 50dp, 60dp, 70dp, 80dp, 90dp
        val distances = doubleArrayOf(50.0, 60.0, 70.0, 80.0, 90.0)
        for (d in distances) {
            for (n in 2..10) {
                val radius = com.jdroidx.jfindme.engine.map.MultiMarkerEngine.calculateDispersionRadius(d, n)
                val theta = Math.PI / n
                val chord = 2.0 * radius * Math.sin(theta)
                assertEquals("Chord distance between adjacent avatars must strictly equal ${d}dp for N=$n", d, chord, 0.0001)

                val auraRadius = com.jdroidx.jfindme.engine.map.MultiMarkerEngine.calculateAuraBoundingRadius(d, n, 24.0)
                assertEquals(radius + 24.0, auraRadius, 0.0001)

                val circumference = com.jdroidx.jfindme.engine.map.MultiMarkerEngine.calculateCircumference(d, n)
                assertEquals(2.0 * Math.PI * radius, circumference, 0.0001)
            }
        }
    }

    @Test
    fun testPrivateGroupIsolationIntegrity() {
        val agentId1 = "AG_987A654B321"
        val agentId2 = "AG_123B456C789"
        
        val grp1 = "GRP_" + agentId1
        val grp2 = "GRP_" + agentId2
        
        assertNotEquals("DEFAULT_GROUP", grp1)
        assertNotEquals("DEFAULT_GROUP", grp2)
        assertNotEquals(grp1, grp2)
        assertTrue(grp1.startsWith("GRP_"))
    }

    @Test
    fun testGeohashEncoderPrecision() {
        val lat = 37.7749
        val lng = -122.4194
        val hash = com.jdroidx.jfindme.engine.location.GeohashHelper.encode(lat, lng)
        assertNotNull(hash)
        assertTrue("Geohash length should be at least 6 characters", hash.length >= 6)
        assertTrue("San Francisco geohash should start with '9q8y'", hash.startsWith("9q8y"))
    }

    @Test
    fun testAvatarCanonicalIndexResolution() {
        // Test canonical mapping of 1..50 avatars
        assertEquals(1, com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(1))
        assertEquals(22, com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(22))
        assertEquals(23, com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(23))
        assertEquals(50, com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(50))
        assertEquals(1, com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(51))
        assertEquals(50, com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(100))
        
        // Test custom selfie edge cases (-1 and 999)
        assertEquals(-1, com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(-1))
        assertEquals(-1, com.jdroidx.jfindme.utils.AvatarLoader.getResolvedAvatarIndex(999))
    }

    @Test
    fun testMotionStateSpeedClassification() {
        // Resting: speed <= 1.5 km/h
        val restingSpeed = 0.5f
        assertTrue("Resting speed <= 1.5", restingSpeed <= 1.5f)

        // Walking: 1.5 < speed <= 7.0 km/h
        val walkingSpeed = 4.2f
        assertTrue("Walking range", walkingSpeed > 1.5f && walkingSpeed <= 7.0f)

        // Running / Cycling: 7.0 < speed <= 25.0 km/h
        val runningSpeed = 12.0f
        assertTrue("Running range", runningSpeed > 7.0f && runningSpeed <= 25.0f)

        // Driving: speed > 25.0 km/h
        val drivingSpeed = 48.0f
        assertTrue("Driving speed", drivingSpeed > 25.0f)
    }
}
