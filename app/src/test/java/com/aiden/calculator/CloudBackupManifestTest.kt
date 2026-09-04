package com.aiden.calculator

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupManifestTest {
    @Test fun `serializes and deserializes manifest`() {
        val item = item()
        val manifest = CloudBackupManifest(
            vaultId = VaultId.ONE,
            generatedAt = 123L,
            items = listOf(CloudBackupItem.fromVaultItem(item)),
        )

        val restored = CloudBackupManifest.fromJson(manifest.toJson())

        assertEquals(1, restored.version)
        assertEquals("aiden-calculator", restored.app)
        assertEquals(VaultId.ONE, restored.vaultId)
        assertEquals(123L, restored.generatedAt)
        assertEquals("blob", restored.items.single().blobName)
    }

    @Test fun `preserves encrypted metadata`() {
        val item = item()
        val backup = CloudBackupManifest
            .fromJson(CloudBackupManifest(vaultId = VaultId.ONE, generatedAt = 1L, items = listOf(CloudBackupItem.fromVaultItem(item))).toJson())
            .items
            .single()
            .toVaultItem(VaultId.ONE)

        assertArrayEquals(item.encryptedName, backup.encryptedName)
        assertArrayEquals(item.encryptedMime, backup.encryptedMime)
        assertArrayEquals(item.encryptedThumbnail, backup.encryptedThumbnail)
    }

    @Test fun `rejects unsupported manifest versions`() {
        val json = CloudBackupManifest(vaultId = VaultId.ONE, generatedAt = 1L, items = emptyList())
            .toJson()
            .replace("\"version\":1", "\"version\":99")

        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupManifest.fromJson(json)
        }
    }

    @Test fun `manifest does not contain plaintext metadata values`() {
        val item = item()
        val json = CloudBackupManifest(vaultId = VaultId.ONE, generatedAt = 1L, items = listOf(CloudBackupItem.fromVaultItem(item))).toJson()

        assertTrue(!json.contains("secret-name"))
        assertTrue(!json.contains("text/plain"))
    }

    private fun item() = VaultItem(
        id = "id",
        vaultId = VaultId.ONE,
        blobName = "blob",
        type = VaultItemType.FILE,
        encryptedName = "secret-name".toByteArray(),
        encryptedMime = "text/plain".toByteArray(),
        encryptedThumbnail = byteArrayOf(1, 2, 3),
        size = 10L,
        plainSize = 4L,
        trashState = TrashState.ACTIVE,
        createdAt = 55L,
    )
}
