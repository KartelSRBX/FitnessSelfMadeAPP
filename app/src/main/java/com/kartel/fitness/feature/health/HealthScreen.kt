package com.kartel.fitness.feature.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kartel.fitness.core.health.HealthConnectBridge
import com.kartel.fitness.core.ui.GradientHeader

/**
 * Модуль Health (roadmap п.6): только чтение Health Connect — шаги/сон/пульс.
 * Требует, чтобы в Samsung Health была включена синхронизация в Health Connect
 * (делается пользователем один раз в настройках Samsung Health).
 */
@Composable
fun HealthScreen() {
    val context = LocalContext.current
    val viewModel: HealthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HealthViewModel(HealthConnectBridge(context)) }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.checkStatusAndRefresh()
    }

    LaunchedEffect(Unit) { viewModel.checkStatusAndRefresh() }

    Column(modifier = Modifier.fillMaxSize()) {
    GradientHeader(title = "Health", subtitle = "Шаги, сон, пульс из Health Connect", icon = Icons.Filled.Favorite)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusCard(
            state = state,
            onCheckClick = { viewModel.checkStatusAndRefresh() },
            onRequestPermissions = { permissionLauncher.launch(viewModel.permissions) },
        )

        if (state.permissionsGranted) {
            DataCard(
                icon = Icons.Filled.DirectionsWalk,
                title = "Шаги сегодня",
                value = state.steps?.toString() ?: "—",
            )
            DataCard(
                icon = Icons.Filled.Bedtime,
                title = "Сон (последняя запись)",
                value = state.sleep?.let { "${it.toHours()}ч ${it.toMinutesPart()}м" } ?: "нет данных",
            )
            DataCard(
                icon = Icons.Filled.Favorite,
                title = "Пульс (последнее измерение)",
                value = state.heartRate?.let { "$it уд/мин" } ?: "нет данных",
            )
        }
    }
    }
}

@Composable
private fun StatusCard(
    state: HealthUiState,
    onCheckClick: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val (icon, tint, label) = when {
                    state.availability == HealthConnectBridge.Availability.UNAVAILABLE ->
                        Triple(Icons.Filled.Error, MaterialTheme.colorScheme.error, "Health Connect недоступен на этом устройстве")
                    state.availability == HealthConnectBridge.Availability.UPDATE_REQUIRED ->
                        Triple(Icons.Filled.Error, MaterialTheme.colorScheme.error, "Нужно обновить Health Connect")
                    state.permissionsGranted ->
                        Triple(Icons.Filled.CheckCircle, Color(0xFF6FCF97), "Подключено, разрешения выданы")
                    else ->
                        Triple(Icons.Filled.Error, MaterialTheme.colorScheme.tertiary, "Health Connect доступен, но разрешения не выданы")
                }
                Icon(icon, contentDescription = null, tint = tint)
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (state.loading) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }

            Text(
                "Убедись, что в настройках Samsung Health включена синхронизация в Health Connect — " +
                    "иначе данных не будет, даже если разрешения выданы.",
                style = MaterialTheme.typography.bodySmall,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCheckClick) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Проверить подключение")
                }
                if (state.availability == HealthConnectBridge.Availability.AVAILABLE && !state.permissionsGranted) {
                    Button(onClick = onRequestPermissions) { Text("Выдать разрешения") }
                }
            }
        }
    }
}

@Composable
private fun DataCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(value, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
