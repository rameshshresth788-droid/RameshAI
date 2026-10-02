package com.rameshai.ai

import com.rameshai.config.RuntimeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Google Gemini (generativelanguage.googleapis.com) provider. Tool-calling schema
 * differs from OpenAI's, so this implementation keeps its own request/response
 * mapping rather than sharing code with [OpenAICompatibleProvider] — the shared
 * contract is only [AIProvider] / [AIResult].
 *
 * NOTE: full Gemini function-calling schema mapping is intentionally minimal here;
 * for complex tool schemas prefer the OpenAI-compatible or OpenRouter provider,
 * which have first-class function-calling support in this app.
 */
class GeminiCompatibleProvider(
    private val config: RuntimeConfig,
    private val client: OkHttpClient = OpenAICompatibleProvider.defaultClient
) : AIProvider {

    override val id: String = "gemini"

    override suspend fun send(
        history: List<ChatMessage>,
        toolNamesAvailable: List<String>
    ): AIResult = withContext(Dispatchers.IO) {
        if (config.aiApiKey.isBlank()) {
            return@withContext AIResult.Error(
                "AI API key set nahi hai. Settings > AI me API key configure karo."
            )
        }

        val base = config.aiBaseUrl.ifBlank { "https://generativelanguage.googleapis.com/v1beta" }
        val url = "${base.trimEnd('/')}/models/${config.aiModel}:generateContent?key=${config.aiApiKey}"

        val contents = JSONArray()
        history.filter { it.role != ChatMessage.Role.SYSTEM }.forEach { msg ->
            contents.put(JSONObject().apply {
                put("role", if (msg.role == ChatMessage.Role.ASSISTANT) "model" else "user")
                put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
            })
        }
        val systemPrompt = history.firstOrNull { it.role == ChatMessage.Role.SYSTEM }?.content

        val body = JSONObject().apply {
            put("contents", contents)
            if (systemPrompt != null) {
                put("systemInstruction", JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put("text", systemPrompt))
                ))
            }
            put("generationConfig", JSONObject().put("temperature", config.aiTemperature.toDouble()))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AIResult.Error("Gemini ne error diya (HTTP ${response.code}).")
                }
                val json = JSONObject(raw)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                if (text.isNullOrBlank()) AIResult.Error("Gemini se khaali response mila.")
                else AIResult.Reply(text)
            }
        } catch (e: IOException) {
            AIResult.Error("Internet connection nahi hai ya Gemini tak nahi pahunch pa raha hoon.", e)
        } catch (e: Exception) {
            AIResult.Error("Gemini response samajhne me error aaya.", e)
        }
    }
}
