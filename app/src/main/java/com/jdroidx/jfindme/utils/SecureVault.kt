package com.jdroidx.jfindme.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SecureVault: Provides AES-256 encrypted storage for sensitive data.
 * Level 5: Security Engine Implementation.
 */
object SecureVault {
    private const val VAULT_NAME = "jFindMe_SecureVault"

    fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            VAULT_NAME,
            masterKey,
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
