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
        @Volatile
        private var instance: DeviceRepository? = null

        @JvmStatic
        fun getInstance(context: Context): DeviceRepository {
            return instance ?: synchronized(this) {
                instance ?: DeviceRepository(context.applicationContext).also { instance = it }
            }
        }
    }

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

    /**
     * Master Administrative Purge: Permanently wipes corrupted or orphaned nodes.
     */
    fun purgeDeviceRecords(targetAgentId: String, onComplete: (Boolean) -> Unit) {
        ioScope.launch {
            try {
                val writableDb = db.writableDatabase
                writableDb.beginTransaction()
                try {
                    writableDb.delete(
                        com.jdroidx.jfindme.data.local.tables.UserIdentityTable.TABLE_NAME,
                        "${com.jdroidx.jfindme.data.local.tables.UserIdentityTable.COL_UID_1} = ? OR ${com.jdroidx.jfindme.data.local.tables.UserIdentityTable.COL_AUTH_UID} = ?",
                        arrayOf(targetAgentId, targetAgentId)
                    )
                    writableDb.delete(
                        com.jdroidx.jfindme.data.local.tables.DeviceTable.TABLE_NAME,
                        "${com.jdroidx.jfindme.data.local.tables.DeviceTable.COL_HARDWARE_ID} = ? OR ${com.jdroidx.jfindme.data.local.tables.DeviceTable.COL_IDENTITY_ID} = ?",
                        arrayOf(targetAgentId, targetAgentId)
                    )
                    writableDb.setTransactionSuccessful()
                } finally {
                    writableDb.endTransaction()
                }
                SystemLogger.i(TAG, "Purged all records for target agent: $targetAgentId")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error purging device records: ${e.message}")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }
}
