package com.cairn.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Fires on every call state change. Rather than parse the call here (the
 * system call log row isn't finalized until the call actually ends), we
 * just nudge [CallArchiveWorker] to reconcile: it re-reads the Android
 * call log for anything newer than our last-archived timestamp and
 * appends it to the permanent Room archive. This is what lets the archive
 * survive the user later clearing their system call log.
 */
class CallLogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val request = OneTimeWorkRequestBuilder<CallArchiveWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("call_archive_reconcile", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
