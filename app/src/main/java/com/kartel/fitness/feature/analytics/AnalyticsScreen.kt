package com.kartel.fitness.feature.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * TODO (roadmap п.9 "Аналитика"): простые графики по каждому модулю
 * (Compose Canvas или MPAndroidChart) — калории за неделю, выполненные задачи,
 * шаги/сон за период. Без кросс-модульных связей в этой версии.
 */
@Composable
fun AnalyticsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Analytics — в разработке")
    }
}
