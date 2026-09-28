package com.kartel.fitness.core.nutrition

/** БЖУ и калории на 100г продукта. */
data class FoodItem(
    val name: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)
