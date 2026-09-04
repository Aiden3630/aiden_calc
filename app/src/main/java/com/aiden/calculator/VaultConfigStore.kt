package com.aiden.calculator

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac

class VaultConfigStore(
    context: Context,
    private val crypto: VaultCrypto,
    private val deviceSecret: DeviceSecretStore = DeviceSecretStore(),
) {
    private val prefs = context.getSharedPreferences("vault_config", Context.MODE_PRIVATE)

    fun isConfigured() = VaultId.entries.all { prefs.contains("${it.name}.master") }

    fun create(id: VaultId, password: String, question: String, answer: String) {
        require(PasswordPolicy.isValidVaultPassword(password) && isPasswordAvailable(id, password)) {
            "Password must be valid and unique"
        }
        val master = crypto.randomKey()
        try {
            val encrypted = crypto.wrapMasterKey(deviceSecret.strengthen(password), master)
            val recoverySalt = crypto.randomSalt()
            prefs.edit()
                .putString("$id.salt", encode(encrypted.salt))
                .putInt("$id.iterations", encrypted.iterations)
                .putString("$id.nonce", encode(encrypted.nonce))
                .putString("$id.master", encode(encrypted.ciphertext))
                .putInt("$id.version", 2)
                .putString("$id.question", question)
                .putString("$id.recoverySalt", encode(recoverySalt))
                .putString("$id.recoveryHash", encode(crypto.hashRecovery(answer, recoverySalt)))
                .apply()
        } finally {
            master.fill(0)
        }
    }

    fun get(id: VaultId): VaultConfig = VaultConfig(
        id = id,
        encryptedMasterKey = EncryptedMasterKey(
            salt = decode(requireNotNull(prefs.getString("$id.salt", null))),
            iterations = prefs.getInt("$id.iterations", VaultCrypto.ITERATIONS),
            nonce = decode(requireNotNull(prefs.getString("$id.nonce", null))),
            ciphertext = decode(requireNotNull(prefs.getString("$id.master", null))),
        ),
        recoveryQuestion = requireNotNull(prefs.getString("$id.question", null)),
        recoverySalt = decode(requireNotNull(prefs.getString("$id.recoverySalt", null))),
        recoveryHash = decode(requireNotNull(prefs.getString("$id.recoveryHash", null))),
    )

    fun unlock(secret: String): Pair<VaultId, ByteArray>? {
        if (!PasswordPolicy.isValidVaultPassword(secret)) return null
        for (id in VaultId.entries) {
            val config = runCatching { get(id) }.getOrNull() ?: continue
            val key = runCatching { unwrap(id, secret, config) }.getOrNull()
            if (key != null) {
                if (prefs.getInt("$id.version", 1) < 2) saveWrapped(id, crypto.wrapMasterKey(deviceSecret.strengthen(secret), key))
                return id to key
            }
        }
        return null
    }

    fun verifyRecovery(id: VaultId, answer: String): Boolean {
        val config = get(id)
        return MessageDigest.isEqual(config.recoveryHash, crypto.hashRecovery(answer, config.recoverySalt))
    }

    fun changePassword(id: VaultId, oldPassword: String, newPassword: String): Boolean {
        if (!PasswordPolicy.isValidVaultPassword(newPassword) || !isPasswordAvailable(id, newPassword)) return false
        val config = get(id)
        val master = runCatching { unwrap(id, oldPassword, config) }.getOrNull()
            ?: return false
        return try {
            saveWrapped(id, crypto.wrapMasterKey(deviceSecret.strengthen(newPassword), master))
        } finally {
            master.fill(0)
        }
    }

    fun updateRecovery(id: VaultId, currentPassword: String, question: String, answer: String): Boolean {
        if (question.isBlank() || answer.isBlank()) return false
        val config = get(id)
        val master = runCatching { unwrap(id, currentPassword, config) }.getOrNull()
            ?: return false
        master.fill(0)
        val recoverySalt = crypto.randomSalt()
        prefs.edit()
            .putString("$id.question", question)
            .putString("$id.recoverySalt", encode(recoverySalt))
            .putString("$id.recoveryHash", encode(crypto.hashRecovery(answer, recoverySalt)))
            .apply()
        return true
    }

    fun clear(id: VaultId) {
        val prefix = "$id."
        prefs.edit().also { editor ->
            prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        }.apply()
    }

    fun isPasswordAvailable(id: VaultId, password: String): Boolean {
        val other = VaultId.entries.first { it != id }
        val config = runCatching { get(other) }.getOrNull() ?: return true
        val master = runCatching { unwrap(other, password, config) }.getOrNull()
            ?: return true
        master.fill(0)
        return false
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)

    private fun unwrap(id: VaultId, password: String, config: VaultConfig): ByteArray =
        if (prefs.getInt("$id.version", 1) >= 2) {
            crypto.unwrapMasterKey(deviceSecret.strengthen(password), config.encryptedMasterKey)
        } else {
            crypto.unwrapMasterKey(password.toCharArray(), config.encryptedMasterKey)
        }

    private fun saveWrapped(id: VaultId, encrypted: EncryptedMasterKey): Boolean = prefs.edit()
        .putString("$id.salt", encode(encrypted.salt))
        .putInt("$id.iterations", encrypted.iterations)
        .putString("$id.nonce", encode(encrypted.nonce))
        .putString("$id.master", encode(encrypted.ciphertext))
        .putInt("$id.version", 2)
        .commit()
}

open class DeviceSecretStore {
    companion object {
        private const val ALIAS = "vault-device-secret-v1"
    }

    open fun strengthen(password: String): CharArray {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = store.getKey(ALIAS, null) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN).build())
            generateKey()
        }
        val digest = Mac.getInstance("HmacSHA256").run {
            init(key)
            doFinal(password.toByteArray())
        }
        return Base64.encodeToString(digest, Base64.NO_WRAP).toCharArray().also { digest.fill(0) }
    }
}

class UnlockCoordinator(
    private val configs: VaultConfigStore,
    private val session: VaultSession,
    private val clock: ElapsedRealtimeClock,
    private val preferences: UnlockPreferences? = null,
) {
    private var failures = 0
    private var retryAfter = 0L

    fun unlock(secret: String): Boolean {
        if (clock.now() < retryAfter) return false
        if (!PasswordPolicy.isValidVaultPassword(secret)) return false
        val unlocked = configs.unlock(secret) ?: run {
            failures++
            if (failures >= 5) retryAfter = clock.now() + minOf(30_000L, (failures - 4) * 2_000L)
            return false
        }
        failures = 0
        retryAfter = 0
        session.unlock(unlocked.first, unlocked.second)
        preferences?.markSuccessfulUnlock()
        unlocked.second.fill(0)
        return true
    }
}

class RecoveryCoordinator(private val configs: VaultConfigStore) {
    fun verify(id: VaultId, answer: String) = configs.verifyRecovery(id, answer)
}
