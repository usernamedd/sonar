package com.sonar.app.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val whisperApiKey: String = "",
    val geminiApiKey: String = "",
    val searchApiKey: String = "",
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("sonar_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                whisperApiKey = prefs.getString(KEY_WHISPER, "") ?: "",
                geminiApiKey = prefs.getString(KEY_GEMINI, "") ?: "",
                searchApiKey = prefs.getString(KEY_SEARCH, "") ?: ""
            )
        }
    }

    fun updateWhisperKey(key: String) {
        _uiState.update { it.copy(whisperApiKey = key, saved = false) }
    }

    fun updateGeminiKey(key: String) {
        _uiState.update { it.copy(geminiApiKey = key, saved = false) }
    }

    fun updateSearchKey(key: String) {
        _uiState.update { it.copy(searchApiKey = key, saved = false) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            prefs.edit()
                .putString(KEY_WHISPER, _uiState.value.whisperApiKey)
                .putString(KEY_GEMINI, _uiState.value.geminiApiKey)
                .putString(KEY_SEARCH, _uiState.value.searchApiKey)
                .apply()
            _uiState.update { it.copy(saved = true) }
        }
    }

    companion object {
        private const val KEY_WHISPER = "whisper_api_key"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_SEARCH = "search_api_key"
    }
}
