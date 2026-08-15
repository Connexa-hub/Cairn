package com.cairn.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cairn.app.data.local.entity.CallLogEntity
import com.cairn.app.data.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * 120ms debounce keeps search feeling instant while avoiding a query
     * dispatch on every single keystroke during fast typing.
     */
    val results: Flow<PagingData<CallLogEntity>> = _query
        .debounce(120)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) emptyFlow() else callLogRepository.search(q)
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    // Surfaced in the UI as helper chips so users discover the query language.
    val exampleQueries = listOf(
        "David 2023",
        "missed calls in March",
        "calls ending in 4421",
        "calls longer than 20 minutes",
        "outgoing this week"
    )
}
