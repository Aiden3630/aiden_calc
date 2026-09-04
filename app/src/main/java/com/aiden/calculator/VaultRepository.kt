package com.aiden.calculator

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class VaultRepository(
    private val context: Context,
    private val dao: VaultItemDao,
    private val blobs: EncryptedBlobStore,
    private val crypto: VaultCrypto,
    private val session: VaultSession,
    private val diagnostics: DiagnosticLogger = DiagnosticLogger.NONE,
) {
    fun observeCurrent(): Flow<List<VaultItem>> {
        val vaultId = requireNotNull(session.vaultId)
        return dao.observe(vaultId).map { rows -> rows.map { it.toModel(vaultId) } }
    }

    suspend fun import(uri: Uri): VaultItem = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val thumbnail = createThumbnail(resolver, uri, mime)
        requireNotNull(resolver.openInputStream(uri)).use { input ->
            importInternal(resolver.displayName(uri), mime, thumbnail, input)
        }
    }

    suspend fun importStream(
        displayName: String,
        mime: String,
        input: InputStream,
    ): VaultItem = withContext(Dispatchers.IO) {
        val safeDisplayName = displayName.ifBlank { "download" }
        val safeMime = mime.ifBlank { "application/octet-stream" }
        if (safeMime.startsWith("image/") || safeMime.startsWith("video/")) {
            importStreamWithThumbnail(safeDisplayName, safeMime, input)
        } else {
            importInternal(safeDisplayName, safeMime, null, input)
        }
    }

    suspend fun export(item: VaultItem, output: OutputStream) = withContext(Dispatchers.IO) {
        diagnostics.event("repository.export", "start id=${item.id} blob=${item.blobName} size=${item.size} plainSize=${item.plainSize}")
        val key = session.requireKey()
        try {
            blobs.decrypt(key, item.blobName, output)
            diagnostics.event("repository.export", "success id=${item.id}")
        } catch (error: Throwable) {
            diagnostics.event("repository.export", "failed id=${item.id}", error)
            throw error
        } finally {
            key.fill(0)
        }
    }

    suspend fun export(item: VaultItem, masterKey: ByteArray, output: OutputStream) = withContext(Dispatchers.IO) {
        diagnostics.event("repository.exportKeyed", "start id=${item.id} blob=${item.blobName} size=${item.size} plainSize=${item.plainSize}")
        try {
            blobs.decrypt(masterKey, item.blobName, output)
            diagnostics.event("repository.exportKeyed", "success id=${item.id}")
        } catch (error: Throwable) {
            diagnostics.event("repository.exportKeyed", "failed id=${item.id}", error)
            throw error
        }
    }

    suspend fun displayName(item: VaultItem): String = withContext(Dispatchers.Default) {
        val encryptedName = item.encryptedName.takeIf { it.isNotEmpty() }
            ?: dao.encryptedName(item.id, item.vaultId)
            ?: return@withContext item.id
        runCatching { decryptText(encryptedName) }.getOrElse { item.id }
    }

    suspend fun displayName(item: VaultItem, masterKey: ByteArray): String = withContext(Dispatchers.Default) {
        decryptText(item.encryptedName, masterKey)
    }

    suspend fun mime(item: VaultItem): String = withContext(Dispatchers.Default) {
        val encryptedMime = item.encryptedMime.takeIf { it.isNotEmpty() }
            ?: dao.encryptedMime(item.id, item.vaultId)
            ?: return@withContext "application/octet-stream"
        runCatching { decryptText(encryptedMime) }.getOrElse { "application/octet-stream" }
    }

    suspend fun thumbnail(item: VaultItem): ByteArray? = withContext(Dispatchers.Default) {
        (item.encryptedThumbnail ?: dao.thumbnail(item.id, item.vaultId))?.let(::decryptBytes)
    }
    suspend fun currentItem(id: String): VaultItem? {
        val vaultId = requireNotNull(session.vaultId)
        return dao.get(id, vaultId)?.toModel(vaultId)
    }
    suspend fun currentItems(): List<VaultItem> {
        val vaultId = requireNotNull(session.vaultId)
        return dao.all(vaultId).map { it.toModel(vaultId) }
    }
    suspend fun currentItems(ids: Collection<String>): List<VaultItem> = withContext(Dispatchers.IO) {
        val vaultId = requireNotNull(session.vaultId)
        if (ids.isEmpty()) return@withContext emptyList()
        ids.chunked(SQLITE_BIND_CHUNK_SIZE)
            .flatMap { dao.byIds(vaultId, it).map { row -> row.toModel(vaultId) } }
    }
    fun openEncryptedBlob(item: VaultItem): InputStream = blobs.openEncrypted(item.blobName)
    fun encryptedBlobExists(blobName: String): Boolean = blobs.exists(blobName)
    fun writeEncryptedBlob(blobName: String, input: InputStream) = blobs.writeExistingEncrypted(blobName, input)

    suspend fun upsertRestored(item: VaultItem): VaultItem = withContext(Dispatchers.IO) {
        val existing = dao.get(item.id, item.vaultId)?.toModel(item.vaultId)
        if (existing != null && blobs.exists(existing.blobName)) return@withContext existing
        dao.upsert(item.toEntity())
        item
    }

    suspend fun plainSize(item: VaultItem): Long = withContext(Dispatchers.IO) {
        item.plainSize?.let { return@withContext it }
        val key = session.requireKey()
        try {
            blobs.openReader(key, item.blobName).use { reader ->
                reader.size.also { dao.updatePlainSize(item.id, requireNotNull(session.vaultId), it) }
            }
        } finally {
            key.fill(0)
        }
    }

    suspend fun trash(ids: List<String>) = setTrash(ids, TrashState.TRASHED)
    suspend fun restore(ids: List<String>) = setTrash(ids, TrashState.ACTIVE)

    suspend fun deleteForever(ids: List<String>) = withContext(Dispatchers.IO) {
        val vaultId = requireNotNull(session.vaultId)
        diagnostics.event("repository.deleteForever", "start vault=$vaultId ids=${ids.size}")
        try {
            ids.chunked(SQLITE_BIND_CHUNK_SIZE).forEachIndexed { chunkIndex, chunk ->
                diagnostics.event("repository.deleteForever", "chunkStart index=$chunkIndex size=${chunk.size}")
                dao.byIds(vaultId, chunk).forEach { item ->
                    diagnostics.event("repository.deleteForever", "item id=${item.id} blob=${item.blobName} encryptedSize=${item.size}")
                    blobs.delete(item.blobName)
                    dao.delete(item.id, vaultId)
                }
                diagnostics.event("repository.deleteForever", "chunkSuccess index=$chunkIndex")
            }
            diagnostics.event("repository.deleteForever", "success ids=${ids.size}")
        } catch (error: Throwable) {
            diagnostics.event("repository.deleteForever", "failed ids=${ids.size}", error)
            throw error
        }
    }

    private suspend fun setTrash(ids: List<String>, state: TrashState) = withContext(Dispatchers.IO) {
        val vaultId = requireNotNull(session.vaultId)
        diagnostics.event("repository.setTrash", "start vault=$vaultId state=$state ids=${ids.size}")
        try {
            ids.chunked(SQLITE_BIND_CHUNK_SIZE).forEachIndexed { chunkIndex, chunk ->
                diagnostics.event("repository.setTrash", "chunkStart index=$chunkIndex size=${chunk.size} state=$state")
                dao.setTrash(chunk, vaultId, state)
                diagnostics.event("repository.setTrash", "chunkSuccess index=$chunkIndex state=$state")
            }
            diagnostics.event("repository.setTrash", "success state=$state ids=${ids.size}")
        } catch (error: Throwable) {
            diagnostics.event("repository.setTrash", "failed state=$state ids=${ids.size}", error)
            throw error
        }
    }

    fun cancelTemporaryImports(): TemporaryCleanupReport = blobs.clearTemporary().plus(clearCachedImportTemps())

    suspend fun deleteVault(id: VaultId) {
        dao.all(id).forEach { blobs.delete(it.blobName) }
        dao.deleteVault(id)
    }

    private fun decryptText(encrypted: ByteArray): String {
        return decryptBytes(encrypted).toString(Charsets.UTF_8)
    }

    private fun decryptText(encrypted: ByteArray, masterKey: ByteArray): String {
        val plain = crypto.decryptSmall(masterKey, encrypted)
        return try {
            plain.toString(Charsets.UTF_8)
        } finally {
            plain.fill(0)
        }
    }

    private fun decryptBytes(encrypted: ByteArray): ByteArray {
        val key = session.requireKey()
        return try {
            crypto.decryptSmall(key, encrypted)
        } finally {
            key.fill(0)
        }
    }

    private fun clearCachedImportTemps(): TemporaryCleanupReport {
        var deleted = 0
        val failed = mutableListOf<String>()
        context.cacheDir.listFiles { file ->
            file.isFile && file.name.startsWith("vault-import-") && file.name.endsWith(".tmp")
        }.orEmpty().forEach { file ->
            if (file.delete()) deleted++ else failed += file.name
        }
        return TemporaryCleanupReport(deleted, failed)
    }

    private suspend fun importInternal(
        displayName: String,
        mime: String,
        thumbnail: ByteArray?,
        input: InputStream,
    ): VaultItem {
        val vaultId = requireNotNull(session.vaultId)
        val key = session.requireKey()
        try {
            val result = blobs.write(key, input)
            try {
                check(session.vaultId == vaultId) { "Vault was locked during import" }
                val item = VaultItem(
                    vaultId = vaultId,
                    blobName = result.blobName,
                    type = typeOf(mime),
                    encryptedName = crypto.encryptSmall(key, displayName.toByteArray()),
                    encryptedMime = crypto.encryptSmall(key, mime.toByteArray()),
                    encryptedThumbnail = thumbnail?.let { crypto.encryptSmall(key, it) },
                    size = result.encryptedSize,
                    plainSize = result.plainSize,
                )
                dao.upsert(item.toEntity())
                return item
            } catch (error: Throwable) {
                blobs.delete(result.blobName)
                throw error
            }
        } finally {
            key.fill(0)
        }
    }

    private fun createThumbnail(resolver: ContentResolver, uri: Uri, mime: String): ByteArray? = when {
        mime.startsWith("image/") -> createImageThumbnail(resolver, uri)
        mime.startsWith("video/") -> createVideoThumbnail(uri)
        else -> null
    }

    private suspend fun importStreamWithThumbnail(
        displayName: String,
        mime: String,
        input: InputStream,
    ): VaultItem {
        val temp = File.createTempFile("vault-import-", ".tmp", context.cacheDir)
        return try {
            temp.outputStream().use { output -> input.copyTo(output) }
            val uri = Uri.fromFile(temp)
            val thumbnail = createThumbnail(context.contentResolver, uri, mime)
            temp.inputStream().use { copied -> importInternal(displayName, mime, thumbnail, copied) }
        } finally {
            temp.delete()
        }
    }

    private fun createImageThumbnail(resolver: ContentResolver, uri: Uri): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > THUMBNAIL_MAX_SIDE || bounds.outHeight / sample > THUMBNAIL_MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        bitmap.toThumbnailBytes()
    }.getOrNull()

    private fun createVideoThumbnail(uri: Uri): ByteArray? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime()
                ?: return null
            bitmap.toThumbnailBytes()
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun typeOf(mime: String) = when {
        mime.startsWith("image/") -> VaultItemType.PHOTO
        mime.startsWith("video/") -> VaultItemType.VIDEO
        else -> VaultItemType.FILE
    }

    private fun ContentResolver.displayName(uri: Uri): String {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return "file"
    }

    private fun Bitmap.toThumbnailBytes(): ByteArray {
        val largestSide = max(width, height)
        val thumbnail = if (largestSide > THUMBNAIL_MAX_SIDE) {
            val scale = THUMBNAIL_MAX_SIDE.toFloat() / largestSide
            Bitmap.createScaledBitmap(
                this,
                (width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1),
                true,
            )
        } else {
            this
        }
        return try {
            ByteArrayOutputStream().use { output ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 68, output)
                output.toByteArray()
            }
        } finally {
            if (thumbnail != this) thumbnail.recycle()
            recycle()
        }
    }

    private companion object {
        const val THUMBNAIL_MAX_SIDE = 180
        const val SQLITE_BIND_CHUNK_SIZE = 900
    }
}
