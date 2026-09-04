package com.aiden.calculator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class TemporaryCleanupReport(
    val deleted: Int = 0,
    val failed: List<String> = emptyList(),
) {
    val clean: Boolean get() = failed.isEmpty()

    fun plus(other: TemporaryCleanupReport) = TemporaryCleanupReport(
        deleted = deleted + other.deleted,
        failed = failed + other.failed,
    )
}

class TemporaryExportManager(
    private val context: Context,
    private val repository: VaultRepository,
) {
    private val directory = File(context.cacheDir, "temporary_exports").apply { mkdirs() }

    suspend fun create(item: VaultItem): File {
        val token = UUID.randomUUID().toString()
        val target = File(directory, "${item.id}-$token")
        val temp = File(directory, "${item.id}-$token.tmp")
        try {
            temp.outputStream().use { repository.export(item, it) }
            check(temp.renameTo(target)) { "Could not finalize temporary export" }
            return target
        } catch (error: Throwable) {
            temp.delete()
            target.delete()
            throw error
        }
    }

    fun uri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun clear(): TemporaryCleanupReport {
        var deleted = 0
        val failed = mutableListOf<String>()
        directory.listFiles().orEmpty().forEach { file ->
            if (file.delete()) deleted++ else failed += file.name
        }
        return TemporaryCleanupReport(deleted, failed)
    }
}

@UnstableApi
class MediaPreviewController(
    private val temporaryExports: TemporaryExportManager,
    private val blobs: EncryptedBlobStore,
    private val session: VaultSession,
) {
    suspend fun materialize(item: VaultItem) = temporaryExports.create(item)

    suspend fun sampledBitmap(item: VaultItem, width: Int, height: Int): Bitmap? = withContext(Dispatchers.IO) {
        val file = temporaryExports.create(item)
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > width * 2 || bounds.outHeight / sample > height * 2) sample *= 2
            BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        } finally {
            file.delete()
        }
    }

    fun videoDataSource(item: VaultItem) = EncryptedBlobDataSource.Factory(blobs, session, item)
}
