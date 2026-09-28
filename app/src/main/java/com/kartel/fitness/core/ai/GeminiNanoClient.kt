package com.kartel.fitness.core.ai

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.generationConfig
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

/**
 * Обёртка над ML Kit GenAI Prompt API (поверх AICore) — roadmap п.1 "Ядро мини-ИИ".
 * Проверяет доступность Gemini Nano на устройстве, скачивает модель при необходимости,
 * и разбирает текстовую фразу в структурированный JSON (модуль + поля).
 *
 * При невалидном JSON или низкой уверенности [ModuleIntent.module] будет null —
 * вызывающий слой обязан предложить пользователю ручной выбор модуля, а не гадать.
 */
class GeminiNanoClient {

    companion object {
        private const val MIN_CONFIDENCE = 0.6
    }

    private var model: GenerativeModel? = null

    private fun client(): GenerativeModel =
        model ?: Generation.getClient(generationConfig {}).also { model = it }

    suspend fun checkAvailability(): GeminiNanoAvailability = try {
        when (client().checkStatus()) {
            FeatureStatus.AVAILABLE -> GeminiNanoAvailability.AVAILABLE
            FeatureStatus.UNAVAILABLE -> GeminiNanoAvailability.UNAVAILABLE
            else -> GeminiNanoAvailability.DOWNLOADABLE
        }
    } catch (e: Exception) {
        GeminiNanoAvailability.UNAVAILABLE
    }

    /** Прогресс скачивания модели — эмитит статусы ML Kit напрямую, UI сам решает как их показать. */
    fun downloadModel(): Flow<DownloadStatus> = client().download()

    suspend fun parseVoiceCommand(text: String): ModuleIntent {
        val raw = try {
            client().generateContent(buildPrompt(text)).candidates.firstOrNull()?.text
        } catch (e: Exception) {
            null
        } ?: return ModuleIntent(module = null, rawModelOutput = null)

        return parseResponse(raw)
    }

    /**
     * Fallback для Nutrition, когда в локальной базе нет совпадения (roadmap п.7):
     * просим саму модель оценить суммарную пищевую ценность произвольного описания
     * приёма пищи целиком, включая составные блюда из нескольких компонентов.
     */
    suspend fun estimateNutrition(description: String): NutritionEstimate? {
        val raw = try {
            client().generateContent(buildNutritionEstimatePrompt(description)).candidates.firstOrNull()?.text
        } catch (e: Exception) {
            null
        } ?: return null

        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return try {
            val obj = JSONObject(cleaned)
            NutritionEstimate(
                calories = obj.optDouble("calories", 0.0),
                protein = obj.optDouble("protein", 0.0),
                fat = obj.optDouble("fat", 0.0),
                carbs = obj.optDouble("carbs", 0.0),
                summary = obj.optString("summary", description).ifBlank { description },
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun buildNutritionEstimatePrompt(description: String): String = """
        Ты - помощник по подсчёту калорий. Оцени суммарную пищевую ценность следующего приёма
        пищи целиком (все перечисленные компоненты вместе), используя типичные значения для
        похожих продуктов и блюд. Верни ТОЛЬКО валидный JSON без пояснений и без markdown, строго
        в формате:
        {"calories": число ккал, "protein": число грамм белка, "fat": число грамм жира, "carbs": число грамм углеводов, "summary": "короткое название блюда на русском, 2-4 слова"}

        Описание приёма пищи: "$description"
    """.trimIndent()

    private fun buildPrompt(text: String): String = """
        Ты - классификатор команд для личного Android-ассистента. Разбери фразу пользователя
        и верни ТОЛЬКО валидный JSON без пояснений и без markdown-разметки, строго в одном
        из следующих форматов - в зависимости от того, к какому модулю относится фраза:

        Долгосрочная личная цель без даты:
        {"module": "PLANS", "confidence": 0.0-1.0, "fields": {"title": "...", "description": "..."}}

        Рабочая задача (может быть с дедлайном):
        {"module": "WORK", "confidence": 0.0-1.0, "fields": {"title": "...", "project": "...", "deadlineDate": "YYYY-MM-DD или пусто"}}

        Встреча/событие с конкретной датой и временем:
        {"module": "SCHEDULE", "confidence": 0.0-1.0, "fields": {"title": "...", "date": "YYYY-MM-DD", "time": "HH:MM"}}

        Приём пищи:
        {"module": "NUTRITION", "confidence": 0.0-1.0, "fields": {"food": "название продукта на русском в единственном числе, как в словаре БЖУ (например курица, рис, яблоко)", "grams": "число граммов, если не указано - 100"}}

        HEALTH используется только для чтения данных - для таких фраз, а также если фраза не
        подходит ни под один модуль или ты не уверен, верни:
        {"module": null, "confidence": 0, "fields": {}}

        Сегодняшняя дата: ${LocalDate.now()}. Используй её, чтобы посчитать даты вида "завтра"/"в пятницу".

        Фраза пользователя: "$text"
    """.trimIndent()

    private fun parseResponse(raw: String): ModuleIntent {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return try {
            val obj = JSONObject(cleaned)
            val confidence = obj.optDouble("confidence", 0.0)
            val moduleStr = obj.optString("module", "null")
            val module = if (confidence >= MIN_CONFIDENCE) {
                runCatching { AppModule.valueOf(moduleStr.uppercase()) }.getOrNull()
            } else {
                null
            }
            val fields = mutableMapOf<String, String>()
            obj.optJSONObject("fields")?.let { fieldsObj ->
                fieldsObj.keys().forEach { key -> fields[key] = fieldsObj.optString(key) }
            }
            ModuleIntent(module = module, fields = fields, rawModelOutput = raw)
        } catch (e: Exception) {
            ModuleIntent(module = null, rawModelOutput = raw)
        }
    }

    fun close() {
        model?.close()
        model = null
    }
}
