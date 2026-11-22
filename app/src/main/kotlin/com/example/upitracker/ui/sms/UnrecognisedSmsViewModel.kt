package com.example.upitracker.ui.sms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.sms.UnrecognisedSmsItem
import com.example.upitracker.data.sms.UnrecognisedSmsScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UnrecognisedSmsUiState {
    data object Loading : UnrecognisedSmsUiState
    data object NoPermission : UnrecognisedSmsUiState
    data class Success(val items: List<UnrecognisedSmsItem>) : UnrecognisedSmsUiState
}

class UnrecognisedSmsViewModel(
    private val scanner: UnrecognisedSmsScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow<UnrecognisedSmsUiState>(UnrecognisedSmsUiState.Loading)
    val uiState: StateFlow<UnrecognisedSmsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = UnrecognisedSmsUiState.Loading
            when (val result = scanner.scan()) {
                is UnrecognisedSmsScanner.ScanResult.NoPermission -> {
                    _uiState.value = UnrecognisedSmsUiState.NoPermission
                }
                is UnrecognisedSmsScanner.ScanResult.Success -> {
                    _uiState.value = UnrecognisedSmsUiState.Success(result.items)
                }
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(UnrecognisedSmsViewModel::class.java)) {
                        return UnrecognisedSmsViewModel(UnrecognisedSmsScanner(context)) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
