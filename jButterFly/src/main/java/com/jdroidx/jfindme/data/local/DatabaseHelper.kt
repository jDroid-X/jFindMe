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
        const val DATABASE_VERSION = 79 // 🛡️ V79: Master REA Schema Refabrication (Motion State, Parental Master PIN, Watchdog Ledger, Device Policy Sentinel & Dual-Module Parity) // 🛡️ V78: Granular Permission Mask & Zero-Regression Network Gateway Hardening // 🧬 V77: Stealth Schedule Config & Security Evasion Audit Log // 🧬 V76: Closed-Loop Telemetry Vitals & Failover Route Caching // 🧬 V75: Master Hardware Specifications & Dual-SIM Parity
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
        const val TABLE_STEALTH_SCHEDULE = "stealth_schedule_config"
        const val TABLE_SECURITY_AUDIT = "security_audit_log"  
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

        // --- 🗄️ REVERSE GEOCODE & ALERT HISTORY ENTITIES ---
        const val TABLE_REVERSE_GEOCODE_CACHE = "reverse_geocode_cache"
        const val TABLE_ALERT_HISTORY = "alert_history"
        const val TABLE_DEVICE_POLICY = "device_policy_state"
        const val TABLE_WATCHDOG_RESURRECTION = "watchdog_resurrection_log"
        const val COL_IS_READ = "is_read"
        const val COL_RESOLVED_AT = "resolved_at"
        const val ALERT_TYPE_SOS_PANIC = "SOS_PANIC"
        const val MASTER_ROSTER_TABLE = "master_roster"
        const val COL_PERMISSIONS_MASK = "permissions_mask"
        const val COL_PHONE_NUMBER = "phone_number"

        // ──────────────────────────────────────────────────────────────────
        // 🛠️ ENTERPRISE FORENSIC METRICS & MAINTENANCE (Screen 21, 24, 27)
        // ──────────────────────────────────────────────────────────────────

        @JvmStatic
        fun getDatabaseForensicMetrics(context: Context): Map<String, Any> {
            val appContext = context.applicationContext ?: context
            val dbFile = appContext.getDatabasePath(DATABASE_NAME)
            val fileSizeBytes = if (dbFile.exists()) dbFile.length() else 0L
            val fileSizeFormatted = String.format(java.util.Locale.US, "%.2f MB", fileSizeBytes / (1024.0 * 1024.0))

            var pageCount = 0L
            var pageSize = 4096L
            var journalMode = "WAL"
            var integrityStatus = "OK"

            try {
                val db = getInstance(appContext).readableDatabase
                db.rawQuery("PRAGMA page_count", null)?.use { cursor ->
                    if (cursor.moveToFirst()) pageCount = cursor.getLong(0)
                }
                db.rawQuery("PRAGMA page_size", null)?.use { cursor ->
                    if (cursor.moveToFirst()) pageSize = cursor.getLong(0)
                }
                db.rawQuery("PRAGMA journal_mode", null)?.use { cursor ->
                    if (cursor.moveToFirst()) journalMode = cursor.getString(0) ?: "WAL"
                }
                db.rawQuery("PRAGMA integrity_check", null)?.use { cursor ->
                    if (cursor.moveToFirst()) integrityStatus = cursor.getString(0) ?: "OK"
                }
            } catch (e: Exception) {
                com.jdroidx.jfindme.utils.SystemLogger.e("DatabaseHelper", "Error fetching forensic metrics: ${e.message}")
            }

            return mapOf(
                "file_size_bytes" to fileSizeBytes,
                "file_size_formatted" to fileSizeFormatted,
                "page_count" to pageCount,
                "page_size" to pageSize,
                "journal_mode" to journalMode,
                "integrity_status" to integrityStatus
            )
        }

        @JvmStatic
        fun getTableRecordCounts(context: Context): Map<String, Long> {
            val counts = mutableMapOf<String, Long>()
            val tables = listOf(
                "locations", "sim_transactions", "security_audit_log", "stealth_schedule_config",
                "device_vitals", "user_profile", "user_identity", "devices", "safe_zones",
                "reverse_geocode_cache", "alert_history", "device_policy_state", "watchdog_resurrection_log"
            )
            try {
                // [Variables] | [Constants] | [Dependencies] | [Integration Points]
                // Forensic Metric Aggregator: Query row counts with resource-safe Cursor auto-closure
                val db = getInstance(context).readableDatabase
                for (tbl in tables) {
                    try {
                        db.rawQuery("SELECT count(*) FROM $tbl", null)?.use { c ->
                            if (c.moveToFirst()) counts[tbl] = c.getLong(0)
                        }
                    } catch (_: Exception) {
                        counts[tbl] = 0L
                    }
                }
            } catch (e: Exception) {
                com.jdroidx.jfindme.utils.SystemLogger.e("DatabaseHelper", "Error querying table counts: ${e.message}")
            }
            return counts
        }

        @JvmStatic
        fun vacuumAndReindex(context: Context): Boolean {
            return try {
                // [Variables] | [Constants] | [Dependencies] | [Integration Points]
                // Database Maintenance: Vacuum and reindex SQLite to defragment B-trees and rebuild indexes
                val db = getInstance(context).writableDatabase
                db.execSQL("PRAGMA optimize")
                db.execSQL("VACUUM")
                db.execSQL("REINDEX")
                com.jdroidx.jfindme.utils.SystemLogger.i("DatabaseHelper", "Database vacuum & reindex successfully executed.")
                true
            } catch (e: Exception) {
                com.jdroidx.jfindme.utils.SystemLogger.e("DatabaseHelper", "Vacuum & reindex failed: ${e.message}")
                false
            }
        }

        @JvmStatic
        @JvmOverloads
        fun getAuditLogsCursor(context: Context, filterEventType: String? = null): android.database.Cursor? {
            val helper = getInstance(context) ?: return null
            val db = helper.readableDatabase
            return if (!filterEventType.isNullOrBlank() && filterEventType != "ALL") {
                db.rawQuery(
                    "SELECT * FROM security_audit_log WHERE event_type = ? ORDER BY timestamp DESC",
                    arrayOf(filterEventType)
                )
            } else {
                db.rawQuery(
                    "SELECT * FROM security_audit_log ORDER BY timestamp DESC",
                    null
                )
            }
        }

        @JvmStatic
        fun setAutoVacuumEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("pref_sqlite_auto_vacuum", enabled).apply()
            try {
                val db = getInstance(context).writableDatabase
                val mode = if (enabled) 1 else 0
                db.execSQL("PRAGMA auto_vacuum = $mode")
            } catch (e: Exception) {
                com.jdroidx.jfindme.utils.SystemLogger.e("DatabaseHelper", "Failed to toggle PRAGMA auto_vacuum: ${e.message}")
            }
        }

        @JvmStatic
        fun isAutoVacuumEnabled(context: Context): Boolean {
            return context.getSharedPreferences("system_registry", Context.MODE_PRIVATE)
                .getBoolean("pref_sqlite_auto_vacuum", true)
        }

        // --- 📋 SPECIALIZED FORENSIC AUDIT DISPATCHERS ---
        @JvmStatic
        fun logTransportHealthCheck(context: Context, details: String) {
            getInstance(context).logSecurityEvent("transport_health_check", details, "INFO")
        }

        @JvmStatic
        fun logWatchdogForceRestart(context: Context, details: String) {
            getInstance(context).logSecurityEvent("watchdog_force_restart", details, "WARN")
        }

        @JvmStatic
        fun logEmergencySirenDismissed(context: Context, details: String) {
            getInstance(context).logSecurityEvent("emergency_siren_dismissed", details, "WARN")
        }

        @JvmStatic
        fun logRosterReclamation(context: Context, details: String) {
            getInstance(context).logSecurityEvent("roster_reclamation", details, "INFO")
        }

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

    // 🛠️ Instance Delegation for Enterprise Forensic Operations
    fun getDatabaseForensicMetrics(): Map<String, Any> = Companion.getDatabaseForensicMetrics(this.context)
    fun getTableRecordCounts(): Map<String, Long> = Companion.getTableRecordCounts(this.context)
    fun vacuumAndReindex(): Boolean = Companion.vacuumAndReindex(this.context)
    fun getAuditLogsCursor(filterEventType: String? = null): android.database.Cursor? = Companion.getAuditLogsCursor(this.context, filterEventType)
    fun setAutoVacuumEnabled(enabled: Boolean) = Companion.setAutoVacuumEnabled(this.context, enabled)
    fun logTransportHealthCheck(details: String) = Companion.logTransportHealthCheck(this.context, details)
    fun logWatchdogForceRestart(details: String) = Companion.logWatchdogForceRestart(this.context, details)
    fun logEmergencySirenDismissed(details: String) = Companion.logEmergencySirenDismissed(this.context, details)
    fun logRosterReclamation(details: String) = Companion.logRosterReclamation(this.context, details)


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
            // Direct Primary Key fallback if uid1 is already a valid row ID
            val asLong = uid1.toLongOrNull()
            if (asLong != null && asLong > 0) {
                db.rawQuery("SELECT ${UserIdentityTable.COL_ID} FROM ${UserIdentityTable.TABLE_NAME} WHERE ${UserIdentityTable.COL_ID} = ? LIMIT 1", arrayOf(uid1)).use {
                    if (it.moveToFirst()) return it.getLong(0)
                }
            }
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
     * 📱 Device Analytical Sync: Hardware-locked registration & hardware telemetry.
     */
    @JvmOverloads
    fun syncDeviceAnalytical(
        uid1: String,
        deviceId: String,
        model: String?,
        imei: String?,
        rooted: Boolean,
        fingerprint: String?,
        lat: Double = 0.0,
        lng: Double = 0.0,
        addr: String? = null,
        brand: String? = null,
        board: String? = null,
        hardware: String? = null,
        deviceName: String? = null,
        imei1: String? = null,
        imei2: String? = null,
        isDualSim: Boolean? = null
    ) {
        val db = this.writableDatabase
        val iId = resolveInternalId(uid1)
        if (iId != -1L) DeviceTable.upsert(
            db = db,
            deviceId = deviceId,
            identityId = iId,
            model = model,
            imei = imei,
            rooted = rooted,
            fingerprint = fingerprint,
            lat = lat,
            lng = lng,
            addr = addr,
            brand = brand,
            board = board,
            hardware = hardware,
            deviceName = deviceName,
            imei1 = imei1,
            imei2 = imei2,
            isDualSim = isDualSim
        )
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

        // 🚀 Self-Healing: Verify PendingSyncQueueTable and DeviceVitalsTable presence
        try {
            db.execSQL(PendingSyncQueueTable.CREATE_TABLE)
            db.execSQL(DeviceVitalsTable.CREATE_TABLE)
        } catch (e: Exception) { }

        // 🚀 Self-Healing: Verify ChangeLogTable presence
        try {
            db.execSQL(ChangeLogTable.CREATE_TABLE)
            db.execSQL(DevicePolicyTable.CREATE_TABLE)
            db.execSQL(WatchdogResurrectionTable.CREATE_TABLE)
        } catch (e: Exception) { }

        // 🚀 Data Sanitization: Purge corrupted legacy logs saved as safe zones
        try {
            db.execSQL("DELETE FROM ${SafeZoneTable.TABLE_NAME} WHERE ${SafeZoneTable.COL_ZONE_ID} LIKE 'HANDSHAKE%' OR (${SafeZoneTable.COL_LAT} = 0 AND ${SafeZoneTable.COL_LNG} = 0)")
        } catch (e: Exception) { }

        // 🛡️ De-duplication Protocol: Eliminate duplicate rows from junction tables and enforce unique constraints
        try {
            db.execSQL("DELETE FROM ${GroupMemberTable.TABLE_NAME} WHERE rowid NOT IN (SELECT MIN(rowid) FROM ${GroupMemberTable.TABLE_NAME} GROUP BY ${GroupMemberTable.COL_GROUP_ID}, ${GroupMemberTable.COL_IDENTITY_ID})")
            db.execSQL("DELETE FROM ${DevicePairingTable.TABLE_NAME} WHERE rowid NOT IN (SELECT MIN(rowid) FROM ${DevicePairingTable.TABLE_NAME} GROUP BY ${DevicePairingTable.COL_DEVICE_A_ID}, ${DevicePairingTable.COL_DEVICE_B_ID})")
            db.execSQL("DELETE FROM ${SafeZoneUserMapTable.TABLE_NAME} WHERE rowid NOT IN (SELECT MIN(rowid) FROM ${SafeZoneUserMapTable.TABLE_NAME} GROUP BY ${SafeZoneUserMapTable.COL_ZONE_ID}, ${SafeZoneUserMapTable.COL_IDENTITY_ID})")
            db.execSQL("DELETE FROM ${UserOrgMapTable.TABLE_NAME} WHERE rowid NOT IN (SELECT MIN(rowid) FROM ${UserOrgMapTable.TABLE_NAME} GROUP BY ${UserOrgMapTable.COL_IDENTITY_ID}, ${UserOrgMapTable.COL_ORG_ID})")

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS uq_group_member ON ${GroupMemberTable.TABLE_NAME} (${GroupMemberTable.COL_GROUP_ID}, ${GroupMemberTable.COL_IDENTITY_ID})")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS uq_device_pairing ON ${DevicePairingTable.TABLE_NAME} (${DevicePairingTable.COL_DEVICE_A_ID}, ${DevicePairingTable.COL_DEVICE_B_ID})")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS uq_safe_zone_user ON ${SafeZoneUserMapTable.TABLE_NAME} (${SafeZoneUserMapTable.COL_ZONE_ID}, ${SafeZoneUserMapTable.COL_IDENTITY_ID})")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS uq_user_org ON ${UserOrgMapTable.TABLE_NAME} (${UserOrgMapTable.COL_IDENTITY_ID}, ${UserOrgMapTable.COL_ORG_ID})")

            // 🛡️ V77 Self-Healing: Stealth Schedule Config & Security Audit Log
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS stealth_schedule_config (
                    config_key TEXT PRIMARY KEY,
                    config_value TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())
            // 🛡️ REVERSE GEOCODE CACHE & ALERT HISTORY
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS reverse_geocode_cache (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    address TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS alert_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    agent_id TEXT,
                    alert_type TEXT DEFAULT 'SOS_PANIC',
                    timestamp INTEGER,
                    details TEXT
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS security_audit_log (
                    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_type TEXT NOT NULL,
                    event_details TEXT NOT NULL,
                    severity TEXT NOT NULL DEFAULT 'INFO',
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_security_audit_ts ON security_audit_log(timestamp DESC)")
            db.execSQL("INSERT OR IGNORE INTO stealth_schedule_config (config_key, config_value, updated_at) VALUES ('dormant_interval_mins', '60', 1792422553000)")
        } catch (_: Exception) {}

        // 🛡️ V74 Anti-GIGO Deduplication Indexes
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_location_event_dedupe ON ${LocationEventTable.TABLE_NAME} (${LocationEventTable.COL_IDENTITY_ID}, ${LocationEventTable.COL_TIMESTAMP})")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_safe_zones_dedupe ON ${SafeZoneTable.TABLE_NAME} (${SafeZoneTable.COL_ZONE_ID})")
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
            db.execSQL(DevicePolicyTable.CREATE_TABLE)

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
            db.execSQL(PendingSyncQueueTable.CREATE_TABLE)
            db.execSQL(DeviceVitalsTable.CREATE_TABLE)
            db.execSQL(ChangeLogTable.CREATE_TABLE)
            db.execSQL(UserProfileTable.CREATE_TABLE_SYS_LOG)
            db.execSQL(WatchdogResurrectionTable.CREATE_TABLE)

            // Performance Compound Indexes
            try {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_location_event_id_ts ON ${LocationEventTable.TABLE_NAME} (${LocationEventTable.COL_IDENTITY_ID}, ${LocationEventTable.COL_TIMESTAMP} DESC)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pending_sync_status ON ${PendingSyncQueueTable.TABLE_NAME} (${PendingSyncQueueTable.COL_STATUS}, ${PendingSyncQueueTable.COL_TIMESTAMP} ASC)")
            } catch (ex: Exception) { }

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

    /**
     * 🛡️ V78: Queries Granular ACL Permission Mask for connected node.
     * Default 0xFF (All permissions granted).
     */
    fun getMemberPermissionsMask(uid1: String): Int {
        return try {
            val db = readableDatabase
            val iId = resolveInternalId(uid1)
            if (iId != -1L) {
                db.rawQuery(
                    "SELECT ${GroupMemberTable.COL_PERMISSIONS_MASK} FROM ${GroupMemberTable.TABLE_NAME} WHERE ${GroupMemberTable.COL_IDENTITY_ID} = ? LIMIT 1",
                    arrayOf(iId.toString())
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val raw = cursor.getString(0) ?: "0xFF"
                        if (raw.startsWith("0x") || raw.startsWith("0X")) {
                            raw.substring(2).toIntOrNull(16) ?: 0xFF
                        } else {
                            raw.toIntOrNull() ?: 0xFF
                        }
                    } else 0xFF
                }
            } else 0xFF
        } catch (e: Exception) {
            0xFF
        }
    }

    /**
     * 🛡️ V78: Updates Granular ACL Permission Mask for connected node.
     */
    fun updateMemberPermissionsMask(uid1: String, mask: Int): Boolean {
        return try {
            val db = writableDatabase
            val iId = resolveInternalId(uid1)
            if (iId != -1L) {
                val hexStr = "0x" + Integer.toHexString(mask).uppercase(java.util.Locale.US)
                val cv = android.content.ContentValues().apply {
                    put(GroupMemberTable.COL_PERMISSIONS_MASK, hexStr)
                }
                db.update(GroupMemberTable.TABLE_NAME, cv, "${GroupMemberTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString())) > 0
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Group-Scoped Soft Deletion: Scopes unlinking to active group_members without poisoning physical user_identities
    @JvmOverloads fun markRosterNodeDeleted(uid1: String, groupId: String? = null): Boolean {
        return try {
            val db = writableDatabase
            val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
            val iId = resolveInternalId(uid1)
            if (iId != -1L) {
                if (!groupId.isNullOrBlank()) {
                    db.delete(GroupMemberTable.TABLE_NAME, "(${GroupMemberTable.COL_IDENTITY_ID} = ? OR ${GroupMemberTable.COL_AGENT_ID} = ?) AND ${GroupMemberTable.COL_GROUP_ID} = ?", arrayOf(iId.toString(), uid1, groupId))
                    db.delete(SafeZoneUserMapTable.TABLE_NAME, "${SafeZoneUserMapTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                } else {
                    val cv = ContentValues().apply {
                        put(UserIdentityTable.COL_STATUS, "DELETED")
                        put(UserIdentityTable.COL_UPDATED_AT, now)
                    }
                    db.update(UserIdentityTable.TABLE_NAME, cv, "${UserIdentityTable.COL_ID} = ?", arrayOf(iId.toString()))
                    db.delete(GroupMemberTable.TABLE_NAME, "${GroupMemberTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                }
            } else if (!groupId.isNullOrBlank()) {
                db.delete(GroupMemberTable.TABLE_NAME, "${GroupMemberTable.COL_AGENT_ID} = ? AND ${GroupMemberTable.COL_GROUP_ID} = ?", arrayOf(uid1, groupId))
            }
            if (groupId.isNullOrBlank()) {
                db.delete("device_pairings", "uid_2 = ? OR uid_1 = ?", arrayOf(uid1, uid1))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    @JvmOverloads fun isDeviceDeleted(uid1: String, groupId: String? = null): Boolean {
        return try {
            val db = readableDatabase
            val cursorGlobal = db.query(
                UserIdentityTable.TABLE_NAME,
                arrayOf(UserIdentityTable.COL_STATUS),
                "(${UserIdentityTable.COL_UID_1} = ? OR ${UserIdentityTable.COL_AUTH_UID} = ?) AND ${UserIdentityTable.COL_STATUS} = 'DELETED'",
                arrayOf(uid1, uid1), null, null, null
            )
            val isGloballyDeleted = cursorGlobal.use { it.moveToFirst() }
            if (isGloballyDeleted) return true

            if (!groupId.isNullOrBlank()) {
                val iId = resolveInternalId(uid1)
                val existsInGroup = if (iId != -1L) {
                    val curGrp = db.query(
                        GroupMemberTable.TABLE_NAME,
                        arrayOf(GroupMemberTable.COL_ROLE),
                        "(${GroupMemberTable.COL_IDENTITY_ID} = ? OR ${GroupMemberTable.COL_AGENT_ID} = ?) AND ${GroupMemberTable.COL_GROUP_ID} = ?",
                        arrayOf(iId.toString(), uid1, groupId), null, null, null
                    )
                    curGrp.use { it.moveToFirst() }
                } else {
                    val curGrp = db.query(
                        GroupMemberTable.TABLE_NAME,
                        arrayOf(GroupMemberTable.COL_ROLE),
                        "${GroupMemberTable.COL_AGENT_ID} = ? AND ${GroupMemberTable.COL_GROUP_ID} = ?",
                        arrayOf(uid1, groupId), null, null, null
                    )
                    curGrp.use { it.moveToFirst() }
                }
                if (!existsInGroup) return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun isDevicePendingUnlink(uid1: String): Boolean {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                UserIdentityTable.TABLE_NAME,
                arrayOf(UserIdentityTable.COL_STATUS),
                "(${UserIdentityTable.COL_UID_1} = ? OR ${UserIdentityTable.COL_AUTH_UID} = ?) AND ${UserIdentityTable.COL_STATUS} = 'PENDING_UNLINK'",
                arrayOf(uid1, uid1), null, null, null
            )
            cursor.use {
                it.moveToFirst()
            }
        } catch (e: Exception) {
            false
        }
    }

    fun parkRosterNode(uid1: String): Boolean {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put(UserIdentityTable.COL_STATUS, "PENDING_UNLINK")
                put(UserIdentityTable.COL_UPDATED_AT, com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(System.currentTimeMillis()))
            }
            val rows = db.update(
                UserIdentityTable.TABLE_NAME,
                cv,
                "${UserIdentityTable.COL_UID_1} = ? OR ${UserIdentityTable.COL_AUTH_UID} = ?",
                arrayOf(uid1, uid1)
            )
            rows > 0
        } catch (e: Exception) {
            false
        }
    }

    fun restoreRosterNode(uid1: String): Boolean {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put(UserIdentityTable.COL_STATUS, "Active")
                put(UserIdentityTable.COL_UPDATED_AT, com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(System.currentTimeMillis()))
            }
            val rows = db.update(
                UserIdentityTable.TABLE_NAME,
                cv,
                "${UserIdentityTable.COL_UID_1} = ? OR ${UserIdentityTable.COL_AUTH_UID} = ?",
                arrayOf(uid1, uid1)
            )
            rows > 0
        } catch (e: Exception) {
            false
        }
    }

    // [Variables] | [Constants] | [Dependencies] | [Integration Points]
    // Cascading Unlink Service: Group-scoped deletion prevents destroying identities participating in multiple circles
    @JvmOverloads fun deleteRosterNode(uid1: String, groupId: String? = null): Boolean {
        markRosterNodeDeleted(uid1, groupId)
        return try {
            val db = writableDatabase
            val iId = resolveInternalId(uid1)
            if (iId != -1L) {
                if (!groupId.isNullOrBlank()) {
                    db.delete(GroupMemberTable.TABLE_NAME, "(${GroupMemberTable.COL_IDENTITY_ID} = ? OR ${GroupMemberTable.COL_AGENT_ID} = ?) AND ${GroupMemberTable.COL_GROUP_ID} = ?", arrayOf(iId.toString(), uid1, groupId))
                    db.delete(SafeZoneUserMapTable.TABLE_NAME, "${SafeZoneUserMapTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                } else {
                    db.delete(GroupMemberTable.TABLE_NAME, "${GroupMemberTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                    db.delete(DeviceTable.TABLE_NAME, "${DeviceTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                    db.delete(SafeZoneUserMapTable.TABLE_NAME, "${SafeZoneUserMapTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                    db.delete(UserOrgMapTable.TABLE_NAME, "${UserOrgMapTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                    db.delete(UserProfileTable.TABLE_NAME, "${UserProfileTable.COL_IDENTITY_ID} = ?", arrayOf(iId.toString()))
                    db.delete(UserIdentityTable.TABLE_NAME, "${UserIdentityTable.COL_ID} = ?", arrayOf(iId.toString()))
                }
            } else if (!groupId.isNullOrBlank()) {
                db.delete(GroupMemberTable.TABLE_NAME, "${GroupMemberTable.COL_AGENT_ID} = ? AND ${GroupMemberTable.COL_GROUP_ID} = ?", arrayOf(uid1, groupId))
            }
            if (groupId.isNullOrBlank()) {
                db.delete("device_pairings", "uid_2 = ? OR uid_1 = ?", arrayOf(uid1, uid1))
            }
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

        if (oldVersion < 74) {
            try {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_location_event_dedupe ON ${LocationEventTable.TABLE_NAME} (${LocationEventTable.COL_IDENTITY_ID}, ${LocationEventTable.COL_TIMESTAMP})")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_safe_zones_dedupe ON ${SafeZoneTable.TABLE_NAME} (${SafeZoneTable.COL_ZONE_ID})")
                android.util.Log.i("DatabaseHelper", "V74 Schema Migration: Deduplication Indexes Deployed.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V74 Upgrade error: ${e.message}")
            }
        }

        if (oldVersion < 75) {
            db.beginTransaction()
            try {
                val newCols = mapOf(
                    DeviceTable.COL_BRAND to "TEXT",
                    DeviceTable.COL_BOARD to "TEXT",
                    DeviceTable.COL_HARDWARE to "TEXT",
                    DeviceTable.COL_DEVICE_NAME to "TEXT",
                    DeviceTable.COL_IMEI_1 to "TEXT",
                    DeviceTable.COL_IMEI_2 to "TEXT",
                    DeviceTable.COL_IS_DUAL_SIM to "INTEGER DEFAULT 0"
                )
                for ((col, def) in newCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${DeviceTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${DeviceTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }
                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V75 Schema Migration: Hardware specifications & Dual-SIM columns deployed.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V75 Migration Failure: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }

        if (oldVersion < 76) {
            db.beginTransaction()
            try {
                val newCols = mapOf(
                    DeviceVitalsTable.COL_ACTIVE_ROUTE to "TEXT DEFAULT 'N1_CELLULAR_FAST'",
                    DeviceVitalsTable.COL_ACK_TIMESTAMP to "INTEGER DEFAULT 0",
                    DeviceVitalsTable.COL_ACK_LATENCY_MS to "INTEGER DEFAULT 0"
                )
                for ((col, def) in newCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${DeviceVitalsTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${DeviceVitalsTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }
                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V76 Schema Migration: Closed-Loop Telemetry Vitals & Failover Route Caching deployed.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V76 Migration Failure: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }

        if (oldVersion < 78) {
            db.beginTransaction()
            try {
                try {
                    db.rawQuery("SELECT ${GroupMemberTable.COL_PERMISSIONS_MASK} FROM ${GroupMemberTable.TABLE_NAME} LIMIT 0", null).use { }
                } catch (e: Exception) {
                    try {
                        db.execSQL("ALTER TABLE ${GroupMemberTable.TABLE_NAME} ADD COLUMN ${GroupMemberTable.COL_PERMISSIONS_MASK} TEXT DEFAULT '0xFF'")
                    } catch (ex: Exception) { }
                }
                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V78 Schema Migration: Granular ACL permissions_mask verified.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V78 Migration Failure: ${e.message}")
            } finally {
                db.endTransaction()
            }
        }

        if (oldVersion < 79) {
            db.beginTransaction()
            try {
                // 1. user_profile additions: nickname, display_name, avatar_url, master_pin_hash, salt
                val profileCols = mapOf(
                    UserProfileTable.COL_NICKNAME to "TEXT",
                    UserProfileTable.COL_DISPLAY_NAME to "TEXT",
                    UserProfileTable.COL_AVATAR_URL to "TEXT",
                    UserProfileTable.COL_MASTER_PIN_HASH to "TEXT",
                    UserProfileTable.COL_SALT to "TEXT"
                )
                for ((col, def) in profileCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${UserProfileTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${UserProfileTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }

                // 2. user_identity additions: token_hash, auth_token, tracking_status
                val identityCols = mapOf(
                    UserIdentityTable.COL_TOKEN_HASH to "TEXT DEFAULT ''",
                    UserIdentityTable.COL_AUTH_TOKEN to "TEXT DEFAULT ''",
                    UserIdentityTable.COL_TRACKING_STATUS to "TEXT DEFAULT 'ACTIVE'"
                )
                for ((col, def) in identityCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${UserIdentityTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${UserIdentityTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }

                // 3. device additions: battery_optimization_exempt, os_version, app_version
                val deviceCols = mapOf(
                    DeviceTable.COL_BATTERY_OPTIMIZATION_EXEMPT to "INTEGER DEFAULT 0",
                    DeviceTable.COL_OS_VERSION to "TEXT",
                    DeviceTable.COL_APP_VERSION to "TEXT"
                )
                for ((col, def) in deviceCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${DeviceTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${DeviceTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }

                // 4. device_vitals additions: motion_state, storage_free_mb, ram_usage_mb
                val vitalsCols = mapOf(
                    DeviceVitalsTable.COL_MOTION_STATE to "TEXT DEFAULT 'STATIONARY'",
                    DeviceVitalsTable.COL_STORAGE_FREE_MB to "INTEGER DEFAULT 0",
                    DeviceVitalsTable.COL_RAM_USAGE_MB to "INTEGER DEFAULT 0"
                )
                for ((col, def) in vitalsCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${DeviceVitalsTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${DeviceVitalsTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }

                // 5. group_members additions: circle_type, is_muted
                val memberCols = mapOf(
                    GroupMemberTable.COL_CIRCLE_TYPE to "TEXT DEFAULT 'FAMILY'",
                    GroupMemberTable.COL_IS_MUTED to "INTEGER DEFAULT 0"
                )
                for ((col, def) in memberCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${GroupMemberTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${GroupMemberTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }

                // 6. location_event additions: accuracy, bearing, altitude, motion_state
                val locCols = mapOf(
                    LocationEventTable.COL_ACCURACY to "REAL DEFAULT 0.0",
                    LocationEventTable.COL_BEARING to "REAL DEFAULT 0.0",
                    LocationEventTable.COL_ALTITUDE to "REAL DEFAULT 0.0",
                    LocationEventTable.COL_MOTION_STATE to "TEXT DEFAULT 'UNKNOWN'"
                )
                for ((col, def) in locCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${LocationEventTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${LocationEventTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }

                // 7. sim additions: serving_cid, carrier_name
                val simCols = mapOf(
                    SimTable.COL_SERVING_CID to "TEXT DEFAULT ''",
                    SimTable.COL_CARRIER_NAME to "TEXT DEFAULT ''"
                )
                for ((col, def) in simCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${SimTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${SimTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }

                // 8. safe_zone additions: radius_meters, is_dwell_alert_enabled
                val safeCols = mapOf(
                    SafeZoneTable.COL_RADIUS_METERS to "REAL DEFAULT 0.0",
                    SafeZoneTable.COL_IS_DWELL_ALERT_ENABLED to "INTEGER DEFAULT 1"
                )
                for ((col, def) in safeCols) {
                    try {
                        db.rawQuery("SELECT $col FROM ${SafeZoneTable.TABLE_NAME} LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE ${SafeZoneTable.TABLE_NAME} ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }

                // 9. alert_history additions: is_read, resolved_at
                val alertCols = mapOf(
                    "is_read" to "INTEGER DEFAULT 0",
                    "resolved_at" to "INTEGER DEFAULT 0"
                )
                for ((col, def) in alertCols) {
                    try {
                        db.rawQuery("SELECT $col FROM alert_history LIMIT 0", null).use { }
                    } catch (e: Exception) {
                        try { db.execSQL("ALTER TABLE alert_history ADD COLUMN $col $def") } catch (ex: Exception) { }
                    }
                }

                // 10. Create new tables
                try { db.execSQL(DevicePolicyTable.CREATE_TABLE) } catch (e: Exception) { }
                try { db.execSQL(WatchdogResurrectionTable.CREATE_TABLE) } catch (e: Exception) { }

                // 11. Performance and compound indexes
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_vitals_motion ON ${DeviceVitalsTable.TABLE_NAME} (${DeviceVitalsTable.COL_HARDWARE_ID}, ${DeviceVitalsTable.COL_MOTION_STATE})")
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_location_motion ON ${LocationEventTable.TABLE_NAME} (${LocationEventTable.COL_IDENTITY_ID}, ${LocationEventTable.COL_MOTION_STATE})")
                } catch (e: Exception) { }

                db.setTransactionSuccessful()
                android.util.Log.i("DatabaseHelper", "V79 Schema Migration: Master REA Schema Refabrication Deployed.")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseHelper", "V79 Migration Failure: ${e.message}")
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
            val isDualSimVal = profileData["is_dual_sim"]?.equals("true", ignoreCase = true) == true
            DeviceTable.upsert(
                db = db,
                deviceId = deviceId,
                identityId = iId,
                model = profileData["model"],
                imei = profileData["imei"],
                rooted = false,
                fingerprint = profileData["fingerprint"],
                lat = lat,
                lng = lng,
                addr = addr,
                brand = profileData["brand"],
                board = profileData["board"],
                hardware = profileData["hardware"],
                deviceName = profileData["device_name"],
                imei1 = profileData["imei_1"] ?: profileData["imei"],
                imei2 = profileData["imei_2"],
                isDualSim = isDualSimVal
            )
            
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
        try {
            com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                context,
                com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.CATEGORY_ROSTER,
                com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPSERT,
                uid1,
                mapOf("uid" to uid1, "name" to name, "role" to role)
            )
        } catch (ignored: Exception) {}
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
                    val polyIdx = it.getColumnIndex(SafeZoneTable.COL_POLYGON_POINTS)
                    map["polygon_points"] = if (polyIdx != -1) (it.getString(polyIdx) ?: "") else ""
                    val dwellIdx = it.getColumnIndex(SafeZoneTable.COL_DWELL_MINUTES)
                    map["dwell_minutes"] = if (dwellIdx != -1) it.getInt(dwellIdx) else 0
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
                    val polyIdx = it.getColumnIndex(SafeZoneTable.COL_POLYGON_POINTS)
                    map["polygon_points"] = if (polyIdx != -1) (it.getString(polyIdx) ?: "") else ""
                    val dwellIdx = it.getColumnIndex(SafeZoneTable.COL_DWELL_MINUTES)
                    map["dwell_minutes"] = if (dwellIdx != -1) it.getInt(dwellIdx) else 0
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
            val updated = db.update(SafeZoneTable.TABLE_NAME, v, "${SafeZoneTable.COL_ZONE_ID} = ?", arrayOf(zoneId)) > 0
            if (updated) {
                try {
                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                        context,
                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.CATEGORY_SAFE_ZONE,
                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPDATE,
                        zoneId,
                        mapOf("zone_id" to zoneId, "is_active" to isActive)
                    )
                } catch (ignored: Exception) {}
            }
            updated
        } catch (e: Exception) { false }
    }

    fun updateSafeZoneAlerts(zoneId: String, alertsEnabled: Boolean): Boolean {
        return try {
            val db = writableDatabase
            val v = android.content.ContentValues().apply {
                put("flag_a", if (alertsEnabled) 1 else 0)
            }
            val updated = db.update(SafeZoneTable.TABLE_NAME, v, "${SafeZoneTable.COL_ZONE_ID} = ?", arrayOf(zoneId)) > 0
            if (updated) {
                try {
                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                        context,
                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.CATEGORY_SAFE_ZONE,
                        com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPDATE,
                        zoneId,
                        mapOf("zone_id" to zoneId, "alerts_enabled" to alertsEnabled)
                    )
                } catch (ignored: Exception) {}
            }
            updated
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
        // 🛡️ Anti-GIGO Coordinate Guard: Reject null island (0,0) and invalid coordinates
        if (lat == 0.0 && lng == 0.0) return
        if (lat.isNaN() || lng.isNaN() || lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return

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
                    MAX(COALESCE(NULLIF(e.${LocationEventTable.COL_ADDRESS}, ''), 'Recorded Location Fix')) as address,
                    AVG(e.${LocationEventTable.COL_SPEED}) as speed,
                    MAX(e.${LocationEventTable.COL_BATTERY}) as battery,
                    MAX(e.${LocationEventTable.COL_EVENT_ID}) as log_id
                FROM ${LocationEventTable.TABLE_NAME} e
                JOIN ${UserIdentityTable.TABLE_NAME} i ON e.${LocationEventTable.COL_IDENTITY_ID} = i.${UserIdentityTable.COL_ID}
                WHERE (i.${UserIdentityTable.COL_UID_1} = ? OR i.${UserIdentityTable.COL_AUTH_UID} = ? OR i.${UserIdentityTable.COL_UID_2} = ? OR i.${UserIdentityTable.COL_CONNECTED_ID} = ?)
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
                    
                    var tsStart = parseForensicStringToMillis(tsStartStr)
                    var tsEnd = parseForensicStringToMillis(tsEndStr)
                    if (tsEnd < tsStart) tsEnd = tsStart
                    
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
        } catch (e: Exception) { 
            com.jdroidx.jfindme.utils.SystemLogger.e("DatabaseHelper", "getHistoryLogsFiltered failed for $uid1", e)
        }
        return list
    }

    /**
     * [Variables] | [Constants] | [Dependencies] | [Integration Points]
     * Variables: currentCanonicalId, phoneOrModel, pruned, orphanUids, orphanUid
     * Constants: UserIdentityTable.TABLE_NAME, UserProfileTable.TABLE_NAME, DeviceTable.TABLE_NAME, UserProfileTable.COL_PHONE, DeviceTable.COL_DEVICE_MODEL, UserProfileTable.COL_NAME, UserProfileTable.COL_NICKNAME, UserProfileTable.COL_DISPLAY_NAME
     * Dependencies: android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.utils.SystemLogger
     * Integration Points: NetworkGatewayActivity.java, FirebaseComm.kt, DevicePairingEngine.kt
     * 🛡️ Multi-Device Zombie Node Pruner: De-duplicates stale instances of the same phone/model
     * on fresh app reinstalls (Solves Redmi Reinstall Zombie Leak #12).
     */
    fun pruneZombieNodes(currentCanonicalId: String, phoneOrModel: String?): Int {
        if (currentCanonicalId.isBlank() || phoneOrModel.isNullOrBlank()) return 0
        var pruned = 0
        try {
            val db = writableDatabase
            val cursor = db.rawQuery(
                """
                SELECT i.id, i.uid_1 
                FROM ${UserIdentityTable.TABLE_NAME} i
                LEFT JOIN ${UserProfileTable.TABLE_NAME} p ON i.id = p.identity_id
                LEFT JOIN ${DeviceTable.TABLE_NAME} d ON i.id = d.identity_id
                WHERE (p.${UserProfileTable.COL_PHONE} = ? OR d.${DeviceTable.COL_DEVICE_MODEL} = ? OR p.${UserProfileTable.COL_NAME} = ? OR p.${UserProfileTable.COL_NICKNAME} = ? OR p.${UserProfileTable.COL_DISPLAY_NAME} = ?) 
                  AND i.uid_1 != ? 
                  AND i.status != 'DELETED'
                """.trimIndent(),
                arrayOf(phoneOrModel, phoneOrModel, phoneOrModel, phoneOrModel, phoneOrModel, currentCanonicalId)
            )
            val orphanUids = mutableListOf<String>()
            cursor.use {
                while (it.moveToNext()) {
                    val orphanUid = it.getString(1)
                    if (!orphanUid.isNullOrBlank()) {
                        orphanUids.add(orphanUid)
                    }
                }
            }
            for (orphanUid in orphanUids) {
                markRosterNodeDeleted(orphanUid)
                pruned++
            }
        } catch (e: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.e("DatabaseHelper", "pruneZombieNodes error", e)
        }
        return pruned
    }

    /**
     * 🛡️ Inactivity TTL Decay: Automatically transitions devices with zero heartbeat
     * for > 7 days into ARCHIVED_GHOST state so active family rosters stay clean (Amber #10).
     */
    fun applyTtlDecay(maxInactiveDays: Int = 7): Int {
        var decayed = 0
        try {
            val db = writableDatabase
            val cutoffMillis = System.currentTimeMillis() - (maxInactiveDays * 86400000L)
            val cutoffStr = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(cutoffMillis)
            
            val cv = ContentValues().apply {
                put(UserIdentityTable.COL_STATUS, "ARCHIVED_GHOST")
            }
            decayed = db.update(
                UserIdentityTable.TABLE_NAME,
                cv,
                "${UserIdentityTable.COL_UPDATED_AT} < ? AND ${UserIdentityTable.COL_STATUS} = 'LIVE'",
                arrayOf(cutoffStr)
            )
        } catch (e: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.e("DatabaseHelper", "applyTtlDecay error", e)
        }
        return decayed
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
            val cursor = db.rawQuery(sql, arrayOf(uid1, uid1, uid1, uid1))
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

    /**
     * 🛰️ Offline-First Raw Location History Extractor:
     * Pulls chronological GPS fixes from local SQLite within the specified time window.
     */
    fun getRawHistoryPoints(uid1: String, startTs: Long, endTs: Long): List<com.jdroidx.jfindme.data.model.UserLocation> {
        val list = mutableListOf<com.jdroidx.jfindme.data.model.UserLocation>()
        try {
            val db = readableDatabase
            val startTsStr = if (startTs > 0) com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(startTs) else "0"
            val endTsStr = if (endTs > 0) com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp(endTs) else "99999999999999"
            val sql = """
                SELECT e.*, i.${UserIdentityTable.COL_UID_1} as uid_1, i.${UserIdentityTable.COL_FULL_NAME} as fullName, i.${UserIdentityTable.COL_CONNECTED_ID} as connectedId
                FROM ${LocationEventTable.TABLE_NAME} e
                JOIN ${UserIdentityTable.TABLE_NAME} i ON e.${LocationEventTable.COL_IDENTITY_ID} = i.${UserIdentityTable.COL_ID}
                WHERE (i.${UserIdentityTable.COL_UID_1} = ? OR i.${UserIdentityTable.COL_AUTH_UID} = ? OR i.${UserIdentityTable.COL_CONNECTED_ID} = ?)
                  AND e.${LocationEventTable.COL_TIMESTAMP} >= ?
                  AND e.${LocationEventTable.COL_TIMESTAMP} <= ?
                  AND (e.${LocationEventTable.COL_LAT} != 0.0 OR e.${LocationEventTable.COL_LNG} != 0.0)
                ORDER BY e.${LocationEventTable.COL_TIMESTAMP} ASC
            """.trimIndent()
            val cursor = db.rawQuery(sql, arrayOf(uid1, uid1, uid1, startTsStr, endTsStr))
            cursor.use {
                while (it.moveToNext()) {
                    val lat = it.getDouble(it.getColumnIndexOrThrow(LocationEventTable.COL_LAT))
                    val lng = it.getDouble(it.getColumnIndexOrThrow(LocationEventTable.COL_LNG))
                    val tsStr = it.getString(it.getColumnIndexOrThrow(LocationEventTable.COL_TIMESTAMP))
                    val speed = if (!it.isNull(it.getColumnIndexOrThrow(LocationEventTable.COL_SPEED))) it.getFloat(it.getColumnIndexOrThrow(LocationEventTable.COL_SPEED)) else 0f
                    val battery = if (!it.isNull(it.getColumnIndexOrThrow(LocationEventTable.COL_BATTERY))) it.getInt(it.getColumnIndexOrThrow(LocationEventTable.COL_BATTERY)) else -1
                    val sos = if (!it.isNull(it.getColumnIndexOrThrow(LocationEventTable.COL_SOS_FLAG))) it.getInt(it.getColumnIndexOrThrow(LocationEventTable.COL_SOS_FLAG)) == 1 else false
                    val addr = it.getString(it.getColumnIndexOrThrow(LocationEventTable.COL_ADDRESS))
                    val name = it.getString(it.getColumnIndexOrThrow("fullName")) ?: ""
                    val devId = it.getString(it.getColumnIndexOrThrow("connectedId")) ?: uid1

                    val point = com.jdroidx.jfindme.data.model.UserLocation(
                        userId = uid1,
                        deviceId = devId,
                        latitude = lat,
                        longitude = lng,
                        timestamp = tsStr,
                        speed = speed,
                        batteryLevel = battery,
                        isSosActive = sos,
                        address = addr,
                        userName = name
                    )
                    list.add(point)
                }
            }
        } catch (e: Exception) {
            com.jdroidx.jfindme.utils.SystemLogger.e("DatabaseHelper", "getRawHistoryPoints failed for $uid1", e)
        }
        return list
    }



    fun logDevicePairing(uid1: String, nameA: String, devA: String?, uid2: String, name2: String, dev2: String?, role2: String, groupId: String? = null) {
        val db = writableDatabase
        val pairId = "PR_${uid1.hashCode()}_${uid2.hashCode()}"
        val resolvedDevA = if (!devA.isNullOrBlank() && devA != "HOST") devA else uid1
        val resolvedDevB = if (!dev2.isNullOrBlank() && dev2 != "TARGET") dev2 else uid2
        DevicePairingTable.logPairing(db, pairId, resolvedDevA, resolvedDevB, "PAIRED")
        
        // Tier 3: Map the relationship to the group if provided
        if (groupId != null) {
            var iId = resolveInternalId(uid2)
            if (iId == -1L && resolvedDevB != uid2) {
                iId = resolveInternalId(resolvedDevB)
            }
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
        val polyPoints = v.getAsString("polygon_points")
        val dwellMin = if (v.containsKey("dwell_minutes")) (v.getAsInteger("dwell_minutes") ?: 0) else 0
        SafeZoneTable.save(
            db = db,
            zoneId = v.getAsString("zone_id") ?: "",
            name = v.getAsString("name"),
            lat = v.getAsDouble("lat") ?: 0.0,
            lng = v.getAsDouble("lng") ?: 0.0,
            radius = v.getAsDouble("radius") ?: 100.0,
            createdBy = if(iId != -1L) iId else null,
            target = v.getAsString("target"),
            alerts = alerts,
            active = active,
            archived = false,
            polygonPoints = polyPoints,
            dwellMinutes = dwellMin
        )
        try {
            val zoneId = v.getAsString("zone_id") ?: ""
            val zoneMap = mutableMapOf<String, Any?>()
            for (key in v.keySet()) {
                zoneMap[key] = v.get(key)
            }
            com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                context,
                com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.CATEGORY_SAFE_ZONE,
                com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_UPSERT,
                zoneId,
                zoneMap
            )
        } catch (ignored: Exception) {}
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
        val deleted = db.delete(SafeZoneTable.TABLE_NAME, "${SafeZoneTable.COL_ZONE_ID} = ?", arrayOf(zoneId)) > 0
        if (deleted) {
            try {
                com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.notifyMasterDataChanged(
                    context,
                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.CATEGORY_SAFE_ZONE,
                    com.jdroidx.jfindme.engine.sync.MasterDataSyncSentinel.TYPE_DELETE,
                    zoneId,
                    mapOf("zone_id" to zoneId)
                )
            } catch (ignored: Exception) {}
        }
        return deleted
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
                LIMIT 1
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
                    d.${DeviceTable.COL_BRAND} as brand,
                    d.${DeviceTable.COL_BOARD} as board,
                    d.${DeviceTable.COL_HARDWARE} as hardware,
                    d.${DeviceTable.COL_DEVICE_NAME} as device_name,
                    d.${DeviceTable.COL_IMEI} as imei,
                    d.${DeviceTable.COL_IMEI_1} as imei_1,
                    d.${DeviceTable.COL_IMEI_2} as imei_2,
                    d.${DeviceTable.COL_IS_DUAL_SIM} as is_dual_sim,
                    d.${DeviceTable.COL_BUILD_FINGERPRINT} as fingerprint
                FROM ${UserIdentityTable.TABLE_NAME} i
                LEFT JOIN ${UserProfileTable.TABLE_NAME} p ON i.${UserIdentityTable.COL_ID} = p.${UserProfileTable.COL_IDENTITY_ID}
                LEFT JOIN ${DeviceTable.TABLE_NAME} d ON i.${UserIdentityTable.COL_ID} = d.${DeviceTable.COL_IDENTITY_ID}
                WHERE i.${UserIdentityTable.COL_UID_1} = ? 
                   OR i.${UserIdentityTable.COL_AUTH_UID} = ? 
                   OR i.${UserIdentityTable.COL_CONNECTED_ID} = ? 
                   OR i.${UserIdentityTable.COL_UID_2} = ?
                LIMIT 1
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
     * 🧬 V75: Self-Healing Hardware Specifications Backfill & Sync Engine
     * Updates old/existing installations by inspecting hardware, persisting to local SQLite,
     * and pushing atomic updates to Firebase Cloud.
     */
    fun backfillCurrentDeviceHardwareSpecs(context: Context) {
        try {
            val agentId = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getOrGenerateAgentId(context)
            val hardwareUid = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareUid(context)
            val specs = com.jdroidx.jfindme.utils.DeviceIdentityHelper.getHardwareSpecs(context)

            // 1. Update local SQLite DeviceTable
            syncDeviceAnalytical(
                uid1 = agentId,
                deviceId = hardwareUid,
                model = specs.model,
                imei = specs.imei1,
                rooted = false,
                fingerprint = android.os.Build.FINGERPRINT,
                brand = specs.brand,
                board = specs.board,
                hardware = specs.hardware,
                deviceName = specs.deviceName,
                imei1 = specs.imei1,
                imei2 = specs.imei2,
                isDualSim = specs.isDualSim
            )

            // 2. Immediate Cloud Matrix Update if authenticated
            val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
            if (!authUid.isNullOrBlank()) {
                val fullDossier = getForensicDossierComplete(agentId).toMutableMap()
                fullDossier["brand"] = specs.brand
                fullDossier["model"] = specs.model
                fullDossier["board"] = specs.board
                fullDossier["hardware"] = specs.hardware
                fullDossier["device_name"] = specs.deviceName
                fullDossier["imei"] = specs.imei1
                fullDossier["imei_1"] = specs.imei1
                fullDossier["imei_2"] = specs.imei2
                fullDossier["is_dual_sim"] = specs.isDualSim
                fullDossier["fingerprint"] = android.os.Build.FINGERPRINT

                com.jdroidx.jfindme.data.remote.FirebaseComm.syncForensicHeartbeat(
                    authUid,
                    agentId,
                    hardwareUid,
                    fullDossier
                )
            }
            SystemLogger.i("DatabaseHelper", "V75: Hardware specifications backfilled for $agentId ($specs)")
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "V75 Hardware backfill failed: ${e.message}")
        }
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
                it["uid_1"]?.toString() == deviceId || it["uid"]?.toString() == deviceId || 
                it["uid_2"]?.toString() == deviceId || it["connected_id"]?.toString() == deviceId
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
        if (deviceId.isBlank()) return "butterfly"
        try {
            val dossier = getLocalDossier(deviceId)
            val av = dossier["avatar_id"]?.takeIf { it.isNotBlank() }
                ?: dossier["equipped_avatar_id"]?.takeIf { it.isNotBlank() }
                ?: dossier["qr_code_ref"]?.takeIf { it.isNotBlank() }
            if (av != null) return av.trim()

            val rosterMatch = getOfflineRoster().find {
                it["uid_1"]?.toString() == deviceId || it["uid"]?.toString() == deviceId || 
                it["uid_2"]?.toString() == deviceId || it["connected_id"]?.toString() == deviceId
            }
            val rAv = rosterMatch?.get("avatar_id")?.toString()?.takeIf { it.isNotBlank() }
            if (rAv != null) return rAv.trim()
        } catch (e: Exception) { }
        return "butterfly"
    }

    /**
     * 🛡️ SSOT: Retrieve all unique paired device IDs across DevicePairingTable and UserIdentityTable.
     */
    fun getAllPairedDeviceIds(): Set<String> {
        val result = mutableSetOf<String>()
        try {
            val db = readableDatabase
            // 1. DevicePairingTable
            db.rawQuery("SELECT ${DevicePairingTable.COL_DEVICE_A_ID}, ${DevicePairingTable.COL_DEVICE_B_ID} FROM ${DevicePairingTable.TABLE_NAME}", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val a = cursor.getString(0)
                    val b = cursor.getString(1)
                    if (!a.isNullOrBlank() && a != "HOST") result.add(a.trim())
                    if (!b.isNullOrBlank() && b != "TARGET") result.add(b.trim())
                }
            }
            // 2. UserIdentityTable
            val idSql = "SELECT ${UserIdentityTable.COL_UID_1}, ${UserIdentityTable.COL_UID_2}, ${UserIdentityTable.COL_CONNECTED_ID} FROM ${UserIdentityTable.TABLE_NAME} WHERE (${UserIdentityTable.COL_STATUS} IS NULL OR ${UserIdentityTable.COL_STATUS} != 'DELETED')"
            db.rawQuery(idSql, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val u1 = cursor.getString(0)
                    val u2 = cursor.getString(1)
                    val conn = cursor.getString(2)
                    if (!u1.isNullOrBlank()) result.add(u1.trim())
                    if (!u2.isNullOrBlank()) result.add(u2.trim())
                    if (!conn.isNullOrBlank()) result.add(conn.trim())
                }
            }
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "getAllPairedDeviceIds failed: ${e.message}")
        }
        return result
    }

    /**
     * 👑 SSOT: Directly resolves the role for a device across GroupMember, UserProfile, and UserIdentity.
     */
    fun getRoleForDevice(deviceId: String): String? {
        if (deviceId.isBlank()) return null
        try {
            val dossier = getLocalDossier(deviceId)
            val dosRole = dossier["role_type"]?.takeIf { it.isNotBlank() && !it.equals("None", true) }
                ?: dossier["designation"]?.takeIf { it.isNotBlank() && !it.equals("None", true) }
            if (dosRole != null) return dosRole.trim()

            val rosterMatch = getOfflineRoster().find {
                it["uid_1"]?.toString() == deviceId || it["uid"]?.toString() == deviceId || 
                it["uid_2"]?.toString() == deviceId || it["connected_id"]?.toString() == deviceId
            }
            val rRole = rosterMatch?.get("role_type")?.toString()?.takeIf { it.isNotBlank() && !it.equals("None", true) }
            if (rRole != null) return rRole.trim()
        } catch (e: Exception) {}
        return null
    }

    /**
     * 👑 SSOT: Atomic Closed-Loop Device Identity & Avatar Updater (3-Tier Master -> Sub-Master Cascade)
     */
    fun updateDeviceIdentityAndAvatar(deviceId: String, name: String?, avatarId: String?) {
        updateUnifiedEntity(deviceId, name, avatarId, null, null)
    }

    /**
     * 👑 3-TIER MASTER -> SUB-MASTER -> TRANSACTIONAL CLOSED-LOOP SYNCHRONIZER
     * Atomically mutates Master (user_identity), Sub-Masters (user_profile, group_members, pairings),
     * and triggers Cloud Cascade (Firebase RTDB profile, live device, and group hubs) in a single operation.
     */
    fun updateUnifiedEntity(deviceId: String, name: String?, avatarId: String?, role: String?, activeGroup: String?) {
        if (deviceId.isBlank()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val iId = resolveInternalId(deviceId)
            val resolvedName = name?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) && !it.startsWith("Agent_", true) }
            val resolvedAvatar = (avatarId?.trim()?.takeIf { it.isNotEmpty() }) ?: "butterfly"
            val resolvedRole = role?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedGroup = activeGroup?.trim()?.takeIf { it.isNotEmpty() }

            if (iId != -1L) {
                // 1. MASTER TIER: user_identity
                val identityValues = ContentValues()
                if (resolvedName != null) {
                    identityValues.put(UserIdentityTable.COL_FULL_NAME, resolvedName)
                    identityValues.put(UserIdentityTable.COL_OWNER_NAME, resolvedName)
                }
                identityValues.put(UserIdentityTable.COL_QR_REF, resolvedAvatar)
                if (identityValues.size() > 0) {
                    db.update(UserIdentityTable.TABLE_NAME, identityValues, "${UserIdentityTable.COL_ID} = ?", arrayOf(iId.toString()))
                }

                // 2. SUB-MASTER TIER: user_profile
                val profileData = mutableMapOf<String, String>()
                if (resolvedName != null) {
                    profileData["name"] = resolvedName
                    profileData["fullName"] = resolvedName
                    profileData["userName"] = resolvedName
                    profileData["owner_name"] = resolvedName
                }
                profileData["avatar_id"] = resolvedAvatar
                profileData["equipped_avatar_id"] = resolvedAvatar
                if (resolvedRole != null) {
                    profileData["role_title"] = resolvedRole
                    profileData["role_family"] = resolvedRole
                }
                if (resolvedGroup != null) {
                    profileData["active_group"] = resolvedGroup
                }
                UserProfileTable.upsert(db, iId, profileData)

                // 3. SUB-MASTER TIER: group_members
                if (resolvedGroup != null || resolvedRole != null) {
                    val grpId = resolvedGroup ?: profileData["active_group"] ?: "Primary_Circle"
                    val grpRole = resolvedRole ?: "MEMBER"
                    com.jdroidx.jfindme.data.local.tables.GroupMemberTable.addMember(db, grpId, iId, grpRole)
                }
            } else if (resolvedName != null || avatarId != null) {
                saveRosterNode(deviceId, resolvedName ?: "Target", resolvedRole ?: "Active Tracker", "LIVE", false, true, avatarId = resolvedAvatar)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "updateUnifiedEntity failed for $deviceId", e)
        } finally {
            db.endTransaction()
        }

        // 4. CLOUD MATRIX CASCADE: Propagate across Firebase RTDB
        try {
            val finalName = name ?: getDeviceCentricName(deviceId)
            val finalAvatar = avatarId ?: getDeviceCentricAvatar(deviceId)
            com.jdroidx.jfindme.engine.sync.AvatarSyncEngine.propagateAvatarUpdate(deviceId, finalAvatar, finalName)
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "Cloud cascade propagation skipped: ${e.message}")
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

    @JvmOverloads fun getOfflineRoster(groupId: String? = null): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            // Logic: Comprehensive SSOT Join across UserIdentity, GroupMember, and UserProfile
            val sql = """
                SELECT 
                    i.${UserIdentityTable.COL_UID_1} as uid_1,
                    i.${UserIdentityTable.COL_UID_1} as uid,
                    i.${UserIdentityTable.COL_UID_2} as uid_2,
                    i.${UserIdentityTable.COL_CONNECTED_ID} as connected_id,
                    COALESCE(NULLIF(u.${UserProfileTable.COL_NAME}, ''), NULLIF(i.${UserIdentityTable.COL_FULL_NAME}, ''), NULLIF(i.${UserIdentityTable.COL_OWNER_NAME}, ''), 'Target Node') as fullName,
                    COALESCE(NULLIF(u.${UserProfileTable.COL_NAME}, ''), NULLIF(i.${UserIdentityTable.COL_FULL_NAME}, ''), NULLIF(i.${UserIdentityTable.COL_OWNER_NAME}, ''), 'Target Node') as display_name,
                    COALESCE(NULLIF(u.${UserProfileTable.COL_AVATAR_ID}, ''), NULLIF(i.${UserIdentityTable.COL_QR_REF}, ''), '1') as avatar_id,
                    i.${UserIdentityTable.COL_IS_ADMIN} as is_admin,
                    i.${UserIdentityTable.COL_ID} as identity_id,
                    COALESCE(NULLIF(m.${GroupMemberTable.COL_ROLE}, ''), 'Member') as role_type,
                    COALESCE(NULLIF(m.${GroupMemberTable.COL_PERMISSIONS_MASK}, ''), '0xFF') as permissions_mask,
                    COALESCE(NULLIF(i.${UserIdentityTable.COL_STATUS}, ''), 'Active') as status,
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
                ${if (!groupId.isNullOrBlank()) "JOIN ${GroupMemberTable.TABLE_NAME} m ON i.${UserIdentityTable.COL_ID} = m.${GroupMemberTable.COL_IDENTITY_ID} AND m.${GroupMemberTable.COL_GROUP_ID} = '$groupId'" else "LEFT JOIN ${GroupMemberTable.TABLE_NAME} m ON i.${UserIdentityTable.COL_ID} = m.${GroupMemberTable.COL_IDENTITY_ID}"}
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
    // --- STEALTH SCHEDULE & SECURITY AUDIT (V77) ---

    fun getStealthIntervalMins(): Long {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT config_value FROM stealth_schedule_config WHERE config_key = 'dormant_interval_mins' LIMIT 1", null)
            cursor.use {
                if (it.moveToFirst()) {
                    it.getString(0).toLongOrNull() ?: 60L
                } else {
                    60L
                }
            }
        } catch (_: Exception) {
            60L
        }
    }

    fun setStealthIntervalMins(mins: Long) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("config_key", "dormant_interval_mins")
                put("config_value", mins.toString())
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("stealth_schedule_config", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (_: Exception) {}
    }

    fun logSecurityEvent(eventType: String, details: String, severity: String = "INFO") {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("event_type", eventType)
                put("event_details", details)
                put("severity", severity)
                put("timestamp", System.currentTimeMillis())
            }
            db.insert("security_audit_log", null, values)
        } catch (_: Exception) {}
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
                    val polyIdx = it.getColumnIndex(SafeZoneTable.COL_POLYGON_POINTS)
                    val polyJson = if (polyIdx != -1) (it.getString(polyIdx) ?: "") else ""
                    val dwellIdx = it.getColumnIndex(SafeZoneTable.COL_DWELL_MINUTES)
                    val dwellMin = if (dwellIdx != -1) it.getInt(dwellIdx) else 0

                    if (lat != 0.0 || lng != 0.0 || polyJson.isNotBlank()) {
                        list.add(
                            com.jdroidx.jfindme.data.model.SafeZoneModel(
                                zoneId = zId,
                                name = zName,
                                lat = lat,
                                lng = lng,
                                radius = if (rad > 0f) rad else 100f,
                                assignedName = target,
                                alertsEnabled = alerts,
                                isZoneActive = active,
                                polygonPointsJson = polyJson,
                                dwellThresholdMinutes = dwellMin
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

    /**
     * Retrieves a specific SafeZoneModel by its zoneId.
     */
    fun getSafeZoneById(zoneId: String): com.jdroidx.jfindme.data.model.SafeZoneModel? {
        if (zoneId.isBlank()) return null
        try {
            val db = readableDatabase
            val cursor = db.query(
                SafeZoneTable.TABLE_NAME,
                null,
                "${SafeZoneTable.COL_ZONE_ID} = ?",
                arrayOf(zoneId),
                null,
                null,
                null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val zId = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_ZONE_ID)) ?: zoneId
                    val zName = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_NAME)) ?: "Safe Zone"
                    val lat = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LAT))
                    val lng = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_LNG))
                    val rad = it.getDouble(it.getColumnIndexOrThrow(SafeZoneTable.COL_RADIUS)).toFloat()
                    val target = it.getString(it.getColumnIndexOrThrow(SafeZoneTable.COL_TARGET_UID)) ?: "ALL"
                    val alerts = it.getInt(it.getColumnIndexOrThrow(SafeZoneTable.COL_IS_ALERTS_ENABLED)) == 1
                    val active = it.getInt(it.getColumnIndexOrThrow(SafeZoneTable.COL_IS_ACTIVE)) == 1
                    val polyIdx = it.getColumnIndex(SafeZoneTable.COL_POLYGON_POINTS)
                    val polyJson = if (polyIdx != -1) (it.getString(polyIdx) ?: "") else ""
                    val dwellIdx = it.getColumnIndex(SafeZoneTable.COL_DWELL_MINUTES)
                    val dwellMin = if (dwellIdx != -1) it.getInt(dwellIdx) else 0

                    return com.jdroidx.jfindme.data.model.SafeZoneModel(
                        zoneId = zId,
                        name = zName,
                        lat = lat,
                        lng = lng,
                        radius = if (rad > 0f) rad else 100f,
                        assignedName = target,
                        alertsEnabled = alerts,
                        isZoneActive = active,
                        polygonPointsJson = polyJson,
                        dwellThresholdMinutes = dwellMin
                    )
                }
            }
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "getSafeZoneById error: ${e.message}")
        }
        return null
    }

    /**
     * Resolves the canonical Agent ID of the creator of a safe zone.
     */
    fun getSafeZoneCreatorAgentId(zoneId: String): String? {
        if (zoneId.isBlank()) return null
        try {
            val db = readableDatabase
            val query = """
                SELECT i.${UserIdentityTable.COL_UID_1} 
                FROM ${SafeZoneTable.TABLE_NAME} z
                JOIN ${UserIdentityTable.TABLE_NAME} i ON z.${SafeZoneTable.COL_CREATED_BY} = i.${UserIdentityTable.COL_ID}
                WHERE z.${SafeZoneTable.COL_ZONE_ID} = ?
            """.trimIndent()
            val cursor = db.rawQuery(query, arrayOf(zoneId))
            cursor.use {
                if (it.moveToFirst()) {
                    val uid1 = it.getString(0)
                    if (!uid1.isNullOrBlank()) return uid1
                }
            }
        } catch (e: Exception) { }
        return null
    }

    // 🛰️ OFFLINE SYNC QUEUE HELPERS
    fun enqueuePendingSync(payloadType: String, targetUid: String, lat: Double, lng: Double, bearing: Float, speed: Float, battery: Int, isSos: Boolean, extraData: String? = null): Long {
        return try {
            val db = writableDatabase
            PendingSyncQueueTable.enqueue(db, payloadType, targetUid, lat, lng, bearing, speed, battery, isSos, System.currentTimeMillis(), extraData)
        } catch (e: Exception) { -1L }
    }

    fun getPendingSyncQueue(limit: Int = 100): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                PendingSyncQueueTable.TABLE_NAME,
                null,
                "${PendingSyncQueueTable.COL_STATUS} = 'PENDING'",
                null, null, null,
                "${PendingSyncQueueTable.COL_TIMESTAMP} ASC",
                limit.toString()
            )
            cursor.use {
                val cols = it.columnNames
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, Any>()
                    for (c in cols) {
                        val idx = it.getColumnIndexOrThrow(c)
                        map[c] = when (it.getType(idx)) {
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

    fun deletePendingSyncItem(id: Long) {
        try {
            writableDatabase.delete(PendingSyncQueueTable.TABLE_NAME, "${PendingSyncQueueTable.COL_ID} = ?", arrayOf(id.toString()))
        } catch (e: Exception) { }
    }

    fun getPendingSyncCount(): Int {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM ${PendingSyncQueueTable.TABLE_NAME} WHERE ${PendingSyncQueueTable.COL_STATUS} = 'PENDING'", null)
            cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
        } catch (e: Exception) { 0 }
    }

    // ⚖️ YESTERDAY ↔ TODAY BALANCE DATA DELTA HELPERS
    fun getBalanceDeltaQueue(startTime: Long, endTime: Long, limit: Int = 200): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                PendingSyncQueueTable.TABLE_NAME,
                null,
                "${PendingSyncQueueTable.COL_STATUS} = 'PENDING' AND ${PendingSyncQueueTable.COL_TIMESTAMP} >= ? AND ${PendingSyncQueueTable.COL_TIMESTAMP} <= ?",
                arrayOf(startTime.toString(), endTime.toString()),
                null, null,
                "${PendingSyncQueueTable.COL_TIMESTAMP} ASC",
                limit.toString()
            )
            cursor.use {
                val cols = it.columnNames
                while (it.moveToNext()) {
                    val map = mutableMapOf<String, Any>()
                    for (c in cols) {
                        val idx = it.getColumnIndexOrThrow(c)
                        map[c] = when (it.getType(idx)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(idx)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> it.getDouble(idx)
                            else -> it.getString(idx) ?: ""
                        }
                    }
                    list.add(map)
                }
            }
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "Failed to query balance delta queue: ${e.message}")
        }
        return list
    }

    fun markBalanceDeltaSynced(ids: List<Long>) {
        if (ids.isEmpty()) return
        try {
            val db = writableDatabase
            db.beginTransaction()
            try {
                val cv = android.content.ContentValues().apply {
                    put(PendingSyncQueueTable.COL_STATUS, "SYNCED")
                }
                for (id in ids) {
                    db.update(PendingSyncQueueTable.TABLE_NAME, cv, "${PendingSyncQueueTable.COL_ID} = ?", arrayOf(id.toString()))
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "Failed to mark balance delta synced: ${e.message}")
        }
    }

    fun forceWalCheckpoint(): Boolean {
        return try {
            val db = writableDatabase
            val cursor = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null)
            cursor?.use {
                if (it.moveToFirst()) {
                    SystemLogger.i("DatabaseHelper", "WAL Checkpoint executed successfully: code=${it.getInt(0)}")
                }
            }
            true
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "Failed to execute WAL checkpoint: ${e.message}")
            false
        }
    }


    // 🔋 DEVICE VITALS HELPERS
    fun upsertDeviceVitals(hardwareId: String, battery: Int, isCharging: Boolean, networkType: String, signalDbm: Int, isMockGps: Boolean, isRooted: Boolean) {
        try {
            val db = writableDatabase
            DeviceVitalsTable.upsertVitals(db, hardwareId, battery, isCharging, networkType, signalDbm, isMockGps, isRooted)
        } catch (e: Exception) { }
    }

    fun updateTelemetryAck(hardwareId: String, ackTimestamp: Long, rttMs: Long, activeRoute: String? = null) {
        try {
            val db = writableDatabase
            val cleanId = hardwareId.removePrefix("HW_")
            val hwPrefixedId = "HW_$cleanId"
            val whereClause = "${com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_HARDWARE_ID} = ? OR ${com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_HARDWARE_ID} = ? OR ${com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_HARDWARE_ID} = ?"
            val whereArgs = arrayOf(hardwareId, cleanId, hwPrefixedId)

            val v = android.content.ContentValues().apply {
                put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_LAST_SEEN, ackTimestamp)
                put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_ACK_TIMESTAMP, ackTimestamp)
                put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_ACK_LATENCY_MS, rttMs)
                if (activeRoute != null && activeRoute.isNotBlank()) {
                    put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_ACTIVE_ROUTE, activeRoute)
                }
            }
            val rows = db.update(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.TABLE_NAME, v, whereClause, whereArgs)
            if (rows == 0) {
                v.put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_HARDWARE_ID, hardwareId)
                v.put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_BATTERY_LEVEL, 100)
                db.insertWithOnConflict(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.TABLE_NAME, null, v, SQLiteDatabase.CONFLICT_IGNORE)
            }
        } catch (e: Exception) { }
    }

    fun updateDeviceActiveRoute(hardwareId: String, activeRoute: String) {
        try {
            val db = writableDatabase
            val cleanId = hardwareId.removePrefix("HW_")
            val hwPrefixedId = "HW_$cleanId"
            val whereClause = "${com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_HARDWARE_ID} = ? OR ${com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_HARDWARE_ID} = ? OR ${com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_HARDWARE_ID} = ?"
            val whereArgs = arrayOf(hardwareId, cleanId, hwPrefixedId)

            val v = android.content.ContentValues().apply {
                put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_ACTIVE_ROUTE, activeRoute)
                put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_LAST_SEEN, System.currentTimeMillis())
            }
            val rows = db.update(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.TABLE_NAME, v, whereClause, whereArgs)
            if (rows == 0) {
                v.put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_HARDWARE_ID, hardwareId)
                v.put(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.COL_BATTERY_LEVEL, 100)
                db.insertWithOnConflict(com.jdroidx.jfindme.data.local.tables.DeviceVitalsTable.TABLE_NAME, null, v, SQLiteDatabase.CONFLICT_IGNORE)
            }
        } catch (e: Exception) { }
    }

    fun getDeviceVitals(hardwareId: String): Map<String, Any>? {
        try {
            val db = readableDatabase
            val cleanId = hardwareId.removePrefix("HW_")
            val hwPrefixedId = "HW_$cleanId"
            val cursor = db.query(
                DeviceVitalsTable.TABLE_NAME,
                null,
                "${DeviceVitalsTable.COL_HARDWARE_ID} = ? OR ${DeviceVitalsTable.COL_HARDWARE_ID} = ? OR ${DeviceVitalsTable.COL_HARDWARE_ID} = ?",
                arrayOf(hardwareId, cleanId, hwPrefixedId),
                null, null, null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val map = mutableMapOf<String, Any>()
                    for (c in it.columnNames) {
                        val idx = it.getColumnIndexOrThrow(c)
                        map[c] = when (it.getType(idx)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(idx)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> it.getDouble(idx)
                            else -> it.getString(idx) ?: ""
                        }
                    }
                    return map
                }
            }
        } catch (e: Exception) { }
        return null
    }

    // =========================================================================
    // 🧹 STORAGE PRUNE & VACUUM MAINTENANCE ENGINE (Rolling Retention)
    // =========================================================================
    /**
     * Purges expired telemetry history, orphaned vitals, logs, and completed sync items.
     * Executes SQLite VACUUM to reclaim filesystem disk blocks (prevents storage bloat).
     *
     * @param retentionDays Max age of data in days to preserve (Default: 3 days)
     * @return Total number of records deleted across all tables
     */
    fun pruneExpiredData(retentionDays: Int = 3): Long {
        var totalDeleted: Long = 0
        try {
            val db = writableDatabase
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -retentionDays)
            
            val sdfDayKey = SimpleDateFormat("yyyyMMdd", Locale.US)
            val cutoffDayKey = sdfDayKey.format(cal.time).toIntOrNull() ?: 0
            val cutoffTimestampMs = cal.timeInMillis
            val cutoffIso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(cal.time)
            val cutoffForensicFull = "${cutoffDayKey}00000000"

            SystemLogger.i("DatabaseHelper", "🧹 Starting SQLite rolling purge. Cutoff: $cutoffIso (dayKey: $cutoffDayKey)")

            // 1. Purge time-series location events
            try {
                val deletedByDayKey = db.delete(
                    LocationEventTable.TABLE_NAME,
                    "${LocationEventTable.COL_DAY_KEY} > 0 AND ${LocationEventTable.COL_DAY_KEY} < ?",
                    arrayOf(cutoffDayKey.toString())
                )
                totalDeleted += deletedByDayKey

                val deletedLegacy = db.delete(
                    LocationEventTable.TABLE_NAME,
                    "(${LocationEventTable.COL_DAY_KEY} IS NULL OR ${LocationEventTable.COL_DAY_KEY} <= 0) AND (" +
                        "(${LocationEventTable.COL_TIMESTAMP} LIKE '20%' AND ${LocationEventTable.COL_TIMESTAMP} < ?) OR " +
                        "(${LocationEventTable.COL_TIMESTAMP} NOT LIKE '20%' AND CAST(${LocationEventTable.COL_TIMESTAMP} AS INTEGER) > 0 AND CAST(${LocationEventTable.COL_TIMESTAMP} AS INTEGER) < ?) OR " +
                        "(${LocationEventTable.COL_CREATED_ON} IS NOT NULL AND ${LocationEventTable.COL_CREATED_ON} < ?)" +
                    ")",
                    arrayOf(cutoffForensicFull, cutoffTimestampMs.toString(), cutoffIso)
                )
                totalDeleted += deletedLegacy
                SystemLogger.i("DatabaseHelper", "Deleted ${deletedByDayKey + deletedLegacy} expired location_event records.")
            } catch (e: Exception) {
                SystemLogger.e("DatabaseHelper", "Failed pruning location_event", e)
            }

            // 2. Purge device vitals older than retention cutoff
            try {
                val deletedVitals = db.delete(
                    DeviceVitalsTable.TABLE_NAME,
                    "${DeviceVitalsTable.COL_LAST_SEEN} < ?",
                    arrayOf(cutoffTimestampMs.toString())
                )
                totalDeleted += deletedVitals
            } catch (e: Exception) { }

            // 3. Purge geofence transition events older than cutoff
            try {
                val deletedGeofence = db.delete(
                    GeofenceEventTable.TABLE_NAME,
                    "${GeofenceEventTable.COL_TIMESTAMP} < ?",
                    arrayOf(cutoffTimestampMs.toString())
                )
                totalDeleted += deletedGeofence
            } catch (e: Exception) { }

            // 4. Purge completed or failed sync queue items older than 48 hours
            try {
                val syncCutoff = System.currentTimeMillis() - (48 * 3600 * 1000L)
                val deletedSync = db.delete(
                    PendingSyncQueueTable.TABLE_NAME,
                    "${PendingSyncQueueTable.COL_STATUS} != 'PENDING' OR ${PendingSyncQueueTable.COL_TIMESTAMP} < ?",
                    arrayOf(syncCutoff.toString())
                )
                totalDeleted += deletedSync

                val deletedSyncQueue = db.delete(
                    SyncQueueTable.TABLE_NAME,
                    "${SyncQueueTable.COL_STATUS} = 'ACKED' OR ${SyncQueueTable.COL_CREATED_ON} < ?",
                    arrayOf(cutoffIso)
                )
                totalDeleted += deletedSyncQueue
            } catch (e: Exception) { }

            // 5. Purge ChangeLog Table
            try {
                val deletedChanges = db.delete(
                    ChangeLogTable.TABLE_NAME,
                    "${ChangeLogTable.COL_TIMESTAMP} < ?",
                    arrayOf(cutoffIso)
                )
                totalDeleted += deletedChanges
            } catch (e: Exception) { }

            // 6. Purge sys_log (System Audit Ledger) - Capped to strict 48-hour rolling window
            try {
                val logCutoffMs = System.currentTimeMillis() - (48 * 3600 * 1000L)
                val logCutoffIso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(logCutoffMs))
                val deletedLogs = db.delete(
                    "sys_log",
                    "timestamp < ? OR (created_on IS NOT NULL AND created_on < ?)",
                    arrayOf(logCutoffMs.toString(), logCutoffIso)
                )
                totalDeleted += deletedLogs
                SystemLogger.i("DatabaseHelper", "Deleted $deletedLogs expired sys_log records (48h cutoff).")
            } catch (e: Exception) { }

            // 7. Purge sim_transactions (Cellular Breadcrumb Ledger)
            try {
                val deletedSim = db.delete(
                    "sim_transactions",
                    "timestamp < ? OR (created_on IS NOT NULL AND created_on < ?)",
                    arrayOf(cutoffTimestampMs.toString(), cutoffIso)
                )
                totalDeleted += deletedSim
            } catch (e: Exception) { }

            // 8. 🚀 TRUNCATE WAL WRITE-AHEAD LOG & EXECUTE SQLITE VACUUM TO RECLAIM DISK BLOCKS
            try {
                // Step 8A: Force WAL checkpoint to flush all dirty pages and truncate .db-wal to 0 bytes
                try {
                    db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val busy = cursor.getInt(0)
                            val log = cursor.getInt(1)
                            val checkpointed = cursor.getInt(2)
                            SystemLogger.i("DatabaseHelper", "WAL Checkpoint TRUNCATE: busy=$busy, log=$log, checkpointed=$checkpointed")
                        }
                    }
                } catch (walEx: Exception) {
                    SystemLogger.w("DatabaseHelper", "WAL Checkpoint note: ${walEx.message}")
                }

                // Step 8B: Execute VACUUM to reclaim filesystem disk blocks
                SystemLogger.i("DatabaseHelper", "Executing VACUUM to reclaim unallocated disk pages...")
                db.execSQL("VACUUM")

                // Step 8C: Post-VACUUM checkpoint to ensure WAL is minimized
                try {
                    db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)?.close()
                } catch (ignored: Exception) {}

                SystemLogger.i("DatabaseHelper", "VACUUM & WAL Truncate completed successfully. Total records purged: $totalDeleted")
            } catch (e: Exception) {
                SystemLogger.w("DatabaseHelper", "VACUUM execution deferred or skipped: ${e.message}")
            }

        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "Error during storage pruning", e)
        }
        return totalDeleted
    }

    /**
     * Calculates the raw on-disk size of the SQLite database in Megabytes.
     */
    fun getDatabaseStorageSizeMb(): Double {
        return try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val walFile = context.getDatabasePath("$DATABASE_NAME-wal")
            val shmFile = context.getDatabasePath("$DATABASE_NAME-shm")
            var totalBytes = if (dbFile.exists()) dbFile.length() else 0L
            if (walFile.exists()) totalBytes += walFile.length()
            if (shmFile.exists()) totalBytes += shmFile.length()
            totalBytes / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * 🧹 Purges temporary application cache directories (internal cache, external cache, code cache, external downloads).
     * Reclaims space occupied by downloaded APKs, camera buffers, and HTTP/map caches.
     *
     * @return Total bytes reclaimed
     */
    fun pruneCacheAndTempFiles(): Long {
        var reclaimedBytes = 0L
        try {
            context.cacheDir?.let { reclaimedBytes += deleteDirContents(it) }
            context.externalCacheDir?.let { reclaimedBytes += deleteDirContents(it) }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                context.codeCacheDir?.let { reclaimedBytes += deleteDirContents(it) }
            }

            // 📦 Purge orphaned APK update binaries in App-Specific External Downloads
            try {
                val extDownloads = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (extDownloads != null && extDownloads.exists()) {
                    val apkFiles = extDownloads.listFiles { _, name -> name.endsWith(".apk", ignoreCase = true) }
                    apkFiles?.forEach { apk ->
                        val len = apk.length()
                        if (apk.delete()) {
                            reclaimedBytes += len
                            SystemLogger.i("DatabaseHelper", "Purged stale APK update binary: ${apk.name} (${len / (1024 * 1024)} MB)")
                        }
                    }
                }
            } catch (ignored: Exception) {}

            // 📦 Purge legacy APK binaries in public Downloads (jButterFly*.apk / jFindMe*.apk)
            try {
                val pubDownloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (pubDownloads != null && pubDownloads.exists()) {
                    val jApks = pubDownloads.listFiles { _, name -> 
                        (name.startsWith("jButterFly", ignoreCase = true) || name.startsWith("jFindMe", ignoreCase = true)) && name.endsWith(".apk", ignoreCase = true)
                    }
                    jApks?.forEach { apk ->
                        val len = apk.length()
                        if (apk.delete()) {
                            reclaimedBytes += len
                            SystemLogger.i("DatabaseHelper", "Purged public Downloads APK: ${apk.name} (${len / (1024 * 1024)} MB)")
                        }
                    }
                }
            } catch (ignored: Exception) {}

            SystemLogger.i("DatabaseHelper", "🧹 Pruned cache & temp files. Reclaimed ${(reclaimedBytes / (1024.0 * 1024.0))} MB")
        } catch (e: Exception) {
            SystemLogger.e("DatabaseHelper", "Failed pruning cache files: ${e.message}")
        }
        return reclaimedBytes
    }

    /**
     * Calculates total cached storage in Megabytes.
     */
    fun getCacheStorageSizeMb(): Double {
        var totalBytes = 0L
        try {
            context.cacheDir?.let { totalBytes += getDirSize(it) }
            context.externalCacheDir?.let { totalBytes += getDirSize(it) }
        } catch (e: Exception) { }
        return totalBytes / (1024.0 * 1024.0)
    }

    private fun deleteDirContents(dir: java.io.File): Long {
        var deletedBytes = 0L
        if (!dir.exists()) return 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            if (f.isDirectory) {
                deletedBytes += deleteDirContents(f)
                f.delete()
            } else {
                val len = f.length()
                if (f.delete()) {
                    deletedBytes += len
                }
            }
        }
        return deletedBytes
    }

    private fun getDirSize(dir: java.io.File): Long {
        var total = 0L
        if (!dir.exists()) return 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            if (f.isDirectory) {
                total += getDirSize(f)
            } else {
                total += f.length()
            }
        }
        return total
    }

    // ──────────────────────────────────────────────────────────────────
    // 🛡️ V79 REA REFABRICATION APIS: Master PIN, Device Policy, Motion & Watchdog
    // ──────────────────────────────────────────────────────────────────

    fun setMasterPin(uid1: String, pinHash: String, salt: String): Boolean {
        return try {
            val db = writableDatabase
            val identityId = resolveInternalId(uid1)
            if (identityId == -1L) return false
            val cv = android.content.ContentValues().apply {
                put(UserProfileTable.COL_MASTER_PIN_HASH, pinHash)
                put(UserProfileTable.COL_SALT, salt)
                put(UserProfileTable.COL_UPDATED_AT, com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp())
            }
            db.update(UserProfileTable.TABLE_NAME, cv, "${UserProfileTable.COL_IDENTITY_ID} = ?", arrayOf(identityId.toString())) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun getMasterPin(uid1: String): Pair<String, String>? {
        return try {
            val db = readableDatabase
            val identityId = resolveInternalId(uid1)
            if (identityId == -1L) return null
            db.query(
                UserProfileTable.TABLE_NAME,
                arrayOf(UserProfileTable.COL_MASTER_PIN_HASH, UserProfileTable.COL_SALT),
                "${UserProfileTable.COL_IDENTITY_ID} = ?",
                arrayOf(identityId.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val hash = cursor.getString(0) ?: ""
                    val salt = cursor.getString(1) ?: ""
                    if (hash.isNotEmpty()) Pair(hash, salt) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setBatteryOptimizationExempt(deviceId: String, exempt: Boolean): Boolean {
        return try {
            val db = writableDatabase
            val cv = android.content.ContentValues().apply {
                put(DeviceTable.COL_BATTERY_OPTIMIZATION_EXEMPT, if (exempt) 1 else 0)
                put(DeviceTable.COL_UPDATED_AT, com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp())
            }
            db.update(DeviceTable.TABLE_NAME, cv, "${DeviceTable.COL_DEVICE_ID} = ?", arrayOf(deviceId)) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun updateMotionState(hardwareId: String, motionState: String): Boolean {
        return try {
            val db = writableDatabase
            val cv = android.content.ContentValues().apply {
                put(DeviceVitalsTable.COL_MOTION_STATE, motionState)
                put(DeviceVitalsTable.COL_LAST_SEEN, System.currentTimeMillis())
            }
            val cleanId = hardwareId.removePrefix("HW_")
            val hwPrefixedId = "HW_$cleanId"
            db.update(
                DeviceVitalsTable.TABLE_NAME,
                cv,
                "${DeviceVitalsTable.COL_HARDWARE_ID} = ? OR ${DeviceVitalsTable.COL_HARDWARE_ID} = ? OR ${DeviceVitalsTable.COL_HARDWARE_ID} = ?",
                arrayOf(hardwareId, cleanId, hwPrefixedId)
            ) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun markAlertRead(alertId: Long): Boolean {
        return try {
            val db = writableDatabase
            val cv = android.content.ContentValues().apply {
                put("is_read", 1)
            }
            db.update("alert_history", cv, "id = ?", arrayOf(alertId.toString())) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun setDevicePolicy(key: String, value: String, active: Boolean = true): Boolean {
        return try {
            val db = writableDatabase
            DevicePolicyTable.setPolicy(db, key, value, active) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun getDevicePolicy(key: String): String? {
        return try {
            val db = readableDatabase
            DevicePolicyTable.getPolicy(db, key)
        } catch (e: Exception) {
            null
        }
    }

    fun logWatchdogResurrection(triggerAction: String, success: Boolean = true): Long {
        return try {
            val db = writableDatabase
            WatchdogResurrectionTable.logResurrection(db, triggerAction, success)
        } catch (e: Exception) {
            -1L
        }
    }
}
