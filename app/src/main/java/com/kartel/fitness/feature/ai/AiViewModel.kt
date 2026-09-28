package com.kartel.fitness.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.DownloadStatus
import com.kartel.fitness.core.ai.ActionRouter
import com.kartel.fitness.core.ai.GeminiNanoAvailability
import com.kartel.fitness.core.ai.GeminiNanoClient
import com.kartel.fitness.core.ai.ModuleIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiUiState(
    val availability: GeminiNanoAvailability = GeminiNanoAvailability.UNAVAILABLE,
    val checking: Boolean = true,
    val downloading: Boolean = false,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val downloadError: String? = null,
    val parsing: Boolean = false,
    val lastResult: ModuleIntent? = null,
    val executionMessage: String? = null,
    val executionNeedsManual: Boolean = false,
)

/** Тестовый экран ядра мини-ИИ (roadmap п.1) — проверка устройства + разбор фраз без голоса пока. */
class AiViewModel(
    private val client: GeminiNanoClient,
    private val actionRouter: ActionRouter,
) : ViewModel() {

    private val _state = MutableStateFlow(AiUiState())
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    fun checkAvailability() {
        viewModelScope.launch {
            _state.value = _state.value.copy(checking = true, downloadError = null)
            val availability = client.checkAvailability()
            _state.value = _state.value.copy(availability = availability, checking = false)
        }
    }

    fun downloadModel() {
        viewModelScope.launch {
            _state.value = _state.value.copy(downloading = true, downloadError = null)
            try {
                client.downloadModel().collect { status ->
                    when (status) {
                        is DownloadStatus.DownloadStarted ->
                            _state.value = _state.value.copy(totalBytes = status.bytesToDownload, downloadedBytes = 0)
                        is DownloadStatus.DownloadProgress ->
                            _state.value = _state.value.copy(downloadedBytes = status.totalBytesDownloaded)
                        is DownloadStatus.DownloadFailed ->
                            _state.value = _state.value.copy(downloading = false, downloadError = status.e.message ?: "Ошибка скачивания")
                        is DownloadStatus.DownloadCompleted ->
                            _state.value = _state.value.copy(downloading = false)
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(downloading = false, downloadError = e.message ?: "Ошибка скачивания")
            }
            checkAvailability()
        }
    }

    fun parse(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(parsing = true, executionMessage = null)
            val result = client.parseVoiceCommand(text)
            val execution = actionRouter.execute(result, text)
            _state.value = _state.value.copy(
                parsing = false,
                lastResult = result,
                executionMessage = when (execution) {
                    is ActionRouter.Result.Success -> execution.message
                    is ActionRouter.Result.NeedsManualInput -> execution.message
                },
                executionNeedsManual = execution is ActionRouter.Result.NeedsManualInput,
            )
        }
    }

    override fun onCleared() {
        client.close()
        super.onCleared()
    }
}
