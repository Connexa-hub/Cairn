package com.cairn.app.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cairn.app.data.local.entity.CallLogEntity
import com.cairn.app.data.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TimelineGranularity { DAY, WEEK, MONTH, YEAR, ALL }

data class TimelineUiState(
    val availableYears: List<String> = emptyList(),
    val selectedYear: String? = null,
    val granularity: TimelineGranularity = TimelineGranularity.ALL
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private val _rangeSelection = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)

    val calls: Flow<PagingData<CallLogEntity>> = _rangeSelection
        .flatMapLatest { range ->
            if (range == null) callLogRepository.timeline()
            else callLogRepository.forRange(range.first, range.second)
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            val years = callLogRepository.availableYears()
            _uiState.update { it.copy(availableYears = years) }
        }
    }

    /** "Jump to 2021" — the year rail affordance described in the spec. */
    fun jumpToYear(year: String) {
        val y = year.toIntOrNull() ?: return
        _rangeSelection.value = LocalDate.of(y, 1, 1) to LocalDate.of(y, 12, 31)
        _uiState.update { it.copy(selectedYear = year, granularity = TimelineGranularity.YEAR) }
    }

    fun jumpToToday() {
        val today = LocalDate.now()
        _rangeSelection.value = today to today
        _uiState.update { it.copy(selectedYear = null, granularity = TimelineGranularity.DAY) }
    }

    fun jumpToThisWeek() {
        val today = LocalDate.now()
        _rangeSelection.value = today.minusDays(6) to today
        _uiState.update { it.copy(selectedYear = null, granularity = TimelineGranularity.WEEK) }
    }

    fun jumpToThisMonth() {
        val today = LocalDate.now()
        _rangeSelection.value = today.withDayOfMonth(1) to today
        _uiState.update { it.copy(selectedYear = null, granularity = TimelineGranularity.MONTH) }
    }

    fun clearRange() {
        _rangeSelection.value = null
        _uiState.update { it.copy(selectedYear = null, granularity = TimelineGranularity.ALL) }
    }
}
