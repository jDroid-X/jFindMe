package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, latitude, longitude, address, latRounded, lngRounded, placeId, locality, sublocality, pincode, createdAt
 * Constants: TABLE_NAME = "reverse_geocode_cache", COL_ID, COL_LATITUDE, COL_LONGITUDE, COL_ADDRESS, COL_LAT_ROUNDED, COL_LNG_ROUNDED, COL_PLACE_ID, COL_LOCALITY, COL_SUBLOCALITY, COL_PINCODE, COL_CREATED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase
 * Integration Points: DatabaseHelper.kt, LocationRepository.kt, MapViewModel.kt
 */
object ReverseGeocodeCacheTable {
    const val TABLE_NAME = "reverse_geocode_cache"

    const val COL_ID = "id"
    const val COL_LATITUDE = "latitude"
    const val COL_LONGITUDE = "longitude"
    const val COL_ADDRESS = "address"
    const val COL_LAT_ROUNDED = "lat_rounded" // 4-decimal rounded (~11m grid cell)
    const val COL_LNG_ROUNDED = "lng_rounded"
    const val COL_PLACE_ID = "place_id"
    const val COL_LOCALITY = "locality"
    const val COL_SUBLOCALITY = "sublocality"
    const val COL_PINCODE = "pincode"
    const val COL_CREATED_AT = "created_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_LATITUDE REAL NOT NULL,
            $COL_LONGITUDE REAL NOT NULL,
            $COL_ADDRESS TEXT NOT NULL,
            $COL_LAT_ROUNDED REAL DEFAULT 0.0,
            $COL_LNG_ROUNDED REAL DEFAULT 0.0,
            $COL_PLACE_ID TEXT DEFAULT '',
            $COL_LOCALITY TEXT DEFAULT '',
            $COL_SUBLOCALITY TEXT DEFAULT '',
            $COL_PINCODE TEXT DEFAULT '',
            $COL_CREATED_AT INTEGER NOT NULL
        )
    """.trimIndent()

    fun putAddress(
        db: SQLiteDatabase,
        lat: Double,
        lng: Double,
        address: String,
        locality: String = "",
        sublocality: String = "",
        pincode: String = ""
    ): Long {
        val latRound = Math.round(lat * 10000.0) / 10000.0
        val lngRound = Math.round(lng * 10000.0) / 10000.0
        val cv = ContentValues().apply {
            put(COL_LATITUDE, lat)
            put(COL_LONGITUDE, lng)
            put(COL_ADDRESS, address)
            put(COL_LAT_ROUNDED, latRound)
            put(COL_LNG_ROUNDED, lngRound)
            put(COL_LOCALITY, locality)
            put(COL_SUBLOCALITY, sublocality)
            put(COL_PINCODE, pincode)
            put(COL_CREATED_AT, System.currentTimeMillis())
        }
        return db.insert(TABLE_NAME, null, cv)
    }

    fun getCachedAddress(db: SQLiteDatabase, lat: Double, lng: Double): String? {
        val latRound = Math.round(lat * 10000.0) / 10000.0
        val lngRound = Math.round(lng * 10000.0) / 10000.0
        return try {
            db.query(
                TABLE_NAME,
                arrayOf(COL_ADDRESS),
                "$COL_LAT_ROUNDED = ? AND $COL_LNG_ROUNDED = ?",
                arrayOf(latRound.toString(), lngRound.toString()),
                null,
                null,
                "$COL_ID DESC",
                "1"
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

