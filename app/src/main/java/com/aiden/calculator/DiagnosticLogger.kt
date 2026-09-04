package com.aiden.calculator

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticLogger private constructor(
    private val directory: File?,
) {
    constructor(context: Context) : this(File(context.cacheDir, "diagnostics"))

    private val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileTimestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    private var crashHandlerInstalled = false

    @Synchronized
    fun event(area: String, message: String, error: Throwable? = null) {
        val line = buildString {
            append(timestamp.format(Date()))
            append(" [")
            append(Thread.currentThread().name)
            append("] ")
            append(area)
            append(": ")
            append(message)
            error?.let {
                append('\n')
                append(stackTrace(it))
            }
        }
        if (error == null) Log.i(TAG, "$area: $message") else Log.e(TAG, "$area: $message", error)
        appendLine(line)
    }

    @Synchronized
    fun exportFile(): File {
        val dir = requireNotNull(directory) { "Diagnostics are disabled" }.apply { mkdirs() }
        val source = activeFile(dir)
        if (!source.exists()) appendLine("${timestamp.format(Date())} [${Thread.currentThread().name}] diagnostics: empty log created")
        val target = File(dir, "aiden-diagnostics-${fileTimestamp.format(Date())}.log")
        source.copyTo(target, overwrite = true)
        return target
    }

    @Synchronized
    fun clear() {
        directory?.listFiles()?.forEach { it.delete() }
    }

    @Synchronized
    fun installCrashHandler() {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            event("crash", "uncaught thread=${thread.name}", error)
            previous?.uncaughtException(thread, error)
        }
        event(
            "startup",
            "diagnostics enabled sdk=${Build.VERSION.SDK_INT} manufacturer=${Build.MANUFACTURER} model=${Build.MODEL}",
        )
    }

    private fun appendLine(line: String) {
        val dir = directory ?: return
        dir.mkdirs()
        val file = activeFile(dir)
        if (file.length() > MAX_LOG_BYTES) {
            val old = File(dir, "diagnostics.previous.log")
            old.delete()
            file.renameTo(old)
        }
        file.appendText(line + "\n", Charsets.UTF_8)
    }

    private fun activeFile(dir: File) = File(dir, "diagnostics.log")

    private fun stackTrace(error: Throwable): String {
        val writer = StringWriter()
        error.printStackTrace(java.io.PrintWriter(writer))
        return writer.toString()
    }

    companion object {
        private const val TAG = "AidenDiagnostics"
        private const val MAX_LOG_BYTES = 2L * 1024L * 1024L
        val NONE = DiagnosticLogger(null)
    }
}
