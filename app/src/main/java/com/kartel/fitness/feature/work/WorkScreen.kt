package com.kartel.fitness.feature.work

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kartel.fitness.FitnessApp
import com.kartel.fitness.core.calendar.CalendarBridge
import com.kartel.fitness.core.data.work.WorkEntity
import com.kartel.fitness.core.ui.GradientFab
import com.kartel.fitness.core.ui.GradientHeader
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Модуль Work: задачи с дедлайнами и статусами (roadmap п.4).
 * Задача с дедлайном создаёт событие в календаре приложения (см. CalendarBridge).
 * Синхронизация с Bitrix24 — TODO, отдельным шагом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as FitnessApp
    val viewModel: WorkViewModel = viewModel(
        factory = viewModelFactory {
            initializer { WorkViewModel(app.database.workDao(), CalendarBridge(context)) }
        }
    )

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var calendarPermissionGranted by remember { mutableStateOf(viewModel.hasCalendarPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        calendarPermissionGranted = result.values.all { it }
    }

    Column(modifier = Modifier.fillMaxSize()) {
    GradientHeader(title = "Work", subtitle = "Задачи, дедлайны, статусы", icon = Icons.Filled.Checklist)
    Scaffold(
        modifier = Modifier.weight(1f),
        floatingActionButton = {
            GradientFab(onClick = { showAddDialog = true }, icon = Icons.Filled.Add, contentDescription = "Добавить задачу")
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (!calendarPermissionGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Разреши доступ к календарю, чтобы дедлайны создавали события",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                            )
                        }) { Text("Разрешить") }
                    }
                }
            }

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Задач пока нет — добавь через +",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        WorkCard(
                            task = task,
                            onCycleStatus = { viewModel.cycleStatus(task) },
                            onDelete = { viewModel.deleteTask(task) },
                        )
                    }
                }
            }
        }
    }
    }

    if (showAddDialog) {
        AddWorkDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, project, deadline ->
                viewModel.addTask(title, description, project, deadline)
                showAddDialog = false
            },
        )
    }
}

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))

@Composable
private fun WorkCard(task: WorkEntity, onCycleStatus: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (task.description.isNotBlank()) {
                Text(task.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(onClick = onCycleStatus, label = { Text(task.status.label) })
                if (task.project.isNotBlank()) {
                    SuggestionChip(onClick = {}, label = { Text(task.project) })
                }
                if (task.deadlineMillis != null) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                    Text(dateFormat.format(task.deadlineMillis), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWorkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var project by remember { mutableStateOf("") }
    var deadlineEnabled by remember { mutableStateOf(false) }
    var deadlineMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = project,
                    onValueChange = { project = it },
                    label = { Text("Проект (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deadlineEnabled,
                        onCheckedChange = {
                            deadlineEnabled = it
                            if (it && deadlineMillis == null) showDatePicker = true
                        },
                    )
                    Text("Установить дедлайн")
                }
                if (deadlineEnabled) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(
                            deadlineMillis?.let { "Дедлайн: ${dateFormat.format(it)}" } ?: "Выбрать дату"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description, project, if (deadlineEnabled) deadlineMillis else null) },
                enabled = title.isNotBlank() && (!deadlineEnabled || deadlineMillis != null),
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = deadlineMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineMillis = state.selectedDateMillis
                    showDatePicker = false
                }) { Text("Ок") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
