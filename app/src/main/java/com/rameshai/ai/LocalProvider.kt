package com.rameshai.ai

/**
 * Placeholder for a future on-device model (e.g. an ONNX/GGUF runtime). Wired
 * into the same [AIProvider] contract so switching to it later is a one-line
 * change in [AIProviderFactory] plus a real implementation here — no changes
 * needed anywhere else in the app (orchestrator, UI, tool executor all depend
 * only on the interface).
 *
 * Until a real local model is integrated, this clearly tells the user offline
 * AI isn't available yet rather than silently failing.
 */
class LocalProvider : AIProvider {
    override val id: String = "local"

    override suspend fun send(history: List<ChatMessage>, toolNamesAvailable: List<String>): AIResult {
        return AIResult.Error(
            "Local/offline AI provider abhi tak configure nahi hua hai. Settings > AI me " +
                "OpenAI, Gemini ya OpenRouter provider select karo."
        )
    }
}
