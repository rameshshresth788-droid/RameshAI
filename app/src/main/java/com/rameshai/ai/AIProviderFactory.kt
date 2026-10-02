package com.rameshai.ai

import com.rameshai.config.RuntimeConfig

/** Picks the right [AIProvider] implementation purely from [RuntimeConfig.aiProvider]. */
object AIProviderFactory {
    fun create(config: RuntimeConfig): AIProvider = when (config.aiProvider.lowercase()) {
        "gemini" -> GeminiCompatibleProvider(config)
        "openrouter" -> OpenRouterProvider(config)
        "local" -> LocalProvider()
        else -> OpenAICompatibleProvider(config) // "openai" and any unknown value fall back safely
    }
}
