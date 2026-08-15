package com.cairn.app.ui.screens.calldetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cairn.app.data.local.dao.CallLogDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val callLogDao: CallLogDao
) : ViewModel() {
    private val callId: Long = savedStateHandle.get<Long>("callId") ?: 0L
    val call = callLogDao.observeById(callId)

    fun saveNote(note: String) = viewModelScope.launch {
        callLogDao.setNote(callId, note.ifBlank { null })
    }
}
