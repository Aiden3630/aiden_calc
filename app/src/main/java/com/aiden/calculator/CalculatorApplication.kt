package com.aiden.calculator

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.media3.common.util.UnstableApi
import androidx.sqlite.db.SupportSQLiteDatabase

@UnstableApi
class CalculatorApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.diagnostics.installCrashHandler()
        LocalePreferences.apply(container.locale.language, this)
        container.cleanupStartupTemporaryFiles()
    }
}

@UnstableApi
class AppContainer(application: Application) {
    val crypto = VaultCrypto()
    val diagnostics = DiagnosticLogger(application)
    val configs = VaultConfigStore(application, crypto)
    val session = VaultSession()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val systemActions = application.getSharedPreferences("system_actions", Application.MODE_PRIVATE)
    val sessionCoordinator = VaultSessionCoordinator(
        session = session,
        clock = ElapsedRealtimeClock(SystemClock::elapsedRealtime),
        scheduleExpiration = { delay, expire -> mainHandler.postDelayed(expire, delay) },
        initialPending = runCatching {
            systemActions.getString("type", null)?.let { type ->
                PendingSystemAction(
                    type = SystemActionType.valueOf(type),
                    vaultId = VaultId.valueOf(requireNotNull(systemActions.getString("vaultId", null))),
                    token = requireNotNull(systemActions.getString("token", null)),
                    expiresAtElapsed = systemActions.getLong("expiresAtElapsed", 0),
                )
            }
        }.getOrNull(),
        pendingChanged = { action ->
            systemActions.edit().apply {
                clear()
                if (action != null) {
                    putString("type", action.type.name)
                    putString("vaultId", action.vaultId.name)
                    putString("token", action.token)
                    putLong("expiresAtElapsed", action.expiresAtElapsed)
                }
            }.apply()
        },
    )
    private val database = Room.databaseBuilder(application, VaultDatabase::class.java, "vault-index.db")
        .addMigrations(
            VaultDatabase.MIGRATION_1_2,
            VaultDatabase.MIGRATION_2_3,
            VaultDatabase.MIGRATION_3_4,
            VaultDatabase.MIGRATION_4_5,
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                // Keep legacy/corrupt index rows from reaching Room's non-null converters.
                VaultDatabase.deleteCorruptRows(db)
            }
        })
        .build()
    private val blobs = EncryptedBlobStore(application, crypto)
    val storage = VaultStorageCoordinator(blobs)
    val repository = VaultRepository(application, database.items(), blobs, crypto, session, diagnostics)
    val privacy = PrivacyPreferences(application)
    val unlockPreferences = UnlockPreferences(application)
    val calculatorInput = CalculatorInputPreferences(application)
    val decoyPreferences = DecoyPreferences(application)
    val locale = LocalePreferences(application)
    val browserPreferences = BrowserPreferences(application)
    val calculatorWelcome = CalculatorWelcomePreferences(application)
    val browserCleanup = BrowserCleanupCoordinator(browserPreferences)
    val wifiTransfer = WifiTransferController(
        exporter = { id, output ->
            repository.currentItem(id)?.let {
                repository.export(it, output)
                true
            } ?: false
        },
    )
    val cloudCredentials = CloudCredentialStore(application)
    val cloudSync = CloudSyncController(cloudCredentials)
    val emergencyLock = EmergencyLockPreferences(application)
    val appearance = AppearancePreferences(application)
    val launcherIcons = LauncherIconManager(application)
    val repair = VaultRepairService(database.items(), blobs, crypto, session)
    val temporaryExports = TemporaryExportManager(application, repository)
    val previews = MediaPreviewController(temporaryExports, blobs, session)
    val unlock = UnlockCoordinator(configs, session, ElapsedRealtimeClock(SystemClock::elapsedRealtime), unlockPreferences)
    val biometricUnlock = BiometricUnlockCoordinator(BiometricVaultStore(application), session)
    val recovery = RecoveryCoordinator(configs)
    val vaultNavigation = VaultNavigationState(VaultUiPreferences(application))
    val calculatorUi = CalculatorUiState()
    var startupCleanupReport = TemporaryCleanupReport()
        private set

    fun cleanupStartupTemporaryFiles() {
        startupCleanupReport = temporaryExports.clear().plus(repository.cancelTemporaryImports())
    }
}

class VaultStorageCoordinator(private val blobs: EncryptedBlobStore) {
    fun hasLegacyStorage(): Boolean = blobs.hasLegacyStorage()

    suspend fun migrateLegacyToPrivate(): StorageMigrationReport =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            blobs.migrateLegacyToPrivate()
        }
}
