package com.aiden.calculator

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreensSmokeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<CalculatorApplication>()

    @Before
    fun clearState() {
        listOf("vault_config", "calculator_input", "calculator_welcome").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test
    fun setupScreenIsReachableOnFreshInstall() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText(context.getString(R.string.setup_space_title, context.getString(R.string.vault_one))).assertExists()
        }
    }

    @Test
    fun calculatorShowsWelcomeAndFormulaPadWhenConfigured() {
        context.container.configs.create(VaultId.ONE, "12345678", "one?", "one")
        context.container.configs.create(VaultId.TWO, "87654321", "two?", "two")
        context.container.calculatorInput.configureManualEntryPin("1234")

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText(context.getString(R.string.calculator_welcome_title)).assertExists()
            compose.onNodeWithText("sin").assertExists()
        }
    }
}
