package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * Tier 1: Identity Core
 * 🧩 user_org_map: Relation between Users and Organizations
 */
object UserOrgMapTable {
    const val TABLE_NAME = "user_org_map"

    const val COL_ID = "id"
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V65.0: Secure Internal Link (FK)
    const val COL_ORG_ID = "org_id" // FK
    const val COL_DEPARTMENT = "department"
    const val COL_ROLE_TYPE = "role_type"
    const val COL_EMPLOYEE_ID = "employee_id"
    const val COL_SUPERVISOR = "supervisor"
    const val COL_SHIFT = "shift_timing"
    const val COL_BADGE = "badge_number"
    const val COL_EXT = "emergency_ext"
    const val COL_CLEARANCE = "access_level"
    const val COL_CREATED_ON = "created_on"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_ORG_ID TEXT NOT NULL,
            $COL_DEPARTMENT TEXT,
            $COL_ROLE_TYPE TEXT,
            $COL_EMPLOYEE_ID TEXT,
            $COL_SUPERVISOR TEXT,
            $COL_SHIFT TEXT,
            $COL_BADGE TEXT,
            $COL_EXT TEXT,
            $COL_CLEARANCE TEXT,
            $COL_CREATED_ON TEXT,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE,
            FOREIGN KEY ($COL_ORG_ID) REFERENCES ${OrganizationTable.TABLE_NAME}(${OrganizationTable.COL_ORG_ID}) ON DELETE CASCADE,
            UNIQUE($COL_IDENTITY_ID, $COL_ORG_ID)
        )
    """.trimIndent()

    fun mapUserToOrg(db: SQLiteDatabase, identityId: Long, orgId: String, v: android.content.ContentValues) {
        val now = FirebaseComm.getForensicTimestamp()
        val professional = ContentValues().apply {
            put(COL_IDENTITY_ID, identityId)
            put(COL_ORG_ID, orgId)
            put(COL_DEPARTMENT, v.getAsString("department"))
            put(COL_ROLE_TYPE, v.getAsString("designation"))
            put(COL_EMPLOYEE_ID, v.getAsString("employee_id"))
            put(COL_SUPERVISOR, v.getAsString("supervisor"))
            put(COL_SHIFT, v.getAsString("shift_timing"))
            put(COL_BADGE, v.getAsString("badge_number"))
            put(COL_EXT, v.getAsString("emergency_ext"))
            put(COL_CLEARANCE, v.getAsString("access_level"))
            put(COL_UPDATED_AT, now)
        }
        val rows = db.update(TABLE_NAME, professional, "$COL_IDENTITY_ID = ? AND $COL_ORG_ID = ?", arrayOf(identityId.toString(), orgId))
        if (rows == 0) {
            professional.put(COL_CREATED_ON, now)
            db.insert(TABLE_NAME, null, professional)
        }
    }
}
