package com.kartel.fitness.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kartel.fitness.core.data.nutrition.MealLogEntity
import com.kartel.fitness.core.nutrition.FoodItem
import com.kartel.fitness.core.nutrition.FoodRepository
import kotlin.math.roundToInt

/**
 * Модуль Nutrition (roadmap п.7): поиск по локальной базе продуктов и
 * логирование приёма пищи с граммовкой. Голосовой ввод и fallback на
 * оценку Gemini Nano — TODO, когда будет готово ядро мини-ИИ.
 */
@Composable
fun NutritionScreen() {
    val context = LocalContext.current
    val viewModel: NutritionViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = context.applicationContext as com.kartel.fitness.FitnessApp
                NutritionViewModel(FoodRepository(context), app.database.mealLogDao())
            }
        }
    )

    var query by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    val results = if (query.isBlank()) emptyList() else viewModel.search(query)
    val todayMeals by viewModel.todayMeals.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Поиск продукта") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (query.isNotBlank()) {
                if (results.isEmpty()) {
                    Text(
                        "Совпадений нет — попробуй другое название",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(results, key = { it.name }) { food ->
                            FoodResultRow(food = food, onClick = { selectedFood = food })
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("Сегодня", style = MaterialTheme.typography.titleMedium)
                    if (todayMeals.isNotEmpty()) {
                        val totalCalories = todayMeals.sumOf { it.calories }
                        val totalProtein = todayMeals.sumOf { it.protein }
                        val totalFat = todayMeals.sumOf { it.fat }
                        val totalCarbs = todayMeals.sumOf { it.carbs }
                        Text(
                            "%.0f ккал · Б %.0f · Ж %.0f · У %.0f".format(
                                totalCalories, totalProtein, totalFat, totalCarbs
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                    }
                    if (todayMeals.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Сегодня ещё ничего не залогировано — найди продукт через поиск",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(todayMeals, key = { it.id }) { meal ->
                                MealRow(meal = meal, onDelete = { viewModel.deleteMeal(meal) })
                            }
                        }
                    }
                }
            }
        }
    }

    selectedFood?.let { food ->
        LogMealDialog(
            food = food,
            onDismiss = { selectedFood = null },
            onConfirm = { grams ->
                viewModel.logMeal(food, grams)
                selectedFood = null
                query = ""
            },
        )
    }
}

@Composable
private fun FoodResultRow(food: FoodItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(food.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "на 100г: %.0f ккал · Б %.1f · Ж %.1f · У %.1f".format(
                    food.calories, food.protein, food.fat, food.carbs
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun MealRow(meal: MealLogEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                val portionLabel = if (meal.isEstimate) "оценка ИИ" else "${meal.grams}г"
                Text("${meal.foodName} · $portionLabel", style = MaterialTheme.typography.titleSmall)
                Text(
                    "%.0f ккал · Б %.1f · Ж %.1f · У %.1f".format(
                        meal.calories, meal.protein, meal.fat, meal.carbs
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LogMealDialog(food: FoodItem, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var gramsText by remember { mutableStateOf("100") }
    val grams = gramsText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column {
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Граммы") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (grams != null && grams > 0) {
                    val factor = grams / 100.0
                    Text(
                        "= %.0f ккал · Б %.1f · Ж %.1f · У %.1f".format(
                            food.calories * factor,
                            food.protein * factor,
                            (food.fat * factor * 10).roundToInt() / 10.0,
                            food.carbs * factor,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { grams?.let(onConfirm) }, enabled = grams != null && grams > 0) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
