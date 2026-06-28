package com.sonar.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonar.application.usecases.*
import com.sonar.domain.entities.Recording
import com.sonar.domain.entities.RecordingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val recordings: List<Recording> = emptyList(),
    val isLoading: Boolean = false,
    val currentRecordingId: UUID? = null,
    val currentDurationMs: Long = 0L,
    val error: String? = null
)

sealed class HomeEvent {
    object LoadRecordings : HomeEvent()
    data class StartRecording(val result: Result<Recording>) : HomeEvent()
    data class StopRecording(val result: Result<Recording>) : HomeEvent()
    data class DeleteRecording(val id: UUID) : HomeEvent()
    data class AnalyzeRecording(val id: UUID) : HomeEvent()
    data class SelectRecording(val id: UUID) : HomeEvent()
    data class Error(val message: String) : HomeEvent()
    object DismissError : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
    private val getRecordingsUseCase: GetRecordingsUseCase,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val analyzeRecordingUseCase: AnalyzeRecordingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        observeRecordings()
    }

    private fun observeRecordings() {
        viewModelScope.launch {
            getRecordingsUseCase.observe().collect { recordings ->
                _uiState.update { it.copy(recordings = recordings) }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadRecordings -> loadRecordings()
            is HomeEvent.StartRecording -> handleStartResult(event.result)
            is HomeEvent.StopRecording -> handleStopResult(event.result)
            is HomeEvent.DeleteRecording -> delete(event.id)
            is HomeEvent.AnalyzeRecording -> analyze(event.id)
            is HomeEvent.SelectRecording -> selectRecording(event.id)
            is HomeEvent.Error -> _uiState.update { it.copy(error = event.message) }
            is HomeEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = startRecordingUseCase()
            _uiState.update { it.copy(isLoading = false) }
            onEvent(HomeEvent.StartRecording(result))
        }
    }

    fun stopRecording() {
        val currentId = _uiState.value.currentRecordingId ?: return
        timerJob?.cancel()
        timerJob = null
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentDurationMs = 0L) }
            val result = stopRecordingUseCase(currentId)
            _uiState.update { it.copy(isLoading = false, currentRecordingId = null) }
            onEvent(HomeEvent.StopRecording(result))
        }
    }

    private fun startRecordingTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                _uiState.update {
                    it.copy(currentDurationMs = System.currentTimeMillis() - startTime)
                }
                delay(1000)
            }
        }
    }

    private fun loadRecordings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val recordings = getRecordingsUseCase()
                _uiState.update { it.copy(recordings = recordings, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun delete(id: UUID) {
        viewModelScope.launch {
            deleteRecordingUseCase(id)
        }
    }

    private fun analyze(id: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = analyzeRecordingUseCase(id)
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
        }
    }

    private fun selectRecording(id: UUID) {
        _uiState.update { it.copy(currentRecordingId = id) }
    }

    private fun handleStartResult(result: Result<Recording>) {
        result.fold(
            onSuccess = { recording ->
                _uiState.update { it.copy(currentRecordingId = recording.id, currentDurationMs = 0L) }
                startRecordingTimer()
            },
            onFailure = { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        )
    }

    private fun handleStopResult(result: Result<Recording>) {
        result.fold(
            onSuccess = { /* Recording saved */ },
            onFailure = { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        )
    }
}
