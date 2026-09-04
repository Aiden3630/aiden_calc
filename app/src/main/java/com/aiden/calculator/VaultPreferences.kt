package com.aiden.calculator

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PrivacyPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("privacy", Context.MODE_PRIVATE)

    var screenshotsBlocked by mutableStateOf(preferences.getBoolean(KEY_SCREENSHOTS_BLOCKED, true))
        private set

    fun updateScreenshotsBlocked(blocked: Boolean) {
        preferences.edit().putBoolean(KEY_SCREENSHOTS_BLOCKED, blocked).apply()
        screenshotsBlocked = blocked
    }

    private companion object {
        const val KEY_SCREENSHOTS_BLOCKED = "screenshotsBlocked"
    }
}

class UnlockPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("unlock", Context.MODE_PRIVATE)

    var requireEqualsForUnlock by mutableStateOf(preferences.getBoolean(KEY_REQUIRE_EQUALS, true))
        private set

    var reminderDays by mutableStateOf(preferences.getInt(KEY_REMINDER_DAYS, REMINDER_OFF))
        private set

    var lastSuccessfulUnlockAt by mutableStateOf(preferences.getLong(KEY_LAST_UNLOCK_AT, 0L))
        private set

    var lastReminderShownAt by mutableStateOf(preferences.getLong(KEY_LAST_REMINDER_SHOWN_AT, 0L))
        private set

    fun updateRequireEqualsForUnlock(requireEquals: Boolean) {
        preferences.edit().putBoolean(KEY_REQUIRE_EQUALS, requireEquals).apply()
        requireEqualsForUnlock = requireEquals
    }

    fun updateReminderDays(days: Int) {
        require(days == REMINDER_OFF || days in REMINDER_OPTIONS)
        preferences.edit().putInt(KEY_REMINDER_DAYS, days).apply()
        reminderDays = days
    }

    fun markSuccessfulUnlock(nowMillis: Long = System.currentTimeMillis()) {
        preferences.edit().putLong(KEY_LAST_UNLOCK_AT, nowMillis).apply()
        lastSuccessfulUnlockAt = nowMillis
    }

    fun shouldShowForgotPasswordReminder(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val last = lastSuccessfulUnlockAt
        val days = reminderDays
        return days != REMINDER_OFF &&
            last > 0L &&
            nowMillis - last >= days * DAY_MS &&
            nowMillis - lastReminderShownAt >= DAY_MS
    }

    fun markForgotPasswordReminderShown(nowMillis: Long = System.currentTimeMillis()) {
        preferences.edit().putLong(KEY_LAST_REMINDER_SHOWN_AT, nowMillis).apply()
        lastReminderShownAt = nowMillis
    }

    companion object {
        const val REMINDER_OFF = 0
        val REMINDER_OPTIONS = listOf(3, 5, 7, 14, 30)
        private const val KEY_REQUIRE_EQUALS = "requireEqualsForUnlock"
        private const val KEY_REMINDER_DAYS = "reminderDays"
        private const val KEY_LAST_UNLOCK_AT = "lastSuccessfulUnlockAt"
        private const val KEY_LAST_REMINDER_SHOWN_AT = "lastReminderShownAt"
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}

class DecoyPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("decoy", Context.MODE_PRIVATE)

    var hintsEnabled by mutableStateOf(preferences.getBoolean(KEY_HINTS_ENABLED, true))
        private set

    fun updateHintsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HINTS_ENABLED, enabled).apply()
        hintsEnabled = enabled
    }

    private companion object {
        const val KEY_HINTS_ENABLED = "hintsEnabled"
    }
}

enum class VaultAccentPreset(
    val label: String,
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondaryContainer: Long,
) {
    GREEN("Зеленая", 0xFF9CBDAF, 0xFF17372E, 0xFF294B41, 0xFFD0E8DE, 0xFF34463F),
    BLUE("Синяя", 0xFF9BC7E8, 0xFF06344F, 0xFF214A63, 0xFFD0E9FA, 0xFF304854),
    PURPLE("Фиолетовая", 0xFFC9B4F4, 0xFF34205B, 0xFF4B386D, 0xFFE9DCFF, 0xFF493F56),
    AMBER("Янтарная", 0xFFE6BE7A, 0xFF493000, 0xFF604A23, 0xFFFFE2AD, 0xFF514633),
    PINK("Розовая", 0xFFF0AEC6, 0xFF572137, 0xFF69384B, 0xFFFFD9E5, 0xFF594048),
    NEUTRAL("Нейтральная", 0xFFBCC4C1, 0xFF26302D, 0xFF414946, 0xFFE0E7E4, 0xFF414947),
}

data class VaultAppearanceState(
    val accent: VaultAccentPreset = VaultAccentPreset.GREEN,
    val albumColumns: Int = 3,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val albumViewMode: AlbumViewMode = AlbumViewMode.GRID,
)

enum class ThemeMode(val label: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

enum class AlbumViewMode(val label: Int) {
    GRID(R.string.album_grid),
    LIST(R.string.album_list),
    COMPACT_GRID(R.string.album_compact_grid),
}

class AppearancePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    var state by mutableStateOf(
        VaultAppearanceState(
            accent = runCatching {
                VaultAccentPreset.valueOf(preferences.getString(KEY_ACCENT, null) ?: "")
            }.getOrDefault(VaultAccentPreset.GREEN),
            albumColumns = preferences.getInt(KEY_COLUMNS, 3).takeIf { it == 3 || it == 4 } ?: 3,
            themeMode = runCatching {
                ThemeMode.valueOf(preferences.getString(KEY_THEME_MODE, null) ?: "")
            }.getOrDefault(ThemeMode.SYSTEM),
            albumViewMode = runCatching {
                AlbumViewMode.valueOf(preferences.getString(KEY_ALBUM_VIEW_MODE, null) ?: "")
            }.getOrDefault(AlbumViewMode.GRID),
        ),
    )
        private set

    fun setAccent(accent: VaultAccentPreset) {
        preferences.edit().putString(KEY_ACCENT, accent.name).apply()
        state = state.copy(accent = accent)
    }

    fun setAlbumColumns(columns: Int) {
        require(columns == 3 || columns == 4)
        preferences.edit().putInt(KEY_COLUMNS, columns).apply()
        state = state.copy(albumColumns = columns)
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
        state = state.copy(themeMode = mode)
    }

    fun setAlbumViewMode(mode: AlbumViewMode) {
        preferences.edit().putString(KEY_ALBUM_VIEW_MODE, mode.name).apply()
        state = state.copy(albumViewMode = mode)
    }

    private companion object {
        const val KEY_ACCENT = "accent"
        const val KEY_COLUMNS = "albumColumns"
        const val KEY_THEME_MODE = "themeMode"
        const val KEY_ALBUM_VIEW_MODE = "albumViewMode"
    }
}
