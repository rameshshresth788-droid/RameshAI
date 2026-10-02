package com.rameshai.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rameshai.MainActivity
import com.rameshai.R
import com.rameshai.RameshAIApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires when a reminder's alarm triggers. Shows a high-priority notification
 * (works even if the app/orb UI isn't open) and, for recurring reminders,
 * re-arms the next occurrence.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()

        showNotification(context, message)

        val app = context.applicationContext as RameshAIApplication
        CoroutineScope(Dispatchers.IO).launch {
            val dao = app.reminderDatabase.reminderDao()
            val reminder = dao.getById(id) ?: return@launch
            if (reminder.recurrence != Recurrence.NONE) {
                val next = nextOccurrence(reminder)
                val updated = reminder.copy(triggerAtMillis = next)
                dao.update(updated)
                ReminderScheduler(context, dao).rearm(updated)
            }
        }
    }

    private fun nextOccurrence(reminder: ReminderEntity): Long {
        val oneDayMillis = 24L * 60 * 60 * 1000
        return when (reminder.recurrence) {
            Recurrence.DAILY -> reminder.triggerAtMillis + oneDayMillis
            Recurrence.WEEKLY -> reminder.triggerAtMillis + (7 * oneDayMillis)
            else -> reminder.triggerAtMillis + oneDayMillis
        }
    }

    private fun showNotification(context: Context, message: String) {
        val openIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, RameshAIApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("RAMESH AI Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(message.hashCode(), notification)
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_MESSAGE = "reminder_message"
    }
}
