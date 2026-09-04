package com.aiden.calculator

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

data class BlobWriteResult(val blobName: String, val plainSize: Long, val encryptedSize: Long)

data class StorageMigrationReport(
    val movedFiles: Int = 0,
    val mergedDuplicates: Int = 0,
    val skippedConflicts: Int = 0,
    val bytesMoved: Long = 0L,
    val failed: List<String> = emptyList(),
) {
    val clean: Boolean get() = failed.isEmpty()
    val changed: Boolean get() = movedFiles > 0 || mergedDuplicates > 0
}

class EncryptedBlobStore internal constructor(
    private val root: File,
    private val legacyRoot: File?,
    private val crypto: VaultCrypto,
) {
    constructor(root: File, crypto: VaultCrypto) : this(root, null, crypto)

    constructor(context: Context, crypto: VaultCrypto) : this(
        root = File(context.noBackupFilesDir, "vault_blobs"),
        legacyRoot = File(context.getExternalFilesDir(null) ?: context.filesDir, "vault_blobs"),
        crypto = crypto,
    )

    init {
        root.mkdirs()
    }

    fun write(masterKey: ByteArray, input: InputStream): BlobWriteResult {
        val name = UUID.randomUUID().toString()
        val target = File(root, name)
        val temp = File(root, "$name.tmp")
        try {
            val plainSize = temp.outputStream().use { crypto.encryptStream(masterKey, input, it) }
            check(temp.renameTo(target)) { "Could not finalize blob" }
            return BlobWriteResult(name, plainSize, target.length())
        } catch (error: Throwable) {
            temp.delete()
            target.delete()
            throw error
        }
    }

    fun decrypt(masterKey: ByteArray, blobName: String, output: OutputStream) {
        resolveBlob(blobName).inputStream().use { crypto.decryptStream(masterKey, it, output) }
    }

    fun openEncrypted(blobName: String): InputStream = resolveBlob(blobName).inputStream()

    fun writeExistingEncrypted(blobName: String, input: InputStream) {
        validateBlobName(blobName)
        val target = File(root, blobName)
        val temp = File(root, "$blobName.restore.tmp")
        try {
            input.use { source -> temp.outputStream().use(source::copyTo) }
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (error: Throwable) {
            temp.delete()
            throw error
        }
    }

    fun openReader(masterKey: ByteArray, blobName: String) = EncryptedBlobReader(resolveBlob(blobName), masterKey, crypto)

    fun delete(blobName: String) {
        validateBlobName(blobName)
        File(root, blobName).delete()
        legacyRoot?.let { File(it, blobName).delete() }
    }

    fun exists(blobName: String) = resolveBlobOrNull(blobName)?.isFile == true

    fun blobSize(blobName: String) = resolveBlobOrNull(blobName)?.length() ?: 0L

    fun blobNames() = blobRoots()
        .flatMap { it.listFiles().orEmpty().asSequence() }
        .filter { it.isFile && !isTemporary(it.name) }
        .map(File::getName)
        .distinct()
        .toList()

    fun temporaryBlobNames() = blobRoots()
        .flatMap { it.listFiles().orEmpty().asSequence() }
        .filter { it.isFile && isTemporary(it.name) }
        .map(File::getName)
        .distinct()
        .toList()

    fun clearTemporary(): TemporaryCleanupReport {
        var deleted = 0
        val failed = mutableListOf<String>()
        blobRoots().forEach { directory ->
            directory.listFiles { file -> isTemporary(file.name) }.orEmpty().forEach { file ->
                if (file.delete()) deleted++ else failed += file.name
            }
        }
        return TemporaryCleanupReport(deleted, failed)
    }

    fun hasLegacyStorage(): Boolean = legacyRoot?.listFiles().orEmpty().any { it.isFile && !isTemporary(it.name) }

    /** Moves encrypted containers only; source files stay until each destination is verified. */
    fun migrateLegacyToPrivate(): StorageMigrationReport {
        val legacy = legacyRoot ?: return StorageMigrationReport()
        val files = legacy.listFiles().orEmpty().filter { it.isFile && !isTemporary(it.name) }
        if (files.isEmpty()) return StorageMigrationReport()

        root.mkdirs()
        var movedFiles = 0
        var mergedDuplicates = 0
        var skippedConflicts = 0
        var bytesMoved = 0L
        val failed = mutableListOf<String>()

        files.forEach { source ->
            val target = File(root, source.name)
            try {
                when {
                    target.isFile && sameEncryptedBytes(source, target) -> {
                        if (source.delete()) mergedDuplicates++ else failed += source.name
                    }
                    target.exists() -> skippedConflicts++
                    else -> {
                        moveEncryptedFile(source, target)
                        movedFiles++
                        bytesMoved += target.length()
                    }
                }
            } catch (error: Throwable) {
                failed += "${source.name}: ${error.message ?: error::class.simpleName}"
            }
        }
        return StorageMigrationReport(movedFiles, mergedDuplicates, skippedConflicts, bytesMoved, failed)
    }

    private fun resolveBlob(blobName: String): File =
        resolveBlobOrNull(blobName) ?: throw java.io.FileNotFoundException(blobName)

    private fun resolveBlobOrNull(blobName: String): File? {
        validateBlobName(blobName)
        val primary = File(root, blobName)
        if (primary.isFile) return primary
        val legacy = legacyRoot?.let { File(it, blobName) }
        return legacy?.takeIf(File::isFile)
    }

    private fun blobRoots() = sequenceOf(root, legacyRoot).filterNotNull().distinctBy { it.absolutePath }.toList()

    private fun validateBlobName(blobName: String) {
        require(blobName.isNotBlank()) { "Blank blob name" }
        require(blobName.none { it == '/' || it == '\\' } && blobName != "." && blobName != "..") {
            "Invalid blob name"
        }
    }

    private fun isTemporary(name: String) =
        name.endsWith(".tmp") || name.endsWith(".restore.tmp") || name.endsWith(".migration.tmp")

    private fun moveEncryptedFile(source: File, target: File) {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            return
        } catch (_: AtomicMoveNotSupportedException) {
            // Cross-filesystem fallback below keeps the destination staged and verifiable.
        } catch (_: FileAlreadyExistsException) {
            error("Destination appeared during migration")
        }

        val staged = File(root, "${target.name}.migration.tmp")
        try {
            source.inputStream().use { input -> staged.outputStream().use { output -> input.copyTo(output) } }
            check(staged.length() == source.length()) { "Encrypted copy size mismatch" }
            try {
                Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(staged.toPath(), target.toPath())
            }
            check(sameEncryptedBytes(source, target)) { "Encrypted copy verification failed" }
            check(source.delete()) { "Could not remove migrated source" }
        } finally {
            staged.delete()
        }
    }

    private fun sameEncryptedBytes(first: File, second: File): Boolean {
        if (first.length() != second.length()) return false
        return sha256(first).contentEquals(sha256(second))
    }

    private fun sha256(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest()
    }
}

class EncryptedBlobReader(file: File, masterKey: ByteArray, private val crypto: VaultCrypto) : AutoCloseable {
    private val source = RandomAccessFile(file, "r")
    private var dataKey = ByteArray(0)
    private val blocks = mutableListOf<Block>()
    private var cachedBlock: Block? = null
    private var cachedPlain = ByteArray(0)
    internal var decryptedBlockCount = 0
        private set
    val size: Long

    init {
        var plainOffset = 0L
        try {
            require(source.readInt() == 0x4149444E) { "Invalid blob" }
            val encryptedKeySize = source.readInt()
            require(encryptedKeySize in 1..1024) { "Invalid data key" }
            require(source.filePointer + encryptedKeySize <= source.length()) { "Truncated data key" }
            dataKey = crypto.decryptSmall(masterKey, ByteArray(encryptedKeySize).also(source::readFully))
            while (true) {
                require(source.filePointer + Int.SIZE_BYTES <= source.length()) { "Missing blob terminator" }
                val plainSize = source.readInt()
                if (plainSize == -1) break
                require(plainSize in 1..VaultCrypto.BLOCK_SIZE) { "Invalid plain block" }
                val recordOffset = source.filePointer
                require(recordOffset + 12 + Int.SIZE_BYTES <= source.length()) { "Truncated block header" }
                source.skipBytes(12)
                val encryptedSize = source.readInt()
                require(encryptedSize in 1..VaultCrypto.BLOCK_SIZE + 16) { "Invalid block" }
                require(source.filePointer + encryptedSize <= source.length()) { "Truncated block" }
                blocks += Block(plainOffset, plainSize, recordOffset, encryptedSize)
                source.seek(source.filePointer + encryptedSize)
                plainOffset += plainSize
            }
        } catch (error: Throwable) {
            dataKey.fill(0)
            source.close()
            throw error
        }
        size = plainOffset
    }

    fun read(position: Long, target: ByteArray, offset: Int, length: Int): Int {
        require(position >= 0) { "Negative read position" }
        require(offset >= 0 && length >= 0 && offset <= target.size - length) { "Invalid target range" }
        if (length == 0) return 0
        if (position >= size) return -1
        var remaining = minOf(length.toLong(), size - position).toInt()
        var writeOffset = offset
        var cursor = position
        while (remaining > 0) {
            val block = blockAt(cursor)
            val plain = decrypt(block)
            val insideBlock = (cursor - block.plainOffset).toInt()
            val count = minOf(remaining, plain.size - insideBlock)
            plain.copyInto(target, writeOffset, insideBlock, insideBlock + count)
            cursor += count
            writeOffset += count
            remaining -= count
        }
        return writeOffset - offset
    }

    override fun close() {
        dataKey.fill(0)
        cachedPlain.fill(0)
        source.close()
    }

    private fun blockAt(position: Long): Block {
        var low = 0
        var high = blocks.lastIndex
        while (low <= high) {
            val middle = (low + high) ushr 1
            val block = blocks[middle]
            when {
                position < block.plainOffset -> high = middle - 1
                position >= block.plainOffset + block.plainSize -> low = middle + 1
                else -> return block
            }
        }
        error("Missing block for position")
    }

    private fun decrypt(block: Block): ByteArray {
        if (cachedBlock == block) return cachedPlain
        source.seek(block.recordOffset)
        val nonce = ByteArray(12).also(source::readFully)
        val encryptedSize = source.readInt()
        require(encryptedSize == block.encryptedSize)
        val encrypted = ByteArray(encryptedSize).also(source::readFully)
        val plain = crypto.decryptSmall(dataKey, nonce + encrypted)
        decryptedBlockCount++
        require(plain.size == block.plainSize) { "Invalid decrypted block" }
        cachedPlain.fill(0)
        cachedBlock = block
        cachedPlain = plain
        return plain
    }

    private data class Block(val plainOffset: Long, val plainSize: Int, val recordOffset: Long, val encryptedSize: Int)
}
