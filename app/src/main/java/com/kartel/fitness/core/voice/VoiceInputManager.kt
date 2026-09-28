package com.kartel.fitness.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Обёртка над Android SpeechRecognizer в офлайн-режиме (roadmap п.2 "Голосовой ввод").
 * Точка входа в приложении — плавающая кнопка: держишь — говоришь, отпустил — запись
 * уходит в мини-ИИ (см. feature-уровень, где VoiceInputManager подключается к
 * GeminiNanoClient/ActionRouter).
 */
class VoiceInputManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onReady: () -> Unit = {},
        preferOffline: Boolean = true,
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Распознавание речи недоступно на этом устройстве")
            return
        }
        stopListening()

        val newRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = newRecognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        newRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onReady()

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (text.isNullOrBlank()) onError("Не удалось распознать речь") else onResult(text)
            }

            override fun onError(error: Int) {
                // Офлайн-пакет языка не скачан на устройстве — тихо пробуем ещё раз через
                // онлайн-распознавание вместо того, чтобы сразу показывать ошибку пользователю.
                val isLanguagePackIssue = error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
                    error == SpeechRecognizer.ERROR_CLIENT
                if (preferOffline && isLanguagePackIssue) {
                    startListening(onResult, onError, onReady, preferOffline = false)
                } else {
                    onError(describeError(error))
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        newRecognizer.startListening(intent)
    }

    fun stopListening() {
        recognizer?.apply {
            stopListening()
            destroy()
        }
        recognizer = null
    }

    private fun describeError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Не расслышал — попробуй ещё раз"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Слишком тихо или долгая пауза"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Проблема с сетью"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения на микрофон"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят, попробуй ещё раз"
        SpeechRecognizer.ERROR_AUDIO -> "Ошибка записи звука"
        else -> "Ошибка распознавания речи"
    }
}
