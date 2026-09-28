package com.kartel.fitness.core.data.nutrition

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Запись о приёме пищи (roadmap, модуль "Nutrition"). БЖУ и калории считаются
 * от найденного в базе [foodName] на момент логирования и сохраняются как есть —
 * история не должна "переписываться" при обновлении датасета.
 */
@Entity(tableName = "meal_log")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodName: String,
    val grams: Int,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val loggedAt: Long = System.currentTimeMillis(),
    /** true, если КБЖУ — оценка Gemini Nano по свободному тексту, а не из базы USDA. */
    val isEstimate: Boolean = false,
)
