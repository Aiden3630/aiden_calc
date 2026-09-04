package com.aiden.calculator

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

data class TestHttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: ByteArray,
)

data class TestHttpResponse(
    val code: Int,
    val body: ByteArray = ByteArray(0),
    val headers: Map<String, String> = emptyMap(),
)

class TestHttpServer(private val handleRequest: (TestHttpRequest) -> TestHttpResponse) : Closeable {
    private val server = ServerSocket(0)
    private val running = AtomicBoolean(true)
    val port: Int = server.localPort

    init {
        thread(isDaemon = true) {
            while (running.get()) {
                runCatching { server.accept().use(::handleSocket) }
            }
        }
    }

    override fun close() {
        running.set(false)
        runCatching { server.close() }
    }

    private fun handleSocket(socket: Socket) {
        val input = socket.getInputStream()
        val headerText = readHeaders(input)
        val lines = headerText.split("\r\n")
        val requestLine = lines.firstOrNull().orEmpty()
        if (requestLine.isBlank()) return
        val parts = requestLine.split(" ")
        val headers = linkedMapOf<String, String>()
        lines.drop(1).forEach { line ->
            if (line.isBlank()) return@forEach
            val key = line.substringBefore(':').trim()
            val value = line.substringAfter(':', "").trim()
            if (key.isNotBlank()) headers[key.lowercase()] = value
        }
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        val body = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = input.read(body, offset, length - offset)
            if (count == -1) break
            offset += count
        }
        val response = handleRequest(
            TestHttpRequest(
                method = headers["x-http-method-override"] ?: parts.getOrElse(0) { "" },
                path = parts.getOrElse(1) { "/" },
                headers = headers,
                body = if (offset == length) body else body.copyOf(offset),
            ),
        )
        val output = socket.getOutputStream()
        val headerBytes = ByteArrayOutputStream().apply {
            write("HTTP/1.1 ${response.code} OK\r\n".toByteArray())
            write("Content-Length: ${response.body.size}\r\n".toByteArray())
            response.headers.forEach { (key, value) -> write("$key: $value\r\n".toByteArray()) }
            write("\r\n".toByteArray())
        }.toByteArray()
        output.write(headerBytes)
        if (response.body.isNotEmpty() && parts.getOrElse(0) { "" } != "HEAD") output.write(response.body)
        output.flush()
    }

    private fun readHeaders(input: java.io.InputStream): String {
        val bytes = ByteArrayOutputStream()
        var previous = IntArray(4) { -1 }
        while (true) {
            val next = input.read()
            if (next == -1) break
            bytes.write(next)
            previous = intArrayOf(previous[1], previous[2], previous[3], next)
            if (previous.contentEquals(intArrayOf('\r'.code, '\n'.code, '\r'.code, '\n'.code))) break
        }
        return bytes.toString(Charsets.ISO_8859_1.name()).removeSuffix("\r\n\r\n")
    }
}
