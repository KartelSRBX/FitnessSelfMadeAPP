package com.kartel.fitness.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.fitness.core.ai.ActionRouter
import com.kartel.fitness.core.ai.GeminiNanoClient
import com.kartel.fitness.core.voice.VoiceInputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoiceUiState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val feedback: String? = null,
    val feedbackIsError: Boolean = false,
)

/**
 * Связывает плавающую кнопку голосового ввода (roadmap п.2) с уже готовым
 * ядром мини-ИИ: голос -> текст (VoiceInputManager) -> разбор в модуль
 * (GeminiNanoClient) -> запись в модуль (ActionRouter) — тот же пайплайн,
 * что и в тестовом текстовом вводе на вкладке AI.
 */
class VoiceCommandViewModel(
    private val voiceInputManager: VoiceInputManager,
    private val geminiNanoClient: GeminiNanoClient,
    private val actionRouter: ActionRouter,
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    fun startListening() {
        _state.value = VoiceUiState(isListening = true)
        voiceInputManager.startListening(
            onResult = { text -> handleRecognized(text) },
            onError = { message ->
                _state.value = VoiceUiState(feedback = message, feedbackIsError = true)
            },
        )
    }

    fun stopListening() {
        voiceInputManager.stopListening()
        if (_state.value.isListening) {
            _state.value = _state.value.copy(isListening = false)
        }
    }

    private fun handleRecognized(text: String) {
        _state.value = VoiceUiState(isProcessing = true, feedback = "Распознано: «$text»")
        viewModelScope.launch {
            val intent = geminiNanoClient.parseVoiceCommand(text)
            val result = actionRouter.execute(intent, text)
            _state.value = VoiceUiState(
                feedback = when (result) {
                    is ActionRouter.Result.Success -> result.message
                    is ActionRouter.Result.NeedsManualInput -> result.message
                },
                feedbackIsError = result is ActionRouter.Result.NeedsManualInput,
            )
        }
    }

    fun dismissFeedback() {
        _state.value = _state.value.copy(feedback = null)
    }

    override fun onCleared() {
        voiceInputManager.stopListening()
        geminiNanoClient.close()
        super.onCleared()
    }
}
