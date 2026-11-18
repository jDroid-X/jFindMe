package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] shareId: String, masterDeviceId: String, submasterDeviceId: String, targetDeviceId: String, relationshipType: String, permissionsMask: Int, currentStatus: String, isActive: Boolean, retryCount: Int, validUntil: Long, createdOn: String, updatedAt: String
 * [Constants] TABLE_NAME = "device_sharing", COL_*
 * [Dependencies] android.database.sqlite.SQLiteDatabase, android.content.ContentValues, FirebaseComm
 * [Integration Points] DatabaseHelper.kt, PairingRepository.kt, TrackingService.kt, MapActivity.java, PersonDetailsViewModel.kt
 *
 * 🛡️ Tier 2: Sub-Master State Table for Triadic Device Sharing (Master ➔ Submaster ➔ Target)
 * Maintains the current standing authorization contract between the Master Device, Delegated Submaster, and the Target Ward/Asset.
 */
object DeviceSharingTable {
    const val TABLE_NAME = "device_sharing"

    const val COL_SHARE_ID = "share_id"                         // PK: UUID / "DSH_xxxx"
    const val COL_MASTER_DEVICE_ID = "master_device_id"         // FK -> device.device_id (Primary Master / JEET)
    const val COL_SUBMASTER_DEVICE_ID = "submaster_device_id"   // FK -> device.device_id (Co-Guardian / Supervisor)
    const val COL_TARGET_DEVICE_ID = "target_device_id"         // FK -> device.device_id (Target Ward / e.g. Riddhi)
    const val COL_RELATIONSHIP_TYPE = "relationship_type"       // CO_GUARDIAN, CAREGIVER, FLEET_SUPERVISOR, GUEST
    const val COL_PERMISSIONS_MASK = "permissions_mask"         // Bitmask: 0x01=GPS, 0x02=History, 0x04=Siren, 0x08=Geofence
    const val COL_CURRENT_STATUS = "current_status"             // REQUESTED, APPROVED, ACCEPTED, ACTIVE, DECLINED, REJECTED, TIMED_OUT, EXPIRED, REVOKED
    const val COL_IS_ACTIVE = "is_active"                       // 1 = Live/Active, 0 = Inactive
    const val COL_RETRY_COUNT = "retry_count"                   // Multi-time request counter
    const val COL_VALID_UNTIL = "valid_until"                   // 0 = Permanent, >0 = Expiry Epoch Ms
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_SHARE_ID TEXT PRIMARY KEY,
            $COL_MASTER_DEVICE_ID TEXT NOT NULL,
            $COL_SUBMASTER_DEVICE_ID TEXT NOT NULL,
            $COL_TARGET_DEVICE_ID TEXT NOT NULL,
            $COL_RELATIONSHIP_TYPE TEXT DEFAULT 'CO_GUARDIAN',
            $COL_PERMISSIONS_MASK INTEGER DEFAULT 255,
            $COL_CURRENT_STATUS TEXT DEFAULT 'REQUESTED',
            $COL_IS_ACTIVE INTEGER DEFAULT 1,
            $COL_RETRY_COUNT INTEGER DEFAULT 0,
            $COL_VALID_UNTIL INTEGER DEFAULT 0,
            $COL_CREATED_ON TEXT NOT NULL,
            $COL_UPDATED_AT TEXT NOT NULL,
            FOREIGN KEY ($COL_MASTER_DEVICE_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE,
            FOREIGN KEY ($COL_SUBMASTER_DEVICE_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE,
            FOREIGN KEY ($COL_TARGET_DEVICE_ID) REFERENCES ${DeviceTable.TABLE_NAME}(${DeviceTable.COL_DEVICE_ID}) ON DELETE CASCADE,
            UNIQUE($COL_MASTER_DEVICE_ID, $COL_SUBMASTER_DEVICE_ID, $COL_TARGET_DEVICE_ID)
        )
    """.trimIndent()

    val CREATE_INDEX_LOOKUP = """
        CREATE INDEX IF NOT EXISTS idx_device_sharing_lookup 
        ON $TABLE_NAME ($COL_MASTER_DEVICE_ID, $COL_SUBMASTER_DEVICE_ID, $COL_TARGET_DEVICE_ID)
    """.trimIndent()

    val CREATE_INDEX_STATUS = """
        CREATE INDEX IF NOT EXISTS idx_device_sharing_status 
        ON $TABLE_NAME ($COL_CURRENT_STATUS, $COL_IS_ACTIVE)
    """.trimIndent()

    // [Variables] db: SQLiteDatabase, shareId: String, masterDev: String, submasterDev: String, targetDev: String, relType: String, mask: Int, status: String, isActive: Boolean, retryCount: Int, validUntil: Long
    // [Constants] TABLE_NAME, COL_*
    // [Dependencies] SQLiteDatabase, ContentValues, FirebaseComm
    // [Integration Points] DatabaseHelper.kt, PairingRepository.kt
    fun upsert(
        db: SQLiteDatabase,
        shareId: String,
        masterDev: String,
        submasterDev: String,
        targetDev: String,
        relType: String = "CO_GUARDIAN",
        mask: Int = 0xFF,
        status: String = "REQUESTED",
        isActive: Boolean = true,
        retryCount: Int = 0,
        validUntil: Long = 0L
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()
        val cv = ContentValues().apply {
            put(COL_SHARE_ID, shareId)
            put(COL_MASTER_DEVICE_ID, masterDev)
            put(COL_SUBMASTER_DEVICE_ID, submasterDev)
            put(COL_TARGET_DEVICE_ID, targetDev)
            put(COL_RELATIONSHIP_TYPE, relType)
            put(COL_PERMISSIONS_MASK, mask)
            put(COL_CURRENT_STATUS, status)
            put(COL_IS_ACTIVE, if (isActive) 1 else 0)
            put(COL_RETRY_COUNT, retryCount)
            put(COL_VALID_UNTIL, validUntil)
            put(COL_UPDATED_AT, now)
        }

        val existingId = db.query(
            TABLE_NAME,
            arrayOf(COL_SHARE_ID),
            "$COL_MASTER_DEVICE_ID = ? AND $COL_SUBMASTER_DEVICE_ID = ? AND $COL_TARGET_DEVICE_ID = ?",
            arrayOf(masterDev, submasterDev, targetDev),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

        return if (existingId != null) {
            db.update(TABLE_NAME, cv, "$COL_SHARE_ID = ?", arrayOf(existingId)).toLong()
        } else {
            cv.put(COL_CREATED_ON, now)
            db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }
}
