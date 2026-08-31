package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * Tier 4: Event & Tracking Layer
 * 🛡️ safe_zone: Master Safe Zone definitions
 */
object SafeZoneTable {
    const val TABLE_NAME = "safe_zone"

    const val COL_ZONE_ID = "zone_id" // PK
    const val COL_NAME = "zone_name"
    const val COL_ZONE_NAME = "zone_name" // Alias for legacy support
    const val COL_LAT = "lat"
    const val COL_LNG = "lng"
    const val COL_RADIUS = "radius"
    const val COL_CREATED_BY = "created_by" // 🧬 V65.0: Internal Identity Handle (FK)
    const val COL_TARGET_UID = "target_uid" // Group/Target link
    const val COL_IS_ALERTS_ENABLED = "is_alerts_enabled"
    const val COL_IS_ACTIVE = "is_active"
    const val COL_IS_ARCHIVED = "is_archived"
    const val COL_CREATED_ON = "created_on"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ZONE_ID TEXT PRIMARY KEY,
            $COL_NAME TEXT,
            $COL_LAT REAL,
            $COL_LNG REAL,
            $COL_RADIUS REAL,
            $COL_CREATED_BY INTEGER,
            $COL_TARGET_UID TEXT,
            $COL_IS_ALERTS_ENABLED INTEGER DEFAULT 1,
            $COL_IS_ACTIVE INTEGER DEFAULT 1,
            $COL_IS_ARCHIVED INTEGER DEFAULT 0,
            $COL_CREATED_ON TEXT,
            FOREIGN KEY ($COL_CREATED_BY) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE SET NULL
        )
    """.trimIndent()

    fun save(db: SQLiteDatabase, zoneId: String, name: String?, lat: Double, lng: Double, radius: Double, createdBy: Long?, target: String?, alerts: Boolean, active: Boolean, archived: Boolean = false) {
        val now = com.jdroidx.jfindme.data.remote.FirebaseComm.getForensicTimestamp()
        try {
            val v = ContentValues().apply {
                put(COL_ZONE_ID, zoneId)
                put(COL_NAME, name)
                put(COL_LAT, lat)
                put(COL_LNG, lng)
                put(COL_RADIUS, radius)
                put(COL_CREATED_BY, createdBy)
                put(COL_TARGET_UID, target)
                put(COL_IS_ALERTS_ENABLED, if (alerts) 1 else 0)
                put(COL_IS_ACTIVE, if (active) 1 else 0)
                put(COL_IS_ARCHIVED, if (archived) 1 else 0)
            }
            val rows = db.update(TABLE_NAME, v, "$COL_ZONE_ID = ?", arrayOf(zoneId))
            if (rows == 0) {
                v.put(COL_CREATED_ON, now)
                db.insert(TABLE_NAME, null, v)
            }
        } catch (e: Exception) {
            // Self-healing fallback: Ensure table columns exist if a legacy schema is encountered
            try { db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_IS_ARCHIVED INTEGER DEFAULT 0") } catch (ex: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_IS_ACTIVE INTEGER DEFAULT 1") } catch (ex: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_IS_ALERTS_ENABLED INTEGER DEFAULT 1") } catch (ex: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_CREATED_BY INTEGER") } catch (ex: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_TARGET_UID TEXT") } catch (ex: Exception) {}
            try { db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_CREATED_ON TEXT") } catch (ex: Exception) {}

            try {
                val v = ContentValues().apply {
                    put(COL_ZONE_ID, zoneId)
                    put(COL_NAME, name)
                    put(COL_LAT, lat)
                    put(COL_LNG, lng)
                    put(COL_RADIUS, radius)
                    put(COL_CREATED_BY, createdBy)
                    put(COL_TARGET_UID, target)
                    put(COL_IS_ALERTS_ENABLED, if (alerts) 1 else 0)
                    put(COL_IS_ACTIVE, if (active) 1 else 0)
                    put(COL_IS_ARCHIVED, if (archived) 1 else 0)
                }
                val rows = db.update(TABLE_NAME, v, "$COL_ZONE_ID = ?", arrayOf(zoneId))
                if (rows == 0) {
                    v.put(COL_CREATED_ON, now)
                    db.insert(TABLE_NAME, null, v)
                }
            } catch (ex: Exception) {
                android.util.Log.e("SafeZoneTable", "SafeZoneTable save error: ${ex.message}")
            }
        }
    }

    fun upsert(db: SQLiteDatabase, zoneId: String, name: String?, lat: Double, lng: Double, radius: Double, createdBy: Long?) {
        save(db, zoneId, name, lat, lng, radius, createdBy, null, true, true, false)
    }
}
