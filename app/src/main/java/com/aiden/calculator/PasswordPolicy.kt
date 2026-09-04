package com.aiden.calculator

object PasswordPolicy {
    private val vaultPassword = Regex("""[A-Za-z0-9]{6,32}""")
    private val manualEntryPin = Regex("""\d{4}""")

    fun isValidVaultPassword(password: String): Boolean = password.matches(vaultPassword)

    fun sanitizeVaultPassword(input: String): String =
        input.filter { it.isLetterOrDigit() && it.code < 128 }.take(32)

    fun isValidManualEntryPin(pin: String): Boolean =
        pin.matches(manualEntryPin) && pin != RECOVERY_CODE

    fun sanitizeManualEntryPin(input: String): String =
        input.filter(Char::isDigit).take(4)

    const val RECOVERY_CODE = "7777"
}
