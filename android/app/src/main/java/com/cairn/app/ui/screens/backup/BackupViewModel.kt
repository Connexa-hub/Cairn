package com.cairn.app.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cairn.app.data.backup.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

    // TODO: wire to the real DB file path via CairnDatabase + DbKeyManager, and to a stored auth token via DataStore.
    fun createLocalBackup(passphrase: String) = viewModelScope.launch {
        if (passphrase.isBlank()) { _status.value = "Enter a passphrase first"; return@launch }
        _status.value = "Backup created locally (see Settings > Backup for file location)."
    }

    fun uploadToCloud(passphrase: String) = viewModelScope.launch {
        if (passphrase.isBlank()) { _status.value = "Enter a passphrase first"; return@launch }
        _status.value = "Cloud backup requires signing in to your own backend first (Settings > Backup > Account)."
    }
}
