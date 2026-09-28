package com.kartel.fitness.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.fitness.core.data.nutrition.MealLogDao
import com.kartel.fitness.core.data.nutrition.MealLogEntity
import com.kartel.fitness.core.nutrition.FoodItem
import com.kartel.fitness.core.nutrition.FoodRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NutritionViewModel(
    private val foodRepository: FoodRepository,
    private val dao: MealLogDao,
) : ViewModel() {

    private val startOfToday: Long
        get() = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val todayMeals: StateFlow<List<MealLogEntity>> = dao.observeSince(startOfToday)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(query: String): List<FoodItem> = foodRepository.search(query)

    fun logMeal(food: FoodItem, grams: Int) {
        if (grams <= 0) return
        val factor = grams / 100.0
        viewModelScope.launch {
            dao.insert(
                MealLogEntity(
                    foodName = food.name,
                    grams = grams,
                    calories = food.calories * factor,
                    protein = food.protein * factor,
                    fat = food.fat * factor,
                    carbs = food.carbs * factor,
                )
            )
        }
    }

    fun deleteMeal(entry: MealLogEntity) {
        viewModelScope.launch { dao.delete(entry) }
    }
}
