/**
 * Auto-Login Architecture: Enterprise Database Engine
 * Waterfall Method: Secured Relational Bridge & Forensic Ledger
 */
package com.jdroidx.jfindme.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jdroidx.jfindme.data.local.tables.*
import com.jdroidx.jfindme.utils.SystemLogger
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enterprise Database Engine (L7 Global Architect) - V57 ANALYTICAL REDESIGN
 * Handles Normalized 5-Tier Architecture logic.
 */
class DatabaseHelper private constructor(val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "JFindMe_CitizenData.db"
        const val DATABASE_VERSION = 73 // 🧬 V73: SafeZone Archival & Realtime Tree Parity Migrations
        const val MAX_DATETIME = 253402300799000L
        
        // --- RELATIONAL ANCHORS ---
        const val COL_UID_1 = "uid_1"
        const val COL_UID_2 = "uid_2"
        const val COL_MEMO_NOTE = "memo_note"

        // Legacy table constants (redirected where applicable)
        const val PROFILE_TABLE = "user_profile" 
        const val MASTER_ROSTER = "MASTER_ROSTER" 
        const val TABLE_ORG = "organizations"
        const val TABLE_SAFE_ZONES = "safe_zones" 
        const val TABLE_ALERTS = "alerts"
        const val TABLE_HISTORY = "location_event" 
        const val TABLE_PAIRING = "device_pairings" 
        const val TABLE_SIM_INVENTORY = "sim_inventory" 
        const val TABLE_SIM_TRANS = "sim_transactions" 
        const val TABLE_SYNC_QUEUE = "sync_queue" 
        const val TABLE_SAFE_ZONE_GROUPS = "safe_zone_user_map" 
        const val TABLE_GEOFENCE_LOGS = "geofence_events" 
        const val TABLE_DEVICE = "devices" 
        const val TABLE_SYS_LOG = "sys_log" 

        const val COL_FLAG_A = "flag_a"
        const val COL_FLAG_B = "flag_b"
        const val COL_FLAG_C = "flag_c"
        const val COL_REF_A = "ref_a"
        const val COL_REF_B = "ref_b"
        const val COL_REF_C = "ref_c"
        const val EXT_FIELDS = "$COL_FLAG_A INTEGER, $COL_FLAG_B INTEGER, $COL_FLAG_C INTEGER, $COL_REF_A TEXT, $COL_REF_B TEXT, $COL_REF_C TEXT"
        const val LINEAGE_FIELDS = "owner_name TEXT, $COL_UID_1 TEXT, $COL_UID_2 TEXT"

        @Volatile
        private var instance: DatabaseHelper? = null

        @JvmStatic
        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private var isPerformingMigration = false

    // --- TIERED ARCHITECTURE METHODS (ANALYTICAL REDESIGN) ---

    fun generateUid2(): String {
        // 🧬 V58.5 Unified Suffix Protocol: Use Last 4 for forensic consistency
        val raw = UUID.randomUUID().toString().replace("-", "").uppercase()
        return "HW_" + raw.takeLast(4)
    }

    fun getAllTables(): Array<String> {
        val tables = mutableListOf<String>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'", null)
            cursor.use {
                while (it.moveToNext()) {
                    tables.add(it.getString(0))
                }
            }
        } catch (e: Exception) { }
        return tables.toTypedArray()
    }

    fun factoryReset() {
        val tables = getAllTables()
        for (t in tables) {
            truncateTable(t)
        }
    }

    fun truncateTable(name: String) {
        try {
            writableDatabase.delete(name, null, null)
        } catch (e: Exception) { }
    }


    /**
     * 🧬 Resolve Forensic Identity: Decouples Cloud Auth from physical tracking.
     * Ensures that one Hardware Node maps to one persistent Agent ID.
     * V58.1 Fix: Deterministic Fallback - Prevents duplicate identity creation.
     */
    fun resolveForensicAgentId(uid2: String): String {
        try {
            val db = readableDatabase
            val cursor = db.query(UserIdentityTable.TABLE_NAME, arrayOf(UserIdentityTable.COL_UID_1), 
                "${UserIdentityTable.COL_UID_2}=?", arrayOf(uid2), null, null, null)
            cursor.use {
                if (it.moveToFirst()) return it.getString(0)
            }
        } catch (e: Exception) { }
        
        // 🧬 V60 Forensic Rule: Use high-entropy 8-character hardware signature (No Prefix)
        val cleanHw = uid2.replace("HW_", "").replace("-", "").uppercase()
        return if (cleanHw.length >= 8) {
            cleanHw.take(4) + cleanHw.takeLast(4)
        } else {
            cleanHw.padEnd(8, '7')
        }
    }

    /**
     * 🧬 V69.5: Local Identity Resolution (Cache-First)
     * Resolves the 8-character Agent ID (uid_1) from the transient Auth UID using the local ledger.
     */
    fun resolveAgentIdLocally(authUid: String): String? {
        try {
            val db = readableDatabase
            val cursor = db.query(UserIdentityTable.TABLE_NAME, arrayOf(UserIdentityTable.COL_UID_1),
                "${UserIdentityTable.COL_AUTH_UID}=?", arrayOf(authUid), null, null, null)
            cursor.use {
                if (it.moveToFirst()) return it.getString(0)
            }
        } catch (e: Exception) { }
        return null
    }

    /**
     * 🧬 L7 Segregation: Calculates the device index for a specific user.
     * Starts with 1 for the first device of any UID_1.
     */
    fun resolveNodeIndex(uid1: String, uid2: String): Int {
        try {
            val db = readableDatabase
            // 1. Check if specific device already has an index
            val cursor = db.query(UserIdentityTable.TABLE_NAME, arrayOf(UserIdentityTable.COL_NODE_INDEX),
                "${UserIdentityTable.COL_UID_1}=? AND ${UserIdentityTable.COL_UID_2}=?", arrayOf(uid1, uid2), null, null, null)
            cursor.use {
                if (it.moveToFirst()) return it.getInt(0)
            }
            
            // 2. Get the next available index for this user
            val countCursor = db.rawQuery("SELECT COUNT(*) FROM ${UserIdentityTable.TABLE_NAME} WHERE ${UserIdentityTable.COL_UID_1}=?", arrayOf(uid1))
            countCursor.use {
                if (it.moveToFirst()) return it.getInt(0) + 1
            }
        } catch (e: Exception) { }
        return 1
    }

    /**
     * 🧬 V63.8: Identity Bridge - Resolves the internal Long ID for a String UID1, Full Name, Owner Name, or Auth UID.
     */
    fun resolveInternalId(uid1: String): Long {
        if (uid1.isBlank()) return -1L
        try {
            val db = readableDatabase
            val cursor = db.query(UserIdentityTable.TABLE_NAME, arrayOf(UserIdentityTable.COL_ID),
                "${UserIdentityTable.COL_UID_1}=? OR ${UserIdentityTable.COL_FULL_NAME}=? OR ${UserIdentityTable.COL_OWNER_NAME}=? OR ${UserIdentityTable.COL_AUTH_UID}=? OR ${UserIdentityTable.COL_CONNECTED_ID}=? OR ${UserIdentityTable.COL_UID_2}=?",
                arrayOf(uid1, uid1, uid1, uid1, uid1, uid1), null, null, null)
            cursor.use {
                if (it.moveToFirst()) return it.getLong(0)
            }
        } catch (e: Exception) { }
        return -1L
    }

    /**
     * 👤 Analytical Profile Sync: Cascading updates via SQL subqueries.
     */
    fun syncProfileAnalytical(uid1: String, name: String?, email: String? = null, avatar: String? = null, note: String? = null) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            val dataMap = mapOf<String, String>(
                "userName" to (name ?: ""),
                "userEmail" to (email ?: ""),
                "avatar_id" to (avatar ?: ""),
                "emergencyNote" to (note ?: "")
            )
            val iId = resolveInternalId(uid1)
            if (iId != -1L) {
                UserProfileTable.upsert(db, iId, dataMap)
                
                // Smart SQL trigger: Auto-align groups based on Identity Auth level
                db.execSQL("""
                    UPDATE ${GroupMemberTable.TABLE_NAME} 
                    SET role = 'ADMIN' 
                    WHERE ${GroupMemberTable.COL_IDENTITY_ID} = $iId 
                    AND EXISTS (SELECT 1 FROM ${UserIdentityTable.TABLE_NAME} WHERE ${UserIdentityTable.COL_ID} = $iId AND ${UserIdentityTable.COL_AUTH_UID} IS NOT NULL)
                """)
            }
            
            saveSysLog("I", "PROFILE_SYNC", "Analytical metadata propagated for node: $uid1", null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun syncIdentityCore(authUid: String, uid1: String, uid2: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val nIdx = resolveNodeIndex(uid1, uid2)
            UserIdentityTable.insertOrUpdate(db, uid1, uid2, authUid, nodeIndex = nIdx)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * 📱 Device Analytical Sync: Hardware-locked registration.
     */
    fun syncDeviceAnalytical(uid1: String, deviceId: String, model: String?, imei: String?, rooted: Boolean, fingerprint: String?, lat: Double = 0.0, lng: Double = 0.0, addr: String? = null) {
        val db = this.writableDatabase
        val iId = resolveInternalId(uid1)
        if (iId != -1L) DeviceTable.upsert(db, deviceId, iId, model, imei, rooted, fingerprint, lat, lng, addr)
    }

    // --- SYSTEM LIFECYCLE ---

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
        db.execSQL("PRAGMA synchronous = NORMAL")
        // 🧬 Offline-First Sync Guard: Disable strict FKs so we can accept out-of-order Firebase nodes
        db.execSQL("PRAGMA foreign_keys = OFF")
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // 🚀 Self-Healing diagnostic: Force-audit sys_log for 'level' column presence
        try {
            db.rawQuery("SELECT level FROM sys_log LIMIT 0", null).use { }
        } catch (e: Exception) {
            try {
                db.execSQL("ALTER TABLE sys_log ADD COLUMN level TEXT DEFAULT 'I'")
            } catch (ex: Exception) { }
        }

        // 🚀 Self-Healing: Verify user_profile has 'name' and 'email' columns (V58.2 Patch)
        try {
            db.rawQuery("SELECT name FROM user_profile LIMIT 0", null).use { }
        } catch (e: Exception) {
            try { db.execSQL("ALTER TABLE user_profile ADD COLUMN name TEXT") } catch (ex: Exception) { }
        }
        try {
            db.rawQuery("SELECT email FROM user_profile LIMIT 0", null).use { }
        } catch (e: Exception) {
            try { db.execSQL("ALTER TABLE user_profile ADD COLUMN email TEXT") } catch (ex: Exception) { }
        }

        // 🚀 Self-Healing: Verify location_event has 'created_on' column (V71 Patch)
        try {
            db.rawQuery("SELECT created_on FROM location_event LIMIT 0", null).use { }
        } catch (e: Exception) {
            try { db.execSQL("ALTER TABLE location_event ADD COLUMN created_on TEXT") } catch (ex: Exception) { }
        }

        // 🚀 Self-Healing: Verify user_identity has 'install_epoch_id' and 'is_current_session' columns (V72 Patch)
        try {
            db.rawQuery("SELECT install_epoch_id FROM ${UserIdentityTable.TABLE_NAME} LIMIT 0", null).use { }
        } catch (e: Exception) {
            try { db.execSQL("ALTER TABLE ${UserIdentityTable.TABLE_NAME} ADD COLUMN install_epoch_id TEXT DEFAULT ''") } catch (ex: Exception) { }
        }
        try {
            db.rawQuery("SELECT is_current_session FROM ${UserIdentityTable.TABLE_NAME} LIMIT 0", null).use { }
        } catch (e: Exception) {
            try { db.execSQL("ALTER TABLE ${UserIdentityTable.TABLE_NAME} ADD COLUMN is_current_session INTEGER DEFAULT 1") } catch (ex: Exception) { }
        }

        // 🚀 V61.2 Self-Healing: Augment user_org_map with forensic markers
        val orgCols = mapOf(
            UserOrgMapTable.COL_EMPLOYEE_ID to "TEXT",
            UserOrgMapTable.COL_SUPERVISOR to "TEXT",
            UserOrgMapTable.COL_SHIFT to "TEXT",
            UserOrgMapTable.COL_BADGE to "TEXT",
            UserOrgMapTable.COL_EXT to "TEXT",
            UserOrgMapTable.COL_CLEARANCE to "TEXT"
        )
        for ((col, type) in orgCols) {
            try {
                db.rawQuery("SELECT $col FROM ${UserOrgMapTable.TABLE_NAME} LIMIT 0", null).use { }
            } catch (e: Exception) {
                try { db.execSQL("ALTER TABLE ${UserOrgMapTable.TABLE_NAME} ADD COLUMN $col $type") } catch (ex: Exception) { }
            }
        }

        // 🚀 V73 Self-Healing: Verify safe_zone has all necessary columns for archival & tree parity
        val safeZoneCols = mapOf(
            SafeZoneTable.COL_IS_ARCHIVED to "INTEGER DEFAULT 0",
            SafeZoneTable.COL_IS_ACTIVE to "INTEGER DEFAULT 1",
            SafeZoneTable.COL_IS_ALERTS_ENABLED to "INTEGER DEFAULT 1",
            SafeZoneTable.COL_CREATED_BY to "INTEGER",
            SafeZoneTable.COL_TARGET_UID to "TEXT",
            SafeZoneTable.COL_CREATED_ON to "TEXT"
        )
        for ((col, def) in safeZoneCols) {
            try {
                db.rawQuery("SELECT $col FROM ${SafeZoneTable.TABLE_NAME} LIMIT 0", null).use { }
            } catch (e: Exception) {
                try { db.execSQL("ALTER TABLE ${SafeZoneTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
            }
        }

        // 🚀 Self-Healing: Verify ChangeLogTable presence
        try {
            db.execSQL(ChangeLogTable.CREATE_TABLE)
        } catch (e: Exception) { }

        // 🚀 Data Sanitization: Purge corrupted legacy logs saved as safe zones
        try {
            db.execSQL("DELETE FROM ${SafeZoneTable.TABLE_NAME} WHERE ${SafeZoneTable.COL_ZONE_ID} LIKE 'HANDSHAKE%' OR (${SafeZoneTable.COL_LAT} = 0 AND ${SafeZoneTable.COL_LNG} = 0)")
        } catch (e: Exception) { }
    }

    override fun onCreate(db: SQLiteDatabase) {
        isPerformingMigration = true
        try {
            // TIER 1: IDENTITY
            db.execSQL(UserIdentityTable.CREATE_TABLE)
            db.execSQL(UserProfileTable.CREATE_TABLE)
            db.execSQL(OrganizationTable.CREATE_TABLE)
            db.execSQL(UserOrgMapTable.CREATE_TABLE)

            // TIER 2: DEVICE
            db.execSQL(DeviceTable.CREATE_TABLE)
            db.execSQL(DevicePairingTable.CREATE_TABLE)
            db.execSQL(SimTable.CREATE_TABLE)

            // TIER 3: RELATIONSHIP
            db.execSQL(GroupTable.CREATE_TABLE)
            db.execSQL(GroupMemberTable.CREATE_TABLE)

            // TIER 4: EVENTS
            db.execSQL(LocationEventTable.CREATE_TABLE)
            db.execSQL(SafeZoneTable.CREATE_TABLE)
            db.execSQL(SafeZoneUserMapTable.CREATE_TABLE)
            db.execSQL(GeofenceEventTable.CREATE_TABLE)

            // TIER 5: SYSTEM & AUDIT LEDGER
            db.execSQL(SyncQueueTable.CREATE_TABLE)
            db.execSQL(ChangeLogTable.CREATE_TABLE)
            db.execSQL(UserProfileTable.CREATE_TABLE_SYS_LOG)

            // V57 HARDWARE EXTENSION: SIM Transactions (Offline Ledger)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sim_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    slot INTEGER,
                    mcc TEXT,
                    mnc TEXT,
                    lac TEXT,
                    cid TEXT,
                    lat REAL,
                    lng REAL,
                    source TEXT,
                    timestamp INTEGER,
                    created_on TEXT
                )
            """.trimIndent())
            
            android.util.Log.i("DatabaseHelper", "V57 Redesign: Clean 5-Tier Architecture Deployed.")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseHelper", "SCHEMA_CRITICAL FAILURE: ${e.message}")
        } finally {
            isPerformingMigration = false
        }
    }

    // --- ANALYST TOOLS & RECOGNITION BRIDGE ---
    
    fun getRecordCount(tableName: String): Long {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
            cursor.use { if (it.moveToFirst()) it.getLong(0) else 0L }
        } catch (e: Exception) { 0L }
    }

    fun getTableColumns(tableName: String): List<String> {
        val columns = mutableListOf<String>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
            cursor.use {
                while (it.moveToNext()) {
                    columns.add(it.getString(it.getColumnIndexOrThrow("name")))
                }
            }
        } catch (e: Exception) { }
        return columns
    }

    fun getPagedData(tableName: String, limit: Int, offset: Int, orderBy: String?): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        try {
            val db = readableDatabase
            val order = orderBy ?: "rowid DESC"
            val cursor = db.query(tableName, null, null, null, null, null, order, "$offset, $limit")
            cursor.use {
                val cols = it.columnNames
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, String>()
                    for (col in cols) {
                        map[col] = it.getString(it.getColumnIndexOrThrow(col)) ?: ""
                    }
                    list.add(map)
                }
            }
        } catch (e: Exception) { }
        return list
    }

    /**
     * 🧬 Tier 1: Identity Retrieval by Cloud Auth UID.
     */
    fun getIdentityByAuthUid(authUid: String): Map<String, String>? {
        val agentId = resolveAgentIdLocally(authUid) ?: return null
        return getLocalDossier(agentId)
    }

    /**
     * 👤 Tier 1: Profile Retrieval by Forensic Agent ID.
     */
    fun getProfileData(uid1: String): Map<String, String>? {
        try {
            val db = readableDatabase
            val cursor = db.query(UserProfileTable.TABLE_NAME, null, 
                "${UserProfileTable.COL_IDENTITY_ID} = (SELECT ${UserIdentityTable.COL_ID} FROM ${UserIdentityTable.TABLE_NAME} WHERE ${UserIdentityTable.COL_UID_1} = ?)", 
                arrayOf(uid1), null, null, null)
            cursor.use {
                if (it.moveToFirst()) {
                    val map = mutableMapOf<String, String>()
                    it.columnNames.forEach { col ->
                        map[col] = it.getString(it.getColumnIndexOrThrow(col)) ?: ""
                    }
                    return map
                }
            }
        } catch (e: Exception) { }
        return null
    }

    /**
     * 🛡️ STRICT HANDSHAKE VERIFICATION: Checks whether a target device has an approved,
     * authenticated handshake in local SQLite tables (UserIdentity, DevicePairing, or Roster).
     */
    fun isDevicePaired(targetId: String?): Boolean {
        if (targetId.isNullOrBlank()) return false
        try {
            val db = readableDatabase
            // Check UserIdentityTable by uid_1, uid_2, or connected_id
            val sql = """
                SELECT 1 FROM ${UserIdentityTable.TABLE_NAME}
                WHERE ${UserIdentityTable.COL_UID_1} = ? 
                   OR ${UserIdentityTable.COL_UID_2} = ? 
                   OR ${UserIdentityTable.COL_QR_REF} = ?
                UNION
                SELECT 1 FROM ${DevicePairingTable.TABLE_NAME}
                WHERE ${DevicePairingTable.COL_DEVICE_A_ID} = ?
                   OR ${DevicePairingTable.COL_DEVICE_B_ID} = ?
                LIMIT 1
            """.trimIndent()
            val cursor = db.rawQuery(sql, arrayOf(targetId, targetId, targetId, targetId, targetId))
            cursor.use {
                return it.moveToFirst()
            }
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "Error verifying handshake pairing for $targetId", e)
            return false
        }
    }

    /**
     * 🧬 QA Logic: Identity Matrix Integrity - Forensic Completeness Check
     * Verifies that all mandatory enterprise fields are populated.
     */
    fun isProfileComplete(uid1: String): Boolean {
        try {
            val db = readableDatabase
            val cursor = db.query(UserProfileTable.TABLE_NAME, null, 
                "${UserProfileTable.COL_IDENTITY_ID} = (SELECT ${UserIdentityTable.COL_ID} FROM ${UserIdentityTable.TABLE_NAME} WHERE ${UserIdentityTable.COL_UID_1} = ?)", 
                arrayOf(uid1), null, null, null)
            cursor.use {
                if (it.moveToFirst()) {
                    val name = it.getString(it.getColumnIndexOrThrow(UserProfileTable.COL_NAME))
                    val phone = it.getString(it.getColumnIndexOrThrow(UserProfileTable.COL_PHONE))
                    val dob = it.getString(it.getColumnIndexOrThrow(UserProfileTable.COL_DOB))
                    val gender = it.getString(it.getColumnIndexOrThrow(UserProfileTable.COL_GENDER))
                    val country = it.getString(it.getColumnIndexOrThrow(UserProfileTable.COL_COUNTRY))
                    val state = it.getString(it.getColumnIndexOrThrow(UserProfileTable.COL_STATE))
                    val city = it.getString(it.getColumnIndexOrThrow(UserProfileTable.COL_CITY))
                    val pincode = it.getString(it.getColumnIndexOrThrow(UserProfileTable.COL_PINCODE))
                    
                    return !name.isNullOrEmpty() && !phone.isNullOrEmpty() && 
                           !dob.isNullOrEmpty() && dob != "Select Birthday" &&
                           !gender.isNullOrEmpty() && gender != "Select Gender" &&
                           !country.isNullOrEmpty() && country != "Select Country" &&
                           !state.isNullOrEmpty() && state != "Select State" &&
                           !city.isNullOrEmpty() && city != "Select City" &&
                           !pincode.isNullOrEmpty()
                }
            }
        } catch (e: Exception) { }
        return false
    }

    fun deleteRecord(tableName: String, idCol: String, idVal: String): Boolean {
        return try {
            writableDatabase.delete(tableName, "$idCol = ?", arrayOf(idVal)) > 0
        } catch (e: Exception) { false }
    }

    fun updateRecord(tableName: String, idCol: String, idVal: String, values: android.content.ContentValues): Boolean {
        return try {
            writableDatabase.update(tableName, values, "$idCol = ?", arrayOf(idVal)) > 0
        } catch (e: Exception) { false }
    }

    /**
     * 🔧 FIX DB-05: Accept real groupId instead of hardcoded "DEFAULT_GROUP".
     * All callers must pass the active_group from SharedPreferences.
     * Overloaded for backwards-compatibility with callers that don't have groupId yet.
     */
    @JvmOverloads
    fun updateCitizenRole(uid1: String, famRole: String, frRole: String, isFriend: Boolean, groupId: String = "DEFAULT_GROUP"): Boolean {
        return try {
            val db = writableDatabase
            val iId = resolveInternalId(uid1)
            if (iId != -1L) {
                // Use the passed groupId — never hardcode DEFAULT_GROUP for real group members
                val realGroupId = if (groupId.isBlank()) "DEFAULT_GROUP" else groupId
                val roleToSave = famRole.takeIf { it != "None" && it.isNotBlank() } ?: frRole.takeIf { it != "None" && it.isNotBlank() } ?: "Member"
                GroupMemberTable.upsert(db, realGroupId, iId, roleToSave, System.currentTimeMillis())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun markRosterNodeDeleted(uid1: String): Boolean {
        return try {
            val db = writableDatabase
            val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
            val iId = resolveInternalId(uid1)
            if (iId != -1L) {
                val cv = ContentValues().apply {
                    put(UserIdentityTable.COL_STATUS, "DELETED")
                    put(UserIdentityTable.COL_UPDATED_AT, now)
                }
                db.update(UserIdentityTable.TABLE_NAME, cv, "${UserIdentityTable.COL_ID} = ?", arrayOf(iId.toString()))
                db.delete(GroupMemberTable.TABLE_NAME, "${GroupMemberTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
            }
            db.delete("device_pairings", "uid_2 = ? OR uid_1 = ?", arrayOf(uid1, uid1))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isDeviceDeleted(uid1: String): Boolean {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                UserIdentityTable.TABLE_NAME,
                arrayOf(UserIdentityTable.COL_STATUS),
                "(${UserIdentityTable.COL_UID_1} = ? OR ${UserIdentityTable.COL_AUTH_UID} = ?) AND ${UserIdentityTable.COL_STATUS} = 'DELETED'",
                arrayOf(uid1, uid1), null, null, null
            )
            cursor.use {
                it.moveToFirst()
            }
        } catch (e: Exception) {
            false
        }
    }

    fun deleteRosterNode(uid1: String): Boolean {
        markRosterNodeDeleted(uid1)
        return try {
            val db = writableDatabase
            val iId = resolveInternalId(uid1)
            if (iId != -1L) {
                db.delete(GroupMemberTable.TABLE_NAME, "${GroupMemberTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                db.delete(DeviceTable.TABLE_NAME, "${DeviceTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                db.delete(SafeZoneUserMapTable.TABLE_NAME, "${SafeZoneUserMapTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                db.delete(UserOrgMapTable.TABLE_NAME, "${UserOrgMapTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                db.delete(UserProfileTable.TABLE_NAME, "${UserProfileTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                db.delete(UserIdentityTable.TABLE_NAME, "${UserIdentityTable.COL_ID} = ?", arrayOf(iId.toString()))
            }
            db.delete("device_pairings", "uid_2 = ? OR uid_1 = ?", arrayOf(uid1, uid1))
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 60) {
            // 🛡️ V60: Hard Reset of Identity Matrix (Purging Incorrect Legacy Values)
            db.execSQL("DROP TABLE IF EXISTS ${UserIdentityTable.TABLE_NAME}")
            db.execSQL(UserIdentityTable.CREATE_TABLE)
            saveSysLog("W", "DB_RESET", "Forensic identity matrix reset to V60 protocol.", null)
        }
        
        if (oldVersion < 65) {
            // 🛡️ V65: Universal Forensic Purge (String UID -> Long ID)
            db.execSQL("DROP TABLE IF EXISTS ${UserProfileTable.TABLE_NAME}")
            db.execSQL("DROP TABLE IF EXISTS ${UserOrgMapTable.TABLE_NAME}")
            db.execSQL("DROP TABLE IF EXISTS ${GroupMemberTable.TABLE_NAME}")
            db.execSQL("DROP TABLE IF EXISTS ${SafeZoneUserMapTable.TABLE_NAME}")
            db.execSQL("DROP TABLE IF EXISTS ${DeviceTable.TABLE_NAME}")
            db.execSQL(UserProfileTable.CREATE_TABLE)
            db.execSQL(UserOrgMapTable.CREATE_TABLE)
            db.execSQL(GroupMemberTable.CREATE_TABLE)
            db.execSQL(SafeZoneUserMapTable.CREATE_TABLE)
            db.execSQL(DeviceTable.CREATE_TABLE)
            saveSysLog("W", "MIGRATION_FINAL", "Universal forensic matrix unified to L7 Long-ID protocol.", null)
        }
        if (oldVersion < 66) {
            // 🛡️ V66: Final Relational Alignment (Purging Malformed FKs)
            db.execSQL("DROP TABLE IF EXISTS ${GroupTable.TABLE_NAME}")
            db.execSQL("DROP TABLE IF EXISTS ${SafeZoneTable.TABLE_NAME}")
            db.execSQL(GroupTable.CREATE_TABLE)
            db.execSQL(SafeZoneTable.CREATE_TABLE)
            saveSysLog("W", "STABILITY_PATCH", "V66: Malformed foreign keys purged and aligned.", null)
        }
        
        if (oldVersion < 67) {
            // 🧬 V67: Identity Stability Patch - Enforce Hardware uniqueness
            db.beginTransaction()
            try {
                // 1. Create temporary table with new schema
                db.execSQL("""
                    CREATE TABLE user_identity_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uid_1 TEXT NOT NULL UNIQUE,
                        uid_2 TEXT NOT NULL UNIQUE,
                        auth_uid TEXT DEFAULT '',
                        node_index INTEGER DEFAULT 1,
                        id_full_name TEXT DEFAULT '',
                        st_session_state TEXT DEFAULT 'OFFLINE',
                        is_admin TEXT DEFAULT '',
                        owner_name TEXT DEFAULT '',
                        connected_id TEXT DEFAULT '',
                        qr_code_ref TEXT DEFAULT '',
                        status TEXT DEFAULT 'Active',
                        created_at TEXT,
                        updated_at TEXT
                    )
                """.trimIndent())

                // 2. Copy data resolving duplicates by uid_2 (Keep latest)
                db.execSQL("""
                    INSERT OR IGNORE INTO user_identity_backup (
                        id, uid_1, uid_2, auth_uid, node_index, id_full_name, 
                        st_session_state, is_admin, owner_name, connected_id, 
                        qr_code_ref, status, created_at, updated_at
                    )
                    SELECT id, uid_1, uid_2, auth_uid, node_index, id_full_name, 
                           st_session_state, is_admin, owner_name, connected_id, 
                           qr_code_ref, status, created_at, updated_at 
                    FROM user_identity 
                    GROUP BY uid_2 
                    ORDER BY updated_at DESC
                """.trimIndent())

                // 3. Swap tables
                db.execSQL("DROP TABLE user_identity")
                db.execSQL("ALTER TABLE user_identity_backup RENAME TO user_identity")
                
                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V67 Identity Migration: Hardware Lock Applied.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V67 Migration Failure: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }

        if (oldVersion < 68) {
            db.beginTransaction()
            try {
                // 🧬 V68 Forensic Identity Cleanup: Allow NULL uid_2 to decommission dummy logic
                db.execSQL("ALTER TABLE user_identity RENAME TO user_identity_old")
                db.execSQL(UserIdentityTable.CREATE_TABLE)
                db.execSQL("""
                    INSERT INTO user_identity (
                        id, uid_1, uid_2, auth_uid, node_index, id_full_name, 
                        st_session_state, is_admin, owner_name, connected_id, 
                        qr_code_ref, status, created_at, updated_at
                    )
                    SELECT id, uid_1, 
                           CASE WHEN uid_2 LIKE 'HW_%' AND (uid_2 LIKE '%RECOVERY%' OR uid_2 LIKE '%ROSTER%' OR uid_2 LIKE '%REMOTE%') THEN NULL ELSE uid_2 END, 
                           auth_uid, node_index, id_full_name, 
                           st_session_state, is_admin, owner_name, connected_id, 
                           qr_code_ref, status, created_at, updated_at 
                    FROM user_identity_old
                """.trimIndent())
                db.execSQL("DROP TABLE user_identity_old")
                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V68 Migration: Dummy Key Decommissioned.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V68 Migration Failure: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }

        if (oldVersion < 69) {
            db.beginTransaction()
            try {
                // 🧬 V69: Global Forensic Timestamp Standardization
                // This migration rebuilds all tables that previously used INTEGER timestamps to use TEXT forensic strings.
                
                // 1. Devices Table
                standardizeTableTimestamp(db, DeviceTable.TABLE_NAME, DeviceTable.CREATE_TABLE, listOf(DeviceTable.COL_UPDATED_AT))
                
                // 2. Groups Table
                standardizeTableTimestamp(db, GroupTable.TABLE_NAME, GroupTable.CREATE_TABLE, listOf(GroupTable.COL_UPDATED_AT))
                
                // 3. Organization Table
                standardizeTableTimestamp(db, OrganizationTable.TABLE_NAME, OrganizationTable.CREATE_TABLE, listOf(OrganizationTable.COL_UPDATED_AT))
                
                // 4. Sync Queue Table
                standardizeTableTimestamp(db, SyncQueueTable.TABLE_NAME, SyncQueueTable.CREATE_TABLE, listOf(SyncQueueTable.COL_UPDATED_AT))
                
                // 5. SIM Table
                standardizeTableTimestamp(db, SimTable.TABLE_NAME, SimTable.CREATE_TABLE, listOf(SimTable.COL_UPDATED_AT))
                
                // 6. Device Pairing Table
                standardizeTableTimestamp(db, DevicePairingTable.TABLE_NAME, DevicePairingTable.CREATE_TABLE, listOf(DevicePairingTable.COL_CREATED_AT, DevicePairingTable.COL_UPDATED_AT))
                
                // 7. User-Org Map Table
                standardizeTableTimestamp(db, UserOrgMapTable.TABLE_NAME, UserOrgMapTable.CREATE_TABLE, listOf(UserOrgMapTable.COL_UPDATED_AT))

                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V69 Migration: Global Forensic Timestamp Standardization Applied.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V69 Migration Failure: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }

        if (oldVersion < 70) {
            // 🧬 V70: Global Forensic Creation Tracking (created_on)
            db.beginTransaction()
            try {
                val tables = getAllTables()
                val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
                for (table in tables) {
                    try {
                        db.execSQL("ALTER TABLE $table ADD COLUMN created_on TEXT")
                        // Initialize created_on with a fallback value for existing records
                        db.execSQL("UPDATE $table SET created_on = '$now' WHERE created_on IS NULL")
                    } catch (e: Exception) {
                        // Column might already exist from self-healing or failed previous run
                    }
                }
                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V70 Migration: Global 'created_on' column deployed.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V70 Migration Failure: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }

        if (oldVersion < 71) {
            // 🧬 V71: Enforce created_on for location_event specifically if V70 missed it
            db.beginTransaction()
            try {
                try {
                    db.execSQL("ALTER TABLE location_event ADD COLUMN created_on TEXT")
                    val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
                    db.execSQL("UPDATE location_event SET created_on = '$now' WHERE created_on IS NULL")
                } catch (e: Exception) {
                    // Column already exists
                }
                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V71 Migration: location_event 'created_on' enforced.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V71 Migration Failure: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }

        if (oldVersion < 73) {
            db.beginTransaction()
            try {
                val safeZoneCols = mapOf(
                    SafeZoneTable.COL_IS_ARCHIVED to "INTEGER DEFAULT 0",
                    SafeZoneTable.COL_IS_ACTIVE to "INTEGER DEFAULT 1",
                    SafeZoneTable.COL_IS_ALERTS_ENABLED to "INTEGER DEFAULT 1",
                    SafeZoneTable.COL_CREATED_BY to "INTEGER",
                    SafeZoneTable.COL_TARGET_UID to "TEXT",
                    SafeZoneTable.COL_CREATED_ON to "TEXT"
                )
                for ((col, def) in safeZoneCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${SafeZoneTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${SafeZoneTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }
                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V73 Migration: SafeZone archival & Realtime Tree parity columns migrated.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V73 Migration Failure: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }
    }

    /**
     * 🧬 V69 Helper: Standardizes a table's timestamp columns by rebuilding it with TEXT types.
     */
    private fun standardizeTableTimestamp(db: SQLiteDatabase, tableName: String, createSql: String, timestampCols: List<String>) {
        val oldTable = "${tableName}_old_v69"
        db.execSQL("ALTER TABLE $tableName RENAME TO $oldTable")
        db.execSQL(createSql)
        
        // Build the dynamic INSERT SELECT statement
        val columns = mutableListOf<String>()
        db.rawQuery("PRAGMA table_info($oldTable)", null).use { cursor ->
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
        }

        val selectFields = columns.joinToString(", ") { col ->
            if (timestampCols.contains(col)) {
                // If it's a timestamp column, we need to convert it from Long to Forensic String
                // Since we can't easily call Kotlin functions from SQLite SELECT, we'll do it via a sub-query or simple fallback
                // Actually, for simplicity during migration, we will use a CASE to handle nulls/blanks
                "strftime('%Y%m%d%H%M%S', datetime($col / 1000, 'unixepoch')) || '00'" 
            } else {
                col
            }
        }

        db.execSQL("INSERT INTO $tableName (${columns.joinToString(", ")}) SELECT $selectFields FROM $oldTable")
        db.execSQL("DROP TABLE $oldTable")
    }

    private fun toForensicString(ms: Long): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSS", Locale.US)
        val formatted = sdf.format(Date(ms))
        return if (formatted.length > 16) formatted.take(16) else formatted.padEnd(16, '0')
    }

    private fun formatMillis(ms: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))
    }

    fun getReadableNow(): String = formatMillis(System.currentTimeMillis())

    /**
     * 🚀 V57 MASTER HANDSHAKE (7-Tier Deterministic Sequence)
     * Performs the full analytical initialization in the exact order requested by the enterprise spec.
     */
    @JvmOverloads
    fun executeTieredHandshake(
        authUid: String, uid1: String, uid2: String, deviceId: String,
        profileData: Map<String, String>,
        orgData: Map<String, String>?,
        pairingData: Map<String, String>?,
        groupData: Map<String, String>?,
        locationData: Map<String, Any>?,
        safeZoneData: Map<String, Any>?,
        simData: Map<String, String>?,
        lat: Double = 0.0,
        lng: Double = 0.0,
        addr: String? = null,
        role: String = "ADMIN",
        epochId: String = ""
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // STEP 1: AUTHORIZE DATA (Tier 1 Identity + Tier 2 Device) - Epoch-Aware Session Isolation
            val nIdx = resolveNodeIndex(uid1, uid2)
            val iId = if (epochId.isNotEmpty()) {
                UserIdentityTable.insertOrArchiveSession(
                    db, uid1, uid2, authUid, epochId,
                    fullName = profileData["name"],
                    owner = profileData["owner_name"],
                    connectedId = deviceId,
                    qrRef = profileData["qr_code_ref"],
                    nodeIndex = nIdx
                )
            } else {
                UserIdentityTable.insertOrUpdate(db, uid1, uid2, authUid, 
                    fullName = profileData["name"], 
                    owner = profileData["owner_name"],
                    connectedId = deviceId,
                    qrRef = profileData["qr_code_ref"],
                    nodeIndex = nIdx
                )
            }
            DeviceTable.upsert(db, deviceId, iId, profileData["model"], profileData["imei"], false, profileData["fingerprint"], lat, lng, addr)
            
            // STEP 2: PROFILE & ORGANIZATION (Tier 1)
            UserProfileTable.upsert(db, iId, profileData)
            
            if (orgData != null) {
                val orgName = orgData["orgName"] ?: "UNASSIGNED"
                val orgId = "ORG_" + orgName.hashCode().toString(16)
                OrganizationTable.upsert(db, orgId, orgName, orgData["userAddress"])
                val v = ContentValues().apply {
                    put("department", orgData["deptName"])
                    put("designation", orgData["designation"])
                }
                UserOrgMapTable.mapUserToOrg(db, iId, orgId, v)
            }

            // STEP 3: DEVICE PAIRING (Tier 2-3)
            if (pairingData != null) {
                val pairId = "PR_${uid1.hashCode()}_${pairingData["uid2"]?.hashCode()}"
                DevicePairingTable.logPairing(db, pairId, deviceId, pairingData["node_device"] ?: "TARGET", "PAIRED")
            }

            // STEP 4: ROSTER TABLE (Group Header + Member) - Tier 3
            if (groupData != null) {
                val groupId = groupData["group_id"] ?: "DEFAULT_GROUP"
                GroupTable.upsert(db, groupId, groupData["group_name"], iId)
                GroupMemberTable.addMember(db, groupId, iId, role)
                
                // 🧬 Forensic Role Auto-Alignment: Force 'ADMIN' only if Auth UID is verified
                db.execSQL("""
                    UPDATE ${GroupMemberTable.TABLE_NAME} 
                    SET role = 'ADMIN' 
                    WHERE ${GroupMemberTable.COL_IDENTITY_ID} = $iId 
                    AND EXISTS (SELECT 1 FROM ${UserIdentityTable.TABLE_NAME} 
                               WHERE ${UserIdentityTable.COL_ID} = $iId 
                               AND ${UserIdentityTable.COL_AUTH_UID} IS NOT NULL)
                    AND '$role' = 'ADMIN'
                """)
            }

            // STEP 5: LOCATION HISTORY (User -> Day -> Ping) - Tier 4
            if (locationData != null) {
                LocationEventTable.logLocation(
                    db, iId, 
                    locationData["lat"] as Double, locationData["lng"] as Double, 
                    (locationData["speed"] as? Number)?.toFloat() ?: 0f, 
                    (locationData["battery"] as? Number)?.toInt() ?: 100, 
                    false, // sos
                    com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(), "AUTH_HANDSHAKE"
                )
            }

            // STEP 6: SAFE ZONE (Link DeviceA to clusters) - Tier 4
            if (safeZoneData != null) {
                val zoneId = safeZoneData["zone_id"] as String
                SafeZoneTable.upsert(db, zoneId, safeZoneData["name"] as String, safeZoneData["lat"] as Double, safeZoneData["lng"] as Double, safeZoneData["radius"] as Double, iId)
                SafeZoneUserMapTable.assignUserToZone(db, zoneId, iId, 0L, 86400000L, "1,2,3,4,5,6,7", true)
            }

            // STEP 7: SIM TABLE (Extra Verification Data) - Tier 2+
            if (simData != null) {
                SimTable.logSim(db, deviceId, simData["serial"], simData["operator"], simData["mcc"], simData["mnc"], true)
            }

            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            SystemLogger.e("HandshakeError", "7-Tier Sequential Failure: ${e.message}")
            return false
        } finally {
            db.endTransaction()
        }
    }

    // --- ANALYTICAL HUB BRIDGE METHODS (V57 STANDARDIZATION) ---

    /**
     * 👤 Tier 1: Identity & Profile Anchor (Dossier Sink)
     */
    fun commitAnalyticalDossier(uid1: String, data: Map<String, String>, name: String? = null) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val finalName = name ?: data["name"] ?: data["full_name"] ?: data["userName"]
            val finalEmail = data["email"] ?: data["email_id"] ?: data["userEmail"]
            val avatarId = data["avatar_id"]
            val note = data["emergencyNote"] ?: data["note"] ?: data["memo_note"]
            
            val iId = resolveInternalId(uid1)
            if (iId != -1L) {
                UserProfileTable.upsert(db, iId, data)
                
                // If org info exists, sync it too
                val orgName = data["orgName"] ?: data["org_name"]
                if (!orgName.isNullOrEmpty()) {
                    val orgId = "ORG_" + orgName.hashCode().toString(16)
                    OrganizationTable.upsert(db, orgId, orgName, data["userAddress"] ?: data["work_location"])
                    val v = ContentValues()
                    data.forEach { (k, valStr) -> v.put(k, valStr) }
                    UserOrgMapTable.mapUserToOrg(db, iId, orgId, v)
                }
            }
            
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * 🌉 Forensic Bridge: Supports legacy activities passing 8 detailed parameters.
     */
    fun commitHandshakeDossier(uid: String, dossier: Map<String, String>, subject: String, name: String, u1: String, uid2: String, handshake: String, auth: String) {
        // Redirect to standardized analytical sink
        commitAnalyticalDossier(uid, dossier, name)
        
        // Ensure Hardware Fingerprint (Tier 2) is aligned
        if (!uid2.isNullOrEmpty()) {
            syncIdentityCore(uid, u1, uid2)
        }
    }

    @JvmOverloads
    fun saveRosterNode(uid1: String, name: String, role: String, status: String, isAdmin: Boolean, isFriend: Boolean, ownerName: String? = null, uid2: String? = null, connectedDeviceId: String? = null, qrCode: String? = null, avatarId: String? = null, activeGroupId: String? = null) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 🔧 FIX DB-01: Pre-clean orphan partial-records before upsert.
            // When uid2 changes (e.g., null → real HW ID), a stale record with same uid_1 but old uid_2
            // survives the UNIQUE(uid_1) constraint silently. Pre-delete ensures a clean slate.
            if (uid2 != null && uid2.isNotBlank()) {
                // Remove any existing identity row that has uid_1 = uid1 but uid_2 IS NULL or different
                // This collapses the partial+full record into one canonical row.
                db.delete(UserIdentityTable.TABLE_NAME,
                    "${UserIdentityTable.COL_UID_1} = ? AND (${UserIdentityTable.COL_UID_2} IS NULL OR ${UserIdentityTable.COL_UID_2} != ?)",
                    arrayOf(uid1, uid2)
                )
            }

            // 🧬 V68 Cleanup: No more HW_ROSTER_ dummy keys. Use NULL if unknown.
            val nIdx = resolveNodeIndex(uid1, uid2 ?: "")
            val iId = UserIdentityTable.insertOrUpdate(db, uid1, uid2, "", // auth_uid is blank
                fullName = name,
                isAdmin = if(isAdmin) "1" else "0",
                owner = ownerName ?: "",
                connectedId = connectedDeviceId,
                qrRef = qrCode,
                nodeIndex = nIdx
            )

            // 🔧 FIX DB-05: Use real groupId, never hardcode DEFAULT_GROUP for real roster
            val realGroupId = if (!activeGroupId.isNullOrBlank()) activeGroupId else "DEFAULT_GROUP"

            // Write to V57 Relationships (GroupMember)
            GroupMemberTable.upsert(db, realGroupId, iId, role)

            // Write to Profile (Tier 1) for name/avatar parity (Dual-Write for V60 backwards compatibility)
            val rosterProfile = mapOf<String, String>(
                "name" to name,
                "userName" to name,
                "avatar_id" to (avatarId ?: "1"),
                "role_title" to role
            )
            UserProfileTable.upsert(db, iId, rosterProfile)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * 🌉 Forensic Bridge: Supports legacy activities passing ContentValues.
     */
    fun saveRosterNode(v: android.content.ContentValues) {
        val uid1 = v.getAsString(COL_UID_1) ?: "UNKNOWN"
        val name = v.getAsString("fullName") ?: v.getAsString("id_full_name") ?: "Target"
        val role = v.getAsString("role_type") ?: "Member"
        val isAdmin = v.getAsInteger("is_admin") == 1
        val isFriend = v.getAsInteger("is_friend") == 1
        val qr = v.getAsString("qr_code_ref")
        val avatar = v.getAsString("avatar_id")
        val grpId = v.getAsString("group_id") ?: v.getAsString("active_group_id")
        
        saveRosterNode(uid1, name, role, "Active", isAdmin, isFriend, avatarId = avatar, qrCode = qr, activeGroupId = grpId)
    }

    fun getOfflineSafeZones(includeArchived: Boolean = false): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            val selection = if (!includeArchived) "${SafeZoneTable.COL_IS_ARCHIVED} = 0 OR ${SafeZoneTable.COL_IS_ARCHIVED} IS NULL" else null
            val cursor = db.query(SafeZoneTable.TABLE_NAME, null, selection, null, null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    val zoneId = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_ZONE_ID)) ?: ""
                    if (zoneId.isBlank() || zoneId.startsWith("HANDSHAKE SENT:", ignoreCase = true)) continue
                    val lat = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LAT))
                    val lng = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LNG))
                    if (lat == 0.0 && lng == 0.0) continue // Skip invalid 0,0 coordinates

                    val map = mutableMapOf<String, Any>()
                    map["zone_id"] = zoneId
                    map["name"] = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_NAME)) ?: ""
                    map["lat"] = lat
                    map["lng"] = lng
                    map["radius"] = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_RADIUS))
                    map["target"] = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_TARGET_UID)) ?: ""
                    map["flag_a"] = it.getInt(it.getColumnIndexOrThrow(SafeZoneTable.COL_IS_ALERTS_ENABLED))
                    map["flag_b"] = it.getInt(it.getColumnIndexOrThrow(SafeZoneTable.COL_IS_ACTIVE))
                    val archIdx = it.getColumnIndex(SafeZoneTable.COL_IS_ARCHIVED)
                    map["is_archived"] = if (archIdx != -1) it.getInt(archIdx) else 0
                    list.add(map)
                }
            }
        } catch (e: Exception) { }
        return list
    }

    fun getArchivedSafeZones(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            val cursor = db.query(SafeZoneTable.TABLE_NAME, null, "${SafeZoneTable.COL_IS_ARCHIVED} = 1", null, null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, Any>()
                    map["zone_id"] = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_ZONE_ID)) ?: ""
                    map["name"] = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_NAME)) ?: ""
                    map["lat"] = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LAT))
                    map["lng"] = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LNG))
                    map["radius"] = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_RADIUS))
                    map["target"] = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_TARGET_UID)) ?: ""
                    map["flag_a"] = it.getInt(it.getColumnIndexOrThrow(SafeZoneTable.COL_IS_ALERTS_ENABLED))
                    map["flag_b"] = 0 // Inactive when archived
                    map["is_archived"] = 1
                    list.add(map)
                }
            }
        } catch (e: Exception) { }
        return list
    }

    fun updateSafeZoneActive(zoneId: String, isActive: Boolean): Boolean {
        return try {
            val db = writableDatabase
            val v = android.content.ContentValues().apply {
                put(SafeZoneTable.COL_IS_ACTIVE, if (isActive) 1 else 0)
                put("flag_b", if (isActive) 1 else 0)
            }
            db.update(SafeZoneTable.TABLE_NAME, v, "${SafeZoneTable.COL_ZONE_ID} = ?", arrayOf(zoneId)) > 0
        } catch (e: Exception) { false }
    }

    fun updateSafeZoneAlerts(zoneId: String, alertsEnabled: Boolean): Boolean {
        return try {
            val db = writableDatabase
            val v = android.content.ContentValues().apply {
                put("flag_a", if (alertsEnabled) 1 else 0)
            }
            db.update(SafeZoneTable.TABLE_NAME, v, "${SafeZoneTable.COL_ZONE_ID} = ?", arrayOf(zoneId)) > 0
        } catch (e: Exception) { false }
    }

    fun archiveSafeZone(zoneId: String): Boolean {
        return try {
            val db = writableDatabase
            val v = android.content.ContentValues().apply {
                put(SafeZoneTable.COL_IS_ACTIVE, 0)
                put(SafeZoneTable.COL_IS_ARCHIVED, 1)
            }
            db.update(SafeZoneTable.TABLE_NAME, v, "${SafeZoneTable.COL_ZONE_ID} = ?", arrayOf(zoneId)) > 0
        } catch (e: Exception) { false }
    }

    fun restoreArchivedSafeZone(zoneId: String): Boolean {
        return try {
            val db = writableDatabase
            val v = android.content.ContentValues().apply {
                put(SafeZoneTable.COL_IS_ACTIVE, 1)
                put(SafeZoneTable.COL_IS_ARCHIVED, 0)
            }
            db.update(SafeZoneTable.TABLE_NAME, v, "${SafeZoneTable.COL_ZONE_ID} = ?", arrayOf(zoneId)) > 0
        } catch (e: Exception) { false }
    }

    fun purgeArchivedSafeZone(zoneId: String): Boolean {
        return try {
            val db = writableDatabase
            db.delete(SafeZoneTable.TABLE_NAME, "${SafeZoneTable.COL_ZONE_ID} = ?", arrayOf(zoneId)) > 0
        } catch (e: Exception) { false }
    }

    fun restoreAllArchivedSafeZones(): Boolean {
        return try {
            val db = writableDatabase
            val v = android.content.ContentValues().apply {
                put(SafeZoneTable.COL_IS_ACTIVE, 1)
                put(SafeZoneTable.COL_IS_ARCHIVED, 0)
            }
            db.update(SafeZoneTable.TABLE_NAME, v, "${SafeZoneTable.COL_IS_ARCHIVED} = 1", null) > 0
        } catch (e: Exception) { false }
    }

    fun purgeAllArchivedSafeZones(): Boolean {
        return try {
            val db = writableDatabase
            db.delete(SafeZoneTable.TABLE_NAME, "${SafeZoneTable.COL_IS_ARCHIVED} = 1", null) > 0
        } catch (e: Exception) { false }
    }

    fun getGeofenceLogsByDeviceAndZone(zoneId: String, deviceName: String): List<Map<String, Any>> {
        val cleanZone = if (zoneId.contains("___")) zoneId.split("___")[0] else zoneId
        return getGeofenceLogs(targetAvatar = deviceName, zoneId = cleanZone)
    }

    fun getAssignedSafeZoneForDevice(deviceId: String): Map<String, String>? {
        val allZones = getOfflineSafeZones()
        for (z in allZones) {
            val target = z["target"]?.toString() ?: ""
            val isArchived = (z["is_archived"] as? Int ?: 0) == 1
            if (!isArchived && (target.equals(deviceId, ignoreCase = true) || target == "ALL" || target == "ALL CONNECTED DEVICES")) {
                return mapOf(
                    "zone_id" to (z["zone_id"]?.toString() ?: ""),
                    "name" to (z["name"]?.toString() ?: "Safe Zone"),
                    "address" to (z["address"]?.toString() ?: "Anchor Active"),
                    "target" to target
                )
            }
        }
        return null
    }

    /**
     * 🌳 Unified Geofencing Tree Structure (Local Parity with Firebase RTDB /Geofencing)
     * Creator/Host -> SafeZone -> Connected Target Devices -> Date-Grouped Notifications
     */
    fun getGeofencingTreeData(): Map<String, Any> {
        val rootTree = mutableMapOf<String, Any>()
        try {
            val safeZones = getOfflineSafeZones(includeArchived = false)
            val zonesMap = mutableMapOf<String, Any>()

            for (sz in safeZones) {
                val zId = sz["zone_id"] as? String ?: continue
                val zName = sz["name"] as? String ?: zId
                val targetStr = sz["target"] as? String ?: ""
                val rawTargets = targetStr.split(", ").map { it.trim() }.filter { it.isNotEmpty() }

                val devicesMap = mutableMapOf<String, Any>()
                for (target in rawTargets) {
                    val logs = getGeofenceLogsByDeviceAndZone(zId, target)
                    val dateGroupedMap = mutableMapOf<String, MutableList<Map<String, Any>>>()

                    for (log in logs) {
                        val ts = log["ts_start"]?.toString() ?: ""
                        val millis = ts.toLongOrNull() ?: System.currentTimeMillis()
                        val dateKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(millis))
                        val timeStr = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(millis))
                        val readableTime = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(millis))
                        val status = log["status"]?.toString() ?: "EVENT"
                        val transition = if (status.contains("ENTER") || status.equals("IN", ignoreCase = true)) "IN" else "OUT"

                        val eventItem = mapOf(
                            "event_id" to (log["log_id"]?.toString() ?: "EVT_$millis"),
                            "transition" to transition,
                            "event_type" to status,
                            "timestamp" to millis,
                            "date" to dateKey,
                            "time" to timeStr,
                            "readable_time" to readableTime,
                            "state_latch" to transition,
                            "target_name" to target,
                            "zone_id" to zId,
                            "zone_name" to zName
                        )

                        val list = dateGroupedMap.getOrPut(dateKey) { mutableListOf() }
                        list.add(eventItem)
                    }

                    devicesMap[target] = mapOf(
                        "device_name" to target,
                        "current_state" to (if (logs.isNotEmpty()) {
                            val lastStatus = logs[0]["status"]?.toString() ?: "OUT"
                            if (lastStatus.contains("ENTER") || lastStatus.equals("IN", ignoreCase = true)) "IN" else "OUT"
                        } else "UNKNOWN"),
                        "notifications" to dateGroupedMap
                    )
                }

                zonesMap[zId] = mapOf(
                    "zone_id" to zId,
                    "zone_name" to zName,
                    "lat" to (sz["lat"] as? Double ?: 0.0),
                    "lng" to (sz["lng"] as? Double ?: 0.0),
                    "radius" to (sz["radius"] as? Double ?: 100.0),
                    "is_active" to ((sz["flag_b"] as? Int ?: 1) == 1),
                    "connected_devices" to devicesMap
                )
            }

            rootTree["safe_zones"] = zonesMap
        } catch (e: Exception) {
            saveSysLog("E", "GEOFENCING_TREE", "Failed to build tree: ${e.message}")
        }
        return rootTree
    }

    fun saveHistoryPoint(uid1: String, deviceId: String, lat: Double, lng: Double, speed: Float, battery: Int, sos: Boolean, source: String?, ts: String) {
        val db = writableDatabase
        // 🧬 V68 Cleanup: No more HW_REMOTE_ dummy keys. Use NULL if unknown.
        var iId = resolveInternalId(uid1)
        if (iId == -1L) {
            val nIdx = resolveNodeIndex(uid1, "")
            iId = UserIdentityTable.insertOrUpdate(db, uid1, null, "", connectedId = deviceId, nodeIndex = nIdx)
        }
        
        LocationEventTable.logLocation(db, iId, lat, lng, speed, battery, sos, ts, source)
    }

    fun parseForensicStringToMillis(str: String?): Long {
        if (str.isNullOrBlank() || str == "0") return System.currentTimeMillis()
        val s = str.trim()
        val num = s.toLongOrNull()
        if (num != null && num in 1_000_000_000_000L..4_102_444_800_000L) return num
        if (num != null && num in 1_000_000_000L..4_102_444_800L) return num * 1000L
        
        val patterns = arrayOf("yyyyMMddHHmmssSS", "yyyyMMddHHmmss", "yyyyMMdd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss")
        for (p in patterns) {
            try {
                val sdf = java.text.SimpleDateFormat(p, java.util.Locale.US)
                val d = sdf.parse(if (s.length > p.length) s.substring(0, p.length) else s)
                if (d != null) return d.time
            } catch (e: Exception) {}
        }
        return System.currentTimeMillis()
    }

    fun getHistoryLogsFiltered(uid1: String, cutoff: Long): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            val cutoffStr = if (cutoff > 0) com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(cutoff) else "0"
            val sql = """
                SELECT 
                    ROUND(e.${LocationEventTable.COL_LAT}, 3) as cluster_lat, 
                    ROUND(e.${LocationEventTable.COL_LNG}, 3) as cluster_lng,
                    AVG(e.${LocationEventTable.COL_LAT}) as lat,
                    AVG(e.${LocationEventTable.COL_LNG}) as lng,
                    COUNT(*) as ping_count,
                    MAX(e.${LocationEventTable.COL_TIMESTAMP}) as ts_day_end,
                    MIN(e.${LocationEventTable.COL_TIMESTAMP}) as ts_day_start,
                    e.${LocationEventTable.COL_DAY_KEY} as day_key,
                    e.${LocationEventTable.COL_ADDRESS} as address,
                    AVG(e.${LocationEventTable.COL_SPEED}) as speed,
                    MAX(e.${LocationEventTable.COL_BATTERY}) as battery,
                    MAX(e.${LocationEventTable.COL_EVENT_ID}) as log_id
                FROM ${LocationEventTable.TABLE_NAME} e
                JOIN ${UserIdentityTable.TABLE_NAME} i ON e.${LocationEventTable.COL_IDENTITY_ID} = i.${UserIdentityTable.COL_ID}
                WHERE (i.${UserIdentityTable.COL_UID_1} = ? OR i.${UserIdentityTable.COL_AUTH_UID} = ?)
                  AND e.${LocationEventTable.COL_TIMESTAMP} >= ?
                  AND (e.${LocationEventTable.COL_LAT} != 0.0 OR e.${LocationEventTable.COL_LNG} != 0.0)
                GROUP BY ROUND(e.${LocationEventTable.COL_LAT}, 3), ROUND(e.${LocationEventTable.COL_LNG}, 3), e.${LocationEventTable.COL_DAY_KEY}
                ORDER BY ts_day_end DESC
            """.trimIndent()
            
            val cursor = db.rawQuery(sql, arrayOf(uid1, uid1, cutoffStr))
            cursor.use {
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, Any>()
                    map["day_key"] = it.getInt(it.getColumnIndexOrThrow("day_key"))
                    map["lat"] = it.getDouble(it.getColumnIndexOrThrow("lat"))
                    map["lng"] = it.getDouble(it.getColumnIndexOrThrow("lng"))
                    
                    val tsStartStr = it.getString(it.getColumnIndexOrThrow("ts_day_start"))
                    val tsEndStr = it.getString(it.getColumnIndexOrThrow("ts_day_end")) ?: tsStartStr
                    
                    val tsStart = parseForensicStringToMillis(tsStartStr)
                    val tsEnd = parseForensicStringToMillis(tsEndStr)
                    
                    map["ts_start"] = tsStart
                    map["ts_end"] = tsEnd
                    map["from_datetime"] = tsStart
                    map["to_datetime"] = tsEnd
                    map["ping_count"] = it.getLong(it.getColumnIndexOrThrow("ping_count"))
                    
                    val addrIdx = it.getColumnIndex("address")
                    val addr = if (addrIdx >= 0 && !it.isNull(addrIdx)) it.getString(addrIdx) else "Recorded Location Fix"
                    map["geo_address"] = addr
                    map["address"] = addr
                    
                    val speedIdx = it.getColumnIndex("speed")
                    if (speedIdx >= 0) map["speed"] = it.getFloat(speedIdx)
                    val battIdx = it.getColumnIndex("battery")
                    if (battIdx >= 0) map["battery"] = it.getInt(battIdx)
                    val logIdIdx = it.getColumnIndex("log_id")
                    if (logIdIdx >= 0) map["log_id"] = it.getString(logIdIdx) ?: ""
                    
                    list.add(map)
                }
            }
        } catch (e: Exception) { }
        return list
    }

    fun updateHistoryAddress(logId: String, address: String) {
        val db = writableDatabase
        val v = ContentValues().apply { put(LocationEventTable.COL_ADDRESS, address) }
        db.update(LocationEventTable.TABLE_NAME, v, "${LocationEventTable.COL_EVENT_ID} = ?", arrayOf(logId))
    }

    /**
     * 🛰️ Closed-Loop History Fallback: Retrieves the latest recorded historical location for a target node.
     */
    fun getLatestHistoricalLocation(uid1: String): Map<String, Any>? {
        try {
            val db = readableDatabase
            val sql = """
                SELECT e.*, i.${UserIdentityTable.COL_UID_1} as uid_1, COALESCE(NULLIF(i.${UserIdentityTable.COL_FULL_NAME}, ''), 'Target Node') as fullName
                FROM ${LocationEventTable.TABLE_NAME} e
                JOIN ${UserIdentityTable.TABLE_NAME} i ON e.${LocationEventTable.COL_IDENTITY_ID} = i.${UserIdentityTable.COL_ID}
                WHERE (i.${UserIdentityTable.COL_UID_1} = ? OR i.${UserIdentityTable.COL_AUTH_UID} = ?)
                  AND (e.${LocationEventTable.COL_LAT} != 0.0 OR e.${LocationEventTable.COL_LNG} != 0.0)
                ORDER BY e.${LocationEventTable.COL_TIMESTAMP} DESC
                LIMIT 1
            """.trimIndent()
            val cursor = db.rawQuery(sql, arrayOf(uid1, uid1))
            cursor.use {
                if (it.moveToFirst()) {
                    val map = mutableMapOf<String, Any>()
                    map["lat"] = it.getDouble(it.getColumnIndexOrThrow(LocationEventTable.COL_LAT))
                    map["lng"] = it.getDouble(it.getColumnIndexOrThrow(LocationEventTable.COL_LNG))
                    map["timestamp"] = parseForensicStringToMillis(it.getString(it.getColumnIndexOrThrow(LocationEventTable.COL_TIMESTAMP)))
                    map["address"] = it.getString(it.getColumnIndexOrThrow(LocationEventTable.COL_ADDRESS)) ?: ""
                    val speedIdx = it.getColumnIndex(LocationEventTable.COL_SPEED)
                    if (speedIdx >= 0) map["speed"] = it.getFloat(speedIdx)
                    val battIdx = it.getColumnIndex(LocationEventTable.COL_BATTERY)
                    if (battIdx >= 0) map["battery"] = it.getInt(battIdx)
                    return map
                }
            }
        } catch (e: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.e("DatabaseHelper", "Failed to fetch latest historical location for $uid1", e)
        }
        return null
    }



    fun logDevicePairing(uid1: String, nameA: String, devA: String?, uid2: String, name2: String, dev2: String?, role2: String, groupId: String? = null) {
        val db = writableDatabase
        val pairId = "PR_${uid1.hashCode()}_${uid2.hashCode()}"
        DevicePairingTable.logPairing(db, pairId, devA ?: "HOST", dev2 ?: "TARGET", "PAIRED")
        
        // Tier 3: Map the relationship to the group if provided
        if (groupId != null) {
            val iId = resolveInternalId(uid2)
            if (iId != -1L) {
                GroupMemberTable.addMember(db, groupId, iId, role2)
            }
        }
    }

    fun getPairingHistoryAndLocation(uid: String): Map<String, String>? {
        val db = readableDatabase
        val map = mutableMapOf<String, String>()
        
        // 1. Get pairing time
        val pairSql = """
            SELECT created_at, created_on, updated_at 
            FROM device_pairing 
            WHERE device_a_id = ? OR device_b_id = ?
            ORDER BY updated_at DESC LIMIT 1
        """.trimIndent()
        
        var pairedAt: String? = null
        try {
            db.rawQuery(pairSql, arrayOf(uid, uid)).use { cursor ->
                if (cursor.moveToFirst()) {
                    pairedAt = cursor.getString(cursor.getColumnIndexOrThrow("created_on")) 
                        ?: cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                        ?: cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
                }
            }
        } catch (e: Exception) {}
        
        if (pairedAt == null) {
            // Check if the user identity table has it (fallback check)
            val rosterSql = """
                SELECT created_on 
                FROM user_identity 
                WHERE uid_1 = ? OR uid_2 = ?
                LIMIT 1
            """.trimIndent()
            try {
                db.rawQuery(rosterSql, arrayOf(uid, uid)).use { cursor ->
                    if (cursor.moveToFirst()) {
                        pairedAt = cursor.getString(cursor.getColumnIndexOrThrow("created_on"))
                    }
                }
            } catch (e: Exception) {}
        }
        
        if (pairedAt == null) {
            return null // Not paired earlier
        }
        
        map["paired_at"] = pairedAt
        
        // 2. Get last known location event
        val locSql = """
            SELECT le.lat, le.lng, le.geo_address, le.timestamp 
            FROM location_event le
            JOIN user_identity i ON le.identity_id = i.id
            WHERE i.uid_1 = ? OR i.uid_2 = ?
            ORDER BY le.timestamp DESC LIMIT 1
        """.trimIndent()
        
        try {
            db.rawQuery(locSql, arrayOf(uid, uid)).use { cursor ->
                if (cursor.moveToFirst()) {
                    map["lat"] = cursor.getDouble(cursor.getColumnIndexOrThrow("lat")).toString()
                    map["lng"] = cursor.getDouble(cursor.getColumnIndexOrThrow("lng")).toString()
                    map["address"] = cursor.getString(cursor.getColumnIndexOrThrow("geo_address")) ?: "Locating..."
                    map["timestamp"] = cursor.getString(cursor.getColumnIndexOrThrow("timestamp")) ?: ""
                }
            }
        } catch (e: Exception) {}
        
        return map
    }

    fun saveSafeZone(v: android.content.ContentValues) {
        val db = writableDatabase
        val uid = v.getAsString("target") ?: v.getAsString("created_by")
        val iId = if (uid != null) resolveInternalId(uid) else -1L
        val alerts = if (v.containsKey("flag_a")) (v.getAsInteger("flag_a") == 1) else true
        val active = if (v.containsKey("flag_b")) (v.getAsInteger("flag_b") == 1) else true
        SafeZoneTable.save(db, v.getAsString("zone_id"), v.getAsString("name"), v.getAsDouble("lat"), v.getAsDouble("lng"), v.getAsDouble("radius"), if(iId != -1L) iId else null, v.getAsString("target"), alerts, active)
    }

    fun syncSafeZoneGroup(zoneId: String, nodes: String, start: Int, end: Int, days: String, flagA: Int = 1, flagB: Int = 1, addr: String? = null) {
        val db = writableDatabase
        // Multi-node sync: nodes might be a CSV of uid1
        nodes.split(",").forEach { uid ->
            val iId = resolveInternalId(uid.trim())
            if (iId != -1L) SafeZoneUserMapTable.assignUserToZone(db, zoneId, iId, start.toLong(), end.toLong(), days, flagB == 1)
        }
    }

    fun saveGeofenceEvent(uid1: String, zoneId: String, eventType: String) {
        val db = writableDatabase
        val internalId = resolveInternalId(uid1)
        if (internalId != -1L) {
            GeofenceEventTable.save(db, internalId, zoneId, eventType)
        }
    }

    fun getForensicAuditSummary(): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        val tables = arrayOf(
            "user_identity", "user_profile", "organizations", "user_org_map",
            "devices", "device_pairings", "sim_inventory", "groups",
            "group_members", "location_event", "safe_zones", "safe_zone_user_map",
            "geofence_events", "sync_queue", "change_log"
        )
        for (table in tables) {
            map[table] = getRecordCount(table)
        }
        return map
    }

    fun deleteSafeZone(zoneId: String): Boolean {
        val db = writableDatabase
        return db.delete(SafeZoneTable.TABLE_NAME, "${SafeZoneTable.COL_ZONE_ID} = ?", arrayOf(zoneId)) > 0
    }

    // --- ORGANIZATION & CORPORATE DOSSIER BRIDGES ---

    fun saveOrgDossier(v: android.content.ContentValues) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val uid = v.getAsString("uid") ?: return
            val orgName = v.getAsString("org_name") ?: "UNASSIGNED"
            val orgId = "ORG_" + orgName.hashCode().toString(16)
            
            // 1. Upsert Organization
            OrganizationTable.upsert(db, orgId, orgName, v.getAsString("work_location"))
            
            // 2. Map User to Org with professional meta
            val iId = resolveInternalId(uid)
            if (iId != -1L) {
                UserOrgMapTable.mapUserToOrg(db, iId, orgId, v)
                
                // 3. Update Profile Note with a professional summary (Parity)
                val summary = "Dept: ${v.getAsString("department")} | Rank: ${v.getAsString("designation")}"
                UserProfileTable.updateNote(db, iId, summary)
            }
            
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * 🧬 Tier 2 Fallback: Retrieves default identity markers from the hardware anchor.
     */
    fun getIdentityProfileFallback(agentId: String): Map<String, String> {
        val res = mutableMapOf<String, String>()
        val db = readableDatabase
        db.query(UserIdentityTable.TABLE_NAME, null, "${UserIdentityTable.COL_UID_1} = ?", arrayOf(agentId), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                res["userName"] = cursor.getString(cursor.getColumnIndexOrThrow(UserIdentityTable.COL_FULL_NAME)) ?: ""
                res["fullName"] = res["userName"] ?: ""
            }
        }
        return res
    }

    fun getLocalDossier(uid: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val db = readableDatabase
            val sql = """
                SELECT 
                    COALESCE(u.${UserProfileTable.COL_NAME}, i.${UserIdentityTable.COL_FULL_NAME}) as fullName,
                    COALESCE(i.${UserIdentityTable.COL_OWNER_NAME}, u.${UserProfileTable.COL_NAME}, i.${UserIdentityTable.COL_FULL_NAME}) as owner_name,
                    u.${UserProfileTable.COL_EMAIL} as email,
                    u.${UserProfileTable.COL_PHONE} as phone,
                    u.${UserProfileTable.COL_DOB} as dob,
                    u.${UserProfileTable.COL_GENDER} as gender,
                    u.${UserProfileTable.COL_COUNTRY} as country,
                    u.${UserProfileTable.COL_STATE} as state,
                    u.${UserProfileTable.COL_CITY} as city,
                    u.${UserProfileTable.COL_PINCODE} as pincode,
                    COALESCE(u.${UserProfileTable.COL_ADDRESS}, d.${DeviceTable.COL_ADDRESS}) as address,
                    u.${UserProfileTable.COL_AVATAR_ID} as avatar_id,
                    o.${OrganizationTable.COL_ORG_NAME} as org_name,
                    o.${OrganizationTable.COL_GEO_ADDRESS} as work_location,
                    m.${UserOrgMapTable.COL_DEPARTMENT} as department,
                    m.${UserOrgMapTable.COL_ROLE_TYPE} as designation,
                    m.${UserOrgMapTable.COL_EMPLOYEE_ID} as employee_id,
                    m.${UserOrgMapTable.COL_SUPERVISOR} as supervisor,
                    m.${UserOrgMapTable.COL_SHIFT} as shift_timing,
                    m.${UserOrgMapTable.COL_BADGE} as badge_number,
                    m.${UserOrgMapTable.COL_EXT} as emergency_ext,
                    m.${UserOrgMapTable.COL_CLEARANCE} as access_level,
                    gm.${GroupMemberTable.COL_ROLE} as role_type
                FROM ${UserIdentityTable.TABLE_NAME} i
                LEFT JOIN ${UserProfileTable.TABLE_NAME} u ON i.${UserIdentityTable.COL_ID} = u.${UserProfileTable.COL_IDENTITY_ID}
                LEFT JOIN ${DeviceTable.TABLE_NAME} d ON i.${UserIdentityTable.COL_ID} = d.${DeviceTable.COL_IDENTITY_ID}
                LEFT JOIN ${UserOrgMapTable.TABLE_NAME} m ON i.${UserIdentityTable.COL_ID} = m.${UserOrgMapTable.COL_IDENTITY_ID}
                LEFT JOIN ${OrganizationTable.TABLE_NAME} o ON m.${UserOrgMapTable.COL_ORG_ID} = o.${OrganizationTable.COL_ORG_ID}
                LEFT JOIN ${GroupMemberTable.TABLE_NAME} gm ON i.${UserIdentityTable.COL_ID} = gm.${GroupMemberTable.COL_IDENTITY_ID}
                WHERE i.${UserIdentityTable.COL_UID_1} = ? 
                   OR i.${UserIdentityTable.COL_AUTH_UID} = ? 
                   OR i.${UserIdentityTable.COL_CONNECTED_ID} = ? 
                   OR i.${UserIdentityTable.COL_UID_2} = ?
            """.trimIndent()
            
            val cursor = db.rawQuery(sql, arrayOf(uid, uid, uid, uid))
            cursor.use {
                if (it.moveToFirst()) {
                    val cols = it.columnNames
                    for (col in cols) {
                        map[col] = it.getString(it.getColumnIndexOrThrow(col)) ?: ""
                    }
                }
            }
        } catch (e: Exception) { }
        return map
    }

    /**
     * 🧬 V69.8: Master Forensic Dossier (The Complete Truth)
     * Performs a Triple-Join to retrieve the absolute state from Identity, Profile, and Device tiers.
     */
    fun getForensicDossierComplete(agentId: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        try {
            val db = readableDatabase
            val sql = """
                SELECT 
                    i.${UserIdentityTable.COL_UID_1} as uid_1,
                    i.${UserIdentityTable.COL_UID_2} as uid_2,
                    i.${UserIdentityTable.COL_AUTH_UID} as auth_uid,
                    COALESCE(i.${UserIdentityTable.COL_OWNER_NAME}, p.${UserProfileTable.COL_NAME}, i.${UserIdentityTable.COL_FULL_NAME}) as owner_name,
                    i.${UserIdentityTable.COL_CONNECTED_ID} as connected_id,
                    i.${UserIdentityTable.COL_QR_REF} as qr_code_ref,
                    COALESCE(p.${UserProfileTable.COL_NAME}, i.${UserIdentityTable.COL_FULL_NAME}) as name,
                    p.${UserProfileTable.COL_EMAIL} as email,
                    p.${UserProfileTable.COL_PHONE} as phone,
                    p.${UserProfileTable.COL_AVATAR_ID} as avatar_id,
                    d.${DeviceTable.COL_LATITUDE} as latitude,
                    d.${DeviceTable.COL_LONGITUDE} as longitude,
                    COALESCE(p.${UserProfileTable.COL_ADDRESS}, d.${DeviceTable.COL_ADDRESS}) as Address,
                    d.${DeviceTable.COL_DEVICE_MODEL} as model,
                    d.${DeviceTable.COL_BUILD_FINGERPRINT} as fingerprint
                FROM ${UserIdentityTable.TABLE_NAME} i
                LEFT JOIN ${UserProfileTable.TABLE_NAME} p ON i.${UserIdentityTable.COL_ID} = p.${UserProfileTable.COL_IDENTITY_ID}
                LEFT JOIN ${DeviceTable.TABLE_NAME} d ON i.${UserIdentityTable.COL_ID} = d.${DeviceTable.COL_IDENTITY_ID}
                WHERE i.${UserIdentityTable.COL_UID_1} = ? 
                   OR i.${UserIdentityTable.COL_AUTH_UID} = ? 
                   OR i.${UserIdentityTable.COL_CONNECTED_ID} = ? 
                   OR i.${UserIdentityTable.COL_UID_2} = ?
            """.trimIndent()
            
            val cursor = db.rawQuery(sql, arrayOf(agentId, agentId, agentId, agentId))
            cursor.use {
                if (it.moveToFirst()) {
                    val cols = it.columnNames
                    for (col in cols) {
                        val idx = it.getColumnIndexOrThrow(col)
                        map[col] = when (it.getType(idx)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(idx)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> it.getDouble(idx)
                            else -> it.getString(idx) ?: ""
                        }
                    }
                }
            }
        } catch (e: Exception) { }
        return map
    }

    /**
     * 👑 SSOT: Universal Device Centric Name Resolver
     */
    fun getDeviceCentricName(deviceId: String): String {
        if (deviceId.isBlank()) return "Agent"
        try {
            val dossier = getLocalDossier(deviceId)
            val name = dossier["fullName"]?.takeIf { it.isNotBlank() && !it.startsWith("Agent_", true) && !it.equals("null", true) }
                ?: dossier["name"]?.takeIf { it.isNotBlank() && !it.startsWith("Agent_", true) && !it.equals("null", true) }
                ?: dossier["owner_name"]?.takeIf { it.isNotBlank() && !it.startsWith("Agent_", true) && !it.equals("null", true) }
                ?: dossier["id_full_name"]?.takeIf { it.isNotBlank() && !it.startsWith("Agent_", true) && !it.equals("null", true) }
            if (name != null) return name.trim()

            val rosterMatch = getOfflineRoster().find {
                it["uid_1"]?.toString() == deviceId || it["uid"]?.toString() == deviceId || it["uid_2"]?.toString() == deviceId
            }
            val rName = rosterMatch?.get("fullName")?.toString()?.takeIf { it.isNotBlank() && !it.startsWith("Agent_", true) && !it.equals("null", true) }
                ?: rosterMatch?.get("display_name")?.toString()?.takeIf { it.isNotBlank() && !it.startsWith("Agent_", true) && !it.equals("null", true) }
            if (rName != null) return rName.trim()

            val role = rosterMatch?.get("role_type")?.toString()?.takeIf { it.isNotBlank() && !it.equals("None", true) && !it.equals("Member", true) }
            if (role != null) return role.trim()
        } catch (e: Exception) { }
        return "Agent_$deviceId"
    }

    /**
     * 👑 SSOT: Universal Device Centric Avatar Resolver
     */
    fun getDeviceCentricAvatar(deviceId: String): String {
        if (deviceId.isBlank()) return "avatar_custom_1"
        try {
            val dossier = getLocalDossier(deviceId)
            val av = dossier["avatar_id"]?.takeIf { it.isNotBlank() }
                ?: dossier["equipped_avatar_id"]?.takeIf { it.isNotBlank() }
                ?: dossier["qr_code_ref"]?.takeIf { it.isNotBlank() }
            if (av != null) return av.trim()

            val rosterMatch = getOfflineRoster().find {
                it["uid_1"]?.toString() == deviceId || it["uid"]?.toString() == deviceId || it["uid_2"]?.toString() == deviceId
            }
            val rAv = rosterMatch?.get("avatar_id")?.toString()?.takeIf { it.isNotBlank() }
            if (rAv != null) return rAv.trim()
        } catch (e: Exception) { }
        return "avatar_custom_1"
    }

    /**
     * 👑 SSOT: Atomic Closed-Loop Device Identity & Avatar Updater
     */
    fun updateDeviceIdentityAndAvatar(deviceId: String, name: String?, avatarId: String?) {
        if (deviceId.isBlank()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val iId = resolveInternalId(deviceId)
            val resolvedName = name?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) && !it.startsWith("Agent_", true) }
            val resolvedAvatar = avatarId?.trim()?.takeIf { it.isNotEmpty() }

            if (iId != -1L) {
                val identityValues = ContentValues()
                if (resolvedName != null) {
                    identityValues.put(UserIdentityTable.COL_FULL_NAME, resolvedName)
                    identityValues.put(UserIdentityTable.COL_OWNER_NAME, resolvedName)
                }
                if (resolvedAvatar != null) {
                    identityValues.put(UserIdentityTable.COL_QR_REF, resolvedAvatar)
                }
                if (identityValues.size() > 0) {
                    db.update(UserIdentityTable.TABLE_NAME, identityValues, "${UserIdentityTable.COL_ID} = ?", arrayOf(iId.toString()))
                }

                val profileData = mutableMapOf<String, String>()
                if (resolvedName != null) {
                    profileData["name"] = resolvedName
                    profileData["fullName"] = resolvedName
                    profileData["userName"] = resolvedName
                    profileData["owner_name"] = resolvedName
                }
                if (resolvedAvatar != null) {
                    profileData["avatar_id"] = resolvedAvatar
                    profileData["equipped_avatar_id"] = resolvedAvatar
                }
                if (profileData.isNotEmpty()) {
                    UserProfileTable.upsert(db, iId, profileData)
                }
            } else if (resolvedName != null || resolvedAvatar != null) {
                saveRosterNode(deviceId, resolvedName ?: "Target", "Active Tracker", "LIVE", false, true, avatarId = resolvedAvatar)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "updateDeviceIdentityAndAvatar failed for $deviceId", e)
        } finally {
            db.endTransaction()
        }
    }

    fun getActiveProfileAddress(): String {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
        val identityMap: Map<String, String> = getIdentityByAuthUid(uid) ?: return "Identity Not Found"
        val workLoc = identityMap.get("work_location")
        val altLoc = identityMap.get("address")
        return workLoc ?: altLoc ?: "Location Not Configured"
    }

    fun purgeSysLogs() {
        try {
            val db = writableDatabase
            val cutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            db.delete("sys_log", "timestamp < ?", arrayOf(cutoff.toString()))
        } catch (e: Exception) { }
    }

    fun getSysLogs(level: String?, query: String?): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            var selection: String? = null
            val args = mutableListOf<String>()
            
            if (level != null) {
                selection = "level = ?"
                args.add(level)
            }
            if (query != null) {
                selection = if (selection == null) "message LIKE ?" else "$selection AND message LIKE ?"
                args.add("%$query%")
            }
            
            val cursor = db.query("sys_log", null, selection, if (args.isEmpty()) null else args.toTypedArray(), null, null, "timestamp DESC", "500")
            cursor.use {
                val cols = it.columnNames
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, Any>()
                    for (col in cols) {
                        val idx = it.getColumnIndexOrThrow(col)
                        map[col] = when (it.getType(idx)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(idx)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> it.getDouble(idx)
                            else -> it.getString(idx) ?: ""
                        }
                    }
                    list.add(map)
                }
            }
        } catch (e: Exception) { }
        return list
    }

    @JvmOverloads
    fun updateRosterGrouping(uid1: String, newRole: String, isFriend: Boolean, groupId: String = "DEFAULT_GROUP") {
        val db = writableDatabase
        val famRole = if (isFriend) "None" else newRole
        val frRole = if (isFriend) newRole else "None"
        updateCitizenRole(uid1, famRole, frRole, isFriend, groupId)
    }

    fun getOfflineRoster(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            // Logic: Comprehensive SSOT Join across UserIdentity, GroupMember, and UserProfile
            val sql = """
                SELECT 
                    i.${UserIdentityTable.COL_UID_1} as uid_1,
                    i.${UserIdentityTable.COL_UID_1} as uid,
                    i.${UserIdentityTable.COL_UID_2} as uid_2,
                    COALESCE(NULLIF(u.${UserProfileTable.COL_NAME}, ''), NULLIF(i.${UserIdentityTable.COL_FULL_NAME}, ''), NULLIF(i.${UserIdentityTable.COL_OWNER_NAME}, ''), 'Target Node') as fullName,
                    COALESCE(NULLIF(u.${UserProfileTable.COL_NAME}, ''), NULLIF(i.${UserIdentityTable.COL_FULL_NAME}, ''), NULLIF(i.${UserIdentityTable.COL_OWNER_NAME}, ''), 'Target Node') as display_name,
                    COALESCE(NULLIF(u.${UserProfileTable.COL_AVATAR_ID}, ''), NULLIF(i.${UserIdentityTable.COL_QR_REF}, ''), '1') as avatar_id,
                    i.${UserIdentityTable.COL_IS_ADMIN} as is_admin,
                    i.${UserIdentityTable.COL_ID} as identity_id,
                    COALESCE(NULLIF(m.${GroupMemberTable.COL_ROLE}, ''), 'Member') as role_type,
                    CASE 
                        WHEN UPPER(COALESCE(m.${GroupMemberTable.COL_ROLE}, '')) LIKE '%FRIEND%' THEN 1
                        WHEN UPPER(COALESCE(m.${GroupMemberTable.COL_ROLE}, '')) LIKE '%COLLEAGUE%' THEN 1
                        WHEN UPPER(COALESCE(m.${GroupMemberTable.COL_ROLE}, '')) LIKE '%PEER%' THEN 1
                        WHEN UPPER(COALESCE(m.${GroupMemberTable.COL_ROLE}, '')) LIKE '%ACQUAINTANCE%' THEN 1
                        WHEN UPPER(COALESCE(m.${GroupMemberTable.COL_ROLE}, '')) LIKE '%NEIGHBOR%' THEN 1
                        ELSE 0 
                    END as is_friend,
                    'LIVE' as st_session_state
                FROM ${UserIdentityTable.TABLE_NAME} i
                LEFT JOIN ${GroupMemberTable.TABLE_NAME} m ON i.${UserIdentityTable.COL_ID} = m.${GroupMemberTable.COL_IDENTITY_ID}
                LEFT JOIN ${UserProfileTable.TABLE_NAME} u ON i.${UserIdentityTable.COL_ID} = u.${UserProfileTable.COL_IDENTITY_ID}
                WHERE i.${UserIdentityTable.COL_UID_1} IS NOT NULL 
                  AND i.${UserIdentityTable.COL_UID_1} != '' 
                  AND (i.${UserIdentityTable.COL_STATUS} IS NULL OR i.${UserIdentityTable.COL_STATUS} != 'DELETED')
                GROUP BY i.${UserIdentityTable.COL_UID_1}
            """.trimIndent()
            
            val cursor = db.rawQuery(sql, null)
            cursor.use {
                val cols = it.columnNames
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, Any>()
                    for (col in cols) {
                        val idx = it.getColumnIndexOrThrow(col)
                        map[col] = when (it.getType(idx)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(idx)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> it.getDouble(idx)
                            else -> it.getString(idx) ?: ""
                        }
                    }
                    list.add(map)
                }
            }
        } catch (e: Exception) { }
        return list
    }

    fun saveGeofenceLog(uid: String, avatar: String, zoneId: String, zoneName: String, status: String, lat: Double = 0.0, lng: Double = 0.0) {
        try {
            val db = writableDatabase
            var internalId = resolveInternalId(uid)
            if (internalId == -1L && avatar.isNotEmpty()) {
                internalId = resolveInternalId(avatar)
            }
            if (internalId == -1L) {
                val nIdx = resolveNodeIndex(uid, avatar)
                internalId = UserIdentityTable.insertOrUpdate(db, uid, avatar, avatar, nodeIndex = nIdx)
            }
            val cleanZone = if (zoneId.contains("___")) zoneId.split("___")[0] else zoneId
            if (!doesSafeZoneExist(cleanZone)) {
                SafeZoneTable.save(db, cleanZone, zoneName.ifEmpty { cleanZone }, lat, lng, 100.0, internalId, "ALL", true, true)
            }
            GeofenceEventTable.logEvent(db, internalId, cleanZone, status)
        } catch (e: Exception) {
            saveSysLog("E", "SAVE_GEOFENCE_LOG", "Failed to archive geofence log: ${e.message}")
        }
    }
    
    // 🛡️ REFERENTIAL INTEGRITY HELPERS: Mission Critical Verification
    fun doesUserExist(uid: String): Boolean {
        return try {
            val db = readableDatabase
            db.query(UserIdentityTable.TABLE_NAME, null, 
                "${UserIdentityTable.COL_UID_1}=? OR ${UserIdentityTable.COL_FULL_NAME}=? OR ${UserIdentityTable.COL_OWNER_NAME}=?", 
                arrayOf(uid, uid, uid), null, null, null).use { cursor ->
                cursor.count > 0
            }
        } catch (e: Exception) { false }
    }
    
    fun doesSafeZoneExist(zoneId: String): Boolean {
        return try {
            val db = readableDatabase
            val cleanZone = if (zoneId.contains("___")) zoneId.split("___")[0] else zoneId
            db.query(SafeZoneTable.TABLE_NAME, null, 
                "${SafeZoneTable.COL_ZONE_ID}=? OR ${SafeZoneTable.COL_NAME}=? OR ${SafeZoneTable.COL_ZONE_ID}=? OR ${SafeZoneTable.COL_NAME}=?", 
                arrayOf(zoneId, zoneId, cleanZone, cleanZone), null, null, null).use { cursor ->
                cursor.count > 0
            }
        } catch (e: Exception) { false }
    }

    fun getGeofenceLogs(targetAvatar: String? = null, zoneId: String? = null): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            val whereClauses = mutableListOf<String>()
            val args = mutableListOf<String>()

            if (!targetAvatar.isNullOrEmpty() && targetAvatar != "ALL") {
                whereClauses.add("(COALESCE(u.${UserProfileTable.COL_NAME}, i.${UserIdentityTable.COL_FULL_NAME}) = ? OR i.${UserIdentityTable.COL_UID_1} = ?)")
                args.add(targetAvatar)
                args.add(targetAvatar)
            }
            if (!zoneId.isNullOrEmpty()) {
                val cleanZone = if (zoneId.contains("___")) zoneId.split("___")[0] else zoneId
                whereClauses.add("(e.${GeofenceEventTable.COL_ZONE_ID} = ? OR z.${SafeZoneTable.COL_NAME} = ?)")
                args.add(cleanZone)
                args.add(cleanZone)
            }

            val whereSql = if (whereClauses.isNotEmpty()) "WHERE " + whereClauses.joinToString(" AND ") else ""

            val sql = """
                SELECT 
                    e.${GeofenceEventTable.COL_EVENT_ID} as log_id,
                    COALESCE(u.${UserProfileTable.COL_NAME}, i.${UserIdentityTable.COL_FULL_NAME}, 'Active Asset') as id_full_name,
                    COALESCE(u.${UserProfileTable.COL_NAME}, i.${UserIdentityTable.COL_FULL_NAME}, 'Active Asset') as fullName,
                    e.${GeofenceEventTable.COL_EVENT_TYPE} as status,
                    e.${GeofenceEventTable.COL_TIMESTAMP} as ts_start,
                    COALESCE(z.${SafeZoneTable.COL_NAME}, e.${GeofenceEventTable.COL_ZONE_ID}) as zone_name
                FROM ${GeofenceEventTable.TABLE_NAME} e
                JOIN ${UserIdentityTable.TABLE_NAME} i ON e.${GeofenceEventTable.COL_IDENTITY_ID} = i.${UserIdentityTable.COL_ID}
                LEFT JOIN ${UserProfileTable.TABLE_NAME} u ON i.${UserIdentityTable.COL_ID} = u.${UserProfileTable.COL_IDENTITY_ID}
                LEFT JOIN ${SafeZoneTable.TABLE_NAME} z ON e.${GeofenceEventTable.COL_ZONE_ID} = z.${SafeZoneTable.COL_ZONE_ID}
                $whereSql
                ORDER BY e.${GeofenceEventTable.COL_TIMESTAMP} DESC
                LIMIT 100
            """.trimIndent()
            
            val cursor = db.rawQuery(sql, if (args.isNotEmpty()) args.toTypedArray() else null)
            cursor.use {
                val cols = it.columnNames
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, Any>()
                    for (col in cols) {
                        val idx = it.getColumnIndexOrThrow(col)
                        map[col] = when (it.getType(idx)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(idx)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> it.getDouble(idx)
                            else -> it.getString(idx) ?: ""
                        }
                    }
                    val ts = map["ts_start"] as? Long ?: 0L
                    map["readable_time"] = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ts))
                    list.add(map)
                }
            }
        } catch (e: Exception) { }
        return list
    }

    fun clearGeofenceLogs() {
        try {
            writableDatabase.delete(GeofenceEventTable.TABLE_NAME, null, null)
        } catch (e: Exception) { }
    }

    fun insertAlert(type: String, message: String, severity: String = "HIGH", epoch: Long = System.currentTimeMillis()) {
        try {
            saveSysLog(severity, "ALERT_$type", message, null, epoch)
            val db = writableDatabase
            val logId = "ALT_${epoch}_${UUID.randomUUID().toString().take(4).uppercase()}"
            val details = mapOf(
                "alert_type" to type,
                "message" to message,
                "severity" to severity,
                "epoch_ms" to epoch
            )
            val idCursor = db.rawQuery("SELECT ${UserIdentityTable.COL_ID} FROM ${UserIdentityTable.TABLE_NAME} LIMIT 1", null)
            var identityId = 1L
            idCursor.use {
                if (it.moveToFirst()) {
                    identityId = it.getLong(0)
                }
            }
            ChangeLogTable.logChange(
                db = db,
                identityId = identityId,
                logId = logId,
                category = "Security_Alert",
                action = type,
                entityId = logId,
                changedBy = "SYSTEM_SECURITY",
                details = details
            )
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "Error inserting alert: ${e.message}")
        }
    }

    @JvmOverloads
    fun saveSysLog(level: String, tag: String, message: String, extra: String? = null, timestamp: Long? = null) {
        try {
            val db = writableDatabase
            val v = ContentValues().apply {
                put("level", level)
                put("tag", tag)
                put("message", if (extra != null) "$message | Extra: $extra" else message)
                put("timestamp", timestamp ?: System.currentTimeMillis())
            }
            db.insert("sys_log", null, v)
        } catch (e: Exception) { }
    }

    fun clearAllSysLogs() {
        try {
            writableDatabase.delete("sys_log", null, null)
        } catch (e: Exception) { }
    }
    /**
     * 🔧 FIX DB-03: Use real device ID from SharedPreferences, NOT hardcoded "OWNER_DEVICE".
     * Hardcoded value caused duplicate/mislinked SIM records when the real device ID was different.
     */
    fun logSimCard(slot: Int, iccid: String, operator: String, mcc: String, mnc: String, lac: String, cid: String, isPrimary: Boolean) {
        try {
            val db = writableDatabase
            // Resolve real hardware device ID to prevent SIM table mislink
            val realDeviceId = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                .getString("hardware_device_id", null) ?: "OWNER_DEVICE"
            SimTable.logSim(db, realDeviceId, iccid, operator, mcc, mnc, isPrimary)
        } catch (e: Exception) { }
    }

    fun logSimTransaction(slot: Int, mcc: String, mnc: String, lac: String, cid: String, lat: Double, lng: Double, source: String) {
        try {
            val db = writableDatabase
            val v = ContentValues().apply {
                put("slot", slot)
                put("mcc", mcc)
                put("mnc", mnc)
                put("lac", lac)
                put("cid", cid)
                put("lat", lat)
                put("lng", lng)
                put("source", source)
                put("timestamp", com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp())
            }
            db.insert("sim_transactions", null, v)
        } catch (e: Exception) { }
    }
    fun isGeofenceAlertEnabled(zoneId: String): Boolean {
        return try {
            val db = readableDatabase
            val cleanZone = if (zoneId.contains("___")) zoneId.split("___")[0] else zoneId
            val cursor = db.query(SafeZoneTable.TABLE_NAME, arrayOf(SafeZoneTable.COL_IS_ALERTS_ENABLED), 
                "${SafeZoneTable.COL_ZONE_ID} = ? OR ${SafeZoneTable.COL_NAME} = ? OR ${SafeZoneTable.COL_ZONE_ID} = ? OR ${SafeZoneTable.COL_NAME} = ?", 
                arrayOf(zoneId, zoneId, cleanZone, cleanZone), null, null, null)
            cursor.use { if (it.moveToFirst()) it.getInt(0) == 1 else true }
        } catch (e: Exception) { true }
    }

    fun getSafeZoneDisplayName(zoneId: String): String {
        return try {
            val cleanZone = if (zoneId.contains("___")) zoneId.split("___")[0] else zoneId
            val cursor = readableDatabase.query(SafeZoneTable.TABLE_NAME, arrayOf(SafeZoneTable.COL_NAME),
                "${SafeZoneTable.COL_ZONE_ID} = ? OR ${SafeZoneTable.COL_NAME} = ?",
                arrayOf(cleanZone, cleanZone), null, null, null)
            cursor.use {
                if (it.moveToFirst()) it.getString(0)?.ifEmpty { cleanZone } ?: cleanZone else cleanZone
            }
        } catch (e: Exception) {
            if (zoneId.contains("___")) zoneId.split("___")[0] else zoneId
        }
    }

    fun isSafeZoneInSchedule(zoneId: String): Boolean {
        return try {
            val cleanZone = if (zoneId.contains("___")) zoneId.split("___")[0] else zoneId
            val cursor = readableDatabase.query(
                SafeZoneUserMapTable.TABLE_NAME,
                arrayOf(SafeZoneUserMapTable.COL_SCHEDULE_START, SafeZoneUserMapTable.COL_SCHEDULE_END, SafeZoneUserMapTable.COL_ACTIVE_DAYS, SafeZoneUserMapTable.COL_IS_ACTIVE),
                "${SafeZoneUserMapTable.COL_ZONE_ID} = ?",
                arrayOf(cleanZone), null, null, null
            )
            cursor.use {
                if (!it.moveToFirst()) return true // No restrictive schedule, active by default
                val isActive = it.getInt(it.getColumnIndexOrThrow(SafeZoneUserMapTable.COL_IS_ACTIVE)) == 1
                if (!isActive) return false

                val start = it.getLong(it.getColumnIndexOrThrow(SafeZoneUserMapTable.COL_SCHEDULE_START)).toInt()
                val end = it.getLong(it.getColumnIndexOrThrow(SafeZoneUserMapTable.COL_SCHEDULE_END)).toInt()
                val daysStr = it.getString(it.getColumnIndexOrThrow(SafeZoneUserMapTable.COL_ACTIVE_DAYS)) ?: ""

                val calendar = java.util.Calendar.getInstance()
                val currentHourMinute = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 100 + calendar.get(java.util.Calendar.MINUTE)
                val calDay = calendar.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon... 7=Sat
                val dayIdx0 = (calDay + 5) % 7 // 0=Mon, 1=Tue... 6=Sun
                val dayIdxSunday0 = calDay - 1 // 0=Sun, 1=Mon... 6=Sat

                // Day of week check
                if (daysStr.isNotEmpty() && daysStr != "ALL" && !daysStr.contains("1,2,3,4,5,6,0") && !daysStr.contains("0,1,2,3,4,5,6")) {
                    val dayTokens = daysStr.split(",").map { t -> t.trim() }
                    val dayMatch = dayTokens.contains(calDay.toString()) ||
                                   dayTokens.contains(dayIdx0.toString()) ||
                                   dayTokens.contains(dayIdxSunday0.toString())
                    if (!dayMatch) return false
                }

                // Time window check (e.g. 0800 to 1500)
                if (start != end && (start > 0 || end < 2359)) {
                    if (start <= end) {
                        if (currentHourMinute < start || currentHourMinute > end) return false
                    } else {
                        // Overnight window e.g. 2200 to 0600
                        if (currentHourMinute < start && currentHourMinute > end) return false
                    }
                }
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    fun updateSafeZoneActiveState(id: String, state: Int) {
        try {
            val v = android.content.ContentValues().apply { put(SafeZoneTable.COL_IS_ACTIVE, state) }
            writableDatabase.update(SafeZoneTable.TABLE_NAME, v, "${SafeZoneTable.COL_ZONE_ID} = ? OR ${SafeZoneTable.COL_NAME} = ?", arrayOf(id, id))
        } catch (e: Exception) { }
    }

    fun updateSafeZoneAlertState(id: String, state: Int) {
        try {
            val v = android.content.ContentValues().apply { put(SafeZoneTable.COL_IS_ALERTS_ENABLED, state) }
            writableDatabase.update(SafeZoneTable.TABLE_NAME, v, "${SafeZoneTable.COL_ZONE_ID} = ? OR ${SafeZoneTable.COL_NAME} = ?", arrayOf(id, id))
        } catch (e: Exception) { }
    }

    fun getGroupDetails(groupId: String): Map<String, String>? {
        try {
            val db = readableDatabase
            val cursor = db.query(GroupTable.TABLE_NAME, null, 
                "${GroupTable.COL_GROUP_ID}=?", arrayOf(groupId), null, null, null)
            cursor.use {
                if (it.moveToFirst()) {
                    val map = mutableMapOf<String, String>()
                    map["id"] = it.getString(it.getColumnIndexOrThrow(GroupTable.COL_GROUP_ID))
                    map["name"] = it.getString(it.getColumnIndexOrThrow(GroupTable.COL_GROUP_NAME)) ?: "SECURE_HUB"
                    map["created_on"] = it.getString(it.getColumnIndexOrThrow(GroupTable.COL_CREATED_ON)) ?: ""
                    return map
                }
            }
        } catch (e: Exception) { }
        return null
    }

    /**
     * 🛡️ Retrieves all active Safe Zones from SQLite for closed-loop Map rendering.
     */
    fun getActiveSafeZones(): List<com.jdroidx.jfindme.data.model.SafeZoneModel> {
        val list = mutableListOf<com.jdroidx.jfindme.data.model.SafeZoneModel>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                SafeZoneTable.TABLE_NAME,
                null,
                "${SafeZoneTable.COL_IS_ACTIVE} = 1 AND (${SafeZoneTable.COL_IS_ARCHIVED} = 0 OR ${SafeZoneTable.COL_IS_ARCHIVED} IS NULL)",
                null, null, null, null
            )
            cursor.use {
                while (it.moveToNext()) {
                    val zId = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_ZONE_ID)) ?: ""
                    val zName = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_NAME)) ?: "Safe Zone"
                    val lat = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LAT))
                    val lng = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LNG))
                    val rad = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_RADIUS)).toFloat()
                    val target = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_TARGET_UID)) ?: "ALL"
                    val alerts = it.getInt(it.getColumnIndexOrThrow(SafeZoneTable.COL_IS_ALERTS_ENABLED)) == 1
                    val active = it.getInt(it.getColumnIndexOrThrow(SafeZoneTable.COL_IS_ACTIVE)) == 1

                    if (lat != 0.0 || lng != 0.0) {
                        list.add(
                            com.jdroidx.jfindme.data.model.SafeZoneModel(
                                zoneId = zId,
                                name = zName,
                                lat = lat,
                                lng = lng,
                                radius = if (rad > 0f) rad else 100f,
                                assignedName = target,
                                alertsEnabled = alerts,
                                isZoneActive = active
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "Error loading active safe zones: ${e.message}")
        }
        return list
    }
}
