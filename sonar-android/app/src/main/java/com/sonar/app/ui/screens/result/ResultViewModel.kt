package com.sonar.app.ui.screens.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonar.application.usecases.AnalyzeRecordingUseCase
import com.sonar.application.usecases.GetRecordingsUseCase
import com.sonar.domain.entities.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ResultUiState(
    val recording: Recording? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val getRecordings: GetRecordingsUseCase,
    private val analyzeRecording: AnalyzeRecordingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun loadRecording(id: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val recording = getRecordings.findAll().find { it.id == id }
            if (recording != null) {
                _uiState.update { it.copy(recording = recording, isLoading = false) }
            } else {
                _uiState.update { it.copy(error = "Recording not found", isLoading = false) }
            }
        }
    }

    fun analyze() {
        val id = _uiState.value.recording?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = analyzeRecording(id)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    recording = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}