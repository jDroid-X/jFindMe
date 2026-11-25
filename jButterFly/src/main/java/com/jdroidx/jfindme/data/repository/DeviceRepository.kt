package com.jdroidx.jfindme.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.model.LiveDeviceModel
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [Variables]: context, db, ioScope, cachedVitals
 * [Constants]: TAG
 * [Dependencies]: DatabaseHelper, FirebaseComm, DeviceIdentityHelper, LiveDeviceModel
 * [Integration Points]: MapActivity, PersonDetailsActivity, NetworkGatewayActivity, SettingsActivity
 *
 * 🚀 DeviceRepository (OOPS-based MVC SSOT)
 * Centralizes all hardware, vitals, policy, and device unlinking/purging operations.
 * Eliminates direct SQLite and Firebase calls scattered across Activities.
 */
class DeviceRepository private constructor(private val context: Context) {

    private val db = DatabaseHelper.getInstance(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "DeviceRepository"

    companion object {
        const val TABLE_ALERTS = DatabaseHelper.TABLE_ALERTS
        const val PROFILE_TABLE = DatabaseHelper.PROFILE_TABLE

        @Volatile
        private var instance: DeviceRepository? = null

        @JvmStatic
        fun getInstance(context: Context): DeviceRepository {
            return instance ?: synchronized(this) {
                instance ?: DeviceRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    val dbHelper: DatabaseHelper get() = db

    /**
     * Resolves human-readable device name from local SQLite SSOT.
     */
    fun getDeviceCentricName(deviceId: String): String {
        return db.getDeviceCentricName(deviceId)
    }

    /**
     * Resolves avatar identifier/path from local SQLite SSOT.
     */
    fun getDeviceCentricAvatar(deviceId: String): String {
        return db.getDeviceCentricAvatar(deviceId)
    }

    /**
     * Retrieves telemetry vitals (battery, charging, temperature, memory) from SQLite.
     * Ordered by latest recorded vital.
     */
    fun getDeviceVitals(hardwareId: String): Map<String, Any>? {
        return db.getDeviceVitals(hardwareId)
    }

    /**
     * Resolves active device policy (e.g., stealth mode, location duty cycle).
     */
    fun getDevicePolicy(key: String, deviceId: String = ""): String? {
        return db.getDevicePolicy(key, deviceId)
    }

    /**
     * Unlinks a target device from the tracking network.
     * Performs atomic updates across SQLite and pushes synchronization signals to Firebase.
     */
    fun unlinkDevice(targetAgentId: String, currentGroupId: String, onComplete: (Boolean) -> Unit) {
        if (targetAgentId.isBlank()) {
            onComplete(false)
            return
        }

        ioScope.launch {
            try {
                // 1. Remove from local SQLite associations
                val writableDb = db.writableDatabase
                writableDb.delete(
                    com.jdroidx.jfindme.data.local.tables.GroupMemberTable.TABLE_NAME,
                    "${com.jdroidx.jfindme.data.local.tables.GroupMemberTable.COL_IDENTITY_ID} = ? OR ${com.jdroidx.jfindme.data.local.tables.GroupMemberTable.COL_AGENT_ID} = ?",
                    arrayOf(targetAgentId, targetAgentId)
                )

                // 2. Broadcast refresh signal to active circle via Firebase
                if (currentGroupId.isNotBlank()) {
                    FirebaseComm.relationshipsRef
                        .child(currentGroupId)
                        .child("signals")
                        .child("refresh_roster")
                        .setValue(System.currentTimeMillis())
                }

                SystemLogger.i(TAG, "Successfully unlinked device: $targetAgentId from group: $currentGroupId")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to unlink device: ${e.message}")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun interface PurgeCallback {
        fun onPurgeComplete(success: Boolean)
    }

    /**
     * Master Administrative Purge: Atomically wipes corrupted or orphaned nodes across all 22 tables.
     */
    fun purgeDeviceRecords(targetAgentId: String, callback: PurgeCallback) {
        ioScope.launch {
            try {
                val success = db.purgeDeviceRecordsMaster(targetAgentId)
                SystemLogger.i(TAG, "Master force purge execution for $targetAgentId: success=$success")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    callback.onPurgeComplete(success)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error purging device records: ${e.message}")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    callback.onPurgeComplete(false)
                }
            }
        }
    }

    /**
     * 📜 Returns device sharing audit history.
     */
    fun getDeviceSharingHistory(): List<Map<String, Any>> = db.getDeviceSharingHistory()

    /**
     * Resolves actionable candidate fleet devices for pairing/sharing workflows.
     * Combines live locations, offline roster, and paired nodes, filtering out excluded IDs.
     */
    // [Variables] excludedIds: Set<String>, liveLocations: List<UserLocation>, filterCoParentsOnly: Boolean, candidates: MutableList<android.util.Pair<String, String>>, seen: MutableSet<String> | [Constants] None | [Dependencies] android.util.Pair, DatabaseHelper, UserLocation | [Integration Points] DeviceRepository, MapActivity, FamilyFriendsActivity
    @JvmOverloads
    fun resolveFleetCandidates(
        excludedId: String,
        filterCoParentsOnly: Boolean = false
    ): List<android.util.Pair<String, String>> = resolveFleetCandidates(setOf(excludedId), emptyList(), filterCoParentsOnly)

    @JvmOverloads
    fun resolveFleetCandidates(
        excludedIds: Set<String>,
        liveLocations: List<com.jdroidx.jfindme.data.model.UserLocation> = emptyList(),
        filterCoParentsOnly: Boolean = false
    ): List<android.util.Pair<String, String>> {
        val candidates = mutableListOf<android.util.Pair<String, String>>()
        val seen = excludedIds.map { it.uppercase().trim() }.toMutableSet()

        // 1. Live Locations
        for (ul in liveLocations) {
            val uid = ul.getResolvedUserId()
            if (uid.isNotBlank() && !seen.contains(uid.uppercase())) {
                if (filterCoParentsOnly) {
                    val r = (ul.role_family ?: "").lowercase()
                    if (!r.contains("guardian") && !r.contains("parent") && !r.contains("spouse") && !r.contains("admin")) {
                        continue
                    }
                }
                seen.add(uid.uppercase())
                var name = db.getDeviceCentricName(uid)
                if (name.isBlank()) name = ul.userName
                if (name.isBlank()) name = "Device $uid"
                candidates.add(android.util.Pair(uid, name))
            }
        }

        // 2. Offline Roster / Known Nodes
        val offline = db.getOfflineRoster()
        for (node in offline) {
            val uid = node["uid_1"] as? String ?: node["connected_id"] as? String
            if (!uid.isNullOrBlank() && !seen.contains(uid.uppercase())) {
                if (filterCoParentsOnly) {
                    val role = (node["role_type"] as? String ?: "").lowercase()
                    if (!role.contains("guardian") && !role.contains("parent") && !role.contains("spouse") && !role.contains("admin") && !role.contains("member")) {
                        continue
                    }
                }
                seen.add(uid.uppercase())
                // [Variables] name, uid | [Constants] None | [Dependencies] DevicePresentationResolver | [Integration Points] DeviceRepository candidate device names
                var name = node["name_alias"] as? String
                if (name.isNullOrBlank()) name = node["fullName"] as? String
                if (name.isNullOrBlank()) name = node["device_name"] as? String
                name = com.jdroidx.jfindme.utils.DevicePresentationResolver.resolveDisplayName(context, uid, name)
                candidates.add(android.util.Pair(uid, name))
            }
        }

        // 3. Paired Devices (if not filtering co-parents only)
        if (!filterCoParentsOnly) {
            val paired = db.getAllPairedDeviceIds()
            for (pid in paired) {
                if (pid.isNotBlank() && !seen.contains(pid.uppercase())) {
                    seen.add(pid.uppercase())
                    var name = db.getDeviceCentricName(pid)
                    if (name.isBlank()) name = "Paired Device $pid"
                    candidates.add(android.util.Pair(pid, name))
                }
            }
        }

        return candidates
    }

    fun getAllPairedDeviceIds(): Set<String> = db.getAllPairedDeviceIds()

    fun getConnectedDeviceIds(agentId: String): Set<String> = db.getConnectedDeviceIds(agentId)

    fun resolveInternalId(agentId: String): Long = db.resolveInternalId(agentId)

    fun inspectSubsetDependencies(agentId: String): List<DatabaseHelper.DeviceSubsetDependency> = db.inspectSubsetDependencies(agentId)

    fun getOfflineRoster(): List<Map<String, Any>> = db.getOfflineRoster()

    fun isChildOrWard(uid: String): Boolean {
        val role = db.getRoleForDevice(uid) ?: return false
        return role.contains("Child", ignoreCase = true) ||
               role.contains("Daughter", ignoreCase = true) ||
               role.contains("Son", ignoreCase = true) ||
               role.contains("Ward", ignoreCase = true) ||
               role.contains("Dependent", ignoreCase = true) ||
               role.contains("Kid", ignoreCase = true)
    }

    fun getRoleForDevice(uid: String): String = db.getRoleForDevice(uid) ?: "Family Member"

    fun getLatestHistoricalLocation(uid: String): Map<String, Any>? = db.getLatestHistoricalLocation(uid)

    fun getActiveButterflyPoints(): List<com.jdroidx.jfindme.data.model.ButterflyPoint> = db.getActiveButterflyPoints()

    // [Variables] limit | [Constants] None | [Dependencies] DatabaseHelper, ButterflyPoint | [Integration Points] ButterflyPointDialog.java
    fun getArchivedButterflyPoints(limit: Int = 20): List<com.jdroidx.jfindme.data.model.ButterflyPoint> = db.getArchivedButterflyPoints(limit)

    fun getButterflyPoint(ptId: String): com.jdroidx.jfindme.data.model.ButterflyPoint? = db.getButterflyPoint(ptId)

    fun updateDeviceAvatar(deviceId: String, avatarId: String) {
        val currentName = db.getDeviceCentricName(deviceId)
        db.updateDeviceIdentityAndAvatar(deviceId, currentName, avatarId)
    }

    fun updateDeviceIdentityAndAvatar(deviceId: String, name: String?, avatarId: String?) {
        db.updateDeviceIdentityAndAvatar(deviceId, name, avatarId)
    }

    fun upsertDeviceSharing(shareId: String, sharerId: String, recipientId: String, senderId: String, role: String, permissionsMask: Int) {
        db.upsertDeviceSharing(shareId, sharerId, recipientId, senderId, role, permissionsMask)
    }

    @JvmOverloads
    fun updateDeviceVitals(
        hardwareId: String,
        batteryLevel: Int,
        lastSeenTs: Long,
        isCharging: Boolean = false,
        motionState: String? = null
    ) {
        db.updateDeviceVitalsTelemetry(hardwareId, batteryLevel, lastSeenTs, isCharging, motionState)
    }

    fun logCoGuardianPairing(coParentId: String, targetUid: String, selfAgentId: String) {
        try {
            com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairing(
                db.writableDatabase,
                "PAIR_${coParentId}_$targetUid",
                coParentId,
                targetUid,
                "PAIRED"
            )
            com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairingWithRoles(
                db.writableDatabase,
                "PAIR_${selfAgentId}_$coParentId",
                selfAgentId,
                coParentId,
                "Guardian",
                "Guardian",
                null,
                "PAIRED"
            )
            db.appendValidLinkedAgent(coParentId, targetUid)
            db.appendValidLinkedAgent(targetUid, coParentId)
            db.appendValidLinkedAgent(selfAgentId, coParentId)
            db.appendValidLinkedAgent(selfAgentId, targetUid)
        } catch (ex: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.e("DeviceRepo", "logCoGuardianPairing warning: ${ex.message}")
        }
    }

    @JvmOverloads fun isDeviceDeleted(deviceId: String, groupId: String? = null): Boolean = db.isDeviceDeleted(deviceId, groupId)

    fun getLocalDossier(uid: String): Map<String, String> = db.getLocalDossier(uid)

    fun resolveAgentIdLocally(uid: String): String? = db.resolveAgentIdLocally(uid)

    fun resolveForensicAgentId(uid2: String?): String = db.resolveForensicAgentId(uid2 ?: "")

    fun getPairingHistoryAndLocation(childId: String): Map<String, String>? = db.getPairingHistoryAndLocation(childId)

    @JvmOverloads
    fun updateCitizenRole(
        deviceId: String,
        roleFamily: String,
        roleFriend: String,
        isFriend: Boolean,
        activeGroupId: String? = null
    ) {
        // [Variables] deviceId, roleFamily, roleFriend, isFriend, activeGroupId | [Constants] "DEFAULT_GROUP" | [Dependencies] DatabaseHelper | [Integration Points] updateCitizenRole
        db.updateCitizenRole(deviceId, roleFamily, roleFriend, isFriend, activeGroupId ?: "DEFAULT_GROUP")
    }

    fun isDevicePendingUnlink(deviceId: String): Boolean = db.isDevicePendingUnlink(deviceId)

    @JvmOverloads
    fun saveRosterNode(
        deviceId: String,
        name: String?,
        role: String?,
        status: String?,
        isAdmin: Boolean,
        isFriend: Boolean,
        ownerName: String? = null,
        uid2: String? = null,
        connectedDeviceId: String? = null,
        qrCode: String? = null,
        avatarId: String? = null,
        activeGroupId: String? = null,
        permissionsMask: String? = null,
        notifyPeers: Boolean = false
    ) {
        db.saveRosterNode(
            deviceId,
            name ?: "",
            role ?: "Member",
            status ?: "ONLINE",
            isAdmin,
            isFriend,
            ownerName,
            uid2,
            connectedDeviceId,
            qrCode,
            avatarId,
            activeGroupId,
            permissionsMask,
            notifyPeers
        )
    }

    fun getOfflineRoster(activeGroupId: String?): List<Map<String, Any>> = db.getOfflineRoster(activeGroupId)

    fun pruneZombieNodes(currentCanonicalId: String, phoneOrModel: String?): Int =
        db.pruneZombieNodes(currentCanonicalId, phoneOrModel)

    fun getLatestBatteryLevel(hardwareId: String): Int =
        db.getLatestBatteryLevel(hardwareId)

    @JvmOverloads
    fun upsertDeviceVitals(
        hardwareId: String,
        battery: Int,
        isCharging: Boolean = false,
        networkType: String = "UNKNOWN",
        signalDbm: Int = 0,
        isMockGps: Boolean = false,
        isRooted: Boolean = false,
        timestamp: Long = System.currentTimeMillis()
    ) {
        db.upsertDeviceVitals(hardwareId, battery, isCharging, networkType, signalDbm, isMockGps, isRooted, timestamp)
    }

    fun getAssignedSafeZoneForDevice(deviceId: String): Map<String, String>? = db.getAssignedSafeZoneForDevice(deviceId)

    fun getActiveSafeZones(): List<com.jdroidx.jfindme.data.model.SafeZoneModel> = db.getActiveSafeZones()

    fun getSafeZoneById(zoneId: String): com.jdroidx.jfindme.data.model.SafeZoneModel? = db.getSafeZoneById(zoneId)

    fun saveGeofenceEvent(uid1: String, zoneId: String, eventType: String) {
        db.saveGeofenceEvent(uid1, zoneId, eventType)
    }

    fun commitAnalyticalDossier(uid: String, data: Map<String, String>, name: String? = null) {
        db.commitAnalyticalDossier(uid, data, name)
    }

    fun saveSafeZone(values: android.content.ContentValues) = db.saveSafeZone(values)

    fun appendValidLinkedAgent(agentA: String, agentB: String) = db.appendValidLinkedAgent(agentA, agentB)

    fun logPairingWithRoles(
        pairId: String,
        devA: String,
        devB: String,
        roleA: String,
        roleB: String,
        permissions: String?,
        status: String
    ) {
        com.jdroidx.jfindme.data.local.tables.DevicePairingTable.logPairingWithRoles(
            db.writableDatabase, pairId, devA, devB, roleA, roleB, permissions, status
        )
    }

    fun saveButterflyPoint(
        pointId: String,
        creatorDeviceId: String,
        creatorIdentityId: Long?,
        pointName: String,
        address: String?,
        lat: Double,
        lng: Double,
        geohash: String?,
        rendezvousTimestamp: Long,
        expiryTimestamp: Long,
        arrivalRadiusMeters: Double = 50.0,
        notes: String? = null,
        iconType: String = "BUTTERFLY_STANDARD",
        isActive: Boolean = true,
        isArchived: Boolean = false
    ): Long {
        return com.jdroidx.jfindme.data.local.tables.ButterflyPointTable.save(
            db.writableDatabase, pointId, creatorDeviceId, creatorIdentityId,
            pointName, address, lat, lng, geohash, rendezvousTimestamp,
            expiryTimestamp, arrivalRadiusMeters, notes, iconType, isActive, isArchived
        )
    }

    fun assignButterflyPointDevice(pointId: String, deviceId: String, initialStatus: String, identityId: Long? = null): Long {
        return com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.assignDevice(
            db.writableDatabase, pointId, deviceId, identityId, initialStatus
        )
    }

    fun getButterflyDeviceStatus(pointId: String, deviceId: String): String? {
        return com.jdroidx.jfindme.data.local.tables.ButterflyPointDeviceMapTable.getDeviceStatus(
            db.readableDatabase, pointId, deviceId
        )
    }

    fun archiveButterflyPoint(pointId: String) {
        com.jdroidx.jfindme.data.local.tables.ButterflyPointTable.archive(db.writableDatabase, pointId)
    }

    fun updateButterflyPointTimestamps(pointId: String, rendezvousTs: Long, expiryTs: Long) {
        db.writableDatabase.execSQL(
            "UPDATE ${com.jdroidx.jfindme.data.local.tables.ButterflyPointTable.TABLE_NAME} SET " +
            "${com.jdroidx.jfindme.data.local.tables.ButterflyPointTable.COL_RENDEZVOUS_TIMESTAMP} = ?, " +
            "${com.jdroidx.jfindme.data.local.tables.ButterflyPointTable.COL_EXPIRY_TIMESTAMP} = ? " +
            "WHERE ${com.jdroidx.jfindme.data.local.tables.ButterflyPointTable.COL_POINT_ID} = ?",
            arrayOf<Any>(rendezvousTs, expiryTs, pointId)
        )
    }

    fun getRawHistoryPoints(userId: String, startTs: Long, endTs: Long): List<com.jdroidx.jfindme.data.model.UserLocation> =
        db.getRawHistoryPoints(userId, startTs, endTs)

    @JvmOverloads
    fun saveHistoryPoint(
        uid1: String,
        deviceId: String,
        lat: Double,
        lng: Double,
        speed: Float,
        battery: Int,
        sos: Boolean,
        source: String?,
        ts: String,
        accuracy: Float = 0f,
        bearing: Float = 0f,
        altitude: Double = 0.0,
        motionState: String? = null
    ) {
        db.saveHistoryPoint(uid1, deviceId, lat, lng, speed, battery, sos, source, ts, accuracy, bearing, altitude, motionState)
    }

    @JvmOverloads
    fun updateDeviceVitalsTelemetry(
        hardwareId: String,
        batteryLevel: Int,
        lastSeenTs: Long,
        isCharging: Boolean = false,
        motionState: String? = null
    ) {
        db.updateDeviceVitalsTelemetry(hardwareId, batteryLevel, lastSeenTs, isCharging, motionState)
    }

    fun getStealthIntervalMins(): Long = db.getStealthIntervalMins()

    fun getDeviceGroupInfo(deviceId: String): Pair<String, String> = db.getDeviceGroupInfo(deviceId)

    fun logSimTransaction(
        slot: Int,
        mcc: String,
        mnc: String,
        lac: String,
        cid: String,
        lat: Double,
        lng: Double,
        event: String
    ) {
        db.logSimTransaction(slot, mcc, mnc, lac, cid, lat, lng, event)
    }

    fun logSecurityEvent(eventType: String, details: String, severity: String) =
        db.logSecurityEvent(eventType, details, severity)

    fun getPendingSyncCount(): Int = db.getPendingSyncCount()

    @JvmOverloads
    fun enqueuePendingSync(
        payloadType: String,
        targetUid: String,
        lat: Double,
        lng: Double,
        bearing: Float,
        speed: Float,
        battery: Int,
        isSos: Boolean,
        extraData: String? = null,
        accuracy: Float = 0f,
        altitude: Double = 0.0,
        motionState: String = "UNKNOWN"
    ): Long = db.enqueuePendingSync(payloadType, targetUid, lat, lng, bearing, speed, battery, isSos, extraData, accuracy, altitude, motionState)

    // [Variables] targetId, uid, dossier, subject, name, u1, uid2, handshake, auth, uid1, mask, agentId | [Constants] None | [Dependencies] DatabaseHelper | [Integration Points] PersonDetailsViewModel, MapViewModel, ProfileViewModel
    fun isDevicePaired(targetId: String?): Boolean = db.isDevicePaired(targetId)

    fun getMemberPermissionsMask(uid1: String): Int = db.getMemberPermissionsMask(uid1)

    fun updateMemberPermissionsMask(uid1: String, mask: Int): Boolean = db.updateMemberPermissionsMask(uid1, mask)

    fun getForensicDossierComplete(agentId: String): Map<String, Any> = db.getForensicDossierComplete(agentId)

    fun getIdentityProfileFallback(agentId: String): Map<String, String> = db.getIdentityProfileFallback(agentId)

    fun syncDeviceAnalytical(
        uid1: String,
        deviceId: String,
        model: String?,
        imei: String?,
        rooted: Boolean,
        fingerprint: String?,
        lat: Double = 0.0,
        lng: Double = 0.0,
        addr: String? = null,
        brand: String? = null,
        board: String? = null,
        hardware: String? = null,
        deviceName: String? = null,
        imei1: String? = null,
        imei2: String? = null,
        isDualSim: Boolean? = null
    ) {
        db.syncDeviceAnalytical(
            uid1, deviceId, model, imei, rooted, fingerprint, lat, lng, addr, brand, board, hardware, deviceName, imei1, imei2, isDualSim
        )
    }

    @JvmOverloads
    fun executeTieredHandshake(
        authUid: String,
        uid1: String,
        uid2: String,
        deviceId: String,
        profileData: Map<String, String>,
        orgData: Map<String, String>? = null,
        pairingData: Map<String, String>? = null,
        groupData: Map<String, String>? = null,
        locationData: Map<String, Any>? = null,
        safeZoneData: Map<String, Any>? = null,
        simData: Map<String, String>? = null,
        lat: Double = 0.0,
        lng: Double = 0.0,
        addr: String? = null,
        role: String = "ADMIN",
        epochId: String = ""
    ): Boolean {
        return db.executeTieredHandshake(
            authUid, uid1, uid2, deviceId, profileData, orgData, pairingData, groupData, locationData, safeZoneData, simData, lat, lng, addr, role, epochId
        )
    }

    fun saveSafeZoneDirect(
        zoneId: String,
        name: String,
        lat: Double,
        lng: Double,
        radius: Double,
        createdBy: Long? = null,
        assignedName: String? = null,
        alertsEnabled: Boolean = true,
        isZoneActive: Boolean = true,
        isArchived: Boolean = false,
        polygonPointsJson: String? = null,
        dwellThresholdMinutes: Int = 0
    ) {
        com.jdroidx.jfindme.data.local.tables.SafeZoneTable.save(
            db.writableDatabase, zoneId, name, lat, lng, radius, createdBy, assignedName, alertsEnabled, isZoneActive, isArchived, polygonPointsJson, dwellThresholdMinutes
        )
    }

    fun commitHandshakeDossier(
        uid: String,
        dossier: Map<String, String>,
        subject: String,
        name: String,
        u1: String,
        uid2: String,
        handshake: String,
        auth: String
    ) {
        db.commitHandshakeDossier(uid, dossier, subject, name, u1, uid2, handshake, auth)
    }

    // [Variables] initiator, source, durationSec, auditId, userAcknowledged, dbLevel, batteryLevel | [Constants] None | [Dependencies] SirenExecutionAuditTable | [Integration Points] AcousticSirenActivity
    fun recordSirenStart(initiator: String, source: String, durationSec: Int): Long =
        com.jdroidx.jfindme.data.local.tables.SirenExecutionAuditTable.recordSirenStart(
            db.writableDatabase, initiator, source, durationSec
        )

    fun resolveSiren(auditId: Long, userAcknowledged: Boolean, dbLevel: Double, batteryLevel: Int): Boolean =
        com.jdroidx.jfindme.data.local.tables.SirenExecutionAuditTable.resolveSiren(
            db.writableDatabase, auditId, userAcknowledged, dbLevel, batteryLevel
        )

    @JvmOverloads
    fun deleteRosterNode(uid1: String, groupId: String? = null): Boolean = db.deleteRosterNode(uid1, groupId)

    fun purgeDeviceRecordsMaster(targetAgentId: String): Boolean = db.purgeDeviceRecordsMaster(targetAgentId)

    fun parkRosterNode(uid1: String): Boolean = db.parkRosterNode(uid1)

    fun restoreRosterNode(uid1: String): Boolean = db.restoreRosterNode(uid1)

    @JvmOverloads
    fun updateRosterGrouping(uid1: String, newRole: String, isFriend: Boolean, groupId: String = "DEFAULT_GROUP") =
        db.updateRosterGrouping(uid1, newRole, isFriend, groupId)

    fun getRecordCount(table: String): Long = db.getRecordCount(table)

    fun getHistoryLogsFiltered(agentId: String, startTs: Long): List<Map<String, Any>> =
        db.getHistoryLogsFiltered(agentId, startTs)

    fun updateHistoryAddress(logId: String, address: String) = db.updateHistoryAddress(logId, address)

    fun getPagedData(tableName: String, limit: Int, offset: Int, query: String? = null): List<Map<String, String>> =
        db.getPagedData(tableName, limit, offset, query)

    fun saveRosterNode(v: android.content.ContentValues) = db.saveRosterNode(v)

    fun setStealthIntervalMins(mins: Long) = db.setStealthIntervalMins(mins)

    @JvmOverloads
    fun saveSysLog(level: String, tag: String, message: String, extra: String? = null, timestamp: Long? = null) =
        db.saveSysLog(level, tag, message, extra, timestamp)

    @JvmOverloads
    fun pruneExpiredData(retentionDays: Int = 0): Long = db.pruneExpiredData(retentionDays)

    fun pruneCacheAndTempFiles(): Long = db.pruneCacheAndTempFiles()

    fun getDatabaseStorageSizeMb(): Double = db.getDatabaseStorageSizeMb()

    fun isProfileComplete(uid1: String): Boolean = db.isProfileComplete(uid1)

    fun backfillCurrentDeviceHardwareSpecs(context: android.content.Context) =
        db.backfillCurrentDeviceHardwareSpecs(context)

    @JvmOverloads
    fun applyTtlDecay(maxInactiveDays: Int = 7): Int = db.applyTtlDecay(maxInactiveDays)

    fun updateSessionState(agentId: String, state: String): Int = db.updateSessionState(agentId, state)

    // [Variables] Geofence & SafeZone Facade Methods | [Constants] None | [Dependencies] DatabaseHelper | [Integration Points] GeofenceViewModel, GeofenceNotificationViewModel
    @JvmOverloads
    fun saveGeofenceLog(uid: String, avatar: String, zoneId: String, zoneName: String, status: String, lat: Double = 0.0, lng: Double = 0.0) =
        db.saveGeofenceLog(uid, avatar, zoneId, zoneName, status, lat, lng)

    @JvmOverloads
    fun getGeofenceLogs(targetAvatar: String? = null, zoneId: String? = null): List<Map<String, Any>> =
        db.getGeofenceLogs(targetAvatar, zoneId)

    fun clearGeofenceLogs() = db.clearGeofenceLogs()

    @JvmOverloads
    fun getOfflineSafeZones(includeArchived: Boolean = false): List<Map<String, Any>> =
        db.getOfflineSafeZones(includeArchived)

    fun saveOrgDossier(v: android.content.ContentValues) = db.saveOrgDossier(v)

    @JvmOverloads
    fun syncSafeZoneGroup(zoneId: String, nodes: String, start: Int, end: Int, days: String, flagA: Int = 1, flagB: Int = 1, addr: String? = null) =
        db.syncSafeZoneGroup(zoneId, nodes, start, end, days, flagA, flagB, addr)

    @JvmOverloads
    fun logDevicePairing(uid1: String, nameA: String, devA: String?, uid2: String, name2: String, dev2: String?, role2: String, groupId: String? = null) =
        db.logDevicePairing(uid1, nameA, devA, uid2, name2, dev2, role2, groupId)

    fun getReadableNow(): String = db.getReadableNow()

    fun logSimCard(slot: Int, iccid: String, operator: String, mcc: String, mnc: String, lac: String, cid: String, isPrimary: Boolean) =
        db.logSimCard(slot, iccid, operator, mcc, mnc, lac, cid, isPrimary)

    fun archiveSafeZone(zoneId: String): Boolean = db.archiveSafeZone(zoneId)

    fun restoreArchivedSafeZone(zoneId: String): Boolean = db.restoreArchivedSafeZone(zoneId)

    fun purgeArchivedSafeZone(zoneId: String): Boolean = db.purgeArchivedSafeZone(zoneId)

    fun restoreAllArchivedSafeZones(): Boolean = db.restoreAllArchivedSafeZones()

    fun purgeAllArchivedSafeZones(): Boolean = db.purgeAllArchivedSafeZones()

    fun getArchivedSafeZones(): List<Map<String, Any>> = db.getArchivedSafeZones()

    fun getGeofenceLogsByDeviceAndZone(zoneId: String, deviceName: String): List<Map<String, Any>> =
        db.getGeofenceLogsByDeviceAndZone(zoneId, deviceName)

    fun updateSafeZoneAlerts(zoneId: String, alertsEnabled: Boolean): Boolean =
        db.updateSafeZoneAlerts(zoneId, alertsEnabled)

    fun updateSafeZoneActive(zoneId: String, isActive: Boolean): Boolean =
        db.updateSafeZoneActive(zoneId, isActive)

    fun getActiveProfileAddress(): String = db.getActiveProfileAddress()

    // [Variables] Logs & Diagnostics Facade Methods | [Constants] None | [Dependencies] DatabaseHelper | [Integration Points] WatchDogViewModel, SystemUpdateViewModel, ForensicVaultExplorerActivity
    @JvmOverloads
    fun getSysLogs(level: String? = null, query: String? = null): List<Map<String, Any>> =
        db.getSysLogs(level, query)

    fun purgeSysLogs() = db.purgeSysLogs()

    fun clearAllSysLogs() = db.clearAllSysLogs()

    fun getTableColumns(tableName: String): List<String> = db.getTableColumns(tableName)

    fun getAllTables(): Array<String> = db.getAllTables()

    fun factoryReset() = db.factoryReset()

    fun truncateTable(name: String) = db.truncateTable(name)

    fun close() = db.close()

    fun deleteRecord(tableName: String, idCol: String, idVal: String): Boolean =
        db.deleteRecord(tableName, idCol, idVal)

    fun updateRecord(tableName: String, idCol: String, idVal: String, values: android.content.ContentValues): Boolean =
        db.updateRecord(tableName, idCol, idVal, values)
}
