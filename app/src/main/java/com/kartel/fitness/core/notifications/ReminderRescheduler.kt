package com.kartel.fitness.core.notifications

import android.content.Context
import com.kartel.fitness.FitnessApp
import com.kartel.fitness.core.calendar.CalendarBridge
import kotlinx.coroutines.flow.first

/**
 * AlarmManager сбрасывает все будильники при перезагрузке телефона — здесь они
 * восстанавливаются из тех же источников данных (Work-дедлайны, Schedule-события),
 * из которых изначально планировались (см. WorkViewModel/ScheduleViewModel).
 */
object ReminderRescheduler {

    private const val DEADLINE_LEAD_MILLIS = 60 * 60 * 1000L // за час, как и напоминание в календаре

    suspend fun rescheduleAll(context: Context) {
        val app = context.applicationContext as FitnessApp
        val scheduler = ReminderScheduler(context)
        val now = System.currentTimeMillis()

        app.database.workDao().observeAll().first().forEach { task ->
            val deadline = task.deadlineMillis ?: return@forEach
            val remindAt = deadline - DEADLINE_LEAD_MILLIS
            if (remindAt > now) {
                scheduler.schedule(
                    key = "work-${task.id}",
                    triggerAtMillis = remindAt,
                    title = "Дедлайн через час: ${task.title}",
                    body = if (task.project.isNotBlank()) task.project else "Work",
                )
            }
        }

        CalendarBridge(context).queryEvents().forEach { event ->
            if (event.startMillis > now) {
                scheduler.schedule(
                    key = "event-${event.id}",
                    triggerAtMillis = event.startMillis,
                    title = event.title,
                    body = "Событие в Schedule",
                )
            }
        }
    }
}
