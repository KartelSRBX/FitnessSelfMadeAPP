package com.kartel.fitness.core.data.work

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Рабочая задача/проект (roadmap, модуль "Work").
 * Задача с [deadlineMillis] автоматически получает событие в системном календаре
 * (см. core/calendar/CalendarBridge) — [calendarEventId] хранит id этого события,
 * чтобы обновлять/удалять его вместо создания дублей.
 *
 * TODO (roadmap): периодическая синхронизация с Bitrix24 REST API — отдельным
 * шагом, когда будет webhook/OAuth-приложение для аккаунта.
 */
@Entity(tableName = "work_tasks")
@TypeConverters(WorkStatusConverter::class)
data class WorkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val project: String = "",
    val status: WorkStatus = WorkStatus.TODO,
    val deadlineMillis: Long? = null,
    val calendarEventId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Момент, когда статус стал DONE — трек выполнения задач для аналитики. null, если не завершена. */
    val completedAt: Long? = null,
)

class WorkStatusConverter {
    @TypeConverter
    fun fromStatus(status: WorkStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): WorkStatus = WorkStatus.valueOf(value)
}
