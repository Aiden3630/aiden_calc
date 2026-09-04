package com.aiden.calculator

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.os.Build
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class BiometricVaultConfig(
    val vaultId: VaultId,
    val ciphertext: ByteArray,
    val nonce: ByteArray,
)

class BiometricVaultStore(context: Context) {
    private val preferences = context.getSharedPreferences("biometric_vault", Context.MODE_PRIVATE)

    fun configuredVaultId(): VaultId? = config()?.vaultId

    fun config(): BiometricVaultConfig? = runCatching {
        BiometricVaultConfig(
            vaultId = VaultId.valueOf(requireNotNull(preferences.getString(KEY_VAULT_ID, null))),
            ciphertext = decode(requireNotNull(preferences.getString(KEY_CIPHERTEXT, null))),
            nonce = decode(requireNotNull(preferences.getString(KEY_NONCE, null))),
        )
    }.getOrElse {
        if (preferences.contains(KEY_VAULT_ID) || preferences.contains(KEY_CIPHERTEXT) || preferences.contains(KEY_NONCE)) clear()
        null
    }

    fun prepareEnrollmentCipher(): Cipher = cipher(Cipher.ENCRYPT_MODE).also {
        it.init(Cipher.ENCRYPT_MODE, existingOrCreateKey())
    }

    fun save(vaultId: VaultId, cipher: Cipher, masterKey: ByteArray) {
        val ciphertext = cipher.doFinal(masterKey)
        preferences.edit()
            .putString(KEY_VAULT_ID, vaultId.name)
            .putString(KEY_CIPHERTEXT, encode(ciphertext))
            .putString(KEY_NONCE, encode(cipher.iv))
            .commit()
        ciphertext.fill(0)
    }

    fun prepareUnlockCipher(): Pair<BiometricVaultConfig, Cipher>? {
        val config = config() ?: return null
        return config to cipher(Cipher.DECRYPT_MODE).also {
            it.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, config.nonce))
        }
    }

    fun unwrap(config: BiometricVaultConfig, cipher: Cipher): ByteArray = cipher.doFinal(config.ciphertext)

    fun clear() {
        preferences.edit().clear().commit()
        runCatching {
            KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(ALIAS)
        }
    }

    private fun key(): SecretKey = requireNotNull(
        KeyStore.getInstance(KEYSTORE).apply { load(null) }.getKey(ALIAS, null) as? SecretKey,
    )

    private fun createKey(): SecretKey {
        KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(ALIAS)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            val builder = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= 30) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }
            init(builder.build())
            generateKey()
        }
    }

    private fun existingOrCreateKey(): SecretKey = runCatching { key() }.getOrElse { createKey() }

    private fun cipher(mode: Int) = Cipher.getInstance("AES/GCM/NoPadding")
    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "biometric-vault-master-v1"
        const val KEY_VAULT_ID = "vaultId"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val KEY_NONCE = "nonce"
    }
}
