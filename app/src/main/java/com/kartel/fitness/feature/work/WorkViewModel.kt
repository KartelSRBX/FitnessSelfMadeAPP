package com.kartel.fitness.feature.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.fitness.core.calendar.CalendarBridge
import com.kartel.fitness.core.data.work.WorkDao
import com.kartel.fitness.core.data.work.WorkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkViewModel(
    private val dao: WorkDao,
    private val calendarBridge: CalendarBridge,
) : ViewModel() {

    val tasks: StateFlow<List<WorkEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasCalendarPermission: Boolean
        get() = calendarBridge.hasCalendarPermission()

    fun addTask(title: String, description: String, project: String, deadlineMillis: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            var task = WorkEntity(
                title = title.trim(),
                description = description.trim(),
                project = project.trim(),
                deadlineMillis = deadlineMillis,
            )
            val id = dao.upsert(task)
            task = task.copy(id = id)
            syncCalendar(task)
        }
    }

    fun cycleStatus(task: WorkEntity) {
        viewModelScope.launch {
            dao.update(task.copy(status = task.status.next(), updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTask(task: WorkEntity) {
        viewModelScope.launch {
            task.calendarEventId?.let { eventId ->
                withContext(Dispatchers.IO) { calendarBridge.deleteEvent(eventId) }
            }
            dao.delete(task)
        }
    }

    private suspend fun syncCalendar(task: WorkEntity) {
        val deadline = task.deadlineMillis ?: return
        if (!calendarBridge.hasCalendarPermission()) return
        val eventId = withContext(Dispatchers.IO) {
            calendarBridge.upsertDeadlineEvent(task.calendarEventId, task.title, deadline)
        } ?: return
        if (eventId != task.calendarEventId) {
            dao.update(task.copy(calendarEventId = eventId))
        }
    }
}
