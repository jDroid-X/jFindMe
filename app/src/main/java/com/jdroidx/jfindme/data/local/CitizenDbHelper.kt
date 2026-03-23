package com.jdroidx.jfindme.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * CitizenDbHelper: Strictly offline-capable SQL Database for Citizen Dossiers.
 * Saves the expanded personal data natively onto the "personal table" as requested.
 */
class CitizenDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "JFindMe_CitizenData.db"
        private const val DATABASE_VERSION = 7 // 🚀 SCHEMA UPGRADE: V7 Medical Minimization Phase
        private const val TABLE_PERSONAL = "personal_table"
        private const val TABLE_ROSTER = "roster_table"
        private const val TABLE_SAFE_ZONES = "safezone_table"
        private const val TABLE_ALERTS = "alerts_table"

        // DB Columns mapped to the Citizen Form Arrays
        private const val COL_UID = "uid"
        private const val COL_NETWORK_ID = "network_id" // 🚀 Cross-Verification Hardware Link 1
        private const val COL_DEVICE_ID = "device_id"   // 🚀 Cross-Verification Hardware Link 2
        private const val COL_FULL_NAME = "full_name"
        private const val COL_DOB = "dob"
        private const val COL_GENDER = "gender"
        private const val COL_ADDRESS = "address"
        private const val COL_EMERGENCY_NOTE = "emergency_note"
        private const val COL_ROLE_FAMILY = "citizen_role_family"
        private const val COL_ROLE_FRIEND = "citizen_role_friend"
        private const val COL_SAFE_ZONE_NAME = "safe_zone_name"
        private const val COL_SAFE_ZONE_RADIUS = "safe_zone_radius"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = ("CREATE TABLE $TABLE_PERSONAL ("
                + "$COL_UID TEXT PRIMARY KEY,"
                + "$COL_NETWORK_ID TEXT,"
                + "$COL_DEVICE_ID TEXT,"
                + "$COL_FULL_NAME TEXT,"
                + "$COL_DOB TEXT,"
                + "$COL_GENDER TEXT,"
                + "$COL_ADDRESS TEXT,"
                + "$COL_EMERGENCY_NOTE TEXT,"
                + "$COL_SAFE_ZONE_NAME TEXT,"
                + "$COL_SAFE_ZONE_RADIUS TEXT,"
                + "$COL_ROLE_FAMILY TEXT,"
                + "$COL_ROLE_FRIEND TEXT)")
        db.execSQL(createTableQuery)
        
        db.execSQL("CREATE TABLE $TABLE_ROSTER (uid TEXT PRIMARY KEY, display_name TEXT, role_title TEXT, status_text TEXT, is_admin INTEGER, is_friend INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_SAFE_ZONES (zone_id TEXT PRIMARY KEY, name TEXT, target TEXT, lat REAL, lng REAL, radius REAL)")
        db.execSQL("CREATE TABLE $TABLE_ALERTS (alert_id TEXT PRIMARY KEY, timestamp INTEGER, title TEXT, message TEXT)")
        
        SystemLogger.i("CitizenDbHelper", "Offline Personal Table Schema allocated in local DB.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PERSONAL")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROSTER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAFE_ZONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALERTS")
        onCreate(db)
    }

    fun saveLocalDossier(uid: String, payload: Map<String, String>) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_UID, uid)
            put(COL_NETWORK_ID, payload["network_id"] ?: "")
            put(COL_DEVICE_ID, payload["device_id"] ?: "")
            put(COL_FULL_NAME, payload["fullName"] ?: "")
            put(COL_DOB, payload["dob"] ?: "")
            put(COL_GENDER, payload["gender"] ?: "")
            put(COL_ADDRESS, payload["address"] ?: "")
            put(COL_EMERGENCY_NOTE, payload["emergencyNote"] ?: "")
            put(COL_SAFE_ZONE_NAME, payload["safeZoneName"] ?: "")
            put(COL_SAFE_ZONE_RADIUS, payload["safeZoneRadius"] ?: "")
            put(COL_ROLE_FAMILY, payload["role_family"] ?: "")
            put(COL_ROLE_FRIEND, payload["role_friend"] ?: "")
        }
        
        // SQLite UPSERT (Update if exists, else Insert)
        val conflictId = db.insertWithOnConflict(TABLE_PERSONAL, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        SystemLogger.w("CitizenDbHelper", "Dossier secured in Local SQL DB under UID=$uid [RowID: $conflictId]")
        db.close()
    }

    fun getLocalDossier(uid: String): Map<String, String>? {
        val db = this.readableDatabase
        var result: Map<String, String>? = null
        
        db.query(TABLE_PERSONAL, null, "$COL_UID=?", arrayOf(uid), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                result = mapOf(
                    "network_id" to cursor.getString(cursor.getColumnIndexOrThrow(COL_NETWORK_ID)),
                    "device_id" to cursor.getString(cursor.getColumnIndexOrThrow(COL_DEVICE_ID)),
                    "fullName" to cursor.getString(cursor.getColumnIndexOrThrow(COL_FULL_NAME)),
                    "dob" to cursor.getString(cursor.getColumnIndexOrThrow(COL_DOB)),
                    "gender" to cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)),
                    "address" to cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDRESS)),
                    "emergencyNote" to cursor.getString(cursor.getColumnIndexOrThrow(COL_EMERGENCY_NOTE)),
                    "safeZoneName" to cursor.getString(cursor.getColumnIndexOrThrow(COL_SAFE_ZONE_NAME)),
                    "safeZoneRadius" to cursor.getString(cursor.getColumnIndexOrThrow(COL_SAFE_ZONE_RADIUS)),
                    "role_family" to cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLE_FAMILY)),
                    "role_friend" to cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLE_FRIEND))
                )
            }
        }
        
        db.close()
        return result
    }

    fun updateCitizenRole(uid: String, famRole: String, frRole: String) {
        // 🚀 PRIME DIRECTIVE: Surgical Upsert to protect Name/DOB but init empty arrays
        val existingDossier = getLocalDossier(uid)
        
        val db = this.writableDatabase
        val dValues = ContentValues().apply {
            put(COL_ROLE_FAMILY, famRole)
            put(COL_ROLE_FRIEND, frRole)
        }
        if (existingDossier != null && existingDossier.isNotEmpty()) {
            db.update(TABLE_PERSONAL, dValues, "$COL_UID=?", arrayOf(uid))
        } else {
            dValues.put(COL_UID, uid)
            dValues.put(COL_FULL_NAME, "Loading...") // Placeholder until FW Push
            db.insert(TABLE_PERSONAL, null, dValues)
        }
        
        val rTitle = if (frRole != "None") frRole else if (famRole != "None") famRole else "GUEST"
        val rValues = ContentValues().apply {
            put("role_title", rTitle.substringBefore("(").trim().uppercase()) 
        }
        val affected = db.update(TABLE_ROSTER, rValues, "uid=?", arrayOf(uid))
        if (affected == 0) {
            rValues.put("uid", uid)
            rValues.put("display_name", "Syncing...")
            db.insert(TABLE_ROSTER, null, rValues)
        }
        
        SystemLogger.i("CitizenDbHelper", "Role Cross-Synced natively inside SQLite for UID: $uid -> [$rTitle]")
        db.close()
    }

    // 🚀 NEW OFFLINE ROSTER ENGINE (Zero-Signal Safe)
    fun saveRosterNode(uid: String, name: String, role: String, status: String, isAdmin: Boolean, isFriend: Boolean) {
        val db = this.writableDatabase
        val v = ContentValues().apply {
            put("uid", uid)
            put("display_name", name)
            put("role_title", role)
            put("status_text", status)
            put("is_admin", if (isAdmin) 1 else 0)
            put("is_friend", if (isFriend) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_ROSTER, null, v, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getOfflineRoster(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        db.query(TABLE_ROSTER, null, null, null, null, null, null).use {
            while (it.moveToNext()) {
                list.add(mapOf(
                    "uid" to it.getString(0),
                    "display_name" to it.getString(1),
                    "role_title" to it.getString(2),
                    "status_text" to it.getString(3),
                    "is_admin" to (it.getInt(4) == 1),
                    "is_friend" to (it.getInt(5) == 1)
                ))
            }
        }
        db.close()
        return list
    }

    // 🚀 NEW OFFLINE GEOFENCE ENGINE (Zero-Signal Safe)
    fun saveSafeZone(zoneId: String, name: String, target: String, lat: Double, lng: Double, radius: Double) {
        val db = this.writableDatabase
        val v = ContentValues().apply {
            put("zone_id", zoneId)
            put("name", name)
            put("target", target)
            put("lat", lat)
            put("lng", lng)
            put("radius", radius)
        }
        db.insertWithOnConflict(TABLE_SAFE_ZONES, null, v, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getOfflineSafeZones(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        db.query(TABLE_SAFE_ZONES, null, null, null, null, null, null).use {
            while (it.moveToNext()) {
                list.add(mapOf(
                    "zone_id" to it.getString(0),
                    "name" to it.getString(1),
                    "target" to it.getString(2),
                    "lat" to it.getDouble(3),
                    "lng" to it.getDouble(4),
                    "radius" to it.getDouble(5)
                ))
            }
        }
        db.close()
        return list
    }

    // 🚀 NEW OFFLINE ALERTS ENGINE (Zero-Signal Safe)
    fun saveAlert(alertId: String, timestamp: Long, title: String, message: String) {
        val db = this.writableDatabase
        val v = ContentValues().apply {
            put("alert_id", alertId)
            put("timestamp", timestamp)
            put("title", title)
            put("message", message)
        }
        db.insertWithOnConflict("alerts_table", null, v, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getOfflineAlerts(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        db.query("alerts_table", null, null, null, null, null, "timestamp DESC").use {
            while (it.moveToNext()) {
                list.add(mapOf(
                    "alert_id" to it.getString(0),
                    "timestamp" to it.getLong(1),
                    "title" to it.getString(2),
                    "message" to it.getString(3)
                ))
            }
        }
        db.close()
        return list
    }
}
