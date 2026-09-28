package com.kartel.fitness.core.ai

/** Оценка ИИ по свободному описанию приёма пищи целиком (не на 100г, а сразу итог). */
data class NutritionEstimate(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val summary: String,
)
