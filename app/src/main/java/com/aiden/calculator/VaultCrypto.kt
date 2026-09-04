package com.aiden.calculator

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class VaultCrypto(private val random: SecureRandom = SecureRandom()) {
    companion object {
        const val BLOCK_SIZE = 1024 * 1024
        const val ITERATIONS = 210_000
        private const val TAG_BITS = 128
        private const val NONCE_SIZE = 12
        private val MAGIC = byteArrayOf(0x41, 0x49, 0x44, 0x4E)
    }

    fun randomKey() = ByteArray(32).also(random::nextBytes)
    fun randomSalt() = ByteArray(16).also(random::nextBytes)

    fun wrapMasterKey(password: CharArray, masterKey: ByteArray, salt: ByteArray = randomSalt()): EncryptedMasterKey {
        val wrappingKey = derive(password, salt, ITERATIONS)
        return try {
            val nonce = nonce()
            EncryptedMasterKey(salt, ITERATIONS, nonce, crypt(Cipher.ENCRYPT_MODE, wrappingKey, nonce, masterKey))
        } finally {
            wrappingKey.fill(0)
            password.fill('\u0000')
        }
    }

    fun unwrapMasterKey(password: CharArray, encrypted: EncryptedMasterKey): ByteArray {
        val wrappingKey = derive(password, encrypted.salt, encrypted.iterations)
        return try {
            crypt(Cipher.DECRYPT_MODE, wrappingKey, encrypted.nonce, encrypted.ciphertext)
        } finally {
            wrappingKey.fill(0)
            password.fill('\u0000')
        }
    }

    fun hashRecovery(answer: String, salt: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(salt + answer.trim().lowercase().toByteArray())

    fun encryptSmall(masterKey: ByteArray, plain: ByteArray): ByteArray {
        val nonce = nonce()
        return nonce + crypt(Cipher.ENCRYPT_MODE, masterKey, nonce, plain)
    }

    fun decryptSmall(masterKey: ByteArray, encrypted: ByteArray): ByteArray =
        crypt(Cipher.DECRYPT_MODE, masterKey, encrypted.copyOfRange(0, NONCE_SIZE), encrypted.copyOfRange(NONCE_SIZE, encrypted.size))

    fun encryptStream(masterKey: ByteArray, input: InputStream, output: OutputStream): Long {
        val dataKey = randomKey()
        var plainSize = 0L
        try {
            DataOutputStream(output.buffered()).use { target ->
                target.write(MAGIC)
                val encryptedKey = encryptSmall(masterKey, dataKey)
                target.writeInt(encryptedKey.size)
                target.write(encryptedKey)
                val buffer = ByteArray(BLOCK_SIZE)
                while (true) {
                    val count = input.readChunk(buffer)
                    if (count == 0) break
                    plainSize += count
                    val nonce = nonce()
                    val encrypted = crypt(Cipher.ENCRYPT_MODE, dataKey, nonce, buffer.copyOf(count))
                    target.writeInt(count)
                    target.write(nonce)
                    target.writeInt(encrypted.size)
                    target.write(encrypted)
                }
                target.writeInt(-1)
            }
            return plainSize
        } finally {
            dataKey.fill(0)
            input.close()
        }
    }

    fun decryptStream(masterKey: ByteArray, input: InputStream, output: OutputStream) {
        DataInputStream(input.buffered()).use { source ->
            require(source.readExact(MAGIC.size).contentEquals(MAGIC)) { "Invalid blob" }
            val encryptedKeySize = source.readInt()
            require(encryptedKeySize in 1..1024) { "Invalid data key" }
            val dataKey = decryptSmall(masterKey, source.readExact(encryptedKeySize))
            try {
                while (true) {
                    val plainSize = source.readInt()
                    if (plainSize == -1) break
                    require(plainSize in 1..BLOCK_SIZE)
                    val nonce = source.readExact(NONCE_SIZE)
                    val encryptedSize = source.readInt()
                    require(encryptedSize in 1..BLOCK_SIZE + 16) { "Invalid block" }
                    val encrypted = source.readExact(encryptedSize)
                    val plain = crypt(Cipher.DECRYPT_MODE, dataKey, nonce, encrypted)
                    require(plain.size == plainSize)
                    output.write(plain)
                }
            } finally {
                dataKey.fill(0)
            }
        }
    }

    private fun nonce() = ByteArray(NONCE_SIZE).also(random::nextBytes)

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun crypt(mode: Int, key: ByteArray, nonce: ByteArray, bytes: ByteArray): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            doFinal(bytes)
        }

    private fun InputStream.readChunk(buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val count = read(buffer, offset, buffer.size - offset)
            if (count == -1) break
            if (count == 0) continue
            offset += count
        }
        return offset
    }

    private fun DataInputStream.readExact(size: Int) = ByteArray(size).also(::readFully)
}
