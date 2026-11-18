package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] id, pointId, deviceId, identityId, status, rejectionReason, distanceToPointMeters, etaSeconds, lastTelemetryTs, arrivalTimestamp, createdAt, updatedAt, now
 * [Constants] TABLE_NAME = "butterfly_point_device_map", COL_ID, COL_POINT_ID, COL_DEVICE_ID, COL_IDENTITY_ID, COL_STATUS, COL_REJECTION_REASON, COL_DISTANCE_METERS, COL_ETA_SECONDS, COL_LAST_TELEMETRY_TS, COL_ARRIVAL_TIMESTAMP, COL_CREATED_AT, COL_UPDATED_AT, STATUS_*
 * [Dependencies] android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm, com.jdroidx.jfindme.data.local.tables.ButterflyPointTable, com.jdroidx.jfindme.data.local.tables.DeviceTable
 * [Integration Points] DatabaseHelper.kt, MapActivity.java, MapViewModel.kt, ButterflyPointEngine.kt, Firebase RTDB /butterfly_points/{pointId}/devices/{deviceId}
 *
 * Tier 2: Sub-Master / Fact Database Table for Connected Devices Rendezvous Participation State
 */
object ButterflyPointDeviceMapTable {
    const val TABLE_NAME = "butterfly_point_device_map"

    const val COL_ID = "id"
    const val COL_POINT_ID = "point_id"
    const val COL_DEVICE_ID = "device_id"
    const val COL_IDENTITY_ID = "identity_id"
    const val COL_STATUS = "status"
    const val COL_REJECTION_REASON = "rejection_reason"
    const val COL_DISTANCE_METERS = "distance_to_point_meters"
    const val COL_ETA_SECONDS = "eta_seconds"
    const val COL_LAST_TELEMETRY_TS = "last_telemetry_ts"
    const val COL_ARRIVAL_TIMESTAMP = "arrival_timestamp"
    const val COL_BATTERY_PCT = "battery_pct"
    const val COL_IS_SOS = "is_sos"
    const val COL_RAG_STATUS = "rag_status"
    const val COL_RAG_REASON = "rag_reason"
    const val COL_LAST_LAT = "last_lat"
    const val COL_LAST_LNG = "last_lng"
    const val COL_INVITED_BY = "invited_by"
    const val COL_CREATED_AT = "created_at"
    const val COL_UPDATED_AT = "updated_at"

    // Tactical Status Identifiers
    const val STATUS_PENDING = "PENDING"
    const val STATUS_ACCEPTED = "ACCEPTED"
    const val STATUS_REJECTED = "REJECTED"
    const val STATUS_ON_MOVE = "ON_MOVE"
    const val STATUS_REACHED = "REACHED"
    const val STATUS_TIMED_OUT = "TIMED_OUT"
    const val STATUS_REVOKED = "REVOKED"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_POINT_ID TEXT NOT NULL,
            $COL_DEVICE_ID TEXT NOT NULL,
            $COL_IDENTITY_ID INTEGER,
            $COL_STATUS TEXT DEFAULT '$STATUS_PENDING',
            $COL_REJECTION_REASON TEXT,
            $COL_DISTANCE_METERS REAL DEFAULT 0.0,
            $COL_ETA_SECONDS INTEGER DEFAULT 0,
            $COL_LAST_TELEMETRY_TS INTEGER DEFAULT 0,
            $COL_ARRIVAL_TIMESTAMP INTEGER DEFAULT 0,
            $COL_BATTERY_PCT INTEGER DEFAULT -1,
            $COL_IS_SOS INTEGER DEFAULT 0,
            $COL_RAG_STATUS TEXT DEFAULT 'GREY',
            $COL_RAG_REASON TEXT,
            $COL_LAST_LAT REAL DEFAULT 0.0,
            $COL_LAST_LNG REAL DEFAULT 0.0,
            $COL_INVITED_BY TEXT,
            $COL_CREATED_AT TEXT NOT NULL,
            $COL_UPDATED_AT TEXT NOT NULL,
            FOREIGN KEY ($COL_POINT_ID) REFERENCES ${ButterflyPointTable.TABLE_NAME}(${ButterflyPointTable.COL_POINT_ID}) ON DELETE CASCADE,
            FOREIGN KEY ($COL_DEVICE_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE,
            UNIQUE($COL_POINT_ID, $COL_DEVICE_ID)
        )
    """.trimIndent()

    val CREATE_INDEX_STATUS = "CREATE INDEX IF NOT EXISTS idx_butterfly_dev_status ON $TABLE_NAME ($COL_POINT_ID, $COL_STATUS)"

    @JvmStatic
    fun getDeviceStatus(db: SQLiteDatabase, pointId: String, deviceId: String): String? {
        val query = "SELECT $COL_STATUS FROM $TABLE_NAME WHERE $COL_POINT_ID = ? AND $COL_DEVICE_ID = ? LIMIT 1"
        return db.rawQuery(query, arrayOf(pointId, deviceId)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    @JvmOverloads
    fun assignDevice(
        db: SQLiteDatabase,
        pointId: String,
        deviceId: String,
        identityId: Long? = null,
        initialStatus: String = STATUS_PENDING,
        invitedBy: String? = null
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()
        val existingStatus = getDeviceStatus(db, pointId, deviceId)

        // 🛡️ State Invariant: Preserve existing resolved status (e.g. ACCEPTED, REACHED, REJECTED)
        // when caller passes default STATUS_PENDING during reconnects or sync cycles
        val statusToSet = if (existingStatus != null && initialStatus == STATUS_PENDING) existingStatus else initialStatus

        val values = ContentValues().apply {
            put(COL_POINT_ID, pointId)
            put(COL_DEVICE_ID, deviceId)
            if (identityId != null) put(COL_IDENTITY_ID, identityId)
            put(COL_STATUS, statusToSet)
            if (invitedBy != null) put(COL_INVITED_BY, invitedBy)
            put(COL_UPDATED_AT, now)
        }

        val rows = db.update(TABLE_NAME, values, "$COL_POINT_ID = ? AND $COL_DEVICE_ID = ?", arrayOf(pointId, deviceId))
        return if (rows == 0) {
            values.put(COL_CREATED_AT, now)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            rows.toLong()
        }
    }

    fun updateStatus(
        db: SQLiteDatabase,
        pointId: String,
        deviceId: String,
        status: String,
        rejectionReason: String? = null
    ) {
        val now = FirebaseComm.getForensicTimestamp()
        val values = ContentValues().apply {
            put(COL_STATUS, status)
            if (rejectionReason != null) put(COL_REJECTION_REASON, rejectionReason)
            put(COL_UPDATED_AT, now)
        }
        db.update(TABLE_NAME, values, "$COL_POINT_ID = ? AND $COL_DEVICE_ID = ?", arrayOf(pointId, deviceId))
    }

    @JvmOverloads
    fun updateTelemetry(
        db: SQLiteDatabase,
        pointId: String,
        deviceId: String,
        distanceMeters: Double,
        etaSeconds: Int,
        telemetryTs: Long,
        isArrived: Boolean = false,
        batteryPct: Int = -1,
        isSos: Boolean = false,
        ragStatus: String = "GREEN",
        ragReason: String = "",
        lastLat: Double = 0.0,
        lastLng: Double = 0.0
    ) {
        val now = FirebaseComm.getForensicTimestamp()
        val values = ContentValues().apply {
            put(COL_DISTANCE_METERS, distanceMeters)
            put(COL_ETA_SECONDS, etaSeconds)
            put(COL_LAST_TELEMETRY_TS, telemetryTs)
            if (lastLat != 0.0) put(COL_LAST_LAT, lastLat)
            if (lastLng != 0.0) put(COL_LAST_LNG, lastLng)
            if (isArrived) {
                put(COL_STATUS, STATUS_REACHED)
                put(COL_ARRIVAL_TIMESTAMP, telemetryTs)
            } else {
                put(COL_STATUS, STATUS_ON_MOVE)
            }
            if (batteryPct != -1) put(COL_BATTERY_PCT, batteryPct)
            put(COL_IS_SOS, if (isSos) 1 else 0)
            put(COL_RAG_STATUS, ragStatus)
            if (ragReason.isNotEmpty()) put(COL_RAG_REASON, ragReason)
            put(COL_UPDATED_AT, now)
        }
        db.update(TABLE_NAME, values, "$COL_POINT_ID = ? AND $COL_DEVICE_ID = ?", arrayOf(pointId, deviceId))
    }
}
