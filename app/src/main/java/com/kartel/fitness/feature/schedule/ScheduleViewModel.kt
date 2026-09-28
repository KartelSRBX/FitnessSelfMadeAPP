package com.kartel.fitness.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.fitness.core.calendar.CalendarBridge
import com.kartel.fitness.core.calendar.CalendarEvent
import com.kartel.fitness.core.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Модуль Schedule (roadmap п.5): читает и пишет события в тот же
 * календарь-аккаунт "Мои задачи и расписание", в который Work кладёт дедлайны —
 * поэтому [events] уже объединённый список "свои события + дедлайны Work".
 *
 * Calendar Provider не отдаёт изменения реактивно без ContentObserver, поэтому
 * список обновляется вручную: при входе на экран и после каждой операции.
 */
class ScheduleViewModel(
    private val calendarBridge: CalendarBridge,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    val hasCalendarPermission: Boolean
        get() = calendarBridge.hasCalendarPermission()

    fun refresh() {
        if (!hasCalendarPermission) return
        viewModelScope.launch {
            _events.value = withContext(Dispatchers.IO) { calendarBridge.queryEvents() }
        }
    }

    fun addEvent(title: String, description: String, startMillis: Long, endMillis: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val eventId = withContext(Dispatchers.IO) {
                calendarBridge.insertEvent(trimmedTitle, description.trim(), startMillis, endMillis)
            }
            if (eventId != null) {
                reminderScheduler.schedule(
                    key = "event-$eventId",
                    triggerAtMillis = startMillis,
                    title = trimmedTitle,
                    body = "Событие в Schedule",
                )
            }
            refresh()
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { calendarBridge.deleteEvent(event.id) }
            reminderScheduler.cancel("event-${event.id}")
            refresh()
        }
    }
}
