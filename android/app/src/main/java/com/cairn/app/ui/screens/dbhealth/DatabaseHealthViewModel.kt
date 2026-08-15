package com.cairn.app.ui.screens.dbhealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cairn.app.data.repository.CallLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DbHealthState(val integrityStatus: String = "Not checked yet")

@HiltViewModel
class DatabaseHealthViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DbHealthState())
    val state: StateFlow<DbHealthState> = _state

    fun runIntegrityCheck() = viewModelScope.launch {
        val ok = callLogRepository.runIntegrityCheck()
        _state.value = DbHealthState(if (ok) "OK — no corruption detected" else "Issue detected — restore from backup recommended")
    }

    fun vacuum() = viewModelScope.launch {
        callLogRepository.vacuum()
        _state.value = DbHealthState("Optimized")
    }
}
