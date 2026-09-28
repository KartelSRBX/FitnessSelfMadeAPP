package com.kartel.fitness.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kartel.fitness.FitnessApp
import com.kartel.fitness.core.data.timer.AlarmEntity
import com.kartel.fitness.core.data.timer.TimerEntity
import com.kartel.fitness.core.notifications.ReminderScheduler
import com.kartel.fitness.core.ui.GradientFab
import com.kartel.fitness.core.ui.GradientHeader
import kotlinx.coroutines.delay

/** Таймер и будильник общего назначения — не привязаны к дедлайнам Work/Schedule. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as FitnessApp
    val viewModel: TimerViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TimerViewModel(app.database.timerDao(), app.database.alarmDao(), ReminderScheduler(context))
            }
        }
    )

    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    var showTimerDialog by remember { mutableStateOf(false) }
    var showAlarmDialog by remember { mutableStateOf(false) }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMillis = System.currentTimeMillis()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(title = "Timer", subtitle = "Таймеры и будильники", icon = Icons.Filled.Timer)
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Таймеры", style = MaterialTheme.typography.titleMedium)
                if (timers.isEmpty()) {
                    Text(
                        "Активных таймеров нет",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    timers.forEach { timer ->
                        TimerRow(timer = timer, nowMillis = nowMillis, onCancel = { viewModel.cancelTimer(timer) })
                    }
                }

                Text("Будильники", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                if (alarms.isEmpty()) {
                    Text(
                        "Будильников нет",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    alarms.forEach { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onDelete = { viewModel.deleteAlarm(alarm) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GradientFab(onClick = { showAlarmDialog = true }, icon = Icons.Filled.AlarmAdd, contentDescription = "Добавить будильник")
                GradientFab(onClick = { showTimerDialog = true }, icon = Icons.Filled.Timer, contentDescription = "Добавить таймер")
            }
        }
    }

    if (showTimerDialog) {
        AddTimerDialog(
            onDismiss = { showTimerDialog = false },
            onConfirm = { label, durationMillis ->
                viewModel.startTimer(label, durationMillis)
                showTimerDialog = false
            },
        )
    }

    if (showAlarmDialog) {
        AddAlarmDialog(
            onDismiss = { showAlarmDialog = false },
            onConfirm = { label, hour, minute ->
                viewModel.addAlarm(label, hour, minute)
                showAlarmDialog = false
            },
        )
    }
}

@Composable
private fun TimerRow(timer: TimerEntity, nowMillis: Long, onCancel: () -> Unit) {
    val remainingMillis = (timer.endMillis - nowMillis).coerceAtLeast(0)
    val totalSeconds = remainingMillis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val timeText = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(timer.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    timeText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Delete, contentDescription = "Отменить таймер", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: AlarmEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineSmall)
                Text(alarm.label, style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить будильник", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private val timerPresetsMinutes = listOf(1, 5, 10, 15, 30)

@Composable
private fun AddTimerDialog(onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    var label by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableIntStateOf(5) }
    var customMinutesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый таймер") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Название (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Длительность, мин", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    timerPresetsMinutes.forEach { minutes ->
                        FilterChip(
                            selected = selectedMinutes == minutes && customMinutesText.isBlank(),
                            onClick = { selectedMinutes = minutes; customMinutesText = "" },
                            label = { Text("$minutes") },
                        )
                    }
                }
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = { customMinutesText = it.filter { c -> c.isDigit() } },
                    label = { Text("Или своё число минут") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            val minutes = customMinutesText.toIntOrNull() ?: selectedMinutes
            TextButton(
                onClick = { onConfirm(label, minutes * 60_000L) },
                enabled = minutes > 0,
            ) { Text("Запустить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlarmDialog(onDismiss: () -> Unit, onConfirm: (String, Int, Int) -> Unit) {
    var label by remember { mutableStateOf("") }
    val timeState = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = true)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Новый будильник", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                TimePicker(state = timeState)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Название (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    TextButton(onClick = { onConfirm(label, timeState.hour, timeState.minute) }) { Text("Добавить") }
                }
            }
        }
    }
}
