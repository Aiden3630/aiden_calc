package com.aiden.calculator

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class LauncherIconPreset(val alias: String, val label: Int) {
    CALCULATOR("CalculatorLauncher", R.string.launcher_calculator),
    SCANNER("ScannerLauncher", R.string.launcher_scanner),
    NOTES("NotesLauncher", R.string.launcher_notes),
    THRONES("ThronesLauncher", R.string.launcher_thrones),
}

internal fun launcherIconEnabledStates(selected: LauncherIconPreset) =
    LauncherIconPreset.entries.associateWith { it == selected }

class LauncherIconManager(private val context: Context) {
    private val packageManager = context.packageManager

    fun current(): LauncherIconPreset = LauncherIconPreset.entries.firstOrNull(::isEnabled)
        ?: LauncherIconPreset.CALCULATOR

    fun select(preset: LauncherIconPreset) {
        if (Build.VERSION.SDK_INT >= 33) {
            val settings = launcherIconEnabledStates(preset).map { (icon, enabled) ->
                PackageManager.ComponentEnabledSetting(
                    component(icon),
                    enabledState(enabled),
                    PackageManager.DONT_KILL_APP,
                )
            }
            packageManager.setComponentEnabledSettings(settings)
        } else {
            launcherIconEnabledStates(preset).forEach { (icon, enabled) ->
                packageManager.setComponentEnabledSetting(
                    component(icon),
                    enabledState(enabled),
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }

    private fun isEnabled(preset: LauncherIconPreset) =
        packageManager.getComponentEnabledSetting(component(preset)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    private fun component(preset: LauncherIconPreset) = ComponentName(context, "${context.packageName}.${preset.alias}")

    private fun enabledState(enabled: Boolean) =
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
}
