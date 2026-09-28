package com.kartel.fitness.core.ai

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.generationConfig
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

    private fun buildPrompt(text: String): String = """
        Ты - классификатор команд для личного Android-ассистента с пятью модулями:
        PLANS (долгосрочные личные цели без дат), WORK (рабочие задачи с дедлайнами),
        SCHEDULE (встречи/события с конкретным временем), HEALTH (данные о здоровье,
        сюда почти никогда не нужно писать), NUTRITION (приёмы пищи).

        Верни ТОЛЬКО валидный JSON без пояснений и без markdown-разметки, строго в формате:
        {"module": "PLANS|WORK|SCHEDULE|HEALTH|NUTRITION|null", "confidence": 0.0-1.0, "fields": {}}

        Если фраза не подходит ни под один модуль или ты не уверен - верни module: null и confidence: 0.

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
