 package com.jdroidx.jfindme.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jdroidx.jfindme.utils.SystemLogger

/**
 * Citizen Database Engine (L7 Global Architect)
 * V36 FINAL PRODUCTION RECONSTRUCTION: Forensic Schema Hardening (Zero-Gap Wipe).
 */
class CitizenDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "JFindMe_CitizenData.db"
        private const val DATABASE_VERSION = 25 // 🚀 V37: Hardened Pairing Ledger
        private const val MAX_DATETIME = 253402300799000L
        
        private const val TABLE_PERSONAL = "personal_table"
        private const val TABLE_ROSTER = "roster_table"
        private const val TABLE_ORG = "org_table"
        private const val TABLE_SAFE_ZONES = "safezone_table"
        private const val TABLE_ALERTS = "alerts_table"
        private const val TABLE_HISTORY = "history_table" 
        private const val TABLE_PAIRING = "pairing_log" 

        private const val COL_FLAG_A = "flag_a"; private const val COL_FLAG_B = "flag_b"; private const val COL_FLAG_C = "flag_c"
        private const val COL_REF_A = "ref_a"; private const val COL_REF_B = "ref_b"; private const val COL_REF_C = "ref_c"
        private const val EXT_FIELDS = "$COL_FLAG_A INTEGER, $COL_FLAG_B INTEGER, $COL_FLAG_C INTEGER, $COL_REF_A TEXT, $COL_REF_B TEXT, $COL_REF_C TEXT"
        private const val LINEAGE_FIELDS = "owner_name TEXT, owner_device_id TEXT, connected_device_id TEXT, qr_code_ref TEXT"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_PERSONAL (log_id TEXT PRIMARY KEY, uid TEXT, network_id TEXT, device_id TEXT, full_name TEXT, user_phone TEXT, user_address TEXT, user_dob TEXT, user_gender TEXT, user_country TEXT, user_state TEXT, user_city TEXT, user_pincode TEXT, user_emergency_note TEXT, role_family TEXT, role_friend TEXT, start_datetime INTEGER, end_datetime INTEGER DEFAULT $MAX_DATETIME, created_at INTEGER, modified_at INTEGER, $EXT_FIELDS, $LINEAGE_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ROSTER (log_id TEXT PRIMARY KEY, uid TEXT, display_name TEXT, role_title TEXT, status_text TEXT, start_datetime INTEGER, end_datetime INTEGER DEFAULT $MAX_DATETIME, is_admin INTEGER DEFAULT 0, is_friend INTEGER DEFAULT 1, created_at INTEGER, modified_at INTEGER, $EXT_FIELDS, $LINEAGE_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ORG (log_id TEXT PRIMARY KEY, uid TEXT, org_name TEXT, department TEXT, employee_id TEXT, designation TEXT, supervisor TEXT, shift_timing TEXT, work_location TEXT, badge_number TEXT, emergency_ext TEXT, access_level TEXT, start_datetime INTEGER, end_datetime INTEGER DEFAULT $MAX_DATETIME, created_at INTEGER, modified_at INTEGER, $EXT_FIELDS, $LINEAGE_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SAFE_ZONES (zone_id TEXT PRIMARY KEY, name TEXT, target TEXT, uid TEXT, lat REAL, lng REAL, radius REAL, created_at INTEGER, modified_at INTEGER, $EXT_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ALERTS (alert_id TEXT PRIMARY KEY, uid TEXT, timestamp INTEGER, title TEXT, message TEXT, $EXT_FIELDS)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (log_id TEXT PRIMARY KEY, uid TEXT, profile_name TEXT, relationship TEXT, timestamp INTEGER, from_datetime INTEGER, to_datetime INTEGER, ping_count INTEGER DEFAULT 1, lat REAL, lng REAL, speed REAL, battery INTEGER, sos_active INTEGER, address TEXT, modified_at INTEGER, modified_reason TEXT, $EXT_FIELDS, $LINEAGE_FIELDS)")
        
        // 🚀 NEW: Device Pairing Ledger (Device A -> All Device B Log)
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_PAIRING (" +
                "pairing_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "host_uid TEXT, host_name TEXT, " +
                "child_uid TEXT, child_name TEXT, child_role TEXT, " +
                "is_active INTEGER DEFAULT 1, is_blocked INTEGER DEFAULT 0, is_handshake_finalized INTEGER DEFAULT 1, " +
                "ref_a TEXT, ref_b TEXT, ref_c TEXT, " +
                "created_at INTEGER, modified_at INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PERSONAL"); db.execSQL("DROP TABLE IF EXISTS $TABLE_ROSTER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ORG"); db.execSQL("DROP TABLE IF EXISTS $TABLE_SAFE_ZONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALERTS"); db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PAIRING")
        onCreate(db)
    }
    
    fun factoryReset() { onUpgrade(this.writableDatabase, 0, DATABASE_VERSION) }

    // 🏆 ULTIMATE BRIDGE SUITE (V36 FINAL)

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
            
            // 🚀 Forensic Role Partitioning: Friends vs Family
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
        }
        savePersonalDossier(v)
    }

    fun updateCitizenRole(uid: String, fam: String, fri: String, isFriend: Boolean) {
        val v = ContentValues().apply { 
            put("uid", uid)
            put("role_title", if(fam!="None") fam else fri)
            put("is_friend", if(isFriend) 1 else 0)
        }
        saveRosterNode(v)
    }

    fun getOfflineRoster(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>(); val db = this.readableDatabase
        db.query(TABLE_ROSTER, null, "end_datetime=?", arrayOf(MAX_DATETIME.toString()), null, null, "display_name ASC").use { cursor ->
            while (cursor.moveToNext()) {
                val row = mutableMapOf<String, Any>()
                for (i in 0 until cursor.columnCount) {
                    val name = cursor.getColumnName(i)
                    if (name == "is_admin" || name == "is_friend") row[name] = cursor.getInt(i) == 1
                    else row[name] = cursor.getString(i) ?: ""
                }
                list.add(row)
            }
        }
        return list
    }

    fun saveRosterNode(uid: String?, name: String?, role: String?, status: String?, isAdmin: Boolean = false, isFriend: Boolean = true, ownerName: String? = null, ownerDeviceId: String? = null, connectedDeviceId: String? = null, qrCode: String? = null) {
        val v = ContentValues().apply {
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
        }
        saveRosterNode(v)
    }

    fun saveRosterNode(v: ContentValues) {
        val db = this.writableDatabase; val now = System.currentTimeMillis(); val uid = v.getAsString("uid") ?: return
        db.query(TABLE_ROSTER, arrayOf("log_id"), "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()), null, null, null).use {
            if (it.moveToFirst()) { v.put("modified_at", now); db.update(TABLE_ROSTER, v, "log_id=?", arrayOf(it.getString(0))) }
            else { val log = "R_${uid}_$now"; v.put("log_id", log); v.put("uid", uid); v.put("start_datetime", now); v.put("end_datetime", MAX_DATETIME); v.put("created_at", now); v.put("modified_at", now); db.insert(TABLE_ROSTER, null, v) }
        }
    }

    fun savePersonalDossier(v: ContentValues) {
        val db = this.writableDatabase; val now = System.currentTimeMillis(); val uid = v.getAsString("uid") ?: "OWNER_001"
        db.query(TABLE_PERSONAL, arrayOf("log_id"), "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()), null, null, null).use {
            if (it.moveToFirst()) { v.put("modified_at", now); db.update(TABLE_PERSONAL, v, "log_id=?", arrayOf(it.getString(0))) }
            else { val log = "${uid}_$now"; v.put("log_id", log); v.put("uid", uid); v.put("start_datetime", now); v.put("end_datetime", MAX_DATETIME); v.put("created_at", now); v.put("modified_at", now); db.insert(TABLE_PERSONAL, null, v) }
        }
    }

    fun getLocalDossier(uid: String? = null): Map<String, String>? {
        val db = this.readableDatabase; val sel = if(uid!=null) "uid=? AND end_datetime=?" else "end_datetime=?"
        val args = if(uid!=null) arrayOf(uid, MAX_DATETIME.toString()) else arrayOf(MAX_DATETIME.toString())
        db.query(TABLE_PERSONAL, null, sel, args, null, null, null).use {
            if (it.moveToFirst()) {
                val map = mutableMapOf<String, String>()
                for (i in 0 until it.columnCount) map[it.getColumnName(i)] = it.getString(i) ?: ""
                return map
            }
        }
        return null
    }

    fun saveHistoryPoint(uid: String, name: String, r: String, lat: Double, lng: Double, spd: Float, b: Int, sos: Boolean, addr: String?) {
        val db = this.writableDatabase; val now = System.currentTimeMillis()
        db.query(TABLE_HISTORY, null, "uid=? AND profile_name=?", arrayOf(uid, name), null, null, "to_datetime DESC", "1").use { c ->
            if (c.moveToFirst()) {
                val d = com.jdroidx.jfindme.engine.data.SpatialProcessor.getDistance(lat, lng, c.getDouble(c.getColumnIndexOrThrow("lat")), c.getDouble(c.getColumnIndexOrThrow("lng")))
                if (d < 20.0) {
                    val v = ContentValues().apply { put("to_datetime", now); put("ping_count", c.getInt(c.getColumnIndexOrThrow("ping_count")) + 1); put("battery", b); put("modified_at", now) }
                    db.update(TABLE_HISTORY, v, "log_id=?", arrayOf(c.getString(c.getColumnIndexOrThrow("log_id")))); return
                }
            }
        }
        val v = ContentValues().apply { put("log_id", "${uid}_$now"); put("uid", uid); put("profile_name", name); put("relationship", r); put("timestamp", now); put("from_datetime", now); put("to_datetime", now); put("ping_count", 1); put("lat", lat); put("lng", lng); put("speed", spd); put("battery", b); put("sos_active", if (sos) 1 else 0); put("address", addr ?: "Tracing..."); put("modified_at", now) }
        db.insert(TABLE_HISTORY, null, v)
    }

    fun saveAlert(uid: String, title: String, message: String) {
        val db = this.writableDatabase; val now = System.currentTimeMillis()
        val v = ContentValues().apply { put("alert_id", "ALRT_$now"); put("uid", uid); put("timestamp", now); put("title", title); put("message", message); put(COL_FLAG_A, 1) }
        db.insert(TABLE_ALERTS, null, v)
    }

    fun saveOrgDossier(v: ContentValues) {
        val db = this.writableDatabase; val now = System.currentTimeMillis(); val uid = v.getAsString("uid") ?: "OWNER_001"
        db.query(TABLE_ORG, arrayOf("log_id"), "uid=? AND end_datetime=?", arrayOf(uid, MAX_DATETIME.toString()), null, null, null).use {
            if (it.moveToFirst()) { v.put("modified_at", now); db.update(TABLE_ORG, v, "log_id=?", arrayOf(it.getString(0))) }
            else { val log = "O_${uid}_$now"; v.put("log_id", log); v.put("uid", uid); v.put("start_datetime", now); v.put("end_datetime", MAX_DATETIME); v.put("created_at", now); v.put("modified_at", now); db.insert(TABLE_ORG, null, v) }
        }
    }

    fun saveSafeZone(v: ContentValues) {
        val db = this.writableDatabase; val now = System.currentTimeMillis(); val zId = v.getAsString("zone_id") ?: "Z_$now"
        db.query(TABLE_SAFE_ZONES, arrayOf("zone_id"), "zone_id=?", arrayOf(zId), null, null, null).use {
            if (it.moveToFirst()) { v.put("modified_at", now); db.update(TABLE_SAFE_ZONES, v, "zone_id=?", arrayOf(zId)) }
            else { v.put("created_at", now); v.put("modified_at", now); db.insert(TABLE_SAFE_ZONES, null, v) }
        }
    }
    
    fun getOfflineSafeZones(): List<Map<String, Any>> { val list = mutableListOf<Map<String, Any>>(); val db = this.readableDatabase; db.query(TABLE_SAFE_ZONES, null, null, null, null, null, null).use { cursor -> while (cursor.moveToNext()) { val row = mutableMapOf<String, Any>(); row["zone_id"] = cursor.getString(cursor.getColumnIndexOrThrow("zone_id")) ?: ""; row["lat"] = cursor.getDouble(cursor.getColumnIndexOrThrow("lat")); row["lng"] = cursor.getDouble(cursor.getColumnIndexOrThrow("lng")); row["radius"] = cursor.getDouble(cursor.getColumnIndexOrThrow("radius")); row["target"] = cursor.getString(cursor.getColumnIndexOrThrow("target")) ?: "All"; list.add(row) } }; return list }
    fun getProfileData(uid: String): Map<String, String>? { return getLocalDossier(uid) }
    fun truncateTable(tableName: String) { val db = this.writableDatabase; db.delete(tableName, null, null) }
    fun getRecordCount(t: String): Long {
        return try { android.database.DatabaseUtils.queryNumEntries(this.readableDatabase, t) } catch (e: Exception) { 0L }
    }
    fun getTableColumns(t: String): List<String> { val cols = mutableListOf<String>(); val db = this.readableDatabase; db.rawQuery("PRAGMA table_info($t)", null).use { c -> if (c.moveToFirst()) { do { cols.add(c.getString(1)) } while (c.moveToNext()) } }; if (cols.contains("uid") && t != TABLE_PERSONAL) cols.add(0, "IDENTITY_RESOLVED"); return cols }
    fun getPagedData(t: String, l: Int, o: Int, q: String?): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>(); val db = this.readableDatabase; val rc = getTableColumns(t).toMutableList(); if(rc.contains("IDENTITY_RESOLVED")) rc.removeAt(0)
        val hasUid = rc.contains("uid") && t != TABLE_PERSONAL; 
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
        }; return list
    }
    fun logDevicePairing(hostId: String, hostName: String, childId: String, childName: String, role: String, ref1: String? = null, ref2: String? = null, ref3: String? = null) {
        val db = this.writableDatabase
        val now = System.currentTimeMillis()
        
        // Ensure pairing is tracked for this unique Host-Child tuple
        val cursor = db.query(TABLE_PAIRING, arrayOf("pairing_id"), "host_uid=? AND child_uid=?", arrayOf(hostId, childId), null, null, null)
        val exists = cursor.moveToFirst()
        cursor.close()

        val v = ContentValues().apply {
            put("host_uid", hostId)
            put("host_name", hostName)
            put("child_uid", childId)
            put("child_name", childName)
            put("child_role", role)
            put("is_active", 1)
            put("is_blocked", 0)
            put("is_handshake_finalized", 1)
            put("ref_a", ref1 ?: "HANDSHAKE_ESTABLISHED")
            put("ref_b", ref2 ?: "L7_V37_PROTOCOL")
            put("ref_c", ref3 ?: "STABLE_PAIR")
            put("modified_at", now)
        }

        if (exists) {
            db.update(TABLE_PAIRING, v, "host_uid=? AND child_uid=?", arrayOf(hostId, childId))
            SystemLogger.i("CitizenDbHelper", "Audit Updated: $hostId physically linked to $childId")
        } else {
            v.put("created_at", now)
            db.insert(TABLE_PAIRING, null, v)
            SystemLogger.i("CitizenDbHelper", "New Audit Logged: $hostId linked with $childId")
        }
    }
}
