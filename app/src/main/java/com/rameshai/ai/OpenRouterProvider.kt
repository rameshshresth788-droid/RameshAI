package com.rameshai.ai

import com.rameshai.config.RuntimeConfig

/** OpenRouter uses the OpenAI-compatible chat/completions API. */
class OpenRouterProvider(config: RuntimeConfig) : AIProvider by OpenAICompatibleProvider(
    config.copy(
        // If the user previously had OpenAI selected, changing only the provider
        // must not accidentally keep the OpenAI endpoint.
        aiBaseUrl = if (config.aiBaseUrl.isBlank() ||
            config.aiBaseUrl.contains("api.openai.com", ignoreCase = true) ||
            config.aiBaseUrl.contains("generativelanguage.googleapis.com", ignoreCase = true)
        ) "https://openrouter.ai/api/v1" else config.aiBaseUrl
    )
) {
    override val id: String = "openrouter"
}
