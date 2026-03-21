package com.jdroidx.jfindme.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * SecureVault: Provides AES-256 encrypted storage for sensitive data.
 * Level 5: Security Engine Implementation.
 */
object SecureVault {
    private const val VAULT_NAME = "jFindMe_SecureVault"

    fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        return EncryptedSharedPreferences.create(
            VAULT_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Stores sensitive pairing tokens or session keys.
     */
    fun saveSecureToken(context: Context, key: String, value: String) {
        getEncryptedPrefs(context).edit().putString(key, value).apply()
    }

    fun getSecureToken(context: Context, key: String): String? {
        return getEncryptedPrefs(context).getString(key, null)
    }
}
