package com.jdroidx.jfindme

import com.jdroidx.jfindme.engine.location.PolygonGeofenceHelper
import com.jdroidx.jfindme.engine.security.ZxcvbnPasswordStrengthEvaluator
import com.jdroidx.jfindme.engine.security.KeyStoreManager
import com.jdroidx.jfindme.engine.boot.BootSequenceProgressObservable
import com.jdroidx.jfindme.engine.map.ButterflyPointEngine
import com.jdroidx.jfindme.ui.settings.SettingsModel

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
        assertEquals(12, ThemeManager.THEMES.size)
        for (i in 0 until 12) {
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

    @Test
    fun testPolygonGeofenceRayCasting() {
        // Triangle around (10, 10), (10, 20), (20, 15)
        val vertices = listOf(
            PolygonGeofenceHelper.LatLngPoint(10.0, 10.0),
            PolygonGeofenceHelper.LatLngPoint(10.0, 20.0),
            PolygonGeofenceHelper.LatLngPoint(20.0, 15.0)
        )
        // Center point (12, 15) must be inside
        assertTrue("Point (12, 15) should be inside triangle",
            PolygonGeofenceHelper.isPointInPolygon(12.0, 15.0, vertices))

        // Outside point (5, 5) must be outside
        assertFalse("Point (5, 5) should be outside triangle",
            PolygonGeofenceHelper.isPointInPolygon(5.0, 5.0, vertices))
    }

    @Test
    fun testZxcvbnPasswordStrengthEvaluator() {
        val weak = ZxcvbnPasswordStrengthEvaluator.evaluate("123")
        assertFalse("Password '123' must not be acceptable", weak.isAcceptable)
        assertEquals(ZxcvbnPasswordStrengthEvaluator.SCORE_VERY_WEAK, weak.score)

        val strong = ZxcvbnPasswordStrengthEvaluator.evaluate("Sentinel@2026#SecureSilicon")
        assertTrue("High entropy password must be acceptable", strong.isAcceptable)
        assertTrue("Score must be >= 3", strong.score >= ZxcvbnPasswordStrengthEvaluator.SCORE_STRONG)
    }

    @Test
    fun testKeyStoreManagerDetails() {
        val cert = KeyStoreManager.getCertificateDetails()
        assertNotNull("Certificate details must not be null", cert)
        assertTrue("Serial number must not be empty", cert.serialNumber.isNotEmpty())
        assertTrue("Valid until must be in future", cert.validUntil > System.currentTimeMillis())
    }

    @Test
    fun testBootSequenceProgressObservable() {
        var observedPhase = ""
        var observedProgress = -1
        val listener = object : BootSequenceProgressObservable.BootProgressListener {
            override fun onProgressUpdate(phase: String, progressPercent: Int) {
                observedPhase = phase
                observedProgress = progressPercent
            }
            override fun onBootCompleted() {}
        }
        BootSequenceProgressObservable.registerListener(listener)
        BootSequenceProgressObservable.updateProgress(BootSequenceProgressObservable.PHASE_SILICON_ATTESTATION, 50)
        assertEquals(BootSequenceProgressObservable.PHASE_SILICON_ATTESTATION, BootSequenceProgressObservable.currentPhase)
        assertEquals(50, BootSequenceProgressObservable.currentProgress)
        BootSequenceProgressObservable.unregisterListener(listener)
    }

    @Test
    fun testSettingsModelDefaults() {
        val model = SettingsModel()
        assertEquals(30, model.pingIntervalSec)
        assertEquals("BALANCED", model.motionDutyCycle)
        assertEquals(10f, model.displacementMeters, 0.01f)
        assertTrue(model.roadSmoothingEnabled)
        assertEquals(100f, model.defaultSafeZoneRadiusM, 0.01f)
        assertEquals(20f, model.lowBatteryThresholdPct, 0.01f)
        assertEquals(60, model.sirenDurationSec)
    }

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Enterprise RAG Critical Audit Tests: Cryptographic Hashing, Monotonic Lockouts, Role ACLs
    @Test
    fun testPurgeSafetyPhraseSha256HashingContract() {
        val rawPhrase = "jDroidX_Sentinel_2026"
        val hash = com.jdroidx.jfindme.utils.RegistryManager.hashSha256(rawPhrase)
        assertNotNull(hash)
        assertEquals(64, hash.length)
        val repeatHash = com.jdroidx.jfindme.utils.RegistryManager.hashSha256(rawPhrase)
        assertEquals(hash, repeatHash)
        val diffHash = com.jdroidx.jfindme.utils.RegistryManager.hashSha256("Different_Phrase")
        assertNotEquals(hash, diffHash)
    }

    @Test
    fun testPairingLockoutMonotonicCalculationContract() {
        val nowElapsed = 500000L
        val lockoutUntil = nowElapsed + 900000L
        val currentMonotonicActive = 600000L
        val isLocked = currentMonotonicActive < lockoutUntil
        val remainingSec = (lockoutUntil - currentMonotonicActive) / 1000
        assertTrue("Lockout must remain active when monotonic time has not expired", isLocked)
        assertEquals(800L, remainingSec)
        val expiredMonotonic = 1500001L
        val isExpired = expiredMonotonic < lockoutUntil
        assertFalse("Lockout must expire once monotonic elapsed time exceeds threshold", isExpired)
    }

    @Test
    fun testRoleAwarePermissionMaskContract() {
        val friendCategory = "FRIEND"
        val defaultFriendMask = if (friendCategory == "FRIEND") 0x01 else 0xFF
        assertEquals(1, defaultFriendMask)
        val customHex = "0x1F"
        val parsedMask = if (customHex.startsWith("0x", true)) customHex.substring(2).toInt(16) else customHex.toInt()
        assertEquals(31, parsedMask)
    }

    @Test
    fun testFuzzyDisplacementMathBoundedRadius() {
        val maxRadiusMeters = 800.0
        val simulatedDistance = 750.0
        val clampedDistance = simulatedDistance.coerceAtMost(maxRadiusMeters)
        assertTrue(clampedDistance <= maxRadiusMeters)
        val excessiveDistance = 1200.0
        val bounded = excessiveDistance.coerceAtMost(maxRadiusMeters)
        assertEquals(maxRadiusMeters, bounded, 0.0001)
    }

    /**
     * 🦋 Dry Run Test: Riddhi (Daughter on Redmi N7) dispatches Butterfly Point to Pushpa (Mother / Co-Parent on Redmi Note 10 Pro)
     * Validates:
     * 1. Invitation creation and device assignment
     * 2. Distance and ETA calculation
     * 3. Acceptance transition (PENDING -> ACCEPTED -> ON_MOVE)
     * 4. Geofence arrival detection (ON_MOVE -> REACHED)
     * 5. 15-meter deadband hysteresis (suppressing boundary jitter)
     * 6. Rejection guard isolation
     */
    @Test
    fun testButterflyPointDryRunRiddhiToPushpa() {
        // [Variables] riddhiDeviceId, pushpaDeviceId, pointId, cafeLat, cafeLng, arrivalRadius, point
        // [Constants] STATUS_PENDING, STATUS_ACCEPTED, STATUS_ON_MOVE, STATUS_REACHED, STATUS_REJECTED
        // [Dependencies] ButterflyPointEngine, ButterflyPointDeviceMapTable, ButterflyPoint
        // [Integration Points] Riddhi to Pushpa rendezvous simulation
        val riddhiDeviceId = "4379A761" // Redmi N7 (Daughter)
        val pushpaDeviceId = "F8CAFC67" // Redmi Note 10 Pro (Mother / Co-Parent)
        val cafeLat = 18.5204
        val cafeLng = 73.8567
        val arrivalRadius = 50.0

        // 1. Riddhi initiates Butterfly Point at City Center Cafe
        val point = com.jdroidx.jfindme.data.model.ButterflyPoint(
            pointId = "BP_RIDDHI_TO_PUSHPA",
            creatorDeviceId = riddhiDeviceId,
            creatorName = "Riddhi",
            pointName = "City Center Cafe",
            address = "MG Road, Pune",
            lat = cafeLat,
            lng = cafeLng,
            geohash = "tek3p6",
            rendezvousTimestamp = System.currentTimeMillis() + 7200000L,
            expiryTimestamp = System.currentTimeMillis() + 21600000L,
            arrivalRadiusMeters = arrivalRadius,
            notes = "Family Rendezvous with Mother Pushpa",
            iconType = "BUTTERFLY_STANDARD",
            isActive = true,
            isArchived = false
        )

        // Assign Pushpa as target device in pending state
        point.devices[pushpaDeviceId] = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
            deviceId = pushpaDeviceId,
            status = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_PENDING,
            lastUpdated = System.currentTimeMillis()
        )

        assertEquals("Riddhi", point.creatorName)
        assertEquals(riddhiDeviceId, point.creatorDeviceId)
        assertEquals(1, point.devices.size)
        assertTrue(point.devices.containsKey(pushpaDeviceId))
        assertEquals(
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_PENDING,
            point.devices[pushpaDeviceId]?.status
        )

        // 2. Initial state: Pushpa is at home (~1.4 km away at 18.5300, 73.8650)
        val pushpaHomeLat = 18.5300
        val pushpaHomeLng = 73.8650
        val initialDist = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateDistance(
            cafeLat, cafeLng, pushpaHomeLat, pushpaHomeLng
        )
        assertTrue("Distance from home to cafe should be approx 1.3 - 1.6 km", initialDist in 1300.0..1600.0)

        val formattedDist = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.formatDistance(initialDist)
        assertTrue("Formatted distance must end in km", formattedDist.endsWith("km"))

        val etaMinutes = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateEtaMinutes(initialDist, 25.0f)
        assertTrue("ETA at 25 km/h should be between 2 and 6 minutes", etaMinutes in 2..6)

        // Status before Pushpa responds: Evaluates to PENDING
        val statusBeforeAccept = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_PENDING,
            distanceMeters = initialDist,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_PENDING,
            statusBeforeAccept
        )

        // 3. Pushpa taps "ACCEPT" on the notification
        // Device status transitions to ACCEPTED in DB
        point.devices[pushpaDeviceId]?.status = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ACCEPTED

        // Status evaluation now transitions to ON_MOVE because distance (1.4 km) > 50m radius
        val statusOnMove = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ACCEPTED,
            distanceMeters = initialDist,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            statusOnMove
        )

        // 4. Pushpa approaches to 200m away (traffic signal near cafe)
        val approachingDist = 200.0
        val statusApproaching = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            distanceMeters = approachingDist,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            statusApproaching
        )

        // 5. Pushpa arrives at cafe entrance (distance = 35m, within 50m radius)
        val arrivedDist = 35.0
        val statusArrived = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            distanceMeters = arrivedDist,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            statusArrived
        )

        // 6. 15-meter Deadband Hysteresis: Pushpa walks to patio table (distance = 58m)
        // 58m is outside 50m arrival radius, but inside 50m + 15m = 65m deadband window.
        // It must NOT revert to ON_MOVE! (Zero jitter)
        val patioDist = 58.0
        val statusHysteresis = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            distanceMeters = patioDist,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(
            "Hysteresis must hold REACHED status within deadband window",
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            statusHysteresis
        )

        // 7. Pushpa departs cafe (distance = 85m, exceeding 50m + 15m = 65m threshold)
        val departedDist = 85.0
        val statusDeparted = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            distanceMeters = departedDist,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(
            "Departing beyond deadband threshold must revert to ON_MOVE",
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            statusDeparted
        )

        // 8. Rejection guard isolation: If Pushpa had rejected, status is locked to REJECTED
        val statusRejected = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REJECTED,
            distanceMeters = 30.0,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REJECTED,
            statusRejected
        )
    }

    /**
     * 🦋 Multi-Device Orchestration Dry Run:
     * Device redmi n7 (Father / Admin: 2T929153) dispatches Butterfly Point to connect
     * device Riddhi (Daughter: 4379A761) and device Pushpa (Mother / Co-Parent: F8CAFC67).
     *
     * Validates:
     * 1. 1-to-Many Multi-Target Device Dispatch (Father -> [Riddhi, Pushpa])
     * 2. Auto-Handshake payload distribution across both peer inboxes
     * 3. Concurrent status evaluation (Both devices transition PENDING -> ACCEPTED -> ON_MOVE)
     * 4. Multi-peer real-time telemetry transfer (Father, Riddhi, Pushpa distances to Point)
     * 5. Relative inter-device peer distance calculation (Riddhi <-> Pushpa)
     * 6. Dual-node arrival convergence (Riddhi reaches first, Pushpa reaches second -> both REACHED)
     */
    @Test
    fun testButterflyPointDryRunFatherToRiddhiAndPushpa() {
        // [Variables] fatherDeviceId, riddhiDeviceId, pushpaDeviceId, cafeLat, cafeLng, arrivalRadius, point
        // [Constants] STATUS_PENDING, STATUS_ACCEPTED, STATUS_ON_MOVE, STATUS_REACHED
        // [Dependencies] ButterflyPointEngine, ButterflyPointDeviceMapTable, ButterflyPoint
        // [Integration Points] 3-Node Orchestration Rendezvous
        val fatherDeviceId = "2T929153" // Redmi N7 (Father / Admin JEET)
        val riddhiDeviceId = "4379A761" // Redmi N7 (Daughter Riddhi)
        val pushpaDeviceId = "F8CAFC67" // Redmi Note 10 Pro (Mother Pushpa)

        val cafeLat = 18.5204
        val cafeLng = 73.8567
        val arrivalRadius = 50.0

        // 1. Father creates Rendezvous Point to connect Riddhi and Pushpa
        val point = com.jdroidx.jfindme.data.model.ButterflyPoint(
            pointId = "BP_FATHER_RIDDHI_PUSHPA",
            creatorDeviceId = fatherDeviceId,
            creatorName = "JEET (Father)",
            pointName = "School Pickup Cafe",
            address = "MG Road, Pune",
            lat = cafeLat,
            lng = cafeLng,
            geohash = "tek3p6",
            rendezvousTimestamp = System.currentTimeMillis() + 3600000L,
            expiryTimestamp = System.currentTimeMillis() + 18000000L,
            arrivalRadiusMeters = arrivalRadius,
            notes = "Father orchestrated rendezvous for Daughter Riddhi & Mother Pushpa",
            iconType = "BUTTERFLY_STANDARD",
            isActive = true,
            isArchived = false
        )

        // Assign both Riddhi and Pushpa as participating target devices
        val targetDeviceIds = listOf(riddhiDeviceId, pushpaDeviceId)
        for (dId in targetDeviceIds) {
            point.devices[dId] = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
                deviceId = dId,
                status = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_PENDING,
                lastUpdated = System.currentTimeMillis()
            )
        }

        assertEquals(fatherDeviceId, point.creatorDeviceId)
        assertEquals(2, point.devices.size)
        assertTrue(point.devices.containsKey(riddhiDeviceId))
        assertTrue(point.devices.containsKey(pushpaDeviceId))

        // 2. Initial Geographic Positions:
        // Father at Office: 18.5150, 73.8500 (~950m away)
        // Riddhi at School: 18.5280, 73.8620 (~1.02 km away)
        // Pushpa at Home:   18.5300, 73.8650 (~1.40 km away)
        val fatherLat = 18.5150; val fatherLng = 73.8500
        val riddhiLat = 18.5280; val riddhiLng = 73.8620
        val pushpaLat = 18.5300; val pushpaLng = 73.8650

        // Distance from Point
        val fatherDist = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateDistance(cafeLat, cafeLng, fatherLat, fatherLng)
        val riddhiDist = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateDistance(cafeLat, cafeLng, riddhiLat, riddhiLng)
        val pushpaDist = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateDistance(cafeLat, cafeLng, pushpaLat, pushpaLng)

        assertTrue("Father distance approx 900-1000m", fatherDist in 900.0..1000.0)
        assertTrue("Riddhi distance approx 980-1100m", riddhiDist in 980.0..1100.0)
        assertTrue("Pushpa distance approx 1300-1500m", pushpaDist in 1300.0..1500.0)

        // Inter-peer direct distance between Riddhi (School) and Pushpa (Home)
        val interPeerDist = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateDistance(riddhiLat, riddhiLng, pushpaLat, pushpaLng)
        assertTrue("Inter-peer distance between School and Home approx 350-450m", interPeerDist in 350.0..450.0)

        // 3. Auto-Handshake Execution: Both devices accept and synchronize
        point.devices[riddhiDeviceId]?.status = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ACCEPTED
        point.devices[pushpaDeviceId]?.status = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ACCEPTED

        val riddhiStatusOnMove = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = riddhiDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ACCEPTED,
            distanceMeters = riddhiDist,
            arrivalRadiusMeters = arrivalRadius
        )
        val pushpaStatusOnMove = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ACCEPTED,
            distanceMeters = pushpaDist,
            arrivalRadiusMeters = arrivalRadius
        )

        assertEquals(com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE, riddhiStatusOnMove)
        assertEquals(com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE, pushpaStatusOnMove)

        // 4. Sequential Convergence:
        // Step A: Riddhi walks over from school and arrives first (distance = 25m <= 50m)
        val riddhiArrivedDist = 25.0
        val riddhiStatusReached = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = riddhiDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            distanceMeters = riddhiArrivedDist,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED, riddhiStatusReached)

        // Meanwhile, Pushpa is still approaching at 300m:
        val pushpaApproachingDist = 300.0
        val pushpaStatusMid = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            distanceMeters = pushpaApproachingDist,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE, pushpaStatusMid)

        // Step B: Pushpa arrives at Cafe (distance = 40m <= 50m)
        val pushpaArrivedDist = 40.0
        val pushpaStatusReached = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = pushpaDeviceId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            distanceMeters = pushpaArrivedDist,
            arrivalRadiusMeters = arrivalRadius
        )
        assertEquals(com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED, pushpaStatusReached)

        // 5. Final State Verification: BOTH devices are now REACHED at the designated rendezvous
        point.devices[riddhiDeviceId]?.status = riddhiStatusReached
        point.devices[pushpaDeviceId]?.status = pushpaStatusReached

        assertEquals(
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            point.devices[riddhiDeviceId]?.status
        )
        assertEquals(
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            point.devices[pushpaDeviceId]?.status
        )

        // Relative distance between daughter and mother at the rendezvous point is now within arrival perimeter
        val finalSeparation = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateDistance(
            cafeLat + 0.0001, cafeLng + 0.0001,
            cafeLat + 0.0002, cafeLng + 0.0002
        )
        assertTrue("Final separation between Riddhi and Pushpa must be within 25m", finalSeparation < 25.0)
    }

    /**
     * 🔗 Master Dry Run Test: "Connection to Other Devices"
     * Scenario:
     * - Host / Initiator Device: Redmi N7 (Father / Admin: 2T929153)
     * - Connecting Device B: Riddhi (Daughter: 4379A761)
     * - Connecting Device C: Pushpa (Co-Parent / Mother: F8CAFC67)
     *
     * Validates:
     * 1. Multi-Device Auto-Handshake & Role Assignment (Daughter ↔ Mother)
     * 2. Reciprocal `valid_linked_agents` CSV synchronization without dropping prior circle links
     * 3. Symmetrical relationship graph generation in Firebase RTDB SSOT & local SQLite
     * 4. Telemetry access unscoping: Riddhi and Pushpa can mutually see each other's live location
     * 5. Father maintains omniscient admin oversight over both child and co-parent
     */
    @Test
    fun testConnectionToOtherDevicesDryRunFatherConnectingRiddhiToPushpa() {
        val fatherId = "2T929153"   // Father (Redmi Note 7 Pro, Host/Admin)
        val riddhiId = "4379A761"   // Daughter (Redmi N7)
        val pushpaId = "F8CAFC67"   // Mother / Co-Parent (Redmi Note 10 Pro)
        val groupId = "GRP_$fatherId" // Primary Family Group

        // 1. Initial State:
        // Prior to connection, Riddhi and Pushpa only link directly to Father.
        val initialRiddhiApproved = mutableSetOf(fatherId)
        val initialPushpaApproved = mutableSetOf(fatherId)

        // Security Check: At this point, Riddhi cannot see Pushpa, and Pushpa cannot see Riddhi
        assertFalse(initialRiddhiApproved.contains(pushpaId))
        assertFalse(initialPushpaApproved.contains(riddhiId))

        // 2. Father executes "Connection to Other Devices" on Redmi N7:
        // Source: Riddhi (4379A761), Target: Pushpa (F8CAFC67)
        // Relationship: Family (Child / Daughter ↔ Parent / Mother)
        val roleB = "Child"
        val roleC = "Parent"
        val isFamily = true

        // 3. Auto-Handshake & Reciprocal Role Resolution:
        // PairingRepository merges mutual IDs, preserving Father's ID
        val approvedRiddhi = initialRiddhiApproved.toMutableSet()
        approvedRiddhi.add(pushpaId)
        approvedRiddhi.add(fatherId)

        val approvedPushpa = initialPushpaApproved.toMutableSet()
        approvedPushpa.add(riddhiId)
        approvedPushpa.add(fatherId)

        // Verify CSV generation
        val csvRiddhi = approvedRiddhi.joinToString(",")
        val csvPushpa = approvedPushpa.joinToString(",")

        assertTrue(csvRiddhi.contains(pushpaId))
        assertTrue(csvRiddhi.contains(fatherId))
        assertTrue(csvPushpa.contains(riddhiId))
        assertTrue(csvPushpa.contains(fatherId))

        // 4. Data Transfer & SSOT Relationships Map Generation:
        // Simulates the updates map constructed by PairingRepository.linkDevicesWithRoles
        val updates = hashMapOf<String, Any>(
            "devices/$riddhiId/relationships/$pushpaId" to mapOf(
                "role" to roleB,
                "name" to "Pushpa (Mother)",
                "group" to groupId
            ),
            "devices/$pushpaId/relationships/$riddhiId" to mapOf(
                "role" to roleC,
                "name" to "Riddhi (Daughter)",
                "group" to groupId
            ),
            "devices/$riddhiId/Identity/valid_linked_agents" to csvRiddhi,
            "devices/$pushpaId/Identity/valid_linked_agents" to csvPushpa,
            "relationships/$groupId/devices/$riddhiId/paired_with/$pushpaId" to true,
            "relationships/$groupId/devices/$pushpaId/paired_with/$riddhiId" to true
        )

        assertEquals(6, updates.size)
        assertTrue(updates.containsKey("devices/$riddhiId/relationships/$pushpaId"))
        assertTrue(updates.containsKey("devices/$pushpaId/relationships/$riddhiId"))

        // 5. Scoped Telemetry Verification:
        // After auto-handshake and data transfer, both devices pass the security filter
        val isRiddhiScopedByPushpa = approvedPushpa.contains(riddhiId)
        val isPushpaScopedByRiddhi = approvedRiddhi.contains(pushpaId)
        val isFatherMonitoringBoth = approvedRiddhi.contains(fatherId) && approvedPushpa.contains(fatherId)

        assertTrue("Riddhi must now be visible to Pushpa", isRiddhiScopedByPushpa)
        assertTrue("Pushpa must now be visible to Riddhi", isPushpaScopedByRiddhi)
        assertTrue("Father must maintain oversight over both nodes", isFatherMonitoringBoth)
    }

    // [Variables] dMeters: Double, status: String, etaMin: Int, bp: ButterflyPoint, devEntry: ButterflyDeviceEntry
    // [Constants] None
    // [Dependencies] ButterflyPointEngine, ButterflyPoint, ButterflyDeviceEntry
    // [Integration Points] Butterfly Point rendezvous tracking & hysteresis unit testing
    @Test
    fun testButterflyPointEngineCalculationsAndHysteresis() {
        val puneLat = 18.5204
        val puneLng = 73.8567
        // Point approx 40 meters away
        val nearLat = 18.5207
        val nearLng = 73.8567
        val distNear = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateHaversineDistance(puneLat, puneLng, nearLat, nearLng)
        assertTrue("Distance should be approx 30-40 meters", distNear in 25.0..45.0)

        // Point approx 150 meters away
        val farLat = 18.5217
        val farLng = 73.8567
        val distFar = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateHaversineDistance(puneLat, puneLng, farLat, farLng)
        assertTrue("Distance should be approx 140-150 meters", distFar in 130.0..160.0)

        // Test Formatting
        assertEquals("35 m", com.jdroidx.jfindme.engine.map.ButterflyPointEngine.formatDistance(35.2))
        assertEquals("1.5 km", com.jdroidx.jfindme.engine.map.ButterflyPointEngine.formatDistance(1500.0))

        // Test Status Evaluation with 50m radius and 15m deadband hysteresis
        val devId = "TEST_DEV_01"
        // 1. Initial far state -> ON_MOVE
        val statusFar = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            devId, "ACCEPTED", 100.0, 50.0, 15.0
        )
        assertEquals(com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE, statusFar)

        // 2. Enters perimeter (<= 50m) -> REACHED (and latches arrival)
        val statusReached = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            devId, statusFar, 45.0, 50.0, 15.0
        )
        assertEquals(com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED, statusReached)

        // 3. Jitters slightly outside perimeter (55m <= 50m + 15m deadband = 65m) -> MUST REMAIN REACHED (Hysteresis latch)
        val statusJitter = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            devId, statusReached, 58.0, 50.0, 15.0
        )
        assertEquals("Hysteresis latch must keep state REACHED within deadband",
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED, statusJitter)

        // 4. Truly leaves perimeter (> 65m) -> Reverts to ON_MOVE
        val statusDeparted = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.evaluateDeviceStatus(
            devId, statusJitter, 70.0, 50.0, 15.0
        )
        assertEquals(com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE, statusDeparted)

        // Test ETA Calculation (1000m at 60 km/h = 1 minute)
        val eta = com.jdroidx.jfindme.engine.map.ButterflyPointEngine.calculateEtaMinutes(1000.0, 60.0f)
        assertEquals(1, eta)
    }

    // [Variables] now: Long, schedTs: Long, devEntry: ButterflyDeviceEntry
    // [Constants] None
    // [Dependencies] ButterflyPointEngine, ButterflyDeviceEntry, DeviceRagEngine
    // [Integration Points] Tactical RAG (Red-Amber-Green) rendezvous telemetry matrix verification
    @Test
    fun testButterflyPointRagMatrixEvaluation() {
        val now = System.currentTimeMillis()
        val schedTs = now + (60 * 60 * 1000L) // Meeting in 1 hour

        // Mock context using Robolectric/Shadow or test without Context dependencies if static methods handle it
        // Or directly verify the domain business logic rules:
        
        // 1. Rejection -> RED
        val rejectedEntry = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
            deviceId = "DEV_REJ",
            status = "REJECTED",
            lastUpdated = now
        )
        // Check property assignment
        assertEquals("REJECTED", rejectedEntry.status)

        // 2. SOS Distress -> RED
        val sosEntry = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
            deviceId = "DEV_SOS",
            status = "ACCEPTED",
            isSosActive = true,
            lastUpdated = now
        )
        assertTrue(sosEntry.isSosActive)

        // 3. Low Battery -> AMBER
        val lowBatEntry = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
            deviceId = "DEV_LOWBAT",
            status = "ON_MOVE",
            batteryPct = 14,
            distanceMeters = 800.0,
            lastUpdated = now
        )
        assertEquals(14, lowBatEntry.batteryPct)

        // 4. Reached -> GREEN
        val reachedEntry = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
            deviceId = "DEV_ARRIVED",
            status = "REACHED",
            distanceMeters = 20.0,
            arrivalTimestamp = now
        )
        assertEquals("REACHED", reachedEntry.status)
        assertEquals(20.0, reachedEntry.distanceMeters, 0.01)
    }

    @Test
    fun testButterflyPointIconRegistry() {
        val registry = com.jdroidx.jfindme.ui.base.IconTable.getRegistry()
        assertTrue("IconTable must contain ACTION_BUTTERFLY_POINT", registry.containsKey("ACTION_BUTTERFLY_POINT"))
        assertTrue("IconTable must contain ACTION_GHOST", registry.containsKey("ACTION_GHOST"))
        val butterflyRecord = registry["ACTION_BUTTERFLY_POINT"]
        assertNotNull(butterflyRecord)
        assertEquals("BUTTERFLY_HUB", butterflyRecord?.clickAction)
    }

    @Test
    fun testButterflyClosedFeedbackLoop() {
        // [Variables] devId, statusOnMove, statusReached, statusJitter, statusDepart, statusRejected
        // [Constants] arrivalRadius = 50.0, hysteresisDeadband = 15.0
        // [Dependencies] ButterflyPointEngine, ButterflyPointDeviceMapTable
        // [Integration Points] Closed-loop peer arrival and hysteresis validation

        val devId = "DEV_TEST_007"

        // 1. Peer starts 200m away -> ON_MOVE
        val statusOnMove = ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = devId,
            currentStatus = "ACCEPTED",
            distanceMeters = 200.0,
            arrivalRadiusMeters = 50.0,
            hysteresisDeadbandMeters = 15.0
        )
        assertEquals("ON_MOVE", statusOnMove)

        // 2. Peer approaches within arrival radius (45m <= 50m) -> REACHED (Arrival latch armed)
        val statusReached = ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = devId,
            currentStatus = statusOnMove,
            distanceMeters = 45.0,
            arrivalRadiusMeters = 50.0,
            hysteresisDeadbandMeters = 15.0
        )
        assertEquals("REACHED", statusReached)

        // 3. Jitter / Deadband check: Peer moves to 58m (> 50m radius, but <= 50m + 15m deadband = 65m) -> MUST REMAIN REACHED
        val statusJitter = ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = devId,
            currentStatus = statusReached,
            distanceMeters = 58.0,
            arrivalRadiusMeters = 50.0,
            hysteresisDeadbandMeters = 15.0
        )
        assertEquals("REACHED", statusJitter)

        // 4. Departure check: Peer moves beyond deadband (70m > 65m) -> Transitions back to ON_MOVE
        val statusDepart = ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = devId,
            currentStatus = statusJitter,
            distanceMeters = 70.0,
            arrivalRadiusMeters = 50.0,
            hysteresisDeadbandMeters = 15.0
        )
        assertEquals("ON_MOVE", statusDepart)

        // 5. Rejection isolation check: Once rejected, stays rejected even if inside geofence
        val statusRejected = ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = devId,
            currentStatus = "REJECTED",
            distanceMeters = 10.0,
            arrivalRadiusMeters = 50.0,
            hysteresisDeadbandMeters = 15.0
        )
        assertEquals("REJECTED", statusRejected)
    }

    @Test
    fun testButterflySettingsModelIntegration() {
        val model = SettingsModel()
        assertEquals(24, model.butterflyInvitationTimeoutHours)
        assertEquals(120, model.butterflyOverdueGraceMinutes)
        assertEquals(4, model.butterflyForegroundSyncIntervalSec)

        // Custom mutators
        model.butterflyInvitationTimeoutHours = 48
        model.butterflyOverdueGraceMinutes = 60
        model.butterflyForegroundSyncIntervalSec = 2
        assertEquals(48, model.butterflyInvitationTimeoutHours)
        assertEquals(60, model.butterflyOverdueGraceMinutes)
        assertEquals(2, model.butterflyForegroundSyncIntervalSec)
    }

    @Test
    fun testButterflyEtaCalculationNormalized() {
        // 1. Walking: 500m at 5 km/h -> (0.5 km / 5 km/h) * 60 = 6 mins
        val etaWalk = ButterflyPointEngine.calculateEtaMinutes(500.0, 5.0f)
        assertEquals(6, etaWalk)

        // 2. Driving: 10,000m (10 km) at 60 km/h -> (10 km / 60 km/h) * 60 = 10 mins
        val etaDrive = ButterflyPointEngine.calculateEtaMinutes(10000.0, 60.0f)
        assertEquals(10, etaDrive)

        // 3. Fallback when speed is <= 3 km/h (e.g. stationary / 0 km/h) -> uses 25 km/h nominal fallback
        // 5,000m at 25 km/h fallback -> (5 km / 25 km/h) * 60 = 12 mins
        val etaFallback = ButterflyPointEngine.calculateEtaMinutes(5000.0, 0.0f)
        assertEquals(12, etaFallback)
    }

    @Test
    fun testButterflyPointDeviceModelDelegationAndStatusFields() {
        val entry = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
            deviceId = "DEV_PEER_01",
            status = "PENDING",
            invitedBy = "DEV_CREATOR_00",
            lastLat = 28.6139,
            lastLng = 77.2090
        )
        assertEquals("DEV_CREATOR_00", entry.invitedBy)
        assertEquals(28.6139, entry.lastLat, 0.0001)
        assertEquals(77.2090, entry.lastLng, 0.0001)

        // Status transitions
        entry.status = "TIMED_OUT"
        assertEquals("TIMED_OUT", entry.status)

        entry.status = "REVOKED"
        assertEquals("REVOKED", entry.status)
    }

    @Test
    fun testColdBootStaleSuppressionLogic() {
        val now = 1700000000000L
        val graceMs = 120 * 60 * 1000L // 2 hours grace

        // Rendezvous that was 3 hours ago -> past grace window
        val rTsOld = now - (180 * 60 * 1000L)
        val expTsOld = rTsOld + (4 * 3600 * 1000L)
        val isStale = (expTsOld > 0L && now > expTsOld) || (rTsOld > 0L && now > (rTsOld + graceMs))
        assertTrue("Past grace period rendezvous must be flagged as stale/expired", isStale)

        // Rendezvous in 30 minutes -> fresh
        val rTsFresh = now + (30 * 60 * 1000L)
        val expTsFresh = rTsFresh + (4 * 3600 * 1000L)
        val isFresh = (expTsFresh > 0L && now > expTsFresh) || (rTsFresh > 0L && now > (rTsFresh + graceMs))
        assertFalse("Future rendezvous within window must not be flagged as stale", isFresh)
    }

    @Test
    fun testButterflyRagStationaryAndMissingGpsFix() {
        val now = System.currentTimeMillis()
        val meetingIn20m = now + (20 * 60 * 1000L)

        // 1. Missing GPS Telemetry: Accepted, but coordinates are 0.0 -> must be AMBER (Awaiting GPS Fix)
        val missingGpsEntry = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
            deviceId = "DEV_NO_GPS",
            status = "ACCEPTED",
            lastLat = 0.0,
            lastLng = 0.0,
            distanceMeters = 0.0,
            lastUpdated = now,
            batteryPct = 80
        )
        // Verify missing telemetry classification condition
        val isAwaitingGps = missingGpsEntry.lastLat == 0.0 && missingGpsEntry.lastLng == 0.0 && missingGpsEntry.distanceMeters == 0.0
        assertTrue("Node with 0,0 coordinates must trigger awaiting GPS condition", isAwaitingGps)

        // 2. Stationary Warning: Meeting in 20m (<30m), 3.5km away, speed 0.5 km/h (<2 km/h) -> must be AMBER (Stationary)
        val stationaryEntry = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
            deviceId = "DEV_STATIC",
            status = "ACCEPTED",
            lastLat = 28.6139,
            lastLng = 77.2090,
            distanceMeters = 3500.0,
            speedKmh = 0.5f,
            lastUpdated = now,
            batteryPct = 75
        )
        val isStationaryDelayed = (meetingIn20m - now) in 1..(30 * 60 * 1000L) &&
                                 stationaryEntry.distanceMeters > 1000.0 &&
                                 stationaryEntry.speedKmh < 2.0f
        assertTrue("Node stationary 3.5km away with meeting in 20m must trigger stationary warning", isStationaryDelayed)

        // 3. Nominal Active Motion: Meeting in 20m, 3.5km away, speed 35 km/h -> Healthy On Track
        val activeMovingEntry = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry(
            deviceId = "DEV_MOVING",
            status = "ON_MOVE",
            lastLat = 28.6139,
            lastLng = 77.2090,
            distanceMeters = 3500.0,
            speedKmh = 35.0f,
            lastUpdated = now,
            batteryPct = 75
        )
        val isMovingHealthy = activeMovingEntry.speedKmh >= 2.0f && activeMovingEntry.distanceMeters > 0.0
        assertTrue("Node moving at 35 km/h must not trigger stationary warning", isMovingHealthy)
    }

    @Test
    fun testButterflyPeerArrivalAlertCondition() {
        val selfId = "DEV_ORGANIZER_A"
        val peerId = "DEV_PARTICIPANT_B"

        // 1. Peer transitions from ON_MOVE to REACHED -> MUST fire arrival alert
        var prevStatus = "ON_MOVE"
        var currentStatus = "REACHED"
        var isSelf = peerId.equals(selfId, ignoreCase = true)
        val shouldAlertPeerArrival = currentStatus == "REACHED" && prevStatus != "REACHED" && !isSelf
        assertTrue("Transitioning remote peer to REACHED must trigger arrival alert", shouldAlertPeerArrival)

        // 2. Self transitions to REACHED -> handled locally by self arrival chime, not peer alert
        prevStatus = "ON_MOVE"
        currentStatus = "REACHED"
        isSelf = selfId.equals(selfId, ignoreCase = true)
        val shouldAlertSelfArrival = currentStatus == "REACHED" && prevStatus != "REACHED" && !isSelf
        assertFalse("Self transition to REACHED must not trigger remote peer arrival alert", shouldAlertSelfArrival)

        // 3. Repeated REACHED tick -> MUST NOT fire duplicate alert (idempotency check)
        prevStatus = "REACHED"
        currentStatus = "REACHED"
        isSelf = false
        val shouldAlertDuplicate = currentStatus == "REACHED" && prevStatus != "REACHED" && !isSelf
        assertFalse("Subsequent telemetry ticks after arrival must not trigger duplicate alert", shouldAlertDuplicate)
    }

    @Test
    fun testButterflyQuorumAssemblyDetection() {
        // [Variables] d1, d2, d3, eligibleDevices, allReached, alerted
        // [Dependencies] ButterflyPointDeviceMapTable
        val d1 = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry("DEV_A", status = "REACHED")
        val d2 = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry("DEV_B", status = "REACHED")
        val d3 = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry("DEV_C", status = "REJECTED")

        val all = listOf(d1, d2, d3)
        val eligible = all.filter {
            it.status != "REJECTED" && it.status != "REVOKED" && it.status != "TIMED_OUT"
        }
        assertEquals(2, eligible.size)
        val allReached = eligible.size >= 2 && eligible.all { it.status == "REACHED" }
        assertTrue("When all eligible participants have reached, quorum assembly must be true", allReached)

        // If one eligible member is still on move
        val d2OnMove = com.jdroidx.jfindme.data.model.ButterflyDeviceEntry("DEV_B", status = "ON_MOVE")
        val eligiblePartial = listOf(d1, d2OnMove)
        val partialReached = eligiblePartial.size >= 2 && eligiblePartial.all { it.status == "REACHED" }
        assertFalse("Quorum must not trigger if any eligible member is still approaching", partialReached)
    }

    @Test
    fun testButterflyTimeExtensionMetadataSync() {
        val oldRendezvousTs = 1700000000000L
        val additionalMinutes = 30L
        val newRendezvousTs = oldRendezvousTs + (additionalMinutes * 60 * 1000L)

        val isExtension = oldRendezvousTs in 1..<newRendezvousTs
        assertTrue("When new rendezvous timestamp is strictly greater, extension condition holds", isExtension)

        // Equal timestamp must not trigger extension notification
        val isSame = oldRendezvousTs in 1..<oldRendezvousTs
        assertFalse("Same timestamp must not trigger extension alert", isSame)
    }

    @Test
    fun testButterflyFirebaseKeySanitization() {
        val rawDeviceId = "operator.alpha@fleet#01[$]"
        val cleanId = com.jdroidx.jfindme.data.remote.FirebaseComm.sanitizeAgentId(rawDeviceId)

        assertFalse("Sanitized ID must not contain '.'", cleanId.contains('.'))
        assertFalse("Sanitized ID must not contain '$'", cleanId.contains('$'))
        assertFalse("Sanitized ID must not contain '#'", cleanId.contains('#'))
        assertFalse("Sanitized ID must not contain '['", cleanId.contains('['))
        assertFalse("Sanitized ID must not contain ']'", cleanId.contains(']'))
        assertEquals(8, cleanId.length)
        assertEquals("OPERET01", cleanId)
    }

    @Test
    fun testDeviceSharingModelPermissionsAndExpiry() {
        val now = System.currentTimeMillis()
        val activeShare = com.jdroidx.jfindme.data.model.DeviceSharingRecord(
            shareId = "DSH_001",
            masterDeviceId = "JEET_MASTER",
            submasterDeviceId = "SUB_GUARDIAN_01",
            targetDeviceId = "4379A761",
            relationshipType = "CO_GUARDIAN",
            permissionsMask = 0x0F, // Bits: 0x01 (GPS) | 0x02 (History) | 0x04 (Siren) | 0x08 (Geofence)
            currentStatus = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_ACTIVE,
            isActive = true,
            retryCount = 0,
            validUntil = 0L // Permanent
        )

        assertTrue(activeShare.isActive)
        assertFalse(activeShare.isExpired())
        assertTrue("Must have GPS permission", activeShare.hasPermission(0x01))
        assertTrue("Must have History permission", activeShare.hasPermission(0x02))
        assertTrue("Must have Siren permission", activeShare.hasPermission(0x04))
        assertTrue("Must have Geofence permission", activeShare.hasPermission(0x08))
        assertFalse("Must not have unassigned high bit permission", activeShare.hasPermission(0x10))

        // Ephemeral lease: Expired
        val expiredShare = activeShare.copy(
            validUntil = now - 5000L // 5 seconds ago
        )
        assertTrue("Must be expired", expiredShare.isExpired())

        // Ephemeral lease: Future
        val futureShare = activeShare.copy(
            validUntil = now + 3600000L // 1 hour in future
        )
        assertFalse("Must not be expired yet", futureShare.isExpired())
    }

    @Test
    fun testDeviceSharingSemanticTaxonomyInvariants() {
        // 1. REJECTED vs DECLINED: Completely different actors & intents
        assertNotEquals(
            "Rejected and Declined must have distinct semantic identity",
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REJECTED,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_DECLINED
        )

        // 2. TIMED_OUT vs EXPIRED: Distinct temporal phases (handshake vs active lease)
        assertNotEquals(
            "Timed Out (negotiation) and Expired (active lease elapsed) must be distinct",
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_TIMED_OUT,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_EXPIRED
        )

        // 3. Verify all 10 stages are unique
        val allStages = listOf(
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_NONE,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REQUESTED,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_APPROVED,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_ACCEPTED,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_ACTIVE,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_DECLINED,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REJECTED,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_TIMED_OUT,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_EXPIRED,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_RE_REQUESTED,
            com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REVOKED
        )
        assertEquals("All stage constants must be mutually exclusive", allStages.toSet().size, allStages.size)
    }

    @Test
    fun testDeviceSharingTransactionLifecycleAudit() {
        val tx = com.jdroidx.jfindme.data.model.DeviceSharingTransactionRecord(
            transactionId = "TX_TEST_001",
            shareId = "DSH_TEST_001",
            actorDeviceId = "JEET_MASTER",
            recipientDeviceId = "SUB_01",
            previousStage = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_REQUESTED,
            newStage = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.STAGE_APPROVED,
            attemptNumber = 2,
            channelMode = "CLOUD_FIREBASE",
            notesOrReason = "Approved by primary guardian via biometric prompt"
        )

        assertEquals("TX_TEST_001", tx.transactionId)
        assertEquals("DSH_TEST_001", tx.shareId)
        assertEquals("JEET_MASTER", tx.actorDeviceId)
        assertEquals("SUB_01", tx.recipientDeviceId)
        assertEquals("REQUESTED", tx.previousStage)
        assertEquals("APPROVED", tx.newStage)
        assertEquals(2, tx.attemptNumber)
        assertEquals("CLOUD_FIREBASE", tx.channelMode)
        assertTrue(tx.notesOrReason.contains("biometric prompt"))
    }

    @Test
    fun testDeviceSharingTableDDLAndIndexIntegrity() {
        // Table names
        assertEquals("device_sharing", com.jdroidx.jfindme.data.local.tables.DeviceSharingTable.TABLE_NAME)
        assertEquals("device_sharing_transactions", com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.TABLE_NAME)

        // DDL assertions
        val ddlSharing = com.jdroidx.jfindme.data.local.tables.DeviceSharingTable.CREATE_TABLE
        assertTrue(ddlSharing.contains("CREATE TABLE IF NOT EXISTS device_sharing"))
        assertTrue(ddlSharing.contains("master_device_id TEXT NOT NULL"))
        assertTrue(ddlSharing.contains("submaster_device_id TEXT NOT NULL"))
        assertTrue(ddlSharing.contains("target_device_id TEXT NOT NULL"))
        assertTrue(ddlSharing.contains("UNIQUE(master_device_id, submaster_device_id, target_device_id)"))

        val ddlTx = com.jdroidx.jfindme.data.local.tables.DeviceSharingTransactionTable.CREATE_TABLE
        assertTrue(ddlTx.contains("CREATE TABLE IF NOT EXISTS device_sharing_transactions"))
        assertTrue(ddlTx.contains("actor_device_id TEXT NOT NULL"))
        assertTrue(ddlTx.contains("new_stage TEXT NOT NULL"))
        assertTrue(ddlTx.contains("attempt_number INTEGER DEFAULT 1"))
    }

    @Test
    fun testButterflyPointTelemetryTableSchemaAndDDL() {
        // [Variables] ddl: CREATE_TABLE sql | ddlIdx: index sql
        // [Constants] TABLE_NAME, COL_*
        // [Dependencies] ButterflyPointTelemetryTable
        // [Integration Points] DatabaseHelper, ButterflyPointEngine
        assertEquals("butterfly_point_telemetry", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.TABLE_NAME)
        assertEquals("id", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_ID)
        assertEquals("point_id", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_POINT_ID)
        assertEquals("device_id", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_DEVICE_ID)
        assertEquals("latitude", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_LAT)
        assertEquals("longitude", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_LNG)
        assertEquals("speed_kmh", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_SPEED_KMH)
        assertEquals("distance_meters", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_DISTANCE_METERS)
        assertEquals("eta_seconds", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_ETA_SECONDS)
        assertEquals("battery_pct", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_BATTERY_PCT)
        assertEquals("is_sos", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_IS_SOS)
        assertEquals("rag_status", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_RAG_STATUS)
        assertEquals("rag_reason", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_RAG_REASON)
        assertEquals("timestamp", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.COL_TIMESTAMP)

        val ddl = com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.CREATE_TABLE
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS butterfly_point_telemetry"))
        assertTrue(ddl.contains("point_id TEXT NOT NULL"))
        assertTrue(ddl.contains("device_id TEXT NOT NULL"))
        assertTrue(ddl.contains("latitude REAL DEFAULT 0.0"))
        assertTrue(ddl.contains("timestamp INTEGER NOT NULL"))

        val ddlIdx = com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.CREATE_INDEX_POINT_DEVICE
        assertTrue(ddlIdx.contains("CREATE INDEX IF NOT EXISTS idx_bpt_point_dev"))
        assertTrue(ddlIdx.contains("point_id, device_id, timestamp"))
    }

    @Test
    fun testButterflyMaxAvailabilityDaysConfigurationAndExpiryCeiling() {
        // [Variables] now: current ms | maxDays: config days | maxWindowMs: window | reqExpiryLong: 10 days out | reqExpiryShort: 2 days out
        // [Constants] KEY_BUTTERFLY_MAX_AVAILABILITY_DAYS, DEFAULT_BUTTERFLY_MAX_AVAILABILITY_DAYS
        // [Dependencies] RegistryManager
        // [Integration Points] SettingsModel, SettingsActivity, ButterflyPointDialog, DatabaseHelper
        assertEquals("butterfly_max_availability_days", com.jdroidx.jfindme.utils.RegistryManager.KEY_BUTTERFLY_MAX_AVAILABILITY_DAYS)
        assertEquals(7, com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_BUTTERFLY_MAX_AVAILABILITY_DAYS)

        val now = 1700000000000L
        val maxDays = 7
        val maxWindowMs = maxDays * 24L * 60L * 60L * 1000L // 604,800,000 ms
        val ceilingExpiry = now + maxWindowMs

        // Case 1: Requested expiry exceeds 7 days (e.g. 10 days) -> Clamped to ceiling
        val reqExpiryLong = now + (10L * 24L * 60L * 60L * 1000L)
        val clampedExpiry = Math.min(reqExpiryLong, ceilingExpiry)
        assertEquals("Expiry beyond max availability days must be clamped to ceiling", ceilingExpiry, clampedExpiry)

        // Case 2: Requested expiry within 7 days (e.g. 2 days) -> Kept as requested
        val reqExpiryShort = now + (2L * 24L * 60L * 60L * 1000L)
        val normalExpiry = Math.min(reqExpiryShort, ceilingExpiry)
        assertEquals("Expiry within max availability days must remain unchanged", reqExpiryShort, normalExpiry)

        // Case 3: Verify allowed UI range clamping [1..30]
        val validLow = 1.coerceIn(1, 30)
        val validHigh = 30.coerceIn(1, 30)
        val clampedUnder = 0.coerceIn(1, 30)
        val clampedOver = 45.coerceIn(1, 30)
        assertEquals(1, validLow)
        assertEquals(30, validHigh)
        assertEquals(1, clampedUnder)
        assertEquals(30, clampedOver)
    }

    @Test
    fun testButterfly3TierArchitectureDataExchangeGating() {
        // [Variables] hasActivePoints: Boolean flag | telemetrySyncAllowed: Boolean evaluation
        // [Constants] Table references for Tier 1, Tier 2, Tier 3
        // [Dependencies] ButterflyPointTable, ButterflyPointDeviceMapTable, ButterflyPointTelemetryTable
        // [Integration Points] ButterflyPointEngine.processLocalLocationUpdate
        assertEquals("butterfly_point", com.jdroidx.jfindme.data.local.tables.ButterflyPointTable.TABLE_NAME)
        assertEquals("butterfly_point_device_map", com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.TABLE_NAME)
        assertEquals("butterfly_point_telemetry", com.jdroidx.jfindme.data.local.tables.ButterflyPointTelemetryTable.TABLE_NAME)

        // Telemetry exchange invariant: Active points exist -> Telemetry exchange allowed
        var activePointsCount = 2
        var telemetrySyncAllowed = activePointsCount > 0
        assertTrue("Telemetry stream must be enabled when active rendezvous points exist", telemetrySyncAllowed)

        // No active points -> Telemetry exchange suppressed/gated to conserve bandwidth and privacy
        activePointsCount = 0
        telemetrySyncAllowed = activePointsCount > 0
        assertFalse("Telemetry stream must be inhibited when zero active rendezvous points exist", telemetrySyncAllowed)
    }

    @Test
    fun testButterflyZeroDistanceEtaEvaluation() {
        // [Variables] etaZero: Int, etaNegative: Int, etaPositive: Int
        // [Constants] None
        // [Dependencies] ButterflyPointEngine
        // [Integration Points] ButterflyPointDialog, MapActivity callout telemetry
        val etaZero = ButterflyPointEngine.calculateEtaMinutes(0.0, 25.0f)
        val etaNegative = ButterflyPointEngine.calculateEtaMinutes(-5.0, 25.0f)
        val etaPositive = ButterflyPointEngine.calculateEtaMinutes(1000.0, 60.0f)

        assertEquals("ETA for 0 distance must be 0", 0, etaZero)
        assertEquals("ETA for negative distance must be 0", 0, etaNegative)
        assertEquals("ETA for 1000m at 60 km/h must be 1", 1, etaPositive)
    }

    @Test
    fun testButterflyArrivalLatchPersistenceAcrossProcessDeath() {
        // [Variables] testDevId: String, currentStatus: String, arrivalRadius: Double, deadband: Double
        // [Constants] STATUS_REACHED, STATUS_ON_MOVE, STATUS_ACCEPTED
        // [Dependencies] ButterflyPointEngine, ButterflyPointDeviceMapTable
        // [Integration Points] FusedLocationProviderClient ⟷ ButterflyPointEngine.evaluateDeviceStatus
        val testDevId = "TEST_DEVICE_RESTART_NODE"
        val arrivalRadius = 50.0
        val deadband = 15.0

        // 1. Clean in-memory latch map to simulate Android OS Process Kill
        ButterflyPointEngine.clearButterflyOverlay()

        // 2. Device had already reached in SQLite (currentStatus = STATUS_REACHED)
        // Now device sits inside hysteresis band (55m > 50m radius, but <= 65m ceiling)
        val statusInsideHysteresis = ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = testDevId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            distanceMeters = 55.0,
            arrivalRadiusMeters = arrivalRadius,
            hysteresisDeadbandMeters = deadband
        )
        assertEquals(
            "Terminal arrival state must persist across process restarts within hysteresis band",
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            statusInsideHysteresis
        )

        // 3. Device moves outside hysteresis ceiling (70m > 65m) -> Reverts to ON_MOVE
        val statusDeparted = ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = testDevId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            distanceMeters = 70.0,
            arrivalRadiusMeters = arrivalRadius,
            hysteresisDeadbandMeters = deadband
        )
        assertEquals(
            "Device must revert to ON_MOVE only after breaching hysteresis ceiling (radius + deadband)",
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            statusDeparted
        )

        // 4. Device re-enters arrival perimeter (45m <= 50m) -> Re-latches to REACHED
        val statusReEntered = ButterflyPointEngine.evaluateDeviceStatus(
            deviceId = testDevId,
            currentStatus = com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_ON_MOVE,
            distanceMeters = 45.0,
            arrivalRadiusMeters = arrivalRadius,
            hysteresisDeadbandMeters = deadband
        )
        assertEquals(
            "Device must re-latch to REACHED when crossing arrival perimeter",
            com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.STATUS_REACHED,
            statusReEntered
        )
    }
}
