/**
 * Auto-Login Architecture: Multi-Cloud Forensic Bridge
 * Waterfall Method: Secured Real-time Sync & Identity Parity
 */
package com.jdroidx.jfindme.data.remote

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.jdroidx.jfindme.data.model.UserLocation
import com.jdroidx.jfindme.utils.SystemLogger
import org.json.JSONException
import android.os.Handler
import android.os.Looper
import kotlin.math.pow

/**
 * Singleton to manage Firebase Database communication.
 * Standardizes access to tracking, user, and history nodes.
 */
object FirebaseComm {
    // --- L7 FORENSIC HUB NODES (UNIFIED RTDB SCHEMA) ---
    const val NODE_DEVICES = "devices"                  // Master Device Registry (Grouped by 8-Char DeviceId)
    const val NODE_LOOKUP_INDICES = "lookup_indices"   // Shallow O(1) Reverse Lookups (auth_to_device, active_monitors)
    const val NODE_PAIRING = "pairing_requests"         // Transient Handshake Buffer for QR Pairing
    const val NODE_HISTORY_ARCHIVE = "History"          // Time-Series Stream (Latest - 1 Historical Data)
    const val NODE_GEOFENCING = "Geofencing"            // 🛡️ Top-Level Geofencing Data Tree Structure

    // --- DEVICE SUB-GROUPS TAXONOMY ---
    const val SUB_IDENTITY = "Identity"          // Identity & Node index (Tier 1: Auth UID, Hardware Bridge)
    const val SUB_PROFILE = "profile"            // Single Source of Truth: Name, Avatar, Org, Note
    const val SUB_TELEMETRY = "telemetry"        // Hardware Vitals: Battery %, Charging, Mock GPS, OS
    const val SUB_DEVICE = "device"              // Hardware IDs & Static Fingerprints
    const val SUB_LIVE = "device/live"           // High-Frequency Live GPS Cursor (O(1) Shallow Read)
    const val SUB_HISTORY = "history"            // Latest-1 Point under Device / Time-Series Stream under /History
    const val SUB_SAFE_ZONES = "safe_zones"      // Assigned Geofences & Radius Thresholds
    const val SUB_RELATIONSHIPS = "relationships"// Device-to-Device Mesh Graph
    const val SUB_ALERTS = "alerts"              // Critical Security Alerts, SOS, Perimeter Breaches
    const val SUB_CHANGE_LOG = "Change_Log"      // 📝 Change Log Ledger: Profile, Pairing Devices, Family n Friends, Safehouse
    const val SUB_ACCESS = "Access"              // 🛡️ Device Access Permissions Matrix (true/false)
    const val NODE_VERSION_UPDATES = "Version_Update" // 📦 System Version Updates & Metadata

    // --- TELEMETRY & PRESENCE KEYS ---
    const val KEY_STATUS = "status"
    const val KEY_SESSION_START = "session_start_epoch"
    const val KEY_DURATION_SEC = "duration_sec"
    const val KEY_DURATION_FORMATTED = "duration_formatted"
    const val KEY_LAST_STATUS_CHANGE = "last_status_change"

    const val STATUS_ONLINE = "ONLINE"
    const val STATUS_OFFLINE = "OFFLINE"
    const val STATUS_IDLE = "IDLE"

    // --- CHANGE LOG ITEMS TAXONOMY ---
    const val LOG_ITEM_PROFILE = "Profile"
    const val LOG_ITEM_PAIRING = "Pairing_Devices"
    const val LOG_ITEM_FAMILY_FRIENDS = "Family_and_Friends"
    const val LOG_ITEM_SAFEHOUSE = "Safehouse"

    // REDUNDANT NODES (DECOMMISSIONED)
    const val NODE_TRACE_ARCHIVES = "History"
    const val NODE_ANALYTICS_EVENTS = "events"
    const val NODE_LOCATIONS = "locations"
    const val NODE_GROUPS = "groups"
    const val NODE_RELATIONSHIPS = "relationships"
    const val NODE_DIAGNOSTICS = "diagnostics"

    val database: FirebaseDatabase by lazy {
        val db = FirebaseDatabase.getInstance()
        try { db.setPersistenceEnabled(true) } catch (e: Exception) {}
        db
    }

    // 🧬 UNIFIED HUB ENDPOINTS (TOP-LEVEL NODES)
    @JvmStatic val devicesRef: DatabaseReference get() = database.reference.child(NODE_DEVICES)
    @JvmStatic val lookupIndicesRef: DatabaseReference get() = database.reference.child(NODE_LOOKUP_INDICES)
    @JvmStatic val pairingRef: DatabaseReference get() = database.reference.child(NODE_PAIRING)
    @JvmStatic val historyArchiveRef: DatabaseReference get() = database.reference.child(NODE_HISTORY_ARCHIVE)
    @JvmStatic val traceArchivesRef: DatabaseReference get() = historyArchiveRef
    @JvmStatic val geofencingRef: DatabaseReference get() = database.reference.child(NODE_GEOFENCING)

    // 🛡️ Safe Path Sanitizer for Firebase Realtime Database Keys (prohibits '.', '#', '$', '[', ']', '/')
    @JvmStatic fun sanitizePathKey(key: String?): String {
        if (key.isNullOrBlank()) return "node_${System.currentTimeMillis()}"
        return key.replace(Regex("[.#$\\[\\]/\\s:]"), "_").trim()
    }

    // 🛡️ GRANULAR GEOFENCING TREE ACCESSORS: Creator -> SafeZone -> Target Device -> Notifications (Date-Grouped)
    @JvmStatic fun getGeofencingCreatorRef(creatorId: String): DatabaseReference = geofencingRef.child(sanitizePathKey(creatorId))
    @JvmStatic fun getGeofencingSafeZonesRef(creatorId: String): DatabaseReference = geofencingRef.child(sanitizePathKey(creatorId)).child("safe_zones")
    @JvmStatic fun getGeofencingSafeZoneRef(creatorId: String, zoneId: String): DatabaseReference = geofencingRef.child(sanitizePathKey(creatorId)).child("safe_zones").child(sanitizePathKey(zoneId))
    @JvmStatic fun getGeofencingConnectedDevicesRef(creatorId: String, zoneId: String): DatabaseReference = getGeofencingSafeZoneRef(creatorId, zoneId).child("connected_devices")
    @JvmStatic fun getGeofencingTargetDeviceRef(creatorId: String, zoneId: String, targetDeviceId: String): DatabaseReference = getGeofencingConnectedDevicesRef(creatorId, zoneId).child(sanitizePathKey(targetDeviceId))
    @JvmStatic fun getGeofencingNotificationsRef(creatorId: String, zoneId: String, targetDeviceId: String, dateStr: String): DatabaseReference = getGeofencingTargetDeviceRef(creatorId, zoneId, targetDeviceId).child("notifications").child(sanitizePathKey(dateStr))

    // 🛡️ GRANULAR SUB-NODE ACCESSORS (Grouped by DeviceId)
    @JvmStatic fun getDeviceRef(agentId: String): DatabaseReference = devicesRef.child(agentId)
    @JvmStatic fun getProfileRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_PROFILE)
    @JvmStatic fun getIdentityRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_IDENTITY)
    @JvmStatic fun getTelemetryRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_TELEMETRY)
    @JvmStatic fun getLiveRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_LIVE)
    @JvmStatic fun getHistoryRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_HISTORY)
    @JvmStatic fun getDeepHistoryRef(agentId: String): DatabaseReference = historyArchiveRef.child(agentId).child(SUB_HISTORY)
    @JvmStatic fun getSafeZonesRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_SAFE_ZONES)
    @JvmStatic fun getRelationshipsRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_RELATIONSHIPS)
    @JvmStatic fun getAlertsRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_ALERTS)
    @JvmStatic fun getChangeLogRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_CHANGE_LOG)
    @JvmStatic fun getProfileChangeLogRef(agentId: String): DatabaseReference = getChangeLogRef(agentId).child(LOG_ITEM_PROFILE)
    @JvmStatic fun getPairingChangeLogRef(agentId: String): DatabaseReference = getChangeLogRef(agentId).child(LOG_ITEM_PAIRING)
    @JvmStatic fun getFamilyFriendsChangeLogRef(agentId: String): DatabaseReference = getChangeLogRef(agentId).child(LOG_ITEM_FAMILY_FRIENDS)
    @JvmStatic fun getSafehouseChangeLogRef(agentId: String): DatabaseReference = getChangeLogRef(agentId).child(LOG_ITEM_SAFEHOUSE)
    @JvmStatic fun getAccessRef(agentId: String): DatabaseReference = devicesRef.child(agentId).child(SUB_ACCESS)
    @JvmStatic val versionUpdatesRef: DatabaseReference get() = database.reference.child(NODE_VERSION_UPDATES)
    @JvmStatic fun getVersionUpdatesDeviceRef(agentId: String): DatabaseReference = versionUpdatesRef.child(agentId)
    @JvmStatic fun getVersionUpdateDetailRef(agentId: String, versionName: String): DatabaseReference = getVersionUpdatesDeviceRef(agentId).child(sanitizePathKey(versionName))

    /**
     * 📝 AUDIT LOGGER: Records forensic change logs in Firebase under /devices/{agentId}/Change_Log/{category}/{logId}
     * and mirrors into local SQLite ChangeLogTable.
     */
    @JvmStatic
    fun logChange(
        agentId: String,
        category: String,
        action: String,
        targetEntityId: String? = "",
        details: Map<String, Any?> = emptyMap(),
        changedBy: String = agentId
    ) {
        if (agentId.isBlank()) return
        val logId = "${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6).uppercase()}"
        val timestamp = getForensicTimestamp()
        val logPayload = mapOf(
            "log_id" to logId,
            "category" to category,
            "action" to action,
            "entity_id" to (targetEntityId ?: ""),
            "changed_by" to changedBy,
            "timestamp" to timestamp,
            "epoch_ms" to System.currentTimeMillis(),
            "metadata" to details
        )
        
        // 1. Write to Cloud Firebase RTDB
        getChangeLogRef(agentId).child(category).child(logId).setValue(logPayload)

        // 2. Mirror into Local SQLite DB
        val context = com.jdroidx.jfindme.JFindMeApplication.getContext()
        if (context != null) {
            try {
                val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                val internalId = db.resolveInternalId(agentId)
                if (internalId != -1L) {
                    com.jdroidx.jfindme.data.local.tables.ChangeLogTable.logChange(
                        db.writableDatabase,
                        internalId,
                        logId,
                        category,
                        action,
                        targetEntityId,
                        changedBy,
                        details
                    )
                }
            } catch (e: Exception) {
                SystemLogger.e("FirebaseComm", "Failed to mirror change log locally: ${e.message}")
            }
        }
    }

    /**
     * 🛡️ PERMISSIONS ACCESS AUDIT: Synchronizes the complete device hardware/software access matrix
     * under /devices/{agentId}/Access
     */
    @JvmStatic
    fun syncDeviceAccessPermissions(context: android.content.Context, agentId: String) {
        if (agentId.isBlank()) return
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
            val isDozeIgnored = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && powerManager != null) {
                powerManager.isIgnoringBatteryOptimizations(pkg)
            } else true

            val canInstall = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                pm.canRequestPackageInstalls()
            } else true

            fun checkPerm(perm: String): Boolean {
                return androidx.core.content.ContextCompat.checkSelfPermission(context, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            val accessMap = mutableMapOf<String, Any>()
            accessMap["location_fine"] = checkPerm(android.Manifest.permission.ACCESS_FINE_LOCATION)
            accessMap["location_coarse"] = checkPerm(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            accessMap["location_background"] = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                checkPerm(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else true

            accessMap["notifications"] = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                checkPerm(android.Manifest.permission.POST_NOTIFICATIONS)
            } else true

            accessMap["foreground_service"] = checkPerm(android.Manifest.permission.FOREGROUND_SERVICE)
            accessMap["camera"] = checkPerm(android.Manifest.permission.CAMERA)
            accessMap["storage"] = checkPerm(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            accessMap["request_install_packages"] = canInstall
            accessMap["ignoring_battery_optimizations"] = isDozeIgnored
            accessMap["wake_lock"] = checkPerm(android.Manifest.permission.WAKE_LOCK)
            accessMap["receive_boot_completed"] = checkPerm(android.Manifest.permission.RECEIVE_BOOT_COMPLETED)
            accessMap["network_state"] = checkPerm(android.Manifest.permission.ACCESS_NETWORK_STATE)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                accessMap["bluetooth_scan"] = checkPerm(android.Manifest.permission.BLUETOOTH_SCAN)
                accessMap["bluetooth_connect"] = checkPerm(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                accessMap["nearby_wifi_devices"] = checkPerm(android.Manifest.permission.NEARBY_WIFI_DEVICES)
            }

            accessMap["updated_at"] = getForensicTimestamp()
            accessMap["epoch_ms"] = System.currentTimeMillis()

            getAccessRef(agentId).setValue(accessMap)
            SystemLogger.i("FirebaseComm", "✅ Device Access matrix synchronized for Node: $agentId")
        } catch (e: Exception) {
            SystemLogger.e("FirebaseComm", "Failed to sync device access matrix: ${e.message}")
        }
    }

    /**
     * 📦 VERSION UPDATE METADATA: Synchronizes version update metadata under /Version_Update/{agentId}/{version_name}
     * and /Version_Update/latest
     */
    @JvmStatic
    fun syncVersionUpdateMetadata(
        context: android.content.Context,
        agentId: String,
        status: String = "INSTALLED",
        packageSizeBytes: Long = 106615515L,
        packageSizeFormatted: String = "101.67 MB"
    ) {
        if (agentId.isBlank()) return
        try {
            val versionName = com.jdroidx.jfindme.BuildConfig.VERSION_NAME
            val versionCode = com.jdroidx.jfindme.BuildConfig.VERSION_CODE
            val now = getForensicTimestamp()
            val epoch = System.currentTimeMillis()

            val driveLink = com.jdroidx.jfindme.utils.RegistryManager.getSystemUpdateLink(context)
            val githubLink = com.jdroidx.jfindme.utils.RegistryManager.getGithubUpdateLink(context)

            val metaMap = mapOf(
                "device_id" to agentId,
                "version_name" to versionName,
                "version_code" to versionCode,
                "database_schema" to "V${com.jdroidx.jfindme.data.local.DatabaseHelper.DATABASE_VERSION}",
                "package_size_bytes" to packageSizeBytes,
                "package_size_formatted" to packageSizeFormatted,
                "download_url_drive" to driveLink,
                "download_url_github" to githubLink,
                "os_environment" to "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
                "device_model" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                "last_updated_ts" to now,
                "epoch_ms" to epoch,
                "status" to status
            )

            // 1. Write under /Version_Update/{agentId}/{version_name}
            getVersionUpdateDetailRef(agentId, "V$versionName").setValue(metaMap)

            // 2. Record device's latest install under /Version_Update/{agentId}/latest
            getVersionUpdatesDeviceRef(agentId).child("latest").setValue(metaMap)

            // 3. Only write to master root if explicitly publishing
            if (status == "PUBLISHED_MASTER") {
                versionUpdatesRef.child("master_release").setValue(metaMap)
                versionUpdatesRef.child("latest").setValue(metaMap)
            }

            SystemLogger.i("FirebaseComm", "✅ Version Update metadata synchronized for Node $agentId: V$versionName")
        } catch (e: Exception) {
            SystemLogger.e("FirebaseComm", "Failed to sync version update metadata: ${e.message}")
        }
    }

    /**
     * 🔄 VERSION DIFFERENCE CHECK & SYNC: Checks for remote version differences and updates backend
     */
    @JvmStatic
    fun checkForVersionDifference(
        context: android.content.Context,
        agentId: String,
        callback: (hasNewVersion: Boolean, remoteVersion: String, remoteCode: Int, downloadUrl: String, size: String) -> Unit
    ) {
        val masterRef = versionUpdatesRef.child("master_release")
        masterRef.get().addOnCompleteListener { masterTask ->
            val snap = if (masterTask.isSuccessful && masterTask.result.exists()) {
                masterTask.result
            } else null

            if (snap != null && snap.exists()) {
                val remoteCode = snap.child("version_code").getValue(Long::class.java)?.toInt() ?: 62
                val remoteName = snap.child("version_name").getValue(String::class.java) ?: "1.0.0"
                val driveUrl = snap.child("download_url_drive").getValue(String::class.java) ?: com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_SYSTEM_UPDATE_LINK
                val githubUrl = snap.child("download_url_github").getValue(String::class.java) ?: com.jdroidx.jfindme.utils.RegistryManager.DEFAULT_GITHUB_UPDATE_LINK
                val size = snap.child("package_size_formatted").getValue(String::class.java) ?: "107.0 MB"
                val bestUrl = if (githubUrl.isNotEmpty()) githubUrl else driveUrl

                val currentCode = com.jdroidx.jfindme.BuildConfig.VERSION_CODE
                val hasNew = remoteCode > currentCode

                if (hasNew) {
                    com.jdroidx.jfindme.utils.RegistryManager.putString(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_UPDATE_AVAILABLE_TS, "V$remoteName (Build $remoteCode) [$size]")
                    com.jdroidx.jfindme.utils.RegistryManager.putBoolean(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_IS_UPDATED, false)
                } else {
                    com.jdroidx.jfindme.utils.RegistryManager.putString(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_UPDATE_AVAILABLE_TS, "None (Running Latest V$remoteName)")
                    com.jdroidx.jfindme.utils.RegistryManager.putBoolean(context, com.jdroidx.jfindme.utils.RegistryManager.KEY_IS_UPDATED, true)
                }

                callback(hasNew, remoteName, remoteCode, bestUrl, size)
            } else {
                // Fallback to Production Master Defaults
                val defaultCode = 62
                val defaultName = "1.0.0"
                val defaultUrl = "https://github.com/jDroid-X/jButterfly/raw/main/jbutterfly/app-universal-release.apk"
                val defaultSize = "107.0 MB"
                val currentCode = com.jdroidx.jfindme.BuildConfig.VERSION_CODE
                val hasNew = defaultCode > currentCode

                callback(hasNew, defaultName, defaultCode, defaultUrl, defaultSize)
            }
        }
    }
    
    // SAFETY REDIRECTS FOR BUILD BACKWARDS-COMPATIBILITY
    @JvmStatic val groupsRef: DatabaseReference get() = database.reference.child(NODE_GROUPS)
    @JvmStatic val eventsRef: DatabaseReference get() = database.reference.child(NODE_ANALYTICS_EVENTS)
    @JvmStatic val locationsRef: DatabaseReference get() = database.reference.child(NODE_LOCATIONS)
    @JvmStatic val relationshipsRef: DatabaseReference get() = database.reference.child(NODE_RELATIONSHIPS)
    @JvmStatic val diagRef: DatabaseReference get() = database.reference.child(NODE_DIAGNOSTICS)

    // 🧬 V67.2: Group logic decommissioned. Redirecting to safe stub for backwards compatibility.
    @JvmStatic fun getGroupRef(agentId: String, groupType: String): DatabaseReference = groupsRef.child(agentId).child(groupType)

    // 🧬 L7 HUB REDIRECTORS (Bridges legacy Java code to the new hierarchy)
    @Deprecated("Use getProfileRef(agentId)")
    @JvmStatic fun getProfilesRef(): DatabaseReference = devicesRef

    @Deprecated("Use getIdentityRef(agentId)")
    @JvmStatic fun getIdentitiesRef(): DatabaseReference = devicesRef

    /**
     * TACTICAL SPEED AUDIT: Simulates the diag.py handshake to measure cloud latency.
     */
    fun performLatencyAudit(callback: (latencyMs: Long) -> Unit) {
        val startTime = System.currentTimeMillis()
        val diagRef = devicesRef.child("system_audit").child("latency")
        
        val pingData = mapOf(
            "timestamp" to startTime,
            "deviceId" to "DIAG_NODE"
        )

        diagRef.setValue(pingData).addOnCompleteListener {
            val latency = System.currentTimeMillis() - startTime
            com.jdroidx.jfindme.utils.SystemLogger.i("FirebaseAudit", "📡 Tactical Cloud Handshake: ${latency}ms")
            callback(latency)
        }
    }

    /**
     * 🛡️ IDENTITY GUARDIAN: Strictly validates Agent IDs to prevent Auth UID leakage.
     * All forensic Agent IDs must be 8-character alphanumeric signatures.
     */
    @JvmStatic
    fun isAgentIdValid(id: String): Boolean {
        if (id.isEmpty()) return false
        // Firebase UIDs are typically 28 characters. Agent IDs are 8.
        return id.length == 8 && id.matches(Regex("^[A-Z0-9]+$"))
    }

    @JvmStatic
    fun resolveHardwareIdentity(authUid: String, callback: (String?) -> Unit) {
        resolveHardwareIdentity(authUid, 0, callback)
    }

    @JvmStatic
    fun resolveHardwareIdentity(authUid: String, retryCount: Int, callback: (String?) -> Unit) {
        val context = com.jdroidx.jfindme.JFindMeApplication.getContext()
        if (context != null) {
            val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val localId = db.resolveAgentIdLocally(authUid)
            if (!localId.isNullOrEmpty()) {
                callback(localId)
                return
            }
        }

        lookupIndicesRef.child("auth_to_device").child(authUid).get().addOnSuccessListener { indexSnap ->
            val indexedAgentId = indexSnap.getValue(String::class.java)
            if (!indexedAgentId.isNullOrEmpty() && isAgentIdValid(indexedAgentId)) {
                callback(indexedAgentId)
                return@addOnSuccessListener
            }

            devicesRef.orderByChild("Identity/auth_uid").equalTo(authUid).limitToFirst(1)
                .get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val key = snapshot.children.firstOrNull()?.key
                        if (key != null && isAgentIdValid(key)) {
                            registerLookupIndex(authUid, key)
                            callback(key)
                        } else {
                            var foundValid = false
                            for (child in snapshot.children) {
                                val childKey = child.key
                                if (childKey != null && isAgentIdValid(childKey)) {
                                    registerLookupIndex(authUid, childKey)
                                    callback(childKey)
                                    foundValid = true
                                    break
                                }
                            }
                            if (!foundValid) callback(null)
                        }
                    } else {
                        callback(null)
                    }
                }.addOnFailureListener {
                    if (retryCount < 3) {
                        val backoff = (1000 * 2.0.pow(retryCount.toDouble())).toLong()
                        SystemLogger.w("FirebaseComm", "Timeout resolving identity, retrying in $backoff ms...")
                        Handler(Looper.getMainLooper()).postDelayed({
                            resolveHardwareIdentity(authUid, retryCount + 1, callback)
                        }, backoff)
                    } else {
                        SystemLogger.e("FirebaseAudit", "Identity Resolution Failed: ${it.message}")
                        callback(null)
                    }
                }
        }.addOnFailureListener {
            callback(null)
        }
    }

    /**
     * 🧹 CLOUD DEDUPLICATION ENGINE: Purges 28-character Auth UID nodes mistakenly created under /devices
     * and consolidates all telemetry and profile markers under the canonical 8-character Agent ID nodes.
     */
    @JvmStatic
    fun purgeLegacyAuthUidNodes() {
        devicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val authUidNodes = mutableListOf<DataSnapshot>()
                val validAgentMap = mutableMapOf<String, String>() // authUid -> agentId

                snapshot.children.forEach { child ->
                    val key = child.key ?: return@forEach
                    if (isAgentIdValid(key)) {
                        val authUid = child.child("Identity").child("auth_uid").getValue(String::class.java)
                        if (!authUid.isNullOrEmpty()) {
                            validAgentMap[authUid] = key
                            registerLookupIndex(authUid, key)
                        }

                        // 🧹 Clean nested duplicate child node (Image 1: /devices/{agentId}/{agentId})
                        if (child.hasChild(key)) {
                            SystemLogger.w("FirebaseComm", "🧹 Purging nested duplicate child node /devices/$key/$key")
                            child.child(key).ref.removeValue()
                        }

                        // 🧹 Clean rogue HW_ child subnodes (Image 2: /devices/{agentId}/HW_...)
                        for (subChild in child.children) {
                            val subKey = subChild.key ?: continue
                            if (subKey.startsWith("HW_")) {
                                SystemLogger.w("FirebaseComm", "🧹 Purging rogue HW_ child node /devices/$key/$subKey")
                                subChild.ref.removeValue()
                            }
                        }

                        // 🧹 Clean root level fcm_token and move to Identity & device subnodes
                        if (child.hasChild("fcm_token")) {
                            val tokenVal = child.child("fcm_token").getValue(String::class.java)
                            if (!tokenVal.isNullOrEmpty()) {
                                child.child("Identity").child("fcm_token").ref.setValue(tokenVal)
                                child.child("device").child("fcm_token").ref.setValue(tokenVal)
                            }
                            child.child("fcm_token").ref.removeValue()
                        }
                    } else {
                        authUidNodes.add(child)
                    }
                }

                authUidNodes.forEach { duplicateSnap ->
                    val authUidKey = duplicateSnap.key ?: return@forEach
                    SystemLogger.w("FirebaseComm", "🧹 DEDUPLICATION: Purging rogue Auth UID node /devices/$authUidKey")
                    
                    val targetAgentId = validAgentMap[authUidKey]
                    if (targetAgentId != null) {
                        val targetRef = devicesRef.child(targetAgentId)
                        if (duplicateSnap.hasChild("device/live") && !snapshot.child(targetAgentId).hasChild("device/live")) {
                            targetRef.child("device/live").setValue(duplicateSnap.child("device/live").value)
                        }
                        if (duplicateSnap.hasChild("profile") && !snapshot.child(targetAgentId).hasChild("profile")) {
                            targetRef.child("profile").setValue(duplicateSnap.child("profile").value)
                        }
                    }
                    devicesRef.child(authUidKey).removeValue()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    @JvmStatic
    fun resolveHardwareIdentityByHardwareId(hwId: String, callback: (String?) -> Unit) {
        resolveHardwareIdentityByHardwareId(hwId, 0, callback)
    }

    @JvmStatic
    fun resolveHardwareIdentityByHardwareId(hwId: String, retryCount: Int, callback: (String?) -> Unit) {
        database.reference.child("hw_mapping").child(hwId).get().addOnSuccessListener { snapshot ->
            val agentId = try {
                snapshot.getValue(String::class.java)
            } catch (e: JSONException) {
                SystemLogger.e("FirebaseComm", "JSONException while parsing hw_mapping: ${e.message}")
                null
            } catch (e: Exception) {
                if (e.cause is JSONException) {
                    SystemLogger.e("FirebaseComm", "JSONException: Malformed payload")
                }
                null
            }
            
            if (agentId != null && isAgentIdValid(agentId)) {
                callback(agentId)
            } else {
                val cleanHw = hwId.replace("HW_", "").replace("-", "").uppercase()
                val derived = if (cleanHw.length >= 8) cleanHw.take(4) + cleanHw.takeLast(4) else cleanHw.padEnd(8, '7')
                callback(derived)
            }
        }.addOnFailureListener {
            if (retryCount < 3) {
                val backoff = (1000 * 2.0.pow(retryCount.toDouble())).toLong()
                SystemLogger.w("FirebaseComm", "Timeout resolving HW identity, retrying in $backoff ms...")
                Handler(Looper.getMainLooper()).postDelayed({
                    resolveHardwareIdentityByHardwareId(hwId, retryCount + 1, callback)
                }, backoff)
            } else {
                val cleanHw = hwId.replace("HW_", "").replace("-", "").uppercase()
                val derived = if (cleanHw.length >= 8) cleanHw.take(4) + cleanHw.takeLast(4) else cleanHw.padEnd(8, '7')
                callback(derived)
            }
        }
    }

    @JvmStatic
    fun <T> safeParseSnapshot(snapshot: DataSnapshot, clazz: Class<T>): T? {
        return try {
            snapshot.getValue(clazz)
        } catch (e: JSONException) {
            SystemLogger.e("FirebaseComm", "JSONException parsing snapshot ${snapshot.key}: ${e.message}")
            null
        } catch (e: Exception) {
            if (e.cause is JSONException || e.message?.contains("JSONException") == true) {
                SystemLogger.e("FirebaseComm", "JSONException: Malformed payload for ${snapshot.key}")
            } else {
                SystemLogger.e("FirebaseComm", "Parse Error for ${snapshot.key}: ${e.message}")
            }
            null
        }
    }

    /**
     * 🧬 V67.0 UNIFIED FORENSIC HEARTBEAT (The Master Program)
     * Consolidates Identity, Profile, Hardware, and Location into ONE atomic sync.
     * Follows strict 1:1 field mapping as per Forensic Identity Matrix.
     */
    @JvmStatic
    fun syncForensicHeartbeat(
        authUid: String,
        agentId: String,
        hardwareId: String,
        payload: Map<String, Any>,
        activeGroupId: String? = null
    ) {
        if (!isAgentIdValid(agentId)) {
            com.jdroidx.jfindme.utils.SystemLogger.e("FirebaseComm", "BLOCKED: Invalid Agent ID $agentId")
            return
        }

        val now = getForensicTimestamp()
        val atomicPayload = mutableMapOf<String, Any>()

        // 🧬 CLEAN HIERARCHY MIRROR (V67.8): All table data even blank
        
        // 1. IDENTITY NODE (Tier 1 Mirror)
        val idPath = "$NODE_DEVICES/$agentId/Identity"
        atomicPayload["$idPath/auth_uid"] = authUid
        atomicPayload["$idPath/uid_1"] = agentId
        atomicPayload["$idPath/uid_2"] = hardwareId
        val fullName = payload["name"] ?: payload["userName"] ?: payload["id_full_name"] ?: ""
        atomicPayload["$idPath/id_full_name"] = fullName
        atomicPayload["$idPath/node_index"] = 1
        atomicPayload["$idPath/is_admin"] = "1"
        atomicPayload["$idPath/owner_name"] = payload["owner_name"] ?: fullName
        atomicPayload["$idPath/connected_id"] = payload["connected_id"] ?: hardwareId
        atomicPayload["$idPath/qr_code_ref"] = payload["qr_code_ref"] ?: ""
        atomicPayload["$idPath/status"] = "Active"
        atomicPayload["$idPath/updated_at"] = now

        // 2. PROFILE NODE (Tier 1 Metadata)
        val profPath = "$NODE_DEVICES/$agentId/profile"
        atomicPayload["$profPath/name"] = payload["name"] ?: payload["userName"] ?: ""
        atomicPayload["$profPath/email"] = payload["email"] ?: payload["userEmail"] ?: ""
        atomicPayload["$profPath/phone"] = payload["phone"] ?: ""
        atomicPayload["$profPath/dob"] = payload["dob"] ?: ""
        atomicPayload["$profPath/gender"] = payload["gender"] ?: ""
        atomicPayload["$profPath/country"] = payload["country"] ?: ""
        atomicPayload["$profPath/state"] = payload["state"] ?: ""
        atomicPayload["$profPath/city"] = payload["city"] ?: ""
        atomicPayload["$profPath/pincode"] = payload["pincode"] ?: ""
        atomicPayload["$profPath/address"] = payload["address"] ?: ""
        atomicPayload["$profPath/org_name"] = payload["org_name"] ?: ""
        atomicPayload["$profPath/dept_name"] = payload["dept_name"] ?: ""
        atomicPayload["$profPath/avatar_id"] = payload["avatar_id"] ?: "1"
        atomicPayload["$profPath/note"] = payload["emergency_note"] ?: payload["note"] ?: ""
        atomicPayload["$profPath/updated_at"] = now

        // 3. DEVICE NODE (Tier 2 Hardware Hub)
        val devPath = "$NODE_DEVICES/$agentId/device"
        atomicPayload["$devPath/device_id"] = hardwareId
        atomicPayload["$devPath/model"] = payload["model"] ?: android.os.Build.MODEL
        atomicPayload["$devPath/imei"] = payload["imei"] ?: ""
        atomicPayload["$devPath/is_rooted"] = if (payload["is_rooted"] == true) "1" else "0"
        atomicPayload["$devPath/fingerprint"] = payload["fingerprint"] ?: ""
        atomicPayload["$devPath/latitude"] = payload["latitude"] ?: 0.0
        atomicPayload["$devPath/longitude"] = payload["longitude"] ?: 0.0
        atomicPayload["$devPath/Address"] = payload["Address"] ?: payload["address"] ?: ""
        atomicPayload["$devPath/updated_at"] = now

        // 🛡️ V67.8: ALL OTHER NODES DECOMMISSIONED
        
        database.reference.updateChildren(atomicPayload).addOnSuccessListener {
            com.jdroidx.jfindme.utils.SystemLogger.i("FirebaseComm", "CLEAN HEARTBEAT SUCCESS: $agentId")
        }.addOnFailureListener {
            com.jdroidx.jfindme.utils.SystemLogger.e("FirebaseComm", "HEARTBEAT FAILURE: ${it.message}")
        }
    }

    /**
     * 🧬 V67.2: DECOMMISSIONED - All legacy registration and safezone logic suspended.
     */
    @JvmStatic fun syncRegistrationBaseline(uid1: String, baseline: Map<String, Any>) {}
    @JvmStatic fun syncSafeZoneAtomic(zoneId: String, deviceId: String, zoneData: Map<String, Any>) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val zoneRef = firestore.collection("safe_zones").document(zoneId)
        val assignedDevicesRef = zoneRef.collection("assigned_devices").document(deviceId)
        // using composite key document for easy querying if needed, or subcollection.
        val userZoneMapRef = firestore.collection("user_safe_zone_map").document(deviceId).collection("zones").document(zoneId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(zoneRef)
            val currentVersion = snapshot.getLong("version") ?: 0L

            transaction.set(zoneRef, zoneData.plus("version" to currentVersion + 1), com.google.firebase.firestore.SetOptions.merge())
            
            val assignedData = mapOf("status" to "ACTIVE", "assigned_at" to com.google.firebase.firestore.FieldValue.serverTimestamp())
            transaction.set(assignedDevicesRef, assignedData, com.google.firebase.firestore.SetOptions.merge())
            
            val mapData = mapOf("status" to "ACTIVE", "role" to "MEMBER", "assigned_at" to com.google.firebase.firestore.FieldValue.serverTimestamp())
            transaction.set(userZoneMapRef, mapData, com.google.firebase.firestore.SetOptions.merge())

            null
        }.addOnSuccessListener {
            SystemLogger.i("SafeZoneEngine", "Atomic batch committed: zone=$zoneId device=$deviceId")
        }.addOnFailureListener { e ->
            SystemLogger.e("SafeZoneEngine", "ATOMIC BATCH FAILED for zoneId: $zoneId", e)
        }
    }

    @JvmStatic
    fun getInvertedForensicKey(input: Any? = System.currentTimeMillis()): String {
        val baseline = 9999123123595999L
        val current = when (input) {
            is Long -> getForensicTimestamp(input).toLongOrNull() ?: input
            is String -> input.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
        return (baseline - current).toString()
    }

    @JvmStatic
    fun parseTimestampFromInvertedKey(key: String): String {
        if (key.isBlank()) return getForensicTimestamp()
        return try {
            val baseline = 9999123123595999L
            val numKey = key.trim().toLongOrNull() ?: return getForensicTimestamp()
            val origTs = baseline - numKey
            val origStr = origTs.toString()
            if (origStr.length >= 14) {
                origStr.padEnd(16, '0').take(16)
            } else {
                getForensicTimestamp(origTs)
            }
        } catch (e: Exception) {
            getForensicTimestamp()
        }
    }

    /**
     * 🧬 V60 High-Precision Forensic Pulse
     * Returns: YYYYMMDDHHMMSSSS (16-character precise timeline)
     */
    @JvmStatic
    fun getForensicTimestamp(input: Any? = System.currentTimeMillis()): String {
        if (input == null) return getForensicTimestamp(System.currentTimeMillis())
        if (input is String) {
            val s = input.trim()
            if (s.length == 16 && s.matches(Regex("^\\d{16}$"))) return s
            if (s.matches(Regex("^\\d{13}$"))) {
                val num = s.toLongOrNull()
                if (num != null) return getForensicTimestamp(num)
            }
            if (s.matches(Regex("^\\d{10}$"))) {
                val num = s.toLongOrNull()
                if (num != null) return getForensicTimestamp(num * 1000L)
            }
            val patterns = arrayOf("yyyyMMddHHmmssSS", "yyyyMMddHHmmss", "yyyyMMdd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss")
            for (p in patterns) {
                try {
                    val sdf = java.text.SimpleDateFormat(p, java.util.Locale.US)
                    val d = sdf.parse(if (s.length > p.length) s.substring(0, p.length) else s)
                    if (d != null) return getForensicTimestamp(d.time)
                } catch (e: Exception) {}
            }
            if (s.isBlank()) return getForensicTimestamp(System.currentTimeMillis())
        }
        
        val timeMs = when (input) {
            is Long -> input
            is Number -> input.toLong()
            else -> System.currentTimeMillis()
        }
        
        val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmssSS", java.util.Locale.US)
        val formatted = sdf.format(java.util.Date(timeMs))
        return if (formatted.length > 16) formatted.take(16) else formatted.padEnd(16, '0')
    }

    @JvmStatic
    fun syncAnalyticalEvent(agentId: String, eventData: UserLocation) {
        if (!isAgentIdValid(agentId)) return
        val now = getForensicTimestamp()
        val updates = mutableMapOf<String, Any>()
        
        // 1. LIVE NODE: Real-time telemetry cursor (Shallow O(1) Read)
        val livePath = "$NODE_DEVICES/$agentId/device/live"
        updates["$livePath/latitude"] = eventData.latitude
        updates["$livePath/longitude"] = eventData.longitude
        updates["$livePath/speed"] = eventData.speed
        updates["$livePath/battery"] = eventData.batteryLevel
        updates["$livePath/address"] = eventData.address ?: ""
        updates["$livePath/updated_at"] = now
        
        // 2. DEVICE HISTORY NODE: Only latest point under device
        val historyKey = getInvertedForensicKey()
        updates["$NODE_DEVICES/$agentId/history"] = mapOf(historyKey to eventData.toMap())

        // 3. TOP-LEVEL /History ARCHIVE: Time-Series Stream (Latest - 1 data)
        val archiveHistPath = "$NODE_HISTORY_ARCHIVE/$agentId/history/$historyKey"
        updates[archiveHistPath] = eventData.toMap()
        
        database.reference.updateChildren(updates).addOnFailureListener {
            com.jdroidx.jfindme.utils.SystemLogger.e("FirebaseComm", "SYNC_FAILURE: ${it.message}")
        }
    }

    /**
     * 🧬 V72.0: Partitioned Multi-Account Trace Synchronization (No Merging / No Overlap)
     * Writes real-time telemetry cursor to devices/$agentId/device/live AND archives immutable
     * time-series point to trace_archives/$authUid/$agentId/$epochId/$timestamp.
     */
    @JvmStatic
    fun syncPartitionedLocationTrace(authUid: String, agentId: String, epochId: String, eventData: UserLocation) {
        if (!isAgentIdValid(agentId)) return
        val now = getForensicTimestamp()
        val updates = mutableMapOf<String, Any>()

        // 1. Live Hub Cursor (O(1) Shallow Read)
        val livePath = "$NODE_DEVICES/$agentId/device/live"
        updates["$livePath/latitude"] = eventData.latitude
        updates["$livePath/longitude"] = eventData.longitude
        updates["$livePath/speed"] = eventData.speed
        updates["$livePath/battery"] = eventData.batteryLevel
        updates["$livePath/address"] = eventData.address ?: ""
        updates["$livePath/userName"] = eventData.userName
        updates["$livePath/avatar_id"] = eventData.avatar_id
        updates["$livePath/bearing"] = eventData.bearing
        updates["$livePath/isSosActive"] = eventData.isSosActive
        updates["$livePath/auth_uid"] = authUid
        updates["$livePath/epoch_id"] = epochId
        updates["$livePath/updated_at"] = now

        // 2. Device History Node: Only latest 1 data point under device
        val historyKey = getInvertedForensicKey()
        val payload = eventData.toMap().toMutableMap()
        payload["auth_uid"] = authUid
        payload["epoch_id"] = if (epochId.isNotEmpty()) epochId else "DEFAULT_EPOCH"
        payload["timestamp"] = now
        updates["$NODE_DEVICES/$agentId/history"] = mapOf(historyKey to payload)

        // 3. TOP-LEVEL /History ARCHIVE: Time-Series Stream (Latest - 1 data)
        val archivePath = "$NODE_HISTORY_ARCHIVE/$agentId/history/$historyKey"
        updates[archivePath] = payload

        database.reference.updateChildren(updates).addOnFailureListener {
            com.jdroidx.jfindme.utils.SystemLogger.e("FirebaseComm", "PARTITIONED_SYNC_FAILURE: ${it.message}")
        }
    }

    /**
     * 🧹 Prune abandoned or expired in-flight QR pairing sessions created by this host.
     */
    @JvmStatic
    @JvmOverloads
    fun cleanupAbandonedPairingSessions(hostId: String, onPruned: ((Int) -> Unit)? = null) {
        if (hostId.isEmpty()) return
        pairingRef.orderByChild("parentDeviceId").equalTo(hostId).get().addOnSuccessListener { snapshot ->
            var count = 0
            for (child in snapshot.children) {
                val status = child.child("status").getValue(String::class.java)
                if (status == "waiting" || status == "cancelled" || status == "expired") {
                    child.ref.removeValue()
                    count++
                }
            }
            if (count > 0) {
                SystemLogger.i("FirebaseComm", "Pruned $count abandoned pairing sessions for host: $hostId")
            }
            onPruned?.invoke(count)
        }.addOnFailureListener {
            SystemLogger.e("FirebaseComm", "Failed to query abandoned pairing sessions: ${it.message}")
            onPruned?.invoke(0)
        }
    }

    /**
     * 🔄 Cloud History Re-hydration: Pulls past traces for an authenticated account.
     */
    @JvmStatic
    fun rehydrateCloudLocationHistory(authUid: String, agentId: String, onComplete: ((List<Map<String, Any>>) -> Unit)) {
        val targetId = if (isAgentIdValid(agentId)) agentId else authUid
        if (targetId.isEmpty()) {
            onComplete(emptyList())
            return
        }
        database.reference.child(NODE_HISTORY_ARCHIVE).child(targetId).child("history").get().addOnSuccessListener { snapshot ->
            val points = mutableListOf<Map<String, Any>>()
            for (pointSnap in snapshot.children) {
                try {
                    val latVal = pointSnap.child("latitude").value ?: pointSnap.child("lat").value
                    val lat = when (latVal) {
                        is Number -> latVal.toDouble()
                        is String -> latVal.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }

                    val lngVal = pointSnap.child("longitude").value ?: pointSnap.child("lng").value
                    val lng = when (lngVal) {
                        is Number -> lngVal.toDouble()
                        is String -> lngVal.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }

                    val tsVal = pointSnap.child("timestamp").value ?: pointSnap.key ?: ""
                    val ts = when (tsVal) {
                        is Number -> {
                            val epoch = tsVal.toLong()
                            if (epoch > 1000000000000L) {
                                java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date(epoch))
                            } else {
                                epoch.toString()
                            }
                        }
                        else -> tsVal.toString()
                    }

                    val speedVal = pointSnap.child("speed").value
                    val speed = when (speedVal) {
                        is Number -> speedVal.toFloat()
                        is String -> speedVal.toFloatOrNull() ?: 0f
                        else -> 0f
                    }

                    val batVal = pointSnap.child("battery").value
                    val battery = when (batVal) {
                        is Number -> batVal.toInt()
                        is String -> batVal.toIntOrNull() ?: 100
                        else -> 100
                    }

                    val addr = pointSnap.child("address").value?.toString() ?: ""

                    if (lat != 0.0 && lng != 0.0) {
                        points.add(mapOf(
                            "lat" to lat,
                            "lng" to lng,
                            "timestamp" to ts,
                            "speed" to speed,
                            "battery" to battery,
                            "address" to addr
                        ))
                    }
                } catch (e: Exception) {
                    SystemLogger.w("FirebaseComm", "Skipping malformed history record: ${e.message}")
                }
            }
            onComplete(points)
        }.addOnFailureListener {
            SystemLogger.e("FirebaseComm", "History rehydration failed: ${it.message}")
            onComplete(emptyList())
        }
    }

    /**
     * 🧬 1-Click Closed-Loop Profile Update (SSOT)
     * Updates user metadata across the whole network instantly by updating the master profile node.
     */
    @JvmStatic
    fun updateProfileAtomic(agentId: String, profileData: Map<String, Any>, onComplete: ((Boolean) -> Unit)? = null) {
        if (!isAgentIdValid(agentId)) {
            onComplete?.invoke(false)
            return
        }
        val now = getForensicTimestamp()
        val payload = profileData.toMutableMap()
        payload["updated_at"] = now

        getProfileRef(agentId).updateChildren(payload).addOnSuccessListener {
            SystemLogger.i("FirebaseComm", "SSOT Profile Atomic Update Success for $agentId")
            onComplete?.invoke(true)
        }.addOnFailureListener {
            SystemLogger.e("FirebaseComm", "SSOT Profile Update Failed: ${it.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * 📱 Hardware Vitals & Telemetry Sync
     */
    @JvmStatic
    fun updateTelemetry(agentId: String, telemetryData: Map<String, Any>) {
        if (!isAgentIdValid(agentId)) return
        val now = getForensicTimestamp()
        val payload = telemetryData.toMutableMap()
        payload["updated_at"] = now
        getTelemetryRef(agentId).updateChildren(payload).addOnFailureListener {
            SystemLogger.e("FirebaseComm", "Telemetry sync failed: ${it.message}")
        }
    }

    /**
     * 🟢 Live Presence & Session Duration Management
     */
    @JvmStatic
    fun updatePresence(agentId: String, isOnline: Boolean, sessionStartEpoch: Long = System.currentTimeMillis()) {
        if (!isAgentIdValid(agentId)) return
        val now = System.currentTimeMillis()
        val durationSec = if (isOnline) 0L else ((now - sessionStartEpoch).coerceAtLeast(0L) / 1000L)
        val formatted = formatDuration(durationSec)
        val presenceData = mapOf(
            KEY_STATUS to (if (isOnline) STATUS_ONLINE else STATUS_OFFLINE),
            KEY_SESSION_START to sessionStartEpoch,
            KEY_DURATION_SEC to durationSec,
            KEY_DURATION_FORMATTED to formatted,
            KEY_LAST_STATUS_CHANGE to now,
            "last_seen" to now
        )
        getTelemetryRef(agentId).updateChildren(presenceData)
    }

    @JvmStatic
    fun formatDuration(seconds: Long): String {
        if (seconds < 60) return "${seconds}s"
        val mins = seconds / 60
        val secs = seconds % 60
        if (mins < 60) return "${mins}m ${secs}s"
        val hours = mins / 60
        val remainingMins = mins % 60
        return "${hours}h ${remainingMins}m"
    }

    /**
     * ⏱️ O(1) Instant Latest-First History Query
     * Uses Inverted-Key natural ordering (limitToFirst retrieves newest data without server sorting).
     */
    @JvmStatic
    fun queryLatestHistory(agentId: String, limit: Int = 30, onResult: (List<Map<String, Any>>) -> Unit) {
        if (!isAgentIdValid(agentId)) {
            onResult(emptyList())
            return
        }
        getHistoryRef(agentId).limitToFirst(limit).get().addOnSuccessListener { snapshot ->
            val list = mutableListOf<Map<String, Any>>()
            for (child in snapshot.children) {
                @Suppress("UNCHECKED_CAST")
                val map = child.value as? Map<String, Any> ?: continue
                list.add(map)
            }
            onResult(list)
        }.addOnFailureListener {
            SystemLogger.e("FirebaseComm", "Failed to query latest history: ${it.message}")
            onResult(emptyList())
        }
    }

    /**
     * 🚨 Security & SOS Alert Broadcaster
     */
    @JvmStatic
    fun publishAlert(agentId: String, alertId: String, alertData: Map<String, Any>) {
        if (!isAgentIdValid(agentId)) return
        val now = getForensicTimestamp()
        val payload = alertData.toMutableMap()
        payload["timestamp"] = now
        getAlertsRef(agentId).child(alertId).setValue(payload).addOnFailureListener {
            SystemLogger.e("FirebaseComm", "Alert publish failed: ${it.message}")
        }
    }

    /**
     * 🚨 1-Tap SOS Emergency Broadcast
     */
    @JvmStatic
    fun publishSosEmergency(agentId: String, latitude: Double, longitude: Double, note: String = "EMERGENCY SOS TRIGGERED") {
        if (!isAgentIdValid(agentId)) return
        val nowMs = System.currentTimeMillis()
        val nowStr = getForensicTimestamp(nowMs)
        val alertPayload = mapOf(
            "type" to "SOS_ALERT",
            "title" to "EMERGENCY SOS SIGNAL",
            "message" to note,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to nowStr,
            "epoch_ms" to nowMs,
            "status" to "ACTIVE"
        )
        publishAlert(agentId, "sos_latest", alertPayload)
        devicesRef.child(agentId).child("device").child("sos_active").setValue(true)
        SystemLogger.w("FirebaseComm", "🚨 SOS_EMERGENCY_BROADCAST_ACTIVE for Node: $agentId at ($latitude, $longitude)")
    }

    /**
     * 🔊 Remote Ring / Acoustic Beacon Trigger
     */
    @JvmStatic
    fun triggerRemoteRingDevice(targetDeviceId: String, durationSec: Int = 30) {
        if (!isAgentIdValid(targetDeviceId)) return
        val cmd = mapOf(
            "action" to "RING_DEVICE",
            "duration_sec" to durationSec,
            "epoch_ms" to System.currentTimeMillis()
        )
        devicesRef.child(targetDeviceId).child("device").child("remote_command").setValue(cmd)
        SystemLogger.w("FirebaseComm", "🔊 Remote ring acoustic beacon dispatched to $targetDeviceId (${durationSec}s)")
    }

    /**
     * 👥 Device-to-Device Mesh Relationship Synchronization
     */
    @JvmStatic
    fun syncRelationship(agentId: String, targetDeviceId: String, relationData: Map<String, Any>) {
        if (!isAgentIdValid(agentId) || !isAgentIdValid(targetDeviceId)) return
        val now = getForensicTimestamp()
        val payload = relationData.toMutableMap()
        payload["updated_at"] = now
        getRelationshipsRef(agentId).child(targetDeviceId).updateChildren(payload).addOnFailureListener {
            SystemLogger.e("FirebaseComm", "Relationship sync failed: ${it.message}")
        }
    }

    /**
     * 🔍 Register Fast Lookup Index (Auth UID -> Device ID)
     */
    @JvmStatic
    fun registerLookupIndex(authUid: String, agentId: String) {
        if (authUid.isEmpty() || !isAgentIdValid(agentId)) return
        lookupIndicesRef.child("auth_to_device").child(authUid).setValue(agentId)
    }

    @Deprecated("Use syncAnalyticalEvent for high-fidelity tiered tracking")
    fun updateMyLocation(userLocation: UserLocation) {
        val rId = userLocation.getResolvedUserId()
        if (rId.isNotEmpty()) {
            syncAnalyticalEvent(rId, userLocation)
        }
    }
}
