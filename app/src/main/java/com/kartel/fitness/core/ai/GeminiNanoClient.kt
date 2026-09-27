package com.kartel.fitness.core.ai

/**
 * Обёртка над ML Kit GenAI Prompt API (поверх AICore).
 *
 * TODO (roadmap п.1, следующий шаг разработки):
 *  - добавить зависимость com.google.mlkit:genai-prompt (или актуальный артефакт
 *    ML Kit GenAI на момент реализации) в app/build.gradle.kts;
 *  - checkAvailability(): опросить FeatureStatus у GenerativeModel/AiCore и смэппить
 *    его в GeminiNanoAvailability;
 *  - если DOWNLOADABLE — инициировать скачивание модели с индикатором прогресса;
 *  - parseVoiceCommand(text): отправить промпт с инструкцией вернуть JSON
 *    { "module": ..., "fields": {...} }, распарсить ответ, при ошибке
 *    парсинга/низкой уверенности вернуть ModuleIntent(module = null) —
 *    вызывающий слой покажет экран ручного выбора модуля.
 */
class GeminiNanoClient {

    suspend fun checkAvailability(): GeminiNanoAvailability {
        // Заглушка до подключения ML Kit GenAI Prompt API.
        return GeminiNanoAvailability.UNAVAILABLE
    }

    suspend fun parseVoiceCommand(text: String): ModuleIntent {
        // Заглушка: пока модель не подключена, всегда просим ручной выбор модуля.
        return ModuleIntent(module = null, rawModelOutput = text)
    }
}
