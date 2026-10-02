package com.rameshai.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rameshai.RameshAIApplication
import com.rameshai.core.AssistantForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AlarmManager alarms do not survive a reboot, so every enabled reminder is
 * re-armed here. Also restarts the background-listening foreground service,
 * but only if the user had it enabled (RuntimeConfig.featureBackgroundListening) —
 * never starts background listening on its own on a device where the owner
 * hadn't turned that on.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as RameshAIApplication
        CoroutineScope(Dispatchers.IO).launch {
            val dao = app.reminderDatabase.reminderDao()
            ReminderScheduler(context, dao).rescheduleAllAfterBoot()

            if (app.configRepository.current().featureBackgroundListening) {
                AssistantForegroundService.start(context)
            }
        }
    }
}
