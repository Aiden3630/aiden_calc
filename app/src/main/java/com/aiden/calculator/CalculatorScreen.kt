package com.aiden.calculator

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SetupStepScreen(vaultId: VaultId, step: Int, forbiddenPassword: String? = null, done: (SetupFields) -> Unit) {
    var fields by remember(vaultId) { mutableStateOf(SetupFields()) }
    var invalid by remember { mutableStateOf(false) }
    val title = stringResource(if (vaultId == VaultId.ONE) R.string.vault_one else R.string.decoy_vault)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 520.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.setup_progress, step), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.setup_space_title, title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.setup_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecretField(fields.password, R.string.password) {
                        fields = fields.copy(password = PasswordPolicy.sanitizeVaultPassword(it))
                    }
                    SecretField(fields.confirm, R.string.confirm_password) {
                        fields = fields.copy(confirm = PasswordPolicy.sanitizeVaultPassword(it))
                    }
                    OutlinedTextField(fields.question, { fields = fields.copy(question = it) }, label = { Text(stringResource(R.string.security_question)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(fields.answer, { fields = fields.copy(answer = it) }, label = { Text(stringResource(R.string.security_answer)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
            if (invalid) Text(stringResource(R.string.invalid_setup), color = MaterialTheme.colorScheme.error)
            Button(
                onClick = {
                    invalid = !fields.valid() || fields.password == forbiddenPassword
                    if (!invalid) done(fields)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(stringResource(R.string.continue_label)) }
        }
    }
}

@Composable
internal fun ManualEntryPinSetupScreen(done: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 520.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.setup_progress, 3), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.manual_entry_setup_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.manual_entry_setup_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecretField(pin, R.string.manual_entry_pin, KeyboardType.NumberPassword) {
                        pin = PasswordPolicy.sanitizeManualEntryPin(it)
                    }
                    SecretField(confirm, R.string.confirm_manual_entry_pin, KeyboardType.NumberPassword) {
                        confirm = PasswordPolicy.sanitizeManualEntryPin(it)
                    }
                }
            }
            if (invalid) Text(stringResource(R.string.invalid_manual_entry_pin), color = MaterialTheme.colorScheme.error)
            Button(
                onClick = {
                    invalid = !PasswordPolicy.isValidManualEntryPin(pin) || pin != confirm
                    if (!invalid) done(pin)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(stringResource(R.string.finish_setup)) }
        }
    }
}

internal data class SetupFields(val password: String = "", val confirm: String = "", val question: String = "", val answer: String = "") {
    fun valid() = PasswordPolicy.isValidVaultPassword(password) && password == confirm && question.isNotBlank() && answer.isNotBlank()
}

@Composable
internal fun SecretField(
    value: String,
    label: Int,
    keyboardType: KeyboardType = KeyboardType.Password,
    update: (String) -> Unit,
) {
    OutlinedTextField(
        value,
        update,
        label = { Text(stringResource(label)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun CalculatorScreen(
    unlock: UnlockCoordinator,
    manualInput: CalculatorInputPreferences,
    ui: CalculatorUiState,
    welcome: CalculatorWelcomePreferences,
    opened: () -> Unit,
    recovery: () -> Unit,
    biometric: () -> Unit,
) {
    var state by remember { mutableStateOf(ui.state) }
    var inputMode by remember { mutableStateOf(ui.inputMode) }
    var showHistory by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(welcome.shouldShowWelcome()) }
    var historyVersion by remember { mutableStateOf(0) }
    val history = remember(historyVersion, state) { ui.history }
    val compact = LocalConfiguration.current.screenHeightDp < 720
    fun sync() {
        state = ui.state
        inputMode = ui.inputMode
    }
    fun update(action: () -> Unit) {
        action()
        sync()
    }
    fun resetManual() = update { ui.resetManualEntry() }
    fun submit() {
        when {
            state.expression == PasswordPolicy.RECOVERY_CODE -> {
                resetManual()
                recovery()
            }
            manualInput.verifyManualEntryPin(state.expression) -> update { ui.unlockManualEntry() }
            ui.manualEntryActive -> {
                if (unlock.unlock(state.expression)) {
                    resetManual()
                    opened()
                } else {
                    update {
                        ui.clear()
                        ui.resetManualEntry()
                    }
                }
            }
            else -> update { ui.evaluate() }
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF11120F),
                        Color(0xFF1D1B16),
                        Color(0xFF2B241A),
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE8E2D5),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showHistory = true }) {
                Icon(Icons.Default.History, contentDescription = stringResource(R.string.history), tint = Color(0xFFE3AA5F))
            }
        }
        Spacer(Modifier.weight(if (compact) 0.12f else 0.35f))
        CalculatorDisplay(
            state = state,
            previous = history.lastOrNull(),
            mode = inputMode,
            compact = compact,
            updateManualSecret = { update { ui.setManualSecret(it) } },
            activateManualInput = { update { ui.beginManualTextEntry() } },
        )
        Spacer(Modifier.weight(if (compact) 0.08f else 0.25f))
        FormulaPad(compact = compact) { token ->
            update {
                when (token) {
                    "x²" -> ui.append("^2")
                    "√" -> ui.append("sqrt(")
                    "π" -> ui.append("pi")
                    else -> ui.append("$token(")
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf("C", "⌫", "%", "(", ")").forEach { key ->
                CalculatorKey(
                    key,
                    onClick = {
                        update {
                            when (key) {
                                "C" -> ui.clear()
                                "⌫" -> ui.backspace()
                                else -> ui.append(key)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    type = KeyType.Service,
                    compact = compact,
                )
            }
        }
        val keys = listOf(
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "-"),
            listOf("±", "0", ".", "+"),
        )
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { key ->
                    CalculatorKey(
                        key,
                        onClick = {
                            update {
                                when (key) {
                                    "±" -> ui.toggleSign()
                                    else -> ui.append(key)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        type = if (key in setOf("÷", "×", "-", "+")) KeyType.Operator else KeyType.Number,
                        compact = compact,
                    )
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(if (compact) 56.dp else 64.dp).shadow(12.dp, RoundedCornerShape(32.dp), clip = false)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFFB35B), Color(0xFFFF7A1A), Color(0xFFE9581A))),
                    RoundedCornerShape(32.dp),
                )
                .combinedClickable(onClick = ::submit, onLongClick = biometric),
            contentAlignment = Alignment.Center,
        ) {
            Text("=", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Text(stringResource(R.string.history), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            if (history.isEmpty()) {
                Text(stringResource(R.string.empty_history), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
            } else {
                LazyColumn {
                    items(history.asReversed()) { entry ->
                        Text(
                            entry,
                            modifier = Modifier.fillMaxWidth().clickable {
                                update { ui.useHistoryEntry(entry) }
                                showHistory = false
                            }.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
            }
            TextButton(
                onClick = { ui.clearHistory(); historyVersion++ },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) { Text(stringResource(R.string.clear_history)) }
        }
    }
    if (showWelcome) {
        CalculatorWelcomeDialog {
            welcome.markWelcomeSeen()
            showWelcome = false
        }
    }
}

@Composable
private fun CalculatorWelcomeDialog(close: () -> Unit) {
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(R.string.calculator_welcome_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.calculator_welcome_intro))
                Text(stringResource(R.string.calculator_welcome_science))
                Text(stringResource(R.string.calculator_welcome_vault))
                Text(stringResource(R.string.calculator_welcome_recovery))
            }
        },
        confirmButton = {
            Button(onClick = close) {
                Text(stringResource(R.string.calculator_welcome_action))
            }
        },
    )
}

@Composable
private fun FormulaPad(compact: Boolean, append: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171713)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF3A3328)),
    ) {
        if (compact) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("sin", "cos", "tan", "√", "x²", "π").forEach { label ->
                    FormulaKey(label, append, Modifier.weight(1f), compact = true)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sin", "cos", "tan").forEach { label ->
                        FormulaKey(label, append, Modifier.weight(1f), compact = false)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("√", "x²", "π").forEach { label ->
                        FormulaKey(label, append, Modifier.weight(1f), compact = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulaKey(label: String, append: (String) -> Unit, modifier: Modifier = Modifier, compact: Boolean) {
    FilledTonalButton(
        onClick = { append(label) },
        modifier = modifier.height(if (compact) 34.dp else 42.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF2D2A23),
            contentColor = Color(0xFFFFC47D),
        ),
    ) {
        Text(label, fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun CalculatorDisplay(
    state: CalculatorEngine.State,
    previous: String?,
    mode: CalculatorInputMode,
    compact: Boolean,
    updateManualSecret: (String) -> Unit,
    activateManualInput: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val manualActive = mode == CalculatorInputMode.MANUAL_TEXT
    Surface(
        color = Color(0xFF10110F),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Color(0xFF3B352B)),
        modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 118.dp else 154.dp).animateContentSize(tween(180))
            .clickable(enabled = mode != CalculatorInputMode.BUTTONS) {
                activateManualInput()
            },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                previous.orEmpty(),
                modifier = Modifier.fillMaxWidth().height(24.dp),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF8F887A),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (manualActive) {
                BasicTextField(
                    value = state.expression,
                    onValueChange = updateManualSecret,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = TextStyle(
                        color = Color(0xFFF2EBDD),
                        fontSize = if (compact) 31.sp else 38.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { inner() }
                    },
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
            } else {
                Crossfade(targetState = state.display, animationSpec = tween(120), label = "calculator-display") { display ->
                    Text(
                        display,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().animateContentSize(tween(180)),
                        color = Color(0xFFF2EBDD),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorKey(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, type: KeyType, compact: Boolean) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(90),
        label = "calculator-key-scale",
    )
    val colors = when (type) {
        KeyType.Number -> ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF2A2B27),
            contentColor = Color(0xFFF4EFE5),
        )
        KeyType.Service -> ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF3A372F),
            contentColor = Color(0xFFD9D0C0),
        )
        KeyType.Operator -> ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF5A3820),
            contentColor = Color(0xFFFFC47D),
        )
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(if (compact) 52.dp else 62.dp).scale(scale).shadow(4.dp, RoundedCornerShape(32.dp), clip = false),
        shape = RoundedCornerShape(32.dp),
        colors = colors,
        elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        interactionSource = interactionSource,
    ) { Text(label, fontSize = if (compact) 18.sp else 21.sp, fontWeight = FontWeight.Medium) }
}

private enum class KeyType {
    Number,
    Service,
    Operator,
}
