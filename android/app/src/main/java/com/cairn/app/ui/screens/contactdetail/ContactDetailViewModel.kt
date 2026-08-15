package com.cairn.app.ui.screens.contactdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.cairn.app.data.repository.CallLogRepository
import com.cairn.app.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository
) : ViewModel() {

    private val contactId: Long = savedStateHandle.get<Long>("contactId") ?: 0L

    val contact = contactRepository.contact(contactId)
    val numbers = contactRepository.numbersFor(contactId)
    val emails = contactRepository.emailsFor(contactId)
    val addresses = contactRepository.addressesFor(contactId)
    val notes = contactRepository.notesFor(contactId)
    val calls = callLogRepository.forContact(contactId).cachedIn(viewModelScope)

    fun toggleFavorite(current: Boolean) = viewModelScope.launch {
        contactRepository.toggleFavorite(contactId, !current)
    }

    fun addNote(body: String) = viewModelScope.launch {
        if (body.isNotBlank()) contactRepository.addNote(contactId, body)
    }
}
