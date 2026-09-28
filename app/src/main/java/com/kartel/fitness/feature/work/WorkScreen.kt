package com.kartel.fitness.feature.work

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kartel.fitness.FitnessApp
import com.kartel.fitness.core.calendar.CalendarBridge
import com.kartel.fitness.core.data.work.WorkEntity
import com.kartel.fitness.core.data.work.WorkStatus
import com.kartel.fitness.core.notifications.ReminderScheduler
import com.kartel.fitness.core.ui.GradientFab
import com.kartel.fitness.core.ui.GradientHeader
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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
            initializer {
                WorkViewModel(
                    app.database.workDao(),
                    CalendarBridge(context),
                    ReminderScheduler(context),
                    app.database.workChecklistDao(),
                )
            }
        }
    )

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var checklistTask by remember { mutableStateOf<WorkEntity?>(null) }
    var viewMode by remember { mutableStateOf(WorkViewMode.LIST) }
    var calendarPermissionGranted by remember { mutableStateOf(viewModel.hasCalendarPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        calendarPermissionGranted = result.values.all { it }
    }

    val reminderScheduler = remember { ReminderScheduler(context) }
    var exactAlarmGranted by remember { mutableStateOf(reminderScheduler.hasExactAlarmPermission()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmGranted = reminderScheduler.hasExactAlarmPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

            if (!exactAlarmGranted) {
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
                            "Разреши точные напоминания, чтобы уведомление о дедлайне пришло вовремя",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }) { Text("Разрешить") }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(16.dp, 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = viewMode == WorkViewMode.LIST,
                    onClick = { viewMode = WorkViewMode.LIST },
                    label = { Text("Список") },
                )
                FilterChip(
                    selected = viewMode == WorkViewMode.DAYS,
                    onClick = { viewMode = WorkViewMode.DAYS },
                    label = { Text("По дням") },
                )
            }

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Задач пока нет — добавь через +",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else if (viewMode == WorkViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        WorkCard(
                            task = task,
                            viewModel = viewModel,
                            onCycleStatus = { viewModel.cycleStatus(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onOpenChecklist = { checklistTask = task },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groupTasksByDay(tasks).forEach { (dayLabel, dayTasks) ->
                        item(key = "header-$dayLabel") {
                            Text(
                                dayLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(dayTasks, key = { it.id }) { task ->
                            WorkCard(
                                task = task,
                                viewModel = viewModel,
                                onCycleStatus = { viewModel.cycleStatus(task) },
                                onDelete = { viewModel.deleteTask(task) },
                                onOpenChecklist = { checklistTask = task },
                                modifier = Modifier.animateItem().padding(bottom = 4.dp),
                            )
                        }
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

    checklistTask?.let { task ->
        ChecklistDialog(task = task, viewModel = viewModel, onDismiss = { checklistTask = null })
    }
}

private val dateFormat = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
private val plainDateFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))

private enum class WorkViewMode { LIST, DAYS }

/**
 * Группирует задачи по дню дедлайна: "Сегодня"/"Завтра"/"Вчера" отдельными подписями,
 * остальные даты — по месяцу и числу, дни идут по возрастанию; задачи без дедлайна —
 * отдельным контейнером в конце (roadmap-запрос "контейнеры распределения по дням").
 */
private fun groupTasksByDay(tasks: List<WorkEntity>): List<Pair<String, List<WorkEntity>>> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val dayLabelFormat = DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("ru"))

    val withDate = tasks
        .filter { it.deadlineMillis != null }
        .groupBy { Instant.ofEpochMilli(it.deadlineMillis!!).atZone(zone).toLocalDate() }
        .toSortedMap()
        .map { (date, dayTasks) ->
            val label = when (date) {
                today -> "Сегодня"
                today.plusDays(1) -> "Завтра"
                today.minusDays(1) -> "Вчера"
                else -> date.format(dayLabelFormat).replaceFirstChar { it.uppercase() }
            }
            label to dayTasks
        }

    val withoutDate = tasks.filter { it.deadlineMillis == null }
    return if (withoutDate.isEmpty()) withDate else withDate + ("Без даты" to withoutDate)
}

@Composable
private fun WorkCard(
    task: WorkEntity,
    viewModel: WorkViewModel,
    onCycleStatus: () -> Unit,
    onDelete: () -> Unit,
    onOpenChecklist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (statusContainer, statusContent, statusIcon) = when (task.status) {
        WorkStatus.TODO -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant, null)
        WorkStatus.IN_PROGRESS -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Filled.HourglassTop)
        WorkStatus.DONE -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Filled.CheckCircle)
    }
    val checklist by viewModel.checklistFor(task.id).collectAsStateWithLifecycle(initialValue = emptyList())

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onOpenChecklist,
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
                AssistChip(
                    onClick = onCycleStatus,
                    label = { Text(task.status.label) },
                    leadingIcon = statusIcon?.let { icon -> { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusContainer,
                        labelColor = statusContent,
                        leadingIconContentColor = statusContent,
                    ),
                )
                if (task.project.isNotBlank()) {
                    SuggestionChip(onClick = {}, label = { Text(task.project) })
                }
                if (task.deadlineMillis != null) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                    Text(dateFormat.format(task.deadlineMillis), style = MaterialTheme.typography.labelMedium)
                }
            }
            if (checklist.isNotEmpty()) {
                val done = checklist.count { it.isDone }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(
                        Icons.Filled.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        " $done/${checklist.size} в чеклисте",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp),
                    )
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
    var deadlineDateMillis by remember { mutableStateOf<Long?>(null) }
    var deadlineHour by remember { mutableIntStateOf(18) }
    var deadlineMinute by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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
                            if (it && deadlineDateMillis == null) showDatePicker = true
                        },
                    )
                    Text("Установить дедлайн")
                }
                if (deadlineEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(deadlineDateMillis?.let { plainDateFormat.format(it) } ?: "Выбрать дату")
                        }
                        TextButton(onClick = { showTimePicker = true }) {
                            Text("%02d:%02d".format(deadlineHour, deadlineMinute))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val deadline = if (deadlineEnabled) {
                        deadlineDateMillis?.let { combineDateAndTime(it, deadlineHour, deadlineMinute) }
                    } else {
                        null
                    }
                    onConfirm(title, description, project, deadline)
                },
                enabled = title.isNotBlank() && (!deadlineEnabled || deadlineDateMillis != null),
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = deadlineDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineDateMillis = state.selectedDateMillis
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

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = deadlineHour, initialMinute = deadlineMinute, is24Hour = true)
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimePicker(state = timeState)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
                        TextButton(onClick = {
                            deadlineHour = timeState.hour
                            deadlineMinute = timeState.minute
                            showTimePicker = false
                        }) { Text("Ок") }
                    }
                }
            }
        }
    }
}

/** DatePicker отдаёт полночь UTC выбранного дня — переносим год/месяц/день на локальные час/минуту. */
private fun combineDateAndTime(dateMillisUtcMidnight: Long, hour: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateMillisUtcMidnight }
    val local = Calendar.getInstance().apply {
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return local.timeInMillis
}

/** Чеклист (подзадачи) внутри задачи Work. */
@Composable
private fun ChecklistDialog(task: WorkEntity, viewModel: WorkViewModel, onDismiss: () -> Unit) {
    val items by viewModel.checklistFor(task.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var newItemText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (items.isEmpty()) {
                    Text(
                        "Пунктов пока нет — добавь первый ниже",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { viewModel.toggleChecklistItem(item) },
                            )
                            Text(
                                item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { viewModel.deleteChecklistItem(item) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Удалить пункт",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = { Text("Новый пункт") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            viewModel.addChecklistItem(task.id, newItemText)
                            newItemText = ""
                        },
                        enabled = newItemText.isNotBlank(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Добавить пункт")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово") }
        },
    )
}
