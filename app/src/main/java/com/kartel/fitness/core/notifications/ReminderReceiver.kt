package com.kartel.fitness.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Срабатывает по будильнику AlarmManager и показывает уведомление-напоминание. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val id = intent.getIntExtra(EXTRA_ID, 0)
        NotificationHelper.show(context, id, title, body)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_ID = "id"
    }
}
