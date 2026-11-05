package com.jdroidx.jfindme.engine.sync

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.local.tables.SyncQueueTable
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.DeviceIdentityHelper
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 🛰️ MasterDataSyncSentinel: Real-Time Master Data & Multi-Device Fan-Out Sentinel
 *
 * Enterprise Closed-Loop Architecture:
 * 1. Monitors and intercepts local master data mutations (Profile, Safe Zones, Roster, Settings).
 * 2. Instantly pushes metadata & payload to Firebase Realtime Database.
 * 3. Simultaneously fans out change alerts to all connected peer/guardian devices via `/inbox/master_sync`.
 * 4. Listens for incoming peer sync updates in background, reconciles local SQLite caches, and fires ACTION_MASTER_DATA_UPDATED.
 * 5. Queues changes in offline mode and flushes immediately upon network restoration.
 */
object MasterDataSyncSentinel {

    private const val TAG = "MasterDataSyncSentinel"

    // Action Constants
    const val ACTION_MASTER_DATA_UPDATED = "com.jdroidx.jfindme.ACTION_MASTER_DATA_UPDATED"
    const val EXTRA_CATEGORY = "EXTRA_CATEGORY"
    const val EXTRA_CHANGE_TYPE = "EXTRA_CHANGE_TYPE"
    const val EXTRA_ENTITY_ID = "EXTRA_ENTITY_ID"
    const val EXTRA_SENDER_ID = "EXTRA_SENDER_ID"

    // Category Constants
    const val CATEGORY_PROFILE = "PROFILE"
    const val CATEGORY_SAFE_ZONE = "SAFE_ZONE"
    const val CATEGORY_SETTINGS = "SETTINGS"
    const val CATEGORY_ROSTER = "ROSTER"
    const val CATEGORY_DEVICE_VITALS = "DEVICE_VITALS"

    // Change Types
    const val TYPE_UPSERT = "UPSERT"
    const val TYPE_DELETE = "DELETE"
    const val TYPE_UPDATE = "UPDATE"

    // Active Child Listeners
    private val activeListeners = ConcurrentHashMap<String, ChildEventListener>()

    /**
     * Checks if physical internet connectivity is active.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 🚀 Master Dispatch: Intercepts local mutation and broadcasts to Cloud + All Connected Devices.
     */
    @JvmStatic
    fun notifyMasterDataChanged(
        context: Context,
        category: String,
        changeType: String,
        entityId: String,
        payload: Map<String, Any?> = emptyMap()
    ) {
        val appContext = context.applicationContext
        val localAgentId = DeviceIdentityHelper.getOrGenerateAgentId(appContext)
        val nowEpoch = System.currentTimeMillis()
        val forensicTs = FirebaseComm.getForensicTimestamp(nowEpoch)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseHelper.getInstance(appContext)

                // 1. If Offline -> Enqueue into local SQLite SyncQueueTable
                if (!isNetworkAvailable(appContext) || !FirebaseComm.isAuthenticated()) {
                    SystemLogger.w(TAG, "Device offline or unauthenticated. Enqueuing master mutation: $category:$entityId")
                    try {
                        SyncQueueTable.queueItem(db.writableDatabase, "MASTER_$category", entityId)
                    } catch (e: Exception) {
                        SystemLogger.e(TAG, "Failed to enqueue master sync mutation", e)
                    }
                    return@launch
                }

                // 2. Online Mode -> Immediate Cloud Update
                val metaRef = FirebaseComm.devicesRef
                    .child(localAgentId)
                    .child("metadata")
                    .child("master_sync")

                val metadataPacket = mapOf(
                    "last_updated_ts" to nowEpoch,
                    "last_updated_forensic" to forensicTs,
                    "category" to category,
                    "change_type" to changeType,
                    "entity_id" to entityId,
                    "source_agent_id" to localAgentId
                )
                metaRef.updateChildren(metadataPacket)

                // If payload is present, mirror to entity node under device
                if (payload.isNotEmpty()) {
                    val entityRef = FirebaseComm.devicesRef
                        .child(localAgentId)
                        .child(category.lowercase())
                        .child(entityId)
                    entityRef.updateChildren(payload)
                }

                SystemLogger.i(TAG, "Local master data pushed to Cloud: $category [$changeType] -> $entityId")

                // 3. Simultaneous Peer Fan-out: Send to all connected/paired devices
                val pairedIds = db.getAllPairedDeviceIds()
                if (pairedIds.isNotEmpty()) {
                    val peerPacket = mapOf(
                        "sender_agent_id" to localAgentId,
                        "category" to category,
                        "change_type" to changeType,
                        "entity_id" to entityId,
                        "timestamp" to nowEpoch,
                        "forensic_time" to forensicTs,
                        "payload" to payload
                    )

                    var sentCount = 0
                    for (peerId in pairedIds) {
                        if (peerId.isNotBlank() && peerId != localAgentId) {
                            try {
                                val inboxRef = FirebaseComm.devicesRef
                                    .child(peerId)
                                    .child("inbox")
                                    .child("master_sync")
                                    .push()
                                inboxRef.setValue(peerPacket)
                                sentCount++
                            } catch (e: Exception) {
                                SystemLogger.w(TAG, "Failed to fan-out to peer $peerId: ${e.message}")
                            }
                        }
                    }
                    SystemLogger.i(TAG, "Simultaneously broadcast master data change to $sentCount connected peer devices.")
                }

            } catch (e: Exception) {
                SystemLogger.e(TAG, "Master data sync error: ${e.message}", e)
            }
        }
    }

    /**
     * 📥 Attaches real-time listener to receive master mutations from connected peer devices.
     */
    @JvmStatic
    fun startRealtimePeerSyncListener(context: Context, localAgentId: String) {
        if (!FirebaseComm.isAuthenticated() || !FirebaseComm.isAgentIdValid(localAgentId)) return
        if (activeListeners.containsKey(localAgentId)) return

        val appContext = context.applicationContext
        val inboxRef = FirebaseComm.devicesRef
            .child(localAgentId)
            .child("inbox")
            .child("master_sync")

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val senderId = snapshot.child("sender_agent_id").getValue(String::class.java) ?: ""
                    val category = snapshot.child("category").getValue(String::class.java) ?: ""
                    val changeType = snapshot.child("change_type").getValue(String::class.java) ?: ""
                    val entityId = snapshot.child("entity_id").getValue(String::class.java) ?: ""

                    SystemLogger.i(TAG, "Incoming master data change from peer $senderId: $category [$changeType] -> $entityId")

                    // Reconcile peer updates in local database cache
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = DatabaseHelper.getInstance(appContext)
                            if (category == CATEGORY_PROFILE && senderId.isNotEmpty()) {
                                // Refresh peer cached profile
                                FirebaseComm.devicesRef.child(senderId).child("profile").get().addOnSuccessListener { profSnap ->
                                    if (profSnap.exists()) {
                                        val name = profSnap.child("name").getValue(String::class.java)
                                            ?: profSnap.child("fullName").getValue(String::class.java) ?: "Member"
                                        val phone = profSnap.child("phone").getValue(String::class.java) ?: ""
                                        db.saveRosterNode(senderId, name, "MEMBER", "ACTIVE", false, true)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Peer data reconciliation note: ${e.message}")
                        }
                    }

                    // Broadcast event to UI components
                    val intent = Intent(ACTION_MASTER_DATA_UPDATED).apply {
                        putExtra(EXTRA_CATEGORY, category)
                        putExtra(EXTRA_CHANGE_TYPE, changeType)
                        putExtra(EXTRA_ENTITY_ID, entityId)
                        putExtra(EXTRA_SENDER_ID, senderId)
                        setPackage(appContext.packageName)
                    }
                    appContext.sendBroadcast(intent)

                    // Acknowledge and clean up inbox node
                    snapshot.ref.removeValue()

                } catch (e: Exception) {
                    SystemLogger.e(TAG, "Failed to process incoming peer master sync", e)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                SystemLogger.w(TAG, "Peer sync inbox listener cancelled: ${error.message}")
            }
        }

        inboxRef.addChildEventListener(listener)
        activeListeners[localAgentId] = listener
        SystemLogger.i(TAG, "Real-time master sync inbox listener registered for $localAgentId")
    }

    /**
     * 🛑 Detaches real-time listener when service stops.
     */
    @JvmStatic
    fun stopRealtimePeerSyncListener(localAgentId: String) {
        val listener = activeListeners.remove(localAgentId) ?: return
        try {
            FirebaseComm.devicesRef
                .child(localAgentId)
                .child("inbox")
                .child("master_sync")
                .removeEventListener(listener)
            SystemLogger.i(TAG, "Real-time master sync inbox listener detached for $localAgentId")
        } catch (e: Exception) {
            SystemLogger.w(TAG, "Error detaching peer sync listener: ${e.message}")
        }
    }

    /**
     * 🔄 Drains pending master data mutations queued in SQLite SyncQueueTable while device was offline.
     */
    @JvmStatic
    fun flushPendingMasterSyncQueue(context: Context) {
        val appContext = context.applicationContext
        if (!isNetworkAvailable(appContext) || !FirebaseComm.isAuthenticated()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseHelper.getInstance(appContext)
                val sqliteDb = db.readableDatabase
                val pendingList = mutableListOf<Triple<Int, String, String>>()

                sqliteDb.rawQuery(
                    "SELECT ${SyncQueueTable.COL_SYNC_ID}, ${SyncQueueTable.COL_ENTITY_TYPE}, ${SyncQueueTable.COL_ENTITY_ID} FROM ${SyncQueueTable.TABLE_NAME} WHERE ${SyncQueueTable.COL_STATUS} = 'PENDING' AND ${SyncQueueTable.COL_ENTITY_TYPE} LIKE 'MASTER_%' LIMIT 50",
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val syncId = cursor.getInt(0)
                        val entityType = cursor.getString(1)
                        val entityId = cursor.getString(2)
                        pendingList.add(Triple(syncId, entityType, entityId))
                    }
                }

                if (pendingList.isEmpty()) return@launch

                SystemLogger.i(TAG, "Draining ${pendingList.size} pending master sync mutations from offline queue.")

                for ((syncId, entityType, entityId) in pendingList) {
                    val category = entityType.removePrefix("MASTER_")
                    val payload = mutableMapOf<String, Any?>()

                    if (category == CATEGORY_PROFILE) {
                        try {
                            val profCursor = sqliteDb.query(
                                com.jdroidx.jfindme.data.local.tables.UserProfileTable.TABLE_NAME,
                                null, null, null, null, null, null, "1"
                            )
                            profCursor.use { c ->
                                if (c.moveToFirst()) {
                                    for (colName in c.columnNames) {
                                        payload[colName] = c.getString(c.getColumnIndexOrThrow(colName)) ?: ""
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Profile payload lookup note: ${e.message}")
                        }
                    } else if (category == CATEGORY_SAFE_ZONE) {
                        try {
                            val zoneCursor = sqliteDb.query(
                                com.jdroidx.jfindme.data.local.tables.SafeZoneTable.TABLE_NAME,
                                null,
                                "${com.jdroidx.jfindme.data.local.tables.SafeZoneTable.COL_ZONE_ID} = ?",
                                arrayOf(entityId),
                                null, null, null
                            )
                            zoneCursor.use { c ->
                                if (c.moveToFirst()) {
                                    for (colName in c.columnNames) {
                                        val idx = c.getColumnIndexOrThrow(colName)
                                        if (c.getType(idx) == android.database.Cursor.FIELD_TYPE_FLOAT) {
                                            payload[colName] = c.getDouble(idx)
                                        } else if (c.getType(idx) == android.database.Cursor.FIELD_TYPE_INTEGER) {
                                            payload[colName] = c.getLong(idx)
                                        } else {
                                            payload[colName] = c.getString(idx) ?: ""
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            SystemLogger.w(TAG, "Safe zone payload lookup note: ${e.message}")
                        }
                    }

                    notifyMasterDataChanged(appContext, category, TYPE_UPSERT, entityId, payload)
                    SyncQueueTable.markSuccess(db.writableDatabase, syncId)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error draining pending master sync queue: ${e.message}", e)
            }
        }
    }
}
