package com.rameshai.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar

/**
 * Creates [ReminderEntity] rows and schedules them with AlarmManager. Uses
 * `setExactAndAllowWhileIdle` so reminders fire on time even under Doze, and
 * degrades gracefully to an inexact alarm if SCHEDULE_EXACT_ALARM isn't granted
 * on Android 12+ rather than crashing.
 */
class ReminderScheduler(private val context: Context, private val dao: ReminderDao) {

    suspend fun schedule(message: String, timeSpec: String, recurrenceSpec: String): Boolean {
        val triggerAt = parseTime(timeSpec) ?: return false
        val recurrence = when (recurrenceSpec.lowercase()) {
            "daily" -> Recurrence.DAILY
            "weekly" -> Recurrence.WEEKLY
            "custom_days" -> Recurrence.CUSTOM_DAYS
            else -> Recurrence.NONE
        }
        val entity = ReminderEntity(
            message = message,
            triggerAtMillis = triggerAt,
            recurrence = recurrence
        )
        val id = dao.insert(entity)
        armAlarm(entity.copy(id = id))
        return true
    }

    suspend fun rescheduleAllAfterBoot() {
        dao.getAllEnabled().forEach { armAlarm(it) }
    }

    suspend fun cancel(reminder: ReminderEntity) {
        val pi = pendingIntentFor(reminder.id)
        alarmManager().cancel(pi)
        dao.delete(reminder)
    }

    /** Public re-arm entry point used by [ReminderReceiver] after a recurring reminder fires. */
    fun rearm(reminder: ReminderEntity) = armAlarm(reminder)

    private fun armAlarm(reminder: ReminderEntity) {
        val am = alarmManager()
        val pi = pendingIntentFor(reminder.id, reminder.message)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pi)
        }
    }

    private fun pendingIntentFor(id: Long, message: String = ""): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, id)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
        }
        return PendingIntent.getBroadcast(
            context, id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alarmManager() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Accepts either a full ISO-8601 local datetime ("2026-09-27T08:00:00") or a
     * bare "HH:mm" for the next occurrence of that time (today if still in the
     * future, otherwise tomorrow) — covers both "kal 8 baje" (resolved upstream
     * to a full datetime by the orchestrator/AI) and simple "8 baje" commands.
     */
    private fun parseTime(spec: String): Long? {
        return try {
            LocalDateTime.parse(spec, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            try {
                val time = LocalTime.parse(spec, DateTimeFormatter.ofPattern("H:mm"))
                var target = LocalDateTime.of(LocalDate.now(), time)
                if (target.isBefore(LocalDateTime.now())) target = target.plusDays(1)
                target.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e2: Exception) {
                null
            }
        }
    }
}
