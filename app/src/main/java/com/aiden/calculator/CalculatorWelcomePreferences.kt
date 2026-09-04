package com.aiden.calculator

import android.content.Context

class CalculatorWelcomePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("calculator_welcome", Context.MODE_PRIVATE)

    fun shouldShowWelcome(): Boolean = !preferences.getBoolean(KEY_SEEN, false)

    fun markWelcomeSeen() {
        preferences.edit().putBoolean(KEY_SEEN, true).apply()
    }

    private companion object {
        const val KEY_SEEN = "seen"
    }
}
