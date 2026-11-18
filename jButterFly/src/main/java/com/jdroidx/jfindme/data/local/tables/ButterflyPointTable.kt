package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] pointId, creatorDeviceId, creatorIdentityId, pointName, address, lat, lng, geohash, rendezvousTimestamp, expiryTimestamp, arrivalRadiusMeters, notes, iconType, isActive, isArchived, createdAt, updatedAt, now
 * [Constants] TABLE_NAME = "butterfly_point", COL_POINT_ID, COL_CREATOR_DEVICE_ID, COL_CREATOR_IDENTITY_ID, COL_POINT_NAME, COL_ADDRESS, COL_LAT, COL_LNG, COL_GEOHASH, COL_RENDEZVOUS_TIMESTAMP, COL_EXPIRY_TIMESTAMP, COL_ARRIVAL_RADIUS_METERS, COL_NOTES, COL_ICON_TYPE, COL_IS_ACTIVE, COL_IS_ARCHIVED, COL_CREATED_AT, COL_UPDATED_AT
 * [Dependencies] android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm, com.jdroidx.jfindme.data.local.tables.UserIdentityTable
 * [Integration Points] DatabaseHelper.kt, MapActivity.java, MapViewModel.kt, ButterflyPointEngine.kt, Firebase RTDB /butterfly_points/{pointId}
 *
 * Tier 1: Master Database Entity for Tactical Butterfly Point Rendezvous Anchors
 */
object ButterflyPointTable {
    const val TABLE_NAME = "butterfly_point"

    const val COL_POINT_ID = "point_id" // PK
    const val COL_CREATOR_DEVICE_ID = "creator_device_id"
    const val COL_CREATOR_IDENTITY_ID = "creator_identity_id"
    const val COL_POINT_NAME = "point_name"
    const val COL_ADDRESS = "address"
    const val COL_LAT = "lat"
    const val COL_LNG = "lng"
    const val COL_GEOHASH = "geohash"
    const val COL_RENDEZVOUS_TIMESTAMP = "rendezvous_timestamp"
    const val COL_EXPIRY_TIMESTAMP = "expiry_timestamp"
    const val COL_ARRIVAL_RADIUS_METERS = "arrival_radius_meters"
    const val COL_NOTES = "notes"
    const val COL_ICON_TYPE = "icon_type"
    const val COL_IS_ACTIVE = "is_active"
    const val COL_IS_ARCHIVED = "is_archived"
    const val COL_CREATED_AT = "created_at"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_POINT_ID TEXT PRIMARY KEY,
            $COL_CREATOR_DEVICE_ID TEXT NOT NULL,
            $COL_CREATOR_IDENTITY_ID INTEGER,
            $COL_POINT_NAME TEXT NOT NULL,
            $COL_ADDRESS TEXT,
            $COL_LAT REAL NOT NULL,
            $COL_LNG REAL NOT NULL,
            $COL_GEOHASH TEXT,
            $COL_RENDEZVOUS_TIMESTAMP INTEGER NOT NULL,
            $COL_EXPIRY_TIMESTAMP INTEGER NOT NULL,
            $COL_ARRIVAL_RADIUS_METERS REAL DEFAULT 50.0,
            $COL_NOTES TEXT,
            $COL_ICON_TYPE TEXT DEFAULT 'BUTTERFLY_STANDARD',
            $COL_IS_ACTIVE INTEGER DEFAULT 1,
            $COL_IS_ARCHIVED INTEGER DEFAULT 0,
            $COL_CREATED_AT TEXT NOT NULL,
            $COL_UPDATED_AT TEXT NOT NULL,
            FOREIGN KEY ($COL_CREATOR_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE SET NULL
        )
    """.trimIndent()

    val CREATE_INDEX_ACTIVE = "CREATE INDEX IF NOT EXISTS idx_butterfly_active ON $TABLE_NAME ($COL_IS_ACTIVE, $COL_IS_ARCHIVED)"

    fun save(
        db: SQLiteDatabase,
        pointId: String,
        creatorDeviceId: String,
        creatorIdentityId: Long?,
        pointName: String,
        address: String?,
        lat: Double,
        lng: Double,
        geohash: String?,
        rendezvousTimestamp: Long,
        expiryTimestamp: Long,
        arrivalRadiusMeters: Double = 50.0,
        notes: String? = null,
        iconType: String = "BUTTERFLY_STANDARD",
        isActive: Boolean = true,
        isArchived: Boolean = false
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()
        val values = ContentValues().apply {
            put(COL_POINT_ID, pointId)
            put(COL_CREATOR_DEVICE_ID, creatorDeviceId)
            if (creatorIdentityId != null) put(COL_CREATOR_IDENTITY_ID, creatorIdentityId)
            put(COL_POINT_NAME, pointName)
            put(COL_ADDRESS, address)
            put(COL_LAT, lat)
            put(COL_LNG, lng)
            put(COL_GEOHASH, geohash)
            put(COL_RENDEZVOUS_TIMESTAMP, rendezvousTimestamp)
            put(COL_EXPIRY_TIMESTAMP, expiryTimestamp)
            put(COL_ARRIVAL_RADIUS_METERS, arrivalRadiusMeters)
            put(COL_NOTES, notes)
            put(COL_ICON_TYPE, iconType)
            put(COL_IS_ACTIVE, if (isActive) 1 else 0)
            put(COL_IS_ARCHIVED, if (isArchived) 1 else 0)
            put(COL_UPDATED_AT, now)
        }

        val rows = db.update(TABLE_NAME, values, "$COL_POINT_ID = ?", arrayOf(pointId))
        return if (rows == 0) {
            values.put(COL_CREATED_AT, now)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            rows.toLong()
        }
    }

    fun archive(db: SQLiteDatabase, pointId: String) {
        val now = FirebaseComm.getForensicTimestamp()
        val values = ContentValues().apply {
            put(COL_IS_ACTIVE, 0)
            put(COL_IS_ARCHIVED, 1)
            put(COL_UPDATED_AT, now)
        }
        db.update(TABLE_NAME, values, "$COL_POINT_ID = ?", arrayOf(pointId))
    }
}
