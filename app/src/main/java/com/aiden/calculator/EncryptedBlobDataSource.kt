package com.aiden.calculator

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec

@UnstableApi
class EncryptedBlobDataSource(
    private val blobs: EncryptedBlobStore,
    private val session: VaultSession,
    private val item: VaultItem,
) : BaseDataSource(false) {
    private var reader: EncryptedBlobReader? = null
    private var position = 0L
    private var remaining = 0L
    private var uri: Uri? = null
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val key = session.requireKey()
        try {
            reader = blobs.openReader(key, item.blobName)
            uri = dataSpec.uri
            position = dataSpec.position
            require(position >= 0 && position <= requireNotNull(reader).size) { "Invalid read position" }
            val available = requireNotNull(reader).size - position
            require(dataSpec.length == C.LENGTH_UNSET.toLong() || dataSpec.length >= 0) { "Invalid read length" }
            remaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) available else minOf(available, dataSpec.length)
        } catch (error: Throwable) {
            reader?.close()
            reader = null
            throw error
        } finally {
            key.fill(0)
        }
        transferStarted(dataSpec)
        opened = true
        return remaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length) { "Invalid target range" }
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val count = requireNotNull(reader).read(position, buffer, offset, minOf(length.toLong(), remaining).toInt())
        if (count == -1) return C.RESULT_END_OF_INPUT
        position += count
        remaining -= count
        bytesTransferred(count)
        return count
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        reader?.close()
        reader = null
        if (opened) transferEnded()
        opened = false
    }

    class Factory(
        private val blobs: EncryptedBlobStore,
        private val session: VaultSession,
        private val item: VaultItem,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = EncryptedBlobDataSource(blobs, session, item)
    }
}
