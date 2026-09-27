package com.kartel.fitness.core.ai

/**
 * Модуль приложения, в который мини-ИИ маршрутизирует голосовую фразу.
 */
enum class AppModule {
    PLANS, WORK, SCHEDULE, HEALTH, NUTRITION
}

/**
 * Структурированный результат разбора голосовой фразы моделью Gemini Nano:
 * модуль + произвольные поля (название цели, дедлайн, продукт и т.п.),
 * см. roadmap п.1 "Ядро мини-ИИ" — "JSON (модуль + поля)".
 *
 * Если модель вернула невалидный JSON или уверенность низкая — [module] равен null,
 * и вызывающий слой обязан предложить пользователю ручной выбор модуля вместо угадывания.
 */
data class ModuleIntent(
    val module: AppModule?,
    val fields: Map<String, String> = emptyMap(),
    val rawModelOutput: String? = null,
)
