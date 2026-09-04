package com.aiden.calculator

import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultInstrumentedTest {
    private val context get() = ApplicationProvider.getApplicationContext<CalculatorApplication>()

    @Before
    fun clearConfiguration() {
        context.getSharedPreferences("vault_config", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("privacy", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("calculator_input", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun bothPasswordsUnlockIndependentlyAndClearingOneKeepsTheOther() {
        val crypto = VaultCrypto()
        val configs = VaultConfigStore(context, crypto)
        configs.create(VaultId.ONE, "12345678", "one?", "one")
        configs.create(VaultId.TWO, "87654321", "two?", "two")

        assertTrue(configs.unlock("12345678")?.first == VaultId.ONE)
        assertTrue(configs.unlock("87654321")?.first == VaultId.TWO)
        configs.clear(VaultId.ONE)
        assertTrue(configs.unlock("12345678") == null)
        assertTrue(configs.unlock("87654321")?.first == VaultId.TWO)
    }

    @Test
    fun duplicatePasswordIsRejectedForChangeAndRecovery() {
        val configs = VaultConfigStore(context, VaultCrypto())
        configs.create(VaultId.ONE, "12345678", "one?", "one")
        configs.create(VaultId.TWO, "87654321", "two?", "two")

        assertFalse(configs.changePassword(VaultId.TWO, "87654321", "12345678"))
        assertFalse(configs.isPasswordAvailable(VaultId.TWO, "12345678"))
        configs.clear(VaultId.TWO)
        assertThrows(IllegalArgumentException::class.java) {
            configs.create(VaultId.TWO, "12345678", "two?", "two")
        }
    }

    @Test
    fun startupClearsTemporaryExports() {
        val directory = File(context.cacheDir, "temporary_exports").apply { mkdirs() }
        val file = File(directory, "leftover").apply { writeText("plain") }
        context.container.temporaryExports.clear()
        assertFalse(file.exists())
    }

    @Test
    fun calculatorAllowsScreenshots() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                assertTrue(flags and WindowManager.LayoutParams.FLAG_SECURE == 0)
            }
        }
    }

    @Test
    fun screenshotsAreBlockedByDefaultAndPreferenceChangesImmediately() {
        val privacy = PrivacyPreferences(context)
        assertTrue(privacy.screenshotsBlocked)
        privacy.updateScreenshotsBlocked(false)
        assertFalse(privacy.screenshotsBlocked)
        assertFalse(PrivacyPreferences(context).screenshotsBlocked)
    }

    @Test
    fun manualEntrySetupPersists() {
        val input = CalculatorInputPreferences(context)

        assertTrue(input.configureManualEntryPin("1234"))

        assertTrue(CalculatorInputPreferences(context).manualEntryConfigured)
        assertTrue(CalculatorInputPreferences(context).verifyManualEntryPin("1234"))
    }

    @Test
    fun appearancePreferencesPersistAccentAndAlbumColumns() {
        AppearancePreferences(context).apply {
            setAccent(VaultAccentPreset.PURPLE)
            setAlbumColumns(4)
        }
        assertEquals(VaultAccentPreset.PURPLE, AppearancePreferences(context).state.accent)
        assertEquals(4, AppearancePreferences(context).state.albumColumns)
    }
}
