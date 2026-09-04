package com.aiden.calculator

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class EncryptedBlobStoreTest {
    @Test fun `startup cleanup removes interrupted restore files`() {
        val root = createTempDir(prefix = "blob-store-")
        try {
            File(root, "orphan.tmp").writeText("partial")
            File(root, "orphan.restore.tmp").writeText("partial")
            val store = EncryptedBlobStore(root, VaultCrypto())

            val report = store.clearTemporary()

            assertEquals(2, report.deleted)
            assertFalse(File(root, "orphan.tmp").exists())
            assertFalse(File(root, "orphan.restore.tmp").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun `blob names cannot escape the storage root`() {
        val root = createTempDir(prefix = "blob-store-")
        try {
            val store = EncryptedBlobStore(root, VaultCrypto())

            assertThrows(IllegalArgumentException::class.java) { store.exists("../outside") }
            assertThrows(IllegalArgumentException::class.java) {
                store.writeExistingEncrypted("nested/blob", ByteArrayInputStream(byteArrayOf(1)))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun `migration moves encrypted bytes and removes source after verification`() {
        val privateRoot = createTempDir(prefix = "private-blobs-")
        val legacyRoot = createTempDir(prefix = "legacy-blobs-")
        try {
            val payload = ByteArray(4096) { (it % 251).toByte() }
            File(legacyRoot, "blob-a").writeBytes(payload)
            val store = EncryptedBlobStore(privateRoot, legacyRoot, VaultCrypto())

            val report = store.migrateLegacyToPrivate()

            assertEquals(1, report.movedFiles)
            assertEquals(0, report.mergedDuplicates)
            assertEquals(payload.toList(), File(privateRoot, "blob-a").readBytes().toList())
            assertFalse(File(legacyRoot, "blob-a").exists())
        } finally {
            privateRoot.deleteRecursively()
            legacyRoot.deleteRecursively()
        }
    }
}
