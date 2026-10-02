package com.rameshai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.media.AudioAttributes
import java.util.Locale
import java.util.UUID

/**
 * Default [TextToSpeechEngine] using Android's built-in TTS. Kept behind the
 * interface so a future provider (e.g. a cloud TTS with more natural Hindi/
 * Hinglish voices) can be swapped in without touching [com.rameshai.core.AssistantOrchestrator]
 * or the UI.
 */
class AndroidTextToSpeechEngine(context: Context) : TextToSpeechEngine {

    private var ready = false
    private var pendingRate = 1.0f
    private var preferFemale = true
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts.setSpeechRate(pendingRate)
                tts.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            }
        }
    }

    override fun speak(text: String, languageTag: String, onDone: () -> Unit) {
        if (!ready) {
            onDone()
            return
        }
        val locale = Locale.forLanguageTag(languageTag)
        var result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            val fallback = if (languageTag.startsWith("hi", ignoreCase = true)) Locale("en", "IN") else Locale.US
            tts.setLanguage(fallback)
        }
        if (preferFemale) selectFemaleVoice(languageTag)
        val utteranceId = UUID.randomUUID().toString()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { onDone() }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() {
        if (::tts.isInitialized) tts.stop()
    }

    fun setFemaleVoice(enabled: Boolean) {
        preferFemale = enabled
        if (ready) selectFemaleVoice("hi-IN")
    }

    private fun selectFemaleVoice(languageTag: String) {
        if (!::tts.isInitialized || !ready) return
        val locale = Locale.forLanguageTag(languageTag)
        val voices = tts.voices ?: return
        val compatible = voices.filter { it.locale.language == locale.language }
        // Android does not expose a universal gender flag. Prefer vendor voice names
        // that commonly identify female voices, then fall back to the first compatible voice.
        val female = compatible.firstOrNull { v ->
            val n = v.name.lowercase(Locale.ROOT)
            n.contains("female") || n.contains("woman") || n.contains("zira") || n.contains("samantha") || n.contains("veena") || n.contains("priya")
        }
        tts.voice = female ?: compatible.firstOrNull()
        tts.setPitch(if (preferFemale) 1.08f else 1.0f)
    }

    override fun setSpeechRate(rate: Float) {
        pendingRate = rate
        if (ready) tts.setSpeechRate(rate)
    }

    override fun release() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}
