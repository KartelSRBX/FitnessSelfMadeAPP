package com.kartel.fitness.core.data.timer

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Активный таймер обратного отсчёта общего назначения (не привязан к задаче/дедлайну). */
@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val endMillis: Long,
)
