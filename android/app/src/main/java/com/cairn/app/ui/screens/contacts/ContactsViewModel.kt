package com.cairn.app.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.cairn.app.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    contactRepository: ContactRepository
) : ViewModel() {
    val contacts = contactRepository.pagedContacts().cachedIn(viewModelScope)
}
