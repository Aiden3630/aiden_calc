package com.aiden.calculator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecurityFlowTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val crypto = VaultCrypto()

    @Before fun clearPreferences() {
        listOf("vault_config", "unlock", "calculator_input").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun `recovery update requires current password and new answer verifies`() {
        val store = store()
        store.create(VaultId.ONE, "Secret1", "old?", "old")

        assertFalse(store.updateRecovery(VaultId.ONE, "000000", "new?", "new"))
        assertFalse(store.verifyRecovery(VaultId.ONE, "new"))

        assertTrue(store.updateRecovery(VaultId.ONE, "Secret1", "new?", "new"))
        assertTrue(store.verifyRecovery(VaultId.ONE, "new"))
        assertEquals("new?", store.get(VaultId.ONE).recoveryQuestion)
    }

    @Test fun `successful unlock marks timestamp and failed unlock does not`() {
        val store = store()
        store.create(VaultId.ONE, "123456", "q", "a")
        val preferences = UnlockPreferences(context)
        val coordinator = UnlockCoordinator(store, VaultSession(), ElapsedRealtimeClock { 0L }, preferences)

        assertFalse(coordinator.unlock("000000"))
        assertEquals(0L, preferences.lastSuccessfulUnlockAt)

        assertTrue(coordinator.unlock("123456"))
        assertNotEquals(0L, preferences.lastSuccessfulUnlockAt)
    }

    @Test fun `alnum passwords unlock primary and decoy vaults`() {
        val store = store()
        store.create(VaultId.ONE, "Secret2026", "q1", "a1")
        store.create(VaultId.TWO, "Decoy2026", "q2", "a2")

        assertEquals(VaultId.ONE, store.unlock("Secret2026")?.first)
        assertEquals(VaultId.TWO, store.unlock("Decoy2026")?.first)
        assertFalse(store.changePassword(VaultId.ONE, "Secret2026", "bad pass"))
        assertTrue(store.changePassword(VaultId.ONE, "Secret2026", "A1B2C3"))
        assertEquals(VaultId.ONE, store.unlock("A1B2C3")?.first)
    }

    @Test fun `manual entry pin stores hashed verification and rejects recovery code`() {
        val input = CalculatorInputPreferences(context)

        assertFalse(input.configureManualEntryPin("7777"))
        assertFalse(input.manualEntryConfigured)

        assertTrue(input.configureManualEntryPin("1234"))
        assertTrue(input.manualEntryConfigured)
        assertTrue(input.verifyManualEntryPin("1234"))
        assertFalse(input.verifyManualEntryPin("9999"))
        assertTrue(CalculatorInputPreferences(context).verifyManualEntryPin("1234"))
    }

    @Test fun `changing decoy password does not change primary password`() {
        val store = store()
        store.create(VaultId.ONE, "123456", "q1", "a1")
        store.create(VaultId.TWO, "654321", "q2", "a2")

        assertTrue(store.changePassword(VaultId.TWO, "654321", "777777"))
        assertTrue(store.unlock("123456")?.first == VaultId.ONE)
        assertTrue(store.unlock("777777")?.first == VaultId.TWO)
    }

    @Test fun `wifi transfer generates pin and stops`() {
        val item = VaultItem(
            id = "allowed",
            vaultId = VaultId.ONE,
            blobName = "blob",
            type = VaultItemType.FILE,
            encryptedName = byteArrayOf(1),
            encryptedMime = byteArrayOf(2),
            size = 1,
        )
        val controller = WifiTransferController(nowMillis = { 1_000L }, pinGenerator = { "123456" })
        val session = controller.start(listOf(item))

        assertEquals("123456", session.pin)
        assertTrue(controller.isRunning())
        controller.stop()
        assertFalse(controller.isRunning())
    }

    private fun store() = VaultConfigStore(context, crypto, object : DeviceSecretStore() {
        override fun strengthen(password: String): CharArray = password.toCharArray()
    })
}
