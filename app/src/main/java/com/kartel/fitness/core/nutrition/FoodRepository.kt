package com.kartel.fitness.core.nutrition

import android.content.Context
import org.json.JSONArray

/**
 * Усечённый локальный датасет на основе USDA FoodData Central (roadmap п.7),
 * вшитый в assets — в основном сырые продукты. Поиск по названию, без сети.
 *
 * TODO (roadmap): если совпадения нет — fallback на оценку Gemini Nano по
 * свободному описанию, когда будет готово ядро мини-ИИ.
 */
class FoodRepository(context: Context) {

    val allFoods: List<FoodItem> by lazy { loadFromAssets(context) }

    fun search(query: String): List<FoodItem> {
        if (query.isBlank()) return emptyList()
        return allFoods
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.name }
            .take(30)
    }

    private fun loadFromAssets(context: Context): List<FoodItem> {
        val json = context.assets.open("food_database.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    FoodItem(
                        name = obj.getString("name"),
                        calories = obj.getDouble("calories"),
                        protein = obj.getDouble("protein"),
                        fat = obj.getDouble("fat"),
                        carbs = obj.getDouble("carbs"),
                    )
                )
            }
        }
    }
}
