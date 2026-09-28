package com.kartel.fitness.feature.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.kartel.fitness.core.data.plans.PlanEntity
import com.kartel.fitness.core.ui.GradientFab
import com.kartel.fitness.core.ui.GradientHeader

/**
 * Модуль Plans: долгосрочные личные цели без дат (roadmap п.3).
 * CRUD и отметка прогресса — пока без голосового ввода.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen() {
    val app = LocalContext.current.applicationContext as FitnessApp
    val viewModel: PlansViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PlansViewModel(app.database.planDao()) }
        }
    )

    val plans by viewModel.plans.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var planForProgress by remember { mutableStateOf<PlanEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
    GradientHeader(title = "Plans", subtitle = "Долгосрочные цели без дат", icon = Icons.Filled.Flag)
    Scaffold(
        modifier = Modifier.weight(1f),
        floatingActionButton = {
            GradientFab(onClick = { showAddDialog = true }, icon = Icons.Filled.Add, contentDescription = "Добавить цель")
        }
    ) { innerPadding ->
        if (plans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Целей пока нет — добавь первую через +",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, innerPadding.calculateTopPadding(), 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(plans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        onClick = { planForProgress = plan },
                        onDelete = { viewModel.deletePlan(plan) },
                    )
                }
            }
        }
    }
    }

    if (showAddDialog) {
        AddPlanDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description ->
                viewModel.addPlan(title, description)
                showAddDialog = false
            },
        )
    }

    planForProgress?.let { plan ->
        ProgressDialog(
            plan = plan,
            onDismiss = { planForProgress = null },
            onConfirm = { percent, note ->
                viewModel.updateProgress(plan, percent, note)
                planForProgress = null
            },
        )
    }
}

@Composable
private fun PlanCard(plan: PlanEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (plan.description.isNotBlank()) {
                Text(
                    plan.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            LinearProgressIndicator(
                progress = { plan.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
            )
            Text(
                "${plan.progressPercent}%" + if (plan.lastProgressNote.isNotBlank()) " · ${plan.lastProgressNote}" else "",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AddPlanDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая цель") },
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
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, description) }, enabled = title.isNotBlank()) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun ProgressDialog(plan: PlanEntity, onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var progress by remember { mutableFloatStateOf(plan.progressPercent.toFloat()) }
    var note by remember { mutableStateOf(plan.lastProgressNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(plan.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Прогресс: ${progress.toInt()}%")
                Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка о прогрессе (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(progress.toInt(), note) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
