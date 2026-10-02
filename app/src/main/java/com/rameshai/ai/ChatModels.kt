package com.rameshai.ai

/** A single turn in the conversation sent to the AI provider. */
data class ChatMessage(
    val role: Role,
    val content: String
) {
    enum class Role { SYSTEM, USER, ASSISTANT, TOOL }
}

/**
 * A structured tool call the model wants executed. Only tools registered in
 * [com.rameshai.tools.ToolDefinitions] are ever accepted — see
 * [com.rameshai.tools.ToolExecutor] for validation before anything runs.
 */
data class ToolCall(
    val tool: String,
    val arguments: Map<String, String>
)

/** Result returned by an [AIProvider] call: either a spoken reply, a tool call, or an error. */
sealed class AIResult {
    data class Reply(val text: String) : AIResult()
    data class ToolInvocation(val call: ToolCall, val spokenAck: String?) : AIResult()
    data class Error(val message: String, val cause: Throwable? = null) : AIResult()
}
