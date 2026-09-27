package com.kartel.fitness.core.ai

/**
 * Статус доступности Gemini Nano (через ML Kit GenAI Prompt API поверх AICore)
 * на текущем устройстве.
 *
 * См. roadmap, п.1 "Ядро мини-ИИ": при первом запуске приложение обязано
 * проверить один из этих статусов и показать понятное сообщение вместо падения,
 * если модель недоступна.
 */
enum class GeminiNanoAvailability {
    /** Модель скачана и готова принимать запросы. */
    AVAILABLE,

    /** Устройство поддерживает AICore, но модель ещё нужно скачать. */
    DOWNLOADABLE,

    /** Устройство не входит в список поддерживаемых (нет AICore/Gemini Nano). */
    UNAVAILABLE,
}
