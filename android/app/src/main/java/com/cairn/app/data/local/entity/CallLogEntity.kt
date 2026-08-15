package com.cairn.app.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, UNKNOWN
}

/**
 * The permanent archive row. Once written, call_logs rows are treated as
 * append-mostly: only `note` and `contactId` (on re-link) are ever updated.
 * This is the table millions of rows accumulate in over years.
 */
@Entity(
    tableName = "call_logs",
    indices = [
        Index("timestampEpoch"),
        Index("normalizedNumber"),
        Index("contactId"),
        Index(value = ["callType", "timestampEpoch"]),
        // Covering index for the hot path: reverse-chronological keyset pagination
        Index(value = ["timestampEpoch", "id"])
    ]
)
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long? = null,
    val rawNumber: String,
    val normalizedNumber: String,
    val callType: CallType,
    val timestampEpoch: Long,
    val durationSeconds: Int = 0,
    val simSlot: Int? = null,
    val simLabel: String? = null,
    val note: String? = null,
    /** Snapshot of the contact's name at archive-time, so history stays readable even if the contact is later deleted/renamed */
    val contactNameSnapshot: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * FTS4 content table mirroring the searchable text columns of call_logs.
 * `content=call_logs` means Room keeps this in sync via triggers and it
 * costs no extra storage for the mirrored columns beyond the index itself.
 */
@Fts4(contentEntity = CallLogEntity::class)
@Entity(tableName = "call_logs_fts")
data class CallLogFts(
    val rawNumber: String,
    val normalizedNumber: String,
    val contactNameSnapshot: String?,
    val note: String?
)

/** Lightweight incrementally-maintained counters so Dashboard never runs COUNT(*) over millions of rows. */
@Entity(tableName = "stats_cache")
data class StatsCacheEntity(
    @PrimaryKey val key: String, // e.g. "total_calls", "total_duration_seconds"
    val longValue: Long = 0
)
