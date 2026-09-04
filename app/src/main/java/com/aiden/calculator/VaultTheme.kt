package com.aiden.calculator

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

@Composable
internal fun CalculatorTheme(accent: VaultAccentPreset, dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = Color(accent.primary),
            onPrimary = Color(accent.onPrimary),
            primaryContainer = Color(accent.primaryContainer),
            secondaryContainer = Color(accent.secondaryContainer),
            background = Color(0xFF101413),
            surface = Color(0xFF1A1F1D),
            surfaceVariant = Color(0xFF28302D),
            onSurfaceVariant = Color(0xFFB8C8C2),
        ) else lightColorScheme(
            primary = Color(accent.primaryContainer),
            onPrimary = Color(accent.onPrimaryContainer),
            primaryContainer = Color(0xFFD7E8E1),
            secondaryContainer = Color(0xFFE5ECE8),
            background = Color(0xFFF7F9F7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE9EFEC),
        ),
        content = content,
    )
}

@Composable
internal fun VaultTheme(accent: VaultAccentPreset, dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = Color(accent.primary),
            onPrimary = Color(accent.onPrimary),
            primaryContainer = Color(accent.primaryContainer),
            onPrimaryContainer = Color(accent.onPrimaryContainer),
            secondaryContainer = Color(accent.secondaryContainer),
            background = Color(0xFF111614),
            surface = Color(0xFF1A211F),
            surfaceVariant = Color(0xFF28312E),
            onSurfaceVariant = Color(0xFFB8C8C2),
        ) else lightColorScheme(
            primary = Color(accent.primaryContainer),
            onPrimary = Color(accent.onPrimaryContainer),
            primaryContainer = Color(0xFFD7E8E1),
            onPrimaryContainer = Color(0xFF1B3B32),
            secondaryContainer = Color(0xFFE5ECE8),
            background = Color(0xFFF7F9F7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE9EFEC),
            onSurfaceVariant = Color(0xFF51615B),
        ),
        content = content,
    )
}

@Composable
internal fun AppTheme(vaultContent: Boolean, accent: VaultAccentPreset, mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    if (vaultContent) VaultTheme(accent, dark, content) else CalculatorTheme(accent, dark, content)
}
