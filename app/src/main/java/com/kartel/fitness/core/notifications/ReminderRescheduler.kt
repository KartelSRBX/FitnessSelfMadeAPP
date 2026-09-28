package com.kartel.fitness.core.notifications

import android.content.Context
import com.kartel.fitness.FitnessApp
import com.kartel.fitness.core.calendar.CalendarBridge
import java.util.Calendar
import kotlinx.coroutines.flow.first

/**
 * AlarmManager сбрасывает все будильники при перезагрузке телефона — здесь они
 * восстанавливаются из тех же источников данных (Work-дедлайны, Schedule-события,
 * таймеры и будильники общего назначения), из которых изначально планировались.
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

        app.database.timerDao().observeAll().first().forEach { timer ->
            if (timer.endMillis > now) {
                scheduler.schedule(
                    key = "timer-${timer.id}",
                    triggerAtMillis = timer.endMillis,
                    title = timer.label,
                    body = "Время вышло",
                )
            }
        }

        val alarmDao = app.database.alarmDao()
        alarmDao.observeAll().first().forEach { alarm ->
            if (!alarm.enabled) return@forEach
            var triggerAt = alarm.triggerAtMillis
            if (triggerAt <= now) {
                // Будильник пропущен, пока телефон был выключен — переносим на следующее
                // наступление этого же времени, а не тихо теряем.
                val candidate = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (candidate.timeInMillis <= now) candidate.add(Calendar.DAY_OF_YEAR, 1)
                triggerAt = candidate.timeInMillis
                alarmDao.update(alarm.copy(triggerAtMillis = triggerAt))
            }
            scheduler.schedule(
                key = "alarm-${alarm.id}",
                triggerAtMillis = triggerAt,
                title = alarm.label,
                body = "Будильник",
            )
        }
    }
}
