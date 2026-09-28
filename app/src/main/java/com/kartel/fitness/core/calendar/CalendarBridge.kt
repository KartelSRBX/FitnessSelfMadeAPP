package com.kartel.fitness.core.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone

/**
 * Мост к системному Calendar Provider (roadmap, модуль "Schedule").
 *
 * При первом обращении создаёт (или переиспользует) локальный календарь-аккаунт
 * "Мои задачи и расписание" внутри Calendar Provider — события Work/Schedule не
 * смешиваются с личными встречами в основном Google/Samsung календаре, но
 * остаются частью единой системы и получают стандартные системные напоминания.
 *
 * Полноценный CRUD и объединённый список событий (свои + дедлайны Work) —
 * отдельно в модуле Schedule; здесь — минимум, нужный модулю Work для дедлайнов.
 */
class CalendarBridge(private val context: Context) {

    companion object {
        private const val CALENDAR_DISPLAY_NAME = "Мои задачи и расписание"
        private const val ACCOUNT_NAME = "Fitness Assistant (локально)"
        private const val DEFAULT_REMINDER_MINUTES_BEFORE = 60L
    }

    fun hasCalendarPermission(): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureLocalCalendarId(): Long? {
        if (!hasCalendarPermission()) return null
        val resolver = context.contentResolver

        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND " +
            "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND " +
            "${CalendarContract.Calendars.NAME} = ?"
        val args = arrayOf(ACCOUNT_NAME, CalendarContract.ACCOUNT_TYPE_LOCAL, CALENDAR_DISPLAY_NAME)

        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            selection,
            args,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF4C7CFF.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }

        val uri = resolver.insert(
            CalendarContract.Calendars.CONTENT_URI
                .buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build(),
            values,
        ) ?: return null

        return ContentUris.parseId(uri)
    }

    /**
     * Создаёт (или обновляет, если [existingEventId] задан) событие-дедлайн на 1 час,
     * начиная с [deadlineMillis], со стандартным напоминанием за час. Возвращает id
     * события в календаре, либо null если разрешения на календарь не выданы.
     */
    fun upsertDeadlineEvent(existingEventId: Long?, title: String, deadlineMillis: Long): Long? {
        if (!hasCalendarPermission()) return null
        val calendarId = ensureLocalCalendarId() ?: return null
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, deadlineMillis)
            put(CalendarContract.Events.DTEND, deadlineMillis + 60 * 60 * 1000)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        val eventId = if (existingEventId != null) {
            resolver.update(
                ContentUris.appendId(CalendarContract.Events.CONTENT_URI.buildUpon(), existingEventId).build(),
                values,
                null,
                null,
            )
            existingEventId
        } else {
            val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
            val newId = ContentUris.parseId(uri)
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, newId)
                put(CalendarContract.Reminders.MINUTES, DEFAULT_REMINDER_MINUTES_BEFORE)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            newId
        }

        return eventId
    }

    fun deleteEvent(eventId: Long) {
        if (!hasCalendarPermission()) return
        context.contentResolver.delete(
            ContentUris.appendId(CalendarContract.Events.CONTENT_URI.buildUpon(), eventId).build(),
            null,
            null,
        )
    }
}
