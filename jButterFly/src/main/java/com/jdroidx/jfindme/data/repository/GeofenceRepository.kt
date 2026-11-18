package com.jdroidx.jfindme.data.repository

import android.content.Context
import com.jdroidx.jfindme.data.local.DatabaseHelper
import com.jdroidx.jfindme.data.local.tables.SafeZoneTable
import com.jdroidx.jfindme.data.model.SafeZoneModel
import com.jdroidx.jfindme.data.remote.FirebaseComm
import com.jdroidx.jfindme.utils.SystemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [Variables]: context, db, ioScope
 * [Constants]: TAG
 * [Dependencies]: DatabaseHelper, FirebaseComm, SafeZoneModel, SafeZoneTable
 * [Integration Points]: GeofenceActivity, GeofenceNotificationActivity, MapActivity
 *
 * 🚀 GeofenceRepository (OOPS-based MVC SSOT)
 * Centralizes all SafeZone boundary models, spatial coordinates, radius validation,
 * and geofence events. Replaces direct SQLite queries in GeofenceActivity and MapActivity.
 */
class GeofenceRepository private constructor(private val context: Context) {

    private val db = DatabaseHelper.getInstance(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "GeofenceRepository"

    companion object {
        @Volatile
        private var instance: GeofenceRepository? = null

        @JvmStatic
        fun getInstance(context: Context): GeofenceRepository {
            return instance ?: synchronized(this) {
                instance ?: GeofenceRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Resolves SafeZone by zone ID from local SQLite SSOT.
     */
    fun getSafeZoneById(zoneId: String): SafeZoneModel? {
        return db.getSafeZoneById(zoneId)
    }

    /**
     * Resolves display label for a safe zone.
     */
    fun getSafeZoneDisplayName(zoneId: String): String {
        return db.getSafeZoneDisplayName(zoneId)
    }

    /**
     * Retrieves creator agent ID for authorization checks.
     */
    fun getSafeZoneCreatorAgentId(zoneId: String): String? {
        return db.getSafeZoneCreatorAgentId(zoneId)
    }

    /**
     * Persists or updates a SafeZone boundary model.
     * Synchronizes to both SQLite and Firebase Realtime Database.
     */
    fun saveSafeZone(zone: SafeZoneModel, onComplete: (Boolean) -> Unit) {
        if (zone.zoneId.isBlank()) {
            onComplete(false)
            return
        }

        ioScope.launch {
            try {
                // 1. Persist to local SQLite
                val writableDb = db.writableDatabase
                val values = android.content.ContentValues().apply {
                    put(SafeZoneTable.COL_ZONE_ID, zone.zoneId)
                    put(SafeZoneTable.COL_NAME, zone.name)
                    put(SafeZoneTable.COL_LAT, zone.lat)
                    put(SafeZoneTable.COL_LNG, zone.lng)
                    put(SafeZoneTable.COL_RADIUS, zone.radius)
                    put(SafeZoneTable.COL_IS_ACTIVE, if (zone.isZoneActive) 1 else 0)
                    put(SafeZoneTable.COL_IS_ALERTS_ENABLED, if (zone.alertsEnabled) 1 else 0)
                    put(SafeZoneTable.COL_UPDATED_AT, FirebaseComm.getForensicTimestamp())
                }
                writableDb.insertWithOnConflict(
                    SafeZoneTable.TABLE_NAME,
                    null,
                    values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )

                // 2. Synchronize to Firebase
                val selfUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
                if (selfUid != null) {
                    FirebaseComm.database.getReference("safe_zones")
                        .child(selfUid)
                        .child(zone.zoneId)
                        .setValue(zone)
                }

                SystemLogger.i(TAG, "Saved SafeZone: ${zone.name} (${zone.zoneId})")
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error saving SafeZone: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    /**
     * Soft-deletes or archives a safe zone.
     */
    fun archiveSafeZone(zoneId: String, onComplete: (Boolean) -> Unit) {
        ioScope.launch {
            try {
                val writableDb = db.writableDatabase
                val values = android.content.ContentValues().apply {
                    put(SafeZoneTable.COL_IS_ARCHIVED, 1)
                    put(SafeZoneTable.COL_IS_ACTIVE, 0)
                    put(SafeZoneTable.COL_UPDATED_AT, System.currentTimeMillis())
                }
                writableDb.update(
                    SafeZoneTable.TABLE_NAME,
                    values,
                    "${SafeZoneTable.COL_ZONE_ID} = ?",
                    arrayOf(zoneId)
                )
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Error archiving SafeZone: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    /**
     * Permanently purges an archived SafeZone.
     */
    fun purgeSafeZone(zoneId: String): Boolean {
        return db.purgeArchivedSafeZone(zoneId)
    }
}
