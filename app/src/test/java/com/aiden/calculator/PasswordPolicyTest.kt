package com.aiden.calculator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {
    @Test fun `vault password accepts latin alnum length six to thirty two`() {
        assertTrue(PasswordPolicy.isValidVaultPassword("abc123"))
        assertTrue(PasswordPolicy.isValidVaultPassword("Secret2026"))
        assertTrue(PasswordPolicy.isValidVaultPassword("A1B2C3"))
    }

    @Test fun `vault password rejects short spaces and symbols`() {
        assertFalse(PasswordPolicy.isValidVaultPassword("a1B2c"))
        assertFalse(PasswordPolicy.isValidVaultPassword("Secret 2026"))
        assertFalse(PasswordPolicy.isValidVaultPassword("Secret-2026"))
    }

    @Test fun `manual entry pin is four digits but not recovery code`() {
        assertTrue(PasswordPolicy.isValidManualEntryPin("1234"))
        assertFalse(PasswordPolicy.isValidManualEntryPin("123"))
        assertFalse(PasswordPolicy.isValidManualEntryPin("12345"))
        assertFalse(PasswordPolicy.isValidManualEntryPin("abcd"))
        assertFalse(PasswordPolicy.isValidManualEntryPin(PasswordPolicy.RECOVERY_CODE))
    }
}
