package com.kartel.fitness.core.ai

import com.kartel.fitness.core.calendar.CalendarBridge
import com.kartel.fitness.core.data.nutrition.MealLogDao
import com.kartel.fitness.core.data.nutrition.MealLogEntity
import com.kartel.fitness.core.data.plans.PlanDao
import com.kartel.fitness.core.data.plans.PlanEntity
import com.kartel.fitness.core.data.work.WorkDao
import com.kartel.fitness.core.data.work.WorkEntity
import com.kartel.fitness.core.nutrition.FoodRepository
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Выполняет [ModuleIntent], распознанный [GeminiNanoClient], реальной записью в модуль —
 * roadmap: "мини-ИИ сам разложил её по нужному модулю". Голосовой ввод будет вызывать
 * тот же класс, что и тестовый экран AI, так что логика не дублируется.
 */
class ActionRouter(
    private val planDao: PlanDao,
    private val workDao: WorkDao,
    private val mealLogDao: MealLogDao,
    private val foodRepository: FoodRepository,
    private val calendarBridge: CalendarBridge,
) {
    sealed class Result {
        data class Success(val message: String) : Result()
        data class NeedsManualInput(val message: String) : Result()
    }

    private val zone = ZoneId.systemDefault()
    private val dateFormat = SimpleDateFormat("d MMM", Locale("ru"))

    suspend fun execute(intent: ModuleIntent): Result {
        val module = intent.module
            ?: return Result.NeedsManualInput("Не распознано уверенно — выбери модуль вручную")
        return when (module) {
            AppModule.NUTRITION -> executeNutrition(intent.fields)
            AppModule.PLANS -> executePlans(intent.fields)
            AppModule.WORK -> executeWork(intent.fields)
            AppModule.SCHEDULE -> executeSchedule(intent.fields)
            AppModule.HEALTH -> Result.NeedsManualInput("Health — только чтение, писать сюда нечего")
        }
    }

    private suspend fun executeNutrition(fields: Map<String, String>): Result {
        val foodQuery = fields["food"]?.takeIf { it.isNotBlank() }
            ?: return Result.NeedsManualInput("Не понял, какой продукт — залогируй вручную на вкладке Nutrition")
        val grams = fields["grams"]?.toIntOrNull()?.takeIf { it > 0 } ?: 100
        val match = foodRepository.search(foodQuery).firstOrNull()
            ?: return Result.NeedsManualInput("«$foodQuery» нет в локальной базе — добавь вручную на вкладке Nutrition")

        val factor = grams / 100.0
        mealLogDao.insert(
            MealLogEntity(
                foodName = match.name,
                grams = grams,
                calories = match.calories * factor,
                protein = match.protein * factor,
                fat = match.fat * factor,
                carbs = match.carbs * factor,
            )
        )
        return Result.Success(
            "Записано в Nutrition: ${match.name}, ${grams}г (~${(match.calories * factor).roundToInt()} ккал)"
        )
    }

    private suspend fun executePlans(fields: Map<String, String>): Result {
        val title = fields["title"]?.takeIf { it.isNotBlank() }
            ?: return Result.NeedsManualInput("Не понял название цели — добавь вручную на вкладке Plans")
        planDao.upsert(PlanEntity(title = title, description = fields["description"] ?: ""))
        return Result.Success("Добавлено в Plans: $title")
    }

    private suspend fun executeWork(fields: Map<String, String>): Result {
        val title = fields["title"]?.takeIf { it.isNotBlank() }
            ?: return Result.NeedsManualInput("Не понял название задачи — добавь вручную на вкладке Work")
        val deadlineMillis = fields["deadlineDate"]?.let(::parseDateMillis)

        var task = WorkEntity(title = title, project = fields["project"] ?: "", deadlineMillis = deadlineMillis)
        val id = workDao.upsert(task)
        task = task.copy(id = id)

        var suffix = ""
        if (deadlineMillis != null) {
            suffix = " (дедлайн ${dateFormat.format(deadlineMillis)})"
            if (calendarBridge.hasCalendarPermission()) {
                val eventId = calendarBridge.upsertDeadlineEvent(null, title, deadlineMillis)
                if (eventId != null) workDao.update(task.copy(calendarEventId = eventId))
            }
        }
        return Result.Success("Добавлено в Work: $title$suffix")
    }

    private suspend fun executeSchedule(fields: Map<String, String>): Result {
        val title = fields["title"]?.takeIf { it.isNotBlank() }
            ?: return Result.NeedsManualInput("Не понял название события — добавь вручную на вкладке Schedule")
        val date = fields["date"]?.let(::parseLocalDate)
            ?: return Result.NeedsManualInput("Не понял дату события — добавь вручную на вкладке Schedule")
        if (!calendarBridge.hasCalendarPermission()) {
            return Result.NeedsManualInput("Нет доступа к календарю — разреши на вкладке Schedule")
        }
        val (hour, minute) = parseTime(fields["time"]) ?: (9 to 0)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli() + (hour * 60 + minute) * 60_000L
        calendarBridge.insertEvent(title, "", start, start + 60 * 60 * 1000)
        return Result.Success("Добавлено в Schedule: $title, ${dateFormat.format(start)}")
    }

    private fun parseLocalDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

    private fun parseDateMillis(value: String): Long? =
        parseLocalDate(value)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()

    private fun parseTime(value: String?): Pair<Int, Int>? {
        val parts = value?.split(":") ?: return null
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour to minute
    }
}
