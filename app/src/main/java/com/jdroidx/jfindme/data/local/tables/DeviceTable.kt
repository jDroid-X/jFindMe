package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * Tier 2: Device & Trust Layer
 * 📱 device: Master Hardware Hub
 */
object DeviceTable {
    const val TABLE_NAME = "device"

    const val COL_DEVICE_ID = "device_id" // PK
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V65.0: Secure Internal Link (FK)
    const val COL_DEVICE_MODEL = "device_model"
    const val COL_IMEI = "imei"
    const val COL_IS_ROOTED = "is_rooted"
    const val COL_BUILD_FINGERPRINT = "build_fingerprint"
    const val COL_LATITUDE = "latitude"
    const val COL_LONGITUDE = "longitude"
    const val COL_ADDRESS = "address"
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_DEVICE_ID TEXT PRIMARY KEY,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_DEVICE_MODEL TEXT,
            $COL_IMEI TEXT,
            $COL_IS_ROOTED INTEGER DEFAULT 0,
            $COL_BUILD_FINGERPRINT TEXT,
            $COL_LATITUDE REAL,
            $COL_LONGITUDE REAL,
            $COL_ADDRESS TEXT,
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    fun upsert(db: SQLiteDatabase, deviceId: String, identityId: Long, model: String? = null, imei: String? = null, rooted: Boolean? = null, fingerprint: String? = null, lat: Double = 0.0, lng: Double = 0.0, addr: String? = null) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues()
        
        // 🧬 Forensic Delta Mapping: Identity and Hardware Specs
        v.put(COL_IDENTITY_ID, identityId)
        if (model != null) v.put(COL_DEVICE_MODEL, model)
        if (imei != null) v.put(COL_IMEI, imei)
        if (rooted != null) v.put(COL_IS_ROOTED, if (rooted) 1 else 0)
        if (fingerprint != null) v.put(COL_BUILD_FINGERPRINT, fingerprint)
        v.put(COL_UPDATED_AT, now)

        // 🧬 Registration Baseline Lock: Only update location if it's currently empty (0.0)
        var existingLat = 0.0
        try {
            db.query(TABLE_NAME, arrayOf(COL_LATITUDE), "$COL_DEVICE_ID = ?", arrayOf(deviceId), null, null, null).use {
                if (it.moveToFirst()) existingLat = it.getDouble(0)
            }
        } catch (e: Exception) { }

        if (existingLat == 0.0) {
            if (lat != 0.0) v.put(COL_LATITUDE, lat)
            if (lng != 0.0) v.put(COL_LONGITUDE, lng)
            if (addr != null) v.put(COL_ADDRESS, addr)
        }
        
        val rows = db.update(TABLE_NAME, v, "$COL_DEVICE_ID = ?", arrayOf(deviceId))
        if (rows == 0) {
            // Force location on initial insertion if not provided yet
            if (v.getAsString(COL_LATITUDE) == null && lat != 0.0) {
                v.put(COL_LATITUDE, lat)
                v.put(COL_LONGITUDE, lng)
                v.put(COL_ADDRESS, addr)
            }
            v.put(COL_DEVICE_ID, deviceId)
            v.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, v)
        }
    }
}
