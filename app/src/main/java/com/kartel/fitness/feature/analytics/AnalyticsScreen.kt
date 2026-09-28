package com.kartel.fitness.feature.analytics

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kartel.fitness.FitnessApp
import com.kartel.fitness.core.export.DataExporter
import com.kartel.fitness.core.health.HealthConnectBridge
import com.kartel.fitness.core.ui.GradientHeader
import com.kartel.fitness.core.ui.charts.SimpleBarChart
import kotlinx.coroutines.launch

/**
 * Модуль Analytics (roadmap п.9): простые графики по каждому модулю отдельно,
 * без кросс-модульных связей. Плюс кнопка ручного экспорта (roadmap п.8).
 */
@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as FitnessApp
    val scope = rememberCoroutineScope()

    val viewModel: AnalyticsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AnalyticsViewModel(
                    app.database.planDao(),
                    app.database.workDao(),
                    app.database.mealLogDao(),
                    HealthConnectBridge(context),
                )
            }
        }
    )

    LaunchedEffect(Unit) { viewModel.refresh() }

    val nutritionWeek by viewModel.nutritionWeek.collectAsStateWithLifecycle()
    val workStatus by viewModel.workStatus.collectAsStateWithLifecycle()
    val healthSteps by viewModel.healthSteps.collectAsStateWithLifecycle()
    val healthAvailable by viewModel.healthAvailable.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
    GradientHeader(title = "Analytics", subtitle = "Графики и экспорт данных", icon = Icons.Filled.Analytics)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Хранение и экспорт", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Все данные хранятся только на этом телефоне. Кнопка ниже собирает Plans/Work/Nutrition " +
                        "в один JSON-файл и открывает системное меню \"Поделиться\" — сохрани его куда угодно " +
                        "как резервную копию.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = {
                    scope.launch {
                        val json = viewModel.buildExportJson()
                        val uri = DataExporter.writeAndGetShareUri(context, json)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Экспорт данных"))
                    }
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Экспортировать данные")
                }
            }
        }

        ChartSection(title = "Nutrition — калории за неделю") {
            SimpleBarChart(data = nutritionWeek)
        }

        ChartSection(title = "Work — задачи по статусам") {
            SimpleBarChart(data = workStatus, barColor = MaterialTheme.colorScheme.secondary)
        }

        ChartSection(title = "Health — шаги за неделю") {
            if (healthAvailable) {
                SimpleBarChart(data = healthSteps, barColor = MaterialTheme.colorScheme.tertiary)
            } else {
                Text(
                    "Разреши доступ к Health Connect на вкладке Health, чтобы увидеть шаги здесь",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    }
}

@Composable
private fun ChartSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}
