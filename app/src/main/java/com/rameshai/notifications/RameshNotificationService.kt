package com.rameshai.notifications

import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Optional. Only receives events once the user explicitly grants Notification
 * Access in Settings — [isEnabled] is checked by [com.rameshai.tools.ToolExecutor]
 * before any read, and the app never nags the user to turn this on.
 *
 * Notification content is kept ONLY in memory (a small ring buffer), never
 * written to disk or sent to any AI provider unless the user's own command
 * ("last notification kya aaya?") triggers a read, and even then only the
 * matching notification text is included in that single request — not the
 * whole buffer.
 */
class RameshNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeInstance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (activeInstance == this) activeInstance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val appLabel = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) {
            sbn.packageName
        }
        val title = sbn.notification.extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString().orEmpty()

        buffer.addFirst(BufferedNotification(appLabel, title, text, System.currentTimeMillis()))
        while (buffer.size > MAX_BUFFER) buffer.removeLast()
    }

    private data class BufferedNotification(val appLabel: String, val title: String, val text: String, val timestamp: Long)

    companion object {
        private const val MAX_BUFFER = 20
        private var activeInstance: RameshNotificationService? = null
        private val buffer = ConcurrentLinkedDeque<BufferedNotification>()

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?: return false
            return enabled.contains(context.packageName)
        }

        /** Returns spoken-friendly strings for the most recent notifications, optionally filtered by app. */
        fun lastNotifications(filterAppName: String?, limit: Int = 3): List<String> {
            val filtered = if (filterAppName.isNullOrBlank()) buffer.toList()
            else buffer.filter { it.appLabel.contains(filterAppName, ignoreCase = true) }
            return filtered.take(limit).map { "${it.appLabel}: ${it.title} - ${it.text}".trim(':', ' ', '-') }
        }
    }
}
