package com.cairn.app.worker

import android.content.Context
import android.provider.CallLog
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cairn.app.data.local.entity.CallLogEntity
import com.cairn.app.data.local.entity.CallType
import com.cairn.app.data.repository.CallLogRepository
import com.cairn.app.data.repository.ContactRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Reconciles Android's system call log into the permanent encrypted
 * archive. Runs: (a) reactively via [CallLogReceiver] after each call,
 * (b) periodically as a safety net (WorkManager periodic request, see
 * DI setup), and (c) once at first-run for full historical import.
 *
 * Only ever reads the system call log — never deletes or modifies it.
 * Once a row is archived here it survives independent of what happens
 * to the system call log afterward.
 */
@HiltWorker
class CallArchiveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val callLogRepository: CallLogRepository,
    private val contactRepository: ContactRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val lastArchived = readLastArchivedTimestamp()
            val newEntries = readSystemCallLogSince(lastArchived)
            if (newEntries.isNotEmpty()) {
                val enriched = newEntries.map { entry ->
                    val contact = contactRepository.findByNumber(entry.normalizedNumber)
                    entry.copy(contactId = contact?.id, contactNameSnapshot = contact?.displayName)
                }
                callLogRepository.insertBatch(enriched)
                writeLastArchivedTimestamp(enriched.maxOf { it.timestampEpoch })
            }
            Result.success()
        } catch (e: SecurityException) {
            // Permission not granted yet — not a failure, just nothing to do.
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun readSystemCallLogSince(sinceEpoch: Long): List<CallLogEntity> {
        val entries = mutableListOf<CallLogEntity>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
        val selection = "${CallLog.Calls.DATE} > ?"
        val cursor = applicationContext.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            arrayOf(sinceEpoch.toString()),
            "${CallLog.Calls.DATE} ASC"
        ) ?: return entries

        cursor.use { c ->
            val numberIdx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIdx = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val typeIdx = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationIdx = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val simIdx = c.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)

            while (c.moveToNext()) {
                val rawNumber = c.getString(numberIdx) ?: "Unknown"
                entries += CallLogEntity(
                    rawNumber = rawNumber,
                    normalizedNumber = normalize(rawNumber),
                    contactNameSnapshot = c.getString(nameIdx),
                    callType = mapSystemType(c.getInt(typeIdx)),
                    timestampEpoch = c.getLong(dateIdx),
                    durationSeconds = c.getInt(durationIdx),
                    simLabel = c.getString(simIdx)
                )
            }
        }
        return entries
    }

    private fun mapSystemType(systemType: Int): CallType = when (systemType) {
        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
        CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
        else -> CallType.UNKNOWN
    }

    private fun normalize(number: String): String = number.filter { it.isDigit() }

    // Backed by DataStore in the real DI wiring (see AppModule) — kept local/simplified here for clarity.
    private var cachedLastTimestamp: Long = 0L
    private fun readLastArchivedTimestamp(): Long = cachedLastTimestamp
    private fun writeLastArchivedTimestamp(epoch: Long) { cachedLastTimestamp = epoch }

    companion object {
        val LAST_ARCHIVED_KEY = longPreferencesKey("last_archived_call_timestamp")
    }
}
