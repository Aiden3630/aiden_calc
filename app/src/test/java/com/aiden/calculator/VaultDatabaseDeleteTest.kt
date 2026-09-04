package com.aiden.calculator

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VaultDatabaseDeleteTest {
    @Test
    fun `observe limits live list to avoid CursorWindow overflow`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = database.items()
            repeat(1_100) { index ->
                dao.upsert(
                    VaultItem(
                        vaultId = VaultId.ONE,
                        blobName = "item-$index.blob",
                        type = VaultItemType.FILE,
                        encryptedName = byteArrayOf(1),
                        encryptedMime = byteArrayOf(2),
                        size = index.toLong(),
                        createdAt = index.toLong(),
                    ).toEntity(),
                )
            }

            val observed = dao.observe(VaultId.ONE).first()

            assertEquals(1_000, observed.size)
            assertEquals("item-1099.blob", observed.first().blobName)
        } finally {
            database.close()
        }
    }

    @Test
    fun `observe does not load encrypted metadata blobs`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = database.items()
            val item = VaultItem(
                vaultId = VaultId.ONE,
                blobName = "metadata-test.blob",
                type = VaultItemType.FILE,
                encryptedName = byteArrayOf(1, 2, 3),
                encryptedMime = byteArrayOf(4, 5, 6),
                size = 7,
            )
            dao.upsert(item.toEntity())

            val observed = dao.observe(VaultId.ONE).first().single()

            assertTrue(observed.encryptedName.isEmpty())
            assertTrue(observed.encryptedMime.isEmpty())
            assertEquals(byteArrayOf(1, 2, 3).toList(), dao.encryptedName(item.id, VaultId.ONE)?.toList())
            assertEquals(byteArrayOf(4, 5, 6).toList(), dao.encryptedMime(item.id, VaultId.ONE)?.toList())
        } finally {
            database.close()
        }
    }

    @Test
    fun `oversized metadata rows are removed before Room maps them`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            database.openHelper.writableDatabase.execSQL(
                """
                INSERT INTO vault_items (
                    id, vaultId, blobName, type, encryptedName, encryptedMime,
                    encryptedThumbnail, size, plainSize, trashState, createdAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    "oversized-metadata",
                    VaultId.ONE.name,
                    "oversized.blob",
                    VaultItemType.FILE.name,
                    ByteArray(8193) { 1 },
                    byteArrayOf(2),
                    null,
                    3L,
                    null,
                    TrashState.ACTIVE.name,
                    4L,
                ),
            )

            VaultDatabase.deleteCorruptRows(database.openHelper.writableDatabase)

            assertTrue(database.items().observe(VaultId.ONE).first().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `corrupt enum rows are removed before Room maps them`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            database.openHelper.writableDatabase.execSQL(
                """
                INSERT INTO vault_items (
                    id, vaultId, blobName, type, encryptedName, encryptedMime,
                    encryptedThumbnail, size, plainSize, trashState, createdAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    "corrupt-type",
                    VaultId.ONE.name,
                    "corrupt.blob",
                    "BROKEN",
                    byteArrayOf(1),
                    byteArrayOf(2),
                    null,
                    3L,
                    null,
                    TrashState.ACTIVE.name,
                    4L,
                ),
            )

            VaultDatabase.deleteCorruptRows(database.openHelper.writableDatabase)

            assertTrue(database.items().observe(VaultId.ONE).first().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `vault converters tolerate legacy null enum values`() {
        val converters = VaultConverters()

        assertEquals(VaultItemType.FILE, converters.stringToType(null))
        assertEquals(TrashState.ACTIVE, converters.stringToTrash(null))
    }

    @Test
    fun `observe can read before and after permanent delete`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = database.items()
            val item = VaultItem(
                vaultId = VaultId.ONE,
                blobName = "delete-test.blob",
                type = VaultItemType.VIDEO,
                encryptedName = byteArrayOf(1),
                encryptedMime = byteArrayOf(2),
                size = 3,
                trashState = TrashState.TRASHED,
            )
            dao.upsert(item.toEntity())

            val before = dao.observe(VaultId.ONE).first()
            assertEquals(listOf(item.id), before.map { it.id })

            dao.delete(item.id, VaultId.ONE)

            val after = dao.observe(VaultId.ONE).first()
            assertTrue(after.isEmpty())
        } finally {
            database.close()
        }
    }
}
