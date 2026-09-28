package com.kartel.fitness.core.data.timer

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Будильник общего назначения на конкретное время (одноразовый — ближайшее наступление). */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val triggerAtMillis: Long,
    val enabled: Boolean = true,
)
