package com.kartel.fitness.feature.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.fitness.core.calendar.CalendarBridge
import com.kartel.fitness.core.data.work.WorkChecklistDao
import com.kartel.fitness.core.data.work.WorkChecklistItem
import com.kartel.fitness.core.data.work.WorkDao
import com.kartel.fitness.core.data.work.WorkEntity
import com.kartel.fitness.core.data.work.WorkStatus
import com.kartel.fitness.core.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkViewModel(
    private val dao: WorkDao,
    private val calendarBridge: CalendarBridge,
    private val reminderScheduler: ReminderScheduler,
    private val checklistDao: WorkChecklistDao,
) : ViewModel() {

    companion object {
        private const val DEADLINE_LEAD_MILLIS = 60 * 60 * 1000L // напомнить за час до дедлайна
    }

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
            scheduleReminder(task)
        }
    }

    fun cycleStatus(task: WorkEntity) {
        viewModelScope.launch {
            val nextStatus = task.status.next()
            val updated = task.copy(
                status = nextStatus,
                updatedAt = System.currentTimeMillis(),
                completedAt = if (nextStatus == WorkStatus.DONE) System.currentTimeMillis() else null,
            )
            dao.update(updated)
            // Задача сделана или снова в работе — напоминание о дедлайне больше не актуально/пересоздастся.
            if (updated.status == WorkStatus.DONE) {
                reminderScheduler.cancel("work-${task.id}")
            } else {
                scheduleReminder(updated)
            }
        }
    }

    fun deleteTask(task: WorkEntity) {
        viewModelScope.launch {
            task.calendarEventId?.let { eventId ->
                withContext(Dispatchers.IO) { calendarBridge.deleteEvent(eventId) }
            }
            reminderScheduler.cancel("work-${task.id}")
            checklistDao.deleteAllForTask(task.id)
            dao.delete(task)
        }
    }

    fun checklistFor(taskId: Long): Flow<List<WorkChecklistItem>> = checklistDao.observeForTask(taskId)

    fun addChecklistItem(taskId: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { checklistDao.insert(WorkChecklistItem(workTaskId = taskId, text = text.trim())) }
    }

    fun toggleChecklistItem(item: WorkChecklistItem) {
        viewModelScope.launch { checklistDao.update(item.copy(isDone = !item.isDone)) }
    }

    fun deleteChecklistItem(item: WorkChecklistItem) {
        viewModelScope.launch { checklistDao.delete(item) }
    }

    private fun scheduleReminder(task: WorkEntity) {
        val deadline = task.deadlineMillis
        if (deadline == null) {
            reminderScheduler.cancel("work-${task.id}")
            return
        }
        reminderScheduler.schedule(
            key = "work-${task.id}",
            triggerAtMillis = deadline - DEADLINE_LEAD_MILLIS,
            title = "Дедлайн через час: ${task.title}",
            body = if (task.project.isNotBlank()) task.project else "Work",
        )
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
