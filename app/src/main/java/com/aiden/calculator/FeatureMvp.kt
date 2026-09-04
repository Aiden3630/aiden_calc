package com.aiden.calculator

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.content.ContentResolver
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ProtocolException
import java.net.ServerSocket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.Deflater
import java.net.URLConnection

enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    RU("ru"),
    EN("en");

    val labelRes: Int
        get() = when (this) {
            SYSTEM -> R.string.system_language
            RU -> R.string.russian_language
            EN -> R.string.english_language
        }
}

class LocalePreferences(private val context: Context) {
    private val preferences = context.getSharedPreferences("locale", Context.MODE_PRIVATE)

    private var currentLanguage by mutableStateOf(readLanguage())
    val language: AppLanguage
        get() = currentLanguage

    fun setLanguage(language: AppLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.name).apply()
        currentLanguage = language
        apply(language, context)
    }

    private fun readLanguage(): AppLanguage = runCatching {
        AppLanguage.valueOf(preferences.getString(KEY_LANGUAGE, null) ?: "")
    }.getOrDefault(AppLanguage.SYSTEM)

    companion object {
        private const val KEY_LANGUAGE = "language"

        fun apply(language: AppLanguage, context: Context) {
            val tag = language.tag.orEmpty()
            if (Build.VERSION.SDK_INT >= 33) {
                context.getSystemService(LocaleManager::class.java).applicationLocales =
                    if (tag.isBlank()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
    }
}

class BrowserPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE)

    var javaScriptEnabled by mutableStateOf(preferences.getBoolean(KEY_JAVASCRIPT, true))
        private set
    var clearOnLock by mutableStateOf(preferences.getBoolean(KEY_CLEAR_ON_LOCK, true))
        private set

    fun updateJavaScriptEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_JAVASCRIPT, enabled).apply()
        javaScriptEnabled = enabled
    }

    fun updateClearOnLock(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CLEAR_ON_LOCK, enabled).apply()
        clearOnLock = enabled
    }

    companion object {
        private const val KEY_JAVASCRIPT = "javaScriptEnabled"
        private const val KEY_CLEAR_ON_LOCK = "clearOnLock"
    }
}

object BrowserUrlNormalizer {
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.contains("://")) return trimmed
        val looksLikeHost = trimmed.equals("localhost", ignoreCase = true) ||
            trimmed.contains('.') ||
            trimmed.substringBefore('/').contains(':')
        if (looksLikeHost && !trimmed.contains(' ')) return "https://$trimmed"
        return "https://duckduckgo.com/?q=" + URLEncoder.encode(trimmed, Charsets.UTF_8.name())
    }
}

class BrowserCleanupCoordinator(private val preferences: BrowserPreferences) {
    var cleanupCount = 0
        private set

    fun cleanupOnLock(context: Context) {
        if (!preferences.clearOnLock) return
        cleanupCount++
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
        WebView(context).clearCache(true)
    }
}

object ShareIntentFactory {
    fun create(text: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
}

data class BatchTransferResult(
    val successes: List<String>,
    val errors: List<String>,
    val successCount: Int = successes.size,
    val errorCount: Int = errors.size,
)

data class ArchiveImportProgress(
    val processed: Int,
    val successes: Int,
    val errors: Int,
)

data class ArchiveImportResult(
    val successes: Int,
    val errors: Int,
    val recentErrors: List<String>,
)

class ArchiveImportService(
    private val resolver: ContentResolver,
    private val repository: VaultRepository,
) {
    suspend fun importZip(
        uri: Uri,
        progress: suspend (ArchiveImportProgress) -> Unit = {},
    ): ArchiveImportResult = withContext(Dispatchers.IO) {
        var processed = 0
        var successes = 0
        var errors = 0
        val recentErrors = ArrayDeque<String>()
        requireNotNull(resolver.openInputStream(uri)).use { input ->
            ZipInputStream(input.buffered(64 * 1024)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    try {
                        val entryName = sanitizeArchiveEntryName(entry.name)
                        if (!entry.isDirectory && entryName != null) {
                            processed++
                            runCatching {
                                repository.importStream(entryName, mimeForArchiveEntry(entryName), NonClosingInputStream(zip))
                            }.onSuccess {
                                successes++
                            }.onFailure { error ->
                                errors++
                                recentErrors += "${entryName}: ${error.message.orEmpty()}"
                                while (recentErrors.size > ARCHIVE_ERROR_PREVIEW_LIMIT) recentErrors.removeFirst()
                            }
                            progress(ArchiveImportProgress(processed, successes, errors))
                        }
                    } finally {
                        runCatching { zip.closeEntry() }
                    }
                }
            }
        }
        ArchiveImportResult(successes, errors, recentErrors.toList())
    }

    private fun sanitizeArchiveEntryName(name: String): String? {
        val normalized = name.replace('\\', '/').trim()
        if (normalized.isBlank() || normalized.endsWith('/')) return null
        if (normalized.startsWith("__MACOSX/")) return null
        if (normalized.startsWith("/") || Regex("^[A-Za-z]:").containsMatchIn(normalized)) return null
        if (normalized.split('/').any { it.isBlank() || it == "." || it == ".." }) return null
        val leaf = normalized.substringAfterLast('/').trim().trim('.')
        if (leaf.isBlank() || leaf == ".DS_Store") return null
        return leaf.takeIf { !FileNameRules.isUnsafeZipPath(it) }
    }

    private fun mimeForArchiveEntry(name: String): String =
        URLConnection.guessContentTypeFromName(name) ?: when (name.substringAfterLast('.', "").lowercase(Locale.US)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }

    private companion object {
        const val ARCHIVE_ERROR_PREVIEW_LIMIT = 3
    }
}

private class NonClosingInputStream(input: InputStream) : FilterInputStream(input) {
    override fun close() = Unit
}

data class BatchTransferProgress(
    val processed: Int,
    val total: Int,
    val successes: Int,
    val errors: Int,
)

class BatchExportService(
    private val repository: VaultRepository,
    private val stagingDirectory: File? = null,
    private val diagnostics: DiagnosticLogger = DiagnosticLogger.NONE,
) {
    suspend fun exportZip(
        items: List<VaultItem>,
        output: OutputStream,
        masterKey: ByteArray? = null,
        progress: suspend (BatchTransferProgress) -> Unit = {},
    ): BatchTransferResult = withContext(Dispatchers.IO) {
        val activeItems = items.filter { it.trashState == TrashState.ACTIVE }
        diagnostics.event(
            "batchExport",
            "start requested=${items.size} active=${activeItems.size} staging=${stagingDirectory?.absolutePath ?: "default"} keyed=${masterKey != null}",
        )
        val successes = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var errorCount = 0
        val usedNames = mutableSetOf<String>()
        progress(BatchTransferProgress(0, activeItems.size, 0, 0))
        try {
            ZipOutputStream(output.buffered(64 * 1024)).use { zip ->
                zip.setLevel(Deflater.BEST_SPEED)
                activeItems.forEachIndexed { index, item ->
                    diagnostics.event(
                        "batchExport",
                        "itemStart index=${index + 1}/${activeItems.size} id=${item.id} blob=${item.blobName} encryptedSize=${item.size} plainSize=${item.plainSize}",
                    )
                    val rawName = runCatching {
                        if (masterKey == null) repository.displayName(item) else repository.displayName(item, masterKey)
                    }.onFailure {
                        diagnostics.event("batchExport", "displayNameFailed index=${index + 1} id=${item.id}", it)
                    }.getOrDefault(item.id)
                    val entryName = uniqueZipEntryName(sanitizeZipEntryName(rawName), usedNames)
                    if (!repository.encryptedBlobExists(item.blobName)) {
                        errorCount++
                        diagnostics.event(
                            "batchExport",
                            "itemMissing index=${index + 1} id=${item.id} blob=${item.blobName} entry=$entryName successes=$successCount errors=$errorCount",
                        )
                        if (errors.size < BATCH_RESULT_PREVIEW_LIMIT) errors += "${rawName.ifBlank { item.id }}: missing blob"
                    } else {
                        runCatching {
                            zip.putNextEntry(ZipEntry(entryName))
                            if (masterKey == null) repository.export(item, zip) else repository.export(item, masterKey, zip)
                        }.onSuccess {
                            zip.closeEntry()
                            successCount++
                            diagnostics.event("batchExport", "zipEntrySuccess index=${index + 1} entry=$entryName successes=$successCount errors=$errorCount")
                            if (successes.size < BATCH_RESULT_PREVIEW_LIMIT) successes += entryName
                        }.onFailure { error ->
                            runCatching { zip.closeEntry() }
                            throw error
                        }
                    }
                    progress(BatchTransferProgress(index + 1, activeItems.size, successCount, errorCount))
                }
            }
            diagnostics.event("batchExport", "success total=${activeItems.size} successes=$successCount errors=$errorCount")
        } catch (error: Throwable) {
            diagnostics.event("batchExport", "fatal total=${activeItems.size} successes=$successCount errors=$errorCount", error)
            throw error
        }
        BatchTransferResult(successes, errors, successCount, errorCount)
    }

    private fun sanitizeZipEntryName(name: String): String {
        val cleaned = name
            .replace('\\', '_')
            .replace('/', '_')
            .replace(Regex("\\.\\.+"), ".")
            .trim()
            .trim('.')
        return cleaned.takeIf { it.isNotBlank() && !FileNameRules.isUnsafeZipPath(it) } ?: "file"
    }

    private fun uniqueZipEntryName(name: String, usedNames: MutableSet<String>): String {
        val normalized = name.ifBlank { "file" }
        if (usedNames.add(normalized)) return normalized
        val dot = normalized.lastIndexOf('.').takeIf { it > 0 }
        val base = dot?.let { normalized.substring(0, it) } ?: normalized
        val extension = dot?.let { normalized.substring(it) }.orEmpty()
        var suffix = 2
        while (true) {
            val candidate = "$base ($suffix)$extension"
            if (usedNames.add(candidate)) return candidate
            suffix++
        }
    }

    private companion object {
        const val BATCH_RESULT_PREVIEW_LIMIT = 20
    }
}

class BrowserDownloadImporter(
    private val repository: VaultRepository,
) {
    suspend fun importDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        referer: String? = null,
        cookies: String? = null,
    ): Result<VaultItem> = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = URL(url)
            require(parsed.protocol == "http" || parsed.protocol == "https") { "Unsupported download URL" }
            val connection = (parsed.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                userAgent?.takeIf { it.isNotBlank() }?.let { setRequestProperty("User-Agent", it) }
                referer?.takeIf { it.isNotBlank() }?.let { setRequestProperty("Referer", it) }
                cookies?.takeIf { it.isNotBlank() }?.let { setRequestProperty("Cookie", it) }
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag())
            }
            try {
                val code = connection.responseCode
                require(code in 200..299) { "Download failed: $code" }
                val resolvedDisposition = contentDisposition ?: connection.getHeaderField("Content-Disposition")
                val name = inferDownloadName(resolvedDisposition, parsed)
                val mime = mimeType?.takeIf { it.isNotBlank() }
                    ?: connection.contentType?.substringBefore(';')?.trim()?.takeIf { it.isNotBlank() }
                    ?: "application/octet-stream"
                connection.inputStream.use { repository.importStream(name, mime, it) }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun inferDownloadName(contentDisposition: String?, url: URL): String {
        contentDisposition?.let(::filenameFromContentDisposition)?.let { return it }
        val pathName = url.path.substringAfterLast('/').takeIf { it.isNotBlank() }
        return pathName?.let { decodeUrlPart(it) }?.takeIf { it.isNotBlank() } ?: "download"
    }

    private fun filenameFromContentDisposition(value: String): String? {
        val filenameStar = Regex("filename\\*=([^']*)''([^;]+)", RegexOption.IGNORE_CASE).find(value)
        if (filenameStar != null) return decodeUrlPart(filenameStar.groupValues[2]).trim('"').takeIf { it.isNotBlank() }
        val filename = Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(value)
        return filename?.groupValues?.getOrNull(1)?.trim()?.trim('"')?.takeIf { it.isNotBlank() }
    }

    private fun decodeUrlPart(value: String): String = runCatching {
        URLDecoder.decode(value, Charsets.UTF_8.name())
    }.getOrDefault(value)
}

private object FileNameRules {
    fun isUnsafeZipPath(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return name.contains('/') || name.contains('\\') || lower == ".." || lower.contains("../") ||
            lower.contains("..\\") || name.startsWith("/") || Regex("^[A-Za-z]:").containsMatchIn(name)
    }
}

data class WifiTransferSession(
    val url: String,
    val pin: String,
    val expiresAtMillis: Long,
    val itemIds: Set<String>,
)

class WifiTransferController(
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val pinGenerator: () -> String = { SecureRandom().nextInt(900_000).plus(100_000).toString() },
    private val exporter: suspend (String, OutputStream) -> Boolean = { _, _ -> false },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    private var server: ServerSocket? = null
    private var job: Job? = null
    private var timeoutJob: Job? = null
    var session by mutableStateOf<WifiTransferSession?>(null)
        private set

    fun start(items: List<VaultItem>): WifiTransferSession {
        stop()
        require(items.isNotEmpty()) { "At least one item is required" }
        val socket = ServerSocket(0)
        server = socket
        val current = WifiTransferSession(
            url = "http://${localIpAddress()}:${socket.localPort}/",
            pin = pinGenerator(),
            expiresAtMillis = nowMillis() + SESSION_MS,
            itemIds = items.map { it.id }.toSet(),
        )
        session = current
        job = scope.launch {
            while (!socket.isClosed) {
                runCatching {
                    val client = socket.accept()
                    launch { handle(client.getInputStream(), client.getOutputStream()) { client.close() } }
                }
            }
        }
        timeoutJob = scope.launch {
            delay(SESSION_MS)
            if (session === current) stop()
        }
        return current
    }

    fun stop() {
        job?.cancel()
        job = null
        timeoutJob?.cancel()
        timeoutJob = null
        runCatching { server?.close() }
        server = null
        session = null
    }

    fun isRunning(): Boolean = server?.isClosed == false && session != null

    internal suspend fun handle(input: InputStream, output: OutputStream, close: () -> Unit = {}) {
        try {
            val requestLine = BufferedReader(InputStreamReader(input)).readLine().orEmpty()
            val path = requestLine.split(" ").getOrNull(1).orEmpty()
            val current = session
            val params = queryParams(path)
            val id = params["id"]
            val pin = params["pin"]
            val status = when {
                current == null || nowMillis() >= current.expiresAtMillis -> 403
                pin != current.pin -> 403
                id == null || id !in current.itemIds -> 404
                else -> null
            }
            if (status != null) {
                output.write("HTTP/1.1 $status Error\r\nContent-Length: 0\r\n\r\n".toByteArray())
                return
            }
            output.write("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
            if (!exporter(requireNotNull(id), output)) {
                output.write("\r\n".toByteArray())
            }
        } finally {
            output.flush()
            close()
        }
    }

    private fun queryParams(path: String): Map<String, String> {
        val query = path.substringAfter('?', "")
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull {
            val key = it.substringBefore('=', "")
            val value = it.substringAfter('=', "")
            if (key.isBlank()) null else key to value
        }.toMap()
    }

    private fun localIpAddress(): String {
        return NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress ?: "127.0.0.1"
    }

    companion object {
        const val SESSION_MS = 10L * 60L * 1000L
    }
}

data class CloudAccountConfig(
    val endpoint: String,
    val username: String,
    val encryptedPassword: ByteArray,
    val enabled: Boolean,
)

enum class CloudSyncState {
    IDLE,
    CHECKING,
    UPLOADING,
    RESTORING,
    ERROR,
}

data class CloudSyncStatus(
    val state: CloudSyncState = CloudSyncState.IDLE,
    val lastSyncAt: Long? = null,
    val lastRestoreAt: Long? = null,
    val message: String? = null,
)

data class CloudBackupManifest(
    val version: Int = 1,
    val app: String = "aiden-calculator",
    val vaultId: VaultId,
    val generatedAt: Long,
    val items: List<CloudBackupItem>,
) {
    fun toJson(): String = buildString {
        append('{')
        append("\"version\":").append(version).append(',')
        append("\"app\":").append(jsonString(app)).append(',')
        append("\"vaultId\":").append(jsonString(vaultId.name)).append(',')
        append("\"generatedAt\":").append(generatedAt).append(',')
        append("\"items\":[")
        items.forEachIndexed { index, item ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":").append(jsonString(item.id)).append(',')
            append("\"blobName\":").append(jsonString(item.blobName)).append(',')
            append("\"type\":").append(jsonString(item.type.name)).append(',')
            append("\"encryptedNameBase64\":").append(jsonString(item.encryptedNameBase64)).append(',')
            append("\"encryptedMimeBase64\":").append(jsonString(item.encryptedMimeBase64)).append(',')
            append("\"encryptedThumbnailBase64\":").append(item.encryptedThumbnailBase64?.let(::jsonString) ?: "null").append(',')
            append("\"size\":").append(item.size).append(',')
            append("\"plainSize\":").append(item.plainSize?.toString() ?: "null").append(',')
            append("\"trashState\":").append(jsonString(item.trashState.name)).append(',')
            append("\"createdAt\":").append(item.createdAt)
            append('}')
        }
        append(']')
        append('}')
    }

    companion object {
        fun fromJson(json: String): CloudBackupManifest {
            val version = jsonLong(json, "version").toInt()
            require(version == 1) { "Unsupported backup version" }
            val app = jsonStringValue(json, "app")
            require(app == "aiden-calculator") { "Unsupported backup app" }
            return CloudBackupManifest(
                version = version,
                app = app,
                vaultId = VaultId.valueOf(jsonStringValue(json, "vaultId")),
                generatedAt = jsonLong(json, "generatedAt"),
                items = jsonItemObjects(json).map { item ->
                    CloudBackupItem(
                        id = jsonStringValue(item, "id"),
                        blobName = jsonStringValue(item, "blobName"),
                        type = VaultItemType.valueOf(jsonStringValue(item, "type")),
                        encryptedNameBase64 = jsonStringValue(item, "encryptedNameBase64"),
                        encryptedMimeBase64 = jsonStringValue(item, "encryptedMimeBase64"),
                        encryptedThumbnailBase64 = jsonNullableStringValue(item, "encryptedThumbnailBase64"),
                        size = jsonLong(item, "size"),
                        plainSize = jsonNullableLong(item, "plainSize"),
                        trashState = TrashState.valueOf(jsonStringValue(item, "trashState")),
                        createdAt = jsonLong(item, "createdAt"),
                    )
                },
            )
        }

        private fun jsonString(value: String): String {
            return "\"" + value.flatMap { char ->
                when (char) {
                    '\\' -> listOf('\\', '\\')
                    '"' -> listOf('\\', '"')
                    '\n' -> listOf('\\', 'n')
                    '\r' -> listOf('\\', 'r')
                    '\t' -> listOf('\\', 't')
                    else -> listOf(char)
                }
            }.joinToString("") + "\""
        }

        private fun jsonStringValue(json: String, key: String): String {
            return requireNotNull(jsonNullableStringValue(json, key)) { "Missing $key" }
        }

        private fun jsonNullableStringValue(json: String, key: String): String? {
            val match = Regex("\"${Regex.escape(key)}\"\\s*:\\s*(null|\"((?:\\\\.|[^\"])*)\")").find(json) ?: return null
            if (match.groupValues[1] == "null") return null
            return unescapeJson(match.groupValues[2])
        }

        private fun jsonLong(json: String, key: String): Long {
            return requireNotNull(jsonNullableLong(json, key)) { "Missing $key" }
        }

        private fun jsonNullableLong(json: String, key: String): Long? {
            val value = Regex("\"${Regex.escape(key)}\"\\s*:\\s*(null|-?\\d+)").find(json)?.groupValues?.get(1) ?: return null
            return if (value == "null") null else value.toLong()
        }

        private fun jsonItemObjects(json: String): List<String> {
            val start = json.indexOf("\"items\"").takeIf { it >= 0 } ?: return emptyList()
            val arrayStart = json.indexOf('[', start)
            val arrayEnd = json.indexOfLast { it == ']' }
            require(arrayStart >= 0 && arrayEnd > arrayStart) { "Invalid items" }
            val array = json.substring(arrayStart + 1, arrayEnd)
            val objects = mutableListOf<String>()
            var depth = 0
            var objectStart = -1
            array.forEachIndexed { index, char ->
                when (char) {
                    '{' -> {
                        if (depth == 0) objectStart = index
                        depth++
                    }
                    '}' -> {
                        depth--
                        if (depth == 0 && objectStart >= 0) objects += array.substring(objectStart, index + 1)
                    }
                }
            }
            return objects
        }

        private fun unescapeJson(value: String): String {
            val builder = StringBuilder()
            var index = 0
            while (index < value.length) {
                val char = value[index++]
                if (char != '\\' || index >= value.length) builder.append(char) else {
                    builder.append(
                        when (val escaped = value[index++]) {
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            else -> escaped
                        },
                    )
                }
            }
            return builder.toString()
        }
    }
}

data class CloudBackupItem(
    val id: String,
    val blobName: String,
    val type: VaultItemType,
    val encryptedNameBase64: String,
    val encryptedMimeBase64: String,
    val encryptedThumbnailBase64: String?,
    val size: Long,
    val plainSize: Long?,
    val trashState: TrashState,
    val createdAt: Long,
) {
    fun toVaultItem(vaultId: VaultId) = VaultItem(
        id = id,
        vaultId = vaultId,
        blobName = blobName,
        type = type,
        encryptedName = decodeBase64(encryptedNameBase64),
        encryptedMime = decodeBase64(encryptedMimeBase64),
        encryptedThumbnail = encryptedThumbnailBase64?.let(::decodeBase64),
        size = size,
        plainSize = plainSize,
        trashState = trashState,
        createdAt = createdAt,
    )

    companion object {
        fun fromVaultItem(item: VaultItem) = CloudBackupItem(
            id = item.id,
            blobName = item.blobName,
            type = item.type,
            encryptedNameBase64 = encodeBase64(item.encryptedName),
            encryptedMimeBase64 = encodeBase64(item.encryptedMime),
            encryptedThumbnailBase64 = item.encryptedThumbnail?.let(::encodeBase64),
            size = item.size,
            plainSize = item.plainSize,
            trashState = item.trashState,
            createdAt = item.createdAt,
        )
    }
}

private fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

private fun decodeBase64(value: String): ByteArray = Base64.getDecoder().decode(value)

object CloudUrlValidator {
    fun isValid(endpoint: String): Boolean = runCatching {
        val url = URL(endpoint)
        url.protocol == "https" || url.protocol == "http"
    }.getOrDefault(false)
}

class CloudCredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences("cloud_credentials", Context.MODE_PRIVATE)

    fun save(endpoint: String, username: String, password: String, enabled: Boolean = true) {
        val encrypted = encrypt(password.toByteArray())
        preferences.edit()
            .putString(KEY_ENDPOINT, endpoint)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, encodeBase64(encrypted.first))
            .putString(KEY_NONCE, encodeBase64(encrypted.second))
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun endpoint(): String = preferences.getString(KEY_ENDPOINT, "").orEmpty()
    fun username(): String = preferences.getString(KEY_USERNAME, "").orEmpty()
    fun enabled(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    fun password(): String? {
        val password = preferences.getString(KEY_PASSWORD, null) ?: return null
        val nonce = preferences.getString(KEY_NONCE, null) ?: return null
        return decrypt(
            decodeBase64(password),
            decodeBase64(nonce),
        ).toString(Charsets.UTF_8)
    }

    private fun encrypt(bytes: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.doFinal(bytes) to cipher.iv
    }

    private fun decrypt(bytes: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, nonce))
        return cipher.doFinal(bytes)
    }

    private fun key(): SecretKey = runCatching { androidKey() }.getOrElse {
        SecretKeySpec(MessageDigest.getInstance("SHA-256").digest("cloud-credential-jvm-fallback".toByteArray()), "AES")
    }

    private fun androidKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    companion object {
        private const val ALIAS = "cloud-credential-v1"
        private const val KEY_ENDPOINT = "endpoint"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "encryptedPassword"
        private const val KEY_NONCE = "nonce"
        private const val KEY_ENABLED = "enabled"
    }
}

class WebDavClient(
    private val endpoint: String,
    private val username: String,
    private val password: String,
) {
    fun checkConnection(): Boolean = request("HEAD").responseCode in 200..399

    fun ensureAppFolder() {
        listOf("aiden-calculator/").forEach { path ->
            val code = request("MKCOL", path).responseCode
            if (code !in listOf(200, 201, 204, 405)) error("WebDAV folder error: $code")
        }
    }

    fun ensureVaultFolder(vaultId: VaultId) {
        ensureAppFolder()
        listOf("aiden-calculator/vaults/", "aiden-calculator/vaults/${vaultId.name}/", "aiden-calculator/vaults/${vaultId.name}/blobs/").forEach { path ->
            val code = request("MKCOL", path).responseCode
            if (code !in listOf(200, 201, 204, 405)) error("WebDAV folder error: $code")
        }
    }

    fun uploadBlob(vaultId: VaultId, name: String, inputStream: InputStream) {
        require(name.isNotBlank() && name.none { it == '/' || it == '\\' }) { "Invalid blob name" }
        put("aiden-calculator/vaults/${vaultId.name}/blobs/$name", inputStream)
    }

    fun uploadManifest(vaultId: VaultId, manifest: CloudBackupManifest) {
        put("aiden-calculator/vaults/${vaultId.name}/manifest.json", ByteArrayInputStream(manifest.toJson().toByteArray()))
    }

    fun downloadManifest(vaultId: VaultId): CloudBackupManifest {
        return CloudBackupManifest.fromJson(getBytes("aiden-calculator/vaults/${vaultId.name}/manifest.json").toString(Charsets.UTF_8))
    }

    fun downloadBlob(vaultId: VaultId, name: String): InputStream {
        require(name.isNotBlank() && name.none { it == '/' || it == '\\' }) { "Invalid blob name" }
        return ByteArrayInputStream(getBytes("aiden-calculator/vaults/${vaultId.name}/blobs/$name"))
    }

    private fun put(path: String, input: InputStream) {
        val connection = request("PUT", path)
        connection.doOutput = true
        input.use { source -> connection.outputStream.use(source::copyTo) }
        if (connection.responseCode !in 200..299) error("Upload failed: ${connection.responseCode}")
    }

    private fun getBytes(path: String): ByteArray {
        val connection = request("GET", path)
        if (connection.responseCode !in 200..299) error("Download failed: ${connection.responseCode}")
        return connection.inputStream.use { input ->
            ByteArrayOutputStream().use { output ->
                input.copyTo(output)
                output.toByteArray()
            }
        }
    }

    private fun request(method: String, path: String = ""): HttpURLConnection {
        val base = endpoint.trimEnd('/') + "/"
        return (URL(base + path).openConnection() as HttpURLConnection).apply {
            setWebDavMethod(method)
            setRequestProperty(
                "Authorization",
                "Basic " + encodeBase64("$username:$password".toByteArray()),
            )
            connectTimeout = 10_000
            readTimeout = 10_000
        }
    }

    private fun HttpURLConnection.setWebDavMethod(method: String) {
        try {
            requestMethod = method
        } catch (_: ProtocolException) {
            requestMethod = "POST"
            setRequestProperty("X-HTTP-Method-Override", method)
        }
    }
}

class CloudSyncController(
    private val credentials: CloudCredentialStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    var status by mutableStateOf(CloudSyncStatus())
        private set

    fun check(endpoint: String, username: String, password: String): Boolean {
        status = status.copy(state = CloudSyncState.CHECKING, message = null)
        return runCatching { WebDavClient(endpoint, username, password).checkConnection() }
            .onSuccess { status = status.copy(state = CloudSyncState.IDLE, message = null) }
            .onFailure { status = CloudSyncStatus(state = CloudSyncState.ERROR, message = it.message) }
            .getOrDefault(false)
    }

    fun save(endpoint: String, username: String, password: String): Boolean {
        if (!CloudUrlValidator.isValid(endpoint) || username.isBlank() || password.isBlank()) return false
        credentials.save(endpoint, username, password)
        status = status.copy(message = null)
        return true
    }

    suspend fun uploadBackup(vaultId: VaultId, items: List<VaultItem>, repository: VaultRepository) = withContext(Dispatchers.IO) {
        status = status.copy(state = CloudSyncState.UPLOADING, message = null)
        runCatching {
            val password = credentials.password() ?: error("Cloud credentials are not saved")
            val client = WebDavClient(credentials.endpoint(), credentials.username(), password)
            client.ensureVaultFolder(vaultId)
            items.forEach { item ->
                repository.openEncryptedBlob(item).use { client.uploadBlob(vaultId, item.blobName, it) }
            }
            client.uploadManifest(
                vaultId,
                CloudBackupManifest(
                    vaultId = vaultId,
                    generatedAt = nowMillis(),
                    items = items.map(CloudBackupItem::fromVaultItem),
                ),
            )
        }.onSuccess {
            status = status.copy(state = CloudSyncState.IDLE, lastSyncAt = nowMillis(), message = null)
        }.onFailure {
            status = status.copy(state = CloudSyncState.ERROR, message = it.message)
            throw it
        }
    }

    suspend fun restoreBackup(vaultId: VaultId, repository: VaultRepository) = withContext(Dispatchers.IO) {
        status = status.copy(state = CloudSyncState.RESTORING, message = null)
        runCatching {
            val password = credentials.password() ?: error("Cloud credentials are not saved")
            val client = WebDavClient(credentials.endpoint(), credentials.username(), password)
            val manifest = client.downloadManifest(vaultId)
            require(manifest.vaultId == vaultId) { "Backup vault mismatch" }
            manifest.items.forEach { backup ->
                if (!repository.encryptedBlobExists(backup.blobName)) {
                    client.downloadBlob(vaultId, backup.blobName).use { repository.writeEncryptedBlob(backup.blobName, it) }
                }
                check(repository.encryptedBlobExists(backup.blobName)) { "Missing restored blob" }
                repository.upsertRestored(backup.toVaultItem(vaultId))
            }
        }.onSuccess {
            status = status.copy(state = CloudSyncState.IDLE, lastRestoreAt = nowMillis(), message = null)
        }.onFailure {
            status = status.copy(state = CloudSyncState.ERROR, message = it.message)
            throw it
        }
    }

    fun markUploadError(message: String?) {
        status = CloudSyncStatus(state = CloudSyncState.ERROR, lastSyncAt = status.lastSyncAt, message = message)
    }
}
