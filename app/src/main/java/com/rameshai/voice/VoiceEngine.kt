package com.rameshai.voice

/** Assistant UI/behavior state, mirrored 1:1 by the orb animation. */
enum class AssistantState { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

sealed class VoiceEvent {
    data class PartialResult(val text: String) : VoiceEvent()
    data class FinalResult(val text: String) : VoiceEvent()
    data class Error(val message: String) : VoiceEvent()
    object TimedOut : VoiceEvent()
    object SpeechStarted : VoiceEvent()
    object SpeechEnded : VoiceEvent()
}

/**
 * Abstraction over speech-to-text so the recognition backend can be swapped
 * later (on-device model, cloud STT, etc.) without touching orchestrator or UI
 * code — everything downstream depends only on this interface.
 *
 * IMPORTANT: recognition here is for COMMAND UNDERSTANDING, not identity.
 * Distinguishing *what* was said from *who* said it is [VoiceProfileVerifier]'s
 * job, and that layer is explicitly convenience-only (see its docs) — never
 * treat successful recognition as proof of the owner's identity for sensitive
 * actions.
 */
interface SpeechToTextEngine {
    fun startListening(languageTag: String, onEvent: (VoiceEvent) -> Unit)
    fun stopListening()
    fun isListening(): Boolean
    fun release()
}

/** Abstraction over text-to-speech, swappable the same way as [SpeechToTextEngine]. */
interface TextToSpeechEngine {
    fun speak(text: String, languageTag: String, onDone: () -> Unit = {})
    fun stop()
    fun setSpeechRate(rate: Float)
    fun release()
}
