package com.kartel.fitness.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Планирует/отменяет напоминания через AlarmManager. [key] — стабильный идентификатор
 * источника (например "work-42" или "event-7"), из него выводится requestCode, поэтому
 * повторный schedule() с тем же key просто заменяет предыдущий будильник.
 *
 * Если у приложения нет разрешения на точные будильники (Android 12+, SCHEDULE_EXACT_ALARM
 * не выдан вручную в настройках) — тихо откатывается на неточный будильник вместо падения:
 * напоминание всё равно придёт, возможно с небольшой задержкой.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(key: String, triggerAtMillis: Long, title: String, body: String) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val pendingIntent = buildPendingIntent(key, title, body)
        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        try {
            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(key: String) {
        alarmManager.cancel(buildPendingIntent(key, title = "", body = ""))
    }

    fun hasExactAlarmPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun buildPendingIntent(key: String, title: String, body: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
            putExtra(ReminderReceiver.EXTRA_ID, key.hashCode())
        }
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
