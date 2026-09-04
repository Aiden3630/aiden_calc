package com.aiden.calculator

enum class CalculatorInputMode {
    BUTTONS,
    MANUAL_READY,
    MANUAL_TEXT,
}

class CalculatorUiState(private val engine: CalculatorEngine = CalculatorEngine()) {
    var state = CalculatorEngine.State()
        private set
    var inputMode = CalculatorInputMode.BUTTONS
        private set
    private val entries = mutableListOf<String>()

    val history: List<String> get() = entries.toList()
    val manualEntryUnlocked: Boolean get() = inputMode != CalculatorInputMode.BUTTONS
    val manualEntryActive: Boolean get() = inputMode == CalculatorInputMode.MANUAL_TEXT

    fun append(token: String) {
        state = engine.append(state, token)
    }

    fun clear() {
        state = engine.clear(state)
        resetManualEntry()
    }

    fun backspace() {
        state = if (manualEntryActive) {
            val expression = state.expression.dropLast(1)
            state.copy(expression = expression, display = expression.ifEmpty { "0" }, error = false)
        } else {
            engine.backspace(state)
        }
    }

    fun toggleSign() {
        state = engine.toggleSign(state)
    }

    fun evaluate() {
        val expression = state.expression
        state = engine.evaluate(state)
        if (!state.error && expression.isNotBlank()) {
            entries += "${engine.formatExpression(expression)} = ${state.display}"
            while (entries.size > MAX_HISTORY) entries.removeAt(0)
        }
    }

    fun useHistoryEntry(entry: String) {
        val result = entry.substringAfterLast(" = ")
        state = CalculatorEngine.State(expression = result, display = engine.formatExpression(result))
    }

    fun clearHistory() {
        entries.clear()
    }

    fun unlockManualEntry() {
        inputMode = CalculatorInputMode.MANUAL_READY
        state = engine.clear(state)
    }

    fun beginManualTextEntry() {
        if (manualEntryUnlocked) inputMode = CalculatorInputMode.MANUAL_TEXT
    }

    fun setManualSecret(secret: String) {
        if (!manualEntryActive) return
        val sanitized = PasswordPolicy.sanitizeVaultPassword(secret)
        state = state.copy(expression = sanitized, display = sanitized.ifEmpty { "0" }, error = false)
    }

    fun resetManualEntry() {
        inputMode = CalculatorInputMode.BUTTONS
    }

    companion object {
        const val MAX_HISTORY = 30
    }
}
