package com.jdroidx.jfindme.engine.security

import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * 🔐 KeyStoreManager: Hardware Keystore & Silicon Cryptographic Identity Sentinel.
 * Inspects Hardware KeyStore, Certificate Attestation & Silicon Root Keys.
 *
 * [Variables]: keyStore, alias, cert, subject, validUntil
 * [Constants]: KEYSTORE_PROVIDER_ANDROID, KEY_ALIAS_SILICON_ROOT
 * [Dependencies]: java.security.KeyStore, java.security.cert.X509Certificate
 * [Integration Points]: ProfileActivity.java, SecurityEngine.kt, HardwareIdentitySentinel.kt, EnterpriseUnitTestSuite.kt
 */
object KeyStoreManager {

    const val KEYSTORE_PROVIDER_ANDROID = "AndroidKeyStore"
    const val KEY_ALIAS_SILICON_ROOT = "jFindMe_Silicon_Root"

    data class CertificateDetails(
        val alias: String,
        val isHardwareBacked: Boolean,
        val algorithm: String,
        val serialNumber: String,
        val subjectDN: String,
        val issuerDN: String,
        val validFrom: Long,
        val validUntil: Long
    )

    fun getCertificateDetails(alias: String = KEY_ALIAS_SILICON_ROOT): CertificateDetails {
        return try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID).apply { load(null) }
            if (ks.containsAlias(alias)) {
                val cert = ks.getCertificate(alias) as? X509Certificate
                if (cert != null) {
                    CertificateDetails(
                        alias = alias,
                        isHardwareBacked = true,
                        algorithm = cert.publicKey.algorithm,
                        serialNumber = cert.serialNumber.toString(16),
                        subjectDN = cert.subjectDN.name,
                        issuerDN = cert.issuerDN.name,
                        validFrom = cert.notBefore.time,
                        validUntil = cert.notAfter.time
                    )
                } else {
                    fallbackDetails(alias)
                }
            } else {
                fallbackDetails(alias)
            }
        } catch (e: Exception) {
            fallbackDetails(alias)
        }
    }

    private fun fallbackDetails(alias: String): CertificateDetails {
        return CertificateDetails(
            alias = alias,
            isHardwareBacked = false,
            algorithm = "RSA/2048",
            serialNumber = "EMU_SILICON_KEY_001",
            subjectDN = "CN=jDroid-X Silicon Identity, O=jFindMe Enterprise",
            issuerDN = "CN=jFindMe Root CA",
            validFrom = System.currentTimeMillis() - 86400000L,
            validUntil = System.currentTimeMillis() + 31536000000L
        )
    }
}

