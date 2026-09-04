package com.aiden.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LauncherIconPresetTest {
    @Test fun `launcher presets use distinct aliases and labels`() {
        assertEquals(LauncherIconPreset.entries.size, LauncherIconPreset.entries.map { it.alias }.toSet().size)
        assertEquals(LauncherIconPreset.entries.size, LauncherIconPreset.entries.map { it.label }.toSet().size)
        assertNotEquals(LauncherIconPreset.CALCULATOR.alias, LauncherIconPreset.SCANNER.alias)
    }

    @Test fun `selecting preset enables exactly one launcher component`() {
        LauncherIconPreset.entries.forEach { selected ->
            val states = launcherIconEnabledStates(selected)
            assertEquals(1, states.values.count { it })
            assertEquals(true, states[selected])
        }
    }
}
