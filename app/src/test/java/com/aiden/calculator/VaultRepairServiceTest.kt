package com.aiden.calculator

import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultRepairServiceTest {
    @Test
    fun `scan finds current vault problems ignores other vault and recovers orphan`() = runBlocking {
        val root = createTempDir(prefix = "vault-repair")
        try {
            val crypto = VaultCrypto()
            val blobs = EncryptedBlobStore(root, crypto)
            val dao = FakeVaultItemDao()
            val session = VaultSession()
            val currentKey = crypto.randomKey()
            val otherKey = crypto.randomKey()
            session.unlock(VaultId.ONE, currentKey)
            val missing = item(VaultId.ONE, "missing")
            dao.upsert(missing.toEntity())
            val orphan = blobs.write(currentKey, ByteArrayInputStream("recover me".toByteArray()))
            blobs.write(otherKey, ByteArrayInputStream("other vault".toByteArray()))
            File(root, "interrupted.tmp").writeText("partial")
            val service = VaultRepairService(dao, blobs, crypto, session)

            val report = service.scan()

            assertEquals(listOf(missing.id), report.missingBlobs.map { it.item.id })
            assertEquals(listOf(orphan.blobName), report.recoverableOrphans.map { it.blobName })
            assertEquals(listOf("interrupted.tmp"), report.staleTemporaryBlobs.map { it.blobName })

            val recovered = service.recoverOrphan(report.recoverableOrphans.single())
            assertEquals(VaultItemType.FILE, recovered.type)
            assertEquals("application/octet-stream", crypto.decryptSmall(currentKey, recovered.encryptedMime).toString(Charsets.UTF_8))
            assertTrue(dao.all(VaultId.ONE).any { it.id == recovered.id })

            service.removeMissingBlob(missing.id)
            service.removeTemporary("interrupted.tmp")
            val cleaned = service.scan()
            assertTrue(cleaned.missingBlobs.isEmpty())
            assertTrue(cleaned.recoverableOrphans.isEmpty())
            assertTrue(cleaned.staleTemporaryBlobs.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun item(vaultId: VaultId, blobName: String) = VaultItem(
        vaultId = vaultId,
        blobName = blobName,
        type = VaultItemType.FILE,
        encryptedName = byteArrayOf(),
        encryptedMime = byteArrayOf(),
        size = 0,
    )

    private class FakeVaultItemDao : VaultItemDao {
        private val rows = mutableListOf<VaultItemEntity>()
        private val observed = MutableStateFlow<List<VaultItemRow>>(emptyList())

        override fun observe(vaultId: VaultId): Flow<List<VaultItemRow>> = observed
        override suspend fun get(id: String, vaultId: VaultId) = rows.find { it.id == id && it.vaultId == vaultId }?.toRow()
        override suspend fun thumbnail(id: String, vaultId: VaultId) =
            rows.find { it.id == id && it.vaultId == vaultId }?.encryptedThumbnail
        override suspend fun encryptedName(id: String, vaultId: VaultId) =
            rows.find { it.id == id && it.vaultId == vaultId }?.encryptedName
        override suspend fun encryptedMime(id: String, vaultId: VaultId) =
            rows.find { it.id == id && it.vaultId == vaultId }?.encryptedMime
        override suspend fun upsert(item: VaultItemEntity) {
            rows.removeAll { it.id == item.id }
            rows += item
            observed.value = rows.map(VaultItemEntity::toRow)
        }
        override suspend fun setTrash(ids: List<String>, vaultId: VaultId, state: TrashState) = Unit
        override suspend fun updatePlainSize(id: String, vaultId: VaultId, plainSize: Long) = Unit
        override suspend fun delete(id: String, vaultId: VaultId) {
            rows.removeAll { it.id == id && it.vaultId == vaultId }
            observed.value = rows.map(VaultItemEntity::toRow)
        }
        override suspend fun all(vaultId: VaultId) = rows.filter { it.vaultId == vaultId }.map(VaultItemEntity::toRow)
        override suspend fun byIds(vaultId: VaultId, ids: List<String>) =
            rows.filter { it.vaultId == vaultId && it.id in ids }.map(VaultItemEntity::toRow)
        override suspend fun deleteVault(vaultId: VaultId) {
            rows.removeAll { it.vaultId == vaultId }
            observed.value = rows.map(VaultItemEntity::toRow)
        }
    }
}
