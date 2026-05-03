/**
 * Auto-Login Architecture: Forensic Profile Ledger
 * Waterfall Method: Secured Metadata Storage & Analytical Parity
 */
package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * Tier 1: Identity Core
 * 👤 user_profile: Detailed user metadata
 */
object UserProfileTable {
    const val TABLE_NAME = "user_profile"

    const val COL_ID = "id"
    const val COL_IDENTITY_ID = "identity_id" // 🧬 V65.0: Secure Internal Link (FK)
    const val COL_NAME = "name"
    const val COL_EMAIL = "email"
    const val COL_PHONE = "phone"
    const val COL_DOB = "dob"
    const val COL_GENDER = "gender"
    const val COL_COUNTRY = "country"
    const val COL_STATE = "state"
    const val COL_CITY = "city"
    const val COL_PINCODE = "pincode"
    const val COL_ADDRESS = "address"
    const val COL_ORG_NAME = "org_name"
    const val COL_DEPT_NAME = "dept_name"
    const val COL_AVATAR_ID = "avatar_id"
    const val COL_NOTE = "note"
    const val COL_CREATED_ON = "created_on" // 🧬 Forensic String: YYYYMMDDHHMMSSSS
    const val COL_UPDATED_AT = "updated_at" // 🧬 Forensic String: YYYYMMDDHHMMSSSS

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS ${TABLE_NAME} (
            ${COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
            ${COL_IDENTITY_ID} INTEGER UNIQUE NOT NULL,
            ${COL_NAME} TEXT,
            ${COL_EMAIL} TEXT,
            ${COL_PHONE} TEXT,
            ${COL_DOB} TEXT,
            ${COL_GENDER} TEXT,
            ${COL_COUNTRY} TEXT,
            ${COL_STATE} TEXT,
            ${COL_CITY} TEXT,
            ${COL_PINCODE} TEXT,
            ${COL_ADDRESS} TEXT,
            ${COL_ORG_NAME} TEXT,
            ${COL_DEPT_NAME} TEXT,
            ${COL_AVATAR_ID} TEXT,
            ${COL_NOTE} TEXT,
            ${COL_CREATED_ON} TEXT,
            ${COL_UPDATED_AT} TEXT,
            FOREIGN KEY (${COL_IDENTITY_ID}) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    val CREATE_TABLE_SYS_LOG = """
        CREATE TABLE IF NOT EXISTS sys_log (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            level TEXT,
            tag TEXT,
            message TEXT,
            timestamp INTEGER,
            created_on TEXT
        )
    """.trimIndent()

    fun upsert(db: SQLiteDatabase, identityId: Long, data: Map<String, String>) {
        val now = FirebaseComm.getForensicTimestamp()
        val v = ContentValues()
        
        // 🧬 Forensic Delta Mapping: Only update fields present in the data map
        if (data.containsKey("userName") || data.containsKey("name") || data.containsKey("fullName")) 
            v.put(COL_NAME, data["userName"] ?: data["name"] ?: data["fullName"])
            
        if (data.containsKey("userEmail") || data.containsKey("email")) 
            v.put(COL_EMAIL, data["userEmail"] ?: data["email"])
            
        if (data.containsKey("userPhone") || data.containsKey("phone")) 
            v.put(COL_PHONE, data["userPhone"] ?: data["phone"])
            
        if (data.containsKey("userDob") || data.containsKey("dob")) 
            v.put(COL_DOB, data["userDob"] ?: data["dob"])
            
        if (data.containsKey("userGender") || data.containsKey("gender")) 
            v.put(COL_GENDER, data["userGender"] ?: data["gender"])
            
        if (data.containsKey("userCountry") || data.containsKey("country")) 
            v.put(COL_COUNTRY, data["userCountry"] ?: data["country"])
            
        if (data.containsKey("userState") || data.containsKey("state")) 
            v.put(COL_STATE, data["userState"] ?: data["state"])
            
        if (data.containsKey("userCity") || data.containsKey("city")) 
            v.put(COL_CITY, data["userCity"] ?: data["city"])
            
        if (data.containsKey("userPincode") || data.containsKey("pincode")) 
            v.put(COL_PINCODE, data["userPincode"] ?: data["pincode"])
            
        if (data.containsKey("userAddress") || data.containsKey("address")) 
            v.put(COL_ADDRESS, data["userAddress"] ?: data["address"])
            
        if (data.containsKey("orgName") || data.containsKey("org_name")) 
            v.put(COL_ORG_NAME, data["orgName"] ?: data["org_name"])
            
        if (data.containsKey("deptName") || data.containsKey("department")) 
            v.put(COL_DEPT_NAME, data["deptName"] ?: data["department"])
            
        if (data.containsKey("avatar_id")) 
            v.put(COL_AVATAR_ID, data["avatar_id"])
            
        if (data.containsKey("emergencyNote") || data.containsKey("note")) 
            v.put(COL_NOTE, data["emergencyNote"] ?: data["note"])
            
        if (v.size() > 0) {
            v.put(COL_UPDATED_AT, now)
            val rows = db.update(TABLE_NAME, v, "$COL_IDENTITY_ID = ?", arrayOf(identityId.toString()))
            if (rows == 0) {
                v.put(COL_IDENTITY_ID, identityId)
                v.put(COL_CREATED_ON, now)
                db.insert(TABLE_NAME, null, v)
            }
        }
    }

    fun updateNote(db: SQLiteDatabase, identityId: Long, note: String) {
        val v = ContentValues().apply {
            put(COL_NOTE, note)
            put(COL_UPDATED_AT, FirebaseComm.getForensicTimestamp())
        }
        db.update(TABLE_NAME, v, "$COL_IDENTITY_ID = ?", arrayOf(identityId.toString()))
    }
}
