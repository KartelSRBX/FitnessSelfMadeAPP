package com.kartel.fitness.core.data.plans

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Долгосрочная личная цель без привязки к дате (roadmap, модуль "Plans").
 * У целей нет дедлайнов и напоминаний по датам — только прогресс.
 */
@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val progressPercent: Int = 0,
    val lastProgressNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
