package com.aiden.calculator

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom

class CalculatorInputPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("calculator_input", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    var manualEntryConfigured by mutableStateOf(preferences.contains(KEY_HASH))
        private set

    fun configureManualEntryPin(pin: String): Boolean {
        if (!PasswordPolicy.isValidManualEntryPin(pin)) return false
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val hash = hash(pin, salt)
        preferences.edit()
            .putString(KEY_SALT, encode(salt))
            .putString(KEY_HASH, encode(hash))
            .apply()
        manualEntryConfigured = true
        return true
    }

    fun verifyManualEntryPin(pin: String): Boolean {
        if (!PasswordPolicy.isValidManualEntryPin(pin)) return false
        val salt = preferences.getString(KEY_SALT, null)?.let(::decode) ?: return false
        val expected = preferences.getString(KEY_HASH, null)?.let(::decode) ?: return false
        return MessageDigest.isEqual(expected, hash(pin, salt))
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(salt + pin.toByteArray())

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val KEY_SALT = "manualEntryPinSalt"
        const val KEY_HASH = "manualEntryPinHash"
        const val SALT_SIZE = 16
    }
}
