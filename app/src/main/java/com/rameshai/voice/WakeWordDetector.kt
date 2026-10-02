package com.rameshai.voice

/**
 * Continuous/background listening architecture: repeatedly runs short
 * recognition passes and checks whether the configured wake phrase (default
 * "RAMESH AI") appears in the transcript. On match, stops passive listening and
 * hands off to a full command-listening session.
 *
 * This is a simple, battery-aware polling approach built on the same
 * [SpeechToTextEngine] used for commands — no separate always-on DSP wake-word
 * model is bundled. It only runs when the user has explicitly enabled
 * "Background listening" in Settings (RuntimeConfig.featureBackgroundListening),
 * and only while the foreground service notification is visible, so the user
 * always knows when the mic may be active.
 */
class WakeWordDetector(
    private val speechEngine: SpeechToTextEngine,
    private var wakePhrase: String
) {
    private var active = false

    fun updateWakePhrase(phrase: String) {
        wakePhrase = phrase
    }

    fun start(languageTag: String, onWakeWordDetected: () -> Unit, onListeningError: (String) -> Unit) {
        if (active) return
        active = true
        listenPass(languageTag, onWakeWordDetected, onListeningError)
    }

    fun stop() {
        active = false
        speechEngine.stopListening()
    }

    private fun listenPass(
        languageTag: String,
        onWakeWordDetected: () -> Unit,
        onListeningError: (String) -> Unit
    ) {
        if (!active) return
        speechEngine.startListening(languageTag) { event ->
            when (event) {
                is VoiceEvent.FinalResult -> {
                    if (event.text.contains(wakePhrase, ignoreCase = true)) {
                        active = false
                        onWakeWordDetected()
                    } else if (active) {
                        listenPass(languageTag, onWakeWordDetected, onListeningError) // keep passively listening
                    }
                }
                is VoiceEvent.TimedOut -> if (active) listenPass(languageTag, onWakeWordDetected, onListeningError)
                is VoiceEvent.Error -> {
                    // Don't hard-fail background listening on a single recognition hiccup.
                    if (active) listenPass(languageTag, onWakeWordDetected, onListeningError)
                    else onListeningError(event.message)
                }
                else -> Unit
            }
        }
    }
}
