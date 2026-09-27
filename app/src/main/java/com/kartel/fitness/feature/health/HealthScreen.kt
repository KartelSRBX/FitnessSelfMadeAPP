package com.kartel.fitness.feature.health

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * TODO (roadmap п.6 "Health"): интеграция с Health Connect SDK — запрос разрешений
 * по типам данных (шаги/сон/пульс), периодическое чтение и агрегация для графиков;
 * только чтение, без записи.
 */
@Composable
fun HealthScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Health — в разработке")
    }
}
