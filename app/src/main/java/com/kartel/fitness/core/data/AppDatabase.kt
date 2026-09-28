package com.kartel.fitness.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kartel.fitness.core.data.plans.PlanDao
import com.kartel.fitness.core.data.plans.PlanEntity

/**
 * Единая локальная Room-база для всех модулей (Plans/Work/Nutrition и т.п.).
 * Health и Schedule читают/пишут в основном через системные Health Connect
 * и Calendar Provider — здесь хранится только то, что этим системам не принадлежит.
 *
 * TODO: по мере реализации Work/Nutrition добавлять их entities и DAO сюда же,
 * поднимая version и добавляя миграции.
 */
@Database(entities = [PlanEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
}
