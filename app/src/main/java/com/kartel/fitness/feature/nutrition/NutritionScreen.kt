package com.kartel.fitness.feature.nutrition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * TODO (roadmap п.7 "Nutrition"): локальная база на основе усечённого
 * USDA FoodData Central (SQLite asset, предзагруженный); сопоставление
 * название -> БЖУ с нечётким поиском; fallback на оценку Gemini Nano.
 */
@Composable
fun NutritionScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Nutrition — в разработке")
    }
}
