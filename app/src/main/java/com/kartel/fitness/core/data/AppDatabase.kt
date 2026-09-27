package com.kartel.fitness.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Единая локальная Room-база для всех модулей (Plans/Work/Schedule-кэш/Nutrition).
 * Health и Schedule читают/пишут в основном через системные Health Connect
 * и Calendar Provider — здесь хранится только то, что этим системам не принадлежит.
 *
 * TODO: добавлять entities и DAO по мере реализации каждого модуля
 * (roadmap, "Инструкции и промпты", пп.3-8), затем указать их в @Database(entities = [...]).
 */
@Database(entities = [], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase()
