package com.cairn.app.data.backup

import android.content.Context
import com.cairn.app.data.security.BackupCrypto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the whole backup lifecycle. The API layer only ever sees
 * bytes that already went through [BackupCrypto.encrypt] — this class is
 * the single choke point where plaintext DB files exist only transiently
 * on local disk, never on the wire.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: BackupApi
) {
    private val backupDir get() = File(context.filesDir, "backups").apply { mkdirs() }

    /** Step 1: snapshot the (already-encrypted-at-rest) DB file to a plain export file for compression/re-encryption. */
    fun exportDatabaseFile(dbFile: File): File {
        val target = File(backupDir, "export_${System.currentTimeMillis()}.db")
        dbFile.copyTo(target, overwrite = true)
        return target
    }

    /** Step 2: encrypt the export with the user's backup passphrase (separate from the device unlock secret). */
    suspend fun createEncryptedBackup(dbFile: File, passphrase: CharArray): File = withContext(Dispatchers.IO) {
        val plain = exportDatabaseFile(dbFile)
        val encrypted = BackupCrypto.encrypt(plain.readBytes(), passphrase)
        val outFile = File(backupDir, "cairn_backup_${System.currentTimeMillis()}.enc")
        outFile.writeBytes(encrypted.toByteArray())
        plain.delete() // never leave a plaintext copy on disk longer than necessary
        outFile
    }

    /** Step 3 (optional): upload the encrypted blob to the user's own backend, if cloud backup is enabled. */
    suspend fun uploadBackup(encryptedFile: File, bearer: String, deviceName: String): BackupMeta =
        withContext(Dispatchers.IO) {
            val part = MultipartBody.Part.createFormData(
                "file", encryptedFile.name,
                encryptedFile.asRequestBody("application/octet-stream".toMediaType())
            )
            val deviceBody = deviceName.toRequestBody("text/plain".toMediaType())
            api.upload("Bearer $bearer", part, deviceBody)
        }

    suspend fun listRemoteBackups(bearer: String): List<BackupMeta> =
        withContext(Dispatchers.IO) { api.list("Bearer $bearer") }

    suspend fun downloadBackup(bearer: String, id: String): File = withContext(Dispatchers.IO) {
        val body = api.download("Bearer $bearer", id)
        val out = File(backupDir, "restore_$id.enc")
        body.byteStream().use { input -> out.outputStream().use { input.copyTo(it) } }
        out
    }

    suspend fun deleteRemoteBackup(bearer: String, id: String) =
        withContext(Dispatchers.IO) { api.delete("Bearer $bearer", id) }

    /** Restore: decrypt with the passphrase and hand the plaintext DB bytes back for a transactional swap-in. */
    suspend fun decryptBackup(encryptedFile: File, passphrase: CharArray): ByteArray =
        withContext(Dispatchers.IO) {
            val encrypted = com.cairn.app.data.security.BackupCrypto.EncryptedBackup.fromByteArray(
                encryptedFile.readBytes()
            )
            BackupCrypto.decrypt(encrypted, passphrase)
        }

    fun listLocalBackups(): List<File> = backupDir.listFiles { f -> f.extension == "enc" }?.toList().orEmpty()
}
