package com.aiden.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorUiStateTest {
    @Test fun `history keeps latest thirty entries and clears manually`() {
        val ui = CalculatorUiState()
        repeat(35) { value ->
            ui.clear()
            ui.append(value.toString())
            ui.evaluate()
        }

        assertEquals(30, ui.history.size)
        assertTrue(ui.history.first().startsWith("5 = "))

        ui.clearHistory()
        assertTrue(ui.history.isEmpty())
    }

    @Test fun `history entry can become current expression`() {
        val ui = CalculatorUiState()
        ui.append("2")
        ui.append("+")
        ui.append("3")
        ui.evaluate()

        ui.useHistoryEntry(ui.history.single())

        assertEquals("5", ui.state.expression)
        assertEquals("5", ui.state.display)
    }

    @Test fun `manual text entry accepts alnum secret and clear resets mode`() {
        val ui = CalculatorUiState()

        ui.unlockManualEntry()
        assertTrue(ui.manualEntryUnlocked)
        ui.beginManualTextEntry()
        ui.setManualSecret("Secret2026!")

        assertTrue(ui.manualEntryActive)
        assertEquals("Secret2026", ui.state.expression)

        ui.clear()

        assertEquals(CalculatorInputMode.BUTTONS, ui.inputMode)
        assertEquals("", ui.state.expression)
    }
}
