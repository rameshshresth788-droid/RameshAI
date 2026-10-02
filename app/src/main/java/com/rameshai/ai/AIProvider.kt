package com.rameshai.ai

/**
 * Abstraction over any chat-completion backend. Concrete implementations:
 *  - [OpenAICompatibleProvider] — OpenAI, and any OpenAI-compatible endpoint
 *  - [GeminiCompatibleProvider] — Google Gemini
 *  - [OpenRouterProvider] — OpenRouter (multi-model router, OpenAI-shaped API)
 *  - A future local/offline provider can implement this same interface without
 *    touching any calling code.
 *
 * Implementations must NEVER throw for expected failure modes (missing key,
 * no internet, HTTP error, timeout) — they should return [AIResult.Error] with a
 * clear, user-facing message instead, so the assistant can speak a graceful
 * response instead of crashing.
 */
interface AIProvider {
    val id: String

    suspend fun send(
        history: List<ChatMessage>,
        toolNamesAvailable: List<String>
    ): AIResult
}
