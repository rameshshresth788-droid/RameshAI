package com.rameshai.ai

import com.rameshai.config.RuntimeConfig
import com.rameshai.tools.ToolDefinitions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Works against any OpenAI-compatible /chat/completions endpoint (OpenAI itself,
 * Azure OpenAI, LM Studio, Ollama's OpenAI-compat mode, etc). Base URL, key and
 * model all come from [RuntimeConfig] — nothing here is hard-coded.
 */
class OpenAICompatibleProvider(
    private val config: RuntimeConfig,
    private val client: OkHttpClient = defaultClient
) : AIProvider {

    override val id: String = "openai"

    override suspend fun send(
        history: List<ChatMessage>,
        toolNamesAvailable: List<String>
    ): AIResult = withContext(Dispatchers.IO) {
        if (config.aiApiKey.isBlank()) {
            return@withContext AIResult.Error(
                "AI API key set nahi hai. Settings > AI me API key configure karo."
            )
        }

        val normalizedBase = when {
            config.aiProvider.equals("openrouter", ignoreCase = true) &&
                (config.aiBaseUrl.isBlank() || config.aiBaseUrl.contains("api.openai.com", ignoreCase = true)) ->
                "https://openrouter.ai/api/v1"
            else -> config.aiBaseUrl.trimEnd('/')
        }
        val url = normalizedBase.trimEnd('/') + "/chat/completions"
        val messages = JSONArray().apply {
            history.forEach { msg ->
                put(JSONObject().apply {
                    put("role", msg.role.name.lowercase())
                    put("content", msg.content)
                })
            }
        }

        fun makeBody(includeTools: Boolean): JSONObject = JSONObject().apply {
            put("model", config.aiModel)
            put("temperature", config.aiTemperature.toDouble())
            put("messages", messages)
            if (includeTools && toolNamesAvailable.isNotEmpty()) {
                put("tools", ToolDefinitions.asOpenAiFunctionSchema(toolNamesAvailable))
            }
        }

        fun requestBody(includeTools: Boolean) = makeBody(includeTools).toString()
            .toRequestBody("application/json".toMediaType())

        try {
            client.newCall(Request.Builder().url(url)
                .addHeader("Authorization", "Bearer ${config.aiApiKey}")
                .addHeader("Content-Type", "application/json")
                .apply {
                    if (config.aiProvider.equals("openrouter", ignoreCase = true)) {
                        addHeader("HTTP-Referer", "https://github.com/rameshshresth788-droid/RameshAI")
                        addHeader("X-Title", "RAMESH AI")
                    }
                }
                .post(requestBody(true)).build()).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (response.isSuccessful) return@withContext parseResponse(raw)

                // Many OpenRouter/free models do not support function calling. Retry once
                // as normal chat so basic conversation still works instead of looking broken.
                if (response.code == 400 && toolNamesAvailable.isNotEmpty()) {
                    client.newCall(Request.Builder().url(url)
                        .addHeader("Authorization", "Bearer ${config.aiApiKey}")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody(false)).build()).execute().use { retry ->
                        val retryRaw = retry.body?.string().orEmpty()
                        if (retry.isSuccessful) return@withContext parseResponse(retryRaw)
                    }
                }
                return@withContext AIResult.Error(
                    "AI provider ne error diya (HTTP ${response.code}). Model/API key check karo."
                )
            }
        } catch (e: IOException) {
            AIResult.Error("Internet connection nahi hai ya AI server tak nahi pahunch pa raha hoon.", e)
        } catch (e: Exception) {
            AIResult.Error("AI response samajhne me error aaya.", e)
        }
    }

    private fun parseResponse(raw: String): AIResult {
        val json = JSONObject(raw)
        val choice = json.optJSONArray("choices")?.optJSONObject(0)
            ?: return AIResult.Error("AI provider se khaali response mila.")
        val message = choice.optJSONObject("message") ?: return AIResult.Error("Invalid AI response.")

        val toolCalls = message.optJSONArray("tool_calls")
        if (toolCalls != null && toolCalls.length() > 0) {
            val first = toolCalls.getJSONObject(0)
            val fn = first.getJSONObject("function")
            val name = fn.getString("name")
            val argsJson = JSONObject(fn.optString("arguments", "{}"))
            val args = mutableMapOf<String, String>()
            argsJson.keys().forEach { key -> args[key] = argsJson.optString(key) }
            return AIResult.ToolInvocation(ToolCall(name, args), message.optString("content").ifBlank { null })
        }

        val content = message.optString("content")
        if (content.isBlank()) return AIResult.Error("AI ne koi jawab nahi diya.")
        return AIResult.Reply(content)
    }

    companion object {
        val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
