package com.jdroidx.jfindme.data.local.tables

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.jdroidx.jfindme.data.remote.FirebaseComm

/**
 * [Variables] | [Constants] | [Dependencies] | [Integration Points]
 * Variables: id, identityId, keyAlias, publicKey, algorithm, keyUsage, fingerprintSha256, isHardwareBacked, createdAt, expiresAt, updatedAt
 * Constants: TABLE_NAME = "security_master_keys", COL_ID, COL_IDENTITY_ID, COL_KEY_ALIAS, COL_PUBLIC_KEY, COL_ALGORITHM, COL_KEY_USAGE, COL_FINGERPRINT_SHA256, COL_IS_HARDWARE_BACKED, COL_CREATED_AT, COL_EXPIRES_AT, COL_UPDATED_AT
 * Dependencies: android.content.ContentValues, android.database.sqlite.SQLiteDatabase, com.jdroidx.jfindme.data.remote.FirebaseComm
 * Integration Points: DatabaseHelper.kt, AuthViewModel.kt, SecurityEngine.kt, NetworkGatewayActivity.java
 */
object SecurityMasterKeysTable {
    const val TABLE_NAME = "security_master_keys"

    const val COL_ID = "id"
    const val COL_IDENTITY_ID = "identity_id" // FK to UserIdentityTable.id
    const val COL_KEY_ALIAS = "key_alias"
    const val COL_PUBLIC_KEY = "public_key"
    const val COL_ALGORITHM = "algorithm" // EC_SECP256R1, RSA_4096, ED25519
    const val COL_KEY_USAGE = "key_usage" // PAIRING_HANDSHAKE, TELEMETRY_SIGNING, SESSION_AUTH
    const val COL_FINGERPRINT_SHA256 = "fingerprint_sha256"
    const val COL_IS_HARDWARE_BACKED = "is_hardware_backed" // 1 = Android KeyStore StrongBox/TEE, 0 = Software fallback
    const val COL_CREATED_AT = "created_at"
    const val COL_EXPIRES_AT = "expires_at"
    const val COL_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS $TABLE_NAME (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_IDENTITY_ID INTEGER NOT NULL,
            $COL_KEY_ALIAS TEXT UNIQUE NOT NULL,
            $COL_PUBLIC_KEY TEXT NOT NULL,
            $COL_ALGORITHM TEXT DEFAULT 'EC_SECP256R1',
            $COL_KEY_USAGE TEXT DEFAULT 'PAIRING_HANDSHAKE',
            $COL_FINGERPRINT_SHA256 TEXT NOT NULL,
            $COL_IS_HARDWARE_BACKED INTEGER DEFAULT 1,
            $COL_CREATED_AT INTEGER NOT NULL,
            $COL_EXPIRES_AT INTEGER DEFAULT 0,
            $COL_UPDATED_AT TEXT,
            FOREIGN KEY ($COL_IDENTITY_ID) REFERENCES ${UserIdentityTable.TABLE_NAME}(${UserIdentityTable.COL_ID}) ON DELETE CASCADE
        )
    """.trimIndent()

    @JvmOverloads
    fun saveKey(
        db: SQLiteDatabase,
        identityId: Long,
        alias: String,
        publicKey: String,
        algorithm: String = "EC_SECP256R1",
        usage: String = "PAIRING_HANDSHAKE",
        fingerprint: String,
        isHardwareBacked: Boolean = true,
        expiresAt: Long = 0L
    ): Long {
        val now = FirebaseComm.getForensicTimestamp()
        val cv = ContentValues().apply {
            put(COL_IDENTITY_ID, identityId)
            put(COL_KEY_ALIAS, alias)
            put(COL_PUBLIC_KEY, publicKey)
            put(COL_ALGORITHM, algorithm)
            put(COL_KEY_USAGE, usage)
            put(COL_FINGERPRINT_SHA256, fingerprint)
            put(COL_IS_HARDWARE_BACKED, if (isHardwareBacked) 1 else 0)
            put(COL_CREATED_AT, System.currentTimeMillis())
            put(COL_EXPIRES_AT, expiresAt)
            put(COL_UPDATED_AT, now)
        }
        return db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getKey(db: SQLiteDatabase, alias: String): String? {
        return try {
            db.query(
                TABLE_NAME,
                arrayOf(COL_PUBLIC_KEY),
                "$COL_KEY_ALIAS = ?",
                arrayOf(alias),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

