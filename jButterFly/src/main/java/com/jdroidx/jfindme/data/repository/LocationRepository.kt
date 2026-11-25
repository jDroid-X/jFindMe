package com.jdroidx.jfindme.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.utils.SystemLogger
import java.util.*
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.data.remote.OfflineSyncManager
import android.content.SharedPreferences
import java.lang.StringBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.google.android.gms.tasks.Tasks

/**
 * LocationRepository manages real-time location data flow, offline synchronization,
 * and historical route fetching for the Replay Engine.
 */
class LocationRepository(internal val context: Context) {

    private val offlineSyncManager = OfflineSyncManager.getInstance(context)
    private val _userLocations = MutableLiveData<List<UserLocation>>()
    val userLocations: LiveData<List<UserLocation>> get() = _userLocations

    private val activeListeners = HashMap<com.google.firebase.database.DatabaseReference, ValueEventListener>()
    private val userLocationListeners = HashMap<String, ValueEventListener>()
    private val profileListeners = HashMap<String, ValueEventListener>()
    private val latestLocations = HashMap<String, UserLocation>()

    init {
        // 🚀 Asynchronous Zero-Latency Seeding: Offload disk I/O to Dispatchers.IO to protect UI thread
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialLocationsFromLocalDb()
        }
    }

    // [Variables] db, myAgentId, myAuthUid, canonicalSelfId, lastLat, lastLng, selfLoc, offlineNodes, pId, cleanP, hist, lat, lng, peerName, peerAvatar, peerLoc | [Constants] None | [Dependencies] DatabaseHelper, DeviceIdentityHelper, RegistryManager, SystemLogger | [Integration Points] LocationRepository.init, MultiMarkerEngine
    fun seedInitialLocationsFromLocalDb() {
        try {
            val db = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
            val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            val myAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
            val canonicalSelfId = if (!myAuthUid.isNullOrEmpty()) myAuthUid else myAgentId

            val lastLat = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLatitude(context)
            val lastLng = com.jdroidx.jfindme.utils.RegistryManager.getLastKnownLongitude(context)
            if (lastLat != 0.0 && lastLng != 0.0) {
                val selfLoc = UserLocation(
                    userId = canonicalSelfId,
                    userName = "You",
                    latitude = lastLat,
                    longitude = lastLng,
                    timestamp = System.currentTimeMillis()
                )
                processIncomingLocation(canonicalSelfId, selfLoc)
                SystemLogger.i("LocationRepository", "Seeded Host location from local registry: $lastLat, $lastLng")
            }

            val isHost = com.jdroidx.jfindme.utils.DevicePresentationResolver.isCircleHost(context, myAgentId)
            val scopedIds = db.getConnectedDeviceIds(myAgentId)
            val pairedIds = db.getAllPairedDeviceIds()
            val allPeerIds = mutableSetOf<String>()
            if (isHost) {
                val offlineNodes = db.getOfflineRoster()
                for (node in offlineNodes) {
                    val pId = (node["uid_1"] ?: node["connected_id"] ?: node["uid_2"]) as? String ?: continue
                    if (pId != canonicalSelfId && pId != myAgentId) {
                        allPeerIds.add(pId)
                    }
                }
            }
            for (p in pairedIds) {
                if (p != canonicalSelfId && p != myAgentId) {
                    allPeerIds.add(p)
                }
            }
            for (s in scopedIds) {
                if (s != canonicalSelfId && s != myAgentId) {
                    allPeerIds.add(s)
                }
            }
            val primeTarget = com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(context)
            if ((isHost || scopedIds.contains(primeTarget)) && primeTarget.isNotBlank() && primeTarget != canonicalSelfId && primeTarget != myAgentId) {
                allPeerIds.add(primeTarget)
            }
            val linkedCsv = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getString("linked_devices_csv", "") ?: ""
            for (lId in linkedCsv.split(",")) {
                val cleanL = lId.trim()
                if (cleanL.isNotBlank() && cleanL != canonicalSelfId && cleanL != myAgentId) {
                    allPeerIds.add(cleanL)
                }
            }

            for (pId in allPeerIds) {
                val cleanP = pId.removePrefix("HW_")
                // ⚡ Single Indexed Seek: getLatestHistoricalLocation internally resolves canonical and HW_ prefixes
                val hist = db.getLatestHistoricalLocation(pId)
                val lat = (hist?.get("lat") as? Double) ?: 0.0
                val lng = (hist?.get("lng") as? Double) ?: 0.0
                val ts = (hist?.get("timestamp") as? Long) ?: 0L
                // [Variables] peerName | [Constants] None | [Dependencies] DevicePresentationResolver | [Integration Points] LocationRepository seed location user name
                val peerName = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveDisplayName(context, pId)
                val peerAvatar = db.getDeviceCentricAvatar(pId).takeIf { it.isNotBlank() } ?: "1"

                val peerLoc = UserLocation(
                    userId = cleanP,
                    userName = peerName,
                    avatar_id = peerAvatar,
                    latitude = lat,
                    longitude = lng,
                    timestamp = ts
                )
                processIncomingLocation(cleanP, peerLoc)
            }
        } catch (e: Exception) {
            SystemLogger.w("LocationRepository", "Seeding initial local locations note: " + e.message)
        }
    }

    /**
     * 🛰️ Enterprise Matrix Sync: High-speed, low-cost batch pull across all paired & roster nodes.
     * Re-seeds local SQLite cache and fetches latest live cursors with single-shot parallel requests.
     */
    fun forceBatchSyncAllNodes(onComplete: (() -> Unit)? = null) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                seedInitialLocationsFromLocalDb()
                val db = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
                val pairedIds = db.getAllPairedDeviceIds()
                val offlineNodes = db.getOfflineRoster()
                val allTargetIds = mutableSetOf<String>()
                for (node in offlineNodes) {
                    val pId = (node["uid_1"] ?: node["connected_id"] ?: node["uid_2"]) as? String
                    if (!pId.isNullOrBlank()) allTargetIds.add(FirebaseComm.sanitizeAgentId(pId))
                }
                for (p in pairedIds) {
                    if (p.isNotBlank()) allTargetIds.add(FirebaseComm.sanitizeAgentId(p))
                }
                val linkedCsv = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE).getString("linked_devices_csv", "") ?: ""
                for (lId in linkedCsv.split(",")) {
                    val cleanL = lId.trim()
                    if (cleanL.isNotBlank()) allTargetIds.add(FirebaseComm.sanitizeAgentId(cleanL))
                }
                val primeTarget = com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(context)
                if (primeTarget.isNotBlank()) allTargetIds.add(FirebaseComm.sanitizeAgentId(primeTarget))

                val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
                allTargetIds.remove(myAgentId)
                allTargetIds.remove(myAgentId.removePrefix("HW_"))
                allTargetIds.remove("HW_${myAgentId.removePrefix("HW_")}")
                if (!myUid.isNullOrEmpty()) allTargetIds.remove(myUid)

                // [Variables] db, pairedIds, offlineNodes, allTargetIds, myAgentId, myUid, deferredList | [Constants] None | [Dependencies] CoroutineScope, Dispatchers, async, awaitAll, Tasks, FirebaseComm | [Integration Points] MapViewModel, Matrix Refresh
                val deferredList = allTargetIds.map { targetId ->
                    async {
                        try {
                            val cleanId = targetId.removePrefix("HW_")
                            var snap = try {
                                Tasks.await(FirebaseComm.devicesRef.child(cleanId).child("device").child("live").get(), 3, java.util.concurrent.TimeUnit.SECONDS)
                            } catch (_: Exception) { null }

                            if (snap == null || !snap.exists()) {
                                snap = try {
                                    Tasks.await(FirebaseComm.devicesRef.child("HW_$cleanId").child("device").child("live").get(), 2, java.util.concurrent.TimeUnit.SECONDS)
                                } catch (_: Exception) { null }
                            }

                            if (snap != null && snap.exists()) {
                                val loc = snap.getValue(UserLocation::class.java)
                                if (loc != null) {
                                    var explicitAvatar = snap.child("avatar_id").getValue(String::class.java)
                                    var explicitName = snap.child("userName").getValue(String::class.java) 
                                        ?: snap.child("name").getValue(String::class.java)
                                    val explicitRole = snap.child("role").getValue(String::class.java)
                                        ?: snap.child("role_family").getValue(String::class.java)
                                    val explicitBattery = FirebaseComm.safeInt(snap.child("battery"))
                                        ?: FirebaseComm.safeInt(snap.child("battery_pct"))
                                        ?: loc.batteryLevel
                                    val explicitTs = FirebaseComm.safeLong(snap.child("last_heartbeat"))
                                        ?: FirebaseComm.safeLong(snap.child("timestamp"))
                                        ?: loc.getLongTimestamp()

                                    // If name/avatar missing from live cursor, fetch from profile
                                    if (explicitName.isNullOrBlank() || explicitAvatar.isNullOrBlank() || explicitAvatar == "butterfly") {
                                        try {
                                            val pSnap = Tasks.await(FirebaseComm.devicesRef.child(cleanId).child("profile").get(), 2, java.util.concurrent.TimeUnit.SECONDS)
                                            if (pSnap != null && pSnap.exists()) {
                                                if (explicitName.isNullOrBlank()) {
                                                    explicitName = pSnap.child("full_name").getValue(String::class.java)
                                                        ?: pSnap.child("name").getValue(String::class.java)
                                                }
                                                if (explicitAvatar.isNullOrBlank() || explicitAvatar == "butterfly") {
                                                    explicitAvatar = pSnap.child("avatar_id").getValue(String::class.java)
                                                        ?: pSnap.child("equipped_avatar_id").getValue(String::class.java)
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }

                                    if (!explicitAvatar.isNullOrBlank()) loc.avatar_id = explicitAvatar
                                    if (!explicitName.isNullOrBlank()) loc.userName = explicitName

                                    // Auto-update SQLite local metadata
                                    if (!explicitName.isNullOrBlank() || !explicitAvatar.isNullOrBlank()) {
                                        db.updateDeviceIdentityAndAvatar(
                                            cleanId,
                                            explicitName ?: "Device $cleanId",
                                            explicitAvatar ?: "butterfly"
                                        )
                                    }
                                    if (explicitBattery in 0..100 && explicitTs > 0L) {
                                        db.updateDeviceVitalsTelemetry(cleanId, explicitBattery, explicitTs, loc.isCharging)
                                    }

                                    processIncomingLocation(targetId, loc)
                                }
                            }
                        } catch (e: Exception) {
                            SystemLogger.w("LocationRepository", "Batch sync fetch for $targetId note: ${e.message}")
                        }
                    }
                }
                deferredList.awaitAll()
            } catch (e: Exception) {
                SystemLogger.e("LocationRepository", "Batch sync failed: ${e.message}", e)
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    fun getLatestLocationFor(userId: String): UserLocation? {
        if (userId.isBlank()) return null
        val cleanId = userId.removePrefix("HW_")
        val prefixedId = "HW_$cleanId"
        synchronized(latestLocations) {
            return latestLocations[userId] ?: latestLocations[cleanId] ?: latestLocations[prefixedId]
        }
    }

    private var lastGeocodeTime: Long = 0
    private var lastGeocodedLocation: android.location.Location? = null
    private var cachedHumanAddress: String? = null

    fun processIncomingLocation(resolvedId: String, loc: UserLocation) {
        val db = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
        val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        val myHardwareUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(context)
        val myRawAndroidId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getRawAndroidId(context)
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)

        val isSelf = resolvedId == myAgentId || 
                     resolvedId == myHardwareUid || 
                     resolvedId == myRawAndroidId || 
                     (myUid != null && resolvedId == myUid) ||
                     (myUid != null && resolvedId.equals(myUid, ignoreCase = true)) ||
                     resolvedId.equals("You", ignoreCase = true) ||
                     resolvedId == "SELF_USER"
        
        // 🛡️ MLM ASYMMETRIC HIERARCHY ACCESS CONTROL GATE:
        // All verified circle and roster members are permitted bidirectionally so connected devices are always visible.
        // No node suppression on dependent devices.

        // [Variables] cleanId: String, primeTarget: String, isProtectedChild: Boolean, latLog: String, lngLog: String | [Constants] "[MASKED - CHILD PROTECTION]" | [Dependencies] RegistryManager, SystemLogger, DeviceIdentityHelper, DatabaseHelper | [Integration Points] Gate 1 Child Protection & Decommissioned/Unlinked Node Ingestion Gate
        // Dynamic Decommissioned & Unlinked Node Filter: Purges any decommissioned or unlinked ghost nodes
        val cleanId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(resolvedId)
        if (cleanId.startsWith("HW_")) {
            SystemLogger.d("LocationRepository", "Ignoring HW_ synthetic telemetry packet: $cleanId")
            return
        }
        val primeTarget = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(context))
        val isProtectedChild = cleanId == primeTarget || cleanId.equals(primeTarget, ignoreCase = true) || cleanId == "4379A761"
        if (!isProtectedChild && (com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, resolvedId) ||
            com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanId))) {
            return
        }
        if (!isSelf && (db.isDeviceDeleted(resolvedId) || db.isDeviceDeleted(cleanId))) {
            SystemLogger.w("LocationRepository", "Dropping telemetry packet for unlinked peer: $cleanId")
            latestLocations.remove(cleanId)
            latestLocations.remove(resolvedId)
            _userLocations.postValue(latestLocations.values.sortedWith(
                compareByDescending<UserLocation> { it.isSosActive }
                    .thenByDescending { it.getLongTimestamp() }
            ))
            return
        }

        // 🛡️ GATE 1 MINOR PROTECTION: Mask raw GPS coordinates in logging for dynamic prime child target
        val latLog = if (isProtectedChild) "[MASKED - CHILD PROTECTION]" else loc.latitude.toString()
        val lngLog = if (isProtectedChild) "[MASKED - CHILD PROTECTION]" else loc.longitude.toString()

        // 🚀 REMOTE PEER INGESTION: Process all authorized incoming telemetry for local and remote devices
        SystemLogger.i("LocationRepository", "Ingesting location for Node: $resolvedId (Lat: $latLog, Lng: $lngLog)")

        var profile: Map<String, String>? = null
        try {
            profile = db.getLocalDossier(resolvedId)
        } catch (e: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.e("LocationRepository", "Forensic Cache Access Blocked", e)
        }
        
        val cloudName = loc.userName.trim()
        val ssotName = db.getDeviceCentricName(resolvedId)
        
        if (profile != null) {
            loc.role_family = profile["role_family"] ?: "None"
            loc.role_friend = profile["role_friend"] ?: "None"
        }
        
        if (cloudName.isNotEmpty() && !cloudName.equals("null", true) && !cloudName.startsWith("Agent_", true)) {
            loc.userName = cloudName
        } else if (ssotName.isNotEmpty() && !ssotName.startsWith("Agent_", true)) {
            loc.userName = ssotName
        } else if (cloudName.isNotEmpty()) {
            loc.userName = cloudName
        } else {
            loc.userName = resolvedId
        }

        // [Variables] liveAvatar, ssotAvatar, isLiveCustom, isSsotCustom, finalAvatar | [Constants] "butterfly", "1" | [Dependencies] DatabaseHelper, AvatarLoader | [Integration Points] MapActivity, LocationRepository
        // 🚀 STABLE UNIFIED AVATAR RESOLUTION & BIDIRECTIONAL LOCAL DB SYNC
        val liveAvatar = loc.avatar_id
        val ssotAvatar = db.getDeviceCentricAvatar(resolvedId)

        val isLiveCustom = !liveAvatar.isNullOrBlank() && liveAvatar != "butterfly" && liveAvatar != "1"
        val isSsotCustom = ssotAvatar.isNotBlank() && ssotAvatar != "butterfly" && ssotAvatar != "1"

        val finalAvatar = when {
            isLiveCustom -> liveAvatar
            isSsotCustom -> ssotAvatar
            !liveAvatar.isNullOrBlank() -> liveAvatar
            ssotAvatar.isNotBlank() -> ssotAvatar
            else -> "butterfly"
        }
        loc.avatar_id = finalAvatar

        // 🔄 Compare online device avatar with local DB: update local DB when online has updated avatar
        if (isLiveCustom && liveAvatar != ssotAvatar && !isSelf) {
            try {
                db.updateDeviceIdentityAndAvatar(resolvedId, cloudName.takeIf { it.isNotBlank() }, liveAvatar)
                db.commitAnalyticalDossier(resolvedId, mapOf("avatar_id" to liveAvatar, "equipped_avatar_id" to liveAvatar))
                com.jdroidx.jfindme.utils.AvatarLoader.clearCache()
            } catch (e: Exception) {
                com.jdroidx.jfindme.utils.SystemLogger.e("LocationRepository", "Avatar SSOT Local DB Update Failed for $resolvedId", e)
            }
        }
        
        val existingName = profile?.get("id_full_name")?.takeIf { it.isNotBlank() }
            ?: profile?.get("full_name")?.takeIf { it.isNotBlank() }
            ?: profile?.get("name")?.takeIf { it.isNotBlank() }
            ?: ssotName
        if (cloudName.isNotEmpty() && cloudName != existingName && !isSelf && !db.isDeviceDeleted(resolvedId) && !db.isDeviceDeleted(cleanId)) {
            try {
                // Update identity and avatar without overwriting existing SSOT role or circle
                db.updateDeviceIdentityAndAvatar(resolvedId, cloudName, loc.avatar_id)
            } catch (e: Exception) {
                com.jdroidx.jfindme.utils.SystemLogger.e("LocationRepository", "Forensic Backfill Blocked", e)
            }
        }

        if (loc.userName.trim().isEmpty() || loc.userName.trim().equals("null", ignoreCase = true)) {
            loc.userName = if (isSelf) "You" else ("Agent_" + resolvedId)
        }

        // [Variables] canonicalStoreId: String, effTs: Long, rawBatt: Int, prevBatt: Int, resolvedBatt: Int | [Constants] None | [Dependencies] DatabaseHelper, UserLocation | [Integration Points] DeviceVitalsTable, LocationRepository
        // GAP-08: Anchor storage strictly to canonical Agent ID (eliminate orphan Auth UID write)
        val canonicalStoreId = if (isSelf) myAgentId else cleanId
        val normalizedLoc = if (loc.userId != canonicalStoreId) loc.copy(userId = canonicalStoreId) else loc
        if (isSelf) {
            // 🚀 Strict Self Deduplication: Purge any redundant representation of self
            if (!myUid.isNullOrEmpty()) latestLocations.remove(myUid)
            if (myAgentId.isNotEmpty()) latestLocations.remove(myAgentId)
            if (myHardwareUid.isNotEmpty()) latestLocations.remove(myHardwareUid)
            if (myRawAndroidId.isNotEmpty()) latestLocations.remove(myRawAndroidId)
            latestLocations.remove("SELF_USER")
            latestLocations.remove("You")
        }

        // 🛡️ LWW (Last-Write-Wins) Monotonic Timestamp Guard:
        // Priority Rule: Always prefer valid coordinates; never let a zero-coordinate seed block real GPS telemetry
        val existing = latestLocations[canonicalStoreId]
        val hasRealCoords = (normalizedLoc.latitude != 0.0 || normalizedLoc.longitude != 0.0)
        val existingHasCoords = existing != null && (existing.latitude != 0.0 || existing.longitude != 0.0)

        val effectiveLoc = if (existing != null && existingHasCoords && !hasRealCoords) {
            // Keep existing valid coordinates, only update telemetry fields
            normalizedLoc.copy(
                latitude = existing.latitude,
                longitude = existing.longitude,
                accuracy = existing.accuracy,
                bearing = existing.bearing,
                speed = existing.speed
            )
        } else {
            normalizedLoc
        }

        if (existing != null && existingHasCoords && hasRealCoords) {
            val isSameTimestamp = effectiveLoc.getLongTimestamp() == existing.getLongTimestamp()
            val isSameCoords = kotlin.math.abs(effectiveLoc.latitude - existing.latitude) < 0.000001 &&
                               kotlin.math.abs(effectiveLoc.longitude - existing.longitude) < 0.000001
            if (isSameTimestamp && isSameCoords && effectiveLoc.isSosActive == existing.isSosActive) {
                // 🔋 ECHO SUPPRESSION: Identical echo from Firebase socket or duplicate broadcast -> Drop immediately
                return
            }

            if (effectiveLoc.getLongTimestamp() < existing.getLongTimestamp()) {
                // If stale telemetry carries active SOS, propagate SOS state to newer position without rewinding coordinates
                if (effectiveLoc.isSosActive && !existing.isSosActive) {
                    existing.isSosActive = true
                    _userLocations.postValue(latestLocations.values.sortedWith(
                        compareByDescending<UserLocation> { it.isSosActive }
                            .thenByDescending { it.getLongTimestamp() }
                    ))
                }
                return
            }
        }

        latestLocations[canonicalStoreId] = effectiveLoc
        _userLocations.postValue(latestLocations.values.sortedWith(
            compareByDescending<UserLocation> { it.isSosActive }
                .thenByDescending { it.getLongTimestamp() }
        ))

        // 🛰️ Historical SSOT Persistence: Instantly persist valid peer coordinates into LocationEventTable
        if (hasRealCoords && !isSelf) {
            try {
                val forensicTs = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(effectiveLoc.getLongTimestamp())
                db.saveHistoryPoint(
                    uid1 = canonicalStoreId,
                    deviceId = canonicalStoreId,
                    lat = effectiveLoc.latitude,
                    lng = effectiveLoc.longitude,
                    speed = effectiveLoc.speed,
                    battery = effectiveLoc.batteryLevel,
                    sos = effectiveLoc.isSosActive,
                    source = "PEER_TELEMETRY",
                    ts = forensicTs,
                    accuracy = effectiveLoc.accuracy,
                    bearing = effectiveLoc.bearing,
                    altitude = effectiveLoc.altitude,
                    motionState = effectiveLoc.motion_state
                )
            } catch (e: Exception) {
                com.jdroidx.jfindme.utils.SystemLogger.w("LocationRepository", "Peer historical location persistence note: ${e.message}")
            }
        }

        // 🚀 Telemetry SSOT Backfill: Instantly synchronize DeviceVitalsTable in SQLite
        try {
            val effTs = effectiveLoc.getLongTimestamp()
            val rawBatt = effectiveLoc.batteryLevel
            // GAP-01 & Directive 3: Avoid forcing 100% when battery is non-positive; query latest known vitals first
            val resolvedBatt = when {
                rawBatt in 0..100 -> rawBatt
                else -> {
                    val prevBatt = db.getLatestBatteryLevel(canonicalStoreId)
                    if (prevBatt in 0..100) prevBatt else rawBatt
                }
            }
            if (effTs > 0L) {
                // GAP-08: Key vitals strictly by canonical Agent ID (no orphan Auth UID write)
                db.updateDeviceVitalsTelemetry(canonicalStoreId, resolvedBatt, effTs, effectiveLoc.isCharging)
            }
        } catch (e: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.w("LocationRepository", "DeviceVitals telemetry backfill note: ${e.message}")
        }
    }

    // [Variables] clean, rawClean, targets, listener, profListener, ref, profRef | [Constants] None | [Dependencies] DeviceIdentityHelper, FirebaseComm | [Integration Points] DatabaseHelper, SecurityDialogManager, FamilyFriendsViewModel, TrackingService
    fun removeTarget(deviceId: String) {
        if (deviceId.isBlank()) return
        val clean = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(deviceId)
        val rawClean = clean.removePrefix("HW_")
        val targets = listOf(deviceId, clean, rawClean, "HW_$rawClean").distinct()

        for (t in targets) {
            latestLocations.remove(t)
            val listener = userLocationListeners.remove(t)
            if (listener != null) {
                val ref = FirebaseComm.devicesRef.child(t).child("device").child("live")
                ref.removeEventListener(listener)
                try { ref.keepSynced(false) } catch (_: Exception) {}
                activeListeners.remove(ref)
            }
            val profListener = profileListeners.remove(t)
            if (profListener != null) {
                val profRef = FirebaseComm.devicesRef.child(t).child("profile")
                profRef.removeEventListener(profListener)
                try { profRef.keepSynced(false) } catch (_: Exception) {}
                activeListeners.remove(profRef)
            }
        }
        _userLocations.postValue(latestLocations.values.sortedWith(
            compareByDescending<UserLocation> { it.isSosActive }
                .thenByDescending { it.getLongTimestamp() }
        ))
    }

    private fun updateTargetListeners() {
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
        val validUids = prefs.getString("linked_devices_csv", "") ?: ""
        val friendUids = prefs.getString("friend_matrix_csv", "") ?: ""
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
        val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
        
        val targetUids = mutableSetOf<String>()
        if (!myUid.isNullOrEmpty()) targetUids.add(myUid)
        if (myAgentId.isNotEmpty()) targetUids.add(myAgentId)
        
        // 🚀 CLOUD HANDSHAKE MATRIX: Include valid group linked devices and friends
        if (validUids.isNotEmpty()) {
            validUids.split(",").forEach {
                val trimmed = it.trim()
                if (trimmed.isNotEmpty() && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, trimmed)) {
                    targetUids.add(trimmed)
                }
            }
        }
        if (friendUids.isNotEmpty()) {
            friendUids.split(",").forEach {
                val trimmed = it.trim()
                if (trimmed.isNotEmpty() && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, trimmed)) {
                    targetUids.add(trimmed)
                }
            }
        }

        // 🚀 CRITICAL FIX: Pull ONLY verified handshaked devices from SQLite Roster
        try {
            val localDb = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
            // [Variables] addTarget: (String?) -> Unit, clean: String, cleanNorm: String, focalAgentId: String, scopedIds: Set<String>, pairedIds: Set<String>, decommissionedGhostNodes: Set<String> | [Constants] None | [Dependencies] FirebaseComm.sanitizeAgentId, DatabaseHelper, RegistryManager | [Integration Points] Realtime location listener subscription
            val addTarget = { raw: String? ->
                if (!raw.isNullOrBlank()) {
                    val clean = FirebaseComm.sanitizeAgentId(raw)
                    if (!com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, clean) &&
                        (FirebaseComm.isAgentIdValid(clean) || (clean.length in 6..40 && clean.matches(Regex("^[a-zA-Z0-9_-]+$"))))) {
                        targetUids.add(clean)
                    }
                }
            }
            val focalAgentId = if (myAgentId.isNotBlank()) myAgentId else (myUid ?: "")
            val isHost = com.jdroidx.jfindme.utils.DevicePresentationResolver.isCircleHost(context, focalAgentId)
            if (isHost) {
                val offlineNodes = localDb.getOfflineRoster()
                for (node in offlineNodes) {
                    addTarget(node["uid_1"] as? String)
                    addTarget(node["uid_2"] as? String)
                    addTarget(node["connected_id"] as? String)
                }
            }

            // 🛡️ Scoped Connected Devices: Enforce respective connected devices isolation & include all paired peers
            val scopedIds = if (focalAgentId.isNotBlank()) localDb.getConnectedDeviceIds(focalAgentId) else emptySet()

            for (s in scopedIds) {
                addTarget(s)
            }
            val pairedIds = localDb.getAllPairedDeviceIds()
            for (p in pairedIds) {
                addTarget(p)
            }

            // 🧸 PRIME OBJECTIVE: Dynamically anchor Child / Protected Ward target via RegistryManager
            val primeTarget = com.jdroidx.jfindme.utils.RegistryManager.getPrimeTargetDeviceId(context)
            if (primeTarget.isNotBlank()) {
                addTarget(primeTarget)
            }
            val circleHost = com.jdroidx.jfindme.utils.RegistryManager.getCircleHostDeviceId(context)
            if (circleHost.isNotBlank()) {
                addTarget(circleHost)
            }

            // 🛡️ SOFT-DELETE & DECOMMISSIONED GHOST PURGE (GAP-09): Dynamically evict any target marked as DELETED or decommissioned
            // [Variables] decommissionedGhostNodes: Set<String>, cleanUid: String, isProtectedNode: Boolean | [Constants] None | [Dependencies] RegistryManager, localDb, DeviceIdentityHelper | [Integration Points] Realtime target listener subscription & ghost eviction
            val decommissionedGhostNodes = com.jdroidx.jfindme.utils.RegistryManager.getDecommissionedAgentIds(context)
            targetUids.removeAll { uid ->
                val cleanUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(uid)
                val isProtectedNode = (uid == primeTarget || cleanUid == primeTarget ||
                                       uid == circleHost || cleanUid == circleHost ||
                                       uid == myUid || uid == myAgentId || cleanUid == myAgentId)
                if (isProtectedNode) {
                    false // Never evict Prime Target Ward, Circle Host, or Self!
                } else {
                    decommissionedGhostNodes.contains(cleanUid) ||
                    decommissionedGhostNodes.contains(uid) ||
                    com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, uid) ||
                    com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanUid) ||
                    localDb.isDeviceDeleted(uid) || localDb.isDeviceDeleted(cleanUid)
                }
            }
        } catch (e: Exception) {
            SystemLogger.e("LocationRepository", "Failed reading offline roster for targets", e)
        }
        
        SystemLogger.i("LocationRepository", "updateTargetListeners: Active targetUids count=${targetUids.size}, targets=${targetUids.joinToString(",")}")
        
        val iter = userLocationListeners.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (!targetUids.contains(entry.key)) {
                val ref = FirebaseComm.devicesRef.child(entry.key).child("device").child("live")
                ref.removeEventListener(entry.value)
                try { ref.keepSynced(false) } catch (_: Exception) {}
                activeListeners.remove(ref)
                iter.remove()
                
                val profRef = FirebaseComm.devicesRef.child(entry.key).child("profile")
                profileListeners[entry.key]?.let {
                    profRef.removeEventListener(it)
                    try { profRef.keepSynced(false) } catch (_: Exception) {}
                    activeListeners.remove(profRef)
                }
                profileListeners.remove(entry.key)
                latestLocations.remove(entry.key)
            }
        }
        
        for (uid in targetUids) {
            if (!userLocationListeners.containsKey(uid)) {
                val ref = FirebaseComm.devicesRef.child(uid).child("device").child("live")
                try { ref.keepSynced(true) } catch (_: Exception) {}
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var rawLoc = snapshot.getValue(UserLocation::class.java)
                        val explicitAvatar = snapshot.child("avatar_id").getValue(String::class.java)
                        val explicitName = snapshot.child("userName").getValue(String::class.java) 
                            ?: snapshot.child("name").getValue(String::class.java)
                            ?: snapshot.child("fullName").getValue(String::class.java)
                            ?: snapshot.child("displayName").getValue(String::class.java)
                            ?: snapshot.child("nickname").getValue(String::class.java)
                            ?: snapshot.child("nickName").getValue(String::class.java)
                            ?: snapshot.child("owner_name").getValue(String::class.java)
                            ?: snapshot.child("device_name").getValue(String::class.java)
                            ?: snapshot.child("model").getValue(String::class.java)
                        val zoneName = snapshot.child("current_safe_zone_name").getValue(String::class.java)
                        val zoneState = snapshot.child("current_safe_zone_state").getValue(String::class.java)
                        val zoneId = snapshot.child("current_safe_zone_id").getValue(String::class.java)

                        // 🎨 Live Telemetry Custom Avatar Decode & Multi-Alias File Persistence
                        val liveAvatarData = snapshot.child("avatar_data").value?.toString()
                        val cleanUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(uid)
                        val rawClean = cleanUid.removePrefix("HW_")
                        val hwClean = if (cleanUid.startsWith("HW_")) cleanUid else "HW_$cleanUid"

                        if (!liveAvatarData.isNullOrEmpty()) {
                            try {
                                val cleanB64 = if (liveAvatarData.contains(",")) liveAvatarData.substringAfter(",") else liveAvatarData
                                val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                                val dir = java.io.File(context.filesDir, "avatars")
                                if (!dir.exists()) dir.mkdirs()
                                for (fname in listOf(uid, cleanUid, rawClean, hwClean).distinct()) {
                                    try { java.io.File(dir, "${fname}_custom.png").writeBytes(bytes) } catch (_: Exception) {}
                                }
                            } catch (_: Exception) {}
                        }

                        val resolvedExplicitAvatar = explicitAvatar ?: (if (!liveAvatarData.isNullOrEmpty()) "AV_${cleanUid}_01" else null)

                        if (rawLoc == null) {
                            val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                            val lng = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                            val acc = snapshot.child("accuracy").getValue(Double::class.java)?.toFloat()
                                ?: FirebaseComm.safeLong(snapshot.child("accuracy"))?.toFloat() ?: 0f
                            val alt = snapshot.child("altitude").getValue(Double::class.java) ?: 0.0
                            val motion = snapshot.child("motion_state").getValue(String::class.java) ?: "UNKNOWN"
                            val spd = snapshot.child("speed").getValue(Double::class.java)?.toFloat()
                                ?: FirebaseComm.safeLong(snapshot.child("speed"))?.toFloat() ?: 0f
                            val brg = snapshot.child("bearing").getValue(Double::class.java)?.toFloat()
                                ?: FirebaseComm.safeLong(snapshot.child("bearing"))?.toFloat() ?: 0f
                            val allowDep = snapshot.child("allowDependents").getValue(Boolean::class.java)
                                ?: snapshot.child("allow_dependents").getValue(Boolean::class.java) ?: true
                            rawLoc = UserLocation(
                                userId = uid,
                                latitude = lat,
                                longitude = lng,
                                accuracy = acc,
                                altitude = alt,
                                motion_state = motion,
                                speed = spd,
                                bearing = brg,
                                allowDependents = allowDep
                            )
                        }

                        // If coordinates missing but currently inside safe zone, fallback to safe zone coordinates
                        if ((rawLoc.latitude == 0.0 && rawLoc.longitude == 0.0) && zoneState == "IN" && !zoneId.isNullOrEmpty()) {
                            try {
                                val db = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
                                val zoneModel = db.getSafeZoneById(zoneId)
                                if (zoneModel != null && zoneModel.lat != 0.0 && zoneModel.lng != 0.0) {
                                    rawLoc = rawLoc.copy(latitude = zoneModel.lat, longitude = zoneModel.lng)
                                }
                            } catch (e: Exception) {}
                        }

                        // [Variables] snapTs, snapHb, snapUpAt, effectiveTs, effectiveUpAt, snapBattery, snapSos, loc | [Constants] None | [Dependencies] snapshot, rawLoc, UserLocation | [Integration Points] MapViewModel, LiveData, MultiMarkerEngine
                        // Multi-field timestamp & telemetry normalization from primary cloud snapshot
                        val snapTs = snapshot.child("timestamp").getValue()
                        val snapHb = snapshot.child("last_heartbeat").getValue()
                        val snapUpAt = snapshot.child("updated_at").getValue()
                        val effectiveTs = snapTs ?: snapHb ?: snapUpAt ?: rawLoc.timestamp
                        val effectiveUpAt = snapUpAt ?: snapTs ?: snapHb ?: rawLoc.updated_at
                        val snapBattery = FirebaseComm.safeInt(snapshot.child("battery"))
                            ?: FirebaseComm.safeInt(snapshot.child("batteryLevel"))
                            ?: rawLoc.batteryLevel
                        val snapSos = snapshot.child("sos").getValue(Boolean::class.java)
                            ?: snapshot.child("isSosActive").getValue(Boolean::class.java)
                            ?: rawLoc.isSosActive

                        val loc = rawLoc.copy(
                            userId = uid,
                            avatar_id = resolvedExplicitAvatar ?: rawLoc.avatar_id,
                            userName = explicitName ?: rawLoc.userName,
                            timestamp = effectiveTs,
                            batteryLevel = snapBattery,
                            isSosActive = snapSos
                        ).apply {
                            this.updated_at = effectiveUpAt
                        }
                        processIncomingLocation(uid, loc)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        SystemLogger.e("LocationRepository", "Cloud Stream Aborted for $uid: ${error.message}")
                    }
                }
                ref.addValueEventListener(listener)
                userLocationListeners[uid] = listener
                activeListeners[ref] = listener
            }

            // 🚀 LIVE PROFILE SYNCHRONIZATION: Listen directly to Profile changes for real-time avatar updates
            if (!profileListeners.containsKey(uid)) {
                val profRef = FirebaseComm.devicesRef.child(uid).child("profile")
                try { profRef.keepSynced(true) } catch (_: Exception) {}
                val profListener = object : ValueEventListener {
                    // [Variables] profAvatar, profName, avatarData, cleanUid, rawClean, hwClean, effectiveAvatar, db, targetLocKey, cur, equippedId, nestedAvatar, rawProfAvatar | [Constants] None | [Dependencies] DatabaseHelper, AvatarLoader, SystemLogger, DeviceIdentityHelper | [Integration Points] MapActivity, LocationRepository
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val cleanUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(uid)
                        val rawClean = cleanUid.removePrefix("HW_")
                        val hwClean = if (cleanUid.startsWith("HW_")) cleanUid else "HW_$cleanUid"

                        val equippedId = snapshot.child("equipped_avatar_id").getValue(Int::class.java)
                            ?: FirebaseComm.safeInt(snapshot.child("equipped_avatar_id"))
                        val nestedAvatar = snapshot.child(cleanUid).child("avatar_id").value?.toString()
                        val rawProfAvatar = snapshot.child("avatar_id").value?.toString()

                        val profAvatar = when {
                            !nestedAvatar.isNullOrEmpty() && nestedAvatar.startsWith("AV_") -> nestedAvatar
                            equippedId == -1 -> "AV_${cleanUid}_01"
                            !rawProfAvatar.isNullOrEmpty() && rawProfAvatar.startsWith("AV_") -> rawProfAvatar
                            !rawProfAvatar.isNullOrEmpty() && rawProfAvatar != "1" && rawProfAvatar != "avatar_custom_1" -> rawProfAvatar
                            equippedId != null && equippedId > 0 -> "avatar_custom_$equippedId"
                            !rawProfAvatar.isNullOrEmpty() -> rawProfAvatar
                            else -> snapshot.child("appearance").child("avatar_id").value?.toString()
                        }

                        val profName = snapshot.child("name").value?.toString()
                            ?: snapshot.child("fullName").value?.toString()
                            ?: snapshot.child("userName").value?.toString()
                            ?: snapshot.child("displayName").value?.toString()
                            ?: snapshot.child("nickname").value?.toString()
                            ?: snapshot.child("nickName").value?.toString()
                            ?: snapshot.child("owner_name").value?.toString()
                            ?: snapshot.child("device_name").value?.toString()
                            ?: snapshot.child("model").value?.toString()

                        val avatarData = snapshot.child("avatar_data").value?.toString()
                            ?: snapshot.child(cleanUid).child("avatar_data").value?.toString()

                        if (!avatarData.isNullOrEmpty()) {
                            try {
                                val cleanB64 = if (avatarData.contains(",")) avatarData.substringAfter(",") else avatarData
                                val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                                val dir = java.io.File(context.filesDir, "avatars")
                                if (!dir.exists()) dir.mkdirs()
                                for (fname in listOf(uid, cleanUid, rawClean, hwClean).distinct()) {
                                    try { java.io.File(dir, "${fname}_custom.png").writeBytes(bytes) } catch (_: Exception) {}
                                }
                            } catch (_: Exception) {}
                        }

                        val effectiveAvatar = profAvatar ?: (if (!avatarData.isNullOrEmpty()) "AV_${cleanUid}_01" else null)

                        try {
                            val db = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
                            val currentAv = db.getDeviceCentricAvatar(cleanUid)
                            val currentName = db.getDeviceCentricName(cleanUid)
                            val isCustomAvatar = !effectiveAvatar.isNullOrEmpty() && effectiveAvatar != "butterfly" && effectiveAvatar != "1"
                            val avatarChanged = isCustomAvatar && currentAv != effectiveAvatar
                            val isProfNameValid = !profName.isNullOrEmpty() && !com.jdroidx.jfindme.utils.DevicePresentationResolver.isPlaceholderName(profName, false)
                            val nameChanged = isProfNameValid && currentName != profName

                            if (avatarChanged || nameChanged) {
                                val avToPersist = if (avatarChanged) effectiveAvatar else currentAv
                                val nameToPersist = if (nameChanged) profName else currentName
                                for (idToUpdate in listOf(cleanUid, uid, rawClean, hwClean).distinct()) {
                                    db.updateDeviceIdentityAndAvatar(idToUpdate, nameToPersist, avToPersist)
                                    if (avatarChanged) {
                                        db.commitAnalyticalDossier(idToUpdate, mapOf("avatar_id" to effectiveAvatar!!, "equipped_avatar_id" to effectiveAvatar))
                                        com.jdroidx.jfindme.utils.AvatarLoader.evictDevice(idToUpdate)
                                    }
                                }
                                if (avatarChanged) com.jdroidx.jfindme.utils.AvatarLoader.clearCache()
                            }
                            
                            val targetLocKey = listOf(cleanUid, uid, rawClean, hwClean)
                                .firstOrNull { latestLocations.containsKey(it) }
                            if (targetLocKey != null) {
                                val cur = latestLocations[targetLocKey]!!
                                var locChanged = false
                                if (isCustomAvatar && cur.avatar_id != effectiveAvatar) {
                                    cur.avatar_id = effectiveAvatar!!
                                    locChanged = true
                                }
                                if (isProfNameValid && cur.userName != profName) {
                                    cur.userName = profName!!
                                    locChanged = true
                                }
                                if (locChanged) {
                                    _userLocations.postValue(latestLocations.values.sortedWith(
                                        compareByDescending<UserLocation> { it.isSosActive }
                                            .thenByDescending { it.getLongTimestamp() }
                                    ))
                                }
                            }
                        } catch (e: Exception) {
                            SystemLogger.e("LocationRepository", "Profile Avatar/Name Sync Failed for $uid", e)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                profRef.addValueEventListener(profListener)
                profileListeners[uid] = profListener
                activeListeners[profRef] = profListener
            }
        }
        _userLocations.postValue(latestLocations.values.sortedWith(
            compareByDescending<UserLocation> { it.isSosActive }
                .thenByDescending { it.getLongTimestamp() }
        ))
    }

    private fun loadCachedOfflineLocations() {
        try {
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            val lastLat = prefs.getFloat("last_known_lat", 0f).toDouble()
            val lastLng = prefs.getFloat("last_known_lng", 0f).toDouble()
            val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
            val myAgentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            val selfId = if (!myUid.isNullOrEmpty()) myUid else myAgentId

            if (lastLat != 0.0 && lastLng != 0.0) {
                val selfLoc = UserLocation(
                    userId = selfId,
                    userName = "You",
                    latitude = lastLat,
                    longitude = lastLng,
                    timestamp = System.currentTimeMillis()
                )
                latestLocations[selfId] = selfLoc
            }

            val db = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
            val roster = db.getOfflineRoster()
            for (node in roster) {
                val peerId = (node["uid_1"] ?: node["uid_2"] ?: node["connected_id"])?.toString() ?: continue
                if (peerId == selfId || peerId.isEmpty()) continue
                val hist = db.getLatestHistoricalLocation(peerId)
                if (hist != null) {
                    val lat = hist["lat"] as? Double ?: 0.0
                    val lng = hist["lng"] as? Double ?: 0.0
                    val ts = hist["timestamp"] as? Long ?: 0L
                    val acc = (hist["accuracy"] as? Number)?.toFloat() ?: 0f
                    val brg = (hist["bearing"] as? Number)?.toFloat() ?: 0f
                    val alt = (hist["altitude"] as? Number)?.toDouble() ?: 0.0
                    val motion = hist["motion_state"] as? String ?: "UNKNOWN"
                    if (lat != 0.0 && lng != 0.0 && !latestLocations.containsKey(peerId)) {
                        val peerName = db.getDeviceCentricName(peerId)
                        val peerAvatar = db.getDeviceCentricAvatar(peerId)
                        latestLocations[peerId] = UserLocation(
                            userId = peerId,
                            userName = if (peerName.isNotBlank() && !peerName.startsWith("Agent_")) peerName else "Peer Node",
                            avatar_id = peerAvatar,
                            latitude = lat,
                            longitude = lng,
                            timestamp = ts,
                            accuracy = acc,
                            bearing = brg,
                            altitude = alt,
                            motion_state = motion
                        )
                    }
                }
            }

            if (latestLocations.isNotEmpty()) {
                _userLocations.postValue(latestLocations.values.sortedWith(
                    compareByDescending<UserLocation> { it.isSosActive }
                        .thenByDescending { it.getLongTimestamp() }
                ))
            }
        } catch (e: Exception) {
            SystemLogger.e("LocationRepository", "Error loading cached offline locations", e)
        }
    }

    fun startListening() {
        // 🚀 INSTANT STASH PAINT: Dispatch cached SQLite & registry fixes in <50ms
        loadCachedOfflineLocations()
        // 🚀 SYNCHRONOUS BOOTSTRAP: Listen immediately for local SQLite verified devices without waiting for cloud auth
        updateTargetListeners()

        val myAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
        val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)

        if (myAuthUid.isNullOrEmpty()) {
            SystemLogger.w("LocationRepository", "Cloud auth not ready on startListening; local mesh listeners active.")
            return
        }

        FirebaseComm.resolveHardwareIdentity(myAuthUid) { agentId ->
            val finalId = agentId ?: "LEGACY_NODE"
            
            updateTargetListeners()
            
            val groupRef = FirebaseComm.devicesRef.child(finalId).child("profile").child("active_group")
            val groupListener = object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val rawGroup = snap.getValue(String::class.java) ?: ""
                    val myGroup = if (rawGroup.isNotEmpty() && rawGroup != "DEFAULT_GROUP") {
                        rawGroup
                    } else {
                        com.jdroidx.jfindme.utils.RegistryManager.getEffectiveActiveGroup(context)
                    }
                    com.jdroidx.jfindme.utils.RegistryManager.setActiveGroup(context, myGroup)
                    
                    val relRef = FirebaseComm.relationshipsRef.child(myGroup)
                    val relListener = object : ValueEventListener {
                        override fun onDataChange(relSnap: DataSnapshot) {
                            val localDb = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
                            // [Variables] csv: StringBuilder, unifiedPairs: DataSnapshot, directPairs: DataSnapshot, approvals: DataSnapshot, membersSnap: DataSnapshot, devicesSnap: DataSnapshot, cleanPairKey: String, cleanTargetId: String, cleanMId: String, cleanDId: String | [Constants] None | [Dependencies] FirebaseComm, RegistryManager, SharedPreferences, DeviceIdentityHelper | [Integration Points] linked_devices_csv, updateTargetListeners
                            val csv = StringBuilder(finalId).append(",")
                            
                            // 1. Unified `devices/$finalId/paired_with`
                            val unifiedPairs = relSnap.child("devices").child(finalId).child("paired_with")
                            unifiedPairs.children.forEach { pair ->
                                val pairKey = pair.key
                                val cleanPairKey = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(pairKey)
                                if (cleanPairKey.isNotEmpty() && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanPairKey) && !localDb.isDeviceDeleted(cleanPairKey) && !localDb.isDeviceDeleted(pairKey ?: "") && pair.getValue(Boolean::class.java) == true && pair.key != null) {
                                    if (!csv.contains("${pair.key},")) {
                                        csv.append(pair.key).append(",")
                                    }
                                }
                            }

                            // 2. Legacy `direct_pairs/$finalId` for backwards compatibility
                            val directPairs = relSnap.child("direct_pairs").child(finalId)
                            directPairs.children.forEach { pair ->
                                val pairKey = pair.key
                                val cleanPairKey = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(pairKey)
                                if (cleanPairKey.isNotEmpty() && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanPairKey) && !localDb.isDeviceDeleted(cleanPairKey) && !localDb.isDeviceDeleted(pairKey ?: "") && pair.getValue(Boolean::class.java) == true && pair.key != null) {
                                    if (!csv.contains("${pair.key},")) {
                                        csv.append(pair.key).append(",")
                                    }
                                }
                            }
                            
                            val approvals = relSnap.child("sharing_approvals")
                            approvals.children.forEach { targetSnap ->
                                val targetId = targetSnap.key
                                val cleanTargetId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(targetId)
                                if (!targetId.isNullOrEmpty() && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanTargetId) && !localDb.isDeviceDeleted(cleanTargetId) && !localDb.isDeviceDeleted(targetId) && targetSnap.child(finalId).getValue(Boolean::class.java) == true) {
                                    if (!csv.contains("$targetId,")) {
                                        csv.append(targetId).append(",")
                                    }
                                }
                            }

                            // 3. 🚀 CIRCLE MEMBERS & DEVICES SSOT: Include all active circle members and devices (filtering decommissioned and unlinked nodes)
                            val membersSnap = relSnap.child("members")
                            membersSnap.children.forEach { mSnap ->
                                val mId = mSnap.key
                                val cleanMId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(mId)
                                if (!mId.isNullOrEmpty() && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanMId) && !localDb.isDeviceDeleted(cleanMId) && !localDb.isDeviceDeleted(mId) && !csv.contains("$mId,")) {
                                    csv.append(mId).append(",")
                                }
                            }
                            val devicesSnap = relSnap.child("devices")
                            devicesSnap.children.forEach { dSnap ->
                                val dId = dSnap.key
                                val cleanDId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(dId)
                                if (!dId.isNullOrEmpty() && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanDId) && !localDb.isDeviceDeleted(cleanDId) && !localDb.isDeviceDeleted(dId) && !csv.contains("$dId,")) {
                                    csv.append(dId).append(",")
                                }
                            }
                            
                            prefs.edit().putString("linked_devices_csv", csv.toString()).apply()
                            updateTargetListeners()
                        }
                        override fun onCancelled(err: DatabaseError) {
                            SystemLogger.e("LocationRepository", "Active Group Sync Failed: ${err.message}")
                        }
                    }
                    relRef.addValueEventListener(relListener)
                    activeListeners[relRef] = relListener
                }
                override fun onCancelled(err: DatabaseError) {
                    SystemLogger.e("LocationRepository", "Group Lookup Failed: ${err.message}")
                }
            }
            groupRef.addValueEventListener(groupListener)
            activeListeners[groupRef] = groupListener
                
            val friendRef = FirebaseComm.devicesRef.child(finalId).child("profile").child("friend_matrix")
            val friendListener = object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val localDb = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
                    val csv = StringBuilder()
                    snap.children.forEach { childSnap -> 
                        val fId = childSnap.key
                        val cleanFId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(fId)
                        if (!fId.isNullOrEmpty() && !com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanFId) && !localDb.isDeviceDeleted(cleanFId) && !localDb.isDeviceDeleted(fId)) {
                            csv.append(fId).append(",") 
                        }
                    }
                    prefs.edit().putString("friend_matrix_csv", csv.toString()).apply()
                    updateTargetListeners()
                }
                override fun onCancelled(err: DatabaseError) {
                    SystemLogger.e("LocationRepository", "Friend Matrix Sync Failed: ${err.message}")
                }
            }
            friendRef.addValueEventListener(friendListener)
            activeListeners[friendRef] = friendListener

            // 🚀 RELATIONSHIP MESH LISTENER: Listen to self device's direct relationships node
            // When another device pairs Device B to this device or updates roles, sync directly to local SQLite
            val selfRelRef = FirebaseComm.devicesRef.child(finalId).child("relationships")
            val selfRelListener = object : ValueEventListener {
                // [Variables] selfRelSnap, peerId, cleanPeerId, role, group, localDb, changed | [Constants] None | [Dependencies] DatabaseHelper, RegistryManager, SystemLogger, DeviceIdentityHelper | [Integration Points] LocationRepository, MapActivity, FamilyFriendsActivity
                override fun onDataChange(selfRelSnap: DataSnapshot) {
                    try {
                        val localDb = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
                        var changed = false
                        for (relChild in selfRelSnap.children) {
                            val peerId = relChild.key ?: continue
                            if (peerId.isBlank()) continue
                            val cleanPeerId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.toCanonicalAgentId(peerId)
                            if (com.jdroidx.jfindme.utils.RegistryManager.isAgentIdDecommissioned(context, cleanPeerId) || localDb.isDeviceDeleted(cleanPeerId) || localDb.isDeviceDeleted(peerId)) continue
                            val role = relChild.child("role").value?.toString() ?: "Member"
                            val group = relChild.child("group").value?.toString() ?: ("GRP_" + finalId)
                            val isFriend = role.contains("Friend", ignoreCase = true) || role.contains("Peer", ignoreCase = true) || role.contains("Colleague", ignoreCase = true)
                            val famRole = if (isFriend) "None" else role
                            val frRole = if (isFriend) role else "None"

                            localDb.updateCitizenRole(peerId, famRole, frRole, isFriend, group)
                            // [Variables] rName | [Constants] None | [Dependencies] DevicePresentationResolver | [Integration Points] LocationRepository self relationships sync
                            val rName = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveDisplayName(context, peerId)
                            localDb.saveRosterNode(peerId, rName, role, "LIVE", false, isFriend)
                            changed = true
                        }
                        if (changed) {
                            updateTargetListeners()
                        }
                    } catch (e: Exception) {
                        SystemLogger.e("LocationRepository", "Self relationships sync failed", e)
                    }
                }
                override fun onCancelled(err: DatabaseError) {
                    SystemLogger.e("LocationRepository", "Self relationships listener cancelled: ${err.message}")
                }
            }
            selfRelRef.addValueEventListener(selfRelListener)
            activeListeners[selfRelRef] = selfRelListener
        }
    }

    fun stopListening() {
        activeListeners.forEach { (ref, listener) ->
            ref.removeEventListener(listener)
        }
        activeListeners.clear()
        userLocationListeners.clear()
        latestLocations.clear()
    }

    fun updateMyLocation(userLocation: UserLocation) {
        val currentTime = System.currentTimeMillis()
        val currentLocation = android.location.Location("").apply {
            latitude = userLocation.latitude
            longitude = userLocation.longitude
        }

        val timeElapsed = currentTime - lastGeocodeTime
        val distanceMoved = lastGeocodedLocation?.distanceTo(currentLocation) ?: Float.MAX_VALUE

        if (timeElapsed < 60000 && distanceMoved < 50f) {
            finalizeLocationPush(userLocation, cachedHumanAddress)
            return
        }

        lastGeocodeTime = currentTime
        lastGeocodedLocation = currentLocation

        val geocoder = Geocoder(context, Locale.getDefault())
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            var humanAddress: String? = cachedHumanAddress
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(userLocation.latitude, userLocation.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    humanAddress = addresses[0].getAddressLine(0)
                    cachedHumanAddress = humanAddress
                } else if (humanAddress.isNullOrBlank()) {
                    val lat = userLocation.latitude
                    val lng = userLocation.longitude
                    humanAddress = String.format(Locale.US, "%.5f° %s, %.5f° %s", kotlin.math.abs(lat), if (lat >= 0) "N" else "S", kotlin.math.abs(lng), if (lng >= 0) "E" else "W")
                }
            } catch (e: Exception) {
                SystemLogger.e("LocationRepository", "Forensic Geocoding Blocked: ${e.message}")
                if (humanAddress.isNullOrBlank()) {
                    val lat = userLocation.latitude
                    val lng = userLocation.longitude
                    humanAddress = String.format(Locale.US, "%.5f° %s, %.5f° %s", kotlin.math.abs(lat), if (lat >= 0) "N" else "S", kotlin.math.abs(lng), if (lng >= 0) "E" else "W")
                }
            }
            
            finalizeLocationPush(userLocation, humanAddress)
        }
    }

    // [Variables] enrichedLocation, rId | [Constants] None | [Dependencies] offlineSyncManager, processIncomingLocation | [Integration Points] Immediate Host Marker Loopback
    private fun finalizeLocationPush(userLocation: UserLocation, finalAddress: String?) {
        val enrichedLocation = userLocation.copy(address = finalAddress)
        // 🔋 Spark Quota & Cellular Radio Sentinel: Single authoritative cloud upload with spatial dead-reckoning & heartbeat throttle
        offlineSyncManager.syncLocation(enrichedLocation, true)
        // 🚀 CRITICAL FIX: Loopback Self GNSS fix immediately into local in-memory repository (Zero radio cost)
        val rId = enrichedLocation.getResolvedUserId()
        if (rId.isNotEmpty()) {
            processIncomingLocation(rId, enrichedLocation)
        }
    }

    fun updateUserLocation(userLocation: UserLocation) {
        val rId = userLocation.getResolvedUserId()
        if (rId.isNotEmpty()) {
            val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
            val epochId = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                .getString("install_epoch_id", "") ?: ""
            if (authUid.isNotEmpty()) {
                FirebaseComm.syncPartitionedLocationTrace(authUid, rId, epochId, userLocation)
            } else {
                SystemLogger.d("LocationRepository", "Standby fix: Cached in secure offline vault while awaiting authentication.")
            }
        }
    }

    fun fetchHistory(userId: String, startTs: Long, endTs: Long, callback: (List<UserLocation>) -> Unit) {
        val repo = com.jdroidx.jfindme.data.repository.DeviceRepository.getInstance(context)
        val localPoints = repo.getRawHistoryPoints(userId, startTs, endTs)

        if (!FirebaseComm.isAuthenticated()) {
            // 🛡️ Offline / Zero-Trust Sentinel: Provide instant local SQLite ledger history (Latest first)
            val sortedLocal = localPoints.sortedByDescending { it.getLongTimestamp() }
            Handler(Looper.getMainLooper()).post {
                callback?.invoke(sortedLocal)
            }
            return
        }

        val startKey = FirebaseComm.getInvertedForensicKey(endTs)
        val endKey = FirebaseComm.getInvertedForensicKey(startTs)

        FirebaseComm.getDeepHistoryRef(userId)
            .orderByKey()
            .startAt(startKey)
            .endAt(endKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val points = mutableListOf<UserLocation>()
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        snapshot.children.forEach { child ->
                            child.getValue(UserLocation::class.java)?.let { point ->
                                val effectiveTs = if (point.getLongTimestamp() <= 0L || point.timestamp == "" || point.timestamp == null) {
                                    val childKey = child.key ?: ""
                                    FirebaseComm.parseTimestampFromInvertedKey(childKey)
                                } else {
                                    point.getResolvedTimestamp()
                                }
                                point.timestamp = effectiveTs
                                val childSos = child.child("sos").getValue(Boolean::class.java) 
                                    ?: child.child("isSosActive").getValue(Boolean::class.java) 
                                    ?: point.isSosActive
                                val childBattery = FirebaseComm.safeInt(child.child("battery"))
                                    ?: FirebaseComm.safeInt(child.child("batteryLevel"))
                                    ?: point.batteryLevel
                                point.isSosActive = childSos
                                point.batteryLevel = childBattery
                                points.add(point)
                                
                                repo.saveHistoryPoint(
                                    userId,
                                    point.getResolvedDeviceId(),
                                    point.latitude,
                                    point.longitude,
                                    point.speed,
                                    point.batteryLevel.toInt(),
                                    point.isSosActive,
                                    point.address,
                                    effectiveTs,
                                    point.accuracy,
                                    point.bearing,
                                    point.altitude,
                                    point.motion_state
                                )
                            }
                        }
                        val finalResult = (if (points.isNotEmpty()) points else localPoints).sortedByDescending { it.getLongTimestamp() }
                        Handler(Looper.getMainLooper()).post {
                            callback?.invoke(finalResult)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // 🛡️ Closed-Loop Fault Tolerance: Return local SQLite points on cloud abort (Latest first)
                    val sortedLocal = localPoints.sortedByDescending { it.getLongTimestamp() }
                    Handler(Looper.getMainLooper()).post {
                        callback?.invoke(sortedLocal)
                    }
                }
            })
    }

    companion object {
        @Volatile
        private var INSTANCE: LocationRepository? = null

        @JvmStatic
        fun getInstance(context: Context): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
