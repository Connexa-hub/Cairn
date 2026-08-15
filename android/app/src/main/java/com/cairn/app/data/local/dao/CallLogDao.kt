package com.cairn.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.cairn.app.data.local.entity.CallLogEntity
import com.cairn.app.data.local.entity.CallLogFts
import com.cairn.app.data.local.entity.CallType
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(call: CallLogEntity): Long

    /** Bulk archival insert path — used by the initial import and the WorkManager archiver, batched ~500/txn by the caller. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(calls: List<CallLogEntity>): List<Long>

    @Update
    suspend fun update(call: CallLogEntity)

    @Query("UPDATE call_logs SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String?)

    @Query("SELECT * FROM call_logs WHERE id = :id")
    suspend fun getById(id: Long): CallLogEntity?

    @Query("SELECT * FROM call_logs WHERE id = :id")
    fun observeById(id: Long): Flow<CallLogEntity?>

    /** Primary timeline feed: reverse-chronological, keyset-paginated via Room Paging 3 (no OFFSET scans). */
    @Query("SELECT * FROM call_logs ORDER BY timestampEpoch DESC, id DESC")
    fun pagingAll(): PagingSource<Int, CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE contactId = :contactId ORDER BY timestampEpoch DESC")
    fun pagingForContact(contactId: Long): PagingSource<Int, CallLogEntity>

    @Query("""
        SELECT * FROM call_logs
        WHERE timestampEpoch BETWEEN :startEpoch AND :endEpoch
        ORDER BY timestampEpoch DESC
    """)
    fun pagingForRange(startEpoch: Long, endEpoch: Long): PagingSource<Int, CallLogEntity>

    /**
     * FTS search over name/number/note. The `query` should already be FTS-escaped
     * (see SearchQueryParser) — e.g. wrapped tokens with `*` for prefix match.
     */
    @Query("""
        SELECT call_logs.* FROM call_logs
        JOIN call_logs_fts ON call_logs.id = call_logs_fts.rowid
        WHERE call_logs_fts MATCH :ftsQuery
        ORDER BY call_logs.timestampEpoch DESC
    """)
    fun pagingSearch(ftsQuery: String): PagingSource<Int, CallLogEntity>

    /** Structured filter search — built dynamically by SearchQueryParser + QueryBuilder for combined filters
     * like "missed calls in March longer than 1 minute ending in 4421". */
    @RawQuery(observedEntities = [CallLogEntity::class])
    fun pagingFiltered(query: SupportSQLiteQuery): PagingSource<Int, CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE normalizedNumber LIKE '%' || :lastDigits ORDER BY timestampEpoch DESC")
    fun pagingEndsWith(lastDigits: String): PagingSource<Int, CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE normalizedNumber LIKE :firstDigits || '%' ORDER BY timestampEpoch DESC")
    fun pagingStartsWith(firstDigits: String): PagingSource<Int, CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE callType = :type ORDER BY timestampEpoch DESC")
    fun pagingByType(type: CallType): PagingSource<Int, CallLogEntity>

    // ---- Dashboard / stats (kept cheap via stats_cache + narrow aggregate queries) ----

    @Query("SELECT COUNT(*) FROM call_logs")
    suspend fun totalCount(): Long

    @Query("SELECT MIN(timestampEpoch) FROM call_logs")
    suspend fun oldestTimestamp(): Long?

    @Query("SELECT SUM(durationSeconds) FROM call_logs")
    suspend fun totalDurationSeconds(): Long?

    @Query("SELECT AVG(durationSeconds) FROM call_logs WHERE durationSeconds > 0")
    suspend fun averageDurationSeconds(): Double?

    @Query("""
        SELECT * FROM call_logs
        WHERE durationSeconds = (SELECT MAX(durationSeconds) FROM call_logs)
        LIMIT 1
    """)
    suspend fun longestCall(): CallLogEntity?

    @Query("""
        SELECT contactId, COUNT(*) as cnt FROM call_logs
        WHERE contactId IS NOT NULL
        GROUP BY contactId ORDER BY cnt DESC LIMIT 1
    """)
    suspend fun mostContactedContactId(): ContactCallCount?

    @Query("""
        SELECT strftime('%Y-%m', timestampEpoch / 1000, 'unixepoch') as period, COUNT(*) as cnt
        FROM call_logs
        WHERE timestampEpoch >= :sinceEpoch
        GROUP BY period ORDER BY period ASC
    """)
    suspend fun monthlyActivity(sinceEpoch: Long): List<PeriodCount>

    @Query("""
        SELECT strftime('%Y', timestampEpoch / 1000, 'unixepoch') as period, COUNT(*) as cnt
        FROM call_logs
        GROUP BY period ORDER BY period ASC
    """)
    suspend fun yearlyActivity(): List<PeriodCount>

    @Query("""
        SELECT strftime('%Y-%m-%d', timestampEpoch / 1000, 'unixepoch') as period, COUNT(*) as cnt
        FROM call_logs
        WHERE timestampEpoch BETWEEN :startEpoch AND :endEpoch
        GROUP BY period
    """)
    suspend fun dailyHeatmap(startEpoch: Long, endEpoch: Long): List<PeriodCount>

    @Query("SELECT DISTINCT strftime('%Y', timestampEpoch / 1000, 'unixepoch') FROM call_logs ORDER BY 1 DESC")
    suspend fun availableYears(): List<String>

    // ---- Maintenance ----

    @Query("PRAGMA integrity_check")
    suspend fun integrityCheck(): List<String>

    @Query("VACUUM")
    suspend fun vacuum()
}

data class ContactCallCount(val contactId: Long, val cnt: Int)
data class PeriodCount(val period: String, val cnt: Int)
