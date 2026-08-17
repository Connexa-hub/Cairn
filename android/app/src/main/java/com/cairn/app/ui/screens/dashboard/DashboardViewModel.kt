package com.cairn.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cairn.app.data.local.entity.ContactEntity
import com.cairn.app.data.repository.CallLogRepository
import com.cairn.app.data.repository.ContactRepository
import com.cairn.app.data.repository.DashboardStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val stats: DashboardStats? = null,
    val mostContactedName: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState(loading = true)
            val stats = callLogRepository.dashboardStats()
            val contactName = stats.mostContactedContactId?.let { id ->
                var name: String? = null
                contactRepository.contact(id).collectLatestOnce { c: ContactEntity? -> name = c?.displayName }
                name
            }
            _uiState.value = DashboardUiState(loading = false, stats = stats, mostContactedName = contactName)
        }
    }
}

/** Small helper: take the first emission of a Flow without pulling in extra deps. */
private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.collectLatestOnce(action: suspend (T) -> Unit) {
    action(this.first())
}
