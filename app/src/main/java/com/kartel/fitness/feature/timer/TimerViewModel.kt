package com.kartel.fitness.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.fitness.core.data.timer.AlarmDao
import com.kartel.fitness.core.data.timer.AlarmEntity
import com.kartel.fitness.core.data.timer.TimerDao
import com.kartel.fitness.core.data.timer.TimerEntity
import com.kartel.fitness.core.notifications.ReminderScheduler
import java.util.Calendar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Таймеры и будильники общего назначения (roadmap-запрос, не привязаны к дедлайнам задач). */
class TimerViewModel(
    private val timerDao: TimerDao,
    private val alarmDao: AlarmDao,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val timers: StateFlow<List<TimerEntity>> = timerDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun startTimer(label: String, durationMillis: Long) {
        if (durationMillis <= 0) return
        viewModelScope.launch {
            val endMillis = System.currentTimeMillis() + durationMillis
            val displayLabel = label.ifBlank { "Таймер" }
            val id = timerDao.insert(TimerEntity(label = displayLabel, endMillis = endMillis))
            reminderScheduler.schedule(
                key = "timer-$id",
                triggerAtMillis = endMillis,
                title = displayLabel,
                body = "Время вышло",
            )
        }
    }

    fun cancelTimer(timer: TimerEntity) {
        viewModelScope.launch {
            reminderScheduler.cancel("timer-${timer.id}")
            timerDao.delete(timer)
        }
    }

    fun addAlarm(label: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            val triggerAt = nextOccurrence(hour, minute)
            val displayLabel = label.ifBlank { "Будильник" }
            val id = alarmDao.insert(
                AlarmEntity(label = displayLabel, hour = hour, minute = minute, triggerAtMillis = triggerAt)
            )
            reminderScheduler.schedule(
                key = "alarm-$id",
                triggerAtMillis = triggerAt,
                title = displayLabel,
                body = "Будильник",
            )
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val enabled = !alarm.enabled
            if (enabled) {
                val triggerAt = nextOccurrence(alarm.hour, alarm.minute)
                alarmDao.update(alarm.copy(enabled = true, triggerAtMillis = triggerAt))
                reminderScheduler.schedule(
                    key = "alarm-${alarm.id}",
                    triggerAtMillis = triggerAt,
                    title = alarm.label,
                    body = "Будильник",
                )
            } else {
                alarmDao.update(alarm.copy(enabled = false))
                reminderScheduler.cancel("alarm-${alarm.id}")
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            reminderScheduler.cancel("alarm-${alarm.id}")
            alarmDao.delete(alarm)
        }
    }

    private fun nextOccurrence(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.timeInMillis <= now.timeInMillis) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return candidate.timeInMillis
    }
}
