package com.aiden.calculator

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultCryptoTest {
    private val crypto = VaultCrypto()

    @Test fun `wrong password cannot unwrap master key`() {
        val encrypted = crypto.wrapMasterKey("12345678".toCharArray(), crypto.randomKey())
        org.junit.Assert.assertThrows(Exception::class.java) {
            crypto.unwrapMasterKey("87654321".toCharArray(), encrypted)
        }
    }

    @Test fun `corrupted encrypted block is rejected`() {
        val key = crypto.randomKey()
        val encrypted = encrypt(key, ByteArray(VaultCrypto.BLOCK_SIZE + 13) { (it % 251).toByte() })
        encrypted[encrypted.lastIndex - 5] = (encrypted[encrypted.lastIndex - 5].toInt() xor 1).toByte()
        org.junit.Assert.assertThrows(Exception::class.java) {
            crypto.decryptStream(key, ByteArrayInputStream(encrypted), ByteArrayOutputStream())
        }
    }

    @Test fun `same file produces different ciphertext`() {
        val key = crypto.randomKey()
        val plain = "same".repeat(100).toByteArray()
        assertFalse(encrypt(key, plain).contentEquals(encrypt(key, plain)))
    }

    @Test fun `vault keys are independent`() {
        val one = crypto.randomKey()
        val two = crypto.randomKey()
        val encrypted = encrypt(one, "private".toByteArray())
        org.junit.Assert.assertThrows(Exception::class.java) {
            crypto.decryptStream(two, ByteArrayInputStream(encrypted), ByteArrayOutputStream())
        }
        val output = ByteArrayOutputStream()
        crypto.decryptStream(one, ByteArrayInputStream(encrypted), output)
        assertArrayEquals("private".toByteArray(), output.toByteArray())
    }

    @Test fun `random access reader decrypts range crossing block boundary`() {
        val key = crypto.randomKey()
        val plain = ByteArray(VaultCrypto.BLOCK_SIZE + 50) { (it % 239).toByte() }
        val file = File.createTempFile("vault", ".blob")
        try {
            file.outputStream().use { crypto.encryptStream(key, ByteArrayInputStream(plain), it) }
            EncryptedBlobReader(file, key, crypto).use { reader ->
                val offset = VaultCrypto.BLOCK_SIZE - 20L
                val output = ByteArray(60)
                org.junit.Assert.assertEquals(60, reader.read(offset, output, 0, output.size))
                assertArrayEquals(plain.copyOfRange(offset.toInt(), offset.toInt() + output.size), output)
            }
        } finally {
            file.delete()
        }
    }

    @Test fun `stream encryption reports plain and container sizes separately`() {
        val key = crypto.randomKey()
        val plain = ByteArray(12345) { (it % 199).toByte() }
        val output = ByteArrayOutputStream()

        val plainSize = crypto.encryptStream(key, ByteArrayInputStream(plain), output)

        assertEquals(plain.size.toLong(), plainSize)
        assertEquals(output.size().toLong(), output.toByteArray().size.toLong())
        assertFalse(plainSize == output.size().toLong())
    }

    @Test fun `random access reader handles inside block and eof`() {
        val key = crypto.randomKey()
        val plain = ByteArray(200) { it.toByte() }
        withReader(key, plain) { reader ->
            val output = ByteArray(25)
            assertEquals(25, reader.read(50, output, 0, output.size))
            assertArrayEquals(plain.copyOfRange(50, 75), output)
            assertEquals(-1, reader.read(plain.size.toLong(), output, 0, output.size))
            assertEquals(0, reader.read(plain.size.toLong(), output, 0, 0))
        }
    }

    @Test fun `random access reader caches block and decrypts again after seek to another block`() {
        val key = crypto.randomKey()
        val plain = ByteArray(VaultCrypto.BLOCK_SIZE + 10) { (it % 197).toByte() }
        withReader(key, plain) { reader ->
            val output = ByteArray(4)
            reader.read(0, output, 0, output.size)
            reader.read(20, output, 0, output.size)
            assertEquals(1, reader.decryptedBlockCount)

            reader.read(VaultCrypto.BLOCK_SIZE.toLong(), output, 0, output.size)
            reader.read(0, output, 0, output.size)
            assertEquals(3, reader.decryptedBlockCount)
        }
    }

    @Test fun `random access reader rejects invalid requests`() {
        val key = crypto.randomKey()
        withReader(key, byteArrayOf(1, 2, 3)) { reader ->
            assertThrows(IllegalArgumentException::class.java) { reader.read(-1, ByteArray(2), 0, 1) }
            assertThrows(IllegalArgumentException::class.java) { reader.read(0, ByteArray(2), 1, 2) }
        }
    }

    @Test fun `random access reader rejects out of bounds plain block`() {
        val key = crypto.randomKey()
        val file = File.createTempFile("vault", ".blob")
        try {
            file.outputStream().use { crypto.encryptStream(key, ByteArrayInputStream(byteArrayOf(1)), it) }
            RandomAccessFile(file, "rw").use { target ->
                target.seek(4)
                val encryptedKeySize = target.readInt()
                target.seek(8L + encryptedKeySize)
                target.writeInt(VaultCrypto.BLOCK_SIZE + 1)
            }
            assertThrows(IllegalArgumentException::class.java) { EncryptedBlobReader(file, key, crypto) }
        } finally {
            file.delete()
        }
    }

    private fun withReader(key: ByteArray, plain: ByteArray, block: (EncryptedBlobReader) -> Unit) {
        val file = File.createTempFile("vault", ".blob")
        try {
            file.outputStream().use { crypto.encryptStream(key, ByteArrayInputStream(plain), it) }
            EncryptedBlobReader(file, key, crypto).use(block)
        } finally {
            file.delete()
        }
    }

    private fun encrypt(key: ByteArray, plain: ByteArray): ByteArray =
        ByteArrayOutputStream().also { crypto.encryptStream(key, ByteArrayInputStream(plain), it) }.toByteArray()
}
