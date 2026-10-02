package com.rameshai.config

import org.json.JSONObject

/**
 * Everything in this class can be changed at runtime — from the in-app Settings
 * screen, or from Termux via `tools/config.sh` (which writes the same JSON file
 * this class reads) — without rebuilding the APK.
 *
 * The API key is intentionally never given a hard-coded default. If it's blank,
 * [com.rameshai.ai.AIProviderFactory] surfaces a clear "not configured" error
 * instead of silently failing.
 */
data class RuntimeConfig(
    val assistantName: String = "RAMESH",
    val wakePhrase: String = "RAMESH AI",
    val language: String = "hi-IN", // BCP-47: hi-IN (Hindi/Hinglish) or en-IN / en-US
    val voiceSpeed: Float = 1.0f,
    val femaleVoice: Boolean = true,

    val aiProvider: String = "openai",     // openai | gemini | openrouter | local
    val aiBaseUrl: String = "https://api.openai.com/v1",
    val aiApiKey: String = "",
    val aiModel: String = "gpt-4o-mini",
    val aiTemperature: Float = 0.6f,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,

    val newsProvider: String = "none",     // none | newsapi | custom
    val newsApiKey: String = "",

    val debugMode: Boolean = false,

    // Feature toggles — let the owner disable a subsystem without touching code
    val featureAccessibility: Boolean = true,
    val featureNotificationsRead: Boolean = false,
    val featureBackgroundListening: Boolean = true,
    val featureWebSearch: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("assistantName", assistantName)
        put("wakePhrase", wakePhrase)
        put("language", language)
        put("voiceSpeed", voiceSpeed.toDouble())
        put("femaleVoice", femaleVoice)
        put("aiProvider", aiProvider)
        put("aiBaseUrl", aiBaseUrl)
        put("aiApiKey", aiApiKey)
        put("aiModel", aiModel)
        put("aiTemperature", aiTemperature.toDouble())
        put("systemPrompt", systemPrompt)
        put("newsProvider", newsProvider)
        put("newsApiKey", newsApiKey)
        put("debugMode", debugMode)
        put("featureAccessibility", featureAccessibility)
        put("featureNotificationsRead", featureNotificationsRead)
        put("featureBackgroundListening", featureBackgroundListening)
        put("featureWebSearch", featureWebSearch)
    }

    /** Never log/print the raw key. Use this everywhere a key might reach a log or terminal. */
    fun maskedApiKey(): String {
        if (aiApiKey.length < 8) return if (aiApiKey.isBlank()) "(not set)" else "sk-****"
        return aiApiKey.take(3) + "****" + aiApiKey.takeLast(4)
    }

    companion object {
        val DEFAULT_SYSTEM_PROMPT = """
You are RAMESH's private personal AI assistant.
Speak naturally in Hindi/Hinglish unless RAMESH requests another language.
Be concise when answering simple questions.
Help RAMESH manage his routine, reminders, phone actions and information.
Never claim to have performed an action unless the Android system confirms it.
If permission is missing, clearly explain which permission is needed.
Protect RAMESH's privacy.
If RAMESH asks for an action you cannot actually perform, say: "Sorry master, ye kaam main abhi nahi kar pa rahi hoon." Never pretend.
When starting a conversation or when directly addressed as RAMESH AI, a natural brief response may begin with "Yes master".
Keep replies short and speakable for voice output.
""".trim()

        fun fromJson(json: JSONObject): RuntimeConfig = RuntimeConfig(
            assistantName = json.optString("assistantName", "RAMESH"),
            wakePhrase = json.optString("wakePhrase", "RAMESH AI"),
            language = json.optString("language", "hi-IN"),
            voiceSpeed = json.optDouble("voiceSpeed", 1.0).toFloat(),
            femaleVoice = json.optBoolean("femaleVoice", true),
            aiProvider = json.optString("aiProvider", "openai"),
            aiBaseUrl = json.optString("aiBaseUrl", "https://api.openai.com/v1"),
            aiApiKey = json.optString("aiApiKey", ""),
            aiModel = json.optString("aiModel", "gpt-4o-mini"),
            aiTemperature = json.optDouble("aiTemperature", 0.6).toFloat(),
            systemPrompt = json.optString("systemPrompt", DEFAULT_SYSTEM_PROMPT),
            newsProvider = json.optString("newsProvider", "none"),
            newsApiKey = json.optString("newsApiKey", ""),
            debugMode = json.optBoolean("debugMode", false),
            featureAccessibility = json.optBoolean("featureAccessibility", true),
            featureNotificationsRead = json.optBoolean("featureNotificationsRead", false),
            featureBackgroundListening = json.optBoolean("featureBackgroundListening", true),
            featureWebSearch = json.optBoolean("featureWebSearch", true)
        )
    }
}
