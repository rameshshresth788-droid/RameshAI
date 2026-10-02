package com.rameshai.core

import android.content.Context
import com.rameshai.ai.AIProviderFactory
import com.rameshai.ai.AIResult
import com.rameshai.ai.ChatMessage
import com.rameshai.ai.ToolCall
import com.rameshai.apps.AppRegistry
import com.rameshai.config.ConfigRepository
import com.rameshai.memory.MemoryRepository
import com.rameshai.news.NewsRepository
import com.rameshai.reminders.ReminderScheduler
import com.rameshai.routine.RoutineRepository
import com.rameshai.tools.PhoneActionTools
import com.rameshai.tools.ToolDefinitions
import com.rameshai.tools.ToolExecutionResult
import com.rameshai.tools.ToolExecutor
import com.rameshai.voice.AssistantState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single place that wires: user speech -> AI provider -> validated tool
 * execution -> spoken response. UI (HomeScreen) and the background service both
 * drive the same orchestrator so behavior is identical whether triggered by tap,
 * wake word, or a scheduled routine announcement.
 */
class AssistantOrchestrator(
    context: Context,
    private val configRepository: ConfigRepository,
    private val memoryRepository: MemoryRepository,
    reminderScheduler: ReminderScheduler,
    routineRepository: RoutineRepository,
    newsRepository: NewsRepository
) {
    private val appContext = context.applicationContext
    private val appRegistry = AppRegistry(appContext).apply { refresh() }
    private val phoneActions = PhoneActionTools(appContext)
    private val toolExecutor = ToolExecutor(
        appContext, appRegistry, phoneActions, reminderScheduler, routineRepository, newsRepository
    )

    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private var pendingConfirmation: ToolCall? = null

    /** Main entry point: user said [userUtterance]. Returns the text to speak back. */
    suspend fun handleUserUtterance(userUtterance: String): String {
        _state.value = AssistantState.THINKING
        memoryRepository.addShortTerm("User: $userUtterance")

        // A pending risky action is awaiting yes/no confirmation.
        pendingConfirmation?.let { call ->
            pendingConfirmation = null
            val confirmed = isAffirmative(userUtterance)
            return if (confirmed) finishToolExecution(call, confirmed = true)
            else speak("Theek hai, cancel kar diya.")
        }

        val config = configRepository.current()
        if (!configRepository.isAiConfigured()) {
            return speak("AI API key set nahi hai. Settings > AI me API key configure karo.")
        }

        val history = buildHistory(config.systemPrompt, userUtterance)
        val provider = AIProviderFactory.create(config)
        val toolNames = ToolDefinitions.ALL.map { it.name }

        return when (val result = provider.send(history, toolNames)) {
            is AIResult.Reply -> speak(result.text)
            is AIResult.Error -> {
                _state.value = AssistantState.ERROR
                speak(result.message)
            }
            is AIResult.ToolInvocation -> {
                result.spokenAck?.let { speak(it, updateState = false) }
                finishToolExecution(result.call, confirmed = false)
            }
        }
    }

    private suspend fun finishToolExecution(call: ToolCall, confirmed: Boolean): String {
        return when (val execResult = toolExecutor.execute(call, confirmed)) {
            is ToolExecutionResult.Success -> speak(execResult.spokenMessage)
            is ToolExecutionResult.Failure -> {
                _state.value = AssistantState.ERROR
                speak(execResult.spokenMessage)
            }
            is ToolExecutionResult.NeedsConfirmation -> {
                pendingConfirmation = execResult.pendingCall
                speak(execResult.spokenPrompt)
            }
        }
    }

    private suspend fun buildHistory(systemPrompt: String, userUtterance: String): List<ChatMessage> {
        val recent = memoryRepository.recentContext()
        val preferences = memoryRepository.preferences()
        val systemWithMemory = buildString {
            append(systemPrompt)
            if (preferences.isNotEmpty()) {
                append("\n\nKnown user preferences:\n")
                preferences.forEach { append("- $it\n") }
            }
        }
        val history = mutableListOf(ChatMessage(ChatMessage.Role.SYSTEM, systemWithMemory))
        recent.forEach { history.add(ChatMessage(ChatMessage.Role.USER, it)) }
        history.add(ChatMessage(ChatMessage.Role.USER, userUtterance))
        return history
    }

    private suspend fun speak(text: String, updateState: Boolean = true): String {
        memoryRepository.addShortTerm("Assistant: $text")
        _lastSpokenText.value = text
        if (updateState) _state.value = AssistantState.SPEAKING
        return text
    }

    fun onSpeakingFinished() {
        if (_state.value == AssistantState.SPEAKING) _state.value = AssistantState.IDLE
    }

    fun onListeningStarted() { _state.value = AssistantState.LISTENING }
    fun onError(message: String) {
        _lastSpokenText.value = message
        _state.value = AssistantState.ERROR
    }
    fun resetToIdle() { _state.value = AssistantState.IDLE }

    private fun isAffirmative(text: String): Boolean {
        val t = text.trim().lowercase()
        return listOf("haan", "yes", "confirm", "ok", "theek hai", "bilkul", "haa").any { t.contains(it) }
    }
}
