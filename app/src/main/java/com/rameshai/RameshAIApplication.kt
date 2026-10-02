package com.rameshai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.rameshai.config.ConfigHttpServer
import com.rameshai.config.ConfigRepository
import com.rameshai.debug.CrashLogger
import com.rameshai.memory.MemoryDatabase
import com.rameshai.reminders.ReminderDatabase

/**
 * Application entry point.
 *
 * Responsibilities:
 *  - Load runtime configuration (config/assistant.json) into memory. This is the
 *    mechanism that lets API keys / model / prompts / wake phrase change WITHOUT
 *    an APK rebuild (see README "No-Rebuild Configuration").
 *  - Lazily hold singleton references to Room databases used across the app.
 *  - Register notification channels required by the foreground service and
 *    reminder notifications.
 */
class RameshAIApplication : Application() {

    lateinit var configRepository: ConfigRepository
        private set

    private lateinit var configHttpServer: ConfigHttpServer

    val memoryDatabase: MemoryDatabase by lazy { MemoryDatabase.build(this) }
    val reminderDatabase: ReminderDatabase by lazy { ReminderDatabase.build(this) }

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        configRepository = ConfigRepository(this)
        configRepository.loadOrCreateDefault()
        createNotificationChannels()

        // Loopback-only server so tools/config.sh (Termux, same device) can
        // read/update config without a PC or ADB. See ConfigHttpServer for
        // the security properties (127.0.0.1 only, masked key on reads).
        configHttpServer = ConfigHttpServer(configRepository)
        configHttpServer.start()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ASSISTANT,
                getString(R.string.assistant_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "RAMESH AI Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    companion object {
        const val CHANNEL_ASSISTANT = "ramesh_assistant_channel"
        const val CHANNEL_REMINDERS = "ramesh_reminders_channel"
    }
}
