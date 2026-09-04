package com.aiden.calculator

import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MissingBlob(val item: VaultItem)
data class RecoverableOrphan(val blobName: String, val encryptedSize: Long, val plainSize: Long)
data class StaleTemporaryBlob(val blobName: String)
data class RepairReport(
    val missingBlobs: List<MissingBlob>,
    val recoverableOrphans: List<RecoverableOrphan>,
    val staleTemporaryBlobs: List<StaleTemporaryBlob>,
)

class VaultRepairService(
    private val dao: VaultItemDao,
    private val blobs: EncryptedBlobStore,
    private val crypto: VaultCrypto,
    private val session: VaultSession,
) {
    suspend fun scan(): RepairReport = withContext(Dispatchers.IO) {
        val vaultId = requireNotNull(session.vaultId)
        val indexed = dao.all(vaultId).map { it.toModel(vaultId) }
        val indexedNames = indexed.mapTo(mutableSetOf(), VaultItem::blobName)
        val missing = indexed.filterNot { blobs.exists(it.blobName) }.map(::MissingBlob)
        val key = session.requireKey()
        val orphans = try {
            blobs.blobNames()
                .asSequence()
                .filterNot(indexedNames::contains)
                .mapNotNull { name ->
                    runCatching {
                        var plainSize = 0L
                        blobs.decrypt(key, name, object : OutputStream() {
                            override fun write(value: Int) {
                                plainSize++
                            }

                            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                                plainSize += length
                            }
                        })
                        RecoverableOrphan(name, blobs.blobSize(name), plainSize)
                    }.getOrNull()
                }
                .toList()
        } finally {
            key.fill(0)
        }
        RepairReport(
            missingBlobs = missing,
            recoverableOrphans = orphans,
            staleTemporaryBlobs = blobs.temporaryBlobNames().map(::StaleTemporaryBlob),
        )
    }

    suspend fun removeMissingBlob(itemId: String) {
        dao.delete(itemId, requireNotNull(session.vaultId))
    }

    suspend fun recoverOrphan(orphan: RecoverableOrphan): VaultItem {
        val vaultId = requireNotNull(session.vaultId)
        val key = session.requireKey()
        return try {
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            VaultItem(
                id = UUID.randomUUID().toString(),
                vaultId = vaultId,
                blobName = orphan.blobName,
                type = VaultItemType.FILE,
                encryptedName = crypto.encryptSmall(key, "Восстановленный файл $date".toByteArray()),
                encryptedMime = crypto.encryptSmall(key, "application/octet-stream".toByteArray()),
                size = orphan.encryptedSize,
                plainSize = orphan.plainSize,
            ).also { dao.upsert(it.toEntity()) }
        } finally {
            key.fill(0)
        }
    }

    fun removeTemporary(blobName: String) {
        require(blobName.endsWith(".tmp"))
        blobs.delete(blobName)
    }
}
