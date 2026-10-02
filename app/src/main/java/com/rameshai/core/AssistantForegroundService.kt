package com.rameshai.core

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rameshai.MainActivity
import com.rameshai.R
import com.rameshai.RameshAIApplication
import com.rameshai.news.NewsRepository
import com.rameshai.reminders.ReminderScheduler
import com.rameshai.routine.RoutineRepository
import com.rameshai.voice.AndroidSpeechRecognizerEngine
import com.rameshai.voice.AndroidTextToSpeechEngine
import com.rameshai.voice.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service backing "background/continuous listening" (see spec
 * section 11). Only ever started when the user has explicitly enabled
 * background listening in Settings (RuntimeConfig.featureBackgroundListening) —
 * see [start]. Always shows the required ongoing notification so the user knows
 * the mic may be listening for the wake phrase, and stops immediately via
 * [stop]/STOP_ACTION when the user disables the feature.
 */
class AssistantForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var speechEngine: AndroidSpeechRecognizerEngine? = null
    private var ttsEngine: AndroidTextToSpeechEngine? = null
    private var orchestrator: AssistantOrchestrator? = null
    @Volatile private var active = false
    @Volatile private var processing = false

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        val app = application as RameshAIApplication
        val config = app.configRepository.current()
        if (!config.featureBackgroundListening) {
            stopSelf()
            return
        }
        speechEngine = AndroidSpeechRecognizerEngine(this)
        ttsEngine = AndroidTextToSpeechEngine(this).also {
            it.setSpeechRate(config.voiceSpeed)
            it.setFemaleVoice(config.femaleVoice)
        }
        orchestrator = AssistantOrchestrator(
            this,
            app.configRepository,
            com.rameshai.memory.MemoryRepository(app.memoryDatabase.memoryDao()),
            ReminderScheduler(this, app.reminderDatabase.reminderDao()),
            RoutineRepository(this),
            NewsRepository(this, config)
        )
        active = true
        listenAgain()
    }

    private fun listenAgain(delayMs: Long = 250L) {
        if (!active || processing) return
        scope.launch {
            kotlinx.coroutines.delay(delayMs)
            if (!active || processing) return@launch
            val engine = speechEngine ?: return@launch
            val config = (application as RameshAIApplication).configRepository.current()
            engine.startListening(config.language) { event ->
                when (event) {
                    is com.rameshai.voice.VoiceEvent.FinalResult -> {
                        if (event.text.isNotBlank() && !processing) {
                            processing = true
                            scope.launch {
                                try {
                                    val cfg = (application as RameshAIApplication).configRepository.current()
                                    val reply = orchestrator?.handleUserUtterance(event.text).orEmpty()
                                    ttsEngine?.setSpeechRate(cfg.voiceSpeed)
                                    ttsEngine?.setFemaleVoice(cfg.femaleVoice)
                                    ttsEngine?.speak(reply.ifBlank { "Sorry master, mujhe jawab nahi mila." }, cfg.language) {
                                        processing = false
                                        listenAgain(350)
                                    }
                                } catch (_: Exception) {
                                    processing = false
                                    listenAgain(500)
                                }
                            }
                        }
                    }
                    is com.rameshai.voice.VoiceEvent.TimedOut,
                    is com.rameshai.voice.VoiceEvent.Error -> listenAgain(350)
                    else -> Unit
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            active = false
            speechEngine?.stopListening()
            stopSelf()
            return START_NOT_STICKY
        }
        if (active && !processing) listenAgain()
        return START_STICKY
    }

    override fun onDestroy() {
        active = false
        speechEngine?.release()
        ttsEngine?.release()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val openIntent = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, RameshAIApplication.CHANNEL_ASSISTANT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("RAMESH AI")
            .setContentText("Background listening active • mic ready")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.rameshai.action.STOP_ASSISTANT"

        fun start(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AssistantForegroundService::class.java).apply { action = ACTION_STOP })
        }
    }
}
