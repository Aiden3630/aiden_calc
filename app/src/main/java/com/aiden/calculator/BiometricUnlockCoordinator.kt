package com.aiden.calculator

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricUnlockCoordinator(
    private val store: BiometricVaultStore,
    private val session: VaultSession,
) {
    fun configuredVaultId() = store.configuredVaultId()

    fun isAvailable(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun enable(activity: FragmentActivity, vaultId: VaultId, masterKey: ByteArray, completed: (Boolean) -> Unit) {
        val cipher = runCatching { store.prepareEnrollmentCipher() }.getOrElse {
            store.clear()
            completed(false)
            return
        }
        authenticate(activity, cipher, completed = { authenticated ->
            if (authenticated == null) return@authenticate completed(false)
            runCatching { store.save(vaultId, authenticated, masterKey) }
                .onFailure { store.clear() }
                .fold({ completed(true) }, { completed(false) })
        })
    }

    fun disable() = store.clear()

    fun unlock(activity: FragmentActivity, completed: (Boolean) -> Unit) {
        val prepared = runCatching { store.prepareUnlockCipher() }.getOrElse {
            store.clear()
            completed(false)
            return
        } ?: return completed(false)
        authenticate(activity, prepared.second, completed = { authenticated ->
            if (authenticated == null) return@authenticate completed(false)
            val masterKey = runCatching { store.unwrap(prepared.first, authenticated) }.getOrElse {
                store.clear()
                completed(false)
                return@authenticate
            }
            try {
                session.unlock(prepared.first.vaultId, masterKey)
                completed(true)
            } finally {
                masterKey.fill(0)
            }
        })
    }

    private fun authenticate(activity: FragmentActivity, cipher: javax.crypto.Cipher, completed: (javax.crypto.Cipher?) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    completed(result.cryptoObject?.cipher)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    completed(null)
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric_prompt_title))
                .setNegativeButtonText(activity.getString(R.string.cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build(),
            BiometricPrompt.CryptoObject(cipher),
        )
    }
}
