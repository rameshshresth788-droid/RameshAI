package com.rameshai.tools

import org.json.JSONArray
import org.json.JSONObject

/**
 * The complete, closed whitelist of tools the AI is allowed to invoke.
 *
 * IMPORTANT SAFETY PROPERTY: the LLM never executes arbitrary code. It can only
 * emit a `{ "tool": "...", "arguments": {...} }` structure, which [ToolExecutor]
 * validates against this list before anything touches the Android system. Any
 * tool name not in [ALL] is rejected outright.
 */
object ToolDefinitions {

    data class ToolSpec(
        val name: String,
        val description: String,
        val params: List<Param>,
        val requiresConfirmation: Boolean = false
    )

    data class Param(val name: String, val type: String, val required: Boolean, val description: String)

    val OPEN_APP = ToolSpec(
        "open_app", "Open an installed application by name or package.",
        listOf(Param("app_name", "string", true, "App name as the user said it, e.g. 'YouTube', 'WhatsApp'"))
    )
    val OPEN_SETTINGS = ToolSpec(
        "open_settings", "Open a specific Android Settings page.",
        listOf(Param("setting", "string", true, "One of: wifi, bluetooth, display, sound, battery, apps, accessibility, notification_listener, general"))
    )
    val OPEN_URL = ToolSpec(
        "open_url", "Open a URL in the default browser.",
        listOf(Param("url", "string", true, "Full URL to open"))
    )
    val SET_BRIGHTNESS = ToolSpec(
        "set_brightness", "Adjust screen brightness where Android permits.",
        listOf(Param("percent", "string", true, "0-100"))
    )
    val MEDIA_PLAY = ToolSpec("media_play", "Resume media playback via the media session.", emptyList())
    val MEDIA_PAUSE = ToolSpec("media_pause", "Pause media playback via the media session.", emptyList())
    val READ_NOTIFICATIONS = ToolSpec(
        "read_notifications", "Read recent notifications (requires Notification Access permission).",
        listOf(Param("app_name", "string", false, "Optional: filter to one app, e.g. 'WhatsApp'"))
    )
    val CREATE_REMINDER = ToolSpec(
        "create_reminder", "Create a one-time or recurring reminder.",
        listOf(
            Param("message", "string", true, "What to remind the user about"),
            Param("time", "string", true, "ISO-8601 local time or HH:mm for the next occurrence"),
            Param("recurrence", "string", false, "none | daily | weekly | custom_days")
        )
    )
    val GET_TODAY_ROUTINE = ToolSpec("get_today_routine", "Fetch the user's configured routine for today.", emptyList())
    val GET_NEWS = ToolSpec(
        "get_news", "Fetch latest news via the configured news provider.",
        listOf(Param("topic", "string", false, "Optional topic/region, e.g. 'Odisha', 'jobs', 'India'"))
    )
    val SEARCH_WEB = ToolSpec(
        "search_web", "Search the internet for current information.",
        listOf(Param("query", "string", true, "Search query"))
    )
    val SEND_MESSAGE = ToolSpec(
        "send_message", "Compose and send a message through a messaging app (e.g. WhatsApp) via Accessibility.",
        listOf(
            Param("app_name", "string", true, "e.g. WhatsApp"),
            Param("contact", "string", true, "Contact name"),
            Param("message", "string", true, "Message text")
        ),
        requiresConfirmation = true
    )
    val MAKE_CALL = ToolSpec(
        "make_call", "Open the dialer with a number ready to call.",
        listOf(Param("contact_or_number", "string", true, "Contact name or phone number")),
        requiresConfirmation = true
    )
    val STOP_ASSISTANT = ToolSpec("stop_assistant", "Stop the background assistant service.", emptyList())

    val ALL: List<ToolSpec> = listOf(
        OPEN_APP, OPEN_SETTINGS, OPEN_URL, SET_BRIGHTNESS, MEDIA_PLAY, MEDIA_PAUSE,
        READ_NOTIFICATIONS, CREATE_REMINDER, GET_TODAY_ROUTINE, GET_NEWS, SEARCH_WEB,
        SEND_MESSAGE, MAKE_CALL, STOP_ASSISTANT
    )

    fun byName(name: String): ToolSpec? = ALL.firstOrNull { it.name == name }

    /** Builds the OpenAI-style `tools` array for function-calling, limited to [allowedNames]. */
    fun asOpenAiFunctionSchema(allowedNames: List<String>): JSONArray {
        val array = JSONArray()
        ALL.filter { allowedNames.isEmpty() || it.name in allowedNames }.forEach { spec ->
            val properties = JSONObject()
            val required = JSONArray()
            spec.params.forEach { p ->
                properties.put(p.name, JSONObject().apply {
                    put("type", p.type)
                    put("description", p.description)
                })
                if (p.required) required.put(p.name)
            }
            array.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", spec.name)
                    put("description", spec.description)
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", properties)
                        put("required", required)
                    })
                })
            })
        }
        return array
    }
}
