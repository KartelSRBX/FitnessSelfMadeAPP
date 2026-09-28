package com.kartel.fitness.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kartel.fitness.FitnessApp
import com.kartel.fitness.core.ai.ActionRouter
import com.kartel.fitness.core.ai.AppModule
import com.kartel.fitness.core.ai.GeminiNanoAvailability
import com.kartel.fitness.core.ai.GeminiNanoClient
import com.kartel.fitness.core.calendar.CalendarBridge
import com.kartel.fitness.core.nutrition.FoodRepository
import com.kartel.fitness.feature.voice.VoiceFabButton

/**
 * Тестовый экран ядра мини-ИИ (roadmap п.1). Ввести фразу можно текстом здесь же,
 * либо голосом через плавающую кнопку внизу справа (roadmap п.2) — оба пути ведут
 * в один и тот же пайплайн GeminiNanoClient -> ActionRouter.
 */
@Composable
fun AiTestScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as FitnessApp
    val viewModel: AiViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val geminiNanoClient = GeminiNanoClient()
                AiViewModel(
                    geminiNanoClient,
                    ActionRouter(
                        app.database.planDao(),
                        app.database.workDao(),
                        app.database.mealLogDao(),
                        FoodRepository(context),
                        CalendarBridge(context),
                        geminiNanoClient,
                    ),
                )
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.checkAvailability() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvailabilityCard(state = state, onCheck = { viewModel.checkAvailability() }, onDownload = { viewModel.downloadModel() })

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Проверить разбор фразы", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Напиши фразу как для одного из модулей, например " +
                        "«хочу выучить испанский» или «съел 200 грамм риса»",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Фраза") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.parse(inputText) },
                    enabled = state.availability == GeminiNanoAvailability.AVAILABLE && !state.parsing && inputText.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(if (state.parsing) "Разбираю..." else "Разобрать")
                }
            }
        }

        state.lastResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Результат", style = MaterialTheme.typography.titleSmall)
                    if (result.module != null) {
                        Text(
                            "Модуль: ${result.module.label()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    state.executionMessage?.let { message ->
                        Icon(
                            if (state.executionNeedsManual) Icons.Filled.Error else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (state.executionNeedsManual) MaterialTheme.colorScheme.tertiary else Color(0xFF6FCF97),
                        )
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.executionNeedsManual) MaterialTheme.colorScheme.tertiary else Color(0xFF6FCF97),
                        )
                    }
                    if (result.fields.isNotEmpty()) {
                        result.fields.forEach { (key, value) ->
                            Text("• $key: $value", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        }

        VoiceFabButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}

private fun AppModule.label(): String = when (this) {
    AppModule.PLANS -> "Plans"
    AppModule.WORK -> "Work"
    AppModule.SCHEDULE -> "Schedule"
    AppModule.HEALTH -> "Health"
    AppModule.NUTRITION -> "Nutrition"
}

@Composable
private fun AvailabilityCard(state: AiUiState, onCheck: () -> Unit, onDownload: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val (icon, tint, label) = when (state.availability) {
                    GeminiNanoAvailability.AVAILABLE ->
                        Triple(Icons.Filled.CheckCircle, Color(0xFF6FCF97), "Gemini Nano готов")
                    GeminiNanoAvailability.DOWNLOADABLE ->
                        Triple(Icons.Filled.Error, MaterialTheme.colorScheme.tertiary, "Поддерживается, но модель не скачана")
                    GeminiNanoAvailability.UNAVAILABLE ->
                        Triple(Icons.Filled.Error, MaterialTheme.colorScheme.error, "Устройство не поддерживает Gemini Nano")
                }
                Icon(icon, contentDescription = null, tint = tint)
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (state.checking) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }

            if (state.downloading) {
                val progress = if (state.totalBytes > 0) state.downloadedBytes.toFloat() / state.totalBytes else 0f
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(
                    "Скачивается: ${state.downloadedBytes / (1024 * 1024)} / ${state.totalBytes / (1024 * 1024)} МБ",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            state.downloadError?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCheck) { Text("Проверить") }
                if (state.availability == GeminiNanoAvailability.DOWNLOADABLE && !state.downloading) {
                    Button(onClick = onDownload) { Text("Скачать модель") }
                }
            }
        }
    }
}
