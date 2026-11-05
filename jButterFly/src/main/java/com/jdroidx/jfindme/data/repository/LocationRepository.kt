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

    private var lastGeocodeTime: Long = 0
    private var lastGeocodedLocation: android.location.Location? = null
    private var cachedHumanAddress: String? = null

    private fun processIncomingLocation(resolvedId: String, loc: UserLocation) {
        val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
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
        // Top-to-Bottom: Permitted by default (parents/guardians view dependents)
        // Bottom-to-Top: Blocked unless explicitly permitted by allowDependents == true or emergency SOS active
        val myRole = db.getRoleForDevice(myAgentId) ?: prefs.getString("user_role", "") ?: ""
        val isLocalDependent = myRole.contains("CHILD", ignoreCase = true) || 
                               myRole.contains("DEPENDENT", ignoreCase = true) || 
                               myRole.contains("WARD", ignoreCase = true) ||
                               prefs.getBoolean("is_dependent", false)
        if (!isSelf && isLocalDependent && !loc.allowDependents && !loc.isSosActive) {
            SystemLogger.w("LocationRepository", "MLM Gate Enforced: Upstream location for $resolvedId suppressed (allowDependents=false, sos=false).")
            val canonicalStoreId = resolvedId
            latestLocations.remove(canonicalStoreId)
            _userLocations.postValue(latestLocations.values.sortedWith(
                compareByDescending<UserLocation> { it.isSosActive }
                    .thenByDescending { it.getLongTimestamp() }
            ))
            return
        }

        // 🚀 REMOTE PEER INGESTION: Process all authorized incoming telemetry for local and remote devices
        SystemLogger.i("LocationRepository", "Ingesting location for Node: $resolvedId (Lat: ${loc.latitude}, Lng: ${loc.longitude})")

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

        // 🚀 STABLE AVATAR RESOLUTION: Custom assigned avatar takes top precedence over fallback default
        val liveAvatar = loc.avatar_id
        val ssotAvatar = db.getDeviceCentricAvatar(resolvedId)

        if (!liveAvatar.isNullOrEmpty() && liveAvatar != "butterfly") {
            loc.avatar_id = liveAvatar
        } else if (ssotAvatar.isNotEmpty()) {
            loc.avatar_id = ssotAvatar
        } else if (!liveAvatar.isNullOrEmpty()) {
            loc.avatar_id = liveAvatar
        } else {
            loc.avatar_id = "butterfly"
        }
        
        if (cloudName.isNotEmpty() && cloudName != profile?.get("id_full_name") && !isSelf) {
            try {
                val uid2 = profile?.get("uid_2") 
                db.saveRosterNode(resolvedId, cloudName, "Active Tracker", "LIVE TARGET TRACKED", false, true, uid2 = uid2, qrCode = loc.avatar_id)
                db.updateDeviceIdentityAndAvatar(resolvedId, cloudName, loc.avatar_id)
            } catch (e: Exception) {
                com.jdroidx.jfindme.utils.SystemLogger.e("LocationRepository", "Forensic Backfill Blocked", e)
            }
        }

        if (loc.userName.trim().isEmpty() || loc.userName.trim().equals("null", ignoreCase = true)) {
            loc.userName = if (isSelf) "You" else ("Agent_" + resolvedId)
        }

        val canonicalStoreId = if (isSelf) (if (!myUid.isNullOrEmpty()) myUid else myAgentId) else resolvedId
        if (isSelf) {
            // 🚀 Strict Self Deduplication: Purge any redundant representation of self
            if (!myUid.isNullOrEmpty()) latestLocations.remove(myUid)
            if (myAgentId.isNotEmpty()) latestLocations.remove(myAgentId)
            if (myHardwareUid.isNotEmpty()) latestLocations.remove(myHardwareUid)
            if (myRawAndroidId.isNotEmpty()) latestLocations.remove(myRawAndroidId)
            latestLocations.remove("SELF_USER")
            latestLocations.remove("You")
        }

        latestLocations[canonicalStoreId] = loc
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
                if (trimmed.isNotEmpty()) targetUids.add(trimmed)
            }
        }
        if (friendUids.isNotEmpty()) {
            friendUids.split(",").forEach {
                val trimmed = it.trim()
                if (trimmed.isNotEmpty()) targetUids.add(trimmed)
            }
        }

        // 🚀 CRITICAL FIX: Pull ONLY verified handshaked devices from SQLite Roster
        try {
            val localDb = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
            val offlineNodes = localDb.getOfflineRoster()
            for (node in offlineNodes) {
                val u1 = node["uid_1"] as? String
                val u2 = node["uid_2"] as? String
                val connId = node["connected_id"] as? String
                if (!u1.isNullOrBlank()) targetUids.add(u1.trim())
                if (!u2.isNullOrBlank()) targetUids.add(u2.trim())
                if (!connId.isNullOrBlank()) targetUids.add(connId.trim())
            }

            // 🛡️ Include all verified paired device IDs across DevicePairingTable and UserIdentityTable
            val pairedIds = localDb.getAllPairedDeviceIds()
            targetUids.addAll(pairedIds)

            // 🛡️ SOFT-DELETE PURGE: Immediately evict any target marked as DELETED
            targetUids.removeAll { uid ->
                uid != myUid && uid != myAgentId && localDb.isDeviceDeleted(uid)
            }
        } catch (e: Exception) {
            SystemLogger.e("LocationRepository", "Failed reading offline roster for targets", e)
        }
        
        val iter = userLocationListeners.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (!targetUids.contains(entry.key)) {
                val ref = FirebaseComm.devicesRef.child(entry.key).child("device").child("live")
                ref.removeEventListener(entry.value)
                activeListeners.remove(ref)
                iter.remove()
                
                val profRef = FirebaseComm.devicesRef.child(entry.key).child("profile")
                profileListeners[entry.key]?.let {
                    profRef.removeEventListener(it)
                    activeListeners.remove(profRef)
                }
                profileListeners.remove(entry.key)
                latestLocations.remove(entry.key)
            }
        }
        
        for (uid in targetUids) {
            if (!userLocationListeners.containsKey(uid)) {
                val ref = FirebaseComm.devicesRef.child(uid).child("device").child("live")
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var rawLoc = snapshot.getValue(UserLocation::class.java)
                        val explicitAvatar = snapshot.child("avatar_id").getValue(String::class.java)
                        val explicitName = snapshot.child("userName").getValue(String::class.java) 
                            ?: snapshot.child("name").getValue(String::class.java)
                        val zoneName = snapshot.child("current_safe_zone_name").getValue(String::class.java)
                        val zoneState = snapshot.child("current_safe_zone_state").getValue(String::class.java)
                        val zoneId = snapshot.child("current_safe_zone_id").getValue(String::class.java)

                        if (rawLoc == null) {
                            val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                            val lng = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                            val acc = snapshot.child("accuracy").getValue(Double::class.java)?.toFloat()
                                ?: snapshot.child("accuracy").getValue(Long::class.java)?.toFloat() ?: 0f
                            val alt = snapshot.child("altitude").getValue(Double::class.java) ?: 0.0
                            val motion = snapshot.child("motion_state").getValue(String::class.java) ?: "UNKNOWN"
                            val spd = snapshot.child("speed").getValue(Double::class.java)?.toFloat()
                                ?: snapshot.child("speed").getValue(Long::class.java)?.toFloat() ?: 0f
                            val brg = snapshot.child("bearing").getValue(Double::class.java)?.toFloat()
                                ?: snapshot.child("bearing").getValue(Long::class.java)?.toFloat() ?: 0f
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
                                val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                                val zoneModel = db.getSafeZoneById(zoneId)
                                if (zoneModel != null && zoneModel.lat != 0.0 && zoneModel.lng != 0.0) {
                                    rawLoc = rawLoc.copy(latitude = zoneModel.lat, longitude = zoneModel.lng)
                                }
                            } catch (e: Exception) {}
                        }

                        // [Variables] | [Constants] | [Dependencies] | [Integration Points]
                        // Multi-field timestamp & telemetry normalization from primary cloud snapshot
                        val snapTs = snapshot.child("timestamp").getValue()
                        val snapHb = snapshot.child("last_heartbeat").getValue()
                        val snapUpAt = snapshot.child("updated_at").getValue()
                        val effectiveTs = snapTs ?: snapHb ?: snapUpAt ?: rawLoc.timestamp
                        val effectiveUpAt = snapUpAt ?: snapTs ?: snapHb ?: rawLoc.updated_at
                        val snapBattery = snapshot.child("battery").getValue(Long::class.java)?.toInt()
                            ?: snapshot.child("batteryLevel").getValue(Long::class.java)?.toInt()
                            ?: rawLoc.batteryLevel

                        val loc = rawLoc.copy(
                            userId = uid,
                            avatar_id = explicitAvatar ?: rawLoc.avatar_id,
                            userName = explicitName ?: rawLoc.userName,
                            timestamp = effectiveTs,
                            batteryLevel = snapBattery
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
                val profListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val profAvatar = snapshot.child("avatar_id").getValue(String::class.java)
                            ?: snapshot.child("equipped_avatar_id").getValue(String::class.java)
                        val profName = snapshot.child("name").getValue(String::class.java)
                            ?: snapshot.child("fullName").getValue(String::class.java)
                            ?: snapshot.child("userName").getValue(String::class.java)

                        if (!profAvatar.isNullOrEmpty()) {
                            try {
                                val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
                                db.commitAnalyticalDossier(uid, mapOf("avatar_id" to profAvatar, "equipped_avatar_id" to profAvatar))
                                com.jdroidx.jfindme.utils.AvatarLoader.clearCache()
                                
                                if (latestLocations.containsKey(uid)) {
                                    val cur = latestLocations[uid]!!
                                    cur.avatar_id = profAvatar
                                    if (!profName.isNullOrEmpty()) cur.userName = profName
                                    _userLocations.postValue(latestLocations.values.sortedWith(
                                        compareByDescending<UserLocation> { it.isSosActive }
                                            .thenByDescending { it.getLongTimestamp() }
                                    ))
                                }
                            } catch (e: Exception) {
                                SystemLogger.e("LocationRepository", "Profile Avatar Sync Failed for $uid", e)
                            }
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

            val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
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
                    val myGroup = if (rawGroup.isNotEmpty() && rawGroup != "DEFAULT_GROUP") rawGroup else "GRP_" + finalId
                    
                    prefs.edit().putString("active_group", myGroup).apply()
                    
                    val relRef = FirebaseComm.relationshipsRef.child(myGroup)
                    val relListener = object : ValueEventListener {
                        override fun onDataChange(relSnap: DataSnapshot) {
                            val csv = StringBuilder(finalId).append(",")
                            
                            // 1. Unified `devices/$finalId/paired_with`
                            val unifiedPairs = relSnap.child("devices").child(finalId).child("paired_with")
                            unifiedPairs.children.forEach { pair ->
                                if (pair.getValue(Boolean::class.java) == true && pair.key != null) {
                                    if (!csv.contains("${pair.key},")) {
                                        csv.append(pair.key).append(",")
                                    }
                                }
                            }

                            // 2. Legacy `direct_pairs/$finalId` for backwards compatibility
                            val directPairs = relSnap.child("direct_pairs").child(finalId)
                            directPairs.children.forEach { pair ->
                                if (pair.getValue(Boolean::class.java) == true && pair.key != null) {
                                    if (!csv.contains("${pair.key},")) {
                                        csv.append(pair.key).append(",")
                                    }
                                }
                            }
                            
                            val approvals = relSnap.child("sharing_approvals")
                            approvals.children.forEach { targetSnap ->
                                val targetId = targetSnap.key
                                if (!targetId.isNullOrEmpty() && targetSnap.child(finalId).getValue(Boolean::class.java) == true) {
                                    if (!csv.contains("$targetId,")) {
                                        csv.append(targetId).append(",")
                                    }
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
                    val csv = StringBuilder()
                    snap.children.forEach { childSnap -> csv.append(childSnap.key).append(",") }
                    prefs.edit().putString("friend_matrix_csv", csv.toString()).apply()
                    updateTargetListeners()
                }
                override fun onCancelled(err: DatabaseError) {
                    SystemLogger.e("LocationRepository", "Friend Matrix Sync Failed: ${err.message}")
                }
            }
            friendRef.addValueEventListener(friendListener)
            activeListeners[friendRef] = friendListener
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
                }
            } catch (e: Exception) {
                SystemLogger.e("LocationRepository", "Forensic Geocoding Blocked: ${e.message}")
            }
            
            finalizeLocationPush(userLocation, humanAddress)
        }
    }

    private fun finalizeLocationPush(userLocation: UserLocation, finalAddress: String?) {
        val enrichedLocation = userLocation.copy(address = finalAddress)
        offlineSyncManager.syncLocation(enrichedLocation)
        updateUserLocation(enrichedLocation)
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
        val db = com.jdroidx.jfindme.data.local.DatabaseHelper.getInstance(context)
        val localPoints = db.getRawHistoryPoints(userId, startTs, endTs)

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
                                points.add(point)
                                
                                db.saveHistoryPoint(
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
}
