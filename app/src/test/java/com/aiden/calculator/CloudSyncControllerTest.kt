package com.aiden.calculator

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CloudSyncControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: VaultDatabase
    private lateinit var repository: VaultRepository
    private lateinit var crypto: VaultCrypto
    private lateinit var session: VaultSession
    private lateinit var root: File
    private lateinit var server: TestHttpServer
    private val remote = linkedMapOf<String, ByteArray>()

    @Before fun setUp() {
        context.getSharedPreferences("cloud_credentials", Context.MODE_PRIVATE).edit().clear().commit()
        database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java).allowMainThreadQueries().build()
        crypto = VaultCrypto()
        root = File(context.cacheDir, "cloud-${UUID.randomUUID()}").apply { mkdirs() }
        session = VaultSession().apply { unlock(VaultId.ONE, crypto.randomKey()) }
        repository = VaultRepository(context, database.items(), EncryptedBlobStore(root, crypto), crypto, session)
        server = TestHttpServer { request ->
            val path = request.path.removePrefix("/dav/").removePrefix("/dav").trimStart('/')
            when (request.method) {
                "HEAD" -> TestHttpResponse(204)
                "MKCOL" -> TestHttpResponse(201)
                "PUT" -> {
                    remote[path] = request.body
                    TestHttpResponse(201)
                }
                "GET" -> remote[path]?.let { TestHttpResponse(200, it) } ?: TestHttpResponse(404)
                else -> TestHttpResponse(405)
            }
        }
    }

    @After fun tearDown() {
        server.close()
        database.close()
        root.deleteRecursively()
        session.clear()
    }

    @Test fun `check and save behavior remains`() {
        val credentials = CloudCredentialStore(context)
        val controller = CloudSyncController(credentials)

        assertTrue(controller.check(endpoint(), "user", "pass"))
        assertTrue(controller.save(endpoint(), "user", "pass"))
        assertEquals(endpoint(), credentials.endpoint())
        assertEquals("user", credentials.username())
    }

    @Test fun `upload success marks last sync`() = runBlocking {
        val credentials = savedCredentials()
        val controller = CloudSyncController(credentials, nowMillis = { 42L })
        val item = repository.importBytes("cloud.txt", "text/plain", "cloud".toByteArray())

        controller.uploadBackup(VaultId.ONE, listOf(item), repository)

        assertEquals(CloudSyncState.IDLE, controller.status.state)
        assertEquals(42L, controller.status.lastSyncAt)
        assertNotNull(remote["aiden-calculator/vaults/ONE/manifest.json"])
        assertNotNull(remote["aiden-calculator/vaults/ONE/blobs/${item.blobName}"])
    }

    @Test fun `restore success marks restoring then idle`() = runBlocking {
        val credentials = savedCredentials()
        val controller = CloudSyncController(credentials, nowMillis = { 50L })
        val item = repository.importBytes("restore.txt", "text/plain", "restore".toByteArray())
        controller.uploadBackup(VaultId.ONE, listOf(item), repository)
        repository.deleteForever(listOf(item.id))

        controller.restoreBackup(VaultId.ONE, repository)
        val restored = repository.currentItem(item.id)
        val exported = ByteArrayOutputStream().also { repository.export(requireNotNull(restored), it) }.toByteArray()

        assertEquals(CloudSyncState.IDLE, controller.status.state)
        assertEquals(50L, controller.status.lastRestoreAt)
        assertArrayEquals("restore".toByteArray(), exported)
    }

    @Test fun `network failures set error`() = runBlocking {
        val credentials = CloudCredentialStore(context)
        credentials.save("http://127.0.0.1:1", "user", "pass")
        val controller = CloudSyncController(credentials)
        val item = repository.importBytes("fail.txt", "text/plain", "fail".toByteArray())

        runCatching { controller.uploadBackup(VaultId.ONE, listOf(item), repository) }

        assertEquals(CloudSyncState.ERROR, controller.status.state)
    }

    private fun savedCredentials(): CloudCredentialStore {
        return CloudCredentialStore(context).also { it.save(endpoint(), "user", "pass") }
    }

    private fun endpoint() = "http://127.0.0.1:${server.port}/dav"

    private suspend fun VaultRepository.importBytes(name: String, mime: String, bytes: ByteArray): VaultItem =
        importStream(name, mime, ByteArrayInputStream(bytes))
}
