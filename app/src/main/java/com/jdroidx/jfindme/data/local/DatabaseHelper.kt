package com.jdroidx.jfindme.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jdroidx.jfindme.utils.SystemLogger
import java.text.SimpleDateFormat
import java.util.*

/**
 * Citizen Database Engine (L7 Global Architect)
 * V38 FINAL PRODUCTION RECONSTRUCTION: Forensic Schema Hardening (Zero-Gap Wipe).
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "JFindMe_CitizenData.db"
        private const val DATABASE_VERSION = 40 // 🚀 V40: Forensic Version Hardening & Downgrade Recovery
        private const val MAX_DATETIME = 253402300799000L
        
        private const val TABLE_PERSONAL = "personal_table"
        private const val TABLE_ROSTER = "roster_table"
        private const val TABLE_ORG = "org_table"
        private const val TABLE_SAFE_ZONES = "SafeZone_Table" 
        private const val TABLE_ALERTS = "GeoFence_Alert_table"
        private const val TABLE_HISTORY = "history_table" 
        private const val TABLE_PAIRING = "pairing_log" 
        private const val TABLE_SIM_INVENTORY = "sim_inventory" 
        private const val TABLE_SIM_TRANS = "sim_transaction" 
        private const val TABLE_SYNC_QUEUE = "geofenc_sync_queue_table" // 🚀 Reliable Sync Engine

        private const val COL_FLAG_A = "flag_a"
        private const val COL_FLAG_B = "flag_b"
        private const val COL_FLAG_C = "flag_c"
        private const val COL_REF_A = "ref_a"
        private const val COL_REF_B = "ref_b"
        private const val COL_REF_C = "ref_c"
        private const val EXT_FIELDS = "$COL_FLAG_A INTEGER, $COL_FLAG_B INTEGER, $COL_FLAG_C INTEGER, $COL_REF_A TEXT, $COL_REF_B TEXT, $COL_REF_C TEXT"
        private const val LINEAGE_FIELDS = "owner_name TEXT, owner_device_id TEXT, connected_device_id TEXT, qr_code_ref TEXT"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_PERSONAL (log_id TEXT PRIMARY KEY, uid TEXT, network_id TEXT, device_id TEXT, full_name TEXT, user_phone TEXT, user_address TEXT, user_dob TEXT, user_gender TEXT, user_country TEXT, user_state TEXT, user_city TEXT, user_pincode TEXT, user_emergency_note TEXT, role_family TEXT, role_friend TEXT, start_datetime INTEGER, end_datetime INTEGER DEFAULT $MAX_DATETIME, created_at INTEGER, modified_at INTEGER, readable_time TEXT, $EXT_FIELDS, $LINEAGE_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ROSTER (log_id TEXT PRIMARY KEY, uid TEXT, display_name TEXT, role_title TEXT, status_text TEXT, start_datetime INTEGER, end_datetime INTEGER DEFAULT $MAX_DATETIME, is_admin INTEGER DEFAULT 0, is_friend INTEGER DEFAULT 1, created_at INTEGER, modified_at INTEGER, readable_time TEXT, $EXT_FIELDS, $LINEAGE_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ORG (log_id TEXT PRIMARY KEY, uid TEXT, org_name TEXT, department TEXT, employee_id TEXT, designation TEXT, supervisor TEXT, shift_timing TEXT, work_location TEXT, badge_number TEXT, emergency_ext TEXT, access_level TEXT, start_datetime INTEGER, end_datetime INTEGER DEFAULT $MAX_DATETIME, created_at INTEGER, modified_at INTEGER, readable_time TEXT, $EXT_FIELDS, $LINEAGE_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SAFE_ZONES (zone_id TEXT PRIMARY KEY, name TEXT, target TEXT, uid TEXT, lat REAL, lng REAL, radius REAL, assigned_uids TEXT, sync_status TEXT DEFAULT 'LOCAL', created_at INTEGER, modified_at INTEGER, readable_time TEXT, $EXT_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ALERTS (alert_id TEXT PRIMARY KEY, uid TEXT, timestamp INTEGER, title TEXT, message TEXT, readable_time TEXT, $EXT_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (log_id TEXT PRIMARY KEY, uid TEXT, profile_name TEXT, relationship TEXT, timestamp INTEGER, from_datetime INTEGER, to_datetime INTEGER, ping_count INTEGER DEFAULT 1, lat REAL, lng REAL, speed REAL, battery INTEGER, sos_active INTEGER, address TEXT, modified_at INTEGER, modified_reason TEXT, readable_time TEXT, $EXT_FIELDS, $LINEAGE_FIELDS)")
        
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_PAIRING (" +
                "pairing_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "device_a_uid TEXT, device_a_name TEXT, device_a_role TEXT, device_a_id TEXT, " +
                "device_b_uid TEXT, device_b_name TEXT, device_b_role TEXT, device_b_id TEXT, " +
                "qr_code TEXT, is_active INTEGER DEFAULT 1, is_blocked INTEGER DEFAULT 0, is_handshake_finalized INTEGER DEFAULT 1, " +
                "flag_a INTEGER DEFAULT 0, flag_b INTEGER DEFAULT 0, flag_c INTEGER DEFAULT 0, " +
                "ref_a TEXT, ref_b TEXT, ref_c TEXT, " +
                "created_at INTEGER, modified_at INTEGER, readable_time TEXT)")

        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SIM_INVENTORY (" +
                "sim_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "slot_index INTEGER, " +
                "sim_serial TEXT, " +
                "operator_name TEXT, " +
                "mcc TEXT, mnc TEXT, lac TEXT, cid TEXT, " +
                "is_active INTEGER DEFAULT 1, " +
                "is_esim INTEGER DEFAULT 0, " +
                "last_sync_at INTEGER DEFAULT 0, " +
                "created_at INTEGER, " +
                "modified_at INTEGER, " +
                "readable_time TEXT, " +
                "$EXT_FIELDS)")

        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SIM_TRANS (" +
                "trans_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "slot_index INTEGER, " +
                "mcc TEXT, mnc TEXT, lac TEXT, cid TEXT, " +
                "lat REAL, lng REAL, address TEXT, " +
                "timestamp INTEGER, " +
                "readable_time TEXT, " +
                "$EXT_FIELDS)")

        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SYNC_QUEUE (" +
                "sync_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "payload_type TEXT, payload_id TEXT, destination_uid TEXT, " +
                "retry_count INTEGER DEFAULT 0, status TEXT DEFAULT 'PENDING', " +
                "acked_at INTEGER DEFAULT 0, created_at INTEGER, " +
                "readable_time TEXT, " +
                "flag_a INTEGER DEFAULT 0, flag_b INTEGER DEFAULT 0, flag_c INTEGER DEFAULT 0, " +
                "ref_a TEXT, ref_b TEXT, ref_c TEXT)")
        
        seedDefaultSafeZones(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PERSONAL")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROSTER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ORG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAFE_ZONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALERTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PAIRING")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SIM_INVENTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SIM_TRANS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SYNC_QUEUE")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    
    fun factoryReset() {
        onUpgrade(this.writableDatabase, 0, DATABASE_VERSION)
    }

    // 🚀 MASTER BRIDGE SUITE (V38 FINAL)

    fun getReadableNow(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun getActiveProfileAddress(): String {
        return getLocalDossier("OWNER_001")?.get("user_address") ?: "Unknown Location"
    }

    fun saveLocalDossier(uid: String, payload: Map<String, String>, role: String? = null, owner: String? = null, devId: String? = null, connId: String? = null, qRef: String? = null) {
        val v = ContentValues().apply {
            put("uid", uid)
            put("full_name", payload["fullName"] ?: "")
            put("user_phone", payload["phone"] ?: "")
            put("user_dob", payload["dob"] ?: "")
            put("user_gender", payload["gender"] ?: "")
            put("user_address", payload["address"] ?: "")
            put("user_country", payload["country"] ?: "")
            put("user_state", payload["state"] ?: "")
            put("user_city", payload["city"] ?: "")
            put("user_pincode", payload["pincode"] ?: "")
            put("user_emergency_note", payload["emergencyNote"] ?: "")
            
            val normalizedRole = role ?: "PEER"
            if (normalizedRole.equals("Friends", ignoreCase = true)) {
                put("role_family", "None")
                put("role_friend", normalizedRole)
            } else {
                put("role_family", normalizedRole)
                put("role_friend", "None")
            }

            put("network_id", payload["network_id"] ?: "")
            put("owner_name", owner ?: "N/A")
            put("owner_device_id", devId ?: "N/A")
            put("connected_device_id", connId ?: "N/A")
            put("qr_code_ref", qRef ?: "N/A")
            put("readable_time", getReadableNow())
        }
        savePersonalDossier(v)
    }

    /**
     * 🚀 Citizen Role Synchronizer: Syncs role titles across management hubs.
     */
    fun updateCitizenRole(uid: String, fam: String, fri: String, isFriend: Boolean) {
        val v = ContentValues().apply { 
            put("uid", uid)
            put("role_title", if(fam != "None") fam else fri)
            put("is_friend", if(isFriend) 1 else 0)
            put("readable_time", getReadableNow())
        }
        saveRosterNode(v)
    }

    fun getOfflineRoster(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        db.query(TABLE_ROSTER, null, "end_datetime=?", arrayOf(MAX_DATETIME.toString()), null, null, "display_name ASC").use { cursor ->
            while (cursor.moveToNext()) {
                val row = mutableMapOf<String, Any>()
                for (i in 0 until cursor.columnCount) {
                    val name = cursor.getColumnName(i)
                    if (name == "is_admin" || name == "is_friend") {
                        row[name] = cursor.getInt(i) == 1
                    } else {
                        row[name] = cursor.getString(i) ?: ""
                    }
                }
                list.add(row)
            }
        }
        return list
    }

    fun saveRosterNode(uid: String?, name: String?, role: String?, status: String?, isAdmin: Boolean = false, isFriend: Boolean = true, ownerName: String? = null, ownerDeviceId: String? = null, connectedDeviceId: String? = null, qrCode: String? = null) {
        val v = ContentValues().apply {
            put("log_id", uid ?: UUID.randomUUID().toString())
            put("uid", uid ?: "n/a")
            put("display_name", name ?: "Unknown Device")
            put("role_title", role ?: "PEER")
            put("status_text", status ?: "Active")
            put("is_admin", if(isAdmin) 1 else 0)
            put("is_friend", if(isFriend) 1 else 0)
            put("owner_name", ownerName ?: "N/A")
            put("owner_device_id", ownerDeviceId ?: "N/A")
            put("connected_device_id", connectedDeviceId ?: "N/A")
            put("qr_code_ref", qrCode ?: "N/A")
            put("created_at", System.currentTimeMillis())
            put("modified_at", System.currentTimeMillis())
            put("readable_time", getReadableNow())
        }
        this.writableDatabase.insertWithOnConflict(TABLE_ROSTER, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun ensureDefaultZones() {
        seedDefaultSafeZones(this.writableDatabase)
    }

    private fun seedDefaultSafeZones(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val readable = getReadableNow()
        val zones = listOf(
            arrayOf("HOME_001", "Home Sanctuary", 0.0, 0.0),
            arrayOf("SCHOOL_001", "Education Hub", 0.01, 0.01)
        )
        for (z in zones) {
            val v = ContentValues().apply {
                put("zone_id", z[0] as String)
                put("name", z[1] as String)
                put("target", "ALL_TARGETS")
                put("uid", "GLOBAL")
                put("lat", z[2] as Double)
                put("lng", z[3] as Double)
                put("radius", 500.0)
                put("created_at", now)
                put("modified_at", now)
                put("readable_time", readable)
                put(COL_FLAG_A, 1)
                put(COL_REF_A, "SYSTEM_DEFAULT")
            }
            db.insertWithOnConflict(TABLE_SAFE_ZONES, null, v, SQLiteDatabase.CONFLICT_IGNORE)
        }
        SystemLogger.i("DatabaseHelper", "Forensic Safe Zones Seeding Completed.")
    }

    fun saveRosterNode(v: ContentValues) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val uid = v.getAsString("uid") ?: return
        
        val vUpd = ContentValues().apply { 
            put("end_datetime", now)
            put("modified_at", now)
            put("readable_time", getReadableNow())
        }
        db.update(TABLE_ROSTER, vUpd, "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()))
        
        val log = "R_${uid}_$now"
        v.put("log_id", log)
        v.put("uid", uid)
        v.put("start_datetime", now + 1)
        v.put("end_datetime", MAX_DATETIME)
        v.put("created_at", now)
        v.put("modified_at", now)
        v.put("readable_time", getReadableNow())
        db.insert(TABLE_ROSTER, null, v)
    }

    fun savePersonalDossier(v: ContentValues) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val uid = v.getAsString("uid") ?: "OWNER_001"
        v.put("readable_time", getReadableNow())

        if (!uid.equals("OWNER_001")) {
            val rc = db.query(TABLE_PERSONAL, arrayOf("log_id"), "uid=? AND end_datetime=?", arrayOf("OWNER_001", MAX_DATETIME.toString()), null, null, null)
            if (rc.moveToFirst()) {
                val upgrade = ContentValues().apply {
                    put("uid", uid)
                    put("modified_at", now)
                    put("readable_time", getReadableNow())
                }
                db.update(TABLE_PERSONAL, upgrade, "log_id=?", arrayOf(rc.getString(0)))
            }
            rc.close()
        }

        val vUpd = ContentValues().apply { 
            put("end_datetime", now)
            put("modified_at", now)
            put("readable_time", getReadableNow())
        }
        db.update(TABLE_PERSONAL, vUpd, "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()))
        
        val log = "${uid}_$now"
        v.put("log_id", log)
        v.put("uid", uid)
        v.put("start_datetime", now + 1)
        v.put("end_datetime", MAX_DATETIME)
        v.put("created_at", now)
        v.put("modified_at", now)
        db.insert(TABLE_PERSONAL, null, v)
    }

    fun getLocalDossier(uid: String? = null): Map<String, String>? {
        val db = this.readableDatabase
        val sel = if(uid != null) "uid=? AND end_datetime=?" else "end_datetime=?"
        val args = if(uid != null) arrayOf(uid, MAX_DATETIME.toString()) else arrayOf(MAX_DATETIME.toString())
        db.query(TABLE_PERSONAL, null, sel, args, null, null, null).use {
            if (it.moveToFirst()) {
                val map = mutableMapOf<String, String>()
                for (i in 0 until it.columnCount) {
                    map[it.getColumnName(i)] = it.getString(i) ?: ""
                }
                return map
            }
        }
        return null
    }

    fun saveHistoryPoint(uid: String, name: String, r: String, lat: Double, lng: Double, spd: Float, b: Int, sos: Boolean, addr: String?, ts: Long? = null) {
        val db = this.writableDatabase
        val now = ts ?: System.currentTimeMillis()
        val readable = if (ts != null) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))
        } else {
            getReadableNow()
        }
        
        // 🚀 QA Restoration: Aggressive Clubbing Matrix (V40 Standard)
        // Rule: If pings are within 1 hour and 20 meters, consolidate into a single session.
        db.query(TABLE_HISTORY, null, "uid=? AND ? BETWEEN (from_datetime - 3600000) AND (to_datetime + 3600000)", arrayOf(uid, now.toString()), null, null, "to_datetime DESC", "1").use { c ->
            if (c.moveToFirst()) {
                val lastLogId = c.getString(c.getColumnIndexOrThrow("log_id"))
                val lastLat = c.getDouble(c.getColumnIndexOrThrow("lat"))
                val lastLng = c.getDouble(c.getColumnIndexOrThrow("lng"))
                val d = com.jdroidx.jfindme.engine.data.SpatialProcessor.getDistance(lat, lng, lastLat, lastLng)
                
                // Allow a 60m spatial radius for aggressive grouping (Recalibrated for GPS Drift)
                if (d < 60.0) {
                    val from = c.getLong(c.getColumnIndexOrThrow("from_datetime"))
                    val to = c.getLong(c.getColumnIndexOrThrow("to_datetime"))
                    
                    val v = ContentValues().apply {
                        if (now < from) put("from_datetime", now)
                        if (now > to) put("to_datetime", now)
                        put("ping_count", c.getInt(c.getColumnIndexOrThrow("ping_count")) + 1)
                        put("modified_at", System.currentTimeMillis())
                        // Update address if it was blank (Resolution Loop)
                        if (addr != null && (c.getString(c.getColumnIndexOrThrow("address"))?.length ?: 0) < 5) {
                            put("address", addr)
                        }
                    }
                    db.update(TABLE_HISTORY, v, "log_id=?", arrayOf(lastLogId))
                    return
                }
            }
        }
        val v = ContentValues().apply {
            put("log_id", "${uid}_$now")
            put("uid", uid)
            put("profile_name", name)
            put("relationship", r)
            put("timestamp", now)
            put("from_datetime", now)
            put("to_datetime", now)
            put("ping_count", 1)
            put("lat", lat)
            put("lng", lng)
            put("speed", spd)
            put("battery", b)
            put("sos_active", if (sos) 1 else 0)
            put("address", addr ?: "Tracing...")
            put("modified_at", System.currentTimeMillis())
            put("readable_time", readable)
        }
        // 🚀 QA Fix: Use CONFLICT_IGNORE for forensic idempotency.
        // This prevents primary key crashes during redundant cloud syncs.
        db.insertWithOnConflict(TABLE_HISTORY, null, v, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun saveAlert(uid: String, title: String, message: String) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("alert_id", "ALRT_$now")
            put("uid", uid)
            put("timestamp", now)
            put("title", title)
            put("message", message)
            put(COL_FLAG_A, 1)
            put("readable_time", getReadableNow())
        }
        db.insert(TABLE_ALERTS, null, v)
    }

    fun saveOrgDossier(v: ContentValues) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val uid = v.getAsString("uid") ?: "OWNER_001"
        
        val vUpd = ContentValues().apply { 
            put("end_datetime", now)
            put("modified_at", now)
            put("readable_time", getReadableNow())
        }
        db.update(TABLE_ORG, vUpd, "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()))
        
        val log = "O_${uid}_$now"
        v.put("log_id", log)
        v.put("uid", uid)
        v.put("start_datetime", now + 1)
        v.put("end_datetime", MAX_DATETIME)
        v.put("created_at", now)
        v.put("modified_at", now)
        v.put("readable_time", getReadableNow())
        db.insert(TABLE_ORG, null, v)
    }

    fun saveSafeZone(v: ContentValues) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val zId = v.getAsString("zone_id") ?: "Z_$now"
        v.put("readable_time", getReadableNow())
        
        db.query(TABLE_SAFE_ZONES, arrayOf("zone_id"), "zone_id=?", arrayOf(zId), null, null, null).use {
            if (it.moveToFirst()) {
                v.put("modified_at", now)
                db.update(TABLE_SAFE_ZONES, v, "zone_id=?", arrayOf(zId))
            } else {
                v.put("created_at", now)
                v.put("modified_at", now)
                db.insert(TABLE_SAFE_ZONES, null, v)
            }
        }
    }

    fun saveSafeZoneAssignment(zoneId: String, uids: String) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("assigned_uids", uids)
            put("sync_status", "PENDING")
            put("modified_at", now)
            put("readable_time", getReadableNow())
        }
        db.update(TABLE_SAFE_ZONES, v, "zone_id=?", arrayOf(zoneId))
        
        uids.split(",").forEach { uid ->
            if (uid.isNotBlank()) queueHandshake("GEOFENCE_ADD", zoneId, uid)
        }
    }

    fun queueHandshake(type: String, pId: String, dest: String) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("payload_type", type)
            put("payload_id", pId)
            put("destination_uid", dest)
            put("created_at", now)
            put("readable_time", getReadableNow())
        }
        db.insert(TABLE_SYNC_QUEUE, null, v)
    }

    fun updateHistoryAddress(logId: String, address: String) {
        val db = this.writableDatabase
        val v = android.content.ContentValues().apply {
            put("address", address)
            put("modified_at", System.currentTimeMillis())
            put("readable_time", getReadableNow())
        }
        db.update(TABLE_HISTORY, v, "log_id=?", arrayOf(logId))
    }

    fun getHistoryLogsFiltered(uid: String, cutoffTime: Long): List<Map<String, Any>> {
        val rawList = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        
        // Step 1: Scavenge raw segments within the time window
        db.query(TABLE_HISTORY, null, "uid=? AND to_datetime >= ?", arrayOf(uid, cutoffTime.toString()), null, null, "to_datetime DESC").use { c ->
            while (c.moveToNext()) {
                val row = mutableMapOf<String, Any>()
                row["log_id"] = c.getString(c.getColumnIndexOrThrow("log_id")) ?: ""
                row["uid"] = c.getString(c.getColumnIndexOrThrow("uid")) ?: ""
                row["profile_name"] = c.getString(c.getColumnIndexOrThrow("profile_name")) ?: ""
                row["relationship"] = c.getString(c.getColumnIndexOrThrow("relationship")) ?: ""
                row["from_datetime"] = c.getLong(c.getColumnIndexOrThrow("from_datetime"))
                row["to_datetime"] = c.getLong(c.getColumnIndexOrThrow("to_datetime"))
                row["ping_count"] = c.getInt(c.getColumnIndexOrThrow("ping_count"))
                row["lat"] = c.getDouble(c.getColumnIndexOrThrow("lat"))
                row["lng"] = c.getDouble(c.getColumnIndexOrThrow("lng"))
                row["speed"] = c.getFloat(c.getColumnIndexOrThrow("speed"))
                row["battery"] = c.getInt(c.getColumnIndexOrThrow("battery"))
                row["sos_active"] = c.getInt(c.getColumnIndexOrThrow("sos_active")) == 1
                row["address"] = c.getString(c.getColumnIndexOrThrow("address")) ?: ""
                rawList.add(row)
            }
        }
        
        if (rawList.isEmpty()) return rawList

        // Step 2: 🚀 Late-Binding Aggressive Consolidation (Rule 6)
        val consolidated = mutableListOf<Map<String, Any>>()
        var current: MutableMap<String, Any>? = null

        for (row in rawList) {
            if (current == null) {
                current = row.toMutableMap()
            } else {
                val lat = row["lat"] as? Double ?: 0.0
                val lng = row["lng"] as? Double ?: 0.0
                val cLat = current["lat"] as? Double ?: 0.0
                val cLng = current["lng"] as? Double ?: 0.0
                
                val dist = com.jdroidx.jfindme.engine.data.SpatialProcessor.getDistance(lat, lng, cLat, cLng)
                
                if (dist < 60.0) {
                    // Club them! (Expand the time range and sum pings)
                    val from = row["from_datetime"] as Long
                    val currentFrom = current["from_datetime"] as Long
                    
                    if (from < currentFrom) current["from_datetime"] = from
                    current["ping_count"] = (current["ping_count"] as Int) + (row["ping_count"] as Int)
                    
                    // Keep most recent metadata for the clubbed tile (With Null Safety)
                    current["speed"] = row["speed"] ?: 0.0f
                    current["battery"] = row["battery"] ?: 0
                } else {
                    consolidated.add(current)
                    current = row.toMutableMap()
                }
            }
        }
        if (current != null) consolidated.add(current)
        
        return consolidated
    }

    fun getHistoryLogs(uid: String): List<Map<String, Any>> {
        // Redirection to the Filtered engine with a 0-offset (Full History Extract)
        return getHistoryLogsFiltered(uid, 0L)
    }

    fun getPendingSyncs(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        db.query(TABLE_SYNC_QUEUE, null, "status='PENDING'", null, null, null, "created_at ASC").use { c ->
            while (c.moveToNext()) {
                val row = mutableMapOf<String, Any>()
                row["sync_id"] = c.getInt(c.getColumnIndexOrThrow("sync_id"))
                row["payload_type"] = c.getString(c.getColumnIndexOrThrow("payload_type")) ?: ""
                row["payload_id"] = c.getString(c.getColumnIndexOrThrow("payload_id")) ?: ""
                row["destination_uid"] = c.getString(c.getColumnIndexOrThrow("destination_uid")) ?: ""
                list.add(row)
            }
        }
        return list
    }

    fun markSyncAcked(syncId: Int) {
        val v = ContentValues().apply {
            put("status", "ACKED")
            put("acked_at", System.currentTimeMillis())
        }
        this.writableDatabase.update(TABLE_SYNC_QUEUE, v, "sync_id=?", arrayOf(syncId.toString()))
    }

    fun getSafeZone(zoneId: String): Map<String, Any>? {
        val db = this.readableDatabase
        db.query(TABLE_SAFE_ZONES, null, "zone_id=?", arrayOf(zoneId), null, null, null).use { c ->
            if (c.moveToFirst()) {
                val row = mutableMapOf<String, Any>()
                row["zone_id"] = c.getString(c.getColumnIndexOrThrow("zone_id")) ?: ""
                row["name"] = c.getString(c.getColumnIndexOrThrow("name")) ?: ""
                row["lat"] = c.getDouble(c.getColumnIndexOrThrow("lat"))
                row["lng"] = c.getDouble(c.getColumnIndexOrThrow("lng"))
                row["radius"] = c.getDouble(c.getColumnIndexOrThrow("radius"))
                row["assigned_uids"] = c.getString(c.getColumnIndexOrThrow("assigned_uids")) ?: ""
                return row
            }
        }
        return null
    }

    fun getOfflineSafeZones(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        db.query(TABLE_SAFE_ZONES, null, null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                val row = mutableMapOf<String, Any>()
                row["zone_id"] = cursor.getString(cursor.getColumnIndexOrThrow("zone_id")) ?: ""
                row["lat"] = cursor.getDouble(cursor.getColumnIndexOrThrow("lat"))
                row["lng"] = cursor.getDouble(cursor.getColumnIndexOrThrow("lng"))
                row["radius"] = cursor.getDouble(cursor.getColumnIndexOrThrow("radius"))
                row["target"] = cursor.getString(cursor.getColumnIndexOrThrow("target")) ?: "All"
                list.add(row)
            }
        }
        return list
    }

    fun truncateTable(t: String) {
        this.writableDatabase.delete(t, null, null)
    }

    fun getRecordCount(t: String): Long {
        return try { android.database.DatabaseUtils.queryNumEntries(this.readableDatabase, t) } catch (e: Exception) { 0L }
    }

    fun getTableColumns(t: String): List<String> {
        val cols = mutableListOf<String>()
        val db = this.readableDatabase
        db.rawQuery("PRAGMA table_info($t)", null).use { c ->
            if (c.moveToFirst()) {
                do {
                    cols.add(c.getString(1))
                } while (c.moveToNext())
            }
        }
        if (cols.contains("uid") && t != TABLE_PERSONAL) cols.add(0, "IDENTITY_RESOLVED")
        return cols
    }

    fun getPagedData(t: String, l: Int, o: Int, q: String?): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = this.readableDatabase
        val rc = getTableColumns(t).toMutableList()
        if(rc.contains("IDENTITY_RESOLVED")) rc.removeAt(0)
        
        val hasUid = rc.contains("uid") && t != TABLE_PERSONAL
        val sel = if (hasUid) "t.*, p.full_name as IDENTITY_RESOLVED" else "*"
        val join = if (hasUid) "$t t LEFT JOIN $TABLE_PERSONAL p ON t.uid = p.uid AND p.end_datetime = $MAX_DATETIME" else "$t t"
        val whr = if (q.isNullOrEmpty()) "" else "WHERE " + rc.joinToString(" OR ") { "t.$it LIKE '%$q%'" }
        
        db.rawQuery("SELECT $sel FROM $join $whr LIMIT $l OFFSET $o", null).use { cursor ->
            val dc = getTableColumns(t)
            while (cursor.moveToNext()) {
                val row = mutableMapOf<String, String>()
                for (cn in dc) {
                    val ix = cursor.getColumnIndex(cn)
                    row[cn] = if (ix != -1) cursor.getString(ix) ?: "---" else "---"
                }
                list.add(row)
            }
        }
        return list
    }

    fun saveDossierLineage(uid: String, owner: String, devId: String, connId: String, qr: String) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("owner_name", owner)
            put("owner_device_id", devId)
            put("connected_device_id", connId)
            put("qr_code_ref", qr)
            put("modified_at", now)
            put("readable_time", getReadableNow())
        }
        db.update(TABLE_PERSONAL, v, "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()))
    }

    fun getDossierHistory(uid: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        this.readableDatabase.query(TABLE_PERSONAL, null, "uid=?", arrayOf(uid), null, null, "start_datetime DESC").use { c ->
            while (c.moveToNext()) {
                val row = mutableMapOf<String, String>()
                for (i in 0 until c.columnCount) row[c.getColumnName(i)] = c.getString(i) ?: ""
                list.add(row)
            }
        }
        return list
    }

    fun saveEnterpriseDossier(v: ContentValues) {
        saveOrgDossier(v)
    }

    fun getEnterpriseDossier(uid: String): Map<String, String>? {
        val db = this.readableDatabase
        db.query(TABLE_ORG, null, "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()), null, null, null).use {
            if (it.moveToFirst()) {
                val map = mutableMapOf<String, String>()
                for (i in 0 until it.columnCount) map[it.getColumnName(i)] = it.getString(i) ?: ""
                return map
            }
        }
        return null
    }

    fun getDossierLineage(uid: String): Map<String, String>? {
        val map = getLocalDossier(uid)
        return map?.filterKeys { it in listOf("owner_name", "owner_device_id", "connected_device_id", "qr_code_ref") }
    }

    fun logDevicePairing(deviceAId: String, deviceAName: String, deviceBId: String, deviceBName: String, deviceBRole: String, deviceARole: String? = null, devA_hwId: String? = null, devB_hwId: String? = null, qr: String? = null, flag1: Int = 0, flag2: Int = 0, flag3: Int = 0, ref1: String? = null, ref2: String? = null, ref3: String? = null) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val readable = getReadableNow()
        val cursor = db.query(TABLE_PAIRING, arrayOf("pairing_id"), "device_a_uid=? AND device_b_uid=?", arrayOf(deviceAId, deviceBId), null, null, null)
        val exists = cursor.moveToFirst()
        cursor.close()
        
        val v = ContentValues().apply {
            put("device_a_uid", deviceAId); put("device_a_name", deviceAName); put("device_a_role", deviceARole ?: "Unknown"); put("device_a_id", devA_hwId ?: "N/A")
            put("device_b_uid", deviceBId); put("device_b_name", deviceBName); put("device_b_role", deviceBRole); put("device_b_id", devB_hwId ?: "N/A")
            put("qr_code", qr ?: "NONE"); put("is_active", 1); put("is_blocked", 0); put("is_handshake_finalized", 1)
            put("flag_a", flag1); put("flag_b", flag2); put("flag_c", flag3)
            put("ref_a", ref1 ?: "CHAIN_LINK_ESTABLISHED"); put("ref_b", ref2 ?: "MULTI_LEVEL_PROTOCOL"); put("ref_c", ref3 ?: "STABLE_CHAIN")
            put("modified_at", now); put("readable_time", readable)
        }
        if (exists) {
            db.update(TABLE_PAIRING, v, "device_a_uid=? AND device_b_uid=?", arrayOf(deviceAId, deviceBId))
        } else {
            v.put("created_at", now)
            db.insert(TABLE_PAIRING, null, v)
        }
    }

    fun logSimCard(slot: Int, serial: String?, op: String?, mcc: String?, mnc: String?, lac: String?, cid: String?, isEsim: Boolean, fA: Int = 0, fB: Int = 0, fC: Int = 0, rA: String? = null, rB: String? = null, rC: String? = null) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("slot_index", slot); put("sim_serial", serial ?: "SIM-UNAVAILABLE"); put("operator_name", op ?: "Unknown")
            put("mcc", mcc ?: "000"); put("mnc", mnc ?: "00"); put("lac", lac ?: "0000"); put("cid", cid ?: "0000")
            put("is_active", 1); put("is_esim", if(isEsim) 1 else 0)
            put(COL_FLAG_A, fA); put(COL_FLAG_B, fB); put(COL_FLAG_C, fC)
            put(COL_REF_A, rA ?: ""); put(COL_REF_B, rB ?: ""); put(COL_REF_C, rC ?: "")
            put("modified_at", now); put("readable_time", getReadableNow())
        }
        val rc = db.query(TABLE_SIM_INVENTORY, arrayOf("sim_id"), "slot_index=? AND sim_serial=?", arrayOf(slot.toString(), serial ?: "SIM-UNAVAILABLE"), null, null, null)
        if (rc.moveToFirst()) {
            db.update(TABLE_SIM_INVENTORY, v, "sim_id=?", arrayOf(rc.getString(0)))
        } else {
            v.put("created_at", now)
            db.insert(TABLE_SIM_INVENTORY, null, v)
        }
        rc.close()
    }

    fun markSimSynced(slot: Int) {
        val v = ContentValues().apply { put("last_sync_at", System.currentTimeMillis()) }
        this.writableDatabase.update(TABLE_SIM_INVENTORY, v, "slot_index=?", arrayOf(slot.toString()))
    }

    fun logSimTransaction(slot: Int, mcc: String?, mnc: String?, lac: String?, cid: String?, lat: Double = 0.0, lng: Double = 0.0, addr: String? = null, fA: Int = 0, fB: Int = 0, fC: Int = 0, rA: String? = null, rB: String? = null, rC: String? = null) {
        val now = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("slot_index", slot); put("mcc", mcc ?: "000"); put("mnc", mnc ?: "00"); put("lac", lac ?: "0000"); put("cid", cid ?: "0000")
            put("lat", lat); put("lng", lng); put("address", addr ?: "No Coordinate Fix"); put("timestamp", now); put("readable_time", getReadableNow())
            put(COL_FLAG_A, fA); put(COL_FLAG_B, fB); put(COL_FLAG_C, fC); put(COL_REF_A, rA ?: ""); put(COL_REF_B, rB ?: ""); put(COL_REF_C, rC ?: "")
        }
        this.writableDatabase.insert(TABLE_SIM_TRANS, null, v)
    }
}
