package com.aiden.calculator

import java.util.UUID

enum class VaultId { ONE, TWO }

data class EncryptedMasterKey(
    val salt: ByteArray,
    val iterations: Int,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
)

data class VaultConfig(
    val id: VaultId,
    val encryptedMasterKey: EncryptedMasterKey,
    val recoveryQuestion: String,
    val recoverySalt: ByteArray,
    val recoveryHash: ByteArray,
)

enum class VaultItemType { PHOTO, VIDEO, FILE }
enum class TrashState { ACTIVE, TRASHED }
enum class SystemActionType { IMPORT, EXPORT }

data class PendingSystemAction(
    val type: SystemActionType,
    val vaultId: VaultId,
    val token: String = UUID.randomUUID().toString(),
    val expiresAtElapsed: Long,
) {
    fun isValid(nowElapsed: Long) = nowElapsed < expiresAtElapsed
}

data class VaultItem(
    val id: String = UUID.randomUUID().toString(),
    val vaultId: VaultId,
    val blobName: String,
    val type: VaultItemType,
    val encryptedName: ByteArray,
    val encryptedMime: ByteArray,
    val encryptedThumbnail: ByteArray? = null,
    val size: Long,
    val plainSize: Long? = null,
    val trashState: TrashState = TrashState.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
)

class VaultSession {
    var vaultId: VaultId? = null
        private set
    private var masterKey: ByteArray? = null

    fun unlock(vaultId: VaultId, key: ByteArray) {
        clear()
        this.vaultId = vaultId
        masterKey = key.copyOf()
    }

    fun requireKey(): ByteArray = masterKey?.copyOf() ?: error("Vault is locked")

    fun clear() {
        masterKey?.fill(0)
        masterKey = null
        vaultId = null
    }
}

fun interface ElapsedRealtimeClock {
    fun now(): Long
}

class VaultSessionCoordinator(
    private val session: VaultSession,
    private val clock: ElapsedRealtimeClock,
    private val scheduleExpiration: (Long, () -> Unit) -> Unit,
    initialPending: PendingSystemAction? = null,
    private val pendingChanged: (PendingSystemAction?) -> Unit = {},
) {
    companion object {
        const val SYSTEM_ACTION_WINDOW_MS = 30 * 60_000L
    }

    private var pending: PendingSystemAction? = initialPending

    init {
        initialPending?.let { action ->
            scheduleExpiration((action.expiresAtElapsed - clock.now()).coerceAtLeast(0)) {
                expireSystemAction(action.token)
            }
        }
    }

    @Synchronized
    fun beginSystemAction(type: SystemActionType): PendingSystemAction {
        val action = PendingSystemAction(
            type = type,
            vaultId = requireNotNull(session.vaultId),
            expiresAtElapsed = clock.now() + SYSTEM_ACTION_WINDOW_MS,
        )
        pending = action
        pendingChanged(action)
        scheduleExpiration(SYSTEM_ACTION_WINDOW_MS) { expireSystemAction(action.token) }
        return action
    }

    @Synchronized
    fun peekSystemAction(): PendingSystemAction? {
        expireIfNeeded()
        return pending
    }

    @Synchronized
    fun consumeSystemAction(type: SystemActionType): PendingSystemAction? {
        expireIfNeeded()
        val action = pending?.takeIf { it.type == type } ?: return null
        pending = null
        pendingChanged(null)
        return action
    }

    @Synchronized
    fun expireSystemAction(token: String? = null) {
        if (token != null && pending?.token != token) return
        session.clear()
    }

    fun isValid(action: PendingSystemAction) = action.isValid(clock.now())

    @Synchronized
    fun clear() {
        pending = null
        pendingChanged(null)
        session.clear()
    }

    private fun expireIfNeeded() {
        if (pending?.isValid(clock.now()) == false) expireSystemAction()
    }
}
