package com.kartel.fitness.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.fitness.core.data.nutrition.MealLogDao
import com.kartel.fitness.core.data.plans.PlanDao
import com.kartel.fitness.core.data.work.WorkDao
import com.kartel.fitness.core.data.work.WorkStatus
import com.kartel.fitness.core.health.HealthConnectBridge
import com.kartel.fitness.core.ui.charts.ChartPoint
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AnalyticsViewModel(
    private val planDao: PlanDao,
    private val workDao: WorkDao,
    private val mealLogDao: MealLogDao,
    private val healthBridge: HealthConnectBridge,
) : ViewModel() {

    private val _nutritionWeek = MutableStateFlow<List<ChartPoint>>(emptyList())
    val nutritionWeek: StateFlow<List<ChartPoint>> = _nutritionWeek.asStateFlow()

    private val _workStatus = MutableStateFlow<List<ChartPoint>>(emptyList())
    val workStatus: StateFlow<List<ChartPoint>> = _workStatus.asStateFlow()

    private val _healthSteps = MutableStateFlow<List<ChartPoint>>(emptyList())
    val healthSteps: StateFlow<List<ChartPoint>> = _healthSteps.asStateFlow()

    private val _healthAvailable = MutableStateFlow(false)
    val healthAvailable: StateFlow<Boolean> = _healthAvailable.asStateFlow()

    private val dayLabel = { date: LocalDate ->
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru")).replaceFirstChar { it.uppercase() }
    }

    fun refresh() {
        viewModelScope.launch { loadNutritionWeek() }
        viewModelScope.launch { loadWorkStatus() }
        viewModelScope.launch { loadHealthSteps() }
    }

    private suspend fun loadNutritionWeek() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startDate = today.minusDays(6)
        val sinceMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val meals = mealLogDao.observeSince(sinceMillis).first()
        val points = (0..6).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val total = meals.filter { it.loggedAt in dayStart until dayEnd }.sumOf { it.calories }
            ChartPoint(dayLabel(date), total.toFloat())
        }
        _nutritionWeek.value = points
    }

    private suspend fun loadWorkStatus() {
        val tasks = workDao.observeAll().first()
        val points = WorkStatus.entries.map { status ->
            ChartPoint(status.label.take(8), tasks.count { it.status == status }.toFloat())
        }
        _workStatus.value = points
    }

    private suspend fun loadHealthSteps() {
        _healthAvailable.value = healthBridge.availability() == HealthConnectBridge.Availability.AVAILABLE &&
            healthBridge.hasAllPermissions()
        if (!_healthAvailable.value) {
            _healthSteps.value = emptyList()
            return
        }
        val steps = healthBridge.readStepsForLastDays(7)
        _healthSteps.value = steps.map { (date, count) -> ChartPoint(dayLabel(date), count.toFloat()) }
    }

    /** Снапшот всех собственных данных приложения (roadmap п.8 "Хранение и экспорт"). */
    suspend fun buildExportJson(): String {
        val plans = planDao.observeAll().first()
        val work = workDao.observeAll().first()
        val meals = mealLogDao.getAll()

        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())

        val plansArray = JSONArray()
        plans.forEach { p ->
            plansArray.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("title", p.title)
                    put("description", p.description)
                    put("progressPercent", p.progressPercent)
                    put("lastProgressNote", p.lastProgressNote)
                    put("createdAt", p.createdAt)
                    put("updatedAt", p.updatedAt)
                }
            )
        }
        root.put("plans", plansArray)

        val workArray = JSONArray()
        work.forEach { w ->
            workArray.put(
                JSONObject().apply {
                    put("id", w.id)
                    put("title", w.title)
                    put("description", w.description)
                    put("project", w.project)
                    put("status", w.status.name)
                    put("deadlineMillis", w.deadlineMillis ?: JSONObject.NULL)
                    put("createdAt", w.createdAt)
                    put("updatedAt", w.updatedAt)
                }
            )
        }
        root.put("work", workArray)

        val mealsArray = JSONArray()
        meals.forEach { m ->
            mealsArray.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("foodName", m.foodName)
                    put("grams", m.grams)
                    put("calories", m.calories)
                    put("protein", m.protein)
                    put("fat", m.fat)
                    put("carbs", m.carbs)
                    put("loggedAt", m.loggedAt)
                }
            )
        }
        root.put("nutrition", mealsArray)

        return root.toString(2)
    }
}
