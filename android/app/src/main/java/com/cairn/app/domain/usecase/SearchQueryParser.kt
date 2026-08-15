package com.cairn.app.domain.usecase

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.cairn.app.data.local.entity.CallType
import java.text.Normalizer
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * Turns a free-text query like:
 *   "David 2023", "calls ending in 4421", "missed calls in March",
 *   "calls longer than 20 minutes"
 * into a structured [ParsedSearch], which [buildRawQuery] turns into a
 * single indexed SQL query against `call_logs` (+ FTS for the free-text
 * remainder). This keeps search feeling instant even on millions of rows
 * because every clause maps to an existing index.
 */
data class ParsedSearch(
    val freeText: String? = null,
    val callType: CallType? = null,
    val yearMonth: YearMonth? = null,
    val year: Int? = null,
    val minDurationSeconds: Int? = null,
    val maxDurationSeconds: Int? = null,
    val endsWithDigits: String? = null,
    val startsWithDigits: String? = null
)

object SearchQueryParser {

    private val callTypeWords = mapOf(
        "missed" to CallType.MISSED,
        "incoming" to CallType.INCOMING,
        "outgoing" to CallType.OUTGOING,
        "rejected" to CallType.REJECTED,
        "declined" to CallType.REJECTED,
        "blocked" to CallType.BLOCKED,
        "unknown" to CallType.UNKNOWN
    )

    private val monthNames = Month.entries.associateBy { it.name.lowercase(Locale.US) }

    fun parse(input: String, nowYear: Int): ParsedSearch {
        var text = " ${input.trim().lowercase(Locale.US)} "
        var callType: CallType? = null
        var year: Int? = null
        var month: Month? = null
        var minDuration: Int? = null
        var endsWith: String? = null
        var startsWith: String? = null

        for ((word, type) in callTypeWords) {
            if (text.contains(" $word ") || text.contains("$word ")) {
                callType = type
                text = text.replace(word, " ")
            }
        }

        Regex("""\b(19|20)\d{2}\b""").find(text)?.let {
            year = it.value.toInt()
            text = text.replace(it.value, " ")
        }

        for ((name, m) in monthNames) {
            if (text.contains(name)) {
                month = m
                text = text.replace(name, " ")
                break
            }
        }

        Regex("""(longer than|more than|over|>)\s*(\d+)\s*(min|minute|minutes)""").find(text)?.let {
            minDuration = it.groupValues[2].toInt() * 60
            text = text.replace(it.value, " ")
        }

        Regex("""ending in\s*(\d{2,})""").find(text)?.let {
            endsWith = it.groupValues[1]
            text = text.replace(it.value, " ")
        }
        Regex("""starting with\s*(\d{2,})""").find(text)?.let {
            startsWith = it.groupValues[1]
            text = text.replace(it.value, " ")
        }
        // Bare trailing digit run of 4+ with no other context => treat as "ends with"
        if (endsWith == null && startsWith == null) {
            Regex("""\b(\d{4,})\b""").find(text)?.let {
                endsWith = it.value
                text = text.replace(it.value, " ")
            }
        }

        val remaining = text.replace(Regex("\\s+"), " ").trim().ifBlank { null }
        val ym = if (year != null && month != null) YearMonth.of(year, month) else null

        return ParsedSearch(
            freeText = remaining,
            callType = callType,
            yearMonth = ym,
            year = if (ym == null) year else null,
            minDurationSeconds = minDuration,
            endsWithDigits = endsWith,
            startsWithDigits = startsWith
        )
    }

    /** Builds one raw SQL query combining every recognized clause against indexed columns. */
    fun buildRawQuery(parsed: ParsedSearch, zone: ZoneId = ZoneId.systemDefault()): SupportSQLiteQuery {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<Any?>()

        if (parsed.callType != null) {
            clauses += "callType = ?"
            args += parsed.callType.name
        }
        parsed.yearMonth?.let { ym ->
            val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = ym.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            clauses += "timestampEpoch >= ? AND timestampEpoch < ?"
            args += start; args += end
        }
        parsed.year?.let { y ->
            val start = java.time.LocalDate.of(y, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = java.time.LocalDate.of(y + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
            clauses += "timestampEpoch >= ? AND timestampEpoch < ?"
            args += start; args += end
        }
        parsed.minDurationSeconds?.let {
            clauses += "durationSeconds >= ?"
            args += it
        }
        parsed.endsWithDigits?.let {
            clauses += "normalizedNumber LIKE ?"
            args += "%$it"
        }
        parsed.startsWithDigits?.let {
            clauses += "normalizedNumber LIKE ?"
            args += "$it%"
        }

        val base = if (parsed.freeText.isNullOrBlank()) {
            "SELECT * FROM call_logs"
        } else {
            // Prefix-match every token against the FTS mirror for the free-text remainder
            val ftsTokens = parsed.freeText.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { escapeFtsToken(it) + "*" }
            clauses += "id IN (SELECT rowid FROM call_logs_fts WHERE call_logs_fts MATCH ?)"
            args += ftsTokens
            "SELECT * FROM call_logs"
        }

        val where = if (clauses.isEmpty()) "" else " WHERE " + clauses.joinToString(" AND ")
        val sql = "$base$where ORDER BY timestampEpoch DESC"
        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    private fun escapeFtsToken(token: String): String {
        val normalized = Normalizer.normalize(token, Normalizer.Form.NFKD)
        return normalized.replace(Regex("[^a-zA-Z0-9]"), "")
    }
}
