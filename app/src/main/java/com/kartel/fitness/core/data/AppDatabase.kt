package com.kartel.fitness.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kartel.fitness.core.data.nutrition.MealLogDao
import com.kartel.fitness.core.data.nutrition.MealLogEntity
import com.kartel.fitness.core.data.plans.PlanDao
import com.kartel.fitness.core.data.plans.PlanEntity
import com.kartel.fitness.core.data.timer.AlarmDao
import com.kartel.fitness.core.data.timer.AlarmEntity
import com.kartel.fitness.core.data.timer.TimerDao
import com.kartel.fitness.core.data.timer.TimerEntity
import com.kartel.fitness.core.data.work.WorkChecklistDao
import com.kartel.fitness.core.data.work.WorkChecklistItem
import com.kartel.fitness.core.data.work.WorkDao
import com.kartel.fitness.core.data.work.WorkEntity

/**
 * Единая локальная Room-база для всех модулей (Plans/Work/Nutrition и т.п.).
 * Health и Schedule читают/пишут в основном через системные Health Connect
 * и Calendar Provider — здесь хранится только то, что этим системам не принадлежит.
 *
 * Миграции пока не пишутся (ранняя разработка) — FitnessApp использует
 * fallbackToDestructiveMigration(), см. TODO там же.
 */
@Database(
    entities = [
        PlanEntity::class, WorkEntity::class, MealLogEntity::class, WorkChecklistItem::class,
        TimerEntity::class, AlarmEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun workDao(): WorkDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun workChecklistDao(): WorkChecklistDao
    abstract fun timerDao(): TimerDao
    abstract fun alarmDao(): AlarmDao
}
