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
        const val DATABASE_VERSION = 70 // 🧬 V70: Global Forensic Creation Tracking (created_on)
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
     * 🧬 V63.8: Identity Bridge - Resolves the internal Long ID for a String UID1.
     */
    fun resolveInternalId(uid1: String): Long {
        try {
            val db = readableDatabase
            val cursor = db.query(UserIdentityTable.TABLE_NAME, arrayOf(UserIdentityTable.COL_ID),
                "${UserIdentityTable.COL_UID_1}=?", arrayOf(uid1), null, null, null)
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
        db.execSQL("PRAGMA foreign_keys = ON")
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

            // TIER 5: SYSTEM
            db.execSQL(SyncQueueTable.CREATE_TABLE)
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
        try {
            val db = readableDatabase
            val cursor = db.query(UserIdentityTable.TABLE_NAME, null, 
                "${UserIdentityTable.COL_AUTH_UID}=?", arrayOf(authUid), null, null, null)
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

    fun updateCitizenRole(uid1: String, famRole: String, frRole: String, isFriend: Boolean) {
        val db = writableDatabase
        val iId = resolveInternalId(uid1)
        if (iId != -1L) {
            GroupMemberTable.upsert(db, "DEFAULT_GROUP", iId, famRole.takeIf { it != "None" } ?: frRole, System.currentTimeMillis())
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
    }

    /**
     * 🧬 V69 Helper: Standardizes a table's timestamp columns by rebuilding it with TEXT types.
     */
    private fun standardizeTableTimestamp(db: SQLiteDatabase, tableName: String, createSql: String, timestampCols: List<String>) {
        val oldTable = "${tableName}_old_v69"
        db.execSQL("ALTER TABLE $tableName RENAME TO $oldTable")
        db.execSQL(createSql)
        
        // Build the dynamic INSERT SELECT statement
        val cursor = db.rawQuery("PRAGMA table_info($oldTable)", null)
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(1))
        }
        cursor.close()

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
        role: String = "ADMIN"
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // STEP 1: AUTHORIZE DATA (Tier 1 Identity + Tier 2 Device)
            val nIdx = resolveNodeIndex(uid1, uid2)
            val iId = UserIdentityTable.insertOrUpdate(db, uid1, uid2, authUid, 
                fullName = profileData["name"], 
                owner = profileData["owner_name"],
                connectedId = deviceId,
                qrRef = profileData["qr_code_ref"],
                nodeIndex = nIdx
            )
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
    fun saveRosterNode(uid1: String, name: String, role: String, status: String, isAdmin: Boolean, isFriend: Boolean, ownerName: String? = null, uid2: String? = null, connectedDeviceId: String? = null, qrCode: String? = null, avatarId: String? = null) {
        val db = writableDatabase
        db.beginTransaction()
        try {
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

            // Write to V57 Relationships (GroupMember)
            GroupMemberTable.addMember(db, "DEFAULT_GROUP", iId, role)
            
            // Write to Profile (Tier 1) for name/avatar parity (Dual-Write for V60 backwards compatibility)
            val rosterProfile = mapOf<String, String>(
                "name" to name, 
                "userName" to name, 
                "avatar_id" to (avatarId ?: "1")
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
        val name = v.getAsString("id_full_name") ?: "Target"
        val role = v.getAsString("role_type") ?: "Member"
        val isAdmin = v.getAsInteger("is_admin") == 1
        val isFriend = v.getAsInteger("is_friend") == 1
        val qr = v.getAsString("qr_code_ref")
        val avatar = v.getAsString("avatar_id")
        
        saveRosterNode(uid1, name, role, "Active", isAdmin, isFriend, avatarId = avatar, qrCode = qr)
    }

    fun getOfflineSafeZones(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            val cursor = db.query(SafeZoneTable.TABLE_NAME, null, null, null, null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, Any>()
                    map["zone_id"] = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_ZONE_ID)) ?: ""
                    map["name"] = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_NAME)) ?: ""
                    map["lat"] = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LAT))
                    map["lng"] = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LNG))
                    map["radius"] = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_RADIUS))
                    map["target"] = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_TARGET_UID)) ?: "ALL"
                    map["flag_a"] = it.getInt(it.getColumnIndexOrThrow(SafeZoneTable.COL_IS_ALERTS_ENABLED))
                    map["flag_b"] = it.getInt(it.getColumnIndexOrThrow(SafeZoneTable.COL_IS_ACTIVE))
                    list.add(map)
                }
            }
        } catch (e: Exception) { }
        return list
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

    fun getHistoryLogsFiltered(uid1: String, cutoff: Long): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            // V57: Hardened Day-wise grouping as requested. 
            // selects the peak/latest ping for each day as a session anchor.
            val sql = """
                SELECT e.*, COUNT(*) as ping_count, MAX(e.${LocationEventTable.COL_TIMESTAMP}) as ts_day_end
                FROM ${LocationEventTable.TABLE_NAME} e
                JOIN ${UserIdentityTable.TABLE_NAME} i ON e.${LocationEventTable.COL_IDENTITY_ID} = i.${UserIdentityTable.COL_ID}
                WHERE i.${UserIdentityTable.COL_UID_1} = ?
                AND e.${LocationEventTable.COL_TIMESTAMP} > ?
                GROUP BY e.${LocationEventTable.COL_DAY_KEY}
                ORDER BY e.${LocationEventTable.COL_DAY_KEY} DESC
            """.trimIndent()
            
            val cursor = db.rawQuery(sql, arrayOf(uid1, cutoff.toString()))
            cursor.use {
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, Any>()
                    map["day_key"] = it.getInt(it.getColumnIndexOrThrow(LocationEventTable.COL_DAY_KEY))
                    map["lat"] = it.getDouble(it.getColumnIndexOrThrow(LocationEventTable.COL_LAT))
                    map["lng"] = it.getDouble(it.getColumnIndexOrThrow(LocationEventTable.COL_LNG))
                    map["ts_start"] = it.getLong(it.getColumnIndexOrThrow(LocationEventTable.COL_TIMESTAMP))
                    map["ts_end"] = it.getLong(it.getColumnIndexOrThrow("ts_day_end"))
                    map["ping_count"] = it.getLong(it.getColumnIndexOrThrow("ping_count"))
                    map["geo_address"] = it.getString(it.getColumnIndexOrThrow(LocationEventTable.COL_ADDRESS)) ?: "Locating..."
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

    fun saveSafeZone(v: android.content.ContentValues) {
        val db = writableDatabase
        val uid = v.getAsString("target") ?: v.getAsString("created_by")
        val iId = if (uid != null) resolveInternalId(uid) else -1L
        SafeZoneTable.save(db, v.getAsString("zone_id"), v.getAsString("name"), v.getAsDouble("lat"), v.getAsDouble("lng"), v.getAsDouble("radius"), if(iId != -1L) iId else null, v.getAsString("target"), v.getAsInteger("flag_a") == 1, v.getAsInteger("flag_b") == 1)
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
            "geofence_events", "sync_queue"
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
        val cursor = db.query(UserIdentityTable.TABLE_NAME, null, "${UserIdentityTable.COL_UID_1} = ?", arrayOf(agentId), null, null, null)
        if (cursor.moveToFirst()) {
            res["userName"] = cursor.getString(cursor.getColumnIndexOrThrow(UserIdentityTable.COL_FULL_NAME)) ?: ""
            res["fullName"] = res["userName"] ?: ""
        }
        cursor.close()
        return res
    }

    fun getLocalDossier(uid: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val db = readableDatabase
            val sql = """
                SELECT 
                    u.${UserProfileTable.COL_NAME} as fullName,
                    u.${UserProfileTable.COL_EMAIL} as email,
                    u.${UserProfileTable.COL_PHONE} as phone,
                    u.${UserProfileTable.COL_DOB} as dob,
                    u.${UserProfileTable.COL_GENDER} as gender,
                    u.${UserProfileTable.COL_COUNTRY} as country,
                    u.${UserProfileTable.COL_STATE} as state,
                    u.${UserProfileTable.COL_CITY} as city,
                    u.${UserProfileTable.COL_PINCODE} as pincode,
                    u.${UserProfileTable.COL_ADDRESS} as address,
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
                    m.${UserOrgMapTable.COL_CLEARANCE} as access_level
                FROM ${UserProfileTable.TABLE_NAME} u
                LEFT JOIN ${UserOrgMapTable.TABLE_NAME} m ON u.${UserProfileTable.COL_IDENTITY_ID} = m.${UserOrgMapTable.COL_IDENTITY_ID}
                LEFT JOIN ${OrganizationTable.TABLE_NAME} o ON m.${UserOrgMapTable.COL_ORG_ID} = o.${OrganizationTable.COL_ORG_ID}
                JOIN ${UserIdentityTable.TABLE_NAME} i ON u.${UserProfileTable.COL_IDENTITY_ID} = i.${UserIdentityTable.COL_ID}
                WHERE i.${UserIdentityTable.COL_UID_1} = ?
            """.trimIndent()
            
            val cursor = db.rawQuery(sql, arrayOf(uid))
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
                    i.${UserIdentityTable.COL_OWNER_NAME} as owner_name,
                    i.${UserIdentityTable.COL_CONNECTED_ID} as connected_id,
                    i.${UserIdentityTable.COL_QR_REF} as qr_code_ref,
                    p.${UserProfileTable.COL_NAME} as name,
                    p.${UserProfileTable.COL_EMAIL} as email,
                    p.${UserProfileTable.COL_PHONE} as phone,
                    p.${UserProfileTable.COL_AVATAR_ID} as avatar_id,
                    d.${DeviceTable.COL_LATITUDE} as latitude,
                    d.${DeviceTable.COL_LONGITUDE} as longitude,
                    d.${DeviceTable.COL_ADDRESS} as Address,
                    d.${DeviceTable.COL_DEVICE_MODEL} as model,
                    d.${DeviceTable.COL_BUILD_FINGERPRINT} as fingerprint
                FROM ${UserIdentityTable.TABLE_NAME} i
                LEFT JOIN ${UserProfileTable.TABLE_NAME} p ON i.${UserIdentityTable.COL_ID} = p.${UserProfileTable.COL_IDENTITY_ID}
                LEFT JOIN ${DeviceTable.TABLE_NAME} d ON i.${UserIdentityTable.COL_ID} = d.${DeviceTable.COL_IDENTITY_ID}
                WHERE i.${UserIdentityTable.COL_UID_1} = ?
            """.trimIndent()
            
            val cursor = db.rawQuery(sql, arrayOf(agentId))
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

    fun getActiveProfileAddress(): String {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: ""
        val identityMap: Map<String, String> = getIdentityByAuthUid(uid) ?: return "Identity Not Found"
        val workLoc = identityMap.get("work_location") as? String
        val altLoc = identityMap.get("userAddress") as? String
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

    fun updateRosterGrouping(uid1: String, newRole: String, isFriend: Boolean) {
        val db = writableDatabase
        val famRole = if (isFriend) "None" else newRole
        val frRole = if (isFriend) newRole else "None"
        updateCitizenRole(uid1, famRole, frRole, isFriend)
    }

    fun getOfflineRoster(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            // Logic: JOIN Tier 3 Relationship Graph with Tier 1 User Profiles
            val sql = """
                SELECT 
                    p.${UserProfileTable.COL_NAME} as display_name,
                    u.${UserProfileTable.COL_NAME} as fullName,
                    u.${UserProfileTable.COL_AVATAR_ID} as avatar_id,
                    m.${GroupMemberTable.COL_IDENTITY_ID} as identity_id,
                    m.${GroupMemberTable.COL_ROLE} as role
                FROM ${GroupMemberTable.TABLE_NAME} m
                JOIN ${UserProfileTable.TABLE_NAME} u ON m.${GroupMemberTable.COL_IDENTITY_ID} = u.${UserProfileTable.COL_IDENTITY_ID}
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
            val internalId = resolveInternalId(uid)
            if (internalId != -1L) {
                GeofenceEventTable.logEvent(db, internalId, zoneId, status)
            }
        } catch (e: Exception) { }
    }
    
    // 🛡️ REFERENTIAL INTEGRITY HELPERS: Mission Critical Verification
    fun doesUserExist(uid: String): Boolean {
        return try {
            val db = readableDatabase
            val cursor = db.query(UserIdentityTable.TABLE_NAME, null, 
                "${UserIdentityTable.COL_UID_1}=?", arrayOf(uid), null, null, null)
            val exists = cursor.count > 0
            cursor.close()
            exists
        } catch (e: Exception) { false }
    }
    
    fun doesSafeZoneExist(zoneId: String): Boolean {
        return try {
            val db = readableDatabase
            val cursor = db.query(SafeZoneTable.TABLE_NAME, null, 
                "${SafeZoneTable.COL_ZONE_ID}=?", arrayOf(zoneId), null, null, null)
            val exists = cursor.count > 0
            cursor.close()
            exists
        } catch (e: Exception) { false }
    }

    fun getGeofenceLogs(targetAvatar: String? = null, zoneId: String? = null): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            val sql = """
                SELECT 
                    e.${GeofenceEventTable.COL_EVENT_ID} as log_id,
                    u.${UserProfileTable.COL_NAME} as id_full_name,
                    e.${GeofenceEventTable.COL_EVENT_TYPE} as status,
                    e.${GeofenceEventTable.COL_TIMESTAMP} as ts_start,
                    z.${SafeZoneTable.COL_NAME} as zone_name
                FROM ${GeofenceEventTable.TABLE_NAME} e
                JOIN ${UserIdentityTable.TABLE_NAME} i ON e.${GeofenceEventTable.COL_IDENTITY_ID} = i.${UserIdentityTable.COL_ID}
                JOIN ${UserProfileTable.TABLE_NAME} u ON i.${UserIdentityTable.COL_ID} = u.${UserProfileTable.COL_IDENTITY_ID}
                JOIN ${SafeZoneTable.TABLE_NAME} z ON e.${GeofenceEventTable.COL_ZONE_ID} = z.${SafeZoneTable.COL_ZONE_ID}
                ORDER BY e.${GeofenceEventTable.COL_TIMESTAMP} DESC
                LIMIT 100
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
                    // Add readable time
                    val ts = map["ts_start"] as? Long ?: 0L
                    map["readable_time"] = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ts))
                    list.add(map)
                }
            }
        } catch (e: Exception) { }
        return list
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
    fun logSimCard(slot: Int, iccid: String, operator: String, mcc: String, mnc: String, lac: String, cid: String, isPrimary: Boolean) {
        try {
            val db = writableDatabase
            // Logic: Delegate to Tier 2 SimTable wrapper
            SimTable.logSim(db, "OWNER_DEVICE", iccid, operator, mcc, mnc, isPrimary)
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
            val cursor = db.query("safe_zones", arrayOf("alerts_active"), "zone_id = ?", arrayOf(zoneId), null, null, null)
            cursor.use { if (it.moveToFirst()) it.getInt(0) == 1 else true }
        } catch (e: Exception) { true }
    }

    fun updateSafeZoneActiveState(id: String, state: Int) {
        try {
            val v = android.content.ContentValues().apply { put("is_active", state) }
            writableDatabase.update("safe_zone", v, "zone_id = ?", arrayOf(id))
        } catch (e: Exception) { }
    }

    fun updateSafeZoneAlertState(id: String, state: Int) {
        try {
            val v = android.content.ContentValues().apply { put("is_alerts_enabled", state) }
            writableDatabase.update("safe_zone", v, "zone_id = ?", arrayOf(id))
        } catch (e: Exception) { }
    }
}
