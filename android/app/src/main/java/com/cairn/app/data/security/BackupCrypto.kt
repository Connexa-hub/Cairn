package com.cairn.app.data.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Zero-knowledge backup encryption. The server (Render) only ever receives
 * the output of [encrypt] — a header of {salt, nonce, iterations} followed
 * by AES-256-GCM ciphertext. There is no code path, on-device or server-
 * side, that can decrypt a backup without the user's passphrase: it is
 * never transmitted, cached, or logged.
 *
 * If the user forgets their backup passphrase, the backup is permanently
 * unrecoverable — this is a deliberate trade-off stated in onboarding.
 */
object BackupCrypto {

    private const val PBKDF2_ITERATIONS = 600_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12

    data class EncryptedBackup(
        val salt: ByteArray,
        val nonce: ByteArray,
        val iterations: Int,
        val ciphertext: ByteArray
    ) {
        /** Serialized as: [4B iterations][1B saltLen][salt][1B nonceLen][nonce][ciphertext] */
        fun toByteArray(): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            java.io.DataOutputStream(out).use { d ->
                d.writeInt(iterations)
                d.writeByte(salt.size); d.write(salt)
                d.writeByte(nonce.size); d.write(nonce)
                d.write(ciphertext)
            }
            return out.toByteArray()
        }

        companion object {
            fun fromByteArray(bytes: ByteArray): EncryptedBackup {
                val din = java.io.DataInputStream(bytes.inputStream())
                val iterations = din.readInt()
                val saltLen = din.readByte().toInt(); val salt = ByteArray(saltLen).also { din.readFully(it) }
                val nonceLen = din.readByte().toInt(); val nonce = ByteArray(nonceLen).also { din.readFully(it) }
                val ciphertext = din.readBytes()
                return EncryptedBackup(salt, nonce, iterations, ciphertext)
            }
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plaintext: ByteArray, passphrase: CharArray): EncryptedBackup {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val nonce = ByteArray(NONCE_BYTES).also { random.nextBytes(it) }
        val key = deriveKey(passphrase, salt, PBKDF2_ITERATIONS)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        val ciphertext = cipher.doFinal(plaintext)

        return EncryptedBackup(salt, nonce, PBKDF2_ITERATIONS, ciphertext)
    }

    fun decrypt(backup: EncryptedBackup, passphrase: CharArray): ByteArray {
        val key = deriveKey(passphrase, backup.salt, backup.iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, backup.nonce))
        return cipher.doFinal(backup.ciphertext)
    }
}
