package com.aiden.calculator

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
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
class BrowserDownloadImporterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: VaultDatabase
    private lateinit var repository: VaultRepository
    private lateinit var blobs: EncryptedBlobStore
    private lateinit var crypto: VaultCrypto
    private lateinit var session: VaultSession
    private lateinit var root: File
    private val responses = mutableMapOf<String, TestHttpResponse>()
    private val observedHeaders = mutableMapOf<String, Map<String, String>>()
    private lateinit var server: TestHttpServer

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java).allowMainThreadQueries().build()
        crypto = VaultCrypto()
        root = File(context.cacheDir, "browser-${UUID.randomUUID()}").apply { mkdirs() }
        blobs = EncryptedBlobStore(root, crypto)
        session = VaultSession().apply { unlock(VaultId.ONE, crypto.randomKey()) }
        repository = VaultRepository(context, database.items(), blobs, crypto, session)
        server = TestHttpServer { request ->
            observedHeaders[request.path] = request.headers
            responses[request.path] ?: TestHttpResponse(404)
        }
    }

    @After fun tearDown() {
        server.close()
        database.close()
        root.deleteRecursively()
        session.clear()
    }

    @Test fun `rejects non-http urls`() = runBlocking {
        val result = BrowserDownloadImporter(repository).importDownload("file:///tmp/a.txt", null, null, null)

        assertTrue(result.isFailure)
    }

    @Test fun `derives filename from content disposition`() = runBlocking {
        server.respond("/download", 200, "body".toByteArray(), "text/plain")

        val item = BrowserDownloadImporter(repository)
            .importDownload(url("/download"), "agent", "attachment; filename=\"report.txt\"", null)
            .getOrThrow()

        assertEquals("report.txt", repository.displayName(item))
        assertEquals("text/plain", repository.mime(item))
    }

    @Test fun `derives fallback filename from url`() = runBlocking {
        server.respond("/files/photo.bin", 200, "bytes".toByteArray(), "application/custom")

        val item = BrowserDownloadImporter(repository)
            .importDownload(url("/files/photo.bin"), null, null, null)
            .getOrThrow()

        assertEquals("photo.bin", repository.displayName(item))
        assertEquals("application/custom", repository.mime(item))
    }

    @Test fun `imports stream into repository path`() = runBlocking {
        val bytes = "encrypted source".toByteArray()
        server.respond("/payload", 200, bytes, "application/octet-stream")

        val item = BrowserDownloadImporter(repository)
            .importDownload(url("/payload"), null, null, null)
            .getOrThrow()
        val exported = java.io.ByteArrayOutputStream().also { repository.export(item, it) }.toByteArray()

        assertArrayEquals(bytes, exported)
    }

    @Test fun `sends browser headers for protected downloads`() = runBlocking {
        server.respond("/protected", 200, "secret".toByteArray(), "text/plain")

        BrowserDownloadImporter(repository)
            .importDownload(
                url("/protected"),
                "browser-agent",
                "attachment; filename=\"secret.txt\"",
                "text/plain",
                referer = "https://example.test/page",
                cookies = "session=abc",
            )
            .getOrThrow()

        val headers = observedHeaders.getValue("/protected")
        assertEquals("browser-agent", headers["user-agent"])
        assertEquals("https://example.test/page", headers["referer"])
        assertEquals("session=abc", headers["cookie"])
    }

    @Test fun `handles http error codes`() = runBlocking {
        server.respond("/missing", 404, ByteArray(0), "text/plain")

        val result = BrowserDownloadImporter(repository).importDownload(url("/missing"), null, null, null)

        assertTrue(result.isFailure)
    }

    private fun TestHttpServer.respond(path: String, code: Int, body: ByteArray, mime: String) {
        responses[path] = TestHttpResponse(code, body, mapOf("Content-Type" to mime))
    }

    private fun url(path: String) = "http://127.0.0.1:${server.port}$path"
}
