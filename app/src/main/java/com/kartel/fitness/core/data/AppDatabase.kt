package com.kartel.fitness.core.data

/**
 * Единая локальная Room-база для всех модулей (Plans/Work/Schedule-кэш/Nutrition).
 * Health и Schedule читают/пишут в основном через системные Health Connect
 * и Calendar Provider — здесь хранится только то, что этим системам не принадлежит.
 *
 * TODO: как только появится первая entity (roadmap п.3 "Plans"), превратить это
 * в абстрактный класс:
 *
 *   @Database(entities = [PlanEntity::class, ...], version = 1, exportSchema = false)
 *   abstract class AppDatabase : RoomDatabase() {
 *       abstract fun planDao(): PlanDao
 *   }
 *
 * Room не даёт скомпилировать @Database без entities, поэтому до первой
 * реализации модуля класс оставлен пустой заглушкой.
 */
object AppDatabase
