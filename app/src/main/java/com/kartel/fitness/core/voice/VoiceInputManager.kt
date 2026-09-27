package com.kartel.fitness.core.voice

/**
 * Обёртка над Android SpeechRecognizer в офлайн-режиме (EXTRA_PREFER_OFFLINE).
 *
 * TODO (roadmap п.2 "Голосовой ввод"):
 *  - overlay-сервис с плавающей кнопкой поверх любого экрана приложения
 *    (WindowManager + TYPE_APPLICATION_OVERLAY, разрешение SYSTEM_ALERT_WINDOW);
 *  - удержание кнопки -> startListening(), отпускание -> stopListening();
 *  - результат распознавания передаётся в GeminiNanoClient.parseVoiceCommand().
 */
class VoiceInputManager {

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        // Заглушка до подключения android.speech.SpeechRecognizer.
        onError("Голосовой ввод ещё не реализован")
    }

    fun stopListening() {
        // no-op пока
    }
}
