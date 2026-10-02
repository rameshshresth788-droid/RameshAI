package com.rameshai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Default [SpeechToTextEngine] built on Android's built-in SpeechRecognizer.
 * Supports both push-to-talk (single [startListening] call) and can be driven
 * repeatedly by [com.rameshai.voice.WakeWordDetector] for a continuous-listening
 * architecture. Handles the standard error codes with clear, non-crashing
 * fallbacks instead of propagating raw Android error ints to the UI.
 */
class AndroidSpeechRecognizerEngine(private val context: Context) : SpeechToTextEngine {

    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    override fun startListening(languageTag: String, onEvent: (VoiceEvent) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onEvent(VoiceEvent.Error("Is device par speech recognition available nahi hai."))
            return
        }
        release()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { listening = true }
                override fun onBeginningOfSpeech() { onEvent(VoiceEvent.SpeechStarted) }
                override fun onRmsChanged(rmsdB: Float) { /* used by orb amplitude reactivity in UI layer */ }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false; onEvent(VoiceEvent.SpeechEnded) }

                override fun onError(error: Int) {
                    listening = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Kuch samajh nahi aaya, dobara boliye."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> null // handled as TimedOut below
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Internet connection weak hai, speech samajhne me dikkat aa rahi hai."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Microphone permission nahi mili. Settings me permission do."
                        SpeechRecognizer.ERROR_AUDIO -> "Microphone se audio nahi mil raha."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy hai, thoda ruk kar try karo."
                        else -> "Voice samajhne me error aaya."
                    }
                    if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) onEvent(VoiceEvent.TimedOut)
                    else onEvent(VoiceEvent.Error(message ?: "Voice error aaya."))
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (text.isNullOrBlank()) onEvent(VoiceEvent.Error("Kuch sunai nahi diya."))
                    else onEvent(VoiceEvent.FinalResult(text))
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) onEvent(VoiceEvent.PartialResult(text))
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
            }
            startListening(intent)
        }
    }

    override fun stopListening() {
        recognizer?.stopListening()
        listening = false
    }

    override fun isListening(): Boolean = listening

    override fun release() {
        recognizer?.destroy()
        recognizer = null
        listening = false
    }
}
