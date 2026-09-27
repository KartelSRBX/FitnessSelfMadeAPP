package com.kartel.fitness.feature.work

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * TODO (roadmap п.4 "Work"): Room-таблица задач (название, дедлайн, статус, проект);
 * при сохранении задачи с дедлайном — создание/обновление события в Calendar Provider;
 * периодическая синхронизация с Bitrix24 REST API.
 */
@Composable
fun WorkScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Work — в разработке")
    }
}
