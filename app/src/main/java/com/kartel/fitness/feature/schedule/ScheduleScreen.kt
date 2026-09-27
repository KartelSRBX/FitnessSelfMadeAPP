package com.kartel.fitness.feature.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * TODO (roadmap п.5 "Schedule"): создание отдельного календаря-аккаунта
 * в Calendar Provider при первом запуске; CRUD событий через CalendarContract;
 * чтение объединённого списка событий (свои + дедлайны Work).
 */
@Composable
fun ScheduleScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Schedule — в разработке")
    }
}
