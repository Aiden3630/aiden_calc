package com.aiden.calculator

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.FileProvider
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DateFormat
import java.util.Date
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@UnstableApi
class MainActivity : FragmentActivity() {
    private val container get() = (application as CalculatorApplication).container
    private var screen by mutableStateOf<Screen>(Screen.Calculator)
    private var firstSetup: SetupFields? = null
    private var secondSetup: SetupFields? = null
    private var pendingImportVault: VaultId? = null
    private var pendingImports: List<Uri> = emptyList()
    private var pendingExport: Pair<VaultItem, Uri>? = null
    private var pendingExportVault: VaultId? = null
    private var pendingExportItemId: String? = null
    private var pendingExportUri: Uri? = null
    private var pendingBatchExportVault: VaultId? = null
    private var pendingBatchExportItemIds: List<String> = emptyList()
    private var pendingBatchExportItems: List<VaultItem> = emptyList()
    private var pendingBatchExportUri: Uri? = null
    private var pendingSourceDeleteUris: List<Uri> = emptyList()
    private val pendingSourceDeleteQueue = ArrayDeque<List<Uri>>()
    private var importJob: Job? = null
    private var importProgress by mutableStateOf<ImportProgress?>(null)
    private var exportProgress by mutableStateOf<ExportProgress?>(null)
    private var storageMigrationReport by mutableStateOf<StorageMigrationReport?>(null)
    private var storageMigrationInProgress by mutableStateOf(false)
    private lateinit var importLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var sourceDeleteLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var exportLauncher: ActivityResultLauncher<String>
    private lateinit var batchExportDirectoryLauncher: ActivityResultLauncher<Uri?>
    private val pendingImportsStore by lazy { getSharedPreferences("pending_imports", Context.MODE_PRIVATE) }
    private val pendingExportStore by lazy { getSharedPreferences("pending_export", Context.MODE_PRIVATE) }
    private val pendingBatchExportStore by lazy { PendingBatchExportStore(this) }
    private val faceDownLock by lazy { FaceDownLockController(this, ::lock) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restorePendingImports()
        restorePendingExport()
        restorePendingBatchExport()
        importLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), ::handlePickedImports)
        sourceDeleteLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult(), ::handleSourceDeleteResult)
        exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*"), ::handleExportDestination)
        batchExportDirectoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree(), ::handleBatchExportDirectory)
        screen = when {
            !container.configs.isConfigured() -> Screen.SetupOne
            !container.calculatorInput.manualEntryConfigured -> Screen.SetupManualEntry
            else -> Screen.Calculator
        }
        setContent {
            val currentScreen = screen
            val screenshotsBlocked = container.privacy.screenshotsBlocked
            var forgotPasswordReminderVisible by remember { mutableStateOf(container.unlockPreferences.shouldShowForgotPasswordReminder()) }
            DisposableEffect(currentScreen, screenshotsBlocked) {
                if (currentScreen.isVaultContent() && screenshotsBlocked) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                onDispose { }
            }
            DisposableEffect(currentScreen, container.emergencyLock.enabled) {
                faceDownLock.updateListening(currentScreen.isVaultContent() && container.emergencyLock.enabled)
                onDispose { faceDownLock.updateListening(false) }
            }
            if (currentScreen.isVaultContent()) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(100)) },
                    label = "vault-media",
                ) { animatedScreen ->
                    AppTheme(animatedScreen.isVaultContent(), container.appearance.state.accent, container.appearance.state.themeMode) {
                        when (animatedScreen) {
                            Screen.Vault -> VaultScreen()
                            is Screen.Video -> VideoPlayerScreen(
                                this@MainActivity,
                                animatedScreen.itemId,
                                animatedScreen.videoIds,
                                container.repository,
                                container.previews,
                            ) { screen = Screen.Vault }
                            else -> Unit
                        }
                    }
                }
            } else AppTheme(currentScreen.isVaultContent(), container.appearance.state.accent, container.appearance.state.themeMode) {
                when (currentScreen) {
                    Screen.SetupOne -> SetupStepScreen(VaultId.ONE, 1) {
                        firstSetup = it
                        screen = Screen.SetupTwo
                    }
                    Screen.SetupTwo -> SetupStepScreen(VaultId.TWO, 2, firstSetup?.password) { second ->
                        secondSetup = second
                        screen = Screen.SetupManualEntry
                    }
                    Screen.SetupManualEntry -> ManualEntryPinSetupScreen { pin ->
                        if (container.calculatorInput.configureManualEntryPin(pin)) {
                            val first = firstSetup
                            val second = secondSetup
                            if (!container.configs.isConfigured() && first != null && second != null) {
                                container.configs.create(VaultId.ONE, first.password, first.question, first.answer)
                                container.configs.create(VaultId.TWO, second.password, second.question, second.answer)
                            }
                            firstSetup = null
                            secondSetup = null
                            screen = Screen.Calculator
                        }
                    }
                    Screen.Calculator -> CalculatorScreen(
                        container.unlock,
                        container.calculatorInput,
                        container.calculatorUi,
                        container.calculatorWelcome,
                        ::openVault,
                        { screen = Screen.Recovery },
                        biometric = {
                            if (container.biometricUnlock.configuredVaultId() != null) {
                                container.biometricUnlock.unlock(this) {
                                    if (it) {
                                        container.unlockPreferences.markSuccessfulUnlock()
                                        openVault()
                                    }
                                }
                            }
                        },
                    )
                    Screen.Recovery -> RecoveryScreen()
                    else -> Unit
                }
            }
            if (currentScreen == Screen.Calculator && forgotPasswordReminderVisible) {
                LaunchedEffect(Unit) {
                    container.unlockPreferences.markForgotPasswordReminderShown()
                }
                AlertDialog(
                    onDismissRequest = { forgotPasswordReminderVisible = false },
                    title = { Text(getString(R.string.forgot_password_reminder)) },
                    text = { Text(getString(R.string.forgot_password_reminder_message)) },
                    confirmButton = {
                        TextButton(onClick = { forgotPasswordReminderVisible = false }) {
                            Text(getString(R.string.close))
                        }
                    },
                )
            }
        }
    }
    override fun onStop() {
        super.onStop()
        if (screen.isVaultContent()) {
            val action = container.sessionCoordinator.peekSystemAction()
            if (action?.let(container.sessionCoordinator::isValid) != true) lock(clearSystemAction = action == null)
        }
    }

    override fun onResume() {
        super.onResume()
        container.sessionCoordinator.peekSystemAction()
        if (screen.isVaultContent() && container.session.vaultId == null) lock(clearSystemAction = false)
    }

    private fun lock(clearSystemAction: Boolean = true) {
        importJob?.cancel()
        importJob = null
        val cleanupReport = container.repository.cancelTemporaryImports().plus(container.temporaryExports.clear())
        container.wifiTransfer.stop()
        container.browserCleanup.cleanupOnLock(this)
        if (clearSystemAction) container.sessionCoordinator.clear() else container.session.clear()
        container.vaultNavigation.clearScrollPositions()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (!cleanupReport.clean) {
            Toast.makeText(this, getString(R.string.temporary_cleanup_failed, cleanupReport.failed.size), Toast.LENGTH_LONG).show()
        }
        screen = Screen.Calculator
    }

    private fun startSystemAction(type: SystemActionType): PendingSystemAction {
        return container.sessionCoordinator.beginSystemAction(type)
    }

    private fun openVault() {
        container.vaultNavigation.beginSession()
        container.calculatorUi.clear()
        screen = Screen.Vault
        val vaultId = container.session.vaultId
        if (pendingImportVault == vaultId && pendingImports.isNotEmpty()) {
            val uris = pendingImports
            clearPendingImports()
            importUris(uris)
        }
        pendingExportUri?.takeIf { pendingExportVault == vaultId }?.let { uri ->
            lifecycleScope.launch {
                val item = pendingExportItemId?.let { container.repository.currentItem(it) }
                clearPendingExport()
                if (item != null) exportTo(item, uri)
            }
        }
        pendingBatchExportUri?.takeIf { pendingBatchExportVault == vaultId }?.let { uri ->
            lifecycleScope.launch {
                val items = pendingBatchExportItems.ifEmpty { container.repository.currentItems(pendingBatchExportItemIds) }
                clearPendingBatchExport()
                if (items.isNotEmpty()) batchExportToDirectory(items, uri)
            }
        }
    }

    private fun handlePickedImports(uris: List<Uri>) {
        val action = container.sessionCoordinator.consumeSystemAction(SystemActionType.IMPORT) ?: return
        if (uris.isEmpty()) {
            clearPendingImports()
            return
        }
        if (container.sessionCoordinator.isValid(action) && container.session.vaultId == action.vaultId) {
            clearPendingImports()
            importUris(uris)
        } else {
            uris.take(PERSISTED_IMPORT_LIMIT).forEach { takePersistablePermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            pendingImportVault = action.vaultId
            pendingImports = uris.take(PERSISTED_IMPORT_LIMIT)
            savePendingImports()
            lock()
            Toast.makeText(this, R.string.unlock_to_continue_import, Toast.LENGTH_LONG).show()
        }
    }

    private fun importUris(uris: List<Uri>) {
        importJob = lifecycleScope.launch {
            var successes = 0
            var errors = 0
            var processed = 0
            var total = uris.size
            val recentErrors = ArrayDeque<String>()
            val importedUris = mutableListOf<Uri>()
            importProgress = ImportProgress(0, total, successes, errors, emptyList())
            uris.forEachIndexed { index, uri ->
                if (isArchiveUri(uri)) {
                    val baseSuccesses = successes
                    val baseErrors = errors
                    var archiveProcessed = 0
                    val archiveResult = runCatching {
                        ArchiveImportService(contentResolver, container.repository).importZip(uri) { archiveProgress ->
                            archiveProcessed = archiveProgress.processed
                            val remainingUris = uris.lastIndex - index
                            total = maxOf(total, processed + archiveProcessed + remainingUris)
                            successes = baseSuccesses + archiveProgress.successes
                            errors = baseErrors + archiveProgress.errors
                            if (archiveProcessed % IMPORT_PROGRESS_UPDATE_STEP == 0) {
                                withContext(Dispatchers.Main) {
                                    importProgress = ImportProgress(processed + archiveProcessed, total, successes, errors, recentErrors.toList())
                                }
                            }
                        }
                    }
                    archiveResult
                        .onSuccess { result ->
                            successes = baseSuccesses + result.successes
                            errors = baseErrors + result.errors
                            result.recentErrors.forEach { recentErrors += it }
                            while (recentErrors.size > IMPORT_ERROR_PREVIEW_LIMIT) recentErrors.removeFirst()
                            if (result.successes > 0 && result.errors == 0) importedUris += uri
                            processed += archiveProcessed.coerceAtLeast(1)
                        }.onFailure {
                            errors = baseErrors + 1
                            recentErrors += "${displayName(uri)}: ${it.message ?: getString(R.string.import_failed)}"
                            while (recentErrors.size > IMPORT_ERROR_PREVIEW_LIMIT) recentErrors.removeFirst()
                            processed += 1
                        }
                } else {
                    runCatching { container.repository.import(uri) }
                        .onSuccess {
                            successes++
                            importedUris += uri
                        }.onFailure {
                            errors++
                            recentErrors += "${uri.lastPathSegment ?: uri}: ${it.message ?: getString(R.string.import_failed)}"
                            while (recentErrors.size > IMPORT_ERROR_PREVIEW_LIMIT) recentErrors.removeFirst()
                        }
                    processed += 1
                }
                if (index == uris.lastIndex || processed % IMPORT_PROGRESS_UPDATE_STEP == 0) {
                    importProgress = ImportProgress(processed, total, successes, errors, recentErrors.toList())
                }
            }
            Toast.makeText(this@MainActivity, getString(R.string.import_result, successes, errors), Toast.LENGTH_LONG).show()
            deleteImportedSources(importedUris)
            uris.forEach { releasePersistablePermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    private fun isArchiveUri(uri: Uri): Boolean {
        val mime = contentResolver.getType(uri).orEmpty().lowercase()
        if (mime in ZIP_MIME_TYPES) return true
        return displayName(uri).endsWith(".zip", ignoreCase = true)
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0).orEmpty()
        }
        return uri.lastPathSegment.orEmpty()
    }

    private fun handleSourceDeleteResult(result: androidx.activity.result.ActivityResult) {
        val count = pendingSourceDeleteUris.size
        pendingSourceDeleteUris = emptyList()
        val message = if (result.resultCode == android.app.Activity.RESULT_OK) {
            getString(R.string.import_sources_delete_requested, count)
        } else {
            pendingSourceDeleteQueue.clear()
            getString(R.string.import_sources_delete_cancelled)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        if (result.resultCode == android.app.Activity.RESULT_OK) launchNextQueuedSourceDeleteRequest()
    }

    private fun deleteImportedSources(uris: List<Uri>) {
        val targets = uris.distinct().map { SourceDeleteTarget(it, mediaStoreDeleteUri(it)) }
        if (targets.isEmpty()) return

        val promptUris = mutableListOf<Uri>()
        var legacyPrompt: Pair<Uri, android.content.IntentSender>? = null
        var deleted = 0
        var failed = 0
        targets.forEach { target ->
            var queuedLegacyPrompt = false
            runCatching {
                contentResolver.delete(target.deleteUri, null, null)
            }.onSuccess { rows ->
                if (rows > 0) {
                    deleted++
                    return@forEach
                }
            }.onFailure { error ->
                if (
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                    error is RecoverableSecurityException &&
                    legacyPrompt == null
                ) {
                    legacyPrompt = target.deleteUri to error.userAction.actionIntent.intentSender
                    queuedLegacyPrompt = true
                } else {
                    Log.w(LOG_TAG, "Source delete needs prompt or failed for ${target.deleteUri}", error)
                }
            }
            if (queuedLegacyPrompt) return@forEach
            if (deleteDocumentUri(target.sourceUri)) {
                deleted++
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && target.deleteUri.authority == MediaStore.AUTHORITY) {
                promptUris += target.deleteUri
            } else {
                failed++
            }
        }

        val uniquePromptUris = promptUris.distinct()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uniquePromptUris.isNotEmpty()) {
            enqueueSourceDeleteRequests(uniquePromptUris)
        } else if (legacyPrompt != null) {
            val (uri, sender) = legacyPrompt
            launchSourceDeleteRequest(listOf(uri), sender)
        } else if (failed > 0 || promptUris.isNotEmpty()) {
            if (deleted == 0) {
                Toast.makeText(this, R.string.import_sources_delete_failed, Toast.LENGTH_LONG).show()
            }
        } else if (deleted > 0) {
            Toast.makeText(this, getString(R.string.import_sources_deleted, deleted), Toast.LENGTH_LONG).show()
        }
    }

    private fun enqueueSourceDeleteRequests(uris: List<Uri>) {
        pendingSourceDeleteQueue.clear()
        uris.chunked(SOURCE_DELETE_CHUNK_SIZE).forEach { pendingSourceDeleteQueue += it }
        launchNextQueuedSourceDeleteRequest()
    }

    private fun launchNextQueuedSourceDeleteRequest() {
        if (pendingSourceDeleteUris.isNotEmpty() || pendingSourceDeleteQueue.isEmpty()) return
        val next = pendingSourceDeleteQueue.removeFirst()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        runCatching {
            MediaStore.createDeleteRequest(contentResolver, next).intentSender
        }.onSuccess { sender ->
            launchSourceDeleteRequest(next, sender)
        }.onFailure {
            Log.w(LOG_TAG, "Could not create MediaStore delete request for ${next.size} source files", it)
            Toast.makeText(this, R.string.import_sources_delete_failed, Toast.LENGTH_LONG).show()
            pendingSourceDeleteQueue.clear()
        }
    }

    private fun launchSourceDeleteRequest(uris: List<Uri>, sender: android.content.IntentSender) {
        pendingSourceDeleteUris = uris
        sourceDeleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    private fun mediaStoreDeleteUri(uri: Uri): Uri {
        if (uri.authority == MediaStore.AUTHORITY) return uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { MediaStore.getMediaUri(this, uri) }.getOrNull()?.let { mediaUri ->
                if (mediaUri.authority == MediaStore.AUTHORITY) return mediaUri
            }
        }
        if (!DocumentsContract.isDocumentUri(this, uri)) return uri
        if (uri.authority != "com.android.providers.media.documents") return uri
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return uri
        val parts = documentId.split(':', limit = 2)
        if (parts.size != 2) return uri
        val id = parts[1].toLongOrNull() ?: return uri
        val collection = when (parts[0]) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> return uri
        }
        return ContentUris.withAppendedId(collection, id)
    }

    private fun deleteDocumentUri(uri: Uri): Boolean {
        if (!DocumentsContract.isDocumentUri(this, uri)) return false
        return runCatching {
            DocumentsContract.deleteDocument(contentResolver, uri)
        }.onFailure {
            Log.w(LOG_TAG, "Could not delete document source $uri", it)
        }.getOrDefault(false)
    }

    private fun handleExportDestination(uri: Uri?) {
        val action = container.sessionCoordinator.consumeSystemAction(SystemActionType.EXPORT) ?: return
        if (uri != null) takePersistablePermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (uri == null) {
            clearPendingExport()
            return
        }
        lifecycleScope.launch {
            val item = pendingExport?.first ?: if (container.session.vaultId == action.vaultId) {
                pendingExportItemId?.let { container.repository.currentItem(it) }
            } else null
            if (container.sessionCoordinator.isValid(action) && container.session.vaultId == action.vaultId && item != null) {
                clearPendingExport()
                exportTo(item, uri)
            } else {
                pendingExport = item?.let { it to uri }
                pendingExportVault = action.vaultId
                pendingExportUri = uri
                savePendingExport()
                lock()
                Toast.makeText(this@MainActivity, R.string.unlock_to_continue_export, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleBatchExportDirectory(uri: Uri?) {
        val action = container.sessionCoordinator.consumeSystemAction(SystemActionType.EXPORT) ?: return
        if (uri != null) takePersistablePermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (uri == null) {
            clearPendingBatchExport()
            return
        }
        lifecycleScope.launch {
            val items = if (container.session.vaultId == action.vaultId) {
                pendingBatchExportItems.ifEmpty { container.repository.currentItems(pendingBatchExportItemIds) }
            } else {
                emptyList()
            }
            if (container.sessionCoordinator.isValid(action) && container.session.vaultId == action.vaultId && items.isNotEmpty()) {
                clearPendingBatchExport()
                batchExportToDirectory(items, uri)
            } else {
                pendingBatchExportVault = action.vaultId
                pendingBatchExportUri = uri
                savePendingBatchExport()
                lock()
                Toast.makeText(this@MainActivity, R.string.unlock_to_continue_export, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun launchExport(item: VaultItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (item.type == VaultItemType.PHOTO || item.type == VaultItemType.VIDEO)) {
            exportMediaToGallery(item)
            return
        }
        pendingExport = item to Uri.EMPTY
        pendingExportVault = item.vaultId
        pendingExportItemId = item.id
        pendingExportUri = null
        savePendingExport()
        startSystemAction(SystemActionType.EXPORT)
        exportLauncher.launch(item.id)
    }

    private fun launchBatchExport(items: List<VaultItem>) {
        pendingBatchExportVault = requireNotNull(container.session.vaultId)
        pendingBatchExportItems = items
        pendingBatchExportItemIds = items.map { it.id }
        pendingBatchExportUri = null
        if (items.size <= PENDING_BATCH_EXPORT_SAVE_LIMIT) {
            savePendingBatchExport()
        } else {
            pendingBatchExportStore.clear()
        }
        startSystemAction(SystemActionType.EXPORT)
        batchExportDirectoryLauncher.launch(null)
    }

    private fun exportTo(item: VaultItem, uri: Uri) {
        lifecycleScope.launch {
            val exported = runCatching {
                requireNotNull(contentResolver.openOutputStream(uri)).use { container.repository.export(item, it) }
            }.isSuccess
            releasePersistablePermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            Toast.makeText(
                this@MainActivity,
                if (exported) R.string.export_complete else R.string.export_retry_destination,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun exportMediaToGallery(item: VaultItem) {
        lifecycleScope.launch {
            val result = runCatching {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) error("MediaStore export requires Android 10+")
                val displayName = container.repository.displayName(item).ifBlank { item.id }
                val mime = container.repository.mime(item)
                val nowMillis = System.currentTimeMillis()
                val nowSeconds = nowMillis / 1000
                val collection = when (item.type) {
                    VaultItemType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    VaultItemType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    VaultItemType.FILE -> error("Only media items can be exported to gallery")
                }
                val relativePath = when (item.type) {
                    VaultItemType.PHOTO -> Environment.DIRECTORY_PICTURES + "/Aiden Calculator"
                    VaultItemType.VIDEO -> Environment.DIRECTORY_MOVIES + "/Aiden Calculator"
                    VaultItemType.FILE -> error("Only media items can be exported to gallery")
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.DATE_ADDED, nowSeconds)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, nowSeconds)
                    put(MediaStore.MediaColumns.DATE_TAKEN, nowMillis)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = requireNotNull(contentResolver.insert(collection, values)) { "Could not create gallery item" }
                try {
                    requireNotNull(contentResolver.openOutputStream(uri)).use { output ->
                        container.repository.export(item, output)
                    }
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                        put(MediaStore.MediaColumns.DATE_ADDED, nowSeconds)
                        put(MediaStore.MediaColumns.DATE_MODIFIED, nowSeconds)
                        put(MediaStore.MediaColumns.DATE_TAKEN, nowMillis)
                    }.also { contentResolver.update(uri, it, null, null) }
                    uri
                } catch (error: Throwable) {
                    runCatching { contentResolver.delete(uri, null, null) }
                    throw error
                }
            }
            Toast.makeText(
                this@MainActivity,
                if (result.isSuccess) R.string.export_gallery_complete else R.string.export_gallery_failed,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun batchExportToDirectory(items: List<VaultItem>, treeUri: Uri) {
        lifecycleScope.launch {
            container.diagnostics.event("activity.batchExport", "start items=${items.size} tree=$treeUri")
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            exportProgress = ExportProgress(0, items.size, 0, 0)
            val result = try {
                val exportKey = container.session.requireKey()
                container.diagnostics.event("activity.batchExport", "keySnapshot acquired bytes=${exportKey.size}")
                try {
                    runCatching {
                        val parts = planBatchExportParts(items)
                        var processedBeforePart = 0
                        var successCount = 0
                        var errorCount = 0
                        parts.forEachIndexed { partIndex, part ->
                            val partUri = createBatchExportPartUri(treeUri, partIndex + 1, parts.size)
                            container.diagnostics.event(
                                "activity.batchExport",
                                "partStart index=${partIndex + 1}/${parts.size} items=${part.items.size} uri=$partUri estimatedBytes=${part.estimatedBytes}",
                            )
                            try {
                                requireNotNull(contentResolver.openOutputStream(partUri)).use { output ->
                                    val partResult = BatchExportService(container.repository, cacheDir, container.diagnostics).exportZip(part.items, output, exportKey) { progress ->
                                        val processed = processedBeforePart + progress.processed
                                        container.diagnostics.event(
                                            "activity.batchExport",
                                            "progress processed=$processed total=${items.size} successes=${successCount + progress.successes} errors=${errorCount + progress.errors}",
                                        )
                                        if (processed == items.size || processed % EXPORT_PROGRESS_UPDATE_STEP == 0) {
                                            withContext(Dispatchers.Main) {
                                                exportProgress = ExportProgress(
                                                    processed = processed,
                                                    total = items.size,
                                                    successes = successCount + progress.successes,
                                                    errors = errorCount + progress.errors,
                                                )
                                            }
                                        }
                                    }
                                    successCount += partResult.successCount
                                    errorCount += partResult.errorCount
                                }
                                container.diagnostics.event("activity.batchExport", "partSuccess index=${partIndex + 1}/${parts.size} uri=$partUri")
                            } catch (error: Throwable) {
                                runCatching { DocumentsContract.deleteDocument(contentResolver, partUri) }
                                throw error
                            }
                            processedBeforePart += part.items.size
                        }
                        BatchTransferResult(emptyList(), emptyList(), successCount, errorCount)
                    }
                } finally {
                    exportKey.fill(0)
                }
            } finally {
                releasePersistablePermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            result
                .onSuccess {
                    container.diagnostics.event("activity.batchExport", "success successCount=${it.successCount} errorCount=${it.errorCount}")
                    exportProgress = ExportProgress(items.size, items.size, it.successCount, it.errorCount)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.export_result, it.successCount, it.errorCount),
                        Toast.LENGTH_LONG,
                    ).show()
                }
                .onFailure {
                    container.diagnostics.event("activity.batchExport", "failed items=${items.size}", it)
                    exportProgress = null
                    Toast.makeText(this@MainActivity, R.string.export_retry_destination, Toast.LENGTH_LONG).show()
                }
        }
    }

    private suspend fun planBatchExportParts(items: List<VaultItem>): List<BatchExportPart> = withContext(Dispatchers.IO) {
        val parts = mutableListOf<BatchExportPart>()
        var currentItems = mutableListOf<VaultItem>()
        var currentBytes = 0L
        items.forEach { item ->
            val estimatedBytes = (item.plainSize ?: item.size).coerceAtLeast(1L) + ZIP_ENTRY_OVERHEAD_BYTES
            if (currentItems.isNotEmpty() && currentBytes + estimatedBytes > MAX_BATCH_EXPORT_ZIP_BYTES) {
                parts += BatchExportPart(currentItems, currentBytes)
                currentItems = mutableListOf()
                currentBytes = 0L
            }
            currentItems += item
            currentBytes += estimatedBytes
        }
        if (currentItems.isNotEmpty()) parts += BatchExportPart(currentItems, currentBytes)
        parts
    }

    private fun createBatchExportPartUri(treeUri: Uri, partNumber: Int, partCount: Int): Uri {
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val name = if (partCount == 1) {
            "aiden-vault-export.zip"
        } else {
            "aiden-vault-export-${partNumber.toString().padStart(3, '0')}.zip"
        }
        return requireNotNull(DocumentsContract.createDocument(contentResolver, parent, "application/zip", name)) {
            "Could not create export archive"
        }
    }

    private fun shareDiagnosticsLog() {
        val file = runCatching { container.diagnostics.exportFile() }
            .onFailure { container.diagnostics.event("activity.diagnostics", "exportLog failed", it) }
            .getOrNull() ?: return
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Aiden diagnostics log")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share_diagnostics_logs),
            ),
        )
    }

    private fun takePersistablePermission(uri: Uri, flags: Int) {
        runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
    }

    private fun releasePersistablePermission(uri: Uri, flags: Int) {
        runCatching { contentResolver.releasePersistableUriPermission(uri, flags) }
    }

    private fun savePendingImports() {
        pendingImportsStore.edit()
            .putString("vaultId", pendingImportVault?.name)
            .putStringSet("uris", pendingImports.map(Uri::toString).toSet())
            .apply()
    }

    private fun restorePendingImports() {
        pendingImportVault = runCatching {
            pendingImportsStore.getString("vaultId", null)?.let(VaultId::valueOf)
        }.getOrNull()
        pendingImports = pendingImportsStore.getStringSet("uris", emptySet()).orEmpty().map(Uri::parse)
    }

    private fun clearPendingImports() {
        pendingImportVault = null
        pendingImports = emptyList()
        pendingImportsStore.edit().clear().apply()
    }

    private fun savePendingExport() {
        pendingExportStore.edit()
            .putString("vaultId", pendingExportVault?.name)
            .putString("itemId", pendingExportItemId)
            .putString("uri", pendingExportUri?.toString())
            .apply()
    }

    private fun restorePendingExport() {
        pendingExportVault = runCatching {
            pendingExportStore.getString("vaultId", null)?.let(VaultId::valueOf)
        }.getOrNull()
        pendingExportItemId = pendingExportStore.getString("itemId", null)
        pendingExportUri = pendingExportStore.getString("uri", null)?.let(Uri::parse)
    }

    private fun clearPendingExport() {
        pendingExport = null
        pendingExportVault = null
        pendingExportItemId = null
        pendingExportUri = null
        pendingExportStore.edit().clear().apply()
    }

    private fun savePendingBatchExport() {
        pendingBatchExportStore.save(
            vaultId = pendingBatchExportVault,
            itemIds = pendingBatchExportItemIds,
            uri = pendingBatchExportUri,
        )
    }

    private fun restorePendingBatchExport() {
        val state = pendingBatchExportStore.restore()
        pendingBatchExportVault = state?.vaultId
        pendingBatchExportItemIds = state?.itemIds?.toList().orEmpty()
        pendingBatchExportUri = state?.uri
    }

    private fun clearPendingBatchExport() {
        pendingBatchExportVault = null
        pendingBatchExportItemIds = emptyList()
        pendingBatchExportItems = emptyList()
        pendingBatchExportUri = null
        pendingBatchExportStore.clear()
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    private fun VaultScreen() {
        val vaultId = requireNotNull(container.session.vaultId)
        var tab by remember(vaultId) { mutableStateOf(container.vaultNavigation.lastContentTab(vaultId).toVaultTab()) }
        var nestedScreen by remember(vaultId) { mutableStateOf<VaultNestedScreen?>(null) }
        val items by container.repository.observeCurrent().collectAsState(emptyList())
        val photos = items.filter { it.type == VaultItemType.PHOTO && it.trashState == TrashState.ACTIVE }
        val appearance = container.appearance.state
        var selected by remember { mutableStateOf(setOf<String>()) }
        var previewItem by remember { mutableStateOf<VaultItem?>(null) }
        var changePassword by remember { mutableStateOf(false) }
        var changeDecoyPassword by remember { mutableStateOf(false) }
        var accentPicker by remember { mutableStateOf(false) }
        var albumLayoutPicker by remember { mutableStateOf(false) }
        var themeModePicker by remember { mutableStateOf(false) }
        var albumViewModePicker by remember { mutableStateOf(false) }
        var iconPicker by remember { mutableStateOf(false) }
        var reminderPicker by remember { mutableStateOf(false) }
        var biometricVersion by remember { mutableStateOf(0) }
        var repairReport by remember { mutableStateOf<RepairReport?>(null) }

        Scaffold(
            topBar = { VaultTopBar(vaultId) },
            bottomBar = { VaultBottomNavigation(tab) {
                tab = it
                selected = emptySet()
                it.contentTab?.let { content -> container.vaultNavigation.select(vaultId, content) }
            } },
            floatingActionButton = {
                if (tab.contentTab != null) {
                    FloatingActionButton(onClick = {
                        pendingImportVault = container.session.vaultId
                        startSystemAction(SystemActionType.IMPORT)
                        importLauncher.launch(importMimeTypes(tab))
                    }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.import_files)) }
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                transitionSpec = { vaultTabTransition(initialState, targetState) },
                label = "vault-tabs",
            ) { animatedTab ->
            if (animatedTab == VaultTab.TOOLS) {
                ToolsScreen(
                    modifier = Modifier.padding(padding),
                    browser = { nestedScreen = VaultNestedScreen.BROWSER },
                    cloud = { nestedScreen = VaultNestedScreen.CLOUD },
                    accent = { accentPicker = true },
                    icons = { iconPicker = true },
                    decoy = { nestedScreen = VaultNestedScreen.DECOY },
                    trash = { nestedScreen = VaultNestedScreen.TRASH },
                )
                return@AnimatedContent
            }
            if (animatedTab == VaultTab.SETTINGS) {
                SettingsScreen(
                    modifier = Modifier.padding(padding),
                    screenshotsBlocked = container.privacy.screenshotsBlocked,
                    biometricEnabled = remember(biometricVersion) { container.biometricUnlock.configuredVaultId() == vaultId },
                    biometricAvailable = container.biometricUnlock.isAvailable(this@MainActivity),
                    emergencyLockEnabled = container.emergencyLock.enabled,
                    emergencyLockSupported = faceDownLock.isSupported(),
                    accent = appearance.accent,
                    albumColumns = appearance.albumColumns,
                    themeMode = appearance.themeMode,
                    albumViewMode = appearance.albumViewMode,
                    launcherIcon = container.launcherIcons.current(),
                    language = container.locale.language,
                    reminderDays = container.unlockPreferences.reminderDays,
                    decoyHintsEnabled = container.decoyPreferences.hintsEnabled,
                    toggleBiometric = { enabled ->
                        if (enabled) {
                            val key = container.session.requireKey()
                            container.biometricUnlock.enable(this@MainActivity, vaultId, key) {
                                key.fill(0)
                                biometricVersion++
                            }
                        } else {
                            container.biometricUnlock.disable()
                            biometricVersion++
                        }
                    },
                    toggleEmergencyLock = container.emergencyLock::updateEnabled,
                    toggleScreenshots = container.privacy::updateScreenshotsBlocked,
                    toggleDecoyHints = container.decoyPreferences::updateHintsEnabled,
                    changePassword = { changePassword = true },
                    recovery = { nestedScreen = VaultNestedScreen.RECOVERY },
                    decoy = { nestedScreen = VaultNestedScreen.DECOY },
                    privacyPolicy = { nestedScreen = VaultNestedScreen.PRIVACY_POLICY },
                    forgotPasswordReminder = { reminderPicker = true },
                    lockNow = ::lock,
                    chooseAccent = { accentPicker = true },
                    chooseThemeMode = { themeModePicker = true },
                    chooseColumns = { albumLayoutPicker = true },
                    chooseAlbumViewMode = { albumViewModePicker = true },
                    chooseIcon = { iconPicker = true },
                    chooseLanguage = { nestedScreen = VaultNestedScreen.LANGUAGE },
                    cloud = { nestedScreen = VaultNestedScreen.CLOUD },
                    wifiTransfer = { nestedScreen = VaultNestedScreen.WIFI_TRANSFER },
                    fileTransfer = { nestedScreen = VaultNestedScreen.FILE_TRANSFER },
                    share = { nestedScreen = VaultNestedScreen.SHARE },
                    trash = { nestedScreen = VaultNestedScreen.TRASH },
                    repair = { nestedScreen = VaultNestedScreen.REPAIR },
                    about = { nestedScreen = VaultNestedScreen.ABOUT },
                )
                return@AnimatedContent
            }
            val all = items.filter { item ->
                when (animatedTab) {
                    VaultTab.PHOTOS -> item.type == VaultItemType.PHOTO && item.trashState == TrashState.ACTIVE
                    VaultTab.VIDEOS -> item.type == VaultItemType.VIDEO && item.trashState == TrashState.ACTIVE
                    VaultTab.FILES -> item.type == VaultItemType.FILE && item.trashState == TrashState.ACTIVE
                    VaultTab.TOOLS, VaultTab.SETTINGS -> false
                }
            }
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (all.isNotEmpty()) {
                        val allIds = all.map { it.id }.toSet()
                        val allSelected = allIds.isNotEmpty() && allIds.all { it in selected }
                        FilledTonalButton(onClick = {
                            selected = if (allSelected) emptySet() else allIds
                        }) {
                            Text(getString(if (allSelected) R.string.clear_selection else R.string.select_all))
                        }
                    }
                    if (selected.isNotEmpty()) {
                        FilledTonalButton(onClick = {
                            val chosen = all.filter { it.id in selected }
                            when (chosen.size) {
                                0 -> Toast.makeText(this@MainActivity, R.string.select_files_first, Toast.LENGTH_LONG).show()
                                1 -> launchExport(chosen.single())
                                else -> launchBatchExport(chosen)
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("${getString(R.string.export)} (${selected.size})")
                        }
                        FilledTonalButton(onClick = {
                            lifecycleScope.launch {
                                val ids = selected.toList()
                                container.diagnostics.event("activity.trash", "start selected=${ids.size} tab=$animatedTab")
                                runCatching {
                                    container.repository.trash(ids)
                                }.onSuccess {
                                    container.diagnostics.event("activity.trash", "success selected=${ids.size}")
                                    selected = emptySet()
                                }.onFailure {
                                    container.diagnostics.event("activity.trash", "failed selected=${ids.size}", it)
                                    Toast.makeText(this@MainActivity, R.string.delete_selected_failed, Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("${getString(R.string.delete)} (${selected.size})")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (all.isEmpty()) {
                    EmptyState(animatedTab)
                } else if (animatedTab == VaultTab.PHOTOS || animatedTab == VaultTab.VIDEOS) {
                    val gridState = rememberGridState(vaultId, requireNotNull(animatedTab.contentTab))
                    if (appearance.albumViewMode == AlbumViewMode.LIST) {
                        val listState = rememberListState(vaultId, requireNotNull(animatedTab.contentTab))
                        Box(Modifier.weight(1f)) {
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
                            ) {
                                listItems(all, key = { it.id }) { item ->
                                    val marked = item.id in selected
                                    MediaListRow(
                                        item = item,
                                        marked = marked,
                                        selecting = selected.isNotEmpty(),
                                        repository = container.repository,
                                        itemMenu = { ItemMenu(it) },
                                        open = {
                                            if (item.type == VaultItemType.VIDEO) {
                                                screen = Screen.Video(item.id, all.map { it.id })
                                            }
                                            else previewItem = item
                                        },
                                        toggle = { selected = if (marked) selected - item.id else selected + item.id },
                                    )
                                }
                            }
                            VaultScrollSlider(
                                totalItems = listState.layoutInfo.totalItemsCount,
                                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                                scrollToItem = { index -> listState.scrollToItem(index) },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        val columns = if (appearance.albumViewMode == AlbumViewMode.COMPACT_GRID) appearance.albumColumns + 1 else appearance.albumColumns
                        Box(Modifier.weight(1f)) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                state = gridState,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
                            ) {
                                gridItems(all, key = { it.id }) { item ->
                                    val marked = item.id in selected
                                    MediaCard(
                                        item = item,
                                        marked = marked,
                                        selecting = selected.isNotEmpty(),
                                        repository = container.repository,
                                        itemMenu = { ItemMenu(it) },
                                        open = {
                                            if (item.type == VaultItemType.VIDEO) {
                                                screen = Screen.Video(item.id, all.map { it.id })
                                            }
                                            else previewItem = item
                                        },
                                        toggle = { selected = if (marked) selected - item.id else selected + item.id },
                                    )
                                }
                            }
                            VaultScrollSlider(
                                totalItems = gridState.layoutInfo.totalItemsCount,
                                firstVisibleItemIndex = gridState.firstVisibleItemIndex,
                                scrollToItem = { index -> gridState.scrollToItem(index) },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 8.dp),
                            )
                        }
                    }
                } else {
                    val listState = rememberListState(vaultId, VaultContentTab.FILES)
                    Box(Modifier.weight(1f)) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
                        ) {
                            listItems(all, key = { it.id }) { item ->
                                val marked = item.id in selected
                                Card(
                                    Modifier.fillMaxWidth().padding(vertical = 5.dp).combinedClickable(
                                        onClick = {
                                            if (selected.isNotEmpty()) {
                                                selected = if (marked) selected - item.id else selected + item.id
                                            } else {
                                                previewItem = item
                                            }
                                        },
                                        onLongClick = { selected = if (marked) selected - item.id else selected + item.id },
                                    ),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (marked) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        ItemName(item, container.repository)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${item.type.name.lowercase()} - ", style = MaterialTheme.typography.labelMedium)
                                            ItemSize(item, container.repository, marked, MaterialTheme.typography.labelMedium)
                                            Spacer(Modifier.weight(1f))
                                            ItemMenu(item)
                                        }
                                    }
                                }
                            }
                        }
                        VaultScrollSlider(
                            totalItems = listState.layoutInfo.totalItemsCount,
                            firstVisibleItemIndex = listState.firstVisibleItemIndex,
                            scrollToItem = { index -> listState.scrollToItem(index) },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            }
        }
        AnimatedContent(
            targetState = nestedScreen,
            transitionSpec = {
                (slideInHorizontally(tween(160)) { it / 8 } + fadeIn(tween(160))) togetherWith
                    (slideOutHorizontally(tween(120)) { it / 8 } + fadeOut(tween(120)))
            },
            label = "vault-nested-screen",
        ) { nested ->
            when (nested) {
                VaultNestedScreen.TRASH -> TrashScreen(
                    items = items.filter { it.trashState == TrashState.TRASHED },
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.trash)) { nestedScreen = null } },
                    restore = { ids ->
                        lifecycleScope.launch {
                            container.diagnostics.event("activity.trashRestore", "start ids=${ids.size}")
                            runCatching { container.repository.restore(ids) }
                                .onSuccess { container.diagnostics.event("activity.trashRestore", "success ids=${ids.size}") }
                                .onFailure {
                                    container.diagnostics.event("activity.trashRestore", "failed ids=${ids.size}", it)
                                    Toast.makeText(this@MainActivity, R.string.delete_selected_failed, Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    deleteForever = { ids ->
                        lifecycleScope.launch {
                            container.diagnostics.event("activity.deleteForever", "start ids=${ids.size}")
                            runCatching { container.repository.deleteForever(ids) }
                                .onSuccess { container.diagnostics.event("activity.deleteForever", "success ids=${ids.size}") }
                                .onFailure {
                                    container.diagnostics.event("activity.deleteForever", "failed ids=${ids.size}", it)
                                    Toast.makeText(this@MainActivity, R.string.delete_selected_failed, Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    itemName = { ItemName(it, container.repository) },
                    itemSize = { ItemSize(it, container.repository, false, MaterialTheme.typography.labelMedium) },
                )
                VaultNestedScreen.REPAIR -> {
                    RepairReportScreen(
                        report = repairReport,
                        topBar = { VaultTopBar(vaultId, stringResource(R.string.find_lost_file)) { nestedScreen = null } },
                        scan = { lifecycleScope.launch { repairReport = container.repair.scan() } },
                        removeMissing = { missing ->
                            lifecycleScope.launch {
                                container.repair.removeMissingBlob(missing.item.id)
                                repairReport = container.repair.scan()
                            }
                        },
                        recover = { orphan ->
                            lifecycleScope.launch {
                                container.repair.recoverOrphan(orphan)
                                repairReport = container.repair.scan()
                            }
                        },
                        removeTemporary = { temporary ->
                            container.repair.removeTemporary(temporary.blobName)
                            lifecycleScope.launch { repairReport = container.repair.scan() }
                        },
                    )
                    LaunchedEffect(Unit) { repairReport = container.repair.scan() }
                }
                VaultNestedScreen.RECOVERY -> RecoveryMethodsScreen(
                    vaultId = vaultId,
                    question = container.configs.get(vaultId).recoveryQuestion,
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.recovery_methods)) { nestedScreen = null } },
                )
                VaultNestedScreen.DECOY -> DecoyVaultScreen(
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.decoy_vault)) { nestedScreen = null } },
                    hintsEnabled = container.decoyPreferences.hintsEnabled,
                    toggleHints = container.decoyPreferences::updateHintsEnabled,
                    changePassword = { changeDecoyPassword = true },
                )
                VaultNestedScreen.PRIVACY_POLICY -> PrivacyPolicyScreen(
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.privacy_policy)) { nestedScreen = null } },
                )
                VaultNestedScreen.BROWSER -> BrowserScreen(
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.personal_browser)) { nestedScreen = null } },
                    preferences = container.browserPreferences,
                )
                VaultNestedScreen.CLOUD -> CloudScreen(
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.cloud_file_protection)) { nestedScreen = null } },
                    controller = container.cloudSync,
                    credentials = container.cloudCredentials,
                    items = items.filter { it.trashState == TrashState.ACTIVE },
                )
                VaultNestedScreen.WIFI_TRANSFER -> WifiTransferScreen(
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.wifi_transfer)) { nestedScreen = null } },
                    items = items.filter { it.trashState == TrashState.ACTIVE },
                )
                VaultNestedScreen.FILE_TRANSFER -> FileTransferScreen(
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.file_transfer)) { nestedScreen = null } },
                    items = items.filter { it.trashState == TrashState.ACTIVE },
                )
                VaultNestedScreen.SHARE -> {
                    LaunchedEffect(Unit) {
                        startActivity(Intent.createChooser(ShareIntentFactory.create(getString(R.string.share_app_text)), getString(R.string.share_with_friends)))
                        nestedScreen = null
                    }
                }
                VaultNestedScreen.LANGUAGE -> LanguageScreen(
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.change_language)) { nestedScreen = null } },
                    selected = container.locale.language,
                    choose = {
                        container.locale.setLanguage(it)
                        recreate()
                    },
                )
                VaultNestedScreen.ABOUT -> AboutScreen(
                    topBar = { VaultTopBar(vaultId, stringResource(R.string.about_app)) { nestedScreen = null } },
                    cleanupReport = container.startupCleanupReport,
                    shareDiagnostics = ::shareDiagnosticsLog,
                    legacyStorageAvailable = container.storage.hasLegacyStorage(),
                    storageMigrationReport = storageMigrationReport,
                    storageMigrationInProgress = storageMigrationInProgress,
                    migrateStorage = {
                        if (!storageMigrationInProgress) {
                            storageMigrationInProgress = true
                            lifecycleScope.launch {
                                runCatching { container.storage.migrateLegacyToPrivate() }
                                    .onSuccess {
                                        storageMigrationReport = it
                                        Toast.makeText(this@MainActivity, R.string.storage_migration_complete, Toast.LENGTH_LONG).show()
                                    }
                                    .onFailure {
                                        storageMigrationReport = StorageMigrationReport(failed = listOf(it.message ?: it::class.simpleName.orEmpty()))
                                        Toast.makeText(this@MainActivity, R.string.storage_migration_failed, Toast.LENGTH_LONG).show()
                                    }
                                storageMigrationInProgress = false
                            }
                        }
                    },
                )
                null -> Unit
            }
        }
        previewItem?.let { item ->
            when (item.type) {
                VaultItemType.PHOTO -> PhotoViewerDialog(photos, item.id, container.repository, container.previews) { previewItem = null }
                VaultItemType.VIDEO -> Unit
                VaultItemType.FILE -> ExternalOpenDialog(item) { previewItem = null }
            }
        }
        importProgress?.let { ImportProgressDialog(it) { importProgress = null } }
        exportProgress?.let { ExportProgressDialog(it) { exportProgress = null } }
        if (changePassword) ChangePasswordDialog(vaultId) { changePassword = false }
        if (changeDecoyPassword) ChangePasswordDialog(VaultId.TWO) { changeDecoyPassword = false }
        if (accentPicker) AccentPickerSheet(appearance.accent, {
            container.appearance.setAccent(it)
            accentPicker = false
        }) { accentPicker = false }
        if (albumLayoutPicker) AlbumLayoutSheet(appearance.albumColumns, {
            container.appearance.setAlbumColumns(it)
            albumLayoutPicker = false
        }) { albumLayoutPicker = false }
        if (themeModePicker) ThemeModeSheet(appearance.themeMode, {
            container.appearance.setThemeMode(it)
            themeModePicker = false
        }) { themeModePicker = false }
        if (albumViewModePicker) AlbumViewModeSheet(appearance.albumViewMode, {
            container.appearance.setAlbumViewMode(it)
            albumViewModePicker = false
        }) { albumViewModePicker = false }
        if (iconPicker) IconPickerSheet(container.launcherIcons.current(), {
            container.launcherIcons.select(it)
            iconPicker = false
            Toast.makeText(this, R.string.icon_update_delay, Toast.LENGTH_LONG).show()
        }) { iconPicker = false }
        if (reminderPicker) ForgotPasswordReminderSheet(container.unlockPreferences.reminderDays, {
            container.unlockPreferences.updateReminderDays(it)
            reminderPicker = false
        }) { reminderPicker = false }
    }

    @Composable
    private fun RecoveryMethodsScreen(vaultId: VaultId, question: String, topBar: @Composable () -> Unit) {
        var currentPassword by remember { mutableStateOf("") }
        var newQuestion by remember(question) { mutableStateOf(question) }
        var newAnswer by remember { mutableStateOf("") }
        var message by remember { mutableStateOf<String?>(null) }
        Scaffold(topBar = topBar) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(getString(R.string.current_security_question), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(question)
                Text(getString(R.string.recovery_warning), color = MaterialTheme.colorScheme.onSurfaceVariant)
                SecretField(currentPassword, R.string.current_password) {
                    currentPassword = PasswordPolicy.sanitizeVaultPassword(it)
                }
                OutlinedTextField(newQuestion, { newQuestion = it }, label = { Text(getString(R.string.security_question)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(newAnswer, { newAnswer = it }, label = { Text(getString(R.string.security_answer)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(onClick = {
                    val updated = container.configs.updateRecovery(vaultId, currentPassword, newQuestion, newAnswer)
                    message = getString(if (updated) R.string.recovery_updated else R.string.recovery_update_failed)
                    if (updated) {
                        currentPassword = ""
                        newAnswer = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(getString(R.string.save))
                }
            }
        }
    }

    @Composable
    private fun DecoyVaultScreen(
        topBar: @Composable () -> Unit,
        hintsEnabled: Boolean,
        toggleHints: (Boolean) -> Unit,
        changePassword: () -> Unit,
    ) {
        Scaffold(topBar = topBar) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(getString(R.string.decoy_vault), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(getString(R.string.decoy_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(getString(R.string.decoy_password_configured), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text(getString(R.string.decoy_files_isolated), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Text(getString(R.string.vault_one), fontWeight = FontWeight.SemiBold)
                        Text(getString(R.string.primary_password_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Text(getString(R.string.decoy_vault), fontWeight = FontWeight.SemiBold)
                        Text(getString(R.string.decoy_password_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(getString(R.string.decoy_hints), fontWeight = FontWeight.SemiBold)
                        Text(getString(R.string.decoy_hints_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(hintsEnabled, toggleHints)
                }
                Button(onClick = changePassword, modifier = Modifier.fillMaxWidth()) {
                    Text(getString(R.string.change_decoy_password))
                }
            }
        }
    }

    @Composable
    private fun BrowserScreen(topBar: @Composable () -> Unit, preferences: BrowserPreferences) {
        var address by remember { mutableStateOf("https://duckduckgo.com") }
        var title by remember { mutableStateOf(getString(R.string.personal_browser)) }
        var progress by remember { mutableStateOf(0) }
        var canGoBack by remember { mutableStateOf(false) }
        var canGoForward by remember { mutableStateOf(false) }
        var webView by remember { mutableStateOf<WebView?>(null) }
        var settingsVisible by remember { mutableStateOf(false) }
        fun refreshNavigationState(view: WebView?) {
            canGoBack = view?.canGoBack() == true
            canGoForward = view?.canGoForward() == true
        }
        fun loadAddress() {
            BrowserUrlNormalizer.normalize(address)?.let { webView?.loadUrl(it) }
        }
        fun clearBrowserData() {
            webView?.apply {
                clearHistory()
                clearCache(true)
                loadUrl("about:blank")
            }
            CookieManager.getInstance().removeAllCookies(null)
            Toast.makeText(this, R.string.browser_data_cleared, Toast.LENGTH_SHORT).show()
        }
        fun importBrowserDownload(
            url: String?,
            userAgent: String?,
            contentDisposition: String?,
            mimeType: String?,
            referer: String?,
        ) {
            val downloadUrl = url?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            if (downloadUrl == null) {
                Toast.makeText(this, R.string.browser_download_unsupported, Toast.LENGTH_LONG).show()
                return
            }
            Toast.makeText(this, R.string.browser_download_started, Toast.LENGTH_SHORT).show()
            val cookies = CookieManager.getInstance().getCookie(downloadUrl)
            lifecycleScope.launch {
                val result = BrowserDownloadImporter(container.repository)
                    .importDownload(downloadUrl, userAgent, contentDisposition, mimeType, referer, cookies)
                val message = result.exceptionOrNull()?.message
                    ?.let { getString(R.string.browser_download_failed_reason, it) }
                    ?: getString(R.string.browser_download_failed)
                Toast.makeText(
                    this@MainActivity,
                    if (result.isSuccess) getString(R.string.browser_download_imported) else message,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
        Scaffold(topBar = topBar) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { settingsVisible = true }) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                            }
                            IconButton(onClick = ::clearBrowserData) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.browser_clear_data))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text(getString(R.string.browser_address)) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { loadAddress() }),
                            )
                            IconButton(onClick = ::loadAddress) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.open))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { webView?.goBack(); refreshNavigationState(webView) }, enabled = canGoBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                            IconButton(onClick = { webView?.goForward(); refreshNavigationState(webView) }, enabled = canGoForward) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.forward))
                            }
                            IconButton(onClick = { webView?.reload() }) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                            }
                            IconButton(onClick = {
                                address = "https://duckduckgo.com"
                                webView?.loadUrl(address)
                            }) {
                                Icon(Icons.Default.Home, contentDescription = stringResource(R.string.browser_private_search))
                            }
                            IconButton(onClick = {
                                val view = webView
                                importBrowserDownload(
                                    url = view?.url ?: address,
                                    userAgent = view?.settings?.userAgentString,
                                    contentDisposition = null,
                                    mimeType = null,
                                    referer = view?.url,
                                )
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.browser_download_current))
                            }
                        }
                        if (progress in 1..99) {
                            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    factory = { context ->
                        WebView(context).apply browserView@ {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            isFocusable = true
                            isFocusableInTouchMode = true
                            setOnTouchListener { view, _ ->
                                if (!view.hasFocus()) view.requestFocus()
                                false
                            }
                            configurePrivateBrowser(preferences)
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    progress = newProgress
                                    refreshNavigationState(view)
                                }

                                override fun onReceivedTitle(view: WebView, pageTitle: String?) {
                                    title = pageTitle?.takeIf { it.isNotBlank() } ?: getString(R.string.personal_browser)
                                }

                                override fun onCreateWindow(
                                    view: WebView,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message,
                                ): Boolean {
                                    val popup = WebView(view.context).apply {
                                        configurePrivateBrowser(preferences)
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(popupView: WebView, request: WebResourceRequest): Boolean {
                                                val target = request.url.toString()
                                                if (target.startsWith("http://") || target.startsWith("https://")) {
                                                    view.loadUrl(target)
                                                } else {
                                                    Toast.makeText(this@MainActivity, R.string.browser_blocked_link, Toast.LENGTH_SHORT).show()
                                                }
                                                return true
                                            }
                                        }
                                        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                                            importBrowserDownload(url, userAgent, contentDisposition, mimeType, view.url)
                                        }
                                    }
                                    (resultMsg.obj as WebView.WebViewTransport).webView = popup
                                    resultMsg.sendToTarget()
                                    return true
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val scheme = request.url.scheme.orEmpty().lowercase()
                                    if (scheme == "http" || scheme == "https") return false
                                    Toast.makeText(this@MainActivity, R.string.browser_blocked_link, Toast.LENGTH_SHORT).show()
                                    return true
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    url?.takeIf { it != "about:blank" }?.let { address = it }
                                    refreshNavigationState(view)
                                }

                                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                                    handler.cancel()
                                    Toast.makeText(this@MainActivity, R.string.browser_ssl_blocked, Toast.LENGTH_LONG).show()
                                }
                            }
                            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                                importBrowserDownload(url, userAgent, contentDisposition, mimeType, this@browserView.url)
                            }
                            loadUrl(address)
                            webView = this
                            post { requestFocus() }
                        }
                    },
                    update = { it.configurePrivateBrowser(preferences) },
                )
            }
        }
        if (settingsVisible) {
            AlertDialog(
                onDismissRequest = { settingsVisible = false },
                title = { Text(getString(R.string.settings)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(getString(R.string.browser_javascript), modifier = Modifier.weight(1f))
                            Switch(preferences.javaScriptEnabled, preferences::updateJavaScriptEnabled)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(getString(R.string.browser_clear_on_lock), modifier = Modifier.weight(1f))
                            Switch(preferences.clearOnLock, preferences::updateClearOnLock)
                        }
                        Text(getString(R.string.browser_safety_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { settingsVisible = false }) {
                        Text(getString(R.string.close))
                    }
                },
            )
        }
    }

    private fun WebView.configurePrivateBrowser(preferences: BrowserPreferences) {
        settings.javaScriptEnabled = preferences.javaScriptEnabled
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.mediaPlaybackRequiresUserGesture = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
    }

    @Composable
    private fun LanguageScreen(topBar: @Composable () -> Unit, selected: AppLanguage, choose: (AppLanguage) -> Unit) {
        Scaffold(topBar = topBar) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    Card(
                        Modifier.fillMaxWidth().clickable { choose(language) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(getString(language.labelRes), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            if (language == selected) Text(getString(R.string.enabled), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FileTransferScreen(topBar: @Composable () -> Unit, items: List<VaultItem>) {
        var selected by remember { mutableStateOf(setOf<String>()) }
        Scaffold(topBar = topBar) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        pendingImportVault = container.session.vaultId
                        startSystemAction(SystemActionType.IMPORT)
                        importLauncher.launch(arrayOf("*/*"))
                    }) { Text(getString(R.string.import_files)) }
                    FilledTonalButton(onClick = {
                        when (selected.size) {
                            0 -> Toast.makeText(this@MainActivity, R.string.select_files_first, Toast.LENGTH_LONG).show()
                            1 -> items.firstOrNull { it.id in selected }?.let(::launchExport)
                            else -> launchBatchExport(items.filter { it.id in selected })
                        }
                    }) { Text(getString(R.string.export_selected)) }
                }
                LazyColumn {
                    listItems(items, key = { it.id }) { item ->
                        val checked = item.id in selected
                        Row(Modifier.fillMaxWidth().clickable {
                            selected = if (checked) selected - item.id else selected + item.id
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked, onCheckedChange = { isChecked ->
                                selected = if (isChecked) selected + item.id else selected - item.id
                            })
                            Column(Modifier.weight(1f)) {
                                ItemName(item, container.repository)
                                ItemSize(item, container.repository, false, MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WifiTransferScreen(topBar: @Composable () -> Unit, items: List<VaultItem>) {
        var selected by remember { mutableStateOf(setOf<String>()) }
        val session = container.wifiTransfer.session
        Scaffold(topBar = topBar) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (session == null) {
                    Button(onClick = {
                        val chosen = items.filter { it.id in selected }
                        if (chosen.isEmpty()) Toast.makeText(this@MainActivity, R.string.select_files_first, Toast.LENGTH_LONG).show()
                        else container.wifiTransfer.start(chosen)
                    }) { Text(getString(R.string.start_transfer)) }
                } else {
                    Text(getString(R.string.wifi_transfer_url, session.url), fontWeight = FontWeight.SemiBold)
                    Text(getString(R.string.wifi_transfer_pin, session.pin))
                    Text(getString(R.string.wifi_transfer_expires))
                    Button(onClick = container.wifiTransfer::stop) { Text(getString(R.string.stop_transfer)) }
                }
                LazyColumn {
                    listItems(items, key = { it.id }) { item ->
                        val checked = item.id in selected
                        Row(Modifier.fillMaxWidth().clickable {
                            selected = if (checked) selected - item.id else selected + item.id
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked, onCheckedChange = { isChecked ->
                                selected = if (isChecked) selected + item.id else selected - item.id
                            })
                            Column(Modifier.weight(1f)) { ItemName(item, container.repository) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CloudScreen(
        topBar: @Composable () -> Unit,
        controller: CloudSyncController,
        credentials: CloudCredentialStore,
        items: List<VaultItem>,
    ) {
        var endpoint by remember { mutableStateOf(credentials.endpoint()) }
        var username by remember { mutableStateOf(credentials.username()) }
        var password by remember { mutableStateOf("") }
        var confirmRestore by remember { mutableStateOf(false) }
        val status = controller.status
        Scaffold(topBar = topBar) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(endpoint, { endpoint = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(getString(R.string.cloud_endpoint)) })
                OutlinedTextField(username, { username = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(getString(R.string.cloud_username)) })
                SecretField(password, R.string.cloud_password) { password = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) { controller.check(endpoint, username, password) }
                        }
                    }) { Text(getString(R.string.check_connection)) }
                    FilledTonalButton(onClick = {
                        val saved = controller.save(endpoint, username, password)
                        Toast.makeText(this@MainActivity, if (saved) R.string.saved else R.string.cloud_save_failed, Toast.LENGTH_LONG).show()
                    }) { Text(getString(R.string.save)) }
                }
                Button(onClick = {
                    lifecycleScope.launch {
                        val vaultId = container.session.vaultId
                        if (vaultId == null) controller.markUploadError(getString(R.string.vault_locked))
                        else runCatching { controller.uploadBackup(vaultId, items, container.repository) }
                            .onSuccess { Toast.makeText(this@MainActivity, R.string.cloud_upload_complete, Toast.LENGTH_LONG).show() }
                            .onFailure { Toast.makeText(this@MainActivity, R.string.cloud_sync_failed, Toast.LENGTH_LONG).show() }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(getString(R.string.sync_now))
                }
                FilledTonalButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(getString(R.string.restore_from_cloud))
                }
                Text(getString(R.string.cloud_state, status.state.name), color = MaterialTheme.colorScheme.onSurfaceVariant)
                status.lastSyncAt?.let { Text(getString(R.string.cloud_last_sync, formatDateTime(it)), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                status.lastRestoreAt?.let { Text(getString(R.string.cloud_last_restore, formatDateTime(it)), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                status.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        if (confirmRestore) {
            AlertDialog(
                onDismissRequest = { confirmRestore = false },
                title = { Text(getString(R.string.confirm_cloud_restore_title)) },
                text = { Text(getString(R.string.confirm_cloud_restore_message)) },
                dismissButton = {
                    TextButton(onClick = { confirmRestore = false }) { Text(getString(R.string.cancel)) }
                },
                confirmButton = {
                    Button(onClick = {
                        confirmRestore = false
                        lifecycleScope.launch {
                            val vaultId = container.session.vaultId
                            if (vaultId == null) controller.markUploadError(getString(R.string.vault_locked))
                            else runCatching { controller.restoreBackup(vaultId, container.repository) }
                                .onSuccess { Toast.makeText(this@MainActivity, R.string.cloud_restore_complete, Toast.LENGTH_LONG).show() }
                                .onFailure { Toast.makeText(this@MainActivity, R.string.cloud_sync_failed, Toast.LENGTH_LONG).show() }
                        }
                    }) { Text(getString(R.string.restore_from_cloud)) }
                },
            )
        }
    }

    @Composable
    private fun PrivacyPolicyScreen(topBar: @Composable () -> Unit) {
        Scaffold(topBar = topBar) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(getString(R.string.privacy_policy), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(getString(R.string.privacy_policy_local), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(getString(R.string.privacy_policy_encryption), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(getString(R.string.privacy_policy_cloud), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(getString(R.string.privacy_policy_temporary), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    @Composable
    private fun AboutScreen(
        topBar: @Composable () -> Unit,
        cleanupReport: TemporaryCleanupReport,
        shareDiagnostics: () -> Unit,
        legacyStorageAvailable: Boolean,
        storageMigrationReport: StorageMigrationReport?,
        storageMigrationInProgress: Boolean,
        migrateStorage: () -> Unit,
    ) {
        Scaffold(topBar = topBar) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(getString(R.string.about_app), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(getString(R.string.about_app_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(getString(R.string.storage_migration_title), fontWeight = FontWeight.SemiBold)
                            Text(
                                getString(if (legacyStorageAvailable) R.string.storage_migration_hint else R.string.storage_migration_no_legacy),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = migrateStorage,
                                enabled = legacyStorageAvailable && !storageMigrationInProgress,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(getString(if (storageMigrationInProgress) R.string.storage_migration_running else R.string.storage_migration_button))
                            }
                            storageMigrationReport?.let { report ->
                                Text(
                                    if (report.clean) {
                                        getString(R.string.storage_migration_result, report.movedFiles, report.mergedDuplicates)
                                    } else {
                                        getString(R.string.storage_migration_failed_count, report.failed.size)
                                    },
                                    color = if (report.clean) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AboutRow(R.string.about_version, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                            AboutRow(R.string.about_build_status, BuildConfig.BUILD_STATUS)
                            AboutRow(
                                R.string.about_cleanup,
                                if (cleanupReport.clean) getString(R.string.temporary_cleanup_ok, cleanupReport.deleted)
                                else getString(R.string.temporary_cleanup_failed, cleanupReport.failed.size),
                            )
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(getString(R.string.diagnostics), fontWeight = FontWeight.SemiBold)
                            Text(getString(R.string.diagnostics_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = shareDiagnostics, modifier = Modifier.fillMaxWidth()) {
                                Text(getString(R.string.share_diagnostics_logs))
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(getString(R.string.privacy), fontWeight = FontWeight.SemiBold)
                            Text(getString(R.string.about_privacy_local), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(getString(R.string.about_privacy_encrypted), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(getString(R.string.about_privacy_cloud), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!cleanupReport.clean) {
                                Text(cleanupReport.failed.joinToString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AboutRow(label: Int, value: String) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(getString(label), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
    }

    @Composable
    private fun rememberGridState(vaultId: VaultId, tab: VaultContentTab): androidx.compose.foundation.lazy.grid.LazyGridState {
        val position = remember(vaultId, tab) { container.vaultNavigation.scrollPosition(vaultId, tab) }
        val state = rememberLazyGridState(position.index, position.offset)
        DisposableEffect(vaultId, tab, state) {
            onDispose {
                container.vaultNavigation.saveScrollPosition(
                    vaultId,
                    tab,
                    TabScrollPosition(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset),
                )
            }
        }
        return state
    }

    @Composable
    private fun rememberListState(vaultId: VaultId, tab: VaultContentTab): androidx.compose.foundation.lazy.LazyListState {
        val position = remember(vaultId, tab) { container.vaultNavigation.scrollPosition(vaultId, tab) }
        val state = rememberLazyListState(position.index, position.offset)
        DisposableEffect(vaultId, tab, state) {
            onDispose {
                container.vaultNavigation.saveScrollPosition(
                    vaultId,
                    tab,
                    TabScrollPosition(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset),
                )
            }
        }
        return state
    }

    @Composable
    private fun VaultScrollSlider(
        totalItems: Int,
        firstVisibleItemIndex: Int,
        scrollToItem: suspend (Int) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        if (totalItems <= 12) return
        val scope = rememberCoroutineScope()
        val maxIndex = (totalItems - 1).coerceAtLeast(1)
        Box(
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 12.dp),
        ) {
            Slider(
                value = firstVisibleItemIndex.coerceIn(0, maxIndex).toFloat(),
                onValueChange = { value ->
                    val index = value.roundToInt().coerceIn(0, maxIndex)
                    scope.launch { scrollToItem(index) }
                },
                valueRange = 0f..maxIndex.toFloat(),
            )
        }
    }

    @Composable
    private fun EmptyState(tab: VaultTab) {
        val message = when (tab) {
            VaultTab.PHOTOS -> R.string.empty_photos
            VaultTab.VIDEOS -> R.string.empty_videos
            VaultTab.FILES -> R.string.empty_files
            VaultTab.TOOLS, VaultTab.SETTINGS -> R.string.empty
        }
        val icon = when (tab) {
            VaultTab.PHOTOS -> Icons.Default.Photo
            VaultTab.VIDEOS -> Icons.Default.Videocam
            VaultTab.FILES -> Icons.Default.Description
            VaultTab.TOOLS, VaultTab.SETTINGS -> Icons.Default.Description
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(46.dp))
                    Text(getString(message), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    }

    @Composable
    private fun ItemMenu(item: VaultItem) {
        var expanded by remember { mutableStateOf(false) }
        IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.actions)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(getString(R.string.export)) },
                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                onClick = { expanded = false; launchExport(item) },
            )
        }
    }

    @Composable
    private fun ImportProgressDialog(progress: ImportProgress, close: () -> Unit) {
        AlertDialog(
            onDismissRequest = { if (progress.processed == progress.total) close() },
            title = { Text(getString(R.string.import_files)) },
            text = {
                Column {
                    Text(getString(R.string.import_progress, progress.processed, progress.total))
                    LinearProgressIndicator(
                        progress = { if (progress.total == 0) 0f else progress.processed.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                    Text(getString(R.string.import_successes, progress.successes))
                    if (progress.errors > 0) Text(getString(R.string.import_errors, progress.errors))
                    progress.recentErrors.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                if (progress.processed == progress.total) TextButton(close) { Text(getString(R.string.close)) }
            },
        )
    }

    @Composable
    private fun ExportProgressDialog(progress: ExportProgress, close: () -> Unit) {
        AlertDialog(
            onDismissRequest = { if (progress.processed == progress.total) close() },
            title = { Text(getString(R.string.export_selected)) },
            text = {
                Column {
                    Text(getString(R.string.import_progress, progress.processed, progress.total))
                    LinearProgressIndicator(
                        progress = { if (progress.total == 0) 0f else progress.processed.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                    Text(getString(R.string.import_successes, progress.successes))
                    if (progress.errors > 0) Text(getString(R.string.import_errors, progress.errors))
                }
            },
            confirmButton = {
                if (progress.processed == progress.total) TextButton(close) { Text(getString(R.string.close)) }
            },
        )
    }

    @Composable
    private fun ExternalOpenDialog(item: VaultItem, close: () -> Unit) {
        AlertDialog(
            onDismissRequest = close,
            text = { Text(getString(R.string.external_open_warning)) },
            dismissButton = { TextButton(close) { Text(getString(R.string.cancel)) } },
            confirmButton = {
                TextButton(onClick = {
                    lifecycleScope.launch {
                        val file = container.previews.materialize(item)
                        val uri = container.temporaryExports.uri(file)
                        val mime = container.repository.mime(item)
                        startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mime)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                        container.session.clear()
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        screen = Screen.Calculator
                        close()
                    }
                }) { Text(getString(R.string.open)) }
            },
        )
    }

    @Composable
    private fun ChangePasswordDialog(vaultId: VaultId, close: () -> Unit) {
        var old by remember { mutableStateOf("") }
        var new by remember { mutableStateOf("") }
        var message by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = close,
            title = { Text(getString(if (vaultId == VaultId.TWO) R.string.change_decoy_password_title else R.string.change_password)) },
            text = {
                Column {
                    SecretField(old, R.string.old_password) { old = it }
                    SecretField(new, R.string.new_password) { new = it }
                    message?.let { Text(it) }
                }
            },
            dismissButton = { TextButton(close) { Text(getString(R.string.cancel)) } },
            confirmButton = {
                TextButton(onClick = {
                    val changed = container.configs.changePassword(vaultId, old, new)
                    message = getString(if (changed) R.string.password_changed else R.string.password_change_failed)
                }) { Text(getString(R.string.save)) }
            },
        )
    }

    @Composable
    private fun RecoveryScreen() {
        var vault by remember { mutableStateOf(VaultId.ONE) }
        var answer by remember { mutableStateOf("") }
        var verified by remember { mutableStateOf(false) }
        var password by remember { mutableStateOf("") }
        var question by remember { mutableStateOf("") }
        var newAnswer by remember { mutableStateOf("") }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(getString(R.string.recovery_title), style = MaterialTheme.typography.headlineSmall)
            Text(getString(R.string.recovery_warning))
            Row { VaultId.entries.forEach { Button(onClick = { vault = it; verified = false }) { Text(it.name) } } }
            if (!verified) {
                Text(container.configs.get(vault).recoveryQuestion)
                OutlinedTextField(answer, { answer = it }, label = { Text(getString(R.string.answer)) })
                Button(onClick = { verified = container.recovery.verify(vault, answer) }) { Text(getString(R.string.save)) }
            } else {
                SecretField(password, R.string.password) { password = it }
                OutlinedTextField(question, { question = it }, label = { Text(getString(R.string.security_question)) })
                OutlinedTextField(newAnswer, { newAnswer = it }, label = { Text(getString(R.string.security_answer)) })
                Button(onClick = {
                    if (PasswordPolicy.isValidVaultPassword(password) &&
                        container.configs.isPasswordAvailable(vault, password) &&
                        question.isNotBlank() &&
                        newAnswer.isNotBlank()
                    ) {
                        lifecycleScope.launch {
                            container.repository.deleteVault(vault)
                            container.configs.clear(vault)
                            container.configs.create(vault, password, question, newAnswer)
                            screen = Screen.Calculator
                        }
                    }
                }) { Text(getString(R.string.reset_vault)) }
            }
        }
    }
}

private sealed interface Screen {
    data object SetupOne : Screen
    data object SetupTwo : Screen
    data object SetupManualEntry : Screen
    data object Calculator : Screen
    data object Vault : Screen
    data object Recovery : Screen
    data class Video(val itemId: String, val videoIds: List<String>) : Screen
}

private fun importMimeTypes(tab: VaultTab): Array<String> = when (tab) {
    VaultTab.PHOTOS -> arrayOf("image/*", "application/zip", "application/x-zip", "application/x-zip-compressed")
    VaultTab.VIDEOS -> arrayOf("video/*", "application/zip", "application/x-zip", "application/x-zip-compressed")
    VaultTab.FILES, VaultTab.TOOLS, VaultTab.SETTINGS -> arrayOf("*/*")
}

private fun formatDateTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

private fun Screen.isVaultContent() = this == Screen.Vault || this is Screen.Video
private data class ImportProgress(
    val processed: Int,
    val total: Int,
    val successes: Int,
    val errors: Int,
    val recentErrors: List<String>,
)
private data class ExportProgress(
    val processed: Int,
    val total: Int,
    val successes: Int,
    val errors: Int,
)
private data class SourceDeleteTarget(val sourceUri: Uri, val deleteUri: Uri)
private data class BatchExportPart(val items: List<VaultItem>, val estimatedBytes: Long)
private const val IMPORT_PROGRESS_UPDATE_STEP = 10
private const val EXPORT_PROGRESS_UPDATE_STEP = 5
private const val IMPORT_ERROR_PREVIEW_LIMIT = 3
private const val PERSISTED_IMPORT_LIMIT = 200
private const val PENDING_BATCH_EXPORT_SAVE_LIMIT = 500
private const val SOURCE_DELETE_CHUNK_SIZE = 500
private const val MAX_BATCH_EXPORT_ZIP_BYTES = 950L * 1024L * 1024L
private const val ZIP_ENTRY_OVERHEAD_BYTES = 512L
private const val LOG_TAG = "AidenCalculator"
private val ZIP_MIME_TYPES = setOf(
    "application/zip",
    "application/x-zip",
    "application/x-zip-compressed",
)
