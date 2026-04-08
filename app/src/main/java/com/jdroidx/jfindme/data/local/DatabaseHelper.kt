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
class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "JFindMe_CitizenData.db"
        const val DATABASE_VERSION = 48 // 🚀 V48: Advanced Forensic Hardware Meta-Sync
        const val MAX_DATETIME = 253402300799000L
        
        const val TABLE_PROFILE = "profile_table"
        const val TABLE_PERSONAL = TABLE_PROFILE // 🚀 Legacy Alias (V37 Forensic Bridge)
        const val TABLE_ROSTER = "roster_table"
        const val TABLE_ORG = "org_table"
        const val TABLE_SAFE_ZONES = "SafeZone_Table" 
        const val TABLE_ALERTS = "GeoFence_Alert_table"
        const val TABLE_HISTORY = "history_table" 
        const val TABLE_PAIRING = "pairing_log" 
        const val TABLE_SIM_INVENTORY = "sim_inventory" 
        const val TABLE_SIM_TRANS = "sim_transaction" 
        const val TABLE_SYNC_QUEUE = "geofenc_sync_queue_table" // 🚀 Reliable Sync Engine
        const val TABLE_SAFE_ZONE_GROUPS = "SafeZoneGroup_Table" // 🚀 Master-Child Cluster Mapping
        const val TABLE_GEOFENCE_LOGS = "GeofenceLog_Table" // 🚀 Forensic Transition Archival
        const val TABLE_DEVICE = "device_table" // 🚀 Master Hardware Hub (Silicon Fingerprint)

        const val COL_FLAG_A = "flag_a"
        const val COL_FLAG_B = "flag_b"
        const val COL_FLAG_C = "flag_c"
        const val COL_REF_A = "ref_a"
        const val COL_REF_B = "ref_b"
        const val COL_REF_C = "ref_c"
        const val EXT_FIELDS = "$COL_FLAG_A INTEGER, $COL_FLAG_B INTEGER, $COL_FLAG_C INTEGER, $COL_REF_A TEXT, $COL_REF_B TEXT, $COL_REF_C TEXT"
        const val LINEAGE_FIELDS = "owner_name TEXT, owner_device_id TEXT, connected_device_id TEXT, qr_code_ref TEXT"

        @Volatile
        private var instance: DatabaseHelper? = null

        @JvmStatic
        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
        try {
            db.execSQL("PRAGMA synchronous = NORMAL")
            db.execSQL("PRAGMA busy_timeout = 5000")
        } catch (e: android.database.sqlite.SQLiteException) {
            // Code 0 SQLITE_OK is technically a success but returns results.
            // Ignore the result-set warning to maintain atomic sync.
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_PROFILE (log_id TEXT PRIMARY KEY, uid TEXT, network_id TEXT, device_id TEXT, full_name TEXT, user_phone TEXT, user_address TEXT, user_dob TEXT, user_gender TEXT, user_country TEXT, user_state TEXT, user_city TEXT, user_pincode TEXT, user_emergency_note TEXT, org_name TEXT, department TEXT, role_family TEXT, role_friend TEXT, start_datetime INTEGER, end_datetime INTEGER DEFAULT $MAX_DATETIME, created_at INTEGER, modified_at INTEGER, readable_time TEXT, $EXT_FIELDS, $LINEAGE_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ROSTER (log_id TEXT PRIMARY KEY, uid TEXT, display_name TEXT, role_title TEXT, status_text TEXT, start_datetime INTEGER, end_datetime INTEGER DEFAULT $MAX_DATETIME, is_admin INTEGER DEFAULT 0, is_friend INTEGER DEFAULT 1, created_at INTEGER, modified_at INTEGER, readable_time TEXT, $EXT_FIELDS, $LINEAGE_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ORG (log_id TEXT PRIMARY KEY, uid TEXT, org_name TEXT, department TEXT, employee_id TEXT, designation TEXT, supervisor TEXT, shift_timing TEXT, work_location TEXT, badge_number TEXT, emergency_ext TEXT, access_level TEXT, start_datetime INTEGER, end_datetime INTEGER DEFAULT $MAX_DATETIME, created_at INTEGER, modified_at INTEGER, readable_time TEXT, $EXT_FIELDS, $LINEAGE_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SAFE_ZONES (zone_id TEXT PRIMARY KEY, name TEXT, target TEXT, uid TEXT, lat REAL, lng REAL, radius REAL, assigned_uids TEXT, sync_status TEXT DEFAULT 'LOCAL', created_at INTEGER, modified_at INTEGER, readable_time TEXT, $EXT_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ALERTS (alert_id TEXT PRIMARY KEY, uid TEXT, timestamp INTEGER, title TEXT, message TEXT, readable_time TEXT, $EXT_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (log_id TEXT PRIMARY KEY, uid TEXT, profile_name TEXT, relationship TEXT, timestamp INTEGER, from_datetime INTEGER, to_datetime INTEGER, ping_count INTEGER DEFAULT 1, lat REAL, lng REAL, speed REAL, battery INTEGER, sos_active INTEGER, address TEXT, modified_at INTEGER, modified_reason TEXT, readable_time TEXT, $EXT_FIELDS, $LINEAGE_FIELDS)")
        
        // 🚀 MASTER-CHILD CLUSTER HUB (L7 Reporting & Analytical Structure)
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SAFE_ZONE_GROUPS (" +
                "log_id TEXT PRIMARY KEY, zone_id TEXT, " +
                "device_id_a TEXT, assigned_device_ids TEXT, " +
                "start_time INTEGER, end_time INTEGER, active_days TEXT, " +
                "is_zone_active INTEGER DEFAULT 1, alerts_enabled INTEGER DEFAULT 1, " +
                "address TEXT, sync_status TEXT DEFAULT 'LOCAL', " +
                "created_at INTEGER, modified_at INTEGER, readable_time TEXT, " +
                "$EXT_FIELDS, $LINEAGE_FIELDS)")
        
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
        
        // 🚀 MASTER-CHILD LOGGING HUB (V42: Human Readable & Analytical Transit Archival)
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_GEOFENCE_LOGS (" +
                "log_id TEXT PRIMARY KEY, avatar_name TEXT, " +
                "zone_id TEXT, zone_name TEXT, status TEXT, " +
                "lat REAL, lng REAL, timestamp INTEGER, readable_time TEXT, " +
                "sync_status TEXT DEFAULT 'LOCAL', " +
                "$EXT_FIELDS, $LINEAGE_FIELDS)")
        
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_DEVICE (" +
                "log_id TEXT PRIMARY KEY, uid TEXT, hardware_id TEXT, device_model TEXT, " +
                "manufacturer TEXT, manufacturing_date TEXT, os_version TEXT, " +
                "security_patch TEXT, kernel_version TEXT, build_fingerprint TEXT, " +
                "total_cpu_cores INTEGER, total_ram TEXT, total_storage TEXT, " +
                "secured_phone TEXT, imei_1 TEXT, imei_2 TEXT, " +
                "iccid_1 TEXT, iccid_2 TEXT, sim_operator_1 TEXT, sim_operator_2 TEXT, " +
                "is_rooted INTEGER DEFAULT 0, mac_address TEXT, " +
                "created_at INTEGER, modified_at INTEGER, readable_time TEXT, " +
                "$EXT_FIELDS, $LINEAGE_FIELDS)")
        
        // seedDefaultSafeZones(db) // 🚀 QA Removal: No Null-Island Placeholders
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROFILE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROSTER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ORG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAFE_ZONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALERTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PAIRING")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SIM_INVENTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SIM_TRANS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SYNC_QUEUE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAFE_ZONE_GROUPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GEOFENCE_LOGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DEVICE")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    
    fun factoryReset() {
        onUpgrade(this.writableDatabase, 0, DATABASE_VERSION)
    }

    // 🚀 MASTER BRIDGE SUITE (V38 FINAL)

    /** 🚀 Universal Forensic Temporal Rule: All date/time must be readable dd-MM-yyyy HH:mm:ss */
    fun getReadableNow(): String {
        return formatMillis(System.currentTimeMillis())
    }

    fun formatMillis(ms: Long): String {
        return SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date(ms))
    }

    fun getActiveProfileAddress(): String {
        return getLocalDossier("OWNER_001")?.get("user_address") ?: "Unknown Location"
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
            put("readable_time", getReadableNow())
            put("created_at", System.currentTimeMillis())
            put("modified_at", System.currentTimeMillis())
            put("start_datetime", System.currentTimeMillis())
        }
        val db = this.writableDatabase
        db.insertWithOnConflict(TABLE_ROSTER, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * 🚀 Roster Grouping Evolution: Updates both role and category (Family/Friends) across ALL ledgers.
     */
    fun updateRosterGrouping(uid: String, newRole: String, isFriend: Boolean) {
        val db = this.writableDatabase
        
        // 📚 Ledger A: The Global Roster Table
        val vRoster = ContentValues().apply {
            put("role_title", newRole)
            put("is_friend", if(isFriend) 1 else 0)
            put("modified_at", System.currentTimeMillis())
            put("readable_time", getReadableNow())
        }
        db.update(TABLE_ROSTER, vRoster, "uid=?", arrayOf(uid))
        
        // 📚 Ledger B: The Personal Dossier (UI Single-Source-of-Truth)
        val vDossier = ContentValues().apply {
            put("role_family", if(isFriend) "None" else newRole)
            put("role_friend", if(isFriend) newRole else "None")
        }
        db.update(TABLE_PROFILE, vDossier, "uid=?", arrayOf(uid))
    }

    fun updateRosterRole(uid: String, newRole: String) {
        val v = ContentValues().apply {
            put("role_title", newRole)
            put("modified_at", System.currentTimeMillis())
            put("readable_time", getReadableNow())
        }
        this.writableDatabase.update(TABLE_ROSTER, v, "uid=?", arrayOf(uid))
    }

    fun ensureDefaultZones() {
        // seedDefaultSafeZones(this.writableDatabase)
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
            val rc = db.query(TABLE_PROFILE, arrayOf("log_id"), "uid=? AND end_datetime=?", arrayOf("OWNER_001", MAX_DATETIME.toString()), null, null, null)
            if (rc.moveToFirst()) {
                val upgrade = ContentValues().apply {
                    put("uid", uid)
                    put("modified_at", now)
                    put("readable_time", getReadableNow())
                }
                db.update(TABLE_PROFILE, upgrade, "log_id=?", arrayOf(rc.getString(0)))
            }
            rc.close()
        }

        val vUpd = ContentValues().apply { 
            put("end_datetime", now)
            put("modified_at", now)
            put("readable_time", getReadableNow())
        }
        db.update(TABLE_PROFILE, vUpd, "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()))
        
        val log = "${uid}_$now"
        v.put("log_id", log)
        v.put("uid", uid)
        v.put("start_datetime", now + 1)
        v.put("end_datetime", MAX_DATETIME)
        v.put("created_at", now)
        v.put("modified_at", now)
        db.insert(TABLE_PROFILE, null, v)
    }

    @JvmOverloads
    /** 🚀 Forensic Sync Bridge for ViewModels */
    fun commitForensicDossier(uid: String, map: Map<String, String>, role: String? = null, name: String? = null, d1: String? = null, d2: String? = null, ref: String? = "SYNC") {
        val v = ContentValues()
        v.put("uid", uid)
        
        // 🧪 Robust Key Mapping: Align camelCase cloud tags with snake_case table columns
        for ((k, value) in map) {
            when(k) {
                "userName", "fullName" -> v.put("full_name", value)
                "userPhone", "phone" -> v.put("user_phone", value)
                "userDob", "dob" -> v.put("user_dob", value)
                "userGender", "gender" -> v.put("user_gender", value)
                "userCountry", "country" -> v.put("user_country", value)
                "userState", "state" -> v.put("user_state", value)
                "userCity", "city" -> v.put("user_city", value)
                "userPincode", "pincode" -> v.put("user_pincode", value)
                "userAddress", "address" -> v.put("user_address", value)
                "userEmergencyNote", "emergencyNote" -> v.put("user_emergency_note", value)
                "orgName" -> v.put("org_name", value)
                "deptName", "department" -> v.put("department", value)
                else -> v.put(k, value)
            }
        }
        
        if (role != null) v.put("role_family", role)
        if (name != null) v.put("owner_name", name)
        if (d1 != null) {
            v.put("owner_device_id", d1)
            v.put("connected_device_id", d1)
        }
        if (ref != null) v.put(COL_REF_A, ref)
        v.put("uid", uid)
        savePersonalDossier(v)
    }

    /** 🚀 Hardware Forensic Bridge: Syncs Deep-Tier Silicon & OS Signatures */
    fun commitHardwareForensic(uid: String, hardwareId: String, model: String, manufacturer: String, mfgDate: String, os: String, patch: String, kernel: String, fingerprint: String, cpu: Int, ram: String, store: String, phone: String, imei1: String, imei2: String, iccid1: String, iccid2: String, op1: String, op2: String, rooted: Boolean, mac: String) {
        val v = ContentValues().apply {
            put("uid", uid)
            put("hardware_id", hardwareId)
            put("device_model", model)
            put("manufacturer", manufacturer)
            put("manufacturing_date", mfgDate)
            put("os_version", os)
            put("security_patch", patch)
            put("kernel_version", kernel)
            put("build_fingerprint", fingerprint)
            put("total_cpu_cores", cpu)
            put("total_ram", ram)
            put("total_storage", store)
            put("secured_phone", phone)
            put("imei_1", imei1)
            put("imei_2", imei2)
            put("iccid_1", iccid1)
            put("iccid_2", iccid2)
            put("sim_operator_1", op1)
            put("sim_operator_2", op2)
            put("is_rooted", if(rooted) 1 else 0)
            put("mac_address", mac)
        }
        saveDeviceDossier(v)
    }

    fun getLocalDossier(uid: String? = null): Map<String, String>? {
        val db = this.readableDatabase
        val sel = if(uid != null) "uid=? AND end_datetime=?" else "end_datetime=?"
        val args = if(uid != null) arrayOf(uid, MAX_DATETIME.toString()) else arrayOf(MAX_DATETIME.toString())
        db.query(TABLE_PROFILE, null, sel, args, null, null, null).use {
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

    fun saveDeviceDossier(v: ContentValues) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val uid = v.getAsString("uid") ?: "OWNER_001"
        v.put("readable_time", getReadableNow())
        v.put("created_at", now)
        v.put("modified_at", now)
        
        // 🚀 Forensic Hardware Logic: We only keep ONE active hardware record per UID to prevent spoofing.
        db.delete(TABLE_DEVICE, "uid=?", arrayOf(uid))
        
        val log = "DEV_${uid}_$now"
        v.put("log_id", log)
        db.insert(TABLE_DEVICE, null, v)
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

    /**
     * 🚀 MASTER-CHILD CLUSTER SYNC: Bridges the SafeZone Registry with Assigned Device Groups
     * V41 Architectural Pivot: Ensuring all geotech and temporal data is linked to the target nodes.
     */
    fun syncSafeZoneGroup(zoneId: String, assignedNodes: String, start: Int, end: Int, days: String, isActive: Int, alerts: Int, addr: String) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        val readable = getReadableNow()
        
        val v = ContentValues().apply {
            put("zone_id", zoneId)
            put("assigned_device_ids", assignedNodes)
            put("start_time", start)
            put("end_time", end)
            put("active_days", days)
            put("is_zone_active", isActive)
            put("alerts_enabled", alerts)
            put("address", addr)
            put("sync_status", "CLOUD_SYNCED")
            put("modified_at", now)
            put("readable_time", readable)
        }
        
        // 🚀 QA Fix: Upsert logic via CONFLICT_REPLACE or manual presence check
        db.query(TABLE_SAFE_ZONE_GROUPS, arrayOf("log_id"), "zone_id=?", arrayOf(zoneId), null, null, null).use {
            if (it.moveToFirst()) {
                db.update(TABLE_SAFE_ZONE_GROUPS, v, "zone_id=?", arrayOf(zoneId))
            } else {
                v.put("log_id", "G_${zoneId}_$now")
                v.put("created_at", now)
                db.insert(TABLE_SAFE_ZONE_GROUPS, null, v)
            }
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
                row["flag_a"] = c.getInt(c.getColumnIndexOrThrow(COL_FLAG_A)) // 🚀 QA Toggle Mapping
                return row
            }
        }
        return null
    }

    /**
     * 🚀 Geofence Integrity Check: Resolves if a specific zone has active siren alerts.
     */
    fun isGeofenceAlertEnabled(zoneId: String): Boolean {
        val db = this.readableDatabase
        db.query(TABLE_SAFE_ZONES, arrayOf(COL_FLAG_A), "zone_id=?", arrayOf(zoneId), null, null, null).use {
            if (it.moveToFirst()) return it.getInt(0) == 1
        }
        return true
    }

    fun getOfflineSafeZones(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        db.query(TABLE_SAFE_ZONES, null, null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                val row = mutableMapOf<String, Any>()
                row["zone_id"] = cursor.getString(cursor.getColumnIndexOrThrow("zone_id")) ?: ""
                row["name"] = cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: "Local Node"
                row["lat"] = cursor.getDouble(cursor.getColumnIndexOrThrow("lat"))
                row["lng"] = cursor.getDouble(cursor.getColumnIndexOrThrow("lng"))
                row["radius"] = cursor.getDouble(cursor.getColumnIndexOrThrow("radius"))
                row["target"] = cursor.getString(cursor.getColumnIndexOrThrow("target")) ?: "All"
                row["flag_a"] = cursor.getInt(cursor.getColumnIndexOrThrow("flag_a"))
                row["flag_b"] = cursor.getInt(cursor.getColumnIndexOrThrow("flag_b"))
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
        if (cols.contains("uid") && t != TABLE_PROFILE) cols.add(0, "IDENTITY_RESOLVED")
        return cols
    }

    /** 🚀 Authoritative Forensic Table Registry: SSOT for Management Consoles */
    fun getAllTables(): Array<String> {
        return arrayOf(
            TABLE_PROFILE, TABLE_ROSTER, TABLE_ORG, TABLE_SAFE_ZONES, 
            TABLE_ALERTS, TABLE_HISTORY, TABLE_PAIRING, TABLE_SIM_INVENTORY, 
            TABLE_SIM_TRANS, TABLE_SYNC_QUEUE, TABLE_SAFE_ZONE_GROUPS, TABLE_GEOFENCE_LOGS, TABLE_DEVICE
        )
    }

    fun getPagedData(t: String, l: Int, o: Int, q: String?): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = this.readableDatabase
        val rc = getTableColumns(t).toMutableList()
        if(rc.contains("IDENTITY_RESOLVED")) rc.removeAt(0)
        
        val hasUid = rc.contains("uid") && t != TABLE_PROFILE
        val sel = if (hasUid) "t.*, p.full_name as IDENTITY_RESOLVED" else "*"
        val join = if (hasUid) "$t t LEFT JOIN $TABLE_PROFILE p ON t.uid = p.uid AND p.end_datetime = $MAX_DATETIME" else "$t t"
        val whr = if (q.isNullOrEmpty()) "" else "WHERE " + rc.joinToString(" OR ") { "t.$it LIKE '%$q%'" }
        
        db.rawQuery("SELECT $sel FROM $join $whr LIMIT $l OFFSET $o", null).use { cursor ->
            val dc = getTableColumns(t)
            while (cursor.moveToNext()) {
                val row = mutableMapOf<String, String>()
                for (cn in dc) {
                    val ix = cursor.getColumnIndex(cn)
                    if (ix != -1) {
                        var raw = cursor.getString(ix) ?: "---"
                        
                        // 🚀 UNIVERSAL FORENSIC TEMPORAL RULE: Auto-Format timestamps
                        if (cn.contains("_at") || cn.contains("_datetime") || cn.contains("timestamp") || cn.contains("_time")) {
                            try {
                                val ms = raw.toLong()
                                if (ms == 253402300799000L) {
                                    raw = "FORENSIC_INFINITY"
                                } else if (ms > 1000000000000L) { // Post-2001 detection
                                    raw = formatMillis(ms)
                                } else if (ms == 0L) {
                                    raw = "GENESIS"
                                }
                            } catch (e: Exception) { /* Field already readable or non-numeric */ }
                        }
                        row[cn] = raw
                    } else {
                        row[cn] = "---"
                    }
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
        db.update(TABLE_PROFILE, v, "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()))
    }

    fun getDossierHistory(uid: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        this.readableDatabase.query(TABLE_PROFILE, null, "uid=?", arrayOf(uid), null, null, "start_datetime DESC").use { c ->
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

    fun deleteRecord(t: String, idCol: String, idVal: String): Boolean {
        return this.writableDatabase.delete(t, "$idCol=?", arrayOf(idVal)) > 0
    }

    fun updateRecord(t: String, idCol: String, idVal: String, v: ContentValues): Boolean {
        return this.writableDatabase.update(t, v, "$idCol=?", arrayOf(idVal)) > 0
    }

    fun getRecordById(t: String, idCol: String, idVal: String): Map<String, String>? {
        val db = this.readableDatabase
        db.query(t, null, "$idCol=?", arrayOf(idVal), null, null, null).use {
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

    fun saveAlert(uid: String, title: String, message: String, flagA: Int = 0, flagB: Int = 0, flagC: Int = 0) {
        val v = android.content.ContentValues().apply {
            put("alert_id", java.util.UUID.randomUUID().toString())
            put("uid", uid); put("timestamp", System.currentTimeMillis()); put("title", title); put("message", message)
            put("readable_time", getReadableNow())
            put(COL_FLAG_A, flagA); put(COL_FLAG_B, flagB); put(COL_FLAG_C, flagC)
        }
        this.writableDatabase.insert(TABLE_ALERTS, null, v)
    }

    fun getAlertHistory(uid: String): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        this.readableDatabase.query(TABLE_ALERTS, null, "uid=?", arrayOf(uid), null, null, "timestamp DESC").use {
            while (it.moveToNext()) {
                val map = mutableMapOf<String, Any>()
                for (i in 0 until it.columnCount) {
                    val name = it.getColumnName(i)
                    val type = it.getType(i)
                    when (type) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> map[name] = it.getLong(i)
                        android.database.Cursor.FIELD_TYPE_FLOAT -> map[name] = it.getDouble(i)
                        android.database.Cursor.FIELD_TYPE_STRING -> map[name] = it.getString(i) ?: ""
                        android.database.Cursor.FIELD_TYPE_BLOB -> map[name] = it.getBlob(i)
                        else -> map[name] = ""
                    }
                }
                list.add(map)
            }
        }
        return list
    }

    // 🚀 MASTER-CHILD LOGGING HUB (V42: Human Readable & Analytical Transit Archival)

    fun saveGeofenceLog(uid: String, avatarName: String, zoneId: String, zoneName: String, status: String, lat: Double = 0.0, lng: Double = 0.0) {
        val now = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("log_id", UUID.randomUUID().toString())
            put("avatar_name", avatarName); put("zone_id", zoneId); put("zone_name", zoneName); put("status", status)
            put("lat", lat); put("lng", lng); put("timestamp", now); put("readable_time", getReadableNow())
            put(COL_FLAG_A, 0); put(COL_FLAG_B, 0); put(COL_FLAG_C, 0)
            put("owner_name", uid); put("owner_device_id", ""); put("connected_device_id", ""); put("qr_code_ref", "")
        }
        val rowId = this.writableDatabase.insert(TABLE_GEOFENCE_LOGS, null, v)
        SystemLogger.i("DatabaseHelper", "GEOFENCE_ARCHIVAL: Mission log archived for $avatarName at $zoneName. Status: $status. ROW_ID: $rowId")
    }

    fun getGeofenceLogs(avatarFilter: String? = null, zoneFilter: String? = null): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val selection = StringBuilder()
        val args = mutableListOf<String>()

        if (!avatarFilter.isNullOrEmpty()) {
            selection.append("avatar_name LIKE ?")
            args.add("%$avatarFilter%")
        }
        if (!zoneFilter.isNullOrEmpty()) {
            if (selection.isNotEmpty()) selection.append(" AND ")
            selection.append("zone_id = ?")
            args.add(zoneFilter)
        }

        val selStr = if (selection.isEmpty()) null else selection.toString()
        val argArray = if (args.isEmpty()) null else args.toTypedArray()

        this.readableDatabase.query(TABLE_GEOFENCE_LOGS, null, selStr, argArray, null, null, "timestamp DESC").use {
            while (it.moveToNext()) {
                val map = mutableMapOf<String, Any>()
                for (i in 0 until it.columnCount) {
                    val name = it.getColumnName(i)
                    map[name] = when (it.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(i)
                        android.database.Cursor.FIELD_TYPE_FLOAT -> it.getDouble(i)
                        else -> it.getString(i) ?: ""
                    }
                }
                list.add(map)
            }
        }
        return list
    }
}
