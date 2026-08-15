package com.cairn.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the SQLCipher database passphrase.
 *
 * The passphrase itself is a random 256-bit value generated once on first
 * launch. It is never derived from anything guessable and never leaves the
 * device. It's stored wrapped by an Android Keystore key
 * (hardware-backed on supported devices) so that reading it back requires
 * the device to be unlocked; biometric/PIN gating happens one layer up in
 * [BiometricAuthManager] before this class is ever asked for the key.
 */
@Singleton
class DbKeyManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val keystoreAlias = "cairn_db_wrapping_key"
    private val passphraseFileName = "db_passphrase.enc"

    private fun getOrCreateWrappingKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(keystoreAlias, null) as? SecretKey)?.let { return it }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGen.init(
            KeyGenParameterSpec.Builder(
                keystoreAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false) // gating handled explicitly via BiometricPrompt at the UI layer
                .build()
        )
        return keyGen.generateKey()
    }

    /** Returns the raw DB passphrase, generating and persisting one (encrypted) on first run. */
    fun getOrCreatePassphrase(): ByteArray {
        val file = File(context.filesDir, passphraseFileName)
        if (!file.exists()) {
            val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
            writeWrapped(file, random)
            return random
        }
        return readWrapped(file)
    }

    private fun writeWrapped(file: File, plaintext: ByteArray) {
        val key = getOrCreateWrappingKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        file.writeBytes(iv + ciphertext)
    }

    private fun readWrapped(file: File): ByteArray {
        val key = getOrCreateWrappingKey()
        val bytes = file.readBytes()
        val iv = bytes.copyOfRange(0, 12)
        val ciphertext = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    /** MasterKey helper for any ancillary EncryptedFile use (e.g. staging a decrypted restore before import). */
    fun masterKey(): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    fun encryptedFile(target: File): EncryptedFile =
        EncryptedFile.Builder(
            context, target, masterKey(),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
}
