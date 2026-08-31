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
    private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    fun getEncryptedPrefs(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        synchronized(this) {
            cachedPrefs?.let { return it }
            val prefs = try {
                createEncryptedPrefs(context)
            } catch (e: Exception) {
                SystemLogger.e("SecureVault", "EncryptedSharedPreferences corrupted (${e.javaClass.simpleName}), executing full keystore purge & self-healing...", e)
                try {
                    purgeKeystoreAndKeysets(context)
                    createEncryptedPrefs(context)
                } catch (retryEx: Exception) {
                    SystemLogger.e("SecureVault", "Keystore recovery failed, engaging application sandbox storage", retryEx)
                    context.getSharedPreferences(VAULT_NAME + "_sandbox", Context.MODE_PRIVATE)
                }
            }
            cachedPrefs = prefs
            return prefs
        }
    }

    private fun purgeKeystoreAndKeysets(context: Context) {
        try {
            // 1. Delete KeyStore alias
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
            }
            if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            }
        } catch (e: Exception) {
            SystemLogger.e("SecureVault", "Failed purging KeyStore alias", e)
        }

        try {
            // 2. Delete all Tink keyset and EncryptedSharedPreferences XML files
            val sharedPrefsDir = java.io.File(context.filesDir.parent, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                val files = sharedPrefsDir.listFiles()
                files?.forEach { file ->
                    if (file.name.contains(VAULT_NAME) || 
                        file.name.contains("__androidx_security_crypto_") || 
                        file.name.contains("_master_key_")) {
                        file.delete()
                    }
                }
            }
            context.getSharedPreferences(VAULT_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        } catch (e: Exception) {
            SystemLogger.e("SecureVault", "Failed purging keyset XML files", e)
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
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
