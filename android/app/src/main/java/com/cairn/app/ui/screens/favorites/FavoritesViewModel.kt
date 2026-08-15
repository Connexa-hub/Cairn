package com.cairn.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import com.cairn.app.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    contactRepository: ContactRepository
) : ViewModel() {
    val favorites = contactRepository.favorites()
}
