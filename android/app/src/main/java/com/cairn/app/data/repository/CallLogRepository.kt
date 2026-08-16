package com.cairn.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.cairn.app.data.local.dao.CallLogDao
import com.cairn.app.data.local.dao.PeriodCount
import com.cairn.app.data.local.db.CairnDatabase
import com.cairn.app.data.local.entity.CallLogEntity
import com.cairn.app.data.local.entity.CallType
import com.cairn.app.domain.usecase.SearchQueryParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

data class DashboardStats(
    val totalCalls: Long,
    val oldestRecordEpoch: Long?,
    val totalDurationSeconds: Long,
    val averageDurationSeconds: Double,
    val longestCall: CallLogEntity?,
    val mostContactedContactId: Long?,
    val monthlyActivity: List<PeriodCount>,
    val yearlyActivity: List<PeriodCount>
)

private const val PAGE_SIZE = 50

@Singleton
class CallLogRepository @Inject constructor(
    private val dao: CallLogDao,
    private val database: CairnDatabase
) {
    private fun pager(source: () -> androidx.paging.PagingSource<Int, CallLogEntity>) =
        Pager(PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PAGE_SIZE, enablePlaceholders = false)) {
            source()
        }.flow

    fun timeline(): Flow<PagingData<CallLogEntity>> = pager { dao.pagingAll() }

    fun forContact(contactId: Long): Flow<PagingData<CallLogEntity>> = pager { dao.pagingForContact(contactId) }

    fun forRange(start: LocalDate, end: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<PagingData<CallLogEntity>> {
        val startEpoch = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endEpoch = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return pager { dao.pagingForRange(startEpoch, endEpoch) }
    }

    fun byType(type: CallType): Flow<PagingData<CallLogEntity>> = pager { dao.pagingByType(type) }

    /** The single entry point for the Search screen — parses natural language, then runs one indexed query. */
    fun search(rawQuery: String, nowYear: Int = LocalDate.now().year): Flow<PagingData<CallLogEntity>> {
        val parsed = SearchQueryParser.parse(rawQuery, nowYear)
        val hasStructuredFilters = parsed.callType != null || parsed.yearMonth != null ||
            parsed.year != null || parsed.minDurationSeconds != null ||
            parsed.endsWithDigits != null || parsed.startsWithDigits != null

        return if (hasStructuredFilters) {
            val sql = SearchQueryParser.buildRawQuery(parsed)
            pager { dao.pagingFiltered(sql) }
        } else {
            val ftsTokens = rawQuery.trim().split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replace(Regex("[^a-zA-Z0-9]"), "") + "*" }
            pager { dao.pagingSearch(ftsTokens.ifBlank { "*" }) }
        }
    }

    suspend fun addOrUpdateNote(callId: Long, note: String?) = dao.setNote(callId, note)

    suspend fun insertArchived(entry: CallLogEntity) = dao.insert(entry)

    /** Batched insert used by the initial full-history import and by the archival worker. */
    suspend fun insertBatch(entries: List<CallLogEntity>, batchSize: Int = 500) {
        entries.chunked(batchSize).forEach { chunk -> dao.insertAll(chunk) }
    }

    suspend fun dashboardStats(): DashboardStats {
        val total = dao.totalCount()
        val oldest = dao.oldestTimestamp()
        val totalDuration = dao.totalDurationSeconds() ?: 0
        val avgDuration = dao.averageDurationSeconds() ?: 0.0
        val longest = dao.longestCall()
        val mostContacted = dao.mostContactedContactId()?.contactId
        val since = LocalDate.now().minusMonths(12).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val monthly = dao.monthlyActivity(since)
        val yearly = dao.yearlyActivity()

        return DashboardStats(
            totalCalls = total,
            oldestRecordEpoch = oldest,
            totalDurationSeconds = totalDuration,
            averageDurationSeconds = avgDuration,
            longestCall = longest,
            mostContactedContactId = mostContacted,
            monthlyActivity = monthly,
            yearlyActivity = yearly
        )
    }

    suspend fun availableYears(): List<String> = dao.availableYears()

    suspend fun runIntegrityCheck(): Boolean = withContext(Dispatchers.IO) {
        database.runIntegrityCheckBlocking()
    }

    suspend fun vacuum() = withContext(Dispatchers.IO) {
        database.vacuumBlocking()
    }
}
