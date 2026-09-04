package com.aiden.calculator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeaturePreferencesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun clearPreferences() {
        listOf("unlock", "decoy", "appearance", "locale", "browser").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun `unlock defaults and toggles persist`() {
        val preferences = UnlockPreferences(context)
        assertTrue(preferences.requireEqualsForUnlock)
        assertEquals(UnlockPreferences.REMINDER_OFF, preferences.reminderDays)

        preferences.updateRequireEqualsForUnlock(false)
        assertFalse(UnlockPreferences(context).requireEqualsForUnlock)
    }

    @Test fun `forgot password reminder respects schedule and daily limit`() {
        val preferences = UnlockPreferences(context)
        preferences.updateReminderDays(3)
        preferences.markSuccessfulUnlock(1_000L)

        assertFalse(preferences.shouldShowForgotPasswordReminder(2_000L))
        assertTrue(preferences.shouldShowForgotPasswordReminder(1_000L + 3 * DAY_MS))
        preferences.markForgotPasswordReminderShown(1_000L + 3 * DAY_MS)
        assertFalse(preferences.shouldShowForgotPasswordReminder(1_000L + 3 * DAY_MS + 1_000L))
    }

    @Test fun `decoy hints default on and persist`() {
        assertTrue(DecoyPreferences(context).hintsEnabled)
        DecoyPreferences(context).updateHintsEnabled(false)
        assertFalse(DecoyPreferences(context).hintsEnabled)
    }

    @Test fun `appearance modes persist and invalid values fallback`() {
        val preferences = AppearancePreferences(context)
        preferences.setThemeMode(ThemeMode.DARK)
        preferences.setAlbumViewMode(AlbumViewMode.COMPACT_GRID)

        assertEquals(ThemeMode.DARK, AppearancePreferences(context).state.themeMode)
        assertEquals(AlbumViewMode.COMPACT_GRID, AppearancePreferences(context).state.albumViewMode)

        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit()
            .putString("themeMode", "BROKEN")
            .putString("albumViewMode", "BROKEN")
            .apply()

        assertEquals(ThemeMode.SYSTEM, AppearancePreferences(context).state.themeMode)
        assertEquals(AlbumViewMode.GRID, AppearancePreferences(context).state.albumViewMode)
    }

    @Test fun `locale preference persists and invalid value falls back`() {
        LocalePreferences(context).setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, LocalePreferences(context).language)

        context.getSharedPreferences("locale", Context.MODE_PRIVATE).edit().putString("language", "BAD").apply()
        assertEquals(AppLanguage.SYSTEM, LocalePreferences(context).language)
    }

    @Test fun `browser preferences persist`() {
        val preferences = BrowserPreferences(context)
        assertTrue(preferences.javaScriptEnabled)
        assertTrue(preferences.clearOnLock)

        preferences.updateJavaScriptEnabled(false)
        preferences.updateClearOnLock(false)

        assertFalse(BrowserPreferences(context).javaScriptEnabled)
        assertFalse(BrowserPreferences(context).clearOnLock)
    }

    @Test fun `browser url normalization`() {
        assertEquals("https://example.com", BrowserUrlNormalizer.normalize("example.com"))
        assertEquals("http://example.com", BrowserUrlNormalizer.normalize("http://example.com"))
        assertEquals("https://duckduckgo.com/?q=private+search", BrowserUrlNormalizer.normalize("private search"))
        assertNull(BrowserUrlNormalizer.normalize(" "))
    }

    @Test fun `share intent is plain text send`() {
        val intent = ShareIntentFactory.create("text")
        assertEquals("android.intent.action.SEND", intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("text", intent.getStringExtra("android.intent.extra.TEXT"))
    }

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
