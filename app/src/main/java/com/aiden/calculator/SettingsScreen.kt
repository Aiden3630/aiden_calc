package com.aiden.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
    screenshotsBlocked: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    emergencyLockEnabled: Boolean,
    emergencyLockSupported: Boolean,
    accent: VaultAccentPreset,
    albumColumns: Int,
    themeMode: ThemeMode,
    albumViewMode: AlbumViewMode,
    launcherIcon: LauncherIconPreset,
    language: AppLanguage,
    reminderDays: Int,
    decoyHintsEnabled: Boolean,
    toggleBiometric: (Boolean) -> Unit,
    toggleEmergencyLock: (Boolean) -> Unit,
    toggleScreenshots: (Boolean) -> Unit,
    toggleDecoyHints: (Boolean) -> Unit,
    changePassword: () -> Unit,
    recovery: () -> Unit,
    decoy: () -> Unit,
    privacyPolicy: () -> Unit,
    forgotPasswordReminder: () -> Unit,
    lockNow: () -> Unit,
    chooseAccent: () -> Unit,
    chooseThemeMode: () -> Unit,
    chooseColumns: () -> Unit,
    chooseAlbumViewMode: () -> Unit,
    chooseIcon: () -> Unit,
    chooseLanguage: () -> Unit,
    cloud: () -> Unit,
    wifiTransfer: () -> Unit,
    fileTransfer: () -> Unit,
    share: () -> Unit,
    trash: () -> Unit,
    repair: () -> Unit,
    about: () -> Unit,
) {
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Card(
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Text(
                    stringResource(R.string.recovery_hint_card),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SettingsSection(R.string.privacy) {
                ToggleSettingsRow(Icons.Default.Fingerprint, R.string.biometric_login, R.string.biometric_login_hint, biometricEnabled, toggleBiometric, biometricAvailable)
                ToggleSettingsRow(Icons.Default.PhoneAndroid, R.string.emergency_lock, R.string.emergency_lock_hint, emergencyLockEnabled, toggleEmergencyLock, emergencyLockSupported)
                ToggleSettingsRow(Icons.Default.Screenshot, R.string.block_screenshots, R.string.block_screenshots_hint, screenshotsBlocked, toggleScreenshots)
                SettingsRow(
                    Icons.Default.Security,
                    R.string.forgot_password_reminder,
                    R.string.forgot_password_reminder_hint,
                    if (reminderDays == UnlockPreferences.REMINDER_OFF) stringResource(R.string.disabled)
                    else stringResource(R.string.days_count, reminderDays),
                    action = forgotPasswordReminder,
                )
            }
            SettingsSection(R.string.customize) {
                SettingsRow(Icons.Default.ColorLens, R.string.theme_color, R.string.theme_color_hint, accent.label, accentColor = Color(accent.primary), action = chooseAccent)
                SettingsRow(Icons.Default.ColorLens, R.string.theme_mode, R.string.theme_mode_hint, stringResource(themeMode.label), action = chooseThemeMode)
                SettingsRow(Icons.Default.GridView, R.string.album_layout, R.string.album_layout_hint, stringResource(albumViewMode.label), action = chooseAlbumViewMode)
                SettingsRow(Icons.Default.GridView, R.string.album_columns, R.string.album_columns_hint, stringResource(R.string.columns_count, albumColumns), action = chooseColumns)
                SettingsRow(Icons.Default.Security, R.string.app_icon, R.string.app_icon_hint, stringResource(launcherIcon.label), action = chooseIcon)
                SettingsRow(Icons.Default.Language, R.string.change_language, R.string.change_language_hint, stringResource(language.labelRes), action = chooseLanguage)
            }
            SettingsSection(R.string.other) {
                SettingsRow(Icons.Default.Password, R.string.change_password, R.string.change_password_hint, action = changePassword)
                SettingsRow(Icons.Default.Security, R.string.recovery_methods, R.string.recovery_methods_hint, action = recovery)
                SettingsRow(Icons.Default.Security, R.string.decoy_vault, R.string.decoy_vault_hint, action = decoy)
                ToggleSettingsRow(Icons.Default.Security, R.string.decoy_hints, R.string.decoy_hints_hint, decoyHintsEnabled, toggleDecoyHints)
                SettingsRow(Icons.Default.Security, R.string.privacy_policy, R.string.privacy_policy_hint, action = privacyPolicy)
                SettingsRow(Icons.Default.Delete, R.string.trash, R.string.trash_hint, action = trash)
                SettingsRow(Icons.Default.Search, R.string.find_lost_file, R.string.find_lost_file_hint, action = repair)
                SettingsRow(Icons.Default.Security, R.string.cloud_file_protection, R.string.cloud_file_protection_hint, action = cloud)
                SettingsRow(Icons.Default.Security, R.string.wifi_transfer, R.string.wifi_transfer_hint, action = wifiTransfer)
                SettingsRow(Icons.Default.Security, R.string.file_transfer, R.string.file_transfer_hint, action = fileTransfer)
                SettingsRow(Icons.Default.Security, R.string.share_with_friends, R.string.share_with_friends_hint, action = share)
                SettingsRow(Icons.Default.Info, R.string.about_app, R.string.about_app_hint, action = about)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: Int, content: @Composable () -> Unit) {
    Text(stringResource(title), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) { Column { content() } }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: Int, subtitle: Int? = null, value: String? = null, accentColor: Color? = null, action: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).clickable(onClick = action).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(stringResource(title), maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(
                    stringResource(it),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        accentColor?.let { Box(Modifier.padding(end = 10.dp).size(18.dp).background(it, MaterialTheme.shapes.extraLarge)) }
        value?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun ToggleSettingsRow(icon: ImageVector, title: Int, subtitle: Int? = null, checked: Boolean, toggle: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBox(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(stringResource(title), maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(
                    stringResource(it),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!enabled) Text(stringResource(R.string.not_supported), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        else Switch(checked, toggle)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun IconBox(icon: ImageVector) {
    androidx.compose.material3.Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), MaterialTheme.shapes.large).padding(7.dp),
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AccentPickerSheet(selected: VaultAccentPreset, choose: (VaultAccentPreset) -> Unit, close: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close) {
        Text(stringResource(R.string.theme_color), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            VaultAccentPreset.entries.forEach { preset ->
                Box(
                    Modifier.size(if (preset == selected) 52.dp else 44.dp)
                        .background(Color(preset.primary), MaterialTheme.shapes.extraLarge)
                        .clickable { choose(preset) },
                )
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AlbumLayoutSheet(selected: Int, choose: (Int) -> Unit, close: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close) {
        Text(stringResource(R.string.album_layout), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        listOf(3, 4).forEach { columns ->
            SettingsRow(Icons.Default.GridView, R.string.album_layout, value = stringResource(R.string.columns_count, columns)) { choose(columns) }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ThemeModeSheet(selected: ThemeMode, choose: (ThemeMode) -> Unit, close: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close) {
        Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        ThemeMode.entries.forEach { mode ->
            SettingsRow(Icons.Default.ColorLens, mode.label, value = if (mode == selected) stringResource(R.string.enabled) else null) { choose(mode) }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AlbumViewModeSheet(selected: AlbumViewMode, choose: (AlbumViewMode) -> Unit, close: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close) {
        Text(stringResource(R.string.album_layout), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        AlbumViewMode.entries.forEach { mode ->
            SettingsRow(Icons.Default.GridView, mode.label, value = if (mode == selected) stringResource(R.string.enabled) else null) { choose(mode) }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun IconPickerSheet(selected: LauncherIconPreset, choose: (LauncherIconPreset) -> Unit, close: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close) {
        Text(stringResource(R.string.app_icon), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        LauncherIconPreset.entries.forEach { preset ->
            SettingsRow(Icons.Default.Security, preset.label, value = if (preset == selected) stringResource(R.string.enabled) else null) { choose(preset) }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ForgotPasswordReminderSheet(selected: Int, choose: (Int) -> Unit, close: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close) {
        Text(stringResource(R.string.forgot_password_reminder), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        SettingsRow(
            Icons.Default.Security,
            R.string.disabled,
            value = if (selected == UnlockPreferences.REMINDER_OFF) stringResource(R.string.enabled) else null,
        ) { choose(UnlockPreferences.REMINDER_OFF) }
        UnlockPreferences.REMINDER_OPTIONS.forEach { days ->
            SettingsRow(
                Icons.Default.Security,
                R.string.forgot_password_reminder,
                value = if (selected == days) stringResource(R.string.enabled) else stringResource(R.string.days_count, days),
            ) { choose(days) }
        }
        Spacer(Modifier.height(18.dp))
    }
}
