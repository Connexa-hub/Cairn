package com.cairn.app.ui.screens.tags

import androidx.lifecycle.ViewModel
import com.cairn.app.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(
    contactRepository: ContactRepository
) : ViewModel() {
    val tags = contactRepository.tags()
}
