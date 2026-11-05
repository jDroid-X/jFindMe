package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: deviceId, identityId, model, brand, board, hardware, deviceName, imei, imei_1, imei_2, is_dual_sim, is_rooted, build_fingerprint, latitude, longitude, address, battery_optimization_exempt, os_version, app_version
 * Constants: TABLE_NAME = "device", COL_DEVICE_ID, COL_IDENTITY_ID, COL_DEVICE_MODEL, COL_BRAND, COL_BOARD, COL_HARDWARE, COL_DEVICE_NAME, COL_IMEI, COL_IMEI_1, COL_IMEI_2, COL_IS_DUAL_SIM, COL_IS_ROOTED, COL_BUILD_FINGERPRINT, COL_LATITUDE, COL_LONGITUDE, COL_ADDRESS, COL_BATTERY_OPTIMIZATION_EXEMPT, COL_OS_VERSION, COL_APP_VERSION, COL_CREATED_ON, COL_UPDATED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, TrackingService.kt, NetworkGatewayActivity.java
 */
object DeviceTable {
    const val TABLE_NAME = "device"

    const val COL_DEVICE_ID = "device_id" // PK
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V65.0: Secure Internal Link (FK)
    const val COL_DEVICE_MODEL = "device_model"
    const val COL_BRAND = "brand"
    const val COL_BOARD = "board"
    const val COL_HARDWARE = "hardware"
    const val COL_DEVICE_NAME = "device_name"
    const val COL_IMEI = "imei"
    const val COL_IMEI_1 = "imei_1"
    const val COL_IMEI_2 = "imei_2"
    const val COL_IS_DUAL_SIM = "is_dual_sim"
    const val COL_IS_ROOTED = "is_rooted"
    const val COL_BUILD_FINGERPRINT = "build_fingerprint"
    const val COL_LATITUDE = "latitude"
    const val COL_LONGITUDE = "longitude"
    const val COL_ADDRESS = "address"

    // 🛡️ V79 REA Refabrication Columns: Doze Exemption & Platform Version Trace
    const val COL_BATTERY_OPTIMIZATION_EXEMPT = "battery_optimization_exempt"
    const val COL_OS_VERSION = "os_version"
    const val COL_APP_VERSION = "app_version"

    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_DEVICE_ID TEXT PRIMARY KEY,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_DEVICE_MODEL TEXT,
            $COL_BRAND TEXT,
            $COL_BOARD TEXT,
            $COL_HARDWARE TEXT,
            $COL_DEVICE_NAME TEXT,
            $COL_IMEI TEXT,
            $COL_IMEI_1 TEXT,
            $COL_IMEI_2 TEXT,
            $COL_IS_DUAL_SIM INTEGER DEFAULT 0,
            $COL_IS_ROOTED INTEGER DEFAULT 0,
            $COL_BUILD_FINGERPRINT TEXT,
            $COL_LATITUDE REAL,
            $COL_LONGITUDE REAL,
            $COL_ADDRESS TEXT,
            $COL_BATTERY_OPTIMIZATION_EXEMPT INTEGER DEFAULT 0,
            $COL_OS_VERSION TEXT,
            $COL_APP_VERSION TEXT,
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    @JvmOverloads
    fun upsert(
        db: SQLiteDatabase,
        deviceId: String,
        identityId: Long,
        model: String? = null,
        imei: String? = null,
        rooted: Boolean? = null,
        fingerprint: String? = null,
        lat: Double = 0.0,
        lng: Double = 0.0,
        addr: String? = null,
        brand: String? = null,
        board: String? = null,
        hardware: String? = null,
        deviceName: String? = null,
        imei1: String? = null,
        imei2: String? = null,
        isDualSim: Boolean? = null,
        batteryOptExempt: Boolean? = null,
        osVersion: String? = null,
        appVersion: String? = null
    ) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues()
        
        // 🧬 Forensic Delta Mapping: Identity and Hardware Specs
        v.put(COL_IDENTITY_ID, identityId)
        if (model != null) v.put(COL_DEVICE_MODEL, model)
        if (brand != null) v.put(COL_BRAND, brand)
        if (board != null) v.put(COL_BOARD, board)
        if (hardware != null) v.put(COL_HARDWARE, hardware)
        if (deviceName != null) v.put(COL_DEVICE_NAME, deviceName)

        val resolvedImei1 = imei1 ?: imei
        if (resolvedImei1 != null) {
            v.put(COL_IMEI, resolvedImei1)
            v.put(COL_IMEI_1, resolvedImei1)
        }
        if (imei2 != null) v.put(COL_IMEI_2, imei2)
        if (isDualSim != null) v.put(COL_IS_DUAL_SIM, if (isDualSim) 1 else 0)

        if (rooted != null) v.put(COL_IS_ROOTED, if (rooted) 1 else 0)
        if (fingerprint != null) v.put(COL_BUILD_FINGERPRINT, fingerprint)

        if (batteryOptExempt != null) v.put(COL_BATTERY_OPTIMIZATION_EXEMPT, if (batteryOptExempt) 1 else 0)
        if (osVersion != null) v.put(COL_OS_VERSION, osVersion)
        if (appVersion != null) v.put(COL_APP_VERSION, appVersion)

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
