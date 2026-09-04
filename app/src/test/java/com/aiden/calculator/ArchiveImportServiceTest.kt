package com.aiden.calculator

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArchiveImportServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: VaultDatabase
    private lateinit var repository: VaultRepository
    private lateinit var root: File
    private lateinit var crypto: VaultCrypto
    private lateinit var session: VaultSession

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java).allowMainThreadQueries().build()
        crypto = VaultCrypto()
        root = File(context.cacheDir, "archive-${UUID.randomUUID()}").apply { mkdirs() }
        session = VaultSession().apply { unlock(VaultId.ONE, crypto.randomKey()) }
        repository = VaultRepository(context, database.items(), EncryptedBlobStore(root, crypto), crypto, session)
    }

    @After fun tearDown() {
        database.close()
        root.deleteRecursively()
        session.clear()
    }

    @Test fun `imports files from zip stream`() = runBlocking {
        val zip = zipFile(
            "photos/one.jpg" to "one".toByteArray(),
            "docs/two.txt" to "two".toByteArray(),
        )

        val result = ArchiveImportService(context.contentResolver, repository).importZip(Uri.fromFile(zip))
        val namedItems = repository.currentItems()
            .map { repository.displayName(it) to it }
            .sortedBy { it.first }

        assertEquals(2, result.successes)
        assertEquals(0, result.errors)
        assertEquals(listOf("one.jpg", "two.txt"), namedItems.map { it.first })
        assertArrayEquals("one".toByteArray(), export(namedItems[0].second))
        assertArrayEquals("two".toByteArray(), export(namedItems[1].second))
    }

    @Test fun `creates thumbnail for image imported from zip stream`() = runBlocking {
        val zip = zipFile("photos/one.jpg" to jpegBytes())

        val result = ArchiveImportService(context.contentResolver, repository).importZip(Uri.fromFile(zip))
        val item = repository.currentItems().single()

        assertEquals(1, result.successes)
        assertEquals(VaultItemType.PHOTO, item.type)
        assertTrue(repository.thumbnail(item)?.isNotEmpty() == true)
    }

    @Test fun `skips unsafe and metadata entries`() = runBlocking {
        val zip = zipFile(
            "__MACOSX/._one.jpg" to "meta".toByteArray(),
            "../bad.txt" to "bad".toByteArray(),
            "good.txt" to "good".toByteArray(),
        )

        ArchiveImportService(context.contentResolver, repository).importZip(Uri.fromFile(zip))
        val items = repository.currentItems()

        assertEquals(listOf("good.txt"), items.map { repository.displayName(it) })
    }

    private fun zipFile(vararg entries: Pair<String, ByteArray>): File {
        val file = File(root, "${UUID.randomUUID()}.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    private fun jpegBytes(): ByteArray =
        ByteArrayOutputStream().use { output ->
            val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(0xFFE84A5F.toInt())
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                output.toByteArray()
            } finally {
                bitmap.recycle()
            }
        }

    private suspend fun export(item: VaultItem): ByteArray =
        ByteArrayOutputStream().also { repository.export(item, it) }.toByteArray()
}
