package com.aiden.calculator

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PendingBatchExportStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun clearPreferences() {
        context.getSharedPreferences("pending_batch_export", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun `saves and restores pending batch export ids without destination`() {
        PendingBatchExportStore(context).save(VaultId.ONE, listOf("first", "second"), null)

        val restored = PendingBatchExportStore(context).restore()

        assertEquals(VaultId.ONE, restored?.vaultId)
        assertEquals(setOf("first", "second"), restored?.itemIds)
        assertNull(restored?.uri)
    }

    @Test fun `saves and restores pending batch export destination`() {
        val uri = Uri.parse("content://exports/aiden-vault-export.zip")

        PendingBatchExportStore(context).save(VaultId.TWO, listOf("photo"), uri)

        val restored = PendingBatchExportStore(context).restore()

        assertEquals(VaultId.TWO, restored?.vaultId)
        assertEquals(setOf("photo"), restored?.itemIds)
        assertEquals(uri, restored?.uri)
    }

    @Test fun `clear removes pending batch export state`() {
        val store = PendingBatchExportStore(context)
        store.save(VaultId.ONE, listOf("file"), Uri.parse("content://exports/file.zip"))

        store.clear()

        assertTrue(PendingBatchExportStore(context).restore() == null)
    }
}
