package com.aiden.calculator

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BatchExportServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: VaultDatabase
    private lateinit var repository: VaultRepository
    private lateinit var blobs: EncryptedBlobStore
    private lateinit var crypto: VaultCrypto
    private lateinit var session: VaultSession
    private lateinit var root: File
    private lateinit var key: ByteArray

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java).allowMainThreadQueries().build()
        crypto = VaultCrypto()
        root = File(context.cacheDir, "batch-${UUID.randomUUID()}").apply { mkdirs() }
        blobs = EncryptedBlobStore(root, crypto)
        session = VaultSession()
        key = crypto.randomKey()
        session.unlock(VaultId.ONE, key)
        repository = VaultRepository(context, database.items(), blobs, crypto, session)
    }

    @After fun tearDown() {
        database.close()
        root.deleteRecursively()
        key.fill(0)
        session.clear()
    }

    @Test fun `exports two files into one zip`() = runBlocking {
        val first = repository.importBytes("first.txt", "text/plain", "one".toByteArray())
        val second = repository.importBytes("second.txt", "text/plain", "two".toByteArray())

        val output = ByteArrayOutputStream()
        val result = BatchExportService(repository).exportZip(listOf(first, second), output)
        val entries = readZip(output.toByteArray())

        assertEquals(listOf("first.txt", "second.txt"), result.successes)
        assertTrue(result.errors.isEmpty())
        assertArrayEquals("one".toByteArray(), entries.getValue("first.txt"))
        assertArrayEquals("two".toByteArray(), entries.getValue("second.txt"))
    }

    @Test fun `duplicate names are de-duplicated`() = runBlocking {
        val first = repository.importBytes("same.txt", "text/plain", "one".toByteArray())
        val second = repository.importBytes("same.txt", "text/plain", "two".toByteArray())

        val output = ByteArrayOutputStream()
        BatchExportService(repository).exportZip(listOf(first, second), output)

        assertEquals(setOf("same.txt", "same (2).txt"), readZip(output.toByteArray()).keys)
    }

    @Test fun `generated duplicate names do not collide with existing suffixed names`() = runBlocking {
        val first = repository.importBytes("same.jpg", "image/jpeg", "one".toByteArray())
        val existingSuffixed = repository.importBytes("same (2).jpg", "image/jpeg", "two".toByteArray())
        val duplicate = repository.importBytes("same.jpg", "image/jpeg", "three".toByteArray())

        val output = ByteArrayOutputStream()
        BatchExportService(repository).exportZip(listOf(first, existingSuffixed, duplicate), output)

        assertEquals(setOf("same.jpg", "same (2).jpg", "same (3).jpg"), readZip(output.toByteArray()).keys)
    }

    @Test fun `zip entry names are sanitized`() = runBlocking {
        val item = repository.importBytes("../dir\\secret.txt", "text/plain", "safe".toByteArray())

        val output = ByteArrayOutputStream()
        BatchExportService(repository).exportZip(listOf(item), output)
        val entryName = readZip(output.toByteArray()).keys.single()

        assertFalse(entryName.contains('/'))
        assertFalse(entryName.contains('\\'))
        assertFalse(entryName.contains(".."))
    }

    @Test fun `partial export returns errors without crashing`() = runBlocking {
        val valid = repository.importBytes("valid.txt", "text/plain", "ok".toByteArray())
        val missing = valid.copy(id = "missing", blobName = "missing-blob")
        repository.upsertRestored(missing)

        val output = ByteArrayOutputStream()
        val result = BatchExportService(repository, root).exportZip(listOf(valid, missing), output)

        assertEquals(1, result.successes.size)
        assertEquals(1, result.errors.size)
        assertTrue(readZip(output.toByteArray()).containsKey("valid.txt"))
    }

    @Test fun `failed file is skipped without corrupting following zip entries`() = runBlocking {
        val first = repository.importBytes("first.txt", "text/plain", "one".toByteArray())
        val missing = first.copy(id = "missing", blobName = "missing-blob")
        val second = repository.importBytes("second.txt", "text/plain", "two".toByteArray())
        repository.upsertRestored(missing)

        val output = ByteArrayOutputStream()
        val result = BatchExportService(repository, root).exportZip(listOf(first, missing, second), output)
        val entries = readZip(output.toByteArray())

        assertEquals(2, result.successCount)
        assertEquals(1, result.errorCount)
        assertEquals(setOf("first.txt", "second.txt"), entries.keys)
        assertArrayEquals("one".toByteArray(), entries.getValue("first.txt"))
        assertArrayEquals("two".toByteArray(), entries.getValue("second.txt"))
    }

    @Test fun `exports hundreds of files into one zip`() = runBlocking {
        val items = (1..600).map { index ->
            repository.importBytes("file-$index.txt", "text/plain", "body-$index".toByteArray())
        }

        val output = ByteArrayOutputStream()
        val result = BatchExportService(repository, root).exportZip(items, output)
        val entries = readZip(output.toByteArray())

        assertEquals(600, result.successCount)
        assertEquals(0, result.errorCount)
        assertEquals(600, entries.size)
        assertArrayEquals("body-600".toByteArray(), entries.getValue("file-600.txt"))
    }

    @Test fun `exports thousands of files into one zip`() = runBlocking {
        val items = (1..7_500).map { index ->
            repository.importBytes("file-$index.txt", "text/plain", "body-$index".toByteArray())
        }

        val output = ByteArrayOutputStream()
        val result = BatchExportService(repository, root).exportZip(items, output)
        val entries = readZip(output.toByteArray())

        assertEquals(7_500, result.successCount)
        assertEquals(0, result.errorCount)
        assertEquals(7_500, entries.size)
        assertArrayEquals("body-7500".toByteArray(), entries.getValue("file-7500.txt"))
    }

    @Test fun `export continues when session locks after key snapshot`() = runBlocking {
        val items = (1..350).map { index ->
            repository.importBytes("locked-$index.txt", "text/plain", "body-$index".toByteArray())
        }
        val exportKey = session.requireKey()
        session.clear()

        val output = ByteArrayOutputStream()
        try {
            val result = BatchExportService(repository, root).exportZip(items, output, exportKey)
            val entries = readZip(output.toByteArray())

            assertEquals(350, result.successCount)
            assertEquals(0, result.errorCount)
            assertEquals(350, entries.size)
            assertArrayEquals("body-350".toByteArray(), entries.getValue("locked-350.txt"))
        } finally {
            exportKey.fill(0)
        }
    }

    @Test fun `trashes and restores more files than sqlite bind limit`() = runBlocking {
        val items = (1..1_005).map { index ->
            repository.importBytes("trash-$index.txt", "text/plain", "body-$index".toByteArray())
        }
        val ids = items.map { it.id }

        repository.trash(ids)
        assertEquals(1_005, repository.currentItems().count { it.trashState == TrashState.TRASHED })

        repository.restore(ids)
        assertEquals(1_005, repository.currentItems().count { it.trashState == TrashState.ACTIVE })
    }

    private suspend fun VaultRepository.importBytes(name: String, mime: String, bytes: ByteArray): VaultItem =
        importStream(name, mime, ByteArrayInputStream(bytes))

    private fun readZip(bytes: ByteArray): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = ByteArrayOutputStream().also { zip.copyTo(it) }.toByteArray()
            }
        }
        return entries
    }
}
