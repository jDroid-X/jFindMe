package com.jdroidx.jbutterfly.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jdroidx.jbutterfly.data.model.LiveDeviceModel
import com.jdroidx.jbutterfly.data.model.NotificationItem
import com.jdroidx.jbutterfly.data.model.SafeZoneModel
import com.jdroidx.jbutterfly.data.model.UserLocation
import com.jdroidx.jbutterfly.utils.SystemLogger

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseHelper"
        const val DATABASE_NAME = "jButterFly.db"
        const val DATABASE_VERSION = 2

        // Table Names
        const val TABLE_ROSTER = "master_roster"
        const val TABLE_SAFE_ZONES = "safe_zones"
        const val TABLE_HISTORY = "tracking_history"
        const val TABLE_SETTINGS = "dynamic_settings"
        const val TABLE_NOTIFICATIONS = "incident_notifications"

        // Roster Columns
        const val COL_ID = "id"
        const val COL_AGENT_ID = "agent_id"
        const val COL_HARDWARE_ID = "hardware_id"
        const val COL_DEVICE_NAME = "device_name"
        const val COL_ROLE = "role"
        const val COL_CATEGORY = "category"
        const val COL_AVATAR_NAME = "avatar_name"
        const val COL_BATTERY = "battery_pct"
        const val COL_LAT = "latitude"
        const val COL_LNG = "longitude"
        const val COL_SPEED = "speed"
        const val COL_HEADING = "heading"
        const val COL_LAST_SEEN = "last_seen_ts"
        const val COL_IS_SOS = "is_sos"
        const val COL_IS_GHOST = "is_ghost"

        // SafeZone Columns
        const val COL_ZONE_ID = "zone_id"
        const val COL_ZONE_NAME = "name"
        const val COL_RADIUS = "radius_meters"
        const val COL_NOTIFY_ENTRY = "notify_entry"
        const val COL_NOTIFY_EXIT = "notify_exit"
        const val COL_CREATED_AT = "created_at"

        // History Columns
        const val COL_ACCURACY = "accuracy"
        const val COL_TIMESTAMP = "timestamp"

        // Settings Columns
        const val COL_KEY = "config_key"
        const val COL_VALUE = "config_value"
        const val COL_DESC = "description"
        const val COL_UPDATED_AT = "updated_at"

        // Notification Columns
        const val COL_NOTIF_TITLE = "title"
        const val COL_NOTIF_MSG = "message"
        const val COL_NOTIF_CAT = "category"
        const val COL_NOTIF_READ = "is_read"

        @Volatile
        private var instance: DatabaseHelper? = null

        @JvmStatic
        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        SystemLogger.i(TAG, "Creating jButterFly SQLite Tables...")

        // Master Roster Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ROSTER (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_AGENT_ID TEXT UNIQUE,
                $COL_HARDWARE_ID TEXT,
                $COL_DEVICE_NAME TEXT,
                $COL_ROLE TEXT,
                $COL_CATEGORY TEXT,
                $COL_AVATAR_NAME TEXT,
                $COL_BATTERY INTEGER DEFAULT 100,
                $COL_LAT REAL DEFAULT 0.0,
                $COL_LNG REAL DEFAULT 0.0,
                $COL_SPEED REAL DEFAULT 0.0,
                $COL_HEADING REAL DEFAULT 0.0,
                $COL_LAST_SEEN INTEGER DEFAULT 0,
                $COL_IS_SOS INTEGER DEFAULT 0,
                $COL_IS_GHOST INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        // Safe Zones Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SAFE_ZONES (
                $COL_ZONE_ID TEXT PRIMARY KEY,
                $COL_ZONE_NAME TEXT,
                $COL_LAT REAL,
                $COL_LNG REAL,
                $COL_RADIUS REAL,
                $COL_NOTIFY_ENTRY INTEGER DEFAULT 1,
                $COL_NOTIFY_EXIT INTEGER DEFAULT 1,
                $COL_CREATED_AT INTEGER
            )
            """.trimIndent()
        )

        // Location Tracking History Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_AGENT_ID TEXT,
                $COL_LAT REAL,
                $COL_LNG REAL,
                $COL_ACCURACY REAL,
                $COL_SPEED REAL,
                $COL_TIMESTAMP INTEGER
            )
            """.trimIndent()
        )

        // Dynamic Settings Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SETTINGS (
                $COL_KEY TEXT PRIMARY KEY,
                $COL_VALUE TEXT,
                $COL_DESC TEXT,
                $COL_UPDATED_AT INTEGER
            )
            """.trimIndent()
        )

        // Incident & Notifications Ledger Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NOTIFICATIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTIF_TITLE TEXT,
                $COL_NOTIF_MSG TEXT,
                $COL_NOTIF_CAT TEXT,
                $COL_TIMESTAMP INTEGER,
                $COL_NOTIF_READ INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        // Enterprise SQLite Query Performance Indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_roster_cat_seen ON $TABLE_ROSTER($COL_CATEGORY, $COL_LAST_SEEN DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_roster_agent ON $TABLE_ROSTER($COL_AGENT_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_agent_ts ON $TABLE_HISTORY($COL_AGENT_ID, $COL_TIMESTAMP DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_safezones_created ON $TABLE_SAFE_ZONES($COL_CREATED_AT DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_notif_time ON $TABLE_NOTIFICATIONS($COL_TIMESTAMP DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        SystemLogger.i(TAG, "Upgrading database from $oldVersion to $newVersion")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROSTER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAFE_ZONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        onCreate(db)
    }

    // ─── ROSTER OPERATIONS ──────────────────────────────────────────────

    fun insertOrUpdateDevice(device: LiveDeviceModel): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_AGENT_ID, device.agentId)
            put(COL_HARDWARE_ID, device.hardwareId)
            put(COL_DEVICE_NAME, device.deviceName)
            put(COL_ROLE, device.role)
            put(COL_CATEGORY, device.category)
            put(COL_AVATAR_NAME, device.avatarName)
            put(COL_BATTERY, device.batteryPct)
            put(COL_LAT, device.latitude)
            put(COL_LNG, device.longitude)
            put(COL_SPEED, device.speed)
            put(COL_HEADING, device.heading)
            put(COL_LAST_SEEN, device.lastSeenTs)
            put(COL_IS_SOS, if (device.isSos) 1 else 0)
            put(COL_IS_GHOST, if (device.isGhost) 1 else 0)
        }
        return db.insertWithOnConflict(TABLE_ROSTER, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllPairedDevices(): List<LiveDeviceModel> {
        val list = mutableListOf<LiveDeviceModel>()
        val db = readableDatabase
        val cursor = db.query(TABLE_ROSTER, null, null, null, null, null, "$COL_LAST_SEEN DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToDevice(c))
            }
        }
        return list
    }

    fun getDevicesByCategory(category: String): List<LiveDeviceModel> {
        val list = mutableListOf<LiveDeviceModel>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ROSTER, null,
            "$COL_CATEGORY = ?", arrayOf(category),
            null, null, "$COL_LAST_SEEN DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToDevice(c))
            }
        }
        return list
    }

    fun getDeviceByAgentId(agentId: String): LiveDeviceModel? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ROSTER, null,
            "$COL_AGENT_ID = ?", arrayOf(agentId),
            null, null, null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToDevice(c)
            }
        }
        return null
    }

    fun updateDeviceLocation(agentId: String, lat: Double, lng: Double, speed: Float, heading: Float, battery: Int) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_LAT, lat)
            put(COL_LNG, lng)
            put(COL_SPEED, speed)
            put(COL_HEADING, heading)
            put(COL_BATTERY, battery)
            put(COL_LAST_SEEN, System.currentTimeMillis())
        }
        db.update(TABLE_ROSTER, cv, "$COL_AGENT_ID = ?", arrayOf(agentId))
    }

    fun removeDevice(agentId: String): Int {
        val db = writableDatabase
        // Clean up historical tracking trail for this node to prevent orphan records
        db.delete(TABLE_HISTORY, "$COL_AGENT_ID = ?", arrayOf(agentId))
        return db.delete(TABLE_ROSTER, "$COL_AGENT_ID = ?", arrayOf(agentId))
    }

    private fun cursorToDevice(c: Cursor): LiveDeviceModel {
        return LiveDeviceModel(
            agentId = c.getString(c.getColumnIndexOrThrow(COL_AGENT_ID)),
            hardwareId = c.getString(c.getColumnIndexOrThrow(COL_HARDWARE_ID)),
            deviceName = c.getString(c.getColumnIndexOrThrow(COL_DEVICE_NAME)),
            role = c.getString(c.getColumnIndexOrThrow(COL_ROLE)),
            category = c.getString(c.getColumnIndexOrThrow(COL_CATEGORY)),
            avatarName = c.getString(c.getColumnIndexOrThrow(COL_AVATAR_NAME)),
            batteryPct = c.getInt(c.getColumnIndexOrThrow(COL_BATTERY)),
            latitude = c.getDouble(c.getColumnIndexOrThrow(COL_LAT)),
            longitude = c.getDouble(c.getColumnIndexOrThrow(COL_LNG)),
            speed = c.getFloat(c.getColumnIndexOrThrow(COL_SPEED)),
            heading = c.getFloat(c.getColumnIndexOrThrow(COL_HEADING)),
            lastSeenTs = c.getLong(c.getColumnIndexOrThrow(COL_LAST_SEEN)),
            isSos = c.getInt(c.getColumnIndexOrThrow(COL_IS_SOS)) == 1,
            isGhost = c.getInt(c.getColumnIndexOrThrow(COL_IS_GHOST)) == 1
        )
    }

    // ─── SAFEZONE OPERATIONS ──────────────────────────────────────────

    fun insertOrUpdateSafeZone(zone: SafeZoneModel): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ZONE_ID, zone.zoneId)
            put(COL_ZONE_NAME, zone.name)
            put(COL_LAT, zone.latitude)
            put(COL_LNG, zone.longitude)
            put(COL_RADIUS, zone.radiusMeters)
            put(COL_NOTIFY_ENTRY, if (zone.notifyEntry) 1 else 0)
            put(COL_NOTIFY_EXIT, if (zone.notifyExit) 1 else 0)
            put(COL_CREATED_AT, zone.createdAt)
        }
        return db.insertWithOnConflict(TABLE_SAFE_ZONES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllSafeZones(): List<SafeZoneModel> {
        val list = mutableListOf<SafeZoneModel>()
        val db = readableDatabase
        val cursor = db.query(TABLE_SAFE_ZONES, null, null, null, null, null, "$COL_CREATED_AT DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    SafeZoneModel(
                        zoneId = c.getString(c.getColumnIndexOrThrow(COL_ZONE_ID)),
                        name = c.getString(c.getColumnIndexOrThrow(COL_ZONE_NAME)),
                        latitude = c.getDouble(c.getColumnIndexOrThrow(COL_LAT)),
                        longitude = c.getDouble(c.getColumnIndexOrThrow(COL_LNG)),
                        radiusMeters = c.getDouble(c.getColumnIndexOrThrow(COL_RADIUS)),
                        notifyEntry = c.getInt(c.getColumnIndexOrThrow(COL_NOTIFY_ENTRY)) == 1,
                        notifyExit = c.getInt(c.getColumnIndexOrThrow(COL_NOTIFY_EXIT)) == 1,
                        createdAt = c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT))
                    )
                )
            }
        }
        return list
    }

    fun deleteSafeZone(zoneId: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_SAFE_ZONES, "$COL_ZONE_ID = ?", arrayOf(zoneId))
    }

    // ─── HISTORY OPERATIONS ───────────────────────────────────────────

    fun recordHistory(loc: UserLocation) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_AGENT_ID, loc.agentId)
            put(COL_LAT, loc.latitude)
            put(COL_LNG, loc.longitude)
            put(COL_ACCURACY, loc.accuracy)
            put(COL_SPEED, loc.speed)
            put(COL_TIMESTAMP, loc.timestamp)
        }
        db.insert(TABLE_HISTORY, null, cv)
    }

    fun getHistoryForAgent(agentId: String, limit: Int = 100): List<UserLocation> {
        val list = mutableListOf<UserLocation>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HISTORY, null,
            "$COL_AGENT_ID = ?", arrayOf(agentId),
            null, null, "$COL_TIMESTAMP DESC", "$limit"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    UserLocation(
                        agentId = c.getString(c.getColumnIndexOrThrow(COL_AGENT_ID)),
                        latitude = c.getDouble(c.getColumnIndexOrThrow(COL_LAT)),
                        longitude = c.getDouble(c.getColumnIndexOrThrow(COL_LNG)),
                        accuracy = c.getFloat(c.getColumnIndexOrThrow(COL_ACCURACY)),
                        speed = c.getFloat(c.getColumnIndexOrThrow(COL_SPEED)),
                        timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
                    )
                )
            }
        }
        return list
    }

    fun pruneOldHistory(keepDays: Int = 30): Int {
        val cutoff = System.currentTimeMillis() - (keepDays.toLong() * 24 * 60 * 60 * 1000L)
        val db = writableDatabase
        return db.delete(TABLE_HISTORY, "$COL_TIMESTAMP < ?", arrayOf(cutoff.toString()))
    }

    fun clearAllHistory(agentId: String? = null): Int {
        val db = writableDatabase
        return if (agentId != null) {
            db.delete(TABLE_HISTORY, "$COL_AGENT_ID = ?", arrayOf(agentId))
        } else {
            db.delete(TABLE_HISTORY, null, null)
        }
    }

    // ─── DYNAMIC CONFIGURATION TABLE ──────────────────────────────────

    fun setConfig(key: String, value: String, description: String = "") {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_KEY, key)
            put(COL_VALUE, value)
            put(COL_DESC, description)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getConfig(key: String, defaultValue: String = ""): String {
        val db = readableDatabase
        val cursor = db.query(TABLE_SETTINGS, arrayOf(COL_VALUE), "$COL_KEY = ?", arrayOf(key), null, null, null)
        cursor.use { c ->
            if (c.moveToFirst()) {
                return c.getString(0) ?: defaultValue
            }
        }
        return defaultValue
    }

    // ─── INCIDENT NOTIFICATIONS OPERATIONS ────────────────────────────

    fun insertNotification(title: String, message: String, category: String = "SYSTEM"): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_NOTIF_TITLE, title)
            put(COL_NOTIF_MSG, message)
            put(COL_NOTIF_CAT, category)
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_NOTIF_READ, 0)
        }
        return db.insert(TABLE_NOTIFICATIONS, null, cv)
    }

    @JvmOverloads
    fun getAllNotifications(limit: Int = 100): List<NotificationItem> {
        val list = mutableListOf<NotificationItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTIFICATIONS, null, null, null, null, null,
            "$COL_TIMESTAMP DESC", "$limit"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    NotificationItem(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                        title = c.getString(c.getColumnIndexOrThrow(COL_NOTIF_TITLE)),
                        message = c.getString(c.getColumnIndexOrThrow(COL_NOTIF_MSG)),
                        category = c.getString(c.getColumnIndexOrThrow(COL_NOTIF_CAT)),
                        timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        isRead = c.getInt(c.getColumnIndexOrThrow(COL_NOTIF_READ)) == 1
                    )
                )
            }
        }
        return list
    }

    fun clearAllNotifications(): Int {
        val db = writableDatabase
        return db.delete(TABLE_NOTIFICATIONS, null, null)
    }

    // ─── ROSTER PROFILE EDIT ──────────────────────────────────────────

    fun updateDeviceProfile(agentId: String, deviceName: String, role: String, category: String, avatarName: String): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_DEVICE_NAME, deviceName)
            put(COL_ROLE, role)
            put(COL_CATEGORY, category)
            put(COL_AVATAR_NAME, avatarName)
        }
        return db.update(TABLE_ROSTER, cv, "$COL_AGENT_ID = ?", arrayOf(agentId))
    }

    fun getLatestLocationForAgent(agentId: String): UserLocation? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HISTORY, null,
            "$COL_AGENT_ID = ?", arrayOf(agentId),
            null, null, "$COL_TIMESTAMP DESC", "1"
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return UserLocation(
                    agentId = c.getString(c.getColumnIndexOrThrow(COL_AGENT_ID)),
                    latitude = c.getDouble(c.getColumnIndexOrThrow(COL_LAT)),
                    longitude = c.getDouble(c.getColumnIndexOrThrow(COL_LNG)),
                    accuracy = c.getFloat(c.getColumnIndexOrThrow(COL_ACCURACY)),
                    speed = c.getFloat(c.getColumnIndexOrThrow(COL_SPEED)),
                    timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
                )
            }
        }
        return null
    }
}
